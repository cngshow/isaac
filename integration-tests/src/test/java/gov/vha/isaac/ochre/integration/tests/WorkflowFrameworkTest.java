package gov.vha.isaac.ochre.integration.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.testing.hk2testng.HK2;
import org.testng.Assert;
import org.testng.annotations.Test;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderService;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.ProcessStatus;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.SubjectMatter;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowActionsPermissionsAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowHistoryAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowProcessInitializerConcluder;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowStatusAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowUpdater;

/**
 * Created by kec on 1/2/16.
 */
@HK2("integration")
public class WorkflowFrameworkTest {
    private static final Logger LOG = LogManager.getLogger();
	private static MVStoreMetaContentProvider store;
	private static WorkflowHistoryAccessor historyAccessor;
	private static WorkflowStatusAccessor statusAccessor;
	private static WorkflowActionsPermissionsAccessor permissionAccessor;
	private static WorkflowProcessInitializerConcluder initConcluder;
	private static WorkflowUpdater updater;
	private static Bpmn2FileImporter importer;
	
	/** The bpmn file path. */
	protected static final String BPMN_FILE_PATH = "src/test/resources/VetzWorkflow.bpmn2";

	private static UUID definitionId;
	private static int userId = 99;
	protected static ArrayList<Integer> stampSequenceForTesting = new ArrayList<>(Arrays.asList(11, 12, 13));
	private static int testConceptSeq;
	private static Set<Integer> testConcepts = new HashSet<>();

    @Test (groups = {"wf"})
    public void testLoadMetaData() {
        LOG.info("Loading Metadata db");
        try {
            BinaryDataReaderService reader = Get.binaryDataReader(Paths.get("target", "data", "IsaacMetadataAuxiliary.ibdf"));
            CommitService commitService = Get.commitService();
            reader.getStream().forEach((object) -> {
                commitService.importNoChecks(object);
            });
            
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowIntegration", true);
            historyAccessor = new WorkflowHistoryAccessor(store);
			statusAccessor = new WorkflowStatusAccessor(store);
			permissionAccessor = new WorkflowActionsPermissionsAccessor(store);
			initConcluder = new WorkflowProcessInitializerConcluder(store);
			updater = new WorkflowUpdater(store);
			
			testConceptSeq = MetaData.ISAAC_METADATA.getConceptSequence();
			testConcepts.add(testConceptSeq);

			importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
			definitionId = importer.getCurrentDefinitionId();
			setupUserRoles();
        } catch (FileNotFoundException e) {
            Assert.fail("File not found", e);
        }
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testStatusAccessorComponentInActiveWorkflow(){
        LOG.info("Testing Workflow History Accessor isComponentInActiveWorkflow()");

        ConceptChronology<? extends ConceptVersion<?>> con = Get.conceptService().getConcept(testConceptSeq);
        SememeChronology<? extends DescriptionSememe<?>> descSem = con.getConceptDescriptionList().iterator().next();

        int conSeq = con.getConceptSequence();
        int semSeq = descSem.getSememeSequence();
        
		try {
			Assert.assertFalse(statusAccessor.isConceptInActiveWorkflow(conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));

			UUID processId = initConcluder.createWorkflowProcess(definitionId, new HashSet<>(Arrays.asList(conSeq)),
					stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			
			Assert.assertTrue(statusAccessor.isConceptInActiveWorkflow(conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));

			initConcluder.launchWorkflowProcess(processId);

			Assert.assertTrue(statusAccessor.isConceptInActiveWorkflow(conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));

			initConcluder.cancelWorkflowProcess(processId, userId, "Canceling Workflow for Testing");
			
			Assert.assertFalse(statusAccessor.isConceptInActiveWorkflow(conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testCancelNoLaunch(){
        LOG.info("Testing ability to cancel a workflow that has only been defined");
    	UUID processId = null;

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
	    	initConcluder.cancelWorkflowProcess(processId, userId, "Cancel for Test");
		} catch (Exception e) {
			Assert.fail();
		}
		
		Assert.assertEquals(ProcessStatus.CANCELED, statusAccessor.getProcessDetail(processId).getProcessStatus());
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testStartCancel(){
        LOG.info("Testing ability to cancel a workflow that has been defined and launched");
    	UUID processId = null;

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflowProcess(processId);
	    	initConcluder.cancelWorkflowProcess(processId, userId, "Cancel for Test");
		} catch (Exception e) {
			Assert.fail();
		}
		
		Assert.assertEquals(ProcessStatus.CANCELED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessCancelState().contains(hx.getOutcome()));
   }
    
    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testFailLaunch(){
        LOG.info("Testing inability to launch a workflow that has yet to be defined");
    	UUID processId = UUID.randomUUID();
    	
    	try {
			initConcluder.launchWorkflowProcess(processId);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
			
			StorableWorkflowContents process = null;
			try {
				process = statusAccessor.getProcessDetail(processId);
			} catch (NullPointerException ee) {
				
			}

			if (process == null) {
				Assert.assertTrue(true);
	    	} else {
	    		Assert.fail();
	    	}
		}
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testFailDefineAfterDefine(){
        LOG.info("Testing inability to define a workflow on a concept that has already been defined");
    	UUID processId = null;
    	Set<Integer> localConcepts = new HashSet<>(Arrays.asList(888888));

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, localConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			processId = initConcluder.createWorkflowProcess(definitionId, localConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
		
		Assert.assertEquals(ProcessStatus.DEFINED, statusAccessor.getProcessDetail(processId).getProcessStatus());
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testFailDefineAfterLaunched(){
        LOG.info("Testing inability to define a workflow on a concept that has already been launched");
    	UUID processId = null;
    	Set<Integer> localConcepts = new HashSet<>(Arrays.asList(555555));

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, localConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflowProcess(processId);
			processId = initConcluder.createWorkflowProcess(definitionId, localConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
		
		Assert.assertEquals(ProcessStatus.LAUNCHED, statusAccessor.getProcessDetail(processId).getProcessStatus());
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testFailConclude(){
        LOG.info("Testing inability to conclude a workflow that hasn't reached a final workflow state");
    	UUID processId = null;
    	Set<Integer> localConcepts = new HashSet<>(Arrays.asList(777777));

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, localConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflowProcess(processId);
	    	initConcluder.concludeWorkflowProcess(processId, userId);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
		
		Assert.assertEquals(ProcessStatus.LAUNCHED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessStartState().contains(hx.getState()));
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testStartAllPassConclude(){
        LOG.info("Testing ability to advance workflow to conclusion via its easy-path");
    	UUID processId = null;

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflowProcess(processId);
	    	updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");
	    	initConcluder.concludeWorkflowProcess(processId, userId);
		} catch (Exception e) {
			Assert.fail();
		}
		
		Assert.assertEquals(ProcessStatus.CONCLUDED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessConcludeState().contains(hx.getOutcome()));
    }
    
    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testFailCancelCall(){
        LOG.info("Testing inability to cancel an already concluded Workflow ");
    	UUID processId = null;

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflowProcess(processId);
	    	updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");
	    	initConcluder.concludeWorkflowProcess(processId, userId);
	    	initConcluder.cancelWorkflowProcess(processId, userId, "Cancel for Test");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
		
		Assert.assertEquals(ProcessStatus.CONCLUDED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessConcludeState().contains(hx.getOutcome()));
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testRedefineCall(){
        LOG.info("Testing ability to define and launch workflow on a concept that has an already-concluded workflow");
    	UUID processId = null;
    	Set<Integer> localConcepts = new HashSet<>(Arrays.asList(333333));

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, localConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflowProcess(processId);
	    	updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");
	    	initConcluder.concludeWorkflowProcess(processId, userId);
	    	
			Assert.assertEquals(ProcessStatus.CONCLUDED, statusAccessor.getProcessDetail(processId).getProcessStatus());
			ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
			Assert.assertTrue(AbstractWorkflowUtilities.getProcessConcludeState().contains(hx.getOutcome()));
			
			processId = initConcluder.createWorkflowProcess(definitionId, localConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			Assert.assertEquals(ProcessStatus.DEFINED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		} catch (Exception e) {
			Assert.fail();
		}
    }


    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testStartAllFailConclude(){
        LOG.info("Testing ability to advance workflow to conclusion via with a rejection/failure happening at each point in path");
    	UUID processId = null;

    	try {
			processId = initConcluder.createWorkflowProcess(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflowProcess(processId);
	    	updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Fails", "Fail Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Edit", "Second Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Reject Edit", "Reject Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "Edit", "Third Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Passes", "Second Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Reject Review", "Reject Review Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Passes", "Third Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");
	    	initConcluder.concludeWorkflowProcess(processId, userId);
	    	initConcluder.concludeWorkflowProcess(processId, userId);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
		
		Assert.assertEquals(ProcessStatus.CONCLUDED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessConcludeState().contains(hx.getOutcome()));
 }

	private void setupUserRoles() {
		updater.addNewUserRole(definitionId, userId, "Editor");
		updater.addNewUserRole(definitionId, userId, "Reviewer");
		updater.addNewUserRole(definitionId, userId, "Approver");
	}
}

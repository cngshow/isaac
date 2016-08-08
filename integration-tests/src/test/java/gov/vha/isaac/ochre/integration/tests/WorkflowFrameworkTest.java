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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.jvnet.testing.hk2testng.HK2;
import org.testng.Assert;
import org.testng.annotations.Test;

import gov.vha.isaac.MetaData;
import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderService;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowActionsPermissionsAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowHistoryAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowInitializerConcluder;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowStatusAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowUpdater;

/**
 * Created by kec on 1/2/16.
 */
@HK2("integration")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowFrameworkTest {
    private static final Logger LOG = LogManager.getLogger();
	private static MVStoreMetaContentProvider store;
	private static WorkflowHistoryAccessor historyAccessor;
	private static WorkflowStatusAccessor statusAccessor;
	private static WorkflowActionsPermissionsAccessor permissionAccessor;
	private static WorkflowInitializerConcluder initConcluder;
	private static WorkflowUpdater updater;
	private static Bpmn2FileImporter importer;
	
	/** The bpmn file path. */
	protected static final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/workflow/provider/VetzWorkflow.bpmn2";

	private static UUID definitionId;
	private static UUID tempProcessId;
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
			initConcluder = new WorkflowInitializerConcluder(store);
			updater = new WorkflowUpdater(store);
			
			testConceptSeq = MetaData.ISAAC_METADATA.getConceptSequence();
			testConcepts.add(testConceptSeq);

			importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
			definitionId = importer.getCurrentDefinitionId();

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

			UUID processId = initConcluder.defineWorkflow(definitionId, new HashSet<>(Arrays.asList(conSeq)),
					stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			
			Assert.assertTrue(statusAccessor.isConceptInActiveWorkflow(conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));

			initConcluder.launchWorkflow(processId);

			Assert.assertTrue(statusAccessor.isConceptInActiveWorkflow(conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));

			initConcluder.cancelWorkflow(processId, "Canceling Workflow for Testing");
			
			Assert.assertFalse(statusAccessor.isConceptInActiveWorkflow(conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testCancelNoLaunch"})
    public void testCancelNoLaunch(){
    	UUID processId = null;
		try {
			processId = initConcluder.defineWorkflow(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
	    	initConcluder.cancelWorkflow(processId, "Cancel for Test");
		} catch (Exception e) {
			Assert.fail();
		}
		
		Assert.assertEquals(ProcessStatus.CANCELED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessCanceledStates().contains(hx.getState()));
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testStartCancel"})
    public void testStartCancel(){
    	UUID processId = null;
		try {
			processId = initConcluder.defineWorkflow(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflow(processId);
	    	initConcluder.cancelWorkflow(processId, "Cancel for Test");
		} catch (Exception e) {
			Assert.fail();
		}
		
		Assert.assertEquals(ProcessStatus.CANCELED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessCanceledStates().contains(hx.getState()));
   }
    
    @Test (groups = {"wf"}, dependsOnMethods = {"testFailLaunch"})
    public void testFailLaunch(){
    	UUID processId = UUID.randomUUID();
    	
    	try {
			initConcluder.launchWorkflow(processId);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
			
	    	ProcessDetail process = statusAccessor.getProcessDetail(processId);
	    	if (process == null) {
				Assert.assertTrue(true);
	    	} else {
	    		Assert.fail();
	    	}
		}
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testCancelNoLaunch"})
    public void testFailDefine(){
    	UUID processId = null;
		try {
			processId = initConcluder.defineWorkflow(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflow(processId);
			processId = initConcluder.defineWorkflow(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
		
		Assert.assertEquals(ProcessStatus.LAUNCHED, statusAccessor.getProcessDetail(processId).getProcessStatus());
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testCancelNoLaunch"})
    public void testFailConclude(){
    	UUID processId = null;
		try {
			processId = initConcluder.defineWorkflow(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflow(processId);
	    	initConcluder.concludeWorkflow(processId);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
		
		Assert.assertEquals(ProcessStatus.LAUNCHED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessStartedStates().contains(hx.getState()));
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testStartAllPassConclude"})
    public void testStartAllPassConclude(){
    	UUID processId = null;
		try {
			processId = initConcluder.defineWorkflow(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflow(processId);
	    	updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");
	    	initConcluder.concludeWorkflow(processId);
		} catch (Exception e) {
			Assert.fail();
		}
		
		Assert.assertEquals(ProcessStatus.CONCLUDED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessConcludedStates().contains(hx.getState()));
    }
    
    @Test (groups = {"wf"}, dependsOnMethods = {"testCancelNoLaunch"})
    public void testFailedCancelCall(){
    	UUID processId = null;
		try {
			processId = initConcluder.defineWorkflow(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflow(processId);
	    	updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
	    	updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
	    	updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");
	    	initConcluder.concludeWorkflow(processId);
	    	initConcluder.cancelWorkflow(processId, "Cancel for Test");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
		
		Assert.assertEquals(ProcessStatus.CONCLUDED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessConcludedStates().contains(hx.getState()));
    }


    @Test (groups = {"wf"}, dependsOnMethods = {"testStartAllFailConclude"})
    public void testStartAllFailConclude(){
    	UUID processId = null;
		try {
			processId = initConcluder.defineWorkflow(definitionId, testConcepts, stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			initConcluder.launchWorkflow(processId);
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
	    	initConcluder.concludeWorkflow(processId);
	    	initConcluder.concludeWorkflow(processId);
		} catch (Exception e) {
			Assert.fail();
		}
		
		Assert.assertEquals(ProcessStatus.CONCLUDED, statusAccessor.getProcessDetail(processId).getProcessStatus());
		ProcessHistory hx = historyAccessor.getLatestForProcess(processId);
		Assert.assertTrue(AbstractWorkflowUtilities.getProcessConcludedStates().contains(hx.getState()));
    }
}

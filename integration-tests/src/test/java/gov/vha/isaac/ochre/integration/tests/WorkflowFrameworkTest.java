package gov.vha.isaac.ochre.integration.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.testing.hk2testng.HK2;
import org.testng.Assert;
import org.testng.annotations.Test;

import gov.vha.isaac.MetaData;
import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderService;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowActionsPermissionsAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowHistoryAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowInitializerConcluder;
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
	private static WorkflowInitializerConcluder initConcluder;
	private static WorkflowUpdater updater;

	private static UUID definitionId = UUID.randomUUID();
	private static UUID processId = UUID.randomUUID();
	private static int userId = 99;
	protected static ArrayList<Integer> stampSequenceForTesting = new ArrayList<>(Arrays.asList(11, 12, 13));
	
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
			
        } catch (FileNotFoundException e) {
            Assert.fail("File not found", e);
        }
    }

    @Test (groups = {"wf"}, dependsOnMethods = {"testLoadMetaData"})
    public void testStatusAccessorComponentInActiveWorkflow(){
        LOG.info("Testing Workflow History Accessor isComponentInActiveWorkflow()");

        ConceptChronology<? extends ConceptVersion<?>> con = Get.conceptService().getConcept(MetaData.ISAAC_METADATA.getConceptSequence());
        SememeChronology<? extends DescriptionSememe<?>> descSem = con.getConceptDescriptionList().iterator().next();

        int conSeq = con.getConceptSequence();
        int semSeq = descSem.getSememeSequence();
        
		try {
			Assert.assertFalse(statusAccessor.isConceptInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));

			processId = initConcluder.defineWorkflow(definitionId, new HashSet<>(Arrays.asList(conSeq)),
					stampSequenceForTesting, userId, SubjectMatter.CONCEPT);
			
			Assert.assertTrue(statusAccessor.isConceptInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));

			initConcluder.launchWorkflow(processId);

			Assert.assertTrue(statusAccessor.isConceptInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertTrue(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));

			initConcluder.cancelWorkflowProcess(processId, "Canceling Workflow for Testing");
			
			Assert.assertFalse(statusAccessor.isConceptInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(con.getOchreObjectType(), conSeq));
			Assert.assertFalse(statusAccessor.isComponentInActiveWorkflow(descSem.getOchreObjectType(), semSeq));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
    }
}

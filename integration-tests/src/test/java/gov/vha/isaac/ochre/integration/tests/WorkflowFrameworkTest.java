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
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
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
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowProcessInitializerConcluder;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowUpdater;

/**
 * Created by kec on 1/2/16.
 */
@HK2("integration")
public class WorkflowFrameworkTest {
	private static final Logger LOG = LogManager.getLogger();
	private static MVStoreMetaContentProvider store;
	private static WorkflowAccessor wfAccessor;
	private static WorkflowProcessInitializerConcluder initConcluder;
	private static WorkflowUpdater updater;
	private static Bpmn2FileImporter importer;

	/** The bpmn file path. */
	private static final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/integration/tests/StaticWorkflowIntegrationTestingDefinition.bpmn2";

	private static UUID definitionId;
	private static int userId = 99;
	protected static ArrayList<Integer> stampSequenceForTesting = new ArrayList<>(Arrays.asList(11, 12, 13));
	private static int testConceptSeq;
	private static Set<Integer> testConcepts = new HashSet<>();
	private ProcessDetailContentStore processDetailStore;
	private ProcessHistoryContentStore processHistoryStore;

	@Test(groups = { "wf" })
	public void testLoadMetaData() {
		LOG.info("Loading Metadata db");
		try {
			BinaryDataReaderService reader = Get
					.binaryDataReader(Paths.get("target", "data", "IsaacMetadataAuxiliary.ibdf"));
			CommitService commitService = Get.commitService();
			reader.getStream().forEach((object) -> {
				commitService.importNoChecks(object);
			});

			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowIntegration", true);
			wfAccessor = new WorkflowAccessor(store);
			initConcluder = new WorkflowProcessInitializerConcluder(store);
			updater = new WorkflowUpdater(store);
			processDetailStore = new ProcessDetailContentStore(store);
			processHistoryStore = new ProcessHistoryContentStore(store);

			testConceptSeq = MetaData.ISAAC_METADATA.getConceptSequence();
			testConcepts.add(testConceptSeq);

			importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
			definitionId = importer.getCurrentDefinitionId();
			setupUserRoles();
		} catch (FileNotFoundException e) {
			Assert.fail("File not found", e);
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testStatusAccessorComponentInActiveWorkflow() {
		LOG.info("Testing Workflow History Accessor isComponentInActiveWorkflow()");

		ConceptChronology<? extends ConceptVersion<?>> con = Get.conceptService().getConcept(testConceptSeq);
		SememeChronology<? extends DescriptionSememe<?>> descSem = con.getConceptDescriptionList().iterator().next();

		int conSeq = con.getConceptSequence();
		int semSeq = descSem.getSememeSequence();

		try {
			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, conSeq));
			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, semSeq));

			UUID processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");

			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, conSeq));
			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, semSeq));

			updater.addComponentToWorkflow(processId, conSeq, 1110);
			updater.addComponentToWorkflow(processId, semSeq, 1111);

			Assert.assertTrue(wfAccessor.isComponentInActiveWorkflow(definitionId, conSeq));
			Assert.assertTrue(wfAccessor.isComponentInActiveWorkflow(definitionId, semSeq));			
			
			initConcluder.launchWorkflowProcess(processId);

			Assert.assertTrue(wfAccessor.isComponentInActiveWorkflow(definitionId, conSeq));
			Assert.assertTrue(wfAccessor.isComponentInActiveWorkflow(definitionId, semSeq));

			initConcluder.endWorkflowProcess(processId, getCancelAction(), userId, "Canceling Workflow for Testing",
					EndWorkflowType.CANCELED);

			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, conSeq));
			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, semSeq));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testCancelNoLaunch() {
		LOG.info("Testing ability to cancel a workflow that has only been defined");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			initConcluder.endWorkflowProcess(processId, getCancelAction(), userId, "Canceling Workflow for Testing",
					EndWorkflowType.CANCELED);

			Assert.assertEquals(ProcessStatus.CANCELED, wfAccessor.getProcessDetails(processId).getStatus());
		} catch (Exception e) {
			Assert.fail();
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testConcludeNoLaunch() {
		LOG.info("Testing ability to conclude a workflow that has only been defined");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			initConcluder.endWorkflowProcess(processId, getConcludeAction(), userId, "Concluding Workflow for Testing",
					EndWorkflowType.CONCLUDED);

		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		Assert.assertEquals(ProcessStatus.DEFINED, wfAccessor.getProcessDetails(processId).getStatus());

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testStartCancel() {
		LOG.info("Testing ability to cancel a workflow that has been defined and launched");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			updater.addComponentToWorkflow(processId, 222, 2220);
			initConcluder.launchWorkflowProcess(processId);
			initConcluder.endWorkflowProcess(processId, getCancelAction(), userId, "Canceling Workflow for Testing",
					EndWorkflowType.CANCELED);

			Assert.assertEquals(ProcessStatus.CANCELED, wfAccessor.getProcessDetails(processId).getStatus());
			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertTrue(isEndState(hx.getOutcomeState(), EndWorkflowType.CANCELED));
		} catch (Exception e) {
			Assert.fail();
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailLaunch() {
		LOG.info("Testing inability to launch a workflow that has yet to be defined");
		UUID processId = UUID.randomUUID();

		try {
			initConcluder.launchWorkflowProcess(processId);
			Assert.fail();
		} catch (Exception e) {
			try {
				Assert.assertTrue(true);

				StorableWorkflowContents process = null;
				try {
					process = wfAccessor.getProcessDetails(processId);
				} catch (NullPointerException ee) {

				}

				if (process == null) {
					Assert.assertTrue(true);
				} else {
					Assert.fail();
				}
			} catch (Exception ee) {
				Assert.fail();
			}
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailDefineAfterDefineSameName() {
		LOG.info("Testing inability to define a workflow on a concept that has already been defined");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
			Assert.assertEquals(ProcessStatus.DEFINED, wfAccessor.getProcessDetails(processId).getStatus());
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailDefineAfterLaunched() {
		LOG.info("Testing inability to add a concept onto a workflow other than one that has already been launched");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			updater.addComponentToWorkflow(processId, 333, 3330);
			try {
				updater.addComponentToWorkflow(processId, 333, 3331);
				Assert.assertTrue(true);
			} catch (Exception e) {
				Assert.fail();
			}
			initConcluder.launchWorkflowProcess(processId);
			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
			try {
				updater.addComponentToWorkflow(processId, 333, 3332);
				Assert.fail();
			} catch (Exception e) {
				Assert.assertTrue(true);
			}

			updater.advanceWorkflow(processId, userId, "QA Fails", "QA Fail");
			UUID processId2 = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name2",
					" Framework Workflow Description");
			try {
				updater.addComponentToWorkflow(processId2, 333, 3333);
				Assert.fail();
			} catch (Exception e) {
				Assert.assertTrue(true);
			}
			updater.addComponentToWorkflow(processId, 333, 3334);

			Assert.assertEquals(ProcessStatus.LAUNCHED, wfAccessor.getProcessDetails(processId).getStatus());
		} catch (Exception e) {
			Assert.fail();
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailConclude() {
		LOG.info("Testing inability to conclude a workflow that hasn't reached a final workflow state");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			updater.addComponentToWorkflow(processId, 444, 4440);
			initConcluder.launchWorkflowProcess(processId);
			initConcluder.endWorkflowProcess(processId, getConcludeAction(), userId, "Conclude Workflow for Testing",
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);

			Assert.assertEquals(ProcessStatus.LAUNCHED, wfAccessor.getProcessDetails(processId).getStatus());
			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertTrue(AbstractWorkflowUtilities.getEditStates().contains(hx.getOutcomeState()));
			Assert.assertTrue(isStartState(definitionId, hx.getInitialState()));
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testStartAllPassConclude() {
		LOG.info("Testing ability to advance workflow to conclusion via its easy-path");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			updater.addComponentToWorkflow(processId, 555, 5550);
			initConcluder.launchWorkflowProcess(processId);
			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
			updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
			updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");
			
			Assert.assertEquals(ProcessStatus.CONCLUDED, wfAccessor.getProcessDetails(processId).getStatus());
			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertTrue(isEndState(hx.getOutcomeState(), EndWorkflowType.CONCLUDED));
		} catch (Exception e) {
			Assert.fail();
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailCancelCall() {
		LOG.info("Testing inability to cancel an already concluded Workflow ");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			updater.addComponentToWorkflow(processId, 666, 6660);
			initConcluder.launchWorkflowProcess(processId);
			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
			updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
			updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");
			initConcluder.endWorkflowProcess(processId, getCancelAction(), userId, "Canceling Workflow for Testing",
					EndWorkflowType.CANCELED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);

			Assert.assertEquals(ProcessStatus.CONCLUDED, wfAccessor.getProcessDetails(processId).getStatus());
			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertTrue(isEndState(hx.getOutcomeState(), EndWorkflowType.CONCLUDED));
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testRedefineCall() {
		LOG.info("Testing ability to define and launch workflow on a concept that has an already-concluded workflow");

		try {
			UUID processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			updater.addComponentToWorkflow(processId, 777, 7770);
			initConcluder.launchWorkflowProcess(processId);
			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
			updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
			updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");

			Assert.assertEquals(ProcessStatus.CONCLUDED, wfAccessor.getProcessDetails(processId).getStatus());
			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertTrue(isEndState(hx.getOutcomeState(), EndWorkflowType.CONCLUDED));

			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name2",
					" Framework Workflow Description");
			Assert.assertEquals(ProcessStatus.DEFINED, wfAccessor.getProcessDetails(processId).getStatus());
		} catch (Exception e) {
			Assert.fail();
		}

		clearStores();
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testStartAllFailConclude() {
		LOG.info(
				"Testing ability to advance workflow to conclusion via with a rejection/failure happening at each point in path");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			updater.addComponentToWorkflow(processId, 888, 8880);
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
			initConcluder.endWorkflowProcess(processId, getConcludeAction(), userId,
					"Canceling Workflow for Testing", EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			try {
				Assert.assertTrue(true);
				Assert.assertEquals(ProcessStatus.CONCLUDED, wfAccessor.getProcessDetails(processId).getStatus());
				ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
				Assert.assertTrue(isEndState(hx.getOutcomeState(), EndWorkflowType.CONCLUDED));
			} catch (Exception ee) {
				Assert.fail();
			}
		}

		clearStores();
	}

	private void clearStores() {
		processDetailStore.removeAllEntries();
		processHistoryStore.removeAllEntries();
	}

	private void setupUserRoles() {
		updater.addNewUserRole(definitionId, userId, "Editor");
		updater.addNewUserRole(definitionId, userId, "Reviewer");
		updater.addNewUserRole(definitionId, userId, "Approver");
	}

	private boolean isStartState(UUID defId, String state) {
		for (AvailableAction action : AbstractWorkflowUtilities.getDefinitionStartActionMap().get(defId)) {
			if (action.getInitialState().equals(state)) {
				return true;
			}
		}

		return false;
	}

	private boolean isEndState(String state, EndWorkflowType type) {
		for (AvailableAction action : AbstractWorkflowUtilities.getEndWorkflowTypeMap().get(type)) {
			if (action.getOutcomeState().equals(state)) {
				return true;
			}
		}

		return false;
	}

	private AvailableAction getCancelAction() {
		return AbstractWorkflowUtilities.getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED).iterator().next();
	}

	private AvailableAction getConcludeAction() {
		return AbstractWorkflowUtilities.getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED).iterator().next();
	}
}

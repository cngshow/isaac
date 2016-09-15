package gov.vha.isaac.ochre.integration.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mahout.math.map.OpenIntIntHashMap;
import org.jvnet.testing.hk2testng.HK2;
import org.testng.Assert;
import org.testng.annotations.Test;

import gov.vha.isaac.MetaData;
import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserPermissionContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AbstractStorableWorkflowContents;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.collections.SememeSequenceSet;
import gov.vha.isaac.ochre.api.collections.StampSequenceSet;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderService;
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

	private static final String LAUNCH_STATE = "Ready for Edit";
	private static final String LAUNCH_ACTION = "Edit";
	private static final String LAUNCH_OUTCOME = "Ready for Review";
	private static final String LAUNCH_COMMENT = "Launch Comment";

	private static final String SEND_TO_APPROVAL_STATE = "Ready for Review";
	private static final String SEND_TO_APPROVAL_ACTION = "Review";
	private static final String SEND_TO_APPROVAL_OUTCOME = "Ready for Approve";
	private static final String SEND_TO_APPROVAL_COMMENT = "Sending for Approval";

	private static final String REJECT_REVIEW_STATE = "Ready for Review";
	private static final String REJECT_REVIEW_ACTION = "Reject QA";
	private static final String REJECT_REVIEW_OUTCOME = "Ready for Edit";
	private static final String REJECT_REVIEW_COMMENT = "Rejecting QA sending back to Edit";

	protected static final String CONCLUDED_WORKFLOW_COMMENT = "Concluded Workflow";
	protected static final String CANCELED_WORKFLOW_COMMENT = "Canceled Workflow";

	/** The bpmn file path. */
	private static final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/integration/tests/StaticWorkflowIntegrationTestingDefinition.bpmn2";

	private static UUID definitionId;
	private static int userId = 99;
	private static int firstTestConceptNid;
	private static int secondTestConceptNid;

	private UserPermissionContentStore userPermissionStore;
	private ProcessDetailContentStore processDetailStore;
	private ProcessHistoryContentStore processHistoryStore;
	protected static AvailableAction cancelAction;

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
			userPermissionStore = new UserPermissionContentStore(store);

			firstTestConceptNid = MetaData.ISAAC_METADATA.getNid();
			secondTestConceptNid = MetaData.ACCEPTABLE.getNid();

			importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
			definitionId = importer.getCurrentDefinitionId();

			cancelAction = importer.getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED).iterator().next();

			setupUserRoles();
		} catch (FileNotFoundException e) {
			Assert.fail("File not found", e);
		}
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testStatusAccessorComponentInActiveWorkflow() {
		clearStores();

		LOG.info("Testing Workflow History Accessor isComponentInActiveWorkflow()");

		ConceptChronology<? extends ConceptVersion<?>> con = Get.conceptService().getConcept(firstTestConceptNid);
		SememeChronology<? extends DescriptionSememe<?>> descSem = con.getConceptDescriptionList().iterator().next();

		int conNid = con.getNid();
		int semNid = descSem.getNid();

		try {
			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, conNid));
			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, semNid));

			UUID processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");

			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, conNid));
			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, semNid));

			Optional<CommitRecord> commitRecord = createCommitRecord(conNid, null, 1110);
			updater.addCommitRecordToWorkflow(processId, commitRecord);
			commitRecord = createCommitRecord(null, semNid, 1111);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

			Assert.assertTrue(wfAccessor.isComponentInActiveWorkflow(definitionId, conNid));
			Assert.assertTrue(wfAccessor.isComponentInActiveWorkflow(definitionId, semNid));

			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");

			Assert.assertTrue(wfAccessor.isComponentInActiveWorkflow(definitionId, conNid));
			Assert.assertTrue(wfAccessor.isComponentInActiveWorkflow(definitionId, semNid));

			initConcluder.endWorkflowProcess(processId, getCancelAction(), userId, "Canceling Workflow for Testing",
					EndWorkflowType.CANCELED);

			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, conNid));
			Assert.assertFalse(wfAccessor.isComponentInActiveWorkflow(definitionId, semNid));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testCancelNoLaunch() {
		clearStores();

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
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testConcludeNoLaunch() {
		clearStores();

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
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testStartCancel() {
		clearStores();

		LOG.info("Testing ability to cancel a workflow that has been defined and launched");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");

			Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 2220);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
			initConcluder.endWorkflowProcess(processId, getCancelAction(), userId, "Canceling Workflow for Testing",
					EndWorkflowType.CANCELED);

			Assert.assertEquals(ProcessStatus.CANCELED, wfAccessor.getProcessDetails(processId).getStatus());
			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertTrue(isEndState(hx.getOutcomeState(), EndWorkflowType.CANCELED));
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailLaunch() {
		clearStores();

		LOG.info("Testing inability to launch a workflow that has yet to be defined");
		UUID processId = UUID.randomUUID();

		try {
			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
			Assert.fail();
		} catch (Exception e) {
			try {
				Assert.assertTrue(true);

				AbstractStorableWorkflowContents process = null;
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
	}

	// TODO: Decide if prevent multiple processes with same name
	/*
	 * @Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	 * public void testFailDefineAfterDefineSameName() { LOG.
	 * info("Testing inability to define a workflow on a concept that has already been defined"
	 * ); UUID processId = null;
	 * 
	 * try { processId = initConcluder.createWorkflowProcess(definitionId,
	 * userId, "Framework Workflow Name", " Framework Workflow Description");
	 * processId = initConcluder.createWorkflowProcess(definitionId, userId,
	 * "Framework Workflow Name", " Framework Workflow Description");
	 * Assert.fail(); } catch (Exception e) { Assert.assertTrue(true);
	 * Assert.assertEquals(ProcessStatus.DEFINED,
	 * wfAccessor.getProcessDetails(processId).getStatus()); }
	 * 
	 * clearStores(); }
	 */

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailDefineAfterLaunched() {
		clearStores();

		LOG.info("Testing inability to add a concept onto a workflow other than one that has already been launched");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");
			Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 3330);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

			commitRecord = createCommitRecord(firstTestConceptNid, null, 3331);
			updater.addCommitRecordToWorkflow(processId, commitRecord);
			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");

			try {
				commitRecord = createCommitRecord(firstTestConceptNid, null, 3332);
				updater.addCommitRecordToWorkflow(processId, commitRecord);
				Assert.fail();
			} catch (Exception e) {
				Assert.assertTrue(true);
			}

			updater.advanceWorkflow(processId, userId, "QA Fails", "QA Fail");

			commitRecord = createCommitRecord(firstTestConceptNid, null, 3333);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

			Assert.assertEquals(ProcessStatus.LAUNCHED, wfAccessor.getProcessDetails(processId).getStatus());
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailConclude() {
		clearStores();

		LOG.info("Testing inability to conclude a workflow that hasn't reached a final workflow state");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");

			Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 4440);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertTrue(importer.getEditStatesMap().get(definitionId).contains(hx.getOutcomeState()));
			Assert.assertTrue(isStartState(definitionId, hx.getInitialState()));

			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
			initConcluder.endWorkflowProcess(processId, getConcludeAction(), userId, "Conclude Workflow for Testing",
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);

			Assert.assertEquals(ProcessStatus.LAUNCHED, wfAccessor.getProcessDetails(processId).getStatus());
			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertFalse(importer.getEditStatesMap().get(definitionId).contains(hx.getOutcomeState()));
			Assert.assertFalse(isStartState(definitionId, hx.getInitialState()));
		}
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testStartAllPassConclude() {
		clearStores();

		LOG.info("Testing ability to advance workflow to conclusion via its easy-path");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");

			Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 5550);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

			updater.advanceWorkflow(processId, userId, "Edit", "Edit Comment");
			updater.advanceWorkflow(processId, userId, "QA Passes", "Review Comment");
			updater.advanceWorkflow(processId, userId, "Approve", "Approve Comment");

			Assert.assertEquals(ProcessStatus.CONCLUDED, wfAccessor.getProcessDetails(processId).getStatus());
			ProcessHistory hx = wfAccessor.getProcessHistory(processId).last();
			Assert.assertTrue(isEndState(hx.getOutcomeState(), EndWorkflowType.CONCLUDED));
		} catch (Exception e) {
			Assert.fail();
		}
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testFailCancelCall() {
		clearStores();

		LOG.info("Testing inability to cancel an already concluded Workflow ");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");

			Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 6660);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

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
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testRedefineCall() {
		clearStores();

		LOG.info("Testing ability to define and launch workflow on a concept that has an already-concluded workflow");

		try {
			UUID processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");

			Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 7770);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

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
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testStartAllFailConclude() {
		clearStores();

		LOG.info(
				"Testing ability to advance workflow to conclusion via with a rejection/failure happening at each point in path");
		UUID processId = null;

		try {
			processId = initConcluder.createWorkflowProcess(definitionId, userId, "Framework Workflow Name",
					" Framework Workflow Description");

			Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 8880);
			updater.addCommitRecordToWorkflow(processId, commitRecord);

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
			initConcluder.endWorkflowProcess(processId, getConcludeAction(), userId, "Canceling Workflow for Testing",
					EndWorkflowType.CONCLUDED);
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
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testIntegrationAddCommitRecordToWorkflow() throws Exception {
		clearStores();

		// Cannot make this work without at least a Mock Database.
		// Added to Integration-Test module's workflowFramworkTest. For now just
		// pass.
		Assert.assertTrue(true);
		UUID processId = createFirstWorkflowProcess(definitionId);
		ProcessDetail details = processDetailStore.getEntry(processId);
		Assert.assertFalse(details.getComponentNidToStampsMap().containsKey(firstTestConceptNid));

		Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 9990);
		updater.addCommitRecordToWorkflow(processId, commitRecord);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentNidToStampsMap().size());
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(firstTestConceptNid));
		Assert.assertEquals(1, details.getComponentNidToStampsMap().get(firstTestConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstTestConceptNid).contains(9990));

		commitRecord = createCommitRecord(firstTestConceptNid, null, 9991);
		updater.addCommitRecordToWorkflow(processId, commitRecord);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentNidToStampsMap().size());
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(firstTestConceptNid));
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(firstTestConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstTestConceptNid).contains(9990));
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstTestConceptNid).contains(9991));

		commitRecord = createCommitRecord(secondTestConceptNid, null, 9990);
		updater.addCommitRecordToWorkflow(processId, commitRecord);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(2, details.getComponentNidToStampsMap().size());
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(firstTestConceptNid));
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(firstTestConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstTestConceptNid).contains(9990));
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstTestConceptNid).contains(9991));
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(secondTestConceptNid));
		Assert.assertEquals(1, details.getComponentNidToStampsMap().get(secondTestConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(secondTestConceptNid).contains(9990));
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testIntegrationRemoveComponentsFromProcess() throws Exception {
		clearStores();

		UUID processId = createFirstWorkflowProcess(definitionId);
		ProcessDetail details = processDetailStore.getEntry(processId);
		Assert.assertEquals(0, details.getComponentNidToStampsMap().size());

		Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 121200);
		updater.addCommitRecordToWorkflow(processId, commitRecord);

		commitRecord = createCommitRecord(firstTestConceptNid, null, 121201);
		updater.addCommitRecordToWorkflow(processId, commitRecord);

		commitRecord = createCommitRecord(secondTestConceptNid, null, 121200);
		updater.addCommitRecordToWorkflow(processId, commitRecord);

		commitRecord = createCommitRecord(secondTestConceptNid, null, 121201);
		updater.addCommitRecordToWorkflow(processId, commitRecord);

		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(2, details.getComponentNidToStampsMap().size());
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(firstTestConceptNid).size());
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(secondTestConceptNid).size());

		updater.removeComponentFromWorkflow(processId, firstTestConceptNid);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentNidToStampsMap().size());
		Assert.assertFalse(details.getComponentNidToStampsMap().containsKey(firstTestConceptNid));
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(secondTestConceptNid));
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(secondTestConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(secondTestConceptNid).contains(121200));
		Assert.assertTrue(details.getComponentNidToStampsMap().get(secondTestConceptNid).contains(121201));

		updater.removeComponentFromWorkflow(processId, secondTestConceptNid);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(0, details.getComponentNidToStampsMap().size());
	}

	@Test(groups = { "wf" }, dependsOnMethods = { "testLoadMetaData" })
	public void testIntegrationFailuresWithAddRemoveComponentsToProcess() throws Exception {
		clearStores();

		UUID processId = UUID.randomUUID();

		try {
			updater.removeComponentFromWorkflow(processId, firstTestConceptNid);
			Assert.fail();
		} catch (Exception e) {

		}

		UUID firstProcessId = createFirstWorkflowProcess(definitionId);

		Optional<CommitRecord> commitRecord = createCommitRecord(firstTestConceptNid, null, 565600);
		updater.addCommitRecordToWorkflow(firstProcessId, commitRecord);

		updater.advanceWorkflow(firstProcessId, userId, "Edit", "Edit Comment");

		try {
			updater.addCommitRecordToWorkflow(firstProcessId, commitRecord);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		try {
			// Go back to no components in any workflow
			updater.removeComponentFromWorkflow(firstProcessId, firstTestConceptNid);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		executeSendForReviewAdvancement(firstProcessId);

		try {
			updater.addCommitRecordToWorkflow(firstProcessId, commitRecord);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		try {
			// Go back to no components in any workflow
			updater.removeComponentFromWorkflow(firstProcessId, firstTestConceptNid);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		// Rejecting QA to get back to edit state
		executeRejectReviewAdvancement(firstProcessId);

		// Go back to no components in any workflow
		updater.removeComponentFromWorkflow(firstProcessId, firstTestConceptNid);

		// Testing LAUNCHED-EDIT Case
		updater.addCommitRecordToWorkflow(firstProcessId, commitRecord);
		ProcessDetail details = processDetailStore.getEntry(firstProcessId);
		Assert.assertEquals(1, details.getComponentNidToStampsMap().size());
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(firstTestConceptNid));
		Assert.assertEquals(1, details.getComponentNidToStampsMap().get(firstTestConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstTestConceptNid).contains(565600));

		// Testing INACTIVE Case
		cancelWorkflow(firstProcessId);

		try {
			updater.removeComponentFromWorkflow(firstProcessId, firstTestConceptNid);
			Assert.fail();
		} catch (

		Exception e) {

		}
	}

	private void clearStores() {
		processDetailStore.removeAllEntries();
		processHistoryStore.removeAllEntries();
	}

	protected void setupUserRoles() {
		UserPermission perm = new UserPermission(definitionId, userId, "Editor");
		userPermissionStore.addEntry(perm);

		perm = new UserPermission(definitionId, userId, "Reviewer");
		userPermissionStore.addEntry(perm);

		perm = new UserPermission(definitionId, userId, "Approver");
		userPermissionStore.addEntry(perm);
	}

	private boolean isStartState(UUID defId, String state) {
		for (AvailableAction action : importer.getDefinitionStartActionMap().get(defId)) {
			if (action.getInitialState().equals(state)) {
				return true;
			}
		}

		return false;
	}

	private boolean isEndState(String state, EndWorkflowType type) {
		for (AvailableAction action : importer.getEndWorkflowTypeMap().get(type)) {
			if (action.getOutcomeState().equals(state)) {
				return true;
			}
		}

		return false;
	}

	private AvailableAction getCancelAction() {
		return importer.getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED).iterator().next();
	}

	private AvailableAction getConcludeAction() {
		return importer.getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED).iterator().next();
	}

	private Optional<CommitRecord> createCommitRecord(Integer conNid, Integer semNid, int stampSeq) {
		ConceptSequenceSet conSet;
		SememeSequenceSet semSet;
		StampSequenceSet stampSet = StampSequenceSet.of(stampSeq);

		if (conNid == null) {
			conSet = new ConceptSequenceSet();
		} else {
			conSet = ConceptSequenceSet.of(conNid);
		}

		if (semNid == null) {
			semSet = new SememeSequenceSet();
		} else {
			semSet = SememeSequenceSet.of(semNid);
		}

		CommitRecord cr = new CommitRecord(Instant.ofEpochMilli(new Date().getTime()), stampSet,
				new OpenIntIntHashMap(), conSet, semSet, "");

		return Optional.ofNullable(cr);
	}

	protected UUID createFirstWorkflowProcess(UUID requestedDefinitionId) {
		return createWorkflowProcess(requestedDefinitionId, "Main Process Name", "Main Process Description");
	}

	protected UUID createSecondWorkflowProcess(UUID requestedDefinitionId) {
		return createWorkflowProcess(requestedDefinitionId, "Secondary Process Name", "Secondary Process Description");
	}

	private UUID createWorkflowProcess(UUID requestedDefinitionId, String name, String description) {
		AvailableAction startNodeAction = importer.getDefinitionStartActionMap().get(definitionId).iterator().next();

		// Mimick the initConcluder's create new process
		AbstractStorableWorkflowContents details = new ProcessDetail(requestedDefinitionId, userId,
				new Date().getTime(), ProcessStatus.DEFINED, name, description);
		UUID processId = processDetailStore.addEntry(details);

		// Add Process History with START_STATE-AUTOMATED-EDIT_STATE
		AvailableAction startAdvancement = new AvailableAction(requestedDefinitionId, startNodeAction.getInitialState(),
				startNodeAction.getAction(), startNodeAction.getOutcomeState(), "Automated By System");
		ProcessHistory advanceEntry = new ProcessHistory(processId, userId, new Date().getTime(),
				startAdvancement.getInitialState(), startAdvancement.getAction(), startAdvancement.getOutcomeState(),
				"");
		processHistoryStore.addEntry(advanceEntry);

		return processId;
	}

	protected void cancelWorkflow(UUID processId) {
		try {
			Thread.sleep(1);

			finishWorkflowProcess(processId, cancelAction, userId, "Canceled Workflow", EndWorkflowType.CANCELED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void finishWorkflowProcess(UUID processId, AvailableAction actionToProcess, int userId, String comment,
			EndWorkflowType endType) throws Exception {
		// Mimick the initConcluder's finish workflow process
		ProcessDetail entry = processDetailStore.getEntry(processId);

		if (endType.equals(EndWorkflowType.CANCELED)) {
			entry.setStatus(ProcessStatus.CANCELED);
		} else if (endType.equals(EndWorkflowType.CONCLUDED)) {
			entry.setStatus(ProcessStatus.CONCLUDED);
		}
		entry.setTimeCanceledOrConcluded(new Date().getTime());
		processDetailStore.updateEntry(processId, entry);

		// Only add Cancel state in Workflow if process has already been
		// launched
		ProcessHistory advanceEntry = new ProcessHistory(processId, userId, new Date().getTime(),
				actionToProcess.getInitialState(), actionToProcess.getAction(), actionToProcess.getOutcomeState(),
				comment);
		processHistoryStore.addEntry(advanceEntry);

		if (endType.equals(EndWorkflowType.CANCELED)) {
			// TODO: Handle cancelation store and handle reverting automatically
		}
	}

	protected void executeLaunchWorkflow(UUID processId) {
		try {
			Thread.sleep(1);
			ProcessDetail entry = processDetailStore.getEntry(processId);

			entry.setStatus(ProcessStatus.LAUNCHED);
			entry.setTimeLaunched(new Date().getTime());
			processDetailStore.updateEntry(processId, entry);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void executeSendForReviewAdvancement(UUID processId) {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		try {
			Thread.sleep(1);

			ProcessHistory advanceEntry = new ProcessHistory(processId, entry.getCreatorNid(), new Date().getTime(),
					LAUNCH_STATE, LAUNCH_ACTION, LAUNCH_OUTCOME, LAUNCH_COMMENT);
			processHistoryStore.addEntry(advanceEntry);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void executeSendForApprovalAdvancement(UUID requestedProcessId) {
		try {
			Thread.sleep(1);

			ProcessHistory entry = new ProcessHistory(requestedProcessId, userId, new Date().getTime(),
					SEND_TO_APPROVAL_STATE, SEND_TO_APPROVAL_ACTION, SEND_TO_APPROVAL_OUTCOME,
					SEND_TO_APPROVAL_COMMENT);

			processHistoryStore.addEntry(entry);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void executeRejectReviewAdvancement(UUID requestedProcessId) {
		try {
			Thread.sleep(1);

			ProcessHistory entry = new ProcessHistory(requestedProcessId, userId, new Date().getTime(),
					REJECT_REVIEW_STATE, REJECT_REVIEW_ACTION, REJECT_REVIEW_OUTCOME, REJECT_REVIEW_COMMENT);

			processHistoryStore.addEntry(entry);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

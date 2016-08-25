package gov.vha.isaac.ochre.workflow.provider.crud;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserPermissionContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.ProcessStatus;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities.StartWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;

/**
 * Test the AbstractWorkflowProviderTestPackage class
 * 
 * {@link WorkflowProcessInitializerConcluderTest}. {@link WorkflowStatusAccessorTest}.
 * {@link WorkflowHistoryAccessorTest}.
 * {@link WorkflowActionsPermissionsAccessorTest}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowProviderTestPackage {

	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	/** The bpmn file path. */
	protected static final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/workflow/provider/VetzWorkflow.bpmn2";

	/** The store. */
	protected WorkflowProcessInitializerConcluder initConcluder;

	protected ProcessDetailContentStore processDetailStore;

	protected ProcessHistoryContentStore processHistoryStore;

	protected DefinitionDetailContentStore definitionDetailStore;

	protected UserPermissionContentStore userPermissionStore;

	protected Bpmn2FileImporter importer;

	protected AvailableAction startNodeAction;

	protected static Set<Integer> secondaryConceptsForTesting = new HashSet<>(Arrays.asList(199, 299));

	protected static int mainUserId = 99;
	protected static int secondaryUserId = 999;

	protected static ArrayList<Integer> stampSequenceForTesting = new ArrayList<>(Arrays.asList(11, 12));

	protected static Set<Integer> conceptsForTesting = new HashSet<>(Arrays.asList(55, 56));

	protected static UUID mainDefinitionId;

	protected static UUID secondaryProcessId;

	protected static UUID firstHistoryEntryId;

	protected static UUID secondHistoryEntryId;

	protected static final long TEST_START_TIME = new Date().getTime();
	protected static String createState;
	protected static String createAction;
	protected static String createOutcome;

	protected static final String LAUNCH_STATE = "Ready for Edit";
	protected static final String LAUNCH_ACTION = "Edit";
	protected static final String LAUNCH_OUTCOME = "Ready for Review";
	protected static final String LAUNCH_COMMENT = "Launch Comment";

	protected static final String CONCLUDED_WORKFLOW_COMMENT = "Concluded Workflow";
	protected static final String CANCELED_WORKFLOW_COMMENT = "Canceled Workflow";

	protected static final String SEND_TO_APPROVAL_STATE = "Ready for Review";
	protected static final String SEND_TO_APPROVAL_ACTION = "Review";
	protected static final String SEND_TO_APPROVAL_OUTCOME = "Ready for Approve";
	protected static final String SEND_TO_APPROVAL_COMMENT = "Sending for Approval";
	protected static UUID secondaryHistoryEntryId;

	protected static File DATASTORE_PATH = new File(
			"C:/SW/WCI/Mantech/Workspace/ISAAC/testDB/vets-1.2-SNAPSHOT-all.data/");

	protected void globalSetup(MVStoreMetaContentProvider store) {
		definitionDetailStore = new DefinitionDetailContentStore(store);

		processDetailStore = new ProcessDetailContentStore(store);

		processHistoryStore = new ProcessHistoryContentStore(store);

		userPermissionStore = new UserPermissionContentStore(store);

		if (definitionDetailStore.getAllEntries().size() == 0) {
			importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
			startNodeAction = AbstractWorkflowUtilities.getStartWorkflowTypeMap().get(StartWorkflowType.SINGLE_CASE);
			createState = startNodeAction.getCurrentState();
			createAction = startNodeAction.getAction();
			createOutcome = startNodeAction.getOutcome();
			mainDefinitionId = importer.getCurrentDefinitionId();
		}
		initConcluder = new WorkflowProcessInitializerConcluder(store);

	}

	protected void setupUserRoles() {
		UserPermission perm = new UserPermission(mainDefinitionId, mainUserId, "Editor");
		userPermissionStore.addEntry(perm);

		perm = new UserPermission(mainDefinitionId, secondaryUserId, "Reviewer");
		userPermissionStore.addEntry(perm);

		perm = new UserPermission(mainDefinitionId, mainUserId, "Approver");
		userPermissionStore.addEntry(perm);
	}

	protected boolean timeSinceYesterdayBeforeTomorrow(long time) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		long yesterdayTimestamp = cal.getTimeInMillis();

		cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		long tomorrowTimestamp = cal.getTimeInMillis();

		return time >= yesterdayTimestamp && time <= tomorrowTimestamp;
	}

	protected void close() {
		AbstractWorkflowUtilities.close();
	}

	protected UUID createWorkflowProcess(UUID requestedDefinitionId) {
		// Create new process
		try {
			return initConcluder.createWorkflowProcess(requestedDefinitionId, mainUserId, "Main Process Name", "Main Process Description", StartWorkflowType.SINGLE_CASE);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
			return null;
		}
	}

	protected void executeLaunchAdvancement(UUID processId, boolean updateProcessDetails) {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		
		if (updateProcessDetails) {
    		entry.setStatus(ProcessStatus.LAUNCHED);
    		entry.setTimeLaunched(new Date().getTime());
    		processDetailStore.updateEntry(processId, entry);
		}
		
		ProcessHistory advanceEntry = new ProcessHistory(processId, entry.getCreator(), new Date().getTime(),
		LAUNCH_STATE, LAUNCH_ACTION, LAUNCH_OUTCOME, LAUNCH_COMMENT);
		processHistoryStore.addEntry(advanceEntry);
	}

	protected UUID createSecondaryDefinition() {
		Set<String> roles = new HashSet<>();
		roles.add("Editor");
		roles.add("Reviewer");
		roles.add("Approver");
		DefinitionDetail createdEntry = new DefinitionDetail("BPMN2 ID-X", "JUnit BPMN2", "Testing", "1.0", roles, "Description of BPMN2 ID-X");
		return definitionDetailStore.addEntry(createdEntry);

	}

	protected void addComponentsToProcess(UUID processId) {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		for (Integer con : conceptsForTesting) {
			entry.getComponentToStampMap().put(con, stampSequenceForTesting);
		}
		
		processDetailStore.updateEntry(processId, entry);
	}

	protected UUID createSecondaryWorkflowProcess(UUID requestedDefinitionId, Set<Integer> concepts) {
		// Create new process
		try {
			return initConcluder.createWorkflowProcess(requestedDefinitionId, mainUserId, "Secondary Process Name", "Secondary Process Description", StartWorkflowType.SINGLE_CASE);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
			return null;
		}
	}

	protected UUID executeSendForApprovalAdvancement(UUID requestedProcessId) {
		ProcessHistory entry = new ProcessHistory(requestedProcessId, mainUserId, new Date().getTime(), SEND_TO_APPROVAL_STATE,
				SEND_TO_APPROVAL_ACTION, SEND_TO_APPROVAL_OUTCOME, SEND_TO_APPROVAL_COMMENT);
		return processHistoryStore.addEntry(entry);
	}

	protected void concludeWorkflow(UUID processId) {
		try {
			initConcluder.finishWorkflowProcess(processId, 
					AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CONCLUDED).iterator().next(), 
					mainUserId, "Concluded Workflow", EndWorkflowType.CONCLUDED);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	protected void cancelWorkflow(UUID processId) {
		try {
			initConcluder.finishWorkflowProcess(processId, 
					AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CANCELED).iterator().next(), 
					mainUserId, "Canceled Workflow", EndWorkflowType.CANCELED);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	protected void assertHistoryForProcess(SortedSet<ProcessHistory> allProcessHistory, UUID processId, int numberOfEntries) {
		Assert.assertEquals(numberOfEntries, allProcessHistory.size());

		int counter = 0;
		for (ProcessHistory entry : allProcessHistory) {
			if (counter == 0) {
				assertCreationHistory(entry, processId);
			} else if (counter == 1) {
				assertLaunchHistory(entry, processId);
			} else if (counter == 2) {
				assertSendToApproverHistory(entry, processId);
			}

			counter++;
			
			if (counter > numberOfEntries) {
				break;
		}
		}
	}

	private void assertSendToApproverHistory(ProcessHistory entry, UUID processId) {
		Assert.assertEquals(processId, entry.getProcessId());
		Assert.assertEquals(mainUserId, entry.getWorkflowUser());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
		Assert.assertEquals(SEND_TO_APPROVAL_STATE, entry.getState());
		Assert.assertEquals(SEND_TO_APPROVAL_ACTION, entry.getAction());
		Assert.assertEquals(SEND_TO_APPROVAL_OUTCOME, entry.getOutcome());
		Assert.assertEquals(SEND_TO_APPROVAL_COMMENT, entry.getComment());
	}

	protected void assertLaunchHistory(ProcessHistory entry, UUID processId) {
		Assert.assertEquals(processId, entry.getProcessId());
		Assert.assertEquals(mainUserId, entry.getWorkflowUser());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
		Assert.assertEquals(LAUNCH_STATE, entry.getState());
		Assert.assertEquals(LAUNCH_ACTION, entry.getAction());
		Assert.assertEquals(LAUNCH_OUTCOME, entry.getOutcome());
		Assert.assertEquals(LAUNCH_COMMENT, entry.getComment());
	}

	protected void assertCreationHistory(ProcessHistory entry, UUID processId) {
		Assert.assertEquals(processId, entry.getProcessId());
		Assert.assertEquals(mainUserId, entry.getWorkflowUser());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
		Assert.assertEquals(createState, entry.getState());
		Assert.assertEquals(createAction, entry.getAction());
		Assert.assertEquals(createOutcome, entry.getOutcome());
		Assert.assertEquals("", entry.getComment());
	}
	
	protected void assertCancelHistory(ProcessHistory entry, UUID processId) {
		Assert.assertEquals(processId, entry.getProcessId());
		Assert.assertEquals(mainUserId, entry.getWorkflowUser());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
		
		AvailableAction cancelAction = 
				AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CANCELED).iterator().next();
		Assert.assertEquals(cancelAction.getCurrentState(), entry.getState());
		Assert.assertEquals(cancelAction.getAction(), entry.getAction());
		Assert.assertEquals(cancelAction.getOutcome(), entry.getOutcome());
		Assert.assertEquals(CANCELED_WORKFLOW_COMMENT, entry.getComment());
	}

	protected void assertConcludeHistory(ProcessHistory entry, UUID processId) {
		Assert.assertEquals(processId, entry.getProcessId());
		Assert.assertEquals(mainUserId, entry.getWorkflowUser());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
		
		AvailableAction concludeAction = 
				AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CONCLUDED).iterator().next();
		Assert.assertEquals(concludeAction.getCurrentState(), entry.getState());
		Assert.assertEquals(concludeAction.getAction(), entry.getAction());
		Assert.assertEquals(concludeAction.getOutcome(), entry.getOutcome());
		Assert.assertEquals(CONCLUDED_WORKFLOW_COMMENT, entry.getComment());
	}
}

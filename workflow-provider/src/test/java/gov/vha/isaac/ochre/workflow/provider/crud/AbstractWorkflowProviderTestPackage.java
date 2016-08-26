package gov.vha.isaac.ochre.workflow.provider.crud;

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
import gov.vha.isaac.metacontent.workflow.AvailableActionContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserPermissionContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.ProcessStatus;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities.StartWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;

/**
 * Test the AbstractWorkflowProviderTestPackage class
 * 
 * {@link WorkflowProcessInitializerConcluderTest}.
 * {@link WorkflowStatusAccessorTest}. {@link WorkflowHistoryAccessorTest}.
 * {@link WorkflowActionsPermissionsAccessorTest}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowProviderTestPackage {

	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	/** The bpmn file path. */
	private static final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/workflow/provider/StaticUnitTestingDefinition.bpmn2";

	/** The store. */
	protected ProcessDetailContentStore processDetailStore;
	protected ProcessHistoryContentStore processHistoryStore;
	protected DefinitionDetailContentStore definitionDetailStore;
	protected UserPermissionContentStore userPermissionStore;
	protected AvailableActionContentStore availableActionStore;

	/*
	 * Defined by importing definition and static throughout testclasses to
	 * simplify process
	 */
	protected static UUID mainDefinitionId;
	private static String createState;
	private static String createAction;
	private static String createOutcome;
	private static String createRole = "Automated By System";

	/* Constants throughout testclasses to simplify process */
	private static final long TEST_START_TIME = new Date().getTime();

	protected static final int firstUserId = 99;
	protected static final int secondUserId = 999;
	protected static final ArrayList<Integer> stampSequenceForTesting = new ArrayList<>(Arrays.asList(11, 12));
	protected static final Set<Integer> conceptsForTesting = new HashSet<>(Arrays.asList(55, 56));

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

	protected void globalSetup(MVStoreMetaContentProvider store) {
		definitionDetailStore = new DefinitionDetailContentStore(store);
		processDetailStore = new ProcessDetailContentStore(store);
		processHistoryStore = new ProcessHistoryContentStore(store);
		userPermissionStore = new UserPermissionContentStore(store);
		availableActionStore = new AvailableActionContentStore(store);
		
		if (definitionDetailStore.getAllEntries().size() == 0) {
			Bpmn2FileImporter importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
			AvailableAction startNodeAction = AbstractWorkflowUtilities.getStartWorkflowTypeMap()
					.get(StartWorkflowType.SINGLE_CASE);
			createState = startNodeAction.getInitialState();
			createAction = startNodeAction.getAction();
			createOutcome = startNodeAction.getOutcomeState();
			mainDefinitionId = importer.getCurrentDefinitionId();
		}

		processDetailStore.removeAllEntries();
		processHistoryStore.removeAllEntries();
		userPermissionStore.removeAllEntries();
	}

	protected void setupUserRoles() {
		UserPermission perm = new UserPermission(mainDefinitionId, firstUserId, "Editor");
		userPermissionStore.addEntry(perm);

		perm = new UserPermission(mainDefinitionId, secondUserId, "Reviewer");
		userPermissionStore.addEntry(perm);

		perm = new UserPermission(mainDefinitionId, firstUserId, "Approver");
		userPermissionStore.addEntry(perm);
	}

	protected UUID createFirstWorkflowProcess(UUID requestedDefinitionId) {
		return createWorkflowProcess(requestedDefinitionId, "Main Process Name", "Main Process Description");
	}

	protected UUID createSecondWorkflowProcess(UUID requestedDefinitionId) {
		return createWorkflowProcess(requestedDefinitionId, "Secondary Process Name", "Secondary Process Description");
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

			ProcessHistory advanceEntry = new ProcessHistory(processId, entry.getCreator(), new Date().getTime(),
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

			ProcessHistory entry = new ProcessHistory(requestedProcessId, firstUserId, new Date().getTime(),
					SEND_TO_APPROVAL_STATE, SEND_TO_APPROVAL_ACTION, SEND_TO_APPROVAL_OUTCOME, SEND_TO_APPROVAL_COMMENT);
			
			processHistoryStore.addEntry(entry);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void  executeRejectReviewAdvancement(UUID requestedProcessId) {
		try {
			Thread.sleep(1);

			ProcessHistory entry = new ProcessHistory(requestedProcessId, firstUserId, new Date().getTime(),
					REJECT_REVIEW_STATE, REJECT_REVIEW_ACTION, REJECT_REVIEW_OUTCOME, REJECT_REVIEW_COMMENT);
			
			processHistoryStore.addEntry(entry);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void concludeWorkflow(UUID processId) {
		try {
			Thread.sleep(1);
			
			finishWorkflowProcess(processId,
					AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CONCLUDED).iterator().next(),
					firstUserId, "Concluded Workflow", EndWorkflowType.CONCLUDED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void cancelWorkflow(UUID processId) {
		try {
			Thread.sleep(1);
			
			finishWorkflowProcess(processId,
					AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CANCELED).iterator().next(),
					firstUserId, "Canceled Workflow", EndWorkflowType.CANCELED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void addComponentsToProcess(UUID processId) {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		for (Integer con : conceptsForTesting) {
			entry.getComponentToStampMap().put(con, stampSequenceForTesting);
		}
		
		processDetailStore.updateEntry(processId, entry);
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

	protected void assertHistoryForProcess(SortedSet<ProcessHistory> allProcessHistory, UUID processId) {
		int counter = 0;
		for (ProcessHistory entry : allProcessHistory) {
			if (counter == 0) {
				Assert.assertEquals(processId, entry.getProcessId());
				Assert.assertEquals(firstUserId, entry.getWorkflowUser());
				Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
				Assert.assertEquals(createState, entry.getInitialState());
				Assert.assertEquals(createAction, entry.getAction());
				Assert.assertEquals(createOutcome, entry.getOutcomeState());
				Assert.assertEquals("", entry.getComment());
			} else if (counter == 1) {
				Assert.assertEquals(processId, entry.getProcessId());
				Assert.assertEquals(firstUserId, entry.getWorkflowUser());
				Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
				Assert.assertEquals(LAUNCH_STATE, entry.getInitialState());
				Assert.assertEquals(LAUNCH_ACTION, entry.getAction());
				Assert.assertEquals(LAUNCH_OUTCOME, entry.getOutcomeState());
				Assert.assertEquals(LAUNCH_COMMENT, entry.getComment());
			} else if (counter == 2) {
				Assert.assertEquals(processId, entry.getProcessId());
				Assert.assertEquals(firstUserId, entry.getWorkflowUser());
				Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
				Assert.assertEquals(SEND_TO_APPROVAL_STATE, entry.getInitialState());
				Assert.assertEquals(SEND_TO_APPROVAL_ACTION, entry.getAction());
				Assert.assertEquals(SEND_TO_APPROVAL_OUTCOME, entry.getOutcomeState());
				Assert.assertEquals(SEND_TO_APPROVAL_COMMENT, entry.getComment());
			}

			counter++;
		}
	}

	protected void assertCancelHistory(ProcessHistory entry, UUID processId) {
		Assert.assertEquals(processId, entry.getProcessId());
		Assert.assertEquals(firstUserId, entry.getWorkflowUser());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());

		AvailableAction cancelAction = AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CANCELED)
				.iterator().next();
		Assert.assertEquals(cancelAction.getInitialState(), entry.getInitialState());
		Assert.assertEquals(cancelAction.getAction(), entry.getAction());
		Assert.assertEquals(cancelAction.getOutcomeState(), entry.getOutcomeState());
		Assert.assertEquals(CANCELED_WORKFLOW_COMMENT, entry.getComment());
	}

	protected void assertConcludeHistory(ProcessHistory entry, UUID processId) {
		Assert.assertEquals(processId, entry.getProcessId());
		Assert.assertEquals(firstUserId, entry.getWorkflowUser());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());

		AvailableAction concludeAction = AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CONCLUDED)
				.iterator().next();
		Assert.assertEquals(concludeAction.getInitialState(), entry.getInitialState());
		Assert.assertEquals(concludeAction.getAction(), entry.getAction());
		Assert.assertEquals(concludeAction.getOutcomeState(), entry.getOutcomeState());
		Assert.assertEquals(CONCLUDED_WORKFLOW_COMMENT, entry.getComment());
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

	private UUID createWorkflowProcess(UUID requestedDefinitionId, String name, String description) {
		// Mimick the initConcluder's create new process
		StorableWorkflowContents details = new ProcessDetail(requestedDefinitionId, firstUserId, new Date().getTime(),
				ProcessStatus.DEFINED, name, description);
		UUID processId = processDetailStore.addEntry(details);

		// Add Process History with START_STATE-AUTOMATED-EDIT_STATE
		AvailableAction startAdvancement = new AvailableAction(requestedDefinitionId, createState, createAction,
				createOutcome, createRole);
		ProcessHistory advanceEntry = new ProcessHistory(processId, firstUserId, new Date().getTime(),
				startAdvancement.getInitialState(), startAdvancement.getAction(), startAdvancement.getOutcomeState(),
				"");
		processHistoryStore.addEntry(advanceEntry);

		return processId;
	}


	protected UUID createSecondaryDefinition() {
		Set<String> roles = new HashSet<>();
		roles.add("Editor");
		roles.add("Reviewer");
		roles.add("Approver");
		DefinitionDetail createdEntry = new DefinitionDetail("BPMN2 ID-X", "JUnit BPMN2", "Testing", "1.0", roles,
				"Description of BPMN2 ID-X");
		UUID defId = definitionDetailStore.addEntry(createdEntry);
		
		// Duplicate Permissions
		Set<UserPermission> permsToAdd = new HashSet<>();
		for (UserPermission perm : userPermissionStore.getAllEntries()) {
			permsToAdd.add(new UserPermission(defId, perm.getUser(), perm.getRole()));
		}
		
		for (UserPermission perm : permsToAdd) {
			userPermissionStore.addEntry(perm);
		}
		
		// Duplicate AvailableActions
		Set<AvailableAction> actionsToAdd = new HashSet<>();
		for (AvailableAction action : availableActionStore.getAllEntries()) {
			actionsToAdd.add(new AvailableAction(defId, action.getInitialState(), action.getAction(), action.getOutcomeState(), action.getRole()));
		}
		
		for (AvailableAction action : actionsToAdd) {
			availableActionStore.addEntry(action);
		}

		return defId;
	}
}

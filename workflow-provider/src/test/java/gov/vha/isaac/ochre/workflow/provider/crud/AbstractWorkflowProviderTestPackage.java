package gov.vha.isaac.ochre.workflow.provider.crud;

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

import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.workflow.model.contents.AvailableAction;
import gov.vha.isaac.ochre.workflow.model.contents.DefinitionDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.model.contents.UserPermission;
import gov.vha.isaac.ochre.workflow.provider.WorkflowProvider;

/**
 * Test the AbstractWorkflowProviderTestPackage class
 * 
 * {@link WorkflowProcessInitializerConcluderTest}.
 * {@link WorkflowAccessorTest}.
 * {@link WorkflowUpdaterTest}.
 * 
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowProviderTestPackage {

	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	/** The bpmn file path. */
	protected static final String BPMN_FILE_PATH = "/gov/vha/isaac/ochre/workflow/provider/StaticUnitTestingDefinition.bpmn2";

	protected static AvailableAction concludeAction;
	protected static AvailableAction cancelAction;

	/*
	 * Defined by importing definition and static throughout testclasses to
	 * simplify process
	 */
	protected static UUID mainDefinitionId;
	private static String createState;
	private static String createAction;
	private static String createOutcome;
	private static String createRole = "Automated By System";

	protected static WorkflowProvider wp_;

	/* Constants throughout testclasses to simplify process */
	private static final long TEST_START_TIME = new Date().getTime();

	protected static final UUID firstUserId = UUID.randomUUID();
	protected static final UUID secondUserId = UUID.randomUUID();
	protected static final Set<Integer> conceptsForTesting = new HashSet<>(Arrays.asList(-55, -56));

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

	protected static void globalSetup() {
		wp_ = LookupService.get().getService(WorkflowProvider.class);

		mainDefinitionId = wp_.getBPMNInfo().getDefinitionId();

		AvailableAction startNodeAction = wp_.getBPMNInfo().getDefinitionStartActionMap().get(mainDefinitionId)
				.iterator().next();
		createState = startNodeAction.getInitialState();
		createAction = startNodeAction.getAction();
		createOutcome = startNodeAction.getOutcomeState();

		cancelAction = wp_.getBPMNInfo().getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED).iterator().next();
		concludeAction = wp_.getBPMNInfo().getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED).iterator().next();
	}

	protected static void setupUserRoles() {
		UserPermission perm = new UserPermission(mainDefinitionId, firstUserId, "Editor");
		wp_.getUserPermissionStore().add(perm);

		perm = new UserPermission(mainDefinitionId, secondUserId, "Reviewer");
		wp_.getUserPermissionStore().add(perm);

		perm = new UserPermission(mainDefinitionId, firstUserId, "Approver");
		wp_.getUserPermissionStore().add(perm);
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
			ProcessDetail entry = wp_.getProcessDetailStore().get(processId);

			entry.setStatus(ProcessStatus.LAUNCHED);
			entry.setTimeLaunched(new Date().getTime());
			wp_.getProcessDetailStore().put(processId, entry);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected void executeSendForReviewAdvancement(UUID processId) {
		ProcessDetail entry = wp_.getProcessDetailStore().get(processId);
		try {
			Thread.sleep(1);

			int historySequence = 1;
			if (wp_.getWorkflowAccessor().getProcessHistory(processId) != null) {
				historySequence = wp_.getWorkflowAccessor().getProcessHistory(processId).last().getHistorySequence();
			}
			ProcessHistory advanceEntry = new ProcessHistory(processId, entry.getCreatorId(), new Date().getTime(),
					LAUNCH_STATE, LAUNCH_ACTION, LAUNCH_OUTCOME, LAUNCH_COMMENT, historySequence + 1);
			wp_.getProcessHistoryStore().add(advanceEntry);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected void executeSendForApprovalAdvancement(UUID requestedProcessId) {
		try {
			Thread.sleep(1);

			int historySequence = 1;
			if (wp_.getWorkflowAccessor().getProcessHistory(requestedProcessId) != null) {
				historySequence = wp_.getWorkflowAccessor().getProcessHistory(requestedProcessId).last().getHistorySequence();
			}
			ProcessHistory entry = new ProcessHistory(requestedProcessId, firstUserId, new Date().getTime(),
					SEND_TO_APPROVAL_STATE, SEND_TO_APPROVAL_ACTION, SEND_TO_APPROVAL_OUTCOME,
					SEND_TO_APPROVAL_COMMENT, historySequence + 1);

			wp_.getProcessHistoryStore().add(entry);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected void executeRejectReviewAdvancement(UUID requestedProcessId) {
		try {
			Thread.sleep(1);

			int historySequence = 1;
			if (wp_.getWorkflowAccessor().getProcessHistory(requestedProcessId) != null) {
				historySequence = wp_.getWorkflowAccessor().getProcessHistory(requestedProcessId).last().getHistorySequence();
			}
			ProcessHistory entry = new ProcessHistory(requestedProcessId, firstUserId, new Date().getTime(),
					REJECT_REVIEW_STATE, REJECT_REVIEW_ACTION, REJECT_REVIEW_OUTCOME, REJECT_REVIEW_COMMENT, historySequence + 1);

			wp_.getProcessHistoryStore().add(entry);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected void concludeWorkflow(UUID processId) {
		try {
			Thread.sleep(1);

			finishWorkflowProcess(processId, concludeAction, firstUserId, "Concluded Workflow",
					EndWorkflowType.CONCLUDED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void cancelWorkflow(UUID processId) {
		try {
			Thread.sleep(1);

			finishWorkflowProcess(processId, cancelAction, firstUserId, "Canceled Workflow", EndWorkflowType.CANCELED);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void addComponentsToProcess(UUID processId, long time) {
		ProcessDetail entry = wp_.getProcessDetailStore().get(processId);
		for (Integer con : conceptsForTesting) {
			entry.getComponentToInitialEditMap().put(con, time);
		}

		wp_.getProcessDetailStore().put(processId, entry);
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
				Assert.assertEquals(firstUserId, entry.getUserId());
				Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
				Assert.assertEquals(createState, entry.getInitialState());
				Assert.assertEquals(createAction, entry.getAction());
				Assert.assertEquals(createOutcome, entry.getOutcomeState());
				Assert.assertEquals("", entry.getComment());
			} else if (counter == 1) {
				Assert.assertEquals(processId, entry.getProcessId());
				Assert.assertEquals(firstUserId, entry.getUserId());
				Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());
				Assert.assertEquals(LAUNCH_STATE, entry.getInitialState());
				Assert.assertEquals(LAUNCH_ACTION, entry.getAction());
				Assert.assertEquals(LAUNCH_OUTCOME, entry.getOutcomeState());
				Assert.assertEquals(LAUNCH_COMMENT, entry.getComment());
			} else if (counter == 2) {
				Assert.assertEquals(processId, entry.getProcessId());
				Assert.assertEquals(firstUserId, entry.getUserId());
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
		Assert.assertEquals(firstUserId, entry.getUserId());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());

		Assert.assertEquals(cancelAction.getInitialState(), entry.getInitialState());
		Assert.assertEquals(cancelAction.getAction(), entry.getAction());
		Assert.assertEquals(cancelAction.getOutcomeState(), entry.getOutcomeState());
		Assert.assertEquals(CANCELED_WORKFLOW_COMMENT, entry.getComment());
	}

	protected void assertConcludeHistory(ProcessHistory entry, UUID processId) {
		Assert.assertEquals(processId, entry.getProcessId());
		Assert.assertEquals(firstUserId, entry.getUserId());
		Assert.assertTrue(TEST_START_TIME < entry.getTimeAdvanced());

		Assert.assertEquals(concludeAction.getInitialState(), entry.getInitialState());
		Assert.assertEquals(concludeAction.getAction(), entry.getAction());
		Assert.assertEquals(concludeAction.getOutcomeState(), entry.getOutcomeState());
		Assert.assertEquals(CONCLUDED_WORKFLOW_COMMENT, entry.getComment());
	}

	private void finishWorkflowProcess(UUID processId, AvailableAction actionToProcess, UUID userId, String comment,
			EndWorkflowType endType) throws Exception {
		// Mimick the initConcluder's finish workflow process
		ProcessDetail entry = wp_.getProcessDetailStore().get(processId);

		if (endType.equals(EndWorkflowType.CANCELED)) {
			entry.setStatus(ProcessStatus.CANCELED);
		} else if (endType.equals(EndWorkflowType.CONCLUDED)) {
			entry.setStatus(ProcessStatus.CONCLUDED);
		}
		entry.setTimeCanceledOrConcluded(new Date().getTime());
		wp_.getProcessDetailStore().put(processId, entry);

		// Only add Cancel state in Workflow if process has already been
		// launched
		int historySequence = 1;
		if (wp_.getWorkflowAccessor().getProcessHistory(processId) != null) {
			historySequence = wp_.getWorkflowAccessor().getProcessHistory(processId).last().getHistorySequence();
		}
		ProcessHistory advanceEntry = new ProcessHistory(processId, userId, new Date().getTime(),
				actionToProcess.getInitialState(), actionToProcess.getAction(), actionToProcess.getOutcomeState(),
				comment, historySequence + 1);
		wp_.getProcessHistoryStore().add(advanceEntry);

		if (endType.equals(EndWorkflowType.CANCELED)) {
			// TODO: Handle cancelation store and handle reverting automatically
		}
	}

	private UUID createWorkflowProcess(UUID requestedDefinitionId, String name, String description) {
		// Mimick the initConcluder's create new process
		ProcessDetail details = new ProcessDetail(requestedDefinitionId, firstUserId, new Date().getTime(),
				ProcessStatus.DEFINED, name, description);
		UUID processId = wp_.getProcessDetailStore().add(details);

		// Add Process History with START_STATE-AUTOMATED-EDIT_STATE
		AvailableAction startAdvancement = new AvailableAction(requestedDefinitionId, createState, createAction,
				createOutcome, createRole);
		ProcessHistory advanceEntry = new ProcessHistory(processId, firstUserId, new Date().getTime(),
				startAdvancement.getInitialState(), startAdvancement.getAction(), startAdvancement.getOutcomeState(),
				"", 0);
		wp_.getProcessHistoryStore().add(advanceEntry);

		return processId;
	}

	protected UUID createSecondaryDefinition() {
		Set<String> roles = new HashSet<>();
		roles.add("Editor");
		roles.add("Reviewer");
		roles.add("Approver");
		DefinitionDetail createdEntry = new DefinitionDetail("BPMN2 ID-X", "JUnit BPMN2", "Testing", "1.0", roles,
				"Description of BPMN2 ID-X");
		UUID defId = wp_.getDefinitionDetailStore().add(createdEntry);

		// Duplicate Permissions
		Set<UserPermission> permsToAdd = new HashSet<>();
		for (UserPermission perm : wp_.getUserPermissionStore().values()) {
			permsToAdd.add(new UserPermission(defId, perm.getUserId(), perm.getRole()));
		}

		for (UserPermission perm : permsToAdd) {
			wp_.getUserPermissionStore().add(perm);
		}

		// Duplicate AvailableActions
		Set<AvailableAction> actionsToAdd = new HashSet<>();
		for (AvailableAction action : wp_.getAvailableActionStore().values()) {
			actionsToAdd.add(new AvailableAction(defId, action.getInitialState(), action.getAction(),
					action.getOutcomeState(), action.getRole()));
		}

		for (AvailableAction action : actionsToAdd) {
			wp_.getAvailableActionStore().add(action);
		}

		return defId;
	}

	protected boolean advanceWorkflow(UUID processId, UUID userId, String actionRequested, String comment)
			throws Exception {
		return wp_.getWorkflowUpdater().advanceWorkflow(processId, userId, actionRequested, comment, null);
	}

	protected void endWorkflowProcess(UUID processId, AvailableAction actionToProcess, UUID userId, String comment,
			EndWorkflowType endType) throws Exception {
		wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, actionToProcess, userId, comment,
				endType, null);

	}
}

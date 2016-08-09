package gov.vha.isaac.ochre.workflow.provider.crud;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
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
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;

/**
 * Test the AbstractWorkflowProviderTestPackage class
 * 
 * {@link WorkflowInitializerConcluderTest}. {@link WorkflowStatusAccessorTest}.
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
	protected WorkflowInitializerConcluder initConcluder;

	protected ProcessDetailContentStore processDetailStore;

	protected ProcessHistoryContentStore processHistoryStore;

	protected DefinitionDetailContentStore definitionDetailStore;

	protected UserPermissionContentStore userPermissionStore;

	protected Bpmn2FileImporter importer;

	protected AvailableAction startNodeAction;

	protected static Set<Integer> secondaryConceptsForTesting = new HashSet<>(Arrays.asList(199, 299));

	protected static int mainUserId = 99;
	protected static int secondaryUserId = 999;

	protected static ArrayList<Integer> stampSequenceForTesting = new ArrayList<>(Arrays.asList(11, 12, 13));

	protected static Set<Integer> conceptsForTesting = new HashSet<>(Arrays.asList(55, 56, 57));

	protected static UUID mainDefinitionId;

	protected static UUID mainProcessId;

	protected static UUID secondaryProcessId;

	protected static UUID firstHistoryEntryId;

	protected static UUID secondHistoryEntryId;

	protected static final long launchHistoryTimestamp = new Date().getTime();
	protected static String launchState;
	protected static String launchAction;
	protected static String launchOutcome;
	protected static final String launchComment = "Automated Launch Advancement";

	protected static final long firstHistoryTimestamp = launchHistoryTimestamp + 10000;
	protected static final String firstState = "Ready for Edit";
	protected static final String firstAction = "Edit";
	protected static final String firstOutcome = "Ready for Review";
	protected static final String firstComment = "Comment #1";

	protected static long secondHistoryTimestamp = launchHistoryTimestamp + 20000;
	protected static final String secondState = "Ready for Review";
	protected static final String secondAction = "Review";
	protected static final String secondOutcome = "Ready for Approve";
	protected static final String secondComment = "Comment #2";
	protected static UUID secondaryHistoryEntryId;

	protected static File DATASTORE_PATH = new File(
			"C:/SW/WCI/Mantech/Workspace/ISAAC/testDB/vets-1.2-SNAPSHOT-all.data/");

	protected void globalSetup(MVStoreMetaContentProvider store) {
		definitionDetailStore = new DefinitionDetailContentStore(store);

		processDetailStore = new ProcessDetailContentStore(store);

		processHistoryStore = new ProcessHistoryContentStore(store);

		userPermissionStore = new UserPermissionContentStore(store);

		// if (firstTimeCreatingStore) {
		if (definitionDetailStore.getAllEntries().size() == 0) {
			importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
			startNodeAction = importer.getStartNodeAction();
			launchState = startNodeAction.getCurrentState();
			launchAction = startNodeAction.getAction();
			launchOutcome = startNodeAction.getOutcome();
			mainDefinitionId = importer.getCurrentDefinitionId();
		}
		initConcluder = new WorkflowInitializerConcluder(store);

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

	protected void launchWorkflow(UUID processId) {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		entry.setProcessStatus(ProcessStatus.LAUNCHED);
		processDetailStore.updateEntry(processId, entry);

		ProcessHistory advanceEntry = new ProcessHistory(processId, entry.getCreator(), launchHistoryTimestamp,
				launchState, launchAction, launchOutcome, launchComment);
		processHistoryStore.addEntry(advanceEntry);
	}

	protected UUID createSecondaryDefinition() {
		Set<String> roles = new HashSet<>();
		roles.add("Editor");
		roles.add("Reviewer");
		roles.add("Approver");
		DefinitionDetail createdEntry = new DefinitionDetail("BPMN2 ID-X", "JUnit BPMN2", "Testing", "1.0", roles);
		return definitionDetailStore.addEntry(createdEntry);

	}

	protected void createMainWorkflowProcess(UUID requestedDefinitionId) {
		// Create new process
		try {
			mainProcessId = initConcluder.defineWorkflow(requestedDefinitionId, conceptsForTesting,
					stampSequenceForTesting, mainUserId, SubjectMatter.CONCEPT);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	protected UUID createSecondaryWorkflowProcess(UUID requestedDefinitionId, Set<Integer> concepts) {
		// Create new process
		try {
			return initConcluder.defineWorkflow(requestedDefinitionId, concepts,
					stampSequenceForTesting, mainUserId, SubjectMatter.CONCEPT);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
			return null;
		}
	}

	protected UUID executeInitialAdvancement(UUID requestedProcessId) {
		ProcessHistory entry = new ProcessHistory(requestedProcessId, mainUserId, firstHistoryTimestamp, firstState,
				firstAction, firstOutcome, firstComment);
		return processHistoryStore.addEntry(entry);
	}

	protected UUID executeSecondAdvancement(UUID requestedProcessId) {
		ProcessHistory entry = new ProcessHistory(requestedProcessId, mainUserId, secondHistoryTimestamp, secondState,
				secondAction, secondOutcome, secondComment);
		return processHistoryStore.addEntry(entry);
	}

	protected void concludeWorkflow(UUID processId, int workflowUser) {
		try {
			initConcluder.concludeWorkflow(processId, workflowUser);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}

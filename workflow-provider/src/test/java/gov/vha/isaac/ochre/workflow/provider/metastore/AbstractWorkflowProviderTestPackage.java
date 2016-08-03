package gov.vha.isaac.ochre.workflow.provider.metastore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.DBLocator;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;

/**
 * Test the AbstractWorkflowProviderTestPackage class
 * 
 * {@link WorkflowInitializerConcluder}. {@link WorkflowStatusAccessorTest}.
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

	protected Bpmn2FileImporter importer;

	protected static Set<Integer> secondaryConceptsForTesting = new HashSet<>(Arrays.asList(199, 299));

	protected static int userId = 99;

	protected static List<Integer> stampSequenceForTesting = Arrays.asList(11, 12, 13);

	protected static Set<Integer> conceptsForTesting = new HashSet<>(Arrays.asList(55, 56, 57));

	protected static UUID mainDefinitionId;

	protected static UUID mainProcessId;

	protected static UUID secondaryProcessId;

	protected static UUID firstHistoryEntryId;

	protected static UUID secondHistoryEntryId;

	protected static final long firstHistoryTimestamp = new Date().getTime();

	protected static final String firstState = "READY_TO_START";
	protected static final String firstAction = "EDIT";
	protected static final String firstOutcome = "READY_TO_EDIT";
	protected static final String firstComment = "Comment #1";

	protected static long secondHistoryTimestamp;

	protected static final String secondState = "READY_TO_EDIT";

	protected static final String secondAction = "REVIEW";

	protected static final String secondOutcome = "READY_TO_APPROV";

	protected static final String secondComment = "Comment #2";
	protected static UUID secondaryHistoryEntryId;

	protected static UUID secondDefinitionId;

	protected static File DATASTORE_PATH = new File(
			"C:/SW/WCI/Mantech/Workspace/ISAAC/testDB/vets-1.2-SNAPSHOT-all.data/");

	protected void globalSetup(boolean firstTimeCreatingStore, MVStoreMetaContentProvider store) {
		definitionDetailStore = new DefinitionDetailContentStore(store);

		processDetailStore = new ProcessDetailContentStore(store);

		processHistoryStore = new ProcessHistoryContentStore(store);

		// if (firstTimeCreatingStore) {
		if (mainDefinitionId == null) {
			importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
			mainDefinitionId = definitionDetailStore.getAllEntries().iterator().next().getId();
		}
		initConcluder = new WorkflowInitializerConcluder(store);
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
		initConcluder.close();
	}

	protected void setupDB() throws Exception {

		LookupService.get();

		File dataStoreLocation = DBLocator.findDBFolder(DATASTORE_PATH);

		if (!dataStoreLocation.exists()) {
			throw new IOException("Couldn't find a data store from the input of '"
					+ dataStoreLocation.getAbsoluteFile().getAbsolutePath() + "'");
		}
		if (!dataStoreLocation.isDirectory()) {
			throw new IOException(
					"The specified data store: '" + dataStoreLocation.getAbsolutePath() + "' is not a folder");
		}

		Path path = dataStoreLocation.toPath();
		ConfigurationService serv = LookupService.getService(ConfigurationService.class);
		serv.setDataStoreFolderPath(path);
		logger.info("  Setup AppContext, data store location = " + dataStoreLocation.getCanonicalPath());

		LookupService.startupIsaac();

		logger.info("Done setting up ISAAC");
	}

	protected void shutdownDB() throws Exception {
		LookupService.shutdownIsaac();

		logger.info("ISAAC shut down");
	}

	protected void launchWorkflow(UUID processId) {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		entry.setProcessStatus(ProcessStatus.LAUNCHED);
		processDetailStore.updateEntry(processId, entry);
	}

	protected void createSecondaryDefinitionWithSingleAdvancement() {
		Set<String> roles = new HashSet<>();
		roles.add("Editor");
		roles.add("Reviewer");
		DefinitionDetail createdEntry = new DefinitionDetail("BPMN2 ID-X", "JUnit BPMN2", "Testing", "1.0", roles);
		secondDefinitionId = definitionDetailStore.addEntry(createdEntry);
		initializeSecondaryWorkflow(secondDefinitionId, secondaryConceptsForTesting);
		secondaryHistoryEntryId = advanceInitialWorkflow(secondaryProcessId);

	}

	protected void initializeMainWorkflow(UUID requestedDefinitionId) {
		// Create new process
		mainProcessId = initConcluder.defineWorkflow(requestedDefinitionId, conceptsForTesting, stampSequenceForTesting,
				userId, SubjectMatter.CONCEPT);

	}

	protected void initializeSecondaryWorkflow(UUID requestedDefinitionId, Set<Integer> requestedConceptForTesting) {
		// Create new process
		secondaryProcessId = initConcluder.defineWorkflow(requestedDefinitionId, requestedConceptForTesting,
				stampSequenceForTesting, userId, SubjectMatter.CONCEPT);

	}

	protected UUID advanceInitialWorkflow(UUID requestedProcessId) {
		ProcessHistory entry = new ProcessHistory(requestedProcessId, userId, firstHistoryTimestamp, firstState,
				firstAction, firstOutcome, firstComment);
		return processHistoryStore.addEntry(entry);
	}

	protected UUID advanceSecondWorkflow(UUID requestedProcessId) {
		secondHistoryTimestamp = new Date().getTime();

		ProcessHistory entry = new ProcessHistory(requestedProcessId, userId, secondHistoryTimestamp, secondState,
				secondAction, secondOutcome, secondComment);
		return processHistoryStore.addEntry(entry);
	}

}

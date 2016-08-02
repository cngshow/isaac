package gov.vha.isaac.ochre.workflow.provider.metastore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;

public abstract class WorkflowProviderTestPackage {

	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	/** The bpmn file path. */
	protected static final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/workflow/provider/VetzWorkflow.bpmn2";

	/** The store. */
	protected static MVStoreMetaContentProvider store;

	protected static Bpmn2FileImporter importer;

}

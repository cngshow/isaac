/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.workflow.provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.AvailableActionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserPermissionWorkflowContentStore;

/**
 * Abstract class for higher-level workflow routines
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowUpdater}
 * {@link Bpmn2FileImporter} {@link WorkflowInitializerConcluder}
 * {@link WorkflowAdvancementAccessor} {@link WorkflowHistoryAccessor}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowUtilities {
	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	/** The user workflow permission store. */
	protected UserPermissionWorkflowContentStore userPermissionStore;

	/** The available action store. */
	protected AvailableActionWorkflowContentStore availableActionStore;

	/** The definition detail store. */
	protected DefinitionDetailWorkflowContentStore definitionDetailStore;

	/** The process detail store. */
	protected ProcessDetailWorkflowContentStore processDetailStore;

	/** The process history store. */
	protected ProcessHistoryContentStore processHistoryStore;

	/** The store. */
	protected static MVStoreMetaContentProvider store = null;

	/**
	 * Instantiates a new abstract workflow utilities.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public AbstractWorkflowUtilities() throws Exception {
		if (store == null) {
			throw new Exception("Store never initialized");
		}
	}

	/**
	 * Instantiates a new abstract workflow utilities.
	 *
	 * @param workflowStore
	 *            the workflow store
	 */
	public AbstractWorkflowUtilities(MVStoreMetaContentProvider workflowStore) {
		if (store == null) {
			store = workflowStore;

			userPermissionStore = new UserPermissionWorkflowContentStore(store);
			availableActionStore = new AvailableActionWorkflowContentStore(store);
			definitionDetailStore = new DefinitionDetailWorkflowContentStore(store);
			processDetailStore = new ProcessDetailWorkflowContentStore(store);
			processHistoryStore = new ProcessHistoryContentStore(store);
		}
	}
}
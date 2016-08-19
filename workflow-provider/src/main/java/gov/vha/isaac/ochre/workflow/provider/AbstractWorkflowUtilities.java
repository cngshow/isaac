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

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.AvailableActionContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserPermissionContentStore;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowActionsPermissionsAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowHistoryAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowProcessInitializerConcluder;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowUpdater;

/**
 * Abstract class for higher-level workflow routines
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowUpdater}
 * {@link Bpmn2FileImporter} {@link WorkflowProcessInitializerConcluder}
 * {@link WorkflowActionsPermissionsAccessor} {@link WorkflowHistoryAccessor}
 * {@line WorkflowUpdater}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowUtilities {
	public static final String SYSTEM_AUTOMATED = "AUTOMATED_SYSTEM_ACTION";

	protected static String processStartState;
	protected static String processCancelState;
	protected static String processCancelAction;
	protected static String processConcludeState;
	protected static String processConcludeAction;

	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	/** The user workflow permission store. */
	protected static UserPermissionContentStore userPermissionStore;

	/** The available action store. */
	protected static AvailableActionContentStore availableActionStore;

	/** The definition detail store. */
	protected static DefinitionDetailContentStore definitionDetailStore;

	/** The process detail store. */
	protected static ProcessDetailContentStore processDetailStore;

	/** The process history store. */
	protected static ProcessHistoryContentStore processHistoryStore;

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

			userPermissionStore = new UserPermissionContentStore(store);
			availableActionStore = new AvailableActionContentStore(store);
			definitionDetailStore = new DefinitionDetailContentStore(store);
			processDetailStore = new ProcessDetailContentStore(store);
			processHistoryStore = new ProcessHistoryContentStore(store);
		}
	}

	public static ProcessDetailContentStore getProcessDetailStore() {
		return processDetailStore;
	}
	
	public static String getProcessStartState() {
		return processStartState;
	}
	
	public static String getProcessCancelAction() {
		return processCancelAction;
	}

	public static String getProcessCancelState() {
		return processCancelState;
	}

	public static String getProcessConcludeAction() {
		return processConcludeAction;
	}

	public static String getProcessConcludeState() {
		return processConcludeState;
	}
	
	public static void close() {
		store = null;

		userPermissionStore.close();
		availableActionStore.close();
		definitionDetailStore.close();
		processDetailStore.close();
		processHistoryStore.close();
	}
}
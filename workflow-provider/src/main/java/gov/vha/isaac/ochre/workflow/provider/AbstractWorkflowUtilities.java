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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.AvailableActionContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserPermissionContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowProcessInitializerConcluder;

/**
 * Abstract class for higher-level workflow routines
 * 
 * {@link AbstractWorkflowUtilities} {@link Bpmn2FileImporter}
 * {@link WorkflowProcessInitializerConcluder} {@link WorkflowAccessor}
 * {@line WorkflowUpdater}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowUtilities {
	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	public enum EndWorkflowType {
		CANCELED, CONCLUDED
	};

	private static final String CANCELED_HISTORY_COMMENT = "See Canceled History Information";
	private static final String AUTOMATED_ROLE = "Automated By System";
	private static final String EDITING_ACTION = "EDITING";

	private static Map<EndWorkflowType, Set<AvailableAction>> endNodeTypeMap = new HashMap<>();
	private static Map<UUID, Set<AvailableAction>> definitionStartActionMap = new HashMap<>();
	private static Set<String> editStates = new HashSet<>();

	/** The workflow stores. */
	protected static MVStoreMetaContentProvider store = null;
	protected static UserPermissionContentStore userPermissionStore;
	protected static AvailableActionContentStore availableActionStore;
	protected static DefinitionDetailContentStore definitionDetailStore;
	protected static ProcessDetailContentStore processDetailStore;
	protected static ProcessHistoryContentStore processHistoryStore;

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

	protected String getCanceledComment() {
		return CANCELED_HISTORY_COMMENT;
	}

	public static String getEditingAction() {
		return EDITING_ACTION;
	}

	public static String getAutomatedRole() {
		return AUTOMATED_ROLE;
	}

	public static Map<EndWorkflowType, Set<AvailableAction>> getEndWorkflowTypeMap() {
		return endNodeTypeMap;
	}

	public static Map<UUID, Set<AvailableAction>> getDefinitionStartActionMap() {
		return definitionStartActionMap;
	}

	public static Set<String> getEditStates() {
		return editStates;
	}
	
	public static boolean isFinishStates(String state) {
		for (AvailableAction action : endNodeTypeMap.get(EndWorkflowType.CONCLUDED)) {
			if (action.getInitialState().equals(state)) {
				return true;
			}
		}
		return false;
	}

	public void clearDefinitionCollections() {
		endNodeTypeMap.clear();
		definitionStartActionMap.clear();
		editStates.clear();
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
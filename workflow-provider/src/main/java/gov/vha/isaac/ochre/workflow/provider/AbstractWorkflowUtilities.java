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
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowAccessor;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowProcessInitializerConcluder;

/**
 * An abstract class extended by all higher-level Workflow classes. These
 * provide useful Methods to interact with workflow removing need for developers
 * to interact with lower level workflow metacontent-store routines.
 * 
 * {@link Bpmn2FileImporter} {@link WorkflowProcessInitializerConcluder}
 * {@link WorkflowAccessor} {@line WorkflowUpdater}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowUtilities {
	/** The Logger made available to each Workflow Content Store class */
	protected final Logger logger = LogManager.getLogger();

	/**
	 * A constant used to inform the user that the system automated the
	 * advancing workflow action rather than a specific user
	 */
	public static final String AUTOMATED_ROLE = "Automated By System";

	/** A map of available actions per type of ending workflow */
	private static Map<EndWorkflowType, Set<AvailableAction>> endNodeTypeMap = new HashMap<>();

	/**
	 * A map of available actions per definition from which a workflow may be
	 * started
	 */
	private static Map<UUID, Set<AvailableAction>> definitionStartActionMap = new HashMap<>();

	/** A map of all states per definition from which a process may be edited */
	private static Map<UUID, Set<String>> editStatesMap = new HashMap<>();

	/** Access to every kind of workflow content store */
	protected static MVStoreMetaContentProvider store = null;
	protected static UserPermissionContentStore userPermissionStore;
	protected static AvailableActionContentStore availableActionStore;
	protected static DefinitionDetailContentStore definitionDetailStore;
	protected static ProcessDetailContentStore processDetailStore;
	protected static ProcessHistoryContentStore processHistoryStore;

	/**
	 * Constructor for each kind of workflow utility class. Presumes a store has
	 * already been created.
	 *
	 * @throws Exception
	 *             Thrown if the store has yet to be created.
	 */
	public AbstractWorkflowUtilities() throws Exception {
		if (store == null) {
			throw new Exception("Store never initialized");
		}
	}

	/**
	 * Constructor for each kind of workflow utility class.
	 *
	 * @param workflowStore
	 *            The metacontent-store containing all workflow data
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

	/**
	 * Retrieves the map of end workflow types to the set of all available
	 * actions causing a process to be ProcessStatus.CANCELED or
	 * ProcessStatus.CONLCUDED.
	 * 
	 * Used for programmatically identifying if a requested action requested is
	 * concluding a process.
	 *
	 * @return the map of available actions per type of ending workflow
	 */
	public Map<EndWorkflowType, Set<AvailableAction>> getEndWorkflowTypeMap() {
		return endNodeTypeMap;
	}

	/**
	 * Retrieves the map of workflow definitions to the set of all available
	 * actions start-process actions available.
	 * 
	 * Used for populating Process History with the initial workflow action via
	 * an automated role.
	 *
	 * @return the map of available actions per type of ending workflow
	 */
	public Map<UUID, Set<AvailableAction>> getDefinitionStartActionMap() {
		return definitionStartActionMap;
	}

	/**
	 * Gets a map of all edit states available per workflow definition.
	 * 
	 * Used to identify if the current state is an edit state. If it is,
	 * modeling and mapping can occur and will be added to the process.
	 * 
	 * @return A set of edit states
	 */
	public Map<UUID, Set<String>> getEditStatesMap() {
		return editStatesMap;
	}

	/**
	 * Identifies if the state represents a concluded state by examining the
	 * endNoeTypeMap's CONCLUDED list of AvaialbleActions
	 * 
	 * @param state
	 *            The state to test
	 * 
	 * @return true if the state is a Concluded state
	 */
	public boolean isConcludedState(String state) {
		for (AvailableAction action : endNodeTypeMap.get(EndWorkflowType.CONCLUDED)) {
			if (action.getInitialState().equals(state)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Identifies if the state is an edit state by examining the editStatesMap.
	 * 
	 * @param definitionId
	 *            The definition which the state's process belongs
	 * @param state
	 *            The state to test
	 * 
	 * @return true if the state is an Edit state
	 */
	public boolean isEditState(UUID definitionId, String state) {
		return editStatesMap.get(definitionId).contains(state);
	}

	/**
	 * Enables a BPMN2 file to be imported into a clean environment
	 */
	public void clearDefinitionCollections() {
		endNodeTypeMap.clear();
		definitionStartActionMap.clear();
		editStatesMap.clear();
	}

	/**
	 * Closes all workflow content stores
	 */
	public void closeContentStores() {
		store = null;

		userPermissionStore.close();
		availableActionStore.close();
		definitionDetailStore.close();
		processDetailStore.close();
		processHistoryStore.close();
	}
}
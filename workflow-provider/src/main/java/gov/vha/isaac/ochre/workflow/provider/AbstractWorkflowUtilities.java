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
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.AvailableActionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailsWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserWorkflowPermissionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.UserWorkflowPermission;

/**
 * Abstract class for higher-level workflow routines
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowUpdater}
 * {@link Bpmn2FileImporter} {@link WorkflowInitializerConcluder}
 * {@link WorkflowInformationAccessor}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowUtilities {
	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	/** The user workflow permission store. */
	protected UserWorkflowPermissionWorkflowContentStore userWorkflowPermissionStore;

	/** The available action store. */
	protected AvailableActionWorkflowContentStore availableActionStore;

	/** The definition detail store. */
	protected DefinitionDetailWorkflowContentStore definitionDetailStore;

	/** The process detail store. */
	protected ProcessDetailsWorkflowContentStore processDetailStore;

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

			userWorkflowPermissionStore = new UserWorkflowPermissionWorkflowContentStore(store);
			availableActionStore = new AvailableActionWorkflowContentStore(store);
			definitionDetailStore = new DefinitionDetailWorkflowContentStore(store);
			processDetailStore = new ProcessDetailsWorkflowContentStore(store);
			processHistoryStore = new ProcessHistoryContentStore(store);
		}
	}

	/**
	 * Gets the permissions for user.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param workflowUser
	 *            the workflow user
	 * @return the permissions for user
	 */
	protected UserWorkflowPermission getPermissionsForUser(UUID definitionId, int workflowUser) {
		Set<UserWorkflowPermission> allPermissions = userWorkflowPermissionStore.getAllEntries();
		for (UserWorkflowPermission permission : allPermissions) {
			if (permission.getDefinitionId() == definitionId && permission.getUser() == workflowUser) {
				return permission;
			}
		}
		return null;
	}

	/**
	 * Gets the valid actions for state.
	 *
	 * @param processId
	 *            the process id
	 * @param currentState
	 *            the current state
	 * @param workflowUser
	 *            the workflow user
	 * @return the valid actions for state
	 */
	protected Set<AvailableAction> getValidActionsForState(UUID processId, String currentState, int workflowUser) {
		// Get Roles that may execute State/Action pair based on Process's
		// Definition
		UUID definitionId = getDefinitionForProcess(processId);

		// Identify possible roles based on definition and current state
		Set<AvailableAction> allAvailableActions = findAvailableRoles(definitionId, currentState);

		// Verify that requested action is optional for user with that role
		UserWorkflowPermission permission = getPermissionsForUser(definitionId, workflowUser);

		Set<AvailableAction> permissableActions = new HashSet<>();

		for (AvailableAction action : allAvailableActions) {
			if (permission.getRoles().contains(action.getRole())) {
				permissableActions.add(action);
			}
		}

		return permissableActions;
	}

	/**
	 * Gets the definition for process.
	 *
	 * @param processId
	 *            the process id
	 * @return the definition for process
	 */
	private UUID getDefinitionForProcess(UUID processId) {
		ProcessDetail processDetail = processDetailStore.getEntry(processId);

		Set<DefinitionDetail> definitionDetails = definitionDetailStore.getAllEntries();

		for (DefinitionDetail definitionDetail : definitionDetails) {
			if (definitionDetail.getId() == processDetail.getDefinitionId()) {
				return definitionDetail.getId();
			}
		}

		return null;
	}

	/**
	 * Find available roles.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param currentState
	 *            the current state
	 * @return the sets the
	 */
	private Set<AvailableAction> findAvailableRoles(UUID definitionId, String currentState) {
		Set<AvailableAction> availableActions = new HashSet<>();
		Set<AvailableAction> allActions = availableActionStore.getAllEntries();

		for (AvailableAction entry : allActions) {
			if (entry.getDefinitionId() == definitionId && entry.getCurrentState() == currentState) {
				availableActions.add(entry);
			}
		}

		return availableActions;
	}
}
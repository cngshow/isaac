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
package gov.vha.isaac.ochre.workflow.provider.crud;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Utility to access workflow data from content stores
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowActionsPermissionsAccessor}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowActionsPermissionsAccessor extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow information accessor.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowActionsPermissionsAccessor() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow information accessor.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowActionsPermissionsAccessor(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Gets the user roles by domain standard.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param userId
	 *            the user id
	 * @return the user roles by domain standard
	 */
	public Set<String> getUserRoles(UUID definitionId, int userId) {
		Set<String> domainRoles = new HashSet<>();

		for (UserPermission permission : getAllPermissionsForUser(definitionId, userId)) {
			// First time seeing domainRoleMap so setup
			domainRoles.add(permission.getRole());
		}

		return domainRoles;
	}

	/**
	 * Gets the permissions for user.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param userId
	 *            the user id
	 * @return the permissions for user
	 */
	public Set<UserPermission> getAllPermissionsForUser(UUID definitionId, int userId) {
		Set<UserPermission> allUserPermissions = new HashSet<>();

		for (UserPermission permission : userPermissionStore.getAllEntries()) {
			if (permission.getUser() == userId && permission.getDefinitionId().equals(definitionId)) {
				allUserPermissions.add(permission);
			}
		}

		return allUserPermissions;
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
	public Set<AvailableAction> getAvailableActionsForState(UUID definitionId, String currentState) {
		Set<AvailableAction> availableActions = new HashSet<>();
		Set<AvailableAction> allActions = availableActionStore.getAllEntries();

		for (AvailableAction entry : allActions) {
			if (entry.getDefinitionId().equals(definitionId) && entry.getCurrentState().equals(currentState)) {
				availableActions.add(entry);
			}
		}

		return availableActions;
	}

	/**
	 * Gets the permissible actions for process.
	 *
	 * @param processId
	 *            the process id
	 * @param userId
	 *            the user id
	 * @return the permissible actions for process
	 */
	public Set<AvailableAction> getUserPermissibleActionsForProcess(UUID processId, int userId) {
		Set<AvailableAction> availableActionsForProcess = new HashSet<>();

		// Get current state of process
		WorkflowHistoryAccessor historyAccessory = new WorkflowHistoryAccessor(store);
		ProcessHistory processLatest = historyAccessory.getLatestForProcess(processId);
		String currentState = processLatest.getOutcome();

		// get user's available roles
		UUID definitionId = processDetailStore.getEntry(processId).getDefinitionId();
		Set<String> availableRoles = getUserRoles(definitionId, userId);

		// get available actions based on current state
		Set<AvailableAction> allAvailableActions = getAvailableActionsForState(definitionId, currentState);

		for (AvailableAction action : allAvailableActions) {
			if (availableRoles.contains(action.getRole())) {
				availableActionsForProcess.add(action);
			}
		}

		return availableActionsForProcess;
	}

	/**
	 * Gets the latest process history permissible by state.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param userId
	 *            the user id
	 * @return the latest process history permissible by state
	 */
	public Map<String, Set<ProcessHistory>> getLatestActivePermissibleByRole(UUID definitionId, int userId) {
		Map<String, Set<ProcessHistory>> roleLatestMap = new HashMap<>();
		
		WorkflowHistoryAccessor historyAccessory = new WorkflowHistoryAccessor(store);
		WorkflowStatusAccessor statusAccessory = new WorkflowStatusAccessor(store);

		// Only care about latest processes for Def
		Map<UUID, SortedSet<ProcessHistory>> processHxMap = historyAccessory.getActiveHistoryByProcess();

		for (UUID processId : processHxMap.keySet()) {
			ProcessDetail processDetails = statusAccessory.getProcessDetail(processId);

			if (processDetails.getDefinitionId().equals(definitionId)) {
				Set<AvailableAction> actions = getUserPermissibleActionsForProcess(processId, userId);

				Set<String> rolesInProcess = new HashSet<>();
				
				for (AvailableAction act : actions) {
					if (!rolesInProcess.contains(act.getRole())) {
    					rolesInProcess.add(act.getRole());
        				if (!roleLatestMap.containsKey(act.getRole())) {
        					Set<ProcessHistory> latestByState = new HashSet<>();
        					roleLatestMap.put(act.getRole(), latestByState);
        				}
        				roleLatestMap.get(act.getRole()).add(processHxMap.get(processId).last());
					}
    			}
    		}
		}    

		return roleLatestMap;
	}
}

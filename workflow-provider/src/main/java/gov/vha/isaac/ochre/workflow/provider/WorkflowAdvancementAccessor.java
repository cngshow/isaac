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
import java.util.SortedSet;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DomainStandard;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;

/**
 * Utility to access workflow data from content stores
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowAdvancementAccessor}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowAdvancementAccessor extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow information accessor.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowAdvancementAccessor() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow information accessor.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowAdvancementAccessor(MVStoreMetaContentProvider store) {
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
	/* USER-BASED INFORMATION */
	public Map<DomainStandard, Set<String>> getUserRolesByDomainStandard(UUID definitionId, int userId) {
		Map<DomainStandard, Set<String>> domainRoleMap = new HashMap<>();

		for (UserPermission permission : getAllPermissionsForUser(definitionId, userId)) {
			if (!domainRoleMap.containsKey(permission.getDomainStandard())) {
				// First time seeing domainRoleMap so setup
				Set<String> roles = new HashSet<>();
				domainRoleMap.put(permission.getDomainStandard(), roles);
			}

			domainRoleMap.get(permission.getDomainStandard()).add(permission.getRole());
		}

		return domainRoleMap;
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
			if (entry.getDefinitionId() == definitionId && entry.getCurrentState() == currentState) {
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
	 * @param domain
	 *            the domain
	 * @return the permissible actions for process
	 */
	public Set<AvailableAction> getUserPermissibleActionsForProcess(UUID processId, int userId, DomainStandard domain) {
		Set<AvailableAction> availableActionsForProcess = new HashSet<>();

		// Get current state of process
		WorkflowHistoryAccessor historyAccessory = new WorkflowHistoryAccessor(store);
		ProcessHistory processLatest = historyAccessory.getLatestForProcess(processId);
		String currentState = processLatest.getOutcome();

		// get user's available roles
		UUID definitionId = processDetailStore.getEntry(processId).getDefinitionId();
		Set<String> availableRoles = getUserRolesByDomainStandard(definitionId, userId).get(domain);

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
	public Map<String, Set<ProcessHistory>> getLatestProcessHistoryPermissibleByState(UUID definitionId, int userId) {
		Map<String, Set<ProcessHistory>> latestProcessHistoryPermissibleByState = new HashMap<>();

		WorkflowHistoryAccessor historyAccessory = new WorkflowHistoryAccessor(store);
		WorkflowStatusAccessor statusAccessory = new WorkflowStatusAccessor(store);

		Map<String, Set<UUID>> stateLatestMap = new HashMap<>();
		Map<DomainStandard, Set<UUID>> domainLatestMap = new HashMap<>();

		// get all active processes
		Map<UUID, SortedSet<ProcessHistory>> processHxMap = historyAccessory.getActiveByProcess();

		for (UUID processId : processHxMap.keySet()) {
			ProcessDetail processDetails = statusAccessory.getProcessDetail(processId);

			if (processDetails.equals(definitionId)) {
				// Identify latest history per active process
				ProcessHistory hx = processHxMap.get(processId).last();

				// Find each active process's current state and group by
				// state-Set<Process> map
				String currentState = hx.getOutcome();
				if (!stateLatestMap.containsKey(currentState)) {
					Set<UUID> latestByState = new HashSet<>();
					stateLatestMap.put(currentState, latestByState);
				}
				stateLatestMap.get(currentState).add(hx.getProcessId());

				// Find the domain for each process and map by
				// domain-Set<Process> map
				DomainStandard domain = processDetails.getDomainStandard();
				if (!domainLatestMap.containsKey(domain)) {
					Set<UUID> latestByDomain = new HashSet<>();
					domainLatestMap.put(domain, latestByDomain);
				}
				domainLatestMap.get(domain).add(hx.getProcessId());

			}
		}

		// get user roles by domain
		Map<DomainStandard, Set<String>> domainRolesMap = getUserRolesByDomainStandard(definitionId, userId);

		// For each state, look through the processes's domain, and ensure user
		// has permissions for the state/domain pair
		for (String state : stateLatestMap.keySet()) {
			for (UUID processId : stateLatestMap.get(state)) {
				ProcessDetail processDetails = statusAccessory.getProcessDetail(processId);

				if (domainLatestMap.get(processDetails.getDomainStandard()).contains(processId)) {
					for (String role : domainRolesMap.get(processDetails.getDomainStandard())) {
						if (!latestProcessHistoryPermissibleByState.containsKey(role)) {
							Set<ProcessHistory> latestProcessHistory = new HashSet<>();
							latestProcessHistoryPermissibleByState.put(role, latestProcessHistory);
						}

						latestProcessHistoryPermissibleByState.get(role).add(processHxMap.get(processId).last());
					}
				}
			}
		}

		return latestProcessHistoryPermissibleByState;
	}
}

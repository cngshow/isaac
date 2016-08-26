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
import java.util.TreeSet;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessDetailComparator;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory.ProcessHistoryComparator;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Utility to access workflow data from content stores
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowAccessor}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowAccessor extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow status accessor.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowAccessor() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow status accessor.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowAccessor(MVStoreMetaContentProvider store) {
		super(store);
	}

	public DefinitionDetail getDefinitionDetails(UUID definitionId) {
		return definitionDetailStore.getEntry(definitionId);
	}

	public ProcessDetail getProcessDetails(UUID processId) {
		return processDetailStore.getEntry(processId);
	}

	public SortedSet<ProcessHistory> getProcessHistory(UUID processId) {
		SortedSet<ProcessHistory> allHistoryForProcess = new TreeSet<>(new ProcessHistoryComparator());

		for (ProcessHistory hx : processHistoryStore.getAllEntries()) {
			if (hx.getProcessId().equals(processId)) {
				allHistoryForProcess.add(hx);
			}
		}

		return allHistoryForProcess;
	}

	public boolean isComponentInActiveWorkflow(UUID definitionId, int compSeq) throws Exception {
		for (ProcessDetail proc : processDetailStore.getAllEntries()) {
			if (proc.getDefinitionId().equals(definitionId) && 
				proc.isActive() && 
				proc.getComponentToStampMap().containsKey(compSeq)) {
				return true;
			}
		} 
		
		return false;
	}

	public Set<UserPermission> getUserPermissions(UUID definitionId, int userId) {
		Set<UserPermission> allUserPermissions = new HashSet<>();

		for (UserPermission permission : userPermissionStore.getAllEntries()) {
			if (permission.getUser() == userId && permission.getDefinitionId().equals(definitionId)) {
				allUserPermissions.add(permission);
			}
		}

		return allUserPermissions;
	}

	public Map<ProcessDetail, SortedSet<ProcessHistory>> getAdvanceableProcessInformation(UUID definitionId, int userId) {
		Map<ProcessDetail, SortedSet<ProcessHistory>> processInformation = new HashMap<>();

		// Get User Roles
		Map<String, Set<AvailableAction>> actionsByInitialState = getUserAvailableActionsByInitiailState(definitionId, userId);
		
		// For each active Processes, see if its current state is "applicable current state"
		for (ProcessDetail process : processDetailStore.getAllEntries()) {
			if (process.isActive() && process.getDefinitionId().equals(definitionId)) {
				SortedSet<ProcessHistory> hx = getProcessHistory(process.getId());
				
				if (actionsByInitialState.containsKey(hx.last().getOutcomeState())) {
					processInformation.put(process, hx);
				}
			}
		}
		
		return processInformation;
	}
	
	public Set<AvailableAction> getUserPermissibleActionsForProcess(UUID processId, int userId) {
		ProcessDetail processDetail = getProcessDetails(processId);
		ProcessHistory processLatest = getProcessHistory(processId).last();
		
		Map<String, Set<AvailableAction>> actionsByInitialState = getUserAvailableActionsByInitiailState(processDetail.getDefinitionId(), userId);

		if (actionsByInitialState.containsKey(processLatest.getOutcomeState())) {
			return actionsByInitialState.get(processLatest.getOutcomeState());
		} 
		
		return new HashSet<AvailableAction>();
	}
	
	private Map<String, Set<AvailableAction>> getUserAvailableActionsByInitiailState(UUID definitionId, int userId) {
		Set<String> userRoles = new HashSet<>();
		Map<String, Set<AvailableAction>> applicableActions = new HashMap<>();

		// Get User Roles
		for (UserPermission perm : getUserPermissions(definitionId, userId)) {
			userRoles.add(perm.getRole());
		}

		// Get Map of available actions (by initialState) that can be executed based on userRoles
		for (AvailableAction action : availableActionStore.getAllEntries()) {
			if (action.getDefinitionId().equals(definitionId) && userRoles.contains(action.getRole())) {
				if (!applicableActions.containsKey(action.getInitialState())) {
					applicableActions.put(action.getInitialState(), new HashSet<AvailableAction>());
				}
				
				applicableActions.get(action.getInitialState()).add(action);
			}
		}
		
		return applicableActions;
	}



/*	private Set<ProcessDetail> getActiveProcessesForDefinition(UUID definitionId) {
		Set<ProcessDetail> activeProcesses = new HashSet<>();

		for (ProcessDetail process : processDetailStore.getAllEntries()) {
			if (process.isActive() && process.getDefinitionId().equals(definitionId)) {
				activeProcesses.add(process);
			}
		}

		return activeProcesses;
	}

*/	}

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
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory.ProcessHistoryComparator;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Contains methods necessary to perform workflow-based accessing
 * 
 * {@link AbstractWorkflowUtilities}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowAccessor extends AbstractWorkflowUtilities {

	/**
	 * Default constructor which presumes the workflow-based content store has
	 * already been setup
	 * 
	 * @throws Exception
	 *             Thrown if workflow-based content store has yet to be setup
	 */
	public WorkflowAccessor() throws Exception {

	}

	/**
	 * Constructor includes setting up workflow-based content store which is
	 * used by the workflow accessing methods to pull data
	 *
	 * @param store
	 *            The workflow content store
	 */
	public WorkflowAccessor(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Gets the Definition Detail entry for the specified definition key
	 * 
	 * Used to access all information associated with a given Workflow
	 * Definition.
	 * 
	 * @param definitionId
	 *            The key the the Definition Detail entry
	 * 
	 * @return The definition details entry requested
	 */
	public DefinitionDetail getDefinitionDetails(UUID definitionId) {
		return definitionDetailStore.getEntry(definitionId);
	}

	/**
	 * Gets the Process Detail entry for the specified process key
	 * 
	 * Used to access all information associated with a given workflow process
	 * (i.e. an instance of a definition).
	 * 
	 * @param processId
	 *            The key the the Process Detail entry
	 * 
	 * @return The process details entry requested
	 */
	public ProcessDetail getProcessDetails(UUID processId) {
		return processDetailStore.getEntry(processId);
	}

	/**
	 * Returns all Process History entries associated with the process id. This
	 * contains all the advancements made during the given process. History is
	 * sorted by advancement time.
	 * 
	 * Used to identify the history of a given workflow process (i.e. an
	 * instance of a definition)
	 * 
	 * @param processId
	 *            The key the the Process Detail entry
	 * 
	 * @return the sorted history of the process.
	 */
	public SortedSet<ProcessHistory> getProcessHistory(UUID processId) {
		SortedSet<ProcessHistory> allHistoryForProcess = new TreeSet<>(new ProcessHistoryComparator());

		for (ProcessHistory hx : processHistoryStore.getAllEntries()) {
			if (hx.getProcessId().equals(processId)) {
				allHistoryForProcess.add(hx);
			}
		}

		return allHistoryForProcess;
	}

	/**
	 * Examines the definition to see if the component is in an active workflow.
	 * An active workflow is a workflow in either DEFINED or LAUNCHED process
	 * status.
	 * 
	 * Used to ensure that a concept or sememe doesn't belong to two active
	 * processes simultaneously as that is not allowed at this point. If a
	 * person attempts to do so, they should get a warning that the commit will
	 * not be added to a workflow.
	 * 
	 * @param definitionId
	 *            The key the the Definition Detail entry
	 * @param compNid
	 *            The component to be investigated
	 * 
	 * @return True if the component is in an active workflow.
	 */
	public boolean isComponentInActiveWorkflow(UUID definitionId, int compNid) {
		for (ProcessDetail proc : processDetailStore.getAllEntries()) {
			if (proc.getDefinitionId().equals(definitionId) && proc.isActive()
					&& proc.getComponentNidToStampsMap().containsKey(compNid)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Gets the exhaustive list of workflow roles available for a user for a
	 * given definition.
	 * 
	 * Used to identify what processes are relevant to a user based on a given
	 * definition.
	 * 
	 * @param definitionId
	 *            The key the the Definition Detail entry
	 * @param userId
	 *            The user for whom relevant processes are being determined
	 * 
	 * @return The list of pertinent workflow roles
	 */
	public Set<String> getUserRoles(UUID definitionId, int userId) {
		Set<String> userRoles = new HashSet<>();

		for (UserPermission permission : userPermissionStore.getAllEntries()) {
			if (permission.getUserNid() == userId && permission.getDefinitionId().equals(definitionId)) {
				userRoles.add(permission.getRole());
			}
		}

		return userRoles;
	}

	/**
	 * Map the process history to each process for which the user's permissions
	 * enable them to advance workflow based on the process's current state.
	 * Only active processes can be advanced thus only those processes with such
	 * a status are returned.
	 * 
	 * Used to determine which processes to list when the user selects the
	 * "Author Workflows" link
	 * 
	 * @param definitionId
	 *            The definition being examined
	 * @param userId
	 *            The user for whom relevant processes are being determined
	 * 
	 * @return The map of advanceable processes to their Process History
	 */
	public Map<ProcessDetail, SortedSet<ProcessHistory>> getAdvanceableProcessInformation(UUID definitionId,
			int userNid) {
		Map<ProcessDetail, SortedSet<ProcessHistory>> processInformation = new HashMap<>();

		// Get User Roles
		Map<String, Set<AvailableAction>> actionsByInitialState = getUserAvailableActionsByInitiailState(definitionId,
				userNid);

		// For each ActiveProcesses, see if its current state is "applicable
		// current state" and if
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

	/**
	 * Identifies the set of Available Actions containing actions which the user
	 * may take on a given process
	 * 
	 * Used to determine which actions populate the Transition Workflow picklist
	 * 
	 * @param processId
	 *            The process being examined
	 * @param userId
	 *            The user for whom available actions are being identified
	 * 
	 * @return A set of AvailableActions defining the actions a user can take on
	 *         the process
	 */
	public Set<AvailableAction> getUserPermissibleActionsForProcess(UUID processId, int userNid) {
		ProcessDetail processDetail = getProcessDetails(processId);
		ProcessHistory processLatest = getProcessHistory(processId).last();

		Map<String, Set<AvailableAction>> actionsByInitialState = getUserAvailableActionsByInitiailState(
				processDetail.getDefinitionId(), userNid);

		if (actionsByInitialState.containsKey(processLatest.getOutcomeState())) {
			return actionsByInitialState.get(processLatest.getOutcomeState());
		}

		return new HashSet<AvailableAction>();
	}

	/**
	 * Returns the of available actions a user has permissions based on the
	 * definition's possible initial-states
	 * 
	 * Used to support the getAdvanceableProcessInformation() and
	 * getUserPermissibleActionsForProcess()
	 * 
	 * @param definitionId
	 *            The definition being examined
	 * @param userId
	 *            The user is being examined
	 *
	 * @return The set of all Available Actions for each initial state for which
	 *         the user can advance workflow.
	 */
	private Map<String, Set<AvailableAction>> getUserAvailableActionsByInitiailState(UUID definitionId, int userId) {
		Map<String, Set<AvailableAction>> applicableActions = new HashMap<>();

		// Get User Roles
		Set<String> userRoles = getUserRoles(definitionId, userId);

		// Get Map of available actions (by initialState) that can be executed
		// based on userRoles
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
}

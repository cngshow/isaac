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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.UserWorkflowPermission;

/**
 * Utility to update workflow content stores after initialization
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowUpdater}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowUpdater extends AbstractWorkflowUtilities {

	public WorkflowUpdater() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow updater.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowUpdater(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Adds the stamps to existing process.
	 *
	 * @param processId
	 *            the process id
	 * @param stampSequence
	 *            the stamp sequence
	 */
	public void addStampsToExistingProcess(UUID processId, List<Integer> stampSequence) {
		ProcessDetail details = processDetailStore.getEntry(processId);
		details.addStampSequences(stampSequence);
	}

	public String advanceWorkflow(UUID processId, int workflowUser, String currentState, String actionRequested,
			String comment) throws Exception {
		Set<AvailableAction> permissableActions = getValidActionsForState(processId, currentState, workflowUser);

		if (permissableActions.isEmpty()) {
			throw new Exception(
					"User does not have permission to advance workflow for this process based on current state: "
							+ currentState);
		}

		String outcome = null;
		// Advance Workflow
		for (AvailableAction action : permissableActions) {
			if (action.getAction().equals(actionRequested)) {
				outcome = action.getOutcome();
			}
		}

		if (outcome == null) {
			throw new Exception("User does not have permission to advance workflow with action: " + actionRequested
					+ " for this process based on current state: " + currentState);
		}

		return outcome;
	}

	/**
	 * Update user permissions.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param author
	 *            the author
	 * @param newRoles
	 *            the new roles
	 * @param replaceRoles
	 *            the replace roles
	 */
	public void updateUserPermissions(UUID definitionId, int workflowUser, Set<String> newRoles, boolean replaceRoles) {
		UserWorkflowPermission permission = getPermissionsForUser(definitionId, workflowUser);

		if (replaceRoles) {
			permission.setRoles(newRoles);
		} else {
			Set<String> currentRoles = permission.getRoles();
			currentRoles.addAll(newRoles);
			permission.setRoles(currentRoles);
		}
	}
}

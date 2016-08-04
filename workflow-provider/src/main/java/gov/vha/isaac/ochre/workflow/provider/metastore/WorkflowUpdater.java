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
package gov.vha.isaac.ochre.workflow.provider.metastore;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Utility to update workflow content stores after initialization
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowUpdater}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowUpdater extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow updater.
	 *
	 * @throws Exception
	 *             the exception
	 */
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

	/**
	 * Advance workflow.
	 *
	 * @param processId
	 *            the process id
	 * @param userId
	 *            the user id
	 * @param actionRequested
	 *            the action requested
	 * @param comment
	 *            the comment
	 * @param domain
	 *            the domain
	 * @return the string
	 * @throws Exception
	 *             the exception
	 */
	public String advanceWorkflow(UUID processId, int userId, String actionRequested, String comment) throws Exception {
		WorkflowActionsPermissionsAccessor advancementAccessor = new WorkflowActionsPermissionsAccessor(store);
		WorkflowHistoryAccessor historyAccessor = new WorkflowHistoryAccessor(store);

		// Get User Permissible actions
		Set<AvailableAction> userPermissableActions = advancementAccessor.getUserPermissibleActionsForProcess(processId,
				userId);

		if (userPermissableActions.isEmpty()) {
			ProcessHistory processLatest = historyAccessor.getLatestForProcess(processId);

			throw new Exception("User does not have permission to advance workflow for this process: " + processId
					+ " for this user: " + userId + " based on current state: " + processLatest.getOutcome());
		}

		// Advance Workflow
		String outcome = null;
		for (AvailableAction action : userPermissableActions) {
			if (action.getAction().equals(actionRequested)) {
				outcome = action.getOutcome();
			}
		}

		return outcome;
	}

	/**
	 * Update user roles.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param user
	 *            the user
	 * @param domain
	 *            the domain
	 * @param newRoles
	 *            the new roles
	 */
	public void updateUserRoles(UUID definitionId, int user, Set<String> newRoles) {
		WorkflowActionsPermissionsAccessor advancementAccessor = new WorkflowActionsPermissionsAccessor(store);

		Set<UserPermission> allUserPermissions = advancementAccessor.getAllPermissionsForUser(definitionId, user);

		// Remove all existing permissions for definition/user/domain triplet
		for (UserPermission permission : allUserPermissions) {
			if (permission.getDefinitionId().equals(definitionId) && permission.getUser() == user) {
				userPermissionStore.removeEntry(permission.getId());
			}
		}

		// For each role, add new entry
		for (String role : newRoles) {
			addNewUserRole(definitionId, user, role);
		}
	}

	/**
	 * Adds the new user role.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param user
	 *            the user
	 * @param domain
	 *            the domain
	 * @param role
	 *            the role
	 * @return the uuid
	 */
	public UUID addNewUserRole(UUID definitionId, int user, String role) {
		return userPermissionStore.addEntry(new UserPermission(definitionId, user, role));
	}

	/**
	 * Update workflow concepts.
	 *
	 * @param processId
	 *            the process id
	 * @param concepts
	 *            the concepts
	 */
	public void updateWorkflowConcepts(UUID processId, Set<Integer> concepts) {
		processDetailStore.getEntry(processId).getConcepts().addAll(concepts);
	}
}

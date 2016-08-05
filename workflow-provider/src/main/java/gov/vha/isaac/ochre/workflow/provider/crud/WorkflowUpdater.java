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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
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
	 * @throws Exception 
	 */
	public void addStampsToExistingProcess(UUID processId, int stamp) throws Exception {
		ProcessDetail details = processDetailStore.getEntry(processId);
		
		if (details.getProcessStatus() != ProcessStatus.READY_TO_LAUNCH) {
			throw new Exception("Cannot add stamps to process that has ProcessStatus: " + details.getProcessStatus());	
		}
	
		details.getStampSequences().add(stamp);
		processDetailStore.updateEntry(processId, details);
	}

	/**
	 * Adds the stamps to existing process.
	 *
	 * @param processId
	 *            the process id
	 * @param stampSequence
	 *            the stamp sequence
	 * @throws Exception 
	 */
	public void addConceptsToExistingProcess(UUID processId, Set<Integer> stampSequence) throws Exception {
		ProcessDetail details = processDetailStore.getEntry(processId);
		
		if (details.getProcessStatus() != ProcessStatus.READY_TO_LAUNCH) {
			throw new Exception("Cannot add stamps to process that has ProcessStatus: " + details.getProcessStatus());	
		}
	
		details.getConcepts().addAll(stampSequence);
		processDetailStore.updateEntry(processId, details);
	}


	/**
	 * Adds the stamps to existing process.
	 *
	 * @param processId
	 *            the process id
	 * @param stampSequence
	 *            the stamp sequence
	 * @throws Exception 
	 */
	public void addCocneptsToExistingProcess(UUID processId, List<Integer> stampSequence) throws Exception {
		ProcessDetail details = processDetailStore.getEntry(processId);
		
		if (details.getProcessStatus() != ProcessStatus.READY_TO_LAUNCH) {
			throw new Exception("Cannot add stamps to process that has ProcessStatus: " + details.getProcessStatus());	
		}
	
		details.getStampSequences().addAll(stampSequence);
		processDetailStore.updateEntry(processId, details);
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
	 * @return the string
	 * @throws Exception
	 *             the exception
	 */
	public String advanceWorkflow(UUID processId, int userId, String actionRequested, String comment) throws Exception {
		String outcome = null;

		WorkflowActionsPermissionsAccessor advancementAccessor = new WorkflowActionsPermissionsAccessor(store);
		WorkflowHistoryAccessor historyAccessor = new WorkflowHistoryAccessor(store);

		// Get User Permissible actions
		Set<AvailableAction> userPermissableActions = advancementAccessor.getUserPermissibleActionsForProcess(processId,
				userId);

		if (userPermissableActions.isEmpty()) {
			ProcessHistory processLatest = historyAccessor.getLatestForProcess(processId);

			logger.info("User does not have permission to advance workflow for this process: " + processId
					+ " for this user: " + userId + " based on current state: " + processLatest.getOutcome());
		} else {
    
    		// Advance Workflow
    		for (AvailableAction action : userPermissableActions) {
    			if (action.getAction().equals(actionRequested)) {
    				outcome = action.getOutcome();
    			}
    		}
		}
		
		return outcome;
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
	 * @throws Exception 
	 */
	public void removeUserRole(UUID definitionId, int user, String role) throws Exception {
		WorkflowActionsPermissionsAccessor advancementAccessor = new WorkflowActionsPermissionsAccessor(store);

		Set<UserPermission> allUserPermissions = advancementAccessor.getAllPermissionsForUser(definitionId, user);

		boolean roleFound = false;
		// Remove all existing permissions for definition/user/domain triplet
		for (UserPermission permission : allUserPermissions) {
			if (permission.getDefinitionId().equals(definitionId) && permission.getUser() == user && permission.getRole().equals(role)) {
				roleFound = true;
				userPermissionStore.removeEntry(permission.getId());
			}
		}
		
		if (!roleFound) {
			throw new Exception("User: " + user + " never had role: " + role);
		}
	}
}

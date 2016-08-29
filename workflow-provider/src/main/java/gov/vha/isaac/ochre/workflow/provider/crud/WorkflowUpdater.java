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

import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.ProcessStatus;
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
	 * Advance workflow.
	 *
	 * @param processId
	 *            the process id
	 * @param workflowUser
	 *            the user id
	 * @param actionRequested
	 *            the action requested
	 * @param comment
	 *            the comment
	 * @return the string
	 * @throws Exception
	 *             the exception
	 */
	public UUID advanceWorkflow(UUID processId, int workflowUser, String actionRequested, String comment)
			throws Exception {
		WorkflowAccessor wfAccessor = new WorkflowAccessor(store);

		// Get User Permissible actions
		Set<AvailableAction> userPermissableActions = wfAccessor.getUserPermissibleActionsForProcess(processId,
				workflowUser);

		// Advance Workflow
		for (AvailableAction action : userPermissableActions) {
			if (action.getAction().equals(actionRequested)) {
				ProcessDetail process = processDetailStore.getEntry(processId);
				
				// Update Process Details for launch, cancel, or conclude
				if (getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED).contains(actionRequested)) {
					// Request to cancel workflow
					WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(store);
					initConcluder.finishWorkflowProcess(processId, action, workflowUser, comment,
							EndWorkflowType.CANCELED);
				} else if (process.getStatus().equals(ProcessStatus.DEFINED)
						&& getStartWorkflowTypeMap().containsKey(action.getInitialState())) {
					// Advancing request is to launch workflow
					WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(store);
					initConcluder.launchWorkflowProcess(processId);
				} else if (getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED).contains(action)) {
					// Conclude Request made
					WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(store);
					initConcluder.finishWorkflowProcess(processId, action, workflowUser, comment,
							EndWorkflowType.CONCLUDED);
				}

				if (getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED).contains(action)) {
					// Special case where comment added to cancel screen and cancel store
					// TODO: Better approach?
					comment = getCanceledComment();
				}

				// Add to process history
				ProcessHistory entry = new ProcessHistory(processId, workflowUser, new Date().getTime(),
						action.getInitialState(), action.getAction(),
						action.getOutcomeState(), comment);
				return processHistoryStore.addEntry(entry);
			}
		}

	return null;

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

	public void removeComponentFromWorkflow(UUID processId, int compSeq) throws Exception {
		ProcessDetail detail = processDetailStore.getEntry(processId);

		if (processIsInAcceptableEditState(detail, compSeq, "remove")) {
			if (!detail.getComponentToStampMap().containsKey(compSeq)) {
				throw new Exception("Component " + compSeq + " is not already in Workflow");
			}

			detail.getComponentToStampMap().remove(compSeq);
			processDetailStore.updateEntry(processId, detail);
		}

		// TODO: Handle reverting automatically
	}

	public void addComponentToWorkflow(UUID processId, int compSeq, int stampSeq) throws Exception {
		ProcessDetail detail = processDetailStore.getEntry(processId);

		if (processIsInAcceptableEditState(detail, compSeq, "add")) {
			if (detail.getComponentToStampMap().containsKey(compSeq)) {
				detail.getComponentToStampMap().get(compSeq).add(stampSeq);
			} else {
				ArrayList<Integer> list = new ArrayList<>();
				list.add(stampSeq);
				detail.getComponentToStampMap().put(compSeq, list);
			}

			processDetailStore.updateEntry(processId, detail);
		}
	}

	private boolean processIsInAcceptableEditState(ProcessDetail detail, int compSeq, String exceptionCase)
			throws Exception {
		// Only can do if
		// CASE A: (Component is not in any workflow || Component is already in
		// current process's workflow) AND one of the following:
		// CASE B: Process is in DEFINED
		// CASE C: Process is in LAUNCHED && latestHistory's Outcome is in
		// Editing state
		if (detail == null) {
			throw new Exception("Cannot " + exceptionCase + " component to a workflow that hasn't been defined yet");
		}

		UUID processId = detail.getId();

		WorkflowAccessor wfAccessor = new WorkflowAccessor(store);
		// Check if in Case A. If not, throw exception
		if (wfAccessor.isComponentInActiveWorkflow(detail.getDefinitionId(), compSeq)
				&& !detail.getComponentToStampMap().containsKey(compSeq)) {
			throw new Exception("Cannot " + exceptionCase
					+ " component to workflow because component is already in another active workflow");
		}

		boolean canAddComponent = false;
		// Test Case B
		if (detail.getStatus() == ProcessStatus.DEFINED) {
			canAddComponent = true;
		} else {
			// Test Case C
			if (detail.getStatus() == ProcessStatus.LAUNCHED) {
				ProcessHistory latestHx = wfAccessor.getProcessHistory(processId).last();
				if (getEditStates().contains(latestHx.getOutcomeState())) {
					canAddComponent = true;
				}
			}
		}

		if (!canAddComponent) {
			if (!detail.isActive()) {
				throw new Exception("Cannot " + exceptionCase + " component to inactive workflow");
			} else {
				throw new Exception("Cannot " + exceptionCase
						+ " component when process is in LAUNCHED state, workflow is not in an EDIT state");
			}
		}

		return true;
	}
}

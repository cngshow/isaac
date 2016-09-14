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
import java.util.Optional;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Utility to update workflow content stores after initialization
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowUpdater}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowUpdater extends AbstractWorkflowUtilities {

	static private UUID restTestProcessId;

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
	 * @param userNid
	 *            the user nid
	 * @param actionRequested
	 *            the action requested
	 * @param comment
	 *            the comment
	 * @return the string
	 * @throws Exception
	 *             the exception
	 */
	public UUID advanceWorkflow(UUID processId, int userNid, String actionRequested, String comment) throws Exception {
		WorkflowAccessor wfAccessor = new WorkflowAccessor(store);

		// Get User Permissible actions
		Set<AvailableAction> userPermissableActions = wfAccessor.getUserPermissibleActionsForProcess(processId,
				userNid);

		// Advance Workflow
		for (AvailableAction action : userPermissableActions) {
			if (action.getAction().equals(actionRequested)) {
				ProcessDetail process = processDetailStore.getEntry(processId);

				// Update Process Details for launch, cancel, or conclude
				if (getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED).contains(actionRequested)) {
					// Request to cancel workflow
					WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(store);
					initConcluder.endWorkflowProcess(processId, action, userNid, comment, EndWorkflowType.CANCELED);
				} else if (process.getStatus().equals(ProcessStatus.DEFINED)) {
					for (AvailableAction startAction : getDefinitionStartActionMap().get(process.getDefinitionId())) {
						if (startAction.getOutcomeState().equals(action.getInitialState())) {
							// Advancing request is to launch workflow
							WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(
									store);
							initConcluder.launchWorkflowProcess(processId);

							break;
						}
					}
				} else if (getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED).contains(action)) {
					// Conclude Request made
					WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(store);
					initConcluder.endWorkflowProcess(processId, action, userNid, comment, EndWorkflowType.CONCLUDED);
				}

				if (getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED).contains(action)) {
					// Special case where comment added to cancel screen and
					// cancel store
					// TODO: Better approach?
					comment = getCanceledComment();
				}

				// Add to process history
				ProcessHistory entry = new ProcessHistory(processId, userNid, new Date().getTime(),
						action.getInitialState(), action.getAction(), action.getOutcomeState(), comment);
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
	 * @param userNid
	 *            the user nid
	 * @param domain
	 *            the domain
	 * @param role
	 *            the role
	 * @return the uuid
	 */
	public UUID addNewUserRole(UUID definitionId, int userNid, String role) {
		return userPermissionStore.addEntry(new UserPermission(definitionId, userNid, role));
	}

	public void removeComponentFromWorkflow(UUID processId, int compNid) throws Exception {
		ProcessDetail detail = processDetailStore.getEntry(processId);

		if (isProcessInAcceptableEditState(detail, compNid, "remove")) {
			if (!detail.getComponentNidToStampsMap().containsKey(compNid)) {
				throw new Exception("Component " + compNid + " is not already in Workflow");
			}

			detail.getComponentNidToStampsMap().remove(compNid);
			processDetailStore.updateEntry(processId, detail);
		}

		// TODO: Handle reverting automatically
	}

	private boolean isProcessInAcceptableEditState(ProcessDetail detail, int compNid, String exceptionCase)
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
		if (wfAccessor.isComponentInActiveWorkflow(detail.getDefinitionId(), compNid)
				&& !detail.getComponentNidToStampsMap().containsKey(compNid)) {
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

	public void addCommitRecordToWorkflow(UUID processId, Optional<CommitRecord> commitRecord) throws Exception {
		if (commitRecord.isPresent()) {
			OfInt conceptItr = Get.identifierService()
					.getConceptNidsForConceptSequences(commitRecord.get().getConceptsInCommit().parallelStream())
					.iterator();
			OfInt sememeItr = Get.identifierService()
					.getSememeNidsForSememeSequences(commitRecord.get().getSememesInCommit().parallelStream())
					.iterator();
			OfInt stampItr = commitRecord.get().getStampsInCommit().getIntIterator();

			while (stampItr.hasNext()) {
				int stampSeq = stampItr.next();
				while (conceptItr.hasNext()) {
					addComponentToWorkflow(processId, conceptItr.next(), stampSeq);
				}

				while (sememeItr.hasNext()) {
					addComponentToWorkflow(processId, sememeItr.next(), stampSeq);
				}
			}
		}
	}

	public void addComponentToWorkflow(UUID processId, int compNid, int stampSeq) throws Exception {
		ProcessDetail detail = processDetailStore.getEntry(processId);

		if (isProcessInAcceptableEditState(detail, compNid, "add")) {
			if (detail.getComponentNidToStampsMap().containsKey(compNid)) {
				detail.getComponentNidToStampsMap().get(compNid).add(stampSeq);
			} else {
				ArrayList<Integer> list = new ArrayList<>();
				list.add(stampSeq);
				detail.getComponentNidToStampsMap().put(compNid, list);
			}

			processDetailStore.updateEntry(processId, detail);
		}
	}

	public UUID getRestTestProcessId() {
		return restTestProcessId;
	}

	public void setRestTestProcessId(UUID processId) {
		restTestProcessId = processId;
	}
}

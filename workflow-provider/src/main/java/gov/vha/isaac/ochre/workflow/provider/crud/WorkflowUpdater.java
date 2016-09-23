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
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Contains methods necessary to update existing workflow content after
 * initialization aside from launching or ending them.
 * 
 * {@link AbstractWorkflowUtilities}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowUpdater extends AbstractWorkflowUtilities {

	static private UUID restTestProcessId;

	/**
	 * Default constructor which presumes the workflow-based content store has
	 * already been setup
	 * 
	 * @throws Exception
	 *             Thrown if workflow-based content store has yet to be setup
	 */
	public WorkflowUpdater() throws Exception {

	}

	/**
	 * Constructor includes setting up workflow-based content store which is
	 * used by the workflow accessing methods to pull data
	 *
	 * @param store
	 *            The workflow content store
	 */
	public WorkflowUpdater(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Advance an existing process with the specified action. In doing so, the
	 * user must add an advancement comment.
	 * 
	 * Used by filling in the information prompted for after selecting a
	 * Transition Workflow action.
	 * 
	 * @param processId
	 *            The process being advanced.
	 * @param userNid
	 *            The user advancing the process.
	 * @param actionRequested
	 *            The advancement action the user requested.
	 * @param comment
	 *            The comment added by the user in advancing the process.
	 * @param editCoordinate
	 * 
	 * @return True if the advancement attempt was successful
	 * 
	 * @throws Exception
	 *             Thrown if the requested action was to launch or end a process
	 *             and while updating the process accordingly, an execption
	 *             occurred
	 */
	public boolean advanceWorkflow(UUID processId, int userNid, String actionRequested, String comment,
			EditCoordinate editCoordinate) throws Exception {
		WorkflowAccessor wfAccessor = new WorkflowAccessor(store);

		// Get User Permissible actions
		Set<AvailableAction> userPermissableActions = wfAccessor.getUserPermissibleActionsForProcess(processId,
				userNid);

		// Advance Workflow
		for (AvailableAction action : userPermissableActions) {
			if (action.getAction().equals(actionRequested)) {
				ProcessDetail process = processDetailStore.getEntry(processId);

				// Update Process Details for launch, cancel, or conclude
				if (getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED).contains(action)) {
					// Request to cancel workflow
					WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(store);
					initConcluder.endWorkflowProcess(processId, action, userNid, comment, EndWorkflowType.CANCELED,
							editCoordinate);
				} else if (process.getStatus().equals(ProcessStatus.DEFINED)) {
					for (AvailableAction startAction : getDefinitionStartActionMap().get(process.getDefinitionId())) {
						if (startAction.getOutcomeState().equals(action.getInitialState())) {
							// Advancing request is to launch workflow
							WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(
									store);
							initConcluder.launchProcess(processId);

							break;
						}
					}
				} else if (getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED).contains(action)) {
					// Conclude Request made
					WorkflowProcessInitializerConcluder initConcluder = new WorkflowProcessInitializerConcluder(store);
					initConcluder.endWorkflowProcess(processId, action, userNid, comment, EndWorkflowType.CONCLUDED,
							null);
				}

				// Add to process history
				ProcessHistory entry = new ProcessHistory(processId, userNid, new Date().getTime(),
						action.getInitialState(), action.getAction(), action.getOutcomeState(), comment);

				processHistoryStore.addEntry(entry);
				return true;
			}
		}

		return false;

	}

	/**
	 * Removes a component from a process where the component had been
	 * previously saved and associated with. In doing so, reverts the component
	 * to its original state prior to the saves associated with the component.
	 * The revert is performed by adding new versions to ensure that the
	 * component attributes are identical prior to any modification associated
	 * with the process. Note that nothing prevents future edits to be performed
	 * upon the component associated with the same process.
	 * 
	 * Used when component is removed from the process's component details panel
	 * 
	 * @param processId
	 *            THe process from which the component is to be removed
	 * @param compNid
	 *            The component whose changes are to be reverted and removed
	 *            from the process
	 * @param editCoordinate
	 *            TODO
	 * @throws Exception
	 *             Thrown if the component has been found to not be currently
	 *             associated with the process
	 */
	public void removeComponentFromWorkflow(UUID processId, int compNid, EditCoordinate editCoordinate)
			throws Exception {
		ProcessDetail detail = processDetailStore.getEntry(processId);

		if (isModifiableComponentInProcess(detail, compNid)) {
			if (!detail.getComponentNidToStampsMap().containsKey(compNid)) {
				throw new Exception("Component " + compNid + " is not already in Workflow");
			}

			detail.getComponentNidToStampsMap().remove(compNid);
			processDetailStore.updateEntry(processId, detail);
		} else {
			throw new Exception("Components may not be renived from Workflow: " + compNid);
		}

		revertChanges(Arrays.asList(compNid), detail.getTimeCreated(), editCoordinate);
	}

	/**
	 * Identifies if process is in an edit state. May only be done if either the
	 * component is not in any workflow or if it is already in this process's
	 * workflow AND one of the following: a) Process status is DEFINED or b)
	 * process status is LAUNCHED while its latestHistory's Outcome is an
	 * Editing state.
	 * 
	 * Used by addCommitRecordToWorkflow() and removeComponentFromWorfklow() to
	 * ensure that the process is in a valid state to be performing such an
	 * action
	 * 
	 * @param process
	 *            The process being investigated
	 * @param compNid
	 *            The component to be added/removed
	 * 
	 * @return True if the component can be added or removed from the process
	 * 
	 * @throws Exception
	 *             Thrown if process doesn't exist,
	 */
	private boolean isModifiableComponentInProcess(ProcessDetail process, int compNid) throws Exception {
		if (process == null) {
			throw new Exception("Cannot examine modification capability as the process doesn't exist");
		}

		UUID processId = process.getId();

		WorkflowAccessor wfAccessor = new WorkflowAccessor(store);
		// Check if in Case A. If not, throw exception
		if (wfAccessor.isComponentInActiveWorkflow(process.getDefinitionId(), compNid)
				&& !process.getComponentNidToStampsMap().containsKey(compNid)) {
			// Can't do so because component is already in another active
			// workflow
			return false;
		}

		boolean canAddComponent = false;
		// Test Case B
		if (process.getStatus() == ProcessStatus.DEFINED) {
			canAddComponent = true;
		} else {
			// Test Case C
			if (process.getStatus() == ProcessStatus.LAUNCHED) {
				ProcessHistory latestHx = wfAccessor.getProcessHistory(processId).last();
				if (isEditState(process.getDefinitionId(), latestHx.getOutcomeState())) {
					canAddComponent = true;
				}
			}
		}

		if (!canAddComponent) {
			if (!process.isActive()) {
				// Cannot do so because process is not active
				return false;
			} else {
				// Cannot do so because process is in LAUNCHED state yet the
				// workflow is not in an EDIT state
				return false;
			}
		}

		return true;
	}

	/**
	 * Attempts to add components associated with a commit to a process. Can
	 * only be done if the process and component are in the process state as
	 * defined by addComponentToWorkflow. Does so for all concepts and sememes
	 * in the commit record as well as the commit record's stamp sequence .
	 * 
	 * Called by the REST implement commit() methods.
	 *
	 * @param processId
	 *            The process to which a commit record is being added
	 * @param commitRecord
	 *            The commit record being associated with the process
	 * 
	 * @throws Exception
	 *             Thrown if process doesn't exist,
	 */
	public void addCommitRecordToWorkflow(UUID processId, Optional<CommitRecord> commitRecord) throws Exception {
		if (commitRecord.isPresent()) {
			ProcessDetail detail = processDetailStore.getEntry(processId);

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
					int conNid = conceptItr.next();
					if (isModifiableComponentInProcess(detail, conNid)) {
						addComponentToWorkflow(detail, conNid, stampSeq);
					} else {
						// TODO: Prevention strategy for when component not
						// deemed "addable" to WF
						throw new Exception("Concept may not be added to Workflow: " + conNid);
					}
				}

				while (sememeItr.hasNext()) {
					int semNid = sememeItr.next();
					if (isModifiableComponentInProcess(detail, semNid)) {
						addComponentToWorkflow(detail, semNid, stampSeq);
					} else {
						// TODO: Prevention strategy for when component not
						// deemed "addable" to WF
						throw new Exception("Sememe may not be added to Workflow: " + semNid);
					}
				}
			}
		}
	}

	/**
	 * Associates a component with a process. In doing so, associates the stamp
	 * sequence as well. Multiple stamps may be associated with any given
	 * component in a single process.
	 * 
	 * Note: Made public to enable unit testing
	 *
	 * @param process
	 *            The process to which a component/stamp pair is being added
	 * @param compNid
	 *            The component being added
	 * @param stampSeq
	 *            The stamp being added
	 */
	public void addComponentToWorkflow(ProcessDetail process, int compNid, int stampSeq) {
		if (process.getComponentNidToStampsMap().containsKey(compNid)) {
			process.getComponentNidToStampsMap().get(compNid).add(stampSeq);
		} else {
			ArrayList<Integer> list = new ArrayList<>();
			list.add(stampSeq);
			process.getComponentNidToStampsMap().put(compNid, list);
		}

		processDetailStore.updateEntry(process.getId(), process);
	}

	/**
	 * Gets the process id for a rest test.
	 */
	public UUID getRestTestProcessId() {
		// TODO: Examine if better solution to this
		return restTestProcessId;
	}

	/**
	 * sets the process id for a rest test.
	 *
	 * @param processId
	 *            The process created for Rest Teseting purposes
	 */
	public void setRestTestProcessId(UUID processId) {
		// TODO: Examine if better solution to this
		restTestProcessId = processId;
	}
}

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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.UUID;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableLogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableLongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableStringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.workflow.model.contents.AvailableAction;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.provider.BPMNInfo;
import gov.vha.isaac.ochre.workflow.provider.WorkflowProvider;

/**
 * Contains methods necessary to update existing workflow content after
 * initialization aside from launching or ending them.
 * 
 * {@link BPMNInfo}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Service
@Singleton
public class WorkflowUpdater {

	private WorkflowProvider workflowProvider_;

	// For HK2
	private WorkflowUpdater() {
		workflowProvider_ = LookupService.get().getService(WorkflowProvider.class);
	}

	/**
	 * A constant used to inform the user that the comment added during
	 * cancelation is in a special location
	 */
	private static final String CANCELED_HISTORY_COMMENT = "See Canceled History Information";

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
		// Get User Permissible actions
		Set<AvailableAction> userPermissableActions = workflowProvider_.getWorkflowAccessor()
				.getUserPermissibleActionsForProcess(processId, userNid);

		// Advance Workflow
		for (AvailableAction action : userPermissableActions) {
			if (action.getAction().equals(actionRequested)) {
				ProcessDetail process = workflowProvider_.getProcessDetailStore().get(processId);

				// Update Process Details for launch, cancel, or conclude
				if (workflowProvider_.getBPMNInfo().getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED)
						.contains(action)) {
					// Request to cancel workflow
					workflowProvider_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, action,
							userNid, comment, EndWorkflowType.CANCELED, editCoordinate);
				} else if (process.getStatus().equals(ProcessStatus.DEFINED)) {
					for (AvailableAction startAction : workflowProvider_.getBPMNInfo().getDefinitionStartActionMap()
							.get(process.getDefinitionId())) {
						if (startAction.getOutcomeState().equals(action.getInitialState())) {
							// Advancing request is to launch workflow
							workflowProvider_.getWorkflowProcessInitializerConcluder().launchProcess(processId);
							break;
						}
					}
				} else if (workflowProvider_.getBPMNInfo().getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED)
						.contains(action)) {
					// Conclude Request made
					workflowProvider_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, action,
							userNid, comment, EndWorkflowType.CONCLUDED, null);
				} else {
					ProcessDetail entry = workflowProvider_.getProcessDetailStore().get(processId);
					entry.setOwnerNid(0);
					workflowProvider_.getProcessDetailStore().put(processId, entry);
				}

				if (workflowProvider_.getBPMNInfo().getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED)
						.contains(action)) {
					// Special case where comment added to cancel screen and
					// cancel store
					// TODO: Better approach?
					comment = CANCELED_HISTORY_COMMENT;
				}

				// Add to process history
				ProcessHistory hx = workflowProvider_.getWorkflowAccessor().getProcessHistory(processId).last();
				ProcessHistory entry = new ProcessHistory(processId, userNid, new Date().getTime(),
						action.getInitialState(), action.getAction(), action.getOutcomeState(), comment, hx.getHistorySequence() + 1);

				workflowProvider_.getProcessHistoryStore().add(entry);
								
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
		ProcessDetail detail = workflowProvider_.getProcessDetailStore().get(processId);

		if (isModifiableComponentInProcess(detail, compNid)) {
			if (!detail.getComponentNids().contains(compNid)) {
				throw new Exception("Component " + compNid + " is not already in Workflow");
			}

			revertChanges(Arrays.asList(compNid), processId, editCoordinate);
			
			detail.getComponentNids().remove(compNid);
			workflowProvider_.getProcessDetailStore().put(processId, detail);
		} else {
			throw new Exception("Components may not be renived from Workflow: " + compNid);
		}
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
		// Check if in Case A. If not, throw exception
		if (workflowProvider_.getWorkflowAccessor().isComponentInActiveWorkflow(process.getDefinitionId(), compNid)
				&& !process.getComponentNids().contains(compNid)) {
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
				ProcessHistory latestHx = workflowProvider_.getWorkflowAccessor().getProcessHistory(processId).last();
				if (workflowProvider_.getBPMNInfo().isEditState(process.getDefinitionId(),
						latestHx.getOutcomeState())) {
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
			ProcessDetail detail = workflowProvider_.getProcessDetailStore().get(processId);

			OfInt conceptItr = Get.identifierService()
					.getConceptNidsForConceptSequences(commitRecord.get().getConceptsInCommit().parallelStream())
					.iterator();
			OfInt sememeItr = Get.identifierService()
					.getSememeNidsForSememeSequences(commitRecord.get().getSememesInCommit().parallelStream())
					.iterator();

			while (conceptItr.hasNext()) {
				int conNid = conceptItr.next();
				if (isModifiableComponentInProcess(detail, conNid)) {
					addComponentToWorkflow(detail, conNid);
				} else {
					// TODO: Prevention strategy for when component not
					// deemed "addable" to WF
					throw new Exception("Concept may not be added to Workflow: " + conNid);
				}
			}

			while (sememeItr.hasNext()) {
				int semNid = sememeItr.next();
				if (isModifiableComponentInProcess(detail, semNid)) {
					addComponentToWorkflow(detail, semNid);
				} else {
					// TODO: Prevention strategy for when component not
					// deemed "addable" to WF
					throw new Exception("Sememe may not be added to Workflow: " + semNid);
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
	public void addComponentToWorkflow(ProcessDetail process, int compNid) {
		if (!process.getComponentNids().contains(compNid)) {
			process.getComponentNids().add(compNid);
		}

		workflowProvider_.getProcessDetailStore().put(process.getId(), process);
	}

	protected void revertChanges(Collection<Integer> compNidSet, UUID processId, EditCoordinate editCoordinate)
			throws Exception {

		if (editCoordinate != null) {
			for (Integer compNid : compNidSet) {
				StampedVersion version = workflowProvider_.getWorkflowAccessor().getVersionPriorToWorkflow(processId, compNid);
				
				// add new version identical to version associated with
				// actualStampSeq
				if (Get.identifierService().getChronologyTypeForNid(compNid) == ObjectChronologyType.CONCEPT) {
					ConceptChronology<?> conceptChron = ((ConceptVersion)version).getChronology();
					conceptChron.createMutableVersion(((ConceptVersion<?>) version).getState(), editCoordinate);
					Get.commitService().addUncommitted(conceptChron);
					Get.commitService().commit("Reverting concept to how it was prior to workflow");
				} else if (Get.identifierService()
						.getChronologyTypeForNid(compNid) == ObjectChronologyType.SEMEME) {
					SememeChronology<?> semChron = ((SememeVersion)version).getChronology();
					SememeVersion createdVersion = ((SememeChronology) semChron).createMutableVersion(
							version.getClass(), ((SememeVersion<?>) version).getState(), editCoordinate);

					createdVersion = populateData(createdVersion, (SememeVersion<?>) version);
					Get.commitService().addUncommitted(createdVersion.getChronology()).get();
					Get.commitService().commit("Reverting sememe to how it was prior to workflow");
				}
			}
		}
	}

	private SememeVersion<?> populateData(SememeVersion<?> newVer, SememeVersion<?> originalVersion) throws Exception {
		switch (newVer.getChronology().getSememeType()) {
		case MEMBER:
			return newVer;
		case COMPONENT_NID:
			((MutableComponentNidSememe<?>) newVer)
					.setComponentNid(((ComponentNidSememe<?>) originalVersion).getComponentNid());
			return newVer;
		case DESCRIPTION:
			((MutableDescriptionSememe<?>) newVer).setText(((DescriptionSememe<?>) originalVersion).getText());
			((MutableDescriptionSememe<?>) newVer).setDescriptionTypeConceptSequence(
					((DescriptionSememe<?>) originalVersion).getDescriptionTypeConceptSequence());
			((MutableDescriptionSememe<?>) newVer).setCaseSignificanceConceptSequence(
					((DescriptionSememe<?>) originalVersion).getCaseSignificanceConceptSequence());
			((MutableDescriptionSememe<?>) newVer)
					.setLanguageConceptSequence(((DescriptionSememe<?>) originalVersion).getLanguageConceptSequence());
			return newVer;
		case DYNAMIC:
			((MutableDynamicSememe<?>) newVer).setData(((DynamicSememe<?>) originalVersion).getData());
			return newVer;
		case LONG:
			((MutableLongSememe<?>) newVer).setLongValue(((LongSememe<?>) originalVersion).getLongValue());
			return newVer;
		case STRING:
			((MutableStringSememe<?>) newVer).setString(((StringSememe<?>) originalVersion).getString());
			return newVer;
		case RELATIONSHIP_ADAPTOR:
			throw new Exception("Cannot handle Relationship adaptors at this time");
			/*
			 * RelationshipVersionAdaptorImpl origRelVer =
			 * (RelationshipVersionAdaptorImpl) originalVersion;
			 * RelationshipAdaptorChronicleKeyImpl key = new
			 * RelationshipAdaptorChronicleKeyImpl(
			 * origRelVer.getOriginSequence(),
			 * origRelVer.getDestinationSequence(),
			 * origRelVer.getTypeSequence(), origRelVer.getGroup(),
			 * origRelVer.getPremiseType(), origRelVer.getNodeSequence());
			 * 
			 * return new RelationshipVersionAdaptorImpl(key, inactiveStampSeq);
			 */
		case LOGIC_GRAPH:
			((MutableLogicGraphSememe<?>) newVer).setGraphData(((LogicGraphSememe<?>) originalVersion).getGraphData());
			return newVer;
		case UNKNOWN:
			throw new UnsupportedOperationException();
		}

		return null;
	}

}

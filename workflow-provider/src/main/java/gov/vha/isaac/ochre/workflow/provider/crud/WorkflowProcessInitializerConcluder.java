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

import java.util.Date;
import java.util.UUID;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.workflow.model.contents.AvailableAction;
import gov.vha.isaac.ochre.workflow.model.contents.DefinitionDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.provider.BPMNInfo;
import gov.vha.isaac.ochre.workflow.provider.WorkflowProvider;

/**
 * Contains methods necessary to start, launch, cancel, or conclude a workflow
 * process
 * 
 * {@link BPMNInfo}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Service
@Singleton
public class WorkflowProcessInitializerConcluder {

	private WorkflowProvider workflowProvider_;
	
	//for HK2
	private WorkflowProcessInitializerConcluder()
	{
		workflowProvider_ = LookupService.get().getService(WorkflowProvider.class);
	}
	
	/**
	 * Creates a new workflow process instance. In turn, a new entry is added to
	 * the ProcessDetails content store. The process status defaults as DEFINED.
	 * 
	 * Used by users when creating a new process
	 * 
	 * @param definitionId
	 *            The definition for which the process should be based on
	 * @param userNid
	 *            The user whom is creating the new process
	 * @param name
	 *            The name of the new process
	 * @param description
	 *            The description of the new process
	 * 
	 * @return The process id which is in turn the key to the Process Detail's
	 *         entry
	 * 
	 * @throws Exception
	 */
	public UUID createWorkflowProcess(UUID definitionId, int userNid, String name, String description)
			throws Exception {
		if (name == null || name.isEmpty() || description == null || description.isEmpty()) {
			throw new Exception("Name and Description must be filled out when creating a process");
		}

		// TODO: Do we actually want this prevention measure?
		/*
		for (ProcessDetail detail : processDetailStore.getAllEntries()) {
			if (detail.getName().equalsIgnoreCase(name)) {
				throw new Exception("Process names must be unique");
			}
		}
		*/

		// Create Process Details with "DEFINED"
		ProcessDetail details = new ProcessDetail(definitionId, userNid, new Date().getTime(),
				ProcessStatus.DEFINED, name, description);
		UUID processId = workflowProvider_.getProcessDetailStore().add(details);

		// Add Process History with START_STATE-AUTOMATED-EDIT_STATE

		// At some point, need to handle the case where multiple startActions
		// may be defined for single DefinitionId. For now, verify only one and
		// use it
		if (workflowProvider_.getBPMNInfo().getDefinitionStartActionMap().get(definitionId).size() != 1) {
			throw new Exception(
					"Currently only able to handle single startAction within a definition. This definition found: "
							+ workflowProvider_.getBPMNInfo().getDefinitionStartActionMap().get(definitionId).size());
		}

		AvailableAction startAdvancement = workflowProvider_.getBPMNInfo().getDefinitionStartActionMap().get(definitionId).iterator().next();
		ProcessHistory advanceEntry = new ProcessHistory(processId, userNid, new Date().getTime(),
				startAdvancement.getInitialState(), startAdvancement.getAction(), startAdvancement.getOutcomeState(),
				"");
		workflowProvider_.getProcessHistoryStore().add(advanceEntry);

		return processId;
	}

	/**
	 * Launch a process as long as a) the process is in a defined state and b)
	 * there are components associated with the process. By launching it, the
	 * ProcessDetails has its status updated to LAUNCHED and the time launched
	 * gets an entry of <NOW>.
	 *
	 * Used when a process is advanced and it is in the edit state and is has a
	 * DEFINED status.
	 * 
	 * @param processId
	 *            the process being launched
	 * 
	 * @throws Exception
	 *             Thrown if a) process doesn't exist, b) process exists but is
	 *             not in the DEFINED status, or c) no components are associated
	 *             with the process
	 */
	public void launchProcess(UUID processId) throws Exception {
		ProcessDetail entry = workflowProvider_.getProcessDetailStore().get(processId);

		if (entry == null) {
			throw new Exception("Cannot launch workflow that hasn't been defined first");
		} else if (entry.getStatus() != ProcessStatus.DEFINED) {
			throw new Exception("Only processes that have a DEFINED status may be launched");
		} else if (entry.getComponentNidToStampsMap().isEmpty()) {
			throw new Exception("Workflow can only be launched when the workflow contains components to work on");
		}

		// Update Process Details with "LAUNCHED"
		entry.setStatus(ProcessStatus.LAUNCHED);
		entry.setTimeLaunched(new Date().getTime());
		workflowProvider_.getProcessDetailStore().put(processId, entry);
	}

	/**
	 * Ends a workflow status either via concluding it or canceling it. In doing
	 * so, the ProcessDetails has its status updated accordingly (either
	 * CANCELED or CONCLUDED) and the time launched gets an entry of <NOW>. In
	 * addition, another Process History entry is added showing the information
	 * associated with this final advancement.
	 *
	 * Used when advancing a process to either a completed state or by canceling
	 * it.
	 *
	 * @param processId
	 *            The process being ended
	 * @param actionToProcess
	 *            The AvailableAction the user requested
	 * @param userNid
	 *            The user ending the workflow
	 * @param comment
	 *            The user added comment associated with the advancement
	 * @param endType
	 *            The type of END-ADVANCEMENT associated with the selected
	 *            action (Canceled or Concluded)
	 * 
	 * @throws Exception
	 *             Thrown if the process doesn't exist or an attempt is made to
	 *             a) cancel or conclude a process which isn't active, b)
	 *             conclude a process where the process is not LAUNCHED, or c)
	 *             conclude a process where the outcome state isn't a concluded
	 *             state according to the definition
	 */
	public void endWorkflowProcess(UUID processId, AvailableAction actionToProcess, int userNid, String comment,
			EndWorkflowType endType) throws Exception {
		ProcessDetail entry = workflowProvider_.getProcessDetailStore().get(processId);

		if (entry == null) {
			throw new Exception("Cannot cancel nor conclude a workflow that hasn't been defined yet");
		} else if (!entry.isActive()) {
			throw new Exception("Cannot end a workflow that is not active.  Current status: " + entry.getStatus());
		} else if (endType == EndWorkflowType.CONCLUDED) {
			if (entry.getStatus() != ProcessStatus.LAUNCHED) {
				throw new Exception("Cannot conclude workflow that is in the following state: " + entry.getStatus());
			} else {
				ProcessHistory hx = workflowProvider_.getWorkflowAccessor().getProcessHistory(processId).last();
				if (!workflowProvider_.getBPMNInfo().isConcludedState(hx.getOutcomeState())) {
					DefinitionDetail defEntry = workflowProvider_.getDefinitionDetailStore().get(entry.getDefinitionId());
					throw new Exception(
							"Cannot perform Conclude action on the definition: " + defEntry.getName() + " version: "
									+ defEntry.getVersion() + " when the workflow state is: " + hx.getOutcomeState());
				}
			}
		}

		if (endType.equals(EndWorkflowType.CANCELED)) {
			entry.setStatus(ProcessStatus.CANCELED);
		} else if (endType.equals(EndWorkflowType.CONCLUDED)) {
			entry.setStatus(ProcessStatus.CONCLUDED);
		}
		entry.setTimeCanceledOrConcluded(new Date().getTime());
		workflowProvider_.getProcessDetailStore().put(processId, entry);

		// Only add Cancel state in Workflow if process has already been launched
		ProcessHistory advanceEntry = new ProcessHistory(processId, userNid, new Date().getTime(),
				actionToProcess.getInitialState(), actionToProcess.getAction(), actionToProcess.getOutcomeState(),
				comment);
		workflowProvider_.getProcessHistoryStore().add(advanceEntry);

		if (endType.equals(EndWorkflowType.CANCELED)) {
			// TODO: Handle cancelation store and handle reverting automatically
		}
	}
}

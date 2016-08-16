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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Utility to start, complete, or conclude a workflow process
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowProcessInitializerConcluder}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowProcessInitializerConcluder extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow process initializer concluder.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowProcessInitializerConcluder() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow proces initializer concluder.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowProcessInitializerConcluder(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Start a new workflow process.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param concepts
	 *            the concepts
	 * @param stampSequences
	 *            the stamp sequence
	 * @param user
	 *            the user
	 * @param subjectMatter
	 *            the subject matter
	 * @return the uuid
	 * @throws Exception
	 */
	public UUID createWorkflowProcess(UUID definitionId, Set<Integer> concepts, List<Integer> stampSequences, int user,
			SubjectMatter subjectMatter) throws Exception {
		WorkflowStatusAccessor accessor = new WorkflowStatusAccessor();

		for (Integer conSeq : concepts) {
			if (accessor.isConceptInActiveWorkflow(conSeq)) {
				throw new Exception("Concept to add in workflow exists in active workflow");
			}
		}
		ArrayList<Integer> stampSequencesArrayList = new ArrayList<>();
		stampSequencesArrayList.addAll(stampSequences);
		ProcessDetail details = new ProcessDetail(definitionId, concepts, stampSequencesArrayList, user, new Date().getTime(),
				subjectMatter, ProcessStatus.READY_TO_LAUNCH);
		UUID processId = processDetailStore.addEntry(details);

		return processId;
	}

	/**
	 * Launch a workflow process.
	 *
	 * @param processId
	 *            the process id
	 * @throws Exception
	 */
	public void launchWorkflowProcess(UUID processId) throws Exception {
		ProcessDetail entry = processDetailStore.getEntry(processId);

		if (entry == null) {
			throw new Exception("Cannot launch workflow that hasn't been defined first");
		} else if (entry.getProcessStatus() != ProcessStatus.READY_TO_LAUNCH) {
			throw new Exception("Cannot launch workflow that has a process status of: " + entry.getProcessStatus());
		}

		entry.setProcessStatus(ProcessStatus.LAUNCHED);
		processDetailStore.updateEntry(processId, entry);

		ProcessHistory advanceEntry = new ProcessHistory(processId, entry.getCreator(), new Date().getTime(), AbstractWorkflowUtilities.processStartState,
				SYSTEM_AUTOMATED, "Ready for Edit", "");
		processHistoryStore.addEntry(advanceEntry);
	}

	/**
	 * Cancel workflow process.
	 *
	 * @param processId
	 *            the process id
	 * @param comment
	 *            the comment
	 * @throws Exception
	 */
	public void cancelWorkflowProcess(UUID processId, int workflowUser, String comment) throws Exception {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		WorkflowStatusAccessor statusAccessor = new WorkflowStatusAccessor(store);
		ProcessHistory processLatest = null;
		
		if (statusAccessor.getProcessDetail(processId).getProcessStatus().equals(ProcessStatus.LAUNCHED)) {
			WorkflowHistoryAccessor historyAccessor = new WorkflowHistoryAccessor(store);
			processLatest = historyAccessor.getLatestForProcess(processId);
		}
		
		if (entry == null) {
			throw new Exception("Cannot cancel workflow that hasn't been defined first");
		} else if (entry.getProcessStatus() == ProcessStatus.CANCELED || entry.getProcessStatus() == ProcessStatus.CONCLUDED) {
			throw new Exception("Cannot cancel workflow that has a process status of: " + entry.getProcessStatus());
		}

		entry.setProcessStatus(ProcessStatus.CANCELED);
		entry.setTimeConcluded(new Date().getTime());
		processDetailStore.updateEntry(processId, entry);

		// Only add Cancel state in Workflow if process has already been launched
		if (processLatest != null) {
			ProcessHistory advanceEntry = new ProcessHistory(processId, workflowUser, new Date().getTime(), processLatest.getOutcome(), AbstractWorkflowUtilities.getProcessCancelAction(),
    				AbstractWorkflowUtilities.getProcessCancelState(), "");
    		processHistoryStore.addEntry(advanceEntry);
		}

		
		// TODO: Handle cancellation store and handle reverting automatically
	}

	/**
	 * Conclude workflow.
	 *
	 * @param processId
	 *            the process id
	 * @throws Exception
	 */
	public void concludeWorkflowProcess(UUID processId, int workflowUser) throws Exception {
		ProcessDetail entry = processDetailStore.getEntry(processId);

		if (entry == null) {
			throw new Exception("Cannot conclude workflow that hasn't been defined first");
		} else if (entry.getProcessStatus() != ProcessStatus.LAUNCHED) {
			throw new Exception("Cannot conclude workflow that has a process status of: " + entry.getProcessStatus());
		} else {
			WorkflowHistoryAccessor accessor = new WorkflowHistoryAccessor();
			ProcessHistory hx = accessor.getLatestForProcess(processId);
			
			if (!processConcludeState.equals(hx.getOutcome())) {
				throw new Exception(
						"Cannot conclude workflow that has a latest history with outcome: " + hx.getOutcome());
			}
		}

		entry.setProcessStatus(ProcessStatus.CONCLUDED);
		entry.setTimeConcluded(new Date().getTime());
		processDetailStore.updateEntry(processId, entry);
		
		WorkflowHistoryAccessor historyAccessor = new WorkflowHistoryAccessor(store);
		ProcessHistory processLatest = historyAccessor.getLatestForProcess(processId);

		ProcessHistory advanceEntry = new ProcessHistory(processId, workflowUser, new Date().getTime(), processLatest.getOutcome(), AbstractWorkflowUtilities.getProcessConcludeAction(),
				AbstractWorkflowUtilities.getProcessConcludeState(), "");
		processHistoryStore.addEntry(advanceEntry);

	}
}

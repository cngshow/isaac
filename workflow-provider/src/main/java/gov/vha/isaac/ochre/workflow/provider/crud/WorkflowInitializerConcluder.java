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
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Utility to start, complete, or conclude a workflow process
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowInitializerConcluder}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowInitializerConcluder extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow initializer concluder.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowInitializerConcluder() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow initializer concluder.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowInitializerConcluder(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Start workflow.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param concepts
	 *            the concepts
	 * @param stampSequence
	 *            the stamp sequence
	 * @param user
	 *            the user
	 * @param subjectMatter
	 *            the subject matter
	 * @return the uuid
	 * @throws Exception
	 */
	public UUID defineWorkflow(UUID definitionId, Set<Integer> concepts, ArrayList<Integer> stampSequence, int user,
			SubjectMatter subjectMatter) throws Exception {
		WorkflowStatusAccessor accessor = new WorkflowStatusAccessor();

		for (Integer conSeq : concepts) {
			if (accessor.isConceptInActiveWorkflow(conSeq)) {
				throw new Exception("Concept to add in workflow exists in active workflow");
			}
		}
		ProcessDetail details = new ProcessDetail(definitionId, concepts, stampSequence, user, new Date().getTime(),
				subjectMatter, ProcessStatus.READY_TO_LAUNCH);
		UUID processId = processDetailStore.addEntry(details);

		return processId;
	}

	/**
	 * Launch workflow.
	 *
	 * @param processId
	 *            the process id
	 * @throws Exception
	 */
	public void launchWorkflow(UUID processId) throws Exception {
		ProcessDetail entry = processDetailStore.getEntry(processId);

		if (entry == null) {
			throw new Exception("Cannot launch workflow that hasn't been defined first");
		} else if (entry.getProcessStatus() != ProcessStatus.READY_TO_LAUNCH) {
			throw new Exception("Cannot launch workflow that has a process status of: " + entry.getProcessStatus());
		}

		entry.setProcessStatus(ProcessStatus.LAUNCHED);
		processDetailStore.updateEntry(processId, entry);

		ProcessHistory advanceEntry = new ProcessHistory(processId, entry.getCreator(), new Date().getTime(), "Start",
				"Started", "Ready for Edit", "");
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
	public void cancelWorkflow(UUID processId, String comment) throws Exception {
		ProcessDetail entry = processDetailStore.getEntry(processId);

		if (entry == null) {
			throw new Exception("Cannot cancel workflow that hasn't been defined first");
		} else if (entry.getProcessStatus() == ProcessStatus.CANCELED || entry.getProcessStatus() == ProcessStatus.CONCLUDED) {
			throw new Exception("Cannot cancel workflow that has a process status of: " + entry.getProcessStatus());
		}

		concludeWorkflow(processId);

		// TODO: Handle cancellation store and handle reverting automatically
	}

	/**
	 * Conclude workflow.
	 *
	 * @param processId
	 *            the process id
	 * @throws Exception
	 */
	public void concludeWorkflow(UUID processId) throws Exception {
		ProcessDetail entry = processDetailStore.getEntry(processId);

		if (entry == null) {
			throw new Exception("Cannot conclude workflow that hasn't been defined first");
		} else if (entry.getProcessStatus() != ProcessStatus.LAUNCHED) {
			throw new Exception("Cannot conclude workflow that has a process status of: " + entry.getProcessStatus());
		} else {
			WorkflowHistoryAccessor accessor = new WorkflowHistoryAccessor();

			if (!processConcludedStates.contains(accessor.getLatestForProcess(processId).getState())) {
				throw new Exception(
						"Cannot conclude workflow that has a process status of: " + entry.getProcessStatus());
			}
		}

		entry.setProcessStatus(ProcessStatus.CONCLUDED);
		entry.setTimeConcluded(new Date().getTime());
		processDetailStore.updateEntry(processId, entry);
	}
}

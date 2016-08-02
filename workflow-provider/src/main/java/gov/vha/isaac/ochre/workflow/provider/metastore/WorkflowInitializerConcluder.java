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

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
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
	 */
	public UUID defineWorkflow(UUID definitionId, Set<Integer> concepts, List<Integer> stampSequence, int user,
			SubjectMatter subjectMatter) {
		ProcessDetail details = new ProcessDetail(definitionId, concepts, stampSequence, user, new Date().getTime(),
				subjectMatter, ProcessStatus.READY_TO_LAUNCH);
		UUID processId = processDetailStore.addEntry(details);

		logger.info("Initializing Workflow " + processId + " with values: " + details.toString());
		return processId;
	}

	/**
	 * Launch workflow.
	 *
	 * @param processId
	 *            the process id
	 */
	public void launchWorkflow(UUID processId) {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		entry.setProcessStatus(ProcessStatus.LAUNCHED);
		processDetailStore.updateEntry(processId, entry);
	}

	/**
	 * Cancel workflow process.
	 *
	 * @param processId
	 *            the process id
	 * @param comment
	 *            the comment
	 */
	public void cancelWorkflowProcess(UUID processId, String comment) {
		concludeWorkflow(processId);
		logger.info("Canceling Workflow " + processId);

		// TODO: Handle cancellation store
	}

	/**
	 * Conclude workflow.
	 *
	 * @param processId
	 *            the process id
	 */
	void concludeWorkflow(UUID processId) {
		ProcessDetail entry = processDetailStore.getEntry(processId);
		entry.setProcessStatus(ProcessStatus.CONCLUDED);
		entry.setTimeConcluded(new Date().getTime());
		processDetailStore.updateEntry(processId, entry);
		logger.info("Concluding Workflow " + processId);
	}
}

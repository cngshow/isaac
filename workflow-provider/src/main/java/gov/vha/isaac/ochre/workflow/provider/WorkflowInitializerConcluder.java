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
package gov.vha.isaac.ochre.workflow.provider;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;

/**
 * Utility to start, complete, or conclude a workflow process
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowInitializerConcluder}
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
	 * @param concept
	 *            the concept
	 * @param stampSequence
	 *            the stamp sequence
	 * @param author
	 *            the author
	 * @return the uuid
	 */
	public UUID startWorkflow(UUID definitionId, UUID concept, List<Integer> stampSequence, int author) {
		ProcessDetail details = new ProcessDetail(definitionId, concept, stampSequence, author, new Date().getTime(),
				true);
		UUID processId = processDetailStore.addEntry(details);

		logger.info("Initializing Workflow " + processId + " with values: " + details.toString());
		return processId;
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
		ProcessDetail detail = processDetailStore.getEntry(processId);
		detail.setActive(false);
		detail.setTimeConcluded(new Date().getTime());
		logger.info("Concluding Workflow " + processId);
	}
}
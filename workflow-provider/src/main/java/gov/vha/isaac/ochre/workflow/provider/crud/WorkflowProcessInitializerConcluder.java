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
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.ProcessStatus;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Utility to start, complete, or conclude a workflow process
 * 
 * {@link AbstractWorkflowUtilities}
 * {@link WorkflowProcessInitializerConcluder}.
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
	 * @param components
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
	public UUID createWorkflowProcess(UUID definitionId, int user, String name, String description, StartWorkflowType type) throws Exception {
    	if (name == null || name.isEmpty() || description == null || description.isEmpty()) {
    		throw new Exception("Name and Description must be filled out when creating a process");
    	}
    
    	// Create Process Details with "DEFINED"
    	StorableWorkflowContents details = new ProcessDetail(definitionId, user, new Date().getTime(),
    			ProcessStatus.DEFINED, name, description);
    	UUID processId = processDetailStore.addEntry(details);
    
    	// Add Process History with START_STATE-AUTOMATED-EDIT_STATE
    	AvailableAction startAdvancement = getStartState(type);
    	ProcessHistory advanceEntry = new ProcessHistory(processId, user, new Date().getTime(),
    			startAdvancement.getCurrentState(), startAdvancement.getAction(), startAdvancement.getOutcome(), "");
    	processHistoryStore.addEntry(advanceEntry);
    
    	return processId;
	}

	/**
	 * Launch a workflow process.
	 *
	 * @param processId
	 *            the process id
	 * @throws Exception
	 */
	protected void launchWorkflowProcess(UUID processId) throws Exception {
		ProcessDetail entry = processDetailStore.getEntry(processId);

		if (entry == null) {
			throw new Exception("Cannot launch workflow that hasn't been defined first");
		} else if (entry.getComponentToStampMap().isEmpty()) {
			throw new Exception("Workflow can only be launched when the workflow contains components to work on");
		}

    	// Update Process Details with "LAUNCHED"
		entry.setStatus(ProcessStatus.LAUNCHED);
		entry.setTimeLaunched(new Date().getTime());
		processDetailStore.updateEntry(processId, entry);
	}

	/**
	 * Cancel workflow process.
	 *
	 * @param processId
	 *            the process id
	 * @param actionToProcess 
	 * @param comment
	 *            the comment
	 * @throws Exception
	 */
	protected void finishWorkflowProcess(UUID processId, AvailableAction actionToProcess, int workflowUser, String comment, EndWorkflowType endType) throws Exception {
		ProcessDetail entry = processDetailStore.getEntry(processId);

		if (entry == null) {
			throw new Exception("Cannot cancel workflow that hasn't been defined and launched first");
		} else if (entry.getStatus() != ProcessStatus.LAUNCHED) {
			throw new Exception("Cannot cancel workflow that has a process status of: " + entry.getStatus());
		}

		if (endType.equals(EndWorkflowType.CANCELED)) {
			entry.setStatus(ProcessStatus.CANCELED);
		} else if (endType.equals(EndWorkflowType.CONCLUDED)) {
			entry.setStatus(ProcessStatus.CONCLUDED);
		}
		entry.setTimeCanceledOrConcluded(new Date().getTime());
		processDetailStore.updateEntry(processId, entry);

		// Only add Cancel state in Workflow if process has already been
		// launched
		ProcessHistory advanceEntry = new ProcessHistory(processId, workflowUser, new Date().getTime(),
				actionToProcess.getCurrentState(), actionToProcess.getAction(), actionToProcess.getOutcome(), comment);
		processHistoryStore.addEntry(advanceEntry);

		if (endType.equals(EndWorkflowType.CANCELED)) {
			// TODO: Handle cancelation store and handle reverting automatically
		}
	}

	private AvailableAction getStartState(StartWorkflowType type) throws Exception {
		switch (type) {
		
		case SINGLE_CASE: 
			 return getStartWorkflowTypeMap().get(type); 
			 
		default:
				 throw new Exception("Unable to discren the Workflow Start State based on the StartWorkflowType: " + type);
		 
		}
	}
}

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

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Utility to access workflow data from content stores
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowStatusAccessor}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowStatusAccessor extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow status accessor.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowStatusAccessor() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow status accessor.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowStatusAccessor(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Gets the processes for concept.
	 *
	 * @param conceptId
	 *            the concept id
	 * @return the processes for concept
	 */
	public SortedSet<ProcessDetail> getProcessesForConcept(int conceptId) {
		SortedSet<ProcessDetail> allProcessesByConcept = new TreeSet<>(new ProcessDetail.ProcessDetailComparator());

		for (ProcessDetail process : processDetailStore.getAllEntries()) {
			if (process.getConcepts().contains(conceptId)) {
				allProcessesByConcept.add(process);
			}
		}

		return allProcessesByConcept;
	}

	/**
	 * Gets the process detail.
	 *
	 * @param processId
	 *            the process id
	 * @return the process detail
	 */
	public ProcessDetail getProcessDetail(UUID processId) {
		return processDetailStore.getEntry(processId);
	}

	/**
	 * Gets the process detail.
	 *
	 * @param definitionId
	 *            the definition id
	 * @return the process detail
	 */
	public Set<ProcessDetail> getActiveProcessesByDefinition(UUID definitionId) {
		Set<ProcessDetail> activeProcesses = new HashSet<>();

		for (ProcessDetail process : processDetailStore.getAllEntries()) {
			if (process.isActive() && process.getDefinitionId().equals(definitionId)) {
				activeProcesses.add(process);
			}
		}

		return activeProcesses;
	}

	/**
	 * Gets the definition.
	 *
	 * @param definitionId
	 *            the definition id
	 * @return the definition
	 */
	public DefinitionDetail getDefinition(UUID definitionId) {
		return definitionDetailStore.getEntry(definitionId);
	}

	/**
	 * Checks if is concept in active workflow.
	 *
	 * @param conceptId
	 *            the concept id
	 * @return true, if is concept in active workflow
	 */
	public boolean isConceptInActiveWorkflow(int conceptId) {
		for (ProcessDetail proc : getProcessesForConcept(conceptId)) {
			if (proc.getProcessStatus() != ProcessStatus.LAUNCHED && 
				proc.getProcessStatus() != ProcessStatus.READY_TO_LAUNCH) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if is component in active workflow.
	 *
	 * @param componentId
	 *            the component id
	 * @return true, if is component in active workflow
	 */
	public boolean isComponentInActiveWorkflow(int componentId) {
		ObjectChronologyType type = Get.identifierService().getChronologyTypeForNid(componentId);

		if (type != ObjectChronologyType.CONCEPT) {
			return isConceptInActiveWorkflow(componentId);
		} else if (type == ObjectChronologyType.UNKNOWN_NID) {
			return isConceptInActiveWorkflow(Get.sememeService().getSememe(componentId).getReferencedComponentNid());
		} else if (type == ObjectChronologyType.UNKNOWN_NID) {
			throw new RuntimeException("Couldn't determine component type from componentId '" + componentId + "'");
		}

		return false;
	}


	/**
	 * Gets the active for concept.
	 *
	 * @param conceptId
	 *            the concept id
	 * @return the active for concept
	 */
	public ProcessDetail getActiveProcessForConcept(int conceptId) {

		for (ProcessDetail proc : getProcessesForConcept(conceptId)) {
			if (proc.getProcessStatus() == ProcessStatus.LAUNCHED || 
				proc.getProcessStatus() == ProcessStatus.READY_TO_LAUNCH) {
				return proc;
			}
		}

		return null;
	}
}

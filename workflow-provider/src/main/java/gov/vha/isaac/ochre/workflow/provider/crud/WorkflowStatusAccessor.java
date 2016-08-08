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
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
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
	 * @param conceptSeq
	 *            the concept sequence
	 * @return the processes for concept
	 */
	public SortedSet<ProcessDetail> getProcessesForConcept(int conceptSeq) {
		SortedSet<ProcessDetail> allProcessesByConcept = new TreeSet<>(new ProcessDetail.ProcessDetailComparator());

		for (ProcessDetail process : processDetailStore.getAllEntries()) {
			if (process.getConceptSequences().contains(conceptSeq)) {
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
	 * @param conceptSeq
	 *            the concept sequence
	 * @return true, if is concept in active workflow
	 */
	public boolean isConceptInActiveWorkflow(OchreExternalizableObjectType type, int conceptSeq) throws Exception {
		if (type != OchreExternalizableObjectType.CONCEPT) {
			throw new Exception("concept: " + conceptSeq + " is not of OchreExternalizableObjectType.CONCEPT type");
		}
		for (ProcessDetail proc : getProcessesForConcept(conceptSeq)) {
			if (proc.getProcessStatus() == ProcessStatus.LAUNCHED || 
				proc.getProcessStatus() == ProcessStatus.READY_TO_LAUNCH) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if is component in active workflow.
	 *
	 * @param componentSequence
	 *            the component sequence
	 * @return true, if is component in active workflow
	 * @throws Exception 
	 */
	public boolean isComponentInActiveWorkflow(OchreExternalizableObjectType type, int componentSequence) throws Exception {
		if (type == OchreExternalizableObjectType.CONCEPT) {
			return isConceptInActiveWorkflow(type, componentSequence);
		} else if (type == OchreExternalizableObjectType.SEMEME) {
			int conSeq = Get.conceptService().getConcept( Get.sememeService().getSememe(componentSequence).getReferencedComponentNid()).getConceptSequence();
			return isConceptInActiveWorkflow(OchreExternalizableObjectType.CONCEPT, conSeq);
		} else {
			throw new RuntimeException("Couldn't determine component type from componentId '" + componentSequence + "'");
		}
	}


	/**
	 * Gets the active for concept.
	 *
	 * @param conceptSequence
	 *            the concept sequence
	 * @return the active for concept
	 */
	public ProcessDetail getActiveProcessForConcept(int conceptSequence) {

		for (ProcessDetail proc : getProcessesForConcept(conceptSequence)) {
			if (proc.getProcessStatus() == ProcessStatus.LAUNCHED || 
				proc.getProcessStatus() == ProcessStatus.READY_TO_LAUNCH) {
				return proc;
			}
		}

		return null;
	}
}

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
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.ProcessStatus;
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
	public SortedSet<ProcessDetail> getProcessesForComponent(int componentSeq) {
		SortedSet<ProcessDetail> allProcessesByConcept = new TreeSet<>(new ProcessDetail.ProcessDetailComparator());

		for (ProcessDetail process : processDetailStore.getAllEntries()) {
			if (process.getComponentToStampMap().containsKey(componentSeq)) {
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
	public Set<ProcessDetail> getActiveProcessesForDefinition(UUID definitionId) {
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
	 * Checks if is component in active workflow.
	 *
	 * @param componentSequence
	 *            the component sequence
	 * @return true, if is component in active workflow
	 * @throws Exception 
	 */
	public boolean isComponentInActiveWorkflow(int componentSequence) throws Exception {
		if (componentSequence < 0) {
			throw new Exception("Expecting Component Sequences, not Component Nids");
		}
		
		for (ProcessDetail proc : getProcessesForComponent(componentSequence)) {
			if (proc.getStatus() == ProcessStatus.LAUNCHED || 
				proc.getStatus() == ProcessStatus.DEFINED) {
				return true;
			}
		}
		
		return false;
	
		/*
		int componentNid = Get.identifierService().getConceptNid(componentSequence);
		ObjectChronologyType type = Get.identifierService().getChronologyTypeForNid(componentNid);
		


		if (type == ObjectChronologyType.CONCEPT) {
			return isConceptInActiveWorkflow(componentSequence);
		} else if (type == ObjectChronologyType.SEMEME) {
			//TODO this isn't safe - referenced component may not be a concept.
			//Also, Dan wants to know, are we only putting concepts into workflow?  Or do we put sememes into workflow??
			//If only concepts go into workflow, how do we know what component of the concept is in workflow??
			int conSeq = Get.conceptService().getConcept( Get.sememeService().getSememe(componentSequence).getReferencedComponentNid()).getConceptSequence();
			return isConceptInActiveWorkflow(conSeq);
		} else {
			throw new RuntimeException("Couldn't determine component type from componentId '" + componentSequence + "'");
		}
		*/
	}


	/**
	 * Gets the active for concept.
	 *
	 * @param conceptSequence
	 *            the concept sequence
	 * @return the active for concept
	 */
	public ProcessDetail getActiveProcessForConcept(int conceptSequence) {

		for (ProcessDetail proc : getProcessesForComponent(conceptSequence)) {
			if (proc.getStatus() == ProcessStatus.LAUNCHED || 
				proc.getStatus() == ProcessStatus.DEFINED) {
				return proc;
			}
		}

		return null;
	}
}

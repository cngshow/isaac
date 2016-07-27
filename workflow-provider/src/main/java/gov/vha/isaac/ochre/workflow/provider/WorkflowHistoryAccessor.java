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

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;

/**
 * Utility to access workflow history from content stores
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowHistoryAccessor}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowHistoryAccessor extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow history accessor.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowHistoryAccessor() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow history accessor.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowHistoryAccessor(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Gets the active by process.
	 *
	 * @return the active by process
	 */
	public Map<UUID, SortedSet<ProcessHistory>> getActiveByProcess() {
		Map<UUID, SortedSet<ProcessHistory>> allHistoryByProcess = new HashMap<>();
		Map<UUID, Boolean> processActivationMap = new HashMap<>();

		for (ProcessHistory hx : processHistoryStore.getAllEntries()) {
			if (!processActivationMap.containsKey(hx.getProcessId())) {
				// First time seeing processId so setup
				ProcessDetail process = processDetailStore.getEntry(hx.getProcessId());

				if (!process.isActive()) {
					processActivationMap.put(process.getId(), false);
				} else {
					processActivationMap.put(process.getId(), true);

					// Active process. But since first time seeing it, setup the
					// history set
					SortedSet<ProcessHistory> processHistory = new TreeSet<>(
							new ProcessHistory.ProcessHistoryComparator());
					allHistoryByProcess.put(hx.getProcessId(), processHistory);
				}
			}

			if (processActivationMap.get(hx.getProcessId())) {
				allHistoryByProcess.get(hx.getProcessId()).add(hx);
			}
		}

		return allHistoryByProcess;
	}

	/**
	 * Gets the active by definition.
	 *
	 * @return the active by definition
	 */
	public Map<UUID, SortedSet<ProcessHistory>> getActiveByDefinition() {
		Map<UUID, SortedSet<ProcessHistory>> allHistoryByDefinition = new HashMap<>();
		Map<UUID, Boolean> processActivationMap = new HashMap<>();

		for (ProcessHistory hx : processHistoryStore.getAllEntries()) {
			if (!processActivationMap.containsKey(hx.getProcessId())) {
				// First time seeing processId so setup
				ProcessDetail process = processDetailStore.getEntry(hx.getProcessId());

				if (!process.isActive()) {
					processActivationMap.put(process.getId(), false);
				} else {
					processActivationMap.put(process.getId(), true);

					// Active process. But since first time seeing it, setup the
					// history set
					SortedSet<ProcessHistory> processHistory = new TreeSet<>(
							new ProcessHistory.ProcessHistoryComparator());
					allHistoryByDefinition.put(hx.getProcessId(), processHistory);
				}
			}

			if (processActivationMap.get(hx.getProcessId())) {
				allHistoryByDefinition.get(hx.getProcessId()).add(hx);
			}
		}

		return allHistoryByDefinition;
	}

	/**
	 * Gets the active for concept.
	 *
	 * @param conceptId
	 *            the concept id
	 * @return the active for concept
	 */
	public SortedSet<ProcessHistory> getActiveForConcept(int conceptId) {
		SortedSet<ProcessHistory> allHistoryForConcept = new TreeSet<>(new ProcessHistory.ProcessHistoryComparator());

		for (ProcessDetail process : processDetailStore.getAllEntries()) {
			if (process.getConcepts().contains(conceptId) && process.isActive()) {
				allHistoryForConcept.addAll(getForProcess(process.getId()));
			}
		}

		return allHistoryForConcept;
	}

	/**
	 * Gets the by process map.
	 *
	 * @return the by process map
	 */
	public Map<UUID, SortedSet<ProcessHistory>> getByProcessMap() {
		Map<UUID, SortedSet<ProcessHistory>> allHistoryByProcess = new HashMap<>();

		for (ProcessHistory hx : processHistoryStore.getAllEntries()) {
			if (!allHistoryByProcess.containsKey(hx.getProcessId())) {
				SortedSet<ProcessHistory> processHistory = new TreeSet<>(new ProcessHistory.ProcessHistoryComparator());
				allHistoryByProcess.put(hx.getProcessId(), processHistory);
			}

			allHistoryByProcess.get(hx.getProcessId()).add(hx);
		}

		return allHistoryByProcess;
	}

	/**
	 * Gets the by definition map.
	 *
	 * @return the by definition map
	 */
	public Map<UUID, SortedSet<ProcessHistory>> getByDefinitionMap() {
		Map<UUID, SortedSet<ProcessHistory>> allHistoryByDefinition = new HashMap<>();

		for (ProcessHistory hx : processHistoryStore.getAllEntries()) {
			if (!allHistoryByDefinition.containsKey(hx.getProcessId())) {
				SortedSet<ProcessHistory> processHistory = new TreeSet<>(new ProcessHistory.ProcessHistoryComparator());
				allHistoryByDefinition.put(hx.getProcessId(), processHistory);
			}

			allHistoryByDefinition.get(hx.getProcessId()).add(hx);
		}

		return allHistoryByDefinition;
	}

	/**
	 * Gets the by concept map.
	 *
	 * @return the by concept map
	 */
	public Map<Integer, SortedSet<ProcessHistory>> getByConceptMap() {
		throw new UnsupportedOperationException(
				"Should not attempt to get all history by concept as return object would be enormous");
	}

	/**
	 * Gets the for concept.
	 *
	 * @param conceptId
	 *            the concept id
	 * @return the for concept
	 */
	public SortedSet<ProcessHistory> getForConcept(int conceptId) {
		SortedSet<ProcessHistory> allHistoryForConcept = new TreeSet<>(new ProcessHistory.ProcessHistoryComparator());

		for (ProcessDetail process : processDetailStore.getAllEntries()) {
			if (process.getConcepts().contains(conceptId)) {
				allHistoryForConcept.addAll(getForProcess(process.getId()));
			}
		}

		return allHistoryForConcept;
	}

	/**
	 * Gets the for process.
	 *
	 * @param processId
	 *            the process id
	 * @return the for process
	 */
	public SortedSet<ProcessHistory> getForProcess(UUID processId) {
		SortedSet<ProcessHistory> allHistoryForProcess = new TreeSet<>(new ProcessHistory.ProcessHistoryComparator());

		for (ProcessHistory hx : processHistoryStore.getAllEntries()) {
			if (hx.getProcessId().equals(processId)) {
				allHistoryForProcess.add(hx);
			}
		}

		return allHistoryForProcess;
	}

	/**
	 * Gets the latest for process.
	 *
	 * @param processId
	 *            the process id
	 * @return the latest for process
	 */
	public ProcessHistory getLatestForProcess(UUID processId) {
		return getForProcess(processId).last();
	}

}

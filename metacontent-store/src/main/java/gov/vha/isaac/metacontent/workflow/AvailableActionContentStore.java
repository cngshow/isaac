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
package gov.vha.isaac.metacontent.workflow;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;

/**
 * Workflow-based Data Store containing the available actions based on role and
 * initial state. Initialized during the importing of a BPMN2 file (containing
 * the definition) and static from then on.
 * 
 * {@link AvailableAction} {@link AbstractWorkflowContentStore}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class AvailableActionContentStore extends AbstractWorkflowContentStore {

	/**
	 * Constructor for the content store.
	 * 
	 * @param single
	 *            store where all workflow content, regardless of type, is
	 *            stored
	 */
	public AvailableActionContentStore(MVStoreMetaContentProvider store) {
		super(store, WorkflowContentStoreType.AVAILABLE_ACTION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.metacontent.workflow.AbstractWorkflowContentStore#getEntry(
	 * java.util.UUID)
	 */
	@Override
	public AvailableAction getEntry(UUID key) {
		return new AvailableAction(getSerializedEntry(key));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.metacontent.workflow.AbstractWorkflowContentStore#
	 * getAllEntries()
	 */
	@Override
	public Set<AvailableAction> getAllEntries() {
		Set<AvailableAction> retSet = new HashSet<>();

		Collection<byte[]> entries = getAllGenericEntries();

		for (byte[] entry : entries) {
			retSet.add(new AvailableAction(entry));
		}

		return retSet;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		int i = 1;
		for (UUID key : keySet()) {
			buf.append("\n\tAvailable Action #" + i++ + ": " + getEntry(key).toString());
			buf.append("\n\n");
		}

		return "AvailableActionContentStore: " + buf.toString();
	}
}
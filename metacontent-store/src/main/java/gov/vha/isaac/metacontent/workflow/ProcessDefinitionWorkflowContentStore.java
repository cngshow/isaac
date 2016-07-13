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
import gov.vha.isaac.metacontent.workflow.contents.ProcessDefinition;

/**
 * Dynamic workflow processes populated at runtime only
 *
 * {@link ProcessDefinitionWorkflowContentStore}
 * {@link AbstractWorkflowContentStore}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class ProcessDefinitionWorkflowContentStore extends AbstractWorkflowContentStore {

	public ProcessDefinitionWorkflowContentStore(MVStoreMetaContentProvider store) {
		super(store, WorkflowContentStoreType.PROCESS_DEFINITION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.metacontent.workflow.AbstractWorkflowContentStore#getEntry(
	 * java.util.UUID)
	 */
	@Override
	public ProcessDefinition getEntry(UUID key) {
		return new ProcessDefinition(getGenericEntry(key));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.metacontent.workflow.AbstractWorkflowContentStore#
	 * getAllEntries()
	 */
	@Override
	public Set<ProcessDefinition> getAllEntries() {
		Set<ProcessDefinition> retSet = new HashSet<>();

		Collection<byte[]> entries = getAllGenericEntries();

		for (byte[] entry : entries) {
			retSet.add(new ProcessDefinition(entry));
		}

		return retSet;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		int i = 1;
		for (UUID key : keySet()) {
			buf.append("\n\tKey #" + i++ + ": " + key.toString());
			buf.append("\n\tProcess Definition" + i++ + ": " + getEntry(key).toString());
			buf.append("\n\n");
		}

		return "ProcessDefinitionWorkflowContentStore: " + buf.toString();
	}

}

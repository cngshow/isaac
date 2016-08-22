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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.metacontent.workflow;

/**
 * Entries for Workflow Content Store
 * 
 * {@link AvailableAction}
 * {@link UserPermission}
 * {@link HistoricalWorkflow} 
 * {@link ProcessInstance}
 * {@link DefinitionDetail}
 * {@link DomainStandard}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a> 
 */
import java.io.IOException;
import java.util.UUID;

/**
 * The Interface StorableWorkflowContents.
 */
public abstract class StorableWorkflowContents {
	/**
	 * The Enum SubjectMatter.
	 */
	public enum SubjectMatter {
	
		/** The mapping. */
		MAPPING,
		/** The concept. */
		CONCEPT
	}

	/**
	 * The Enum DefiningStatus.
	 */
	public enum ProcessStatus {
	
		DEFINED, LAUNCHED, CANCELED, CONCLUDED
	}

	/** The id. */
	protected UUID id;

	/**
	 * Turn the concrete class into a suitable byte[] for storage.
	 *
	 * @return the byte[]
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public abstract byte[] serialize() throws IOException;

	/**
	 * Sets the id.
	 *
	 * @param key
	 *            the new id
	 */
	public void setId(UUID key) {
		id = key;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public UUID getId() {
		return id;
	}
}

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
 * Classes extending this abstract class.  Represent all Workflow Content Stores
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
import java.text.SimpleDateFormat;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An abstract class extended by all Workflow Content Store Entry classes.
 * Contains fields and methods shared by all such Entries .
 * 
 * {@link UserPermission} {@link AvailableAction} {@link ProcessHistory}
 * {@link ProcessDetail} {@link DefinitionDetail} {@link DomainStandard}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class StorableWorkflowContents {
	/** The Logger made available to each Workflow Content Store Entry class */
	protected final Logger logger = LogManager.getLogger();

	/** A universal means of expressing a workflow time stamp */
	public SimpleDateFormat workflowDateFormatrer = new SimpleDateFormat("hh:mm:ssa MM/dd/yy");

	/**
	 * As every content store entry is key-value based and as all keys are of
	 * type UUID, add in abstract
	 */
	protected UUID id;

	/**
	 * Ensure all extending classes are serialized
	 * 
	 * @return
	 * @throws IOException
	 */
	public abstract byte[] serialize() throws IOException;

	/**
	 * Set an entry's key
	 * 
	 * @param key
	 *            to each content-store entry
	 */
	public void setId(UUID key) {
		id = key;
	}

	/**
	 * Return an entry's key
	 * 
	 * @return content-store entry key
	 */
	public UUID getId() {
		return id;
	}
}
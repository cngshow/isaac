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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * An abstract class extended by all Workflow Content Store classes. Contains
 * fields and methods shared by all such Content Stores.
 * 
 * {@link UserPermissionContentStore} {@link AvailableActionContentStore}
 * {@link ProcessHistoryContentStore} {@link ProcessDetailContentStore}
 * {@link DefinitionDetailContentStore} {@link DomainStandardContentStore}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowContentStore {
	/** The Logger made available to each Workflow Content Store class */
	protected static final Logger logger = LogManager.getLogger();

	/** The Enum listing each Workflow Content Store Type. */
	protected static enum WorkflowContentStoreType {
		USER_PERMISSION, AVAILABLE_ACTION, DEFINITION_DETAIL, DOMAIN_STANDARD, HISTORICAL_WORKFLOW, PROCESS_DEFINITION
	};

	/**
	 * The storage mechanism of all entries. It is a map of key to Content Store
	 * Entry type.
	 */
	private ConcurrentMap<UUID, byte[]> map = null;

	/** The actual content store for all workflow content stores. */
	protected MVStoreMetaContentProvider store = null;

	/**
	 * Instantiates a new workflow content store based on the type requested.
	 *
	 * @param store
	 *            The storage facility for all workflow-based content stores
	 * 
	 * @param type
	 *            The type of workflow content store being instantiated
	 */
	public AbstractWorkflowContentStore(MVStoreMetaContentProvider store, WorkflowContentStoreType type) {
		if (this.store == null) {
			this.store = store;
		}

		if (map == null) {
			map = store.<UUID, byte[]>openStore(type.toString());
		}
	}

	/**
	 * Gets the entry based on the key. Returns an object that is casted based
	 * on the appropriate WorkflowContentStore type
	 *
	 * @param key
	 *            defining the entry to retrieve
	 * 
	 * @return the entry requested as an Object class
	 */
	abstract public Object getEntry(UUID key);

	/**
	 * Gets the all entries of the Content Store Type.
	 *
	 * @return all the entries
	 */
	abstract public Collection<?> getAllEntries();

	/**
	 * Gets the number of entries of the Content Store Type.
	 *
	 * @return number of entries
	 */
	public int getNumberOfEntries() {
		return map.size();
	}

	/**
	 * Adds a new entry to the content store. Key is generated and returned.
	 *
	 * @param entry
	 *            the already populated entry which is to be added
	 *
	 * @return the key of the new entry
	 */
	public UUID addEntry(StorableWorkflowContents entry) {
		UUID key = UUID.randomUUID();
		entry.setId(key);

		try {
			map.put(key, entry.serialize());
		} catch (IOException e) {
			logger.error("Failed to serialize object during add: " + entry, e);
			e.printStackTrace();
		}

		return key;
	}

	/**
	 * Updates an existing entry as specified defined by the key
	 *
	 * @param key
	 *            the key of the entry being updated
	 * @param entry
	 *            the updated contents of the entry
	 */
	public void updateEntry(UUID key, StorableWorkflowContents entry) {
		try {
			map.put(key, entry.serialize());
		} catch (IOException e) {
			logger.error("Failed to serialize object during update: " + entry, e);
			e.printStackTrace();
		}
	}

	/**
	 * Removes the entry specified by the key.
	 *
	 * @param key
	 *            specifying the entry to be removed
	 */
	public void removeEntry(UUID key) {
		map.remove(key);
	}

	/**
	 * Removes every single entry of the content store type.
	 */
	public void removeAllEntries() {
		map.clear();
	}

	/**
	 * Gets the entry based on the key. Returns the object in a serialized
	 * manner.
	 *
	 * @param key
	 *            the key to the entry
	 * 
	 * @return the serialized entry requested
	 */
	protected byte[] getSerializedEntry(UUID key) {
		return map.get(key);
	}

	/**
	 * Gets all entries in a serialized manner.
	 *
	 * @return all entries in a serialized manner
	 */
	protected Collection<byte[]> getAllGenericEntries() {
		return map.values();
	}

	/**
	 * Returns the set of all entry keys.
	 *
	 * @return keys of all entries
	 */
	protected Set<UUID> keySet() {
		return map.keySet();
	}

	/**
	 * Gets the new uuid.
	 *
	 * @param hashCode
	 *            the hash code
	 * @return the new uuid
	 */
	private UUID getNewUUID(int hashCode) {
		// TODO: Decide if need reproducible UUID for contentStores. If not,
		// remove method. If so, use in addEntry() in place of UUID.randomUUID()
		// call. May want to make this abstract so can seed UUID with each part
		// of entry
		return UUID.randomUUID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		AbstractWorkflowContentStore other = (AbstractWorkflowContentStore) obj;

		return this.map.equals(other.map);
	}

	public void close() {
		store = null;
		map = null;
	}
}

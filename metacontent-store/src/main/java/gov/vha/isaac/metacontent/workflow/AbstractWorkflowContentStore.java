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
 * Abstract Content Store specific to Workflow to avoid repeated functionality.
 * 
 * {@link UserPermissionWorkflowContentStore}
 * {@link AvailableActionWorkflowContentStore}
 * {@link ProcessHistoryContentStore} {@link ProcessDetailWorkflowContentStore}
 * {@link DefinitionDetailWorkflowContentStore}
 * {@link DomainStandardWorkflowContentStore}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public abstract class AbstractWorkflowContentStore {

	/**
	 * The Enum WorkflowContentStoreType.
	 */
	protected static enum WorkflowContentStoreType {
		USER_PERMISSION, AVAILABLE_ACTION, DEFINITION_DETAIL, DOMAIN_STANDARD, HISTORICAL_WORKFLOW, PROCESS_DEFINITION
	};

	/** The map. */
	private ConcurrentMap<UUID, byte[]> map = null;

	/** The store. */
	protected MVStoreMetaContentProvider store = null;

	/** The Constant logger. */
	protected static final Logger logger = LogManager.getLogger();

	/**
	 * Instantiates a new abstract workflow content store.
	 *
	 * @param store
	 *            the store
	 * @param type
	 *            the type
	 */
	public AbstractWorkflowContentStore(MVStoreMetaContentProvider store, WorkflowContentStoreType type) {
		if (this.store == null) {
			this.store = store;
		}

		if (map == null) {
			map = store.<UUID, byte[]> openStore(type.toString());
		}
	}

	/**
	 * Gets the entry.
	 *
	 * @param key
	 *            the key
	 * @return the entry
	 */
	abstract public Object getEntry(UUID key);

	/**
	 * Gets the all entries.
	 *
	 * @return the all entries
	 */
	abstract public Collection<?> getAllEntries();

	/**
	 * Gets the number of entries.
	 *
	 * @return the number of entries
	 */
	public int getNumberOfEntries() {
		return map.size();
	}

	/**
	 * Adds the entry.
	 *
	 * @param entry
	 *            the entry
	 * @return the uuid
	 */
	public UUID addEntry(StorableWorkflowContents entry) {
		UUID key = getNewUUID(entry.hashCode());
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
	 * Update entry.
	 *
	 * @param key
	 *            the key
	 * @param entry
	 *            the entry
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
	 * Removes the entry.
	 *
	 * @param key
	 *            the key
	 */
	public void removeEntry(UUID key) {
		map.remove(key);
	}

	/**
	 * Removes the all entries.
	 */
	public void removeAllEntries() {
		map.clear();
	}

	/**
	 * Gets the generic entry.
	 *
	 * @param key
	 *            the key
	 * @return the generic entry
	 */
	protected byte[] getGenericEntry(UUID key) {
		return map.get(key);
	}

	/**
	 * Gets the all generic entries.
	 *
	 * @return the all generic entries
	 */
	protected Collection<byte[]> getAllGenericEntries() {
		return map.values();
	}

	/**
	 * Key set.
	 *
	 * @return the sets the
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
}

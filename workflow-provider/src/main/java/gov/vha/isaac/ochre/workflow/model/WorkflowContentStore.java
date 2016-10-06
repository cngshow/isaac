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
package gov.vha.isaac.ochre.workflow.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gov.vha.isaac.ochre.workflow.model.contents.AbstractStorableWorkflowContents;

/**
 * An generic storage class utilized to store all Workflow Content Store classes. 
 * Contains fields and methods shared by all such Content Stores.
 * 
 * Implements the Map interface, plus a couple of other convenience methods
 * 
 * {@link UserPermissionContentStore} {@link AvailableActionContentStore}
 * {@link ProcessHistoryContentStore} {@link ProcessDetailContentStore}
 * {@link DefinitionDetailContentStore} {@link DomainStandardContentStore}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowContentStore<T extends AbstractStorableWorkflowContents> implements Map<UUID, T> {
	/** The Logger made available to each Workflow Content Store class */
	protected final Logger logger = LogManager.getLogger();

	/**
	 * The storage mechanism of all entries. It is a map of key to Content Store
	 * Entry type.  This map is backed by the metacontent store.
	 */
	private ConcurrentMap<UUID, byte[]> map = null;

	private Function<byte[], T> deserializer_;
	
	/**
	 * Constructor for each new workflow content store based on the type
	 * requested.
	 *
	 * @param type
	 *            The type of workflow content store being instantiated
	 * @param deserializer
	 *            How to create a new object of type T from the submitted bytes
	 */
	public WorkflowContentStore(ConcurrentMap<UUID, byte[]> dataStore, Function<byte[], T> deserializer) {
		deserializer_ = deserializer;
		map = dataStore;
	}

	/**
	 * Adds a new entry to the content store. 
	 * 
	 * Key is generated and returned (and injected into the object) via {@link AbstractStorableWorkflowContents#setId(UUID)}
	 *
	 * @param entry
	 *            The entry the already populated entry which is to be added
	 *
	 * @return the key of the new entry
	 */
	public UUID add(T entry) {
		if (entry.getId() == null)
		{
			entry.setId(UUID.randomUUID());
		}
		
		map.put(entry.getId(), entry.getDataToWrite());
		return entry.getId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WorkflowContentStore)
		{
			@SuppressWarnings("unchecked")
			WorkflowContentStore<T> other = (WorkflowContentStore<T>) obj;
			return this.map.equals(other.map);
		}
		else
		{
			return false;
		}
	}
	

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		int i = 1;
		for (UUID key : keySet()) {
			buf.append("\n\tStored Item #" + i++ + ": " + get(key).toString());
			buf.append("\n\n");
		}

		return "Stored Items: " + buf.toString();
	}

	/**
	 * @see java.util.Map#keySet()
	 */
	@Override
	public Set<UUID> keySet() {
		return map.keySet();
	}

	/**
	 * @see java.util.Map#size()
	 */
	@Override
	public int size()
	{
		return map.size();
	}

	/**
	 * @see java.util.Map#isEmpty()
	 */
	@Override
	public boolean isEmpty()
	{
		return map.isEmpty();
	}

	/**
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	@Override
	public boolean containsKey(Object key)
	{
		return map.containsKey(key);
	}

	/**
	 * Unsupported in this impl.
	 */
	@Override
	public boolean containsValue(Object value)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * @see java.util.Map#get(java.lang.Object)
	 */
	@Override
	public T get(Object key)
	{
		return deserializer_.apply(map.get(key));
	}

	/**
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public T put(UUID key, T value)
	{
		if (value.getId() == null)
		{
			value.setId(key);
		}
		else if (!key.equals(value.getId()))
		{
			throw new RuntimeException("Attempt to store an object with a mis-matched key");
		}
		return deserializer_.apply(map.put(key, value.getDataToWrite()));
	}

	/**
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	@Override
	public T remove(Object key)
	{
		return deserializer_.apply(map.remove(key));
	}

	/**
	 * Not implemented in this implementation
	 */
	@Override
	public void putAll(Map<? extends UUID, ? extends T> m)
	{
		throw new UnsupportedOperationException();
		
	}

	/**
	 * @see java.util.Map#clear()
	 */
	@Override
	public void clear()
	{
		map.clear();
	}

	/**
	 * @see java.util.Map#values()
	 */
	@Override
	public Collection<T> values()
	{
		return map.values().stream().map((bytes) -> deserializer_.apply(bytes)).collect(Collectors.toList());
	}

	/**
	 * @see java.util.Map#entrySet()
	 */
	@Override
	public Set<java.util.Map.Entry<UUID, T>> entrySet()
	{
		HashMap<UUID, T> retSet = new HashMap<>();

		for (java.util.Map.Entry<UUID, byte[]> x : map.entrySet()) {
			retSet.put(x.getKey(), deserializer_.apply(x.getValue()));
		}
		return retSet.entrySet();
	}
}

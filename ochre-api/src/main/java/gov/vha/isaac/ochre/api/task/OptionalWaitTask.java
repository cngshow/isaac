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
package gov.vha.isaac.ochre.api.task;

import java.util.concurrent.ExecutionException;
import javafx.concurrent.Task;

/**
 * {@link OptionalWaitTask}
 * This task services the purpose of allowing an object that is created 
 * to be returned immediately, if the caller wishes, or wait for another 
 * subtask related to the created object.
 * 
 * A use case for this is in the object builders - the object is created and read, 
 * but a subtask is issued to write the object to disk.
 * 
 * Some callers may not care to wait for the write to disk, while others may.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public abstract class OptionalWaitTask<T> extends Task<T>
{
	private T value;
	public OptionalWaitTask(T value)
	{
		this.value = value;
	}
	
	/**
	 * Return the object immediately (but this object may not yet be serialized throughout the system)
	 */
	public T getNoWait()
	{
		return value;
	}
	
	/**
	 * Calls {@link #get() but translates exceptions to runtime exceptions for convenient use in streaming APIs.}
	 */
	public T getNoThrow()
	{
		try
		{
			return get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			throw new RuntimeException(e);
		}
	}
	
}

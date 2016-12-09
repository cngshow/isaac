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
package gov.vha.isaac.ochre.api.externalizable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.ochre.api.LookupService;

/**
 * Simple wrapper class to allow us to serialize to multiple formats at once
 * {@link MultipleDataWriterService}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class MultipleDataWriterService implements DataWriterService
{
	ArrayList<DataWriterService> writers_ = new ArrayList<>();
	private Logger logger = LoggerFactory.getLogger(MultipleDataWriterService.class);
	
	public MultipleDataWriterService(Optional<Path> jsonPath, Optional<Path> ibdfPath) throws IOException
	{
		if (jsonPath.isPresent())
		{
			//Use HK2 here to make fortify stop false-flagging an open resource error
			DataWriterService writer = LookupService.get().getService(DataWriterService.class, "jsonWriter");
			if (writer != null)
			{
				writer.configure(jsonPath.get());
				writers_.add(writer);
			}
			else
			{
				LogManager.getLogger().warn("json writer was requested, but not found on classpath!");
			}
		}
		if (ibdfPath.isPresent())
		{
			DataWriterService writer = LookupService.get().getService(DataWriterService.class, "ibdfWriter");
			if (writer != null)
			{
				writer.configure(ibdfPath.get());
				writers_.add(writer);
			}
			else
			{
				LogManager.getLogger().warn("ibdf writer was requested, but not found on classpath!");
			}
		}
	}
	
	/**
	 * @throws IOException 
	 * @see gov.vha.isaac.ochre.api.externalizable.DataWriterService#put(gov.vha.isaac.ochre.api.externalizable.OchreExternalizable)
	 */
	@Override
	public void put(OchreExternalizable ochreObject) throws RuntimeException
	{
		try
		{
			handleMulti((writer) -> 
			{
				try
				{
					writer.put(ochreObject);
					return null;
				}
				catch (RuntimeException e)
				{
					return new IOException(e);
				}
			});
		}
		catch (IOException e)
		{
			if (e.getCause() != null && e.getCause() instanceof RuntimeException)
			{
				throw (RuntimeException)e.getCause();
			}
			else
			{
				logger.warn("Unexpected", e);
				throw new RuntimeException(e);
			}
		}
	}
	
	/**
	 * @throws IOException 
	 * @see gov.vha.isaac.ochre.api.externalizable.DataWriterService#close()
	 */
	@Override
	public void close() throws IOException
	{
		handleMulti((writer) -> 
		{
			try
			{
				writer.close();
				return null;
			}
			catch (IOException e)
			{
				return e;
			}
		});
	}
	
	/**
	 * @see gov.vha.isaac.ochre.api.externalizable.DataWriterService#flush()
	 */
	@Override
	public void flush() throws IOException
	{
		handleMulti((writer) -> 
		{
			try
			{
				writer.flush();
				return null;
			}
			catch (IOException e)
			{
				return e;
			}
		});
	}

	/**
	 * @throws IOException 
	 * @see gov.vha.isaac.ochre.api.externalizable.DataWriterService#pause()
	 */
	@Override
	public void pause() throws IOException
	{
		handleMulti((writer) -> 
		{
			try
			{
				writer.pause();
				return null;
			}
			catch (IOException e)
			{
				return e;
			}
		});
	}

	/**
	 * @throws IOException 
	 * @see gov.vha.isaac.ochre.api.externalizable.DataWriterService#resume()
	 */
	@Override
	public void resume() throws IOException
	{
		handleMulti((writer) -> 
		{
			try
			{
				writer.resume();
				return null;
			}
			catch (IOException e)
			{
				return e;
			}
		});
	}
	
	public void handleMulti(Function<DataWriterService, IOException> function) throws IOException
	{
		ArrayList<IOException> exceptions = new ArrayList<>();
		for (DataWriterService writer : writers_)
		{
			IOException e = function.apply(writer);
			if (e != null)
			{
				exceptions.add(e);
			}
		}
		if (exceptions.size() > 0)
		{
			if (exceptions.size() > 1)
			{
				for (int i = 1; i < exceptions.size(); i++)
				{
					logger.error("extra, unthrown exception: ", exceptions.get(i));
				}
			}
			throw exceptions.get(0);
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.externalizable.DataWriterService#configure(java.nio.file.Path)
	 */
	@Override
	public void configure(Path path) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException("Method not supported");
	}
}

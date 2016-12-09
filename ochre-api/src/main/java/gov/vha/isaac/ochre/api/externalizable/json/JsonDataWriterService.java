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
package gov.vha.isaac.ochre.api.externalizable.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cedarsoftware.util.io.JsonWriter;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.externalizable.DataWriterService;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.util.TimeFlushBufferedOutputStream;

/**
 * {@link JsonDataWriterService} - serialize to JSON
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service(name="jsonWriter")
@PerLookup
public class JsonDataWriterService implements DataWriterService
{
	private JsonWriter json_;
	private FileOutputStream fos_;
	private Logger logger = LoggerFactory.getLogger(JsonDataWriterService.class);
	
	private Semaphore pauseBlock = new Semaphore(1);
	
	Path dataPath;
	
	private JsonDataWriterService() throws IOException
	{
		//For HK2
	}
	
	/**
	 * To support non HK2 useage
	 * @param path
	 * @throws IOException
	 */
	public JsonDataWriterService(Path path) throws IOException
	{
		this();
		configure(path);
	}
	
	public JsonDataWriterService(File path) throws IOException
	{
		this();
		configure(path.toPath());
	}
	
	@Override
	public void configure(Path path) throws IOException
	{
		if (json_ != null)
		{
			throw new IOException("Reconfiguration not supported");
		}
		Map<String, Object> args = new HashMap<>();
		args.put(JsonWriter.PRETTY_PRINT, true);
		dataPath = path;
		fos_ = new FileOutputStream(path.toFile());
		json_ = new JsonWriter(new TimeFlushBufferedOutputStream(fos_), args);
		json_.addWriter(ConceptChronology.class, new Writers.ConceptChronologyJsonWriter());
		json_.addWriter(SememeChronology.class, new Writers.SememeChronologyJsonWriter());
		logger.info("json changeset writer has been configured to write to " + dataPath.toAbsolutePath().toString());
	}

	@Override
	public void put(OchreExternalizable ochreObject)
	{
		try
		{
			pauseBlock.acquireUninterruptibly();
			json_.write(ochreObject);
		}
		finally
		{
			pauseBlock.release();
		}
	}
	
	/**
	 * Write out a string object to the json file - this will encode all illegal characters within the string.
	 * Useful for writing debugging files
	 */
	public void put(String string)
	{
		try
		{
			pauseBlock.acquireUninterruptibly();
			json_.write(string);
		}
		finally
		{
			pauseBlock.release();
		}
	}

	@Override
	public void close() throws IOException
	{
		try 
		{
			json_.close();
			fos_.close();
		}
		finally
		{
			json_ = null;
			fos_ = null;
		}
	}
	
	/**
	 * @throws IOException 
	 * @see gov.vha.isaac.ochre.api.externalizable.DataWriterService#flush()
	 */
	@Override
	public void flush() throws IOException
	{
		if (json_ != null)
		{
			json_.flush();
		}
	}

	@Override
	public void pause() throws IOException
	{
		if (json_ == null)
		{
			logger.warn("already paused!");
			return;
		}
		pauseBlock.acquireUninterruptibly();
		close();

		logger.debug("json writer paused");
	}

	@Override
	public void resume() throws IOException
	{
		if (json_ != null)
		{
			logger.warn("asked to resume but not paused!");
			return;
		}
		configure(dataPath);
		pauseBlock.release();
		logger.debug("json writer resumed");
	}
}

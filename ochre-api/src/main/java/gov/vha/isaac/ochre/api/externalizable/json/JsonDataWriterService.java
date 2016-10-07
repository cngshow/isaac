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
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import com.cedarsoftware.util.io.JsonWriter;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataWriterService;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;

/**
 * {@link JsonDataWriterService} - serialize to JSON
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@PerLookup
public class JsonDataWriterService implements BinaryDataWriterService
{
	private JsonWriter json_;
	
	private JsonDataWriterService() throws IOException
	{
		//For HK2
	}
	
	//To support non HK2 useage
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
	
	public void configure(Path path) throws IOException
	{
		if (json_ != null)
		{
			throw new IOException("Reconfiguration not supported");
		}
		Map<String, Object> args = new HashMap<>();
		args.put(JsonWriter.PRETTY_PRINT, true);
		json_ = new JsonWriter(new FileOutputStream(path.toFile()), args);
		json_.addWriter(ConceptChronology.class, new Writers.ConceptChronologyJsonWriter());
		json_.addWriter(SememeChronology.class, new Writers.SememeChronologyJsonWriter());
	}

	@Override
	public void put(OchreExternalizable ochreObject)
	{
		json_.write(ochreObject);
	}
	
	/**
	 * Write out a string object to the json file - this will encode all illegal characters within the string.
	 * Useful for writing debugging files
	 */
	public void put(String string)
	{
		json_.write(string);
	}

	@Override
	public void close()
	{
		json_.close();
	}
}

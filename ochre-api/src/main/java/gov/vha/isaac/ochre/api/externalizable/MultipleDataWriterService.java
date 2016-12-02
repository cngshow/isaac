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
import org.apache.logging.log4j.LogManager;
import gov.vha.isaac.ochre.api.LookupService;

/**
 * Simple wrapper class to allow us to serialize to multiple formats at once
 * {@link MultipleDataWriterService}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class MultipleDataWriterService implements BinaryDataWriterService
{
	ArrayList<BinaryDataWriterService> writers_ = new ArrayList<>();
	
	public MultipleDataWriterService(Optional<Path> jsonPath, Optional<Path> ibdfPath) throws IOException
	{
		if (jsonPath.isPresent())
		{
			//Use HK2 here to make fortify stop false-flagging an open resource error
			BinaryDataWriterService writer = LookupService.get().getService(BinaryDataWriterService.class, "jsonWriter");
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
			BinaryDataWriterService writer = LookupService.get().getService(BinaryDataWriterService.class, "ibdfWriter");
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
	
	@Override
	public void put(OchreExternalizable ochreObject)
	{
		for (BinaryDataWriterService writer : writers_)
		{
			writer.put(ochreObject);
		}
	}

	@Override
	public void close()
	{
		for (BinaryDataWriterService writer : writers_)
		{
			writer.close();
		}
	}

	@Override
	public void configure(Path path) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException("Method not supported");
	}
}

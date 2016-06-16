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
package gov.vha.isaac.ochre.pombuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.pom._4_0.Model;
import org.apache.maven.pom._4_0.ObjectFactory;
import gov.vha.isaac.ochre.pombuilder.dbbuilder.DBConfigurationCreator;

/**
 * 
 * {@link FileUtil}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class FileUtil
{
	private static final Logger LOG = LogManager.getLogger();
	
	public static void writeFile(String fromFolder, String relativePath, File toFolder, HashMap<String, String> replacementValues, String append) throws IOException
	{
		InputStream is = FileUtil.class.getResourceAsStream("/" + fromFolder + "/" + relativePath);
		byte[] buffer = new byte[is.available()];
		is.read(buffer);

		String temp = new String(buffer, Charset.forName("UTF-8"));
		if (replacementValues != null)
		{
			for (Entry<String, String> item : replacementValues.entrySet())
			{
				while (temp.contains(item.getKey()))
				{
					temp = temp.replace(item.getKey(), item.getValue());
				}
			}
		}
		
		if (relativePath.startsWith("DOT"))
		{
			relativePath = relativePath.replaceFirst("^DOT", ".");  //front of string
		}
		else if (relativePath.contains("/DOT"))
		{
			relativePath = relativePath.replaceFirst("/DOT", "/.");  //down in the relative path
		}
		
		File targetFile = new File(toFolder, relativePath);
		targetFile.getParentFile().mkdirs();
		OutputStream outStream = new FileOutputStream(targetFile);
		outStream.write(temp.getBytes());
		if (StringUtils.isNotBlank(append))
		{
			outStream.write(append.getBytes());
		}
		outStream.close();
	}
	
	public static void writeFile(String fromFolder, String relativePath, File toFolder) throws IOException
	{
		writeFile(fromFolder, relativePath, toFolder, null, null);
	}
	
	public static void writePomFile(Model model, File projectFolder) throws Exception
	{
		try
		{
			JAXBContext ctx = JAXBContext.newInstance(Model.class);
			Marshaller ma = ctx.createMarshaller();
			ma.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");
			ma.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			ma.marshal(new ObjectFactory().createProject(model), new File(projectFolder, "pom.xml"));
		}

		catch (JAXBException e)
		{
			LOG.error("Error writing", e);
			throw new Exception("Error writing pom: " + e);
		}
	}
	
	public static String readFile(String fileName) throws IOException
	{
		InputStream is = DBConfigurationCreator.class.getResourceAsStream("/" + fileName);
		byte[] buffer = new byte[is.available()];
		is.read(buffer);

		return new String(buffer, Charset.forName("UTF-8"));
	}
}

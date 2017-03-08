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
package gov.vha.isaac.ochre.pombuilder;

import java.io.File;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * {@link VersionFinder}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class VersionFinder
{
	private static final Logger LOG = LogManager.getLogger();
			
	public static String findProjectVersion()
	{
		try (InputStream is = VersionFinder.class.getResourceAsStream("/META-INF/maven/gov.vha.isaac.ochre.modules/db-config-builder/pom.xml");)
		{
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			//added to avoid XXE injections
			domFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			
			DocumentBuilder builder = domFactory.newDocumentBuilder();

			Document dDoc;
			
			if (is != null)
			{
				dDoc = builder.parse(is);
			}
			else
			{
				dDoc = builder.parse(new File("pom.xml"));  //running in eclipse, this should work.
			}

			XPath xPath = XPathFactory.newInstance().newXPath();
			String temp = ((Node) xPath.evaluate("/project/parent/version", dDoc, XPathConstants.NODE)).getTextContent();
			LOG.debug("VersionFinder finds {} (for the version of this release of the converter library)", temp);
			return temp;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}

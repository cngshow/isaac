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
package gov.vha.isaac.ochre.pombuilder.converter;

import gov.vha.isaac.ochre.pombuilder.RepositoryLocation;
import gov.vha.isaac.ochre.pombuilder.artifacts.SDOSourceContent;

/**
 * 
 * {@link ContentConverterCreator}
 *
 * A class that has the convenience methods that will construct and publish a pom project - which when executed, will
 * convert SDO source content into IBDF content.  The convenience methods in this class carry all of the documentation 
 * and information necessary to create various conversion types.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ContentConverterCreator
{
	
	public static SupportedConverterTypes[] getSupportedConversions()
	{
		return SupportedConverterTypes.values();
	}
	
	
	/**
	 * Create a source conversion project which is executable via maven.
	 * @param sourceContent - The artifact information for the content to be converted.  The artifact information must follow known naming conventions - group id should 
	 * be gov.vha.isaac.terminology.source.  Currently supported artifactIds are 'loinc-src-data', 'loinc-src-data-tech-preview', 'rf2-src-data-*', 'vhat' 
	 * @param converterVersion - The version number of the content converter code to utilize.  The jar file for this converter must be available to the 
	 * maven execution environment at the time when the conversion is run.
	 * @param additionalContent - Some converters require additional data files to satisfy dependencies. Currently:
	 * loinc-src-data-tech-preview requires:
	 *   - loinc-src-data
	 *   - rf2-src-data-sct
	 * Any RF2 extension files (rf2-src-data-*-ext) requires:
	 *   - rf2-src-data-sct
	 *   
	 * See {@link #getSupportedConversions()} for accurate dependencies for any given conversion type.
	 * @return
	 * @throws Exception
	 */
	public static RepositoryLocation createContentConverter(SDOSourceContent sourceContent, String converterVersion, SDOSourceContent[] additionalContent) throws Exception
	{
		

		//TODO attach to GIT / push
		return new RepositoryLocation("a", "b");
	}
}

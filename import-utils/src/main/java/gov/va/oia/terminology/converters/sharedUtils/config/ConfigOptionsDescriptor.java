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
package gov.va.oia.terminology.converters.sharedUtils.config;

import org.jvnet.hk2.annotations.Contract;
import gov.vha.isaac.ochre.pombuilder.converter.ConverterOptionParam;

/**
 * 
 * {@link ConfigOptionsDescriptor}
 *
 * A class that should be implemented in each conversion mojo that describes what options 
 * it takes in via maven, so that we can pick them up via a GUI, later.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

@Contract
public interface ConfigOptionsDescriptor
{
	/**
	 * Which options are applicable for this converter
	 */
	public ConverterOptionParam[] getConfigOptions();
	
	/**
	 * What converter is this config option describing.  Recommend returning the artifactId here of the containing project
	 */
	public String getName();
}

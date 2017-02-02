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
package gov.vha.isaac.ochre.deployment.hapi.extension.hl7.message;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The <code>getHL7SenderProperties</code> class serves the purpose of
 * retrieving values from .properties files based on a passed key value.
 * <p>
 *
 * @author vhaislempeyd
 */

public class ApplicationPropertyReader
{
	private static Logger LOG = LogManager.getLogger(ApplicationPropertyReader.class);

	/**
	 * The <code>getApplicationProperties</code> method retrieves from
	 * <code>application.properties</code> the value of the passed parameter.
	 *
	 * @param parameterName
	 * @return String value of parameter
	 * @throws MissingResourceException
	 */
	public static String getApplicationProperty(String parameterName) {
		String parameterValue = null;

		String baseName = "application";

		try {
			ResourceBundle resourceBundle = ResourceBundle.getBundle(baseName);
			String key = parameterName;

			parameterValue = resourceBundle.getString(key);
		} catch (Exception e) {
			LOG.debug(e.getMessage());
		}

		return parameterValue;
	}
}
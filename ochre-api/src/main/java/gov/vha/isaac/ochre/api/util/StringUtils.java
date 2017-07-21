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
package gov.vha.isaac.ochre.api.util;

/**
 * {@link StringUtils}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils
{

	/**
	 * Null-safe string compare
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static int compareStringsIgnoreCase(String s1, String s2)
	{
		int rval = 0;

		if (s1 != null || s2 != null)
		{
			if (s1 == null)
			{
				rval = -1;
			}
			else if (s2 == null)
			{
				rval = 1;
			}
			else
			{
				return s1.compareToIgnoreCase(s2);
			}
		}
		return rval;
	}
	
	public static String stringForFortify(String convertString)
	{
		StringBuilder temp = new StringBuilder();
		if (convertString != null) {
			convertString.chars().forEach(c -> temp.append((char)c));
		}
		return temp.toString();
	}
}

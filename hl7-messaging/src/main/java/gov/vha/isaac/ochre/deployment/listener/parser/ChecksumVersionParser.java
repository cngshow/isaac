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
package gov.vha.isaac.ochre.deployment.listener.parser;

import java.util.StringTokenizer;


/**
 * 
 * This class will parse a string to return the value for a given key.
 * eg. ;CHECKSUM:4bdb6ba422ce11216529bb9b085eb54f;VERSION:0;
 * 
 * @author nuno
 *
 */
public class ChecksumVersionParser
{
	/**
	 * Returns the value for a given key.  The string is parameterized twice
	 * with ; and :.
	 * 
	 * eg. ;CHECKSUM:4bdb6ba422ce11216529bb9b085eb54f;VERSION:0;
	 * 
	 * Passing in "CHECKSUM" and the string above will return the MD5 hash.
	 * 
	 * @param token - key of the name value pair.
	 * @param input - string to parse.
	 * 
	 * @return String - value of matching the key
	 */
	public static String getValueFromTokenizedString(String token, String input) {
		StringTokenizer tokenizer = new StringTokenizer(input, ";");
		String currentToken = null;
		int colonIndex = -1;
		String result = "";
		
		while (tokenizer.hasMoreTokens()) {
			currentToken = tokenizer.nextToken();
			if (currentToken.startsWith(token)) {
				colonIndex = currentToken.indexOf(':');
				if (colonIndex != -1 && (currentToken.length() > (colonIndex + 1))) {
					result = currentToken.substring(colonIndex + 1);
				}
			}
		}
		
		return result; 
	}
}

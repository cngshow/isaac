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

/**
 * {@link RepositoryLocation}
 * Carries back the GIT URL and tag of the pom project that was created by a utility in this class.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class RepositoryLocation
{
	private String url_;
	private String tag_;
	
	public RepositoryLocation(String url, String tag)
	{
		url_ = url;
		tag_ = tag;
	}

	public String getUrl()
	{
		return url_;
	}

	public String getTag()
	{
		return tag_;
	}
}

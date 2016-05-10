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
package gov.vha.isaac.ochre.pombuilder.artifacts;

/**
 * {@link IBDFFile}
 * An artifact that points to an file of type ibdf to pass into the conversion tool
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class IBDFFile extends Artifact
{
	public IBDFFile(String groupId, String artifactId, String version)
	{
		this(groupId, artifactId, version, null);
	}
	
	public IBDFFile(String groupId, String artifactId, String version, String classifier)
	{
		super(groupId, artifactId, version, classifier);
	}
}

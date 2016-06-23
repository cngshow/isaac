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
 * 
 * {@link Artifact}
 * A base class for providing artifact information to the config builder tool.
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public abstract class Artifact
{
	private String groupId_;
	private String artifactId_;
	private String version_;
	private String classifier_;
	
	public Artifact(String groupId, String artifactId, String version)
	{
		this(groupId, artifactId, version, null);
	}
	
	public Artifact(String groupId, String artifactId, String version, String classifier)
	{
		groupId_ = groupId;
		artifactId_ = artifactId;
		version_ = version;
		classifier_ = classifier;
	}

	public String getGroupId()
	{
		return groupId_;
	}

	public String getArtifactId()
	{
		return artifactId_;
	}

	public String getVersion()
	{
		return version_;
	}

	public String getClassifier()
	{
		return classifier_;
	}
	
	public boolean hasClassifier()
	{
		if (classifier_ == null || classifier_.trim().length() == 0)
		{
			return false;
		}
		return true;
	}
}

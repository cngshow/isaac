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
package gov.vha.isaac.ochre.api.util.metainf;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link MavenArtifactInfo}
 * 
 * This class carries Maven dependency information
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */

public class MavenArtifactInfo
{
	/**
	 * Maven Dependency Group ID
	 */
	public String groupId;

	/**
	 * Maven Dependency Artifact ID
	 */
	public String artifactId;

	/**
	 * Maven Dependency Version
	 */
	public String version;

	/**
	 * Maven Dependency Classifier
	 */
	public String classifier;

	/**
	 * Maven Dependency Type
	 */
	public String type;

	/**
	 * Database Licenses
	 */
	public List<MavenLicenseInfo> dbLicenses = new ArrayList<>();

	/**
	 * The source content that was built into the underlying database.
	 */
	public List<MavenArtifactInfo> dbDependencies = new ArrayList<>();

	public MavenArtifactInfo()
	{

	}

	/**
	 * @param groupId Maven Dependency Group ID
	 * @param artifactId Maven Dependency Artifact ID
	 * @param version Maven Dependency Version
	 * @param classifier Maven Dependency Classifier
	 * @param type Maven Dependency Type
	 */
	public MavenArtifactInfo(String groupId, String artifactId, String version, String classifier, String type)
	{
		super();
		setValues(groupId, artifactId, version, classifier, type);
	}

	/**
	 * @param groupId Maven Dependency Group ID
	 * @param artifactId Maven Dependency Artifact ID
	 * @param version Maven Dependency Version
	 * @param classifier Maven Dependency Classifier
	 * @param type Maven Dependency Type
	 */
	public void setValues(String groupId, String artifactId, String version, String classifier, String type)
	{
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.classifier = classifier;
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("MavenArtifactInfo\r\n");
		sb.append("  groupId=" + groupId + "\r\n");
		sb.append("  artifactId=" + artifactId + "\r\n");
		sb.append("  version=" + version + "\r\n");
		sb.append("  classifier=" + classifier + "\r\n");
		sb.append("  type=" + type + "\r\n");
		sb.append("\r\n");
		sb.append("Licenses\r\n");
		dbLicenses.forEach(license -> sb.append("  " + license.toString() + "\r\n"));
		sb.append("\r\n");
		sb.append("Database Dependencies\r\n");
		dbDependencies.forEach(dbDependendy -> sb.append("  " + dbDependendy.toString() + "\r\n"));
		return sb.toString();
	}
}

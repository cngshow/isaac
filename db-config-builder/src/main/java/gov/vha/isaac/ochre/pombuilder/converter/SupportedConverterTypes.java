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

/**
 * {@link SupportedConverterTypes}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum SupportedConverterTypes
{
	LOINC("loinc-src-data", new String[] {}, new String[] {}),
	LOINC_TECH_PREVIEW("loinc-src-data-tech-preview", new String[] {"loinc-src-data"}, new String[] {"rf2-ibdf-sct"}),
	SCT("rf2-src-data-sct", new String[] {}, new String[] {}),
	SCT_EXTENSION("rf2-src-data-*-extension", new String[] {}, new String[] {"rf2-ibdf-sct"}),
	VHAT("vhat-src-data", new String[] {}, new String[] {}),
	RXNORM("rxnorm-src-data", new String[] {}, new String[] {"rf2-ibdf-sct"}),
	RXNORM_SOLOR("rxnorm-src-data", new String[] {}, new String[] {"rf2-ibdf-sct"}),;
	
	private String artifactId_;
	private String[] artifactSrcDependencies_;
	private String[] artifactIBDFDependencies_;
	
	private SupportedConverterTypes(String artifactId, String[] artifactSourceDependencies, String[] artifactIBDFDependencies)
	{
		artifactId_ = artifactId;
		artifactSrcDependencies_ = artifactSourceDependencies;
		artifactIBDFDependencies_ = artifactIBDFDependencies;
		
	}

	/**
	 * Note that the artifactID may include a wildcard ('*') for some, such as SCT_EXTENSION
	 * @return
	 */
	public String getArtifactId()
	{
		return artifactId_;
	}

	/**
	 * In order to execute a conversion of the specified type, you must also provide dependencies for each of the listed
	 * artifact identifiers.
	 * @return
	 */
	public String[] getArtifactDependencies()
	{
		return artifactSrcDependencies_;
	}
	
	/**
	 * In order to execute a conversion of the specified type, you must also provide dependencies for each of the listed
	 * artifact identifiers.
	 * @return
	 */
	public String[] getIBDFDependencies()
	{
		return artifactIBDFDependencies_;
	}
}

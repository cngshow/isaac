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

import java.util.Arrays;
import java.util.List;

/**
 * {@link SupportedConverterTypes}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum SupportedConverterTypes
{
	LOINC("loinc-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] {
			
		//(?i) and (?-i) constructs are not supported in JavaScript (they are in Ruby)
			new UploadFileInfo("", "https://loinc.org/downloads/loinc", 
					"LOINC_2.54_Text.zip",
					"The primary LOINC file is the 'LOINC Table File' in the csv format'.  This should be a zip file that contains a file named 'loinc.csv'."
					+ "  Additionally, the zip file may (optionally) contain 'map_to.csv' and 'source_organization.csv'."
					+ "  The zip file must contain 'text' within its name.", ".*text.*\\.zip$", true),
			new UploadFileInfo("", "https://loinc.org/downloads/files/loinc-multiaxial-hierarchy", 
					"LOINC_2.54_MULTI-AXIAL_HIERARCHY.zip",
					"The Multiaxial Hierarchy file is a zip file that contains a file named *multi-axial_hierarchy.csv.  The zip file containing the multiaxial hierarchy"
					+ " must contain 'multi-axial_hierarchy' within its name", ".*multi\\-axial_hierarchy.*\\.zip$", true),
			new UploadFileInfo("", "https://loinc.org/downloads/loinc",
					"LOINC_ReleaseNotes.txt",
					"The LOINC Release Notes file must be included for recent versions of LOINC.", "(?i).*releasenotes\\.txt", true)
	}),
	LOINC_TECH_PREVIEW("loinc-src-data-tech-preview", new String[] {"loinc-src-data"}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
			new UploadFileInfo("", "https://www.nlm.nih.gov/healthit/snomedct/international.html",
					"SnomedCT_LOINC_AlphaPhase3_INT_20160401.zip",
					"  The expected file is the RF2 release (NOT the Human Readable release nor the OWL release) "
					+ "The file must be a zip file, which ends with .zip", ".*\\.zip$", true)
	}),
	SCT("rf2-src-data-sct", new String[] {}, new String[] {}, new UploadFileInfo[] {
			new UploadFileInfo("", "https://www.nlm.nih.gov/healthit/snomedct/international.html",
					"SnomedCT_RF2Release_INT_20160131.zip",
					"The expected file is the RF2 release zip file.  The filename must end with .zip, and must contain the release date in the Snomed standard"
					+ " naming convention (4 digit year, 2 digit month, 2 digit day).",
					".*_\\d{8}.*\\.zip$", true)
	}),
	SCT_EXTENSION("rf2-src-data-*-extension", new String[] {}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
			new UploadFileInfo("Snomed Extensions come from a variety of sources.  Note that the NLM has choosen to stop advertising the download links to the "
					+ " US Extension, but still publishes it.  The current download pattern is: "
					+ "http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_Release_US1000124_YYYYMMDD_Extension.zip",
					"",
					"SnomedCT_Release_US1000124_20160301_Extension.zip",
					"The expected file is the RF2 release zip file.  The filename must end with .zip, and must contain the release date in the Snomed standard"
					+ " naming convention (4 digit year, 2 digit month, 2 digit day).",
					".*_\\d{8}.*\\.zip$", true)
	}),
	VHAT("vhat-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] {
			new UploadFileInfo("VHAT content is typically exported from a VETs system.  ", "",
					"VHAT.xml",
					"Any XML file that is valid per the VETs TerminologyData.xsd schema.  The file name is ignored", 
					".*", true)
	}),
	RXNORM("rxnorm-src-data", new String[] {}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
			new UploadFileInfo("", "https://www.nlm.nih.gov/research/umls/rxnorm/docs/rxnormfiles.html", 
					"RxNorm_full_06062016.zip",
					"The file must be a zip file, which starts with 'rxnorm_full' and ends with .zip", "rxnorm_full.*\\.zip$", true)
	}),
	RXNORM_SOLOR("rxnorm-src-data", new String[] {}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
			new UploadFileInfo("", "https://www.nlm.nih.gov/research/umls/rxnorm/docs/rxnormfiles.html", 
					"RxNorm_full_06062016.zip",
					"The file must be a zip file, which starts with 'rxnorm_full' and ends with .zip", "rxnorm_full.*\\.zip$", true)
	});
	
	private String artifactId_;
	private List<String> artifactSrcDependencies_;
	private List<String> artifactIBDFDependencies_;
	private List<UploadFileInfo> uploadFileInfo_;  //If we were really clever, we would pull this from an options file published with the converter itself.
	/*
	 * unfortunately, that gets tricky, because the user needs to populate these when they are uploading, without necessarily knowing what particular 
	 * version of the converter will execute against this uploaded content.  So, will hardcode them here for now, and developers will have to manually
	 * update these if the patterns change in the future.
	 */
	
	private SupportedConverterTypes(String artifactId, String[] artifactSourceDependencies, String[] artifactIBDFDependencies, UploadFileInfo[] uploadFileInfo)
	{
		artifactId_ = artifactId;
		artifactSrcDependencies_ = Arrays.asList(artifactSourceDependencies);
		artifactIBDFDependencies_ = Arrays.asList(artifactIBDFDependencies);
		uploadFileInfo_ = Arrays.asList(uploadFileInfo);
		
	}

	/**
	 * Note that the artifactID may include a wildcard ('*') for some, such as SCT_EXTENSION
	 */
	public String getArtifactId()
	{
		return artifactId_;
	}

	/**
	 * In order to execute a conversion of the specified type, you must also provide dependencies for each of the listed
	 * Source artifact identifiers.
	 */
	public List<String> getArtifactDependencies()
	{
		return artifactSrcDependencies_;
	}
	
	/**
	 * In order to execute a conversion of the specified type, you must also provide dependencies for each of the listed
	 * IBDF artifact identifiers.
	 */
	public List<String> getIBDFDependencies()
	{
		return artifactIBDFDependencies_;
	}
	
	/**
	 * The information describing the files that an end user must upload into the system to allow the execution of a particular converter.
	 */
	public List<UploadFileInfo> getUploadFileInfo()
	{
		return uploadFileInfo_;
	}
}

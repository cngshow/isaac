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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import gov.vha.isaac.ochre.api.util.StringUtils;
import gov.vha.isaac.ochre.pombuilder.FileUtil;

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
					"The LOINC Release Notes file must be included for recent versions of LOINC.", ".*releasenotes\\.txt$", true)
	}, "loinc-mojo", "loinc-ibdf", "convert-loinc-to-ibdf", "gov.vha.isaac.terminology.source.loinc", "LOINC", 
			new String[] {"shared/licenses/loinc.xml"}, 
			new String[] {"shared/noticeAdditions/loinc-NOTICE-addition.txt"}),
	
	LOINC_TECH_PREVIEW("loinc-src-data-tech-preview", new String[] {"loinc-src-data"}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
			new UploadFileInfo("", "https://www.nlm.nih.gov/healthit/snomedct/international.html",
					"SnomedCT_LOINC_AlphaPhase3_INT_20160401.zip",
					"  The expected file is the RF2 release (NOT the Human Readable release nor the OWL release). "
					+ "The file must be a zip file, which ends with .zip", ".*\\.zip$", true)
	}, "loinc-mojo", "loinc-ibdf-tech-preview", "convert-loinc-tech-preview-to-ibdf", "gov.vha.isaac.terminology.source.loinc", "LOINC Tech Preview", 
			new String[] {"shared/licenses/loinc.xml", "shared/licenses/sct.xml"}, 
			new String[] {"shared/noticeAdditions/loinc-tech-preview-NOTICE-addition.txt", "shared/noticeAdditions/loinc-NOTICE-addition.txt", "shared/noticeAdditions/rf2-sct-NOTICE-addition.txt"}),
	
	SCT("rf2-src-data-sct", new String[] {}, new String[] {}, new UploadFileInfo[] {
			new UploadFileInfo("", "https://www.nlm.nih.gov/healthit/snomedct/international.html",
					"SnomedCT_RF2Release_INT_20160131.zip",
					"The expected file is the RF2 release zip file.  The filename must end with .zip, and must contain the release date in the Snomed standard"
					+ " naming convention (4 digit year, 2 digit month, 2 digit day).",
					".*_\\d{8}.*\\.zip$", true)
	}, "rf2-mojo", "rf2-ibdf-sct", "convert-RF2-to-ibdf", "gov.vha.isaac.terminology.source.rf2", "SnomedCT", 
			new String[] {"shared/licenses/sct.xml"},
			new String[] {"shared/noticeAdditions/rf2-sct-NOTICE-addition.txt"}),
	
	SCT_EXTENSION("rf2-src-data-*-extension", new String[] {}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
			new UploadFileInfo("Snomed Extensions come from a variety of sources.  Note that the NLM has choosen to stop advertising the download links to the "
					+ " US Extension, but still publishes it.  The current download pattern is: "
					+ "http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_Release_US1000124_YYYYMMDD_Extension.zip",
					"",
					"SnomedCT_Release_US1000124_20160301_Extension.zip",
					"The expected file is the RF2 release zip file.  The filename must end with .zip, and must contain the release date in the Snomed standard"
					+ " naming convention (4 digit year, 2 digit month, 2 digit day).",
					".*_\\d{8}.*\\.zip$", true)
	}, "rf2-mojo", "rf2-ibdf-", "convert-RF2-to-ibdf", "gov.vha.isaac.terminology.source.rf2", "SnomedCT Extension", 
			new String[] {"shared/licenses/sct.xml"},
			new String[] {"shared/noticeAdditions/rf2-sct-NOTICE-addition.txt"}),
	
	VHAT("vhat-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] {
			new UploadFileInfo("VHAT content is typically exported from a VETs system.  ", "",
					"VHAT.xml",
					"Any XML file that is valid per the VETs TerminologyData.xsd schema.  The file name is ignored", 
					".*\\.xml$", true)
	}, "vhat-mojo", "vhat-ibdf", "convert-VHAT-to-ibdf", "gov.vha.isaac.terminology.source.vhat", "VHAT", 
			new String[] {""}, 
			new String[] {""}),
	
	RXNORM("rxnorm-src-data", new String[] {}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
			new UploadFileInfo("", "https://www.nlm.nih.gov/research/umls/rxnorm/docs/rxnormfiles.html", 
					"RxNorm_full_06062016.zip",
					"The file must be a zip file, which starts with 'rxnorm_full' and ends with .zip", "rxnorm_full.*\\.zip$", true)
	}, "rxnorm-mojo", "rxnorm-ibdf", "convert-rxnorm-to-ibdf", "gov.vha.isaac.terminology.source.rxnorm","RxNorm", 
			new String[] {"shared/licenses/rxnorm.xml"}, 
			new String[] {"shared/noticeAdditions/rxnorm-NOTICE-addition.txt"}),
	RXNORM_SOLOR("rxnorm-src-data", new String[] {}, new String[] {"rf2-ibdf-sct"}, new UploadFileInfo[] {
			new UploadFileInfo("", "https://www.nlm.nih.gov/research/umls/rxnorm/docs/rxnormfiles.html", 
					"RxNorm_full_06062016.zip",
					"The file must be a zip file, which starts with 'rxnorm_full' and ends with .zip", "rxnorm_full.*\\.zip$", true)
	}, "rxnorm-mojo", "rxnorm-ibdf-solor", "convert-rxnorm-solor-to-ibdf", "gov.vha.isaac.terminology.source.rxnorm", "RxNorm Solor", 
			new String[] {"shared/licenses/rxnorm.xml"}, 
			new String[] {"shared/noticeAdditions/rxnorm-NOTICE-addition.txt"}),
	
	HL7v3("hl7v3-src-data", new String[] {}, new String[] {}, new UploadFileInfo[] {
		new UploadFileInfo("", "http://gforge.hl7.org/gf/project/design-repos/frs/?action=FrsReleaseBrowse&frs_package_id=30", 
				"hl7-rimRepos-2.47.7.zip",
				"The file must be a zip file, which should have 'rimRepos' in the file name and end with '.zip'.  This uploaded zip file" +
				 " MUST contain a file that has 'DEFN=UV=VO' in the file name, and ends with .coremif", ".*rim.*\\.zip$", true)
	}, "hl7v3-mojo", "hl7v3-ibdf", "convert-hl7v3-to-ibdf", "gov.vha.isaac.terminology.source.hl7v3", "HL7v3", 
			new String[] {"shared/licenses/hl7v3.xml"}, 
			new String[] {"shared/noticeAdditions/hl7v3-NOTICE-addition.txt"});
	
	private String srcArtifactId_;
	private List<String> artifactSrcDependencies_;
	private List<String> artifactIBDFDependencies_;
	private List<UploadFileInfo> uploadFileInfo_;  //If we were really clever, we would pull this from an options file published with the converter itself.
	private String converterGroupId_ = "gov.vha.isaac.terminology.converters";
	private String converterArtifactId_;
	private String converterOutputArtifactId_;
	private String converterMojoName_;  //Must match the value from the mojo - aka - @ Mojo( name = "convert-loinc-to-ibdf", defaultPhase... used as the goal in the pom.
	private String sourceUploadGroupId_;
	private String niceName_;
	private String[] licenseInformation;
	private String[] noticeInformation;
	
	/*
	 * unfortunately, that gets tricky, because the user needs to populate these when they are uploading, without necessarily knowing what particular 
	 * version of the converter will execute against this uploaded content.  So, will hardcode them here for now, and developers will have to manually
	 * update these if the patterns change in the future.
	 */
	
	private SupportedConverterTypes(String artifactId, String[] artifactSourceDependencies, String[] artifactIBDFDependencies, UploadFileInfo[] uploadFileInfo, 
			String converterArtifactId, String converterOutputArtifactId, String converterMojoName, String sourceUploadGroupId, String niceName,
			String[] licenseFilePaths, String[] noticeFilePaths)
	{
		srcArtifactId_ = artifactId;
		artifactSrcDependencies_ = Arrays.asList(artifactSourceDependencies);
		artifactIBDFDependencies_ = Arrays.asList(artifactIBDFDependencies);
		uploadFileInfo_ = Arrays.asList(uploadFileInfo);
		converterArtifactId_ = converterArtifactId;
		converterOutputArtifactId_ = converterOutputArtifactId;
		converterMojoName_ = converterMojoName;
		sourceUploadGroupId_ = sourceUploadGroupId;
		niceName_ = niceName;
		licenseInformation = new String[licenseFilePaths.length];
		noticeInformation = new String[noticeFilePaths.length];
		try
		{
			for (int i = 0; i < licenseFilePaths.length; i++)
			{
				if (StringUtils.isBlank(licenseFilePaths[i]))
				{
					licenseInformation[i] = "";
				}
				else
				{
					licenseInformation[i] =  FileUtil.readFile(licenseFilePaths[i]);
				}
			}
			for (int i = 0; i < noticeFilePaths.length; i++)
			{
				if (StringUtils.isBlank(noticeFilePaths[i]))
				{
					noticeInformation[i] = "";
				}
				else
				{
					noticeInformation[i] =  FileUtil.readFile(noticeFilePaths[i]);
				}
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Note that the artifactID may include a wildcard ('*') for some, such as SCT_EXTENSION - note - this is the pattern
	 * for the source artifact upload, not the artifact id related to the converter.
	 * 
	 * This is used during SOURCE UPLOAD
	 */
	public String getArtifactId()
	{
		return srcArtifactId_;
	}

	/**
	 * In order to execute a conversion of the specified type, you must also provide dependencies for each of the listed
	 * Source artifact identifiers.
	 * 
	 * This is used during SOURCE UPLOAD
	 */
	public List<String> getArtifactDependencies()
	{
		return artifactSrcDependencies_;
	}
	
	/**
	 * In order to execute a conversion of the specified type, you must also provide dependencies for each of the listed
	 * IBDF artifact identifiers.
	 * 
	 * This is used during IBDF CONVERSION
	 */
	public List<String> getIBDFDependencies()
	{
		return artifactIBDFDependencies_;
	}
	
	/**
	 * The information describing the files that an end user must upload into the system to allow the execution of a particular converter.
	 * 
	 * This is used during SOURCE UPLOAD
	 */
	public List<UploadFileInfo> getUploadFileInfo()
	{
		return uploadFileInfo_;
	}

	/**
	 * Not for PRISME
	 */
	protected String getConverterGroupId()
	{
		return converterGroupId_;
	}

	/**
	 * Not for PRISME
	 */
	protected String getConverterArtifactId()
	{
		return converterArtifactId_;
	}
	
	/**
	 * Not for PRISME
	 */
	protected String getConverterOutputArtifactId()
	{
		return converterOutputArtifactId_;
	}

	/**
	 * Not for PRISME
	 */
	protected String getConverterMojoName()
	{
		return converterMojoName_;
	}

	/**
	 * Not for PRISME
	 */
	public String getSourceUploadGroupId()
	{
		return sourceUploadGroupId_;
	}

	/**
	 * Not for PRISME (but you can use it if you want)
	 */
	public String getNiceName()
	{
		return niceName_;
	}

	/**
	 * Not for PRISME
	 */
	public String[] getLicenseInformation()
	{
		return licenseInformation;
	}

	/**
	 * Not for PRISME
	 */
	public String[] getNoticeInformation()
	{
		return noticeInformation;
	}
}

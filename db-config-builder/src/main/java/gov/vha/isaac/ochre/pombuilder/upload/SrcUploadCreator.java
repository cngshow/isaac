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
package gov.vha.isaac.ochre.pombuilder.upload;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import gov.vha.isaac.ochre.pombuilder.FileUtil;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.converter.SupportedConverterTypes;

/**
 * 
 * {@link SrcUploadCreator}
 * Create a new maven pom project which when executed, will upload a set of SDO input files
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class SrcUploadCreator
{
	/**
	 * @param gitRepositoryURL - The URL to publish this built project to
	 * @param gitUsername - The username to utilize to publish this project
	 * @param getPassword - the password to utilize to publish this project
	 * @return the tag created in the repository that carries the created project
	 * @throws Exception 
	 */
	public static String createSrcUploadConfiguration(SupportedConverterTypes uploadType, String version, String extensionName, 
			String gitRepositoryURL, String gitUsername, String gitPassword,
			String artifactRepository, String repositoryUsername, String repositoryPassword) throws Exception
	{
		File f = Files.createTempDirectory("srcUpload").toFile();
		StringBuffer noticeAppend = new StringBuffer();
		HashMap<String, String> pomSwaps = new HashMap<>();
		
		pomSwaps.put("#VERSION#", version);
		pomSwaps.put("#SCM_URL#", gitRepositoryURL);
		
		switch(uploadType)
		{
			case SCT:
				noticeAppend.append(FileUtil.readFile("shared/noticeAdditions/rf2-sct-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", FileUtil.readFile("shared/licenses/sct.xml"));
				pomSwaps.put("#GROUPID#", "gov.vha.isaac.terminology.source.rf2");
				pomSwaps.put("#ARTIFACTID#", "rf2-src-data-sct");
				pomSwaps.put("#NAME#", "SnomedCT Source Upload");
				break;
			case SCT_EXTENSION:
				noticeAppend.append(FileUtil.readFile("shared/noticeAdditions/rf2-sct-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", FileUtil.readFile("shared/licenses/sct.xml"));
				pomSwaps.put("#GROUPID#", "gov.vha.isaac.terminology.source.rf2");
				pomSwaps.put("#ARTIFACTID#", "rf2-src-data-" +extensionName + "-extension");
				pomSwaps.put("#NAME#", "SnomedCT Extension Source Upload");
				break;
			case LOINC:
				noticeAppend.append(FileUtil.readFile("shared/noticeAdditions/loinc-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", FileUtil.readFile("shared/licenses/loinc.xml"));
				pomSwaps.put("#GROUPID#", "gov.vha.isaac.terminology.source.loinc");
				pomSwaps.put("#ARTIFACTID#", "loinc-src-data");
				pomSwaps.put("#NAME#", "LOINC Source Upload");
				break;
			case LOINC_TECH_PREVIEW:
				noticeAppend.append(FileUtil.readFile("shared/noticeAdditions/loinc-tech-preview-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", FileUtil.readFile("shared/licenses/loinc.xml"));
				pomSwaps.put("#GROUPID#", "gov.vha.isaac.terminology.source.loinc");
				pomSwaps.put("#ARTIFACTID#", "loinc-src-data-tech-preview");
				pomSwaps.put("#NAME#", "LOINC Tech Preview Source Upload");
				break;
			case VHAT:
				pomSwaps.put("#LICENSE#", "");
				pomSwaps.put("#GROUPID#", "gov.vha.isaac.terminology.source.vhat");
				pomSwaps.put("#ARTIFACTID#", "vhat-src-data");
				pomSwaps.put("#NAME#", "VHAT Source Upload");
				break;
			case RXNORM:
				noticeAppend.append(FileUtil.readFile("shared/noticeAdditions/rxnorm-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", FileUtil.readFile("shared/licenses/rxnorm.xml"));
				pomSwaps.put("#GROUPID#", "gov.vha.isaac.terminology.source.rxnorm");
				pomSwaps.put("#ARTIFACTID#", "rxnorm-src-data");
				pomSwaps.put("#NAME#", "RxNorm Source Upload");
				break;
			//TODO RXNORM_SOLOR support
			default :
				throw new RuntimeException("oops");
		}
		
		String tagWithoutRevNumber = pomSwaps.get("#GROUPID#") + "/" + pomSwaps.get("#ARTIFACTID#") + "/" + pomSwaps.get("#VERSION#");
		
		ArrayList<String> existingTags = GitPublish.readTags(gitRepositoryURL, gitUsername, gitPassword);
		int highestBuildRevision = GitPublish.readHighestRevisionNumber(existingTags, tagWithoutRevNumber);
		
		String tag;
		//Fix version number
		if (highestBuildRevision == -1)
		{
			//no tag for this artifact.  tag it without a rev number
			pomSwaps.put("#VERSION#", pomSwaps.get("#VERSION#"));
			tag = tagWithoutRevNumber;
		}
		else
		{
			//increment the rev number
			pomSwaps.put("#VERSION#", pomSwaps.get("#VERSION#") + "-" + (highestBuildRevision + 1));
			tag = tagWithoutRevNumber + "-" + (highestBuildRevision + 1);
		}

		pomSwaps.put("#SCM_TAG#", tag);

		FileUtil.writeFile("shared", "LICENSE.txt", f);
		FileUtil.writeFile("shared", "NOTICE.txt", f, null, noticeAppend.toString());
		FileUtil.writeFile("srcUploadProjectTemplate", "native-source/DOTgitignore", f);
		FileUtil.writeFile("srcUploadProjectTemplate", "assembly.xml", f);
		FileUtil.writeFile("srcUploadProjectTemplate", "pom.xml", f, pomSwaps, "");
		
		GitPublish.publish(f, gitRepositoryURL, gitUsername, gitPassword, tag);
		return tag;
	}
}

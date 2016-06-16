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
import org.apache.commons.lang3.StringUtils;
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
	 * @param uploadType - What type of content is being uploaded.
	 * @param version - What version number does the passed in content represent
	 * @param extensionName - optional - If the upload type is a type such as {@link SupportedConverterTypes#SCT_EXTENSION} which contains a 
	 * wildcard '*' in its {@link SupportedConverterTypes#getArtifactId()} value, this parameter must be provided, and is the string to use to 
	 * replace the wildcard.  This would typically be a value such as "en" or "fr", when used for snomed extension content.
	 * @param folderContainingContent - The folder that contains the required data files - these files will be zipped into an artifact and uploaded
	 * to the artifactRepositoryURL.
	 * @param gitRepositoryURL - The URL to publish this built project to
	 * @param gitUsername - The username to utilize to publish this project
	 * @param getPassword - The password to utilize to publish this project
	 * @return the tag created in the repository that carries the created project
	 * @param artifactRepositoryURL - The artifact server (nexus) path where the created artifact should be transferred.  This path should go all the way down to 
	 * a specific repository, such as http://vadev.mantech.com:8081/nexus/content/repositories/releases/ or http://vadev.mantech.com:8081/nexus/content/repositories/termdata/
	 * This should not point to a URL that represents a 'group' repository view.
	 * @param repositoryUsername - The username to utilize to upload the artifact to the artifact server
	 * @param repositoryPassword - The passwordto utilize to upload the artifact to the artifact server
	 * @return
	 * @throws Exception
	 */
	public static String createSrcUploadConfiguration(SupportedConverterTypes uploadType, String version, String extensionName, File folderContainingContent, 
			String gitRepositoryURL, String gitUsername, String gitPassword,
			String artifactRepositoryURL, String repositoryUsername, String repositoryPassword) throws Exception
	{
		if (folderContainingContent == null || !folderContainingContent.isDirectory())
		{
			throw new Exception("The provided path does not exist as a folder!");
		}
		if (folderContainingContent.listFiles().length == 0)
		{
			throw new Exception("No content was found to upload!");
		}
		
		//Otherwise, move forward.  Create our native-source folder, and move everything into it.
		File nativeSource = new File(folderContainingContent, "native-source");
		if (nativeSource.exists())
		{
			throw new RuntimeException("Unexpected file found in upload content!");
		}
		File[] filesToUpload = folderContainingContent.listFiles();
		nativeSource.mkdir();  //make this after listing the pre-existing files
		for (File f : filesToUpload)
		{
			//validate it is a file, move it into native-source
			if (f.isFile())
			{
				Files.move(f.toPath(), nativeSource.toPath().resolve(f.toPath().getFileName()));
			}
			else
			{
				throw new Exception("Unexpected directory found in upload content!");
			}
		}
		
		
		StringBuffer noticeAppend = new StringBuffer();
		HashMap<String, String> pomSwaps = new HashMap<>();
		
		pomSwaps.put("#VERSION#", version);
		pomSwaps.put("#SCM_URL#", gitRepositoryURL);
		if (uploadType.getArtifactId().contains("*") && StringUtils.isBlank(extensionName))
		{
			throw new Exception("ExtensionName is required when the upload type artifact id contains a wildcard");
		}
		
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
				pomSwaps.put("#ARTIFACTID#", "rf2-src-data-" + extensionName + "-extension");
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
			//No tag at all - create without rev number, don't need to change our pomSwaps
			tag = tagWithoutRevNumber;
		}
		else
		{
			//If we are a SNAPSHOT, don't embed a build number, because nexus won't allow the upload, otherwise, embed a rev number
			if (!pomSwaps.get("#VERSION#").endsWith("SNAPSHOT"))
			{
				pomSwaps.put("#VERSION#", pomSwaps.get("#VERSION#") + "-" + (highestBuildRevision + 1));
			}
			tag = tagWithoutRevNumber + "-" + (highestBuildRevision + 1);
		}

		pomSwaps.put("#SCM_TAG#", tag);

		FileUtil.writeFile("shared", "LICENSE.txt", folderContainingContent);
		FileUtil.writeFile("shared", "NOTICE.txt", folderContainingContent, null, noticeAppend.toString());
		FileUtil.writeFile("srcUploadProjectTemplate", "native-source/DOTgitignore", folderContainingContent);
		FileUtil.writeFile("srcUploadProjectTemplate", "assembly.xml", folderContainingContent);
		FileUtil.writeFile("srcUploadProjectTemplate", "pom.xml", folderContainingContent, pomSwaps, "");
		
		GitPublish.publish(folderContainingContent, gitRepositoryURL, gitUsername, gitPassword, tag);
		return tag;
		//TODO implement nexus upload
	}
}

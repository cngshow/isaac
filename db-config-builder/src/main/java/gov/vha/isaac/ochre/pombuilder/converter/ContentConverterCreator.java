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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Set;
import gov.vha.isaac.ochre.api.util.UUIDUtil;
import gov.vha.isaac.ochre.pombuilder.FileUtil;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.VersionFinder;
import gov.vha.isaac.ochre.pombuilder.artifacts.Converter;
import gov.vha.isaac.ochre.pombuilder.artifacts.IBDFFile;
import gov.vha.isaac.ochre.pombuilder.artifacts.SDOSourceContent;
import javafx.util.Pair;

/**
 * 
 * {@link ContentConverterCreator}
 *
 * A class that has the convenience methods that will construct and publish a pom project - which when executed, will
 * convert SDO source content into IBDF content.  The convenience methods in this class carry all of the documentation 
 * and information necessary to create various conversion types.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ContentConverterCreator
{
	private static final Logger LOG = LogManager.getLogger();
	
	/**
	 * Return information about all of the supported conversion types, including all of the information types
	 * that must be supplied with each converter.
	 * @return
	 */
	public static SupportedConverterTypes[] getSupportedConversions()
	{
		return SupportedConverterTypes.values();
	}
	
	/**
	 * Will return an Artifact with only the groupID and artifactID populated - this represents the 
	 * artifact that is capable of handling the conversion of the specified source content.
	 * @param artifactId - the artifactid of the source content that the user desires to converter.
	 * @return - the group and artifact id of the converter tool that is capable of handling that content.
	 */
	public static Converter getConverterForSourceArtifact(String artifactId)
	{
		SupportedConverterTypes supportedConverterType = getConverterType(artifactId).getKey();
		
		return new Converter(supportedConverterType.getConverterGroupId(), supportedConverterType.getConverterArtifactId(), "");
	}
	
	private static Pair<SupportedConverterTypes, String> getConverterType(String artifactId)
	{
		SupportedConverterTypes conversionType = null;
		String extensionSuffix = "";
		for (SupportedConverterTypes type : SupportedConverterTypes.values())
		{
			if (type.getArtifactId().equals(artifactId))
			{
				conversionType = type;
				break;
			}
			if (type.getArtifactId().contains("*"))
			{
				String[] temp = type.getArtifactId().split("\\*");
				if (artifactId.startsWith(temp[0]) && artifactId.endsWith(temp[1]))
				{
					conversionType = type;
					extensionSuffix = artifactId.substring(temp[0].length(), artifactId.length());
					break;
				}
			}
		}
		if (conversionType == null)
		{
			throw new RuntimeException("Unuspported source content artifact type");
		}
		return new Pair<>(conversionType, extensionSuffix);
	}
	
	
	/**
	 * Create a source conversion project which is executable via maven.
	 * @param sourceContent - The artifact information for the content to be converted.  The artifact information must follow known naming conventions - group id should 
	 * be gov.vha.isaac.terminology.source.  Currently supported artifactIds are 'loinc-src-data', 'loinc-src-data-tech-preview', 'rf2-src-data-*', 'vhat' 
	 * @param converterVersion - The version number of the content converter code to utilize.  The jar file for this converter must be available to the 
	 * maven execution environment at the time when the conversion is run.
	 * @param additionalSourceDependencies - Some converters require additional data files to satisfy dependencies. See {@link #getSupportedConversions()} 
	 * for accurate dependencies for any given conversion type.
	 * @param additionalIBDFDependencies - Some converters require additional data files to satisfy dependencies. See {@link #getSupportedConversions()} 
	 * for accurate dependencies for any given conversion type.
	 * @param converterOptionValues a map of converter options (fetched via {@link #getConverterOptions(Converter, String, String, String)} to a set of values.
	 * The values are the items that the user selected / entered.  This may be blank, depending on the converter and/or the user choices.
	 * @param gitRepositoryURL - The URL to publish this built project to
	 * @param gitUsername - The username to utilize to publish this project
	 * @param gitPassword - the password to utilize to publish this project
	 * @return the tag created in the repository that carries the created project
	 * @throws Exception
	 */
	public static String createContentConverter(SDOSourceContent sourceContent, String converterVersion, SDOSourceContent[] additionalSourceDependencies, 
		IBDFFile[] additionalIBDFDependencies, Map<ConverterOptionParam, Set<String>> converterOptionValues, String gitRepositoryURL, String gitUsername, char[] gitPassword) 
				throws Exception
	{
		File f = Files.createTempDirectory("converter-builder").toFile();
		
		Pair<SupportedConverterTypes, String> artifactInfo = getConverterType(sourceContent.getArtifactId());
		SupportedConverterTypes conversionType = artifactInfo.getKey();
		String extensionSuffix = artifactInfo.getValue();
		
		StringBuilder extraProperties = new StringBuilder();
		
		FileUtil.writeFile("converterProjectTemplate", "src/assembly/MANIFEST.MF", f, new HashMap<>(), "");
		FileUtil.writeFile("shared", "LICENSE.txt", f, new HashMap<>(), "");
		
		StringBuffer noticeAppend = new StringBuffer();
		HashMap<String, String> pomSwaps = new HashMap<>();
		
		pomSwaps.put("#VERSION#", sourceContent.getVersion() + "-loader-" + converterVersion);
		
		pomSwaps.put("#NAME#", conversionType.getNiceName() + " Artifact Converter");
		
		pomSwaps.put("#SOURCE_DATA_VERSION#", sourceContent.getVersion());
		pomSwaps.put("#LOADER_VERSION#", converterVersion);
		
		pomSwaps.put("#SCM_URL#", GitPublish.constructChangesetRepositoryURL(gitRepositoryURL));
		
		String temp = FileUtil.readFile("converterProjectTemplate/pomSnippits/fetchExecution.xml");
		temp = temp.replace("#GROUPID#", sourceContent.getGroupId());
		temp = temp.replace("#ARTIFACTID#", sourceContent.getArtifactId());
		temp = temp.replace("#VERSION#", sourceContent.getVersion());
		
		StringBuilder fetches = new StringBuilder(temp);
		
		for (SDOSourceContent ac : additionalSourceDependencies)
		{
			temp = FileUtil.readFile("converterProjectTemplate/pomSnippits/fetchExecution.xml");
			temp = temp.replace("#GROUPID#", ac.getGroupId());
			temp = temp.replace("#ARTIFACTID#", ac.getArtifactId());
			temp = temp.replace("#VERSION#", ac.getVersion());
			fetches.append(temp);
			extraProperties.append("<" + ac.getArtifactId() + ".version>" + ac.getVersion() + "</" + ac.getArtifactId() + ".version>\n");
		}
		
		pomSwaps.put("#FETCH_EXECUTION#", fetches.toString());

		StringBuilder dependencies = new StringBuilder();
		StringBuilder unpackArtifacts = new StringBuilder();
		String unpackDependencies = "";
		if (additionalIBDFDependencies.length > 0)
		{
			unpackDependencies = FileUtil.readFile("converterProjectTemplate/pomSnippits/unpackDependency.xml");
			for (IBDFFile ibdf : additionalIBDFDependencies)
			{
				temp = FileUtil.readFile("converterProjectTemplate/pomSnippits/ibdfDependency.xml");
				temp = temp.replace("#GROUPID#", ibdf.getGroupId());
				temp = temp.replace("#ARTIFACTID#", ibdf.getArtifactId());
				temp = temp.replace("#CLASSIFIER#", (ibdf.hasClassifier() ? ibdf.getClassifier() : ""));
				temp = temp.replace("#VERSION#", ibdf.getVersion());
				dependencies.append(temp);
				unpackArtifacts.append(ibdf.getArtifactId());
				unpackArtifacts.append(",");
			}
			temp = FileUtil.readFile("converterProjectTemplate/pomSnippits/ibdfDependency.xml");
			temp = temp.replace("#GROUPID#", "gov.vha.isaac.ochre.modules");
			temp = temp.replace("#ARTIFACTID#", "ochre-metadata");
			temp = temp.replace("#CLASSIFIER#", "all");
			temp = temp.replace("#VERSION#", VersionFinder.findProjectVersion());
			dependencies.append(temp);
			unpackArtifacts.append("ochre-metadata");
			unpackDependencies = unpackDependencies.replace("#UNPACK_ARTIFACTS#", unpackArtifacts.toString());
		}
		
		pomSwaps.put("#IBDF_DEPENDENCY#", dependencies.toString());
		
		pomSwaps.put("#UNPACK_DEPENDENCIES#", unpackDependencies);
		
		String goal = conversionType.getConverterMojoName();
		
		pomSwaps.put("#LOADER_ARTIFACT#", conversionType.getConverterArtifactId());
		pomSwaps.put("#ARTIFACTID#", conversionType.getConverterOutputArtifactId() + extensionSuffix);
		StringBuffer licenseInfo = new StringBuffer();
		for (String s : conversionType.getLicenseInformation())
		{
			licenseInfo.append(s);
		}
		pomSwaps.put("#LICENSE#", licenseInfo.toString());
		for (String s : conversionType.getNoticeInformation())
		{
			noticeAppend.append(s);
		}
		
		StringBuilder userOptions = new StringBuilder();
		if (converterOptionValues != null)
		{
			String optionIndent = "									";
			for (Entry<ConverterOptionParam, Set<String>> option : converterOptionValues.entrySet())
			{
				if (option.getValue() != null)
				{
					if (!option.getKey().isAllowMultiSelect() && option.getValue().size() > 1)
					{
						throw new Exception("The option " + option.getKey().getDisplayName() + " allows at most, one value");
					}
					if (!option.getKey().isAllowNoSelection() && option.getValue().size() == 0)
					{
						throw new Exception("This option " + option.getKey().getDisplayName() + " requires a value");
					}
					if (option.getValue().size() > 0)
					{
						if (option.getKey().isAllowMultiSelect())
						{
							userOptions.append(optionIndent + "<" + option.getKey().getInternalName() + "s>\n");
							for (String value : option.getValue())
							{
								userOptions.append(optionIndent + "\t<" + option.getKey().getInternalName() + ">");
								if (UUIDUtil.isUUID(value))
								{
									userOptions.append("\n");
									userOptions.append(optionIndent + "\t\t<description></description>\n");  //Its ok not to populate this
									userOptions.append(optionIndent + "\t\t<uuid>" + value + "</uuid>\n");
									userOptions.append(optionIndent + "\t");
								}
								else
								{
									userOptions.append(value);
								}
								userOptions.append("</" + option.getKey().getInternalName() + ">\n");
							}
							userOptions.append(optionIndent + "</" + option.getKey().getInternalName() + "s>");
						}
						else
						{
							String value = option.getValue().iterator().next();
							userOptions.append(optionIndent + "<" + option.getKey().getInternalName() + ">");
							if (UUIDUtil.isUUID(value))
							{
								userOptions.append("\n");
								userOptions.append(optionIndent + "\t<description></description>\n");  //Its ok not to populate this
								userOptions.append(optionIndent + "\t<uuid>" + value + "</uuid>\n");
								userOptions.append(optionIndent);
							}
							else
							{
								userOptions.append(value);
							}
							userOptions.append("</" + option.getKey().getInternalName() + ">");
						}
					}
				}
				else if (!option.getKey().isAllowNoSelection())
				{
					throw new Exception("This option " + option.getKey().getDisplayName() + " requires a value");
				}
			}
		}

		StringBuilder profiles = new StringBuilder();
		String[] classifiers = new String[] {};
		switch(conversionType)
		{
			case SCT: case SCT_EXTENSION:
				classifiers = new String[] {"Snapshot", "Delta", "Full"};
				break;
			default :
				classifiers = new String[] {""};
				break;
			
		}
		for (String classifier : classifiers)
		{
			temp = FileUtil.readFile("converterProjectTemplate/pomSnippits/profile.xml");
			temp = temp.replaceAll("#CLASSIFIER#", classifier);
			temp = temp.replaceAll("#CONVERTER#", conversionType.getConverterArtifactId());
			temp = temp.replaceAll("#CONVERTER_VERSION#", converterVersion);
			temp = temp.replaceAll("#GOAL#", goal);
			temp = temp.replaceAll("#USER_CONFIGURATION_OPTIONS#", userOptions.toString());
			profiles.append(temp);
			
			String assemblyInfo = FileUtil.readFile("converterProjectTemplate/src/assembly/assembly.xml");
			StringBuilder assemblySnippits = new StringBuilder();
			for (String classifier2 : classifiers)
			{
				String assemblyRef = FileUtil.readFile("converterProjectTemplate/src/assembly/assemblySnippits/assemblyRef.xml");
				assemblyRef = assemblyRef.replace("#ASSEMBLY#", "assembly-" + classifier2 + ".xml");
				assemblySnippits.append(assemblyRef);
			}
			assemblyInfo = assemblyInfo.replace("#ASSEMBLY_FILES#", assemblySnippits.toString());
			assemblyInfo = assemblyInfo.replaceAll("#CLASSIFIER#", classifier);
			File assemblyFile = new File(f, "src/assembly/assembly-" + classifier + ".xml");
			assemblyFile.getParentFile().mkdirs();
			Files.write(assemblyFile.toPath(), assemblyInfo.getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		}
		
		pomSwaps.put("#PROFILE#", profiles.toString());
		
		String tagWithoutRevNumber = "gov.vha.isaac.terminology.converted" + "/" + pomSwaps.get("#ARTIFACTID#") + "/" + pomSwaps.get("#VERSION#");
		
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
		if (extraProperties.length() > 0)
		{
			extraProperties.setLength(extraProperties.length() - 1);
		}
		pomSwaps.put("#EXTRA_PROPERTIES#", extraProperties.toString());
		
		FileUtil.writeFile("shared", "NOTICE.txt", f, new HashMap<>(), noticeAppend.toString());
		FileUtil.writeFile("converterProjectTemplate", "pom.xml", f, pomSwaps, "");
		
		GitPublish.publish(f, gitRepositoryURL, gitUsername, gitPassword, tag);
		try
		{
			FileUtil.recursiveDelete(f);
		}
		catch (Exception e)
		{
			LOG.error("Problem cleaning up temp folder " + f, e);
		}
		return tag;
	}

	/**
	 * @see {@link ConverterOptionParam#fromArtifact(Converter, String, String, String)};
	 */
	public static ConverterOptionParam[] getConverterOptions(Converter converter, String repositoryBaseURL, String repositoryUsername, String repositoryPassword) 
			throws Exception
	{
		return ConverterOptionParam.fromArtifact(converter, repositoryBaseURL, repositoryUsername, repositoryPassword);
	}
}

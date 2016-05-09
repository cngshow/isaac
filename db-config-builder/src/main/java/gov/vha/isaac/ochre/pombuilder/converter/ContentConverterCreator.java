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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map.Entry;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.VersionFinder;
import gov.vha.isaac.ochre.pombuilder.artifacts.Artifact;
import gov.vha.isaac.ochre.pombuilder.artifacts.IBDFFile;
import gov.vha.isaac.ochre.pombuilder.artifacts.SDOSourceContent;
import gov.vha.isaac.ochre.pombuilder.dbbuilder.DBConfigurationCreator;
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
	public static Artifact getConverterForSourceArtifact(String artifactId)
	{
		switch(getConverterType(artifactId).getKey())
		{
			case SCT:
				return new SDOSourceContent("gov.vha.isaac.terminology.converters", "rf2-mojo", "");
			case LOINC:
				return new SDOSourceContent("gov.vha.isaac.terminology.converters", "loinc-mojo", "");
			case LOINC_TECH_PREVIEW:
				return new SDOSourceContent("gov.vha.isaac.terminology.converters", "loinc-mojo", "");
			case SCT_EXTENSION:
				return new SDOSourceContent("gov.vha.isaac.terminology.converters", "rf2-mojo", "");
			case VHAT:
				return new SDOSourceContent("gov.vha.isaac.terminology.converters", "vhat-mojo", "");
			default :
				throw new RuntimeException("Oops");
		}
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
	 * @param gitRepositoryURL - The URL to publish this built project to
	 * @param gitUsername - The username to utilize to publish this project
	 * @param gitPassword - the password to utilize to publish this project
	 * @return the tag created in the repository that carries the created project
	 * @throws Exception
	 */
	public static String createContentConverter(SDOSourceContent sourceContent, String converterVersion, SDOSourceContent[] additionalSourceDependencies, 
		IBDFFile[] additionalIBDFDependencies, String gitRepositoryURL, String gitUsername, String gitPassword) throws Exception
	{
		File f = Files.createTempDirectory("converter-builder").toFile();
		
		Pair<SupportedConverterTypes, String> artifactInfo = getConverterType(sourceContent.getArtifactId());
		SupportedConverterTypes conversionType = artifactInfo.getKey();
		String extensionSuffix = artifactInfo.getValue();
		
		writeFile("converterProjectTemplate", "src/assembly/MANIFEST.MF", f, new HashMap<>(), "");
		writeFile("converterProjectTemplate", "LICENSE.txt", f, new HashMap<>(), "");
		
		StringBuffer noticeAppend = new StringBuffer();
		HashMap<String, String> pomSwaps = new HashMap<>();
		
		pomSwaps.put("#VERSION#", sourceContent.getVersion() + "-loader-" + converterVersion);
		
		pomSwaps.put("#NAME#", conversionType.name() + " Artifact Converter");
		
		pomSwaps.put("#SOURCE_DATA_VERSION#", sourceContent.getVersion());
		pomSwaps.put("#LOADER_VERSION#", converterVersion);
		
		pomSwaps.put("#SCM_URL#", gitRepositoryURL);
		
		String temp = readFile("converterProjectTemplate/pomSnippits/fetchExecution.xml");
		temp = temp.replace("#GROUPID#", sourceContent.getGroupId());
		temp = temp.replace("#ARTIFACTID#", sourceContent.getArtifactId());
		temp = temp.replace("#VERSION#", sourceContent.getVersion());
		
		StringBuilder fetches = new StringBuilder(temp);
		
		for (SDOSourceContent ac : additionalSourceDependencies)
		{
			temp = readFile("converterProjectTemplate/pomSnippits/fetchExecution.xml");
			temp = temp.replace("#GROUPID#", ac.getGroupId());
			temp = temp.replace("#ARTIFACTID#", ac.getArtifactId());
			temp = temp.replace("#VERSION#", ac.getVersion());
			fetches.append(temp);
		}
		
		pomSwaps.put("#FETCH_EXECUTION#", fetches.toString());

		StringBuilder dependencies = new StringBuilder();
		StringBuilder unpackArtifacts = new StringBuilder();
		String unpackDependencies = "";
		if (additionalIBDFDependencies.length > 0)
		{
			unpackDependencies = readFile("converterProjectTemplate/pomSnippits/unpackDependency.xml");
			for (IBDFFile ibdf : additionalIBDFDependencies)
			{
				temp = readFile("converterProjectTemplate/pomSnippits/ibdfDependency.xml");
				temp = temp.replace("#GROUPID#", ibdf.getGroupId());
				temp = temp.replace("#ARTIFACTID#", ibdf.getArtifactId());
				temp = temp.replace("#CLASSIFIER#", ibdf.getClassifier());
				temp = temp.replace("#VERSION#", ibdf.getVersion());
				dependencies.append(temp);
				unpackArtifacts.append(ibdf.getArtifactId());
				unpackArtifacts.append(",");
			}
			temp = readFile("converterProjectTemplate/pomSnippits/ibdfDependency.xml");
			temp = temp.replace("#GROUPID#", "gov.vha.isaac.ochre.modules");
			temp = temp.replace("#ARTIFACTID#", "metadata");
			temp = temp.replace("#CLASSIFIER#", "all");
			temp = temp.replace("#VERSION#", VersionFinder.findProjectVersion());
			dependencies.append(temp);
			unpackArtifacts.append("metadata");
			unpackDependencies = unpackDependencies.replace("#UNPACK_ARTIFACTS#", unpackArtifacts.toString());
		}
		
		pomSwaps.put("#IBDF_DEPENDENCY#", dependencies.toString());
		
		pomSwaps.put("#UNPACK_DEPENDENCIES#", unpackDependencies);
		
		String goal = null;
		String converter = null;
		
		switch(conversionType)
		{
			case SCT:
				noticeAppend.append(readFile("converterProjectTemplate/noticeAdditions/rf2-sct-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", readFile("converterProjectTemplate/pomSnippits/licenses/sct.xml"));
				pomSwaps.put("#ARTIFACTID#", "rf2-ibdf-sct");
				pomSwaps.put("#LOADER_ARTIFACT#", "rf2-mojo");
				converter = "rf2-mojo";
				goal = "convert-RF2-to-ibdf";
				break;
			case LOINC:
				noticeAppend.append(readFile("converterProjectTemplate/noticeAdditions/loinc-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", readFile("converterProjectTemplate/pomSnippits/licenses/loinc.xml"));
				pomSwaps.put("#ARTIFACTID#", "loinc-ibdf");
				pomSwaps.put("#LOADER_ARTIFACT#", "loinc-mojo");
				converter = "loinc-mojo";
				goal = "convert-loinc-to-ibdf";
				break;
			case LOINC_TECH_PREVIEW:
				noticeAppend.append(readFile("converterProjectTemplate/noticeAdditions/loinc-NOTICE-addition.txt"));
				noticeAppend.append(readFile("converterProjectTemplate/noticeAdditions/loinc-tech-preview-NOTICE-addition.txt"));
				noticeAppend.append(readFile("converterProjectTemplate/noticeAdditions/rf2-sct-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", readFile("converterProjectTemplate/pomSnippits/licenses/loinc.xml"));
				pomSwaps.put("#ARTIFACTID#", "loinc-ibdf-tech-preview");
				pomSwaps.put("#LOADER_ARTIFACT#", "loinc-mojo");
				converter = "loinc-mojo";
				goal = "convert-loinc-tech-preview-to-ibdf";
				break;
			case SCT_EXTENSION:
				noticeAppend.append(readFile("converterProjectTemplate/noticeAdditions/rf2-sct-NOTICE-addition.txt"));
				pomSwaps.put("#LICENSE#", readFile("converterProjectTemplate/pomSnippits/licenses/sct.xml"));
				pomSwaps.put("#ARTIFACTID#", "rf2-ibdf-" + extensionSuffix);
				pomSwaps.put("#LOADER_ARTIFACT#", "rf2-mojo");
				converter = "rf2-mojo";
				goal = "convert-RF2-to-ibdf";
				break;
			case VHAT:
				pomSwaps.put("#LICENSE#", "");
				pomSwaps.put("#ARTIFACTID#", "vhat-ibdf");
				pomSwaps.put("#LOADER_ARTIFACT#", "vhat-mojo");
				converter = "vhat-mojo";
				goal = "convert-VHAT-to-ibdf";
				break;
			default :
				throw new RuntimeException("oops");
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
			temp = readFile("converterProjectTemplate/pomSnippits/profile.xml");
			temp = temp.replaceAll("#CLASSIFIER#", classifier);
			temp = temp.replaceAll("#CONVERTER#", converter);
			temp = temp.replaceAll("#CONVERTER_VERSION#", converterVersion);
			temp = temp.replaceAll("#GOAL#", goal);
			profiles.append(temp);
			
			String assemblyInfo = readFile("converterProjectTemplate/src/assembly/assembly.xml");
			StringBuilder assemblySnippits = new StringBuilder();
			for (String classifier2 : classifiers)
			{
				String assemblyRef = readFile("converterProjectTemplate/src/assembly/assemblySnippits/assemblyRef.xml");
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
		
		String tag = pomSwaps.get("#ARTIFACTID#") + "/" + pomSwaps.get("#VERSION#");
		pomSwaps.put("#SCM_TAG#", tag);
		
		writeFile("converterProjectTemplate", "NOTICE.txt", f, new HashMap<>(), noticeAppend.toString());
		writeFile("converterProjectTemplate", "pom.xml", f, pomSwaps, "");
		
		GitPublish.publish(f, gitRepositoryURL, gitUsername, gitPassword, tag);
		return tag;
	}
	
	private static String readFile(String fileName) throws IOException
	{
		InputStream is = DBConfigurationCreator.class.getResourceAsStream("/" + fileName);
		byte[] buffer = new byte[is.available()];
		is.read(buffer);

		return new String(buffer, Charset.forName("UTF-8"));
	}
	
	private static void writeFile(String fromFolder, String relativePath, File toFolder, HashMap<String, String> replacementValues, String append) throws IOException
	{
		InputStream is = DBConfigurationCreator.class.getResourceAsStream("/" + fromFolder + "/" + relativePath);
		byte[] buffer = new byte[is.available()];
		is.read(buffer);

		String temp = new String(buffer, Charset.forName("UTF-8"));
		for (Entry<String, String> item : replacementValues.entrySet())
		{
			while (temp.contains(item.getKey()))
			{
				temp = temp.replace(item.getKey(), item.getValue());
			}
		}
		
		relativePath = relativePath.replaceFirst("^DOT", ".");
		
		File targetFile = new File(toFolder, relativePath);
		targetFile.getParentFile().mkdirs();
		OutputStream outStream = new FileOutputStream(targetFile);
		outStream.write(temp.getBytes());
		outStream.write(append.getBytes());
		outStream.close();
	}
}

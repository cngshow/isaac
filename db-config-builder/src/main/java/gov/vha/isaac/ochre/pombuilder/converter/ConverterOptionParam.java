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
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.ArtifactUtilities;
import gov.vha.isaac.ochre.api.util.DownloadUnzipTask;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.pombuilder.artifacts.Converter;

/**
 * 
 * {@link ConverterOptionParam}
 * 
 * The set of options that apply to a particular converter. Converters build this object, and serialize it to json, and publish it to maven.
 * Consumers (the GUI) read the json file, and pass it here, or ask us to read the json file and parse it.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ConverterOptionParam
{
	public static final String MAVEN_FILE_TYPE = "options.json";
	private String displayName;
	private String internalName;
	private String description;
	private boolean allowNoSelection;
	private boolean allowMultiSelect;
	private ConverterOptionParamSuggestedValue[] suggestedPickListValues;

	@SuppressWarnings("unused")
	private ConverterOptionParam()
	{
		//for jackson
	}

	/**
	 * @param displayName The name of this option
	 * @param internalName The name to use when writing the option to a pom file
	 * @param description A description suitable for display to end users of the system (in the GUI)
	 * @param allowNoSelection true if it is valid for the user to select 0 entries from the pick list, false if they must select 1 or more.
	 * @param allowMultiSelect true if it is valie for the user to select more than 1 entry from the pick list, false if they may select at most 1.
	 * @param suggestedPickListValues the values to provide the user to select from. This may not be an all-inclusive list of values - the
	 * user should still have the option to provide their own value. 
	 */
	@SafeVarargs
	public ConverterOptionParam(String displayName, String internalName, String description, boolean allowNoSelection, boolean allowMultiSelect,
			ConverterOptionParamSuggestedValue ... suggestedPickListValues)
	{
		this.displayName = displayName;
		this.internalName = internalName;
		this.description = description;
		this.allowNoSelection = allowNoSelection;
		this.allowMultiSelect = allowMultiSelect;
		this.suggestedPickListValues = suggestedPickListValues;
	}

	/**
	 * Read the options specification from a json file found on the provided maven artifact server, with the provided artifact type.
	 * May return an empty array, will not return null.
	 * @throws Exception 
	 */
	public static ConverterOptionParam[] fromArtifact(Converter artifact, String baseMavenUrl, String mavenUsername, String mavenPassword) throws Exception
	{
		File tempFolder = File.createTempFile("jsonDownload", "");
		tempFolder.delete();
		tempFolder.mkdir();
		
		//First, try to get the pom file to validate the params they sent us.  If this fails, they sent bad info, and we fail.
		URL pomURL = ArtifactUtilities.makeFullURL(baseMavenUrl, mavenUsername, mavenPassword, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
				artifact.getClassifier(), "pom");

		DownloadUnzipTask dut = new DownloadUnzipTask(mavenUsername, mavenPassword, pomURL, false, true, tempFolder);
		WorkExecutors.safeExecute(dut);

		File pomFile = dut.get();
		if (!pomFile.exists())
		{
			throw new Exception("Failed to find the pom file for the specified project");
		}
		else
		{
			pomFile.delete();
		}
		
		//Now that we know that the credentials / artifact / version are good - see if there is a config file (there may not be)
		try
		{
			URL config = ArtifactUtilities.makeFullURL(baseMavenUrl, mavenUsername, mavenPassword, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
					artifact.getClassifier(), MAVEN_FILE_TYPE);

			dut = new DownloadUnzipTask(mavenUsername, mavenPassword, config, false, true, tempFolder);
			WorkExecutors.safeExecute(dut);

			File jsonFile = dut.get();
			return fromFile(jsonFile);
		}
		catch (Exception e)
		{ 
			//If we successfully downloaded the pom file, but failed here, just assume this file doesn't exist / isn't applicable to this converter.
			LoggerFactory.getLogger(ConverterOptionParam.class)
				.info("No config file found for converter " + artifact.getArtifactId());
			return new ConverterOptionParam[] {};
		}
	}

	/**
	 * Read the options specification from a json file.
	 */
	public static ConverterOptionParam[] fromFile(File jsonConverterOptionFile) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(jsonConverterOptionFile, ConverterOptionParam[].class);
	}

	/**
	 * Serialize to json
	 * 
	 * @throws IOException
	 */
	public static void serialize(ConverterOptionParam[] options, File outputFile) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		try
		{
			mapper.writeValue(outputFile, options);
		}
		catch (JsonProcessingException e)
		{
			throw new RuntimeException("Unexpected error", e);
		}
	}

	/**
	 * The displayName of this option - suitable for GUI use to the end user
	 */
	public String getDisplayName()
	{
		return displayName;
	}
	
	/**
	 * The internalName of this option - use when creating the pom file
	 */
	public String getInternalName()
	{
		return internalName;
	}

	/**
	 * The description of this option suitable to display to the end user, in a GUI.
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * true if it is valid for the user to select 0 entries from the pick list, false if they must select 1 or more.
	 */
	public boolean isAllowNoSelection()
	{
		return allowNoSelection;
	}
	
	/**
	 * true if it is valie for the user to select more than 1 entry from the pick list, false if they may select at most 1.
	 */
	public boolean isAllowMultiSelect()
	{
		return allowMultiSelect;
	}

	/**
	 * @param suggestedPickListValues the suggested values to provide the user to select from. This may not be an all-inclusive list of values - the
	 * user should still have the option to provide their own value.
	 */
	public ConverterOptionParamSuggestedValue[] getSuggestedPickListValues()
	{
		return suggestedPickListValues;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (allowNoSelection ? 1231 : 1237);
		result = prime * result + (allowMultiSelect ? 1231 : 1237);
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
		result = prime * result + ((internalName == null) ? 0 : internalName.hashCode());
		result = prime * result + Arrays.hashCode(suggestedPickListValues);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		ConverterOptionParam other = (ConverterOptionParam) obj;
		if (allowNoSelection != other.allowNoSelection)
		{
			return false;
		}
		if (allowMultiSelect != other.allowMultiSelect)
		{
			return false;
		}
		if (description == null)
		{
			if (other.description != null)
			{
				return false;
			}
		}
		else if (!description.equals(other.description))
		{
			return false;
		}
		if (displayName == null)
		{
			if (other.displayName != null)
			{
				return false;
			}
		}
		else if (!displayName.equals(other.displayName))
		{
			return false;
		}
		if (internalName == null)
		{
			if (other.internalName != null)
			{
				return false;
			}
		}
		else if (!internalName.equals(other.internalName))
		{
			return false;
		}
		if (!Arrays.equals(suggestedPickListValues, other.suggestedPickListValues))
		{
			return false;
		}
		return true;
	}
}

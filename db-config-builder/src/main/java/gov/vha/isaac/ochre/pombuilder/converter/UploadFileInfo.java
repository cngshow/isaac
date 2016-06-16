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
 * 
 * {@link UploadFileInfo}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class UploadFileInfo
{
	private String suggestedSourceLocation;
	private String suggestedSourceURL;
	private String sampleName;
	private String expectedNamingPatternDescription;
	private String expectedNamingPatternRegExpPattern;
	private boolean fileIsRequired;
	
	protected UploadFileInfo(String suggestedSourceLocation, String suggestedSourceURL, String sampleName, String expectedNamingPatternDescription, 
			String expectedNamingPatternRegExpPattern, boolean fileIsRequired)
	{
		this.suggestedSourceLocation = suggestedSourceLocation;
		this.suggestedSourceURL = suggestedSourceURL;
		this.sampleName = sampleName;
		this.expectedNamingPatternDescription = expectedNamingPatternDescription;
		this.expectedNamingPatternRegExpPattern = expectedNamingPatternRegExpPattern;
		this.fileIsRequired = fileIsRequired;
	}

	/**
	 * This is not always populated - it will typically only be populated if {@link #getSuggestedSourceURL()} is NOT populated.
	 */
	public String getSuggestedSourceLocation()
	{
		return suggestedSourceLocation;
	}
	
	/**
	 * This is not always populated - it will typically only be populated if {@link #getSuggestedSourceLocation()} is NOT populated.
	 */
	public String getSuggestedSourceURL()
	{
		return suggestedSourceURL;
	}
	
	public String getSampleName()
	{
		return sampleName;
	}

	public String getExpectedNamingPatternDescription()
	{
		return expectedNamingPatternDescription;
	}

	public String getExpectedNamingPatternRegExpPattern()
	{
		return expectedNamingPatternRegExpPattern;
	}

	public boolean fileIsRequired()
	{
		return fileIsRequired;
	}
}

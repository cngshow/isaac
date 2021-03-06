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
package gov.vha.isaac.ochre.access.maint.deployment.dto;

public class PublishChecksumMessageDTO implements PublishChecksumMessage
{
	private Site site;
	private long messageId;
	private String subset;
	private String checksum;
	private String version;
	private String rawHL7Message;
	
	/**
	 * Instantiate a new HL7MessagePublication with the required parameters
	 * 
	 * @param site
	 * @param messageId
	 */
	public PublishChecksumMessageDTO(long messageId, Site site, String subset) {
		this.site = site;
		this.messageId = messageId;
		this.subset = subset;
	}

	@Override
	public String getSubset()
	{
		return subset;
	}

	@Override
	public Site getSite()
	{
		return site;
	}

	@Override
	public long getMessageId()
	{
		return messageId;
	}

	@Override
	public void setVersion(String version)
	{
		this.version = version;
	}

	@Override
	public void setChecksum(String checksum)
	{
		this.checksum = checksum;
	}

	@Override
	public void setRawHL7Message(String rawMessage)
	{
		this.rawHL7Message = rawMessage;
	}

	public String getChecksum()
	{
		return checksum;
	}

	public String getVersion()
	{
		return version;
	}

	public String getRawHL7Message()
	{
		return rawHL7Message;
	}
}

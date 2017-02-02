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

public class PublishMessageDTO
{
	private SiteDTO site;
	private long messageId;

	public PublishMessageDTO() {
	}

	/**
	 * Instantiate a new HL7MessagePublication with the required parameters
	 * 
	 * @param site
	 * @param messageId
	 */
	public PublishMessageDTO(long messageId, SiteDTO site) {
		this.site = site;
		this.messageId = messageId;
	}

	/**
	 * Returns the HL7 message ID
	 * 
	 * @return Returns the messageId.
	 */
	public long getMessageId() {
		return messageId;
	}

	/**
	 * Sets the HL7 message ID
	 * 
	 * @param messageId
	 *            The messageId to set.
	 */
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	/**
	 * Returns the site to which the message (with message ID) was sent
	 * 
	 * @return Returns the site.
	 */
	public SiteDTO getSite() {
		return site;
	}

	/**
	 * Sets the site to which the message (with message ID) was sent
	 * 
	 * @param site
	 *            The site to set.
	 */
	public void setSite(SiteDTO site) {
		this.site = site;
	}
}

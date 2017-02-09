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

public class PublishMessageDTO implements PublishMessage
{
	private Site site;
	private long messageId;

	public PublishMessageDTO() {
	}

	/**
	 * Instantiate a new HL7MessagePublication with the required parameters
	 * 
	 * @param site
	 * @param messageId
	 */
	public PublishMessageDTO(long messageId, Site site) {
		this.site = site;
		this.messageId = messageId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage#getMessageId()
	 */
	@Override
	public long getMessageId() {
		return messageId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage#setMessageId(long)
	 */
	@Override
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage#getSite()
	 */
	@Override
	public Site getSite() {
		return site;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage#setSite(gov.vha.isaac.ochre.access.maint.deployment.dto.Site)
	 */
	@Override
	public void setSite(Site site) {
		this.site = site;
	}
}

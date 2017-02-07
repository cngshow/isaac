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

import java.io.Serializable;

/**
 * @author VHAISLMURDOH
 */

public class SiteDTO implements Serializable, Comparable<Site>, Site
{
	private long id;
	private String name;
	private String vaSiteId;
	private String type;
	private String groupName;
	private String messageType;

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#getId()
	 */
	@Override
	public long getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#setId(long)
	 */
	@Override
	public void setId(long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#getGroupName()
	 */
	@Override
	public String getGroupName() {
		return groupName;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#setGroupName(java.lang.String)
	 */
	@Override
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#getType()
	 */
	@Override
	public String getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#setType(java.lang.String)
	 */
	@Override
	public void setType(String type) {
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#getVaSiteId()
	 */
	@Override
	public String getVaSiteId() {
		return vaSiteId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#setVaSiteId(java.lang.String)
	 */
	@Override
	public void setVaSiteId(String vaSiteId) {
		this.vaSiteId = vaSiteId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#getMessageType()
	 */
	@Override
	public String getMessageType() {
		return messageType;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#setMessageType(java.lang.String)
	 */
	@Override
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.Site#compareTo(gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDTO)
	 */
	@Override
	public int compareTo(Site site) {
		return this.getName().compareTo(site.getName());
	}
	
	//Comparable<SiteDTO>.compareTo(SiteDTO)
}

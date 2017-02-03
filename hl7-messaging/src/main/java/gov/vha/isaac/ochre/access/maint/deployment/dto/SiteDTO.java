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

public class SiteDTO implements Serializable, Comparable<SiteDTO>
{
	private long id;
	private String name;
	private String vaSiteId;
	private String type;
	private String groupName;
	private String messageType;

	/**
	 * @return
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return Returns the site group name.
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * @param groupName
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * @return Returns the site name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the site type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return Returns the site VA Site Id.
	 */
	public String getVaSiteId() {
		return vaSiteId;
	}

	/**
	 * @param vaSiteId
	 */
	public void setVaSiteId(String vaSiteId) {
		this.vaSiteId = vaSiteId;
	}

	/**
	 * @return Returns the site message type.
	 */
	public String getMessageType() {
		return messageType;
	}

	/**
	 * @param messageType
	 */
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SiteDTO site) {
		return this.getName().compareTo(site.getName());
	}

}

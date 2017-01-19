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
package gov.vha.isaac.ochre.deployment.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * @author VHAISLMURDOH
 * @hibernate.class table="SITE"
 *
 */
@Entity
@javax.persistence.SequenceGenerator(name="SEQ_STORE", sequenceName="SITE_SEQ", allocationSize=1 )
@Table(name="SITE")
public class Site implements Serializable, Comparable<Site>
{
	private long id;
	private String name;
	private String vaSiteId;
	private String type;
	private String groupName;
	private String messageType;

	/**
	 * @return Returns the id.
	 * @hibernate.id generator-class="gov.va.med.term.services.util.TableNameSequenceGenerator"

	 */
	@Id @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SEQ_STORE")
	public long getId()
	{
		return id;
	}
	private void setId(long id)
	{
		this.id = id;
	}
	/**
	 * @hibernate.property column="GROUPNAME" length="100"
	 * @return

	 */
	@Column (name="GROUPNAME", length=100, nullable=true)
	public String getGroupName()
	{
		return groupName;
	}
	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}

	/**
	 * @hibernate.property column="name" not-null="true"
	 * @return

	 */
	@Column (name="name", nullable=false)
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * @hibernate.property column="type" length="15" not-null="true"
	 * @return

	 */
	@Column (name="type", length=15, nullable=false)
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}

	/**
	 * @hibernate.property column="vaSiteId" length="8" not-null="true"
	 * @return
	 */
	@Column (name="vaSiteId", length=8, nullable=false)
	public String getVaSiteId()
	{
		return vaSiteId;
	}
	public void setVaSiteId(String vaSiteId)
	{
		this.vaSiteId = vaSiteId;
	}

	/**
	 * @hibernate.property column="messageType" length="1" not-null="true"
	 * @return
	 */
	@Column (name="messageType", length=1, nullable=false)
	public String getMessageType()
	{
		return messageType;
	}
	public void setMessageType(String messageType)
	{
		this.messageType = messageType;
	}

	@Override
	public int compareTo(Site site)
	{
		return this.getName().compareTo(site.getName());
	}

}

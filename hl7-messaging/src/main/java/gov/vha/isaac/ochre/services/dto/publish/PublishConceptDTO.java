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
package gov.vha.isaac.ochre.services.dto.publish;

import java.io.Serializable;
import java.util.List;

import gov.vha.isaac.ochre.services.business.DataChange.DataChangeType;

/**
 * @author VHAISAOSTRAR
 *
 */
public class PublishConceptDTO implements Serializable
{
	private String publishName;
	private Long vuid;
	private boolean active;
	private DataChangeType changeType;
	private List<NameValueDTO> propertyList;
	private List<NameValueDTO> relationshipList;
	private List<NameValueDTO> designationList;

	public PublishConceptDTO() {
		
	}
	
	public PublishConceptDTO(String publishName, 
			Long vuid, 
			boolean active, 
			DataChangeType changeType) {
		
		this.publishName = publishName;
		this.vuid = vuid;
		this.active = active;
		this.changeType = changeType;
	}
	
	public PublishConceptDTO(String publishName, 
			Long vuid, 
			boolean active, 
			DataChangeType changeType,
			List<NameValueDTO> propertyList, 
			List<NameValueDTO> relationshipList, 
			List<NameValueDTO> designationList) {
		
		this.publishName = publishName;
		this.vuid = vuid;
		this.active = active;
		this.changeType = changeType;
		this.propertyList = propertyList;
		this.relationshipList = relationshipList;
		this.designationList = designationList;
	}
	
	
	
	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active
	 *            the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the propertyList
	 */
	public List<NameValueDTO> getPropertyList() {
		return propertyList;
	}

	/**
	 * @param propertyList
	 *            the propertyList to set
	 */
	public void setPropertyList(List<NameValueDTO> propertyList) {
		this.propertyList = propertyList;
	}

	/**
	 * @return the publishName
	 */
	public String getPublishName() {
		return publishName;
	}

	/**
	 * @param publishName
	 *            the publishName to set
	 */
	public void setPublishName(String publishName) {
		this.publishName = publishName;
	}

	/**
	 * @return the relationshipList
	 */
	public List<NameValueDTO> getRelationshipList() {
		return relationshipList;
	}

	/**
	 * @param relationshipList
	 *            the relationshipList to set
	 */
	public void setRelationshipList(List<NameValueDTO> relationshipList) {
		this.relationshipList = relationshipList;
	}

	/**
	 * Get the designation list
	 * 
	 * @return the designation list
	 */
	public List<NameValueDTO> getDesignationList() {
		return designationList;
	}

	/**
	 * Set the designation list
	 * 
	 * @param designationList
	 *            to set
	 */
	public void setDesignationList(List<NameValueDTO> designationList) {
		this.designationList = designationList;
	}

	/**
	 * @return the vuid
	 */
	public Long getVuid() {
		return vuid;
	}

	/**
	 * @param vuid
	 *            the vuid to set
	 */
	public void setVuid(Long vuid) {
		this.vuid = vuid;
	}

	public DataChangeType getChangeType() {
		return changeType;
	}

	public void setChangeType(DataChangeType changeType) {
		this.changeType = changeType;
	}
}

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

/**
 * @author VHAISAOSTRAR
 *
 */
public class PublishRegionDTO implements Serializable
{
	private String regionName;
	private List<PublishConceptDTO> publishConceptDTOList;


	public PublishRegionDTO(String regionName, List<PublishConceptDTO> publishConceptDTOList)
	{
		super();
		this.regionName = regionName;
		this.publishConceptDTOList = publishConceptDTOList;
	}
	/**
	 * @return the publishConceptDTOList
	 */
	public List<PublishConceptDTO> getPublishConceptDTOList()
	{
		return publishConceptDTOList;
	}
	/**
	 * @param publishConceptDTOList the publishConceptDTOList to set
	 */
	public void setPublishConceptDTOList(
			List<PublishConceptDTO> publishConceptDTOList)
	{
		this.publishConceptDTOList = publishConceptDTOList;
	}
	/**
	 * @return the regionName
	 */
	public String getRegionName()
	{
		return regionName;
	}
	/**
	 * @param regionName the regionName to set
	 */
	public void setRegionName(String regionName)
	{
		this.regionName = regionName;
	}


}

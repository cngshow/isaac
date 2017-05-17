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
package gov.vha.isaac.ochre.rest.api.data.vuid;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 
 * {@link RestVuidBlockData}
 * This class is returned by an allocate() request.  It contains start and end VUID numbers which define a continuous block.
 * 
 * The API returns this class.
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, defaultImpl=RestVuidBlockData.class)
public class RestVuidBlockData
{	
	/**
	 * VUID block boundary with absolute value <= end value
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public int start;

	/**
	 * VUID block boundary with absolute value >= start value
	 */
	@XmlElement
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public int end;

	protected RestVuidBlockData()
	{
		//for Jaxb
	}

	/**
	 * @param blockSize
	 */
	public RestVuidBlockData(
			int start,
			int end) {
		super();

		this.start = start;
		this.end = end;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RestVuidAllocationData [start=" + start + ", end=" + end + "]";
	}
}

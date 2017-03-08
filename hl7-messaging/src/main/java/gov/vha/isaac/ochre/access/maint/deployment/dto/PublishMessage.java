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

/**
 * An interface for passing over the necessary site / subset information for sending a request, with the messageID prepopulated
 * and setters for putting in the result(s).
 * 
 * {@link PublishMessage}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public interface PublishMessage
{

	public String getSubset();
	
	/**
	 * Returns the site to which the message (with message ID) was sent
	 * 
	 * @return Returns the site.
	 */
	public Site getSite();
	
	/**
	 * Returns the HL7 message ID
	 * 
	 * @return Returns the messageId.
	 */
	public long getMessageId();
	
	
	/**
	 * HL7 message as a string.
	 * @param rawMessage
	 */
	public void setRawHL7Message(String rawMessage);
	
	/**
	 * Returns the HL7 message as a string
	 * 
	 * @return Returns the HL7 message as a string.
	 */
	public String getRawHL7Message();
}
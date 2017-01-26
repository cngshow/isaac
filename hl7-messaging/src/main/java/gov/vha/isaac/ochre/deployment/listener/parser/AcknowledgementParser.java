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
package gov.vha.isaac.ochre.deployment.listener.parser;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.MFK_M01;
import ca.uhn.hl7v2.model.v26.segment.MSA;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.parser.PipeParser;
//import gov.va.med.term.deployment.business.ListenerDelegate;
import gov.vha.isaac.ochre.deployment.model.Site;

public class AcknowledgementParser extends BaseParser
{
	private static Logger log = LogManager.getLogger(AcknowledgementParser.class.getPackage().getName());
	
    /**
     * Iterate over the message and write to the log.  If a checksum is found,
     * it is written to the database.
     * @param content Incoming message as a String
     */
	public void processMessage(String content) throws Exception
	{
		Message message = null;
		PipeParser parser = new PipeParser();
		String msaMessage = null;
		
		String mshSendingFacility = null;
		String msaAcknowledgementCode = null;
		String mshMessageControlId = null;
		String msaMessageControlId = null;
		
		try
		{
			message = parser.parse(content);
			if(message instanceof MFK_M01)
			{
				MFK_M01 mfk = (MFK_M01)message;
				MSH msh = mfk.getMSH();
				MSA msa = mfk.getMSA();
				
				mshSendingFacility = msh.getSendingFacility().getNamespaceID().toString();
				msaAcknowledgementCode = msa.getAcknowledgmentCode().toString();
				mshMessageControlId = msh.getMessageControlID().toString();
				msaMessageControlId = msa.getMessageControlID().toString();
				
				long messageId = Long.parseLong(msaMessageControlId);
				
				//Get the MSA message segment
				msaMessage = msa.getTextMessage().toString();

				Site site = this.resolveSiteId(mshSendingFacility);
                log.info("STATUS: " + msaAcknowledgementCode
                         + "; SITE NAME: " + site.getName()
                         + "; SITE ID: " + site.getVaSiteId()
                         + "; ACK MSG. ID: " + mshMessageControlId
                         + "; ORIGINAL MSG. ID: " + msaMessageControlId);
                
                // update deployment history acknowledgment status
                //TODO: is this needed?
                //ListenerDelegate.updateDeploymentHistory(messageId, msaAcknowledgementCode);
			}
			else
			{
				log.error("Unknown message type.  Message header: " + msaMessage);
			}
		}
		catch(EncodingNotSupportedException e)
		{
			throw new Exception(e);
		}
		catch(HL7Exception e)
		{
			throw new Exception(e);
		}
	}
}

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
package gov.vha.isaac.ochre.deployment.listener;

import gov.vha.isaac.ochre.deployment.publish.HL7DateHelper;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.MFK_M01;
import ca.uhn.hl7v2.model.v26.message.MFN_M01;
import ca.uhn.hl7v2.model.v26.message.MFR_M01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.parser.PipeParser;

public class ListenerHelper
{
    /**
     * Returns a response message that is to be returned to the Interface Engine
     * indicating that the message was received from the Interface Engine.  This response
     * is sent before parsing of the message because the Commit Acknowledgement only indicates
     * the message was received by the Listener.
     * @param messageHeader
     * @return Commit Acknowledgement as a String
     */
    public static String getResponseMessage(String messageHeader) throws Exception
    {
    	String response = null;
    	
    	//MSH fields
		String mshSegmentType = "MSH";
		String fieldSeparator = "^";
		String encodingChars = "~|\\&";
		String subFieldSeparator = "~";
		String sendingAppNamespaceId = "";
		String sendingAppUniversalIdType = "";
		String sendingFacility = "";
		String receivingAppNamespaceId = "";
		String receivingAppUniversalIdType = "";
		String receivingFacility = "";
		String messageType = "";
		String processingId = "";
		String versionId = "2.4";
		//MSA fields
		String msaSegmentType = "MSA";
		String ackCode = "CA";
		String messageControlId = "";
    	
		PipeParser parser = new PipeParser();
		
    	try
    	{
    		Message message = parser.parse(messageHeader);
    		MFK_M01 mfk = null;
    		MFN_M01 mfn = null;
    		MFR_M01 mfr = null;
    		MSH msh = null;

    		if(message instanceof MFK_M01)
    		{
    			mfk = (MFK_M01)message;
    			msh = mfk.getMSH();
    		}
    		else if(message instanceof MFN_M01)
    		{
    			mfn = (MFN_M01)message;
    			msh = mfn.getMSH();
    		}
    		else if(message instanceof MFR_M01)
    		{
    			mfr = (MFR_M01)message;
    			msh = mfr.getMSH();
    		}
    		else
    		{
    			System.out.println("MESSAGE TYPE UNRECOGNIZED");
    		}
    		
    		sendingAppNamespaceId = msh.getSendingApplication().getNamespaceID().toString();
    		
    		//If the result of getting the sending app universal ID type is null,
    		//leave sendingAppUniversalIdType field as "".
    		String testSendingApplicationUniversalIDType = msh.getSendingApplication().getUniversalIDType().toString();
    		if(testSendingApplicationUniversalIDType != null)
    		{
    			sendingAppUniversalIdType = testSendingApplicationUniversalIDType;
    		}
    		
    		sendingFacility =  msh.getSendingFacility().getNamespaceID().toString();
    		
    		receivingAppNamespaceId = msh.getReceivingApplication().getNamespaceID().toString();
    		
    		//If the result of getting the receiving app universal ID type is null,
    		//leave receivingAppUniversalIdType field as "".
    		String testReceivingAppUniversalIdType = msh.getReceivingApplication().getUniversalIDType().toString();
    		if(testReceivingAppUniversalIdType != null)
    		{
    			receivingAppUniversalIdType = testReceivingAppUniversalIdType;
    		}
    		
    		receivingFacility = msh.getReceivingFacility().getNamespaceID().toString();
    		//messageType = msh.getMessageType().getMessageType().toString();
    		messageType = msh.getMessageType().getMessage().toString();
    		messageControlId = msh.getMessageControlID().toString();
    		processingId = msh.getProcessingID().getProcessingID().toString();
    		
    	
    		String localRecAppSubfieldSeparator = "";
    		if((receivingAppUniversalIdType != null) && (receivingAppUniversalIdType.length() > 0))
    		{
    			localRecAppSubfieldSeparator = subFieldSeparator + subFieldSeparator;
    		}
        
    		String localSendAppSubfieldSeparator = "";
    		if((sendingAppUniversalIdType != null) && (sendingAppUniversalIdType.length() > 0))
    		{
    			localSendAppSubfieldSeparator = subFieldSeparator + subFieldSeparator;
    		}
        
    		String hl7DateString = HL7DateHelper.getHL7DateFormat(HL7DateHelper.getCurrentDate());
        
    		String hl7MessageID = 	HL7DateHelper.getCurrentYear()
        				+ HL7DateHelper.getCurrentMonth()
        				+ HL7DateHelper.getCurrentDay()
        				+ HL7DateHelper.getCurrentHour()
        				+ HL7DateHelper.getCurrentMinute()
        				+ HL7DateHelper.getCurrentSecond();
        
    		response =	(char)11 + mshSegmentType + fieldSeparator
        				+ encodingChars + fieldSeparator
        				+ receivingAppNamespaceId + localRecAppSubfieldSeparator + receivingAppUniversalIdType + fieldSeparator
        				+ receivingFacility + fieldSeparator
        				+ sendingAppNamespaceId + localSendAppSubfieldSeparator + sendingAppUniversalIdType + fieldSeparator
        				+ sendingFacility + fieldSeparator
        				+ hl7DateString + fieldSeparator
        				+ fieldSeparator
        				+ messageType + fieldSeparator
        				+ hl7MessageID + fieldSeparator
        				+ processingId + fieldSeparator
        				+ versionId + ""
        				+ (char)13
        				+ msaSegmentType + fieldSeparator
        				+ ackCode + fieldSeparator
        				+ messageControlId + fieldSeparator
        				+ (char)13 + (char)28 + (char)13;
    	}
        catch (EncodingNotSupportedException e)
        {
            throw new Exception(e);
        }
        catch (HL7Exception e)
        {
            throw new Exception(e);
        }
        
        return response;
    }
    
    /**
     * Return the next line in the string 
     * if it is the end then return null
     * @param content
     */
    public static String getNextLine(String content, int index)
    {
        String line = null;
        if (index < content.length())
        {
            // find the position of the CR
            int pos = content.indexOf((char)13, index);
            // get the string to that position
            if(pos >= 0)
            {
            	line = content.substring(index, pos);
            }
            // set the index past the CR
            index=pos+1;
            // skip past any linefeeds
            while (content.charAt(index)=='\n')
            {
               // don't index beyond the end of the string
               if (++index >= content.length())
               {
                   break;
               }
            }
        }
        return line;
    }
}
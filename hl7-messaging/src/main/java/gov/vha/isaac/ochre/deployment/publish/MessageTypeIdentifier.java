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
package gov.vha.isaac.ochre.deployment.publish;

import java.util.ArrayList;
import java.util.List;

import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;
import gov.vha.isaac.ochre.services.exception.STSException;

public class MessageTypeIdentifier
{

	public static final String MFN_TYPE = "MFN";
	public static final String MFK_TYPE = "MFK";
	public static final String MFR_TYPE = "MFR";
	public static final String MFQ_TYPE = "MFQ";

	public static HL7ApplicationProperties applicationProperties = null;

	/**
	 * Retrieves the message header by searching for "MSH".
	 * 
	 * @param content
	 *            Incoming message as a String
	 * @return Returns only the MSH segment
	 */
	public static String getMessageHeader(String content) throws STSException {
		String messageHeader = null;
		String line = null;
		int index = 0;

		while ((line = HL7PublishHelper.getNextLine(content, index)) != null) {
			if (line.startsWith("MSH")) {
				messageHeader = line;
				break;
			}
			index++;
		}

		if (messageHeader == null) {
			throw new STSException("Unable to find MSH segment in the message.");
		}

		return messageHeader;
	}

	/**
	 * Examines the message header to determine what type of message it is.
	 * Returned message types can be one of "MFK", "MFR" or "MFN"
	 * 
	 * @param messageHeader
	 * @return messate type as a string
	 */
	public static String getMessageType(String messageHeader) throws STSException {
		String messageType = null;
		String[] params = messageHeader.split("\\^");
		String fullMessageType = params[8];
		String[] subParams = fullMessageType.split("~");

		if (subParams[0].equals(MFK_TYPE)) {
			messageType = MFK_TYPE;
		}
		if (subParams[0].equals(MFN_TYPE)) {
			messageType = MFN_TYPE;
		}
		if (subParams[0].equals(MFR_TYPE)) {
			messageType = MFR_TYPE;
		}
		if (subParams[0].equals(MFQ_TYPE)) {
			messageType = MFQ_TYPE;
		}
		try {
			if (messageType == null) {
				throw new Exception("Message type unknown: not of type " + MFN_TYPE + ", " + MFK_TYPE + ", " + MFR_TYPE
						+ " or " + MFQ_TYPE + ".");
			}
		} catch (Exception e) {
			throw new STSException(e);
		}

		return messageType;
	}

	/**
	 * This method answers the question, "what application flag sent this
	 * incoming message?"
	 * 
	 * @param messageHeader
	 * @return
	 * @throws ETSBusinessException
	 */
	public static String getIncomingMessageSendingApp(String messageHeader) throws STSException {
		String sendingApplication = null;

		// Make a list of all the sending application names.
		// Because the message is incoming to deployment server, the
		// sending app name is the flag that was the target for the
		// original outgoing message to which this incoming message
		// is a response, therefore sending app name in this incoming
		// message is equivalent to our receiving app name in the
		// application.properties file.
		List<String> sendingApps = new ArrayList<>();
		sendingApps.add(applicationProperties.getReceivingApplicationNamespaceIdSiteData());
		sendingApps.add(applicationProperties.getReceivingApplicationNamespaceIdMD5());
		sendingApps.add(applicationProperties.getReceivingApplicationNamespaceIdUpdate());

		String[] params = messageHeader.split("\\^");
		sendingApplication = params[2];
		if (!sendingApps.contains(sendingApplication)) {
			throw new STSException("Sending application in incoming message is not recognized: " + sendingApplication);
		}

		return sendingApplication;
	}

	/**
	 * This method answers the question, "what application flag is the target
	 * for this incoming message?"
	 * 
	 * @param messageHeader
	 * @return
	 */
	public static String getIncomingMessageReceivingApp(String messageHeader) throws STSException {
		String receivingApplication = null;

		// Make a list of all the receiving application names.
		// Because the message is incoming to deployment server, the
		// receiving app name is the flag that originated the outgoing
		// message to which this incoming message is a response, therefore
		// receiving app name in this incoming message is equivalent to
		// our sending app name in the application.properties file.
		List<String> receivingApps = new ArrayList<>();
		receivingApps.add(applicationProperties.getSendingApplicationNamespaceIdSiteData());
		receivingApps.add(applicationProperties.getSendingApplicationNamespaceIdMD5());
		receivingApps.add(applicationProperties.getSendingApplicationNamespaceIdUpdate());

		String[] params = messageHeader.split("\\^");
		receivingApplication = params[4];
		if (!receivingApps.contains(receivingApplication)) {
			throw new STSException(
					"Receiving application in incoming message is not recognized: " + receivingApplication);
		}

		return receivingApplication;
	}
}

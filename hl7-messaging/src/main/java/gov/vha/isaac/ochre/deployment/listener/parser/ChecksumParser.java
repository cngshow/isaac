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

import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.MFR_M01;
import ca.uhn.hl7v2.model.v24.segment.MSA;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.parser.PipeParser;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDTO;

public class ChecksumParser extends BaseParser
{
	private static Logger log = LogManager.getLogger(ChecksumParser.class.getPackage().getName());

	private static String MSA_MESSAGE_CONTROL_ID = "";

	/**
	 * Iterate over the message and write to the log. If a checksum is found, it
	 * is written to the database.
	 * 
	 * @param content
	 *            Incoming message as a String
	 */
	public void processMessage(String content) throws Exception {
		Message message = null;
		PipeParser parser = new PipeParser();
		String msaMessage = null;

		String mshSendingFacility = null;
		String msaAcknowledgementCode = null;
		String mshMessageControlId = null;
		String msaMessageControlId = null;

		try {
			message = parser.parse(content);
			if (message instanceof MFR_M01) {
				MFR_M01 mfk = (MFR_M01) message;
				MSH msh = mfk.getMSH();
				MSA msa = mfk.getMSA();

				mshSendingFacility = msh.getSendingFacility().getNamespaceID().toString();
				msaAcknowledgementCode = msa.getAcknowledgementCode().toString();
				mshMessageControlId = msh.getMessageControlID().toString();
				msaMessageControlId = msa.getMessageControlID().toString();

				// Get the MSA message segment
				msaMessage = msa.getTextMessage().toString();
				if (msaMessage != null && msaMessage.length() > 0) {
					log.debug("Message in MSA segment: " + msaMessage);
					if (isChecksumMessage(msaMessage)) {
						MSA_MESSAGE_CONTROL_ID = mfk.getMSA().getMessageControlID().toString();
						processChecksumResponse(MSA_MESSAGE_CONTROL_ID, msaMessage);
					}
				}
				SiteDTO site = this.resolveSiteId(mshSendingFacility);
				log.info("STATUS: " + msaAcknowledgementCode + "; SITE NAME: " + site.getName() + "; SITE ID: "
						+ site.getVaSiteId() + "; ACK MSG. ID: " + mshMessageControlId + "; ORIGINAL MSG. ID: "
						+ msaMessageControlId);
			} else {
				log.error("Unknown message type.  Message header: " + msaMessage);
			}
		} catch (EncodingNotSupportedException e) {
			throw new Exception(e);
		} catch (HL7Exception e) {
			throw new Exception(e);
		}
	}

	/**
	 * The msaMessage will be vista embedded string
	 * ;CHECKSUM:f8788e471664f6b285b4472660e2e3c9;VERSION:13; The vista embedded
	 * string may or may not be present in the message.
	 * 
	 * @param msaMessage
	 */
	private void processChecksumResponse(String messageId, String msaMessage) {
		StringTokenizer tokenizer = new StringTokenizer(msaMessage, ";");
		String version = null;
		String checksum = null;
		String token = null;
		int colonIndex = -1;

		while (tokenizer.hasMoreTokens()) {
			token = tokenizer.nextToken();
			if (token.startsWith("CHECKSUM")) {
				colonIndex = token.indexOf(':');
				if (colonIndex != -1 && (token.length() > (colonIndex + 1))) {
					checksum = token.substring(colonIndex + 1);
				}
			} else if (token.startsWith("VERSION")) {
				colonIndex = token.indexOf(':');
				if (colonIndex != -1 && (token.length() > (colonIndex + 1))) {
					version = token.substring(colonIndex + 1);
				}
			}
		}

		// Now check if the values are parsed
		if (checksum != null /* && version != null */) {
			log.debug("The checksum is: " + checksum);
			if (version == null) {
				log.warn("The version is null.");
			} else {
				log.debug("The version is: " + version);
			}

			// Convert messageId to a long
			long longMessageId = Long.parseLong(messageId);

			// TODO: is this needed?
			// ListenerDelegate.updateChecksumRequest(longMessageId, checksum);
		}
	}

	/*
	 * Search for the CHECKSUM substring to determine if the message is a
	 * checksum response
	 */
	private boolean isChecksumMessage(String msaMessage) {
		if (msaMessage != null && msaMessage.length() > 0) {
			if (msaMessage.indexOf("CHECKSUM") != -1 && msaMessage.indexOf("VERSION") != -1) {
				log.debug("Checksum found in the MSA segment.");
				return true;
			}
		}
		return false;
	}
}

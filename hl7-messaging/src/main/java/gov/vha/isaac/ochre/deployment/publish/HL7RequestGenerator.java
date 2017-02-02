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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.MFQ_M01;
import ca.uhn.hl7v2.parser.EncodingNotSupportedException;
import ca.uhn.hl7v2.parser.PipeParser;
import gov.vha.isaac.ochre.deployment.hapi.extension.VetsMfqM01;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;
import gov.vha.isaac.ochre.services.exception.STSException;

public class HL7RequestGenerator extends HL7BaseGenerator
{
	private static Logger log = LogManager.getLogger(HL7RequestGenerator.class);

	/**
	 * The <code>getSiteDataRequestMessage</code> method returns a String
	 * representation of a MFQ_M01 message used to request Site Data from MFS.
	 * 
	 * @param regionName
	 * @return String representation of site data request message
	 * @throws STSException
	 */
	public static String getSiteDataRequestMessage(String regionName, HL7ApplicationProperties applicationProperties)
			throws STSException {

		// String sendingApplicationName =
		// ApplicationPropertyReader.getApplicationProperty("msh.sendingApplication.namespaceId.siteData");
		String sendingApplicationName = applicationProperties.getSendingApplicationNamespaceIdSiteData();

		// String receivingApplicationName =
		// ApplicationPropertyReader.getApplicationProperty("msh.receivingApplication.namespaceId.siteData");
		String receivingApplicationName = applicationProperties.getReceivingApplicationNamespaceIdSiteData();

		MFQ_M01 message = null;
		if (regionName != null) {
			message = getHL7RequestMessage(sendingApplicationName, receivingApplicationName, null, regionName,
					applicationProperties);
		} else {
			throw new STSException("Region name is null");
		}

		return getRequestMessage(message);
	}

	/**
	 * The <code>getMappingSiteDataRequestMessage</code> method returns a String
	 * representation of a MFQ_M01 message used to request Mapping Site Data
	 * from MFS
	 * 
	 * @param mapSetVuid
	 * @return String representation of mapping site data request message
	 * @throws STSException
	 */
	public static String getMappingSiteDataRequestMessage(Long mapSetVuid,
			HL7ApplicationProperties applicationProperties) throws STSException {

		// String sendingApplicationName =
		// ApplicationPropertyReader.getApplicationProperty("msh.sendingApplication.namespaceId.siteData");
		String sendingApplicationName = applicationProperties.getSendingApplicationNamespaceIdSiteData();

		// String receivingApplicationName =
		// ApplicationPropertyReader.getApplicationProperty("msh.receivingApplication.namespaceId.siteData");
		String receivingApplicationName = applicationProperties.getReceivingApplicationNamespaceIdSiteData();

		MFQ_M01 message = getHL7RequestMessage(sendingApplicationName, receivingApplicationName, mapSetVuid, null,
				applicationProperties);

		return getRequestMessage(message);
	}

	/**
	 * The <code>getChecksumRequestMessage</code> method returns a String
	 * representation of a MFQ_M01 message used to request a Checksum from MFS.
	 * 
	 * @param regionName
	 * @return String representation of checksum request message
	 * @throws STSException
	 */
	public static String getChecksumRequestMessage(String regionName, HL7ApplicationProperties applicationProperties)
			throws STSException {

		String sendingApplicationName = applicationProperties.getSendingApplicationNamespaceIdMD5();
		String receivingApplicationName = applicationProperties.getReceivingApplicationNamespaceIdMD5();

		MFQ_M01 message = null;
		if (regionName != null) {
			message = getHL7RequestMessage(sendingApplicationName, receivingApplicationName, null, regionName,
					applicationProperties);
		} else {
			throw new STSException("Region name is null");
		}

		return getRequestMessage(message);
	}

	/**
	 * The <code>getMappingChecksumRequestMessage</code> method returns a String
	 * representation of a MFQ_M01 message used to request a Mapping Checksum
	 * from MFS.
	 * 
	 * @param mapSetVuid
	 * @return String representation of mapping checksum request message
	 * @throws STSException
	 */
	public static String getMappingChecksumRequestMessage(Long mapSetVuid,
			HL7ApplicationProperties applicationProperties) throws STSException {

		String sendingApplicationName = applicationProperties.getSendingApplicationNamespaceIdMD5();
		String receivingApplicationName = applicationProperties.getReceivingApplicationNamespaceIdMD5();

		MFQ_M01 message = getHL7RequestMessage(sendingApplicationName, receivingApplicationName, mapSetVuid, null,
				applicationProperties);

		return getRequestMessage(message);
	}

	/*
	 * The <code>getDataRequestMessage</code> method generates an MFQ_M01
	 * message that requests a Site Data return message.
	 *
	 * @param regionName
	 * 
	 * @return MFQ_M01 message object
	 * 
	 * @throws VETSPublisherException
	 */
	private static MFQ_M01 getHL7RequestMessage(String sendingApplicationName, String receivingApplicationName,
			Long mapSetVuid, String regionName, HL7ApplicationProperties applicationProperties) throws STSException {
		VetsMfqM01 dataRequestMessage = new VetsMfqM01();

		String hl7DateString = HL7DateHelper.getHL7DateFormat(HL7DateHelper.getCurrentDateTime());

		try {
			dataRequestMessage.addMshSegment(dataRequestMessage, sendingApplicationName, receivingApplicationName,
					applicationProperties);

			if (regionName != null) {
				dataRequestMessage.addQrdSegment(dataRequestMessage, hl7DateString, regionName, applicationProperties);
			} else {
				dataRequestMessage.addFilteredQrdSegment(dataRequestMessage, hl7DateString, MAPPINGS_IDENTIFIER,
						mapSetVuid.toString(), applicationProperties);
			}
		} catch (DataTypeException e) {
			String errorMessage = "Exception generating the MFQ_M01 message object.";
			log.error(errorMessage, e);
			throw new STSException(errorMessage, e);
		} catch (HL7Exception e) {
			String errorMessage = "Exception generating the MFQ_M01 message object.";
			log.error(errorMessage, e);
			throw new STSException(errorMessage, e);
		}

		return dataRequestMessage;
	}

	/**
	 * The <code>getMessage</code> method converts from a MFQ_M01 message object
	 * generated by the HAPI library to a String representation of the message
	 * that may be printed to the console, etc.
	 *
	 * @param message
	 * @return String representation of message object
	 * @throws EncodingNotSupportedException
	 * @throws HL7Exception
	 */
	public static String getRequestMessage(MFQ_M01 message) throws STSException {
		String messageString = null;

		try {
			PipeParser parser = new PipeParser();
			messageString = parser.encode(message);
		} catch (EncodingNotSupportedException e) {
			String errorMessage = "Exception converting the MFQ_M01 message object to a string.";
			log.error(errorMessage, e);
			throw new STSException(errorMessage, e);
		} catch (HL7Exception e) {
			String errorMessage = "Exception converting the MFQ_M01 message object to a string.";
			log.error(errorMessage, e);
			throw new STSException(errorMessage, e);
		}

		return messageString;
	}

	/**
	 * The <code>getMessage</code> method converts from an HL7 message string to
	 * a MFN_M01 message object.
	 *
	 * @param messageString
	 * @return MFN_M01 message object
	 * @throws HL7Exception
	 */
	public static MFQ_M01 getRequestMessage(String messageString) throws STSException {
		MFQ_M01 messageObject = new MFQ_M01();

		PipeParser parser = new PipeParser();
		try {
			Message message = parser.parse(messageString);

			messageObject = (MFQ_M01) message;

			return messageObject;
		} catch (HL7Exception e) {
			String errorMessage = "Error generating MFQ_M01 request message object from message string.";
			log.error(errorMessage, e);
			throw new STSException(errorMessage, e);
		}
	}

	// public static void main(String[] args) throws STSException {
	// String filteredRequestMessage =
	// HL7RequestGenerator.getSiteDataRequestMessage("SCT2ICD9");
	// String unfilteredRequestMessage =
	// HL7RequestGenerator.getSiteDataRequestMessage("Reactants");
	// System.out.println("Filtered Site Data example: ");
	// System.out.println(filteredRequestMessage);
	// System.out.println("Unfiltered Site Data example: ");
	// System.out.println(unfilteredRequestMessage);
	//
	// String filteredMD5RequestMessage =
	// HL7RequestGenerator.getChecksumRequestMessage("SCT2ICD9");
	// String unfilteredMD5RequestMessage =
	// HL7RequestGenerator.getChecksumRequestMessage("Reactants");
	// System.out.println("Filtered Checksum example: ");
	// System.out.println(filteredMD5RequestMessage);
	// System.out.println("Unfiltered Checksum example: ");
	// System.out.println(unfilteredMD5RequestMessage);
	// }
}

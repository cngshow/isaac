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
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v24.message.MFN_M01;
import ca.uhn.hl7v2.model.v24.message.MFQ_M01;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage;
import gov.vha.isaac.ochre.access.maint.deployment.dto.Site;
import gov.vha.isaac.ochre.access.maint.messaging.hl7.MessageDispatcher;
import gov.vha.isaac.ochre.access.maint.messaging.hl7.factory.BusinessWareMessageDispatcher;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.deployment.listener.HL7ResponseListener;
import gov.vha.isaac.ochre.deployment.listener.HL7ResponseReceiveListener;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.MessageProperties;
import gov.vha.isaac.ochre.services.exception.STSException;

/**
 * The <code>HL7Sender</code> class manages the conversion from the deployment
 * server database to HL7 and transmits the HL7 message.
 * <p>
 * The <code>HL7Sender</code> class is the main interface for the entire process
 * of creating HL7 messages to be sent to the Vitria Interface Engine, and then
 * on to the Master File Server (MFS) in the VistA environment.
 * <p>
 *
 * @author vhaislempeyd
 */

public class HL7Sender
{
	private static Logger LOG = LogManager.getLogger(HL7Sender.class);

	private static MessageDispatcher dispatcher = new BusinessWareMessageDispatcher();

	private static final String UPDATE_MESSAGE_TYPE = "UPDATE";
	private static final String QUERY_MESSAGE_TYPE = "QUERY";

	private static boolean useInterfaceEngine;

	private String hl7Message_;
	private PublishMessage publishMessage_;
	private ApplicationProperties applicationProperties_;
	private MessageProperties messageProperties_;

	/**
	 * Send the HL7 Message to the sites in the site list. This method detects
	 * the type of message and calls the correct private method to actually send
	 * the message to the sites.
	 * 
	 * @param hl7UpdateMessage
	 * @param siteList
	 * @return List of message IDs
	 * @throws STSException
	 */

	public HL7Sender(String hl7Message, PublishMessage publishMessage, ApplicationProperties applicationProperties,
			MessageProperties messageProperties) {

		hl7Message_ = hl7Message;
		publishMessage_ = publishMessage;
		applicationProperties_ = applicationProperties;
		messageProperties_ = messageProperties;
	}

	/**
	 * returns true, if a message was sent that we expect a response from.  False, if we should not expect a response, 
	 * due to, for example, the userInterfaceEngine flag being false, or some error happening during the send that prevented the send.
	 */
	public boolean send(HL7ResponseReceiveListener notifyOnResponseReceived) throws STSException {
		useInterfaceEngine = getInterfaceEngineUsage(Boolean.toString(applicationProperties_.getUseInterfaceEngine()));
		
		if (useInterfaceEngine) {
			String messageType = MessageTypeIdentifier
					.getMessageType(MessageTypeIdentifier.getMessageHeader(hl7Message_));
			if (MessageTypeIdentifier.MFN_TYPE.equals(messageType)) {
				// MFN M01: Master file not otherwise specified
				MFN_M01 message = HL7SubsetUpdateGenerator.getMessage(hl7Message_);
				sendHL7UpdateMessage(message, publishMessage_, applicationProperties_, notifyOnResponseReceived);
			} else if (MessageTypeIdentifier.MFQ_TYPE.equals(messageType)) {
				// MFQ M01: Query for master file record
				MFQ_M01 message = HL7RequestGenerator.getRequestMessage(hl7Message_);
				sendHL7RequestMessage(message, publishMessage_, applicationProperties_, messageProperties_, notifyOnResponseReceived);
			} else {
				LOG.error("Unknown message type.  Message header: {} ",
						MessageTypeIdentifier.getMessageHeader(hl7Message_));
				throw new STSException("Unkown message type. " + MessageTypeIdentifier.getMessageHeader(hl7Message_));
			} 
			
			//TODO, perhaps, in the future, we may have cases where false is the appropriate value to return for certain messages....
			return true;
		} else {
			LOG.info("No Emulator, please set useInterfaceEngine to true.");
			return false;
		}
	}

	/*
	 * Send the HL7 Update Message to the specified topics. A new message id is
	 * generated for each message.
	 *
	 * @param message message object of type MFN_M01 that needs to be sent to
	 * the sites
	 * 
	 * @param site list of receiving sites
	 *
	 * @return message Id of the messages that have been sent to the sites.
	 *
	 * @throws STSException
	 */
	private synchronized static void sendHL7UpdateMessage(MFN_M01 message, PublishMessage publishMessage,
			ApplicationProperties applicationProperties, HL7ResponseReceiveListener notifyOnResponseReceived) throws STSException {
		try {
			// insert the topic and message id
			MSH msh = message.getMSH();
			Site site = publishMessage.getSite();

			String sendingFacility = applicationProperties.getSendingFacilityNamespaceId();
			msh.getSendingFacility().getNamespaceID().setValue(sendingFacility);
			msh.getReceivingFacility().getNamespaceID().setValue(site.getVaSiteId());
			msh.getMessageControlID().setValue(((Long) publishMessage.getMessageId()).toString());

			// insert the message type for the topic
			msh.getProcessingID().getProcessingID().setValue(site.getMessageType());
			LOG.info("Use interface engine is set to " + useInterfaceEngine + " and applicationProperties.getUseInterfaceEngine() is " + applicationProperties.getUseInterfaceEngine());
			// Send the HL7 message
			if (applicationProperties.getUseInterfaceEngine()) {
				LookupService.get().getService(HL7ResponseListener.class).registerListener(publishMessage.getMessageId(), notifyOnResponseReceived);
				dispatcher.send(message, applicationProperties);
			} else {
				// TODO: find code to re-implement if necessary. Leaving
				// this out for now. and logging error.
				/*
				 * String url =
				 * ApplicationPropertyReader.getApplicationProperty(
				 * "emulator.url"); if (url != null) {
				 * EmulatorDelegate.sendMessage(HL7SubsetUpdateGenerator.
				 * getMessage(message), url); }
				 */
				LOG.info("No Emulator, please set useInterfaceEngine to true.");
			}

			getDeploymentStatusMessage(HL7SubsetUpdateGenerator.getMessage(message), useInterfaceEngine, site,
					msh.getMessageControlID().toString(), msh.getProcessingID().getProcessingID().getValue(),
					UPDATE_MESSAGE_TYPE);

		} catch (DataTypeException e) {
			LOG.error("Exception when setting topic in message.", e);
			throw new STSException("Exception when setting topic in message.", e);
		} catch (RuntimeException e) {
			String errorMessage = "Exception when attempting to send the message.  Interface Engine may not be responding.";
			LOG.error(errorMessage);
			throw new STSException(errorMessage, e);
		} catch (Exception e) {
			LOG.error("Exception in finding message type", e);
			throw new STSException("Exception in finding message type", e);
		}
	}

	/*
	 * Send the HL7 Request Message to the specified topics. A new message id is
	 * generated for each message.
	 *
	 * @param message message object of type MFQ_M01 that needs to be sent to
	 * the sites
	 * 
	 * @param site list of receiving sites
	 *
	 * @return message Id of the messages that have been sent to the sites.
	 *
	 * @throws VETSPublisherException
	 * 
	 * @throws HL7Exception
	 */
	private synchronized static void sendHL7RequestMessage(MFQ_M01 message, PublishMessage publishMessage,
			ApplicationProperties applicationProperties, MessageProperties messageProperties, HL7ResponseReceiveListener notifyOnResponseReceived)
			throws STSException, STSException {
		try {

			// insert the topic and message id
			MSH msh = message.getMSH();
			Site site = publishMessage.getSite();
			String sendingFacility = applicationProperties.getSendingFacilityNamespaceId();
			LOG.info("sendingFacility: {}", sendingFacility);

			msh.getSendingFacility().getNamespaceID().setValue(sendingFacility);
			LOG.info("getSendingFacility().getNamespaceID().setValue: {}", sendingFacility);

			msh.getReceivingFacility().getNamespaceID().setValue(site.getVaSiteId());
			LOG.info("getReceivingFacility().getNamespaceID().setValue: {}", site.getVaSiteId());

			msh.getMessageControlID().setValue(((Long) publishMessage.getMessageId()).toString());
			LOG.info("getMessageControlID().setValue: {}", publishMessage.getMessageId());

			// insert the message type for the topic
			msh.getProcessingID().getProcessingID().setValue(site.getMessageType());
			LOG.info("getProcessingID().getProcessingID().setValue: {}", site.getMessageType());

			LOG.info("Message before calling dispatcher");
			LOG.info(message.toString());

			// Send the HL7 message
			if (useInterfaceEngine) {
				LOG.info("calling dispatcher to send message");
				LookupService.get().getService(HL7ResponseListener.class).registerListener(publishMessage.getMessageId(), notifyOnResponseReceived);
				dispatcher.send(message, applicationProperties);
			} else {
				// TODO: find code to re-implement if necessary. Leaving
				// this out for now and logging error.
				/*
				 * String url =
				 * ApplicationPropertyReader.getApplicationProperty(
				 * "emulator.url"); if (url != null) {
				 * EmulatorDelegate.sendMessage(HL7SubsetUpdateGenerator.
				 * getMessage(message), url); }
				 */
				LOG.error("No Emulator, please set useInterfaceEngine to true.");
			}

			getDeploymentStatusMessage(HL7RequestGenerator.getRequestMessage(message), useInterfaceEngine, site,
					msh.getMessageControlID().toString(), msh.getProcessingID().getProcessingID().getValue(),
					QUERY_MESSAGE_TYPE);

		} catch (DataTypeException e) {
			LOG.error("Exception in setting topic in message.", e);
			throw new STSException("Exception generating message.", e);
		} catch (RuntimeException e) {
			String errorMessage = "Exception when attempting to send the message.  Interface Engine may not be responding.";
			LOG.error(errorMessage);
			throw new STSException(errorMessage, e);
		} catch (Exception e) {
			LOG.error("Exception in finding message type", e);
			throw new STSException("Exception in finding message type", e);
		}
	}

	private static void getDeploymentStatusMessage(String message, boolean useInterfaceEngine, Site site,
			String messageId, String messageProcessingType, String messageType) {

		StringBuilder deploymentStatus = new StringBuilder();
		deploymentStatus.append("Site: ").append(site.getName()).append("; Message ID: ").append(messageId);
		deploymentStatus.append("; Message Type: ").append(messageProcessingType);
		deploymentStatus.append("; Parameter useInterfaceEngine is set to '");
		deploymentStatus.append(useInterfaceEngine).append("' so a message ");
		deploymentStatus.append(useInterfaceEngine == true ? "was " : "WAS NOT ");
		deploymentStatus.append("deployed to ").append(site.getName()).append(".");
		LOG.info(deploymentStatus.toString());

		int mshBeginIndex = message.indexOf("MSH");

		int mshEndIndex = 0;
		if (messageType.equals(UPDATE_MESSAGE_TYPE)) {
			mshEndIndex = message.indexOf("MFI");
		} else {
			mshEndIndex = message.indexOf("QRD");
		}

		String messageHeader = message.substring(mshBeginIndex, mshEndIndex);

		LOG.info("Message header: {}", messageHeader);

		LOG.info("Full message: \n{}", message);
	}

	/*
	 * Determine whether we will be using the Interface Engine when the message
	 * is sent.
	 * 
	 * @return boolean
	 * 
	 * @throws STSException
	 */
	private static boolean getInterfaceEngineUsage(String useIE) throws STSException {

		// default to not use Interface Engine
		boolean ieUsage = false;
		if ("true".equalsIgnoreCase(useIE) || "false".equalsIgnoreCase(useIE)) {
			useIE.toLowerCase();
			ieUsage = Boolean.valueOf(useIE).booleanValue();
		} else {
			throw new STSException("useInterfaceEngine parameter must be 'true' or 'false'.");
		}
		return ieUsage;
	}
}
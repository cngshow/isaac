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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.message.MFN_M01;
import ca.uhn.hl7v2.model.v26.message.MFQ_M01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.access.maint.messaging.hl7.MessageDispatcher;
import gov.vha.isaac.ochre.access.maint.messaging.hl7.factory.BusinessWareMessageDispatcher;
import gov.vha.isaac.ochre.deployment.model.Site;
import gov.vha.isaac.ochre.services.exception.STSException;
import javafx.concurrent.Task;

/**
 * The <code>HL7Sender</code> class manages the conversion from the deployment
 * server database to HL7 and transmits the HL7 message.
 * <p>
 * The <code>HL7Sender</code> class is the main interface for the entire
 * process of creating HL7 messages to be sent to the Vitria Interface Engine,
 * and then on to the Master File Server (MFS) in the VistA environment.
 * <p>
 *
 * @author vhaislempeyd
 */

public class HL7Sender extends Task<Integer>
{
	private static Logger log = LogManager.getLogger(HL7Sender.class.getPackage().getName());

	private static MessageDispatcher dispatcher = new BusinessWareMessageDispatcher();

	private static final String UPDATE_MESSAGE_TYPE = "UPDATE";
	private static final String QUERY_MESSAGE_TYPE = "QUERY";

	private static boolean useInterfaceEngine;

	/**
	 * Send the HL7 Message to the sites in the site list.  This method detects
	 * the type of message and calls the correct private method to actually send
	 * the message to the sites.
	 * @param hl7UpdateMessage
	 * @param siteList
	 * @return List of message IDs
	 * @throws STSException
	 */
	public static void send(String hl7UpdateMessage, List<PublishMessageDTO> siteList)
			throws STSException
	{
		useInterfaceEngine = getInterfaceEngineUsage();

		String messageType = MessageTypeIdentifier.getMessageType(
				MessageTypeIdentifier.getMessageHeader(hl7UpdateMessage));

		if(MessageTypeIdentifier.MFN_TYPE.equals(messageType))
		{
			//MFN M01: Master file not otherwise specified
			MFN_M01 message = HL7SubsetUpdateGenerator.getMessage(hl7UpdateMessage);
			sendHL7UpdateMessage(message, siteList);
		}
		else if(MessageTypeIdentifier.MFQ_TYPE.equals(messageType))
		{
			//MFQ M01: Query for master file record
			MFQ_M01 message = HL7RequestGenerator.getRequestMessage(hl7UpdateMessage);
			sendHL7RequestMessage(message, siteList);
		}
		else
		{
			log.error("Unknown message type.  Message header: {} ", MessageTypeIdentifier.getMessageHeader(hl7UpdateMessage));
			throw new STSException("Unkown message type."
					+ MessageTypeIdentifier.getMessageHeader(hl7UpdateMessage));
		}
	}

	/*
	 * Send the HL7 Update Message to the specified topics. A new message id is
	 * generated for each message.
	 *
	 * @param message
	 *            message object of type MFN_M01 that needs to be sent to the sites
	 * @param site
	 *            list of receiving sites
	 *
	 * @return message Id of the messages that have been sent to the sites.
	 *
	 * @throws STSException
	 */
	private synchronized static void sendHL7UpdateMessage(MFN_M01 message, List<PublishMessageDTO> siteList)
			throws STSException
	{
		try
		{
			for(PublishMessageDTO publishMessageDTO : siteList)
			{
				// insert the topic and message id
				MSH msh = message.getMSH();
				String sendingFacility = ApplicationPropertyReader.getApplicationProperty("msh.sendingFacility.namespaceId");
				msh.getSendingFacility().getNamespaceID().setValue(sendingFacility);
				msh.getReceivingFacility().getNamespaceID().setValue(publishMessageDTO.getSite().getVaSiteId());
				msh.getMessageControlID().setValue(((Long)publishMessageDTO.getMessageId()).toString());

				//insert the message type for the topic
				msh.getProcessingID().getProcessingID().setValue(publishMessageDTO.getSite().getMessageType());

				// Send the HL7 message
				if (useInterfaceEngine)
				{
					dispatcher.send(message);
				}
				else
				{
					//TODO: find code to re-implement if necessary. Leaving this out for now.
					/*
					String url = ApplicationPropertyReader.getApplicationProperty("emulator.url");
					if (url != null)
					{
						EmulatorDelegate.sendMessage(HL7SubsetUpdateGenerator.getMessage(message), url);
					}
					 */
				}

				getDeploymentStatusMessage(HL7SubsetUpdateGenerator.getMessage(message), useInterfaceEngine,
						publishMessageDTO.getSite(), msh.getMessageControlID().toString(),
						msh.getProcessingID().getProcessingID().getValue(),
						UPDATE_MESSAGE_TYPE);
			}
		}
		catch (DataTypeException e)
		{
			log.error("Exception when setting topic in message.", e);
			throw new STSException("Exception when setting topic in message.", e);
		}
		catch(RuntimeException e)
		{
			String errorMessage = "Exception when attempting to send the message.  Interface Engine may not be responding.";
			log.error(errorMessage);
			throw new STSException(errorMessage, e);
		}
		catch (Exception e)
		{
			log.error("Exception in finding message type", e);
			throw new STSException("Exception in finding message type", e);
		}
	}

	/*
	 * Send the HL7 Request Message to the specified topics. A new message id is
	 * generated for each message.
	 *
	 * @param message
	 *            message object of type MFQ_M01 that needs to be sent to the sites
	 * @param site
	 *            list of receiving sites
	 *
	 * @return message Id of the messages that have been sent to the sites.
	 *
	 * @throws VETSPublisherException
	 * @throws HL7Exception
	 */
	private synchronized static void sendHL7RequestMessage(MFQ_M01 message, List<PublishMessageDTO> siteList)
			throws STSException, STSException
	{
		try
		{
			for(PublishMessageDTO publishMessageDTO : siteList)
			{
				// insert the topic and message id
				MSH msh = message.getMSH();
				String sendingFacility = ApplicationPropertyReader.getApplicationProperty("msh.sendingFacility.namespaceId");
				msh.getSendingFacility().getNamespaceID().setValue(sendingFacility);
				msh.getReceivingFacility().getNamespaceID().setValue(publishMessageDTO.getSite().getVaSiteId());
				msh.getMessageControlID().setValue(((Long)publishMessageDTO.getMessageId()).toString());

				//insert the message type for the topic
				msh.getProcessingID().getProcessingID().setValue(publishMessageDTO.getSite().getMessageType());

				// Send the HL7 message
				if (useInterfaceEngine)
				{
					dispatcher.send(message);
				}
				else
				{
					//TODO: find code to re-implement if necessary. Leaving this out for now.
					/*
					String url = ApplicationPropertyReader.getApplicationProperty("emulator.url");
					if (url != null)
					{
						EmulatorDelegate.sendRequestMessage(HL7RequestGenerator.getRequestMessage(message), url);
					}
					 */
				}

				getDeploymentStatusMessage(HL7RequestGenerator.getRequestMessage(message), useInterfaceEngine,
						publishMessageDTO.getSite(), msh.getMessageControlID().toString(),
						msh.getProcessingID().getProcessingID().getValue(),
						QUERY_MESSAGE_TYPE);
			}
		}
		catch (DataTypeException e)
		{
			log.error("Exception in setting topic in message.", e);
			throw new STSException("Exception generating message.", e);
		}
		catch(RuntimeException e)
		{
			String errorMessage = "Exception when attempting to send the message.  Interface Engine may not be responding.";
			log.error(errorMessage);
			throw new STSException(errorMessage, e);
		}
		catch (Exception e)
		{
			log.error("Exception in finding message type", e);
			throw new STSException("Exception in finding message type", e);
		}
	}

	private static void getDeploymentStatusMessage(String message, boolean useInterfaceEngine,
			Site site, String messageId, String messageProcessingType, String messageType)
	{
		StringBuilder deploymentStatus = new StringBuilder();
		deploymentStatus.append("Site: ").append(site.getName()).append("; Message ID: ").append(messageId);
		deploymentStatus.append("; Message Type: ").append(messageProcessingType);
		deploymentStatus.append("; Parameter useInterfaceEngine is set to '").append(useInterfaceEngine).append("' so a message ");
		deploymentStatus.append(useInterfaceEngine == true ? "was " : "WAS NOT ");
		deploymentStatus.append("deployed to ").append( site.getName()).append(".");
		log.info(deploymentStatus.toString());

		int mshBeginIndex = message.indexOf("MSH");

		int mshEndIndex = 0;
		if(messageType.equals(UPDATE_MESSAGE_TYPE))
		{
			mshEndIndex = message.indexOf("MFI");
		}
		else
		{
			mshEndIndex = message.indexOf("QRD");
		}

		String messageHeader = message.substring(mshBeginIndex, mshEndIndex);

		log.info("Message header: {}", messageHeader);

		log.info("Full message: \n{}", message);
	}

	/*
	 * Determine whether we will be using the Interface Engine when the message is sent.
	 * @throws ETSBusinessException
	 * @return boolean
	 * @throws STSException
	 */
	private static boolean getInterfaceEngineUsage() throws STSException
	{
		String useIE = ApplicationPropertyReader.getApplicationProperty("useInterfaceEngine");

		// default to not use Interface Engine
		boolean ieUsage = false;
		if (useIE.equalsIgnoreCase("true") || useIE.equalsIgnoreCase("false"))
		{
			useIE.toLowerCase();
			ieUsage = Boolean.valueOf(useIE).booleanValue();
		}
		else
		{
			throw new STSException("useInterfaceEngine parameter must be 'true' or 'false'.");
		}
		return ieUsage;
	}

	/**
	 * @see javafx.concurrent.Task#call()
	 */
	@Override
	protected Integer call() throws Exception
	{
		updateProgress(-1, 0);

		//		updateMessage("Creating Checksum Files");
		//		writeChecksumFile(pomFile_, "MD5");
		//		writeChecksumFile(pomFile_, "SHA1");
		//
		//		for (File f : dataFiles_)
		//		{
		//			writeChecksumFile(f, "MD5");
		//			writeChecksumFile(f, "SHA1");
		//		}
		//
		//		updateMessage("Uploading data files");
		//		for (File f : dataFiles_)
		//		{
		//			//TODO check maven upload order
		//			putFile(f, null);
		//			putFile(new File(f.getParentFile(), f.getName() + ".md5"), null);
		//			putFile(new File(f.getParentFile(), f.getName() + ".sha1"), null);
		//		}
		//
		//		updateMessage("Uploading pom files");
		//		putFile(pomFile_, "pom");
		//		putFile(new File(pomFile_.getParentFile(), pomFile_.getName() + ".md5"), "pom.md5");
		//		putFile(new File(pomFile_.getParentFile(), pomFile_.getName() + ".sha1"), "pom.sha1");
		//
		//		updateMessage("Publish Complete");
		//		updateProgress(10, 10);
		return 0;
	}
}
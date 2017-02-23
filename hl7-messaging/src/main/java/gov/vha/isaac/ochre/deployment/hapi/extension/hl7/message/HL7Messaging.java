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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.deployment.hapi.extension.hl7.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.MFR_M01;
import ca.uhn.hl7v2.model.v24.segment.MSA;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.deployment.listener.HL7ResponseListener;
import gov.vha.isaac.ochre.deployment.publish.HL7RequestGenerator;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.MessageProperties;
import gov.vha.isaac.ochre.services.exception.STSException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 * {@link HL7Messaging}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class HL7Messaging
{
	private static final Logger LOG = LogManager.getLogger(HL7Messaging.class);
	private static ApplicationProperties applicationProperties_ = null;
	
	/**
	 * Call this method once, at the initial system startup, to configure and enable the socket listener
	 * that listens to responses routed back to us.
	 * 
	 * This method should only be called once in the lifecycle of the app - and you do not need to do a shutdown, 
	 * the underlying service will automatically shutdown when the {@link LookupService} notifies of a shutdown.	
	 * @param props
	 * @throws IOException if the service fails to start for any reason (port not available, unexpected error, etc)
	 */
	public static void enableListener(ApplicationProperties applicationProperties) throws IOException
	{
		if (applicationProperties_ != null)
		{
			throw new IllegalArgumentException("Application Properties should only be set once per lifetime of the system.");
		}
		if (applicationProperties == null) {
			LOG.error("HL7ApplicationProperties is null!");
			throw new IllegalArgumentException("HL7ApplicationProperties is null");
		}
		LOG.info("ApplicationProperties.getUseInterfaceEngine() is " + applicationProperties.getUseInterfaceEngine());
		applicationProperties_ = applicationProperties;
		LookupService.get().getService(HL7ResponseListener.class).finishInit(applicationProperties_);
	}
	
	/**
	 * returns true if the underlying response listener is up and running properly on the port specified via a call to {@link #enableListener(ApplicationProperties)}
	 * false if it either hasn't yet been called, or failed to start for some reason.
	 */
	public static boolean isRunning()
	{
		return LookupService.get().getService(HL7ResponseListener.class).isRunning();
	}
	
	/**
	 * This method will create a task for each {@link PublishMessage} object provided.  The tasks will be returned in the same order as the {@link PublishMessage}
	 * list.  If any {@link PublishMessage} is invalid, the entire batch will fail (via a RuntimeException thrown by this method) and no tasks will be created / started.
	 * 
	 * The returned task list will already be executing in a thread pool upon return.  Tasks will go to a completed state in parallel, as the responses come in.
	 * When a response comes in, the methods {@link PublishMessage#setChecksum(String)} and {@link PublishMessage#setRawHL7Message(String)} will be called prior to the 
	 * task completing.  The task returns no response - upon completion, the caller should inspect the updated PublishMessage for the results.
	 * 
	 * Note, if there was a timeout, and no response was received, neither of the setters above will be called, to indicate no data was available.
	 * 
	 * By calling {@link Task#getMessage()}, one can see / observe the current state of the request - this message will progress from user friendly strings like 
	 * 'Sending Message' to 'Waiting for response'.  If there was a timeout, the ending message will indicate so as well (in addition to the setters not being called)
	 * 
	 * @param publishMessages
	 * @param applicationProperties
	 * @param messageProperties
	 * @return
	 */
	public static List<Task<Void>> checksum(List<PublishMessage> publishMessages, MessageProperties messageProperties) {
		
		HL7ResponseListener hl7rl = LookupService.get().getService(HL7ResponseListener.class); 
		if (!hl7rl.isRunning())
		{
			throw new RuntimeException("The HL7 Listener service is not running.  Cannot receive responses");
		}

		LOG.info("Building the task to send an HL7 checksum message...");
		
		if (messageProperties == null) {
			LOG.error("HL7MessageProperties is null!");
			throw new IllegalArgumentException("HL7MessageProperties is null");
		}
		if (publishMessages == null) {
			LOG.error("PublishMessage is null!");
			throw new IllegalArgumentException("PublishMessage is null!");
		}
		List<Task<Void>> tasks = new ArrayList<>();
		// if messages are constructed, send
		for (PublishMessage message : publishMessages) {

			checkMessage(message);
			Task<Void> sender = new Task<Void>() 
			{
				{
					this.messageProperty().addListener(new ChangeListener<String>()
					{
						@Override
						public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
						{
							LOG.info("Message: " + newValue);
						}
					});
				}
				@Override
				protected Void call() throws Exception 
				{

					String hl7ChecksumMessage;
					updateTitle("Checksum Request");
					updateMessage("Preparing to send checksum");

					try 
					{
						hl7ChecksumMessage = HL7RequestGenerator.getChecksumRequestMessage(message.getSubset(),applicationProperties_, messageProperties);
						LOG.info("Sending HL7 message without site: " + hl7ChecksumMessage);
						HL7Sender hl7Sender = new HL7Sender(hl7ChecksumMessage, message, applicationProperties_, messageProperties);

						updateMessage("Sending");
						VistaRequestResponseHandler vrrh = new VistaRequestResponseHandler();
						hl7Sender.send(vrrh);
						updateMessage("Message Sent, waiting for response");
						Message m = vrrh.waitForResponse();
						
						if (m == null)
						{
							updateMessage("No response received");
						}
						else
						{
							updateMessage("Processing response");
							if (message instanceof MFR_M01)
							{
								MFR_M01 mfr = (MFR_M01) message;
								MSA msa = mfr.getMSA();
								
								/*
								The msaMessage will be vista embedded string
								;CHECKSUM:f8788e471664f6b285b4472660e2e3c9;VERSION:13;
								*/
								message.setChecksum(getValueFromTokenizedString("CHECKSUM", msa.getTextMessage().toString()));
								message.setVersion(getValueFromTokenizedString("VERSION", msa.getTextMessage().toString()));
								message.setRawHL7Message(mfr.toString());
							}
						}
						
						updateTitle("Complete");
					} 
					catch (STSException e) 
					{
						String msg = String.format(
								"Could not create HL7 message.  Please check logs from incoming string {}.  Also verify HL7ApplicationProperties.",
								message.getSubset());

						LOG.error(msg);
						updateMessage("Unexpected internal error");
						throw new Exception(msg);
					} 
					catch (Throwable e) 
					{
						LOG.error("Unexpected error", e);
						updateMessage("Unexpected internal error");
						throw new Exception(e);
					}
					return null;
				}
			};
			tasks.add(sender);
		}
		
		//Start all the tasks
		for (Task<Void> t : tasks)
		{
			hl7rl.launchListener(t);
		}
		
		return tasks; 
	}
	
	public static List<Task<Void>> discovery(List<PublishMessage> publishMessages, MessageProperties messageProperties) {
		
		HL7ResponseListener hl7rl = LookupService.get().getService(HL7ResponseListener.class); 
		if (!hl7rl.isRunning())
		{
			throw new RuntimeException("The HL7 Listener service is not running.  Cannot receive responses");
		}

		LOG.info("Building the task to send an HL7 discovery message...");

		if (messageProperties == null) {
			LOG.error("HL7MessageProperties is null!");
			throw new IllegalArgumentException("HL7MessageProperties is null");
		}
		if (publishMessages == null) {
			LOG.error("PublishMessage is null!");
			throw new IllegalArgumentException("PublishMessage is null!");
		}
		
		List<Task<Void>> tasks = new ArrayList<>();
		for (PublishMessage message : publishMessages) 
		{
			checkMessage(message);
			
			Task<Void> sender = new Task<Void>() 
			{
				{
					this.messageProperty().addListener(new ChangeListener<String>()
					{
						@Override
						public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
						{
							LOG.info("Message: " + newValue);
						}
					});
				}
				
				@Override
				protected Void call() throws Exception 
				{
					updateTitle("Discovery Request");
					updateMessage("Preparing to send discovery");

					try 
					{
						String hl7DiscoveryMessage =  HL7RequestGenerator.getSiteDataRequestMessage(message.getSubset(), applicationProperties_, messageProperties);
						LOG.info("Sending HL7 message without site: " + hl7DiscoveryMessage);
						HL7Sender hl7Sender = new HL7Sender(hl7DiscoveryMessage, message, applicationProperties_, messageProperties);
						
						updateTitle("Sending");
						VistaRequestResponseHandler vrrh = new VistaRequestResponseHandler();
						hl7Sender.send(vrrh);
						updateMessage("Message Sent, waiting for response");
						Message m = vrrh.waitForResponse();
						
						if (m == null)
						{
							updateMessage("No response received");
						}
						else
						{
							updateMessage("Processing response");
							//TODO Nuno process
							//We need to parse out the table of data, and change the signature of the message.setDiscovery...() to carry the parsed data table
							//and potentially, the headers
						}

						updateTitle("Complete");
					}
					catch (STSException e) 
					{
						String msg = String.format(
								"Could not create HL7 message.  Please check logs from incoming string {}.  Also verify HL7ApplicationProperties.",
								message.getSubset());
						LOG.error(msg);
						updateMessage("Unexpected internal error");
						throw new RuntimeException(msg);
					} catch (Throwable e) {
						LOG.error("Unexpected error", e);
						updateMessage("Unexpected internal error");
						throw new RuntimeException(e);
					}
					return null;
				}
			};
			
			tasks.add(sender);
		}
		
		//Start all the tasks
		for (Task<Void> t : tasks)
		{
			hl7rl.launchListener(t);
		}
		
		return tasks; 
	}
	
	private static void checkMessage(PublishMessage message)
	{
		if (StringUtils.isBlank(message.getSubset())) {
			LOG.error("No checksum message to send for id {}", message.getMessageId());
			throw new IllegalArgumentException("No checksum message to send for id " + message.getMessageId());
		} else if (message.getSite() == null) {
			LOG.error("No sites to send for id {}", message.getMessageId());
			throw new IllegalArgumentException("No sites to send for id " + message.getMessageId());
		}
	}
	
	private static String getValueFromTokenizedString(String token, String input) {
		StringTokenizer tokenizer = new StringTokenizer(input, ";");
		String currentToken = null;
		int colonIndex = -1;
		String result = "";
		
		while (tokenizer.hasMoreTokens()) {
			currentToken = tokenizer.nextToken();
			if (currentToken.startsWith(token)) {
				colonIndex = token.indexOf(':');
				if (colonIndex != -1 && (token.length() > (colonIndex + 1))) {
					result = token.substring(colonIndex + 1);
				}
			}
		}
		
		return result; 
	}
}

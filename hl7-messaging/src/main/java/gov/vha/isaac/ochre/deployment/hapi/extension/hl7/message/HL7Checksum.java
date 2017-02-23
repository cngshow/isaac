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
package gov.vha.isaac.ochre.deployment.hapi.extension.hl7.message;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.model.Message;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.deployment.listener.HL7ResponseReceiveListener;
import gov.vha.isaac.ochre.deployment.publish.HL7RequestGenerator;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.MessageProperties;
import gov.vha.isaac.ochre.services.exception.STSException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

public class HL7Checksum implements HL7ResponseReceiveListener
{
	private static final Logger LOG = LogManager.getLogger(HL7Checksum.class);

	// object to lock on
	private final Object lock = new Object();
	private volatile boolean running = true;
	
	private String messageId_;
	private Message responseMessage_;

	// hey the entire system was shutdown

	public static Task<String> checksum(List<PublishMessage> publishMessages,
			ApplicationProperties applicationProperties, MessageProperties messageProperties) {

		LOG.info("Building the task to send an HL7 message...");
		if (applicationProperties == null) {
			LOG.error("HL7ApplicationProperties is null!");
			throw new IllegalArgumentException("HL7ApplicationProperties is null");
		}
		LOG.info("ApplicationProperties.getUseInterfaceEngine() is " + applicationProperties.getUseInterfaceEngine());
		if (messageProperties == null) {
			LOG.error("HL7MessageProperties is null!");
			throw new IllegalArgumentException("HL7MessageProperties is null");
		}
		if (publishMessages == null) {
			LOG.error("PublishMessage is null!");
			throw new IllegalArgumentException("PublishMessage is null!");
		}

		// if messages are constructed, send
		for (PublishMessage message : publishMessages) {

			if (StringUtils.isBlank(message.getSubset())) {
				LOG.error("No checksum message to send for id {}", message.getMessageId());
			} else if (message.getSite() == null) {
				LOG.error("No sites to send for id {}", message.getMessageId());
			} else {
				Task<String> sender = new Task() {

					@Override
					protected String call() throws Exception {

						String hl7ChecksumMessage;
						String tag = "done";
						updateMessage("Preparing");
						LOG.info("Preparing");

						try {

							try {
								hl7ChecksumMessage = HL7RequestGenerator.getChecksumRequestMessage(message.getSubset(),
										applicationProperties, messageProperties);

							} catch (STSException e) {

								String msg = String.format(
										"Could not create HL7 message.  Please check logs from incoming string {}.  Also verify HL7ApplicationProperties.",
										message.getSubset());

								LOG.error(msg);
								throw new RuntimeException(msg);
							}

							updateTitle("Sending HL7 message");
							LOG.info("Sending HL7 message without site: " + hl7ChecksumMessage);

							HL7Sender hl7Sender = new HL7Sender(hl7ChecksumMessage, message, applicationProperties,
									messageProperties);

							hl7Sender.send();

							hl7Sender.progressProperty().addListener(new ChangeListener<Number>() {
								@Override
								public void changed(ObservableValue<? extends Number> observable, Number oldValue,
										Number newValue) {
									updateProgress(hl7Sender.getWorkDone(), hl7Sender.getTotalWork());
								}
							});
							hl7Sender.messageProperty().addListener(new ChangeListener<String>() {
								@Override
								public void changed(ObservableValue<? extends String> observable, String oldValue,
										String newValue) {
									updateMessage(newValue);
								}
							});

							WorkExecutors.get().getExecutor().execute(hl7Sender);
							hl7Sender.get();

							updateTitle("Complete");
							LOG.info("Complete");
						} catch (Throwable e) {

							LOG.error("Unexpected error", e);
							throw new RuntimeException(e);
						} finally {
							// unregister
						}
						return tag;
					}

				};

			}
		}
		return null; // this should be Task<String> or sender.

	}

	@Override
	public String getListenerId() {
		return messageId_;
	}

	@Override
	public void handleResponse(Message message) {
		LOG.debug("HANDLE RESPONSE");

		synchronized (lock) {
			while (running) {
				try {
					lock.wait(1000 * 60 * 30);
				} catch (InterruptedException e) {
					// oh no...
				}
			}
		}

	}

	@Override
	public void wakeUp(Message responseMessage) {
		synchronized (lock) {
			responseMessage_ = responseMessage;
			lock.notify();
		}
	}

	/**
	 * A utility method to execute a task and wait for it to complete.
	 * 
	 * @param task
	 * @return the string returned by the task
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static String executeAndBlock(Task<String> task) throws InterruptedException, ExecutionException {
		LOG.info("executeAndBlock with task " + task);
		WorkExecutors.get().getExecutor().execute(task);
		String result = task.get();
		LOG.info("result of task: " + result);
		return result;
	}

}

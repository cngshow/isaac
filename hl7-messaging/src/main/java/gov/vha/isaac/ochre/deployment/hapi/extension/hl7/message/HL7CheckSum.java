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

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.deployment.publish.HL7RequestGenerator;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;
import gov.vha.isaac.ochre.services.exception.STSException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

public class HL7CheckSum
{

	private static final Logger LOG = LogManager.getLogger(HL7CheckSum.class);

	public static Task<String> checkSum(String checkSum, List<PublishMessageDTO> siteList,
			HL7ApplicationProperties applicationProperties) {

		LOG.info("Building the task to send an HL7 message...");

		if (StringUtils.isBlank(checkSum)) {
			LOG.error("No checksum message to send!");
			throw new IllegalArgumentException("No checksum message to send!");
		}
		if (siteList == null || siteList.size() == 0) {
			LOG.error("No sites to send to!");
			throw new IllegalArgumentException("No sites to send to!");
		}

		String hl7CheckSumMessage;
		try {
			hl7CheckSumMessage = HL7RequestGenerator.getChecksumRequestMessage(checkSum, applicationProperties);
		} catch (STSException e) {
			String msg = String.format(
					"Could not create HL7 message.  Please check logs from incoming string {}.  Also verify HL7ApplicationProperties.",
					checkSum);
			LOG.error(msg);
			throw new RuntimeException(msg);
		}

		// if message is constructed, send
		Task<String> sender = new Task<String>() {
			@Override
			protected String call() throws Exception {
				String tag = "done";
				updateMessage("Preparing");
				LOG.info("Preparing");
				try {
					updateTitle("Sending HL7 message");
					LOG.info("Sending HL7 message without site: " + hl7CheckSumMessage);

					HL7Sender hl7Sender = new HL7Sender(hl7CheckSumMessage, siteList, applicationProperties);
					//hl7Sender.send(hl7CheckSumMessage, siteList, applicationProperties);

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

					return tag;
				} catch (Throwable e) {
					LOG.error("Unexpected error", e);
					throw new RuntimeException(e);
				}

			}
		};

		LOG.info("returning");
		return sender;
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

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

public class HL7CheckSum {

	private static final Logger LOG = LogManager.getLogger();

	public static Task<String> checkSum(String hl7Message, List<PublishMessageDTO> siteList) throws Throwable
	{
		LOG.info("Building the task to send an HL7 message...");

		if (StringUtils.isBlank(hl7Message))
		{
			LOG.error("No message to send!");
			throw new Exception("No message to send!");
		}
		if (siteList == null || siteList.size() == 0)
		{
			LOG.error("No sites to send to!");
			throw new Exception("No sites to send to!");
		}


		Task<String> sender = new Task<String>()
		{
			@Override
			protected String call() throws Exception
			{
				String tag = "";
				updateMessage("Preparing");
				try
				{
					updateTitle("Sending HL7 message");

					HL7Sender hl7Sender = new HL7Sender(hl7Message, siteList);

					hl7Sender.progressProperty().addListener(new ChangeListener<Number>()
					{
						@Override
						public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
						{
							updateProgress(hl7Sender.getWorkDone(), hl7Sender.getTotalWork());
						}
					});
					hl7Sender.messageProperty().addListener(new ChangeListener<String>()
					{
						@Override
						public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
						{
							updateMessage(newValue);
						}
					});

					WorkExecutors.get().getExecutor().execute(hl7Sender);

					//updateTitle("sleep for 10 seconds");
					//Thread.sleep(10000);

					updateTitle("Complete");

					return tag;
				}
				catch (Throwable e)
				{
					LOG.error("Unexpected error", e);
					throw new RuntimeException(e);
				}

			}
		};

		LOG.info("returning");
		return sender;
	}

	//	/**
	//	 * A utility method to execute a task and wait for it to complete.
	//	 * @param task
	//	 * @return the string returned by the task
	//	 * @throws InterruptedException
	//	 * @throws ExecutionException
	//	 */
	//	public static String executeAndBlock(Task<String> task) throws InterruptedException, ExecutionException
	//	{
	//		LOG.trace("executeAndBlock with task " + task);
	//		System.out.println("executeAndBlock with task " + task);
	//		WorkExecutors.get().getExecutor().execute(task);
	//		String result = task.get();
	//		LOG.trace("result of task: " + result);
	//		System.out.println("result of task: " + result);
	//		return result;
	//	}

}

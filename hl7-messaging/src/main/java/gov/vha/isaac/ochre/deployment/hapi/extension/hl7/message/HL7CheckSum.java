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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import javafx.concurrent.Task;

public class HL7CheckSum {

	private static final Logger LOG = LogManager.getLogger();

	public static Task<String> checkSum(String hl7Message, List<PublishMessageDTO> siteList) throws Throwable
	{
		LOG.info("Building the task to send an HL7 message...");

		if (hl7Message == null)
		{
			throw new Exception("No message to send!");
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

					//WorkExecutors.get().getExecutor().execute(HL7Sender.send(hl7Message, siteList));


					//block till upload complete
					//pm.get();

					//updateTitle("Cleaning Up");
					Thread.sleep(1000);

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

		return sender;
	}

}

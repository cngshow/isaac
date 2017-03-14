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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.model.Message;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.deployment.listener.HL7ResponseListener;
import gov.vha.isaac.ochre.deployment.listener.HL7ResponseReceiveListener;

/**
 * {@link VistaRequestResponseHandler}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class VistaRequestResponseHandler implements HL7ResponseReceiveListener
{
	// object to lock on
	final Object lock_ = new Object();
	private volatile Message responseMessage_ = null;
	private long createTime_ = System.currentTimeMillis();
	
	/** A logger for messages produced by this class. */
	private static Logger LOG = LogManager.getLogger(VistaRequestResponseHandler.class);

	protected Message waitForResponse()
	{
		long sleepTime = calculateNextSleep();
		while (responseMessage_ == null && sleepTime > 0 && LookupService.get().getService(HL7ResponseListener.class).isRunning())
		{
			synchronized (lock_)
			{
				try
				{
					LOG.debug("Sleeping for " + sleepTime + "ms");  //TODO change this to trace later
					lock_.wait(sleepTime);
				}
				catch (InterruptedException e)
				{
					// noop, loop
				}
			}
			sleepTime = calculateNextSleep();
		}
		if (responseMessage_ == null)
		{
			LOG.debug("No response was generated - left the wait loop with last calculated sleep time: " + sleepTime
					+ " isRunning: " + LookupService.get().getService(HL7ResponseListener.class).isRunning());
		}
		return responseMessage_;
	}
	
	private long calculateNextSleep()
	{
		long waitUntil = HL7ResponseListener.MAX_WAIT_TIME + createTime_;
		return waitUntil - System.currentTimeMillis();
	}

	@Override
	public void handleResponse(Message message)
	{
		synchronized (lock_) {
			responseMessage_ = message;
			lock_.notify();
		}
	}

}
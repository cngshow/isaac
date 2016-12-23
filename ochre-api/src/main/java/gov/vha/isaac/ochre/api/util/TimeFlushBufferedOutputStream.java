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
package gov.vha.isaac.ochre.api.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.ochre.api.Get;

/**
 * {@link TimeFlushBufferedOutputStream}
 * 
 * A wrapper for a {@link BufferedOutputStream} which calls flush once a minute, to ensure
 * that data is not hanging out unbuffered for a long period of time, before being written to disk.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class TimeFlushBufferedOutputStream extends BufferedOutputStream
{
	private static final ArrayList<WeakReference<TimeFlushBufferedOutputStream>> instances_ = new ArrayList<>(); 
	private static ScheduledFuture<?> scheduledJob_;
	private static final Logger logger = LoggerFactory.getLogger(TimeFlushBufferedOutputStream.class);

	/**
	 * @param out
	 */
	public TimeFlushBufferedOutputStream(OutputStream out)
	{
		super(out);
		scheduleFlush();
	}
	
	public TimeFlushBufferedOutputStream(OutputStream out, int size)
	{
		super(out, size);
		scheduleFlush();
	}
	
	@Override
	public void close() throws IOException
	{
		synchronized(instances_)
		{
			Iterator<WeakReference<TimeFlushBufferedOutputStream>> it = instances_.iterator();
			while (it.hasNext())
			{
				if (it.next().get() == this)
				{
					it.remove();
					break;
				}
			}
		}
		super.close();
	}
	
	private void scheduleFlush()
	{
		synchronized (instances_)
		{
			instances_.add(new WeakReference<TimeFlushBufferedOutputStream>(this));
		}
		
		//Just sync on something at the class level
		synchronized (logger)
		{
			if (scheduledJob_ == null)
			{
				logger.info("Scheduling thread to flush time flush buffers");
				scheduledJob_ = Get.workExecutors().getScheduledThreadPoolExecutor().scheduleAtFixedRate(() -> 
				{
					if (instances_.size() == 0)
					{
						scheduledJob_.cancel(false);
						scheduledJob_ = null;
						logger.info("Stopping time flush buffer thread, as no instances are registered");
					}
					else
					{
						logger.debug("Calling flush on " + instances_.size() + " buffered writers");
						synchronized (instances_)
						{
							Iterator<WeakReference<TimeFlushBufferedOutputStream>> it = instances_.iterator();
							while (it.hasNext())
							{
								WeakReference<TimeFlushBufferedOutputStream> item = it.next();
								if (item.get() == null)
								{
									it.remove();
								}
								else
								{
									try
									{
										item.get().flush();
									}
									catch (Exception e)
									{
										logger.error("error during time flush", e);
									}
								}
							}
						}
					}
				}, 1, 1, TimeUnit.MINUTES);
			}
		}
	}
}

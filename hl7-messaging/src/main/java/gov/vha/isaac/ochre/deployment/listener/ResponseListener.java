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
package gov.vha.isaac.ochre.deployment.listener;

import gov.vha.isaac.ochre.deployment.listener.parser.AcknowledgementParser;
import gov.vha.isaac.ochre.deployment.listener.parser.ChecksumParser;
import gov.vha.isaac.ochre.deployment.listener.parser.SiteDataParser;
import gov.vha.isaac.ochre.deployment.publish.MessageTypeIdentifier;
import gov.vha.isaac.ochre.services.exception.STSException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResponseListener extends Thread
{
	private static Logger log = LogManager.getLogger(ResponseListener.class.getPackage().getName());

	private static Map<SelectionKey, StringBuffer> messageMap = Collections
			.synchronizedMap(new HashMap<SelectionKey, StringBuffer>());

	int port;
	Selector selector = null;
	ServerSocketChannel selectableChannel = null;

	int keysAdded = 0;

	/*
	 * Make this constructor private because this class must be instantiated
	 * with the port.
	 */
	private ResponseListener()
	{}

	public ResponseListener(int port)
	{
		this.port = port;
	}

	public void initialize() throws IOException
	{
		this.selector = SelectorProvider.provider().openSelector();
		this.selectableChannel = ServerSocketChannel.open();
		this.selectableChannel.configureBlocking(false);
		InetAddress localHost = InetAddress.getLocalHost();
		InetSocketAddress isa = new InetSocketAddress(localHost, this.port);

		if (this.selectableChannel.isOpen() == true)
		{
			this.selectableChannel.socket().setReuseAddress(true);
			this.selectableChannel.socket().bind(isa);
		}
	}

	public void acceptConnections() throws IOException
	{
		SelectionKey acceptKey = null;
		if (selector.isOpen() == true & selectableChannel != null & selectableChannel.isOpen() == true)
		{
			acceptKey = this.selectableChannel.register(this.selector, SelectionKey.OP_ACCEPT);
		}
		else
		{
			return;
		}

		log.debug("Non-blocking server: acceptor loop...");
		while (selectableChannel.isOpen() == true & selector.isOpen() == true
				& acceptKey != null
				& (this.keysAdded = acceptKey.selector().select()) > 0)
		{
			if (selector.isOpen() == false | this.selectableChannel.isOpen() == false)
			{
				break;
			}
			Set readyKeys = this.selector.selectedKeys();
			Iterator i = readyKeys.iterator();
			while (i.hasNext())
			{
				SelectionKey key = (SelectionKey) i.next();
				i.remove();
				if (key.isValid() && key.isAcceptable())
				{
					try
					{
						ServerSocketChannel nextReady = (ServerSocketChannel) key.channel();
						SocketChannel channel = nextReady.accept();
						channel.configureBlocking(false);
						SelectionKey readKey = channel.register(this.selector, SelectionKey.OP_READ
								| SelectionKey.OP_WRITE);
						readKey.attach(new ChannelCallback(channel));
					}
					catch (Exception e)
					{
						log.error("Problem accepting key.", e);
					}
				}
				if (key.isValid() && key.isReadable())
				{
					try
					{
						this.readMessage((ChannelCallback) key.attachment(), key);
					}
					catch (Exception e)
					{
						log.error("Exception in call to readMessage()", e);
					}
				}
			}
		}

		log.debug("Non-blocking server: end acceptor loop...");
	}

	static final int BUFSIZE = 1024;

	public String decode(ByteBuffer byteBuffer) throws CharacterCodingException
	{
		Charset charset = Charset.forName("us-ascii");
		CharsetDecoder decoder = charset.newDecoder();
		CharBuffer charBuffer = decoder.decode(byteBuffer);
		String result = charBuffer.toString();
		return result;
	}

	public void readMessage(ChannelCallback callback, SelectionKey key) throws STSException, IOException,
			InterruptedException
	{
		ByteBuffer byteBuffer = ByteBuffer.allocate(BUFSIZE);
		byteBuffer.clear();
		callback.getChannel().read(byteBuffer);
		byteBuffer.flip();
		String resultToAppend = this.decode(byteBuffer);

		if (resultToAppend.length() > 0)
		{
			StringBuffer inboundMessage = (StringBuffer) messageMap.get(key);
			if (inboundMessage == null)
			{
				inboundMessage = new StringBuffer("");
			}
			inboundMessage.append(resultToAppend);
			messageMap.put(key, inboundMessage);

			int indexOfEndChar = resultToAppend.indexOf((char) 28);
	        if (indexOfEndChar >= 0)
	        {
	            // parse the message, etc.
	            this.writeMessage(callback, key);
	        }
		}
		else 
		{
		    key.channel().close();
		}
	}

	public void writeMessage(ChannelCallback callback, SelectionKey key) throws STSException
	{
		int timeoutSeconds = 120;
		long start = System.currentTimeMillis();
		while (!key.isWritable())
		{
			// Wait here until the SocketChannel is writable
			long elapsedTimeMillis = System.currentTimeMillis() - start;
			float elapsedTimeMin = elapsedTimeMillis / (1000f);
			if (elapsedTimeMin > timeoutSeconds)
			{
				throw new STSException("Socket timeout after " + timeoutSeconds + " seconds. No message processed.");
			}
		}

		try
		{
			if (messageMap.containsKey(key))
			{
				StringBuffer inboundMessageBuffer = (StringBuffer) messageMap.get(key);

				if (inboundMessageBuffer != null)
				{
					// Remove the vertical tab character if it exists and anything before it
					int verticalTabIndex = inboundMessageBuffer.indexOf(String.valueOf((char) 11));
					if (verticalTabIndex > 0)
					{
						inboundMessageBuffer.delete(0, verticalTabIndex);
					}
					else if (verticalTabIndex == 0)
					{
						inboundMessageBuffer.deleteCharAt(verticalTabIndex);
					}

					String messageToParse = inboundMessageBuffer.toString();

					// get the MSH line and save to a string
					String messageHeader = MessageTypeIdentifier.getMessageHeader(messageToParse);

					// generate the CA response message
					String responseMessage = ListenerHelper.getResponseMessage(messageHeader);

					// If there is a response to send, send it immediately, before parsing starts
					if (responseMessage != null)
					{
						log.debug("Outbound response message: " + responseMessage);
						ByteBuffer buf = ByteBuffer.wrap(responseMessage.getBytes());
						callback.getChannel().write(buf);
					}

					// Write every incoming message to the log
					log.info("Incoming message: " + messageToParse);

					// find out what type of message this is
					String messageType = MessageTypeIdentifier.getMessageType(messageHeader);

					// parse the acknowledgement message type
					if (messageType.equals(MessageTypeIdentifier.MFK_TYPE))
					{
						AcknowledgementParser ackParser = new AcknowledgementParser();
						ackParser.processMessage(messageToParse);
					}
					// parse the site data message type
					else if (messageType.equals(MessageTypeIdentifier.MFR_TYPE))
					{
						// Find out what the target app flag is
						String receivingApp = MessageTypeIdentifier.getIncomingMessageReceivingApp(messageHeader);

						if (receivingApp.equals(MessageTypeIdentifier.receivingAppSiteData))
						{
							 SiteDataParser discoveryParser = new SiteDataParser();
							 discoveryParser.processMessage(messageToParse);
						}
						else if (receivingApp.equals(MessageTypeIdentifier.receivingAppMd5))
						{
							ChecksumParser checksumParser = new ChecksumParser();
							checksumParser.processMessage(messageToParse);
						}
						else
						{
							log.error("Unknown receiving application name: " + receivingApp);
						}
					}
					else
					{
						log.error("Unknown message type.  Message header: " + messageHeader);
					}
				}
				else
				{
					throw new STSException("inboundMessageBuffer is empty: no message processed.");
				}
			}
			else
			{
				throw new STSException("Key not found in message map: no message processed.");
			}
		}
		catch (Exception e)
		{
			log.error(e);
			throw new STSException(e);
		}
		finally
		{
			messageMap.remove(key);
			try
			{
				callback.getChannel().close();
				log.info("SocketChannel connection closed.  Continuing to listen on port " + port + ".");
			}
			catch(IOException e)
			{
				log.error("Unable to close listener SocketChannel", e);
			}
		}
	}

	public void run()
	{
			try
			{
				initialize();
				acceptConnections();
			}
			catch (IOException e)
			{
				log.error("IOException thrown to run()", e);
			}
	}

	// Clean up work
	public void stopThread()
	{
		try
		{
			this.selectableChannel.socket().close();
			this.selectableChannel.close();
			this.selector.wakeup();
			this.selector.close();
		}
		catch (IOException e)
		{
			log.error(e);
		}
	}
}


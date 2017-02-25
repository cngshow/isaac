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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.MFN_M01;
import ca.uhn.hl7v2.model.v24.message.MFR_M01;
import ca.uhn.hl7v2.model.v24.segment.MSA;
import ca.uhn.hl7v2.parser.PipeParser;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.NamedThreadFactory;
import gov.vha.isaac.ochre.deployment.listener.parser.AcknowledgementParser;
import gov.vha.isaac.ochre.deployment.publish.MessageTypeIdentifier;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.exception.STSException;
import javafx.concurrent.Task;

@Service
@RunLevel(value = LookupService.WORKERS_STARTED_RUNLEVEL)
public class HL7ResponseListener
{
	/** A logger for messages produced by this class. */
	private static Logger LOG = LogManager.getLogger(HL7ResponseListener.class);

	/** A logger for messages inbound hl7 messages. */
	private static Logger HL7LOG = LogManager.getLogger("hl7messages");  //don't change this without adjusting rails_prisme/lib/logging/log4j2.xml

	private Map<SelectionKey, StringBuffer> messageMap = Collections.synchronizedMap(new HashMap<SelectionKey, StringBuffer>());

	private Selector selector = null;
	private ServerSocketChannel selectableChannel = null;
	
	//TODO get this from props_
	public static final long MAX_WAIT_TIME = 15 * 60 * 1000;  //15 minutes max wait for vitria response

	private static final int BUFSIZE = 1024;

	private static final String VETSDATA = "VETS DATA";
	private static final String VETSMD5 = "VETS MD5";
//	private static final String VETSUPDATE = "VETS UPDATE";

	ConcurrentHashMap<Long, HL7ResponseReceiveListener> hl7ResponseListeners = new ConcurrentHashMap<>();

	private int keysAdded = 0;  //TODO need to figure out what is up with this logic - why stored but unused?
	
	private boolean listening = false;
	
	private ThreadPoolExecutor responseListenerThreads_;
	
	private ApplicationProperties props_ = null;

	/*
	 * for HK2
	 */
	private HL7ResponseListener() {
	}
	
	public void finishInit(ApplicationProperties properties) throws IOException
	{
		if (props_ != null)
		{
			throw new IllegalArgumentException("Properties may only be changed after a service-level shutdown");
		}
		props_ = properties;
		
		LOG.info("Starting HL7ResponseListener on port {}.", props_.getListenerPort());

		responseListenerThreads_ = new ThreadPoolExecutor(200, 200, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
				new NamedThreadFactory("HL7ResponseListenerPool", true));
		responseListenerThreads_.allowCoreThreadTimeOut(true);
		
		initialize();
		LOG.debug("Started ResponseListener initialized");
		
		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					LOG.debug("Starting thread that reads socket data");
					acceptConnections();
					
				} catch (IOException e) {
					LOG.error("Error : {}", e.getMessage());
				}
				finally
				{
					LOG.info("Socket data reading thread dies!");
				}
			}
		};
		

		
		Thread listenThread = new Thread(r, "HL7-MIF-ReadThread");
		listenThread.setDaemon(true);
		listenThread.start();

		LOG.info("Started HL7ResponseListener on port {}.", props_.getListenerPort());
	}

	@PostConstruct
	private void startMe() {
		LOG.debug("HL7ResponseListener start called (but this is a noop - will not activate until finishInit is called)");
	}

	@PreDestroy
	private void stopMe() {
		try
		{
			this.selector.close();
			this.listening = false;
			this.responseListenerThreads_.shutdownNow();
			props_ = null;
		}
		catch (IOException e)
		{
			LOG.error("Error closing HL7Response Listener socket", e);
		}
		LOG.info("Finished HL7ResponseListener stop");
	}

	private void initialize() throws IOException {
		this.selector = SelectorProvider.provider().openSelector();
		this.selectableChannel = ServerSocketChannel.open();
		this.selectableChannel.configureBlocking(false);
		InetAddress localHost = InetAddress.getLocalHost();
		InetSocketAddress isa = new InetSocketAddress(localHost, props_.getListenerPort());

		if (this.selectableChannel.isOpen() == true) {
			this.selectableChannel.socket().setReuseAddress(true);
			this.selectableChannel.socket().bind(isa);
		}
		listening = true;
		LOG.debug("initialized on port {}", props_.getListenerPort());
	}

	private void acceptConnections() throws IOException {
		try
		{
			SelectionKey acceptKey = null;
			if (selector.isOpen() == true & selectableChannel != null & selectableChannel.isOpen() == true) {
				acceptKey = this.selectableChannel.register(this.selector, SelectionKey.OP_ACCEPT);
			} else {
				return;
			}

			LOG.debug("Non-blocking server: acceptor loop begins");
			while (selectableChannel.isOpen() == true & selector.isOpen() == true & acceptKey != null
					& (this.keysAdded = acceptKey.selector().select()) > 0) {
				if (selector.isOpen() == false | this.selectableChannel.isOpen() == false) {
					break;
				}
				Set<SelectionKey> readyKeys = this.selector.selectedKeys();
				Iterator<SelectionKey> i = readyKeys.iterator();
				while (i.hasNext()) {
					SelectionKey key = i.next();
					i.remove();
					if (key.isValid() && key.isAcceptable()) {
						try {
							ServerSocketChannel nextReady = (ServerSocketChannel) key.channel();
							SocketChannel channel = nextReady.accept();
							channel.configureBlocking(false);
							SelectionKey readKey = channel.register(this.selector,
									SelectionKey.OP_READ | SelectionKey.OP_WRITE);
							readKey.attach(new ChannelCallback(channel));
						} catch (Exception e) {
							LOG.error("Problem accepting key.", e);
						}
					}
					if (key.isValid() && key.isReadable()) {
						try {
							this.readMessage((ChannelCallback) key.attachment(), key);
						} catch (Exception e) {
							LOG.error("Exception in call to readMessage()", e);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOG.error("Unexpected error in HL7Response Listener thread - exiting!", e);
			throw e;
		}
		finally
		{
			listening = false;
			hl7ResponseListeners.clear();
			LOG.info("HL7ResponseListner stops listening for responses");
		}
	}

	private String decode(ByteBuffer byteBuffer) throws CharacterCodingException {
		Charset charset = Charset.forName("us-ascii");
		CharsetDecoder decoder = charset.newDecoder();
		CharBuffer charBuffer = decoder.decode(byteBuffer);
		String result = charBuffer.toString();
		return result;
	}

	private void readMessage(ChannelCallback callback, SelectionKey key)
			throws STSException, IOException, InterruptedException {

		LOG.debug("read message");
		ByteBuffer byteBuffer = ByteBuffer.allocate(BUFSIZE);
		byteBuffer.clear();
		callback.getChannel().read(byteBuffer);
		byteBuffer.flip();
		String resultToAppend = this.decode(byteBuffer);

		if (resultToAppend.length() > 0) {
			StringBuffer inboundMessage = (StringBuffer) messageMap.get(key);
			if (inboundMessage == null) {
				inboundMessage = new StringBuffer("");
			}
			inboundMessage.append(resultToAppend);
			messageMap.put(key, inboundMessage);

			int indexOfEndChar = resultToAppend.indexOf((char) 28);
			if (indexOfEndChar >= 0) {
				// parse the message, etc.
				this.writeMessage(callback, key);
			}
		} else {
			key.channel().close();
		}
	}

	private void writeMessage(ChannelCallback callback, SelectionKey key) throws STSException {

		LOG.debug("write message");
		int timeoutSeconds = 120;
		long start = System.currentTimeMillis();
		while (!key.isWritable()) {
			// Wait here until the SocketChannel is writable
			long elapsedTimeMillis = System.currentTimeMillis() - start;
			float elapsedTimeMin = elapsedTimeMillis / (1000f);
			if (elapsedTimeMin > timeoutSeconds) {
				throw new STSException("Socket timeout after " + timeoutSeconds + " seconds. No message processed.");
			}
		}

		try {
			if (messageMap.containsKey(key)) {
				StringBuffer inboundMessageBuffer = (StringBuffer) messageMap.get(key);

				if (inboundMessageBuffer != null) {
					LOG.debug("Incoming message: {}", inboundMessageBuffer.toString());

					// Remove the vertical tab character if it exists and
					// anything before it
					int verticalTabIndex = inboundMessageBuffer.indexOf(String.valueOf((char) 11));
					if (verticalTabIndex > 0) {
						inboundMessageBuffer.delete(0, verticalTabIndex);
					} else if (verticalTabIndex == 0) {
						inboundMessageBuffer.deleteCharAt(verticalTabIndex);
					}

					String messageToParse = inboundMessageBuffer.toString();
					HL7LOG.info(messageToParse);

					// get the MSH line and save to a string
					String messageHeader = MessageTypeIdentifier.getMessageHeader(messageToParse);
					LOG.debug("messageHeader: {}", messageHeader);

					// generate the CA response message
					String responseMessage = ListenerHelper.getResponseMessage(messageHeader);
					LOG.debug("responseMessage: {}", responseMessage);

					// If there is a response to send, send it immediately,
					// before parsing starts
					if (responseMessage != null) {
						LOG.debug("Outbound response message: {}", responseMessage);
						ByteBuffer buf = ByteBuffer.wrap(responseMessage.getBytes());
						callback.getChannel().write(buf);
					}

					// find out what type of message this is
					String messageType = MessageTypeIdentifier.getMessageType(messageHeader);
					LOG.debug("messageType: {}", messageType);

					// parse the acknowledgement message type
					if (MessageTypeIdentifier.MFK_TYPE.equals(messageType)) {
						AcknowledgementParser ackParser = new AcknowledgementParser();
						ackParser.processMessage(messageToParse);
					}
					// parse the site data message type
					else if (MessageTypeIdentifier.MFR_TYPE.equals(messageType)) {
						// Find out what the target app flag is
						String receivingApp = MessageTypeIdentifier.getIncomingMessageReceivingApp(messageHeader);

						if (VETSDATA.equalsIgnoreCase(receivingApp)) {
							PipeParser parser = new PipeParser();
							Message message = parser.parse(messageToParse);

							handleResponseNotification(getMessageControlId(message), message);
						}

						else if (VETSMD5.equalsIgnoreCase(receivingApp)) {
							PipeParser parser = new PipeParser();
							Message message = parser.parse(messageToParse);

							handleResponseNotification(getMessageControlId(message), message);

						} else {
							LOG.error("Unknown receiving application name: " + receivingApp);
						}

					} else {
						LOG.error("Unknown message type.  Message header: " + messageHeader);
					}
				} else {
					throw new STSException("inboundMessageBuffer is empty: no message processed.");
				}
			} else {
				throw new STSException("Key not found in message map: no message processed.");
			}
		} catch (Exception e) {
			LOG.error(e);
			throw new STSException(e);
		} finally {
			messageMap.remove(key);
			try {
				callback.getChannel().close();
				LOG.debug("SocketChannel connection closed.  Continuing to listen on port {}.", props_.getListenerPort());
			} catch (IOException e) {
				LOG.error("Unable to close listener SocketChannel", e);
			}
		}
	}

	private void handleResponseNotification(String messageId, Message message) {

		LOG.debug("in handleResponseNotification hl7ResponseListeners count: {}", hl7ResponseListeners.size());
		
		if (LOG.isTraceEnabled())
		{
			hl7ResponseListeners.forEach((id, listener) -> {
				LOG.trace("hl7ResponseListeners: {}", id);
			} );
		}
		
		try
		{
			long id = Long.parseLong(messageId.trim());
			HL7ResponseReceiveListener waitingTask = hl7ResponseListeners.remove(id);
			if (waitingTask == null)
			{
				LOG.error("No listener was registered for the message with an id of {} - {}", id, message);
			}
			else
			{
				waitingTask.handleResponse(message);
			}
		}
		catch (Exception e)
		{
			LOG.error("Unable to parse back the message ID from {}", messageId);
		}
	}

	//get the id from the message.
	//referred as message control id in hapi
	private String getMessageControlId(Message message) {

		String msaMessageControlId = "";

		if (message instanceof MFR_M01) {
			MFR_M01 mfk = (MFR_M01) message;
			MSA msa = mfk.getMSA();
			msaMessageControlId = msa.getMessageControlID().toString();
			
		} else if (message instanceof MFN_M01) {
			//MFN_M01 mfn = (MFN_M01) message;
			//MSH msh = mfn.getMSH();
			//msaMessageControlId = msh.getMessageControlID().toString();
			
			BufferedReader br = new BufferedReader(new StringReader(message.toString()));
			String line;
			
			try {
				while((line = br.readLine()) != null) {
					if (line.startsWith("MSA"))
					{
						String[] params = line.split("\\^");
						msaMessageControlId = params[2];
					}
				}
			}
			catch(IOException e)
			{
				LOG.error("Error getting message control id for {}", message.toString());
			}
		}

		return msaMessageControlId;
	}
	
	public boolean isRunning()
	{
		return listening;
	}

	/**
	 * @param messageId
	 * @param notifyOnResponseReceived
	 */
	public void registerListener(long messageId, HL7ResponseReceiveListener notifyOnResponseReceived)
	{
		if (!listening)
		{
			LOG.error("attempting to register a listener while the service was not running - ignoring!");
		}
		else
		{
			HL7ResponseReceiveListener foo = hl7ResponseListeners.put(messageId, notifyOnResponseReceived);
			if (foo != null)
			{
				LOG.error("Upon registering a listner, we already had a registration for id {} - duplicate ID - design failure!", messageId);
			}
		}
	}
	
	public void launchListener(Task<?> t)
	{
		responseListenerThreads_.execute(t);
	}
}

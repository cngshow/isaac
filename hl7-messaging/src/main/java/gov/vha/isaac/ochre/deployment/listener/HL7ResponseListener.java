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

import java.io.IOException;
import java.lang.ref.WeakReference;
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
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.message.MFN_M01;
import ca.uhn.hl7v2.model.v24.message.MFR_M01;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import ca.uhn.hl7v2.parser.PipeParser;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.deployment.listener.parser.AcknowledgementParser;
import gov.vha.isaac.ochre.deployment.publish.MessageTypeIdentifier;
import gov.vha.isaac.ochre.services.exception.STSException;

@Service
@RunLevel(value = 5)
public class HL7ResponseListener
{
	/** A logger for messages produced by this class. */
	private static Logger LOG = LogManager.getLogger(HL7ResponseListener.class);

	/** A logger for messages inbound hl7 messages. */
	private static Logger HL7LOG = LogManager.getLogger("hl7messages");

	private static Map<SelectionKey, StringBuffer> messageMap = Collections
			.synchronizedMap(new HashMap<SelectionKey, StringBuffer>());

	private static int port;
	private static Selector selector = null;
	private static ServerSocketChannel selectableChannel = null;

	private static final int BUFSIZE = 1024;

	private static final String VETSDATA = "VETS DATA";
	private static final String VETSMD5 = "VETS MD5";
	private static final String VETSUPDATE = "VETS UPDATE";
	private static final int PORT = 49990;

	ConcurrentSkipListSet<WeakReference<HL7ResponseReceiveListener>> hl7ResponseListeners = new ConcurrentSkipListSet<>();

	private int keysAdded = 0;

	/*
	 * for HK2
	 */
	private HL7ResponseListener() {
	}

	@PostConstruct
	private void startMe() {

		LOG.info("Starting ResponseListener pre-construct on port {}.", this.port);

		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					initialize();
					LOG.info("Starting ResponseListener initialize");
					acceptConnections();
					LOG.info("Starting ResponseListener accept connections");
				} catch (IOException e) {
					LOG.error("Error : {}", e.getMessage());
				}
			}
		};

		LookupService.get().getService(WorkExecutors.class).getExecutor().execute(r);

		LOG.info("Started ResponseListener pre-construct on port {}.", this.port);
	}

	@PreDestroy
	private void stopMe() {
		LOG.info("Finished ResponseListener pre-destroy.");
	}

	public void initialize() throws IOException {
		this.selector = SelectorProvider.provider().openSelector();
		this.selectableChannel = ServerSocketChannel.open();
		this.selectableChannel.configureBlocking(false);
		InetAddress localHost = InetAddress.getLocalHost();
		InetSocketAddress isa = new InetSocketAddress(localHost, this.port);

		if (this.selectableChannel.isOpen() == true) {
			this.selectableChannel.socket().setReuseAddress(true);
			this.selectableChannel.socket().bind(isa);
		}
		LOG.info("initialized on port {}", this.port);
	}

	public void acceptConnections() throws IOException {
		SelectionKey acceptKey = null;
		if (selector.isOpen() == true & selectableChannel != null & selectableChannel.isOpen() == true) {
			acceptKey = this.selectableChannel.register(this.selector, SelectionKey.OP_ACCEPT);
		} else {
			return;
		}

		LOG.info("Non-blocking server: acceptor loop...");
		while (selectableChannel.isOpen() == true & selector.isOpen() == true & acceptKey != null
				& (this.keysAdded = acceptKey.selector().select()) > 0) {
			if (selector.isOpen() == false | this.selectableChannel.isOpen() == false) {
				break;
			}
			Set readyKeys = this.selector.selectedKeys();
			Iterator i = readyKeys.iterator();
			while (i.hasNext()) {
				SelectionKey key = (SelectionKey) i.next();
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

		LOG.info("Non-blocking server: end acceptor loop...");
	}

	public String decode(ByteBuffer byteBuffer) throws CharacterCodingException {
		Charset charset = Charset.forName("us-ascii");
		CharsetDecoder decoder = charset.newDecoder();
		CharBuffer charBuffer = decoder.decode(byteBuffer);
		String result = charBuffer.toString();
		return result;
	}

	public void readMessage(ChannelCallback callback, SelectionKey key)
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

	public void writeMessage(ChannelCallback callback, SelectionKey key) throws STSException {

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
				LOG.info("SocketChannel connection closed.  Continuing to listen on port " + port + ".");
			} catch (IOException e) {
				LOG.error("Unable to close listener SocketChannel", e);
			}
		}
	}

	protected void handleResponseNotification(String messageId, Message message) {

		LOG.debug("in handleResponseNotification hl7ResponseListeners count: {}", hl7ResponseListeners.size());
		
		// only send notification to the listener waiting with the same id
		hl7ResponseListeners.forEach((listenerRef) -> {
			if (listenerRef.get().getListenerId() == messageId) {
				HL7ResponseReceiveListener listener = listenerRef.get();
				if (listener == null) {
					hl7ResponseListeners.remove(listenerRef);
				} else {
					LOG.info("send notification");
					listener.handleResponse(message);
					// should the listener be removed when done since there
					// should only be one response?
				}
			}
		});

	}

	//get the id from the message.
	//referred as message control id in hapi
	private String getMessageControlId(Message message) {

		String mshMessageControlId = "";

		if (message instanceof MFR_M01) {
			MFR_M01 mfk = (MFR_M01) message;
			MSH msh = mfk.getMSH();
			mshMessageControlId = msh.getMessageControlID().toString();
			
		} else if (message instanceof MFN_M01) {
			MFN_M01 mfn = (MFN_M01) message;
			MSH msh = mfn.getMSH();
			mshMessageControlId = msh.getMessageControlID().toString();
		}

		return mshMessageControlId;
	}

}

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

package gov.vha.isaac.ochre.access.maint.messaging.hl7.factory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import gov.va.med.term.access.util.Settings;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.GenericParser;
import ca.uhn.hl7v2.util.Terser;
import gov.vha.isaac.ochre.access.maint.messaging.hl7.Encoding;
import gov.vha.isaac.ochre.access.maint.messaging.hl7.MLLP;
import gov.vha.isaac.ochre.access.maint.messaging.hl7.MediaType;
import gov.vha.isaac.ochre.access.maint.messaging.hl7.MessageDispatcher;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;

/**
 * Sends the given HL7 message to BusinessWare for distribution.
 */
public class BusinessWareMessageDispatcher implements MessageDispatcher
{

	/** A logger for messages produced by this class. */
	private static Logger LOG = LogManager.getLogger(BusinessWareMessageDispatcher.class);

	// /** The HL7 encoding to use BusinessWare. */
	// TODO: remove hardcoded.
	private static final Encoding encoding_ = Encoding.valueOf("VB");

	/** The parser to use for encoding messages. */
	private static final GenericParser parser_ = new GenericParser();

	@Override
	public void send(Message message, ApplicationProperties applicationProperties) {
		try {
			LOG.info("Opening connection to {}", applicationProperties.getInterfaceEngineURL().toString());

			URL interfaceURL = new URL(applicationProperties.getInterfaceEngineURL());

			final HttpURLConnection connection = (HttpURLConnection) interfaceURL.openConnection();

			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type",
					((encoding_ == Encoding.xml) ? MediaType.xml.name : MediaType.er7.name) + "; charset=UTF-8");

			// Generate encoded message.

			final String encodedMessage = parser_.encode(message, encoding_.toString());

			// Write XML message.
			try (final OutputStream outputStream = connection.getOutputStream();
					final OutputStreamWriter output = new OutputStreamWriter(outputStream, "UTF-8");) {
				output.write(MLLP.SB);
				output.write(encodedMessage);
				output.write(MLLP.EB);
				output.write(MLLP.CR);
			}

			// Read response
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				final String error = inputStreamToString(connection.getErrorStream());
				final String msg = "Error sending message (" + connection.getResponseCode() + "): message ("
						+ connection.getResponseMessage() + "), error stream (" + error + ")";
				LOG.error(msg);
				throw new RuntimeException(msg);
			}

			final Terser terser = new Terser(message);
			LOG.info("Sent Message: "
					+ terser.get("MSH-9-3") /* msg structure */
					+ "[ctrl_id=" + terser.get("MSH-10") /* control id */
					+ "]: " + encodedMessage.length() + " characters");
		} catch (Exception e) {
			final String msg = "Error sending message.";
			LOG.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	/**
	 * Converts the input stream to a string.
	 *
	 * @param stream
	 *            the stream to convert
	 *
	 * @return A String containing the contents of the input stream.
	 *
	 * @throws IOException
	 *             if an error occurs reading the input stream.
	 */
	private String inputStreamToString(InputStream stream) throws IOException {
		if (stream == null) {
			return "";
		}

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				final StringWriter buffer = new StringWriter();
				final PrintWriter writer = new PrintWriter(buffer);) {
			String line;
			while ((line = reader.readLine()) != null) {
				writer.println(line);
			}
			return buffer.toString();
		}

	}
}

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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.access.maint.deployment.dto.Site;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDTO;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDiscovery;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.deployment.listener.parser.ChecksumVersionParser;
import gov.vha.isaac.ochre.deployment.listener.parser.SiteDiscoveryParser;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.HL7MessageProperties;
import gov.vha.isaac.ochre.services.dto.publish.MessageProperties;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 * Various units tests for HL7 messaging module.
 *
 * {@link TestHL7Messaging}
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public class TestHL7Messaging
{
	private static final Logger LOG = LogManager.getLogger(TestHL7Messaging.class);
	private static ApplicationProperties applicationProperties_ = getDefaultServerProperties();

	@BeforeClass
	public static void initialize() throws Exception {
		HL7Messaging.enableListener(applicationProperties_);
	}

	@AfterClass
	public static void finish() throws Exception {
		LookupService.shutdownSystem();
	}

	@Test
	public void testSiteDiscoveryParser() throws Exception {

		String test = "MSH^~|\\&^VETS UPDATE^660DEV2^XUMF UPDATE^^20080509095700.000-0600^^MFN~M01^^^2.4^^^AL^AL^USA\r"
				+ "MFI^Standard Terminology~~ERT^^MUP^20080509095700.000-0600^20080509095700.000-0600^NE\r"
				+ "MFE^MUP^^^Order Status@4500659\r" + "ZRT^Term^ACTIVE\r" + "ZRT^VistA_Short_Name^actv\r"
				+ "ZRT^VistA_Abbreviation^a\r"
				+ "ZRT^VistA_Description^Orders that are active or have been accepted by the service for processing.  e.g., Dietetic orders are active upon being ordered, Pharmacy orders are active when the order is verified, Lab orders are active when the sample has been collected, Radiology orders are active upon registration.\r"
				+ "ZRT^Status^1\r" + "MFE^MUP^^^Order Status@4501011\r" + "ZRT^Term^CANCELLED\r"
				+ "ZRT^VistA_Short_Name^canc\r" + "ZRT^VistA_Abbreviation^x\r"
				+ "ZRT^VistA_Description^Orders that have been rejected by the ancillary service without being acted on, or terminated while still delayed.\r"
				+ "ZRT^Status^1\r" + "MFE^MUP^^^Order Status@4501088\r" + "ZRT^Term^COMPLETE\r"
				+ "ZRT^VistA_Short_Name^comp\r" + "ZRT^VistA_Abbreviation^c\r"
				+ "ZRT^VistA_Description^Orders that require no further action by the ancillary service.  e.g., Lab orders are completed when results are available, Radiology orders are complete when results are available.\r"
				+ "ZRT^Status^1\r";

		// parse the text into an HL7 message.
		PipeParser parser = new PipeParser();
		Message message = parser.parse(test);

		// parse the HL7 message into SiteDiscovery object
		SiteDiscoveryParser siteDiscoveryParser = new SiteDiscoveryParser();
		SiteDiscovery siteDiscovery = siteDiscoveryParser.parseMessage(message);

		// validate string
		Assert.assertEquals("Order Status", siteDiscovery.getRefset());
		Assert.assertEquals(siteDiscovery.getHeaders().size(), 6);
		Assert.assertEquals(siteDiscovery.getValues().size(), 3); // like rows
																	// of values
		for (ArrayList<String> row : siteDiscovery.getValues()) {
			// each row has 6 values, same as header
			Assert.assertEquals(row.size(), 6);
		}

	}
	
	@Test
	public void testChecksumVersionParser()
	{
		String test = ";CHECKSUM:4bdb6ba422ce11216529bb9b085eb54f;VERSION:25;";
		
		Assert.assertEquals("4bdb6ba422ce11216529bb9b085eb54f", ChecksumVersionParser.getValueFromTokenizedString("CHECKSUM", test));
		Assert.assertEquals("25", ChecksumVersionParser.getValueFromTokenizedString("VERSION", test));
		
	}

	@Test(expected = Exception.class)
	public void testSendChecksumMessageEmpty() throws Throwable {
		// 1. Fail if no message.
		LOG.info("1. Fail if no message.");

		String hl7Message = "";

		Site site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("950");
		site.setGroupName("");
		site.setName("STLVETSDEV");
		site.setType("");
		site.setMessageType("T");

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);

		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.checksum(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	@Test // (expected = Exception.class)
	public void testSendChecksumMessageNoSite() throws Throwable {
		// 2. Fail if no site.
		LOG.info("2. Fail if no site.");

		String hl7Message = "Radiology Procedures";

		Site site = new SiteDTO();

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);

		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.checksum(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());
	}

	@Test
	public void testSendChecksumMessageBadSite() throws Throwable {
		// 3. Fail if site is garbage.
		LOG.info("3. Fail if site is garbage.");

		String hl7Message = "Radiology Procedures";

		Site site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("AA");
		site.setGroupName("");
		site.setName("BB");
		site.setType("");
		site.setMessageType("T");

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);

		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.checksum(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	@Test
	public void testSendChecksumMessageGood() throws Throwable {
		// 4. Success if message and site are OK.
		LOG.info("4. Success if message and site are OK.");

		String hl7Message = "Radiology Procedures";

		Site site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("950");
		site.setGroupName("");
		site.setName("STLVETSDEV");
		site.setType("");
		site.setMessageType("T");

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);

		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.checksum(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	@Test(expected = Exception.class)
	public void testSendSiteDataMessageEmpty() throws Throwable {

		// 1. Fail if no message.
		LOG.info("1. Fail if no message.");

		String hl7Message = "";

		Site site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("950");
		site.setGroupName("");
		site.setName("STLVETSDEV");
		site.setType("");
		site.setMessageType("T");

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);

		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.discovery(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	@Test // (expected = Exception.class)
	public void testSendSiteDataMessageNoSite() throws Throwable {
		// 2. Fail if no site.
		LOG.info("2. Fail if no site.");

		String hl7Message = "Vital Qualifiers";

		Site site = new SiteDTO();

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);

		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.discovery(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());
	}

	@Test
	public void testSendSiteDataMessageBadSite() throws Throwable {
		// 3. Fail if site is garbage.
		LOG.info("3. Fail if site is garbage.");

		String hl7Message = "Vital Qualifiers";

		Site site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("AA");
		site.setGroupName("");
		site.setName("BB");
		site.setType("");
		site.setMessageType("T");

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);

		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.discovery(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	@Test
	public void testSendSiteDataMessageGood() throws Throwable {
		// 4. Success if message and site are OK.
		LOG.info("4. Success if message and site are OK.");

		String hl7Message = "Vital Qualifiers";

		Site site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("950");
		site.setGroupName("");
		site.setName("STLVETSDEV");
		site.setType("");
		site.setMessageType("T");

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);

		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.discovery(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	private static ApplicationProperties getDefaultServerProperties() {

		ApplicationProperties appProp = new HL7ApplicationProperties();

		// Application Server Message String
		appProp.setApplicationServerName("ISAAC");
		appProp.setApplicationVersion("8.4.0.0");

		// Listener Port
		appProp.setListenerPort(49990);

		// Sending Facility Site ID
		appProp.setSendingFacilityNamespaceId("200ET1");

		// Target Vitria Interface Engine
		appProp.setInterfaceEngineURL(
				"http://vaaacvies64.aac.dva.va.gov:8080/FrameworkClient-1.1/Framework2ServletHTTPtoChannel");

		// Encoding type
		appProp.setHl7EncodingType("VB");

		appProp.setUseInterfaceEngine(false);

		return appProp;

	}

	private static MessageProperties getDefaultMessageProperties() {

		MessageProperties messageProperties = new HL7MessageProperties();

		// Settings used by the converter to configure the sending application.
		messageProperties.setSendingApplicationNamespaceIdUpdate("VETS UPDATE");
		messageProperties.setSendingApplicationNamespaceIdMD5("VETS MD5");
		messageProperties.setSendingApplicationNamespaceIdSiteData("VETS DATA");

		// Target Application at VistA sites
		messageProperties.setReceivingApplicationNamespaceIdUpdate("XUMF UPDATE");
		messageProperties.setReceivingApplicationNamespaceIdMD5("XUMFMD5");
		messageProperties.setReceivingApplicationNamespaceIdSiteData("XUMF DATA");

		// Message Version ID
		messageProperties.setVersionId("2.4");

		// acceptAcknowledgementType
		messageProperties.setAcceptAcknowledgementType("AL");
		messageProperties.setApplicationAcknowledgementType("AL");

		messageProperties.setCountryCode("USA");

		// MFI field values
		messageProperties.setMasterFileIdentifier("Standard Terminology");
		messageProperties.setNameOfCodingSystem("ERT");
		messageProperties.setFileLevelEventCode("MUP");
		messageProperties.setResponseLevelCode("NE");

		// MFE field values
		messageProperties.setRecordLevelEventCode("NE");

		// QRD field values
		messageProperties.setQueryFormatCode("R");
		messageProperties.setQueryPriority("I");
		messageProperties.setQueryId("Standard Terminology Query");
		messageProperties.setQueryLimitedRequestQuantity(99999);
		// appProp.setQueryLimitedRequestUnits();

		messageProperties.setQueryWhoSubjectFilterIdNumber("ALL");
		messageProperties.setQueryWhatDepartmentDataCodeIdentifier("VA");

		// CE static field values
		messageProperties.setSubFieldSeparator("@");

		return messageProperties;

	}

	private void taskLog(Task<?> t) {

		t.messageProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				LOG.info("Message: " + newValue);
			}
		});

	}
}

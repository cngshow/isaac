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
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishChecksumMessage;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishChecksumMessageDTO;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishSiteDiscoveryMessage;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishSiteDiscoveryMessageDTO;
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

		// partial response message
		StringBuilder m = new StringBuilder();
		m.append(
				"MSH^~|\\&^XUMF DATA^950^VETS DATA^200ET1^20170310105914-0400^^MFR~M01^65760934422^T^2.4^^^AL^NE^USA")
				.append("\n");
		m.append("MSA^AA^10023").append("\n");
		m.append("QRD^20170310095900.000-0600^R^I^Standard Terminology Query^^^99999~5^ALL^Reactants^VA")
				.append("\n");
		m.append("MFI^Reactants^Standard Terminology^MUP^20170310105906-0400^20170310105906-0400^NE").append("\n");
		m.append("MFE^MUP^^20170310105906-0400^Reactants@4538520").append("\n");
		m.append("ZRT^Term^OTHER ALLERGY/ADVERSE REACTION").append("\n");
		m.append("ZRT^Allergy_Type^OTHER").append("\n");
		m.append("ZRT^Status^0").append("\n");
		m.append("MFE^MUP^^20170310105906-0400^Reactants@4538521").append("\n");
		m.append("ZRT^Term^IODINE").append("\n");
		m.append("ZRT^Allergy_Type^DRUG").append("\n");
		m.append("ZRT^has_drug_class^DE101").append("\n");
		m.append("ZRT^has_drug_class^DX101").append("\n");
		m.append("ZRT^has_drug_class^PH000").append("\n");
		m.append("ZRT^has_drug_ingredient^IODINE").append("\n");
		m.append("ZRT^Status^0").append("\n");
		m.append("MFE^MUP^^20170310105906-0400^Reactants@4538522").append("\n");
		m.append("ZRT^Term^IRON FILLINGS").append("\n");
		m.append("ZRT^Allergy_Type^OTHER").append("\n");
		m.append("ZRT^Status^0").append("\n");
		m.append("MFE^MUP^^20170310105906-0400^Reactants@4538524").append("\n");
		m.append("ZRT^Term^RED FOOD DYE").append("\n");
		m.append("ZRT^Allergy_Type^DRUG, FOOD").append("\n");
		m.append("ZRT^has_drug_ingredient^F D \\T\\ C RED #3").append("\n");
		m.append("ZRT^has_drug_ingredient^F D \\T\\ C RED #40").append("\n");
		m.append("ZRT^has_drug_ingredient^F D \\T\\ C RED #40 LAKE").append("\n");
		m.append("ZRT^Status^0").append("\n");
		m.append("MFE^MUP^^20170310105906-0400^Reactants@4538526").append("\n");
		m.append("ZRT^Term^ANTIRABIES SERUM").append("\n");
		m.append("ZRT^Allergy_Type^DRUG, FOOD").append("\n");
		m.append("ZRT^has_drug_class^IM400").append("\n");
		m.append("ZRT^has_drug_ingredient^ANTIRABIES SERUM").append("\n");
		m.append("ZRT^Status^0").append("\n");

		// parse the text into an HL7 message.
		PipeParser parser = new PipeParser();
		Message message = parser.parse(m.toString());

		// parse the HL7 message into SiteDiscovery object
		SiteDiscoveryParser siteDiscoveryParser = new SiteDiscoveryParser();
		SiteDiscovery siteDiscovery = siteDiscoveryParser.parseMessage(message);

		// validate string
		Assert.assertEquals("Reactants", siteDiscovery.getRefset());
		Assert.assertEquals(siteDiscovery.getHeaders().size(), 6);
		// like rows of values
		Assert.assertEquals(siteDiscovery.getValues().size(), 5);

	}

	@Test
	public void testChecksumVersionParser() {
		String test = ";CHECKSUM:4bdb6ba422ce11216529bb9b085eb54f;VERSION:25;";

		Assert.assertEquals("4bdb6ba422ce11216529bb9b085eb54f",
				ChecksumVersionParser.getValueFromTokenizedString("CHECKSUM", test));
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

		PublishChecksumMessage publishMessage = new PublishChecksumMessageDTO(1, site, hl7Message);

		List<PublishChecksumMessage> publishMessages = new ArrayList<PublishChecksumMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.checksum(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	@Test (expected = Exception.class)
	public void testSendChecksumMessageNoSite() throws Throwable {
		// 2. Fail if no site.
		LOG.info("2. Fail if no site.");

		String hl7Message = "Radiology Procedures";

		Site site = new SiteDTO();

		PublishChecksumMessage publishMessage = new PublishChecksumMessageDTO(2, site, hl7Message);

		List<PublishChecksumMessage> publishMessages = new ArrayList<PublishChecksumMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.checksum(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());
	}

	// @Test
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

		PublishChecksumMessage publishMessage = new PublishChecksumMessageDTO(3, site, hl7Message);

		List<PublishChecksumMessage> publishMessages = new ArrayList<PublishChecksumMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.checksum(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	// @Test
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

		PublishChecksumMessage publishMessage = new PublishChecksumMessageDTO(4, site, hl7Message);

		List<PublishChecksumMessage> publishMessages = new ArrayList<PublishChecksumMessage>();
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

		PublishSiteDiscoveryMessage publishMessage = new PublishSiteDiscoveryMessageDTO(5, site, hl7Message);

		List<PublishSiteDiscoveryMessage> publishMessages = new ArrayList<PublishSiteDiscoveryMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.discovery(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	@Test (expected = Exception.class)
	public void testSendSiteDataMessageNoSite() throws Throwable {
		// 2. Fail if no site.
		LOG.info("2. Fail if no site.");

		String hl7Message = "Vital Qualifiers";

		Site site = new SiteDTO();

		PublishSiteDiscoveryMessage publishMessage = new PublishSiteDiscoveryMessageDTO(6, site, hl7Message);

		List<PublishSiteDiscoveryMessage> publishMessages = new ArrayList<PublishSiteDiscoveryMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.discovery(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());
	}

	// @Test
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

		PublishSiteDiscoveryMessage publishMessage = new PublishSiteDiscoveryMessageDTO(7, site, hl7Message);

		List<PublishSiteDiscoveryMessage> publishMessages = new ArrayList<PublishSiteDiscoveryMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Messaging.discovery(publishMessages, getDefaultMessageProperties()).get(0);
		taskLog(t);
		// unit test does not send message to server, the result is null.
		Assert.assertNull(t.get());

	}

	// @Test
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

		PublishSiteDiscoveryMessage publishMessage = new PublishSiteDiscoveryMessageDTO(9, site, hl7Message);

		List<PublishSiteDiscoveryMessage> publishMessages = new ArrayList<PublishSiteDiscoveryMessage>();
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

		t.messageProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				LOG.info("Message: " + newValue);
			}
		});

	}
}

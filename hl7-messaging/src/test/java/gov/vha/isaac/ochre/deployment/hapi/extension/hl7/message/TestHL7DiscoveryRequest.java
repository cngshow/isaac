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

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessage;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.access.maint.deployment.dto.Site;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDTO;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.HL7MessageProperties;
import gov.vha.isaac.ochre.services.dto.publish.MessageProperties;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 *
 *
 * {@link TestHL7DiscoveryRequest}
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public class TestHL7DiscoveryRequest
{
	private static Logger LOG = LogManager.getLogger(TestHL7DiscoveryRequest.class);

	//@Test(expected = Exception.class)
	public void testSendMessageEmpty() throws Throwable {

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

		Task<Void> t = HL7Discovery.discovery(publishMessages, getDefaultServerProperties(), getDefaultMessageProperties()).get(0);
		taskLog(t);
		LOG.info("Result {}", t.get());

	}

	//@Test(expected = Exception.class)
	public void testSendMessageNoSite() throws Throwable {
		// 2. Fail if no site.
		LOG.info("2. Fail if no site.");

		String hl7Message = "Vital Qualifiers";

		Site site = new SiteDTO();

		PublishMessage publishMessage = new PublishMessageDTO(1, site, hl7Message);
		
		List<PublishMessage> publishMessages = new ArrayList<PublishMessage>();
		publishMessages.add(publishMessage);

		Task<Void> t = HL7Discovery.discovery(publishMessages, getDefaultServerProperties(), getDefaultMessageProperties()).get(0);
		taskLog(t);
		LOG.info("Result {}", t.get());
	}

	// TODO: fix, this test will hang on task
	// @Test
	public void testSendMessageBadSite() throws Throwable {
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

		Task<Void> t = HL7Discovery.discovery(publishMessages, getDefaultServerProperties(), getDefaultMessageProperties()).get(0);
		taskLog(t);
		LOG.info("Result {}", t.get());

	}

	// TODO: fix, this test will hang on task
	// @Test
	public void testSendMessageGood() throws Throwable {
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

		Task<Void> t = HL7Discovery.discovery(publishMessages, getDefaultServerProperties(),getDefaultMessageProperties()).get(0);
		taskLog(t);
		LOG.info("Result {}", t.get());

	}

	private static ApplicationProperties getDefaultServerProperties() {

		ApplicationProperties appProp = new HL7ApplicationProperties();

		// Application Server Message String
		appProp.setApplicationServerName("Development");
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

		appProp.setUseInterfaceEngine(true);

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

	private void taskLog(Task t) {

		t.progressProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				LOG.info("[Change] Progress " + newValue);
			}
		});
		t.messageProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				LOG.info("[Change] Message " + newValue);
			}
		});
		t.titleProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				LOG.info("[Change] Title " + newValue);
			}
		});

	}
}

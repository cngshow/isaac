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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDTO;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 *
 *
 * {@link TestHL7CheckSumRequest}
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public class TestHL7CheckSumRequest {

	private static Logger LOG = LogManager.getLogger(HL7Sender.class.getPackage().getName());

	@Test(expected=Exception.class)
	public void testSendMessageEmpty() throws Throwable {
		//1. Fail if no message.
		LOG.info("1. Fail if no message.");

		String hl7Message = "";
		
		SiteDTO site;
		site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("582");
		site.setGroupName("");
		site.setName("STLVETSDEV");
		site.setType("");
		site.setMessageType("T");

		PublishMessageDTO publishMessage;
		publishMessage = new PublishMessageDTO();
		publishMessage.setMessageId(1);
		publishMessage.setSite(site);

		List<PublishMessageDTO> siteList = new ArrayList<>();
		siteList.clear();
		siteList.add(publishMessage);

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList, getDefaultServerConfig());
		taskLog(t);
		LOG.info(": Result " + t.get());

	}

	@Test(expected=Exception.class)
	public void testSendMessageNoSite() throws Throwable {
		//2. Fail if no site.
		LOG.info("2. Fail if no site.");

		String hl7Message = "Radiology Procedures";
		
		SiteDTO site;
		PublishMessageDTO publishMessage;
		List<PublishMessageDTO> siteList = new ArrayList<>();

		siteList.clear();

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList, getDefaultServerConfig());
		taskLog(t);
		LOG.info(": Result " + t.get());
	}

	//TODO: fix, this test will hang on task
	//@Test
	public void testSendMessageBadSite() throws Throwable
	{
		//3. Fail if site is garbage.
		LOG.info("3. Fail if site is garbage.");

		String hl7Message = "Radiology Procedures";

		SiteDTO site;
		site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("AA");
		site.setGroupName("BB");
		site.setName("test site");
		site.setType("test");

		PublishMessageDTO publishMessage;
		publishMessage = new PublishMessageDTO();
		publishMessage.setMessageId(1);
		publishMessage.setSite(site);

		List<PublishMessageDTO> siteList = new ArrayList<>();
		siteList.clear();
		siteList.add(publishMessage);

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList, getDefaultServerConfig());
		taskLog(t);
		LOG.info(": Result " + t.get());

	}

	//TODO: fix, this test will hang on task
	//@Test
	public void testSendMessageGood() throws Throwable
	{
		//4. Success if message and site are OK.
		LOG.info("4. Success if message and site are OK.");

		String hl7Message = "Radiology Procedures";
		
		SiteDTO site;
		site = new SiteDTO();
		site.setId(1);
		site.setVaSiteId("582");
		site.setGroupName("");
		site.setName("STLVETSDEV");
		site.setType("");
		site.setMessageType("T");

		PublishMessageDTO publishMessage;
		publishMessage = new PublishMessageDTO();
		publishMessage.setMessageId(1);
		publishMessage.setSite(site);

		List<PublishMessageDTO> siteList = new ArrayList<>();
		siteList.clear();
		siteList.add(publishMessage);

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList, getDefaultServerConfig());
		taskLog(t);
		LOG.info(": Result " + t.get());

	}
	
	private static HL7ApplicationProperties getDefaultServerConfig() throws MalformedURLException {
		
		HL7ApplicationProperties appProp = new HL7ApplicationProperties();

		// Application Server Message String
		appProp.setApplicationServerName("Development");
		appProp.setApplicationVersion("8.4.0.0");

		// Listener Port
		appProp.setListenerPort(49990);

		// Sending Facility Site ID
		appProp.setSendingFacilityNamespaceId("660VM2");

		// Target Vitria Interface Engine
		appProp.setInterfaceEngineURL(new URL("http://vhaisfviev24:8080/FrameworkClient-1.1/Framework2ServletHTTPtoChannel"));

		// Encoding type
		appProp.setHl7EncodingType("VB");
		
		appProp.setEnvironment("");

		// Settings used by the converter to configure the sending application.
		appProp.setSendingApplicationNamespaceIdUpdate("VETS UPDATE");
		appProp.setSendingApplicationNamespaceIdMD5("VETS MD5");
		appProp.setSendingApplicationNamespaceIdSiteData("VETS DATA");

		// Target Application at VistA sites
		appProp.setReceivingApplicationNamespaceIdUpdate("XUMF UPDATE");
		appProp.setReceivingApplicationNamespaceIdMD5("XUMFMD5");
		appProp.setReceivingApplicationNamespaceIdSiteData("XUMF DATA");

		// Message Version ID
		appProp.setVersionId("2.4");

		// acceptAcknowledgementType
		appProp.setAcceptAcknowledgementType("AL");
		appProp.setApplicationAcknowledgementType("AL");

		appProp.setCountryCode("USA");

		// MFI field values
		appProp.setMasterFileIdentifier("Standard Terminology");
		appProp.setNameOfCodingSystem("ERT");
		appProp.setFileLevelEventCode("MUP");
		appProp.setResponseLevelCode("NE");

		// MFE field values
		appProp.setRecordLevelEventCode("NE");

		// QRD field values
		appProp.setQueryFormatCode("R");
		appProp.setQueryPriority("I");
		appProp.setQueryId("Standard Terminology Query");
		appProp.setQueryLimitedRequestQuantity(99999);
		//appProp.setQueryLimitedRequestUnits();

		appProp.setQueryWhoSubjectFilterIdNumber("ALL");
		appProp.setQueryWhatDepartmentDataCodeIdentifier("VA");

		// CE static field values
		appProp.setSubFieldSeparator("@");
		appProp.setUseInterfaceEngine(true);

		return appProp;

	}
	
	private void taskLog(Task t) {

		t.progressProperty().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				LOG.info("[Change] Progress " + newValue);
			}
		});
		t.messageProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				LOG.info("[Change] Message " + newValue);
			}
		});
		t.titleProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				LOG.info("[Change] Title " + newValue);
			}
		});

	}
}

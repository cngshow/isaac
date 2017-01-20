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
package gov.vha.isaac.ochre;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.deployment.hapi.extension.hl7.message.HL7CheckSum;
import gov.vha.isaac.ochre.deployment.model.Site;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 *
 *
 * {@link HL7CheckSumRequestTest}
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public class HL7CheckSumRequestTest {

	private static Logger log = LogManager.getLogger(HL7Sender.class.getPackage().getName());

	@Test(expected=Exception.class)
	public void testSendMessageEmpty() throws Throwable {
		//1. Fail if no message.
		System.out.println("1. Fail if no message.");

		String hl7Message = "";
		Site site;
		site = new Site();
		site.setId(1L);
		site.setVaSiteId("");
		site.setGroupName("");
		site.setName("");
		site.setType("");

		PublishMessageDTO publishMessage;
		publishMessage = new PublishMessageDTO();
		publishMessage.setMessageId(1);
		publishMessage.setSite(site);

		List<PublishMessageDTO> siteList = new ArrayList<>();
		siteList.clear();
		siteList.add(publishMessage);

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList);
		taskLog(t);
		System.out.println(": Result " + t.get());
		if (t != null) {
			t = null;
		}
	}

	//TODO: fix, this test will hang on task
	//@Test //(expected=Exception.class)
	public void testSendMessageGarbage() throws Throwable {
		//2. Fail if message is garbage.
		System.out.println("2. Fail if message is garbage.");

		String hl7Message = "alsjfkasjdfkjas;djfasd;kjfsd;ajf";

		Site site;
		site = new Site();
		site.setId(1L);
		site.setVaSiteId("");
		site.setGroupName("");
		site.setName("");
		site.setType("");

		PublishMessageDTO publishMessage;
		publishMessage = new PublishMessageDTO();
		publishMessage.setMessageId(1);
		publishMessage.setSite(site);

		List<PublishMessageDTO> siteList = new ArrayList<>();
		siteList.clear();
		siteList.add(publishMessage);

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList);
		taskLog(t);
		System.out.println(": Result " + t.get());
		if (t != null) {
			t = null;
		}
	}

	@Test(expected=Exception.class)
	public void testSendMessageNoSite() throws Throwable {
		//3. Fail if no site.
		System.out.println("3. Fail if no site.");

		String hl7Message = "MSH^~|\\&^VETS MD5^660VM13^XUMF MD5^950^20170104061200.000-0700^^MFQ~M01^70470^T^2.4^^^AL^AL^USA QRD^20170104061200.000-0700^R^I^Standard Terminology Query^^^99999^ALL^Radiology Procedures^VA";
		Site site;
		PublishMessageDTO publishMessage;
		List<PublishMessageDTO> siteList = new ArrayList<>();

		siteList.clear();

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList);
		taskLog(t);
		System.out.println(": Result " + t.get());
		if (t != null) {
			t = null;
		}
	}

	//TODO: fix, this test will hang on task
	//@Test
	public void testSendMessageBadSite() throws Throwable
	{
		//4. Fail if site is garbage.
		System.out.println("4. Fail if site is garbage.");

		String hl7Message = "MSH^~|\\&^VETS MD5^660VM13^XUMF MD5^950^20170104061200.000-0700^^MFQ~M01^70470^T^2.4^^^AL^AL^USA QRD^20170104061200.000-0700^R^I^Standard Terminology Query^^^99999^ALL^Radiology Procedures^VA";


		Site site;
		site = new Site();
		site.setId(1L);
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

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList);
		taskLog(t);
		System.out.println(": Result " + t.get());
		if (t != null) {
			t = null;
		}
	}

	//TODO: fix, this test will hang on task
	//@Test
	public void testSendMessageGood() throws Throwable
	{
		//5. Success if message and site are OK.
		System.out.println("5. Success if message and site are OK.");

		String hl7Message = "MSH^~|\\&^VETS MD5^660VM13^XUMF MD5^950^20170104061200.000-0700^^MFQ~M01^70470^T^2.4^^^AL^AL^USA QRD^20170104061200.000-0700^R^I^Standard Terminology Query^^^99999^ALL^Radiology Procedures^VA";

		//Need details of test site.
		Site site;
		site = new Site();
		site.setId(1L);
		site.setVaSiteId("");
		site.setGroupName("");
		site.setName("");
		site.setType("");

		PublishMessageDTO publishMessage;
		publishMessage = new PublishMessageDTO();
		publishMessage.setMessageId(1);
		publishMessage.setSite(site);

		List<PublishMessageDTO> siteList = new ArrayList<>();
		siteList.clear();
		siteList.add(publishMessage);

		Task<String> t = HL7CheckSum.checkSum(hl7Message, siteList);
		taskLog(t);
		System.out.println(": Result " + t.get());
		if (t != null) {
			t = null;
		}

	}

	private void taskLog(Task t) {

		t.progressProperty().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				System.out.println("[Change] Progress " + newValue);
			}
		});
		t.messageProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				System.out.println("[Change] Message " + newValue);
			}
		});
		t.titleProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				System.out.println("[Change] Title " + newValue);
			}
		});

	}
}

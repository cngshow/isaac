package gov.vha.isaac.ochre.deployment.hapi.extension.hl7.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.deployment.listener.ResponseListener;
import gov.vha.isaac.ochre.deployment.model.Site;
import gov.vha.isaac.ochre.deployment.publish.ApplicationPropertyReader;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;

public class TestHL7CheckSum
{
	private static final Logger LOG = LogManager.getLogger();
	
	public static void main(String[] args) throws Throwable
	{
 
		try
		{
			//Launch listener before sending message.
			ResponseListener listener;
			int port = Integer.parseInt(ApplicationPropertyReader.getApplicationProperty("listenerPort"));
			listener = new ResponseListener(port);
			listener.start();
			
			LOG.info("Begin");
			
			//HAPI version 2.4
			//String hl7Message = "MSH^~|\\&^VETS MD5^660VM13^XUMF MD5^950^20170104061200.000-0700^^MFQ~M01^70470^T^2.4^^^AL^AL^USA" + (char)13 
			//		+ "QRD^20170104061200.000-0700^R^I^Standard Terminology Query^^^99999^ALL^Radiology Procedures^VA";
			
			//HAPI version 2.6 
			String hl7Message = "MSH^~|\\&^VETS MD5^660VM13^XUMF MD5^950^20170104061200.000-0700^^MFQ~M01^70470^T^2.6^^^AL^AL^USA" + (char)13 
					+ "QRD^20170104061200.000-0700^R^I^Standard Terminology Query^^^99999^ALL^Radiology Procedures^VA";
			
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
			
			HL7Sender sender = new HL7Sender(hl7Message, siteList);
			sender.send(hl7Message, siteList);
			
			//Wait for 1 minute
			Thread.sleep(60 * 1000);
			
			LOG.info("End");
			System.exit(0);
		}
		catch(Exception e)
		{
			LOG.info("Error - Ending");
			System.exit(-1);
		}
	}

}

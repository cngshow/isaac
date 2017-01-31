package gov.vha.isaac.ochre.deployment.hapi.extension.hl7.message;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDTO;
import gov.vha.isaac.ochre.deployment.listener.ResponseListener;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;


public class TestHL7Discovery
{
	private static final Logger LOG = LogManager.getLogger();
	private static HL7ApplicationProperties applProps;
	
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
			
			//replace with Discovery message
			
			//HAPI version 2.4
			//String hl7Message = "MSH^~|\\&^VETS MD5^660VM13^XUMF MD5^950^20170104061200.000-0700^^MFQ~M01^70470^T^2.4^^^AL^AL^USA" + (char)13 
			//		+ "QRD^20170104061200.000-0700^R^I^Standard Terminology Query^^^99999^ALL^Radiology Procedures^VA";
			
			//HAPI version 2.6 
			String hl7Message = "MSH^~|\\&^VETS MD5^660VM13^XUMF MD5^950^20170104061200.000-0700^^MFQ~M01^70470^T^2.6^^^AL^AL^USA" + (char)13 
					+ "QRD^20170104061200.000-0700^R^I^Standard Terminology Query^^^99999^ALL^Radiology Procedures^VA";
			
			//Need details of test site.
			SiteDTO site;
			site = new SiteDTO();
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
			
			HL7Sender sender = new HL7Sender(hl7Message, siteList, applProps);
			sender.send(hl7Message, siteList, applProps);
			
			//Wait for 5 minutes
			Thread.sleep(5 * 60 * 1000);
			
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

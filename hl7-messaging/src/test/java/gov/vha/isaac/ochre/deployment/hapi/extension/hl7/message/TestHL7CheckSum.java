package gov.vha.isaac.ochre.deployment.hapi.extension.hl7.message;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDTO;
import gov.vha.isaac.ochre.deployment.listener.ResponseListener;
import gov.vha.isaac.ochre.deployment.publish.HL7RequestGenerator;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;

public class TestHL7CheckSum
{
	private static final Logger LOG = LogManager.getLogger(TestHL7CheckSum.class);

	public static void main(String[] args) throws Throwable {
		int timeToWaitForShutdown = 60 * 1000;
		try {
			// time to wait in seconds before shutdown
			if (args.length > 0) {
				try {
					if (Integer.parseInt(args[0]) > 0) {
						timeToWaitForShutdown = Integer.parseInt(args[0]) * 1000;
					}
				} catch (Exception e) {
					System.out.println("Parameter must be an number.");
				}
			}

			HL7ApplicationProperties serverConfig = getDefaultServerConfigFromPropteriesFile();

			// Launch listener before sending message.
			ResponseListener listener;
			listener = new ResponseListener(serverConfig.getListenerPort());
			listener.start();

			LOG.info("Begin");

			String hl7Message = HL7RequestGenerator.getChecksumRequestMessage("Radiology Procedures", serverConfig);
			
			LOG.info("MESSAGE: {}", hl7Message);
			
			// Add site from properties file.
			SiteDTO site;
			site = new SiteDTO();
			site.setId(Long.parseLong(getPropValue("test.site.id")));
			site.setVaSiteId(getPropValue("test.site.vaSiteId"));
			site.setGroupName(getPropValue("test.site.groupName"));
			site.setName(getPropValue("test.site.name"));
			site.setType(getPropValue("test.site.type"));
			site.setMessageType(getPropValue("test.site.message.type"));

			PublishMessageDTO publishMessage;
			publishMessage = new PublishMessageDTO();

			publishMessage.setMessageId(System.currentTimeMillis());
			publishMessage.setSite(site);

			List<PublishMessageDTO> siteList = new ArrayList<>();
			siteList.clear();
			siteList.add(publishMessage);

			HL7Sender sender = new HL7Sender(hl7Message, siteList, serverConfig);
			sender.send(hl7Message, siteList, serverConfig);

			// Wait for before shutdown
			Thread.sleep(timeToWaitForShutdown);

			LOG.info("End");
			System.exit(0);

		} catch (Exception e) {

			LOG.error("Error - Ending");
			LOG.error(e.getMessage());
			System.exit(-1);
		}
	}

	private static HL7ApplicationProperties getDefaultServerConfigFromPropteriesFile() throws MalformedURLException {
		HL7ApplicationProperties appProp = new HL7ApplicationProperties();
		// Application Server Message String
		appProp.setApplicationServerName(getPropValue("application.server.name"));

		appProp.setApplicationVersion(getPropValue("application.version"));

		// Listener Port
		String listenerPort = getPropValue("listenerPort");
		if (listenerPort != null && StringUtils.isNotBlank(listenerPort)) {
			appProp.setListenerPort(Integer.parseInt(listenerPort));
		}

		// Sending Facility Site ID
		appProp.setSendingFacilityNamespaceId(getPropValue("msh.sendingFacility.namespaceId"));

		// Target Vitria Interface Engine
		String interfaceEngineURL = getPropValue(
				"gov.vha.isaac.orche.term.access.maint.messaging.hl7.factory.BusinessWareMessageDispatcher/url");
		if (interfaceEngineURL != null && StringUtils.isNotBlank(interfaceEngineURL)) {
			appProp.setInterfaceEngineURL(new URL(interfaceEngineURL));
		}

		// Encoding type
		appProp.setHl7EncodingType(getPropValue(
				"gov.vha.isaac.orche.term.access.maint.messaging.hl7.factory.BusinessWareMessageDispatcher/encoding"));
		
		appProp.setEnvironment("");

		// Settings used by the converter to configure the sending application.
		appProp.setSendingApplicationNamespaceIdUpdate(getPropValue("msh.sendingApplication.namespaceId.update"));
		appProp.setSendingApplicationNamespaceIdMD5(getPropValue("msh.sendingApplication.namespaceId.md5"));
		appProp.setSendingApplicationNamespaceIdSiteData(getPropValue("msh.sendingApplication.namespaceId.siteData"));

		// Target Application at VistA sites
		appProp.setReceivingApplicationNamespaceIdUpdate(getPropValue("msh.receivingApplication.namespaceId.update"));
		appProp.setReceivingApplicationNamespaceIdMD5(getPropValue("msh.receivingApplication.namespaceId.md5"));
		appProp.setReceivingApplicationNamespaceIdSiteData(
				getPropValue("msh.receivingApplication.namespaceId.siteData"));

		// Message Version ID
		appProp.setVersionId(getPropValue("msh.versionId"));

		// acceptAcknowledgementType
		appProp.setAcceptAcknowledgementType(getPropValue("msh.acceptAcknowledgementType"));
		appProp.setApplicationAcknowledgementType(getPropValue("msh.applicationAcknowledgementType"));

		appProp.setCountryCode(getPropValue("msh.countryCode"));

		// MFI field values
		appProp.setMasterFileIdentifier(getPropValue("mfi.masterFileIdentifier"));
		appProp.setNameOfCodingSystem(getPropValue("mfi.nameOfCodingSystem"));
		appProp.setFileLevelEventCode(getPropValue("mfi.fileLevelEventCode"));
		appProp.setResponseLevelCode(getPropValue("mfi.responseLevelCode"));

		// MFE field values
		appProp.setRecordLevelEventCode(getPropValue("mfe.recordLevelEventCode"));

		// QRD field values
		appProp.setQueryFormatCode(getPropValue("qrd.queryFormatCode"));
		appProp.setQueryPriority(getPropValue("qrd.queryPriority"));
		appProp.setQueryId(getPropValue("qrd.queryId"));

		String quantity = getPropValue("qrd.quantityLimitedRequest.quantity");
		if (quantity != null && StringUtils.isNotBlank(quantity)) {
			appProp.setQueryLimitedRequestQuantity(Integer.parseInt(quantity));
		}

		String units = getPropValue("qrd.quantityLimitedRequest.units");
		if (units != null && StringUtils.isNotBlank(units)) {
			appProp.setQueryLimitedRequestUnits(Integer.parseInt(units));
		}

		appProp.setQueryWhoSubjectFilterIdNumber(getPropValue("qrd.whoSubjectFilter.idNumber"));
		appProp.setQueryWhatDepartmentDataCodeIdentifier(getPropValue("qrd.whatDepartmentDataCode.identifier"));

		// CE static field values
		appProp.setSubFieldSeparator(getPropValue("ce.subFieldSeparator"));
		
		String useIE = getPropValue("useInterfaceEngine");
		boolean ieUsage = false;
		
		if ("true".equalsIgnoreCase(useIE) || "false".equalsIgnoreCase(useIE)) {
			useIE.toLowerCase();
			ieUsage = Boolean.valueOf(useIE).booleanValue();
		}
		appProp.setUseInterfaceEngine(ieUsage);

		return appProp;

	}

	private static String getPropValue(String key) {

		if (key != null) {
			return ApplicationPropertyReader.getApplicationProperty(key);
		} else {
			return null;
		}
	}

}

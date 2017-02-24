package gov.vha.isaac.ochre.deployment.hapi.extension.hl7.message;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.model.Message;
import gov.vha.isaac.ochre.access.maint.deployment.dto.PublishMessageDTO;
import gov.vha.isaac.ochre.access.maint.deployment.dto.Site;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDTO;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.deployment.publish.HL7RequestGenerator;
import gov.vha.isaac.ochre.deployment.publish.HL7Sender;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.HL7MessageProperties;
import gov.vha.isaac.ochre.services.dto.publish.MessageProperties;

public class TestHL7Discovery
{
	private static final Logger LOG = LogManager.getLogger(TestHL7Discovery.class);

	public static void main(String[] args) throws Throwable {
		String subset = getPropValue("test.discovery.name");
	
		try {
			ApplicationProperties applicationProperties = getDefaultServerPropertiesFromFile();
			MessageProperties messageProperties = getDefaultMessagePropertiesFromFile();
			
			HL7Messaging.enableListener(applicationProperties);

			LOG.info("Begin - get site data for: {}", subset);

			String hl7Message = HL7RequestGenerator.getSiteDataRequestMessage(subset, 
					applicationProperties, messageProperties);

			LOG.info("MESSAGE: {}", hl7Message);

			// Add site from properties file.
			Site site;
			site = new SiteDTO();
			site.setId(Long.parseLong(getPropValue("test.site.id")));
			site.setVaSiteId(getPropValue("test.site.vaSiteId"));
			site.setGroupName(getPropValue("test.site.groupName"));
			site.setName(getPropValue("test.site.name"));
			site.setType(getPropValue("test.site.type"));
			site.setMessageType(getPropValue("test.site.message.type"));

			PublishMessageDTO publishMessage = new PublishMessageDTO(System.currentTimeMillis(), site, subset);

			HL7Sender sender = new HL7Sender(hl7Message, publishMessage, applicationProperties, messageProperties);
			VistaRequestResponseHandler vrrh = new VistaRequestResponseHandler();
			sender.send(vrrh);
			
			System.out.println("Waiting for response");
			Message result = vrrh.waitForResponse();

			System.out.println(result == null ? "null" : result.printStructure());

			LOG.info("End");
			LookupService.shutdownSystem();
			System.exit(0);

		} catch (Exception e) {

			LOG.error("Error - Ending");
			LOG.error(e.getMessage());
			System.exit(-1);
		}
	}

	private static ApplicationProperties getDefaultServerPropertiesFromFile() {
		ApplicationProperties appProp = new HL7ApplicationProperties();
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
				appProp.setInterfaceEngineURL(getPropValue(
						"gov.vha.isaac.orche.term.access.maint.messaging.hl7.factory.BusinessWareMessageDispatcher/url"));

		// Encoding type
		appProp.setHl7EncodingType(getPropValue(
				"gov.vha.isaac.orche.term.access.maint.messaging.hl7.factory.BusinessWareMessageDispatcher/encoding"));

		String useIE = getPropValue("useInterfaceEngine");
		boolean ieUsage = false;

		if ("true".equalsIgnoreCase(useIE) || "false".equalsIgnoreCase(useIE)) {
			useIE.toLowerCase();
			ieUsage = Boolean.valueOf(useIE).booleanValue();
		}
		appProp.setUseInterfaceEngine(ieUsage);

		return appProp;

	}

	private static MessageProperties getDefaultMessagePropertiesFromFile() {
		MessageProperties messageProperties = new HL7MessageProperties();

		// Settings used by the converter to configure the sending application.
		messageProperties
				.setSendingApplicationNamespaceIdUpdate(getPropValue("msh.sendingApplication.namespaceId.update"));
		messageProperties.setSendingApplicationNamespaceIdMD5(getPropValue("msh.sendingApplication.namespaceId.md5"));
		messageProperties
				.setSendingApplicationNamespaceIdSiteData(getPropValue("msh.sendingApplication.namespaceId.siteData"));

		// Target Application at VistA sites
		messageProperties
				.setReceivingApplicationNamespaceIdUpdate(getPropValue("msh.receivingApplication.namespaceId.update"));
		messageProperties
				.setReceivingApplicationNamespaceIdMD5(getPropValue("msh.receivingApplication.namespaceId.md5"));
		messageProperties.setReceivingApplicationNamespaceIdSiteData(
				getPropValue("msh.receivingApplication.namespaceId.siteData"));

		// Message Version ID
		messageProperties.setVersionId(getPropValue("msh.versionId"));

		// acceptAcknowledgementType
		messageProperties.setAcceptAcknowledgementType(getPropValue("msh.acceptAcknowledgementType"));
		messageProperties.setApplicationAcknowledgementType(getPropValue("msh.applicationAcknowledgementType"));

		messageProperties.setCountryCode(getPropValue("msh.countryCode"));

		// MFI field values
		messageProperties.setMasterFileIdentifier(getPropValue("mfi.masterFileIdentifier"));
		messageProperties.setNameOfCodingSystem(getPropValue("mfi.nameOfCodingSystem"));
		messageProperties.setFileLevelEventCode(getPropValue("mfi.fileLevelEventCode"));
		messageProperties.setResponseLevelCode(getPropValue("mfi.responseLevelCode"));

		// MFE field values
		messageProperties.setRecordLevelEventCode(getPropValue("mfe.recordLevelEventCode"));

		// QRD field values
		messageProperties.setQueryFormatCode(getPropValue("qrd.queryFormatCode"));
		messageProperties.setQueryPriority(getPropValue("qrd.queryPriority"));
		messageProperties.setQueryId(getPropValue("qrd.queryId"));

		String quantity = getPropValue("qrd.quantityLimitedRequest.quantity");
		if (quantity != null && StringUtils.isNotBlank(quantity)) {
			messageProperties.setQueryLimitedRequestQuantity(Integer.parseInt(quantity));
		}

		String units = getPropValue("qrd.quantityLimitedRequest.units");
		if (units != null && StringUtils.isNotBlank(units)) {
			messageProperties.setQueryLimitedRequestUnits(Integer.parseInt(units));
		}

		messageProperties.setQueryWhoSubjectFilterIdNumber(getPropValue("qrd.whoSubjectFilter.idNumber"));
		messageProperties
				.setQueryWhatDepartmentDataCodeIdentifier(getPropValue("qrd.whatDepartmentDataCode.identifier"));

		// CE static field values
		messageProperties.setSubFieldSeparator(getPropValue("ce.subFieldSeparator"));

		String useIE = getPropValue("useInterfaceEngine");
		boolean ieUsage = false;

		if ("true".equalsIgnoreCase(useIE) || "false".equalsIgnoreCase(useIE)) {
			useIE.toLowerCase();
			ieUsage = Boolean.valueOf(useIE).booleanValue();
		}

		return messageProperties;

	}

	private static String getPropValue(String key) {

		if (key != null) {
			return ApplicationPropertyReader.getApplicationProperty(key);
		} else {
			return null;
		}
	}

}

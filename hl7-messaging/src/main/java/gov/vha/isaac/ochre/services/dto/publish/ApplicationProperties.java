package gov.vha.isaac.ochre.services.dto.publish;

public interface ApplicationProperties
{

	String getApplicationServerName();

	void setApplicationServerName(String applicationServerName);

	String getApplicationVersion();

	void setApplicationVersion(String applicationVersion);

	int getListenerPort();

	void setListenerPort(int listenerPort);

	String getSendingFacilityNamespaceId();

	void setSendingFacilityNamespaceId(String sendingFacilityNamespaceId);

	String getInterfaceEngineURL();

	void setInterfaceEngineURL(String interfaceEngineURL);

	boolean getUseInterfaceEngine();

	void setUseInterfaceEngine(boolean useInterfaceEngine);

	String getHl7EncodingType();

	void setHl7EncodingType(String hl7EncodingType);

}
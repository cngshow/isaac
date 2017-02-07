package gov.vha.isaac.ochre.services.dto.publish;

public interface MessageProperties
{

	String getSendingApplicationNamespaceIdUpdate();

	void setSendingApplicationNamespaceIdUpdate(String sendingApplicationNamespaceIdUpdate);

	String getSendingApplicationNamespaceIdMD5();

	void setSendingApplicationNamespaceIdMD5(String sendingApplicationNamespaceIdMD5);

	String getSendingApplicationNamespaceIdSiteData();

	void setSendingApplicationNamespaceIdSiteData(String sendingApplicationNamespaceIdSiteData);

	String getReceivingApplicationNamespaceIdUpdate();

	void setReceivingApplicationNamespaceIdUpdate(String receivingApplicationNamespaceIdUpdate);

	String getReceivingApplicationNamespaceIdMD5();

	void setReceivingApplicationNamespaceIdMD5(String receivingApplicationNamespaceIdMD5);

	String getReceivingApplicationNamespaceIdSiteData();

	void setReceivingApplicationNamespaceIdSiteData(String receivingApplicationNamespaceIdSiteData);

	String getVersionId();

	void setVersionId(String versionId);

	String getCountryCode();

	void setCountryCode(String countryCode);

	String getApplicationAcknowledgementType();

	void setApplicationAcknowledgementType(String applicationAcknowledgementType);

	String getAcceptAcknowledgementType();

	void setAcceptAcknowledgementType(String acceptAcknowledgementType);

	String getMasterFileIdentifier();

	void setMasterFileIdentifier(String masterFileIdentifier);

	String getNameOfCodingSystem();

	void setNameOfCodingSystem(String nameOfCodingSystem);

	String getFileLevelEventCode();

	void setFileLevelEventCode(String fileLevelEventCode);

	String getResponseLevelCode();

	void setResponseLevelCode(String responseLevelCode);

	String getRecordLevelEventCode();

	void setRecordLevelEventCode(String recordLevelEventCode);

	String getQueryFormatCode();

	void setQueryFormatCode(String queryFormatCode);

	String getQueryPriority();

	void setQueryPriority(String queryPriority);

	String getQueryId();

	void setQueryId(String queryId);

	Integer getQueryLimitedRequestQuantity();

	void setQueryLimitedRequestQuantity(int queryLimitedRequestQuantity);

	Integer getQueryLimitedRequestUnits();

	void setQueryLimitedRequestUnits(int queryLimitedRequestUnits);

	String getQueryWhoSubjectFilterIdNumber();

	void setQueryWhoSubjectFilterIdNumber(String queryWhoSubjectFilterIdNumber);

	String getQueryWhatDepartmentDataCodeIdentifier();

	void setQueryWhatDepartmentDataCodeIdentifier(String queryWhatDepartmentDataCodeIdentifier);

	String getSubFieldSeparator();

	void setSubFieldSeparator(String subFieldSeparator);

}
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

package gov.vha.isaac.ochre.services.dto.publish;

import java.net.URL;

public class HL7ApplicationProperties
{
	// Application Server Message String
	private String applicationServerName;
	private String applicationVersion;

	// Listener Port
	private int listenerPort;

	// Sending Facility Site ID
	private String sendingFacilityNamespaceId;

	// Target Vitria Interface Engine
	private URL interfaceEngineURL;
	private boolean useInterfaceEngine;
	private String environment;

	// Encoding type
	private String hl7EncodingType;

	// Settings used by the converter to configure the sending application.
	private String sendingApplicationNamespaceIdUpdate;
	private String sendingApplicationNamespaceIdMD5;
	private String sendingApplicationNamespaceIdSiteData;

	// Target Application at VistA sites
	private String receivingApplicationNamespaceIdUpdate;
	private String receivingApplicationNamespaceIdMD5;
	private String receivingApplicationNamespaceIdSiteData;

	// Message Version ID
	private String versionId;

	// acceptAcknowledgementType
	private String acceptAcknowledgementType;
	private String applicationAcknowledgementType;

	private String countryCode;

	// MFI field values
	private String masterFileIdentifier;
	private String nameOfCodingSystem;
	private String fileLevelEventCode;
	private String responseLevelCode;

	// MFE field values
	private String recordLevelEventCode;

	// QRD field values
	private String queryFormatCode;
	private String queryPriority;
	private String queryId;
	private Integer queryLimitedRequestQuantity;
	private Integer queryLimitedRequestUnits;
	private String queryWhoSubjectFilterIdNumber;
	private String queryWhatDepartmentDataCodeIdentifier;

	// CE static field values
	private String subFieldSeparator;

	public String getApplicationServerName() {
		return this.applicationServerName;
	}

	public void setApplicationServerName(String applicationServerName) {
		this.applicationServerName = applicationServerName;
	}

	public String getApplicationVersion() {
		return this.applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public int getListenerPort() {
		return this.listenerPort;
	}

	public void setListenerPort(int listenerPort) {
		this.listenerPort = listenerPort;
	}

	public String getSendingFacilityNamespaceId() {
		return this.sendingFacilityNamespaceId;
	}

	public void setSendingFacilityNamespaceId(String sendingFacilityNamespaceId) {
		this.sendingFacilityNamespaceId = sendingFacilityNamespaceId;
	}

	public URL getInterfaceEngineURL() {
		return this.interfaceEngineURL;
	}

	public void setInterfaceEngineURL(URL interfaceEngineURL) {
		this.interfaceEngineURL = interfaceEngineURL;
	}

	public boolean getUseInterfaceEngine() {
		return this.useInterfaceEngine;
	}

	public void setUseInterfaceEngine(boolean useInterfaceEngine) {
		this.useInterfaceEngine = useInterfaceEngine;
	}

	public String getHl7EncodingType() {
		return this.hl7EncodingType;
	}

	public void setHl7EncodingType(String hl7EncodingType) {
		this.hl7EncodingType = hl7EncodingType;
	}

	public String getSendingApplicationNamespaceIdUpdate() {
		return this.sendingApplicationNamespaceIdUpdate;
	}

	public void setSendingApplicationNamespaceIdUpdate(String sendingApplicationNamespaceIdUpdate) {
		this.sendingApplicationNamespaceIdUpdate = sendingApplicationNamespaceIdUpdate;
	}

	public String getSendingApplicationNamespaceIdMD5() {
		return this.sendingApplicationNamespaceIdMD5;
	}

	public void setSendingApplicationNamespaceIdMD5(String sendingApplicationNamespaceIdMD5) {
		this.sendingApplicationNamespaceIdMD5 = sendingApplicationNamespaceIdMD5;
	}

	public String getSendingApplicationNamespaceIdSiteData() {
		return this.sendingApplicationNamespaceIdSiteData;
	}

	public void setSendingApplicationNamespaceIdSiteData(String sendingApplicationNamespaceIdSiteData) {
		this.sendingApplicationNamespaceIdSiteData = sendingApplicationNamespaceIdSiteData;
	}

	public String getReceivingApplicationNamespaceIdUpdate() {
		return this.receivingApplicationNamespaceIdUpdate;
	}

	public void setReceivingApplicationNamespaceIdUpdate(String receivingApplicationNamespaceIdUpdate) {
		this.receivingApplicationNamespaceIdUpdate = receivingApplicationNamespaceIdUpdate;
	}

	public String getReceivingApplicationNamespaceIdMD5() {
		return this.receivingApplicationNamespaceIdMD5;
	}

	public void setReceivingApplicationNamespaceIdMD5(String receivingApplicationNamespaceIdMD5) {
		this.receivingApplicationNamespaceIdMD5 = receivingApplicationNamespaceIdMD5;
	}

	public String getReceivingApplicationNamespaceIdSiteData() {
		return this.receivingApplicationNamespaceIdSiteData;
	}

	public void setReceivingApplicationNamespaceIdSiteData(String receivingApplicationNamespaceIdSiteData) {
		this.receivingApplicationNamespaceIdSiteData = receivingApplicationNamespaceIdSiteData;
	}

	public String getVersionId() {
		return this.versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public String getCountryCode() {
		return this.countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getApplicationAcknowledgementType() {
		return this.applicationAcknowledgementType;
	}

	public void setApplicationAcknowledgementType(String applicationAcknowledgementType) {
		this.applicationAcknowledgementType = applicationAcknowledgementType;
	}

	public String getAcceptAcknowledgementType() {
		return this.acceptAcknowledgementType;
	}

	public void setAcceptAcknowledgementType(String acceptAcknowledgementType) {
		this.acceptAcknowledgementType = acceptAcknowledgementType;
	}

	public String getMasterFileIdentifier() {
		return this.masterFileIdentifier;
	}

	public void setMasterFileIdentifier(String masterFileIdentifier) {
		this.masterFileIdentifier = masterFileIdentifier;
	}

	public String getNameOfCodingSystem() {
		return this.nameOfCodingSystem;
	}

	public void setNameOfCodingSystem(String nameOfCodingSystem) {
		this.nameOfCodingSystem = nameOfCodingSystem;
	}

	public String getFileLevelEventCode() {
		return this.fileLevelEventCode;
	}

	public void setFileLevelEventCode(String fileLevelEventCode) {
		this.fileLevelEventCode = fileLevelEventCode;
	}

	public String getResponseLevelCode() {
		return this.responseLevelCode;
	}

	public void setResponseLevelCode(String responseLevelCode) {
		this.responseLevelCode = responseLevelCode;
	}

	public String getRecordLevelEventCode() {
		return this.recordLevelEventCode;
	}

	public void setRecordLevelEventCode(String recordLevelEventCode) {
		this.recordLevelEventCode = recordLevelEventCode;
	}

	public String getQueryFormatCode() {
		return this.queryFormatCode;
	}

	public void setQueryFormatCode(String queryFormatCode) {
		this.queryFormatCode = queryFormatCode;
	}

	public String getQueryPriority() {
		return this.queryPriority;
	}

	public void setQueryPriority(String queryPriority) {
		this.queryPriority = queryPriority;
	}

	public String getQueryId() {
		return this.queryId;
	}

	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}

	public Integer getQueryLimitedRequestQuantity() {
		return this.queryLimitedRequestQuantity;
	}

	public void setQueryLimitedRequestQuantity(int queryLimitedRequestQuantity) {
		this.queryLimitedRequestQuantity = queryLimitedRequestQuantity;
	}

	public Integer getQueryLimitedRequestUnits() {
		return this.queryLimitedRequestUnits;
	}

	public void setQueryLimitedRequestUnits(int queryLimitedRequestUnits) {
		this.queryLimitedRequestUnits = queryLimitedRequestUnits;
	}

	public String getQueryWhoSubjectFilterIdNumber() {
		return this.queryWhoSubjectFilterIdNumber;
	}

	public void setQueryWhoSubjectFilterIdNumber(String queryWhoSubjectFilterIdNumber) {
		this.queryWhoSubjectFilterIdNumber = queryWhoSubjectFilterIdNumber;
	}

	public String getQueryWhatDepartmentDataCodeIdentifier() {
		return this.queryWhatDepartmentDataCodeIdentifier;
	}

	public void setQueryWhatDepartmentDataCodeIdentifier(String queryWhatDepartmentDataCodeIdentifier) {
		this.queryWhatDepartmentDataCodeIdentifier = queryWhatDepartmentDataCodeIdentifier;
	}

	public String getSubFieldSeparator() {
		return this.subFieldSeparator;
	}

	public void setSubFieldSeparator(String subFieldSeparator) {
		this.subFieldSeparator = subFieldSeparator;
	}

	public String getEnvironment() {
		return this.environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

}

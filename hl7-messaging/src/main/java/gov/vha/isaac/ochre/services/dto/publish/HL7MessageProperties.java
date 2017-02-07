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

public class HL7MessageProperties implements MessageProperties
{
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

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getSendingApplicationNamespaceIdUpdate()
	 */
	@Override
	public String getSendingApplicationNamespaceIdUpdate() {
		return this.sendingApplicationNamespaceIdUpdate;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setSendingApplicationNamespaceIdUpdate(java.lang.String)
	 */
	@Override
	public void setSendingApplicationNamespaceIdUpdate(String sendingApplicationNamespaceIdUpdate) {
		this.sendingApplicationNamespaceIdUpdate = sendingApplicationNamespaceIdUpdate;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getSendingApplicationNamespaceIdMD5()
	 */
	@Override
	public String getSendingApplicationNamespaceIdMD5() {
		return this.sendingApplicationNamespaceIdMD5;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setSendingApplicationNamespaceIdMD5(java.lang.String)
	 */
	@Override
	public void setSendingApplicationNamespaceIdMD5(String sendingApplicationNamespaceIdMD5) {
		this.sendingApplicationNamespaceIdMD5 = sendingApplicationNamespaceIdMD5;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getSendingApplicationNamespaceIdSiteData()
	 */
	@Override
	public String getSendingApplicationNamespaceIdSiteData() {
		return this.sendingApplicationNamespaceIdSiteData;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setSendingApplicationNamespaceIdSiteData(java.lang.String)
	 */
	@Override
	public void setSendingApplicationNamespaceIdSiteData(String sendingApplicationNamespaceIdSiteData) {
		this.sendingApplicationNamespaceIdSiteData = sendingApplicationNamespaceIdSiteData;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getReceivingApplicationNamespaceIdUpdate()
	 */
	@Override
	public String getReceivingApplicationNamespaceIdUpdate() {
		return this.receivingApplicationNamespaceIdUpdate;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setReceivingApplicationNamespaceIdUpdate(java.lang.String)
	 */
	@Override
	public void setReceivingApplicationNamespaceIdUpdate(String receivingApplicationNamespaceIdUpdate) {
		this.receivingApplicationNamespaceIdUpdate = receivingApplicationNamespaceIdUpdate;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getReceivingApplicationNamespaceIdMD5()
	 */
	@Override
	public String getReceivingApplicationNamespaceIdMD5() {
		return this.receivingApplicationNamespaceIdMD5;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setReceivingApplicationNamespaceIdMD5(java.lang.String)
	 */
	@Override
	public void setReceivingApplicationNamespaceIdMD5(String receivingApplicationNamespaceIdMD5) {
		this.receivingApplicationNamespaceIdMD5 = receivingApplicationNamespaceIdMD5;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getReceivingApplicationNamespaceIdSiteData()
	 */
	@Override
	public String getReceivingApplicationNamespaceIdSiteData() {
		return this.receivingApplicationNamespaceIdSiteData;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setReceivingApplicationNamespaceIdSiteData(java.lang.String)
	 */
	@Override
	public void setReceivingApplicationNamespaceIdSiteData(String receivingApplicationNamespaceIdSiteData) {
		this.receivingApplicationNamespaceIdSiteData = receivingApplicationNamespaceIdSiteData;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getVersionId()
	 */
	@Override
	public String getVersionId() {
		return this.versionId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setVersionId(java.lang.String)
	 */
	@Override
	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getCountryCode()
	 */
	@Override
	public String getCountryCode() {
		return this.countryCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setCountryCode(java.lang.String)
	 */
	@Override
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getApplicationAcknowledgementType()
	 */
	@Override
	public String getApplicationAcknowledgementType() {
		return this.applicationAcknowledgementType;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setApplicationAcknowledgementType(java.lang.String)
	 */
	@Override
	public void setApplicationAcknowledgementType(String applicationAcknowledgementType) {
		this.applicationAcknowledgementType = applicationAcknowledgementType;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getAcceptAcknowledgementType()
	 */
	@Override
	public String getAcceptAcknowledgementType() {
		return this.acceptAcknowledgementType;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setAcceptAcknowledgementType(java.lang.String)
	 */
	@Override
	public void setAcceptAcknowledgementType(String acceptAcknowledgementType) {
		this.acceptAcknowledgementType = acceptAcknowledgementType;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getMasterFileIdentifier()
	 */
	@Override
	public String getMasterFileIdentifier() {
		return this.masterFileIdentifier;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setMasterFileIdentifier(java.lang.String)
	 */
	@Override
	public void setMasterFileIdentifier(String masterFileIdentifier) {
		this.masterFileIdentifier = masterFileIdentifier;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getNameOfCodingSystem()
	 */
	@Override
	public String getNameOfCodingSystem() {
		return this.nameOfCodingSystem;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setNameOfCodingSystem(java.lang.String)
	 */
	@Override
	public void setNameOfCodingSystem(String nameOfCodingSystem) {
		this.nameOfCodingSystem = nameOfCodingSystem;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getFileLevelEventCode()
	 */
	@Override
	public String getFileLevelEventCode() {
		return this.fileLevelEventCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setFileLevelEventCode(java.lang.String)
	 */
	@Override
	public void setFileLevelEventCode(String fileLevelEventCode) {
		this.fileLevelEventCode = fileLevelEventCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getResponseLevelCode()
	 */
	@Override
	public String getResponseLevelCode() {
		return this.responseLevelCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setResponseLevelCode(java.lang.String)
	 */
	@Override
	public void setResponseLevelCode(String responseLevelCode) {
		this.responseLevelCode = responseLevelCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getRecordLevelEventCode()
	 */
	@Override
	public String getRecordLevelEventCode() {
		return this.recordLevelEventCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setRecordLevelEventCode(java.lang.String)
	 */
	@Override
	public void setRecordLevelEventCode(String recordLevelEventCode) {
		this.recordLevelEventCode = recordLevelEventCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getQueryFormatCode()
	 */
	@Override
	public String getQueryFormatCode() {
		return this.queryFormatCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setQueryFormatCode(java.lang.String)
	 */
	@Override
	public void setQueryFormatCode(String queryFormatCode) {
		this.queryFormatCode = queryFormatCode;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getQueryPriority()
	 */
	@Override
	public String getQueryPriority() {
		return this.queryPriority;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setQueryPriority(java.lang.String)
	 */
	@Override
	public void setQueryPriority(String queryPriority) {
		this.queryPriority = queryPriority;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getQueryId()
	 */
	@Override
	public String getQueryId() {
		return this.queryId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setQueryId(java.lang.String)
	 */
	@Override
	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getQueryLimitedRequestQuantity()
	 */
	@Override
	public Integer getQueryLimitedRequestQuantity() {
		return this.queryLimitedRequestQuantity;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setQueryLimitedRequestQuantity(int)
	 */
	@Override
	public void setQueryLimitedRequestQuantity(int queryLimitedRequestQuantity) {
		this.queryLimitedRequestQuantity = queryLimitedRequestQuantity;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getQueryLimitedRequestUnits()
	 */
	@Override
	public Integer getQueryLimitedRequestUnits() {
		return this.queryLimitedRequestUnits;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setQueryLimitedRequestUnits(int)
	 */
	@Override
	public void setQueryLimitedRequestUnits(int queryLimitedRequestUnits) {
		this.queryLimitedRequestUnits = queryLimitedRequestUnits;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getQueryWhoSubjectFilterIdNumber()
	 */
	@Override
	public String getQueryWhoSubjectFilterIdNumber() {
		return this.queryWhoSubjectFilterIdNumber;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setQueryWhoSubjectFilterIdNumber(java.lang.String)
	 */
	@Override
	public void setQueryWhoSubjectFilterIdNumber(String queryWhoSubjectFilterIdNumber) {
		this.queryWhoSubjectFilterIdNumber = queryWhoSubjectFilterIdNumber;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getQueryWhatDepartmentDataCodeIdentifier()
	 */
	@Override
	public String getQueryWhatDepartmentDataCodeIdentifier() {
		return this.queryWhatDepartmentDataCodeIdentifier;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setQueryWhatDepartmentDataCodeIdentifier(java.lang.String)
	 */
	@Override
	public void setQueryWhatDepartmentDataCodeIdentifier(String queryWhatDepartmentDataCodeIdentifier) {
		this.queryWhatDepartmentDataCodeIdentifier = queryWhatDepartmentDataCodeIdentifier;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#getSubFieldSeparator()
	 */
	@Override
	public String getSubFieldSeparator() {
		return this.subFieldSeparator;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.services.dto.publish.MessageProperties#setSubFieldSeparator(java.lang.String)
	 */
	@Override
	public void setSubFieldSeparator(String subFieldSeparator) {
		this.subFieldSeparator = subFieldSeparator;
	}


}


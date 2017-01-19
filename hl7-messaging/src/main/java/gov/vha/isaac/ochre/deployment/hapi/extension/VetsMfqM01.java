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
package gov.vha.isaac.ochre.deployment.hapi.extension;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.message.MFQ_M01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.QRD;
import gov.vha.isaac.ochre.deployment.publish.ApplicationPropertyReader;
import gov.vha.isaac.ochre.deployment.publish.HL7DateHelper;

public class VetsMfqM01 extends MFQ_M01
{
	/**
	 * Populates the MSH segment of the message header
	 *
	 * @param queryMessage Message to which the MSH segment will be added
	 * @param sendingApplication Name of the sending application
	 * @param receivingApplication Name of the receiving application
	 * @throws DataTypeException
	 */
	public void addMshSegment(VetsMfqM01 queryMessage, String sendingApplication, String receivingApplication)
			throws DataTypeException
	{
		String sendingFacilityId = null;
		String versionId = null;
		String acceptAcknowledgementType = null;
		String applicationAcknowledgementType = null;
		String countryCode = null;

		String hl7DateString = HL7DateHelper.getHL7DateFormat(HL7DateHelper.getCurrentDateTime());

		MSH msh = queryMessage.getMSH();

		//This is a static value that will not change
		msh.getFieldSeparator().setValue("^");
		//This is a static value that will not change
		msh.getEncodingCharacters().setValue("~|\\&");
		//MSH.3.1
		msh.getSendingApplication().getNamespaceID().setValue(sendingApplication);
		//MSH.4.1
		sendingFacilityId = ApplicationPropertyReader.getApplicationProperty("msh.sendingFacility.namespaceId");
		msh.getSendingFacility().getNamespaceID().setValue(sendingFacilityId);
		//MSH.5.1
		msh.getReceivingApplication().getNamespaceID().setValue(receivingApplication);
		//MSH.6.1 - left blank until time of deployment to VistA site(s)
		msh.getReceivingFacility().getNamespaceID().setValue("");
		//MSH.7.1 - left blank until time of deployment to VistA site(s)
		//TODO: FIX this line -> msh.getDateTimeOfMessage().getTimeOfAnEvent().setValue(hl7DateString);
		//MSH.9.1
		//TODO: FIX this line -> msh.getMessageType().getMessageType().setValue("MFQ");
		//MSH.9.2
		msh.getMessageType().getTriggerEvent().setValue("M01");
		//MSH.9.3 - left blank because this piece would be redundant
		msh.getMessageType().getMessageStructure().setValue("");
		//MSH.10 - left blank until time of deployment to VistA site(s)
		msh.getMessageControlID().setValue("");
		//MSH.11.1 - 'T'EST, 'P'RODUCTION or 'D'EBUG - left blank until time of deployment
		msh.getProcessingID().getProcessingID().setValue("");
		//MSH.12.1
		versionId = ApplicationPropertyReader.getApplicationProperty("msh.versionId");
		msh.getVersionID().getVersionID().setValue(versionId);
		//MSH.15 - 'AL' or 'NE'
		acceptAcknowledgementType = ApplicationPropertyReader.getApplicationProperty("msh.acceptAcknowledgementType");
		msh.getAcceptAcknowledgmentType().setValue(acceptAcknowledgementType);
		//MSH.16 - 'AL' or 'NE'
		applicationAcknowledgementType = ApplicationPropertyReader.getApplicationProperty("msh.applicationAcknowledgementType");
		msh.getApplicationAcknowledgmentType().setValue(applicationAcknowledgementType);
		//MSH.17 - Set this from a constant
		countryCode = ApplicationPropertyReader.getApplicationProperty("msh.countryCode");
		msh.getCountryCode().setValue(countryCode);
	}

	/**
	 * Populates the QRD segment to the message without a filter value
	 *
	 * @param message Message to which the QRD segment will be added
	 * @param hl7DateString Date this message was created
	 * @param subsetName Name of the subset
	 * @throws DataTypeException
	 */
	public void addQrdSegment(VetsMfqM01 queryMessage, String hl7DateString, String regionName) throws HL7Exception, DataTypeException
	{
		addFilteredQrdSegment(queryMessage, hl7DateString, regionName, null);
	}

	/**
	 * Populates the QRD segment to the message and includes a filter value
	 *
	 * @param message Message to which the QRD segment will be added
	 * @param hl7DateString Date this message was created
	 * @param subsetName Name of the subset
	 * @param filterValue Value by which result should be filtered
	 * @throws DataTypeException
	 */
	public void addFilteredQrdSegment(VetsMfqM01 queryMessage, String hl7DateString, String regionName, String filterValue) throws HL7Exception, DataTypeException
	{
		String queryFormatCode = null;
		String queryPriority = null;
		String queryId = null;
		String quantityLimitedRequest = null;
		String quantityLimitedRequestUnits = null;
		String whoSubjectFilterIdNumber = null;
		String whatDeptDataCodeIdentifier = null;

		QRD qrd = queryMessage.getQRD();

		//QRD.1.1
		//TODO: FIX this line -> qrd.getQueryDateTime().getTimeOfAnEvent().setValue(hl7DateString);
		//QRD.2
		queryFormatCode = ApplicationPropertyReader.getApplicationProperty("qrd.queryFormatCode");
		qrd.getQueryFormatCode().setValue(queryFormatCode); //"R"
		//QRD.3
		queryPriority = ApplicationPropertyReader.getApplicationProperty("qrd.queryPriority");
		qrd.getQueryPriority().setValue(queryPriority); //"I"
		//QRD.4
		queryId = ApplicationPropertyReader.getApplicationProperty("qrd.queryId");
		qrd.getQueryID().setValue(queryId); //"Standard Terminology Query"
		//QRD.6.1
		//TODO: FIX this line -> qrd.getDeferredResponseDateTime().getTimeOfAnEvent().setValue("");
		//QRD.7.1
		quantityLimitedRequest = ApplicationPropertyReader.getApplicationProperty("qrd.quantityLimitedRequest.quantity");
		qrd.getQuantityLimitedRequest().getQuantity().setValue(quantityLimitedRequest); //"24"
		//QRD.7.2.1
		quantityLimitedRequestUnits = ApplicationPropertyReader.getApplicationProperty("qrd.quantityLimitedRequest.units");
		qrd.getQuantityLimitedRequest().getUnits().getIdentifier().setValue(quantityLimitedRequestUnits); //"99999"
		//QRD.8.1
		whoSubjectFilterIdNumber = ApplicationPropertyReader.getApplicationProperty("qrd.whoSubjectFilter.idNumber");
		qrd.getWhoSubjectFilter(0).getIDNumber().setValue(whoSubjectFilterIdNumber); //"ALL"
		//QRD.9.1
		qrd.getWhatSubjectFilter(0).getIdentifier().setValue(regionName);
		//QRD.9.2
		qrd.getWhatSubjectFilter(0).getText().setValue(filterValue);
		//QRD.10.1
		whatDeptDataCodeIdentifier = ApplicationPropertyReader.getApplicationProperty("qrd.whatDepartmentDataCode.identifier");
		qrd.getWhatDepartmentDataCode(0).getIdentifier().setValue(whatDeptDataCodeIdentifier); //"VETS"
	}
}

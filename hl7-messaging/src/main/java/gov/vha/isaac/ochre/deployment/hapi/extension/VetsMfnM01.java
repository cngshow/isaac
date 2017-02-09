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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v24.group.MFN_M01_MF;
import ca.uhn.hl7v2.model.v24.message.MFN_M01;
import ca.uhn.hl7v2.model.v24.segment.MFE;
import ca.uhn.hl7v2.model.v24.segment.MFI;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.MessageProperties;

/**
 * @author vhaislempeyd
 */

public class VetsMfnM01 extends MFN_M01
{
	private static Logger LOG = LogManager.getLogger(VetsMfnM01.class.getPackage().getName());

	/**
	 * Creates a new MFN_M01 Group.
	 */
	public VetsMfnM01() {
		try {
			this.add(MSH.class, true, false);
			this.add(MFI.class, true, false);
			this.add(MFN_M01_MF.class, true, true);
			this.add(VetsMfnM01Mfezxx.class, true, true);
		} catch (HL7Exception e) {
			LOG.error("Unexpected error creating VetsMfnM01 - this is probably a bug in the source code generator.", e);
		}
	}

	public VetsMfnM01Mfezxx getVetsMfnM01Mfezxx() {
		VetsMfnM01Mfezxx ret = null;
		try {
			ret = (VetsMfnM01Mfezxx) this.get("fezxx");
		} catch (HL7Exception e) {
			LOG.error("Unexpected error accessing data - this is probably a bug in the source code generator.", e);
		}
		return ret;
	}

	/**
	 * Returns a specific repetition of MFN_M01_MFEZxx (a Group object) -
	 * creates it if necessary throws HL7Exception if the repetition requested
	 * is more than one greater than the number of existing repetitions.
	 */
	public VetsMfnM01Mfezxx getVetsMfnM01Mfezxx(int rep) throws HL7Exception {
		return (VetsMfnM01Mfezxx) this.get("fezxx", rep);
	}

	/**
	 * Returns the number of existing repetitions of VetsMfnM01Mfezxx
	 */
	public int getVetsMfnM01MfezxxReps() {
		int reps = -1;
		try {
			reps = this.getAll("fezxx").length;
		} catch (HL7Exception e) {
			String message = "Unexpected error accessing data - this is probably a bug in the source code generator.";
			LOG.error(message, e);
			throw new Error(message);
		}
		return reps;
	}

	/**
	 * Populates the MSH segment of the message header
	 * 
	 * @param message
	 *            Message to which the MSH segment will be added
	 * @param hl7DateString
	 *            Date this message was created
	 * @throws DataTypeException
	 */
	public void addMshSegment(VetsMfnM01 message, String hl7DateString, ApplicationProperties applicationProperties,
			MessageProperties messageProperties) throws DataTypeException {
		
		String namespaceId = null;
		String sendingFacilityId = null;
		String receivingApplicationNamespaceId = null;
		String versionId = null;
		String acceptAcknowledgementType = null;
		String applicationAcknowledgementType = null;
		String countryCode = null;

		MSH msh = message.getMSH();

		// This is a static value that will not change
		msh.getFieldSeparator().setValue("^");

		// This is a static value that will not change
		msh.getEncodingCharacters().setValue("~|\\&");

		// MSH.3.1
		namespaceId = messageProperties.getSendingApplicationNamespaceIdUpdate();
		msh.getSendingApplication().getNamespaceID().setValue(namespaceId);

		// MSH.3.3
		// universalIdType =
		// HL7SenderProperties.getApplicationProperties("msh.sendingApplication.universalIdType");
		// msh.getSendingApplication().getUniversalIDType().setValue(universalIdType);

		// MSH.4.1
		sendingFacilityId = applicationProperties.getSendingFacilityNamespaceId();
		msh.getSendingFacility().getNamespaceID().setValue(sendingFacilityId);

		// MSH.5.1
		receivingApplicationNamespaceId = messageProperties.getReceivingApplicationNamespaceIdUpdate();
		msh.getReceivingApplication().getNamespaceID().setValue(receivingApplicationNamespaceId);

		// MSH.6.1 - left blank until time of deployment to VistA site(s)
		msh.getReceivingFacility().getNamespaceID().setValue("");

		// MSH.7.1 - left blank until time of deployment to VistA site(s)
		msh.getDateTimeOfMessage().getTimeOfAnEvent().setValue(hl7DateString);

		// MSH.9.1
		msh.getMessageType().getMessageType().setValue("MFN");

		// MSH.9.2
		msh.getMessageType().getTriggerEvent().setValue("M01");

		// MSH.9.3 - left blank because this piece would be redundant
		msh.getMessageType().getMessageStructure().setValue("");

		// MSH.10 - left blank until time of deployment to VistA site(s)
		msh.getMessageControlID().setValue("");

		// MSH.11.1 - 'T'EST, 'P'RODUCTION or 'D'EBUG - left blank until time of
		// deployment
		msh.getProcessingID().getProcessingID().setValue("");

		// MSH.12.1
		// versionId =
		versionId = messageProperties.getVersionId();
		msh.getVersionID().getVersionID().setValue(versionId);

		// MSH.15 - 'AL' or 'NE'
		acceptAcknowledgementType = messageProperties.getAcceptAcknowledgementType();
		msh.getAcceptAcknowledgmentType().setValue(acceptAcknowledgementType);

		// MSH.16 - 'AL' or 'NE'
		applicationAcknowledgementType = messageProperties.getApplicationAcknowledgementType();
		msh.getApplicationAcknowledgmentType().setValue(applicationAcknowledgementType);

		// MSH.17 - Set this from a constant
		countryCode = messageProperties.getCountryCode();
		msh.getCountryCode().setValue(countryCode);
	}

	/**
	 * Populates the MFI segment of the message header
	 * 
	 * @param message
	 *            Message to which the MFI segment will be added
	 * @param hl7DateString
	 *            Date this message was created
	 * @throws DataTypeException
	 */
	public void addMfiSegment(VetsMfnM01 message, String hl7DateString, MessageProperties messageProperties)
			throws DataTypeException {
		String masterFileIdentifier = null;
		String nameOfCodingSystem = null;
		String fileLevelEventCode = null;
		String responseLevelCode = null;

		MFI mfi = message.getMFI();

		// MFI.1.1
		masterFileIdentifier = messageProperties.getMasterFileIdentifier();
		mfi.getMasterFileIdentifier().getIdentifier().setValue(masterFileIdentifier);

		// MFI.1.3
		nameOfCodingSystem = messageProperties.getNameOfCodingSystem();
		mfi.getMasterFileIdentifier().getNameOfCodingSystem().setValue(nameOfCodingSystem);

		// MFI.3
		fileLevelEventCode = messageProperties.getFileLevelEventCode();
		mfi.getFileLevelEventCode().setValue(fileLevelEventCode);

		// MFI.4.1
		mfi.getEnteredDateTime().getTimeOfAnEvent().setValue(hl7DateString);

		// MFI.5.1
		mfi.getEffectiveDateTime().getTimeOfAnEvent().setValue(hl7DateString);

		// MFI.6
		responseLevelCode = messageProperties.getResponseLevelCode();
		mfi.getResponseLevelCode().setValue(responseLevelCode);
	}

	/**
	 * populates the MFE segment of the message for each group of ZRT segments
	 * This method does not take the vuid as a parameter for concepts that do
	 * not have a VUID - specifically for the case of updating the standard
	 * terminology version file.
	 * 
	 * @param message
	 *            Message to which the MFE segment will be added.
	 * @param hl7DateString
	 *            Date this message was created
	 * @param subset
	 *            Subset name
	 * @param conceptName
	 *            Name of concept for which the MFE segment will be added
	 * @param iteration
	 *            Ordinal ranking of this MFE segment in the current message
	 * @return MFE segment
	 * @throws HL7Exception
	 */
	public VetsMfnM01Mfezxx addMfeSegment(VetsMfnM01 message, String hl7DateString, String subset, String conceptName,
			int iteration, MessageProperties messageProperties) throws HL7Exception {
		String recordLevelEventCode = null;

		VetsMfnM01Mfezxx mfeGroup = message.getVetsMfnM01Mfezxx(iteration);

		MFE mfe = mfeGroup.getMFE();

		// g1R.MFE.1
		recordLevelEventCode = messageProperties.getRecordLevelEventCode();
		mfe.getRecordLevelEventCode().setValue(recordLevelEventCode);

		// g1R.MFE.3.1
		// mfe.getEffectiveDateTime().getTimeOfAnEvent().setValue(hl7DateString);

		// g1R.MFE.4
		mfe.getPrimaryKeyValueMFE(0).setData(mfeGroup.getCeObject(subset, message));

		return mfeGroup;
	}

	/**
	 * Populates the MFE segment of the message for each group of ZRT segments
	 * 
	 * @param message
	 *            Message to which the MFE segment will be added.
	 * @param hl7DateString
	 *            Date this message was created
	 * @param subset
	 *            Subset name
	 * @param conceptVuid
	 *            VUID of the concept for which the MFE segment will be added
	 * @param conceptName
	 *            Name of concept for which the MFE segment will be added
	 * @param iteration
	 *            Ordinal ranking of this MFE segment in the current message
	 * @return MFE segment
	 * @throws HL7Exception
	 */
	public VetsMfnM01Mfezxx addMfeSegment(VetsMfnM01 message, String hl7DateString, String subset,
			String conceptVuidString, String conceptName, int iteration, MessageProperties messageProperties)
			throws HL7Exception {
		String recordLevelEventCode = null;

		VetsMfnM01Mfezxx mfeGroup = message.getVetsMfnM01Mfezxx(iteration);

		MFE mfe = mfeGroup.getMFE();

		// g1R.MFE.1
		recordLevelEventCode = messageProperties.getRecordLevelEventCode();
		mfe.getRecordLevelEventCode().setValue(recordLevelEventCode);

		// g1R.MFE.3.1
		// mfe.getEffectiveDateTime().getTimeOfAnEvent().setValue(hl7DateString);

		// g1R.MFE.4
		mfe.getPrimaryKeyValueMFE(0).setData(mfeGroup.getCeObject(subset, conceptVuidString, message, messageProperties));

		return mfeGroup;
	}
}

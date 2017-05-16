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
package gov.vha.isaac.ochre.deployment.publish;

import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import gov.vha.isaac.ochre.deployment.hapi.extension.VetsMfnM01;
import gov.vha.isaac.ochre.deployment.hapi.extension.VetsMfnM01Mfezxx;
import gov.vha.isaac.ochre.services.dto.publish.ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.MessageProperties;
import gov.vha.isaac.ochre.services.dto.publish.NameValueDTO;
import gov.vha.isaac.ochre.services.dto.publish.PublishConceptDTO;
import gov.vha.isaac.ochre.services.dto.publish.PublishRegionDTO;
import gov.vha.isaac.ochre.services.exception.STSException;

public class HL7SubsetUpdateGenerator extends HL7BaseGenerator
{
	private static Logger log = LogManager.getLogger(HL7SubsetUpdateGenerator.class);

	public static final String TERM_FIELD_NAME = "Term";

	/**
	 * The <code>getVetsMessageObject</code> method returns a message String.
	 * 
	 * @param publishRegionDTOList
	 * @return String HL7 message
	 * @throws STSException
	 */
	public static String getMessage(List<PublishRegionDTO> publishRegionDTOList,
			ApplicationProperties applicationProperties, MessageProperties messageProperties)
			throws STSException {
		HL7SubsetUpdateGenerator generator = new HL7SubsetUpdateGenerator();
		return generator.produceMessage(publishRegionDTOList, applicationProperties, messageProperties);
	}

	public String produceMessage(List<PublishRegionDTO> publishRegionDTOList,
			ApplicationProperties applicationProperties, MessageProperties messageProperties)
			throws STSException {
		if (publishRegionDTOList.size() == 0) {
			return null;
		}
		VetsMfnM01 message = new VetsMfnM01();
		String hl7DateString = HL7DateHelper.getHL7DateFormat(HL7DateHelper.getCurrentDateTime());
		String subsetName = null;
		int mfeCounter = 0;
		int zrtCounter = 0;

		try {
			// Get the MSH and MFI parts of the header
			message.addMshSegment(message, hl7DateString, applicationProperties, messageProperties);
			message.addMfiSegment(message, hl7DateString, messageProperties);

			VetsMfnM01Mfezxx mfeGroup = null;

			Iterator publishRegionListIter = publishRegionDTOList.iterator();
			while (publishRegionListIter.hasNext()) {
				PublishRegionDTO publishRegionDTO = (PublishRegionDTO) publishRegionListIter.next();
				subsetName = publishRegionDTO.getRegionName();

				List<PublishConceptDTO> publishConceptDTOList = publishRegionDTO.getPublishConceptDTOList();

				if (publishConceptDTOList == null || publishConceptDTOList.size() == 0) {
					continue;
				}

				Iterator publishConceptDTOListIter = publishConceptDTOList.iterator();
				while (publishConceptDTOListIter.hasNext()) {
					PublishConceptDTO publishConceptDTO = (PublishConceptDTO) publishConceptDTOListIter.next();
					mfeGroup = mfeSegment(message, hl7DateString, subsetName, mfeCounter, publishConceptDTO,
							messageProperties);

					// Add the 'Term' ZRT segment then increment the zrtCounter
					mfeGroup.addZrtSegment(mfeGroup,
							new NameValueDTO(TERM_FIELD_NAME, publishConceptDTO.getPublishName()), zrtCounter);
					zrtCounter++;

					// Set the properties in the ZRT segments
					List properties = publishConceptDTO.getPropertyList();
					if (properties != null && properties.size() != 0) {
						Iterator propertyIter = properties.iterator();
						while (propertyIter.hasNext()) {
							NameValueDTO property = (NameValueDTO) propertyIter.next();
							mfeGroup.addZrtSegment(mfeGroup, property, zrtCounter);
							zrtCounter++;
						}
					}

					// Set the designations in the ZRT segments
					List designations = publishConceptDTO.getDesignationList();
					if (designations != null && designations.size() != 0) {
						Iterator designationIter = designations.iterator();
						while (designationIter.hasNext()) {
							NameValueDTO designation = (NameValueDTO) designationIter.next();
							mfeGroup.addZrtSegment(mfeGroup, designation, zrtCounter);
							zrtCounter++;
						}
					}

					// Set the relationships in the ZRT segments
					List relationships = publishConceptDTO.getRelationshipList();
					if (relationships != null && relationships.size() != 0) {
						Iterator relationshipIter = relationships.iterator();
						while (relationshipIter.hasNext()) {
							NameValueDTO relationship = (NameValueDTO) relationshipIter.next();
							mfeGroup.addZrtSegment(mfeGroup, relationship, zrtCounter);
							zrtCounter++;
						}
					}
					zrtCounter = additionalSegments(subsetName, zrtCounter, mfeGroup, publishConceptDTO);

					// Add a ZRT segment for each concept for the status value
					String conceptStatusValue = getStatusValue(publishConceptDTO.isActive());
					NameValueDTO statusInfo = new NameValueDTO(STATUS_FIELD_NAME, conceptStatusValue);
					mfeGroup.addZrtSegment(mfeGroup, statusInfo, zrtCounter);

					// After adding all ZRTs, set the zrtCounter back to 0 for
					// next group of ZRT segments
					zrtCounter = 0;

					// Increment the mfeCounter
					mfeCounter++;
				}
			}

			// add the update to the version file
			VetsMfnM01Mfezxx versionMFEGroup = message.addMfeSegment(message, hl7DateString, TERMINOLOGY_VERSION_SUBSET,
					TERMINOLOGY_VERSION_NAME, mfeCounter, messageProperties);

			// Set the VERSION_FIELD_NAME value to zero (0) because it is no
			// longer meaningful to send to VistA
			versionMFEGroup.addZrtSegment(versionMFEGroup, new NameValueDTO(VERSION_FIELD_NAME, "0"), zrtCounter);

			String messageString = HL7SubsetUpdateGenerator.getMessage(message);

			// return message;
			return messageString;
		} catch (DataTypeException e) {
			log.error("Data Type Exception: Error generating a portion of the message.");
			throw new STSException("Data Type Exception: Error generating a portion of the message.", e);
		} catch (HL7Exception e) {
			log.error("HL7 Exception: Error adding a segment of the message.");
			throw new STSException("HL7 Exception: Error adding a segment of the message.", e);
		}
	}

	protected int additionalSegments(String subsetName, int zrtCounter, VetsMfnM01Mfezxx mfeGroup,
			PublishConceptDTO publishConceptDTO) throws HL7Exception {
		return zrtCounter;
	}

	protected VetsMfnM01Mfezxx mfeSegment(VetsMfnM01 message, String hl7DateString, String subsetName, int mfeCounter,
			PublishConceptDTO publishConceptDTO, MessageProperties messageProperties) throws HL7Exception {
		VetsMfnM01Mfezxx mfeGroup;
		mfeGroup = message.addMfeSegment(message, hl7DateString, subsetName, Long.toString(publishConceptDTO.getVuid()),
				publishConceptDTO.getPublishName(), mfeCounter, messageProperties);
		return mfeGroup;
	}
}

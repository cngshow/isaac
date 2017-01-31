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
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.datatype.CE;
import ca.uhn.hl7v2.model.v24.group.MFN_M01_MF;
import ca.uhn.hl7v2.model.v24.segment.Zxx;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import gov.vha.isaac.ochre.services.dto.publish.HL7ApplicationProperties;
import gov.vha.isaac.ochre.services.dto.publish.NameValueDTO;

/**
 * @author vhaislempeyd
 */

public class VetsMfnM01Mfezxx extends MFN_M01_MF
{
	private static Logger log = LogManager.getLogger(VetsMfnM01Mfezxx.class.getPackage().getName());

	/**
	 * Creates a new VetsMfnM01Mfezxx Group.
	 */
	public VetsMfnM01Mfezxx(Group parent, ModelClassFactory factory) {
		super(parent, factory);
		try {
			this.add(ZRT.class, true, true);
			this.add(Zxx.class, false, false);
		} catch (HL7Exception e) {
			log.error(
					"Unexpected error creating VetsMfnM01Mfezxx - this is probably a bug in the source code generator.",
					e);
		}
	}

	/**
	 * Returns ZRT - creates it if necessary
	 */
	public ZRT getZrt() {
		ZRT ret = null;
		try {
			ret = (ZRT) this.get("ZRT");
		} catch (HL7Exception e) {
			log.error("Unexpected error accessing data - this is probably a bug in the source code generator.", e);
		}
		return ret;
	}

	/**
	 * Populates the ZRT segments under each MFE segment
	 *
	 * @param mfeGroup
	 *            MFE segment to shich ZRT segments will be added
	 * @param nameValueObject
	 *            Data to be added in the ZRT segment, such as Properties or
	 *            Relationships name/value pairs
	 * @param zrtCounter
	 *            Ordinal ranking of this ZRT segment under the current MFE
	 *            segment
	 * @throws HL7Exception
	 */
	public void addZrtSegment(VetsMfnM01Mfezxx mfeGroup, NameValueDTO nameValueObject, int zrtCounter)
			throws HL7Exception {
		mfeGroup.addNonstandardSegment("ZRT");
		ZRT zrt = (ZRT) mfeGroup.get("ZRT", zrtCounter);
		zrt.getFieldName().setValue(nameValueObject.getName());

		String zrtFieldValue = nameValueObject.getValue();
		if (zrtFieldValue.length() > 0) {
			zrt.getFieldValue().setValue(zrtFieldValue);
		} else {
			zrt.getFieldValue().setValue("\"\"");
		}
	}

	/**
	 * Creates and populates a CE object that may be passed as a parameter to
	 * MFE.getPrimaryKeyValueMFE() in an MFE segment. This method does not take
	 * the VUID as a parameter for concepts that do not have a VUID -
	 * specifically for the case of updating the standard terminology version
	 * file.
	 *
	 * @param subset
	 *            Name of the subset for which the record is being created
	 * @param conceptName
	 *            Name of the concept for which the record is being created
	 * @return CE datatype object
	 * @throws DataTypeException
	 */
	public CE getCeObject(String subset, Message message) throws DataTypeException {
		CE ce = new CE(message);

		// g1R.MFE.4.1 - This is two pieces: File Flag '@' VUID
		ce.getIdentifier().setValue(subset);

		return ce;
	}

	/**
	 * Creates and populates a CE object that may be passed as a parameter to
	 * MFE.getPrimaryKeyValueMFE() in an MFE segment.
	 *
	 * @param subset
	 *            Name of the subset for which the record is being created
	 * @param conceptVuid
	 *            VUID of the concept for which the record is being created
	 * @param conceptName
	 *            Name of the concept for which the record is being created
	 * @return CE datatype object
	 * @throws DataTypeException
	 */
	public CE getCeObject(String subset, String conceptVuidString, Message message,
			HL7ApplicationProperties serverConfig) throws DataTypeException {
		String subFieldSeparator;
		String identifier = null;

		CE ce = new CE(message);

		// g1R.MFE.4.1 - This is two pieces: File Flag '@' VUID
		// subFieldSeparator =
		// ApplicationPropertyReader.getApplicationProperty("ce.subFieldSeparator");
		subFieldSeparator = serverConfig.getSubFieldSeparator();
		identifier = subset.concat(subFieldSeparator).concat(conceptVuidString);
		ce.getIdentifier().setValue(identifier);

		return ce;
	}
}

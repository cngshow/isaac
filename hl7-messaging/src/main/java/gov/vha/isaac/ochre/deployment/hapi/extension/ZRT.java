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
import ca.uhn.hl7v2.model.AbstractSegment;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.v24.datatype.ST;
import ca.uhn.hl7v2.parser.ModelClassFactory;

/**
 * @author vhaislempeyd
 */
public class ZRT extends AbstractSegment
{
	private static Logger LOG = LogManager.getLogger(ZRT.class);

	/**
	 * Creates a Zrt segment object that belongs to the given message.
	 */
	@SuppressWarnings("deprecation")
	public ZRT(Group parent, ModelClassFactory factory) {
		super(parent, factory);
		try {
			this.add(ST.class, true, 1, 50, null);
			this.add(ST.class, true, 1, 80, null);
		} catch (HL7Exception he) {
			LOG.error("Can't instantiate " + this.getClass().getName(), he);
		}
	}

	/**
	 * Returns field name
	 * 
	 * @return HAPI String type (ST) name of the field
	 */
	public ST getFieldName() {
		ST ret = null;
		try {
			Type t = this.getField(1, 0);
			ret = (ST) t;
		} catch (ClassCastException e) {
			LOG.error("Unexpected problem obtaining field value.  This is a bug.", e);
		} catch (HL7Exception e) {
			LOG.error("Unexpected problem obtaining field value.  This is a bug.", e);
		}
		return ret;
	}

	/**
	 * Returns field value
	 * 
	 * @return HAPI String type (ST) value of the field
	 */
	public ST getFieldValue() {
		ST ret = null;
		try {
			Type t = this.getField(2, 0);
			ret = (ST) t;
		} catch (ClassCastException e) {
			LOG.error("Unexpected problem obtaining field value.  This is a bug.", e);
		} catch (HL7Exception e) {
			LOG.error("Unexpected problem obtaining field value.  This is a bug.", e);
		}
		return ret;
	}
}
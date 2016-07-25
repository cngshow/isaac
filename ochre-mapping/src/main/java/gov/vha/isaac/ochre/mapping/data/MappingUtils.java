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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.mapping.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.impl.utility.SimpleDisplayConcept;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;

/**
 * {@link MappingUtils}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class MappingUtils
{
	protected static final Logger LOG = LoggerFactory.getLogger(MappingUtils.class);
	
	public static final HashMap<String, ConceptSpecification> CODE_SYSTEM_CONCEPTS = new HashMap<String, ConceptSpecification>(); 
	static 
	{
		CODE_SYSTEM_CONCEPTS.put("SNOMED CT", MetaData.SNOMED_CT_CORE_MODULE);
		CODE_SYSTEM_CONCEPTS.put("SNOMED CT US Extension", MetaData.US_EXTENSION_MODULE);
		CODE_SYSTEM_CONCEPTS.put("LOINC", MetaData.LOINC_MODULE);
		CODE_SYSTEM_CONCEPTS.put("RxNorm", MetaData.RXNORM_MODULE);
		CODE_SYSTEM_CONCEPTS.put("VHAT", MetaData.VHA_MODULE);
	}
	
	public static List<SimpleDisplayConcept> getStatusConcepts() throws IOException
	{
		ArrayList<SimpleDisplayConcept> result = new ArrayList<>();
//		for (Integer conSequence : Frills.getAllChildrenOfConcept(IsaacMappingConstants.get().MAPPING_STATUS.getSequence(), true, false))
//		{
//			result.add(new SimpleDisplayConcept(conSequence));
//		}
		
		Collections.sort(result);
		return result;
	}
	
	public static List<SimpleDisplayConcept> getQualifierConcepts() throws IOException
	{
		ArrayList<SimpleDisplayConcept> result = new ArrayList<>();
		for (Integer conSequence : Frills.getAllChildrenOfConcept(IsaacMappingConstants.get().MAPPING_QUALIFIERS.getSequence(), true, false))
		{
			result.add(new SimpleDisplayConcept(conSequence));
		}

		Collections.sort(result);
		return result;
	}
	
	public static List<SimpleDisplayConcept> getCodeSystems() {
		
		List<SimpleDisplayConcept> codeSystems = new ArrayList<SimpleDisplayConcept>();
		CODE_SYSTEM_CONCEPTS.entrySet().forEach((item) -> codeSystems.add(new SimpleDisplayConcept(item.getKey(), item.getValue().getNid())));
		return codeSystems;
	}
	
}

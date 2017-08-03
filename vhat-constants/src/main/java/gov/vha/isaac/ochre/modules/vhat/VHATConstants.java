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

package gov.vha.isaac.ochre.modules.vhat;

import java.util.UUID;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.constants.MetadataConceptConstant;
import gov.vha.isaac.ochre.api.constants.ModuleProvidedConstants;

/**
 * 
 * {@link VHATConstants}
 * 
 * A bunch of constants that actually gets generated by the VHAT DB loader.
 * It is the VHAT DB Loaders job to validate that the UUIDs match these hard-coded values. 
 * 
 * If you add any additional constants here, please add sanity checkers the the ISAAC-term-convert-vhat VhatUtil class.
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@Service
public class VHATConstants implements ModuleProvidedConstants {

	
	public final static MetadataConceptConstant VHAT_HAS_PARENT_ASSOCIATION_TYPE = 
			new MetadataConceptConstant("has_parent", UUID.fromString("551f4a6d-9c3f-5bfb-829c-4b6e51feb80d")) {};
			
	public final static MetadataConceptConstant VHAT_ABBREVIATION = new MetadataConceptConstant("Abbreviation",
			UUID.fromString("07ffbd1b-a18e-5990-a111-e14baf20e2a3")){};

	public final static MetadataConceptConstant VHAT_FULLY_SPECIFIED_NAME = new MetadataConceptConstant("Fully Specified Name",
			UUID.fromString("e5109ab2-0265-5b73-b0e8-f97da51ec92f")){};

	public final static MetadataConceptConstant VHAT_PREFERRED_NAME = new MetadataConceptConstant("Preferred Name",
			UUID.fromString("7b402b1e-5587-5732-80bf-69be40426df3")){};

	public final static MetadataConceptConstant VHAT_SYNONYM = new MetadataConceptConstant("Synonym",
			UUID.fromString("d517f97f-f3f0-5ea0-bbd7-521bafd8365b")){};

	public final static MetadataConceptConstant VHAT_VISTA_NAME = new MetadataConceptConstant("VistA Name",
			UUID.fromString("22460f8d-952d-5f85-9311-4b571c2110b4")){};
			
	public final static MetadataConceptConstant VHAT_ATTRIBUTE_TYPES = new MetadataConceptConstant("VHAT Attribute Types",
					UUID.fromString("8287530a-b6b0-594d-bf46-252e09434f7e")){};
	
	public final static MetadataConceptConstant VHAT_REFSETS = new MetadataConceptConstant("VHAT Refsets",
			UUID.fromString("99173138-dcaa-5a77-a4eb-311b01991b88")){};
			
	public final static MetadataConceptConstant VHAT_ROOT_CONCEPT = new MetadataConceptConstant("VHAT", 
			UUID.fromString("6e60d7fd-3729-5dd3-9ce7-6d97c8f75447")){};
			
	public final static MetadataConceptConstant VHAT_DESCRIPTION_TYPES = new MetadataConceptConstant("VHAT Description Types", 
			UUID.fromString("fc134ddd-9a15-5540-8fcc-987bf2af9198")){};
			
	public final static MetadataConceptConstant VHAT_ALL_CONCEPTS = new MetadataConceptConstant("All VHAT Concepts", 
			UUID.fromString("30230ef4-41ac-5b76-95be-94ed60b607e3")){};
			
	public final static MetadataConceptConstant VHAT_MISSING_SDO_CODE_SYSTEM_CONCEPTS = new MetadataConceptConstant("Missing SDO Code System Concepts", 
			UUID.fromString("52460eeb-1388-512d-a5e4-fddd64fe0aee")){};

	@Override
	public MetadataConceptConstant[] getConstantsToCreate()
	{
		//Nothing from this class should be generated into the DB, they are made by the DB loaders.
		return new MetadataConceptConstant[] {};
	}

	@Override
	public MetadataConceptConstant[] getConstantsForInfoOnly()
	{
		return new MetadataConceptConstant[] {VHAT_HAS_PARENT_ASSOCIATION_TYPE, VHAT_ABBREVIATION, VHAT_FULLY_SPECIFIED_NAME, VHAT_PREFERRED_NAME, 
				VHAT_SYNONYM, VHAT_VISTA_NAME, VHAT_ATTRIBUTE_TYPES, VHAT_REFSETS, VHAT_ROOT_CONCEPT, VHAT_DESCRIPTION_TYPES, VHAT_ALL_CONCEPTS, 
				VHAT_MISSING_SDO_CODE_SYSTEM_CONCEPTS};
	}
	
	
}

package gov.vha.isaac.ochre.mapping.constants;

import java.util.UUID;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.constants.MetadataConceptConstant;
import gov.vha.isaac.ochre.api.constants.MetadataConceptConstantGroup;
import gov.vha.isaac.ochre.api.constants.MetadataDynamicSememeConstant;
import gov.vha.isaac.ochre.api.constants.ModuleProvidedConstants;

/**
 * 
 * @author darmbrust
 * Unfortunately, due to the indirect use of the LookupService within this class - and the class itself being provided by a LookupService, 
 * we cannot create these constants as static - it leads to recursion in the LookupService init which breaks things.
 * 
 * The 100' view of the mapset / map item setup in the system is:
 * 
 * A Dynamic Sememe is created to represent the Mapset.  This dynamic sememe will (probably) be a child of DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.
 * The data columns on this sememe will at a minimum, carry a 'target concept' column in position 0, (making it a valid association) and carry a 'mapping qualifiers'
 * column in position 1.  (Note, this sememe is describing the mapping items, not the map set) There will optionally be additional fields, which carry any other 
 * types / values that the user specified at the creation of the map set.
 * 
 * A Dynamic Sememe that represents a mapset will carry at least 2, and possibly more sememe annotations.
 *  - {@link DynamicSememeConstants#DYNAMIC_SEMEME_ASSOCIATION_SEMEME}
 *  - {@link #DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE}
 * 
 * Map Items are instances of the map set sememe, with the columns populated as necessary.
 */
@Service
@Singleton
public class IsaacMappingConstants implements ModuleProvidedConstants
{
	private IsaacMappingConstants()
	{
		//Only for HK2 to construct
	}
	
	public static IsaacMappingConstants get()
	{
		return LookupService.getService(IsaacMappingConstants.class);
	}
	
	//This is just used as salt for generating other UUIDs
	public final MetadataConceptConstant MAPPING_NAMESPACE = new MetadataConceptConstant("mapping namespace", 
		UUID.fromString("9b93f811-7b66-5024-bebf-6a7019743e88"),
		"A concept used to hold the UUID used as the namespace ID generation when creating mappings") {};
		
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_PURPOSE = new MetadataConceptConstant("mapping purpose", 
		UUID.fromString("e5de9548-35b9-5e3b-9968-fd9c0a665b51"),
		"Stores the editor stated purpose of the mapping set") 
	{
		{
			setParent(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS);
		}
	};

		
	//These next 3 don't have to be public - just want the hierarchy created during the DB build
	private static final MetadataConceptConstant broader = new MetadataConceptConstant("Broader Than", 
		UUID.fromString("c1068428-a986-5c12-9583-9b2d3a24fdc6")) {};
	
	private static final MetadataConceptConstant exact = new MetadataConceptConstant("Exact", 
		UUID.fromString("8aa6421d-4966-5230-ae5f-aca96ee9c2c1")) {};
	
	private static final MetadataConceptConstant narrower = new MetadataConceptConstant("Narrower Than", 
		UUID.fromString("250d3a08-4f28-5127-8758-e8df4947f89c")) {};
		
	public final MetadataConceptConstantGroup MAPPING_QUALIFIERS = new MetadataConceptConstantGroup("mapping qualifiers", 
		UUID.fromString("83204ca8-bd51-530c-af04-5edbec04a7c6"), 
		"Stores the editor selected mapping qualifier") 
		{
			{
				addChild(broader);
				addChild(exact);
				addChild(narrower);
			}
		};
		
	public final MetadataConceptConstantGroup MAPPING_METADATA = new MetadataConceptConstantGroup("mapping metadata", 
		UUID.fromString("9b5de306-e582-58e3-a23a-0dbf49cbdfe7")) 
	{
		{
			addChild(MAPPING_NAMESPACE);
			addChild(MAPPING_QUALIFIERS);
			setParent(DynamicSememeConstants.get().DYNAMIC_SEMEME_METADATA);
		}
	};
		
	public final MetadataDynamicSememeConstant DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE = new MetadataDynamicSememeConstant("Mapping Sememe Type", 
		UUID.fromString("aa4c75a1-fc69-51c9-88dc-a1a1c7f84e01"),
		"A Sememe used annotate sememe definition concepts that represent a mapping definition.  Mapping sememes will contain a data column named 'target concept', "
		+ "another named 'mapping qualifiers', and may contain additional extended columns.  This sememe carries additional information about the sememe definition.", 
			new DynamicSememeColumnInfo[] {
				new DynamicSememeColumnInfo(0, DYNAMIC_SEMEME_COLUMN_MAPPING_PURPOSE.getUUID(), DynamicSememeDataType.STRING, 
						null, false, true),},
			null) 
	{
		{
			setParent(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES);
		}
	};
	
	public final MetadataDynamicSememeConstant DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION = new MetadataDynamicSememeConstant("Mapping String Extension", 
			UUID.fromString("095f1fae-1fc0-5e5d-8d87-675d712522d5"),
			"A Sememe used annotate sememe definition concepts that represent a mapping definition.  This annotation type carries pair data"
			 + "of a concept used for a label, and a string value.", 
				new DynamicSememeColumnInfo[] {
					new DynamicSememeColumnInfo(0, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_NAME.getUUID(), DynamicSememeDataType.NID, 
							null, true, true),
					new DynamicSememeColumnInfo(1, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_VALUE.getUUID(), DynamicSememeDataType.STRING, 
						null, false, true),},
				null) 
		{
			{
				setParent(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES);
			}
		};
		
		public final MetadataDynamicSememeConstant DYNAMIC_SEMEME_MAPPING_NID_EXTENSION = new MetadataDynamicSememeConstant("Mapping NID Extension", 
				UUID.fromString("276bf07c-4aa7-5176-9853-5f4bd294f163"),
				"A Sememe used annotate sememe definition concepts that represent a mapping definition.  This annotation type carries pair data"
				 + "of a concept used for a label, and a nid value.", 
					new DynamicSememeColumnInfo[] {
						new DynamicSememeColumnInfo(0, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_NAME.getUUID(), DynamicSememeDataType.NID, 
								null, true, true),
						new DynamicSememeColumnInfo(1, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_VALUE.getUUID(), DynamicSememeDataType.NID, 
							null, false, true),},
					null) 
			{
				{
					setParent(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES);
				}
			};

	@Override
	public MetadataConceptConstant[] getConstantsToCreate() {
		return new MetadataConceptConstant[] {DYNAMIC_SEMEME_COLUMN_MAPPING_PURPOSE, DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION, 
				DYNAMIC_SEMEME_MAPPING_NID_EXTENSION, MAPPING_METADATA, DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE};
	} 
}

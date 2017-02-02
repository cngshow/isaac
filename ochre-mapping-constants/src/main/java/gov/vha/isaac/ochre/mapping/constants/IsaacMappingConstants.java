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
	
	private static IsaacMappingConstants cache_;
	
	public static IsaacMappingConstants get()
	{
		if (cache_ == null)
		{
			cache_ = LookupService.getService(IsaacMappingConstants.class);
		}
		return cache_;
	}
	
	//This is just used as salt for generating other UUIDs
	public final MetadataConceptConstant MAPPING_NAMESPACE = new MetadataConceptConstant("mapping namespace", 
		UUID.fromString("9b93f811-7b66-5024-bebf-6a7019743e88"),
		"A concept used to hold the UUID used as the namespace ID generation when creating mappings") {};

	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_DISPLAY_FIELDS = new MetadataConceptConstant("mapping display fields", 
			UUID.fromString("4e627b9c-cecb-5563-82fc-cb0ee25113b1"),
			"Stores the mapping field ids in an array corresponding to an ordered displayable row",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 

	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_PURPOSE = new MetadataConceptConstant("mapping purpose", 
		UUID.fromString("e5de9548-35b9-5e3b-9968-fd9c0a665b51"),
		"Stores the editor stated purpose of the mapping set",
		DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
	
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_EQUIVALENCE_TYPE = new MetadataConceptConstant("mapping equivalence type", 
			UUID.fromString("8e84c657-5f47-51b8-8ebf-89a9d025a9ef"),
			"Stores the editor selected mapping equivalence type",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
		
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_SEQUENCE = new MetadataConceptConstant("mapping sequence", 
			UUID.fromString("83e8e74e-596e-5622-b945-17dbe8e9c05c"),
			"The sequence value attached to the mapping",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
	
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_GROUPING = new MetadataConceptConstant("mapping grouping", 
			UUID.fromString("8d76ead7-6c75-5d25-84d4-ca76d928f8a6"),
			"The grouping attached to the mapping",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
	
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_EFFECTIVE_DATE = new MetadataConceptConstant("mapping effective date", 
			UUID.fromString("a332f7bc-f7c1-58cd-a834-cd2660b984da"),
			"The effective date attached to the mapping",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
	
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_GEM_FLAGS = new MetadataConceptConstant("mapping GEM Flags", 
			UUID.fromString("21bab5a4-18a5-5848-905d-2d99305090d9"),
			"The General Equivalence Mappings value",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 

		
	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_BROAD_TO_NARROW = new MetadataConceptConstant("Broad to Narrow", 
		UUID.fromString("c1068428-a986-5c12-9583-9b2d3a24fdc6")) {};
	
	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_ONE_TO_ONE = new MetadataConceptConstant("One to One", 
		UUID.fromString("8aa6421d-4966-5230-ae5f-aca96ee9c2c1")) {};
	
	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_NARROW_TO_BROAD = new MetadataConceptConstant("Narrow to Broad", 
		UUID.fromString("250d3a08-4f28-5127-8758-e8df4947f89c")) {};

	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_UNMAPPABLE = new MetadataConceptConstant("Unmappable", 
		UUID.fromString("e5f7f98f-9607-55a7-bbc4-25f2e61df23d")) {};

	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_PARTIAL = new MetadataConceptConstant("Partial", 
		UUID.fromString("a7f9574c-8e8b-515d-9c21-9896063cc3b8")) {};
			
	public final MetadataConceptConstantGroup MAPPING_EQUIVALENCE_TYPES = new MetadataConceptConstantGroup("mapping qualifiers", 
		UUID.fromString("83204ca8-bd51-530c-af04-5edbec04a7c6"), 
		"A grouping of different types types of allowed mapping qualifiers") 
		{
			{
				addChild(MAPPING_EQUIVALENCE_TYPE_BROAD_TO_NARROW);
				addChild(MAPPING_EQUIVALENCE_TYPE_ONE_TO_ONE);
				addChild(MAPPING_EQUIVALENCE_TYPE_NARROW_TO_BROAD);
				addChild(MAPPING_EQUIVALENCE_TYPE_UNMAPPABLE);
				addChild(MAPPING_EQUIVALENCE_TYPE_PARTIAL);
			}
		};
		
	public final MetadataConceptConstant MAPPING_SOURCE_CODE_SYSTEM = new MetadataConceptConstant("mapping source code system", 
				UUID.fromString("32e30e80-3fac-5317-80cf-d85eab22fa9e"),
				"A concept used to annotate the source code system of a map set") {};
	
	public final MetadataConceptConstant MAPPING_SOURCE_CODE_SYSTEM_VERSION = new MetadataConceptConstant("mapping source code system version", 
			UUID.fromString("5b3479cb-25b2-5965-a031-54238588218f"),
			"A concept used to annotate the source code system version of a map set") {};
				
	public final MetadataConceptConstant MAPPING_TARGET_CODE_SYSTEM = new MetadataConceptConstant("mapping target code system", 
			UUID.fromString("6b31a67a-7e6d-57c0-8609-52912076fce8"),
			"A concept used to annotate the target code system of a map set") {};
			
	public final MetadataConceptConstant MAPPING_TARGET_CODE_SYSTEM_VERSION = new MetadataConceptConstant("mapping target code system version", 
			UUID.fromString("b5165f68-b934-5c79-ac71-bd5375f7c809"),
			"A concept used to annotate the target code system version of a map set") {};
		
	public final MetadataConceptConstantGroup MAPPING_METADATA = new MetadataConceptConstantGroup("mapping metadata", 
		UUID.fromString("9b5de306-e582-58e3-a23a-0dbf49cbdfe7")) 
	{
		{
			addChild(MAPPING_NAMESPACE);
			addChild(MAPPING_EQUIVALENCE_TYPES);
			addChild(MAPPING_SOURCE_CODE_SYSTEM);
			addChild(MAPPING_SOURCE_CODE_SYSTEM_VERSION);
			addChild(MAPPING_TARGET_CODE_SYSTEM);
			addChild(MAPPING_TARGET_CODE_SYSTEM_VERSION);
			setParent(DynamicSememeConstants.get().DYNAMIC_SEMEME_METADATA);
		}
	};
	
	/** 
	 * see {@link DynamicSememeConstants#DYNAMIC_SEMEME_MAPPING_DISPLAY_FIELDS}
	 * see {@link #DYNAMIC_SEMEME_COLUMN_MAPPING_DISPLAY_FIELDS}
	 */
	public final MetadataDynamicSememeConstant DYNAMIC_SEMEME_MAPPING_DISPLAY_FIELDS = new MetadataDynamicSememeConstant("Mapping Display Fields", 
		UUID.fromString("8d6463c2-b0ec-5e34-a882-1208d52703ea"),
		"A Sememe used to annotate Mapping Set concepts. This sememe carries field identifiers for organizing an ordered columnar display row.", 
			new DynamicSememeColumnInfo[] {
				new DynamicSememeColumnInfo(0, DYNAMIC_SEMEME_COLUMN_MAPPING_DISPLAY_FIELDS.getUUID(), DynamicSememeDataType.ARRAY, 
						null, true, true),},
			null) 
	{
		{
			setParent(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES);
		}
	};

	/** 
	 * see {@link DynamicSememeConstants#DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT}
	 * see {@link #MAPPING_EQUIVALENCE_TYPES}
	 */
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
		return new MetadataConceptConstant[] {DYNAMIC_SEMEME_COLUMN_MAPPING_DISPLAY_FIELDS, DYNAMIC_SEMEME_MAPPING_DISPLAY_FIELDS, DYNAMIC_SEMEME_COLUMN_MAPPING_PURPOSE, DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION, 
				DYNAMIC_SEMEME_MAPPING_NID_EXTENSION, MAPPING_METADATA, DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE, DYNAMIC_SEMEME_COLUMN_MAPPING_EQUIVALENCE_TYPE,
				DYNAMIC_SEMEME_COLUMN_MAPPING_SEQUENCE, DYNAMIC_SEMEME_COLUMN_MAPPING_GROUPING, DYNAMIC_SEMEME_COLUMN_MAPPING_EFFECTIVE_DATE, 
				DYNAMIC_SEMEME_COLUMN_MAPPING_GEM_FLAGS};
	} 
}

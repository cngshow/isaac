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
 * The data columns on this sememe will at a minimum, carry a 'target concept' column in position 0, (making it a valid association) and carry a 'mapping equivalence types'
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

	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_DISPLAY_FIELDS = new MetadataConceptConstant("Display Fields", 
			UUID.fromString("4e627b9c-cecb-5563-82fc-cb0ee25113b1"),
			"Stores the mapping field ids in an array corresponding to an ordered displayable row",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 

	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_PURPOSE = new MetadataConceptConstant("Purpose", 
		UUID.fromString("e5de9548-35b9-5e3b-9968-fd9c0a665b51"),
		"Stores the editor stated purpose of the mapping set",
		DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
	
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_EQUIVALENCE_TYPE = new MetadataConceptConstant("Equivalence Type", 
			UUID.fromString("8e84c657-5f47-51b8-8ebf-89a9d025a9ef"),
			"Stores the editor selected mapping equivalence type",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
		
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_SEQUENCE = new MetadataConceptConstant("Sequence", 
			UUID.fromString("83e8e74e-596e-5622-b945-17dbe8e9c05c"),
			"The sequence value attached to the mapping",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
	
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_GROUPING = new MetadataConceptConstant("Grouping", 
			UUID.fromString("8d76ead7-6c75-5d25-84d4-ca76d928f8a6"),
			"The grouping attached to the mapping",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
	
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_EFFECTIVE_DATE = new MetadataConceptConstant("Effective Date", 
			UUID.fromString("a332f7bc-f7c1-58cd-a834-cd2660b984da"),
			"The effective date attached to the mapping",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 
	
	public final MetadataConceptConstant DYNAMIC_SEMEME_COLUMN_MAPPING_GEM_FLAGS = new MetadataConceptConstant("GEM Flags", 
			UUID.fromString("21bab5a4-18a5-5848-905d-2d99305090d9"),
			"The General Equivalence Mappings value",
			DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS) {}; 

	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_BROAD_TO_NARROW = new MetadataConceptConstant("Broad to Narrow", 
		UUID.fromString("c1068428-a986-5c12-9583-9b2d3a24fdc6"), "source is less specific than target") {};
	
	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_EXACT = new MetadataConceptConstant("Exact", 
		UUID.fromString("8aa6421d-4966-5230-ae5f-aca96ee9c2c1"), "source and target are semantic or exact lexical match") {};
	
	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_NARROW_TO_BROAD = new MetadataConceptConstant("Narrow to Broad", 
		UUID.fromString("250d3a08-4f28-5127-8758-e8df4947f89c"), "source is more specific than target") {};

	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_UNMAPPABLE = new MetadataConceptConstant("Unmappable", 
		UUID.fromString("e5f7f98f-9607-55a7-bbc4-25f2e61df23d"), "source cannot be assigned to an appropriate target") {};

	public static final MetadataConceptConstant MAPPING_EQUIVALENCE_TYPE_PARTIAL = new MetadataConceptConstant("Partial", 
		UUID.fromString("a7f9574c-8e8b-515d-9c21-9896063cc3b8"), "partial overlap between source and target and reliability") {};
			
	public final MetadataConceptConstantGroup MAPPING_EQUIVALENCE_TYPES = new MetadataConceptConstantGroup("Equivalence Types", 
		UUID.fromString("83204ca8-bd51-530c-af04-5edbec04a7c6"), 
		"A grouping of different types types of allowed mapping equivalence types") 
		{
			{
				addChild(MAPPING_EQUIVALENCE_TYPE_BROAD_TO_NARROW);
				addChild(MAPPING_EQUIVALENCE_TYPE_EXACT);
				addChild(MAPPING_EQUIVALENCE_TYPE_NARROW_TO_BROAD);
				addChild(MAPPING_EQUIVALENCE_TYPE_UNMAPPABLE);
				addChild(MAPPING_EQUIVALENCE_TYPE_PARTIAL);
			}
		};
		
	public final MetadataConceptConstant MAPPING_SOURCE_CODE_SYSTEM = new MetadataConceptConstant("Source Code System", 
				UUID.fromString("32e30e80-3fac-5317-80cf-d85eab22fa9e"),
				"A concept used to annotate the source code system of a map set") {};
	
	public final MetadataConceptConstant MAPPING_SOURCE_CODE_SYSTEM_VERSION = new MetadataConceptConstant("Source Code Version", 
			UUID.fromString("5b3479cb-25b2-5965-a031-54238588218f"),
			"A concept used to annotate the source code system version of a map set") {};
				
	public final MetadataConceptConstant MAPPING_TARGET_CODE_SYSTEM = new MetadataConceptConstant("Target Code System", 
			UUID.fromString("6b31a67a-7e6d-57c0-8609-52912076fce8"),
			"A concept used to annotate the target code system of a map set") {};
			
	public final MetadataConceptConstant MAPPING_TARGET_CODE_SYSTEM_VERSION = new MetadataConceptConstant("Target Code Version", 
			UUID.fromString("b5165f68-b934-5c79-ac71-bd5375f7c809"),
			"A concept used to annotate the target code system version of a map set") {};
			
	public final MetadataConceptConstant MAPPING_IPO_MAP_PATHWAY_ID = new MetadataConceptConstant("Map Pathway ID", 
			UUID.fromString("e90d3645-8d4a-5ca7-b6ea-78fbc2d85084"),
			"A concept used to annotate the MapPathway ID field of an IPO map set") {};
			
	public final MetadataConceptConstant MAPPING_IPO_VA_STATION_NUMBER = new MetadataConceptConstant("VA Station Number", 
			UUID.fromString("172ee183-a183-5f6c-8527-afbc658dd49f"),
			"A concept used to annotate the VA_Station_Number field of an IPO map set") {};
			
	public final MetadataConceptConstant MAPPING_IPO_VA_STATION_IEN = new MetadataConceptConstant("VA Station IEN", 
			UUID.fromString("416f763f-12b1-55a1-bf32-8f6001fc0eff"),
			"A concept used to annotate the VA_Station_IEN field of an IPO map set") {};

	public final MetadataConceptConstant MAPPING_IPO_TARGET_TERMINOLOGY_DATE = new MetadataConceptConstant("Target Terminology Date", 
			UUID.fromString("d8f2ba8a-c81d-5acf-8c2b-79af9fd645e8"),
			"A concept used to annotate the Target_Terminology_Date field of an IPO map set") {};
		
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
			addChild(MAPPING_IPO_MAP_PATHWAY_ID);
			addChild(MAPPING_IPO_VA_STATION_NUMBER);
			addChild(MAPPING_IPO_VA_STATION_IEN);
			addChild(MAPPING_IPO_TARGET_TERMINOLOGY_DATE);
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
	
	public final MetadataConceptConstant MAPPING_CODE_DESCRIPTION = new MetadataConceptConstant("Description", 
			UUID.fromString("87118daf-d28c-55fb-8657-cd6bc8425600"),
			"A concept used as a placeholder in the computable columns configuration of maps") 
	{
		{
			setParent(DYNAMIC_SEMEME_MAPPING_DISPLAY_FIELDS);
		}
	};
	
	public final MetadataConceptConstant MAPPING_NAME = new MetadataConceptConstant("Name", 
			UUID.fromString("6406b947-0b71-5a56-a95f-4c7940c8f3e0"),
			"A concept used in a label on the mapping screen") 
	{
		{
			setParent(DYNAMIC_SEMEME_MAPPING_DISPLAY_FIELDS);
		}
	};

	/** 
	 * see {@link DynamicSememeConstants#DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT}
	 * see {@link #MAPPING_EQUIVALENCE_TYPES}
	 */
	public final MetadataDynamicSememeConstant DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE = new MetadataDynamicSememeConstant("Mapping Sememe Type", 
		UUID.fromString("aa4c75a1-fc69-51c9-88dc-a1a1c7f84e01"),
		"A Sememe used annotate sememe definition concepts that represent a mapping definition.  Mapping sememes will contain a data column named 'target concept', "
		+ "another named 'mapping equivalence type', and may contain additional extended columns.  This sememe carries additional information about the sememe definition.", 
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
				DYNAMIC_SEMEME_COLUMN_MAPPING_GEM_FLAGS, MAPPING_CODE_DESCRIPTION, MAPPING_NAME};
	} 
}

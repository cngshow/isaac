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
package gov.va.oia.terminology.converters.sharedUtils;

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.NecessarySet;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.SomeRole;
import java.beans.PropertyVetoException;
import java.io.DataOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import gov.va.oia.terminology.converters.sharedUtils.gson.MultipleDataWriterService;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Associations;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Descriptions;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Refsets;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Relations;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Skip;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.Property;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyAssociation;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.ValuePropertyPair;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import gov.va.oia.terminology.converters.sharedUtils.stats.LoadStats;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LanguageCode;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.collections.UuidIntMapMap;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilderService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeInteger;
import gov.vha.isaac.ochre.api.constants.Constants;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataWriterService;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUtilityImpl;
import gov.vha.isaac.ochre.model.concept.ConceptChronologyImpl;
import gov.vha.isaac.ochre.model.configuration.LogicCoordinates;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;
import gov.vha.isaac.ochre.model.sememe.SememeChronologyImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeArrayImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeIntegerImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUIDImpl;
import gov.vha.isaac.ochre.model.sememe.version.DescriptionSememeImpl;

/**
 * 
 * {@link EConceptUtility}
 * 
 * Various constants and methods for building up workbench TtkConceptChronicles.
 * 
 * A much easier interfaces to use than trek - takes care of boilerplate stuff for you.
 * Also, forces consistency in how things are converted.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class EConceptUtility
{
	public static enum DescriptionType 
	{
		FSN, SYNONYM, DEFINITION;
		
		public ConceptSpecification getConceptSpec()
		{
			if (DescriptionType.FSN == this)
			{
				return MetaData.FULLY_SPECIFIED_NAME;
			}
			else if (DescriptionType.SYNONYM == this)
			{
				return MetaData.SYNONYM;
			}
			else if (DescriptionType.DEFINITION == this)
			{
				return MetaData.DEFINITION_DESCRIPTION_TYPE;
			}
			else
			{
				throw new RuntimeException("Unsupported descriptiontype '" + this + "'");
			}
		}

		public static DescriptionType parse(UUID typeId)
		{
			if (MetaData.FULLY_SPECIFIED_NAME.getPrimordialUuid().equals(typeId))
			{
				return FSN;
			}
			else if (MetaData.SYNONYM.getPrimordialUuid().equals(typeId))
			{
				return SYNONYM;
			}
			if (MetaData.DEFINITION_DESCRIPTION_TYPE.getPrimordialUuid().equals(typeId))
			{
				return DEFINITION;
			}
			throw new RuntimeException("Unknown description type UUID " + typeId);
		}
	};

	private final int authorSeq_;
	private final int terminologyPathSeq_;
	private final long defaultTime_;
	
	private final static UUID isARelUuid_ = MetaData.IS_A.getPrimordialUuid();
	
	private int moduleSeq_ = 0;
	private HashMap<UUID, DynamicSememeColumnInfo[]> refexAllowedColumnTypes_ = new HashMap<>();
	
	private ConceptBuilderService conceptBuilderService_;
	private LogicalExpressionBuilderService expressionBuilderService_;
	private SememeBuilderService<?> sememeBuilderService_;
	protected static StampCoordinate readBackStamp_;
	
	private BinaryDataWriterService writer_;

	private LoadStats ls_ = new LoadStats();

	/**
	 * Creates and stores the path concept - sets up the various namespace details.
	 * @param moduleToCreate - if present, a new concept will be created, using this value as the FSN / preferred term for use as the module
	 * @param preExistingModule - if moduleToCreate is not present, lookup the concept with this UUID to use as the module.
	 * @param outputFile - The path to write the output file to
	 * @param defaultTime - the timestamp to place on created elements, when no other timestamp is specified on the element itself.
	 * @throws Exception
	 */
	public EConceptUtility(Optional<String> moduleToCreate, Optional<ConceptSpecification> preExistingModule, File outputDirectory, 
			String outputFileNameWithoutExtension, boolean outputGson, long defaultTime) throws Exception
	{
		UuidIntMapMap.NID_TO_UUID_CACHE_SIZE = 2500000;
		File file = new File(outputDirectory, "isaac-db");
		//make sure this is empty
		FileUtils.deleteDirectory(file);
		
		System.setProperty(Constants.DATA_STORE_ROOT_LOCATION_PROPERTY, file.getCanonicalPath());

		LookupService.startupIsaac();
		
		authorSeq_ = MetaData.USER.getConceptSequence();
		terminologyPathSeq_ = MetaData.DEVELOPMENT_PATH.getConceptSequence();
		
		ConverterUUID.addMapping("isA", isARelUuid_);
		ConverterUUID.addMapping("Synonym", MetaData.SYNONYM.getPrimordialUuid());
		ConverterUUID.addMapping("Fully Specified Name", MetaData.FULLY_SPECIFIED_NAME.getPrimordialUuid());
		
		//TODO automate this somehow....
		registerDynamicSememeColumnInfo(IsaacMetadataConstants.DYNAMIC_SEMEME_EXTENSION_DEFINITION.getUUID(), 
				IsaacMetadataConstants.DYNAMIC_SEMEME_EXTENSION_DEFINITION.getDynamicSememeColumns());
		registerDynamicSememeColumnInfo(IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getUUID(), 
				IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getDynamicSememeColumns());
		registerDynamicSememeColumnInfo(IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getUUID(), 
				IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getDynamicSememeColumns());
		registerDynamicSememeColumnInfo(IsaacMetadataConstants.DYNAMIC_SEMEME_REFERENCED_COMPONENT_RESTRICTION.getUUID(), 
				IsaacMetadataConstants.DYNAMIC_SEMEME_REFERENCED_COMPONENT_RESTRICTION.getDynamicSememeColumns());
		registerDynamicSememeColumnInfo(IsaacMetadataConstants.DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getUUID(), 
				IsaacMetadataConstants.DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getDynamicSememeColumns());
		registerDynamicSememeColumnInfo(IsaacMetadataConstants.DYNAMIC_SEMEME_INDEX_CONFIGURATION.getUUID(), 
				IsaacMetadataConstants.DYNAMIC_SEMEME_INDEX_CONFIGURATION.getDynamicSememeColumns());
		registerDynamicSememeColumnInfo(IsaacMetadataConstants.DYNAMIC_SEMEME_COMMENT_ATTRIBUTE.getUUID(), 
				IsaacMetadataConstants.DYNAMIC_SEMEME_COMMENT_ATTRIBUTE.getDynamicSememeColumns());
		
		
		conceptBuilderService_ = Get.conceptBuilderService();
		conceptBuilderService_.setDefaultLanguageForDescriptions(MetaData.ENGLISH_LANGUAGE);
		conceptBuilderService_.setDefaultDialectAssemblageForDescriptions(MetaData.US_ENGLISH_DIALECT);
		conceptBuilderService_.setDefaultLogicCoordinate(LogicCoordinates.getStandardElProfile());

		expressionBuilderService_ = Get.logicalExpressionBuilderService();
		
		sememeBuilderService_ = Get.sememeBuilderService();
		
		defaultTime_ = defaultTime;
		
		UUID moduleUUID = moduleToCreate.isPresent() ? UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC, moduleToCreate.get()) : 
			preExistingModule.get().getPrimordialUuid();
		
		//Just use the module as the namespace
		ConverterUUID.configureNamespace(moduleUUID);
		
		writer_ = new MultipleDataWriterService(
				outputGson ? Optional.of(new File(outputDirectory, outputFileNameWithoutExtension + ".gson")) : Optional.empty(),
						Optional.of(new File(outputDirectory, outputFileNameWithoutExtension + ".ibdf").toPath()));
		
		if (moduleToCreate.isPresent())
		{
			ConceptChronology<? extends ConceptVersion<?>> module =  createConcept(moduleUUID, moduleToCreate.get(), MetaData.MODULE.getPrimordialUuid());
			moduleSeq_ = module.getConceptSequence();
		}
		else
		{
			moduleSeq_ = preExistingModule.get().getConceptSequence();
		}
		
		StampPosition stampPosition = new StampPositionImpl(Long.MAX_VALUE, MetaData.DEVELOPMENT_PATH.getConceptSequence());
		readBackStamp_ = new StampCoordinateImpl(StampPrecedence.PATH, stampPosition, ConceptSequenceSet.EMPTY, State.ANY_STATE_SET);
		
		ConsoleUtil.println("Loading with module '" + moduleSeq_+ " on DEVELOPMENT path");
		
	}

	/**
	 * Create a concept, automatically setting as many fields as possible (adds a description, calculates
	 * the UUID, status current, etc)
	 */
	public ConceptChronology<? extends ConceptVersion<?>> createConcept(String preferredDescription)
	{
		return createConcept(ConverterUUID.createNamespaceUUIDFromString(preferredDescription), preferredDescription);
	}

	/**
	 * Create a concept, link it to a parent via is_a, setting as many fields as possible automatically.
	 */
	public ConceptChronology<? extends ConceptVersion<?>> createConcept(String name, UUID parentConceptPrimordial)
	{
		ConceptChronology<? extends ConceptVersion<?>> concept = createConcept(name);
		addRelationship(ComponentReference.fromConcept(concept), parentConceptPrimordial);
		return concept;
	}

	/**
	 * Create a concept, link it to a parent via is_a, setting as many fields as possible automatically.
	 */
	public ConceptChronology<? extends ConceptVersion<?>> createConcept(UUID conceptPrimordialUuid, String name, UUID relParentPrimordial)
	{
		ConceptChronology<? extends ConceptVersion<?>> concept = createConcept(conceptPrimordialUuid, name);
		addRelationship(ComponentReference.fromConcept(concept), relParentPrimordial);
		return concept;
	}
	
	public ConceptChronology<? extends ConceptVersion<?>> createConcept(UUID conceptPrimordialUuid)
	{
		return createConcept(conceptPrimordialUuid, (Long)null, State.ACTIVE, null);
	}

	/**
	 * Create a concept, automatically setting as many fields as possible (adds a description (en US)
	 * status current, etc
	 */
	public ConceptChronology<? extends ConceptVersion<?>> createConcept(UUID conceptPrimordialUuid, String preferredDescription)
	{
		return createConcept(conceptPrimordialUuid, preferredDescription, null, State.ACTIVE);
	}

	/**
	 * Create a concept, automatically setting as many fields as possible (adds a description (en US))
	 * 
	 * @param time - set to now if null
	 */
	public ConceptChronology<? extends ConceptVersion<?>> createConcept(UUID conceptPrimordialUuid, String preferredDescription, Long time, State status)
	{
		ConceptChronology<? extends ConceptVersion<?>> conceptChronology = createConcept(conceptPrimordialUuid, time, status, null);
		addFullySpecifiedName(conceptChronology, preferredDescription);
		addDescription(ComponentReference.fromConcept(conceptChronology), preferredDescription, DescriptionType.SYNONYM, true, null, null, State.ACTIVE);
		return conceptChronology;
	}

	/**
	 * Just create a concept.
	 * 
	 * @param conceptPrimordialUuid
	 * @param time - if null, set to default
	 * @param status - if null, set to current
	 * @param module - if null, uses the default
	 * @return
	 */
	public ConceptChronology<? extends ConceptVersion<?>> createConcept(UUID conceptPrimordialUuid, Long time, State status, UUID module) 
	{
		ConceptChronologyImpl conceptChronology = (ConceptChronologyImpl) Get.conceptService().getConcept(conceptPrimordialUuid);
		conceptChronology.createMutableVersion(createStamp(status, time, module));
		writer_.put(conceptChronology);
		ls_.addConcept();
		return conceptChronology;
	}

	/**
	 * Add a workbench official "Fully Specified Name".  Convenience method for adding a description of type FSN
	 */
	public SememeChronology<DescriptionSememe<?>> addFullySpecifiedName(ConceptChronology<? extends ConceptVersion<?>> concept, String fullySpecifiedName)
	{
		return addDescription(ComponentReference.fromConcept(concept), fullySpecifiedName, DescriptionType.FSN, true, null, null, State.ACTIVE);
	}
	
	
	/**
	 * Add a batch of WB descriptions, following WB rules in always generating a FSN (picking the value based on the propertySubType order). 
	 * And then adding other types as specified by the propertySubType value, setting preferred / acceptable according to their ranking. 
	 */
	public List<SememeChronology<DescriptionSememe<?>>> addDescriptions(ComponentReference concept, List<? extends ValuePropertyPair> descriptions)
	{
		ArrayList<SememeChronology<DescriptionSememe<?>>> result = new ArrayList<>(descriptions.size());
		Collections.sort(descriptions);
		
		boolean haveFSN = false;
		boolean havePreferredSynonym = false;
		boolean havePreferredDefinition = false;
		for (ValuePropertyPair vpp : descriptions)
		{
			DescriptionType descriptionType = null;
			boolean preferred;
			
			if (!haveFSN)
			{
				descriptionType = DescriptionType.FSN;
				preferred = true;
				haveFSN = true;
			}
			else
			{
				if (vpp.getProperty().getPropertySubType() < BPT_Descriptions.SYNONYM)
				{
					descriptionType = DescriptionType.FSN;
					preferred = false;  //true case is handled above
				}
				else if (vpp.getProperty().getPropertySubType() >= BPT_Descriptions.SYNONYM && 
						(vpp.getProperty().getPropertySubType() < BPT_Descriptions.DEFINITION || vpp.getProperty().getPropertySubType() == Integer.MAX_VALUE))
				{
					descriptionType = DescriptionType.SYNONYM;
					if (!havePreferredSynonym)
					{
						preferred = true;
						havePreferredSynonym = true;
					}
					else
					{
						preferred = false;
					}
				}
				else if (vpp.getProperty().getPropertySubType() >= BPT_Descriptions.DEFINITION)
				{
					descriptionType = DescriptionType.DEFINITION;
					if (!havePreferredDefinition)
					{
						preferred = true;
						havePreferredDefinition = true;
					}
					else
					{
						preferred = false;
					}
				}
				else
				{
					throw new RuntimeException("Unexpected error");
				}
			}
			
			if (!(vpp.getProperty().getPropertyType() instanceof BPT_Descriptions))
			{
				throw new RuntimeException("This method requires properties that have a parent that are an instance of BPT_Descriptions");
			}
			BPT_Descriptions descPropertyType = (BPT_Descriptions) vpp.getProperty().getPropertyType();
			
			result.add(addDescription(concept, vpp.getUUID(), vpp.getValue(), descriptionType, preferred, null, null, null, null, vpp.getProperty().getUUID(), 
					descPropertyType.getPropertyTypeReferenceSetUUID(), (vpp.isDisabled() ? State.INACTIVE : State.ACTIVE), null));
		}
		
		return result;
	}
	
	/**
	 * Add a description to the concept.  UUID for the description is calculated from the target concept, description value, type, and preferred flag.
	 */
	public SememeChronology<DescriptionSememe<?>> addDescription(ComponentReference concept, String descriptionValue, DescriptionType wbDescriptionType, 
			boolean preferred, UUID sourceDescriptionTypeUUID, UUID sourceDescriptionRefsetUUID, State status)
	{
		return addDescription(concept, null, descriptionValue, wbDescriptionType, preferred, null, null, null, null, sourceDescriptionTypeUUID, 
				sourceDescriptionRefsetUUID, status, null);
	}
	

	/**
	 * Add a description to the concept.
	 * 
	 * @param concept - the concept to add this description to
	 * @param descriptionPrimordialUUID - if not supplied, created from the concept UUID and the description value and description type
	 * @param descriptionValue - the text value
	 * @param wbDescriptionType - the type of the description
	 * @param preferred - true, false, or null to not create any acceptability entry see {@link #addDescriptionAcceptibility()}
	 * @param dialect - ignored if @param preferred is set to null.  if null, defaults to {@link MetaData#US_ENGLISH_DIALECT}
	 * @param caseSignificant - if null, defaults to {@link MetaData#DESCRIPTION_NOT_CASE_SENSITIVE}
	 * @param languageCode - if null, uses {@link MetaData#ENGLISH_LANGUAGE}
	 * @param module - if null, uses the default from the EConceptUtility instance
	 * @param sourceDescriptionTypeUUID - this optional value is attached as the 'data' of the source annotation.
	 * @param sourceDescriptionRefsetUUID - if null, this and sourceDescriptionTypeUUID are ignored.  This is the ID of the terminology 
	 * specific refset.
	 * @param state active / inactive
	 * @param time - defaults to concept time
	 */
	@SuppressWarnings("unchecked")
	public SememeChronology<DescriptionSememe<?>> addDescription(ComponentReference concept, UUID descriptionPrimordialUUID, String descriptionValue, 
			DescriptionType wbDescriptionType, Boolean preferred, UUID dialect, UUID caseSignificant, UUID languageCode, UUID module, 
			UUID sourceDescriptionTypeUUID, UUID sourceDescriptionRefsetUUID, State state, Long time)
	{
		if (descriptionPrimordialUUID == null)
		{
			descriptionPrimordialUUID = ConverterUUID.createNamespaceUUIDFromStrings(concept.getPrimordialUuid().toString(), descriptionValue, 
					wbDescriptionType.name());
		}
		
		@SuppressWarnings({ "rawtypes" }) 
		SememeBuilder<? extends SememeChronology<? extends DescriptionSememe>> descBuilder = sememeBuilderService_.getDescriptionSememeBuilder(
						Get.identifierService().getConceptSequenceForUuids(caseSignificant == null ? MetaData.DESCRIPTION_NOT_CASE_SENSITIVE.getPrimordialUuid() : caseSignificant),
						languageCode == null ? MetaData.ENGLISH_LANGUAGE.getConceptSequence() : Get.identifierService().getConceptSequenceForUuids(languageCode),
						wbDescriptionType.getConceptSpec().getConceptSequence(), 
						descriptionValue, 
						concept.getNid());
		descBuilder.setPrimordialUuid(descriptionPrimordialUUID);

		ArrayList<OchreExternalizable> builtObjects = new ArrayList<>();
		
		SememeChronology<DescriptionSememe<?>> newDescription = (SememeChronology<DescriptionSememe<?>>)
				descBuilder.build(
						createStamp(state, selectTime(time, concept), module), 
						builtObjects);

		if (preferred == null)
		{
			//noop
		}
		else
		{
			sememeBuilderService_.getComponentSememeBuilder(preferred ? TermAux.PREFERRED.getNid() : TermAux.ACCEPTABLE.getNid(), newDescription.getNid(), 
						Get.identifierService().getConceptSequenceForUuids(dialect == null ? MetaData.US_ENGLISH_DIALECT.getPrimordialUuid() : dialect))
					.build(createStamp(state, selectTime(time, concept), module), builtObjects);
		}

		
		for (OchreExternalizable ochreObject : builtObjects)
		{
			writer_.put(ochreObject);
		}
		
		ls_.addDescription(wbDescriptionType.name() + (sourceDescriptionTypeUUID == null ? (sourceDescriptionRefsetUUID == null ? "" : ":-member-:") :
				":" + getOriginStringForUuid(sourceDescriptionTypeUUID) + ":")
					+ (sourceDescriptionRefsetUUID == null ? "" : getOriginStringForUuid(sourceDescriptionRefsetUUID)));
		
		if (sourceDescriptionRefsetUUID != null)
		{
			addAnnotation(ComponentReference.fromChronology(newDescription), null, (sourceDescriptionTypeUUID == null ? null : new DynamicSememeUUIDImpl(sourceDescriptionTypeUUID)),
				sourceDescriptionRefsetUUID, null, null);
		}
		
		return newDescription;
	}
	
	/**
	 * Add a description to the concept.
	 * 
	 * @param acceptabilityPrimordialUUID - if not supplied, created from the description UUID, dialectRefsetg and preferred flag
	 * @param dialectRefset - A UUID for a refset like MetaData.US_ENGLISH_DIALECT
	 * @param preferred - true for preferred, false for acceptable
	 * @param state - 
	 * @param time - if null, uses the description time
	 * @param module - optional
	 */
	public SememeChronology<ComponentNidSememe<?>> addDescriptionAcceptibility(ComponentReference description, UUID acceptabilityPrimordialUUID, 
			UUID dialectRefset, boolean preferred, State state, Long time, UUID module)
	{
		@SuppressWarnings("rawtypes")
		SememeBuilderService sememeBuilderService = LookupService.getService(SememeBuilderService.class);
		@SuppressWarnings("rawtypes")
		SememeBuilder sb = sememeBuilderService.getComponentSememeBuilder(preferred ? TermAux.PREFERRED.getNid() : TermAux.ACCEPTABLE.getNid(),
				description.getNid(), Get.identifierService().getConceptSequenceForUuids(dialectRefset));
		
		if (acceptabilityPrimordialUUID == null)
		{
			//TODO not sure if preferred should be part of UUID
			acceptabilityPrimordialUUID = ConverterUUID.createNamespaceUUIDFromStrings(description.getPrimordialUuid().toString(), 
					dialectRefset.toString(), preferred + "");
		}
		
		sb.setPrimordialUuid(acceptabilityPrimordialUUID);
		
		ArrayList<OchreExternalizable> builtObjects = new ArrayList<>();
		@SuppressWarnings("unchecked")
		SememeChronology<ComponentNidSememe<?>> sc = (SememeChronology<ComponentNidSememe<?>>)sb.build(
				createStamp(state, selectTime(time, description), module), builtObjects);
		
		ls_.addAnnotation("Description", getOriginStringForUuid(dialectRefset));
		return sc;
	}
	
	/**
	 * Add an alternate ID to the concept.
	 */
	public void addUUID(UUID existingUUID, UUID newUUID)
	{
		ConceptChronologyImpl conceptChronology = (ConceptChronologyImpl) Get.conceptService().getConcept(existingUUID);
		conceptChronology.addAdditionalUuids(newUUID);
		writer_.put(conceptChronology);
	}
	
	/**
	 * uses the concept time, UUID is created from the component UUID, the annotation value and type.
	 */
	public SememeChronology<DynamicSememe<?>> addStringAnnotation(ComponentReference referencedComponent, String annotationValue, UUID refsetUuid, State status)
	{
		return addAnnotation(referencedComponent, null, new DynamicSememeData[] {new DynamicSememeStringImpl(annotationValue)}, refsetUuid, status, null, null);
	}
	
	public SememeChronology<DynamicSememe<?>> addAnnotationStyleRefsetMembership(ComponentReference referencedComponent, UUID refexDynamicTypeUuid, State status, Long time)
	{
		return addAnnotation(referencedComponent, null, (DynamicSememeData[])null, refexDynamicTypeUuid, status, time, null);
	}
	
	public SememeChronology<DynamicSememe<?>> addAnnotation(ComponentReference referencedComponent, UUID uuidForCreatedAnnotation, DynamicSememeData value, 
			UUID refexDynamicTypeUuid, State status, Long time)
	{
		return addAnnotation(referencedComponent, uuidForCreatedAnnotation, 
				(value == null ? new DynamicSememeData[] {} : new DynamicSememeData[] {value}), refexDynamicTypeUuid, status, time, null);
	}
	
	/**
	 * @param referencedComponent The component to attach this annotation to
	 * @param uuidForCreatedAnnotation  - the UUID to use for the created annotation.  If null, generated from uuidForCreatedAnnotation, value, refexDynamicTypeUuid
	 * @param values - the values to attach (may be null if the annotation only serves to mark 'membership') - columns must align with values specified in the definition
	 * of the sememe represented by refexDynamicTypeUuid
	 * @param refexDynamicTypeUuid - the uuid of the dynamic sememe type - 
	 * @param state
	 * @param time - if null, uses the component time
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public SememeChronology<DynamicSememe<?>> addAnnotation(ComponentReference referencedComponent, UUID uuidForCreatedAnnotation, DynamicSememeData[] values, 
			UUID refexDynamicTypeUuid, State state, Long time, UUID module)
	{
		validateDataTypes(refexDynamicTypeUuid, values);
		@SuppressWarnings("rawtypes")
		SememeBuilder sb = sememeBuilderService_.getDynamicSememeBuilder(referencedComponent.getNid(), 
				Get.identifierService().getConceptSequenceForUuids(refexDynamicTypeUuid), values);
		
		if (uuidForCreatedAnnotation == null)
		{
			StringBuilder temp = new StringBuilder();
			temp.append(refexDynamicTypeUuid.toString()); 
			temp.append(referencedComponent.getPrimordialUuid().toString());
			if (values != null)
			{
				for (DynamicSememeData d : values)
				{
					if (d == null)
					{
						temp.append("null");
					}
					else
					{
						temp.append(d.getDynamicSememeDataType().getDisplayName());
						temp.append(new String(d.getData()));
					}
				}
			}
			uuidForCreatedAnnotation = ConverterUUID.createNamespaceUUIDFromString(temp.toString());
		}
		
		sb.setPrimordialUuid(uuidForCreatedAnnotation);
		
		ArrayList<OchreExternalizable> builtObjects = new ArrayList<>();
		SememeChronology<DynamicSememe<?>> sc = (SememeChronology<DynamicSememe<?>>)sb.build(createStamp(state, selectTime(time, referencedComponent), module), builtObjects);
		
		for (OchreExternalizable ochreObject : builtObjects)
		{
			writer_.put(ochreObject);
		}
		if (values == null || values.length == 0)
		{
			ls_.addRefsetMember(getOriginStringForUuid(refexDynamicTypeUuid));
		}
		else
		{
			if (BPT_Associations.isAssociation(refexDynamicTypeUuid))
			{
				ls_.addAssociation(getOriginStringForUuid(refexDynamicTypeUuid));
			}
			else
			{
				ls_.addAnnotation((referencedComponent.getTypeString().length() == 0 ? 
						getOriginStringForUuid(referencedComponent.getPrimordialUuid())
								: referencedComponent.getTypeString()), getOriginStringForUuid(refexDynamicTypeUuid));
			}
			
			
			//TODO not sure if we need any special handling for these cases any longer.  Might need better handling for rel... but not sure 
			//how to do it at the moment...
//			else if (component instanceof TtkRelationshipChronicle)
//			{
//				ls_.addAnnotation(getOriginStringForUuid(((TtkRelationshipChronicle) component).getTypeUuid()), getOriginStringForUuid(refsetUuid));
//			}
//			else if (component instanceof TtkRefexStringMemberChronicle)
//			{
//				ls_.addAnnotation(getOriginStringForUuid(((TtkRefexStringMemberChronicle) component).getAssemblageUuid()), 
//						(BPT_Associations.isAssociation(refsetUuid) ? "Association:" : "")  + getOriginStringForUuid(refsetUuid));
//			}
//			else if (component instanceof TtkRefexUuidMemberChronicle)
//			{
//				ls_.addAnnotation(getOriginStringForUuid(((TtkRefexUuidMemberChronicle) component).getAssemblageUuid()), getOriginStringForUuid(refsetUuid));
//			}
//			else if (component instanceof TtkRefexDynamicMemberChronicle)
//			{
//				ls_.addAnnotation((BPT_Associations.isAssociation(refsetUuid) ? "Association:" : "") 
//					+ getOriginStringForUuid(((TtkRefexDynamicMemberChronicle) component).getRefexAssemblageUuid()), getOriginStringForUuid(refsetUuid));
//			}
			
		}
		return sc;
	}
	
	private boolean isConfiguredAsDynamicSememe(UUID refexDynamicTypeUuid)
	{
		return refexAllowedColumnTypes_.containsKey(refexDynamicTypeUuid);
	}

	/**
	 * @param refexDynamicTypeUuid
	 * @param values
	 */
	private void validateDataTypes(UUID refexDynamicTypeUuid, DynamicSememeData[] values)
	{
		//TODO this should be a much better validator - checking all of the various things in RefexDynamicCAB.validateData - or in 
		//generateMetadataEConcepts
		
		if (!refexAllowedColumnTypes_.containsKey(refexDynamicTypeUuid))
		{
			throw new RuntimeException("Attempted to store data on a concept not configured as a dynamic sememe");
		}
		
		DynamicSememeColumnInfo[] colInfo = refexAllowedColumnTypes_.get(refexDynamicTypeUuid);
		
		if (values != null && values.length > 0)
		{
			if (colInfo != null)
			{
				for (int i = 0; i < values.length; i++)
				{
					DynamicSememeColumnInfo column = null;
					for (DynamicSememeColumnInfo x : colInfo)
					{
						if(x.getColumnOrder() == i)
						{
							column = x;
							break;
						}
					}
					if (column == null)
					{
						throw new RuntimeException("Column count mismatch");
					}
					else
					{
						if (values[i] == null && column.isColumnRequired())
						{
							throw new RuntimeException("Missing column data for column " + column.getColumnName());
						}
						else if (values[i] != null && column.getColumnDataType() != values[i].getDynamicSememeDataType())
						{
							throw new RuntimeException("Datatype mismatch - " + column.getColumnDataType() + " - " + values[i].getDynamicSememeDataType());
						}
					}
				}
			}
			else if (values.length > 0)
			{
				throw new RuntimeException("Column count mismatch - this dynamic sememe doesn't allow columns!");
			}
		}
		else if (colInfo != null)
		{
			for (DynamicSememeColumnInfo ci : colInfo)
			{
				if (ci.isColumnRequired())
				{
					throw new RuntimeException("Missing column data for column " + ci.getColumnName());
				}
			}
		}
	}
	/**
	 * uses the concept time, UUID is created from the component UUID, the annotation value and type.
	 */
	public SememeChronology<StringSememe<?>> addStaticStringAnnotation(ComponentReference referencedComponent, String annotationValue, UUID refsetUuid, 
			State state)
	{
		@SuppressWarnings("rawtypes")
		SememeBuilder sb = sememeBuilderService_.getStringSememeBuilder(annotationValue, referencedComponent.getNid(), 
				Get.identifierService().getConceptSequenceForUuids(refsetUuid));
		
		StringBuilder temp = new StringBuilder();
		temp.append(annotationValue);
		temp.append(refsetUuid.toString()); 
		temp.append(referencedComponent.getPrimordialUuid().toString());
		sb.setPrimordialUuid(ConverterUUID.createNamespaceUUIDFromString(temp.toString()));

		ArrayList<OchreExternalizable> builtObjects = new ArrayList<>();
		@SuppressWarnings("unchecked")
		SememeChronology<StringSememe<?>> sc = (SememeChronology<StringSememe<?>>)sb.build(createStamp(state, selectTime(null, referencedComponent)), builtObjects);
		
		for (OchreExternalizable ochreObject : builtObjects)
		{
			writer_.put(ochreObject);
		}
		ls_.addAnnotation(getOriginStringForUuid(referencedComponent.getPrimordialUuid()), getOriginStringForUuid(refsetUuid));

		return sc;
	}

	/**
	 * Generates the UUID, uses the component time
	 */
	public SememeChronology<DynamicSememe<?>> addUuidAnnotation(ComponentReference object, UUID value, UUID refsetUuid)
	{
		return addAnnotation(object, null, new DynamicSememeData[] {new DynamicSememeUUIDImpl(value)}, refsetUuid, null, null, null);
	}

	//I don't think we need this any longer
//	/**
//	 * annotationPrimordialUuid - if null, generated from component UUID, value, type
//	 * @param time - If time is null, uses the component time.
//	 */
//	@SuppressWarnings("unchecked")
//	public SememeChronology<SememeVersion<?>> addLegacyUuidAnnotation(ObjectChronology<?> referencedComponent, UUID annotationPrimordialUuid, UUID refsetUuid, 
//			State state, Long time)
//	{
//		@SuppressWarnings("rawtypes")
//		SememeBuilder sb = sememeBuilderService_.getMembershipSememeBuilder(referencedComponent.getNid(), 
//				Get.identifierService().getConceptSequenceForUuids(refsetUuid));
//		
//		if (annotationPrimordialUuid == null)
//		{
//			StringBuilder temp = new StringBuilder();
//			temp.append(refsetUuid.toString()); 
//			temp.append(referencedComponent.getPrimordialUuid().toString());
//			
//			annotationPrimordialUuid = ConverterUUID.createNamespaceUUIDFromString(temp.toString());
//		}
//		
//		sb.setPrimordialUuid(annotationPrimordialUuid);
//		
//		ArrayList<OchreExternalizable> builtObjects = new ArrayList<>();
//		SememeChronology<SememeVersion<?>> sc;
//		if (time == null)
//		{
//			sc = (SememeChronology<SememeVersion<?>>)sb.build(createStamp(state, referencedComponent), builtObjects);
//		}
//		else
//		{
//			sc = (SememeChronology<SememeVersion<?>>)sb.build(createStamp(state, time), builtObjects);
//		}
//		
//		for (OchreExternalizable ochreObject : builtObjects)
//		{
//			writer_.put(ochreObject);
//		}
//		ls_.addRefsetMember(getOriginStringForUuid(refsetUuid));
//
//		return sc;
//	}

	public SememeChronology<DynamicSememe<?>> addDynamicRefsetMember(UUID refsetConcept, ComponentReference targetUuid, UUID uuidForCreatedAnnotation, State status, Long time)
	{
		return addAnnotation(targetUuid, uuidForCreatedAnnotation, (DynamicSememeData)null, refsetConcept, status, time);
	}
	
	/**
	 * Add an association. The source of the association is assumed to be the specified concept.
	 * 
	 * @param associationPrimordialUuid - optional - if not provided, created from the source, target and type.
	 * @param associationTypeUuid required
	 * @param time - if null, default is used
	 */
	public SememeChronology<DynamicSememe<?>> addAssociation(ComponentReference concept, UUID associationPrimordialUuid, UUID targetUuid, 
			UUID associationTypeUuid, State state, Long time, UUID module)
	{
		if (!isConfiguredAsDynamicSememe(associationTypeUuid))
		{
			configureConceptAsAssociation(associationTypeUuid, null);
		}
		return addAnnotation(concept, associationPrimordialUuid, 
				new DynamicSememeData[]{new DynamicSememeUUIDImpl(targetUuid)}, 
				associationTypeUuid, state, time, module);
	}

	/**
	 * Add an IS_A_REL relationship, with the time set to now.
	 */
	public SememeChronology<LogicGraphSememe<?>> addRelationship(ComponentReference ttkConceptChronicle, UUID targetUuid)
	{
		return addRelationship(ttkConceptChronicle, null, targetUuid, null, null, null, null);
	}

	/**
	 * Add a relationship. The source of the relationship is assumed to be the specified concept. The UUID of the
	 * relationship is generated.
	 * 
	 * @param relTypeUuid - is optional - if not provided, the default value of IS_A_REL is used.
	 * @param time - if null, default is used
	 */
	public SememeChronology<LogicGraphSememe<?>> addRelationship(ComponentReference ttkConceptChronicle, UUID targetUuid, UUID relTypeUuid, Long time)
	{
		return addRelationship(ttkConceptChronicle, null, targetUuid, relTypeUuid, null, null, time);
	}
	
	/**
	 * This rel add method handles the advanced cases where a rel type 'foo' is actually being loaded as "is_a" (or some other arbitrary type)
	 * it makes the swap, and adds the second value as a UUID annotation on the created relationship. 
	 */
	public SememeChronology<LogicGraphSememe<?>> addRelationship(ComponentReference ttkConceptChronicle, UUID targetUuid, Property p, Long time)
	{
		if (p.getWBTypeUUID() == null)
		{
			return addRelationship(ttkConceptChronicle, null, targetUuid, p.getUUID(), null, null, time);
		}
		else
		{
			return addRelationship(ttkConceptChronicle, null, targetUuid, p.getWBTypeUUID(), p.getUUID(), p.getPropertyType().getPropertyTypeReferenceSetUUID(), time);
		}
	}
	
	/**
	 * Add a relationship. The source of the relationship is assumed to be the specified concept.
	 * 
	 * @param relPrimordialUuid - optional - if not provided, created from the source, target and type.
	 * @param relTypeUuid - is optional - if not provided, the default value of IS_A_REL is used.
	 * @param time - if null, default is used
	 */
	public SememeChronology<LogicGraphSememe<?>> addRelationship(ComponentReference concept, UUID relPrimordialUuid, UUID targetUuid, UUID relTypeUuid, UUID sourceRelTypeUUID,
			UUID sourceRelRefsetUUID, Long time)
	{
		//TODO this is going to end up creating one logic graph per rel, which, I suspect, isn't correct.
		//Need to talk to Keith about this, figure out if I can do this one by one, or if I have to build the entire logic graph at once.
		//If at once, then, what do I do with the individual relType annotations?
		LogicalExpressionBuilder leb = expressionBuilderService_.getLogicalExpressionBuilder();

		if (relTypeUuid == null || relTypeUuid.equals(isARelUuid_))
		{
			NecessarySet(And(ConceptAssertion(Get.conceptService().getConcept(targetUuid), leb)));
		}
		else
		{
			SomeRole(Get.conceptService().getConcept(relTypeUuid),
					ConceptAssertion(Get.conceptService().getConcept(targetUuid), leb));
		}

		LogicalExpression logicalExpression = leb.build();

		@SuppressWarnings("rawtypes")
		SememeBuilder sb = sememeBuilderService_.getLogicalExpressionSememeBuilder(logicalExpression, concept.getNid(),
				conceptBuilderService_.getDefaultLogicCoordinate().getStatedAssemblageSequence());

		sb.setPrimordialUuid(relPrimordialUuid != null ? relPrimordialUuid
				: ConverterUUID.createNamespaceUUIDFromStrings(concept.getPrimordialUuid().toString(), targetUuid.toString(),
						(relTypeUuid == null ? isARelUuid_.toString() : relTypeUuid.toString())));

		ArrayList<OchreExternalizable> builtObjects = new ArrayList<>();

		@SuppressWarnings("unchecked")
		SememeChronology<LogicGraphSememe<?>> sci = (SememeChronology<LogicGraphSememe<?>>) sb.build(createStamp(State.ACTIVE, selectTime(time, concept)), builtObjects);

		for (OchreExternalizable ochreObject : builtObjects)
		{
			writer_.put(ochreObject);
		}

		if (sourceRelTypeUUID != null && sourceRelRefsetUUID != null)
		{
			addUuidAnnotation(ComponentReference.fromChronology(sci), sourceRelTypeUUID, sourceRelRefsetUUID);
			ls_.addRelationship(getOriginStringForUuid(relTypeUuid) + ":" + getOriginStringForUuid(sourceRelTypeUUID));
		}
		else
		{
			ls_.addRelationship(getOriginStringForUuid(relTypeUuid == null ? isARelUuid_ : relTypeUuid));
		}
		return sci;
	}
	
	public SememeChronology<LogicGraphSememe<?>> addRelationshipGraph(ComponentReference concept, UUID graphPrimordialUuid, 
			LogicalExpression logicalExpression, boolean stated, Long time, UUID module)
	{
		@SuppressWarnings("rawtypes")
		SememeBuilder sb = sememeBuilderService_.getLogicalExpressionSememeBuilder(logicalExpression, concept.getNid(),
				stated ? conceptBuilderService_.getDefaultLogicCoordinate().getStatedAssemblageSequence() : 
					conceptBuilderService_.getDefaultLogicCoordinate().getInferredAssemblageSequence());

		//TODO come up with a way to correctly generate a UUID from a logicalExpression
		sb.setPrimordialUuid(graphPrimordialUuid != null ? graphPrimordialUuid
				: null);

		ArrayList<OchreExternalizable> builtObjects = new ArrayList<>();

		@SuppressWarnings("unchecked")
		SememeChronology<LogicGraphSememe<?>> sci = (SememeChronology<LogicGraphSememe<?>>) sb.build(
				createStamp(State.ACTIVE, selectTime(time, concept), module), builtObjects);

		for (OchreExternalizable ochreObject : builtObjects)
		{
			writer_.put(ochreObject);
		}

		ls_.addGraph();
		return sci;
	}
	
	/**
	 * uses providedTime first, if present, followed by readTimeFrom.
	 * 
	 * Note, this still may return null.
	 */
	public Long selectTime(Long providedTime, ComponentReference readTimeFrom)
	{
		if (providedTime != null)
		{
			return providedTime;
		}
		else
		{
			return readTimeFrom.getTime();
		}
	}

	/**
	 * Set up all the boilerplate stuff.
	 * 
	 * @param state - state or null (for current)
	 * @param time - time or null (for default)
	 */
	public int createStamp(State state, Long time) 
	{
		return createStamp(state, time, null);
	}
	
	/**
	 * Set up all the boilerplate stuff.
	 * 
	 * @param state - state or null (for current)
	 * @param time - time or null (for default)
	 */
	public int createStamp(State state, Long time, UUID module) 
	{
		return Get.stampService().getStampSequence(
				state == null ? State.ACTIVE : state,
				time == null ? defaultTime_ : time.longValue(), 
				authorSeq_, (module == null ? moduleSeq_ : Get.identifierService().getConceptSequenceForUuids(module)), terminologyPathSeq_);
	}

	private String getOriginStringForUuid(UUID uuid)
	{
		String temp = ConverterUUID.getUUIDCreationString(uuid);
		if (temp != null)
		{
			String[] parts = temp.split(":");
			if (parts != null && parts.length > 1)
			{
				return parts[parts.length - 1];
			}
			return temp;
		}
		return "Unknown";
	}

	public LoadStats getLoadStats()
	{
		return ls_;
	}

	public void clearLoadStats()
	{
		ls_ = new LoadStats();
	}

	/**
	 * Utility method to build and store a concept.
	 * @param callback - optional - used to fire a callback if present.  No impact on created concept.  
	 * @param dos - optional - does not store when not provided
	 * @param secondParent - optional
	 */
	public ConceptChronology<? extends ConceptVersion<?>> createConcept(UUID primordial, String fsnName, String preferredName, String altName, String definition, UUID relParentPrimordial, 
			UUID secondParent)
	{
		ConceptChronology<? extends ConceptVersion<?>> concept = createConcept(primordial, fsnName);
		addRelationship(ComponentReference.fromConcept(concept), relParentPrimordial);
		if (secondParent != null)
		{
			addRelationship(ComponentReference.fromConcept(concept), secondParent);
		}
		if (StringUtils.isNotEmpty(preferredName))
		{
			addDescription(ComponentReference.fromConcept(concept), preferredName, DescriptionType.SYNONYM, true, null, null, State.ACTIVE);
		}
		if (StringUtils.isNotEmpty(altName))
		{
			addDescription(ComponentReference.fromConcept(concept), altName, DescriptionType.SYNONYM, false, null, null, State.ACTIVE);
		}
		if (StringUtils.isNotEmpty(definition))
		{
			addDescription(ComponentReference.fromConcept(concept), definition, DescriptionType.DEFINITION, true, null, null, State.ACTIVE);
		}
		
		return concept;
	}


	/**
	 * Create metadata TtkConceptChronicles from the PropertyType structure
	 * NOTE - Refset types are not stored!
	 */
	public void loadMetaDataItems(PropertyType propertyType, UUID parentPrimordial, DataOutputStream dos) throws Exception
	{
		ArrayList<PropertyType> propertyTypes = new ArrayList<PropertyType>();
		propertyTypes.add(propertyType);
		loadMetaDataItems(propertyTypes, parentPrimordial, dos);
	}

	/**
	 * Create metadata TtkConceptChronicles from the PropertyType structure
	 * NOTE - Refset types are not stored!
	 */
	public void loadMetaDataItems(Collection<PropertyType> propertyTypes, UUID parentPrimordial, DataOutputStream dos) throws Exception
	{
		for (PropertyType pt : propertyTypes)
		{
			if (pt instanceof BPT_Skip)
			{
				continue;
			}
			
			createConcept(pt.getPropertyTypeUUID(), pt.getPropertyTypeDescription(), parentPrimordial);
			
			UUID secondParent = null;
			if (pt instanceof BPT_Refsets)
			{
				ConceptChronology<? extends ConceptVersion<?>> refsetTermGroup = createConcept(pt.getPropertyTypeReferenceSetName(), 
						MetaData.SOLOR_REFSETS.getPrimordialUuid());
				((BPT_Refsets) pt).setRefsetIdentityParent(refsetTermGroup.getPrimordialUuid());
				secondParent = refsetTermGroup.getPrimordialUuid(); 
			}
			else if (pt instanceof BPT_Descriptions)
			{
				//only do this once, in case we see a BPT_Descriptions more than once
				secondParent = setupWbPropertyMetadata(MetaData.DESCRIPTION_SOURCE_TYPE_REFERENCE_SETS.getPrimordialUuid(),
						MetaData.DESCRIPTION_TYPE_IN_SOURCE_TERMINOLOGY.getPrimordialUuid(), pt, dos);
			}
			
			else if (pt instanceof BPT_Relations)
			{
				secondParent = setupWbPropertyMetadata(MetaData.RELATIONSHIP_SOURCE_TYPE_REFERENCE_SETS.getPrimordialUuid(),
						MetaData.RELATIONSHIP_TYPE_IN_SOURCE_TERMINOLOGY.getPrimordialUuid(), pt, dos);
			}
			
			for (Property p : pt.getProperties())
			{
				ConceptChronology<? extends ConceptVersion<?>> concept = createConcept(p.getUUID(), p.getSourcePropertyNameFSN(), p.getSourcePropertyPreferredName(), 
						p.getSourcePropertyAltName(), p.getSourcePropertyDefinition(), pt.getPropertyTypeUUID(), secondParent);
				if (pt.createAsDynamicRefex())
				{
					configureConceptAsDynamicRefex(ComponentReference.fromConcept(concept), 
							(StringUtils.isNotEmpty(p.getSourcePropertyDefinition()) ? p.getSourcePropertyDefinition() : "Dynamic Sememe"),
							p.getDataColumnsForDynamicRefex(), null, null);
				}
				
				else if (p instanceof PropertyAssociation)
				{
					//TODO need to migrate code from otf-util (AssociationType, etc) down into the ISAAC packages... integrate here, at least at doc level
					//associations return false for "createAsDynamicRefex"
					PropertyAssociation item = (PropertyAssociation)p;
					
					//Make this a dynamic refex - with the association column info
					configureConceptAsDynamicRefex(ComponentReference.fromConcept(concept), item.getSourcePropertyDefinition(),
							item.getDataColumnsForDynamicRefex(), item.getAssociationComponentTypeRestriction(), item.getAssociationComponentTypeSubRestriction());
					
					//add the inverse name, if it has one
					if (!StringUtils.isBlank(item.getAssociationInverseName()))
					{
						addDescription(ComponentReference.fromConcept(concept), item.getAssociationInverseName(), DescriptionType.SYNONYM, false, null, null, 
								State.ACTIVE);
					}
					
					//Add this concept to the association sememe
					addDynamicRefsetMember(IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getUUID(), ComponentReference.fromConcept(concept), null, 
							State.ACTIVE, null);
				}
			}
		}
	}
	
	private UUID setupWbPropertyMetadata(UUID refsetSynonymParent, UUID refsetValueParent, PropertyType pt, DataOutputStream dos) throws Exception
	{
		if (pt.getPropertyTypeReferenceSetName() == null || pt.getPropertyTypeReferenceSetUUID() == null)
		{
			throw new RuntimeException("Unhandled case!");
		}
		
		//Create the terminology specific refset type
		ConceptChronology<? extends ConceptVersion<?>> cc = createConcept(pt.getPropertyTypeReferenceSetUUID(), pt.getPropertyTypeReferenceSetName(), refsetSynonymParent);
		ConverterUUID.addMapping(pt.getPropertyTypeReferenceSetName(), pt.getPropertyTypeReferenceSetUUID());
		configureConceptAsDynamicRefex(ComponentReference.fromConcept(cc), "Carries the source description type information", 
				new DynamicSememeColumnInfo[] {
						new DynamicSememeColumnInfo(0, IsaacMetadataConstants.DYNAMIC_SEMEME_COLUMN_VALUE.getUUID(), DynamicSememeDataType.UUID, null, true)
						}, 
				null, null);

		//Now create the terminology specific refset type as a child - very similar to above, but since this isn't the refset concept, just an organization
		//concept, I add an 's' to make it plural, and use a different UUID (calculated from the new plural)
		return createConcept(ConverterUUID.createNamespaceUUIDFromString(pt.getPropertyTypeReferenceSetName() + "s", true), 
				pt.getPropertyTypeReferenceSetName() + "s", refsetValueParent).getPrimordialUuid();
	}
	
	public void registerDynamicSememeColumnInfo(UUID sememeUUID, DynamicSememeColumnInfo[] columnInfo)
	{
		refexAllowedColumnTypes_.put(sememeUUID, columnInfo);
	}
	
	public void configureConceptAsAssociation(UUID associationTypeConcept, String inverseName)
	{
		DynamicSememeColumnInfo[] colInfo = new DynamicSememeColumnInfo[] {new DynamicSememeColumnInfo(
				0, IsaacMetadataConstants.DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT.getPrimordialUuid(), DynamicSememeDataType.UUID, null, true)};
		configureConceptAsDynamicRefex(ComponentReference.fromConcept(associationTypeConcept), 
				"Defines an Association Type", colInfo, null, null);
		
		addDynamicRefsetMember(IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getUUID(), ComponentReference.fromConcept(associationTypeConcept), null, 
				State.ACTIVE, null);
		
		if (!StringUtils.isBlank(inverseName))
		{
			SememeChronology<DescriptionSememe<?>> inverseDesc = addDescription(ComponentReference.fromConcept(associationTypeConcept), inverseName, 
					DescriptionType.SYNONYM, false, null, null, State.ACTIVE);
			
			addDynamicRefsetMember(IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getUUID(), ComponentReference.fromChronology(inverseDesc), null, 
					State.ACTIVE, selectTime(null, ComponentReference.fromChronology(inverseDesc)));
		}
		BPT_Associations.registerAsAssociation(associationTypeConcept);
	}
	
	public void configureConceptAsDynamicRefex(ComponentReference concept, String refexDescription,
			DynamicSememeColumnInfo[] columns, ObjectChronologyType referencedComponentTypeRestriction, SememeType referencedComponentTypeSubRestriction)
	{
		// See {@link DynamicSememeUsageDescriptionBI} class for more details on this format.
		//Add the special synonym to establish this as an assemblage concept
		SememeChronology<DescriptionSememe<?>> desc = addDescription(concept, refexDescription, DescriptionType.DEFINITION, true, null, null, State.ACTIVE);
		
		//Annotate the description as the 'special' type that means this concept is suitable for use as an assemblage concept
		addAnnotation(ComponentReference.fromChronology(desc), null, (DynamicSememeData)null, 
				IsaacMetadataConstants.DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getUUID(), State.ACTIVE, null);
		
		//define the data columns (if any)
		if (columns != null && columns.length > 0)
		{
			for (DynamicSememeColumnInfo col : columns)
			{
				DynamicSememeData[] data = DynamicSememeUtilityImpl.configureDynamicSememeDefinitionDataForColumn(col);
				addAnnotation(concept, null, data, IsaacMetadataConstants.DYNAMIC_SEMEME_EXTENSION_DEFINITION.getUUID(), State.ACTIVE, null, null);
			}
			
			//TODO should really not mark unindexable column types
			DynamicSememeInteger[] indexInfo = new DynamicSememeInteger[columns.length];
			for (int i = 0; i < indexInfo.length; i++)
			{
				indexInfo[i] = new DynamicSememeIntegerImpl(i);
			}
			
			addAnnotation(concept, null, new DynamicSememeData[] {new DynamicSememeArrayImpl<DynamicSememeInteger>(indexInfo)},
					IsaacMetadataConstants.DYNAMIC_SEMEME_INDEX_CONFIGURATION.getPrimordialUuid(), State.ACTIVE, null, null);
		}
		registerDynamicSememeColumnInfo(concept.getPrimordialUuid(), columns);
		
		//Add the restriction information (if any)
		DynamicSememeData[] data = DynamicSememeUtilityImpl.configureDynamicSememeRestrictionData(referencedComponentTypeRestriction, referencedComponentTypeSubRestriction);
		if (data != null)
		{
			addAnnotation(concept, null, data, IsaacMetadataConstants.DYNAMIC_SEMEME_REFERENCED_COMPONENT_RESTRICTION.getUUID(), State.ACTIVE, null, null);
		}
	}
	
	public void shutdown()
	{
		writer_.close();
		LookupService.shutdownIsaac();
	}
}
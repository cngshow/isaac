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
package gov.vha.isaac.ochre.utility.export;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.va.med.term.vhat.xml.model.ActionType;
import gov.va.med.term.vhat.xml.model.DesignationType;
import gov.va.med.term.vhat.xml.model.KindType;
import gov.va.med.term.vhat.xml.model.PropertyType;
import gov.va.med.term.vhat.xml.model.Terminology;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships.SubsetMembership;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.TaxonomyService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUtility;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.associations.AssociationInstance;
import gov.vha.isaac.ochre.associations.AssociationUtilities;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.configuration.LanguageCoordinates;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;
import gov.vha.isaac.ochre.modules.vhat.VHATConstants;


public class VetsExporter {

	private Logger log = LogManager.getLogger();

	private Map<UUID, String> designationTypes = new HashMap<>();
	private Map<UUID, String> propertyTypes = new HashMap<>();
	private Map<UUID, String> relationshipTypes = new HashMap<>();

	private Map<UUID, String> assemblagesMap = new HashMap<>();
	private Map<String, Long> subsetMap = new HashMap<>();

	private Terminology terminology;

	private StampCoordinate STAMP_COORDINATES = null;

	TaxonomyService ts = Get.taxonomyService();
	
	boolean fullExportMode = false;

	public VetsExporter()
	{
	}

	/**
	 *
	 * @param writeTo the output stream object handling the export
	 * @param startDate only export concepts modified on or after this date.  Set to 0, if you want to start from the beginning
	 * @param endDate only export concepts where their most recent modified date is on or before this date.  Set to Long.MAX_VALUE to get everything.
	 * @param fullExportMode if true, exports all content present at the end of the date range, ignoring start date.  All actions are set to add.
	 *   If false - delta mode - calculates the action based on the start and end dates, and includes only the minimum required elements in the xml file.
	 */
	public void export(OutputStream writeTo, long startDate, long endDate, boolean fullExportMode) {

		this.fullExportMode = fullExportMode;

		STAMP_COORDINATES = new StampCoordinateImpl(StampPrecedence.PATH, new StampPositionImpl(endDate, MetaData.DEVELOPMENT_PATH.getConceptSequence()),
				ConceptSequenceSet.EMPTY, State.ANY_STATE_SET);
		// Build Assemblages map
		Get.sememeService().getAssemblageTypes().forEach((assemblageSeqId) -> {
			assemblagesMap.put(Get.conceptSpecification(assemblageSeqId).getPrimordialUuid(),
					Get.conceptSpecification(assemblageSeqId).getConceptDescriptionText());
		});

		// XML object
		terminology = new Terminology();

		// Types
		terminology.setTypes(new Terminology.Types());
		Terminology.Types.Type xmlType;

		// Subsets/Refsets
		terminology.setSubsets(new Terminology.Subsets());

		// CodeSystem
		Terminology.CodeSystem xmlCodeSystem = new Terminology.CodeSystem();
		Terminology.CodeSystem.Version xmlVersion = new Terminology.CodeSystem.Version();
		Terminology.CodeSystem.Version.CodedConcepts xmlCodedConcepts = new Terminology.CodeSystem.Version.CodedConcepts();

		// For any added types to export
		Set<String> newDesignationTypes = new HashSet<>();
		Set<String> newPropertyTypes = new HashSet<>();
		Set<String> newRelationshipTypes = new HashSet<>();
		
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				VHATConstants.VHAT_ASSOCIATION_TYPES.getNid()).forEach((conceptId) -> {
					ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
					String preferredNameDescriptionType = getPreferredNameDescriptionType(concept.getNid());
					relationshipTypes.put(concept.getPrimordialUuid(), preferredNameDescriptionType);
					
					if (determineAction(concept, startDate, endDate).equals(ActionType.ADD))
					{
						newRelationshipTypes.add(preferredNameDescriptionType);
					}
				});
		
		if (fullExportMode)
		{
			// Adds all of the elements in the specified collection to this set if they're not already present 
			newRelationshipTypes.addAll(relationshipTypes.values());
		}

		for (String s : newRelationshipTypes)
		{
			xmlType = new Terminology.Types.Type();
			xmlType.setKind(KindType.RELATIONSHIP_TYPE);
			xmlType.setName(s);
			terminology.getTypes().getType().add(xmlType);
		}

		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				VHATConstants.VHAT_ATTRIBUTE_TYPES.getNid()).forEach((conceptId) -> {
					ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
					String preferredNameDescriptionType = getPreferredNameDescriptionType(concept.getNid());
					propertyTypes.put(concept.getPrimordialUuid(), preferredNameDescriptionType);
					
					if (determineAction(concept, startDate, endDate).equals(ActionType.ADD))
					{
						newPropertyTypes.add(preferredNameDescriptionType);
					}
				});

		if (fullExportMode)
		{
			// Adds all of the elements in the specified collection to this set if they're not already present 
			newPropertyTypes.addAll(propertyTypes.values());
		}

		for (String s : newPropertyTypes)
		{
			xmlType = new Terminology.Types.Type();
			xmlType.setKind(KindType.PROPERTY_TYPE);
			xmlType.setName(s);
			terminology.getTypes().getType().add(xmlType);
		}

		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(VHATConstants.VHAT_DESCRIPTION_TYPES.getPrimordialUuid())).forEach((conceptId) -> {
					ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
					String preferredNameDescriptionType = getPreferredNameDescriptionType(concept.getNid());
					designationTypes.put(concept.getPrimordialUuid(), preferredNameDescriptionType);
					
					if (determineAction(concept, startDate, endDate).equals(ActionType.ADD))
					{
						newDesignationTypes.add(preferredNameDescriptionType);
					}
				});

		if (fullExportMode)
		{
			// Adds all of the elements in the specified collection to this set if they're not already present 
			newDesignationTypes.addAll(designationTypes.values());
		}

		for (String s : newDesignationTypes)
		{
			xmlType = new Terminology.Types.Type();
			xmlType.setKind(KindType.DESIGNATION_TYPE);
			xmlType.setName(s);
			terminology.getTypes().getType().add(xmlType);
		}

		// Get data, Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(VHATConstants.VHAT_REFSETS.getNid()).forEach((tcs) ->
		{
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(tcs);
			// Excluding these:
			if (concept.getPrimordialUuid().equals(VHATConstants.VHAT_ALL_CONCEPTS.getPrimordialUuid())
					|| concept.getPrimordialUuid().equals(VHATConstants.VHAT_MISSING_SDO_CODE_SYSTEM_CONCEPTS.getPrimordialUuid())
					|| Frills.definesMapping(concept.getConceptSequence()) ) 
			{ 
				// Skip
			} else {
				Terminology.Subsets.Subset xmlSubset = new Terminology.Subsets.Subset();
				xmlSubset.setAction(determineAction(concept, startDate, endDate));
				xmlSubset.setName(getPreferredNameDescriptionType(concept.getNid()));
				xmlSubset.setActive(concept.isLatestVersionActive(STAMP_COORDINATES));

				//read VUID
				xmlSubset.setVUID(Frills.getVuId(concept.getNid(), STAMP_COORDINATES).orElse(null));

				if (xmlSubset.getVUID() == null)
				{
					log.warn("Failed to find VUID for subset concept " + concept.getPrimordialUuid());
				}

				if (xmlSubset.getAction() != ActionType.NONE)
				{
					terminology.getSubsets().getSubset().add(xmlSubset);
				}
				subsetMap.put(xmlSubset.getName(), xmlSubset.getVUID());
			}
		});


		ConceptChronology<? extends ConceptVersion<?>> vhatConcept = Get.conceptService().getConcept(VHATConstants.VHAT_ROOT_CONCEPT.getNid());

		xmlCodeSystem.setAction(ActionType.NONE);
		xmlCodeSystem.setName(getPreferredNameDescriptionType(vhatConcept.getNid()));
		xmlCodeSystem.setVUID(Frills.getVuId(VHATConstants.VHAT_ROOT_CONCEPT.getNid()).orElse(null));
		xmlCodeSystem.setDescription("VHA Terminology");  //This is in an acceptable synonym, but easier to hard code at the moment...
		xmlCodeSystem.setCopyright(Year.now().getValue() + "");
		xmlCodeSystem.setCopyrightURL("");
		xmlCodeSystem.setPreferredDesignationType("Preferred Name");

		xmlVersion.setAppend(Boolean.TRUE);
		xmlVersion.setName("Authoring Version");
		xmlVersion.setDescription("Delta output from ISAAC");

		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String formattedDate = sdf.format(System.currentTimeMillis());
			XMLGregorianCalendar _xmlEffDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(formattedDate);
			XMLGregorianCalendar _xmlRelDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(formattedDate);
			xmlVersion.setEffectiveDate(_xmlEffDate);
			xmlVersion.setReleaseDate(_xmlRelDate);
		}
		catch (Exception pe)
		{
			log.error("Misconfiguration of date parser!", pe);
		}

		xmlVersion.setSource("");

		AtomicInteger skippedForNonVHAT = new AtomicInteger();
		AtomicInteger skippedDateRange = new AtomicInteger();
		AtomicInteger observedVhatConcepts = new AtomicInteger();
		AtomicInteger exportedVhatConcepts = new AtomicInteger();

		List<Terminology.CodeSystem.Version.MapSets.MapSet> xmlMapSetCollection = new ArrayList<>();

		Get.conceptService().getConceptChronologyStream().forEach((concept) ->
		{
			if (fullExportMode) 
			{
				Get.sememeService().getSememesForComponentFromAssemblage(concept.getConceptSequence(),
						IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getConceptSequence()).forEach(mappingSememe -> 
				{
					Terminology.CodeSystem.Version.MapSets.MapSet xmlMapSet = new Terminology.CodeSystem.Version.MapSets.MapSet();
					xmlMapSet.setAction(determineAction(concept, startDate, endDate));
					xmlMapSet.setActive(concept.isLatestVersionActive(STAMP_COORDINATES));
					xmlMapSet.setCode(getCodeFromNid(concept.getNid()));
					xmlMapSet.setName(getPreferredNameDescriptionType(concept.getNid()));
					xmlMapSet.setVUID(Frills.getVuId(concept.getNid(), STAMP_COORDINATES).orElse(null));

					// Source and Target CodeSystem
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Optional<LatestVersion<? extends DynamicSememe>> mappingSememeVersion 
							= ((SememeChronology) mappingSememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);

					if (mappingSememeVersion.isPresent()) 
					{
						// Get referenced component for the MapSet values
						ConceptChronology<? extends ConceptVersion<?>> cc = Get.conceptService().getConcept(mappingSememeVersion.get().value().getReferencedComponentNid());

						@SuppressWarnings({ "rawtypes", "unchecked" })
						Optional<LatestVersion<ConceptVersion<?>>> cv 
								= ((ConceptChronology) cc).getLatestVersion(ConceptVersion.class, STAMP_COORDINATES);

						if (cv.isPresent()) 
						{
							Get.sememeService().getSememesForComponentFromAssemblage(cv.get().value().getChronology().getNid(),
									IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getConceptSequence()).forEach(mappingStrExt -> {
								@SuppressWarnings({ "unchecked", "rawtypes" })
								Optional<LatestVersion<? extends DynamicSememe>> mappingStrExtVersion 
										= ((SememeChronology) mappingStrExt).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);

								// TODO:DA review
								if (mappingStrExtVersion.isPresent())
								{
									DynamicSememeData dsd[] = mappingStrExtVersion.get().value().getData();
									if (dsd.length == 2) 
									{
										if (dsd[0].getDataObject().equals(IsaacMappingConstants.get().MAPPING_SOURCE_CODE_SYSTEM.getNid())) 
										{
											xmlMapSet.setSourceCodeSystem(dsd[1].getDataObject().toString());
										}
										else if (dsd[0].getDataObject().equals(IsaacMappingConstants.get().MAPPING_SOURCE_CODE_SYSTEM_VERSION.getNid())) 
										{
											xmlMapSet.setSourceVersionName(dsd[1].getDataObject().toString());
										}
										else if (dsd[0].getDataObject().equals(IsaacMappingConstants.get().MAPPING_TARGET_CODE_SYSTEM.getNid()))
										{
											xmlMapSet.setTargetCodeSystem(dsd[1].getDataObject().toString());
										}
										else if (dsd[0].getDataObject().equals(IsaacMappingConstants.get().MAPPING_TARGET_CODE_SYSTEM_VERSION.getNid()))
										{
											xmlMapSet.setTargetVersionName(dsd[1].getDataObject().toString());
										}
									}
								}
							});

							// MapEntries
							// MapEntry->Properties
							// TODO: MapEntry->Designations (currently ignored - none found in import XML, importer doesn't implement)
							// TODO: MapEntry->Relationships (currently ignored - none found in import XML, importer doesn't implement)
							// TODO:DA review - not using MapEntryType as it doesn't allow for .setProperties(), needed for GEM_Flags
							Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries xmlMapEntries 
									= new Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries();
							for (Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry me : 
									readMapEntryTypes(cv.get().value().getChronology().getNid(), startDate, endDate))
							{
								xmlMapEntries.getMapEntry().add(me);
							}
							xmlMapSet.setMapEntries(xmlMapEntries);
						}
					}

					// Designations
					Terminology.CodeSystem.Version.MapSets.MapSet.Designations xmlMapSetDesignations 
							= new Terminology.CodeSystem.Version.MapSets.MapSet.Designations();

					for (DesignationType d : getDesignations(concept, startDate, endDate,
							(() -> new Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation())))
					{
						// MapSets contain a phantom description with no typeName, code or VUID - need to keep those out
						// There's probably a more appropriate way to do this - quick and dirty for now
						// TODO:DA review
						if (!((d.getTypeName() == null || d.getTypeName().isEmpty())
								&& (d.getCode() == null || d.getCode().isEmpty()) && (d.getVUID() == null)))
						{
							xmlMapSetDesignations.getDesignation().add((Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation) d);
						}
					}

					xmlMapSet.setDesignations(xmlMapSetDesignations);

					// Properties
					Terminology.CodeSystem.Version.MapSets.MapSet.Properties xmlMapSetProperties 
							= new Terminology.CodeSystem.Version.MapSets.MapSet.Properties();

					for (PropertyType pt : readPropertyTypes(concept.getNid(), startDate, endDate,
							() -> new Terminology.CodeSystem.Version.MapSets.MapSet.Properties.Property()))
					{
						xmlMapSetProperties.getProperty().add((Terminology.CodeSystem.Version.MapSets.MapSet.Properties.Property) pt);
					}
					xmlMapSet.setProperties(xmlMapSetProperties);

					if (xmlMapSet.getAction() != ActionType.NONE || (xmlMapSet.getMapEntries() != null
							&& xmlMapSet.getMapEntries().getMapEntry().size() > 0)) 
					{
						xmlMapSetCollection.add(xmlMapSet);
					}
				});
			}
			
			if (!ts.wasEverKindOf(concept.getConceptSequence(), VHATConstants.VHAT_ROOT_CONCEPT.getNid()))
			{
				// Needed to ignore all the dynamically created/non-imported concepts
				skippedForNonVHAT.getAndIncrement();
			}
			else if (concept.getNid() == VHATConstants.VHAT_ROOT_CONCEPT.getNid())
			{
				//skip
			}
			else
			{
				observedVhatConcepts.getAndIncrement();
				int conceptNid = concept.getNid();

				if (!wasConceptOrNestedValueModifiedInDateRange(concept, startDate))
				{
					skippedDateRange.getAndIncrement();
				}
				else
				{
					exportedVhatConcepts.getAndIncrement();

					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept xmlCodedConcept 
							= new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept();
					xmlCodedConcept.setAction(determineAction(concept, startDate, endDate));
					xmlCodedConcept.setName(getPreferredNameDescriptionType(conceptNid));
					xmlCodedConcept.setVUID(Frills.getVuId(conceptNid, null).orElse(null));
					xmlCodedConcept.setCode(getCodeFromNid(conceptNid));
					xmlCodedConcept.setActive(Boolean.valueOf(concept.isLatestVersionActive(STAMP_COORDINATES)));

					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations xmlDesignations =
							new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations();
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties xmlProperties =
							new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties();


					for (DesignationType d : getDesignations(concept, startDate, endDate,
							() -> new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation()))
					{
						xmlDesignations.getDesignation().add((Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation)d);
					}

					for (PropertyType pt : readPropertyTypes(concept.getNid(), startDate, endDate,
							() -> new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties.Property()))
					{
						xmlProperties.getProperty().add((Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties.Property)pt);
					}


					// Relationships
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships xmlRelationships
							= new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships();
					for (Relationship rel : getRelationships(concept, startDate, endDate))
					{
						xmlRelationships.getRelationship().add(rel);
					}

					// Try to keep XML output somewhat clean, without empty elements (i.e. <Element/> or <Element></Element>
					if (xmlDesignations.getDesignation().size() > 0) {
						xmlCodedConcept.setDesignations(xmlDesignations);
					}

					if (xmlProperties.getProperty().size() > 0) {
						xmlCodedConcept.setProperties(xmlProperties);
					}

					if (xmlRelationships.getRelationship().size() > 0) {
						xmlCodedConcept.setRelationships(xmlRelationships);
					}

					// Add all CodedConcept elements
					xmlCodedConcepts.getCodedConcept().add(xmlCodedConcept);
				}
			}
		});


		// Close out XML
		xmlVersion.setCodedConcepts(xmlCodedConcepts);

		// MapSets
		if (xmlMapSetCollection.size() > 0)
		{
			Terminology.CodeSystem.Version.MapSets xmlMapSets = new Terminology.CodeSystem.Version.MapSets();
			xmlMapSets.getMapSet().addAll(xmlMapSetCollection);
			xmlVersion.setMapSets(xmlMapSets);
		}

		xmlCodeSystem.setVersion(xmlVersion);
		terminology.setCodeSystem(xmlCodeSystem);

		log.info("Skipped " + skippedForNonVHAT.get() + " concepts for non-vhat");
		log.info("Skipped " + skippedDateRange.get() + " concepts for outside date range");
		log.info("Processed " + observedVhatConcepts.get() + " concepts");
		log.info("Exported " + exportedVhatConcepts.get() + " concepts");

		writeXml(writeTo);
	}

	/**
	 * 
	 * @param componentNid
	 * @param startDate
	 * @param endDate
	 * @param constructor
	 * @return a List of the PropertyType objects for the specific component
	 */
	private List<PropertyType> readPropertyTypes(int componentNid, long startDate, long endDate, Supplier<PropertyType> constructor)
	{
		ArrayList<PropertyType> pts = new ArrayList<>();

		Get.sememeService().getSememesForComponent(componentNid).forEach((sememe) ->
		{
			//skip code and vuid properties - they have special handling
			if (sememe.getAssemblageSequence() != MetaData.VUID.getConceptSequence() && sememe.getAssemblageSequence() != MetaData.CODE.getConceptSequence()
					&& ts.wasEverKindOf(sememe.getAssemblageSequence(), VHATConstants.VHAT_ATTRIBUTE_TYPES.getNid()))
			{
				PropertyType property = buildProperty(sememe, startDate, endDate, constructor);
				if (property != null)
				{
					pts.add(property);
				}
			}
		});
		return pts;
	}
	

	/**
	 * 
	 * @param componentNid
	 * @param startDate
	 * @param endDate
	 * @return a List of the MapEntry objects for the MapSet item
	 */
	private List<Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry> readMapEntryTypes(int componentNid, long startDate, long endDate)
	{
		ArrayList<Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry> mes = new ArrayList<>();
		
		Get.sememeService().getSememesFromAssemblage(Get.identifierService().getConceptSequence(componentNid)).forEach(sememe ->
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<? extends DynamicSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
			if (sememeVersion.isPresent() && sememeVersion.get().value().getData() != null && sememeVersion.get().value().getData().length > 0)
			{
				try {
					Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry me = new Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry();
					
					me.setAction(ActionType.ADD); // TODO: There is currently no requirement or ability to deploy MapSet deltas, this if for the full export only
					me.setVUID(Frills.getVuId(sememe.getNid(), STAMP_COORDINATES).orElse(null));
					String code = getCodeFromNid(sememeVersion.get().value().getReferencedComponentNid());
					if (null == code)
					{
						code = Frills.getDescription(sememeVersion.get().value().getReferencedComponentNid()).orElse("");
					}
					me.setSourceCode(code);
					boolean isActive = sememeVersion.get().value().getState() == State.ACTIVE;
					me.setActive(isActive);
					
					DynamicSememeUtility ls =  LookupService.get().getService(DynamicSememeUtility.class);
					if (ls == null)
					{
						throw new RuntimeException("An implementation of DynamicSememeUtility is not available on the classpath");
					}
					else
					{
						DynamicSememeColumnInfo[] dsci = ls.readDynamicSememeUsageDescription(sememeVersion.get().value().getAssemblageSequence()).getColumnInfo();
						DynamicSememeData dsd[] = sememeVersion.get().value().getData();
						
						for (DynamicSememeColumnInfo d : dsci)
						{
							UUID columnUUID = d.getColumnDescriptionConcept();
							int col = d.getColumnOrder();
							
							if (null != dsd[col] && null != columnUUID)
							{
								if (columnUUID.equals(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT.getPrimordialUuid()))
								{
									me.setTargetCode(Frills.getDescription(UUID.fromString(dsd[col].getDataObject().toString())).orElse(""));
								}
								else if (columnUUID.equals(IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_EQUIVALENCE_TYPE.getPrimordialUuid()))
								{
									// Currently ignored, no XML representation
								}
								else if (columnUUID.equals(IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_SEQUENCE.getPrimordialUuid()))
								{
									me.setSequence(Integer.parseInt(dsd[col].getDataObject().toString()));
								}
								else if (columnUUID.equals(IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_GROUPING.getPrimordialUuid()))
								{
									me.setGrouping(Long.parseLong(dsd[col].getDataObject().toString()));
								}
								else if (columnUUID.equals(IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_EFFECTIVE_DATE.getPrimordialUuid()))
								{
									SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
									String formattedDate = sdf.format(dsd[col].getDataObject());
									me.setEffectiveDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(formattedDate));
								}
								else if (columnUUID.equals(IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_GEM_FLAGS.getPrimordialUuid()))
								{
									Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry.Properties.Property gem_prop 
										= new Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry.Properties.Property();
									Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry.Properties props
										= new Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry.Properties();
									gem_prop.setAction(ActionType.ADD); // See 'TODO' above
									gem_prop.setActive(Boolean.TRUE); // TODO: Defaulting to true as it appears in the import XML, not sure how to determine otherwise
									gem_prop.setTypeName("GEM_Flags");
									gem_prop.setValueNew(dsd[col].getDataObject().toString());
									props.getProperty().add(gem_prop);
									me.setProperties(props);
								}
								else
								{
									log.warn("No mapping match found for UUID: ", columnUUID);
								}
							}
						}
						mes.add(me);
					}
				} 
				catch (NumberFormatException nfe)
				{
					log.error("Misconfiguration of integer parser!", nfe);
				}
				catch (IllegalArgumentException iae)
				{
					log.error("Misconfiguration of date parser!", iae);
				}
				catch (NullPointerException npe)
				{
					log.error("Misconfiguration of date parser!", npe);
				}
				catch (DatatypeConfigurationException dce)
				{
					log.error("Misconfiguration of date parser!", dce);
				}
				catch (Exception e)
				{
					log.error("General MapEntry failure!", e);
				}
			}
		});
		return mes;
	}

	/**
	 * 
	 * @param sememe
	 * @param startDate
	 * @param endDate
	 * @param constructor
	 * @return A PropertyType object for the property, or null
	 */
	private PropertyType buildProperty(SememeChronology<?> sememe, long startDate, long endDate, Supplier<PropertyType> constructor)
	{
		String newValue = null;
		String oldValue = null;
		boolean isActive = false;
		if (sememe.getSememeType() == SememeType.DYNAMIC)
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<? extends DynamicSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
			if (sememeVersion.isPresent() && sememeVersion.get().value().getData() != null && sememeVersion.get().value().getData().length > 0)
			{
				newValue = sememeVersion.get().value().getData()[0] == null ? null : sememeVersion.get().value().getData()[0].dataToString();
				@SuppressWarnings({ "unchecked", "rawtypes" })
				List<DynamicSememe<?>> coll = ((SememeChronology) sememe).getVisibleOrderedVersionList(STAMP_COORDINATES);
				Collections.reverse(coll);
				for(DynamicSememe<?> s : coll)
				{
					if (s.getTime() < startDate)
					{
						oldValue = (s.getData()[0] != null) ? s.getData()[0].dataToString() : null;
						break;
					}
				}
				isActive = sememeVersion.get().value().getState() == State.ACTIVE;
			}
		}
		else if (sememe.getSememeType() == SememeType.STRING)
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<? extends StringSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(StringSememe.class, STAMP_COORDINATES);
			if (sememeVersion.isPresent())
			{
				newValue = sememeVersion.get().value().getString();
				@SuppressWarnings({ "unchecked", "rawtypes" })
				List<StringSememe<?>> coll = ((SememeChronology) sememe).getVisibleOrderedVersionList(STAMP_COORDINATES);
				Collections.reverse(coll);
				for(StringSememe<?> s : coll)
				{
					if (s.getTime() < startDate)
					{
						oldValue = s.getString();
						break;
					}
				}
				isActive = sememeVersion.get().value().getState() == State.ACTIVE;
			}
		}
		else
		{
			log.warn("Unexpectedly passed sememe " + sememe + " when we only expected a dynamic or a string type");
			return null;
		}

		if (newValue == null)
		{
			return null;
		}

		PropertyType property = constructor == null ? new gov.va.med.term.vhat.xml.model.PropertyType() : constructor.get();
		property.setAction(determineAction(sememe, startDate, endDate));
		if (isActive && property.getAction() == ActionType.NONE && newValue.equals(oldValue))
		{
			return null;
		}
		else if(!newValue.equals(oldValue) && property.getAction() == ActionType.NONE)
		{
			// change action to update if the new and old values are not the same.
			property.setAction(ActionType.UPDATE);
		}
		//got to here, there is change.
		property.setActive(isActive);
		
		if ((property.getAction() == ActionType.UPDATE || property.getAction() == ActionType.ADD)
				&& !newValue.equals(oldValue))
		{
			property.setValueNew(newValue);			
		}
		
		if (oldValue != null && property.getAction() != ActionType.ADD)
		{
			property.setValueOld(oldValue);
		}
		
		if (property.getAction() == ActionType.NONE)
		{
			return null;
		}
		
		property.setTypeName(getPreferredNameDescriptionType(Get.identifierService().getConceptNid(sememe.getAssemblageSequence())));
		return property;
	}

	/**
	 * 
	 * @param concept
	 * @param startDate
	 * @param endDate
	 * @param constructor
	 * @return a List of DesignationTypes for the concept
	 */
	private List<DesignationType> getDesignations(ConceptChronology<?> concept, long startDate, long endDate, Supplier<DesignationType> constructor) {

		List<DesignationType> designations = new ArrayList<>();

		Get.sememeService().getSememesForComponent(concept.getNid()).forEach(sememe ->
		{
			if (sememe.getSememeType() == SememeType.DESCRIPTION)
			{
				boolean hasChild = false;
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<DescriptionSememe>> descriptionVersion 
						= ((SememeChronology) sememe).getLatestVersion(DescriptionSememe.class, STAMP_COORDINATES);
				if (descriptionVersion.isPresent())
				{
					DesignationType d = constructor.get();
					
					d.setAction(determineAction(sememe, startDate, endDate));
					
					if (d.getAction() != ActionType.ADD)
					{
						@SuppressWarnings({ "unchecked", "rawtypes" })
						List<DescriptionSememe<?>> coll = ((SememeChronology) sememe).getVisibleOrderedVersionList(STAMP_COORDINATES);
						Collections.reverse(coll);
						for(DescriptionSememe<?> s : coll)
						{
							if (s.getTime() < startDate) {
								d.setValueOld(s.getText());
								break;
							}
						}
					}
					
					if (d.getAction() == ActionType.UPDATE || d.getAction() == ActionType.ADD) {
						d.setValueNew(descriptionVersion.get().value().getText());
					}
					
					if (d.getValueNew() != null && d.getValueOld() != null && d.getValueNew().equals(d.getValueOld()))
					{
						d.setValueOld(null);
						d.setValueNew(null);
					}
					
					d.setCode(getCodeFromNid(sememe.getNid()));
					d.setVUID(Frills.getVuId(sememe.getNid(), STAMP_COORDINATES).orElse(null));
					d.setActive(descriptionVersion.get().value().getState() == State.ACTIVE);

					//Get the extended type
					Optional<UUID> descType = Frills.getDescriptionExtendedTypeConcept(STAMP_COORDINATES, sememe.getNid());
					if (descType.isPresent())
					{
						d.setTypeName(designationTypes.get(descType.get()));
					}
					else
					{
						log.warn("No extended description type present on description " + sememe.getPrimordialUuid() + " " + descriptionVersion.get().value().getText());
					}

					if (d instanceof gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation)
					{
						//Read any nested properties or subset memberships on this description
						Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties xmlDesignationProperties =
								new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties();
						Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships xmlSubsetMemberships =
								new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships();


						Get.sememeService().getSememesForComponent(sememe.getNid()).forEach((nestedSememe) -> {
							//skip code and vuid properties - they are handled already
							
							if (nestedSememe.getAssemblageSequence() != MetaData.VUID.getConceptSequence() 
									&& nestedSememe.getAssemblageSequence() != MetaData.CODE.getConceptSequence())
							{
								if (ts.wasEverKindOf(nestedSememe.getAssemblageSequence(), VHATConstants.VHAT_ATTRIBUTE_TYPES.getNid()))
								{
									PropertyType property = buildProperty(nestedSememe, startDate, endDate, null);
									if (property != null)
									{
										xmlDesignationProperties.getProperty().add(property);
									}
								}
								//a refset that doesn't represent a mapset
								else if (ts.wasEverKindOf(nestedSememe.getAssemblageSequence(), VHATConstants.VHAT_REFSETS.getNid()) &&
										! ts.wasEverKindOf(nestedSememe.getAssemblageSequence(), IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getNid()))
								{
									SubsetMembership sm = buildSubsetMembership(nestedSememe, startDate, endDate);
									if (sm != null)
									{
										xmlSubsetMemberships.getSubsetMembership().add(sm);
									}
								}
							}
						});

						if (xmlDesignationProperties.getProperty().size() > 0) {
							((gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation)d)
							.setProperties(xmlDesignationProperties);
							hasChild = true;
						}

						if (xmlSubsetMemberships.getSubsetMembership().size() > 0) {
							((gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation)d)
							.setSubsetMemberships(xmlSubsetMemberships);
							hasChild = true;
						}
					}

					if (d.getAction() != ActionType.NONE || hasChild)
					{
						designations.add(d);
					}
				}
			}
		});
		return designations;
	}


	/**
	 *
	 * @param sememe the Chronicle object (concept) representing the Subset
	 * @param startDate
	 * @param endDate
	 * @return the SubsetMembership object built for the sememe, or null
	 */
	private SubsetMembership buildSubsetMembership(SememeChronology<?> sememe, long startDate, long endDate)
	{
		if (sememe.getSememeType() == SememeType.DYNAMIC)
		{
				SubsetMembership subsetMembership = new SubsetMembership();
				subsetMembership.setActive(sememe.isLatestVersionActive(STAMP_COORDINATES));
				subsetMembership.setAction(determineAction(sememe, startDate, endDate));
				
				if (subsetMembership.getAction() == ActionType.NONE)
				{
					return null;
				}
				
				long vuid = Frills.getVuId(Get.identifierService().getConceptNid(sememe.getAssemblageSequence()), 
						STAMP_COORDINATES).orElse(0L).longValue();
				if (vuid > 0)
				{
					subsetMembership.setVUID(vuid);
				}
				else
				{
					log.warn("No VUID found for Subset UUID: " + sememe.getPrimordialUuid());
				}
				
				return subsetMembership;
		}
		else
		{
			log.error("Unexpected sememe type! " + sememe);
			return null;
		}
	}

	/**
	 *
	 * @param concept
	 * @param startDate
	 * @param endDate
	 * @return a List of Relationship objects for the concept
	 */
	private List<Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship> getRelationships(ConceptChronology<?> concept,
			long startDate, long endDate)
	{

		List<Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship> relationships = new ArrayList<>();

		for (AssociationInstance ai : AssociationUtilities.getSourceAssociations(concept.getNid(), STAMP_COORDINATES))
		{
			SememeChronology<?> sc = ai.getData().getChronology();
			ActionType action = determineAction(sc, startDate, endDate);
			
			Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship xmlRelationship =
					new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship();
			
			try
			{
				String newTargetCode = null;
				String oldTargetCode = null;
				if (ai.getTargetComponent().isPresent())
				{
					newTargetCode = getCodeFromNid(Get.identifierService().getNidForUuids(ai.getTargetComponent().get().getPrimordialUuid()));
					if (newTargetCode == null || newTargetCode.isEmpty())
					{
						log.warn("Failed to find new target code for concept " + ai.getTargetComponent().get().getPrimordialUuid());
					}
				}
				
				if (action == ActionType.UPDATE) 
				{
					// This is an active/inactive change
					oldTargetCode = newTargetCode;
					newTargetCode = null;
				}
				else if (action != ActionType.ADD)
				{
					// Get the old target value
					@SuppressWarnings({ "unchecked", "rawtypes" })
					List<DynamicSememe<?>> coll = ((SememeChronology) sc).getVisibleOrderedVersionList(STAMP_COORDINATES);
					Collections.reverse(coll);
					for(DynamicSememe<?> s : coll)
					{
						if (s.getTime() < startDate) {
							AssociationInstance assocInst = AssociationInstance.read(s, null);
							oldTargetCode = getCodeFromNid(Get.identifierService().getNidForUuids(assocInst.getTargetComponent().get().getPrimordialUuid()));
							if (oldTargetCode == null || oldTargetCode.isEmpty())
							{
								log.error("Failed to find old target code for concept " + ai.getTargetComponent().get().getPrimordialUuid());
							}
							break;
						}
					}
					// if NONE && old != new => UPDATE
					if (newTargetCode != null && !newTargetCode.equals(oldTargetCode) && action == ActionType.NONE) {
						action = ActionType.UPDATE;
					}
				}
				
				xmlRelationship.setAction(action);
				xmlRelationship.setActive(ai.getData().getState() == State.ACTIVE);
				xmlRelationship.setTypeName(ai.getAssociationType().getAssociationName());
				if (oldTargetCode != null) {
					xmlRelationship.setOldTargetCode(oldTargetCode);
				}
				if (newTargetCode != null) {
					xmlRelationship.setNewTargetCode(newTargetCode);
				}
				
				if (action != ActionType.NONE)
				{
					relationships.add(xmlRelationship);
				}			
			}
			catch (Exception e)
			{
				// Per Dan, catch()-ing to protect against export failure if this were to cause a problem
				// as this code is being added very late to Release 2
				log.error("Association build failure");
			}
		}
			
		return relationships;
	}

	/**
	 * 
	 * @param conceptNid
	 * @return the preferred description type for the concept 
	 */
	private String getPreferredNameDescriptionType(int conceptNid)
	{
		ArrayList<String> descriptions = new ArrayList<>(1);
		ArrayList<String> inActiveDescriptions = new ArrayList<>(1);
		Get.sememeService().getDescriptionsForComponent(conceptNid).forEach(sememeChronology ->
		{
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Optional<LatestVersion<DescriptionSememe<?>>> latestVersion = ((SememeChronology)sememeChronology).getLatestVersion(DescriptionSememe.class, STAMP_COORDINATES);
			if (latestVersion.isPresent()
					&& VHATConstants.VHAT_PREFERRED_NAME.getPrimordialUuid()
						.equals(Frills.getDescriptionExtendedTypeConcept(STAMP_COORDINATES, sememeChronology.getNid()).orElse(null)))
			{
				if (latestVersion.get().value().getState() == State.ACTIVE)
				{
					descriptions.add(latestVersion.get().value().getText());
				}
				else
				{
					inActiveDescriptions.add(latestVersion.get().value().getText());
				}
			}
		});

		if (descriptions.size() == 0)
		{
			descriptions.addAll(inActiveDescriptions);
		}
		if (descriptions.size() == 0)
		{
			//This doesn't happen for concept that represent subsets, for example.
			log.debug("Failed to find a description flagged as preferred on concept " + Get.identifierService().getUuidPrimordialForNid(conceptNid));
			String description = Frills.getDescription(conceptNid, STAMP_COORDINATES,
					LanguageCoordinates.getUsEnglishLanguagePreferredTermCoordinate()).orElse("ERROR!");
			if (description.equals("ERROR!"))
			{
				log.error("Failed to find any description on concept " + Get.identifierService().getUuidPrimordialForNid(conceptNid));
			}
			return description;
		}
		if (descriptions.size() > 1)
		{
			log.warn("Found " + descriptions.size() + " descriptions flagged as the 'Preferred' vhat type on concept "
					+ Get.identifierService().getUuidPrimordialForNid(conceptNid));
		}
		return descriptions.get(0);
	}

	/**
	 * 
	 * @param object
	 * @param startDate
	 * @param endDate
	 * @return the ActionType object representing the change
	 */
	private ActionType determineAction(ObjectChronology<? extends StampedVersion> object, long startDate, long endDate)
	{
		ActionType action = ActionType.ADD;
		
		if (!fullExportMode)
		{
			List<? extends StampedVersion> versions = object.getVersionList();
			versions.sort(new Comparator<StampedVersion>()
			{
				@Override
				public int compare(StampedVersion o1, StampedVersion o2)
				{
					return -1 * Long.compare(o1.getTime(), o2.getTime());
				}
			});
	
			boolean latest = true;
			int versionCountInDateRange = 0;
			int actionCountPriorToStartDate = 0;
			State beginState = null;
			State endState = null;
		
			for (StampedVersion sv : versions)
			{
				
				if (sv.getTime() < startDate)
				{
					//last value prior to start date
					if (beginState == null )
					{
						beginState = sv.getState();
					}
					actionCountPriorToStartDate++;
				}
				if (sv.getTime() <= endDate && sv.getTime() >= startDate)
				{
					//last value prior to end date
					if (endState == null)
					{
						endState = sv.getState();
					}
					versionCountInDateRange++;
				}
				latest = false;
	
			}
			
			if (beginState == null && endState != null)
			{
				action = ActionType.ADD;
			}
			else if (beginState != null && endState == null)
			{
				action = ActionType.NONE;
			}
			else
			{
				if (beginState != endState)
				{
					action = ActionType.UPDATE;
				}
				else
				{
					if (versionCountInDateRange == 0)
					{
						action = ActionType.NONE;
					}
					else if (versionCountInDateRange > 0)
					{
						action = ActionType.UPDATE;
					}
					else if (actionCountPriorToStartDate > 0 && versionCountInDateRange > 0)
					{
						return ActionType.UPDATE;
					}
					/* The UI does not allow Remove. Only Active and Inactive. May be revisited in R3 
					else if (finalStateIsInactive && actionCountPriorToStartDate > 0)
					{
						return ActionType.REMOVE;
					}
					*/
				}
			}
		}
		
		return action;
	}


	/**
	 * Scan through all (nested) components associated with this concept, and the concept itself, and see if the latest edit
	 * date for any component is within our filter range.
	 * @return true or false, if the concept or a nested value was modified within the date range
	 */
	@SuppressWarnings("rawtypes")
	private boolean wasConceptOrNestedValueModifiedInDateRange(ConceptChronology concept, long startDate)
	{
		@SuppressWarnings("unchecked")
		Optional<LatestVersion<ConceptVersion>> cv = concept.getLatestVersion(ConceptVersion.class, STAMP_COORDINATES);
		if (cv.isPresent())
		{
			if (cv.get().value().getTime() >= startDate)
			{
				return true;
			}
		}

		return hasSememeModifiedInDateRange(concept.getNid(), startDate);
	}

	/**
	 * 
	 * @param nid
	 * @param startDate
	 * @return true or false, if the sememe was modified in the date range
	 */
	private boolean hasSememeModifiedInDateRange(int nid, long startDate)
	{
		//Check all the nested sememes
		return Get.sememeService().getSememesForComponent(nid).anyMatch(sc ->
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<SememeVersion>> sv = ((SememeChronology)sc).getLatestVersion(SememeVersion.class, STAMP_COORDINATES);
			if (sv.isPresent())
			{
				if (sv.get().value().getTime() > startDate)
				{
					return true;
				}
			}
			//recurse
			if (hasSememeModifiedInDateRange(sc.getNid(), startDate))
			{
				return true;
			}
			return false;
		});
	}

	/**
	 * 
	 * @param componentNid
	 * @return the Code value found based on the Nid
	 */
	private String getCodeFromNid(int componentNid)
	{

		Optional<SememeChronology<? extends SememeVersion<?>>> sc = Get.sememeService().getSememesForComponentFromAssemblage(componentNid,
				MetaData.CODE.getConceptSequence()).findFirst();
		if (sc.isPresent())
		{
			//There was a bug in the older terminology loaders which loaded 'Code' as a static sememe, but marked it as a dynamic sememe.
			//So during edits, new entries would get saves as dynamic sememes, while old entries were static.  Handle either....

			if (sc.get().getSememeType() == SememeType.STRING)
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<StringSememe<?>>> sv = ((SememeChronology)sc.get()).getLatestVersion(StringSememe.class, STAMP_COORDINATES);
				if (sv.isPresent())
				{
					return sv.get().value().getString();
				}
			}
			else if (sc.get().getSememeType() == SememeType.DYNAMIC)  //this path will become dead code, after the data is fixed.
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<? extends DynamicSememe>> sv = ((SememeChronology) sc.get()).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
				if (sv.isPresent())
				{
					if (sv.get().value().getData() != null && sv.get().value().getData().length == 1)
					{
						return sv.get().value().getData()[0].dataToString();
					}
				}
			}
			else
			{
				log.error("Unexpected sememe type for 'Code' sememe on nid " + componentNid);
			}
		}
		return null;
	}

	/**
	 * 
	 * @param writeTo
	 */
	private void writeXml(OutputStream writeTo)
	{
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(Terminology.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(terminology, writeTo);

		}
		catch (Exception e)
		{
			log.error("Unexpected", e);
			throw new RuntimeException(e);
		}
	}
}
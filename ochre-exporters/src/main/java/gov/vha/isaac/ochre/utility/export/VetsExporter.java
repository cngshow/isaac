package gov.vha.isaac.ochre.utility.export;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
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
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.TaxonomyService;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
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
import gov.vha.isaac.ochre.model.sememe.DynamicSememeUsageDescriptionImpl;


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
	//VHAT Metadata -> "Attribute Types"
	final UUID vhatPropertyTypesUUID = UUID.fromString("eb7696e7-fe40-5985-9b2e-4e3d840a47b7"); 
	final int vhatPropertyTypesNid = Get.identifierService().getNidForUuids(vhatPropertyTypesUUID);
	
	//VHAT Metadata -> "Refsets"
	final UUID vhatRefsetTypesUUID = UUID.fromString("fab80263-6dae-523c-b604-c69e450d8c7f"); 
	final int vhatRefsetTypesNid = Get.identifierService().getNidForUuids(vhatRefsetTypesUUID);
	int codeAssemblageConceptSeq;
	
	// VHAT CodeSystem
	final UUID vhatCodeSystemUUID = UUID.fromString("6e60d7fd-3729-5dd3-9ce7-6d97c8f75447"); 
	final int vhatCodeSystemNid = Get.identifierService().getNidForUuids(vhatCodeSystemUUID); 
	
	//VHAT Description Types -> Preferred Name
	final UUID preferredNameExtendedType = UUID.fromString("a20e5175-6257-516a-a97d-d7f9655916b8");
	
	boolean fullExportMode = false;
	
	public VetsExporter()
	{
	}
	
	
	private UUID getFromMapByValue(Map<UUID, String> haystack, String needle) {
		if (haystack == null || needle == null || (haystack.size() == 0) | (needle.length() == 0)) {
			return null;
		}
		
		String nddl = needle.toLowerCase();
		
		for (Map.Entry<UUID, String> entry : haystack.entrySet()) {
			String hstk = entry.getValue().toLowerCase();
			if (hstk.equals(nddl)) {
				return entry.getKey();
			}
		}
		
		return null;
	}

	
	/**
	 * 
	 * @param writeTo
	 * @param startDate - only export concepts modified on or after this date.  Set to 0, if you want to start from the beginning
	 * @param endDate - only export concepts where their most recent modified date is on or before this date.  Set to Long.MAX_VALUE to get everything.
	 * @param fullExportMode - if true, exports all content present at the end of the date range, ignoring start date.  All actions are set to add.
	 *   If false - delta mode - calculates the action based on the start and end dates, and includes only the minimum required elements in the xml file. 
	 */
	public void export(OutputStream writeTo, long startDate, long endDate, boolean fullExportMode) {
		
		this.fullExportMode = fullExportMode;
		
		STAMP_COORDINATES = new StampCoordinateImpl(StampPrecedence.PATH, new StampPositionImpl(endDate, TermAux.DEVELOPMENT_PATH.getConceptSequence()), 
						ConceptSequenceSet.EMPTY, State.ANY_STATE_SET);
		// Build Assemblages map
		Get.sememeService().getAssemblageTypes().forEach((assemblageSeqId) -> {
			assemblagesMap.put(Get.conceptSpecification(assemblageSeqId).getPrimordialUuid(), 
					Get.conceptSpecification(assemblageSeqId).getConceptDescriptionText());
		});
		
		codeAssemblageConceptSeq = Get.identifierService().getConceptSequenceForUuids(getFromMapByValue(assemblagesMap, "Code"));
		
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
		
		// ISAAC Associations => RelationshipType UUID
		UUID vhatAssociationTypesUUID = UUID.fromString("55f56c52-757a-5db8-bf1e-3ed613711386");

		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatAssociationTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			relationshipTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});
		
		if (fullExportMode)
		{
			// Build XML
			for (String s : relationshipTypes.values()) {
				xmlType = new Terminology.Types.Type();
				xmlType.setKind(KindType.RELATIONSHIP_TYPE);
				xmlType.setName(s);
				terminology.getTypes().getType().add(xmlType);
			}
		}
		
		
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatPropertyTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			propertyTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});
		
		if (fullExportMode)
		{
			// Build XML
			for (String s : propertyTypes.values()) {
				xmlType = new Terminology.Types.Type();
				xmlType.setKind(KindType.PROPERTY_TYPE);
				xmlType.setName(s);
				terminology.getTypes().getType().add(xmlType);
			}
		}
		
		// ISAAC Descriptions => DesignationType UUID
		UUID vhatDesignationTypesUUID = UUID.fromString("09c43aa9-eaed-5217-bc5f-23cacca4df38"); 
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatDesignationTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			designationTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});
		
		if (fullExportMode)
		{
			// Build XML
			for (String s : designationTypes.values()) {
				xmlType = new Terminology.Types.Type();
				xmlType.setKind(KindType.DESIGNATION_TYPE);
				xmlType.setName(s);
				terminology.getTypes().getType().add(xmlType);
			}
		}
		
		// Get data, Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(Get.identifierService().getNidForUuids(vhatRefsetTypesUUID)).forEach((tcs) -> 
		{
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(tcs);
			// Excluding these:
			if (concept.getPrimordialUuid().equals(UUID.fromString("f2df3cf5-a426-50f9-a660-081a5ca22c70")) //All vhat concepts
				|| concept.getPrimordialUuid().equals(UUID.fromString("52460eeb-1388-512d-a5e4-fddd64fe0aee"))) {  //Missing SDO Code Systems Concepts
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
		
		
		ConceptChronology<? extends ConceptVersion<?>> vhatConcept = Get.conceptService().getConcept(vhatCodeSystemNid);
		
		xmlCodeSystem.setAction(ActionType.NONE);
		xmlCodeSystem.setName(getPreferredNameDescriptionType(vhatConcept.getNid()));
		xmlCodeSystem.setVUID(Frills.getVuId(vhatCodeSystemNid, null).orElse(null));
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
			if (Frills.definesMapping(concept.getConceptSequence()) && ts.wasEverKindOf(concept.getConceptSequence(), vhatRefsetTypesNid)) 
			{
				Terminology.CodeSystem.Version.MapSets.MapSet xmlMapSet = new Terminology.CodeSystem.Version.MapSets.MapSet();
				xmlMapSet.setAction(determineAction((ObjectChronology<? extends StampedVersion>) concept, startDate, endDate));
				xmlMapSet.setActive(concept.isLatestVersionActive(STAMP_COORDINATES));
				xmlMapSet.setCode(getCodeFromNid(concept.getNid()));
				xmlMapSet.setName(getPreferredNameDescriptionType(concept.getNid())); 
				xmlMapSet.setVUID(Frills.getVuId(concept.getNid(), STAMP_COORDINATES).orElse(null));
				
				// Designations
				Terminology.CodeSystem.Version.MapSets.MapSet.Designations xmlMapSetDesignations = new Terminology.CodeSystem.Version.MapSets.MapSet.Designations();
				
				for (DesignationType d : getDesignations(concept, startDate, endDate, 
						(() -> new Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation())))
				{
					xmlMapSetDesignations.getDesignation().add((Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation)d);
				}
				
				xmlMapSet.setDesignations(xmlMapSetDesignations);
				

				//TODO this is all broken, you can't use nids
//				int dsedNid = -2147483452; // SourceCodeSystem / SourceVersionName / TargetCodeSystem / TargetVersionName
//				int dsedSeq = Get.conceptService().getConcept(dsedNid).getConceptSequence();
//				Get.sememeService().getSememesForComponentFromAssemblage(_concept.getNid(), dsedSeq).forEach((sememe) -> {
//					Optional<LatestVersion<? extends DynamicSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
//					//System.out.println(sememeVersion.get().value());
//					DynamicSememeData dsd[] = sememeVersion.get().value().getData();
//					if (dsd[0].getDataObject().equals(-2147483446)) {
//						//System.out.println("SourceCodeSystem = " + dsd[1].getDataObject());
//						_xmlMapSet.setSourceCodeSystem(dsd[1].getDataObject().toString());
//					} else if (dsd[0].getDataObject().equals(-2147483445)) {
//						//System.out.println("SourceVersionName = " + dsd[1].getDataObject());
//						_xmlMapSet.setSourceVersionName(dsd[1].getDataObject().toString());
//					} else if (dsd[0].getDataObject().equals(-2147483444)) {
//						//System.out.println("TargetCodeSystem = " + dsd[1].getDataObject());
//						_xmlMapSet.setTargetCodeSystem(dsd[1].getDataObject().toString());
//					} else if (dsd[0].getDataObject().equals(-2147483443)) {
//						//System.out.println("TargetVersionName = " + dsd[1].getDataObject());
//						_xmlMapSet.setTargetVersionName(dsd[1].getDataObject().toString());
//					}
//				});
				
				// Properties
				Terminology.CodeSystem.Version.MapSets.MapSet.Properties xmlMapSetProperties = new Terminology.CodeSystem.Version.MapSets.MapSet.Properties();
				
				for (PropertyType pt :  readPropertyTypes(concept.getNid(), startDate, endDate, 
						() -> new Terminology.CodeSystem.Version.MapSets.MapSet.Properties.Property()))
				{
					xmlMapSetProperties.getProperty().add((Terminology.CodeSystem.Version.MapSets.MapSet.Properties.Property)pt);
				}
				xmlMapSet.setProperties(xmlMapSetProperties);
				
				
				// There are no relationships on mapset definitions.
				// TODO: MapEntries
				// TODO: MapEntry->Designations
				// TODO: MapEntry->Properties
				// TODO: MapEntry->Relationships
				
				
				//_xmlMapSet.setMapEntries(value);

				if (xmlMapSet.getAction() != ActionType.NONE || xmlMapSet.getMapEntries().getMapEntry().size() > 0)
				{
					xmlMapSetCollection.add(xmlMapSet);
				}
			}
			//TODO I noticed there is a small discrepencey between the "all vhat concepts" refset, and the children of VHAT.  Need to determine if this
			//is something other than just metadata, or if there are some orphans in there.
			else if (!ts.wasEverKindOf(concept.getConceptSequence(), vhatCodeSystemNid))
			{
				// Needed to ignore all the dynamically created/non-imported concepts
				skippedForNonVHAT.getAndIncrement();
			}
			else if (concept.getNid() == vhatCodeSystemNid)
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
					
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept xmlCodedConcept = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept();
					xmlCodedConcept.setAction(determineAction((ObjectChronology<? extends StampedVersion>)concept, startDate, endDate));
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
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships xmlRelationships = 
							new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships();
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
	
	private List<PropertyType> readPropertyTypes(int componentNid, long startDate, long endDate, Supplier<PropertyType> constructor)
	{
		ArrayList<PropertyType> pts = new ArrayList<>();
		
		Get.sememeService().getSememesForComponent(componentNid).forEach((sememe) -> 
		{
			//skip code and vuid properties - they have special handling
			if (sememe.getNid() != MetaData.VUID.getNid() && sememe.getSememeSequence() != codeAssemblageConceptSeq
					&& ts.wasEverKindOf(sememe.getAssemblageSequence(), vhatPropertyTypesNid))
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
	 * @param sememe
	 * @param startDate
	 * @param endDate
	 * @return Map of property Objects
	 */
	private PropertyType buildProperty(SememeChronology<?> sememe, long startDate, long endDate, Supplier<PropertyType> constructor) 
	{
		String value = null;
		boolean isActive = false;
		if (sememe.getSememeType() == SememeType.DYNAMIC)
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<? extends DynamicSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
			if (sememeVersion.isPresent() && sememeVersion.get().value().getData() != null && sememeVersion.get().value().getData().length > 0)
			{
				value = sememeVersion.get().value().getData()[0] == null ? null : sememeVersion.get().value().getData()[0].dataToString();
				isActive = sememeVersion.get().value().getState() == State.ACTIVE;
			}
		}
		else if (sememe.getSememeType() == SememeType.STRING)
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<? extends StringSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(StringSememe.class, STAMP_COORDINATES);
			if (sememeVersion.isPresent())
			{
				value = sememeVersion.get().value().getString();
				isActive = sememeVersion.get().value().getState() == State.ACTIVE;
			}
		}
		else
		{
			log.warn("Unexpectedly passed sememe " + sememe + " when we only expected a dynamic or a string type");
			return null;
		}
		
		if (value == null)
		{
			return null;
		}
		
		PropertyType property = constructor == null ? new gov.va.med.term.vhat.xml.model.PropertyType() : constructor.get();
		property.setAction(determineAction((ObjectChronology<? extends StampedVersion>) sememe, startDate, endDate));
		if (property.getAction() == ActionType.NONE)
		{
			return null;
		}
		property.setActive(isActive);
		property.setValueNew(value);
		//property.setValueOld("");  //TODO old value?
		property.setTypeName(getPreferredNameDescriptionType(Get.identifierService().getConceptNid(sememe.getAssemblageSequence())));
		return property;
	}
	
	/**
	 * 
	 * @param sememe
	 * @param startDate
	 * @param endDate
	 * @return Map of Objects representing the Designation elements
	 */
	private List<DesignationType> getDesignations(ConceptChronology<?> concept, long startDate, long endDate, Supplier<DesignationType> constructor) {
		
		List<DesignationType> designations = new ArrayList<>();
		
		Get.sememeService().getSememesForComponent(concept.getNid()).forEach(sememe -> 
		{
			if (sememe.getSememeType() == SememeType.DESCRIPTION)
			{
				boolean hasChild = false;
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<DescriptionSememe>> descriptionVersion = ((SememeChronology) sememe).getLatestVersion(DescriptionSememe.class, STAMP_COORDINATES);
				if (descriptionVersion.isPresent()) 
				{
					DesignationType d = constructor.get(); 
					d.setValueNew(descriptionVersion.get().value().getText());
					d.setAction(determineAction((ObjectChronology<? extends StampedVersion>) sememe, startDate, endDate));
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
							if (sememe.getNid() != MetaData.VUID.getNid() && sememe.getSememeSequence() != codeAssemblageConceptSeq)
							{
								if (ts.wasEverKindOf(nestedSememe.getAssemblageSequence(), vhatPropertyTypesNid)) 
								{
									PropertyType property = buildProperty(nestedSememe, startDate, endDate, null);
									if (property != null)
									{
										xmlDesignationProperties.getProperty().add(property);
									}
								}
								//a refset that doesn't represent a mapset
								else if (ts.wasEverKindOf(nestedSememe.getAssemblageSequence(), vhatRefsetTypesNid) &&
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
	 * @param sememe
	 * @param startDate
	 * @param endDate
	 * @return Map of Objects representing the SubsetMemberships elements
	 */
	private SubsetMembership buildSubsetMembership(SememeChronology<?> sememe, long startDate, long endDate) 
	{
		if (sememe.getSememeType() == SememeType.DYNAMIC)
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<? extends DynamicSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
			if (sememeVersion.isPresent())
			{
				if (sememeVersion.get().value().getData() != null && sememeVersion.get().value().getData().length > 0)
				{
					log.warn("A sememe with data was passed to readSubsetMembership!");
					return null;
				}
				
				SubsetMembership subsetMembership = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships.SubsetMembership();
				subsetMembership.setActive(sememeVersion.get().value().getState() == State.ACTIVE);
				subsetMembership.setAction(determineAction((ObjectChronology<? extends StampedVersion>) sememe, startDate, endDate));
				
				if (subsetMembership.getAction() == ActionType.NONE)
				{
					return null;
				}
				subsetMembership.setVUID(subsetMap.get(DynamicSememeUsageDescriptionImpl.read(sememeVersion.get().value().getAssemblageSequence()).getDynamicSememeName()));
				return subsetMembership;
			}
			return null;
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
	 * @return List of Maps of relationship Objects
	 */
	private List<Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship> getRelationships(ConceptChronology<?> concept, 
			long startDate, long endDate) 
	{
		
		List<Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship> relationships = new ArrayList<>();
		
		for (AssociationInstance ai : AssociationUtilities.getSourceAssociations(concept.getNid(), STAMP_COORDINATES)) 
		{
			SememeChronology<?> sc = ai.getData().getChronology();
			ActionType action = determineAction((ObjectChronology<? extends StampedVersion>) sc, startDate, endDate);
			if (action != ActionType.NONE)
			{
				Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship xmlRelationship = 
						new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship();
				xmlRelationship.setAction(action);
				xmlRelationship.setActive(ai.getData().getState() == State.ACTIVE);
				
				//xmlRelationship.setOldTargetCode("");  //TODO old value?
				xmlRelationship.setTypeName(ai.getAssociationType().getAssociationName());
				if (ai.getTargetComponent().isPresent())
				{
					xmlRelationship.setNewTargetCode(getCodeFromNid(Get.identifierService().getNidForUuids(ai.getTargetComponent().get().getPrimordialUuid())));
					if (xmlRelationship.getNewTargetCode() == null)
					{
						log.warn("Failed to find target code for concept " + ai.getTargetComponent().get().getPrimordialUuid());
					}
				}
				relationships.add(xmlRelationship);
			}
		}
		return relationships;
	}
	
	private String getPreferredNameDescriptionType(int conceptNid)
	{
		ArrayList<String> descriptions = new ArrayList<>(1);
		ArrayList<String> inActiveDescriptions = new ArrayList<>(1);
		Get.sememeService().getDescriptionsForComponent(conceptNid).forEach(sememeChronology ->
		{
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Optional<LatestVersion<DescriptionSememe<?>>> latestVersion = ((SememeChronology)sememeChronology).getLatestVersion(DescriptionSememe.class, STAMP_COORDINATES);
			if (latestVersion.isPresent() 
					&& preferredNameExtendedType.equals(Frills.getDescriptionExtendedTypeConcept(STAMP_COORDINATES, sememeChronology.getNid()).orElse(null)))
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
	 * @param _concept
	 * @return
	 */
	private ActionType determineAction(ObjectChronology<? extends StampedVersion> object, long startDate, long endDate)
	{
		if (fullExportMode)
		{
			return ActionType.ADD;
		}
		List<? extends StampedVersion> versions = object.getVersionList();
		versions.sort(new Comparator<StampedVersion>()
		{
			//TODO check and see if Dan got the sort backwards
			@Override
			public int compare(StampedVersion o1, StampedVersion o2)
			{
				return -1 * Long.compare(o1.getTime(), o2.getTime());
			}
		});
		
		boolean latest = true;
		boolean finalStateIsInactive = false;
		boolean firstStateIsInactive = false;
		int versionCountInDateRange = 0;
		int actionCountPriorToStartDate = 0;
		for (StampedVersion sv : versions)
		{
			if (sv.getTime() < startDate)
			{
				actionCountPriorToStartDate++;
			}
			if (sv.getTime() <= endDate && sv.getTime() >= startDate)
			{
				if (latest && sv.getState() != State.ACTIVE)
				{
					finalStateIsInactive = true;
				}
				firstStateIsInactive = sv.getState() != State.ACTIVE ? true : false;
				versionCountInDateRange++;
			}
			latest = false;
			
		}

		//TODO this logic needs some serious sanity checking / junit tests.  Dan is up to late to get it right on the first try.
		if (firstStateIsInactive && finalStateIsInactive)
		{
			return ActionType.NONE;
		}
		else if (finalStateIsInactive && actionCountPriorToStartDate > 0)
		{
			return ActionType.REMOVE;
		}
		
		if (versionCountInDateRange == 0)
		{
			return ActionType.NONE;
		}
		
		if (actionCountPriorToStartDate > 0 && versionCountInDateRange > 0)
		{
			return ActionType.UPDATE;
		}
		else
		{
			return ActionType.ADD;
		}
	}


	/**
	 * Scan through all (nested) components associated with this concept, and the concept itself, and see if the latest edit
	 * date for any component is within our filter range.
	 * @return
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
	 * This is a recursive method
	 * @param nid
	 * @param startDate
	 * @param endDate
	 * @return
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

	private String getCodeFromNid(int componentNid) 
	{
		
		Optional<SememeChronology<? extends SememeVersion<?>>> sc = Get.sememeService().getSememesForComponentFromAssemblage(componentNid, 
				codeAssemblageConceptSeq).findFirst();
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
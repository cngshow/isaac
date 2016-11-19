package gov.vha.isaac.ochre.utility.export;

import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gov.va.med.term.vhat.xml.model.ActionType;
import gov.va.med.term.vhat.xml.model.KindType;
import gov.va.med.term.vhat.xml.model.Terminology;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.TaxonomyService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.associations.AssociationInstance;
import gov.vha.isaac.ochre.associations.AssociationUtilities;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;
import gov.vha.isaac.ochre.model.sememe.DynamicSememeUsageDescriptionImpl;


public class VetsExporter {

	private Logger log = LogManager.getLogger();
	
	private Map<UUID, String> designationTypes = new HashMap<>();
	private Map<UUID, String> propertyTypes = new HashMap<>();
	private Map<UUID, String> relationshipTypes = new HashMap<>();
	private Map<String, List<String>> subsets = new TreeMap<>();
	
	private Map<UUID, String> assemblagesMap = new HashMap<>();
	private Map<String, Long> subsetMap = new HashMap<>();
	
	private Terminology terminology;
	
	private List<Terminology.CodeSystem.Version.MapSets.MapSet>_xmlMapSetCollection = new ArrayList<>();
	
	private StampCoordinate STAMP_COORDINATES = StampCoordinates.getDevelopmentLatest();
	
	
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
	 */
	public void export(OutputStream writeTo, long startDate, long endDate) {
		
		// Build Assemblages map
		Get.sememeService().getAssemblageTypes().forEach((assemblageSeqId) -> {
			assemblagesMap.put(Get.conceptSpecification(assemblageSeqId).getPrimordialUuid(), 
					Get.conceptSpecification(assemblageSeqId).getConceptDescriptionText());
		});
		
		// XML object
		terminology = new Terminology();
		
		// Types
		terminology.setTypes(new Terminology.Types());
		Terminology.Types.Type _xmlType;
		
		// Subsets/Refsets
		terminology.setSubsets(new Terminology.Subsets());
		Terminology.Subsets.Subset _xmlSubset;
		
		// CodeSystem
		Terminology.CodeSystem _xmlCodeSystem = new Terminology.CodeSystem();
		Terminology.CodeSystem.Version _xmlVersion = new Terminology.CodeSystem.Version();
		Terminology.CodeSystem.Version.CodedConcepts _xmlCodedConcepts = new Terminology.CodeSystem.Version.CodedConcepts();
		//Terminology.CodeSystem.Version.CodedConcepts.CodedConcept _xmlCodedConcept = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept();
		
		// ISAAC Associations => RelationshipType UUID
		UUID vhatAssociationTypesUUID = UUID.fromString("55f56c52-757a-5db8-bf1e-3ed613711386");
		//int vhatAssociationTypesNid = Get.identifierService().getNidForUuids(vhatAssociationTypesUUID); //-2147481336
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatAssociationTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			relationshipTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});
		
		// Build XML
		for (String s : relationshipTypes.values()) {
			_xmlType = new Terminology.Types.Type();
			_xmlType.setKind(KindType.RELATIONSHIP_TYPE);
			_xmlType.setName(s);
			//terminology.getTypes().getType().add(_xmlType);
		}
		
		// ISAAC Attributes => PropertyType UUID
		UUID vhatPropertyTypesUUID = UUID.fromString("eb7696e7-fe40-5985-9b2e-4e3d840a47b7"); 
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatPropertyTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			propertyTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});
		
		// Build XML
		for (String s : propertyTypes.values()) {
			_xmlType = new Terminology.Types.Type();
			_xmlType.setKind(KindType.PROPERTY_TYPE);
			_xmlType.setName(s);
			//terminology.getTypes().getType().add(_xmlType);
		}
		
		// ISAAC Descriptions => DesignationType UUID
		UUID vhatDesignationTypesUUID = UUID.fromString("09c43aa9-eaed-5217-bc5f-23cacca4df38"); 
		//int vhatDesignationTypesNid = Get.identifierService().getNidForUuids(vhatDesignationTypesUUID); //-2147481914
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatDesignationTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			designationTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});
		
		// Build XML
		for (String s : designationTypes.values()) {
			_xmlType = new Terminology.Types.Type();
			_xmlType.setKind(KindType.DESIGNATION_TYPE);
			_xmlType.setName(s);
			//terminology.getTypes().getType().add(_xmlType);
		}
		
		// ISAAC VHAT Refsets => Subsets UUID
		UUID vhatRefsetsUUID = UUID.fromString("fab80263-6dae-523c-b604-c69e450d8c7f");
		//int vhatRefsetsNid = Get.identifierService().getNidForUuids(vhatRefsetsUUID); // -2147481168
		// Get data, Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(Get.identifierService().getNidForUuids(vhatRefsetsUUID)).forEach((tcs) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(tcs);
			// Excluding these:
			// 		All VHAT Concepts / UUID: f2df3cf5-a426-50f9-a660-081a5ca22c70 / Nid: -2147480814
			// 		Missing SDO Code System Concepts / UUID: 52460eeb-1388-512d-a5e4-fddd64fe0aee / Nid: -2146188289
			if (concept.getPrimordialUuid() == UUID.fromString("f2df3cf5-a426-50f9-a660-081a5ca22c70") 
				|| 	concept.getPrimordialUuid() == UUID.fromString("52460eeb-1388-512d-a5e4-fddd64fe0aee")) {
				// Skip
			} else {
				Get.sememeService().getSememesForComponent(concept.getNid()).forEach((sememe) -> {
					if (sememe.getSememeType() == SememeType.STRING) {
						@SuppressWarnings({ "rawtypes", "unchecked" })
						Optional<LatestVersion<? extends StringSememe<?>>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(StringSememe.class, STAMP_COORDINATES);
						
						if (sememeVersion.isPresent()) {
							List<String> _subsetList = new ArrayList<>();
							ActionType action = determineAction(sememeVersion.get().value().getChronology(), startDate, endDate);
							_subsetList.add(action.toString()); // Action
							String subsetName = concept.getConceptDescriptionText();
							_subsetList.add(sememeVersion.get().value().getString()); // VUID
							// I'm assuming this will always be 'true' or 'false' - never empty or another identifier
							String active = Boolean.toString(sememe.isLatestVersionActive(STAMP_COORDINATES));
							_subsetList.add(active); // Active
							// Just incase it's needed, might as well have it
							_subsetList.add(concept.getPrimordialUuid().toString()); // UUID
							// Add to map
							subsets.put(subsetName, _subsetList);
						}
					}
				});
			}
		});
		
		// Build XML
		for (Map.Entry<String, List<String>> entry : subsets.entrySet()) {
			_xmlSubset = new Terminology.Subsets.Subset();
			String name = entry.getKey();
			List<String> al = entry.getValue(); // 0: action, 1: VUID, 2: active, 3: UUID
			_xmlSubset.setAction(ActionType.fromValue(al.get(0)));
			_xmlSubset.setName(name);
			long vuid = Long.valueOf(al.get(1));
			_xmlSubset.setVUID(vuid);
			_xmlSubset.setActive(Boolean.parseBoolean(al.get(2)));
			terminology.getSubsets().getSubset().add(_xmlSubset);
			subsetMap.put(name, vuid);
		}
		
		// VHAT CodeSystem
		UUID vhatCodeSystemUUID = UUID.fromString("6e60d7fd-3729-5dd3-9ce7-6d97c8f75447"); 
		int vhatCodeSystemNid = Get.identifierService().getNidForUuids(vhatCodeSystemUUID); //-2147377575
		ConceptChronology<? extends ConceptVersion<?>> vhatConcept = Get.conceptService().getConcept(vhatCodeSystemNid);
		String csName = vhatConcept.getConceptDescriptionText();
		Long csVUID = Frills.getVuId(vhatCodeSystemNid, null).orElse(0L); // Probably not right
		String csPrefDesigType = "Preferred Name"; // ? TODO:
		String csDescription = Frills.getDescription(vhatCodeSystemNid).orElse("none"); // This should be "VHA Terminology" but can't seem to get that ... easily
		String csCopyright = "2007"; // ? TODO: 
		String csCopyrightURL = ""; // ? TODO:
		
		_xmlCodeSystem.setAction(ActionType.NONE);
		_xmlCodeSystem.setName(csName);
		_xmlCodeSystem.setVUID(csVUID);
		_xmlCodeSystem.setDescription(csDescription);
		_xmlCodeSystem.setCopyright(csCopyright);
		_xmlCodeSystem.setCopyrightURL(csCopyrightURL);
		_xmlCodeSystem.setPreferredDesignationType(csPrefDesigType);
		
		_xmlVersion.setAppend(Boolean.TRUE);
		_xmlVersion.setName("Authoring Version"); // ? TODO:
		_xmlVersion.setDescription("This is the version that is given to authoring changes before they are finalized."); // ? TODO:
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String formattedDate = sdf.format(sdf.parse("2011-02-16"));
			XMLGregorianCalendar _xmlEffDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(formattedDate);
			XMLGregorianCalendar _xmlRelDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(formattedDate);
			_xmlVersion.setEffectiveDate(_xmlEffDate);
			_xmlVersion.setReleaseDate(_xmlRelDate);
		} catch (DatatypeConfigurationException dtce) {
			// TODO: Just leave empty elements if there is a parsing error?
		} catch (ParseException pe) {
			// TODO:
		}
		
		_xmlVersion.setSource(""); // TODO:
		
		// VHAT Module
		// CodeSystems : Standard Code Systems  
		//UUID vhatStandardCodeSystemsUUID = UUID.fromString("fa27aa69-ba88-556d-88ba-77b9be26db60");
		//int vhatStandardCodeSystemsNid = Get.identifierService().getNidForUuids(vhatStandardCodeSystemsUUID); // -2147387560;
		int vhatPropertyTypesNid = Get.identifierService().getNidForUuids(vhatPropertyTypesUUID); //-2147481872
		
		TaxonomyService ts = Get.taxonomyService();
		int vhatSequence = Get.identifierService().getConceptSequenceForUuids(UUID.fromString("6e60d7fd-3729-5dd3-9ce7-6d97c8f75447"));  //VHAT root concept
		AtomicInteger skippedForNonVHAT = new AtomicInteger();
		AtomicInteger skippedDateRange = new AtomicInteger();
		AtomicInteger observedVhatConcepts = new AtomicInteger();
		AtomicInteger exportedVhatConcepts = new AtomicInteger();
		
		Get.conceptService().getConceptChronologyStream().forEach((_concept) -> {
			
			//TODO I noticed there is a small discrepencey between the "all vhat concepts" refset, and the children of VHAT.  Need to determine if this
			//is something other than just metadata, or if there are some orphans in there.
			if (!ts.wasEverKindOf(_concept.getConceptSequence(), vhatSequence))
			{
				// Needed to ignore all the dynamically created/non-imported concepts
				skippedForNonVHAT.getAndIncrement();
			}
			else
			{
				observedVhatConcepts.getAndIncrement();
				int _conceptNid = _concept.getNid();
				
				if (!wasModifiedInDateRange(_concept, startDate, endDate))
				{
					skippedDateRange.getAndIncrement();
				}
				else
				{
					exportedVhatConcepts.getAndIncrement();
					
					//TODO at this point, we know some component related to this concept was modified in our specified date range - but we don't know which one.
					//For a proper TDS changeset file, we need to stub out certain things like the concept with an action field of "none" - but then when we encounter
					//the component that was new (action = add) or retired (action = remove) or just edited (action = update) we need to set it appropriately
					
					//TODO this needs to change - name needs to come from a specific description type, not an arbitrary one, which is what the convenience method does.
					// Not sure if this is appropriate or not
					String _name = Get.conceptSpecification(_conceptNid).getConceptDescriptionText();
					// Need to skip over/handle the blank one in the GUI/DB "No desc for: -2147304122"?
					if (!(_name.length() > 0)) {
						log.error("Missing description for concept " + _concept.getPrimordialUuid());
//Debug code
//						Get.sememeService().getSememesForComponent(_concept.getNid()).forEach(sc ->
//						{
//							System.out.println(sc.toUserString());
//						});
						return;
					}
					
					long _vuid = Frills.getVuId(_conceptNid, null).orElse(0L);
					if (_vuid == 0)
					{
						log.warn("Missing VUID for concept " + _conceptNid);
					}
					
					String _code = getCodeFromNid(_conceptNid);
					boolean _active = _concept.isLatestVersionActive(STAMP_COORDINATES);
					
					//System.out.println("*** " + _name + " -- " + _vuid + " -- " + _code + " -- " + _active);
					
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept _xmlCodedConcept = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept();
					//TODO use this action logic elsewhere.
					_xmlCodedConcept.setAction(determineAction((ObjectChronology<? extends StampedVersion>)_concept, startDate, endDate));
					_xmlCodedConcept.setName(_name);
					_xmlCodedConcept.setVUID(_vuid);
					_xmlCodedConcept.setCode(_code);
					_xmlCodedConcept.setActive(Boolean.valueOf(_active));
					
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations _xmlDesignations = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations();
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties _xmlProperties = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties();
					//Terminology.CodeSystem.Version.MapSets.MapSet.Designations _xmlMapSetDesignations = new Terminology.CodeSystem.Version.MapSets.MapSet.Designations();
					//Terminology.CodeSystem.Version.MapSets.MapSet.Properties _xmlMapSetProperties = new Terminology.CodeSystem.Version.MapSets.MapSet.Properties();
					
					// TODO: This isn't quite right, either
					Get.sememeService().getSememesForComponent(_conceptNid).forEach((_sememe) -> {
						Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation _xmlDesignation = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation();
						Map<String, Object> m = getDesignations(_sememe, startDate, endDate);
						if (m != null) { // TODO: This is a hack for now
							_xmlDesignation = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation();
							_xmlDesignation.setAction((ActionType) m.get("Action"));
							_xmlDesignation.setCode((String) m.get("Code"));
							_xmlDesignation.setTypeName((String) m.get("TypeName"));
							_xmlDesignation.setValueNew((String) m.get("ValueNew"));
							_xmlDesignation.setVUID((Long) m.get("VUID"));
							_xmlDesignation.setActive((Boolean) m.get("Active"));
							
							Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties _xmlDesignationProperties = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties();
							Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships _xmlSubsetMemberships = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships();
							
							if (Frills.hasNestedSememe(_sememe)) {
								Get.sememeService().getSememesForComponent(_sememe.getNid()).forEach((_s) -> {
									if (_s.getSememeType() == SememeType.DYNAMIC) {
										// TODO: Properties
										if (ts.wasEverKindOf(_s.getAssemblageSequence(), vhatPropertyTypesNid)) {
											Map<String, Object> prop = getProperties(_s, startDate, endDate);
											
											if (!prop.isEmpty()) {
												gov.va.med.term.vhat.xml.model.PropertyType _xmlDesignationProperty = new gov.va.med.term.vhat.xml.model.PropertyType();
												_xmlDesignationProperty.setAction((ActionType) prop.get("Action"));
												_xmlDesignationProperty.setTypeName((String) prop.get("TypeName"));
												_xmlDesignationProperty.setValueNew((String) prop.get("ValueNew"));
												_xmlDesignationProperty.setActive((Boolean) prop.get("Active"));
												_xmlDesignationProperties.getProperty().add(_xmlDesignationProperty);
												//System.out.println("Action=" + (ActionType) prop.get("Action") + ", TypeName=" + (String) prop.get("TypeName") + ", ValueNew=" + (String) prop.get("ValueNew") + ", Active=" + (Boolean) prop.get("Active"));
											}
										}
									
										// TODO: SubsetMemberships
										// TODO: Validate there are no duplicate Subset entries
										else {
											Map<String, Object> subMems = getSubsetMemberships(_s, startDate, endDate);
											if (!subMems.isEmpty()) {
												Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships.SubsetMembership _xmlSubsetMembership = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships.SubsetMembership();
								            	_xmlSubsetMembership.setAction((ActionType) subMems.get("Action"));
								            	_xmlSubsetMembership.setVUID((Long) subMems.get("VUID"));
								            	_xmlSubsetMembership.setActive((Boolean) subMems.get("Active"));
								            	//System.out.println("Action=" + action + ", VUID=" + vuid + ", Active=" + active);
								    			_xmlSubsetMemberships.getSubsetMembership().add(_xmlSubsetMembership);
											}
										}
									}
								});
								
								if (_xmlDesignationProperties.getProperty().size() > 0) {
									_xmlDesignation.setProperties(_xmlDesignationProperties);
								}
								
								if (_xmlSubsetMemberships.getSubsetMembership().size() > 0) {
									_xmlDesignation.setSubsetMemberships(_xmlSubsetMemberships);
								}
								//System.out.println("---");
							}
							
							_xmlDesignations.getDesignation().add(_xmlDesignation);
						}
						
						// TODO: Properties
						if (_sememe.getSememeType() == SememeType.DYNAMIC && ts.wasEverKindOf(_sememe.getAssemblageSequence(), vhatPropertyTypesNid)) {
							Map<String, Object> prop = getProperties(_sememe, startDate, endDate);
							
							if (!prop.isEmpty()) {
								Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties.Property _xmlProperty = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties.Property();
								_xmlProperty.setAction((ActionType) prop.get("Action"));
								_xmlProperty.setTypeName((String) prop.get("TypeName"));
								_xmlProperty.setValueNew((String) prop.get("ValueNew"));
								_xmlProperty.setActive((Boolean) prop.get("Active"));
								_xmlProperties.getProperty().add(_xmlProperty);
							}
						}
					});
					
					// Relationships
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships _xmlRelationships = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships();
					List<Map<String, Object>> rels = getRelationships(_concept, startDate, endDate);
					for (Map<String, Object> rel : rels) {
						Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship _xmlRelationship = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship();
						_xmlRelationship.setAction((ActionType) rel.get("Action"));
						_xmlRelationship.setTypeName((String) rel.get("TypeName"));
						_xmlRelationship.setNewTargetCode((String) rel.get("NewTargetCode"));
						_xmlRelationship.setActive((Boolean) rel.get("Active"));
						_xmlRelationships.getRelationship().add(_xmlRelationship);
					}
					
					// Try to keep XML output somewhat clean, without empty elements (i.e. <Element/> or <Element></Element> 
					if (_xmlDesignations.getDesignation().size() > 0) {
						_xmlCodedConcept.setDesignations(_xmlDesignations);
					}
					
					if (_xmlProperties.getProperty().size() > 0) {
						_xmlCodedConcept.setProperties(_xmlProperties);
					}
					
					if (_xmlRelationships.getRelationship().size() > 0) {
						_xmlCodedConcept.setRelationships(_xmlRelationships);
					}

					// Add all CodedConcept elements
					_xmlCodedConcepts.getCodedConcept().add(_xmlCodedConcept);
				
					// TODO: MapSets
				}
			}
		});
		
		// MapSets
		// TODO: This should probably be moved up into the other stream processing
		// TODO: Need to add this XML set to the full XML tree
		Get.conceptService().getConceptChronologyStream().forEach((_concept) -> {
			UUID mappingUUID = UUID.fromString("3a0d3b6f-da93-5e07-8783-678b4deb382b"); //MapSet->Properties
			int mappingNid = Get.identifierService().getNidForUuids(mappingUUID);
			int mappingSeq = Get.conceptService().getConcept(mappingNid).getConceptSequence();
			
			if (Frills.definesMapping(_concept.getConceptSequence())) {
				//System.out.println(_concept.getConceptDescriptionText());
				
				Terminology.CodeSystem.Version.MapSets.MapSet _xmlMapSet = new Terminology.CodeSystem.Version.MapSets.MapSet();
				_xmlMapSet.setAction(determineAction((ObjectChronology<? extends StampedVersion>) _concept, startDate, endDate));
				_xmlMapSet.setActive(_concept.isLatestVersionActive(STAMP_COORDINATES));
				_xmlMapSet.setCode(getCodeFromNid(_concept.getNid()));
				_xmlMapSet.setName(_concept.getConceptDescriptionText()); // TODO: ??
				long vuid = Frills.getVuId(_concept.getNid(), STAMP_COORDINATES).orElse(0L);
				_xmlMapSet.setVUID(vuid);
				
				// Designations
				Terminology.CodeSystem.Version.MapSets.MapSet.Designations _xmlMapSetDesignations = new Terminology.CodeSystem.Version.MapSets.MapSet.Designations();
				Get.sememeService().getSememesForComponent(_concept.getNid()).forEach((sememe) -> {
					Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation _xmlMapSetDesignation = new Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation();
					Map<String, Object> msDescMap = getDesignations(sememe, startDate, endDate);
					if (msDescMap != null) { // TODO: This is a hack for now
						_xmlMapSetDesignation = new Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation();
						_xmlMapSetDesignation.setAction((ActionType) msDescMap.get("Action"));
						_xmlMapSetDesignation.setCode((String) msDescMap.get("Code"));
						_xmlMapSetDesignation.setTypeName((String) msDescMap.get("TypeName"));
						_xmlMapSetDesignation.setValueNew((String) msDescMap.get("ValueNew"));
						_xmlMapSetDesignation.setVUID((Long) msDescMap.get("VUID"));
						_xmlMapSetDesignation.setActive((Boolean) msDescMap.get("Active"));
						_xmlMapSetDesignations.getDesignation().add(_xmlMapSetDesignation);
						//System.out.println("D - Action = " + msDescMap.get("Action") + ", Code = " + msDescMap.get("Code") + ", TypeName = " + msDescMap.get("TypeName") + ", ValueNew = " + msDescMap.get("ValueNew") + ", VUID = " + (Long) msDescMap.get("VUID") + ", Active = " + msDescMap.get("Active"));
					}
				});	
				
				int dsedNid = -2147483452; // SourceCodeSystem / SourceVersionName / TargetCodeSystem / TargetVersionName
				int dsedSeq = Get.conceptService().getConcept(dsedNid).getConceptSequence();
				Get.sememeService().getSememesForComponentFromAssemblage(_concept.getNid(), dsedSeq).forEach((sememe) -> {
					Optional<LatestVersion<? extends DynamicSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
					//System.out.println(sememeVersion.get().value());
					DynamicSememeData dsd[] = sememeVersion.get().value().getData();
                	if (dsd[0].getDataObject().equals(-2147483446)) {
                		//System.out.println("SourceCodeSystem = " + dsd[1].getDataObject());
                		_xmlMapSet.setSourceCodeSystem(dsd[1].getDataObject().toString());
                	} else if (dsd[0].getDataObject().equals(-2147483445)) {
                		//System.out.println("SourceVersionName = " + dsd[1].getDataObject());
                		_xmlMapSet.setSourceVersionName(dsd[1].getDataObject().toString());
                	} else if (dsd[0].getDataObject().equals(-2147483444)) {
                		//System.out.println("TargetCodeSystem = " + dsd[1].getDataObject());
                		_xmlMapSet.setTargetCodeSystem(dsd[1].getDataObject().toString());
                	} else if (dsd[0].getDataObject().equals(-2147483443)) {
                		//System.out.println("TargetVersionName = " + dsd[1].getDataObject());
                		_xmlMapSet.setTargetVersionName(dsd[1].getDataObject().toString());
                	}
				});
				
				// Properties
				Terminology.CodeSystem.Version.MapSets.MapSet.Properties _xmlMapSetProperties = new Terminology.CodeSystem.Version.MapSets.MapSet.Properties();
				_concept.getSememeListFromAssemblage(mappingSeq).forEach((s) -> {
					//System.out.println("---> "+s.toUserString());
					// TODO: MapSet Data
					// TODO: Designations
					// Properties - only 1 per MapSet?
					Map<String, Object> prop = getProperties(s, startDate, endDate);
					if (!prop.isEmpty()) {
						Terminology.CodeSystem.Version.MapSets.MapSet.Properties.Property _xmlMapSetProperty = new Terminology.CodeSystem.Version.MapSets.MapSet.Properties.Property();
						_xmlMapSetProperty.setAction((ActionType) prop.get("Action"));
						_xmlMapSetProperty.setTypeName((String) prop.get("TypeName"));
						_xmlMapSetProperty.setValueNew((String) prop.get("ValueNew"));
						_xmlMapSetProperty.setActive((Boolean) prop.get("Active"));
						_xmlMapSetProperties.getProperty().add(_xmlMapSetProperty);
						//System.out.println("P - Action = " + prop.get("Action") + ", TypeName = " + prop.get("TypeName") + ", ValueNew = " + prop.get("ValueNew") + ", Active = " + prop.get("Active"));
					}
					
					
				});
				// TODO: Relationships
				/* Disabled - there doesn't appear to be any MapSet->Relationships in the source TerminologyData.xml file, confirmed in VHAT importer code/comments
				Terminology.CodeSystem.Version.MapSets.MapSet.Relationships _xmlMapSetRelationships = new Terminology.CodeSystem.Version.MapSets.MapSet.Relationships();
				List<Map<String, Object>> rels = getRelationships(r, startDate, endDate);
				for (Map<String, Object> rel : rels) {
					Terminology.CodeSystem.Version.MapSets.MapSet.Relationships.Relationship _xmlMapSetRelationship = new Terminology.CodeSystem.Version.MapSets.MapSet.Relationships.Relationship();
					_xmlMapSetRelationship.setAction((ActionType) rel.get("Action"));
					_xmlMapSetRelationship.setTypeName((String) rel.get("TypeName"));
					_xmlMapSetRelationship.setNewTargetCode((String) rel.get("NewTargetCode"));
					_xmlMapSetRelationship.setActive((Boolean) rel.get("Active"));
					_xmlMapSetRelationships.getRelationship().add(_xmlMapSetRelationship);
					System.out.println("Action = " + rel.get("Action") + ", TypeName = " + rel.get("TypeName") + ", ValueNew = " + rel.get("NewTargetCode") + ", Active = " + rel.get("Active"));
				}*/
				// TODO: MapEntries
				// 		TODO: MapEntry->Designations
				// 		TODO: MapEntry->Properties
				// 		TODO: MapEntry->Relationships
				
				_xmlMapSet.setDesignations(_xmlMapSetDesignations);
				//_xmlMapSet.setMapEntries(value);
				_xmlMapSet.setProperties(_xmlMapSetProperties);
				//_xmlMapSet.setRelationships(_xmlMapSetRelationships);
				
				_xmlMapSetCollection.add(_xmlMapSet);
			}
		});
		//--- /MapSets
				
		// Close out XML
		_xmlVersion.setCodedConcepts(_xmlCodedConcepts);
		
		// MapSets
		//Terminology.CodeSystem.Version.MapSets _xmlMapSets = new Terminology.CodeSystem.Version.MapSets();
		//_xmlMapSets.getMapSet().addAll(_xmlMapSetCollection);
		//_xmlVersion.setMapSets(_xmlMapSets);
	
		_xmlCodeSystem.setVersion(_xmlVersion);
		terminology.setCodeSystem(_xmlCodeSystem); 
		
		log.info("Skipped " + skippedForNonVHAT.get() + " concepts for non-vhat");
		log.info("Skipped " + skippedDateRange.get() + " concepts for outside date range");
		log.info("Processed " + observedVhatConcepts.get() + " concepts");
		log.info("Exported " + exportedVhatConcepts.get() + " concepts");
		
		writeXml(writeTo);
		
		/*for (UUID key : assemblagesMap.keySet()) {
			System.out.println(key + " -> " + assemblagesMap.get(key));
		}*/
	}
	
	/**
	 * 
	 * @param sememe
	 * @param startDate
	 * @param endDate
	 * @return Map of property Objects
	 */
	private Map<String, Object> getProperties(SememeChronology<?> sememe, long startDate, long endDate) {
		
		if (sememe == null) {
			return new HashMap<>();
		}
		
		Map<String, Object> tmpMap = new HashMap<>();
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<LatestVersion<? extends DynamicSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
		
		if (sememeVersion.isPresent()) {
			// TODO: this same date logic here?
			@SuppressWarnings({ "unchecked" })
			ActionType action = determineAction((ObjectChronology<? extends StampedVersion>) sememeVersion.get().value().getChronology(), startDate, endDate);
			tmpMap.put("Action", action);
			
			DynamicSememeUsageDescription dsud = DynamicSememeUsageDescriptionImpl.read(sememeVersion.get().value().getAssemblageSequence());
			String typeName = dsud.getDynamicSememeUsageDescription();
			tmpMap.put("TypeName", typeName);
			
			@SuppressWarnings("rawtypes")
            DynamicSememe ds = sememeVersion.get().value();
            DynamicSememeData[] dsd = ds.getData();
            String valueNew = "";
            if (dsd.length > 0) {
            	valueNew = dsd[0].getDataObject().toString();
            }
			tmpMap.put("ValueNew", valueNew);

			Boolean active = sememeVersion.get().value().getChronology().isLatestVersionActive(STAMP_COORDINATES);
			tmpMap.put("Active", active);
		}
		
		return tmpMap;
	}
	
	/**
	 * 
	 * @param sememe
	 * @param startDate
	 * @param endDate
	 * @return Map of Objects representing the Designation elements
	 */
	private Map<String, Object> getDesignations(SememeChronology<?> sememe, long startDate, long endDate) {
		
		Map<String, Object> tmpMap = new HashMap<>();
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<LatestVersion<SememeVersion>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(SememeVersion.class, STAMP_COORDINATES);
		if (sememeVersion.isPresent()) {
			String valueNew = "";
			String typeName = "";
			
			if (sememeVersion.get().value().getChronology().getSememeType() == SememeType.DESCRIPTION) {
				SememeChronology<?> sc = sememeVersion.get().value().getChronology();
                @SuppressWarnings({ "rawtypes", "unchecked" })
                Optional<LatestVersion<? extends DescriptionSememe>> sv = ((SememeChronology) sc).getLatestVersion(DescriptionSememe.class, STAMP_COORDINATES);
                if (sv.isPresent()) {
                    String ds = sv.get().value().getText();
                    valueNew = ds;
                }
            }
			
			// TODO: This is horrendous and potentially very flakey
			for ( SememeChronology<?> sc : ((SememeChronology<?>) sememeVersion.get().value().getChronology()).getSememeList()) {
				if (sc.getSememeType() == SememeType.DYNAMIC) {
	                @SuppressWarnings({ "rawtypes", "unchecked" })
	                Optional<LatestVersion<? extends DynamicSememe>> sv = ((SememeChronology) sc).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
	                if (sv.isPresent()) {
	                    @SuppressWarnings("rawtypes")
	                    DynamicSememe ds = sv.get().value();
	                    DynamicSememeData[] dsd = ds.getData();
	                    if (dsd.length > 0 && dsd[0].getDynamicSememeDataType().getDisplayName().equals("UUID")) {
	                    	typeName = designationTypes.get(dsd[0].getDataObject());
	                    	break;
	                    }
	                }					        		
				}
			}
			
			@SuppressWarnings("unchecked")
			ActionType action = determineAction((ObjectChronology<? extends StampedVersion>) sememeVersion.get().value().getChronology(), startDate, endDate);
			tmpMap.put("Action", action);
			
			String code = getCodeFromNid(sememeVersion.get().value().getNid());
			tmpMap.put("Code", code);
			
			long vuid = Frills.getVuId(sememeVersion.get().value().getNid(), null).orElse(0L);
			tmpMap.put("VUID", vuid);
			
			tmpMap.put("TypeName", typeName);
			tmpMap.put("ValueNew", valueNew);
			
			boolean active = sememeVersion.get().value().getChronology().isLatestVersionActive(STAMP_COORDINATES);
			tmpMap.put("Active", active);
			
			// TODO: This isn't the right way to handle this, I'm sure
			if (vuid == 0L) {
				return null;
			}
		}
		
		return tmpMap;
	}
	
	/**
	 * 
	 * @param sememe
	 * @param startDate
	 * @param endDate
	 * @return Map of Objects representing the SubsetMemberships elements
	 */
	private Map<String, Object> getSubsetMemberships(SememeChronology<?> sememe, long startDate, long endDate) {
		
		Map<String, Object> tmpMap = new HashMap<>();
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<LatestVersion<? extends DynamicSememe>> sememeVersion = ((SememeChronology) sememe).getLatestVersion(DynamicSememe.class, STAMP_COORDINATES);
		if (sememeVersion.isPresent()) {
			@SuppressWarnings("rawtypes")
            DynamicSememe ds = sememeVersion.get().value();
            DynamicSememeData[] dsd = ds.getData();
            // This implies a Subset - sememe with 0 columns?
            if (dsd.length == 0) {
            	DynamicSememeUsageDescription dsud = DynamicSememeUsageDescriptionImpl.read(sememeVersion.get().value().getAssemblageSequence());
				String name = dsud.getDynamicSememeName();
				
				@SuppressWarnings("unchecked")
				SememeChronology<? extends SememeVersion<?>> sc = sememeVersion.get().value().getChronology();
				
            	ActionType action = determineAction((ObjectChronology<? extends StampedVersion>) sc, startDate, endDate);
            	tmpMap.put("Action", action);
            	
             	long vuid = 0L;
             	if (subsetMap.containsKey(name)) {
             		vuid = subsetMap.get(name);
             	}
             	tmpMap.put("VUID", vuid);
    			
             	Boolean active = sc.isLatestVersionActive(STAMP_COORDINATES);
            	tmpMap.put("Active", active);
            	
            }
		}
		
		return tmpMap;
	}
	
	/**
	 * 
	 * @param concept
	 * @param startDate
	 * @param endDate
	 * @return List of Maps of relationship Objects
	 */
	private List<Map<String, Object>> getRelationships(ConceptChronology<?> concept, long startDate, long endDate) {
		
		List<Map<String, Object>> tmpList = new ArrayList<>();
		
		List<AssociationInstance> aiList = AssociationUtilities.getSourceAssociations(concept.getNid(), STAMP_COORDINATES);
		
		for (AssociationInstance ai : aiList) {
			Map<String, Object> tmpMap = new HashMap<>();
			
			ActionType action = determineAction((ObjectChronology<? extends StampedVersion>) concept, startDate, endDate);
			tmpMap.put("Action", action);
			
			String typeName = ai.getAssociationType().getAssociationName();
			tmpMap.put("TypeName", typeName);
			
			String targetUUID = ai.getTargetComponentData().get().getDataObject().toString();
			ConceptChronology<?> targetConcept = Get.conceptService().getConcept(UUID.fromString(targetUUID));
			String newTargetCode = getCodeFromNid(targetConcept.getNid());
			tmpMap.put("NewTargetCode", newTargetCode);
			
			// TODO: This doesn't appear to be used in the input TerminologyData.xml file, but might be needed for TDS changes ??
			String oldTargetCode = "";
			tmpMap.put("OldTargetCode", oldTargetCode);
			
			// TODO: Is this current or target concept active?
			boolean active = targetConcept.isLatestVersionActive(STAMP_COORDINATES);
			tmpMap.put("Active", active);
			
			tmpList.add(tmpMap);
		}
		
		return tmpList;
	}
	
	/**
	 * @param _concept
	 * @return
	 */
	private ActionType determineAction(ObjectChronology<? extends StampedVersion> object, long startDate, long endDate)
	{
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
			if (sv.getTime() <= endDate || sv.getTime() >= startDate)
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
		
		if (actionCountPriorToStartDate > 0)
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
	private boolean wasModifiedInDateRange(ConceptChronology concept, long startDate, long endDate)
	{
		@SuppressWarnings("unchecked")
		Optional<LatestVersion<ConceptVersion>> cv = concept.getLatestVersion(ConceptVersion.class, STAMP_COORDINATES);
		if (cv.isPresent())
		{
			if (cv.get().value().getTime() >= startDate && cv.get().value().getTime() <= endDate)
			{
				return true;
			}
		}
		
		return hasSememeModifiedInDateRange(concept.getNid(), startDate, endDate);
	}


	/**
	 * @param nid
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	private boolean hasSememeModifiedInDateRange(int nid, long startDate, long endDate)
	{
		//Check all the nested sememes
		return Get.sememeService().getSememesForComponent(nid).anyMatch(sc -> 
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<SememeVersion>> sv = ((SememeChronology)sc).getLatestVersion(SememeVersion.class, STAMP_COORDINATES);
			if (sv.isPresent())
			{
				if (sv.get().value().getTime() > startDate && sv.get().value().getTime() < endDate)
				{
					return true;
				}
			}
			//recurse
			if (hasSememeModifiedInDateRange(sc.getNid(), startDate, endDate))
			{
				return true;
			}
			return false;
		});
	}


	private String getCodeFromNid(int conceptNid) {
		
		// Code Assemblage
		int _codeAssemblageConceptSeq = Get.identifierService().getConceptSequenceForUuids(getFromMapByValue(assemblagesMap, "Code"));
		Optional<SememeChronology<? extends SememeVersion<?>>> sc = Get.sememeService().getSememesForComponentFromAssemblage(conceptNid, _codeAssemblageConceptSeq).findFirst();
		if (sc.isPresent())
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<StringSememe<?>>> sv = ((SememeChronology)sc.get()).getLatestVersion(StringSememe.class, STAMP_COORDINATES);
			if (sv.isPresent())
			{
				return sv.get().value().getString();
			}
		}
		return null;
	}

	private void writeXml(OutputStream writeTo) {
		
		try {
			
			JAXBContext jaxbContext = JAXBContext.newInstance(Terminology.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(terminology, writeTo);
			
		} catch (Exception e) {
			log.error("Unexpected", e);
			throw new RuntimeException(e);
		}
	}
}
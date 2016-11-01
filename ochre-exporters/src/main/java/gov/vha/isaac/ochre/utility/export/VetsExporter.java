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
import gov.va.med.term.vhat.xml.model.PropertyType;
import gov.va.med.term.vhat.xml.model.Terminology;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties;
import gov.vha.isaac.MetaData;
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
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.associations.AssociationInstance;
import gov.vha.isaac.ochre.associations.AssociationUtilities;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.impl.utility.SimpleDisplayConcept;
import gov.vha.isaac.ochre.model.configuration.LanguageCoordinates;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;
import gov.vha.isaac.ochre.model.configuration.TaxonomyCoordinates;
import gov.vha.isaac.ochre.model.sememe.DynamicSememeUsageDescriptionImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeDataImpl;


public class VetsExporter {

	private Logger log = LogManager.getLogger();
	
	private Map<UUID, String> designationTypes = new HashMap<>();
	private Map<UUID, String> propertyTypes = new HashMap<>();
	private Map<UUID, String> relationshipTypes = new HashMap<>();
	private Map<String, List<String>> subsets = new TreeMap<>();
	
	private Map<UUID, String> assemblagesMap = new HashMap<>();
	
	private Terminology terminology;
	
	
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
			if (hstk.equals(nddl)) { //  || hstk.contains(nddl)
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
			terminology.getTypes().getType().add(_xmlType);
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
			terminology.getTypes().getType().add(_xmlType);
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
			terminology.getTypes().getType().add(_xmlType);
		}
		
		// ISAAC VHAT Refsets => Subsets UUID
		UUID vhatRefsetsUUID = UUID.fromString("99173138-dcaa-5a77-a4eb-311b01991b88");
		//int vhatRefsetsNid = Get.identifierService().getNidForUuids(vhatRefsetsUUID); //-2147481162
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
				// VUID UUID = f31f3d89-5a6f-5e1f-81d3-5b68344d96f9
				Get.sememeService().getSememesForComponent(concept.getNid()).forEach((sememe) -> {
					if (sememe.getSememeType() == SememeType.STRING) {
						@SuppressWarnings({ "rawtypes", "unchecked" })
						Optional<LatestVersion<? extends StringSememe>> sememeVersion
								= ((SememeChronology) sememe).getLatestVersion(StringSememe.class, StampCoordinates.getDevelopmentLatestActiveOnly());
						
						if (sememeVersion.isPresent()) {
							List<String> _subsetList = new ArrayList<>();
							_subsetList.add("add"); // Action
							String subsetName = concept.getConceptDescriptionText();
							_subsetList.add(sememeVersion.get().value().getString()); // VUID
							// I'm assuming this will always be 'true' or 'false' - never empty or another identifier
							String active = Boolean.toString(sememe.isLatestVersionActive(StampCoordinates.getDevelopmentLatestActiveOnly()));
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
			if (al.equals("add")) { 
				_xmlSubset.setAction(ActionType.ADD); 
			}
			_xmlSubset.setName(name);
			_xmlSubset.setVUID(Long.valueOf(al.get(1)));
			_xmlSubset.setActive(Boolean.parseBoolean(al.get(2)));
			terminology.getSubsets().getSubset().add(_xmlSubset);
		}
		
		/*
	<CodeSystem>
	<Action>none</Action>
	<Name>VHAT</Name>
	<VUID>4707199</VUID>
	<Description>VHA Terminology</Description>
   ? <Copyright>2007</Copyright>
   ? <CopyrightURL />
	<PreferredDesignationType>Preferred Name</PreferredDesignationType>
	<Version>
	  <Append>true</Append>
	  <Name>Authoring Version</Name>
	  <Description>This is the version that is given to authoring changes before they are finalized.</Description>
	? <EffectiveDate>2011-02-16</EffectiveDate>
	? <ReleaseDate>2011-02-16</ReleaseDate>
	  <Source />
	  <CodedConcepts>
		<CodedConcept>
		  <Action>add</Action>
		  <Code>4516261</Code>
		  <Name>Enterprise Clinical Terms</Name>
		  <VUID>4516261</VUID>
		  <Active>true</Active>
		  <Designations>
			<Designation>
			  <Action>add</Action>
			  <Code>4775680</Code>
			  <TypeName>Preferred Name</TypeName>
			  <VUID>4775680</VUID>
			  <ValueNew>Enterprise Clinical Terms</ValueNew>
			  <Active>true</Active>
			</Designation>
		  </Designations>
		  <Relationships>
			<Relationship>
			  <Action>add</Action>
			  <TypeName>has_parent</TypeName>
			  <NewTargetCode>4712493</NewTargetCode>
			  <Active>true</Active>
			</Relationship>
		  </Relationships>
		</CodedConcept>
		
		 */
		
		/*for (UUID key : assemblagesMap.keySet()) {
			System.out.println(key + " -> " + assemblagesMap.get(key));
		}*/
		
		
		
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
		_xmlVersion.setName("Authoring Version"); // ? TODO
		_xmlVersion.setDescription("This is the version that is given to authoring changes before they are finalized."); // ? TODO
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String formattedDate = sdf.format(sdf.parse("2011-02-16"));
			XMLGregorianCalendar _xmlEffDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(formattedDate);
			XMLGregorianCalendar _xmlRelDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(formattedDate);
			_xmlVersion.setEffectiveDate(_xmlEffDate);
			_xmlVersion.setReleaseDate(_xmlRelDate);
			//DatatypeFactory.newInstance().newXMLGregorianCalendarDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH), DatatypeConstants.FIELD_UNDEFINED);
		} catch (DatatypeConfigurationException dtce) {
			// TODO: Just leave empty elements if there is a parsing error?
		} catch (ParseException pe) {
			// TODO:
		}
		
		_xmlVersion.setSource("");
		
		/*
		UUID _edaUUID = getFromMapByValue(assemblagesMap, "English description assemblage (ISAAC)");
		UUID _edtUUID = getFromMapByValue(assemblagesMap, "extended description type (ISAAC)");
		UUID _vuidUUID = getFromMapByValue(assemblagesMap, "VUID (ISAAC)");
		UUID _codeUUID = getFromMapByValue(assemblagesMap, "Code");
		*/
		
		// VHAT Module
		// CodeSystems : Standard Code Systems  
		UUID vhatStandardCodeSystemsUUID = UUID.fromString("fa27aa69-ba88-556d-88ba-77b9be26db60");
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
					String _name = Frills.getDescription(_conceptNid).orElse("");
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
					boolean _active = _concept.isLatestVersionActive(StampCoordinates.getDevelopmentLatest());
					
					//System.out.println("*** " + _name + " -- " + _vuid + " -- " + _code + " -- " + _active);
					
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept _xmlCodedConcept = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept();
					//TODO use this action logic elsewhere.
					_xmlCodedConcept.setAction(determineAction((ObjectChronology<? extends StampedVersion>)_concept, startDate, endDate));
					_xmlCodedConcept.setName(_name);
					_xmlCodedConcept.setVUID(_vuid);
					_xmlCodedConcept.setCode(_code);
					if (_active) {
						_xmlCodedConcept.setActive(Boolean.TRUE);
					} else {
						_xmlCodedConcept.setActive(Boolean.FALSE);
					}
					
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations _xmlDesignations = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations();
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties _xmlProperties = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties();
					
					Get.sememeService().getSememesForComponent(_conceptNid).forEach((a) -> {
						if (a.getSememeType() == SememeType.DESCRIPTION) {
							// TODO: Designations
							/*
							  <Designations>
								<Designation>
								  <Action>add</Action>
								  <Code>4775680</Code>
								  <TypeName>Preferred Name</TypeName>
								  <VUID>4775680</VUID>
								  <ValueNew>Enterprise Clinical Terms</ValueNew>
								  <Active>true</Active>
								</Designation>
							  </Designations>
				*/
							@SuppressWarnings({ "unchecked", "rawtypes" })
							Optional<LatestVersion<SememeVersion>> __sv = ((SememeChronology) a).getLatestVersion(SememeVersion.class, StampCoordinates.getDevelopmentLatest());
							if (__sv.isPresent()) {
								int __sememeNid = __sv.get().value().getNid();
								long __vuid = Frills.getVuId(__sememeNid, null).orElse(0L);
								if (__vuid == 0)
								{
									log.warn("Missing VUID for concept " + __sememeNid);
								}
								
								Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation _xmlDesignation = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation();
								// TODO: this same date logic here?
								//@SuppressWarnings({ "unchecked" })
								_xmlDesignation.setAction(determineAction((ObjectChronology<? extends StampedVersion>) __sv.get().value().getChronology(), startDate, endDate));
								_xmlDesignation.setCode(getCodeFromNid(__sememeNid));
								_xmlDesignation.setTypeName(""); // TODO
								_xmlDesignation.setVUID(__vuid);
								_xmlDesignation.setActive(__sv.get().value().getChronology().isLatestVersionActive(StampCoordinates.getDevelopmentLatest()));
								
								
								Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties __xmlProperties = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties();
								List<SememeChronology<? extends SememeVersion<?>>> __scList = __sv.get().value().getChronology().getSememeList();
								for (SememeChronology<? extends SememeVersion<?>> ___sc : __scList) {
									if (ts.wasEverKindOf(___sc.getAssemblageSequence(), vhatPropertyTypesNid)) {
										PropertyType __xmlProperty = new PropertyType();
										// TODO: this same date logic here?
										//@SuppressWarnings({ "unchecked" })
										Optional<String> __typeName =  Optional.of("ToDo"); // TODO
										__xmlProperty.setAction(determineAction((ObjectChronology<? extends StampedVersion>) __sv.get().value().getChronology(), startDate, endDate));
										__xmlProperty.setTypeName(__typeName.orElse(""));
										__xmlProperty.setValueNew(_name); // TODO: Propery CodedConcept <Name> value?
										__xmlProperty.setActive(__sv.get().value().getChronology().isLatestVersionActive(StampCoordinates.getDevelopmentLatest()));
										
										__xmlProperties.getProperty().add(__xmlProperty);
									}
								}
								
								
								if (__xmlProperties.getProperty().size() > 0) {
									_xmlDesignation.setProperties(__xmlProperties);
								}
								
								_xmlDesignations.getDesignation().add(_xmlDesignation);
							}
						} else if (a.getSememeType() == SememeType.DYNAMIC && ts.wasEverKindOf(a.getAssemblageSequence(), vhatPropertyTypesNid)) {
							// TODO: Properties
							/*
									  <Properties>
							            <Property>
							              <Action>add</Action>
							              <TypeName>Allergy_Type</TypeName>
							              <ValueNew>DRUG</ValueNew>
							              <Active>true</Active>
							            </Property>
							          </Properties>
							 */
							@SuppressWarnings({ "unchecked", "rawtypes" })
							Optional<LatestVersion<SememeVersion>> __sv = ((SememeChronology) a).getLatestVersion(SememeVersion.class, StampCoordinates.getDevelopmentLatest());
							if (__sv.isPresent()) {
								Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties.Property _xmlProperty = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties.Property();
								// TODO: this same date logic here?
								//@SuppressWarnings({ "unchecked" })
								DynamicSememeUsageDescription __dsud = DynamicSememeUsageDescriptionImpl.read(__sv.get().value().getAssemblageSequence());
								_xmlProperty.setAction(determineAction((ObjectChronology<? extends StampedVersion>) __sv.get().value().getChronology(), startDate, endDate));
								_xmlProperty.setTypeName(__dsud.getDynamicSememeUsageDescription());
								_xmlProperty.setValueNew(_name); // TODO: Propery CodedConcept <Name> value?
								_xmlProperty.setActive(__sv.get().value().getChronology().isLatestVersionActive(StampCoordinates.getDevelopmentLatest()));
								
								_xmlProperties.getProperty().add(_xmlProperty);
							}
						}
					});
					
					// Relationships
					/*
					 <Relationships>
						<Relationship>
						  <Action>add</Action>
						  <TypeName>has_parent</TypeName>
						  <NewTargetCode>4712493</NewTargetCode>
						  <Active>true</Active>
						</Relationship>
					  </Relationships>
					 */
					
					Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships _xmlRelationships = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships();
					List<AssociationInstance> aiList = AssociationUtilities.getSourceAssociations(_concept.getNid(), StampCoordinates.getDevelopmentLatestActiveOnly());
					for (AssociationInstance ai : aiList) {
						String __typeName = ai.getAssociationType().getAssociationName();
						String __targetUUID = ai.getTargetComponentData().get().getDataObject().toString();
						ConceptChronology<?> __concept = Get.conceptService().getConcept(UUID.fromString(__targetUUID));
						String __newTargetCode = getCodeFromNid(__concept.getNid());
						boolean __active = __concept.isLatestVersionActive(StampCoordinates.getDevelopmentLatestActiveOnly());
						
						//System.out.println("Relationship: add, " + __typeName + ", " + __newTargetCode + ", " + __active);
						
						Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship _xmlRelationship = new Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship();
						
						_xmlRelationship.setAction(ActionType.ADD);
						_xmlRelationship.setTypeName(__typeName);
						_xmlRelationship.setNewTargetCode(__newTargetCode);
						_xmlRelationship.setActive(Boolean.valueOf(__active));
						
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
				}
			}
		});
		
		// Close out XML
		_xmlVersion.setCodedConcepts(_xmlCodedConcepts);
		_xmlCodeSystem.setVersion(_xmlVersion);
		terminology.setCodeSystem(_xmlCodeSystem); 
		
		log.info("Skipped " + skippedForNonVHAT.get() + " concepts for non-vhat");
		log.info("Skipped " + skippedDateRange.get() + " concepts for outside date range");
		log.info("Processed " + observedVhatConcepts.get() + " concepts");
		log.info("Exported " + exportedVhatConcepts.get() + " concepts");
		
		writeXml(writeTo);
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
		Optional<LatestVersion<ConceptVersion>> cv = concept.getLatestVersion(ConceptVersion.class, StampCoordinates.getDevelopmentLatest());
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
			Optional<LatestVersion<SememeVersion>> sv = ((SememeChronology)sc).getLatestVersion(SememeVersion.class, StampCoordinates.getDevelopmentLatest());
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
			Optional<LatestVersion<StringSememe<?>>> sv = ((SememeChronology)sc.get()).getLatestVersion(StringSememe.class, StampCoordinates.getDevelopmentLatest());
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
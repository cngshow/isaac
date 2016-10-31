package gov.vha.isaac.ochre.utility.export;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gov.va.med.term.vhat.xml.model.ActionType;
import gov.va.med.term.vhat.xml.model.KindType;
import gov.va.med.term.vhat.xml.model.Terminology;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;


public class VetsExporter {

	private Logger log = LogManager.getLogger();
	
	private Map<UUID, String> designationTypes = new HashMap<>();
	private Map<UUID, String> propertyTypes = new HashMap<>();
	private Map<UUID, String> relationshipTypes = new HashMap<>();
	private Map<String, List<String>> subsets = new TreeMap<>();
	
	private Map<UUID, String> assemblagesMap = new HashMap<>();
	
	
	public VetsExporter()
	{
		setupExporter();
	}
	
	private void setupExporter() {
		
		// Add all assemblages to Map for later lookup
		Get.sememeService().getAssemblageTypes().forEach((assemblageSeqId) -> {
			/*System.out.println(assemblageSeqId 
					+ " -> " 
					+ Get.conceptSpecification(assemblageSeqId).getConceptDescriptionText()
					+ " -> "
					+ Get.conceptSpecification(assemblageSeqId).getPrimordialUuid()
					);
			*/
			assemblagesMap.put(Get.conceptSpecification(assemblageSeqId).getPrimordialUuid(), 
					Get.conceptSpecification(assemblageSeqId).getConceptDescriptionText());
		});

	}
	
	/*private int getFromMapByValue(Map<UUID, String> haystack, String needle) {
		// Not worried about the order changing - nothing should be added to the map after initial import
		if (haystack == null || needle == null || (haystack.size() == 0) | (needle.length() == 0)) {
			return -1;
		}
		
		List<String> values = new ArrayList<>(haystack.values());
		for (int i = 0; i < values.size(); i++) {
			String hstk = ((String) values.get(i)).toLowerCase();
			String nddl = needle.toLowerCase();
			if (hstk.equals(nddl) || hstk.contains(nddl)) {
				return i;
			}
		} 
		
		return -1;
	}*/
	
	private UUID getFromMapByValue(Map<UUID, String> haystack, String needle) {
		// Not worried about the order changing - nothing should be added to the map after initial import
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

	
	public void export(OutputStream writeTo) {
		// ISAAC Associations => RelationshipType UUID
		UUID vhatAssociationTypesUUID = UUID.fromString("55f56c52-757a-5db8-bf1e-3ed613711386");
		//int vhatAssociationTypesNid = Get.identifierService().getNidForUuids(vhatAssociationTypesUUID); //-2147481336
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatAssociationTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			relationshipTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});

		// ISAAC Attributes => PropertyType UUID
		UUID vhatPropertyTypesUUID = UUID.fromString("eb7696e7-fe40-5985-9b2e-4e3d840a47b7"); 
		//int vhatPropertyTypesNid = Get.identifierService().getNidForUuids(vhatPropertyTypesUUID); //-2147481872 
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatPropertyTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			propertyTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});

		// ISAAC Descriptions => DesignationType UUID
		UUID vhatDesignationTypesUUID = UUID.fromString("09c43aa9-eaed-5217-bc5f-23cacca4df38"); 
		//int vhatDesignationTypesNid = Get.identifierService().getNidForUuids(vhatDesignationTypesUUID); //-2147481914
		// Add to map
		Get.taxonomyService().getAllRelationshipOriginSequences(
				Get.identifierService().getNidForUuids(vhatDesignationTypesUUID)).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			designationTypes.put(concept.getPrimordialUuid(), concept.getConceptDescriptionText());
		});
		
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
	  <EffectiveDate>2011-02-16</EffectiveDate>
	  <ReleaseDate>2011-02-16</ReleaseDate>
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
		String csAction = "none";
		ConceptChronology<? extends ConceptVersion<?>> vhatConcept = Get.conceptService().getConcept(vhatCodeSystemNid);
		String csName = vhatConcept.getConceptDescriptionText();
		//System.out.println(csName);
		//System.out.println(Get.c (vhatConcept.getNid())); //5a2e7786-3e41-11dc-8314-0800200c9a66
		//String csVUID = "";
		//String csPrefDesigType;
		//String csDescription;
		//String csCopyright;
		//String csCopyrightURL;
		
		UUID _edaUUID = getFromMapByValue(assemblagesMap, "English description assemblage (ISAAC)");
		//System.out.println("English description assemblage (ISAAC) -> " + _edaUUID);
		UUID _edtUUID = getFromMapByValue(assemblagesMap, "extended description type (ISAAC)");
		//System.out.println("extended description type (ISAAC) -> " + _edtUUID);
		UUID _vuidUUID = getFromMapByValue(assemblagesMap, "VUID (ISAAC)");
		//System.out.println("VUID (ISAAC) -> " + _vuidUUID);
		UUID _codeUUID = getFromMapByValue(assemblagesMap, "Code");
		//System.out.println("Code -> " + _codeUUID);
		
		// VHAT Module
		Get.sememeService().getSememesForComponent(vhatConcept.getNid()).forEach((sememe) -> {
			switch (sememe.getSememeType()) {
			case STRING:
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Optional<LatestVersion<? extends StringSememe>> sememeString
						= ((SememeChronology) sememe).getLatestVersion(StringSememe.class, StampCoordinates.getDevelopmentLatestActiveOnly());
				
				UUID test1 = Get.identifierService().getUuidPrimordialFromConceptSequence(sememeString.get().value().getAssemblageSequence()).get();
				//System.out.println(" -----> "+sememeString.get().value().getString() + " -> "+ assemblagesMap.get(test1)); // VUID
				break;
			case DESCRIPTION:
				for (SememeChronology<?> sc : sememe.getSememeList()) {
					if (Get.identifierService().getUuidPrimordialFromConceptSequence(sc.getAssemblageSequence()).get().equals(_edtUUID)) {
						UUID test2 = Get.identifierService().getUuidPrimordialFromConceptSequence(sc.getAssemblageSequence()).get();
						//System.out.println(" -----> " + assemblagesMap.get(test2));
					}
				}
				/*@SuppressWarnings({ "rawtypes", "unchecked" })
				Optional<LatestVersion<? extends DescriptionSememe>> sememeDescription
						= ((SememeChronology) sememe).getLatestVersion(DescriptionSememe.class, StampCoordinates.getDevelopmentLatestActiveOnly());
				
				UUID test2 = Get.identifierService().getUuidPrimordialFromConceptSequence(sememeDescription.get().value().getAssemblageSequence()).get();
				System.out.println(test2);
				
				if ( test2.equals(_edaUUID)) { 
					System.out.println(" -----> "+sememeDescription.get().value().getText() + " -> "+ assemblagesMap.get(test2));
				}*/
				break;
			case DYNAMIC:
				break;
			case COMPONENT_NID:
				break;
			case MEMBER:
				break;
			case UNKNOWN:
				break;
			}
		});
		
		
		/*Get.sememeService().getSememesForComponent(vhatConcept.getNid()).forEach((sememe) -> {
			if (sememe.getSememeType() == SememeType.STRING) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				Optional<LatestVersion<? extends StringSememe>> sememeVersion
						= ((SememeChronology) sememe).getLatestVersion(StringSememe.class, StampCoordinates.getDevelopmentLatestActiveOnly());
				
				//if (sememeVersion.isPresent()) {
					UUID test = Get.identifierService().getUuidPrimordialFromConceptSequence(sememeVersion.get().value().getAssemblageSequence()).get();
					System.out.println(" -----> "+sememeVersion.get().value().getString() + " -> "+ assemblagesMap.get(test)); // VUID
					
				//}
			}
		});*/
		
		// CodeSystems
		Get.taxonomyService().getAllRelationshipOriginSequences(vhatCodeSystemNid).forEach((conceptId) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptId);
			Get.sememeService().getSememesForComponent(concept.getNid()).forEach((sememe) -> {
				if (sememe.getSememeType() == SememeType.STRING) {
					@SuppressWarnings({ "rawtypes", "unchecked" })
					Optional<LatestVersion<? extends StringSememe>> sememeVersion
							= ((SememeChronology) sememe).getLatestVersion(StringSememe.class, StampCoordinates.getDevelopmentLatestActiveOnly());
					
					if (sememeVersion.isPresent()) {
						UUID test = Get.identifierService().getUuidPrimordialFromConceptSequence(sememeVersion.get().value().getAssemblageSequence()).get();
						//System.out.println(sememeVersion.get().value().getString()+" -> "+ assemblagesMap.get(test)); // VUID or Code
					}
				}
			});
			// The VHAT submodules
			//System.out.println(concept.getConceptDescriptionText());
			Get.taxonomyService().getAllRelationshipOriginSequences(conceptId).forEach((c) -> {
				// Each sub-submodule (codesystems, VistA, etc.)
				ConceptChronology<? extends ConceptVersion<?>> c2 = Get.conceptService().getConcept(c);
				// Need to skip over/handle the blank one in the GUI/DB "No desc for: -2147304122"?
				//System.out.println("|--- "+c2.getConceptDescriptionText());
			});
			/*for (SememeChronology<? extends DescriptionSememe<?>> a : concept.getConceptDescriptionList()) {
				System.out.println(a.toUserString());
			}*/
		});

		//Get.sememeService().getAssemblageTypes().
		//System.out.println(Get.conceptService().getConceptChronologyStream().count());
		
		//System.out.println(Get.sememeService().getAssemblageTypes().count());
		
		
		buildXml(writeTo);
	}

	private void buildXml(OutputStream writeTo) {
		
		try {
			
			JAXBContext jaxbContext = JAXBContext.newInstance(Terminology.class);
			Terminology terminology = new Terminology();
			
			// Types
			terminology.setTypes(new Terminology.Types());
			Terminology.Types.Type _type;

			for (String s : designationTypes.values()) {
				_type = new Terminology.Types.Type();
				_type.setKind(KindType.DESIGNATION_TYPE);
				_type.setName(s);
				terminology.getTypes().getType().add(_type);
			}
			
			for (String s : propertyTypes.values()) {
				_type = new Terminology.Types.Type();
				_type.setKind(KindType.PROPERTY_TYPE);
				_type.setName(s);
				terminology.getTypes().getType().add(_type);
			}
						
			for (String s : relationshipTypes.values()) {
				_type = new Terminology.Types.Type();
				_type.setKind(KindType.RELATIONSHIP_TYPE);
				_type.setName(s);
				terminology.getTypes().getType().add(_type);
			}
			
			// Subsets/Refsets
			terminology.setSubsets(new Terminology.Subsets());
			Terminology.Subsets.Subset _subset;
			
			for (Map.Entry<String, List<String>> entry : subsets.entrySet()) {
				_subset = new Terminology.Subsets.Subset();
				String name = entry.getKey();
				List<String> al = entry.getValue(); // 0: action, 1: VUID, 2: active, 3: UUID
				if (al.equals("add")) { 
					_subset.setAction(ActionType.ADD); 
				}
				_subset.setName(name);
				_subset.setVUID(Long.valueOf(al.get(1)));
				_subset.setActive(Boolean.parseBoolean(al.get(2)));
				terminology.getSubsets().getSubset().add(_subset);
			}
			
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(terminology, writeTo);
			
		} catch (Exception e) {
			log.error("Unexpected", e);
			throw new RuntimeException(e);
		}
	}
}
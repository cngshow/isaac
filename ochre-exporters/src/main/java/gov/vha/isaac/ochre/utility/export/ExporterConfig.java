package gov.vha.isaac.ochre.utility.export;

import static gov.vha.isaac.ochre.api.constants.Constants.DATA_STORE_ROOT_LOCATION_PROPERTY;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
//import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.collections.SememeSequenceSet;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableLogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableLongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableStringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnUtility;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUtility;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeValidatorType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArray;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;
import gov.vha.isaac.ochre.api.util.ArtifactUtilities;
import gov.vha.isaac.ochre.api.util.DBLocator;
import gov.vha.isaac.ochre.api.util.DownloadUnzipTask;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.impl.utility.SimpleDisplayConcept;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;
import gov.vha.isaac.ochre.model.relationship.RelationshipAdaptorChronicleKeyImpl;
import gov.vha.isaac.ochre.model.relationship.RelationshipVersionAdaptorImpl;
import gov.vha.isaac.ochre.model.sememe.DynamicSememeUsageDescriptionImpl;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;

import gov.va.med.term.vhat.xml.model.ActionType;
import gov.va.med.term.vhat.xml.model.CodeSystemType;
import gov.va.med.term.vhat.xml.model.ConceptType;
import gov.va.med.term.vhat.xml.model.DesignationSubsetType;
import gov.va.med.term.vhat.xml.model.DesignationType;
import gov.va.med.term.vhat.xml.model.KindType;
import gov.va.med.term.vhat.xml.model.MapEntryType;
import gov.va.med.term.vhat.xml.model.MapSetType;
import gov.va.med.term.vhat.xml.model.ObjectFactory;
import gov.va.med.term.vhat.xml.model.PropertyType;
import gov.va.med.term.vhat.xml.model.RelationshipType;
import gov.va.med.term.vhat.xml.model.SubsetType;
import gov.va.med.term.vhat.xml.model.Terminology;
import gov.va.med.term.vhat.xml.model.Type;
import gov.va.med.term.vhat.xml.model.TypeType;
import gov.va.med.term.vhat.xml.model.VersionType;


@SuppressWarnings("restriction")
public class ExporterConfig {

	private Logger log = LogManager.getLogger();
	
	private boolean shutdown = false;
	
	private Map<UUID, String> designationTypes = new HashMap<>();
	private Map<UUID, String> propertyTypes = new HashMap<>();
	private Map<UUID, String> relationshipTypes = new HashMap<>();
	private Map<String, List<String>> subsets = new TreeMap<>();
	
	
	public static void main(String[] args) {

		ExporterConfig ec = new ExporterConfig();
		
		ec.testXml();
		
		javafx.application.Platform.exit();
		
	}
	
	public ExporterConfig()
	{
		issacInit();
	}
	
	public boolean isIsaacReady()
	{
		return LookupService.isIsaacStarted();
	}
	
	private void issacInit()
	{
		log.info("Isaac Init called");
		
		try
		{
			log.info("ISAAC Init thread begins");
			
			if (StringUtils.isBlank(System.getProperty(DATA_STORE_ROOT_LOCATION_PROPERTY)))
			{
				//if there isn't an official system property set, check this one.
				String sysProp = System.getProperty("isaacDatabaseLocation");
				//File temp = new File(sysProp);
				
				if (shutdown)
				{
					return;
				}
				
				File dataStoreLocation = DBLocator.findDBFolder(new File("")); //temp
				
				if (!dataStoreLocation.exists())
				{
					throw new RuntimeException("Couldn't find a data store from the input of '" + dataStoreLocation.getAbsoluteFile().getAbsolutePath() + "'");
				}
				if (!dataStoreLocation.isDirectory())
				{
					throw new RuntimeException("The specified data store: '" + dataStoreLocation.getAbsolutePath() + "' is not a folder");
				}
				
				//use the passed in JVM parameter location
				LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(dataStoreLocation.toPath());
				System.out.println("  Setup AppContext, data store location = " + dataStoreLocation.getAbsolutePath());
			}

			if (shutdown)
			{
				return;
			}
			
			//status_.set("Starting ISAAC");
			LookupService.startupIsaac();
			
			//status_.set("Ready");
			System.out.println("Done setting up ISAAC");

		}
		catch (Exception e)
		{
			log.error("Failure starting ISAAC", e);
		}
			
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

		//System.out.println(Get.conceptService().getConceptChronologyStream().count());
        
        //System.out.println(Get.sememeService().getAssemblageTypes().count());
		
		shutdown = true;
		log.info("Stopping ISAAC");
		LookupService.shutdownIsaac();
		log.info("ISAAC stopped");

	}

	private void testXml() {
		
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
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			//To avoid XXE injections add setFeature
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        Document document = db.newDocument();
	        
	        Marshaller marshaller = jaxbContext.createMarshaller();
	        marshaller.marshal(terminology, document);
	        
	        TransformerFactory tf = TransformerFactory.newInstance();
	        //To protect a TransformerFactory against XXE injection add setAttribute
	        tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
	        Transformer t = tf.newTransformer();
	        DOMSource source = new DOMSource(document);
	        StreamResult result = new StreamResult(System.out);
	        t.transform(source, result);
			
			/* TODO: We want to use this method, after we have time to adjust
			 * for Fortify scans and not create any warnings/failures
			 * 
			Marshaller marshaller = jaxbContext.createMarshaller();
	        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	        marshaller.marshal(terminology, System.out );
	        */
	        
		} catch (Exception e) {
			// TODO
		}
		
	}
}
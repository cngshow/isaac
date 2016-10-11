package gov.vha.isaac.ochre.utility.export;

import static gov.vha.isaac.ochre.api.constants.Constants.DATA_STORE_ROOT_LOCATION_PROPERTY;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
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
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.collections.SememeSequenceSet;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.util.ArtifactUtilities;
import gov.vha.isaac.ochre.api.util.DBLocator;
import gov.vha.isaac.ochre.api.util.DownloadUnzipTask;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.impl.utility.SimpleDisplayConcept;
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
	
	public static void main(String[] args) {

		ExporterConfig ec = new ExporterConfig();
		
		ec.testXml();
		
		javafx.application.Platform.exit();
		
	}
	
	private void testXml() {
		
		try {
			JAXBContext jc = JAXBContext.newInstance(Terminology.class);
			Terminology tx = new Terminology();
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        Document document = db.newDocument();
	        
	        Marshaller marshaller = jc.createMarshaller();
	        marshaller.marshal(tx, document);
	        
	        TransformerFactory tf = TransformerFactory.newInstance();
	        Transformer t = tf.newTransformer();
	        DOMSource source = new DOMSource(document);
	        StreamResult result = new StreamResult(System.out);
	        t.transform(source, result);
		} catch (Exception e) {
			
		}
		
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
			
		// TODO
		java.util.Map<Integer, java.util.UUID> conceptDescriptions = new java.util.HashMap<>();
		
		Get.conceptService().getConceptChronologyStream()
			.forEach((conceptChronology) -> {
				System.out.println(conceptChronology.toUserString()+": ");
				Get.sememeService().getSememesForComponent(conceptChronology.getNid())
					.forEach((sememeChronology) -> {
						System.out.print("     "+sememeChronology.getNid()+" <");
						if (Get.sememeService().hasSememe(sememeChronology.getNid())) {
							System.out.print(sememeChronology.getSememeType()+" ");
							if (sememeChronology.getSememeType() == SememeType.DESCRIPTION) {
								conceptDescriptions.put(sememeChronology.getNid(), sememeChronology.getPrimordialUuid());
							}
							/*Get.sememeService().getDescriptionsForComponent(sememeChronology.getNid())
								.forEach((sememeDescription) -> {
								System.out.print(((DescriptionSememe<?>)sememeDescription).getText());
							});*/
						}
						System.out.println("> ");
				});
				System.out.println();
	        });
		System.out.println("Descriptions: " + conceptDescriptions.size());
		
		/*Get.sememeService().getAssemblageTypes().forEach(assemblageSeq -> {
			System.out.println(assemblageSeq 
								//+ ":" + Get.sememeService().getSememe(assemblageSeq).getNid() 
								+ ":" + Get.sememeService().getSememe(assemblageSeq).toUserString()
								//+ ":" + Get.sememeService().getSememe(assemblageSeq).getPrimordialUuid().toString()
								//+ ":" + Get.sememeService().getSememe(assemblageSeq).getSememeType().toString()
								+ "\n"
								);
		});*/
		
        System.out.println(Get.conceptService().getConceptChronologyStream().count());
        
        // Produces NPE
        /*Get.sememeService().getSememeChronologyStream()
		.forEach((sememeChronology) -> {
			List<? extends SememeChronology<? extends SememeVersion<?>>> _sc = sememeChronology.getSememeList();
			System.out.print(sememeChronology.getSememeType() + ": ");
			if (_sc.size() > 0) {
				for (SememeChronology<? extends SememeVersion<?>>__sc : _sc) {
					System.out.print(__sc.getNid()+" ");
				}
				System.out.println();
			} else {
				System.out.println(sememeChronology.getNid());
			}
		});*/
        
        System.out.println(Get.sememeService().getAssemblageTypes().count());
		
		shutdown = true;
		log.info("Stopping ISAAC");
		LookupService.shutdownIsaac();
		log.info("ISAAC stopped");

	}
	

}

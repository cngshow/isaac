package gov.vha.isaac.ochre.utility.importer;

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.NecessarySet;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;
import gov.va.med.term.vhat.xml.deltaIn.model.ActionType;
import gov.va.med.term.vhat.xml.deltaIn.model.ConceptType;
import gov.va.med.term.vhat.xml.deltaIn.model.DesignationType;
import gov.va.med.term.vhat.xml.deltaIn.model.PropertyType;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships.SubsetMembership;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties.Property;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.MapSets;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.MapSets.MapSet;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.Subsets.Subset;
import gov.va.med.term.vhat.xml.deltaIn.model.Terminology.Types.Type;
import gov.va.oia.terminology.converters.sharedUtils.ComponentReference;
import gov.va.oia.terminology.converters.sharedUtils.ConverterBaseMojo;
import gov.va.oia.terminology.converters.sharedUtils.IBDFCreationUtility;
import gov.va.oia.terminology.converters.sharedUtils.IBDFCreationUtility.DescriptionType;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Annotations;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Associations;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.BPT_Refsets;
import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyAssociation;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.DataTarget;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.commit.Alert;
import gov.vha.isaac.ochre.api.commit.CommitTask;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableLogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableStringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeValidatorType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.LogicCoordinate;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.assertions.Assertion;
import gov.vha.isaac.ochre.api.logic.assertions.ConceptAssertion;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.configuration.LanguageCoordinates;
import gov.vha.isaac.ochre.model.configuration.LogicCoordinates;
import gov.vha.isaac.ochre.model.coordinate.EditCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;
import gov.vha.isaac.ochre.model.coordinate.TaxonomyCoordinateImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeArrayImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeIntegerImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeLongImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNidImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUIDImpl;
import gov.vha.isaac.ochre.model.sememe.version.StringSememeImpl;
import gov.vha.isaac.ochre.modules.vhat.VHATConstants;
import gov.vha.isaac.ochre.modules.vhat.VHATIsAHasParentSynchronizingChronologyChangeListenerI;

/**
 * Goal which converts VHAT data into the workbench jbin format
 * 
 */
public class VHATDeltaImport extends ConverterBaseMojo
{
	private IBDFCreationUtility importUtil_;
	private Map<String, UUID> extendedDescriptionTypeNameMap = new HashMap<>();
	
	private gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType associations_ = new BPT_Associations("VHAT");
	private gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType annotations_ = new BPT_Annotations("VHAT");
	private BPT_Refsets subsets_ = new BPT_Refsets("VHAT");
	private Map<Long, UUID> vuidToSubsetMap_ = new HashMap<>();
	private StampCoordinate readCoordinate_;
	private LogicCoordinate logicReadCoordinate_;
	private EditCoordinate editCoordinate_;
	private LongSupplier vuidSupplier_;
	private HashSet<String> conceptsToBeCreated_ = new HashSet<>();
	
	private static final Logger LOG = LogManager.getLogger();
	
	/***
	 * @param xmlData The data to import
	 * @param author The user to attribute the changes to
	 * @param module The module to put the changes on
	 * @param path The path to put the changes on
	 * @param vuidSupplier (optional) a supplier that provides vuids, or null, if no automated vuid assignment is desired
	 * @param debugOutputFolder (optional) a path to write json debug to, if provided.
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	public VHATDeltaImport(String xmlData, UUID author, UUID module, UUID path, LongSupplier vuidSupplier, File debugOutputFolder) throws IOException
	{
		vuidSupplier_ = vuidSupplier;
		try
		{
			LOG.debug("Processing passed in XML data of length " + xmlData.length());
			try
			{
				schemaValidate(xmlData);
			}
			catch (SAXException | IOException e)
			{
				LOG.info("Submitted xml data failed schema validation", e);
				throw new IOException("The provided XML data failed Schema Validation.  Details: " + e.toString());
			}
			
			LOG.debug("Passed in VHAT XML data is schema Valid");
			Terminology terminology;
			
			try
			{
				JAXBContext jaxbContext = JAXBContext.newInstance(Terminology.class);
	
				XMLInputFactory xif = XMLInputFactory.newFactory();
				xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
				xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
				XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(xmlData));
				
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				terminology = (Terminology) jaxbUnmarshaller.unmarshal(xsr);
			}
			catch (JAXBException | XMLStreamException e)
			{
				LOG.error("Unexpected error parsing submitted VETs XML.", e);
				throw new IOException("Unexpected error parsing the xml.  Details: " + e.toString());
			}
			
			LOG.info("VHA XML Parsed");
			
			extendedDescriptionTypeNameMap.put(VHATConstants.VHAT_ABBREVIATION.getPrimaryName().toLowerCase(), VHATConstants.VHAT_ABBREVIATION.getPrimordialUuid());
			extendedDescriptionTypeNameMap.put(VHATConstants.VHAT_FULLY_SPECIFIED_NAME.getPrimaryName().toLowerCase(), VHATConstants.VHAT_FULLY_SPECIFIED_NAME.getPrimordialUuid());
			extendedDescriptionTypeNameMap.put(VHATConstants.VHAT_PREFERRED_NAME.getPrimaryName().toLowerCase(), VHATConstants.VHAT_PREFERRED_NAME.getPrimordialUuid());
			extendedDescriptionTypeNameMap.put(VHATConstants.VHAT_SYNONYM.getPrimaryName().toLowerCase(), VHATConstants.VHAT_SYNONYM.getPrimordialUuid());
			extendedDescriptionTypeNameMap.put(VHATConstants.VHAT_VISTA_NAME.getPrimaryName().toLowerCase(), VHATConstants.VHAT_VISTA_NAME.getPrimordialUuid());
			
			ConverterUUID.configureNamespace(TermAux.VHAT_MODULES.getPrimordialUuid());
			
			try
			{
				ConceptSequenceSet modulesToRead = new ConceptSequenceSet();
				modulesToRead.add(MetaData.ISAAC_MODULE.getConceptSequence());
				modulesToRead.add(MetaData.VHAT_MODULES.getConceptSequence());
				Frills.getAllChildrenOfConcept(MetaData.VHAT_MODULES.getConceptSequence(), true, false).forEach(i -> modulesToRead.add(i));
				
				readCoordinate_ = new StampCoordinateImpl(StampPrecedence.PATH,  
					new StampPositionImpl(Long.MAX_VALUE, TermAux.DEVELOPMENT_PATH.getConceptSequence()),
					modulesToRead, State.ANY_STATE_SET);
				editCoordinate_ = new EditCoordinateImpl(Get.identifierService().getConceptSequenceForUuids(author), 
					Get.identifierService().getConceptSequenceForUuids(module), Get.identifierService().getConceptSequenceForUuids(path));
				
				logicReadCoordinate_ = LogicCoordinates.getStandardElProfile();
			}
			catch (Exception e)
			{
				throw new IOException("Unexpected error setting up", e);
			}
			
			headerCheck(terminology);
			vuidCheck(terminology);
			populateNewProperties(terminology);
			populateNewSubsets(terminology);
			requiredChecks(terminology);
			
			try
			{
				// Disable VHATIsAHasParentSynchronizingChronologyChangeListener listener
				// because the import generates all necessary components
				LookupService.getService(VHATIsAHasParentSynchronizingChronologyChangeListenerI.class).disable();

				importUtil_ = new IBDFCreationUtility(author, module, path, debugOutputFolder);
				LOG.info("Import Util configured");
				createNewProperties(terminology);
				createNewSubsets(terminology);
				
				LOG.info("Processing changes");
				loadConcepts(terminology.getCodeSystem().getVersion().getCodedConcepts());
				loadMapSets(terminology.getCodeSystem().getVersion().getMapSets());
				
				
				LOG.info("Committing Changes");
				CommitTask ct = Get.commitService().commit("VHAT Delta file");
				
				if (ct.get().isPresent())
				{
					LOG.info("Load complete!");
				}
				else
				{
					LOG.error("commit failed to process!");
					for (Alert a : ct.getAlerts())
					{
						LOG.error(a.getAlertType().name() + ": " + a.getAlertText());
					}
					throw new RuntimeException("Unexpected internal error!");
				}
			}
			catch (RuntimeException e)
			{
				Get.commitService().cancel(editCoordinate_);
				throw e;
			}
			catch (Exception e)
			{
				LOG.warn("Unexpected error setting up", e);
				throw new IOException("Unexpected error setting up", e);
			} finally {
				LookupService.getService(VHATIsAHasParentSynchronizingChronologyChangeListenerI.class).enable();
			}
		}
		catch (RuntimeException | IOException e)
		{
			LOG.info("Input XML not being processed because: ", e);
			throw e;
		}
		catch (Throwable e)
		{
			LOG.warn("Inpux XML processing failure",  e);
			throw e;
		}
		finally
		{
			ConverterUUID.clearCache();
		}
	}

	private void schemaValidate(String xmlData) throws SAXException, IOException
	{
		LOG.info("Doing schema validation");
		
		URL url = VHATDeltaImport.class.getResource("/TerminologyData.xsd.hidden");
		if (url == null)
		{
			throw new RuntimeException("Unable to locate the schema file!");
		}
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);	  
		Schema schema = factory.newSchema(url);	  
		Validator validator = schema.newValidator();	  
		validator.validate(new StreamSource(new StringReader(xmlData)));
	}
	
	private void vuidCheck(Terminology terminology)
	{
		LOG.info("Checking for in use VUIDs");
		HashSet<Long> vuidsInXmlFile_ = new HashSet<>();
		
		if (terminology.getCodeSystem().getAction() == ActionType.ADD && terminology.getCodeSystem().getVUID() != null)
		{
			if (Frills.getNidForVUID(terminology.getCodeSystem().getVUID()).isPresent())
			{
				throw new RuntimeException("The VUID specified for the new code system '" + terminology.getCodeSystem().getName() + "' : '" 
						+ terminology.getCodeSystem().getVUID() + "' is already in use");
			}
			else if (!vuidsInXmlFile_.add(terminology.getCodeSystem().getVUID()))
			{
				throw new RuntimeException("The VUID specified for the new code system '" + terminology.getCodeSystem().getName() + "' : '" 
						+ terminology.getCodeSystem().getVUID() + "' is not unique to the import data");
			}
		}
		
		if (terminology.getCodeSystem().getVersion().getCodedConcepts() != null)
		{
			for (CodedConcept cc : terminology.getCodeSystem().getVersion().getCodedConcepts().getCodedConcept())
			{
				if (cc.getAction() == ActionType.ADD && cc.getVUID() != null)
				{
					if (Frills.getNidForVUID(cc.getVUID()).isPresent())
					{
						throw new RuntimeException("The VUID specified for the new concept '" + cc.getName() + "' : '" + cc.getVUID() + "' is already in use");
					}
					else if (!vuidsInXmlFile_.add(cc.getVUID()))
					{
						throw new RuntimeException("The VUID specified for the new concept '" + cc.getName() + "' : '" 
								+ cc.getVUID() + "' is not unique to the import data");
					}
				}
				if (cc.getDesignations() != null && cc.getDesignations().getDesignation() != null)
				{
					for (Designation d : cc.getDesignations().getDesignation())
					{
						if (d.getAction() == ActionType.ADD && d.getVUID() != null)
						{
							if (Frills.getNidForVUID(d.getVUID()).isPresent())
							{
								throw new RuntimeException("The VUID specified for the new designation '" + d.getValueNew() + "' : '" + d.getVUID() + "' is already in use");
							}
							else if (!vuidsInXmlFile_.add(d.getVUID()))
							{
								throw new RuntimeException("The VUID specified for the new designation '" + d.getValueNew() + "' : '" 
										+ d.getVUID() + "' is not unique to the import data");
							}
						}
					}
				}
			}
		}
		
		if (terminology.getCodeSystem().getVersion().getMapSets() != null)
		{
			for (MapSet ms : terminology.getCodeSystem().getVersion().getMapSets().getMapSet())
			{
				if (ms.getAction() == ActionType.ADD && ms.getVUID() != null)
				{
					if (Frills.getNidForVUID(ms.getVUID()).isPresent())
					{
						throw new RuntimeException("The VUID specified for the new mapset '" + ms.getName() + "' : '" + ms.getVUID() + "' is already in use");
					}
					else if (!vuidsInXmlFile_.add(ms.getVUID()))
					{
						throw new RuntimeException("The VUID specified for the new mapset '" + ms.getName() + "' : '" 
								+ ms.getVUID() + "' is not unique to the import data");
					}
					
					if (ms.getDesignations() != null && ms.getDesignations().getDesignation() != null)
					{
						for (MapSet.Designations.Designation d : ms.getDesignations().getDesignation())
						{
							if (d.getAction() == ActionType.ADD && d.getVUID() != null)
							{
								if (Frills.getNidForVUID(d.getVUID()).isPresent())
								{
									throw new RuntimeException("The VUID specified for the new mapset designation '" + d.getValueNew() + "' : '" + d.getVUID() + "' is already in use");
								}
								else if (!vuidsInXmlFile_.add(d.getVUID()))
								{
									throw new RuntimeException("The VUID specified for the new mapset designation '" + d.getValueNew() + "' : '" 
											+ d.getVUID() + "' is not unique to the import data");
								}
							}
						}
					}
				}
				if (ms.getMapEntries() != null && ms.getMapEntries().getMapEntry() != null)
				{
					for (MapEntry me : ms.getMapEntries().getMapEntry())
					{
						if (me.getAction() == ActionType.ADD && me.getVUID() != null)
						{
							if (Frills.getNidForVUID(me.getVUID()).isPresent())
							{
								throw new RuntimeException("The VUID specified for the new map entry '" + me.getSourceCode() + "' : '" + ms.getVUID() + "' is already in use");
							}
							else if (!vuidsInXmlFile_.add(me.getVUID()))
							{
								throw new RuntimeException("The VUID specified for the new map entry '" + me.getSourceCode() + "' : '" 
										+ me.getVUID() + "' is not unique to the import data");
							}
						}
						if (me.getDesignations() != null && me.getDesignations().getDesignation() != null)
						{
							for (MapEntry.Designations.Designation d : me.getDesignations().getDesignation())
							{
								if (d.getAction() == ActionType.ADD && d.getVUID() != null)
								{
									if (Frills.getNidForVUID(d.getVUID()).isPresent())
									{
										throw new RuntimeException("The VUID specified for the new mapentry designation '" + d.getValueNew() + "' : '" + d.getVUID() + "' is already in use");
									}
									else if (!vuidsInXmlFile_.add(d.getVUID()))
									{
										throw new RuntimeException("The VUID specified for the new mapentry designation '" + d.getValueNew() + "' : '" 
												+ d.getVUID() + "' is not unique to the import data");
									}
								}
							}
						}
					}
				}
			}
		}
		
		if (terminology.getSubsets() != null)
		{
			for (Subset s : terminology.getSubsets().getSubset())
			{
				if (s.getAction() == ActionType.ADD && s.getVUID() != null)
				{
					if (Frills.getNidForVUID(s.getVUID()).isPresent())
					{
						throw new RuntimeException("The VUID specified for the new subset '" + s.getName() + "' : '" + s.getVUID() + "' is already in use");
					}
					else if (!vuidsInXmlFile_.add(s.getVUID()))
					{
						throw new RuntimeException("The VUID specified for the new subset '" + s.getName() + "' : '" 
								+ s.getVUID() + "' is not unique to the import data");
					}
				}
			}
		}
	}
	
	/**
	 * Put any annotations and associations listed in the new section into our lists for UUID lookup.
	 */
	private void populateNewProperties(Terminology terminology)
	{
		LOG.info("Checking for properties that need creation");
		
		if (terminology.getTypes() != null)
		{
			for (Type t : terminology.getTypes().getType())
			{
				switch (t.getKind())
				{
					case DESIGNATION_TYPE:
						throw new RuntimeException("New extended designations types aren't supported yet");
					case PROPERTY_TYPE:
						annotations_.addProperty(t.getName());
						break;
					case RELATIONSHIP_TYPE:
						associations_.addProperty(new PropertyAssociation(associations_, t.getName(), t.getName(), null, t.getName(), false));	
						break;
					default :
						throw new RuntimeException("Unexepected error");
					
				}
			}
		}
	}
	
	/**
	 * Put any Subsets listed in the new section into our lists for UUID lookup.
	 */
	private void populateNewSubsets(Terminology terminology) throws IOException
	{
		LOG.info("Checking for subsets that need creation");
		
		if (terminology.getSubsets() != null)
		{
			for (Subset s : terminology.getSubsets().getSubset())
			{
				switch (s.getAction())
				{
					case ADD:
						subsets_.addProperty(s.getName());
						
						if (s.getVUID() != null)
						{
							vuidToSubsetMap_.put(s.getVUID(), subsets_.getProperty(s.getName()).getUUID());
						}
						break;
					case REMOVE: case NONE:
						// no-op
						break;
					case UPDATE:
						throw new IOException("Update of subset is not supported: " + s.getName());
					default :
						throw new RuntimeException("Unexepected error");
				}
			}
		}
	}
	
	/**
	 * Just loads the ones that were specified as new.
	 */
	private void createNewProperties(Terminology terminology) throws Exception
	{
		//Have to turn off dupe detection, cause they were already entered once, above, in the populate code.
		ConverterUUID.disableUUIDMap_ = true;
		LOG.info("Checking for properties that need creation");
		gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType associations = new BPT_Associations("VHAT");
		gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType annotations = new BPT_Annotations("VHAT");
		
		if (terminology.getTypes() != null)
		{
			for (Type t : terminology.getTypes().getType())
			{
				switch (t.getKind())
				{
					case DESIGNATION_TYPE:
						throw new RuntimeException("New extended designations types aren't supported yet");
					case PROPERTY_TYPE:
						annotations.addProperty(t.getName());
						break;
					case RELATIONSHIP_TYPE:
						associations.addProperty(new PropertyAssociation(associations, t.getName(), t.getName(), null, t.getName(), false));	
						break;
					default :
						throw new RuntimeException("Unexepected error");
					
				}
			}
		}
		
		importUtil_.loadMetaDataItems(associations, null);
		importUtil_.loadMetaDataItems(annotations, null);
		ConverterUUID.disableUUIDMap_ = false;
	}
	
	private void createNewSubsets(Terminology terminology) throws IOException
	{
		LOG.info("Creating new Subsets");
		
		if (terminology.getSubsets() != null)
		{
			//create the new ones
			try
			{
				importUtil_.loadMetaDataItems(subsets_, null);
			}
			catch (Exception e)
			{
				throw new RuntimeException("Unexpected error");
			}
			
			for (Subset s : terminology.getSubsets().getSubset())
			{
				switch (s.getAction())
				{
					case ADD:
						//add the vuid, now that the concept is there
						Long vuid = s.getVUID() == null ? 
								(vuidSupplier_ == null ? null : vuidSupplier_.getAsLong())
								: s.getVUID();
				
						if (vuid != null)
						{
							vuidToSubsetMap_.put(vuid, subsets_.getProperty(s.getName()).getUUID());
							importUtil_.addStaticStringAnnotation(ComponentReference.fromConcept(subsets_.getProperty(s.getName()).getUUID()), vuid.toString(), 
									MetaData.VUID.getPrimordialUuid(), State.ACTIVE);
						}
						break;
					case REMOVE:
						// add it into the subset map
						subsets_.addProperty(s.getName());
						importUtil_.createConcept(subsets_.getProperty(s.getName()).getUUID(), null, State.INACTIVE, null);
						break;
					case NONE:
						// add it into the subset map
						subsets_.addProperty(s.getName());
						break;
					default :
						throw new RuntimeException("Unexepected error");
				}
			}
		}
	}
	
	
	private void headerCheck(Terminology terminology) throws IOException
	{
		LOG.info("Checking the file header");
		CodeSystem cs = terminology.getCodeSystem();
		if (cs.getAction() != null && cs.getAction() != ActionType.NONE)
		{
			throw new IOException("Code System must be null or 'none' for this importer");
		}
		
		if (StringUtils.isNotBlank(cs.getName()) && !cs.getName().equalsIgnoreCase("VHAT"))
		{
			throw new IOException("Code System Name should be blank or VHAT");
		}
		
		if (cs.getVersion().isAppend() != null && !cs.getVersion().isAppend().booleanValue())
		{
			throw new IOException("Append must be true if provided");
		}
		
		// TODO (later) ever need to handle preferred designation type?  I don't think you can change it during a delta update
		// currently, we don't even have / store this information, so need to look at that from a bigger picture
	}
	
	/**
	 * @param terminology
	 * @throws IOException 
	 */
	private void requiredChecks(Terminology terminology) throws IOException
	{
		if (terminology.getCodeSystem() != null && terminology.getCodeSystem().getVersion() != null)
		{
			if (terminology.getCodeSystem().getVersion().getCodedConcepts() != null)
			{
				for (CodedConcept cc : terminology.getCodeSystem().getVersion().getCodedConcepts().getCodedConcept())
				{
					UUID conceptUUID = conceptChecks(cc);
					
					if (cc.getDesignations() != null)
					{
						for (Designations.Designation d : cc.getDesignations().getDesignation())
						{
							designationChecks(d, cc, conceptUUID);
						}
					}
					
					if (cc.getProperties() != null)
					{
						for (Property p : cc.getProperties().getProperty())
						{
							propertyChecks(p, cc, conceptUUID);
						}
					}
					
					if (cc.getRelationships() != null)
					{
						for (Relationship r : cc.getRelationships().getRelationship())
						{
							if (associations_.getProperty(r.getTypeName()) == null)
							{
								associations_.addProperty(new PropertyAssociation(null, r.getTypeName(), null, null, "doesn't-matter", false));
								if (!Get.conceptService().hasConcept(Get.identifierService()
										.getConceptSequenceForUuids(associations_.getProperty(r.getTypeName()).getUUID())))
								{
									throw new IOException("The association '" + r.getTypeName() + "' isn't in the system - from " + cc.getCode() 
										+ " and it wasn't listed as a new association.  Expected to find " + associations_.getProperty(r.getTypeName()).getUUID());
								}
							}
							if (r.getAction() == null)
							{
								throw new IOException("Action must be provided on every relationship.  Missing on " + cc.getCode() + ":" 
									+ r.getTypeName());
							}
							if (r.getAction() == ActionType.REMOVE && r.isActive() == null)
							{
								r.setActive(false);
							}

							if (r.isActive() == null)
							{
								throw new IOException("Active must be provided on every relationship.  Missing on " + cc.getCode() + ":" 
									+ r.getTypeName());
							}
							
							switch(r.getAction())
							{
								case ADD:
									Optional<UUID> targetConcept = findConcept(r.getNewTargetCode());
									if (StringUtils.isBlank(r.getNewTargetCode()) || !targetConcept.isPresent())
									{
										throw new IOException("New Target Code must be provided for new relationships.  Missing on " + cc.getCode() + 
											cc.getCode() + ":" + r.getTypeName());
									}
									if (conceptUUID != null && findAssociationSememe(conceptUUID, associations_.getProperty(r.getTypeName()).getUUID(), 
											targetConcept.get()).isPresent())
									{
										throw new IOException("Add was specified for the association." + cc.getCode() + 
											cc.getCode() + ":" + r.getTypeName() + ":" + r.getNewTargetCode() + " but is already seems to exist");
									}
									break;
								case NONE:
									//noop
									break;
								case REMOVE:
								case UPDATE:
									Optional<UUID> oldTarget = findConcept(r.getOldTargetCode());
									if (StringUtils.isBlank(r.getOldTargetCode()) || !oldTarget.isPresent())
									{
										throw new IOException("Old Target Code must be provided for existing relationships.  Missing on " + cc.getCode() + 
											cc.getCode() + ":" + r.getTypeName());
									}
									if (!findAssociationSememe(conceptUUID, associations_.getProperty(r.getTypeName()).getUUID(), 
										oldTarget.get()).isPresent())
									{
										throw new IOException("Can't locate existing association to update for .  Missing on " + cc.getCode() + 
											cc.getCode() + ":" + r.getTypeName() + ":" + r.getOldTargetCode());
									}
									break;
							}
						}
					}
				}
			}
			
			if (terminology.getCodeSystem().getVersion().getMapSets() != null && terminology.getCodeSystem().getVersion().getMapSets().getMapSet() != null)
			{
				if (terminology.getCodeSystem().getVersion().getMapSets().getMapSet().size() > 0 && associations_.getProperty("has_parent") == null)
				{
					associations_.addProperty(new PropertyAssociation(null, "has_parent", null, null, "doesn't-matter", false));
					if (!Get.conceptService().hasConcept(Get.identifierService()
							.getConceptSequenceForUuids(associations_.getProperty("has_parent").getUUID())))
					{
						throw new IOException("The association 'has_parent' isn't in the system - required for mapsets "
							+ " and it wasn't listed as a new association.  Expected to find " + associations_.getProperty("has_parent").getUUID());
					}
				}
				for (MapSet ms : terminology.getCodeSystem().getVersion().getMapSets().getMapSet())
				{
					UUID mapsetUUID = conceptChecks(ms);
					
					if (ms.getDesignations() != null && ms.getDesignations().getDesignation() != null)
					{
						for (gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation d : 
							ms.getDesignations().getDesignation())
						{
							designationChecks(d, ms, mapsetUUID);
						}
					}
					
					if (ms.getProperties() != null)
					{
						for (gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.MapSets.MapSet.Properties.Property p : 
							ms.getProperties().getProperty())
						{
							propertyChecks(p, ms, mapsetUUID);
						}
					}
					
					if (ms.getRelationships() != null && ms.getRelationships().getRelationship() != null && ms.getRelationships().getRelationship().size() > 0)
					{
						throw new IOException("Relationships are not supported on mapsets.  Mapset '" + ms.getName() + "'");
					}
					
					if (StringUtils.isBlank(ms.getName()))
					{
						throw new IOException("Mapsets must have names - not found on " + ms.getCode());
					}
					
					for (MapEntry me : ms.getMapEntries().getMapEntry())
					{
						if (me.getDesignations() != null && me.getDesignations().getDesignation() != null && me.getDesignations().getDesignation().size() > 0)
						{
							throw new IOException("Designations are not supported on map entries!");
						}
						
						if (me.getRelationships() != null && me.getRelationships().getRelationship() != null && me.getRelationships().getRelationship().size() > 0)
						{
							throw new IOException("Designations are not supported on map entries!");
						}
						
						if (me.getProperties() != null && me.getProperties().getProperty() != null)
						{
							String gemFlag = null;
							for (Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry.Properties.Property p : 
								me.getProperties().getProperty())
							{
								if (!p.getTypeName().equals("GEM_Flags"))
								{
									throw new IOException("The only property supported on mapsets is 'GEM_Flags'.  '" + p.getTypeName() + "' is not supported");
								}
								else
								{
									if (gemFlag != null)
									{
										throw new IOException("Only expect one gem flag per map entry");
									}
									else
									{
										gemFlag = p.getValueNew();
									}
								}
							}
						}
						
						if (me.getAction() == null)
						{
							throw new IOException("Action is required on all map entries!");
						}

						if (me.getAction() == ActionType.ADD)
						{
							if (StringUtils.isBlank(me.getSourceCode()))
							{
								throw new IOException("Source code is required on all add map entries");
							}
							
							if (StringUtils.isBlank(me.getTargetCode()))
							{
								throw new IOException("Target code is required on all add map entries");
							}
							if (me.getVUID() == null && vuidSupplier_ == null)
							{
								throw new IOException("No VUID was supplied for map entry '" + me.getSourceCode() + ":" + me.getTargetCode() 
								+ "' and vuid generation was not requested.");
							}
							
							if (!findConcept(me.getSourceCode()).isPresent() && !conceptsToBeCreated_.contains(me.getSourceCode()))
							{
								throw new IOException("The source concept '" + me.getSourceCode() + "' doesn't exist for creating the mapset " + ms.getName());
							}
							
							if (!findConcept(me.getTargetCode()).isPresent() && !conceptsToBeCreated_.contains(me.getTargetCode()))
							{
								throw new IOException("The source concept '" + me.getSourceCode() + "' doesn't exist for creating the mapset " + ms.getName());
							}
							
							if (mapsetUUID != null && me.getVUID() != null &&  Get.identifierService().hasUuid(createNewMapItemUUID(mapsetUUID, me.getVUID().toString())) 
									&& Get.sememeService().hasSememe(Get.identifierService().getSememeSequenceForUuids(createNewMapItemUUID(mapsetUUID, me.getVUID().toString()))))
							{
								throw new IOException("The map entry '" + me.getVUID() + "' already exists");
							}
						}
						
						if (me.getAction() == ActionType.REMOVE || me.getAction() == ActionType.UPDATE)
						{
							if (me.getVUID() == null)
							{
								throw new IOException("VUID is required on all remove or update vuid entries");
							}
							
							if (!Get.identifierService().hasUuid(createNewMapItemUUID(mapsetUUID, me.getVUID().toString())) 
									|| Get.sememeService().hasSememe(Get.identifierService().getSememeSequenceForUuids(createNewMapItemUUID(mapsetUUID, me.getVUID().toString()))))
							{
								throw new IOException("The map entry for '" + me.getVUID() + "' could not be found");
							}
							
							if (me.getAction() == ActionType.UPDATE)
							{
								UUID existingSourceUUID = Get.identifierService().getUuidPrimordialFromConceptId(Get.sememeService().getSememe(
										Get.identifierService().getSememeSequenceForUuids(createNewMapItemUUID(mapsetUUID, me.getVUID().toString()))).getReferencedComponentNid()).get();
								if (existingSourceUUID.equals(findConcept(me.getSourceCode()).get()))
								{
									throw new IOException("Changing the source concept of a map entry isn't allowed.  Retire the map entry, create a new one.");
								}
							}
						}
					}
				}
			}
			
		}
	}
	
	/**
	 * @return may return null, if no code was specified in the concept
	 * @throws IOException
	 */
	private UUID conceptChecks(ConceptType concept) throws IOException
	{
		UUID conceptUUID = null;
		final String typeLabel;
		if (concept instanceof Terminology.CodeSystem.Version.CodedConcepts.CodedConcept)
		{
			typeLabel = "concept";
		}
		else if (concept instanceof Terminology.CodeSystem.Version.MapSets.MapSet)
		{
			typeLabel = "mapset";
		}
		else
		{
			typeLabel = "unexpected";
		}
		if (concept.getAction() == null)
		{
			throw new IOException("Action must be provided on every " + typeLabel + ".  Missing on " + concept.getCode());
		}
		if (concept.getAction() == ActionType.REMOVE && concept.isActive() == null)
		{
			concept.setActive(false);
		}
		if (concept.isActive() == null)
		{
			throw new IOException("Active must be provided on every " + typeLabel + ".  Missing on " + concept.getCode());
		}
		if ((concept.getAction() == ActionType.UPDATE || concept.getAction() == ActionType.REMOVE || concept.getAction() == ActionType.NONE) 
			&& StringUtils.isBlank(concept.getCode()))
		{
			throw new IOException("Concept code must be provided on every " + typeLabel + " where action is update or remove or none.");
		}
		else if (concept.getAction() != ActionType.ADD)
		{
			conceptUUID = findConcept(concept.getCode()).orElseThrow(() -> new RuntimeException("Cannot locate " + typeLabel + " for code '" + concept.getCode() + "'"));
		}
		if (concept.getAction() == ActionType.ADD)
		{
			if (StringUtils.isBlank(concept.getCode()) && concept.getVUID() == null && vuidSupplier_ == null)
			{
				throw new IOException("No code or vuid was supplied, and generate vuids was not requested for " + concept.getName());
			}
			
			String code = StringUtils.isBlank(concept.getCode()) ? (concept.getVUID() == null ? null : concept.getVUID().toString()) : concept.getCode();
			if (StringUtils.isNotBlank(code) && findConcept(code).isPresent())
			{
				throw new IOException("Add was specified for the " + typeLabel + " '" + code + "' but that " + typeLabel + " already exists!");
			}
			if (StringUtils.isNotBlank(code))
			{
				conceptsToBeCreated_.add(code);
				conceptUUID = createNewConceptUuid(code);
			}
		}
		return conceptUUID;
	}
	
	private void designationChecks(DesignationType d, ConceptType parentConcept, UUID parentConceptUUID) throws IOException
	{
		if (d.getAction() == null)
		{
			throw new IOException("Action must be provided on every designation.  Missing on " + parentConcept.getCode() + ":" + d.getCode());
		}
		if (d.getAction() == ActionType.REMOVE && d.isActive() == null)
		{
			d.setActive(false);
		}
		if (d.isActive() == null && StringUtils.isBlank(d.getMoveFromConceptCode()))
		{
			throw new IOException("Active must be provided on designations in this case.  Missing on " + parentConcept.getCode() + ":" + d.getCode());
		}
		if (StringUtils.isNotBlank(d.getTypeName()))
		{
			if (extendedDescriptionTypeNameMap.get(d.getTypeName().toLowerCase()) == null)
			{
				throw new IOException("Unexpected TypeName on " + parentConcept.getCode() + ":" + d.getCode() + ": " + d.getTypeName());
			}
		}
		else
		{
			//type is required on add
			if (d.getAction() == ActionType.ADD)
			{
				throw new IOException("Missing TypeName on " + parentConcept.getCode() + ":" + d.getCode());
			}
		}
		
		if ((d.getAction() == ActionType.ADD || d.getAction() == ActionType.NONE || d.getAction() == ActionType.REMOVE) 
				&& StringUtils.isNotBlank(d.getMoveFromConceptCode()))
		{
			throw new IOException("Move From Concept Code should only be used with action type of UPDATE");
		}
		
		UUID descriptionUUID = null;
		
		if (d.getAction() == ActionType.REMOVE || d.getAction() == ActionType.UPDATE || d.getAction() == ActionType.NONE)
		{
			if (StringUtils.isBlank(d.getCode()))
			{
				throw new IOException("The designation '" + d.getTypeName() + "' doesn't have a code - from " + parentConcept.getCode()+ ":" 
					+ d.getCode());
			}
			if (d.getAction() == ActionType.UPDATE && StringUtils.isNotBlank(d.getMoveFromConceptCode()))
			{
				Optional<UUID> donerConcept = findConcept(d.getMoveFromConceptCode());
				if (!donerConcept.isPresent())
				{
					throw new IOException("Cannot locate MoveFromConceptCode '" + d.getMoveFromConceptCode() + "' for designation '"
						+ d.getCode() + "' on concept:" +  parentConcept.getCode()+ ":" + d.getCode());
				}
				Optional<UUID> description = findDescription(donerConcept.get(), d.getCode());
				if (!description.isPresent())
				{
					throw new IOException("The designation '" + d.getCode() + "' doesn't seem to exist on doner concept:" +  d.getMoveFromConceptCode() 
						+ ":"+ d.getCode());
				}
				descriptionUUID = description.get();
			}
			else 
			{
				Optional<UUID> description = findDescription(parentConceptUUID, d.getCode());
				if (!description.isPresent())
				{
					throw new IOException("The designation '" + d.getCode() + "' doesn't seem to exist on concept:" +  parentConcept.getCode()+ ":" 
							+ d.getCode());
				}
				descriptionUUID = description.get();
			}
		}
		
		if (d.getAction() == ActionType.ADD)
		{
			if (StringUtils.isBlank(d.getCode()) && d.getVUID() == null && vuidSupplier_ == null)
			{
				throw new IOException("No code or vuid was supplied, and generate vuids was not requested for a designation");
			}
			
			String code = StringUtils.isBlank(d.getCode()) ? (d.getVUID() == null ? null : d.getVUID().toString()) : d.getCode();
			if (StringUtils.isNotBlank(code) && findDescription(parentConceptUUID, code).isPresent())
			{
				throw new IOException("Add was specified for the designation '" + code + "' but that designation already exists!");
			}
			if (StringUtils.isNotBlank(code))
			{
				descriptionUUID = createNewDescriptionUuid(parentConceptUUID, code);
			}
		}
		
		if (d instanceof Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation)
		{
			Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation cd = 
					(Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation)d;
			if (cd.getProperties() != null)
			{
				for ( PropertyType p : cd.getProperties().getProperty())
				{
					if (annotations_.getProperty(p.getTypeName()) == null)
					{
						//add it, so we have the UUID mapping, but we don't need to create it, since it should already exist
						annotations_.addProperty(p.getTypeName());
						if (!Get.conceptService().hasConcept(Get.identifierService()
								.getConceptSequenceForUuids(annotations_.getProperty(p.getTypeName()).getUUID())))
						{
							throw new IOException("The property '" + p.getTypeName() + "' isn't in the system - from " + parentConcept.getCode() + ":" 
								+ d.getCode() + " and it wasn't listed as a new property");
						}
					}
					if (p.getAction() == null)
					{
						throw new IOException("Action must be provided on every property.  Missing on " + parentConcept.getCode() + ":" 
							+ d.getCode() + ":" + p.getTypeName());
					}
					if (p.getAction() == ActionType.REMOVE && p.isActive() == null)
					{
						p.setActive(false);
					}
	
					if (p.isActive() == null)
					{
						throw new IOException("Active must be provided on every property.  Missing on " + parentConcept.getCode() + ":" 
							+ d.getCode() + ":" + p.getTypeName());
					}
					
					if (p.getAction() == ActionType.REMOVE || p.getAction() == ActionType.UPDATE)
					{
						if (StringUtils.isBlank(p.getValueOld()))
						{
							throw new IOException("The property '" + p.getTypeName() + "' doesn't have a ValueOld - from " + parentConcept.getCode()+ ":" 
								+ d.getCode());
						}
						if (!findPropertySememe(descriptionUUID, annotations_.getProperty(p.getTypeName()).getUUID(), p.getValueOld()).isPresent())
						{
							throw new IOException("The property '" + p.getTypeName() + "' with an old Value of ' " + p.getValueOld() 
								+ "' doesn't seem to exist on concept:" +  parentConcept.getCode()+ ":" 
									+ d.getCode());
						}
					}
					
					if (p.getAction() == ActionType.ADD && findPropertySememe(descriptionUUID, annotations_.getProperty(p.getTypeName()).getUUID(),
							p.getValueNew()).isPresent())
					{
						throw new IOException("Add was specified for the property '" + p.getTypeName() + "' with an new Value of ' " + p.getValueNew() 
							+ "' but that property already exists on :" +  parentConcept.getCode()+ ":"+ d.getCode());
					}
					
				}
			}
			
			if (cd.getSubsetMemberships() != null)
			{
				for (SubsetMembership sm : cd.getSubsetMemberships().getSubsetMembership())
				{
					if (vuidToSubsetMap_.get(sm.getVUID()) == null)
					{
						//add it, so we have the UUID mapping, but we don't need to create it, since it should already exist
						Optional<Integer> subsetNid = Frills.getNidForVUID(sm.getVUID());
						if (!subsetNid.isPresent())
						{
							throw new IOException("The subset '" + sm.getVUID() + "' isn't in the system - from " + parentConcept.getCode() + ":" 
								+ d.getCode() + " and it wasn't listed as a new subset");
						}
						else
						{
							vuidToSubsetMap_.put(sm.getVUID(), Get.identifierService().getUuidPrimordialFromConceptId(subsetNid.get()).get());
						}
					}
					if (sm.getAction() == null)
					{
						throw new IOException("Action must be provided on every subset membership.  Missing on " + parentConcept.getCode() + ":" 
							+ d.getCode() + ":" + sm.getVUID());
					}
					if (sm.getAction() == ActionType.REMOVE && sm.isActive() == null)
					{
						sm.setActive(false);
					}
	
					if (sm.isActive() == null)
					{
						throw new IOException("Active must be provided on every subset membership.  Missing on " + parentConcept.getCode() + ":" 
							+ d.getCode() + ":" + sm.getVUID());
					}
					
					boolean membershipExists = descriptionUUID == null ? false : 
						Get.sememeService().getSememesForComponentFromAssemblage(Get.identifierService().getNidForUuids(descriptionUUID), 
							Get.identifierService().getConceptSequenceForUuids(vuidToSubsetMap_.get(sm.getVUID()))).findAny().isPresent();
					if (sm.getAction() == ActionType.ADD && membershipExists)
					{
						throw new IOException("Add was specified for a subset membership, but it appears to already exist for " + parentConcept.getCode() + ":" 
								+ d.getCode() + ":" + sm.getVUID());
					}
					else if ((sm.getAction() == ActionType.UPDATE || sm.getAction() == ActionType.REMOVE) && !membershipExists)
					{
						throw new IOException("Remove or update was specified for a subset membership, but the membership doesn't currently exist for "
								+ parentConcept.getCode() + ":"+ d.getCode() + ":" + sm.getVUID());
					}
				}
			}
		}
	}
	
	private void propertyChecks(PropertyType p, ConceptType parentConcept, UUID parentConceptUUID) throws IOException
	{
		if (annotations_.getProperty(p.getTypeName()) == null)
		{
			annotations_.addProperty(p.getTypeName());
			if (!Get.conceptService().hasConcept(Get.identifierService()
					.getConceptSequenceForUuids(annotations_.getProperty(p.getTypeName()).getUUID())))
			{
				throw new IOException("The property '" + p.getTypeName() + "' isn't in the system - from " + parentConcept.getCode() 
					+ " and it wasn't listed as a new property.  Expected to find " + annotations_.getProperty(p.getTypeName()).getUUID());
			}
		}
		if (p.getAction() == null)
		{
			throw new IOException("Action must be provided on every property.  Missing on " + parentConcept.getCode() + ":" 
				+ p.getTypeName());
		}
		if (p.getAction() == ActionType.REMOVE && p.isActive() == null)
		{
			p.setActive(false);
		}

		if (p.isActive() == null)
		{
			throw new IOException("Active must be provided on every property.  Missing on " + parentConcept.getCode() + ":" 
				+ p.getTypeName());
		}
		if (p.getAction() == ActionType.ADD && StringUtils.isBlank(p.getValueNew()))
		{
			throw new IOException("The property '" + p.getTypeName() + "' doesn't have a ValueNew - from " + parentConcept.getCode());
		}
		if (p.getAction() == ActionType.REMOVE || p.getAction() == ActionType.UPDATE)
		{
			if (StringUtils.isBlank(p.getValueOld()))
			{
				throw new IOException("The property '" + p.getTypeName() + "' doesn't have a ValueOld - from " + parentConcept.getCode());
			}
			if (!findPropertySememe(parentConceptUUID, annotations_.getProperty(p.getTypeName()).getUUID(), p.getValueOld()).isPresent())
			{
				throw new IOException("The property '" + p.getTypeName() + "' with an old Value of ' " + p.getValueOld() 
					+ "' doesn't seem to exist on concept:" +  parentConcept.getCode());
			}
		}
		
		if (p.getAction() == ActionType.ADD && findPropertySememe(parentConceptUUID, annotations_.getProperty(p.getTypeName()).getUUID(),
				p.getValueNew()).isPresent())
		{
			throw new IOException("Add was specified for the property '" + p.getTypeName() + "' with an new Value of ' " + p.getValueNew() 
				+ "' but that property already exists on :" +  parentConcept.getCode());
		}
	}

	private void loadConcepts(CodedConcepts codedConcepts) throws IOException
	{
		if (codedConcepts != null)
		{
			LOG.info("Loading "  + codedConcepts.getCodedConcept().size() + " Concepts");
			
			for (CodedConcept cc : codedConcepts.getCodedConcept())
			{
				ComponentReference concept = null;
				switch (cc.getAction())
				{
					case ADD:
						String vuid = cc.getVUID() == null ? 
								(vuidSupplier_ == null ? null : vuidSupplier_.getAsLong() + "")
								: cc.getVUID().toString();
						
						String code = StringUtils.isBlank(cc.getCode()) ? vuid : cc.getCode();
						
						if (StringUtils.isBlank(code))
						{
							throw new RuntimeException("No code supplied, and vuid generation is disabled!");
						}
						concept = ComponentReference.fromConcept(importUtil_.createConcept(createNewConceptUuid(code), null, 
										cc.isActive() ? State.ACTIVE : State.INACTIVE, null));
								
						if (StringUtils.isNotBlank(vuid))
						{
							importUtil_.addStaticStringAnnotation(concept, vuid, MetaData.VUID.getPrimordialUuid(), State.ACTIVE);
						}
						
						if (StringUtils.isNotBlank(code))
						{
							importUtil_.addStaticStringAnnotation(concept, code, MetaData.CODE.getPrimordialUuid(), State.ACTIVE);
						}
						break;
					case NONE:
						//noop
						break;
					case REMOVE:
						concept = ComponentReference.fromConcept(importUtil_.createConcept(findConcept(cc.getCode()).get(), null, State.INACTIVE, null));
						for (ObjectChronology<?> o : recursiveRetireNested(concept.getPrimordialUuid()))
						{
							importUtil_.storeManualUpdate(o);
						}
						break;
					case UPDATE:
						//We could, potentially support updating vuid, but the current system doesnt.
						//so we only process activate / inactivate changes here.
						concept = ComponentReference.fromConcept(importUtil_.createConcept(findConcept(cc.getCode()).get(), null, 
							cc.isActive() ? State.ACTIVE : State.INACTIVE, null));
						break;
					default :
						throw new RuntimeException("Unexpected error");
				}
				if (concept == null)
				{
					concept = ComponentReference.fromConcept(findConcept(cc.getCode()).get());
				}
				loadDesignations(concept, cc.getDesignations());
				loadConceptProperties(concept, cc.getProperties());
				loadRelationships(concept, cc.getRelationships());
			}
		}
	}

	private UUID createNewConceptUuid(String codeId)
	{
		return ConverterUUID.createNamespaceUUIDFromString("code:" + codeId, true);
	}
	
	private UUID createNewDescriptionUuid(UUID concept, String descriptionId)
	{
		if (concept == null || StringUtils.isBlank(descriptionId))
		{
			return null;
		}
		return ConverterUUID.createNamespaceUUIDFromString("description:" + concept.toString() + ":" + descriptionId, true);
	}
	
	private UUID createNewMapItemUUID(UUID mapSetUUID, String mapItemVuid)
	{
		return ConverterUUID.createNamespaceUUIDFromString("mapSetUuid:" + mapSetUUID + "mapItemVuid:" + mapItemVuid, true);
	}

	/**
	 * @param properties
	 */
	private void loadConceptProperties(ComponentReference concept, 
		gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties properties)
	{
		if (properties != null)
		{
			for (Property p : properties.getProperty())
			{
				handleProperty(concept, p.getTypeName(),  p.getValueOld(),  p.getValueNew(),  p.isActive(),  p.getAction());
			}
		}
	}
	
	private void loadConceptProperties(ComponentReference concept, 
			gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.MapSets.MapSet.Properties properties)
		{
			if (properties != null)
			{
				for (gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.MapSets.MapSet.Properties.Property p : properties.getProperty())
				{
					handleProperty(concept, p.getTypeName(),  p.getValueOld(),  p.getValueNew(),  p.isActive(),  p.getAction());
				}
			}
		}
	
	private void handleProperty(ComponentReference component, String propertyName, String oldValue, String newValue, boolean isActive, ActionType action)
	{
		switch (action)
		{
			case ADD:
				importUtil_.addStringAnnotation(component, newValue, annotations_.getProperty(propertyName).getUUID(), 
					isActive ? State.ACTIVE : State.INACTIVE);
				break;
			case NONE:
				//noop
				break;
			case REMOVE:
				// REMOVE directive takes precedence. Explicitely set active=false, and fall-through
				isActive = false;
			case UPDATE:
				//These cases are a bit tricky, because the UUID calculated for the sememe was based on the value.  If the value
				//only changed once, you could use old value to find it, but, if it changes twice, you can no longer calculate back to the UUID.
				//So, we will have to match the oldvalue text string directly to the value, to find the right property.
				Optional<UUID> oldProperty = findPropertySememe(component.getPrimordialUuid(), annotations_.getProperty(propertyName).getUUID(), oldValue);
				//we tested this lookup in an earlier error checking pass above, it shouldn't come back null.
				if (!oldProperty.isPresent())
				{
					throw new RuntimeException("oops");
				}
				
				SememeChronology<?> sc = Get.sememeService().getSememe(Get.identifierService().getSememeSequenceForUuids(oldProperty.get()));
				if (sc.getSememeType() == SememeType.STRING)
				{
					//not allowing them to set the value to empty, just assume they only meant to change status in the case where new value is missing
					@SuppressWarnings("unchecked")
					MutableStringSememe<?> mss = ((SememeChronology<StringSememe<?>>)sc)
						.createMutableVersion(MutableStringSememe.class, isActive ? State.ACTIVE : State.INACTIVE, editCoordinate_);
					mss.setString(StringUtils.isBlank(newValue) ? oldValue : newValue);
				}
				else if (sc.getSememeType() == SememeType.DYNAMIC)
				{
					@SuppressWarnings("unchecked")
					MutableDynamicSememe<?> mds = ((SememeChronology<DynamicSememe<?>>)sc)
						.createMutableVersion(MutableDynamicSememe.class, isActive ? State.ACTIVE : State.INACTIVE, editCoordinate_);
					if (mds.getDynamicSememeUsageDescription().getColumnInfo().length != 1 || 
							mds.getDynamicSememeUsageDescription().getColumnInfo()[0].getColumnDataType() != DynamicSememeDataType.STRING)
					{
						throw new RuntimeException("Unexpected dynamic sememe data config!");
					}
					else
					{
						mds.setData(new DynamicSememeData[] {new DynamicSememeStringImpl(StringUtils.isBlank(newValue) ? oldValue : newValue)});
					}
				}
				else
				{
					throw new RuntimeException("Unexpected sememe type!");
				}
				importUtil_.storeManualUpdate(sc);
				break;
			default :
				throw new RuntimeException("Unexepected error");
		}
	}
	
	private Optional<UUID> findPropertySememe(UUID referencedComponent, UUID propertyType, String propertyValue)
	{
		if (referencedComponent == null)
		{
			return Optional.empty();
		}
		return Get.sememeService().getSememesForComponentFromAssemblage(Get.identifierService().getNidForUuids(referencedComponent), 
			Get.identifierService().getConceptSequenceForUuids(propertyType)).filter(sememe ->
			{
				if (sememe.getSememeType() == SememeType.STRING)
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Optional<LatestVersion<StringSememe<?>>> sv = ((SememeChronology)sememe).getLatestVersion(StringSememeImpl.class, readCoordinate_);
					if (sv.isPresent())
					{
						return propertyValue.equals(sv.get().value().getString());
					}
				}
				else if (sememe.getSememeType() == SememeType.DYNAMIC)
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Optional<LatestVersion<DynamicSememe<?>>> sv = ((SememeChronology)sememe).getLatestVersion(DynamicSememe.class, readCoordinate_);
					if (sv.isPresent() && sv.get().value().getDynamicSememeUsageDescription().getColumnInfo().length == 1 && 
							sv.get().value().getDynamicSememeUsageDescription().getColumnInfo()[0].getColumnDataType() == DynamicSememeDataType.STRING)
					{
						return propertyValue.equals(sv.get().value().getData()[0].dataToString());
					}
				}
				return false;
			}).findFirst().<UUID>map(sememe -> 
			{
				return Get.identifierService().getUuidPrimordialForNid(sememe.getNid()).get();
			});
	}
	
	private Optional<UUID> findAssociationSememe(UUID sourceConcept, UUID associationType, UUID targetConcept)
	{
		return Get.sememeService().getSememesForComponentFromAssemblage(Get.identifierService().getNidForUuids(sourceConcept), 
			Get.identifierService().getConceptSequenceForUuids(associationType)).filter(sememe ->
			{
				if (sememe.getSememeType() == SememeType.DYNAMIC)
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Optional<LatestVersion<DynamicSememe<?>>> sv = ((SememeChronology)sememe).getLatestVersion(DynamicSememe.class, readCoordinate_);
					if (sv.isPresent() && sv.get().value().getDynamicSememeUsageDescription().getColumnInfo().length == 1 && 
							sv.get().value().getDynamicSememeUsageDescription().getColumnInfo()[0].getColumnDataType() == DynamicSememeDataType.UUID)
					{
						return  targetConcept.equals(((DynamicSememeUUID) sv.get().value().getData()[0]).getDataUUID());
					}
				}
				return false;
			}).findFirst().<UUID>map(sememe -> 
			{
				return Get.identifierService().getUuidPrimordialForNid(sememe.getNid()).get();
			});
	}
	
	private void loadDesignations(ComponentReference concept, Designations designations)
	{
		if (designations != null)
		{
			for (Designation d : designations.getDesignation())
			{
				loadDesignation(concept, d);
			}
		}
	}
	
	private void loadDesignations(ComponentReference concept, Terminology.CodeSystem.Version.MapSets.MapSet.Designations designations)
	{
		if (designations != null)
		{
			for (gov.va.med.term.vhat.xml.deltaIn.model.Terminology.CodeSystem.Version.MapSets.MapSet.Designations.Designation d : designations.getDesignation())
			{
				loadDesignation(concept, d);
			}
		}
	}

	/**
	 * @param designations
	 */
	private void loadDesignation(ComponentReference concept, DesignationType d)
	{
		ComponentReference descRef = null;
		
		switch (d.getAction())
		{
			case ADD:
				
				String vuid = d.getVUID() == null ? 
						(vuidSupplier_ == null ? null : vuidSupplier_.getAsLong() + "")
						: d.getVUID().toString();
						
				String code = StringUtils.isBlank(d.getCode()) ? vuid : d.getCode();
				
				descRef = ComponentReference.fromChronology(importUtil_.addDescription(concept, createNewDescriptionUuid(concept.getPrimordialUuid(), code), 
					d.getValueNew(), DescriptionType.SYNONYM, false, extendedDescriptionTypeNameMap.get(d.getTypeName().toLowerCase()), 
					d.isActive() ? State.ACTIVE : State.INACTIVE));
				
				
				if (StringUtils.isNotBlank(vuid))
				{
					importUtil_.addStaticStringAnnotation(descRef, vuid, MetaData.VUID.getPrimordialUuid(), State.ACTIVE);
				}
				
				if (StringUtils.isNotBlank(code))
				{
					importUtil_.addStaticStringAnnotation(descRef, code, MetaData.CODE.getPrimordialUuid(), State.ACTIVE);
				}
				
				break;
			case NONE:
				//noop
				break;
			case REMOVE:
				// Here, we will iterate through the nested sememes, setting all to have a status 
				// of 'false' and then fall-through to the UPDATE clause to handle setting the status
				// of the designation itself
				// REMOVE directive takes precedence over Active element 
				// Explicitely set active=false, and fall-through
				d.setActive(false);
				// No break, fall-through for update to the designation itself
			case UPDATE:
				if (StringUtils.isBlank(d.getMoveFromConceptCode()))
				{
					Optional<UUID> oldDescription = findDescription(concept.getPrimordialUuid(), d.getCode());
					//we tested this lookup in an earlier error checking pass above, it shouldn't come back null.
					if (!oldDescription.isPresent())
					{
						throw new RuntimeException("oops");
					}
					
					SememeChronology<? extends SememeVersion<?>> sc = Get.sememeService().getSememe(Get.identifierService().getSememeSequenceForUuids(oldDescription.get()));
					
					@SuppressWarnings({ "unchecked", "rawtypes" }) 
					Optional<LatestVersion<DescriptionSememe<?>>> latest = ((SememeChronology)sc).getLatestVersion(DescriptionSememe.class, readCoordinate_);
					if (!latest.isPresent())
					{
						throw new RuntimeException("Unexected!");
					}
					
					descRef = ComponentReference.fromChronology(sc);
					if (sc.getSememeType() == SememeType.DESCRIPTION)
					{
						//not allowing them to set the value to empty, just assume they only meant to change status in the case where new value is missing
						@SuppressWarnings("unchecked")
						MutableDescriptionSememe<?> mss = ((SememeChronology<DescriptionSememe<?>>)sc)
							.createMutableVersion(MutableDescriptionSememe.class, d.isActive() ? State.ACTIVE : State.INACTIVE, editCoordinate_);
						mss.setText(StringUtils.isBlank(d.getValueNew()) ? 
								(StringUtils.isBlank(d.getValueOld()) ? latest.get().value().getText() : d.getValueOld()) 
								: d.getValueNew());
						mss.setCaseSignificanceConceptSequence(latest.get().value().getCaseSignificanceConceptSequence());
						mss.setDescriptionTypeConceptSequence(latest.get().value().getDescriptionTypeConceptSequence());
						mss.setLanguageConceptSequence(latest.get().value().getLanguageConceptSequence());

						// If the designation is inactivated/removed, set all nested sememes to inactive
						if (!d.isActive())
						{
							for (ObjectChronology<?> o : recursiveRetireNested(sc.getPrimordialUuid()))
							{
								importUtil_.storeManualUpdate(o);
							}
						}
						
						//No changing of type name, code, or vuid
					}
					else
					{
						throw new RuntimeException("Unexpected sememe type!");
					}
					importUtil_.storeManualUpdate(sc);
				}
				else
				{
					//moveFromConceptCode is the VUID of the concept where this description currently exists.
					//if populated, we need to find this description under the old concept, and retire it, while copying all nested items here...
					
					UUID sourceConcept = findConcept(d.getMoveFromConceptCode()).get();
					//we tested this lookup in an earlier error checking pass above, it shouldn't come back null.
					UUID oldDescription = findDescription(sourceConcept, d.getCode()).orElseThrow(() -> new RuntimeException("oops"));
					
					SememeChronology<? extends SememeVersion<?>> oldSc = Get.sememeService().getSememe(Get.identifierService().getSememeSequenceForUuids(oldDescription));
					
					@SuppressWarnings({ "rawtypes", "unchecked" })
					Optional<LatestVersion<DescriptionSememe<?>>> latest = ((SememeChronology)oldSc).getLatestVersion(DescriptionSememe.class, readCoordinate_);
					if (!latest.isPresent())
					{
						throw new RuntimeException("Unexected!");
					}
					
					//Make a new description with the provided and/or old values
					final SememeChronology<DescriptionSememe<?>> newDescription = 
							importUtil_.addDescription(concept, createNewDescriptionUuid(concept.getPrimordialUuid(), d.getCode()), 
							StringUtils.isBlank(d.getValueNew()) ? 
									(StringUtils.isBlank(d.getValueOld()) ? latest.get().value().getText() : d.getValueOld()) 
									: d.getValueNew(),
							DescriptionType.parse(latest.get().value().getDescriptionTypeConceptSequence()), 
							Frills.isDescriptionPreferred(latest.get().value().getNid(), readCoordinate_),
							StringUtils.isBlank(d.getTypeName()) ? 
									Frills.getDescriptionExtendedTypeConcept(readCoordinate_, latest.get().value().getSememeSequence()).orElse(null)
									: extendedDescriptionTypeNameMap.get(d.getTypeName().toLowerCase()), 
							d.isActive() == null ? latest.get().value().getState() : (d.isActive() ? State.ACTIVE : State.INACTIVE));
					descRef = ComponentReference.fromChronology(newDescription);
					
					//copy all other nested components
					Get.sememeService().getSememesForComponent(oldSc.getNid()).forEach(existingNestedSememe ->
					{
						if (existingNestedSememe.getAssemblageSequence() == DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENDED_DESCRIPTION_TYPE.getConceptSequence() ||
								existingNestedSememe.getAssemblageSequence() == MetaData.CODE.getConceptSequence() ||
								existingNestedSememe.getAssemblageSequence() == MetaData.VUID.getConceptSequence() ||
								existingNestedSememe.getAssemblageSequence() == MetaData.US_ENGLISH_DIALECT.getConceptSequence())
						{
							//ignore - these are handled with special case code above and below....
						}
						else
						{
							@SuppressWarnings({ "rawtypes", "unchecked" }) 
							Optional<LatestVersion<SememeVersion<?>>> latestVersionOfExistingNestedSememe = ((SememeChronology)existingNestedSememe).getLatestVersion(SememeVersion.class, readCoordinate_);
							
							if (latestVersionOfExistingNestedSememe.isPresent() && latestVersionOfExistingNestedSememe.get().value().getState() == State.ACTIVE)
							{
								SememeChronology<?> copyOfExistingNestedSememe = null;
								
								if (latestVersionOfExistingNestedSememe.get().contradictions().isPresent()) {
									// TODO handle contradictions
								}
								//expect these to be, primarily, dynamic sememes, refset entries or strings...
								switch (existingNestedSememe.getSememeType())
								{
								case DYNAMIC:
									copyOfExistingNestedSememe = importUtil_.addAnnotation(
											ComponentReference.fromSememe(newDescription.getPrimordialUuid()),
											null,
											((DynamicSememe<?>)latestVersionOfExistingNestedSememe.get().value()).getData(),
											Get.identifierService().getUuidPrimordialFromConceptId(existingNestedSememe.getAssemblageSequence()).get(),
											State.ACTIVE,
											null,
											null);
									break;
								case MEMBER:
									SememeVersion<?> memberSememe = latestVersionOfExistingNestedSememe.get().value();	
									copyOfExistingNestedSememe = importUtil_.addRefsetMembership(ComponentReference.fromSememe(newDescription.getPrimordialUuid()), Get.identifierService().getUuidPrimordialFromConceptId(memberSememe.getAssemblageSequence()).get(), State.ACTIVE, null);
									break;
								case STRING:
									StringSememe<?> stringSememe = (StringSememe<?>)latestVersionOfExistingNestedSememe.get().value();
									// TODO is Get.identifierService().getUuidPrimordialFromConceptId(stringSememe.getAssemblageSequence()) == refsetUuid?
									copyOfExistingNestedSememe = importUtil_.addStringAnnotation(ComponentReference.fromSememe(newDescription.getPrimordialUuid()), stringSememe.getString(), Get.identifierService().getUuidPrimordialFromConceptId(stringSememe.getAssemblageSequence()).get(), State.ACTIVE);
									break;

									//None of these are expected in vhat data
								case DESCRIPTION:
								case LOGIC_GRAPH:
								case LONG:
								case COMPONENT_NID:
								case RELATIONSHIP_ADAPTOR:
								case UNKNOWN:
								default:
									throw new RuntimeException("MoveFromConceptCode doesn't supported nested sememes of type " + existingNestedSememe.getSememeType() + 
											" for designation " + d.getCode());
								}

								if (copyOfExistingNestedSememe != null) {
									copy(importUtil_, copyOfExistingNestedSememe, existingNestedSememe, readCoordinate_, editCoordinate_);
								}
							}
						}
					});
					
					Long vuidToMigrate = d.getVUID() == null ? Frills.getVuId(latest.get().value().getNid(), readCoordinate_).orElse(null) : d.getVUID();
					
					if (vuidToMigrate != null)
					{
						importUtil_.addStaticStringAnnotation(descRef, vuidToMigrate.toString(), MetaData.VUID.getPrimordialUuid(), State.ACTIVE);
					}
					
					String codeToMigrate = null;
					if (StringUtils.isBlank(d.getCode()))
					{
						List<String> oldCodes = Frills.getCodes(latest.get().value().getNid(), readCoordinate_);
						if (oldCodes.size() > 0)
						{
							codeToMigrate = oldCodes.get(0);
							if (oldCodes.size() > 1)
							{
								LOG.warn("More than one code on concept " + concept.getPrimordialUuid());
							}
						}
						else if (vuidToMigrate != null)
						{
							codeToMigrate = vuidToMigrate.toString();
						}
					}
					else
					{
						//use the new code
						codeToMigrate = d.getCode();
					}
					if (StringUtils.isNotBlank(codeToMigrate))
					{
						importUtil_.addStaticStringAnnotation(descRef, codeToMigrate, MetaData.CODE.getPrimordialUuid(), State.ACTIVE);
					}
					
					
					//retire the old sememe:
					try
					{
						importUtil_.storeManualUpdate(Frills.resetStateWithNoCommit(State.INACTIVE, oldSc.getNid(), editCoordinate_, readCoordinate_));
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
					
					//retire nested components
					for (ObjectChronology<?> o : recursiveRetireNested(oldSc.getPrimordialUuid()))
					{
						importUtil_.storeManualUpdate(o);
					}
				}
				
				break;
			default :
				throw new RuntimeException("Unexpected error");
			
		}
		
		if (descRef == null)
		{
			descRef = ComponentReference.fromSememe(findDescription(concept.getPrimordialUuid(), d.getCode()).get());
		}
		
		if (d instanceof Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation)
		{
			Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation dd = 
					(Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation)d;
			loadDesignationProperties(descRef, dd.getProperties());
			loadSubsetMembership(descRef, dd.getSubsetMemberships());
		}
	}
	
	private static void copy(
			IBDFCreationUtility importUtil,
			SememeChronology<?> existingParentComponent,
			SememeChronology<?> copyOfParentComponent,
			StampCoordinate readCoordinate, EditCoordinate editCoordinate) {
		Get.sememeService().getSememesForComponent(existingParentComponent.getNid()).forEach(existingNestedSememe -> {
			@SuppressWarnings({ "rawtypes", "unchecked" }) 
			Optional<LatestVersion<SememeVersion<?>>> latestVersionOfExistingNestedSememe = ((SememeChronology)existingNestedSememe).getLatestVersion(SememeVersion.class, readCoordinate);

			if (latestVersionOfExistingNestedSememe.isPresent() && latestVersionOfExistingNestedSememe.get().value().getState() == State.ACTIVE)
			{
				if (latestVersionOfExistingNestedSememe.get().contradictions().isPresent()) {
					// TODO handle contradictions
				}
				
				SememeChronology<?> copyOfExistingNestedSememe = null;

				//expect these to be, primarily, dynamic sememes, refset entries or strings...
				switch (latestVersionOfExistingNestedSememe.get().value().getChronology().getSememeType())
				{
				case DYNAMIC:
					DynamicSememe<?> dynamicSememe = (DynamicSememe<?>)latestVersionOfExistingNestedSememe.get().value();
					copyOfExistingNestedSememe = importUtil.addAnnotation(
							ComponentReference.fromChronology(copyOfParentComponent),
							null,
							dynamicSememe.getData(),
							Get.identifierService().getUuidPrimordialFromConceptId(existingNestedSememe.getAssemblageSequence()).get(),
							State.ACTIVE,
							null,
							null);
					break;
				case MEMBER:
					SememeVersion<?> memberSememe = latestVersionOfExistingNestedSememe.get().value();
					copyOfExistingNestedSememe = importUtil.addRefsetMembership(ComponentReference.fromChronology(copyOfParentComponent), Get.identifierService().getUuidPrimordialFromConceptId(memberSememe.getAssemblageSequence()).get(), State.ACTIVE, null);
					
					break;
				case STRING:
					StringSememe<?> stringSememe = (StringSememe<?>)latestVersionOfExistingNestedSememe.get().value();
					// TODO is Get.identifierService().getUuidPrimordialFromConceptId(stringSememe.getAssemblageSequence()) == refsetUuid?
					copyOfExistingNestedSememe = importUtil.addStringAnnotation(ComponentReference.fromChronology(copyOfParentComponent), stringSememe.getString(), Get.identifierService().getUuidPrimordialFromConceptId(stringSememe.getAssemblageSequence()).get(), State.ACTIVE);
					break;

					//None of these are expected in vhat data
				case DESCRIPTION:
				case LOGIC_GRAPH:
				case LONG:
				case COMPONENT_NID:
				case RELATIONSHIP_ADAPTOR:
				case UNKNOWN:
				default:
					throw new RuntimeException("MoveFromConceptCode doesn't supported nested sememes of type " + existingParentComponent.getSememeType());
				}

				if (copyOfExistingNestedSememe != null) {
					copy(importUtil, copyOfExistingNestedSememe, existingNestedSememe, readCoordinate, editCoordinate);
				}
			}
		});
	}
	/**
	 * Retire any sememes attached to this component.  Do not change the component.
	 * @param component
	 */
	private List <ObjectChronology<?>> recursiveRetireNested(UUID component) 
	{
		ArrayList<ObjectChronology<?>> updated = new ArrayList<>();
		Get.sememeService().getSememesForComponent(Get.identifierService().getNidForUuids(component)).forEach(sememe ->
		{
			try 
			{
				updated.add(Frills.resetStateWithNoCommit(State.INACTIVE, sememe.getNid(), editCoordinate_, readCoordinate_));
				updated.addAll(recursiveRetireNested(sememe.getPrimordialUuid()));
			} catch (Exception e) 
			{
				throw new RuntimeException(e);
			}
		});
		return updated;
	}

	/**
	 * @param primordialUuid
	 * @param code
	 * @return
	 */
	private Optional<UUID> findDescription(UUID concept, String descriptionCode)
	{
		if (concept == null)
		{
			return Optional.empty();
		}
		return Get.sememeService().getDescriptionsForComponent(Get.identifierService().getNidForUuids(concept)).filter(desc ->
		{
			return findPropertySememe(desc.getPrimordialUuid(),  MetaData.CODE.getPrimordialUuid(), descriptionCode).isPresent();
		}).findAny().<UUID>map(desc -> desc.getPrimordialUuid());
	}
	
	private Optional<UUID> findConcept(String conceptCode)
	{
		IndexServiceBI si = LookupService.get().getService(IndexServiceBI.class, "sememe indexer");
		if (si != null) {
			//force the prefix algorithm, and add a trailing space - quickest way to do an exact-match type of search
			@SuppressWarnings("rawtypes")
			ArrayList<SememeChronology> candidates = new ArrayList<>();
			List<SearchResult> result = si.query(conceptCode + " ", true, new Integer[] {MetaData.CODE.getConceptSequence()}, 50, Long.MAX_VALUE, null);
			result.forEach(sr -> 
			{
				@SuppressWarnings("rawtypes")
				SememeChronology sc = Get.sememeService().getSememe(sr.getNid());
				@SuppressWarnings("unchecked")
				Optional<LatestVersion<StringSememe<?>>> ss = sc.getLatestVersion(StringSememe.class, readCoordinate_);
				if (ss.isPresent() && ss.get().value().getState() == State.ACTIVE && ss.get().value().getString().equals(conceptCode))
				{
					candidates.add(sc);
				}
			});
			if (candidates.size() == 0)
			{
				return Optional.empty();
			}
			else if (candidates.size() > 1)
			{
				throw new RuntimeException("There is more than one concept in the system with a 'Code' of '" + conceptCode + "'");
			}
			else
			{
				return Get.identifierService().getUuidPrimordialForNid(candidates.get(0).getReferencedComponentNid());
			}
		} else {
			LOG.warn("Sememe Index not available - can't lookup VUID");
		}
		return Optional.empty();
	}

	/**
	 * @param subsetMemberships
	 */
	private void loadSubsetMembership(ComponentReference description, SubsetMemberships subsetMemberships)
	{
		if (subsetMemberships != null)
		{
			for(SubsetMembership sm : subsetMemberships.getSubsetMembership())
			{
				switch (sm.getAction())
				{
					case ADD:
						//subset lookups validated previously
						importUtil_.addRefsetMembership(description, vuidToSubsetMap_.get(sm.getVUID()), sm.isActive() ? State.ACTIVE : State.INACTIVE, null);
						break;
					case NONE:
						//noop
						break;
					case REMOVE:
						// REMOVE directive takes precedence. Explicitely set active=false, and fall-through
						sm.setActive(false);
					case UPDATE:
						Get.sememeService().getSememesForComponentFromAssemblage(description.getNid(), Get.identifierService()
							//There really shouldn't be more than one of these, but if there is, no harm in changing state on all of them.
							.getConceptSequenceForUuids(vuidToSubsetMap_.get(sm.getVUID()))).forEach(sc -> 
							{
								@SuppressWarnings({ "rawtypes", "unchecked" })
								Optional<DynamicSememe> ds = ((SememeChronology)sc).getLatestVersion(DynamicSememe.class, readCoordinate_);
								if (ds.isPresent())
								{
									@SuppressWarnings({ "unchecked", "unused" })
									MutableDynamicSememe<?> mds = ((SememeChronology<DynamicSememe<?>>)sc).createMutableVersion(MutableDynamicSememe.class, 
										sm.isActive() ? State.ACTIVE : State.INACTIVE, editCoordinate_);
									importUtil_.storeManualUpdate(sc);
								}
							});
						break;
					default :
						throw new RuntimeException("Unexpected Error");
					
				}
			}
		}
	}

	/**
	 * @param properties
	 */
	private void loadDesignationProperties(ComponentReference designationSememe, Properties properties)
	{
		if (properties != null)
		{
			for (PropertyType p : properties.getProperty())
			{
				handleProperty(designationSememe, p.getTypeName(),  p.getValueOld(),  p.getValueNew(),  p.isActive(),  p.getAction());
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void loadRelationships(ComponentReference concept, Relationships relationships)
	{
		if (relationships != null)
		{
			HashSet<UUID> gatheredisA = new HashSet<>();
			HashSet<UUID> retireIsA = new HashSet<>();
			LogicalExpressionBuilder leb = Get.logicalExpressionBuilderService().getLogicalExpressionBuilder();
			for (Relationship r : relationships.getRelationship())
			{
				Optional<UUID> newTarget = StringUtils.isBlank(r.getNewTargetCode()) ? Optional.empty() : findConcept(r.getNewTargetCode());
				
				switch(r.getAction())
				{
					case ADD:
						importUtil_.addAssociation(concept, null, newTarget.get(), associations_.getProperty(r.getTypeName()).getUUID(), 
							r.isActive() ? State.ACTIVE : State.INACTIVE, null, null);
						if (r.getTypeName().equals("has_parent") && r.isActive())
						{
							gatheredisA.add(newTarget.get());
						}
						break;
					case NONE:
						//noop
						break;
					case REMOVE:
						// REMOVE directive takes precedence. Explicitely set active=false, and fall-through
						r.setActive(false);
					case UPDATE:
						Optional<UUID> oldTarget = findConcept(r.getOldTargetCode());
						UUID existingAssociation = findAssociationSememe(concept.getPrimordialUuid(), associations_.getProperty(r.getTypeName()).getUUID(), 
							oldTarget.get()).get();
						
						@SuppressWarnings("rawtypes")
						SememeChronology sc = Get.sememeService().getSememe(Get.identifierService().getSememeSequenceForUuids(existingAssociation));
						
						@SuppressWarnings({ "rawtypes"})
						Optional<DynamicSememe> ds = sc.getLatestVersion(DynamicSememe.class, readCoordinate_);
						if (ds.isPresent())
						{
							MutableDynamicSememe<?> mds = ((SememeChronology<DynamicSememe<?>>)sc).createMutableVersion(MutableDynamicSememe.class, 
									r.isActive() ? State.ACTIVE : State.INACTIVE, editCoordinate_);
							mds.setData(new DynamicSememeData[] {new DynamicSememeUUIDImpl(newTarget.isPresent() ? newTarget.get() : oldTarget.get())});
							importUtil_.storeManualUpdate(sc);
						}
						else
						{
							throw new RuntimeException("Couldn't find existing association for " + r.getTypeName() + " " + r.getNewTargetCode());
						}
						
						if (r.getTypeName().equals("has_parent"))
						{
							if (r.isActive())
							{
								UUID xTarget = newTarget.isPresent() ? newTarget.get() : oldTarget.get();
								gatheredisA.add(xTarget);
							}
							else
							{
								retireIsA.add(oldTarget.get());
								if (newTarget.isPresent())
								{
									retireIsA.add(newTarget.get());
								}
							}
						}
						
						break;
					default :
						throw new RuntimeException("Unexpected error");
				}
			}
			
			Optional<SememeChronology<? extends SememeVersion<?>>> logicGraph = Frills.getStatedDefinitionChronology(concept.getNid(), logicReadCoordinate_);
			
			LogicalExpression le = null;			
			if (logicGraph.isPresent())
			{
				ArrayList<ConceptAssertion> cas = new ArrayList<>(gatheredisA.size());
				
				if (gatheredisA.size() > 0)
				{
					for (UUID uuid : gatheredisA)
					{
						cas.add(ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(uuid), leb));
					}
				}
				
				Get.taxonomyService().getTaxonomyParentSequences(concept.getNid(), new TaxonomyCoordinateImpl(PremiseType.STATED, readCoordinate_, 
					LanguageCoordinates.getUsEnglishLanguagePreferredTermCoordinate(), logicReadCoordinate_)).forEach(parent ->
					{
						UUID potential = Get.identifierService().getUuidPrimordialFromConceptId(parent).get();
						
						if (!gatheredisA.contains(potential) && !retireIsA.contains(potential))
						{
							//Nothing was said about this pre-existing parent, so keep it.
							cas.add(ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(potential), leb));
						}
					});
				
				if (cas.size() > 0)
				{
					NecessarySet(And(cas.toArray(new Assertion[gatheredisA.size()])));
					
					le = leb.build();
					
					MutableLogicGraphSememe<?> mlgs = ((SememeChronology<LogicGraphSememe<?>>)logicGraph.get()).
						createMutableVersion(MutableLogicGraphSememe.class, State.ACTIVE, editCoordinate_);
					
					mlgs.setGraphData(le.getData(DataTarget.INTERNAL));
				}
				else
				{
					//If we ended up with nothing active, just read the current, and set the entire thing to inactive.
					MutableLogicGraphSememe<?> mlgs = ((SememeChronology<LogicGraphSememe<?>>)logicGraph.get()).
						createMutableVersion(MutableLogicGraphSememe.class, State.INACTIVE, editCoordinate_);
					mlgs.setGraphData(Frills.getLogicGraphVersion((SememeChronology<? extends LogicGraphSememe<?>>) logicGraph.get(), readCoordinate_)
						.get().value().getGraphData());
				}
				
				importUtil_.storeManualUpdate(logicGraph.get());
			}
			else
			{
				if (gatheredisA.size() > 0)
				{
					ArrayList<ConceptAssertion> cas = new ArrayList<>(gatheredisA.size());
					
					for (UUID uuid : gatheredisA)
					{
						cas.add(ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(uuid), leb));
					}
					
					NecessarySet(And(cas.toArray(new Assertion[gatheredisA.size()])));
					le = leb.build();
					importUtil_.addRelationshipGraph(concept, null, le, true, null,  null, VHATConstants.VHAT_HAS_PARENT_ASSOCIATION_TYPE.getPrimordialUuid());
				}
			}
		}	
	}
	
	private void loadMapSets(MapSets mapsets) throws IOException {
		
		if (mapsets != null)
		{
			for (MapSet ms : mapsets.getMapSet())
			{
				//before defining the columns, we need to determine if this mapset makes use of gem flags
				boolean mapSetDefinitionHasGemFlag = false;
				if (ms.getMapEntries() != null && ms.getMapEntries().getMapEntry() != null)
				for (MapEntry me : ms.getMapEntries().getMapEntry())
				{
					if (mapSetDefinitionHasGemFlag)
					{
						break;
					}
					if (me.getProperties() != null && me.getProperties().getProperty() != null)
					{
						for (Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry.Properties.Property mapItemProperty :
							me.getProperties().getProperty())
						{
							if (mapItemProperty.getTypeName().equals("GEM_Flags"))
							{
								mapSetDefinitionHasGemFlag = true;
								break;
							}
						}
					}
				}
				
				String mapSetVuid;
				
				ComponentReference concept = null;
				switch (ms.getAction())
				{
					case ADD:
						mapSetVuid = ms.getVUID() == null ? 
								(vuidSupplier_ == null ? null : vuidSupplier_.getAsLong() + "")
								: ms.getVUID().toString();
						
						String code = StringUtils.isBlank(ms.getCode()) ? mapSetVuid : ms.getCode();
						
						if (StringUtils.isBlank(code))
						{
							throw new RuntimeException("No code supplied, and vuid generation is disabled!");
						}
						concept = ComponentReference.fromConcept(importUtil_.createConcept(createNewConceptUuid(code), null, 
										ms.isActive() ? State.ACTIVE : State.INACTIVE, null));
								
						if (StringUtils.isNotBlank(mapSetVuid))
						{
							importUtil_.addStaticStringAnnotation(concept, mapSetVuid, MetaData.VUID.getPrimordialUuid(), State.ACTIVE);
						}
						
						if (StringUtils.isNotBlank(code))
						{
							importUtil_.addStaticStringAnnotation(concept, code, MetaData.CODE.getPrimordialUuid(), State.ACTIVE);
						}
						
						importUtil_.addAssociation(concept, null, subsets_.getPropertyTypeUUID(), associations_.getProperty("has_parent").getUUID(), 
								State.ACTIVE, null, null);
						importUtil_.addAssociation(concept, null, subsets_.getAltMetaDataParentUUID(), associations_.getProperty("has_parent").getUUID(), 
								State.ACTIVE, null, null);
						importUtil_.addAssociation(concept, null, IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getPrimordialUuid(), 
								associations_.getProperty("has_parent").getUUID(), State.ACTIVE, null, null);
						
						LogicalExpressionBuilder leb = Get.logicalExpressionBuilderService().getLogicalExpressionBuilder();
						
						NecessarySet(And(new Assertion[] {
								ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(subsets_.getAltMetaDataParentUUID()), leb),
								ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(subsets_.getPropertyTypeUUID()), leb),
								ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(
										IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getPrimordialUuid()), leb)}));

						LogicalExpression le = leb.build();
						
						importUtil_.addRelationshipGraph(concept, null, le, true, null,  null);
						
						DynamicSememeColumnInfo[] columns = new DynamicSememeColumnInfo[mapSetDefinitionHasGemFlag ? 6 : 5];
						int col = 0;
						columns[col] = new DynamicSememeColumnInfo(col++, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT.getUUID(),
								DynamicSememeDataType.UUID, null, false, DynamicSememeValidatorType.COMPONENT_TYPE,
								new DynamicSememeArrayImpl<>(new DynamicSememeString[] { new DynamicSememeStringImpl(ObjectChronologyType.CONCEPT.name()) }), true);
						columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_EQUIVALENCE_TYPE.getUUID(), DynamicSememeDataType.UUID,
								null, false, DynamicSememeValidatorType.IS_KIND_OF, new DynamicSememeUUIDImpl(IsaacMappingConstants.get().MAPPING_EQUIVALENCE_TYPES.getUUID()), true);
						columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_SEQUENCE.getUUID(), DynamicSememeDataType.INTEGER,
								null, false, true);
						columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_GROUPING.getUUID(), DynamicSememeDataType.LONG,
								null, false, true);
						columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_EFFECTIVE_DATE.getUUID(),
								DynamicSememeDataType.LONG, null, false, true);
						//moved to end - make it more convenient for GUI where target and qualifier are extracted, and used elsewhere - its convenient not to have the order change.
						if (mapSetDefinitionHasGemFlag)
						{
							columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_GEM_FLAGS.getUUID(),
									DynamicSememeDataType.STRING, null, false, true);
						}
						
						importUtil_.configureConceptAsDynamicRefex(concept, ms.getName(), columns, ObjectChronologyType.CONCEPT, null);
						
						//Annotate this concept as a mapset definition concept.
						importUtil_.addAnnotation(concept, null, null, IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getUUID(), State.ACTIVE, null);
						
						//Now that we have defined the map sememe, add the other annotations onto the map set definition.
						if (StringUtils.isNotBlank(ms.getSourceCodeSystem()))
						{
							importUtil_.addAnnotation(concept, null, 
								new DynamicSememeData[] {
										new DynamicSememeNidImpl(IsaacMappingConstants.get().MAPPING_SOURCE_CODE_SYSTEM.getNid()),
										new DynamicSememeStringImpl(ms.getSourceCodeSystem())},
								IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getPrimordialUuid(), State.ACTIVE, null, null);
						}
						
						if (StringUtils.isNotBlank(ms.getSourceVersionName()))
						{
							importUtil_.addAnnotation(concept, null, 
								new DynamicSememeData[] {
										new DynamicSememeNidImpl(IsaacMappingConstants.get().MAPPING_SOURCE_CODE_SYSTEM_VERSION.getNid()),
										new DynamicSememeStringImpl(ms.getSourceVersionName())},
								IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getPrimordialUuid(), State.ACTIVE, null, null);
						}
						
						if (StringUtils.isNotBlank(ms.getTargetCodeSystem()))
						{
							importUtil_.addAnnotation(concept, null, 
								new DynamicSememeData[] {
										new DynamicSememeNidImpl(IsaacMappingConstants.get().MAPPING_TARGET_CODE_SYSTEM.getNid()),
										new DynamicSememeStringImpl(ms.getTargetCodeSystem())},
								IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getPrimordialUuid(), State.ACTIVE, null, null);
						}
						
						if (StringUtils.isNotBlank(ms.getTargetVersionName()))
						{
							importUtil_.addAnnotation(concept, null, 
								new DynamicSememeData[] {
										new DynamicSememeNidImpl(IsaacMappingConstants.get().MAPPING_TARGET_CODE_SYSTEM_VERSION.getNid()),
										new DynamicSememeStringImpl(ms.getTargetVersionName())},
								IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getPrimordialUuid(), State.ACTIVE, null, null);
						}
						break;
					case NONE:
						//noop
						break;
					case REMOVE:
						concept = ComponentReference.fromConcept(importUtil_.createConcept(findConcept(ms.getCode()).get(), null, State.INACTIVE, null));
						for (ObjectChronology<?> o : recursiveRetireNested(concept.getPrimordialUuid()))
						{
							importUtil_.storeManualUpdate(o);
						}
						break;
					case UPDATE:
						//We could, potentially support updating vuid, but the current system doesn't.
						//Also, source / target stuff, but leaving as unhandled, for now.
						//so we only process activate / inactivate changes here.
						concept = ComponentReference.fromConcept(importUtil_.createConcept(findConcept(ms.getCode()).get(), null, 
							ms.isActive() ? State.ACTIVE : State.INACTIVE, null));
						break;
					default :
						throw new RuntimeException("Unexpected error");
				}
				if (concept == null)
				{
					concept = ComponentReference.fromConcept(findConcept(ms.getCode()).get());
				}
				loadDesignations(concept, ms.getDesignations());
				loadConceptProperties(concept, ms.getProperties());
				
				for (MapEntry me : ms.getMapEntries().getMapEntry())
				{
					if (me.getAction() == ActionType.ADD || me.getAction() == ActionType.UPDATE)
					{
						String gemFlag = null;
						if (me.getProperties() != null && me.getProperties().getProperty() != null)
						{
							for (Terminology.CodeSystem.Version.MapSets.MapSet.MapEntries.MapEntry.Properties.Property property : me.getProperties().getProperty())
							{
								if (property.getTypeName().equals("GEM_Flags"))
								{
									gemFlag = property.getValueNew();
									break;
								}
							}
						}
						
						DynamicSememeData[] columnData = new DynamicSememeData[mapSetDefinitionHasGemFlag ? 6 : 5];
						int col = 0;
						columnData[col++] = new DynamicSememeUUIDImpl(findConcept(me.getTargetCode()).get());
						columnData[col++] = null;  //qualifier column
						columnData[col++] = new DynamicSememeIntegerImpl(me.getSequence()); //sequence column
						columnData[col++] = me.getGrouping() != null ? new DynamicSememeLongImpl(me.getGrouping()) : null; //grouping column
						columnData[col++] = me.getEffectiveDate() != null ? new DynamicSememeLongImpl(me.getEffectiveDate().toGregorianCalendar().getTimeInMillis()) : null; //effectiveDate
						if (mapSetDefinitionHasGemFlag )
						{
							columnData[col++] = gemFlag == null ? null : new DynamicSememeStringImpl(gemFlag);
						}
						
						if (me.getAction() == ActionType.ADD)
						{
							String mapEntryVuid = me.getVUID() == null ? vuidSupplier_.getAsLong() + "" : me.getVUID().toString();
							
							SememeChronology<DynamicSememe<?>> association = importUtil_.addAnnotation(ComponentReference.fromConcept(findConcept(me.getSourceCode()).get()), 
									createNewMapItemUUID(concept.getPrimordialUuid(),mapEntryVuid), columnData, concept.getPrimordialUuid(), 
									me.isActive() ? State.ACTIVE : State.INACTIVE, null, null);
							
							importUtil_.addStaticStringAnnotation(ComponentReference.fromChronology(association, () -> "Association"), 
									mapEntryVuid, annotations_.getProperty("VUID").getUUID(), State.ACTIVE);
						}
						else
						{
							SememeChronology<?> sc = Get.sememeService().getSememe(Get.identifierService().getSememeSequenceForUuids(
									createNewMapItemUUID(concept.getPrimordialUuid(), me.getVUID().toString())));
							
							@SuppressWarnings("unchecked") 
							MutableDynamicSememe<?> mds = ((SememeChronology<DynamicSememe<?>>)sc)
									.createMutableVersion(MutableDynamicSememe.class, me.isActive() ? State.ACTIVE : State.INACTIVE, editCoordinate_);
							mds.setData(columnData);
							importUtil_.storeManualUpdate(sc);

						}
					}
					if (me.getAction() == ActionType.REMOVE)
					{
						try
						{
							ObjectChronology<?> oc = Frills.resetStateWithNoCommit(State.INACTIVE, Get.identifierService().getNidForUuids(
									createNewMapItemUUID(concept.getPrimordialUuid(), me.getVUID().toString())), editCoordinate_, readCoordinate_);
							importUtil_.storeManualUpdate(oc);
						}
						catch (Exception e)
						{
							throw new IOException(e);
						}
					}
				}
			}
		}
	}
}


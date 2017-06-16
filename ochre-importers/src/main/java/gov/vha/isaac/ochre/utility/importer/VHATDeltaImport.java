package gov.vha.isaac.ochre.utility.importer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;
import gov.va.med.term.vhat.xml.model.ActionType;
import gov.va.med.term.vhat.xml.model.PropertyType;
import gov.va.med.term.vhat.xml.model.Terminology;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.Properties;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Designations.Designation.SubsetMemberships.SubsetMembership;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties.Property;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships;
import gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Relationships.Relationship;
import gov.va.med.term.vhat.xml.model.Terminology.Types.Type;
import gov.va.oia.terminology.converters.sharedUtils.ConverterBaseMojo;
import gov.va.oia.terminology.converters.sharedUtils.IBDFCreationUtility;

/**
 * Goal which converts VHAT data into the workbench jbin format
 */
public class VHATDeltaImport extends ConverterBaseMojo
{
	private IBDFCreationUtility importUtil_;
	private Map<String, UUID> extendedDescriptionTypeNameMap = new HashMap<>();
	private Map<String, UUID> propertyTypeNameMap = new HashMap<>();
	
	private static final Logger LOG = LogManager.getLogger();
	
	public VHATDeltaImport(String xmlData, UUID author, UUID module, UUID path, File debugOutputFolder) throws IOException
	{
		try
		{
			schemaValidate(xmlData);
		}
		catch (SAXException | IOException e)
		{
			LOG.info("Submitted xml data failed schema validation", e);
			throw new IOException("The provided XML data failed Schema Validation.  Details: " + e.toString());
		}
		Terminology terminology;
		
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(Terminology.class);

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			terminology = (Terminology) jaxbUnmarshaller.unmarshal(new StringReader(xmlData));
		}
		catch (JAXBException e)
		{
			LOG.error("Unexpected error parsing submitted VETs XML.", e);
			throw new IOException("Unexpected error parsing the xml.  Details: " + e.toString());
		}
		
		LOG.info("XML Parsed");
		
		extendedDescriptionTypeNameMap.put("abbreviation", UUID.fromString("630759bf-98e0-5548-ab51-4ac4b155802c"));
		extendedDescriptionTypeNameMap.put("fully specified name", UUID.fromString("97f32713-bdd2-5885-bc88-0d5298ab4b52"));
		extendedDescriptionTypeNameMap.put("preferred name", UUID.fromString("a20e5175-6257-516a-a97d-d7f9655916b8"));
		extendedDescriptionTypeNameMap.put("synonym", UUID.fromString("11af6808-1ee9-5571-a169-0dac0c74c579"));
		extendedDescriptionTypeNameMap.put("vista name", UUID.fromString("72518b09-d5dd-5af0-bd41-0c7e46dfe85e"));
		
		headerCheck(terminology);
			
		vuidCheck(terminology);
			
		propertyCheck(terminology);
		
		requiredChecks(terminology);
		
		try
		{
			importUtil_ = new IBDFCreationUtility(author, module, path, debugOutputFolder);
		}
		catch (Exception e)
		{
			throw new IOException("Unexpected error setting up", e);
		}
		
		loadConcepts(terminology.getCodeSystem().getVersion().getCodedConcepts());
		
		System.out.println("Load complete!");
		
	}

	private void schemaValidate(String xmlData) throws SAXException, IOException
	{
		LOG.info("Doing schema validation");
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		
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
		//TODO implement - make sure any supplied VUID on a new item is not in use
	}
	
	private void propertyCheck(Terminology terminology)
	{
		LOG.info("Checking for properties that need creation");
		//TODO implement - make sure we have properties for all properties supplied and/or build any new properties (how are new ones denoted?))
		if (terminology.getTypes() != null)
		{
			for (Type t : terminology.getTypes().getType())
			{
				switch (t.getKind())
				{
					case DESIGNATION_TYPE:
						break;
					case PROPERTY_TYPE:
						break;
					case RELATIONSHIP_TYPE:
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
		
		// TODO ever need to handle preferred designation type?
		
		
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
					if (cc.getAction() == null)
					{
						throw new IOException("Action must be provided on every concept.  Missing on " + cc.getCode());
					}
					if (cc.isActive() == null)
					{
						throw new IOException("Active must be provided on every concept.  Missing on " + cc.getCode());
					}
					if (cc.getDesignations() != null)
					{
						for (Designation d : cc.getDesignations().getDesignation())
						{
							if (d.getAction() == null)
							{
								throw new IOException("Action must be provided on every designation.  Missing on " + cc.getCode() + ":" + d.getCode());
							}
							if (d.isActive() == null)
							{
								throw new IOException("Active must be provided on every designation.  Missing on " + cc.getCode() + ":" + d.getCode());
							}
							if (StringUtils.isNotBlank(d.getTypeName()))
							{
								if (extendedDescriptionTypeNameMap.get(d.getTypeName().toLowerCase()) == null)
								{
									throw new IOException("Unexpected TypeName on " + cc.getCode() + ":" + d.getCode() + ": " + d.getTypeName());
								}
							}
							else
							{
								//type is required on add
								if (d.getAction() == ActionType.ADD)
								{
									throw new IOException("Missing  TypeName on " + cc.getCode() + ":" + d.getCode());
								}
							}
							if (d.getProperties() != null)
							{
								for ( PropertyType p : d.getProperties().getProperty())
								{
									if (p.getAction() == null)
									{
										throw new IOException("Action must be provided on every property.  Missing on " + cc.getCode() + ":" 
											+ d.getCode() + ":" + p.getTypeName());
									}
									if (p.isActive() == null)
									{
										throw new IOException("Active must be provided on every property.  Missing on " + cc.getCode() + ":" 
											+ d.getCode() + ":" + p.getTypeName());
									}
								}
							}
							
							if (d.getSubsetMemberships() != null)
							{
								for (SubsetMembership sm : d.getSubsetMemberships().getSubsetMembership())
								{
									if (sm.getAction() == null)
									{
										throw new IOException("Action must be provided on every subset membership.  Missing on " + cc.getCode() + ":" 
											+ d.getCode() + ":" + sm.getVUID());
									}
									if (sm.isActive() == null)
									{
										throw new IOException("Active must be provided on every subset membership.  Missing on " + cc.getCode() + ":" 
											+ d.getCode() + ":" + sm.getVUID());
									}
								}
							}
						}
					}
					
					if (cc.getProperties() != null)
					{
						for (Property p : cc.getProperties().getProperty())
						{
							if (p.getAction() == null)
							{
								throw new IOException("Action must be provided on every property.  Missing on " + cc.getCode() + ":" 
									+ p.getTypeName());
							}
							if (p.isActive() == null)
							{
								throw new IOException("Active must be provided on every property.  Missing on " + cc.getCode() + ":" 
									+ p.getTypeName());
							}
						}
					}
					
					if (cc.getRelationships() != null)
					{
						for (Relationship r : cc.getRelationships().getRelationship())
						{
							if (r.getAction() == null)
							{
								throw new IOException("Action must be provided on every relationship.  Missing on " + cc.getCode() + ":" 
									+ r.getTypeName());
							}
							if (r.isActive() == null)
							{
								throw new IOException("Active must be provided on every relationship.  Missing on " + cc.getCode() + ":" 
									+ r.getTypeName());
							}
						}
					}
					
				}
			}
		}
	}
	
	private void loadConcepts(CodedConcepts codedConcepts) throws IOException
	{
		LOG.info("Loading "  + codedConcepts.getCodedConcept().size() + " Concepts");
		
		for (CodedConcept cc : codedConcepts.getCodedConcept())
		{
			
			switch (cc.getAction())
			{
				//TODO concepts
				case ADD:
					
					loadDesignations(cc.getDesignations());
					break;
				case NONE:
					break;
				case REMOVE:
					break;
				case UPDATE:
					break;
				default :
					throw new RuntimeException("Unexpected error");
				
			}
			loadConceptProperties(cc.getProperties());
			loadRelationships(cc.getRelationships());
		}
	}


	/**
	 * @param properties
	 */
	private void loadConceptProperties(gov.va.med.term.vhat.xml.model.Terminology.CodeSystem.Version.CodedConcepts.CodedConcept.Properties properties)
	{
		//TODO concept properties
		if (properties != null)
		{
			for (Property p : properties.getProperty())
			{
				switch (p.getAction())
				{
					case ADD:
						break;
					case NONE:
						break;
					case REMOVE:
						break;
					case UPDATE:
						break;
					default :
						throw new RuntimeException("Unexepected error");
					
				}
			}
		}
		
	}

	/**
	 * @param designations
	 */
	private void loadDesignations(Designations designations)
	{
		// TODO concept designations
		for (Designation d : designations.getDesignation())
		{
			switch (d.getAction())
			{
				case ADD:
					loadDesignationProperties(d.getProperties());
					loadSubsetMembership(d.getSubsetMemberships());
					break;
				case NONE:
					break;
				case REMOVE:
					break;
				case UPDATE:
					break;
				default :
					throw new RuntimeException("Unexpected error");
				
			}
		}
		
	}

	/**
	 * @param subsetMemberships
	 */
	private void loadSubsetMembership(SubsetMemberships subsetMemberships)
	{
		//TODO subset memberships
		if (subsetMemberships != null)
		{
			for(SubsetMembership sm : subsetMemberships.getSubsetMembership())
			{
				switch (sm.getAction())
				{
					case ADD:
						break;
					case NONE:
						break;
					case REMOVE:
						break;
					case UPDATE:
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
	private void loadDesignationProperties(Properties properties)
	{
		if (properties != null)
		{
			//TODO designation properties
			for (PropertyType p : properties.getProperty())
			{
				switch(p.getAction())
				{
					case ADD:
						break;
					case NONE:
						break;
					case REMOVE:
						break;
					case UPDATE:
						break;
					default :
						throw new RuntimeException("Unexpected error");
					
				}
			}
		}
	}
	

	private void loadRelationships(Relationships relationships)
	{
		if (relationships != null)
		{
			for (Relationship r : relationships.getRelationship())
			{
				switch(r.getAction())
				{
					//TODO relationships
					case ADD:
						break;
					case NONE:
						break;
					case REMOVE:
						break;
					case UPDATE:
						break;
					default :
						throw new RuntimeException("Unexpected error");
					
				}
			}
		}
		
	}
	
	

//	@Override
//	public void execute() throws MojoExecutionException
//	{
//
//		try
//		{
//			super.execute();
//
//			String temp = converterOutputArtifactVersion.substring(0, 11);
//
//			importUtil_ = new IBDFCreationUtility(Optional.of("VHAT " + converterSourceArtifactVersion), Optional.of(MetaData.VHAT_MODULES), outputDirectory, 
//					converterOutputArtifactId, converterOutputArtifactVersion, converterOutputArtifactClassifier, false, new SimpleDateFormat("yyyy.MM.dd").parse(temp).getTime());
//			
//			attributes_ = new PT_Annotations();
//			descriptions_ = new BPT_Descriptions(VHATConstants.TERMINOLOGY_NAME);
//			associations_ = new BPT_Associations(VHATConstants.TERMINOLOGY_NAME);
//			relationships_ = new BPT_Relations(VHATConstants.TERMINOLOGY_NAME);
//			refsets_ = new BPT_Refsets(VHATConstants.TERMINOLOGY_NAME);
//			refsets_.addProperty("All VHAT Concepts");
//
//			TerminologyDataReader importer = new TerminologyDataReader(inputFileLocation);
//			TerminologyDTO terminology = importer.process();
//
//			List<TypeImportDTO> dto = terminology.getTypes();
//
//			ComponentReference vhatMetadata = ComponentReference.fromConcept(createType(MetaData.SOLOR_CONTENT_METADATA.getPrimordialUuid(), 
//					"VHAT Metadata" + IBDFCreationUtility.metadataSemanticTag_));
//			
//			importUtil_.loadTerminologyMetadataAttributes(converterSourceArtifactVersion, 
//					Optional.empty(), converterOutputArtifactVersion, Optional.ofNullable(converterOutputArtifactClassifier), converterVersion);
//			
//			//TODO would be nice to automate this
//			importUtil_.registerDynamicSememeColumnInfo(IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_NID_EXTENSION.getUUID(), 
//					IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_NID_EXTENSION.getDynamicSememeColumns());
//			importUtil_.registerDynamicSememeColumnInfo(IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getUUID(), 
//					IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getDynamicSememeColumns());
//			importUtil_.registerDynamicSememeColumnInfo(IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getUUID(), 
//					IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getDynamicSememeColumns());
//
//			// read in the dynamic types
//			for (TypeImportDTO typeImportDTO : dto)
//			{
//				if (typeImportDTO.getKind().equals("DesignationType"))
//				{
//					Property p = descriptions_.addProperty(typeImportDTO.getName());
//					//Add some rankings for FSN / synonym handling
//					if (p.getSourcePropertyNameFSN().equals("Fully Specified Name"))
//					{
//						p.setPropertySubType(BPT_Descriptions.FSN);
//					}
//					else if (p.getSourcePropertyNameFSN().equals("Preferred Name"))
//					{
//						p.setPropertySubType(BPT_Descriptions.SYNONYM);
//					}
//					else if (p.getSourcePropertyNameFSN().equals("Synonym"))
//					{
//						p.setPropertySubType(BPT_Descriptions.SYNONYM + 1);
//					}
//				}
//				else if (typeImportDTO.getKind().equals("RelationshipType"))
//				{
//					//currently loading isA as a graph rel, and an assoiation
//					if (typeImportDTO.getName().equals("has_parent"))
//					{
//						Property p = relationships_.addProperty(typeImportDTO.getName());
//						p.setWBPropertyType(MetaData.IS_A.getPrimordialUuid());
//					}
//					associations_.addProperty(new PropertyAssociation(associations_, typeImportDTO.getName(), typeImportDTO.getName(), null, 
//							typeImportDTO.getName(), false));
//				}
//				else if (typeImportDTO.getKind().equals("PropertyType"))
//				{
//					if (typeImportDTO.getName().equals("GEM_Flags"))
//					{
//						//skip - GEM_Flags are loaded as a column on a mapset instead.
//					}
//					else
//					{
//						attributes_.addProperty(typeImportDTO.getName());
//					}
//				}
//				else
//				{
//					System.err.println("Unexpected Type!");
//				}
//			}
//			
//			//get the refset names
//			for (SubsetImportDTO subset : terminology.getSubsets())
//			{
//				refsets_.addProperty(subset.getSubsetName());
//			}
//			
//			importUtil_.loadMetaDataItems(Arrays.asList(descriptions_, attributes_, associations_, relationships_, refsets_), vhatMetadata.getPrimordialUuid());
//
//			ConsoleUtil.println("Metadata load stats");
//			for (String line : importUtil_.getLoadStats().getSummary())
//			{
//				ConsoleUtil.println(line);
//			}
//			
//			importUtil_.clearLoadStats();
//
//			allVhatConceptsRefset = refsets_.getProperty("All VHAT Concepts").getUUID();
//			loadedConcepts.put(allVhatConceptsRefset, "All VHAT Concepts");
//
//			Map<Long, Set<String>> subsetMembershipMap = new HashMap<>();
//			// get the subset memberships to build the refset for each subset
//			for (ConceptImportDTO item : terminology.getCodeSystem().getVersion().getConcepts())
//			{
//				for (DesignationExtendedImportDTO designation : item.getDesignations())
//				{
//					for (SubsetMembershipImportDTO subsetMembership : designation.getSubsets())
//					{
//						Set<String> codes = subsetMembershipMap.get(subsetMembership.getVuid());
//						if (codes == null)
//						{
//							codes = new HashSet<>();
//							subsetMembershipMap.put(subsetMembership.getVuid(), codes);
//						}
//						codes.add(designation.getCode());
//					}
//				}
//			}
//			
//			for (SubsetImportDTO subset : terminology.getSubsets())
//			{
//				loadRefset(subset.getSubsetName(), subset.getVuid(), subsetMembershipMap.get(subset.getVuid()));
//			}
//
//			//TODO use the codesystem version info?
//			//TODO use the Version info?
//			//
//
//			for (ConceptImportDTO item : terminology.getCodeSystem().getVersion().getConcepts())
//			{
//				conceptCount++;
//				writeEConcept(item);
//				if (conceptCount % 500 == 0)
//				{
//					ConsoleUtil.showProgress();
//				}
//				if (conceptCount % 10000 == 0)
//				{
//					ConsoleUtil.println("Processed " + conceptCount + " concepts");
//				}
//			}
//			
//			ConsoleUtil.println("Processed " + conceptCount + " concepts");
//			ConsoleUtil.println("Starting mapsets");
//			
//
//			for (MapSetImportDTO item : terminology.getCodeSystem().getVersion().getMapsets())
//			{
//				mapSetCount++;
//				writeEConcept(item);
//				if (mapEntryCount % 100 == 0)
//				{
//					ConsoleUtil.showProgress();
//				}
//				if (mapEntryCount % 500 == 0)
//				{
//					ConsoleUtil.println("Processed " + mapSetCount + " mapsets with " + mapEntryCount + " members");
//				}
//			}
//			
//			ConsoleUtil.println("Processed " + mapSetCount + " mapsets with " + mapEntryCount + " members");
//
//			ArrayList<UUID> missingConcepts = new ArrayList<>();
//
//			for (UUID refUUID : referencedConcepts.keySet())
//			{
//				if (loadedConcepts.get(refUUID) == null)
//				{
//					missingConcepts.add(refUUID);
//					ConsoleUtil.printErrorln("Data error - The concept " + refUUID + " - " + referencedConcepts.get(refUUID)
//							+ " was referenced, but not loaded - will be created as '-MISSING-'");
//				}
//			}
//
//			if (missingConcepts.size() > 0)
//			{
//				ConceptChronology<? extends ConceptVersion<?>> missingParent = importUtil_.createConcept("Missing Concepts", true);
//				importUtil_.addParent(ComponentReference.fromChronology(missingParent), rootConceptUUID);
//				for (UUID refUUID : missingConcepts)
//				{
//					ComponentReference c = ComponentReference.fromChronology(importUtil_.createConcept(refUUID, "-MISSING-", true));
//					importUtil_.addParent(c, missingParent.getPrimordialUuid());
//				}
//			}
//			
//			//Handle missing association sources and targets
//			ConsoleUtil.println("Creating placeholder concepts for " + associationOrphanConcepts.size() + " association orphans");
//			//We currently don't have these association targets, so need to invent placeholder concepts.
//			ComponentReference missingSDORefset = ComponentReference.fromConcept(importUtil_.createConcept(null, "Missing SDO Code System Concepts", 
//					null, null, null, refsets_.getPropertyTypeUUID(), (UUID)null));
//			importUtil_.configureConceptAsDynamicRefex(missingSDORefset, "A simple refset to store the missing concepts we have to create during import because"
//					+ " we don't yet have the SDO code systems in place",
//					null, ObjectChronologyType.CONCEPT, null);
//			for (Entry<UUID, String> item : associationOrphanConcepts.entrySet())
//			{
//				if (loadedConcepts.get(item.getKey()) == null)
//				{
//					importUtil_.addRefsetMembership(ComponentReference.fromConcept(importUtil_.createConcept(item.getKey(), item.getValue(), true)), 
//							missingSDORefset.getPrimordialUuid(), State.ACTIVE, null);
//				}
//			}
//			
//
//			// Put in names instead of IDs so the load stats print nicer:
//			Hashtable<String, String> stringsToSwap = new Hashtable<String, String>();
//			for (SubsetImportDTO subset : terminology.getSubsets())
//			{
//				stringsToSwap.put(subset.getVuid() + "", subset.getSubsetName());
//			}
//			
//			for (MapSetImportDTO mapSet : terminology.getCodeSystem().getVersion().getMapsets())
//			{
//				stringsToSwap.put(mapSet.getVuid() + "", mapSet.getName());
//			}
//			
//
//			ConsoleUtil.println("Load Statistics");
//			// swap out vuids with names to make it more readable...
//			for (String line : importUtil_.getLoadStats().getSummary())
//			{
//				Enumeration<String> e = stringsToSwap.keys();
//				while (e.hasMoreElements())
//				{
//					String current = e.nextElement();
//					line = line.replaceAll(current, stringsToSwap.get(current));
//				}
//				ConsoleUtil.println(line);
//			}
//
//			// this could be removed from final release. Just added to help debug editor problems.
//			ConsoleUtil.println("Dumping UUID Debug File");
//			ConverterUUID.dump(outputDirectory, "vhatUuid");
//
//			if (conceptsWithNoDesignations.size() > 0)
//			{
//				ConsoleUtil.printErrorln(conceptsWithNoDesignations.size() + " concepts were found with no descriptions at all.  These were assigned '-MISSING-'");
//				FileWriter fw = new FileWriter(new File(outputDirectory, "NoDesignations.txt"));
//				for (String s : conceptsWithNoDesignations)
//				{
//					fw.write(s);
//					fw.write(System.getProperty("line.separator"));
//				}
//				fw.close();
//			}
//			importUtil_.shutdown();
//			ConsoleUtil.writeOutputToFile(new File(outputDirectory, "ConsoleOutput.txt").toPath());
//		}
//		catch (Exception ex)
//		{
//			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
//		}
//
//	}
//
//	private void writeEConcept(NamedPropertiedItemImportDTO conceptOrMapSet) throws Exception
//	{
//		boolean isMapSet = false;
//		if (conceptOrMapSet instanceof MapSetImportDTO)
//		{
//			isMapSet = true;
//			mapSetCount++;
//		}
//
//		ComponentReference concept = ComponentReference.fromConcept(importUtil_.createConcept(getConceptUuid(conceptOrMapSet.getCode()), null, 
//				conceptOrMapSet.isActive() ? State.ACTIVE : State.INACTIVE, null));
//		loadedConcepts.put(concept.getPrimordialUuid(), conceptOrMapSet.getCode());
//		importUtil_.addStaticStringAnnotation(concept, conceptOrMapSet.getVuid().toString(), attributes_.getProperty("VUID").getUUID(), State.ACTIVE);
//		importUtil_.addStaticStringAnnotation(concept, conceptOrMapSet.getCode(), attributes_.getProperty("Code").getUUID(), State.ACTIVE);
//
//		ArrayList<ValuePropertyPairExtended> descriptionHolder = new ArrayList<>();
//		if (isMapSet)
//		{
//			for (DesignationImportDTO didto : ((MapSetImportDTO)conceptOrMapSet).getDesignations())
//			{
//				descriptionHolder.add(new ValuePropertyPairExtended(didto.getValueNew(), getDescriptionUuid(didto.getCode().toString()),
//						descriptions_.getProperty(didto.getTypeName()), didto, !didto.isActive()));
//			}
//		}
//		else
//		{
//			for (DesignationExtendedImportDTO didto : ((ConceptImportDTO)conceptOrMapSet).getDesignations())
//			{
//				descriptionHolder.add(new ValuePropertyPairExtended(didto.getValueNew(), getDescriptionUuid(didto.getCode().toString()),
//						descriptions_.getProperty(didto.getTypeName()), didto, !didto.isActive()));
//			}
//		}
//		
//		for (PropertyImportDTO property : conceptOrMapSet.getProperties())
//		{
//			importUtil_.addStringAnnotation(concept, property.getValueNew(), attributes_.getProperty(property.getTypeName()).getUUID(), 
//					property.isActive() ? State.ACTIVE : State.INACTIVE);
//		}
//
//		List<SememeChronology<DescriptionSememe<?>>> wbDescriptions = importUtil_.addDescriptions(concept, descriptionHolder);
//		
//		//Descriptions have now all been added to the concepts - now we need to process the rest of the ugly bits of vhat
//		//and place them on the descriptions.
//		for (int i = 0; i < descriptionHolder.size(); i++)
//		{
//			ValuePropertyPairExtended vpp = descriptionHolder.get(i);
//			SememeChronology<DescriptionSememe<?>> desc = wbDescriptions.get(i);
//			
//			if (vpp.getValue().equals(VHATConstants.TERMINOLOGY_NAME))
//			{
//				// On the root node, we need to add some extra attributes
//				importUtil_.addDescription(concept, VHATConstants.TERMINOLOGY_NAME, DescriptionType.SYNONYM, true, null, State.ACTIVE);
//				importUtil_.addDescription(concept, "VHA Terminology", DescriptionType.SYNONYM, false, null, State.ACTIVE);
//				ConsoleUtil.println("Root concept FSN is 'VHAT' and the UUID is " + concept.getPrimordialUuid());
//				importUtil_.addParent(concept, MetaData.ISAAC_ROOT.getPrimordialUuid());
//				rootConceptUUID = concept.getPrimordialUuid();
//			}
//			importUtil_.addStaticStringAnnotation(ComponentReference.fromChronology(desc, () -> "Description"), vpp.getDesignationImportDTO().getVuid() + "", attributes_.getProperty("VUID").getUUID(), State.ACTIVE);
//			importUtil_.addStaticStringAnnotation(ComponentReference.fromChronology(desc, () -> "Description"), vpp.getDesignationImportDTO().getCode(), attributes_.getProperty("Code").getUUID(), State.ACTIVE);
//
//			// VHAT is kind of odd, in that the attributes are attached to the description, rather than the concept.
//			if (!isMapSet)
//			{
//				for (PropertyImportDTO property : ((DesignationExtendedImportDTO)vpp.getDesignationImportDTO()).getProperties())
//				{
//					importUtil_.addStringAnnotation(ComponentReference.fromChronology(desc, () -> "Description"), property.getValueNew(), 
//							attributes_.getProperty(property.getTypeName()).getUUID(), property.isActive() ? State.ACTIVE : State.INACTIVE);
//				}
//			}
//		}
//		
//		if (descriptionHolder.size() == 0)
//		{
//			// Seems like a data error - but it is happening... no descriptions at all.....
//			conceptsWithNoDesignations.add(conceptOrMapSet.getCode());
//			// The workbench implodes if you don't have a fully specified name....
//			importUtil_.addDescription(concept, "-MISSING-", DescriptionType.FSN, true, null, State.ACTIVE);
//		}
//
//		List<RelationshipImportDTO> relationshipImports = conceptOrMapSet.getRelationships();
//		LogicalExpressionBuilder leb = Get.logicalExpressionBuilderService().getLogicalExpressionBuilder();
//		ArrayList<ConceptAssertion> assertions = new ArrayList<>();
//		if (relationshipImports != null)
//		{
//			for (RelationshipImportDTO relationshipImportDTO : relationshipImports)
//			{
//				UUID sourceUuid = getConceptUuid(conceptOrMapSet.getCode());
//				UUID targetUuid = getConceptUuid(relationshipImportDTO.getNewTargetCode());
//
//				referencedConcepts.put(targetUuid, relationshipImportDTO.getNewTargetCode());
//
//				if (!sourceUuid.equals(concept.getPrimordialUuid()))
//				{
//					throw new MojoExecutionException("Design failure!");
//				}
//
//				importUtil_.addAssociation(concept, null, targetUuid, associations_.getProperty(relationshipImportDTO.getTypeName()).getUUID(),
//						relationshipImportDTO.isActive() ? State.ACTIVE : State.INACTIVE, null, null);
//				
//				//If it is an isA rel, also create it as a rel.
//				
//				if (relationships_.getProperty(relationshipImportDTO.getTypeName()) != null)
//				{
//					if (relationships_.getProperty(relationshipImportDTO.getTypeName()).getWBTypeUUID().equals(MetaData.IS_A.getPrimordialUuid()))
//					{
//						assertions.add(ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(targetUuid), leb));
//					}
//					else
//					{
//						throw new RuntimeException("Only is_a is handled so far as a relationship");
//					}
//				}
//			}
//			if (assertions.size() > 0)
//			{
//				NecessarySet(And(assertions.toArray(new Assertion[assertions.size()])));
//				importUtil_.addRelationshipGraph(concept, null, leb.build(), true, null, null);  //TODO handle inactive
//			}
//		}
//
//		importUtil_.addRefsetMembership(concept, allVhatConceptsRefset, State.ACTIVE, null);
//
//		if (isMapSet)
//		{
//			//Add a relationship to the subsets metadata concept.
//			if (relationshipImports != null && relationshipImports.size() > 0)
//			{
//				throw new RuntimeException("Didn't expect mapsets to have their own relationships!");
//			}
//			
//			//add it as an association too
//			importUtil_.addAssociation(concept, null, refsets_.getPropertyTypeUUID(), associations_.getProperty("has_parent").getUUID(),
//					State.ACTIVE, null, null);
//			
//			//place it in three places - refsets under VHAT Metadata, vhat refsets under SOLOR Refsets, and the dynamic sememe mapping sememe type.
//			NecessarySet(And(new Assertion[] {
//					//ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(refsets_.getSecondParentUUID()), leb),
//					ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(refsets_.getPropertyTypeUUID()), leb),
//					ConceptAssertion(Get.identifierService().getConceptSequenceForUuids(IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getPrimordialUuid()), leb)}));
//			importUtil_.addRelationshipGraph(concept, null, leb.build(), true, null, null);
//
//			MapSetImportDTO mapSet = ((MapSetImportDTO)conceptOrMapSet);
//			
//			//before defining the columns, we need to determine if this mapset makes use of gem flags
//			boolean mapSetDefinitionHasGemFlag = false;
//			for (MapEntryImportDTO mapItem : mapSet.getMapEntries())
//			{
//				if (mapSetDefinitionHasGemFlag)
//				{
//					break;
//				}
//				for (PropertyImportDTO mapItemProperty : mapItem.getProperties())
//				{
//					if (mapItemProperty.getTypeName().equals("GEM_Flags"))
//					{
//						mapSetDefinitionHasGemFlag = true;
//						break;
//					}
//				}
//			}
//			
//			
//			DynamicSememeColumnInfo[] columns = new DynamicSememeColumnInfo[mapSetDefinitionHasGemFlag ? 6 : 5];
//			int col = 0;
//			columns[col] = new DynamicSememeColumnInfo(col++, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT.getUUID(),
//					DynamicSememeDataType.UUID, null, false, DynamicSememeValidatorType.COMPONENT_TYPE,
//					new DynamicSememeArrayImpl<>(new DynamicSememeString[] { new DynamicSememeStringImpl(ObjectChronologyType.CONCEPT.name()) }), true);
//			columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_EQUIVALENCE_TYPE.getUUID(), DynamicSememeDataType.UUID,
//					null, false, DynamicSememeValidatorType.IS_KIND_OF, new DynamicSememeUUIDImpl(IsaacMappingConstants.get().MAPPING_EQUIVALENCE_TYPES.getUUID()), true);
//			columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_SEQUENCE.getUUID(), DynamicSememeDataType.INTEGER,
//					null, false, true);
//			columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_GROUPING.getUUID(), DynamicSememeDataType.LONG,
//					null, false, true);
//			columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_EFFECTIVE_DATE.getUUID(),
//					DynamicSememeDataType.LONG, null, false, true);
//			//moved to end - make it more convenient for GUI where target and qualifier are extracted, and used elsewhere - its convenient not to have the order change.
//			if (mapSetDefinitionHasGemFlag)
//			{
//				columns[col] = new DynamicSememeColumnInfo(col++, IsaacMappingConstants.get().DYNAMIC_SEMEME_COLUMN_MAPPING_GEM_FLAGS.getUUID(),
//						DynamicSememeDataType.STRING, null, false, true);
//			}
//			
//			importUtil_.configureConceptAsDynamicRefex(concept, mapSet.getName(), columns, ObjectChronologyType.CONCEPT, null);
//			
//			//Annotate this concept as a mapset definition concept.
//			importUtil_.addAnnotation(concept, null, null, IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getUUID(), State.ACTIVE, null);
//			
//			//Now that we have defined the map sememe, add the other annotations onto the map set definition.
//			if (StringUtils.isNotBlank(mapSet.getSourceCodeSystemName()))
//			{
//				importUtil_.addAnnotation(concept, null, 
//					new DynamicSememeData[] {
//							new DynamicSememeNidImpl(IsaacMappingConstants.get().MAPPING_SOURCE_CODE_SYSTEM.getNid()),
//							new DynamicSememeStringImpl(mapSet.getSourceCodeSystemName())},
//					IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getPrimordialUuid(), State.ACTIVE, null, null);
//			}
//			
//			if (StringUtils.isNotBlank(mapSet.getSourceVersionName()))
//			{
//				importUtil_.addAnnotation(concept, null, 
//					new DynamicSememeData[] {
//							new DynamicSememeNidImpl(IsaacMappingConstants.get().MAPPING_SOURCE_CODE_SYSTEM_VERSION.getNid()),
//							new DynamicSememeStringImpl(mapSet.getSourceVersionName())},
//					IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getPrimordialUuid(), State.ACTIVE, null, null);
//			}
//			
//			if (StringUtils.isNotBlank(mapSet.getTargetCodeSystemName()))
//			{
//				importUtil_.addAnnotation(concept, null, 
//					new DynamicSememeData[] {
//							new DynamicSememeNidImpl(IsaacMappingConstants.get().MAPPING_TARGET_CODE_SYSTEM.getNid()),
//							new DynamicSememeStringImpl(mapSet.getTargetCodeSystemName())},
//					IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getPrimordialUuid(), State.ACTIVE, null, null);
//			}
//			
//			if (StringUtils.isNotBlank(mapSet.getTargetVersionName()))
//			{
//				importUtil_.addAnnotation(concept, null, 
//					new DynamicSememeData[] {
//							new DynamicSememeNidImpl(IsaacMappingConstants.get().MAPPING_TARGET_CODE_SYSTEM_VERSION.getNid()),
//							new DynamicSememeStringImpl(mapSet.getTargetVersionName())},
//					IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_STRING_EXTENSION.getPrimordialUuid(), State.ACTIVE, null, null);
//			}
//			
//			for (MapEntryImportDTO mapItem : mapSet.getMapEntries())
//			{
//				ComponentReference sourceConcept = ComponentReference.fromConcept(mapSet.getSourceCodeSystemName().equals(VHATConstants.TERMINOLOGY_NAME) ?
//						getConceptUuid(mapItem.getSourceCode()) : getAssociationOrphanUuid(mapItem.getSourceCode()));
//
//				if (!loadedConcepts.containsKey(sourceConcept.getPrimordialUuid()))
//				{
//					if (mapSet.getSourceCodeSystemName().equals(VHATConstants.TERMINOLOGY_NAME))
//					{
//						ConsoleUtil.printErrorln("Missing VHAT association source concept! " + mapItem.getSourceCode());
//					}
//					associationOrphanConcepts.put(sourceConcept.getPrimordialUuid(), mapItem.getSourceCode());
//				}
//				
//				UUID targetConcept =  mapSet.getTargetCodeSystemName().equals(VHATConstants.TERMINOLOGY_NAME) ?
//						getConceptUuid(mapItem.getTargetCode()) : getAssociationOrphanUuid(mapItem.getTargetCode());
//				
//				if (!loadedConcepts.containsKey(targetConcept))
//				{
//					if (mapSet.getTargetCodeSystemName().equals(VHATConstants.TERMINOLOGY_NAME))
//					{
//						ConsoleUtil.printErrorln("Missing VHAT association target concept! " + mapItem.getTargetCode());
//					}
//					associationOrphanConcepts.put(targetConcept, mapItem.getTargetCode());
//				}
//				
//				String gemFlag = null;
//				if (mapItem.getProperties() != null)
//				{
//					for (PropertyImportDTO property : mapItem.getProperties())
//					{
//						if (property.getTypeName().equals("GEM_Flags"))
//						{
//							if (gemFlag != null)
//							{
//								throw new RuntimeException("Didn't expect multiple gem flags on a single mapItem!");
//							}
//							gemFlag = property.getValueNew();
//						}
//					}
//				}
//				
//				DynamicSememeData[] columnData = new DynamicSememeData[mapSetDefinitionHasGemFlag ? 6 : 5];
//				col = 0;
//				columnData[col++] = new DynamicSememeUUIDImpl(targetConcept);
//				columnData[col++] = null;  //qualifier column
//				columnData[col++] = new DynamicSememeIntegerImpl(mapItem.getSequence()); //sequence column
//				columnData[col++] = mapItem.getGrouping() != null ? new DynamicSememeLongImpl(mapItem.getGrouping()) : null; //grouping column
//				columnData[col++] = mapItem.getEffectiveDate() != null ? new DynamicSememeLongImpl(mapItem.getEffectiveDate().getTime()) : null; //effectiveDate
//				if (mapSetDefinitionHasGemFlag )
//				{
//					columnData[col++] = gemFlag == null ? null : new DynamicSememeStringImpl(gemFlag);
//				}
//				
//				SememeChronology<DynamicSememe<?>> association = importUtil_.addAnnotation(sourceConcept, getMapItemUUID(mapSet.getVuid().toString(),
//						mapItem.getVuid().toString()), columnData, concept.getPrimordialUuid(), mapItem.isActive() ? State.ACTIVE : State.INACTIVE, null, null);
//				
//				importUtil_.addStaticStringAnnotation(ComponentReference.fromChronology(association, () -> "Association"), mapItem.getVuid().toString(), attributes_.getProperty("VUID").getUUID(), State.ACTIVE);
//
//				if (mapItem.getProperties() != null)
//				{
//					for (PropertyImportDTO property : mapItem.getProperties())
//					{
//						if (property.getTypeName().equals("GEM_Flags"))
//						{
//							//already handled above
//						}
//						else 
//						{
//							throw new RuntimeException("Properties on map set items not yet handled"); 
//							//This code is correct, but our gui doesn't expect this, so throw an error for now, so we know if any show up.
//						}
//					}
//				}
//				if (mapItem.getDesignations() != null && mapItem.getDesignations().size() > 0)
//				{
//					throw new RuntimeException("Designations on map set items not yet handled");
//				}
//				if (mapItem.getRelationships() != null && mapItem.getRelationships().size() > 0)
//				{
//					throw new RuntimeException("Relationships on map set items not yet handled");
//				}
//				mapEntryCount++;
//			}
//			
//		}
//	}
//
//	private ConceptChronology<? extends ConceptVersion<?>> createType(UUID parentUuid, String typeName) throws Exception
//	{
//		ConceptChronology<? extends ConceptVersion<?>> concept = importUtil_.createConcept(typeName, true);
//		loadedConcepts.put(concept.getPrimordialUuid(), typeName);
//		importUtil_.addParent(ComponentReference.fromConcept(concept), parentUuid);
//		return concept;
//	}
//
//	private void loadRefset(String typeName, Long vuid, Set<String> refsetMembership) throws Exception
//	{
//		UUID concept = refsets_.getProperty(typeName).getUUID();
//		loadedConcepts.put(concept, typeName);
//		if (vuid != null)
//		{
//			importUtil_.addStaticStringAnnotation(ComponentReference.fromConcept(concept), vuid.toString(), attributes_.getProperty("VUID").getUUID(), State.ACTIVE);
//		}
//
//		if (refsetMembership != null)
//		{
//			for (String memberCode : refsetMembership)
//			{
//				importUtil_.addRefsetMembership(ComponentReference.fromSememe(getDescriptionUuid(memberCode)), concept, State.ACTIVE, null);
//			}
//		}
//	}
//	
//	private UUID getAssociationOrphanUuid(String code)
//	{
//		return ConverterUUID.createNamespaceUUIDFromString("associationOrphan:" + code, true);
//	}
//	
//	private UUID getMapItemUUID(String mapSetVuid, String vuid)
//	{
//		return ConverterUUID.createNamespaceUUIDFromString("mapSetVuid:" + mapSetVuid + "mapItemVuid:" + vuid, false);
//	}
//
//	private UUID getConceptUuid(String codeId)
//	{
//		return ConverterUUID.createNamespaceUUIDFromString("code:" + codeId, true);
//	}
//
//	private UUID getDescriptionUuid(String descriptionId)
//	{
//		return ConverterUUID.createNamespaceUUIDFromString("description:" + descriptionId, true);
//	}
}
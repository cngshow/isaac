package gov.vha.isaac.ochre.mapping.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeValidatorType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUIDImpl;
import gov.vha.isaac.ochre.model.sememe.version.DynamicSememeImpl;
import gov.vha.isaac.ochre.query.provider.lucene.indexers.SememeIndexerConfiguration;
import javafx.concurrent.Task;

/**
 * {@link MappingSet}
 *
 * A Convenience class to hide unnecessary OTF bits from the Mapping APIs.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class MappingSetDAO extends MappingDAO
{
	private static final Logger LOG = LoggerFactory.getLogger(MappingSetDAO.class);
	
	/**
	 * Create and store a new mapping set in the DB.
	 * @param mappingName - The name of the mapping set (used for the FSN and preferred term of the underlying concept)
	 * @param inverseName - (optional) inverse name of the mapping set (if it makes sense for the mapping)
	 * @param purpose - (optional) - user specified purpose of the mapping set
	 * @param description - the intended use of the mapping set
	 * @param editorStatus - (optional) user specified status concept of the mapping set
	 * @return
	 * @throws IOException
	 */
	public static MappingSet createMappingSet(String mappingName, String inverseName, String purpose, String description, UUID editorStatus, StampCoordinate stampCoord, 
			EditCoordinate editCoord) throws IOException
	{
		//We need to create a new concept - which itself is defining a dynamic sememe - so set that up here.
		DynamicSememeUsageDescription rdud = Frills.createNewDynamicSememeUsageDescriptionConcept(
				mappingName, mappingName, description, 
				new DynamicSememeColumnInfo[] {
					new DynamicSememeColumnInfo(0, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT.getUUID(), 
							DynamicSememeDataType.UUID, null, false, false),
					new DynamicSememeColumnInfo(1, IsaacMappingConstants.get().MAPPING_QUALIFIERS.getUUID(), DynamicSememeDataType.UUID, null, false, 
							DynamicSememeValidatorType.IS_KIND_OF, new DynamicSememeUUIDImpl(IsaacMappingConstants.get().MAPPING_QUALIFIERS.getUUID()), false)},
//					new DynamicSememeColumnInfo(2, IsaacMappingConstants.get().MAPPING_STATUS.getUUID(), DynamicSememeDataType.UUID, null, false, 
//							DynamicSememeValidatorType.IS_KIND_OF, new DynamicSememeUUIDImpl(IsaacMappingConstants.get().MAPPING_STATUS.getUUID()), false)}, 
				null, ObjectChronologyType.CONCEPT, null);
		
		Get.workExecutors().getExecutor().execute(() ->
		{
			try
			{
				SememeIndexerConfiguration.configureColumnsToIndex(rdud.getDynamicSememeUsageDescriptorSequence(), new Integer[] {0, 1, 2}, true);
			}
			catch (Exception e)
			{
				LOG.error("Unexpected error enabling the index on newly created mapping set!", e);
			}
		});
		
		//Then, annotate the concept created above as a member of the MappingSet dynamic sememe, and add the inverse name, if present.
		if (!StringUtils.isBlank(inverseName))
		{
			ObjectChronology<?> builtDesc = LookupService.get().getService(DescriptionBuilderService.class).getDescriptionBuilder(inverseName, rdud.getDynamicSememeUsageDescriptorSequence(), 
					MetaData.SYNONYM, MetaData.ENGLISH_LANGUAGE).build(
							editCoord, ChangeCheckerMode.ACTIVE);
			
			Get.sememeBuilderService().getDynamicSememeBuilder(builtDesc.getNid(),DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getSequence()).build(
					editCoord, ChangeCheckerMode.ACTIVE);
		}
		
		@SuppressWarnings("rawtypes")
		SememeChronology mappingAnnotation = Get.sememeBuilderService().getDynamicSememeBuilder(Get.identifierService().getConceptNid(rdud.getDynamicSememeUsageDescriptorSequence()),
				IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getSequence(), 
				new DynamicSememeData[] {
//						(editorStatus == null ? null : new DynamicSememeUUIDImpl(editorStatus)),
						(StringUtils.isBlank(purpose) ? null : new DynamicSememeStringImpl(purpose))}).build(
				editCoord, ChangeCheckerMode.ACTIVE);

		
		Get.sememeBuilderService().getDynamicSememeBuilder(Get.identifierService().getConceptNid(rdud.getDynamicSememeUsageDescriptorSequence()),
				DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getSequence()).build(
				editCoord, ChangeCheckerMode.ACTIVE);
		
		@SuppressWarnings("deprecation")
		Task<Optional<CommitRecord>> task = Get.commitService().commit("update mapping item");
		
		try
		{
			task.get();
		}
		catch (Exception e)
		{
			throw new RuntimeException();
		}
		
		
		@SuppressWarnings("unchecked")
		Optional<LatestVersion<DynamicSememe<?>>> sememe = mappingAnnotation.getLatestVersion(DynamicSememe.class, stampCoord);
		
		//Find the constructed dynamic refset
		return new MappingSet(sememe.get().value(), stampCoord);
	}
	
	/**
	 * Store the changes (done via set methods) on the passed in mapping set.  
	 * @param mappingSet - The mappingSet that carries the changes
	 * @throws IOException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	public static void updateMappingSet(MappingSet mappingSet, StampCoordinate stampCoord, EditCoordinate editCoord) throws RuntimeException 
	{
		ConceptChronology mappingConcept = Get.conceptService().getConcept(mappingSet.getPrimordialUUID());
		
		Get.sememeService().getSememesForComponentFromAssemblage(mappingConcept.getNid(), 
				MetaData.DESCRIPTION_ASSEMBLAGE.getConceptSequence())
			.forEach(descriptionC ->
			{
				Optional<LatestVersion<DescriptionSememe<?>>> latest = ((SememeChronology)descriptionC).getLatestVersion(DescriptionSememe.class, 
						stampCoord);
				if (latest.isPresent())
				{
					DescriptionSememe<?> ds = latest.get().value();
					if (ds.getDescriptionTypeConceptSequence() == MetaData.SYNONYM.getConceptSequence())
					{
						if (Frills.isDescriptionPreferred(ds.getNid(), null))
						{
							if (!ds.getText().equals(mappingSet.getName()))
							{
								MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds)
										.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
								mutable.setText(mappingSet.getName());
								Get.commitService().addUncommitted((SememeChronology<DescriptionSememe>)ds);
							}
						}
						else
						//see if it is the inverse name
						{
							if (Get.sememeService().getSememesForComponentFromAssemblage(ds.getNid(), 
									DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getSequence()).anyMatch(sememeC -> 
									{
										return sememeC.isLatestVersionActive(stampCoord);
									}))
							{
								if (!ds.getText().equals(mappingSet.getInverseName()))
								{
									MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds)
											.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
									mutable.setText(mappingSet.getInverseName());
									Get.commitService().addUncommitted((SememeChronology<DescriptionSememe>)ds);
								}
							}
						}
					}
					else if (ds.getDescriptionTypeConceptSequence() == MetaData.DEFINITION_DESCRIPTION_TYPE.getConceptSequence())
					{
						if (Frills.isDescriptionPreferred(ds.getNid(), null))
						{
							if (!mappingSet.getDescription().equals(ds.getText()))
							{
								MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds.getChronology())
										.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
								mutable.setText(mappingSet.getDescription());
								Get.commitService().addUncommitted(ds.getChronology());
							}
						}
					}
				}
			});
		

		Optional<SememeChronology<? extends SememeVersion<?>>> mappingSememe =  Get.sememeService().getSememesForComponentFromAssemblage(mappingConcept.getNid(), 
				IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getSequence()).findAny();
					
		if (!mappingSememe.isPresent())
		{
			LOG.error("Couldn't find mapping refex?");
			throw new RuntimeException("internal error");
		}
		Optional<LatestVersion<DynamicSememe<?>>> latestVersion = ((SememeChronology)mappingSememe.get()).getLatestVersion(DynamicSememe.class, 
				stampCoord.makeAnalog(State.ACTIVE, State.INACTIVE));
		
		DynamicSememe<?> latest = latestVersion.get().value();
		
		if (latest.getData()[0] == null && mappingSet.getPurpose() != null || mappingSet.getPurpose() == null && latest.getData()[0] != null
				|| (latest.getData()[0] != null && ((DynamicSememeUUID)latest.getData()[0]).getDataUUID().equals(mappingSet.getEditorStatusConcept())) 
				|| latest.getData()[1] == null && mappingSet.getPurpose() != null || mappingSet.getPurpose() == null && latest.getData()[1] != null
				|| (latest.getData()[1] != null && ((DynamicSememeString)latest.getData()[1]).getDataString().equals(mappingSet.getPurpose())))
		{
			DynamicSememeImpl mutable = (DynamicSememeImpl) ((SememeChronology)mappingSememe.get()).createMutableVersion(MutableDynamicSememe.class, 
					latest.getStampSequence());

			mutable.setData(new DynamicSememeData[] {
					(mappingSet.getEditorStatusConcept() == null ? null : new DynamicSememeUUIDImpl(mappingSet.getEditorStatusConcept())),
					(StringUtils.isBlank(mappingSet.getPurpose()) ? null : new DynamicSememeStringImpl(mappingSet.getPurpose()))});
			Get.commitService().addUncommitted(latest.getChronology());
		}
		
		Get.commitService().commit("Update mapping");
	}

	// TODO call the below method from the above method
	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	public static void updateMappingSet(
			ConceptChronology<?> mappingConcept,
			String mapName,
			String mapInverseName,
			String mapDescription,
			String mapPurpose,
			ConceptChronology<?> editorConceptChronology, // optional
			StampCoordinate stampCoord,
			EditCoordinate editCoord) throws RuntimeException 
	{		
		Get.sememeService().getSememesForComponentFromAssemblage(mappingConcept.getNid(), 
				MetaData.DESCRIPTION_ASSEMBLAGE.getConceptSequence())
			.forEach(descriptionC ->
			{
				Optional<LatestVersion<DescriptionSememe<?>>> latest = ((SememeChronology)descriptionC).getLatestVersion(DescriptionSememe.class, 
						stampCoord);
				if (latest.isPresent())
				{
					DescriptionSememe<?> ds = latest.get().value();
					if (ds.getDescriptionTypeConceptSequence() == MetaData.SYNONYM.getConceptSequence())
					{
						if (Frills.isDescriptionPreferred(ds.getNid(), null))
						{
							if (!ds.getText().equals(mapName))
							{
								MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds)
										.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
								mutable.setText(mapName);
								Get.commitService().addUncommitted((SememeChronology<DescriptionSememe>)ds);
							}
						}
						else
						//see if it is the inverse name
						{
							if (Get.sememeService().getSememesForComponentFromAssemblage(ds.getNid(), 
									DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getSequence()).anyMatch(sememeC -> 
									{
										return sememeC.isLatestVersionActive(stampCoord);
									}))
							{
								if (!ds.getText().equals(mapInverseName))
								{
									MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds)
											.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
									mutable.setText(mapInverseName);
									Get.commitService().addUncommitted((SememeChronology<DescriptionSememe>)ds);
								}
							}
						}
					}
					else if (ds.getDescriptionTypeConceptSequence() == MetaData.DEFINITION_DESCRIPTION_TYPE.getConceptSequence())
					{
						if (Frills.isDescriptionPreferred(ds.getNid(), null))
						{
							if (!mapDescription.equals(ds.getText()))
							{
								MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds.getChronology())
										.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
								mutable.setText(mapDescription);
								Get.commitService().addUncommitted(ds.getChronology());
							}
						}
					}
				}
			});
		

		Optional<SememeChronology<? extends SememeVersion<?>>> mappingSememe =  Get.sememeService().getSememesForComponentFromAssemblage(mappingConcept.getNid(), 
				IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getSequence()).findAny();
					
		if (!mappingSememe.isPresent())
		{
			LOG.error("Couldn't find mapping refex?");
			throw new RuntimeException("internal error");
		}
		Optional<LatestVersion<DynamicSememe<?>>> latestVersion = ((SememeChronology)mappingSememe.get()).getLatestVersion(DynamicSememe.class, 
				stampCoord.makeAnalog(State.ACTIVE, State.INACTIVE));
		
		DynamicSememe<?> latest = latestVersion.get().value();
		
		if (latest.getData()[0] == null && mapPurpose != null || mapPurpose == null && latest.getData()[0] != null
				|| (latest.getData()[0] != null && ((DynamicSememeUUID)latest.getData()[0]).getDataUUID().equals(editorConceptChronology.getPrimordialUuid())) 
				|| latest.getData()[1] == null && mapPurpose != null || mapPurpose == null && latest.getData()[1] != null
				|| (latest.getData()[1] != null && ((DynamicSememeString)latest.getData()[1]).getDataString().equals(mapPurpose)))
		{
			DynamicSememeImpl mutable = (DynamicSememeImpl) ((SememeChronology)mappingSememe.get()).createMutableVersion(MutableDynamicSememe.class, 
					latest.getStampSequence());

			mutable.setData(new DynamicSememeData[] {
					(editorConceptChronology == null ? null : new DynamicSememeUUIDImpl(editorConceptChronology.getPrimordialUuid())),
					(StringUtils.isBlank(mapPurpose) ? null : new DynamicSememeStringImpl(mapPurpose))});
			Get.commitService().addUncommitted(latest.getChronology());
		}
		
		Get.commitService().commit("Update mapping");
	}
	
	public static List<MappingSet> getMappingSets(StampCoordinate stampCoord) throws IOException
	{
		ArrayList<MappingSet> result = new ArrayList<>();
		
		Get.sememeService().getSememesFromAssemblage(IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getSequence()).forEach(sememeC -> 
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)sememeC).getLatestVersion(DynamicSememe.class, 
						stampCoord);
				
				if (latest.isPresent())
				{
					//TODO handle contradictions properly
					result.add(new MappingSet(latest.get().value(), stampCoord));
					if (latest.get().contradictions().isPresent())
					{
						latest.get().contradictions().get().forEach((contradiction) -> result.add(new MappingSet(contradiction, stampCoord)));
					}
				}
			});

		return result;
	}
	
	public static void retireMappingSet(UUID mappingSetPrimordialUUID, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException
	{
		setConceptStatus(mappingSetPrimordialUUID, State.INACTIVE, stampCoord, editCoord);
	}
	
	public static void unRetireMappingSet(UUID mappingSetPrimordialUUID, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException
	{
		setConceptStatus(mappingSetPrimordialUUID, State.ACTIVE, stampCoord, editCoord);
	}
	
	public static Optional<ConceptVersion<?>> getMappingConcept(DynamicSememe<?> sememe, StampCoordinate stampCoord) throws RuntimeException {
		
		ConceptChronology<? extends ConceptVersion<?>> cc = Get.conceptService().getConcept(sememe.getReferencedComponentNid());
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Optional<LatestVersion<ConceptVersion<?>>> cv =  ((ConceptChronology) cc).getLatestVersion(ConceptVersion.class, stampCoord.makeAnalog(State.ACTIVE, State.INACTIVE));
		
		if (cv.isPresent())
		{ 
			if(cv.get().contradictions().isPresent())
			{
				//TODO handle these properly
				LOG.warn("Concept has contradictions!");
			}
			return Optional.of(cv.get().value());
		}
		return Optional.empty();
	}
}

package gov.vha.isaac.ochre.mapping.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUIDImpl;
import javafx.concurrent.Task;

public class MappingItemDAO extends MappingDAO
{
	/**
	 * Construct (and save to the DB) a new MappingItem.  
	 * @param sourceConcept - the primary ID of the source concept
	 * @param mappingSetID - the primary ID of the mapping type
	 * @param targetConcept - the primary ID of the target concept
	 * @param qualifierID - (optional) the primary ID of the qualifier concept
	 * @param editorStatusID - (optional) the primary ID of the status concept
	 * @throws IOException
	 */
	public static MappingItem createMappingItem(ConceptSnapshot sourceConcept, UUID mappingSetID, ConceptSnapshot targetConcept, 
			UUID qualifierID, UUID editorStatusID, StampCoordinate stampCoord, EditCoordinate editCoord) throws RuntimeException
	{
		SememeBuilder<? extends SememeChronology<?>> sb =  Get.sememeBuilderService().getDynamicSememeBuilder(
				sourceConcept.getNid(),  
				Get.identifierService().getConceptSequenceForUuids(mappingSetID), 
				new DynamicSememeData[] {
						(targetConcept == null ? null : new DynamicSememeUUIDImpl(targetConcept.getPrimordialUuid())),
						(qualifierID == null ? null : new DynamicSememeUUIDImpl(qualifierID)),
						(editorStatusID == null ? null : new DynamicSememeUUIDImpl(editorStatusID))});
		
		UUID mappingItemUUID = UuidT5Generator.get(IsaacMappingConstants.get().MAPPING_NAMESPACE.getUUID(), 
				sourceConcept.getPrimordialUuid().toString() + "|" 
				+ mappingSetID.toString() + "|"
				+ ((targetConcept == null)? "" : targetConcept.getPrimordialUuid().toString()) + "|" 
				+ ((qualifierID == null)?   "" : qualifierID.toString()));
		
		if (Get.identifierService().hasUuid(mappingItemUUID))
		{
			throw new RuntimeException("A mapping with the specified source, target and qualifier already exists in this set.  Please edit that mapping.");
		}
		
		sb.setPrimordialUuid(mappingItemUUID);
		@SuppressWarnings("rawtypes")
		SememeChronology built = sb.build(editCoord,ChangeCheckerMode.ACTIVE);

		@SuppressWarnings("deprecation")
		Task<Optional<CommitRecord>> task = Get.commitService().commit("Added comment");
		
		try
		{
			task.get();
		}
		catch (Exception e)
		{
			throw new RuntimeException();
		}
		
		@SuppressWarnings({ "unchecked" })
		Optional<LatestVersion<DynamicSememe<?>>> latest = built.getLatestVersion(DynamicSememe.class, 
				stampCoord.makeAnalog(State.ACTIVE, State.INACTIVE));
		
		return new MappingItem(latest.get().value(), stampCoord);
	}
	
	/**
	 * Read all of the mappings items which are defined as part of the specified mapping set.
	 * 
	 * @param mappingSetID - the mapping set that contains the mapping items
	 * @return
	 * @throws IOException
	 */
	public static List<MappingItem> getMappingItems(UUID mappingSetID, StampCoordinate stampCoord) throws IOException
	{
		ArrayList<MappingItem> result = new ArrayList<>();
		Get.sememeService().getSememesFromAssemblage(Get.identifierService().getNidForUuids(mappingSetID)).forEach(sememeC -> 
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)sememeC).getLatestVersion(DynamicSememe.class, 
						stampCoord);
				
				if (latest.isPresent())
				{
					//TODO figure out how to handle contradictions!
					result.add(new MappingItem(latest.get().value(), stampCoord));
					if (latest.get().contradictions().isPresent())
					{
						latest.get().contradictions().get().forEach((contradiction) -> result.add(new MappingItem(contradiction, stampCoord)));
					}
				}
			});
		return result;
	}

	/**
	 * Just test / demo code
	 * @param mappingSetUUID
	 * @throws IOException
	 */
	/*
	public static void generateRandomMappingItems(UUID mappingSetUUID)
	{
		try
		{
			LuceneDescriptionIndexer ldi = AppContext.getService(LuceneDescriptionIndexer.class);
			List<SearchResult> result = ldi.query("acetaminophen", ComponentProperty.DESCRIPTION_TEXT, 100);

			for (int i = 0; i < 10; i++)
			{
				UUID source;
				UUID target = null;

				int index = (int) (Math.random() * 100);
				source = ExtendedAppContext.getDataStore().getConceptForNid(result.get(index).getNid()).getPrimordialUuid();

				while (target == null || target.equals(source))
				{
					index = (int) (Math.random() * 100);
					target = ExtendedAppContext.getDataStore().getConceptForNid(result.get(index).getNid()).getPrimordialUuid();
				}

				createMappingItem(source, mappingSetUUID, target, UUID.fromString("c1068428-a986-5c12-9583-9b2d3a24fdc6"),
						UUID.fromString("d481125e-b8ca-537c-b688-d09d626e5ff9"));
			}
		}
		catch (Exception e)
		{
			LOG.error("oops", e);
		}
	}
	*/
	
	/**
	 * Store the values passed in as a new revision of a mappingItem (the old revision remains in the DB)
	 * @param mappingItem - The MappingItem with revisions (contains fields where the setters have been called)
	 * @throws IOException
	 */
	public static void updateMappingItem(MappingItem mappingItem, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException
	{
		DynamicSememe<?> rdv = readCurrentRefex(mappingItem.getPrimordialUUID(), stampCoord);
		
		DynamicSememeData[] data = rdv.getData();
		data[2] = (mappingItem.getEditorStatusConcept() != null ? new DynamicSememeUUIDImpl(mappingItem.getEditorStatusConcept()) : null);
		
		Get.sememeBuilderService().getDynamicSememeBuilder(rdv.getReferencedComponentNid(),  
				rdv.getAssemblageSequence(), data).build(editCoord, ChangeCheckerMode.ACTIVE);

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
	}

	/**
	 * @param mappingItemPrimordial - The identifier of the mapping item to be retired
	 * @throws IOException
	 */
	public static void retireMappingItem(UUID mappingItemPrimordial, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException
	{
		setSememeStatus(mappingItemPrimordial, State.INACTIVE, stampCoord, editCoord);
	}

	/**
	 * @param mappingItemPrimordial - The identifier of the mapping item to be re-activated
	 * @throws IOException
	 */
	public static void unRetireMappingItem(UUID mappingItemPrimordial, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException
	{
		setSememeStatus(mappingItemPrimordial, State.ACTIVE, stampCoord, editCoord);
	}
}

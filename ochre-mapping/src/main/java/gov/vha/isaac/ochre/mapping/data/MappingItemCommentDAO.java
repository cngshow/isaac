package gov.vha.isaac.ochre.mapping.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import javafx.concurrent.Task;

public class MappingItemCommentDAO extends MappingDAO 
{
	/**
	 * Create (and store to the DB) a new comment
	 * @param pMappingItemUUID - The item the comment is being added to
	 * @param pCommentText - The text of the comment
	 * @param commentContext - (optional) field for storing other arbitrary info about the comment.  An editor may wish to put certain keywords on 
	 * some comments - this field is indexed, so a search for comments could query this field.
	 * @throws IOException
	 */
	public static MappingItemComment createMappingItemComment(UUID pMappingItemUUID, String pCommentText, String commentContext, StampCoordinate stampCoord,
			EditCoordinate editCoord) throws RuntimeException
	{
		if (pMappingItemUUID == null)
		{
			throw new RuntimeException("UUID of component to attach the comment to is required");
		}
		if (StringUtils.isBlank(pCommentText))
		{
			throw new RuntimeException("The comment is required");
		}

		SememeChronology<? extends DynamicSememe<?>> built =  Get.sememeBuilderService().getDynamicSememeBuilder(
				Get.identifierService().getNidForUuids(pMappingItemUUID),  
				DynamicSememeConstants.get().DYNAMIC_SEMEME_COMMENT_ATTRIBUTE.getSequence(), 
				new DynamicSememeData[] {new DynamicSememeStringImpl(pCommentText),
						(StringUtils.isBlank(commentContext) ? null : new DynamicSememeStringImpl(commentContext))})
			.build(editCoord, ChangeCheckerMode.ACTIVE);

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
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)built).getLatestVersion(DynamicSememe.class, 
				stampCoord.makeAnalog(State.ACTIVE, State.INACTIVE));
		
		return new MappingItemComment(latest.get().value());
	}

	/**
	 * Read all comments for a particular mapping item (which could be a mapping set, or a mapping item)
	 * @param mappingUUID - The UUID of a MappingSet or a MappingItem
	 * @return
	 * @throws RuntimeException
	 */
	public static List<MappingItemComment> getComments(UUID mappingUUID, StampCoordinate stampCoord) throws RuntimeException {
		List<MappingItemComment> comments = new ArrayList<MappingItemComment>();
		
		Get.sememeService().getSememesForComponentFromAssemblage(Get.identifierService().getNidForUuids(mappingUUID),
				DynamicSememeConstants.get().DYNAMIC_SEMEME_COMMENT_ATTRIBUTE.getSequence()).forEach(sememeC -> 
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)sememeC).getLatestVersion(DynamicSememe.class, 
							stampCoord.makeAnalog(State.ACTIVE, State.INACTIVE));
					
					if (latest.isPresent())
					{
						comments.add(new MappingItemComment(latest.get().value()));
						if (latest.get().contradictions().isPresent())
						{
							//TODO handle contradictions properly
							latest.get().contradictions().get().forEach((contradiction) -> comments.add(new MappingItemComment(contradiction)));
						}
					}
				});

		return comments;
	}
	
	/**
	 * @param commentPrimordialUUID - The ID of the comment to be re-activated
	 * @throws IOException
	 */
	public static void unRetireComment(UUID commentPrimordialUUID, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException 
	{
		setSememeStatus(commentPrimordialUUID, State.ACTIVE, stampCoord, editCoord);
	}
	
	/**
	 * @param commentPrimordialUUID - The ID of the comment to be retired
	 * @throws IOException
	 */
	public static void retireComment(UUID commentPrimordialUUID, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException 
	{
		setSememeStatus(commentPrimordialUUID, State.INACTIVE, stampCoord, editCoord);
	}
	
	/**
	 * Store the values passed in as a new revision of a comment (the old revision remains in the DB)
	 * @param comment - The MappingItemComment with revisions (contains fields where the setters have been called)
	 * @throws IOException
	 */
	public static void updateComment(MappingItemComment comment, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException 
	{
		DynamicSememe<?> rdv = readCurrentRefex(comment.getPrimordialUUID(), stampCoord);
		Get.sememeBuilderService().getDynamicSememeBuilder(rdv.getReferencedComponentNid(),  
				DynamicSememeConstants.get().DYNAMIC_SEMEME_COMMENT_ATTRIBUTE.getSequence(), 
				new DynamicSememeData[] {new DynamicSememeStringImpl(comment.getCommentText()),
						(StringUtils.isBlank(comment.getCommentContext()) ? null : new DynamicSememeStringImpl(comment.getCommentContext()))})
			.build(editCoord,
					ChangeCheckerMode.ACTIVE);

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
	}
}

/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.mapping.data;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.util.StringUtils;
import gov.vha.isaac.ochre.impl.utility.Frills;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;

/**
 * {@link MappingItem}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class MappingItem extends MappingObject
{
	private static final Logger LOG = LoggerFactory.getLogger(MappingItem.class);

	private static final String NO_MAP_NAME = "(not mapped)";
	
	private List<UUID> uuids;
	private int	sourceConceptNid, mappingSetSequence;
	private UUID qualifierConcept, targetConcept;
	private DynamicSememeData[] data_;
	

	private transient boolean lazyLoadComplete = false;
	private transient UUID mappingSetIDConcept, sourceConcept;
	private transient int targetConceptNid, qualifierConceptNid;
	private transient final SimpleStringProperty sourceConceptProperty = new SimpleStringProperty();
	private transient final SimpleStringProperty targetConceptProperty = new SimpleStringProperty();
	private transient final SimpleStringProperty qualifierConceptProperty = new SimpleStringProperty();
	private transient final SimpleStringProperty commentsProperty = new SimpleStringProperty();
	
	protected MappingItem(DynamicSememe<?> sememe) throws RuntimeException
	{
		read(sememe);
	}
	
	private void read(DynamicSememe<?> sememe) throws RuntimeException
	{
		readStampDetails(sememe);
		mappingSetSequence = sememe.getAssemblageSequence();
		sourceConceptNid = sememe.getReferencedComponentNid();
		uuids = sememe.getUuidList();
		data_ = sememe.getData();
		setTargetConcept(((data_ != null && data_.length > 0 && data_[0] != null) ? ((DynamicSememeUUID) data_[0]).getDataUUID() : null));
		setQualifierConcept(((data_ != null && data_.length > 1 && data_[1] != null) ? ((DynamicSememeUUID) data_[1]).getDataUUID() : null)); 
	}
	
	private void lazyLoad()
	{
		if (!lazyLoadComplete)
		{
			mappingSetIDConcept = Get.identifierService().getUuidPrimordialForNid(mappingSetSequence).get();
			setSourceConcept(Get.identifierService().getUuidPrimordialForNid(sourceConceptNid).get());
	
			//TODO remove this
			setEditorStatusConcept(((data_ != null && data_.length > 2 && data_[2] != null) ? ((DynamicSememeUUID) data_[2]).getDataUUID() : null));
			
			targetConceptNid    = getNidForUuidSafe(targetConcept);
			qualifierConceptNid = getNidForUuidSafe(qualifierConcept);
		}
		lazyLoadComplete = true;
	}

	public int getSourceConceptNid() 
	{ 
		return sourceConceptNid; 
	}
	public int getTargetConceptNid() 
	{ 
		lazyLoad();
		return targetConceptNid; 
	}
	
	public int getQualifierConceptNid() 
	{ 
		lazyLoad();
		return qualifierConceptNid; 
	}
	
	public String getSummary() {
		return  (isActive() ? "Active " : "Retired ") + "Mapping: " + Frills.getDescription(sourceConcept).get() 
				+ "-" + Frills.getDescription(mappingSetIDConcept).get() 
				+ "-" + (targetConcept == null ? "not mapped" : Frills.getDescription(targetConcept).get() ) + "-" 
				+ (qualifierConcept == null ? "no qualifier" : Frills.getDescription(qualifierConcept).get() ) 
				+ "-" + (editorStatusConcept == null ? "no status" : Frills.getDescription(editorStatusConcept).get() ) + "-" + uuids.get(0).toString();
	}
	
	/**
	 * @return Any comments attached to this mapping set.
	 * @throws IOException 
	 */
	public List<MappingItemComment> getComments(StampCoordinate stampCoord) throws IOException
	{
		return MappingItemCommentDAO.getComments(getPrimordialUUID(), stampCoord);
	}
	
	/**
	 * Add a comment to this mapping set
	 * @param commentText - the text of the comment
	 * @return - the added comment
	 * @throws IOException
	 */
	public MappingItemComment addComment(String commentText, StampCoordinate stampCoord, EditCoordinate editCoord) throws IOException
	{
		//TODO do we want to utilize the other comment field (don't have to)
		return MappingItemCommentDAO.createMappingItemComment(this.getPrimordialUUID(), commentText, null, stampCoord, editCoord);
	}

	/**
	 * @return the primordialUUID of this Mapping Item.  Note that this doesn't uniquely identify a mapping item within the system
	 * as changes to the mapping item will retain the same ID - there will now be multiple versions.  They will differ by date.
	 */
	public UUID getPrimordialUUID()
	{
		return uuids.get(0);
	}
	
	/**
	 * @return the UUIDs of this Mapping Item.  Note that this doesn't uniquely identify a mapping item within the system
	 * as changes to the mapping item will retain the same ID - there will now be multiple versions.  They will differ by date.
	 * There will typically be only one entry in this list (identical to the value of {@link #getPrimordialUUID}
	 */
	public List<UUID> getUUIDs()
	{
		return uuids;
	}
	
	public int getMapSetSequence()
	{
		return mappingSetSequence;
	}

	public UUID getMappingSetIDConcept()
	{ 
		lazyLoad();
		return mappingSetIDConcept;
	}
	public UUID getSourceConcept()
	{ 
		lazyLoad();
		return sourceConcept;	
	}
	public UUID getTargetConcept()
	{
		return targetConcept;	
	}
	public UUID getQualifierConcept()
	{
		return qualifierConcept;	
	}
	
	public SimpleStringProperty getSourceConceptProperty()
	{
		lazyLoad();
		return sourceConceptProperty;
	}
	
	public SimpleStringProperty getCommentsProperty(StampCoordinate stampCoord)
	{
		refreshCommentsProperty(stampCoord);
		return sourceConceptProperty;
	}
	
	public SimpleStringProperty getTargetConceptProperty()
	{
		lazyLoad();
		return targetConceptProperty;	
	}
	public SimpleStringProperty getQualifierConceptProperty() 
	{ 
		lazyLoad();
		return qualifierConceptProperty; 
	}
	
	private void setSourceConcept(UUID sourceConcept) {
		this.sourceConcept = sourceConcept;
		propertyLookup(sourceConcept, sourceConceptProperty);
	}
	
	private void setTargetConcept(UUID targetConcept) {
		this.targetConcept = targetConcept;
		if (targetConcept == null) {
			targetConceptProperty.set(NO_MAP_NAME);
		} else {
			propertyLookup(targetConcept, targetConceptProperty);
		}
	}
	
	private void setQualifierConcept(UUID qualifierConcept) {
		this.qualifierConcept = qualifierConcept;
		propertyLookup(qualifierConcept, qualifierConceptProperty);
	}
	
	public void refreshCommentsProperty(StampCoordinate stampCoord) {
		Get.workExecutors().getExecutor().execute(() ->
		{
			StringBuilder commentValue = new StringBuilder();
			try
			{
				List<MappingItemComment> comments = getComments(stampCoord);
				if (comments.size() > 0) {
					commentValue.append(comments.get(0).getCommentText());
				}
				if (comments.size() > 1) {
					commentValue.append(" (+" + Integer.toString(comments.size() - 1) + " more)");
				}
			}
			catch (IOException e)
			{
				LOG.error("Error reading comments!", e);
			}
			Platform.runLater(() ->
			{
				commentsProperty.set(commentValue.toString());
			});
		});
	}

	public static final Comparator<MappingItem> sourceComparator = new Comparator<MappingItem>() {
		@Override
		public int compare(MappingItem o1, MappingItem o2) {
			return StringUtils.compareStringsIgnoreCase(o1.getSourceConceptProperty().get(), o2.getSourceConceptProperty().get());
		}
	};
	
	public static final Comparator<MappingItem> targetComparator = new Comparator<MappingItem>() {
		@Override
		public int compare(MappingItem o1, MappingItem o2) {
			return StringUtils.compareStringsIgnoreCase(o1.getTargetConceptProperty().get(), o2.getTargetConceptProperty().get());
		}
	};
	
	public static final Comparator<MappingItem> qualifierComparator = new Comparator<MappingItem>() {
		@Override
		public int compare(MappingItem o1, MappingItem o2) {
			return StringUtils.compareStringsIgnoreCase(o1.getQualifierConceptProperty().get(), o2.getQualifierConceptProperty().get());
		}
	};
}

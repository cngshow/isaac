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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.mapping.data;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.UUID;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.util.StringUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;

/**
 * {@link StampedItem}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public abstract class StampedItem
{
	private StampedVersion componentVersion_;
	
	private transient boolean lazyLoadFinished_ = false;
	private transient SimpleStringProperty authorSSP = new SimpleStringProperty("-");
	private transient SimpleStringProperty moduleSSP = new SimpleStringProperty("-");;
	private transient SimpleStringProperty pathSSP   = new SimpleStringProperty("-");;
	private transient SimpleStringProperty statusSSP   = new SimpleStringProperty("-");;
	private transient SimpleStringProperty timeSSP   = new SimpleStringProperty("-");;
	private transient UUID authorUUID;
	private transient UUID moduleUUID;
	private transient UUID pathUUID;
	

	protected void readStampDetails(StampedVersion componentVersion) throws RuntimeException
	{
		componentVersion_ = componentVersion;
	}
	
	private void lazyLoad()
	{
		if (!lazyLoadFinished_)
		{
			authorUUID = Get.identifierService().getUuidPrimordialFromConceptId(componentVersion_.getAuthorSequence()).get();
			moduleUUID = Get.identifierService().getUuidPrimordialFromConceptId(componentVersion_.getModuleSequence()).get();
			pathUUID = Get.identifierService().getUuidPrimordialFromConceptId(componentVersion_.getPathSequence()).get();
			
			Get.workExecutors().getExecutor().execute(() ->
			{
				String authorName = Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(authorUUID));
				String moduleName = Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(moduleUUID));
				String pathName =   Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(pathUUID));
				Platform.runLater(() -> {
					authorSSP.set(authorName);
					moduleSSP.set(moduleName);
					pathSSP.set(pathName);
					statusSSP.set(this.isActive() ? "Active" : "Inactive");
					timeSSP.set(new SimpleDateFormat("MM/dd/yy HH:mm").format(this.getTime()));
				});
			});
		}
		lazyLoadFinished_ = true;
	}
	
	/**
	 * @return the authorName - a UUID that identifies a concept that represents the Author
	 */
	public UUID getAuthorName()
	{
		lazyLoad();
		return authorUUID;
	}

	/**
	 * @return the creationDate
	 */
	public long getTime()
	{
		return componentVersion_.getTime();
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive()
	{
		return componentVersion_.getState() == State.ACTIVE;
	}

	/**
	 * @return the moduleUUID
	 */
	public UUID getModuleUUID()
	{
		lazyLoad();
		return moduleUUID;
	}

	/**
	 * @return the pathUUID
	 */
	public UUID getPathUUID()
	{
		lazyLoad();
		return pathUUID;
	}

	public SimpleStringProperty getStatusProperty() 
	{ 
		lazyLoad();
		return statusSSP;
	}
	
	public SimpleStringProperty getTimeProperty()   
	{
		lazyLoad();
		return timeSSP;
	}
	
	public SimpleStringProperty getAuthorProperty() 
	{ 
		lazyLoad();
		return authorSSP; 
	}
	
	public SimpleStringProperty getModuleProperty() 
	{
		lazyLoad();
		return moduleSSP; 
	}
	
	public SimpleStringProperty getPathProperty()
	{ 
		lazyLoad();
		return pathSSP; 
	}
	
	public int getAuthor() 
	{ 
		return componentVersion_.getAuthorSequence(); 
	}
	public int getModule() 
	{ 
		return componentVersion_.getModuleSequence(); 
	}
	public int getPath()   
	{ 
		return componentVersion_.getPathSequence(); 
	}
	
	public StampedVersion getComponentVersion()
	{
		return componentVersion_;
	}
	
	public static final Comparator<StampedItem> statusComparator = new Comparator<StampedItem>() {
		@Override
		public int compare(StampedItem o1, StampedItem o2) {
			// o1 and o2 intentionally reversed in this call, to make Active come before Inactive
			return Boolean.compare(o2.isActive(), o1.isActive());
		}
	};
	
	public static final Comparator<StampedItem> timeComparator = new Comparator<StampedItem>() {
		@Override
		public int compare(StampedItem o1, StampedItem o2) {
			return Long.compare(o1.getTime(), o2.getTime());
		}
	};
	
	public static final Comparator<StampedItem> authorComparator = new Comparator<StampedItem>() {
		@Override
		public int compare(StampedItem o1, StampedItem o2) {
			return StringUtils.compareStringsIgnoreCase(o1.getAuthorProperty().get(), o2.getAuthorProperty().get());
		}
	};
	
	public static final Comparator<StampedItem> moduleComparator = new Comparator<StampedItem>() {
		@Override
		public int compare(StampedItem o1, StampedItem o2) {
			return StringUtils.compareStringsIgnoreCase(o1.getModuleProperty().get(), o2.getModuleProperty().get());
		}
	};
	
	public static final Comparator<StampedItem> pathComparator = new Comparator<StampedItem>() {
		@Override
		public int compare(StampedItem o1, StampedItem o2) {
			return StringUtils.compareStringsIgnoreCase(o1.getPathProperty().get(), o2.getPathProperty().get());
		}
	};
}

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
package gov.vha.isaac.ochre.associations;

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.model.configuration.LanguageCoordinates;

/**
 * {@link AssociationInstance}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class AssociationInstance
{
	private DynamicSememe<?> sememe_;
	private StampCoordinate stampCoord_;
	
	private transient AssociationType assnType_;

	//TODO Write the code that checks the index states on startup
	
	private AssociationInstance(DynamicSememe<?> data, StampCoordinate stampCoordinate)
	{
		sememe_ = data;
		stampCoord_ = stampCoordinate;
	}
	
	/**
	 * Read the dynamic sememe instance (that represents an association) and turn it into an association object.
	 * @param data - the sememe to read
	 * @param stampCoordinate - optional - only used during readback of the association type - will only be utilized
	 * if one calls {@link AssociationInstance#getAssociationType()} - see {@link AssociationType#read(int, StampCoordinate)}
	 * @return
	 */
	public static AssociationInstance read(DynamicSememe<?> data, StampCoordinate stampCoordinate)
	{
		return new AssociationInstance(data, stampCoordinate);
	}
	
	public AssociationType getAssociationType()
	{
		if (assnType_ == null)
		{
			assnType_ = AssociationType.read(sememe_.getAssemblageSequence(), stampCoord_, LanguageCoordinates.getUsEnglishLanguagePreferredTermCoordinate());
		}
		return assnType_;
	}



	/**
	 * @return the source component of the association.
	 */
	public ObjectChronology<? extends StampedVersion> getSourceComponent()
	{
		return Get.identifiedObjectService().getIdentifiedObjectChronology(sememe_.getReferencedComponentNid()).get();
	}
	
	/**
	 * @return the nid of the source component of the association
	 */
	public int getSourceComponentData()
	{
		return sememe_.getReferencedComponentNid();
	}

	/**
	 * @return - the target component (if any) linked by this association instance
	 * This may return an empty if there was no target linked, or, if the target linked
	 * was a UUID that isn't resolveable in this DB (in which case, see the {@link #getTargetComponentData()} method)
	 */
	public Optional<? extends ObjectChronology<? extends StampedVersion>> getTargetComponent()
	{
		int targetColIndex = AssociationUtilities.findTargetColumnIndex(sememe_.getAssemblageSequence());
		if (targetColIndex >= 0)
		{
			DynamicSememeData[] data = sememe_.getData();
			if (data != null && data.length > targetColIndex && data[targetColIndex] != null)
			{
				int nid = 0;
				if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.UUID 
						&& Get.identifierService().hasUuid(((DynamicSememeUUID) data[targetColIndex]).getDataUUID()))
				{
					nid = Get.identifierService().getNidForUuids(((DynamicSememeUUID) data[targetColIndex]).getDataUUID());
				}
				else if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.NID)
				{
					nid = ((DynamicSememeNid) data[targetColIndex]).getDataNid();
				}
				if (nid != 0)
				{	
					return Get.identifiedObjectService().getIdentifiedObjectChronology(nid);
				}
			}
		}
		else
		{
			throw new RuntimeException("unexpected");
		}
		
		return Optional.empty();
	}
	
	/**
	 * @return the raw target component data - which will be of type {@link DynamicSememeNidBI} or {@link DynamicSememeUUID}
	 * or, it may be empty, if there was not target.
	 */
	public Optional<DynamicSememeData> getTargetComponentData()
	{
		int targetColIndex = AssociationUtilities.findTargetColumnIndex(sememe_.getAssemblageSequence());
		if (targetColIndex >= 0)
		{
			DynamicSememeData[] data = sememe_.getData();
			if (data != null && data.length > targetColIndex)
			{
				if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.UUID)
				{
					return Optional.of(((DynamicSememeUUID) data[targetColIndex]));
				}
				else if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.NID)
				{
					return Optional.of((DynamicSememeNid) data[targetColIndex]);
				}
			}
		}
		else
		{
			throw new RuntimeException("unexpected");
		}
		
		return Optional.empty();
	}

	/**
	 * @return the concept sequence of the association type concept (without incurring the overhead of reading the AssoicationType object)
	 */
	public int getAssociationTypeSequenece() 
	{
		return sememe_.getAssemblageSequence();
	}

	public DynamicSememe<?> getData()
	{
		return sememe_;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		try
		{
			return "Association [Name: " + getAssociationType().getAssociationName() + " Inverse Name: " + getAssociationType().getAssociationInverseName() 
					+ " Source: " + getSourceComponent().getPrimordialUuid() 
					+ " Type: " + getAssociationType().getAssociationTypeConcept().getPrimordialUuid() + " Target: " + getTargetComponentData().toString() + "]";
		}
		catch (Exception e)
		{
			LogManager.getLogger().error("Error formatting association instance", e);
			return sememe_.toString();
		}
	}
}

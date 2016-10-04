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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.model.sememe.DynamicSememeUtilityImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import gov.vha.isaac.ochre.query.provider.lucene.indexers.SememeIndexer;

/**
 * {@link AssociationUtilities}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class AssociationUtilities
{
	private static int associationSequence = Integer.MIN_VALUE;

	private static int getAssociationSequence()
	{
		if (associationSequence == Integer.MIN_VALUE)
		{
			associationSequence = DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getSequence();
		}
		return associationSequence;
	}

	/**
	 * Get all associations that originate on the specified componentNid
	 * @param componentNid
	 * @param stamp - optional - if not provided, uses the default from the config service
	 */
	public static List<AssociationInstance> getSourceAssociations(int componentNid, StampCoordinate stamp)
	{
		ArrayList<AssociationInstance> results = new ArrayList<>();
		Get.sememeService().getSememesForComponentFromAssemblage(componentNid, getAssociationSequence())
			.forEach(associationC -> 
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)associationC).getLatestVersion(DynamicSememe.class, 
							stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp);
					if (latest.isPresent())
					{
						results.add(AssociationInstance.read(latest.get().value(), stamp));
					}
					
				});
		return results;
	}

	/**
	 * Get all association instances that have a target of the specified componentNid
	 * @param componentNid
	 * @param stamp - optional - if not provided, uses the default from the config service
	 */
	//TODO should probabaly have a method here that takes in a target UUID, since that seems to be how I stored them?
	public static List<AssociationInstance> getTargetAssociations(int componentNid, StampCoordinate stamp)
	{
		ArrayList<AssociationInstance> result = new ArrayList<>();

		SememeIndexer indexer = LookupService.getService(SememeIndexer.class);
		if (indexer == null)
		{
			throw new RuntimeException("Required index is not available");
		}
		
		for (Integer associationTypeSequenece : getAssociationConceptSequences())
		{
			try
			{
				int colIndex = findTargetColumnIndex(associationTypeSequenece);
				UUID uuid = Get.identifierService().getUuidPrimordialForNid(componentNid).orElse(null);
				List<SearchResult> refexes = indexer.query(new DynamicSememeStringImpl(componentNid + (uuid == null ? "" : " OR " + uuid)),
						false, new Integer[] {associationTypeSequenece}, new Integer[] {colIndex}, Integer.MAX_VALUE, null);
				for (SearchResult sr : refexes)
				{
					@SuppressWarnings("rawtypes")
					Optional<LatestVersion<DynamicSememe>> latest = Get.sememeService().getSnapshot(DynamicSememe.class, 
							stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp).getLatestSememeVersion(sr.getNid());
					
					if (latest.isPresent())
					{
						result.add(AssociationInstance.read(latest.get().value(), stamp));
					}
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	/**
	 * 
	 * @param associationTypeConceptNid
	 * @param stamp - optional - if not provided, uses the default from the config service
	 * @return
	 */
	public static List<AssociationInstance> getAssociationsOfType(int associationTypeConceptNid, StampCoordinate stamp)
	{
		ArrayList<AssociationInstance> results = new ArrayList<>();
		Get.sememeService().getSememesFromAssemblage(associationTypeConceptNid)
			.forEach(associationC -> 
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)associationC).getLatestVersion(DynamicSememe.class, 
							stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp);
					if (latest.isPresent())
					{
						results.add(AssociationInstance.read(latest.get().value(), stamp));
					}
					
				});
		return results;
	}

	/**
	 * Get a list of all of the concepts that identify a type of association - returning their concept sequence identifier.
	 * @return
	 */
	public static List<Integer> getAssociationConceptSequences()
	{
		ArrayList<Integer> result = new ArrayList<>();

		Get.sememeService().getSememesFromAssemblage(DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getSequence()).forEach(associationC ->
		{
			result.add(Get.identifierService().getConceptSequence(associationC.getReferencedComponentNid()));
		});
		return result;
	}

	/**
	 * @param assemblageNidOrSequence
	 */
	protected static int findTargetColumnIndex(int assemblageNidOrSequence)
	{
		DynamicSememeUsageDescription rdud = LookupService.get().getService(DynamicSememeUtilityImpl.class).readDynamicSememeUsageDescription(assemblageNidOrSequence);

		for (DynamicSememeColumnInfo rdci : rdud.getColumnInfo())
		{
			if (rdci.getColumnDescriptionConcept().equals(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT.getUUID()))
			{
				return rdci.getColumnOrder();
			}
		}
		return Integer.MIN_VALUE;
	}
}

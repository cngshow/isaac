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
package gov.va.isaac.ochre.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.query.provider.lucene.LuceneDescriptionType;

/**
 * {@link Search}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class Search
{
	/**
	 * Launch a search in a background thread (returns immediately) handing back a handle to the search.
	 * @param searchString - the query string
	 * @param operationToRunWhenSearchComplete - (optional) Pass the function that you want to have executed when the search is complete and the results 
	 * are ready for use.  Note that this function will also be executed in the background thread.
	 * @param descriptionType - (optional) if provided, only searches within the specified description type
	 * @param advancedDescriptionType - (optional) if provided, only searches within the specified advanced description type.  
	 * When this parameter is provided, the descriptionType parameter is ignored.
	 * @param targetCodeSystemPathNidOrSequence - (optional) Restrict the results to concepts from the specified path. 
	 * @param memberOfRefsetNid - (optional) Restrict the results to concepts that are members of the specified refset.
	 * @param kindOfNid - (optional) restrict the results to concepts that are a kind of the specified concept
	 * @param childOfNid - (optional) restrict the results to concepts that are children of the specified concept
	 * @return - A handle to the running search.
	 * @throws IOException
	 */
	public static SearchHandle search(String searchString, Consumer<SearchHandle> operationToRunWhenSearchComplete, LuceneDescriptionType descriptionType, 
			UUID advancedDescriptionType, Integer targetCodeSystemPathNidOrSequence, Integer memberOfRefsetNid, Integer kindOfNid) throws IOException
	{
		ArrayList<Function<List<CompositeSearchResult>, List<CompositeSearchResult>>> filters = new ArrayList<>();
		
		if (targetCodeSystemPathNidOrSequence != null)
		{
			int pathFilterSequence = targetCodeSystemPathNidOrSequence < 0 ? Get.identifierService().getConceptSequence(targetCodeSystemPathNidOrSequence)
					: targetCodeSystemPathNidOrSequence;

			filters.add(new Function<List<CompositeSearchResult>, List<CompositeSearchResult>>()
			{
				@Override
				public List<CompositeSearchResult> apply(List<CompositeSearchResult> t)
				{
					ArrayList<CompositeSearchResult> keep = new ArrayList<>();
					
					for (CompositeSearchResult csr : t)
					{
						if (csr.getContainingConcept().isPresent() && csr.getContainingConcept().get().getPathSequence() == pathFilterSequence)
						{
							keep.add(csr);
						}
					}
					return keep;
				}
			});
		}
		
		if (memberOfRefsetNid != null)
		{
			filters.add(new Function<List<CompositeSearchResult>, List<CompositeSearchResult>>()
			{
				@Override
				public List<CompositeSearchResult> apply(List<CompositeSearchResult> t)
				{
					try
					{
						ArrayList<CompositeSearchResult> keep = new ArrayList<>();
						HashSet<Integer> refsetMembers = new HashSet<>();

						Get.sememeService().getSememesFromAssemblage(Get.identifierService().getSememeSequence(memberOfRefsetNid)).forEach(sememeC ->
						{
							refsetMembers.add(sememeC.getReferencedComponentNid());
						});
						
						for (CompositeSearchResult csr : t)
						{
							if (csr.getContainingConcept().isPresent() && refsetMembers.contains(csr.getContainingConcept().get().getNid()))
							{
								keep.add(csr);
							}
						}
						return keep;
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				}
			});
		}
		
		if (kindOfNid != null)
		{
			filters.add(new Function<List<CompositeSearchResult>, List<CompositeSearchResult>>()
			{
				@Override
				public List<CompositeSearchResult> apply(List<CompositeSearchResult> t)
				{
					ArrayList<CompositeSearchResult> keep = new ArrayList<>();
					
					for (CompositeSearchResult csr : t)
					{
						if (csr.getContainingConcept().isPresent() && Get.taxonomyService().wasEverKindOf(csr.getContainingConcept().get().getNid(), kindOfNid))
						{
							keep.add(csr);
						}
					}
					return keep;
				}
			});
		}
		
		SearchResultsIntersectionFilter filterSet = (filters.size() > 0 ? new SearchResultsIntersectionFilter(filters) : null);
		
		//TODO At some point, Dan needs to update this to avoid the query processor when we are automating the query generation
		//we also need to more consistently handle characters like [ and ( when they are going into the query parser
		//but that is a problem bigger than just the usage in mapping.
		searchString = SearchStringProcessor.prepareSearchString(searchString);
		
		if (descriptionType == null && advancedDescriptionType == null)
		{
			return SearchHandler.descriptionSearch(searchString, 500, false, (UUID)null, operationToRunWhenSearchComplete, null, filterSet, null, true, false);
		}
		else if (advancedDescriptionType != null)
		{
			return SearchHandler.descriptionSearch(searchString, 500, false, advancedDescriptionType, operationToRunWhenSearchComplete, null, filterSet, null, true, false);
		}
		else if (descriptionType != null)
		{
			return SearchHandler.descriptionSearch(searchString, 500, false, descriptionType, operationToRunWhenSearchComplete, null, filterSet, null, true, false);
		}
		else
		{
			throw new RuntimeException("Logic failure!");
		}
	}
	
	/**
	 * Launch a search in a background thread (returns immediately) handing back a handle to the search.
	 * @param sourceConceptNid - the source concept of the map - the descriptions of this concept will be used to create a search
	 * @param operationToRunWhenSearchComplete - (optional) Pass the function that you want to have executed when the search is complete and the results 
	 * are ready for use.  Note that this function will also be executed in the background thread.
	 * @param descriptionType - (optional) if provided, only searches within the specified description type
	 * @param advancedDescriptionType - (optional) if provided, only searches within the specified advanced description type.  
	 * When this parameter is provided, the descriptionType parameter is ignored.
	 * @param targetCodeSystemPathNid - (optional) Restrict the results to concepts from the specified path. 
	 * @param memberOfRefsetNid - (optional) Restrict the results to concepts that are members of the specified refset.
	 * @param childOfNid - (optional) restrict the results to concepts that are children of the specified concept
	 * @param stampCoord - (optional) - use this stamp coordinate for fetching descriptions to build the search - otherwise, uses the default stamp coord.
	 * @return - A handle to the running search.
	 * @throws IOException
	 */
	public static SearchHandle search(int sourceConceptNid, Consumer<SearchHandle> operationToRunWhenSearchComplete, LuceneDescriptionType descriptionType, 
			UUID advancedDescriptionType, Integer targetCodeSystemPathNid, Integer memberOfRefsetNid, Integer kindOfNid, StampCoordinate stampCoord) throws IOException
	{
		StringBuilder searchString;
		searchString = new StringBuilder();
		
		Get.sememeService().getDescriptionsForComponent(sourceConceptNid).forEach(descriptionC ->
		{
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Optional<LatestVersion<DescriptionSememe<?>>> latest = ((SememeChronology)descriptionC).getLatestVersion(DescriptionSememe.class, 
					stampCoord == null ? Get.configurationService().getDefaultStampCoordinate() : stampCoord);
			
			if (latest.isPresent())
			{
				searchString.append(latest.get().value().getText());
				searchString.append(" ");
			}
			
		});
		
		return search(searchString.toString(), operationToRunWhenSearchComplete, descriptionType, advancedDescriptionType, targetCodeSystemPathNid, memberOfRefsetNid, kindOfNid);
	}
}

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

/**
 * SearchBuilder
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
package gov.va.isaac.ochre.search;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import gov.vha.isaac.ochre.api.util.TaskCompleteCallback;

public class SearchBuilder {
	String query;
	Integer sizeLimit = Integer.MAX_VALUE;
	Integer taskId;
	boolean prefixSearch;
	boolean mergeResultsOnConcept = false;
	Function<List<CompositeSearchResult>, List<CompositeSearchResult>> filter = null;
	
	Comparator<CompositeSearchResult> comparator;
	TaskCompleteCallback callback;
	
	// Description search builder factory methods
	public static SearchBuilder descriptionSearchBuilder(String query) {
		SearchBuilder search = new SearchBuilder();
		
		search.setQuery(query);
		search.setPrefixSearch(false);
		search.setComparator(new CompositeSearchResultComparator());
		
		return search;
	}
	public static SearchBuilder descriptionPrefixSearchBuilder(String query) {
		SearchBuilder search = new SearchBuilder();
		
		search.setQuery(query);
		search.setPrefixSearch(true);
		search.setComparator(new CompositeSearchResultComparator());
		
		return search;
	}

	// Concept search builder factory methods
	public static SearchBuilder conceptDescriptionSearchBuilder(String query) {
		SearchBuilder search = new SearchBuilder();
		
		search.setQuery(query);
		search.setPrefixSearch(false);
		search.setComparator(new CompositeSearchResultComparator());
		
		return search;
	}
	public static SearchBuilder conceptPrefixSearchBuilder(String query) {
		SearchBuilder search = new SearchBuilder();
		
		search.setQuery(query);
		search.setPrefixSearch(true);
		search.setComparator(new CompositeSearchResultComparator());
		
		return search;
	}
	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * @param query the query to set
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * @return the sizeLimit
	 */
	public Integer getSizeLimit() {
		return sizeLimit;
	}

	/**
	 * @param sizeLimit the sizeLimit to set
	 */
	public void setSizeLimit(Integer sizeLimit) {
		this.sizeLimit = sizeLimit;
	}

	/**
	 * @return the taskId
	 */
	public Integer getTaskId() {
		return taskId;
	}

	/**
	 * @param taskId the taskId to set
	 */
	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}

	/**
	 * @return the prefixSearch
	 */
	public boolean isPrefixSearch() {
		return prefixSearch;
	}

	/**
	 * @param prefixSearch the prefixSearch to set
	 */
	public void setPrefixSearch(boolean prefixSearch) {
		this.prefixSearch = prefixSearch;
	}
	
	/**
	 * @return the mergeResultsOnConcept
	 */
	public boolean getMergeResultsOnConcept()
	{
		return mergeResultsOnConcept;
	}
	/**
	 * @param mergeResultsOnConcept the mergeResultsOnConcept to set
	 */
	public void setMergeResultsOnConcept(boolean mergeResultsOnConcept)
	{
		this.mergeResultsOnConcept = mergeResultsOnConcept;
	}
	/**
	 * @return the filter
	 */
	public Function<List<CompositeSearchResult>, List<CompositeSearchResult>> getFilter() {
		return filter;
	}
	/**
	 * @param filter the SearchResultsFilter to set
	 */
	public void setFilter(Function<List<CompositeSearchResult>, List<CompositeSearchResult>> filter) {
		this.filter = filter;
	}
	
	/**
	 * @return the comparator
	 */
	public Comparator<CompositeSearchResult> getComparator() {
		return comparator;
	}

	/**
	 * @param comparator the comparator to set
	 */
	public void setComparator(Comparator<CompositeSearchResult> comparator) {
		this.comparator = comparator;
	}

	/**
	 * @return the callback
	 */
	public TaskCompleteCallback getCallback() {
		return callback;
	}

	/**
	 * @param callback the callback to set
	 */
	public void setCallback(TaskCompleteCallback callback) {
		this.callback = callback;
	}
}
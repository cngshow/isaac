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
 *	 http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.ochre.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.identity.IdentifiedObject;
import gov.vha.isaac.ochre.impl.utility.Frills;

/**
 * Encapsulates a data store search result.
 * <p>
 * Logic has been mostly copied from LEGO {@code SnomedSearchResult}.
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class CompositeSearchResult {

	private Optional<ConceptSnapshot> containingConcept = null;
	private final Set<ObjectChronology<?>> matchingComponents = new HashSet<>();
	private int matchingComponentNid_;
	private float bestScore; // best score, rather than score, as multiple matches may go into a SearchResult
	
	private static final Logger LOG = LoggerFactory.getLogger(CompositeSearchResult.class);

	public CompositeSearchResult(ObjectChronology<?> matchingComponent, float score) {
		this.matchingComponents.add(matchingComponent);
		this.bestScore = score;
		//matchingComponent may be null, if the match is not on our view path...
		if (matchingComponent == null) {
			throw new RuntimeException("Please call the constructor that takes a nid, if matchingComponent is null...");
		} else {
			this.matchingComponentNid_ = matchingComponent.getNid();
		}
		this.containingConcept = locateContainingConcept(matchingComponent.getNid());
		
	}
	public CompositeSearchResult(int matchingComponentNid, float score) {
		this.bestScore = score;
		//matchingComponent may be null, if the match is not on our view path...
		this.containingConcept = Optional.empty();
		this.matchingComponentNid_ = matchingComponentNid;
		
	}
	
	protected void adjustScore(float newScore) {
		bestScore = newScore;
	}

	public float getBestScore() {
		return bestScore;
	}
	
	/**
	 * This may return an empty, if the concept and/or matching component was not on the path
	 */
	public Optional<ConceptSnapshot> getContainingConcept() {
		return containingConcept;
	}

	/**
	 * A convenience method to get string values from the matching Components
	 */
	public List<String> getMatchingStrings(Optional<StampCoordinate> stampCoord) {
		ArrayList<String> strings = new ArrayList<>();
		if (matchingComponents.size() == 0)
		{
			if (!containingConcept.isPresent())
			{
				strings.add("Match to NID (not on path):" + matchingComponentNid_);
			}
			else
			{
				throw new RuntimeException("Unexpected");
			}
		}
		for (IdentifiedObject iol : matchingComponents)
		{
			if (iol instanceof ConceptChronology<?>)
			{
				//This means they matched on a UUID or other ID lookup.
				//Return UUID for now - matches on other ID types will be handled differently 
				//in the near future - so ignore the SCTID case for now.
				strings.add(iol.getPrimordialUuid().toString());
			}
			else if (iol instanceof SememeChronology<?> && ((SememeChronology<?>)iol).getSememeType() == SememeType.DESCRIPTION)
			{
				Optional<LatestVersion<DescriptionSememe>> ds = ((SememeChronology<DescriptionSememe>)iol).getLatestVersion(DescriptionSememe.class, 
						stampCoord.orElse(Get.configurationService().getDefaultStampCoordinate()));
				if (ds.isPresent())
				{
					strings.add(ds.get().value().getText());
				}
				else
				{
					strings.add("No description available on stamp coordinate!");
				}
			}
			else if (iol instanceof SememeChronology<?> && ((SememeChronology<?>)iol).getSememeType() == SememeType.STRING)
			{
				Optional<LatestVersion<StringSememe>> ds = ((SememeChronology<StringSememe>)iol).getLatestVersion(StringSememe.class, 
						stampCoord.orElse(Get.configurationService().getDefaultStampCoordinate()));
				if (ds.isPresent())
				{
					strings.add(ds.get().value().getString());
				}
				else
				{
					strings.add("No sememe available on stamp coordinate!");
				}
			}
			else if (iol instanceof SememeChronology<?> && ((SememeChronology<?>)iol).getSememeType() == SememeType.DYNAMIC)
			{
				Optional<LatestVersion<DynamicSememe>> ds = ((SememeChronology<DynamicSememe>)iol).getLatestVersion(DynamicSememe.class, 
						stampCoord.orElse(Get.configurationService().getDefaultStampCoordinate()));
				if (ds.isPresent())
				{
					strings.add(ds.get().value().dataToString());
				}
				else
				{
					strings.add("No sememe available on stamp coordinate!");
				}
			}
			else
			{
				strings.add("ERROR: No string extractor available for " + iol.getClass().getName());
			}
		}
		return strings;
	}

	public Set<ObjectChronology<?>> getMatchingComponents() {
		return matchingComponents;
	}
	
	/**
	 * Convenience method to return a filtered list of matchingComponents such that it only returns
	 * Description type components
	 */
	public Set<SememeChronology<DescriptionSememe>> getMatchingDescriptionComponents() {
		Set<SememeChronology<DescriptionSememe>> setToReturn = new HashSet<>();
		for (ObjectChronology<?> comp : matchingComponents) {
			if (comp instanceof SememeChronology<?> && ((SememeChronology<?>) comp).getSememeType() == SememeType.DESCRIPTION) {
				setToReturn.add(((SememeChronology<DescriptionSememe>)comp));
			}
		}
		
		return Collections.unmodifiableSet(setToReturn);
	}
	
	protected void merge(CompositeSearchResult other)
	{
		if (containingConcept.get().getNid() !=  other.containingConcept.get().getNid())
		{
			throw new RuntimeException("Unmergeable!");
		}
		if (other.bestScore > bestScore)
		{
			bestScore = other.bestScore;
		}
		matchingComponents.addAll(other.getMatchingComponents());
	}
	
	public String toShortString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompositeSearchResult [containingConcept=");
		builder.append(containingConcept.isPresent() ? containingConcept.get().getNid() : null);
		
		if (matchingComponentNid_ != 0) {
			builder.append(", matchingComponentNid_=");
			builder.append(matchingComponentNid_);
		}

		builder.append(", bestScore=");
		builder.append(bestScore);

		if (matchingComponents != null && matchingComponents.size() > 0) {
			builder.append(", matchingComponents=");
			List<Integer> matchingComponentNids = new ArrayList<>();
			for (ObjectChronology<?> matchingComponent : matchingComponents) {
				matchingComponentNids.add(matchingComponent != null ? matchingComponent.getNid() : null);
			}
			builder.append(matchingComponentNids);
		}

		builder.append("]");
		return builder.toString();
	}

	public String toStringWithDescriptions() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompositeSearchResult [containingConcept=");
		String containingConceptDesc = null;
		if (containingConcept.isPresent()) {
			try {
				containingConceptDesc = containingConcept.get().getConceptDescriptionText();
			} catch (Exception e) {
				containingConceptDesc = "{nid=" + containingConcept.get().getNid() + "}";
			}
		}
		builder.append(containingConceptDesc);

		builder.append(", matchingComponentNid_=");
		builder.append(matchingComponentNid_);
		
		String matchingComponentDesc = null;
		if (matchingComponentNid_ != 0) {
			try {
				Optional<? extends ObjectChronology<?>> cc =  Get.identifiedObjectService().getIdentifiedObjectChronology(matchingComponentNid_);
				if (cc.isPresent()) {
					matchingComponentDesc = cc.get().toUserString();
				}
			} catch (Exception e) {
				LOG.warn("Unexpected:", e);
			}
		}
		if (matchingComponentDesc != null) {
			builder.append(", matchingComponent=");
			builder.append(matchingComponentDesc);
		}

		builder.append(", bestScore=");
		builder.append(bestScore);

		builder.append(", numMatchingComponents=");
		List<Integer> matchingComponentNids = new ArrayList<>();
		for (ObjectChronology<?> matchingComponent : getMatchingComponents()) {
			matchingComponentNids.add(matchingComponent != null ? matchingComponent.getNid() : null);
		}
		builder.append(matchingComponentNids);

		builder.append("]");
		return builder.toString();
	}
	public String toLongString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompositeSearchResult [containingConcept=");
		builder.append(containingConcept.isPresent() ? containingConcept.get() : "null");
		builder.append(", matchingComponentNid_=");
		builder.append(matchingComponentNid_);
		builder.append(", bestScore=");
		builder.append(bestScore);
		builder.append(", getMatchingComponents()=");
		List<String> matchingComponentDescs = new ArrayList<>();
		for (ObjectChronology<?> matchingComponent : getMatchingComponents()) {
			matchingComponentDescs.add(matchingComponent != null ? matchingComponent.toUserString() : null);
		}
		builder.append(matchingComponentDescs);
		
		builder.append("]");
		return builder.toString();
	}
	
	private Optional<ConceptSnapshot> locateContainingConcept(int componentNid)
	{
		ObjectChronologyType type = Get.identifierService().getChronologyTypeForNid(componentNid);
		if (type == ObjectChronologyType.UNKNOWN_NID)
		{
			return Optional.empty();
		}
		else if (type == ObjectChronologyType.CONCEPT)
		{
			return Frills.getConceptSnapshot(componentNid, null, null);
		}
		else if (type == ObjectChronologyType.SEMEME)
		{
			return locateContainingConcept(Get.sememeService().getSememe(componentNid).getReferencedComponentNid());
		}
		else
		{
			throw new RuntimeException("oops");
		}
	}
	
	public static int removeNullResults(Collection<CompositeSearchResult> results) {
		Collection<CompositeSearchResult> nullResults = new ArrayList<>();
		if (results != null) {
			for (CompositeSearchResult result : results) {
				if (result.getContainingConcept() == null) {
					nullResults.add(result);
				}
			}
			results.removeAll(nullResults);
		}
		return nullResults.size();
	}
}

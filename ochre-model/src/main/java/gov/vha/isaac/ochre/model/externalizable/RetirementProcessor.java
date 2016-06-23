package gov.vha.isaac.ochre.model.externalizable;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;

public class RetirementProcessor {

	private Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap;
	private Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap;
	private Set<UUID> matchedSet;

	public RetirementProcessor(Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap, Set<UUID> matchedSet) {
		this.oldContentMap = oldContentMap;
		this.matchedSet = matchedSet;
	}

	public List<OchreExternalizable> identifyRetiredContent(IbdfDiffUtility diffUtil, int inactiveStampSeq) {
		List<OchreExternalizable> retiredComponents = new ArrayList<>();
		
		Set<OchreExternalizable> conceptsToRetire = identifyComponentsToRetire(OchreExternalizableObjectType.CONCEPT);
		Set<OchreExternalizable> sememesToRetire = identifyComponentsToRetire(OchreExternalizableObjectType.SEMEME);
		
		// Ensure retire child before parent
		List<OchreExternalizable> orderedConcepts = orderConceptsToRetire(conceptsToRetire);
//		List<OchreExternalizable> orderedSememes = orderSememesToRetire(sememesToRetire);
		
//		retiredComponents.addAll(retireComponents(orderedSememes, diffUtil, inactiveStampSeq));
		retiredComponents.addAll(retireComponents(orderedConcepts, diffUtil, inactiveStampSeq));
		
		return retiredComponents;
	}

	private List<OchreExternalizable> orderConceptsToRetire(Set<OchreExternalizable> conceptsToRetire) {
		List<OchreExternalizable> orderedRetirees = new ArrayList<>();

		while (!conceptsToRetire.isEmpty()) {
			Set<ConceptChronology<?>> retireesThisIteration = new HashSet<>();

			for (OchreExternalizable externalizableConcept : conceptsToRetire) {
				ConceptChronology<?> con = (ConceptChronology<?>)externalizableConcept;
				
				if (!hasChildren(con)) {
					retireesThisIteration.add(con);
				}
			}
			
			for (ConceptChronology<?> con : retireesThisIteration) {
				orderedRetirees.add(con);
				conceptsToRetire.remove(con);
			}
		}
		
		return orderedRetirees;
	}



	@SuppressWarnings("unchecked")
	public static Optional<LatestVersion<? extends RelationshipVersionAdaptor<?>>> getLatestRelationshipVersionAdaptor(
			SememeChronology<? extends RelationshipVersionAdaptor<?>> sememeChronology, StampCoordinate stampCoordinate) {
		@SuppressWarnings("rawtypes")
		SememeChronology rawSememChronology = (SememeChronology)sememeChronology;
		
		return rawSememChronology.getLatestVersion(RelationshipVersionAdaptor.class, stampCoordinate);
	}

	
	
	private boolean hasChildren(ConceptChronology<?> con) {
		List<? extends SememeChronology<? extends RelationshipVersionAdaptor<?>>> outgoingRelChronicles = con.getRelationshipListWithConceptAsDestination();
		
		for (SememeChronology<? extends RelationshipVersionAdaptor<?>> rel : outgoingRelChronicles) {
			Optional<LatestVersion<? extends RelationshipVersionAdaptor<?>>> latest = getLatestRelationshipVersionAdaptor(rel, 
					StampCoordinates.getDevelopmentLatestActiveOnly());
			
			if (latest.isPresent() && latest.get().value() != null) {
				return true;
			}
		}

		return false;
	}

	private List<OchreExternalizable> retireComponents(List<OchreExternalizable> orderedComponents, IbdfDiffUtility diffUtil, int inactiveStampSeq) {
		List<OchreExternalizable> retiredComponents = new ArrayList<>();

		for (OchreExternalizable comp : orderedComponents) {
    		try {
    			retiredComponents.add(diffUtil.addNewInactiveVersion(comp, comp.getOchreObjectType(), inactiveStampSeq));
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
		}	
		
		return retiredComponents;
	}

	private Set<OchreExternalizable> identifyComponentsToRetire(OchreExternalizableObjectType type) {
		Set<OchreExternalizable> componentsToRetire = new HashSet<>();
		
		for (OchreExternalizable oldComp : oldContentMap.get(type)) {
			if (!matchedSet.contains(((ObjectChronology<?>) oldComp).getPrimordialUuid())) {
				componentsToRetire.add(oldComp);
			}
		}

		return componentsToRetire;
	}

	public void printRetiredContent(List<OchreExternalizable> retiredComponents, Writer retiredWriter) {
		for (OchreExternalizable comp : retiredComponents) {
    		try {
    			retiredWriter.write(comp.toString());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
		}
	}
}

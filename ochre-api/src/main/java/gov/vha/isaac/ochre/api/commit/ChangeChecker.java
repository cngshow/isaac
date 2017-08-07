/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.vha.isaac.ochre.api.commit;

import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;

import java.util.Collection;

/**
 * This must be comparable, because it gets used in ConcurrentSkipListSet in the CommitProvider, which assumes things are comparable
 * @author kec
 */
public interface ChangeChecker extends Comparable<ChangeChecker>{
    
    void check(ConceptChronology<? extends ConceptVersion<?>> cc, 
               Collection<Alert> alertCollection,
               CheckPhase checkPhase);
    
    void check(SememeChronology<? extends SememeVersion<?>> sc, 
               Collection<Alert> alertCollection,
               CheckPhase checkPhase);
    
    /**
     * @return the desired ordering of your change checker, lower numbers execute first.  Used in the implementation of comparable.
     */
    default int getRank() {
        return 1;
    }

    /**
     * Sorts based on {@link #getRank()}
     */
    @Override
    default int compareTo(ChangeChecker o) {
        return Integer.compare(getRank(), o.getRank());
    }
}

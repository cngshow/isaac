/*
 * Copyright 2015 kec.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.component.concept;

import gov.vha.isaac.ochre.api.DatabaseServices;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jvnet.hk2.annotations.Contract;

/**
 *
 * @author kec
 */
@Contract
public interface ConceptService extends DatabaseServices {
    
    /**
     * 
     * @param conceptId either a concept sequence or a concept nid. 
     * @return the concept chronology associated with the identifier. 
     */
    ConceptChronology<? extends ConceptVersion<?>> getConcept(int conceptId);
    
    /**
     * 
     * @param conceptUuids a UUID that identifies a concept.
     * @return the concept chronology associated with the identifier. 
     */
    ConceptChronology<? extends ConceptVersion<?>> getConcept(UUID... conceptUuids);
    
    /**
     * Use in circumstances when not all concepts may have been loaded. 
     * @param conceptId Either a nid or concept sequence
     * @return an Optional ConceptChronology.
     */
    Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> getOptionalConcept(int conceptId);
    
    /**
     * Use in circumstances when not all concepts may have been loaded to find out if a concept is present,
     * without incurring the overhead of reading back the object. 
     * @param conceptId Either a nid or concept sequence
     * @return true if present, false otherwise
     */
    boolean hasConcept(int conceptId);
    
    /**
     * Use in circumstances when not all concepts may have been loaded. 
     * @param conceptUuids uuids that identify the concept
     * 
     * This implementation should not have a side effect of adding the UUID to any indexes, if the UUID isn't yet present.
     * @return an Optional ConceptChronology.
     */
    Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> getOptionalConcept(UUID... conceptUuids);

    /**
     * Write a concept to the concept service. Will not overwrite a concept if one already exists, rather it will
     * merge the written concept with the provided concept.
     *
     *
     * The persistence of the concept is dependent on the persistence
     * of the underlying service.
     * @param concept to be written.
     */
    void writeConcept(ConceptChronology<? extends ConceptVersion<?>> concept);

    boolean isConceptActive(int conceptSequence, StampCoordinate stampCoordinate);
    
    ConceptSnapshotService getSnapshot(StampCoordinate stampCoordinate, LanguageCoordinate languageCoordinate);

    int getConceptCount();
    
    Stream<ConceptChronology<? extends ConceptVersion<?>>> getConceptChronologyStream();
    Stream<ConceptChronology<? extends ConceptVersion<?>>> getParallelConceptChronologyStream();
    
    IntStream getConceptKeyStream();
    
    IntStream getConceptKeyParallelStream();

    Stream<ConceptChronology<? extends ConceptVersion<?>>> getConceptChronologyStream(ConceptSequenceSet conceptSequences);
    Stream<ConceptChronology<? extends ConceptVersion<?>>> getParallelConceptChronologyStream(ConceptSequenceSet conceptSequences);
    
}

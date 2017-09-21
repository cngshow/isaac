/*
 * Copyright 2015 U.S. Department of Veterans Affairs.
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
package gov.vha.isaac.ochre.model.builder;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.BiConsumer;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilder;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.task.OptionalWaitTask;
import gov.vha.isaac.ochre.api.util.UuidFactory;
import gov.vha.isaac.ochre.model.sememe.SememeChronologyImpl;
import gov.vha.isaac.ochre.model.sememe.version.DescriptionSememeImpl;

/**
 *
 * @author kec
 * @param <T>
 * @param <V>
 */
public class DescriptionBuilderOchreImpl<T extends SememeChronology<V>, V extends DescriptionSememeImpl> extends 
            ComponentBuilder<T> 
    implements DescriptionBuilder<T,V> {

    private final HashMap<ConceptSpecification, SememeBuilder<?>> preferredInDialectAssemblages = new HashMap<>();
    private final HashMap<ConceptSpecification, SememeBuilder<?>> acceptableInDialectAssemblages = new HashMap<>();
    
    private final String descriptionText;
    private final ConceptSpecification descriptionType;
    private final ConceptSpecification languageForDescription;
    private final ConceptBuilder conceptBuilder;
    private int conceptSequence = Integer.MAX_VALUE;

    public DescriptionBuilderOchreImpl(String descriptionText, 
            int conceptSequence,
            ConceptSpecification descriptionType, 
            ConceptSpecification languageForDescription) {
        this.descriptionText = descriptionText;
        this.conceptSequence = conceptSequence;
        this.descriptionType = descriptionType;
        this.languageForDescription = languageForDescription;
        this.conceptBuilder = null;
    }
    public DescriptionBuilderOchreImpl(String descriptionText, 
            ConceptBuilder conceptBuilder,
            ConceptSpecification descriptionType, 
            ConceptSpecification languageForDescription) {
        this.descriptionText = descriptionText;
        this.descriptionType = descriptionType;
        this.languageForDescription = languageForDescription;
        this.conceptBuilder = conceptBuilder;
    }

    @Override
    public DescriptionBuilder addPreferredInDialectAssemblage(ConceptSpecification dialectAssemblage) {
        preferredInDialectAssemblages.put(dialectAssemblage, null);
        return this; 
   }

    @Override
    public DescriptionBuilder addAcceptableInDialectAssemblage(ConceptSpecification dialectAssemblage) {
        acceptableInDialectAssemblages.put(dialectAssemblage, null);
        return this;
    }

    @Override
    public OptionalWaitTask<T> build(EditCoordinate editCoordinate, ChangeCheckerMode changeCheckerMode, 
            List<ObjectChronology<? extends StampedVersion>> builtObjects) throws IllegalStateException {
        if (conceptSequence == Integer.MAX_VALUE) {
            conceptSequence = Get.identifierService().getConceptSequenceForUuids(conceptBuilder.getUuids());
        }
        ArrayList<OptionalWaitTask<?>> nestedBuilders = new ArrayList<>();
        SememeBuilderService sememeBuilder = LookupService.getService(SememeBuilderService.class);
        SememeBuilder<? extends SememeChronology<? extends DescriptionSememe>> descBuilder
                = sememeBuilder.getDescriptionSememeBuilder(Get.languageCoordinateService().caseSignificanceToConceptSequence(false),
                        languageForDescription.getConceptSequence(),
                        descriptionType.getConceptSequence(),
                        descriptionText,
                        Get.identifierService().getConceptNid(conceptSequence));
        
        descBuilder.setPrimordialUuid(this.getPrimordialUuid());
        OptionalWaitTask<SememeChronologyImpl<DescriptionSememeImpl>> newDescription = (OptionalWaitTask<SememeChronologyImpl<DescriptionSememeImpl>>)
                descBuilder.setState(state).build(editCoordinate, changeCheckerMode, builtObjects);
        nestedBuilders.add(newDescription);
        
        getSememeBuilders().forEach((builder) -> nestedBuilders.add(builder.build(editCoordinate, changeCheckerMode, builtObjects)));
        
        return new OptionalWaitTask<T>(null, (T)newDescription.getNoWait(), nestedBuilders);
    }

    @Override
    public T build(int stampSequence, List<ObjectChronology<? extends StampedVersion>> builtObjects) throws IllegalStateException {
        if (conceptSequence == Integer.MAX_VALUE) {
            conceptSequence = Get.identifierService().getConceptSequenceForUuids(conceptBuilder.getUuids());
        }
        SememeBuilderService sememeBuilder = LookupService.getService(SememeBuilderService.class);
        
        SememeBuilder<? extends SememeChronology<? extends DescriptionSememe>> descBuilder
                = sememeBuilder.getDescriptionSememeBuilder(
                        TermAux.caseSignificanceToConceptSequence(false),
                        languageForDescription.getConceptSequence(),
                        descriptionType.getConceptSequence(),
                        descriptionText,
                        Get.identifierService().getConceptNid(conceptSequence));
        descBuilder.setPrimordialUuid(this.getPrimordialUuid());
        SememeChronologyImpl<DescriptionSememeImpl> newDescription = (SememeChronologyImpl<DescriptionSememeImpl>)
                descBuilder.build(stampSequence, builtObjects);
         getSememeBuilders().forEach((builder) -> builder.build(stampSequence, builtObjects));
        return (T) newDescription;
    }
    
    @Override
    public DescriptionBuilder setT5Uuid(UUID namespace, BiConsumer<String, UUID> consumer) {
        if (isPrimordialUuidSet() && getPrimordialUuid().version() == 4) {
            throw new RuntimeException("Attempting to set Type 5 UUID where the UUID was previously set to random");
        }
        
        if (!isPrimordialUuidSet()) {
            int assemblageSeq = TermAux.getDescriptionAssemblageConceptSequence(languageForDescription.getConceptSequence());
            int caseSigNid = Get.identifierService().getConceptNid(Get.languageCoordinateService().caseSignificanceToConceptSequence(false));
            
            setPrimordialUuid(
                    UuidFactory.getUuidForDescriptionSememe(namespace,
                            Get.identifierService().getUuidPrimordialFromConceptId(assemblageSeq).get(),
                            conceptBuilder.getPrimordialUuid(), 
                            Get.identifierService().getUuidPrimordialForNid(caseSigNid).get(),
                            descriptionType.getPrimordialUuid(),
                            languageForDescription.getPrimordialUuid(), 
                            descriptionText,
                            consumer));
        }
        
        return this;
    }
    @Override
    public List<SememeBuilder<?>> getSememeBuilders() {
        ArrayList<SememeBuilder<?>> temp = new ArrayList<>(super.getSememeBuilders().size() + preferredInDialectAssemblages.size() 
            + acceptableInDialectAssemblages.size());
        
        temp.addAll(super.getSememeBuilders());
        
        SememeBuilderService sememeBuilderService = LookupService.getService(SememeBuilderService.class);
        
        for (Entry<ConceptSpecification, SememeBuilder<?>> p : preferredInDialectAssemblages.entrySet()) {
            if (p.getValue() == null) {
                p.setValue(sememeBuilderService.getComponentSememeBuilder(TermAux.PREFERRED.getNid(), this,
                    Get.identifierService().getConceptSequenceForProxy(p.getKey())));
            }
            temp.add(p.getValue());
        }
        
        for (Entry<ConceptSpecification, SememeBuilder<?>> a : acceptableInDialectAssemblages.entrySet()) {
            if (a.getValue() == null) {
                a.setValue(sememeBuilderService.getComponentSememeBuilder(TermAux.ACCEPTABLE.getNid(), this,
                    Get.identifierService().getConceptSequenceForProxy(a.getKey())));
            }
            temp.add(a.getValue());
        }
        
        return temp;
    }
    
    
}

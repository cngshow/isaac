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
package gov.vha.isaac.ochre.api;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommittableComponent;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.identity.IdentifiedObject;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.task.OptionalWaitTask;

/**
 * The Interface IdentifiedComponentBuilder.
 *
 * @author kec
 * @param <T> the generic type
 */
public interface IdentifiedComponentBuilder<T extends CommittableComponent> extends IdentifiedObject {
    
    /**
     * If already set, a runtime exception will be thrown.
     * @param uuid the primordial uuid for the component to be built. 
     * @return the builder for chaining of operations in a fluent pattern. 
     */
    IdentifiedComponentBuilder<T> setPrimordialUuid(UUID uuid);
    
    /**
     * If not set, a randomly generated UUID will be automatically used. 
     * @param uuidString the primordial uuid for the component to be built. 
     * @return the builder for chaining of operations in a fluent pattern. 
     */
    default IdentifiedComponentBuilder<T> setPrimordialUuid(String uuidString) {
        return setPrimordialUuid(UUID.fromString(uuidString));
    }
    
    /**
     * Add additional uuids as identifiers for this component. 
     * @param uuids the additional uuids to add as identifiers for this component
     * @return  the builder for chaining of operations in a fluent pattern.
     */
    IdentifiedComponentBuilder<T> addUuids(UUID... uuids);
    
    /**
     * define the state that the component will be created with.  if setState is not called, 
     * the component will be build as active.  Note, this will not impact any nested builders.
     * Nested builders should have their own state set, if you wish to override the default 
     * active value.  This is only used for calls to {@link #build(EditCoordinate, ChangeCheckerMode)}
     * or {@link #build(EditCoordinate, ChangeCheckerMode, List)} (where a active state would otherwise be assumed)
     * It is not used with a call to {@link #build(int, List)}
     *
     * @param state the state
     * @return the builder for chaining of operations in a fluent pattern.
     */
    IdentifiedComponentBuilder<T> setState(State state);

    /**
     * Sets the identifier for authority.
     *
     * @param identifier a string identifier such as a SNOMED CT id, or a LOINC id.
     * @param identifierAuthority a concept that identifies the authority that assigns the identifier.
     * @return the builder for chaining of operations in a fluent pattern.
     */
    IdentifiedComponentBuilder<T> setIdentifierForAuthority(String identifier, 
            ConceptProxy identifierAuthority);
    
    /**
     * Create a component with a state of ACTIVE. 
     *
     * @param editCoordinate the edit coordinate that determines the author, module and path for the change
     * @param changeCheckerMode determines if added to the commit manager with or without checks.
     * @return a task which will return the constructed component after it has been added to the commit manager -
     * the write to the commit manager is not complete until the task is complete (the task has already been launched)
     * @throws IllegalStateException the illegal state exception
     */
    OptionalWaitTask<T> build(EditCoordinate editCoordinate, 
            ChangeCheckerMode changeCheckerMode) throws IllegalStateException;
    
    /**
     * Create a component with a state of ACTIVE. 
     *
     * @param editCoordinate the edit coordinate that determines the author, module and path for the change
     * @param changeCheckerMode determines if added to the commit manager with or without checks.
     * @param subordinateBuiltObjects a list of subordinate objects also build as a result of building this object.  Includes top-level object being built.
     * @return a task which will return the constructed component after it has been added to the commit manager -
     * the write to the commit manager is not complete until the task is complete (the task has already been launched)
     * @throws IllegalStateException the illegal state exception
     */
    OptionalWaitTask<T> build(EditCoordinate editCoordinate, 
            ChangeCheckerMode changeCheckerMode,
            List<ObjectChronology<? extends StampedVersion>> subordinateBuiltObjects) throws IllegalStateException;

    /**
     * The caller is responsible to write the component to the proper store when 
     * all updates to the component are complete. 
     *
     * @param stampSequence the stamp sequence
     * @param builtObjects a list objects build as a result of call build.
     * Includes top-level object being built. 
     * The caller is also responsible to write all build objects to the proper store. 
     * @return the constructed component, not yet written to the database.
     * @throws IllegalStateException the illegal state exception
     */
    T build(int stampSequence, List<ObjectChronology<? extends StampedVersion>> builtObjects) throws IllegalStateException;
 
    /**
     * Add a nested Sememe that should be chained / built when build is called on this component.
     *
     * @param sememeBuilder the sememe builder
     * @return this object
     */
    public IdentifiedComponentBuilder<T> addSememe(SememeBuilder<?> sememeBuilder);

    /**
     * Add a nested membership sememes that should be chained / built when build is called on this component.
     *
     * @param assemblageConcepts the assemblage concepts
     * @return this object
     */
    default public IdentifiedComponentBuilder<T> addAssemblageMembership(IdentifiedObject...assemblageConcepts) {
        for (IdentifiedObject obj : assemblageConcepts) {
            addSememe(Get.sememeBuilderService().getMembershipSememeBuilder(this, obj.getNid()));
        }
        return this;
    }

    /**
     * Gets the sememe builders stored by the builder.
     * This should include any 'special' builders, like logical expression builders
     * @return the sememe builders
     */
    List<SememeBuilder<?>> getSememeBuilders();

    /**
     * Throws runtime exception if Primordial UUID has been set and is random (t4).
     * Does nothing if UUID has already been set to a non-random (Not a Type 4 UUID) value.
     * 
     * @param namespace - what namespace to use to generate the UUIDs
     * @param consumer - an optional function that can be passed in.  Has no impact on the UUID generation.  Implementors of 
     * the method will receive the UUID seed string into the consumer during generation (useful as a debug aid), and the resulting UUID
     *
     * @return the identified component builder
     */
    public IdentifiedComponentBuilder<T> setT5Uuid(UUID namespace, BiConsumer<String, UUID> consumer);

    /**
     * Returns true if the primordial UUID has already been set.
     *
     * @return true, if is primordial uuid set
     */
    public boolean isPrimordialUuidSet();
}

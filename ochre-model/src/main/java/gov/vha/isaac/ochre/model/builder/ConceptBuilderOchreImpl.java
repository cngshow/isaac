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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY_STATE_SET KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.model.builder;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilder;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilderService;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.LogicCoordinate;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.task.OptionalWaitTask;
import gov.vha.isaac.ochre.model.concept.ConceptChronologyImpl;
import javafx.concurrent.Task;

/**
 *
 * @author kec
 */
public class ConceptBuilderOchreImpl extends ComponentBuilder<ConceptChronology<?>> implements ConceptBuilder {

    private final String conceptName;
    private final String semanticTag;
    private final ConceptSpecification defaultLanguageForDescriptions;
    private final ConceptSpecification defaultDialectAssemblageForDescriptions;
    private final LogicCoordinate defaultLogicCoordinate;
    private final List<DescriptionBuilder<?, ?>> descriptionBuilders = new ArrayList<>();
    private final List<LogicalExpressionBuilder> logicalExpressionBuilders = new ArrayList<>();
    private final List<LogicalExpression> logicalExpressions = new ArrayList<>();
    
    private transient DescriptionBuilder<?, ?> fsnDescriptionBuilder = null;
    private transient DescriptionBuilder<?, ?> preferredDescriptionBuilder = null;

    /**
     * @param conceptName - Optional - if specified, a FSN will be created using this value (but see additional information on semanticTag)
     * @param semanticTag - Optional - if specified, conceptName must be specified, and two descriptions will be created using the following forms: 
     *   FSN: -     "conceptName (semanticTag)"
     *   Preferred: "conceptName"
     * If not specified, only a FSN will be created, using exactly the conceptName value.  No Preferred term will be created.
     * @param logicalExpression - Optional
     * @param defaultLanguageForDescriptions - Optional - used as the language for the created FSN and preferred term
     * @param defaultDialectAssemblageForDescriptions - Optional - used as the language for the created FSN and preferred term
     * @param defaultLogicCoordinate - Optional - used during the creation of the logical expression, if either a logicalExpression 
     * is passed, or if @link {@link #addLogicalExpression(LogicalExpression)} or {@link #addLogicalExpressionBuilder(LogicalExpressionBuilder)} are 
     * used later.
     */
    public ConceptBuilderOchreImpl(String conceptName,
            String semanticTag,
            LogicalExpression logicalExpression,
            ConceptSpecification defaultLanguageForDescriptions,
            ConceptSpecification defaultDialectAssemblageForDescriptions,
            LogicCoordinate defaultLogicCoordinate) {
        this.conceptName = conceptName;
        this.semanticTag = semanticTag;
        this.defaultLanguageForDescriptions = defaultLanguageForDescriptions;
        this.defaultDialectAssemblageForDescriptions = defaultDialectAssemblageForDescriptions;
        this.defaultLogicCoordinate = defaultLogicCoordinate;
        if (logicalExpression != null) {
            this.logicalExpressions.add(logicalExpression);
        }
    }

    @Override
    public DescriptionBuilder<?, ?> getFullySpecifiedDescriptionBuilder() {
        synchronized (this) {
            if (fsnDescriptionBuilder == null && StringUtils.isNotBlank(conceptName)) {
                StringBuilder descriptionTextBuilder = new StringBuilder();
                descriptionTextBuilder.append(conceptName);
                if (StringUtils.isNotBlank(semanticTag)) {
                    descriptionTextBuilder.append(" (");
                    descriptionTextBuilder.append(semanticTag);
                    descriptionTextBuilder.append(")");
                }
                if (defaultLanguageForDescriptions == null || defaultDialectAssemblageForDescriptions == null) {
                    throw new IllegalStateException("language and dialect are required if a concept name is provided");
                }
                fsnDescriptionBuilder = LookupService.getService(DescriptionBuilderService.class).
                    getDescriptionBuilder(descriptionTextBuilder.toString(), this,
                            TermAux.FULLY_SPECIFIED_DESCRIPTION_TYPE,
                            defaultLanguageForDescriptions).
                    addPreferredInDialectAssemblage(defaultDialectAssemblageForDescriptions);
            }
        }
        return fsnDescriptionBuilder;
    }

    @Override
    public DescriptionBuilder<?, ?> getSynonymPreferredDescriptionBuilder() {
        synchronized (this) {
            if (preferredDescriptionBuilder == null && StringUtils.isNotBlank(conceptName) && StringUtils.isNotBlank(semanticTag)) {
                if (defaultLanguageForDescriptions == null || defaultDialectAssemblageForDescriptions == null) {
                    throw new IllegalStateException("language and dialect are required if a concept name is provided");
                }
                preferredDescriptionBuilder = LookupService.getService(DescriptionBuilderService.class).
                getDescriptionBuilder(conceptName, this,
                        TermAux.SYNONYM_DESCRIPTION_TYPE,
                        defaultLanguageForDescriptions).
                addPreferredInDialectAssemblage(defaultDialectAssemblageForDescriptions);
            }
        }
        return preferredDescriptionBuilder;
    }

    @Override
    public ConceptBuilder addDescription(DescriptionBuilder<?, ?> descriptionBuilder) {
        descriptionBuilders.add(descriptionBuilder);
        return this;
    }

    @Override
    public ConceptBuilder addLogicalExpressionBuilder(LogicalExpressionBuilder logicalExpressionBuilder) {
        this.logicalExpressionBuilders.add(logicalExpressionBuilder);
        return this;
    }

    @Override
    public ConceptBuilder addLogicalExpression(LogicalExpression logicalExpression) {
        this.logicalExpressions.add(logicalExpression);
        return this;
    }

    @Override
    public OptionalWaitTask<ConceptChronology<?>> build(EditCoordinate editCoordinate, ChangeCheckerMode changeCheckerMode, List builtObjects) throws IllegalStateException {

        ArrayList<OptionalWaitTask<?>> nestedBuilders = new ArrayList<>();
        ConceptChronologyImpl conceptChronology = (ConceptChronologyImpl) Get.conceptService().getConcept(getUuids());
        conceptChronology.createMutableVersion(state, editCoordinate);
        builtObjects.add(conceptChronology);
        if (getFullySpecifiedDescriptionBuilder() != null) {
            descriptionBuilders.add(getFullySpecifiedDescriptionBuilder());
        }
        if (getSynonymPreferredDescriptionBuilder() != null) {
            descriptionBuilders.add(getSynonymPreferredDescriptionBuilder());
        }
        descriptionBuilders.forEach((builder) -> {
            nestedBuilders.add(builder.build(editCoordinate, changeCheckerMode, builtObjects));
        });
        if (defaultLogicCoordinate == null && (logicalExpressions.size() > 0 || logicalExpressionBuilders.size() > 0)) {
            throw new IllegalStateException("A logic coordinate is required when a logical expression is passed");
        }
        SememeBuilderService builderService = LookupService.getService(SememeBuilderService.class);
        for (LogicalExpression logicalExpression : logicalExpressions) {
            sememeBuilders.add(builderService.
                    getLogicalExpressionSememeBuilder(logicalExpression, this, defaultLogicCoordinate.getStatedAssemblageSequence()));
        }
        for (LogicalExpressionBuilder builder : logicalExpressionBuilders) {
            sememeBuilders.add(builderService.
                    getLogicalExpressionSememeBuilder(builder.build(), this, defaultLogicCoordinate.getStatedAssemblageSequence()));
        }
        sememeBuilders.forEach((builder) -> nestedBuilders.add(builder.build(editCoordinate, changeCheckerMode, builtObjects)));
        Task<Void> primaryNested;
        if (changeCheckerMode == ChangeCheckerMode.ACTIVE) {
            primaryNested = Get.commitService().addUncommitted(conceptChronology);
        } else {
            primaryNested = Get.commitService().addUncommittedNoChecks(conceptChronology);
        }
        return new OptionalWaitTask<ConceptChronology<?>>(primaryNested, conceptChronology, nestedBuilders);
    }

    @Override
    public ConceptChronology build(int stampCoordinate, List builtObjects) throws IllegalStateException {

        ConceptChronologyImpl conceptChronology = (ConceptChronologyImpl) Get.conceptService().getConcept(getUuids());
        conceptChronology.createMutableVersion(stampCoordinate);
        builtObjects.add(conceptChronology);
        if (getFullySpecifiedDescriptionBuilder() != null) {
            descriptionBuilders.add(getFullySpecifiedDescriptionBuilder());
        }
        if (getSynonymPreferredDescriptionBuilder() != null) {
            descriptionBuilders.add(getSynonymPreferredDescriptionBuilder());
        }
        descriptionBuilders.forEach((builder) -> {
            builder.build(stampCoordinate, builtObjects);
        });
        if (defaultLogicCoordinate == null && (logicalExpressions.size() > 0 || logicalExpressionBuilders.size() > 0)) {
            throw new IllegalStateException("A logic coordinate is required when a logical expression is passed");
        }
        SememeBuilderService builderService = LookupService.getService(SememeBuilderService.class);
        for (LogicalExpression logicalExpression : logicalExpressions) {
            sememeBuilders.add(builderService.
                    getLogicalExpressionSememeBuilder(logicalExpression, this, defaultLogicCoordinate.getStatedAssemblageSequence()));
        }
        for (LogicalExpressionBuilder builder : logicalExpressionBuilders) {
            sememeBuilders.add(builderService.
                    getLogicalExpressionSememeBuilder(builder.build(), this, defaultLogicCoordinate.getStatedAssemblageSequence()));
        }
        sememeBuilders.forEach((builder) -> builder.build(stampCoordinate, builtObjects));
        return conceptChronology;
    }

    @Override
    public String getConceptDescriptionText() {
        return conceptName;
    }
}

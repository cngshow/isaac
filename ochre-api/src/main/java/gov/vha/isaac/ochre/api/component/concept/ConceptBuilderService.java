/*
 * Copyright 2015 kec.
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
package gov.vha.isaac.ochre.api.component.concept;

import gov.vha.isaac.ochre.api.coordinate.LogicCoordinate;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import org.jvnet.hk2.annotations.Contract;

/**
 *
 * @author kec
 */
@Contract
public interface ConceptBuilderService
{

	/**
	 * @param conceptName - Optional - if specified, a FSN will be created using this value (but see additional information on semanticTag)
	 * @param semanticTag - Optional - if specified, conceptName must be specified, and two descriptions will be created using the following forms:
	 *   FSN:       "conceptName (semanticTag)"
	 *   Preferred: "conceptName"
	 * If not specified, only a FSN will be created, using exactly the conceptName value. No Preferred term will be created.
	 * @param logicalExpression - Optional
	 */
	ConceptBuilder getDefaultConceptBuilder(String conceptName, String semanticTag, LogicalExpression logicalExpression);

	ConceptBuilderService setDefaultLanguageForDescriptions(ConceptSpecification languageForDescriptions);

	ConceptSpecification getDefaultLanguageForDescriptions();

	ConceptBuilderService setDefaultDialectAssemblageForDescriptions(ConceptSpecification dialectForDescriptions);

	ConceptSpecification getDefaultDialectForDescriptions();

	ConceptBuilderService setDefaultLogicCoordinate(LogicCoordinate logicCoordinate);

	LogicCoordinate getDefaultLogicCoordinate();

	/**
	 * @param conceptName - Optional - if specified, a FSN will be created using this value (but see additional information on semanticTag)
	 * @param semanticTag - Optional - if specified, conceptName must be specified, and two descriptions will be created using the following forms:
	 * FSN: - "conceptName (semanticTag)"
	 * Preferred: "conceptName"
	 * If not specified, only a FSN will be created, using exactly the conceptName value. No Preferred term will be created.
	 * @param logicalExpression - Optional
	 * @param languageForDescriptions - Optional - used as the language for the created FSN and preferred term
	 * @param dialectForDescriptions - Optional - used as the language for the created FSN and preferred term
	 * @param logicCoordinate - Optional - used during the creation of the logical expression, if any are passed for creation.
	 */
	ConceptBuilder getConceptBuilder(String conceptName, String semanticTag, LogicalExpression logicalExpression, ConceptSpecification languageForDescriptions,
			ConceptSpecification dialectForDescriptions, LogicCoordinate logicCoordinate);
}

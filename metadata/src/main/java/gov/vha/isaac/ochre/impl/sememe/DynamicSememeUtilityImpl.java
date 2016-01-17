package gov.vha.isaac.ochre.impl.sememe;

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.NecessarySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilder;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilderService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUtility;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArray;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeBoolean;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeByteArray;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeDouble;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeFloat;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeInteger;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeLong;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeSequence;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.configuration.EditCoordinates;
import gov.vha.isaac.ochre.model.configuration.LogicCoordinates;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeArrayImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeBooleanImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeIntegerImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUIDImpl;
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

/**
 * {@link DynamicSememeUtility}
 *
 * Convenience methods related to DynamicSememes.  Implemented as an interface and a singleton to provide 
 * lower level code with access to these methods at runtime via HK2.
  *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Singleton
public class DynamicSememeUtilityImpl implements DynamicSememeUtility
{
	
	/**
	 * Read the {@link DynamicSememeUsageDescription} for the specified assemblage concept
	 */
	@Override
	public DynamicSememeUsageDescription readDynamicSememeUsageDescription(int assemblageNidOrSequence)
	{
		return DynamicSememeUsageDescriptionImpl.read(assemblageNidOrSequence);
	}

	/**
	 * Create a new concept using the provided columnName and columnDescription values which is suitable 
	 * for use as a column descriptor within {@link DynamicSememeUsageDescription}.
	 * 
	 * The new concept will be created under the concept {@link IsaacMetadataConstants#DYNAMIC_SEMEME_COLUMNS}
	 * 
	 * A complete usage pattern (where both the refex assemblage concept and the column name concept needs
	 * to be created) would look roughly like this:
	 * 
	 * DynamicSememeUtility.createNewDynamicSememeUsageDescriptionConcept(
	 *	 "The name of the Sememe", 
	 *	 "The description of the Sememe",
	 *	 new DynamicSememeColumnInfo[]{new DynamicSememeColumnInfo(
	 *		 0,
	 *		 DynamicSememeColumnInfo.createNewDynamicSememeColumnInfoConcept(
	 *			 "column name",
	 *			 "column description"
	 *			 )
	 *		 DynamicSememeDataType.STRING,
	 *		 new DynamicSememeStringImpl("default value")
	 *		 )}
	 *	 )
	 * 
	 * //TODO (artf231856) [REFEX] figure out language details (how we know what language to put on the name/description
	 * @throws RuntimeException 
	 */
	
	public static ConceptChronology<? extends ConceptVersion<?>> createNewDynamicSememeColumnInfoConcept(String columnName, String columnDescription) 
			throws RuntimeException
	{
		if (columnName == null || columnName.length() == 0 || columnDescription == null || columnDescription.length() == 0)
		{
			throw new RuntimeException("Both the column name and column description are required");
		}
		
		ConceptBuilderService conceptBuilderService = LookupService.getService(ConceptBuilderService.class);
		conceptBuilderService.setDefaultLanguageForDescriptions(MetaData.ENGLISH_LANGUAGE);
		conceptBuilderService.setDefaultDialectAssemblageForDescriptions(MetaData.US_ENGLISH_DIALECT);
		conceptBuilderService.setDefaultLogicCoordinate(LogicCoordinates.getStandardElProfile());

		DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class);
		LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();

		NecessarySet(And(ConceptAssertion(Get.conceptService().getConcept(IsaacMetadataConstants.DYNAMIC_SEMEME_COLUMNS.getNid()), defBuilder)));

		LogicalExpression parentDef = defBuilder.build();

		ConceptBuilder builder = conceptBuilderService.getDefaultConceptBuilder(columnName, null, parentDef);

		DescriptionBuilder<?, ?> definitionBuilder = descriptionBuilderService.getDescriptionBuilder(columnName, builder,
						MetaData.SYNONYM,
						MetaData.ENGLISH_LANGUAGE);

		definitionBuilder.setPreferredInDialectAssemblage(MetaData.US_ENGLISH_DIALECT);
		builder.addDescription(definitionBuilder);
		
		definitionBuilder = descriptionBuilderService.getDescriptionBuilder(columnDescription, builder, MetaData.DEFINITION_DESCRIPTION_TYPE,
				MetaData.ENGLISH_LANGUAGE);
		definitionBuilder.setPreferredInDialectAssemblage(MetaData.US_ENGLISH_DIALECT);
		builder.addDescription(definitionBuilder);

		ConceptChronology<? extends ConceptVersion<?>> newCon = builder.build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE, new ArrayList<>());

		Get.commitService().addUncommitted(newCon);

		Get.commitService().commit("creating new dynamic sememe column: " + columnName);
		return newCon;
	}
	
	/**
	 * See {@link DynamicSememeUsageDescription} for the full details on what this builds.
	 * 
	 * Does all the work to create a new concept that is suitable for use as an Assemblage Concept for a new style Dynamic Sememe.
	 * 
	 * The concept will be created under the concept {@link IsaacMetadataConstants#DYNAMIC_SEMEME_ASSEMBLAGES} if a parent is not specified
	 * 
	 * //TODO (artf231856) [REFEX] figure out language details (how we know what language to put on the name/description
	 * @param sememePreferredTerm - The preferred term for this refex concept that will be created.
	 * @param sememeDescription - A user friendly string the explains the overall intended purpose of this sememe (what it means, what it stores)
	 * @param columns - The column information for this new refex.  May be an empty list or null.
	 * @param parentConceptNidOrSequence  - optional - if null, uses {@link IsaacMetadataConstants#DYNAMIC_SEMEME_ASSEMBLAGES}
	 * @param referencedComponentRestriction - optional - may be null - if provided - this restricts the type of object referenced by the nid or 
	 * UUID that is set for the referenced component in an instance of this sememe.  If {@link ObjectChronologyType#UNKNOWN_NID} is passed, it is ignored, as 
	 * if it were null.
	 * @param referencedComponentSubRestriction - optional - may be null - subtype restriction for {@link ObjectChronologyType#SEMEME} restrictions
	 * @return a reference to the newly created sememe item
	 */
	public static DynamicSememeUsageDescription createNewDynamicSememeUsageDescriptionConcept(String sememeFSN, String sememePreferredTerm, 
			String sememeDescription, DynamicSememeColumnInfo[] columns, Integer parentConceptNidOrSequence, ObjectChronologyType referencedComponentRestriction,
			SememeType referencedComponentSubRestriction)
	{

		ConceptBuilderService conceptBuilderService = LookupService.getService(ConceptBuilderService.class);
		conceptBuilderService.setDefaultLanguageForDescriptions(MetaData.ENGLISH_LANGUAGE);
		conceptBuilderService.setDefaultDialectAssemblageForDescriptions(MetaData.US_ENGLISH_DIALECT);
		conceptBuilderService.setDefaultLogicCoordinate(LogicCoordinates.getStandardElProfile());

		DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class);
		LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();

		ConceptChronology<?> parentConcept =  Get.conceptService().getConcept(parentConceptNidOrSequence == null ? 
				IsaacMetadataConstants.DYNAMIC_SEMEME_ASSEMBLAGES.getNid() 
				: parentConceptNidOrSequence);
		
		NecessarySet(And(ConceptAssertion(parentConcept, defBuilder)));

		LogicalExpression parentDef = defBuilder.build();

		ConceptBuilder builder = conceptBuilderService.getDefaultConceptBuilder(sememeFSN, null, parentDef);

		DescriptionBuilder<?, ?> definitionBuilder = descriptionBuilderService.getDescriptionBuilder(sememePreferredTerm, builder,
						MetaData.SYNONYM,
						MetaData.ENGLISH_LANGUAGE);
		definitionBuilder.setPreferredInDialectAssemblage(MetaData.US_ENGLISH_DIALECT);
		builder.addDescription(definitionBuilder);
		
		ConceptChronology<? extends ConceptVersion<?>> newCon = builder.build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE, new ArrayList<>());
		Get.commitService().addUncommitted(newCon);
		
		{
			//Set up the dynamic sememe 'special' definition
			definitionBuilder = descriptionBuilderService.getDescriptionBuilder(sememeDescription, builder, MetaData.DEFINITION_DESCRIPTION_TYPE,
					MetaData.ENGLISH_LANGUAGE);
			definitionBuilder.setPreferredInDialectAssemblage(MetaData.US_ENGLISH_DIALECT);
			@SuppressWarnings("rawtypes")
			SememeChronology definitonSememe = (SememeChronology) definitionBuilder.build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE);
			Get.commitService().addUncommitted(definitonSememe);
			
			SememeChronology<? extends SememeVersion<?>> sememe = Get.sememeBuilderService().getDynamicSememeBuilder(definitonSememe.getNid(), 
					IsaacMetadataConstants.DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getSequence(), null).build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE);
			Get.commitService().addUncommitted(sememe);
		}

		if (columns != null)
		{
			//Ensure that we process in column order - we don't always keep track of that later - we depend on the data being stored in the right order.
			TreeSet<DynamicSememeColumnInfo> sortedColumns = new TreeSet<>(Arrays.asList(columns));
			
			for (DynamicSememeColumnInfo ci : sortedColumns)
			{
				DynamicSememeData[] data = configureDynamicSememeDefinitionDataForColumn(ci);

				SememeChronology<? extends SememeVersion<?>> sememe = Get.sememeBuilderService().getDynamicSememeBuilder(newCon.getNid(), 
						IsaacMetadataConstants.DYNAMIC_SEMEME_EXTENSION_DEFINITION.getSequence(), data)
					.build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE);
				Get.commitService().addUncommitted(sememe);
			}
		}
		
		DynamicSememeData[] data = configureDynamicSememeRestrictionData(referencedComponentRestriction, referencedComponentSubRestriction);
		
		if (data != null)
		{
			SememeChronology<? extends SememeVersion<?>> sememe = Get.sememeBuilderService().getDynamicSememeBuilder(newCon.getNid(), 
					IsaacMetadataConstants.DYNAMIC_SEMEME_REFERENCED_COMPONENT_RESTRICTION.getSequence(), data)
				.build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE);
			Get.commitService().addUncommitted(sememe);
		}

		Get.commitService().commit("creating new dynamic sememe assemblage: " + sememeFSN);
		return new DynamicSememeUsageDescriptionImpl(newCon.getNid());
	}
	
	public static DynamicSememeData[] configureDynamicSememeRestrictionData(ObjectChronologyType referencedComponentRestriction,
			SememeType referencedComponentSubRestriction)
	{
		if (referencedComponentRestriction != null && ObjectChronologyType.UNKNOWN_NID != referencedComponentRestriction)
		{
			int size = 1;
			if (referencedComponentSubRestriction != null &&  SememeType.UNKNOWN != referencedComponentSubRestriction)
			{
				size = 2;
			}

			DynamicSememeData[] data = new DynamicSememeData[size];
			data[0] = new DynamicSememeStringImpl(referencedComponentRestriction.name());
			if (size == 2)
			{
				data[1] = new DynamicSememeStringImpl(referencedComponentSubRestriction.name());
			}
			return data;
		}
		return null;
	}

	public static DynamicSememeData[] configureDynamicSememeDefinitionDataForColumn(DynamicSememeColumnInfo ci)
	{
		DynamicSememeData[] data = new DynamicSememeData[7];
		
		data[0] = new DynamicSememeIntegerImpl(ci.getColumnOrder());
		data[1] = new DynamicSememeUUIDImpl(ci.getColumnDescriptionConcept());
		if (DynamicSememeDataType.UNKNOWN == ci.getColumnDataType())
		{
			throw new RuntimeException("Error in column - if default value is provided, the type cannot be polymorphic");
		}
		data[2] = new DynamicSememeStringImpl(ci.getColumnDataType().name());
		data[3] = convertPolymorphicDataColumn(ci.getDefaultColumnValue(), ci.getColumnDataType());
		data[4] = new DynamicSememeBooleanImpl(ci.isColumnRequired());
		
		if (ci.getValidator() != null)
		{
			DynamicSememeString[] validators = new DynamicSememeString[ci.getValidator().length];
			for (int i = 0; i < validators.length; i++)
			{
				validators[i] = new DynamicSememeStringImpl(ci.getValidator()[i].name());
			}
			data[5] = new DynamicSememeArrayImpl<DynamicSememeString>(validators);
		}
		else
		{
			data[5] = null;
		}
		
		if (ci.getValidatorData() != null)
		{
			DynamicSememeData[] validatorData = new DynamicSememeData[ci.getValidatorData().length];
			for (int i = 0; i < validatorData.length; i++)
			{
				validatorData[i] = convertPolymorphicDataColumn(ci.getValidatorData()[i], ci.getValidatorData()[i].getDynamicSememeDataType());
			}
			data[6] = new DynamicSememeArrayImpl<DynamicSememeData>(validatorData);
		}
		else
		{
			data[6] = null;
		}
		return data;
	}
	
	private static DynamicSememeData convertPolymorphicDataColumn(DynamicSememeData defaultValue, DynamicSememeDataType columnType) 
	{
		DynamicSememeData result;
		
		if (defaultValue != null)
		{
			try
			{
				if (DynamicSememeDataType.BOOLEAN == columnType)
				{
					result = (DynamicSememeBoolean)defaultValue;
				}
				else if (DynamicSememeDataType.BYTEARRAY == columnType)
				{
					result = (DynamicSememeByteArray)defaultValue;
				}
				else if (DynamicSememeDataType.DOUBLE == columnType)
				{
					result = (DynamicSememeDouble)defaultValue;
				}
				else if (DynamicSememeDataType.FLOAT == columnType)
				{
					result = (DynamicSememeFloat)defaultValue;
				}
				else if (DynamicSememeDataType.INTEGER == columnType)
				{
					result = (DynamicSememeInteger)defaultValue;
				}
				else if (DynamicSememeDataType.LONG == columnType)
				{
					result = (DynamicSememeLong)defaultValue;
				}
				else if (DynamicSememeDataType.NID == columnType)
				{
					result = (DynamicSememeNid)defaultValue;
				}
				else if (DynamicSememeDataType.STRING == columnType)
				{
					result = (DynamicSememeString)defaultValue;
				}
				else if (DynamicSememeDataType.UUID == columnType)
				{
					result = (DynamicSememeUUID)defaultValue;
				}
				else if (DynamicSememeDataType.ARRAY == columnType)
				{
					result = (DynamicSememeArray<?>)defaultValue;
				}
				else if (DynamicSememeDataType.SEQUENCE== columnType)
				{
					result = (DynamicSememeSequence)defaultValue;
				}
				else if (DynamicSememeDataType.POLYMORPHIC == columnType)
				{
					throw new RuntimeException("Error in column - if default value is provided, the type cannot be polymorphic");
				}
				else
				{
					throw new RuntimeException("Actually, the implementation is broken.  Ooops.");
				}
			}
			catch (ClassCastException e)
			{
				throw new RuntimeException("Error in column - if default value is provided, the type must be compatible with the the column descriptor type");
			}
		}
		else
		{
			result = null;
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public String[] readDynamicSememeColumnNameDescription(UUID columnDescriptionConcept)
	{
		String columnName = null;
		String columnDescription = null;
		String fsn = null;
		String acceptableSynonym = null;
		String acceptableDefinition = null;
		try
		{
			ConceptChronology<? extends ConceptVersion<?>> cc = Get.conceptService().getConcept(columnDescriptionConcept);
			for (SememeChronology<? extends DescriptionSememe<?>> dc : cc.getConceptDescriptionList())
			{
				if (columnName != null && columnDescription != null)
				{
					break;
				}
				
				@SuppressWarnings("rawtypes")
				Optional<LatestVersion<DescriptionSememe<?>>> descriptionVersion = ((SememeChronology)dc)
						.getLatestVersion(DescriptionSememe.class, Get.configurationService().getDefaultStampCoordinate());
				
				if (descriptionVersion.isPresent())
				{
					DescriptionSememe<?> d = descriptionVersion.get().value();
					if (d.getDescriptionTypeConceptSequence() == MetaData.FULLY_SPECIFIED_NAME.getConceptSequence())
					{
						fsn = d.getText();
					}
					else if (d.getDescriptionTypeConceptSequence() == MetaData.SYNONYM.getConceptSequence())
					{
						if (Frills.isDescriptionPreferred(d.getNid(), null))
						{
							columnName = d.getText();
						}
						else
						{
							acceptableSynonym = d.getText();
						}
					}
					else if (d.getDescriptionTypeConceptSequence() == MetaData.DEFINITION_DESCRIPTION_TYPE.getConceptSequence())
					{
						if (Frills.isDescriptionPreferred(d.getNid(), null))
						{
							columnDescription = d.getText();
						}
						else
						{
							acceptableDefinition = d.getText();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failure reading DynamicSememeColumnInfo '" + columnDescriptionConcept + "'", e);
		}
		if (columnName == null)
		{
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "No preferred synonym found on '" + columnDescriptionConcept + "' to use "
					+ "for the column name - using FSN");
			columnName = (fsn == null ? "ERROR - see log" : fsn);
		}
		
		if (columnDescription == null && acceptableDefinition != null)
		{
			columnDescription = acceptableDefinition;
		}
		
		if (columnDescription == null && acceptableSynonym != null)
		{
			columnDescription = acceptableSynonym;
		}
		
		if (columnDescription == null)
		{
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "No preferred or acceptable definition or acceptable synonym found on '" 
					+ columnDescriptionConcept + "' to use for the column description- re-using the the columnName, instead.");
			columnDescription = columnName;
		}
		return new String[] {columnName, columnDescription};
	}
}
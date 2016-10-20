package gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe;

import java.security.InvalidParameterException;
import java.util.UUID;

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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jvnet.hk2.annotations.Contract;
import org.slf4j.LoggerFactory;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArray;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;

/**
 * {@link DynamicSememeUtility}
 * 
 * This class exists as an interface primarily to allow classes in ochre-api and ochre-impl to have access to these methods
 * that need to be implemented further down the dependency tree (with access to metadata, etc)
 * 
 *  Code in ochre-util and ochre-api will access the impl via HK2.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Contract
public interface DynamicSememeUtility {
	
	/**
	 * Convenience method to read all of the extended details of a DynamicSememeAssemblage
	 * @param assemblageNidOrSequence
	 */
	public DynamicSememeUsageDescription readDynamicSememeUsageDescription(int assemblageNidOrSequence);
	
	public DynamicSememeData[] configureDynamicSememeDefinitionDataForColumn(DynamicSememeColumnInfo ci);
	
	public DynamicSememeData[] configureDynamicSememeRestrictionData(ObjectChronologyType referencedComponentRestriction, 
			SememeType referencedComponentSubRestriction);
	
	/**
	 * This will return the column index configuration that will mark each supplied column that is indexable, for indexing.
	 * Returns null, if no columns need indexing.
	 */
	public DynamicSememeArray<DynamicSememeData> configureColumnIndexInfo(DynamicSememeColumnInfo[] columns);
	
	public DynamicSememeString createDynamicStringData(String value);
	
	public DynamicSememeUUID createDynamicUUIDData(UUID value);
	
	/**
	 * validate that the proposed dynamicSememeData aligns with the definition.
	 * @param dsud
	 * @param data
	 * @param referencedComponentNid
	 * @param stampCoordinate - optional - column specific validators may be skipped if this is not provided
	 * @param taxonomyCoordinate - optional - column specific validators may be skipped if this is not provided
	 * @throws InvalidParameterException - if anything fails validation
	 */
	public default void validate(DynamicSememeUsageDescription dsud, DynamicSememeData[] data, int referencedComponentNid, StampCoordinate stampCoordinate, 
			TaxonomyCoordinate taxonomyCoordinate) throws InvalidParameterException
	{
		//Make sure the referenced component meets the ref component restrictions, if any are present.
		if (dsud.getReferencedComponentTypeRestriction() != null && dsud.getReferencedComponentTypeRestriction() != ObjectChronologyType.UNKNOWN_NID)
		{
			ObjectChronologyType requiredType = dsud.getReferencedComponentTypeRestriction();
			ObjectChronologyType foundType = Get.identifierService().getChronologyTypeForNid(referencedComponentNid);
			
			if (requiredType != foundType)
			{
				throw new InvalidParameterException("The referenced component must be of type " + requiredType + ", but a " + foundType + " was passed");
			}
			
			if (requiredType == ObjectChronologyType.SEMEME && dsud.getReferencedComponentTypeSubRestriction() != null 
					&& dsud.getReferencedComponentTypeSubRestriction() != SememeType.UNKNOWN)
			{
				SememeType requiredSememeType = dsud.getReferencedComponentTypeSubRestriction();
				SememeType foundSememeType = Get.sememeService().getSememe(referencedComponentNid).getSememeType();
				
				if (requiredSememeType != foundSememeType)
				{
					throw new InvalidParameterException("The referenced component must be a sememe of type " + requiredSememeType + ", but a " 
							+ foundSememeType + " was passed");
				}
			}
		}

		if (data == null)
		{
			return;
		}
		
		//specifically allow < - we don't need the trailing columns, if they were defined as optional.
		if (data.length > dsud.getColumnInfo().length)
		{
			throw new InvalidParameterException("The Assemblage concept: " + dsud.getDynamicSememeName() + " specifies " + dsud.getColumnInfo().length + 
					" columns of data, while the provided data contains " + data.length + " columns.  The data size array must not exeed the sememe definition."
					+ " (the data column count may be less, if the missing columns are defined as optional)");
		}
		
		//If they provided less columns, make sure the remaining columns are all optional
		for (int i = data.length; i < dsud.getColumnInfo().length; i++)
		{
			if (dsud.getColumnInfo()[i].isColumnRequired())
			{
				throw new InvalidParameterException("No data was supplied for column '" + dsud.getColumnInfo()[i].getColumnName() + "' [" 
						+  (i + 1) + "(index " + i + ")] but the column is specified as a required column");
			}
		}
		
		for (int dataColumn = 0; dataColumn < data.length; dataColumn++)
		{
			DynamicSememeColumnInfo dsci = dsud.getColumnInfo()[dataColumn];
			
			if (data[dataColumn] == null)
			{
				if (dsci.isColumnRequired())
				{
					throw new InvalidParameterException("No data was supplied for column " + (dataColumn + 1) + " but the column is specified as a required column");
				}
			}
			else
			{
				DynamicSememeDataType allowedDT = dsci.getColumnDataType();
				if (data[dataColumn] != null && allowedDT != DynamicSememeDataType.POLYMORPHIC && data[dataColumn].getDynamicSememeDataType() != allowedDT)
				{
					throw new InvalidParameterException("The supplied data for column " + dataColumn + " is of type " + data[dataColumn].getDynamicSememeDataType() + 
							" but the assemblage concept declares that it must be " + allowedDT);
				}
				
				
				if (dsci.getValidator() != null && dsci.getValidator().length > 0)
				{
					try
					{
						for (int i = 0; i < dsci.getValidator().length; i++)
						{
							try
							{
								if (!dsci.getValidator()[i].passesValidator(data[dataColumn], dsci.getValidatorData()[i], stampCoordinate, taxonomyCoordinate))
								{
									throw new InvalidParameterException("The supplied data for column " + dataColumn 
											+ " does not pass the assigned validator(s) for this dynamic sememe");
								}
							}
							catch  (IllegalArgumentException e)
							{
								LoggerFactory.getLogger(DynamicSememeUtility.class).debug("Couldn't execute validator due to missing coordiantes");
							}
						}
					}
					catch (RuntimeException e)
					{
						throw new InvalidParameterException(e.getMessage());
					}
				}
			}
		}
	}
}

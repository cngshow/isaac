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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe;

import java.security.InvalidParameterException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.*;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.util.Interval;
import gov.vha.isaac.ochre.api.util.NumericUtils;


/**
 * {@link DynamicSememeValidatorType}
 * 
 * The acceptable validatorDefinitionData object type(s) for the following fields:
 * {@link DynamicSememeValidatorType#LESS_THAN}
 * {@link DynamicSememeValidatorType#GREATER_THAN}
 * {@link DynamicSememeValidatorType#LESS_THAN_OR_EQUAL}
 * {@link DynamicSememeValidatorType#GREATER_THAN_OR_EQUAL}
 * 
 * are one of ( {@link DynamicSememeInteger}, {@link DynamicSememeLong}, {@link DynamicSememeFloat}, {@link DynamicSememeDouble})
 * 
 * {@link DynamicSememeValidatorType#INTERVAL} - Should be a {@link DynamicSememeString} with valid interval notation - such as "[4,6)"
 * 
 * {@link DynamicSememeValidatorType#REGEXP} - Should be a {@link DynamicSememeString} with valid regular expression, per
 * http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
 * 
 * And for the following two:
 * {@link DynamicSememeValidatorType#IS_CHILD_OF}
 * {@link DynamicSememeValidatorType#IS_KIND_OF}
 * The validatorDefinitionData should be either an {@link DynamicSememeNid} or {@link DynamicSememeSequence} or a {@link DynamicSememeUUID}.
 * 
 * For {@link DynamicSememeValidatorType#COMPONENT_TYPE} the validator definition data should be a {@link DynamicSememeArray <DynamicSememeString>}
 * where position 0 is a string constant parseable by {@link ObjectChronologyType#parse(String)}.  Postion 1 is optional, and is only applicable when 
 * position 0 is {@link ObjectChronologyType#SEMEME} - in which case - the value should be parsable by {@link SememeType#parse(String)}
 * 
 * For {@link DynamicSememeValidatorType#EXTERNAL} the validatorDefinitionData should be a {@link DynamicSememeArray <DynamicSememeString>}
 * which contains (in the first position of the array) the name of an HK2 named service which implements {@link DynamicSememeExternalValidator} 
 * the name that you provide should be the value of the '@Name' annotation within the class which implements the ExternalValidatorBI class.  
 * This code will request that implementation (by name) and pass the validation call to it.
 * 
 * Optionally, the validatorDefinitionData more that one {@link DynamicSememeString} in the array - only the first position of the array
 * will be considered as the '@Name' to be used for the HK2 lookup.  All following data is ignored, and may be used by the external validator 
 * implementation to store other data.  For example, if the validatorDefinitionData {@link DynamicSememeArray <DynamicSememeString>}
 * contains an array of strings such as new String[]{"mySuperRefexValidator", "somespecialmappingdata", "some other mapping data"} 
 * then the following HK2 call will be made to locate the validator implementation (and validate):
 * <pre>
 *   ExternalValidatorBI validator = LookupService.get().getService(ExternalValidatorBI.class, "mySuperRefexValidator");
 *   return validator.validate(userData, validatorDefinitionData, viewCoordinate);
 * </pre>
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum DynamicSememeValidatorType
{
	LESS_THAN("<"), GREATER_THAN(">"), LESS_THAN_OR_EQUAL("<="), GREATER_THAN_OR_EQUAL(">="),  //Standard math stuff 
	INTERVAL("Interval"), //math interval notation - such as [5,10)
	REGEXP("Regular Expression"),  //http://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
	EXTERNAL("External"), //see class docs above - implemented by an ExternalValidatorBI
	IS_CHILD_OF("Is Child Of"), //OTF is child of - which only includes immediate (not recursive) children on the 'Is A' relationship. 
	IS_KIND_OF("Is Kind Of"), //OTF kind of - which is child of - but recursive, and self (heart disease is a kind-of heart disease);
	COMPONENT_TYPE("Component Type Restriction"),  //specify which type of nid can be put into a UUID or nid column
	UNKNOWN("Unknown");  //Not a real validator, only exists to allow GUI convenience, or potentially store other validator data that we don't support in OTF
	//but we may need to store / retreive
	
	private String displayName_;
	private static Logger logger = Logger.getLogger(DynamicSememeValidatorType.class.getName());
	
	private DynamicSememeValidatorType(String displayName)
	{
		displayName_ = displayName;
	}
	
	public String getDisplayName()
	{
		return displayName_;
	}
	
	public static DynamicSememeValidatorType[] parse(String[] nameOrEnumId, boolean exceptionOnParseFail)
	{
		if (nameOrEnumId == null)
		{
			return null;
		}
		DynamicSememeValidatorType[] temp = new DynamicSememeValidatorType[nameOrEnumId.length];
		{
			for (int i = 0; i < nameOrEnumId.length; i++)
			{
				temp[i] = parse(nameOrEnumId[i], exceptionOnParseFail);
			}
		}
		return temp;
	}
	
	public static DynamicSememeValidatorType parse(String nameOrEnumId, boolean exceptionOnParseFail)
	{
		if (nameOrEnumId == null)
		{
			return null;
		}
		String clean = nameOrEnumId.toLowerCase(Locale.ENGLISH).trim();
		if (StringUtils.isBlank(clean))
		{
			return null;
		}
		try
		{
			int i = Integer.parseInt(clean);
			//enumId
			return DynamicSememeValidatorType.values()[i];
		}
		catch (NumberFormatException e)
		{
			for (DynamicSememeValidatorType x : DynamicSememeValidatorType.values())
			{
				if (x.displayName_.equalsIgnoreCase(clean) || x.name().toLowerCase().equals(clean))
				{
					return x;
				}
			}
		}
		if (exceptionOnParseFail)
		{
			throw new InvalidParameterException("The value " + nameOrEnumId + " could not be parsed as a DynamicSememeValidatorType");
		}
		else
		{
			return UNKNOWN;
		}
	}
	
	public boolean validatorSupportsType(DynamicSememeDataType type)
	{
		//These are supported by all types - external specifies itself, what it supports, and we always include UNKNOWN.
		if (this == UNKNOWN || this == EXTERNAL)
		{
			return true;
		}
		
		switch (type)
		{
			case BOOLEAN: case POLYMORPHIC: 
			{
				//technically, regexp would work here... but makes no sense.
				return false;
			}	
			case DOUBLE: case FLOAT: case INTEGER: case LONG:
			{
				if (this == GREATER_THAN || this == GREATER_THAN_OR_EQUAL || 
						this == LESS_THAN || this == LESS_THAN_OR_EQUAL || 
						this == INTERVAL || this == REGEXP)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			case NID: case UUID:
			{
				if (this == IS_CHILD_OF || this == IS_KIND_OF || this == REGEXP || this == COMPONENT_TYPE)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			case SEQUENCE:  //can't support component type with sequence, because we don't know how to look it up
			{
				if (this == IS_CHILD_OF || this == IS_KIND_OF || this == REGEXP)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			case STRING: case BYTEARRAY:
			{
				if (this == REGEXP)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			default:
			{
				logger.warning("Unexpected case!");
				return false;
			}
		}
	}
	
	/**
	 * These are all defined from the perspective of the userData - so for passesValidator to return true -
	 * userData must be LESS_THAN validatorDefinitionData, for example.
	 * 
	 * @param userData
	 * @param validatorDefinitionData
	 * @param sc The Stamp Coordinate - not needed for some types of validations. Null allowed when unneeded (for math based tests, for example)
	 *   {@link IllegalArgumentException} will be thrown if the coordinate was required for the validator (but it wasn't supplied)
	 * @param tc The Taxonomy Coordinate - not needed for some types of validations. Null allowed when unneeded (for math based tests, for example)
	 *    {@link IllegalArgumentException} will be thrown if the coordinate was required for the validator (but it wasn't supplied)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean passesValidator(DynamicSememeData userData, DynamicSememeData validatorDefinitionData, StampCoordinate sc, TaxonomyCoordinate tc)
	{
		if (validatorDefinitionData == null)
		{
			throw new RuntimeException("The validator definition data is required");
		}
		if (this == DynamicSememeValidatorType.EXTERNAL)
		{
			DynamicSememeExternalValidator validator = null;
			DynamicSememeString[] valNameInfo = null;
			DynamicSememeArray<DynamicSememeString> stringValidatorDefData = null;
			String valName = null;
			if (validatorDefinitionData != null)
			{
				stringValidatorDefData = (DynamicSememeArray<DynamicSememeString>)validatorDefinitionData;
				valNameInfo = stringValidatorDefData.getDataArray();
			}
			if (valNameInfo != null && valNameInfo.length > 0)
			{
				valName = valNameInfo[0].getDataString();
				logger.fine("Looking for an ExternalValidatorBI with the name of '" + valName + "'");
				validator = LookupService.get().getService(DynamicSememeExternalValidator.class, valName);
			}
			else
			{
				logger.severe("An external validator type was specified, but no DynamicSememeExternalValidatorBI 'name' was provided.  API misuse!");
			}
			if (validator == null)
			{
				throw new RuntimeException("Could not locate an implementation of DynamicSememeExternalValidatorBI with the requested name of '" + valName + "'");
			}
			return validator.validate(userData, stringValidatorDefData, sc, tc);
		}
		else if (this == DynamicSememeValidatorType.REGEXP)
		{
			try
			{
				if (userData == null)
				{
					return false;
				}
				return Pattern.matches(((DynamicSememeString)validatorDefinitionData).getDataString(), userData.getDataObject().toString());
			}
			catch (Exception e)
			{
				throw new RuntimeException("The specified validator data object was not a valid regular expression: " + e.getMessage());
			}
		}
		else if (this == DynamicSememeValidatorType.IS_CHILD_OF || this == DynamicSememeValidatorType.IS_KIND_OF)
		{
			try
			{
				int childId;
				int parentId;

				if (userData instanceof DynamicSememeUUID)
				{
					childId = Get.identifierService().getNidForUuids(((DynamicSememeUUID) userData).getDataUUID());
				}
				else if (userData instanceof DynamicSememeNid)
				{
					childId = ((DynamicSememeNid) userData).getDataNid();
				}
				else if (userData instanceof DynamicSememeSequence)
				{
					childId = ((DynamicSememeSequence) userData).getDataSequence();
				}
				else
				{
					throw new RuntimeException("Userdata is invalid for a IS_CHILD_OF or IS_KIND_OF comparison");
				}

				if (validatorDefinitionData instanceof DynamicSememeUUID)
				{
					parentId = Get.identifierService().getNidForUuids(((DynamicSememeUUID) validatorDefinitionData).getDataUUID());
				}
				else if (validatorDefinitionData instanceof DynamicSememeNid)
				{
					parentId = ((DynamicSememeNid) validatorDefinitionData).getDataNid();
				}
				else if (userData instanceof DynamicSememeSequence)
				{
					parentId = ((DynamicSememeSequence) validatorDefinitionData).getDataSequence();
				}
				else
				{
					throw new RuntimeException("Validator DefinitionData is invalid for a IS_CHILD_OF or IS_KIND_OF comparison");
				}
				
				if (this == DynamicSememeValidatorType.IS_CHILD_OF)
				{
					if (tc == null)
					{
						throw new IllegalArgumentException("A taxonomy coordinate must be provided to evaluate IS_CHILD_OF");
					}
					return Get.taxonomyService().isChildOf(childId, parentId, tc);
				}
				else
				{
					if (tc == null)
					{
						return Get.taxonomyService().wasEverKindOf(childId, parentId);
					}
					else
					{
						return Get.taxonomyService().isKindOf(childId, parentId, tc);
					}
				}
			}
			catch (IllegalArgumentException e)
			{
				throw e;
			}
			catch (Exception e)
			{
				logger.log(Level.WARNING, "Failure executing validator", e);
				throw new RuntimeException("Failure executing validator", e);
			}
		}
		else if (this == DynamicSememeValidatorType.COMPONENT_TYPE)
		{
			try
			{
				int nid;
				if (userData instanceof DynamicSememeUUID)
				{
					DynamicSememeUUID uuid = (DynamicSememeUUID) userData;
					if (!Get.identifierService().hasUuid(uuid.getDataUUID()))
					{
						throw new RuntimeException("The specified UUID can not be found in the database, so the validator cannot execute");
					}
					else
					{
						nid = Get.identifierService().getNidForUuids(uuid.getDataUUID());
					}
				}
				else if (userData instanceof DynamicSememeNid)
				{
					nid = ((DynamicSememeNid) userData).getDataNid();
				}
				else
				{
					throw new RuntimeException("Userdata is invalid for a COMPONENT_TYPE comparison");
				}
				
				//Position 0 tells us the ObjectChronologyType.  When the type is Sememe, position 2 tells us the (optional) SememeType of the assemblage restriction
				DynamicSememeString[] valData = ((DynamicSememeArray<DynamicSememeString>)validatorDefinitionData).getDataArray();
				
				ObjectChronologyType expectedCT = ObjectChronologyType.parse(valData[0].getDataString(), false);
				ObjectChronologyType component = Get.identifierService().getChronologyTypeForNid(nid); 
				
				if (expectedCT == ObjectChronologyType.UNKNOWN_NID)
				{
					throw new RuntimeException("Couldn't determine validator type from validator data '" + valData + "'");
				}
				
				if (component != expectedCT)
				{
					throw new RuntimeException("The specified component must be of type " + expectedCT.toString() + ", not " + component);
				}
				
				if (expectedCT == ObjectChronologyType.SEMEME && valData.length == 2)
				{
					//they specified a specific sememe type.  Verify.
					SememeType st = SememeType.parse(valData[1].getDataString(), false);
					SememeChronology<? extends SememeVersion<?>> sememe = Get.sememeService().getSememe(nid);
					
					if (sememe.getSememeType() != st)
					{
						throw new RuntimeException("The specified component must be of type " + st.toString() + ", not " + sememe.getSememeType().toString());
					}
				}
				return true;
			}
			catch (RuntimeException e)
			{
				throw e;
			}
			catch (Exception e)
			{
				logger.log(Level.WARNING, "Failure executing validator", e);
				throw new RuntimeException("Failure executing validator", e);
			}
		}
		else
		{
			Number userDataNumber = NumericUtils.readNumber(userData);
			Number validatorDefinitionDataNumber;
			if (this == DynamicSememeValidatorType.INTERVAL)
			{
				String s = validatorDefinitionData.getDataObject().toString().trim();
				Interval interval = new Interval(s);

				if (interval.getLeft() != null)
				{
					int compareLeft = NumericUtils.compare(userDataNumber, interval.getLeft());
					if ((!interval.isLeftInclusive() && compareLeft == 0) || compareLeft < 0)
					{
						return false;
					}
				}
				if (interval.getRight() != null)
				{
					int compareRight = NumericUtils.compare(userDataNumber, interval.getRight());
					if ((!interval.isRightInclusive() && compareRight == 0) || compareRight > 0)
					{
						return false;
					}
				}
				return true;
			}
			else
			{
				validatorDefinitionDataNumber = NumericUtils.readNumber(validatorDefinitionData);
				int compareResult = NumericUtils.compare(userDataNumber, validatorDefinitionDataNumber);

				switch (this)
				{
					case LESS_THAN:
						return compareResult < 0;
					case GREATER_THAN:
						return compareResult > 0;
					case GREATER_THAN_OR_EQUAL:
						return compareResult >= 0;
					case LESS_THAN_OR_EQUAL:
						return compareResult <= 0;
					default:
						throw new RuntimeException("oops");
				}
			}
		}
	}
	
	/**
	 * A convenience wrapper of {@link #passesValidator(DynamicSememeDataBI, DynamicSememeDataBI, ViewCoordinate)} that just returns a string - never
	 * throws an error
	 * 
	 * These are all defined from the perspective of the userData - so for passesValidator to return true -
	 * userData must be LESS_THAN validatorDefinitionData, for example.
	 * 
	 * @param userData
	 * @param validatorDefinitionData
	 * @param sc - The Stamp Coordinate - not needed for some types of validations. Null allowed when unneeded (for math based tests, for example)
	 * @param tc - The Taxonomy Coordinate - not needed for some types of validations. Null allowed when unneeded (for math based tests, for example)
	 * @return - empty string if valid, an error message otherwise.
	 */
	public String passesValidatorStringReturn(DynamicSememeData userData, DynamicSememeData validatorDefinitionData, StampCoordinate sc, TaxonomyCoordinate tc)
	{
		try
		{
			if (passesValidator(userData, validatorDefinitionData, sc, tc))
			{
				return "";
			}
			else
			{
				return "The value does not pass the validator";
			}
		}
		catch (Exception e)
		{
			return e.getMessage();
		}
	}
}

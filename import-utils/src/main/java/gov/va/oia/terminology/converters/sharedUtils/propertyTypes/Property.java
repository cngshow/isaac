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
package gov.va.oia.terminology.converters.sharedUtils.propertyTypes;

import java.util.UUID;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;

/**
 * 
 * {@link Property}
 *
 * The converters common code uses this property abstraction system to handle converting different property 
 * types in the WB, while maintaining consistency in how properties are represented.  Also handles advanced
 * cases where we do things like map a property to an existing WB property type, and then annotate the property 
 * instance with the terminology specific property info.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class Property
{
	private String sourcePropertyNameFSN_;
	private String sourcePropertyAltName_;
	private String sourcePropertyDefinition_;
	private boolean isDisabled_ = false;
	private boolean isFromConceptSpec_ = false;
	private int propertySubType_ = Integer.MAX_VALUE;  //Used for subtypes of descriptions, at the moment - FSN, synonym, etc.
	private PropertyType owner_;
	private UUID propertyUUID = null;
	private UUID useWBPropertyTypeInstead = null;  //see comments in setter
	private DynamicSememeColumnInfo[] dataColumnsForDynamicRefex_ = null;
	private boolean isIdentifier_;
	private UUID secondParent_ = null;
	
	/**
	 * @param dataTypesForDynamicRefex - if null - will use the default information for the parent {@link PropertyType} - otherwise, 
	 * uses as provided here (even if empty)
	 */
	public Property(PropertyType owner, String sourcePropertyNameFSN, String sourcePropertyAltName, 
			String sourcePropertyDefinition, boolean disabled, int propertySubType, DynamicSememeColumnInfo[] columnInforForDynamicRefex)
	{
		this(owner, sourcePropertyNameFSN, sourcePropertyAltName, sourcePropertyDefinition, disabled, false, propertySubType, columnInforForDynamicRefex);
	}
	public Property(
			PropertyType owner,
			String sourcePropertyNameFSN,
			String sourcePropertyAltName, 
			String sourcePropertyDefinition,
			boolean disabled,
			boolean isIdentifier,
			int propertySubType,
			DynamicSememeColumnInfo[] columnInforForDynamicRefex)
	{
		this.owner_ = owner;
		this.sourcePropertyNameFSN_ = sourcePropertyNameFSN;
		this.sourcePropertyAltName_ = sourcePropertyAltName;
		this.sourcePropertyDefinition_ = sourcePropertyDefinition;
		this.isDisabled_ = disabled;
		this.propertySubType_ = propertySubType;
		this.isIdentifier_ = isIdentifier;
		if (isIdentifier_)
		{
			this.secondParent_ = MetaData.IDENTIFIER_SOURCE.getPrimordialUuid();
		}
		
		//if owner is null, have to delay this until the setOwner call
		//leave the assemblageConceptUUID null for now - it should be set to "getUUID()" but that isn't always ready
		//at the time this code runs.  We make sure it is set down below, in the getter.
		if (columnInforForDynamicRefex == null && owner_ != null && owner_.getDefaultColumnInfo() != null)
		{
			//Create a single required column, with the column name just set to 'value'
			dataColumnsForDynamicRefex_ = new DynamicSememeColumnInfo[] {
					new DynamicSememeColumnInfo(null, 0, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_VALUE.getUUID(),
					owner_.getDefaultColumnInfo(), null, true, null, null, true)
			};
		}
		else
		{
			dataColumnsForDynamicRefex_ = columnInforForDynamicRefex;
		}
		
		if (dataColumnsForDynamicRefex_ != null && owner_ != null && !owner_.createAsDynamicRefex()) 
		{
			throw new RuntimeException("Tried to attach dynamic sememe data where it isn't allowed.");
		}
	}

	public Property(PropertyType owner, String sourcePropertyNameFSN, boolean isIdentifier)
	{
		this(owner, sourcePropertyNameFSN, null, null, false, isIdentifier, Integer.MAX_VALUE, null);
	}

	public Property(PropertyType owner, String sourcePropertyNameFSN)
	{
		this(owner, sourcePropertyNameFSN, null, null, false, false, Integer.MAX_VALUE, null);
	}
	
	public Property(PropertyType owner, ConceptSpecification cs) {
		this(owner, cs, false);
	}

	public Property(ConceptSpecification cs, boolean isIdentifier) {
		this((PropertyType)null, cs, isIdentifier);
	}

	public Property(PropertyType owner, ConceptSpecification cs, boolean isIdentifier)
	{
		this(owner, cs.getConceptDescriptionText(), null, null, false, Integer.MAX_VALUE, null);
		propertyUUID = cs.getPrimordialUuid();
		ConverterUUID.addMapping(cs.getConceptDescriptionText(), cs.getPrimordialUuid());
		isFromConceptSpec_ = true;
		this.isIdentifier_ = isIdentifier;
	}
	
	public Property(PropertyType owner, String sourcePropertyNameFSN, String sourcePropertyAltName, String sourcePropertyDefinition, boolean isIdentifier) {
		this(owner, sourcePropertyNameFSN, sourcePropertyAltName, sourcePropertyDefinition, false, isIdentifier, Integer.MAX_VALUE, null);
	}
	public Property(String sourcePropertyNameFSN, String sourcePropertyAltName, String sourcePropertyDefinition, boolean isIdentifier) {
		this(null, sourcePropertyNameFSN, sourcePropertyAltName, sourcePropertyDefinition, false, isIdentifier, Integer.MAX_VALUE, null);
	}
	/**
	 * owner must be set via the set method after using this constructor!
	 */
	public Property(String sourcePropertyNameFSN, String sourcePropertyAltName, String sourcePropertyDefinition, UUID wbRelType)
	{
		this(null, sourcePropertyNameFSN, sourcePropertyAltName, sourcePropertyDefinition, false, Integer.MAX_VALUE, null);
		setWBPropertyType(wbRelType);
	}

	public Property(String sourcePropertyNameFSN, boolean disabled, int propertySubType, 
			DynamicSememeColumnInfo[] columnInforForDynamicRefex)
	{
		this(null, sourcePropertyNameFSN, null, null, disabled, false, propertySubType, columnInforForDynamicRefex);
	}
	
	public String getSourcePropertyNameFSN()
	{
		return sourcePropertyNameFSN_;
	}
	
	public String getSourcePropertyAltName()
	{
		return sourcePropertyAltName_;
	}
	
	public String getSourcePropertyDefinition()
	{
		return sourcePropertyDefinition_;
	}

	public boolean isIdentifier()
	{
		return isIdentifier_;
	}

	/**
	 * Normally, we just create the relation names as specified.  However, some, we map to 
	 * other existing WB relationships, and put the source rel name on as an extension - for example
	 * To enable the map case, set this (and use the appropriate addRelationship method)
	 */
	public void setWBPropertyType(UUID wbRelType)
	{
		this.useWBPropertyTypeInstead = wbRelType;
	}
	
	public UUID getWBTypeUUID()
	{
		return useWBPropertyTypeInstead;
	}

	protected void setOwner(PropertyType owner)
	{
		this.owner_ = owner;
		
		if (dataColumnsForDynamicRefex_ == null && owner_ != null && owner_.getDefaultColumnInfo() != null)
		{
			//Create a single required column, with the column name concept tied back to the assemblage concept itself.
			//leave the assemblageConceptUUID null for now - it should be set to "getUUID()" but that isn't always ready
			//at the time this code runs.  We make sure it is set down below, in the getter.
			dataColumnsForDynamicRefex_ = new DynamicSememeColumnInfo[] { new DynamicSememeColumnInfo(null, 0, getUUID(),
					owner_.getDefaultColumnInfo(), null, true, null, null, true)};
		}
		if (dataColumnsForDynamicRefex_ != null && !owner_.createAsDynamicRefex()) 
		{
			throw new RuntimeException("Tried to attach dynamic sememe data where it isn't allowed.");
		}
		
	}

	public void setIsIdentifier(boolean isIdentifier) {
		this.isIdentifier_ = isIdentifier;
		if (isIdentifier_)
		{
			this.secondParent_ = MetaData.IDENTIFIER_SOURCE.getPrimordialUuid();
		}
	}

	public UUID getUUID()
	{
		if (propertyUUID == null)
		{
			propertyUUID = owner_.getPropertyUUID(this.sourcePropertyNameFSN_);
		}
		return propertyUUID;
	}
	
	public UUID getSecondParent()
	{
		return secondParent_;
	}
	
	public void setSecondParent(UUID secondParent)
	{
		this.secondParent_ = secondParent;
	}

	public boolean isDisabled()
	{
		return isDisabled_;
	}
	
	public boolean isFromConceptSpec()
	{
		return isFromConceptSpec_;
	}
	
	public void setPropertySubType(int value)
	{
		this.propertySubType_ = value;
	}
	
	public int getPropertySubType()
	{
		return propertySubType_;
	}
	
	public PropertyType getPropertyType()
	{
		return owner_;
	}
	
	public DynamicSememeColumnInfo[] getDataColumnsForDynamicRefex()
	{
		if (dataColumnsForDynamicRefex_ != null && dataColumnsForDynamicRefex_.length == 1 && dataColumnsForDynamicRefex_[0].getAssemblageConcept() == null)
		{
			dataColumnsForDynamicRefex_[0].setAssemblageConcept(getUUID());
		}
		return dataColumnsForDynamicRefex_;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Property [FSN=" + sourcePropertyNameFSN_ + ", isIdentifier=" + isIdentifier_ + "]";
	}
}

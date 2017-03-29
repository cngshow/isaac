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

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;
import gov.va.oia.terminology.converters.sharedUtils.stats.ConverterUUID;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract base class to help in mapping code system property types into the workbench data model.
 * 
 * The main purpose of this structure is to keep the UUID generation sane across the various
 * places where UUIDs are needed in the workbench.
 * 
 * @author Daniel Armbrust
 */

public abstract class PropertyType
{
	protected static int srcVersion_ = 1;
	private UUID propertyTypeUUID = null;
	private String propertyTypeDescription_;
	private boolean createAsDynamicRefex_ = false;  //It could make sense to set this at the individual Property level... but in general, everything of the same type 
	//will be handled in the same way - relationships are not dynamic sememes, assoications are, for example.
	private DynamicSememeDataType defaultDataColumn_;  //If the property is specified without further column instructions, and createAsDynamicRefex is true, 
	//use this information to configure the (single) data column.

	private Map<String, Property> properties_;
	
	private Map<String, String> altNamePropertyMap_ = null;
	
	protected List<String> skipList_ = null;
	
	public static void setSourceVersion(int version)
	{
		srcVersion_ = version;
	}
	
	/**
	 * @param propertyTypeDescription - The name used for the property category within the terminology specific hierarchy -typically something like
	 * "Attribute Types" or "Association Types".  This text is also used to construct the UUID for this property type grouping. 
	 * @param createAsDynamicRefex - true to mark as a dynamic refex, false otherwise.
	 * @param defaultDynamicRefexColumnType - If the property is specified without further column instructions, and createAsDynamicRefex is true, 
	//use this information to configure the (single) data column.
	 */
	protected PropertyType(String propertyTypeDescription, boolean createAsDynamicRefex, DynamicSememeDataType defaultDynamicRefexColumnType)
	{
		this.properties_ = new HashMap<String, Property>();
		this.propertyTypeDescription_ = propertyTypeDescription;
		this.createAsDynamicRefex_ = createAsDynamicRefex;
		this.defaultDataColumn_ = defaultDynamicRefexColumnType;
	}

	public UUID getPropertyTypeUUID()
	{
		if (propertyTypeUUID == null)
		{
			propertyTypeUUID = ConverterUUID.createNamespaceUUIDFromString(propertyTypeDescription_);
		}
		return propertyTypeUUID;
	}

	public String getPropertyTypeDescription()
	{
		return propertyTypeDescription_;
	}

	protected UUID getPropertyUUID(String propertyName)
	{
		return ConverterUUID.createNamespaceUUIDFromString(propertyTypeDescription_ + ":" + propertyName);
	}

	public Property getProperty(String propertyName)
	{
		Property p = properties_.get(propertyName);
		if (p == null && altNamePropertyMap_ != null)
		{
			String altKey = altNamePropertyMap_.get(propertyName);
			if (altKey != null)
			{
				p = properties_.get(altKey);
			}
		}
		return p;
	}

	public Set<String> getPropertyNames()
	{
		return properties_.keySet();
	}

	public Collection<Property> getProperties()
	{
		return properties_.values();
	}

	public boolean containsProperty(String propertyName)
	{
		boolean result = properties_.containsKey(propertyName);
		if (!result && altNamePropertyMap_ != null)
		{
			String altKey = altNamePropertyMap_.get(propertyName);
			if (altKey != null)
			{
				result = properties_.containsKey(altKey);
			}
		}
		return result;
	}

	public Property addProperty(Property property)
	{
		if (skipList_ != null)
		{
			for (String s : skipList_)
			{
				if (property.getSourcePropertyNameFSN().equals(s))
				{
					ConsoleUtil.println("Skipping property '" + s + "' because of skip list configuration");
					return property;
				}
			}
		}
		property.setOwner(this);
		Property old = properties_.put(property.getSourcePropertyNameFSN(), property);
		if (old != null)
		{
			throw new RuntimeException("Duplicate property name: " + property.getSourcePropertyNameFSN());
		}
		if (altNamePropertyMap_ != null && StringUtils.isNotEmpty(property.getSourcePropertyAltName()))
		{
			String s = altNamePropertyMap_.put(property.getSourcePropertyAltName(), property.getSourcePropertyNameFSN());
			if (s != null)
			{
				throw new RuntimeException("Alt Indexing Error - duplicate!");
			}
		}
		return property;
	}

	public Property addProperty(String propertyNameFSN) {
		return addProperty(propertyNameFSN, false);
	}
	public Property addProperty(String propertyNameFSN, boolean isIdentifier)
	{
		return addProperty(propertyNameFSN, -1, isIdentifier);
	}
	
	public Property addProperty(String propertyNameFSN, int propertySubType) {
		return addProperty(propertyNameFSN, propertySubType, false);
	}
	public Property addProperty(String propertyNameFSN, int propertySubType, boolean isIdentifier)
	{
		return addProperty(propertyNameFSN, null, null, false, isIdentifier, propertySubType, null);
	}

	public Property addProperty(ConceptSpecification cs, boolean isIdentifier) {
		return addProperty(new Property((PropertyType)null, cs, isIdentifier));
	}
	public Property addProperty(PropertyType owner, ConceptSpecification cs, boolean isIdentifier) {
		return addProperty(new Property(owner, cs, isIdentifier));
	}
	public Property addProperty(String sourcePropertyNameFSN, String sourcePropertyAltName, String sourcePropertyDefinition)
	{
		return addProperty(sourcePropertyNameFSN, sourcePropertyAltName, sourcePropertyDefinition, false, -1, null);
	}
	
	public Property addProperty(String sourcePropertyNameFSN, String sourcePropertyAltName, String sourcePropertyDefinition, 
			boolean disabled, int propertySubType, DynamicSememeColumnInfo[] dataColumnForDynamicRefex) {
		return addProperty(sourcePropertyNameFSN, sourcePropertyAltName, sourcePropertyDefinition, disabled, false, propertySubType, dataColumnForDynamicRefex);
	}
	public Property addProperty(String sourcePropertyNameFSN, String sourcePropertyAltName, String sourcePropertyDefinition, 
			boolean disabled, boolean isIdentifier, int propertySubType, DynamicSememeColumnInfo[] dataColumnForDynamicRefex)
	{
		return addProperty(new Property(this, sourcePropertyNameFSN, sourcePropertyAltName, sourcePropertyDefinition, disabled, isIdentifier,
				propertySubType, dataColumnForDynamicRefex));
	}
	
	public Property addProperty(String sourcePropertyNameFSN, String sourcePropertyAltName, String sourcePropertyDefinition, boolean isIdentifier) {
		return addProperty(new Property(this, sourcePropertyNameFSN, sourcePropertyAltName, sourcePropertyDefinition, isIdentifier));
	}

	/**
	 * Only adds the property if the version of the data file falls between min and max, inclusive.
	 * pass 0 in min or max to specify no min or no max, respectively
	 */
	public Property addProperty(String propertyNameFSN, int minVersion, int maxVersion)
	{
		return addProperty(propertyNameFSN, null, null, minVersion, maxVersion, false, -1);
	}
	
	/**
	 * Only adds the property if the version of the data file falls between min and max, inclusive.
	 * pass 0 in min or max to specify no min or no max, respectively
	 */
	public Property addProperty(String propertyNameFSN, int minVersion, int maxVersion, boolean disabled)
	{
		return addProperty(propertyNameFSN, null, null, minVersion, maxVersion, disabled, -1);
	}

	/**
	 * Only adds the property if the version of the data file falls between min and max, inclusive.
	 * pass 0 in min or max to specify no min or no max, respectively
	 */
	public Property addProperty(String sourcePropertyNameFSN, String altName, String sourcePropertyDefinition, 
			int minVersion, int maxVersion, boolean disabled, int propertySubType)
	{
		if ((minVersion != 0 && srcVersion_ < minVersion) || (maxVersion != 0 && srcVersion_ > maxVersion))
		{
			return null;
		}
		return addProperty(sourcePropertyNameFSN, altName, sourcePropertyDefinition, disabled, propertySubType, null);
	}
	
	public boolean createAsDynamicRefex()
	{
		return createAsDynamicRefex_;
	}
	
	/**
	 * Enable index and lookup of properties by their altName field
	 */
	public void indexByAltNames()
	{
		if (altNamePropertyMap_ == null)
		{
			altNamePropertyMap_ = new HashMap<>();
		}
	}
	
	protected DynamicSememeDataType getDefaultColumnInfo()
	{
		return defaultDataColumn_;
	}
}

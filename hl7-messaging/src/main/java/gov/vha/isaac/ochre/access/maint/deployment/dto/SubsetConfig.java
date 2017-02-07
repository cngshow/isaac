package gov.vha.isaac.ochre.access.maint.deployment.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author BORG4
 */
public class SubsetConfig implements Comparable<SubsetConfig>, Serializable
{
	private int id;
	private String name;
	private boolean active;
	private List<DependentSubsetRule> dependentSubsetRules;
	private List<RelationshipConfig> relationshipFilters;
	private List<PropertyConfig> propertyFilters;
	private List<DesignationConfig> designationFilters;

	/**
	 *  
	 */
	public SubsetConfig(int id, String name, boolean active, List<DependentSubsetRule> dependents,
			List<PropertyConfig> propertyFilters, List<RelationshipConfig> relationshipFilters,
			List<DesignationConfig> designationFilters) {
		this.id = id;
		this.name = name;
		this.active = active;
		this.dependentSubsetRules = dependents;
		this.propertyFilters = propertyFilters;
		this.relationshipFilters = relationshipFilters;
		this.designationFilters = designationFilters;
	}

	public SubsetConfig(String name, boolean active, List<DependentSubsetRule> dependents,
			List<PropertyConfig> propertyFilters, List<RelationshipConfig> relationshipFilters,
			List<DesignationConfig> designationFilters) {
		this(0, name, active, dependents, propertyFilters, relationshipFilters, designationFilters);
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the active.
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active
	 *            The active to set.
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return Returns the id.
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            The id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

	public List<PropertyConfig> getPropertyFilters() {
		return propertyFilters;
	}

	public List<String> getPropertyNameList() {
		List<String> propertyNames = new ArrayList<String>();
		// build the property list
		for (Iterator iter = propertyFilters.iterator(); iter.hasNext();) {
			PropertyConfig filter = (PropertyConfig) iter.next();
			propertyNames.add(filter.getName());
		}
		Collections.sort(propertyNames);
		return propertyNames;
	}

	public List<RelationshipConfig> getRelationshipsFilters() {
		return relationshipFilters;
	}

	public List<String> getRelationshipNameList() {
		List<String> relationshipNames = new ArrayList<String>();
		// build the property list
		for (Iterator iter = relationshipFilters.iterator(); iter.hasNext();) {
			RelationshipConfig filter = (RelationshipConfig) iter.next();
			relationshipNames.add(filter.getName());
		}
		Collections.sort(relationshipNames);
		return relationshipNames;
	}

	public List<DependentSubsetRule> getDependentSubsetRules() {
		return dependentSubsetRules;
	}

	public void setDependentSubsetRules(List<DependentSubsetRule> dependentSubsetRules) {
		this.dependentSubsetRules = dependentSubsetRules;
	}

	public List<String> getRegularRelationshipNameList() {
		List<String> relationshipNames = new ArrayList<String>();
		// build the property list
		for (Iterator iter = relationshipFilters.iterator(); iter.hasNext();) {
			RelationshipConfig filter = (RelationshipConfig) iter.next();
			if (!filter.isInverse()) {
				relationshipNames.add(filter.getName());
			}
		}
		Collections.sort(relationshipNames);
		return relationshipNames;
	}

	public List<String> getInverseRelationshipNameList() {
		List<String> relationshipNames = new ArrayList<String>();
		// build the property list
		for (Iterator iter = relationshipFilters.iterator(); iter.hasNext();) {
			RelationshipConfig filter = (RelationshipConfig) iter.next();
			if (filter.isInverse()) {
				relationshipNames.add(filter.getName());
			}
		}
		Collections.sort(relationshipNames);
		return relationshipNames;
	}

	public List<DesignationConfig> getDesignationFilters() {
		return designationFilters;
	}

	public List<String> getDesignationNameList() {
		List<String> names = new ArrayList<String>();
		// build the property list
		for (Iterator iter = designationFilters.iterator(); iter.hasNext();) {
			DesignationConfig filter = (DesignationConfig) iter.next();
			names.add(filter.getName());
		}
		Collections.sort(names);
		return names;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(SubsetConfig subsetConfig2) {
		return name.compareTo(subsetConfig2.name);
	}

}

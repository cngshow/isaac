package gov.vha.isaac.ochre.access.maint.deployment.dto;

import java.io.Serializable;

/**
 * @author vhaislnobleb
 * 
 */
public class DependentSubsetRule implements Serializable
{
	protected String subsetName;
	protected String relationshipName;

	/**
	 * @param subsetName
	 * @param relationshipName
	 */
	public DependentSubsetRule(String subsetName, String relationshipName) {
		this.subsetName = subsetName;
		this.relationshipName = relationshipName;
	}

	/**
	 * @return Returns the subsetName.
	 */
	public String getSubsetName() {
		return subsetName;
	}

	/**
	 * @param subsetName
	 *            The subsetName to set.
	 */
	public void setSubsetName(String subsetName) {
		this.subsetName = subsetName;
	}

	/**
	 * @return Returns the relationshipName.
	 */
	public String getRelationshipName() {
		return relationshipName;
	}

	/**
	 * @param relationshipName
	 *            The relationshipName to set.
	 */
	public void setRelationshipName(String relationshipName) {
		this.relationshipName = relationshipName;
	}
}

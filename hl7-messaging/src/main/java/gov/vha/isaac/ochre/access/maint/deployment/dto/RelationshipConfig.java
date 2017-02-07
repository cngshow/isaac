package gov.vha.isaac.ochre.access.maint.deployment.dto;

/**
 * @author BORG4
 * 
 */
public class RelationshipConfig extends BaseConfig
{
	protected boolean isInverse;

	protected String propertyType;

	protected String propertyValue;

	protected String includeWithChange;

	/**
	 * @param name
	 * @param isAllowEmpty
	 * @param isList
	 */
	public RelationshipConfig(String name, boolean isAllowEmpty, boolean isInverse, String propertyType,
			String propertyValue, boolean isList, String includeWithChange) {
		super(name, isAllowEmpty, isList);
		this.isInverse = isInverse;
		this.propertyType = propertyType;
		this.propertyValue = propertyValue;
		this.includeWithChange = includeWithChange;
	}

	/**
	 * @return Returns the isInverse.
	 */
	public boolean isInverse() {
		return isInverse;
	}

	/**
	 * @param isInverse
	 *            The isInverse to set.
	 */
	public void setInverse(boolean isInverse) {
		this.isInverse = isInverse;
	}

	/**
	 * @return Returns the propertyType.
	 */
	public String getPropertyType() {
		return propertyType;
	}

	/**
	 * @param propertyType
	 *            The propertyType to set.
	 */
	public void setPropertyType(String propertyType) {
		this.propertyType = propertyType;
	}

	/**
	 * @return Returns the propertyValue.
	 */
	public String getPropertyValue() {
		return propertyValue;
	}

	/**
	 * @param propertyValue
	 *            The propertyValue to set.
	 */
	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public boolean isSpecialHandling() {
		return isInverse;
	}

	public String getIncludeWithChange() {
		return includeWithChange;
	}

	public void setIncludeWithChange(String includeWithChange) {
		this.includeWithChange = includeWithChange;
	}
}

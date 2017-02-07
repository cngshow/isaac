package gov.vha.isaac.ochre.access.maint.deployment.dto;

public interface SiteData
{

	/**
	 * The name to get
	 * @return
	 */
	String getName();

	/**
	 * The name to set
	 * @param name
	 */
	void setName(String name);

	/**
	 * The value to get
	 * @return
	 */
	String getValue();

	/**
	 * The value to set
	 * @param value
	 */
	void setValue(String value);

	/**
	 * The vuid to get
	 * @return
	 */
	long getVuid();

	/**
	 * The vuid to set
	 * @param vuid
	 */
	void setVuid(long vuid);

	/**
	 * The Site to get
	 * @return
	 */
	Site getSite();

	/**
	 * The Site to set
	 * @param site
	 */
	void setSite(Site site);

	/**
	 * The subsetName to get
	 * @return
	 */
	String getSubsetName();

	/**
	 * The subsetName to set
	 * @param subsetName
	 */
	void setSubsetName(String subsetName);

	/**
	 * The type to get
	 * @return
	 */
	String getType();

	/**
	 * The type to set
	 * @param type
	 */
	void setType(String type);

	/**
	 * The status to get
	 * @return
	 */
	boolean isActive();

	/**
	 * The status to set
	 * @param active
	 */
	void setActive(boolean active);

}
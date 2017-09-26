package gov.vha.isaac.ochre.pombuilder;

public interface PomBuilderProperties {

	/**
	 * @return the name
	 */
	String getName();

	/**
	 * @param name the name to set
	 */
	void setName(String name);

	/**
	 * @return the version
	 */
	String getVersion();

	/**
	 * @param version the version to set
	 */
	void setVersion(String version);

	/**
	 * @return the description
	 */
	String getDescription();

	/**
	 * @param description the description to set
	 */
	void setDescription(String description);

	/**
	 * @return the metadataVerion
	 */
	String getMetadataVerion();

	/**
	 * @param metadataVerion the metadataVerion to set
	 */
	void setMetadataVerion(String metadataVerion);

}
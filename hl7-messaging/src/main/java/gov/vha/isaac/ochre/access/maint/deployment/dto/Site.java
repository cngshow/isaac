package gov.vha.isaac.ochre.access.maint.deployment.dto;

public interface Site
{

	/**
	 * @return
	 */
	long getId();

	/**
	 * @param id
	 */
	void setId(long id);

	/**
	 * @return Returns the site group name.
	 */
	String getGroupName();

	/**
	 * @param groupName
	 */
	void setGroupName(String groupName);

	/**
	 * @return Returns the site name.
	 */
	String getName();

	/**
	 * @param name
	 */
	void setName(String name);

	/**
	 * @return Returns the site type.
	 */
	String getType();

	/**
	 * @param type
	 */
	void setType(String type);

	/**
	 * @return Returns the site VA Site Id.
	 */
	String getVaSiteId();

	/**
	 * @param vaSiteId
	 */
	void setVaSiteId(String vaSiteId);

	/**
	 * @return Returns the site message type.
	 */
	String getMessageType();

	/**
	 * @param messageType
	 */
	void setMessageType(String messageType);

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	int compareTo(Site site);

}
package gov.vha.isaac.ochre.access.maint.deployment.dto;

public interface PublishMessage
{

	/**
	 * Returns the HL7 message ID
	 * 
	 * @return Returns the messageId.
	 */
	long getMessageId();

	/**
	 * Sets the HL7 message ID
	 * 
	 * @param messageId
	 *            The messageId to set.
	 */
	void setMessageId(long messageId);

	/**
	 * Returns the site to which the message (with message ID) was sent
	 * 
	 * @return Returns the site.
	 */
	Site getSite();

	/**
	 * Sets the site to which the message (with message ID) was sent
	 * 
	 * @param site
	 *            The site to set.
	 */
	void setSite(Site site);

}
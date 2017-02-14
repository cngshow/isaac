package gov.vha.isaac.ochre.access.maint.deployment.dto;

import java.util.List;

public interface PublishMessage
{

	String getSubset();
	
	void setSubset(String subset);
	
	void setChecksum(String checksum);
	
	void setSiteDiscovery(String siteData);
	
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
	List<Site> getSites();

	/**
	 * Sets the site to which the message (with message ID) was sent
	 * 
	 * @param site
	 *            The site to set.
	 */
	void setSites(List<Site> sites);

}
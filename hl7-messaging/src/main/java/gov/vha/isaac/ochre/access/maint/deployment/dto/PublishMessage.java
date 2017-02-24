package gov.vha.isaac.ochre.access.maint.deployment.dto;

/**
 * An interface for passing over the necessary site / subset information for sending a request, with the messageID prepopulated
 * and setters for putting in the result(s).
 * 
 * {@link PublishMessage}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public interface PublishMessage
{

	public String getSubset();
	
	/**
	 * Returns the site to which the message (with message ID) was sent
	 * 
	 * @return Returns the site.
	 */
	public Site getSite();
	
	/**
	 * Returns the HL7 message ID
	 * 
	 * @return Returns the messageId.
	 */
	public long getMessageId();
	
	public void setVersion(String version);
	
	public void setChecksum(String checksum);
	
	public void setSiteDiscovery(String siteData);
	
	public void setRawHL7Message(String rawMessage);
}
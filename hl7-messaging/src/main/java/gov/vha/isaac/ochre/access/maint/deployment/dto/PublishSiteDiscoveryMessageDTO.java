package gov.vha.isaac.ochre.access.maint.deployment.dto;

public class PublishSiteDiscoveryMessageDTO implements PublishSiteDiscoveryMessage
{
	private Site site;
	private long messageId;
	private String subset;
	private SiteDiscovery siteDiscovery;
	private String rawHL7Message;
	
	/**
	 * Instantiate a new HL7MessagePublication with the required parameters
	 * 
	 * @param site
	 * @param messageId
	 */
	public PublishSiteDiscoveryMessageDTO(long messageId, Site site, String subset) {
		this.site = site;
		this.messageId = messageId;
		this.subset = subset;
	}

	@Override
	public String getSubset()
	{
		return subset;
	}

	@Override
	public Site getSite()
	{
		return site;
	}

	@Override
	public long getMessageId()
	{
		return messageId;
	}

//	@Override
//	public void setSiteDiscovery(SiteDiscovery siteDiscovery)
//	{
//		this.siteDiscovery = siteDiscovery;
//	}
//
//	public SiteDiscovery getSiteDiscovery()
//	{
//		return siteDiscovery;
//	}
	
	@Override
	public void setRawHL7Message(String rawMessage)
	{
		this.rawHL7Message = rawMessage;
	}

	@Override
	public String getRawHL7Message()
	{
		return rawHL7Message;
	}
}

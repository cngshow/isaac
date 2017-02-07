package gov.vha.isaac.ochre.access.maint.deployment.dto;


public class SiteDataDTO implements SiteData
{
	private String name;
	private String value;
	private long vuid;
	private Site site;
	private String subsetName;
	private String type;
	private boolean active;
	
	public SiteDataDTO()
	{
		super();
	}

	public SiteDataDTO(String name, String value, long vuid, Site site, String subsetName, String type)
	{
		super();
		this.name = name;
		this.value = value;
		this.vuid = vuid;
		this.site = site;
		this.subsetName = subsetName;
		this.type = type;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#setName(java.lang.String)
	 */
	@Override
	public void setName(String name)
	{
		this.name = name;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#getValue()
	 */
	@Override
	public String getValue()
	{
		return value;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#setValue(java.lang.String)
	 */
	@Override
	public void setValue(String value)
	{
		this.value = value;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#getVuid()
	 */
	@Override
	public long getVuid()
	{
		return vuid;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#setVuid(long)
	 */
	@Override
	public void setVuid(long vuid)
	{
		this.vuid = vuid;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#getSite()
	 */
	@Override
	public Site getSite()
	{
		return site;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#setSite(gov.vha.isaac.ochre.access.maint.deployment.dto.Site)
	 */
	@Override
	public void setSite(Site site)
	{
		this.site = site;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#getSubsetName()
	 */
	@Override
	public String getSubsetName()
	{
		return subsetName;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#setSubsetName(java.lang.String)
	 */
	@Override
	public void setSubsetName(String subsetName)
	{
		this.subsetName = subsetName;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#getType()
	 */
	@Override
	public String getType()
	{
		return type;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#setType(java.lang.String)
	 */
	@Override
	public void setType(String type)
	{
		this.type = type;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#isActive()
	 */
	@Override
	public boolean isActive()
	{
		return active;
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.access.maint.deployment.dto.SiteData#setActive(boolean)
	 */
	@Override
	public void setActive(boolean active)
	{
		this.active = active;
	}
}


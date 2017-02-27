package gov.vha.isaac.ochre.access.maint.deployment.dto;

import java.util.Map;

/**
 * An interface for passing over the necessary site discovery information for sending a request, 
 * with setters for putting in the result(s).
 * 
 * {@link SiteDiscovery}
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public interface SiteDiscovery
{
	/**
	 * 
	 */
	public void setSubset(String subset);
	
	/**
	 * 
	 * @return
	 */
	public String getSubset();
	
	/**
	 * 
	 */
	public void setVuid(String vuid);
	
	/**
	 * 
	 * @return
	 */
	public String getVuid();
	
	/**
	 * 
	 */
	public void setSegments(Map<String, String> segments);
	
	/**
	 * 
	 * @return
	 */
	public Map<String, String> getSegments();
	
	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void addSegment(String key, String value);
		
}

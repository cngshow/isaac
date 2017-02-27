package gov.vha.isaac.ochre.access.maint.deployment.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * An interface for passing over the necessary site discovery information for sending a request, 
 * with setters for putting in the result(s).
 * 
 * {@link SiteDiscovery}
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public class SiteDiscoveryDTO implements SiteDiscovery
{
	private String subset;
	private String vuid;
	private Map<String, String> segments = new HashMap<String, String>();

	public SiteDiscoveryDTO(String subset, String vuid, Map<String, String> segments) {
		this.subset = subset;
		this.vuid = vuid;
		this.segments = segments;
	}
	
	public SiteDiscoveryDTO() {
	}
	
	@Override
	public void setSubset(String subset) {
		this.subset = subset;
	}
	
	@Override
	public String getSubset() {
		return subset;
	}
	
	@Override
	public void setVuid(String vuid) {
		this.vuid = vuid;
	}
	
	@Override
	public String getVuid() {
		return vuid;
	}
	
	@Override
	public void setSegments(Map<String, String> segments) {
		this.segments = segments;
	}
	
	@Override
	public Map<String, String> getSegments() {
		return segments;
	}
	
	@Override
	public void addSegment(String key, String value) {
		if (segments != null) {
			this.segments.put(key, value);
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("subset: ").append(subset).append("\n");
		sb.append("vuid: ").append(vuid).append("\n");
		
		for(Map.Entry<String, String> entry : segments.entrySet()) {
			sb.append("name: ").append(entry.getKey().toString()).append(" value: ").append(entry.getValue().toString()).append("\n");
		}
		return sb.toString();
	}
}

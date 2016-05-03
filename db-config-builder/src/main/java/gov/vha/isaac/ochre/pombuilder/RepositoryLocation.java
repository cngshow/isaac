package gov.vha.isaac.ochre.pombuilder;

public class RepositoryLocation
{
	private String url_;
	private String tag_;
	
	public RepositoryLocation(String url, String tag)
	{
		url_ = url;
		tag_ = tag;
	}

	public String getUrl()
	{
		return url_;
	}

	public String getTag()
	{
		return tag_;
	}
}

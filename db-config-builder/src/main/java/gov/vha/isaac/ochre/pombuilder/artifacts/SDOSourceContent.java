package gov.vha.isaac.ochre.pombuilder.artifacts;

public class SDOSourceContent extends Artifact
{
	public SDOSourceContent(String groupId, String artifactId, String version)
	{
		this(groupId, artifactId, version, null);
	}
	
	public SDOSourceContent(String groupId, String artifactId, String version, String classifier)
	{
		super(groupId, artifactId, version, classifier);
	}
}

package gov.vha.isaac.ochre.pombuilder.artifacts;

public class IBDFFile extends Artifact
{
	public IBDFFile(String groupId, String artifactId, String version)
	{
		this(groupId, artifactId, version, null);
	}
	
	public IBDFFile(String groupId, String artifactId, String version, String classifier)
	{
		super(groupId, artifactId, version, classifier);
	}
}

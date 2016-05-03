package gov.vha.isaac.ochre.pombuilder.artifacts;

public abstract class Artifact
{
	private String groupId_;
	private String artifactId_;
	private String version_;
	private String classifier_;
	
	public Artifact(String groupId, String artifactId, String version)
	{
		this(groupId, artifactId, version, null);
	}
	
	public Artifact(String groupId, String artifactId, String version, String classifier)
	{
		groupId_ = groupId;
		artifactId_ = artifactId;
		version_ = version;
		classifier_ = classifier;
	}

	public String getGroupId()
	{
		return groupId_;
	}

	public String getArtifactId()
	{
		return artifactId_;
	}

	public String getVersion()
	{
		return version_;
	}

	public String getClassifier()
	{
		return classifier_;
	}
}

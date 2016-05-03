package gov.vha.isaac.ochre.pombuilder.converter;

public enum SupportedConverterTypes
{
	LOINC("loinc-src-data"),
	LOINC_TECH_PREVIEW("loinc-src-data-tech-preview", "loinc-src-data", "rf2-src-data-sct"),
	SCT("rf2-src-data-sct"),
	SCT_EXTENSION("rf2-src-data-*-extension", "rf2-src-data-sct"),
	VHAT("vhat");
	
	private String artifactId_;
	private String[] artifactDependencies_;
	
	private SupportedConverterTypes(String artifactId, String ... artifactDependencies)
	{
		artifactId_ = artifactId;
		artifactDependencies_ = artifactDependencies;
	}

	public String getArtifactId()
	{
		return artifactId_;
	}

	public String[] getArtifactDependencies()
	{
		return artifactDependencies_;
	}
}

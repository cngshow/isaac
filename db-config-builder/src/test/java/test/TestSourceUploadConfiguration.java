package test;

import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.converter.SupportedConverterTypes;
import gov.vha.isaac.ochre.pombuilder.upload.SrcUploadCreator;

public class TestSourceUploadConfiguration
{

	public static void main(String[] args) throws Exception
	{
		String gitTestURL = "https://github.com/darmbrust/test.git";
		String gitUsername = "";
		String gitPassword = "";
		
		String artifactRepository = "http://vadev.mantech.com:8081/nexus/content/groups/public/";
		String repositoryUsername = "";
		String repositoryPassword = "";

		System.setProperty("java.awt.headless", "true");
		
		System.out.println(GitPublish.readTags(gitTestURL, gitUsername, gitPassword));
		
		System.out.println(SrcUploadCreator.createSrcUploadConfiguration(SupportedConverterTypes.SCT_EXTENSION, "50.6", "us", gitTestURL, gitUsername, gitPassword, 
				artifactRepository, repositoryUsername, repositoryPassword));
	}
}

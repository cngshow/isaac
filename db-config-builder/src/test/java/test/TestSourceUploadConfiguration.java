package test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
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
		
		String artifactRepository = "http://vadev.mantech.com:8081/nexus/content/sites/ets_tooling_snapshot/";
		String repositoryUsername = "";
		String repositoryPassword = "";

		System.setProperty("java.awt.headless", "true");
		
		File f = new File("testJunk");
		f.mkdir();
		Files.write(new File(f, "foo.txt").toPath(), "Hi there".getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		
		System.out.println(GitPublish.readTags(gitTestURL, gitUsername, gitPassword));
		
		System.out.println(SrcUploadCreator.createSrcUploadConfiguration(SupportedConverterTypes.SCT_EXTENSION, "50.6", "us", f, gitTestURL, gitUsername, gitPassword, 
				artifactRepository, repositoryUsername, repositoryPassword));
	}
}

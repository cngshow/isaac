package test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import gov.vha.isaac.ochre.pombuilder.FileUtil;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.converter.SupportedConverterTypes;
import gov.vha.isaac.ochre.pombuilder.upload.SrcUploadCreator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

public class TestSourceUploadConfiguration
{

	public static void main(String[] args) throws Throwable
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
		
		ArrayList<File> files = new ArrayList<>();
		files.add(new File(f, "foo.txt"));
		
		System.out.println(GitPublish.readTags(gitTestURL, gitUsername, gitPassword));
		
		Task<String> t = SrcUploadCreator.createSrcUploadConfiguration(SupportedConverterTypes.SCT_EXTENSION, "50.6", "us", files, gitTestURL, gitUsername, gitPassword, 
				artifactRepository, repositoryUsername, repositoryPassword);
		
		t.progressProperty().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				System.out.println("Progress " + newValue);
			}
		});
		t.messageProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				System.out.println("Message " + newValue);
			}
		});
		t.titleProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				System.out.println("Title " + newValue);
			}
		});
		
		SrcUploadCreator.executeAndBlock(t);
		System.out.println("Result " + t.get());
		FileUtil.recursiveDelete(f);
	}
}

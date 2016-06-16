package test;

import gov.vha.isaac.ochre.pombuilder.artifacts.IBDFFile;
import gov.vha.isaac.ochre.pombuilder.dbbuilder.DBConfigurationCreator;

public class TestDBConfiguration
{

	public static void main(String[] args) throws Exception
	{
		String testURL = "https://github.com/darmbrust/test.git";
		String username = "";
		String password = "";
		
		System.setProperty("java.awt.headless", "true");
		
		//VHAT
		System.out.println(DBConfigurationCreator.createDBConfiguration("vhat-test", "2.0", "a test database", "all", true, 
			new IBDFFile[] {new IBDFFile("gov.vha.isaac.terminology.converted", "vhat-ibdf", "2016.01.07-loader-4.1-SNAPSHOT")},
			"3.03-SNAPSHOT",
			testURL, username, password));
		
		//VETS
		System.out.println(DBConfigurationCreator.createDBConfiguration("vets-test", "2.0", "a test database", "all", true, 
			new IBDFFile[] {
				new IBDFFile("gov.vha.isaac.terminology.converted", "vhat-ibdf", "2016.01.07-loader-4.1-SNAPSHOT"),
				new IBDFFile("gov.vha.isaac.terminology.converted", "loinc-ibdf", "2.54-loader-5.1-SNAPSHOT"),
				new IBDFFile("gov.vha.isaac.terminology.converted", "rf2-ibdf-sct", "20150731-loader-3.1-SNAPSHOT", "Snapshot")},
			"3.03-SNAPSHOT",
			testURL, username, password));
	}
}

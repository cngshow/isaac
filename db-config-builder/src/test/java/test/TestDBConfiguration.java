package test;

import gov.vha.isaac.ochre.pombuilder.artifacts.IBDFFile;
import gov.vha.isaac.ochre.pombuilder.dbbuilder.DBConfigurationCreator;

public class TestDBConfiguration
{

	public static void main(String[] args) throws Exception
	{
		DBConfigurationCreator.createDBConfiguration("test", "1.0", "a test database", "all", true, new IBDFFile[] {new IBDFFile("org.foo", "loinc", "5.0")}, "4",
			"https://github.com/darmbrust/test.git", "", "");
	}
}

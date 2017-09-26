package test;

import gov.vha.isaac.ochre.pombuilder.GitProperties;
import gov.vha.isaac.ochre.pombuilder.GitPropertiesImpl;
import gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferConfigurationCreator;
import gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties;
import gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferPropertiesImpl;

public class TestIBDFDifferConfiguration {

	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "true");

		GitProperties gitProps = new GitPropertiesImpl();
		gitProps.setGitRepositoryURL("https://vadev.mantech.com:4848/git/r/ibdf-differ-test.git");
		gitProps.setGitUsername("");
		gitProps.setGitPassword("".toCharArray());

		IBDFDifferProperties dbProps = new IBDFDifferPropertiesImpl();

		// general
		dbProps.setName("IBDF-Diff-Generator"); //no spaces
		dbProps.setVersion("1.0-SNAPSHOT"); //no spaces
		dbProps.setDescription("Generate deltas between versions of the same database.");

		// new
		dbProps.setNewArtifactId("vhat-ibdf-new");
		dbProps.setNewGroupId("gov.vha.isaac.terminology.converted");
		dbProps.setNewVersion("2016.08.18-loader");
		dbProps.setNewLoader("4.17-SNAPSHOT");

		// base
		dbProps.setBaseArtifactId("vhat-ibdf-old");
		dbProps.setBaseGroupId("gov.vha.isaac.terminology.converted");
		dbProps.setBaseVersion("2016.08.18-loader");
		dbProps.setBaseLoader("4.17-SNAPSHOT");

		// database
		dbProps.setDbGroupId("gov.vha.isaac.db");
		dbProps.setDbArtifactId("SNOMED");
		dbProps.setDbVersion("1.0-Me");
		dbProps.setDbClassifier("all");
		dbProps.setDbType("cradle.zip");
		dbProps.setDbIndexType("lucene.zip");

		// diff data
		dbProps.setImportDate("2017.05.04");
		dbProps.setDeltaIbdfFileName("delta.ibdf.file.name");
		dbProps.setGenerateAnalysisFiles(true);
		dbProps.setConverterSourceArtifactVersion("2017.05.04");

		// diff settings
		dbProps.setDiffOnTimestamp(true);
		dbProps.setDiffOnAuthor(true);
		dbProps.setDiffOnModule(true);
		dbProps.setDiffOnPath(true);

		System.out.println(IBDFDifferConfigurationCreator.createDBDiffConfiguration(dbProps, gitProps));

	}
}

package gov.vha.isaac.ochre.ibdf.differ;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService.ChangeType;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
import gov.vha.isaac.ochre.api.util.DBLocator;
import gov.vha.isaac.ochre.ibdf.provider.BinaryDataDifferProvider;

/**
 * Unit test for IbdfDiff.
 */
public class IbdfDiffTest {
	private static final Logger log = LogManager.getLogger();
	private File dataStoreLocation = new File("target/db");

	BinaryDataDifferProvider differProvider = new BinaryDataDifferProvider();

	@Before
	public void setupDB() throws Exception {

		// Make sure the service Locator comes up ok
		LookupService.get();

		dataStoreLocation = DBLocator.findDBFolder(dataStoreLocation);

		if (!dataStoreLocation.exists()) {
			throw new IOException("Couldn't find a data store from the input of '"
					+ dataStoreLocation.getAbsoluteFile().getAbsolutePath() + "'");
		}
		if (!dataStoreLocation.isDirectory()) {
			throw new IOException(
					"The specified data store: '" + dataStoreLocation.getAbsolutePath() + "' is not a folder");
		}

		LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(dataStoreLocation.toPath());
		log.info("  Setup AppContext, data store location = " + dataStoreLocation.getCanonicalPath());

		LookupService.startupIsaac();

		log.info("Done setting up ISAAC");
	}
	
	@After
	public void shutdownDB() throws Exception {
		LookupService.shutdownIsaac();

		log.info("Done shutting down ISAAC");
	}


	/**
	 * Mimick the IbdfDiffMojo
	 */
	@Test
	public void testDiff() {
		// Parameters used by Mojo
		// Input Files
		final File oldVersionFile = new File("src/test/resources/data/VHAT-V1.ibdf");
		final File newVersionFile = new File("src/test/resources/data/VHAT-V2.ibdf");
		
		// Output Files
		final String ibdfFileOutputDir = "target/unitTestOutput/ibdfFileOutputDir/";
		final String analysisFilesOutputDir = "target/unitTestOutput/analysisFilesOutputDir/";
		final String changesetFileName = "vhatChangeset-2016-09-30";

		// Others
		final String importDate = "2016-09-30";
		final boolean diffOnStatus = true;
		final boolean diffOnAuthor = true;
		final boolean diffOnModule = true;
		final boolean diffOnPath = true;
		final boolean diffOnTimestamp = true;
		final boolean createAnalysisFiles = true;

		// Use ibdf & db stored in src/test/resources
		differProvider.initialize(analysisFilesOutputDir, ibdfFileOutputDir, changesetFileName, createAnalysisFiles,
				diffOnStatus, diffOnTimestamp, diffOnAuthor, diffOnModule, diffOnPath, importDate);

		try {
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap = differProvider
					.processVersion(oldVersionFile);

			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap = differProvider
					.processVersion(newVersionFile);

			Map<ChangeType, List<OchreExternalizable>> changedComponents = differProvider
					.identifyVersionChanges(oldContentMap, newContentMap);

			differProvider.generateDiffedIbdfFile(changedComponents);

			if (createAnalysisFiles) {
				differProvider.writeFilesForAnalysis(oldContentMap, newContentMap, changedComponents, ibdfFileOutputDir,
						analysisFilesOutputDir);
			}
		} catch (Exception e) {
			assertTrue(false);
		}
		/*
		 * LookupService.get(); LookupService.startupIsaac(); StampService
		 * service = LookupService.getService(StampService.class);
		 */

		assertTrue(true);
	}
}

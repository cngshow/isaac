/*
 * Copyright 2015 U.S. Department of Veterans Affairs.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.ibdf.diff;

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
import gov.vha.isaac.ochre.ibdf.provider.diff.BinaryDataDifferProvider;

/**
 * Unit test for BinaryDataDifferProvider. Uses database defined in pom
 * 
 * {@link BinaryDataDifferProvider}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */

public class BinaryDataDifferTest {
	private static final Logger log = LogManager.getLogger();

	private final String TERMINOLOGY_INPUT_FILE_NAME = "vhat-ibdf";
	private final String OLD_VERSION = "4.3-SNAPSHOT";
	private final String NEW_VERSION = "4.31-SNAPSHOT";
	private final File DATASTORE_PATH = new File("target/db");

	private BinaryDataDifferProvider differProvider = new BinaryDataDifferProvider();

	@Before
	public void setupDB() throws Exception {
		File dataStoreLocation = DBLocator.findDBFolder(DATASTORE_PATH);

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

		log.info("ISAAC shut down");
	}

	/**
	 * Mimick the IbdfDiffMojo
	 */
	@Test
	public void testDiff() {
		// Parameters used by Mojo
		// Input Files
		final File oldVersionFile = new File("src/test/resources/data/old/" + TERMINOLOGY_INPUT_FILE_NAME + ".ibdf");
		final File newVersionFile = new File("src/test/resources/data/new/" + TERMINOLOGY_INPUT_FILE_NAME + ".ibdf");

		// Output Files
		final String ibdfFileOutputDir = "target/unitTestOutput/ibdfFileOutputDir/";
		final String analysisFilesOutputDir = "target/unitTestOutput/analysisFilesOutputDir/";
		final String ouptutIbdfFileName = TERMINOLOGY_INPUT_FILE_NAME + "-Diff-" + OLD_VERSION + "-to-" + NEW_VERSION
				+ ".ibdf";

		// Others
		final String importDate = "2016-09-30";
		final boolean diffOnStatus = true;
		final boolean diffOnAuthor = true;
		final boolean diffOnModule = true;
		final boolean diffOnPath = true;
		final boolean diffOnTimestamp = true;
		final boolean createAnalysisFiles = true;

		differProvider.initialize(analysisFilesOutputDir, ibdfFileOutputDir, ouptutIbdfFileName, createAnalysisFiles,
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

		assertTrue(true);
	}
}
/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.commit.manager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.api.ChangeSetLoadService;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.SystemStatusService;
import gov.vha.isaac.ochre.api.commit.CommitService;

/**
 * {@link ChangeSetLoadProvider}
 * This will load all .ibdf files in the database directory. It will rename the ChangeSet.ibdf
 * and ChangeSet.json files so they are not over written when ChangeSetWriterHandler starts.
 * Please make sure only files to be loaded are in this directory for loading at application startup.
 * The database directory the parent directory of the value returned from
 * LookupService.getService(ConfigurationService.class).getDataStoreFolderPath();
 * ChangeSetWritterHandler must have a RunLevel greater than the value of ChangeSetLoadProvider
 * otherwise the file ChangeSetWriterHandler will overwrite and lock the ChangeSet files.
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
@Service
@RunLevel(value = 3)
public class ChangeSetLoadProvider implements ChangeSetLoadService {

	private static final Logger LOG = LogManager.getLogger();

	private static Optional<Path> databasePath;

	//For HK2
	private ChangeSetLoadProvider() {

	}

	@PostConstruct
	private void startMe() {
		try {
			LOG.info("Loading change set files.");
			databasePath = LookupService.getService(ConfigurationService.class).getDataStoreFolderPath();
			
			Path changesetPath = databasePath.get().resolve("changesets");
			Files.createDirectories(changesetPath);
			if (!changesetPath.toFile().isDirectory()) {
				throw new RuntimeException(
						"Cannot initialize Changeset Store - was unable to create " + changesetPath.toAbsolutePath());
			}
			
			LOG.debug("Looking for .ibdf file in {}.", changesetPath.toAbsolutePath());
			CommitService commitService = Get.commitService();
			Files.newDirectoryStream(changesetPath,
					path -> path.toFile().isFile() && path.toString().endsWith(".ibdf"))
			.forEach(path -> {
				LOG.debug("File {}", path.toAbsolutePath());
				try
				{
					Get.binaryDataReader(path).getStream().forEach(o -> {
						commitService.importNoChecks(o);
					});
					commitService.postProcessImportNoChecks();
				}
				catch (FileNotFoundException e) {
					LOG.error("Change Set Load Provider failed to load file {}", path.toAbsolutePath());
					throw new RuntimeException(e);
				}

			});

			//rename ChangeSet files after load otherwise ChangeSetWriterHandler will overwrite the files when it starts up.
			renameFiles(changesetPath);

			LOG.info("Finished Change Set Load Provider load.");

		} catch (Exception e) {
			LOG.error("Error ", e);
			LookupService.getService(SystemStatusService.class).notifyServiceConfigurationFailure("Change Set Load Provider", e);
			throw new RuntimeException(e);
		}
	}

	@PreDestroy
	private void stopMe() {
		LOG.info("Finished ChangeSet Load Provider pre-destory.");
	}

	private void renameFiles(Path changeSetPath) throws IOException {

		LOG.debug("Rename files for directory {}", changeSetPath.toAbsolutePath());

		Files.newDirectoryStream(changeSetPath,
				filePath -> filePath.toFile().isFile()
				&& (filePath.getFileName().toString().equalsIgnoreCase("ChangeSet.ibdf")
						|| filePath.getFileName().toString().equalsIgnoreCase("ChangeSet.json" )))
		.forEach(filePath -> {

			LOG.debug("Rename file {}", filePath.toAbsolutePath());
			try {
				Files.move(filePath, filePath.getParent().toAbsolutePath().resolve(
						System.currentTimeMillis() + "_" + filePath.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				LOG.error("Error renaming file {}", filePath.toAbsolutePath());
				LOG.error(e);
			}
		});


	}
}

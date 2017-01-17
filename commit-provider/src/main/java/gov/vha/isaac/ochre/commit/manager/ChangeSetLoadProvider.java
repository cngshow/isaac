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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.commit.manager;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
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
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.metacontent.MetaContentService;
import gov.vha.isaac.ochre.model.configuration.EditCoordinates;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;

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
public class ChangeSetLoadProvider implements ChangeSetLoadService
{

	private static final Logger LOG = LogManager.getLogger();

	private static Optional<Path> databasePath;
	private static final String CHANGESETS = "changesets";
	private static final String CHANGESETS_ID = "changesetId.txt";

	//For HK2
	private ChangeSetLoadProvider()
	{

	}

	@PostConstruct
	private void startMe()
	{
		try
		{
			AtomicInteger loaded = new AtomicInteger();
			AtomicInteger skipped = new AtomicInteger();

			LOG.info("Loading change set files.");
			databasePath = LookupService.getService(ConfigurationService.class).getDataStoreFolderPath();

			Path changesetPath = databasePath.get().resolve(CHANGESETS);
			Files.createDirectories(changesetPath);
			if (!changesetPath.toFile().isDirectory())
			{
				throw new RuntimeException("Cannot initialize Changeset Store - was unable to create " + changesetPath.toAbsolutePath());
			}

			UUID chronicleDbId = Get.conceptService().getDataStoreId();
			if (chronicleDbId == null)
			{
				throw new RuntimeException("Chronicle store did not return a dbId!");
			}

			UUID changesetsDbId = null;
			Path changesetsIdPath = changesetPath.resolve(CHANGESETS_ID);
			if (changesetsIdPath.toFile().exists())
			{
				try
				{
					changesetsDbId = UUID.fromString(new String(Files.readAllBytes(changesetsIdPath)));
				}
				catch (Exception e)
				{
					LOG.warn("The " + CHANGESETS_ID + " file does not contain a valid UUID!", e);
				}
			}

			UUID sememeDbId = readSememeDbId();
			
			if ((sememeDbId != null && !chronicleDbId.equals(sememeDbId)) || changesetsDbId != null && !chronicleDbId.equals(changesetsDbId))
			{
				throw new RuntimeException(
						"Database identity mismatch!  ChronicleDbId: " + chronicleDbId + " SememeDbId: " + sememeDbId + " Changsets DbId: " + changesetsDbId);
			}
			
			if (changesetsDbId == null)
			{
				changesetsDbId = chronicleDbId;
				Files.write(changesetsIdPath, changesetsDbId.toString().getBytes());
			}
			
			//if the sememeDbId is null, lets wait and see if it appears after processing the changesets.

			//We store the list of files that we have already read / processed in the metacontent store, so we don't have to process them again.
			//files that "appear" in this folder via the git integration, for example, we will need to process - but files that we create 
			//during normal operation do not need to be reprocessed.  The BinaryDataWriterProvider also automatically updates this list with the 
			//files as it writes them.
			MetaContentService mcs = LookupService.get().getService(MetaContentService.class);
			final ConcurrentMap<String, Boolean> processedChangesets = mcs == null ? null : mcs.<String, Boolean>openStore("processedChangesets");

			LOG.debug("Looking for .ibdf file in {}.", changesetPath.toAbsolutePath());
			CommitService commitService = Get.commitService();
			Files.newDirectoryStream(changesetPath, path -> path.toFile().isFile() && path.toString().endsWith(".ibdf")).forEach(path -> {
				LOG.debug("File {}", path.toAbsolutePath());
				try
				{
					if (processedChangesets != null && processedChangesets.containsKey(path.getFileName().toString()))
					{
						skipped.incrementAndGet();
						LOG.debug("Skipping already processed changeset file");
					}
					else
					{
						loaded.incrementAndGet();
						LOG.debug("Importing changeset file");
						Get.binaryDataReader(path).getStream().forEach(o -> {
							commitService.importNoChecks(o);
						});
						commitService.postProcessImportNoChecks();
						if (processedChangesets != null)
						{
							processedChangesets.put(path.getFileName().toString(), true);
						}
					}
				}
				catch (FileNotFoundException e)
				{
					LOG.error("Change Set Load Provider failed to load file {}", path.toAbsolutePath());
					throw new RuntimeException(e);
				}
			});

			LOG.info("Finished Change Set Load Provider load.  Loaded {}, Skipped {} because they were previously processed", loaded.get(), skipped.get());
			
			if (sememeDbId == null)
			{
				sememeDbId = readSememeDbId();
				if (!Get.configurationService().inDBBuildMode() && sememeDbId == null)
				{
					if (loaded.get() > 0)
					{
						LOG.warn("No database identify was found stored in a sememe, after loading changesets.");
					}
					Get.sememeBuilderService().getStringSememeBuilder(chronicleDbId.toString(), TermAux.ISAAC_ROOT.getNid(), TermAux.DATABASE_UUID.getConceptSequence())
						.build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE).get();
					Get.commitService().commit("Storing database ID on root concept");
				}
			}
		}
		catch (Exception e)
		{
			LOG.error("Error ", e);
			LookupService.getService(SystemStatusService.class).notifyServiceConfigurationFailure("Change Set Load Provider", e);
			throw new RuntimeException(e);
		}
	}

	@PreDestroy
	private void stopMe()
	{
		LOG.info("Finished ChangeSet Load Provider pre-destory.");
	}
	
	private UUID readSememeDbId()
	{
		Optional<SememeChronology<? extends SememeVersion<?>>> sdic = Get.sememeService()
				.getSememesForComponentFromAssemblage(TermAux.ISAAC_ROOT.getNid(), TermAux.DATABASE_UUID.getConceptSequence()).findFirst();
		if (sdic.isPresent())
		{
			Optional<LatestVersion<StringSememe>> sdi = ((SememeChronology) sdic.get()).getLatestVersion(StringSememe.class, StampCoordinates.getDevelopmentLatest());
			if (sdi.isPresent())
			{
				try
				{
					return UUID.fromString(sdi.get().value().getString());
				}
				catch (Exception e)
				{
					LOG.warn("The Database UUID annotation on Isaac Root does not contain a valid UUID!", e);
				}
			}
		}
		return null;
	}
	
}

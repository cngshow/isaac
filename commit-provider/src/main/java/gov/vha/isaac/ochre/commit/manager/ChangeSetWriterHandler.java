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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.SystemStatusService;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.collections.SememeSequenceSet;
import gov.vha.isaac.ochre.api.commit.ChangeSetListener;
import gov.vha.isaac.ochre.api.commit.ChangeSetWriterService;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.externalizable.DataWriterService;
import gov.vha.isaac.ochre.api.externalizable.MultipleDataWriterService;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.util.NamedThreadFactory;

/**
 * {@link ChangeSetWriterHandler}
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
@Service(name = "Change Set Writer Handler")
@RunLevel(value = LookupService.SL_L4_ISAAC_STARTED_RUNLEVEL)
public class ChangeSetWriterHandler implements ChangeSetWriterService, ChangeSetListener {

	private static final Logger LOG = LogManager.getLogger();

	private static final String jsonFileSuffix = "json";
	private static final String ibdfFileSuffix = "ibdf";
	private static final String CHANGESETS = "changesets";
	private DataWriterService writer;
	private final UUID changeSetWriterHandlerUuid = UUID.randomUUID();
	private ExecutorService changeSetWriteExecutor;
	private boolean writeEnabled;
	private Boolean dbBuildMode;
	private Path changeSetFolder;

	public ChangeSetWriterHandler() throws Exception {

		Optional<Path> databasePath = LookupService.getService(ConfigurationService.class).getDataStoreFolderPath();

		changeSetFolder = databasePath.get().resolve(CHANGESETS);
		Files.createDirectories(changeSetFolder);
		if (!changeSetFolder.toFile().isDirectory()) {
			throw new RuntimeException(
					"Cannot initialize Changeset Store - was unable to create " + changeSetFolder.toAbsolutePath());
		}

		writer = new MultipleDataWriterService(changeSetFolder, "ChangeSet-", Optional.of(jsonFileSuffix), Optional.of(ibdfFileSuffix));
	}

	private void sequenceSetChange(ConceptSequenceSet conceptSequenceSet) {

		conceptSequenceSet.stream().forEach((conceptSequence) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptSequence);
			try {
				writeToFile(concept);
			} catch (Exception e) {
				throw new RuntimeException("Error writing concept " + conceptSequence , e);
			}
		});
	}

	private void sequenceSetChange(SememeSequenceSet sememeSequenceSet) {

		sememeSequenceSet.stream().forEach((sememeSequence) -> {
			SememeChronology<? extends SememeVersion<?>> sememe = Get.sememeService().getSememe(sememeSequence);
			try {
				writeToFile(sememe);
			} catch (Exception e) {
				throw new RuntimeException("Error writing sememe " + sememeSequence, e);
			}
		});
	}

	private void writeToFile(OchreExternalizable ochreObject) throws IOException {
		writer.put(ochreObject);
	}


	@PostConstruct
	private void startMe() {
		try {
			LOG.info("Starting ChangeSetWriterHandler post-construct");
			enable();

			changeSetWriteExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("ISAAC-changeset-write", false));
			Get.postCommitService().addChangeSetListener(this);

		} catch(Exception e) {
			LOG.error("Error in ChangeSetWriterHandler post-construct ", e);
			LookupService.getService(SystemStatusService.class).notifyServiceConfigurationFailure("Change Set Writer Handler", e);
			throw new RuntimeException(e);
		}
	}

	@PreDestroy
	private void stopMe()
	{
		LOG.info("Stopping ChangeSetWriterHandler pre-destroy");
		disable();
		if (changeSetWriteExecutor != null)
		{
			changeSetWriteExecutor.shutdown();
			changeSetWriteExecutor = null;
		}
		if (writer != null) {
			LOG.debug("Close writer");
			try
			{
				writer.close();
			}
			catch (IOException e)
			{
				LOG.error("Error closing changeset writer!", e);
			}
			finally
			{
				writer = null;
			}
		}

	}

	@Override
	public UUID getListenerUuid() {
		return changeSetWriterHandlerUuid;
	}

	@Override
	public void handlePostCommit(CommitRecord commitRecord) {

		LOG.info("handle Post Commit");
		if (dbBuildMode == null)
		{
			dbBuildMode = Get.configurationService().inDBBuildMode();
			if (dbBuildMode)
			{
				stopMe();
			}
		}
		if (writeEnabled && !dbBuildMode)
		{
			//Do in the backgound
			Runnable r = new Runnable() {

				@Override
				public void run()
				{
					try
					{
						if (commitRecord.getConceptsInCommit() != null && commitRecord.getConceptsInCommit().size() > 0)
						{
							sequenceSetChange(commitRecord.getConceptsInCommit());
							LOG.debug("handle Post Commit: {} concepts", commitRecord.getConceptsInCommit().size() );
						}
						if (commitRecord.getSememesInCommit() != null && commitRecord.getSememesInCommit().size() > 0)
						{
							sequenceSetChange(commitRecord.getSememesInCommit());
							LOG.debug("handle Post Commit: {} sememes", commitRecord.getSememesInCommit().size());
						}
					} catch (Exception e) {
						LOG.error("Error in Change set writer handler ", e);
						throw new RuntimeException(e);
					}
				}
			};

			changeSetWriteExecutor.execute(r);
		}
		else
		{
			LOG.info("ChangeSetWriter ignoring commit");
		}
	}

	@Override
	public void disable()
	{
		writeEnabled = false;
	}

	@Override
	public void enable()
	{
		writeEnabled = true;
	}

	@Override
	public boolean getWriteStatus()
	{
		return writeEnabled;
	}


	@Override
	public void pause() throws IOException
	{
		if (writer != null)
		{
			writer.pause();
		}
	}

	@Override
	public void resume() throws IOException
	{
		if (writer != null)
		{
			writer.resume();
		}
	}

	@Override
	public Path getWriteFolder()
	{
		return changeSetFolder;
	}
}

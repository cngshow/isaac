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
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

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
import gov.vha.isaac.ochre.api.externalizable.BinaryDataWriterService;
import gov.vha.isaac.ochre.api.externalizable.MultipleDataWriterService;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;

/**
 * {@link ChangeSetWriterHandler}
 *
 *
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
@Service(name = "Change Set Writer Handler")
@RunLevel(value = 2)
public class ChangeSetWriterHandler implements ChangeSetWriterService, ChangeSetListener {

	private static final Logger LOG = LogManager.getLogger();

	private Path filePath;
	private static final String jsonFileSuffix = ".json";
	private static final String ibdfFileSuffix = ".ibdf";
	private BinaryDataWriterService writer;
	private final UUID changeSetWriterHandlerUuid = UUID.randomUUID();

	public ChangeSetWriterHandler() throws Exception {

		filePath = Paths.get("C:/Users/Nuno/Desktop/test/");
		//filePath = LookupService.getService(ChangeSetWriterService.class).getChronicleFolderPath().resolve("changeset-service");

		if (!Files.exists(filePath)) {
			Files.createDirectories(filePath);
		}

		Optional<Path> jsonPath = Optional.of(filePath.resolve("ChangeSet" + jsonFileSuffix));
		Optional<Path> ibdfPath = Optional.of(filePath.resolve("ChangeSet" + ibdfFileSuffix));
		writer = new MultipleDataWriterService(jsonPath, ibdfPath);
	}


	/*
	 */
	@Override
	public void sequenceSetChange(ConceptSequenceSet conceptSequenceSet) {

		conceptSequenceSet.stream().forEach((conceptSequence) -> {
			ConceptChronology<? extends ConceptVersion<?>> concept = Get.conceptService().getConcept(conceptSequence);
			try {
				writeToFile(concept);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	/*
	 */
	@Override
	public void sequenceSetChange(SememeSequenceSet sememeSequenceSet) {

		sememeSequenceSet.stream().forEach((sememeSequence) -> {
			SememeChronology<? extends SememeVersion<?>> sememe = Get.sememeService().getSememe(sememeSequence);
			try {
				writeToFile(sememe);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void writeToFile(OchreExternalizable ochreObject) throws IOException {

		/*
		Runnable r = new Runnable() {
			@Override
			public void run() {
				writer.put(ochreObject);
			}
		};
		ExecutorService executor = Executors.newCachedThreadPool();
		executor.submit(r);
		 */

		writer.put(ochreObject);
	}


	@PostConstruct
	private void startMe() {
		try {
			LOG.info("Starting ChangeSetWriterHandler post-construct");
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
		if (writer != null) {
			LOG.debug("Close writer");
			writer.close();
		}
	}

	@Override
	public UUID getListenerUuid() {
		return changeSetWriterHandlerUuid;
	}

	@Override
	public void handlePostCommit(CommitRecord commitRecord) {
		try {
			if (commitRecord.getConceptsInCommit() != null) {
				sequenceSetChange(commitRecord.getConceptsInCommit());
			}
			if (commitRecord.getSememesInCommit() != null) {
				sequenceSetChange(commitRecord.getSememesInCommit());
			}
		} catch (Exception e) {
			LOG.error("Error in Change set writer handler ", e.getMessage());
			throw new RuntimeException(e);
		}

	}

}

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
package gov.vha.isaac.ochre.ibdf.provider.diff;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import javax.activation.UnsupportedDataTypeException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import com.cedarsoftware.util.io.JsonWriter;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IdentifierService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.commit.CommitService;
import gov.vha.isaac.ochre.api.commit.StampService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderQueueService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderService;
import gov.vha.isaac.ochre.api.externalizable.DataWriterService;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
import gov.vha.isaac.ochre.api.externalizable.StampAlias;
import gov.vha.isaac.ochre.api.externalizable.StampComment;
import gov.vha.isaac.ochre.api.externalizable.json.JsonDataWriterService;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;

/**
 * Routines enabling the examination of two ibdf files containing two distinct
 * versions of the same terminology and identifies the new/inactivated/modified
 * content between the two versions.
 * 
 * Once identified, a new changeset file may be generated containing these
 * changes. This file can then be imported into an existing database containing
 * the old version of the terminology. This will upgrade it to the new
 * terminology.
 * 
 * {@link BinaryDataDifferService}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Service(name = "binary data differ")
@Singleton
// TODO there are some serious thread-safety issues in this class
public class BinaryDataDifferProvider implements BinaryDataDifferService {

	/** The log. */
	private final Logger log = LogManager.getLogger(BinaryDataDifferProvider.class);

	/** object to lock on */
	private final static ReentrantLock diffProcessLock = new ReentrantLock();

	/** The diff util. */
	private final BinaryDataDifferProviderUtility diffUtil = new BinaryDataDifferProviderUtility();

	/** The comment count. */
	private AtomicInteger conceptCount = new AtomicInteger();
	private AtomicInteger sememeCount = new AtomicInteger();
	private AtomicInteger aliasCount = new AtomicInteger();
	private AtomicInteger commentCount = new AtomicInteger();
	private AtomicInteger itemCount = new AtomicInteger();
	
	/** The input analysis dir. */
	// Analysis File Readers/Writers
	private AtomicReference<String>  inputAnalysisDirStr = new AtomicReference<String>();

	/** The comparison analysis dir. */
	private AtomicReference<String>  analysisArtifactsDirStr = new AtomicReference<String>();

	/** The delta ibdf file path. */
	private AtomicReference<String>  deltaIbdfFilePathStr = new AtomicReference<String>();

	/** The module uuid. */
	private AtomicReference<String> moduleUuidStr = new AtomicReference<String>();

	/** The text input file name. */
	private final String textBaseNewIbdfFileName = "bothVersions.txt";

	/** The json full comparison file name. */
	private final String jsonFullComparisonFileName = "allChangedComponents.json";

	/** The text full comparison file name. */
	private final String textFullComparisonFileName = "allChangedComponents.txt";

	/** The skipped items. */
	ConcurrentSkipListSet<Integer> skippedItems = new ConcurrentSkipListSet<>();

	/**
	 * Instantiates a new binary data differ provider.
	 */
	public BinaryDataDifferProvider() {
		// For HK2
		log.info("binary data differ constructed");
	}

	/**
	 * Start me.
	 */
	@PostConstruct
	private void startMe() {
		log.info("Starting BinaryDataDifferProvider.");
	}

	/**
	 * Stop me.
	 */
	@PreDestroy
	private void stopMe() {
		log.info("Stopping BinaryDataDifferProvider.");
	}

	@Override
	public Map<ChangeType, List<OchreExternalizable>> computeDelta(
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> baseContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap) {
		List<OchreExternalizable> addedComponents = new ArrayList<>();
		List<OchreExternalizable> retiredComponents = new ArrayList<>();
		List<OchreExternalizable> changedComponents = new ArrayList<>();

		final int activeStampSeq = createStamp(State.ACTIVE);
		final int inactiveStampSeq = createStamp(State.INACTIVE);

		// Find existing
		for (OchreExternalizableObjectType type : OchreExternalizableObjectType.values()) {

			if (type.equals(OchreExternalizableObjectType.CONCEPT)
					|| type.equals(OchreExternalizableObjectType.SEMEME)) {
				processVersionedContent(type, baseContentMap, newContentMap, addedComponents, retiredComponents,
						changedComponents, activeStampSeq, inactiveStampSeq);
			} else if (type.equals(OchreExternalizableObjectType.STAMP_ALIAS)
					|| type.equals(OchreExternalizableObjectType.STAMP_COMMENT)) {
				processStampedContent(type, baseContentMap, newContentMap, addedComponents);
			}
		}

		// Having identified the new, retired, and modified content of all
		// OchreExternalizable types, add them to return map
		Map<ChangeType, List<OchreExternalizable>> retMap = new HashMap<>();
		retMap.put(ChangeType.NEW_COMPONENTS, addedComponents);
		retMap.put(ChangeType.RETIRED_COMPONENTS, retiredComponents);
		retMap.put(ChangeType.MODIFIED_COMPONENTS, changedComponents);

		return retMap;
	}

	/**
	 * Identify changes between the base ibdf file and the new ibdf file for the
	 * types {@link #OchreExternalizableObjectType.CONCEPT} and
	 * {@link #OchreExternalizableObjectType.SEMEME}.
	 *
	 * @param type
	 *            the {@link OchreExternalizableObjectType} type
	 * @param baseContentMap
	 *            the base ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param newContentMap
	 *            the new ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param addedComponents
	 *            all {@link OchreExternalizable} determined to be new in the
	 *            new content map
	 * @param retiredComponents
	 *            all {@link OchreExternalizable} determined to be inactivated
	 *            in the new content map
	 * @param changedComponents
	 *            all {@link OchreExternalizable} determined to be modified in
	 *            the new content map
	 * @param activeStampSeq
	 *            the active stamp sequence used to make new versions
	 * @param inactiveStampSeq
	 *            the inactive stamp sequence used to make new versions
	 */
	private void processVersionedContent(OchreExternalizableObjectType type,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> baseContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap,
			List<OchreExternalizable> addedComponents, List<OchreExternalizable> retiredComponents,
			List<OchreExternalizable> changedComponents, int activeStampSeq, int inactiveStampSeq) {
		Set<UUID> matchedComponentSet = new HashSet<UUID>();
		CommitService commitService = Get.commitService();

		// Search for modified components
		for (OchreExternalizable baseComp : baseContentMap.get(type)) {
			ObjectChronology<?> baseCompChron = (ObjectChronology<?>) baseComp;
			for (OchreExternalizable newComp : newContentMap.get(type)) {
				ObjectChronology<?> newCompChron = (ObjectChronology<?>) newComp;

				if (baseCompChron.getPrimordialUuid().equals(newCompChron.getPrimordialUuid())) {
					matchedComponentSet.add(baseCompChron.getPrimordialUuid());

					try {
						OchreExternalizable modifiedComponents = diffUtil.diff(baseCompChron, newCompChron,
								activeStampSeq, type);
						if (modifiedComponents != null) {
							changedComponents.add(modifiedComponents);
						}
					} catch (Exception e) {
						log.error("Failed On type: " + type + " on component: " + baseCompChron.getPrimordialUuid());
						e.printStackTrace();
					}

					continue;
				}
			}
		}

		// Add baseComps not in matchedSet
		for (OchreExternalizable baseComp : baseContentMap.get(type)) {
			if (!matchedComponentSet.contains(((ObjectChronology<?>) baseComp).getPrimordialUuid())) {
				OchreExternalizable retiredComp = diffUtil.addNewInactiveVersion(baseComp,
						baseComp.getOchreObjectType(), inactiveStampSeq);

				if (retiredComp != null) {
					retiredComponents.add(retiredComp);
				}
			}
		}

		// Add newComps not in matchedSet
		for (OchreExternalizable newComp : newContentMap.get(type)) {
			if (!matchedComponentSet.contains(((ObjectChronology<?>) newComp).getPrimordialUuid())) {

				OchreExternalizable addedComp = diffUtil.diff(null, (ObjectChronology<?>) newComp, activeStampSeq,
						type);

				if (addedComp != null) {
					addedComponents.add(addedComp);
					commitService.importNoChecks(addedComp);
				}
			}
		}

		commitService.postProcessImportNoChecks();
	}

	/**
	 * Identify new stamp-=based content added between the base ibdf file and
	 * the new ibdf file for the types
	 * {@link #OchreExternalizableObjectType.STAMP_ALIAS} and
	 * {@link #OchreExternalizableObjectType.STAMP_COMMENT}.
	 *
	 * @param type
	 *            the {@link OchreExternalizableObjectType} type
	 * @param baseContentMap
	 *            the base ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param newContentMap
	 *            the new ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param addedComponents
	 *            all {@link OchreExternalizable} determined to be new in the
	 *            new content map
	 */
	private void processStampedContent(OchreExternalizableObjectType type,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> baseContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap,
			List<OchreExternalizable> addedComponents) {
		Set<Integer> matchedStampSequenceSet = new HashSet<Integer>();
		CommitService commitService = Get.commitService();

		// Search for modified stamp content
		for (OchreExternalizable baseComp : baseContentMap.get(type)) {
			boolean matchFound = false;
			for (OchreExternalizable newComp : newContentMap.get(type)) {
				if (type.equals(OchreExternalizableObjectType.STAMP_ALIAS)) {
					if (((StampAlias) baseComp).equals((StampAlias) newComp)) {
						matchedStampSequenceSet.add(((StampAlias) baseComp).getStampSequence());
						matchFound = true;
					}
				} else {
					if (((StampComment) baseComp).equals((StampComment) newComp)) {
						matchedStampSequenceSet.add(((StampComment) baseComp).getStampSequence());
						matchFound = true;
					}
				}

				if (matchFound) {
					continue;
				}
			}
		}

		// Add new stamp content not in matchedSet
		for (OchreExternalizable newComp : newContentMap.get(type)) {
			int newSequence;
			if (type.equals(OchreExternalizableObjectType.STAMP_ALIAS)) {
				newSequence = ((StampAlias) newComp).getStampSequence();
			} else {
				newSequence = ((StampComment) newComp).getStampSequence();
			}

			if (!matchedStampSequenceSet.contains(newSequence)) {
				addedComponents.add(newComp);
				commitService.importNoChecks(newComp);
			}
		}

		commitService.postProcessImportNoChecks();
	}

	@Override
	public void generateDeltaIbdfFile(Map<ChangeType, List<OchreExternalizable>> changedComponents) throws IOException {
		DataWriterService componentCSWriter = Get.binaryDataWriter(new File(deltaIbdfFilePathStr.get()).toPath());

		for (ChangeType key : changedComponents.keySet()) {
			for (OchreExternalizable c : changedComponents.get(key)) {
				componentCSWriter.put(c);
			}
		}

		componentCSWriter.close();
	}

	@Override
	public void generateAnalysisFiles(Map<OchreExternalizableObjectType, Set<OchreExternalizable>> baseContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap,
			Map<ChangeType, List<OchreExternalizable>> changedComponents) {
		try {
			// Handle Input Files
			if (baseContentMap != null) {
				generateInputAnalysisFile(baseContentMap, "BASE", "baseVersion.json");
			} else {
				log.info("baseContentMap empty so not writing json/text Input files for base content");
			}

			if (newContentMap != null) {
				generateInputAnalysisFile(newContentMap, "NEW", "newVersion.json");
			} else {
				log.info("newContentMap empty so not writing json/text Input files for new content");
			}

			// Handle Comparison Files
			if (changedComponents != null) {
				generateComparisonAnalysisFile(changedComponents);
				writeChangeSetForVerification();
			} else {
				log.info("changedComponents empty so not writing json/text Output files");
			}

		} catch (IOException e) {
			log.error(
					"Failed in creating analysis files (not in processing the content written to the analysis files)");
		}
	}

	/**
	 * Generate a json and txt file containing the changed components. The
	 * printed out contents are grouped by {@link ChangeType}, and within each
	 * {@link ChangeType}, by {@link OchreExternalizable}.
	 *
	 * @param changedComponents
	 *            map of {@link ChangeType} defined as (new, inactivated,
	 *            modified) to the identified {@link OchreExternalizable}
	 *            content
	 * @throws IOException
	 *             Thrown if an exception occurred in writing the json or text
	 *             files.
	 */
	private void generateComparisonAnalysisFile(Map<ChangeType, List<OchreExternalizable>> changedComponents)
			throws IOException {
		try (FileWriter allChangesTextWriter = new FileWriter(analysisArtifactsDirStr.get() + textFullComparisonFileName);
				JsonDataWriterService allChangesJsonWriter = new JsonDataWriterService(
						new File(analysisArtifactsDirStr.get() + jsonFullComparisonFileName));) {

			for (ChangeType key : changedComponents.keySet()) {
				int counter = 1;

				FileWriter changeTypeWriter = new FileWriter(analysisArtifactsDirStr.get() + key + "_File.txt");

				try {
					List<OchreExternalizable> components = changedComponents.get(key);

					allChangesJsonWriter.put("\n\n\n\t\t\t**** " + key.toString() + " ****");
					allChangesTextWriter.write("\n\n\n\t\t\t**** " + key.toString() + " ****");

					for (OchreExternalizable c : components) {
						String componentType;
						if (c.getOchreObjectType() == OchreExternalizableObjectType.CONCEPT) {
							componentType = "Concept";
						} else if (c.getOchreObjectType() == OchreExternalizableObjectType.SEMEME) {
							componentType = "Sememe";
						} else if (c.getOchreObjectType() == OchreExternalizableObjectType.STAMP_ALIAS) {
							componentType = "Stamp Alias";
						} else if (c.getOchreObjectType() == OchreExternalizableObjectType.STAMP_COMMENT) {
							componentType = "Stamp Comment";
						} else {
							throw new UnsupportedDataTypeException();
						}

						String componentToWrite;
						if ((c.getOchreObjectType() == OchreExternalizableObjectType.CONCEPT
								|| c.getOchreObjectType() == OchreExternalizableObjectType.SEMEME)) {
							componentToWrite = "---- " + key.toString() + " component #" + counter++ + " of type "
									+ componentType + " with Primordial UUID: "
									+ ((ObjectChronology<?>) c).getPrimordialUuid() + " ----\n";
						} else {
							int sequence;
							if (c.getOchreObjectType() == OchreExternalizableObjectType.STAMP_ALIAS) {
								sequence = ((StampAlias) c).getStampSequence();
							} else {
								sequence = ((StampComment) c).getStampSequence();
							}
							componentToWrite = "---- " + key.toString() + " #" + counter++ + " of type '"
									+ componentType + "' with Sequence# " + sequence + " ----\n";
						}

						// Print Header
						allChangesJsonWriter.put(componentToWrite);
						allChangesTextWriter.write("\n\n\n\t\t\t" + componentToWrite);
						changeTypeWriter.write("\n\n\n\t\t\t" + componentToWrite);
						// Print Value (JSON Working TXT has issues)
						allChangesJsonWriter.put(c);

						try {
							changeTypeWriter.write(c.toString() + "\n\n\n");
						} catch (Exception e) {
							// This process will be trying to read content not
							// yet imported into database. So not actual error
							// Exception thrown.
						}

						try {
							allChangesTextWriter.write(c.toString());
						} catch (Exception e) {
							// This process will be trying to read content not
							// yet imported into database. So not actual error
							// Exception thrown.
						}
					}
				} catch (IOException e) {
					log.error("Failure processing changes of type " + key.toString());
				} finally {
					changeTypeWriter.close();
				}
			}
		}

	}

	/**
	 * Returns a stamp sequence based on passed in a) {@link State}, b) "Import
	 * Date" and c) "Module UUID" while always using "USER" and "DEVELOPEMENT
	 * PATH".
	 *
	 * @param state
	 *            - state or null (for current)
	 * @return the stamp sequence
	 */
	private int createStamp(State state) {
		return LookupService.getService(StampService.class).getStampSequence(state, diffUtil.getNewImportDate(),
				TermAux.USER.getConceptSequence(), // Author
				LookupService.getService(IdentifierService.class).getConceptSequenceForUuids(UUID.fromString(moduleUuidStr.get())), // Module
				TermAux.DEVELOPMENT_PATH.getConceptSequence()); // Path
	}

	@Override
	public void initialize(String analysisArtifactsDirStr, String inputAnalysisDirStr, String deltaIbdfPathFile,
			Boolean generateAnalysisFiles, boolean diffOnTimestamp, boolean diffOnAuthor, boolean diffOnModule,
			boolean diffOnPath, String importDate, String moduleToCreate) {

		diffProcessLock.lock();

		diffUtil.setDiffOptions(diffOnTimestamp, diffOnAuthor, diffOnModule, diffOnPath);
		diffUtil.setNewImportDate(importDate);

		moduleUuidStr.set(UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC, moduleToCreate).toString());
		this.inputAnalysisDirStr.set(inputAnalysisDirStr);
		this.analysisArtifactsDirStr.set(analysisArtifactsDirStr);
		this.deltaIbdfFilePathStr.set(deltaIbdfPathFile);

		if (generateAnalysisFiles) {
			File f = new File(inputAnalysisDirStr);
			deleteDirectoryFiles(f);
			f.mkdirs();

			f = new File(analysisArtifactsDirStr);
			deleteDirectoryFiles(f);
			f.mkdirs();
		}
	}

	@Override
	public Map<OchreExternalizableObjectType, Set<OchreExternalizable>> processInputIbdfFile(File versionFile)
			throws Exception {
		log.info("Processing file: " + versionFile.getAbsolutePath());
		BinaryDataReaderService reader = Get.binaryDataReader(versionFile.toPath());

		itemCount.set(0);
		conceptCount.set(0);
		sememeCount.set(0);
		aliasCount.set(0);
		commentCount.set(0);

		Map<OchreExternalizableObjectType, Set<OchreExternalizable>> retMap = new HashMap<>();
		retMap.put(OchreExternalizableObjectType.CONCEPT, new HashSet<OchreExternalizable>());
		retMap.put(OchreExternalizableObjectType.SEMEME, new HashSet<OchreExternalizable>());
		retMap.put(OchreExternalizableObjectType.STAMP_ALIAS, new HashSet<OchreExternalizable>());
		retMap.put(OchreExternalizableObjectType.STAMP_COMMENT, new HashSet<OchreExternalizable>());
		try {
			reader.getStream().forEach((object) -> {
				if (object != null) {
					itemCount.getAndIncrement();

					try {
						if (object.getOchreObjectType() == OchreExternalizableObjectType.CONCEPT) {
							conceptCount.getAndIncrement();
							retMap.get(object.getOchreObjectType()).add(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.SEMEME) {
							sememeCount.getAndIncrement();
							retMap.get(object.getOchreObjectType()).add(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.STAMP_ALIAS) {
							aliasCount.getAndIncrement();
							retMap.get(object.getOchreObjectType()).add(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.STAMP_COMMENT) {
							commentCount.getAndIncrement();
							retMap.get(object.getOchreObjectType()).add(object);
						} else {
							throw new UnsupportedOperationException("Unknown ochre object type: " + object);
						}
					} catch (Exception e) {
						log.error("Failure at " + conceptCount + " concepts, " + sememeCount + " sememes, ", e);
						Map<String, Object> args = new HashMap<>();
						args.put(JsonWriter.PRETTY_PRINT, true);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						JsonWriter json = new JsonWriter(baos, args);

						UUID primordial = null;
						if (object instanceof ObjectChronology) {
							primordial = ((ObjectChronology<?>) object).getPrimordialUuid();
						}

						json.write(object);
						log.error("Failed on "
								+ (primordial == null ? ": "
										: "object with primoridial UUID " + primordial.toString() + ": ")
								+ baos.toString());
						json.close();

					}

					if (itemCount.get() % 10000 == 0) {
						log.info("Still processing ibdf file.  Status: " + itemCount.get() + " entries, " + "Loaded "
								+ conceptCount.get() + " concepts, " + sememeCount.get() + " sememes, "
								+ aliasCount.get() + " aliases, " + commentCount.get() + " comments");
					}
				}
			});
		} catch (Exception ex) {
			log.info("Exception during load: Loaded " + conceptCount.get() + " concepts, " + sememeCount.get()
					+ " sememes, " + aliasCount.get() + " aliases, " + commentCount.get() + " comments"
					+ (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));
			throw new Exception(ex.getLocalizedMessage(), ex);
		}

		log.info("Finished processing ibdf file.  Results: " + itemCount.get() + " entries, " + "Loaded "
				+ conceptCount.get() + " concepts, " + sememeCount.get() + " sememes, " + aliasCount.get()
				+ " aliases, " + commentCount.get() + " comments");

		return retMap;
	}

	/**
	 * Generates human readable analysis json file containing the contents in
	 * the ibdf delta file located at {@link #deltaIbdfFilePathStr}.
	 *
	 * @throws FileNotFoundException
	 *             Thrown if the delta file is not found at location specified
	 *             by {@link #deltaIbdfFilePathStr}
	 */
	private void writeChangeSetForVerification() throws FileNotFoundException {
		int ic = 0;
		int cc = 0;
		int sc = 0;
		int sta = 0;
		int stc = 0;

		BinaryDataReaderQueueService reader = Get.binaryDataQueueReader(new File(deltaIbdfFilePathStr.get()).toPath());
		BlockingQueue<OchreExternalizable> queue = reader.getQueue();

		Map<String, Object> args = new HashMap<>();
		args.put(JsonWriter.PRETTY_PRINT, true);

		try (FileOutputStream fos = new FileOutputStream(new File(analysisArtifactsDirStr.get() + "verificationChanges.json"));
				JsonWriter verificationWriter = new JsonWriter(fos, args);) {

			while (!queue.isEmpty() || !reader.isFinished()) {
				OchreExternalizable object = queue.poll(500, TimeUnit.MILLISECONDS);
				if (object != null) {
					ic++;
					try {
						if (object.getOchreObjectType() == OchreExternalizableObjectType.CONCEPT) {
							cc++;
							verificationWriter.write(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.SEMEME) {
							sc++;
							verificationWriter.write(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.STAMP_ALIAS) {
							sta++;
							verificationWriter.write(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.STAMP_COMMENT) {
							stc++;
							verificationWriter.write(object);
						} else {
							throw new UnsupportedOperationException("Unknown ochre object type: " + object);
						}
					} catch (Exception e) {
						log.error("Failure at " + ic + " items " + cc + " concepts, " + sc + " sememes, " + sta
								+ " stamp aliases, " + sc + " stamp comments, ", e);
						args = new HashMap<>();
						args.put(JsonWriter.PRETTY_PRINT, true);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						JsonWriter json = new JsonWriter(baos, args);

						UUID primordial = null;
						if (object instanceof ObjectChronology) {
							primordial = ((ObjectChronology<?>) object).getPrimordialUuid();
						}

						json.write(object);
						log.error("Failed on "
								+ (primordial == null ? ": "
										: "object with primoridial UUID " + primordial.toString() + ": ")
								+ baos.toString());
						json.close();

					}

					if (ic % 10000 == 0) {
						log.info("Read " + ic + " entries, " + "Loaded " + cc + " concepts, " + sc + " sememes, " + sta
								+ " stamp aliases, " + sc + " stamp comments, ");
					}
				}
			}

		} catch (Exception ex) {
			log.info("Loaded " + ic + " items, " + cc + " concepts, " + sc + " sememes, " + sta + " stamp aliases, "
					+ sc + " stamp comments, "
					+ (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));
		}

		log.info("Finished with " + ic + " items, " + cc + " concepts, " + sc + " sememes, " + sta + " stamp aliases, "
				+ sc + " stamp comments, "
				+ (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));

	}

	/**
	 * Generates human readable analysis files out of the contents found in the
	 * base and new inputed ibdf files to help debug process.
	 *
	 * @param contentMap
	 *            The ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param version
	 *            String representing the version of contents (either BASE or
	 *            NEW)
	 * @param jsonOutputFile
	 *            the output file, in json, representing the contentMap
	 * @throws IOException
	 *             Thrown if an exception occurred in writing the json or text
	 *             files.
	 */
	private void generateInputAnalysisFile(Map<OchreExternalizableObjectType, Set<OchreExternalizable>> contentMap,
			String version, String jsonOutputFile) throws IOException {

		int i = 1;
		try (FileWriter textWriter = new FileWriter(inputAnalysisDirStr.get() + textBaseNewIbdfFileName, true);
				JsonDataWriterService jsonWriter = new JsonDataWriterService(
						new File(inputAnalysisDirStr.get() + jsonOutputFile));) {

			textWriter.write("\n\n\n\n\n\n\t\t\t**** " + version + " LIST ****");
			jsonWriter.put("\n\n\n\n\n\n\t\t\t**** " + version + " LIST ****");
			for (OchreExternalizable component : contentMap.get(OchreExternalizableObjectType.CONCEPT)) {
				ConceptChronology<?> cc = (ConceptChronology<?>) component;
				jsonWriter.put("#---- " + version + " Concept #" + i + "   " + cc.getPrimordialUuid() + " ----");
				jsonWriter.put(cc);
				textWriter.write(
						"\n\n\n\t\t\t---- " + version + " Concept #" + i + "   " + cc.getPrimordialUuid() + " ----\n");
				textWriter.write(cc.toString());
				i++;
			}

			i = 1;
			for (OchreExternalizable component : contentMap.get(OchreExternalizableObjectType.SEMEME)) {
				SememeChronology<?> se = (SememeChronology<?>) component;
				jsonWriter.put("--- " + version + " Sememe #" + i + "   " + se.getPrimordialUuid() + " ----");
				jsonWriter.put(se);

				try {
					textWriter.write("\n\n\n\t\t\t---- " + version + " Sememe #" + i + "   " + se.getPrimordialUuid()
							+ " ----\n");
					textWriter.write(se.toString());
				} catch (Exception e) {
					textWriter.write("Failure on TXT writing " + se.getSememeType() + " which is index: " + i
							+ " in writing *" + version + "* content to text file for analysis (UUID: "
							+ se.getPrimordialUuid() + ".");
				}
				i++;
			}

			i = 1;
			for (OchreExternalizable component : contentMap.get(OchreExternalizableObjectType.STAMP_ALIAS)) {
				StampAlias sa = (StampAlias) component;
				jsonWriter.put("--- " + version + " Stamp Alias #" + i + "   " + sa.getStampSequence() + " ----");
				jsonWriter.put(sa);

				try {
					textWriter.write("\n\n\n\t\t\t---- " + version + " Stamp Alias #" + i + "   "
							+ sa.getStampSequence() + " ----\n");
					textWriter.write(sa.toString());
				} catch (Exception e) {
					textWriter.write("Failure on TXT writing Stamp Alias: " + sa.getStampSequence()
							+ " which is index: " + i + " in writing *" + version + ".");
				}
				i++;
			}

			i = 1;
			for (OchreExternalizable component : contentMap.get(OchreExternalizableObjectType.STAMP_COMMENT)) {
				StampComment sc = (StampComment) component;
				jsonWriter.put("--- " + version + " Stamp Comment #" + i + "   " + sc.getStampSequence() + " ----");
				jsonWriter.put(sc);

				try {
					textWriter.write("\n\n\n\t\t\t---- " + version + " Stamp Comment #" + i + "   "
							+ sc.getStampSequence() + " ----\n");
					textWriter.write(sc.toString());
				} catch (Exception e) {
					textWriter.write("Failure on TXT writing Stamp Comment: " + sc.getStampSequence()
							+ " which is index: " + i + " in writing *" + version + ".");
				}
				i++;
			}
		} catch (Exception e) {
			log.error("Failure on writing index: " + i + " in writing *" + version
					+ "* content to text file for analysis.");
		}
	}

	/**
	 * Delete the {@link #inputAnalysisDirStr} and the {@link #analysisArtifactsDirStr}
	 * directories. This is useful for when the databases are so large that
	 * rerunning the process with the "clean" maven directive adds considerable
	 * amount of time to unzip databases.
	 *
	 * @param dir
	 *            the directory to delete
	 */
	private void deleteDirectoryFiles(File dir) {
		if (dir.isDirectory() == false) {
			return;
		}

		File[] listFiles = dir.listFiles();
		for (File file : listFiles) {
			file.delete();
		}

		// now directory is empty, so we can delete it
		dir.delete();
	}

	@Override
	public void releaseLock() {
		diffProcessLock.unlock();		
	}
}
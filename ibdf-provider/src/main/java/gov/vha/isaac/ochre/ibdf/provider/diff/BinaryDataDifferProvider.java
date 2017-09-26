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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
import gov.vha.isaac.ochre.api.commit.StampService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderQueueService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderService;
import gov.vha.isaac.ochre.api.externalizable.DataWriterService;
import gov.vha.isaac.ochre.api.externalizable.InputIbdfVersionContent;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
import gov.vha.isaac.ochre.api.externalizable.StampAlias;
import gov.vha.isaac.ochre.api.externalizable.StampComment;
import gov.vha.isaac.ochre.api.externalizable.json.JsonDataWriterService;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;
import gov.vha.isaac.ochre.api.util.WorkExecutors;

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
public class BinaryDataDifferProvider implements BinaryDataDifferService {

	/** The log. */
	private final Logger log = LogManager.getLogger(BinaryDataDifferProvider.class);

	/** object to lock on */
	private final static ReentrantLock diffProcessLock = new ReentrantLock();

	/** The diff util. */
	private final BinaryDataDifferProviderUtility diffUtil = new BinaryDataDifferProviderUtility();

	/** The input analysis dir. */
	// Analysis File Readers/Writers
	private AtomicReference<String> inputAnalysisDirStr = new AtomicReference<String>();

	/** The comparison analysis dir. */
	private AtomicReference<String> analysisArtifactsDirStr = new AtomicReference<String>();

	/** The delta ibdf file path. */
	private AtomicReference<String> deltaIbdfFilePathStr = new AtomicReference<String>();

	/** The module uuid. */
	private AtomicReference<String> moduleUuidStr = new AtomicReference<String>();

	/** The text input file name. */
	private final String textCombinedIbdfFileName = "bothVersions.txt";

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
	public ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> computeDelta(
			InputIbdfVersionContent baseContent, InputIbdfVersionContent newContent) throws Exception {
		CopyOnWriteArrayList<OchreExternalizable> addedComponents = new CopyOnWriteArrayList<>();
		CopyOnWriteArrayList<OchreExternalizable> retiredComponents = new CopyOnWriteArrayList<>();
		CopyOnWriteArrayList<OchreExternalizable> changedComponents = new CopyOnWriteArrayList<>();

		final int activeStampSeq = createStamp(State.ACTIVE);
		final int inactiveStampSeq = createStamp(State.INACTIVE);
		Future<CopyOnWriteArrayList<OchreExternalizable>> futureRetiredComponents = null;

		// Find existing
		for (OchreExternalizableObjectType type : OchreExternalizableObjectType.values()) {

			if (type.equals(OchreExternalizableObjectType.CONCEPT)
					|| type.equals(OchreExternalizableObjectType.SEMEME)) {
				Set<UUID> matchedVersionedComponentSet = identifyCommonVersionedContent(type, baseContent, newContent,
						changedComponents, activeStampSeq);

				// Run Retired Check on background thread
				ExecutorService processRetiredComponentsService = LookupService.getService(WorkExecutors.class)
						.getIOExecutor();
				futureRetiredComponents = processRetiredComponentsService.submit(new ProcessRetiredComponents(type,
						baseContent, matchedVersionedComponentSet, inactiveStampSeq));

				// Run New check on current thread
				addedComponents
						.addAll(processNewComponents(type, newContent, matchedVersionedComponentSet, activeStampSeq));

				// Block until completed background thread
				retiredComponents.addAll(futureRetiredComponents.get());
				Get.commitService().postProcessImportNoChecks();
				log.info("Completed postProcessImportNoChecks for adds & inactivations");
				// Commit
			} else if (type.equals(OchreExternalizableObjectType.STAMP_ALIAS)
					|| type.equals(OchreExternalizableObjectType.STAMP_COMMENT)) {
				log.info("Starting processing of " + type);

				processStampedContent(type, baseContent.getStampMap(), newContent.getStampMap(), addedComponents);
				Get.commitService().postProcessImportNoChecks();

				log.info("Completed postProcessImportNoChecks for type: " + type);
			}
		}

		// Having identified the new, retired, and modified content of all
		// OchreExternalizable types, add them to return map
		ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> retMap = new ConcurrentHashMap<>();
		retMap.put(ChangeType.NEW_COMPONENTS, addedComponents);
		retMap.put(ChangeType.RETIRED_COMPONENTS, retiredComponents);
		retMap.put(ChangeType.MODIFIED_COMPONENTS, changedComponents);
		log.info("Completed computeDelta()");

		return retMap;
	}

	/**
	 * Identify changes between the BASE ibdf file and the NEW ibdf file for the
	 * types {@link #OchreExternalizableObjectType.CONCEPT} and
	 * {@link #OchreExternalizableObjectType.SEMEME}.
	 *
	 * @param type
	 *            the {@link OchreExternalizableObjectType} type
	 * @param baseContent
	 *            the BASE content in {@link InputIbdfVersionContent} form
	 * @param newContent
	 *            the NEW content in {@link InputIbdfVersionContent} form
	 * @param changedComponents
	 *            the differences between the BASE and NEW content of
	 *            {@link OchreExternalizableObjectType} type expressed as a list
	 *            of {@link OchreExternalizable} content
	 * @param activeStampSeq
	 *            the active stamp sequence used to make new versions
	 * @return a set of UUIDs that represent the content that existed in both
	 *         the baseContent and newContent.
	 */
	private Set<UUID> identifyCommonVersionedContent(OchreExternalizableObjectType type,
			InputIbdfVersionContent baseContent, InputIbdfVersionContent newContent,
			CopyOnWriteArrayList<OchreExternalizable> changedComponents, int activeStampSeq) {
		Set<UUID> matchedComponentSet = new HashSet<UUID>();
		log.info("Starting process to identify matches on " + type.toString());
		List<UUID> baseKeys = new ArrayList<UUID>(baseContent.getTypeToUuidMap().get(type));
		List<UUID> newKeys = new ArrayList<UUID>(newContent.getTypeToUuidMap().get(type));

		int i = 0;
		int j = 0;
		int numberOfBaseConcepts = baseKeys.size();
		log.info("Will be finding matches on " + numberOfBaseConcepts + " " + type.toString());
		int matches = 0;
		int diffs = 0;
		double percentage = .1;
		do {
			if (baseKeys.get(i).equals(newKeys.get(j))) {
				// Handle equals
				ObjectChronology<?> baseCompChron = (ObjectChronology<?>) baseContent.getUuidToComponentMap()
						.get(baseKeys.get(i));
				;
				ObjectChronology<?> newCompChron = (ObjectChronology<?>) newContent.getUuidToComponentMap()
						.get(newKeys.get(j));
				matchedComponentSet.add(baseCompChron.getPrimordialUuid());
				if (++matches % 10000 == 0) {
					log.info("Found " + matches + " matches");
				}

				try {
					OchreExternalizable modifiedComponent = diffUtil.diff(baseCompChron, newCompChron, activeStampSeq,
							type);

					if (modifiedComponent != null) {
						changedComponents.add(modifiedComponent);
						Get.commitService().importNoChecks(modifiedComponent);
						if (++diffs == 1 || diffs % 25 == 0) {
							log.info("Found " + diffs + " diffs");
						}
					}
				} catch (Exception e) {
					log.error("Failed On type: " + type + " on component: " + baseCompChron.getPrimordialUuid());
					e.printStackTrace();
				}

				i++;
				j++;
			} else if (baseKeys.get(i).compareTo(newKeys.get(j)) < 0) {
				i++;
			} else {
				j++;
			}

			// Log Status
			if (i == Math.round(numberOfBaseConcepts * percentage)) {
				log.info("Completed matching " + new DecimalFormat("#00").format(percentage * 100) + "% of "
						+ type.toString());
				percentage += .1;
			}
		} while (i < baseKeys.size() && j < newKeys.size());

		log.info("Completed 100% of processing finding " + matches + " matches and " + diffs + " diffs");
		Get.commitService().postProcessImportNoChecks();
		log.info("Completed postProcessImportNoChecks for matches");

		return matchedComponentSet;
	}

	/**
	 * Compares the BASE content and matchedComponentSet of type
	 * {@link OchreExternalizableObjectType}. The operation will retire those
	 * {@link OchreExternalizable} components found in the BASE but not the
	 * matchedComponentSet.
	 * 
	 * To retire the components, a new inactive version is made. Those retired
	 * versions are then returned.
	 *
	 * @param type
	 *            the type the {@link OchreExternalizableObjectType} type
	 * @param baseContent
	 *            the BASE content in {@link InputIbdfVersionContent} form
	 * @param matchedComponentSet
	 *            a set of UUIDs that represent the content that existed in both
	 *            the baseContent and newContent
	 * @param inactiveStampSeq
	 *            the inactive stamp sequence used to make new versions
	 * @return the {@link OchreExternalizableObjectType} content retired between
	 *         the BASE and NEW versions expressed as a list of
	 *         {@link OchreExternalizable}
	 */
	private CopyOnWriteArrayList<OchreExternalizable> processRetiredComponents(OchreExternalizableObjectType type,
			InputIbdfVersionContent baseContent, Set<UUID> matchedComponentSet, int inactiveStampSeq) {
		int counter = 0;
		int retires = 0;
		double percentage = .1;
		CopyOnWriteArrayList<OchreExternalizable> retiredComponents = new CopyOnWriteArrayList<>();
		int numberOfComponents = baseContent.getTypeToUuidMap().get(type).size();

		log.info("Will be finding retired on " + numberOfComponents + " " + type.toString());

		// Add baseComps not in matchedSet --> Blocking Thread 1
		for (UUID uuidKey : baseContent.getTypeToUuidMap().get(type)) {
			OchreExternalizable baseComp = baseContent.getUuidToComponentMap().get(uuidKey);
			if (counter++ == Math.round(numberOfComponents * percentage)) {
				log.info("Completed inactivating " + new DecimalFormat("#00").format(percentage * 100) + "% of "
						+ type.toString());
				percentage += .1;
			}

			if (!matchedComponentSet.contains(((ObjectChronology<?>) baseComp).getPrimordialUuid())) {
				OchreExternalizable retiredComp = diffUtil.addNewInactiveVersion(baseComp,
						baseComp.getOchreObjectType(), inactiveStampSeq);

				if (retiredComp != null) {
					retiredComponents.add(retiredComp);
					Get.commitService().importNoChecks(retiredComp);
					if (++retires % 1000 == 0) {
						log.info("Found " + retires + " retires");
					}
				}
			}
		}

		log.info("Completed 100% inactivations with " + retires + " retires");

		return retiredComponents;
	}

	/**
	 * Compares the NEW content and matchedComponentSet of type
	 * {@link OchreExternalizableObjectType}. The operation will add a new
	 * Chronicle for those {@link OchreExternalizable} components found in the
	 * NEW but not the matchedComponentSet.
	 * 
	 * Those new versions are then returned.
	 *
	 * @param type
	 *            the type the {@link OchreExternalizableObjectType} type
	 * @param newContent
	 *            the NEW content in {@link InputIbdfVersionContent} form
	 * @param matchedComponentSet
	 *            a set of UUIDs that represent the content that existed in both
	 *            the baseContent and newContent
	 * @param activeStampSeq
	 *            the active stamp sequence used to make new versions
	 * @return the {@link OchreExternalizableObjectType} content added between
	 *         the BASE and NEW versions expressed as a list of
	 *         {@link OchreExternalizable}
	 */
	private CopyOnWriteArrayList<OchreExternalizable> processNewComponents(OchreExternalizableObjectType type,
			InputIbdfVersionContent newContent, Set<UUID> matchedComponentSet, int activeStampSeq) {
		int counter = 0;
		int adds = 0;
		double percentage = .1;
		CopyOnWriteArrayList<OchreExternalizable> addedComponents = new CopyOnWriteArrayList<>();
		int numberOfComponents = newContent.getTypeToUuidMap().get(type).size();
		log.info("Will be finding new on " + numberOfComponents + " " + type.toString());

		for (UUID uuidKey : newContent.getTypeToUuidMap().get(type)) {
			OchreExternalizable newComp = newContent.getUuidToComponentMap().get(uuidKey);
			if (counter++ == Math.round(numberOfComponents * percentage)) {
				log.info("Completed adding " + new DecimalFormat("#00").format(percentage * 100) + "% of "
						+ type.toString());
				percentage += .1;
			}
			if (!matchedComponentSet.contains(((ObjectChronology<?>) newComp).getPrimordialUuid())) {
				OchreExternalizable addedComp = diffUtil.diff(null, (ObjectChronology<?>) newComp, activeStampSeq,
						type);

				if (addedComp != null) {
					addedComponents.add(addedComp);
					Get.commitService().importNoChecks(addedComp);

					if (++adds % 20000 == 0) {
						log.info("Found " + adds + " adds");
					}
				}
			}
		}

		log.info("Completed 100% of adds finding " + adds + " additions");

		return addedComponents;
	}

	/**
	 * Identify new stamp-=based content added between the BASE ibdf file and
	 * the NEW ibdf file for the types
	 * {@link #OchreExternalizableObjectType.STAMP_ALIAS} and
	 * {@link #OchreExternalizableObjectType.STAMP_COMMENT}.
	 *
	 * @param type
	 *            the {@link OchreExternalizableObjectType} type
	 * @param baseContent
	 *            the BASE ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param newContent
	 *            the NEW ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param addedComponents
	 *            all {@link OchreExternalizable} determined to be new in the
	 *            new content map
	 */
	private void processStampedContent(OchreExternalizableObjectType type,
			ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> baseContent,
			ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> newContent,
			CopyOnWriteArrayList<OchreExternalizable> addedComponents) {
		Set<Integer> matchedStampSequenceSet = new HashSet<Integer>();

		// Search for modified stamp content
		for (OchreExternalizable baseComp : baseContent.get(type)) {
			boolean matchFound = false;
			for (OchreExternalizable newComp : newContent.get(type)) {
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
		for (OchreExternalizable newComp : newContent.get(type)) {
			int newSequence;
			if (type.equals(OchreExternalizableObjectType.STAMP_ALIAS)) {
				newSequence = ((StampAlias) newComp).getStampSequence();
			} else {
				newSequence = ((StampComment) newComp).getStampSequence();
			}

			if (!matchedStampSequenceSet.contains(newSequence)) {
				addedComponents.add(newComp);
				Get.commitService().importNoChecks(newComp);
			}
		}
	}

	@Override
	public Boolean createDeltaIbdfFile(
			ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents) {
		Map<String, Object> args = new HashMap<>();
		args.put(JsonWriter.PRETTY_PRINT, true);

		try {
			// the IBDF file being generated
			DataWriterService componentCSWriter = Get.binaryDataWriter(new File(deltaIbdfFilePathStr.get()).toPath());

			for (ChangeType key : changedComponents.keySet()) {
				int i = 1;
				log.info("About to write " + changedComponents.get(key).size() + " number of changes for "
						+ key.toString());

				for (OchreExternalizable c : changedComponents.get(key)) {
					componentCSWriter.put(c);
					if (i % 5000 == 0) {
						log.info("Completed " + i + " writes");
						componentCSWriter.flush();
					}
				}
				log.info("Completed writing all changes for " + key.toString());
			}
			log.info("Completed writing all changes for all types");

			componentCSWriter.close();
			log.info("Closed writer");
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public Boolean generateAnalysisFileFromDiffObject(
			ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents)
			throws IOException {
		if (changedComponents != null) {
			try (FileWriter allChangesTextWriter = new FileWriter(
					analysisArtifactsDirStr.get() + textFullComparisonFileName);
					JsonDataWriterService allChangesJsonWriter = new JsonDataWriterService(
							new File(analysisArtifactsDirStr.get() + jsonFullComparisonFileName));) {
				for (ChangeType key : changedComponents.keySet()) {
					log.info("About to write " + changedComponents.size()
							+ " number of changes for comparison analysis files (txt & json) on type "
							+ key.toString());
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

							if (counter % 5000 == 0) {
								log.info("Completed " + counter + " writes for json/txt");
								allChangesJsonWriter.flush();
								allChangesTextWriter.flush();
								changeTypeWriter.flush();
							}

							try {
								changeTypeWriter.write(c.toString() + "\n\n\n");
							} catch (Exception e) {
								// This process will be trying to read content
								// not yet imported into database. So not
								// actually an error.
							}

							try {
								allChangesTextWriter.write(c.toString());
							} catch (Exception e) {
								// This process will be trying to read content
								// not yet imported into database. So not
								// actually an error.
							}
						}

						log.info("Completed writing json/txt changes for " + key.toString());

					} catch (IOException e) {
						log.error(
								"Failed in creating analysis files (not in processing the content written to the analysis files)");
						return false;
					} finally {
						changeTypeWriter.close();
						log.info("Closed writers");
					}
				}
			}
		} else {
			log.info("changedComponents empty so not writing json/text Output files");
		}

		return true;
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
				LookupService.getService(IdentifierService.class)
						.getConceptSequenceForUuids(UUID.fromString(moduleUuidStr.get())), // Module
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
	public InputIbdfVersionContent transformInputIbdfFile(File versionFile, IbdfInputVersion verion) throws Exception {

		log.info("Processing " + verion + " file: " + versionFile.getAbsolutePath());
		BinaryDataReaderService reader = Get.binaryDataReader(versionFile.toPath());
		/** The comment count. */
		AtomicInteger conceptCount = new AtomicInteger(0);
		AtomicInteger sememeCount = new AtomicInteger(0);
		AtomicInteger aliasCount = new AtomicInteger(0);
		AtomicInteger commentCount = new AtomicInteger(0);
		AtomicInteger itemCount = new AtomicInteger(0);

		InputIbdfVersionContent content = new InputIbdfVersionContent();

		try {
			reader.getStream().forEach((object) -> {
				if (object != null) {
					itemCount.getAndIncrement();

					try {
						if (object.getOchreObjectType() == OchreExternalizableObjectType.CONCEPT) {
							conceptCount.getAndIncrement();
							content.getTypeToUuidMap().get(OchreExternalizableObjectType.CONCEPT)
									.add(((ConceptChronology<?>) object).getPrimordialUuid());
							content.getUuidToComponentMap().put(((ConceptChronology<?>) object).getPrimordialUuid(),
									object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.SEMEME) {
							sememeCount.getAndIncrement();
							content.getTypeToUuidMap().get(OchreExternalizableObjectType.SEMEME)
									.add(((SememeChronology<?>) object).getPrimordialUuid());
							content.getUuidToComponentMap().put(((SememeChronology<?>) object).getPrimordialUuid(),
									object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.STAMP_ALIAS) {
							aliasCount.getAndIncrement();
							content.getStampMap().get(OchreExternalizableObjectType.STAMP_ALIAS).add(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.STAMP_COMMENT) {
							commentCount.getAndIncrement();
							content.getStampMap().get(OchreExternalizableObjectType.STAMP_COMMENT).add(object);
						} else {
							throw new UnsupportedOperationException("Unknown ochre object type: " + object);
						}
					} catch (Exception e) {
						log.error("Failure on " + verion + " at " + conceptCount + " concepts, " + sememeCount
								+ " sememes, ", e);
						Map<String, Object> args = new HashMap<>();
						args.put(JsonWriter.PRETTY_PRINT, true);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						JsonWriter json = new JsonWriter(baos, args);

						UUID primordial = null;
						if (object instanceof ObjectChronology) {
							primordial = ((ObjectChronology<?>) object).getPrimordialUuid();
						}

						json.write(object);
						log.error("Failed " + verion + " on "
								+ (primordial == null ? ": "
										: "object with primoridial UUID " + primordial.toString() + ": ")
								+ baos.toString());
						json.close();

					}

					if (itemCount.get() % 250000 == 0) {
						log.info("Still processing " + verion + " ibdf file.  Status: " + itemCount.get() + " entries, "
								+ "Loaded " + conceptCount.get() + " concepts, " + sememeCount.get() + " sememes, "
								+ aliasCount.get() + " aliases, " + commentCount.get() + " comments");
					}
				}
			});
		} catch (Exception ex) {
			log.info("Exception during " + verion + " load: Loaded " + conceptCount.get() + " concepts, "
					+ sememeCount.get() + " sememes, " + aliasCount.get() + " aliases, " + commentCount.get()
					+ " comments" + (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));
			throw new Exception(ex.getLocalizedMessage(), ex);
		}

		log.info("Finished processing " + verion + " ibdf file.  Results: " + itemCount.get() + " entries, " + "Loaded "
				+ conceptCount.get() + " concepts, " + sememeCount.get() + " sememes, " + aliasCount.get()
				+ " aliases, " + commentCount.get() + " comments");

		return content;
	}

	/**
	 * Generates human readable analysis json file containing the contents in
	 * the ibdf delta file located at {@link #deltaIbdfFilePathStr}.
	 *
	 * @throws FileNotFoundException
	 *             Thrown if the delta file is not found at location specified
	 *             by {@link #deltaIbdfFilePathStr}
	 */
	@Override
	public void writeChangeSetForVerification() throws FileNotFoundException {
		int ic = 0;
		int cc = 0;
		int sc = 0;
		int sta = 0;
		int stc = 0;

		BinaryDataReaderQueueService reader = Get.binaryDataQueueReader(new File(deltaIbdfFilePathStr.get()).toPath());
		BlockingQueue<OchreExternalizable> queue = reader.getQueue();

		Map<String, Object> args = new HashMap<>();
		args.put(JsonWriter.PRETTY_PRINT, true);

		try (FileOutputStream fos = new FileOutputStream(
				new File(analysisArtifactsDirStr.get() + "verificationChanges.json"));
				JsonWriter verificationWriter = new JsonWriter(fos, args);) {

			while (!queue.isEmpty() || !reader.isFinished()) {
				OchreExternalizable object = queue.poll(500, TimeUnit.MILLISECONDS);
				if (object != null) {
					ic++;
					if (ic % 10000 == 0) {
						log.info("Completed writing " + ic + " components FROM diff ibdf");
						verificationWriter.flush();
					}
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

					if (ic % 250000 == 0) {
						log.info("Read " + ic + " entries, " + "Loaded " + cc + " concepts, " + sc + " sememes, " + sta
								+ " stamp aliases, " + stc + " stamp comments, ");
					}
				}
				log.info("Completed writing all components FROM diff ibdf successfully");

			}

		} catch (Exception ex) {
			log.info("Loaded " + ic + " items, " + cc + " concepts, " + sc + " sememes, " + sta + " stamp aliases, "
					+ stc + " stamp comments, "
					+ (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));
		}

		log.info("Finished with " + ic + " items, " + cc + " concepts, " + sc + " sememes, " + sta + " stamp aliases, "
				+ stc + " stamp comments, "
				+ (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));

		log.info("Closed analysis file written FROM diff ibdf successfully");

	}

	@Override
	public Boolean generateInputAnalysisFile(InputIbdfVersionContent content, IbdfInputVersion version)
			throws IOException {
		String fileName = version.toString().toLowerCase() + "Version.json";
		if (content != null) {
			int i = 1;
			try (FileWriter textWriter = new FileWriter(inputAnalysisDirStr.get() + textCombinedIbdfFileName, true);
					JsonDataWriterService jsonWriter = new JsonDataWriterService(
							new File(inputAnalysisDirStr.get() + fileName));) {

				textWriter.write("\n\n\n\n\n\n\t\t\t**** " + version + " LIST ****");
				jsonWriter.put("\n\n\n\n\n\n\t\t\t**** " + version + " LIST ****");
				Set<UUID> conceptUuids = content.getTypeToUuidMap().get(OchreExternalizableObjectType.CONCEPT);
				int numberOfConcepts = content.getTypeToUuidMap().get(OchreExternalizableObjectType.CONCEPT).size();

				for (UUID uuidKey : conceptUuids) {
					ConceptChronology<?> cc = (ConceptChronology<?>) content.getUuidToComponentMap().get(uuidKey);
					jsonWriter.put("#---- " + version + " Concept #" + i + "   " + cc.getPrimordialUuid() + " ----");
					jsonWriter.put(cc);
					textWriter.write("\n\n\n\t\t\t---- " + version + " Concept #" + i + "   " + cc.getPrimordialUuid()
							+ " ----\n");
					textWriter.write(cc.toString());
					if (i++ % 25000 == 0) {
						log.info("Just completed writing " + i + " concept writes (out of " + (numberOfConcepts - 1)
								+ " total concepts)");
						textWriter.flush();
						jsonWriter.flush();
					}
				}

				i = 1;
				Set<UUID> sememeUuids = content.getTypeToUuidMap().get(OchreExternalizableObjectType.SEMEME);
				int numberOfSememes = content.getTypeToUuidMap().get(OchreExternalizableObjectType.SEMEME).size();
				for (UUID uuidKey : sememeUuids) {
					SememeChronology<?> se = (SememeChronology<?>) content.getUuidToComponentMap().get(uuidKey);
					jsonWriter.put("--- " + version + " Sememe #" + i + "   " + se.getPrimordialUuid() + " ----");
					jsonWriter.put(se);

					try {
						textWriter.write("\n\n\n\t\t\t---- " + version + " Sememe #" + i + "   "
								+ se.getPrimordialUuid() + " ----\n");
						textWriter.write(se.toString());
					} catch (Exception e) {
						textWriter.write("Failure on TXT writing " + se.getSememeType() + " which is index: " + i
								+ " in writing *" + version + "* content to text file for analysis (UUID: "
								+ se.getPrimordialUuid() + ".");
					}

					if (i++ % 250000 == 0) {
						log.info("Just completed writing " + i + " sememe writes (out of " + (numberOfSememes - 1)
								+ " total sememes)");
						textWriter.flush();
						jsonWriter.flush();
					}
				}

				i = 1;
				Set<OchreExternalizable> aliasMap = content.getStampMap()
						.get(OchreExternalizableObjectType.STAMP_ALIAS);
				for (OchreExternalizable component : aliasMap) {
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
				Set<OchreExternalizable> commentMap = content.getStampMap()
						.get(OchreExternalizableObjectType.STAMP_COMMENT);
				for (OchreExternalizable component : commentMap) {
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
				log.info("Completed analyzing " + fileName + " input file");
			} catch (Exception e) {
				log.error("Failure on writing index: " + i + " in writing *" + version
						+ "* content to text file for analysis.");
				return false;
			}
		} else {
			log.info(version + "ContentMap empty so not writing json/text Input files for " + version + " content");
		}

		return true;
	}

	/**
	 * Delete the {@link #inputAnalysisDirStr} and the
	 * {@link #analysisArtifactsDirStr} directories. This is useful for when the
	 * databases are so large that rerunning the process with the "clean" maven
	 * directive adds considerable amount of time to unzip databases.
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

	private class ProcessRetiredComponents implements Callable<CopyOnWriteArrayList<OchreExternalizable>> {

		private OchreExternalizableObjectType type_;
		private InputIbdfVersionContent baseContent_;
		private Set<UUID> matchedComponentSet_;
		private int inactiveStampSeq_;

		public ProcessRetiredComponents(OchreExternalizableObjectType type, InputIbdfVersionContent baseContent,
				Set<UUID> matchedComponentSet, int inactiveStampSeq) {
			type_ = type;
			baseContent_ = baseContent;
			matchedComponentSet_ = matchedComponentSet;
			inactiveStampSeq_ = inactiveStampSeq;
		}

		@Override
		public CopyOnWriteArrayList<OchreExternalizable> call() {
			return processRetiredComponents(type_, baseContent_, matchedComponentSet_, inactiveStampSeq_);
		}

	}
}
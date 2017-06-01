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
import java.util.concurrent.TimeUnit;
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
import gov.vha.isaac.ochre.api.externalizable.json.JsonDataWriterService;

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
	private final Logger log = LogManager.getLogger();
	private BinaryDataDifferProviderUtility diffUtil;

	// Stream hack
	private int conceptCount, sememeCount, itemCount, aliasCount, commentCount;

	// Analysis File Readers/Writers
	private String inputAnalysisDir;
	private String comparisonAnalysisDir;
	private String deltaIbdfPath;

	private final String textInputFileName = "bothVersions.txt";
	private final String jsonFullComparisonFileName = "allChangedComponents.json";
	private final String textFullComparisonFileName = "allChangedComponents.txt";

	// Changeset File Writer
	private DataWriterService componentCSWriter = null;

	HashSet<Integer> skippedItems = new HashSet<>();

	public BinaryDataDifferProvider() {
		// For HK2
		log.info("binary data differ constructed");
	}

	@PostConstruct
	private void startMe() {
		log.info("Starting BinaryDataDifferProvider.");
	}

	@PreDestroy
	private void stopMe() {
		log.info("Stopping BinaryDataDifferProvider.");
	}

	@Override
	public Map<ChangeType, List<OchreExternalizable>> computeDelta(
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap) {
		List<OchreExternalizable> addedComponents = new ArrayList<>();
		List<OchreExternalizable> retiredComponents = new ArrayList<>();
		List<OchreExternalizable> changedComponents = new ArrayList<>();
		CommitService commitService = Get.commitService();

		final int activeStampSeq = createStamp(State.ACTIVE);
		final int inactiveStampSeq = createStamp(State.INACTIVE);

		// Find existing
		for (OchreExternalizableObjectType type : OchreExternalizableObjectType.values()) {
			Set<UUID> matchedSet = new HashSet<UUID>();
			if (type != OchreExternalizableObjectType.CONCEPT && type != OchreExternalizableObjectType.SEMEME) {
				// Given using the OchreExternalizableObjectType.values()
				// collection, ensure only handling the supported types
				continue;
			}

			// Search for modified components
			for (OchreExternalizable oldComp : oldContentMap.get(type)) {
				for (OchreExternalizable newComp : newContentMap.get(type)) {
					ObjectChronology<?> oldCompChron = (ObjectChronology<?>) oldComp;
					ObjectChronology<?> newCompChron = (ObjectChronology<?>) newComp;

					if (oldCompChron.getPrimordialUuid().equals(newCompChron.getPrimordialUuid())) {
						matchedSet.add(oldCompChron.getPrimordialUuid());

						try {
							OchreExternalizable modifiedComponents = diffUtil.diff(oldCompChron, newCompChron,
									activeStampSeq, type);
							if (modifiedComponents != null) {
								if (type == OchreExternalizableObjectType.CONCEPT) {
									throw new Exception("Cannot modify Concept in current Object Model");
								}

								changedComponents.add(modifiedComponents);
							}
						} catch (Exception e) {
							log.error("Failed ON type: " + type + " on component: " + oldCompChron.getPrimordialUuid());
							e.printStackTrace();
						}

						continue;
					}
				}
			}

			// Add newCons not in newList
			for (OchreExternalizable oldComp : oldContentMap.get(type)) {
				if (!matchedSet.contains(((ObjectChronology<?>) oldComp).getPrimordialUuid())) {
					OchreExternalizable retiredComp = diffUtil.addNewInactiveVersion(oldComp,
							oldComp.getOchreObjectType(), inactiveStampSeq);

					if (retiredComp != null) {
						retiredComponents.add(retiredComp);
					}
				}
			}

			// Add newCons not in newList
			for (OchreExternalizable newComp : newContentMap.get(type)) {
				if (!matchedSet.contains(((ObjectChronology<?>) newComp).getPrimordialUuid())) {

					OchreExternalizable addedComp = diffUtil.diff(null, (ObjectChronology<?>) newComp, activeStampSeq,
							type);

					if (addedComp != null) {
						addedComponents.add(addedComp);
						commitService.importNoChecks(addedComp);
					}
				}
			}
			commitService.postProcessImportNoChecks();

		} // Close Type Loop

		Map<ChangeType, List<OchreExternalizable>> retMap = new HashMap<>();
		retMap.put(ChangeType.NEW_COMPONENTS, addedComponents);
		retMap.put(ChangeType.RETIRED_COMPONENTS, retiredComponents);
		retMap.put(ChangeType.MODIFIED_COMPONENTS, changedComponents);

		return retMap;
	}

	@Override
	public void generateDeltaIbdfFile(Map<ChangeType, List<OchreExternalizable>> changedComponents) throws IOException {
		componentCSWriter = Get.binaryDataWriter(new File(deltaIbdfPath).toPath());

		for (ChangeType key : changedComponents.keySet()) {
			for (OchreExternalizable c : changedComponents.get(key)) {
				componentCSWriter.put(c);
			}
		}

		componentCSWriter.close();
	}

	@Override
	public void createAnalysisFiles(Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap,
			Map<ChangeType, List<OchreExternalizable>> changedComponents) {
		try {
			// Handle Input Files
			if (oldContentMap != null) {
				generateInputAnalysisFile(oldContentMap, "OLD", "oldVersion.json");
			} else {
				log.info("oldContentMap empty so not writing json/text Input files for old content");
			}

			if (newContentMap != null) {
				generateInputAnalysisFile(newContentMap, "New", "newVersion.json");
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

	private void generateComparisonAnalysisFile(Map<ChangeType, List<OchreExternalizable>> changedComponents)
			throws IOException {
		try (FileWriter textComparisonWriter = new FileWriter(comparisonAnalysisDir + textFullComparisonFileName);
				JsonDataWriterService comparisonAnalysisJsonWriter = new JsonDataWriterService(
						new File(comparisonAnalysisDir + jsonFullComparisonFileName));) {

			for (ChangeType key : changedComponents.keySet()) {
				int counter = 1;

				FileWriter ComparisonAnalysisTextWriter = new FileWriter(comparisonAnalysisDir + key + "_File.txt");

				try {
					List<OchreExternalizable> components = changedComponents.get(key);

					comparisonAnalysisJsonWriter.put("\n\n\n\t\t\t**** " + key.toString() + " ****");
					textComparisonWriter.write("\n\n\n\t\t\t**** " + key.toString() + " ****");

					for (OchreExternalizable c : components) {
						String componentType;
						if (c.getOchreObjectType() == OchreExternalizableObjectType.CONCEPT) {
							componentType = "Concept";
						} else {
							componentType = "Sememe";
						}

						String componentToWrite = "---- " + key.toString() + " " + componentType + " #" + counter++
								+ "   " + ((ObjectChronology<?>) c).getPrimordialUuid() + " ----\n";

						comparisonAnalysisJsonWriter.put(componentToWrite);

						textComparisonWriter.write("\n\n\n\t\t\t" + componentToWrite);
						comparisonAnalysisJsonWriter.put(c);
						try {
							ComparisonAnalysisTextWriter.write(c.toString());
							ComparisonAnalysisTextWriter.write("\n\n\n");
							String s = c.toString();
							textComparisonWriter.write(s);
						} catch (Exception e) {
							log.debug("Failure writing toString for: " + ((ObjectChronology<?>) c).getPrimordialUuid());
						}
					}
				} catch (IOException e) {
					log.error("Failure processing changes of type " + key.toString());
				} finally {
					ComparisonAnalysisTextWriter.close();
				}
			}
		}

	}

	/**
	 * Set up all the boilerplate stuff.
	 * 
	 * Create a stamp in current database... create seq... then when
	 * serializing, point it
	 * 
	 * @param state
	 *            - state or null (for current)
	 * @param time
	 *            - time or null (for default)
	 */
	private int createStamp(State state) {

		return LookupService.getService(StampService.class).getStampSequence(state, diffUtil.getNewImportDate(),
				LookupService.getService(IdentifierService.class)
						.getConceptSequenceForUuids(UUID.fromString("f7495b58-6630-3499-a44e-2052b5fcf06c")), // USER
				LookupService.getService(IdentifierService.class)
						.getConceptSequenceForUuids(UUID.fromString("8aa5fda8-33e9-5eaf-88e8-dd8a024d2489")), // VHA
																												// MODULE
				LookupService.getService(IdentifierService.class)
						.getConceptSequenceForUuids(UUID.fromString("1f200ca6-960e-11e5-8994-feff819cdc9f"))); // DEV
																												// PATH
	}

	@Override
	public void initialize(String comparisonAnalysisDir, String inputAnalysisDir, String deltaIbdfPathFile,
			Boolean generateAnalysisFiles, boolean diffOnStatus, boolean diffOnTimestamp, boolean diffOnAuthor,
			boolean diffOnModule, boolean diffOnPath, String importDate) {
		diffUtil = new BinaryDataDifferProviderUtility(diffOnStatus, diffOnTimestamp, diffOnAuthor, diffOnModule,
				diffOnPath);
		diffUtil.setNewImportDate(importDate);

		this.inputAnalysisDir = inputAnalysisDir;
		this.comparisonAnalysisDir = comparisonAnalysisDir;
		this.deltaIbdfPath = deltaIbdfPathFile;
		this.inputAnalysisDir = inputAnalysisDir;

		if (generateAnalysisFiles) {
			File f = new File(inputAnalysisDir);
			f.mkdirs();
		}

		File f = new File(comparisonAnalysisDir);
		f.mkdirs();
	}

	@Override
	public Map<OchreExternalizableObjectType, Set<OchreExternalizable>> processInputIbdfFil(File versionFile)
			throws Exception {
		log.info("Processing file: " + versionFile.getAbsolutePath());
		BinaryDataReaderService reader = Get.binaryDataReader(versionFile.toPath());

		itemCount = 0;
		conceptCount = 0;
		sememeCount = 0;
		aliasCount = 0;
		commentCount = 0;

		Map<OchreExternalizableObjectType, Set<OchreExternalizable>> retMap = new HashMap<>();
		retMap.put(OchreExternalizableObjectType.CONCEPT, new HashSet<OchreExternalizable>());
		retMap.put(OchreExternalizableObjectType.SEMEME, new HashSet<OchreExternalizable>());
		retMap.put(OchreExternalizableObjectType.STAMP_ALIAS, new HashSet<OchreExternalizable>());
		retMap.put(OchreExternalizableObjectType.STAMP_COMMENT, new HashSet<OchreExternalizable>());
		try {
			reader.getStream().forEach((object) -> {
				if (object != null) {
					itemCount++;

					try {
						if (object.getOchreObjectType() == OchreExternalizableObjectType.CONCEPT) {
							conceptCount++;
							retMap.get(object.getOchreObjectType()).add(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.SEMEME) {
							sememeCount++;
							retMap.get(object.getOchreObjectType()).add(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.STAMP_ALIAS) {
							aliasCount++;
							retMap.get(object.getOchreObjectType()).add(object);
						} else if (object.getOchreObjectType() == OchreExternalizableObjectType.STAMP_COMMENT) {
							commentCount++;
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

					if (itemCount % 250 == 0) {
						log.info("Still processing ibdf file.  Status: " + itemCount + " entries, " + "Loaded "
								+ conceptCount + " concepts, " + sememeCount + " sememes, " + aliasCount + " aliases, "
								+ commentCount + " comments");
					}
				}
			});
		} catch (Exception ex) {
			log.info("Loaded " + conceptCount + " concepts, " + sememeCount + " sememes, " + aliasCount + " aliases, "
					+ commentCount + " comments"
					+ (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));
			throw new Exception(ex.getLocalizedMessage(), ex);
		}

		log.info("Finished processing ibdf file.  Results: " + itemCount + " entries, " + "Loaded " + conceptCount
				+ " concepts, " + sememeCount + " sememes, " + aliasCount + " aliases, " + commentCount + " comments");

		return retMap;
	}

	private void writeChangeSetForVerification() throws FileNotFoundException {
		int ic = 0;
		int cc = 0;
		int sc = 0;

		BinaryDataReaderQueueService reader = Get.binaryDataQueueReader(new File(deltaIbdfPath).toPath());
		BlockingQueue<OchreExternalizable> queue = reader.getQueue();

		Map<String, Object> args = new HashMap<>();
		args.put(JsonWriter.PRETTY_PRINT, true);

		try (FileOutputStream fos = new FileOutputStream(new File(comparisonAnalysisDir + "verificationChanges.json"));
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
						} else {
							throw new UnsupportedOperationException("Unknown ochre object type: " + object);
						}
					} catch (Exception e) {
						log.error("Failure at " + ic + " items " + cc + " concepts, " + sc + " sememes, ", e);
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

					if (ic % 250 == 0) {
						log.info("Read " + ic + " entries, " + "Loaded " + cc + " concepts, " + sc + " sememes, ");
					}
				}
			}

		} catch (Exception ex) {
			log.info("Loaded " + ic + " items, " + cc + " concepts, " + sc + " sememes, "
					+ (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));
		}

		log.info("Finished with " + ic + " items, " + cc + " concepts, " + sc + " sememes, "
				+ (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));

	}

	private void generateInputAnalysisFile(Map<OchreExternalizableObjectType, Set<OchreExternalizable>> contentMap,
			String version, String jsonInputFileName) throws IOException {

		int i = 1;

		try (FileWriter inputAnalysisTextWriter = new FileWriter(inputAnalysisDir + textInputFileName, true);
				JsonDataWriterService inputAnalysisJsonWriter = new JsonDataWriterService(
						new File(inputAnalysisDir + jsonInputFileName));) {

			inputAnalysisTextWriter.write("\n\n\n\n\n\n\t\t\t**** " + version + " LIST ****");
			inputAnalysisJsonWriter.put("\n\n\n\n\n\n\t\t\t**** " + version + " LIST ****");
			for (OchreExternalizable component : contentMap.get(OchreExternalizableObjectType.CONCEPT)) {
				ConceptChronology<?> cc = (ConceptChronology<?>) component;
				inputAnalysisJsonWriter
						.put("#---- " + version + " Concept #" + i + "   " + cc.getPrimordialUuid() + " ----");
				inputAnalysisJsonWriter.put(cc);
				inputAnalysisTextWriter.write(
						"\n\n\n\t\t\t---- " + version + " Concept #" + i + "   " + cc.getPrimordialUuid() + " ----\n");
				inputAnalysisTextWriter.write(cc.toString());
				i++;
			}

			i = 1;

			for (OchreExternalizable component : contentMap.get(OchreExternalizableObjectType.SEMEME)) {
				SememeChronology<?> se = (SememeChronology<?>) component;
				inputAnalysisJsonWriter
						.put("--- " + version + " Sememe #" + i + "   " + se.getPrimordialUuid() + " ----");
				inputAnalysisJsonWriter.put(se);

				try {
					inputAnalysisTextWriter.write("\n\n\n\t\t\t---- " + version + " Sememe #" + i + "   "
							+ se.getPrimordialUuid() + " ----\n");
					inputAnalysisTextWriter.write(se.toString());
				} catch (Exception e) {
					log.error("Failure on TXT writing " + se.getSememeType() + " which is index: " + i + " in writing *"
							+ version + "* content to text file for analysis (UUID: " + se.getPrimordialUuid() + ".");
					inputAnalysisTextWriter.write("Failure on TXT writing " + se.getSememeType() + " which is index: "
							+ i + " in writing *" + version + "* content to text file for analysis (UUID: "
							+ se.getPrimordialUuid() + ".");
				}
				i++;
			}
		} catch (Exception e) {
			log.error("Failure on writing index: " + i + " in writing *" + version
					+ "* content to text file for analysis.");
		}
	}
}
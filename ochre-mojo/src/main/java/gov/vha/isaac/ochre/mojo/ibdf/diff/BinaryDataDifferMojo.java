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
package gov.vha.isaac.ochre.mojo.ibdf.diff;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService.ChangeType;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.mojo.external.QuasiMojo;

/**
 * Examines two ibdf files containing two distinct versions of the same
 * terminology and identifies the new/inactivated/modified content between the
 * two versions.
 * 
 * Once identified, a new changeset file is generated containing these changes.
 * This file can then be imported into an existing database contining the old
 * version of the terminology. This will upgrade it to the new terminology.
 * 
 * {@link QuasiMojo}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Service(name = "diff-ibdfs")
public class BinaryDataDifferMojo extends QuasiMojo {
	/**
	 * {@code ibdf format} files to import.
	 */
	@Parameter(required = true)
	private File baseVersionFile;

	/**
	 * {@code ibdf format} files to import.
	 */
	@Parameter(required = true)
	private File newVersionFile;

	/**
	 * {@code ibdf format} files to import.
	 */
	@Parameter(required = true)
	private String inputAnalysisDir;

	/**
	 * {@code ibdf format} files to import.
	 */
	@Parameter(required = true)
	private String analysisArtifactsDir;

	@Parameter
	private Boolean diffOnTimestamp = false;

	@Parameter
	private Boolean diffOnAuthor = false;

	@Parameter
	private Boolean diffOnModule = false;

	@Parameter
	private Boolean diffOnPath = false;

	@Parameter
	private String importDate;

	@Parameter(required = true)
	private String deltaIbdfFilePath;

	@Parameter
	private Boolean generateAnalysisFiles = true;

	@Parameter(required = true)
	protected String converterSourceArtifactVersion;

	private static BinaryDataDifferService differService;

	private static final Logger log = LogManager.getLogger(BinaryDataDifferMojo.class);

	public void execute() throws MojoExecutionException {

		ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> baseContentMap = null;
		ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap = null;
		ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents = null;

		differService = LookupService.getService(BinaryDataDifferService.class);
		differService.initialize(analysisArtifactsDir, inputAnalysisDir, deltaIbdfFilePath, generateAnalysisFiles,
				diffOnTimestamp, diffOnAuthor, diffOnModule, diffOnPath, importDate,
				"VHAT " + converterSourceArtifactVersion);

		Map<OchreExternalizableObjectType, Set<OchreExternalizable>> baseContentMapOrig = null;
		Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMapOrig = null;
		Map<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponentsOrig = null;

		boolean ranInputAnalysis = false;
		boolean ranOutputAnalysis = false;

		int threadCount = Runtime.getRuntime().availableProcessors();

		try {
			log.info("Processing Base version IBDF File in background thread");
			ExecutorService processBaseInputIbdfService = LookupService.getService(WorkExecutors.class).getIOExecutor();
			Future<ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>>> futureBaseContentMap = processBaseInputIbdfService
					.submit(new ProcessIbdfFile(differService, baseVersionFile, "BASE"));

			log.info("Processing New version IBDF File");
			newContentMap = differService.processInputIbdfFile(newVersionFile, "NEW");
			baseContentMap = futureBaseContentMap.get();

			/*
			 * //We don't shutdown the writer service we are using, because it
			 * is the core isaac thread pool. //waiting for the future checker
			 * service is sufficient to ensure that all write operations are
			 * complete. baseVersionProcesserService.shutdown();
			 * baseVersionProcesserService.awaitTermination(15,
			 * TimeUnit.MINUTES); newVersionProcesserService.shutdown();
			 * newVersionProcesserService.awaitTermination(15,
			 * TimeUnit.MINUTES);
			 * 
			 */

			// Transform input base & new content into text & json files
			Future<Boolean> futureBaseAnalysisGenerated = null;
			Future<Boolean> futureNewAnalysisGenerated = null;
			Future<Boolean> futureDeltaAnalysisGenerated = null;
			if (generateAnalysisFiles) {
				log.info("Creating analysis files for input/output files");
				ranInputAnalysis = true;

				ExecutorService analyzeBaseInputIbdfService = LookupService.getService(WorkExecutors.class)
						.getIOExecutor();
				futureBaseAnalysisGenerated = analyzeBaseInputIbdfService.submit(
						new GenerateInputAnalysisFile(differService, baseContentMap, "BASE", "baseVersion.json"));

				ExecutorService analyzeNewInputIbdfService = LookupService.getService(WorkExecutors.class)
						.getIOExecutor();
				futureNewAnalysisGenerated = analyzeNewInputIbdfService
						.submit(new GenerateInputAnalysisFile(differService, baseContentMap, "NEW", "newVersion.json"));
			}

			// Execute diff process
			log.info("Running Compute Delta");
			changedComponents = differService.computeDelta(baseContentMap, newContentMap);

			// Create diff IBDF file
			log.info("\n\nCreating the delta ibdf file");
			differService.generateDeltaIbdfFile(changedComponents);

			// Transform diff IBDF file into text & json files
			log.info("\n\nCreating analysis files for diff file");
			if (generateAnalysisFiles) {
				ranOutputAnalysis = true;
				ExecutorService analyzeDeltaService = LookupService.getService(WorkExecutors.class).getIOExecutor();
				futureDeltaAnalysisGenerated = analyzeDeltaService
						.submit(new GenerateDeltaAnalysisFile(differService, changedComponents));

				differService.writeChangeSetForVerification();
			}
			futureBaseAnalysisGenerated.get();
			futureNewAnalysisGenerated.get();
			futureDeltaAnalysisGenerated.get();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			differService.releaseLock();
		}

	}

	/**
	 * Class to ensure that any exceptions associated with indexingFutures are
	 * properly logged.
	 */
	private static class FutureChecker implements Runnable {

		Future<Map<OchreExternalizableObjectType, Set<OchreExternalizable>>> future_;

		public FutureChecker(Future<Map<OchreExternalizableObjectType, Set<OchreExternalizable>>> future) {
			future_ = future;
		}

		@Override
		public void run() {
			try {
				future_.get();
			} catch (InterruptedException | ExecutionException ex) {
				log.fatal("Unexpected error in future checker!", ex);
			}
		}
	}

	private class ProcessIbdfFile
			implements Callable<ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>>> {

		BinaryDataDifferService differService_ = null;
		File baseVersionFile_ = null;
		private String type_;

		public ProcessIbdfFile(BinaryDataDifferService differService, File baseVersionFile, String type) {
			differService_ = differService;
			baseVersionFile_ = baseVersionFile;
			type_ = type;
		}

		@Override
		public ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> call() throws Exception {
			return differService_.processInputIbdfFile(baseVersionFile_, type_);
		}

	}

	private class GenerateInputAnalysisFile implements Callable<Boolean> {

		private BinaryDataDifferService differService_ = null;
		private ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> mapToProcess_ = null;
		private String type_;
		private String name_;

		public GenerateInputAnalysisFile(BinaryDataDifferService differService,
				ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> contentMap, String type,
				String name) {
			differService_ = differService;
			mapToProcess_ = contentMap;
			type_ = type;
			name_ = name;
		}

		@Override
		public Boolean call() throws Exception {
			return differService_.analyzeInputMap(mapToProcess_, type_, name_);
		}

	}

	private class GenerateDeltaAnalysisFile implements Callable<Boolean> {

		private BinaryDataDifferService differService_ = null;
		private ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> mapToProcess_ = null;

		public GenerateDeltaAnalysisFile(BinaryDataDifferService differService,
				ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents) {
			differService_ = differService;
			mapToProcess_ = changedComponents;
		}

		@Override
		public Boolean call() throws Exception {
			return differService_.analyzeDeltaMap(mapToProcess_);
		}

	}

	private class GenerateDeltaIbdfFile implements Callable<Boolean> {

		private BinaryDataDifferService differService_ = null;
		private ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> mapToProcess_ = null;

		public GenerateDeltaIbdfFile(BinaryDataDifferService differService,
				ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents) {
			differService_ = differService;
			mapToProcess_ = changedComponents;
		}

		@Override
		public Boolean call() throws Exception {
			return differService.generateDeltaIbdfFile(mapToProcess_);
		}

	}
}

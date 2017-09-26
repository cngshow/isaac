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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService.ChangeType;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService.IbdfInputVersion;
import gov.vha.isaac.ochre.api.externalizable.InputIbdfVersionContent;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
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

	private static final Logger log = LogManager.getLogger(BinaryDataDifferMojo.class);

	public void execute() throws MojoExecutionException {
		ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents = null;

		final BinaryDataDifferService differService = LookupService.getService(BinaryDataDifferService.class);
		differService.initialize(analysisArtifactsDir, inputAnalysisDir, deltaIbdfFilePath, generateAnalysisFiles,
				diffOnTimestamp, diffOnAuthor, diffOnModule, diffOnPath, importDate,
				"VHAT " + converterSourceArtifactVersion);

		try {
			log.info("Processing Base version IBDF File in background thread");
			ExecutorService processBaseInputIbdfService = LookupService.getService(WorkExecutors.class).getIOExecutor();
			Future<InputIbdfVersionContent> futureBaseContentMap = processBaseInputIbdfService
					.submit(new ProcessIbdfFile(differService, baseVersionFile, IbdfInputVersion.BASE));

			log.info("Processing New version IBDF File");

			final AtomicReference<InputIbdfVersionContent> newContent = new AtomicReference<>(
					differService.transformInputIbdfFile(newVersionFile, IbdfInputVersion.NEW));
			final AtomicReference<InputIbdfVersionContent> baseContent = new AtomicReference<>(
					futureBaseContentMap.get());

			// Transform input base & new content into text & json files
			Future<Boolean> futureNewAnalysisGenerated = null;
			Future<Boolean> futureDeltaAnalysisGenerated = null;
			if (generateAnalysisFiles) {
				log.info("Creating analysis files for input/output files");

				differService.generateInputAnalysisFile(newContent.get(), IbdfInputVersion.BASE);

				ExecutorService analyzeNewInputIbdfService = LookupService.getService(WorkExecutors.class)
						.getIOExecutor();
				futureNewAnalysisGenerated = analyzeNewInputIbdfService
						.submit(new GenerateInputAnalysisFile(differService, newContent, IbdfInputVersion.NEW));
			}

			// Execute diff process
			log.info("Running Compute Delta");
			changedComponents = differService.computeDelta(baseContent.get(), newContent.get());

			// Create diff IBDF file
			log.info("Creating the delta ibdf file");
			differService.createDeltaIbdfFile(changedComponents);

			// Transform diff IBDF file into text & json files
			if (generateAnalysisFiles) {
				log.info("\n\nCreating analysis files for diff file");
				ExecutorService analyzeDeltaService = LookupService.getService(WorkExecutors.class).getIOExecutor();
				futureDeltaAnalysisGenerated = analyzeDeltaService
						.submit(new GenerateDeltaAnalysisFile(differService, changedComponents));

				log.info("\n\nCreating analysis files of diff ibdf file");
				differService.writeChangeSetForVerification();
			}
			futureNewAnalysisGenerated.get();
			futureDeltaAnalysisGenerated.get();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			// TODO: Still need?
			differService.releaseLock();
		}

	}

	private class ProcessIbdfFile implements Callable<InputIbdfVersionContent> {

		BinaryDataDifferService differService_ = null;
		File baseVersionFile_ = null;
		IbdfInputVersion verion_;

		public ProcessIbdfFile(BinaryDataDifferService differService, File baseVersionFile, IbdfInputVersion verion) {
			differService_ = differService;
			baseVersionFile_ = baseVersionFile;
			verion_ = verion;
		}

		@Override
		public InputIbdfVersionContent call() throws Exception {
			return differService_.transformInputIbdfFile(baseVersionFile_, verion_);
		}
	}

	private class GenerateInputAnalysisFile implements Callable<Boolean> {

		private BinaryDataDifferService differService_ = null;
		private AtomicReference<InputIbdfVersionContent> content_ = null;
		private IbdfInputVersion verion_;

		public GenerateInputAnalysisFile(BinaryDataDifferService differService,
				AtomicReference<InputIbdfVersionContent> content, IbdfInputVersion verion) {
			differService_ = differService;
			content_ = content;
			verion_ = verion;
		}

		@Override
		public Boolean call() throws Exception {
			return differService_.generateInputAnalysisFile(content_.get(), verion_);
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
			return differService_.generateAnalysisFileFromDiffObject(mapToProcess_);
		}

	}
}

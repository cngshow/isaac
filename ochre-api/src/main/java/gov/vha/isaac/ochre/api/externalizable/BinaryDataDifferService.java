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
package gov.vha.isaac.ochre.api.externalizable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jvnet.hk2.annotations.Contract;

import gov.vha.isaac.ochre.api.util.InputIbdfVersionContent;

/**
 * HK2 Service Contract for BinaryDataDifferProvider
 * 
 * {@link BinaryDataDifferProvider}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Contract
public interface BinaryDataDifferService {

	/**
	 * An Enum used to represent the types of changes encountered.
	 */
	public enum ChangeType {

		/** The new components. */
		NEW_COMPONENTS,
		/** The retired components. */
		RETIRED_COMPONENTS,
		/** The modified components. */
		MODIFIED_COMPONENTS;
	}

	/**
	 * An Enum used to represent either the original ibdf or the new ibdf to be
	 * diffed.
	 */
	public enum IbdfInputVersion {

		/** The base IBDF version. */
		BASE,
		/** The new IBDF version. */
		NEW;
	}

	/**
	 * Initialize the process prior to execution specifying directories, the
	 * metadata should be included when diffing contents, and stamp definition.
	 *
	 * @param analysisArtifactsDir
	 *            Directory used to store analysis files generated during call
	 *            to {@link #generateAnalysisFiles(Map, Map, Map)}
	 * @param inputFilesDir
	 *            Directory containing BASE and NEW ibdf files to diff
	 * @param deltaIbdfFilePath
	 *            The location of the output ibdf file containing the diff
	 *            between the BASE and NEW ibdf files.
	 * @param generateAnalysisFiles
	 *            True if {@link #generateAnalysisFiles(Map, Map, Map)} should
	 *            be executed
	 * @param diffOnTimestamp
	 *            True if should consider two components having same UUID
	 *            different if the two versions have different timestamps
	 * @param diffOnAuthor
	 *            True if should consider two components having same UUID
	 *            different if the two versions have different authors
	 * @param diffOnModule
	 *            True if should consider two components having same UUID
	 *            different if the two versions have different modules
	 * @param diffOnPath
	 *            True if should consider two components having same UUID
	 *            different if the two versions have different paths
	 * @param importDate
	 *            The date to be used in the Stamps that are used when creating
	 *            new versions in the delta ibdf file
	 * @param moduleName
	 *            The module to be used in the Stamps that are used when
	 *            creating new versions in the delta ibdf file
	 */
	public void initialize(String analysisArtifactsDir, String inputFilesDir, String deltaIbdfFilePath,
			Boolean generateAnalysisFiles, boolean diffOnTimestamp, boolean diffOnAuthor, boolean diffOnModule,
			boolean diffOnPath, String importDate, String moduleName);

	/**
	 * Process that iterates over an input ibdf file and transforms its contents
	 * into a {@link InputIbdfVersionContent} for further evaluation.
	 *
	 * @param versionFile
	 *            The ibdf file to be written out for analysis. It is either the
	 *            input BASE ibdf file or the input NEW ibdf file that are to be
	 *            diffed.
	 * @param verion
	 *            the {@link IbdfInputVersion} (either BASE or NEW)
	 * @return the {@link InputIbdfVersionContent} object containing the content
	 *         found in the versionFile (concepts, sememes, stamp aliases, and
	 *         stamp comments)
	 * @throws Exception
	 *             Thrown when problem encountered reading the input file
	 */
	public InputIbdfVersionContent transformInputIbdfFile(File versionFile, IbdfInputVersion verion) throws Exception;

	/**
	 * Process generating the differences between the BASE and NEW content in
	 * multi-threaded fashion
	 *
	 * @param baseContent
	 *            the BASE content in {@link InputIbdfVersionContent} form
	 * @param newContent
	 *            the NEW content in {@link InputIbdfVersionContent} form
	 * @return the differences expressed as a map of {@link ChangeType} to the
	 *         diffed {@link OchreExternalizable} content of that type
	 * @throws Exception
	 *             to handle multi-threaded exceptions
	 */
	public ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> computeDelta(
			InputIbdfVersionContent baseContent, InputIbdfVersionContent newContent) throws Exception;

	/**
	 * Generates the actual ibdf delta file that can then be used to directly
	 * import onto a database containing only BASE content. It contains the
	 * identified differences between the BASE and NEW ibdf files as calculated
	 * during the method {@link #computeDelta(Map, Map)}.
	 *
	 * @param changedComponents
	 *            Map of {@link ChangeType} to the diffed changed
	 *            {@link OchreExternalizable} content of that type
	 * @return a boolean indicating success/failure of operation
	 */
	public Boolean createDeltaIbdfFile(
			ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents);

	/**
	 * Generates two files. 1) A json representation of just the content found
	 * in the content passed in and 2) a text representation containing the
	 * content of both {@link IbdfInputVersion}.
	 * 
	 * For each file, the file's contents are grouped by {@link ChangeType}, and
	 * within each {@link ChangeType}, by {@link OchreExternalizable}. The text
	 * file has a top-level grouping by {@link IbdfInputVersion}.
	 * 
	 * File location defined by the {@link BinaryDataDifferService#initialize}
	 * method.
	 *
	 * @param content
	 *            the content represented in {@link InputIbdfVersionContent}
	 *            form
	 * @param version
	 *            the {@link IbdfInputVersion} (either BASE or NEW)
	 * @return a boolean indicating success/failure of operation
	 * @throws IOException
	 */
	public Boolean generateInputAnalysisFile(InputIbdfVersionContent content, IbdfInputVersion verion)
			throws IOException;

	/**
	 * Generates a json and a text file containing the changed components based
	 * on the contents of the Diff object. The printed out contents are grouped
	 * by {@link ChangeType}, and within each {@link ChangeType}, by
	 * {@link OchreExternalizable}.
	 *
	 * File location defined by the {@link BinaryDataDifferService#initialize}
	 * method.
	 * 
	 * @param changedComponents
	 *            the differences between the BASE and NEW content expressed as
	 *            a map of {@link ChangeType} to the diffed
	 *            {@link OchreExternalizable} content of that type
	 * @throws IOException
	 *             Thrown if an exception occurred in writing the json or text
	 *             files.
	 * @return a boolean indicating success/failure of operation
	 */
	public Boolean generateAnalysisFileFromDiffObject(
			ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents)
			throws IOException;

	/**
	 * Generates a json containing the changed components based on the ibdf
	 * delta file. The printed out contents are grouped by {@link ChangeType},
	 * and within each {@link ChangeType}, by {@link OchreExternalizable}.
	 *
	 * The location of both the delta file (used as input) and the generated
	 * output file is defined by the {@link BinaryDataDifferService#initialize}
	 * method.
	 * 
	 * @throws FileNotFoundException
	 *             Thrown if the delta file is not found as expected
	 */
	public void writeChangeSetForVerification() throws FileNotFoundException;

	/**
	 * Releases the mutli-threading lock.
	 */
	public void releaseLock();
}

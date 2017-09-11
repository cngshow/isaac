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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jvnet.hk2.annotations.Contract;

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
	 * The Enum ChangeType.
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
	 * Initialize the process prior to execution specifying directories, the
	 * metadata should be included when diffing contents, and stamp definition.
	 *
	 * @param analysisArtifactsDir
	 *            Directory used to store analysis files generated during call
	 *            to {@link #generateAnalysisFiles(Map, Map, Map)}
	 * @param inputFilesDir
	 *            Directory containing base and new ibdf files to diff
	 * @param deltaIbdfFilePath
	 *            The location of the output ibdf file containing the diff
	 *            between the base and new ibdf files.
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
	 * Process that iterates over the input ibdf files and returns their
	 * contents to be evaluated.
	 *
	 * @param versionFile
	 *            The ibdf file to be written out for analysis. It is either the
	 *            input base ibdf file or the input new ibdf file that are to be
	 *            diffed.
	 * @param type
	 * @return the map The base ibdf file's content in a map of
	 *         {@link OchreExternalizableObjectType} to
	 *         {@link OchreExternalizable}
	 * @throws Exception
	 *             Thrown when problem encountered reading the input file
	 */
	public ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> processInputIbdfFile(
			File versionFile, String type) throws Exception;

	/**
	 * Top level process generating the identification of
	 * new/inactivated/modified {@link OchreExternalizable} types.
	 *
	 * @param baseContentMap
	 *            the base ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param newContentMap
	 *            the new ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @return the map of {@link ChangeType} defined as (new, inactivated,
	 *         modified) to the identified {@link OchreExternalizable} content
	 */
	public ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> computeDelta(
			ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> baseContentMap,
			ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap);

	/**
	 * Generates human readable analysis files to help debug process. Contents
	 * generated represent a) both inputed ibdf files, b) mapped
	 * {@link ChangeType) to {@link OchreExternalizable} content, and c) delta
	 * ibdf file.
	 *
	 * @param baseContentMap
	 *            the base ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param newContentMap
	 *            the new ibdf file's content in a map of
	 *            {@link OchreExternalizableObjectType} to
	 *            {@link OchreExternalizable}
	 * @param changedComponents
	 *            map of {@link ChangeType} defined as (new, inactivated,
	 *            modified) to the identified {@link OchreExternalizable}
	 *            content.
	 */
	public Boolean analyzeDeltaMap(ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> deltaMap);

	public Boolean analyzeInputMap(ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> inputMap,
			String type, String name);

	/**
	 * Generates the ibdf delta file, which is the desired artifact of this
	 * operation, containing the identified differences between the base and new
	 * ibdf files as calculated during the method
	 * {@link #computeDelta(Map, Map)}.
	 *
	 * @param changedComponents
	 *            Map of {@link ChangeType} defined as (new, inactivated,
	 *            modified) to the identified changed
	 *            {@link OchreExternalizable} content
	 * @throws Exception
	 *             Thrown when problem either reading the contents of the map or
	 *             in writing to the ibdf file
	 */
	public Boolean generateDeltaIbdfFile(
			ConcurrentHashMap<ChangeType, CopyOnWriteArrayList<OchreExternalizable>> changedComponents);

	public void releaseLock();

	public void writeChangeSetForVerification() throws FileNotFoundException;

}

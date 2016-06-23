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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jvnet.hk2.annotations.Contract;

/**
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Contract
public interface BinaryDataDifferService {
	public enum ChangeType {
		NEW_COMPONENTS, RETIRED_COMPONENTS, MODIFIED_COMPONENTS;
	}

	/**
	 * Return a queue view of the data reader service - the queue being
	 * populated by a multi-threaded operation. Order is not maintained.
	 * @param importDate 
	 * 
	 * @return
	 */
	public void initialize(String analysisFilesOutputDir, String ibdfFileOutputDir, String changesetFileName,
			Boolean createAnalysisFiles, boolean diffOnStatus, boolean diffOnTimestamp, boolean diffOnAuthor,
			boolean diffOnModule, boolean diffOnPath, String importDate);

	/**
	 * Call to determine if no futher elements will populate the queue
	 * 
	 * @param newVersionFile
	 * @param oldVersionFile
	 * @throws Exception
	 */
	public Map<OchreExternalizableObjectType, Set<OchreExternalizable>> processVersion(File versionFile)
			throws Exception;

	/**
	 * Cancel any inprogress processing
	 * 
	 * @param newContentMap
	 * @param oldContentMap
	 */
	public Map<ChangeType, List<OchreExternalizable>> identifyVersionChanges(
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap);

	public void writeFilesForAnalysis(Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap,
			Map<ChangeType, List<OchreExternalizable>> changedComponents, String ibdfFileOutputDir,
			String analysisFilesOutputDir);

	public void generateDiffedIbdfFile(Map<ChangeType, List<OchreExternalizable>> changedComponents) throws Exception;
}

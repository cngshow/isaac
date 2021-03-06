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
 * HK2 Service Contract for BinaryDataDifferProvider
 * 
 * {@link BinaryDataDifferProvider}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Contract
public interface BinaryDataDifferService {
	public enum ChangeType {
		NEW_COMPONENTS, RETIRED_COMPONENTS, MODIFIED_COMPONENTS;
	}

	public void initialize(String comparisonAnalysisDir, String inputFilesDir, String deltaIbdfPath,
			Boolean createAnalysisFiles, boolean diffOnStatus, boolean diffOnTimestamp, boolean diffOnAuthor,
			boolean diffOnModule, boolean diffOnPath, String importDate, String moduleName);

	public Map<OchreExternalizableObjectType, Set<OchreExternalizable>> processInputIbdfFil(File versionFile)
			throws Exception;

	public Map<ChangeType, List<OchreExternalizable>> computeDelta(
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap);

	public void createAnalysisFiles(Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap,
			Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap,
			Map<ChangeType, List<OchreExternalizable>> changedComponents);

	public void generateDeltaIbdfFile(Map<ChangeType, List<OchreExternalizable>> changedComponents) throws Exception;
}

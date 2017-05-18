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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataDifferService.ChangeType;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
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
	private File oldVersionFile;

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
	private String comparisonAnalysisDir;

	@Parameter
	private Boolean diffOnStatus = false;

	@Parameter
	private Boolean diffOnTimestamp = false;

	@Parameter
	private Boolean diffOnAuthor = false;

	@Parameter
	private Boolean diffOnModule = false;

	@Parameter
	private Boolean diffOnPath = false;

	@Parameter
	private Boolean createAnalysisFiles = true;

	@Parameter
	private String importDate;

	@Parameter(required = true)
	private String deltaIbdfPath;

	public void execute() throws MojoExecutionException {
		BinaryDataDifferService differService = LookupService.getService(BinaryDataDifferService.class);
		differService.initialize(comparisonAnalysisDir, inputAnalysisDir, deltaIbdfPath, createAnalysisFiles,
				diffOnStatus, diffOnTimestamp, diffOnAuthor, diffOnModule, diffOnPath, importDate);

		Map<OchreExternalizableObjectType, Set<OchreExternalizable>> oldContentMap = null;
		Map<OchreExternalizableObjectType, Set<OchreExternalizable>> newContentMap = null;
		
		try {
			oldContentMap = differService.processInputIbdfFil(oldVersionFile);
			newContentMap = differService.processInputIbdfFil(newVersionFile);

			Map<ChangeType, List<OchreExternalizable>> changedComponents = differService
					.computeDelta(oldContentMap, newContentMap);

			differService.generateDeltaIbdfFile(changedComponents);

			if (createAnalysisFiles) {
				differService.createAnalysisFiles(oldContentMap, newContentMap, changedComponents);
			}
		} catch (Exception e) {
			if (createAnalysisFiles) {
				differService.createAnalysisFiles(oldContentMap, newContentMap, null);
			}
			throw new MojoExecutionException(e.getMessage(), e);
		}

	}
}

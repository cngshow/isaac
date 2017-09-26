/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.vha.isaac.ochre.pombuilder.dbdiff;

public class IBDFDifferPropertiesImpl implements IBDFDifferProperties {

	private String name;
	private String version;
	private String description;
	private String metadataVerion;

	// Base Terminology as defined by the below attributes
	private String baseGroupId;
	private String baseArtifactId;
	private String baseVersion;
	private String baseLoader;

	// New Terminology as defined by the below attributes
	private String newGroupId;
	private String newArtifactId;
	private String newVersion;
	private String newLoader;

	// Database to be used as defined by the below attributes
	private String dbGroupId;
	private String dbArtifactId;
	private String dbVersion;
	private String dbClassifier;
	private String dbType;
	private String dbIndexType;

	// Timestamp to be used in STAMP (for when creating new components or new
	// versions of
	// existing components). Right now, just a date in this format: 2017-09-30.
	// Note, may be changed to include time.
	private String importDate;

	// Delta IBDF file name containing changes b/w Base & New
	// Required
	private String deltaIbdfFileName;

	// Boolean letting user request if analysis files are to be generated. This
	// will slow things down but help analysis
	// Required
	// Default=True
	private Boolean generateAnalysisFiles = true;

	// Module to create i.e. “VHAT 2017.05.04”
	// Required
	private String converterSourceArtifactVersion;

	// Defines if everything is the same other than the Timestamp, if there
	// should be considered different
	// (Likely to be removed)
	private Boolean diffOnTimestamp = false;

	// Defines if everything is the same other than the Author, if there should
	// be considered different
	// (Likely to be removed)
	private Boolean diffOnAuthor = false;

	// Defines if everything is the same other than the Module, if there should
	// be considered different
	// (Likely to be removed)
	private Boolean diffOnModule = false;

	// Defines if everything is the same other than the Path, if there should be
	// considered different
	// (Likely to be removed)
	private Boolean diffOnPath = false;

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.PomBuilderProperties#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.PomBuilderProperties#setName(java.lang.
	 * String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.PomBuilderProperties#getVersion()
	 */
	@Override
	public String getVersion() {
		return version;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.PomBuilderProperties#setVersion(java.lang.
	 * String)
	 */
	@Override
	public void setVersion(String version) {
		this.version = version;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.PomBuilderProperties#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.PomBuilderProperties#setDescription(java.
	 * lang.String)
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.PomBuilderProperties#getMetadataVerion()
	 */
	@Override
	public String getMetadataVerion() {
		return metadataVerion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.PomBuilderProperties#setMetadataVerion(
	 * java.lang.String)
	 */
	@Override
	public void setMetadataVerion(String metadataVerion) {
		this.metadataVerion = metadataVerion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getBaseGroupId()
	 */
	@Override
	public String getBaseGroupId() {
		return baseGroupId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setBaseGroupId(java.lang.String)
	 */
	@Override
	public void setBaseGroupId(String baseGroupId) {
		this.baseGroupId = baseGroupId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getBaseArtifactId()
	 */
	@Override
	public String getBaseArtifactId() {
		return baseArtifactId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setBaseArtifactId(java.lang.String)
	 */
	@Override
	public void setBaseArtifactId(String baseArtifactId) {
		this.baseArtifactId = baseArtifactId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getBaseVersion()
	 */
	@Override
	public String getBaseVersion() {
		return baseVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setBaseVersion(java.lang.String)
	 */
	@Override
	public void setBaseVersion(String baseVersion) {
		this.baseVersion = baseVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getBaseLoader
	 * ()
	 */
	@Override
	public String getBaseLoader() {
		return baseLoader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setBaseLoader
	 * (java.lang.String)
	 */
	@Override
	public void setBaseLoader(String baseLoader) {
		this.baseLoader = baseLoader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getNewGroupId
	 * ()
	 */
	@Override
	public String getNewGroupId() {
		return newGroupId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setNewGroupId
	 * (java.lang.String)
	 */
	@Override
	public void setNewGroupId(String newGroupId) {
		this.newGroupId = newGroupId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getNewArtifactId()
	 */
	@Override
	public String getNewArtifactId() {
		return newArtifactId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setNewArtifactId(java.lang.String)
	 */
	@Override
	public void setNewArtifactId(String newArtifactId) {
		this.newArtifactId = newArtifactId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getNewVersion
	 * ()
	 */
	@Override
	public String getNewVersion() {
		return newVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setNewVersion
	 * (java.lang.String)
	 */
	@Override
	public void setNewVersion(String newVersion) {
		this.newVersion = newVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getNewLoader(
	 * )
	 */
	@Override
	public String getNewLoader() {
		return newLoader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setNewLoader(
	 * java.lang.String)
	 */
	@Override
	public void setNewLoader(String newLoader) {
		this.newLoader = newLoader;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getDbGroupId(
	 * )
	 */
	@Override
	public String getDbGroupId() {
		return dbGroupId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setDbGroupId(
	 * java.lang.String)
	 */
	@Override
	public void setDbGroupId(String dbGroupId) {
		this.dbGroupId = dbGroupId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getDbArtifactId()
	 */
	@Override
	public String getDbArtifactId() {
		return dbArtifactId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setDbArtifactId(java.lang.String)
	 */
	@Override
	public void setDbArtifactId(String dbArtifactId) {
		this.dbArtifactId = dbArtifactId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getDbVersion(
	 * )
	 */
	@Override
	public String getDbVersion() {
		return dbVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setDbVersion(
	 * java.lang.String)
	 */
	@Override
	public void setDbVersion(String dbVersion) {
		this.dbVersion = dbVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getDbClassifier()
	 */
	@Override
	public String getDbClassifier() {
		return dbClassifier;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setDbClassifier(java.lang.String)
	 */
	@Override
	public void setDbClassifier(String dbClassifier) {
		this.dbClassifier = dbClassifier;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getDbType()
	 */
	@Override
	public String getDbType() {
		return dbType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setDbType(
	 * java.lang.String)
	 */
	@Override
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getDbIndexType()
	 */
	@Override
	public String getDbIndexType() {
		return dbIndexType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setDbIndexType(java.lang.String)
	 */
	@Override
	public void setDbIndexType(String dbIndexType) {
		this.dbIndexType = dbIndexType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getImportDate
	 * ()
	 */
	@Override
	public String getImportDate() {
		return importDate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setImportDate
	 * (java.lang.String)
	 */
	@Override
	public void setImportDate(String importDate) {
		this.importDate = importDate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getDeltaIbdfFileName()
	 */
	@Override
	public String getDeltaIbdfFileName() {
		return deltaIbdfFileName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setDeltaIbdfFileName(java.lang.String)
	 */
	@Override
	public void setDeltaIbdfFileName(String deltaIbdfFileName) {
		this.deltaIbdfFileName = deltaIbdfFileName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getGenerateAnalysisFiles()
	 */
	@Override
	public Boolean getGenerateAnalysisFiles() {
		return generateAnalysisFiles;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setGenerateAnalysisFiles(java.lang.Boolean)
	 */
	@Override
	public void setGenerateAnalysisFiles(Boolean generateAnalysisFiles) {
		this.generateAnalysisFiles = generateAnalysisFiles;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getConverterSourceArtifactVersion()
	 */
	@Override
	public String getConverterSourceArtifactVersion() {
		return converterSourceArtifactVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setConverterSourceArtifactVersion(java.lang.String)
	 */
	@Override
	public void setConverterSourceArtifactVersion(String converterSourceArtifactVersion) {
		this.converterSourceArtifactVersion = converterSourceArtifactVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getDiffOnTimestamp()
	 */
	@Override
	public Boolean getDiffOnTimestamp() {
		return diffOnTimestamp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setDiffOnTimestamp(java.lang.Boolean)
	 */
	@Override
	public void setDiffOnTimestamp(Boolean diffOnTimestamp) {
		this.diffOnTimestamp = diffOnTimestamp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getDiffOnAuthor()
	 */
	@Override
	public Boolean getDiffOnAuthor() {
		return diffOnAuthor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setDiffOnAuthor(java.lang.Boolean)
	 */
	@Override
	public void setDiffOnAuthor(Boolean diffOnAuthor) {
		this.diffOnAuthor = diffOnAuthor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * getDiffOnModule()
	 */
	@Override
	public Boolean getDiffOnModule() {
		return diffOnModule;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#
	 * setDiffOnModule(java.lang.Boolean)
	 */
	@Override
	public void setDiffOnModule(Boolean diffOnModule) {
		this.diffOnModule = diffOnModule;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#getDiffOnPath
	 * ()
	 */
	@Override
	public Boolean getDiffOnPath() {
		return diffOnPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.pombuilder.dbdiff.IBDFDifferProperties2#setDiffOnPath
	 * (java.lang.Boolean)
	 */
	@Override
	public void setDiffOnPath(Boolean diffOnPath) {
		this.diffOnPath = diffOnPath;
	}

}

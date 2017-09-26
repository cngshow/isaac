package gov.vha.isaac.ochre.pombuilder.dbdiff;

import gov.vha.isaac.ochre.pombuilder.PomBuilderProperties;

public interface IBDFDifferProperties extends PomBuilderProperties{

	/**
	 * @return the baseGroupId
	 */
	String getBaseGroupId();

	/**
	 * @param baseGroupId the baseGroupId to set
	 */
	void setBaseGroupId(String baseGroupId);

	/**
	 * @return the baseArtifactId
	 */
	String getBaseArtifactId();

	/**
	 * @param baseArtifactId the baseArtifactId to set
	 */
	void setBaseArtifactId(String baseArtifactId);

	/**
	 * @return the baseVersion
	 */
	String getBaseVersion();

	/**
	 * @param baseVersion the baseVersion to set
	 */
	void setBaseVersion(String baseVersion);

	/**
	 * @return the baseLoader
	 */
	String getBaseLoader();

	/**
	 * @param baseLoader the baseLoader to set
	 */
	void setBaseLoader(String baseLoader);

	/**
	 * @return the newGroupId
	 */
	String getNewGroupId();

	/**
	 * @param newGroupId the newGroupId to set
	 */
	void setNewGroupId(String newGroupId);

	/**
	 * @return the newArtifactId
	 */
	String getNewArtifactId();

	/**
	 * @param newArtifactId the newArtifactId to set
	 */
	void setNewArtifactId(String newArtifactId);

	/**
	 * @return the newVersion
	 */
	String getNewVersion();

	/**
	 * @param newVersion the newVersion to set
	 */
	void setNewVersion(String newVersion);

	/**
	 * @return the newLoader
	 */
	String getNewLoader();

	/**
	 * @param newLoader the newLoader to set
	 */
	void setNewLoader(String newLoader);

	/**
	 * @return the dbGroupId
	 */
	String getDbGroupId();

	/**
	 * @param dbGroupId the dbGroupId to set
	 */
	void setDbGroupId(String dbGroupId);

	/**
	 * @return the dbArtifactId
	 */
	String getDbArtifactId();

	/**
	 * @param dbArtifactId the dbArtifactId to set
	 */
	void setDbArtifactId(String dbArtifactId);

	/**
	 * @return the dbVersion
	 */
	String getDbVersion();

	/**
	 * @param dbVersion the dbVersion to set
	 */
	void setDbVersion(String dbVersion);

	/**
	 * @return the dbClassifier
	 */
	String getDbClassifier();

	/**
	 * @param dbClassifier the dbClassifier to set
	 */
	void setDbClassifier(String dbClassifier);

	/**
	 * @return the dbType
	 */
	String getDbType();

	/**
	 * @param dbType the dbType to set
	 */
	void setDbType(String dbType);

	/**
	 * @return the dbIndexType
	 */
	String getDbIndexType();

	/**
	 * @param dbIndexType the dbIndexType to set
	 */
	void setDbIndexType(String dbIndexType);

	/**
	 * @return the importDate
	 */
	String getImportDate();

	/**
	 * @param importDate the importDate to set
	 */
	void setImportDate(String importDate);

	/**
	 * @return the deltaIbdfFileName
	 */
	String getDeltaIbdfFileName();

	/**
	 * @param deltaIbdfFileName the deltaIbdfFileName to set
	 */
	void setDeltaIbdfFileName(String deltaIbdfFileName);

	/**
	 * @return the generateAnalysisFiles
	 */
	Boolean getGenerateAnalysisFiles();

	/**
	 * @param generateAnalysisFiles the generateAnalysisFiles to set
	 */
	void setGenerateAnalysisFiles(Boolean generateAnalysisFiles);

	/**
	 * @return the converterSourceArtifactVersion
	 */
	String getConverterSourceArtifactVersion();

	/**
	 * @param converterSourceArtifactVersion the converterSourceArtifactVersion to set
	 */
	void setConverterSourceArtifactVersion(String converterSourceArtifactVersion);

	/**
	 * @return the diffOnTimestamp
	 */
	Boolean getDiffOnTimestamp();

	/**
	 * @param diffOnTimestamp the diffOnTimestamp to set
	 */
	void setDiffOnTimestamp(Boolean diffOnTimestamp);

	/**
	 * @return the diffOnAuthor
	 */
	Boolean getDiffOnAuthor();

	/**
	 * @param diffOnAuthor the diffOnAuthor to set
	 */
	void setDiffOnAuthor(Boolean diffOnAuthor);

	/**
	 * @return the diffOnModule
	 */
	Boolean getDiffOnModule();

	/**
	 * @param diffOnModule the diffOnModule to set
	 */
	void setDiffOnModule(Boolean diffOnModule);

	/**
	 * @return the diffOnPath
	 */
	Boolean getDiffOnPath();

	/**
	 * @param diffOnPath the diffOnPath to set
	 */
	void setDiffOnPath(Boolean diffOnPath);

}
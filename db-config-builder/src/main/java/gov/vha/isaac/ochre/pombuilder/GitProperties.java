package gov.vha.isaac.ochre.pombuilder;

public interface GitProperties {

	/**
	 * @return the gitRepositoryURL
	 */
	String getGitRepositoryURL();

	/**
	 * @param gitRepositoryURL the gitRepositoryURL to set
	 */
	void setGitRepositoryURL(String gitRepositoryURL);

	/**
	 * @return the gitUsername
	 */
	String getGitUsername();

	/**
	 * @param gitUsername the gitUsername to set
	 */
	void setGitUsername(String gitUsername);

	/**
	 * @return the gitPassword
	 */
	char[] getGitPassword();

	/**
	 * @param gitPassword the gitPassword to set
	 */
	void setGitPassword(char[] gitPassword);

}
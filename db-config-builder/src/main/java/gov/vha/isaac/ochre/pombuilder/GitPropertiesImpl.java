package gov.vha.isaac.ochre.pombuilder;

public class GitPropertiesImpl implements GitProperties {

	private String gitRepositoryURL;
	private String gitUsername;
	private char[] gitPassword;
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.pombuilder.GitProperties#getGitRepositoryURL()
	 */
	@Override
	public String getGitRepositoryURL() {
		return gitRepositoryURL;
	}
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.pombuilder.GitProperties#setGitRepositoryURL(java.lang.String)
	 */
	@Override
	public void setGitRepositoryURL(String gitRepositoryURL) {
		this.gitRepositoryURL = gitRepositoryURL;
	}
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.pombuilder.GitProperties#getGitUsername()
	 */
	@Override
	public String getGitUsername() {
		return gitUsername;
	}
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.pombuilder.GitProperties#setGitUsername(java.lang.String)
	 */
	@Override
	public void setGitUsername(String gitUsername) {
		this.gitUsername = gitUsername;
	}
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.pombuilder.GitProperties#getGitPassword()
	 */
	@Override
	public char[] getGitPassword() {
		return gitPassword;
	}
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.pombuilder.GitProperties#setGitPassword(char[])
	 */
	@Override
	public void setGitPassword(char[] gitPassword) {
		this.gitPassword = gitPassword;
	}
	
}

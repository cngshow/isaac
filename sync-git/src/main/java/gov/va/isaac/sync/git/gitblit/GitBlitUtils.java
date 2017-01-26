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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.sync.git.gitblit;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.sync.git.gitblit.models.RepositoryModel;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils.AccessRestrictionType;


/**
 * {@link GitBlitUtils}
 * 
 * This entire package exists because the GitBlit client API is a bit painful to use, and the client libraries they produce 
 * aren't available in maven central, and they have a dependency chain we may not want to drag in.
 * 
 * The code in this package, and below, are extracted from http://gitblit.github.io/gitblit-maven/ 
 * within the com.gitblit:gbapi:1.8.0 module.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class GitBlitUtils
{
	private static Logger log = LoggerFactory.getLogger(GitBlitUtils.class);
	

	/**
	 * Create a repository on a remote gitblit server
	 * @param baseRemoteAddress - should be a url like https://vadev.mantech.com:4848/git/ (though {@link #adjustBareUrlForGitBlit(String)} is utilized
	 * @param repoName a name such a foo or foo.git
	 * @param repoDesc the description
	 * @param username
	 * @param password
	 * @param allowRead true to allow unauthenticated users to read / clone the repository.  False to lock down the repository
	 * @throws IOException
	 */
	public static void createRepository(String baseRemoteAddress, String repoName, String repoDesc, String username, char[] password, boolean allowRead) throws IOException
	{
		try
		{
			RepositoryModel rm = new RepositoryModel(repoName, repoDesc, username, new Date());
			if (allowRead)
			{
				rm.accessRestriction = AccessRestrictionType.PUSH.toString();
			}
			
			boolean status =  RpcUtils.createRepository(rm, adjustBareUrlForGitBlit(baseRemoteAddress),
					username, password);
			log.info("Repository: "+repoName +", create successfully: " + status);
			if (!status)
			{
				throw new IOException("Create of repo '" + repoName + "' failed");
			}
		}
		catch (Exception e)
		{
			log.error("Failed to create repository: "+repoName +", Unexpected Error: ", e);
			throw new IOException("Failed to create repository: "+repoName +", Internal error", e);
		}
	}
	
	public static Set<String> readRepositories(String baseRemoteAddress, String username, char[] password) throws IOException
	{
		HashSet<String> results = new HashSet<>();
		RpcUtils.getRepositories(adjustBareUrlForGitBlit(baseRemoteAddress), username, password).forEach((name, value) -> results.add((String)value.get("name")));
		return results;
	}
	
	/**
	 * Take in a URL like https://vadev.mantech.com:4848/git/r/db_test.git
	 * and turn it into https://vadev.mantech.com:4848/git/
	 * @param url
	 * @return
	 * @throws IOException 
	 */
	public static String parseBaseRemoteAddress(String url) throws IOException
	{
		Pattern p = Pattern.compile("(?i)(https?:\\/\\/[a-zA-Z0-9\\.\\-_]+:?\\d*\\/[a-zA-Z0-9\\-_]+\\/)r\\/[a-zA-Z0-9\\-_]+.git$");
		Matcher m = p.matcher(url);
		if (m.find())
		{
			return m.group(1);
		}
		throw new IOException("Not a known giblit url pattern!");
	}
	
	/**
	 * This hackery is being done because of a code-sync issue between PRISME and ISAAC-Rest, where PRISME is putting a bare URL into the props file.
	 * It will be fixed on the PRISME side, eventually, making this method a noop - but for now, handle either the old or new style.
	 * 
	 * Essentially, if we see a bare URL like https://vaauscttdbs80.aac.va.gov:8080 we add /git to the end of it.
	 * If we see a URL that includes a location - like https://vaauscttdbs80.aac.va.gov:8080/gitServer - we do nothing more than add a trailing forward slash
	 * @param url
	 * @return
	 */
	public static String adjustBareUrlForGitBlit(String url)
	{
		String temp = url;
		if (!temp.endsWith("/"))
		{
			temp += "/";
		}
		if (temp.matches("(?i)https?:\\/\\/[a-zA-Z0-9\\.\\-_]+:?\\d*\\/$"))
		{
			temp += "git/";
		}
		return temp;
	}
}

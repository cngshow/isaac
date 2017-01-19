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
	 * Create a repository on the Gitblit server.
	 *
	 * @param serverUrl
	 * @param repo name
	 * @param repo description 
	 * @param account
	 * @param password
	 * @return true if the action succeeded
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
			
			boolean status =  RpcUtils.createRepository(rm, adjustUrlForGitBlit(baseRemoteAddress),
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
		RpcUtils.getRepositories(adjustUrlForGitBlit(baseRemoteAddress), username, password).forEach((name, value) -> results.add((String)value.get("name")));
		return results;
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
	public static String adjustUrlForGitBlit(String url)
	{
		String temp = url;
		if (!temp.endsWith("/"))
		{
			temp += "/";
		}
		if (temp.matches("https?:\\/\\/[a-zA-Z0-9\\.]+:?\\d*\\/$"))
		{
			temp += "git/";
		}
		return temp;
	}
}

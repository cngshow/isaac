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
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.sync.git.gitblit.models.RepositoryModel;
import gov.va.isaac.sync.git.gitblit.utils.RpcUtils;


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
	public static void createRepository(String baseRemoteAddress, String repoName, String repoDesc, String username, char[] password) throws IOException
	{
		try
		{
			boolean status =  RpcUtils.createRepository(new RepositoryModel(repoName, repoDesc, username, new Date()), baseRemoteAddress, username, password);
			log.info("Repository: "+repoName +", create successfully: " + status);
			if (!status)
			{
				throw new IOException("Create of repo '" + repoName + "' failed");
			}
		}
		catch (Exception e)
		{
			log.error("Failed to create repository: "+repoName +", Unexpected Error: ", e);
			throw new IOException("Failed to create repository: "+repoName +",Internal error", e);
		}
	}
	
	public static Set<String> readRepositories(String baseRemoteAddress, String username, char[] password) throws IOException
	{
		return RpcUtils.getRepositories(baseRemoteAddress, username, password).keySet();
	}
}

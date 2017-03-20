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
package gov.vha.isaac.ochre.pombuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.va.isaac.sync.git.SyncServiceGIT;
import gov.va.isaac.sync.git.gitblit.GitBlitUtils;
import gov.vha.isaac.ochre.api.util.NumericUtils;

/**
 * {@link GitPublish}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class GitPublish
{
	private static final Logger LOG = LogManager.getLogger();
	
	/**
	 * Support locking our threads across multiple operations (such as read tags, push a new tag) to ensure that two threads running in parallel 
	 * don't end up in a state where they can't push, due to a non-fastforward.
	 */
	private static final HashMap<String, ReentrantLock> repoLock = new HashMap<>();
	
	/**
	 * Take in a URL such as https://vadev.mantech.com:4848/git/ or https://vadev.mantech.com:4848/git and turn it into
	 * https://vadev.mantech.com:4848/git/r/contentConfigurations.git
	 * 
	 * If a full repo URL is passed in, such as https://vadev.mantech.com:4848/git/r/contentConfigurations.git, this does no processing
	 * and returns the passed in value.
	 * 
	 * @param gitblitBaseURL a URL like https://vadev.mantech.com:4848/git/
	 * @return the full git URL to a contentConfigurations repository.
	 * @throws IOException 
	 */
	public static String constructChangesetRepositoryURL(String gitblitBaseURL) throws IOException
	{
		if (gitblitBaseURL.matches("(?i)https?:\\/\\/[a-zA-Z0-9\\.\\-_]+:?\\d*\\/[a-zA-Z0-9\\-_]+\\/?$"))
		{
			return gitblitBaseURL + (gitblitBaseURL.endsWith("/") ? "" : "/") + "r/contentConfigurations.git";
		}
		else if (gitblitBaseURL.matches("(?i)https?:\\/\\/[a-zA-Z0-9\\.\\-_]+:?\\d*\\/[a-zA-Z0-9\\-_]+\\/r\\/[a-zA-Z0-9\\-_]+\\.git$"))
		{
			return gitblitBaseURL;
		}
		else
		{
			LOG.info("Failing constructChangesetRepositoryURL {}", gitblitBaseURL);
			throw new IOException("Unexpected gitblit server pattern");
		}
	}
	
	/**
	 * This routine will check out the project from the repository (which should have an empty master branch) - then locally 
	 * commit the changes to master, then tag it - then push the tag (but not the changes to master) so the upstream repo only 
	 * receives the tag. 
	 * 
	 * Calls {@link #constructChangesetRepositoryURL(String) to adjust the URL as necessary
	 */
	public static void publish(File folderWithProject, String gitRepository, String gitUserName, char[] gitPassword, String tagToCreate) throws Exception
	{
		LOG.debug("Publishing '{}' to '{}' using tag '{}'", folderWithProject.getAbsolutePath(), gitRepository, tagToCreate);
		String correctedURL = constructChangesetRepositoryURL(gitRepository);
		createRepositoryIfNecessary(correctedURL, gitUserName, gitPassword);
		SyncServiceGIT svc = new SyncServiceGIT();
		svc.setReadmeFileContent("ISAAC Dataprocessing Configuration Storage\n====\nIt is highly recommended you do not manually interact with this repository.");
		svc.setGitIgnoreContent("");
		boolean ignoreExists = new File(folderWithProject, ".gitignore").exists();
		boolean readmeExists = new File(folderWithProject, "README.md").exists();
		svc.setRootLocation(folderWithProject);
		svc.linkAndFetchFromRemote(correctedURL, gitUserName, gitPassword);
		svc.branch(folderWithProject.getName());
		//linkAndFetch creates these in master, but I don't want them in my branch (if they didn't exist before I linked / fetched).
		if (!ignoreExists)
		{
			new File(folderWithProject, ".gitignore").delete();
		}
		if (!readmeExists)
		{
			new File(folderWithProject, "README.md").delete();
		}
		svc.addUntrackedFiles();
		svc.commitAndTag("publishing conversion project", tagToCreate);
		svc.pushTag(tagToCreate, gitUserName, gitPassword);
		//Notice, I do NOT push the updates to the branch
	}
	
	/**
	 * Calls {@link #constructChangesetRepositoryURL(String) to adjust the URL as necessary
	 * @param gitRepository
	 * @param gitUserName
	 * @param gitPassword
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> readTags(String gitRepository, String gitUserName, char[] gitPassword) throws Exception
	{
		String correctedURL = constructChangesetRepositoryURL(gitRepository);
		createRepositoryIfNecessary(correctedURL, gitUserName, gitPassword);
		SyncServiceGIT svc = new SyncServiceGIT();
		
		File tempFolder = Files.createTempDirectory("tagRead").toFile();
		
		svc.setRootLocation(tempFolder);
		svc.linkAndFetchFromRemote(correctedURL, gitUserName, gitPassword);
		ArrayList<String> temp = svc.readTags(gitUserName, gitPassword);
		try
		{
			FileUtil.recursiveDelete(tempFolder);
		}
		catch (Exception e)
		{
			LOG.error("Problem cleaning up temp folder " + tempFolder, e);
		}
		return temp;
	}
	
	public static void createRepositoryIfNecessary(String gitRepository, String gitUserName, char[] gitPassword) throws IOException
	{
		String baseUrl = GitBlitUtils.parseBaseRemoteAddress(gitRepository);
		
		Set<String> repos = GitBlitUtils.readRepositories(baseUrl, gitUserName, gitPassword);
		
		String repoName = gitRepository.substring(gitRepository.lastIndexOf("/") + 1);
		
		if (!repos.contains(repoName))
		{
			LOG.info("Requested repository '" + gitRepository + "' does not exist - creating");
			GitBlitUtils.createRepository(baseUrl, repoName, "Configuration Storage Repository", gitUserName, gitPassword, true);
		}
		else
		{
			LOG.info("Requested repository '" + gitRepository + "' exists");
		}
	}
	
	/**
	 * This will return -1 if no tag was found matching the tagWithoutRevNumber.
	 * This will return 0 if a tag was found matching the tagWithoutRefNumber (but no tag was found with a revision number)
	 * This will return X > 0 if one or more tags were found with a revision number - returning the highest value.
	 */
	public static int readHighestRevisionNumber(ArrayList<String> existingTags, String tagWithoutRevNumber)
	{
		int highestBuildRevision = -1;
		for (String s : existingTags)
		{
			if (s.equals("refs/tags/" + tagWithoutRevNumber))
			{
				if (0 > highestBuildRevision)
				{
					highestBuildRevision = 0;
				}
			}
			else if (s.startsWith("refs/tags/" + tagWithoutRevNumber + "-"))
			{
				String revNumber = s.substring(("refs/tags/" + tagWithoutRevNumber + "-").length(), s.length());
				if (NumericUtils.isInt(revNumber))
				{
					int parsed = Integer.parseInt(revNumber);
					if (parsed > highestBuildRevision)
					{
						highestBuildRevision = parsed;
					}
				}
			}
		}
		return highestBuildRevision;
	}
	
	public static void lock(String gitRepository) throws IOException
	{
		String correctedURL = constructChangesetRepositoryURL(gitRepository);
		ReentrantLock lock = repoLock.get(correctedURL);
		
		synchronized (repoLock)
		{
			lock = repoLock.get(correctedURL);
			if (lock == null)
			{
				lock = new ReentrantLock();
				repoLock.put(correctedURL, lock);
			}
		}
		
		LOG.debug("Locking {}", correctedURL);
		lock.lock();
	}
	
	public static void unlock(String gitRepository) throws IOException
	{
		String correctedURL = constructChangesetRepositoryURL(gitRepository);
		ReentrantLock lock = repoLock.get(correctedURL);
		if (lock == null)
		{
			LOG.error("Unlock called, but no lock was present!");
		}
		else
		{
			if (lock.isHeldByCurrentThread())
			{
				LOG.debug("Unlocking {}", correctedURL);
				lock.unlock();
			}
			else
			{
				//To support rapid unlock, but also allow an unlock in a finally block, make it ok to unlock when not locked
				LOG.debug("Unlock called, but the lock wasn't held by this thread for {}", correctedURL);
			}
		}
	}
}

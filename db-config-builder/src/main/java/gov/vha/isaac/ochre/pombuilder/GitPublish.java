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
import java.nio.file.Files;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gov.va.isaac.sync.git.SyncServiceGIT;
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
	 * This routine will check out the project from the repository (which should have an empty master branch) - then locally 
	 * commit the changes to master, then tag it - then push the tag (but not the changes to master) so the upstream repo only 
	 * receives the tag. 
	 */
	public static void publish(File folderWithProject, String gitRepository, String gitUserName, String gitPassword, String tagToCreate) throws Exception
	{
		SyncServiceGIT svc = new SyncServiceGIT();
		svc.setReadmeFileContent("ISAAC Dataprocessing Configuration Storage\n====\nIt is highly recommended you do not manually interact with this repository.");
		svc.setGitIgnoreContent("");
		boolean ignoreExists = new File(folderWithProject, ".gitignore").exists();
		boolean readmeExists = new File(folderWithProject, "README.md").exists();
		svc.setRootLocation(folderWithProject);
		svc.linkAndFetchFromRemote(gitRepository, gitUserName, gitPassword);
		svc.branch(folderWithProject.getName());
		//linkAndFetch creates these in master, but I don't want them in my branch.
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
	
	public static ArrayList<String> readTags(String gitRepository, String gitUserName, String gitPassword) throws Exception
	{
		SyncServiceGIT svc = new SyncServiceGIT();
		
		File tempFolder = Files.createTempDirectory("tagRead").toFile();
		
		svc.setRootLocation(tempFolder);
		svc.linkAndFetchFromRemote(gitRepository, gitUserName, gitPassword);
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
}

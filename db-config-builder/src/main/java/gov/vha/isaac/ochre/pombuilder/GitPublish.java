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
import gov.va.isaac.sync.git.SyncServiceGIT;

/**
 * {@link GitPublish}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class GitPublish
{
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
	}
	
	public static ArrayList<String> readTags(String gitRepository, String gitUserName, String gitPassword) throws Exception
	{
		SyncServiceGIT svc = new SyncServiceGIT();
		
		File tempFolder = Files.createTempDirectory("tagRead").toFile();
		
		svc.setRootLocation(tempFolder);
		svc.linkAndFetchFromRemote(gitRepository, gitUserName, gitPassword);
		return svc.readTags(gitUserName, gitPassword);
		
	}
}

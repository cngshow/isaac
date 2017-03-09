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
package gov.vha.isaac.ochre.pombuilder.upload;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gov.vha.isaac.ochre.api.util.MavenPublish;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.api.util.Zip;
import gov.vha.isaac.ochre.pombuilder.FileUtil;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.converter.SupportedConverterTypes;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

/**
 * 
 * {@link SrcUploadCreator}
 * Create a new maven pom project which when executed, will upload a set of SDO input files
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class SrcUploadCreator
{
	private static final Logger LOG = LogManager.getLogger();
	
	
	/**
	 * @param uploadType - What type of content is being uploaded.
	 * @param version - What version number does the passed in content represent
	 * @param extensionName - optional - If the upload type is a type such as {@link SupportedConverterTypes#SCT_EXTENSION} which contains a 
	 * wildcard '*' in its {@link SupportedConverterTypes#getArtifactId()} value, this parameter must be provided, and is the string to use to 
	 * replace the wildcard.  This would typically be a value such as "en" or "fr", when used for snomed extension content.
	 * @param folderContainingContent - The folder that contains the required data files - these files will be zipped into an artifact and uploaded
	 * to the artifactRepositoryURL.
	 * @param gitRepositoryURL - The URL to publish this built project to
	 * @param gitUsername - The username to utilize to publish this project
	 * @param getPassword - The password to utilize to publish this project
	 * @return the tag created in the repository that carries the created project
	 * @param artifactRepositoryURL - The artifact server (nexus) path where the created artifact should be transferred.  This path should go all the way down to 
	 * a specific repository, such as http://vadev.mantech.com:8081/nexus/content/repositories/releases/ or http://vadev.mantech.com:8081/nexus/content/repositories/termdata/
	 * This should not point to a URL that represents a 'group' repository view.
	 * @param repositoryUsername - The username to utilize to upload the artifact to the artifact server
	 * @param repositoryPassword - The passwordto utilize to upload the artifact to the artifact server
	 * @return - the task handle - which will return the tag that was created in the git repository upon completion.  Note that the task is NOT yet started, when 
	 * it is returned.
	 * @throws Throwable 
	 */
	public static Task<String> createSrcUploadConfiguration(SupportedConverterTypes uploadType, String version, String extensionName, List<File> filesToUpload, 
			String gitRepositoryURL, String gitUsername, char[] gitPassword,
			String artifactRepositoryURL, String repositoryUsername, String repositoryPassword) throws Throwable
	{
		LOG.info("Building the task to create a source upload configuration for {}, version: {}, extensionName: {}, to git: {} and artifact server: {}", 
				uploadType, version, extensionName, gitRepositoryURL, artifactRepositoryURL);
		if (LOG.isDebugEnabled() && filesToUpload != null)
		{
			LOG.debug("Provided files []", Arrays.toString(filesToUpload.toArray(new File[filesToUpload.size()])));
		}

		if (filesToUpload == null || filesToUpload.size() == 0)
		{
			LOG.info("Throwing an exception because No content was found to upload");
			throw new Exception("No content was found to upload!");
		}
		
		Task<String> uploader = new Task<String>()
		{
			@Override
			protected String call() throws Exception
			{
				updateMessage("Preparing");
				File baseFolder = null;
				try
				{
					baseFolder = Files.createTempDirectory("src-upload").toFile();
					
					//Otherwise, move forward.  Create our native-source folder, and move everything into it.
					File nativeSource = new File(baseFolder, "native-source");
					if (nativeSource.exists())
					{
						LOG.info("Task failing due to unexpected file in upload content '{}'", nativeSource);
						throw new RuntimeException("Unexpected file found in upload content!");
					}
					nativeSource.mkdir();
					
					for (File f : filesToUpload)
					{
						//validate it is a file, move it into native-source
						if (f.isFile())
						{
							Files.move(f.toPath(), nativeSource.toPath().resolve(f.toPath().getFileName()));
						}
						else
						{
							LOG.info("Task failing due to unexpected directory in upload content: '{}'", f.getAbsolutePath());
							throw new Exception("Unexpected directory found in upload content!  " + f.getAbsolutePath());
						}
					}
					
					StringBuffer noticeAppend = new StringBuffer();
					HashMap<String, String> pomSwaps = new HashMap<>();
					
					pomSwaps.put("#VERSION#", version);
					pomSwaps.put("#SCM_URL#", GitPublish.constructChangesetRepositoryURL(gitRepositoryURL));
					if (uploadType.getArtifactId().contains("*") && StringUtils.isBlank(extensionName))
					{
						throw new Exception("ExtensionName is required when the upload type artifact id contains a wildcard");
					}
					
					pomSwaps.put("#GROUPID#", uploadType.getSourceUploadGroupId());
					
					String temp = uploadType.getArtifactId();
					if (temp.contains("*"))
					{
						temp = temp.replace("*", extensionName);
					}
					
					pomSwaps.put("#ARTIFACTID#", temp);
					pomSwaps.put("#NAME#", uploadType.getNiceName() + " Source Upload");
					pomSwaps.put("#LICENSE#", uploadType.getLicenseInformation()[0]);  //we only use the first license for source upload
					noticeAppend.append(uploadType.getNoticeInformation()[0]);  //only use the first notice info
					
					String tagWithoutRevNumber = pomSwaps.get("#GROUPID#") + "/" + pomSwaps.get("#ARTIFACTID#") + "/" + pomSwaps.get("#VERSION#");
					LOG.debug("Desired tag (withoutRevNumber): {}", tagWithoutRevNumber);
					
					ArrayList<String> existingTags = GitPublish.readTags(gitRepositoryURL, gitUsername, gitPassword);
					
					if (LOG.isDebugEnabled())
					{
						LOG.debug("Currently Existing tags in '{}': {} ", gitRepositoryURL, Arrays.toString(existingTags.toArray(new String[existingTags.size()])));
					}
					
					int highestBuildRevision = GitPublish.readHighestRevisionNumber(existingTags, tagWithoutRevNumber);
					
					String tag;
					//Fix version number
					if (highestBuildRevision == -1)
					{
						//No tag at all - create without rev number, don't need to change our pomSwaps
						tag = tagWithoutRevNumber;
					}
					else
					{
						//If we are a SNAPSHOT, don't embed a build number, because nexus won't allow the upload, otherwise, embed a rev number
						if (!pomSwaps.get("#VERSION#").endsWith("SNAPSHOT"))
						{
							pomSwaps.put("#VERSION#", pomSwaps.get("#VERSION#") + "-" + (highestBuildRevision + 1));
						}
						tag = tagWithoutRevNumber + "-" + (highestBuildRevision + 1);
					}
					
					LOG.info("Final calculated tag: '{}'", tag);

					pomSwaps.put("#SCM_TAG#", tag);

					FileUtil.writeFile("shared", "LICENSE.txt", baseFolder);
					FileUtil.writeFile("shared", "NOTICE.txt", baseFolder, null, noticeAppend.toString());
					FileUtil.writeFile("srcUploadProjectTemplate", "native-source/DOTgitignore", baseFolder);
					FileUtil.writeFile("srcUploadProjectTemplate", "assembly.xml", baseFolder);
					FileUtil.writeFile("srcUploadProjectTemplate", "pom.xml", baseFolder, pomSwaps, "");
					
					updateTitle("Publishing configuration to Git");
					GitPublish.publish(baseFolder, gitRepositoryURL, gitUsername, gitPassword, tag);
					
					updateTitle("Zipping content");
					LOG.debug("Zipping content");
					
					Zip z = new Zip(pomSwaps.get("#ARTIFACTID#"), pomSwaps.get("#VERSION#"), null, null, new File(baseFolder, "target"), nativeSource, false);
					
					ArrayList<File> toZip = new ArrayList<>();
					for (File f : nativeSource.listFiles())
					{
						if (f.getName().equals(".gitignore"))
						{
							//noop
						}
						else
						{
							toZip.add(f);
						}
					}
					
					z.getStatus().addListener(new ChangeListener<String>()
					{
						@Override
						public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
						{
							updateMessage(newValue);
						}
					});
					z.getTotalWork().add(z.getWorkComplete()).addListener(new ChangeListener<Number>()
					{
						@Override
						public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
						{
							updateProgress(z.getWorkComplete().get(), z.getTotalWork().get());
						}
					});
					
					//This blocks till complete
					File zipFile = z.addFiles(toZip);
					
					LOG.info("Zip complete, publishing to artifact repo {}", artifactRepositoryURL);
					updateTitle("Publishing files to the Artifact Repository");
					
					MavenPublish pm = new MavenPublish(pomSwaps.get("#GROUPID#"), pomSwaps.get("#ARTIFACTID#"), pomSwaps.get("#VERSION#"), 
							new File(baseFolder, "pom.xml"), new File[] {zipFile}, artifactRepositoryURL, repositoryUsername, repositoryPassword);
					
					pm.progressProperty().addListener(new ChangeListener<Number>()
					{
						@Override
						public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
						{
							updateProgress(pm.getWorkDone(), pm.getTotalWork());
						}
					});
					pm.messageProperty().addListener(new ChangeListener<String>()
					{
						@Override
						public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
						{
							updateMessage(newValue);
						}
					});
					
					WorkExecutors.get().getExecutor().execute(pm);
					
					//block till upload complete
					pm.get();
					
					updateTitle("Cleaning Up");
					try
					{
						FileUtil.recursiveDelete(baseFolder);
					}
					catch (Exception e)
					{
						LOG.error("Problem cleaning up temp folder " + baseFolder, e);
					}
					
					updateTitle("Complete");
					return tag;
				}
				catch (Throwable e)
				{
					LOG.error("Unexpected error", e);
					throw new RuntimeException(e);
				}
				finally
				{
					try
					{
						FileUtil.recursiveDelete(baseFolder);
					}
					catch (Exception e)
					{
						LOG.error("Problem cleaning up temp folder " + baseFolder, e);
					}
				}
				
			}
		};
		
		return uploader;
	}
}
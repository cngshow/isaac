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
package gov.vha.isaac.ochre.pombuilder.dbbuilder;

import java.io.File;
import java.nio.file.Files;
import org.apache.maven.pom._4_0.Build;
import org.apache.maven.pom._4_0.Build.Plugins;
import org.apache.maven.pom._4_0.Dependency;
import org.apache.maven.pom._4_0.License;
import org.apache.maven.pom._4_0.Model;
import org.apache.maven.pom._4_0.Model.Dependencies;
import org.apache.maven.pom._4_0.Model.Licenses;
import org.apache.maven.pom._4_0.Model.Properties;
import org.apache.maven.pom._4_0.Parent;
import org.apache.maven.pom._4_0.Plugin;
import org.apache.maven.pom._4_0.Plugin.Executions;
import org.apache.maven.pom._4_0.PluginExecution;
import org.apache.maven.pom._4_0.PluginExecution.Configuration;
import org.apache.maven.pom._4_0.PluginExecution.Configuration.IbdfFiles;
import org.apache.maven.pom._4_0.PluginExecution.Goals;
import org.apache.maven.pom._4_0.Scm;
import gov.vha.isaac.ochre.pombuilder.FileUtil;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.VersionFinder;
import gov.vha.isaac.ochre.pombuilder.artifacts.IBDFFile;

/**
 * 
 * {@link DBConfigurationCreator}
 * Create a new maven pom project which when executed, will input a set if IBDF files, and build them into a runnable database for ISAAC systems.
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class DBConfigurationCreator
{
	private static final String parentGroupId = "gov.vha.isaac.ochre.modules";
	private static final String parentArtifactId = "db-builder";
	private static final String parentVersion = VersionFinder.findProjectVersion();
	private static final String groupId = "gov.vha.isaac.db";
	
	/**
	 * Construct a new DB builder project which is executable via maven.
	 * @param name - The name to use for the maven artifact that will result from executing this generated pom file.
	 * @param version - The version to use for the maven artifact that will result from executing this generated pom file.
	 * @param description - Describe the purpose / contents of the database being constructed
	 * @param resultClassifier - The (optional) maven classifer to use for the maven artifact that will result from executing this generated pom file.
	 * @param classify - True to classify the content with the snorocket classifer as part of the database build, false to skip classification.
	 * @param ibdfFiles - The set of IBDF files to be included in the DB.  Do not include the metadata IBDF file from ISAAC, it is always included.
	 * @param metadataVersion - The version of the metadata content to include in the DB
	 * @param gitRepositoryURL - The URL to publish this built project to
	 * @param gitUsername - The username to utilize to publish this project
	 * @param getPassword - the password to utilize to publish this project
	 * @return the tag created in the repository that carries the created project
	 * @throws Exception 
	 */
	public static String createDBConfiguration(String name, String version, String description,  String resultClassifier, boolean classify, 
			IBDFFile[] ibdfFiles, String metadataVersion, String gitRepositoryURL, String gitUsername, String gitPassword) throws Exception
	{
		Model model = new Model();
		
		model.setModelVersion("4.0.0");
		
		Parent parent = new Parent();
		parent.setGroupId(parentGroupId);
		parent.setArtifactId(parentArtifactId);
		parent.setVersion(parentVersion);
		model.setParent(parent);
		
		model.setGroupId(groupId);
		model.setArtifactId(name);
		model.setVersion(version);
		model.setName(parentArtifactId + ": " + name);
		model.setPackaging("pom");
		
		model.setDescription(description);

		Scm scm = new Scm();
		scm.setUrl(gitRepositoryURL);
		scm.setTag(groupId + "/" + name + "/" + version);
		model.setScm(scm);
		
		Properties properties = new Properties();
		properties.setInParent("false");
		if (resultClassifier != null && resultClassifier.length() > 0)
		{
			properties.setResultArtifactClassifier(resultClassifier);
		}
		model.setProperties(properties);
		
		Licenses licenses = new Licenses();
		
		License l = new License();
		l.setName("The Apache Software License, Version 2.0");
		l.setUrl("http://www.apache.org/licenses/LICENSE-2.0.txt");
		l.setDistribution("repo");
		l.setComments("Copyright Notice\n" 
				+ "                This is a work of the U.S. Government and is not subject to copyright\n"
				+ "                protection in the United States. Foreign copyrights may apply.");
		
		licenses.getLicense().add(l);
		
		//TODO extract licenses from IBDF file(s), include here.
		
		model.setLicenses(licenses);
		
		Dependencies dependencies = new Dependencies();
		
		Dependency dependency = new Dependency();
		dependency.setGroupId("gov.vha.isaac.ochre.modules");
		dependency.setArtifactId("metadata");
		dependency.setClassifier("all");
		dependency.setVersion(metadataVersion);
		dependency.setType("ibdf.zip");
		dependency.setOptional(true);
		dependencies.getDependency().add(dependency);
		
		for (IBDFFile ibdf : ibdfFiles)
		{
			dependency = new Dependency();
			dependency.setGroupId(ibdf.getGroupId());
			dependency.setArtifactId(ibdf.getArtifactId());
			dependency.setVersion(ibdf.getVersion());
			if (ibdf.hasClassifier())
			{
				dependency.setClassifier(ibdf.hasClassifier() ? ibdf.getClassifier() : "");
			}
			dependency.setType("ibdf.zip");
			dependency.setOptional(true);
			dependency.setScope("compile");
			dependencies.getDependency().add(dependency);
		}
		
		model.setDependencies(dependencies);

		Build build = new Build();
		Plugins plugins = new Plugins();
		
		Plugin plugin = new Plugin();
		
		plugin.setGroupId("org.apache.maven.plugins");
		plugin.setArtifactId("maven-dependency-plugin");
		Executions executions = new Executions();
		
		//Extract dependencies
		
		PluginExecution pe = new PluginExecution();
		pe.setId("extract-ibdf");
		pe.setPhase("generate-resources");
		Goals goals = new Goals();
		goals.getGoal().add("unpack-dependencies");
		pe.setGoals(goals);
		Configuration configuration = new Configuration();
		StringBuilder sb = new StringBuilder();
		for (IBDFFile ibdf : ibdfFiles)
		{
			sb.append(ibdf.getArtifactId());
			sb.append(",");
		}
		sb.append("metadata");
		configuration.setIncludeArtifactIds(sb.toString());
		configuration.setOutputDirectory("${project.build.directory}/data");
		
		pe.setConfiguration(configuration);
		
		executions.getExecution().add(pe);
		plugin.setExecutions(executions);
		plugins.getPlugin().add(plugin);
		
		//new plugin
		plugin = new Plugin();
		plugin.setGroupId("gov.vha.isaac.ochre.modules");
		plugin.setArtifactId("ochre-mojo");
		executions = new Executions();
		
		//setup isaac
		pe = new PluginExecution();
		pe.setId("setup-isaac");
		goals = new Goals();
		goals.getGoal().add("setup-isaac");
		goals.getGoal().add("count-concepts");
		pe.setGoals(goals);
		configuration = new Configuration();
		configuration.setDataStoreLocation("${project.build.directory}/${project.build.finalName}${resultArtifactClassifierWithLeadingHyphen}.data/");
		pe.setConfiguration(configuration);
		executions.getExecution().add(pe);
		
		//load termstore
		pe = new PluginExecution();
		pe.setId("load-termstore");
		goals = new Goals();
		goals.getGoal().add("load-termstore");
		pe.setGoals(goals);
		configuration = new Configuration();
		IbdfFiles files = new IbdfFiles();
		for (IBDFFile ibdf : ibdfFiles)
		{
			files.getIbdfFile().add("${project.build.directory}/data/" + ibdf.getArtifactId() + (ibdf.hasClassifier() ? "-" + ibdf.getClassifier() : "") + ".ibdf");
		}
		files.getIbdfFile().add("${project.build.directory}/data/IsaacMetadataAuxiliary.ibdf");
		configuration.setIbdfFiles(files);
		pe.setConfiguration(configuration);
		executions.getExecution().add(pe);
		
		//count
		pe = new PluginExecution();
		pe.setId("count-after-load");
		goals = new Goals();
		goals.getGoal().add("count-concepts");
		pe.setGoals(goals);
		executions.getExecution().add(pe);
		
		//classify
		if (classify)
		{
			pe = new PluginExecution();
			pe.setId("classify");
			goals = new Goals();
			goals.getGoal().add("quasi-mojo-executor");
			pe.setGoals(goals);
			configuration = new Configuration();
			configuration.setQuasiMojoName("full-classification");
			pe.setConfiguration(configuration);
			executions.getExecution().add(pe);
		}
		
		//index and shutdown
		pe = new PluginExecution();
		pe.setId("index-and-shutdown");
		goals = new Goals();
		goals.getGoal().add("index-termstore");
		goals.getGoal().add("stop-heap-ticker");
		goals.getGoal().add("stop-tasks-ticker");
		goals.getGoal().add("shutdown-isaac");
		pe.setGoals(goals);
		executions.getExecution().add(pe);

		plugin.setExecutions(executions);
		plugins.getPlugin().add(plugin);
		
		build.setPlugins(plugins);
		model.setBuild(build);

		File f = Files.createTempDirectory("db-builder").toFile();
		FileUtil.writePomFile(model, f);

		FileUtil.writeFile("dbProjectTemplate", "DOTgitattributes", f);
		FileUtil.writeFile("dbProjectTemplate", "DOTgitignore", f);
		FileUtil.writeFile("shared", "LICENSE.txt", f);
		FileUtil.writeFile("shared", "NOTICE.txt", f);
		FileUtil.writeFile("dbProjectTemplate", "src/assembly/cradle.xml", f);
		FileUtil.writeFile("dbProjectTemplate", "src/assembly/lucene.xml", f);
		FileUtil.writeFile("dbProjectTemplate", "src/assembly/MANIFEST.MF", f);
		
		GitPublish.publish(f, gitRepositoryURL, gitUsername, gitPassword, scm.getTag());
		return scm.getTag();
	}
}
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
package gov.vha.isaac.ochre.pombuilder.dbdiff;

import java.io.File;
import java.nio.file.Files;

import javax.validation.constraints.NotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.pom._4_0.Build;
import org.apache.maven.pom._4_0.Build.Plugins;
import org.apache.maven.pom._4_0.BuildBase;
import org.apache.maven.pom._4_0.Dependency;
import org.apache.maven.pom._4_0.License;
import org.apache.maven.pom._4_0.Model;
import org.apache.maven.pom._4_0.Model.Dependencies;
import org.apache.maven.pom._4_0.Model.Licenses;
import org.apache.maven.pom._4_0.Model.Profiles;
import org.apache.maven.pom._4_0.Model.Properties;
import org.apache.maven.pom._4_0.Parent;
import org.apache.maven.pom._4_0.Plugin;
import org.apache.maven.pom._4_0.Plugin.Executions;
import org.apache.maven.pom._4_0.PluginExecution;
import org.apache.maven.pom._4_0.PluginExecution.Configuration;
import org.apache.maven.pom._4_0.PluginExecution.Configuration.ArtifactItems;
import org.apache.maven.pom._4_0.PluginExecution.Configuration.Artifacts;
import org.apache.maven.pom._4_0.PluginExecution.Configuration.Descriptors;
import org.apache.maven.pom._4_0.PluginExecution.Configuration.Parameters;
import org.apache.maven.pom._4_0.PluginExecution.Configuration.Target;
import org.apache.maven.pom._4_0.PluginExecution.Configuration.Target.EchoProperties;
import org.apache.maven.pom._4_0.PluginExecution.Goals;
import org.apache.maven.pom._4_0.PluginManagement;
import org.apache.maven.pom._4_0.Profile;
import org.apache.maven.pom._4_0.Scm;

import gov.vha.isaac.ochre.pombuilder.FileUtil;
import gov.vha.isaac.ochre.pombuilder.GitProperties;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.VersionFinder;

/**
 * 
 * {@link IBDFDifferConfigurationCreator} [TODO: Explain]
 * 
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public class IBDFDifferConfigurationCreator {
	private static final Logger LOG = LogManager.getLogger();

	private static final String parentGroupId = "gov.vha.isaac.ochre";
	private static final String parentArtifactId = "isaac-parent";
	private static final String parentVersion = VersionFinder.findProjectVersion();
	public static final String groupId = "gov.vha.isaac.db";

	/**
	 * Construct a new DB diff builder project which is executable via maven.
	 * 
	 * @param ibdfDifferProperties
	 *            - Compare two similar databases of different versions and
	 *            produce a delta database file.
	 * @param gitProperties
	 *            - Connection settings to maven repository.
	 * @return the tag created in the repository that carries the created
	 *         project
	 * @throws Exception
	 */
	public static String createDBDiffConfiguration(@NotNull IBDFDifferProperties ibdfDifferProperties,
			@NotNull GitProperties gitProperties) throws Exception {

		LOG.info(
				"Creating a db differ configuration for \n\tNEW : GroupId {}, ArtifactId {}, Version {}, Loader {}\n\tBASE: GroupId {}, ArtifactId {}, Version {}, Loader {}",
				ibdfDifferProperties.getNewGroupId(), ibdfDifferProperties.getNewArtifactId(),
				ibdfDifferProperties.getNewVersion(), ibdfDifferProperties.getNewLoader(),
				ibdfDifferProperties.getBaseGroupId(), ibdfDifferProperties.getBaseArtifactId(),
				ibdfDifferProperties.getBaseVersion(), ibdfDifferProperties.getBaseLoader());

		try {

			Model model = new Model();

			model.setModelVersion("4.0.0");

			Parent parent = new Parent();
			parent.setGroupId(parentGroupId);
			parent.setArtifactId(parentArtifactId);
			parent.setVersion(parentVersion);
			model.setParent(parent);

			model.setGroupId(groupId);
			model.setArtifactId(ibdfDifferProperties.getName());
			model.setVersion(ibdfDifferProperties.getVersion());
			model.setName(parentArtifactId + ": " + ibdfDifferProperties.getName());
			model.setPackaging("pom");
			model.setDescription(ibdfDifferProperties.getDescription());

			Scm scm = new Scm();
			scm.setUrl(GitPublish.constructChangesetRepositoryURL(gitProperties.getGitRepositoryURL()));
			scm.setTag(groupId + "/" + ibdfDifferProperties.getName() + "/" + ibdfDifferProperties.getVersion());
			model.setScm(scm);

			Licenses licenses = new Licenses();
			License l = new License();
			l.setName("The Apache Software License, Version 2.0");
			l.setUrl("http://www.apache.org/licenses/LICENSE-2.0.txt");
			l.setDistribution("repo");
			l.setComments("Copyright Notice\n"
					+ "                This is a work of the U.S. Government and is not subject to copyright\n"
					+ "                protection in the United States. Foreign copyrights may apply.");
			licenses.getLicense().add(l);

			model.setLicenses(licenses);

			Properties properties = new Properties();

			properties.setSourceDataVersion(ibdfDifferProperties.getImportDate());
			properties.setIsaacVersion(ibdfDifferProperties.getVersion());
			properties.setTerminologyGroupId(groupId);
			// new
			properties.setTerminologyArtifactIdNew(ibdfDifferProperties.getNewArtifactId());
			properties.setTerminologyVersionNew(ibdfDifferProperties.getNewVersion());
			properties.setTerminologyLoaderVersionNew(ibdfDifferProperties.getNewLoader());
			properties.setTerminologyNew("${terminology.version.new}-${terminology.loader.version.new}");
			// base
			properties.setTerminologyArtifactIdBase(ibdfDifferProperties.getBaseArtifactId());
			properties.setTerminologyVersionBase(ibdfDifferProperties.getBaseVersion());
			properties.setTerminologyLoaderVersionBase(ibdfDifferProperties.getBaseLoader());
			properties.setTerminologyBase("${terminology.version.base}-${terminology.loader.version.base}");

			properties.setDatabaseGroupId(ibdfDifferProperties.getDbGroupId());
			properties.setDatabaseArtifactId(ibdfDifferProperties.getDbArtifactId());
			properties.setDatabaseVersion(ibdfDifferProperties.getDbVersion());
			properties.setDatabaseClassifier(ibdfDifferProperties.getDbClassifier());
			properties.setIndexType(ibdfDifferProperties.getDbIndexType());

			properties.setProjectBuildSourceEncoding("UTF-8");

			model.setProperties(properties);

			Dependencies dependencies = new Dependencies();

			Dependency dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("ibdf-provider");
			dependency.setVersion("${isaac.version}");
			dependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("ochre-mojo-log-config");
			dependency.setVersion("${isaac.version}");
			dependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("${database.groupId}");
			dependency.setArtifactId("${database.artifactId}");
			dependency.setVersion("${database.version}");
			dependency.setClassifier("${database.classifier}");
			dependency.setType("${database.type}");
			dependencies.getDependency().add(dependency);
			// add once more
			dependencies.getDependency().add(dependency);

			model.setDependencies(dependencies);

			Build build = new Build();
			PluginManagement pluginMgmt = new PluginManagement();
			PluginManagement.Plugins pluginMgmtPlugins = new PluginManagement.Plugins();

			Plugin plugin = new Plugin();
			plugin.setGroupId("gov.vha.isaac.ochre.modules");
			plugin.setArtifactId("ochre-mojo");
			plugin.setVersion("${isaac.version}");

			Plugin.Dependencies pluginDependencies = new Plugin.Dependencies();

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("ochre-mojo-log-config");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("ibdf-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("commit-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("stamp-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("identifier-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("concept-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("sememe-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("coordinate-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("path-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			dependency = new Dependency();
			dependency.setGroupId("gov.vha.isaac.ochre.modules");
			dependency.setArtifactId("logic-provider");
			dependency.setVersion("${isaac.version}");
			pluginDependencies.getDependency().add(dependency);

			plugin.setDependencies(pluginDependencies);
			pluginMgmtPlugins.getPlugin().add(plugin);
			pluginMgmt.setPlugins(pluginMgmtPlugins);

			build.setPluginManagement(pluginMgmt);

			Plugins plugins = new Plugins();
			plugin = new Plugin();
			plugin.setGroupId("org.apache.maven.plugins");
			plugin.setArtifactId("maven-antrun-plugin");

			Executions executions = new Executions();
			PluginExecution pe = new PluginExecution();
			pe.setId("write-pom-properties");
			pe.setPhase("validate");

			Configuration config = new Configuration();
			Target target = new Target();
			EchoProperties echoProperties = new EchoProperties();
			echoProperties.setDestfile("${project.build.directory}/pom.properties");
			target.setEchoProperties(echoProperties);
			config.setTarget(target);

			Goals goals = new Goals();
			goals.getGoal().add("run");
			pe.setGoals(goals);

			executions.getExecution().add(pe);
			plugin.setExecutions(executions);
			plugins.getPlugin().add(plugin);

			plugin = new Plugin();
			plugin.setGroupId("org.apache.maven.plugins");
			plugin.setArtifactId("maven-dependency-plugin");

			executions = new Executions();
			pe = new PluginExecution();
			pe.setId("extract-db-for-test");
			pe.setPhase("generate-test-resources");
			goals = new Goals();
			goals.getGoal().add("unpack");
			pe.setGoals(goals);

			config = new Configuration();
			config.setSkip("${skipTests}");
			config.setIncludeArtifactIds("${database.artifactId}");
			config.setOutputDirectory("${project.build.directory}/db/");

			ArtifactItems artifactItems = new ArtifactItems();
			ArtifactItems.Artifact artifact = new ArtifactItems.Artifact();
			artifact.setGroupId("${database.groupId}");
			artifact.setArtifactId("${database.artifactId}");
			artifact.setVersion("${database.version}");
			artifact.setClassifier("${database.classifier}");
			artifact.setType("${database.type}");
			artifactItems.getArtifact().add(artifact);

			artifact = new ArtifactItems.Artifact();
			artifact.setGroupId("${database.groupId}");
			artifact.setArtifactId("${database.artifactId}");
			artifact.setVersion("${database.version}");
			artifact.setClassifier("${database.classifier}");
			artifact.setType("${index.type}");
			artifactItems.getArtifact().add(artifact);

			config.setArtifactItems(artifactItems);

			pe.setConfiguration(config);
			executions.getExecution().add(pe);
			plugin.setExecutions(executions);
			plugins.getPlugin().add(plugin);

			plugin = new Plugin();
			plugin.setGroupId("org.apache.maven.plugins");
			plugin.setArtifactId("maven-assembly-plugin");
			executions = new Executions();
			pe = new PluginExecution();
			pe.setId("attach-result");
			pe.setPhase("package");
			goals = new Goals();
			goals.getGoal().add("single");
			pe.setGoals(goals);
			config = new Configuration();
			Descriptors descriptors = new Descriptors();
			descriptors.getDescriptor().add("src/assembly/assembly.xml");
			config.setDescriptors(descriptors);
			config.setAttach(false);
			config.setEncoding("UTF-8");
			pe.setConfiguration(config);
			executions.getExecution().add(pe);
			plugin.setExecutions(executions);
			plugins.getPlugin().add(plugin);

			plugin = new Plugin();
			plugin.setGroupId("org.codehaus.mojo");
			plugin.setArtifactId("build-helper-maven-plugin");
			executions = new Executions();
			pe = new PluginExecution();
			pe.setId("attach-artifact");
			goals = new Goals();
			goals.getGoal().add("attach-artifact");
			pe.setGoals(goals);
			config = new Configuration();
			Artifacts artifacts = new Artifacts();
			Artifacts.Artifact a = new Artifacts.Artifact();
			a.setType("ibdf.zip");
			a.setFile("${project.build.directory}/${project.build.finalName}-.ibdf.zip");
			a.setClassifier("${resultArtifactClassifier}");
			artifacts.getArtifact().add(a);
			config.setArtifacts(artifacts);
			pe.setConfiguration(config);
			executions.getExecution().add(pe);
			plugin.setExecutions(executions);
			plugins.getPlugin().add(plugin);

			build.setPlugins(plugins);
			model.setBuild(build);

			Profiles profiles = new Profiles();
			Profile profile = new Profile();
			profile.setId("diff-ibdf");

			BuildBase buildBase = new BuildBase();
			BuildBase.Plugins buildBasePlugins = new BuildBase.Plugins();
			plugin = new Plugin();
			plugin.setGroupId("org.apache.maven.plugins");
			plugin.setArtifactId("maven-dependency-plugin");
			executions = new Executions();
			pe = new PluginExecution();
			pe.setId("extract-db");
			pe.setPhase("generate-resources");
			goals = new Goals();
			goals.getGoal().add("unpack-dependencies");
			pe.setGoals(goals);
			config = new Configuration();
			config.setIncludeArtifactIds("${database.artifactId}");
			config.setOutputDirectory("${project.build.directory}/db");
			pe.setConfiguration(config);
			executions.getExecution().add(pe);
			plugin.setExecutions(executions);
			plugins.getPlugin().add(plugin);

			plugin = new Plugin();
			plugin.setGroupId("gov.vha.isaac.ochre.modules");
			plugin.setArtifactId("ochre-mojo");
			plugin.setVersion("${isaac.version}");
			executions = new Executions();
			pe = new PluginExecution();
			pe.setId("setup");
			pe.setPhase("process-resources");
			goals = new Goals();
			goals.getGoal().add("setup-isaac");
			pe.setGoals(goals);
			config = new Configuration();
			config.setDataStoreLocation(
					"${project.build.directory}/db/${database.artifactId}-${database.version}-${database.classifier}.data/");
			pe.setConfiguration(config);
			executions.getExecution().add(pe);

			pe = new PluginExecution();
			pe.setId("diff-ibdfs");
			pe.setPhase("process-resources");
			goals = new Goals();
			goals.getGoal().add("quasi-mojo-executor");
			pe.setGoals(goals);
			config = new Configuration();
			config.setQuasiMojoName(
					"${project.build.directory}/db/${database.artifactId}-${database.version}-${database.classifier}.data/");

			Parameters params = new Parameters();
			params.setInputAnalysisDir("${project.build.directory}/inputFiles/");
			params.setAnalysisArtifactsDir("${project.build.directory}/comparisonFiles/");
			params.setDeltaIbdfFilePath(
					"${project.build.directory}/${terminology.artifactId.base}-Diff-${terminology.artifactId.new}.ibdf");
			// params.setBaseVersionFile("FIX ibdfDifferProperties");
			// params.setNewVersionFile("FIX ibdfDifferProperties");
			params.setDiffOnAuthor(ibdfDifferProperties.getDiffOnAuthor());
			params.setDiffOnModule(ibdfDifferProperties.getDiffOnModule());
			params.setDiffOnPath(ibdfDifferProperties.getDiffOnPath());
			params.setConverterSourceArtifactVersion("${sourceData.version}");
			params.setGenerateAnalysisFiles(ibdfDifferProperties.getGenerateAnalysisFiles());
			params.setImportDate(ibdfDifferProperties.getImportDate());

			pe.setConfiguration(config);
			executions.getExecution().add(pe);

			pe = new PluginExecution();
			pe.setId("shutdown");
			pe.setPhase("process-resources");
			goals = new Goals();
			goals.getGoal().add("shutdown-isaac");
			pe.setGoals(goals);

			executions.getExecution().add(pe);

			plugin.setExecutions(executions);
			buildBasePlugins.getPlugin().add(plugin);

			buildBase.setPlugins(buildBasePlugins);
			profile.setBuild(buildBase);

			profiles.getProfile().add(profile);

			model.setProfiles(profiles);

			File f = Files.createTempDirectory("ibdf-differ-builder").toFile();
			FileUtil.writePomFile(model, f);

			FileUtil.writeFile("dbProjectTemplate", "DOTgitattributes", f);
			FileUtil.writeFile("dbProjectTemplate", "DOTgitignore", f);
			FileUtil.writeFile("shared", "LICENSE.txt", f);
			FileUtil.writeFile("shared", "NOTICE.txt", f);
			FileUtil.writeFile("dbProjectTemplate", "src/assembly/cradle.xml", f);
			FileUtil.writeFile("dbProjectTemplate", "src/assembly/lucene.xml", f);
			FileUtil.writeFile("dbProjectTemplate", "src/assembly/MANIFEST.MF", f);

			GitPublish.publish(f, gitProperties.getGitRepositoryURL(), gitProperties.getGitUsername(),
					gitProperties.getGitPassword(), scm.getTag());
			String tag = scm.getTag();
			try {
				FileUtil.recursiveDelete(f);
			} catch (Exception e) {
				LOG.error("Problem cleaning up temp folder " + f, e);
			}
			return tag;
		} catch (Exception e) {
			LOG.error("createDBConfiguration failed with ", e);
			throw e;
		}
	}
}
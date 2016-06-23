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
package gov.va.oia.terminology.converters.sharedUtils.config;

import java.io.File;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.mojo.external.QuasiMojo;
import gov.vha.isaac.ochre.pombuilder.converter.ConverterOptionParam;

/**
 * {@link ConfigArtifactCreator}
 * Locate the files on the classpath that document a configuration for a converter, and create the json output 
 * artifacts for them.
 * 
 * This will find any class on the class path that implements {@link ConfigOptionsDescriptor}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service(name = "create-config-artifact")
public class ConfigArtifactCreator extends QuasiMojo
{
	@Override
	public void execute() throws MojoExecutionException
	{
		try
		{
			List<ConfigOptionsDescriptor> configs = LookupService.get().getAllServices(ConfigOptionsDescriptor.class);
			for (ConfigOptionsDescriptor c : configs)
			{
				ConverterOptionParam.serialize(c.getConfigOptions(), new File(outputDirectory, c.getName() + "." + ConverterOptionParam.MAVEN_FILE_TYPE));
			}
			getLog().info("Output Config artifact files for " + configs.size() + " entries found on the classpath.");
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("Unexpected error validating the resources folder", e);
		}
	}
}

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
package test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.constants.Constants;
import gov.vha.isaac.ochre.mojo.IndexTermstore;
import gov.vha.isaac.ochre.mojo.LoadTermstore;
import gov.vha.isaac.ochre.utility.importer.VHATDeltaImport;

/**
 * {@link SimpleTest}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class SimpleTest
{
	public static void main(String[] args) throws MojoExecutionException, IOException
	{
		try
		{
			File db = new File("target/db");
			FileUtils.deleteDirectory(db);
			db.mkdirs();
			System.setProperty(Constants.DATA_STORE_ROOT_LOCATION_PROPERTY, db.getCanonicalPath());
			LookupService.startupIsaac();
			LoadTermstore lt = new LoadTermstore();
			lt.setLog(new SystemStreamLog());
			lt.setibdfFilesFolder(new File("src/test/resources/ibdf/"));
			lt.execute();
			new IndexTermstore().execute();
			VHATDeltaImport i = new VHATDeltaImport(
				//new String(Files.readAllBytes(Paths.get("src/test/resources/VHAT XML Update files/NTRT Allergies Sept 19, 2013.xml"))),
				new String(Files.readAllBytes(Paths.get("src/test/resources/VHAT XML Update files/new domain.xml"))),
				TermAux.USER.getPrimordialUuid(), TermAux.VHAT_EDIT.getPrimordialUuid(), TermAux.DEVELOPMENT_PATH.getPrimordialUuid(), new File("target"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			LookupService.shutdownSystem();
			System.exit(0);
		}
		
	}
}

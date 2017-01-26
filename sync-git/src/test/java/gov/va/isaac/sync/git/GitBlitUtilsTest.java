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
package gov.va.isaac.sync.git;

import org.junit.Assert;
import org.junit.Test;
import gov.va.isaac.sync.git.gitblit.GitBlitUtils;

/**
 * {@link GitBlitUtilsTest}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class GitBlitUtilsTest
{
	@Test
	public void TestURLAdjust() throws Exception
	{
		Assert.assertEquals("https://vaauscttdbs80.aac.va.gov:8080/git/", GitBlitUtils.adjustBareUrlForGitBlit("https://vaauscttdbs80.aac.va.gov:8080/"));
		Assert.assertEquals("https://Vaauscttdbs80.aac.va.gov:8080/git/", GitBlitUtils.adjustBareUrlForGitBlit("https://Vaauscttdbs80.aac.va.gov:8080"));
		Assert.assertEquals("http://vaauscttdbs80.aac.va.gov:8080/git/", GitBlitUtils.adjustBareUrlForGitBlit("http://vaauscttdbs80.aac.va.gov:8080/"));
		Assert.assertEquals("http://vaauscttdbs80.aac.va.gov:8080/git/", GitBlitUtils.adjustBareUrlForGitBlit("http://vaauscttdbs80.aac.va.gov:8080"));
		Assert.assertEquals("https://vaauscttdbs80.aac.va.gov/git/", GitBlitUtils.adjustBareUrlForGitBlit("https://vaauscttdbs80.aac.va.gov/"));
		Assert.assertEquals("https://vaauscttdbs80.aac.va.gov/git/", GitBlitUtils.adjustBareUrlForGitBlit("https://vaauscttdbs80.aac.va.gov"));
		Assert.assertEquals("https://vaauscttdbs80.aac.va.gov/fred/", GitBlitUtils.adjustBareUrlForGitBlit("https://vaauscttdbs80.aac.va.gov/fred"));
		Assert.assertEquals("https://vaauscttdbs80.aac.va.gov:8080/git/", GitBlitUtils.adjustBareUrlForGitBlit("https://vaauscttdbs80.aac.va.gov:8080/git"));
		Assert.assertEquals("https://vaauscttdbs80.aac.va.gov:8080/git/", GitBlitUtils.adjustBareUrlForGitBlit("https://vaauscttdbs80.aac.va.gov:8080/git/"));
		Assert.assertEquals("https://vaauscttdbs80.aac.va.gov:8080/fred/", GitBlitUtils.adjustBareUrlForGitBlit("https://vaauscttdbs80.aac.va.gov:8080/fred"));
		Assert.assertEquals("https://vaauscttdbs80.aac.va.gov:8080/fred/", GitBlitUtils.adjustBareUrlForGitBlit("https://vaauscttdbs80.aac.va.gov:8080/fred/"));
		Assert.assertEquals("HTtps://vaa-uscttdbs8_0.aac.va.gov:8080/git/", GitBlitUtils.adjustBareUrlForGitBlit("HTtps://vaa-uscttdbs8_0.aac.va.gov:8080/"));
	}
	
	@Test
	public void TestBaseURLParse() throws Exception
	{
		Assert.assertEquals("https://vadev.mantech.com:4848/git/", GitBlitUtils.parseBaseRemoteAddress("https://vadev.mantech.com:4848/git/r/db_test.git"));
		Assert.assertEquals("https://vadev.mantech.com:4848/git/", GitBlitUtils.parseBaseRemoteAddress("https://vadev.mantech.com:4848/git/r/db_test.GIT"));
		Assert.assertEquals("Https://vadev.ma-nt_e0ch.com:4848/git/", GitBlitUtils.parseBaseRemoteAddress("Https://vadev.ma-nt_e0ch.com:4848/git/r/db_-test.git"));
	}
	
	
}

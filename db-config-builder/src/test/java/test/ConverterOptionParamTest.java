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
import org.junit.Assert;
import org.junit.Test;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.converter.ConverterOptionParam;
import gov.vha.isaac.ochre.pombuilder.converter.ConverterOptionParamSuggestedValue;

/**
 * {@link ConverterOptionParamTest}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ConverterOptionParamTest
{
	@Test
	public void testJson() throws Exception
	{
		ConverterOptionParam foo = new ConverterOptionParam("cc", "a", "b", true, true, new ConverterOptionParamSuggestedValue("e", "e1"),
				new ConverterOptionParamSuggestedValue("f"));
		Assert.assertEquals("e", foo.getSuggestedPickListValues()[0].getValue());
		Assert.assertEquals("e1", foo.getSuggestedPickListValues()[0].getDescription());
		Assert.assertEquals("f", foo.getSuggestedPickListValues()[1].getValue());
		Assert.assertEquals("f", foo.getSuggestedPickListValues()[1].getDescription());
		ConverterOptionParam foo2 = new ConverterOptionParam("33", "1", "2", true, false, new ConverterOptionParamSuggestedValue("3", "31"),
				new ConverterOptionParamSuggestedValue("4", "41"));
		ConverterOptionParam.serialize(new ConverterOptionParam[] { foo, foo2 }, new File("foo.json"));

		ConverterOptionParam[] foo3 = ConverterOptionParam.fromFile(new File("foo.json"));
		Assert.assertEquals(foo3[0], foo);
		Assert.assertEquals(foo3[1], foo2);
		new File("foo.json").delete();
	}

	@Test
	public void testChangesetURLRewrite() throws IOException
	{
		Assert.assertEquals("https://vadev.mantech.com:4848/git/r/contentConfigurations.git",
				GitPublish.constructChangesetRepositoryURL("https://vadev.mantech.com:4848/git/"));
		Assert.assertEquals("https://vadev.mantech.com:4848/git/r/contentConfigurations.git",
				GitPublish.constructChangesetRepositoryURL("https://vadev.mantech.com:4848/git"));
		Assert.assertEquals("http://vadev.mantech.com:4848/git/r/contentConfigurations.git",
				GitPublish.constructChangesetRepositoryURL("http://vadev.mantech.com:4848/git/"));
		Assert.assertEquals("https://vadev.mantech.com:4848/git/r/contentConfigurations.git",
				GitPublish.constructChangesetRepositoryURL("https://vadev.mantech.com:4848/git/r/contentConfigurations.git"));
		Assert.assertEquals("https://vadev.mantech.com:4848/git/r/foo.git", GitPublish.constructChangesetRepositoryURL("https://vadev.mantech.com:4848/git/r/foo.git"));
	}
}

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
package gov.va.isaac.util;

import org.junit.Assert;
import org.junit.Test;
import gov.vha.isaac.ochre.api.util.ChecksumGenerator;

/**
 * {@link ChecksumGeneratorTest}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ChecksumGeneratorTest
{
	@Test
	public void checksumTestOne() throws Exception
	{
		String hash = ChecksumGenerator.calculateChecksum("MD5", "Some random data".getBytes());
		Assert.assertTrue(hash.equals("b08f254d76b1c6a7ad924708c0032251"));
	}
	
	@Test
	public void checksumTestTwo() throws Exception
	{
		String hash = ChecksumGenerator.calculateChecksum("SHA1", "Some random data".getBytes());
		Assert.assertTrue(hash.equals("3b0af1dd47d543b2166440b83bbf0ed0235173d8"));
	}
}

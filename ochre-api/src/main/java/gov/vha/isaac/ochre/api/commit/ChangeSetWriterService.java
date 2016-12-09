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
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.commit;

import org.jvnet.hk2.annotations.Contract;

/**
 * {@link ChangeSetWriterService}
 *
 * Any @Service annotated class which implements this interface will get the notifications below, when
 * index events happen.
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */

@Contract
public interface ChangeSetWriterService {


	/**
	 * Disable {@link ChangeSetWriterService} from writing.  Anything received
	 * while service is disabled is skipped.
	 *
	 */
	public void disable ();

	/**
	 * Enable {@link ChangeSetWriterService} to write.
	 *
	 */
	public void enable();

	/**
	 * Determine if the writer in the service is disabled or enabled for writing.
	 *
	 * @return {@code true} if enabled or {@code false} if disabled.
	 */
	public boolean getWriteStatus();

}
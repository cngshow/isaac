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

import java.util.UUID;

/**
 * {@link ChangeSetListener}
 *
 * Any @service annotated class which implements this interface will get notifications of the
 * commit record after its commit.
 *
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 */
public interface ChangeSetListener {

	/**
	 *
	 * @return a unique UUID for this listener.
	 */
	UUID getListenerUuid();

	/**
	 * Don't do work on or block the calling thread.
	 * @param commitRecord a record of post successful commit.
	 */
	void handlePostCommit(CommitRecord commitRecord);

}
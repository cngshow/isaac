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

package gov.vha.isaac.ochre.modules.vhat;

import org.jvnet.hk2.annotations.Contract;

import gov.vha.isaac.ochre.api.commit.ChronologyChangeListener;

/**
 * 
 * {@link VHATIsAHasParentSynchronizingChronologyChangeListenerI}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@Contract
public interface VHATIsAHasParentSynchronizingChronologyChangeListenerI extends ChronologyChangeListener {
	/**
	 * Allow clients to exempt and unexempt specified components from handling by the listener,
	 * presumably because the client is creating all necessary component changes itself
	 * 
	 * @param nids
	 */
	void addNidsOfGeneratedSememesToIgnore(int... nids);
	void removeNidsOfGeneratedSememesToIgnore(int... nids);

	/**
	 * Call to ensure that all background processing completed before continuing
	 */
	void waitForJobsToComplete();
}
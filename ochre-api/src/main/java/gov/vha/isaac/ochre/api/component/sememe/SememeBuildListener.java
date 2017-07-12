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

package gov.vha.isaac.ochre.api.component.sememe;

/**
 * 
 * {@link SememeBuildListener}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public abstract class SememeBuildListener implements SememeBuildListenerI {
	private boolean enabled = true;

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.component.sememe.SememeBuildListenerI#getListenerName()
	 */
	@Override
	public String getListenerName() {
		return getClass().getSimpleName();
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.component.sememe.SememeBuildListenerI#enable()
	 */
	@Override
	public void enable() {
		enabled = true;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.component.sememe.SememeBuildListenerI#disable()
	 */
	@Override
	public void disable() {
		enabled = false;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.component.sememe.SememeBuildListenerI#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return enabled;
	}
}

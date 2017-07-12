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

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuildListener;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.impl.utility.Frills;

/**
 * 
 * {@link VHATIsAHasASynchronizingSememeBuildListener}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@Service(name = "VHATIsAHasASynchronizingSememeBuildListener")
@RunLevel(value = LookupService.SL_L3)
public class VHATIsAHasASynchronizingSememeBuildListener extends SememeBuildListener {
	private static final Logger LOG = LogManager.getLogger(VHATIsAHasASynchronizingSememeBuildListener.class);

	// Cached VHAT module sequences
	private static Set<Integer> VHAT_MODULES = null;
	
	VHATIsAHasASynchronizingSememeBuildListener() {
		// Initialize VHAT module sequences cache
		synchronized(VHATIsAHasASynchronizingSememeBuildListener.class) {
			if (VHAT_MODULES == null) { // Should be unnecessary
				VHAT_MODULES = Frills.getAllChildrenOfConcept(MetaData.VHAT_MODULES.getConceptSequence(), true, true);
			}
		}
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.component.sememe.SememeBuildListenerI#applyAfter(gov.vha.isaac.ochre.api.coordinate.EditCoordinate, gov.vha.isaac.ochre.api.commit.ChangeCheckerMode, gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion, java.util.List)
	 */
	@Override
	public void applyAfter(EditCoordinate editCoordinate, ChangeCheckerMode changeCheckerMode,
			SememeVersion<?> builtSememeVersion, List<ObjectChronology<? extends StampedVersion>> builtObjects) {

		// Only apply to edits to cached VHAT modules
		if (VHAT_MODULES.contains(editCoordinate.getModuleSequence())) {
			LOG.debug("Running " + getListenerName() + " applyAfter()...");
			super.applyAfter(editCoordinate, changeCheckerMode, builtSememeVersion, builtObjects);
		}
	}
}

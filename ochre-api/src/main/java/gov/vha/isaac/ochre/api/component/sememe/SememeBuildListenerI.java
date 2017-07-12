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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.component.sememe;

import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.identity.StampedVersion;



/**
 * {@link SememeBuildListenerI}
 *
 * An interface that allows implementers to easily mark their listener as something that will register 
 * itself as a class that does things on build of a sememe.
 * 
 * The only intent of this interface is to provide a global handle to all modules which are registered to do 
 * things - so that then can be enabled or disabled individually, or across the board prior to performing 
 * programmatic operations where you don't want the listeners firing.
 * 
 * =====================================
 * Implementers of this class MUST use a @Named annotation:
 * <code>@Named (value="the name")</code>
 * =====================================
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a> 
 */

@Contract
public interface SememeBuildListenerI
{
	/**
	 * Returns the HK2 service name of this listener.  This should return the same string as the 
	 * <code>@Named (value="the name")</code>
	 * annotation on the implementation of the class.
	 */
	public String getListenerName();
	
	/**
	 * Tell the listener that it MAY enable itself (it does not have to).  A listener may be primarily controlled
	 * by a user preference, for example.  While {@link #disable()} overrides the user preferences, and orders the 
	 * listener disabled - this call only informs the user that it is no longer being overridden - and may return to 
	 * the enabled state if that was the users preference.
	 */
	public void enable();
	
	/**
	 * Tell the listener to disable itself - either by unregistering upstream, or ignoring all commit related events 
	 * until {@link #enable()} is called.
	 */
	public void disable();

	
	/**
	 * Query whether or not the listener is enabled
	 * @return boolean indicating whether or not the listener is enabled
	 */
	public boolean isEnabled();

	/**
	 * The caller is responsible to write the component to the proper store when 
	 * all updates to the component are complete. 
	 * @param stampSequence
	 * @param builtObjects a list objects build as a result of call build. 
	 * Includes top-level object being built. 
	 * The caller is also responsible to write all build objects to the proper store. 
	 * @return 
	 */
	default void applyBefore(int stampSequence, List<ObjectChronology<? extends StampedVersion>> builtObjects) {}


	/**
	 * The caller is responsible to write the component to the proper store when 
	 * all updates to the component are complete. 
	 * @param stampSequence
	 * @param builtSememe sememe built as a result of building this object
	 * @param builtObjects a list objects build as a result of call build. 
	 * Includes top-level object being built. 
	 * The caller is also responsible to write all build objects to the proper store. 
	 * @return 
	 */
	default void applyAfter(int stampSequence, SememeVersion<?> builtSememe, List<ObjectChronology<? extends StampedVersion>> builtObjects) {}

	/**
	 * A listener method that applies to a SememeBuilder before building a component with a state of ACTIVE. 
	 * @param editCoordinate the edit coordinate that determines the author, module and path for the change
	 * @param changeCheckerMode determines if added to the commit manager with or without checks. 
	 * @param subordinateBuiltObjects a list of subordinate objects also build as a result of building this object.  Includes top-level object being built. 
	 * @return
	 */
	default public void applyBefore(
			EditCoordinate editCoordinate, 
			ChangeCheckerMode changeCheckerMode,
			List<ObjectChronology<? extends StampedVersion>> builtObjects) {}

	/**
	 * A listener method that applies to a SememeBuilder after building a component with a state of ACTIVE. 
	 * @param editCoordinate the edit coordinate that determines the author, module and path for the change
	 * @param changeCheckerMode determines if added to the commit manager with or without checks. 
	 * @param builtSememe sememe built as a result of building this object 
	 * @param subordinateBuiltObjects a list of subordinate objects also build as a result of building this object.  Includes top-level object being built. 
	 * @return
	 */
	default public void applyAfter(
			EditCoordinate editCoordinate, 
			ChangeCheckerMode changeCheckerMode,
			SememeVersion<?> builtSememeVersion,
			List<ObjectChronology<? extends StampedVersion>> builtObjects) {}
}

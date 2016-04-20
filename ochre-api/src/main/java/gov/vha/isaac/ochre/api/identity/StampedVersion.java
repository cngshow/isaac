/*
 * Copyright 2015 kec.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api.identity;

import gov.vha.isaac.ochre.api.State;

/**
 * The chronicle objects use {@code StampedVersion} objects to
 * provide a means to generically represent the revisions to a component over time,
 * and to index those revisions by state (active, inactive),
 * effective time of change,
 * author of change, module within which the change occurred (international edition, US extension, etc.),
 * and the development path of the change (development, release candidate, etc.).
 * Taken together, these fields can be referred to as a versions STAMP
 * (state, time, author, module, and path).
 * The version STAMP provides a foundation for version control and
 * configuration management of all the components of the informatics architecture.
 * 
 * @author kec
 */
public interface StampedVersion
{

	/**
	 * A sequence starting at one that uniquely identifies a particular combination of
	 * state, time, author, module, and path. Stamp sequences are idempotent, and there will be
	 * only one stamp sequence for any unique combination of values.
	 * 
	 * @return the stamp sequence of this version.
	 */
	int getStampSequence();

	/**
	 *
	 * @return the state of this version.
	 */
	State getState();

	/**
	 * Long.MIN_VALUE indicates a canceled transaction. Long.MAX_VALUE indicates an
	 * uncommitted transaction.
	 * 
	 * @return the commit time of this version measured in milliseconds,
	 * between the current time and midnight, January 1, 1970 UTC.
	 */
	long getTime();

	/**
	 *
	 * @return the sequence of the concept that represents the author who committed this version.
	 */
	int getAuthorSequence();

	/**
	 * Modules are analogous to OSGI modules, where they represent a collection of components
	 * sufficient to accomplish a task, when taken together with the transitive module dependencies.
	 * 
	 * @return the sequence of the concept that represents the module this version is part of.
	 */
	int getModuleSequence();

	/**
	 *
	 * @return the sequence of the concept that represents the path on which this version is committed.
	 */

	int getPathSequence();
}

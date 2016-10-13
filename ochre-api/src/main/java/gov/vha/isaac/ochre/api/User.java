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

package gov.vha.isaac.ochre.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 
 * {@link User}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class User {
	private final String name;
	private final UUID id;
	private final Set<UserRole> roles = new HashSet<>();
	
	public User(String name, UUID id, Collection<UserRole> roles) {
		this.name = name;
		this.id = id;
		if (roles != null) {
			this.roles.addAll(roles);
		}
	}
	
	/**
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * @return
	 */
	public UUID getId() {
		return id;
	}
	/**
	 * @return
	 */
	public Set<UserRole> getRoles() {
		return Collections.unmodifiableSet(roles);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "User [name=" + name + ", id=" + id + ", roles=" + roles + "]";
	}
}
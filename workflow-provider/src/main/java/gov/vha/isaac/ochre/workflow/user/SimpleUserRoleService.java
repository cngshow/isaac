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
package gov.vha.isaac.ochre.workflow.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Singleton;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.UserRoleService;

/**
 * A simple implementation of a role service that can be manually configured, for test (or any other) purpose.
 * It has no storage - must be manually configured with non-interface methods
 *
 * {@link SimpleUserRoleService}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Service(name="simpleUserRoleService")
@Rank(value = -50)
@Singleton
public class SimpleUserRoleService implements UserRoleService {
	
	/**  The user role map  (for Unit Testing) */
	private Map<UUID, Set<String>> userRoleMap = new HashMap<>();

	/**  The definition roles. */
	private Set<String> definitionRoles = new HashSet<>();

	/**
	 * Defines the user roles for the Mock case 
	 */
	private SimpleUserRoleService() {
		//For HK2 to construct
	}

	/**
	 * 
	 * @see gov.vha.isaac.ochre.api.UserRoleService#getUserRoles(java.util.UUID)
	 */
	@Override
	public Set<String> getUserRoles(UUID userId) {
		return userRoleMap.get(userId);
	}

	/**
	 * 
	 * @see gov.vha.isaac.ochre.api.UserRoleService#getAllUserRoles()
	 */
	@Override
	public Set<String> getAllUserRoles() {
		return definitionRoles;
	}
	
	public void addRole(String roleName)
	{
		definitionRoles.add(roleName);
	}
	
	public void addUser(UUID user, Set<String> roles)
	{
		userRoleMap.put(user, roles);
	}
}

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

import gov.vha.isaac.ochre.api.User;
import gov.vha.isaac.ochre.api.PrismeRole;
import gov.vha.isaac.ochre.api.UserRoleService;

/**
 * A simple implementation of a role service that can be manually configured, for test (or any other) purpose.
 * It has no storage - must be manually configured with non-interface methods
 * 
 * This only exists in a non-test package, so it can also be used by integration-tests
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
	private Map<UUID, User> userMap = new HashMap<>();

	/**  The definition roles. */
	private Set<PrismeRole> definitionRoles = new HashSet<>();

	/**
	 * Defines the user roles for the Mock case 
	 */
	private SimpleUserRoleService() {
		//For HK2 to construct
	}

	/**
	 * 
	 * @see gov.vha.isaac.ochre.api.UserRoleService#getAllPossibleUserRoles()
	 */
	@Override
	public Set<PrismeRole> getAllPossibleUserRoles() {
		return definitionRoles;
	}
	
	public void addRole(PrismeRole roleName)
	{
		definitionRoles.add(roleName);
	}
	
	public void addUser(User user)
	{
		userMap.put(user.getId(), user);
	}

	@Override
	public User getUser(UUID userId) {
		return userMap.get(userId);
	}
}

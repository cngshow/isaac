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

import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.api.UserRole;
import gov.vha.isaac.ochre.api.UserRoleService;

/**
 * The Class MockUserRoleService.
 *
 * {@link MockUserRoleService}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Service
@Rank(value = -50)
public class MockUserRoleService implements UserRoleService {
	
	/**  The Constant firstUserId (for Unit Testing) */
	protected static final UUID firstUserId = UUID.randomUUID();
	
	/**  The Constant secondUserId (for Unit Testing) */
	protected static final UUID secondUserId = UUID.randomUUID();
	
	/**  The Constant fullRoleUserId  (for Integration Testing) */
	protected static final UUID fullRoleUserId = UUID.randomUUID();
	
	/**  The Constant restTestingUserId (for REST Testing) */
	protected static final UUID restTestingUserId = UUID.fromString("85af9e52-8cce-11e6-ae22-56b6b6499611");
	
	/**  The user role map  (for Unit Testing) */
	Map<UUID, Set<UserRole>> userRoleMap = new HashMap<>();

	/**  The definition roles. */
	Set<UserRole> definitionRoles = new HashSet<>();

	/**
	 * Defines the user roles for the Mock case 
	 */
	public MockUserRoleService() {
		definitionRoles.add(UserRole.EDITOR);
		definitionRoles.add(UserRole.REVIEWER);
		definitionRoles.add(UserRole.APPROVER);
		definitionRoles.add(UserRole.AUTOMATED);

		// Setup User Role Maps
		userRoleMap.put(firstUserId, new HashSet<>());
		userRoleMap.get(firstUserId).add(UserRole.EDITOR);
		userRoleMap.get(firstUserId).add(UserRole.APPROVER);

		userRoleMap.put(secondUserId, new HashSet<>());
		userRoleMap.get(secondUserId).add(UserRole.REVIEWER);

		userRoleMap.put(fullRoleUserId, new HashSet<>());
		userRoleMap.get(fullRoleUserId).add(UserRole.EDITOR);
		userRoleMap.get(fullRoleUserId).add(UserRole.REVIEWER);
		userRoleMap.get(fullRoleUserId).add(UserRole.APPROVER);

    	userRoleMap.put(restTestingUserId, new HashSet<>());
    	userRoleMap.get(restTestingUserId).add(UserRole.EDITOR);
    	userRoleMap.get(restTestingUserId).add(UserRole.REVIEWER);
    	userRoleMap.get(restTestingUserId).add(UserRole.APPROVER);
	}

	/* see superclass */
	@Override
	public Set<UserRole> getUserRoles(UUID userId) {
		return userRoleMap.get(userId);
	}

	/* see superclass */
	@Override
	public Set<UserRole> getAllUserRoles() {
		return definitionRoles;
	}

	/**
	 * Returns the first test user (for Unit Testing)
	 *
	 * @return the first test user
	 */
	public static UUID getFirstTestUser() {
		return firstUserId;
	}

	/**
	 * Returns the second test user (for Unit Testing)
	 *
	 * @return the second test user
	 */

	public static UUID getSecondTestUser() {
		return secondUserId;
	}

	/**
	 * Returns the full role test user (for Integration Testing)
	 *
	 * @return the full role test user
	 */
	/*
	 * For Integration Test
	 */
	public static UUID getFullRoleTestUser() {
		return fullRoleUserId;
	}

	/**
	 * Returns the rest testing user id  (for REST Testing)
	 *
	 * @return the rest testing user id
	 */
	/*
	 * For Integration Test
	 */
	public static UUID getRestTestingUserId() {
		return restTestingUserId;
	}

	/**
	 * Returns the first test user seq (Value doesn't matter as long as consistent)
	 *
	 * @return the first test user seq
	 */
	public static int getFirstTestUserSeq() {
		return 1;
	}

	/**
	 * Returns the second test user seq (Value doesn't matter as long as consistent)
	 *
	 * @return the second test user seq
	 */
	public static int getSecondTestUserSeq() {
		return 2;
	}
}

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
package gov.vha.isaac.ochre.workflow.provider.user;

import java.util.HashSet;
import java.util.UUID;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.workflow.provider.BPMNInfo;
import gov.vha.isaac.ochre.workflow.user.SimpleUserRoleService;

/**
 * {@link RoleConfigurator}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class RoleConfigurator
{
	/**  The Constant firstUserId (for Unit Testing) */
	private static final UUID firstUserId = UUID.randomUUID();
	
	/**  The Constant secondUserId (for Unit Testing) */
	private static final UUID secondUserId = UUID.randomUUID();
	
	
	public static void configureForTest()
	{
		SimpleUserRoleService rolesService = LookupService.get().getService(SimpleUserRoleService.class);
		rolesService.addRole("Editor");
		rolesService.addRole("Reviewer");
		rolesService.addRole("Approver");
		rolesService.addRole(BPMNInfo.AUTOMATED_ROLE);

		// Setup User Role Maps
		HashSet<String> roles = new HashSet<>();
		roles.add("Editor");
		roles.add("Approver");
		rolesService.addUser(firstUserId, roles);

		roles = new HashSet<>();
		roles.add("Reviewer");
		rolesService.addUser(secondUserId, roles);
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

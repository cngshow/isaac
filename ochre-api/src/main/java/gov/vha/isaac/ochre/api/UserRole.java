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

import java.util.Optional;

/**
 * 
 * {@link UserRole}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public enum UserRole {
	// Do not rearrange. Add new values to end.

	/*
	 * AUTOMATED is used to capture that the system automated the workflow
	 * advancement rather than a specific user
	 */
	AUTOMATED(UserRoleConstants.AUTOMATED),

	SUPER_USER(UserRoleConstants.SUPER_USER),
	ADMINISTRATOR(UserRoleConstants.ADMINISTRATOR),

	READ_ONLY(UserRoleConstants.READ_ONLY),
	EDITOR(UserRoleConstants.EDITOR),
	REVIEWER(UserRoleConstants.REVIEWER),
	APPROVER(UserRoleConstants.APPROVER),
	MANAGER(UserRoleConstants.MANAGER),
	VUID_REQUESTOR(UserRoleConstants.VUID_REQUESTOR);
	
	private final String text;
	private UserRole(String text) { this.text = text; }
	
	public String getText() { return text; }
	
	public static Optional<UserRole> safeValueOf(String str) {
		for (UserRole role : UserRole.values()) {
			if (role.getText().equalsIgnoreCase(str)) {
				return Optional.of(role);
			}
		}
		
		return Optional.empty();
	}
	public static Optional<UserRole> safeValueOf(int ord) {
		for (UserRole role : UserRole.values()) {
			if (role.ordinal() == ord) {
				return Optional.of(role);
			}
		}
		
		return Optional.empty();
	}
	
	public String toString() {
		return text;
	}
}

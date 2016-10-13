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

import java.util.Set;
import java.util.UUID;

import org.jvnet.hk2.annotations.Contract;

/**
 * The Interface UserRoleService. The service is used for accessing the
 * roles available to users. They can be defined in prism or hard coded for
 * testing
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@Contract
public interface UserRoleService {
	/**
	 * Return all user roles available for a given user
	 *
	 * @param userId
	 *            the user's id represented as a UUID
	 * @return the user roles available to the user in question
	 */
	Set<UserRole> getUserRoles(UUID userId);

	/**
	 * List out all user roles defined in prism
	 *
	 * @return all user roles defined in prism
	 */
	Set<UserRole> getAllUserRoles();
}

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

/**
 * 
 * {@link UserRoleConstants}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class UserRoleConstants {
	private UserRoleConstants() {}
	
	public final static String AUTOMATED = "automated";

	public final static String SUPER_USER = "super_user";
	public final static String ADMINISTRATOR = "administrator";
	public final static String READ_ONLY = "read_only";
	public final static String EDITOR = "editor";
	public final static String REVIEWER = "reviewer";
	public final static String APPROVER = "approver";
	public final static String MANAGER = "manager";
}

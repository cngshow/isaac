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
 * This is the master role file.  Both Prisme and IsaacRest should refer to this file.
 * @author cshupp
 * git sucks
 */
public enum PrismeRole {

	SUPER_USER(PrismeRoleType.GENERAL, "super_user"),
	ADMINISTRATOR(PrismeRoleType.GENERAL, "administrator"),
	READ_ONLY(PrismeRoleType.MODELING, "read_only"),
	EDITOR(PrismeRoleType.MODELING, "editor"),
	REVIEWER(PrismeRoleType.MODELING, "reviewer"),
	APRROVER(PrismeRoleType.MODELING, "approver"),
	MANAGER(PrismeRoleType.GENERAL, "manager"),
	VUID_REQUESTOR(PrismeRoleType.GENERAL, "vuid_requestor"),
	NTRT(PrismeRoleType.GENERAL, "ntrt"),
	DEPLOYMENT_MANAGER(PrismeRoleType.DEPLOYMENT, "deployment_manager");


	private String myName;
	private PrismeRoleType myType;

	private PrismeRole(PrismeRoleType type, String name) {
		myType = type;
		myName = name;
	}

	public String toString() {
		return myName;
	}
	
	public PrismeRoleType getType() {
		return myType;
	}

}

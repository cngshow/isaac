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
package gov.vha.isaac.metacontent.workflow.contents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

import gov.vha.isaac.metacontent.workflow.UserPermissionContentStore;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * Workflow roles available for each user for a given workflow definition
 * 
 * {@link UserPermissionContentStore} {@link StorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class UserPermission extends StorableWorkflowContents {
	/**
	 * The workflow definition key for which the User Permission is relevant.
	 */
	private UUID definitionId;

	/** The user whose Workflow Permission is being defined. */
	private int userNid;

	/**
	 * The workflow role available to the user for the associated definition. A
	 * user may have multiple roles.
	 */
	private String role;

	/**
	 * Constructor for a new user permission based on specified entry fields.
	 * 
	 * @param definitionId
	 * @param userNid
	 * @param role
	 */
	public UserPermission(UUID definitionId, int userNid, String role) {
		this.definitionId = definitionId;
		this.userNid = userNid;
		this.role = role;
	}

	/**
	 * Constructor for a new user permission based on serialized content.
	 * 
	 * @param data
	 */
	public UserPermission(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.id = (UUID) in.readObject();
			this.definitionId = (UUID) in.readObject();
			this.userNid = (Integer) in.readObject();
			this.role = (String) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into UserPermission", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast UserPermission fields into String", e);
			e.printStackTrace();
		}
	}

	/**
	 * Gets the definition key for which the user permission is pertinent.
	 *
	 * @return the definition Id
	 */
	public UUID getDefinitionId() {
		return definitionId;
	}

	/**
	 * Gets the user whose permission is being defined
	 * 
	 * @return the user nid
	 */
	public int getUserNid() {
		return userNid;
	}

	/**
	 * Gets the workflow role for the user.
	 *
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents#
	 * serialize()
	 */
	@Override
	public byte[] serialize() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);

		// write the object
		out.writeObject(id);
		out.writeObject(definitionId);
		out.writeObject(userNid);
		out.writeObject(role);

		return bos.toByteArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tUser: " + userNid
				+ "\n\t\tRole: " + role;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		UserPermission other = (UserPermission) obj;

		return this.definitionId.equals(other.definitionId) && this.userNid == other.userNid
				&& this.role.equals(other.role);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitionId.hashCode() + userNid + role.hashCode();
	}
}

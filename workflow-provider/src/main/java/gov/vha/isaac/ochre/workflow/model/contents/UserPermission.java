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
package gov.vha.isaac.ochre.workflow.model.contents;

import java.util.UUID;
import gov.vha.isaac.ochre.api.externalizable.ByteArrayDataBuffer;

/**
 * Workflow roles available for each user for a given workflow definition
 * 
 * {@link UserPermissionContentStore} {@link AbstractStorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class UserPermission extends AbstractStorableWorkflowContents {
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
     * Definition uuid most significant bits for this component
     */
	private long definitionIdMsb;
    
	/**
     * Definition uuid least significant bits for this component
     */
    private long definitionIdLsb;

	/**
	 * Constructor for a new user permission based on specified entry fields.
	 * 
	 * @param definitionId
	 * @param userNid
	 * @param role
	 */
	public UserPermission(UUID definitionId, int userNid, String role) {
		this.definitionId = definitionId;
        this.definitionIdMsb = definitionId.getMostSignificantBits();
        this.definitionIdLsb = definitionId.getLeastSignificantBits();
		this.userNid = userNid;
		this.role = role;
	}

	/**
	 * Constructor for a new user permission based on serialized content.
	 * 
	 * @param data
	 *            The data to deserialize into its components
	 */
	public UserPermission(byte[] data) {
		readData(new ByteArrayDataBuffer(data));
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

	@Override
	protected void putAdditionalWorkflowFields(ByteArrayDataBuffer out) {
		out.putLong(definitionIdMsb);
		out.putLong(definitionIdLsb);
		out.putNid(userNid);
		out.putByteArrayField(role.getBytes());
	}

	@Override
	protected void getAdditionalWorkflowFields(ByteArrayDataBuffer in) {
		definitionIdMsb = in.getLong();
		definitionIdLsb = in.getLong();
		definitionId = new UUID(definitionIdMsb, definitionIdLsb);

		userNid = in.getNid();
		role = new String(in.getByteArrayField());
	}

	@Override
	protected void skipAdditionalWorkflowFields(ByteArrayDataBuffer in) {
		in.getLong();
		in.getLong();
		in.getNid();
		in.getByteArrayField();
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

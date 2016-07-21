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
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * Roles available to a given workflow user
 * 
 * NOTE: The DefinitionId is the Key of the Definition Details Workflow Content
 * Store. Used to support different roles for different workflow definitions.
 *
 * {@link UserWorkflowPermission} {@link StorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
/**
 * @author yishai
 *
 */
public class UserWorkflowPermission extends StorableWorkflowContents {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The definition id. */
	private UUID definitionId;

	/** The user. */
	private int user;

	/** The roles. */
	private Set<String> roles;

	/**
	 * Instantiates a new workflow user permission.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param user
	 *            the user
	 * @param roles
	 *            the roles
	 */
	public UserWorkflowPermission(UUID definitionId, int user, Set<String> roles) {
		this.definitionId = definitionId;
		this.user = user;
		this.roles = roles;
	}

	/**
	 * Instantiates a new workflow user permission.
	 *
	 * @param data
	 *            the data
	 */
	public UserWorkflowPermission(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.id = (UUID) in.readObject();
			this.definitionId = (UUID) in.readObject();
			this.user = (Integer) in.readObject();
			this.roles = (Set<String>) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into UserWorkflowPermission", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast UserWorkflowPermission fields into String", e);
			e.printStackTrace();
		}
	}

	/**
	 * Gets the definition Id.
	 *
	 * @return the definition Id
	 */
	public UUID getDefinitionId() {
		return definitionId;
	}

	/**
	 * Gets the user.
	 *
	 * @return the user
	 */
	public int getUser() {
		return user;
	}

	/**
	 * Gets the roles.
	 *
	 * @return the roles
	 */
	public Set<String> getRoles() {
		return roles;
	}

	/**
	 * Sets the roles.
	 *
	 * @param newRoles the new roles
	 */
	public void setRoles(Set<String> newRoles) {
		roles = newRoles;
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
		out.writeObject(user);
		out.writeObject(roles);

		return bos.toByteArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		for (String r : roles) {
			buf.append(r + ", ");
		}

		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tUser: " + user
				+ "\n\t\tRoles: " + buf.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		UserWorkflowPermission other = (UserWorkflowPermission) obj;

		return this.definitionId.equals(other.definitionId) && this.user == other.user
				&& this.roles.equals(other.roles);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitionId.hashCode() + user + roles.hashCode();
	}
}

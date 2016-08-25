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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * Definition of Actions to Outcome States based on Roles and Initial State
 * 
 * NOTE: The DefinitionId is the Key of the Definition Details Workflow Content
 * Store. Different actions are defined per workflow definitions.
 * 
 * {@link AvailableAction} {@link StorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class AvailableAction extends StorableWorkflowContents {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The definition id. */
	private UUID definitionId;

	/** The initial state. */
	private String initialState;

	/** The action. */
	private String action;

	/** The outcome state. */
	private String outcomeState;

	/** The role. */
	private String role;

	/**
	 * Instantiates a new possible action.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param initialState
	 *            the initial state
	 * @param action
	 *            the action
	 * @param outcomeState
	 *            the outcome state
	 * @param role
	 *            the role
	 */
	public AvailableAction(UUID definitionId, String initialState, String action, String outcomeState, String role) {
		this.definitionId = definitionId;
		this.initialState = initialState;
		this.action = action;
		this.outcomeState = outcomeState;
		this.role = role;
	}

	/**
	 * Instantiates a new possible action.
	 *
	 * @param data
	 *            the data
	 */
	public AvailableAction(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.id = (UUID) in.readObject();
			this.definitionId = (UUID) in.readObject();
			this.initialState = (String) in.readObject();
			this.action = (String) in.readObject();
			this.outcomeState = (String) in.readObject();
			this.role = (String) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into Available Action", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast Available Action fields", e);
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
	 * Gets the initial state.
	 *
	 * @return the initial state
	 */
	public String getInitialState() {
		return initialState;
	}

	/**
	 * Gets the action.
	 *
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Gets the outcome state.
	 *
	 * @return the outcomeState
	 */
	public String getOutcomeState() {
		return outcomeState;
	}

	/**
	 * Gets the role.
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
		out.writeObject(initialState);
		out.writeObject(action);
		out.writeObject(outcomeState);
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
		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tInitial State: "
				+ initialState + "\n\t\tAction: " + action + "\n\t\tOutcome State: " + outcomeState + "\n\t\tRole: " + role;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		AvailableAction other = (AvailableAction) obj;

		return this.definitionId.equals(other.definitionId) && this.initialState.equals(other.initialState)
				&& this.action.equals(other.action) && this.outcomeState.equals(other.outcomeState)
				&& this.role.equals(other.role);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitionId.hashCode() + initialState.hashCode() + action.hashCode() + outcomeState.hashCode()
				+ role.hashCode();
	}
}
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

/**
 * The available workflow actions as defined via the workflow definition. Each
 * entry contains a single initial state-action-outcome state triplet that is an
 * available action for a given role.
 * 
 * The workflow must be in the initial state and a user must have the workflow
 * role to be able to perform the action.
 * 
 * {@link AvailableActionContentStore} {@link AbstractStorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class AvailableAction extends AbstractStorableWorkflowContents {
	/**
	 * The workflow definition key for which the Available Action is relevant.
	 */
	private UUID definitionId;

	/** The state which the action described may be executed upon. */
	private String initialState;

	/** The action that may be taken. */
	private String action;

	/** The resulting state based on the action taken on the initial state. */
	private String outcomeState;

	/** The workflow role which may perform the action on the initial state. */
	private String role;

	/**
	 * Constructor for a new available action on specified entry fields.
	 * 
	 * @param definitionId
	 * @param initialState
	 * @param action
	 * @param outcomeState
	 * @param role
	 */
	public AvailableAction(UUID definitionId, String initialState, String action, String outcomeState, String role) {
		this.definitionId = definitionId;
		this.initialState = initialState;
		this.action = action;
		this.outcomeState = outcomeState;
		this.role = role;
	}

	/**
	 * Constructor for a new available action based on serialized content.
	 *
	 * @param data
	 *            The data to deserialize into its components
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
		} 
		catch (Exception e)
		{
			throw new RuntimeException("Failure to deserialize data into Available Action", e);
		}
	}

	/**
	 * Gets the definition Id associated with the process.
	 *
	 * @return the key of the definition from which the process is created
	 */
	public UUID getDefinitionId() {
		return definitionId;
	}

	/**
	 * Gets the initial state associated with the available action.
	 *
	 * @return the initial state
	 */
	public String getInitialState() {
		return initialState;
	}

	/**
	 * Gets the action which can be executed on the initial state.
	 *
	 * @return the action
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Gets the outcome state if the action is performed.
	 *
	 * @return the outcomeState
	 */
	public String getOutcomeState() {
		return outcomeState;
	}

	/**
	 * Gets the workflow role that a user must have to perform the action.
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
	public byte[] serialize() {
		try
		{
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
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tInitial State: "
				+ initialState + "\n\t\tAction: " + action + "\n\t\tOutcome State: " + outcomeState + "\n\t\tRole: "
				+ role;
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
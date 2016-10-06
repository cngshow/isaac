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
 * The available workflow actions as defined via the workflow definition. Each
 * entry contains a single initial state-action-outcome state triplet that is an
 * available action for a given role.
 * 
 * The workflow must be in the initial state and a user must have the workflow
 * role to be able to perform the action.
 * 
 * {@link AbstractStorableWorkflowContents}
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
     * Definition uuid most significant bits for this component
     */
	private long definitionIdMsb;
    
	/**
     * Definition uuid least significant bits for this component
     */
    private long definitionIdLsb;

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
        this.definitionIdMsb = definitionId.getMostSignificantBits();
        this.definitionIdLsb = definitionId.getLeastSignificantBits();

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
		readData(new ByteArrayDataBuffer(data));
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

	@Override
	protected void putAdditionalWorkflowFields(ByteArrayDataBuffer out)	{
		out.putLong(definitionIdMsb);
		out.putLong(definitionIdLsb);
		out.putByteArrayField(initialState.getBytes());
		out.putByteArrayField(action.getBytes());
		out.putByteArrayField(outcomeState.getBytes());
		out.putByteArrayField(role.getBytes());
	}

	@Override
	protected void getAdditionalWorkflowFields(ByteArrayDataBuffer in) {
		definitionIdMsb = in.getLong();
		definitionIdLsb = in.getLong();
		initialState = new String(in.getByteArrayField());
		action = new String(in.getByteArrayField());
		outcomeState = new String(in.getByteArrayField());
		role = new String(in.getByteArrayField());

		definitionId = new UUID(definitionIdMsb, definitionIdLsb);
	}

	@Override
	protected void skipAdditionalWorkflowFields(ByteArrayDataBuffer in)	{
		in.getLong();
		in.getLong();
		in.getByteArrayField();
		in.getByteArrayField();
		in.getByteArrayField();
		in.getByteArrayField();
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
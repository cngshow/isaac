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
 * Definition of Actions to Outcomes based on Roles and Current State
 * 
 * NOTE: The DefinitionId is the Key of the Definition Details Workflow Content Store.
 * Different actions are defined per workflow definitions.
 * 
 * {@link AvailableAction} {@link StorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class AvailableAction implements StorableWorkflowContents {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The definition id. */
	private UUID definitionId;

	/** The current state. */
	private String currentState;

	/** The action. */
	private String action;

	/** The outcome. */
	private String outcome;

	/** The role. */
	private String role;

	/**
	 * Instantiates a new possible action.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param currentState
	 *            the current state
	 * @param action
	 *            the action
	 * @param outcome
	 *            the outcome
	 * @param role
	 *            the role
	 */
	public AvailableAction(UUID definitionId, String currentState, String action, String outcome, String role) {
		this.definitionId = definitionId;
		this.currentState = currentState;
		this.action = action;
		this.outcome = outcome;
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
			this.definitionId = (UUID) in.readObject();
			this.currentState = (String) in.readObject();
			this.action = (String) in.readObject();
			this.outcome = (String) in.readObject();
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
	 * Gets the current state.
	 *
	 * @return the current state
	 */
	public String getCurrentState() {
		return currentState;
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
	 * Gets the outcome.
	 *
	 * @return the outcome
	 */
	public String getOutcome() {
		return outcome;
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
		out.writeObject(definitionId);
		out.writeObject(currentState);
		out.writeObject(action);
		out.writeObject(outcome);
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
		return "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tCurrent State: " + currentState
				+ "\n\t\tAction: " + action + "\n\t\tOutcome: " + outcome + "\n\t\tRole State: " + role;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		AvailableAction other = (AvailableAction) obj;

		return this.definitionId.equals(other.definitionId) && this.currentState.equals(other.currentState)
				&& this.action.equals(other.action) && this.outcome.equals(other.outcome)
				&& this.role.equals(other.role);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitionId.hashCode() + currentState.hashCode() + action.hashCode() + outcome.hashCode()
				+ role.hashCode();
	}
}
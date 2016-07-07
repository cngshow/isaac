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
package gov.vha.isaac.metacontent.workflow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Definition of Actions to Outcomes based on Roles and Current State
 *
 * {@link PossibleAction}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@SuppressWarnings("serial")
public class PossibleAction implements Serializable {
	private static final Logger logger = LogManager.getLogger();

	private String currentState;
	private String action;
	private String outcome;
	private String role;

	public PossibleAction(String currentState, String action, String outcome, String role) {
		this.currentState = currentState;
		this.action = action;
		this.outcome = outcome;
		this.role = role;
	}

	public PossibleAction(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.currentState = ((PossibleAction) in.readObject()).currentState;
			this.action = ((PossibleAction) in.readObject()).action;
			this.outcome = ((PossibleAction) in.readObject()).outcome;
			this.role = ((PossibleAction) in.readObject()).role;
		} catch (IOException e) {
			logger.error("Failure to deserialize data into PossibleAction", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast PossibleAction fields into String", e);
			e.printStackTrace();
		}
	}

	public String getCurrentState() {
		return currentState;
	}

	public void setCurrentState(String currentState) {
		this.currentState = currentState;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		// default serialization
		out.defaultWriteObject();
		// write the object
		out.writeObject(currentState);
		out.writeObject(action);
		out.writeObject(outcome);
		out.writeObject(role);
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default deserialization
		ois.defaultReadObject();

		currentState = (String) ois.readObject();
		action = (String) ois.readObject();
		outcome = (String) ois.readObject();
		role = (String) ois.readObject();
	}

	@Override
	public String toString() {
		return "Current State: " + currentState + ",Action: " + action + ",Outcome: " + outcome + ",Role State: "
				+ role;
	}

	@Override
	public boolean equals(Object obj) {
		PossibleAction other = (PossibleAction) obj;

		return this.currentState.equals(other.currentState) && this.action.equals(other.action)
				&& this.outcome.equals(other.outcome) && this.role.equals(other.role);

	}

	@Override
	public int hashCode() {
		return currentState.hashCode() + action.hashCode() + outcome.hashCode() + role.hashCode();
	}

}

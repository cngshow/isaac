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
import java.util.Comparator;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * Storage of workflow history
 * 
 * {@link ProcessHistory} {@link StorableWorkflowContents}
 *
 * NOTE: The processId is the Key of the Process Definition Content Store.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class ProcessHistory extends StorableWorkflowContents {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The process id. */
	private UUID processId;

	/** The workflowUser. */
	private int workflowUser;

	/** The time advanced. */
	private long timeAdvanced;

	/** The state. */
	private String state;

	/** The action. */
	private String action;

	/** The outcome. */
	private String outcome;

	/** The comment. */
	private String comment;

	/**
	 * Instantiates a new workflow advancement.
	 *
	 * @param processId
	 *            the process id
	 * @param workflowUser
	 *            the workflowUser
	 * @param timeAdvanced
	 *            the time advanced
	 * @param state
	 *            the state
	 * @param action
	 *            the action
	 * @param outcome
	 *            the outcome
	 * @param comment
	 *            the comment
	 */
	public ProcessHistory(UUID processId, int workflowUser, long timeAdvanced, String state, String action,
			String outcome, String comment) {
		this.processId = processId;
		this.workflowUser = workflowUser;
		this.timeAdvanced = timeAdvanced;
		this.state = state;
		this.action = action;
		this.outcome = outcome;
		this.comment = comment;
	}

	/**
	 * Instantiates a new workflow advancement.
	 *
	 * @param data
	 *            the data
	 */
	public ProcessHistory(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.id = (UUID) in.readObject();
			this.processId = (UUID) in.readObject();
			this.workflowUser = (Integer) in.readObject();
			this.timeAdvanced = (Long) in.readObject();
			this.state = (String) in.readObject();
			this.action = (String) in.readObject();
			this.outcome = (String) in.readObject();
			this.comment = (String) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into ProcessHistory", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast ProcessHistory fields", e);
			e.printStackTrace();
		}
	}

	/**
	 * Gets the process id.
	 *
	 * @return the process id
	 */
	public UUID getProcessId() {
		return processId;
	}

	/**
	 * Gets the workflowUser.
	 *
	 * @return the workflowUser
	 */
	public int getWorkflowUser() {
		return workflowUser;
	}

	/**
	 * Gets the time advanced.
	 *
	 * @return the time advanced
	 */
	public long getTimeAdvanced() {
		return timeAdvanced;
	}

	/**
	 * Gets the state.
	 *
	 * @return the state
	 */
	public String getState() {
		return state;
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
	 * Gets the comment.
	 *
	 * @return the comment
	 */
	public String getComment() {
		return comment;
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
		out.writeObject(processId);
		out.writeObject(workflowUser);
		out.writeObject(timeAdvanced);
		out.writeObject(state);
		out.writeObject(action);
		out.writeObject(outcome);
		out.writeObject(comment);

		return bos.toByteArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "\n\t\tId: " + id + "\n\t\tProcess Id: " + processId + "\n\t\tWorkflowUser Id: " + workflowUser
				+ "\n\t\tTime Advanced: " + timeAdvanced + "\n\t\tState: " + state + "\n\t\tAction: " + action
				+ "\n\t\tOutcome: " + outcome + "\n\t\tComment: " + comment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		ProcessHistory other = (ProcessHistory) obj;

		return this.processId.equals(other.processId) && this.workflowUser == other.workflowUser
				&& this.timeAdvanced == other.timeAdvanced && this.state.equals(other.state)
				&& this.action.equals(other.action) && this.outcome.equals(other.outcome)
				&& this.comment.equals(other.comment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return processId.hashCode() + workflowUser + new Long(timeAdvanced).hashCode() + state.hashCode()
				+ action.hashCode() + outcome.hashCode() + comment.hashCode();
	}

	public static class ProcessHistoryComparator implements Comparator<ProcessHistory> {
		public ProcessHistoryComparator() {

		}

		@Override
		public int compare(ProcessHistory o1, ProcessHistory o2) {
			long t1 = o1.getTimeAdvanced();
			long t2 = o2.getTimeAdvanced();
			if (t2 > t1)
				return 1;
			else if (t1 > t2)
				return -1;
			else
				return 0;
		}
	}
}

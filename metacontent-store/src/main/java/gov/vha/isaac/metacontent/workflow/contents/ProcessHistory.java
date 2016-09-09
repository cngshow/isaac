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
import java.util.Date;
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
	private int userNid;

	/** The time advanced. */
	private long timeAdvanced;

	/** The initial state. */
	private String initialState;

	/** The action. */
	private String action;

	/** The outcome state. */
	private String outcomeState;

	/** The comment. */
	private String comment;

	/**
	 * Instantiates a new workflow advancement.
	 *
	 * @param processId
	 *            the process id
	 * @param userNid
	 *            the workflowUser
	 * @param timeAdvanced
	 *            the time advanced
	 * @param initialState
	 *            the initial state
	 * @param action
	 *            the action
	 * @param outcomeState
	 *            the outcome state
	 * @param comment
	 *            the comment
	 */
	public ProcessHistory(UUID processId, int userNid, long timeAdvanced, String initialState, String action,
			String outcomeState, String comment) {
		this.processId = processId;
		this.userNid = userNid;
		this.timeAdvanced = timeAdvanced;
		this.initialState = initialState;
		this.action = action;
		this.outcomeState = outcomeState;
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
			this.userNid = (Integer) in.readObject();
			this.timeAdvanced = (Long) in.readObject();
			this.initialState = (String) in.readObject();
			this.action = (String) in.readObject();
			this.outcomeState = (String) in.readObject();
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
	public int getUserNid() {
		return userNid;
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
	 * Gets the initial state.
	 *
	 * @return the initialState
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
		out.writeObject(userNid);
		out.writeObject(timeAdvanced);
		out.writeObject(initialState);
		out.writeObject(action);
		out.writeObject(outcomeState);
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
	    Date date=new Date(timeAdvanced);
	    String timeAdvancedString = workflowDateFormatrer.format(date);

		return "\n\t\tId: " + id + "\n\t\tProcess Id: " + processId + "\n\t\tWorkflowUser Id: " + userNid
				+ "\n\t\tTime Advanced as Long: " + timeAdvanced+ "\n\t\tTime Advanced: " + timeAdvancedString + "\n\t\tInitial State: " + initialState + "\n\t\tAction: " + action
				+ "\n\t\tOutcome State: " + outcomeState + "\n\t\tComment: " + comment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		ProcessHistory other = (ProcessHistory) obj;

		return this.processId.equals(other.processId) && this.userNid == other.userNid
				&& this.timeAdvanced == other.timeAdvanced && this.initialState.equals(other.initialState)
				&& this.action.equals(other.action) && this.outcomeState.equals(other.outcomeState)
				&& this.comment.equals(other.comment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return processId.hashCode() + userNid + new Long(timeAdvanced).hashCode() + initialState.hashCode()
				+ action.hashCode() + outcomeState.hashCode() + comment.hashCode();
	}

	public static class ProcessHistoryComparator implements Comparator<ProcessHistory> {
		public ProcessHistoryComparator() {

		}

		@Override
		public int compare(ProcessHistory o1, ProcessHistory o2) {
			long t1 = o1.getTimeAdvanced();
			long t2 = o2.getTimeAdvanced();
			if (t1 > t2)
				return 1;
			else if (t1 < t2)
				return -1;
			else return (o1.getProcessId().compareTo(o2.getProcessId())); 
		}
	}
}

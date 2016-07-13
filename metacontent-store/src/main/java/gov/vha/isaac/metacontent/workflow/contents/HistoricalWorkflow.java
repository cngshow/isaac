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
 * Storage of workflow advancements executed by all users
 * 
 * {@link HistoricalWorkflow} {@link StorableWorkflowContents}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class HistoricalWorkflow implements StorableWorkflowContents {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The task id. */
	private UUID taskId;

	/** The advancer. */
	private Integer advancer;

	/** The time advanced. */
	private Long timeAdvanced;

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
	 * @param taskId
	 *            the task id
	 * @param advancer
	 *            the advancer
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
	public HistoricalWorkflow(UUID taskId, Integer advancer, Long timeAdvanced, String state, String action,
			String outcome, String comment) {
		this.taskId = taskId;
		this.advancer = advancer;
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
	public HistoricalWorkflow(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.taskId = (UUID) in.readObject();
			this.advancer = (Integer) in.readObject();
			this.timeAdvanced = (Long) in.readObject();
			this.state = (String) in.readObject();
			this.action = (String) in.readObject();
			this.outcome = (String) in.readObject();
			this.comment = (String) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into HistoricalWorkflow", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast HistoricalWorkflow fields", e);
			e.printStackTrace();
		}
	}

	/**
	 * Gets the task id.
	 *
	 * @return the task id
	 */
	public UUID getTaskId() {
		return taskId;
	}

	/**
	 * Gets the advancer.
	 *
	 * @return the advancer
	 */
	public Integer getAdvancer() {
		return advancer;
	}

	/**
	 * Gets the time advanced.
	 *
	 * @return the time advanced
	 */
	public Long getTimeAdvanced() {
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
		out.writeObject(taskId);
		out.writeObject(advancer);
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
		return "\n\t\tTask Id: " + taskId + "\n\t\tAdvancer Id: " + advancer + "\n\t\tTime Advanced: " + timeAdvanced
				+ "\n\t\tState: " + state + "\n\t\tAction: " + action + "\n\t\tOutcome: " + outcome + "\n\t\tComment: "
				+ comment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		HistoricalWorkflow other = (HistoricalWorkflow) obj;

		return this.taskId.equals(other.taskId) && this.advancer.equals(other.advancer)
				&& this.timeAdvanced.equals(other.timeAdvanced) && this.state.equals(other.state)
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
		return taskId.hashCode() + advancer.hashCode() + timeAdvanced.hashCode() + state.hashCode() + action.hashCode()
				+ outcome.hashCode() + comment.hashCode();
	}
}

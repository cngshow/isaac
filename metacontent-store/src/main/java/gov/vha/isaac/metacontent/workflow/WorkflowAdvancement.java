package gov.vha.isaac.metacontent.workflow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("serial")
public class WorkflowAdvancement implements Serializable {
	private static final Logger logger = LogManager.getLogger();

	private Integer taskId = null;
	private Integer advancer = null;
	private Long timeCreated = null;
	private String state = null;
	private String action = null;
	private String outcome = null;
	private String comment = null;
	
	public WorkflowAdvancement(Integer taskId, Integer advancer, Long timeCreated, String state, String action, String outcome, String comment) {
		this.taskId = taskId;
		this.advancer = advancer;
		this.timeCreated = timeCreated;
		this.state = state;
		this.action = action;
		this.outcome = outcome;
		this.comment = comment;		
	}

	public WorkflowAdvancement(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.taskId = (Integer) in.readObject();
			this.advancer = (Integer) in.readObject();
			this.timeCreated = (Long) in.readObject();
			this.state = (String) in.readObject();
			this.action = (String) in.readObject();
			this.outcome = (String) in.readObject();
			this.comment = (String) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into WorkflowAdvancement", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast WorkflowAdvancement fields into String", e);
			e.printStackTrace();
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		// default serialization
		out.defaultWriteObject();
		// write the object
		out.writeObject(taskId);
		out.writeObject(advancer);
		out.writeObject(timeCreated);
		out.writeObject(state);
		out.writeObject(action);
		out.writeObject(outcome);
		out.writeObject(comment);
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default deserialization
		ois.defaultReadObject();

		taskId = (Integer) ois.readObject();
		advancer = (Integer) ois.readObject();
		timeCreated = (Long) ois.readObject();
		state = (String) ois.readObject();
		action = (String) ois.readObject();
		outcome = (String) ois.readObject();
		comment = (String) ois.readObject();
	}

	@Override
	public String toString() {
		return  
		   "\n\t\tTask Id: " + taskId +
		   "\n\t\tAdvancer Id: " + advancer +  
		   "\n\t\tTime Created: " + timeCreated +
		   "\n\t\tState: " + state +  
		   "\n\t\tAction: " + action +  
		   "\n\t\tOutcome: " + outcome +  
		   "\n\t\tComment: " + comment;	 
	}

	@Override
	public boolean equals(Object obj) {
		WorkflowAdvancement other = (WorkflowAdvancement) obj;

		return this.taskId.equals(other.taskId) &&
			   this.advancer.equals(other.advancer) &&
			   this.timeCreated.equals(other.timeCreated) &&
			   this.state.equals(other.state) &&
			   this.action.equals(other.action) &&
			   this.outcome.equals(other.outcome) &&
			   this.comment.equals(other.comment);
	}

	@Override
	public int hashCode() {
		return taskId.hashCode() + advancer.hashCode() + timeCreated.hashCode() +  state.hashCode() +
				action.hashCode() +  outcome.hashCode() + comment.hashCode();
	}

	public Integer getTaskId() {
		return taskId;
	}

	public Integer getAdvancer() {
		return advancer;
	}

	public Long getTimeCreated() {
		return timeCreated;
	}

	public String getState() {
		return state;
	}

	public String getAction() {
		return action;
	}

	public String getOutcome() {
		return outcome;
	}

	public String getComment() {
		return comment;
	}
}

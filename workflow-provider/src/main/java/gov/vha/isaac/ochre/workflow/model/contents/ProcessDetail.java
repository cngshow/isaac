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
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The metadata defining a given process (or workflow instance). This doesn't
 * include its history which is available via {@link ProcessHistory}
 * 
 * {@link ProcessDetailContentStore} {@link AbstractStorableWorkflowContents}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class ProcessDetail extends AbstractStorableWorkflowContents {
	/**
	 * The exhaustive list of possible process statuses.
	 */
	public enum ProcessStatus {
		/** Process is being defined and has yet to be launched. */
		DEFINED,
		/** Process has been launched */
		LAUNCHED,
		/** A previously launched or defined process that has been canceled */
		CANCELED,
		/** A previously launched process that is completed */
		CONCLUDED
	}

	/**
	 * The exhaustive list of possible ways an instantiated process may be ended
	 *
	 * 
	 */
	public enum EndWorkflowType {
		/** Process is stopped without reaching a completed state */
		CANCELED,
		/** Process has been finished by reaching a completed state */
		CONCLUDED
	};

	/** The workflow definition key for which the Process Detail is relevant. */
	private UUID definitionId;

	/**
	 * A set of all compontent nids modified within the workflow process.
	 * Therefore, if a component has been modified multiple times within a
	 * single process, all those stamps are persisted in order.
	 */
	private Set<Integer> componentNids = new HashSet<>();

	/** The user who originally defined (created) the workflow process. */
	private int creatorNid;

	/** The time the workflow process was created. */
	private long timeCreated;

	/** The time the workflow process was launched. */
	private long timeLaunched = -1L;

	/**
	 * The time the workflow process was finished (either via canceled Or
	 * Concluded).
	 */
	private long timeCanceledOrConcluded = -1L;

	/** The workflow process's status. */
	private ProcessStatus status;

	/** The workflow process's name. */
	private String name;

	/** The workflow process's description. */
	private String description;

	/**
	 * Constructor for a new process based on specified entry fields.
	 * 
	 * @param definitionId
	 * @param creatorNid
	 * @param timeCreated
	 * @param status
	 * @param name
	 * @param description
	 */
	public ProcessDetail(UUID definitionId, int creatorNid, long timeCreated, ProcessStatus status, String name,
			String description) {
		this.definitionId = definitionId;
		this.creatorNid = creatorNid;
		this.timeCreated = timeCreated;
		this.status = status;
		this.name = name;
		this.description = description;
	}

	/**
	 * Constructor for a new process based on serialized content.
	 *
	 * @param data
	 * The data to deserialize into its components
	 */
	public ProcessDetail(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.id = (UUID) in.readObject();
			this.definitionId = (UUID) in.readObject();

			// TODO these nids need to swap to UUIDs - certainly when writing to
			// the changeset file
			@SuppressWarnings("unchecked") Set<Integer> componentNidsReadObject = (Set<Integer>) in.readObject();
			this.componentNids = componentNidsReadObject;

			this.creatorNid = (Integer) in.readObject();
			this.timeCreated = (Long) in.readObject();
			this.timeLaunched = (Long) in.readObject();
			this.timeCanceledOrConcluded = (Long) in.readObject();
			this.status = (ProcessStatus) in.readObject();
			this.name = (String) in.readObject();
			this.description = (String) in.readObject();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failure to deserialize data into ProcessDetail", e);
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
	 * Gets the process's component nids.
	 *
	 * @return map of component nids to ordered stamp sequences
	 */
	public Set<Integer> getComponentNids()
	{
		return componentNids;
	}

	/**
	 * Gets the process creator.
	 *
	 * @return the process creator's nid
	 */
	public int getCreatorNid() {
		return creatorNid;
	}

	/**
	 * Gets the time the process was created as long primitive type.
	 *
	 * @return the time the process was created
	 */
	public long getTimeCreated() {
		return timeCreated;
	}

	/**
	 * Gets the time the process ended either via cancelation or conclusion as
	 * long primitive type.
	 *
	 * @return the time the process was canceled or concluded
	 */
	public long getTimeCanceledOrConcluded() {
		return timeCanceledOrConcluded;
	}

	/**
	 * Sets the time the process ended either via cancelation or conclusion as
	 * this isn't available during the object's construction.
	 *
	 * @param time
	 * The time the process was canceled/concluded as long primitive
	 * type
	 */
	public void setTimeCanceledOrConcluded(long time) {
		timeCanceledOrConcluded = time;
	}

	/**
	 * Get the time the process was launched as long primitive type
	 * 
	 * @return the time launched
	 */
	public long getTimeLaunched() {
		return timeLaunched;
	}

	/**
	 * Sets the time the process as launched as this isn't available during the
	 * object's construction.
	 *
	 * @param time
	 * The time the process was launched as long primitive type
	 */
	public void setTimeLaunched(long time) {
		timeLaunched = time;
	}

	/**
	 * Gets the process's current status.
	 *
	 * @return the process Status
	 */
	public ProcessStatus getStatus() {
		return status;
	}

	/**
	 * Sets the process's current status as this is updated over the course of
	 * the process.
	 *
	 * @param status
	 * The process's current status
	 */
	public void setStatus(ProcessStatus status) {
		this.status = status;
	}

	/**
	 * The name of the process
	 * 
	 * @return process name
	 */
	public String getName() {
		return name;
	}

	/**
	 * The description of the process
	 * 
	 * @return process description
	 */
	public String getDescription() {
		return description;
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
			out.writeObject(componentNids);
			out.writeObject(creatorNid);
			out.writeObject(timeCreated);
			out.writeObject(timeLaunched);
			out.writeObject(timeCanceledOrConcluded);
			out.writeObject(status);
			out.writeObject(name);
			out.writeObject(description);

			return bos.toByteArray();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public boolean isActive() {
		return status == ProcessStatus.LAUNCHED || status == ProcessStatus.DEFINED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		for (Integer compNid : componentNids)
		{
			buf.append("\n\t\t\tFor Component Nid: " + compNid);
		}

		Date date = new Date(timeCreated);
		String timeCreatedString = workflowDateFormatrer.format(date);

		date = new Date(timeLaunched);
		String timeLaunchedString = workflowDateFormatrer.format(date);

		date = new Date(timeCanceledOrConcluded);
		String timeCanceledOrConcludedString = workflowDateFormatrer.format(date);

		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tComponents to Sequences Map: " + buf.toString() + "\n\t\tCreator Id: "
				+ creatorNid + "\n\t\tTime Created: " + timeCreatedString + "\n\t\tTime Launched: " + timeLaunchedString + "\n\t\tTime Canceled or Concluded: "
				+ timeCanceledOrConcludedString + "\n\t\tStatus: " + status + "\n\t\tName: " + name + "\n\t\tDescription: " + description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		ProcessDetail other = (ProcessDetail) obj;

		return this.definitionId.equals(other.definitionId) && this.componentNids.equals(other.componentNids) && this.creatorNid == other.creatorNid
				&& this.timeCreated == other.timeCreated && this.timeLaunched == other.timeLaunched && this.timeCanceledOrConcluded == other.timeCanceledOrConcluded
				&& this.status == other.status && this.name.equals(other.name) && this.description.equals(other.description);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitionId.hashCode() + componentNids.hashCode() + creatorNid + new Long(timeCreated).hashCode() + new Long(timeLaunched).hashCode()
				+ new Long(timeCanceledOrConcluded).hashCode() + status.hashCode() + name.hashCode() + description.hashCode();
	}

	/**
	 * A custom comparator to assist in ordering process detial information.
	 * Based on creation time.
	 *
	 */
	public static class ProcessDetailComparator implements Comparator<ProcessDetail> {

		/**
		 * Instantiates a new process detail comparator.
		 */
		public ProcessDetailComparator() {

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(ProcessDetail o1, ProcessDetail o2) {
			long t1 = o1.getTimeCreated();
			long t2 = o2.getTimeCreated();
			if (t2 > t1)
				return 1;
			else if (t1 > t2)
				return -1;
			else
				return o1.getId().compareTo(o2.getId());
		}
	}
}

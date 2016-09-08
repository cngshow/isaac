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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * Definition of process when new workflow instance created
 * 
 * {@link ProcessDetail} {@link StorableWorkflowContents}.
 *
 * NOTE: The DefinitionId is the Key of the Definition Details Workflow Content
 * Store as multiple Process Detailss will exist for a single Workflow
 * Definition
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class ProcessDetail extends StorableWorkflowContents {
	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The definition id. */
	private UUID definitionId;

	/** The map from component nids to all stamps associated with component. */
	private Map<Integer, List<Integer>> componentToStampsMap = new HashMap<>();

	/** The creator. */
	private int creator;

	/** The time created. */
	private long timeCreated;

	/** The time launched. */
	private long timeLaunched = -1L;

	/** The time canceled Or Concluded. */
	private long timeCanceledOrConcluded = -1L;

	/** The status. */
	private ProcessStatus status;

	/** The name. */
	private String name;

	/** The description. */
	private String description;

	/**
	 * Instantiates a new process detail.
	 */
	public ProcessDetail() {

	}

	/**
	 * Instantiates a new process definition.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param creator
	 *            the creator
	 * @param timeCreated
	 *            the time created
	 * @param active
	 *            the active flag
	 * @param ProcessStatus
	 *            the process status
	 * @param name
	 *            the name
	 * @param description
	 *            the description
	 */
	public ProcessDetail(UUID definitionId, int creator, long timeCreated, ProcessStatus status, String name,
			String description) {
		this.definitionId = definitionId;
		this.creator = creator;
		this.timeCreated = timeCreated;
		this.status = status;
		this.name = name;
		this.description = description;
	}

	/**
	 * Instantiates a new process definition.
	 *
	 * @param data
	 *            the data
	 */
	public ProcessDetail(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.id = (UUID) in.readObject();
			this.definitionId = (UUID) in.readObject();

			@SuppressWarnings("unchecked")
			Map<Integer, List<Integer>> componentToStampMapReadObject = (Map<Integer, List<Integer>>) in.readObject();
			this.componentToStampsMap = componentToStampMapReadObject;

			this.creator = (Integer) in.readObject();
			this.timeCreated = (Long) in.readObject();
			this.timeLaunched = (Long) in.readObject();
			this.timeCanceledOrConcluded = (Long) in.readObject();
			this.status = (ProcessStatus) in.readObject();
			this.name = (String) in.readObject();
			this.description = (String) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into ProcessDetail", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast ProcessDetail fields", e);
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
	 * Gets the component to statmps map.
	 *
	 * @return the stamp sequences
	 */
	public Map<Integer, List<Integer>> getComponentToStampsMap() {
		return componentToStampsMap;
	}

	/**
	 * Gets the creator.
	 *
	 * @return the creator
	 */
	public int getCreator() {
		return creator;
	}

	/**
	 * Gets the time created.
	 *
	 * @return the time created
	 */
	public long getTimeCreated() {
		return timeCreated;
	}

	/**
	 * Gets the time concluded.
	 *
	 * @return the time concluded
	 */
	public long getTimeCanceledOrConcluded() {
		return timeCanceledOrConcluded;
	}

	/**
	 * Sets the time concluded.
	 *
	 * @param time
	 *            the new time concluded
	 */
	public void setTimeCanceledOrConcluded(long time) {
		timeCanceledOrConcluded = time;
	}

	public long getTimeLaunched() {
		return timeLaunched;
	}

	public void setTimeLaunched(long time) {
		timeLaunched = time;
	}

	/**
	 * Gets the defining status.
	 *
	 * @return the defining status
	 */
	public ProcessStatus getStatus() {
		return status;
	}

	/**
	 * Sets the defining status.
	 *
	 * @param definingStatus
	 *            the new defining status
	 */
	public void setStatus(ProcessStatus status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

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
	public byte[] serialize() throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);

		// write the object
		out.writeObject(id);
		out.writeObject(definitionId);
		out.writeObject(componentToStampsMap);
		out.writeObject(creator);
		out.writeObject(timeCreated);
		out.writeObject(timeLaunched);
		out.writeObject(timeCanceledOrConcluded);
		out.writeObject(status);
		out.writeObject(name);
		out.writeObject(description);

		return bos.toByteArray();
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

		for (Integer compNid : componentToStampsMap.keySet()) {
			buf.append("\n\t\t\tFor Component: " + compNid + " have following stamp sequences:");

			for (Integer stampSeq : componentToStampsMap.get(compNid)) {
				buf.append(stampSeq + ", ");
			}
		}

		Date date = new Date(timeCreated);
		String timeCreatedString = workflowDateFormatrer.format(date);

		date = new Date(timeLaunched);
		String timeLaunchedString = workflowDateFormatrer.format(date);

		date = new Date(timeCanceledOrConcluded);
		String timeCanceledOrConcludedString = workflowDateFormatrer.format(date);

		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString()
				+ "\n\t\tComponents to Sequences Map: " + buf.toString() + "\n\t\tCreator Id: " + creator
				+ "\n\t\tTime Created: " + timeCreatedString + "\n\t\tTime Launched: " + timeLaunchedString
				+ "\n\t\tTime Canceled or Concluded: " + timeCanceledOrConcludedString + "\n\t\tStatus: " + status
				+ "\n\t\tName: " + name + "\n\t\tDescription: " + description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		ProcessDetail other = (ProcessDetail) obj;

		return this.definitionId.equals(other.definitionId)
				&& this.componentToStampsMap.equals(other.componentToStampsMap) && this.creator == other.creator
				&& this.timeCreated == other.timeCreated && this.timeLaunched == other.timeLaunched
				&& this.timeCanceledOrConcluded == other.timeCanceledOrConcluded && this.status == other.status
				&& this.name.equals(other.name) && this.description.equals(other.description);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitionId.hashCode() + componentToStampsMap.hashCode() + creator + new Long(timeCreated).hashCode()
				+ new Long(timeLaunched).hashCode() + new Long(timeCanceledOrConcluded).hashCode() + status.hashCode()
				+ name.hashCode() + description.hashCode();
	}

	/**
	 * The Class ProcessDetailComparator.
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

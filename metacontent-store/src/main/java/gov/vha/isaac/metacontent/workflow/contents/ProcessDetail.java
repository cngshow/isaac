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
import java.util.List;
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

	/** The stamp sequences. */
	private List<Integer> stampSequences;

	/** The concept. */
	private UUID concept;

	/** The creator. */
	private int creator;

	/** The time created. */
	private long timeCreated;

	/** The time created. */
	private long timeConcluded = -1L;

	/** The active flag. */
	private Boolean active;

	/**
	 * Instantiates a new process definition.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param stampSequences
	 *            the stamp sequences
	 * @param concept
	 *            the concept
	 * @param creator
	 *            the creator
	 * @param timeCreated
	 *            the time created
	 */
	public ProcessDetail(UUID definitionId, UUID concept, List<Integer> stampSequences, int creator, long timeCreated,
			boolean active) {
		this.definitionId = definitionId;
		this.stampSequences = stampSequences;
		this.concept = concept;
		this.creator = creator;
		this.timeCreated = timeCreated;
		this.active = active;
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
			this.stampSequences = (List<Integer>) in.readObject();
			this.concept = (UUID) in.readObject();
			this.creator = (Integer) in.readObject();
			this.timeCreated = (Long) in.readObject();
			this.timeConcluded = (Long) in.readObject();
			this.active = (Boolean) in.readObject();
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
	 * Gets the stamp sequences.
	 *
	 * @return the stamp sequences
	 */
	public List<Integer> getStampSequences() {
		return stampSequences;
	}

	/**
	 * Gets the stamp sequences.
	 *
	 * @return the stamp sequences
	 */
	public List<Integer> addStampSequences(List<Integer> newStamps) {
		stampSequences.addAll(newStamps);

		return stampSequences;
	}

	/**
	 * Gets the concept.
	 *
	 * @return the concept
	 */
	public UUID getConcept() {
		return concept;
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
	public long getTimeConcluded() {
		return timeConcluded;
	}

	/**
	 * Sets the time concluded.
	 */
	public void setTimeConcluded(long time) {
		timeConcluded = time;
	}

	/**
	 * Gets the active flag.
	 *
	 * @return the active flag value
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Sets the active flag.
	 */
	public void setActive(boolean val) {
		active = val;
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
		out.writeObject(stampSequences);
		out.writeObject(concept);
		out.writeObject(creator);
		out.writeObject(timeCreated);
		out.writeObject(timeConcluded);
		out.writeObject(active);

		return bos.toByteArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		for (Integer stampSeq : stampSequences) {
			buf.append(stampSeq + ", ");
		}

		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tStamp Sequence: "
				+ buf.toString() + "\n\t\tConcept: " + concept.toString() + "\n\t\tCreator Id: " + creator
				+ "\n\t\tTime Created: " + timeCreated + "\n\t\tTime Concluded: " + timeConcluded + "\n\t\tActive: "
				+ active;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		ProcessDetail other = (ProcessDetail) obj;

		return this.definitionId.equals(other.definitionId) && this.stampSequences.equals(other.stampSequences)
				&& this.concept.equals(other.concept) && this.creator == other.creator
				&& this.timeCreated == other.timeCreated && this.timeConcluded == other.timeConcluded
				&& this.active.equals(other.active);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitionId.hashCode() + stampSequences.hashCode() + concept.hashCode() + creator
				+ new Long(timeCreated).hashCode() + new Long(timeConcluded).hashCode() + active.hashCode();
	}
}

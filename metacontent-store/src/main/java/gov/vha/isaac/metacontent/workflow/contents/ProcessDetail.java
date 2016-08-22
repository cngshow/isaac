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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
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
	private ArrayList<Integer> stampSequences = new ArrayList<>();

	/** The concept Sequences. */
	private Set<Integer> conceptSequences;

	/** The creator. */
	private int creator;

	/** The time created. */
	private long timeCreated;

	/** The time created. */
	private long timeConcluded = -1L;

	/** The subject matter. */
	private SubjectMatter subjectMatter;

	/** The defining status. */
	private ProcessStatus processStatus;

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
	 * @param conceptSequences
	 *            the concepts
	 * @param stampSequences
	 *            the stamp sequences
	 * @param creator
	 *            the creator
	 * @param timeCreated
	 *            the time created
	 * @param active
	 *            the active flag
	 * @param subjectMatter
	 *            the subject matter
	 * @param definingStatus
	 *            the defining status
	 * @param domainStandard
	 *            the domain standard
	 */
	public ProcessDetail(UUID definitionId, Set<Integer> conceptSequences, List<Integer> stampSequences, int creator,
			long timeCreated, SubjectMatter subjectMatter, ProcessStatus definingStatus) {
		this.definitionId = definitionId;
		this.stampSequences.addAll(stampSequences);
		this.conceptSequences = conceptSequences;
		this.creator = creator;
		this.timeCreated = timeCreated;
		this.subjectMatter = subjectMatter;
		this.processStatus = definingStatus;
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
			ArrayList<Integer> stampSequencesReadObject = (ArrayList<Integer>) in.readObject();
			this.stampSequences.addAll(stampSequencesReadObject);

			@SuppressWarnings("unchecked")
			Set<Integer> conceptSequencesReadObject = (Set<Integer>) in.readObject();
			this.conceptSequences = conceptSequencesReadObject;

			this.creator = (Integer) in.readObject();
			this.timeCreated = (Long) in.readObject();
			this.timeConcluded = (Long) in.readObject();
			this.subjectMatter = (SubjectMatter) in.readObject();
			this.processStatus = (ProcessStatus) in.readObject();
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
	 * Gets the conceptSequences.
	 *
	 * @return the concept sequences
	 */
	public Set<Integer> getConceptSequences() {
		return conceptSequences;
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
	 *
	 * @param time
	 *            the new time concluded
	 */
	public void setTimeConcluded(long time) {
		timeConcluded = time;
	}

	/**
	 * Gets the subjectMatter.
	 *
	 * @return the workflow subject matter
	 */
	public SubjectMatter getSubjectMatter() {
		return subjectMatter;
	}

	/**
	 * Gets the defining status.
	 *
	 * @return the defining status
	 */
	public ProcessStatus getProcessStatus() {
		return processStatus;
	}

	/**
	 * Sets the defining status.
	 *
	 * @param definingStatus
	 *            the new defining status
	 */
	public void setProcessStatus(ProcessStatus definingStatus) {
		this.processStatus = definingStatus;
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
		out.writeObject(conceptSequences);
		out.writeObject(creator);
		out.writeObject(timeCreated);
		out.writeObject(timeConcluded);
		out.writeObject(subjectMatter);
		out.writeObject(processStatus);

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

		StringBuffer buf2 = new StringBuffer();

		for (Integer conId : conceptSequences) {
			buf2.append(conId + ", ");
		}

		return "\n\t\tId: " + id + "\n\t\tDefinition Id: " + definitionId.toString() + "\n\t\tStamp Sequence: "
				+ buf.toString() + "\n\t\tConcept: " + buf2.toString() + "\n\t\tCreator Id: " + creator
				+ "\n\t\tTime Created: " + timeCreated + "\n\t\tTime Concluded: " + timeConcluded
				+ "\n\t\tSubject Matter: " + subjectMatter + "\n\t\tProcess Status: " + processStatus;
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
				&& this.conceptSequences.equals(other.conceptSequences) && this.creator == other.creator
				&& this.timeCreated == other.timeCreated && this.timeConcluded == other.timeConcluded
				&& this.subjectMatter == other.subjectMatter && this.processStatus == other.processStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitionId.hashCode() + stampSequences.hashCode() + conceptSequences.hashCode() + creator
				+ new Long(timeCreated).hashCode() + new Long(timeConcluded).hashCode() + subjectMatter.hashCode()
				+ processStatus.hashCode();
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

	public boolean isActive() {
		return processStatus == ProcessStatus.LAUNCHED || processStatus == ProcessStatus.DEFINED;
	}
}

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
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * Definition of Actions to Outcomes based on Roles and Current State
 * 
 * NOTE: The ClinicalDomain is the Key of the Definition Details Workflow
 * Content Store. Different actions are defined per workflow definitions.
 * 
 * {@link DomainStandard} {@link StorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class DomainStandard extends StorableWorkflowContents implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 5383504672997938543L;

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The clinical domain. */
	private WorkflowDomain clinicalDomain;

	/** The element terminology map. */
	private Map<WorkflowDataElement, Set<WorkflowTerminology>> elementTerminologyMap;

	/**
	 * Instantiates a new possible action.
	 *
	 * @param clinicalDomain
	 *            the clinical domain
	 * @param elementTerminologyMap
	 *            the element terminology map
	 */
	public DomainStandard(WorkflowDomain clinicalDomain,
			Map<WorkflowDataElement, Set<WorkflowTerminology>> elementTerminologyMap) {
		this.clinicalDomain = clinicalDomain;
		this.elementTerminologyMap = elementTerminologyMap;
	}

	/**
	 * Instantiates a new possible action.
	 *
	 * @param data
	 *            the data
	 */
	public DomainStandard(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.id = (UUID) in.readObject();
			this.clinicalDomain = (WorkflowDomain) in.readObject();
			this.elementTerminologyMap = (Map<WorkflowDataElement, Set<WorkflowTerminology>>) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into Available Action", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast Available Action fields", e);
			e.printStackTrace();
		}
	}

	/**
	 * Gets the clinical domain.
	 *
	 * @return the clinical domain
	 */
	public WorkflowDomain getClinicalDomain() {
		return clinicalDomain;
	}

	/**
	 * Gets the element terminology map.
	 *
	 * @return the element terminology map
	 */
	public Map<WorkflowDataElement, Set<WorkflowTerminology>> getElementTerminologyMap() {
		return elementTerminologyMap;
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
		out.writeObject(clinicalDomain);
		out.writeObject(elementTerminologyMap);

		return bos.toByteArray();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "\n\t\tId: " + id + "\n\t\tClinical Domain: " + clinicalDomain.toString()
				+ "\n\t\tElement Terminology Map: " + elementTerminologyMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		DomainStandard other = (DomainStandard) obj;

		return this.clinicalDomain.equals(other.clinicalDomain)
				&& this.elementTerminologyMap.equals(other.elementTerminologyMap);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return clinicalDomain.hashCode() + elementTerminologyMap.hashCode();
	}
}
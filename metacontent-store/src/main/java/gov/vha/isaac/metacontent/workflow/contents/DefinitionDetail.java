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
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * Definition of workflow to Outcomes based on Roles and Current State
 * 
 * NOTE: The DefinitionId is the Key of the Definition Details Workflow Content
 * Store. Different actions are defined per workflow definitions.
 * 
 * {@link DefinitionDetail} {@link StorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class DefinitionDetail extends StorableWorkflowContents {
	public enum StartWorkflowType {
		SINGLE_CASE
	}

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The bpmn2 id. */
	private String bpmn2Id;

	/** The name. */
	private String name;

	/** The namespace. */
	private String namespace;

	/** The version. */
	private String version;

	/** The roles. */
	private Set<String> roles;

	/** A description of the purpose of the Definition pulled by BPMN2. */
	private String description;

	/** Automated date when BPMN2 imported into bundle. */
	private long importDate;

	/** The start type. */
	private StartWorkflowType startType;

	/**
	 * Instantiates a new definition details.
	 *
	 * @param bpmn2Id
	 *            the bpmn2 id
	 * @param name
	 *            the name
	 * @param namespace
	 *            the namespace
	 * @param version
	 *            the version
	 * @param roles
	 *            the roles
	 * @param description
	 *            the description
	 */
	public DefinitionDetail(String bpmn2Id, String name, String namespace, String version, Set<String> roles,
			String description, StartWorkflowType startType) {
		this.bpmn2Id = bpmn2Id;
		this.name = name;
		this.namespace = namespace;
		this.version = version;
		this.roles = roles;
		this.description = description;
		this.importDate = new Date().getTime();
		this.startType = startType;
	}

	/**
	 * Instantiates a new definition details from a serialized byte array.
	 *
	 * @param data
	 *            serialized byte array
	 */
	public DefinitionDetail(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.id = (UUID) in.readObject();
			this.bpmn2Id = (String) in.readObject();
			this.name = (String) in.readObject();
			this.namespace = (String) in.readObject();
			this.version = (String) in.readObject();
			this.roles = (Set<String>) in.readObject();
			this.description = (String) in.readObject();
			this.importDate = (long) in.readObject();
			this.startType = (StartWorkflowType) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into Definition Detail", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast Definition Detail fields", e);
			e.printStackTrace();
		}
	}

	public String getBpmn2Id() {
		return bpmn2Id;
	}

	public String getName() {
		return name;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getVersion() {
		return version;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public String getDescription() {
		return description;
	}

	public long getImportDate() {
		return importDate;
	}

	public StartWorkflowType getWorkflowStartType() {
		return startType;
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
		out.writeObject(bpmn2Id);
		out.writeObject(name);
		out.writeObject(namespace);
		out.writeObject(version);
		out.writeObject(roles);
		out.writeObject(description);
		out.writeObject(importDate);
		out.writeObject(startType);

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

		for (String r : roles) {
			buf.append(r + ", ");
		}

		Date date = new Date(importDate);
		String importDateString = workflowDateFormatrer.format(date);

		return "\n\t\tId: " + id + "\n\t\tBPMN2 Id: " + bpmn2Id + "\n\t\tName: " + name + "\n\t\tNamespace: "
				+ namespace + "\n\t\tVersion: " + version + "\n\t\tRoles: " + buf.toString() + "\n\t\tDescription: "
				+ description + "\n\t\tImport Date: " + importDateString + "\n\t\tStart Type: " + startType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		DefinitionDetail other = (DefinitionDetail) obj;

		return this.bpmn2Id.equals(other.bpmn2Id) && this.name.equals(other.name)
				&& this.namespace.equals(other.namespace) && this.version.equals(other.version)
				&& this.roles.equals(other.roles) && this.description.equals(other.description)
				&& this.importDate == other.importDate && this.startType.equals(other.startType);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return bpmn2Id.hashCode() + name.hashCode() + namespace.hashCode() + version.hashCode() + roles.hashCode()
				+ description.hashCode() + new Long(importDate).hashCode() + startType.hashCode();
	}
}
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
package gov.vha.isaac.metacontent.workflow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContent;

/**
 * Static workflow permissions initialized during reading of WF Definition only
 *
 * {@link StaticAuthorRoleContentStore}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class StaticAuthorRoleContentStore implements StorableWorkflowContent {
	private Integer author;
	private Set<String> roles = new HashSet<>();

	private static final Logger logger = LogManager.getLogger();

	public StaticAuthorRoleContentStore(Integer author, Set<String> roles) {
		this.author = author;
		this.roles = roles;
	}

	@SuppressWarnings("unchecked")
	public StaticAuthorRoleContentStore(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.author = (Integer) in.readObject();
			this.roles = (HashSet<String>) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into StaticAuthorRoleContentStore", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast StaticAuthorRoleContentStore's fields into String", e);
			e.printStackTrace();
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.userPrefs.StorableWorkflowContent#serialize()
	 */
	@Override
	public byte[] serialize() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out;

		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(author);
			out.writeObject(roles);

			return bos.toByteArray();
		} catch (IOException e) {
			logger.error("Failure to serialize data from StaticAuthorRoleContentStore", e);
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		for (String r : roles) {
			buf.append(r + ",");
		}

		return "StaticAuthorRoleContentStore:" + "\nAuthor: " + author + "\nRoles: " + buf.toString();
	}

	@Override
	public boolean equals(Object obj) {
		StaticAuthorRoleContentStore other = (StaticAuthorRoleContentStore) obj;

		return this.author.equals(other.author) && this.roles.equals(other.roles);

	}
	
	public Integer getAuthor() {
		return author;
	}
	
	public Set<String> getAuthorRoles() {
		return roles;
	}
}

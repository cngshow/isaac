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
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents;

/**
 * Roles available to a given author
 * 
 * {@link AvailableAction}
 * {@link StorableWorkflowContents}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class AuthorPermission implements StorableWorkflowContents {
	
	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The author. */
	private Integer author;
	
	/** The roles. */
	private Set<String> roles;

	/**
	 * Instantiates a new author permission.
	 *
	 * @param author the author
	 * @param roles the roles
	 */
	public AuthorPermission(Integer author, Set<String> roles) {
		this.author = author;
		this.roles = roles;
	}

	/**
	 * Instantiates a new author permission.
	 *
	 * @param data the data
	 */
	public AuthorPermission(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.author = (Integer) in.readObject();
			this.roles = (Set<String>) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into AuthorPermission", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast AuthorPermission fields into String", e);
			e.printStackTrace();
		}
	}

	/**
	 * Gets the author.
	 *
	 * @return the author
	 */
	public Integer getAuthor() {
		return author;
	}

	/**
	 * Gets the roles.
	 *
	 * @return the roles
	 */
	public Set<String> getRoles() {
		return roles;
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
		out.writeObject(author);
		out.writeObject(roles);

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
		
		return "\n\t\tAuthor: " + author + "\n\t\tRoles: " + buf.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		AuthorPermission other = (AuthorPermission) obj;

		return this.author.equals(other.author) && this.roles.equals(other.roles);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return author.hashCode() + roles.hashCode();
	}
}

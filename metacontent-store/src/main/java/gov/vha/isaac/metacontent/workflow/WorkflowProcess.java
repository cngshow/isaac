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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Definition of Actions to Outcomes based on Roles and Current State
 *
 * {@link WorkflowProcess}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@SuppressWarnings("serial")
public class WorkflowProcess implements Serializable {
	private static final Logger logger = LogManager.getLogger();

	private Integer taskId = null;
	private Integer stampSequence = null;
	private Set<UUID> components = new HashSet<UUID>();
	private Integer creator = null;
	private Long timeCreated = null;

	public WorkflowProcess(Integer taskId, Integer stampSequence, Set<UUID> components, Integer creator, Long timeCreated) {
		this.taskId = taskId;
		this.stampSequence = stampSequence;
		this.components = components;
		this.creator = creator;
		this.timeCreated = timeCreated;
	}

	public WorkflowProcess(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.taskId = ((WorkflowProcess) in.readObject()).taskId;
			this.stampSequence = ((WorkflowProcess) in.readObject()).stampSequence;
			this.components = ((WorkflowProcess) in.readObject()).components;
			this.creator = ((WorkflowProcess) in.readObject()).creator;
			this.timeCreated = ((WorkflowProcess) in.readObject()).timeCreated;
		} catch (IOException e) {
			logger.error("Failure to deserialize data into WorkflowProcess", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast WorkflowProcess fields into String", e);
			e.printStackTrace();
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		// default serialization
		out.defaultWriteObject();
		// write the object
		out.writeObject(taskId);
		out.writeObject(stampSequence);
		out.writeObject(components);
		out.writeObject(creator);
		out.writeObject(timeCreated);
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		// default deserialization
		ois.defaultReadObject();

		taskId = (Integer) ois.readObject();
		stampSequence = (Integer) ois.readObject();
		components = (Set<UUID>) ois.readObject();
		creator = (Integer) ois.readObject();
		timeCreated = (Long) ois.readObject();
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		for (UUID uid : components) {
			buf.append(uid + ", ");
		}

		return "UsersProcessCreationContentStore:" + 
			   "\n\t\tTask Id: " + taskId +
			   "\n\t\tStamp Sequence: " + stampSequence +  
			   "\n\t\tComponents: " + buf.toString() +  
			   "\n\t\tCreator Id: " + creator +  
			   "\n\t\tTime Created: " + timeCreated;
	}

	@Override
	public boolean equals(Object obj) {
		WorkflowProcess other = (WorkflowProcess) obj;

		return this.taskId.equals(other.taskId) &&
			   this.stampSequence.equals(other.stampSequence) &&
			   this.components.equals(other.components) &&
			   this.creator.equals(other.creator) &&
			   this.timeCreated.equals(other.timeCreated);
	}

	@Override
	public int hashCode() {
		return taskId.hashCode() + stampSequence.hashCode() + components.hashCode() + creator.hashCode() + timeCreated.hashCode();
	}

}

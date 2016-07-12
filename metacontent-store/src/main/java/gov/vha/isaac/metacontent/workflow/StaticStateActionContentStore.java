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
 * Static workflow possible actions initialized during reading of WF Definition
 * only
 *
 * {@link StaticStateActionContentStore}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */

public class StaticStateActionContentStore implements StorableWorkflowContent {

	private static final Logger logger = LogManager.getLogger();
	Set<PossibleAction> actions = new HashSet<>();

	public StaticStateActionContentStore(Set<PossibleAction> actions) {
		this.actions = actions;
	}

	@SuppressWarnings("unchecked")
	public StaticStateActionContentStore(byte[] data) {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in;
		try {
			in = new ObjectInputStream(bis);
			this.actions = (Set<PossibleAction>) in.readObject();
		} catch (IOException e) {
			logger.error("Failure to deserialize data into StaticStateActionContentStore", e);
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			logger.error("Failure to cast StaticStateActionContentStore's fields into Set", e);
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
			out.writeObject(actions);

			return bos.toByteArray();
		} catch (IOException e) {
			logger.error("Failure to serialize data from StaticStateActionContentStore", e);
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		int i = 1;
		for (PossibleAction a : actions) {
			buf.append("\n\tPossible Action #" + i++ + ": " + a + "\n");
		}

		return "StaticStateActionContentStore: " + buf.toString();
	}

	@Override
	public boolean equals(Object obj) {
		StaticStateActionContentStore other = (StaticStateActionContentStore) obj;

		return this.actions.equals(other.actions);
	}
	
	public Set<PossibleAction> getPossibleActions() {
		return actions;
	}
}

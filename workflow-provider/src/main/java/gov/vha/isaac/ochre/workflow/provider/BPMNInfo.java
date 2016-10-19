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
package gov.vha.isaac.ochre.workflow.provider;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.ochre.workflow.model.contents.AvailableAction;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.EndWorkflowType;

/**
 * A class that stores various info about the BPMN workflow.
 * 
 * {@link Bpmn2FileImporter} {@link WorkflowProvider}
 * 
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class BPMNInfo {

	public static final UUID UNOWNED_PROCESS = new UUID(0,0);

	/** A universal means of expressing a workflow time stamp */
	final static public SimpleDateFormat workflowDateFormatter = new SimpleDateFormat("hh:mm:ssa MM/dd/yy");

	/** A map of available actions per type of ending workflow */
	private Map<EndWorkflowType, Set<AvailableAction>> endNodeTypeMap;

	/**
	 * A map of available actions per definition from which a workflow may be
	 * started
	 */
	private Map<UUID, Set<AvailableAction>> definitionStartActionMap;
	
	private UUID definitionId;

	/** A map of all states per definition from which a process may be edited */
	private Map<UUID, Set<String>> editStatesMap;
	
	protected BPMNInfo(UUID definitionId, Map<EndWorkflowType, Set<AvailableAction>> endNodeTypeMap, Map<UUID, Set<AvailableAction>> definitionStartActionMap, 
			Map<UUID, Set<String>> editStatesMap)
	{
		this.definitionId = definitionId;
		this.endNodeTypeMap = Collections.unmodifiableMap(endNodeTypeMap);
		this.definitionStartActionMap = Collections.unmodifiableMap(definitionStartActionMap);
		this.editStatesMap = Collections.unmodifiableMap(editStatesMap);
	}


	/**
	 * Retrieves the map of end workflow types to the set of all available
	 * actions causing a process to be ProcessStatus.CANCELED or
	 * ProcessStatus.CONLCUDED.
	 * 
	 * Used for programmatically identifying if a requested action requested is
	 * concluding a process.
	 *
	 * @return the read-only map of available actions per type of ending workflow
	 */
	public Map<EndWorkflowType, Set<AvailableAction>> getEndWorkflowTypeMap() {
		return endNodeTypeMap;
	}

	/**
	 * Retrieves the map of workflow definitions to the set of all available
	 * actions start-process actions available.
	 * 
	 * Used for populating Process History with the initial workflow action via
	 * an automated role.
	 *
	 * @return the read-only map of available actions per type of ending workflow
	 */
	public Map<UUID, Set<AvailableAction>> getDefinitionStartActionMap() {
		return definitionStartActionMap;
	}

	/**
	 * Gets a map of all edit states available per workflow definition.
	 * 
	 * Used to identify if the current state is an edit state. If it is,
	 * modeling and mapping can occur and will be added to the process.
	 * 
	 * @return the read-only set of edit states
	 */
	public Map<UUID, Set<String>> getEditStatesMap() {
		return editStatesMap;
	}

	/**
	 * Identifies if the state represents a concluded state by examining the
	 * endNoeTypeMap's CONCLUDED list of AvaialbleActions
	 * 
	 * @param state
	 *            The state to test
	 * 
	 * @return true if the state is a Concluded state
	 */
	public boolean isConcludedState(String state) {
		for (AvailableAction action : endNodeTypeMap.get(EndWorkflowType.CONCLUDED)) {
			if (action.getInitialState().equals(state)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Identifies if the state is an edit state by examining the editStatesMap.
	 * 
	 * @param definitionId
	 *            The definition which the state's process belongs
	 * @param state
	 *            The state to test
	 * 
	 * @return true if the state is an Edit state
	 */
	public boolean isEditState(UUID definitionId, String state) {
		return editStatesMap.get(definitionId).contains(state);
	}
	
	public UUID getDefinitionId()
	{
		return definitionId;
	}
}
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserWorkflowPermission;

/**
 * Utility to access workflow data from content stores 
 * 
 * {@link AbstractWorkflowUtilities} {@link WorkflowInformationAccessor}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowInformationAccessor extends AbstractWorkflowUtilities {

	/**
	 * Instantiates a new workflow information accessor.
	 *
	 * @throws Exception
	 *             the exception
	 */
	public WorkflowInformationAccessor() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow information accessor.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowInformationAccessor(MVStoreMetaContentProvider store) {
		super(store);

		definitionDetailStore = new DefinitionDetailWorkflowContentStore(store);
	}

	/**
	 * Checks if is concept in workflow.
	 *
	 * @param concept
	 *            the concept
	 * @return true, if is concept in workflow
	 */
	public boolean isConceptInWorkflow(UUID concept) {
		return false;

	}

	/**
	 * Checks if is component in workflow.
	 *
	 * @param component
	 *            the component
	 * @return true, if is component in workflow
	 */
	public boolean isComponentInWorkflow(UUID component) {
		return false;

	}

	/**
	 * Gets the full history.
	 *
	 * @return the full history
	 */
	public List<ProcessHistory> getFullHistory() {
		List<ProcessHistory> list = new ArrayList<>();

		return list;
	}

	/**
	 * Gets the auther permissions.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param authorId
	 *            the author id
	 * @return the auther permissions
	 */
	public Set<UserWorkflowPermission> getAutherPermissions(UUID definitionId, int authorId) {
		Set<UserWorkflowPermission> permission = null;

		return permission;
	}

	/**
	 * Gets the available actions.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param role
	 *            the role
	 * @return the available actions
	 */
	public Set<AvailableAction> getAvailableActions(UUID definitionId, String role) {
		Set<AvailableAction> actions = null;

		return actions;
	}

	/**
	 * Gets the definition detail.
	 *
	 * @param definitionId
	 *            the definition id
	 * @return the definition detail
	 */
	public DefinitionDetail getDefinitionDetail(UUID definitionId) {
		DefinitionDetail detail = null;

		return detail;
	}

	/**
	 * Gets the process detail.
	 *
	 * @param processId
	 *            the process id
	 * @return the process detail
	 */
	public ProcessDetail getProcessDetail(UUID processId) {
		ProcessDetail detail = null;

		return detail;
	}

	/**
	 * Checks for permission.
	 *
	 * @param definitionId
	 *            the definition id
	 * @param authorId
	 *            the author id
	 * @param state
	 *            the state
	 * @return true, if successful
	 */
	public boolean hasPermission(UUID definitionId, int authorId, String state) {

		return false;
	}
}

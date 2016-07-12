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
package gov.vha.isaac.ochre.workflow.provider.contentstore;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.PossibleAction;
import gov.vha.isaac.metacontent.workflow.StaticAuthorRoleContentStore;
import gov.vha.isaac.metacontent.workflow.StaticStateActionContentStore;
import gov.vha.isaac.ochre.api.metacontent.MetaContentService.WorkflowContentTypes;
import gov.vha.isaac.ochre.workflow.provider.WorkflowDefinitionUtility;

/**
 * Test both static and user based workflow content as defined in the
 * metacontent-store
 *
 * {@link StaticWorkflowContentStoreTest}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class StaticWorkflowContentStoreTest {
	private final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/workflow/provider/VetzWorkflow.bpmn2";
	private MVStoreMetaContentProvider store;

	@Before
	public void setUp() {
		store = new MVStoreMetaContentProvider(new File("target"), "test", true);
		WorkflowDefinitionUtility util = new WorkflowDefinitionUtility(store);
		util.setNodes(BPMN_FILE_PATH);
	}

	@After
	public void tearDown() {
		store.close();
	}

	@Test
	public void testWorkflowStartupStores() throws Exception {
		// Create Initial Content
		Set<String> roles = new HashSet<>();
		roles.add("Reviewer");
		roles.add("Author");
		StaticAuthorRoleContentStore createdAuthorRoleContent = new StaticAuthorRoleContentStore(5, roles);

		Set<PossibleAction> actions = new HashSet<>();
		actions.add(new PossibleAction("EDIT", "REVIEW", "REVIEW", "REVIEWER"));
		actions.add(new PossibleAction("REVIEW", "APPROVE", "APPROVE", "APPROVER"));
		StaticStateActionContentStore createdStateActionContent = new StaticStateActionContentStore(actions);

		// Write content into database
		store.putWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE, createdAuthorRoleContent);
		store.putWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME, createdStateActionContent);

		// Read from DB and confirm content is same as created
		StaticAuthorRoleContentStore pulledAuthorRoleContent = new StaticAuthorRoleContentStore(
				store.getWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE));
		StaticStateActionContentStore pulledStateActionContent = new StaticStateActionContentStore(
				store.getWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME));

		Assert.assertTrue(pulledAuthorRoleContent.equals(createdAuthorRoleContent));
		Assert.assertTrue(pulledStateActionContent.equals(createdStateActionContent));

		store.close();

		// Reopen database and confirm content is still same as created
		store = new MVStoreMetaContentProvider(new File("target"), "test", false);

		pulledAuthorRoleContent = new StaticAuthorRoleContentStore(
				store.getWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE));
		pulledStateActionContent = new StaticStateActionContentStore(
				store.getWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME));

		Assert.assertTrue(pulledAuthorRoleContent.equals(createdAuthorRoleContent));
		Assert.assertTrue(pulledStateActionContent.equals(createdStateActionContent));
		store.close();

		// Remove single type in database
		store = new MVStoreMetaContentProvider(new File("target"), "test", false);
		store.removeWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE);
		store.close();

		// Read from DB and confirm results empty and confirm graceful handling
		// of already empty workflow content
		store = new MVStoreMetaContentProvider(new File("target"), "test", false);
		Assert.assertNull(store.getWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE));
		Assert.assertNotNull(store.getWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME));
		store.close();

		// Add content to DB once again
		store = new MVStoreMetaContentProvider(new File("target"), "test", true);
		store.putWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE, createdAuthorRoleContent);
		store.putWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME, createdStateActionContent);
		store.close();

		// Reopen database and confirm content is still same as created
		pulledAuthorRoleContent = new StaticAuthorRoleContentStore(
				store.getWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE));
		pulledStateActionContent = new StaticStateActionContentStore(
				store.getWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME));
		store.close();

		// Open DB with clean-wipe and confirm is empty
		store = new MVStoreMetaContentProvider(new File("target"), "test", true);
		Assert.assertNull(store.getWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE));
		Assert.assertNull(store.getWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME));
		store.close();

		// Read from DB and confirm graceful handling of already empty workflow
		// content
		store = new MVStoreMetaContentProvider(new File("target"), "test", false);
		Assert.assertNull(store.getWorkflowContent(WorkflowContentTypes.AUTHOR_ROLE));
		Assert.assertNull(store.getWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME));
	}

	@Test
	public void testActualWorkflowStartupStores() throws Exception {
		StaticStateActionContentStore pulledStateActionContent = new StaticStateActionContentStore(
				store.getWorkflowContent(WorkflowContentTypes.STATE_ACTION_OUTCOME));

		Assert.assertSame("Expected number of actionOutome records not what expected",
				pulledStateActionContent.getPossibleActions().size(), 9);

		for (PossibleAction actionOutcome : pulledStateActionContent.getPossibleActions()) {
			System.out.println(actionOutcome);
		}
	}

}

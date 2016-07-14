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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.AvailableActionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;

/**
 * Test the WorkflowDefinitionUtility class
 * 
 * {@link ImportBpmn2FileUtility}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class ImportBpmn2FileTest {

	/** The bpmn file path. */
	private final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/workflow/provider/VetzWorkflow.bpmn2";

	/** The store. */
	private MVStoreMetaContentProvider store;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUp() {
		store = new MVStoreMetaContentProvider(new File("target"), "test", true);
		new ImportBpmn2FileUtility(store, BPMN_FILE_PATH);
	}

	/**
	 * Tear down.
	 */
	@After
	public void tearDown() {
		store.close();
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testVetzWorkflowSetNodes() throws Exception {
		AvailableActionWorkflowContentStore createdStateActionCdontent = new AvailableActionWorkflowContentStore(store);

		Assert.assertSame("Expected number of actionOutome records not what expected",
				createdStateActionCdontent.getNumberOfEntries(), 7);

		List<String> possibleRoles = Arrays.asList("Editor", "Reviewer", "Approver", "Promoter");
		List<String> possibleActions = Arrays.asList("Cancel Workflow", "QA Fails", "QA Passes", "Approve",
				"Reject Edit", "Reject Review");
		List<String> possibleStates = Arrays.asList("Canceled", "Ready for Edit", "Ready for Approve",
				"Ready for Publish", "Ready for Review");

		UUID definitionId = null;

		for (AvailableAction entry : createdStateActionCdontent.getAllEntries()) {
			if (definitionId == null) {
				definitionId = entry.getDefinitionId();
			}
			
			Assert.assertEquals(definitionId, entry.getDefinitionId());
			Assert.assertTrue(possibleRoles.contains(entry.getRole()));
			Assert.assertTrue(possibleStates.contains(entry.getOutcome()));
			Assert.assertTrue(possibleStates.contains(entry.getCurrentState()));
			Assert.assertTrue(possibleActions.contains(entry.getAction()));
		}
	}

}

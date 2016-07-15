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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.AvailableActionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;

/**
 * Test the WorkflowDefinitionUtility class
 * 
 * {@link Bpmn2FileImporter}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class Bpmn2FileImporterTest {

	/** The bpmn file path. */
	static private final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/workflow/provider/VetzWorkflow.bpmn2";

	/** The store. */
	static private MVStoreMetaContentProvider store;

	/**
	 * Sets the up.
	 */
	@BeforeClass
	public static void setUpClass() {
		store = new MVStoreMetaContentProvider(new File("target"), "test", true);
		new Bpmn2FileImporter(store, BPMN_FILE_PATH);
	}

	/**
	 * Tear down.
	 */
	@AfterClass
	public static void tearDownClass() {
		store.close();
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testVetzWorkflowSetDefinition() throws Exception {
		DefinitionDetailWorkflowContentStore createdDefinitionDetailContentStore = new DefinitionDetailWorkflowContentStore(
				store);

		Assert.assertSame("Expected number of actionOutome records not what expected",
				createdDefinitionDetailContentStore.getNumberOfEntries(), 1);

		DefinitionDetail entry = createdDefinitionDetailContentStore.getAllEntries().iterator().next();
		Set<String> expectedRoles = new HashSet<>();
		expectedRoles.add("Editor");
		expectedRoles.add("Reviewer");
		expectedRoles.add("Approver");

		Assert.assertEquals(entry.getBpmn2Id(), "VetzWorkflow");
		Assert.assertEquals(entry.getName(), "VetzWorkflow");
		Assert.assertEquals(entry.getNamespace(), "org.jbpm");
		Assert.assertEquals(entry.getVersion(), "1.2");
		Assert.assertEquals(entry.getRoles(), expectedRoles);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testVetzWorkflowSetNodes() throws Exception {
		System.out.println("**** AAAA");
		DefinitionDetailWorkflowContentStore createdDefinitionDetailContentStore = new DefinitionDetailWorkflowContentStore(
				store);
		System.out.println("**** BBBB");
		AvailableActionWorkflowContentStore createdAvailableActionContentStore = new AvailableActionWorkflowContentStore(
				store);
		System.out.println("**** CCCC");

		Assert.assertSame("Expected number of actionOutome records not what expected",
				createdAvailableActionContentStore.getNumberOfEntries(), 7);

		DefinitionDetail definitionDetails = createdDefinitionDetailContentStore.getAllEntries().iterator().next();

		List<String> possibleActions = Arrays.asList("Cancel Workflow", "QA Fails", "QA Passes", "Approve",
				"Reject Edit", "Reject Review");
		List<String> possibleStates = Arrays.asList("Canceled", "Ready for Edit", "Ready for Approve",
				"Ready for Publish", "Ready for Review");

		for (AvailableAction entry : createdAvailableActionContentStore.getAllEntries()) {
			Assert.assertEquals(definitionDetails.getId(), entry.getDefinitionId());
			Assert.assertTrue(definitionDetails.getRoles().contains(entry.getRole()));
			Assert.assertTrue(possibleStates.contains(entry.getOutcome()));
			Assert.assertTrue(possibleStates.contains(entry.getCurrentState()));
			Assert.assertTrue(possibleActions.contains(entry.getAction()));
		}
	}
}

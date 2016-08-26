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
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.AvailableActionContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities.StartWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.crud.AbstractWorkflowProviderTestPackage;

/**
 * Test the WorkflowDefinitionUtility class
 * 
 * {@link Bpmn2FileImporter}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class Bpmn2FileImporterTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;

	private static MVStoreMetaContentProvider store;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testBpmn2FileImport", true);
			setupCompleted = true;
		}

		globalSetup(store);
	}

	@AfterClass
	public static void tearDownClass() {
		AbstractWorkflowUtilities.close();
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testVetzWorkflowSetDefinition() throws Exception {
		DefinitionDetailContentStore createdDefinitionDetailContentStore = new DefinitionDetailContentStore(
				store);

		Assert.assertSame("Expected number of actionOutome records not what expected",
				createdDefinitionDetailContentStore.getNumberOfEntries(), 1);

		DefinitionDetail entry = createdDefinitionDetailContentStore.getAllEntries().iterator().next();
		Set<String> expectedRoles = new HashSet<>();
		expectedRoles.add("Editor");
		expectedRoles.add("Reviewer");
		expectedRoles.add("Approver");
		expectedRoles.add(AbstractWorkflowUtilities.getAutomatedRole());

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
		DefinitionDetailContentStore createdDefinitionDetailContentStore = new DefinitionDetailContentStore(
				store);
		AvailableActionContentStore createdAvailableActionContentStore = new AvailableActionContentStore(
				store);

		Assert.assertSame("Expected number of actionOutome records not what expected",
				createdAvailableActionContentStore.getNumberOfEntries(), 9);

		DefinitionDetail definitionDetails = createdDefinitionDetailContentStore.getAllEntries().iterator().next();

		List<String> possibleActions = Arrays.asList("Cancel Workflow", "Edit", "QA Fails", "QA Passes", "Approve",
				"Reject Edit", "Reject Review", "Create Workflow Process");
		
		List<String> possibleStates = Arrays.asList("Assigned", "Canceled During Review", "Canceled During Approval", "Ready for Edit", "Ready for Approve",
				"Ready for Publish", "Ready for Review");

		int count = 0;
		Set<AvailableAction> identifiedCanceledActions = new HashSet<>();
		Set<AvailableAction> identifiedConcludedActions = new HashSet<>();
		Set<AvailableAction> identifiedStartTypeActions = new HashSet<>();
		Set<String> identifiedEditingActions = new HashSet<>();

		for (AvailableAction entry : createdAvailableActionContentStore.getAllEntries()) {
			if (entry.getAction().equals("Cancel Workflow")) {
				identifiedCanceledActions.add(entry);
			} else if (entry.getAction().equals("Approve")) {
				identifiedConcludedActions.add(entry);
			} else if (entry.getAction().equals("Edit")) {
				identifiedEditingActions.add(entry.getInitialState());
			} else if (entry.getInitialState().equals("Assigned")) {
				identifiedStartTypeActions.add(entry);
			}
			
			Assert.assertEquals(definitionDetails.getId(), entry.getDefinitionId());
			Assert.assertTrue(definitionDetails.getRoles().contains(entry.getRole()));
			Assert.assertTrue(possibleStates.contains(entry.getOutcomeState()));
			Assert.assertTrue(possibleStates.contains(entry.getInitialState()));
			Assert.assertTrue(possibleActions.contains(entry.getAction()));
		}
		
		Set<AvailableAction> concludedActions = AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CONCLUDED);
		Set<AvailableAction> canceledActions = AbstractWorkflowUtilities.getEndNodeTypeMap().get(EndWorkflowType.CANCELED);
		
		Assert.assertEquals(canceledActions, identifiedCanceledActions);
		Assert.assertEquals(concludedActions, identifiedConcludedActions);
				
		Assert.assertEquals(AbstractWorkflowUtilities.getStartWorkflowTypeMap().keySet().size(), 1);
		Assert.assertEquals(AbstractWorkflowUtilities.getStartWorkflowTypeMap().size(), identifiedStartTypeActions.size());
		Assert.assertEquals(AbstractWorkflowUtilities.getStartWorkflowTypeMap().keySet().iterator().next(), StartWorkflowType.SINGLE_CASE);
		Assert.assertEquals(AbstractWorkflowUtilities.getStartWorkflowTypeMap().get(StartWorkflowType.SINGLE_CASE), identifiedStartTypeActions.iterator().next());

		Assert.assertEquals(AbstractWorkflowUtilities.getEditStates(), identifiedEditingActions);
	}
}

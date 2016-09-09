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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	public void testStaticBpmnSetDefinition() throws Exception {
		DefinitionDetailContentStore createdDefinitionDetailContentStore = new DefinitionDetailContentStore(store);

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
	public void testStaticBpmnSetNodes() throws Exception {
		DefinitionDetailContentStore createdDefinitionDetailContentStore = new DefinitionDetailContentStore(store);
		AvailableActionContentStore createdAvailableActionContentStore = new AvailableActionContentStore(store);

		Assert.assertSame("Expected number of actionOutome records not what expected",
				createdAvailableActionContentStore.getNumberOfEntries(), 10);

		DefinitionDetail definitionDetails = createdDefinitionDetailContentStore.getAllEntries().iterator().next();

		List<String> possibleActions = Arrays.asList("Cancel Workflow", "Edit", "QA Fails", "QA Passes", "Approve",
				"Reject Edit", "Reject Review", "Create Workflow Process");

		List<String> possibleStates = Arrays.asList("Assigned", "Canceled During Edit", "Canceled During Review", "Canceled During Approval",
				"Ready for Edit", "Ready for Approve", "Ready for Publish", "Ready for Review");

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

		Set<AvailableAction> concludedActions = AbstractWorkflowUtilities.getEndWorkflowTypeMap()
				.get(EndWorkflowType.CONCLUDED);
		Set<AvailableAction> canceledActions = AbstractWorkflowUtilities.getEndWorkflowTypeMap()
				.get(EndWorkflowType.CANCELED);

		Assert.assertEquals(canceledActions, identifiedCanceledActions);
		Assert.assertEquals(concludedActions, identifiedConcludedActions);

		Assert.assertEquals(AbstractWorkflowUtilities.getDefinitionStartActionMap().keySet().size(), 1);
		Assert.assertEquals(AbstractWorkflowUtilities.getDefinitionStartActionMap().size(),
				identifiedStartTypeActions.size());
		Assert.assertEquals(AbstractWorkflowUtilities.getDefinitionStartActionMap().keySet().iterator().next(),
				definitionDetails.getId());
		Assert.assertEquals(AbstractWorkflowUtilities.getDefinitionStartActionMap().get(definitionDetails.getId()),
				identifiedStartTypeActions);

		Assert.assertEquals(AbstractWorkflowUtilities.getEditStates(), identifiedEditingActions);
	}

	@Test
	public void testStaticBpmnAvailableActions() throws Exception {
		Map<String, Set<AvailableAction>> actionMap = new HashMap<>();
		
		for (AvailableAction action : availableActionStore.getAllEntries()) {
			if (!actionMap.containsKey(action.getInitialState())) {
				actionMap.put(action.getInitialState(), new HashSet<AvailableAction>());
			}
			
			actionMap.get(action.getInitialState()).add(action);
		}
		
		for (String initState : actionMap.keySet()) {
			if (initState.equals("Assigned")) {
				assertAssignedActions(actionMap.get(initState));
			} else if (initState.equals("Ready for Edit")) {
				assertReadyForEditActions(actionMap.get(initState));
			} else if (initState.equals("Ready for Review")) {
				assertReadyForReviewActions(actionMap.get(initState));
			} else if (initState.equals("Ready for Approval")) {
				assertReadyForApprovalActions(actionMap.get(initState));
			}
		}
	}

	private void assertAssignedActions(Set<AvailableAction> actions) {
		Assert.assertEquals(1, actions.size());
		Assert.assertEquals("Create Workflow Process", actions.iterator().next().getAction());
		Assert.assertEquals("Ready for Edit", actions.iterator().next().getOutcomeState());
		Assert.assertEquals("Automated By System", actions.iterator().next().getRole());
	}

	private void assertReadyForEditActions(Set<AvailableAction> actions) {
		Assert.assertEquals(2, actions.size());

		for (AvailableAction act : actions) {
			Assert.assertEquals("Editor", act.getRole());

			if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertEquals("Canceled During Edit", act.getOutcomeState());
			} else if (act.getAction().equals("Edit")) {
				Assert.assertEquals("Ready for Review", act.getOutcomeState());
			} else {
				Assert.fail();
			}
		}
	}

	private void assertReadyForReviewActions(Set<AvailableAction> actions) {
		Assert.assertEquals(3, actions.size());
		
		for (AvailableAction act : actions) {
			Assert.assertEquals("Reviewer", act.getRole());

			if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertEquals("Canceled During Review", act.getOutcomeState());
			} else if (act.getAction().equals("QA Fails")) {
				Assert.assertEquals("Ready for Edit", act.getOutcomeState());
			} else if (act.getAction().equals("QA Passes")) {
				Assert.assertEquals("Ready for Approve", act.getOutcomeState());
			} else {
				Assert.fail();
			}
		}
	}

	private void assertReadyForApprovalActions(Set<AvailableAction> actions) {
		Assert.assertEquals(4, actions.size());
		
		for (AvailableAction act : actions) {
			Assert.assertEquals("Approver", act.getRole());

			if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertEquals("Canceled During Approval", act.getOutcomeState());
			} else if (act.getAction().equals("Reject Edit")) {
				Assert.assertEquals("Ready for Edit", act.getOutcomeState());
			} else if (act.getAction().equals("Reject Review")) {
				Assert.assertEquals("Reject Review", act.getOutcomeState());
			} else if (act.getAction().equals("Approve")) {
				Assert.assertEquals("Ready for Publish", act.getOutcomeState());
			} else {
				Assert.fail();
			}
		}
		
	}
}

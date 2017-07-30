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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.PrismeRole;
import gov.vha.isaac.ochre.api.util.RecursiveDelete;
import gov.vha.isaac.ochre.workflow.model.WorkflowContentStore;
import gov.vha.isaac.ochre.workflow.model.contents.AvailableAction;
import gov.vha.isaac.ochre.workflow.model.contents.DefinitionDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.provider.crud.AbstractWorkflowProviderTestPackage;

/**
 * Test the Bpmn2FileImporter class
 * 
 * {@link Bpmn2FileImporter} {@link AbstractWorkflowProviderTestPackage}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class Bpmn2FileImporterTest extends AbstractWorkflowProviderTestPackage {

	/**
	 * Sets the up.
	 */
	@BeforeClass
	public static void setUpClass() {
		WorkflowProvider.BPMN_PATH = BPMN_FILE_PATH;
		LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(new File("target/store").toPath());
		LookupService.startupMetadataStore();
		globalSetup();
	}

	@AfterClass
	public static void tearDownClass() throws IOException {
		LookupService.shutdownSystem();
		RecursiveDelete.delete(new File("target/store"));
	}
	
	@Before
	public void beforeTest()
	{
		wp_.getProcessDetailStore().clear();
		wp_.getProcessHistoryStore().clear();
	}

	/**
	 * Test proper definition metadata following import of a bpmn2 file
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testImportBpmn2FileMetadata() throws Exception {
		WorkflowContentStore<DefinitionDetail> createdDefinitionDetailContentStore = LookupService.get().getService(WorkflowProvider.class).getDefinitionDetailStore();

		Assert.assertSame("Expected number of actionOutome records not what expected",
				createdDefinitionDetailContentStore.size(), 1);

		DefinitionDetail entry = createdDefinitionDetailContentStore.values().iterator().next();
		Set<PrismeRole> expectedRoles = new HashSet<>();
		expectedRoles.add(PrismeRole.EDITOR);
		expectedRoles.add(PrismeRole.REVIEWER);
		expectedRoles.add(PrismeRole.APPROVER);
		expectedRoles.add(PrismeRole.AUTOMATED);

		Assert.assertEquals(entry.getBpmn2Id(), "VetzWorkflow");
		Assert.assertEquals(entry.getName(), "VetzWorkflow");
		Assert.assertEquals(entry.getNamespace(), "org.jbpm");
		Assert.assertEquals(entry.getVersion(), "1.2");
		Assert.assertEquals(entry.getRoles(), expectedRoles);
	}

	/**
	 * Test proper definition nodes following import of a bpmn2 file
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testStaticBpmnSetNodes() throws Exception {
		WorkflowContentStore<DefinitionDetail> createdDefinitionDetailContentStore = LookupService.get().getService(WorkflowProvider.class).getDefinitionDetailStore();
		WorkflowContentStore<AvailableAction> createdAvailableActionContentStore = LookupService.get().getService(WorkflowProvider.class).getAvailableActionStore();

		Assert.assertSame("Expected number of actionOutome records not what expected",
				createdAvailableActionContentStore.size(), 10);

		DefinitionDetail definitionDetails = createdDefinitionDetailContentStore.values().iterator().next();

		List<String> possibleActions = Arrays.asList("Cancel Workflow", "Edit", "QA Fails", "QA Passes", "Approve",
				"Reject Edit", "Reject Review", "Create Workflow Process");

		List<String> possibleStates = Arrays.asList("Assigned", "Canceled During Edit", "Canceled During Review",
				"Canceled During Approval", "Ready for Edit", "Ready for Approve", "Modeling Review Complete",
				"Ready for Review");

		Set<AvailableAction> identifiedCanceledActions = new HashSet<>();
		Set<AvailableAction> identifiedConcludedActions = new HashSet<>();
		Set<AvailableAction> identifiedStartTypeActions = new HashSet<>();
		Set<String> identifiedEditingActions = new HashSet<>();

		for (AvailableAction entry : createdAvailableActionContentStore.values()) {
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

		Set<AvailableAction> concludedActions =  LookupService.get().getService(WorkflowProvider.class).getBPMNInfo().getEndWorkflowTypeMap().get(EndWorkflowType.CONCLUDED);
		Set<AvailableAction> canceledActions = LookupService.get().getService(WorkflowProvider.class).getBPMNInfo().getEndWorkflowTypeMap().get(EndWorkflowType.CANCELED);

		Assert.assertEquals(canceledActions, identifiedCanceledActions);
		Assert.assertEquals(concludedActions, identifiedConcludedActions);

		Map<UUID, Set<AvailableAction>> defStartMap = LookupService.get().getService(WorkflowProvider.class).getBPMNInfo().getDefinitionStartActionMap();
		Assert.assertEquals(defStartMap.keySet().size(), 1);
		Assert.assertEquals(defStartMap.size(), identifiedStartTypeActions.size());
		Assert.assertEquals(defStartMap.keySet().iterator().next(),
				definitionDetails.getId());
		Assert.assertEquals(defStartMap.get(definitionDetails.getId()), identifiedStartTypeActions);

		Assert.assertEquals(LookupService.get().getService(WorkflowProvider.class).getBPMNInfo().getEditStatesMap().get(definitionDetails.getId()), identifiedEditingActions);
	}

	/**
	 * Test proper available actions following import of a bpmn2 file
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testStaticBpmnAvailableActions() throws Exception {
		Map<String, Set<AvailableAction>> actionMap = new HashMap<>();

		for (AvailableAction action : LookupService.get().getService(WorkflowProvider.class).getAvailableActionStore().values()) {
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
		Assert.assertEquals(PrismeRole.AUTOMATED, actions.iterator().next().getRole());
	}

	private void assertReadyForEditActions(Set<AvailableAction> actions) {
		Assert.assertEquals(2, actions.size());

		for (AvailableAction act : actions) {
			Assert.assertEquals(PrismeRole.EDITOR, act.getRole());

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
			Assert.assertEquals(PrismeRole.REVIEWER, act.getRole());

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
			Assert.assertEquals(PrismeRole.APPROVER, act.getRole());

			if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertEquals("Canceled During Approval", act.getOutcomeState());
			} else if (act.getAction().equals("Reject Edit")) {
				Assert.assertEquals("Ready for Edit", act.getOutcomeState());
			} else if (act.getAction().equals("Reject Review")) {
				Assert.assertEquals("Reject Review", act.getOutcomeState());
			} else if (act.getAction().equals("Approve")) {
				Assert.assertEquals("Modeling Review Complete", act.getOutcomeState());
			} else {
				Assert.fail();
			}
		}

	}
}

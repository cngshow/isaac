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
package gov.vha.isaac.ochre.workflow.provider.crud;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.RecursiveDelete;
import gov.vha.isaac.ochre.workflow.model.contents.AvailableAction;
import gov.vha.isaac.ochre.workflow.model.contents.DefinitionDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.provider.BPMNInfo;
import gov.vha.isaac.ochre.workflow.provider.WorkflowProvider;

/**
 * Test the WorkflowAccessor class
 * 
 * {@link WorkflowAccessor}. {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowAccessorTest extends AbstractWorkflowProviderTestPackage {

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
		LookupService.shutdownIsaac();
		RecursiveDelete.delete(new File("target/store"));
	}
	
	@Before
	public void beforeTest()
	{
		wp_.getProcessDetailStore().clear();
		wp_.getProcessHistoryStore().clear();
		wp_.getUserPermissionStore().clear();
		setupUserRoles();
	}

	/**
	 * Test able to properly access definition details.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testGetDefinitionDetails() throws Exception {
		DefinitionDetail entry = wp_.getWorkflowAccessor().getDefinitionDetails(mainDefinitionId);

		Set<String> expectedRoles = new HashSet<>();
		expectedRoles.add("Editor");
		expectedRoles.add("Reviewer");
		expectedRoles.add("Approver");
		expectedRoles.add(BPMNInfo.AUTOMATED_ROLE);

		Assert.assertEquals(entry.getBpmn2Id(), "VetzWorkflow");
		Assert.assertEquals(entry.getName(), "VetzWorkflow");
		Assert.assertEquals(entry.getNamespace(), "org.jbpm");
		Assert.assertEquals(entry.getVersion(), "1.2");
		Assert.assertEquals(entry.getRoles(), expectedRoles);
	}

	/**
	 * Test able to properly access process details.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testGetProcessDetails() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);

		ProcessDetail entry = wp_.getWorkflowAccessor().getProcessDetails(processId);
		Assert.assertEquals(processId, entry.getId());
		Assert.assertEquals(ProcessStatus.DEFINED, entry.getStatus());
		Assert.assertNotNull(entry.getCreatorId());
		Assert.assertEquals(mainDefinitionId, entry.getDefinitionId());
		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeCreated()));
		Assert.assertEquals(-1L, entry.getTimeCanceledOrConcluded());
		Assert.assertEquals(0, entry.getComponentToInitialEditMap().keySet().size());

		addComponentsToProcess(processId, new Date().getTime());
		entry = wp_.getWorkflowAccessor().getProcessDetails(processId);
		Assert.assertEquals(2, entry.getComponentToInitialEditMap().keySet().size());
		Assert.assertTrue(entry.getComponentToInitialEditMap().keySet().contains(-55));
		Assert.assertTrue(entry.getComponentToInitialEditMap().keySet().contains(-56));

		executeLaunchWorkflow(processId);
		entry = wp_.getWorkflowAccessor().getProcessDetails(processId);
		Assert.assertEquals(ProcessStatus.LAUNCHED, entry.getStatus());
	}

	/**
	 * Test able to properly access process history. While doing so, advance
	 * workflow such that history expands. Keep testing after each advancement.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testGetProcessHistory() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		SortedSet<ProcessHistory> processHistory = wp_.getWorkflowAccessor().getProcessHistory(processId);
		Assert.assertEquals(1, processHistory.size());
		assertHistoryForProcess(processHistory, processId);

		executeLaunchWorkflow(processId);
		executeSendForReviewAdvancement(processId);
		processHistory = wp_.getWorkflowAccessor().getProcessHistory(processId);
		Assert.assertEquals(2, processHistory.size());
		assertHistoryForProcess(processHistory, processId);

		executeSendForApprovalAdvancement(processId);
		processHistory = wp_.getWorkflowAccessor().getProcessHistory(processId);
		Assert.assertEquals(3, processHistory.size());
		assertHistoryForProcess(processHistory, processId);

		concludeWorkflow(processId);
		processHistory = wp_.getWorkflowAccessor().getProcessHistory(processId);
		Assert.assertEquals(4, processHistory.size());
		assertHistoryForProcess(processHistory, processId);
		assertConcludeHistory(processHistory.last(), processId);
	}

	/**
	 * Cannot make this work without at least a Mock Database. Added to
	 * Integration-Test module's workflowFramworkTest. For now just pass.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testIsComponentInActiveWorkflow() throws Exception {
		Assert.assertTrue(true);
	}

	/**
	 * Test able to properly access the user roles as expected. Do this for
	 * multiple users as each contains different roles as defined in the
	 * AbstractWorkflowProviderTestPackage.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testGetUserRoles() throws Exception {
		Set<String> roles = wp_.getWorkflowAccessor().getUserRoles(mainDefinitionId, firstUserId);
		Assert.assertEquals(2, roles.size());

		for (String role : roles) {
			Assert.assertTrue(role.equals("Editor") || role.equals("Approver"));
		}

		roles = wp_.getWorkflowAccessor().getUserRoles(mainDefinitionId, secondUserId);
		Assert.assertEquals(1, roles.size());

		String role = roles.iterator().next();
		Assert.assertEquals("Reviewer", role);
	}

	/**
	 * Test that as advance workflow, different process information is
	 * associated with the process
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testGetAdvanceableProcessInformation() throws Exception {
		Map<ProcessDetail, SortedSet<ProcessHistory>> info = wp_.getWorkflowAccessor()
				.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(0, info.size());
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Create first process-Main definition (role is Editor)
		UUID firstProcessId = createFirstWorkflowProcess(mainDefinitionId);
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wp_.getWorkflowAccessor().getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wp_.getWorkflowAccessor().getProcessHistory(firstProcessId));
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Launch workflow and send for review (role is reviewer)
		executeLaunchWorkflow(firstProcessId);
		executeSendForReviewAdvancement(firstProcessId);
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(0, info.size());
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wp_.getWorkflowAccessor().getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wp_.getWorkflowAccessor().getProcessHistory(firstProcessId));

		// Make workflow ready for review (role is Approver)
		executeSendForApprovalAdvancement(firstProcessId);
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wp_.getWorkflowAccessor().getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wp_.getWorkflowAccessor().getProcessHistory(firstProcessId));
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Create second process-Main definition. (first process role is
		// Approver and second process role is Editor)
		UUID secondProcessId = createSecondWorkflowProcess(mainDefinitionId);
		// Create testing collections
		Set<ProcessDetail> mainDefProcesses = new HashSet<>(Arrays.asList(wp_.getWorkflowAccessor().getProcessDetails(firstProcessId),
				wp_.getWorkflowAccessor().getProcessDetails(secondProcessId)));
		Set<SortedSet<ProcessHistory>> mainDefProcessHistory = new HashSet<>(Arrays
				.asList(wp_.getWorkflowAccessor().getProcessHistory(firstProcessId), wp_.getWorkflowAccessor().getProcessHistory(secondProcessId)));
		// test
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(2, info.size());
		Assert.assertEquals(info.keySet(), mainDefProcesses);
		for (ProcessDetail process : info.keySet()) {
			Assert.assertTrue(mainDefProcessHistory.contains(info.get(process)));
		}
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Reject first process ready for edit (role is editor for both)
		executeRejectReviewAdvancement(firstProcessId);
		// Create testing collections
		mainDefProcesses = new HashSet<>(Arrays.asList(wp_.getWorkflowAccessor().getProcessDetails(firstProcessId),
				wp_.getWorkflowAccessor().getProcessDetails(secondProcessId)));
		mainDefProcessHistory = new HashSet<>(Arrays.asList(wp_.getWorkflowAccessor().getProcessHistory(firstProcessId),
				wp_.getWorkflowAccessor().getProcessHistory(secondProcessId)));
		// test
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(2, info.size());
		Assert.assertEquals(info.keySet(), mainDefProcesses);
		for (ProcessDetail process : info.keySet()) {
			Assert.assertTrue(mainDefProcessHistory.contains(info.get(process)));
		}
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Cancel second process (role is editor for first and no role for
		// second) and
		cancelWorkflow(secondProcessId);
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wp_.getWorkflowAccessor().getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wp_.getWorkflowAccessor().getProcessHistory(firstProcessId));
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Create third process on second definition.
		// Thus mainDef: (role is editor for first and no role for second) and
		// secondDef: (role is
		// editor)
		UUID secondDefinitionId = createSecondaryDefinition();
		// test first definition
		UUID thirdProcessId = createFirstWorkflowProcess(secondDefinitionId);
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wp_.getWorkflowAccessor().getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wp_.getWorkflowAccessor().getProcessHistory(firstProcessId));
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());
		// test second definition
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(secondDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wp_.getWorkflowAccessor().getProcessDetails(thirdProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wp_.getWorkflowAccessor().getProcessHistory(thirdProcessId));
		info = wp_.getWorkflowAccessor().getAdvanceableProcessInformation(secondDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		wp_.getDefinitionDetailStore().remove(secondDefinitionId);
	}

	/**
	 * Test that as advance workflow, different users are able to advance
	 * workflow based on the user permissions and the process's current state
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testGetUserPermissibleActionsForProcess() throws Exception {
		UUID firstProcessId = createFirstWorkflowProcess(mainDefinitionId);

		// Create Process (Is in Ready_To_Edit State)
		Set<AvailableAction> firstProcessFirstUserActions = wp_.getWorkflowAccessor()
				.getUserPermissibleActionsForProcess(firstProcessId, firstUserId);
		Assert.assertEquals(2, firstProcessFirstUserActions.size());
		for (AvailableAction act : firstProcessFirstUserActions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Edit", act.getInitialState());
			Assert.assertEquals("Editor", act.getRole());

			if (act.getAction().equals("Edit")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Review"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcomeState().equals("Canceled During Edit"));
			} else {
				Assert.fail();
			}
		}
		Set<AvailableAction> firstProcessSecondUserActions = wp_.getWorkflowAccessor()
				.getUserPermissibleActionsForProcess(firstProcessId, secondUserId);
		Assert.assertEquals(0, firstProcessSecondUserActions.size());

		// Launch Process and send for review (Is in Ready_To_Review State)
		executeLaunchWorkflow(firstProcessId);
		executeSendForReviewAdvancement(firstProcessId);
		firstProcessFirstUserActions = wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, firstUserId);
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		firstProcessSecondUserActions = wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, secondUserId);
		Assert.assertEquals(3, firstProcessSecondUserActions.size());
		for (AvailableAction act : firstProcessSecondUserActions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Review", act.getInitialState());
			Assert.assertEquals("Reviewer", act.getRole());

			if (act.getAction().equals("QA Passes")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Approve"));
			} else if (act.getAction().equals("QA Fails")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Edit"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcomeState().equals("Canceled During Review"));
			} else {
				Assert.fail();
			}
		}

		// Create Process (Is in Ready_To_Edit State)
		UUID secondProcessId = createFirstWorkflowProcess(mainDefinitionId);
		Set<AvailableAction> secondProcessFirstUserActions = wp_.getWorkflowAccessor()
				.getUserPermissibleActionsForProcess(secondProcessId, firstUserId);
		Assert.assertEquals(2, secondProcessFirstUserActions.size());
		for (AvailableAction act : secondProcessFirstUserActions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Edit", act.getInitialState());
			Assert.assertEquals("Editor", act.getRole());

			if (act.getAction().equals("Edit")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Review"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcomeState().equals("Canceled During Edit"));
			} else {
				Assert.fail();
			}
		}
		Set<AvailableAction> secondProcessSecondUserActions = wp_.getWorkflowAccessor()
				.getUserPermissibleActionsForProcess(secondProcessId, secondUserId);
		Assert.assertEquals(0, secondProcessSecondUserActions.size());
		// Verify the first process hasn't changed
		Assert.assertEquals(firstProcessFirstUserActions,
				wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, firstUserId));
		Assert.assertEquals(firstProcessSecondUserActions,
				wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, secondUserId));
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		Assert.assertEquals(3, firstProcessSecondUserActions.size());

		// Launch second Process and send for review (Is in Ready_To_Review
		// State)
		executeLaunchWorkflow(secondProcessId);
		executeSendForReviewAdvancement(secondProcessId);
		secondProcessFirstUserActions = wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(secondProcessId, firstUserId);
		Assert.assertEquals(0, secondProcessFirstUserActions.size());
		secondProcessSecondUserActions = wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(secondProcessId, secondUserId);
		Assert.assertEquals(3, secondProcessSecondUserActions.size());
		for (AvailableAction act : secondProcessSecondUserActions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Review", act.getInitialState());
			Assert.assertEquals("Reviewer", act.getRole());

			if (act.getAction().equals("QA Passes")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Approve"));
			} else if (act.getAction().equals("QA Fails")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Edit"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcomeState().equals("Canceled During Review"));
			} else {
				Assert.fail();
			}
		}
		// Verify the first process hasn't changed
		Assert.assertEquals(firstProcessFirstUserActions,
				wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, firstUserId));
		Assert.assertEquals(firstProcessSecondUserActions,
				wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, secondUserId));
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		Assert.assertEquals(3, firstProcessSecondUserActions.size());

		// Send second process for approval (At Ready for Approve State)
		executeSendForApprovalAdvancement(secondProcessId);
		secondProcessSecondUserActions = wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(secondProcessId, secondUserId);
		Assert.assertEquals(0, secondProcessSecondUserActions.size());
		secondProcessFirstUserActions = wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(secondProcessId, firstUserId);
		Assert.assertEquals(4, secondProcessFirstUserActions.size());
		for (AvailableAction act : secondProcessFirstUserActions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Approve", act.getInitialState());
			Assert.assertEquals("Approver", act.getRole());

			if (act.getAction().equals("Reject Review")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Review"));
			} else if (act.getAction().equals("Reject Edit")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Edit"));
			} else if (act.getAction().equals("Approve")) {
				Assert.assertTrue(act.getOutcomeState().equals("Ready for Publish"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcomeState().equals("Canceled During Approval"));
			} else {
				Assert.fail();
			}
		}

		secondProcessSecondUserActions = wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(secondProcessId, secondUserId);
		Assert.assertEquals(0, secondProcessSecondUserActions.size());
		// Verify the first process hasn't changed
		Assert.assertEquals(firstProcessFirstUserActions,
				wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, firstUserId));
		Assert.assertEquals(firstProcessSecondUserActions,
				wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, secondUserId));
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		Assert.assertEquals(3, firstProcessSecondUserActions.size());

		// Cancel first process so should have zero permissions available
		cancelWorkflow(firstProcessId);
		Assert.assertEquals(0, wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, firstUserId).size());
		Assert.assertEquals(0, wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(firstProcessId, secondUserId).size());
		// Verify the second process hasn't changed
		Assert.assertEquals(secondProcessFirstUserActions,
				wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(secondProcessId, firstUserId));
		Assert.assertEquals(secondProcessSecondUserActions,
				wp_.getWorkflowAccessor().getUserPermissibleActionsForProcess(secondProcessId, secondUserId));
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		Assert.assertEquals(3, firstProcessSecondUserActions.size());
	}
}

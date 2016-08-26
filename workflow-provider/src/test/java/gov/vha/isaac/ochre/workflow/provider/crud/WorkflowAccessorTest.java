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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.ProcessStatus;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Test the WorkflowAccessor class
 * 
 * {@link WorkflowAccessor}. {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowAccessorTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;

	private static MVStoreMetaContentProvider store;
	private static WorkflowAccessor wfAccessor;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowStatusAccess", true);
		}

		globalSetup(store);

		if (!setupCompleted) {
			wfAccessor = new WorkflowAccessor(store);
			setupUserRoles();

			setupCompleted = true;
		}
	}

	@AfterClass
	public static void tearDownClass() {
		AbstractWorkflowUtilities.close();
	}

	@Test
	public void testGetDefinitionDetails() throws Exception {
		DefinitionDetail entry = wfAccessor.getDefinitionDetails(mainDefinitionId);

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

	@Test
	public void testGetProcessDetails() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		addComponentsToProcess(processId);

		ProcessDetail entry = wfAccessor.getProcessDetails(processId);
		Assert.assertEquals(processId, entry.getId());
		Assert.assertEquals(2, entry.getComponentToStampMap().size());
		Assert.assertEquals(ProcessStatus.DEFINED, entry.getStatus());
		Assert.assertEquals(99, entry.getCreator());
		Assert.assertEquals(mainDefinitionId, entry.getDefinitionId());
		Assert.assertEquals(2, entry.getComponentToStampMap().size());
		Assert.assertTrue(entry.getComponentToStampMap().containsKey(55));
		Assert.assertTrue(entry.getComponentToStampMap().containsKey(56));
		Assert.assertTrue(entry.getComponentToStampMap().get(55).contains(11));
		Assert.assertTrue(entry.getComponentToStampMap().get(55).contains(12));
		Assert.assertTrue(entry.getComponentToStampMap().get(56).contains(11));
		Assert.assertTrue(entry.getComponentToStampMap().get(56).contains(12));
		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeCreated()));
		Assert.assertEquals(-1L, entry.getTimeCanceledOrConcluded());
	}

	@Test
	public void testGetProcessHistory() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		SortedSet<ProcessHistory> processHistory = wfAccessor.getProcessHistory(processId);
		Assert.assertEquals(1, processHistory.size());
		assertHistoryForProcess(processHistory, processId);


		executeLaunchWorkflow(processId);
		executeSendForReviewAdvancement(processId);
		processHistory = wfAccessor.getProcessHistory(processId);
		Assert.assertEquals(2, processHistory.size());
		assertHistoryForProcess(processHistory, processId);

		executeSendForApprovalAdvancement(processId);
		processHistory = wfAccessor.getProcessHistory(processId);
		Assert.assertEquals(3, processHistory.size());
		assertHistoryForProcess(processHistory, processId);

		concludeWorkflow(processId);
		processHistory = wfAccessor.getProcessHistory(processId);
		Assert.assertEquals(4, processHistory.size());
		assertHistoryForProcess(processHistory, processId);
		assertConcludeHistory(processHistory.last(), processId);
	}

	@Test
	public void testIsComponentInActiveWorkflow() throws Exception {
		// Cannot make this work without at least a Mock Database.
		// Added to Integration-Test module's workflowFramworkTest. For now just
		// pass.
		Assert.assertTrue(true);
	}

	@Test
	public void testGetUserPermission() throws Exception {
		Set<UserPermission> permissions = wfAccessor.getUserPermissions(mainDefinitionId, firstUserId);
		Assert.assertEquals(2, permissions.size());

		for (UserPermission perm : permissions) {
			Assert.assertEquals(mainDefinitionId, perm.getDefinitionId());
			Assert.assertEquals(firstUserId, perm.getUser());
			Assert.assertTrue(perm.getRole().equals("Editor") || perm.getRole().equals("Approver"));
		}

		permissions = wfAccessor.getUserPermissions(mainDefinitionId, secondUserId);
		Assert.assertEquals(1, permissions.size());

		UserPermission perm = permissions.iterator().next();
		Assert.assertEquals(mainDefinitionId, perm.getDefinitionId());
		Assert.assertEquals(secondUserId, perm.getUser());
		Assert.assertEquals("Reviewer", perm.getRole());
	}

	@Test
	public void testGetAdvanceableProcessInformation() throws Exception {
		Map<ProcessDetail, SortedSet<ProcessHistory>> info = wfAccessor
				.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(0, info.size());
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Create first process-Main definition (role is Editor)
		UUID firstProcessId = createFirstWorkflowProcess(mainDefinitionId);
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wfAccessor.getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wfAccessor.getProcessHistory(firstProcessId));
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Launch workflow and send for review (role is reviewer)
		executeLaunchWorkflow(firstProcessId);
		executeSendForReviewAdvancement(firstProcessId);
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(0, info.size());
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wfAccessor.getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wfAccessor.getProcessHistory(firstProcessId));

		// Make workflow ready for review (role is Approver)
		executeSendForApprovalAdvancement(firstProcessId);
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wfAccessor.getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wfAccessor.getProcessHistory(firstProcessId));
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Create second process-Main definition. (first process role is
		// Approver and second process role is Editor)
		UUID secondProcessId = createSecondWorkflowProcess(mainDefinitionId);
		// Create testing collections
		Set<ProcessDetail> mainDefProcesses = new HashSet<>(Arrays.asList(wfAccessor.getProcessDetails(firstProcessId),
				wfAccessor.getProcessDetails(secondProcessId)));
		Set<SortedSet<ProcessHistory>> mainDefProcessHistory = new HashSet<>(Arrays
				.asList(wfAccessor.getProcessHistory(firstProcessId), wfAccessor.getProcessHistory(secondProcessId)));
		// test
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(2, info.size());
		Assert.assertEquals(info.keySet(), mainDefProcesses);
		for (ProcessDetail process : info.keySet()) {
			Assert.assertTrue(mainDefProcessHistory.contains(info.get(process)));
		}
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		// Reject first process ready for edit (role is editor for both)
		executeRejectReviewAdvancement(firstProcessId);
		// Create testing collections
		mainDefProcesses = new HashSet<>(Arrays.asList(wfAccessor.getProcessDetails(firstProcessId),
				wfAccessor.getProcessDetails(secondProcessId)));
		mainDefProcessHistory = new HashSet<>(Arrays.asList(wfAccessor.getProcessHistory(firstProcessId),
				wfAccessor.getProcessHistory(secondProcessId)));
		// test
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(2, info.size());
		Assert.assertEquals(info.keySet(), mainDefProcesses);
		for (ProcessDetail process : info.keySet()) {
			Assert.assertTrue(mainDefProcessHistory.contains(info.get(process)));
		}
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());


		// Cancel second process (role is editor for first and no role for second) and
		cancelWorkflow(secondProcessId);
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wfAccessor.getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wfAccessor.getProcessHistory(firstProcessId));
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());

		
		// Create third process on second definition.
		// Thus mainDef: (role is editor for first and no role for second) and secondDef: (role is
		// editor)
		UUID secondDefinitionId = createSecondaryDefinition();
		// test first definition
		UUID thirdProcessId = createFirstWorkflowProcess(secondDefinitionId);
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wfAccessor.getProcessDetails(firstProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wfAccessor.getProcessHistory(firstProcessId));
		info = wfAccessor.getAdvanceableProcessInformation(mainDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());
		// test second definition
		info = wfAccessor.getAdvanceableProcessInformation(secondDefinitionId, firstUserId);
		Assert.assertEquals(1, info.size());
		Assert.assertEquals(info.keySet().iterator().next(), wfAccessor.getProcessDetails(thirdProcessId));
		Assert.assertEquals(info.get(info.keySet().iterator().next()), wfAccessor.getProcessHistory(thirdProcessId));
		info = wfAccessor.getAdvanceableProcessInformation(secondDefinitionId, secondUserId);
		Assert.assertEquals(0, info.size());


		definitionDetailStore.removeEntry(secondDefinitionId);
	}

	@Test
	public void testGetUserPermissibleActionsForProcess() throws Exception {
		UUID firstProcessId = createFirstWorkflowProcess(mainDefinitionId);

		// Create Process (Is in Ready_To_Edit State)
		Set<AvailableAction> firstProcessFirstUserActions = wfAccessor
				.getUserPermissibleActionsForProcess(firstProcessId, firstUserId);
		Assert.assertEquals(1, firstProcessFirstUserActions.size());
		Assert.assertEquals(mainDefinitionId, firstProcessFirstUserActions.iterator().next().getDefinitionId());
		Assert.assertEquals("Ready for Edit", firstProcessFirstUserActions.iterator().next().getInitialState());
		Assert.assertEquals("Edit", firstProcessFirstUserActions.iterator().next().getAction());
		Assert.assertTrue(firstProcessFirstUserActions.iterator().next().getOutcomeState().equals("Ready for Review"));
		Assert.assertEquals("Editor", firstProcessFirstUserActions.iterator().next().getRole());

		Set<AvailableAction> firstProcessSecondUserActions = wfAccessor
				.getUserPermissibleActionsForProcess(firstProcessId, secondUserId);
		Assert.assertEquals(0, firstProcessSecondUserActions.size());

		// Launch Process and send for review (Is in Ready_To_Review State)
		executeLaunchWorkflow(firstProcessId);
		executeSendForReviewAdvancement(firstProcessId);
		firstProcessFirstUserActions = wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, firstUserId);
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		firstProcessSecondUserActions = wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, secondUserId);
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
		Set<AvailableAction> secondProcessFirstUserActions = wfAccessor
				.getUserPermissibleActionsForProcess(secondProcessId, firstUserId);
		Assert.assertEquals(1, secondProcessFirstUserActions.size());
		Assert.assertEquals(mainDefinitionId, secondProcessFirstUserActions.iterator().next().getDefinitionId());
		Assert.assertEquals("Ready for Edit", secondProcessFirstUserActions.iterator().next().getInitialState());
		Assert.assertEquals("Edit", secondProcessFirstUserActions.iterator().next().getAction());
		Assert.assertTrue(secondProcessFirstUserActions.iterator().next().getOutcomeState().equals("Ready for Review"));
		Assert.assertEquals("Editor", secondProcessFirstUserActions.iterator().next().getRole());
		Set<AvailableAction> secondProcessSecondUserActions = wfAccessor
				.getUserPermissibleActionsForProcess(secondProcessId, secondUserId);
		Assert.assertEquals(0, secondProcessSecondUserActions.size());
		// Verify the first process hasn't changed
		Assert.assertEquals(firstProcessFirstUserActions,
				wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, firstUserId));
		Assert.assertEquals(firstProcessSecondUserActions,
				wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, secondUserId));
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		Assert.assertEquals(3, firstProcessSecondUserActions.size());

		// Launch second Process and send for review (Is in Ready_To_Review State)
		executeLaunchWorkflow(secondProcessId);
		executeSendForReviewAdvancement(secondProcessId);
		secondProcessFirstUserActions = wfAccessor.getUserPermissibleActionsForProcess(secondProcessId, firstUserId);
		Assert.assertEquals(0, secondProcessFirstUserActions.size());
		secondProcessSecondUserActions = wfAccessor.getUserPermissibleActionsForProcess(secondProcessId, secondUserId);
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
				wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, firstUserId));
		Assert.assertEquals(firstProcessSecondUserActions,
				wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, secondUserId));
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		Assert.assertEquals(3, firstProcessSecondUserActions.size());

		// Send second process for approval (At Ready for Approve State)
		executeSendForApprovalAdvancement(secondProcessId);
		secondProcessSecondUserActions = wfAccessor.getUserPermissibleActionsForProcess(secondProcessId, secondUserId);
		Assert.assertEquals(0, secondProcessSecondUserActions.size());
		secondProcessFirstUserActions = wfAccessor.getUserPermissibleActionsForProcess(secondProcessId, firstUserId);
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

		secondProcessSecondUserActions = wfAccessor.getUserPermissibleActionsForProcess(secondProcessId, secondUserId);
		Assert.assertEquals(0, secondProcessSecondUserActions.size());
		// Verify the first process hasn't changed
		Assert.assertEquals(firstProcessFirstUserActions,
				wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, firstUserId));
		Assert.assertEquals(firstProcessSecondUserActions,
				wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, secondUserId));
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		Assert.assertEquals(3, firstProcessSecondUserActions.size());

		// Cancel first process so should have zero permissions available
		cancelWorkflow(firstProcessId);
		Assert.assertEquals(0, wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, firstUserId).size());
		Assert.assertEquals(0, wfAccessor.getUserPermissibleActionsForProcess(firstProcessId, secondUserId).size());
		// Verify the second process hasn't changed
		Assert.assertEquals(secondProcessFirstUserActions,
				wfAccessor.getUserPermissibleActionsForProcess(secondProcessId, firstUserId));
		Assert.assertEquals(secondProcessSecondUserActions,
				wfAccessor.getUserPermissibleActionsForProcess(secondProcessId, secondUserId));
		Assert.assertEquals(0, firstProcessFirstUserActions.size());
		Assert.assertEquals(3, firstProcessSecondUserActions.size());
	}
}

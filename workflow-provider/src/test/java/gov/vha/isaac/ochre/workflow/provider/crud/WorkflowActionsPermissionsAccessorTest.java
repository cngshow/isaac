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
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.SubjectMatter;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Test the WorkflowInitializerConcluder class
 * 
 * {@link WorkflowActionsPermissionsAccessor}.
 * {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowActionsPermissionsAccessorTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;
	private static WorkflowActionsPermissionsAccessor accessor;

	private static MVStoreMetaContentProvider store;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowPermsAccess", true);
			accessor = new WorkflowActionsPermissionsAccessor(store);
		}

		globalSetup(store);

		if (!setupCompleted) {
			setupUserRoles();
			createMainWorkflowProcess(mainDefinitionId);
			secondaryProcessId = createSecondaryWorkflowProcess(mainDefinitionId, secondaryConceptsForTesting);

			launchWorkflow(mainProcessId);
			launchWorkflow(secondaryProcessId);

			executeInitialAdvancement(mainProcessId);
			setupCompleted = true;
		}
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
	public void testAGetUserRoles() throws Exception {
		Set<String> roles = accessor.getUserRoles(mainDefinitionId, mainUserId);
		Assert.assertEquals(2, roles.size());
		Assert.assertTrue(roles.contains("Editor"));
		Assert.assertTrue(roles.contains("Approver"));

		roles = accessor.getUserRoles(mainDefinitionId, secondaryUserId);
		Assert.assertEquals(1, roles.size());
		Assert.assertTrue(roles.contains("Reviewer"));
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testBGetAllPermissionsForUser() throws Exception {
		Set<UserPermission> permissions = accessor.getAllPermissionsForUser(mainDefinitionId, mainUserId);
		Assert.assertEquals(2, permissions.size());

		for (UserPermission perm : permissions) {
			Assert.assertEquals(mainDefinitionId, perm.getDefinitionId());
			Assert.assertEquals(mainUserId, perm.getUser());
			Assert.assertTrue(perm.getRole().equals("Editor") || perm.getRole().equals("Approver"));
		}

		permissions = accessor.getAllPermissionsForUser(mainDefinitionId, secondaryUserId);
		Assert.assertEquals(1, permissions.size());

		UserPermission perm = permissions.iterator().next();
		Assert.assertEquals(mainDefinitionId, perm.getDefinitionId());
		Assert.assertEquals(secondaryUserId, perm.getUser());
		Assert.assertEquals("Reviewer", perm.getRole());
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testCGetAvailableActionsForState() throws Exception {
		Set<AvailableAction> actions = accessor.getAvailableActionsForState(mainDefinitionId, "Ready for Edit");
		Assert.assertEquals(1, actions.size());
		AvailableAction singleAction = actions.iterator().next();
		Assert.assertEquals(mainDefinitionId, singleAction.getDefinitionId());
		Assert.assertEquals("Ready for Edit", singleAction.getCurrentState());
		Assert.assertEquals("Editor", singleAction.getRole());
		Assert.assertEquals("Ready for Review", singleAction.getOutcome());
		Assert.assertEquals("Edit", singleAction.getAction());

		actions = accessor.getAvailableActionsForState(mainDefinitionId, "Ready for Review");
		Assert.assertEquals(3, actions.size());
		for (AvailableAction act : actions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Review", act.getCurrentState());
			Assert.assertEquals("Reviewer", act.getRole());

			if (act.getAction().equals("QA Passes")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Approve"));
			} else if (act.getAction().equals("QA Fails")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Edit"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcome().equals("Canceled"));
			} else {
				Assert.fail();
			}
		}

		actions = accessor.getAvailableActionsForState(mainDefinitionId, "Ready for Approve");
		Assert.assertEquals(4, actions.size());
		for (AvailableAction act : actions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Approve", act.getCurrentState());
			Assert.assertEquals("Approver", act.getRole());

			if (act.getAction().equals("Reject Review")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Review"));
			} else if (act.getAction().equals("Reject Edit")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Edit"));
			} else if (act.getAction().equals("Approve")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Publish"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcome().equals("Canceled"));
			} else {
				Assert.fail();
			}
		}

		actions = accessor.getAvailableActionsForState(mainDefinitionId, "Ready for Publish");
		Assert.assertEquals(0, actions.size());

		actions = accessor.getAvailableActionsForState(mainDefinitionId, "Canceled");
		Assert.assertEquals(0, actions.size());
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testDGetUserPermissibleActionsForProcess() throws Exception {
		Set<AvailableAction> actions = accessor.getUserPermissibleActionsForProcess(mainProcessId, mainUserId);
		Assert.assertEquals(0, actions.size());

		actions = accessor.getUserPermissibleActionsForProcess(mainProcessId, secondaryUserId);
		Assert.assertEquals(3, actions.size());
		for (AvailableAction act : actions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Review", act.getCurrentState());
			Assert.assertEquals("Reviewer", act.getRole());

			if (act.getAction().equals("QA Passes")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Approve"));
			} else if (act.getAction().equals("QA Fails")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Edit"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcome().equals("Canceled"));
			} else {
				Assert.fail();
			}
		}

		actions = accessor.getUserPermissibleActionsForProcess(secondaryProcessId, mainUserId);
		Assert.assertEquals(1, actions.size());
		AvailableAction singleAction = actions.iterator().next();
		Assert.assertEquals(mainDefinitionId, singleAction.getDefinitionId());
		Assert.assertEquals("Ready for Edit", singleAction.getCurrentState());
		Assert.assertEquals("Editor", singleAction.getRole());
		Assert.assertEquals("Ready for Review", singleAction.getOutcome());
		Assert.assertEquals("Edit", singleAction.getAction());

		actions = accessor.getUserPermissibleActionsForProcess(secondaryProcessId, secondaryUserId);
		Assert.assertEquals(0, actions.size());

		// Should make secondaryProcess look like mainProcess
		executeInitialAdvancement(secondaryProcessId);

		actions = accessor.getUserPermissibleActionsForProcess(secondaryProcessId, mainUserId);
		Assert.assertEquals(0, actions.size());

		actions = accessor.getUserPermissibleActionsForProcess(secondaryProcessId, secondaryUserId);
		Assert.assertEquals(3, actions.size());
		for (AvailableAction act : actions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Review", act.getCurrentState());
			Assert.assertEquals("Reviewer", act.getRole());

			if (act.getAction().equals("QA Passes")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Approve"));
			} else if (act.getAction().equals("QA Fails")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Edit"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcome().equals("Canceled"));
			} else {
				Assert.fail();
			}
		}

		executeSecondAdvancement(secondaryProcessId);
		actions = accessor.getUserPermissibleActionsForProcess(secondaryProcessId, secondaryUserId);
		Assert.assertEquals(0, actions.size());

		actions = accessor.getUserPermissibleActionsForProcess(secondaryProcessId, mainUserId);
		Assert.assertEquals(4, actions.size());
		for (AvailableAction act : actions) {
			Assert.assertEquals(mainDefinitionId, act.getDefinitionId());
			Assert.assertEquals("Ready for Approve", act.getCurrentState());
			Assert.assertEquals("Approver", act.getRole());

			if (act.getAction().equals("Reject Review")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Review"));
			} else if (act.getAction().equals("Reject Edit")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Edit"));
			} else if (act.getAction().equals("Approve")) {
				Assert.assertTrue(act.getOutcome().equals("Ready for Publish"));
			} else if (act.getAction().equals("Cancel Workflow")) {
				Assert.assertTrue(act.getOutcome().equals("Canceled"));
			} else {
				Assert.fail();
			}
		}

	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testEGetLatestActivePermissibleByRole() throws Exception {
		Map<String, Set<ProcessHistory>> stateHistoryMap = accessor.getLatestActivePermissibleByRole(mainDefinitionId,
				mainUserId);

		Assert.assertEquals(1, stateHistoryMap.keySet().size());
		Set<ProcessHistory> allHistory = stateHistoryMap.get(stateHistoryMap.keySet().iterator().next());
		Assert.assertEquals(1, allHistory.size());

		ProcessHistory stateHistory = allHistory.iterator().next();
		Assert.assertEquals(secondaryProcessId, stateHistory.getProcessId());
		Assert.assertEquals(mainUserId, stateHistory.getWorkflowUser());
		Assert.assertEquals(secondHistoryTimestamp, stateHistory.getTimeAdvanced());
		Assert.assertEquals(secondState, stateHistory.getState());
		Assert.assertEquals(secondAction, stateHistory.getAction());
		Assert.assertEquals(secondOutcome, stateHistory.getOutcome());
		Assert.assertEquals(secondComment, stateHistory.getComment());

		stateHistoryMap = accessor.getLatestActivePermissibleByRole(mainDefinitionId, secondaryUserId);
		Assert.assertEquals(1, stateHistoryMap.keySet().size());
		allHistory = stateHistoryMap.get(stateHistoryMap.keySet().iterator().next());
		Assert.assertEquals(1, allHistory.size());

		stateHistory = allHistory.iterator().next();
		Assert.assertEquals(mainProcessId, stateHistory.getProcessId());
		Assert.assertEquals(mainUserId, stateHistory.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, stateHistory.getTimeAdvanced());
		Assert.assertEquals(firstState, stateHistory.getState());
		Assert.assertEquals(firstAction, stateHistory.getAction());
		Assert.assertEquals(firstOutcome, stateHistory.getOutcome());
		Assert.assertEquals(firstComment, stateHistory.getComment());

		UUID testingProcId = initConcluder.createWorkflowProcess(mainDefinitionId, new HashSet<>(Arrays.asList(9999)),
				stampSequenceForTesting, mainUserId, SubjectMatter.CONCEPT);
		launchWorkflow(testingProcId);

		stateHistoryMap = accessor.getLatestActivePermissibleByRole(mainDefinitionId, secondaryUserId);
		Assert.assertEquals(1, stateHistoryMap.keySet().size());

		stateHistoryMap = accessor.getLatestActivePermissibleByRole(mainDefinitionId, mainUserId);
		Assert.assertEquals(2, stateHistoryMap.keySet().size());

		for (String role : stateHistoryMap.keySet()) {
			allHistory = stateHistoryMap.get(role);
			Assert.assertEquals(1, allHistory.size());
		}
	}
}

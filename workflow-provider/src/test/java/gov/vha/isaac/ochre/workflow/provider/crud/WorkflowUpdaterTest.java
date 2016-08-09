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
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Test the WorkflowInitializerConcluder class
 * 
 * {@link WorkflowUpdater}.
 * {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowUpdaterTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;

	private static MVStoreMetaContentProvider store;
	private static WorkflowUpdater updater;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowUpdate", true);
		}

		globalSetup(store);

		if (!setupCompleted) {
			updater = new WorkflowUpdater(store);
			setupUserRoles();

			createMainWorkflowProcess(mainDefinitionId);
			secondaryProcessId = createSecondaryWorkflowProcess(mainDefinitionId, secondaryConceptsForTesting);

			launchWorkflow(mainProcessId);
			firstHistoryEntryId = executeInitialAdvancement(mainProcessId);

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
	public void testAaddStampsToExistingProcess() throws Exception {
		try {
			updater.addStampsToExistingProcess(mainProcessId, 12345);
			Assert.fail();
		} catch (Exception e) {
			ProcessDetail details = processDetailStore.getEntry(mainProcessId);

			Assert.assertFalse(details.getStampSequences().contains(12345));
		}
		
		Assert.assertFalse(processDetailStore.getEntry(secondaryProcessId).getStampSequences().contains(12345));
		updater.addStampsToExistingProcess(secondaryProcessId, 12345);
		Assert.assertTrue(processDetailStore.getEntry(secondaryProcessId).getStampSequences().contains(12345));
	}
	
	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testBaddConceptsToExistingProcess() throws Exception {
		try {
			updater.addConceptsToExistingProcess(mainProcessId, new HashSet<>(Arrays.asList(12345)));
			Assert.fail();
		} catch (Exception e) {
			ProcessDetail details = processDetailStore.getEntry(mainProcessId);

			Assert.assertFalse(details.getConceptSequences().contains(12345));
		}
		
		Assert.assertFalse(processDetailStore.getEntry(secondaryProcessId).getConceptSequences().contains(12345));
		updater.addConceptsToExistingProcess(secondaryProcessId, new HashSet<> (Arrays.asList(12345)));
		Assert.assertTrue(processDetailStore.getEntry(secondaryProcessId).getConceptSequences().contains(12345));
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testCAdvanceWorkflow() throws Exception {

		UUID entryId = updater.advanceWorkflow(mainProcessId, mainUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);
	
		entryId = updater.advanceWorkflow(mainProcessId, secondaryUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(mainProcessId, secondaryUserId, "QA Passes", "Comment #1");
		Assert.assertNotNull(entryId);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testDAddNewUserRole() throws Exception {
		WorkflowActionsPermissionsAccessor accessor = new WorkflowActionsPermissionsAccessor();
		Set<UserPermission> permissions = accessor.getAllPermissionsForUser(mainDefinitionId, mainUserId);
		
		for (UserPermission perm : permissions) {
			Assert.assertFalse(perm.getRole().equals("Reviewer"));
		}
		
		updater.addNewUserRole(mainDefinitionId, mainUserId, "Reviewer");
		
		permissions = accessor.getAllPermissionsForUser(mainDefinitionId, mainUserId);
		boolean newRoleFound = false;
		for (UserPermission perm : permissions) {
			if (perm.getRole().equals("Reviewer")) {
				newRoleFound = true;
			}
		}
		Assert.assertTrue(newRoleFound);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testDremoveUserRole() throws Exception {
		WorkflowActionsPermissionsAccessor accessor = new WorkflowActionsPermissionsAccessor();
		Set<UserPermission> permissions = accessor.getAllPermissionsForUser(mainDefinitionId, mainUserId);
		
		boolean newRoleFound = false;
		for (UserPermission perm : permissions) {
			if (perm.getRole().equals("Reviewer")) {
				newRoleFound = true;
			}
		}
		Assert.assertTrue(newRoleFound);

		updater.removeUserRole(mainDefinitionId, mainUserId, "Reviewer");
		
		permissions = accessor.getAllPermissionsForUser(mainDefinitionId, mainUserId);
		newRoleFound = false;
		for (UserPermission perm : permissions) {
			if (perm.getRole().equals("Reviewer")) {
				newRoleFound = true;
			}
		}
		Assert.assertFalse(newRoleFound);
	}
}

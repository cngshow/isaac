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
 * {@link WorkflowUpdater}. {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowUpdaterTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;

	private static MVStoreMetaContentProvider store;
	private static WorkflowUpdater updater;

	private static int firstConceptNid = 0;
	private static int secondConceptNid = 0;
	private static int firstStampSeq = -1;
	private static int secondStampSeq = -1;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowUpdate", true);
		}

		globalSetup(store);
		setupUserRoles();

		if (!setupCompleted) {
			updater = new WorkflowUpdater(store);

			for (Integer nid : conceptsForTesting) {
				if (firstConceptNid == 0) {
					firstConceptNid = nid;
				} else {
					secondConceptNid = nid;
				}
			}

			for (Integer seq : stampSequenceForTesting) {
				if (firstStampSeq == -1) {
					firstStampSeq = seq;
				} else {
					secondStampSeq = seq;
				}
			}

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
	public void testAddComponentsToProcess() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		ProcessDetail details = processDetailStore.getEntry(processId);
		Assert.assertFalse(details.getComponentToStampsMap().containsKey(firstConceptNid));

		updater.addComponentToWorkflow(processId, firstConceptNid, firstStampSeq);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentToStampsMap().size());
		Assert.assertTrue(details.getComponentToStampsMap().containsKey(firstConceptNid));
		Assert.assertEquals(1, details.getComponentToStampsMap().get(firstConceptNid).size());
		Assert.assertTrue(details.getComponentToStampsMap().get(firstConceptNid).contains(firstStampSeq));

		updater.addComponentToWorkflow(processId, firstConceptNid, secondStampSeq);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentToStampsMap().size());
		Assert.assertTrue(details.getComponentToStampsMap().containsKey(firstConceptNid));
		Assert.assertEquals(2, details.getComponentToStampsMap().get(firstConceptNid).size());
		Assert.assertTrue(details.getComponentToStampsMap().get(firstConceptNid).contains(firstStampSeq));
		Assert.assertTrue(details.getComponentToStampsMap().get(firstConceptNid).contains(secondStampSeq));

		updater.addComponentToWorkflow(processId, secondConceptNid, firstStampSeq);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(2, details.getComponentToStampsMap().size());
		Assert.assertTrue(details.getComponentToStampsMap().containsKey(firstConceptNid));
		Assert.assertEquals(2, details.getComponentToStampsMap().get(firstConceptNid).size());
		Assert.assertTrue(details.getComponentToStampsMap().get(firstConceptNid).contains(firstStampSeq));
		Assert.assertTrue(details.getComponentToStampsMap().get(firstConceptNid).contains(secondStampSeq));
		Assert.assertTrue(details.getComponentToStampsMap().containsKey(secondConceptNid));
		Assert.assertEquals(1, details.getComponentToStampsMap().get(secondConceptNid).size());
		Assert.assertTrue(details.getComponentToStampsMap().get(secondConceptNid).contains(firstStampSeq));
	}

	@Test
	public void testFailuresWithAddRemoveComponentsToProcess() throws Exception {
		UUID processId = UUID.randomUUID();
		try {
			updater.removeComponentFromWorkflow(processId, firstConceptNid);
			Assert.fail();
		} catch (Exception e) {

		}

		UUID firstProcessId = createFirstWorkflowProcess(mainDefinitionId);
		UUID secondProcessId = createFirstWorkflowProcess(mainDefinitionId);
		updater.addComponentToWorkflow(secondProcessId, firstConceptNid, firstStampSeq);
		try {
			updater.addComponentToWorkflow(firstProcessId, firstConceptNid, firstStampSeq);
			Assert.fail();
		} catch (Exception e) {
			// Go back to no components in any workflow
			updater.removeComponentFromWorkflow(secondProcessId, firstConceptNid);
		}

		// Testing LAUNCHED-NON-EDIT Case
		executeLaunchWorkflow(firstProcessId);
		executeSendForReviewAdvancement(firstProcessId);
		try {
			updater.addComponentToWorkflow(firstProcessId, firstConceptNid, firstStampSeq);
			Assert.fail();
		} catch (Exception e) {

		}

		// Rejecting QA to get back to edit state
		executeRejectReviewAdvancement(firstProcessId);

		// Testing LAUNCHED-EDIT Case
		updater.addComponentToWorkflow(firstProcessId, firstConceptNid, firstStampSeq);
		ProcessDetail details = processDetailStore.getEntry(firstProcessId);
		Assert.assertEquals(1, details.getComponentToStampsMap().size());
		Assert.assertTrue(details.getComponentToStampsMap().containsKey(firstConceptNid));
		Assert.assertEquals(1, details.getComponentToStampsMap().get(firstConceptNid).size());
		Assert.assertTrue(details.getComponentToStampsMap().get(firstConceptNid).contains(firstStampSeq));

		// Testing INACTIVE Case
		cancelWorkflow(firstProcessId);
		try {
			updater.removeComponentFromWorkflow(firstProcessId, firstConceptNid);
			Assert.fail();
		} catch (Exception e) {

		}

	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testRemoveComponentsFromProcess() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		ProcessDetail details = processDetailStore.getEntry(processId);
		Assert.assertEquals(0, details.getComponentToStampsMap().size());

		updater.addComponentToWorkflow(processId, firstConceptNid, firstStampSeq);
		updater.addComponentToWorkflow(processId, firstConceptNid, secondStampSeq);
		updater.addComponentToWorkflow(processId, secondConceptNid, firstStampSeq);
		updater.addComponentToWorkflow(processId, secondConceptNid, secondStampSeq);

		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(2, details.getComponentToStampsMap().size());
		Assert.assertEquals(2, details.getComponentToStampsMap().get(firstConceptNid).size());
		Assert.assertEquals(2, details.getComponentToStampsMap().get(secondConceptNid).size());

		updater.removeComponentFromWorkflow(processId, firstConceptNid);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentToStampsMap().size());
		Assert.assertFalse(details.getComponentToStampsMap().containsKey(firstConceptNid));
		Assert.assertTrue(details.getComponentToStampsMap().containsKey(secondConceptNid));
		Assert.assertEquals(2, details.getComponentToStampsMap().get(secondConceptNid).size());
		Assert.assertTrue(details.getComponentToStampsMap().get(secondConceptNid).contains(firstStampSeq));
		Assert.assertTrue(details.getComponentToStampsMap().get(secondConceptNid).contains(secondStampSeq));

		updater.removeComponentFromWorkflow(processId, secondConceptNid);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(0, details.getComponentToStampsMap().size());
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAdvanceWorkflow() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		addComponentsToProcess(processId);
		executeLaunchWorkflow(processId);

		// Process in Ready to Edit state: Can execute action "Edit" by firstUser
		UUID entryId = updater.advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "Edit", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "Edit", "Comment #1");
		Assert.assertNotNull(entryId);

		// Process in Ready for Review state: Can execute action "QA Passes" by secondUser
		entryId = updater.advanceWorkflow(processId, firstUserId, "Edit", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "Edit", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1");
		Assert.assertNotNull(entryId);
		
		// Process in Ready for Approve state: Can execute action "Approve" by firstUser
		entryId = updater.advanceWorkflow(processId, secondUserId, "Edit", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1");
		Assert.assertNull(entryId);
		
		entryId = updater.advanceWorkflow(processId, firstUserId, "Edit", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "Approve", "Comment #1");
		Assert.assertNotNull(entryId);
		
		// Process in Publish state: no one can advance
		entryId = updater.advanceWorkflow(processId, secondUserId, "Edit", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1");
		Assert.assertNull(entryId);
		
		entryId = updater.advanceWorkflow(processId, firstUserId, "Edit", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1");
		Assert.assertNull(entryId);

		entryId = updater.advanceWorkflow(processId, firstUserId, "Approve", "Comment #1");
		Assert.assertNull(entryId);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAddNewUserRole() throws Exception {
		WorkflowAccessor wfAccessor = new WorkflowAccessor();
		Set<UserPermission> permissions = wfAccessor.getUserPermissions(mainDefinitionId, firstUserId);

		for (UserPermission perm : permissions) {
			Assert.assertFalse(perm.getRole().equals("Reviewer"));
		}

		updater.addNewUserRole(mainDefinitionId, firstUserId, "Reviewer");

		permissions = wfAccessor.getUserPermissions(mainDefinitionId, firstUserId);
		boolean newRoleFound = false;
		for (UserPermission perm : permissions) {
			if (perm.getRole().equals("Reviewer")) {
				newRoleFound = true;
			}
		}
		Assert.assertTrue(newRoleFound);
	}
}

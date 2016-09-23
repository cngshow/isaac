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
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;

/**
 * Test the WorkflowUpdater class
 * 
 * {@link WorkflowUpdater}. {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
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
		updater.closeContentStores();
	}

	/**
	 * Test ability to add components and stamps to the process.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	/*
	 * Note this is a simplified test as using addComponentToWorkflow. More
	 * realistic and complex test, using addCommitRecordToWorkflow, is found in
	 * WorkflowFrameworkTest as commitRecords require IdentifierService.
	 */
	public void testAddComponentsToProcess() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		ProcessDetail details = processDetailStore.getEntry(processId);
		Assert.assertFalse(details.getComponentNidToStampsMap().containsKey(firstConceptNid));

		updater.addComponentToWorkflow(details, firstConceptNid, firstStampSeq);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentNidToStampsMap().size());
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(firstConceptNid));
		Assert.assertEquals(1, details.getComponentNidToStampsMap().get(firstConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstConceptNid).contains(firstStampSeq));

		updater.addComponentToWorkflow(details, firstConceptNid, secondStampSeq);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentNidToStampsMap().size());
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(firstConceptNid));
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(firstConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstConceptNid).contains(firstStampSeq));
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstConceptNid).contains(secondStampSeq));

		updater.addComponentToWorkflow(details, secondConceptNid, firstStampSeq);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(2, details.getComponentNidToStampsMap().size());
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(firstConceptNid));
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(firstConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstConceptNid).contains(firstStampSeq));
		Assert.assertTrue(details.getComponentNidToStampsMap().get(firstConceptNid).contains(secondStampSeq));
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(secondConceptNid));
		Assert.assertEquals(1, details.getComponentNidToStampsMap().get(secondConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(secondConceptNid).contains(firstStampSeq));
	}

	/**
	 * Test ability to add and then remove components and stamps to the process.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	/*
	 * Note this is a simplified test as using addComponentToWorkflow. More
	 * realistic and complex test, using addCommitRecordToWorkflow, is found in
	 * WorkflowFrameworkTest as commitRecords require IdentifierService.
	 */
	public void testRemoveComponentsFromProcess() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		ProcessDetail details = processDetailStore.getEntry(processId);
		Assert.assertEquals(0, details.getComponentNidToStampsMap().size());

		updater.addComponentToWorkflow(details, firstConceptNid, firstStampSeq);
		updater.addComponentToWorkflow(details, firstConceptNid, secondStampSeq);
		updater.addComponentToWorkflow(details, secondConceptNid, firstStampSeq);
		updater.addComponentToWorkflow(details, secondConceptNid, secondStampSeq);

		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(2, details.getComponentNidToStampsMap().size());
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(firstConceptNid).size());
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(secondConceptNid).size());

		updater.removeComponentFromWorkflow(processId, firstConceptNid, null);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(1, details.getComponentNidToStampsMap().size());
		Assert.assertFalse(details.getComponentNidToStampsMap().containsKey(firstConceptNid));
		Assert.assertTrue(details.getComponentNidToStampsMap().containsKey(secondConceptNid));
		Assert.assertEquals(2, details.getComponentNidToStampsMap().get(secondConceptNid).size());
		Assert.assertTrue(details.getComponentNidToStampsMap().get(secondConceptNid).contains(firstStampSeq));
		Assert.assertTrue(details.getComponentNidToStampsMap().get(secondConceptNid).contains(secondStampSeq));

		updater.removeComponentFromWorkflow(processId, secondConceptNid, null);
		details = processDetailStore.getEntry(processId);
		Assert.assertEquals(0, details.getComponentNidToStampsMap().size());
	}

	/**
	 * Test that advancing process not only works, but only is permitted based
	 * on current state (modified while advancing) only available actions based
	 * on user permissions can advance process.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testAdvanceWorkflow() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		addComponentsToProcess(processId);
		executeLaunchWorkflow(processId);

		// Process in Ready to Edit state: Can execute action "Edit" by
		// firstUser
		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Approve", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Approve", "Comment #1"));

		Assert.assertTrue(advanceWorkflow(processId, firstUserId, "Edit", "Comment #1"));

		// Process in Ready for Review state: Can execute action "QA Passes" by
		// secondUser
		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Approve", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Approve", "Comment #1"));

		Assert.assertTrue(advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1"));

		// Process in Ready for Approve state: Can execute action "Approve" by
		// firstUser
		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Approve", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1"));

		Assert.assertTrue(advanceWorkflow(processId, firstUserId, "Approve", "Comment #1"));

		// Process in Publish state: no one can advance
		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Approve", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Approve", "Comment #1"));
	}

}

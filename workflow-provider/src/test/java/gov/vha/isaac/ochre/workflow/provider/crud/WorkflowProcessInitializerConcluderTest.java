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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessDetailComparator;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory.ProcessHistoryComparator;

/**
 * Test the WorkflowProcessInitializerConcluder class
 * 
 * {@link WorkflowProcessInitializerConcluder}.
 * {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowProcessInitializerConcluderTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;

	private static MVStoreMetaContentProvider store;

	private static WorkflowProcessInitializerConcluder initConcluder;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowInitConclude", true);
			initConcluder = new WorkflowProcessInitializerConcluder(store);
			setupCompleted = true;
		}

		globalSetup(store);
	}

	@AfterClass
	public static void tearDownClass() {
		initConcluder.closeContentStores();
	}

	/**
	 * Test creation of workflow process creates the process details as expected
	 * and adds a process history entry as expected
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testCreateWorkflowProcess() throws Exception {
		// Initialization
		UUID processId = initConcluder.createWorkflowProcess(mainDefinitionId, firstUserId, "Main Process Name",
				"Main Process Description");
		addComponentsToProcess(processId);

		// verify content in workflow is as expected
		assertProcessDefinition(ProcessStatus.DEFINED, mainDefinitionId, processId);

		SortedSet<ProcessHistory> hxEntries = new TreeSet<>(new ProcessHistoryComparator());
		hxEntries.addAll(processHistoryStore.getAllEntries());
		Assert.assertEquals(1, hxEntries.size());
		assertHistoryForProcess(hxEntries, processId);
	}

	/**
	 * Test launching of workflow process updates the process details as
	 * expected and adds a process history entry as expected. Furthermore,
	 * ensure that cannot launch a process that has a) hasn't been created or b)
	 * already been launched
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testLaunchWorkflow() throws Exception {
		// Attempt to launch a process that hasn't yet been created
		try {
			initConcluder.launchProcess(UUID.randomUUID());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		UUID processId = initConcluder.createWorkflowProcess(mainDefinitionId, firstUserId, "Main Process Name",
				"Main Process Description");
		Thread.sleep(1);

		addComponentsToProcess(processId);
		executeSendForReviewAdvancement(processId);
		initConcluder.launchProcess(processId);

		assertProcessDefinition(ProcessStatus.LAUNCHED, mainDefinitionId, processId);

		SortedSet<ProcessHistory> hxEntries = new TreeSet<>(new ProcessHistoryComparator());
		hxEntries.addAll(processHistoryStore.getAllEntries());
		Assert.assertEquals(2, hxEntries.size());
		assertHistoryForProcess(hxEntries, processId);

		// Attempt to launch an already launched process
		try {
			initConcluder.launchProcess(processId);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
	}

	/**
	 * Test cancelation of workflow process updates the process details as
	 * expected and adds a process history entry as expected. Furthermore,
	 * ensure that cannot cancel a process that has a) hasn't been created or b)
	 * already been canceled
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testCancelWorkflowProcess() throws Exception {
		// Attempt to cancel a process that hasn't yet been created
		try {
			endWorkflowProcess(UUID.randomUUID(), cancelAction, firstUserId, CANCELED_WORKFLOW_COMMENT,
					EndWorkflowType.CANCELED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		UUID processId = initConcluder.createWorkflowProcess(mainDefinitionId, firstUserId, "Main Process Name",
				"Main Process Description");
		Thread.sleep(1);

		addComponentsToProcess(processId);
		executeSendForReviewAdvancement(processId);
		initConcluder.launchProcess(processId);
		Thread.sleep(1);

		executeSendForApprovalAdvancement(processId);
		Thread.sleep(1);

		SortedSet<ProcessHistory> hxEntries = new TreeSet<>(new ProcessHistoryComparator());
		hxEntries.addAll(processHistoryStore.getAllEntries());

		Assert.assertEquals(3, hxEntries.size());
		assertHistoryForProcess(hxEntries, processId);

		endWorkflowProcess(processId, cancelAction, firstUserId, CANCELED_WORKFLOW_COMMENT,
				EndWorkflowType.CANCELED);

		assertProcessDefinition(ProcessStatus.CANCELED, mainDefinitionId, processId);
		hxEntries.clear();
		hxEntries.addAll(processHistoryStore.getAllEntries());
		assertCancelHistory(hxEntries.last(), processId);

		// Attempt to cancel an already launched process
		try {
			endWorkflowProcess(processId, cancelAction, firstUserId, CANCELED_WORKFLOW_COMMENT,
					EndWorkflowType.CANCELED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		// TODO: After complete the cancelation store creation & integration
	}

	/**
	 * Test concluding of workflow process updates the process details as
	 * expected and adds a process history entry as expected. Furthermore,
	 * ensure that cannot conclude a process that has a) hasn't been created, b)
	 * hasn't been launched, c) isn't at an end state or d) has already been
	 * concluded
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testConcludeWorkflow() throws Exception {
		// Attempt to conclude a process that hasn't yet been created
		try {
			endWorkflowProcess(UUID.randomUUID(), concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		UUID processId = initConcluder.createWorkflowProcess(mainDefinitionId, firstUserId, "Main Process Name",
				"Main Process Description");
		Thread.sleep(1);

		// Attempt to conclude a process that hasn't yet been launched
		try {
			endWorkflowProcess(processId, concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		addComponentsToProcess(processId);
		executeSendForReviewAdvancement(processId);
		initConcluder.launchProcess(processId);
		Thread.sleep(1);

		// Attempt to conclude a process that isn't at an end state
		try {
			endWorkflowProcess(processId, concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		executeSendForApprovalAdvancement(processId);
		Thread.sleep(1);

		SortedSet<ProcessHistory> hxEntries = new TreeSet<>(new ProcessHistoryComparator());
		hxEntries.addAll(processHistoryStore.getAllEntries());

		Assert.assertEquals(3, hxEntries.size());
		assertHistoryForProcess(hxEntries, processId);

		endWorkflowProcess(processId, concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
				EndWorkflowType.CONCLUDED);

		assertProcessDefinition(ProcessStatus.CONCLUDED, mainDefinitionId, processId);
		hxEntries.clear();
		hxEntries.addAll(processHistoryStore.getAllEntries());
		assertConcludeHistory(hxEntries.last(), processId);

		// Attempt to cancel an already launched process
		try {
			endWorkflowProcess(processId, concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
	}

	private void assertProcessDefinition(ProcessStatus processStatus, UUID definitionId, UUID processId) {
		SortedSet<ProcessDetail> detailEntries = new TreeSet<>(new ProcessDetailComparator());
		detailEntries.addAll(processDetailStore.getAllEntries());
		ProcessDetail entry = detailEntries.last();

		Assert.assertEquals(processId, entry.getId());
		Assert.assertEquals(2, entry.getComponentNidToStampsMap().size());
		Assert.assertTrue(entry.getComponentNidToStampsMap().containsKey(-55));
		Assert.assertTrue(entry.getComponentNidToStampsMap().get(-55).contains(11));
		Assert.assertTrue(entry.getComponentNidToStampsMap().get(-55).contains(12));

		Assert.assertEquals(processStatus, entry.getStatus());
		Assert.assertEquals(99, entry.getCreatorNid());
		Assert.assertEquals(definitionId, entry.getDefinitionId());
		Assert.assertTrue(entry.getComponentNidToStampsMap().containsKey(-56));
		Assert.assertTrue(entry.getComponentNidToStampsMap().get(-56).contains(11));
		Assert.assertTrue(entry.getComponentNidToStampsMap().get(-56).contains(12));
		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeCreated()));

		if (processStatus == ProcessStatus.DEFINED) {
			Assert.assertEquals(-1L, entry.getTimeLaunched());
			Assert.assertEquals(-1L, entry.getTimeCanceledOrConcluded());
		} else {
			Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeLaunched()));
			if (processStatus == ProcessStatus.LAUNCHED) {
				Assert.assertEquals(-1L, entry.getTimeCanceledOrConcluded());
			} else {
				Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeCanceledOrConcluded()));
			}
		}
	}
}

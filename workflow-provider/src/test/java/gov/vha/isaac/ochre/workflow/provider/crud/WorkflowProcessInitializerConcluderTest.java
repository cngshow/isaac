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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.RecursiveDelete;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.EndWorkflowType;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.ProcessDetailComparator;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessHistory.ProcessHistoryComparator;
import gov.vha.isaac.ochre.workflow.provider.WorkflowProvider;

/**
 * Test the WorkflowProcessInitializerConcluder class
 * 
 * {@link WorkflowProcessInitializerConcluder}.
 * {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowProcessInitializerConcluderTest extends AbstractWorkflowProviderTestPackage {

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
		UUID processId = wp_.getWorkflowProcessInitializerConcluder().createWorkflowProcess(mainDefinitionId, firstUserId, "Main Process Name",
				"Main Process Description");
		addComponentsToProcess(processId);

		// verify content in workflow is as expected
		assertProcessDefinition(ProcessStatus.DEFINED, mainDefinitionId, processId);

		SortedSet<ProcessHistory> hxEntries = new TreeSet<>(new ProcessHistoryComparator());
		hxEntries.addAll(wp_.getProcessHistoryStore().values());
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
			wp_.getWorkflowProcessInitializerConcluder().launchProcess(UUID.randomUUID());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		UUID processId = wp_.getWorkflowProcessInitializerConcluder().createWorkflowProcess(mainDefinitionId, firstUserId, "Main Process Name",
				"Main Process Description");
		Thread.sleep(1);

		addComponentsToProcess(processId);
		executeSendForReviewAdvancement(processId);
		wp_.getWorkflowProcessInitializerConcluder().launchProcess(processId);

		assertProcessDefinition(ProcessStatus.LAUNCHED, mainDefinitionId, processId);

		SortedSet<ProcessHistory> hxEntries = new TreeSet<>(new ProcessHistoryComparator());
		hxEntries.addAll(wp_.getProcessHistoryStore().values());
		Assert.assertEquals(2, hxEntries.size());
		assertHistoryForProcess(hxEntries, processId);

		// Attempt to launch an already launched process
		try {
			wp_.getWorkflowProcessInitializerConcluder().launchProcess(processId);
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
			wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(UUID.randomUUID(), cancelAction, firstUserId, CANCELED_WORKFLOW_COMMENT,
					EndWorkflowType.CANCELED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		UUID processId = wp_.getWorkflowProcessInitializerConcluder().createWorkflowProcess(mainDefinitionId, firstUserId, "Main Process Name",
				"Main Process Description");
		Thread.sleep(1);

		addComponentsToProcess(processId);
		executeSendForReviewAdvancement(processId);
		wp_.getWorkflowProcessInitializerConcluder().launchProcess(processId);
		Thread.sleep(1);

		executeSendForApprovalAdvancement(processId);
		Thread.sleep(1);

		SortedSet<ProcessHistory> hxEntries = new TreeSet<>(new ProcessHistoryComparator());
		hxEntries.addAll(wp_.getProcessHistoryStore().values());

		Assert.assertEquals(3, hxEntries.size());
		assertHistoryForProcess(hxEntries, processId);

		wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, cancelAction, firstUserId, CANCELED_WORKFLOW_COMMENT,
				EndWorkflowType.CANCELED);

		assertProcessDefinition(ProcessStatus.CANCELED, mainDefinitionId, processId);
		hxEntries.clear();
		hxEntries.addAll(wp_.getProcessHistoryStore().values());
		assertCancelHistory(hxEntries.last(), processId);

		// Attempt to cancel an already launched process
		try {
			wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, cancelAction, firstUserId, CANCELED_WORKFLOW_COMMENT,
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
			wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(UUID.randomUUID(), concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		UUID processId = wp_.getWorkflowProcessInitializerConcluder().createWorkflowProcess(mainDefinitionId, firstUserId, "Main Process Name",
				"Main Process Description");
		Thread.sleep(1);

		// Attempt to conclude a process that hasn't yet been launched
		try {
			wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		addComponentsToProcess(processId);
		executeSendForReviewAdvancement(processId);
		wp_.getWorkflowProcessInitializerConcluder().launchProcess(processId);
		Thread.sleep(1);

		// Attempt to conclude a process that isn't at an end state
		try {
			wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}

		executeSendForApprovalAdvancement(processId);
		Thread.sleep(1);

		SortedSet<ProcessHistory> hxEntries = new TreeSet<>(new ProcessHistoryComparator());
		hxEntries.addAll(wp_.getProcessHistoryStore().values());

		Assert.assertEquals(3, hxEntries.size());
		assertHistoryForProcess(hxEntries, processId);

		wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
				EndWorkflowType.CONCLUDED);

		assertProcessDefinition(ProcessStatus.CONCLUDED, mainDefinitionId, processId);
		hxEntries.clear();
		hxEntries.addAll(wp_.getProcessHistoryStore().values());
		assertConcludeHistory(hxEntries.last(), processId);

		// Attempt to cancel an already launched process
		try {
			wp_.getWorkflowProcessInitializerConcluder().endWorkflowProcess(processId, concludeAction, firstUserId, CONCLUDED_WORKFLOW_COMMENT,
					EndWorkflowType.CONCLUDED);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(true);
		}
	}

	private void assertProcessDefinition(ProcessStatus processStatus, UUID definitionId, UUID processId) {
		SortedSet<ProcessDetail> detailEntries = new TreeSet<>(new ProcessDetailComparator());
		detailEntries.addAll(wp_.getProcessDetailStore().values());
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

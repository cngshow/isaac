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
import java.util.Date;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.ProcessStatus;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.SubjectMatter;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Test the WorkflowInitializerConcluder class
 * 
 * {@link WorkflowProcessInitializerConcluder}.
 * {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowInitializerConcluderTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;

	private static MVStoreMetaContentProvider store;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowInitConclude", true);
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
	public void testADefineWorkflow() throws Exception {
		// Initialization
		createMainWorkflowProcess(mainDefinitionId);

		Set<ProcessDetail> entries = processDetailStore.getAllEntries();

		// verify content in workflow is as expected
		ProcessDetail entry = entries.iterator().next();
		Assert.assertEquals(mainProcessId, entry.getId());
		Assert.assertEquals(3, entry.getConceptSequences().size());
		Assert.assertTrue(entry.getConceptSequences().contains(55));

		Assert.assertEquals(SubjectMatter.CONCEPT, entry.getSubjectMatter());
		Assert.assertEquals(ProcessStatus.DEFINED, entry.getProcessStatus());
		Assert.assertEquals(99, entry.getCreator());
		Assert.assertEquals(mainDefinitionId, entry.getDefinitionId());
		Assert.assertEquals(3, entry.getStampSequences().size());
		Assert.assertTrue(entry.getStampSequences().contains(11));

		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeCreated()));
		Assert.assertEquals(-1L, entry.getTimeConcluded());
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testBLaunchWorkflow() throws Exception {
		initConcluder.launchWorkflowProcess(mainProcessId);

		ProcessDetailContentStore processDetailStore = new ProcessDetailContentStore(store);

		ProcessDetail detailEntry = processDetailStore.getEntry(mainProcessId);
		Assert.assertEquals(mainProcessId, detailEntry.getId());

		Assert.assertEquals(3, detailEntry.getConceptSequences().size());
		Assert.assertTrue(detailEntry.getConceptSequences().contains(55));

		Assert.assertEquals(SubjectMatter.CONCEPT, detailEntry.getSubjectMatter());
		Assert.assertEquals(ProcessStatus.LAUNCHED, detailEntry.getProcessStatus());
		Assert.assertEquals(mainUserId, detailEntry.getCreator());
		Assert.assertEquals(mainDefinitionId, detailEntry.getDefinitionId());
		Assert.assertEquals(3, detailEntry.getStampSequences().size());
		Assert.assertTrue(detailEntry.getStampSequences().contains(11));

		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(detailEntry.getTimeCreated()));
		Assert.assertEquals(-1L, detailEntry.getTimeConcluded());
		
		// Verify the automated creation of the initial history record is correct
		WorkflowHistoryAccessor historyAccessor = new WorkflowHistoryAccessor();
		ProcessHistory historyEntry = historyAccessor.getLatestForProcess(mainProcessId);
		Assert.assertEquals(AbstractWorkflowUtilities.getProcessStartState(), historyEntry.getState());
		Assert.assertEquals(AbstractWorkflowUtilities.SYSTEM_AUTOMATED, historyEntry.getAction());
		Assert.assertEquals("Ready for Edit", historyEntry.getOutcome());
		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(historyEntry.getTimeAdvanced()));

	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testCCancelWorkflowProcess() throws Exception {
		// TODO: After complete the cancellation store creation & integration
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testDConcludeWorkflow() throws Exception {
		// TODO: Once completed implementation testCCancelWorkflowProcess(),
		// must recreate A&B test methods

		ProcessDetail entry = processDetailStore.getEntry(mainProcessId);
		entry.setProcessStatus(ProcessStatus.CONCLUDED);
		entry.setTimeConcluded(new Date().getTime());
		processDetailStore.updateEntry(mainProcessId, entry);

		ProcessDetailContentStore processDetailStore = new ProcessDetailContentStore(store);

		entry = processDetailStore.getEntry(mainProcessId);
		Assert.assertEquals(mainProcessId, entry.getId());

		Assert.assertEquals(3, entry.getConceptSequences().size());
		Assert.assertTrue(entry.getConceptSequences().contains(55));

		Assert.assertEquals(SubjectMatter.CONCEPT, entry.getSubjectMatter());
		Assert.assertEquals(ProcessStatus.CONCLUDED, entry.getProcessStatus());
		Assert.assertEquals(99, entry.getCreator());
		Assert.assertEquals(mainDefinitionId, entry.getDefinitionId());
		Assert.assertEquals(3, entry.getStampSequences().size());
		Assert.assertTrue(entry.getStampSequences().contains(11));

		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeCreated()));
		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeConcluded()));
	}
}

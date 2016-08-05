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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.ProcessDetailContentStore;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;
import gov.vha.isaac.ochre.workflow.provider.crud.WorkflowInitializerConcluder;

/**
 * Test the WorkflowInitializerConcluder class
 * 
 * {@link WorkflowInitializerConcluder}.
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
		Assert.assertEquals(ProcessStatus.READY_TO_LAUNCH, entry.getProcessStatus());
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
		initConcluder.launchWorkflow(mainProcessId);

		ProcessDetailContentStore processDetailStore = new ProcessDetailContentStore(store);

		ProcessDetail entry = processDetailStore.getEntry(mainProcessId);
		Assert.assertEquals(mainProcessId, entry.getId());

		Assert.assertEquals(3, entry.getConceptSequences().size());
		Assert.assertTrue(entry.getConceptSequences().contains(55));

		Assert.assertEquals(SubjectMatter.CONCEPT, entry.getSubjectMatter());
		Assert.assertEquals(ProcessStatus.LAUNCHED, entry.getProcessStatus());
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

		initConcluder.concludeWorkflow(mainProcessId);

		ProcessDetailContentStore processDetailStore = new ProcessDetailContentStore(store);

		ProcessDetail entry = processDetailStore.getEntry(mainProcessId);
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

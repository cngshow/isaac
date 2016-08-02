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
package gov.vha.isaac.ochre.workflow.provider.metastore;

import java.io.File;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;

/**
 * Test the WorkflowInitializerConcluder class
 * 
 * {@link Bpmn2FileImporter}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowInitializerConcluderTest extends WorkflowProviderTestPackage {
	private static int userIdForTesting;

	private static List<Integer> stampSequenceForTesting;

	private static Set<Integer> conceptsForTesting;

	private static WorkflowInitializerConcluder initConcluder;

	private static UUID definitionId;

	private static UUID processId;

	/**
	 * Sets the up.
	 */
	@BeforeClass
	public static void setUpClass() {
		if (store == null) {
			store = new MVStoreMetaContentProvider(new File("target"), "test", true);
			importer = new Bpmn2FileImporter(store, BPMN_FILE_PATH);
		}

		initConcluder = new WorkflowInitializerConcluder(store);

		DefinitionDetailWorkflowContentStore definitionDetailStore = new DefinitionDetailWorkflowContentStore(store);
		definitionId = definitionDetailStore.getAllEntries().iterator().next().getId();

		userIdForTesting = 99;

		stampSequenceForTesting = Arrays.asList(11, 12, 13);

		conceptsForTesting = new HashSet<>(Arrays.asList(55, 56, 57));

	}

	/**
	 * Tear down.
	 */
	@AfterClass
	public static void tearDownClass() {
		System.out.println("OUT");
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
		ProcessDetailWorkflowContentStore processDetailStore = new ProcessDetailWorkflowContentStore(store);

		// Create new process
		processId = initConcluder.defineWorkflow(definitionId, conceptsForTesting, stampSequenceForTesting,
				userIdForTesting, SubjectMatter.CONCEPT);

		Set<ProcessDetail> entries = processDetailStore.getAllEntries();

		// verify content in workflow is as expected
		ProcessDetail entry = entries.iterator().next();
		Assert.assertEquals(processId, entry.getId());
		Assert.assertEquals(3, entry.getConcepts().size());
		Assert.assertTrue(entry.getConcepts().contains(55));

		Assert.assertEquals(SubjectMatter.CONCEPT, entry.getSubjectMatter());
		Assert.assertEquals(ProcessStatus.READY_TO_LAUNCH, entry.getProcessStatus());
		Assert.assertEquals(99, entry.getCreator());
		Assert.assertEquals(definitionId, entry.getDefinitionId());
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
		initConcluder.launchWorkflow(processId);

		ProcessDetailWorkflowContentStore processDetailStore = new ProcessDetailWorkflowContentStore(store);

		ProcessDetail entry = processDetailStore.getEntry(processId);
		Assert.assertEquals(processId, entry.getId());

		Assert.assertEquals(3, entry.getConcepts().size());
		Assert.assertTrue(entry.getConcepts().contains(55));

		Assert.assertEquals(SubjectMatter.CONCEPT, entry.getSubjectMatter());
		Assert.assertEquals(ProcessStatus.LAUNCHED, entry.getProcessStatus());
		Assert.assertEquals(99, entry.getCreator());
		Assert.assertEquals(definitionId, entry.getDefinitionId());
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

		initConcluder.concludeWorkflow(processId);

		ProcessDetailWorkflowContentStore processDetailStore = new ProcessDetailWorkflowContentStore(store);

		ProcessDetail entry = processDetailStore.getEntry(processId);
		Assert.assertEquals(processId, entry.getId());

		Assert.assertEquals(3, entry.getConcepts().size());
		Assert.assertTrue(entry.getConcepts().contains(55));

		Assert.assertEquals(SubjectMatter.CONCEPT, entry.getSubjectMatter());
		Assert.assertEquals(ProcessStatus.CONCLUDED, entry.getProcessStatus());
		Assert.assertEquals(99, entry.getCreator());
		Assert.assertEquals(definitionId, entry.getDefinitionId());
		Assert.assertEquals(3, entry.getStampSequences().size());
		Assert.assertTrue(entry.getStampSequences().contains(11));

		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeCreated()));
		Assert.assertTrue(timeSinceYesterdayBeforeTomorrow(entry.getTimeConcluded()));
	}

	private boolean timeSinceYesterdayBeforeTomorrow(long time) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		long yesterdayTimestamp = cal.getTimeInMillis();

		cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		long tomorrowTimestamp = cal.getTimeInMillis();

		return time >= yesterdayTimestamp && time <= tomorrowTimestamp;
	}

}

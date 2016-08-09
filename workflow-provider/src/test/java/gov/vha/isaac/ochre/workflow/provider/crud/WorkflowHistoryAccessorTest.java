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
import java.util.TreeSet;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Test the WorkflowInitializerConcluder class
 * 
 * {@link WorkflowHistoryAccessor}.
 * {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowHistoryAccessorTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;

	private static MVStoreMetaContentProvider store;
	private static WorkflowHistoryAccessor accessor;

	private static UUID secondaryDefinitionId;

	private static UUID thirdProcessId;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowHistoryAccessor", true);
		}

		globalSetup(store);

		if (!setupCompleted) {
			createMainWorkflowProcess(mainDefinitionId);
			launchWorkflow(mainProcessId);
			accessor = new WorkflowHistoryAccessor(store);
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
	public void testAGetActiveForConcept() throws Exception {
		SortedSet<ProcessHistory> allProcessHistory = accessor
				.getActiveForConcept(conceptsForTesting.iterator().next());
		Assert.assertEquals(2, allProcessHistory.size());

		ProcessHistory testingEntry = allProcessHistory.last();
		Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(mainUserId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

		// Add second Workflow
		secondHistoryEntryId = executeSecondAdvancement(mainProcessId);

		// Initialization
		allProcessHistory = accessor.getActiveForConcept(conceptsForTesting.iterator().next());
		assertHistoryForMainDefinition(allProcessHistory, mainProcessId, 3);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testBGetActiveByDefinition() throws Exception {

		Map<UUID, SortedSet<ProcessHistory>> historyByDef = accessor.getActiveByDefinition();
		Assert.assertEquals(1, historyByDef.keySet().size());
		Assert.assertEquals(mainDefinitionId, historyByDef.keySet().iterator().next());
		assertHistoryForMainDefinition(historyByDef.get(mainDefinitionId), mainProcessId, 3);


		// Add new Definition and initialize with single Advancement
		secondaryDefinitionId = createSecondaryDefinition();
		historyByDef = accessor.getActiveByDefinition();
		Assert.assertEquals(1, historyByDef.keySet().size());
		Assert.assertEquals(3, historyByDef.get(mainDefinitionId).size());
		
		secondaryProcessId = createSecondaryWorkflowProcess(secondaryDefinitionId, secondaryConceptsForTesting);
		historyByDef = accessor.getActiveByDefinition();
		Assert.assertEquals(1, historyByDef.keySet().size());
		Assert.assertEquals(3, historyByDef.get(mainDefinitionId).size());

		launchWorkflow(secondaryProcessId);
		historyByDef = accessor.getActiveByDefinition();
		Assert.assertEquals(2, historyByDef.keySet().size());
		assertHistoryForMainDefinition(historyByDef.get(secondaryDefinitionId), secondaryProcessId, 1);

		executeInitialAdvancement(secondaryProcessId);
		historyByDef = accessor.getActiveByDefinition();
		Assert.assertEquals(2, historyByDef.keySet().size());
		assertHistoryForMainDefinition(historyByDef.get(secondaryDefinitionId), secondaryProcessId, 2);

	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testCGetActiveForConcept() throws Exception {
		SortedSet<ProcessHistory> processHistory = accessor.getActiveForConcept(conceptsForTesting.iterator().next());
		assertHistoryForMainDefinition(processHistory, mainProcessId, 3);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testDGetByProcessMap() throws Exception {
		Set<Integer> concepts = new HashSet<> (Arrays.asList(8888));
		thirdProcessId = createSecondaryWorkflowProcess(mainDefinitionId, concepts);
		launchWorkflow(thirdProcessId);
		
		Map<UUID, SortedSet<ProcessHistory>> historyByProcess = accessor.getByProcessMap();
		Assert.assertEquals(3, historyByProcess.keySet().size());

		for (UUID procId : historyByProcess.keySet()) {
			Assert.assertTrue(procId.equals(mainProcessId) || procId.equals(secondaryProcessId) || procId.equals(thirdProcessId));

			SortedSet<ProcessHistory> processHistory = historyByProcess.get(procId);

			if (procId.equals(mainProcessId)) {
				assertHistoryForMainDefinition(processHistory, mainProcessId, 3);
			} else if (procId.equals(secondaryProcessId)) {
				assertHistoryForMainDefinition(processHistory, secondaryProcessId, 2);
			} else {
				assertHistoryForMainDefinition(processHistory, thirdProcessId, 1);
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
	public void testEGetByDefinitionMap() throws Exception {
		Map<UUID, SortedSet<ProcessHistory>> historyByDefinition = accessor.getByDefinitionMap();
	
		Assert.assertEquals(2, historyByDefinition.keySet().size());
		Assert.assertEquals(4, historyByDefinition.get(mainDefinitionId).size());
		Assert.assertEquals(2, historyByDefinition.get(secondaryDefinitionId).size());

	

		SortedSet<ProcessHistory> definitionHistory = historyByDefinition.get(secondaryDefinitionId);
		assertHistoryForMainDefinition(definitionHistory, secondaryProcessId, 2);

		definitionHistory = historyByDefinition.get(mainDefinitionId);
		
		// Break up by ProcessId
		SortedSet<ProcessHistory> mainProcHistory = new TreeSet<>(new ProcessHistory.ProcessHistoryComparator()); 
		SortedSet<ProcessHistory> thirdProcHistory = new TreeSet<>(new ProcessHistory.ProcessHistoryComparator()); 
		for (ProcessHistory hx : definitionHistory) {
			if (hx.getProcessId().equals(mainProcessId)) {
				mainProcHistory.add(hx);
			} else {
				thirdProcHistory.add(hx);
			}
		}
		
		assertHistoryForMainDefinition(mainProcHistory, mainProcessId, 3);
		assertHistoryForMainDefinition(thirdProcHistory, thirdProcessId, 1);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	 @Test
	public void testFGetForConcept() throws Exception {
		for (int conId : conceptsForTesting) {
			SortedSet<ProcessHistory> processHistory = accessor.getForConcept(conId);
			assertHistoryForMainDefinition(processHistory, mainProcessId, 3);
		}

	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	 @Test
	public void testGGetForProcess() throws Exception {
		SortedSet<ProcessHistory> processHistory = accessor.getForProcess(mainProcessId);
		assertHistoryForMainDefinition(processHistory, mainProcessId, 3);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	 @Test
	public void testHGetLatestForProcess() throws Exception {
		ProcessHistory testingEntry = accessor.getLatestForProcess(mainProcessId);
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(mainUserId, testingEntry.getWorkflowUser());
		Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(secondState, testingEntry.getState());
		Assert.assertEquals(secondAction, testingEntry.getAction());
		Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
		Assert.assertEquals(secondComment, testingEntry.getComment());

		testingEntry = accessor.getLatestForProcess(secondaryProcessId);
		Assert.assertEquals(secondaryProcessId, testingEntry.getProcessId());
		Assert.assertEquals(mainUserId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());
	}


	private void assertHistoryForMainDefinition(SortedSet<ProcessHistory> allProcessHistory, UUID processId, int numberOfEntries) {
		Assert.assertEquals(numberOfEntries, allProcessHistory.size());

		int counter = 0;
		for (ProcessHistory entry : allProcessHistory) {
			if (counter == 0) {
				Assert.assertEquals(processId, entry.getProcessId());
				Assert.assertEquals(mainUserId, entry.getWorkflowUser());
				Assert.assertEquals(launchHistoryTimestamp, entry.getTimeAdvanced());
				Assert.assertEquals(launchState, entry.getState());
				Assert.assertEquals(launchAction, entry.getAction());
				Assert.assertEquals(launchOutcome, entry.getOutcome());
				Assert.assertEquals(launchComment, entry.getComment());
			} else if (counter == 1) {
				Assert.assertEquals(processId, entry.getProcessId());
				Assert.assertEquals(mainUserId, entry.getWorkflowUser());
				Assert.assertEquals(firstHistoryTimestamp, entry.getTimeAdvanced());
				Assert.assertEquals(firstState, entry.getState());
				Assert.assertEquals(firstAction, entry.getAction());
				Assert.assertEquals(firstOutcome, entry.getOutcome());
				Assert.assertEquals(firstComment, entry.getComment());
			} else if (counter == 2) {
				Assert.assertEquals(processId, entry.getProcessId());
				Assert.assertEquals(mainUserId, entry.getWorkflowUser());
				Assert.assertEquals(secondHistoryTimestamp, entry.getTimeAdvanced());
				Assert.assertEquals(secondState, entry.getState());
				Assert.assertEquals(secondAction, entry.getAction());
				Assert.assertEquals(secondOutcome, entry.getOutcome());
				Assert.assertEquals(secondComment, entry.getComment());
			}

			counter++;
			
			if (counter > numberOfEntries) {
				break;
			}
		}
	}
}

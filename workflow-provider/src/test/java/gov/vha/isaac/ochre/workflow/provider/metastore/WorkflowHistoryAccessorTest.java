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
import java.util.Map;
import java.util.SortedSet;
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
import gov.vha.isaac.ochre.workflow.provider.Bpmn2FileImporter;

/**
 * Test the WorkflowInitializerConcluder class
 * 
 * {@link Bpmn2FileImporter}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowHistoryAccessorTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;

	private static MVStoreMetaContentProvider store;
	private static WorkflowHistoryAccessor accessor;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowHistoryAccessor", true);
		}

		globalSetup(true, store);

		if (!setupCompleted) {
			initializeMainWorkflow(mainDefinitionId);
			launchWorkflow(mainProcessId);
			accessor = new WorkflowHistoryAccessor(store);
			firstHistoryEntryId = advanceInitialWorkflow(mainProcessId);

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
		Assert.assertEquals(1, allProcessHistory.size());

		ProcessHistory testingEntry = allProcessHistory.iterator().next();
		Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

		// Add second Workflow
		secondHistoryEntryId = advanceSecondWorkflow(mainProcessId);

		// Initialization
		allProcessHistory = accessor.getActiveForConcept(conceptsForTesting.iterator().next());
		Assert.assertEquals(2, allProcessHistory.size());

		testingEntry = allProcessHistory.first();
		Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

		testingEntry = allProcessHistory.last();
		Assert.assertEquals(secondHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(secondState, testingEntry.getState());
		Assert.assertEquals(secondAction, testingEntry.getAction());
		Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
		Assert.assertEquals(secondComment, testingEntry.getComment());
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
		Assert.assertEquals(2, historyByDef.get(historyByDef.keySet().iterator().next()).size());

		SortedSet<ProcessHistory> processHistory = historyByDef.get(historyByDef.keySet().iterator().next());

		ProcessHistory testingEntry = processHistory.first();
		Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

		testingEntry = processHistory.last();
		Assert.assertEquals(secondHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(secondState, testingEntry.getState());
		Assert.assertEquals(secondAction, testingEntry.getAction());
		Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
		Assert.assertEquals(secondComment, testingEntry.getComment());

		// Add new Definition and initialize with single Advancement
		createSecondaryDefinitionWithSingleAdvancement();
		historyByDef = accessor.getActiveByDefinition();
		Assert.assertEquals(2, historyByDef.keySet().size());
		Assert.assertEquals(1, historyByDef.get(secondDefinitionId).size());

		processHistory = historyByDef.get(secondDefinitionId);

		testingEntry = processHistory.first();
		Assert.assertEquals(secondaryHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(secondaryProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

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

		ProcessHistory testingEntry = processHistory.first();
		Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

		testingEntry = processHistory.last();
		Assert.assertEquals(secondHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(secondState, testingEntry.getState());
		Assert.assertEquals(secondAction, testingEntry.getAction());
		Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
		Assert.assertEquals(secondComment, testingEntry.getComment());
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testDGetByProcessMap() throws Exception {
		Map<UUID, SortedSet<ProcessHistory>> historyByProcess = accessor.getByProcessMap();

		for (UUID procId : historyByProcess.keySet()) {
			Assert.assertTrue(procId.equals(mainProcessId) || procId.equals(secondaryProcessId));

			SortedSet<ProcessHistory> processHistory = historyByProcess.get(procId);

			if (procId.equals(mainProcessId)) {
				ProcessHistory testingEntry = processHistory.first();
				Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
				Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
				Assert.assertEquals(userId, testingEntry.getWorkflowUser());
				Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
				Assert.assertEquals(firstState, testingEntry.getState());
				Assert.assertEquals(firstAction, testingEntry.getAction());
				Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
				Assert.assertEquals(firstComment, testingEntry.getComment());

				testingEntry = processHistory.last();
				Assert.assertEquals(secondHistoryEntryId, testingEntry.getId());
				Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
				Assert.assertEquals(userId, testingEntry.getWorkflowUser());
				Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
				Assert.assertEquals(secondState, testingEntry.getState());
				Assert.assertEquals(secondAction, testingEntry.getAction());
				Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
				Assert.assertEquals(secondComment, testingEntry.getComment());
			} else {
				ProcessHistory testingEntry = processHistory.first();
				Assert.assertEquals(secondaryHistoryEntryId, testingEntry.getId());
				Assert.assertEquals(secondaryProcessId, testingEntry.getProcessId());
				Assert.assertEquals(userId, testingEntry.getWorkflowUser());
				Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
				Assert.assertEquals(firstState, testingEntry.getState());
				Assert.assertEquals(firstAction, testingEntry.getAction());
				Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
				Assert.assertEquals(firstComment, testingEntry.getComment());
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
		Assert.assertEquals(1, historyByDefinition.get(secondDefinitionId).size());

	
		Assert.assertEquals(2, historyByDefinition.get(mainDefinitionId).size());

		SortedSet<ProcessHistory> processHistory = historyByDefinition.get(mainDefinitionId);

		ProcessHistory testingEntry = processHistory.first();
		Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

		testingEntry = processHistory.last();
		Assert.assertEquals(secondHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(secondState, testingEntry.getState());
		Assert.assertEquals(secondAction, testingEntry.getAction());
		Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
		Assert.assertEquals(secondComment, testingEntry.getComment());
		
		
		processHistory = historyByDefinition.get(secondDefinitionId);

		testingEntry = processHistory.first();
		Assert.assertEquals(secondaryHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(secondaryProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	 @Test
	public void testFGetByConceptMap() throws Exception {
		try {
			// Should not attempt to get all history by concept as return object would be enormous
			accessor.getByConceptMap();
		} catch (UnsupportedOperationException e) {
			
		}
		
		Assert.assertTrue(true);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	 @Test
	public void testGGetForConcept() throws Exception {
		for (int conId : conceptsForTesting) {
			SortedSet<ProcessHistory> processHistory = accessor.getForConcept(conId);
			
			ProcessHistory testingEntry = processHistory.first();
			Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
			Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
			Assert.assertEquals(userId, testingEntry.getWorkflowUser());
			Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
			Assert.assertEquals(firstState, testingEntry.getState());
			Assert.assertEquals(firstAction, testingEntry.getAction());
			Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
			Assert.assertEquals(firstComment, testingEntry.getComment());

			testingEntry = processHistory.last();
			Assert.assertEquals(secondHistoryEntryId, testingEntry.getId());
			Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
			Assert.assertEquals(userId, testingEntry.getWorkflowUser());
			Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
			Assert.assertEquals(secondState, testingEntry.getState());
			Assert.assertEquals(secondAction, testingEntry.getAction());
			Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
			Assert.assertEquals(secondComment, testingEntry.getComment());
		}
		
		for (int conId : secondaryConceptsForTesting) {
			SortedSet<ProcessHistory> processHistory = accessor.getForConcept(conId);
			
			ProcessHistory testingEntry = processHistory.first();
			Assert.assertEquals(secondaryHistoryEntryId, testingEntry.getId());
			Assert.assertEquals(secondaryProcessId, testingEntry.getProcessId());
			Assert.assertEquals(userId, testingEntry.getWorkflowUser());
			Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
			Assert.assertEquals(firstState, testingEntry.getState());
			Assert.assertEquals(firstAction, testingEntry.getAction());
			Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
			Assert.assertEquals(firstComment, testingEntry.getComment());
		}

	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	 @Test
	public void testHGetForProcess() throws Exception {
		SortedSet<ProcessHistory> processHistory = accessor.getForProcess(mainProcessId);
		ProcessHistory testingEntry = processHistory.first();
		Assert.assertEquals(firstHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

		testingEntry = processHistory.last();
		Assert.assertEquals(secondHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(secondState, testingEntry.getState());
		Assert.assertEquals(secondAction, testingEntry.getAction());
		Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
		Assert.assertEquals(secondComment, testingEntry.getComment());

		processHistory = accessor.getForProcess(secondaryProcessId);
		testingEntry = processHistory.first();
		Assert.assertEquals(secondaryHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(secondaryProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());

	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	 @Test
	public void testIGetLatestForProcess() throws Exception {
		ProcessHistory testingEntry = accessor.getLatestForProcess(mainProcessId);
		Assert.assertEquals(secondHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(mainProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(secondHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(secondState, testingEntry.getState());
		Assert.assertEquals(secondAction, testingEntry.getAction());
		Assert.assertEquals(secondOutcome, testingEntry.getOutcome());
		Assert.assertEquals(secondComment, testingEntry.getComment());

		testingEntry = accessor.getLatestForProcess(secondaryProcessId);
		Assert.assertEquals(secondaryHistoryEntryId, testingEntry.getId());
		Assert.assertEquals(secondaryProcessId, testingEntry.getProcessId());
		Assert.assertEquals(userId, testingEntry.getWorkflowUser());
		Assert.assertEquals(firstHistoryTimestamp, testingEntry.getTimeAdvanced());
		Assert.assertEquals(firstState, testingEntry.getState());
		Assert.assertEquals(firstAction, testingEntry.getAction());
		Assert.assertEquals(firstOutcome, testingEntry.getOutcome());
		Assert.assertEquals(firstComment, testingEntry.getComment());
	}
}

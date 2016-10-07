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
package gov.vha.isaac.ochre.workflow;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.RecursiveDelete;
import gov.vha.isaac.ochre.workflow.model.WorkflowContentStore;
import gov.vha.isaac.ochre.workflow.model.contents.AbstractStorableWorkflowContents;
import gov.vha.isaac.ochre.workflow.model.contents.AvailableAction;
import gov.vha.isaac.ochre.workflow.model.contents.DefinitionDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessHistory;
import gov.vha.isaac.ochre.workflow.provider.WorkflowProvider;

/**
 * Test both static and user based workflow content as defined in the
 * metacontent-store
 * 
 * {@link WorkflowContentStore} {@link WorkflowProvider}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowContentStoreTest {

	/**
	 * Sets the up.
	 */
	@Before
	public void setUp() {
		WorkflowProvider.BPMN_PATH = null;
		LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(new File("target/store").toPath());
		LookupService.startupMetadataStore();
	}

	/**
	 * Tear down.
	 * @throws IOException 
	 */
	@After
	public void tearDown() throws IOException {
		LookupService.shutdownIsaac();
		RecursiveDelete.delete(new File("target/store"));
	}

	/**
	 * Test available action store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testAvailableActionStore() throws Exception {
		AvailableAction createdEntry1 = new AvailableAction(UUID.randomUUID(), "EDIT", "REVIEW", "REVIEW", "REVIEWER");

		// New scope to ensure closing store
		WorkflowContentStore<AvailableAction> availableActionStore = LookupService.get().getService(WorkflowProvider.class).getAvailableActionStore();

		// Add new entry
		UUID key1 = availableActionStore.add(createdEntry1);
		LookupService.setRunLevel(LookupService.WORKERS_STARTED_RUNLEVEL);  //bring down the metacontent store
		LookupService.startupMetadataStore();

		// Get entry with new store
		availableActionStore = LookupService.get().getService(WorkflowProvider.class).getAvailableActionStore();
		AvailableAction pulledEntry1 = availableActionStore.get(key1);

		Assert.assertEquals(availableActionStore.size(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		AvailableAction createdEntry2 = new AvailableAction(UUID.randomUUID(), "REVIEW", "APPROVE", "APPROVE",
				"APPROVER");
		UUID key2 = availableActionStore.add(createdEntry2);
		Assert.assertEquals(availableActionStore.size(), 2);

		// Verify entries are as expected
		AvailableAction pulledEntry2 = availableActionStore.get(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<AvailableAction> allEntries = availableActionStore.values();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		AvailableAction updatedEntry2 = new AvailableAction(createdEntry2.getDefinitionId(), "REVIEW",
				"Ready for Approval", "APPROVE", "APPROVER");
		availableActionStore.put(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = availableActionStore.get(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getDefinitionId(), pulledEntry2.getDefinitionId());
		Assert.assertEquals(createdEntry2.getInitialState(), pulledEntry2.getInitialState());
		Assert.assertEquals(createdEntry2.getOutcomeState(), pulledEntry2.getOutcomeState());
		Assert.assertEquals(createdEntry2.getRole(), pulledEntry2.getRole());
		Assert.assertNotEquals(createdEntry2.getAction(), pulledEntry2.getAction());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		// Test Removing single entry
		availableActionStore.remove(key2);
		Assert.assertEquals(availableActionStore.size(), 1);
		allEntries = availableActionStore.values();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = availableActionStore.add(createdEntry2);
		Assert.assertEquals(availableActionStore.size(), 2);

		// Test Removing all entries
		availableActionStore.clear();
		Assert.assertEquals(availableActionStore.size(), 0);
		allEntries = availableActionStore.values();
		Assert.assertEquals(allEntries.size(), 0);
	}

	/**
	 * Test historical workflow store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testHistoricalWorkflowStore() throws Exception {
		ProcessHistory createdEntry1 = new ProcessHistory(UUID.randomUUID(), UUID.randomUUID(), new Date().getTime(), "Edit", "Review",
				"Ready for Approval", "No issues found", 1);

		WorkflowContentStore<ProcessHistory> historicalWorkflowStore = LookupService.get().getService(WorkflowProvider.class).getProcessHistoryStore();

		// Add new entry
		UUID key1 = historicalWorkflowStore.add(createdEntry1);
		LookupService.setRunLevel(LookupService.WORKERS_STARTED_RUNLEVEL);  //bring down the metacontent store
		LookupService.startupMetadataStore();

		// Get entry with new store
		historicalWorkflowStore = LookupService.get().getService(WorkflowProvider.class).getProcessHistoryStore();
		ProcessHistory pulledEntry1 = historicalWorkflowStore.get(key1);

		Assert.assertEquals(historicalWorkflowStore.size(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		ProcessHistory createdEntry2 = new ProcessHistory(UUID.randomUUID(), UUID.randomUUID(), new Date().getTime(), "Commit", "Edit",
				"Ready for Review", "", 1);
		UUID key2 = historicalWorkflowStore.add(createdEntry2);
		Assert.assertEquals(historicalWorkflowStore.size(), 2);

		// Verify entries are as expected
		ProcessHistory pulledEntry2 = historicalWorkflowStore.get(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<ProcessHistory> allEntries = historicalWorkflowStore.values();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		ProcessHistory updatedEntry2 = new ProcessHistory(createdEntry2.getProcessId(), createdEntry2.getUserId(),
				createdEntry2.getTimeAdvanced(), "Commit", "Edit", "Ready for Review",
				"Added description I think is missing", 2);
		historicalWorkflowStore.put(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = historicalWorkflowStore.get(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getProcessId(), pulledEntry2.getProcessId());
		Assert.assertEquals(createdEntry2.getUserId(), pulledEntry2.getUserId());
		Assert.assertEquals(createdEntry2.getTimeAdvanced(), pulledEntry2.getTimeAdvanced());
		Assert.assertEquals(createdEntry2.getInitialState(), pulledEntry2.getInitialState());
		Assert.assertEquals(createdEntry2.getAction(), pulledEntry2.getAction());
		Assert.assertEquals(createdEntry2.getOutcomeState(), pulledEntry2.getOutcomeState());
		Assert.assertNotEquals(createdEntry2.getComment(), pulledEntry2.getComment());
		Assert.assertNotEquals(createdEntry2.getHistorySequence(), pulledEntry2.getHistorySequence());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		// Test Removing single entry
		historicalWorkflowStore.remove(key2);
		Assert.assertEquals(historicalWorkflowStore.size(), 1);
		allEntries = historicalWorkflowStore.values();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = historicalWorkflowStore.add(createdEntry2);
		Assert.assertEquals(historicalWorkflowStore.size(), 2);

		// Test Removing all entries
		historicalWorkflowStore.clear();
		Assert.assertEquals(historicalWorkflowStore.size(), 0);
		allEntries = historicalWorkflowStore.values();
		Assert.assertEquals(allEntries.size(), 0);
	}

	/**
	 * Test process definition store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testProcessInstanceStore() throws Exception {
		String name = "Process Name";
		String description = "Process Description";

		ProcessDetail createdEntry1 = new ProcessDetail(UUID.randomUUID(), UUID.randomUUID(), new Date().getTime(), ProcessStatus.DEFINED, name, description);

		WorkflowContentStore<ProcessDetail> processInstanceStore = LookupService.get().getService(WorkflowProvider.class).getProcessDetailStore();

		// Add new entry
		UUID key1 = processInstanceStore.add(createdEntry1);
		LookupService.setRunLevel(LookupService.WORKERS_STARTED_RUNLEVEL);  //bring down the metacontent store
		LookupService.startupMetadataStore();

		// Get entry with new store
		processInstanceStore = LookupService.get().getService(WorkflowProvider.class).getProcessDetailStore();
		AbstractStorableWorkflowContents pulledEntry1 = processInstanceStore.get(key1);

		Assert.assertEquals(processInstanceStore.size(), 1);
		Assert.assertEquals(createdEntry1.getCreatorId(), createdEntry1.getCreatorId());
		Assert.assertEquals(createdEntry1, pulledEntry1);
		
		// Add second entry
		ProcessDetail createdEntry2 = new ProcessDetail(UUID.randomUUID(), UUID.randomUUID(), new Date().getTime(), ProcessStatus.DEFINED, name, description);

		UUID key2 = processInstanceStore.add(createdEntry2);
		Assert.assertEquals(processInstanceStore.size(), 2);

		// Verify entries are as expected
		ProcessDetail pulledEntry2 = processInstanceStore.get(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<ProcessDetail> allEntries = processInstanceStore.values();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		ProcessDetail updatedEntry2 = new ProcessDetail(createdEntry2.getDefinitionId(), createdEntry2.getCreatorId(),
				createdEntry2.getTimeCreated(), ProcessStatus.DEFINED, createdEntry2.getName(), "This is a second Description");
		processInstanceStore.put(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = processInstanceStore.get(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getDefinitionId(), pulledEntry2.getDefinitionId());
		Assert.assertEquals(createdEntry2.getCreatorId(), pulledEntry2.getCreatorId());
		Assert.assertEquals(createdEntry2.getTimeCreated(), pulledEntry2.getTimeCreated());
		Assert.assertEquals(createdEntry2.getStatus(), pulledEntry2.getStatus());
		Assert.assertEquals(createdEntry2.getName(), pulledEntry2.getName());
		Assert.assertEquals(createdEntry2.getOwnerId(), pulledEntry2.getOwnerId());
		Assert.assertNotEquals(createdEntry2.getDescription(), pulledEntry2.getDescription());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		// Test Removing single entry
		processInstanceStore.remove(key2);
		Assert.assertEquals(processInstanceStore.size(), 1);
		allEntries = processInstanceStore.values();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = processInstanceStore.add(createdEntry2);
		Assert.assertEquals(processInstanceStore.size(), 2);

		// Test Removing all entries
		processInstanceStore.clear();
		Assert.assertEquals(processInstanceStore.size(), 0);
		allEntries = processInstanceStore.values();
		Assert.assertEquals(allEntries.size(), 0);
	}

	/**
	 * Test process definition store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testDefinitionDetailsStore() throws Exception {
		Set<String> roles1 = new HashSet<>();
		roles1.add("Editor");
		roles1.add("Reviewer");
		String description = "This is the description for this unit test";

		DefinitionDetail createdEntry1 = new DefinitionDetail("BPMN2 ID-X", "JUnit BPMN2", "Testing", "1.0", roles1,
				description);

		// New scope to ensure closing store
		WorkflowContentStore<DefinitionDetail> definitionDetailStore = LookupService.get().getService(WorkflowProvider.class).getDefinitionDetailStore();

		// Add new entry
		UUID key1 = definitionDetailStore.add(createdEntry1);
		LookupService.setRunLevel(LookupService.WORKERS_STARTED_RUNLEVEL);  //bring down the metacontent store
		LookupService.startupMetadataStore();

		// Get entry with new store
		definitionDetailStore = LookupService.get().getService(WorkflowProvider.class).getDefinitionDetailStore();
		DefinitionDetail pulledEntry1 = definitionDetailStore.get(key1);

		Assert.assertEquals(definitionDetailStore.size(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		Set<String> roles2 = new HashSet<>();
		roles2.add("Editor");
		roles2.add("Approver");
		DefinitionDetail createdEntry2 = new DefinitionDetail("BPMN2 ID-Y", "JUnit BPMN2", "Testing", "1.0", roles2,
				description);

		UUID key2 = definitionDetailStore.add(createdEntry2);
		Assert.assertEquals(definitionDetailStore.size(), 2);

		// Verify entries are as expected
		DefinitionDetail pulledEntry2 = definitionDetailStore.get(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<DefinitionDetail> allEntries = definitionDetailStore.values();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));
		
		Thread.sleep(1);

		// Test update of an entry
		DefinitionDetail updatedEntry2 = new DefinitionDetail(createdEntry2.getBpmn2Id(), createdEntry2.getName(),
				createdEntry2.getNamespace(), "2.0", createdEntry2.getRoles(), createdEntry2.getDescription());
		definitionDetailStore.put(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = definitionDetailStore.get(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getBpmn2Id(), pulledEntry2.getBpmn2Id());
		Assert.assertEquals(createdEntry2.getName(), pulledEntry2.getName());
		Assert.assertEquals(createdEntry2.getNamespace(), pulledEntry2.getNamespace());
		Assert.assertEquals(createdEntry2.getRoles(), pulledEntry2.getRoles());
		Assert.assertNotEquals(createdEntry2.getVersion(), pulledEntry2.getVersion());
		Assert.assertEquals(createdEntry2.getDescription(), pulledEntry2.getDescription());
		Assert.assertNotEquals(createdEntry2.getImportDate(), pulledEntry2.getImportDate());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		// Test Removing single entry
		definitionDetailStore.remove(key2);
		Assert.assertEquals(definitionDetailStore.size(), 1);
		allEntries = definitionDetailStore.values();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = definitionDetailStore.add(createdEntry2);
		Assert.assertEquals(definitionDetailStore.size(), 2);

		// Test Removing all entries
		definitionDetailStore.clear();
		Assert.assertEquals(definitionDetailStore.size(), 0);
		allEntries = definitionDetailStore.values();
		Assert.assertEquals(allEntries.size(), 0);
	}
}

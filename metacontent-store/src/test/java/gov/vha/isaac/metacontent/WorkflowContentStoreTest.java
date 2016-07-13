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
package gov.vha.isaac.metacontent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.metacontent.workflow.AuthorPermissionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.AvailableActionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.HistoricalWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDefinitionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AuthorPermission;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.HistoricalWorkflow;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDefinition;

/**
 * Test both static and user based workflow content as defined in the
 * metacontent-store
 *
 * {@link WorkflowContentStoreTest}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowContentStoreTest {
	private MVStoreMetaContentProvider store;

	@Before
	public void setUp() {
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", true);
	}

	@After
	public void tearDown() {
		store.close();
	}

	@Test
	public void testAuthorPermissionStore() throws Exception {
		Set<String> roles1 = new HashSet<>();
		roles1.add("Role A");
		roles1.add("Role B");
		AuthorPermission createdEntry1 = new AuthorPermission(1, roles1);

		// New scope to ensure closing store
		AuthorPermissionWorkflowContentStore availableActionStore = new AuthorPermissionWorkflowContentStore(store);
    
		// Add new entry
		UUID key1 = availableActionStore.addEntry(createdEntry1);
		store.close();
		
		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		availableActionStore = new AuthorPermissionWorkflowContentStore(store);
		AuthorPermission pulledEntry1 = availableActionStore.getEntry(key1);
		
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		Set<String> roles2 = new HashSet<>();
		roles2.add("Role C");
		roles2.add("Role D");
		AuthorPermission createdEntry2 = new AuthorPermission(2, roles2);

		UUID key2 = availableActionStore.addEntry(createdEntry2);
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		AuthorPermission pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<AuthorPermission> allEntries = availableActionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		Set<String> roles3 = new HashSet<>();
		roles3.add("Role E");
		roles3.add("Role F");
		AuthorPermission updatedEntry2 = new AuthorPermission(2, roles3);
		availableActionStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);
		
		pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);
		
		Assert.assertEquals(createdEntry2.getAuthor(), pulledEntry2.getAuthor());
		Assert.assertNotEquals(createdEntry2.getRoles(), pulledEntry2.getRoles());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		
		
		// Test Removing single entry
		availableActionStore.removeEntry(key2);
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 1);
		allEntries = availableActionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = availableActionStore.addEntry(createdEntry2);
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 2);

		// Test Removing all entries
		availableActionStore.removeAllEntries();
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 0);
		allEntries = availableActionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 0);
	}
	
	@Test
	public void testAvailableActionStore() throws Exception {
		AvailableAction createdEntry1 = new AvailableAction("EDIT", "REVIEW", "REVIEW", "REVIEWER");

		// New scope to ensure closing store
		AvailableActionWorkflowContentStore availableActionStore = new AvailableActionWorkflowContentStore(store);
    
		// Add new entry
		UUID key1 = availableActionStore.addEntry(createdEntry1);
		store.close();
		
		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		availableActionStore = new AvailableActionWorkflowContentStore(store);
		AvailableAction pulledEntry1 = availableActionStore.getEntry(key1);
		
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		AvailableAction createdEntry2 = new AvailableAction("REVIEW", "APPROVE", "APPROVE", "APPROVER");
		UUID key2 = availableActionStore.addEntry(createdEntry2);
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		AvailableAction pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<AvailableAction> allEntries = availableActionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		AvailableAction updatedEntry2 = new AvailableAction("REVIEW", "Ready for Approval", "APPROVE", "APPROVER");
		availableActionStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);
		
		pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);
		
		Assert.assertEquals(createdEntry2.getCurrentState(), pulledEntry2.getCurrentState());
		Assert.assertEquals(createdEntry2.getOutcome(), pulledEntry2.getOutcome());
		Assert.assertEquals(createdEntry2.getRole(), pulledEntry2.getRole());
		Assert.assertNotEquals(createdEntry2.getAction(), pulledEntry2.getAction());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		
		
		// Test Removing single entry
		availableActionStore.removeEntry(key2);
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 1);
		allEntries = availableActionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = availableActionStore.addEntry(createdEntry2);
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 2);

		// Test Removing all entries
		availableActionStore.removeAllEntries();
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 0);
		allEntries = availableActionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 0);
	}

	@Test
	public void testHistoricalWorkflowStore() throws Exception {
		HistoricalWorkflow createdEntry1 = new HistoricalWorkflow(UUID.randomUUID(), 1, new Date().getTime(), "Edit", "Review", "Ready for Approval", "No issues found");

		// New scope to ensure closing store
		HistoricalWorkflowContentStore historicalWorkflowStore = new HistoricalWorkflowContentStore(store);
    
		// Add new entry
		UUID key1 = historicalWorkflowStore.addEntry(createdEntry1);
		store.close();
		
		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		historicalWorkflowStore = new HistoricalWorkflowContentStore(store);
		HistoricalWorkflow pulledEntry1 = historicalWorkflowStore.getEntry(key1);
		
		Assert.assertEquals(historicalWorkflowStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		HistoricalWorkflow createdEntry2 = new HistoricalWorkflow(UUID.randomUUID(), 2, new Date().getTime(), "Commit", "Edit", "Ready for Review", "");
		UUID key2 = historicalWorkflowStore.addEntry(createdEntry2);
		Assert.assertEquals(historicalWorkflowStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		HistoricalWorkflow pulledEntry2 = historicalWorkflowStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<HistoricalWorkflow> allEntries = historicalWorkflowStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		HistoricalWorkflow updatedEntry2 = new HistoricalWorkflow(createdEntry2.getTaskId(), 2, createdEntry2.getTimeAdvanced(), "Commit", "Edit", "Ready for Review", "Added description I think is missing");
		historicalWorkflowStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);
		
		pulledEntry2 = historicalWorkflowStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);
		
		Assert.assertEquals(createdEntry2.getTaskId(), pulledEntry2.getTaskId());
		Assert.assertEquals(createdEntry2.getAdvancer(), pulledEntry2.getAdvancer());
		Assert.assertEquals(createdEntry2.getTimeAdvanced(), pulledEntry2.getTimeAdvanced());
		Assert.assertEquals(createdEntry2.getState(), pulledEntry2.getState());
		Assert.assertEquals(createdEntry2.getAction(), pulledEntry2.getAction());
		Assert.assertEquals(createdEntry2.getOutcome(), pulledEntry2.getOutcome());
		Assert.assertNotEquals(createdEntry2.getComment(), pulledEntry2.getComment());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		
		
		// Test Removing single entry
		historicalWorkflowStore.removeEntry(key2);
		Assert.assertEquals(historicalWorkflowStore.getNumberOfEntries(), 1);
		allEntries = historicalWorkflowStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = historicalWorkflowStore.addEntry(createdEntry2);
		Assert.assertEquals(historicalWorkflowStore.getNumberOfEntries(), 2);

		// Test Removing all entries
		historicalWorkflowStore.removeAllEntries();
		Assert.assertEquals(historicalWorkflowStore.getNumberOfEntries(), 0);
		allEntries = historicalWorkflowStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 0);
	}

	@Test
	public void testProcessDefinitionStore() throws Exception {
		List<Integer> sequences1 = new ArrayList<>();
		sequences1.add(90);
		sequences1.add(91);
		ProcessDefinition createdEntry1 = new ProcessDefinition(sequences1, UUID.randomUUID(), 2, new Date().getTime());

		// New scope to ensure closing store
		ProcessDefinitionWorkflowContentStore processDefinitionStore = new ProcessDefinitionWorkflowContentStore(store);
    
		// Add new entry
		UUID key1 = processDefinitionStore.addEntry(createdEntry1);
		store.close();
		
		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		processDefinitionStore = new ProcessDefinitionWorkflowContentStore(store);
		ProcessDefinition pulledEntry1 = processDefinitionStore.getEntry(key1);
		
		Assert.assertEquals(processDefinitionStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		List<Integer> sequences2 = new ArrayList<>();
		sequences2.add(90);
		sequences2.add(91);
		ProcessDefinition createdEntry2 = new ProcessDefinition(sequences2, UUID.randomUUID(), 3, new Date().getTime());

		UUID key2 = processDefinitionStore.addEntry(createdEntry2);
		Assert.assertEquals(processDefinitionStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		ProcessDefinition pulledEntry2 = processDefinitionStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<ProcessDefinition> allEntries = processDefinitionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		ProcessDefinition updatedEntry2 = new ProcessDefinition(sequences2, UUID.randomUUID(), 3, createdEntry2.getTimeCreated());
		processDefinitionStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);
		
		pulledEntry2 = processDefinitionStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);
		
		Assert.assertEquals(createdEntry2.getStampSequences(), pulledEntry2.getStampSequences());
		Assert.assertEquals(createdEntry2.getCreator(), pulledEntry2.getCreator());
		Assert.assertEquals(createdEntry2.getTimeCreated(), pulledEntry2.getTimeCreated());
		Assert.assertNotEquals(createdEntry2.getConcept(), pulledEntry2.getConcept());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		
		
		// Test Removing single entry
		processDefinitionStore.removeEntry(key2);
		Assert.assertEquals(processDefinitionStore.getNumberOfEntries(), 1);
		allEntries = processDefinitionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = processDefinitionStore.addEntry(createdEntry2);
		Assert.assertEquals(processDefinitionStore.getNumberOfEntries(), 2);

		// Test Removing all entries
		processDefinitionStore.removeAllEntries();
		Assert.assertEquals(processDefinitionStore.getNumberOfEntries(), 0);
		allEntries = processDefinitionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 0);
	}
}

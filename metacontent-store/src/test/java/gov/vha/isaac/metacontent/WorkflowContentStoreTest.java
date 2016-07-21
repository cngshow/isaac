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

import gov.vha.isaac.metacontent.workflow.AvailableActionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailsWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserWorkflowPermissionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserWorkflowPermission;

/**
 * Test both static and user based workflow content as defined in the
 * metacontent-store
 * 
 * {@link UserWorkflowPermissionWorkflowContentStore}
 * {@link AvailableActionWorkflowContentStore}
 * {@link DefinitionDetailWorkflowContentStore}
 * {@link ProcessHistoryContentStore} {@link ProcessDetailsWorkflowContentStore}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowContentStoreTest {

	/** The store. */
	private MVStoreMetaContentProvider store;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUp() {
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", true);
	}

	/**
	 * Tear down.
	 */
	@After
	public void tearDown() {
		store.close();
	}

	/**
	 * Test author permission store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testUserWorkflowPermissionStore() throws Exception {
		Set<String> roles1 = new HashSet<>();
		roles1.add("Role A");
		roles1.add("Role B");
		UserWorkflowPermission createdEntry1 = new UserWorkflowPermission(UUID.randomUUID(), 1, roles1);

		// New scope to ensure closing store
		UserWorkflowPermissionWorkflowContentStore availableActionStore = new UserWorkflowPermissionWorkflowContentStore(
				store);

		// Add new entry
		UUID key1 = availableActionStore.addEntry(createdEntry1);
		store.close();

		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		availableActionStore = new UserWorkflowPermissionWorkflowContentStore(store);
		UserWorkflowPermission pulledEntry1 = availableActionStore.getEntry(key1);

		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		Set<String> roles2 = new HashSet<>();
		roles2.add("Role C");
		roles2.add("Role D");
		UserWorkflowPermission createdEntry2 = new UserWorkflowPermission(UUID.randomUUID(), 2, roles2);

		UUID key2 = availableActionStore.addEntry(createdEntry2);
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		UserWorkflowPermission pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<UserWorkflowPermission> allEntries = availableActionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		Set<String> roles3 = new HashSet<>();
		roles3.add("Role E");
		roles3.add("Role F");
		UserWorkflowPermission updatedEntry2 = new UserWorkflowPermission(createdEntry2.getDefinitionId(), 2, roles3);
		availableActionStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getDefinitionId(), pulledEntry2.getDefinitionId());
		Assert.assertEquals(createdEntry2.getUser(), pulledEntry2.getUser());
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
		AvailableAction createdEntry2 = new AvailableAction(UUID.randomUUID(), "REVIEW", "APPROVE", "APPROVE",
				"APPROVER");
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
		AvailableAction updatedEntry2 = new AvailableAction(createdEntry2.getDefinitionId(), "REVIEW",
				"Ready for Approval", "APPROVE", "APPROVER");
		availableActionStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getDefinitionId(), pulledEntry2.getDefinitionId());
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

	/**
	 * Test historical workflow store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testHistoricalWorkflowStore() throws Exception {
		ProcessHistory createdEntry1 = new ProcessHistory(UUID.randomUUID(), 1, new Date().getTime(), "Edit", "Review",
				"Ready for Approval", "No issues found");

		// New scope to ensure closing store
		ProcessHistoryContentStore historicalWorkflowStore = new ProcessHistoryContentStore(store);

		// Add new entry
		UUID key1 = historicalWorkflowStore.addEntry(createdEntry1);
		store.close();

		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		historicalWorkflowStore = new ProcessHistoryContentStore(store);
		ProcessHistory pulledEntry1 = historicalWorkflowStore.getEntry(key1);

		Assert.assertEquals(historicalWorkflowStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		ProcessHistory createdEntry2 = new ProcessHistory(UUID.randomUUID(), 2, new Date().getTime(), "Commit", "Edit",
				"Ready for Review", "");
		UUID key2 = historicalWorkflowStore.addEntry(createdEntry2);
		Assert.assertEquals(historicalWorkflowStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		ProcessHistory pulledEntry2 = historicalWorkflowStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<ProcessHistory> allEntries = historicalWorkflowStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		ProcessHistory updatedEntry2 = new ProcessHistory(createdEntry2.getProcessId(), 2,
				createdEntry2.getTimeAdvanced(), "Commit", "Edit", "Ready for Review",
				"Added description I think is missing");
		historicalWorkflowStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = historicalWorkflowStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getProcessId(), pulledEntry2.getProcessId());
		Assert.assertEquals(createdEntry2.getWorkflowUser(), pulledEntry2.getWorkflowUser());
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

	/**
	 * Test process definition store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testProcessInstanceStore() throws Exception {
		List<Integer> sequences1 = new ArrayList<>();
		sequences1.add(90);
		sequences1.add(91);
		ProcessDetail createdEntry1 = new ProcessDetail(UUID.randomUUID(), UUID.randomUUID(), sequences1, 2,
				new Date().getTime(), true);

		// New scope to ensure closing store
		ProcessDetailsWorkflowContentStore processInstanceStore = new ProcessDetailsWorkflowContentStore(store);

		// Add new entry
		UUID key1 = processInstanceStore.addEntry(createdEntry1);
		store.close();

		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		processInstanceStore = new ProcessDetailsWorkflowContentStore(store);
		ProcessDetail pulledEntry1 = processInstanceStore.getEntry(key1);

		Assert.assertEquals(processInstanceStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		List<Integer> sequences2 = new ArrayList<>();
		sequences2.add(90);
		sequences2.add(91);
		ProcessDetail createdEntry2 = new ProcessDetail(UUID.randomUUID(), UUID.randomUUID(), sequences2, 3,
				new Date().getTime(), true);

		UUID key2 = processInstanceStore.addEntry(createdEntry2);
		Assert.assertEquals(processInstanceStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		ProcessDetail pulledEntry2 = processInstanceStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<ProcessDetail> allEntries = processInstanceStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		ProcessDetail updatedEntry2 = new ProcessDetail(createdEntry2.getDefinitionId(), UUID.randomUUID(), sequences2,
				3, createdEntry2.getTimeCreated(), true);
		processInstanceStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = processInstanceStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getDefinitionId(), pulledEntry2.getDefinitionId());
		Assert.assertEquals(createdEntry2.getStampSequences(), pulledEntry2.getStampSequences());
		Assert.assertEquals(createdEntry2.getCreator(), pulledEntry2.getCreator());
		Assert.assertEquals(createdEntry2.getTimeCreated(), pulledEntry2.getTimeCreated());
		Assert.assertNotEquals(createdEntry2.getConcept(), pulledEntry2.getConcept());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		// Test Removing single entry
		processInstanceStore.removeEntry(key2);
		Assert.assertEquals(processInstanceStore.getNumberOfEntries(), 1);
		allEntries = processInstanceStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = processInstanceStore.addEntry(createdEntry2);
		Assert.assertEquals(processInstanceStore.getNumberOfEntries(), 2);

		// Test Removing all entries
		processInstanceStore.removeAllEntries();
		Assert.assertEquals(processInstanceStore.getNumberOfEntries(), 0);
		allEntries = processInstanceStore.getAllEntries();
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
		DefinitionDetail createdEntry1 = new DefinitionDetail("BPMN2 ID-X", "JUnit BPMN2", "Testing", "1.0", roles1);

		// New scope to ensure closing store
		DefinitionDetailWorkflowContentStore definitionDetailsStore = new DefinitionDetailWorkflowContentStore(store);

		// Add new entry
		UUID key1 = definitionDetailsStore.addEntry(createdEntry1);
		store.close();

		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		definitionDetailsStore = new DefinitionDetailWorkflowContentStore(store);
		DefinitionDetail pulledEntry1 = definitionDetailsStore.getEntry(key1);

		Assert.assertEquals(definitionDetailsStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		Set<String> roles2 = new HashSet<>();
		roles2.add("Editor");
		roles2.add("Approver");
		DefinitionDetail createdEntry2 = new DefinitionDetail("BPMN2 ID-Y", "JUnit BPMN2", "Testing", "1.0", roles2);

		UUID key2 = definitionDetailsStore.addEntry(createdEntry2);
		Assert.assertEquals(definitionDetailsStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		DefinitionDetail pulledEntry2 = definitionDetailsStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<DefinitionDetail> allEntries = definitionDetailsStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		DefinitionDetail updatedEntry2 = new DefinitionDetail(createdEntry2.getBpmn2Id(), createdEntry2.getName(),
				createdEntry2.getNamespace(), "2.0", createdEntry2.getRoles());
		definitionDetailsStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = definitionDetailsStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getBpmn2Id(), pulledEntry2.getBpmn2Id());
		Assert.assertEquals(createdEntry2.getName(), pulledEntry2.getName());
		Assert.assertEquals(createdEntry2.getNamespace(), pulledEntry2.getNamespace());
		Assert.assertEquals(createdEntry2.getRoles(), pulledEntry2.getRoles());
		Assert.assertNotEquals(createdEntry2.getVersion(), pulledEntry2.getVersion());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		// Test Removing single entry
		definitionDetailsStore.removeEntry(key2);
		Assert.assertEquals(definitionDetailsStore.getNumberOfEntries(), 1);
		allEntries = definitionDetailsStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = definitionDetailsStore.addEntry(createdEntry2);
		Assert.assertEquals(definitionDetailsStore.getNumberOfEntries(), 2);

		// Test Removing all entries
		definitionDetailsStore.removeAllEntries();
		Assert.assertEquals(definitionDetailsStore.getNumberOfEntries(), 0);
		allEntries = definitionDetailsStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 0);
	}
}

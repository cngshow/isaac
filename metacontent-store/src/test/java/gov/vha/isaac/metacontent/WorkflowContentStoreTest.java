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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.metacontent.workflow.AvailableActionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.DefinitionDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.DomainStandardWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessDetailWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.ProcessHistoryContentStore;
import gov.vha.isaac.metacontent.workflow.UserPermissionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.DomainStandard;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.DefiningStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.metacontent.workflow.contents.ProcessHistory;
import gov.vha.isaac.metacontent.workflow.contents.UserPermission;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.WorkflowDataElement;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.WorkflowDomain;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContents.WorkflowTerminology;

/**
 * Test both static and user based workflow content as defined in the
 * metacontent-store
 * 
 * {@link UserPermissionWorkflowContentStore}
 * {@link AvailableActionWorkflowContentStore}
 * {@link DefinitionDetailWorkflowContentStore}
 * {@link ProcessHistoryContentStore} {@link ProcessDetailWorkflowContentStore}
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
	 * Test user permission store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testUserPermissionStore() throws Exception {
		DomainStandard standard = new DomainStandard(WorkflowDomain.ADMISSIONS, createTestingElementTerminologyMap());
		UserPermission createdEntry1 = new UserPermission(UUID.randomUUID(), 1, "Role A", standard);

		// New scope to ensure closing store
		UserPermissionWorkflowContentStore availableActionStore = new UserPermissionWorkflowContentStore(store);

		// Add new entry
		UUID key1 = availableActionStore.addEntry(createdEntry1);
		store.close();

		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		availableActionStore = new UserPermissionWorkflowContentStore(store);
		UserPermission pulledEntry1 = availableActionStore.getEntry(key1);

		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		UserPermission createdEntry2 = new UserPermission(UUID.randomUUID(), 2, "Role B", standard);

		UUID key2 = availableActionStore.addEntry(createdEntry2);
		Assert.assertEquals(availableActionStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		UserPermission pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<UserPermission> allEntries = availableActionStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		UserPermission updatedEntry2 = new UserPermission(createdEntry2.getDefinitionId(), 2, "Role C", standard);
		availableActionStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = availableActionStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getDefinitionId(), pulledEntry2.getDefinitionId());
		Assert.assertEquals(createdEntry2.getUser(), pulledEntry2.getUser());
		Assert.assertEquals(createdEntry2.getDomainStandard(), pulledEntry2.getDomainStandard());
		Assert.assertNotEquals(createdEntry2.getRole(), pulledEntry2.getRole());

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
		Set<Integer> concepts1 = new HashSet<>();
		concepts1.add(98);
		concepts1.add(99);
		DomainStandard domain = new DomainStandard(WorkflowDomain.ADMISSIONS, createTestingElementTerminologyMap());

		ProcessDetail createdEntry1 = new ProcessDetail(UUID.randomUUID(), concepts1, sequences1, 2,
				new Date().getTime(), true, SubjectMatter.CONCEPT, DefiningStatus.ENABLED, domain);

		// New scope to ensure closing store
		ProcessDetailWorkflowContentStore processInstanceStore = new ProcessDetailWorkflowContentStore(store);

		// Add new entry
		UUID key1 = processInstanceStore.addEntry(createdEntry1);
		store.close();

		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		processInstanceStore = new ProcessDetailWorkflowContentStore(store);
		ProcessDetail pulledEntry1 = processInstanceStore.getEntry(key1);

		Assert.assertEquals(processInstanceStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		List<Integer> sequences2 = new ArrayList<>();
		sequences2.add(90);
		sequences2.add(91);
		Set<Integer> concepts2 = new HashSet<>();
		concepts2.add(98);
		concepts2.add(97);
		ProcessDetail createdEntry2 = new ProcessDetail(UUID.randomUUID(), concepts2, sequences2, 3,
				new Date().getTime(), true, SubjectMatter.CONCEPT, DefiningStatus.ENABLED, domain);

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
		ProcessDetail updatedEntry2 = new ProcessDetail(createdEntry2.getDefinitionId(), concepts1, sequences2, 3,
				createdEntry2.getTimeCreated(), true, SubjectMatter.CONCEPT, DefiningStatus.ENABLED, domain);
		processInstanceStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = processInstanceStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getDefinitionId(), pulledEntry2.getDefinitionId());
		Assert.assertEquals(createdEntry2.getStampSequences(), pulledEntry2.getStampSequences());
		Assert.assertEquals(createdEntry2.getCreator(), pulledEntry2.getCreator());
		Assert.assertEquals(createdEntry2.getTimeCreated(), pulledEntry2.getTimeCreated());
		Assert.assertEquals(createdEntry2.isActive(), pulledEntry2.isActive());
		Assert.assertEquals(createdEntry2.getSubjectMatter(), pulledEntry2.getSubjectMatter());
		Assert.assertEquals(createdEntry2.getDefiningStatus(), pulledEntry2.getDefiningStatus());
		Assert.assertEquals(createdEntry2.getDomainStandard(), pulledEntry2.getDomainStandard());
		Assert.assertNotEquals(createdEntry2.getConcepts(), pulledEntry2.getConcepts());

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
		DefinitionDetailWorkflowContentStore definitionDetailStore = new DefinitionDetailWorkflowContentStore(store);

		// Add new entry
		UUID key1 = definitionDetailStore.addEntry(createdEntry1);
		store.close();

		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		definitionDetailStore = new DefinitionDetailWorkflowContentStore(store);
		DefinitionDetail pulledEntry1 = definitionDetailStore.getEntry(key1);

		Assert.assertEquals(definitionDetailStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		Set<String> roles2 = new HashSet<>();
		roles2.add("Editor");
		roles2.add("Approver");
		DefinitionDetail createdEntry2 = new DefinitionDetail("BPMN2 ID-Y", "JUnit BPMN2", "Testing", "1.0", roles2);

		UUID key2 = definitionDetailStore.addEntry(createdEntry2);
		Assert.assertEquals(definitionDetailStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		DefinitionDetail pulledEntry2 = definitionDetailStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<DefinitionDetail> allEntries = definitionDetailStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		DefinitionDetail updatedEntry2 = new DefinitionDetail(createdEntry2.getBpmn2Id(), createdEntry2.getName(),
				createdEntry2.getNamespace(), "2.0", createdEntry2.getRoles());
		definitionDetailStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = definitionDetailStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getBpmn2Id(), pulledEntry2.getBpmn2Id());
		Assert.assertEquals(createdEntry2.getName(), pulledEntry2.getName());
		Assert.assertEquals(createdEntry2.getNamespace(), pulledEntry2.getNamespace());
		Assert.assertEquals(createdEntry2.getRoles(), pulledEntry2.getRoles());
		Assert.assertNotEquals(createdEntry2.getVersion(), pulledEntry2.getVersion());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		// Test Removing single entry
		definitionDetailStore.removeEntry(key2);
		Assert.assertEquals(definitionDetailStore.getNumberOfEntries(), 1);
		allEntries = definitionDetailStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = definitionDetailStore.addEntry(createdEntry2);
		Assert.assertEquals(definitionDetailStore.getNumberOfEntries(), 2);

		// Test Removing all entries
		definitionDetailStore.removeAllEntries();
		Assert.assertEquals(definitionDetailStore.getNumberOfEntries(), 0);
		allEntries = definitionDetailStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 0);
	}

	/**
	 * Test domain standard store.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testDomainStandardStore() throws Exception {
		DomainStandard createdEntry1 = new DomainStandard(WorkflowDomain.ADMISSIONS,
				createTestingElementTerminologyMap());

		// New scope to ensure closing store
		DomainStandardWorkflowContentStore domainStandardStore = new DomainStandardWorkflowContentStore(store);

		// Add new entry
		UUID key1 = domainStandardStore.addEntry(createdEntry1);
		store.close();

		// Get entry with new store
		store = new MVStoreMetaContentProvider(new File("target"), "testWorkflow", false);
		domainStandardStore = new DomainStandardWorkflowContentStore(store);
		DomainStandard pulledEntry1 = domainStandardStore.getEntry(key1);

		Assert.assertEquals(domainStandardStore.getNumberOfEntries(), 1);
		Assert.assertEquals(createdEntry1, pulledEntry1);

		// Add second entry
		DomainStandard createdEntry2 = new DomainStandard(WorkflowDomain.ADVANCE_DIRECTIVES,
				createTestingElementTerminologyMap());

		UUID key2 = domainStandardStore.addEntry(createdEntry2);
		Assert.assertEquals(domainStandardStore.getNumberOfEntries(), 2);

		// Verify entries are as expected
		DomainStandard pulledEntry2 = domainStandardStore.getEntry(key2);
		Assert.assertEquals(createdEntry2, pulledEntry2);
		Collection<DomainStandard> allEntries = domainStandardStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 2);
		Assert.assertTrue(allEntries.contains(createdEntry1));
		Assert.assertTrue(allEntries.contains(createdEntry2));

		// Test update of an entry
		DomainStandard updatedEntry2 = new DomainStandard(WorkflowDomain.ALLERGIES,
				createdEntry1.getElementTerminologyMap());
		domainStandardStore.updateEntry(key2, updatedEntry2);
		Assert.assertEquals(allEntries.size(), 2);

		pulledEntry2 = domainStandardStore.getEntry(key2);
		Assert.assertNotEquals(createdEntry2, pulledEntry2);

		Assert.assertEquals(createdEntry2.getElementTerminologyMap(), pulledEntry2.getElementTerminologyMap());
		Assert.assertNotEquals(createdEntry2.getClinicalDomain(), pulledEntry2.getClinicalDomain());

		Assert.assertEquals(updatedEntry2, pulledEntry2);

		// Test Removing single entry
		domainStandardStore.removeEntry(key2);
		Assert.assertEquals(domainStandardStore.getNumberOfEntries(), 1);
		allEntries = domainStandardStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 1);
		Assert.assertFalse(allEntries.contains(createdEntry2));
		Assert.assertTrue(allEntries.contains(createdEntry1));

		// Add second entry again
		key2 = domainStandardStore.addEntry(createdEntry2);
		Assert.assertEquals(domainStandardStore.getNumberOfEntries(), 2);

		// Test Removing all entries
		domainStandardStore.removeAllEntries();
		Assert.assertEquals(domainStandardStore.getNumberOfEntries(), 0);
		allEntries = domainStandardStore.getAllEntries();
		Assert.assertEquals(allEntries.size(), 0);
	}

	private Map<WorkflowDataElement, Set<WorkflowTerminology>> createTestingElementTerminologyMap() {
		Map<WorkflowDataElement, Set<WorkflowTerminology>> encounters = new HashMap<>();
		Set<WorkflowTerminology> encounterSet1 = new HashSet<>();
		Set<WorkflowTerminology> encounterSet2 = new HashSet<>();
		encounterSet1.add(WorkflowTerminology.CPT4);
		encounterSet2.add(WorkflowTerminology.SNOMED);
		encounters.put(WorkflowDataElement.ENCOUNTER, encounterSet1);
		encounters.put(WorkflowDataElement.ENCOUNTER_DIAGNOSIS, encounterSet2);
		return encounters;
	}
}

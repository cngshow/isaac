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
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.ProcessStatus;
import gov.vha.isaac.metacontent.workflow.contents.ProcessDetail.SubjectMatter;
import gov.vha.isaac.ochre.workflow.provider.AbstractWorkflowUtilities;

/**
 * Test the WorkflowStatusAccessor class
 * 
 * {@link WorkflowStatusAccessor}.
 * {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WorkflowStatusAccessorTest extends AbstractWorkflowProviderTestPackage {
	private static boolean setupCompleted = false;
	private static Set<Integer> specialTestingConId = new HashSet<> (Arrays.asList(1234));
	
	private static MVStoreMetaContentProvider store;
	private static WorkflowStatusAccessor accessor;

	/**
	 * Sets the up.
	 */
	@Before
	public void setUpClass() {
		if (!setupCompleted ) {
			store = new MVStoreMetaContentProvider(new File("target"), "testWorkflowStatusAccess", true);
		}

		globalSetup(store);

		if (!setupCompleted ) {
			createMainWorkflowProcess(mainDefinitionId);
			accessor = new WorkflowStatusAccessor(store);
			
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
	public void testAGetProcessesForConcept() throws Exception {
		Set<ProcessDetail> processes = new HashSet<>();
		
		for (Integer conId : conceptsForTesting) {
			Set<ProcessDetail> conceptProcesses = accessor.getProcessesForConcept(conId);
			Assert.assertEquals(1, conceptProcesses.size());
			processes.addAll(conceptProcesses);
		}
		
		for (ProcessDetail entry : processes) {
    		// verify content in workflow is as expected
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
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testBGetProcessDetail() throws Exception {
		ProcessDetail entry = accessor.getProcessDetail(mainProcessId);

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
	public void testCGetActiveProcessesByDefinition() throws Exception {
		secondaryProcessId = initConcluder.defineWorkflow(mainDefinitionId, specialTestingConId, stampSequenceForTesting,
				mainUserId, SubjectMatter.CONCEPT);
		Assert.assertEquals(1, accessor.getProcessesForConcept(specialTestingConId.iterator().next()).size());
		Assert.assertEquals(1, accessor.getProcessesForConcept(conceptsForTesting.iterator().next()).size());
		Assert.assertEquals(2, accessor.getActiveProcessesByDefinition(mainDefinitionId).size());
		
		initConcluder.concludeWorkflow(secondaryProcessId);
		
		Assert.assertEquals(1, accessor.getProcessesForConcept(specialTestingConId.iterator().next()).size());
		Assert.assertEquals(1, accessor.getProcessesForConcept(conceptsForTesting.iterator().next()).size());

		Set<ProcessDetail> processes = accessor.getActiveProcessesByDefinition(mainDefinitionId);
		Assert.assertEquals(1, processes.size());

		ProcessDetail entry = processes.iterator().next();
		
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
	public void testDDefinition() throws Exception {
		DefinitionDetail entry = accessor.getDefinition(mainDefinitionId);

		Set<String> expectedRoles = new HashSet<>();
		expectedRoles.add("Editor");
		expectedRoles.add("Reviewer");
		expectedRoles.add("Approver");
		expectedRoles.add(AbstractWorkflowUtilities.SYSTEM_AUTOMATED);

		Assert.assertEquals(entry.getBpmn2Id(), "VetzWorkflow");
		Assert.assertEquals(entry.getName(), "VetzWorkflow");
		Assert.assertEquals(entry.getNamespace(), "org.jbpm");
		Assert.assertEquals(entry.getVersion(), "1.2");
		Assert.assertEquals(entry.getRoles(), expectedRoles);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testEIsConceptInActiveWorkflow() throws Exception {
		Assert.assertTrue(accessor.isConceptInActiveWorkflow(conceptsForTesting.iterator().next()));
		Assert.assertFalse(accessor.isConceptInActiveWorkflow(specialTestingConId.iterator().next()));
	}


	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testFIsComponentInActiveWorkflow() throws Exception {
		// Cannot make this work without a full database.  Added to Integration-Test module's workflowFramworkTest
		Assert.assertTrue(true);
	}

	/**
	 * Test vetz workflow set nodes.
	 *
	 * @throws Exception
	 *             the exception
	 */
	@Test
	public void testGGetActiveProcessForConcept() throws Exception {
		ProcessDetail entry = accessor.getActiveProcessForConcept(conceptsForTesting.iterator().next());

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

}

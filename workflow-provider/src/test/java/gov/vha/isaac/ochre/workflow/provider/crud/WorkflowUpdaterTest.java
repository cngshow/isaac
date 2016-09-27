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
import java.io.IOException;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.util.RecursiveDelete;
import gov.vha.isaac.ochre.workflow.model.contents.ProcessDetail;
import gov.vha.isaac.ochre.workflow.provider.WorkflowProvider;

/**
 * Test the WorkflowUpdater class
 * 
 * {@link WorkflowUpdater}. {@link AbstractWorkflowProviderTestPackage}.
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowUpdaterTest extends AbstractWorkflowProviderTestPackage {
	private static int firstConceptNid = 0;
	private static int secondConceptNid = 0;

	/**
	 * Sets the up.
	 */
	@BeforeClass
	public static void setUpClass() {
		WorkflowProvider.BPMN_PATH = BPMN_FILE_PATH;
		LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(new File("target/store").toPath());
		LookupService.startupMetadataStore();
		globalSetup();
		for (Integer nid : conceptsForTesting) {
			if (firstConceptNid == 0) {
				firstConceptNid = nid;
			} else {
				secondConceptNid = nid;
			}
		}
	}

	@AfterClass
	public static void tearDownClass() throws IOException {
		LookupService.shutdownIsaac();
		RecursiveDelete.delete(new File("target/store"));
	}

	@Before
	public void beforeTest() {
		wp_.getProcessDetailStore().clear();
		wp_.getProcessHistoryStore().clear();
		wp_.getUserPermissionStore().clear();
		setupUserRoles();
	}

	/**
	 * Test ability to add components and stamps to the process.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	/*
	 * Note this is a simplified test as using addComponentToWorkflow. More
	 * realistic and complex test, using addCommitRecordToWorkflow, is found in
	 * WorkflowFrameworkTest as commitRecords require IdentifierService.
	 */
	public void testAddComponentsToProcess() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		ProcessDetail details = wp_.getProcessDetailStore().get(processId);
		Assert.assertFalse(details.getComponentNids().contains(firstConceptNid));

		wp_.getWorkflowUpdater().addComponentToWorkflow(details, firstConceptNid);
		details = wp_.getProcessDetailStore().get(processId);
		Assert.assertEquals(1, details.getComponentNids().size());
		Assert.assertTrue(details.getComponentNids().contains(firstConceptNid));

		wp_.getWorkflowUpdater().addComponentToWorkflow(details, secondConceptNid);
		details = wp_.getProcessDetailStore().get(processId);
		Assert.assertEquals(2, details.getComponentNids().size());
		Assert.assertTrue(details.getComponentNids().contains(firstConceptNid));
		Assert.assertTrue(details.getComponentNids().contains(secondConceptNid));
	}

	/**
	 * Test ability to add and then remove components and stamps to the process.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	/*
	 * Note this is a simplified test as using addComponentToWorkflow. More
	 * realistic and complex test, using addCommitRecordToWorkflow, is found in
	 * WorkflowFrameworkTest as commitRecords require IdentifierService.
	 */
	public void testRemoveComponentsFromProcess() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		ProcessDetail details = wp_.getProcessDetailStore().get(processId);
		Assert.assertEquals(0, details.getComponentNids().size());

		wp_.getWorkflowUpdater().addComponentToWorkflow(details, firstConceptNid);
		wp_.getWorkflowUpdater().addComponentToWorkflow(details, firstConceptNid);
		wp_.getWorkflowUpdater().addComponentToWorkflow(details, secondConceptNid);
		wp_.getWorkflowUpdater().addComponentToWorkflow(details, secondConceptNid);

		details = wp_.getProcessDetailStore().get(processId);
		Assert.assertEquals(2, details.getComponentNids().size());

		wp_.getWorkflowUpdater().removeComponentFromWorkflow(processId, firstConceptNid, null);
		details = wp_.getProcessDetailStore().get(processId);
		Assert.assertEquals(1, details.getComponentNids().size());
		Assert.assertFalse(details.getComponentNids().contains(firstConceptNid));
		Assert.assertTrue(details.getComponentNids().contains(secondConceptNid));

		wp_.getWorkflowUpdater().removeComponentFromWorkflow(processId, secondConceptNid, null);
		details = wp_.getProcessDetailStore().get(processId);
		Assert.assertEquals(0, details.getComponentNids().size());
	}

	/**
	 * Test that advancing process not only works, but only is permitted based
	 * on current state (modified while advancing) only available actions based
	 * on user permissions can advance process.
	 *
	 * @throws Exception
	 *             Thrown if test fails
	 */
	@Test
	public void testAdvanceWorkflow() throws Exception {
		UUID processId = createFirstWorkflowProcess(mainDefinitionId);
		addComponentsToProcess(processId);
		executeLaunchWorkflow(processId);

		// Process in Ready to Edit state: Can execute action "Edit" by
		// firstUser
		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Approve", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Approve", "Comment #1"));

		Assert.assertTrue(advanceWorkflow(processId, firstUserId, "Edit", "Comment #1"));

		// Process in Ready for Review state: Can execute action "QA Passes" by
		// secondUser
		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Approve", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Approve", "Comment #1"));

		Assert.assertTrue(advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1"));

		// Process in Ready for Approve state: Can execute action "Approve" by
		// firstUser
		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Approve", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1"));

		Assert.assertTrue(advanceWorkflow(processId, firstUserId, "Approve", "Comment #1"));

		// Process in Publish state: no one can advance
		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "Approve", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, secondUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Edit", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "QA Passes", "Comment #1"));

		Assert.assertFalse(advanceWorkflow(processId, firstUserId, "Approve", "Comment #1"));
	}

}

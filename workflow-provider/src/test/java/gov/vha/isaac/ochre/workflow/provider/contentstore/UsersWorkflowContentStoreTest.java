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
package gov.vha.isaac.ochre.workflow.provider.contentstore;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.UsersProcessAdvancementContentStore;
import gov.vha.isaac.metacontent.workflow.UsersProcessCreationContentStore;
import gov.vha.isaac.metacontent.workflow.WorkflowAdvancement;
import gov.vha.isaac.metacontent.workflow.WorkflowProcess;
import gov.vha.isaac.ochre.api.metacontent.MetaContentService.WorkflowContentTypes;

/**
 * Test both static and user based workflow content as defined in the
 * metacontent-store
 *
 * {@link UsersWorkflowContentStoreTest}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class UsersWorkflowContentStoreTest {

	private MVStoreMetaContentProvider store;

	@Before
	public void setUp() {
		store = new MVStoreMetaContentProvider(new File("target"), "test", true);
	}

	@After
	public void tearDown() {
		store.close();
	}

	@Test
	public void testUsersProcessCreationStore() throws Exception {
		// Create Initial Content
		Set<UUID> components = new HashSet<>();
		components.add(UUID.randomUUID());
		components.add(UUID.randomUUID());
		WorkflowProcess p = new WorkflowProcess(1, 2, components, 3, new Date().getTime());
		Set<WorkflowProcess> processes = new HashSet<>();
		processes.add(p);

		UsersProcessCreationContentStore createdProcessContentStore = new UsersProcessCreationContentStore(processes);

		// Write content into database
		store.putWorkflowContent(WorkflowContentTypes.PROCESS_CREATION, createdProcessContentStore);

		// Read from DB and confirm content is same as created
		UsersProcessCreationContentStore pulledProcesses = new UsersProcessCreationContentStore(
				store.getWorkflowContent(WorkflowContentTypes.PROCESS_CREATION));

		Assert.assertTrue(pulledProcesses.equals(createdProcessContentStore));
		store.close();

		// Reopen database and confirm content is still same as created
		store = new MVStoreMetaContentProvider(new File("target"), "test", false);

		pulledProcesses = new UsersProcessCreationContentStore(
				store.getWorkflowContent(WorkflowContentTypes.PROCESS_CREATION));

		Assert.assertTrue(pulledProcesses.equals(createdProcessContentStore));
		store.close();

		// Add another process without committing it to verify not added to
		// database
		components.clear();
		components.add(UUID.randomUUID());
		components.add(UUID.randomUUID());
		WorkflowProcess p2 = new WorkflowProcess(2, 4, components, 3, new Date().getTime());
		processes.add(p2);

		store = new MVStoreMetaContentProvider(new File("target"), "test", false);
		pulledProcesses = new UsersProcessCreationContentStore(
				store.getWorkflowContent(WorkflowContentTypes.PROCESS_CREATION));

		Assert.assertFalse(pulledProcesses.equals(createdProcessContentStore));

		// Write content into database
		createdProcessContentStore.getWorkflowProcesses().add(p2);
		store.putWorkflowContent(WorkflowContentTypes.PROCESS_CREATION, createdProcessContentStore);

		// Read from DB and confirm content is same as created
		store.removeWorkflowContent(WorkflowContentTypes.PROCESS_CREATION);
		store.close();

		// Ensure content empty
		store = new MVStoreMetaContentProvider(new File("target"), "test", false);
		byte[] content = store.getWorkflowContent(WorkflowContentTypes.PROCESS_CREATION);
		Assert.assertNull(content);
		store.close();
	}

	@Test
	public void testUsersProcessAdvancementStore() throws Exception {
		// Create Initial Content
		WorkflowAdvancement adv = new WorkflowAdvancement(1, 3, new Date().getTime(), "REQUEST", "Edit Content",
				"Ready for Review", "Do not need description");
		Set<WorkflowAdvancement> advancements = new HashSet<>();
		advancements.add(adv);

		UsersProcessAdvancementContentStore createdProcessAdvancementStore = new UsersProcessAdvancementContentStore(
				advancements);

		// Write content into database
		store.putWorkflowContent(WorkflowContentTypes.ADVANCEMENT, createdProcessAdvancementStore);

		// Read from DB and confirm content is same as created
		UsersProcessAdvancementContentStore pulledAdvancements = new UsersProcessAdvancementContentStore(
				store.getWorkflowContent(WorkflowContentTypes.ADVANCEMENT));

		System.out.println(
				" \n\n\n\n\n **** PULLED ADVANCEMENT ***** \n" + pulledAdvancements + "********* END ********");
		System.out.println(" \n\n\n\n\n **** Created Process Advancement Store ***** \n"
				+ createdProcessAdvancementStore + "********* END ********");
		boolean b = pulledAdvancements.equals(createdProcessAdvancementStore);
		System.out.println("Boolean: " + b);
		System.out.println("HERE#####0");
		Assert.assertTrue(b);
		Assert.assertTrue(pulledAdvancements.equals(createdProcessAdvancementStore));
		store.close();

		// Reopen database and confirm content is still same as created
		store = new MVStoreMetaContentProvider(new File("target"), "test", false);

		pulledAdvancements = new UsersProcessAdvancementContentStore(
				store.getWorkflowContent(WorkflowContentTypes.ADVANCEMENT));

		Assert.assertTrue(pulledAdvancements.equals(createdProcessAdvancementStore));
		store.close();

		// Add another process without committing it to verify not added to
		// database
		WorkflowAdvancement adv2 = new WorkflowAdvancement(1, 3, new Date().getTime(), "Ready for Review", "QA Passes",
				"Ready for Approval", "");
		advancements.add(adv2);

		store = new MVStoreMetaContentProvider(new File("target"), "test", false);
		pulledAdvancements = new UsersProcessAdvancementContentStore(
				store.getWorkflowContent(WorkflowContentTypes.ADVANCEMENT));
		Assert.assertFalse(pulledAdvancements.equals(createdProcessAdvancementStore));

		// Write content into database
		createdProcessAdvancementStore.getAdvancements().add(adv2);
		store.putWorkflowContent(WorkflowContentTypes.ADVANCEMENT, createdProcessAdvancementStore);

		// Read from DB and confirm content is same as created
		store.removeWorkflowContent(WorkflowContentTypes.ADVANCEMENT);
		store.close();

		// Ensure content empty
		store = new MVStoreMetaContentProvider(new File("target"), "test", false);
		byte[] content = store.getWorkflowContent(WorkflowContentTypes.ADVANCEMENT);
		Assert.assertNull(content);
		store.close();
	}
}

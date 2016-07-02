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
package gov.vha.isaac.ochre.workflow.provider;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jbpm.bpmn2.core.SequenceFlow;
import org.jbpm.bpmn2.xml.ProcessHandler;
import org.jbpm.kie.services.impl.bpmn2.ProcessDescriptor;
import org.jbpm.kie.services.impl.model.ProcessAssetDesc;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.HumanTaskNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;
import org.jbpm.workflow.core.node.StartNode;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.definition.process.Connection;
import org.kie.api.definition.process.Node;

/**
 * {@link WorkflowDefinitionUtilityTest}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowDefinitionUtilityTest {
	private final String BPMN_FILE_PATH = "src/test/resources/gov/vha/isaac/ochre/workflow/provider/VetzWorkflow.bpmn2";
	private Map<Long, List<Long>> nodeToOutgoingMap;
	private List<SequenceFlow> connections;
 
	@Test
	public void testProcessDefinition() throws Exception {
		WorkflowDefinitionUtility util = new WorkflowDefinitionUtility();
		util.setDefinition(BPMN_FILE_PATH);
		
		ProcessAssetDesc definition = util.getProcessDefinition();
		Assert.assertEquals(definition.getName(), "VetzWorkflow");

		/* Process Definition */
		System.out.println("\t\t ***** Definition Processing *****");
		analyzeDefinition(definition, util.getProcessDescriptor());
	}

	@Test
	public void testProcessNodes() throws Exception {
		WorkflowDefinitionUtility util = new WorkflowDefinitionUtility();
		util.setNodes(BPMN_FILE_PATH);

		List<Node> nodes = util.getProcessNodes();
		nodeToOutgoingMap = util.getNodesToOutgoingMap();
		connections = (List<SequenceFlow>) util.getProcess().getMetaData(ProcessHandler.CONNECTIONS);

		Assert.assertEquals(nodes.size(), 14);

		/* Process Nodes */
		System.out.println("\n\n\n\n\t\t ***** Node Processing *****");
		// Print out remaining nodes
		for (Node node : nodes) {
			analyzeNodes(node);
		}
	}

	private void analyzeDefinition(ProcessAssetDesc definition, ProcessDescriptor processDescriptor) {
		// TODO Auto-generated method stub
		System.out.println("Definition Name: " + definition.getName());
		System.out.println("Definition Namespace: " + definition.getPackageName());
		System.out.println("Definition Id: " + definition.getId());
		System.out.println("Definition DeploymentId: " + definition.getDeploymentId());
		System.out.println("Definition Knowledge Type: " + definition.getKnowledgeType());
		System.out.println("Definition Type: " + definition.getType());
		System.out.println("Definition Version: " + definition.getVersion());

		printOutHelperContents(processDescriptor);
	}

	private void printOutHelperContents(ProcessDescriptor processDescriptor) {
		System.out.println("*****Printing out Global Item Definitions Map<String, String>*****");
		Map<String, String> globalItems = processDescriptor.getGlobalItemDefinitions();
		for (String key : globalItems.keySet()) {
			System.out.println("Key: " + key + " with values: " + globalItems.get(key));
		}

		System.out.println("\n\n\n\n*****Printing out Task AssignmentsMap<String, Collection<String>> *****");
		Map<String, Collection<String>> taskAssignments = processDescriptor.getTaskAssignments();
		for (String key : taskAssignments.keySet()) {
			System.out.println("\nKey: " + key + " with values:");
			for (String colValue : taskAssignments.get(key)) {
				System.out.println("Value: " + colValue);
			}
		}

		System.out.println("\n\n\n\n*****Printing out Task Input Mappings Map<String, Map<String, String>>*****");
		Map<String, Map<String, String>> taskInputMappings = processDescriptor.getTaskInputMappings();
		for (String key : taskInputMappings.keySet()) {
			System.out.println("\nKey: " + key + " with sub-key/value:");
			for (String key2 : taskInputMappings.get(key).keySet()) {
				String val = taskInputMappings.get(key).get(key2);
				System.out.println("\tKey2: " + key2 + " with value: " + val);
			}
		}

		System.out.println("\n\n\n\n*****Printing out Task Output Mappings Map<String, Map<String, String>>*****");
		Map<String, Map<String, String>> taskOutputMappings = processDescriptor.getTaskOutputMappings();
		for (String key : taskOutputMappings.keySet()) {
			System.out.println("\nKey: " + key + " with sub-key/value:");
			for (String key2 : taskOutputMappings.get(key).keySet()) {
				String val = taskOutputMappings.get(key).get(key2);
				System.out.println("\tKey2: " + key2 + " with value: " + val);
			}
		}
	}

	private void analyzeNodes(Node node) {
		if (node.getName() == null || node.getName().isEmpty()) {
			System.out.println("\n\n\n**** Printing out unnamed node");
		} else {
			System.out.println("\n\n\n**** Printing out node named: " + node.getName());
		}

		System.out.println("ID: " + node.getId());

		if (node instanceof StartNode) {
			System.out.println("Type: StartNode");
		} else if (node instanceof EndNode) {
			System.out.println("Type: EndNode");
		} else if (node instanceof HumanTaskNode) {
			System.out.println("Type: HumanTaskNode");
		} else if (node instanceof Join) {
			System.out.println("Type: Join");
		} else if (node instanceof Split) {
			System.out.println("Type: Split");
		}

		if (!nodeToOutgoingMap.get(node.getId()).isEmpty()) {
			System.out.println("This node has the following outgoing connections:");

			for (Long id : nodeToOutgoingMap.get(node.getId())) {
				if (node instanceof Split) {
					System.out.println(
							"\t" + id + " that is associated to action: " + identifySplitOptions((Split) node, id));
				} else {
					System.out.println("\t" + id);
				}
			}
		}
	}

	private String identifySplitOptions(Split split, long outgoingId) {

		for (Connection connection : split.getDefaultOutgoingConnections()) {
			if (connection.getTo().getId() == outgoingId) {
				String connectionId = (String) connection.getMetaData().get("UniqueId");

				for (SequenceFlow sequence : connections) {
					if (sequence.getId().equals(connectionId)) {
						return sequence.getName();
					}
				}
			}
		}

		System.out.println("Couldn't find the expected Constraint for Split: " + split.getId() + " named "
				+ split.getName() + " for outgoingId: " + outgoingId);

		return "";
	}
}
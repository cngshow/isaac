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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.drools.core.io.impl.ByteArrayResource;
import org.drools.core.xml.SemanticModules;
import org.jbpm.bpmn2.core.SequenceFlow;
import org.jbpm.bpmn2.xml.BPMNDISemanticModule;
import org.jbpm.bpmn2.xml.BPMNSemanticModule;
import org.jbpm.bpmn2.xml.ProcessHandler;
import org.jbpm.compiler.xml.XmlProcessReader;
import org.jbpm.kie.services.impl.bpmn2.BPMN2DataServiceImpl;
import org.jbpm.kie.services.impl.bpmn2.ProcessDescriptor;
import org.jbpm.kie.services.impl.model.ProcessAssetDesc;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.services.api.DefinitionService;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.HumanTaskNode;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;
import org.jbpm.workflow.core.node.StartNode;
import org.kie.api.definition.process.Connection;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.Process;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.definition.KnowledgePackage;
import org.xml.sax.SAXException;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.AvailableActionWorkflowContentStore;
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;

/**
 * Routines enabling access of content built when importing a bpmn2 file
 * 
 * {@link WorkflowDefinitionUtility}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowDefinitionUtility {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The bpmn2 service. */
	protected DefinitionService bpmn2Service = new BPMN2DataServiceImpl();

	/** The process descriptor. */
	private ProcessDescriptor processDescriptor = null;

	/** The process definition. */
	private ProcessAssetDesc processDefinition = null;

	/** The process nodes. */
	// Handling of Nodes
	private List<Node> processNodes = new ArrayList<Node>();

	/** The visited nodes. */
	private List<Long> visitedNodes = new ArrayList<>();

	/** The node to outgoing map. */
	private Map<Long, List<Long>> nodeToOutgoingMap = new HashMap<Long, List<Long>>();

	/** The print for analysis. */
	boolean printForAnalysis = false;

	/** The process. */
	private RuleFlowProcess process;

	/** The store. */
	MVStoreMetaContentProvider store = null;

	/**
	 * Instantiates a new workflow definition utility.
	 *
	 * @param store
	 *            the store
	 */
	public WorkflowDefinitionUtility(MVStoreMetaContentProvider store) {
		this.store = store;
	}

	/**
	 * Instantiates a new workflow definition utility.
	 *
	 * @param store
	 *            the store
	 * @param bpmn2FilePath
	 *            the bpmn2 file path
	 */
	public WorkflowDefinitionUtility(MVStoreMetaContentProvider store, String bpmn2FilePath) {
		this.store = store;
		setDefinition(bpmn2FilePath);
		setNodes(bpmn2FilePath);
	}

	/**
	 * Sets the definition.
	 *
	 * @param bpmn2FilePath
	 *            the new definition
	 */
	public void setDefinition(String bpmn2FilePath) {
		String xmlContents;
		try {
			xmlContents = readFile(bpmn2FilePath, Charset.defaultCharset());
			processWorkflowDefinition("test", xmlContents);

			if (printForAnalysis) {
				printProcessDefinition();
			}
		} catch (IOException e) {
			logger.error("Failed in processing the workflow definition defined at: " + bpmn2FilePath);
			e.printStackTrace();
		}
	}

	/**
	 * Sets the nodes.
	 *
	 * @param bpmn2FilePath
	 *            the new nodes
	 */
	public void setNodes(String bpmn2FilePath) {
		processNodes.clear();
		visitedNodes.clear();
		nodeToOutgoingMap.clear();

		process = buildProcessFromFile(bpmn2FilePath);
		List<Long> nodesInOrder = identifyOutputOrder(process.getStartNodes().iterator().next(), new ArrayList<Long>());

		// Populate the actual nodes object
		for (Long nodeId : nodesInOrder) {
			processNodes.add(process.getNode(nodeId));
		}

		populateActionOutcomesRecords(bpmn2FilePath);

		if (printForAnalysis) {
			printNodes();
		}
	}

	/**
	 * Populate action outcomes records.
	 *
	 * @param bpmn2FilePath
	 *            the bpmn2 file path
	 */
	private void populateActionOutcomesRecords(String bpmn2FilePath) {
		List<SequenceFlow> connections = (List<SequenceFlow>) process.getMetaData(ProcessHandler.CONNECTIONS);

		try {
			Set<AvailableAction> entries = generateAvaialbleActions(processNodes, nodeToOutgoingMap, connections);
			AvailableActionWorkflowContentStore createdStateActionCdontent = new AvailableActionWorkflowContentStore(
					store);

			for (AvailableAction entry : entries) {
				// Write content into database
				createdStateActionCdontent.addEntry(entry);
			}
		} catch (Exception e) {
			logger.error("Failed in transforming the workflow definition into Possible Actions: " + bpmn2FilePath);
			e.printStackTrace();
		}
	}

	/**
	 * Generate avaialble actions.
	 *
	 * @param nodes
	 *            the nodes
	 * @param nodeToOutgoingMap2
	 *            the node to outgoing map2
	 * @param connections
	 *            the connections
	 * @return the sets the
	 * @throws Exception
	 *             the exception
	 */
	private Set<AvailableAction> generateAvaialbleActions(List<Node> nodes, Map<Long, List<Long>> nodeToOutgoingMap2,
			List<SequenceFlow> connections) throws Exception {
		Set<AvailableAction> actions = new HashSet<>();

		for (Node node : nodes) {
			if (node instanceof Split) {
				for (Long id : nodeToOutgoingMap.get(node.getId())) {
					String state = node.getName();

					// ** JOEL TO UPDATE role (replacing 'null') ***
					String role = null;
					role = "SME";
					String action = null;
					String outcome = null;

					for (Connection connection : ((Split) node).getDefaultOutgoingConnections()) {
						if (connection.getTo().getId() == id) {
							String connectionId = (String) connection.getMetaData().get("UniqueId");

							for (SequenceFlow sequence : connections) {
								if (sequence.getId().equals(connectionId)) {
									action = sequence.getName();
									outcome = sequence.getExpression().replaceAll("'", "");
									break;
								}
							}
						}

						if (action != null) {
							break;
						}
					}

					if (action == null || outcome == null) {
						throw new Exception("BPMN2 file missing key requirements");
					}

					actions.add(new AvailableAction(state, action, outcome, role));
				}
			}
		}

		return actions;
	}

	/**
	 * Process workflow definition.
	 *
	 * @param string
	 *            the string
	 * @param xmlContents
	 *            the xml contents
	 */
	private void processWorkflowDefinition(String string, String xmlContents) {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(new ByteArrayResource(xmlContents.getBytes()), ResourceType.BPMN2);
		KnowledgePackage pckg = kbuilder.getKnowledgePackages().iterator().next();

		Process process = pckg.getProcesses().iterator().next();

		processDescriptor = (ProcessDescriptor) process.getMetaData().get("ProcessDescriptor");
		processDefinition = processDescriptor.getProcess();

		processDefinition.setAssociatedEntities(processDescriptor.getTaskAssignments());
		processDefinition.setProcessVariables(processDescriptor.getInputs());
		processDefinition.setServiceTasks(processDescriptor.getServiceTasks());

		processDefinition.setAssociatedEntities(processDescriptor.getTaskAssignments());
		processDefinition.setProcessVariables(processDescriptor.getInputs());
		processDefinition.setServiceTasks(processDescriptor.getServiceTasks());

		processDefinition.setReusableSubProcesses(processDescriptor.getReusableSubProcesses());

	}

	/**
	 * Builds the process from file.
	 *
	 * @param bpmn2FilePath
	 *            the bpmn2 file path
	 * @return the rule flow process
	 */
	private RuleFlowProcess buildProcessFromFile(String bpmn2FilePath) {
		SemanticModules modules = new SemanticModules();
		modules.addSemanticModule(new BPMNSemanticModule());
		modules.addSemanticModule(new BPMNDISemanticModule());
		XmlProcessReader processReader = new XmlProcessReader(modules, getClass().getClassLoader());
		try {
			InputStream in = new FileInputStream(new File(bpmn2FilePath));
			List<Process> processes = processReader.read(in);
			in.close();
			return (RuleFlowProcess) processes.get(0);
		} catch (FileNotFoundException e) {
			logger.error("Couldn't Find Fine: " + bpmn2FilePath, e);
			e.printStackTrace();
		} catch (IOException ioe) {
			logger.error("Error in readFile method: " + bpmn2FilePath, ioe);
			ioe.printStackTrace();
		} catch (SAXException se) {
			logger.error("Error in parsing XML file: " + bpmn2FilePath, se);
			se.printStackTrace();
		}

		return null;
	}

	/**
	 * Identify output order.
	 *
	 * @param node
	 *            the node
	 * @param retList
	 *            the ret list
	 * @return the list
	 */
	private List<Long> identifyOutputOrder(Node node, List<Long> retList) {
		if (visitedNodes.contains(node.getId())) {
			return retList;
		} else {
			visitedNodes.add(node.getId());
			retList.add(node.getId());
			List<Long> outgoingNodeIds = new ArrayList<Long>();

			for (Node n : getOutgoingNodes(node)) {
				outgoingNodeIds.add(n.getId());
				retList = identifyOutputOrder(n, retList);
			}

			nodeToOutgoingMap.put(node.getId(), outgoingNodeIds);

			return retList;
		}
	}

	/**
	 * Read file.
	 *
	 * @param path
	 *            the path
	 * @param encoding
	 *            the encoding
	 * @return the string
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	/**
	 * Gets the outgoing nodes.
	 *
	 * @param node
	 *            the node
	 * @return the outgoing nodes
	 */
	private List<Node> getOutgoingNodes(Node node) {
		List<Node> retList = new ArrayList<Node>();

		for (final Iterator<List<Connection>> it = node.getOutgoingConnections().values().iterator(); it.hasNext();) {
			final List<Connection> list = it.next();
			for (final Iterator<Connection> it2 = list.iterator(); it2.hasNext();) {
				retList.add(it2.next().getTo());
			}
		}

		return retList;
	}

	/**
	 * Prints the process definition.
	 */
	private void printProcessDefinition() {
		System.out.println("\t\t ***** Definition Processing *****");
		System.out.println("Definition Name: " + processDefinition.getName());
		System.out.println("Definition Namespace: " + processDefinition.getPackageName());
		System.out.println("Definition Id: " + processDefinition.getId());
		System.out.println("Definition DeploymentId: " + processDefinition.getDeploymentId());
		System.out.println("Definition Knowledge Type: " + processDefinition.getKnowledgeType());
		System.out.println("Definition Type: " + processDefinition.getType());
		System.out.println("Definition Version: " + processDefinition.getVersion());

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

	/**
	 * Prints the nodes.
	 */
	private void printNodes() {
		/* Process Nodes */
		System.out.println("\n\n\n\n\t\t ***** Node Processing *****");
		List<SequenceFlow> connections = (List<SequenceFlow>) process.getMetaData(ProcessHandler.CONNECTIONS);

		// Print out remaining nodes
		for (Node node : processNodes) {
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
					String splitOption = "NOT FOUND";
					for (Connection connection : ((Split) node).getDefaultOutgoingConnections()) {
						if (connection.getTo().getId() == id) {
							String connectionId = (String) connection.getMetaData().get("UniqueId");

							for (SequenceFlow sequence : connections) {
								if (sequence.getId().equals(connectionId)) {
									splitOption = sequence.getName();
								}
							}
						}
					}

					if (node instanceof Split) {
						System.out.println("\t" + id + " that is associated to action: " + splitOption);
					} else {
						System.out.println("\t" + id);
					}
				}
			}
		}
	}
}

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
import java.util.UUID;

import org.drools.core.io.impl.ByteArrayResource;
import org.drools.core.process.core.Work;
import org.drools.core.xml.SemanticModules;
import org.hamcrest.core.IsInstanceOf;
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
import gov.vha.isaac.metacontent.workflow.contents.AvailableAction;
import gov.vha.isaac.metacontent.workflow.contents.DefinitionDetail;

/**
 * Routines enabling access of content built when importing a bpmn2 file
 * 
 * {@link AbstractWorkflowUtilities} {@link Bpmn2FileImporter}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class Bpmn2FileImporter extends AbstractWorkflowUtilities {

	/** The bpmn2 service. */
	protected DefinitionService bpmn2Service = new BPMN2DataServiceImpl();

	/** The process nodes. */
	// Handling of Nodes
	private List<Node> processNodes = new ArrayList<Node>();

	/** The visited nodes. */
	private List<Long> visitedNodes = new ArrayList<>();

	/** The node to outgoing map. */
	private Map<Long, List<Long>> nodeToOutgoingMap = new HashMap<Long, List<Long>>();

	/** The node to outgoing map. */
	private Map<Long, String> nodeNameMap = new HashMap<Long, String>();

	/** The print for analysis. */
	boolean printForAnalysis = false;

	/** The process. */
	private RuleFlowProcess process;

	private Set<Long> humanNodesProcessed = new HashSet<>();

	public Bpmn2FileImporter() throws Exception {
		// Default Constructor fails if store not already set
	}

	/**
	 * Instantiates a new workflow definition utility.
	 *
	 * @param store
	 *            the store
	 */
	public Bpmn2FileImporter(MVStoreMetaContentProvider store) {
		super(store);
	}

	/**
	 * Instantiates a new workflow definition utility.
	 *
	 * @param store
	 *            the store
	 * @param bpmn2FilePath
	 *            the bpmn2 file path
	 */
	public Bpmn2FileImporter(MVStoreMetaContentProvider store, String bpmn2FilePath) {
		super(store);

		UUID key = setDefinition(bpmn2FilePath);
		setNodes(bpmn2FilePath, key);
	}

	/**
	 * Sets the definition.
	 *
	 * @param bpmn2FilePath
	 *            the new definition
	 * @return the uuid
	 */
	public UUID setDefinition(String bpmn2FilePath) {
		String xmlContents;
		UUID key = null;

		try {
			xmlContents = readFile(bpmn2FilePath, Charset.defaultCharset());
			ProcessDescriptor descriptor = processWorkflowDefinition(xmlContents);

			key = populateWorkflowDefinitionRecords(descriptor);

			if (printForAnalysis) {
				printProcessDefinition(descriptor);
			}
		} catch (IOException e) {
			logger.error("Failed in processing the workflow definition defined at: " + bpmn2FilePath);
			e.printStackTrace();
		}

		return key;
	}

	/**
	 * Populate workflow definition records.
	 *
	 * @param descriptor
	 *            the descriptor
	 * @return the uuid
	 */
	private UUID populateWorkflowDefinitionRecords(ProcessDescriptor descriptor) {
		Set<String> roles = new HashSet<>();

		ProcessAssetDesc definition = descriptor.getProcess();

		for (String key : descriptor.getTaskAssignments().keySet()) {
			roles.addAll(descriptor.getTaskAssignments().get(key));
		}

		DefinitionDetail entry = new DefinitionDetail(definition.getId(), definition.getName(),
				definition.getNamespace(), definition.getVersion(), roles);

		return definitionDetailStore.addEntry(entry);
	}

	/**
	 * Sets the nodes.
	 *
	 * @param bpmn2FilePath
	 *            the new nodes
	 * @param definitionId
	 *            the definition id
	 */
	public void setNodes(String bpmn2FilePath, UUID definitionId) {
		processNodes.clear();
		visitedNodes.clear();
		nodeToOutgoingMap.clear();
		nodeNameMap.clear();
		humanNodesProcessed.clear();

		process = buildProcessFromFile(bpmn2FilePath);
		List<Long> nodesInOrder = identifyOutputOrder(process.getStartNodes().iterator().next(), new ArrayList<Long>());

		// Populate the actual nodes object
		for (Long nodeId : nodesInOrder) {
			processNodes.add(process.getNode(nodeId));
		}

		populateAvailableActionRecords(bpmn2FilePath, definitionId);

		if (printForAnalysis) {
			printNodes();
		}
	}

	/**
	 * Populate action outcomes records.
	 *
	 * @param bpmn2FilePath
	 *            the bpmn2 file path
	 * @param definitionId
	 *            the definition id
	 */
	private void populateAvailableActionRecords(String bpmn2FilePath, UUID definitionId) {
		List<SequenceFlow> connections = (List<SequenceFlow>) process.getMetaData(ProcessHandler.CONNECTIONS);

		try {
			Set<AvailableAction> entries = generateAvailableActions(definitionId,
					connections);

			for (AvailableAction entry : entries) {
				// Write content into database
				availableActionStore.addEntry(entry);
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
	 * @param nodeNameMap 
	 * @param definitionId
	 *            the definition id
	 * @param connections
	 *            the connections
	 * @return the sets the
	 * @throws Exception
	 *             the exception
	 */
	private Set<AvailableAction> generateAvailableActions(UUID definitionId, List<SequenceFlow> connections) throws Exception {
		Set<AvailableAction> actions = new HashSet<>();
		String currentState = null;
		Set<String> roles = new HashSet<>();
		
		for (Node node : processNodes) {
			if (node.getName() != null && !node.getName().isEmpty() && !(node instanceof HumanTaskNode) && !(node instanceof EndNode)) {
				currentState = node.getName();
			}
			
			if (node instanceof StartNode) {
				Set<AvailableAction> availActions = identifyNodeActions(node, connections, definitionId, currentState, roles,
						((StartNode) node).getDefaultOutgoingConnections());
				actions.addAll(availActions);
			} else if (node instanceof Split) {
				Set<AvailableAction> availActions = identifyNodeActions(node, connections, definitionId, currentState, roles,
						((Split) node).getDefaultOutgoingConnections());
				actions.addAll(availActions);
			} else if (node instanceof HumanTaskNode) {
				roles = getActorFromHumanTask((HumanTaskNode)node);
				if (!humanNodesProcessed.contains(node.getId())) {
    				Set<AvailableAction> availActions = identifyNodeActions(node, connections, definitionId, currentState, roles,
    						((HumanTaskNode) node).getDefaultOutgoingConnections());
    				actions.addAll(availActions);
    				humanNodesProcessed.add(node.getId());
				}
			}
		}

		return actions;
	}

	private Set<AvailableAction> identifyNodeActions(Node node, List<SequenceFlow> connections, UUID definitionId,
			String currentState, Set<String> roles, List<Connection> defaultOutgoingConnections) throws Exception {
		Set<AvailableAction> availActions = new HashSet<>();

		for (Long id : nodeToOutgoingMap.get(node.getId())) {
			String action = null;
			String outcome = null;
			String state = currentState;

			for (Connection connection : defaultOutgoingConnections) {
				if (connection.getTo().getId() == id) {
					String connectionId = (String) connection.getMetaData().get("UniqueId");

					for (SequenceFlow sequence : connections) {
						if (sequence.getId().equals(connectionId)) {
							action = sequence.getName();
							if (!(connection.getTo() instanceof HumanTaskNode)) {
								outcome = nodeNameMap.get(id);
							} else {
								outcome = ((HumanTaskNode)connection.getTo()).getDefaultOutgoingConnections().iterator().next().getTo().getName();
								humanNodesProcessed.add(connection.getTo().getId());
							}
							break;
						}
					}
					
		 		}

				if (outcome != null && action == null) {
					if (node instanceof HumanTaskNode) {
						action = getHumanTaskName((HumanTaskNode)node);
    				}
				}
				
				if (outcome != null || action != null) {
					break;
				} 
			}

			if (roles.size() == 0 && node.getId() == process.getStartNodes().iterator().next().getId()) {
				roles.add("SYSTEM_AUTOMATED");
			}

			// Verify that all requirements met
			if (action != null && outcome != null && state != null && roles.size() > 0) {
    
    			// Generate a new AvailableAction for each role
    			for (String role : roles) {
    				AvailableAction newAction = new AvailableAction(definitionId, state, action, outcome, role);
    				availActions.add(newAction);
    			}
    		}
		}

		return availActions;
	}

	/**
	 * Process workflow definition.
	 * 
	 * @param xmlContents
	 *            the xml contents
	 *
	 * @return the process descriptor
	 */
	private ProcessDescriptor processWorkflowDefinition(String xmlContents) {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(new ByteArrayResource(xmlContents.getBytes()), ResourceType.BPMN2);
		KnowledgePackage pckg = kbuilder.getKnowledgePackages().iterator().next();

		Process process = pckg.getProcesses().iterator().next();

		return (ProcessDescriptor) process.getMetaData().get("ProcessDescriptor");
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
			if (node instanceof HumanTaskNode) {
				nodeNameMap.put(node.getId(), getHumanTaskName((HumanTaskNode)node));
			} else {
				nodeNameMap.put(node.getId(), node.getName());
			}
			
			return retList;
		}
	}

	/**
	 * @param node
	 *            any node
	 * @return Set of role strings
	 * 
	 *         IFF passed Node is a HumanTask then simply return the HumanTask
	 *         ActorId(s) from parameters, ELSE recursively apply to nodes that
	 *         precede this one until a HumanTask is found and its role(s)
	 *         returned
	 * 
	 *         This will return empty set for nodes preceded only by Start event
	 *         (i.e. no preceding HumanTask)
	 */
	private static Set<String> getActorFromHumanTask(HumanTaskNode node) {
		Set<String> restrictions = new HashSet<>();

		// Get HumanTaskNode's restrictions
		Work work = node.getWork();

		if (work.getParameters() != null) {
			String roleString = (String) work.getParameters().get("ActorId");

			if (roleString != null) {
				String[] roles = roleString.split(",");
				for (String role : roles) {
					restrictions.add(role.trim());
				}
			}
		}

		return restrictions;
	}

	private static String getHumanTaskName(HumanTaskNode node) {
		Work work = node.getWork();

		if (work.getParameters() != null) {
			return (String) work.getParameters().get("TaskName");
		}

		return null;
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
	 *
	 * @param processDescriptor
	 *            the process descriptor
	 */
	private void printProcessDefinition(ProcessDescriptor processDescriptor) {
		ProcessAssetDesc processDefinition = processDescriptor.getProcess();

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

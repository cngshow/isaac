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
import org.jbpm.workflow.core.node.Split;
import org.kie.api.definition.process.Connection;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.Process;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.definition.KnowledgePackage;
import org.xml.sax.SAXException;

import gov.vha.isaac.metacontent.MVStoreMetaContentProvider;
import gov.vha.isaac.metacontent.workflow.PossibleAction;
import gov.vha.isaac.metacontent.workflow.StaticStateActionContentStore;
import gov.vha.isaac.ochre.api.metacontent.MetaContentService.StaticWorkflowContentTypes;

/**
 * Routines enabling access of workflow definition.
 * 
 * {@link WorkflowDefinitionUtility}
 *
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 */
public class WorkflowDefinitionUtility {
	private static final Logger LOG = LogManager.getLogger();

	protected DefinitionService bpmn2Service = new BPMN2DataServiceImpl();

	private ProcessDescriptor processDescriptor = null;
	private ProcessAssetDesc processDefinition = null;
	private List<Node> processNodes = new ArrayList<Node>();

	private List<Long> visitedNodes = new ArrayList<>();
	private Map<Long, List<Long>> nodeToOutgoingMap = new HashMap<Long, List<Long>>();

	private RuleFlowProcess process;
	MVStoreMetaContentProvider store = new MVStoreMetaContentProvider(new File("target"), "test", true);

	public WorkflowDefinitionUtility() {

	}

	public WorkflowDefinitionUtility(String bpmn2FilePath) {
		setDefinition(bpmn2FilePath);
		setNodes(bpmn2FilePath);
	}

	public void setDefinition(String bpmn2FilePath) {
		String xmlContents;
		try {
			xmlContents = readFile(bpmn2FilePath, Charset.defaultCharset());
			processDefinition("test", xmlContents, null, true);
		} catch (IOException e) {
			LOG.error("Failed in processing the workflow definition defined at: " + bpmn2FilePath);
			e.printStackTrace();
		}
	}

	public void setNodes(String bpmn2FilePath) {
		processNodes(bpmn2FilePath);

		populateActionOutcomesRecords(bpmn2FilePath);

		store.close();
	}

	private void populateActionOutcomesRecords(String bpmn2FilePath) {
		Set<PossibleAction> actions = new HashSet<>();
		List<Node> nodes = getProcessNodes();
		Map<Long, List<Long>> nodeToOutgoingMap = getNodesToOutgoingMap();

		List<SequenceFlow> connections = (List<SequenceFlow>) getProcess().getMetaData(ProcessHandler.CONNECTIONS);

		try {
			transformNodesToPossibleActions(nodes, nodeToOutgoingMap, connections, actions);

			StaticStateActionContentStore createdStateActionContent = new StaticStateActionContentStore(actions);

			// Write content into database
			store.putStaticWorkflowContent(StaticWorkflowContentTypes.STATE_ACTION_OUTCOME, createdStateActionContent);
		} catch (Exception e) {
			LOG.error("Failed in transforming the workflow definition into Possible Actions: " + bpmn2FilePath);
			e.printStackTrace();
		}
	}

	private void transformNodesToPossibleActions(List<Node> nodes, Map<Long, List<Long>> nodeToOutgoingMap2,
			List<SequenceFlow> connections, Set<PossibleAction> actions) throws Exception {
		for (Node node : nodes) {
			if (node instanceof Split) {
				for (Long id : nodeToOutgoingMap.get(node.getId())) {
					String state = node.getName();
					String role = "SME";
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

					actions.add(new PossibleAction(state, action, outcome, role));
				}
			}
		}
	}

	private void processNodes(String bpmn2FilePath) {
		process = buildProcessFromFile(bpmn2FilePath);

		visitedNodes.clear();
		nodeToOutgoingMap.clear();
		List<Long> nodesInOrder = identifyOutputOrder(process.getStartNodes().iterator().next(), new ArrayList<Long>());

		// Populate the actual nodes object
		processNodes.clear();
		for (Long nodeId : nodesInOrder) {
			processNodes.add(process.getNode(nodeId));
		}
	}

	private void processDefinition(String string, String xmlContents, Object object, boolean b) {
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
			System.out.println("Couldn't Find Fine: " + bpmn2FilePath);
			e.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("Error in readFile method: " + bpmn2FilePath);
			ioe.printStackTrace();
		} catch (SAXException se) {
			System.out.println("Error in parsing XML file: " + bpmn2FilePath);
			se.printStackTrace();
		}

		return null;
	}

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

	private String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

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

	public List<Node> getProcessNodes() {
		return processNodes;
	}

	public Map<Long, List<Long>> getNodesToOutgoingMap() {
		return nodeToOutgoingMap;
	}

	public ProcessAssetDesc getProcessDefinition() {
		return processDefinition;
	}

	public RuleFlowProcess getProcess() {
		return process;
	}

	public ProcessDescriptor getProcessDescriptor() {
		return processDescriptor;
	}
}

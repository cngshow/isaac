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

package gov.vha.isaac.ochre.modules.vhat;

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.NecessarySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.DataTarget;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuildListener;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.logic.LogicNode;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.api.logic.NodeSemantic;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;
import gov.vha.isaac.ochre.model.logic.node.AbstractLogicNode;
import gov.vha.isaac.ochre.model.logic.node.AndNode;
import gov.vha.isaac.ochre.model.logic.node.NecessarySetNode;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUIDImpl;
import gov.vha.isaac.ochre.model.sememe.version.DynamicSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.LogicGraphSememeImpl;

/**
 * 
 * {@link VHATIsAHasParentSynchronizingSememeBuildListener}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
@Service(name = "VHATIsAHasASynchronizingSememeBuildListener")
@RunLevel(value = LookupService.SL_L3)
public class VHATIsAHasParentSynchronizingSememeBuildListener extends SememeBuildListener {
	private static final Logger LOG = LogManager.getLogger(VHATIsAHasParentSynchronizingSememeBuildListener.class);

	// TODO better tie this String literal UUID to the imported value
	private final static UUID HAS_PARENT_VHAT_ASSOCIATION_TYPE = UUID.fromString("4ab30955-f50a-5f5f-8397-3fe473b22ed1");

	// Cached VHAT module sequences
	private static Set<Integer> VHAT_MODULES = null;
	private static Set<Integer> getVHATModules() {
		// Initialize VHAT module sequences cache
		if (VHAT_MODULES == null) { // Should be unnecessary
			VHAT_MODULES = Frills.getAllChildrenOfConcept(MetaData.VHAT_MODULES.getConceptSequence(), true, true);
		}
		return VHAT_MODULES;
	}
	
	// Cached HAS_PARENT_VHAT_ASSOCIATION_TYPE_OBJECT
	private static Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> HAS_PARENT_VHAT_ASSOCIATION_TYPE_OBJECT = null;
	private static Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> getHasParentVHATAssociationTypeObject() {
		// Lazily initialize HAS_PARENT_VHAT_ASSOCIATION_TYPE_OBJECT
		if (HAS_PARENT_VHAT_ASSOCIATION_TYPE_OBJECT == null) {
			HAS_PARENT_VHAT_ASSOCIATION_TYPE_OBJECT = Get.conceptService().getOptionalConcept(Get.identifierService().getNidForUuids(HAS_PARENT_VHAT_ASSOCIATION_TYPE));
		}
		
		return HAS_PARENT_VHAT_ASSOCIATION_TYPE_OBJECT;
	}
	
	VHATIsAHasParentSynchronizingSememeBuildListener() {
		
	}

	private static Collection<DynamicSememeImpl> getActiveHasParentAssociationDynamicSememesAttachedToComponent(int nid) {
		final Set<Integer> selectedAssemblages = new HashSet<>();
		if (!getHasParentVHATAssociationTypeObject().isPresent())
		{
			String msg = "No concept for VHAT has_parent UUID=" + HAS_PARENT_VHAT_ASSOCIATION_TYPE + ". Disabling listener " + VHATIsAHasParentSynchronizingSememeBuildListener.class.getSimpleName();
			LOG.error(msg);
			throw new RuntimeException(msg);
		}
		selectedAssemblages.add(getHasParentVHATAssociationTypeObject().get().getConceptSequence());
		
		final Set<SememeType> sememeTypesToExclude = new HashSet<>();
		for (SememeType type : SememeType.values()) {
			if (type != SememeType.DYNAMIC) {
				sememeTypesToExclude.add(type);
			}
		}

		final StampPosition stampPosition = new StampPositionImpl(Long.MAX_VALUE, TermAux.DEVELOPMENT_PATH.getConceptSequence());
		final StampCoordinate activeVhatStampCoordinate = 
				new StampCoordinateImpl(StampPrecedence.PATH,
						stampPosition, 
						ConceptSequenceSet.of(getVHATModules()),
						State.ACTIVE_ONLY_SET);
		final Iterator<SememeChronology<? extends SememeVersion<?>>> it = Frills.getSememesForComponentFromAssemblagesFilteredBySememeType(nid, selectedAssemblages, sememeTypesToExclude).iterator();
		final List<DynamicSememeImpl> hasParentAssociationDynamicSememesToReturn = new ArrayList<>();
		while (it.hasNext()) {
			SememeChronology<DynamicSememeImpl> hasParentAssociationDynamicSememe = (SememeChronology<DynamicSememeImpl>)it.next();
			Optional<LatestVersion<DynamicSememeImpl>> optionalLatestVersion =  hasParentAssociationDynamicSememe.getLatestVersion(DynamicSememeImpl.class, activeVhatStampCoordinate);
			if (optionalLatestVersion.isPresent()) {
				if (optionalLatestVersion.get().contradictions().isPresent()) {
					// TODO handle contradictions
				}
				hasParentAssociationDynamicSememesToReturn.add(optionalLatestVersion.get().value());
			}
		}
		
		return Collections.unmodifiableList(hasParentAssociationDynamicSememesToReturn);
	}

	private static Set<Integer> getParentConceptSequencesFromLogicGraph(LogicGraphSememeImpl logicGraph) {
		Set<Integer> parentConceptSequences = new HashSet<>();
		Stream<LogicNode> isAs = logicGraph.getLogicalExpression().getNodesOfType(NodeSemantic.NECESSARY_SET);
		for (Iterator<LogicNode> necessarySetsIterator = isAs.distinct().iterator(); necessarySetsIterator.hasNext();) {
			NecessarySetNode necessarySetNode = (NecessarySetNode)necessarySetsIterator.next();
			for (AbstractLogicNode childOfNecessarySetNode : necessarySetNode.getChildren()) {
				if (childOfNecessarySetNode.getNodeSemantic() == NodeSemantic.AND) {
					AndNode andNode = (AndNode)childOfNecessarySetNode;
					for (AbstractLogicNode childOfAndNode : andNode.getChildren()) {
						if (childOfAndNode.getNodeSemantic() == NodeSemantic.CONCEPT) {
							ConceptNodeWithSequences conceptNode = (ConceptNodeWithSequences)childOfAndNode;
							parentConceptSequences.add(conceptNode.getConceptSequence());
						}
					}
				} else if (childOfNecessarySetNode.getNodeSemantic() == NodeSemantic.CONCEPT) {
					ConceptNodeWithSequences conceptNode = (ConceptNodeWithSequences)childOfNecessarySetNode;
					parentConceptSequences.add(conceptNode.getConceptSequence());
				} else {
					String msg = "Logic graph for concept NID=" + logicGraph.getReferencedComponentNid() + " has child of NecessarySet logic graph node of unexpected type \"" + childOfNecessarySetNode.getNodeSemantic() + "\". Expected AndNode or ConceptNode in " + logicGraph;
					LOG.error(msg);
					throw new RuntimeException(msg);
				}
			}
		}
		
		return parentConceptSequences;
	}
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.component.sememe.SememeBuildListenerI#applyAfter(gov.vha.isaac.ochre.api.coordinate.EditCoordinate, gov.vha.isaac.ochre.api.commit.ChangeCheckerMode, gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion, java.util.List)
	 */
	@Override
	public void applyAfter(EditCoordinate editCoordinate, ChangeCheckerMode changeCheckerMode,
			SememeVersion<?> builtSememeVersion, List<ObjectChronology<? extends StampedVersion>> builtObjects) {

		// Only apply to edits to cached VHAT modules
		if (! getVHATModules().contains(editCoordinate.getModuleSequence())) {
			return;
		}
		
		LOG.debug("Running " + getListenerName() + " applyAfter()...");
		
		// Fail if getHasParentVHATAssociationTypeObject() does not exist
		if (!getHasParentVHATAssociationTypeObject().isPresent())
		{
			String msg = "No concept for VHAT has_parent UUID=" + HAS_PARENT_VHAT_ASSOCIATION_TYPE + ". Disabling listener " + this.getListenerName();
			LOG.error(msg);
			this.disable();
			throw new RuntimeException(msg);
		}

		final Optional<? extends ObjectChronology<? extends StampedVersion>> referencedComponent = Get.identifiedObjectService()
				.getIdentifiedObjectChronology(builtSememeVersion.getReferencedComponentNid());

		if (!referencedComponent.isPresent()) {
			String msg = "No identified object for referenced component NID=" + builtSememeVersion.getReferencedComponentNid() + " for sememe " + builtSememeVersion;
			LOG.error(msg);
			throw new RuntimeException(msg);
		}

		if (builtSememeVersion.getChronology().getSememeType() == SememeType.LOGIC_GRAPH) {
			// Handle changes to LOGIC_GRAPH
			// In practice, this should only be for a new concept

			// Do not support retirement of a logic graph
			if (builtSememeVersion.getState() != State.ACTIVE) {
				String msg = "NON ACTIVE (" + builtSememeVersion.getState() + " ) LOGIC_GRAPH sememes NOT supported: " + builtSememeVersion;
				LOG.error(msg);
				throw new RuntimeException(msg);
			}
			
			// TODO check for and prohibit edits to logic graph, which are not supported
			
			Set<Integer> parentConceptSequences = getParentConceptSequencesFromLogicGraph((LogicGraphSememeImpl)builtSememeVersion);
			if (parentConceptSequences.size() == 0) {
				String msg = "Encountered logic graph for concept NID=" + referencedComponent.get().getNid() + ", UUID=" + referencedComponent.get().getPrimordialUuid() + " with no specified parents (concept nodes in necessary set nodes)";
				LOG.error(msg);
				throw new RuntimeException(msg);
			}
			
			for (int parentConceptSequence : parentConceptSequences) {
				final Optional<? extends ObjectChronology<? extends StampedVersion>> parentConcept = Get.identifiedObjectService()
						.getIdentifiedObjectChronology(Get.identifierService().getConceptNid(parentConceptSequence));
				if (!parentConcept.isPresent())
				{
					String msg = "No identified object for parentConcept UUID=" + parentConcept.get().getPrimordialUuid() + " for referenced component " + referencedComponent.get().getPrimordialUuid();
					LOG.error(msg);
					throw new RuntimeException(msg);
				}
				
				DynamicSememeData[] data = new DynamicSememeData[1];
				data[0] = (parentConcept.isPresent() ?  new DynamicSememeUUIDImpl(parentConcept.get().getPrimordialUuid()) : null);
				
				SememeBuilder<? extends SememeChronology<?>> associationSememeBuilder =  Get.sememeBuilderService().getDynamicSememeBuilder(
						referencedComponent.get().getNid(), getHasParentVHATAssociationTypeObject().get().getConceptSequence(), data);
				
				UUID associationItemUUID = UuidT5Generator.get(IsaacMappingConstants.get().MAPPING_NAMESPACE.getUUID(), 
						referencedComponent.get().getPrimordialUuid().toString() + "|" 
						+ getHasParentVHATAssociationTypeObject().get().getPrimordialUuid().toString() + "|"
						+ (!(parentConcept.isPresent()) ? "" : parentConcept.get().getPrimordialUuid().toString()) + "|");
				
				if (Get.identifierService().hasUuid(associationItemUUID))
				{
					String msg = "A has_parent association with the specified source (" + referencedComponent.get().getPrimordialUuid() + ") and target (" + parentConcept.get().getPrimordialUuid() + ") already exists";
					LOG.error(msg);
					throw new RuntimeException(msg);
				}
				
				associationSememeBuilder.setPrimordialUuid(associationItemUUID);
				
				ObjectChronology<? extends StampedVersion> builtHasParentAssociation = associationSememeBuilder.build(editCoordinate, ChangeCheckerMode.ACTIVE).getNoThrow();	
			
				builtObjects.add(builtHasParentAssociation);
			}
		} else if (builtSememeVersion.getChronology().getSememeType() == SememeType.DYNAMIC) {
			// Handle changes to associations
			
			// Get active has_parent association dynamic sememes attached to component
			Collection<DynamicSememeImpl> hasParentAssociationDynamicSememes = getActiveHasParentAssociationDynamicSememesAttachedToComponent(referencedComponent.get().getNid());

			// Create set of parent concept sequences from active has_parent association dynamic sememes attached to component
			Set<Integer> parentSequencesFromHasParentAssociationDynamicSememes = new HashSet<>();
			for (DynamicSememeImpl hasParentAssociationDynamicSememe : hasParentAssociationDynamicSememes) {
				UUID parentUuid = ((DynamicSememeUUIDImpl)hasParentAssociationDynamicSememe.getData()[0]).getDataUUID();
				parentSequencesFromHasParentAssociationDynamicSememes.add(Get.identifierService().getConceptSequenceForUuids(parentUuid));
			}
			
			// Get parent concept sequences from logic graph
			Optional<SememeChronology<? extends LogicGraphSememe<?>>> conceptLogicGraphSememeChronology = Frills.getLogicGraphChronology(referencedComponent.get().getNid(), true);
			if (! conceptLogicGraphSememeChronology.isPresent()) {
				String msg = "No logic graph sememe found for concept (" + referencedComponent.get().getPrimordialUuid() + ")";
				LOG.error(msg);
				throw new RuntimeException(msg);
			}
			
			// The below code is only necessary if comparing parents from has_parent association dynamic sememes to parents from logic graph
//			Set<Integer> parentSequencesFromLogicGraph = getParentConceptSequencesFromLogicGraph((LogicGraphSememeImpl)builtSememeVersion);
//
//			if (parentSequencesFromHasParentAssociationDynamicSememes.containsAll(parentSequencesFromLogicGraph)
//					&& parentSequencesFromLogicGraph.containsAll(parentSequencesFromHasParentAssociationDynamicSememes)) {
//				// This new builtSememeVersion has not resulted in added or retired or changed has_parent association
//				// No need to rebuild logic graph
//				
//				return;
//			}

			// This new builtSememeVersion may have resulted in added or retired or changed has_parent association
			// Need to rebuild logic graph
			
			LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();
			for (int parentConceptSequence : parentSequencesFromHasParentAssociationDynamicSememes) {
				NecessarySet(And(ConceptAssertion(parentConceptSequence, defBuilder)));
			}
			LogicalExpression parentDef = defBuilder.build();
			
			LogicGraphSememeImpl newLogicGraphSememeVersion = ((SememeChronology<LogicGraphSememeImpl>)(conceptLogicGraphSememeChronology.get())).createMutableVersion(LogicGraphSememeImpl.class, State.ACTIVE, editCoordinate);
			newLogicGraphSememeVersion.setGraphData(parentDef.getData(DataTarget.INTERNAL));
		}
	}
}

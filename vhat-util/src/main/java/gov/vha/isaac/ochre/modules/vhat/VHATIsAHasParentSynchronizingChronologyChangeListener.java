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
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

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
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.ChronologyChangeListener;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.coordinate.EditCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUIDImpl;
import gov.vha.isaac.ochre.model.sememe.version.DynamicSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.LogicGraphSememeImpl;

/**
 * 
 * {@link VHATIsAHasParentSynchronizingChronologyChangeListener}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
//@Service // TODO uncomment to activate
//@RunLevel(value = LookupService.SL_L1) // TODO uncomment to activate
public class VHATIsAHasParentSynchronizingChronologyChangeListener implements ChronologyChangeListener {
	private static final Logger LOG = LogManager.getLogger(VHATIsAHasParentSynchronizingChronologyChangeListener.class);

	// Cached VHAT module sequences
	private static ConceptSequenceSet VHAT_MODULES = null;
	private static ConceptSequenceSet getVHATModules() {
		// Initialize VHAT module sequences cache
		if (VHAT_MODULES == null || VHAT_MODULES.size() == 0) { // Should be unnecessary
			VHAT_MODULES = ConceptSequenceSet.of(Frills.getAllChildrenOfConcept(MetaData.VHAT_MODULES.getConceptSequence(), true, true));
		}
		return VHAT_MODULES;
	}
	
	private static StampCoordinate VHAT_STAMP_COORDINATE = null;
	private StampCoordinate getVHATDevelopmentLatestStampCoordinate() {
		if (VHAT_STAMP_COORDINATE == null) {
			StampPosition stampPosition = new StampPositionImpl(Long.MAX_VALUE, 
					TermAux.DEVELOPMENT_PATH.getConceptSequence());
			VHAT_STAMP_COORDINATE = new StampCoordinateImpl(StampPrecedence.PATH, stampPosition, 
					getVHATModules(), State.ANY_STATE_SET);
		}

		return VHAT_STAMP_COORDINATE;
	}
	
	private static Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> getHasParentVHATAssociationTypeObject() {
		return Get.conceptService().getOptionalConcept(VHATConstants.VHAT_HAS_PARENT_ASSOCIATION_TYPE.getNid());
	}

	private final static ConcurrentSkipListSet<Integer> sequencesOfGeneratedSememesToIgnore = new ConcurrentSkipListSet<>();
	private final static UUID providerUuid = UUID.randomUUID();

	private final ConcurrentSkipListSet<Integer> sememeSequencesForUnhandledLogicGraphChanges = new ConcurrentSkipListSet<>();
	private final ConcurrentSkipListSet<Integer> sememeSequencesForUnhandledHasParentAssociationChanges = new ConcurrentSkipListSet<>();

	public VHATIsAHasParentSynchronizingChronologyChangeListener() {
	}

	@PostConstruct
	private void startMe() {
		Get.commitService().addChangeListener(this);
	}

	@PreDestroy
	private void stopMe() {
		Get.commitService().removeChangeListener(this);
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.commit.ChronologyChangeListener#getListenerUuid()
	 */
	@Override
	public UUID getListenerUuid() {
		return providerUuid;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.commit.ChronologyChangeListener#handleChange(gov.vha.isaac.ochre.api.component.concept.ConceptChronology)
	 */
	@Override
	public void handleChange(ConceptChronology<? extends StampedVersion> cc) {
		// Only using handleCommit()
	}

	private <S extends SememeVersion<? extends SememeVersion<?>>> Optional<S> getLatestVersion(SememeChronology<S> sememeChronology, StampCoordinate stampCoordinate) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final Optional<LatestVersion<SememeVersion<?>>> rawLatestOptional = ((SememeChronology)sememeChronology).getLatestVersion(SememeVersion.class, stampCoordinate);
		if (! rawLatestOptional.isPresent()) {
			// Ignore non-VHAT commit
			return Optional.empty();
		}
		
		// TODO handle LatestVersion contradictions
		
		SememeVersion rawLatestVersion = rawLatestOptional.get().value();
		return Optional.of((S)rawLatestVersion);
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.commit.ChronologyChangeListener#handleChange(gov.vha.isaac.ochre.api.component.sememe.SememeChronology)
	 */
	@Override
	public void handleChange(SememeChronology<? extends SememeVersion<?>> sc) {
		if (sequencesOfGeneratedSememesToIgnore.contains(sc.getSememeSequence())) {
			// This is a sememe generated by this listener, so remove it and ignore it
			sequencesOfGeneratedSememesToIgnore.remove(sc.getSememeSequence());
			return;
		}
		
		final Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> hasParentVHATAssociationTypeObject = getHasParentVHATAssociationTypeObject();
		if (! hasParentVHATAssociationTypeObject.isPresent()) {
			// Ignore because VHAT not configured in DB
			return;
		}
		
		if (sc.getSememeType() == SememeType.LOGIC_GRAPH) {
			sememeSequencesForUnhandledLogicGraphChanges.add(sc.getSememeSequence());
		} else if (sc.getSememeType() == SememeType.DYNAMIC
				&& sc.getAssemblageSequence() == getHasParentVHATAssociationTypeObject().get().getConceptSequence()) {
			sememeSequencesForUnhandledHasParentAssociationChanges.add(sc.getSememeSequence());
		} else {
			// Ignore if not either LOGIC_GRAPH or DYNAMIC has_parent association sememe
			return;
		}
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.commit.ChronologyChangeListener#handleCommit(gov.vha.isaac.ochre.api.commit.CommitRecord)
	 */
	@Override
	public void handleCommit(CommitRecord commitRecord) {
		// Only using handleChange(SememeChronology<? extends SememeVersion<?>> sc)
		for (int logicGraphSequence : sememeSequencesForUnhandledLogicGraphChanges) {
			try {
				if (! commitRecord.getSememesInCommit().contains(logicGraphSequence)) {
					continue;
				}
				SememeChronology<? extends SememeVersion<?>> sc = Get.sememeService().getSememe(logicGraphSequence);
				Optional<LogicGraphSememeImpl> logicGraph = getLatestVersion((SememeChronology<LogicGraphSememeImpl>)sc, getVHATDevelopmentLatestStampCoordinate());
				if (! logicGraph.isPresent()) {
					// Apparently not a relevant LOGIC_GRAPH sememe
					return;
				}

				LOG.debug("Running VHATIsAHasParentSynchronizingChronologyChangeListener handleChange() on VHAT LOGIC_GRAPH dynamic sememe " + sc.getPrimordialUuid() + " for concept " + Get.identifierService().getUuidPrimordialForNid(sc.getReferencedComponentNid()));

				ConceptChronology<? extends ConceptVersion<?>> referencedConcept = Get.conceptService().getConcept(logicGraph.get().getReferencedComponentNid());

				// Handle changes to LOGIC_GRAPH
				// In practice, this should only be for a new concept
				Set<Integer> parentsAccordingToNewLogicGraphVersion = new HashSet<>();
				if (logicGraph.get().getState() == State.INACTIVE) {
					// Retire all has_parent association sememes
				} else {
					parentsAccordingToNewLogicGraphVersion.addAll(Frills.getParentConceptSequencesFromLogicGraph((LogicGraphSememeImpl)logicGraph.get()));
					if (parentsAccordingToNewLogicGraphVersion.size() == 0) {
						String msg = "Encountered logic graph for concept NID=" + referencedConcept.getNid() + ", UUID=" + referencedConcept.getPrimordialUuid() + " with no specified parents (concept nodes in necessary set nodes)";
						LOG.warn(msg);
					}
				}

				final EditCoordinate editCoordinate = new EditCoordinateImpl(
						Get.identifierService().getConceptNid(logicGraph.get().getAuthorSequence()),
						Get.identifierService().getConceptNid(logicGraph.get().getModuleSequence()),
						Get.identifierService().getConceptNid(logicGraph.get().getPathSequence()));

				final Collection<DynamicSememeImpl> hasParentAssociationDynamicSememes = getActiveHasParentAssociationDynamicSememesAttachedToComponent(logicGraph.get().getReferencedComponentNid());
				Set<Integer> parentsAccordingToHasParentAssociationDynamicSememes = new HashSet<>();
				// For each active has_parent association
				if (hasParentAssociationDynamicSememes.size() > 0) {
					for (DynamicSememeImpl hasParentSememe : hasParentAssociationDynamicSememes) {
						final DynamicSememeUUID target = (DynamicSememeUUID)hasParentSememe.getData(0);
						final int targetSeq = Get.identifierService().getConceptSequenceForUuids(target.getDataUUID());
						// Accumulate a list of parents from has_parent sememes
						parentsAccordingToHasParentAssociationDynamicSememes.add(targetSeq);
						// If the active has_parent is not represented in an active logic graph, retire it
						if (! parentsAccordingToNewLogicGraphVersion.contains(targetSeq)) {
							DynamicSememeImpl mutableVersion = hasParentSememe.getChronology().createMutableVersion(DynamicSememeImpl.class, State.INACTIVE, editCoordinate);
							mutableVersion.setData(hasParentSememe.getData());
							sequencesOfGeneratedSememesToIgnore.add(hasParentSememe.getNid());
							Get.commitService().addUncommitted(mutableVersion.getChronology());
						}
					}

					Get.commitService().commit("Retiring " + hasParentAssociationDynamicSememes.size() + " VHAT has_parent sememes");
				}

				// For each parent from an active logic graph
				for (int parentAccordingToNewLogicGraphVersion : parentsAccordingToNewLogicGraphVersion) {
					// If the parent from the active logic graph is not already represented by an active has_parent sememe
					// Create a new has_parent sememe
					if (! parentsAccordingToHasParentAssociationDynamicSememes.contains(parentAccordingToNewLogicGraphVersion)) {
						Optional<UUID> uuidOfParentAccordingToNewLogicGraphVersion = Get.identifierService().getUuidPrimordialFromConceptId(parentAccordingToNewLogicGraphVersion);
						if (! uuidOfParentAccordingToNewLogicGraphVersion.isPresent()) {
							LOG.error("FAILED finding UUID for parent seq=" + parentAccordingToNewLogicGraphVersion + " from logic graph for concept NID=" + referencedConcept.getNid() + ", UUID=" + referencedConcept.getPrimordialUuid());
							continue;
						}
						DynamicSememeData[] data = new DynamicSememeData[1];
						data[0] = new DynamicSememeUUIDImpl(uuidOfParentAccordingToNewLogicGraphVersion.get());

						SememeBuilder<? extends SememeChronology<?>> associationSememeBuilder =  Get.sememeBuilderService().getDynamicSememeBuilder(
								referencedConcept.getNid(), getHasParentVHATAssociationTypeObject().get().getConceptSequence(), data);
						UUID associationItemUUID = UuidT5Generator.get(IsaacMappingConstants.get().MAPPING_NAMESPACE.getUUID(), 
								referencedConcept.getPrimordialUuid().toString() + "|" 
										+ getHasParentVHATAssociationTypeObject().get().getPrimordialUuid().toString() + "|"
										+ uuidOfParentAccordingToNewLogicGraphVersion.get());
						if (Get.identifierService().hasUuid(associationItemUUID))
						{
							String msg = "A has_parent association with the specified source (" + referencedConcept.getPrimordialUuid() + ") and target (" + uuidOfParentAccordingToNewLogicGraphVersion.get() + ") already exists";
							LOG.error(msg);
							continue;
						}
						associationSememeBuilder.setPrimordialUuid(associationItemUUID);

						/* ObjectChronology<? extends StampedVersion> builtHasParentAssociation = */ associationSememeBuilder.build(editCoordinate, ChangeCheckerMode.ACTIVE).getNoThrow();
						LOG.debug("Built new has_parent association sememe with SOURCE/CHILD={} and TARGET/PARENT={}", referencedConcept.getPrimordialUuid(), uuidOfParentAccordingToNewLogicGraphVersion.get());
					}
				}
			} finally {
				sememeSequencesForUnhandledLogicGraphChanges.remove(logicGraphSequence);
			}
		}

		for (int hasParentSememeSequence : sememeSequencesForUnhandledHasParentAssociationChanges) {
			try {
				if (! commitRecord.getSememesInCommit().contains(hasParentSememeSequence)) {
					continue;
				}
				SememeChronology<? extends SememeVersion<?>> sc = Get.sememeService().getSememe(hasParentSememeSequence);
				Optional<DynamicSememeImpl> hasParentSememe = getLatestVersion((SememeChronology<DynamicSememeImpl>)sc, getVHATDevelopmentLatestStampCoordinate());
				if (! hasParentSememe.isPresent()) {
					// Apparently not a relevant has_parent association sememe
					return;
				}

				ConceptChronology<? extends ConceptVersion<?>> referencedConcept = Get.conceptService().getConcept(hasParentSememe.get().getReferencedComponentNid());

				LOG.debug("Running VHATIsAHasParentSynchronizingChronologyChangeListener handleChange() on VHAT has_parent dynamic sememe {} for concept {}", sc.getPrimordialUuid(), referencedConcept.getPrimordialUuid());

				final EditCoordinate editCoordinate = new EditCoordinateImpl(
						Get.identifierService().getConceptNid(hasParentSememe.get().getAuthorSequence()),
						Get.identifierService().getConceptNid(hasParentSememe.get().getModuleSequence()),
						Get.identifierService().getConceptNid(hasParentSememe.get().getPathSequence()));

				// Handle changes to associations

				// Get active has_parent association dynamic sememes attached to component
				Collection<DynamicSememeImpl> hasParentAssociationDynamicSememes = getActiveHasParentAssociationDynamicSememesAttachedToComponent(referencedConcept.getNid());

				// Create set of parent concept sequences from active has_parent association dynamic sememes attached to component
				Set<Integer> parentSequencesFromHasParentAssociationDynamicSememes = new HashSet<>();
				for (DynamicSememeImpl hasParentAssociationDynamicSememe : hasParentAssociationDynamicSememes) {
					UUID parentUuid = ((DynamicSememeUUIDImpl)hasParentAssociationDynamicSememe.getData()[0]).getDataUUID();
					parentSequencesFromHasParentAssociationDynamicSememes.add(Get.identifierService().getConceptSequenceForUuids(parentUuid));
				}

				// Get logic graph sememe chronology in order to create new version
				Optional<SememeChronology<? extends LogicGraphSememe<?>>> conceptLogicGraphSememeChronology = Frills.getLogicGraphChronology(referencedConcept.getNid(), true);
				if (! conceptLogicGraphSememeChronology.isPresent()) {
					String msg = "No logic graph sememe found for concept (NID=" + referencedConcept.getPrimordialUuid() + ")";
					LOG.error(msg);
					return;
				}

				// This new builtSememeVersion may have resulted in added or retired or changed has_parent association
				// Need to rebuild logic graph

				LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();
				for (int parentConceptSequence : parentSequencesFromHasParentAssociationDynamicSememes) {
					NecessarySet(And(ConceptAssertion(parentConceptSequence, defBuilder)));
				}
				LogicalExpression parentDef = defBuilder.build();

				@SuppressWarnings("unchecked")
				LogicGraphSememeImpl newLogicGraphSememeVersion = ((SememeChronology<LogicGraphSememeImpl>)(conceptLogicGraphSememeChronology.get())).createMutableVersion(LogicGraphSememeImpl.class, (hasParentAssociationDynamicSememes.size() > 0 ? State.ACTIVE : State.INACTIVE), editCoordinate);
				newLogicGraphSememeVersion.setGraphData(parentDef.getData(DataTarget.INTERNAL));

				sequencesOfGeneratedSememesToIgnore.add(conceptLogicGraphSememeChronology.get().getSememeSequence());
				Get.commitService().addUncommitted(conceptLogicGraphSememeChronology.get());

				Runnable runnable = new Runnable() {
					public void run() {
						try {
							Get.commitService().commit("Updating VHAT logic graph sememe for concept (NID=" + referencedConcept.getPrimordialUuid() + ")").get();
							LOG.debug("Created new version of logic graph sememe {} with {} parent(s) for concept {}", newLogicGraphSememeVersion.getPrimordialUuid(), parentSequencesFromHasParentAssociationDynamicSememes.size(), referencedConcept.getPrimordialUuid());
						} catch (InterruptedException | ExecutionException e) {
							LOG.error("FAILED committing new version of logic graph sememe " + newLogicGraphSememeVersion.getPrimordialUuid() + " with " + parentSequencesFromHasParentAssociationDynamicSememes.size() + " parent(s) for concept " + referencedConcept.getPrimordialUuid(), e);
						}
					}
				};
				Get.workExecutors().getExecutor().submit(runnable);
			} finally {
				sememeSequencesForUnhandledHasParentAssociationChanges.remove(hasParentSememeSequence);
			}
		}
	}
	
	private static Collection<DynamicSememeImpl> getActiveHasParentAssociationDynamicSememesAttachedToComponent(int nid) {
		final Set<Integer> selectedAssemblages = new HashSet<>();
		if (!getHasParentVHATAssociationTypeObject().isPresent())
		{
			String msg = "No concept for VHAT has_parent UUID=" + VHATConstants.VHAT_HAS_PARENT_ASSOCIATION_TYPE.getPrimordialUuid();
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
}
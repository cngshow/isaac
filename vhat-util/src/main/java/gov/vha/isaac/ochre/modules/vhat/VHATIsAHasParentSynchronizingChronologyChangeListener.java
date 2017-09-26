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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
import gov.vha.isaac.ochre.api.logic.assertions.Assertion;
import gov.vha.isaac.ochre.impl.utility.Frills;
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
@Service
@RunLevel(value = LookupService.SL_L1)
public class VHATIsAHasParentSynchronizingChronologyChangeListener implements VHATIsAHasParentSynchronizingChronologyChangeListenerI {
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
	private static StampCoordinate getVHATDevelopmentLatestStampCoordinate() {
		if (VHAT_STAMP_COORDINATE == null) {
			StampPosition stampPosition = new StampPositionImpl(Long.MAX_VALUE, 
					TermAux.DEVELOPMENT_PATH.getConceptSequence());
			VHAT_STAMP_COORDINATE = new StampCoordinateImpl(StampPrecedence.PATH, stampPosition, 
					getVHATModules(), State.ANY_STATE_SET);
		}

		return VHAT_STAMP_COORDINATE;
	}
	
	private boolean enabled = true;

	/**
	 * Set of nids of component versions created by VHATIsAHasParentSynchronizingChronologyChangeListener
	 * which should NOT be processed by VHATIsAHasParentSynchronizingChronologyChangeListener,
	 * in order to avoid infinite recursion
	 */
	private final Set<Integer> nidsOfGeneratedSememesToIgnore = new ConcurrentSkipListSet<>();
	private final UUID providerUuid = UUID.randomUUID();

	private final ConcurrentSkipListSet<Integer> sememeSequencesForUnhandledLogicGraphChanges = new ConcurrentSkipListSet<>();
	private final ConcurrentSkipListSet<Integer> sememeSequencesForUnhandledHasParentAssociationChanges = new ConcurrentSkipListSet<>();
	
	private ConcurrentLinkedQueue<Future<?>> inProgressJobs = new ConcurrentLinkedQueue<>();
	private ScheduledFuture<?> sf;

	public VHATIsAHasParentSynchronizingChronologyChangeListener() {
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.modules.vhat.VHATIsAHasParentSynchronizingChronologyChangeListenerI#addNidsOfGeneratedSememesToIgnore(int)
	 */
	@Override
	public void addNidsOfGeneratedSememesToIgnore(int...nids) {
		for (int nid : nids) {
			nidsOfGeneratedSememesToIgnore.add(nid);
		}
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.VHATIsAHasParentSynchronizingChronologyChangeListenerI#removeNidsOfGeneratedSememesToIgnore(int[])
	 */
	@Override
	public void removeNidsOfGeneratedSememesToIgnore(int... nids) {
		for (int nid : nids) {
			nidsOfGeneratedSememesToIgnore.remove(nid);
		}
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.VHATIsAHasParentSynchronizingChronologyChangeListenerI#disable()
	 */
	@Override
	public void disable() {
		enabled = false;
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.VHATIsAHasParentSynchronizingChronologyChangeListenerI#enable()
	 */
	@Override
	public void enable() {
		enabled = true;
	}

	@PostConstruct
	private void startMe() {
		Get.commitService().addChangeListener(this);
		//Prevent a memory leak, by scheduling a thread to periodically empty the job list 
		sf = Get.workExecutors().getScheduledThreadPoolExecutor().scheduleAtFixedRate((() -> waitForJobsToComplete()), 5, 5, TimeUnit.MINUTES);
	}

	@PreDestroy
	private void stopMe() {
		Get.commitService().removeChangeListener(this);
		sf.cancel(true);
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
		if (! enabled) {
			return;
		}
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.commit.ChronologyChangeListener#handleChange(gov.vha.isaac.ochre.api.component.sememe.SememeChronology)
	 */
	@Override
	public void handleChange(SememeChronology<? extends SememeVersion<?>> sc) {
		if (! enabled) {
			LOG.debug("Ignoring, while listener disabled, change to sememe " + sc.getSememeType() + " " + sc.getNid() + " " + sc.getSememeSequence());

			return;
		}

		// Determine if this is a sememe generated by this listener
		if (nidsOfGeneratedSememesToIgnore.contains(sc.getNid())) {
			// This is a sememe generated by this listener, so remove it and ignore it
			nidsOfGeneratedSememesToIgnore.remove(sc.getNid());
			LOG.info("Ignoring recursive change to sememe " + sc.getSememeType() + " " + sc.getNid() + " " + sc.getSememeSequence());
			return;
		}
		
		if (sc.getSememeType() == SememeType.LOGIC_GRAPH) {
			sememeSequencesForUnhandledLogicGraphChanges.add(sc.getSememeSequence());
			LOG.info("Adding LogicGraph " + sc.getNid() + " " + sc.getSememeSequence() + " to the list of commits to process");
		} else if (sc.getSememeType() == SememeType.DYNAMIC
				&& sc.getAssemblageSequence() == VHATConstants.VHAT_HAS_PARENT_ASSOCIATION_TYPE.getConceptSequence()) {
			sememeSequencesForUnhandledHasParentAssociationChanges.add(sc.getSememeSequence());
			LOG.info("Adding Association sememe " + sc.getNid() + " " + sc.getSememeSequence()  + " to the list of commits to process");
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
		// For new and updated VHAT logic graphs, create or retire has_parent associations, as appropriate
		LOG.info("HandleCommit looking for - logic graphs: " + sememeSequencesForUnhandledLogicGraphChanges + " associations: " 
				+ sememeSequencesForUnhandledHasParentAssociationChanges + " the commit contains " + commitRecord.getSememesInCommit());
		for (int logicGraphSequence : sememeSequencesForUnhandledLogicGraphChanges) {
			if (! commitRecord.getSememesInCommit().contains(logicGraphSequence)) {
				continue;
			}
			sememeSequencesForUnhandledLogicGraphChanges.remove(logicGraphSequence);
			SememeChronology<? extends SememeVersion<?>> sc = Get.sememeService().getSememe(logicGraphSequence);
			@SuppressWarnings("unchecked")
			Optional<LogicGraphSememeImpl> logicGraph = Frills.getLatestVersion((SememeChronology<LogicGraphSememeImpl>)sc, getVHATDevelopmentLatestStampCoordinate());
			if (! logicGraph.isPresent()) {
				// Apparently not a relevant LOGIC_GRAPH sememe
				return;
			}

			LOG.debug("Running VHATIsAHasParentSynchronizingChronologyChangeListener handleChange() on VHAT LOGIC_GRAPH dynamic sememe " 
					+ sc.getPrimordialUuid() + " for concept " + Get.identifierService().getUuidPrimordialForNid(sc.getReferencedComponentNid()));

			ConceptChronology<? extends ConceptVersion<?>> referencedConcept = Get.conceptService().getConcept(logicGraph.get().getReferencedComponentNid());

			// Handle changes to LOGIC_GRAPH
			// In practice, this should only be for a new concept
			Set<Integer> parentsAccordingToNewLogicGraphVersion = new HashSet<>();
			if (logicGraph.get().getState() == State.INACTIVE) {
				// Retire all has_parent association sememes
			} else {
				parentsAccordingToNewLogicGraphVersion.addAll(Frills.getParentConceptSequencesFromLogicGraph((LogicGraphSememeImpl)logicGraph.get()));
				if (parentsAccordingToNewLogicGraphVersion.size() == 0) {
					String msg = "Encountered logic graph for concept NID=" + referencedConcept.getNid() + ", UUID=" + referencedConcept.getPrimordialUuid() 
					+ " with no specified parents (concept nodes in necessary set nodes)";
					LOG.warn(msg);
				}
			}

			final EditCoordinate editCoordinate = new EditCoordinateImpl(
					Get.identifierService().getConceptNid(logicGraph.get().getAuthorSequence()),
					Get.identifierService().getConceptNid(logicGraph.get().getModuleSequence()),
					Get.identifierService().getConceptNid(logicGraph.get().getPathSequence()));

			final Collection<DynamicSememeImpl> hasParentAssociationDynamicSememes = 
					getActiveHasParentAssociationDynamicSememesAttachedToComponent(logicGraph.get().getReferencedComponentNid());
			Set<Integer> parentsAccordingToHasParentAssociationDynamicSememes = new HashSet<>();
			AtomicReference<Future<?>> f = new AtomicReference<Future<?>>(null);
			// For each active has_parent association
			if (hasParentAssociationDynamicSememes.size() > 0) {
				Runnable runnable = new Runnable() {
					public void run() {
						int retireCount = 0;
						for (DynamicSememeImpl hasParentSememe : hasParentAssociationDynamicSememes) {
							final DynamicSememeUUID target = (DynamicSememeUUID)hasParentSememe.getData(0);
							final int targetSeq = Get.identifierService().getConceptSequenceForUuids(target.getDataUUID());
							// Accumulate a list of parents from has_parent sememes
							parentsAccordingToHasParentAssociationDynamicSememes.add(targetSeq);
							// If the active has_parent is not represented in an active logic graph, retire it
							if (! parentsAccordingToNewLogicGraphVersion.contains(targetSeq)) {
								DynamicSememeImpl mutableVersion = hasParentSememe.getChronology().createMutableVersion(DynamicSememeImpl.class, State.INACTIVE, editCoordinate);
								mutableVersion.setData(hasParentSememe.getData());
								LOG.info("Putting association sememe " + mutableVersion.getNid() + " " + mutableVersion.getSememeSequence() 
									+ " into the ignore list prior to commit");

								// This is a sememe generated by this listener, so add it to list so listener will ignore it
								nidsOfGeneratedSememesToIgnore.add(hasParentSememe.getNid());
								try {
									Get.commitService().addUncommitted(mutableVersion.getChronology()).get();
									retireCount++;
								} catch (Exception e) {
									// New version of this sememe failed to be added to commit list, so remove sememe from list so listener won't ignore it
									nidsOfGeneratedSememesToIgnore.remove(hasParentSememe.getNid());
									LOG.error("FAILED calling addUncommitted() to retire VHAT has_parent association sememe " + hasParentSememe, e);
								}
							}
						}

						if (retireCount > 0)
						{
							try {
								Get.commitService().commit("Retiring " + retireCount + " VHAT has_parent sememes").get();
							} catch (InterruptedException | ExecutionException e) {
								LOG.error("FAILED commit while retiring " + retireCount + " VHAT has_parent sememes");
							}
						}
					}
				};
				f.set(Get.workExecutors().getExecutor().submit(runnable));
				inProgressJobs.add(f.get());
			}

			// For each parent from an active logic graph

		
			// If the parent from the active logic graph is not already represented by an active has_parent sememe
			// Create new or unretire existing has_parent sememe
			Runnable runnable = new Runnable() {
				public void run() {
					//Our logic depends on the retirements above, being done...
					if (f.get() != null){	
						try
						{
							f.get().get();
						}
						catch (Exception e)
						{
							LOG.error("error in required prior thread",e);
							return;
						}
					}
					int buildCount = 0;
					for (int parentAccordingToNewLogicGraphVersion : parentsAccordingToNewLogicGraphVersion) {
						if (! parentsAccordingToHasParentAssociationDynamicSememes.contains(parentAccordingToNewLogicGraphVersion)) {

							Optional<UUID> uuidOfParentAccordingToNewLogicGraphVersion = Get.identifierService()
									.getUuidPrimordialFromConceptId(parentAccordingToNewLogicGraphVersion);
							if (! uuidOfParentAccordingToNewLogicGraphVersion.isPresent()) {
								LOG.error("FAILED finding UUID for parent seq=" + parentAccordingToNewLogicGraphVersion 
										+ " from logic graph for concept NID=" + referencedConcept.getNid() + ", UUID=" + referencedConcept.getPrimordialUuid());
								continue;
							}
							
							// Check for existence of retired version of has_parent association with this parent target
							Optional<DynamicSememeImpl> retiredHasParentSememeVersion = getInactiveHasParentAssociationDynamicSememeAttachedToComponent(referencedConcept.getNid(), parentAccordingToNewLogicGraphVersion);
							if (retiredHasParentSememeVersion.isPresent()) {
								// If retired version of has_parent association with this parent target exists, then re-activate it
								DynamicSememeImpl unretiredHasParentSememeVersion = ((SememeChronology<DynamicSememeImpl>)(retiredHasParentSememeVersion.get().getChronology())).createMutableVersion(DynamicSememeImpl.class, State.ACTIVE, editCoordinate);
								unretiredHasParentSememeVersion.setData(retiredHasParentSememeVersion.get().getData());
								// This is a sememe generated by this listener, so add it to list so listener will ignore it
								nidsOfGeneratedSememesToIgnore.add(retiredHasParentSememeVersion.get().getNid());

								try {
									Get.commitService().addUncommittedNoChecks(retiredHasParentSememeVersion.get().getChronology()).get();
								} catch (Exception e) {
									// New version of this sememe failed to be added to commit list, so remove sememe from list so listener won't ignore it
									nidsOfGeneratedSememesToIgnore.remove(retiredHasParentSememeVersion.get().getNid());
									LOG.error("FAILED calling addUncommitted() to unretire has_parent association sememe (target UUID=" + uuidOfParentAccordingToNewLogicGraphVersion + ") of VHAT concept " + referencedConcept, e);
									return;
								}
								try {
									Get.commitService().commit("Unretiring VHAT has_parent association sememe (target UUID=" + uuidOfParentAccordingToNewLogicGraphVersion + ") for concept (UUID=" + referencedConcept.getPrimordialUuid() + ")").get();
								} catch (Exception e) {
									// New version of this sememe may have failed to be committed, so remove sememe from list so listener won't ignore it
									nidsOfGeneratedSememesToIgnore.remove(retiredHasParentSememeVersion.get().getNid());
									LOG.error("FAILED calling commit() to unretire has_parent association sememe (target UUID=" + uuidOfParentAccordingToNewLogicGraphVersion + ") of VHAT concept " + referencedConcept, e);
									return;
								}
								LOG.debug("Unretired has_parent association sememe {} with target {} for concept {}", retiredHasParentSememeVersion.get().getPrimordialUuid(), uuidOfParentAccordingToNewLogicGraphVersion, referencedConcept.getPrimordialUuid());
							} else {
								// If retired version on this has_parent association does not exist, then create a new one

								DynamicSememeData[] data = new DynamicSememeData[1];
								data[0] = new DynamicSememeUUIDImpl(uuidOfParentAccordingToNewLogicGraphVersion.get());

								SememeBuilder<? extends SememeChronology<?>> associationSememeBuilder =  Get.sememeBuilderService().getDynamicSememeBuilder(
										referencedConcept.getNid(), VHATConstants.VHAT_HAS_PARENT_ASSOCIATION_TYPE.getConceptSequence(), data);

								// This is a sememe generated by this listener, so add it to list so listener will ignore it
								LOG.info("Putting association sememe " + associationSememeBuilder.getNid()+ " into the ignore list prior to build");
								nidsOfGeneratedSememesToIgnore.add(associationSememeBuilder.getNid());

								/* ObjectChronology<? extends StampedVersion> builtHasParentAssociation = */ associationSememeBuilder
										.build(editCoordinate, ChangeCheckerMode.ACTIVE).getNoThrow();
								buildCount++;

								LOG.debug("Built new has_parent association sememe with SOURCE/CHILD={} and TARGET/PARENT={}", referencedConcept.getPrimordialUuid(), 
										uuidOfParentAccordingToNewLogicGraphVersion.get());
							}
						}
					}
					
					if (buildCount > 0)
					{
						try {
							Get.commitService().commit("Committing " + buildCount + " new has_parent association sememes.").get();
						} catch (InterruptedException | ExecutionException e) {
							LOG.error("FAILED committing new has_parent association sememes", e);
							return;
						}
					}
				}
			};
			inProgressJobs.add(Get.workExecutors().getExecutor().submit(runnable));
		}

		// For new, updated or retired VHAT has_parent association sememes, update existing logic graph
		for (int hasParentSememeSequence : sememeSequencesForUnhandledHasParentAssociationChanges) {
			if (! commitRecord.getSememesInCommit().contains(hasParentSememeSequence)) {
				continue;
			}
			sememeSequencesForUnhandledHasParentAssociationChanges.remove(hasParentSememeSequence);
			SememeChronology<? extends SememeVersion<?>> sc = Get.sememeService().getSememe(hasParentSememeSequence);
			@SuppressWarnings("unchecked")
			Optional<DynamicSememeImpl> hasParentSememe = Frills.getLatestVersion((SememeChronology<DynamicSememeImpl>)sc, getVHATDevelopmentLatestStampCoordinate());
			if (! hasParentSememe.isPresent()) {
				// Apparently not a relevant has_parent association sememe
				return;
			}

			ConceptChronology<? extends ConceptVersion<?>> referencedConcept = Get.conceptService().getConcept(hasParentSememe.get().getReferencedComponentNid());

			LOG.debug("Running VHATIsAHasParentSynchronizingChronologyChangeListener handleChange() on VHAT has_parent dynamic sememe {} for concept {}", 
					sc.getPrimordialUuid(), referencedConcept.getPrimordialUuid());

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
			final Optional<SememeChronology<? extends LogicGraphSememe<?>>> conceptLogicGraphSememeChronology = Frills.getLogicGraphChronology(referencedConcept.getNid(), true);
			if (! conceptLogicGraphSememeChronology.isPresent()) {
				String msg = "No logic graph sememe found for concept (NID=" + referencedConcept.getPrimordialUuid() + ")";
				LOG.error(msg);
				return;
			}

			final Runnable updateExistingLogicGraphRunnable = new Runnable() {
				public void run() {
					try {
						// This new builtSememeVersion may have resulted in added or retired or changed has_parent association
						// Need to rebuild logic graph
						LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();
						ArrayList<Assertion> assertions = new ArrayList<>();
						for (int parentConceptSequence : parentSequencesFromHasParentAssociationDynamicSememes) {
							assertions.add(ConceptAssertion(parentConceptSequence, defBuilder));
						}
						
						NecessarySet(And(assertions.toArray(new Assertion[assertions.size()])));
						LogicalExpression parentDef = defBuilder.build();

						// This code for use when updating an existing logic graph sememe
						@SuppressWarnings("unchecked")
						LogicGraphSememeImpl newLogicGraphSememeVersion = ((SememeChronology<LogicGraphSememeImpl>)(conceptLogicGraphSememeChronology.get()))
							.createMutableVersion(LogicGraphSememeImpl.class, (hasParentAssociationDynamicSememes.size() > 0 ? State.ACTIVE : State.INACTIVE), 
									editCoordinate);
						newLogicGraphSememeVersion.setGraphData(parentDef.getData(DataTarget.INTERNAL));
						// This is a sememe generated by this listener, so add it to list so listener will ignore it
						LOG.info("Putting logic graph " + newLogicGraphSememeVersion.getNid() + " " + newLogicGraphSememeVersion.getSememeSequence() 
						+ " into the ignore list prior to commit");
						nidsOfGeneratedSememesToIgnore.add(conceptLogicGraphSememeChronology.get().getNid());
						LOG.debug("Created the logic graph " + newLogicGraphSememeVersion + " due to an association change ");
						try {
							Get.commitService().addUncommittedNoChecks(conceptLogicGraphSememeChronology.get()).get();
						} catch (RuntimeException | InterruptedException | ExecutionException e) {
							// New version of this sememe failed to be added to commit list, so remove sememe from list so listener won't ignore it
							nidsOfGeneratedSememesToIgnore.remove(conceptLogicGraphSememeChronology.get().getNid());
							LOG.error("FAILED calling addUncommitted() on logic graph of VHAT concept " + referencedConcept, e);
							return;
						}
						Get.commitService().commit("Committing new version of logic graph sememe " + conceptLogicGraphSememeChronology.get().getPrimordialUuid() 
								+ " with " + parentSequencesFromHasParentAssociationDynamicSememes.size() + " parent(s) for concept " 
								+ referencedConcept.getPrimordialUuid()).get();
					} catch (Exception e) {
						LOG.error("FAILED committing new version of logic graph sememe " + conceptLogicGraphSememeChronology.get().getPrimordialUuid() + " with " + parentSequencesFromHasParentAssociationDynamicSememes.size() + " parent(s) for concept " + referencedConcept.getPrimordialUuid(), e);
					}
				}
			};
			
			// Use either updateExistingLogicGraphRunnable or retireAndCreateLogicGraphRunnable,
			// depending on which (if either) works better with logic graph merge
			final Runnable runnableToUse = updateExistingLogicGraphRunnable;
			inProgressJobs.add(Get.workExecutors().getExecutor().submit(runnableToUse));
		}
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.modules.vhat.VHATIsAHasParentSynchronizingChronologyChangeListenerI#waitForJobsToComplete()
	 */
	@Override
	public void waitForJobsToComplete()
	{
		LOG.info("Waiting for " + inProgressJobs.size());
		Future<?> f = null;
		f = inProgressJobs.peek();
		while (f != null)
		{
			try
			{
				//wait for execution of the job to complete
				f.get();
			}
			catch (Exception e)
			{
				LOG.error("There was an error in a submitted job!", e);
			}
			inProgressJobs.remove(f);
			f = inProgressJobs.peek();
		}
		LOG.info("Wait complete");
	}

	private static Stream<SememeChronology<? extends SememeVersion<?>>> getSememesForComponentFromAssemblagesFilteredBySememeType(int nid) {
		final Set<Integer> selectedAssemblages = new HashSet<>();
		selectedAssemblages.add(VHATConstants.VHAT_HAS_PARENT_ASSOCIATION_TYPE.getConceptSequence());
		
		final Set<SememeType> sememeTypesToExclude = new HashSet<>();
		for (SememeType type : SememeType.values()) {
			if (type != SememeType.DYNAMIC) {
				sememeTypesToExclude.add(type);
			}
		}

		return Frills.getSememesForComponentFromAssemblagesFilteredBySememeType(nid, selectedAssemblages, sememeTypesToExclude);
	}

	public static Collection<DynamicSememeImpl> getActiveHasParentAssociationDynamicSememesAttachedToComponent(int nid) {
		final Iterator<SememeChronology<? extends SememeVersion<?>>> it = getSememesForComponentFromAssemblagesFilteredBySememeType(nid).iterator();
		final List<DynamicSememeImpl> hasParentAssociationDynamicSememesToReturn = new ArrayList<>();
		while (it.hasNext()) {
			@SuppressWarnings("unchecked")
			SememeChronology<DynamicSememeImpl> hasParentAssociationDynamicSememe = (SememeChronology<DynamicSememeImpl>)it.next();
			// Ensure only working with ACTIVE hasParentAssociationDynamicSememe version
			if (hasParentAssociationDynamicSememe.isLatestVersionActive(getVHATDevelopmentLatestStampCoordinate().makeAnalog(State.ACTIVE_ONLY_SET))) {
				Optional<LatestVersion<DynamicSememeImpl>> optionalLatestVersion =  hasParentAssociationDynamicSememe.getLatestVersion(DynamicSememeImpl.class, getVHATDevelopmentLatestStampCoordinate());
				if (optionalLatestVersion.isPresent()) {
					if (optionalLatestVersion.get().contradictions().isPresent()) {
						// TODO handle contradictions
					}

					// This check should be redundant
					if (optionalLatestVersion.get().value().getState() == State.ACTIVE) {
						hasParentAssociationDynamicSememesToReturn.add(optionalLatestVersion.get().value());
					}
				}
			}
		}
		
		return Collections.unmodifiableList(hasParentAssociationDynamicSememesToReturn);
	}
	
	// Get any (first) inactive VHAT has_parent association sememe with specified target parent concept
	public static Optional<DynamicSememeImpl> getInactiveHasParentAssociationDynamicSememeAttachedToComponent(int nid, int targetParentConceptId) {
		ConceptChronology<? extends ConceptVersion<?>> targetParentConcept = Get.conceptService().getConcept(targetParentConceptId);
		final Iterator<SememeChronology<? extends SememeVersion<?>>> it = getSememesForComponentFromAssemblagesFilteredBySememeType(nid).iterator();
		while (it.hasNext()) {
			@SuppressWarnings("unchecked")
			SememeChronology<DynamicSememeImpl> hasParentAssociationDynamicSememe = (SememeChronology<DynamicSememeImpl>)it.next();
			Optional<LatestVersion<DynamicSememeImpl>> optionalLatestVersion =  hasParentAssociationDynamicSememe.getLatestVersion(DynamicSememeImpl.class, getVHATDevelopmentLatestStampCoordinate());
			if (optionalLatestVersion.isPresent()) {
				if (optionalLatestVersion.get().contradictions().isPresent()) {
					// TODO handle contradictions
				}

				if (optionalLatestVersion.get().value().getState() == State.INACTIVE) {
					UUID inactiveHasParentSememeTargetParentUuid = ((DynamicSememeUUIDImpl)optionalLatestVersion.get().value().getData()[0]).getDataUUID();
					
					if (targetParentConcept.getPrimordialUuid().equals(inactiveHasParentSememeTargetParentUuid)) {
						return Optional.of(optionalLatestVersion.get().value());
					}
				}
			}
		}

		return Optional.empty();
	}
}

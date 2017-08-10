package gov.vha.isaac.ochre.mojo;

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.NecessarySet;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import com.cedarsoftware.util.io.JsonWriter;
import gov.vha.isaac.ochre.api.DataTarget;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IsaacTaxonomy;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.collections.SememeSequenceSet;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableLogicGraphSememe;
import gov.vha.isaac.ochre.api.externalizable.BinaryDataReaderQueueService;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizable;
import gov.vha.isaac.ochre.api.externalizable.StampAlias;
import gov.vha.isaac.ochre.api.externalizable.StampComment;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.logic.IsomorphicResults;
import gov.vha.isaac.ochre.api.logic.LogicNode;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.NodeSemantic;
import gov.vha.isaac.ochre.api.logic.assertions.Assertion;
import gov.vha.isaac.ochre.model.logic.node.AbstractLogicNode;
import gov.vha.isaac.ochre.model.logic.node.AndNode;
import gov.vha.isaac.ochre.model.logic.node.NecessarySetNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

/**
 * Goal which loads a database from eConcept files.
 */
@Mojo(name = "load-termstore", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)

public class LoadTermstore extends AbstractMojo
{
	/**
	 * The preferred mechanism for specifying ibdf files - provide a folder that contains IBDF files, all found IBDF files in this 
	 * folder will be processed.
	 */
	@Parameter(required = false) 
	private File ibdfFileFolder;
	public void setibdfFilesFolder(File folder)
	{
		ibdfFileFolder = folder;
	}
	
	/**
	 * The optional (old) way to specify ibdf files - requires each file to be listed one by one.
	 */
	@Parameter(required = false) 
	private File[] ibdfFiles;
	
	public void setibdfFiles(File[] files)
	{
		ibdfFiles = files;
	}

	@Parameter(required = false) 
	private boolean activeOnly = false;
	
	public void setActiveOnly(boolean activeOnly)
	{
		this.activeOnly = activeOnly;
	}
	
	private final HashSet<SememeType> sememeTypesToSkip = new HashSet<>();
	public void skipSememeTypes(Collection<SememeType> types )
	{
		sememeTypesToSkip.addAll(types);
	}
	
	private int conceptCount, sememeCount, stampAliasCount, stampCommentCount, itemCount, itemFailure, mergeCount;
	private final HashSet<Integer> skippedItems = new HashSet<>();
	private boolean skippedAny = false;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void execute() throws MojoExecutionException
	{
		Get.configurationService().setDBBuildMode();
		
		final int statedSequence = Get.identifierService().getConceptSequenceForUuids(TermAux.EL_PLUS_PLUS_STATED_ASSEMBLAGE.getPrimordialUuid());
		int statedDups = 0;
		//Load IsaacMetadataAuxiliary first, otherwise, we have issues....
		final AtomicBoolean hasMetadata = new AtomicBoolean(false);
		
		Set<File> mergedFiles;
		try
		{
			mergedFiles = new HashSet<>();
			if (ibdfFiles != null)
			{
				for (File f : ibdfFiles)
				{
					mergedFiles.add(f.getCanonicalFile());
				}
			}
			
			if (ibdfFileFolder != null)
			{
				if (!ibdfFileFolder.isDirectory())
				{
					throw new MojoExecutionException("If ibdfFileFolder is provided, it must point to a folder");
				}
				for (File f : ibdfFileFolder.listFiles())
				{
					if (!f.isFile())
					{
						getLog().info("The file " + f.getAbsolutePath() + " is not a file - ignoring.");
					}
					else if (!f.getName().toLowerCase(Locale.ENGLISH).endsWith(".ibdf"))
					{
						getLog().info("The file " + f.getAbsolutePath() + " does not match the expected type of ibdf - ignoring.");
					}
					else
					{
						mergedFiles.add(f);
					}
				}
			}
		}
		catch (IOException e1)
		{
			throw new MojoExecutionException("Problem reading ibdf files", e1);
		}
		
		File[] temp = mergedFiles.toArray(new File[mergedFiles.size()]);
		
		Arrays.sort(temp, new Comparator<File>()
		{
			@Override
			public int compare(File o1, File o2)
			{
				if (o1.getName().equals("IsaacMetadataAuxiliary.ibdf"))
				{
					hasMetadata.set(true);
					return -1;
				}
				else if (o2.getName().equals("IsaacMetadataAuxiliary.ibdf"))
				{
					hasMetadata.set(true);
					return 1;
				}
				else
				{
					return ((o1.length() - o2.length()) > 0 ? 1 :((o1.length() - o2.length()) < 0 ? -1 : 0)); 
				}
			}
		});
		
		if (temp.length == 1 && temp[0].getName().equals("IsaacMetadataAuxiliary.ibdf"))
		{
			hasMetadata.set(true);
		}
		
		if (!hasMetadata.get())
		{
			getLog().warn("No Metadata IBDF file found!  This probably isn't good....");
		}
		
		if (temp.length == 0)
		{
			throw new MojoExecutionException("Failed to find any ibdf files to load");
		}
		
		getLog().info("Identified " + temp.length + " ibdf files");
		
		Set<Integer> deferredActionNids = new HashSet<>();
		try
		{
			for (File f : temp)
			{
				getLog().info("Loading termstore from " + f.getCanonicalPath() + (activeOnly ? " active items only" : ""));
				BinaryDataReaderQueueService reader = Get.binaryDataQueueReader(f.toPath());
				
				BlockingQueue<OchreExternalizable> queue = reader.getQueue();
				
				while (!queue.isEmpty() || !reader.isFinished())
				{
					OchreExternalizable object = queue.poll(500, TimeUnit.MILLISECONDS);
					if (object != null)
					{
						itemCount++;
						try
						{
							if (null != object.getOchreObjectType())
							switch (object.getOchreObjectType()) {
								case CONCEPT:
									if (!activeOnly || isActive((ObjectChronology)object))
									{
										Get.conceptService().writeConcept(((ConceptChronology)object));
										conceptCount++;
									}
									else
									{
										skippedItems.add(((ObjectChronology)object).getNid());
									}
									break;
								case SEMEME:
									SememeChronology sc = (SememeChronology)object;
									if (sc.getAssemblageSequence() == statedSequence) {
										SememeSequenceSet sequences = Get.sememeService().getSememeSequencesForComponentFromAssemblage(sc.getReferencedComponentNid(), statedSequence);
										if (!sequences.isEmpty()) {
											List<LogicalExpression> listToMerge = new ArrayList<>();
											listToMerge.add(getLatestLogicalExpression(sc));
											getLog().debug("\nDuplicate: " + sc);
											sequences.stream().forEach((sememeSequence) ->  listToMerge.add(getLatestLogicalExpression(Get.sememeService().getSememe(sememeSequence))));
											
											getLog().debug("Duplicates: " + listToMerge);
											
											if (listToMerge.size() > 2) {
												throw new UnsupportedOperationException("Can't merge list of size: " + listToMerge.size() + "\n" + listToMerge);
											}
											
											Set<Integer> mergedParents = new HashSet<>();
											for (LogicalExpression le : listToMerge)
											{
												mergedParents.addAll(getParentConceptSequencesFromLogicExpression(le));
											}
											
											byte[][] data;
											
											if (mergedParents.size() == 0)
											{
												//The logic graph is too complex for our stupid merger - Use the isomorphic one.
												IsomorphicResults isomorphicResults = listToMerge.get(0).findIsomorphisms(listToMerge.get(1));
												getLog().debug("Isomorphic results: " + isomorphicResults);
												data = isomorphicResults.getMergedExpression().getData(DataTarget.INTERNAL);
											}
											else
											{
												//Use our stupid merger to just merge parents, cause the above merge isn't really designed to handle ibdf 
												//import merges - especially in metadata where we keep adding additional parents one ibdf file at a time.
												//Note, this hack won't work at all to merge more complex logic graphs.  Probably won't work for RF2 content.
												//But for IBDF files, which are just adding extra parents, this avoids a bunch of issues with the logic graphs.
												Assertion[] assertions = new Assertion[mergedParents.size()];
												LogicalExpressionBuilder leb = Get.logicalExpressionBuilderService().getLogicalExpressionBuilder();
												int i = 0;
												for (Integer parent : mergedParents) {
													assertions[i++] = ConceptAssertion(parent, leb);
												}
												
												NecessarySet(And(assertions));
												data = leb.build().getData(DataTarget.INTERNAL);
											}
											
											mergeCount++;
											
											SememeChronology existingChronology = Get.sememeService().getSememe(sequences.findFirst().getAsInt());
											
											int stampSequence = Get.stampService().getStampSequence(State.ACTIVE, System.currentTimeMillis(), TermAux.USER.getConceptSequence(), 
													TermAux.ISAAC_MODULE.getConceptSequence(), TermAux.DEVELOPMENT_PATH.getConceptSequence());
											MutableLogicGraphSememe newVersion = (MutableLogicGraphSememe) existingChronology
													.createMutableVersion(MutableLogicGraphSememe.class, stampSequence);
											newVersion.setGraphData(data);
											
//											TODO mess - this isn't merging properly - how should we merge - I think this issue referrs to UUIDs... ?
//											for (UUID uuid : sc.getUuidList())
//											{
//												Get.identifierService().addUuidForNid(uuid, newVersion.getNid());
//											}
											sc = existingChronology;
										}
									}
									if (!sememeTypesToSkip.contains(sc.getSememeType()) &&
										(!activeOnly || (isActive(sc) && !skippedItems.contains(sc.getReferencedComponentNid()))))
									{
										Get.sememeService().writeSememe(sc);
										if (sc.getSememeType() == SememeType.LOGIC_GRAPH)
										{
											deferredActionNids.add(sc.getNid());
										}
										sememeCount++;
									}
									else
									{
										skippedItems.add(sc.getNid());
									}
									break;
								case STAMP_ALIAS:
									Get.commitService().addAlias(((StampAlias)object).getStampSequence(), ((StampAlias)object).getStampAlias(), null);
									stampAliasCount++;
									break;
								case STAMP_COMMENT:
									Get.commitService().setComment(((StampComment)object).getStampSequence(), ((StampComment)object).getComment());
									stampCommentCount++;
									break;
								default:
									throw new UnsupportedOperationException("Unknown ochre object type: " + object);
							}
						}
						catch (Exception e)
						{
							itemFailure++;
							getLog().error("Failure at " + conceptCount + " concepts, " + sememeCount + " sememes, " + stampAliasCount + " stampAlias, " 
									+ stampCommentCount + " stampComments", e);
							
							Map<String, Object> args = new HashMap<>();
							args.put(JsonWriter.PRETTY_PRINT, true);
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							JsonWriter json = new JsonWriter(baos, args);
							
							UUID primordial = null;
							if (object instanceof ObjectChronology)
							{
								primordial = ((ObjectChronology)object).getPrimordialUuid();
							}
							
							json.write(object);
							getLog().error("Failed on " + (primordial == null ? ": " : "object with primoridial UUID " + primordial.toString() + ": ") +  baos.toString());
							json.close();
							
						}
						
						if (itemCount % 50000 == 0)
						{
							getLog().info("Read " + itemCount + " entries, " + "Loaded " + conceptCount + " concepts, " + sememeCount + " sememes, " + stampAliasCount + " stampAlias, " 
									+ stampCommentCount + " stampComment");
						}
					}
				};
				
				if (skippedItems.size() > 0)
				{
					skippedAny = true;
				}
				
				getLog().info("Loaded " + conceptCount + " concepts, " + sememeCount + " sememes, " + stampAliasCount + " stampAlias, " 
						+ stampCommentCount + " stampComments, " + mergeCount + " merged sememes" + (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : "") 
						+ (itemFailure > 0 ? " Failures " + itemFailure : "") + " from file " + f.getName());
				conceptCount = 0;
				sememeCount = 0;
				stampAliasCount = 0;
				stampCommentCount = 0;
				skippedItems.clear();
			}
			
			getLog().info("Completing processing on " + deferredActionNids.size() + " defered items");
			for (int nid : deferredActionNids)
			{
				if (ObjectChronologyType.SEMEME.equals(Get.identifierService().getChronologyTypeForNid(nid)))
				{
					SememeChronology sc = Get.sememeService().getSememe(nid);
					if (sc.getSememeType() == SememeType.LOGIC_GRAPH)
					{
						try
						{
							Get.taxonomyService().updateTaxonomy(sc);
						}
						catch (Exception e)
						{
							Map<String, Object> args = new HashMap<>();
							args.put(JsonWriter.PRETTY_PRINT, true);
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							JsonWriter json = new JsonWriter(baos, args);
							
							UUID primordial = sc.getPrimordialUuid();
							json.write(sc);
							getLog().error("Failed on taxonomy update for object with primoridial UUID " + primordial.toString() + ": " +  baos.toString());
							json.close();
						}
					}
					else
					{
						throw new UnsupportedOperationException("Unexpected nid in deferred set: " + nid);
					}
				}
				else
				{
					throw new UnsupportedOperationException("Unexpected nid in deferred set: " + nid);
				}
			}
			
			if (skippedAny)
			{
				//Loading with activeOnly set to true causes a number of gaps in the concept / sememe providers
				Get.identifierService().clearUnusedIds();
			}
		}
		catch (Exception ex)
		{
			getLog().info("Loaded " + conceptCount + " concepts, " + sememeCount + " sememes, " + stampAliasCount + " stampAlias, " 
					+ stampCommentCount + " stampComments" + (skippedItems.size() > 0 ? ", skipped for inactive " + skippedItems.size() : ""));
			throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
		}
	}
	
	private boolean isActive(ObjectChronology<?> object)
	{
		if (object.getVersionList().size() != 1)
		{
			throw new RuntimeException("Didn't expect version list of size " + object.getVersionList());
		}
		else
		{
			return ((StampedVersion)object.getVersionList().get(0)).getState() == State.ACTIVE;
		}
	}

	private static LogicalExpression getLatestLogicalExpression(SememeChronology sc) {
		SememeChronology<? extends LogicGraphSememe> lgsc =  sc;
		LogicGraphSememe latestVersion = null;
		for (LogicGraphSememe version: lgsc.getVersionList()) {
			if (latestVersion == null) {
				latestVersion = version;
			} else if (latestVersion.getTime() < version.getTime()) {
					latestVersion = version;
			}
		}
		return (latestVersion != null) ? latestVersion.getLogicalExpression() : null ;
	}
	
	/**
	 * Shamelessly copied from FRILLS, as I can't use it there, due to dependency chain issues.  But then modified a bit, 
	 * so it fails if it encounters things it can't handle.
	 */
	private Set<Integer> getParentConceptSequencesFromLogicExpression(LogicalExpression logicExpression) {
		Set<Integer> parentConceptSequences = new HashSet<>();
		Stream<LogicNode> isAs = logicExpression.getNodesOfType(NodeSemantic.NECESSARY_SET);
		int necessaryCount = 0;
		int allCount = 1;  //start at 1, for root.
		for (Iterator<LogicNode> necessarySetsIterator = isAs.distinct().iterator(); necessarySetsIterator.hasNext();) {
			necessaryCount++;
			allCount++;
			NecessarySetNode necessarySetNode = (NecessarySetNode)necessarySetsIterator.next();
			for (AbstractLogicNode childOfNecessarySetNode : necessarySetNode.getChildren()) {
				allCount++;
				if (childOfNecessarySetNode.getNodeSemantic() == NodeSemantic.AND) {
					AndNode andNode = (AndNode)childOfNecessarySetNode;
					for (AbstractLogicNode childOfAndNode : andNode.getChildren()) {
						allCount++;
						if (childOfAndNode.getNodeSemantic() == NodeSemantic.CONCEPT) {
							if (childOfAndNode instanceof ConceptNodeWithSequences) {
								ConceptNodeWithSequences conceptNode = (ConceptNodeWithSequences)childOfAndNode;
								parentConceptSequences.add(conceptNode.getConceptSequence());
							} else if (childOfAndNode instanceof ConceptNodeWithUuids) {
								ConceptNodeWithUuids conceptNode = (ConceptNodeWithUuids)childOfAndNode;
								parentConceptSequences.add(Get.identifierService().getConceptSequenceForUuids(conceptNode.getConceptUuid()));
							} else {
								// Should never happen - return an empty set to our call above doesn't use this mechanism
								return new HashSet<>();
							}
						}
					}
				} else if (childOfNecessarySetNode.getNodeSemantic() == NodeSemantic.CONCEPT) {
					if (childOfNecessarySetNode instanceof ConceptNodeWithSequences) {
						ConceptNodeWithSequences conceptNode = (ConceptNodeWithSequences)childOfNecessarySetNode;
						parentConceptSequences.add(conceptNode.getConceptSequence());
					} else if (childOfNecessarySetNode instanceof ConceptNodeWithUuids) {
						ConceptNodeWithUuids conceptNode = (ConceptNodeWithUuids)childOfNecessarySetNode;
						parentConceptSequences.add(Get.identifierService().getConceptSequenceForUuids(conceptNode.getConceptUuid()));
					} else {
						// Should never happen - return an empty set to our call above doesn't use this mechanism
						return new HashSet<>();
					}
				} else {
					// we don't understand this log graph.  Return an empty set to our call above doesn't use this mechanism
					return new HashSet<>();
				}
			}
		}
		
		if (logicExpression.getRoot().getChildren().length != necessaryCount || allCount != logicExpression.getNodeCount()) {
			// we don't understand this log graph.  Return an empty set to our call above doesn't use this mechanism
			return new HashSet<>();
		}
		
		return parentConceptSequences;
	}
}

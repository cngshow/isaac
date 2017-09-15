package gov.vha.isaac.ochre.impl.utility;

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.NecessarySet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.bootstrap.TermAux;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.collections.LruCache;
import gov.vha.isaac.ochre.api.collections.SememeSequenceSet;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.Stamp;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilder;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilderService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptService;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptSpecification;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnUtility;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUtility;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.LogicCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.externalizable.OchreExternalizableObjectType;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.api.index.SememeIndexerBI;
import gov.vha.isaac.ochre.api.logic.LogicNode;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.api.logic.NodeSemantic;
import gov.vha.isaac.ochre.api.logic.assertions.Assertion;
import gov.vha.isaac.ochre.api.util.NumericUtils;
import gov.vha.isaac.ochre.api.util.TaskCompleteCallback;
import gov.vha.isaac.ochre.api.util.UUIDUtil;
import gov.vha.isaac.ochre.mapping.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.concept.ConceptVersionImpl;
import gov.vha.isaac.ochre.model.configuration.EditCoordinates;
import gov.vha.isaac.ochre.model.configuration.LanguageCoordinates;
import gov.vha.isaac.ochre.model.configuration.LogicCoordinates;
import gov.vha.isaac.ochre.model.configuration.StampCoordinates;
import gov.vha.isaac.ochre.model.configuration.TaxonomyCoordinates;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;
import gov.vha.isaac.ochre.model.logic.node.AbstractLogicNode;
import gov.vha.isaac.ochre.model.logic.node.AndNode;
import gov.vha.isaac.ochre.model.logic.node.NecessarySetNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;
import gov.vha.isaac.ochre.model.relationship.RelationshipVersionAdaptorImpl;
import gov.vha.isaac.ochre.model.sememe.DynamicSememeUsageDescriptionImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUIDImpl;
import gov.vha.isaac.ochre.model.sememe.version.ComponentNidSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.DescriptionSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.DynamicSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.LogicGraphSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.LongSememeImpl;
import gov.vha.isaac.ochre.model.sememe.version.StringSememeImpl;

//This is a service, simply to implement the DynamicSememeColumnUtility interface.  Everything else is static, and may be used directly
@Service  
@Singleton
public class Frills implements DynamicSememeColumnUtility {

	private static Logger log = LogManager.getLogger(Frills.class);
	
	private static LruCache<Integer, Boolean> isAssociationCache = new LruCache<>(50);
	private static LruCache<Integer, Boolean> isMappingCache = new LruCache<>(50);
	private static LruCache<Integer, Integer> moduleToTermTypeCache = new LruCache<>(50);
	/**
	 * @param version StampedVersion from which to generate StampCoordinate
	 * @return StampCoordinate corresponding to StampedVersion values
	 * 
	 * StampPrecedence set to StampPrecedence.TIME
	 * 
	 * Use StampCoordinate.makeAnalog() to customize result
	 */
	public static StampCoordinate getStampCoordinateFromVersion(StampedVersion version, StampPrecedence precedence) {
		StampPosition stampPosition = new StampPositionImpl(version.getTime(), version.getPathSequence());
		StampCoordinate stampCoordinate = new StampCoordinateImpl(
				precedence,
				stampPosition,
				ConceptSequenceSet.of(version.getModuleSequence()),
				EnumSet.of(version.getState()));
				
		log.debug("Created StampCoordinate from StampedVersion: " + toString(version) + ": " + stampCoordinate);

		return stampCoordinate;
	}
	/**
	 * @param version StampedVersion from which to generate StampCoordinate
	 * @return StampCoordinate corresponding to StampedVersion values
	 * 
	 * StampPrecedence set to StampPrecedence.TIME
	 * 
	 * Use StampCoordinate.makeAnalog() to customize result
	 */
	public static StampCoordinate getStampCoordinateFromVersion(StampedVersion version) {
		return getStampCoordinateFromVersion(version, StampPrecedence.TIME);
	}

	/**
	 * @param version toString for StampedVersion
	 * @return
	 */
	public static String toString(StampedVersion version) {
		return version.getClass().getSimpleName()
				+ " STAMP=" + version.getStampSequence() + "{state=" + version.getState()
				+ ", time=" + version.getTime() + ", author=" + version.getAuthorSequence() + ", module="
				+ version.getModuleSequence() + ", path=" + version.getPathSequence() + "}";
	}

	/**
	 * @param stamp Stamp from which to generate StampCoordinate
	 * @param precedence Precedence to assign StampCoordinate
	 * @return StampCoordinate corresponding to Stamp values
	 * 
	 * Use StampCoordinate.makeAnalog() to customize result
	 */
	public static StampCoordinate getStampCoordinateFromStamp(Stamp stamp, StampPrecedence precedence) {
		StampPosition stampPosition = new StampPositionImpl(stamp.getTime(), stamp.getPathSequence());
		StampCoordinate stampCoordinate = new StampCoordinateImpl(
				precedence,
				stampPosition,
				ConceptSequenceSet.of(stamp.getModuleSequence()),
				EnumSet.of(stamp.getStatus()));
				
		log.debug("Created StampCoordinate from Stamp: " + stamp + ": " + stampCoordinate);

		return stampCoordinate;
	}

	/**
	 * @param stamp Stamp from which to generate StampCoordinate
	 * @return StampCoordinate corresponding to Stamp values
	 * 
	 * StampPrecedence set to StampPrecedence.TIME
	 * 
	 * Use StampCoordinate.makeAnalog() to customize result
	 */
	public static StampCoordinate getStampCoordinateFromStamp(Stamp stamp) {
		return getStampCoordinateFromStamp(stamp, StampPrecedence.TIME);
	}
	
	/**
	 * Construct a stamp coordinate from an existing stamp coordinate, and the path from the edit coordinate, ensuring that the returned 
	 * stamp coordinate includes the module edit coordinate.
	 * 
	 * @param stampCoordinate - optional - used to fill in the stamp details not available from the edit coordinate.  If not provided, 
	 * uses the system defaults.
	 * @param editCoordinate - ensure that the returned stamp coordinate includes the module and path from this edit coordinate.
	 * @return a new stamp coordinate
	 */
	public static StampCoordinate getStampCoordinateFromEditCoordinate(StampCoordinate stampCoordinate, EditCoordinate editCoordinate)
	{
		if (stampCoordinate == null)
		{
			stampCoordinate = Get.configurationService().getDefaultStampCoordinate();
		}
		
		StampPosition stampPosition = new StampPositionImpl(stampCoordinate.getStampPosition().getTime(), editCoordinate.getPathSequence());
		
		StampCoordinateImpl temp = new StampCoordinateImpl(stampCoordinate.getStampPrecedence(), stampPosition,
				stampCoordinate.getModuleSequences(), stampCoordinate.getAllowedStates());
		
		if (temp.getModuleSequences().size() > 0)
		{
			temp.getModuleSequences().add(editCoordinate.getModuleSequence());
		}
		return temp;
	}
	
	/**
	 * 
	 * {@link IdInfo}
	 *
	 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
	 *
	 * Class to contain and hide map generated by getIdInfo(). Only useful method is toString(). The returned String is not meant to be parsed.
	 */
	private final static class IdInfo {
		private final Map<String, Object> map_;
		private IdInfo(Map<String, Object> map) { map_ = map; }
		
		public String toString() { return map_.toString(); }
	};

	/**
	 * @param id int identifier
	 * calls {@link #getIdInfo(String)}
	 * @return a IdInfo, the toString() for which will display known identifiers and descriptions associated with the passed id
	 * 
	 * This method should only be used for logging. The returned data structure is not meant to be parsed.
	 */
	private static IdInfo getIdInfo(int id) {
		return getIdInfo(Integer.toString(id));
	}
	
	/**
	 * @param id int identifier
	 * calls {@link #getIdInfo(String, StampCoordinate, LanguageCoordinate)
	 * @return a IdInfo, the toString() for which will display known identifiers and descriptions associated with the passed id
	 * 
	 * This method should only be used for logging. The returned data structure is not meant to be parsed.
	 */
	private static IdInfo getIdInfo(int id, StampCoordinate sc, LanguageCoordinate lc) {
		return getIdInfo(Integer.toString(id), sc, lc);
	}
	/**
	 * @param id String identifier may parse to int NID, int sequence or UUID
	 * 
	 * Calls {@link #getIdInfo(String, StampCoordinate, LanguageCoordinate)} with development latest and US English FSN
	 * @return a IdInfo, the toString() for which will display known identifiers and descriptions associated with the passed id
	 * 
	 * This method should only be used for logging. The returned data structure is not meant to be parsed.
	 */
	private static IdInfo getIdInfo(String id) {
		return getIdInfo(id, StampCoordinates.getDevelopmentLatest(), LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate());
	}
	
	/**
	 * 
	 * @param id String identifier may parse to int NID, int sequence or UUID
	 * @param sc The stamp coordinate to use, when looking up descriptions - uses dev latest if not passed
	 * @param lc the language coordinate to use, when looking up descriptions.  Uses us english, if not provided.
	 * @return a IdInfo, the toString() for which will display known identifiers and descriptions associated with the passed id
	 * 
	 * This method should only be used for logging. The returned data structure is not meant to be parsed.
	 */
	private static IdInfo getIdInfo(String id, StampCoordinate sc, LanguageCoordinate lc) {
		Map<String, Object> idInfo = new HashMap<>();

		Long sctId = null;
		Integer seq = null;
		Integer nid = null;
		UUID[] uuids = null;
		ObjectChronologyType typeOfPassedId = null;
		
		if (sc == null)
		{
			sc = StampCoordinates.getDevelopmentLatest();
		}
		
		if (lc == null)
		{
			lc = LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate();
		}

		try {
			Optional<Integer> intId = NumericUtils.getInt(id);
			if (intId.isPresent())
			{
				// id interpreted as the id of the referenced component
				if (intId.get() > 0) {
					seq = intId.get();
					nid = Get.identifierService().getConceptNid(seq);
				} else if (intId.get() < 0) {
					nid = intId.get();
					seq = Get.identifierService().getConceptSequence(intId.get());
				}

				if (nid != null) {
					typeOfPassedId = Get.identifierService().getChronologyTypeForNid(nid);
					uuids = Get.identifierService().getUuidArrayForNid(nid);
				}
			}
			else
			{
				Optional<UUID> uuidId = UUIDUtil.getUUID(id);
				if (uuidId.isPresent())
				{
					// id interpreted as the id of either a sememe or a concept
					nid = Get.identifierService().getNidForUuids(uuidId.get());
					typeOfPassedId = Get.identifierService().getChronologyTypeForNid(nid);

					switch (typeOfPassedId) {
					case CONCEPT: {
						seq = Get.identifierService().getConceptSequenceForUuids(uuidId.get());
						break;
					}
					case SEMEME: {
						seq = Get.identifierService().getSememeSequenceForUuids(uuidId.get());
						break;
					}
					case UNKNOWN_NID:
					default:
					}
				}
			}

			if (nid != null) {
				idInfo.put("DESC", Get.conceptService().getSnapshot(sc, lc).conceptDescriptionText(nid));
				if (typeOfPassedId == ObjectChronologyType.CONCEPT) {
					Optional<Long> optSctId = Frills.getSctId(nid, sc);
					if (optSctId.isPresent()) {
						sctId = optSctId.get();
						
						idInfo.put("SCTID", sctId);
					}
				}
			}
		} catch (Exception e) {
			log.warn("Problem getting idInfo for \"{}\". Caught {}", e.getClass().getName(), e.getLocalizedMessage());
		}
		idInfo.put("PASSED_ID", id);
		idInfo.put("SEQ", seq);
		idInfo.put("NID", nid);
		idInfo.put("UUIDs", Arrays.toString(uuids));
		idInfo.put("TYPE", typeOfPassedId);

		return new IdInfo(idInfo);
	}
	/**
	 * @param lgs The LogicGraphSememe containing the logic graph data
	 * @return true if the corresponding concept is fully defined, otherwise returns false (for primitive concepts)
	 * 
	 * Things that are defined with at least one SUFFICIENT_SET node are defined.
	 * Things that are defined without any SUFFICIENT_SET nodes are primitive.
	 */
	public static <T extends LogicGraphSememe<T>> boolean isConceptFullyDefined(LogicGraphSememe<T> lgs) {
		return lgs.getLogicalExpression().contains(NodeSemantic.SUFFICIENT_SET);
	}
	
	/**
	 * Return true for fully defined, false for primitive, or empty for unknown, on the standard logic coordinates / standard development path
	 * @param conceptNid
	 * @param stated
	 * @return
	 */
	public static Optional<Boolean> isConceptFullyDefined(int conceptNid, boolean stated)
	{
		Optional<SememeChronology<? extends SememeVersion<?>>> sememe = Get.sememeService().getSememesForComponentFromAssemblage(conceptNid, 
				(stated ? 
						LogicCoordinates.getStandardElProfile().getStatedAssemblageSequence() :
							LogicCoordinates.getStandardElProfile().getInferredAssemblageSequence())).findAny();

		if (sememe.isPresent())
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<LogicGraphSememe>> sv = ((SememeChronology)sememe.get()).getLatestVersion(LogicGraphSememe.class, StampCoordinates.getDevelopmentLatest());
			if (sv.isPresent())
			{
				return Optional.of(isConceptFullyDefined((LogicGraphSememe<?>)sv.get().value()));
			}
		}
		return Optional.empty();
	}

	/**
	*
	* @param conceptId either a concept nid or sequence.
	* @param logicCoordinate LogicCoordinate.
	* @return the stated definition chronology for the specified concept
	* according to the default logic coordinate.
	*/
	public static Optional<SememeChronology<? extends SememeVersion<?>>> getStatedDefinitionChronology(int conceptId, LogicCoordinate logicCoordinate) {
		conceptId = Get.identifierService().getConceptNid(conceptId);
		return Get.sememeService().getSememesForComponentFromAssemblage(conceptId, logicCoordinate.getStatedAssemblageSequence()).findAny();
	}

	/**
	*
	* @param conceptId either a concept nid or sequence.
	* @param logicCoordinate LogicCoordinate.
	* @return the inferred definition chronology for the specified concept
	* according to the default logic coordinate.
	*/
	public static Optional<SememeChronology<? extends SememeVersion<?>>> getInferredDefinitionChronology(int conceptId, LogicCoordinate logicCoordinate) {
		conceptId = Get.identifierService().getConceptNid(conceptId);
		return Get.sememeService().getSememesForComponentFromAssemblage(conceptId, logicCoordinate.getInferredAssemblageSequence()).findAny();
	}
	
	/**
	 * @param id The int sequence or NID of the Concept for which the logic graph is requested
	 * @param stampCoordinate The StampCoordinate for which the logic graph is requested
	 * @param languageCoordinate The LanguageCoordinate for which the logic graph is requested
	 * @param logicCoordinate the LogicCoordinate for which the logic graph is requested
	 * @param stated boolean indicating stated vs inferred definition chronology should be used
	 * @return An Optional containing a LogicGraphSememe SememeChronology
	 */
	public static Optional<SememeChronology<? extends LogicGraphSememe<?>>> getLogicGraphChronology(int id, boolean stated, StampCoordinate stampCoordinate, 
			LanguageCoordinate languageCoordinate, LogicCoordinate logicCoordinate)
	{
		log.debug("Getting {} logic graph chronology for {}", (stated ? "stated" : "inferred"), Optional.ofNullable(Frills.getIdInfo(id, stampCoordinate, languageCoordinate)));
		Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = stated ? getStatedDefinitionChronology(id, logicCoordinate) : getInferredDefinitionChronology(id, logicCoordinate);
		if (defChronologyOptional.isPresent())
		{
			log.debug("Got {} logic graph chronology for {}", (stated ? "stated" : "inferred"), Optional.ofNullable(Frills.getIdInfo(id, stampCoordinate, languageCoordinate)));

			@SuppressWarnings("unchecked")
			SememeChronology<? extends LogicGraphSememe<?>> sememeChronology = (SememeChronology<? extends LogicGraphSememe<?>>)defChronologyOptional.get();

			return Optional.of(sememeChronology);
		} else {
			log.warn("NO {} logic graph chronology for {}", (stated ? "stated" : "inferred"), Optional.ofNullable(Frills.getIdInfo(id, stampCoordinate, languageCoordinate)));

			return Optional.empty();
		}
	}
	
	/**
	 * @param id The int sequence or NID of the Concept for which the logic graph is requested
	 * @param stated boolean indicating stated vs inferred definition chronology should be used
	 * @return An Optional containing a LogicGraphSememe SememeChronology
	 */
	public static Optional<SememeChronology<? extends LogicGraphSememe<?>>> getLogicGraphChronology(int id, boolean stated)
	{
		log.debug("Getting {} logic graph chronology for {}", (stated ? "stated" : "inferred"), Optional.ofNullable(Frills.getIdInfo(id)));
		Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = stated ? Get.statedDefinitionChronology(id) : Get.inferredDefinitionChronology(id);
		if (defChronologyOptional.isPresent())
		{
			log.debug("Got {} logic graph chronology for {}", (stated ? "stated" : "inferred"), Optional.ofNullable(Frills.getIdInfo(id)));

			@SuppressWarnings("unchecked")
			SememeChronology<? extends LogicGraphSememe<?>> sememeChronology = (SememeChronology<? extends LogicGraphSememe<?>>)defChronologyOptional.get();

			return Optional.of(sememeChronology);
		} else {
			log.warn("NO {} logic graph chronology for {}", (stated ? "stated" : "inferred"), Optional.ofNullable(Frills.getIdInfo(id)));

			return Optional.empty();
		}
	}
	/**
	 * @param logicGraphSememeChronology The SememeChronology<? extends LogicGraphSememe<?>> chronology for which the logic graph version is requested
	 * @param stampCoordinate StampCoordinate to be used for selecting latest version
	 * @return An Optional containing a LogicGraphSememe SememeChronology
	 */
	public static Optional<LatestVersion<LogicGraphSememe<?>>> getLogicGraphVersion(SememeChronology<? extends LogicGraphSememe<?>> logicGraphSememeChronology, StampCoordinate stampCoordinate)
	{
		log.debug("Getting logic graph sememe for {}", Optional.ofNullable(Frills.getIdInfo(logicGraphSememeChronology.getReferencedComponentNid())));

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<LatestVersion<LogicGraphSememe<?>>> latest = ((SememeChronology)logicGraphSememeChronology).getLatestVersion(LogicGraphSememe.class, stampCoordinate);
		if (latest.isPresent()) {
			log.debug("Got logic graph sememe for {}", Optional.ofNullable(Frills.getIdInfo(logicGraphSememeChronology.getReferencedComponentNid())));
		} else {
			log.warn("NO logic graph sememe for {}", Optional.ofNullable(Frills.getIdInfo(logicGraphSememeChronology.getReferencedComponentNid())));
		}
		return latest;
	}
	/**
	 *
	 * Determine if Chronology has nested sememes
	 *
	 * @param chronology
	 * @return true if there is a nested sememe, false otherwise
	 */
	public static boolean hasNestedSememe(ObjectChronology<?> chronology) {
		return !chronology.getSememeList().isEmpty();
	}

	/**
	 * Find the SCTID for a component (if it has one)
	 *
	 * @param componentNid
	 * @param stamp - optional - if not provided uses default from config
	 * service
	 * @return the id, if found, or empty (will not return null)
	 */
	public static Optional<Long> getSctId(int componentNid, StampCoordinate stamp) {
		try {
			Optional<LatestVersion<StringSememeImpl>> sememe = Get.sememeService().getSnapshot(StringSememeImpl.class,
					stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp)
					.getLatestSememeVersionsForComponentFromAssemblage(componentNid,
							MetaData.SCTID.getConceptSequence()).findFirst();
			if (sememe.isPresent()) {
				return Optional.of(Long.parseLong(sememe.get().value().getString()));
			}
		} catch (Exception e) {
			log.error("Unexpected error trying to find SCTID for nid " + componentNid, e);
		}
		return Optional.empty();
	}

	
	/**
	 * Find the VUID for a component (if it has one) {@link MetaData#VUID}
	 *
	 * @param componentNid
	 * @param stamp - optional - if not provided uses default from config
	 * service
	 * @return the id, if found, or empty (will not return null)
	 */
	@SuppressWarnings("rawtypes")
	public static Optional<Long> getVuId(int componentNid, StampCoordinate stamp) {
		try 
		{
			ArrayList<Long> vuids = new ArrayList<>(1);
			Get.sememeService().getSnapshot(SememeVersion.class,
					stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp)
					.getLatestSememeVersionsForComponentFromAssemblage(componentNid,
							MetaData.VUID.getConceptSequence()).forEach(latestSememe ->
							{
								//expected path
								if (latestSememe.value().getChronology().getSememeType() == SememeType.STRING)
								{
									vuids.add(Long.parseLong(((StringSememe)latestSememe.value()).getString()));
								}
								//Data model bug path (can go away, after bug is fixed)
								else if (latestSememe.value().getChronology().getSememeType() == SememeType.DYNAMIC)
								{
									vuids.add(Long.parseLong(((DynamicSememe)latestSememe.value()).getData()[0].dataToString()));
								}
							});
			if (vuids.size() > 1)
			{
				log.warn("Found multiple VUIDs on component " + Get.identifierService().getUuidPrimordialForNid(componentNid) 
						+ " only the first value of '" + vuids.get(0) + "' will be returned");
			}
			if (vuids.size() > 0)
			{
				return Optional.of(vuids.get(0));
			}
		}
		catch (Exception e) 
		{
			log.error("Unexpected error trying to find VUID for nid " + componentNid, e);
		}
		return Optional.empty();
	}
	
	/**
	 * Find the CODE(s) for a component (if it has one) {@link MetaData#CODE}
	 *
	 * @param componentNid
	 * @param stamp - optional - if not provided uses default from config
	 * service
	 * @return the codes, if found, or empty (will not return null)
	 */
	@SuppressWarnings("rawtypes")
	public static List<String> getCodes(int componentNid, StampCoordinate stamp) {
		try 
		{
			ArrayList<String> codes = new ArrayList<>(1);
			Get.sememeService().getSnapshot(SememeVersion.class,
					stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp)
					.getLatestSememeVersionsForComponentFromAssemblage(componentNid,
							MetaData.CODE.getConceptSequence()).forEach(latestSememe ->
							{
								//expected path
								if (latestSememe.value().getChronology().getSememeType() == SememeType.STRING)
								{
									codes.add(((StringSememe)latestSememe.value()).getString());
								}
								//Data model bug path (can go away, after bug is fixed)
								else if (latestSememe.value().getChronology().getSememeType() == SememeType.DYNAMIC)
								{
									codes.add(((DynamicSememe)latestSememe.value()).getData()[0].dataToString());
								}
							});
			return codes;
		}
		catch (Exception e) 
		{
			log.error("Unexpected error trying to find CODE for nid " + componentNid, e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Find the VUID for a component (if it has one)
	 *
	 * @param componentNid
	 * @return the id, if found, or empty (will not return null)
	 */
	public static Optional<Long> getVuId(int componentNid) {
		return getVuId(componentNid, null);
	}
	

	/**
	 * Determine if a particular description sememe is flagged as preferred IN
	 * ANY LANGUAGE. Returns false if there is no acceptability sememe.
	 *
	 * @param descriptionSememeNid
	 * @param stamp - optional - if not provided, uses default from config
	 * service
	 * @throws RuntimeException If there is unexpected data (incorrectly)
	 * attached to the sememe
	 */
	public static boolean isDescriptionPreferred(int descriptionSememeNid, StampCoordinate stamp) throws RuntimeException {
		AtomicReference<Boolean> answer = new AtomicReference<>();

		//Ignore the language annotation... treat preferred in any language as good enough for our purpose here...
		Get.sememeService().getSememesForComponent(descriptionSememeNid).forEach(nestedSememe
				-> {
			if (nestedSememe.getSememeType() == SememeType.COMPONENT_NID) {
				@SuppressWarnings({"rawtypes", "unchecked"})
				Optional<LatestVersion<ComponentNidSememe>> latest = ((SememeChronology) nestedSememe).getLatestVersion(ComponentNidSememe.class,
						stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp);

				if (latest.isPresent()) {
					if (latest.get().value().getComponentNid() == MetaData.PREFERRED.getNid()) {
						if (answer.get() != null && answer.get() != true) {
							throw new RuntimeException("contradictory annotations about preferred status!");
						}
						answer.set(true);
					} else if (latest.get().value().getComponentNid() == MetaData.ACCEPTABLE.getNid()) {
						if (answer.get() != null && answer.get() != false) {
							throw new RuntimeException("contradictory annotations about preferred status!");
						}
						answer.set(false);
					} else {
						throw new RuntimeException("Unexpected component nid!");
					}

				}
			}
		});
		if (answer.get() == null) {
			log.warn("Description nid {} does not have an acceptability sememe!", descriptionSememeNid);
			return false;
		}
		return answer.get();
	}

	/**
	 * Returns a Map correlating each dialect sequence for a passed
	 * descriptionSememeId with its respective acceptability nid (preferred vs
	 * acceptable)
	 *
	 * @param descriptionSememeNid
	 * @param stamp - optional - if not provided, uses default from config
	 * service
	 * @throws RuntimeException If there is inconsistent data (incorrectly)
	 * attached to the sememe
	 */
	public static Map<Integer, Integer> getAcceptabilities(int descriptionSememeNid, StampCoordinate stamp) throws RuntimeException {
		Map<Integer, Integer> dialectSequenceToAcceptabilityNidMap = new ConcurrentHashMap<>();

		Get.sememeService().getSememesForComponent(descriptionSememeNid).forEach(nestedSememe
				-> {
			if (nestedSememe.getSememeType() == SememeType.COMPONENT_NID) {
				int dialectSequence = nestedSememe.getAssemblageSequence();

				@SuppressWarnings({"rawtypes", "unchecked"})
				Optional<LatestVersion<ComponentNidSememe>> latest = ((SememeChronology) nestedSememe).getLatestVersion(ComponentNidSememe.class,
						stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp);

				if (latest.isPresent()) {
					if (latest.get().value().getComponentNid() == MetaData.PREFERRED.getNid()
							|| latest.get().value().getComponentNid() == MetaData.ACCEPTABLE.getNid()) {
						if (dialectSequenceToAcceptabilityNidMap.get(dialectSequence) != null
								&& dialectSequenceToAcceptabilityNidMap.get(dialectSequence) != latest.get().value().getComponentNid()) {
							throw new RuntimeException("contradictory annotations about acceptability!");
						} else {
							dialectSequenceToAcceptabilityNidMap.put(dialectSequence, latest.get().value().getComponentNid());
						}
					} else {
						UUID uuid = null;
						String componentDesc = null;
						try {
							Optional<UUID> uuidOptional = Get.identifierService().getUuidPrimordialForNid(latest.get().value().getComponentNid());
							if (uuidOptional.isPresent()) {
								uuid = uuidOptional.get();
							}
							Optional<LatestVersion<DescriptionSememe<?>>> desc = Get.conceptService().getSnapshot(StampCoordinates.getDevelopmentLatest(), LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate()).getDescriptionOptional(latest.get().value().getComponentNid());
							componentDesc = desc.isPresent() ? desc.get().value().getText() : null;
						} catch (Exception e) {
							// NOOP
						}

						log.warn("Unexpected component " + componentDesc + " (uuid=" + uuid + ", nid=" + latest.get().value().getComponentNid() + ")");
						//throw new RuntimeException("Unexpected component " + componentDesc + " (uuid=" + uuid + ", nid=" + latest.get().value().getComponentNid() + ")");
						//dialectSequenceToAcceptabilityNidMap.put(dialectSequence, latest.get().value().getComponentNid());
					}
				}
			}
		});
		return dialectSequenceToAcceptabilityNidMap;
	}

	/**
	 * Convenience method to extract the latest version of descriptions of the
	 * requested type
	 *
	 * @param conceptNid The concept to read descriptions for
	 * @param descriptionType expected to be one of
	 * {@link MetaData#SYNONYM} or
	 * {@link MetaData#FULLY_SPECIFIED_NAME} or
	 * {@link MetaData#DEFINITION_DESCRIPTION_TYPE}
	 * @param stamp - optional - if not provided gets the default from the
	 * config service
	 * @return the descriptions - may be empty, will not be null
	 */
	public static List<DescriptionSememe<?>> getDescriptionsOfType(int conceptNid, ConceptSpecification descriptionType,
			StampCoordinate stamp) {
		ArrayList<DescriptionSememe<?>> results = new ArrayList<>();
		Get.sememeService().getSememesForComponent(conceptNid)
				.forEach(descriptionC
						-> {
					if (descriptionC.getSememeType() == SememeType.DESCRIPTION) {
						@SuppressWarnings({"unchecked", "rawtypes"})
						Optional<LatestVersion<DescriptionSememe<?>>> latest = ((SememeChronology) descriptionC).getLatestVersion(DescriptionSememe.class,
								stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp);
						if (latest.isPresent()) {
							DescriptionSememe<?> ds = latest.get().value();
							if (ds.getDescriptionTypeConceptSequence() == descriptionType.getConceptSequence()) {
								results.add(ds);
							}
						}
					}
				});
		return results;
	}

	public static Optional<Integer> getNidForSCTID(long sctID) {
		IndexServiceBI si = LookupService.get().getService(IndexServiceBI.class, "sememe indexer");
		if (si != null) {
			//force the prefix algorithm, and add a trailing space - quickest way to do an exact-match type of search
			List<SearchResult> result = si.query(sctID + " ", true,
					new Integer[] {MetaData.SCTID.getConceptSequence()}, 5, Long.MAX_VALUE, null);
			if (result.size() > 0) {
				return Optional.of(Get.sememeService().getSememe(result.get(0).getNid()).getReferencedComponentNid());
			}
		} else {
			log.warn("Sememe Index not available - can't lookup SCTID");
		}
		return Optional.empty();
	}

	/**
	 * Returns the nid of the component that that vuid is attached to (if any found)
	 * @param vuID
	 * @return
	 */
	public static Optional<Integer> getNidForVUID(long vuID) {
		IndexServiceBI si = LookupService.get().getService(IndexServiceBI.class, "sememe indexer");
		if (si != null) {
			//force the prefix algorithm, and add a trailing space - quickest way to do an exact-match type of search
			List<SearchResult> result = si.query(vuID + " ", true,
					new Integer[] {MetaData.VUID.getConceptSequence()}, 5, Long.MAX_VALUE, null);
			if (result.size() > 0) {
				return Optional.of(Get.sememeService().getSememe(result.get(0).getNid()).getReferencedComponentNid());
			}
		} else {
			final String msg = "Sememe Index not available - can't lookup VUID " + vuID;
			log.error(msg);
			throw new RuntimeException(msg);
		}
		return Optional.empty();
	}

	/**
	 * Returns the nids of all matching vuid sememes (if any found on view coordinate).
	 * @param vuID
	 * @param stampCoordinate
	 * @return
	 */
	public static Set<Integer> getVuidSememeNidsForVUID(long vuID) {
		final SememeIndexerBI si = LookupService.get().getService(SememeIndexerBI.class);
		if (si == null) {
			final String msg = "Sememe Index not available - can't lookup VUID " + vuID;
			log.error(msg);
			throw new RuntimeException(msg);
		}

		// StampCoordinate with LATEST ACTIVE_ONLY from all VHAT modules
		final StampPosition stampPosition = new StampPositionImpl(Long.MAX_VALUE, 
				TermAux.DEVELOPMENT_PATH.getConceptSequence());
		final Set<Integer> vhatModules = Frills.getAllChildrenOfConcept(MetaData.VHAT_MODULES.getConceptSequence(), true, true);
		final StampCoordinate stampCoordinate = new StampCoordinateImpl(StampPrecedence.PATH, stampPosition, 
				ConceptSequenceSet.of(vhatModules), State.ACTIVE_ONLY_SET);

		final Set<Integer> matchingVuidSememeNids = new HashSet<>();

		final Predicate<Integer> filter = new Predicate<Integer>() {
			@Override
			public boolean test(Integer t) {
				final Optional<SememeChronology<SememeVersion<?>>> sememeChronologyToCheck = (Optional<SememeChronology<SememeVersion<?>>>) Get.sememeService().getOptionalSememe(t);
				if (sememeChronologyToCheck.isPresent()
						&& sememeChronologyToCheck.get().getAssemblageSequence() == MetaData.VUID.getConceptSequence() // This check should be redundant
						) {
					final SememeChronology<StringSememeImpl> existingVuidSememe = (SememeChronology<StringSememeImpl>)((SememeChronology)sememeChronologyToCheck.get());
					Optional<LatestVersion<StringSememeImpl>> latestVersionOptional = existingVuidSememe.getLatestVersion(StringSememeImpl.class, stampCoordinate);

					if (latestVersionOptional.isPresent()) {
						// TODO do we care about contradictions?
						StringSememeImpl sememeVersion = latestVersionOptional.get().value();
						if ((vuID + "").equals(sememeVersion.getString())) {
							return true;
						}
					}
				}
				return false;
			}
		};
		//force the prefix algorithm, and add a trailing space - quickest way to do an exact-match type of search
		List<SearchResult> results = si.query(vuID + " ", true,
				new Integer[] {MetaData.VUID.getConceptSequence()}, 1000, Long.MAX_VALUE, filter, null);
		if (results.size() > 0) {
			for (SearchResult result : results) {
				matchingVuidSememeNids.add(result.getNid());
			}
		}

		return Collections.unmodifiableSet(matchingVuidSememeNids);
	}

	/**
	 * Convenience method to return sequences of a distinct set of modules in
	 * which versions of an ObjectChronology have been defined
	 *
	 * @param chronology The ObjectChronology
	 * @return sequences of a distinct set of modules in which versions of an
	 * ObjectChronology have been defined
	 */
	public static Set<Integer> getAllModuleSequences(ObjectChronology<? extends StampedVersion> chronology) {
		Set<Integer> moduleSequences = new HashSet<>();
		for (StampedVersion version : chronology.getVersionList()) {
			moduleSequences.add(version.getModuleSequence());
		}

		return Collections.unmodifiableSet(moduleSequences);
	}

	public static StampCoordinate makeStampCoordinateAnalogVaryingByModulesOnly(StampCoordinate existingStampCoordinate, int requiredModuleSequence, int... optionalModuleSequences) {
		ConceptSequenceSet moduleSequenceSet = new ConceptSequenceSet();
		moduleSequenceSet.add(requiredModuleSequence);
		if (optionalModuleSequences != null) {
			for (int seq : optionalModuleSequences) {
				moduleSequenceSet.add(seq);
			}
		}

		EnumSet<State> allowedStates = EnumSet.allOf(State.class);
		allowedStates.addAll(existingStampCoordinate.getAllowedStates());
		StampCoordinate newStampCoordinate = new StampCoordinateImpl(
				existingStampCoordinate.getStampPrecedence(),
				existingStampCoordinate.getStampPosition(),
				moduleSequenceSet, allowedStates);

		return newStampCoordinate;
	}

	public static void refreshIndexes() {
		LookupService.get().getAllServiceHandles(IndexServiceBI.class).forEach(index
				-> {
			//Making a query, with long.maxValue, causes the index to refresh itself, and look at the latest updates, if there have been updates.
			index.getService().query("hi", null, 1, Long.MAX_VALUE, null);
		});
	}
	
	/**
	 * Get isA children of a concept.  Does not return the requested concept in any circumstance.
	 * @param conceptSequence The concept to look at
	 * @param recursive recurse down from the concept
	 * @param leafOnly only return leaf nodes
	 * @return the set of concept sequence ids that represent the children
	 */
	public static Set<Integer> getAllChildrenOfConcept(int conceptSequence, boolean recursive, boolean leafOnly)
	{
		Set<Integer> temp = getAllChildrenOfConcept(new HashSet<Integer>(), conceptSequence, recursive, leafOnly);
		if (leafOnly && temp.size() == 1)
		{
			temp.remove(conceptSequence);
		}
		return temp;
	}
	
	/**
	 * Recursively get Is a children of a concept.  May inadvertently return the requested starting sequence when leafOnly is true, and 
	 * there are no children.
	 */
	private static Set<Integer> getAllChildrenOfConcept(Set<Integer> handledConceptSequenceIds, int conceptSequence, boolean recursive, boolean leafOnly)
	{
		Set<Integer> results = new HashSet<>();
		
		// This both prevents infinite recursion and avoids processing or returning of duplicates
		if (handledConceptSequenceIds.contains(conceptSequence)) {
			return results;
		}

		AtomicInteger count = new AtomicInteger();
		IntStream children = Get.taxonomyService().getTaxonomyChildSequences(conceptSequence);

		children.forEach((conSequence) ->
		{
			count.getAndIncrement();
			if (leafOnly)
			{
				Set<Integer> temp = getAllChildrenOfConcept(handledConceptSequenceIds, conSequence, recursive, leafOnly);
				
				if (recursive)
				{
					results.addAll(temp);
				}
				else
				{
					temp.remove(conSequence);  //remove itself
					if (temp.size() == 0)
					{
						//This is a leaf node.  Add it to results.
						results.add(conSequence);
					}
				}
			}
			else
			{
				results.add(conSequence);
				if (recursive)
				{
					results.addAll(getAllChildrenOfConcept(handledConceptSequenceIds, conSequence, recursive, leafOnly));
				}
			}
		});
		
		
		if (leafOnly && count.get() == 0)
		{
			results.add(conceptSequence);
		}
		handledConceptSequenceIds.add(conceptSequence);
		return results;
	}
	
	/**
	 * Create a new concept using the provided columnName and columnDescription values which is suitable 
	 * for use as a column descriptor within {@link DynamicSememeUsageDescription}.
	 * 
	 * The new concept will be created under the concept {@link DynamicSememeConstants#DYNAMIC_SEMEME_COLUMNS}
	 * 
	 * A complete usage pattern (where both the refex assemblage concept and the column name concept needs
	 * to be created) would look roughly like this:
	 * 
	 * DynamicSememeUtility.createNewDynamicSememeUsageDescriptionConcept(
	 *	 "The name of the Sememe", 
	 *	 "The description of the Sememe",
	 *	 new DynamicSememeColumnInfo[]{new DynamicSememeColumnInfo(
	 *		 0,
	 *		 DynamicSememeColumnInfo.createNewDynamicSememeColumnInfoConcept(
	 *			 "column name",
	 *			 "column description"
	 *			 )
	 *		 DynamicSememeDataType.STRING,
	 *		 new DynamicSememeStringImpl("default value")
	 *		 )}
	 *	 )
	 * 
	 * //TODO [REFEX] figure out language details (how we know what language to put on the name/description
	 * @throws RuntimeException 
	 */
	@SuppressWarnings("deprecation")
	public static ConceptChronology<? extends ConceptVersion<?>> createNewDynamicSememeColumnInfoConcept(String columnName, String columnDescription) 
			throws RuntimeException
	{
		ConceptChronology<? extends ConceptVersion<?>> newCon = buildNewDynamicSememeColumnInfoConcept(columnName, columnDescription);
		try {
			Get.commitService().commit("creating new dynamic sememe column: " + columnName).get();
			return newCon;
		} catch (InterruptedException | ExecutionException e) {
			String msg = "Failed committing new DynamicSememeColumnInfo concept columnName=\"" + columnName + "\", columnDescription=\"" + columnDescription + "\"";
			log.error(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	/**
	 * Build, without committing, a new concept using the provided columnName and columnDescription values which is suitable 
	 * for use as a column descriptor within {@link DynamicSememeUsageDescription}.
	 * 
	 * The new concept will be created under the concept {@link DynamicSememeConstants#DYNAMIC_SEMEME_COLUMNS}
	 * 
	 * A complete usage pattern (where both the refex assemblage concept and the column name concept needs
	 * to be created) would look roughly like this:
	 * 
	 * DynamicSememeUtility.createNewDynamicSememeUsageDescriptionConcept(
	 *	 "The name of the Sememe", 
	 *	 "The description of the Sememe",
	 *	 new DynamicSememeColumnInfo[]{new DynamicSememeColumnInfo(
	 *		 0,
	 *		 DynamicSememeColumnInfo.createNewDynamicSememeColumnInfoConcept(
	 *			 "column name",
	 *			 "column description"
	 *			 )
	 *		 DynamicSememeDataType.STRING,
	 *		 new DynamicSememeStringImpl("default value")
	 *		 )}
	 *	 )
	 * 
	 * //TODO [REFEX] figure out language details (how we know what language to put on the name/description
	 * @throws RuntimeException 
	 */
	public static ConceptChronology<? extends ConceptVersion<?>> buildNewDynamicSememeColumnInfoConcept(String columnName, String columnDescription) 
			throws RuntimeException
	{
		if (columnName == null || columnName.length() == 0 || columnDescription == null || columnDescription.length() == 0)
		{
			throw new RuntimeException("Both the column name and column description are required");
		}
		
		ConceptBuilderService conceptBuilderService = LookupService.getService(ConceptBuilderService.class);
		conceptBuilderService.setDefaultLanguageForDescriptions(MetaData.ENGLISH_LANGUAGE);
		conceptBuilderService.setDefaultDialectAssemblageForDescriptions(MetaData.US_ENGLISH_DIALECT);
		conceptBuilderService.setDefaultLogicCoordinate(LogicCoordinates.getStandardElProfile());

		DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class);
		LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();

		NecessarySet(And(ConceptAssertion(Get.conceptService().getConcept(DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMNS.getNid()), defBuilder)));

		LogicalExpression parentDef = defBuilder.build();

		ConceptBuilder builder = conceptBuilderService.getDefaultConceptBuilder(columnName, null, parentDef);

		DescriptionBuilder<?, ?> definitionBuilder = descriptionBuilderService.getDescriptionBuilder(columnName, builder,
						MetaData.SYNONYM,
						MetaData.ENGLISH_LANGUAGE);

		definitionBuilder.addPreferredInDialectAssemblage(MetaData.US_ENGLISH_DIALECT);
		builder.addDescription(definitionBuilder);
		
		definitionBuilder = descriptionBuilderService.getDescriptionBuilder(columnDescription, builder, MetaData.DEFINITION_DESCRIPTION_TYPE,
				MetaData.ENGLISH_LANGUAGE);
		definitionBuilder.addPreferredInDialectAssemblage(MetaData.US_ENGLISH_DIALECT);
		builder.addDescription(definitionBuilder);

		ConceptChronology<? extends ConceptVersion<?>> newCon;

		try
		{
			newCon = builder.build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE, new ArrayList<>()).get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			String msg = "Failed building new DynamicSememeColumnInfo concept columnName=\"" + columnName + "\", columnDescription=\"" + columnDescription + "\"";
			log.error(msg, e);
			throw new RuntimeException(msg, e);
		}
		return newCon;
	}

	/**
	 * See {@link DynamicSememeUsageDescription} for the full details on what this builds.
	 * 
	 * Does all the work to create a new concept that is suitable for use as an Assemblage Concept for a new style Dynamic Sememe.
	 * 
	 * The concept will be created under the concept {@link DynamicSememeConstants#DYNAMIC_SEMEME_ASSEMBLAGES} if a parent is not specified
	 * 
	 * //TODO [REFEX] figure out language details (how we know what language to put on the name/description
	 * @param sememePreferredTerm - The preferred term for this refex concept that will be created.
	 * @param sememeDescription - A user friendly string the explains the overall intended purpose of this sememe (what it means, what it stores)
	 * @param columns - The column information for this new refex.  May be an empty list or null.
	 * @param parentConceptNidOrSequence  - optional - if null, uses {@link DynamicSememeConstants#DYNAMIC_SEMEME_ASSEMBLAGES}
	 * @param referencedComponentRestriction - optional - may be null - if provided - this restricts the type of object referenced by the nid or 
	 * UUID that is set for the referenced component in an instance of this sememe.  If {@link ObjectChronologyType#UNKNOWN_NID} is passed, it is ignored, as 
	 * if it were null.
	 * @param referencedComponentSubRestriction - optional - may be null - subtype restriction for {@link ObjectChronologyType#SEMEME} restrictions
	 * @param editCoord - optional - the coordinate to use during create of the sememe concept (and related descriptions) - if not provided, uses system default.
	 * @return a reference to the newly created sememe item
	 */
	@SuppressWarnings("deprecation")
	public static DynamicSememeUsageDescription createNewDynamicSememeUsageDescriptionConcept(String sememeFSN, String sememePreferredTerm, 
			String sememeDescription, DynamicSememeColumnInfo[] columns, Integer parentConceptNidOrSequence, ObjectChronologyType referencedComponentRestriction,
			SememeType referencedComponentSubRestriction, EditCoordinate editCoord)
	{	
		ConceptChronology<? extends ConceptVersion<?>> newDynamicSememeUsageDescriptionConcept = buildUncommittedNewDynamicSememeUsageDescription(
				sememeFSN,
				sememePreferredTerm,
				sememeDescription,
				columns,
				parentConceptNidOrSequence,
				referencedComponentRestriction,
				referencedComponentSubRestriction,
				editCoord);
		
		try {
			Get.commitService().commit("creating new dynamic sememe assemblage (DynamicSememeUsageDescription): NID=" + newDynamicSememeUsageDescriptionConcept.getNid() + ", FSN=" + sememeFSN 
					+ ", PT=" + sememePreferredTerm + ", DESC=" + sememeDescription).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Commit of Dynamic Sememe Failed!", e);
		}

		return new DynamicSememeUsageDescriptionImpl(newDynamicSememeUsageDescriptionConcept.getNid());			
	}

	/**
	 * This method returns an uncommitted refexUsageDescriptor concept chronology.
	 * A DynamicSememeUsageDescription may be constructed by passing it to the DynamicSememeUsageDescriptionImpl ctor.
	 * 
	 * @param sememeFSN
	 * @param sememePreferredTerm
	 * @param sememeDescription
	 * @param columns
	 * @param parentConceptNidOrSequence
	 * @param referencedComponentRestriction
	 * @param referencedComponentSubRestriction
	 * @param editCoord
	 * @return
	 */
	public static ConceptChronology<? extends ConceptVersion<?>> buildUncommittedNewDynamicSememeUsageDescription(String sememeFSN, String sememePreferredTerm, 
			String sememeDescription, DynamicSememeColumnInfo[] columns, Integer parentConceptNidOrSequence, ObjectChronologyType referencedComponentRestriction,
			SememeType referencedComponentSubRestriction, EditCoordinate editCoord)
	{
		try
		{
			EditCoordinate localEditCoord = (editCoord == null ? Get.configurationService().getDefaultEditCoordinate() : editCoord);
			
			ConceptBuilderService conceptBuilderService = LookupService.getService(ConceptBuilderService.class);
			conceptBuilderService.setDefaultLanguageForDescriptions(MetaData.ENGLISH_LANGUAGE);
			conceptBuilderService.setDefaultDialectAssemblageForDescriptions(MetaData.US_ENGLISH_DIALECT);
			conceptBuilderService.setDefaultLogicCoordinate(LogicCoordinates.getStandardElProfile());

			DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class);
			LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();

			ConceptChronology<?> parentConcept =  Get.conceptService().getConcept(parentConceptNidOrSequence == null ? 
					DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSEMBLAGES.getNid() 
					: parentConceptNidOrSequence);
			
			NecessarySet(And(ConceptAssertion(parentConcept, defBuilder)));

			LogicalExpression parentDef = defBuilder.build();

			ConceptBuilder builder = conceptBuilderService.getDefaultConceptBuilder(sememeFSN, null, parentDef);

			DescriptionBuilder<? extends SememeChronology<?> , ? extends MutableDescriptionSememe<?>> definitionBuilder = descriptionBuilderService.
					getDescriptionBuilder(sememePreferredTerm, builder, MetaData.SYNONYM, MetaData.ENGLISH_LANGUAGE);
			definitionBuilder.addPreferredInDialectAssemblage(MetaData.US_ENGLISH_DIALECT);
			builder.addDescription(definitionBuilder);
			
			ConceptChronology<? extends ConceptVersion<?>> newCon = builder.build(localEditCoord, ChangeCheckerMode.ACTIVE, new ArrayList<>()).getNoThrow();
			
			{
				//Set up the dynamic sememe 'special' definition
				definitionBuilder = descriptionBuilderService.getDescriptionBuilder(sememeDescription, builder, MetaData.DEFINITION_DESCRIPTION_TYPE,
						MetaData.ENGLISH_LANGUAGE);
				definitionBuilder.addPreferredInDialectAssemblage(MetaData.US_ENGLISH_DIALECT);
				SememeChronology<?> definitionSememe = definitionBuilder.build(localEditCoord, ChangeCheckerMode.ACTIVE).getNoThrow();
				
				Get.sememeBuilderService().getDynamicSememeBuilder(definitionSememe.getNid(), 
						DynamicSememeConstants.get().DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getSequence(), null).build(localEditCoord, 
								ChangeCheckerMode.ACTIVE).getNoThrow();
			}

			if (columns != null)
			{
				//Ensure that we process in column order - we don't always keep track of that later - we depend on the data being stored in the right order.
				TreeSet<DynamicSememeColumnInfo> sortedColumns = new TreeSet<>(Arrays.asList(columns));
				
				for (DynamicSememeColumnInfo ci : sortedColumns)
				{
					DynamicSememeData[] data = LookupService.getService(DynamicSememeUtility.class).configureDynamicSememeDefinitionDataForColumn(ci);

					Get.sememeBuilderService().getDynamicSememeBuilder(newCon.getNid(), 
							DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENSION_DEFINITION.getSequence(), data)
						.build(localEditCoord, ChangeCheckerMode.ACTIVE).getNoThrow();
				}
			}
			
			DynamicSememeData[] data = LookupService.getService(DynamicSememeUtility.class).
					configureDynamicSememeRestrictionData(referencedComponentRestriction, referencedComponentSubRestriction);
			
			if (data != null)
			{
				Get.sememeBuilderService().getDynamicSememeBuilder(newCon.getNid(), 
						DynamicSememeConstants.get().DYNAMIC_SEMEME_REFERENCED_COMPONENT_RESTRICTION.getSequence(), data)
					.build(localEditCoord, ChangeCheckerMode.ACTIVE).getNoThrow();
			}

			return newCon;
		}
		catch (IllegalStateException e)
		{
			throw new RuntimeException("Creation of Dynamic Sememe Failed!", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public String[] readDynamicSememeColumnNameDescription(UUID columnDescriptionConcept)
	{
		String columnName = null;
		String columnDescription = null;
		String fsn = null;
		String acceptableSynonym = null;
		String acceptableDefinition = null;
		try
		{
			ConceptChronology<? extends ConceptVersion<?>> cc = Get.conceptService().getConcept(columnDescriptionConcept);
			for (SememeChronology<? extends DescriptionSememe<?>> dc : cc.getConceptDescriptionList())
			{
				if (columnName != null && columnDescription != null)
				{
					break;
				}
				
				@SuppressWarnings("rawtypes")
				Optional<LatestVersion<DescriptionSememe<?>>> descriptionVersion = ((SememeChronology)dc)
						.getLatestVersion(DescriptionSememe.class, Get.configurationService().getDefaultStampCoordinate().makeAnalog(State.ACTIVE, State.INACTIVE, State.CANCELED, State.PRIMORDIAL));
				
				if (descriptionVersion.isPresent())
				{
					DescriptionSememe<?> d = descriptionVersion.get().value();
					if (d.getDescriptionTypeConceptSequence() == TermAux.FULLY_SPECIFIED_DESCRIPTION_TYPE.getConceptSequence())
					{
						fsn = d.getText();
					}
					else if (d.getDescriptionTypeConceptSequence() == TermAux.SYNONYM_DESCRIPTION_TYPE.getConceptSequence())
					{
						if (Frills.isDescriptionPreferred(d.getNid(), null))
						{
							columnName = d.getText();
						}
						else
						{
							acceptableSynonym = d.getText();
						}
					}
					else if (d.getDescriptionTypeConceptSequence() == TermAux.DEFINITION_DESCRIPTION_TYPE.getConceptSequence())
					{
						if (Frills.isDescriptionPreferred(d.getNid(), null))
						{
							columnDescription = d.getText();
						}
						else
						{
							acceptableDefinition = d.getText();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.warn("Failure reading DynamicSememeColumnInfo '" + columnDescriptionConcept + "'", e);
		}
		if (columnName == null)
		{
			log.warn("No preferred synonym found on '" + columnDescriptionConcept + "' to use " + "for the column name - using FSN");
			columnName = (fsn == null ? "ERROR - see log" : fsn);
		}
		
		if (columnDescription == null && acceptableDefinition != null)
		{
			columnDescription = acceptableDefinition;
		}
		
		if (columnDescription == null && acceptableSynonym != null)
		{
			columnDescription = acceptableSynonym;
		}
		
		if (columnDescription == null)
		{
			log.info("No preferred or acceptable definition or acceptable synonym found on '" 
					+ columnDescriptionConcept + "' to use for the column description- re-using the the columnName, instead.");
			columnDescription = columnName;
		}
		return new String[] {columnName, columnDescription};
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends SememeVersion<T>> Optional<T> getLatestVersion(SememeChronology<T> sememeChronology, StampCoordinate sc) {
		return getLatestVersion((SememeChronology)sememeChronology, SememeVersion.class, sc);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends ConceptVersion<T>> Optional<T> getLatestVersion(ConceptChronology<T> conceptChronology, StampCoordinate sc) {
		return getLatestVersion((ConceptChronology)conceptChronology, ConceptVersion.class, sc);
	}
	public static <T extends StampedVersion> Optional<T> getLatestVersion(ObjectChronology<T> objectChronology, Class<T> clazz, StampCoordinate sc) {
			Optional<LatestVersion<T>> latestVersionOptional = objectChronology.getLatestVersion(clazz, sc);
	
			if (latestVersionOptional.isPresent()) {
				if (latestVersionOptional.get().contradictions().isPresent()) {
					// TODO properly handle contradictions
					final OchreExternalizableObjectType objectType = objectChronology.getOchreObjectType();
					String detail = "object";
					switch (objectType) {
					case SEMEME:
						detail = objectType + " UUID=" + objectChronology.getPrimordialUuid() + ", SEMEME SEQ=" + ((SememeChronology<?>)objectChronology).getSememeSequence() + ", REF COMP NID=" + ((SememeChronology<?>)objectChronology).getReferencedComponentNid();
						break;
					case CONCEPT:
					case STAMP_ALIAS:
					case STAMP_COMMENT:
						detail = objectType + " UUID=" + objectChronology.getPrimordialUuid();
						break;
					default:
						throw new RuntimeException("Unsupported OchreExternalizableObjectType for passed ObjectChronology UUID=" + objectChronology.getPrimordialUuid());
					}
	//				log.warn("Getting latest version of " + detail + " with " + latestVersionOptional.get().contradictions().get().size() 
	//						+ " version contradictions");
				}
				
				return Optional.of(latestVersionOptional.get().value());
			}
	
			return Optional.empty();
		}
	/**
	 * Utility method to get the best text value description for a concept, according to the user preferences.  
	 * Calls {@link #getDescription(UUID, LanguageCoordinate, StampCoordinate)} with nulls. 
	 * @param conceptUUID - identifier for a concept
	 * @return
	 */
	public static Optional<String> getDescription(UUID conceptUUID) {
		return getDescription(conceptUUID, null, null);
	}

	/**
	 * Utility method to get the best text value description for a concept, according to the user preferences.  
	 * Calls {@link #getDescription(int, LanguageCoordinate, StampCoordinate)} using the default system stamp coordinate, 
	 * modified to return all states (this may return an inactive description)
	 * @param conceptId - either a sequence or a nid
	 * @return
	 */
	public static Optional<String> getDescription(int conceptId) {
		return getDescription(conceptId, Get.configurationService().getDefaultStampCoordinate().makeAnalog(State.ACTIVE, State.INACTIVE, State.CANCELED, State.PRIMORDIAL), null);
	}
	
	/**
	 * Utility method to get the best text value description for a concept, according to the passed in options, 
	 * or the user preferences.  Calls {@link #getDescription(UUID, LanguageCoordinate, StampCoordinate)} with values 
	 * extracted from the taxonomyCoordinate, or null. 
	 * @param conceptUUID - identifier for a concept
	 * @param tc - optional - if not provided, defaults to system preferences values
	 * @return
	 */
	public static Optional<String> getDescription(UUID conceptUUID, TaxonomyCoordinate taxonomyCoordinate) {
		return getDescription(conceptUUID, taxonomyCoordinate == null ? null : taxonomyCoordinate.getStampCoordinate(), 
				taxonomyCoordinate == null ? null : taxonomyCoordinate.getLanguageCoordinate());
	}

	/**
	 * Utility method to get the best text value description for a concept, according to the passed in options, 
	 * or the user preferences.  Calls {@link #getDescription(int, LanguageCoordinate, StampCoordinate)} with values 
	 * extracted from the taxonomyCoordinate, or null. 
	 * @param conceptId - either a sequence or a nid
	 * @param tc - optional - if not provided, defaults to system preferences values
	 * @return
	 */
	public static Optional<String> getDescription(int conceptId, TaxonomyCoordinate taxonomyCoordinate) {
		return getDescription(conceptId, taxonomyCoordinate == null ? null : taxonomyCoordinate.getStampCoordinate(), 
				taxonomyCoordinate == null ? null : taxonomyCoordinate.getLanguageCoordinate());
	}
	
	/**
	 * Utility method to get the best text value description for a concept, according to the passed in options, 
	 * or the user preferences.  Calls {@link #getDescription(int, LanguageCoordinate, StampCoordinate)} with values 
	 * extracted from the taxonomyCoordinate, or null. 
	 * @param conceptId - either a sequence or a nid
	 * @param languageCoordinate - optional - if not provided, defaults to system preferences values
	 * @param stampCoordinate - optional - if not provided, defaults to system preference values
	 * @return
	 */
	public static Optional<String> getDescription(UUID conceptUUID, StampCoordinate stampCoordinate, LanguageCoordinate languageCoordinate) 
	{
		return getDescription(Get.identifierService().getConceptSequenceForUuids(conceptUUID), stampCoordinate, languageCoordinate);
	}

	/**
	 * Utility method to get the best text value description for a concept, according to the passed in options, 
	 * or the user preferences. 
	 * @param conceptId - either a sequence or a nid
	 * @param languageCoordinate - optional - if not provided, defaults to system preferences values
	 * @param stampCoordinate - optional - if not provided, defaults to system preference values
	 * @return
	 */
	public static Optional<String> getDescription(int conceptId, StampCoordinate stampCoordinate, LanguageCoordinate languageCoordinate) 
	{
		Optional<LatestVersion<DescriptionSememe<?>>> desc = Get.conceptService()
			.getSnapshot(stampCoordinate == null ? Get.configurationService().getDefaultStampCoordinate() : stampCoordinate,
						languageCoordinate == null ? Get.configurationService().getDefaultLanguageCoordinate() : languageCoordinate)
					.getDescriptionOptional(conceptId);
		
		return desc.isPresent() ? Optional.of(desc.get().value().getText()) : Optional.empty();
	}
	
	public static List<SimpleDisplayConcept> getExtendedDescriptionTypes() throws IOException
	{
		Set<Integer> extendedDescriptionTypes;
		ArrayList<SimpleDisplayConcept> temp = new ArrayList<>();
		extendedDescriptionTypes = Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_TYPE_IN_SOURCE_TERMINOLOGY.getConceptSequence(), true, true);
		for (Integer seq : extendedDescriptionTypes)
		{
			temp.add(new SimpleDisplayConcept(seq));
		}
		Collections.sort(temp);
		return temp;
	}
	
	/**
	 * If this description is flagged as an extended description type, return the type concept of the extension.
	 * @param sc - optional Stamp - pass null to use the default stamp.  In either case, this only looks for an active extended type - state is overridden.
	 * @param descriptionId - the nid or sequence of the description sememe to check for an extended type. 
	 * @param returnInactiveExtendedType - true to return an extended description type even if it is INACTVE .  
	 * false to only return the extended description type if it is present and active (returns EMPTY if the extended type is missing or inactive)
	 * @return
	 */
	public static Optional<UUID> getDescriptionExtendedTypeConcept(StampCoordinate stampCoordinate, int descriptionId, boolean returnInactiveExtendedType) 
	{
		Optional<SememeChronology<? extends SememeVersion<?>>> descriptionExtendedTypeAnnotationSememe =
				getAnnotationSememe(Get.identifierService().getSememeNid(descriptionId), 
						DynamicSememeConstants.get().DYNAMIC_SEMEME_EXTENDED_DESCRIPTION_TYPE.getConceptSequence());
		
		if (descriptionExtendedTypeAnnotationSememe.isPresent()) 
		{
			final StampCoordinate effectiveStampCoordinate = (stampCoordinate == null) ? Get.configurationService().getDefaultStampCoordinate()
					.makeAnalog(State.ANY_STATE_SET.toArray(new State[State.ANY_STATE_SET.size()])) 
					: stampCoordinate.makeAnalog(State.ANY_STATE_SET.toArray(new State[State.ANY_STATE_SET.size()]));
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Optional<LatestVersion<DynamicSememeImpl>> optionalLatestSememeVersion = ((SememeChronology)(descriptionExtendedTypeAnnotationSememe.get()))
				.getLatestVersion(DynamicSememeImpl.class, effectiveStampCoordinate);
			if (! optionalLatestSememeVersion.isPresent()) {
				log.info("No latest version present for descriptionExtendedTypeAnnotationSememe chronology " 
						+ descriptionExtendedTypeAnnotationSememe.get().getPrimordialUuid() + " using " + (stampCoordinate != null ? "passed" : "default") 
						+ " stamp coordinate analog " + effectiveStampCoordinate);
				return Optional.empty();
			}
			if (optionalLatestSememeVersion.get().contradictions().isPresent() && optionalLatestSememeVersion.get().contradictions().get().size() > 0) {
				//TODO handle contradictions
				log.warn("Component " + descriptionId + " " + " has DYNAMIC_SEMEME_EXTENDED_DESCRIPTION_TYPE annotation with " + optionalLatestSememeVersion.get()
					.contradictions().get().size() + " contradictions");
			}
			if (!returnInactiveExtendedType && optionalLatestSememeVersion.get().value().getState() != State.ACTIVE) {
				log.info("Latest version present is NOT ACTIVE for descriptionExtendedTypeAnnotationSememe chronology " 
						+ descriptionExtendedTypeAnnotationSememe.get().getPrimordialUuid() + " using " + (stampCoordinate != null ? "passed" : "default") 
						+ " stamp coordinate analog " + effectiveStampCoordinate);
				return Optional.empty();	
			}
			
			DynamicSememeData[] dataColumns = optionalLatestSememeVersion.get().value().getData();
			if (dataColumns.length != 1)
			{
				throw new RuntimeException("Invalidly specified DYNAMIC_SEMEME_EXTENDED_DESCRIPTION_TYPE.  Should always have a column size of 1");
			}
			
			if (dataColumns[0].getDynamicSememeDataType() == DynamicSememeDataType.UUID) 
			{
				return Optional.of(((DynamicSememeUUIDImpl)dataColumns[0]).getDataUUID());
			}
			// This isn't supposed to happen, but we have some bad data where it did.
			else if (dataColumns[0].getDynamicSememeDataType() == DynamicSememeDataType.STRING) 
			{
				log.warn("Extended description type data found with type string instead of type UUID!");
				return Optional.of(UUID.fromString(((DynamicSememeStringImpl)dataColumns[0]).getDataString()));
			}
			
			throw new RuntimeException("Failed to find UUID DynamicSememeData type in DYNAMIC_SEMEME_EXTENDED_DESCRIPTION_TYPE annotation dynamic sememe");
		}
		return Optional.empty();
	}
	
	/**
	 * A convenience method to determine if a particular component has 0 or 1 annotations of a particular type.  If there is more than one 
	 * annotation of a particular type, this method will throw a runtime exception.
	 * @param componentNid - the component to check for the assemblage
	 * @param assemblageConceptId - the assemblage type you are interested in (nid or concept sequence)
	 * @return
	 */
	public static Optional<SememeChronology<? extends SememeVersion<?>>> getAnnotationSememe(int componentNid, int assemblageConceptId) 
	{
		Set<Integer> allowedAssemblages = new HashSet<>();
		allowedAssemblages.add(assemblageConceptId);
		Set<SememeChronology<? extends SememeVersion<?>>> sememeSet= Get.sememeService()
				.getSememesForComponentFromAssemblages(componentNid, allowedAssemblages).collect(Collectors.toSet());
		switch(sememeSet.size()) 
		{
			case 0:
				return Optional.empty();
			case 1:
				return Optional.of(sememeSet.iterator().next());
				default:
					log.fatal("Component " + componentNid + " has " + sememeSet.size() + " annotations of type " + 
							Get.conceptDescriptionText(assemblageConceptId) + " (should only have zero or 1) - returning arbitrary result!");
					return Optional.of(sememeSet.iterator().next());
		}
	}
	
	/**
	 * Calls {@link #getConceptForUnknownIdentifier(String)} in a background thread.  returns immediately. 
	 * 
	 * 
	 * @param identifier - what to search for
	 * @param callback - who to inform when lookup completes
	 * @param callId - An arbitrary identifier that will be returned to the caller when this completes
	 * @param stampCoord - optional - what stamp to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @param langCoord - optional - what lang coord to use when returning the ConceptSnapshot (defaults to user prefs)
	 */
	public static void lookupConceptForUnknownIdentifier(
			final String identifier,
			final TaskCompleteCallback<ConceptSnapshot> callback,
			final Integer callId,
			final StampCoordinate stampCoord,
			final LanguageCoordinate langCoord)
	{
		log.debug("Threaded Lookup: '{}'", identifier);
		final long submitTime = System.currentTimeMillis();
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				ConceptSnapshot result = null;
				Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> c = getConceptForUnknownIdentifier(identifier);
				if (c.isPresent())
				{
					Optional<ConceptSnapshot> temp = getConceptSnapshot(c.get().getConceptSequence(), stampCoord, langCoord);
					if (temp.isPresent())
					{
						result = temp.get();
					}
					callback.taskComplete(result, submitTime, callId);
				}
				else {
					callback.taskComplete(null, submitTime, callId);
				}
				
			}
		};
		Get.workExecutors().getExecutor().execute(r);
	}
	
	/**
	 * If the passed in value is a {@link UUID}, calls {@link ConceptService#getOptionalConcept(int)} after converting the UUID to nid.
	 * Next, if no hit, if the passed in value is parseable as a int < 0 (a nid), calls {@link ConceptService#getOptionalConcept(int)}
	 * Next, if no hit, if the passed in value is parseable as a long, and is a valid SCTID (checksum is valid) - treats it as 
	 * a SCTID and attempts to look up the SCTID in the lucene index.  Note that is is possible for some 
	 * sequence identifiers to look like SCTIDs - if a passed in value is valid as both a SCTID and a sequence identifier - it will be 
	 * treated as an SCTID.
	 * Finally, if it is a positive integer, it treats is as a sequence identity, converts it to a nid, then looks up the nid.
	 */
	public static Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> getConceptForUnknownIdentifier(String identifier)
	{
		log.debug("Concept Chronology lookup by string '{}'", identifier);

		if (StringUtils.isBlank(identifier))
		{
			return Optional.empty();
		}
		String localIdentifier = identifier.trim();

		Optional<UUID> uuid = UUIDUtil.getUUID(localIdentifier);
		if (uuid.isPresent())
		{
			return Get.conceptService().getOptionalConcept(uuid.get());
		}
		
		//if it is a negative integer, assume nid
		Optional<Integer> nid = NumericUtils.getNID(localIdentifier);
		if (nid.isPresent()) {
			return Get.conceptService().getOptionalConcept(nid.get());
		}
		
		if (SctId.isValidSctId(localIdentifier))
		{
			
			IndexServiceBI si = LookupService.get().getService(IndexServiceBI.class, "sememe indexer");
			if (si != null)
			{
				//force the prefix algorithm, and add a trailing space - quickest way to do an exact-match type of search
				List<SearchResult> result = si.query(localIdentifier + " ", true, 
						new Integer[] {MetaData.SCTID.getConceptSequence()}, 5, Long.MIN_VALUE, null);
				if (result.size() > 0)
				{
					int componentNid = Get.sememeService().getSememe(result.get(0).getNid()).getReferencedComponentNid();
					if (Get.identifierService().getChronologyTypeForNid(componentNid) == ObjectChronologyType.CONCEPT)
					{
						return Get.conceptService().getOptionalConcept(componentNid);
					}
					else
					{
						log.warn("Passed in SCTID is not a Concept ID!");
						return Optional.empty();
					}
				}
			}
			else
			{
				log.warn("Sememe Index not available - can't lookup SCTID");
			}
		}
		else if (NumericUtils.isInt(localIdentifier))
		{
			//Must be a postive integer, which wasn't a valid SCTID - it may be a sequence ID.
			int nidFromSequence = Get.identifierService().getConceptNid(Integer.parseInt(localIdentifier));
			if (nidFromSequence != 0)
			{
				return Get.conceptService().getOptionalConcept(nidFromSequence);
			}
		}
		return Optional.empty();
	}
	
	
	/**
	 * @param conceptNidOrSequence
	 * @param stampCoord - optional - what stamp to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @param langCoord - optional - what lang coord to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @return the ConceptSnapshot, or an optional that indicates empty, if the identifier was invalid, or if the concept didn't 
	 * have a version available on the specified stampCoord
	 */
	public static Optional<ConceptSnapshot> getConceptSnapshot(int conceptNidOrSequence, 
			StampCoordinate stampCoord, LanguageCoordinate langCoord)
	{
		Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> c = Get.conceptService().getOptionalConcept(conceptNidOrSequence);
		if (c.isPresent())
		{
			try
			{
				return Optional.of(Get.conceptService().getSnapshot(
						stampCoord == null ? Get.configurationService().getDefaultStampCoordinate() : stampCoord,
						langCoord == null ? Get.configurationService().getDefaultLanguageCoordinate() : langCoord)
							.getConceptSnapshot(c.get().getConceptSequence()));
			}
			catch (Exception e)
			{
				//TODO conceptSnapshot APIs are currently broken, provide no means of detecting if a concept doesn't exist on a given coordinate
				//See slack convo https://informatics-arch.slack.com/archives/dev-isaac/p1440568057000512
				return Optional.empty();
			}
		}
		return Optional.empty();
	}
	
	/**
	 * @param conceptUUID
	 * @param stampCoord - optional - what stamp to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @param langCoord - optional - what lang coord to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @return the ConceptSnapshot, or an optional that indicates empty, if the identifier was invalid, or if the concept didn't 
	 *	have a version available on the specified stampCoord
	 */
	public static Optional<ConceptSnapshot> getConceptSnapshot(UUID conceptUUID, StampCoordinate stampCoord, LanguageCoordinate langCoord)
	{
		return getConceptSnapshot(Get.identifierService().getNidForUuids(conceptUUID), stampCoord, langCoord);
	}
	
	/**
	 * 
	 * All done in a background thread, method returns immediately
	 * 
	 * @param identifier - The NID to search for
	 * @param callback - who to inform when lookup completes
	 * @param callId - An arbitrary identifier that will be returned to the caller when this completes
	 * @param stampCoord - optional - what stamp to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @param langCoord - optional - what lang coord to use when returning the ConceptSnapshot (defaults to user prefs)
	 */
	public static void lookupConceptSnapshot(
			final int nid,
			final TaskCompleteCallback<ConceptSnapshot> callback,
			final Integer callId,
			final StampCoordinate stampCoord,
			final LanguageCoordinate langCoord)
	{
		log.debug("Threaded Lookup: '{}'", nid);
		final long submitTime = System.currentTimeMillis();
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				Optional<ConceptSnapshot> c = getConceptSnapshot(nid, stampCoord, langCoord);
				if (c.isPresent()) {
					callback.taskComplete(c.get(), submitTime, callId);
				}
				else {
					callback.taskComplete(null, submitTime, callId);
				}
			}
		};
		Get.workExecutors().getExecutor().execute(r);
	}


	/**
	 * Convenience method to find the nearest concept related to a sememe.  Recursively walks referenced components until it finds a concept.
	 * @param nid 
	 * @return the nearest concept sequence, or -1, if no concept can be found.
	 */
	public static int findConcept(int nid)
	{
		Optional<? extends ObjectChronology<? extends StampedVersion>> c = Get.identifiedObjectService().getIdentifiedObjectChronology(nid);
		
		if (c.isPresent())
		{
			if (c.get().getOchreObjectType() == OchreExternalizableObjectType.SEMEME)
			{
				return findConcept(((SememeChronology<?>)c.get()).getReferencedComponentNid());
			}
			else if (c.get().getOchreObjectType() == OchreExternalizableObjectType.CONCEPT)
			{
				return ((ConceptChronology<?>)c.get()).getConceptSequence();
			}
			else
			{
				log.warn("Unexpected object type: " + c.get().getOchreObjectType());
			}
		}
		return -1;
	}

	public static Class<? extends StampedVersion> getVersionType(int nid) {
		Optional<? extends ObjectChronology<? extends StampedVersion>> obj = Get.identifiedObjectService().getIdentifiedObjectChronology(nid);
		if (! obj.isPresent()) {
			throw new RuntimeException("No StampedVersion object exists with NID=" + nid);
		}
		return getVersionType(obj.get());
	}
	public static Class<? extends StampedVersion> getVersionType(ObjectChronology<? extends StampedVersion> obj) {
		switch (obj.getOchreObjectType()) {
		case SEMEME: {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			SememeChronology<? extends SememeVersion> sememeChronology = (SememeChronology<? extends SememeVersion>)obj;
			switch (sememeChronology.getSememeType()) {
			case COMPONENT_NID:
				return ComponentNidSememeImpl.class;
			case DESCRIPTION:
				return DescriptionSememeImpl.class;
			case DYNAMIC:
				return DynamicSememeImpl.class;
			case LOGIC_GRAPH:
				return LogicGraphSememeImpl.class;
			case LONG:
				return LongSememeImpl.class;
			case STRING:
				return StringSememeImpl.class;
			case RELATIONSHIP_ADAPTOR:
				return RelationshipVersionAdaptorImpl.class;
			case UNKNOWN:
			case MEMBER:
			default:
				throw new RuntimeException("Sememe with NID=" + obj.getNid() + " is of unsupported SememeType " + sememeChronology.getSememeType());
			}
		}
		case CONCEPT:
			return ConceptVersionImpl.class;
			default:
				throw new RuntimeException("Object with NID=" + obj.getNid() + " is of unsupported OchreExternalizableObjectType " + obj.getOchreObjectType());
		}
	}
	
	public static boolean definesAssociation(int conceptSequence)
	{
		if (isAssociationCache.containsKey(conceptSequence))
		{
			return isAssociationCache.get(conceptSequence);
		}
		boolean temp = Get.sememeService().getSememesForComponentFromAssemblage(Get.identifierService().getConceptNid(conceptSequence), 
				DynamicSememeConstants.get().DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getConceptSequence()).anyMatch(sememe -> true);
		isAssociationCache.put(conceptSequence, temp);
		return temp;
	}
	
	public static boolean isAssociation(SememeChronology<? extends SememeVersion<?>> sc)
	{
		return definesAssociation(sc.getAssemblageSequence());
	}
	
	public static boolean definesMapping(int conceptSequence)
	{
		if (isMappingCache.containsKey(conceptSequence))
		{
			return isMappingCache.get(conceptSequence);
		}
		boolean temp = Get.sememeService().getSememesForComponentFromAssemblage(Get.identifierService().getConceptNid(conceptSequence), 
				IsaacMappingConstants.get().DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getConceptSequence()).anyMatch(sememe -> true);
		isMappingCache.put(conceptSequence, temp);
		return temp;
	}
	
	public static boolean isMapping(SememeChronology<? extends SememeVersion<?>> sc)
	{
		return definesMapping(sc.getAssemblageSequence());
	}
	
	public static boolean definesDynamicSememe(int conceptSequence)
	{
		return DynamicSememeUsageDescriptionImpl.isDynamicSememe(conceptSequence);
	}
	

	/**
	 * Returns true if a concept has a {@link MetaData#IDENTIFIER_SOURCE} sememe attached to it (at any coordinate)
	 * @param assemblageNidOrSequence
	 * @return
	 */
	public static boolean definesIdentifierSememe(int assemblageNidOrSequence) 
	{
		if (assemblageNidOrSequence >= 0 || Get.identifierService().getChronologyTypeForNid(assemblageNidOrSequence) == ObjectChronologyType.CONCEPT) 
		{
			Optional<SememeChronology<? extends SememeVersion<?>>> sememe = Get.sememeService().getSememesForComponentFromAssemblage(
					Get.identifierService().getConceptNid(assemblageNidOrSequence),
						MetaData.IDENTIFIER_SOURCE.getConceptSequence()).findAny();
		if (sememe.isPresent())
			{
				return true;
			}
		}
		return false;
	}

	public static Optional<String> getAnnotationStringValue(int componentId, int assemblageConceptId, StampCoordinate stamp) {
		try 
		{
			Optional<UUID> assemblageConceptUuid = Get.identifierService().getUuidPrimordialFromConceptId(assemblageConceptId);
			if (! assemblageConceptUuid.isPresent()) {
				throw new RuntimeException("getUuidPrimordialFromConceptId() return empty UUID for assemblageConceptId " + assemblageConceptId);
			}
			int componentNid = Get.identifierService().getConceptNid(componentId);
			
			ArrayList<String> values = new ArrayList<>(1);

			int assemblageConceptSequence = Get.identifierService().getConceptSequenceForUuids(assemblageConceptUuid.get());
			Get.sememeService().getSnapshot(SememeVersion.class,
					stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp)
			.getLatestSememeVersionsForComponentFromAssemblage(componentNid,
					assemblageConceptSequence).forEach(latestSememe ->
					{
						if (latestSememe.value().getChronology().getSememeType() == SememeType.STRING)
						{
							values.add(((StringSememeImpl)latestSememe.value()).getString());
						}
						else if (latestSememe.value().getChronology().getSememeType() == SememeType.COMPONENT_NID)
						{
							values.add(((ComponentNidSememeImpl)latestSememe.value()).getComponentNid() + "");
						}
						else if (latestSememe.value().getChronology().getSememeType() == SememeType.LONG)
						{
							values.add(((LongSememeImpl)latestSememe.value()).getLongValue() + "");
						}
						else if (latestSememe.value().getChronology().getSememeType() == SememeType.DYNAMIC)
						{
							DynamicSememeData[] data = ((DynamicSememeImpl)latestSememe.value()).getData();
							if (data.length > 0) {
								log.warn("Found multiple (" + data.length + ") dynamic sememe data fields in sememe " + latestSememe.value().getNid() + " of assemblage type " + assemblageConceptUuid + " on component " + Get.identifierService().getUuidPrimordialForNid(componentNid));
							}
							values.add(data[0].dataToString());
						}
					});

			if (values.size() > 1)
			{
				log.warn("Found multiple (" + values.size() + ") " + assemblageConceptUuid + " annotation sememes on component " + Get.identifierService().getUuidPrimordialForNid(componentNid) + ". Using first value \"" + values.get(0) + "\".");
			}
			if (values.size() > 0)
			{
				return Optional.of(values.get(0));
			}
		}
		catch (Exception e) 
		{
			log.error("Unexpected error trying to find " + assemblageConceptId + " annotation sememe on component " + componentId, e);
		}
		return Optional.empty();
	}
	
	/**
	 * Returns the set of terminology types (which are concepts directly under {@link MetaData#MODULE} for any concept 
	 * or sememe in the system as a set of concept sequences.
	 * 
	 * Also, if the concept is a child of {@link MetaData#ISAAC_METADATA}, then it will also be marked with the terminology type 
	 * of {@link MetaData#ISAAC_MODULE} - even if there is no concept version that exists using the ISAAC_MODULE module - this gives
	 * an easy way to identify "metadata" concepts.
	 * 
	 * @param oc - the object to read modules for
	 * @param coord - if null, return the modules ignoring coordinates.  If not null, only return modules visible on the given coordinate
	 */
	public static HashSet<Integer> getTerminologyTypes(ConceptChronology<? extends StampedVersion> oc, StampCoordinate coord)
	{
		HashSet<Integer> modules = new HashSet<>();
		HashSet<Integer> terminologyTypes = new HashSet<>();
		
		if (coord == null)
		{
			oc.getVersionStampSequences().forEach(stampSequence -> 
			{
				modules.add(Get.stampService().getModuleSequenceForStamp(stampSequence));
			});
			if (Get.taxonomyService().wasEverKindOf(oc.getConceptSequence(), MetaData.ISAAC_METADATA.getConceptSequence()))
			{
				terminologyTypes.add(MetaData.ISAAC_MODULE.getConceptSequence());
			}
		}
		else
		{
			oc.getVersionList().stream().filter(version -> 
			{
				return coord.getAllowedStates().contains(version.getState()) && 
						(coord.getModuleSequences().size() == 0 ? true : coord.getModuleSequences().contains(version.getModuleSequence()));
			}).forEach(version ->
			{
				modules.add(version.getModuleSequence());
			});
			if (Get.taxonomyService().isKindOf(oc.getConceptSequence(), MetaData.ISAAC_METADATA.getConceptSequence(), 
					TaxonomyCoordinates.getStatedTaxonomyCoordinate(coord, LanguageCoordinates.getUsEnglishLanguagePreferredTermCoordinate())))  //lang doesn't matter for our use case.
			{
				terminologyTypes.add(MetaData.ISAAC_MODULE.getConceptSequence());
			}
		}
		
		for (int moduleSequence : modules)
		{
			if (moduleToTermTypeCache.containsKey(moduleSequence))
			{
				terminologyTypes.add(moduleToTermTypeCache.get(moduleSequence));
			}
			else
			{
				if (Get.taxonomyService().wasEverKindOf(moduleSequence, MetaData.MODULE.getConceptSequence()))
				{
					Integer termTypeConcept = findTermTypeConcept(moduleSequence);
					if (termTypeConcept != null)
					{
						terminologyTypes.add(termTypeConcept);
						moduleToTermTypeCache.put(moduleSequence, termTypeConcept);
					}
				}
			}
		}
		return terminologyTypes;
	}
	
	/**
	 * Walk up the module tree, looking for the module concept sequence directly under MetaData.MODULE - return it if found, otherwise, return null.
	 */
	private static Integer findTermTypeConcept(int conceptModuleSequence)
	{
		OfInt parents = Get.taxonomyService().getTaxonomyParentSequences(conceptModuleSequence).iterator();
		while (parents.hasNext())
		{
			int current = parents.next();
			if (current == MetaData.MODULE.getConceptSequence())
			{
				return conceptModuleSequence;
			}
			else
			{
				return findTermTypeConcept(current);
			}
		}
		return null;
	}

	/**
	 * Get all sememes for a specified component of specified assemblages restricted by SememeType
	 * @param componentNid - referenced component nid of requested sememes
	 * @param allowedAssemblageSequences - set of concept sequences of allowed assemblages
	 * @param typesToExclude - set of SememeType restrictions 
	 * @return
	 */
	public static Stream<SememeChronology<? extends SememeVersion<?>>> getSememesForComponentFromAssemblagesFilteredBySememeType(int componentNid, 
			Set<Integer> allowedAssemblageSequences, Set<SememeType> typesToExclude) {
		SememeSequenceSet sememeSequences = Get.sememeService().getSememeSequencesForComponentFromAssemblages(componentNid, allowedAssemblageSequences);
		if (typesToExclude == null || typesToExclude.size() == 0) {
			return sememeSequences.stream().mapToObj((int sememeSequence) -> Get.sememeService().getSememe(sememeSequence));
		} else {
			final ArrayList<SememeChronology<? extends SememeVersion<?>>> filteredList = new ArrayList<>();
			for (PrimitiveIterator.OfInt it = sememeSequences.getIntIterator(); it.hasNext();) {
				SememeChronology<? extends SememeVersion<?>> chronology = Get.sememeService().getSememe(it.nextInt());
				boolean exclude = false;
				for (SememeType type : typesToExclude) {
					if (chronology.getSememeType() == type) {
						exclude = true;
						break;
					}
				}

				if (! exclude) {
					filteredList.add(chronology);
				}
			}

			return filteredList.stream();
		}
	}
	

	/**
	 * Reset the state of an object to the new state, copying by creating a new version of the object with the same mutable values as the existing object.
	 * 
	 * This returns an empty optional (and does a NOOP) if the existing state is already the same as the desired state.
	 * 
	 * @param state - The desired new state
	 * @param nid - the id of the object to change the state of
	 * @param editCoordinate - where to write the new state.
	 * @param readCoordinates - (optional) the read coordinates to read the current state from.  Defaults to the system default if not provided.  When more than
	 * one is provided, it tries each in order, until is finds the first one that is present.
	 * @return - empty optional, if no change, or the uncommitted chronology of the object that was changed.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public static Optional<ObjectChronology> resetStateWithNoCommit(State state, int nid, EditCoordinate editCoordinate, StampCoordinate ... readCoordinates ) 
			throws Exception {
		
		final ObjectChronologyType type = Get.identifierService().getChronologyTypeForNid(nid);

		ObjectChronology objectToCommit = null;

		State priorState = null;
		int sequence = 0;
		
		switch (type)
		{
			case CONCEPT:
			{
				ConceptChronology cc = Get.conceptService().getConcept(nid);
				sequence = cc.getConceptSequence();

				@SuppressWarnings("unchecked")
				VersionUpdatePair<ConceptVersion> updatePair = resetConceptState(state, cc, editCoordinate, readCoordinates);
				if (updatePair != null) {
					priorState = updatePair.latest.getState();
					objectToCommit = cc;
				}
				break;
			}
				
			case SEMEME:
			{
				SememeChronology<? extends SememeVersion<?>> sememe = Get.sememeService().getSememe(nid);
				sequence = sememe.getSememeSequence();
				switch (sememe.getSememeType()) 
				{
					case DESCRIPTION: {
						@SuppressWarnings("unchecked")
						VersionUpdatePair<DescriptionSememeImpl> sememeUpdatePair = resetState(state, (SememeChronology<DescriptionSememeImpl>)sememe, 
								DescriptionSememeImpl.class, editCoordinate, readCoordinates);
	
						if (sememeUpdatePair != null) {
							priorState = sememeUpdatePair.latest.getState();
							sememeUpdatePair.mutable.setCaseSignificanceConceptSequence(sememeUpdatePair.latest.getCaseSignificanceConceptSequence());
							sememeUpdatePair.mutable.setDescriptionTypeConceptSequence(sememeUpdatePair.latest.getDescriptionTypeConceptSequence());
							sememeUpdatePair.mutable.setLanguageConceptSequence(sememeUpdatePair.latest.getLanguageConceptSequence());
							sememeUpdatePair.mutable.setText(sememeUpdatePair.latest.getText());
							objectToCommit = sememe;
						}
						break;
					}
					case STRING: {
						@SuppressWarnings("unchecked")
						VersionUpdatePair<StringSememeImpl> sememeUpdatePair = resetState(state, (SememeChronology<StringSememeImpl>)sememe, StringSememeImpl.class,
								editCoordinate, readCoordinates);
	
						if (sememeUpdatePair != null) {
							priorState = sememeUpdatePair.latest.getState();
							sememeUpdatePair.mutable.setString(sememeUpdatePair.latest.getString());
							objectToCommit = sememe;
						} 
	
						break;
					}
					case DYNAMIC: {
						@SuppressWarnings("unchecked")
						VersionUpdatePair<DynamicSememeImpl> sememeUpdatePair = resetState(state, (SememeChronology<DynamicSememeImpl>)sememe, DynamicSememeImpl.class, 
								editCoordinate, readCoordinates);
	
						if (sememeUpdatePair != null) {
							priorState = sememeUpdatePair.latest.getState();
							sememeUpdatePair.mutable.setData(sememeUpdatePair.latest.getData());
							objectToCommit = sememe;
						}
						break;
					}
					case COMPONENT_NID: {
						@SuppressWarnings("unchecked")
						VersionUpdatePair<ComponentNidSememeImpl> sememeUpdatePair = resetState(state, (SememeChronology<ComponentNidSememeImpl>)sememe, 
								ComponentNidSememeImpl.class, editCoordinate, readCoordinates);
	
						if (sememeUpdatePair != null) {
							priorState = sememeUpdatePair.latest.getState();
							sememeUpdatePair.mutable.setComponentNid(sememeUpdatePair.latest.getComponentNid());
							objectToCommit = sememe;
						} 
						break;
					}
					case LOGIC_GRAPH: {
						@SuppressWarnings("unchecked")
						VersionUpdatePair<LogicGraphSememeImpl> sememeUpdatePair = resetState(state, (SememeChronology<LogicGraphSememeImpl>)sememe, 
								LogicGraphSememeImpl.class, editCoordinate, readCoordinates);
	
						if (sememeUpdatePair != null) {
							priorState = sememeUpdatePair.latest.getState();
							sememeUpdatePair.mutable.setGraphData(sememeUpdatePair.latest.getGraphData());
							objectToCommit = sememe;
						}
						break;
					}
					case LONG: {
						@SuppressWarnings("unchecked")
						VersionUpdatePair<LongSememeImpl> sememeUpdatePair = resetState(state, (SememeChronology<LongSememeImpl>)sememe, LongSememeImpl.class, 
								editCoordinate, readCoordinates);
	
						if (sememeUpdatePair != null) {
							priorState = sememeUpdatePair.latest.getState();
							sememeUpdatePair.mutable.setLongValue(sememeUpdatePair.latest.getLongValue());
							objectToCommit = sememe;
						}
						break;
					}
					case MEMBER:
						// TODO figure out why compiling of generics failing on command line but not in eclipse, requiring this hack
						@SuppressWarnings("unchecked")
						VersionUpdatePair<SememeVersion> sememeUpdatePair = resetState(state, (SememeChronology)sememe, SememeVersion.class, 
								editCoordinate, readCoordinates);
						
						if (sememeUpdatePair != null) {
							priorState = sememeUpdatePair.latest.getState();
							objectToCommit = sememe;
						}
						break;
					case RELATIONSHIP_ADAPTOR:
					case UNKNOWN:
					default:
						String detail = sememe.getSememeType() + " (UUID=" + sememe.getPrimordialUuid() + ", SEMEME SEQ=" + sememe.getSememeSequence() 
						+ ", REF COMP NID=" + sememe.getReferencedComponentNid() + ")";

						throw new Exception("Unsupported sememe of type " + detail + "from id " + nid);
				}
				break;
			}

			case UNKNOWN_NID:
			default :
				throw new Exception("Could not locate component '" + nid + "' of unexpected type " + type + " to change its state");
		}
		
		if (objectToCommit != null) 
		{
			log.debug("Built updated version of " + type + " " + nid + "<" + sequence + ">" + " with state changed (from " + priorState + " to " + state + ")");
		} 
		else 
		{
			log.debug("No need to commit update of " + type + " " + nid + "<" + sequence + ">" + " with unchanged state (" + state + ")");
		}

		return Optional.ofNullable(objectToCommit);
	}
	
	private static class VersionUpdatePair<T extends StampedVersion> {
		T mutable;
		T latest;
		
		public void set(T mutable, T latest) {
			this.mutable = mutable;
			this.latest = latest;
		}
	}
	
	/**
	 * calls {@link Frills#resetState(State, ObjectChronology, Class, EditCoordinate, StampCoordinate...) with a null Class
	 */
	private static <T extends ConceptVersion<T>> VersionUpdatePair<T> resetConceptState(State state, ConceptChronology<T> chronology, 
			EditCoordinate editCoordinate, StampCoordinate ... readCoordinates) throws Exception {	
		return resetState(state, chronology, (Class<T>)null, editCoordinate, readCoordinates);
	}
	
	/**
	 * 
	 * Reset the state of the chronology IFF an existing version corresponding to passed edit and/or stamp coordinates
	 * either does not exist or differs in state.
	 * 
	 * @param state - state to which to set new version of chronology
	 * @param chronology - the chronology of the object that we want to create a new version of with the specified state
	 * @param clazz - Java Class of StampVersion implementation for which to create a new version of the specified state.  Required for sememes, not used for concepts.
	 * @param editCoordinate - where to create the new version
	 * @param readCoordinates -  (optional) the read coordinates to read the current state from.  Defaults to the system default if not provided.  When more than
	 * one is provided, it tries each in order, until is finds the first one that is present.
	 * @return - null, if no change was required, or, the mutable that will need to be committed.  Also returns the latestVersion that the state was read from for 
	 * convenience.
	 * @throws RestException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T extends StampedVersion> VersionUpdatePair<T> resetState(State state, ObjectChronology<T> chronology, Class<T> clazz, 
			EditCoordinate editCoordinate, StampCoordinate ... readCoordinates)
			throws Exception 
	{
		String detail = chronology.getOchreObjectType() + " " + chronology.getClass().getSimpleName() + " (UUID=" + chronology.getPrimordialUuid() + ")";
		Optional<LatestVersion<T>> latestVersion = null;
		
		if (readCoordinates == null || readCoordinates.length == 0)
		{
			latestVersion = chronology.getLatestVersion(clazz, Get.configurationService().getDefaultStampCoordinate());
		}
		else
		{
			for (StampCoordinate rc : readCoordinates)
			{
				latestVersion = chronology.getLatestVersion(clazz, rc);
				if (latestVersion.isPresent())
				{
					break;
				}
			}
		}
		
		if (!latestVersion.isPresent())
		{
			throw new Exception("Failed getting latest version of " + detail + ". May require different stamp or edit coordinate parameters.");
		}
		
		if (latestVersion.get().value().getState() == state) 
		{
			log.debug("Not resetting state of " + detail + "from " + latestVersion.get().value().getState() + " to " + state);
			return null;
		}
		
		StampedVersion rawMutableVersion = null;
		if (chronology instanceof SememeChronology) 
		{
			rawMutableVersion = ((SememeChronology)chronology).createMutableVersion(clazz, state, editCoordinate);
		} 
		else if (chronology instanceof ConceptChronology) 
		{
			rawMutableVersion = ((ConceptChronology)chronology).createMutableVersion(state, editCoordinate);
		} 
		else 
		{
			throw new RuntimeException("Unsupported ObjectChronology type " + detail);
		}

		VersionUpdatePair<T> versionsHolder = new VersionUpdatePair<>();
		versionsHolder.set((T)rawMutableVersion, latestVersion.get().value());

		return versionsHolder;
	}
	
	/**
	 * Retrieve the set of integer parent concept sequences stored in the logic graph necessary sets
	 * 
	 * @param logicGraph
	 * @return
	 */
	public static Set<Integer> getParentConceptSequencesFromLogicGraph(LogicGraphSememe<?> logicGraph) {
		Set<Integer> parentConceptSequences = new HashSet<>();
		Stream<LogicNode> isAs = logicGraph.getLogicalExpression().getNodesOfType(NodeSemantic.NECESSARY_SET);
		for (Iterator<LogicNode> necessarySetsIterator = isAs.distinct().iterator(); necessarySetsIterator.hasNext();) {
			NecessarySetNode necessarySetNode = (NecessarySetNode)necessarySetsIterator.next();
			for (AbstractLogicNode childOfNecessarySetNode : necessarySetNode.getChildren()) {
				if (childOfNecessarySetNode.getNodeSemantic() == NodeSemantic.AND) {
					AndNode andNode = (AndNode)childOfNecessarySetNode;
					for (AbstractLogicNode childOfAndNode : andNode.getChildren()) {
						if (childOfAndNode.getNodeSemantic() == NodeSemantic.CONCEPT) {
							if (childOfAndNode instanceof ConceptNodeWithSequences) {
								ConceptNodeWithSequences conceptNode = (ConceptNodeWithSequences)childOfAndNode;
								parentConceptSequences.add(conceptNode.getConceptSequence());
							} else if (childOfAndNode instanceof ConceptNodeWithUuids) {
								ConceptNodeWithUuids conceptNode = (ConceptNodeWithUuids)childOfAndNode;
								parentConceptSequences.add(Get.identifierService().getConceptSequenceForUuids(conceptNode.getConceptUuid()));
							} else {
								// Should never happen
								String msg = "Logic graph for concept NID=" + logicGraph.getReferencedComponentNid() + " has child of AndNode logic graph node of unexpected type \"" + childOfAndNode.getClass().getSimpleName() + "\". Expected ConceptNodeWithSequences or ConceptNodeWithUuids in " + logicGraph;
								log.error(msg);
								throw new RuntimeException(msg);
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
						// Should never happen
						String msg = "Logic graph for concept NID=" + logicGraph.getReferencedComponentNid() + " has child of NecessarySet logic graph node of unexpected type \"" + childOfNecessarySetNode.getClass().getSimpleName() + "\". Expected ConceptNodeWithSequences or ConceptNodeWithUuids in " + logicGraph;
						log.error(msg);
						throw new RuntimeException(msg);
					}
				} else {
					String msg = "Logic graph for concept NID=" + logicGraph.getReferencedComponentNid() + " has child of NecessarySet logic graph node of unexpected type \"" + childOfNecessarySetNode.getNodeSemantic() + "\". Expected AndNode or ConceptNode in " + logicGraph;
					log.error(msg);
					throw new RuntimeException(msg);
				}
			}
		}
		
		return parentConceptSequences;
	}
	
	/**
	 * Create a logical expression suitable for populating a concept's logic graph sememe
	 * 
	 * @param parentConceptSequences
	 * @return
	 */
	public static LogicalExpression createConceptParentLogicalExpression(int...parentConceptSequences) {
		// build logic graph
		LogicalExpressionBuilder defBuilder = LookupService.getService(LogicalExpressionBuilderService.class).getLogicalExpressionBuilder();
		ArrayList<Assertion> assertions = new ArrayList<>();
		for (int parentConceptSequence : parentConceptSequences) {
			assertions.add(ConceptAssertion(parentConceptSequence, defBuilder));
		}
		
		NecessarySet(And(assertions.toArray(new Assertion[assertions.size()])));
		LogicalExpression parentDef = defBuilder.build();
		
		return parentDef;
	}
}

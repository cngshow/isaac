package gov.vha.isaac.ochre.query.provider.lucene.indexers;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.api.logic.LogicNode;
import gov.vha.isaac.ochre.api.tree.TreeNodeVisitData;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeLongImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNidImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import gov.vha.isaac.ochre.query.provider.lucene.LuceneDescriptionType;
import gov.vha.isaac.ochre.query.provider.lucene.LuceneIndexer;
import gov.vha.isaac.ochre.query.provider.lucene.PerFieldAnalyzer;

/**
 * Lucene Manager for a Description index. Provides the description indexing
 * service.
 *
 * This has been redesigned such that is now creates multiple columns within the
 * index
 *
 * There is a 'everything' column, which gets all descriptions, to support the
 * standard search where you want to match on a text value anywhere it appears.
 *
 * There are 3 columns to support FSN / Synonym / Definition - to support
 * searching that subset of descriptions. There are also data-defined columns to
 * support extended definition types - for example - loinc description types -
 * to support searching terminology specific fields.
 *
 * Each of the columns above is also x2, as everything is indexed both with a
 * standard analyzer, and with a whitespace analyzer.
 *
 * @author aimeefurber
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service(name = "stamp indexer")
@RunLevel(value = LookupService.SL_L2_DATABASE_SERVICES_STARTED_RUNLEVEL)
public class StampIndexer extends LuceneIndexer implements IndexServiceBI {
	
	private static final Logger log = LogManager.getLogger();

	private static final String FIELD_INDEXED_MODULE_STRING_VALUE = "_module_content_";
    private static final String FIELD_INDEXED_PATH_STRING_VALUE = "_path_content_";
    
    protected enum StampType
    {
    	MODULE, PATH, ALL;
    }

    // for HK2 only
    private StampIndexer() throws IOException {
        super("stamps");
    }

    @Override
	protected boolean indexChronicle(ObjectChronology<?> chronicle)
	{
		if (chronicle instanceof SememeChronology<?>)
		{
			SememeChronology<?> sememeChronology = (SememeChronology<?>) chronicle;
			if (sememeChronology.getSememeType() == SememeType.DYNAMIC 
					|| sememeChronology.getSememeType() == SememeType.STRING 
					|| sememeChronology.getSememeType() == SememeType.LONG 
					|| sememeChronology.getSememeType() == SememeType.COMPONENT_NID 
					|| sememeChronology.getSememeType() == SememeType.LOGIC_GRAPH
					|| sememeChronology.getSememeType() == SememeType.DESCRIPTION)
			{
				return true;
			}
		}
		return false;
	}

    @Override
	protected void addFields(ObjectChronology<?> chronicle, Document doc)
	{
		SememeChronology<?> sememeChronology = (SememeChronology<?>) chronicle;
		doc.add(new TextField(FIELD_SEMEME_ASSEMBLAGE_SEQUENCE, sememeChronology.getAssemblageSequence() + "", Field.Store.NO));

		if (indexChronicle(chronicle))
		{
			for (SememeVersion<?> sv : sememeChronology.getVersionList())
			{
				indexModule(doc, sv.getModuleSequence());
				indexPath(doc, sv.getPathSequence());
			}
		}
		else
		{
			log.error("Unexpected type handed to addFields in Stamp Indexer: " + sememeChronology.toString());
		}

		//Due to indexing all of the versions, we may have added duplicate field name/value combinations to the document.
		//Remove the dupes.
		Iterator<IndexableField> it = doc.iterator();
		HashSet<String> uniqueFields = new HashSet<>();
		while (it.hasNext())
		{
			IndexableField field = it.next();
			String temp = field.name() + "::" + field.stringValue();
			
			if (uniqueFields.contains(temp))
			{
				it.remove();
			}
			else
			{
				uniqueFields.add(temp);
			}
		}
	}

    private void addField(Document doc, String fieldName, String value, boolean tokenize) {
        // TODO: Not sure this is necessary ...
    	//index twice per field - once with the standard analyzer, once with the whitespace analyzer.
        if (tokenize) {
            doc.add(new TextField(fieldName, value, Field.Store.NO));
        }
        doc.add(new TextField(fieldName + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, value, Field.Store.NO));
    }
    
    /**
     * Indexing the UUID of the ISAAC modules for each sememe.
     * 
     * Note: there will be 1 or more modules/version.
     * 
     * @param doc - The Lucene document/record to index
     * @param moduleSeq - The ISAAC module sequence identifier
     */
    private void indexModule(Document doc, int moduleSeq)
    {
    	Get.sememeService().getSememesForComponentFromAssemblage(moduleSeq, 
    			MetaData.MODULE.getConceptSequence()).forEach((s) -> {
			Optional<UUID> uuid = Get.identifierService().getUuidPrimordialFromConceptId(s.getNid()); 
			if (uuid.isPresent())
			{
				addField(doc, FIELD_INDEXED_MODULE_STRING_VALUE, uuid.get().toString(), true);
				incrementIndexedItemCount("Module - " + s.getSememeType().toString());
			}
    	});    	
    }
    
    
    /**
     * Indexing the UUID of the ISAAC paths for each sememe.
     * 
     * Note: there will be 1 or more paths/version.
     * 
     * @param doc - The Lucene document/record to index
     * @param moduleSeq - The ISAAC module sequence identifier
     */
    private void indexPath(Document doc, int pathSeq)
    {
    	Get.sememeService().getSememesForComponentFromAssemblage(pathSeq, 
    			MetaData.PATH.getConceptSequence()).forEach((s) -> {
			Optional<UUID> uuid = Get.identifierService().getUuidPrimordialFromConceptId(s.getNid()); 
			if (uuid.isPresent())
			{
				addField(doc, FIELD_INDEXED_PATH_STRING_VALUE, uuid.get().toString(), true);
				incrementIndexedItemCount("Path - " + s.getSememeType().toString());
			}
    	});
    }
    
    /**
     * Search the specified stamp type.
     *
     * @param query - The query to apply
     * @param stampUuid - The UUID of the staMP (module or path) to query
     * @param sizeLimit - The maximum size of the result list.
     * @param targetGeneration - The target generation that must be included in the
     * search or Long.MIN_VALUE if there is no need to wait for a target
     * generation. Long.MAX_VALUE can be passed in to force this query to wait
     * until any in progress indexing operations are completed - and then use
     * the latest index.
     * @return A List of <code>SearchResult</codes> that contains the nid of the
     * component that matched, and the score of that match relative to other
     * matches.
    */
    public final List<SearchResult> query(String query, UUID stampUuid, int sizeLimit, Long targetGeneration)
    {
    	ConceptChronology<? extends ConceptVersion<?>> stampCC = Get.conceptService().getConcept(stampUuid);
    	// TODO: defaults to 'module' until implementation details are discussed further
    	String field = FIELD_INDEXED_MODULE_STRING_VALUE;
    	if (Get.taxonomyService().wasEverKindOf(stampCC.getNid(), MetaData.PATH.getNid()))
    	{
    		field = FIELD_INDEXED_PATH_STRING_VALUE;
    	}
    	else if (Get.taxonomyService().wasEverKindOf(stampCC.getNid(), MetaData.MODULE.getNid()))
    	{
    		field = FIELD_INDEXED_MODULE_STRING_VALUE;
    	}
    	log.debug("Querying using {} for value {}", field, stampUuid);
        return search(buildTokenizedStringQuery(query, field, false, false), sizeLimit, targetGeneration, null);
    }

	/**
	 * This shouldn't be used, added for interface
	 */
    @Override
	public List<SearchResult> query(String query, boolean prefixSearch, Integer[] sememeConceptSequence, int sizeLimit,
			Long targetGeneration) 
   {
    	return new java.util.ArrayList<>();
    	/*return search(
                restrictToSememe(buildTokenizedStringQuery(query, FIELD_INDEXED_STRING_VALUE, prefixSearch, false), sememeConceptSequence),
                sizeLimit, targetGeneration, null);*/
	}

}

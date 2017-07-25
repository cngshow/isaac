package gov.vha.isaac.ochre.query.provider.lucene.indexers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.index.SearchResult;
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
		
		if (indexChronicle(chronicle))
		{
			for (SememeVersion<?> sv : sememeChronology.getVersionList())
			{
				indexModule(doc, sv.getModuleSequence());
				indexPath(doc, sv.getPathSequence());
				doc.add(new TextField(FIELD_SEMEME_ASSEMBLAGE_SEQUENCE, sv.getChronology().getAssemblageSequence() + "", Field.Store.NO));
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
    	log.debug("Querying for {}.", stampUuid.toString());
    	return query(query, false, new Integer[]{stampCC.getConceptSequence()}, sizeLimit, targetGeneration);
    }

	/**
	 * Iterate through the various STAMP field options based on the supplied sememe sequences.
	 * 
	 * @param query - The query to apply
	 * @param prefixSearch - Not used, but normally if true, utilize a search algorithm that is optimized for prefix searching, such as the searching 
     * that would be done to implement a type-ahead style search.  Does not use the Lucene Query parser.  Every term (or token) 
     * that is part of the query string will be required to be found in the result. 
     * @param semeneConceptSequence optional - The concept seqeuence of the sememes that you wish to search within, such as
     * {@link MetaData#MODULE} and {@link MetaData#PATH} for example. If null or empty searches all indexed content.
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
    @Override
	public List<SearchResult> query(String query, boolean prefixSearch, Integer[] sememeConceptSequence, int sizeLimit,
			Long targetGeneration) 
    {
    	List<SearchResult> results = new ArrayList<>();
    	List<String> fields = new ArrayList<>();
    	
    	// If not supplied, search all indexed content
    	if (sememeConceptSequence == null || sememeConceptSequence.length < 1)
    	{
    		sememeConceptSequence = new Integer[]
    				{ MetaData.MODULE.getConceptSequence(), MetaData.PATH.getConceptSequence() };
    	}
    	
    	// Search all sememe types, in case both PATH and MODULE (etc.) are supplied
    	for (Integer seq : sememeConceptSequence)
    	{
    		if (seq != null)
    		{
    			Optional<String> field = getFieldName(seq.intValue());
        		if (field.isPresent() && !fields.contains(field.get()))
        		{
        			fields.add(field.get());
        			log.debug("Querying using field {}.", field.get());
        			results.addAll(search(buildTokenizedStringQuery(query, field.get(), prefixSearch, false), 
        					sizeLimit, targetGeneration, null));
        		}
    		}
    	}
    	return results;
	}
    
    private Optional<String> getFieldName(int nidOrSeq)
    {
    	String field = null;
    	if (Get.taxonomyService().wasEverKindOf(nidOrSeq, MetaData.PATH.getNid()))
    	{
    		field = FIELD_INDEXED_PATH_STRING_VALUE;
    	}
    	else if (Get.taxonomyService().wasEverKindOf(nidOrSeq, MetaData.MODULE.getNid()))
    	{
    		field = FIELD_INDEXED_MODULE_STRING_VALUE;
    	}
    	else
    	{
    		log.warn("Nid or sequence {} is not valid to determine index field for stamp search.", nidOrSeq);
    	}
    	return Optional.ofNullable(field);
    }
}

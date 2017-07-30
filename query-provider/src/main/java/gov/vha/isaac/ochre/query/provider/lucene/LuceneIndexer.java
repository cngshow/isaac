package gov.vha.isaac.ochre.query.provider.lucene;

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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LegacyIntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexFormatTooOldException;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.SystemStatusService;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.commit.ChronologyChangeListener;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.identity.StampedVersion;
import gov.vha.isaac.ochre.api.index.ComponentSearchResult;
import gov.vha.isaac.ochre.api.index.ConceptSearchResult;
import gov.vha.isaac.ochre.api.index.GenerateIndexes;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.index.IndexedGenerationCallable;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.api.util.NamedThreadFactory;
import gov.vha.isaac.ochre.api.util.RecursiveDelete;
import gov.vha.isaac.ochre.api.util.UUIDUtil;
import gov.vha.isaac.ochre.api.util.UuidT5Generator;
import gov.vha.isaac.ochre.api.util.WorkExecutors;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.query.provider.lucene.indexers.DescriptionIndexer;
import gov.vha.isaac.ochre.query.provider.lucene.indexers.SememeIndexer;
import gov.vha.isaac.ochre.query.provider.lucene.indexers.TopDocsFilteredCollector;

// See example for help with the Controlled Real-time indexing...
// http://stackoverflow.com/questions/17993960/lucene-4-4-0-new-controlledrealtimereopenthread-sample-usage?answertab=votes#tab-top

public abstract class LuceneIndexer implements IndexServiceBI {

    public static final String DEFAULT_LUCENE_FOLDER = "lucene";
    private static final Logger log = LogManager.getLogger();
    private static final UnindexedFuture unindexedFuture = new UnindexedFuture();
    
    private File indexFolder_ = null;
    private ChronologyChangeListener changeListenerRef_;
    
    //don't need to analyze this - and even though it is an integer, we index it as a string, as that is faster when we are only doing
    //exact matches.
    protected static final String FIELD_SEMEME_ASSEMBLAGE_SEQUENCE = "_sememe_type_sequence_" + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER;
    //don't need to analyze, we only ever put a single char here - "t" - when a description is on a concept that is a part of the metadata tree.
    protected static final String FIELD_CONCEPT_IS_METADATA = "_concept_metadata_marker_" + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER;
    protected static final String FIELD_CONCEPT_IS_METADATA_VALUE = "t";

    //this isn't indexed
    public static final String FIELD_COMPONENT_NID = "_component_nid_";
    private static final String FIELD_INDEXED_MODULE_STRING_VALUE = "_module_content_";
    private static final String FIELD_INDEXED_PATH_STRING_VALUE = "_path_content_";
    
    protected static final FieldType FIELD_TYPE_INT_STORED_NOT_INDEXED;

    static {
        FIELD_TYPE_INT_STORED_NOT_INDEXED = new FieldType();
        FIELD_TYPE_INT_STORED_NOT_INDEXED.setNumericType(FieldType.LegacyNumericType.INT);
        FIELD_TYPE_INT_STORED_NOT_INDEXED.setIndexOptions(IndexOptions.NONE);
        FIELD_TYPE_INT_STORED_NOT_INDEXED.setStored(true);
        FIELD_TYPE_INT_STORED_NOT_INDEXED.setTokenized(false);
        FIELD_TYPE_INT_STORED_NOT_INDEXED.freeze();
    }
    
    private final HashMap<String, AtomicInteger> indexedComponentStatistics = new HashMap<>();
    private Semaphore indexedComponentStatisticsBlock = new Semaphore(1);

    private final ConcurrentHashMap<Integer, IndexedGenerationCallable> componentNidLatch = new ConcurrentHashMap<>();
    private boolean enabled_ = true;
    protected final ExecutorService luceneWriterService;
    protected ExecutorService luceneWriterFutureCheckerService;
    private final ControlledRealTimeReopenThread<IndexSearcher> reopenThread;
    private IndexWriter indexWriter;
    private final ReferenceManager<IndexSearcher> referenceManager;
    private final String indexName_;
    private Boolean dbBuildMode = null;
    private DatabaseValidity databaseValidity = DatabaseValidity.NOT_SET;
    boolean reindexRequired = false;

    protected LuceneIndexer(String indexName) throws IOException {
        try {
            indexName_ = indexName;
            luceneWriterService = LookupService.getService(WorkExecutors.class).getIOExecutor();
            luceneWriterFutureCheckerService = Executors.newFixedThreadPool(1, new NamedThreadFactory(indexName + " Lucene future checker", false));
            
            Path searchFolder = LookupService.getService(ConfigurationService.class).getSearchFolderPath();
            
            File luceneRootFolder = new File(searchFolder.toFile(), DEFAULT_LUCENE_FOLDER);
            luceneRootFolder.mkdirs();
            
            indexFolder_ = new File(luceneRootFolder, indexName);
            if (!indexFolder_.exists()) {
                databaseValidity = DatabaseValidity.MISSING_DIRECTORY;
            } else if (indexFolder_.list().length > 0) {
                databaseValidity = DatabaseValidity.POPULATED_DIRECTORY;
            }

            indexFolder_.mkdirs();

            log.info("Index: " + indexFolder_.getAbsolutePath());
            
            Directory indexDirectory = new MMapDirectory(indexFolder_.toPath()); //switch over to MMapDirectory - in theory - this gives us back some 
            //room on the JDK stack, letting the OS directly manage the caching of the index files - and more importantly, gives us a huge 
            //performance boost during any operation that tries to do multi-threaded reads of the index (like the SOLOR rules processing) because
            //the default value of SimpleFSDirectory is a huge bottleneck.

            try
            {
                indexWriter = new IndexWriter(indexDirectory, getIndexWriterConfig());
            }
            catch (IndexFormatTooOldException e)
            {
                log.warn("Lucene index format was too old in'" + getIndexerName() + "'.  Reindexing!");
                RecursiveDelete.delete(indexFolder_);
                indexFolder_.mkdirs();
                indexWriter = new IndexWriter(indexDirectory, getIndexWriterConfig());
                reindexRequired = true;
            }
            //In the case of a blank index, we need to kick it to disk, otherwise, the search manager constructor fails.
            indexWriter.commit();

            // [3]: Create the ControlledRealTimeReopenThread that reopens the index periodically taking into 
            //      account the changes made to the index and tracked by the TrackingIndexWriter instance
            //      The index is refreshed every 60sc when nobody is waiting 
            //      and every 100 millis whenever is someone waiting (see search method)
            //      (see http://lucene.apache.org/core/4_3_0/core/org/apache/lucene/search/NRTManagerReopenThread.html)
            referenceManager = new SearcherManager(indexDirectory, new SearcherFactory());
            reopenThread = new ControlledRealTimeReopenThread<IndexSearcher>(indexWriter, referenceManager, 60.00, 0.1);
   
            this.startThread();
            
            //Register for commits:
            
            log.info("Registering indexer " + getIndexerName() + " for commits");
            changeListenerRef_ = new ChronologyChangeListener()
            {
                
                @Override
                public void handleCommit(CommitRecord commitRecord)
                {
                    if (dbBuildMode == null)
                    {
                        dbBuildMode = Get.configurationService().inDBBuildMode();
                    }
                    if (dbBuildMode)
                    {
                        log.debug("Ignore commit due to db build mode");
                        return;
                    }
                    int size = commitRecord.getSememesInCommit().size();
                    if (size < 100)
                    {
                        log.info("submitting sememes " + commitRecord.getSememesInCommit().toString() + " to indexer " + getIndexerName() + " due to commit");
                    }
                    else
                    {
                        log.info("submitting " + size + " sememes to indexer " + getIndexerName() + " due to commit");
                    }
                    ArrayList<Future<Long>> futures = new ArrayList<>();
                    commitRecord.getSememesInCommit().stream().forEach(sememeId -> 
                    {
                        SememeChronology<?> sc = Get.sememeService().getSememe(sememeId);
                        futures.add(index(sc));
                    });
                    //wait for all indexing operations to complete
                    for (Future<Long> f : futures)
                    {
                        try 
                        {
                            f.get();
                        } 
                        catch (InterruptedException | ExecutionException e) 
                        {
                            log.error("Unexpected error waiting for index update",  e);
                        }
                    }
                    commitWriter();
                }
                
                @Override
                public void handleChange(SememeChronology<? extends SememeVersion<?>> sc)
                {
                    //noop
                }
                
                @Override
                public void handleChange(ConceptChronology<? extends StampedVersion> cc)
                {
                    // noop
                }
                
                @Override
                public UUID getListenerUuid()
                {
                    return UuidT5Generator.get(getIndexerName());
                }
            };
            
            Get.commitService().addChangeListener(changeListenerRef_);
            
        }
        catch (Exception e) {
            LookupService.getService(SystemStatusService.class).notifyServiceConfigurationFailure(indexName, e);
            throw e;
        }
    }
    
    private IndexWriterConfig getIndexWriterConfig()
    {
        IndexWriterConfig config = new IndexWriterConfig(new PerFieldAnalyzer());
        config.setRAMBufferSizeMB(256);
        MergePolicy mergePolicy = new LogByteSizeMergePolicy();

        config.setMergePolicy(mergePolicy);
        config.setSimilarity(new ShortTextSimilarity());
        return config;
    }

    private void startThread() {
        reopenThread.setName("Lucene " + indexName_ + " Reopen Thread");
        reopenThread.setPriority(Math.min(Thread.currentThread().getPriority() + 2, Thread.MAX_PRIORITY));
        reopenThread.setDaemon(true);
        reopenThread.start();
    }

    @Override
    public String getIndexerName() {
        return indexName_;
    }

    /**
     * Query index with no specified target generation of the index.
     * 
     * Calls {@link #query(String, Integer, int, long)} with the semeneConceptSequence set to null and 
     * the targetGeneration field set to Long.MIN_VALUE
     *
     * @param query The query to apply.
     * @param sizeLimit The maximum size of the result list.
     * @return a List of {@code SearchResult} that contains the nid of the
     * component that matched, and the score of that match relative to other matches.
     */
    @Override
    public final List<SearchResult> query(String query, int sizeLimit) {
        return query(query, null, sizeLimit, Long.MIN_VALUE);
    }
    
    /**
    *
    * Calls {@link #query(String, boolean, Integer, int, long)} with the prefixSearch field set to false.
    *
    * @param query The query to apply.
    * @param semeneConceptSequence optional - The concept seqeuence of the sememe that you wish to search within.  If null, 
    * searches all indexed content.  This would be set to the concept sequence of {@link MetaData#ENGLISH_DESCRIPTION_ASSEMBLAGE}
    * or the concept sequence {@link MetaData#SCTID} for example.
    * @param sizeLimit The maximum size of the result list.
    * @param targetGeneration target generation that must be included in the search or Long.MIN_VALUE if there is no 
    * need to wait for a target generation.  Long.MAX_VALUE can be passed in to force this query to wait until any 
    * in-progress indexing operations are completed - and then use the latest index.
    * @return a List of {@code SearchResult} that contains the nid of the component that matched, and the score of 
    * that match relative to other matches.
    */
   @Override
    public final List<SearchResult> query(String query, Integer[] semeneConceptSequence, int sizeLimit, Long targetGeneration) {
       return query(query, false, semeneConceptSequence, sizeLimit, targetGeneration);
   }

    /**
     * A generic query API that handles most common cases.  The cases handled for various component property types
     * are detailed below.
     * 
     * NOTE - subclasses of LuceneIndexer may have other query(...) methods that allow for more specific and or complex
     * queries.  Specifically both {@link SememeIndexer} and {@link DescriptionIndexer} have their own 
     * query(...) methods which allow for more advanced queries.
     *
     * @param query The query to apply.
     * @param prefixSearch if true, utilize a search algorithm that is optimized for prefix searching, such as the searching 
     * that would be done to implement a type-ahead style search.  Does not use the Lucene Query parser.  Every term (or token) 
     * that is part of the query string will be required to be found in the result.
     * 
     * Note, it is useful to NOT trim the text of the query before it is sent in - if the last word of the query has a 
     * space character following it, that word will be required as a complete term.  If the last word of the query does not 
     * have a space character following it, that word will be required as a prefix match only.
     * 
     * For example:
     * The query "family test" will return results that contain 'Family Testudinidae'
     * The query "family test " will not match on  'Testudinidae', so that will be excluded.
     * 
     * @param semeneConceptSequence optional - The concept seqeuence of the sememes that you wish to search within.  If null or empty
     * searches all indexed content.  This would be set to the concept sequence of {@link MetaData#ENGLISH_DESCRIPTION_ASSEMBLAGE}
     * or the concept sequence {@link MetaData#SCTID} for example.
     * @param sizeLimit The maximum size of the result list.
     * @param targetGeneration target generation that must be included in the search or Long.MIN_VALUE if there is no need 
     * to wait for a target generation.  Long.MAX_VALUE can be passed in to force this query to wait until any in progress 
     * indexing operations are completed - and then use the latest index.
     *
     * @return a List of {@link SearchResult} that contains the nid of the component that matched, and the score of that match relative 
     * to other matches.
     */
    @Override
    public abstract List<SearchResult> query(String query, boolean prefixSearch, Integer[] sememeConceptSequence, int sizeLimit, Long targetGeneration);

    @Override
    public final void clearIndex() {
        try {
            indexWriter.deleteAll();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void forceMerge() {
        try {
            indexWriter.forceMerge(1);
            referenceManager.maybeRefreshBlocking();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final void closeWriter() {
        try {
            reopenThread.close();
            //We don't shutdown the writer service we are using, because it is the core isaac thread pool.
            //waiting for the future checker service is sufficient to ensure that all write operations are complete.
            luceneWriterFutureCheckerService.shutdown();
            luceneWriterFutureCheckerService.awaitTermination(15, TimeUnit.MINUTES);
            indexWriter.close();
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final void commitWriter() {
        try {
            indexWriter.commit();
            referenceManager.maybeRefreshBlocking();
         } catch (IOException ex) {
                throw new RuntimeException(ex);
         }
    }

    /**
     * Subclasses may call this method with much more specific queries than this generic class is capable of constructing.
     * @param q - the query
     * @param sizeLimit - how many results to return (at most)
     * @param targetGeneration - target generation that must be included in the search or Long.MIN_VALUE if there is no need 
     * to wait for a target generation.  Long.MAX_VALUE can be passed in to force this query to wait until any in progress 
     * indexing operations are completed - and then use the latest index.
     * @param filter - an optional filter on results - if provided, the filter should expect nids, and can return true, if
     * the nid should be allowed in the result, false otherwise.  Note that this may cause large performance slowdowns, depending
     * on the implementation of your filter
     * @return
     */
    protected final List<SearchResult> search(Query q, int sizeLimit, Long targetGeneration, Predicate<Integer> filter) {
        try 
        {
            if (targetGeneration != null && targetGeneration != Long.MIN_VALUE) {
                if (targetGeneration == Long.MAX_VALUE)
                {
                    referenceManager.maybeRefreshBlocking();
                }
                else
                {
                    try
                    {
                        reopenThread.waitForGeneration(targetGeneration);
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
            
            IndexSearcher searcher = referenceManager.acquire();
            
            try 
            {
                log.debug("Running query: {}", q.toString());
                
                //Since the index carries some duplicates by design, which we will remove - get a few extra results up front.
                //so we are more likely to come up with the requested number of results
                long limitWithExtras = sizeLimit + (long)((double)sizeLimit * 0.25d);
                
                int adjustedLimit = (limitWithExtras > Integer.MAX_VALUE ? sizeLimit : (int)limitWithExtras);
                
                TopDocs topDocs;
                if (filter != null)
                {
                    TopDocsFilteredCollector tdf = new TopDocsFilteredCollector(adjustedLimit, searcher, filter);
                    searcher.search(q, tdf);
                    topDocs = tdf.topDocs();
                }
                else
                {
                    topDocs = searcher.search(q, adjustedLimit);
                }
                List<SearchResult> results = new ArrayList<>(topDocs.totalHits);
                HashSet<Integer> includedComponentNids = new HashSet<>();
                
                for (ScoreDoc hit : topDocs.scoreDocs)
                {
                    log.debug("Hit: {} Score: {}", new Object[]{hit.doc, hit.score});
                    
                    Document doc = searcher.doc(hit.doc);
                    int componentNid = doc.getField(FIELD_COMPONENT_NID).numericValue().intValue();
                    if (includedComponentNids.contains(componentNid))
                    {
                        continue;
                    }
                    else
                    {
                        includedComponentNids.add(componentNid);
                        results.add(new ComponentSearchResult(componentNid, hit.score));
                        if (results.size() == sizeLimit)
                        {
                            break;
                        }
                    }
                }
                log.debug("Returning {} results from query", results.size());
                return results;
            } finally {
                referenceManager.release(searcher);
            }
        }   catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * @param nid for the component that the caller wished to wait until it's document is added to the index.
     * @return a {@link IndexedGenerationCallable} object that will block until this indexer has added the 
     * document to the index. The {@link IndexedGenerationCallable#call()} method on the object will return the 
     * index generation that contains the document, which can be used in search calls to make sure the generation
     * is available to the searcher.
     */
    @Override
    public IndexedGenerationCallable getIndexedGenerationCallable(int nid) {
        IndexedGenerationCallable indexedLatch = new IndexedGenerationCallable();
        IndexedGenerationCallable existingIndexedLatch = componentNidLatch.putIfAbsent(nid, indexedLatch);

        if (existingIndexedLatch != null) {
            return existingIndexedLatch;
        }

        return indexedLatch;
    }

    @Override
    public void setEnabled(boolean enabled) {
        enabled_ = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled_;
    }
    
    @Override
    public File getIndexerFolder() {
        return indexFolder_;
    }

    protected void releaseLatch(int latchNid, long indexGeneration)
    {
        IndexedGenerationCallable latch = componentNidLatch.remove(latchNid);

        if (latch != null) {
            latch.setIndexGeneration(indexGeneration);
        }
    }
    protected Query restrictToSememe(Query query, Integer[] sememeConceptSequence)
    {
        ArrayList<Integer> nullSafe = new ArrayList<>();
        if (sememeConceptSequence != null) {
            for (Integer i : sememeConceptSequence) {
                if (i != null) {
                    nullSafe.add(i);
                }
            }
        }
        if (nullSafe.size() > 0) {
            Builder outerWrap = new BooleanQuery.Builder();
            outerWrap.add(query, Occur.MUST);
            Builder wrap = new BooleanQuery.Builder();

            //or together the sememeConceptSequences, but require at least one of them to match.
            for (int i : nullSafe) {
                wrap.add(new TermQuery(new Term(FIELD_SEMEME_ASSEMBLAGE_SEQUENCE, i + "")), Occur.SHOULD);
            }
            outerWrap.add(wrap.build(), Occur.MUST);
            
            return outerWrap.build();
        }
        else
        {
            return query;
        }
    }

    /**
     * Create a query that will match on the specified text using either the WhitespaceAnalyzer or the StandardAnalyzer.
     * Uses the Lucene Query Parser if prefixSearch is false, otherwise, uses a custom prefix algorithm.  
     * See {@link LuceneIndexer#query(String, boolean, Integer, int, Long)} for details on the prefix search algorithm. 
     */
    protected Query buildTokenizedStringQuery(String query, String field, boolean prefixSearch, boolean metadataOnly)
    {
        try
        {
            Builder bq = new BooleanQuery.Builder();
            
            if (metadataOnly)
            {
                bq.add(new TermQuery(new Term(FIELD_CONCEPT_IS_METADATA, FIELD_CONCEPT_IS_METADATA_VALUE)), Occur.MUST);
            }
            
            if (prefixSearch) 
            {
                bq.add(buildPrefixQuery(query,field, new PerFieldAnalyzer()), Occur.SHOULD);
                bq.add(buildPrefixQuery(query,field + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, new PerFieldAnalyzer()), Occur.SHOULD);
            }
            else {
                QueryParser qp1 = new QueryParser(field, new PerFieldAnalyzer());
                qp1.setAllowLeadingWildcard(true);
                bq.add(qp1.parse(query), Occur.SHOULD);
                QueryParser qp2 = new QueryParser(field + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, new PerFieldAnalyzer());
                qp2.setAllowLeadingWildcard(true);
                bq.add(qp2.parse(query), Occur.SHOULD);
            }
            return new BooleanQuery.Builder().add(bq.build(), Occur.MUST).build();
        }
        catch (IOException|ParseException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    protected Query buildPrefixQuery(String searchString, String field, Analyzer analyzer) throws IOException
    {
        StringReader textReader = new StringReader(searchString);
        TokenStream tokenStream = analyzer.tokenStream(field, textReader);
        tokenStream.reset();
        List<String> terms = new ArrayList<>();
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        
        while (tokenStream.incrementToken())
        {
            terms.add(charTermAttribute.toString());
        }
        textReader.close();
        tokenStream.close();
        analyzer.close();
        
        Builder bq = new BooleanQuery.Builder();
        if (terms.size() > 0 && !searchString.endsWith(" "))
        {
            String last = terms.remove(terms.size() - 1);
            bq.add(new PrefixQuery((new Term(field, last))), Occur.MUST);
        }
        terms.stream().forEach((s) -> {
            bq.add(new TermQuery(new Term(field, s)), Occur.MUST);
        });
        
        return bq.build();
    }
    
    @Override
    public final Future<Long> index(ObjectChronology<?> chronicle) {
        return index((() -> new AddDocument(chronicle)), (() -> indexChronicle(chronicle)), chronicle.getNid());
    }
    
    private Future<Long> index(Supplier<AddDocument> documentSupplier, BooleanSupplier indexChronicle, int chronicleNid) {
        if (!enabled_) {
            releaseLatch(chronicleNid, Long.MIN_VALUE);
            return null;
        }

        if (indexChronicle.getAsBoolean()) {
            Future<Long> future = luceneWriterService.submit(documentSupplier.get());

            luceneWriterFutureCheckerService.execute(new FutureChecker(future));

            return future;
        }
        else
        {
            releaseLatch(chronicleNid, Long.MIN_VALUE);
        }

        return unindexedFuture;
    }
    
    private static class UnindexedFuture implements Future<Long> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Long get() throws InterruptedException, ExecutionException {
            return Long.MIN_VALUE;
        }

        @Override
        public Long get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return Long.MIN_VALUE;
        }
    }
    
    /**
     * Class to ensure that any exceptions associated with indexingFutures are properly logged.
     */
    private static class FutureChecker implements Runnable {

        Future<Long> future_;

        public FutureChecker(Future<Long> future) {
            future_ = future;
        }

        @Override
        public void run() {
            try {
                future_.get();
            } catch (InterruptedException | ExecutionException ex) {
                log.fatal("Unexpected error in future checker!", ex);
            }
        }
    }
    
    private class AddDocument implements Callable<Long> {

        ObjectChronology<?> chronicle = null;

        public AddDocument(ObjectChronology<?> chronicle) {
            this.chronicle = chronicle;
        }
        
        public int getNid() {
            return chronicle.getNid();
        }

        @Override
        public Long call() throws Exception {
            Document doc = new Document();
            doc.add(new LegacyIntField(FIELD_COMPONENT_NID, chronicle.getNid(), LuceneIndexer.FIELD_TYPE_INT_STORED_NOT_INDEXED));

            addFields(chronicle, doc);

            // Note that the addDocument operation could cause duplicate documents to be
            // added to the index if a new luceneVersion is added after initial index
            // creation. It does this to avoid the performance penalty of
            // finding and deleting documents prior to inserting a new one.
            //
            // At this point, the number of duplicates should be
            // small, and we are willing to accept a small number of duplicates
            // because the new versions are additive (we don't allow deletion of content)
            // so the search results will be the same. Duplicates can be removed
            // by regenerating the index.
            long indexGeneration = indexWriter.addDocument(doc);

            releaseLatch(getNid(), indexGeneration);

            return indexGeneration;
        }
    }
    
    @Override
    public HashMap<String, Integer> reportIndexedItems() {
        HashMap<String, Integer> result = new HashMap<String, Integer>();
        indexedComponentStatistics.forEach((name, value) ->
        {
            result.put(name, value.get());
        });
        return result;
    }

    @Override
    public void clearIndexedStatistics() {
        indexedComponentStatistics.clear();
    }
    

    @Override
    public List<ConceptSearchResult> mergeResultsOnConcept(List<SearchResult> searchResult) {
        HashMap<Integer, ConceptSearchResult> merged = new HashMap<>();
        List<ConceptSearchResult> result = new ArrayList<>();
        for (SearchResult sr : searchResult) {
            int conSequence = Frills.findConcept(sr.getNid());
            if (conSequence < 0) {
                log.error("Failed to find a concept that references nid " + sr.getNid());
            }
            else if (merged.containsKey(conSequence)) {
                merged.get(conSequence).merge(sr);
            }
            else {
                ConceptSearchResult csr = new ConceptSearchResult(conSequence, sr.getNid(), sr.getScore());
                merged.put(conSequence, csr);
                result.add(csr);
            }
        }
        return result;
    }

    protected void incrementIndexedItemCount(String name) {
        AtomicInteger temp = indexedComponentStatistics.get(name);
        if (temp == null) {
            try {
                indexedComponentStatisticsBlock.acquireUninterruptibly();
                temp = indexedComponentStatistics.get(name);
                if (temp == null) {
                    temp = new AtomicInteger(0);
                    indexedComponentStatistics.put(name, temp);
                }
            }
            finally{
                indexedComponentStatisticsBlock.release();
            }
        }
        temp.incrementAndGet();
    }

    @PreDestroy
    private void stopMe() {
        log.info("Stopping " + getIndexerName() + " pre-destroy. ");
        commitWriter();
        closeWriter();
    }
    

    @PostConstruct
    private void startMe() {
        log.info("Starting " + getIndexerName() + " post-construct");
        if (reindexRequired)
        {
            try
            {
                log.info("Starting reindex of '" + getIndexerName() + "' due to out-of-date index");
                GenerateIndexes gi = new GenerateIndexes(this);
                LookupService.getService(WorkExecutors.class).getExecutor().execute(gi);
                gi.get();
            } 
            catch (Exception e) 
            {
                log.fatal("bah!", e);
                throw new RuntimeException(e.getMessage());
            }
         }
         log.info("Reindex complete");
    }

    protected abstract boolean indexChronicle(ObjectChronology<?> chronicle);
    protected abstract void addFields(ObjectChronology<?> chronicle, Document doc);

    @Override
    public void clearDatabaseValidityValue() {
        // Reset to enforce analysis
        databaseValidity = DatabaseValidity.NOT_SET;
    }

    @Override
    public DatabaseValidity getDatabaseValidityStatus() {
        return databaseValidity;
    }

    @Override
    public Path getDatabaseFolder() {
        return indexFolder_.toPath(); 
	}

	protected void addField(Document doc, String fieldName, String value, boolean tokenize) {
		// index twice per field - once with the standard analyzer, once with
		// the whitespace analyzer.
		if (tokenize) 
		{
			doc.add(new TextField(fieldName, value, Field.Store.NO));
		}
		doc.add(new TextField(fieldName + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, value, Field.Store.NO));
	}

	/**
	 * Indexing the UUID of the ISAAC modules for each sememe.
	 * 
	 * @param doc - The Lucene document/record to index
	 * @param moduleSeq - The ISAAC module sequence identifier
	 */
	protected void indexModule(Document doc, int moduleSeq) 
	{
		UUID moduleUuid = Get.conceptSpecification(moduleSeq).getPrimordialUuid();
		addField(doc, FIELD_INDEXED_MODULE_STRING_VALUE, moduleUuid.toString(), false);
		incrementIndexedItemCount("Module"); // - " + moduleUuid
	}

	/**
	 * Indexing the UUID of the ISAAC paths for each sememe.
	 * 
	 * @param doc - The Lucene document/record to index
	 * @param pathSeq - The ISAAC module sequence identifier
	 */
	protected void indexPath(Document doc, int pathSeq) 
	{
		UUID pathUuid = Get.conceptSpecification(pathSeq).getPrimordialUuid();
		addField(doc, FIELD_INDEXED_PATH_STRING_VALUE, pathUuid.toString(), false);
		incrementIndexedItemCount("Path"); // - " + pathUuid
	}
}

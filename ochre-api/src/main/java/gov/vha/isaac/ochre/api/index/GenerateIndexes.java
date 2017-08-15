/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.vha.isaac.ochre.api.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.task.TimedTask;

/**
 *
 * @author kec
 */
public class GenerateIndexes extends TimedTask<Void> {
    private static final Logger log = LogManager.getLogger();

    List<IndexServiceBI> indexers = new ArrayList<>();
    long componentCount;
    AtomicLong processed = new AtomicLong(0);

    public GenerateIndexes(Class<?> ... indexersToReindex) {
        updateTitle("Index generation");
        updateProgress(-1, Long.MAX_VALUE); // Indeterminate progress
        if (indexersToReindex == null || indexersToReindex.length == 0)
        {
            indexers = LookupService.get().getAllServices(IndexServiceBI.class);
        }
        else
        {
            for (Class<?> clazz : indexersToReindex)
            {
                if (!IndexServiceBI.class.isAssignableFrom(clazz))
                {
                    throw new RuntimeException("Invalid Class passed in to the index generator.  Classes must implement IndexService ");
                }
                IndexServiceBI temp = (IndexServiceBI)LookupService.get().getService(clazz);
                if (temp != null)
                {
                    indexers.add(temp);
                }
            }
        }
        
        List<IndexStatusListenerBI> islList = LookupService.get().getAllServices(IndexStatusListenerBI.class);
        for (IndexServiceBI i : indexers) {
            if (islList != null)
            {
                for (IndexStatusListenerBI isl : islList)
                {
                    isl.reindexBegan(i);
                }
            }            
            log.info("Clearing index for: " + i.getIndexerName());
            i.clearIndex();
            i.clearIndexedStatistics();
        }
    }
    
    /**
     * Used to avoid circular dependencies during a re-index upon startup
     * @param indexersToReindex
     */
    public GenerateIndexes(IndexServiceBI ...indexersToReindex) {
        updateTitle("Index generation");
        updateProgress(-1, Long.MAX_VALUE); // Indeterminate progress
        
        if (indexersToReindex != null)
        {
            for (IndexServiceBI i : indexersToReindex)
            {
                indexers.add(i);
            }
        }
        
        List<IndexStatusListenerBI> islList = LookupService.get().getAllServices(IndexStatusListenerBI.class);
        for (IndexServiceBI i : indexers) {
            if (islList != null)
            {
                for (IndexStatusListenerBI isl : islList)
                {
                    isl.reindexBegan(i);
                }
            }            
            log.info("Clearing index for: " + i.getIndexerName());
            i.clearIndex();
            i.clearIndexedStatistics();
        }
    }

    @Override
    protected Void call() throws Exception {
        Get.activeTasks().add(this);
        try {
            //We only need to indexes sememes now
            //In the future, there may be a need for indexing Concepts from the concept service - for instance, if we wanted to index the concepts
            //by user, or by some other attribute that is attached to the concept.  But there simply isn't much on the concept at present, and I have
            //no use case for indexing the concepts.  The IndexService APIs would need enhancement if we allowed indexing things other than sememes.
            long sememeCount = (int) Get.identifierService().getSememeSequenceStream().count();
            log.info("Sememes to index: " + sememeCount);
            componentCount = sememeCount;
            
            for (SememeChronology<?> sememe : (Iterable<SememeChronology<? extends SememeVersion<?>>>) Get.sememeService().getParallelSememeStream()::iterator)
            {
                for (IndexServiceBI i : indexers) {
                    try {
                        if (sememe == null) {
                            //noop - this error is already logged elsewhere.  Just skip.
                        }
                        else {
                            i.index(sememe).get();
                        }
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                updateProcessedCount();
            }
            
            List<IndexStatusListenerBI> islList = LookupService.get().getAllServices(IndexStatusListenerBI.class);

            for (IndexServiceBI i : indexers) {
                if (islList != null)
                {
                    for(IndexStatusListenerBI isl : islList) {
                        isl.reindexCompleted(i);
                    }
                }
                i.commitWriter();
                i.forceMerge();
                log.info(i.getIndexerName() + " indexing complete.  Statistics follow:");
                
                for (Map.Entry<String, Integer> entry : i.reportIndexedItems().entrySet()){
                    log.info(entry.getKey() + ": " + entry.getValue());
                }
                
                i.clearIndexedStatistics();
            }
            return null;
        } finally {
           Get.activeTasks().remove(this);
        }
    }

    protected void updateProcessedCount() {
        long processedCount = processed.incrementAndGet();
        if (processedCount % 1000 == 0) {
            updateProgress(processedCount, componentCount);
            updateMessage(String.format("Indexed %,d components...", processedCount));
            //We were committing too often every 1000 components, it was bad for performance.
            if (processedCount % 100000 == 0)
            {
                for (IndexServiceBI i : indexers) {
                    i.commitWriter();
                }
                log.info("Indexed " + processedCount + " sememes");
            }
        }
    }
}

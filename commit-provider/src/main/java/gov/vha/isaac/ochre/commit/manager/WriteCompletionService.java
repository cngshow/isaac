/*
 * Copyright 2015 kec.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.commit.manager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.concurrent.Task;

/**
 * This class executes the jobs to write to disk, and also has a single thread running to call .get() on 
 * the future for each job submitted, to ensure that any errors are logged / warned (if the caller didn't bother
 * to call get)
 * 
 * Previous versions of this class used the ExecuterCompletionService stuff... but that turns out to be fundamentally
 * broken in combination with JavaFX Task objects - you end up with a Task and a Future, and the Task never gets completion 
 * notification.
 * 
 * @author kec
 * @author darmbrust
 */
public class WriteCompletionService implements Runnable {
    private static final Logger log = LogManager.getLogger();
    
    private ExecutorService writeConceptCompletionServiceThread;
    private ExecutorCompletionService<Void> conversionService;
    private ExecutorService workerPool;
    private boolean run = false;
    
    protected Future<Void> submit(Task<Void> task)
    {
        return conversionService.submit(task, null);
    }
    
    @Override
    public void run() {
        log.info("WriteCompletionService starting");
        while (run) {
            try {
                conversionService.take().get();
            } catch (InterruptedException ex) {
                if (run) {
                    //Only warn if we were not asked to shutdown
                    log.warn(ex.getLocalizedMessage(), ex);
                }
            } catch (ExecutionException ex) {
                log.error(ex.getLocalizedMessage(), ex);
            }
        }
        conversionService = null;
        writeConceptCompletionServiceThread = null;
        workerPool = null;
        log.info("WriteCompletionService closed");
    }

    public void start() {
        log.info("Starting WriteCompletionService");
        run = true;
        workerPool = Executors.newFixedThreadPool(4, (Runnable r) -> {
            return new Thread(r, "writeCommitDataPool");
        });
        
        conversionService = new ExecutorCompletionService<>(workerPool);

        writeConceptCompletionServiceThread = Executors.newSingleThreadExecutor((Runnable r) -> {
            return new Thread(r, "writeCompletionService");
        });
        writeConceptCompletionServiceThread.submit(this);
    }
    
    public void stop() {
        log.info("Stopping WriteCompletionService");
        run = false;
        writeConceptCompletionServiceThread.shutdown();
        workerPool.shutdown();
    }
}

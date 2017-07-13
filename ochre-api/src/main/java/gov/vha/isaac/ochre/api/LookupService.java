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
package gov.vha.isaac.ochre.api;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.hk2.runlevel.RunLevelFuture;
import com.sun.javafx.application.PlatformImpl;
import gov.va.oia.HK2Utilities.HK2RuntimeInitializer;
import gov.vha.isaac.ochre.api.DatabaseServices.DatabaseValidity;
import gov.vha.isaac.ochre.api.constants.Constants;
import gov.vha.isaac.ochre.api.util.HeadlessToolkit;

/**
 *
 * @author kec
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@SuppressWarnings("restriction")
public class LookupService {
    private static final Logger LOG = LogManager.getLogger();
    private static volatile ServiceLocator looker = null;
    private static volatile boolean fxPlatformUp = false;
    public static final int SL_L5_ISAAC_DEPENDENTS_RUNLEVEL = 5;  //Anything that depends on issac as a whole to be started should be 5 - this is the fully-started state.
    public static final int SL_L4_ISAAC_STARTED_RUNLEVEL = 4;  //at level 3 and 4, secondary isaac services start, such as changeset providers, etc.
    public static final int SL_L3 = 3; 
    public static final int SL_L2_DATABASE_SERVICES_STARTED_RUNLEVEL = 2;  //In general, ISAAC data-store services start between 0 and 2, in an isaac specific order.
    //Below 0, we have utility stuff... no ISAAC services.
    public static final int SL_L1 = 1; 
    public static final int SL_L0 = 0; 
    public static final int SL_NEG_1_METADATA_STORE_STARTED_RUNLEVEL = -1; 
    public static final int SL_NEG_2_WORKERS_STARTED_RUNLEVEL = -2;
    public static final int SL_NEG_3_SYSTEM_STOPPED_RUNLEVEL = -3;
    private static final Object STARTUP_LOCK = new Object();
    private static DatabaseValidity discoveredValidityValue = null;

    /**
     * @return the {@link ServiceLocator} that is managing this ISAAC instance
     */
    public static ServiceLocator get() {
        if (looker == null) {
            synchronized (STARTUP_LOCK) {
                if (looker == null) {
                    startupFxPlatform();
                    ArrayList<String> packagesToSearch = new ArrayList<>(Arrays.asList("gov.va", "gov.vha", "org.ihtsdo", "org.glassfish", "com.informatics"));

                    boolean readInhabitantFiles = Boolean.valueOf(System.getProperty(Constants.READ_INHABITANT_FILES, "false"));
                    if (System.getProperty(Constants.EXTRA_PACKAGES_TO_SEARCH) != null) {
                        String[] extraPackagesToSearch = System.getProperty(Constants.EXTRA_PACKAGES_TO_SEARCH).split(";");
                        packagesToSearch.addAll(Arrays.asList(extraPackagesToSearch));
                    }
                    try {
                        String[] packages = packagesToSearch.toArray(new String[]{});
                        LOG.info("Looking for HK2 annotations " + (readInhabitantFiles ? "from inhabitant files" : "skipping inhabitant files") 
                                + "; and scanning in the packages: " + Arrays.toString(packages));

                        ServiceLocator temp = HK2RuntimeInitializer.init("ISAAC", readInhabitantFiles, packages);
                        if (looker != null)
                        {
                            RuntimeException e = new RuntimeException("RECURSIVE Lookup Service Reference!  Ensure that there are no static variables "
                                    + "objects utilizing the LookupService during their init!");
                            e.printStackTrace();
                            throw e;
                        }
                        looker = temp;
                        
                        LOG.info("HK2 initialized.  Identifed " + looker.getAllServiceHandles((criteria) -> {return true;}).size() + " services");
                    }
                    catch (IOException | ClassNotFoundException | MultiException e) {
                        throw new RuntimeException(e);
                    }
                    try
                    {
                        LookupService.startupWorkExecutors();
                    }
                    catch (Exception e)
                    {
                        RuntimeException ex = new RuntimeException("Unexpected error trying to come up to the work executors level, possible classpath problems!", e);
                        ex.printStackTrace();  //We are in a world of hurt if this happens, make sure this exception makes it out somewhere, and doesn't get eaten.
                        throw ex;
                    }
                }
            }
        }
        return looker;
    }
    
    /**
     * Return true if and only if any service implements the requested contract or implementation class.
     * @see ServiceLocator#getService(Class, Annotation...) 
     */
    public static boolean hasService(Class<?> contractOrImpl) {
        return get().getServiceHandle(contractOrImpl, new Annotation[0]) != null;
    }

    /**
     * Return true if and only if there is a service with the specified name.  If no service with the specified name is available, 
     * this returns false (even if there is a service with another name [or no name] which would meet the contract)
     * 
     * @see ServiceLocator#getService(Class, String, Annotation...)
     * 
     * @param contractOrService May not be null, and is the contract or concrete implementation to get the best instance of
     * @param name May not be null or empty
     */
    public static boolean hasService(Class<?> contractOrService, String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("You must specify a service name to use this method");
        }
        return get().getServiceHandle(contractOrService, name, new Annotation[0]) != null;
    }

    /**
     * Return the highest ranked service that implements the requested contract or implementation class.
     * @see ServiceLocator#getService(Class, Annotation...) 
     */
    public static <T> T getService(Class<T> contractOrImpl) {
        T service = get().getService(contractOrImpl, new Annotation[0]);
        LOG.debug("LookupService returning {} for {}", (service != null ? service.getClass().getName() : null), contractOrImpl.getName());

        return service;
    }
    
    /**
     * Find the best ranked service with the specified name.  If no service with the specified name is available, 
     * this returns null (even if there is a service with another name [or no name] which would meet the contract)
     * 
     * @see ServiceLocator#getService(Class, String, Annotation...)
     * 
     * @param contractOrService May not be null, and is the contract or concrete implementation to get the best instance of
     * @param name May not be null or empty
     */
    public static <T> T getService(Class<T> contractOrService, String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("You must specify a service name to use this method");
        }
        T service = get().getService(contractOrService, name, new Annotation[0]);
        
        LOG.debug("LookupService returning {} for {} with name={}", (service != null ? service.getClass().getName() : null), contractOrService.getName(), name);

        return service;
    }
    
    /**
     * Find a service by name, and automatically fall back to any service which implements the contract if the named service was not available.
     * 
     * @param contractOrService May not be null, and is the contract or concrete implementation to get the best instance of
     * @param name May be null (to indicate any name is ok), and is the name of the implementation to be returned
     */
    public static <T> T getNamedServiceIfPossible(Class<T> contractOrService, String name) {
        T service = null;
        if (StringUtils.isEmpty(name)) {
            service = get().getService(contractOrService);
        }
        else {
            service = get().getService(contractOrService, name);
            if (service == null) {
                service = get().getService(contractOrService);
            }
        }
        
        LOG.debug("LookupService returning {} for {} with name={}", (service != null ? service.getClass().getName() : null), contractOrService.getName(), name);

        return service;
    }
    
    public static int getCurrentRunLevel() {
        return getService(RunLevelController.class).getCurrentRunLevel();
    }

    public static void setRunLevel(int runLevel) {
        RunLevelController rlc = getService(RunLevelController.class);
        int current = rlc.getCurrentRunLevel();
        if (current > runLevel) {
            //Make sure we aren't still proceeding somewhere, if so, we need to wait...
            RunLevelFuture rlf = rlc.getCurrentProceeding();
            if (rlf != null)
            {
                LOG.info("Attempting to cancel previous runlevel request");
                rlf.cancel(true);
                try {
                    rlf.get();
                }
                catch (InterruptedException | ExecutionException e) {
                    // noop
                }
            }
            get().getAllServiceHandles(OchreCache.class).forEach(handle ->
            {
                if (handle.isActive()) {
                    LOG.info("Clear cache for: " + handle.getActiveDescriptor().getImplementation());
                    handle.getService().reset();
                }
            });
            get().getAllServices(OchreCache.class).forEach((cache) -> {cache.reset();});
        }
        getService(RunLevelController.class).proceedTo(runLevel);
    }
    
    /**
     * Start the WorkExecutor services (without starting ISAAC core services), blocking until started (or failed). 
     */
    public static void startupWorkExecutors() {
        if (getService(RunLevelController.class).getCurrentRunLevel() < SL_NEG_2_WORKERS_STARTED_RUNLEVEL) {
            setRunLevel(SL_NEG_2_WORKERS_STARTED_RUNLEVEL);
        }
    }
    
    /**
     * Start the Metadata services (without starting ISAAC core services), blocking until started (or failed). 
     */
    public static void startupMetadataStore() {
        if (getService(RunLevelController.class).getCurrentRunLevel() < SL_NEG_1_METADATA_STORE_STARTED_RUNLEVEL) {
            setRunLevel(SL_NEG_1_METADATA_STORE_STARTED_RUNLEVEL);
        }
    }
    
    /**
     * Start all core isaac services, blocking until started (or failed)
     */
    public static void startupIsaac() {
        try {
        	//So Fortify does not complain about Locale dependent comparison
        	//when the application uses .equals or 
        	Locale.setDefault(Locale.US);
        	
            // Set run level to startup database and associated services running on top of database
            setRunLevel(SL_L2_DATABASE_SERVICES_STARTED_RUNLEVEL);

            // Validate that databases and associated services directories uniformly exist and are uniformly populated during startup 
            validateDatabaseFolderStatus();

            // If database is validated, startup remaining run levels
            setRunLevel(SL_L4_ISAAC_STARTED_RUNLEVEL);

            setRunLevel(SL_L5_ISAAC_DEPENDENTS_RUNLEVEL);
        } catch (Exception e) {
            // Will inform calling routines that database is corrupt
            throw e;
        } finally {
            // Regardless of successful or failed startup, reset database and lucene services' validityCalculated flag for next startup attempt
            get().getAllServiceHandles(DatabaseServices.class).forEach(handle -> {
                if (handle.isActive()) {
                    handle.getService().clearDatabaseValidityValue();
                }
            });
        }
    }

    /* 
     * Check database directories. Either all must exist or none may exist. Inconsistent state suggests database
     * corruption
     */
    private static void validateDatabaseFolderStatus() {
        discoveredValidityValue = null;

        get().getAllServiceHandles(DatabaseServices.class).forEach(handle -> {
            if (handle.isActive()) {
                if (discoveredValidityValue == null) {
                    // Initial time through. All other database directories and lucene directories must have same state
                    discoveredValidityValue = handle.getService().getDatabaseValidityStatus();
                    LOG.info("First database service handler (" + handle.getActiveDescriptor().getImplementation()
                            + ") has database validity value: " + discoveredValidityValue);
                } else {
                    // Verify database directories have same state as identified in first time through
                    LOG.info("Comparing database validity value for Provider " + handle.getActiveDescriptor().getImplementation() + " to see if consistent at startup.  Status: "
                            + handle.getService().getDatabaseValidityStatus());

                    if (discoveredValidityValue != handle.getService().getDatabaseValidityStatus()) {
                        // Inconsistency discovered
                        throw new RuntimeException("Database Corruption Observed: Provider " + handle.getActiveDescriptor().getImplementation()
                                + " has inconsistent database validity value prior to startup");
                    }
                }
            }
        });
    }

    /**
     * Stop all core isaac service, blocking until stopped (or failed)
     */
    public static void shutdownIsaac() {
        setRunLevel(SL_NEG_2_WORKERS_STARTED_RUNLEVEL);
        // Fully release any system locks to database
        System.gc();
    }
    
    /**
     * Stop all system services, blocking until stopped (or failed)
     */
   public static void shutdownSystem () {
       if (isInitialized()) {
           setRunLevel(SL_NEG_3_SYSTEM_STOPPED_RUNLEVEL);
           looker.shutdown();
           ServiceLocatorFactory.getInstance().destroy(looker);
           looker = null;
           
        }
    }
    
    /**
     * start all core isaac services in a background thread, returning immediately.  
     * @param callWhenStartComplete (optional) - if provided,  a call back will be provided
     * notifying of successfully start of ISAAC, or providing the Exception, if the startup sequence failed.
     */
    public static void startupIsaac(BiConsumer<Boolean, Exception> callWhenStartComplete) {
        LOG.info("Background starting ISAAC services");
        Thread backgroundLoad = new Thread(() ->
        {
            try {
                startupIsaac();
                LOG.info("Background start complete - runlevel now " + getService(RunLevelController.class).getCurrentRunLevel());
                if (callWhenStartComplete != null) {
                    callWhenStartComplete.accept(isIsaacStarted(), null);
                }
            }
            catch (Exception e) {
                LOG.warn("Background start failed - runlevel now " + getService(RunLevelController.class).getCurrentRunLevel(), e);
                if (callWhenStartComplete != null) {
                    callWhenStartComplete.accept(false, e);
                }
            }
        }, "Datastore init thread");
        backgroundLoad.start();
    }
    
    public static boolean isIsaacStarted() {
        return isInitialized() ? getService(RunLevelController.class).getCurrentRunLevel() >= SL_L4_ISAAC_STARTED_RUNLEVEL : false;
    }
    
    public static boolean isInitialized() {
        return looker != null;
    }
    
    /**
     * This is automatically done when a typical ISAAC pattern is utilized.  This method is only exposed as public for obscure use cases where 
     * we want to utilize the javaFX task API, but are not looking to start up HK2 and other various ISAAC services.
     */
    public static void startupFxPlatform() {
        if (!fxPlatformUp) {
            LOG.debug("FxPlatform is not yet up - obtaining lock");
            synchronized (STARTUP_LOCK) {
                LOG.debug("Lock obtained, starting fxPlatform");
                if (!fxPlatformUp) {
                    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
                    if (GraphicsEnvironment.isHeadless()) {
                        LOG.info("Installing headless toolkit");
                        HeadlessToolkit.installToolkit();
                    }
                    LOG.debug("Starting JavaFX Platform");
                    PlatformImpl.startup(() -> {
                        // No need to do anything here
                    });
                    fxPlatformUp = true;
                }
            }
        }
    }
}
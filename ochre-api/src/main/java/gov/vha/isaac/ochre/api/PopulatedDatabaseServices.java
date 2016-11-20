package gov.vha.isaac.ochre.api;

import org.jvnet.hk2.annotations.Contract;

/**
 * Contract for database caches that need to be post-processed when the services are started specifically analyzing their database contents.
 * 
 * @author Jesse Efron
 */
@Contract
public interface PopulatedDatabaseServices extends DatabaseServices {
    boolean isPopulated();
}

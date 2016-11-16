package gov.vha.isaac.ochre.api;

import org.jvnet.hk2.annotations.Contract;

/**
 * Contract for database caches that need to be post-processed when the services
 * are started. 
 * @author kec
 */
@Contract
public interface DatabaseServices {
	boolean databaseExistsBeforeStartup();
}

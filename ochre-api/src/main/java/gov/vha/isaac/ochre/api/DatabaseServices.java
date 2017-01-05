package gov.vha.isaac.ochre.api;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jvnet.hk2.annotations.Contract;

/**
 * Contract used to validate that databases & lucene directories uniformly exist and are uniformly populated during startup. If fails, signals that
 * database is corrupted and force a pull of new database. Launched via {@link LookupService}
 * 
 * @author Jesse Efron
 */
@Contract
public interface DatabaseServices {
    public enum DatabaseValidity {
        NOT_SET, MISSING_DIRECTORY, EMPTY_DIRECTORY, POPULATED_DIRECTORY
    };

    
    /*
     * Clear flag indicating that database has had its validity calculated. Will force a real-time investigation second time through
     */
    void clearDatabaseValidityValue();

    /*
     * Flag indicating that folder path of the database.
     */
    public Path getDatabaseFolder();
    
    /*
     * get database validity status
     */
    public DatabaseValidity getDatabaseValidityStatus();
    
}

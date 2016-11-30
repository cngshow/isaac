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
        MISSING_DIRECTORY, EMPTY_DIRECTORY, POPULATED_DIRECTORY
    };

    /*
     * Used by provider to uniformly identify what the Database Validity Value is.
     */
    default DatabaseValidity getDatabaseValidity() {
        if (isValidityCalculated()) {
            if (isDatabaseMissing()) {
                return DatabaseValidity.MISSING_DIRECTORY;
            } else {
                if (isDatabasePopulated()) {
                    return DatabaseValidity.POPULATED_DIRECTORY;
                } else {
                    return DatabaseValidity.EMPTY_DIRECTORY;
                }
            }
        } else {
            // Second time processing (due to download of new database).
            if (!Files.exists(getDatabaseFolder())) {
                return DatabaseValidity.MISSING_DIRECTORY;
            } else {
                return DatabaseValidity.POPULATED_DIRECTORY;
            }
        }
    }

    /*
     * Clear flag indicating that database has had its validity calculated. Will force a real-time investigation second time through
     */
    void clearDatabaseValidityValue();

    /*
     * Flag indicating that database has had its validity calculated.
     */
    public boolean isValidityCalculated();

    /*
     * Flag indicating that during database validity calculation, database folder wasn't found.
     */
    public boolean isDatabaseMissing();

    /*
     * Flag indicating that during database validity calculation, database folder wasn't fully populated with the expected files.
     */
    public boolean isDatabasePopulated();

    /*
     * Flag indicating that folder path of the database.
     */
    public Path getDatabaseFolder();
}

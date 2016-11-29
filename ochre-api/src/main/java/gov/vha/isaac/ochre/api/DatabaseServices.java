package gov.vha.isaac.ochre.api;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jvnet.hk2.annotations.Contract;

/**
 * Contract for database caches that need to be post-processed when the services are started.
 * 
 * @author Jesse Efron
 */
@Contract
public interface DatabaseServices {
    public enum DatabaseValidity { MISSING_DIRECTORY, EMPTY_DIRECTORY, POPULATED_DIRECTORY};

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

    void clearDatabaseValidityValue();

    public boolean isValidityCalculated();

    public boolean isDatabaseMissing();

    public boolean isDatabasePopulated();

    public Path getDatabaseFolder();
}

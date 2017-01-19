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
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.hk2.annotations.Contract;
import gov.vha.isaac.ochre.api.constants.Constants;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableEditCoordinate;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableLanguageCoordinate;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableLogicCoordinate;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableStampCoordinate;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableTaxonomyCoordinate;

/**
 * An interface used for system configuration. Services started by the
 * {@link LookupService} will utilize an implementation of this service in order
 * to configure themselves.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Contract
public interface ConfigurationService {

    /**
     * @return The root folder of the database - the returned path should contain 
     * subfolders of
     * {@link Constants#DEFAULT_CHRONICLE_FOLDER} and
     * {@link Constants#DEFAULT_SEARCH_FOLDER}.
     *
     *
     * This method will return (in the following order):
     *
     * - The value specified by a call to {@link #setDataStoreFolderPath(Path)}
     * - a path constructed from the value of
     * {@link Constants#DATA_STORE_ROOT_LOCATION_PROPERTY} if
     * {@link #setDataStoreFolderPath(Path)} was never called 
     * - Nothing if
     * {@link Constants#DATA_STORE_ROOT_LOCATION_PROPERTY} has not been set.
     *
     * If a value is returned, the returned path will exist on disk at the time
     * that this method returns.
     */
    public Optional<Path> getDataStoreFolderPath();

    /**
     * Specify the root folder of the database. The specified folder should
     * contain subfolders of {@link Constants#DEFAULT_CHRONICLE_FOLDER} and
     * {@link Constants#DEFAULT_SEARCH_FOLDER}.
     *
     * This method can only be utilized prior to the first call to
     * {@link LookupService#startupIsaac()}
     *
     * @param dataStoreFolderPath
     * @throws IllegalStateException if this is called after the system has
     * already started.
     * @throws IllegalArgumentException if the provided dbFolderPath is an
     * existing file, rather than a folder.
     */
    public void setDataStoreFolderPath(Path dataStoreFolderPath) throws IllegalStateException, IllegalArgumentException;

    /**
     * @return The root folder of the database - one would expect to find a
     * data-store specific folder such as "cradle" inside this folder. The
     * default implementation returns the result of
     * {@link #getDataStoreFolderPath()} + {@link Constants#DEFAULT_CHRONICLE_FOLDER}
     *
     * The returned path exists on disk at the time that this method returns.
     */
    public default Path getChronicleFolderPath() {
        Path result;
        Optional<Path> rootPath = getDataStoreFolderPath();
        if (!rootPath.isPresent()) {
            throw new IllegalStateException("The ConfigurationService implementation has not been configured by a call to setDataStoreFolderPath(),"
                    + " and the system property " + Constants.DATA_STORE_ROOT_LOCATION_PROPERTY + " has not been set.  Cannot construct the chronicle folder path.");
        } else {
            result = rootPath.get().resolve(Constants.DEFAULT_CHRONICLE_FOLDER);
        }
        try {
            Files.createDirectories(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * @return The root folder of the search data store - one would expect to
     * find a data-store specific folder such as "lucene" inside this folder.
     * The default implementation returns either:
     *
     * A path as specified exactly via
     * {@link Constants#SEARCH_ROOT_LOCATION_PROPERTY} (if the property is set)
     * or the result of
     * {@link #getDataStoreFolderPath()} + {@link Constants#DEFAULT_SEARCH_FOLDER}
     *
     * The returned path exists on disk at the time that this method returns.
     */
    public default Path getSearchFolderPath() {
        Path result;
        Optional<Path> rootPath = getDataStoreFolderPath();
        if (!rootPath.isPresent()) {
            throw new IllegalStateException("The ConfigurationService implementation has not been configured by a call to setDataStoreFolderPath(),"
                    + " and the system property " + Constants.DATA_STORE_ROOT_LOCATION_PROPERTY + " has not been set.  Cannot construct the search folder path.");
        } else {
            result = rootPath.get().resolve(Constants.DEFAULT_SEARCH_FOLDER);
        }
        try {
            Files.createDirectories(result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Sets the default user for editing and role-based access control. When
     * changed, other default objects that reference this object will be updated
     * accordingly. Default: The value to use if another value is not provided.
     *
     * @param conceptId either a nid or conceptSequence
     */
    void setDefaultUser(int conceptId);

    /**
     * Sets the default module for editing operations. When changed, other
     * default objects that reference this object will be updated accordingly.
     * Default: The value to use if another value is not provided.
     *
     * @param conceptId either a nid or conceptSequence
     */
    void setDefaultModule(int conceptId);

    /**
     * Sets the default path for editing operations. When changed, other default
     * objects that reference this object will be updated accordingly. Default:
     * The value to use if another value is not provided.
     *
     * @param conceptId either a nid or conceptSequence
     */
    void setDefaultPath(int conceptId);

    /**
     * Sets the default language for description retrieval. When changed, other
     * default objects that reference this object will be updated accordingly.
     * Default: The value to use if another value is not provided.
     *
     * @param conceptId either a nid or conceptSequence
     */
    void setDefaultLanguage(int conceptId);

    /**
     * Sets the default dialect preference list for description retrieval. When
     * changed, other default objects that reference this object will be updated
     * accordingly. Default: The value to use if another value is not provided.
     *
     * @param dialectAssemblagePreferenceList prioritized preference list of
     * dialect assemblage sequences
     */
    void setDefaultDialectAssemblagePreferenceList(int[] dialectAssemblagePreferenceList);

    /**
     * Sets the default description type preference list for description
     * retrieval. When changed, other default objects that reference this object
     * will be updated accordingly. Default: The value to use if another value
     * is not provided.
     *
     * @param descriptionTypePreferenceList prioritized preference list of
     * description type sequences
     */
    void setDefaultDescriptionTypePreferenceList(int[] descriptionTypePreferenceList);

    /**
     * Sets the default stated definition assemblage. When changed, other
     * default objects that reference this object will be updated accordingly.
     * Default: The value to use if another value is not provided.
     *
     * @param conceptId either a nid or conceptSequence
     */
    void setDefaultStatedAssemblage(int conceptId);

    /**
     * Sets the default inferred definition assemblage. When changed, other
     * default objects that reference this object will be updated accordingly.
     * Default: The value to use if another value is not provided.
     *
     * @param conceptId either a nid or conceptSequence
     */
    void setDefaultInferredAssemblage(int conceptId);

    /**
     * Sets the default description-logic profile. When changed, other default
     * objects that reference this object will be updated accordingly. Default:
     * The value to use if another value is not provided.
     *
     * @param conceptId either a nid or conceptSequence
     */
    void setDefaultDescriptionLogicProfile(int conceptId);

    /**
     * Sets the default classifier. When changed, other default objects that
     * reference this object will be updated accordingly. Default: The value to
     * use if another value is not provided.
     *
     * @param conceptId
     */
    void setDefaultClassifier(int conceptId);

    /**
     * Sets the default time for viewing versions of components When changed,
     * other default objects that reference this object will be updated
     * accordingly. Default: The value to use if another value is not provided.
     *
     * @param timeInMs Time in milliseconds since unix epoch. Long.MAX_VALUE is
     * used to represent the latest versions.
     */
    void setDefaultTime(long timeInMs);

    /**
     *
     * @return an {@code ObservableEditCoordinate} based on the configuration
     * defaults.
     */
    ObservableEditCoordinate getDefaultEditCoordinate();

    /**
     *
     * @return an {@code ObservableLanguageCoordinate} based on the
     * configuration defaults.
     */
    ObservableLanguageCoordinate getDefaultLanguageCoordinate();

    /**
     *
     * @return an {@code ObservableLogicCoordinate} based on the configuration
     * defaults.
     */
    ObservableLogicCoordinate getDefaultLogicCoordinate();

    /**
     *
     * @return an {@code ObservableStampCoordinate} based on the configuration
     * defaults.
     */
    ObservableStampCoordinate getDefaultStampCoordinate();

    /**
     *
     * @return an {@code ObservableTaxonomyCoordinate} based on the
     * configuration defaults.
     */
    ObservableTaxonomyCoordinate getDefaultTaxonomyCoordinate();
    
    /**
     * @return true if verbose debug has been enabled.  This default implementation allows the 
     * feature to be enabled by setting the system property {@link Constants#ISAAC_DEBUG} to 'true'
     **/
    public default boolean enableVerboseDebug() 
    {
        String value = System.getProperty(Constants.ISAAC_DEBUG);
        if (StringUtils.isNotBlank(value)) 
        {
            return value.trim().equalsIgnoreCase("true");
        } 
        else 
        {
            return false;
        }
    }
    
    /**
     * There are some cases where validators and such cannot be properly executed if we are in bootstrap mode - building 
     * the system for the first time.  The default implementation of this returns false.
     */
    public default boolean inBootstrapMode()
    {
        return false;
    }
    /**
     * See {@link #inBootstrapMode()}
     */
    public default void setBootstrapMode()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * When building a DB, we don't want to index per commit, or write changeset files, among other things.
     * 
     * Note that this mode can be enabled-only only.  If you enable dbBuildMode, the mode cannot be turned off later.
     */
    public default boolean inDBBuildMode()
    {
        return false;
    }
    
    /**
     * See {@link #inDBBuildMode()}
     */
    public default void setDBBuildMode()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Return the known (if any) details to utilize to make a GIT server connection.
     * The returned URL should point to the root of the git server - not to a particular repository. 
     * @return
     */
    public default Optional<RemoteServiceInfo> getGitConfiguration()
    {
        return Optional.empty();
    }
    
    /**
     * Specify the details to be returned by {@link #getGitConfiguration()}.  This method is optional, and may not be supported
     * (in which case, it throws an {@link UnsupportedOperationException})
     */
    public default void setGitConfiguration(RemoteServiceInfo rsi)
    {
        throw new UnsupportedOperationException();
    }
}

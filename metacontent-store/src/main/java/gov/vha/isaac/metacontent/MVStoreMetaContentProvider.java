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
package gov.vha.isaac.metacontent;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jvnet.hk2.annotations.Service;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.metacontent.MetaContentService;
import gov.vha.isaac.ochre.api.metacontent.userPrefs.StorableUserPreferences;

/**
 * 
 * {@link MVStoreMetaContentProvider}
 *
 * An implementation of a MetaContentService wrapped around the MVStore from the
 * H2 DB project http://www.h2database.com/html/mvstore.html
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service(name = "MVStoreMetaContent")
@RunLevel(value = LookupService.SL_NEG_1_METADATA_STORE_STARTED_RUNLEVEL)
public class MVStoreMetaContentProvider implements MetaContentService {
	private final Logger LOG = LogManager.getLogger();

	private static final String USER_PREFS_STORE = "_userPrefs_";

	MVStore store;
	MVMap<Integer, byte[]> userPrefsMap;

	@SuppressWarnings("unused")
	private MVStoreMetaContentProvider() {
		// For HK2
		LOG.info("Constructing MVStoreMetaContent service " + this.hashCode());
	}

	/**
	 * Typically, this object should be retrieved from HK2 / the Lookup service
	 * - which already had a Service instance created. However, it is allowable
	 * to create your own instance outside of the management of HK2 using this
	 * method.
	 * 
	 * @param storageFolder
	 *            - The folder to utilize for storage.
	 * @param storePrefix
	 *            - optional - a prefix to utilize on all files/folders created
	 *            by the service inside the storageFolder
	 * @param wipeExisting
	 *            - true to erase preexisting content and start fresh, false to
	 *            read existing data.
	 */
	public MVStoreMetaContentProvider(File storageFolder, String storePrefix, boolean wipeExisting) {
		LOG.info("Starting a user-requested MVStoreMetaContent instance");
		initialize(storageFolder, storePrefix, wipeExisting);
	}

	private MetaContentService initialize(File storageFolder, String storePrefix, boolean wipeExisting) {
		File dataFile = new File(storageFolder,
				(StringUtils.isNotBlank(storePrefix) ? storePrefix : "") + "MetaContent.mv");
		if (wipeExisting && dataFile.exists()) {
			if (!dataFile.delete()) {
				throw new RuntimeException(
						"wipeExisting was requested, but can't delete " + dataFile.getAbsolutePath());
			}
		}
		LOG.info("MVStoreMetaContent store path: " + dataFile.getAbsolutePath());
		store = new MVStore.Builder().fileName(dataFile.getAbsolutePath()).open();
		// store.setVersionsToKeep(0); TODO check group answer
		userPrefsMap = store.<Integer, byte[]> openMap(USER_PREFS_STORE);
		return this;
	}
	
	@PostConstruct
	private void start()
	{
		LOG.info("Starting MVStoreMetaContent service");
		Optional<Path> path = Get.configurationService().getDataStoreFolderPath();
		if (!path.isPresent())
		{
			throw new RuntimeException("Unable to start MVStore - no folder path is available in the Configuration Service!");
		}
		File temp = new File(path.get().toFile(), "metacontent");
		temp.mkdir();
		if (!temp.isDirectory()) {
			throw new RuntimeException(
					"Cannot initialize MetaContent Store - was unable to create " + temp.getAbsolutePath());
		}
		initialize(temp, "service_", false);
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#close()
	 */
	@Override
	@PreDestroy
	public void close() {
		LOG.info("Stopping a MVStoreMetaContent service");
		if (store != null) {
			store.close();
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#putUserPrefs(int,
	 *      gov.vha.isaac.ochre.api.metacontent.userPrefs.StorableUserPreferences)
	 */
	@Override
	public byte[] putUserPrefs(int userId, StorableUserPreferences userPrefs) {
		return userPrefsMap.put(userId > 0 ? userId : Get.identifierService().getConceptSequence(userId),
				userPrefs.serialize());
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#getUserPrefs(int)
	 */
	@Override
	public byte[] getUserPrefs(int userId) {
		return userPrefsMap.get(userId > 0 ? userId : Get.identifierService().getConceptSequence(userId));
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#removeUserPrefs(int)
	 */
	@Override
	public void removeUserPrefs(int userId) {
		userPrefsMap.remove(userId > 0 ? userId : Get.identifierService().getConceptSequence(userId));
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#openStore(java.lang.String)
	 */
	@Override
	public <K, V> ConcurrentMap<K, V> openStore(String storeName) {
		if (storeName.equals(USER_PREFS_STORE)) {
			throw new IllegalArgumentException("reserved store name");
		}
		return store.<K, V> openMap(storeName);
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#removeStore(java.lang.String)
	 */
	@Override
	public void removeStore(String storeName) {
		if (storeName.equals(USER_PREFS_STORE)) {
			throw new IllegalArgumentException("reserved store name");
		}
		store.removeMap(store.openMap(storeName));
	}
}

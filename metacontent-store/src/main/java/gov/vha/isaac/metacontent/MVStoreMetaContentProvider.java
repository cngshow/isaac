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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.metacontent.MetaContentService;
import gov.vha.isaac.ochre.api.metacontent.userPrefs.StorableUserPreferences;
import gov.vha.isaac.ochre.api.metacontent.workflow.StorableWorkflowContent;

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
@RunLevel(value = 2)
public class MVStoreMetaContentProvider implements MetaContentService {
	private final Logger LOG = LogManager.getLogger();

	private static final String USER_PREFS_STORE = "_userPrefs_";
	private static final String STATIC_WORKFLOW_STORE = "_static_workflow_";
	private static final String USER_WORKFLOW_STORE = "_user_workflow_";

	MVStore store;
	MVMap<Integer, byte[]> userPrefsMap;
	MVMap<Integer, Map<UserWorkflowContentTypes, byte[]>> usersWorkflowContentMap;
	MVMap<StaticWorkflowContentTypes, byte[]> staticWorkflowContentMap;

	@SuppressWarnings("unused")
	private MVStoreMetaContentProvider() {
		// For HK2
		LOG.info("Starting MVStoreMetaContent service");
		File temp = new File(Get.configurationService().getDataStoreFolderPath().get().toFile(), "metacontent");
		temp.mkdir();
		if (!temp.isDirectory()) {
			throw new RuntimeException(
					"Cannot initialize MetaContent Store - was unable to create " + temp.getAbsolutePath());
		}
		initialize(temp, "service_", false);
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
		store = new MVStore.Builder().fileName(dataFile.getAbsolutePath()).open();
		// store.setVersionsToKeep(0); TODO check group answer
		userPrefsMap = store.<Integer, byte[]> openMap(USER_PREFS_STORE);
		usersWorkflowContentMap = store.<Integer, Map<UserWorkflowContentTypes, byte[]>> openMap(USER_WORKFLOW_STORE);
		staticWorkflowContentMap = store.<StaticWorkflowContentTypes, byte[]> openMap(STATIC_WORKFLOW_STORE);
		return this;
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
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#putUserWorkflowContent(int,
	 *      UserWorkflowContentTypes,
	 *      gov.vha.isaac.ochre.api.metacontent.userPrefs.StorableWorkflowContent)
	 */
	@Override
	public byte[] putUserWorkflowContent(int userId, UserWorkflowContentTypes type,
			StorableWorkflowContent workflowContent) {
		int actualUserId = userId > 0 ? userId : Get.identifierService().getConceptSequence(userId);
		if (!usersWorkflowContentMap.containsKey(actualUserId)) {
			Map<UserWorkflowContentTypes, byte[]> userUserWorkflowContentMap = new HashMap<>();

			usersWorkflowContentMap.put(actualUserId, userUserWorkflowContentMap);
		}

		return usersWorkflowContentMap.get(actualUserId).put(type, workflowContent.serialize());
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#getUserWorkflowContent(int,
	 *      UserWorkflowContentTypes)
	 */
	@Override
	public byte[] getUserWorkflowContent(int userId, UserWorkflowContentTypes type) {
		try {
			return usersWorkflowContentMap.get(userId > 0 ? userId : Get.identifierService().getConceptSequence(userId))
					.get(type);
		} catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#removeUserWorkflowContent(int,
	 *      UserWorkflowContentTypes)
	 */
	@Override
	public void removeUserWorkflowContent(int userId, UserWorkflowContentTypes type) {
		try {
			usersWorkflowContentMap.get(userId > 0 ? userId : Get.identifierService().getConceptSequence(userId))
					.remove(type);
		} catch (NullPointerException e) {
			LOG.info("Workflow content of type " + type + " already empty for User: " + userId + ".  No action taken");
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#removeUserWorkflowContent(int)
	 */
	@Override
	public void removeUserWorkflowContent(int userId) {
		try {
			usersWorkflowContentMap.remove(userId > 0 ? userId : Get.identifierService().getConceptSequence(userId));
		} catch (NullPointerException e) {
			LOG.info("Workflow content already empty for User: " + userId + ".  No action taken");
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#putStaticWorkflowContent(StaticWorkflowContentTypes,
	 *      gov.vha.isaac.ochre.api.metacontent.userPrefs.StorableWorkflowContent)
	 */
	@Override
	public byte[] putStaticWorkflowContent(StaticWorkflowContentTypes type, StorableWorkflowContent workflowContent) {
		return staticWorkflowContentMap.put(type, workflowContent.serialize());
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#getStaticWorkflowContent(StaticWorkflowContentTypes)
	 */
	@Override
	public byte[] getStaticWorkflowContent(StaticWorkflowContentTypes type) {
		try {
			return staticWorkflowContentMap.get(type);
		} catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#removeStaticWorkflowContent(StaticWorkflowContentTypes)
	 */
	@Override
	public void removeStaticWorkflowContent(StaticWorkflowContentTypes type) {
		try {
			staticWorkflowContentMap.remove(type);
		} catch (NullPointerException e) {
			LOG.info("Workflow content already empty for Type: " + type + ".  No action taken");
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#removeStaticWorkflowContent()
	 */
	@Override
	public void removeStaticWorkflowContent() {
		staticWorkflowContentMap.clear();
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#openStore(java.lang.String)
	 */
	@Override
	public <K, V> ConcurrentMap<K, V> openStore(String storeName) {
		if (storeName.equals(USER_PREFS_STORE) || storeName.equals(USER_WORKFLOW_STORE)
				|| storeName.equals(STATIC_WORKFLOW_STORE)) {
			throw new IllegalArgumentException("reserved store name");
		}
		return store.<K, V> openMap(storeName);
	}

	/**
	 * @see gov.vha.isaac.ochre.api.metacontent.MetaContentService#removeStore(java.lang.String)
	 */
	@Override
	public void removeStore(String storeName) {
		if (storeName.equals(USER_PREFS_STORE) || storeName.equals(USER_WORKFLOW_STORE)
				|| storeName.equals(STATIC_WORKFLOW_STORE)) {
			throw new IllegalArgumentException("reserved store name");
		}
		store.removeMap(store.openMap(storeName));
	}
}

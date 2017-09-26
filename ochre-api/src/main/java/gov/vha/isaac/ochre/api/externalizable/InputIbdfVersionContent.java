package gov.vha.isaac.ochre.api.externalizable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class representing content imported from an IBDF file. Content includes all
 * objects extending {@link OchreExternalizable} and at a basic level, includes
 * concepts, sememes, stamp aliases, and stamp comments.
 * 
 * Content is grouped in two fashions, those versioned (Concepts & Sememes) and
 * those not (Stamp Aliases and Stamp Comments).
 * 
 * In both cases, they are stored in thread-safe manner.
 * 
 * For those versioned, the content is stored in two objects: 1) type to
 * SortedSet of UUIDs and 2) UUID to {@link OchreExternalizable}. This will
 * expedite later processing without duplicating storage.
 * 
 * 
 */
public class InputIbdfVersionContent {

	/** The component map. */
	private ConcurrentHashMap<OchreExternalizableObjectType, Set<UUID>> typeToUuidMap_ = new ConcurrentHashMap<>();

	private ConcurrentHashMap<UUID, OchreExternalizable> uuidToComponentMap_ = new ConcurrentHashMap<>();

	/** The stamp map. */
	private ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> stampMap_ = new ConcurrentHashMap<>();

	/**
	 * Instantiates a new input ibdf version content.
	 */
	public InputIbdfVersionContent() {
		Set<OchreExternalizable> aliasSet = ConcurrentHashMap.newKeySet();
		Set<OchreExternalizable> commentSet = ConcurrentHashMap.newKeySet();
		stampMap_.put(OchreExternalizableObjectType.STAMP_ALIAS, aliasSet);
		stampMap_.put(OchreExternalizableObjectType.STAMP_COMMENT, commentSet);

		Set<UUID> conceptSet = ConcurrentHashMap.newKeySet();
		Set<UUID> sememeSet = ConcurrentHashMap.newKeySet();
		typeToUuidMap_.put(OchreExternalizableObjectType.CONCEPT, conceptSet);
		typeToUuidMap_.put(OchreExternalizableObjectType.SEMEME, sememeSet);
	}

	/**
	 * Gets the component map.
	 *
	 * @return the component map
	 */
	public ConcurrentHashMap<OchreExternalizableObjectType, Set<UUID>> getTypeToUuidMap() {
		return typeToUuidMap_;
	}

	/**
	 * Gets the component map.
	 *
	 * @return the component map
	 */
	public ConcurrentHashMap<UUID, OchreExternalizable> getUuidToComponentMap() {
		return uuidToComponentMap_;
	}

	/**
	 * Gets the stamp map.
	 *
	 * @return the stamp map
	 */
	public ConcurrentHashMap<OchreExternalizableObjectType, Set<OchreExternalizable>> getStampMap() {
		return stampMap_;
	}
}

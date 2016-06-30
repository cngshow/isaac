package gov.va.isaac.sync.git.gitblit.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import gov.va.isaac.sync.git.gitblit.models.RepositoryModel;

/**
* Utility methods for rpc calls.
*
*/
public class RpcUtils {
	public static final String RPC_PATH = "/rpc/";
	
	/**
	 *
	 * @param remoteURL
	 *            the url of the remote gitblit instance
	 * @param req
	 *            the rpc request type
	 * @param name
	 *            the name of the actionable object
	 * @return
	 */
	public static String asLink(String remoteURL, RpcRequest req, String name) {
		if (remoteURL.length() > 0 && remoteURL.charAt(remoteURL.length() - 1) == '/') {
			remoteURL = remoteURL.substring(0, remoteURL.length() - 1);
		}
		if (req == null) {
			req = RpcRequest.LIST_REPOSITORIES;
		}
		return remoteURL + RPC_PATH + "?req=" + req.name().toLowerCase()
				+ (name == null ? "" : ("&name=" + StringUtils.encodeURL(name)));
	}

		/**
	 * Create a repository on the Gitblit server.
	 *
	 * @param repository
	 * @param serverUrl
	 * @param account
	 * @param password
	 * @return true if the action succeeded
	 * @throws IOException
	 */
	public static boolean createRepository(RepositoryModel repository, String serverUrl,
			String account, char[] password) throws IOException {
		// ensure repository name ends with .git
		if (!repository.name.endsWith(".git")) {
			repository.name += ".git";
		}
		return doAction(RpcRequest.CREATE_REPOSITORY, null, repository, serverUrl, account,
				password);

	}


	/**
	 * Do the specified administrative action on the Gitblit server.
	 *
	 * @param request
	 * @param name
	 *            the name of the object (may be null)
	 * @param object
	 * @param serverUrl
	 * @param account
	 * @param password
	 * @return true if the action succeeded
	 * @throws IOException
	 */
	protected static boolean doAction(RpcRequest request, String name, Object object,
			String serverUrl, String account, char[] password) throws IOException {
		String url = asLink(serverUrl, request, name);
		String json = JsonUtils.toJsonString(object);
		int resultCode = JsonUtils.sendJsonString(url, json, account, password);
		return resultCode == 200;
	}
	/**
	 * Enumeration representing the possible remote procedure call requests from
	 * a client.
	 */
	public static enum RpcRequest {
		CREATE_REPOSITORY,LIST_REPOSITORIES ;

		public static RpcRequest fromName(String name) {
			for (RpcRequest type : values()) {
				if (type.name().equalsIgnoreCase(name)) {
					return type;
				}
			}
			return null;
		}

		public boolean exceeds(RpcRequest type) {
			return this.ordinal() > type.ordinal();
		}

		@Override
		public String toString() {
			return name();
		}
	}
	/**
	 * Enumeration representing the four access restriction levels.
	 */
	public static enum AccessRestrictionType {
		NONE, PUSH, CLONE, VIEW;

		private static final AccessRestrictionType [] AUTH_TYPES = { PUSH, CLONE, VIEW };

		public static AccessRestrictionType fromName(String name) {
			for (AccessRestrictionType type : values()) {
				if (type.name().equalsIgnoreCase(name)) {
					return type;
				}
			}
			return NONE;
		}

		public static List<AccessRestrictionType> choices(boolean allowAnonymousPush) {
			if (allowAnonymousPush) {
				return Arrays.asList(values());
			}
			return Arrays.asList(AUTH_TYPES);
		}

		public boolean exceeds(AccessRestrictionType type) {
			return this.ordinal() > type.ordinal();
		}

		public boolean atLeast(AccessRestrictionType type) {
			return this.ordinal() >= type.ordinal();
		}

		@Override
		public String toString() {
			return name();
		}

		public boolean isValidPermission(AccessPermission permission) {
			switch (this) {
			case VIEW:
				// VIEW restriction
				// all access permissions are valid
				return true;
			case CLONE:
				// CLONE restriction
				// only CLONE or greater access permissions are valid
				return permission.atLeast(AccessPermission.CLONE);
			case PUSH:
				// PUSH restriction
				// only PUSH or greater access permissions are valid
				return permission.atLeast(AccessPermission.PUSH);
			case NONE:
				// NO access restriction
				// all access permissions are invalid
				return false;
			}
			return false;
		}
	}

	/**
	 * Enumeration representing the types of authorization control for an
	 * access restricted resource.
	 */
	public static enum AuthorizationControl {
		AUTHENTICATED, NAMED;

		public static AuthorizationControl fromName(String name) {
			for (AuthorizationControl type : values()) {
				if (type.name().equalsIgnoreCase(name)) {
					return type;
				}
			}
			return NAMED;
		}

		@Override
		public String toString() {
			return name();
		}
	}
	/**
	 * The access permissions available for a repository.
	 */
	public static enum AccessPermission {
		NONE("N"), EXCLUDE("X"), VIEW("V"), CLONE("R"), PUSH("RW"), CREATE("RWC"), DELETE("RWD"), REWIND("RW+"), OWNER("RW+");

		public static final AccessPermission [] NEWPERMISSIONS = { EXCLUDE, VIEW, CLONE, PUSH, CREATE, DELETE, REWIND };

		public static final AccessPermission [] SSHPERMISSIONS = { VIEW, CLONE, PUSH };

		public static AccessPermission LEGACY = REWIND;

		public final String code;

		private AccessPermission(String code) {
			this.code = code;
		}

		public boolean atMost(AccessPermission perm) {
			return ordinal() <= perm.ordinal();
		}

		public boolean atLeast(AccessPermission perm) {
			return ordinal() >= perm.ordinal();
		}

		public boolean exceeds(AccessPermission perm) {
			return ordinal() > perm.ordinal();
		}

		public String asRole(String repository) {
			return code + ":" + repository;
		}

		@Override
		public String toString() {
			return code;
		}

		public static AccessPermission permissionFromRole(String role) {
			String [] fields = role.split(":", 2);
			if (fields.length == 1) {
				// legacy/undefined assume full permissions
				return AccessPermission.LEGACY;
			} else {
				// code:repository
				return AccessPermission.fromCode(fields[0]);
			}
		}

		public static String repositoryFromRole(String role) {
			String [] fields = role.split(":", 2);
			if (fields.length == 1) {
				// legacy/undefined assume full permissions
				return role;
			} else {
				// code:repository
				return fields[1];
			}
		}

		public static AccessPermission fromCode(String code) {
			for (AccessPermission perm : values()) {
				if (perm.code.equalsIgnoreCase(code)) {
					return perm;
				}
			}
			return AccessPermission.NONE;
		}
	}
	/**
	 * Enumeration representing the federation types.
	 */
	public static enum FederationStrategy {
		EXCLUDE, FEDERATE_THIS, FEDERATE_ORIGIN;

		public static FederationStrategy fromName(String name) {
			for (FederationStrategy type : values()) {
				if (type.name().equalsIgnoreCase(name)) {
					return type;
				}
			}
			return FEDERATE_THIS;
		}

		public boolean exceeds(FederationStrategy type) {
			return this.ordinal() > type.ordinal();
		}

		public boolean atLeast(FederationStrategy type) {
			return this.ordinal() >= type.ordinal();
		}

		@Override
		public String toString() {
			return name();
		}
	}

}

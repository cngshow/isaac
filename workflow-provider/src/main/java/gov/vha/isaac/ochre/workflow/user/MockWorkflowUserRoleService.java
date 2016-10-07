package gov.vha.isaac.ochre.workflow.user;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.workflow.provider.BPMNInfo;

/**
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 *
 */
@Service
@Rank(value = -50)
public class MockWorkflowUserRoleService implements WorkflowUserRoleService {
	protected static final UUID firstUserId = UUID.randomUUID();
	protected static final UUID secondUserId = UUID.randomUUID();
	protected static final UUID fullRoleUserId = UUID.randomUUID();
	protected static final UUID restTestingUserId = UUID.fromString("85af9e52-8cce-11e6-ae22-56b6b6499611");
	
	Map<UUID, Set<String>> userRoleMap = new HashMap<>();

	Set<String> definitionRoles = new HashSet<>();

	public MockWorkflowUserRoleService() {
		definitionRoles.add("Editor");
		definitionRoles.add("Reviewer");
		definitionRoles.add("Approver");
		definitionRoles.add(BPMNInfo.AUTOMATED_ROLE);

		// Setup User Role Maps
		userRoleMap.put(firstUserId, new HashSet<>());
		userRoleMap.get(firstUserId).add("Editor");
		userRoleMap.get(firstUserId).add("Approver");

		userRoleMap.put(secondUserId, new HashSet<>());
		userRoleMap.get(secondUserId).add("Reviewer");

		userRoleMap.put(fullRoleUserId, new HashSet<>());
		userRoleMap.get(fullRoleUserId).add("Editor");
		userRoleMap.get(fullRoleUserId).add("Reviewer");
		userRoleMap.get(fullRoleUserId).add("Approver");

    	userRoleMap.put(restTestingUserId, new HashSet<>());
    	userRoleMap.get(restTestingUserId).add("Editor");
    	userRoleMap.get(restTestingUserId).add("Reviewer");
    	userRoleMap.get(restTestingUserId).add("Approver");
	}

	@Override
	public Set<String> getUserRoles(UUID userId) {
		return userRoleMap.get(userId);
	}

	@Override
	public Set<String> getAllDefinitionRoles(UUID definitionId) {
		return definitionRoles;
	}

	/*
	 * For User Test
	 */
	/**
	 * @return
	 */
	public static UUID getFirstTestUser() {
		return firstUserId;
	}

	/*
	 * For User Test
	 */
	public static UUID getSecondTestUser() {
		return secondUserId;
	}

	/*
	 * For Integration Test
	 */
	public static UUID getFullRoleTestUser() {
		return fullRoleUserId;
	}

	/*
	 * For Integration Test
	 */
	public static UUID getRestTestingUserId() {
		return restTestingUserId;
	}

	public static int getFirstTestUserSeq() {
		return 1;
	}

	public static int getSecondTestUserSeq() {
		return 2;
	}
}

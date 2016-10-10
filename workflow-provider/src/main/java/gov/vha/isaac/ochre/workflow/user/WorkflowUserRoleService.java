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
package gov.vha.isaac.ochre.workflow.user;

import java.util.Set;
import java.util.UUID;

import org.jvnet.hk2.annotations.Contract;

/**
 * @author <a href="mailto:jefron@westcoastinformatics.com">Jesse Efron</a>
 *
 */
@Contract
public interface WorkflowUserRoleService {
	// Return all user roles based on userId
	Set<String> getUserRoles(UUID userId);

	// Not necessarily required.  May be useful in future though so leaving in place.
	Set<String> getAllDefinitionRoles(UUID definitionId);
}
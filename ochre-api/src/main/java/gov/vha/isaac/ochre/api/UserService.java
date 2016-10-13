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
package gov.vha.isaac.ochre.api;

import java.util.Optional;

import org.jvnet.hk2.annotations.Contract;

/**
 * The interface UserService. The service is used for converting SSO token strings
 * into users and their respective roles and for retrieving all available roles.
 * They can be defined in prism or hard coded for testing
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 */
@Contract
public interface UserService {
	/**
	 * Return a user and roles available for that user
	 *
	 * @param ssoToken
	 *            the user's SSO token string
	 * @return the user and roles available to the user
	 */
	Optional<User> getUser(String ssoToken);
}

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

package gov.vha.isaac.rest.user;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import gov.vha.isaac.ochre.api.User;
import gov.vha.isaac.ochre.api.PrismeRole;

/**
 * 
 * {@link RestApplicationSecurityContext}
 *
 * @author <a href="mailto:joel.kniaz.list@gmail.com">Joel Kniaz</a>
 *
 */
public class RestApplicationSecurityContext implements SecurityContext {
	private User user;
	private String scheme;

	public RestApplicationSecurityContext(User user, String scheme) {
		this.user = user;
		this.scheme = scheme;
	}

	/* (non-Javadoc)
	 * @see javax.ws.rs.core.SecurityContext#getUserPrincipal()
	 * 
	 * This should never return null, as RequestInfo sets a default read_only User
	 * if no SSO token or EditToken passed.
	 */
	@Override
	public Principal getUserPrincipal() {return this.user;}

	@Override
	public boolean isUserInRole(String s) {
		if (user == null)
		{
			return false;
		}
		for (PrismeRole role : user.getRoles()) {
			if (role.toString().equalsIgnoreCase(s)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isSecure() {return "https".equals(this.scheme);}

	@Override
	public String getAuthenticationScheme() {
		return "custom"; //SecurityContext.BASIC_AUTH;
	}
}

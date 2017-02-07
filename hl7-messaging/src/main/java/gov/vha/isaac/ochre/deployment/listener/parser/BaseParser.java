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
package gov.vha.isaac.ochre.deployment.listener.parser;

import gov.vha.isaac.ochre.access.maint.deployment.dto.Site;

public abstract class BaseParser
{
	/**
	 * Retrieve information from the Site table to describe the site by name
	 * 
	 * @param siteId
	 * @return Site object
	 */
	public Site resolveSiteId(String vaStringSiteId) {
		Site site = null;

		// TODO: replace this?
		// site = ListenerDelegate.getSiteByVAStringId(vaStringSiteId);

		return site;
	}
}

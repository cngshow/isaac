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

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ca.uhn.hl7v2.model.Message;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDiscovery;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDiscoveryDTO;

/**
 * This class will parse a HL7 message for site data/discovery into a
 * SiteDiscovery object.
 * 
 * {@link SiteDiscoveryParser}
 * 
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 *
 */
public class SiteDiscoveryParser
{
	private static final Logger LOG = LogManager.getLogger(SiteDiscoveryParser.class);

	private static final String MSH_SEGMENT = "MSH";
	private static final String MSA_SEGMENT = "MSA";
	private static final String MFE_SEGMENT = "MFE";
	private static final String ZRT_SEGMENT = "ZRT";
	private static final String VUID = "VUID";

	private static String[] substitutions = { "\\F\\", "\\S\\", "\\R\\", "\\T\\", "\\E\\" };
	private static String[] replacements = { "^", "~", "|", "&", "\\" };

	public SiteDiscovery parseMessage(Message message) throws Exception {

		SiteDiscovery siteDiscovery = new SiteDiscoveryDTO();
		BufferedReader br = new BufferedReader(new StringReader(message.toString()));
		String line;
		boolean firstPass = true;
		String lastSection = "";
		String lastZRTKey = "";
		ArrayList<String> headers = new ArrayList<String>();
		
		ArrayList<ArrayList<String>> values = new ArrayList<ArrayList<String>>();

		headers.add("VUID");
		//get columns headers
		while((line = br.readLine()) != null) {
			//if line begins with ZRT add header to list if not already there
			if (line.startsWith(ZRT_SEGMENT))  {
				String[] lineItems = line.split("\\^");
				makeSubstitutions(lineItems);
				if (!headers.contains(lineItems[1].toString().trim())) {
					headers.add(lineItems[1].toString().trim());
				}
			}
		}
		
		line = null;
		ArrayList<String> groupValues = new ArrayList<String>(headers.size());
		br = new BufferedReader(new StringReader(message.toString()));
		
		while ((line = br.readLine()) != null) {
			String[] lineItems = line.split("\\^");
			makeSubstitutions(lineItems);

			if (line.startsWith(MSH_SEGMENT)) {
				if (lineItems.length < 10) {
					throw new Exception("MSH segment parameter has {} items.  It needs to have more than 9.");
				}
				lastSection = MSH_SEGMENT;
				lastZRTKey = "";
			}

			else if (line.startsWith(MFE_SEGMENT)) {
				// add headers on first pass
				if (firstPass) {
					if (ZRT_SEGMENT.equals(lastSection)) {
						firstPass = false;
						values.add(groupValues);
						groupValues = new ArrayList<>(headers.size());
						//seed row with nulls
						for (int i = 0; i < headers.size(); i++)
						{
							groupValues.add(null);
						}
					}
				} else {
					siteDiscovery.setRefset(lineItems[4].split("\\@")[0]);
					values.add(groupValues);
					groupValues = new ArrayList<>(headers.size());
					for (int i = 0; i < headers.size(); i++) {
						groupValues.add(null);
					}
				}
				if (lineItems.length > 3 & lineItems[4].contains("@")) {
					groupValues.add(headers.indexOf("VUID"), lineItems[4].split("\\@")[1]);
				} 
				lastSection = MFE_SEGMENT;
				lastZRTKey = "";
			}

			else if (line.startsWith(ZRT_SEGMENT)) {
				
				if (lastZRTKey.equals(lineItems[1].toString())) {
					groupValues.set(
						headers.indexOf(lineItems[1]), 
						groupValues.get(headers.indexOf(lineItems[1])) +"|" +  (lineItems.length > 2 ? lineItems[2] : null));
				}
				else {
					groupValues.add(
							headers.indexOf(lineItems[1]), 
							lineItems.length > 2 ? lineItems[2] : null);
				}

				lastSection = ZRT_SEGMENT;
				lastZRTKey = lineItems[1].toString();
			}

		}
		// add final group
		values.add(groupValues);

		siteDiscovery.setHeaders(headers);
		siteDiscovery.setValues(values);
		return siteDiscovery;
	}

	// BISACODYL 10MG SUPP/CITRATE \T\ SO4 MAGNESIA 30GM/SENNA EXT 74ML
	protected void makeSubstitutions(String[] params) {
		for (int i = 0; i < params.length; i++) {
			for (int j = 0; j < replacements.length; j++) {
				params[i] = params[i].replace(substitutions[j], replacements[j]);
			}
		}
	}

}

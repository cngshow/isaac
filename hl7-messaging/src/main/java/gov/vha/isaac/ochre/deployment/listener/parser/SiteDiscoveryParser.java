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
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDiscovery;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDiscoveryDTO;

/**
 * This class will parse a HL7 message for site data/discovery into a SiteDiscovery object.
 * 
 * {@link SiteDiscoveryParser}
 * 
 * @author <a href="mailto:nmarques@westcoastinformatics.com">Nuno Marques</a>
 *
 */
public class SiteDiscoveryParser
{
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
		ArrayList<String> headers = new ArrayList<String>();
		ArrayList<String> groupValues = new ArrayList<String>();
		ArrayList<ArrayList<String>> values = new ArrayList<ArrayList<String>>();

		try {

			while ((line = br.readLine()) != null) {
				String[] lineItems = line.split("\\^");
				makeSubstitutions(lineItems);

				if (line.startsWith(MSH_SEGMENT)) {
					if (lineItems.length < 10) {
						throw new Exception("MSH segment parameter has {} items.  It needs to have more than 9.");
					}
					lastSection = MSH_SEGMENT;
				}

				else if (line.startsWith(MFE_SEGMENT)) {
					// add headers on first pass
					if (firstPass && lastSection != ZRT_SEGMENT) {
						headers.add(VUID);
						if (ZRT_SEGMENT.equals(lastSection)) {
							firstPass = false;
						}
					} else {
						siteDiscovery.setRefset(lineItems[4].split("\\@")[0]);
						values.add(groupValues);
						groupValues = new ArrayList<>();
					}
					if (lineItems.length > 3 & lineItems[4].contains("@")) {
						groupValues.add(lineItems[4].split("\\@")[1]);
					} else {
						groupValues.add(null);
					}
					lastSection = MFE_SEGMENT;
				}

				else if (line.startsWith(ZRT_SEGMENT)) {
					if (firstPass) {
						if (lineItems.length >= 3) {
							headers.add(lineItems[1]);
						}
					}
					groupValues.add(lineItems.length > 2 ? lineItems[2] : null);
					lastSection = ZRT_SEGMENT;
				}

			}
			// add final group
			values.add(groupValues);

		} catch (IOException e) {
			System.out.println("Error:");
			System.out.println(e.getMessage());
		}

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

	public static final void main(String[] args) throws Exception {

		String test = "MSH^~|\\&^VETS UPDATE^660DEV2^XUMF UPDATE^^20080509095700.000-0600^^MFN~M01^^^2.4^^^AL^AL^USA\r"
				+ "MFI^Standard Terminology~~ERT^^MUP^20080509095700.000-0600^20080509095700.000-0600^NE\r"
				+ "MFE^MUP^^^Order Status@4500659\r" + "ZRT^Term^ACTIVE\r" + "ZRT^VistA_Short_Name^actv\r"
				+ "ZRT^VistA_Abbreviation^a\r"
				+ "ZRT^VistA_Description^Orders that are active or have been accepted by the service for processing.  e.g., Dietetic orders are active upon being ordered, Pharmacy orders are active when the order is verified, Lab orders are active when the sample has been collected, Radiology orders are active upon registration.\r"
				+ "ZRT^Status^1\r" + "MFE^MUP^^^Order Status@4501011\r" + "ZRT^Term^CANCELLED\r"
				+ "ZRT^VistA_Short_Name^canc\r" + "ZRT^VistA_Abbreviation^x\r"
				+ "ZRT^VistA_Description^Orders that have been rejected by the ancillary service without being acted on, or terminated while still delayed.\r"
				+ "ZRT^Status^1\r" + "MFE^MUP^^^Order Status@4501088\r" + "ZRT^Term^COMPLETE\r"
				+ "ZRT^VistA_Short_Name^comp\r" + "ZRT^VistA_Abbreviation^c\r"
				+ "ZRT^VistA_Description^Orders that require no further action by the ancillary service.  e.g., Lab orders are completed when results are available, Radiology orders are complete when results are available.\r"
				+ "ZRT^Status^1\r" + "MFE^MUP^^^VERSION\r" + "ZRT^version^0\r";

		PipeParser parser = new PipeParser();
		Message message = parser.parse(test);

		SiteDiscoveryParser siteDiscoveryParser = new SiteDiscoveryParser();
		SiteDiscovery siteDiscovery = siteDiscoveryParser.parseMessage(message);

		System.out.println(siteDiscovery.toString());
	}
}

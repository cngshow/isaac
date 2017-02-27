package gov.vha.isaac.ochre.deployment.listener.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDiscovery;
import gov.vha.isaac.ochre.access.maint.deployment.dto.SiteDiscoveryDTO;

public class SiteDiscoveryParser
{
	private static final String MSH_SEGMENT = "MSH";
	private static final String MSA_SEGMENT = "MSA";
	private static final String MFE_SEGMENT = "MFE";
	private static final String ZRT_SEGMENT = "ZRT";

	private static String[] substitutions = { "\\F\\", "\\S\\", "\\R\\", "\\T\\", "\\E\\" };
	private static String[] replacements = { "^", "~", "|", "&", "\\" };

	public List<SiteDiscovery> parseMessage(Message message) throws Exception {
		List<SiteDiscovery> siteDiscoveryList = new ArrayList<>();

		BufferedReader br = new BufferedReader(new StringReader(message.toString()));
		String line;
		SiteDiscovery siteDiscovery = new SiteDiscoveryDTO();
		boolean firstPass = true;

		try {

			while ((line = br.readLine()) != null) {
				String[] lineItems = line.split("\\^");
				makeSubstitutions(lineItems);

				if (line.startsWith(MSH_SEGMENT)) {
					if (lineItems.length < 10) {
						throw new Exception("MSH segment parameter has {} items.  It needs to have more than 9. ");
					}

				}
				if (line.startsWith(MFE_SEGMENT)) {
					if (!firstPass) {
						siteDiscoveryList.add(siteDiscovery);
						siteDiscovery = new SiteDiscoveryDTO();
					}
					if (lineItems.length >= 5) {
						String[] parts = lineItems[4].split("\\@");
						if (parts.length >= 1)
							siteDiscovery.setSubset(parts[0]);
						if (parts.length >= 2)
							siteDiscovery.setVuid(parts[1]);
					}
				}
				if (line.startsWith(ZRT_SEGMENT)) {
					if (lineItems.length >= 3) {
						siteDiscovery.addSegment(lineItems[1], lineItems[2]);
					}
					firstPass = false;
				}

			}
			siteDiscoveryList.add(siteDiscovery);
		} catch (IOException e) {
			System.out.println("Error:");
			System.out.println(e.getMessage());
		}

		return siteDiscoveryList;
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
				+ "MFE^MUP^^^Order Status@4500659\r" 
				+ "ZRT^Term^ACTIVE\r" 
				+ "ZRT^VistA_Short_Name^actv\r"
				+ "ZRT^VistA_Abbreviation^a\r"
				+ "ZRT^VistA_Description^Orders that are active or have been accepted by the service for processing.  e.g., Dietetic orders are active upon being ordered, Pharmacy orders are active when the order is verified, Lab orders are active when the sample has been collected, Radiology orders are active upon registration.\r"
				+ "ZRT^Status^1\r" 
				+ "MFE^MUP^^^Order Status@4501011\r" 
				+ "ZRT^Term^CANCELLED\r"
				+ "ZRT^VistA_Short_Name^canc\r" 
				+ "ZRT^VistA_Abbreviation^x\r"
				+ "ZRT^VistA_Description^Orders that have been rejected by the ancillary service without being acted on, or terminated while still delayed.\r"
				+ "ZRT^Status^1\r" 
				+ "MFE^MUP^^^Order Status@4501088\r" 
				+ "ZRT^Term^COMPLETE\r"
				+ "ZRT^VistA_Short_Name^comp\r" 
				+ "ZRT^VistA_Abbreviation^c\r"
				+ "ZRT^VistA_Description^Orders that require no further action by the ancillary service.  e.g., Lab orders are completed when results are available, Radiology orders are complete when results are available.\r"
				+ "ZRT^Status^1\r" 
				+ "MFE^MUP^^^VERSION\r" 
				+ "ZRT^version^0\r";

		PipeParser parser = new PipeParser();
		Message message = parser.parse(test);
		
		SiteDiscoveryParser siteDiscoveryParser = new SiteDiscoveryParser();
		List<SiteDiscovery> sites =  siteDiscoveryParser.parseMessage(message);
		
		for(SiteDiscovery site : sites) {
			System.out.println(site.toString());
		}
	}
}

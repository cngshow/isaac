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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Super Simple HL7 Message parser - used to reduce memory footprint instead of
 * HAPI library
 * 
 * @author VHAISLJACOXB
 * 
 */
public class SiteDataParser extends BaseParser
{
	private static Logger log = LogManager.getLogger(SiteDataParser.class.getPackage().getName());

	private static final String MAP_DEFINITION = "MapDefinition";
	private static final String EFFECTIVE_DATE = "EffectiveDate";
	private static final String STATUS = "Status";
	private static final String ACTIVE = "Active";
	private static final String MAPPINGS = "Mappings";
	private static final String MSH_SEGEMENT = "MSH";
	private static final String ZRT_SEGMENT = "ZRT";
	private static final String MFE_SEGMENT = "MFE";
	private static final String PROPERTY_TYPE = "P";
	private static final String RELATIONSHIP_TYPE = "R";
	private static final String MAPPING_TYPE = "M";
	private static final String TERM_TYPE = "T";

	private static String[] substitutions = { "\\F\\", "\\S\\", "\\R\\", "\\T\\", "\\E\\" };

	private static String[] replacements = { "^", "~", "|", "&", "\\" };

	// index into the content string
	int index = 0;

	/**
	 * Iterate over the message and write data to the SiteData table
	 * 
	 * @param content
	 *            Incoming message as a String
	 */
	public void processMessage(String content) throws Exception {

		// String line;
		// String vuid = null;
		// String regionName = null;
		// boolean isMapping = false;
		// String siteValue = null;
		// String messageId = null;
		// Site site = null;
		// SubsetConfig subsetConfig = null;
		// HashSet<String> propertySet = new HashSet<String>();
		// HashSet<String> relationshipSet = new HashSet<String>();
		// boolean hasStatus = false;
		// boolean status = false;
		// List<SiteDataDTO> saveList = new ArrayList<SiteDataDTO>();
		// boolean first = true;
		//
		// List<SiteDataDTO> siteDataDTOs = new ArrayList<SiteDataDTO>();
		//
		// log.debug("SiteDataParser starting");
		// try
		// {
		// while ((line = getNextLine(content)) != null)
		// {
		// String[] params = line.split("\\^");
		// makeSubstitutions(params);
		// if (line.startsWith(MSH_SEGEMENT))
		// {
		// if (params.length < 10)
		// {
		// throw new Exception("MSH_SEGMENT paramter count ="+params.length+"
		// need to have greater than 9");
		// }
		// siteValue = params[3];
		// messageId = params[9];
		// log.debug("MSH for site: " + siteValue);
		//
		// site = this.getSite(siteValue);
		// }
		// if (line.startsWith(MFE_SEGMENT))
		// {
		// // check to see if we didn't get the status line and are dumping
		// values
		// if (saveList.size() > 0)
		// {
		// for(SiteDataDTO siteData : saveList)
		// {
		// log.error("Site: "+site.getName()+"| "+siteData.getName()+"| "
		// + siteData.getSubsetName()
		// +"| was not written to the db because status line not received!");
		// }
		// saveList.clear();
		// }
		// if (params.length >= 5) // make sure we have the right number of
		// parameters
		// {
		// hasStatus = false; // do we know if this is an inactive or
		// // not
		// String[] parts = params[4].split("\\@");
		// makeSubstitutions(parts);
		//
		// regionName = parts[0];
		//
		// log.debug("MFE for subset:" + regionName);
		// // this is an MFE with VUID
		// if (parts.length > 1)
		// {
		// vuid = parts[1];
		// if (!regionName.equalsIgnoreCase(MAPPINGS))
		// {
		// subsetConfig = getSubsetConfig(regionName);
		// propertySet.addAll(subsetConfig.getPropertyNameList());
		// relationshipSet.addAll(subsetConfig.getRelationshipNameList());
		// if (first)
		// {
		// log.debug("Creating entry in siteDataRequest for " + site.getId() + "
		// subset: " + regionName);
		// ListenerDelegate.createSiteDataRequest(site.getId(), regionName);
		// first = false;
		// }
		// }
		// else
		// {
		// isMapping = true;
		// }
		// }
		// // if this is a VERSION MFE segment, there's no VUID
		// else
		// {
		// // set vuid back to its initialized value
		// vuid = null;
		// }
		// }
		// }
		// else if (line.startsWith(ZRT_SEGMENT))
		// {
		// if (params.length >= 3) // make sure we have the right number of
		// parameters
		// {
		// log.debug("ZRT site:" + site.getName() + "| " + regionName + "| " +
		// vuid + "| " + params[1] + "| "
		// + params[2]);
		// String typeName = params[1];
		// String value = params[2];
		// for (int i = 3; i < params.length; i++)
		// {
		// value = value+"^"+params[i];
		// }
		// String type = null;
		//
		// if (!isMapping)
		// {
		// if (propertySet.contains(typeName))
		// {
		// type = PROPERTY_TYPE; // property
		// }
		// else if (relationshipSet.contains(typeName))
		// {
		// type = RELATIONSHIP_TYPE; // relationship
		// }
		// else if (typeName.equals(HL7SubsetUpdateGenerator.TERM_FIELD_NAME))
		// {
		// type = TERM_TYPE; // term
		// }
		// }
		// else
		// {
		// // if this is a mapDefinition then we know what mapping it is
		// if (MAP_DEFINITION.equals(typeName))
		// {
		// MapSet mapSet = MapSetDelegate.getByVuid(Long.valueOf(value),
		// HibernateSessionFactory.AUTHORING_VERSION_ID);
		// regionName = mapSet.getName();
		// if (first)
		// {
		// log.debug("Creating entry in siteDataRequest for " + site.getId() + "
		// mapping: " + regionName);
		// ListenerDelegate.createSiteDataRequest(site.getId(), regionName);
		// first = false;
		// }
		// }
		// else if (!typeName.equals(ACTIVE) &&
		// !typeName.equals(EFFECTIVE_DATE))
		// {
		// type = MAPPING_TYPE;
		// }
		// }
		//
		// if (typeName.equals(STATUS) || typeName.equals(ACTIVE))
		// {
		// status = value.equals("1") ? true : false;
		// hasStatus = true;
		//
		// for(SiteDataDTO siteDataDTO : saveList)
		// {
		// siteDataDTO.setActive(status);
		// siteDataDTOs.add(siteDataDTO);
		// }
		// saveList.clear();
		// }
		// else if (type != null && vuid != null)
		// {
		// if(value.length() > 2000)
		// {
		// log.warn("Incoming site data value over 2000 characters. Data value
		// truncated in database. " +
		// "Full data value: " + value);
		// value = "TRUNCATED: " + value.substring(0, 1989);
		// }
		//
		// SiteDataDTO siteDataDTO = new SiteDataDTO(typeName, value,
		// Long.parseLong(vuid.trim()), site, regionName, type);
		// if (hasStatus)
		// {
		// siteDataDTO.setActive(status);
		// siteDataDTOs.add(siteDataDTO);
		// }
		// else
		// {
		// saveList.add(siteDataDTO);
		// }
		// }
		// else
		// {
		// // only log an error if the parameter is something
		// // other
		// // than status
		// if (!typeName.equals(STATUS) && !isMapping)
		// {
		// log.error("Ignoring entry " + params[1] + " for subset " + regionName
		// + " because it was not in the subsetConfig.xml file");
		// }
		// }
		// }
		// }
		// }
		// //Now that we have all the data in a list, persist to the DB
		// //TODO: check if required
		// //ListenerDelegate.createSiteData(site.getId(), regionName,
		// siteDataDTOs);
		//
		// log.info("SITE DATA MESSAGE RECEIVED" + "; SITE NAME: " +
		// site.getName() + "; SITE ID: " + site.getVaSiteId() //getTopic()
		// + "; MSG. ID: " + messageId);
		// }
		// catch (Exception e)
		// {
		// log.error(e);
		// throw new STSException("Error writing site data to the database", e);
		// }
		log.info("SiteData message {}", content);
	}

	/**
	 * Read the subsets that are configured and Return a subset by the given
	 * name
	 * 
	 * @param subsetName
	 * @return
	 * @throws Exception
	 */
	// private SubsetConfig getSubsetConfig(String subsetName) throws Exception
	// {
	// return ListenerDelegate.getSubsetConfigByName(subsetName);
	// }

	/**
	 * Return the next line in the string if it is the end then return null
	 * 
	 * @param content
	 */
	private String getNextLine(String content) {
		String line = null;
		if (index < content.length()) {
			// find the position of the CR
			int pos = content.indexOf("\r", index);
			// get the string to that position
			if (pos >= 0) {
				line = content.substring(index, pos);
			}
			// set the index past the CR
			index = (pos + 1);
			// skip past any linefeeds
			while (index < content.length() && content.charAt(index) == '\n') {
				index++;
			}
		}
		return line;
	}

	/*
	 * Get all the fields from the Site table by topic (a.k.a. Site ID in the
	 * Interface Engine world)
	 */
	// private Site getSite(String vaSiteId) throws Exception
	// {
	// Site site = null;
	//
	// site = ListenerDelegate.getSiteByVAStringId(vaSiteId);
	//
	// return site;
	// }

	// BISACODYL 10MG SUPP/CITRATE \T\ SO4 MAGNESIA 30GM/SENNA EXT 74ML
	protected void makeSubstitutions(String[] params) {
		for (int i = 0; i < params.length; i++) {
			for (int j = 0; j < replacements.length; j++) {
				params[i] = params[i].replace(substitutions[j], replacements[j]);
			}
		}
	}

	// TODO: move to test if the above is added back
	public static final void main(String[] args) throws Exception {
		String content = "MSH^~|&^VETS UPDATE^660DEV2^XUMF UPDATE^^20080512053800.000-0600^^MFN~M01^^^2.4^^^AL^AL^USA\r"
				+ "MFI^Standard Terminology~~ERT^^MUP^20080512053800.000-0600^20080512053800.000-0600^NE\r"
				+ "MFE^MUP^^^Mappings@210\r" + "ZRT^MapDefinition^1884397\r" + "ZRT^SourceCode^2972007\r"
				+ "ZRT^TargetCode^433.80\r" + "ZRT^Order^1\r" + "ZRT^EffectiveDate^20060131\r" + "ZRT^Active^1\r"
				+ "MFE^MUP^^^Mappings@211\r" + "ZRT^MapDefinition^1884397\r" + "ZRT^SourceCode^2973002\r"
				+ "ZRT^TargetCode^867.8v\r" + "ZRT^Order^1\r" + "ZRT^EffectiveDate^20060131\r" + "ZRT^Active^1\r"
				+ "MFE^MUP^^^Mappings@212\r" + "ZRT^MapDefinition^1884397\r" + "ZRT^SourceCode^2992000\r"
				+ "ZRT^TargetCode^333.0\r" + "ZRT^Order^1\r" + "ZRT^EffectiveDate^20060131\r" + "ZRT^Active^1\r";

		SiteDataParser parser = new SiteDataParser();
		parser.processMessage(content);

		String content2 = "MSH^~|&^VETS UPDATE^660DEV2^XUMF UPDATE^^20080509095700.000-0600^^MFN~M01^^^2.4^^^AL^AL^USA\r"
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

		parser = new SiteDataParser();
		parser.processMessage(content2);
	}
}

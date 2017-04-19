package gov.va.oia.terminology.converters.sharedUtils.umlsUtils.propertyTypes;

import gov.va.oia.terminology.converters.sharedUtils.propertyTypes.PropertyType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.constants.DynamicSememeConstants;

/**
 * @author Daniel Armbrust
 */
public class PT_SAB_Metadata extends PropertyType
{
	public PT_SAB_Metadata(String termName)
	{
		//from http://www.nlm.nih.gov/research/umls/rxnorm/docs/2013/rxnorm_doco_full_2013-2.html#s12_8
		super(termName + " Source Vocabulary Metadata", true, DynamicSememeDataType.STRING);
		indexByAltNames();
		addProperty("Versioned CUI", "VCUI", "CUI of the versioned SRC concept for a source");
		addProperty("Root CUI", "RCUI", "CUI of the root SRC concept for a source");
		addProperty("Versioned Source Abbreviation", "VSAB", "The versioned source abbreviation_ for a source, e.g., NDDF_2004_11_03");
		addProperty("Root Source Abbreviation", "RSAB","The root source abbreviation_, for a source e.g. NDDF");
		addProperty("Source Official Name", "SON","The official name for a source");
		addProperty("Source Family", "SF","The Source Family for a source");
		addProperty("Source Version", "SVER","The source version, e.g., 2001");
		addProperty("Meta Start Date","VSTART","The date a source became active, e.g., 2001_04_03");
		addProperty("Meta End Date", "VEND","The date a source ceased to be active, e.g., 2001_05_10");
		addProperty("Meta Insert Version", "IMETA","The version of the Metathesaurus a source first appeared, e.g., 2001AB");
		addProperty("Meta Remove Version", "RMETA","The version of the Metathesaurus a source was removed, e.g., 2001AC");
		addProperty("Source License Contact", "SLC",
				"The source license contact information. A semi-colon separated list containing the following fields: Name; Title; Organization; Address 1; Address 2; City; State or Province; Country; Zip or Postal Code; Telephone; Contact Fax; Email; URL");
		addProperty("Source Content Contact", "SCC","The source content contact information. A semi-colon separated list containing the following fields: Name; Title; Organization; Address 1; Address 2; City; State or Province; Country; Zip or Postal Code; Telephone; Contact Fax; Email; URL");
		addProperty("Source Restriction Level", "SRL","0,1,2,3,4 - explained in the License Agreement.", false, -1, 
				new DynamicSememeColumnInfo[] { new DynamicSememeColumnInfo(null, 0, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_VALUE.getUUID(),
				DynamicSememeDataType.UUID, null, true, null, null, true)});
		addProperty("Term Frequency", "TFR","The number of terms for this source in RXNCONSO.RRF, e.g., 12343 (not implemented yet)");
		addProperty("CUI Frequency", "CFR","The number of CUIs associated with this source, e.g., 10234 (not implemented yet)");
		addProperty("Context Type", "CXTY","The type of relationship label (Section 2.4.2 of UMLS Reference Manual)", false, -1, 
				new DynamicSememeColumnInfo[] { new DynamicSememeColumnInfo(null, 0, DynamicSememeConstants.get().DYNAMIC_SEMEME_COLUMN_VALUE.getUUID(),
				DynamicSememeDataType.UUID, null, true, null, null, true)});
		addProperty("Term Type List", "TTYL","Term type list from source, e.g., MH,EN,PM,TQ");
		addProperty("Attribute Name List", "ATNL","The attribute name list, e.g., MUI,RN,TH,...");
		addProperty("Language", "LAT","The language of the terms in the source");
		addProperty("Character Encoding", "CENC",
				"Character set as specified by the IANA official names for character assignments http://www.iana.org/assignments/character-sets");
		addProperty("Current Version", "CURVER",
				"A Y or N flag indicating whether or not this row corresponds to the current version of the named source");
		addProperty("Source in Subset", "SABIN",
				"A Y or N flag indicating whether or not this row is represented in the current MetamorphoSys subset. Initially always Y where CURVER is Y, but later is recomputed by MetamorphoSys.");
		addProperty("Source short name", "SSN",
				"The short name of a source as used by the NLM Knowledge Source Server.");
		addProperty("Source citation", "SCIT",
				"Citation information for a source. A semi-colon separated list containing the following fields: Author(s); Author(s) address; Author(s) organization; Editor(s); Title; Content Designator; Medium Designator; Edition; Place of Publication; Publisher; Date of Publication/copyright; Date of revision; Location; Extent; Series; Availability Statement (URL); Language; Notes");	
	}
}

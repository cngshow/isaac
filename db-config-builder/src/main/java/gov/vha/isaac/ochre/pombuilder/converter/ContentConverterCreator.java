package gov.vha.isaac.ochre.pombuilder.converter;

import gov.vha.isaac.ochre.pombuilder.RepositoryLocation;
import gov.vha.isaac.ochre.pombuilder.artifacts.SDOSourceContent;

public class ContentConverterCreator
{
	
	/**
	 * Create a source conversion project which is executable via maven.
	 * @param sourceContent - The artifact information for the content to be converted.  The artifact information must follow known naming conventions - group id should 
	 * be gov.vha.isaac.terminology.source.  Currently supported artifactIds are 'loinc-src-data', 'loinc-src-data-tech-preview', 'rf2-src-data-*', 'vhat' 
	 * @param converterVersion - The version number of the content converter code to utilize.
	 * @param additionalContent - Some converters require additional data files to satisfy dependencies. Currently:
	 * loinc-src-data-tech-preview requires:
	 *   - loinc-src-data
	 *   - rf2-src-data-sct
	 * Any RF2 extension files (rf2-src-data-*-ext) requires:
	 *   - rf2-src-data-sct
	 * @return
	 * @throws Exception
	 */
	public static RepositoryLocation createContentConverter(SDOSourceContent sourceContent, String converterVersion, SDOSourceContent[] additionalContent) throws Exception
	{
		

		//TODO attach to GIT / push
		return new RepositoryLocation("a", "b");
	}
}

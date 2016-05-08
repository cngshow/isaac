package test;

import gov.vha.isaac.ochre.pombuilder.artifacts.IBDFFile;
import gov.vha.isaac.ochre.pombuilder.artifacts.SDOSourceContent;
import gov.vha.isaac.ochre.pombuilder.converter.ContentConverterCreator;

public class TestConversionConfiguration
{

	public static void main(String[] args) throws Exception
	{
		ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.vhat", "vhat-src-data", "2016.01.07"), 
			"4.1-SNAPSHOT", new SDOSourceContent[0], new IBDFFile[0]);
	}
}

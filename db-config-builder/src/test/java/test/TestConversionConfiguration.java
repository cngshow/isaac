package test;

import gov.vha.isaac.ochre.pombuilder.artifacts.IBDFFile;
import gov.vha.isaac.ochre.pombuilder.artifacts.SDOSourceContent;
import gov.vha.isaac.ochre.pombuilder.converter.ContentConverterCreator;

public class TestConversionConfiguration
{

	public static void main(String[] args) throws Exception
	{
		//vhat
//		ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.vhat", "vhat-src-data", "2016.01.07"), 
//			"4.1-SNAPSHOT", new SDOSourceContent[0], new IBDFFile[0]);
		
		//loinc
//		ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.loinc", "loinc-src-data", "2.54"), 
//			"5.1-SNAPSHOT", new SDOSourceContent[0], new IBDFFile[0]);
		
		//loinc-tech-preview
//		ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.loinc", "loinc-src-data-tech-preview", "2015.08.01"), 
//			"5.1-SNAPSHOT", 
//			new SDOSourceContent[] {new SDOSourceContent("gov.vha.isaac.terminology.source.loinc", "loinc-src-data", "2.54")}, 
//			new IBDFFile[] {new IBDFFile("gov.vha.isaac.terminology.converted", "rf2-ibdf-sct", "20150731-loader-3.1-SNAPSHOT", "Snapshot")});
		
		//sct
		ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.rf2", "rf2-src-data-sct", "20150731"), 
			"3.1-SNAPSHOT", new SDOSourceContent[0], new IBDFFile[0]);
	}
}

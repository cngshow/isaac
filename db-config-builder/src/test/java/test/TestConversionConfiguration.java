package test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import gov.vha.isaac.ochre.pombuilder.GitPublish;
import gov.vha.isaac.ochre.pombuilder.artifacts.Converter;
import gov.vha.isaac.ochre.pombuilder.artifacts.IBDFFile;
import gov.vha.isaac.ochre.pombuilder.artifacts.SDOSourceContent;
import gov.vha.isaac.ochre.pombuilder.converter.ContentConverterCreator;
import gov.vha.isaac.ochre.pombuilder.converter.ConverterOptionParam;

public class TestConversionConfiguration
{

	public static void main(String[] args) throws Exception
	{
		String gitTestURL = "https://vadev.mantech.com:4848/git/";
		String gitUsername = "";
		char[] gitPassword = "".toCharArray();
		
		String nexusUrl = "https://vadev.mantech.com:8080/nexus/content/groups/public/";
		String nexusUsername = "";
		String nexusPassword = "";

		System.setProperty("java.awt.headless", "true");
		
		System.out.println(GitPublish.readTags(gitTestURL, gitUsername, gitPassword));
		//vhat
//		System.out.println(ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.vhat", "vhat-src-data", "2016.01.07"), 
//			"4.1-SNAPSHOT", new SDOSourceContent[0], new IBDFFile[0], null, gitTestURL, gitUsername, gitPassword));
		
		//loinc
//		System.out.println(ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.loinc", "loinc-src-data", "2.54"), 
//			"5.1-SNAPSHOT", new SDOSourceContent[0], new IBDFFile[0], null, gitTestURL, gitUsername, gitPassword));
		
		//loinc-tech-preview
//		System.out.println(ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.loinc", "loinc-src-data-tech-preview", "2015.08.01"), 
//			"5.1-SNAPSHOT", 
//			new SDOSourceContent[] {new SDOSourceContent("gov.vha.isaac.terminology.source.loinc", "loinc-src-data", "2.54")}, 
//			new IBDFFile[] {new IBDFFile("gov.vha.isaac.terminology.converted", "rf2-ibdf-sct", "20150731-loader-3.1-SNAPSHOT", "Snapshot")},
//			null, gitTestURL, gitUsername, gitPassword));
		
//		//sct
//		System.out.println(ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.rf2", "rf2-src-data-sct", "20150731"), 
//			"3.1-SNAPSHOT", new SDOSourceContent[0], new IBDFFile[0], null, gitTestURL, gitUsername, gitPassword));
		
		//sct-us-ext
		
		ConverterOptionParam[] optionTypes = ContentConverterCreator.getConverterOptions(new Converter("gov.vha.isaac.terminology.converters", "rf2-mojo", "3.3-SNAPSHOT"), 
				nexusUrl, nexusUsername, nexusPassword);
		
		HashMap<ConverterOptionParam, Set<String>> options = new HashMap<>();
		for (ConverterOptionParam x : optionTypes)
		{
			if (x.getInternalName().equals("moduleUUID"))
			{
				options.put(x, new HashSet<String>(Arrays.asList(new String[] {"4822ec5a-fa64-5e0c-b88b-e81fff954eb9"})));
			}
		}
		
		System.out.println(ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.rf2", "rf2-src-data-us-extension", "20150301"), 
			"3.3-SNAPSHOT", 
			new SDOSourceContent[0], 
			new IBDFFile[] {new IBDFFile("gov.vha.isaac.terminology.converted", "rf2-ibdf-sct", "20150731-loader-3.3-SNAPSHOT", "Snapshot")},
				options, gitTestURL, gitUsername, gitPassword));
		
		//rxnorm
		
//		ConverterOptionParam[] optionTypes = ContentConverterCreator.getConverterOptions(new Converter("gov.vha.isaac.terminology.converters", "rxnorm-mojo", "5.1-SNAPSHOT"), 
//				nexusUrl, nexusUsername, nexusPassword);
//		
//		HashMap<ConverterOptionParam, Set<String>> options = new HashMap<>();
//		for (ConverterOptionParam x : optionTypes)
//		{
//			if (x.getInternalName().equals("ttyRestriction"))
//			{
//				options.put(x, new HashSet<String>(Arrays.asList(new String[] {"IN", "SCD"})));
//			}
//			else if (x.getInternalName().equals("sabsToInclude"))
//			{
//				options.put(x, new HashSet<String>(Arrays.asList(new String[] {"ATC"})));
//			}
//		}
//		
//		System.out.println(ContentConverterCreator.createContentConverter(new SDOSourceContent("gov.vha.isaac.terminology.source.rxnorm", "rxnorm-src-data", "2016.05.02"), 
//		"5.1-SNAPSHOT", 
//		new SDOSourceContent[0], 
//		new IBDFFile[] {new IBDFFile("gov.vha.isaac.terminology.converted", "rf2-ibdf-sct", "20150731-loader-3.1-SNAPSHOT", "Snapshot")},
//			options,
//			gitTestURL, gitUsername, gitPassword));
	}
}

package gov.vha.isaac.ochre.integration.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.testing.hk2testng.HK2;
import org.testng.Assert;
import org.testng.annotations.Test;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.impl.utility.Frills;

/**
 * 
 * {@link FrillsTest}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@HK2("integration")
public class FrillsTest {
	private static final Logger LOG = LogManager.getLogger();


	@Test(groups = { "frills" }, dependsOnGroups = { "load" })
	public void testChildren() {
		LOG.info("Testing Child methods");
		
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_ASSEMBLAGE.getConceptSequence(), true, true).size(), 10);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_ASSEMBLAGE.getConceptSequence(), false, true).size(), 10);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_ASSEMBLAGE.getConceptSequence(), true, false).size(), 10);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.DESCRIPTION_ASSEMBLAGE.getConceptSequence(), false, false).size(), 10);
		
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.FEATURE.getConceptSequence(), false, false).size(), 0);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.FEATURE.getConceptSequence(), true, false).size(), 0);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.FEATURE.getConceptSequence(), false, true).size(), 0);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.FEATURE.getConceptSequence(), true, true).size(), 0);
		
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.VERSION_PROPERTIES.getConceptSequence(), false, false).size(), 8);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.VERSION_PROPERTIES.getConceptSequence(), true, false).size(), 12);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.VERSION_PROPERTIES.getConceptSequence(), false, true).size(), 7);
		Assert.assertEquals(Frills.getAllChildrenOfConcept(MetaData.VERSION_PROPERTIES.getConceptSequence(), true, true).size(), 11);
		
		Assert.assertTrue(Frills.definesIdentifierSememe(MetaData.SCTID.getConceptSequence()));
		Assert.assertTrue(Frills.definesIdentifierSememe(MetaData.SCTID.getNid()));
		
		Assert.assertTrue(Frills.definesIdentifierSememe(MetaData.VUID.getNid()));
		Assert.assertTrue(Frills.definesIdentifierSememe(MetaData.CODE.getNid()));
		Assert.assertTrue(Frills.definesIdentifierSememe(MetaData.GENERATED_UUID.getNid()));
		
		Assert.assertFalse(Frills.definesIdentifierSememe(MetaData.ACCEPTABLE.getNid()));
	}

}

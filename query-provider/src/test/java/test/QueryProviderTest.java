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
package test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import gov.vha.isaac.MetaData;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.constants.Constants;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.mojo.IndexTermstore;
import gov.vha.isaac.ochre.mojo.LoadTermstore;
import gov.vha.isaac.ochre.query.provider.lucene.LuceneIndexer;
import gov.vha.isaac.ochre.query.provider.lucene.indexers.DescriptionIndexer;

public class QueryProviderTest
{
	String query_ = "dynamic*";
	String field_ = "_string_content_";
	boolean prefixSearch_ = true;
	boolean metadataOnly_ = false;
	
	StampCoordinate stamp1_;
	StampCoordinate stamp2_;
	StampCoordinate stamp3_;
	StampCoordinate stamp4_;
	
	Query q_base_ = null;
	Query q_stamp0_ = null;
	Query q_stamp1_ = null;
	Query q_stamp2_ = null;
	Query q_stamp3_ = null;
	Query q_stamp4_ = null;
	
	LuceneIndexer li_ = null;
	
	@BeforeClass
	public void configure() throws Exception
	{
		try 
		{
			File db = new File("target/db");
			FileUtils.deleteDirectory(db);
			db.mkdirs();
			System.setProperty(Constants.DATA_STORE_ROOT_LOCATION_PROPERTY, db.getCanonicalPath());
			LookupService.startupIsaac();
			LoadTermstore lt = new LoadTermstore();
			lt.setLog(new SystemStreamLog());
			lt.setibdfFilesFolder(new File("src/test/resources/ibdf/"));
			lt.execute();
			new IndexTermstore().execute();
			
			li_ = LookupService.get().getService(DescriptionIndexer.class);
			
			stamp1_ = Get.coordinateFactory().createDevelopmentLatestStampCoordinate();
			stamp2_ = Get.coordinateFactory().createStampCoordinate(Get.conceptSpecification(stamp1_.getStampPosition().getStampPath().getPathConceptSequence()), 
					stamp1_.getStampPrecedence(), 
					Arrays.asList(Get.conceptSpecification(MetaData.ISAAC_MODULE.getConceptSequence())),
					stamp1_.getAllowedStates(),
					2017, 1, 1, 1, 0, 0);
			stamp3_ = Get.coordinateFactory().createStampCoordinate(Get.conceptSpecification(stamp1_.getStampPosition().getStampPath().getPathConceptSequence()), 
					stamp1_.getStampPrecedence(), 
					Arrays.asList(Get.conceptSpecification(MetaData.ISAAC_MODULE.getConceptSequence()),
							Get.conceptSpecification(MetaData.LOINC_MODULES.getConceptSequence()),
							Get.conceptSpecification(MetaData.VHAT_MODULES.getConceptSequence())),
					stamp1_.getAllowedStates(),
					2017, 1, 1, 1, 0, 0);
			stamp4_ = Get.coordinateFactory().createMasterLatestActiveOnlyStampCoordinate();
			
			Method buildTokenizedStringQuery = LuceneIndexer.class
					.getDeclaredMethod("buildTokenizedStringQuery", String.class, String.class, boolean.class, boolean.class);
			buildTokenizedStringQuery.setAccessible(true);
			
			Method buildStampQuery = LuceneIndexer.class
					.getDeclaredMethod("buildStampQuery", Query.class, StampCoordinate.class);
			buildStampQuery.setAccessible(true);
			
			q_base_ = (Query) buildTokenizedStringQuery.invoke(li_, query_, field_, prefixSearch_, metadataOnly_);
			q_stamp0_ = (Query) buildStampQuery.invoke(li_, q_base_, null);
			q_stamp1_ = (Query) buildStampQuery.invoke(li_, q_base_, stamp1_);
			q_stamp2_ = (Query) buildStampQuery.invoke(li_, q_base_, stamp2_);
			q_stamp3_ = (Query) buildStampQuery.invoke(li_, q_base_, stamp3_);
			q_stamp4_ = (Query) buildStampQuery.invoke(li_, q_base_, stamp4_);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@AfterClass
	public void shutdown()
	{
		LookupService.shutdownSystem();
	}

	@Test
	public void test_buildStampQuery() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
	{
		Assert.assertEquals(q_base_, q_stamp0_);
		
		String str_q_stamp1 = q_stamp1_.toString();
		boolean queryContainsDevPath = str_q_stamp1.contains(MetaData.DEVELOPMENT_PATH.getPrimordialUuid().toString());
		boolean queryContainsMasterPath = str_q_stamp1.contains(MetaData.MASTER_PATH.getPrimordialUuid().toString());
		Assert.assertNotEquals(q_base_, q_stamp1_);
		Assert.assertTrue(queryContainsDevPath);
		Assert.assertFalse(queryContainsMasterPath);
		
		String str_q_stamp2 = q_stamp2_.toString();
		boolean queryContainsIsaacModule = str_q_stamp2.contains(MetaData.ISAAC_MODULE.getPrimordialUuid().toString());
		boolean queryContainsLoincModule = str_q_stamp2.contains(MetaData.LOINC_MODULES.getPrimordialUuid().toString());
		Assert.assertNotEquals(q_base_, q_stamp2_);
		Assert.assertTrue(queryContainsIsaacModule);
		Assert.assertFalse(queryContainsLoincModule);
		
		String str_q_stamp3 = q_stamp3_.toString();
		boolean queryContainsIsaacLoincVhatModule = str_q_stamp3.contains(MetaData.ISAAC_MODULE.getPrimordialUuid().toString())
								&& str_q_stamp3.contains(MetaData.LOINC_MODULES.getPrimordialUuid().toString())
								&& str_q_stamp3.contains(MetaData.VHAT_MODULES.getPrimordialUuid().toString());
		boolean queryContainsCptModule = str_q_stamp3.contains(MetaData.CPT_MODULES.getPrimordialUuid().toString());
		Assert.assertNotEquals(q_base_, q_stamp3_);
		Assert.assertTrue(queryContainsIsaacLoincVhatModule);
		Assert.assertFalse(queryContainsCptModule);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void test_search() throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Method search = LuceneIndexer.class
				.getDeclaredMethod("search", Query.class, int.class, Long.class, Predicate.class, StampCoordinate.class);
		search.setAccessible(true);
		
		// No stamp (null)
		List<SearchResult> result = (List<SearchResult>) search.invoke(li_, q_base_, 100, Long.MAX_VALUE, null, null);
		Assert.assertEquals(result.size(), 29);
		
		// Dev latest stamp coord
		result = (List<SearchResult>) search.invoke(li_, q_stamp1_, 100, Long.MAX_VALUE, null, stamp1_);
		Assert.assertEquals(result.size(), 29);
		
		// Dev with Isaac stamp coord 
		result = (List<SearchResult>) search.invoke(li_, q_stamp2_, 10, Long.MAX_VALUE, null, stamp2_);
		Assert.assertEquals(result.size(), 10);
		
		// Master latest stamp coord
		result = (List<SearchResult>) search.invoke(li_, q_stamp4_, 100, Long.MAX_VALUE, null, stamp4_);
		Assert.assertEquals(result.size(), 0);
	}
}

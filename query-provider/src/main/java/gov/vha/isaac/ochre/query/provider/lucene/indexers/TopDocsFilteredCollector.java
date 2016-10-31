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
package gov.vha.isaac.ochre.query.provider.lucene.indexers;

import java.io.IOException;
import java.util.function.Predicate;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import gov.vha.isaac.ochre.query.provider.lucene.LuceneIndexer;

/**
 * {@link TopDocsFilteredCollector}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class TopDocsFilteredCollector extends Collector
{
	TopScoreDocCollector collector_;
	IndexSearcher searcher_;
	Predicate<Integer> filter_;
	
	/**
	 * @param numHits - how many results to return
	 * @param query - needed to setup the TopScoreDocCollector properly
	 * @param searcher - needed to read the nids out of the matching documents
	 * @param filter - a predicate that should return true, if the given nid should be allowed in the results, false, if not.
	 * @throws IOException 
	 */
	public TopDocsFilteredCollector(int numHits, Query query, IndexSearcher searcher, Predicate<Integer> filter) throws IOException
	{
		collector_ = TopScoreDocCollector.create(numHits, null, !searcher.createNormalizedWeight(query).scoresDocsOutOfOrder());
		searcher_ = searcher;
		filter_ = filter;
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException
	{
		collector_.setScorer(scorer);
	}

	@Override
	public void collect(int docId) throws IOException
	{
		Document document = searcher_.doc(docId);
		int componentNid = document.getField(LuceneIndexer.FIELD_COMPONENT_NID).numericValue().intValue();
		
		if (filter_.test(componentNid))
		{
			collector_.collect(docId);
		}
	}

	@Override
	public void setNextReader(AtomicReaderContext context) throws IOException
	{
		collector_.setNextReader(context);
	}

	@Override
	public boolean acceptsDocsOutOfOrder()
	{
		return collector_.acceptsDocsOutOfOrder();
	}
	
	public TopDocs getTopDocs()
	{
		return collector_.topDocs();
	}
}

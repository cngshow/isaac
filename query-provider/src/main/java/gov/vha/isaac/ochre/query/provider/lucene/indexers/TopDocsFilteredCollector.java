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
import java.util.HashMap;
import java.util.function.Predicate;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.FilterCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

import gov.vha.isaac.ochre.query.provider.lucene.LuceneIndexer;

/**
 * {@link TopDocsFilteredCollector}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class TopDocsFilteredCollector extends FilterCollector
{
	IndexSearcher searcher_;
	Predicate<Integer> filter_;
	HashMap<LeafReaderContext, LeafCollector> collectors = new HashMap<>();
	
	/**
	 * @param numHits - how many results to return
	 * @param lastDoc - the document at the bottom of the previous page
	 * @param query - needed to setup the TopScoreDocCollector properly
	 * @param searcher - needed to read the nids out of the matching documents
	 * @param filter - a predicate that should return true, if the given nid should be allowed in the results, false, if not.
	 * @throws IOException 
	 */
	public TopDocsFilteredCollector(int numHits, ScoreDoc after, IndexSearcher searcher, Predicate<Integer> filter) throws IOException
	{
		super(TopScoreDocCollector.create(numHits, after));
		searcher_ = searcher;
		filter_ = filter;
	}

	/**
	 * @param numHits - how many results to return
	 * @param query - needed to setup the TopScoreDocCollector properly
	 * @param searcher - needed to read the nids out of the matching documents
	 * @param filter - a predicate that should return true, if the given nid should be allowed in the results, false, if not.
	 * @throws IOException 
	 */
	public TopDocsFilteredCollector(int numHits, IndexSearcher searcher, Predicate<Integer> filter) throws IOException
	{
		this(numHits, null, searcher, filter);
	}

	@Override
	public LeafCollector getLeafCollector(final LeafReaderContext context) throws IOException {
		if (collectors.get(context) == null)
		{
			collectors.put(context, new LeafCollector() {
				
			LeafCollector delegate = in.getLeafCollector(context);
			int docBase = context.docBase;
			
			@Override
			public void setScorer(Scorer scorer) throws IOException {
				delegate.setScorer(scorer);
			}
			
			@Override
			public void collect(int docId) throws IOException {
				Document document = searcher_.doc(docId + docBase);
				int componentNid = document.getField(LuceneIndexer.FIELD_COMPONENT_NID).numericValue().intValue();
				if (filter_.test(componentNid))
				{
					delegate.collect(docId);
				}
				else
				{
					System.out.println("Filter says no for doc " + (docId + docBase));
					System.out.println(document.toString());
				}
			}
			});
		}
		return collectors.get(context);
	}
	
	public TopDocs topDocs()
	{
		return ((TopScoreDocCollector)in).topDocs();
	}
}

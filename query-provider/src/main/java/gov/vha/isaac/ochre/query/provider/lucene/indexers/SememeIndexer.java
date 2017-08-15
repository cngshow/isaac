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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LegacyDoubleField;
import org.apache.lucene.document.LegacyFloatField;
import org.apache.lucene.document.LegacyIntField;
import org.apache.lucene.document.LegacyLongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.LongSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.StringSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArray;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeBoolean;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeByteArray;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeDouble;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeFloat;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeInteger;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeLong;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememePolymorphic;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeSequence;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.api.index.SememeIndexerBI;
import gov.vha.isaac.ochre.api.logic.LogicNode;
import gov.vha.isaac.ochre.api.tree.TreeNodeVisitData;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeLongImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNidImpl;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeStringImpl;
import gov.vha.isaac.ochre.query.provider.lucene.LuceneIndexer;
import gov.vha.isaac.ochre.query.provider.lucene.PerFieldAnalyzer;

/**
 * This class provides indexing for all String, Nid, Long and Logic Graph sememe types.
 * 
 * Additionally, this class provides flexible indexing of all DynamicSememe data types.
 *
 * @author kec
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service(name = "sememe indexer")
@RunLevel(value = LookupService.SL_L2_DATABASE_SERVICES_STARTED_RUNLEVEL)
public class SememeIndexer extends LuceneIndexer implements SememeIndexerBI
{
	private static final Logger log = LogManager.getLogger();

	public static final String INDEX_NAME = "sememes";
	private static final String COLUMN_FIELD_DATA = "colData";

	@Inject private SememeIndexerConfiguration lric;

	private SememeIndexer() throws IOException
	{
		//For HK2
		super(INDEX_NAME);
	}
	
	@Override
	protected boolean indexChronicle(ObjectChronology<?> chronicle)
	{
		if (chronicle instanceof SememeChronology<?>)
		{
			SememeChronology<?> sememeChronology = (SememeChronology<?>) chronicle;
			if (sememeChronology.getSememeType() == SememeType.DYNAMIC || sememeChronology.getSememeType() == SememeType.STRING 
					|| sememeChronology.getSememeType() == SememeType.LONG || sememeChronology.getSememeType() == SememeType.COMPONENT_NID 
					|| sememeChronology.getSememeType() == SememeType.LOGIC_GRAPH)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	protected void addFields(ObjectChronology<?> chronicle, Document doc)
	{
		SememeChronology<?> sememeChronology = (SememeChronology<?>) chronicle;
		doc.add(new TextField(FIELD_SEMEME_ASSEMBLAGE_SEQUENCE, sememeChronology.getAssemblageSequence() + "", Field.Store.NO));

		for (Object sv : sememeChronology.getVersionList())
		{
			if (sv instanceof DynamicSememe)
			{
				DynamicSememe<?> dsv = (DynamicSememe<?>) sv;

				Integer[] columns = lric.whatColumnsToIndex(dsv.getAssemblageSequence());
				if (columns != null)
				{
					int dataColCount = dsv.getData().length;
					for (int col : columns)
					{
						DynamicSememeData dataCol = col >= dataColCount ? null : dsv.getData(col);
						//Only pass in a column number if we were asked to index more than one column for this sememe
						handleType(doc, dataCol, columns.length > 1 ? col : -1);  
					}
				}
			}
			//TODO enhance the index configuration to allow us to configure Static sememes as indexed, or not indexed
			//static sememe types are never more than 1 column, always pass -1
			else if (sv instanceof StringSememe)
			{
				StringSememe<?> ssv = (StringSememe<?>) sv;
				handleType(doc, new DynamicSememeStringImpl(ssv.getString()), -1);
				incrementIndexedItemCount("Sememe String");
			}
			else if (sv instanceof LongSememe)
			{
				LongSememe<?> lsv = (LongSememe<?>) sv;
				handleType(doc, new DynamicSememeLongImpl(lsv.getLongValue()), -1);
				incrementIndexedItemCount("Sememe Long");
			}
			else if (sv instanceof ComponentNidSememe)
			{
				ComponentNidSememe<?> csv = (ComponentNidSememe<?>) sv;
				handleType(doc, new DynamicSememeNidImpl(csv.getComponentNid()), -1);
				incrementIndexedItemCount("Sememe Component Nid");
			}
			else if (sv instanceof LogicGraphSememe)
			{
				LogicGraphSememe<?> lgsv = (LogicGraphSememe<?>) sv;
				ConceptSequenceSet css = new ConceptSequenceSet();
				lgsv.getLogicalExpression().processDepthFirst((LogicNode logicNode, TreeNodeVisitData data) -> {
					logicNode.addConceptsReferencedByNode(css);
				});
				css.stream().forEach(sequence -> 
				{
					handleType(doc, new DynamicSememeNidImpl(Get.identifierService().getConceptNid(sequence)), -1);
				});
			}
			else
			{
				log.error("Unexpected type handed to addFields in Sememe Indexer: " + sememeChronology.toString());
			}
		}
		
		//Due to indexing all of the versions, we may have added duplicate field name/value combinations to the document.
		//Remove the dupes.
		Iterator<IndexableField> it = doc.iterator();
		HashSet<String> uniqueFields = new HashSet<>();
		while (it.hasNext())
		{
			IndexableField field = it.next();
			String temp = field.name() + "::" + field.stringValue();
			
			if (uniqueFields.contains(temp))
			{
				it.remove();
			}
			else
			{
				uniqueFields.add(temp);
			}
		}
	}

	private void handleType(Document doc, DynamicSememeData dataCol, int colNumber)
	{
		//Not the greatest design for diskspace / performance... but if we want to be able to support searching across 
		//all fields / all sememes - and also support searching per-field within a single sememe, we need to double index 
		//all of the data.  Once with a standard field name, and once with a field name that includes the column number.
		//at search time, restricting to certain field matches is only allowed if they are also restricting to an assemblage,
		//so we can compute the correct field number list at search time.
		
		//Note, we optimize by only doing the double indexing in cases where the sememe has more than one column to begin with.
		//At query time, we construct the query appropriately to handle this optimization.

		//the cheaper option from a disk space perspective (maybe, depending on the data) would be to create a document per 
		//column.  The queries would be trivial to write then, but we would be duplicating the component nid and assemblage nid
		//in each document, which is also expensive.  It also doesn't fit the model in OTF, of a document per component.

		//We also duplicate again, on string fields by indexing with the white space analyzer, in addition to the normal one.
		if (dataCol == null)
		{
			//noop
		}
		else if (dataCol instanceof DynamicSememeBoolean)
		{
			doc.add(new StringField(COLUMN_FIELD_DATA + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, ((DynamicSememeBoolean) dataCol).getDataBoolean() + "", Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new StringField(COLUMN_FIELD_DATA + "_" + colNumber + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, 
						((DynamicSememeBoolean) dataCol).getDataBoolean() + "", Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe Boolean");
		}
		else if (dataCol instanceof DynamicSememeByteArray)
		{
			log.warn("Sememe Indexer configured to index a field that isn''t indexable (byte array)");
		}
		else if (dataCol instanceof DynamicSememeDouble)
		{
			doc.add(new LegacyDoubleField(COLUMN_FIELD_DATA, ((DynamicSememeDouble) dataCol).getDataDouble(), Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new LegacyDoubleField(COLUMN_FIELD_DATA + "_" + colNumber, ((DynamicSememeDouble) dataCol).getDataDouble(), Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe Double");
		}
		else if (dataCol instanceof DynamicSememeFloat)
		{
			doc.add(new LegacyFloatField(COLUMN_FIELD_DATA, ((DynamicSememeFloat) dataCol).getDataFloat(), Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new LegacyFloatField(COLUMN_FIELD_DATA + "_" + colNumber, ((DynamicSememeFloat) dataCol).getDataFloat(), Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe Float");
		}
		else if (dataCol instanceof DynamicSememeInteger)
		{
			doc.add(new LegacyIntField(COLUMN_FIELD_DATA, ((DynamicSememeInteger) dataCol).getDataInteger(), Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new LegacyIntField(COLUMN_FIELD_DATA + "_" + colNumber, ((DynamicSememeInteger) dataCol).getDataInteger(), Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe Integer");
		}
		else if (dataCol instanceof DynamicSememeSequence)
		{
			doc.add(new LegacyIntField(COLUMN_FIELD_DATA, ((DynamicSememeSequence) dataCol).getDataSequence(), Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new LegacyIntField(COLUMN_FIELD_DATA + "_" + colNumber, ((DynamicSememeSequence) dataCol).getDataSequence(), Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe Sequence");
		}
		else if (dataCol instanceof DynamicSememeLong)
		{
			doc.add(new LegacyLongField(COLUMN_FIELD_DATA, ((DynamicSememeLong) dataCol).getDataLong(), Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new LegacyLongField(COLUMN_FIELD_DATA + "_" + colNumber, ((DynamicSememeLong) dataCol).getDataLong(), Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe Long");
		}
		else if (dataCol instanceof DynamicSememeNid)
		{
			//No need for ranges on a nid, no need for tokenization (so textField, instead of string field).
			doc.add(new StringField(COLUMN_FIELD_DATA + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, ((DynamicSememeNid) dataCol).getDataNid() + "", Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new StringField(COLUMN_FIELD_DATA + "_" + colNumber + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER,
						((DynamicSememeNid) dataCol).getDataNid() + "", Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe Nid");
		}
		else if (dataCol instanceof DynamicSememePolymorphic)
		{
			log.error("This should have been impossible (polymorphic?)");
		}
		else if (dataCol instanceof DynamicSememeString)
		{
			doc.add(new TextField(COLUMN_FIELD_DATA, ((DynamicSememeString) dataCol).getDataString(), Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new TextField(COLUMN_FIELD_DATA + "_" + colNumber, ((DynamicSememeString) dataCol).getDataString(), Store.NO));
			}
			//yes, indexed 4 different times - twice with the standard analyzer, twice with the whitespace analyzer.
			doc.add(new TextField(COLUMN_FIELD_DATA + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, ((DynamicSememeString) dataCol).getDataString(), Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new TextField(COLUMN_FIELD_DATA + "_" + colNumber + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, ((DynamicSememeString) dataCol).getDataString(),
					Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe String");
		}
		else if (dataCol instanceof DynamicSememeUUID)
		{
			//Use the whitespace analyzer on UUIDs
			doc.add(new StringField(COLUMN_FIELD_DATA + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, ((DynamicSememeUUID) dataCol).getDataUUID().toString(), Store.NO));
			if (colNumber >= 0)
			{
				doc.add(new StringField(COLUMN_FIELD_DATA + "_" + colNumber + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER,
					((DynamicSememeUUID) dataCol).getDataUUID().toString(), Store.NO));
			}
			incrementIndexedItemCount("Dynamic Sememe UUID");
		}
		else if (dataCol instanceof DynamicSememeArray<?>)
		{
			for (DynamicSememeData nestedData : ((DynamicSememeArray<?>) dataCol).getDataArray())
			{
				handleType(doc, nestedData, colNumber);
			}
		}
		else
		{
			log.error("This should have been impossible (no match on col type) {}", dataCol);
		}
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.query.provider.lucene.indexers.SememeIndexerItf#queryNumericRange(gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData, boolean, gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData, boolean, java.lang.Integer[], java.lang.Integer[], int, java.lang.Long)
	 */
	@Override
	public final List<SearchResult> queryNumericRange(final DynamicSememeData queryDataLower, final boolean queryDataLowerInclusive,
			final DynamicSememeData queryDataUpper, final boolean queryDataUpperInclusive, Integer[] sememeConceptSequence, Integer[] searchColumns, int sizeLimit,
			Long targetGeneration, StampCoordinate stamp)
	{
		Query q = new QueryWrapperForColumnHandling()
		{
			@Override
			Query buildQuery(String columnName)
			{
				return buildNumericQuery(queryDataLower, queryDataLowerInclusive, queryDataUpper, queryDataUpperInclusive, columnName);
			}
		}.buildColumnHandlingQuery(sememeConceptSequence, searchColumns);
		
		
		return search(restrictToSememe(q, sememeConceptSequence), sizeLimit, targetGeneration, null, stamp);
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.query.provider.lucene.indexers.SememeIndexerItf#query(java.lang.String, boolean, java.lang.Integer[], int, java.lang.Long)
	 */
	@Override
	public final List<SearchResult> query(String queryString, boolean prefixSearch, Integer[] sememeConceptSequence, int sizeLimit, Long targetGeneration, StampCoordinate stamp)
	{
		return query(new DynamicSememeStringImpl(queryString), prefixSearch, sememeConceptSequence, null, sizeLimit, targetGeneration, null, stamp);
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.index.SememeIndexerBI#query(java.lang.String, boolean, java.lang.Integer[], int, java.lang.Long, java.util.function.Predicate)
	 */
	@Override
	public final List<SearchResult> query(String queryString, boolean prefixSearch, Integer[] sememeConceptSequence, int sizeLimit, Long targetGeneration, Predicate<Integer> filter, StampCoordinate stamp)
	{
		return query(new DynamicSememeStringImpl(queryString), prefixSearch, sememeConceptSequence, null, sizeLimit, targetGeneration, filter, stamp);
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.query.provider.lucene.indexers.SememeIndexerItf#query(gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData, boolean, java.lang.Integer[], java.lang.Integer[], int, java.lang.Long)
	 */
	//TODO fix this limitation on the column restriction...
	@Override
	public final List<SearchResult> query(final DynamicSememeData queryData, final boolean prefixSearch, Integer[] sememeConceptSequence, 
			Integer[] searchColumns, int sizeLimit, Long targetGeneration, StampCoordinate stamp)
	{
		return query(queryData, prefixSearch, sememeConceptSequence, searchColumns, sizeLimit, targetGeneration, (Predicate<Integer>)null, stamp);
	}
	
	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.api.index.SememeIndexerBI#query(gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeData, boolean, java.lang.Integer[], java.lang.Integer[], int, java.lang.Long, java.util.function.Predicate)
	 */
	@Override
	public final List<SearchResult> query(final DynamicSememeData queryData, final boolean prefixSearch, Integer[] sememeConceptSequence, 
			Integer[] searchColumns, int sizeLimit, Long targetGeneration, Predicate<Integer> filter, StampCoordinate stamp)
	{
		Query q = null;

		if (queryData instanceof DynamicSememeString)
		{
			q = new QueryWrapperForColumnHandling()
			{
				@Override
				Query buildQuery(String columnName)
				{
					//This is the only query type that needs tokenizing, etc.
					String queryString = ((DynamicSememeString) queryData).getDataString();
					//'-' signs are operators to lucene... but we want to allow nid lookups.  So escape any leading hyphens
					//and any hyphens that are preceeded by spaces.  This way, we don't mess up UUID handling.
					//(lucene handles UUIDs ok, because the - sign is only treated special at the beginning, or when preceeded by a space)

					if (queryString.startsWith("-"))
					{
						queryString = "\\" + queryString;
					}
					queryString = queryString.replaceAll("\\s-", " \\\\-");
					log.debug("Modified search string is: ''{}''", queryString);
					return buildTokenizedStringQuery(queryString, columnName, prefixSearch, false);
				}
			}.buildColumnHandlingQuery(sememeConceptSequence, searchColumns);
		}
		else
		{
			if (queryData instanceof DynamicSememeBoolean || queryData instanceof DynamicSememeNid || queryData instanceof DynamicSememeUUID)
			{
				q = new QueryWrapperForColumnHandling()
				{
					@Override
					Query buildQuery(String columnName)
					{
						return new TermQuery(new Term(columnName + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER , queryData.getDataObject().toString()));
					}
				}.buildColumnHandlingQuery(sememeConceptSequence, searchColumns);
			}
			else if (queryData instanceof DynamicSememeDouble || queryData instanceof DynamicSememeFloat || queryData instanceof DynamicSememeInteger
					|| queryData instanceof DynamicSememeLong || queryData instanceof DynamicSememeSequence)
			{
				q = new QueryWrapperForColumnHandling()
				{
					@Override
					Query buildQuery(String columnName)
					{
						Query temp = buildNumericQuery(queryData, true, queryData, true, columnName);

						if ((queryData instanceof DynamicSememeLong && ((DynamicSememeLong) queryData).getDataLong() < 0)
								|| (queryData instanceof DynamicSememeInteger && ((DynamicSememeInteger) queryData).getDataInteger() < 0))
						{
							//Looks like a nid... wrap in an or clause that would do a match on the exact term if it was indexed as a nid, rather than a numeric
							Builder wrapper = new BooleanQuery.Builder();
							wrapper.add(new TermQuery(new Term(columnName, queryData.getDataObject().toString())), Occur.SHOULD);
							wrapper.add(temp, Occur.SHOULD);
							temp = wrapper.build();
						}
						return temp;
					}
				}.buildColumnHandlingQuery(sememeConceptSequence, searchColumns);
			}
			else if (queryData instanceof DynamicSememeByteArray)
			{
				throw new RuntimeException("DynamicSememeByteArray isn't indexed");
			}
			else if (queryData instanceof DynamicSememePolymorphic)
			{
				throw new RuntimeException("This should have been impossible (polymorphic?)");
			}
			else if (queryData instanceof DynamicSememeArray)
			{
				throw new RuntimeException("DynamicSememeArray isn't a searchable type");
			}
			else
			{
				log.error("This should have been impossible (no match on col type)");
				throw new RuntimeException("unexpected error, see logs");
			}
		}
		return search(restrictToSememe(q, sememeConceptSequence), sizeLimit, targetGeneration, filter, stamp);
	}

	/* (non-Javadoc)
	 * @see gov.vha.isaac.ochre.query.provider.lucene.indexers.SememeIndexerItf#query(int, java.lang.Integer[], java.lang.Integer[], int, java.lang.Long)
	 */
	@Override
	public List<SearchResult> query(int nid, Integer[] sememeConceptSequence, Integer[] searchColumns, int sizeLimit, Long targetGeneration, StampCoordinate stamp)
	{
		Query q = new QueryWrapperForColumnHandling()
		{
			@Override
			Query buildQuery(String columnName)
			{
				return new TermQuery(new Term(columnName + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, nid + ""));
			}
		}.buildColumnHandlingQuery(sememeConceptSequence, searchColumns);
		
		return search(restrictToSememe(q, sememeConceptSequence), sizeLimit, targetGeneration, null, stamp);
	}
	
	private Query buildNumericQuery(DynamicSememeData queryDataLower, boolean queryDataLowerInclusive, DynamicSememeData queryDataUpper,
			boolean queryDataUpperInclusive, String columnName)
	{
		//Convert both to the same type (if they differ) - go largest data type to smallest, so we don't lose precision
		//Also - if they pass in longs that would fit in an int, also generate an int query.
		//likewise, with Double - if they pass in a double, that would fit in a float, also generate a float query.
		try
		{
			Builder bq = new BooleanQuery.Builder();
			boolean fitsInFloat = false;
			boolean fitsInInt = false;
			if (queryDataLower instanceof DynamicSememeDouble || queryDataUpper instanceof DynamicSememeDouble)
			{
				Double upperVal = (queryDataUpper == null ? null
						: (queryDataUpper instanceof DynamicSememeDouble ? ((DynamicSememeDouble) queryDataUpper).getDataDouble()
								: ((Number) queryDataUpper.getDataObject()).doubleValue()));
				Double lowerVal = (queryDataLower == null ? null
						: (queryDataLower instanceof DynamicSememeDouble ? ((DynamicSememeDouble) queryDataLower).getDataDouble()
								: ((Number) queryDataLower.getDataObject()).doubleValue()));
				bq.add(LegacyNumericRangeQuery.newDoubleRange(columnName, lowerVal, upperVal, queryDataLowerInclusive, queryDataUpperInclusive), Occur.SHOULD);

				if ((upperVal != null && upperVal <= Float.MAX_VALUE && upperVal >= Float.MIN_VALUE)
						|| (lowerVal != null && lowerVal <= Float.MAX_VALUE && lowerVal >= Float.MIN_VALUE))
				{
					fitsInFloat = true;
				}
			}

			if (fitsInFloat || queryDataLower instanceof DynamicSememeFloat || queryDataUpper instanceof DynamicSememeFloat)
			{
				Float upperVal = (queryDataUpper == null ? null
						: (queryDataUpper == null ? null
								: (queryDataUpper instanceof DynamicSememeFloat ? ((DynamicSememeFloat) queryDataUpper).getDataFloat()
										: (fitsInFloat && ((Number) queryDataUpper.getDataObject()).doubleValue() > Float.MAX_VALUE ? Float.MAX_VALUE
												: ((Number) queryDataUpper.getDataObject()).floatValue()))));
				Float lowerVal = (queryDataLower == null ? null
						: (queryDataLower instanceof DynamicSememeFloat ? ((DynamicSememeFloat) queryDataLower).getDataFloat()
								: (fitsInFloat && ((Number) queryDataLower.getDataObject()).doubleValue() < Float.MIN_VALUE ? Float.MIN_VALUE
										: ((Number) queryDataLower.getDataObject()).floatValue())));
				bq.add(LegacyNumericRangeQuery.newFloatRange(columnName, lowerVal, upperVal, queryDataLowerInclusive, queryDataUpperInclusive), Occur.SHOULD);
			}

			if (queryDataLower instanceof DynamicSememeLong || queryDataUpper instanceof DynamicSememeLong)
			{
				Long upperVal = (queryDataUpper == null ? null
						: (queryDataUpper instanceof DynamicSememeLong ? ((DynamicSememeLong) queryDataUpper).getDataLong()
								: ((Number) queryDataUpper.getDataObject()).longValue()));
				Long lowerVal = (queryDataLower == null ? null
						: (queryDataLower instanceof DynamicSememeLong ? ((DynamicSememeLong) queryDataLower).getDataLong()
								: ((Number) queryDataLower.getDataObject()).longValue()));
				bq.add(LegacyNumericRangeQuery.newLongRange(columnName, lowerVal, upperVal, queryDataLowerInclusive, queryDataUpperInclusive), Occur.SHOULD);
				if ((upperVal != null && upperVal <= Integer.MAX_VALUE && upperVal >= Integer.MIN_VALUE)
						|| (lowerVal != null && lowerVal <= Integer.MAX_VALUE && lowerVal >= Integer.MIN_VALUE))
				{
					fitsInInt = true;
				}
			}

			if (fitsInInt || queryDataLower instanceof DynamicSememeInteger || queryDataUpper instanceof DynamicSememeInteger
					|| queryDataLower instanceof DynamicSememeSequence || queryDataUpper instanceof DynamicSememeSequence)
			{
				Integer upperVal = (queryDataUpper == null ? null
						: (queryDataUpper instanceof DynamicSememeInteger ? ((DynamicSememeInteger) queryDataUpper).getDataInteger()
								: (queryDataUpper instanceof DynamicSememeSequence ? ((DynamicSememeSequence) queryDataUpper).getDataSequence()
										: (fitsInInt && ((Number) queryDataUpper.getDataObject()).longValue() > Integer.MAX_VALUE ? Integer.MAX_VALUE
												: ((Number) queryDataUpper.getDataObject()).intValue()))));
				Integer lowerVal = (queryDataLower == null ? null
						: (queryDataLower instanceof DynamicSememeInteger ? ((DynamicSememeInteger) queryDataLower).getDataInteger()
								: (queryDataLower instanceof DynamicSememeSequence ? ((DynamicSememeSequence) queryDataLower).getDataSequence()
										: (fitsInInt && ((Number) queryDataLower.getDataObject()).longValue() < Integer.MIN_VALUE ? Integer.MIN_VALUE
												: ((Number) queryDataLower.getDataObject()).intValue()))));
				bq.add(LegacyNumericRangeQuery.newIntRange(columnName, lowerVal, upperVal, queryDataLowerInclusive, queryDataUpperInclusive), Occur.SHOULD);
			}
			
			BooleanQuery builtBQ = bq.build();
			
			if (builtBQ.clauses().size() == 0)
			{
				throw new RuntimeException("Not a numeric data type - can't perform a range query");
			}
			else
			{
				return new BooleanQuery.Builder().add(builtBQ, Occur.MUST).build();
			}
		}
		catch (ClassCastException e)
		{
			throw new RuntimeException("One of the values is not a numeric data type - can't perform a range query");
		}
	}

	private abstract class QueryWrapperForColumnHandling
	{
		abstract Query buildQuery(String columnName);

		protected Query buildColumnHandlingQuery(Integer[] sememeConceptSequence, Integer[] searchColumns)
		{
			Integer[] sememeIndexedColumns = null;
			
			if (searchColumns != null && searchColumns.length > 0)
			{
				//If they provide a search column - then they MUST provide one and only one sememeConceptSequence
				if (sememeConceptSequence == null || sememeConceptSequence.length != 1)
				{
					throw new RuntimeException("If a list of search columns is provided, then the sememeConceptSequence variable must contain 1 (and only 1) sememe");
				}
				else
				{
					sememeIndexedColumns = lric.whatColumnsToIndex(sememeConceptSequence[0]);
				}
			}
			
			//If only 1 column was indexed from a sememe, we don't create field specific columns.
			if (searchColumns == null || searchColumns.length == 0 || sememeIndexedColumns == null || sememeIndexedColumns.length < 2)
			{
				return buildQuery(COLUMN_FIELD_DATA);
			}
			else  //If they passed a specific column to search AND the dynamic sememe type has more than 1 indexed column, then do a column specific search.
			{
				Builder group = new BooleanQuery.Builder();
				for (int i : searchColumns)
				{
					group.add(buildQuery(COLUMN_FIELD_DATA + "_" + i), Occur.SHOULD);
				}
				return group.build();
			}
		}
	}
}

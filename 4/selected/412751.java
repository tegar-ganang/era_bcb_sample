package org.apache.lucene.search;

import java.io.IOException;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.MockRAMDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetDISI;

public class TestCachingWrapperFilter extends LuceneTestCase {

    public void testCachingWorks() throws Exception {
        Directory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new KeywordAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        writer.close();
        IndexReader reader = IndexReader.open(dir, true);
        MockFilter filter = new MockFilter();
        CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
        cacher.getDocIdSet(reader);
        assertTrue("first time", filter.wasCalled());
        cacher.getDocIdSet(reader);
        filter.clear();
        cacher.getDocIdSet(reader);
        assertFalse("second time", filter.wasCalled());
        reader.close();
    }

    public void testNullDocIdSet() throws Exception {
        Directory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new KeywordAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        writer.close();
        IndexReader reader = IndexReader.open(dir, true);
        final Filter filter = new Filter() {

            @Override
            public DocIdSet getDocIdSet(IndexReader reader) {
                return null;
            }
        };
        CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
        assertSame(DocIdSet.EMPTY_DOCIDSET, cacher.getDocIdSet(reader));
        reader.close();
    }

    public void testNullDocIdSetIterator() throws Exception {
        Directory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new KeywordAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        writer.close();
        IndexReader reader = IndexReader.open(dir, true);
        final Filter filter = new Filter() {

            @Override
            public DocIdSet getDocIdSet(IndexReader reader) {
                return new DocIdSet() {

                    @Override
                    public DocIdSetIterator iterator() {
                        return null;
                    }
                };
            }
        };
        CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
        assertSame(DocIdSet.EMPTY_DOCIDSET, cacher.getDocIdSet(reader));
        reader.close();
    }

    private static void assertDocIdSetCacheable(IndexReader reader, Filter filter, boolean shouldCacheable) throws IOException {
        final CachingWrapperFilter cacher = new CachingWrapperFilter(filter);
        final DocIdSet originalSet = filter.getDocIdSet(reader);
        final DocIdSet cachedSet = cacher.getDocIdSet(reader);
        assertTrue(cachedSet.isCacheable());
        assertEquals(shouldCacheable, originalSet.isCacheable());
        if (originalSet.isCacheable()) {
            assertEquals("Cached DocIdSet must be of same class like uncached, if cacheable", originalSet.getClass(), cachedSet.getClass());
        } else {
            assertTrue("Cached DocIdSet must be an OpenBitSet if the original one was not cacheable", cachedSet instanceof OpenBitSetDISI);
        }
    }

    public void testIsCacheAble() throws Exception {
        Directory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new KeywordAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
        writer.close();
        IndexReader reader = IndexReader.open(dir, true);
        assertDocIdSetCacheable(reader, new QueryWrapperFilter(new TermQuery(new Term("test", "value"))), false);
        assertDocIdSetCacheable(reader, NumericRangeFilter.newIntRange("test", Integer.valueOf(10000), Integer.valueOf(-10000), true, true), true);
        assertDocIdSetCacheable(reader, FieldCacheRangeFilter.newIntRange("test", Integer.valueOf(10), Integer.valueOf(20), true, true), true);
        assertDocIdSetCacheable(reader, new Filter() {

            @Override
            public DocIdSet getDocIdSet(IndexReader reader) {
                return new OpenBitSet();
            }
        }, true);
        reader.close();
    }

    public void testEnforceDeletions() throws Exception {
        Directory dir = new MockRAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
        IndexReader reader = writer.getReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        Document doc = new Document();
        doc.add(new Field("id", "1", Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(doc);
        reader = refreshReader(reader);
        searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(new MatchAllDocsQuery(), 1);
        assertEquals("Should find a hit...", 1, docs.totalHits);
        final Filter startFilter = new QueryWrapperFilter(new TermQuery(new Term("id", "1")));
        CachingWrapperFilter filter = new CachingWrapperFilter(startFilter, CachingWrapperFilter.DeletesMode.IGNORE);
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        assertEquals("[query + filter] Should find a hit...", 1, docs.totalHits);
        ConstantScoreQuery constantScore = new ConstantScoreQuery(filter);
        docs = searcher.search(constantScore, 1);
        assertEquals("[just filter] Should find a hit...", 1, docs.totalHits);
        writer.deleteDocuments(new Term("id", "1"));
        reader = refreshReader(reader);
        searcher = new IndexSearcher(reader);
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        assertEquals("[query + filter] Should *not* find a hit...", 0, docs.totalHits);
        docs = searcher.search(constantScore, 1);
        assertEquals("[just filter] Should find a hit...", 1, docs.totalHits);
        filter = new CachingWrapperFilter(startFilter, CachingWrapperFilter.DeletesMode.RECACHE);
        writer.addDocument(doc);
        reader = refreshReader(reader);
        searcher = new IndexSearcher(reader);
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        assertEquals("[query + filter] Should find a hit...", 1, docs.totalHits);
        constantScore = new ConstantScoreQuery(filter);
        docs = searcher.search(constantScore, 1);
        assertEquals("[just filter] Should find a hit...", 1, docs.totalHits);
        IndexReader newReader = refreshReader(reader);
        assertTrue(reader != newReader);
        reader = newReader;
        searcher = new IndexSearcher(reader);
        int missCount = filter.missCount;
        docs = searcher.search(constantScore, 1);
        assertEquals("[just filter] Should find a hit...", 1, docs.totalHits);
        assertEquals(missCount, filter.missCount);
        writer.deleteDocuments(new Term("id", "1"));
        reader = refreshReader(reader);
        searcher = new IndexSearcher(reader);
        missCount = filter.missCount;
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        assertEquals(missCount + 1, filter.missCount);
        assertEquals("[query + filter] Should *not* find a hit...", 0, docs.totalHits);
        docs = searcher.search(constantScore, 1);
        assertEquals("[just filter] Should *not* find a hit...", 0, docs.totalHits);
        filter = new CachingWrapperFilter(startFilter, CachingWrapperFilter.DeletesMode.DYNAMIC);
        writer.addDocument(doc);
        reader = refreshReader(reader);
        searcher = new IndexSearcher(reader);
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        assertEquals("[query + filter] Should find a hit...", 1, docs.totalHits);
        constantScore = new ConstantScoreQuery(filter);
        docs = searcher.search(constantScore, 1);
        assertEquals("[just filter] Should find a hit...", 1, docs.totalHits);
        writer.deleteDocuments(new Term("id", "1"));
        reader = refreshReader(reader);
        searcher = new IndexSearcher(reader);
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        assertEquals("[query + filter] Should *not* find a hit...", 0, docs.totalHits);
        missCount = filter.missCount;
        docs = searcher.search(constantScore, 1);
        assertEquals("[just filter] Should *not* find a hit...", 0, docs.totalHits);
        assertEquals(missCount, filter.missCount);
    }

    private static IndexReader refreshReader(IndexReader reader) throws IOException {
        IndexReader oldReader = reader;
        reader = reader.reopen();
        if (reader != oldReader) {
            oldReader.close();
        }
        return reader;
    }
}

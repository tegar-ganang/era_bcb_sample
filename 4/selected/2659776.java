package org.apache.lucene.search;

import java.io.IOException;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MockRAMDirectory;
import org.apache.lucene.util.LuceneTestCase;

public class TestCachingSpanFilter extends LuceneTestCase {

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
        final SpanFilter startFilter = new SpanQueryFilter(new SpanTermQuery(new Term("id", "1")));
        CachingSpanFilter filter = new CachingSpanFilter(startFilter, CachingWrapperFilter.DeletesMode.IGNORE);
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
        filter = new CachingSpanFilter(startFilter, CachingWrapperFilter.DeletesMode.RECACHE);
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
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        assertEquals("[query + filter] Should *not* find a hit...", 0, docs.totalHits);
        docs = searcher.search(constantScore, 1);
        assertEquals("[just filter] Should *not* find a hit...", 0, docs.totalHits);
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

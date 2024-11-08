package org.apache.lucene.store;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.util.Random;
import joelwilson.GAETestCase;
import joelwilson.lucene.CountingDirectory;
import joelwilson.lucene.StatsGatherer;
import junit.framework.Assert;
import org.apache.lucene.GAERAMIndexProvider;
import org.apache.lucene.IndexProvider;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.junit.Test;

public class GAERAMDirectoryTestCase extends GAETestCase {

    static String[] textWords = { "hello", "you", "my", "computer", "game", "test", "lucene", "apache", "bigtable", "datastore", "memcache", "video", "toy", "recipe", "blueberry", "blackberry", "book", "index" };

    Random rand = new Random();

    Directory dir = null;

    public void makeDirectory() throws IOException {
        dir = new CountingDirectory(new GAERAMDirectory("unit test case directory"));
    }

    @Test
    public void emptyTest() {
    }

    @Test
    public void noWritesIndexSearch() throws IOException {
        IndexProvider indexProvider = new GAERAMIndexProvider("test");
        IndexSearcher searcher = indexProvider.getIndexSearcher();
        Query query = new TermQuery(new Term("partnum", "Q36"));
        TopDocs rs = searcher.search(query, null, 10);
        assertEquals(0, rs.totalHits);
    }

    @Test
    public void SimpleOperation() throws IOException {
        makeDirectory();
        IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
        Document doc = new Document();
        doc.add(new Field("partnum", "Q36", Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("description", buildRandomString(100), Field.Store.NO, Field.Index.ANALYZED));
        writer.addDocument(doc);
        writer.close();
        IndexSearcher searcher = new IndexSearcher(dir);
        Query query = new TermQuery(new Term("partnum", "Q36"));
        TopDocs rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir);
        query = new TermQuery(new Term("partnum", "Q37"));
        rs = searcher.search(query, null, 10);
        assertEquals(0, rs.totalHits);
        printStats("Basic single write single read operations");
    }

    @Test
    public void bulkWriteOperation() throws IOException {
        makeDirectory();
        boolean overwrite = dir.listAll().length == 0;
        IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), overwrite, IndexWriter.MaxFieldLength.UNLIMITED);
        Document doc = new Document();
        Field id = new Field("id", "", Field.Store.YES, Field.Index.NOT_ANALYZED);
        doc.add(id);
        Field content = new Field("content", "", Field.Store.NO, Field.Index.ANALYZED);
        doc.add(content);
        for (int i = 0; i < 20; i++) {
            id.setValue(Integer.toString(i));
            content.setValue(buildRandomString(rand.nextInt(100)));
            writer.addDocument(doc);
        }
        writer.close();
        printStats("Bulk Write Pre-Read");
        IndexSearcher searcher = new IndexSearcher(dir);
        QueryParser qParser = new QueryParser(Version.LUCENE_30, "content", new StandardAnalyzer(Version.LUCENE_30));
        try {
            Query query = qParser.parse(buildRandomString(2));
            TopDocs rs = searcher.search(query, null, 100);
            System.out.println("Hits Found: " + rs.totalHits);
        } catch (ParseException parseException) {
            System.out.println("Parse Exception: " + parseException.getMessage());
        }
        searcher.close();
        dir.close();
        printStats("Bulk Write Post-Read");
        makeDirectory();
        try {
            searcher = new IndexSearcher(dir);
            Term term = new Term("id", "0");
            Query query = new TermQuery(term);
            TopDocs rs = searcher.search(query, null, 100);
            System.out.println("Hits Found: " + rs.totalHits);
            assertEquals("The first document was found using a term query against the id", 1, rs.totalHits);
            searcher.close();
            dir.close();
        } catch (IOException ioException) {
            System.out.println(ioException.getMessage());
            Assert.fail("Exception thrown while opening searcher on datastore read index.");
        } finally {
            printStats("Bulk Write Post-Search (Directory contents pulled from Datastore)");
        }
    }

    @Test
    public void bulkIndependentWriteOperations() throws IOException {
        makeDirectory();
        for (int i = 0; i < 20; i++) {
            boolean overwrite = dir.listAll().length == 0;
            IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_30), overwrite, IndexWriter.MaxFieldLength.UNLIMITED);
            writer.setRAMBufferSizeMB(1);
            Document doc = new Document();
            Field id = new Field("id", "", Field.Store.YES, Field.Index.NOT_ANALYZED);
            doc.add(id);
            Field content = new Field("content", "", Field.Store.NO, Field.Index.ANALYZED);
            doc.add(content);
            id.setValue(Integer.toString(i));
            content.setValue(buildRandomString(rand.nextInt(100)));
            writer.addDocument(doc);
            writer.close();
        }
        printStats("Bulk Independent Write Pre-Optimize");
        printDirectoryList();
        IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), false, IndexWriter.MaxFieldLength.UNLIMITED);
        writer.optimize();
        writer.close();
        printStats("Bulk Independent Write Pre-Read");
        IndexSearcher searcher = new IndexSearcher(dir);
        Query query = new TermQuery(new Term("content", buildRandomString(0)));
        TopDocs rs = searcher.search(query, null, 100);
        System.out.println("Hits Found: " + rs.totalHits);
        printStats("Bulk Independent Write Post-Read");
    }

    @Test
    public void testThreeInstancesOfDirectoryInteracting() throws IOException {
        makeDirectory();
        Directory dir2 = new GAERAMDirectory("unit test case directory");
        IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
        Document doc = new Document();
        doc.add(new Field("partnum", "Q36", Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("description", buildRandomString(100), Field.Store.NO, Field.Index.ANALYZED));
        writer.addDocument(doc);
        writer.close();
        ((GAERAMDirectory) dir2).pullLatest();
        IndexSearcher searcher = new IndexSearcher(dir2);
        Query query = new TermQuery(new Term("partnum", "Q36"));
        TopDocs rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        Directory dir3 = new GAERAMDirectory("unit test case directory");
        searcher = new IndexSearcher(dir3);
        query = new TermQuery(new Term("partnum", "Q36"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        writer = new IndexWriter(dir3, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
        doc = new Document();
        doc.add(new Field("partnum", "Q37", Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("description", buildRandomString(100), Field.Store.NO, Field.Index.ANALYZED));
        writer.addDocument(doc);
        writer.close();
        writer = new IndexWriter(dir2, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
        doc = new Document();
        doc.add(new Field("partnum", "Q38", Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field("description", buildRandomString(100), Field.Store.NO, Field.Index.ANALYZED));
        writer.addDocument(doc);
        writer.close();
        ((GAERAMDirectory) ((CountingDirectory) dir).getInnerDirectory()).pullLatest();
        ((GAERAMDirectory) dir3).pullLatest();
        searcher = new IndexSearcher(dir);
        query = new TermQuery(new Term("partnum", "Q36"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir2);
        query = new TermQuery(new Term("partnum", "Q36"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir3);
        query = new TermQuery(new Term("partnum", "Q36"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir);
        query = new TermQuery(new Term("partnum", "Q37"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir2);
        query = new TermQuery(new Term("partnum", "Q37"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir3);
        query = new TermQuery(new Term("partnum", "Q37"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir);
        query = new TermQuery(new Term("partnum", "Q38"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir2);
        query = new TermQuery(new Term("partnum", "Q38"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        searcher = new IndexSearcher(dir3);
        query = new TermQuery(new Term("partnum", "Q38"));
        rs = searcher.search(query, null, 10);
        assertEquals(1, rs.totalHits);
        printStats("Basic single write single read operations");
    }

    private String buildRandomString(int words) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= words; i++) {
            builder.append(textWords[rand.nextInt(textWords.length)]);
            if (i < words) builder.append(' ');
        }
        return builder.toString();
    }

    private void printStats(String pointName) {
        if (dir instanceof StatsGatherer) {
            System.out.println("----------------------------------");
            System.out.println(pointName);
            System.out.println(((StatsGatherer) dir).getStatistics());
            System.out.println("----------------------------------");
        } else {
            System.out.println("No stats available for " + pointName);
        }
    }

    private void printDirectoryList() throws IOException {
        System.out.println("----------------------------------");
        System.out.println("Directory File List");
        for (String fileList : dir.listAll()) System.out.println(fileList);
        System.out.println("----------------------------------");
    }
}

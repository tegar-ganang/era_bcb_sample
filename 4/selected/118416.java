package org.apache.lucene.search;

import org.apache.lucene.util.LuceneTestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import java.io.IOException;

/**
 *
 **/
public class TestBooleanPrefixQuery extends LuceneTestCase {

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestBooleanPrefixQuery.class);
    }

    public TestBooleanPrefixQuery(String name) {
        super(name);
    }

    private int getCount(IndexReader r, Query q) throws Exception {
        if (q instanceof BooleanQuery) {
            return ((BooleanQuery) q).getClauses().length;
        } else if (q instanceof ConstantScoreQuery) {
            DocIdSetIterator iter = ((ConstantScoreQuery) q).getFilter().getDocIdSet(r).iterator();
            int count = 0;
            while (iter.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                count++;
            }
            return count;
        } else {
            throw new RuntimeException("unepxected query " + q);
        }
    }

    public void testMethod() throws Exception {
        RAMDirectory directory = new RAMDirectory();
        String[] categories = new String[] { "food", "foodanddrink", "foodanddrinkandgoodtimes", "food and drink" };
        Query rw1 = null;
        Query rw2 = null;
        IndexReader reader = null;
        try {
            IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);
            for (int i = 0; i < categories.length; i++) {
                Document doc = new Document();
                doc.add(new Field("category", categories[i], Field.Store.YES, Field.Index.NOT_ANALYZED));
                writer.addDocument(doc);
            }
            writer.close();
            reader = IndexReader.open(directory, true);
            PrefixQuery query = new PrefixQuery(new Term("category", "foo"));
            rw1 = query.rewrite(reader);
            BooleanQuery bq = new BooleanQuery();
            bq.add(query, BooleanClause.Occur.MUST);
            rw2 = bq.rewrite(reader);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        assertEquals("Number of Clauses Mismatch", getCount(reader, rw1), getCount(reader, rw2));
    }
}

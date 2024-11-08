package org.apache.solr.search.function;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.solr.search.SolrIndexReader;
import java.io.IOException;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Returns a score for each document based on a ValueSource,
 * often some function of the value of a field.
 *
 * <b>Note: This API is experimental and may change in non backward-compatible ways in the future</b>
 *
 * @version $Id: FunctionQuery.java 816202 2009-09-17 14:08:13Z yonik $
 */
public class FunctionQuery extends Query {

    ValueSource func;

    /**
   * @param func defines the function to be used for scoring
   */
    public FunctionQuery(ValueSource func) {
        this.func = func;
    }

    /** @return The associated ValueSource */
    public ValueSource getValueSource() {
        return func;
    }

    public Query rewrite(IndexReader reader) throws IOException {
        return this;
    }

    public void extractTerms(Set terms) {
    }

    protected class FunctionWeight extends Weight {

        protected Searcher searcher;

        protected float queryNorm;

        protected float queryWeight;

        protected Map context;

        public FunctionWeight(Searcher searcher) throws IOException {
            this.searcher = searcher;
            this.context = func.newContext();
            func.createWeight(context, searcher);
        }

        public Query getQuery() {
            return FunctionQuery.this;
        }

        public float getValue() {
            return queryWeight;
        }

        @Override
        public float sumOfSquaredWeights() throws IOException {
            queryWeight = getBoost();
            return queryWeight * queryWeight;
        }

        @Override
        public void normalize(float norm) {
            this.queryNorm = norm;
            queryWeight *= this.queryNorm;
        }

        @Override
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
            return new AllScorer(getSimilarity(searcher), reader, this);
        }

        @Override
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            SolrIndexReader topReader = (SolrIndexReader) reader;
            SolrIndexReader[] subReaders = topReader.getLeafReaders();
            int[] offsets = topReader.getLeafOffsets();
            int readerPos = SolrIndexReader.readerIndex(doc, offsets);
            int readerBase = offsets[readerPos];
            return scorer(subReaders[readerPos], true, true).explain(doc - readerBase);
        }
    }

    protected class AllScorer extends Scorer {

        final IndexReader reader;

        final FunctionWeight weight;

        final int maxDoc;

        final float qWeight;

        int doc = -1;

        final DocValues vals;

        final boolean hasDeletions;

        public AllScorer(Similarity similarity, IndexReader reader, FunctionWeight w) throws IOException {
            super(similarity);
            this.weight = w;
            this.qWeight = w.getValue();
            this.reader = reader;
            this.maxDoc = reader.maxDoc();
            this.hasDeletions = reader.hasDeletions();
            vals = func.getValues(weight.context, reader);
        }

        @Override
        public int docID() {
            return doc;
        }

        @Override
        public int nextDoc() throws IOException {
            for (; ; ) {
                ++doc;
                if (doc >= maxDoc) {
                    return doc = NO_MORE_DOCS;
                }
                if (hasDeletions && reader.isDeleted(doc)) continue;
                return doc;
            }
        }

        @Override
        public int advance(int target) throws IOException {
            doc = target - 1;
            return nextDoc();
        }

        public boolean next() throws IOException {
            for (; ; ) {
                ++doc;
                if (doc >= maxDoc) {
                    return false;
                }
                if (hasDeletions && reader.isDeleted(doc)) continue;
                return true;
            }
        }

        public int doc() {
            return doc;
        }

        public float score() throws IOException {
            float score = qWeight * vals.floatVal(doc);
            return score > Float.NEGATIVE_INFINITY ? score : -Float.MAX_VALUE;
        }

        public boolean skipTo(int target) throws IOException {
            doc = target - 1;
            return next();
        }

        public Explanation explain(int doc) throws IOException {
            float sc = qWeight * vals.floatVal(doc);
            Explanation result = new ComplexExplanation(true, sc, "FunctionQuery(" + func + "), product of:");
            result.addDetail(vals.explain(doc));
            result.addDetail(new Explanation(getBoost(), "boost"));
            result.addDetail(new Explanation(weight.queryNorm, "queryNorm"));
            return result;
        }
    }

    public Weight createWeight(Searcher searcher) throws IOException {
        return new FunctionQuery.FunctionWeight(searcher);
    }

    /** Prints a user-readable version of this query. */
    public String toString(String field) {
        float boost = getBoost();
        return (boost != 1.0 ? "(" : "") + func.toString() + (boost == 1.0 ? "" : ")^" + boost);
    }

    /** Returns true if <code>o</code> is equal to this. */
    public boolean equals(Object o) {
        if (FunctionQuery.class != o.getClass()) return false;
        FunctionQuery other = (FunctionQuery) o;
        return this.getBoost() == other.getBoost() && this.func.equals(other.func);
    }

    /** Returns a hash code value for this object. */
    public int hashCode() {
        return func.hashCode() * 31 + Float.floatToIntBits(getBoost());
    }
}

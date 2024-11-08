package gate.creole.annic.apache.lucene.search;

import gate.creole.annic.apache.lucene.index.IndexReader;
import java.io.IOException;
import java.util.BitSet;

/**
 * A query that applies a filter to the results of another query.
 *
 * <p>Note: the bits are retrieved from the filter each time this
 * query is used in a search - use a CachingWrapperFilter to avoid
 * regenerating the bits every time.
 *
 * <p>Created: Apr 20, 2004 8:58:29 AM
 *
 * @author  Tim Jones
 * @since   1.4
 * @version $Id: FilteredQuery.java 3362 2006-10-27 15:37:08Z niraj $
 * @see     CachingWrapperFilter
 */
public class FilteredQuery extends Query {

    Query query;

    Filter filter;

    /**
   * Constructs a new query which applies a filter to the results of the original query.
   * Filter.bits() will be called every time this query is used in a search.
   * @param query  Query to be filtered, cannot be <code>null</code>.
   * @param filter Filter to apply to query results, cannot be <code>null</code>.
   */
    public FilteredQuery(Query query, Filter filter) {
        this.query = query;
        this.filter = filter;
    }

    /**
   * Returns a Weight that applies the filter to the enclosed query's Weight.
   * This is accomplished by overriding the Scorer returned by the Weight.
   */
    protected Weight createWeight(final Searcher searcher) {
        final Weight weight = query.createWeight(searcher);
        return new Weight() {

            public float getValue() {
                return weight.getValue();
            }

            public float sumOfSquaredWeights() throws IOException {
                return weight.sumOfSquaredWeights();
            }

            public void normalize(float v) {
                weight.normalize(v);
            }

            public Explanation explain(IndexReader ir, int i) throws IOException {
                return weight.explain(ir, i);
            }

            public Query getQuery() {
                return FilteredQuery.this;
            }

            public Scorer scorer(IndexReader reader, IndexSearcher searcher) throws IOException {
                return scorer(reader);
            }

            public Scorer scorer(IndexReader indexReader) throws IOException {
                final Scorer scorer = weight.scorer(indexReader);
                final BitSet bitset = filter.bits(indexReader);
                return new Scorer(query.getSimilarity(searcher)) {

                    public boolean next() throws IOException {
                        return scorer.next();
                    }

                    public int doc() {
                        return scorer.doc();
                    }

                    public boolean skipTo(int i) throws IOException {
                        return scorer.skipTo(i);
                    }

                    public float score(IndexSearcher searcher) throws IOException {
                        return score();
                    }

                    public float score() throws IOException {
                        return (bitset.get(scorer.doc())) ? scorer.score() : 0.0f;
                    }

                    public Explanation explain(int i) throws IOException {
                        Explanation exp = scorer.explain(i);
                        if (bitset.get(i)) exp.setDescription("allowed by filter: " + exp.getDescription()); else exp.setDescription("removed by filter: " + exp.getDescription());
                        return exp;
                    }
                };
            }
        };
    }

    /** Rewrites the wrapped query. */
    public Query rewrite(IndexReader reader) throws IOException {
        Query rewritten = query.rewrite(reader);
        if (rewritten != query) {
            FilteredQuery clone = (FilteredQuery) this.clone();
            clone.query = rewritten;
            return clone;
        } else {
            return this;
        }
    }

    public Query getQuery() {
        return query;
    }

    /** Prints a user-readable version of this query. */
    public String toString(String s) {
        return "filtered(" + query.toString(s) + ")->" + filter;
    }

    /** Returns true iff <code>o</code> is equal to this. */
    public boolean equals(Object o) {
        if (o instanceof FilteredQuery) {
            FilteredQuery fq = (FilteredQuery) o;
            return (query.equals(fq.query) && filter.equals(fq.filter));
        }
        return false;
    }

    /** Returns a hash code value for this object. */
    public int hashCode() {
        return query.hashCode() ^ filter.hashCode();
    }
}

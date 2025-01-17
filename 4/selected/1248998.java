package gate.creole.annic.apache.lucene.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import gate.creole.annic.apache.lucene.index.IndexReader;

/**
 * The abstract base class for queries.
 */
public abstract class Query implements java.io.Serializable, Cloneable {

    private float boost = 1.0f;

    /** Sets the boost for this query clause to <code>b</code>.  Documents
   * matching this clause will (in addition to the normal weightings) have
   * their score multiplied by <code>b</code>.
   */
    public void setBoost(float b) {
        boost = b;
    }

    /** Gets the boost for this clause.  Documents matching
   * this clause will (in addition to the normal weightings) have their score
   * multiplied by <code>b</code>.   The boost is 1.0 by default.
   */
    public float getBoost() {
        return boost;
    }

    /**
   * Prints a query to a string, with <code>field</code> as the default field
   * for terms.
   */
    public abstract String toString(String field);

    /** Prints a query to a string. */
    public String toString() {
        return toString("");
    }

    /** Expert: Constructs an appropriate Weight implementation for this query.
   *
   * <p>Only implemented by primitive queries, which re-write to themselves.
   */
    protected Weight createWeight(Searcher searcher) {
        throw new UnsupportedOperationException();
    }

    /** Expert: Constructs an initializes a Weight for a top-level query. */
    public Weight weight(Searcher searcher) throws IOException {
        Query query = searcher.rewrite(this);
        Weight weight = query.createWeight(searcher);
        float sum = weight.sumOfSquaredWeights();
        float norm = getSimilarity(searcher).queryNorm(sum);
        weight.normalize(norm);
        return weight;
    }

    /** Expert: called to re-write queries into primitive queries. */
    public Query rewrite(IndexReader reader) throws IOException {
        return this;
    }

    /** Expert: called when re-writing queries under MultiSearcher.
   *
   * <p>Only implemented by derived queries, with no
   * {@link #createWeight(Searcher)} implementatation.
   */
    public Query combine(Query[] queries) {
        throw new UnsupportedOperationException();
    }

    /** Expert: merges the clauses of a set of BooleanQuery's into a single
   * BooleanQuery.
   *
   *<p>A utility for use by {@link #combine(Query[])} implementations.
   */
    public static Query mergeBooleanQueries(Query[] queries) {
        HashSet allClauses = new HashSet();
        for (int i = 0; i < queries.length; i++) {
            BooleanClause[] clauses = ((BooleanQuery) queries[i]).getClauses();
            for (int j = 0; j < clauses.length; j++) {
                allClauses.add(clauses[j]);
            }
        }
        BooleanQuery result = new BooleanQuery();
        Iterator i = allClauses.iterator();
        while (i.hasNext()) {
            result.add((BooleanClause) i.next());
        }
        return result;
    }

    /** Expert: Returns the Similarity implementation to be used for this query.
   * Subclasses may override this method to specify their own Similarity
   * implementation, perhaps one that delegates through that of the Searcher.
   * By default the Searcher's Similarity implementation is returned.*/
    public Similarity getSimilarity(Searcher searcher) {
        return searcher.getSimilarity();
    }

    /** Returns a clone of this query. */
    public Object clone() {
        try {
            return (Query) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone not supported: " + e.getMessage());
        }
    }
}

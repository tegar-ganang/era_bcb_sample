package gate.creole.annic.apache.lucene.search;

import java.io.IOException;
import gate.creole.annic.apache.lucene.index.Term;
import gate.creole.annic.apache.lucene.index.TermEnum;
import gate.creole.annic.apache.lucene.index.IndexReader;

/**
 * A Query that matches documents within an exclusive range.
 *
 * @version $Id: RangeQuery.java 529 2004-10-05 11:55:26Z niraj $
 */
public class RangeQuery extends Query {

    private Term lowerTerm;

    private Term upperTerm;

    private boolean inclusive;

    /** Constructs a query selecting all terms greater than
     * <code>lowerTerm</code> but less than <code>upperTerm</code>.
     * There must be at least one term and either term may be null,
     * in which case there is no bound on that side, but if there are
     * two terms, both terms <b>must</b> be for the same field.
     */
    public RangeQuery(Term lowerTerm, Term upperTerm, boolean inclusive) {
        if (lowerTerm == null && upperTerm == null) {
            throw new IllegalArgumentException("At least one term must be non-null");
        }
        if (lowerTerm != null && upperTerm != null && lowerTerm.field() != upperTerm.field()) {
            throw new IllegalArgumentException("Both terms must be for the same field");
        }
        if (lowerTerm != null) {
            this.lowerTerm = lowerTerm;
        } else {
            this.lowerTerm = new Term(upperTerm.field(), "");
        }
        this.upperTerm = upperTerm;
        this.inclusive = inclusive;
    }

    /**
     * FIXME: Describe <code>rewrite</code> method here.
     *
     * @param reader an <code>IndexReader</code> value
     * @return a <code>Query</code> value
     * @exception IOException if an error occurs
     */
    public Query rewrite(IndexReader reader) throws IOException {
        BooleanQuery query = new BooleanQuery();
        TermEnum enumerator = reader.terms(lowerTerm);
        try {
            boolean checkLower = false;
            if (!inclusive) checkLower = true;
            String testField = getField();
            do {
                Term term = enumerator.term();
                if (term != null && term.field() == testField) {
                    if (!checkLower || term.text().compareTo(lowerTerm.text()) > 0) {
                        checkLower = false;
                        if (upperTerm != null) {
                            int compare = upperTerm.text().compareTo(term.text());
                            if ((compare < 0) || (!inclusive && compare == 0)) break;
                        }
                        TermQuery tq = new TermQuery(term);
                        tq.setBoost(getBoost());
                        query.add(tq, false, false);
                    }
                } else {
                    break;
                }
            } while (enumerator.next());
        } finally {
            enumerator.close();
        }
        return query;
    }

    public Query combine(Query[] queries) {
        return Query.mergeBooleanQueries(queries);
    }

    /** Returns the field name for this query */
    public String getField() {
        return (lowerTerm != null ? lowerTerm.field() : upperTerm.field());
    }

    /** Returns the lower term of this range query */
    public Term getLowerTerm() {
        return lowerTerm;
    }

    /** Returns the upper term of this range query */
    public Term getUpperTerm() {
        return upperTerm;
    }

    /** Returns <code>true</code> if the range query is inclusive */
    public boolean isInclusive() {
        return inclusive;
    }

    /** Prints a user-readable version of this query. */
    public String toString(String field) {
        StringBuffer buffer = new StringBuffer();
        if (!getField().equals(field)) {
            buffer.append(getField());
            buffer.append(":");
        }
        buffer.append(inclusive ? "[" : "{");
        buffer.append(lowerTerm != null ? lowerTerm.text() : "null");
        buffer.append(" TO ");
        buffer.append(upperTerm != null ? upperTerm.text() : "null");
        buffer.append(inclusive ? "]" : "}");
        if (getBoost() != 1.0f) {
            buffer.append("^");
            buffer.append(Float.toString(getBoost()));
        }
        return buffer.toString();
    }
}

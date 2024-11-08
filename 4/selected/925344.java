package org.apache.lucene.queryParser.surround.query;

import java.util.ArrayList;
import java.io.IOException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause;

public abstract class SimpleTerm extends SrndQuery implements DistanceSubQuery, Comparable {

    public SimpleTerm(boolean q) {
        quoted = q;
    }

    private boolean quoted;

    boolean isQuoted() {
        return quoted;
    }

    public String getQuote() {
        return "\"";
    }

    public String getFieldOperator() {
        return "/";
    }

    public abstract String toStringUnquoted();

    public int compareTo(Object o) {
        SimpleTerm ost = (SimpleTerm) o;
        return this.toStringUnquoted().compareTo(ost.toStringUnquoted());
    }

    protected void suffixToString(StringBuffer r) {
        ;
    }

    public String toString() {
        StringBuffer r = new StringBuffer();
        if (isQuoted()) {
            r.append(getQuote());
        }
        r.append(toStringUnquoted());
        if (isQuoted()) {
            r.append(getQuote());
        }
        suffixToString(r);
        weightToString(r);
        return r.toString();
    }

    public abstract void visitMatchingTerms(IndexReader reader, String fieldName, MatchingTermVisitor mtv) throws IOException;

    public interface MatchingTermVisitor {

        void visitMatchingTerm(Term t) throws IOException;
    }

    public String distanceSubQueryNotAllowed() {
        return null;
    }

    public Query makeLuceneQueryFieldNoBoost(final String fieldName, final BasicQueryFactory qf) {
        return new Query() {

            public String toString(String fn) {
                return getClass().toString() + " " + fieldName + " (" + fn + "?)";
            }

            public Query rewrite(IndexReader reader) throws IOException {
                final ArrayList luceneSubQueries = new ArrayList();
                visitMatchingTerms(reader, fieldName, new MatchingTermVisitor() {

                    public void visitMatchingTerm(Term term) throws IOException {
                        luceneSubQueries.add(qf.newTermQuery(term));
                    }
                });
                return (luceneSubQueries.size() == 0) ? SrndQuery.theEmptyLcnQuery : (luceneSubQueries.size() == 1) ? (Query) luceneSubQueries.get(0) : SrndBooleanQuery.makeBooleanQuery(luceneSubQueries, false, false);
            }
        };
    }

    public void addSpanQueries(final SpanNearClauseFactory sncf) throws IOException {
        visitMatchingTerms(sncf.getIndexReader(), sncf.getFieldName(), new MatchingTermVisitor() {

            public void visitMatchingTerm(Term term) throws IOException {
                sncf.addTermWeighted(term, getWeight());
            }
        });
    }
}

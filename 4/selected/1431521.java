package org.apache.lucene.queryParser.surround.query;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;

public class DistanceQuery extends ComposedQuery implements DistanceSubQuery {

    public DistanceQuery(List queries, boolean infix, int opDistance, String opName, boolean ordered) {
        super(queries, infix, opName);
        this.opDistance = opDistance;
        this.ordered = ordered;
    }

    private int opDistance;

    public int getOpDistance() {
        return opDistance;
    }

    private boolean ordered;

    public boolean subQueriesOrdered() {
        return ordered;
    }

    public String distanceSubQueryNotAllowed() {
        Iterator sqi = getSubQueriesIterator();
        while (sqi.hasNext()) {
            Object leq = sqi.next();
            if (leq instanceof DistanceSubQuery) {
                DistanceSubQuery dsq = (DistanceSubQuery) leq;
                String m = dsq.distanceSubQueryNotAllowed();
                if (m != null) {
                    return m;
                }
            } else {
                return "Operator " + getOperatorName() + " does not allow subquery " + leq.toString();
            }
        }
        return null;
    }

    public void addSpanQueries(SpanNearClauseFactory sncf) throws IOException {
        Query snq = getSpanNearQuery(sncf.getIndexReader(), sncf.getFieldName(), getWeight(), sncf.getBasicQueryFactory());
        sncf.addSpanNearQuery(snq);
    }

    public Query makeLuceneQueryFieldNoBoost(final String fieldName, final BasicQueryFactory qf) {
        return new Query() {

            public String toString(String fn) {
                return getClass().toString() + " " + fieldName + " (" + fn + "?)";
            }

            public Query rewrite(IndexReader reader) throws IOException {
                return getSpanNearQuery(reader, fieldName, getBoost(), qf);
            }
        };
    }

    public Query getSpanNearQuery(IndexReader reader, String fieldName, float boost, BasicQueryFactory qf) throws IOException {
        SpanQuery[] spanNearClauses = new SpanQuery[getNrSubQueries()];
        Iterator sqi = getSubQueriesIterator();
        int qi = 0;
        while (sqi.hasNext()) {
            SpanNearClauseFactory sncf = new SpanNearClauseFactory(reader, fieldName, qf);
            ((DistanceSubQuery) sqi.next()).addSpanQueries(sncf);
            if (sncf.size() == 0) {
                while (sqi.hasNext()) {
                    ((DistanceSubQuery) sqi.next()).addSpanQueries(sncf);
                    sncf.clear();
                }
                return SrndQuery.theEmptyLcnQuery;
            }
            spanNearClauses[qi] = sncf.makeSpanNearClause();
            qi++;
        }
        SpanNearQuery r = new SpanNearQuery(spanNearClauses, getOpDistance() - 1, subQueriesOrdered());
        r.setBoost(boost);
        return r;
    }
}

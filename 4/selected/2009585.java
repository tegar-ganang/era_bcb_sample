package gate.creole.annic.apache.lucene.search;

import java.io.IOException;
import gate.creole.annic.apache.lucene.index.Term;
import gate.creole.annic.apache.lucene.index.TermEnum;
import gate.creole.annic.apache.lucene.index.IndexReader;

/** A Query that matches documents containing terms with a specified prefix. */
public class PrefixQuery extends Query {

    private Term prefix;

    /** Constructs a query for terms starting with <code>prefix</code>. */
    public PrefixQuery(Term prefix) {
        this.prefix = prefix;
    }

    /** Returns the prefix of this query. */
    public Term getPrefix() {
        return prefix;
    }

    public Query rewrite(IndexReader reader) throws IOException {
        BooleanQuery query = new BooleanQuery();
        TermEnum enumerator = reader.terms(prefix);
        try {
            String prefixText = prefix.text();
            String prefixField = prefix.field();
            do {
                Term term = enumerator.term();
                if (term != null && term.text().startsWith(prefixText) && term.field() == prefixField) {
                    TermQuery tq = new TermQuery(term);
                    tq.setBoost(getBoost());
                    query.add(tq, false, false);
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

    /** Prints a user-readable version of this query. */
    public String toString(String field) {
        StringBuffer buffer = new StringBuffer();
        if (!prefix.field().equals(field)) {
            buffer.append(prefix.field());
            buffer.append(":");
        }
        buffer.append(prefix.text());
        buffer.append('*');
        if (getBoost() != 1.0f) {
            buffer.append("^");
            buffer.append(Float.toString(getBoost()));
        }
        return buffer.toString();
    }
}

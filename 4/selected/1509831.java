package green.search.query;

import java.io.IOException;
import java.util.Set;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

public class SVDQuery extends Query {

    Query query = null;

    public SVDQuery(Query query) {
        System.out.println("## SVDQuery = " + query.getClass());
        System.out.println("## SVDQuery = " + query);
        this.query = query;
    }

    @Override
    public Weight weight(Searcher searcher) throws IOException {
        Weight weight = new SVDWeight(searcher, query);
        System.out.println("## SVDQuery:weight = " + weight.getClass());
        return weight;
    }

    @Override
    public String toString(String arg0) {
        System.out.println("## SVDQuery:toString = " + arg0);
        return query.toString();
    }

    public Object clone() {
        return query.clone();
    }

    public Query combine(Query[] arg0) {
        return query.combine(arg0);
    }

    public boolean equals(Object obj) {
        return query.equals(obj);
    }

    public void extractTerms(Set terms) {
        query.extractTerms(terms);
    }

    public float getBoost() {
        return query.getBoost();
    }

    public Similarity getSimilarity(Searcher searcher) {
        return query.getSimilarity(searcher);
    }

    public int hashCode() {
        return query.hashCode();
    }

    public Query rewrite(IndexReader reader) throws IOException {
        return query.rewrite(reader);
    }

    public void setBoost(float b) {
        query.setBoost(b);
    }

    public String toString() {
        return query.toString();
    }
}

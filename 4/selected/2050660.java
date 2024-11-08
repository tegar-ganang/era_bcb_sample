package org.apache.lucene.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.FuzzyTermEnum;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.util.PriorityQueue;

/**
 * Fuzzifies ALL terms provided as strings and then picks the best n differentiating terms.
 * In effect this mixes the behaviour of FuzzyQuery and MoreLikeThis but with special consideration
 * of fuzzy scoring factors.
 * This generally produces good results for queries where users may provide details in a number of 
 * fields and have no knowledge of boolean query syntax and also want a degree of fuzzy matching and
 * a fast query.
 * 
 * For each source term the fuzzy variants are held in a BooleanQuery with no coord factor (because
 * we are not looking for matches on multiple variants in any one doc). Additionally, a specialized
 * TermQuery is used for variants and does not use that variant term's IDF because this would favour rarer 
 * terms eg misspellings. Instead, all variants use the same IDF ranking (the one for the source query 
 * term) and this is factored into the variant's boost. If the source query term does not exist in the
 * index the average IDF of the variants is used. 
 * @author maharwood
 */
public class FuzzyLikeThisQuery extends Query {

    static Similarity sim = new DefaultSimilarity();

    Query rewrittenQuery = null;

    ArrayList fieldVals = new ArrayList();

    Analyzer analyzer;

    ScoreTermQueue q;

    int MAX_VARIANTS_PER_TERM = 50;

    boolean ignoreTF = false;

    /**
     * 
     * @param maxNumTerms The total number of terms clauses that will appear once rewritten as a BooleanQuery
     * @param analyzer
     */
    public FuzzyLikeThisQuery(int maxNumTerms, Analyzer analyzer) {
        q = new ScoreTermQueue(maxNumTerms);
        this.analyzer = analyzer;
    }

    class FieldVals {

        String queryString;

        String fieldName;

        float minSimilarity;

        int prefixLength;

        public FieldVals(String name, float similarity, int length, String queryString) {
            fieldName = name;
            minSimilarity = similarity;
            prefixLength = length;
            this.queryString = queryString;
        }
    }

    /**
     * Adds user input for "fuzzification" 
     * @param queryString The string which will be parsed by the analyzer and for which fuzzy variants will be parsed
     * @param fieldName
     * @param minSimilarity The minimum similarity of the term variants (see FuzzyTermEnum)
     * @param prefixLength Length of required common prefix on variant terms (see FuzzyTermEnum)
     */
    public void addTerms(String queryString, String fieldName, float minSimilarity, int prefixLength) {
        fieldVals.add(new FieldVals(fieldName, minSimilarity, prefixLength, queryString));
    }

    private void addTerms(IndexReader reader, FieldVals f) throws IOException {
        if (f.queryString == null) return;
        TokenStream ts = analyzer.tokenStream(f.fieldName, new StringReader(f.queryString));
        int corpusNumDocs = reader.numDocs();
        Term internSavingTemplateTerm = new Term(f.fieldName, null);
        HashSet processedTerms = new HashSet();
        for (Token nextToken = ts.next(); nextToken != null; nextToken = ts.next()) {
            String term = nextToken.termText();
            if (!processedTerms.contains(term)) {
                processedTerms.add(term);
                ScoreTermQueue variantsQ = new ScoreTermQueue(MAX_VARIANTS_PER_TERM);
                float minScore = 0;
                Term startTerm = new Term(internSavingTemplateTerm.field(), term);
                FuzzyTermEnum fe = new FuzzyTermEnum(reader, startTerm, f.minSimilarity, f.prefixLength);
                TermEnum origEnum = reader.terms(startTerm);
                int df = 0;
                if (startTerm.equals(origEnum.term())) {
                    df = origEnum.docFreq();
                }
                int numVariants = 0;
                int totalVariantDocFreqs = 0;
                do {
                    Term possibleMatch = fe.term();
                    if (possibleMatch != null) {
                        numVariants++;
                        totalVariantDocFreqs += fe.docFreq();
                        float score = fe.difference();
                        if (variantsQ.size() < MAX_VARIANTS_PER_TERM || score > minScore) {
                            ScoreTerm st = new ScoreTerm(possibleMatch, score, startTerm);
                            variantsQ.insert(st);
                            minScore = ((ScoreTerm) variantsQ.top()).score;
                        }
                    }
                } while (fe.next());
                if (numVariants == 0) {
                    break;
                }
                int avgDf = totalVariantDocFreqs / numVariants;
                if (df == 0) {
                    df = avgDf;
                }
                int size = variantsQ.size();
                for (int i = 0; i < size; i++) {
                    ScoreTerm st = (ScoreTerm) variantsQ.pop();
                    st.score = (st.score * st.score) * sim.idf(df, corpusNumDocs);
                    q.insert(st);
                }
            }
        }
    }

    public Query rewrite(IndexReader reader) throws IOException {
        if (rewrittenQuery != null) {
            return rewrittenQuery;
        }
        for (Iterator iter = fieldVals.iterator(); iter.hasNext(); ) {
            FieldVals f = (FieldVals) iter.next();
            addTerms(reader, f);
        }
        fieldVals.clear();
        BooleanQuery bq = new BooleanQuery();
        HashMap variantQueries = new HashMap();
        int size = q.size();
        for (int i = 0; i < size; i++) {
            ScoreTerm st = (ScoreTerm) q.pop();
            ArrayList l = (ArrayList) variantQueries.get(st.fuzziedSourceTerm);
            if (l == null) {
                l = new ArrayList();
                variantQueries.put(st.fuzziedSourceTerm, l);
            }
            l.add(st);
        }
        for (Iterator iter = variantQueries.values().iterator(); iter.hasNext(); ) {
            ArrayList variants = (ArrayList) iter.next();
            if (variants.size() == 1) {
                ScoreTerm st = (ScoreTerm) variants.get(0);
                TermQuery tq = new FuzzyTermQuery(st.term, ignoreTF);
                tq.setBoost(st.score);
                bq.add(tq, false, false);
            } else {
                BooleanQuery termVariants = new BooleanQuery();
                for (Iterator iterator2 = variants.iterator(); iterator2.hasNext(); ) {
                    ScoreTerm st = (ScoreTerm) iterator2.next();
                    TermQuery tq = new FuzzyTermQuery(st.term, ignoreTF);
                    tq.setBoost(st.score);
                    termVariants.add(tq, false, false);
                }
                bq.add(termVariants, false, false);
            }
        }
        bq.setBoost(getBoost());
        this.rewrittenQuery = bq;
        return bq;
    }

    private static class ScoreTerm {

        public Term term;

        public float score;

        Term fuzziedSourceTerm;

        public ScoreTerm(Term term, float score, Term fuzziedSourceTerm) {
            this.term = term;
            this.score = score;
            this.fuzziedSourceTerm = fuzziedSourceTerm;
        }
    }

    private static class ScoreTermQueue extends PriorityQueue {

        public ScoreTermQueue(int size) {
            initialize(size);
        }

        protected boolean lessThan(Object a, Object b) {
            ScoreTerm termA = (ScoreTerm) a;
            ScoreTerm termB = (ScoreTerm) b;
            if (termA.score == termB.score) return termA.term.compareTo(termB.term) > 0; else return termA.score < termB.score;
        }
    }

    private static class FuzzyTermQuery extends TermQuery {

        boolean ignoreTF;

        public FuzzyTermQuery(Term t, boolean ignoreTF) {
            super(t);
            this.ignoreTF = ignoreTF;
        }

        public Similarity getSimilarity(Searcher searcher) {
            Similarity result = super.getSimilarity(searcher);
            result = new SimilarityDelegator(result) {

                public float tf(float freq) {
                    if (ignoreTF) {
                        return 1;
                    }
                    return super.tf(freq);
                }

                public float idf(int docFreq, int numDocs) {
                    return 1;
                }
            };
            return result;
        }
    }

    public String toString(String field) {
        return null;
    }

    public boolean isIgnoreTF() {
        return ignoreTF;
    }

    public void setIgnoreTF(boolean ignoreTF) {
        this.ignoreTF = ignoreTF;
    }
}

package uk.ac.wlv.clg.nlp.entityannotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.wlv.clg.nlp.misc.AnnotationPosition;
import uk.ac.wlv.clg.nlp.misc.Annotator;

/**
 * A hybrid {@link Annotator} which combines character based token similarity metrics with 
 * 			bag-of-words similarity between text spans, represented as lists of words. 
 * 
 * @author iustin
 * @version SVN $Rev$ by $Author$
 */
public class HybridEntityAnnotator implements Annotator {

    /** maximum number of candidate entities to be considered per sentence */
    int maxCandidatesConsidered = 100;

    /** 
	 * Minimum character level similarity (Levenstein) between two words 
	 * when expanding the query using lucene's fuzzy operator (e.g. {@code "term"~.85})
	 * Valid values must be within the interval {@code (0.0 , 1.0)} 
	 */
    double minSimilarity = 0.85;

    /** threshold value for the token-level similarity between two spans of text */
    double minHybridSimilarity = 0.8;

    double confidenceThreshold = 0.1;

    /** String similarity measure used when computing the hybrid similarity */
    AbstractStringMetric characterBasedSimilarity = new JaroWinkler();

    /** Access to a lucene reader */
    IndexReader reader = null;

    /** whether or not to ignore stop-words when computing the hybrid similarity score*/
    boolean filterStopwords = true;

    /** the type of entities annotated by this {link Annotator} @see AnnotationPosition */
    String entityType = AnnotationPosition.OTHER;

    /** name of the field in the index containing an alias of an entity */
    String searchField = "text";

    /** name of the field in the index containing the name (id) of an entity */
    String nameField = "name";

    Searcher searcher;

    Analyzer analyzer;

    QueryParser parser;

    /**
	 * Creates an annotator using an index and a similarity metric
	 * @param reader
	 * @param characterSimilarity
	 */
    public HybridEntityAnnotator(IndexReader reader, AbstractStringMetric characterSimilarity) {
        characterBasedSimilarity = characterSimilarity;
        this.reader = reader;
        init();
    }

    /**
	 *  Create an annotator which uses a character-level string edit distance between words
	 *  combined with an word level edit distance.
	 *    
	 * @param reader the underlying Lucene index that stores the known entities 
	 * 					that are to be fuzzily recognized 
	 * @param characterSimilarity the character-level string similarity measure 
	 * @param filterStopWords whether to ignore common stop-words in English 
	 * 					when calculating the similarity; default (@code true}
	 * @param entityType the type of the entity used in the prototype
	 * 
	 * @see AnnotationPosition
	 */
    public HybridEntityAnnotator(IndexReader reader, AbstractStringMetric characterSimilarity, boolean filterStopWords, String entityType) {
        characterBasedSimilarity = characterSimilarity;
        this.reader = reader;
        this.filterStopwords = filterStopWords;
        this.entityType = entityType;
        init();
    }

    /**
	 *  Create an annotator which uses the JaroWinkler character-level similarity between words
	 *  combined with an word level edit distance.
	 *    
	 * @param reader the underlying Lucene index that stores the known entities 
	 * 					that are to be fuzzily recognized 
	 * @param filterStopWords whether to ignore common stop-words in English 
	 * 					when calculating the similarity; default (@code true}
	 * @param entityType the type of the entity used in the prototype
	 * 
	 * @see AnnotationPosition
	 */
    public HybridEntityAnnotator(IndexReader reader, boolean filterStopWords, String entityType) {
        this.reader = reader;
        this.filterStopwords = filterStopWords;
        this.entityType = entityType;
        init();
    }

    /**
	 * Create an annotator which uses the default underlying string similarity measure (JaroWinkler) and 
	 * filters out common English stopwords. 
	 * @param reader
	 */
    public HybridEntityAnnotator(IndexReader reader) {
        this(reader, new JaroWinkler());
    }

    /**
	 * initialise Lucene search objects
	 */
    protected void init() {
        searcher = new IndexSearcher(reader);
        analyzer = new StandardAnalyzer(new String[] {});
        parser = new QueryParser(searchField, analyzer);
    }

    @Override
    public TreeSet<AnnotationPosition> annotateInstances(String sentence) {
        TreeSet<AnnotationPosition> annotations = new TreeSet<AnnotationPosition>();
        if (sentence == null || sentence.trim().length() == 0) return annotations;
        AnnotationPosition ap = null;
        BagOfWords tkns = new BagOfWords(sentence);
        String token = null;
        StringBuffer sb = new StringBuffer();
        String similarity = "~" + minSimilarity + " ";
        for (int ti = 0; ti < tkns.tokens.length; ++ti) {
            token = tkns.tokens[ti];
            sb.append(token).append(similarity);
        }
        if (sb.length() == 0) return annotations;
        Query query;
        Hits hits;
        try {
            query = parser.parse(sb.toString());
            query = query.rewrite(reader);
            hits = searcher.search(query);
            HashMap<String, AnnotationPosition> matchedEntities = new HashMap<String, AnnotationPosition>();
            int end = Math.min(hits.length(), maxCandidatesConsidered);
            if (hits.length() >= 1) {
                for (int i = 0; i < end; ++i) {
                    if (matchedEntities.get(hits.doc(i).get(nameField)) != null) continue; else {
                        ap = match(sentence, hits.doc(i).get(searchField));
                        if (ap != null) {
                            ap.setNormalised(hits.doc(i).get(nameField));
                            if (ap.getConfidenceRate() >= confidenceThreshold) {
                                annotations.add(ap);
                                matchedEntities.put(hits.doc(i).get(nameField), ap);
                            }
                        }
                    }
                }
            }
        } catch (ParseException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return annotations;
    }

    /**
	 * Identifies the mention of entity in a sentence. If a match is found 
	 * with confidence above the threshold, then an {@link AnnotationPosition} 
	 * object is returned.
	 * 
	 * Assumes that there is only one <b>best</b> match of the entity in the sentence.
	 * <p>
	 * The similarity measure between spans of text is computed at a word-level, 
	 * rather than at character level, allowing for flexible word order and missing words.
	 * 
	 * @param sentence to be marked
	 * @param entity to be identified
	 * @return an {@link AnnotationPosition} if a mention is found or {@code null} otherwise
	 */
    private AnnotationPosition match(String sentence, String entity) {
        int start = sentence.toLowerCase().indexOf(entity.toLowerCase());
        if (start >= 0) {
            AnnotationPosition ap = new AnnotationPosition(start, entity.length(), this.entityType);
            ap.setConfidenceRate(1.0);
            return ap;
        }
        String[] tTokens = new BagOfWords(sentence, filterStopwords).getTokens();
        String[] uTokens = new BagOfWords(entity, filterStopwords).getTokens();
        ArrayList<Candidates> candidateList = obtainCandidateList(tTokens, uTokens);
        sortCandidateList(candidateList);
        double scoreValue = 0.0;
        Set<Integer> tMap = new HashSet<Integer>(), uMap = new HashSet<Integer>();
        Candidates[] match = new Candidates[tTokens.length];
        int minTPos = tTokens.length, maxTPos = -1, idx = 0;
        for (Candidates actualCandidates : candidateList) {
            Integer tPos = new Integer(actualCandidates.getTPos());
            Integer uPos = new Integer(actualCandidates.getUPos());
            if ((!tMap.contains(tPos)) && (!uMap.contains(uPos))) {
                scoreValue += actualCandidates.getScore();
                tMap.add(tPos);
                uMap.add(uPos);
                idx = tPos.intValue();
                match[idx] = actualCandidates;
                if (idx < minTPos) minTPos = idx;
                if (idx > maxTPos) maxTPos = idx;
            }
        }
        if (scoreValue / uTokens.length < minHybridSimilarity) return null;
        double newScore = 0.;
        int span = (maxTPos - minTPos + 1) + uTokens.length - uMap.size(), newSpan = span;
        for (Candidates actualCandidates : candidateList) {
            Integer tPos = new Integer(actualCandidates.getTPos());
            Integer uPos = new Integer(actualCandidates.getUPos());
            if (uPos.intValue() == match[minTPos].getUPos() && actualCandidates != match[minTPos]) {
                if (tPos.intValue() > minTPos && tPos.intValue() < maxTPos) {
                    newSpan = (maxTPos - Math.min(tPos.intValue(), next(match, minTPos)) + 1) + uTokens.length - uMap.size();
                    newScore = scoreValue - match[minTPos].getScore() + actualCandidates.getScore();
                    if (newScore / newSpan > scoreValue / span) {
                        scoreValue = newScore;
                        match[minTPos] = null;
                        minTPos = Math.min(tPos.intValue(), next(match, minTPos));
                        match[minTPos] = actualCandidates;
                        span = newSpan;
                    }
                }
            } else if (uPos.intValue() == match[maxTPos].getUPos() && actualCandidates != match[maxTPos]) {
                if (tPos.intValue() > minTPos && tPos.intValue() < maxTPos) {
                    newSpan = (Math.max(tPos.intValue(), previous(match, maxTPos)) - minTPos + 1) + uTokens.length - uMap.size();
                    newScore = scoreValue - match[maxTPos].getScore() + actualCandidates.getScore();
                    if (newScore / newSpan > scoreValue / span) {
                        scoreValue = newScore;
                        match[maxTPos] = null;
                        maxTPos = Math.max(tPos.intValue(), previous(match, maxTPos));
                        match[maxTPos] = actualCandidates;
                        span = newSpan;
                    }
                }
            }
        }
        scoreValue = scoreValue / span;
        sentence = sentence.toLowerCase();
        start = 0;
        int end = 0, i = 0;
        for (i = 0; i <= minTPos; i++) start = sentence.indexOf(tTokens[i], start);
        for (end = start + tTokens[i - 1].length(); i <= maxTPos; i++) end = sentence.indexOf(tTokens[i], end) + tTokens[i].length();
        if (scoreValue >= minHybridSimilarity) {
            AnnotationPosition ap = new AnnotationPosition(start, end - start, this.entityType);
            ap.setConfidenceRate(scoreValue);
            return ap;
        }
        return null;
    }

    /**
	 * Returns the first not null position in the array, starting with the startIdx
	 * 
	 * @param array 	the array to look in
	 * @param startIdx  the staring index (not inclusive)
	 * @return
	 */
    private int next(Candidates[] array, int startIdx) {
        int i = startIdx + 1;
        for (; i < array.length; i++) {
            if (array[i] != null) return i;
        }
        return startIdx;
    }

    /**
	 * Returns the previous not null position in the array, starting with the startIdx 
	 * 
	 * @param array 	the array to look in
	 * @param startIdx  the staring index (not inclusive)
	 * @return
	 */
    private int previous(Candidates[] array, int startIdx) {
        int i = startIdx - 1;
        for (; i >= 0; i--) {
            if (array[i] != null) return i;
        }
        return startIdx;
    }

    /**
	 * Get a list of candidate pairs of similar words (corresponding to a 
	 * sparse similarity matrix). 
	 *
	 * @param tTokens 
	 *            array of the words in the sentence 
	 * @param uTokens 
	 *            array of words of the candidate entity
	 * @return 
	 *            a list of pairs of words from the two arrays with a 
	 *            similarity score above the threshold {@link HybridEntityAnnotator#minSimilarity}
	 */
    private ArrayList<Candidates> obtainCandidateList(String[] tTokens, String[] uTokens) {
        ArrayList<Candidates> candidateList = new ArrayList<Candidates>();
        for (int t = 0; t < tTokens.length; t++) {
            int lastTr = -1;
            for (int u = 0, flag = 0; u < uTokens.length && flag == 0; u++) {
                int tr = Math.abs(t - u);
                if (lastTr >= 0 && lastTr < tr) {
                    flag = 1;
                } else {
                    String tTok = tTokens[t], uTok = uTokens[u];
                    double innerScore = (characterBasedSimilarity.getSimilarity(tTok, uTok));
                    if (innerScore == 1) {
                        lastTr = tr;
                    }
                    if (innerScore > minSimilarity) candidateList.add(new Candidates(t, u, innerScore));
                }
            }
        }
        return candidateList;
    }

    /**
	 * Sort the list of Candidates in decreasing order
	 * @param list
	 */
    @SuppressWarnings("unchecked")
    private void sortCandidateList(ArrayList<Candidates> list) {
        java.util.Collections.sort(list, new java.util.Comparator() {

            public int compare(Object o1, Object o2) {
                double scoreT = ((Candidates) o1).getScore();
                double scoreU = ((Candidates) o2).getScore();
                int c = (int) (scoreU * 100000000.00) - (int) (scoreT * 100000000.00);
                return c;
            }
        });
    }

    /**
	 * getMinStringSize count the number of characters in String array tTokens and
	 * String array uTokens and return the minimun size.
	 *
	 * @param tTokens String[]
	 * @param uTokens String[]
	 * @return double
	 */
    @SuppressWarnings("unused")
    private double getMinStringSize(String[] tTokens, String[] uTokens) {
        double tSize = 0, uSize = 0;
        for (int i = 0; i < tTokens.length; i++) {
            tSize += tTokens[i].length();
        }
        for (int i = 0; i < uTokens.length; i++) {
            uSize += uTokens[i].length();
        }
        return Math.min(tSize, uSize);
    }

    /**
	 * Check if this annotator instance ignores stop words
	 * @return {@code true} if the token level similarity measure ignores stop words and
	 * 			{@code false} if they are factored in the similarity score
	 */
    public boolean isFilterStopwords() {
        return filterStopwords;
    }

    /**
	 * Set whether or not this annotator instance should ignore stop words
	 * 
	 * @param filterStopwords {@code true} if the token level similarity measure should ignore stop words and
	 * 			{@code false} if they should be factored in the similarity score
	 */
    public void setFilterStopwords(boolean filterStopwords) {
        this.filterStopwords = filterStopwords;
    }

    /**
	 * The type of entity recognised (such as {@code PERSON}, {@code CINEMA}, see {@link AnnotationPosition})
	 * @return an entity type
	 */
    public String getEntityType() {
        return entityType;
    }

    /**
	 * Set the type of entity recognised (such as {@code PERSON}, {@code CINEMA}, see {@link AnnotationPosition})
	 * @param entityType an entity type
	 */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public double getMinHybridSimilarity() {
        return minHybridSimilarity;
    }

    /**
	 *  Set the threshold value for the token-level similarity 
	 *  		between spans of text. 
	 * 			Must be a positive value less than 1.0.
	 * 
	 * 			If the similarity score between two spans is below this limit
	 * 			then they are considered too dissimilar to be considered a  
	 * 			mention to the same entity. 
	 * @param minHybridSimilarity
	 */
    public void setMinHybridSimilarity(double minHybridSimilarity) {
        this.minHybridSimilarity = minHybridSimilarity;
    }

    public int getMaxCandidatesConsidered() {
        return maxCandidatesConsidered;
    }

    public double getMinSimilarity() {
        return minSimilarity;
    }

    /**
	 * Sets the maximum number of candidates considered relevant from the 
	 * 			entities returned from the index. 
	 * @param maxCandidatesConsidered
	 */
    public void setMaxCandidatesConsidered(int maxCandidatesConsidered) {
        this.maxCandidatesConsidered = maxCandidatesConsidered;
    }

    /**
	 * Set the threshold value for the edit distance between words. 
	 * 			Must be a positive value less than 1.0.
	 * 
	 * 			If the similarity score between two words is below this limit
	 * 			then they are considered too dissimilar to be part of the same 
	 * 			entity mention. 
	 *   
	 * @param minSimilarity
	 */
    public void setMinSimilarity(double minSimilarity) {
        this.minSimilarity = minSimilarity;
    }

    /** 
	 * Get the name of the index field containing an alias of an entity. 
	 * @return the field name
	 */
    public String getSearchField() {
        return searchField;
    }

    /**
	 * Get the name of the field in the index containing the name (id) of an entity.
	 * @return the field name
	 */
    public String getNameField() {
        return nameField;
    }

    /**
	 * Set the name of the index field containing an alias of an entity. 
	 * Default value is  {@code "text"}.
	 * @param searchField the field name
	 */
    public void setSearchField(String searchField) {
        this.searchField = searchField;
    }

    /**
	 * Set the name of the field in the index containing the name (id) of an entity.
	 * Default value is {@code "name"}
	 * @param nameField the field name
	 */
    public void setNameField(String nameField) {
        this.nameField = nameField;
    }
}

/**
 * This class represents cells of the sparse similarity matrix between two spans of text
 * @author idornescu
 */
class Candidates {

    private int tPos, uPos;

    private double score;

    /**
	 * Candidates constructor. Creates an instance of a candidate string pair T and U. It
	 * requires the position of the pair in the string and the similarity score
	 * between them.
	 *
	 * @param tPos the position of a token in the first text span (T) 
	 * @param uPos the position of a token in the second text span (U)
	 * @param score the similarity between the corresponding tokens
	 */
    public Candidates(int tPos, int uPos, double score) {
        this.tPos = tPos;
        this.uPos = uPos;
        this.score = score;
    }

    /**
	 * Return the position of the token in the first span (T).
	 * @return positive integer
	 */
    public int getTPos() {
        return tPos;
    }

    /**
	 * Return the position of the token in the second span (U).
	 * @return positive integer
	 */
    public int getUPos() {
        return uPos;
    }

    /**
	 * Return the similarity between the corresponding tokens.
	 *
	 * @return a value between {@code 0} (no similarity) and {@code 1} (identical)
	 */
    public double getScore() {
        return score;
    }
}

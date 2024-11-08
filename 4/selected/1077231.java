package org.bejug.javacareers.jobs.search.lucene;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenGroup;

/**
 * Adapted from JUnit Test for Highlighter class by mark@searcharea.co.uk
 *
 * @author Bavo Bruylandt (Last modified by $Author: shally $)
 * @version $Revision: 1.7 $ - $Date: 2005/12/20 15:36:46 $
 */
public class ContextSearcher implements Formatter {

    private static final Log LOG = LogFactory.getLog(ContextSearcher.class);

    private static final String FIELD_NAME = "body";

    private static final String PATH_NAME = "path";

    private static final String USER_NAME = "user";

    private static ContextSearcher contextSearch;

    private IndexReader reader;

    private Query query;

    private Searcher searcher;

    private Hits hits;

    private String indexPath;

    private Analyzer analyzer = new StandardAnalyzer();

    private String endTag = "<b>";

    private String startTag = "</b>";

    /**
     * Constructor for ContextSearcher.
     *
     * @param path Path to file
     */
    private ContextSearcher(String path) {
        File indexPathFile = new File(path);
        indexPath = indexPathFile.getAbsolutePath() + File.separator;
        if (!indexPathFile.exists() || !indexPathFile.isDirectory()) {
            LOG.info("Debug: IndexPath didnt exist: " + path + " creating indexer first...");
            try {
                PdfIndexer.createPdfIndexer(path);
                LOG.info("Debug: Indexer created");
            } catch (PdfException e) {
                LOG.error(e);
            }
        }
    }

    /**
     *
     * @param searchString String
     * @param contextLength int
     * @return List
     * @throws PdfException if an error
     */
    public List getContext(String searchString, int contextLength) throws PdfException {
        if (contextLength < 0) {
            throw new IllegalArgumentException("contextlength < 0");
        }
        List list = new ArrayList();
        doSearching(searchString);
        Formatter formatter = new SimpleHTMLFormatter(startTag, endTag);
        Highlighter highlighter = new Highlighter(formatter, new QueryScorer(query));
        highlighter.setTextFragmenter(new SimpleFragmenter(contextLength));
        int maxNumFragmentsRequired = 2;
        StringBuffer allresults = new StringBuffer();
        LOG.info("Debug: Searching..." + searchString + " " + contextLength);
        for (int i = 0; i < hits.length(); i++) {
            String text = null;
            String path = null;
            String user = null;
            Document doc = null;
            double score = -1;
            try {
                doc = hits.doc(i);
                text = doc.get(FIELD_NAME);
                path = doc.get(PATH_NAME);
                user = doc.get(USER_NAME);
                score = hits.score(i);
            } catch (IOException e) {
                LOG.debug(e);
            }
            TokenStream tokenStream = analyzer.tokenStream(FIELD_NAME, new StringReader(text));
            String result = null;
            try {
                result = highlighter.getBestFragments(tokenStream, text, maxNumFragmentsRequired, "...");
            } catch (IOException e) {
                LOG.debug(e);
            }
            allresults.append(result);
            SearchResult searchResult = new SearchResultImpl();
            searchResult.addContext(result);
            searchResult.setFile(path);
            searchResult.setQuery(query.toString());
            searchResult.setWeight(score);
            searchResult.setUser(user);
            list.add(searchResult);
        }
        try {
            if (reader != null) {
                reader.close();
            }
            if (searcher != null) {
                searcher.close();
            }
        } catch (IOException e) {
            LOG.debug(e);
        }
        LOG.info("Debug: Serach list: " + list);
        LOG.info("Debug: Found: " + list.size() + " items");
        return list;
    }

    /**
     *
     * @param originalText String
     * @param group TokenGroup
     * @return text string
     */
    public String highlightTerm(String originalText, TokenGroup group) {
        if (group.getTotalScore() <= 0) {
            return originalText;
        }
        return "" + originalText + "";
    }

    /**
     *
     * @param queryString String
     */
    private void doSearching(final String queryString) {
        try {
            reader = IndexReader.open(indexPath);
            String querieString = queryString.replaceAll("\\\\", "\\\\");
            querieString = querieString.replaceAll(":", "\\\\:");
            querieString = "\"" + querieString + "\"";
            LOG.info("Debug: Query after adaption:" + querieString);
            searcher = new IndexSearcher(indexPath);
            querieString = USER_NAME + ":" + querieString + " " + querieString;
            query = QueryParser.parse(querieString, FIELD_NAME, new StandardAnalyzer());
            LOG.info("Debug: Reader: " + reader);
            LOG.info("Debug: Query parsed: " + query.toString());
            query = query.rewrite(reader);
            LOG.info("Debug: Searching for: " + query.toString(FIELD_NAME) + " in " + indexPath);
            hits = searcher.search(query);
        } catch (IOException e) {
            LOG.debug(e);
        } catch (ParseException e) {
            LOG.debug(e);
        }
    }

    /**
     * @param startTag String
     * @param endTag String
     */
    public void setHighlightTags(String startTag, String endTag) {
        this.startTag = startTag;
        this.endTag = endTag;
    }

    /**
     *
     * @param path String
     * @return ContextSearcher 
     */
    public static ContextSearcher createContextSearch(String path) {
        if (contextSearch == null) {
            contextSearch = new ContextSearcher(path);
        }
        return contextSearch;
    }
}

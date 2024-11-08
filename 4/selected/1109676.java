package com.success.task.web.lucene.service;

import java.io.File;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import com.success.task.web.exceptions.LuceneException;
import com.success.task.web.lucene.core.FolderIndexCreator;
import com.success.task.web.lucene.data.DefaultSearchResult;
import com.success.task.web.lucene.data.ResultData;
import com.success.task.web.lucene.data.SearchResult;
import com.success.task.web.lucene.interfaces.IndexHighLighterInterface;
import com.success.task.web.lucene.interfaces.SearchServiceInterface;

public class DefaultSearchServiceImpl implements SearchServiceInterface {

    private Directory directory = null;

    private String indexDataDir = FolderIndexCreator.DEFAULT_DATA_DIRS;

    private Analyzer analyzer = null;

    private IndexReader reader = null;

    private QueryParser parser = null;

    private Searcher searcher = null;

    private String analyzerName = "net.paoding.analysis.analyzer.PaodingAnalyzer";

    public void init() throws LuceneException {
        try {
            directory = FSDirectory.getDirectory(new File(indexDataDir));
            analyzer = (Analyzer) Class.forName(analyzerName).newInstance();
            reader = IndexReader.open(directory);
            parser = new MultiFieldQueryParser(new String[] { FolderIndexCreator.INDEX_NAME, FolderIndexCreator.INDEX_FILE_NAME }, analyzer);
            searcher = new IndexSearcher(reader);
        } catch (Exception e) {
            throw new LuceneException(e);
        }
    }

    @Override
    public SearchResult search(String keywords, IndexHighLighterInterface ih, int page) throws LuceneException {
        List<ResultData> searchResult = null;
        long timeStart = System.currentTimeMillis();
        Hits hits = null;
        try {
            if (!reader.isCurrent()) {
                searcher.close();
                reader.close();
                reader = IndexReader.open(directory);
                searcher = new IndexSearcher(reader);
            }
            Query query = parser.parse(keywords).rewrite(reader);
            hits = searcher.search(query);
            searchResult = ih.getHighLightedList(hits, reader, query, page);
        } catch (Exception e) {
            throw new LuceneException(e);
        }
        long timeEnd = System.currentTimeMillis();
        return new DefaultSearchResult(searchResult, (timeEnd - timeStart), hits == null ? 0L : hits.length());
    }

    public void setAnalyzerName(String analyzerName) {
        this.analyzerName = analyzerName;
    }

    public void setIndexDataDir(String indexDataDir) {
        this.indexDataDir = indexDataDir;
    }
}

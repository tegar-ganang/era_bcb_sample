package p2p.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.apache.tika.exception.TikaException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.xml.sax.SAXException;
import p2p.lucene.model.FileDocument;

public class IndexManager {

    private static final Logger logger = Logger.getLogger(IndexManager.class);

    private static final int HITS_PER_PAGE = 10;

    private File indexDir;

    private File docDir;

    private IndexWriter writer;

    private StandardAnalyzer analyzer;

    private IndexReader reader;

    public boolean initIndex(File indexDir) throws CorruptIndexException, LockObtainFailedException, IOException {
        this.indexDir = indexDir;
        boolean exists = false;
        if (indexDir.exists()) exists = true;
        analyzer = new StandardAnalyzer(Version.LUCENE_30);
        Directory directory = FSDirectory.open(indexDir);
        writer = new IndexWriter(directory, analyzer, IndexWriter.MaxFieldLength.LIMITED);
        reader = writer.getReader();
        docDir = new File("Documents");
        return exists;
    }

    public void destroy() {
        try {
            writer.close();
            analyzer.close();
            reader.close();
        } catch (Exception e) {
            logger.error("Problem destroying the index manager", e);
        }
    }

    private IndexManager() {
    }

    /**
	 * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
	 * or the first access to SingletonHolder.INSTANCE, not before.
	 */
    private static class SingletonHolder {

        private static final IndexManager INSTANCE = new IndexManager();
    }

    public static IndexManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void createIndex(File docDir) {
        this.docDir = docDir;
        if (!docDir.exists() || !docDir.canRead()) {
            logger.warn("Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path");
            return;
        }
        Date start = new Date();
        try {
            logger.info("Indexing to directory '" + indexDir + "'...");
            indexDocs(docDir);
            logger.info("Optimizing...");
            finish();
            Date end = new Date();
            logger.info(end.getTime() - start.getTime() + " total milliseconds");
        } catch (IOException e) {
            logger.error(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    private void finish() throws CorruptIndexException, IOException {
        writer.optimize();
        writer.commit();
    }

    private void indexDocs(File file) throws IOException {
        if (file.canRead()) {
            if (file.isDirectory()) {
                String[] files = file.list();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        indexDocs(new File(file, files[i]));
                    }
                }
            } else {
                logger.trace("adding " + file);
                try {
                    writer.addDocument(FileDocument.Document(file, docDir));
                } catch (FileNotFoundException fnfe) {
                } catch (SAXException e) {
                    logger.warn("Problem parsing document: " + file.getName(), e);
                } catch (TikaException e) {
                    logger.warn("Problem parsing document: " + file.getName(), e);
                }
            }
        }
    }

    public void addDocument(File newFile) throws Exception {
        if (!newFile.exists() || !newFile.canRead()) {
            logger.warn("Document or directory '" + newFile.getAbsolutePath() + "' does not exist or is not readable, please check the path");
            return;
        }
        if (!indexDir.exists()) {
            logger.warn("Index does not exist. Exiting.");
            return;
        }
        Date start = new Date();
        try {
            logger.debug("Adding '" + newFile + "' to index '" + indexDir + "'...");
            indexDocs(newFile);
            logger.debug("Optimizing...");
            finish();
            Date end = new Date();
            logger.debug(end.getTime() - start.getTime() + " total milliseconds");
        } catch (IOException e) {
            logger.error(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
    }

    public void removeDocument(File file) throws Exception {
        if (!indexDir.exists()) {
            logger.warn("Index does not exist. Exiting.");
            return;
        }
        String path = file.getPath().replace(docDir.getPath(), "");
        logger.debug("Path to remove: " + path);
        writer.deleteDocuments(new Term("path", path));
        finish();
        logger.debug("Removed '" + file.getName() + "' from index");
    }

    public void updateDocument(File file) throws Exception {
        if (!indexDir.exists()) {
            logger.warn("Index does not exist. Exiting.");
            return;
        }
        String path = file.getPath().replace(docDir.getPath(), "");
        writer.updateDocument(new Term("path", path), FileDocument.Document(file, docDir));
        finish();
    }

    public String search(String line) throws Exception {
        JSONObject jsonObj = (JSONObject) JSONValue.parse(line);
        String query = (String) jsonObj.remove("query");
        List<Document> results = doSearch(query);
        if (results == null) return null;
        JSONArray resultArr = new JSONArray();
        if (results != null) {
            for (Document d : results) {
                JSONObject resultObj = new JSONObject();
                resultObj.put("path", d.get("path"));
                resultObj.put("modified", d.get("modified"));
                resultObj.put("size", d.get("size"));
                resultArr.add(resultObj);
            }
        }
        jsonObj.put("searchResults", resultArr);
        return jsonObj.toJSONString();
    }

    private List<Document> doSearch(String line) throws Exception {
        String field = "contents";
        try {
            reader = reader.reopen();
        } catch (IOException e) {
            logger.warn("Exception in doSimpleSearch - Could not open index: " + indexDir.getAbsolutePath());
            return null;
        }
        Searcher searcher = new IndexSearcher(reader);
        QueryParser parser = new QueryParser(Version.LUCENE_30, field, analyzer);
        Query query = parser.parse(line);
        TopScoreDocCollector collector = TopScoreDocCollector.create(HITS_PER_PAGE, false);
        searcher.search(query, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;
        List<Document> docs = new ArrayList<Document>(hits.length);
        for (int i = 0; i < hits.length; i++) {
            Document doc = searcher.doc(hits[i].doc);
            docs.add(doc);
        }
        searcher.close();
        return docs;
    }
}

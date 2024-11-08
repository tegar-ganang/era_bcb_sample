package org.rg.lucene;

import gnu.trove.TLongLongHashMap;
import java.io.File;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.document.NumberTools;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ModelIndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.solr.util.NumberUtils;
import org.rg.common.Keys;
import org.rg.common.QueryKeys;
import org.rg.common.properties.CommonProperties;
import org.rg.common.query.ConstraintEncoding;
import org.rg.common.query.QueryWrapper;
import org.rg.lucene.clientrecords.ClientRecord;
import org.rg.lucene.clientrecords.PageCallback;
import org.rg.lucene.util.FieldAnalyzer;

public class RMIServer extends QueryBase implements Remote, SearcherConnection, IndexerConnection, ControllerConnection {

    static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog("RMIServer");

    private static final int MAX_FIELD_LENGTH = 500000;

    static final int MAXIMUM_BOOLEAN_CLAUSES = 50000;

    static final int RAM_BUFFER_SIZE = 48;

    /**
    * Initialize some shared lucene objects, including analyzer, parser
    * @return true if all objects are created successfully
    */
    private boolean initLuceneTools() {
        try {
            analyzer = new FieldAnalyzer("English", StandardAnalyzer.STOP_WORDS);
            if (analyzer == null) return false;
            parser = new RgQueryParser(LuceneFieldName.content.toString(), analyzer);
            if (parser == null) return false;
        } catch (Exception e) {
            log.error("Can't initialize lucene tools", e);
            return false;
        }
        return true;
    }

    private IndexReader reader = null;

    private IndexSearcher searcher = null;

    private IndexSearcher querySearcher = null;

    /** Time after which new searcher is opened and warmed up (minimum time gap) */
    private static final int SEARCHER_REOPEN_MIN_INTERVAL = 5;

    /** Time after which new searcher is opened and warmed up */
    private static final int SEARCHER_REOPEN_MAX_INTERVAL = 40;

    private List<IndexReader> oldReadersList = new ArrayList<IndexReader>();

    /** Reader will remain open after closed is called (in secs) */
    static final int READER_CLOSE_TIME_INTERVAL = 300000;

    private static TLongLongHashMap verTimeMap = new TLongLongHashMap(10);

    /**
    * Get time wait for again reopening the searcher
    * @param maxDocs number of documents in the index
    * @return waiting time
    */
    private int getTimeInterval(int maxDocs) {
        int numDocs = 20000;
        int ratio = maxDocs / numDocs;
        int timeInterval = ratio * SEARCHER_REOPEN_MIN_INTERVAL;
        if (timeInterval < 1) {
            timeInterval = 1;
        }
        return (timeInterval > SEARCHER_REOPEN_MAX_INTERVAL || timeInterval <= 0) ? SEARCHER_REOPEN_MAX_INTERVAL : timeInterval;
    }

    /**
    * Thread used to reopen the searcher at periodic interval and warm up the cache
    * @author vmehra
    */
    class SearcherOpenThread implements Runnable {

        public void run() {
            while (true) {
                try {
                    IndexReader reader = getReader();
                    if (querySearcher == null || querySearcher.getIndexReader() != reader) {
                        IndexSearcher searcher = new ModelIndexSearcher(reader);
                        TopDocCollector collector = new TopDocCollector(reader.numDocs());
                        searcher.search(new MatchAllDocsQuery(), collector);
                        ScoreDoc[] docs = collector.topDocs().scoreDocs;
                        querySearcher = searcher;
                    }
                    log.debug("Query Searcher updated at time" + new Date());
                } catch (Exception e) {
                    log.error("Error updating the searcher at periodic interval", e);
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(getTimeInterval(reader.maxDoc()) * 60 * 1000);
                } catch (Exception e) {
                    log.error("Error in thread sleeping", e);
                }
            }
        }
    }

    /**
    * For uptoDate = true, return an instance of IndexSearcher. For uptoDate = false, return an instance of
    * ModelIndexSearcher. IndexSearcher gets initialized faster and uses a no or minimal cache IndexReader.
    * IndexSearcher can be used for duplicate detection, doument id lookup etc. ModelIndexSearcher takes longer to
    * initialize and takes up memory as it loads relevance values as String[]. ModelIndexSearcher performs better IR
    * task and should be used to perform keyword queries.
    * @param uptoDate
    * @return if uptoDate is true, return IndexSearcher, else return an instance of ModelIndexSearcher
    * @throws IOException
    */
    public synchronized IndexSearcher getSearcher(boolean uptoDate) throws IOException {
        if (uptoDate) {
            IndexReader indexReader = getReader();
            if (searcher == null) {
                searcher = new IndexSearcher(indexReader);
            } else {
                if (searcher.getIndexReader() != indexReader) {
                    searcher.close();
                    searcher = new IndexSearcher(indexReader);
                }
            }
        } else {
            if (querySearcher == null) {
                return new ModelIndexSearcher(getReader());
            }
            return querySearcher;
        }
        return searcher;
    }

    /**
    * Get an IndexReader
    * @return the Reader
    */
    public synchronized IndexReader getReader() throws IOException {
        if (reader == null) {
            Directory dir = FSDirectory.getDirectory(indexDir);
            reader = IndexReader.open(dir, true);
        } else {
            IndexReader newReader = reader.reopen();
            if (newReader != reader) {
                reader = newReader;
            }
        }
        return reader;
    }

    /**
    * close the reader
    * @param reader IndexReader
    */
    private void closeReader(IndexReader reader) {
        try {
            reader.close();
        } catch (Exception e) {
            log.error("Error closing the reader", e);
        }
    }

    /**
    * Method check if it is safe to close the old unused reader
    * @param reader IndexReader
    * @return true, if it is safe to close the reader
    */
    private boolean safeToCloseReader(IndexReader reader) {
        long ver = reader.getVersion();
        if (searcher != null) {
            long searchVer = searcher.getIndexReader().getVersion();
            if (ver == searchVer) {
                return false;
            }
        }
        long time = verTimeMap.get(ver);
        if (time != 0 && (System.currentTimeMillis() - time) < READER_CLOSE_TIME_INTERVAL) {
            return false;
        }
        verTimeMap.remove(ver);
        return true;
    }

    private int numDocs = 0;

    private long indexTime = 0;

    /** a set of the available indexer names to use for text fields */
    private Set<String> customTextFieldNames;

    /** a set of the available indexer names to use for integer fields */
    private Set<String> customIntegerFieldNames;

    /** a set of the available indexer names to use for date fields */
    private Set<String> customDateFieldNames;

    /** an object to encapsulate database functions */
    private DBFunctions dbFunctions;

    private long maxID = 1L;

    /**
    * This method initialize next ID to assign.
    * @return false if an exception occurs
    */
    private boolean initIDDispenser() {
        IndexReader reader = null;
        try {
            reader = this.getReader();
            if (reader.numDocs() > 0) {
                long start = System.currentTimeMillis();
                int maxDoc = reader.maxDoc();
                long tempMaxID = 0;
                FieldSelector fs = new MapFieldSelector(new String[] { "id" });
                for (int i = 0; i < maxDoc; ++i) {
                    if (!reader.isDeleted(i)) {
                        Document d = reader.document(i, fs);
                        String id = d.get("id");
                        long idLong = NumberTools.stringToLong(id);
                        if (idLong > tempMaxID) {
                            tempMaxID = idLong;
                        }
                    }
                }
                maxID = tempMaxID + 1;
                long end = System.currentTimeMillis();
                log.info("Initial id:" + maxID + " Computed in : " + (end - start) + " ms.");
            }
        } catch (Exception e) {
            log.error("Couldn't initialized ID dispenser.", e);
            return false;
        }
        return true;
    }

    /**
    * Initialize a index writer. This is a little tricky because the index directory might be locked by an active writer
    * or a dead writer (killed by the user or some things unusal. At this time we assume the lock is held by a dead
    * writer and force to unlock it. Note: it is problematic if the lock is held by an actove writer.
    * @return true of an index writer is assigned to writer variable; false if all attempts fail -- disk full is one
    *         possible reason
    */
    private boolean initIndexWriter(boolean forceUnlock) {
        File index = new File(indexDir);
        boolean create = !(index).exists();
        if (index.exists()) {
            if (!index.isDirectory()) {
                create = true;
            } else {
                File[] files = index.listFiles();
                if (files.length == 0) {
                    create = true;
                }
            }
        }
        try {
            if (IndexWriter.isLocked(indexDir) && forceUnlock) {
                IndexWriter.unlock(FSDirectory.getDirectory(indexDir));
                log.info("Index directory locked! Unlocked forcefully.");
            }
            writer = new IndexWriter(indexDir, analyzer, create, new IndexWriter.MaxFieldLength(MAX_FIELD_LENGTH));
            writer.setRAMBufferSizeMB(RAM_BUFFER_SIZE);
        } catch (IOException ioe) {
            log.error("Cannot open index directory!", ioe);
            return false;
        }
        return initIDDispenser();
    }

    public synchronized Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("indexCount", numDocs);
        stats.put("indexTime", indexTime);
        try {
            stats.put("indexDocNum", getReader().numDocs());
        } catch (IOException ioe) {
            log.error("Error getting stats!", ioe);
        }
        return stats;
    }

    /**
    * The currently available highest index.
    * @return currently available highest index.
    */
    public long currentId() {
        return maxID;
    }

    /**
    * Insert a document into the index.
    */
    public long add(Document doc) throws LuceneException {
        long id;
        try {
            if (doc.get(LuceneFieldName.id.toString()) == null) {
                id = maxID++;
            } else {
                String idStr = doc.get(LuceneFieldName.id.toString());
                id = NumberTools.stringToLong(idStr);
                if (getReader().docFreq(new Term(LuceneFieldName.id.toString(), idStr)) > 0) {
                    log.warn("Adding document, duplicate id:" + id);
                    return -id;
                }
            }
            doc.add(LuceneFieldName.createField("id", id));
            numDocs++;
            long t = System.currentTimeMillis();
            writer.addDocument(doc);
            indexTime += (System.currentTimeMillis() - t);
            writer.commit();
        } catch (IOException ioe) {
            log.error("Index writer IO exception.", ioe);
            throw new LuceneException("Index Writer Error.");
        }
        return id;
    }

    /**
    * @param docs
    * @param custom_metadata
    * @return
    * @throws org.rg.lucene.LuceneException
    */
    public synchronized long[] add(List<Document> docs) throws LuceneException {
        long id, ids[] = new long[docs.size()];
        Iterator<Document> iter = docs.iterator();
        String idStr;
        int i = 0;
        try {
            while (iter.hasNext()) {
                Document doc = iter.next();
                if (doc.get(LuceneFieldName.id.toString()) == null) {
                    ids[i] = maxID++;
                    doc.add(LuceneFieldName.createField("id", ids[i++]));
                } else {
                    idStr = doc.get(LuceneFieldName.id.toString());
                    id = NumberTools.stringToLong(idStr);
                    if (getReader().docFreq(new Term(LuceneFieldName.id.toString(), idStr)) > 0) {
                        log.warn("Adding documents, duplicate id:" + id);
                        ids[i++] = -id;
                        continue;
                    } else {
                        ids[i++] = id;
                        if (id >= maxID) maxID = id + 1;
                    }
                }
                numDocs++;
                long t = System.currentTimeMillis();
                writer.addDocument(doc);
                indexTime += (System.currentTimeMillis() - t);
            }
            writer.commit();
        } catch (IOException e) {
            log.error("Index writer IO exception.", e);
            throw new LuceneException(e.toString());
        }
        return ids;
    }

    private Analyzer analyzer = null;

    private IndexWriter writer = null;

    private QueryParser parser = null;

    private String indexDir = null;

    static int MAX_HITS = 100000;

    /**
    * Constructs a lucene server. In the future we may want to limit the role of the server: for example searcher only,
    * controller only or indexer only
    * @param index is the index directory name
    * @param host is the DB server ip address
    * @param database is the database name
    * @param user is the user name to access the DB
    * @param password is the password of the user
    * @throws Exception when the database cannot be contacted
    * @throws RemoteException
    */
    public RMIServer(String index, boolean forceUnlock, String driver, String url, String user, String password) throws Exception, RemoteException {
        super();
        if (driver != null) {
            LuceneRMIServerProperties p = LuceneRMIServerProperties.props;
            String contentUrl = p.getProperty(CommonProperties.GLOBAL_DB_URL);
            String contentDriver = p.getProperty(CommonProperties.GLOBAL_DB_DRIVER);
            String contentUser = p.getProperty(CommonProperties.GLOBAL_DB_USER);
            String contentPass = p.getProperty(CommonProperties.GLOBAL_DB_PASSWORD);
            try {
                dbFunctions = new DBFunctions(driver, url, user, password);
            } catch (Exception ex) {
                log.error("Error initializing DB connection or functions.", ex);
                throw ex;
            }
        }
        try {
            indexDir = index;
            if (!initLuceneTools() || !initIndexWriter(forceUnlock)) {
                System.exit(-1);
            } else {
                new Thread(new SearcherOpenThread()).start();
            }
        } catch (Exception e) {
            System.exit(-1);
        }
    }

    public RMIServer(String index, boolean forceUnlock) throws Exception, RemoteException {
        this(index, forceUnlock, null, null, null, null);
    }

    public RMIServer(String index, String driver, String url, String user, String password) throws Exception, RemoteException {
        this(index, false, driver, url, user, password);
    }

    /**
    * documentsExists checks duplicate documents in a batch using documentExists it saves latency although I doubt the
    * actual gain
    * @param fpss is an array of fingerprints
    * @return an array of boolean
    */
    public boolean[] documentsExist(long[] fpss[]) throws LuceneException {
        boolean[] ret = new boolean[fpss.length];
        for (int i = 0; i < fpss.length; i++) ret[i] = documentExists(fpss[i]);
        return ret;
    }

    /**
    * check if list of web urls already exist in the lucene
    * @param urls is an array of web urls
    * @return an array of boolean
    */
    public boolean[] urlsExist(String[] urls) throws LuceneException {
        boolean[] ret = new boolean[urls.length];
        Term term = new Term(LuceneFieldName.url.getTranslatedName(), "");
        try {
            long t1 = System.currentTimeMillis();
            IndexReader reader = getReader();
            int trueCount = 0;
            for (int i = 0; i < urls.length; i++) {
                int docFreq = reader.docFreq(term.createTerm(urls[i]));
                if (docFreq > 0) {
                    ret[i] = true;
                    trueCount++;
                }
            }
            long t2 = System.currentTimeMillis();
            log.info("URL Exist check took " + (t2 - t1) + " No of urls: " + urls.length + " true found " + trueCount);
        } catch (IOException ioe) {
            log.error("Error looking for urls", ioe);
            throw new LuceneException("Lucene io error.");
        }
        return ret;
    }

    /**
    * documentExists check if a document has something similar in the index, the specification is there is a match if a
    * document in the index has more than MIX_FPS_MATCH fingerprints in common or if the input fingerprints are less
    * than MAX_FPS_NUM, all fingerprints must match The algorithm is very simple: sequentially querying the index on
    * each fingerprint. The catch is that if we cannot find a match for a certain number of fingerprints, the verdict is
    * sealed. For example, if we cannot find a match for 3 fingerprints, there is not possible to have 8 fingerprints
    * matching.
    * @param fps is the array of fingerprints
    * @return if a similar document is already in the index
    */
    public boolean documentExists(long fps[]) throws LuceneException {
        final int MAX_FPS_NUM = 10;
        final int MIN_FPS_MATCH = 8;
        long padding[] = new long[MAX_FPS_NUM];
        int fpsNum, minFpsMatch = MIN_FPS_MATCH;
        for (fpsNum = 0; (fpsNum < fps.length) && (fps[fpsNum] != 0); fpsNum++) padding[fpsNum] = fps[fpsNum];
        if (fpsNum < MAX_FPS_NUM) {
            minFpsMatch = MAX_FPS_NUM;
            for (; fpsNum < MAX_FPS_NUM; fpsNum++) padding[fpsNum] = MAX_FPS_NUM - fpsNum - 1;
        }
        fps = padding;
        int fpsMaxDiff = fpsNum - minFpsMatch;
        Map<Integer, Integer> candidates = new HashMap<Integer, Integer>();
        try {
            IndexReader reader = getReader();
            int found = 0;
            for (int i = 0; i < fpsNum; i++) {
                if (i - found > fpsMaxDiff) {
                    return false;
                }
                TermDocs docs = reader.termDocs(new Term("fingerprint", NumberTools.longToString(fps[i])));
                if (docs.next()) {
                    found++;
                    do {
                        int id = docs.doc();
                        Integer count;
                        if ((count = candidates.get(id)) == null) {
                            candidates.put(id, 1);
                        } else if (candidates.put(id, count + 1) >= (minFpsMatch - 1)) {
                            return true;
                        }
                    } while (docs.next());
                }
            }
        } catch (IOException ioe) {
            log.error("Error looking for duplicates", ioe);
            throw new LuceneException("Lucene io error.");
        }
        return false;
    }

    /**
    * This is the main method for a search request. Exhaustive search (no limit)
    * @param query is the query string
    * @return a documents object containing the matching documents
    */
    public Documents search(String queryString) {
        return search(queryString, null, -1, false);
    }

    /**
    * This is the main method for a search request with limit
    * @param query is the query string
    * @param limit is the max number of documents
    * @param ids list of ids to be searched in the index
    * @return a documents object containing the matching documents
    */
    public Documents search(String queryString, Integer[] ids, int limit) {
        return search(queryString, ids, limit, false);
    }

    /**
    * Search for content
    * @param query is the query string
    * @param customFields are the custom fields
    * @param content is the content
    * @param priority is the priority
    */
    public void search(Map query, PageCallback content, float priority) throws RemoteException {
        QueryWrapper qw = new QueryWrapper(query);
        Integer[] ids = (Integer[]) query.get(Keys.CONTENT_IDS);
        ClientRecord clientRecords;
        int limit = Integer.MAX_VALUE;
        Map constraints = (Map) query.get(Keys.CONSTRAINT_ELEMENTS);
        List<ConstraintEncoding> newconstraints = qw.getConstraints();
        if (newconstraints.size() > 0) {
            if (constraints == null) constraints = new HashMap();
            for (ConstraintEncoding ce : newconstraints) constraints.put(ce.getType(), ce.getEncoding());
        }
        if (constraints != null) {
            if (constraints.containsKey(QueryKeys.RESULT_LIMIT)) {
                limit = Integer.parseInt((String) constraints.get(QueryKeys.RESULT_LIMIT));
            }
        }
        String queryString = qw.getLuceneQuery(priority);
        Documents docs = search(queryString, null, limit);
        clientRecords = new ClientRecord(docs, this);
        gviThread = new GetVisInfoThread(content, clientRecords);
        gviThread.start();
    }

    /**
    * This is the main method for a search request with limit results are by default sorted by priority if ids are null.
    * In case if ids are not null, results will be sorted only if sort argument is true
    * @param query is the query string
    * @param limit is the max number of documents
    * @param ids list of ids to be searched in the index
    * @param sort sort the results by the priority, applies only if ids are not null
    * @return a documents object containing the matching documents
    */
    public Documents search(String queryString, Integer[] ids, int limit, boolean sort) {
        Query query = null;
        try {
            if (queryString != null && queryString.trim().length() > 0) {
                query = parser.parse(queryString);
            }
            Query idQuery = query;
            if (ids != null) {
                String idString = QueryWrapper.getLuceneIdsQuery(ids);
                idQuery = parser.parse(idString);
            }
            IndexSearcher searcher = null;
            IndexReader reader = null;
            long t1 = System.currentTimeMillis();
            TopDocCollector collector = null;
            TopDocs topDocs = null;
            long t2 = 0;
            if (ids == null || ids.length > limit) {
                searcher = getSearcher(false);
                reader = searcher.getIndexReader();
                int maxDoc = searcher.maxDoc();
                int limitSearch = limit;
                if ((limitSearch < 0) || (limitSearch > maxDoc)) limitSearch = maxDoc;
                collector = new TopDocCollector(limitSearch);
                t2 = System.currentTimeMillis();
                searcher.search(idQuery, collector);
                topDocs = collector.topDocs();
            }
            if (ids != null && (collector == null || collector.getTotalHits() < limit)) {
                searcher = getSearcher(true);
                reader = searcher.getIndexReader();
                int maxDoc = searcher.maxDoc();
                int limitSearch = limit;
                if ((limitSearch < 0) || (limitSearch > maxDoc)) limitSearch = maxDoc;
                t2 = System.currentTimeMillis();
                if (!sort) {
                    collector = new TopDocCollector(limitSearch);
                    searcher.search(idQuery, collector);
                    topDocs = collector.topDocs();
                } else {
                    topDocs = searcher.search(idQuery, null, limitSearch);
                }
            }
            long t3 = System.currentTimeMillis();
            ScoreDoc[] docs = topDocs.scoreDocs;
            long t4 = System.currentTimeMillis();
            if (query != null) {
                query = query.rewrite(reader);
            }
            long t5 = System.currentTimeMillis();
            long ctdt = t2 - t1;
            long st = t3 - t2;
            long tdt = t4 - t3;
            long rwt = t5 - t4;
            log.info("query:" + idQuery.toString() + " original:" + queryString + " time:cs(" + ctdt + ") srch(" + st + ") ftd(" + tdt + ") rewrt(" + rwt + ") count:" + docs.length);
            if (docs.length == 0) {
                return new Documents();
            } else {
                HashMap<String, String[]> copy = new HashMap<String, String[]>(queryFieldSelectors);
                QueryHandle qh = new QueryHandle(reader, docs, query);
                return new Documents(qh, copy, docs.length, st);
            }
        } catch (Exception e) {
            if (e instanceof IOException) log.error("Lucene IO error when performing search.", e); else if (e instanceof ParseException) log.error("Lucene parsing error on \"" + queryString + "\"", e); else {
                log.error("Error executing query", e);
                e.printStackTrace();
            }
            return new Documents();
        }
    }

    /** the thread that pulls the batches. */
    private GetVisInfoThread gviThread;

    public static final int s_MAX_RECORDS = 100;

    /**
    * Grab each chunk of the data in the background while the client is parsing the previous batch.
    * @author redman
    */
    class GetVisInfoThread extends java.lang.Thread {

        /** this is set if there is an exception. */
        private Throwable thrown = null;

        private PageCallback content = null;

        /** hashmap contains the query key for each current running query. */
        private ClientRecord clientRecords = null;

        /**
       * give the thread an appropriate name.
       */
        GetVisInfoThread(PageCallback content, ClientRecord clientRecords) {
            super("GetVisInfoThread");
            this.content = content;
            this.clientRecords = clientRecords;
        }

        /**
       * grab the batches of pages.
       */
        public void run() {
            HashMap[] batch;
            do {
                try {
                    batch = clientRecords.populateReturnMaps(content, s_MAX_RECORDS);
                    if (content.submitBatch(batch)) break;
                } catch (InterruptedException ie) {
                    break;
                } catch (Throwable te) {
                    setThrown(te);
                    log.error("Exception in GetVisInfoThread. We will " + "reset the connection and abort.", te);
                    try {
                        content.submitBatch(null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            } while (!isInterrupted() && batch != null);
        }

        /**
       * for execptions, we store the exception to be reported by the entry system.
       * @return the exception that was thrown, or null if no exception.
       */
        public synchronized Throwable getThrown() {
            return thrown;
        }

        /**
       * @param thrown
       */
        private synchronized void setThrown(Throwable thrown) {
            this.thrown = thrown;
        }
    }

    /**
    * Delete a document from the index given its content id.
    * @param content_id the content id
    * @return true if successful
    * @throws org.rg.lucene.LuceneException
    * @throws java.rmi.RemoteException
    */
    public boolean deleteDocument(long content_id) throws LuceneException, RemoteException {
        long[] ids = new long[] { content_id };
        return deleteDocuments(ids);
    }

    /**
    * Delete documents from the index with the given content ids.
    * @param content_ids the content ids
    * @return true if successful
    * @throws org.rg.lucene.LuceneException
    * @throws java.rmi.RemoteException
    */
    public synchronized boolean deleteDocuments(long[] content_ids) throws LuceneException, RemoteException {
        log.info("Attempting to delete " + content_ids.length + " documents from lucene index.  Size before: " + numDocs);
        Term[] terms = new Term[content_ids.length];
        for (int i = 0; i < content_ids.length; i++) {
            terms[i] = new Term(LuceneFieldName.id.getTranslatedName(), NumberTools.longToString(content_ids[i]));
        }
        try {
            writer.deleteDocuments(terms);
            writer.commit();
            try {
                numDocs = getReader().numDocs();
            } catch (IOException ex) {
                numDocs -= content_ids.length;
            }
            log.info("Deleted " + content_ids.length + " documents from the lucene index.  Size after: " + numDocs);
            return true;
        } catch (IOException ioe) {
            try {
                writer.rollback();
            } catch (IOException io) {
                log.error("Lucene IO error during rollback of deleting documents.", io);
                throw new LuceneException("IO Error when rolling back after delete failed.");
            }
            log.error("Lucene IO error when deleting document.", ioe);
            throw new LuceneException("IO error when deleting document.");
        }
    }

    /**
    * Delete old documents to reduce the corpus to the provided limit.
    * @param limit the maximum number of documents we will maintain.
    * @return number of documents deleted.
    * @throws org.rg.lucene.LuceneException
    * @throws IOException if there is an isssue with the index.
    */
    public synchronized int deleteOldestDocuments(int limit) throws LuceneException, RemoteException {
        try {
            IndexSearcher searcher = getSearcher(true);
            limit = reader.numDocs() - limit;
            if (limit <= 0) return 0;
            Sort sorter = new Sort(LuceneFieldName.date.getTranslatedName());
            BooleanQuery query = new BooleanQuery(true);
            query.add(new BooleanClause(new MatchAllDocsQuery(), BooleanClause.Occur.MUST));
            query.add(new BooleanClause(new TermQuery(new Term(LuceneFieldName.protocol.getTranslatedName(), "file")), BooleanClause.Occur.MUST_NOT));
            TopFieldDocs docs = searcher.search(query, null, limit, sorter);
            ScoreDoc[] td = docs.scoreDocs;
            Term[] terms = new Term[td.length];
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            for (int i = 0; i < td.length; i++) {
                Document doc = reader.document(td[i].doc);
                log.info("deleted " + doc.get(LuceneFieldName.url.getTranslatedName()));
                log.info("   protocol = " + doc.get(LuceneFieldName.protocol.getTranslatedName()));
                String dt = doc.get(LuceneFieldName.date.getTranslatedName());
                log.info("   date = " + dt);
                try {
                    Date date = DateTools.stringToDate(dt);
                    long dv = date.getTime();
                    if (dv > max) max = dv;
                    if (dv < min) min = dv;
                } catch (java.text.ParseException e) {
                    log.error("A date appears to be bad.", e);
                }
                terms[i] = new Term(LuceneFieldName.id.getTranslatedName(), doc.get(LuceneFieldName.id.getTranslatedName()));
            }
            writer.deleteDocuments(terms);
            writer.commit();
            try {
                numDocs = getReader().numDocs();
            } catch (IOException ex) {
                numDocs -= terms.length;
            }
            log.info("Delete " + terms.length + " documents based on date from " + new Date(min) + " to " + new Date(max) + ", " + numDocs + " remain.");
            return terms.length;
        } catch (IOException ioe) {
            try {
                writer.rollback();
            } catch (IOException io) {
                log.error("Lucene IO error during rollback of deleting documents.", io);
                throw new LuceneException("IO Error when rolling back after delete failed.");
            }
            log.error("Lucene IO error when deleting document.", ioe);
            throw new LuceneException("IO error when deleting document.");
        }
    }

    /**
    * Given the urls, delete the content associated with them.
    * @param urls the urls associated with the content to delete.
    * @return true if successful
    * @throws LuceneException 
    * @throws java.rmi.RemoteException
    */
    public synchronized boolean deleteDocumentsForURLs(String[] urls) throws LuceneException, RemoteException {
        log.info("Attempting to delete " + urls.length + " documents from lucene index, given their ids.  Size before: " + numDocs);
        Term[] terms = new Term[urls.length];
        for (int i = 0; i < urls.length; i++) {
            terms[i] = new Term(LuceneFieldName.url.getTranslatedName(), urls[i]);
        }
        try {
            writer.deleteDocuments(terms);
            writer.commit();
            try {
                numDocs = getReader().numDocs();
            } catch (IOException ex) {
                numDocs -= urls.length;
            }
            log.info("Deleted " + urls.length + " documents from the lucene index.  Size after: " + numDocs);
            return true;
        } catch (IOException ioe) {
            try {
                writer.rollback();
            } catch (IOException io) {
                log.error("Lucene IO error during rollback of deleting documents.", io);
                throw new LuceneException("IO Error when rolling back after delete failed.");
            }
            log.error("Lucene IO error when deleting document.", ioe);
            throw new LuceneException("IO error when deleting document.");
        }
    }

    /**
    * Close the main lucene index writer
    */
    public void closeIndexWriter() {
        if (writer == null) return;
        try {
            writer.close();
            log.info("Index writer closed succesfully");
        } catch (IOException ioe) {
            log.error("Cannot close the index writer: " + writer.getDirectory(), ioe);
        } finally {
            try {
                if (IndexWriter.isLocked(writer.getDirectory())) {
                    IndexWriter.unlock(writer.getDirectory());
                }
            } catch (IOException ioe) {
                log.error("Cannot unlock the index writer: " + writer.getDirectory(), ioe);
            }
        }
        writer = null;
    }

    protected void finalize() {
        closeIndexWriter();
    }

    public static void main(String args[]) throws Exception {
        String usage = "Usage: java com.rg.lucene.RMIServer [-port r] [-index i] [-driver d] [-url l] [-user u] [-password p]";
        if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
            log.error(usage);
            System.exit(0);
        }
        String index = "index.wordindex";
        String port = "";
        String driver = "";
        String url = "";
        String user = "";
        String password = "";
        for (int i = 0; i < args.length; i++) {
            if ("-port".equals(args[i])) {
                port = args[i + 1];
                i++;
            } else if ("-index".equals(args[i])) {
                index = args[i + 1];
                i++;
            } else if ("-driver".equals(args[i])) {
                driver = args[i + 1];
                i++;
            } else if ("-url".equals(args[i])) {
                url = args[i + 1];
                i++;
            } else if ("-user".equals(args[i])) {
                user = args[i + 1];
                i++;
            } else if ("-password".equals(args[i])) {
                password = args[i + 1];
            }
        }
        String name = "//localhost:" + port + "/luceneIndexor";
        Registry registry = LocateRegistry.createRegistry(Integer.parseInt(port));
        final RMIServer server = new RMIServer(index, false, driver, url, user, password);
        Remote obj = (Remote) UnicastRemoteObject.exportObject(server, 0);
        try {
            Naming.bind(name, obj);
        } catch (AlreadyBoundException abe) {
            log.error("Could not bind LuceneServer at " + name);
            System.exit(-1);
        }
        log.info("Starting LuceneServer at " + name);
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                if (server != null) server.closeIndexWriter();
            }
        });
    }

    /**
    * Functions to modify the database table.
    */
    private class DBFunctions {

        Connection connection;

        String driver;

        String url;

        String username;

        String password;

        /**
       * Construct the database storage method given a database connection.
       * @param dbcm
       */
        public DBFunctions(String driver, String url, String username, String password) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
            this.driver = driver;
            this.url = url;
            this.username = username;
            this.password = password;
            DriverManager.registerDriver((Driver) Class.forName(driver).newInstance());
        }

        /**
       * Return the database connection. If a connection can not be established, show a Restart.
       * @return the connection object.
       * @throws MessageStorageException
       */
        synchronized Connection getConnectionOrFail() throws Exception {
            if (connection == null) {
                try {
                    if (username == null) {
                        connection = DriverManager.getConnection(url);
                    } else {
                        connection = DriverManager.getConnection(url, username, password);
                    }
                } catch (Throwable e) {
                    throw new Exception("Could not connect to the message database.", e);
                }
            }
            return connection;
        }
    }

    /**
    * (non-Javadoc)
    * @see org.rg.lucene.SearcherConnection#relevanceRange(int)
    */
    public float[] relevanceRange() throws RemoteException, LuceneException {
        try {
            IndexSearcher searcher = getSearcher(false);
            if (searcher instanceof ModelIndexSearcher) {
                ModelIndexSearcher modelIndexSearcher = (ModelIndexSearcher) searcher;
                return modelIndexSearcher.relevanceRange();
            }
        } catch (IOException e) {
            log.error(e);
        }
        return null;
    }
}

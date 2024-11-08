package com.bugull.mongo.cache;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

/**
 * Cache(Map) contains IndexSearcher.
 * 
 * @author Frank Wen(xbwen@hotmail.com)
 */
public class IndexSearcherCache {

    private static final Logger logger = Logger.getLogger(IndexSearcherCache.class);

    private static IndexSearcherCache instance = new IndexSearcherCache();

    private Map<String, IndexSearcher> cache;

    private IndexSearcherCache() {
        cache = new ConcurrentHashMap<String, IndexSearcher>();
    }

    public static IndexSearcherCache getInstance() {
        return instance;
    }

    public IndexSearcher get(String name) {
        IndexSearcher searcher = null;
        if (cache.containsKey(name)) {
            searcher = cache.get(name);
        } else {
            synchronized (this) {
                if (cache.containsKey(name)) {
                    searcher = cache.get(name);
                } else {
                    IndexWriter writer = IndexWriterCache.getInstance().get(name);
                    IndexReader reader = null;
                    try {
                        reader = IndexReader.open(writer, true);
                    } catch (CorruptIndexException ex) {
                        logger.error("Something is wrong when open lucene IndexWriter", ex);
                    } catch (IOException ex) {
                        logger.error("Something is wrong when open lucene IndexWriter", ex);
                    }
                    searcher = new IndexSearcher(reader);
                    cache.put(name, searcher);
                }
            }
        }
        return searcher;
    }

    public Map<String, IndexSearcher> getAll() {
        return cache;
    }

    public void put(String name, IndexSearcher searcher) {
        cache.put(name, searcher);
    }
}

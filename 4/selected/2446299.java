package com.jaeksoft.searchlib.index;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import org.xml.sax.SAXException;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.cache.FieldCache;
import com.jaeksoft.searchlib.cache.FilterCache;
import com.jaeksoft.searchlib.cache.SearchCache;
import com.jaeksoft.searchlib.index.term.Term;
import com.jaeksoft.searchlib.index.term.TermEnum;
import com.jaeksoft.searchlib.index.term.TermFreqVector;
import com.jaeksoft.searchlib.query.MoreLikeThis;
import com.jaeksoft.searchlib.query.Query;
import com.jaeksoft.searchlib.request.AbstractRequest;
import com.jaeksoft.searchlib.request.DocumentsRequest;
import com.jaeksoft.searchlib.request.SearchRequest;
import com.jaeksoft.searchlib.result.AbstractResult;
import com.jaeksoft.searchlib.result.ResultDocument;
import com.jaeksoft.searchlib.schema.Schema;
import com.jaeksoft.searchlib.util.ReadWriteLock;
import com.jaeksoft.searchlib.util.XmlWriter;

public class IndexSingle extends IndexAbstract {

    private final ReadWriteLock rwl = new ReadWriteLock();

    private ReaderInterface reader = null;

    private WriterInterface writer = null;

    private volatile boolean online;

    private volatile boolean readonly;

    public IndexSingle(File configDir, IndexConfig indexConfig, boolean createIfNotExists) throws IOException, URISyntaxException, InstantiationException, IllegalAccessException, ClassNotFoundException, SearchLibException {
        super(indexConfig);
        online = true;
        readonly = false;
        if (indexConfig.getNativeOSSE() || true == Boolean.TRUE) {
            reader = new ReaderNativeOSSE(configDir, indexConfig);
            writer = new WriterNativeOSSE(configDir, indexConfig, (ReaderNativeOSSE) reader);
        } else {
            reader = ReaderLocal.fromConfig(configDir, indexConfig, createIfNotExists);
            writer = new WriterLocal(indexConfig, (ReaderLocal) reader);
        }
    }

    @Override
    public void close() {
        rwl.w.lock();
        try {
            if (reader != null) reader.close();
        } finally {
            rwl.w.unlock();
        }
    }

    @Override
    public void optimize() throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.r.lock();
        try {
            if (writer != null) writer.optimize();
        } finally {
            rwl.r.unlock();
        }
        reload();
    }

    @Override
    public boolean isOptimizing() {
        rwl.r.lock();
        try {
            if (writer != null) return writer.isOptimizing();
            return false;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public boolean deleteDocument(Schema schema, String uniqueField) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.r.lock();
        try {
            if (writer != null) return writer.deleteDocument(schema, uniqueField); else return false;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public int deleteDocuments(Schema schema, Collection<String> uniqueFields) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.r.lock();
        try {
            if (writer != null) return writer.deleteDocuments(schema, uniqueFields); else return 0;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public int deleteDocuments(SearchRequest query) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.r.lock();
        try {
            return writer.deleteDocuments(query);
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public void addBeforeUpdate(BeforeUpdateInterface beforeUpdate) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.r.lock();
        try {
            if (writer != null) writer.addBeforeUpdate(beforeUpdate);
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public boolean updateDocument(Schema schema, IndexDocument document) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.r.lock();
        try {
            if (writer != null) return writer.updateDocument(schema, document); else return false;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public int updateDocuments(Schema schema, Collection<IndexDocument> documents) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.r.lock();
        try {
            if (writer != null) return writer.updateDocuments(schema, documents); else return 0;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public void reload() throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.w.lock();
        try {
            if (reader != null) reader.reload();
        } finally {
            rwl.w.unlock();
        }
    }

    @Override
    public void swap(long version, boolean deleteOld) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        if (readonly) throw new SearchLibException("Index is read only");
        rwl.w.lock();
        try {
            if (reader != null) reader.swap(version, deleteOld);
        } finally {
            rwl.w.unlock();
        }
    }

    @Override
    public AbstractResult<?> request(AbstractRequest request) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.request(request);
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public String explain(SearchRequest searchRequest, int docId, boolean bHtml) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.explain(searchRequest, docId, bHtml);
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public ResultDocument[] documents(DocumentsRequest documentsRequest) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.documents(documentsRequest);
        } finally {
            rwl.r.unlock();
        }
        return null;
    }

    @Override
    public boolean sameIndex(ReaderInterface reader) {
        rwl.r.lock();
        try {
            if (reader == this) return true;
            if (reader == this.reader) return true;
            return false;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public IndexStatistics getStatistics() throws IOException {
        if (!online) throw new IOException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.getStatistics();
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public IndexSingle get(String name) {
        return this;
    }

    @Override
    public int getDocFreq(Term term) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.getDocFreq(term);
            return 0;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public TermEnum getTermEnum() throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.getTermEnum();
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public TermEnum getTermEnum(String field, String term) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.getTermEnum(field, term);
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public TermFreqVector getTermFreqVector(int docId, String field) throws IOException {
        if (!online) throw new IOException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.getTermFreqVector(docId, field);
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public void push(URI dest) throws SearchLibException {
        if (reader == null) return;
        boolean oldReadOnly;
        rwl.w.lock();
        try {
            oldReadOnly = readonly;
            readonly = true;
        } finally {
            rwl.w.unlock();
        }
        rwl.r.lock();
        try {
            reader.push(dest);
        } finally {
            rwl.r.unlock();
        }
        readonly = oldReadOnly;
    }

    @Override
    public boolean isOnline() {
        return online;
    }

    @Override
    public boolean isReadOnly() {
        return readonly;
    }

    @Override
    public void setOnline(boolean v) {
        online = v;
    }

    @Override
    public void setReadOnly(boolean v) {
        readonly = v;
    }

    @Override
    public long getVersion() {
        if (reader == null) return 0;
        return reader.getVersion();
    }

    public SearchCache getSearchCache() {
        rwl.r.lock();
        try {
            if (reader != null) if (reader instanceof ReaderLocal) return ((ReaderLocal) reader).getSearchCache();
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    public FilterCache getFilterCache() {
        rwl.r.lock();
        try {
            if (reader != null) if (reader instanceof ReaderLocal) return ((ReaderLocal) reader).getFilterCache();
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    public FieldCache getFieldCache() {
        rwl.r.lock();
        try {
            if (reader != null) if (reader instanceof ReaderLocal) return ((ReaderLocal) reader).getFieldCache();
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    protected void writeXmlConfigIndex(XmlWriter xmlWriter) throws SAXException {
        indexConfig.writeXmlConfig(xmlWriter);
    }

    @Override
    public Collection<?> getFieldNames() throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.getFieldNames();
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public Query rewrite(Query query) throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.rewrite(query);
            return null;
        } finally {
            rwl.r.unlock();
        }
    }

    @Override
    public MoreLikeThis getMoreLikeThis() throws SearchLibException {
        if (!online) throw new SearchLibException("Index is offline");
        rwl.r.lock();
        try {
            if (reader != null) return reader.getMoreLikeThis();
            return null;
        } finally {
            rwl.r.unlock();
        }
    }
}

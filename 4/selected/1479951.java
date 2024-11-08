package com.technosophos.rhizome.repository.lucene;

import static com.technosophos.rhizome.repository.lucene.LuceneElements.*;
import com.technosophos.rhizome.document.*;
import com.technosophos.rhizome.repository.DocumentIndexer;
import com.technosophos.rhizome.repository.RepositoryContext;
import com.technosophos.rhizome.repository.RepositoryManager;
import com.technosophos.rhizome.repository.RhizomeInitializationException;
import com.technosophos.rhizome.repository.RepositoryAccessException;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * Maintain the Index in Apache Lucene.
 * 
 * <p>This is an implementation of the DocumentIndexer that uses Apache Lucene as
 * a search index. The methods in this class can be used to prepare documents to be
 * indexed, index one or more RhizomeDocument objects, and perform basic maintenance
 * on the index.</p>
 * <p>The underlying Lucene code is supposed to be thread safe, but I have not
 * thoroughly tested it.</p>
 * @author mbutcher
 *
 */
public class LuceneIndexer implements DocumentIndexer {

    private RepositoryContext context;

    private String indexLocation = null;

    private String indexName = null;

    private LuceneIndexer() {
    }

    /**
	 * Use this constructor in conjunction with {@link #setConfiguration(RepositoryContext)}.
	 * @param indexName The name of the repository. (Index Name and repository name are identical)
	 */
    public LuceneIndexer(String indexName) {
        this(indexName, new RepositoryContext());
    }

    /**
	 * Main constructor.
	 * @param indexName The name of the repository.
	 * @param context
	 */
    public LuceneIndexer(String indexName, RepositoryContext context) {
        this.indexName = indexName;
        this.context = context;
        this.indexLocation = getIndexPath(this.indexName, context);
    }

    /**
	 * This is reusable.
	 */
    public boolean isReusable() {
        return true;
    }

    public String getIndexName() {
        return this.indexName;
    }

    /**
	 * Create and initialize an index.
	 * This must be done in cases where no documents will initially be added to the 
	 * index.
	 * @throws IOException If the path to the index cannot be found, or does not allow read/write.
	 */
    public void createIndex() throws IOException {
        IndexWriter indWriter = new IndexWriter(this.getIndexDir(), new StandardAnalyzer(), true);
        indWriter.close();
    }

    /**
	 * Completely rebuild the index.
	 * <p>This should run incrementally, and not interrupt traffic too much.</p>
	 * @param repman Initialized repository manager
	 * @param repoName The name of teh repository to reindex.
	 */
    public long reindex(RepositoryManager repman) throws RepositoryAccessException, RhizomeInitializationException {
        String[] all_docs = repman.getRepository(this.indexName).getAllDocumentIDs();
        int doc_count = 0;
        try {
            IndexWriter indWriter = new IndexWriter(this.getIndexDir(), new StandardAnalyzer(), true);
            Document doc;
            RhizomeDocument rd;
            for (int i = 0; i < all_docs.length; ++i) {
                rd = repman.getRepository(this.indexName).getDocument(all_docs[i]);
                Metadatum m = rd.getMetadatum("title");
                doc = this.prepareDocument(rd);
                indWriter.addDocument(doc);
            }
            indWriter.flush();
            indWriter.optimize();
            doc_count = indWriter.docCount();
        } catch (RhizomeParseException e) {
            throw new RepositoryAccessException("Could not parse document: " + e.getMessage());
        } catch (IOException e) {
            throw new RhizomeInitializationException("Lucene: " + e.getMessage());
        }
        return doc_count;
    }

    /**
	 * Index this document.
	 * <p>If the document is already in the index, the old entries will be overwritten
	 * by the new entry.</p>
	 */
    public void updateIndex(RhizomeDocument doc) throws RhizomeInitializationException {
        Document luceneDoc = this.prepareDocument(doc);
        Term id = new Term(LUCENE_DOCID_FIELD, doc.getDocumentID());
        try {
            IndexWriter indWrite = new IndexWriter(this.getIndexDir(), new StandardAnalyzer());
            indWrite.updateDocument(id, luceneDoc);
            indWrite.close();
        } catch (IOException ioe) {
            throw new RhizomeInitializationException("Could not write to index: " + ioe.getMessage());
        }
    }

    public void updateIndex(String docID, RepositoryManager repman) throws RhizomeParseException, RhizomeInitializationException, RepositoryAccessException {
        this.updateIndex(repman.getRepository(this.indexName).getDocument(docID));
    }

    /**
	 * Delete a document from the index.
	 * <p>
	 * This always returns true. The underlying function does not give any information
	 * about success, and deleted documents are buffered (though not returned in searches)
	 * until the delete buffer is full.</p>
	 * @return boolean true all the time
	 * @param documentID for document to be deleted
	 * @see RhizomeDocument.getDocumentID()
	 */
    public boolean deleteFromIndex(String docID) throws RhizomeInitializationException {
        Term id = new Term(LUCENE_DOCID_FIELD, docID);
        try {
            IndexWriter indWrite = new IndexWriter(this.getIndexDir(), new StandardAnalyzer());
            indWrite.deleteDocuments(id);
            indWrite.flush();
            indWrite.close();
        } catch (IOException ioe) {
            throw new RhizomeInitializationException("Could not delete doc from index: " + ioe.getMessage());
        }
        return true;
    }

    /**
	 * This takes a RhizomeDocument and prepares it to be indexed.
	 * <p>This will index the metadata and relations. It will index the Data (the body)
	 * iff the isIndexible() flag is set, and the document is marked as text, (X)HTML, or XML.</p>
	 * 
	 * <p>Lucene has its own document format. This uses that format.</p>
	 * @param doc the Rhizome document.
	 * @return document suitable for Lucene indexing.
	 * @see com.technosophos.rhizome.document.RhizomeData
	 * @see com.technosophos.rhizome.document.RhizomeDocument
	 */
    public Document prepareDocument(RhizomeDocument doc) {
        Document ldoc = new Document();
        Field docID_field = new Field(LUCENE_DOCID_FIELD, doc.getDocumentID(), Field.Store.YES, Field.Index.UN_TOKENIZED);
        ldoc.add(docID_field);
        Iterator<Metadatum> md = doc.getMetadata().iterator();
        Field mfield;
        Metadatum m;
        while (md.hasNext()) {
            m = md.next();
            Iterator<String> vals = m.getValues().iterator();
            while (vals.hasNext()) {
                mfield = new Field(m.getName(), vals.next(), Field.Store.YES, Field.Index.TOKENIZED);
                ldoc.add(mfield);
            }
        }
        Iterator<Relation> relations = doc.getRelations().iterator();
        Relation r;
        StringBuffer sb;
        while (relations.hasNext()) {
            sb = new StringBuffer();
            r = relations.next();
            sb.append(r.getDocID());
            sb.append(RELATION_SEPARATOR);
            if (r.hasRelationType()) sb.append(r.getRelationType());
            mfield = new Field(LUCENE_RELATION_FIELD, sb.toString(), Field.Store.YES, Field.Index.TOKENIZED);
            ldoc.add(mfield);
        }
        Field bodyField;
        RhizomeData data = doc.getData();
        String mimetype = data.getMimeType();
        if (data.isIndexible()) {
            if (RhizomeData.MIME_PLAINTEXT.equals(mimetype)) {
                bodyField = new Field(LUCENE_BODY_FIELD, data.toString(), Field.Store.NO, Field.Index.TOKENIZED);
                ldoc.add(bodyField);
            } else if (data.isTaggedText()) {
                bodyField = new Field(LUCENE_BODY_FIELD, FastTagStripper.strip(data.toString()), Field.Store.NO, Field.Index.TOKENIZED);
                ldoc.add(bodyField);
            }
        }
        Iterator<Extension> exts = doc.getExtensions().iterator();
        Extension ext;
        while (exts.hasNext()) {
            ext = exts.next();
            if (ext.isIndexible()) {
                mfield = new Field(LUCENE_EXTENSION_FIELD, ext.toIndexibleString(), Field.Store.NO, Field.Index.TOKENIZED);
                ldoc.add(mfield);
            }
        }
        return ldoc;
    }

    public RepositoryContext getConfiguration() {
        return this.context;
    }

    public void setConfiguration(RepositoryContext context) {
        this.context = context;
        this.indexLocation = getIndexPath(this.indexName, context);
    }

    /**
	 * Get the index directory.
	 * <p>Also, make sure index directory can be found and used.
	 * This is public to allow applications to test the index path before it is 
	 * lazily instantiated later.</p>
	 * @param pathname
	 * @throws FileNotFoundException if the directory does not exist.
	 * @throws IOException if the directory is not readible or writeable
	 */
    public File getIndexDir() throws FileNotFoundException, IOException {
        if (this.indexLocation == null) throw new IOException("Index dir not specified.");
        String pathname = this.indexLocation;
        File indexDir = new File(pathname);
        if (!indexDir.exists()) throw new FileNotFoundException("Index Dir not found: " + pathname);
        if (!indexDir.isDirectory()) throw new IOException("Index Dir is not a directory: " + pathname);
        if (!indexDir.canRead() || !indexDir.canWrite()) throw new IOException("Index Dir must have read/write access: " + pathname);
        return indexDir;
    }

    /**
	 * Get the path to a named index.
	 * This path could be relative -- it depends on the path information passed in from
	 * the Context.
	 * @param name
	 * @param cxt
	 * @return
	 */
    public static String getIndexPath(String name, RepositoryContext cxt) {
        if (!cxt.hasKey(LUCENE_INDEX_PATH_PARAM)) return null;
        StringBuilder sb = new StringBuilder();
        sb.append(cxt.getParam(LUCENE_INDEX_PATH_PARAM));
        if (sb.lastIndexOf(File.separator) != sb.length() - 1) sb.append(File.separatorChar);
        sb.append(name);
        return sb.toString();
    }
}

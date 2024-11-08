package eu.popeye.middleware.dataSearch;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.LockObtainFailedException;
import org.jdom.Element;
import eu.popeye.middleware.dataSharing.Metadata;
import eu.popeye.middleware.dataSharing.common.util.DocumentHandler;

public class Index {

    IndexWriter writer = null;

    IndexSearcher searcher = null;

    String indexDir;

    String indexName;

    RemoteQueryBroker remoteQueryBroker = null;

    public static final String INTERNAL_PATH = "_" + Metadata.PATH + "_";

    public Index(String indexDir, RemoteQueryBroker remoteQueryBroker) {
        this.remoteQueryBroker = remoteQueryBroker;
        this.indexDir = indexDir;
        File dir = new File(indexDir);
        this.indexName = dir.getName();
        if (!(dir.exists() && dir.isDirectory())) {
            try {
                Analyzer analyzer = new WhitespaceAnalyzer();
                boolean createFlag = true;
                writer = new IndexWriter(indexDir, analyzer, createFlag);
                writer.optimize();
                writer.close();
                System.out.println("Index created sucessfully");
            } catch (CorruptIndexException e) {
                e.printStackTrace();
            } catch (LockObtainFailedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                writer = new IndexWriter(indexDir, new WhitespaceAnalyzer(), false);
                System.out.println("Index already exists. It contains " + writer.docCount() + " docs");
                writer.close();
            } catch (CorruptIndexException e) {
                e.printStackTrace();
            } catch (LockObtainFailedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.updateBroker();
    }

    /**
	 * Construct a new Lucene Documnet and add it to the index
	 * @param metadata
	 */
    public synchronized void add(Metadata metadata) {
        System.out.println("INDEX.add " + metadata.get(Metadata.PATH));
        Document indexableDocument = new Document();
        if (metadata != null) {
            this.recurseXml(metadata.getMetadata().getRootElement(), "", indexableDocument);
        }
        try {
            Analyzer analyzer = new WhitespaceAnalyzer();
            boolean createFlag = false;
            writer = new IndexWriter(indexDir, analyzer, createFlag);
            writer.addDocument(indexableDocument);
            writer.flush();
            writer.optimize();
            writer.close();
            this.updateBroker();
        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * recursively visit all the metadata nodes and add their value to the metadata Document (which will be 
	 * later added to the index)
	 * @param element
	 * @param nodeName
	 * @param metadataDocument
	 */
    private void recurseXml(Element element, String nodeName, Document indexableDocument) {
        List elementList = element.getChildren();
        Iterator listIterator = elementList.iterator();
        while (listIterator.hasNext()) {
            Element currentElement = (Element) listIterator.next();
            String xmlTag = nodeName + currentElement.getName();
            String value = currentElement.getAttribute("value").getValue();
            indexableDocument.add(new Field(xmlTag, value, Field.Store.YES, Field.Index.TOKENIZED));
            if (xmlTag.equals(Metadata.PATH)) {
                indexableDocument.add(new Field(INTERNAL_PATH, value, Field.Store.YES, Field.Index.UN_TOKENIZED));
            }
            recurseXml(currentElement, xmlTag + "/", indexableDocument);
        }
    }

    /**
	 * Replace an existing metadata Document in th current index for an updated version
	 * The xmlTag which we use to find out the old version of the metadata Document is the "id" tag,
	 * since it must be unique between documents. 
	 * @param metadata
	 */
    public synchronized void update(Metadata metadata) {
        String docPath = (String) metadata.get(Metadata.PATH);
        this.delete(new Term("path", docPath));
        this.add(metadata);
    }

    /**
	 * Delete a metadata document (or documents) mathcing the parameter term from the index
	 * @param term
	 */
    public synchronized void delete(Term term) {
        try {
            Analyzer analyzer = new WhitespaceAnalyzer();
            boolean createFlag = false;
            writer = new IndexWriter(indexDir, analyzer, createFlag);
            int count = writer.docCount();
            writer.deleteDocuments(term);
            writer.flush();
            writer.optimize();
            writer.close();
            writer = new IndexWriter(indexDir, analyzer, createFlag);
            writer.close();
            this.updateBroker();
        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (LockObtainFailedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Update and export index searcher so local and remote queries give up to date results
	 *
	 */
    public void updateBroker() {
        try {
            if (this.searcher != null) {
                this.searcher.close();
            }
            this.searcher = new IndexSearcher(indexDir);
            this.remoteQueryBroker.setSearcher(this.searcher);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

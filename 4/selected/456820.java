package de.ios.framework.httpd;

import de.ios.framework.basic.FIFO;
import de.ios.framework.basic.Debug;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Class to load and cache documents loaded from the HTTPD.
 */
class DocumentCache {

    Object[] documentBases;

    Hashtable documents = new Hashtable();

    FIFO fifo = new FIFO();

    /** Maximum size of document to store in cache. */
    public static long MAX_CACHABLE_DOCSIZE = 1024 * 1024;

    /** Maximum size of cache. */
    public static long MAX_CACHESIZE = 10 * 1024 * 1024;

    /** Timeout in milliseconds a document is cached. */
    public static long CACHE_TIMEOUT = 120 * 1000;

    long actualCacheSize = 0;

    DocumentCache(String[] documentBaseNames) throws IOException {
        documentBases = new Object[documentBaseNames.length];
        for (int i = 0; i < documentBaseNames.length; i++) {
            File docBase = new File(documentBaseNames[i]);
            if (docBase.isDirectory()) documentBases[i] = docBase; else if (docBase.isFile()) documentBases[i] = new ZipFile(docBase); else {
                documentBases[i] = null;
                Debug.println(Debug.WARNING, this, "Warning: " + documentBaseNames[i] + " not found.");
            }
        }
    }

    /**
     * Resets the cache. All cached documents are removed.
     */
    public void reset() {
        Debug.println(Debug.BIGINFO, this, "Removing all documents");
        documents.clear();
        fifo.clear();
        actualCacheSize = 0;
    }

    /**
     * Remove one document from cache.
     */
    public void removeDocument(Document d) {
        Debug.println(Debug.BIGINFO, this, "Removing document " + d.documentName);
        documents.remove(d.documentName);
        actualCacheSize -= d.content.length;
        fifo.remove(d);
    }

    /**
     * Add the doucment to the cache.
     * It is ensured that the cache-size is below MAX_CACHESIZE.
     */
    public void addDocument(Document d) {
        if (d.content.length <= MAX_CACHABLE_DOCSIZE) {
            cleanUp(d.content.length);
            actualCacheSize += d.content.length;
            Debug.println(Debug.BIGINFO, this, "Adding document " + d.documentName);
            documents.put(d.documentName, d);
            fifo.put(d);
        }
    }

    /**
     * Ensure that the cache has some space left.<br>
     * First all documents with a lifetime > CACHE_TIMEOUT are removed from cache.
     * If more space is needed, the oldest contents are removed.
     * @param 
     */
    public void cleanUp(long neededSize) {
        if (neededSize >= MAX_CACHESIZE) reset(); else {
            if (CACHE_TIMEOUT > 0) {
                Document d;
                long timeout = System.currentTimeMillis() - CACHE_TIMEOUT;
                while (fifo.size() > 0) {
                    d = (Document) fifo.peek();
                    if (d.lastAccess > timeout) break;
                    removeDocument(d);
                }
            }
            while ((actualCacheSize + neededSize) > MAX_CACHESIZE) {
                if (fifo.size() == 0) {
                    reset();
                    return;
                }
                removeDocument((Document) fifo.peek());
            }
        }
    }

    public Document fetchDocument(String documentName) {
        Document doc = (Document) documents.get(documentName);
        if (doc != null) {
            if (CACHE_TIMEOUT > 0) if (doc.lastAccess < (System.currentTimeMillis() - CACHE_TIMEOUT)) doc = null;
            if (doc != null) {
                fifo.remove(doc);
                fifo.put(doc);
            }
        }
        if (doc != null) {
            doc.touch();
            return doc;
        }
        if (!documentName.startsWith("/")) return null;
        InputStream in = null;
        boolean defaultPage;
        for (int i = 0; i < documentBases.length; i++) {
            defaultPage = false;
            if (documentBases[i] instanceof ZipFile) {
                ZipFile zipArchive = (ZipFile) documentBases[i];
                String relativeDocumentName = documentName.substring(1, documentName.length());
                ZipEntry entry = zipArchive.getEntry(relativeDocumentName);
                if (entry.getSize() == 0) entry = zipArchive.getEntry(relativeDocumentName + "/index.html");
                if (entry == null) continue;
                try {
                    in = new BufferedInputStream(zipArchive.getInputStream(entry));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    continue;
                }
            } else if (documentBases[i] instanceof File) {
                try {
                    File docBase = (File) documentBases[i];
                    File docFile = new File(docBase, documentName);
                    if (docFile.isDirectory()) {
                        docFile = new File(docFile, "index.html");
                        defaultPage = true;
                    }
                    if (!docFile.exists()) continue;
                    if (!docFile.getCanonicalPath().startsWith(docBase.getAbsolutePath())) {
                        Debug.println(Debug.ERROR, this, "Security Warning:");
                        Debug.println(Debug.ERROR, this, "Illegal attempt to access document: " + docFile.getCanonicalPath());
                        Debug.println(Debug.ERROR, this, "(Document not in '" + docBase.getAbsolutePath() + "')");
                        continue;
                    }
                    in = new BufferedInputStream(new FileInputStream(docFile));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    continue;
                }
            }
            if (in != null) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) > 0) out.write(buffer, 0, len);
                    if (defaultPage) doc = new Document(documentName, out.toByteArray(), Document.TEXT_HTML); else doc = new Document(documentName, out.toByteArray());
                    addDocument(doc);
                    return doc;
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    continue;
                }
            }
        }
        return null;
    }
}

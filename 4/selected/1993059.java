package org.eclipse.help.internal.search;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.util.HelpProperties;
import org.eclipse.help.internal.protocols.HelpURLConnection;
import org.eclipse.help.internal.protocols.HelpURLStreamHandler;
import org.eclipse.help.internal.toc.TocFileProvider;
import org.eclipse.help.internal.toc.TocManager;
import org.eclipse.help.internal.util.ResourceLocator;
import org.eclipse.help.search.ISearchIndex;
import org.eclipse.help.search.LuceneSearchParticipant;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Text search index. Documents added to this index can than be searched against a search query.
 */
public class SearchIndex implements ISearchIndex {

    private IndexReader ir;

    private IndexWriter iw;

    private File indexDir;

    private String locale;

    private String relativePath;

    private TocManager tocManager;

    private AnalyzerDescriptor analyzerDescriptor;

    private PluginVersionInfo docPlugins;

    private HelpProperties indexedDocs;

    public static final String INDEXED_CONTRIBUTION_INFO_FILE = "indexed_contributions";

    public static final String INDEXED_DOCS_FILE = "indexed_docs";

    public static final String DEPENDENCIES_VERSION_FILENAME = "indexed_dependencies";

    public static final String DEPENDENCIES_KEY_LUCENE = "lucene";

    public static final String DEPENDENCIES_KEY_ANALYZER = "analyzer";

    private static final String LUCENE_BUNDLE_ID = "org.apache.lucene";

    private static final String FIELD_NAME = "name";

    private static final String FIELD_INDEX_ID = "index_path";

    private File inconsistencyFile;

    private HTMLSearchParticipant htmlSearchParticipant;

    private IndexSearcher searcher;

    private Object searcherCreateLock = new Object();

    private HelpProperties dependencies;

    private boolean closed = false;

    private Collection searches = new ArrayList();

    private FileLock lock;

    /**
	 * Constructor.
	 * 
	 * @param locale
	 *            the locale this index uses
	 * @param analyzerDesc
	 *            the analyzer used to index
	 */
    public SearchIndex(String locale, AnalyzerDescriptor analyzerDesc, TocManager tocManager) {
        this(new File(HelpBasePlugin.getConfigurationDirectory(), "index/" + locale), locale, analyzerDesc, tocManager, null);
    }

    /**
	 * Alternative constructor that provides index directory.
	 * 
	 * @param indexDir
	 * @param locale
	 * @param analyzerDesc
	 * @param tocManager
	 * @since 3.1
	 */
    public SearchIndex(File indexDir, String locale, AnalyzerDescriptor analyzerDesc, TocManager tocManager, String relativePath) {
        this.locale = locale;
        this.analyzerDescriptor = analyzerDesc;
        this.tocManager = tocManager;
        this.indexDir = indexDir;
        this.relativePath = relativePath;
        inconsistencyFile = new File(indexDir.getParentFile(), locale + ".inconsistent");
        htmlSearchParticipant = new HTMLSearchParticipant(indexDir.getAbsolutePath());
        if (!exists()) {
            try {
                if (tryLock()) {
                    try {
                        unzipProductIndex();
                    } finally {
                        releaseLock();
                    }
                }
            } catch (OverlappingFileLockException ofle) {
            }
        }
    }

    /**
	 * Indexes one document from a stream. Index has to be open and close outside of this method
	 * 
	 * @param name
	 *            the document identifier (could be a URL)
	 * @param url
	 *            the URL of the document
	 * @return IStatus
	 */
    public IStatus addDocument(String name, URL url) {
        try {
            Document doc = new Document();
            doc.add(new Field(FIELD_NAME, name, Field.Store.YES, Field.Index.UN_TOKENIZED));
            addExtraFields(doc);
            String pluginId = LocalSearchManager.getPluginId(name);
            if (relativePath != null) {
                doc.add(new Field(FIELD_INDEX_ID, relativePath, Field.Store.YES, Field.Index.UN_TOKENIZED));
            }
            LuceneSearchParticipant participant = null;
            HelpURLConnection urlc = new HelpURLConnection(url);
            String id = urlc.getValue("id");
            String pid = urlc.getValue("participantId");
            if (pid != null) participant = BaseHelpSystem.getLocalSearchManager().getGlobalParticipant(pid);
            if (participant == null) participant = BaseHelpSystem.getLocalSearchManager().getParticipant(pluginId, name);
            if (participant != null) {
                IStatus status = participant.addDocument(this, pluginId, name, url, id, doc);
                if (status.getSeverity() == IStatus.OK) {
                    String filters = doc.get("filters");
                    indexedDocs.put(name, filters != null ? filters : "0");
                    if (id != null) doc.add(new Field("id", id, Field.Store.YES, Field.Index.NO));
                    if (pid != null) doc.add(new Field("participantId", pid, Field.Store.YES, Field.Index.NO));
                    iw.addDocument(doc);
                }
                return status;
            }
            IStatus status = htmlSearchParticipant.addDocument(this, pluginId, name, url, id, doc);
            if (status.getSeverity() == IStatus.OK) {
                String filters = doc.get("filters");
                indexedDocs.put(name, filters != null ? filters : "0");
                iw.addDocument(doc);
            }
            return status;
        } catch (IOException e) {
            return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR, "IO exception occurred while adding document " + name + " to index " + indexDir.getAbsolutePath() + ".", e);
        } catch (Exception e) {
            return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR, "An unexpected internal error occurred while adding document " + name + " to index " + indexDir.getAbsolutePath() + ".", e);
        }
    }

    /**
	 * Add any extra fields that need to be added to this document. Subclasses
	 * should override to add more fields.
	 * 
	 * @param doc the document to add fields to
	 */
    protected void addExtraFields(Document doc) {
    }

    /**
	 * Starts additions. To be called before adding documents.
	 */
    public synchronized boolean beginAddBatch(boolean firstOperation) {
        try {
            if (iw != null) {
                iw.close();
            }
            boolean create = false;
            if (!indexDir.exists() || !isLuceneCompatible() || !isAnalyzerCompatible() || inconsistencyFile.exists() && firstOperation) {
                create = true;
                indexDir.mkdirs();
                if (!indexDir.exists()) return false;
            }
            indexedDocs = new HelpProperties(INDEXED_DOCS_FILE, indexDir);
            indexedDocs.restore();
            setInconsistent(true);
            iw = new IndexWriter(indexDir, analyzerDescriptor.getAnalyzer(), create);
            iw.setMergeFactor(20);
            iw.setMaxFieldLength(1000000);
            return true;
        } catch (IOException e) {
            HelpBasePlugin.logError("Exception occurred in search indexing at beginAddBatch.", e);
            return false;
        }
    }

    /**
	 * Starts deletions. To be called before deleting documents.
	 */
    public synchronized boolean beginDeleteBatch() {
        try {
            if (ir != null) {
                ir.close();
            }
            indexedDocs = new HelpProperties(INDEXED_DOCS_FILE, indexDir);
            indexedDocs.restore();
            setInconsistent(true);
            ir = IndexReader.open(indexDir);
            return true;
        } catch (IOException e) {
            HelpBasePlugin.logError("Exception occurred in search indexing at beginDeleteBatch.", e);
            return false;
        }
    }

    /**
	 * Starts deletions. To be called before deleting documents.
	 */
    public synchronized boolean beginRemoveDuplicatesBatch() {
        try {
            if (ir != null) {
                ir.close();
            }
            ir = IndexReader.open(indexDir);
            return true;
        } catch (IOException e) {
            HelpBasePlugin.logError("Exception occurred in search indexing at beginDeleteBatch.", e);
            return false;
        }
    }

    /**
	 * Deletes a single document from the index.
	 * 
	 * @param name -
	 *            document name
	 * @return IStatus
	 */
    public IStatus removeDocument(String name) {
        Term term = new Term(FIELD_NAME, name);
        try {
            ir.deleteDocuments(term);
            indexedDocs.remove(name);
        } catch (IOException e) {
            return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR, "IO exception occurred while removing document " + name + " from index " + indexDir.getAbsolutePath() + ".", e);
        }
        return Status.OK_STATUS;
    }

    /**
	 * Finish additions. To be called after adding documents.
	 */
    public synchronized boolean endAddBatch(boolean optimize, boolean lastOperation) {
        try {
            if (iw == null) return false;
            if (optimize) iw.optimize();
            iw.close();
            iw = null;
            getDocPlugins().save();
            saveDependencies();
            if (lastOperation) {
                indexedDocs.save();
                indexedDocs = null;
                setInconsistent(false);
            }
            if (searcher != null) {
                searcher.close();
                searcher = null;
            }
            return true;
        } catch (IOException e) {
            HelpBasePlugin.logError("Exception occurred in search indexing at endAddBatch.", e);
            return false;
        }
    }

    /**
	 * Finish deletions. To be called after deleting documents.
	 */
    public synchronized boolean endDeleteBatch() {
        try {
            if (ir == null) return false;
            ir.close();
            ir = null;
            indexedDocs.save();
            indexedDocs = null;
            getDocPlugins().save();
            saveDependencies();
            if (searcher != null) {
                searcher.close();
                searcher = null;
            }
            return true;
        } catch (IOException e) {
            HelpBasePlugin.logError("Exception occurred in search indexing at endDeleteBatch.", e);
            return false;
        }
    }

    /**
	 * Finish deletions. To be called after deleting documents.
	 */
    public synchronized boolean endRemoveDuplicatesBatch() {
        try {
            if (ir == null) return false;
            ir.close();
            ir = null;
            indexedDocs.save();
            indexedDocs = null;
            getDocPlugins().save();
            saveDependencies();
            setInconsistent(false);
            return true;
        } catch (IOException e) {
            HelpBasePlugin.logError("Exception occurred in search indexing at endDeleteBatch.", e);
            return false;
        }
    }

    /**
	 * If
	 * 
	 * @param dirs
	 * @param monitor
	 * @return Map. Keys are /pluginid/href of all merged Docs. Values are null for added document,
	 *         or String[] of indexIds with duplicates of the document
	 */
    public Map merge(PluginIndex[] pluginIndexes, IProgressMonitor monitor) {
        ArrayList dirList = new ArrayList(pluginIndexes.length);
        Map mergedDocs = new HashMap();
        for (int p = 0; p < pluginIndexes.length; p++) {
            List indexIds = pluginIndexes[p].getIDs();
            List indexPaths = pluginIndexes[p].getPaths();
            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            for (int i = 0; i < indexPaths.size(); i++) {
                String indexId = (String) indexIds.get(i);
                String indexPath = (String) indexPaths.get(i);
                try {
                    dirList.add(FSDirectory.getDirectory(indexPath, false));
                } catch (IOException ioe) {
                    HelpBasePlugin.logError("Help search indexing directory could not be created for directory " + indexPath, ioe);
                    continue;
                }
                HelpProperties prebuiltDocs = new HelpProperties(INDEXED_DOCS_FILE, new File(indexPath));
                prebuiltDocs.restore();
                Set prebuiltHrefs = prebuiltDocs.keySet();
                for (Iterator it = prebuiltHrefs.iterator(); it.hasNext(); ) {
                    String href = (String) it.next();
                    if (i == 0) {
                        mergedDocs.put(href, null);
                    } else {
                        if (mergedDocs.containsKey(href)) {
                            String[] dups = (String[]) mergedDocs.get(href);
                            if (dups == null) {
                                mergedDocs.put(href, new String[] { indexId });
                            } else {
                                String[] newDups = new String[dups.length + 1];
                                System.arraycopy(dups, 0, newDups, 0, dups.length);
                                newDups[dups.length] = indexId;
                                mergedDocs.put(href, newDups);
                            }
                        } else {
                            mergedDocs.put(href, null);
                        }
                    }
                }
            }
        }
        for (Iterator it = mergedDocs.keySet().iterator(); it.hasNext(); ) {
            indexedDocs.put(it.next(), "0");
        }
        Directory[] luceneDirs = (Directory[]) dirList.toArray(new Directory[dirList.size()]);
        try {
            iw.addIndexes(luceneDirs);
        } catch (IOException ioe) {
            HelpBasePlugin.logError("Merging search indexes failed.", ioe);
            return new HashMap();
        }
        return mergedDocs;
    }

    public IStatus removeDuplicates(String name, String[] index_paths) {
        TermDocs hrefDocs = null;
        TermDocs indexDocs = null;
        Term hrefTerm = new Term(FIELD_NAME, name);
        try {
            for (int i = 0; i < index_paths.length; i++) {
                Term indexTerm = new Term(FIELD_INDEX_ID, index_paths[i]);
                if (i == 0) {
                    hrefDocs = ir.termDocs(hrefTerm);
                    indexDocs = ir.termDocs(indexTerm);
                } else {
                    hrefDocs.seek(hrefTerm);
                    indexDocs.seek(indexTerm);
                }
                removeDocuments(hrefDocs, indexDocs);
            }
        } catch (IOException ioe) {
            return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR, "IO exception occurred while removing duplicates of document " + name + " from index " + indexDir.getAbsolutePath() + ".", ioe);
        } finally {
            if (hrefDocs != null) {
                try {
                    hrefDocs.close();
                } catch (IOException e) {
                }
            }
            if (indexDocs != null) {
                try {
                    indexDocs.close();
                } catch (IOException e) {
                }
            }
        }
        return Status.OK_STATUS;
    }

    /**
	 * Removes documents containing term1 and term2
	 * 
	 * @param doc1
	 * @param docs2
	 * @throws IOException
	 */
    private void removeDocuments(TermDocs doc1, TermDocs docs2) throws IOException {
        if (!doc1.next()) {
            return;
        }
        if (!docs2.next()) {
            return;
        }
        while (true) {
            if (doc1.doc() < docs2.doc()) {
                if (!doc1.skipTo(docs2.doc())) {
                    if (!doc1.next()) {
                        return;
                    }
                }
            } else if (doc1.doc() > docs2.doc()) {
                if (!docs2.skipTo(doc1.doc())) {
                    if (!doc1.next()) {
                        return;
                    }
                }
            }
            if (doc1.doc() == docs2.doc()) {
                ir.deleteDocument(doc1.doc());
                if (!doc1.next()) {
                    return;
                }
                if (!docs2.next()) {
                    return;
                }
            }
        }
    }

    /**
	 * Checks if index exists and is usable.
	 * 
	 * @return true if index exists
	 */
    public boolean exists() {
        return indexDir.exists() && !isInconsistent();
    }

    /**
	 * Performs a query search on this index
	 */
    public void search(ISearchQuery searchQuery, ISearchHitCollector collector) throws QueryTooComplexException {
        try {
            if (closed) return;
            registerSearch(Thread.currentThread());
            if (closed) return;
            QueryBuilder queryBuilder = new QueryBuilder(searchQuery.getSearchWord(), analyzerDescriptor);
            Query luceneQuery = queryBuilder.getLuceneQuery(searchQuery.getFieldNames(), searchQuery.isFieldSearch());
            String highlightTerms = queryBuilder.gethighlightTerms();
            if (luceneQuery != null) {
                if (searcher == null) {
                    openSearcher();
                }
                Hits hits = searcher.search(luceneQuery);
                collector.addHits(LocalSearchManager.asList(hits), highlightTerms);
            }
        } catch (BooleanQuery.TooManyClauses tmc) {
            throw new QueryTooComplexException();
        } catch (QueryTooComplexException qe) {
            throw qe;
        } catch (Exception e) {
            HelpBasePlugin.logError("Exception occurred performing search for: " + searchQuery.getSearchWord() + ".", e);
        } finally {
            unregisterSearch(Thread.currentThread());
        }
    }

    public String getLocale() {
        return locale;
    }

    /**
	 * Returns the list of all the plugins in this session that have declared a help contribution.
	 */
    public PluginVersionInfo getDocPlugins() {
        if (docPlugins == null) {
            Set totalIds = new HashSet();
            IExtensionRegistry registry = Platform.getExtensionRegistry();
            IExtensionPoint extensionPoint = registry.getExtensionPoint(TocFileProvider.EXTENSION_POINT_ID_TOC);
            IExtension[] extensions = extensionPoint.getExtensions();
            for (int i = 0; i < extensions.length; ++i) {
                try {
                    totalIds.add(extensions[i].getNamespaceIdentifier());
                } catch (InvalidRegistryObjectException e) {
                }
            }
            Collection additionalPluginIds = BaseHelpSystem.getLocalSearchManager().getPluginsWithSearchParticipants();
            totalIds.addAll(additionalPluginIds);
            docPlugins = new PluginVersionInfo(INDEXED_CONTRIBUTION_INFO_FILE, totalIds, indexDir, !exists());
        }
        return docPlugins;
    }

    /**
	 * Sets the list of all plug-ns in this session. This method is used for external indexer.
	 * 
	 * @param docPlugins
	 */
    public void setDocPlugins(PluginVersionInfo docPlugins) {
        this.docPlugins = docPlugins;
    }

    /**
	 * We use HelpProperties, but a list would suffice. We only need the key values.
	 * 
	 * @return HelpProperties, keys are URLs of indexed documents
	 */
    public HelpProperties getIndexedDocs() {
        HelpProperties indexedDocs = new HelpProperties(INDEXED_DOCS_FILE, indexDir);
        if (exists()) indexedDocs.restore();
        return indexedDocs;
    }

    /**
	 * Gets properties with versions of Lucene plugin and Analyzer used in existing index
	 */
    private HelpProperties getDependencies() {
        if (dependencies == null) {
            dependencies = new HelpProperties(DEPENDENCIES_VERSION_FILENAME, indexDir);
            dependencies.restore();
        }
        return dependencies;
    }

    private boolean isLuceneCompatible() {
        String usedLuceneVersion = getDependencies().getProperty(DEPENDENCIES_KEY_LUCENE);
        return isLuceneCompatible(usedLuceneVersion);
    }

    public boolean isLuceneCompatible(String luceneVersion) {
        if (luceneVersion == null) return false;
        String currentLuceneVersion = "";
        Bundle luceneBundle = Platform.getBundle(LUCENE_BUNDLE_ID);
        if (luceneBundle != null) {
            currentLuceneVersion += (String) luceneBundle.getHeaders().get(Constants.BUNDLE_VERSION);
        }
        if (currentLuceneVersion.equals(luceneVersion)) return true;
        Version version = new Version(currentLuceneVersion);
        Version currentVersion = new Version(luceneVersion);
        return version.getMajor() == currentVersion.getMajor() && version.getMinor() == currentVersion.getMinor() && version.getMicro() == currentVersion.getMicro();
    }

    private boolean isAnalyzerCompatible() {
        String usedAnalyzer = getDependencies().getProperty(DEPENDENCIES_KEY_ANALYZER);
        return isAnalyzerCompatible(usedAnalyzer);
    }

    public boolean isAnalyzerCompatible(String analyzerId) {
        if (analyzerId == null) {
            analyzerId = "";
        }
        return analyzerDescriptor.isCompatible(analyzerId);
    }

    /**
	 * Saves Lucene version and analyzer identifier to a file.
	 */
    private void saveDependencies() {
        getDependencies().put(DEPENDENCIES_KEY_ANALYZER, analyzerDescriptor.getId());
        Bundle luceneBundle = Platform.getBundle(LUCENE_BUNDLE_ID);
        if (luceneBundle != null) {
            String luceneBundleVersion = "" + luceneBundle.getHeaders().get(Constants.BUNDLE_VERSION);
            getDependencies().put(DEPENDENCIES_KEY_LUCENE, luceneBundleVersion);
        } else {
            getDependencies().put(DEPENDENCIES_KEY_LUCENE, "");
        }
        getDependencies().save();
    }

    /**
	 * @return Returns true if index has been left in inconsistent state If analyzer has changed to
	 *         incompatible one, index is treated as inconsistent as well.
	 */
    public boolean isInconsistent() {
        if (inconsistencyFile.exists()) {
            return true;
        }
        return !isLuceneCompatible() || !isAnalyzerCompatible();
    }

    /**
	 * Writes or deletes inconsistency flag file
	 */
    public void setInconsistent(boolean inconsistent) {
        if (inconsistent) {
            try {
                FileOutputStream fos = new FileOutputStream(inconsistencyFile);
                fos.close();
            } catch (IOException ioe) {
            }
        } else inconsistencyFile.delete();
    }

    public void openSearcher() throws IOException {
        synchronized (searcherCreateLock) {
            if (searcher == null) {
                searcher = new IndexSearcher(indexDir.getAbsolutePath());
            }
        }
    }

    /**
	 * Closes IndexReader used by Searcher. Should be called on platform shutdown, or when TOCs have
	 * changed when no more reading from this index is to be performed.
	 */
    public void close() {
        closed = true;
        synchronized (searches) {
            while (searches.size() > 0) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                }
            }
            if (searcher != null) {
                try {
                    searcher.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
	 * Finds and unzips prebuild index specified in preferences
	 */
    private void unzipProductIndex() {
        String indexPluginId = HelpBasePlugin.getDefault().getPluginPreferences().getString("productIndex");
        if (indexPluginId == null || indexPluginId.length() <= 0) {
            return;
        }
        InputStream zipIn = ResourceLocator.openFromPlugin(indexPluginId, "doc_index.zip", getLocale());
        if (zipIn == null) {
            return;
        }
        setInconsistent(true);
        cleanOldIndex();
        byte[] buf = new byte[8192];
        File destDir = indexDir;
        ZipInputStream zis = new ZipInputStream(zipIn);
        FileOutputStream fos = null;
        try {
            ZipEntry zEntry;
            while ((zEntry = zis.getNextEntry()) != null) {
                if (zEntry.isDirectory()) {
                    new File(destDir, zEntry.getName()).mkdirs();
                    continue;
                }
                String filePath = zEntry.getName();
                int lastSeparator = filePath.lastIndexOf("/");
                String fileDir = "";
                if (lastSeparator >= 0) {
                    fileDir = filePath.substring(0, lastSeparator);
                }
                new File(destDir, fileDir).mkdirs();
                File outFile = new File(destDir, filePath);
                fos = new FileOutputStream(outFile);
                int n = 0;
                while ((n = zis.read(buf)) >= 0) {
                    fos.write(buf, 0, n);
                }
                fos.close();
            }
            setInconsistent(false);
        } catch (IOException ioe) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioe2) {
                }
            }
        } finally {
            try {
                zipIn.close();
                if (zis != null) zis.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
	 * Cleans any old index and Lucene lock files by initializing a new index.
	 */
    private void cleanOldIndex() {
        IndexWriter cleaner = null;
        try {
            cleaner = new IndexWriter(indexDir, analyzerDescriptor.getAnalyzer(), true);
        } catch (IOException ioe) {
        } finally {
            try {
                if (cleaner != null) cleaner.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
	 * Returns true when the index must be updated.
	 */
    public synchronized boolean needsUpdating() {
        if (!exists()) {
            return true;
        }
        return getDocPlugins().detectChange();
    }

    /**
	 * @return Returns the tocManager.
	 */
    public TocManager getTocManager() {
        return tocManager;
    }

    private void registerSearch(Thread t) {
        synchronized (searches) {
            searches.add(t);
        }
    }

    private void unregisterSearch(Thread t) {
        synchronized (searches) {
            searches.remove(t);
        }
    }

    /**
	 * @return Returns the closed.
	 */
    public boolean isClosed() {
        return closed;
    }

    /**
	 * @return true if lock obtained for this Eclipse instance
	 * @throws OverlappingFileLockException
	 *             if lock already obtained
	 */
    public synchronized boolean tryLock() throws OverlappingFileLockException {
        if (lock != null) {
            throw new OverlappingFileLockException();
        }
        File lockFile = getLockFile();
        lockFile.getParentFile().mkdirs();
        try {
            RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
            FileLock l = raf.getChannel().tryLock();
            if (l != null) {
                lock = l;
                return true;
            }
        } catch (IOException ioe) {
            lock = null;
        }
        return false;
    }

    private File getLockFile() {
        return new File(indexDir.getParentFile(), locale + ".lock");
    }

    /**
	 * Deletes the lock file. The lock must be released prior to this call.
	 * 
	 * @return <code>true</code> if the file has been deleted, <code>false</code> otherwise.
	 */
    public synchronized boolean deleteLockFile() {
        if (lock != null) return false;
        File lockFile = getLockFile();
        if (lockFile.exists()) return lockFile.delete();
        return true;
    }

    public synchronized void releaseLock() {
        if (lock != null) {
            try {
                lock.channel().close();
            } catch (IOException ioe) {
            }
            lock = null;
        }
    }

    public static String getIndexableHref(String url) {
        String fileName = url.toLowerCase(Locale.ENGLISH);
        if (fileName.endsWith(".htm") || fileName.endsWith(".html") || fileName.endsWith(".xhtml") || fileName.endsWith(".xml") || fileName.endsWith(".txt")) {
        } else if (fileName.indexOf(".htm#") >= 0 || fileName.indexOf(".html#") >= 0 || fileName.indexOf(".xhtml#") >= 0 || fileName.indexOf(".xml#") >= 0) {
            url = url.substring(0, url.lastIndexOf('#'));
        } else {
            return BaseHelpSystem.getLocalSearchManager().isIndexable(url) ? url : null;
        }
        return url;
    }

    /**
	 * Checks if document is indexable, and creates a URL to obtain contents.
	 * 
	 * @param locale
	 * @param url
	 *            specified in the navigation
	 * @return URL to obtain document content or null
	 */
    public static URL getIndexableURL(String locale, String url) {
        return getIndexableURL(locale, url, null, null);
    }

    /**
	 * Checks if document is indexable, and creates a URL to obtain contents.
	 * 
	 * @param locale
	 * @param url
	 * @param participantId
	 *            the search participant or <code>null</code> specified in the navigation
	 * @return URL to obtain document content or null
	 */
    public static URL getIndexableURL(String locale, String url, String id, String participantId) {
        if (participantId == null) url = getIndexableHref(url);
        if (url == null) return null;
        try {
            StringBuffer query = new StringBuffer();
            query.append("?");
            query.append("lang=" + locale);
            if (id != null) query.append("&id=" + id);
            if (participantId != null) query.append("&participantId=" + participantId);
            return new URL("help", null, -1, url + query.toString(), HelpURLStreamHandler.getDefault());
        } catch (MalformedURLException mue) {
            return null;
        }
    }

    public IStatus addDocument(String pluginId, String name, URL url, String id, Document doc) {
        LuceneSearchParticipant participant = BaseHelpSystem.getLocalSearchManager().getParticipant(pluginId, name);
        if (participant != null) {
            try {
                return participant.addDocument(this, pluginId, name, url, id, doc);
            } catch (Throwable t) {
                return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR, "Error while adding document to search participant (addDocument()): " + name + ", " + url + "for participant " + participant.getClass().getName(), t);
            }
        }
        return htmlSearchParticipant.addDocument(this, pluginId, name, url, id, doc);
    }
}

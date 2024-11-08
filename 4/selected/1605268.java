package org.opencms.search;

import org.opencms.db.CmsPublishedResource;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.loader.CmsLoaderException;
import org.opencms.loader.CmsResourceManager;
import org.opencms.main.CmsEvent;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.CmsIllegalStateException;
import org.opencms.main.CmsLog;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.report.CmsLogReport;
import org.opencms.report.I_CmsReport;
import org.opencms.scheduler.I_CmsScheduledJob;
import org.opencms.search.documents.A_CmsVfsDocument;
import org.opencms.search.documents.CmsExtractionResultCache;
import org.opencms.search.documents.I_CmsDocumentFactory;
import org.opencms.search.documents.I_CmsTermHighlighter;
import org.opencms.search.fields.CmsSearchField;
import org.opencms.search.fields.CmsSearchFieldConfiguration;
import org.opencms.search.fields.CmsSearchFieldMapping;
import org.opencms.security.CmsRole;
import org.opencms.security.CmsRoleViolationException;
import org.opencms.util.A_CmsModeStringEnumeration;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.FSDirectory;

/**
 * Implements the general management and configuration of the search and 
 * indexing facilities in OpenCms.<p>
 * 
 * @author Alexander Kandzior
 * @author Carsten Weinholz 
 * 
 * @version $Revision: 1.64 $ 
 * 
 * @since 6.0.0 
 */
public class CmsSearchManager implements I_CmsScheduledJob, I_CmsEventListener {

    /**
     *  Enumeration class for force unlock types.<p>
     */
    public static final class CmsSearchForceUnlockMode extends A_CmsModeStringEnumeration {

        /** Force unlock type always. */
        public static final CmsSearchForceUnlockMode ALWAYS = new CmsSearchForceUnlockMode("always");

        /** Force unlock type never. */
        public static final CmsSearchForceUnlockMode NEVER = new CmsSearchForceUnlockMode("never");

        /** Force unlock tyoe only full. */
        public static final CmsSearchForceUnlockMode ONLYFULL = new CmsSearchForceUnlockMode("onlyfull");

        /** serializable version id. */
        private static final long serialVersionUID = 74746076708908673L;

        /**
         * Creates a new force unlock type with the given name.<p>
         * 
         * @param mode the mode id to use
         */
        protected CmsSearchForceUnlockMode(String mode) {
            super(mode);
        }

        /**
         * Returns the lock type for the given type value.<p>
         *  
         * @param type the type value to get the lock type for
         * 
         * @return the lock type for the given type value
         */
        public static CmsSearchForceUnlockMode valueOf(String type) {
            if (type.equals(ALWAYS.toString())) {
                return ALWAYS;
            } else if (type.equals(NEVER.toString())) {
                return NEVER;
            } else {
                return ONLYFULL;
            }
        }
    }

    /** The default value used for generating search result exerpts (1024 chars). */
    public static final int DEFAULT_EXCERPT_LENGTH = 1024;

    /** The default value used for keeping the extraction results in the cache (672 hours = 4 weeks). */
    public static final float DEFAULT_EXTRACTION_CACHE_MAX_AGE = 672.0f;

    /** The default timeout value used for generating a document for the search index (60000 msec = 1 min). */
    public static final int DEFAULT_TIMEOUT = 60000;

    /** Scheduler parameter: Update only a specified list of indexes. */
    public static final String JOB_PARAM_INDEXLIST = "indexList";

    /** Scheduler parameter: Write the output of the update to the logfile. */
    public static final String JOB_PARAM_WRITELOG = "writeLog";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsSearchManager.class);

    /** The Admin cms object to index Cms resources. */
    private CmsObject m_adminCms;

    /** Configured analyzers for languages using &lt;analyzer&gt;. */
    private HashMap m_analyzers;

    /** A map of document factory configurations. */
    private List m_documentTypeConfigs;

    /** A map of document factories keyed by their matching Cms resource types and/or mimetypes. */
    private Map m_documentTypes;

    /** The max age for extraction results to remain in the cache. */
    private float m_extractionCacheMaxAge;

    /** The cache for the extration results. */
    private CmsExtractionResultCache m_extractionResultCache;

    /** Contains the available field configurations. */
    private Map m_fieldConfigurations;

    /** The force unlock type. */
    private CmsSearchForceUnlockMode m_forceUnlockMode;

    /** The class used to highlight the search terms in the excerpt of a search result. */
    private I_CmsTermHighlighter m_highlighter;

    /** A list of search indexes. */
    private List m_indexes;

    /** Seconds to wait for an index lock. */
    private int m_indexLockMaxWaitSeconds = 10;

    /** Configured index sources. */
    private Map m_indexSources;

    /** The max. char. length of the excerpt in the search result. */
    private int m_maxExcerptLength;

    /** Path to index files below WEB-INF/. */
    private String m_path;

    /** Timeout for abandoning indexing thread. */
    private long m_timeout;

    /**
     * Default constructor when called as cron job.<p>
     */
    public CmsSearchManager() {
        m_documentTypes = new HashMap();
        m_documentTypeConfigs = new ArrayList();
        m_analyzers = new HashMap();
        m_indexes = new ArrayList();
        m_indexSources = new TreeMap();
        m_extractionCacheMaxAge = DEFAULT_EXTRACTION_CACHE_MAX_AGE;
        m_maxExcerptLength = DEFAULT_EXCERPT_LENGTH;
        m_fieldConfigurations = new HashMap();
        addFieldConfiguration(CmsSearchFieldConfiguration.DEFAULT_STANDARD);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_START_SEARCH_CONFIG_0));
        }
    }

    /**
     * Adds an analyzer.<p>
     * 
     * @param analyzer an analyzer
     */
    public void addAnalyzer(CmsSearchAnalyzer analyzer) {
        m_analyzers.put(analyzer.getLocale(), analyzer);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_ADD_ANALYZER_2, analyzer.getLocale(), analyzer.getClassName()));
        }
    }

    /**
     * Adds a document type.<p>
     * 
     * @param documentType a document type
     */
    public void addDocumentTypeConfig(CmsSearchDocumentType documentType) {
        m_documentTypeConfigs.add(documentType);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SEARCH_DOC_TYPES_2, documentType.getName(), documentType.getClassName()));
        }
    }

    /**
     * Adds a search field configuration to the search manager.<p>
     * 
     * @param fieldConfiguration the search field configuration to add
     */
    public void addFieldConfiguration(CmsSearchFieldConfiguration fieldConfiguration) {
        m_fieldConfigurations.put(fieldConfiguration.getName(), fieldConfiguration);
    }

    /**
     * Adds a search index to the configuration.<p>
     * 
     * @param searchIndex the search index to add
     */
    public void addSearchIndex(CmsSearchIndex searchIndex) {
        if ((searchIndex.getSources() == null) || (searchIndex.getPath() == null)) {
            if (OpenCms.getRunLevel() > OpenCms.RUNLEVEL_2_INITIALIZING) {
                try {
                    searchIndex.initialize();
                } catch (CmsSearchException e) {
                }
            }
        }
        String name = searchIndex.getName();
        if (CmsStringUtil.isEmptyOrWhitespaceOnly(name)) {
            throw new CmsIllegalArgumentException(Messages.get().container(Messages.ERR_SEARCHINDEX_CREATE_MISSING_NAME_0));
        }
        if (m_indexSources.keySet().contains(name)) {
            throw new CmsIllegalArgumentException(Messages.get().container(Messages.ERR_SEARCHINDEX_CREATE_INVALID_NAME_1, name));
        }
        m_indexes.add(searchIndex);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_ADD_SEARCH_INDEX_2, searchIndex.getName(), searchIndex.getProject()));
        }
    }

    /**
     * Adds a search index source configuration.<p>
     * 
     * @param searchIndexSource a search index source configuration
     */
    public void addSearchIndexSource(CmsSearchIndexSource searchIndexSource) {
        m_indexSources.put(searchIndexSource.getName(), searchIndexSource);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SEARCH_INDEX_SOURCE_2, searchIndexSource.getName(), searchIndexSource.getIndexerClassName()));
        }
    }

    /**
     * Implements the event listener of this class.<p>
     * 
     * @see org.opencms.main.I_CmsEventListener#cmsEvent(org.opencms.main.CmsEvent)
     */
    public void cmsEvent(CmsEvent event) {
        switch(event.getType()) {
            case I_CmsEventListener.EVENT_REBUILD_SEARCHINDEXES:
                List indexNames = null;
                if ((event.getData() != null) && CmsStringUtil.isNotEmptyOrWhitespaceOnly((String) event.getData().get(I_CmsEventListener.KEY_INDEX_NAMES))) {
                    indexNames = CmsStringUtil.splitAsList((String) event.getData().get(I_CmsEventListener.KEY_INDEX_NAMES), ",", true);
                }
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(Messages.get().getBundle().key(Messages.LOG_EVENT_REBUILD_SEARCHINDEX_1, indexNames == null ? "" : CmsStringUtil.collectionAsString(indexNames, ",")));
                    }
                    if (indexNames == null) {
                        rebuildAllIndexes(getEventReport(event));
                    } else {
                        rebuildIndexes(indexNames, getEventReport(event));
                    }
                } catch (CmsException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(Messages.get().getBundle().key(Messages.ERR_EVENT_REBUILD_SEARCHINDEX_1, indexNames == null ? "" : CmsStringUtil.collectionAsString(indexNames, ",")), e);
                    }
                }
                break;
            case I_CmsEventListener.EVENT_CLEAR_CACHES:
                if (LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().getBundle().key(Messages.LOG_EVENT_CLEAR_CACHES_0));
                }
                break;
            case I_CmsEventListener.EVENT_PUBLISH_PROJECT:
                CmsUUID publishHistoryId = new CmsUUID((String) event.getData().get(I_CmsEventListener.KEY_PUBLISHID));
                if (LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().getBundle().key(Messages.LOG_EVENT_PUBLISH_PROJECT_1, publishHistoryId));
                }
                updateAllIndexes(m_adminCms, publishHistoryId, getEventReport(event));
                if (LOG.isDebugEnabled()) {
                    LOG.debug(Messages.get().getBundle().key(Messages.LOG_EVENT_PUBLISH_PROJECT_FINISHED_1, publishHistoryId));
                }
                break;
            default:
        }
    }

    /**
     * Returns an unmodifiable view of the map that contains the {@link CmsSearchAnalyzer} list.<p>
     * 
     * The keys in the map are {@link Locale} objects, and the values are {@link CmsSearchAnalyzer} objects.
     *
     * @return an unmodifiable view of the Analyzers Map
     */
    public Map getAnalyzers() {
        return Collections.unmodifiableMap(m_analyzers);
    }

    /**
     * Returns the search analyzer for the given locale.<p>
     * 
     * @param locale the locale to get the analyzer for
     * 
     * @return the search analyzer for the given locale
     */
    public CmsSearchAnalyzer getCmsSearchAnalyzer(Locale locale) {
        return (CmsSearchAnalyzer) m_analyzers.get(locale);
    }

    /**
     * Returns the name of the directory below WEB-INF/ where the search indexes are stored.<p>
     * 
     * @return the name of the directory below WEB-INF/ where the search indexes are stored
     */
    public String getDirectory() {
        return m_path;
    }

    /**
     * Returns a document type config.<p>
     * 
     * @param name the name of the document type config
     * @return the document type config.
     */
    public CmsSearchDocumentType getDocumentTypeConfig(String name) {
        for (int i = 0; i < m_documentTypeConfigs.size(); i++) {
            CmsSearchDocumentType type = (CmsSearchDocumentType) m_documentTypeConfigs.get(i);
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable view (read-only) of the DocumentTypeConfigs Map.<p>
     *
     * @return an unmodifiable view (read-only) of the DocumentTypeConfigs Map
     */
    public List getDocumentTypeConfigs() {
        return Collections.unmodifiableList(m_documentTypeConfigs);
    }

    /**
     * Returns the maximum age a text extraction result is kept in the cache (in hours).<p>
     *
     * @return the maximum age a text extraction result is kept in the cache (in hours)
     */
    public float getExtractionCacheMaxAge() {
        return m_extractionCacheMaxAge;
    }

    /**
     * Returns the search field configuration with the given name.<p>
     * 
     * In case no configuration is available with the given name, <code>null</code> is returned.<p>
     * 
     * @param name the name to get the search field configuration for
     * 
     * @return the search field configuration with the given name
     */
    public CmsSearchFieldConfiguration getFieldConfiguration(String name) {
        return (CmsSearchFieldConfiguration) m_fieldConfigurations.get(name);
    }

    /**
     * Returns the unmodifieable List of configured {@link CmsSearchFieldConfiguration} entries.<p>
     * 
     * @return the unmodifieable List of configured {@link CmsSearchFieldConfiguration} entries
     */
    public List getFieldConfigurations() {
        List result = new ArrayList(m_fieldConfigurations.values());
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the force unlock mode during indexing.<p>
     *
     * @return the force unlock mode during indexing
     */
    public CmsSearchForceUnlockMode getForceunlock() {
        return m_forceUnlockMode;
    }

    /**
     * Returns the highlighter.<p>
     * 
     * @return the highlighter
     */
    public I_CmsTermHighlighter getHighlighter() {
        return m_highlighter;
    }

    /**
     * Returns the index belonging to the passed name.<p>
     * The index must exist already.
     * 
     * @param indexName then name of the index
     * @return an object representing the desired index
     */
    public CmsSearchIndex getIndex(String indexName) {
        for (int i = 0, n = m_indexes.size(); i < n; i++) {
            CmsSearchIndex searchIndex = (CmsSearchIndex) m_indexes.get(i);
            if (indexName.equalsIgnoreCase(searchIndex.getName())) {
                return searchIndex;
            }
        }
        return null;
    }

    /**
     * Returns the seconds to wait for an index lock during an update operation.<p>
     * 
     * @return the seconds to wait for an index lock during an update operation
     */
    public int getIndexLockMaxWaitSeconds() {
        return m_indexLockMaxWaitSeconds;
    }

    /**
     * Returns the names of all configured indexes.<p>
     * 
     * @return list of names
     */
    public List getIndexNames() {
        List indexNames = new ArrayList();
        for (int i = 0, n = m_indexes.size(); i < n; i++) {
            indexNames.add(((CmsSearchIndex) m_indexes.get(i)).getName());
        }
        return indexNames;
    }

    /**
     * Returns a search index source for a specified source name.<p>
     * 
     * @param sourceName the name of the index source
     * @return a search index source
     */
    public CmsSearchIndexSource getIndexSource(String sourceName) {
        return (CmsSearchIndexSource) m_indexSources.get(sourceName);
    }

    /**
     * Returns the max. excerpt length.<p>
     *
     * @return the max excerpt length
     */
    public int getMaxExcerptLength() {
        return m_maxExcerptLength;
    }

    /**
     * Returns an unmodifiable list of all configured <code>{@link CmsSearchIndex}</code> instances.<p>
     * 
     * @return an unmodifiable list of all configured <code>{@link CmsSearchIndex}</code> instances
     */
    public List getSearchIndexes() {
        return Collections.unmodifiableList(m_indexes);
    }

    /**
     * Returns an unmodifiable view (read-only) of the SearchIndexSources Map.<p>
     * 
     * @return an unmodifiable view (read-only) of the SearchIndexSources Map
     */
    public Map getSearchIndexSources() {
        return Collections.unmodifiableMap(m_indexSources);
    }

    /**
     * Returns the timeout to abandon threads indexing a resource.<p>
     *
     * @return the timeout to abandon threads indexing a resource
     */
    public long getTimeout() {
        return m_timeout;
    }

    /**
     * Initializes the search manager.<p>
     * 
     * @param cms the cms object
     * 
     * @throws CmsRoleViolationException in case the given opencms object does not have <code>{@link CmsRole#WORKPLACE_MANAGER}</code> permissions
     */
    public void initialize(CmsObject cms) throws CmsRoleViolationException {
        OpenCms.getRoleManager().checkRole(cms, CmsRole.WORKPLACE_MANAGER);
        try {
            m_adminCms = OpenCms.initCmsObject(cms);
        } catch (CmsException e) {
        }
        m_adminCms.getRequestContext().setSiteRoot("/");
        m_extractionResultCache = new CmsExtractionResultCache(OpenCms.getSystemInfo().getAbsoluteRfsPathRelativeToWebInf(getDirectory()), "/extractCache");
        initializeIndexes();
        Similarity.setDefault(new CmsSearchSimilarity());
        OpenCms.addCmsEventListener(this, new int[] { I_CmsEventListener.EVENT_CLEAR_CACHES, I_CmsEventListener.EVENT_PUBLISH_PROJECT, I_CmsEventListener.EVENT_REBUILD_SEARCHINDEXES });
    }

    /**
     * Initializes all configured document types and search indexes.<p>
     * 
     * This methods needs to be called if after a change in the index configuration has been made.
     */
    public void initializeIndexes() {
        initAvailableDocumentTypes();
        initSearchIndexes();
    }

    /**
     * Updates the indexes from as a scheduled job.<p> 
     * 
     * @param cms the OpenCms user context to use when reading resources from the VFS
     * @param parameters the parameters for the scheduled job
     * 
     * @throws Exception if something goes wrong
     * 
     * @return the String to write in the scheduler log
     * 
     * @see org.opencms.scheduler.I_CmsScheduledJob#launch(org.opencms.file.CmsObject, java.util.Map)
     */
    public String launch(CmsObject cms, Map parameters) throws Exception {
        CmsSearchManager manager = OpenCms.getSearchManager();
        I_CmsReport report = null;
        boolean writeLog = Boolean.valueOf((String) parameters.get(JOB_PARAM_WRITELOG)).booleanValue();
        if (writeLog) {
            report = new CmsLogReport(cms.getRequestContext().getLocale(), CmsSearchManager.class);
        }
        List updateList = null;
        String indexList = (String) parameters.get(JOB_PARAM_INDEXLIST);
        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(indexList)) {
            updateList = new ArrayList();
            String[] indexNames = CmsStringUtil.splitAsArray(indexList, '|');
            for (int i = 0; i < indexNames.length; i++) {
                if (manager.getIndex(indexNames[i]) != null) {
                    updateList.add(indexNames[i]);
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(Messages.get().getBundle().key(Messages.LOG_NO_INDEX_WITH_NAME_1, indexNames[i]));
                    }
                }
            }
        }
        long startTime = System.currentTimeMillis();
        if (updateList == null) {
            manager.rebuildAllIndexes(report);
        } else {
            manager.rebuildIndexes(updateList, report);
        }
        long runTime = System.currentTimeMillis() - startTime;
        String finishMessage = Messages.get().getBundle().key(Messages.LOG_REBUILD_INDEXES_FINISHED_1, CmsStringUtil.formatRuntime(runTime));
        if (LOG.isInfoEnabled()) {
            LOG.info(finishMessage);
        }
        return finishMessage;
    }

    /**
     * Rebuilds (if required creates) all configured indexes.<p>
     * 
     * @param report the report object to write messages (or <code>null</code>)
     * 
     * @throws CmsException if something goes wrong
     */
    public synchronized void rebuildAllIndexes(I_CmsReport report) throws CmsException {
        CmsMessageContainer container = null;
        for (int i = 0, n = m_indexes.size(); i < n; i++) {
            CmsSearchIndex searchIndex = (CmsSearchIndex) m_indexes.get(i);
            try {
                updateIndex(searchIndex, report, null);
            } catch (CmsException e) {
                container = new CmsMessageContainer(Messages.get(), Messages.ERR_INDEX_REBUILD_ALL_1, new Object[] { searchIndex.getName() });
                LOG.error(Messages.get().getBundle().key(Messages.ERR_INDEX_REBUILD_ALL_1, searchIndex.getName()), e);
            }
        }
        m_extractionResultCache.cleanCache(m_extractionCacheMaxAge);
        if (container != null) {
            throw new CmsSearchException(container);
        }
    }

    /**
     * Rebuilds (if required creates) the index with the given name.<p>
     * 
     * @param indexName the name of the index to rebuild
     * @param report the report object to write messages (or <code>null</code>)
     * 
     * @throws CmsException if something goes wrong
     */
    public synchronized void rebuildIndex(String indexName, I_CmsReport report) throws CmsException {
        CmsSearchIndex index = getIndex(indexName);
        updateIndex(index, report, null);
        m_extractionResultCache.cleanCache(m_extractionCacheMaxAge);
    }

    /**
     * Rebuilds (if required creates) the List of indexes with the given name.<p>
     * 
     * @param indexNames the names (String) of the index to rebuild
     * @param report the report object to write messages (or <code>null</code>)
     * 
     * @throws CmsException if something goes wrong
     */
    public synchronized void rebuildIndexes(List indexNames, I_CmsReport report) throws CmsException {
        Iterator i = indexNames.iterator();
        while (i.hasNext()) {
            String indexName = (String) i.next();
            CmsSearchIndex index = getIndex(indexName);
            if (index != null) {
                updateIndex(index, report, null);
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(Messages.get().getBundle().key(Messages.LOG_NO_INDEX_WITH_NAME_1, indexName));
                }
            }
        }
        m_extractionResultCache.cleanCache(m_extractionCacheMaxAge);
    }

    /**
     * Removes this fieldconfiguration from the OpenCms configuration (if it is not used any more).<p>
     * 
     * @param fieldConfiguration the fieldconfiguration to remove from the configuration 
     * 
     * @return true if remove was successful, false if preconditions for removal are ok but the given 
     *         field configuration was unknown to the manager.
     * 
     * @throws CmsIllegalStateException if the given field configuration is still used by at least one 
     *         <code>{@link CmsSearchIndex}</code>.
     *  
     */
    public boolean removeSearchFieldConfiguration(CmsSearchFieldConfiguration fieldConfiguration) throws CmsIllegalStateException {
        if (fieldConfiguration.getName().equals(CmsSearchFieldConfiguration.STR_STANDARD)) {
            throw new CmsIllegalStateException(Messages.get().container(Messages.ERR_INDEX_CONFIGURATION_DELETE_STANDARD_1, fieldConfiguration.getName()));
        }
        Iterator itIndexes = m_indexes.iterator();
        CmsSearchIndex idx;
        List referrers = new LinkedList();
        CmsSearchFieldConfiguration refFieldConfig;
        while (itIndexes.hasNext()) {
            idx = (CmsSearchIndex) itIndexes.next();
            refFieldConfig = idx.getFieldConfiguration();
            if (refFieldConfig.equals(fieldConfiguration)) {
                referrers.add(idx);
            }
        }
        if (referrers.size() > 0) {
            throw new CmsIllegalStateException(Messages.get().container(Messages.ERR_INDEX_CONFIGURATION_DELETE_2, fieldConfiguration.getName(), referrers.toString()));
        }
        return m_fieldConfigurations.remove(fieldConfiguration.getName()) != null;
    }

    /**
     * Removes a search field from the field configuration.<p>
     * 
     * @param fieldConfiguration the field configuration
     * @param field field to remove from the field configuration
     * 
     * @return true if remove was successful, false if preconditions for removal are ok but the given 
     *         field was unknown.
     * 
     * @throws CmsIllegalStateException if the given field is the last field inside the given field configuration.
     */
    public boolean removeSearchFieldConfigurationField(CmsSearchFieldConfiguration fieldConfiguration, CmsSearchField field) throws CmsIllegalStateException {
        if (fieldConfiguration.getFields().size() < 2) {
            throw new CmsIllegalStateException(Messages.get().container(Messages.ERR_CONFIGURATION_FIELD_DELETE_2, field.getName(), fieldConfiguration.getName()));
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info(Messages.get().getBundle().key(Messages.LOG_REMOVE_FIELDCONFIGURATION_FIELD_INDEX_2, field.getName(), fieldConfiguration.getName()));
            }
            return fieldConfiguration.getFields().remove(field);
        }
    }

    /**
     * Removes a search field mapping from the given field.<p>
     * 
     * @param field the field
     * @param mapping mapping to remove from the field
     * 
     * @return true if remove was successful, false if preconditions for removal are ok but the given 
     *         mapping was unknown.
     * 
     * @throws CmsIllegalStateException if the given mapping is the last mapping inside the given field.
     */
    public boolean removeSearchFieldMapping(CmsSearchField field, CmsSearchFieldMapping mapping) throws CmsIllegalStateException {
        if (field.getMappings().size() < 2) {
            throw new CmsIllegalStateException(Messages.get().container(Messages.ERR_FIELD_MAPPING_DELETE_2, mapping.getType().toString(), field.getName()));
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info(Messages.get().getBundle().key(Messages.LOG_REMOVE_FIELD_MAPPING_INDEX_2, mapping.toString(), field.getName()));
            }
            return field.getMappings().remove(mapping);
        }
    }

    /**
     * Removes a search index from the configuration.<p>
     * 
     * @param searchIndex the search index to remove
     */
    public void removeSearchIndex(CmsSearchIndex searchIndex) {
        m_indexes.remove(searchIndex);
        if (LOG.isInfoEnabled()) {
            LOG.info(Messages.get().getBundle().key(Messages.LOG_REMOVE_SEARCH_INDEX_2, searchIndex.getName(), searchIndex.getProject()));
        }
    }

    /**
     * Removes all indexes included in the given list (which must contain the name of an index to remove).<p>
     * 
     * @param indexNames the names of the index to remove
     */
    public void removeSearchIndexes(List indexNames) {
        Iterator i = indexNames.iterator();
        while (i.hasNext()) {
            String indexName = (String) i.next();
            CmsSearchIndex index = getIndex(indexName);
            if (index != null) {
                removeSearchIndex(index);
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(Messages.get().getBundle().key(Messages.LOG_NO_INDEX_WITH_NAME_1, indexName));
                }
            }
        }
    }

    /**
     * Removes this indexsource from the OpenCms configuration (if it is not used any more).<p>
     * 
     * @param indexsource the indexsource to remove from the configuration 
     * 
     * @return true if remove was successful, false if preconditions for removal are ok but the given 
     *         searchindex was unknown to the manager.
     * 
     * @throws CmsIllegalStateException if the given indexsource is still used by at least one 
     *         <code>{@link CmsSearchIndex}</code>.
     *  
     */
    public boolean removeSearchIndexSource(CmsSearchIndexSource indexsource) throws CmsIllegalStateException {
        Iterator itIndexes = m_indexes.iterator();
        CmsSearchIndex idx;
        List referrers = new LinkedList();
        List refsources;
        while (itIndexes.hasNext()) {
            idx = (CmsSearchIndex) itIndexes.next();
            refsources = idx.getSources();
            if (refsources != null) {
                if (refsources.contains(indexsource)) {
                    referrers.add(idx);
                }
            }
        }
        if (referrers.size() > 0) {
            throw new CmsIllegalStateException(Messages.get().container(Messages.ERR_INDEX_SOURCE_DELETE_2, indexsource.getName(), referrers.toString()));
        }
        return m_indexSources.remove(indexsource.getName()) != null;
    }

    /**
     * Sets the name of the directory below WEB-INF/ where the search indexes are stored.<p>
     * 
     * @param value the name of the directory below WEB-INF/ where the search indexes are stored
     */
    public void setDirectory(String value) {
        m_path = value;
    }

    /**
     * Sets the maximum age a text extraction result is kept in the cache (in hours).<p>
     *
     * @param extractionCacheMaxAge the maximum age for a text extraction result to set
     */
    public void setExtractionCacheMaxAge(float extractionCacheMaxAge) {
        m_extractionCacheMaxAge = extractionCacheMaxAge;
    }

    /**
     * Sets the maximum age a text extraction result is kept in the cache (in hours) as a String.<p>
     *
     * @param extractionCacheMaxAge the maximum age for a text extraction result to set
     */
    public void setExtractionCacheMaxAge(String extractionCacheMaxAge) {
        try {
            setExtractionCacheMaxAge(Float.parseFloat(extractionCacheMaxAge));
        } catch (NumberFormatException e) {
            LOG.error(Messages.get().getBundle().key(Messages.LOG_PARSE_EXTRACTION_CACHE_AGE_FAILED_2, extractionCacheMaxAge, new Float(DEFAULT_EXTRACTION_CACHE_MAX_AGE)), e);
            setExtractionCacheMaxAge(DEFAULT_EXTRACTION_CACHE_MAX_AGE);
        }
    }

    /**
     * Sets the unlock mode during indexing.<p>
     * 
     * @param value the value 
     */
    public void setForceunlock(String value) {
        m_forceUnlockMode = CmsSearchForceUnlockMode.valueOf(value);
    }

    /**
     * Sets the highlighter.<p>
     *
     * A highlighter is a class implementing org.opencms.search.documents.I_TermHighlighter.<p>
     *
     * @param highlighter the package/class name of the highlighter
     */
    public void setHighlighter(String highlighter) {
        try {
            m_highlighter = (I_CmsTermHighlighter) Class.forName(highlighter).newInstance();
        } catch (Exception exc) {
            m_highlighter = null;
        }
    }

    /**
     * Sets the seconds to wait for an index lock during an update operation.<p>
     * 
     * @param value the seconds to wait for an index lock during an update operation
     */
    public void setIndexLockMaxWaitSeconds(int value) {
        m_indexLockMaxWaitSeconds = value;
    }

    /**
     * Sets the max. excerpt length.<p>
     *
     * @param maxExcerptLength the max. excerpt length to set
     */
    public void setMaxExcerptLength(int maxExcerptLength) {
        m_maxExcerptLength = maxExcerptLength;
    }

    /**
     * Sets the max. excerpt length as a String.<p>
     *
     * @param maxExcerptLength the max. excerpt length to set
     */
    public void setMaxExcerptLength(String maxExcerptLength) {
        try {
            setMaxExcerptLength(Integer.parseInt(maxExcerptLength));
        } catch (Exception e) {
            LOG.error(Messages.get().getBundle().key(Messages.LOG_PARSE_EXCERPT_LENGTH_FAILED_2, maxExcerptLength, new Integer(DEFAULT_EXCERPT_LENGTH)), e);
            setMaxExcerptLength(DEFAULT_EXCERPT_LENGTH);
        }
    }

    /**
     * Sets the timeout to abandon threads indexing a resource.<p>
     * 
     * @param value the timeout in milliseconds
     */
    public void setTimeout(long value) {
        m_timeout = value;
    }

    /**
     * Sets the timeout to abandon threads indexing a resource as a String.<p>
     * 
     * @param value the timeout in milliseconds
     */
    public void setTimeout(String value) {
        try {
            setTimeout(Long.parseLong(value));
        } catch (Exception e) {
            LOG.error(Messages.get().getBundle().key(Messages.LOG_PARSE_TIMEOUT_FAILED_2, value, new Long(DEFAULT_TIMEOUT)), e);
            setTimeout(DEFAULT_TIMEOUT);
        }
    }

    /**
     * Proceed the unlocking of the given index depending on the setting of <code>m_forceUnlockMode</code> and the given mode.<p>
     * 
     * @param index the index to check the lock for
     * @param report the report to write error messages on
     * @param mode the mode of the index process if true the index is updated otherwise it is rebuild completely
     * 
     * @throws CmsIndexException if unlocking of the index is impossible for some reasons
     */
    protected void forceIndexUnlock(CmsSearchIndex index, I_CmsReport report, boolean mode) throws CmsIndexException {
        File indexPath = new File(index.getPath());
        boolean indexLocked = true;
        if (indexPath.exists()) {
            try {
                indexLocked = IndexReader.isLocked(index.getPath());
            } catch (Exception e) {
                LOG.error(Messages.get().getBundle().key(Messages.LOG_IO_INDEX_READER_OPEN_2, index.getPath(), index.getName()), e);
            }
            if (indexLocked) {
                if ((m_forceUnlockMode != null) && m_forceUnlockMode.equals(CmsSearchForceUnlockMode.ALWAYS)) {
                    try {
                        IndexReader.unlock(FSDirectory.getDirectory(index.getPath()));
                    } catch (Exception e) {
                        CmsMessageContainer msg = Messages.get().container(Messages.ERR_INDEX_LOCK_FAILED_1, index.getName());
                        report.println(msg, I_CmsReport.FORMAT_ERROR);
                        throw new CmsIndexException(msg, e);
                    }
                } else if ((m_forceUnlockMode != null) && m_forceUnlockMode.equals(CmsSearchForceUnlockMode.NEVER)) {
                    indexLocked = waitIndexLock(index, report, indexLocked);
                    if (indexLocked) {
                        CmsMessageContainer msg = Messages.get().container(Messages.ERR_INDEX_LOCK_FAILED_1, index.getName());
                        report.println(msg, I_CmsReport.FORMAT_ERROR);
                        throw new CmsIndexException(msg);
                    }
                } else {
                    if (mode) {
                        indexLocked = waitIndexLock(index, report, indexLocked);
                    }
                    if (indexLocked) {
                        if (mode) {
                            CmsMessageContainer msg = Messages.get().container(Messages.ERR_INDEX_LOCK_FAILED_1, index.getName());
                            report.println(msg, I_CmsReport.FORMAT_ERROR);
                            throw new CmsIndexException(msg);
                        } else {
                            try {
                                IndexReader.unlock(FSDirectory.getDirectory(index.getPath()));
                            } catch (Exception e) {
                                CmsMessageContainer msg = Messages.get().container(Messages.ERR_INDEX_LOCK_FAILED_1, index.getName());
                                report.println(msg, I_CmsReport.FORMAT_ERROR);
                                throw new CmsIndexException(msg, e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns an analyzer for the given language.<p>
     * 
     * The analyzer is selected according to the analyzer configuration.<p>
     * 
     * @param locale the locale to get the analyzer for
     * @return the appropriate lucene analyzer
     * @throws CmsIndexException if something goes wrong
     */
    protected Analyzer getAnalyzer(Locale locale) throws CmsIndexException {
        Analyzer analyzer = null;
        String className = null;
        CmsSearchAnalyzer analyzerConf = (CmsSearchAnalyzer) m_analyzers.get(locale);
        if (analyzerConf == null) {
            throw new CmsIndexException(Messages.get().container(Messages.ERR_ANALYZER_NOT_FOUND_1, locale));
        }
        try {
            className = analyzerConf.getClassName();
            Class analyzerClass = Class.forName(className);
            String stemmerAlgorithm = analyzerConf.getStemmerAlgorithm();
            if (stemmerAlgorithm != null) {
                analyzer = (Analyzer) analyzerClass.getDeclaredConstructor(new Class[] { String.class }).newInstance(new Object[] { stemmerAlgorithm });
            } else {
                analyzer = (Analyzer) analyzerClass.newInstance();
            }
        } catch (Exception e) {
            throw new CmsIndexException(Messages.get().container(Messages.ERR_LOAD_ANALYZER_1, className), e);
        }
        PerFieldAnalyzerWrapper wrapper = new PerFieldAnalyzerWrapper(analyzer);
        wrapper.addAnalyzer(CmsSearchField.FIELD_ROOT, new WhitespaceAnalyzer());
        return wrapper;
    }

    /**
     * Returns a lucene document factory for given resource.<p>
     * 
     * The type of the document factory is selected by the type of the resource
     * and the MIME type of the resource content, according to the configuration in <code>opencms-search.xml</code>.<p>
     * 
     * @param resource a cms resource
     * @return a lucene document factory or null
     */
    protected I_CmsDocumentFactory getDocumentFactory(CmsResource resource) {
        String mimeType = OpenCms.getResourceManager().getMimeType(resource.getRootPath(), null, CmsResourceManager.MIMETYPE_TEXT);
        I_CmsDocumentFactory result = null;
        String typeName = null;
        try {
            typeName = OpenCms.getResourceManager().getResourceType(resource.getTypeId()).getTypeName();
        } catch (CmsLoaderException e) {
        }
        if (typeName != null) {
            String documentTypeKey = A_CmsVfsDocument.getDocumentKey(typeName, mimeType);
            result = (I_CmsDocumentFactory) m_documentTypes.get(documentTypeKey);
            if (result == null) {
                result = (I_CmsDocumentFactory) m_documentTypes.get(A_CmsVfsDocument.getDocumentKey(typeName, null));
            }
        }
        return result;
    }

    /**
     * Returns the set of names of all configured documenttypes.<p>
     * 
     * @return the set of names of all configured documenttypes
     */
    protected List getDocumentTypes() {
        List names = new ArrayList();
        for (Iterator i = m_documentTypes.values().iterator(); i.hasNext(); ) {
            I_CmsDocumentFactory factory = (I_CmsDocumentFactory) i.next();
            names.add(factory.getName());
        }
        return names;
    }

    /**
     * Initializes the available Cms resource types to be indexed.<p>
     * 
     * A map stores document factories keyed by a string representing
     * a colon separated list of Cms resource types and/or mimetypes.<p>
     * 
     * The keys of this map are used to trigger a document factory to convert 
     * a Cms resource into a Lucene index document.<p>
     * 
     * A document factory is a class implementing the interface
     * {@link org.opencms.search.documents.I_CmsDocumentFactory}.<p>
     */
    protected void initAvailableDocumentTypes() {
        CmsSearchDocumentType documenttype = null;
        String className = null;
        String name = null;
        I_CmsDocumentFactory documentFactory = null;
        List resourceTypes = null;
        List mimeTypes = null;
        Class c = null;
        m_documentTypes = new HashMap();
        for (int i = 0, n = m_documentTypeConfigs.size(); i < n; i++) {
            documenttype = (CmsSearchDocumentType) m_documentTypeConfigs.get(i);
            name = documenttype.getName();
            try {
                className = documenttype.getClassName();
                resourceTypes = documenttype.getResourceTypes();
                mimeTypes = documenttype.getMimeTypes();
                if (name == null) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCTYPE_NO_NAME_0));
                }
                if (className == null) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCTYPE_NO_CLASS_DEF_0));
                }
                if (resourceTypes.size() == 0) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCTYPE_NO_RESOURCETYPE_DEF_0));
                }
                try {
                    c = Class.forName(className);
                    documentFactory = (I_CmsDocumentFactory) c.getConstructor(new Class[] { String.class }).newInstance(new Object[] { name });
                } catch (ClassNotFoundException exc) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCCLASS_NOT_FOUND_1, className), exc);
                } catch (Exception exc) {
                    throw new CmsIndexException(Messages.get().container(Messages.ERR_DOCCLASS_INIT_1, className), exc);
                }
                if (documentFactory.isUsingCache()) {
                    documentFactory.setCache(m_extractionResultCache);
                }
                for (Iterator key = documentFactory.getDocumentKeys(resourceTypes, mimeTypes).iterator(); key.hasNext(); ) {
                    m_documentTypes.put(key.next(), documentFactory);
                }
            } catch (CmsException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(Messages.get().getBundle().key(Messages.LOG_DOCTYPE_CONFIG_FAILED_1, name), e);
                }
            }
        }
    }

    /**
     * Initializes the configured search indexes.<p>
     * 
     * This initializes also the list of Cms resources types
     * to be indexed by an index source.<p>
     */
    protected void initSearchIndexes() {
        CmsSearchIndex index = null;
        for (int i = 0, n = m_indexes.size(); i < n; i++) {
            index = (CmsSearchIndex) m_indexes.get(i);
            index.setEnabled(true);
            if (index.checkConfiguration(m_adminCms)) {
                try {
                    index.initialize();
                } catch (CmsException e) {
                    if (CmsLog.INIT.isInfoEnabled()) {
                        CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_SEARCH_INIT_FAILED_1, index.getName()), e);
                    }
                }
            }
            if (CmsLog.INIT.isInfoEnabled()) {
                if (index.isEnabled()) {
                    CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_INDEX_CONFIGURED_2, index.getName(), index.getProject()));
                } else {
                    CmsLog.INIT.info(Messages.get().getBundle().key(Messages.INIT_INDEX_NOT_CONFIGURED_2, index.getName(), index.getProject()));
                }
            }
        }
    }

    /**
     * Incrementally updates all indexes that have their rebuild mode set to <code>"auto"</code>
     * after resources have been published.<p> 
     * 
     * @param adminCms an OpenCms user context with Admin permissions
     * @param publishHistoryId the history ID of the published project 
     * @param report the report to write the output to
     */
    protected synchronized void updateAllIndexes(CmsObject adminCms, CmsUUID publishHistoryId, I_CmsReport report) {
        List publishedResources;
        try {
            publishedResources = adminCms.readPublishedResources(publishHistoryId);
        } catch (CmsException e) {
            LOG.error(Messages.get().getBundle().key(Messages.LOG_READING_CHANGED_RESOURCES_FAILED_1, publishHistoryId), e);
            return;
        }
        List updateResources = new ArrayList();
        Iterator itPubRes = publishedResources.iterator();
        while (itPubRes.hasNext()) {
            CmsPublishedResource res = (CmsPublishedResource) itPubRes.next();
            if (res.isFolder() || res.getState().isUnchanged()) {
                continue;
            }
            if (res.getState().isDeleted() || res.getState().isNew() || res.getState().isChanged()) {
                if (updateResources.contains(res)) {
                    updateResources.remove(res);
                    updateResources.add(res);
                } else {
                    updateResources.add(res);
                    if (!res.getState().isDeleted() && (res.getSiblingCount() > 1)) {
                        try {
                            List siblings = adminCms.readSiblings(res.getRootPath(), CmsResourceFilter.ALL);
                            Iterator itSib = siblings.iterator();
                            while (itSib.hasNext()) {
                                CmsResource sibling = (CmsResource) itSib.next();
                                CmsPublishedResource sib = new CmsPublishedResource(sibling);
                                if (!updateResources.contains(sib)) {
                                    updateResources.add(sib);
                                }
                            }
                        } catch (CmsException e) {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn(Messages.get().getBundle().key(Messages.LOG_UNABLE_TO_READ_SIBLINGS_1, res.getRootPath()), e);
                            }
                        }
                    }
                }
            }
        }
        if (!updateResources.isEmpty()) {
            Collections.sort(updateResources);
            Iterator i = m_indexes.iterator();
            while (i.hasNext()) {
                CmsSearchIndex index = (CmsSearchIndex) i.next();
                if (CmsSearchIndex.REBUILD_MODE_AUTO.equals(index.getRebuildMode())) {
                    try {
                        updateIndex(index, report, updateResources);
                    } catch (CmsException e) {
                        LOG.error(Messages.get().getBundle().key(Messages.LOG_UPDATE_INDEX_FAILED_1, index.getName()), e);
                    }
                }
            }
        }
        m_extractionResultCache.cleanCache(m_extractionCacheMaxAge);
    }

    /**
     * Returns the report in the given event data, if <code>null</code>
     * a new log report is used.<p>
     * 
     * @param event the event to get the report for
     * 
     * @return the report
     */
    private I_CmsReport getEventReport(CmsEvent event) {
        I_CmsReport report = null;
        if (event.getData() != null) {
            report = (I_CmsReport) event.getData().get(I_CmsEventListener.KEY_REPORT);
        }
        if (report == null) {
            report = new CmsLogReport(Locale.ENGLISH, getClass());
        }
        return report;
    }

    /**
     * Updates (if required creates) the index with the given name.<p>
     * 
     * If the optional List of <code>{@link CmsPublishedResource}</code> instances is provided, the index will be 
     * incrementally updated for these resources only. If this List is <code>null</code> or empty, 
     * the index will be fully rebuild.<p>
     * 
     * @param index the index to update or rebuild
     * @param report the report to write output messages to 
     * @param resourcesToIndex an (optional) list of <code>{@link CmsPublishedResource}</code> objects to update in the index
     * 
     * @throws CmsException if something goes wrong
     */
    private void updateIndex(CmsSearchIndex index, I_CmsReport report, List resourcesToIndex) throws CmsException {
        CmsObject cms = OpenCms.initCmsObject(m_adminCms);
        if (report == null) {
            report = new CmsLogReport(cms.getRequestContext().getLocale(), CmsSearchManager.class);
        }
        if (!index.checkConfiguration(cms)) {
            return;
        }
        cms.getRequestContext().setSiteRoot("/");
        cms.getRequestContext().setCurrentProject(cms.readProject(index.getProject()));
        if ((resourcesToIndex == null) || resourcesToIndex.isEmpty()) {
            forceIndexUnlock(index, report, false);
            CmsIndexingThreadManager threadManager = new CmsIndexingThreadManager(m_timeout);
            IndexWriter writer = null;
            try {
                writer = index.getIndexWriter(true);
                report.println(Messages.get().container(Messages.RPT_SEARCH_INDEXING_REBUILD_BEGIN_1, index.getName()), I_CmsReport.FORMAT_HEADLINE);
                Iterator sources = index.getSources().iterator();
                while (sources.hasNext()) {
                    CmsSearchIndexSource source = (CmsSearchIndexSource) sources.next();
                    I_CmsIndexer indexer = source.getIndexer().newInstance(cms, report, index);
                    indexer.rebuildIndex(writer, threadManager, source);
                }
                while (threadManager.isRunning()) {
                    try {
                        wait(1000);
                    } catch (InterruptedException e) {
                    }
                }
                try {
                    writer.optimize();
                } catch (IOException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn(Messages.get().getBundle().key(Messages.LOG_IO_INDEX_WRITER_OPTIMIZE_1, index.getPath(), index.getName()), e);
                    }
                }
                report.println(Messages.get().container(Messages.RPT_SEARCH_INDEXING_REBUILD_END_1, index.getName()), I_CmsReport.FORMAT_HEADLINE);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn(Messages.get().getBundle().key(Messages.LOG_IO_INDEX_WRITER_CLOSE_2, index.getPath(), index.getName()), e);
                        }
                    }
                }
            }
            threadManager.reportStatistics(report);
        } else {
            List updateCollections = new ArrayList();
            boolean hasResourcesToDelete = false;
            boolean hasResourcesToUpdate = false;
            Iterator sources = index.getSources().iterator();
            while (sources.hasNext()) {
                CmsSearchIndexSource source = (CmsSearchIndexSource) sources.next();
                I_CmsIndexer indexer = source.getIndexer().newInstance(cms, report, index);
                CmsSearchIndexUpdateData updateData = indexer.getUpdateData(source, resourcesToIndex);
                if (!updateData.isEmpty()) {
                    updateCollections.add(updateData);
                    hasResourcesToDelete = hasResourcesToDelete | updateData.hasResourcesToDelete();
                    hasResourcesToUpdate = hasResourcesToUpdate | updateData.hasResourceToUpdate();
                }
            }
            if (hasResourcesToDelete || hasResourcesToUpdate) {
                report.println(Messages.get().container(Messages.RPT_SEARCH_INDEXING_UPDATE_BEGIN_1, index.getName()), I_CmsReport.FORMAT_HEADLINE);
            }
            forceIndexUnlock(index, report, true);
            if (hasResourcesToDelete) {
                IndexReader reader = null;
                try {
                    reader = IndexReader.open(index.getPath());
                } catch (IOException e) {
                    LOG.error(Messages.get().getBundle().key(Messages.LOG_IO_INDEX_READER_OPEN_2, index.getPath(), index.getName()), e);
                }
                if (reader != null) {
                    try {
                        Iterator i = updateCollections.iterator();
                        while (i.hasNext()) {
                            CmsSearchIndexUpdateData updateCollection = (CmsSearchIndexUpdateData) i.next();
                            if (updateCollection.hasResourcesToDelete()) {
                                updateCollection.getIndexer().deleteResources(reader, updateCollection.getResourcesToDelete());
                            }
                        }
                    } finally {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            LOG.error(Messages.get().getBundle().key(Messages.LOG_IO_INDEX_READER_CLOSE_2, index.getPath(), index.getName()), e);
                        }
                    }
                }
            }
            if (hasResourcesToUpdate) {
                CmsIndexingThreadManager threadManager = new CmsIndexingThreadManager(m_timeout);
                IndexWriter writer = null;
                try {
                    writer = index.getIndexWriter(false);
                    Iterator i = updateCollections.iterator();
                    while (i.hasNext()) {
                        CmsSearchIndexUpdateData updateCollection = (CmsSearchIndexUpdateData) i.next();
                        if (updateCollection.hasResourceToUpdate()) {
                            updateCollection.getIndexer().updateResources(writer, threadManager, updateCollection.getResourcesToUpdate());
                        }
                    }
                    while (threadManager.isRunning()) {
                        try {
                            wait(1000);
                        } catch (InterruptedException e) {
                        }
                    }
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            LOG.error(Messages.get().getBundle().key(Messages.LOG_IO_INDEX_WRITER_CLOSE_2, index.getPath(), index.getName()), e);
                        }
                    }
                }
            }
            if (hasResourcesToDelete || hasResourcesToUpdate) {
                report.println(Messages.get().container(Messages.RPT_SEARCH_INDEXING_UPDATE_END_1, index.getName()), I_CmsReport.FORMAT_HEADLINE);
            }
        }
    }

    /**
     * Checks is a given index is locked, if so waits for a numer of seconds and checks again,
     * until either the index is unlocked or a limit of seconds set by <code>{@link #setIndexLockMaxWaitSeconds(int)}</code>
     * is reached and returns the lock state of the index.<p>
     * 
     * @param index the index to check the lock for
     * @param report the report to write error messages on
     * @param indexLocked the boolean value if the index is locked
     * 
     * @return the lock state of the index
     */
    private boolean waitIndexLock(CmsSearchIndex index, I_CmsReport report, boolean indexLocked) {
        try {
            int lockSecs = 0;
            while (indexLocked && (lockSecs < m_indexLockMaxWaitSeconds)) {
                indexLocked = IndexReader.isLocked(index.getPath());
                if (indexLocked) {
                    report.println(Messages.get().container(Messages.RPT_SEARCH_INDEXING_LOCK_WAIT_2, index.getName(), new Integer(m_indexLockMaxWaitSeconds - lockSecs)), I_CmsReport.FORMAT_ERROR);
                    Thread.sleep(1000);
                    lockSecs++;
                }
            }
        } catch (Exception e) {
            LOG.error(Messages.get().getBundle().key(Messages.LOG_IO_INDEX_READER_OPEN_2, index.getPath(), index.getName()), e);
        }
        return indexLocked;
    }
}

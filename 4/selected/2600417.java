package org.amlfilter.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.amlfilter.dao.DAOEntityInterface;
import org.amlfilter.model.AdminLogEntry;
import org.amlfilter.model.Analytic;
import org.amlfilter.model.Entity;
import org.amlfilter.service.GenericService;
import org.amlfilter.util.AlgorithmUtils;
import org.amlfilter.util.DateUtils;
import org.amlfilter.util.GeneralConstants;
import org.amlfilter.util.GeneralUtils;
import org.amlfilter.util.OutputRecordProcessor;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Creates the search engine resources
 * @author Harish Seshadri
 * @version $Id$
 */
public class SearchEngineResourceCreatorService extends GenericService implements SearchEngineResourceCreatorServiceInterface, InitializingBean {

    private HibernateTransactionManager mTransactionManager;

    private SessionFactory mSessionFactory;

    private int mBatchSize = 10000;

    private int mNumberOfRecordsToProcess;

    private SynonymService mSynonymService;

    private AnalyticService mAnalyticService;

    private DesignationService mDesignationService;

    private EntitySourcesService mEntitySourcesService;

    private SearchEngineResourceService mSearchEngineResourceService;

    private DAOEntityInterface mDAOEntity;

    private AdminLogServiceInterface mAdminLogService;

    private String mTrainerInputMFilePath;

    private String mTrainerInputNFilePath;

    private String mTrainerInputPFilePath;

    private String mVsMFilePath;

    private String mVsNFilePath;

    private String mVsPFilePath;

    private String mListSelections;

    private AtomicBoolean mBusy = new AtomicBoolean(false);

    /**
     * Get the list selections
     * @return The list selections
     */
    public String getListSelections() {
        return mListSelections;
    }

    /**
     * Set the list selections
     * @param pListSelections The list selections
     */
    public void setListSelections(String pListSelections) {
        mListSelections = pListSelections;
    }

    /**
     * Get the synonym service
     * @return The synonym service
     */
    public SynonymService getSynonymService() {
        return mSynonymService;
    }

    public void setSynonymService(SynonymService pSynonymService) {
        mSynonymService = pSynonymService;
    }

    /**
	 * Get the entity sources service
	 * @return The entity sources service
	 */
    public EntitySourcesService getEntitySourcesService() {
        return mEntitySourcesService;
    }

    /**
	 * Set the entity sources service
	 * @param pEntitySourcesService The entity sources service
	 */
    public void setEntitySourcesService(EntitySourcesService pEntitySourcesService) {
        mEntitySourcesService = pEntitySourcesService;
    }

    /**
     * Get the designation service
     * @return The designation service
     */
    public DesignationService getDesignationService() {
        return mDesignationService;
    }

    /**
     * Set the designation service
     * @param pDesignationService The designation service
     */
    public void setDesignationService(DesignationService pDesignationService) {
        mDesignationService = pDesignationService;
    }

    /**
     * Get the trainer input mandatory file path
     * @return The trainer input mandatory file path
     */
    public String getTrainerInputMFilePath() {
        return mTrainerInputMFilePath;
    }

    /**
     * Set the trainer input mandatory file path
     * @param pTrainerInputMFilePath The trainer input mandatory file path
     */
    public void setTrainerInputMFilePath(String pTrainerInputMFilePath) {
        mTrainerInputMFilePath = pTrainerInputMFilePath;
    }

    /**
     * Get the trainer input non-mandatory file path
     * @return The trainer input mandatory and non-mandatory file path
     */
    public String getTrainerInputNFilePath() {
        return mTrainerInputNFilePath;
    }

    /**
     * Set the trainer input non-mandatory file path
     * @param pTrainerInputNFilePath The trainer input non-mandatory file path
     */
    public void setTrainerInputNFilePath(String pTrainerInputNFilePath) {
        mTrainerInputNFilePath = pTrainerInputNFilePath;
    }

    /**
     * Get the trainer input PEP file path
     * @return The trainer input PEP file path
     */
    public String getTrainerInputPFilePath() {
        return mTrainerInputPFilePath;
    }

    /**
     * Set the trainer input PEP file path
     * @param pTrainerInputPFilePath The trainer input PEP file path
     */
    public void setTrainerInputPFilePath(String pTrainerInputPFilePath) {
        mTrainerInputPFilePath = pTrainerInputPFilePath;
    }

    /**
     * Get the VS PEP file path
     * @return The VS PEP file path
     */
    public String getVsPFilePath() {
        return mVsPFilePath;
    }

    /**
     * Set the VS PEP file path
     * @param pVsPFilePath The VS PEP file path
     */
    public void setVsPFilePath(String pVsPFilePath) {
        mVsPFilePath = pVsPFilePath;
    }

    /**
     * Get the VS mandatory file path
     * @return The VS mandatory file path
     */
    public String getVsMFilePath() {
        return mVsMFilePath;
    }

    /**
     * Set the VS mandatory file path
     * @param mVsMFilePath  The VS mandatory file path
     */
    public void setVsMFilePath(String pVsMFilePath) {
        mVsMFilePath = pVsMFilePath;
    }

    /**
     * Get the VS non-mandatory file path
     * @return The VS non-mandatory file path
     */
    public String getVsNFilePath() {
        return mVsNFilePath;
    }

    /**
     * Set the VS non-mandatory file path
     * @param The VS non-mandatory file path
     */
    public void setVsNFilePath(String pVsNFilePath) {
        mVsNFilePath = pVsNFilePath;
    }

    /**
	 * Get the DAO  entity
	 * @return The DAO  entity
	 */
    public DAOEntityInterface getDAOEntity() {
        return mDAOEntity;
    }

    /**
	 * Set the DAO  entity
	 * @param pDAOEntity The DAO  entity
	 */
    public void setDAOEntity(DAOEntityInterface pDAOEntity) {
        mDAOEntity = pDAOEntity;
    }

    /**
	 * Get the admin log service
	 * @return The admin log service
	 */
    public AdminLogServiceInterface getAdminLogService() {
        return mAdminLogService;
    }

    /**
	 * Set the admin log service
	 * @param pAdminLogService The admin log service
	 */
    public void setAdminLogService(AdminLogServiceInterface pAdminLogService) {
        mAdminLogService = pAdminLogService;
    }

    /**
     * Is the search engine resource creator busy?
     * @return True if busy, false otherwise
     */
    public boolean isBusy() {
        return mBusy.get();
    }

    /**
     * Set the busy flag
     */
    public void setBusy(boolean pBusy) {
        mBusy.set(pBusy);
    }

    /**
     * Get the analytic service
     * @return The analytic service
     */
    public AnalyticService getAnalyticService() {
        return mAnalyticService;
    }

    /**
     * Set the analytic service
     * @param pAnalyticService The analytic service
     */
    public void setAnalyticService(AnalyticService pAnalyticService) {
        mAnalyticService = pAnalyticService;
    }

    /**
     * Get the search engine resource service
     * @return The search engine resource service
     */
    public SearchEngineResourceService getSearchEngineResourceService() {
        return mSearchEngineResourceService;
    }

    /**
     * Set the search engine resource service
     * @param pSearchEngineResourceService The search engine resource service
     */
    public void setSearchEngineResourceService(SearchEngineResourceService pSearchEngineResourceService) {
        mSearchEngineResourceService = pSearchEngineResourceService;
    }

    /**
     * Get the number of records to process
     * @return The number of records to process
     */
    public int getNumberOfRecordsToProcess() {
        return mNumberOfRecordsToProcess;
    }

    /**
     * Get the transaction manager
     * @return The transaction manager
     */
    public HibernateTransactionManager getTransactionManager() {
        return mTransactionManager;
    }

    /**
     * Set the transaction manager
     * @param pTransactionManager The transaction manager
     */
    public void setTransactionManager(HibernateTransactionManager pTransactionManager) {
        mTransactionManager = pTransactionManager;
    }

    /**
     * Get the session factory
     * @return The session factory
     */
    public SessionFactory getSessionFactory() {
        return mSessionFactory;
    }

    /**
     * Set the session factory
     * @param pSessionFactory The session factory
     */
    public void setSessionFactory(SessionFactory pSessionFactory) {
        mSessionFactory = pSessionFactory;
    }

    /**
     * Get the batch size
     * @return The batch size
     */
    public int getBatchSize() {
        return mBatchSize;
    }

    /**
     * Set the batch size
     * @param pBatchSizeThe batch size
     */
    public void setBatchSize(int pBatchSize) {
        mBatchSize = pBatchSize;
    }

    /**
     * Persist the search engine resource artifacts
     * (the meta info file and the serialized search engine resource)
     * @throws IOException
     *
     */
    protected void persistSearchEngineResourceArtifacts() throws Exception {
        Date now = new Date();
        String searchEngineResourceDateStr = Long.toString(System.currentTimeMillis());
        searchEngineResourceDateStr += "_" + DateUtils.convertDateToString(now, SearchEngineResourceService.SEARCH_ENGINE_RESOURCE_DIR_DATE_FORMAT);
        String auditResourceDirPath = getSearchEngineResourceService().getSearchEngineResourceAuditDirectoryPath();
        auditResourceDirPath = GeneralUtils.generateAbsolutePath(auditResourceDirPath, searchEngineResourceDateStr, GeneralConstants.FORWARD_SLASH_TOKEN);
        File auditResourceDir = new File(auditResourceDirPath);
        FileUtils.forceMkdir(auditResourceDir);
        SearchEngineResource searchEngineResource = getSearchEngineResourceService().createSearchEngineResource();
        String auditSearchEngineResourceFilePath = GeneralUtils.generateAbsolutePath(auditResourceDirPath, SearchEngineResourceService.SEARCH_ENGINE_RESOURCE_FILE_NAME, GeneralConstants.FORWARD_SLASH_TOKEN);
        File auditSearchEngineResourceFile = new File(auditSearchEngineResourceFilePath);
        System.gc();
        if (isLoggingInfo()) {
            logInfo("Before serializing the search engine resource");
        }
        getSearchEngineResourceService().serializeSearchEngineResource(searchEngineResource, auditSearchEngineResourceFilePath);
        if (isLoggingInfo()) {
            logInfo("After serializing the search engine resource");
        }
        SearchEngineResourceMetaInfo searchEngineResourceMetaInfo = new SearchEngineResourceMetaInfo();
        searchEngineResourceMetaInfo.setSearchEngineResourceDate(now);
        long timeStamp = System.currentTimeMillis();
        searchEngineResourceMetaInfo.setSearchEngineResourceTimeStamp(timeStamp);
        long searchEngineResourceChecksum = FileUtils.checksumCRC32(auditSearchEngineResourceFile);
        searchEngineResourceMetaInfo.setSearchEngineResourceChecksum(searchEngineResourceChecksum);
        String auditSearchEngineResourceMetaInfoFilePath = GeneralUtils.generateAbsolutePath(auditResourceDirPath, SearchEngineResourceService.SEARCH_ENGINE_RESOURCE_META_INFO_FILE_NAME, GeneralConstants.FORWARD_SLASH_TOKEN);
        File auditSearchEngineResourceMetaInfoFile = new File(auditSearchEngineResourceMetaInfoFilePath);
        getSearchEngineResourceService().generateSearchEngineResourceMetaInfoFile(searchEngineResourceMetaInfo, auditSearchEngineResourceMetaInfoFilePath);
        if (isLoggingInfo()) {
            logInfo("Serialized the search engine resource in: " + auditSearchEngineResourceFilePath);
            logInfo("Wrote the search engine resource meta file: " + auditSearchEngineResourceMetaInfoFilePath);
        }
        String latestResourceDirPath = getSearchEngineResourceService().getSearchEngineResourceLatestDirectoryPath();
        File latestResourceDir = new File(latestResourceDirPath);
        FileUtils.deleteDirectory(latestResourceDir);
        FileUtils.forceMkdir(latestResourceDir);
        FileUtils.copyFileToDirectory(auditSearchEngineResourceFile, latestResourceDir);
        FileUtils.copyFileToDirectory(auditSearchEngineResourceMetaInfoFile, latestResourceDir);
        if (isLoggingInfo()) {
            logInfo("Copied " + auditSearchEngineResourceFilePath + " to " + latestResourceDir);
            logInfo("Copied " + auditSearchEngineResourceMetaInfoFilePath + " to " + latestResourceDir);
        }
    }

    /**
     * Creates the search engine resources from the entities found in the DB. It essentially generates
     * 1) The analytical cache
     * 2) The vector spaces
     * 3) Creates the search engines resources out of those
     *
     * Note: Uses the default configurations
     */
    public void createSearchEngineResources(List<AdminLogEntry> pAdminLogEntryList) throws Exception {
        createSearchEngineResources(getTrainerInputMFilePath(), getTrainerInputNFilePath(), getTrainerInputPFilePath(), getVsMFilePath(), getVsNFilePath(), getVsPFilePath(), pAdminLogEntryList);
    }

    /**
     * Get the cleaned names
     * @param pEntityNames The entity names
     * @return A list cleaned names
     */
    protected Set<String> getCleanNames(String pEntityNames) {
        String[] namesArray = pEntityNames.split(GeneralConstants.TOKEN_SEPARATOR);
        Set<String> namesSet = new HashSet<String>();
        String name = null;
        for (int i = 0; i < namesArray.length; i++) {
            name = namesArray[i];
            name = AlgorithmUtils.cleanString(name);
            namesSet.add(name);
            name = getSynonymService().getSynonymName(name);
            name = AlgorithmUtils.cleanString(name);
            namesSet.add(name);
        }
        return namesSet;
    }

    /**
     * Map the entity to the analytic object
     * @param pEntity The entity
     * @param pAnalytic The analytic
     */
    protected void mapEntityToAnalytic(Entity pEntity, Analytic pAnalytic) {
        pAnalytic.setEntityCode(pEntity.getEntityCodeInSource());
        pAnalytic.setNames(pEntity.getEntityNames());
        pAnalytic.setEntityType(pEntity.getEntityType());
        pAnalytic.setEntitySources(pEntity.getEntitySources());
        pAnalytic.setGender(pEntity.getGender());
        pAnalytic.setPlacesOfInception(pEntity.getPlacesOfInception());
        pAnalytic.setDatesOfInception(pEntity.getDatesOfInception());
        pAnalytic.setIdentificationDocuments(pEntity.getIdentificationDocuments());
        pAnalytic.setAddresses(pEntity.getAddresses());
        pAnalytic.setCitizenships(pEntity.getCitizenships());
        pAnalytic.setLastModifiedDateForAnalyticRecord(pEntity.getLastModifiedDateForAnalyticRecord());
        pAnalytic.setLastModifiedDateForFullRecord(pEntity.getLastModifiedDateForSourceData());
        if (null != pAnalytic.getEntitySourceList() && pAnalytic.getEntitySourceList().size() > 0) {
            String[] entitySourceCodes = new String[pAnalytic.getEntitySourceList().size()];
            for (int i = 0; i < pAnalytic.getEntitySourceList().size(); i++) {
                String entitySourceKey = pAnalytic.getEntitySourceList().get(i);
                entitySourceCodes[i] = entitySourceKey;
            }
            String designation = getDesignationService().getDesignationByEntitySources(entitySourceCodes);
            pAnalytic.setDesignation(designation);
        } else {
            String masterListDefaultDesignation = getDesignationService().getDefaultDesignationForMasterList(pEntity.getListName());
            if (null == masterListDefaultDesignation) {
                String errorMessage = "masterListDefaultDesignation: " + masterListDefaultDesignation + "; listName: " + pEntity.getListName();
                if (isLoggingError()) {
                    logError(errorMessage);
                }
                throw new IllegalArgumentException(errorMessage);
            }
            pAnalytic.setDesignation(masterListDefaultDesignation);
        }
    }

    /**
     * Adds the training records for the corresponding analytic cache
     * @param pAnalyticCache The analytic cache
     * @param pTrainerInputFileBufferedWriter The training input file buffered writer
     * @throws IOException
     */
    protected void addTrainingRecordsFromAnalyticCache(AnalyticCache pAnalyticCache, BufferedWriter pTrainerInputFileBufferedWriter) throws IOException {
        Iterator<Map.Entry<String, List<Analytic>>> nameToAnalyticListIterator = pAnalyticCache.getNameToAnalyticListMapEntryIterator();
        while (nameToAnalyticListIterator.hasNext()) {
            Map.Entry<String, List<Analytic>> nameToAnalyticListEntry = nameToAnalyticListIterator.next();
            String name = nameToAnalyticListEntry.getKey();
            String designation = null;
            Set<String> designationSet = new HashSet<String>();
            List<Analytic> analyticList = nameToAnalyticListEntry.getValue();
            Iterator<Analytic> analyticListIterator = analyticList.iterator();
            while (analyticListIterator.hasNext()) {
                Analytic analytic = analyticListIterator.next();
                designation = analytic.getDesignation();
                if (!designationSet.contains(designation)) {
                    StringBuilder trainingFormatBuilder = new StringBuilder();
                    trainingFormatBuilder.append(name);
                    trainingFormatBuilder.append(GeneralConstants.COMMA_TOKEN);
                    trainingFormatBuilder.append(designation);
                    trainingFormatBuilder.append(GeneralConstants.NEW_LINE_TOKEN);
                    String record = trainingFormatBuilder.toString();
                    pTrainerInputFileBufferedWriter.write(record);
                }
                designationSet.add(designation);
            }
        }
    }

    /**
     * Generate the vector space all training file.
     * This uses the analytical cache that was created to generate a de-duplicated vector space file
     * with the correct precedence criteria for the field gender, entityType and designation
     * @throws IOException
     */
    protected void generateVectorSpaceTrainingFiles(Map<String, AnalyticCache> pDesignationToAnalyticCacheMap, String pTrainerInputMFileName, String pTrainerInputNFileName, String pTrainerInputPFileName) throws IOException {
        BufferedWriter trainerInputM_BW = null;
        BufferedWriter trainerInputN_BW = null;
        BufferedWriter trainerInputP_BW = null;
        try {
            FileOutputStream fos1 = new FileOutputStream(pTrainerInputMFileName);
            trainerInputM_BW = new BufferedWriter(new OutputStreamWriter(fos1, GeneralConstants.UTF8));
            FileOutputStream fos2 = new FileOutputStream(pTrainerInputNFileName);
            trainerInputN_BW = new BufferedWriter(new OutputStreamWriter(fos2, GeneralConstants.UTF8));
            FileOutputStream fos3 = new FileOutputStream(pTrainerInputPFileName);
            trainerInputP_BW = new BufferedWriter(new OutputStreamWriter(fos3, GeneralConstants.UTF8));
            AnalyticCache mandatoryAnalyticCache = pDesignationToAnalyticCacheMap.get(DesignationService.DESIGNATION_MANDATORY);
            AnalyticCache nonMandatoryAnalyticCache = pDesignationToAnalyticCacheMap.get(DesignationService.DESIGNATION_NONMANDATORY);
            AnalyticCache pepAnalyticCache = pDesignationToAnalyticCacheMap.get(DesignationService.DESIGNATION_PEP);
            addTrainingRecordsFromAnalyticCache(mandatoryAnalyticCache, trainerInputM_BW);
            addTrainingRecordsFromAnalyticCache(nonMandatoryAnalyticCache, trainerInputN_BW);
            addTrainingRecordsFromAnalyticCache(pepAnalyticCache, trainerInputP_BW);
        } finally {
            if (null != trainerInputM_BW) {
                trainerInputM_BW.close();
            }
            if (null != trainerInputN_BW) {
                trainerInputN_BW.close();
            }
            if (null != trainerInputP_BW) {
                trainerInputP_BW.close();
            }
        }
    }

    /**
     * Generate the search engine entry
     * @param pEntity The  entity
     * @param pDesignationToAnalyticCacheMap The designation to analytic cache map
     */
    protected void generateAnalyticalCacheEntry(Entity pEntity, Map<String, AnalyticCache> pDesignationToAnalyticCacheMap) {
        final String methodSignature = "void generateAnalyticalCacheEntry(Entity,Map<String,AnalyticCache>): ";
        AnalyticCache analyticCache = null;
        Analytic analytic = new Analytic();
        mapEntityToAnalytic(pEntity, analytic);
        analyticCache = pDesignationToAnalyticCacheMap.get(analytic.getDesignation());
        Set<String> cleanedNamesSet = getCleanNames(pEntity.getEntityNames());
        Iterator<String> cleanedNamesSetIterator = cleanedNamesSet.iterator();
        int emptyNameCount = 0;
        while (cleanedNamesSetIterator.hasNext()) {
            String cleanedName = cleanedNamesSetIterator.next();
            if (null != cleanedName && !cleanedName.trim().isEmpty()) {
                if (!analytic.getDesignation().equals(DesignationService.DESIGNATION_NOT_APPLICABLE)) {
                    getAnalyticService().createAnalyticCacheEntry(analyticCache, analytic, cleanedName);
                }
            } else {
                emptyNameCount++;
            }
        }
        if (emptyNameCount == cleanedNamesSet.size()) {
            String warningMessage = "The analytic object with code (" + analytic.getEntityCode() + ") does not contain a name";
            if (isLoggingWarning()) {
                logWarning(warningMessage);
            }
            getAdminLogService().storeAdminLogEntry(GeneralConstants.ADMIN_LOG_TASK__LOAD_LISTS, getClass().getName(), methodSignature, warningMessage, System.currentTimeMillis());
        }
    }

    /**
     * Creates the analytical cache by reading all the entities from the DB
     * and then persists it as a serialized file
     */
    protected Map<String, AnalyticCache> createDesignationAnalyticalCaches() throws Exception {
        final String methodSignature = "void createDesignationAnalyticalCaches(): ";
        Map<String, AnalyticCache> designationToAnalyticCacheMap = new HashMap<String, AnalyticCache>();
        AnalyticCache mandatoryAnalyticCache = new AnalyticCache();
        AnalyticCache nonMandatoryAnalyticCache = new AnalyticCache();
        AnalyticCache pepAnalyticCache = new AnalyticCache();
        designationToAnalyticCacheMap.put(DesignationService.DESIGNATION_MANDATORY, mandatoryAnalyticCache);
        designationToAnalyticCacheMap.put(DesignationService.DESIGNATION_NONMANDATORY, nonMandatoryAnalyticCache);
        designationToAnalyticCacheMap.put(DesignationService.DESIGNATION_PEP, pepAnalyticCache);
        Session session = SessionFactoryUtils.getSession(getSessionFactory(), false);
        List<Entity> amlfEntities = null;
        List<String> listNames = null;
        boolean useListNames = false;
        if (null != getListSelections() && !getListSelections().trim().isEmpty()) {
            String[] listSelectionsArray = getListSelections().split(GeneralConstants.COMMA_TOKEN);
            if (listSelectionsArray.length > 0) {
                listNames = new ArrayList<String>();
                if (null != listSelectionsArray) {
                    for (int i = 0; i < listSelectionsArray.length; i++) {
                        if (!listSelectionsArray[i].trim().isEmpty()) {
                            listNames.add(listSelectionsArray[i].trim());
                        }
                    }
                    if (listNames.size() > 0) {
                        useListNames = true;
                    }
                }
            }
        }
        if (useListNames) {
            amlfEntities = getDAOEntity().getEntitiesByListNames(0, getBatchSize(), listNames);
        } else {
            amlfEntities = getDAOEntity().getEntities(0, getBatchSize());
        }
        int count = 0;
        if (isLoggingInfo()) {
            logInfo("Using list names: " + listNames);
        }
        if (0 == amlfEntities.size()) {
            if (isLoggingInfo()) {
                logInfo("Count: " + count + "; nothing to process!");
            }
            return designationToAnalyticCacheMap;
        }
        int i = 0;
        while (i < amlfEntities.size()) {
            Entity amlfEntity = (Entity) amlfEntities.get(i);
            generateAnalyticalCacheEntry(amlfEntity, designationToAnalyticCacheMap);
            count++;
            if (0 == (count % getBatchSize())) {
                if (isLoggingInfo()) {
                    logInfo("Create Analytical Caches; processed: " + count);
                }
                session.flush();
            }
            if (i == amlfEntities.size() - 1) {
                session.flush();
                session.clear();
                if (useListNames) {
                    amlfEntities = getDAOEntity().getEntitiesByListNames(count, getBatchSize(), listNames);
                } else {
                    amlfEntities = getDAOEntity().getEntities(count, getBatchSize());
                }
                i = 0;
            } else {
                i++;
            }
        }
        getAnalyticService().serializeDesignationToAnalyticalCacheMap(designationToAnalyticCacheMap);
        if (isLoggingInfo()) {
            logInfo("Mandatory [AnalyticCodeToAnalyticString] Cache Size: " + mandatoryAnalyticCache.getAnalyticByAnalyticCodeCacheSize());
            logInfo("Non-Mandatory [AnalyticCodeToAnalyticString] Cache Size: " + nonMandatoryAnalyticCache.getAnalyticByAnalyticCodeCacheSize());
            logInfo("Pep [AnalyticCodeToAnalyticString] Cache Size: " + pepAnalyticCache.getAnalyticByAnalyticCodeCacheSize());
            logInfo("Mandatory [NameToAnalyticCodeList] Cache Size: " + mandatoryAnalyticCache.getAnalyticListByNameCacheSize());
            logInfo("Non-Mandatory [NameToAnalyticCodeList] Cache Size: " + nonMandatoryAnalyticCache.getAnalyticListByNameCacheSize());
            logInfo("Pep [NameToAnalyticCodeList] Cache Size: " + pepAnalyticCache.getAnalyticListByNameCacheSize());
        }
        return designationToAnalyticCacheMap;
    }

    /**
     * Generate the vector spaces
     * @throws Exception
     */
    protected void generateVectorSpaces(String pTrainerInputMFileName, String pTrainerInputNFileName, String pTrainerInputPFileName, String pVsMFileName, String pVsNFileName, String pVsPFileName) throws Exception {
        TrainDesignationFiles tdf = new TrainDesignationFiles();
        tdf.trainDesignationFiles(pTrainerInputMFileName, pTrainerInputNFileName, pTrainerInputPFileName, pVsMFileName, pVsNFileName, pVsPFileName);
        if (isLoggingInfo()) {
            logInfo("Finished training the vector space designation files (" + pTrainerInputMFileName + ", " + pTrainerInputNFileName + ", " + pTrainerInputPFileName + ")");
        }
    }

    protected void forceCreationOfFilePaths(String pTrainerInputMFileName, String pTrainerInputNFileName, String pTrainerInputPFileName, String pVsMFileName, String pVsNFileName, String pVsPFileName) throws Exception {
        if (null == pTrainerInputMFileName) {
            pTrainerInputMFileName = getTrainerInputMFilePath();
        }
        org.amlfilter.util.FileUtils.forceDirCreation(pTrainerInputMFileName);
        org.amlfilter.util.FileUtils.forceDirCreation(pTrainerInputNFileName);
        org.amlfilter.util.FileUtils.forceDirCreation(pTrainerInputPFileName);
        org.amlfilter.util.FileUtils.forceDirCreation(pVsMFileName);
        org.amlfilter.util.FileUtils.forceDirCreation(pVsNFileName);
        org.amlfilter.util.FileUtils.forceDirCreation(pVsPFileName);
    }

    /**
     * Creates the search engine resources from the entities found in the DB. It essentially generates
     * 1) The analytical cache
     * 2) The vector spaces
     * 3) Creates the search engines resources out of those
     * @param pTrainerInputMFilePath The trainer input M (Mandatory) file path
     * @param pTrainerInputNFilePath The trainer input N (Non-Mandatory) file path
     * @param pTrainerInputPFilePath The trainer input P (Pep) file path
     */
    public void createSearchEngineResources(String pTrainerInputMFilePath, String pTrainerInputNFilePath, String pTrainerInputPFilePath, String pVsMFilePath, String pVsNFilePath, String pVsPFilePath, List<AdminLogEntry> pAdminLogEntryList) throws Exception {
        final String methodSignature = "void createSearchEngineResources(String,String,String,String,String,String,int): ";
        boolean success = true;
        if (isBusy()) {
            if (isLoggingInfo()) {
                logInfo("Search engine resource creator is busy!");
            }
            return;
        } else {
            setBusy(true);
        }
        if (null == pTrainerInputMFilePath || pTrainerInputMFilePath.trim().isEmpty()) {
            pTrainerInputMFilePath = getTrainerInputMFilePath();
        }
        if (null == pTrainerInputNFilePath || pTrainerInputNFilePath.trim().isEmpty()) {
            pTrainerInputNFilePath = getTrainerInputNFilePath();
        }
        if (null == pTrainerInputPFilePath || pTrainerInputPFilePath.trim().isEmpty()) {
            pTrainerInputPFilePath = getTrainerInputPFilePath();
        }
        if (null == pVsMFilePath || pVsMFilePath.trim().isEmpty()) {
            pVsMFilePath = getVsMFilePath();
        }
        if (null == pVsNFilePath || pVsNFilePath.trim().isEmpty()) {
            pVsNFilePath = getVsNFilePath();
        }
        if (null == pVsPFilePath || pVsPFilePath.trim().isEmpty()) {
            pVsPFilePath = getVsPFilePath();
        }
        if (isLoggingInfo()) {
            String filePathsSummary = "trainerInputMFilePath: " + pTrainerInputMFilePath + "; trainerInputNFilePath: " + pTrainerInputNFilePath + "; trainerInputPFilePath: " + pTrainerInputPFilePath + "; vsMFilePath: " + pVsMFilePath + "; vsNFilePath: " + pVsNFilePath + "; vsPFilePath: " + pVsPFilePath;
            logInfo(filePathsSummary);
        }
        forceCreationOfFilePaths(pTrainerInputMFilePath, pTrainerInputNFilePath, pTrainerInputPFilePath, pVsMFilePath, pVsNFilePath, pVsPFilePath);
        long startTime = System.currentTimeMillis();
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setTimeout(3600 * 20);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = null;
        OutputRecordProcessor orp = null;
        try {
            status = getTransactionManager().getTransaction(def);
            Map<String, AnalyticCache> designationToAnalyticCacheMap = createDesignationAnalyticalCaches();
            generateVectorSpaceTrainingFiles(designationToAnalyticCacheMap, pTrainerInputMFilePath, pTrainerInputNFilePath, pTrainerInputPFilePath);
            designationToAnalyticCacheMap = null;
            generateVectorSpaces(pTrainerInputMFilePath, pTrainerInputNFilePath, pTrainerInputPFilePath, pVsMFilePath, pVsNFilePath, pVsPFilePath);
            persistSearchEngineResourceArtifacts();
            getTransactionManager().commit(status);
        } catch (Exception e) {
            getTransactionManager().rollback(status);
            String errorMessage = GeneralUtils.getStackTraceAsString(e, 50);
            if (isLoggingError()) {
                logError(errorMessage);
            }
            success = false;
        } finally {
            SearchEngineResourceService.setSearchEngineResource(null);
            String message = "Successfully created search engine resources";
            AdminLogEntry adminLogEntry = new AdminLogEntry();
            adminLogEntry.setComponentName(getClass().getName());
            adminLogEntry.setContext(methodSignature);
            adminLogEntry.setDescription(message);
            adminLogEntry.setRecordEntryTime(System.currentTimeMillis());
            adminLogEntry.setTaskName(GeneralConstants.ADMIN_LOG_TASK__LOAD_LISTS);
            pAdminLogEntryList.add(adminLogEntry);
            if (null != orp) {
                orp.cleanResources();
            }
            setBusy(false);
        }
        long endTime = System.currentTimeMillis();
        if (isLoggingInfo()) {
            logInfo("Operation successful: " + success);
            logInfo("Total Time: " + ((endTime - startTime)));
        }
    }

    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
    }

    public static void main(String[] args) throws Exception {
        String basePath = "C:/Projects/amlfilter/workspace/amlf-loader/";
        PropertyConfigurator.configure(basePath + "src/amlf-loader_log4j.properties");
        XmlBeanFactory beanFactory = new XmlBeanFactory(new FileSystemResource(basePath + "/src/amlf-loader_applicationContext.xml"));
        PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
        cfg.setLocation(new FileSystemResource(basePath + "src/amlf-loader_admin-config.properties"));
        cfg.postProcessBeanFactory(beanFactory);
        SearchEngineResourceCreatorService searchEngineResourceCreatorService = (SearchEngineResourceCreatorService) beanFactory.getBean("searchEngineResourceCreatorService");
        String amlfHome = System.getProperty(GeneralConstants.AMLFILTER_HOME);
        String trainerInputMFileName = amlfHome + "/data/tmp/vs/trainer_input-M.dat";
        String trainerInputNFileName = amlfHome + "/data/tmp/vs/trainer_input-N.dat";
        String trainerInputPFileName = amlfHome + "/data/tmp/vs/trainer_input-P.dat";
        String vsMFileName = amlfHome + "/data/tmp/vs/M.vs";
        String vsNFileName = amlfHome + "/data/tmp/vs/N.vs";
        String vsPFileName = amlfHome + "/data/tmp/vs/P.vs";
        searchEngineResourceCreatorService.setBatchSize(10000);
        List<AdminLogEntry> adminLogEntryList = new ArrayList<AdminLogEntry>();
        searchEngineResourceCreatorService.createSearchEngineResources(trainerInputMFileName, trainerInputNFileName, trainerInputPFileName, vsMFileName, vsNFileName, vsPFileName, adminLogEntryList);
    }
}

package org.amlfilter.loader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.amlfilter.dao.DAOSuspectFileProcessingStatusInterface;
import org.amlfilter.model.SuspectFileProcessingStatus;
import org.amlfilter.service.GenericService;
import org.amlfilter.service.SuspectsLoaderServiceInterface;
import org.amlfilter.util.GeneralConstants;
import org.amlfilter.util.GeneralUtils;
import org.amlfilter.util.URLUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate3.HibernateTransactionManager;

public class ListRetrieverImpl extends GenericService implements ListRetriever {

    private String mListName;

    private String mUrl;

    private String mDownloadDirectoryPath;

    private String mUserName;

    private String mPassword;

    private String mProxyHost;

    private int mProxyPort = -1;

    private boolean mUseProxy;

    private SuspectsLoaderServiceInterface mSuspectsLoaderService;

    private DAOSuspectFileProcessingStatusInterface mDAOSuspectFileProcessingStatus;

    private HibernateTransactionManager mTransactionManager;

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
	 * Get the DAO suspect file processing status
	 * @return pDAOSuspectFileProcessingStatus The DAO suspect file processing status
	 */
    public DAOSuspectFileProcessingStatusInterface getDAOSuspectFileProcessingStatus() {
        return mDAOSuspectFileProcessingStatus;
    }

    /**
	 * Set the DAO suspect file processing status
	 * @param pDAOSuspectFileProcessingStatus The DAO suspect file processing status
	 */
    public void setDAOSuspectFileProcessingStatus(DAOSuspectFileProcessingStatusInterface pDAOSuspectFileProcessingStatus) {
        mDAOSuspectFileProcessingStatus = pDAOSuspectFileProcessingStatus;
    }

    /**
	 * Get the list name
	 * @return The list name
	 */
    public String getListName() {
        return mListName;
    }

    /**
	 * Set the list name
	 * @param pListName The list name
	 */
    public void setListName(String pListName) {
        mListName = pListName;
    }

    /**
	 * Get the URL
	 * @return The URL
	 */
    public String getUrl() {
        return mUrl;
    }

    /**
	 * Set the URL
	 * @param pURL The URL
	 */
    public void setUrl(String pUrl) {
        mUrl = pUrl;
    }

    /**
	 * Get the download directory path
	 * @return The download directory path
	 */
    public String getDownloadDirectoryPath() {
        return mDownloadDirectoryPath;
    }

    /**
	 * Set the download directory path
	 * @param pDownloadDirectoryPath The download directory path
	 */
    public void setDownloadDirectoryPath(String pDownloadDirectoryPath) {
        mDownloadDirectoryPath = pDownloadDirectoryPath;
    }

    /**
	 * Get the use proxy flag
	 * @return The use proxy flag
	 */
    public boolean getUseProxy() {
        return mUseProxy;
    }

    /**
	 * Set the use proxy flag
	 * @param pUseProxy The use proxy flag
	 */
    public void setUseProxy(boolean pUseProxy) {
        mUseProxy = pUseProxy;
    }

    /**
	 * Get the proxy host
	 * @return The proxy host
	 */
    public String getProxyHost() {
        return mProxyHost;
    }

    /**
	 * Get the proxy host
	 * @return The proxy host
	 */
    public void setProxyHost(String pProxyHost) {
        mProxyHost = pProxyHost;
    }

    /**
	 * Get the proxy port
	 * @return The proxy port
	 */
    public int getProxyPort() {
        return mProxyPort;
    }

    /**
	 * Get the proxy port
	 * @return The proxy port
	 */
    public void setProxyPort(int pProxyPort) {
        mProxyPort = pProxyPort;
    }

    /**
	 * Get the user name
	 * @return The user name
	 */
    public String getUserName() {
        return mUserName;
    }

    /**
	 * Set the user name
	 * @param pUserName The user name
	 */
    public void setUserName(String pUserName) {
        mUserName = pUserName;
    }

    /**
	 * Get the password
	 * @return The password
	 */
    public String getPassword() {
        return mPassword;
    }

    /**
	 * Set the password
	 * @param pPassword The password
	 */
    public void setPassword(String pPassword) {
        mPassword = pPassword;
    }

    /**
     * Get the suspects loader service
     * @return The suspects loader service
     */
    public SuspectsLoaderServiceInterface getSuspectsLoaderService() {
        return mSuspectsLoaderService;
    }

    /**
     * Set the suspects loader service
     * @param pSuspectsLoaderService The suspects loader service
     */
    public void setSuspectsLoaderService(SuspectsLoaderServiceInterface pSuspectsLoaderService) {
        mSuspectsLoaderService = pSuspectsLoaderService;
    }

    /**
	 * Base implementation that retrieves (downloading) only one list file
	 * Note: Override this method within a subclass for downloading ore than one file 
	 * @throws Exception
	 */
    public List<SuspectFileProcessingStatus> retrieve() throws Exception {
        BufferedOutputStream bos = null;
        try {
            String listFilePath = GeneralUtils.generateAbsolutePath(getDownloadDirectoryPath(), getListName(), "/");
            listFilePath = listFilePath.concat(".xml");
            if (!new File(getDownloadDirectoryPath()).exists()) {
                FileUtils.forceMkdir(new File(getDownloadDirectoryPath()));
            }
            FileOutputStream listFileOutputStream = new FileOutputStream(listFilePath);
            bos = new BufferedOutputStream(listFileOutputStream);
            InputStream is = null;
            if (getUseProxy()) {
                is = URLUtils.getResponse(getUrl(), getUserName(), getPassword(), URLUtils.HTTP_GET_METHOD, getProxyHost(), getProxyPort());
                IOUtils.copyLarge(is, bos);
            } else {
                URLUtils.getResponse(getUrl(), getUserName(), getPassword(), bos, null);
            }
            bos.flush();
            bos.close();
            File listFile = new File(listFilePath);
            if (!listFile.exists()) {
                throw new IllegalStateException("The list file did not get created");
            }
            if (isLoggingInfo()) {
                logInfo("Downloaded list file : " + listFile);
            }
            List<SuspectFileProcessingStatus> sfpsList = new ArrayList<SuspectFileProcessingStatus>();
            String loadType = GeneralConstants.LOAD_TYPE_FULL;
            String feedType = GeneralConstants.EMPTY_TOKEN;
            String listName = getListName();
            String errorCode = "";
            String description = "";
            SuspectFileProcessingStatus sfps = getSuspectsLoaderService().storeFileIntoListIncomingDir(listFile, loadType, feedType, listName, errorCode, description);
            sfpsList.add(sfps);
            if (isLoggingInfo()) {
                logInfo("Retrieved list file with SuspectFileProcessingStatus: " + sfps);
            }
            return sfpsList;
        } finally {
            if (null != bos) {
                bos.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        PropertyConfigurator.configure("/Projects/AMLFilter/workspace/amlf-loader/src/amlf-loader_log4j.properties");
        XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("amlf-loader_applicationContext.xml"));
        PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
        cfg.setLocation(new ClassPathResource("amlf-loader_admin-config.properties"));
        cfg.postProcessBeanFactory(beanFactory);
        ListRetriever nlr = (ListRetriever) beanFactory.getBean("sdnListRetriever");
        List<SuspectFileProcessingStatus> sfpsList = nlr.retrieve();
        for (int i = 0; i < sfpsList.size(); i++) {
            System.out.println("sfpsList[" + i + "]: " + sfpsList.get(i));
        }
    }
}

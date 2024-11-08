package org.amlfilter.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.amlfilter.util.URLUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * A simple client that makes requests and dumps responses
 * Note: See the service integration document for details about the format
 * @author Harish Seshadri
 * @version $Id$
 */
public class AMLFClient {

    private String mSearchBaseURL = "http://localhost:21001/amlf";

    private String mServerUserName = null;

    private String mServerUserPassword = null;

    private boolean mUseServerCredentials = false;

    private String mProxyHost = null;

    private int mProxyPort = -1;

    private boolean mUseProxy = false;

    private String mSearchRequestRelativeFilePath = null;

    private String mSearchResponseRelativeFilePath = null;

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
	 * Get the use server credentials flag
	 * @return The use server credentials flag
	 */
    public boolean getUseServerCredentials() {
        return mUseServerCredentials;
    }

    /**
	 * Set the use server credentials flag
	 * @param pUseServerCredentials The use server credentials flag
	 */
    public void setUseServerCredentials(boolean pUseServerCredentials) {
        mUseServerCredentials = pUseServerCredentials;
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
	 * Get the server user name
	 * @return The server user name
	 */
    public String getServerUserName() {
        return mServerUserName;
    }

    /**
	 * Set the server user name
	 * @param pServerUserName The server user name
	 */
    public void setServerUserName(String pServerUserName) {
        mServerUserName = pServerUserName;
    }

    /**
	 * Get the server user password
	 * @return The server user password
	 */
    public String getServerUserPassword() {
        return mServerUserPassword;
    }

    /**
	 * Set the server user password
	 * @param pPassword The server user password
	 */
    public void setServerUserPassword(String pServerUserPassword) {
        mServerUserPassword = pServerUserPassword;
    }

    /**
	 * Get the search base URL
	 * @return The search base URL
	 */
    public String getSearchBaseURL() {
        return mSearchBaseURL;
    }

    /**
	 * Set the search base URL
	 * @param pURL The URL
	 */
    public void setSearchBaseURL(String pSearchBaseURL) {
        mSearchBaseURL = pSearchBaseURL;
    }

    /**
	 * Get the search request relative file path
	 * @param pSearchRequestRelativeFilePath
	 */
    public String getSearchRequestRelativeFilePath() {
        return mSearchRequestRelativeFilePath;
    }

    /**
	 * Get the search request relative file path
	 * @param pSearchRequestRelativeFilePath
	 */
    public void setSearchRequestRelativeFilePath(String pSearchRequestRelativeFilePath) {
        mSearchRequestRelativeFilePath = pSearchRequestRelativeFilePath;
    }

    /**
	 * Get the search response relative file path
	 * @param pSearchResponseFilePath
	 */
    public String getSearchResponseRelativeFilePath() {
        return mSearchResponseRelativeFilePath;
    }

    /**
	 * Get the search response relative file path
	 * @param pSearchResponseRelativeFilePath
	 */
    public void setSearchResponseRelativeFilePath(String pSearchResponseRelativeFilePath) {
        mSearchResponseRelativeFilePath = pSearchResponseRelativeFilePath;
    }

    /**
	 * Process the request and get the response
	 * and persist it in a file
	 * @throws Exception 
	 */
    public void process() throws Exception {
        String searchXML = FileUtils.readFileToString(new File(getSearchRequestRelativeFilePath()));
        Map<String, String> parametersMap = new HashMap<String, String>();
        parametersMap.put("searchXML", searchXML);
        String proxyHost = null;
        int proxyPort = -1;
        String serverUserName = null;
        String serverUserPassword = null;
        FileOutputStream fos = null;
        if (getUseProxy()) {
            serverUserName = getServerUserName();
            serverUserPassword = getServerUserPassword();
        }
        if (getUseProxy()) {
            proxyHost = getProxyHost();
            proxyPort = getProxyPort();
        }
        try {
            InputStream responseInputStream = URLUtils.getHttpResponse(getSearchBaseURL(), serverUserName, serverUserPassword, URLUtils.HTTP_POST_METHOD, proxyHost, proxyPort, parametersMap, -1);
            fos = new FileOutputStream(getSearchResponseRelativeFilePath());
            IOUtils.copyLarge(responseInputStream, fos);
        } finally {
            if (null != fos) {
                fos.flush();
                fos.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        XmlBeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("amlf-client_applicationContext.xml"));
        PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
        cfg.setLocation(new ClassPathResource("amlf-client_admin-config.properties"));
        cfg.postProcessBeanFactory(beanFactory);
        String log4jPath = new ClassPathResource("amlf-client_log4j.properties").getFile().getPath();
        System.out.println("log4jPath: " + log4jPath);
        PropertyConfigurator.configure(log4jPath);
        AMLFClient amlfClient = (AMLFClient) beanFactory.getBean("amlfClient");
        amlfClient.process();
    }
}

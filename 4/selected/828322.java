package cn.com.believer.songyuanframework.openapi.storage.xdrive.impl.simple.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.log4j.Logger;
import cn.com.believer.songyuanframework.openapi.storage.xdrive.constant.XDriveConstant;

/**
 * @author Jimmy
 * 
 */
public final class XDriveHTTPManager {

    /** log4j object. */
    protected static final Logger LOGGER = Logger.getLogger(XDriveHTTPManager.class);

    /** singleton instance. */
    private static XDriveHTTPManager instance;

    /** config properties. */
    private Properties config;

    /** only one instance in this application. */
    private HttpClient hc;

    /**
     * private constructor, singleton.
     */
    private XDriveHTTPManager() {
        loadConfigProperties();
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        String maxConPerHost = config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_MAXCONNECTIONSPERHOST);
        connectionManager.getParams().setDefaultMaxConnectionsPerHost(Integer.parseInt(maxConPerHost));
        String maxTotalCons = config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_MAXTOTALCONNECTIONS);
        connectionManager.getParams().setMaxTotalConnections(Integer.parseInt(maxTotalCons));
        String connTimeout = config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_CONNECTIONTIMEOUT);
        connectionManager.getParams().setConnectionTimeout(Integer.parseInt(connTimeout));
        String soConnTimeout = config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_SOCONNECTIONTIMEOUT);
        connectionManager.getParams().setSoTimeout(Integer.parseInt(soConnTimeout));
        this.hc = new HttpClient(connectionManager);
    }

    /**
     * @return the config
     */
    public Properties getConfig() {
        return this.config;
    }

    /**
     * @param config
     *            the config to set
     */
    public void setConfig(Properties config) {
        this.config = config;
        HttpConnectionManager connectionManager = this.hc.getHttpConnectionManager();
        String maxConPerHost = config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_MAXCONNECTIONSPERHOST);
        connectionManager.getParams().setDefaultMaxConnectionsPerHost(Integer.parseInt(maxConPerHost));
        String maxTotalCons = config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_MAXTOTALCONNECTIONS);
        connectionManager.getParams().setMaxTotalConnections(Integer.parseInt(maxTotalCons));
        String connTimeout = config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_CONNECTIONTIMEOUT);
        connectionManager.getParams().setConnectionTimeout(Integer.parseInt(connTimeout));
        String soConnTimeout = config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_SOCONNECTIONTIMEOUT);
        connectionManager.getParams().setSoTimeout(Integer.parseInt(soConnTimeout));
    }

    /**
     * load config file to properties.
     */
    private void loadConfigProperties() {
        this.config = new Properties();
        try {
            String userDir = System.getProperty("user.dir");
            String propertyPath = userDir + File.separator + "xdrive4j-config.properties";
            InputStream in = new FileInputStream(new File(propertyPath));
            this.config.load(in);
        } catch (FileNotFoundException e) {
            LOGGER.warn("xdrive4j-config.properties not found in classpath, use xdrive4j-config-default.properties.");
            InputStream in = this.getClass().getResourceAsStream("xdrive4j-config-default.properties");
            try {
                this.config.load(in);
            } catch (IOException e1) {
                LOGGER.fatal("io exception happened when loading xdrive4j-config-default.properties", e1);
            }
        } catch (IOException e) {
            LOGGER.fatal("io exception occured when read xdrive4j-config.properties", e);
        }
    }

    /**
     * get the only one manager.
     * 
     * @return XDriveHTTPManager
     */
    public static XDriveHTTPManager getXDriveHTTPManager() {
        if (instance == null) {
            instance = new XDriveHTTPManager();
        }
        return instance;
    }

    /**
     * post data to gateway.
     * 
     * @param url
     *            http URL
     * @param postData
     *            string of json
     * @return string response
     * @throws IOException
     *             IOException
     */
    public String doPost(String url, String postData) throws IOException {
        long t1 = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("##### doPost-start #####, url=" + url + ", postData=\n" + postData);
        }
        String response = null;
        PostMethod pMethod = new PostMethod(url);
        if ("yes".equalsIgnoreCase(config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_IGNORECOOKIES))) {
            pMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        }
        try {
            NameValuePair type = new NameValuePair("data", postData);
            pMethod.setRequestBody(new NameValuePair[] { type });
            this.hc.executeMethod(pMethod);
            response = pMethod.getResponseBodyAsString();
        } finally {
            pMethod.releaseConnection();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("##### doPost-end   #####, used time: " + (System.currentTimeMillis() - t1) + " ms,response=\n" + response + "\n");
        }
        return response;
    }

    /**
     * 
     * @param url
     *            server URL
     * @param postData
     *            post data
     * @return byte array
     * @throws IOException
     *             IO exception
     */
    public byte[] doPostGetBytes(String url, String postData) throws IOException {
        long t1 = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("##### doPost-start #####, url=" + url + ", postData=\n" + postData);
        }
        byte[] responseBody = null;
        PostMethod pMethod = new PostMethod(url);
        if ("yes".equalsIgnoreCase(config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_IGNORECOOKIES))) {
            pMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        }
        try {
            NameValuePair type = new NameValuePair("data", postData);
            pMethod.setRequestBody(new NameValuePair[] { type });
            this.hc.executeMethod(pMethod);
            responseBody = pMethod.getResponseBody();
        } finally {
            pMethod.releaseConnection();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("##### doPost-end   #####, used time: " + (System.currentTimeMillis() - t1) + " ms,response=\n" + responseBody.length + "\n");
        }
        return responseBody;
    }

    /**
     * 
     * @param url
     *            server URL
     * @param postData
     *            post data
     * @param inFile
     *            input file object
     * @return output file object
     * @throws IOException
     *             IO exception
     */
    public File doPostGetFile(String url, String postData, File inFile) throws IOException {
        long t1 = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("##### doPost-start #####, url=" + url + ", postData=\n" + postData);
        }
        InputStream responseBodyInputStream = null;
        PostMethod pMethod = new PostMethod(url);
        if ("yes".equalsIgnoreCase(config.getProperty(XDriveConstant.CONFIG_HTTPCLIENT_IGNORECOOKIES))) {
            pMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        }
        try {
            NameValuePair type = new NameValuePair("data", postData);
            pMethod.setRequestBody(new NameValuePair[] { type });
            this.hc.executeMethod(pMethod);
            responseBodyInputStream = pMethod.getResponseBodyAsStream();
            final int bufferSize = 2048;
            FileOutputStream fout = new FileOutputStream(inFile);
            byte[] buffer = new byte[bufferSize];
            int readCount = 0;
            while ((readCount = responseBodyInputStream.read(buffer)) != -1) {
                if (readCount < bufferSize) {
                    fout.write(buffer, 0, readCount);
                } else {
                    fout.write(buffer);
                }
            }
            fout.close();
        } finally {
            pMethod.releaseConnection();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("##### doPost-end   #####, used time: " + (System.currentTimeMillis() - t1) + " ms,response=[InputStream]\n");
        }
        return inFile;
    }

    /**
     * upload multiple files.
     * 
     * @param url
     *            http URL
     * @param postData
     *            post data in string format.
     * @param filesHashMap
     *            hashmap, key is string(file name), value is byte array.
     * @return response
     * @throws IOException
     *             exception
     */
    public String doMultipartPost(String url, String postData, HashMap filesHashMap) throws IOException {
        long t1 = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("doPost, start, url=" + url + ", postData" + postData);
        }
        PostMethod pMethod = new PostMethod(url);
        XDriveStringPart stringPart = new XDriveStringPart("data", postData);
        int fileCounts = filesHashMap.size();
        Part[] parts = new Part[fileCounts + 1];
        parts[0] = stringPart;
        Iterator it = filesHashMap.keySet().iterator();
        int i = 1;
        while (it.hasNext()) {
            String key = (String) it.next();
            byte[] data = (byte[]) filesHashMap.get(key);
            ByteArrayPartSource byteArrayPartSource = new ByteArrayPartSource(key, data);
            FilePart filePart = new FilePart("Filedata" + i, byteArrayPartSource);
            parts[i] = filePart;
            i++;
        }
        MultipartRequestEntity requestEntity = new MultipartRequestEntity(parts, pMethod.getParams());
        pMethod.setRequestEntity(requestEntity);
        this.hc.executeMethod(pMethod);
        byte[] responseBody = pMethod.getResponseBody();
        String response = new String(responseBody);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("doPost, end, used time: " + (System.currentTimeMillis() - t1));
            LOGGER.debug("doPost, end, response=\n" + response);
        }
        return response;
    }

    /**
     * upload multiple files.
     * 
     * @param url
     *            http URL
     * @param postData
     *            post data in string format.
     * @param fileList
     *            file list(File list)
     * @return response
     * @throws IOException
     *             exception
     */
    public String doMultipartPost(String url, String postData, List fileList) throws IOException {
        long t1 = System.currentTimeMillis();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("doPost, start, url=" + url + ", postData" + postData);
        }
        PostMethod pMethod = new PostMethod(url);
        XDriveStringPart stringPart = new XDriveStringPart("data", postData);
        int fileCounts = fileList.size();
        Part[] parts = new Part[fileCounts + 1];
        parts[0] = stringPart;
        for (int i = 0; i < fileList.size(); i++) {
            File f = (File) fileList.get(i);
            int k = i + 1;
            FilePart filePart = new FilePart("Filedata" + i, f);
            parts[k] = filePart;
        }
        MultipartRequestEntity requestEntity = new MultipartRequestEntity(parts, pMethod.getParams());
        pMethod.setRequestEntity(requestEntity);
        this.hc.executeMethod(pMethod);
        byte[] responseBody = pMethod.getResponseBody();
        String response = new String(responseBody);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("doPost, end, used time: " + (System.currentTimeMillis() - t1));
            LOGGER.debug("doPost, end, response=\n" + response);
        }
        return response;
    }
}

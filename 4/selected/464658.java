package com.patientis.framework.utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.webdav.lib.methods.CopyMethod;
import com.patientis.framework.locale.SystemUtil;
import com.patientis.framework.logging.Log;

/**
 * @author patientos
 *
 */
public class WebDavUtility {

    /**
	 * 
	 * @param hostname
	 * @param username
	 * @param password
	 * @return
	 */
    public static HttpClient initClient(String hostname, String username, String password) throws Exception {
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost(hostname);
        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        int maxHostConnections = 20;
        params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
        connectionManager.setParams(params);
        HttpClient client = new HttpClient(connectionManager);
        Credentials creds = new UsernamePasswordCredentials(username, password);
        client.getState().setCredentials(AuthScope.ANY, creds);
        client.setHostConfiguration(hostConfig);
        return client;
    }

    /**
	 * 
	 */
    public static void copyFile(HttpClient client, String urlFrom, String urlTo) throws Exception {
        CopyMethod copy = new CopyMethod(urlFrom, urlTo, true);
        client.executeMethod(copy);
    }

    /**
	 * 
	 * @param hostname
	 * @param url
	 * @param username
	 * @param password
	 * @return
	 * @throws Exception
	 */
    public static void copyFile(String hostname, String url, String username, String password, File targetFile) throws Exception {
        org.apache.commons.httpclient.HttpClient client = WebDavUtility.initClient("files-cert.rxhub.net", username, password);
        HttpMethod method = new GetMethod(url);
        client.executeMethod(method);
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(targetFile));
        IOUtils.copyLarge(method.getResponseBodyAsStream(), output);
    }
}

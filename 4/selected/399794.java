package org.amlfilter.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;

/**
 * The purpose of this class is to provide
 * some general purpose URL management utility functions
 *
 * @author Harish Seshadri
 * @version $Id: URLUtils.java,v 1.3 2008/03/19 01:36:02 sss Exp $
 */
public class URLUtils {

    public static final String HTTP_PROTOCOL = "http";

    public static final String HTTPS_PROTOCOL = "https";

    public static final int BUFFER_SIZE = 4096;

    public static final String HTTP_GET_METHOD = "GET";

    public static final String HTTP_POST_METHOD = "POST";

    public static final int HTTP_SOCKET_TIMEOUT = 60000;

    /**
     * Given the base url and a map of
     * parameters, it builds a URL and returns it
     * @param pBaseURL The base URL
     * @param pParametersMap The parameters map
     * @return The built URL
     */
    public static String buildURL(String pBaseURL, Map pParametersMap) {
        if (null == pParametersMap || 0 == pParametersMap.size()) {
            return pBaseURL;
        }
        StringBuilder urlBuffer = new StringBuilder();
        if (null != pBaseURL) {
            urlBuffer.append(pBaseURL);
            urlBuffer.append("?");
        }
        String queryString = mapToQueryString(pParametersMap);
        return urlBuffer.append(queryString).toString();
    }

    /**
     * Convert the query string into a map
     * @param pQueryString The query string
     * @param pQueryStringMap The optional query string map
     * @return The query string map
     */
    public static Map queryStringToMap(String pQueryString, Map pQueryStringMap) {
        if (null == pQueryString) {
            return null;
        }
        return StringUtils.stringToMap(pQueryStringMap, pQueryString, "=,&");
    }

    /**
     * Given a map, it is converted to a query string
     * @param pQueryString The query string
     * @param pQueryStringMap The optional query string map
     * @return The query string map
     */
    public static String mapToQueryString(Map pQueryStringMap) {
        if (null == pQueryStringMap) {
            return null;
        }
        return StringUtils.mapToString(pQueryStringMap, "=", "&");
    }

    /**
     * Get the http method given the method type (default GET)
     * @param pHttpMethodType The http method type
     * @return The http method
     */
    public static HttpMethod getHttpMethod(String pHttpMethodType, String pURL) {
        HttpMethod httpMethod = null;
        if (null == pHttpMethodType || HTTP_GET_METHOD.equalsIgnoreCase(pHttpMethodType)) {
            httpMethod = new GetMethod(pURL);
        } else if (HTTP_POST_METHOD.equalsIgnoreCase(pHttpMethodType)) {
            httpMethod = new PostMethod(pURL);
        } else {
            throw new IllegalArgumentException("Unsupported http method type");
        }
        return httpMethod;
    }

    /**
     * Given a URL to connect to; it retrieves the response for the URL
     * @param pURL The URL
     * @param pPostParametersMap The post parameters map 
     * @param pScheme The scheme (http/https) - http is the default
     * @return
     * @throws Exception
     */
    public static final String getResponse(String pURL, Map pPostParameters, String pScheme) throws Exception {
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setParameter("http.socket.timeout", HTTP_SOCKET_TIMEOUT);
        Protocol httpsProtocol = null;
        if (pScheme.trim().equalsIgnoreCase(HTTPS_PROTOCOL)) {
            httpsProtocol = new Protocol(HTTPS_PROTOCOL, new EasySSLProtocolSocketFactory(), 443);
            Protocol.registerProtocol(HTTPS_PROTOCOL, httpsProtocol);
        }
        PostMethod postMethod = new PostMethod(pURL);
        postMethod.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        if (null != pPostParameters) {
            Iterator postParametersIterator = pPostParameters.entrySet().iterator();
            while (postParametersIterator.hasNext()) {
                Map.Entry entry = (Map.Entry) postParametersIterator.next();
                postMethod.addParameter((String) entry.getKey(), (String) entry.getValue());
            }
        }
        try {
            postMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
            postMethod.getParams().setContentCharset("UTF-8");
            httpClient.executeMethod(postMethod);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(postMethod.getResponseBodyAsStream(), "UTF-8"));
            StringBuilder responseBodyBuffer = new StringBuilder();
            int count = 0;
            char[] data = new char[BUFFER_SIZE];
            while ((count = bufferedReader.read(data, 0, BUFFER_SIZE)) != -1) {
                responseBodyBuffer.append(data, 0, count);
            }
            String response = responseBodyBuffer.toString();
            return response;
        } finally {
            postMethod.releaseConnection();
        }
    }

    /**
	 * Get the response given a URL, with optional credentials and write the response to the output stream
	 * @param pURL The URL
	 * @param pUserName The user name
	 * @param pPassword The password
	 * @param pResponseOutputStream The response output stream
	 * @param pHttpMethodType The http method type
	 * @return
	 */
    public static InputStream getResponse(String pURL, String pUserName, String pPassword, OutputStream pResponseOutputStream, String pHttpMethodType) throws Exception {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        URL url = new URL(pURL);
        int port = url.getPort();
        if (-1 == port) {
            port = url.getDefaultPort();
        }
        if (url.getProtocol().equals(HTTPS_PROTOCOL)) {
            Protocol httpsProtocol = new Protocol(HTTPS_PROTOCOL, new EasySSLProtocolSocketFactory(), port);
            Protocol.registerProtocol(HTTPS_PROTOCOL, httpsProtocol);
        }
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setParameter("http.socket.timeout", HTTP_SOCKET_TIMEOUT);
        HttpMethod httpMethod = getHttpMethod(pHttpMethodType, pURL);
        if (null != pUserName && null != pPassword) {
            httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(pUserName, pPassword));
        }
        try {
            httpClient.executeMethod(httpMethod);
            if (null != pResponseOutputStream) {
                IOUtils.copy(httpMethod.getResponseBodyAsStream(), pResponseOutputStream);
            }
            return httpMethod.getResponseBodyAsStream();
        } catch (Exception e) {
            throw (e);
        } finally {
            httpMethod.releaseConnection();
        }
    }

    /**
	 * Get the response given a URL, with optional credentials and write the response to the output stream
	 * @param pURL The URL
	 * @param pUserName The user name
	 * @param pPassword The password
	 * @param pResponseOutputStream The response output stream
	 * @param pHttpMethodType The http method type
	 * @return
	 */
    public static InputStream getResponse(String pURL, String pUserName, String pPassword, String pHttpMethodType, String pProxyHost, int pProxyPort) throws Exception {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        URL url = new URL(pURL);
        int port = url.getPort();
        if (-1 == port) {
            port = url.getDefaultPort();
        }
        if (url.getProtocol().equals(HTTPS_PROTOCOL)) {
            Protocol httpsProtocol = new Protocol(HTTPS_PROTOCOL, new EasySSLProtocolSocketFactory(), port);
            Protocol.registerProtocol(HTTPS_PROTOCOL, httpsProtocol);
        }
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setParameter("http.socket.timeout", HTTP_SOCKET_TIMEOUT);
        HttpMethod httpMethod = getHttpMethod(pHttpMethodType, pURL);
        if (null != pUserName && null != pPassword) {
            httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(pUserName, pPassword));
        }
        if (null != pProxyHost && pProxyPort != -1) {
            httpClient.getHostConfiguration().setProxy(pProxyHost, pProxyPort);
        }
        try {
            httpClient.executeMethod(httpMethod);
            return httpMethod.getResponseBodyAsStream();
        } catch (Exception e) {
            throw (e);
        }
    }

    /**
	 * Basic validation of the Http URL
	 * @param pHttpURL The http URL
	 * @throws IllegalArgumentException
	 */
    public static void validateHttpURL(String pHttpURL) {
        if (null == pHttpURL || pHttpURL.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid URL (It must be a http style URL)");
        }
        if (!pHttpURL.startsWith(HTTP_PROTOCOL) && !pHttpURL.startsWith(HTTPS_PROTOCOL)) {
            throw new IllegalArgumentException("Invalid URL (It must be a http style URL)");
        }
    }

    /**
	 * Validate the http method type (POST/GET)
	 * @param pHttpMethodType The http method type
	 * @throws IllegalArgumentException
	 */
    public static void validateHttpMethodType(String pHttpMethodType) {
        if (null == pHttpMethodType || pHttpMethodType.trim().isEmpty()) {
            throw new IllegalArgumentException("Http method type cannot be empty");
        }
        if (!pHttpMethodType.equalsIgnoreCase(HTTP_GET_METHOD) && !pHttpMethodType.equalsIgnoreCase(HTTP_POST_METHOD)) {
            throw new IllegalArgumentException("(It must be a POST|GET)");
        }
    }

    /**
	 * A very generic method that gets the response given the criteria.
	 * + It supports both HTTP/HTTPS
	 * + It supports both GET/POST
	 * + It authenticates with server credentials (BASIC)
	 * + It can use a proxy configuration (host/port)
	 * + In the case of a POST it will pass the post parameters 
	 * @param pURL The URL where the scheme will be dynamically determined
	 * @param pServerUserName The user name
	 * @param pServerPassword The password
	 * @param pHttpMethodType The method type (GET/POST)
	 * @param pProxyHost The proxy host name (If null the proxy configuration is ignored)
	 * @param pProxyPort The proxy port (If -1 the proxy configuration is ignored)
	 * @parma pPostParametersMap A map of POST parameters
	 * @param pSocketTimeoutInMillis The socket timeout in millis (If -1 then use the default 60000)
	 * @return An input stream with the response
	 * @throws Exception
	 */
    public static InputStream getHttpResponse(String pURL, String pServerUserName, String pServerPassword, String pHttpMethodType, String pProxyHost, int pProxyPort, Map pPostParametersMap, int pSocketTimeoutInMillis) throws Exception {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        validateHttpURL(pURL);
        validateHttpMethodType(pHttpMethodType);
        URL url = new URL(pURL);
        int port = url.getPort();
        if (-1 == port) {
            port = url.getDefaultPort();
        }
        if (url.getProtocol().equals(HTTPS_PROTOCOL)) {
            Protocol httpsProtocol = new Protocol(HTTPS_PROTOCOL, new EasySSLProtocolSocketFactory(), port);
            Protocol.registerProtocol(HTTPS_PROTOCOL, httpsProtocol);
        }
        HttpClient httpClient = new HttpClient();
        if (-1 != pSocketTimeoutInMillis) {
            httpClient.getParams().setParameter("http.socket.timeout", pSocketTimeoutInMillis);
        } else {
            httpClient.getParams().setParameter("http.socket.timeout", HTTP_SOCKET_TIMEOUT);
        }
        HttpMethod httpMethod = getHttpMethod(pHttpMethodType, pURL);
        if (null != pServerUserName && null != pServerPassword) {
            httpClient.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(pServerUserName, pServerPassword));
        }
        if (null != pProxyHost && pProxyPort != -1) {
            httpClient.getHostConfiguration().setProxy(pProxyHost, pProxyPort);
        }
        if (null != pPostParametersMap && pPostParametersMap.size() > 0) {
            if (HTTP_POST_METHOD.equalsIgnoreCase(pHttpMethodType)) {
                Iterator postParametersIterator = pPostParametersMap.entrySet().iterator();
                while (postParametersIterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) postParametersIterator.next();
                    PostMethod postMethod = (PostMethod) httpMethod;
                    postMethod.addParameter((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
        try {
            httpClient.executeMethod(httpMethod);
            return httpMethod.getResponseBodyAsStream();
        } catch (Exception e) {
            throw (e);
        }
    }
}

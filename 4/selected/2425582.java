package org.docflower.engine.connector.netclient.transport;

import java.io.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.protocol.*;
import org.apache.commons.logging.*;
import org.docflower.engine.connector.consts.Consts;
import org.docflower.engine.connector.remotejobs.*;
import org.docflower.engine.connector.util.*;
import org.docflower.xml.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.w3c.dom.Document;

public class DocFlowerServer {

    private static final int DOCFLOWER_SERVER_PORT = 7557;

    private static final String DOCFLOWER_SERVER_HOST = "localhost";

    private static final String DOCFLOWER_SERVER_PROTO = "https";

    private static final String DOCFLOWER_SERVER_REALM = "DocFlower Server Realm";

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(DocFlowerServer.class);

    private HttpClient client;

    private URI faceUri;

    private URI registryUri;

    public DocFlowerServer(String username, String password) {
        try {
            this.faceUri = new URI(DOCFLOWER_SERVER_PROTO + "://" + DOCFLOWER_SERVER_HOST + ":" + DOCFLOWER_SERVER_PORT + "/Face", true);
            this.registryUri = new URI(DOCFLOWER_SERVER_PROTO + "://" + DOCFLOWER_SERVER_HOST + ":" + DOCFLOWER_SERVER_PORT + "/Registry", true);
            LOG.debug("Server URI was created:" + getFaceUri().toString());
            ProtocolSocketFactory psf = new EasySSLProtocolSocketFactory();
            Protocol easyhttps = new Protocol(DOCFLOWER_SERVER_PROTO, psf, DOCFLOWER_SERVER_PORT);
            HostConfiguration hc = new HostConfiguration();
            hc.setHost(faceUri.getHost(), faceUri.getPort(), easyhttps);
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
            client = createHttpClient(credentials, hc);
        } catch (Exception e) {
            LOG.error("Unable to create server URI", e);
        }
    }

    public Document request(String usecase) {
        Document result = null;
        LOG.debug("Start making the String command Request");
        try {
            GetMethod httpget = new GetMethod(getFaceUri().getPathQuery());
            HttpClient client = getHttpClient();
            httpget.setDoAuthentication(true);
            try {
                httpget.setQueryString(Consts.ATTR_USE_CASE + "=" + usecase);
                LOG.debug("executeMethod GET for string command.");
                int status = client.executeMethod(httpget);
                if (status == 200) {
                    LOG.debug("GET successfuly done reading the DOM response");
                    result = XMLUtils.getDocumentBuilder().parse(httpget.getResponseBodyAsStream());
                    DOMUtils.printDoc(result);
                    LOG.debug("GET response for one command was read into the Document");
                } else {
                    LOG.error("GET for one string command returns the error status:" + status);
                }
            } finally {
                httpget.releaseConnection();
            }
        } catch (Exception e) {
            LOG.error("Unable to execute the GET", e);
        }
        return result;
    }

    public Document request(Document queryDoc) {
        Document result = null;
        LOG.debug("Start making the DOM Request");
        try {
            PostMethod httppost = new PostMethod(getFaceUri().getPathQuery());
            HttpClient client = getHttpClient();
            httppost.setDoAuthentication(true);
            try {
                httppost.setRequestEntity(new DocumentRequestEntity(queryDoc));
                LOG.debug("executeMethod POST.");
                int status = client.executeMethod(httppost);
                if (status == 200) {
                    LOG.debug("POST successfuly done reading the DOM response");
                    result = XMLUtils.getDocumentBuilder().parse(httppost.getResponseBodyAsStream());
                    LOG.debug("POST response was read into the Document");
                } else {
                    LOG.error("POST returns the error status:" + status);
                }
            } finally {
                httppost.releaseConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean streamRequest(Document queryDoc, Writer writer, IUpdateJobProgress examJob, IProgressMonitor monitor) {
        boolean result = false;
        try {
            PostMethod httppost = new PostMethod(getFaceUri().getPathQuery());
            HttpClient client = getHttpClient();
            httppost.setDoAuthentication(true);
            try {
                httppost.setRequestEntity(new DocumentRequestEntity(queryDoc));
                int status = client.executeMethod(httppost);
                if (status == 200) {
                    LOG.debug("POST successfuly done reading the DOM response");
                    XMLUtils.getSAXParser().parse(httppost.getResponseBodyAsStream(), new StreamSAXParserEventHandler(writer, examJob, monitor));
                    result = true;
                } else {
                    LOG.error("POST returns the error status:" + status);
                }
            } finally {
                httppost.releaseConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
	 * Opens simple stream for metadata XML resource.
	 * 
	 * 
	 * @param sourceUrl
	 *            the server location of the resource. Usually it is the
	 *            bundleName/theRelativePathInTheResourceFolder
	 * @param prefix
	 *            the prefix added to the path in the bundle. Can be null.
	 * @param cacheResourceFile
	 *            the filename where to store the downloaded resource data
	 * 
	 */
    public void saveStream(String sourceUrl, String prefix, File cacheResourceFile) {
        LOG.debug("Start making the openStream");
        try {
            GetMethod httpget = new GetMethod(getRegistryUri().getPathQuery() + "/" + sourceUrl);
            if (prefix != null) {
                httpget.setQueryString("prefix=" + prefix);
            }
            HttpClient client = getHttpClient();
            httpget.setDoAuthentication(true);
            try {
                LOG.debug("executeMethod GET for openStream.");
                int status = client.executeMethod(httpget);
                if (status == 200) {
                    LOG.debug("GET successfuly done.");
                    saveStream(httpget.getResponseBodyAsStream(), cacheResourceFile);
                } else if (status == 404) {
                    LOG.debug("File not found: " + cacheResourceFile);
                    saveFileNotFoundMark(cacheResourceFile);
                } else {
                    LOG.error("GET for one string command returns the error status:" + status);
                }
            } finally {
                httpget.releaseConnection();
            }
        } catch (Exception e) {
            LOG.error("Unable to execute the GET", e);
            throw new DocFlowerConnectorException("Unable to retrive the resource:" + sourceUrl);
        }
    }

    private void saveFileNotFoundMark(File cacheResourceFile) throws IOException {
        createNewFile(cacheResourceFile);
    }

    private void saveStream(InputStream responseBodyAsStream, File cacheResourceFile) throws IOException {
        createNewFile(cacheResourceFile);
        BufferedOutputStream destStream = new BufferedOutputStream(new FileOutputStream(cacheResourceFile));
        try {
            int readByte;
            while ((readByte = responseBodyAsStream.read()) != -1) {
                destStream.write(readByte);
            }
        } finally {
            destStream.flush();
            destStream.close();
        }
    }

    private void createNewFile(File cacheResourceFile) throws IOException {
        cacheResourceFile.getParentFile().mkdirs();
        cacheResourceFile.createNewFile();
    }

    private HttpClient createHttpClient(UsernamePasswordCredentials credentials, HostConfiguration hc) {
        LOG.debug("Creating the HttpClient");
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpClient httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(hc);
        httpClient.getState().setCredentials(new AuthScope(DOCFLOWER_SERVER_HOST, DOCFLOWER_SERVER_PORT, DOCFLOWER_SERVER_REALM), credentials);
        return httpClient;
    }

    private HttpClient getHttpClient() {
        return client;
    }

    public URI getFaceUri() {
        return faceUri;
    }

    public URI getRegistryUri() {
        return registryUri;
    }

    public void setRegistryUri(URI registryUri) {
        this.registryUri = registryUri;
    }
}

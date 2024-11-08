package org.ludo.blocklist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ludo.config.ConfigConstants;
import org.ludo.config.ConfigEntry;
import org.ludo.net.HttpConstants;
import org.ludo.safepeer.cache.DefaultEvilIPCache;
import org.ludo.safepeer.database.DatabaseDataRetriever;
import org.ludo.safepeer.database.PeerGuardianDatabaseDataRetriever;
import org.ludo.safepeer.parser.DatabaseDataParser;
import org.ludo.safepeer.parser.PeerGuardianDatabaseDataParser;
import org.ludo.util.URLParser;

/**
 * @author  <a href="mailto:masterludo@gmx.net">Ludovic Kim-Xuan Galibert</a>
 * @revision $Id: BlocklistManager.java,v 1.4 2004/12/12 09:19:30 masterludo Exp $
 * @created Mar 15, 2004
 */
public class BlocklistManager {

    /** Holds the singleton instance */
    private static BlocklistManager instance;

    /** The version number */
    public static final String INTERNAL_REVISION = "1.2";

    /**
	 * Creates a new BlocklistManager
	 */
    protected BlocklistManager() {
    }

    /**
	 * Gets the singleton instance of this class.
	 * @return  a BlocklistManager
	 */
    public static BlocklistManager getInstance() {
        if (null == instance) {
            instance = new BlocklistManager();
        }
        return instance;
    }

    /**
	 * Loads the blocklist from the blocklist URL and parse it.
	 * @param aConfigEntry  the ConfigEntry for this BlocklistManager
	 * @return  true if the blocklist was successfuly loaded, otherwise false.
	 */
    public boolean loadBlocklist(ConfigEntry aConfigEntry) {
        boolean result = false;
        String blocklists = null;
        try {
            URL url = new URL(aConfigEntry.getProperty(BlocklistConstants.BLOCKLIST_URL));
            System.out.println("should load blocklist from: " + url.toString());
            InputStream configListStream = null;
            GetMethod method = null;
            if (url.getProtocol().equalsIgnoreCase("http")) {
                method = new GetMethod(url.toString());
                method.setFollowRedirects(true);
                String host = url.getHost();
                method.addRequestHeader(HttpConstants.HEADER_HOST, host);
                HttpClient client = new HttpClient();
                HostConfiguration config = new HostConfiguration();
                config.setHost(host);
                handleProxySettings(aConfigEntry, config);
                config.setHost(host);
                client.setHostConfiguration(config);
                long timeout;
                String toval = aConfigEntry.getProperty(ConfigConstants.SOCKET_CONNECT_TIMEOUT);
                if (null != toval) {
                    timeout = Long.parseLong(toval) * 1000;
                } else {
                    timeout = 60000;
                }
                client.getParams().setConnectionManagerTimeout(timeout);
                try {
                    client.executeMethod(method);
                    if (200 == method.getStatusCode() || 301 == method.getStatusCode() || 302 == method.getStatusCode()) {
                        if (301 == method.getStatusCode() || 302 == method.getStatusCode()) {
                            URL newUrl = new URL(method.getResponseHeader(HttpConstants.HEADER_LOCATION).getValue());
                            aConfigEntry.getProperties().setProperty(BlocklistConstants.BLOCKLIST_URL, newUrl.toString());
                            method.releaseConnection();
                            result = loadBlocklist(aConfigEntry);
                        } else {
                            configListStream = method.getResponseBodyAsStream();
                        }
                    } else {
                        method.releaseConnection();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (url.getProtocol().equalsIgnoreCase("file")) {
                try {
                    configListStream = new FileInputStream(url.getFile());
                } catch (FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                }
            }
            if (null != configListStream) {
                try {
                    File configFile = File.createTempFile("blocklist", ".config");
                    FileOutputStream fos = new FileOutputStream(configFile);
                    readInputStream(fos, configListStream);
                    fos.close();
                    if (null != method) {
                        method.releaseConnection();
                    }
                    configListStream.close();
                    blocklists = getBlocklists(configFile.toURL(), aConfigEntry);
                    configFile.delete();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            blocklists = null;
        }
        String cachefile = aConfigEntry.getConfigPath() + aConfigEntry.getProperty(BlocklistConstants.BLOCKLIST_CACHE_FILE);
        if (null != blocklists) {
            StringTokenizer tokenizer = new StringTokenizer(blocklists, "\n");
            List entries = new ArrayList(50);
            while (tokenizer.hasMoreTokens()) {
                String line = tokenizer.nextToken();
                BlocklistEntry entry = parseBlocklistLine(line);
                if (null != entry) {
                    entries.add(entry);
                }
            }
            parseBlocklists(entries, aConfigEntry);
            result = true;
        } else {
            DatabaseDataRetriever retriever = new PeerGuardianDatabaseDataRetriever();
            DatabaseDataParser parser = new PeerGuardianDatabaseDataParser();
            try {
                List files = retriever.retrieveDataFiles(new File(cachefile).toURL(), aConfigEntry);
                if (null != files) {
                    List ranges = parser.parse(files, null);
                    if (null != ranges) {
                        DefaultEvilIPCache.getInstance().addEvilPeerEntry(ranges);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    protected void readInputStream(OutputStream anOS, InputStream anIS) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        int read;
        while ((read = anIS.read(buffer)) >= 0) {
            anOS.write(buffer, 0, read);
        }
        anOS.flush();
    }

    protected BlocklistEntry parseBlocklistLine(String aLine) {
        BlocklistEntry result = null;
        try {
            if (!aLine.trim().equals("")) {
                StringTokenizer tokenizer = new StringTokenizer(aLine, ",");
                String type = tokenizer.nextToken();
                URL url = new URL(tokenizer.nextToken());
                String shortDescription = tokenizer.nextToken();
                int status = Integer.valueOf(tokenizer.nextToken()).intValue();
                String longDescription = tokenizer.nextToken();
                result = new SimpleBlocklistEntry(url, type, status);
            }
        } catch (Exception ex) {
        }
        return result;
    }

    /**
	 * Gets the list of blocklists from the given URL.
	 * @param anUrl  the source URL.
	 * @param aConfigEntry  a ConfigEntry, must not be <code>null</code>.
	 * @return
	 */
    protected String getBlocklists(URL anUrl, ConfigEntry aConfigEntry) {
        String result = null;
        try {
            if (anUrl.getProtocol().equalsIgnoreCase("http")) {
                HttpMethod method = new GetMethod(anUrl.toString());
                method.setFollowRedirects(true);
                String host = anUrl.getHost();
                method.addRequestHeader(HttpConstants.HEADER_HOST, host);
                HttpClient client = new HttpClient();
                HostConfiguration config = new HostConfiguration();
                config.setHost(host);
                client.setHostConfiguration(config);
                handleProxySettings(aConfigEntry, config);
                config.setHost(host);
                client.setHostConfiguration(config);
                long timeout;
                String toval = aConfigEntry.getProperty(ConfigConstants.SOCKET_CONNECT_TIMEOUT);
                if (null != toval) {
                    timeout = Long.parseLong(toval) * 1000;
                } else {
                    timeout = 60000;
                }
                client.getParams().setConnectionManagerTimeout(timeout);
                try {
                    client.executeMethod(method);
                    if (200 == method.getStatusCode() || 301 == method.getStatusCode() || 302 == method.getStatusCode()) {
                        if (301 == method.getStatusCode() || 302 == method.getStatusCode()) {
                            URL newUrl = new URL(method.getResponseHeader(HttpConstants.HEADER_LOCATION).getValue());
                            result = getBlocklists(newUrl, aConfigEntry);
                        } else {
                            Header contentType = method.getResponseHeader(HttpConstants.HEADER_CONTENT_TYPE);
                            if (null != contentType) {
                                if (contentType.getValue().toLowerCase().startsWith("text/html")) {
                                    result = SimpleHtmlToPlainParser.getInstance().parse(method.getResponseBodyAsStream());
                                } else if (contentType.getValue().toLowerCase().startsWith("text/plain")) {
                                    result = URLParser.readInputStream(method.getResponseBodyAsStream());
                                } else {
                                    throw new UnknownServiceException("Unsupported content-type '" + contentType + "' in Blocklist manager.");
                                }
                            }
                        }
                    } else {
                        throw new IOException("The URL " + anUrl.toString() + " could not be found");
                    }
                    method.releaseConnection();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else if (anUrl.getProtocol().equalsIgnoreCase("file")) {
                BufferedReader reader = new BufferedReader(new FileReader(anUrl.getFile()));
                StringBuffer buffer = new StringBuffer(256);
                String line = null;
                while (null != (line = reader.readLine())) {
                    buffer.append(line);
                    buffer.append("\n");
                }
                result = buffer.toString();
                reader.close();
            } else {
                throw new UnknownServiceException("The following protocol is not supported: " + anUrl.getProtocol());
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (UnknownServiceException use) {
            use.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return result;
    }

    /**
	 * Handles the proxy settings from the given ConfigEntry for the given HttpClient.
	 * @param anEntry  a ConfigEntry, must not be <code>null</code>.
	 * @param aConfig  a HostConfiguration, must not be <code>null</code>.
	 */
    public static void handleProxySettings(ConfigEntry anEntry, HostConfiguration aConfig) {
        if (null != anEntry && null != aConfig) {
            boolean proxyEnabled = Boolean.valueOf(anEntry.getProperty(ConfigConstants.ENABLE_HTTP_PROXY)).booleanValue();
            if (proxyEnabled) {
                String proxyHost = anEntry.getProperty(ConfigConstants.HTTP_PROXY_HOST);
                if (null != proxyHost) {
                    String port = anEntry.getProperty(ConfigConstants.HTTP_PROXY_PORT);
                    if (null != port) {
                        try {
                            int proxyPort = Integer.parseInt(port);
                            aConfig.setProxy(proxyHost, proxyPort);
                        } catch (NumberFormatException nfe) {
                            LOG.warn("Could not parse proxy port: " + port);
                            LOG.warn(nfe);
                        }
                    }
                }
            }
        }
    }

    protected void parseBlocklists(List someBlocklists, ConfigEntry aConfigEntry) {
        if (null == someBlocklists) return;
        StringBuffer buffer = new StringBuffer(1024);
        Iterator it = someBlocklists.iterator();
        DatabaseDataParser parser = new PeerGuardianDatabaseDataParser();
        DatabaseDataRetriever retriever = new PeerGuardianDatabaseDataRetriever();
        String filename = aConfigEntry.getConfigPath() + aConfigEntry.getProperty(BlocklistConstants.BLOCKLIST_CACHE_FILE);
        File cacheFile = new File(filename);
        File tmpCache = new File(filename + ".old");
        FileOutputStream fos = null;
        try {
            if (cacheFile.exists()) {
                FileOutputStream tmpFos = new FileOutputStream(tmpCache);
                FileInputStream tmpFis = new FileInputStream(cacheFile);
                readInputStream(tmpFos, tmpFis);
                tmpFis.close();
                tmpFos.close();
            }
            fos = new FileOutputStream(cacheFile);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        while (it.hasNext()) {
            BlocklistEntry entry = (BlocklistEntry) it.next();
            List files = retriever.retrieveDataFiles(entry.getURL(), aConfigEntry);
            if (null != files) {
                try {
                    List evilPeers = parser.parse(files, fos);
                    DefaultEvilIPCache.getInstance().addEvilPeerEntry(evilPeers);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        try {
            if (null != tmpCache && tmpCache.exists()) {
                tmpCache.delete();
            }
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static final Log LOG = LogFactory.getLog(BlocklistManager.class);
}

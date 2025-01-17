package org.webcastellum;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import javax.servlet.FilterConfig;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class DefaultGeoLocator implements GeoLocator {

    private static final boolean DEBUG = false;

    public static final String PARAM_ENABLED = "DefaultGeoLocatorEnabled";

    public static final String PARAM_CACHING_NEGATIVES_ALLOWED = "DefaultGeoLocatorCachingOfNegativeRepliesAllowed";

    public static final String PARAM_TIMEOUT_CONNECT = "DefaultGeoLocatorConnectTimeout";

    public static final String PARAM_TIMEOUT_READ = "DefaultGeoLocatorReadTimeout";

    public static final String PARAM_SERVICE_URL = "DefaultGeoLocatorServiceUrl";

    public static final String PARAM_INTERNET_PROXY_HOST = "DefaultGeoLocatorInternetProxyHost";

    public static final String PARAM_INTERNET_PROXY_PORT = "DefaultGeoLocatorInternetProxyPort";

    public static final String PARAM_XML_ELEMENT_COUNTRY_CODE = "DefaultGeoLocatorXmlCountryCodeElement";

    private boolean enabled = false, cachingOfNegativeRepliesAllowed = false;

    private int connectTimeout, readTimeout;

    private String servicePattern, xmlElementName;

    private Proxy proxy;

    public void setFilterConfig(FilterConfig filterConfig) throws FilterConfigurationException {
        final ConfigurationManager configManager = ConfigurationUtils.createConfigurationManager(filterConfig);
        this.enabled = ("" + true).equals(ConfigurationUtils.extractOptionalConfigValue(configManager, PARAM_ENABLED, "" + false).trim().toLowerCase());
        this.cachingOfNegativeRepliesAllowed = ("" + true).equals(ConfigurationUtils.extractOptionalConfigValue(configManager, PARAM_CACHING_NEGATIVES_ALLOWED, "" + true).trim().toLowerCase());
        {
            final String value = ConfigurationUtils.extractOptionalConfigValue(configManager, PARAM_TIMEOUT_CONNECT, "750");
            try {
                this.connectTimeout = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new FilterConfigurationException("Unable to parse value into integer: " + value, e);
            }
        }
        {
            final String value = ConfigurationUtils.extractOptionalConfigValue(configManager, PARAM_TIMEOUT_READ, "1250");
            try {
                this.readTimeout = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new FilterConfigurationException("Unable to parse value into integer: " + value, e);
            }
        }
        {
            final String proxyHost = ConfigurationUtils.extractOptionalConfigValue(configManager, PARAM_INTERNET_PROXY_HOST, null);
            final String proxyPort = ConfigurationUtils.extractOptionalConfigValue(configManager, PARAM_INTERNET_PROXY_PORT, null);
            if (proxyHost == null && proxyPort == null) this.proxy = Proxy.NO_PROXY; else {
                if (proxyHost == null || proxyPort == null) throw new FilterConfigurationException("Proxy configuration requires both values (host and port) to be set");
                try {
                    final int port = Integer.parseInt(proxyPort);
                    this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port));
                } catch (NumberFormatException e) {
                    throw new FilterConfigurationException("Unable to parse proxy port value into integer: " + proxyPort, e);
                }
            }
        }
        this.servicePattern = ConfigurationUtils.extractOptionalConfigValue(configManager, PARAM_SERVICE_URL, "http://api.hostip.info/?ip={0}");
        this.xmlElementName = ConfigurationUtils.extractOptionalConfigValue(configManager, PARAM_XML_ELEMENT_COUNTRY_CODE, "countryAbbrev");
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isCachingOfNegativeRepliesAllowed() {
        return this.cachingOfNegativeRepliesAllowed;
    }

    public String getCountryCode(final String ip) throws GeoLocatingException {
        if (ip == null) return null;
        if (!WebCastellumFilter.PATTERN_VALID_CLIENT_ADDRESS.matcher(ip).matches()) return null;
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            final java.net.URL url = new java.net.URL(MessageFormat.format(this.servicePattern, new Object[] { ip }));
            HttpURLConnection.setFollowRedirects(true);
            connection = (HttpURLConnection) url.openConnection(this.proxy);
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(this.connectTimeout);
            connection.setReadTimeout(this.readTimeout);
            connection.connect();
            final int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new GeoLocatingException("Non-OK status code returned from geo-locating site: " + status);
            input = connection.getInputStream();
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            final NodeList countryAbbrevs = document.getElementsByTagName(this.xmlElementName);
            if (countryAbbrevs.getLength() == 0) throw new GeoLocatingException("No country containing XML received from geo-locating site");
            final String result = countryAbbrevs.item(0).getTextContent();
            if (DEBUG) System.out.println("DefaultGeoLocator: " + result);
            if ("XX".equalsIgnoreCase(result)) return null;
            return result;
        } catch (MalformedURLException e) {
            throw new GeoLocatingException("Malformed URL", e);
        } catch (SocketTimeoutException e) {
            throw new GeoLocatingException("Socket timeout", e);
        } catch (IOException e) {
            throw new GeoLocatingException("I/O failure", e);
        } catch (ParserConfigurationException e) {
            throw new GeoLocatingException("XML parser configuration failure", e);
        } catch (SAXException e) {
            throw new GeoLocatingException("XML parser (SAX) failure", e);
        } catch (RuntimeException e) {
            throw new GeoLocatingException("Unexpected runtime failure", e);
        } finally {
            if (input != null) try {
                input.close();
            } catch (IOException ignored) {
            }
            if (connection != null) try {
                connection.disconnect();
            } catch (RuntimeException ignored) {
            }
        }
    }
}

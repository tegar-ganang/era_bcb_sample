package org.gvsig.remoteClient.wcs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import org.gvsig.remoteClient.OGCProtocolHandler;
import org.gvsig.remoteClient.exceptions.ServerErrorException;
import org.gvsig.remoteClient.exceptions.WCSException;
import org.gvsig.remoteClient.utils.ExceptionTags;
import org.gvsig.remoteClient.utils.Utilities;
import org.gvsig.remoteClient.wms.ICancellable;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author jaume
 *
 */
public abstract class WCSProtocolHandler extends OGCProtocolHandler {

    /**
	 * Encoding used to parse different xml documents.
	 */
    protected String encoding = "UTF-8";

    protected Hashtable layerPool = new Hashtable();

    /**
     * WCS metadata
     */
    protected ServiceInformation serviceInfo = new ServiceInformation();

    public void setHost(String host) {
        try {
            new URL(host);
            int index = host.indexOf("?");
            if (index == -1) super.setHost(host); else super.setHost(host.substring(0, index));
        } catch (MalformedURLException m) {
            super.setHost(host);
        }
    }

    /**
     * <p>
     * Builds a GetCapabilities request that is sent to the WCS
     * the response will be parse to extract the data needed by the
     * WCS client.
     * </p>
     * @param override, if true the cache is ignored
     */
    public void getCapabilities(WCSStatus status, boolean override, ICancellable cancel) {
        URL request = null;
        try {
            request = new URL(buildCapabilitiesRequest(status));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (override) Utilities.removeURL(request);
            File f = Utilities.downloadFile(request, "wcs_capabilities.xml", cancel);
            if (f != null) parseCapabilities(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds a complete URL-string that can be used to send a GetCapabilities request.
     * @return String
     */
    private String buildCapabilitiesRequest(WCSStatus status) {
        StringBuffer req = new StringBuffer();
        String symbol = null;
        String onlineResource;
        if (status == null || status.getOnlineResource() == null) onlineResource = getHost(); else onlineResource = status.getOnlineResource();
        symbol = getSymbol(onlineResource);
        req.append(onlineResource).append(symbol).append("REQUEST=GetCapabilities&SERVICE=WCS&");
        req.append("VERSION=").append(getVersion()).append("&EXCEPTIONS=XML");
        return req.toString();
    }

    /**
     * Builds a complete URL-string that can be used to send a DescribeCoverage request.
     * If status is null, then default settings are used.
     * @param WCSStatus
     * @return String
     */
    private String buildDescribeCoverageRequest(WCSStatus status) {
        StringBuffer req = new StringBuffer();
        String symbol = null;
        String onlineResource;
        if (status == null || status.getOnlineResource() == null) onlineResource = getHost(); else onlineResource = status.getOnlineResource();
        symbol = getSymbol(onlineResource);
        req.append(onlineResource).append(symbol).append("REQUEST=DescribeCoverage&SERVICE=WCS&");
        if (status != null && status.getCoverageName() != null) req.append("COVERAGE=" + status.getCoverageName() + "&");
        req.append("VERSION=").append(getVersion()).append("&EXCEPTIONS=XML");
        return req.toString();
    }

    /**
     * parses the data retrieved by the DescribeCoverage XML document
     */
    public abstract boolean parseDescribeCoverage(File f);

    /**
     * Send a DescribeCoverage request using the settings passed in the status argument.
     * If status is null, then default settings are used.
     * @param override
     * @return String
     */
    public void describeCoverage(WCSStatus status, boolean override, ICancellable cancel) {
        URL request = null;
        try {
            request = new URL(buildDescribeCoverageRequest(status));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (override) Utilities.removeURL(request);
            File f = Utilities.downloadFile(request, "wcs_describeCoverage.xml", cancel);
            if (f != null) parseDescribeCoverage(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a GetCoverage request using the settings passed in the status.
     * @return String
     */
    public File getCoverage(WCSStatus status, ICancellable cancel) throws ServerErrorException, WCSException {
        URL request = null;
        try {
            request = new URL(buildCoverageRequest(status));
            File f = Utilities.downloadFile(request, "wcsGetCoverage", cancel);
            if (f != null && Utilities.isTextFile(f)) {
                FileInputStream fis = new FileInputStream(f);
                FileChannel fc = fis.getChannel();
                byte[] data = new byte[(int) fc.size()];
                ByteBuffer bb = ByteBuffer.wrap(data);
                fc.read(bb);
                WCSException wcsEx = null;
                String exceptionMessage = parseException(data);
                if (exceptionMessage == null) {
                    String error = new String(data);
                    int pos = error.indexOf("<?xml");
                    if (pos != -1) {
                        String xml = error.substring(pos, error.length());
                        exceptionMessage = parseException(xml.getBytes());
                    }
                    if (exceptionMessage == null) exceptionMessage = new String(data);
                }
                wcsEx = new WCSException(exceptionMessage);
                wcsEx.setWCSMessage(new String(data));
                Utilities.removeURL(request);
                throw wcsEx;
            }
            return f;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServerErrorException();
        }
    }

    /**
     * Parses the WCS Exception document.
     * @param bytes, byte[]
     * @return
     */
    private String parseException(byte[] data) {
        ArrayList errors = new ArrayList();
        KXmlParser kxmlParser = new KXmlParser();
        try {
            kxmlParser.setInput(new ByteArrayInputStream(data), encoding);
            kxmlParser.nextTag();
            int tag;
            if (kxmlParser.getEventType() != KXmlParser.END_DOCUMENT) {
                kxmlParser.require(KXmlParser.START_TAG, null, ExceptionTags.EXCEPTION_ROOT);
                tag = kxmlParser.nextTag();
                while (tag != KXmlParser.END_DOCUMENT) {
                    switch(tag) {
                        case KXmlParser.START_TAG:
                            if (kxmlParser.getName().compareTo(ExceptionTags.SERVICE_EXCEPTION) == 0) {
                                String errorCode = kxmlParser.getAttributeValue("", ExceptionTags.CODE);
                                errorCode = (errorCode != null) ? "[" + errorCode + "] " : "";
                                String errorMessage = kxmlParser.nextText();
                                errors.add(errorCode + errorMessage);
                            }
                            break;
                        case KXmlParser.END_TAG:
                            break;
                    }
                    tag = kxmlParser.nextTag();
                }
            }
        } catch (XmlPullParserException parser_ex) {
            parser_ex.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        String message = errors.size() > 0 ? "" : null;
        for (int i = 0; i < errors.size(); i++) {
            message += (String) errors.get(i) + "\n";
        }
        return message;
    }

    /**
     * Builds the GetMapRequest according to the OGC WCS Specifications
     */
    private String buildCoverageRequest(WCSStatus status) {
        StringBuffer req = new StringBuffer();
        String symbol = null;
        String onlineResource = null;
        if (status.getOnlineResource() == null) onlineResource = getHost(); else onlineResource = status.getOnlineResource();
        symbol = getSymbol(onlineResource);
        req.append(onlineResource + symbol + "service=WCS&version=").append(getVersion()).append("&request=GetCoverage&");
        req.append(getPartialQuery(status));
        if (status.getExceptionFormat() != null) {
            req.append("&EXCEPTIONS=" + status.getExceptionFormat());
        } else {
            req.append("&EXCEPTIONS=XML");
        }
        return req.toString().replaceAll(" ", "%20");
    }

    /**
     * Gets the part of the OGC request that share GetMap and GetFeatureInfo
     * @return String request
     */
    public String getPartialQuery(WCSStatus status) {
        StringBuffer req = new StringBuffer();
        req.append((status.getTime() != null) ? "TIME=" + status.getTime() : "").append("&COVERAGE=" + status.getCoverageName()).append("&CRS=" + status.getSrs()).append("&FORMAT=" + status.getFormat()).append("&HEIGHT=" + status.getHeight()).append("&WIDTH=" + status.getWidth()).append((status.getDepth() != null) ? "&DEPTH=" + status.getDepth() : "").append("&BBOX=" + status.getExtent().getMinX() + ",").append(status.getExtent().getMinY() + ",").append(status.getExtent().getMaxX() + ",").append(status.getExtent().getMaxY()).append((status.getParameters() != null) ? "&" + status.getParameters() : "");
        return req.toString();
    }

    /**
     * Builds the GetCapabilitiesRequest according to the OGC WCS Specifications
     * without a VERSION, to get the highest version than a WCS supports.
     */
    public static String buildCapabilitiesSuitableVersionRequest(String _host, String _version) {
        int index = _host.indexOf('?');
        if (index > -1) {
            String host = _host.substring(0, index + 1);
            String query = _host.substring(index + 1, _host.length());
            StringTokenizer tokens = new StringTokenizer(query, "&");
            String newQuery = "", token;
            while (tokens.hasMoreTokens()) {
                token = tokens.nextToken().trim();
                if (token.toUpperCase().compareTo("REQUEST=GETCAPABILITIES") == 0) continue;
                if (token.toUpperCase().compareTo("SERVICE=WCS") == 0) continue;
                if ((_version != null) && (_version.length() > 0)) {
                    if (token.toUpperCase().compareTo("VERSION=" + _version) == 0) continue;
                }
                if (token.toUpperCase().compareTo("EXCEPTIONS=XML") == 0) continue;
                newQuery += token + "&";
            }
            _host = host + newQuery;
        } else {
            _host += "?";
        }
        if ((_version != null) && (_version.compareTo("") != 0)) _host += "REQUEST=GetCapabilities&SERVICE=WCS&VERSION=" + _version + "&EXCEPTIONS=XML"; else _host += "REQUEST=GetCapabilities&SERVICE=WCS&EXCEPTIONS=XML";
        return _host.replaceAll(" ", "%20");
    }

    /**
     * Just for not repeat code. Gets the correct separator according to the server URL
     * @param h
     * @return
     */
    private static String getSymbol(String h) {
        String symbol;
        if (h.indexOf("?") == -1) symbol = "?"; else if (h.indexOf("?") != h.length() - 1) symbol = "&"; else symbol = "";
        return symbol;
    }

    public ArrayList getFormats() {
        return new ArrayList(serviceInfo.formats);
    }

    public class ServiceInformation {

        public String online_resource = null;

        public String version;

        public String name;

        public String scope;

        public String title;

        public String abstr;

        public String keywords;

        public String fees;

        public String operationsInfo;

        public String personname;

        public String organization;

        public String function;

        public String addresstype;

        public String address;

        public String place;

        public String province;

        public String postcode;

        public String country;

        public String phone;

        public String fax;

        public String email;

        public Vector formats;

        public HashMap operations;

        public ServiceInformation() {
            version = new String();
            name = new String();
            scope = new String();
            title = new String();
            abstr = new String();
            keywords = new String();
            fees = new String();
            operationsInfo = new String();
            personname = new String();
            organization = new String();
            function = new String();
            addresstype = new String();
            address = new String();
            place = new String();
            province = new String();
            postcode = new String();
            country = new String();
            phone = new String();
            fax = new String();
            email = new String();
            operations = new HashMap();
        }
    }

    public Hashtable getLayers() {
        return layerPool;
    }
}

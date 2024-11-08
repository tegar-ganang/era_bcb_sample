package org.gvsig.remoteClient.wms;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import org.gvsig.remoteClient.exceptions.ServerErrorException;
import org.gvsig.remoteClient.exceptions.WMSException;
import org.gvsig.remoteClient.utils.CapabilitiesTags;
import org.gvsig.remoteClient.utils.ExceptionTags;
import org.gvsig.remoteClient.utils.Utilities;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * <p> Abstract class that represents handlers to comunicate via WMS protocol.
 * </p>
 *
 */
public abstract class WMSProtocolHandler {

    /**
	 * Encoding used to parse different xml documents.
	 */
    protected String encoding = "UTF-8";

    /**
	 * procotol handler name
	 */
    protected String name;

    /**
     * protocol handler version
     */
    protected String version;

    /**
     * host of the WMS to connect
     */
    protected String host;

    /**
     * port number of the comunication channel of the WMS to connect
     */
    protected String port;

    /**
     * WMS metadata
     */
    protected ServiceInformation serviceInfo;

    public TreeMap layers;

    public WMSLayer rootLayer;

    /**
     * parses the data retrieved by the WMS in XML format.
     * It will be mostly the WMS Capabilities, but the implementation
     * will be placed in the handler implementing certain version of the protocol.
     *
     */
    public abstract void parse(File f);

    /**
     * returns the alfanumeric information of the layers at the specified point.
     * the diference between the other getfeatureInfo method is that this will
     * be implemented by each specific version because the XML from the server will be
     * parsed and presented by a well known structure.
     */
    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ServiceInformation getServiceInformation() {
        return serviceInfo;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String _host) {
        host = _host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String _port) {
        port = _port;
    }

    /**
	 * <p>Builds a GetCapabilities request that is sent to the WMS
	 * the response will be parse to extract the data needed by the
	 * WMS client</p>
	 * @param override, if true the previous downloaded data will be overridden
	 */
    public void getCapabilities(WMSStatus status, boolean override, ICancellable cancel) {
        URL request = null;
        try {
            request = new URL(buildCapabilitiesRequest(status));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (override) Utilities.removeURL(request);
            File f = Utilities.downloadFile(request, "wms_capabilities.xml", cancel);
            if (f == null) return;
            clear();
            parse(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clear() {
        layers.clear();
        serviceInfo.clear();
    }

    /**
     * <p>It will send a GetFeatureInfo request to the WMS
     * Parsing the response and redirecting the info to the WMS client</p>
     * TODO: return a stored file instead a String.
     */
    public String getFeatureInfo(WMSStatus status, int x, int y, int featureCount, ICancellable cancel) {
        URL request = null;
        StringBuffer output = new StringBuffer();
        String outputFormat = new String();
        String ServiceException = "ServiceExceptionReport";
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        try {
            request = new URL(buildGetFeatureInfoRequest(status, x, y));
            outputFormat = request.openConnection().getContentType();
            File f = Utilities.downloadFile(request, "wms_feature_info.xml", cancel);
            if (f == null) return "";
            FileReader fReader = new FileReader(f);
            char[] buffer = new char[1024 * 256];
            for (int i = fReader.read(buffer); i > 0; i = fReader.read(buffer)) {
                String str = new String(buffer, 0, i);
                output.append(str);
            }
            if ((outputFormat == null) || (outputFormat.indexOf("xml") != -1) || output.toString().toLowerCase().startsWith("<?xml") || (outputFormat.indexOf("gml") != -1)) {
                int tag;
                KXmlParser kxmlParser = null;
                kxmlParser = new KXmlParser();
                kxmlParser.setInput(new FileReader(f));
                tag = kxmlParser.nextTag();
                if (kxmlParser.getName().compareTo(ServiceException) == 0) {
                    sb.append("<INFO>").append(parseException(output.toString().getBytes())).append("</INFO>");
                    return sb.toString();
                } else if (kxmlParser.getName().compareToIgnoreCase("ERROR") == 0) {
                    return output.toString();
                } else {
                    return output.toString();
                }
            } else {
                return output.toString();
            }
        } catch (XmlPullParserException parserEx) {
            if (output.toString().toLowerCase().indexOf("xml") != -1) {
                return output.toString().trim();
            } else {
                sb.append("<INFO>").append("Info format not supported").append("</INFO>");
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            sb.append("<INFO>").append("Info format not supported").append("</INFO>");
            return sb.toString();
        }
    }

    /**
     * <p>Builds a GetMap request that is sent to the WMS
     * the response (image) will be redirect to the
     * WMS client</p>
     */
    public byte[] _getMap(WMSStatus status) throws ServerErrorException, WMSException {
        URL request = null;
        try {
            request = new URL(buildMapRequest(status));
            URLConnection conn = request.openConnection();
            System.out.println(request.toString());
            String type = conn.getContentType();
            byte[] imageBytes = null;
            byte[] buffer = new byte[1024 * 256];
            InputStream is = conn.getInputStream();
            int readed = 0;
            for (int i = is.read(buffer); i > 0; i = is.read(buffer)) {
                byte[] buffered = new byte[readed + i];
                for (int j = 0; j < buffered.length; j++) {
                    if (j < readed) {
                        buffered[j] = imageBytes[j];
                    } else {
                        buffered[j] = buffer[j - readed];
                    }
                }
                imageBytes = (byte[]) buffered.clone();
                readed += i;
            }
            if ((type != null && !type.subSequence(0, 5).equals("image")) || (Utilities.isTextData(imageBytes))) {
                WMSException wmsEx = null;
                String exceptionMessage = parseException(imageBytes);
                if (exceptionMessage == null) {
                    String error = new String(imageBytes);
                    int pos = error.indexOf("<?xml");
                    if (pos != -1) {
                        String xml = error.substring(pos, error.length());
                        exceptionMessage = parseException(xml.getBytes());
                        if (exceptionMessage == null) exceptionMessage = new String(imageBytes);
                    }
                }
                wmsEx = new WMSException(exceptionMessage);
                wmsEx.setWMSMessage(new String(imageBytes));
                throw wmsEx;
            }
            return imageBytes;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServerErrorException();
        }
    }

    public File getLegendGraphic(WMSStatus status, String layerName, ICancellable cancel) throws ServerErrorException, WMSException {
        URL request = null;
        try {
            request = new URL(buildGetLegendGraphicRequest(status, layerName));
            System.out.println(request);
            File f = Utilities.downloadFile(request, "wmsGetLegendGraphic", cancel);
            if (f == null) return null;
            if (Utilities.isTextFile(f)) {
                FileInputStream fis = new FileInputStream(f);
                FileChannel fc = fis.getChannel();
                byte[] data = new byte[(int) fc.size()];
                ByteBuffer bb = ByteBuffer.wrap(data);
                fc.read(bb);
                WMSException wmsEx = null;
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
                wmsEx = new WMSException(exceptionMessage);
                wmsEx.setWMSMessage(new String(data));
                Utilities.removeURL(request);
                throw wmsEx;
            }
            return f;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServerErrorException();
        }
    }

    public File getMap(WMSStatus status, ICancellable cancel) throws ServerErrorException, WMSException {
        URL request = null;
        try {
            request = new URL(buildMapRequest(status));
            File f = Utilities.downloadFile(request, "wmsGetMap", cancel);
            if (f == null) return null;
            if (Utilities.isTextFile(f)) {
                FileInputStream fis = new FileInputStream(f);
                FileChannel fc = fis.getChannel();
                byte[] data = new byte[(int) fc.size()];
                ByteBuffer bb = ByteBuffer.wrap(data);
                fc.read(bb);
                WMSException wmsEx = null;
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
                wmsEx = new WMSException(exceptionMessage);
                wmsEx.setWMSMessage(new String(data));
                Utilities.removeURL(request);
                throw wmsEx;
            }
            return f;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServerErrorException();
        }
    }

    protected String parseException(byte[] data) {
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
     * Builds the GetCapabilitiesRequest according to the OGC WMS Specifications
     * without a VERSION, to get the highest version than a WMS supports.
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
                if (token.toUpperCase().compareTo("SERVICE=WMS") == 0) continue;
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
        if ((_version != null) && (_version.compareTo("") != 0)) _host += "REQUEST=GetCapabilities&SERVICE=WMS&VERSION=" + _version + "&EXCEPTIONS=XML"; else _host += "REQUEST=GetCapabilities&SERVICE=WMS&EXCEPTIONS=XML";
        return _host;
    }

    /**
     * Builds the GetCapabilitiesRequest according to the OGC WMS Specifications
     * @param WMSStatus
     */
    private String buildCapabilitiesRequest(WMSStatus status) {
        StringBuffer req = new StringBuffer();
        String symbol = null;
        String onlineResource;
        if (status == null || status.getOnlineResource() == null) onlineResource = getHost(); else onlineResource = status.getOnlineResource();
        symbol = getSymbol(onlineResource);
        req.append(onlineResource).append(symbol).append("REQUEST=GetCapabilities&SERVICE=WMS&");
        req.append("VERSION=").append(getVersion());
        return req.toString();
    }

    /**
     * Builds the GetFeatureInfoRequest according to the OGC WMS Specifications
     */
    protected String buildGetFeatureInfoRequest(WMSStatus status, int x, int y) {
        StringBuffer req = new StringBuffer();
        String symbol = null;
        String onlineResource;
        if (status.getOnlineResource() == null) onlineResource = getHost(); else onlineResource = status.getOnlineResource();
        symbol = getSymbol(onlineResource);
        req.append(onlineResource).append(symbol).append("REQUEST=GetFeatureInfo&SERVICE=WMS&");
        req.append("QUERY_LAYERS=").append(Utilities.Vector2CS(status.getLayerNames()));
        req.append("&VERSION=").append(getVersion()).append("&INFO_FORMAT=application/vnd.ogc.gml&");
        req.append(getPartialQuery(status)).append("&x=" + x + "&y=" + y);
        req.append("&FEATURE_COUNT=10000");
        return req.toString().replaceAll(" ", "%20");
    }

    /**
     * @return string that represents the url for getting the wms legend
     * If the layer has the object layer-->style-->legendurl that url will be returned 
     * otherwise builds the getLegendGraphic according to the OGC WMS Specifications
     * 
     */
    private String buildGetLegendGraphicRequest(WMSStatus status, String layerName) {
        WMSLayer lyr = (WMSLayer) this.layers.get(layerName);
        WMSStyle sty = null;
        if (lyr != null) {
            Iterator it = lyr.getStyles().iterator();
            while (it.hasNext()) {
                sty = (WMSStyle) it.next();
                if (sty.getName().equals(status.getStyles().get(0))) {
                    return sty.getLegendURLOnlineResourceHRef();
                }
            }
        }
        StringBuffer req = new StringBuffer();
        String symbol = null;
        String onlineResource = null;
        if (status.getOnlineResource() == null) onlineResource = getHost(); else onlineResource = status.getOnlineResource();
        symbol = getSymbol(onlineResource);
        req.append(onlineResource + symbol + "REQUEST=GetLegendGraphic&SERVICE=WMS&VERSION=").append(getVersion());
        req.append("&LAYER=" + layerName).append("&TRANSPARENT=TRUE").append("&FORMAT=image/png");
        return req.toString().replaceAll(" ", "%20");
    }

    /**
     * Builds the GetMapRequest according to the OGC WMS Specifications
     */
    private String buildMapRequest(WMSStatus status) {
        StringBuffer req = new StringBuffer();
        String symbol = null;
        String onlineResource = null;
        if (status.getOnlineResource() == null) onlineResource = getHost(); else onlineResource = status.getOnlineResource();
        symbol = getSymbol(onlineResource);
        req.append(onlineResource + symbol + "REQUEST=GetMap&SERVICE=WMS&VERSION=").append(getVersion()).append("&");
        req.append(getPartialQuery(status));
        return req.toString().replaceAll(" ", "%20");
    }

    /**
     * Just for not repeat code. Gets the correct separator according to the server URL
     * @param h
     * @return
     */
    protected static String getSymbol(String h) {
        String symbol;
        if (h.indexOf("?") == -1) symbol = "?"; else if (h.indexOf("?") != h.length() - 1) symbol = "&"; else symbol = "";
        return symbol;
    }

    /**
     * Gets the part of the OGC request that share GetMap and GetFeatureInfo
     * @return String request
     */
    public String getPartialQuery(WMSStatus status) {
        StringBuffer req = new StringBuffer();
        req.append("LAYERS=" + Utilities.Vector2CS(status.getLayerNames())).append("&" + getSRSParameter() + "=" + status.getSrs()).append("&BBOX=" + status.getExtent().getMinX() + ",").append(status.getExtent().getMinY() + ",").append(status.getExtent().getMaxX() + ",").append(status.getExtent().getMaxY()).append("&WIDTH=" + status.getWidth()).append("&HEIGHT=" + status.getHeight()).append("&FORMAT=" + status.getFormat()).append("&STYLES=");
        Vector v = status.getStyles();
        if (v != null && v.size() > 0) req.append(Utilities.Vector2CS(v));
        v = status.getDimensions();
        if (v != null && v.size() > 0) req.append("&" + Utilities.Vector2URLParamString(v));
        if (status.getTransparency()) {
            req.append("&TRANSPARENT=TRUE");
        }
        return req.toString();
    }

    /**
     * @return the parameter for the SRS/CRS
     */
    protected String getSRSParameter() {
        return "SRS";
    }

    public void close() {
    }

    /**
     * Inner class that represents the description of the WMS metadata.
     * The first part of the capabilities will return the service information
     * from the WMS, this class will hold this information.
     *
     */
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
            formats = new Vector();
            operations = new HashMap();
        }

        public boolean isQueryable() {
            if (operations.keySet().contains(CapabilitiesTags.GETFEATUREINFO)) return true; else return false;
        }

        public boolean hasLegendGraphic() {
            if (operations.keySet().contains(CapabilitiesTags.GETLEGENDGRAPHIC)) return true; else return false;
        }

        public void clear() {
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
            formats = new Vector();
            operations = new HashMap();
        }
    }

    /**
     * @return true if the layer has legendurl (layer-->style object in capabilities)
     *  returns false when more than one layer is selected
     *   
     */
    public boolean hasLegendUrl(WMSStatus status, String layerName) {
        WMSLayer lyr = (WMSLayer) this.layers.get(layerName);
        WMSStyle style = null;
        if (lyr != null) {
            Iterator it = lyr.getStyles().iterator();
            while (it.hasNext()) {
                style = (WMSStyle) it.next();
                if (style.getName().equals(status.getStyles().get(0))) {
                    return true;
                }
            }
        }
        return false;
    }
}

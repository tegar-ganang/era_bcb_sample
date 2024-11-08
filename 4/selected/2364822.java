package org.xaware.server.engine.instruction.bizcomps.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.httpclient.NameValuePair;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;
import org.springframework.core.io.Resource;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IChannelSpecification;
import org.xaware.server.engine.context.AbstractConfigTranslator;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.server.engine.instruction.bizcomps.http.HttpConfigInfo.RequestType;
import org.xaware.server.resources.ResourceHelper;
import org.xaware.shared.util.EncodingHelper;
import org.xaware.shared.util.ExceptionMessageHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class takes info from a HttpBizComponent and fills out HttpConfigInfo objects representing work to do.
 * 
 * @author jtarnowski
 */
public class HttpConfigTranslator extends AbstractConfigTranslator {

    /** XAwareLogger instance for HttpConfigTranslator. */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(HttpConfigTranslator.class.getName());

    /** Constant for request */
    public static final String REQUEST_ELEMENT_NAME = XAwareConstants.BIZCOMPONENT_ELEMENT_REQUEST;

    /** Constant for HttpConfigTranslator */
    private static final String classLocation = "HttpConfigTranslator";

    /** Namespace for convenience */
    private static final Namespace ns = XAwareConstants.xaNamespace;

    /** Holds HttpConfigInfo.RequestType instance */
    private HttpConfigInfo.RequestType requestType;

    /**
     * Constructor
     * @param configElement Element containing the configuration information
     * @param context Context used in substitution.
     * @param logger {@link XAwareLogger} used for logging statements.
     * @throws XAwareConfigurationException
     * @throws XAwareSubstitutionException
     *             when the attributes associated with the found element can not be substituted
     */
    public HttpConfigTranslator(final Element configElement, final IBizViewContext context, final XAwareLogger logger) throws XAwareException {
        super(context, logger, classLocation, configElement);
        setRequestType(configElement);
    }

    /**
     * @retuns XAwareConstants.REQUEST_ELEMENT_NAME
     * @see org.xaware.server.engine.context.BaseConfigTranslator#getElementNameConstant()
     */
    @Override
    public String getElementNameConstant() {
        return REQUEST_ELEMENT_NAME;
    }

    void setRequestType(Element requestElem) throws XAwareException {
        String reqType = requestElem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_REQUEST_TYPE, ns);
        if (XAwareConstants.BIZCOMPONENT_HTTP_GET.equals(reqType)) {
            requestType = HttpConfigInfo.RequestType.GET;
        } else if (XAwareConstants.BIZCOMPONENT_HTTP_POST.equals(reqType)) {
            requestType = HttpConfigInfo.RequestType.POST;
        } else if (XAwareConstants.BIZCOMPONENT_HTTP_MULTIPLE.equals(reqType)) {
            requestType = HttpConfigInfo.RequestType.MULTI;
        } else if (XAwareConstants.BIZCOMPONENT_HTTP_PUT.equals(reqType)) {
            requestType = HttpConfigInfo.RequestType.PUT;
        } else if (XAwareConstants.BIZCOMPONENT_HTTP_DELETE.equals(reqType)) {
            requestType = HttpConfigInfo.RequestType.DELETE;
        } else {
            throw new XAwareException("xa:request_type must be get, post or multiple");
        }
    }

    /**
     * Get all of the config objects that we will deal with.
     * 
     * @param request -
     *            Element
     * @param driver -
     *            HttpBizDriver that has channel spec
     * @param requestType -
     *            GET, POST or MULTI
     * @return List of HttpConfigInfo objects
     * @throws XAwareException
     */
    public List<HttpConfigInfo> getConfigurationList(IBizDriver driver) throws XAwareException {
        List<HttpConfigInfo> list = new ArrayList<HttpConfigInfo>();
        switch(requestType) {
            case GET:
                setUpGet(m_configElement, driver, list);
                break;
            case POST:
                setUpPost(m_configElement, driver, list);
                break;
            case MULTI:
                setUpMulti(m_configElement, driver, list);
                break;
            case DELETE:
                setUpDelete(m_configElement, driver, list);
                break;
            case PUT:
                setUpPut(m_configElement, driver, list);
                break;
        }
        return list;
    }

    /**
     * Create a HttpConfigInfo for a GET, populate it, and put it on the list
     * 
     * @param request
     *            Element
     * @param driver
     *            HttpBizDriver
     * @param list
     *            List that has already been allocated
     */
    public void setUpGet(Element request, IBizDriver driver, List<HttpConfigInfo> list) throws XAwareException {
        HttpConfigInfo config = new HttpConfigInfo();
        IChannelSpecification spec = driver.getChannelSpecification();
        config.setUrl(spec.getProperty(XAwareConstants.BIZDRIVER_URL));
        config.setRequestType(RequestType.GET);
        setUpCommonGetPost(request, spec, config);
        list.add(config);
    }

    /**
     * Create a HttpConfigInfo for a DELETE, populate it, and put it on the list
     * 
     * @param request
     *            Element
     * @param driver
     *            HttpBizDriver
     * @param list
     *            List that has already been allocated
     */
    public void setUpDelete(Element request, IBizDriver driver, List<HttpConfigInfo> list) throws XAwareException {
        HttpConfigInfo config = new HttpConfigInfo();
        IChannelSpecification spec = driver.getChannelSpecification();
        config.setUrl(spec.getProperty(XAwareConstants.BIZDRIVER_URL));
        config.setRequestType(RequestType.DELETE);
        setUpCommonGetPost(request, spec, config);
        list.add(config);
    }

    /**
     * Create a HttpConfigInfo for a POST, populate it, and put it on the list
     * 
     * @param request
     *            Element
     * @param driver
     *            HttpBizDriver
     * @param list
     *            List that has already been allocated
     */
    @SuppressWarnings("unchecked")
    public void setUpPost(Element request, IBizDriver driver, List<HttpConfigInfo> list) throws XAwareException {
        HttpConfigInfo config = new HttpConfigInfo();
        IChannelSpecification spec = driver.getChannelSpecification();
        config.setUrl(spec.getProperty(XAwareConstants.BIZDRIVER_URL));
        config.setRequestType(RequestType.POST);
        setUpCommonGetPost(request, spec, config);
        config.setBoundary(request.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_BOUNDARY, ns));
        list.add(config);
    }

    /**
     * Create a HttpConfigInfo for a PUT, populate it, and put it on the list
     * 
     * @param request
     *            Element
     * @param driver
     *            HttpBizDriver
     * @param list
     *            List that has already been allocated
     */
    @SuppressWarnings("unchecked")
    public void setUpPut(Element request, IBizDriver driver, List<HttpConfigInfo> list) throws XAwareException {
        HttpConfigInfo config = new HttpConfigInfo();
        IChannelSpecification spec = driver.getChannelSpecification();
        config.setUrl(spec.getProperty(XAwareConstants.BIZDRIVER_URL));
        config.setRequestType(RequestType.PUT);
        setUpCommonGetPost(request, spec, config);
        config.setBoundary(request.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_BOUNDARY, ns));
        list.add(config);
    }

    /**
     * Populate values common to both get and post operations.
     * 
     * @param request
     *            Element
     * @param spec
     *            IChannelSpecification
     * @param config
     *            HttpConfigInfo that you wish to populate
     * @throws XAwareException
     */
    @SuppressWarnings("unchecked")
    public void setUpCommonGetPost(Element request, IChannelSpecification spec, HttpConfigInfo config) throws XAwareException {
        config.setUser(spec.getProperty(XAwareConstants.BIZDRIVER_USER));
        config.setPwd(spec.getProperty(XAwareConstants.BIZDRIVER_PWD));
        config.setHost(spec.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_HOST));
        config.setPort(spec.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PORT));
        config.setProxyUid(spec.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER));
        config.setProxyPwd(spec.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD));
        config.setProxyHost(spec.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYHOST));
        config.setProxyPort(spec.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPORT));
        config.setResultName(request.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_RESULT_NAME, ns));
        config.setOutputType(request.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_OUTPUT_TYPE, ns));
        config.setOutputFile(request.getAttributeValue(HttpConfigInfo.HTTP_ATTRIBUTE_LOCAL_FILE, ns));
        config.setEncoding(request.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_ENCODE, ns));
        request.removeAttribute(XAwareConstants.BIZCOMPONENT_ATTR_ENCODE, ns);
        config.setMessage_Charset(request.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_MESSAGE_CHARSET, ns));
        List<Attribute> attribs = request.getAttributes();
        for (Attribute attr : attribs) {
            if (!ns.equals(attr.getNamespace())) {
                NameValuePair pair = new NameValuePair(attr.getName(), attr.getValue());
                config.addParam(pair);
            }
        }
        List<Element> headers = request.getChildren("header", ns);
        int size = headers.size();
        if (size > 0) {
            List<String> headerStrings = new ArrayList<String>(size);
            for (Element element : headers) {
                headerStrings.add(element.getValue());
            }
            config.setHeaders(headerStrings);
        }
    }

    /**
     * Handle setting up configuration for MULTI
     * 
     * @param request
     *            Element
     * @param driver
     *            HttpBizDriver
     * @param list
     *            List that has already been allocated
     */
    @SuppressWarnings("unchecked")
    public void setUpMulti(Element request, IBizDriver driver, List<HttpConfigInfo> list) throws XAwareException {
        IChannelSpecification spec = driver.getChannelSpecification();
        List<Element> elements = request.getChildren();
        for (Element child : elements) {
            if (child.getNamespace().equals(XAwareConstants.xaNamespace)) {
                if (XAwareConstants.BIZCOMPONENT_HTTP_GET.equals(child.getName())) {
                    list.add(setUpOneMultiGet(child, spec));
                } else if (XAwareConstants.BIZCOMPONENT_HTTP_POST.equals(child.getName())) {
                    list.add(setUpOneMultiPost(child, spec));
                }
            }
        }
    }

    /**
     * Parse all the values needed for a single GET within a multi request
     * 
     * @param get
     *            Element
     * @param driver
     *            HttpBizDriver
     * @return HttpConfigInfo
     */
    HttpConfigInfo setUpOneMultiGet(Element elem, IChannelSpecification spec) throws XAwareException {
        HttpConfigInfo config = null;
        config = new HttpConfigInfo();
        config.setRequestType(RequestType.GET);
        config.setUrl(spec.getProperty("base_url") + elem.getAttributeValue("path", ns));
        setUpCommonGetPost(elem, spec, config);
        Element returnElem = elem.getChild("return", ns);
        config.setReturnElement(returnElem);
        if (returnElem != null) {
            config.setDataType(returnElem.getAttributeValue("datatype", ns));
        }
        return config;
    }

    /**
     * Parse all the values needed for a single POST within a multi request
     * 
     * @param get
     *            Element
     * @param driver
     *            HttpBizDriver
     * @return HttpConfigInfo
     */
    HttpConfigInfo setUpOneMultiPost(Element elem, IChannelSpecification spec) throws XAwareException {
        HttpConfigInfo config = null;
        config = new HttpConfigInfo();
        config.setRequestType(RequestType.POST);
        config.setUrl(spec.getProperty("base_url") + elem.getAttributeValue("path", ns));
        config.setPath(elem.getAttributeValue("path", ns));
        setUpCommonGetPost(elem, spec, config);
        Element returnElem = elem.getChild("return", ns);
        config.setReturnElement(returnElem);
        if (returnElem != null) {
            config.setDataType(returnElem.getAttributeValue("datatype", ns));
        }
        config.setBoundary(elem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_BOUNDARY, ns));
        return config;
    }

    /**
     * Get the string to post to the server
     * @param parent
     * @param boundary
     * @param config
     * @return
     * @throws XAwareException
     */
    @SuppressWarnings("unchecked")
    public String getPostString(Element parent, HttpConfigInfo config) throws XAwareException {
        String postString = "";
        List<Element> sections = parent.getChildren();
        for (Element elem : sections) {
            if (elem.getName().equals(XAwareConstants.BIZCOMPONENT_ELEMENT_SECTION)) {
                postString += parseSectionElement(elem, config);
            } else if (elem.getName().equalsIgnoreCase(XAwareConstants.BIZCOMPONENT_ELEMENT_CONTENT)) {
                postString += parseContentElement(elem, config);
            }
        }
        return postString;
    }

    /**
     * Get the string to put to the server
     * @param parent
     * @param boundary
     * @param config
     * @return
     * @throws XAwareException
     */
    @SuppressWarnings("unchecked")
    public String getPutString(Element parent, HttpConfigInfo config) throws XAwareException {
        String putString = "";
        List<Element> sections = parent.getChildren();
        for (Element elem : sections) {
            if (elem.getName().equals(XAwareConstants.BIZCOMPONENT_ELEMENT_SECTION)) {
                putString += parseSectionElement(elem, config);
            } else if (elem.getName().equalsIgnoreCase(XAwareConstants.BIZCOMPONENT_ELEMENT_CONTENT)) {
                putString += parseContentElement(elem, config);
            }
        }
        return putString;
    }

    /**
     * Parse each section of the request element
     * @param sectionElem Element named xa:section
     * @param boundary - String of boundary characters
     * @return String representing the section
     * @throws XAwareException
     */
    private String parseSectionElement(Element sectionElem, HttpConfigInfo config) throws XAwareException {
        String sReturn = "";
        Iterator iter = sectionElem.getChildren().iterator();
        Element elem;
        boolean firstContent = true;
        String boundary = config.getBoundary();
        if (boundary != null && boundary.length() > 0) sReturn += "\r\n--" + boundary;
        while (iter.hasNext()) {
            elem = (Element) iter.next();
            if (elem.getName().equalsIgnoreCase(XAwareConstants.BIZCOMPONENT_ELEMENT_HEADER)) {
                sReturn += "\r\n" + this.m_context.substitute(elem.getText(), elem, null, m_subFailureLevel);
            } else if (elem.getName().equalsIgnoreCase(XAwareConstants.BIZCOMPONENT_ELEMENT_CONTENT)) {
                if (firstContent) {
                    sReturn += "\r\n\r\n";
                    firstContent = false;
                }
                sReturn += parseContentElement(elem, config);
            }
        }
        String charset = sectionElem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_MESSAGE_CHARSET, XAwareConstants.xaNamespace);
        if (charset != null) {
            try {
                sReturn = new String(sReturn.getBytes(charset));
            } catch (UnsupportedEncodingException exception) {
                String errorMessage = "UnsupportedEncodingException : " + ExceptionMessageHelper.getExceptionMessage(exception);
                logger.severe(errorMessage, exception);
                throw new XAwareException(errorMessage, exception);
            }
        }
        return sReturn;
    }

    /**
     * Parses the content of the element and puts it in a string
     * @param elem - Element
     * @return String
     * @throws XAwareException
     */
    private String parseContentElement(Element elem, HttpConfigInfo config) throws XAwareException {
        String sOutput = "";
        StringBuffer sDataBuffer = new StringBuffer(1024);
        StringBuffer contentBuffer = new StringBuffer(1024);
        String sFileNm = elem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_FILENAME, ns);
        if (sFileNm != null && sFileNm.length() > 0) {
            sFileNm = m_context.substitute(sFileNm, elem, null, m_subFailureLevel);
            try {
                if (sFileNm != null && sFileNm.length() > 0) {
                    Resource resource = ResourceHelper.getResource(sFileNm);
                    InputStream inStream = resource.getInputStream();
                    if (inStream == null) {
                        return "";
                    }
                    BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        sOutput += inputLine;
                        sOutput += "\r\n";
                    }
                    contentBuffer.append(sOutput);
                } else {
                }
            } catch (java.io.FileNotFoundException e) {
                return "";
            } catch (java.io.IOException e) {
                return "";
            }
        }
        sDataBuffer = processXAContentElement(elem, config);
        String dataType = elem.getAttributeValue(XAwareConstants.XAWARE_ATTR_DATATYPE, ns);
        if (dataType == null) dataType = XAwareConstants.XAWARE_DATATYPE_XML; else dataType = substitute(dataType);
        if (dataType != null && dataType.equals(XAwareConstants.XAWARE_DATATYPE_BINARY)) {
            try {
                sun.misc.BASE64Decoder base64d = new sun.misc.BASE64Decoder();
                contentBuffer.append(new String(base64d.decodeBuffer(sDataBuffer.toString())));
            } catch (java.io.IOException e) {
                return "";
            }
        } else contentBuffer.append(sDataBuffer);
        String charset = elem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_MESSAGE_CHARSET, XAwareConstants.xaNamespace);
        if (charset != null) {
            String toReturn = "";
            try {
                toReturn = new String(contentBuffer.toString().getBytes(charset));
            } catch (UnsupportedEncodingException exception) {
                String errorMessage = "UnsupportedEncodingException : " + ExceptionMessageHelper.getExceptionMessage(exception);
                logger.severe(errorMessage, exception);
                throw new XAwareException(errorMessage, exception);
            }
            return toReturn;
        } else return contentBuffer.toString();
    }

    /**
     * Add substituted content to a StringBuffer
     * 
     * @param contentElement
     *            Element
     * @return StringBuffer
     * @throws XAwareException
     */
    protected StringBuffer processXAContentElement(final Element contentElement, HttpConfigInfo config) throws XAwareException {
        final StringBuffer sContent = new StringBuffer(1024);
        final Iterator iter = contentElement.getContent().iterator();
        while (iter.hasNext()) {
            final Object obj = iter.next();
            if (obj instanceof CDATA) {
                sContent.append(((CDATA) obj).getText());
            } else if (obj instanceof Element) {
                final Element tempelem = (Element) obj;
                final String encodeValue = tempelem.getAttributeValue(XAwareConstants.BIZCOMPONENT_ATTR_ENCODE, ns);
                if (XAwareConstants.XAWARE_YES.equals(config.getEncoding()) || ((encodeValue != null) && (encodeValue.length() > 0) && (encodeValue.equals(XAwareConstants.XAWARE_YES)))) {
                    final XMLOutputter out = new XMLOutputter();
                    sContent.append(EncodingHelper.urlEncodeText(out.outputString(tempelem), null));
                } else {
                    final XMLOutputter out = new XMLOutputter();
                    sContent.append(out.outputString(tempelem));
                }
            } else if ((obj instanceof Text) || (obj instanceof String)) {
                String sobj = null;
                if (obj instanceof Text) {
                    sobj = ((Text) obj).getText();
                } else {
                    sobj = (String) obj;
                }
                sobj = m_context.substitute(sobj.trim(), contentElement, null, m_subFailureLevel);
                if (XAwareConstants.XAWARE_YES.equals(config.getEncoding())) {
                    sobj = EncodingHelper.urlEncodeText(sobj, null);
                }
                sContent.append(sobj);
            }
        }
        return sContent;
    }

    /**
     * @return the requestType
     */
    public RequestType getRequestType() {
        return requestType;
    }
}

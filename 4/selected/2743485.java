package org.xaware.server.engine.channel.soap;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.jdom.Element;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IChannelSpecification;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractChannelSpecification;
import org.xaware.server.engine.channel.LocalChannelKey;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;

/**
 * This is the implementation of the SOAP channel specification. It uses Element passed in from the bizdriver,
 * and retrieves the Http client properties from it.
 * 
 * @author Basil Ibegbu
 */
public class SoapChannelSpecification extends AbstractChannelSpecification implements IChannelSpecification {

    /**  */
    private static final String CHANNEL_SPEC_ELEMENT_NAME = "http";

    /**
     * The unique key identifying this instance of the bizDriver - for caching purposes
     */
    private IChannelKey key = null;

    private String m_uid = null;

    private String m_pwd = null;

    private String m_urlString = null;

    private String m_proxyUser = null;

    private String m_proxyPwd = null;

    private String m_proxyHost = null;

    private int m_proxyPort = AuthScope.ANY_PORT;

    private int m_timeout = -1;

    private Element m_channelSpecElement;

    /**
     * This implementation produces a {@link SoapTemplate} object used by the {@link SoapTemplateFactory}
     */
    public Object getChannelObject() throws XAwareException {
        HttpClient httpClient = new HttpClient();
        HostConfiguration hostConfiguration = new HostConfiguration();
        try {
            hostConfiguration.setHost(new URI(m_urlString, false));
        } catch (Exception e) {
            throw new XAwareException("Failed to parse the url: " + m_urlString, e);
        }
        httpClient.setHostConfiguration(hostConfiguration);
        if (m_timeout >= 0) {
            httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(m_timeout);
        }
        HttpState httpState = null;
        if (m_uid != null && !"".equals(m_uid.trim())) {
            AuthScope authScope = new AuthScope(hostConfiguration.getHost(), hostConfiguration.getPort());
            if (httpState == null) {
                httpState = new HttpState();
            }
            httpState.setCredentials(authScope, new UsernamePasswordCredentials(m_uid, m_pwd));
        }
        if (m_proxyUser != null && !"".equals(m_proxyUser.trim())) {
            AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
            if (m_proxyHost != null && !"".equals(m_proxyHost.trim())) {
                authScope = new AuthScope(m_proxyHost, m_proxyPort);
            }
            if (httpState == null) {
                httpState = new HttpState();
            }
            httpState.setProxyCredentials(authScope, new UsernamePasswordCredentials(m_proxyUser, m_proxyPwd));
        }
        if (httpState != null) {
            httpClient.setState(httpState);
        }
        SoapTemplate template = new SoapTemplate(httpClient);
        template.setServiceAddress(m_urlString);
        return template;
    }

    /**
     * Transfer Element and attribute values from the JDOM
     * 
     * @see org.xaware.server.engine.channel.AbstractChannelSpecification#transformSpecInfo(org.xaware.server.engine.IBizViewContext)
     */
    @Override
    public void transformSpecInfo(final IBizViewContext p_bizViewContext) throws XAwareConfigMissingException, XAwareSubstitutionException, XAwareException {
        m_channelSpecElement = m_bizDriverRootElement.getChild(CHANNEL_SPEC_ELEMENT_NAME, XAwareConstants.xaNamespace);
        if (m_channelSpecElement == null) {
            throw new XAwareConfigMissingException("No xa:" + CHANNEL_SPEC_ELEMENT_NAME + " provided");
        }
        setupUrl(p_bizViewContext);
        setupUser(p_bizViewContext);
        if (m_uid != null) {
            setupPassword(p_bizViewContext);
        }
        setupProxy(p_bizViewContext);
        setupTimeout(p_bizViewContext);
    }

    /**
     * Reads the text value of the xa:url element.
     * @param p_bizViewContext
     * @throws XAwareException
     */
    private void setupUrl(IBizViewContext p_bizViewContext) throws XAwareException {
        m_urlString = getChildElementValue(m_channelSpecElement, XAwareConstants.BIZDRIVER_URL, p_bizViewContext, true);
    }

    /**
     * Reads the text value from the xa:user element.
     * @param p_bizViewContext
     * @throws XAwareException
     */
    private void setupUser(IBizViewContext p_bizViewContext) throws XAwareException {
        m_uid = getChildElementValue(m_channelSpecElement, XAwareConstants.BIZDRIVER_USER, p_bizViewContext, false);
    }

    /**
     * Reads the text value from the xa:pwd element.
     * @param p_bizViewContext
     * @throws XAwareException
     */
    private void setupPassword(IBizViewContext p_bizViewContext) throws XAwareException {
        m_pwd = getChildElementValue(m_channelSpecElement, XAwareConstants.BIZDRIVER_PWD, p_bizViewContext, false);
    }

    /**
     * Reads the text value from the xa:timeout element.
     * @param p_bizViewContext
     * @throws XAwareException
     */
    private void setupTimeout(IBizViewContext p_bizViewContext) throws XAwareException {
        String timeoutString = getChildElementValue(m_channelSpecElement, XAwareConstants.BIZDRIVER_TIMEOUT, p_bizViewContext, false);
        if (timeoutString != null && !"".equals(timeoutString.trim())) {
            try {
                m_timeout = Integer.parseInt(timeoutString);
            } catch (NumberFormatException e) {
                throw new XAwareException("Invalid value provided for timeout: " + timeoutString, e);
            }
        } else {
            m_timeout = -1;
        }
    }

    /**
     * If the xa:proxy element exists the proxy attributes are read in.
     * @param p_bizViewContext
     * @throws XAwareException
     */
    private void setupProxy(IBizViewContext p_bizViewContext) throws XAwareException {
        Element proxy = m_channelSpecElement.getChild(XAwareConstants.BIZDRIVER_PROXY, XAwareConstants.xaNamespace);
        if (proxy != null) {
            setupProxyUser(proxy, p_bizViewContext);
            setupProxyPassword(proxy, p_bizViewContext);
            setupProxyHost(proxy, p_bizViewContext);
            setupProxyPort(proxy, p_bizViewContext);
        }
    }

    /**
     * Reads the text value of the xa:proxy_user element.
     * @param p_proxyElment
     * @param p_bizViewContext
     */
    private void setupProxyUser(Element p_proxyElment, IBizViewContext p_bizViewContext) throws XAwareException {
        m_proxyUser = getChildElementValue(p_proxyElment, XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER, p_bizViewContext, false);
    }

    /**
     * Reads the text value of the xa:proxy_pwd element.
     * @param p_proxyElment
     * @param p_bizViewContext
     */
    private void setupProxyPassword(Element p_proxyElment, IBizViewContext p_bizViewContext) throws XAwareException {
        m_proxyPwd = getChildElementValue(p_proxyElment, XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD, p_bizViewContext, false);
    }

    /**
     * Reads the text value of the xa:proxy_host element
     * @param p_proxyElment
     * @param p_bizViewContext
     */
    private void setupProxyHost(Element p_proxyElment, IBizViewContext p_bizViewContext) throws XAwareException {
        m_proxyHost = getChildElementValue(p_proxyElment, XAwareConstants.BIZCOMPONENT_ATTR_PROXYHOST, p_bizViewContext, false);
    }

    /**
     * @param p_proxyElment
     * @param p_bizViewContext
     */
    private void setupProxyPort(Element p_proxyElment, IBizViewContext p_bizViewContext) throws XAwareException {
        String portString = getChildElementValue(p_proxyElment, XAwareConstants.BIZCOMPONENT_ATTR_PROXYPORT, p_bizViewContext, false);
        if (portString != null && !"".equals(portString.trim())) {
            try {
                m_proxyPort = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                throw new XAwareException("Invalid value provided for proxy port: " + portString, e);
            }
        } else {
            m_proxyPort = AuthScope.ANY_PORT;
        }
    }

    /**
     * This is what is used by caching and transactions to determine if there needs to be a new BizDriver and
     * ChannelSpecification created or if it can reuse this one. The key should uniquely identify this BizDriver so it
     * can be re-used or not re-used.  Passwords should not be part of any clear text keys.
     * 
     * @return IChannelKey built from the values of this channel spec.
     */
    public IChannelKey produceKey() {
        if (key == null) {
            StringBuffer buff = new StringBuffer(200);
            buff.append(getSegmentDelimiter()).append("url=").append(m_urlString).append(getNameValueDelimiter());
            buff.append(getSegmentDelimiter()).append("user=").append(m_uid).append(getNameValueDelimiter());
            buff.append(getSegmentDelimiter()).append("timeout=").append(m_timeout).append(getNameValueDelimiter());
            buff.append(getSegmentDelimiter()).append("proxyhost=").append(m_proxyHost).append(getNameValueDelimiter());
            buff.append(getSegmentDelimiter()).append("proxyport=").append(m_proxyPort).append(getNameValueDelimiter());
            key = new LocalChannelKey(buff.toString());
        }
        return key;
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, instead use getChannelObject()");
    }
}

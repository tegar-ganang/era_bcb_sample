package org.xaware.server.engine.channel.smtp;

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
 * This is the implementation of the smtp channel specification. It takes a
 * xa:smtp Element from the bizdriver, and sticks child element values in it's Properties object.
 * 
 * @author dwieland
 */
public class SmtpChannelSpecification extends AbstractChannelSpecification implements IChannelSpecification {

    public static final String AUTH_KEY = "mail.smtp.auth";

    public Object getChannelObject() throws XAwareException {
        return this;
    }

    /**
	 * Transfer Element and attribute values from the JDOM
	 * 
     * @see org.xaware.server.engine.channel.AbstractChannelSpecification#transformSpecInfo(org.xaware.server.engine.IBizViewContext)
     */
    @Override
    public void transformSpecInfo(final IBizViewContext bizViewContext) throws XAwareConfigMissingException, XAwareSubstitutionException, XAwareException {
        Element smtp = bizViewContext.getScriptRoot().getChild("smtp", XAwareConstants.xaNamespace);
        if (smtp == null) {
            throw new XAwareException("xa:smtp element not found in SmtpBizDriver " + bizViewContext.getBizViewName());
        }
        substituteAllAttributes(smtp, bizViewContext);
        m_props.setProperty(PN_BIZDRIVER_IDENTIFIER, bizViewContext.getBizViewName());
        addProperty(XAwareConstants.BIZDRIVER_SERVER, getChildElementValue(smtp, XAwareConstants.BIZDRIVER_SERVER, bizViewContext, true));
        addProperty(XAwareConstants.BIZDRIVER_PORT, getChildPortElementValue(smtp, bizViewContext));
        addProperty(XAwareConstants.BIZDRIVER_USER, getChildElementValue(smtp, XAwareConstants.BIZDRIVER_USER, bizViewContext, true));
        addProperty(XAwareConstants.BIZDRIVER_PWD, getChildElementValue(smtp, XAwareConstants.BIZDRIVER_PWD, bizViewContext, true));
        addProperty(AUTH_KEY, getChildAuthenticationElementValue(smtp, bizViewContext));
    }

    /**
	 * This is what is used by caching and transactions to determine if there
	 * needs to be a new BizDriver and ChannelSpecification created or if it can
	 * reuse this one. The key should uniquely identify this BizDriver so it can
	 * be re-used or not re-used.
	 * 
	 * @return String
	 */
    public IChannelKey produceKey() {
        StringBuffer buff = new StringBuffer(getProperty(PN_BIZDRIVER_IDENTIFIER));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_SERVER));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_PORT));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_USER));
        return new LocalChannelKey(buff.toString());
    }

    /**
     * return the port element value or default to 25
     * @param elem
     * @param context
     * @return
     * @throws XAwareException
     */
    private String getChildPortElementValue(Element elem, IBizViewContext context) throws XAwareException {
        String portVal = getChildElementValue(elem, XAwareConstants.BIZDRIVER_PORT, context, true);
        if (portVal == null) {
            portVal = "25";
        }
        return portVal;
    }

    /**
     * return the authentication element value or default to no
     * @param elem
     * @param context
     * @return
     * @throws XAwareException
     */
    private String getChildAuthenticationElementValue(Element elem, IBizViewContext context) throws XAwareException {
        String authVal = getChildElementValue(elem, XAwareConstants.BIZDRIVER_USE_AUTHENTICATION, context, false);
        if (authVal == null || XAwareConstants.XAWARE_NO.equals(authVal)) {
            authVal = XAwareConstants.XAWARE_FALSE;
        } else if (XAwareConstants.XAWARE_YES.equals(authVal)) {
            authVal = XAwareConstants.XAWARE_TRUE;
        }
        return authVal;
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, instead use getChannelObject()");
    }
}

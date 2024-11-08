package org.xaware.server.engine.channel.http;

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
 * This is the implementation of the Http channel specification. It takes a
 * xa:http Element from the bizdriver, and sticks child element values in it's Properties object.
 * 
 * @author jtarnowski
 */
public class HttpChannelSpecification extends AbstractChannelSpecification implements IChannelSpecification {

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
        Element http = bizViewContext.getScriptRoot().getChild("http", XAwareConstants.xaNamespace);
        if (http == null) {
            throw new XAwareException("xa:http element not found in HttpBizDriver " + bizViewContext.getBizViewName());
        }
        m_props.setProperty(PN_BIZDRIVER_IDENTIFIER, bizViewContext.getBizViewName());
        addProperty(XAwareConstants.BIZDRIVER_USER, getChildElementValueNeverNull(http, XAwareConstants.BIZDRIVER_USER, bizViewContext));
        addProperty(XAwareConstants.BIZDRIVER_PWD, getChildElementValueNeverNull(http, XAwareConstants.BIZDRIVER_PWD, bizViewContext));
        addProperty(XAwareConstants.BIZDRIVER_URL, getChildElementValue(http, XAwareConstants.BIZDRIVER_URL, bizViewContext, false));
        addProperty(XAwareConstants.BIZCOMPONENT_ATTR_BASE_URL, getChildElementValue(http, XAwareConstants.BIZCOMPONENT_ATTR_BASE_URL, bizViewContext, false));
        Element proxy = http.getChild(XAwareConstants.BIZDRIVER_PROXY, XAwareConstants.xaNamespace);
        if (proxy != null) {
            addProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER, getChildElementValue(proxy, XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER, bizViewContext, false));
            addProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD, getChildElementValue(proxy, XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD, bizViewContext, false));
            addProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYHOST, getChildElementValue(proxy, XAwareConstants.BIZCOMPONENT_ATTR_PROXYHOST, bizViewContext, false));
            addProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPORT, getChildElementValue(proxy, XAwareConstants.BIZCOMPONENT_ATTR_PROXYPORT, bizViewContext, false));
        }
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
        buff.append(getProperty(XAwareConstants.BIZDRIVER_URL));
        buff.append(getProperty(XAwareConstants.BIZCOMPONENT_ATTR_BASE_URL));
        buff.append(getProperty(XAwareConstants.BIZCOMPONENT_ATTR_HOST));
        buff.append(getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PORT));
        buff.append(getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYHOST));
        buff.append(getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPORT));
        return new LocalChannelKey(buff.toString());
    }

    String getChildElementValueNeverNull(Element elem, String name, IBizViewContext context) throws XAwareException {
        String rc = this.getChildElementValue(elem, name, context, false);
        if (rc == null) {
            rc = "";
        }
        return rc;
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, instead use getChannelObject()");
    }
}

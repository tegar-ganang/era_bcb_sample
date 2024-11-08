package org.xaware.server.engine.channel.ftp;

import org.jdom.Element;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IChannelSpecification;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractChannelSpecification;
import org.xaware.server.engine.channel.LocalChannelKey;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;

/**
 * This is the implementation of the FTP channel specification. It takes a
 * xa:ftp Element from the bizdriver, and sticks child element values in it's
 * Properties object.
 * 
 * @author Vasu Thadaka
 */
public class FTPChannelSpecification extends AbstractChannelSpecification implements IChannelSpecification {

    /** Returns the channel object. */
    public Object getChannelObject() throws XAwareException {
        return this;
    }

    /** Utilitity method which always return empty if the element value is null */
    private String getChildElementValueNeverNull(Element elem, String name, IBizViewContext context) throws XAwareException {
        String rc = this.getChildElementValue(elem, name, context, false);
        if (rc == null) {
            rc = "";
        }
        return rc;
    }

    /**
	 * This is what is used by caching and transactions to determine if there
	 * needs to be a new BizDriver and ChannelSpecification created or if it can
	 * reuse this one. The key should uniquely identify this BizDriver so it can
	 * be re-used or not re-used.
	 * 
	 * @return IChannelKey
	 */
    public IChannelKey produceKey() {
        StringBuffer buff = new StringBuffer(getProperty(PN_BIZDRIVER_IDENTIFIER));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_HOST));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_PORT));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_USER));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_PWD));
        buff.append(getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER));
        buff.append(getProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD));
        buff.append(getProperty(XAwareConstants.XAWARE_FTP_REMOTE_VERIFICATION));
        return new LocalChannelKey(buff.toString());
    }

    @Override
    public void transformSpecInfo(IBizViewContext bizViewContext) throws XAwareConfigMissingException, XAwareConfigurationException, XAwareSubstitutionException, XAwareException {
        Element ftp = bizViewContext.getScriptRoot().getChild(FTPBizDriver.SPECIFICATION_ELEMENT_NAME, XAwareConstants.xaNamespace);
        if (ftp == null) {
            throw new XAwareException("xa:ftp element not found in FTPBizDriver " + bizViewContext.getBizViewName());
        }
        m_props.setProperty(PN_BIZDRIVER_IDENTIFIER, bizViewContext.getBizViewName());
        addProperty(XAwareConstants.BIZDRIVER_HOST, getChildElementValueNeverNull(ftp, XAwareConstants.BIZDRIVER_HOST, bizViewContext));
        addProperty(XAwareConstants.BIZDRIVER_PORT, getChildElementValueNeverNull(ftp, XAwareConstants.BIZDRIVER_PORT, bizViewContext));
        addProperty(XAwareConstants.BIZDRIVER_USER, getChildElementValue(ftp, XAwareConstants.BIZDRIVER_USER, bizViewContext, false));
        addProperty(XAwareConstants.BIZDRIVER_PWD, getChildElementValue(ftp, XAwareConstants.BIZDRIVER_PWD, bizViewContext, false));
        addProperty(XAwareConstants.XAWARE_FTP_REMOTE_VERIFICATION, getChildElementValue(ftp, XAwareConstants.XAWARE_FTP_REMOTE_VERIFICATION, bizViewContext, false));
        Element proxy = ftp.getChild(XAwareConstants.BIZDRIVER_PROXY, XAwareConstants.xaNamespace);
        if (proxy != null) {
            addProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER, getChildElementValue(proxy, XAwareConstants.BIZCOMPONENT_ATTR_PROXYUSER, bizViewContext, false));
            addProperty(XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD, getChildElementValue(proxy, XAwareConstants.BIZCOMPONENT_ATTR_PROXYPASSWORD, bizViewContext, false));
        }
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, instead use getChannelObject()");
    }
}

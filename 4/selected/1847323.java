package org.xaware.server.engine.channel.jms;

import org.jdom.Element;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;

/**
 * This class implements the non jms 1.0.2 channel specification
 */
public class JmsJndiChannelSpecification extends AbstractJmsJndiChannelSpecification {

    String jmsUserName = null;

    String jmsPassword = null;

    @Override
    public void transformSpecInfo(IBizViewContext bizViewContext) throws XAwareConfigMissingException, XAwareConfigurationException, XAwareSubstitutionException, XAwareException {
        super.transformSpecInfo(bizViewContext);
        Element connectionElement = this.m_bizDriverRootElement.getChild(XAwareConstants.BIZDRIVER_CONNECTION, XAwareConstants.xaNamespace);
        if (connectionElement != null) {
            jmsUserName = this.getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_USER, bizViewContext, false);
            if (jmsUserName != null && jmsUserName.length() > 0) {
                jmsPassword = this.getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_PWD, bizViewContext, false);
            }
        }
    }

    @Override
    protected boolean isJms102() {
        return false;
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, instead use getChannelObject()");
    }
}

package org.xaware.server.engine.channel.jms;

import javax.jms.ConnectionFactory;
import javax.naming.NamingException;
import org.jdom.Element;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.channel.AbstractJndiChannel;
import org.xaware.server.engine.channel.JndiDestinationResolver;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.shared.util.ExceptionMessageHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class will get the connection factory from the JNDI server, the child classes simply need to implement
 * {@link #isJms102()} to specify whether it implements the JMS 1.0.2 spec.
 * @author tferguson
 *
 */
public abstract class AbstractJmsJndiChannelSpecification extends AbstractJndiChannel {

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(AbstractJmsJndiChannelSpecification.class.getName());

    private JmsConnectionFactoryHolder connectionFactoryHolder = null;

    private IBizViewContext bizViewContext;

    @Override
    public void transformSpecInfo(IBizViewContext bizViewContext) throws XAwareConfigMissingException, XAwareConfigurationException, XAwareSubstitutionException, XAwareException {
        this.bizViewContext = bizViewContext;
        try {
            this.setupInitialContext(getBizDriverIdentifier() + ":JmsJndiChannelSpec", bizViewContext, lf);
            connectionFactoryHolder = lookupConnectionFactory(bizViewContext);
        } catch (NamingException e) {
            lf.debug(e);
            throw new XAwareConfigMissingException(ExceptionMessageHelper.getExceptionMessage(e), e);
        }
    }

    private JmsConnectionFactoryHolder lookupConnectionFactory(IBizViewContext bizViewContext) throws XAwareException, NamingException {
        String lookupName = this.getChildElementValue(initContextElement, XAwareConstants.BIZDRIVER_INITIALCONTEXT_LOOKUPNAME, bizViewContext, true);
        Object obj = jndiAccessor.getJndiTemplate().lookup(lookupName);
        if (null == obj) {
            throw new XAwareConfigMissingException("Unable to locate object in JNDI: " + lookupName);
        }
        if (!(obj instanceof ConnectionFactory)) {
            throw new XAwareConfigMissingException("Object in JNDI is not a " + ConnectionFactory.class.getCanonicalName() + ": " + lookupName);
        }
        Element connectionElement = this.m_bizDriverRootElement.getChild(XAwareConstants.BIZDRIVER_CONNECTION, XAwareConstants.xaNamespace);
        String jmsUserName = null;
        String jmsPassword = null;
        if (connectionElement != null) {
            jmsUserName = this.getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_USER, bizViewContext, false);
            if (jmsUserName != null && jmsUserName.length() > 0) {
                jmsPassword = this.getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_PWD, bizViewContext, false);
            }
        }
        JndiDestinationResolver destinationResolver = new JndiDestinationResolver();
        destinationResolver.setJndiEnvironment(getProps());
        destinationResolver.setFallbackToDynamicDestination(true);
        return new JmsConnectionFactoryHolder((ConnectionFactory) obj, isJms102(), destinationResolver, jmsUserName, jmsPassword);
    }

    /**
     * Will return a {@link JmsConnectionFactoryHolder} 
     */
    public Object getChannelObject() throws XAwareException {
        if (connectionFactoryHolder == null) {
            try {
                connectionFactoryHolder = this.lookupConnectionFactory(bizViewContext);
            } catch (NamingException e) {
                lf.debug(e);
                throw new XAwareException(e);
            }
        }
        return connectionFactoryHolder;
    }

    /**
     * This is done so that a Jms and Jms102 channel specification can be created
     * @return
     */
    protected abstract boolean isJms102();
}

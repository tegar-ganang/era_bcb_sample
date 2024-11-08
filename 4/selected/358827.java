package org.xaware.server.engine.channel.jmx;

import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.jdom.Attribute;
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
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * Channel Specification for JMX
 * 
 * @author blueAlly
 */
public class JMXChannelSpecification extends AbstractChannelSpecification implements IChannelSpecification {

    /** Logger for JMXChannelSpecification class */
    private static final XAwareLogger logger = XAwareLogger.getXAwareLogger(JMXChannelSpecification.class.getName());

    /** Class name */
    private String className = JMXChannelSpecification.class.getName();

    /**
     * (non-Javadoc)
     * 
     * @see org.xaware.server.engine.channel.AbstractChannelSpecification#transformSpecInfo(org.xaware.server.engine.IBizViewContext)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void transformSpecInfo(IBizViewContext p_bizViewContext) throws XAwareConfigMissingException, XAwareConfigurationException, XAwareSubstitutionException, XAwareException {
        Element connectionElement = m_bizDriverRootElement.getChild(XAwareConstants.BIZCOMPONENT_ELEMENT_CONNECTION, XAwareConstants.xaNamespace);
        if (connectionElement == null) {
            throw new XAwareException("xa:connection element not found in JMXBizDriver " + p_bizViewContext.getBizViewName());
        }
        final Properties connectionProps = new Properties();
        m_props.put(PN_CONNECTION_PROPERTIES, connectionProps);
        m_props.setProperty(PN_BIZDRIVER_IDENTIFIER, p_bizViewContext.getBizViewName());
        addProperty(XAwareConstants.BIZDRIVER_CONNECTION_TYPE, getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_CONNECTION_TYPE, p_bizViewContext, true));
        addProperty(XAwareConstants.BIZDRIVER_AGENT_ID, getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_AGENT_ID, p_bizViewContext, false));
        addProperty(XAwareConstants.BIZDRIVER_SERVICE_URL, getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_SERVICE_URL, p_bizViewContext, false));
        String tmpString = getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_USER, p_bizViewContext, false);
        if (tmpString != null) {
            connectionProps.setProperty(XAwareConstants.BIZDRIVER_USER, tmpString);
        }
        tmpString = getChildElementValue(connectionElement, XAwareConstants.BIZDRIVER_PWD, p_bizViewContext, false);
        if (tmpString != null) {
            connectionProps.setProperty(XAwareConstants.BIZDRIVER_PWD, tmpString);
        }
        Element element = connectionElement.getChild(XAwareConstants.ELEMENT_PROPERTIES, XAwareConstants.xaNamespace);
        if (element != null) {
            List<Element> children = element.getChildren();
            String name;
            String value;
            if (children != null) {
                for (Element child : children) {
                    substituteAllAttributes(element, p_bizViewContext);
                    Attribute attr = child.getAttribute(XAwareConstants.BIZDRIVER_ATTR_PARAM_NAME, XAwareConstants.xaNamespace);
                    if (attr == null) {
                        throw new XAwareConfigMissingException("No " + XAwareConstants.BIZDRIVER_ATTR_PARAM_NAME + " attribute specified for " + XAwareConstants.BIZDRIVER_PARAM + " element");
                    }
                    name = attr.getValue();
                    attr = child.getAttribute(XAwareConstants.BIZDRIVER_ATTR_PARAM_VALUE, XAwareConstants.xaNamespace);
                    if (attr == null) {
                        throw new XAwareConfigMissingException("No " + XAwareConstants.BIZDRIVER_ATTR_PARAM_VALUE + " attribute specified for " + XAwareConstants.BIZDRIVER_PARAM + " element");
                    }
                    value = attr.getValue();
                    connectionProps.setProperty(name, value);
                    logger.finest("[Name=" + name + "], [Value" + value + "]", getBizDriverIdentifier() + ":JMXChannelSpec", "transformSpecInfo");
                }
            }
        }
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.xaware.server.engine.IChannelSpecification#getChannelObject()
     */
    public Object getChannelObject() throws XAwareException {
        throw new XAwareException(className + ".getChannelObject() is not implemented instead use getChannelTemplate()");
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.xaware.server.engine.IChannelSpecification#getChannelTemplate()
     */
    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        return new JMXTemplate(m_props);
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.xaware.server.engine.IChannelSpecificationBase#produceKey()
     */
    public IChannelKey produceKey() {
        StringBuffer buff = new StringBuffer(getProperty(PN_BIZDRIVER_IDENTIFIER));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_CONNECTION_TYPE));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_AGENT_ID));
        buff.append(getProperty(XAwareConstants.BIZDRIVER_SERVICE_URL));
        Properties connectionProperties = (Properties) m_props.get(PN_CONNECTION_PROPERTIES);
        Enumeration keys = connectionProperties.keys();
        while (keys.hasMoreElements()) buff.append((String) keys.nextElement());
        return new LocalChannelKey(buff.toString());
    }
}

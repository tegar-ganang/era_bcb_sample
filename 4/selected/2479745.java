package org.xaware.server.engine.channel;

import java.util.Map;
import org.jdom.Document;
import org.jdom.Element;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IChannelPoolingSpecification;
import org.xaware.server.engine.IChannelSpecification;
import org.xaware.server.engine.ISessionStateRegistry;
import org.xaware.server.engine.context.BizDriverContext;
import org.xaware.server.engine.enums.SubstitutionFailureLevel;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.server.engine.instruction.bizcomps.config.BizDriverConfig;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;

/**
 * AbstractBizDriver is a base class to implement the IBizDriver interface and perform the common duties across all biz
 * drivers. It is abstract because it does not implement IBizDriver.createChannelObject() The channel specification and
 * channel pooling specification are injected, usually through spring via the ApplicationContext.xml file and they
 * ultimately determine how the channel is setup and are used by the IBizDriver.createChannelObject() method.
 * 
 * @author jweaver
 */
public abstract class AbstractBizDriver implements IBizDriver {

    protected String m_bizDriverIdentifier = null;

    protected String m_bizDriverLocation = null;

    protected IChannelSpecification m_channelSpecification = null;

    protected IChannelPoolingSpecification m_channelPoolingSpecification = null;

    protected Document m_jdomStructure = null;

    protected boolean m_transferedChannelConfigInfo = false;

    protected boolean m_transferedPoolingConfigInfo = false;

    protected String m_bizDriverType = null;

    protected BizDriverContext m_context;

    /**
     * @param p_bizViewContext
     * 
     * @see org.xaware.server.engine.IBizDriver#getChannelPoolingSpecification()
     */
    public final IChannelPoolingSpecification getChannelPoolingSpecification() throws XAwareConfigMissingException, XAwareConfigurationException, XAwareSubstitutionException, XAwareException {
        if (m_channelPoolingSpecification != null && !m_channelPoolingSpecification.isSpecificationInfoTransferred()) {
            m_channelPoolingSpecification.transferSpecificationInfo(getBizDriverIdentifier(), getRootElement(), m_context);
        }
        return m_channelPoolingSpecification;
    }

    /**
     * @param p_bizViewContext
     * @see org.xaware.server.engine.IBizDriver#getChannelSpecification()
     */
    public final IChannelSpecification getChannelSpecification() throws XAwareConfigMissingException, XAwareConfigurationException, XAwareSubstitutionException, XAwareException {
        if (m_channelSpecification == null) {
            throw new XAwareConfigurationException("No configuration info provided for BizDriver " + getBizDriverIdentifier());
        }
        if (!m_channelSpecification.isSpecificationInfoTransferred()) {
            m_channelSpecification.transferSpecificationInfo(getBizDriverIdentifier(), getRootElement(), m_context);
        }
        return m_channelSpecification;
    }

    /**
     * @param p_poolingSpec
     * @see org.xaware.server.engine.IBizDriver#setChannelPoolingSpecification(org.xaware.server.engine.IChannelPoolingSpecification)
     */
    public final void setChannelPoolingSpecification(final IChannelPoolingSpecification p_poolingSpec) {
        m_channelPoolingSpecification = p_poolingSpec;
    }

    /**
     * @param p_channelSpec
     * @see org.xaware.server.engine.IBizDriver#setChannelSpecification(org.xaware.server.engine.IChannelSpecification)
     */
    public final void setChannelSpecification(final IChannelSpecification p_channelSpec) {
        m_channelSpecification = p_channelSpec;
    }

    /**
     * Provided setting structure containing the configuration info for this BizDriver.
     * 
     * @param p_jdom
     *            the structure to store.
     * @see org.xaware.server.engine.IBizDriver#setJdomDocument(org.jdom.Document)
     */
    public final void setJdomDocument(final Document p_jdom) {
        m_jdomStructure = p_jdom;
    }

    /**
     * 
     * @return The root element from the structure provided by a call to {@link #setJdomDocument(Document)}.
     */
    public final Element getRootElement() {
        if (m_jdomStructure == null) {
            return null;
        }
        return m_jdomStructure.getRootElement();
    }

    /**
     * @param p_bizDriverIdentifier
     */
    public final void setBizDriverIdentifier(final String p_bizDriverIdentifier) {
        m_bizDriverIdentifier = p_bizDriverIdentifier;
    }

    /**
     * @return the BizDriver Identifier (name underwhich the driver was requested)
     */
    public final String getBizDriverIdentifier() {
        return m_bizDriverIdentifier;
    }

    /**
     * @return the bizDriverLocation (where the Resource was found)
     */
    public final String getBizDriverLocation() {
        return m_bizDriverLocation;
    }

    /**
     * @param p_bizDriverLocation
     *            the bizDriverLocation to set
     */
    public final void setBizDriverLocation(final String p_bizDriverLocation) {
        m_bizDriverLocation = p_bizDriverLocation;
    }

    /**
     * @return the bizDriverType
     */
    public final String getBizDriverType() {
        return m_bizDriverType;
    }

    /**
     * @param p_bizDriverType
     *            the bizDriverType to set
     */
    public final void setBizDriverType(final String p_bizDriverType) {
        m_bizDriverType = p_bizDriverType;
    }

    public void setParentContext(final IBizViewContext p_parentContext) {
    }

    public void setupContext(final IBizViewContext p_parentContext, final BizDriverConfig p_bizDriverConfig) throws XAwareException {
        m_bizDriverIdentifier = p_bizDriverConfig.getBizDriverName();
        if (p_parentContext == null) {
            throw new XAwareException("parent context is null");
        }
        m_context = new BizDriverContext(getBizDriverIdentifier(), p_parentContext, m_jdomStructure, p_bizDriverConfig.getParams());
        m_bizDriverType = m_jdomStructure.getRootElement().getAttributeValue(XAwareConstants.BIZDRIVER_ATTR_TYPE, XAwareConstants.xaNamespace);
        m_context.setInputXmlRoot(p_bizDriverConfig.getInputXML());
        getChannelSpecification();
        getChannelPoolingSpecification();
    }

    public void setupContext(final String p_bizViewName, final Map<String, Object> p_params, final String p_inputXml) throws XAwareException {
        m_context = new BizDriverContext(getBizDriverIdentifier(), m_jdomStructure, p_params, p_inputXml);
        m_bizDriverType = m_jdomStructure.getRootElement().getAttributeValue(XAwareConstants.BIZDRIVER_ATTR_TYPE, XAwareConstants.xaNamespace);
        getChannelSpecification();
        getChannelPoolingSpecification();
    }

    /**
     * Determines the effective SubstitutionFailureLevel for the configElement provided. It may come from a
     * xa:on_substitute_failure declaration on the configElement or any of its ancestors, or if none have such a
     * declaration, it is inherited from the ScriptNode on the top of the call stack in the SessionStateRegistry.
     * 
     * @param p_element
     *            the Element for which the effective SubstitutionFailureLevel is requested.
     * @param p_bizViewContext
     *            the IBizViewContext of the configElement
     * @return the effective SubstitutionFailureLevel for specified configElement
     * @throws XAwareException
     *             if a failure occurs determining the effective SubstitutionFailureLevel.
     */
    protected SubstitutionFailureLevel getEffectiveSubstitutionFailureLevel(final Element p_element, final IBizViewContext p_bizViewContext) throws XAwareException {
        final boolean searchAncestorElements = true;
        ISessionStateRegistry ssr = p_bizViewContext.getRegistry();
        if (ssr != null) {
            return ssr.getEffectiveSubstitutionFailureLevel(p_element, searchAncestorElements);
        }
        return SubstitutionFailureLevel.IGNORE;
    }

    /**
     * 
     * @param p_element
     * @param p_bizViewContext
     * @throws XAwareSubstitutionException
     * @throws XAwareException
     */
    protected void substituteAllAttributes(final Element p_element, final IBizViewContext p_bizViewContext) throws XAwareSubstitutionException, XAwareException {
        final SubstitutionFailureLevel substFailureLevel = getEffectiveSubstitutionFailureLevel(p_element, p_bizViewContext);
        p_bizViewContext.substituteAllAttributes(p_element, null, substFailureLevel);
    }

    /**
     * 
     * @param p_value
     * @param p_element
     * @param p_bizViewContext
     * @return
     * @throws XAwareSubstitutionException
     * @throws XAwareException
     */
    protected String substitute(final String p_value, final Element p_element, final IBizViewContext p_bizViewContext) throws XAwareSubstitutionException, XAwareException {
        final SubstitutionFailureLevel substFailureLevel = getEffectiveSubstitutionFailureLevel(p_element, p_bizViewContext);
        return p_bizViewContext.substitute(p_value, p_element, null, substFailureLevel);
    }

    public IChannelKey getChannelSpecificationKey() {
        return m_channelSpecification.produceKey();
    }
}

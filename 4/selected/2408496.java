package org.xaware.server.engine.channel.file;

import org.jdom.Element;
import org.jdom.Namespace;
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
 * This is the implementation of the File channel specification. It takes a xa:file Element from the bizdriver, that
 * states the filename and request type (read, write or append) when applyConfiguration is called. The biz driver xml
 * would be like this:<xa:BizDriver><br/><xa:file><br/><xa:name>data/myFile.txt</xa:name><br/><xa:request_type>append</xa:request_type>
 * <br/></xa:file><br/></xa:BizDriver
 * 
 * It would seem like the utility class could handle what this class produces, so refactoring might make sense
 * 
 * @author jtarnowski
 */
public class FileChannelSpecification extends AbstractChannelSpecification implements IChannelSpecification {

    /** our logger */
    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(FileChannelSpecification.class.getName());

    /**
     * Transfer Element and attribute values from the JDOM
     * 
     * @see org.xaware.server.engine.channel.AbstractChannelSpecification#transformSpecInfo(org.xaware.server.engine.IBizViewContext)
     */
    @Override
    public void transformSpecInfo(final IBizViewContext p_bizViewContext) throws XAwareConfigMissingException, XAwareSubstitutionException, XAwareException {
        parseConnectionDefinition(p_bizViewContext, getBizDriverIdentifier() + ":JdbcChannelSpec");
    }

    /**
     * Return the driver class configured in the factory element for this channel
     * 
     * @return
     */
    public final String getDriverClassName() {
        return m_props.getProperty(XAwareConstants.BIZDRIVER_FACTORY);
    }

    /**
     * Return the name configured for this channel
     * 
     * @return
     */
    public final String getName() {
        return m_props.getProperty(XAwareConstants.XAWARE_FILE_NAME);
    }

    /**
     * Return the requestType
     * 
     * @return requestType - String
     */
    public final String getRequestType() {
        return m_props.getProperty(XAwareConstants.BIZCOMPONENT_ATTR_REQUEST_TYPE);
    }

    /**
     * Applies each connection parameter received from the BizDriver file to the given {@link FileDriverData}
     * 
     * @param object
     *            this object must be of type FileDriverData
     * @throws XAwareConfigurationException
     */
    public Object getChannelObject() throws XAwareException {
        FileDriverData data = new FileDriverData();
        data.setFileName((String) m_props.get(XAwareConstants.XAWARE_FILE_NAME));
        data.setMode((String) m_props.get(XAwareConstants.BIZCOMPONENT_ATTR_REQUEST_TYPE));
        return data;
    }

    /**
     * Produces the key to uniquely identify this channel specification
     * 
     * @return
     */
    public IChannelKey produceKey() {
        return new LocalChannelKey(PN_BIZDRIVER_IDENTIFIER + getNameValueDelimiter() + getBizDriverIdentifier() + getSegmentDelimiter() + XAwareConstants.BIZDRIVER_FACTORY + getNameValueDelimiter() + getDriverClassName() + getSegmentDelimiter() + XAwareConstants.BIZCOMPONENT_ATTR_REQUEST_TYPE + getNameValueDelimiter() + getRequestType() + getSegmentDelimiter() + XAwareConstants.XAWARE_FILE_NAME + getNameValueDelimiter() + getName() + getSegmentDelimiter());
    }

    /**
     * Parse the connection paremeters, this is the method that actually does the work.
     * 
     * @param p_bizViewContext
     * @param p_classLocation
     */
    @SuppressWarnings("unchecked")
    private void parseConnectionDefinition(final IBizViewContext p_bizViewContext, final String p_classLocation) throws XAwareSubstitutionException, XAwareException {
        final String methodName = "parseConnectionDefinition";
        lf.entering(p_classLocation, methodName);
        final Namespace xaNamespace = XAwareConstants.xaNamespace;
        Element fileElement = m_bizDriverRootElement.getChild(XAwareConstants.XAWARE_FILE, xaNamespace);
        if (fileElement == null) {
            throw new XAwareConfigMissingException("No " + XAwareConstants.XAWARE_FILE + " element found");
        }
        String s = this.getChildElementValue(fileElement, XAwareConstants.XAWARE_FILE_NAME, p_bizViewContext, true);
        if (s == null) {
            throw new XAwareException("FileBizDriver: xa:name must be specified");
        }
        m_props.put(XAwareConstants.XAWARE_FILE_NAME, s);
        s = this.getChildElementValue(fileElement, XAwareConstants.BIZCOMPONENT_ATTR_REQUEST_TYPE, p_bizViewContext, true);
        if (s == null) {
            throw new XAwareException("FileBizDriver: " + XAwareConstants.BIZCOMPONENT_ATTR_REQUEST_TYPE + " must be present");
        }
        m_props.put(XAwareConstants.BIZCOMPONENT_ATTR_REQUEST_TYPE, s);
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, instead use getChannelObject()");
    }
}

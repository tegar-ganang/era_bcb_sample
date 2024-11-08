package org.xaware.server.engine.channel.sql;

import java.util.Enumeration;
import java.util.Properties;
import org.jdom.Element;
import org.jdom.Namespace;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IChannelSpecification;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractConnectionChannel;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This is the implementation of the SQL JDBC channel specification.  It takes a connection element from
 * the bizdriver, that states the factory, connection string, url, username, password, and any connection
 * properties and sets them on a BasicDataSource when applyConfiguration is called.  The configuration in 
 * the biz driver xml would be like this: 
 * <connection>
 *     <factory>com.oracle.jdbc.jdbcdriver</factory>
 *     <url>jdbc:thin:...<url>
 *     <user>user</user>
 *     <pwd>password</pwd>
 *     <connectString>someconnectstring</connectString>
 * </connection>
 * 
 * @author jweaver
 * 
 */
public class JdbcChannelSpecification extends AbstractConnectionChannel implements IChannelSpecification {

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(JdbcChannelSpecification.class.getName());

    private Element m_connectionElement;

    @Override
    public void transformSpecInfo(final IBizViewContext p_bizViewContext) throws XAwareConfigMissingException, XAwareSubstitutionException, XAwareException {
        final Namespace xaNamespace = XAwareConstants.xaNamespace;
        m_connectionElement = m_bizDriverRootElement.getChild(XAwareConstants.BIZDRIVER_CONNECTION, xaNamespace);
        parseConnectionDefinition(m_connectionElement, p_bizViewContext, getBizDriverIdentifier() + ":JdbcChannelSpec", lf, true, true);
    }

    /**
     * Applies each connection parameter received from the BizDriver file to the given {@link org.apache.commons.dbcp.BasicDataSource}
     * 
     * return object
     *          this object is of type {@link org.apache.commons.dbcp.BasicDataSource}.
     */
    public Object getChannelObject() throws XAwareException {
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(getFactoryClassName());
        basicDataSource.setUrl(getUrl());
        basicDataSource.setUsername(getUsername());
        basicDataSource.setPassword(getPassword());
        final Properties connProps = getConnectionProperties();
        final Enumeration keysEnum = connProps.keys();
        while (keysEnum.hasMoreElements()) {
            final String key = (String) keysEnum.nextElement();
            final String value = connProps.getProperty(key);
            basicDataSource.addConnectionProperty(key, value);
        }
        return basicDataSource;
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, instead use getChannelObject()");
    }
}

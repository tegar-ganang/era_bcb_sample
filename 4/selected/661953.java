package org.xaware.server.engine.channel.sql;

import javax.naming.NamingException;
import javax.sql.DataSource;
import org.xaware.server.engine.IBizViewContext;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractJndiChannel;
import org.xaware.server.engine.channel.JndiChannelKey;
import org.xaware.server.engine.exceptions.XAwareConfigMissingException;
import org.xaware.server.engine.exceptions.XAwareConfigurationException;
import org.xaware.shared.util.ExceptionMessageHelper;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.XAwareSubstitutionException;
import org.xaware.shared.util.logging.XAwareLogger;

public class SqlJndiChannelSpecification extends AbstractJndiChannel {

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(SqlJndiChannelSpecification.class.getName());

    private DataSource ds = null;

    private IBizViewContext bizViewContext;

    @Override
    public void transformSpecInfo(IBizViewContext bizViewContext) throws XAwareConfigMissingException, XAwareConfigurationException, XAwareSubstitutionException, XAwareException {
        this.bizViewContext = bizViewContext;
        try {
            this.setupInitialContext(getBizDriverIdentifier() + ":SqlJndiChannelSpec", bizViewContext, lf);
            ds = lookupDataSource(bizViewContext);
        } catch (NamingException e) {
            lf.severe(JndiChannelKey.dumpJndiTree(jndiAccessor));
            lf.debug(e);
            throw new XAwareConfigMissingException(ExceptionMessageHelper.getExceptionMessage(e), e);
        }
    }

    private DataSource lookupDataSource(IBizViewContext bizViewContext) throws XAwareException, NamingException {
        String lookupName = this.getChildElementValue(initContextElement, XAwareConstants.BIZDRIVER_INITIALCONTEXT_LOOKUPNAME, bizViewContext, true);
        Object obj = jndiAccessor.getJndiTemplate().lookup(lookupName);
        if (null == obj) {
            throw new XAwareConfigMissingException("Unable to locate object in JNDI: " + lookupName);
        }
        System.out.println("Datasource returned: " + obj);
        if (!(obj instanceof DataSource)) {
            throw new XAwareConfigMissingException("Object in JNDI is not a " + DataSource.class.getCanonicalName() + ": " + lookupName);
        }
        return (DataSource) obj;
    }

    public Object getChannelObject() throws XAwareException {
        if (ds == null) {
            try {
                ds = this.lookupDataSource(bizViewContext);
            } catch (NamingException e) {
                lf.debug(e);
                throw new XAwareException(e);
            }
        }
        return this.ds;
    }

    public IGenericChannelTemplate getChannelTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, instead use getChannelObject()");
    }
}

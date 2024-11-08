package org.xaware.server.engine.channel.sql;

import javax.sql.DataSource;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * The SqlBizDriver returns a {@link org.apache.commons.dbcp.BasicDataSource} from the
 * createChannelObject() method which it is depending on the channel specification and
 * pooling channel specification to apply the correct configuration information to
 * 
 * @author jweaver
 * 
 */
public class SqlBizDriver extends AbstractBizDriver implements IBizDriver {

    /**
     * Returns a see {@link DataSource} object
     */
    public Object createChannelObject() throws XAwareException {
        DataSource dataSource = (DataSource) this.m_channelSpecification.getChannelObject();
        this.m_channelPoolingSpecification.applyConfiguration(dataSource);
        return dataSource;
    }

    public IGenericChannelTemplate createTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, user createChannelObject() at this time");
    }
}

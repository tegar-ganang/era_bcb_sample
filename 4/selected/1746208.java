package org.xaware.server.engine.channel.sql;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.ITransactionContext;
import org.xaware.server.engine.ITransactionalChannel;
import org.xaware.server.engine.channel.IChannelPoolManager;
import org.xaware.shared.util.XAwareConstants;
import org.xaware.shared.util.XAwareException;
import org.xaware.shared.util.logging.XAwareLogger;

/**
 * This class implements a way to create and retrieve a JdbcTemplate that will have a pooled connection behind it.
 * The 
 * @author Tim Ferguson
 */
public class JdbcTemplateFactory implements IJdbcTemplateFactory, IChannelPoolManager {

    private static final String className = JdbcTemplateFactory.class.getName();

    private static final XAwareLogger lf = XAwareLogger.getXAwareLogger(className);

    /**
     * Currently the idea is that this class is made a singleton by Spring therefore the dynamicDataSources is
     * not a static member.  
     */
    private Map<IChannelKey, DataSource> dynamicDataSources = new HashMap<IChannelKey, DataSource>();

    /**
     * The only time that the transaction context should be null is when Designer is using the 
     * template factory to test out the connection when creating the biz driver.
     * 
     * @see org.xaware.server.engine.channel.sql.IJdbcTemplateFactory#getJdbcTemplate(org.xaware.server.engine.IBizDriver,
     *      org.xaware.server.engine.ITransactionContext)
     */
    public BatchingJdbcTemplate getJdbcTemplate(final IBizDriver bizDriver, final ITransactionContext transactionContext) throws SQLException, XAwareException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        BatchingJdbcTemplate batchingJdbcTemplate = null;
        if (transactionContext != null) {
            batchingJdbcTemplate = (BatchingJdbcTemplate) transactionContext.getTransactionalChannel(key);
            if (batchingJdbcTemplate != null) {
                return batchingJdbcTemplate;
            }
        }
        DataSource ds = this.dynamicDataSources.get(key);
        if (ds == null) {
            ds = (DataSource) bizDriver.createChannelObject();
            this.dynamicDataSources.put(key, ds);
        }
        ITransactionalChannel.Type transactionalChannelType = (XAwareConstants.BIZDRIVER_ATTR_SQL_JDBC.equals(bizDriver.getBizDriverType())) ? ITransactionalChannel.Type.LOCAL_JDBC : ITransactionalChannel.Type.DISTRIBUTED_JDBC;
        batchingJdbcTemplate = new BatchingJdbcTemplate(ds, transactionalChannelType);
        if (transactionContext != null) {
            transactionContext.setTransactionalChannel(key, batchingJdbcTemplate);
        }
        return batchingJdbcTemplate;
    }

    public void clearPool(final String p_poolName) {
        if (getEntryCount() == 0) {
            return;
        }
        final DataSource dataSource = dynamicDataSources.get(p_poolName);
        if (dataSource != null) {
            if (dataSource instanceof BasicDataSource) {
                BasicDataSource bds = (BasicDataSource) dataSource;
                try {
                    bds.getConnection().close();
                    bds.close();
                } catch (SQLException e) {
                    lf.debug(e);
                }
            }
            dynamicDataSources.remove(p_poolName);
        }
    }

    public void clearPools() {
        if (getEntryCount() == 0) {
            return;
        }
        final String[] keys = getPoolNames();
        for (String key : keys) {
            clearPool(key);
        }
    }

    public int getEntryCount() {
        if (dynamicDataSources == null) {
            return 0;
        }
        return dynamicDataSources.size();
    }

    public String getPoolManagerName() {
        return "JdbcTemplateFactory";
    }

    public String[] getPoolNames() {
        if (getEntryCount() == 0) {
            return new String[0];
        }
        final Set<IChannelKey> keys = this.dynamicDataSources.keySet();
        final String[] keyArray = new String[keys.size()];
        int i = 0;
        for (IChannelKey key : this.dynamicDataSources.keySet()) {
            keyArray[i] = key.toString();
            i++;
        }
        return keyArray;
    }
}

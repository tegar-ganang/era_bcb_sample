package org.swemas.data.sql.pgsql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import org.swemas.core.Module;
import org.swemas.core.ModuleNotFoundException;
import org.swemas.core.kernel.IKernel;
import org.swemas.core.messaging.ILocaleProvidingChannel;
import org.swemas.data.sql.DbConnection;
import org.swemas.data.sql.ISqlChannel;
import org.swemas.data.sql.SqlException;
import org.swemas.data.sql.ErrorCode;
import org.postgresql.ds.*;
import org.apache.commons.dbcp.*;
import org.apache.commons.pool.impl.*;

/**
 * @author Alexey Chernov
 * 
 */
public class SwPgSql extends Module implements ISqlChannel {

    /**
	 * @param kernel
	 * @throws InstantiationException
	 */
    public SwPgSql(IKernel kernel) throws InstantiationException {
        super(kernel);
    }

    @Override
    public Statement createStatement(DbConnection connection) throws SqlException {
        try {
            PGSimpleDataSource source = new PGSimpleDataSource();
            source.setServerName(connection.hostname);
            source.setDatabaseName(connection.database);
            source.setPortNumber(connection.port);
            source.setUser(connection.username);
            source.setPassword(connection.pass);
            source.setSsl(connection.secure);
            GenericObjectPool connectionPool = new GenericObjectPool(null);
            ConnectionFactory connectionFactory = new DataSourceConnectionFactory(source);
            new PoolableConnectionFactory(connectionFactory, connectionPool, null, null, false, true);
            PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(!connection.transactive);
            return conn.createStatement();
        } catch (SQLException e) {
            try {
                throw new SqlException(null, name(((ILocaleProvidingChannel) kernel().getChannel(ILocaleProvidingChannel.class)).getCurrentLocale()), ErrorCode.DatabaseConnectionError.getCode());
            } catch (ModuleNotFoundException m) {
                throw new SqlException(null, name(new Locale("en", "US")), ErrorCode.DatabaseConnectionError.getCode());
            }
        }
    }
}

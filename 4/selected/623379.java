package de.objectcode.time4u.store.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import de.objectcode.time4u.StorePlugin;
import de.objectcode.time4u.store.RepositoryException;
import de.objectcode.time4u.store.UserContext;

public class HSQLDBRepository extends AbstractDBRepository {

    private File m_baseDir;

    private ReadWriteLock m_readWriteLock = new ReentrantReadWriteLock();

    public HSQLDBRepository(UserContext userContext, HSQLDBRepositoryParameters parameters) throws RepositoryException {
        super(userContext);
        m_baseDir = parameters.getBaseDirectory();
        m_baseDir.mkdirs();
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            Connection connection = DriverManager.getConnection("jdbc:hsqldb:" + m_baseDir.toURL() + "/time4u", "sa", "");
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("SET PROPERTY \"hsqldb.default_table_type\" 'cached'");
            stmt.executeUpdate("SET WRITE_DELAY FALSE");
            stmt.executeUpdate("SHUTDOWN");
            stmt.close();
            connection.close();
            org.hsqldb.jdbc.jdbcDataSource ds = new org.hsqldb.jdbc.jdbcDataSource();
            ds.setDatabase("jdbc:hsqldb:" + m_baseDir.toURL() + "/time4u");
            ds.setUser("sa");
            ds.setPassword("");
            init(ds);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    public void close() {
        try {
            Connection connection = m_dataSource.getConnection();
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("SHUTDOWN");
            stmt.close();
            connection.close();
        } catch (Exception e) {
            StorePlugin.getDefault().log(e);
        }
    }

    @Override
    protected Lock lockRead() {
        Lock lock = m_readWriteLock.readLock();
        lock.lock();
        return lock;
    }

    @Override
    protected Lock lockWrite() {
        Lock lock = m_readWriteLock.writeLock();
        lock.lock();
        return lock;
    }
}

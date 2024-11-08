package jws.services.common;

import jws.core.config.ConfigurationException;
import java.util.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Implements {@link IPersister} that stores persistence records
 * in relational database. This persister is quite fast and useful
 * for clustered environments of when the persisted
 * data must be preserved across server restarts.<br/><br/>
 * {@link jws.services.common.DbPersister} requires additional parameters
 * to work properly. Below is an example configuration of a
 * {@link jws.services.common.DbPersister}
 * <pre>
 * &lt;param name="persister" type="jws.services.remoting.impl.PersisterConfiguration"&gt;
 *     &lt;persister-class&gt;jws.services.remoting.impl.DbPersister&lt;/persister-class&gt;
 *     &lt;parameters&gt;
 *          &lt;!-- parameter "datasource" is required, but may
 *                  not neccessarily be DirectDataSource --&gt;
 *          &lt;param name="datasource" type="jws.services.remoting.impl.DirectDataSource"&gt;
 *              &lt;driver-class&gt;oracle.jdbc.driver.OracleDriver&lt;/driver-class&gt;
 *              &lt;connection-url&gt;jdbc:oracle:thin:@orcl&lt;/connection-url&gt;
 *              &lt;driver-parameters&gt;
 *                  &lt;param name="user" type="string"&gt;scott&lt;/param&gt;
 *                  &lt;param name="password" type="string"&gt;tiger&lt;/param&gt;
 *              &lt;/driver-parameters&gt;
 *          &lt;/param&gt;
 *          &lt;!-- schema-related parameters, also required --&gt;
 *          &lt;param name="persister.db.tableName" type="string"&gt;STATE&lt;/param&gt;
 *          &lt;param name="persister.db.oidColumnName" type="string"&gt;OID&lt;/param&gt;
 *          &lt;param name="persister.db.keyColumnName" type="string"&gt;LOCK_KEY&lt;/param&gt;
 *          &lt;param name="persister.db.timestampColumnName" type="string"&gt;LAST_ACCESS&lt;/param&gt;
 *          &lt;param name="persister.db.dataColumnName" type="string"&gt;OBJ_DATA&lt;/param&gt;
 *      &lt;/parameters&gt;
 * &lt;/param&gt;
 * </pre>
 * Please, see JWS User Manual for a detailed information on configuring
 * {@link jws.services.common.DbPersister}
 */
public final class DbPersister implements IPersister {

    private static final String NULL = "NULL";

    private IDataSource _ds;

    private String _table_name;

    private String _oid_col;

    private String _key_col;

    private String _data_col;

    private String _ts_col;

    public DbPersister(Map<String, Object> params) throws ConfigurationException {
        Object obj = params.get("datasource");
        if ((obj == null) || (!(obj instanceof IDataSourceProvider))) {
            throw new ConfigurationException("Required parameter 'datasource' is missing or improperly defined");
        }
        IDataSourceProvider ds_conf = (IDataSourceProvider) obj;
        _ds = ds_conf.getDataSource();
        _table_name = getRequiredParameter(params, "persister.db.tableName");
        _oid_col = getRequiredParameter(params, "persister.db.oidColumnName");
        _key_col = getRequiredParameter(params, "persister.db.keyColumnName");
        _data_col = getRequiredParameter(params, "persister.db.dataColumnName");
        _ts_col = getRequiredParameter(params, "persister.db.timestampColumnName");
    }

    private String getRequiredParameter(Map<String, Object> params, String name) throws ConfigurationException {
        Object obj = params.get(name);
        if (obj == null) {
            throw new ConfigurationException("Required parameter '" + name + "' is not provided");
        }
        return obj.toString();
    }

    public void create(String oid, Serializable obj) throws PersisterException {
        String key = getLock(oid);
        if (key != null) {
            throw new PersisterException("Object already exists: OID = " + oid);
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            byte[] data = serialize(obj);
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("insert into " + _table_name + "(" + _oid_col + "," + _data_col + "," + _ts_col + ") values (?,?,?)");
            ps.setString(1, oid);
            ps.setBinaryStream(2, new ByteArrayInputStream(data), data.length);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Throwable th) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Throwable th2) {
                }
            }
            throw new PersisterException("Failed to create object: OID = " + oid, th);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
    }

    public void remove(String oid) throws PersisterException {
        String key = getLock(oid);
        if (key == null) {
            throw new PersisterException("Object does not exist: OID = " + oid);
        } else if (!NULL.equals(key)) {
            throw new PersisterException("The object is currently locked: OID = " + oid + ", LOCK = " + key);
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("delete from " + _table_name + " where " + _oid_col + " = ?");
            ps.setString(1, oid);
            ps.executeUpdate();
        } catch (Throwable th) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Throwable th2) {
                }
            }
            throw new PersisterException("Failed to delete object: OID = " + oid, th);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
    }

    public void save(String oid, String key, Serializable obj) throws PersisterException {
        String lock = getLock(oid);
        if (lock == null) {
            throw new PersisterException("Object does not exist: OID = " + oid);
        } else if (!NULL.equals(lock) && (!lock.equals(key))) {
            throw new PersisterException("The object is currently locked with another key: OID = " + oid + ", LOCK = " + lock + ", KEY = " + key);
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            byte[] data = serialize(obj);
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("update " + _table_name + " set " + _data_col + " = ?, " + _ts_col + " = ? where " + _oid_col + " = ?");
            ps.setBinaryStream(1, new ByteArrayInputStream(data), data.length);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, oid);
            ps.executeUpdate();
        } catch (Throwable th) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Throwable th2) {
                }
            }
            throw new PersisterException("Failed to save object: OID = " + oid, th);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
    }

    public Serializable load(String oid, String key) throws PersisterException {
        String lock = getLock(oid);
        if (lock == null) {
            throw new PersisterException("Object does not exist: OID = " + oid);
        } else if (!NULL.equals(lock) && (!lock.equals(key))) {
            throw new PersisterException("The object is currently locked with another key: OID = " + oid + ", LOCK = " + lock + ", KEY = " + key);
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Serializable obj = null;
        try {
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("select " + _data_col + " from " + _table_name + " where " + _oid_col + " = ?");
            ps.setString(1, oid);
            rs = ps.executeQuery();
            if (rs.next()) {
                obj = deserialize(rs.getBinaryStream(_data_col));
                if (rs.wasNull()) {
                    obj = null;
                }
            }
        } catch (Throwable th) {
            throw new PersisterException("Failed read lock key for object: OID = " + oid, th);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Throwable th) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
        if (obj == null) {
            throw new PersisterException("Object has been deleted: OID = " + oid);
        }
        return obj;
    }

    public void lock(String oid, String key) throws PersisterException {
        String lock = getLock(oid);
        if (lock == null) {
            throw new PersisterException("Object does not exist: OID = " + oid);
        } else if (!NULL.equals(lock) && (!lock.equals(key))) {
            throw new PersisterException("The object is currently locked with another key: OID = " + oid + ", LOCK = " + lock + ", KEY = " + key);
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("update " + _table_name + " set " + _key_col + " = ?, " + _ts_col + " = ? where " + _oid_col + " = ?");
            ps.setString(1, key);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, oid);
            ps.executeUpdate();
        } catch (Throwable th) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Throwable th2) {
                }
            }
            throw new PersisterException("Failed to lock object: OID = " + oid + ", KEY = " + key, th);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
    }

    public void unlock(String oid, String key) throws PersisterException {
        String lock = getLock(oid);
        if (lock == null) {
            throw new PersisterException("Object does not exist: OID = " + oid);
        } else if (!NULL.equals(lock) && (!lock.equals(key))) {
            throw new PersisterException("The object is currently locked with another key: OID = " + oid + ", LOCK = " + lock + ", KEY = " + key);
        }
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("update " + _table_name + " set " + _key_col + " = NULL, " + _ts_col + " = ? where " + _oid_col + " = ?");
            ps.setLong(1, System.currentTimeMillis());
            ps.setString(2, oid);
            ps.executeUpdate();
        } catch (Throwable th) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Throwable th2) {
                }
            }
            throw new PersisterException("Failed to unlock object: OID = " + oid + ", KEY = " + key, th);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
    }

    public Date getTimestamp(String oid) throws PersisterException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Long ts = null;
        try {
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("select " + _ts_col + " from " + _table_name + " where " + _oid_col + " = ?");
            ps.setString(1, oid);
            rs = ps.executeQuery();
            if (rs.next()) {
                ts = rs.getLong(_ts_col);
                if (rs.wasNull()) {
                    ts = null;
                }
            }
        } catch (Throwable th) {
            throw new PersisterException("Failed read lock key for object: OID = " + oid, th);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Throwable th) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
        return new Date(ts);
    }

    public void cleanup(long timeout) throws PersisterException {
        long threshold = System.currentTimeMillis() - timeout;
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("delete from " + _table_name + " where " + _ts_col + " < ?");
            ps.setLong(1, threshold);
            ps.executeUpdate();
        } catch (Throwable th) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Throwable th2) {
                }
            }
            throw new PersisterException("Failed to cleanup timed out objects: ", th);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
    }

    public Collection<PersisterRecord> getObjects(Class cls) throws PersisterException {
        List<PersisterRecord> list = new ArrayList<PersisterRecord>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        Serializable obj = null;
        try {
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("select " + _data_col + "," + _ts_col + " from " + _table_name);
            rs = ps.executeQuery();
            while (rs.next()) {
                obj = deserialize(rs.getBinaryStream(_data_col));
                if (rs.wasNull()) {
                    continue;
                }
                long ts = rs.getLong(_ts_col);
                if (rs.wasNull()) {
                    continue;
                }
                if ((cls == null) || !cls.isAssignableFrom(obj.getClass())) {
                    continue;
                }
                list.add(new PersisterRecord(obj, ts));
            }
            return list;
        } catch (Throwable th) {
            throw new PersisterException("Failed to get objects from DB", th);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Throwable th) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
    }

    private String getLock(String oid) throws PersisterException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String key = null;
        try {
            conn = _ds.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement("select " + _key_col + " from " + _table_name + " where " + _oid_col + " = ?");
            ps.setString(1, oid);
            rs = ps.executeQuery();
            if (rs.next()) {
                key = rs.getString(_key_col);
                if (rs.wasNull()) {
                    key = NULL;
                }
            }
        } catch (Throwable th) {
            throw new PersisterException("Failed read lock key for object: OID = " + oid, th);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Throwable th) {
                }
            }
            if (ps != null) {
                try {
                    ps.close();
                } catch (Throwable th) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable th) {
                }
            }
        }
        return key;
    }

    private byte[] serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
        baos.flush();
        baos.close();
        return baos.toByteArray();
    }

    private Serializable deserialize(InputStream ins) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(ins);
        return (Serializable) ois.readObject();
    }
}

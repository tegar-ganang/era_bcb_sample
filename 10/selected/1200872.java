package openvend.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import openvend.main.I_OvDao;
import openvend.main.OvLog;
import openvend.main.OvUuid;
import org.apache.commons.logging.Log;

/**
 * Abstract base class for DAO implementations.<p/>
 * 
 * @author Thomas Weckert
 * @version $Revision: 1.10 $
 * @see http://java.sun.com/j2se/1.4.2/docs/guide/jdbc/getstart/mapping.html
 * @since 1.0
 */
public abstract class A_OvDao implements I_OvDao {

    private static final long serialVersionUID = 2936914953990822547L;

    private static Log log = OvLog.getLog(A_OvDao.class);

    private Object primaryKey;

    public A_OvDao() {
        super();
        this.primaryKey = OvUuid.NULL_UUID;
    }

    /**
     * @see openvend.main.I_OvDao#createNewPrimaryKey()
     */
    public Object createNewPrimaryKey() {
        return new OvUuid().toString();
    }

    /**
     * @see openvend.main.I_OvDao#delete(java.sql.Connection, boolean)
     */
    public void delete(Connection conn, boolean commit) throws SQLException {
        PreparedStatement stmt = null;
        if (isNew()) {
            String errorMessage = "Unable to delete non-persistent DAO '" + getClass().getName() + "'";
            if (log.isErrorEnabled()) {
                log.error(errorMessage);
            }
            throw new SQLException(errorMessage);
        }
        try {
            stmt = conn.prepareStatement(getDeleteSql());
            stmt.setObject(1, getPrimaryKey());
            int rowCount = stmt.executeUpdate();
            if (rowCount != 1) {
                if (commit) {
                    conn.rollback();
                }
                String errorMessage = "Invalid number of rows changed!";
                if (log.isErrorEnabled()) {
                    log.error(errorMessage);
                }
                throw new SQLException(errorMessage);
            } else if (commit) {
                conn.commit();
            }
        } finally {
            OvJdbcUtils.closeStatement(stmt);
        }
    }

    /**
     * @see openvend.main.I_OvDao#isNew()
     */
    public boolean isNew() {
        return (OvUuid.NULL_UUID.equals(getPrimaryKey()));
    }

    /**
     * @see openvend.main.I_OvDao#load(java.sql.Connection, java.lang.Object)
     */
    public boolean load(Connection conn, Object primaryKey) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet res = null;
        try {
            stmt = conn.prepareStatement(getSelectSql());
            stmt.setObject(1, primaryKey);
            res = stmt.executeQuery();
            if (res.next()) {
                getValues(res);
            }
        } finally {
            OvJdbcUtils.closeResultSet(res);
            OvJdbcUtils.closeStatement(stmt);
        }
        return !isNew();
    }

    /**
     * @see openvend.main.I_OvDao#save(java.sql.Connection, boolean)
     */
    public void save(Connection conn, boolean commit) throws SQLException {
        PreparedStatement stmt = null;
        if (!isValid()) {
            String errorMessage = "Unable to save invalid DAO '" + getClass().getName() + "'!";
            if (log.isErrorEnabled()) {
                log.error(errorMessage);
            }
            throw new SQLException(errorMessage);
        }
        try {
            if (isNew()) {
                primaryKey = createNewPrimaryKey();
                stmt = conn.prepareStatement(getInsertSql());
            } else {
                stmt = conn.prepareStatement(getUpdateSql());
            }
            setValues(stmt);
            int rowCount = stmt.executeUpdate();
            if (rowCount != 1) {
                primaryKey = OvUuid.NULL_UUID;
                if (commit) {
                    conn.rollback();
                }
                String errorMessage = "Invalid number of rows changed!";
                if (log.isErrorEnabled()) {
                    log.error(errorMessage);
                }
                throw new SQLException(errorMessage);
            } else {
                if (commit) {
                    conn.commit();
                }
            }
        } finally {
            OvJdbcUtils.closeStatement(stmt);
        }
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof I_OvDao)) {
            throw new ClassCastException("Unable to compare non-DAO objects!");
        }
        return getPrimaryKey().toString().compareTo(((I_OvDao) obj).getPrimaryKey().toString());
    }

    /** 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof I_OvDao)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        I_OvDao otherDao = (I_OvDao) obj;
        return (getPrimaryKey().equals(otherDao.getPrimaryKey()));
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /**
     * @see openvend.main.I_OvDao#getPrimaryKey()
     */
    public Object getPrimaryKey() {
        return primaryKey;
    }

    /**
     * @see openvend.main.I_OvDao#setPrimaryKey(java.lang.Object)
     */
    public void setPrimaryKey(Object primaryKey) {
        this.primaryKey = primaryKey;
    }

    protected abstract String getInsertSql();

    protected abstract String getUpdateSql();

    protected abstract String getSelectSql();

    protected abstract String getDeleteSql();

    protected void setString(PreparedStatement stmt, int index, String value) throws SQLException {
        setValue(stmt, index, value, Types.VARCHAR);
    }

    protected void setLong(PreparedStatement stmt, int index, Long value) throws SQLException {
        setValue(stmt, index, value, Types.BIGINT);
    }

    protected void setDouble(PreparedStatement stmt, int index, Double value) throws SQLException {
        setValue(stmt, index, value, Types.DOUBLE);
    }

    protected void setFloat(PreparedStatement stmt, int index, Float value) throws SQLException {
        setValue(stmt, index, value, Types.FLOAT);
    }

    protected void setInteger(PreparedStatement stmt, int index, Integer value) throws SQLException {
        setValue(stmt, index, value, Types.INTEGER);
    }

    protected void setShort(PreparedStatement stmt, int index, Short value) throws SQLException {
        setValue(stmt, index, value, Types.SMALLINT);
    }

    protected void setBoolean(PreparedStatement stmt, int index, Boolean value) throws SQLException {
        setValue(stmt, index, value, Types.BIT);
    }

    protected void setDate(PreparedStatement stmt, int index, java.util.Date value) throws SQLException {
        setValue(stmt, index, value, Types.DATE);
    }

    protected void setValue(PreparedStatement stmt, int index, Object value, int sqlType) throws SQLException {
        if (value != null) {
            stmt.setObject(index, value, sqlType);
        } else {
            stmt.setNull(index, sqlType);
        }
    }

    protected Long getLong(ResultSet res, String columnName) throws SQLException {
        long longValue = res.getLong(columnName);
        if (!res.wasNull()) {
            return new Long(longValue);
        }
        return null;
    }

    protected Integer getInteger(ResultSet res, String columnName) throws SQLException {
        int intValue = res.getInt(columnName);
        if (!res.wasNull()) {
            return new Integer(intValue);
        }
        return null;
    }

    protected Double getDouble(ResultSet res, String columnName) throws SQLException {
        double doubleValue = res.getDouble(columnName);
        if (!res.wasNull()) {
            return new Double(doubleValue);
        }
        return null;
    }

    protected Float getFloat(ResultSet res, String columnName) throws SQLException {
        float floatValue = res.getFloat(columnName);
        if (!res.wasNull()) {
            return new Float(floatValue);
        }
        return null;
    }

    protected Short getShort(ResultSet res, String columnName) throws SQLException {
        short shortValue = res.getShort(columnName);
        if (!res.wasNull()) {
            return new Short(shortValue);
        }
        return null;
    }

    protected Boolean getBoolean(ResultSet res, String columnName) throws SQLException {
        boolean booleanValue = res.getBoolean(columnName);
        if (!res.wasNull()) {
            return new Boolean(booleanValue);
        }
        return null;
    }

    protected String getString(ResultSet res, String columnName) throws SQLException {
        String stringValue = res.getString(columnName);
        if (!res.wasNull()) {
            return stringValue;
        }
        return null;
    }

    protected java.util.Date getDate(ResultSet res, String columnName) throws SQLException {
        java.util.Date dateValue = res.getDate(columnName);
        if (!res.wasNull()) {
            return dateValue;
        }
        return null;
    }
}

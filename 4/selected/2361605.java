package mipt.rdb.dbms;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.*;
import oracle.jdbc.OracleResultSet;
import oracle.sql.BLOB;
import oracle.sql.CLOB;
import mipt.rdb.DBMSAgent;

/**
 * DBMSAgent for Oracle (with sequences)
 * Explicitely depends on oracle.* packages (because of LOBs) 
 * @author Evdokimov
 */
public class OracleDBMSAgent extends DBMSAgent {

    /**
	 * @see mipt.rdb.DBMSAgent#getDbmsName()
	 */
    public String getDbmsName() {
        return "Oracle";
    }

    /**
	 * @see mipt.rdb.DBMSAgent#getNewIdValue(java.lang.String)
	 */
    public String getNewIdValue(String tableName) {
        return getSequenceName(tableName) + ".NEXTVAL";
    }

    public String getLastIdValue(String tableName) {
        return getSequenceName(tableName) + ".CURRVAL";
    }

    /**
	 * @see mipt.rdb.DBMSAgent#getLastIdQuery(java.lang.String)
	 */
    public String getLastIdQuery(String tableName) {
        return "select " + getLastIdValue(tableName) + " from DUAL";
    }

    /**
	 * Default implementation: sequence should be named as table name but with "_SEQ" ("Seq") postfix!
	 */
    protected String getSequenceName(String tableName) {
        if (tableName.charAt(0) != '"') return tableName + "_SEQ";
        return tableName.substring(0, tableName.length() - 1) + "Seq\"";
    }

    /**
	 * @see mipt.rdb.DBMSAgent#roundDate(java.lang.String, java.lang.String)
	 */
    public String roundDate(String date, String roundMode) {
        if (roundMode != null) date = "TRUNC(" + date + (roundMode.length() == 0 ? ")" : ", '" + roundMode + "')");
        return date;
    }

    /**
	 * @see mipt.rdb.DBMSAgent#db2java(java.lang.String, java.lang.String, java.lang.Object)
	 */
    public Object db2java(String tableName, String columnName, Object value) {
        if (value instanceof BigDecimal) return new Integer(((BigDecimal) value).intValue());
        return super.db2java(tableName, columnName, value);
    }

    /**
	 * Explicitely depends on oracle.sql package!
	 * @see mipt.rdb.DBMSAgent#db2java(java.sql.ResultSet, int, int, boolean)
	 */
    public Object db2java(ResultSet set, int i, int sqlType, boolean createLong) throws SQLException {
        Object obj = super.db2java(set, i, sqlType, createLong);
        if (sqlType != Types.OTHER) return obj;
        boolean isClob = obj instanceof Clob;
        boolean isBlob = isClob ? false : (obj instanceof Blob);
        if (isClob || (obj instanceof CLOB)) {
            Clob clob = isClob ? (Clob) obj : null;
            CLOB _clob = isClob ? null : (CLOB) obj;
            int clen = (int) (isClob ? clob.length() : _clob.length()) + 1;
            StringWriter writer = new StringWriter(clen);
            if (!readAndWrite(isClob ? clob.getCharacterStream() : _clob.getCharacterStream(), writer, clen)) return null; else return writer.getBuffer();
        } else if (isBlob || (obj instanceof BLOB)) {
            Blob blob = isBlob ? (Blob) obj : null;
            BLOB _blob = isBlob ? (BLOB) obj : null;
            int blen = (int) (isBlob ? blob.length() : _blob.length()) + 1;
            ByteArrayOutputStream output = new ByteArrayOutputStream(blen);
            if (!readAndWrite(isBlob ? blob.getBinaryStream() : _blob.getBinaryStream(), output, blen)) return null; else return output.toByteArray();
        }
        return obj;
    }

    /**
	 * @see mipt.rdb.DBMSAgent#getWriter(int, java.sql.ResultSet)
	 */
    protected Writer getWriter(int i, ResultSet set) throws SQLException {
        return ((OracleResultSet) set).getCLOB(i).getCharacterOutputStream();
    }

    /**
	 * @see mipt.rdb.DBMSAgent#getOutputStream(int, java.sql.ResultSet)
	 */
    protected OutputStream getOutputStream(int i, ResultSet set) throws SQLException {
        return ((OracleResultSet) set).getBLOB(i).getBinaryOutputStream();
    }

    /**
	 * @see mipt.rdb.DBMSAgent#getInsertEmptyLOBFunction(boolean)
	 */
    public String getInsertEmptyLOBFunction(boolean isCLOB) {
        return isCLOB ? "EMPTY_CLOB()" : "EMPTY_BLOB()";
    }
}

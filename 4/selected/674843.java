package mipt.rdb;

import java.io.*;
import java.sql.*;
import mipt.search.Criterion;

/**
 * Collaborator of RDB*Table(Factory) depending on DBMS product and version
 * Supports following types:
 *  String - VARCHAR(*)[SQL92]
 *  Boolean - NUMERIC(1)[SQL92] || BIT[few]
 *  Long, Integer - INTEGER[SQL92] || NUMERIC(10)[SQL92]
 *  Double - DECIMAL(30,*)[SQL92] (|| DOUBLE[few])
	   //decimal(30,15) is more than double but since it has fixed point
 	   //(floating point is supported in different ways on different DBMS),
	 //it must has enough accuracy (of 15 digits) for big and small numbers
 *  InputStream - LONG VARBINARY[ODBC, ! SQL92, ! Oracle Server] || LONG RAW[Oracle]
 * Note: depends of mipt.search.Criterion only
 * TO DO: reorganize (and split between Oracle and other implementations) datatype conversions
 *   (note: simple java2db and db2java is not used now). Until done, this class works only for Oracle & Oracle Lite
 * @author Evdokimov
 */
public abstract class DBMSAgent {

    public static class ZeroInputStream extends FilterInputStream {

        public ZeroInputStream() {
            super(null);
        }

        public int available() {
            return 0;
        }
    }

    ;

    /**
	 * Is often coinsides with DatabaseMetaData.getProductName
	 */
    public abstract String getDbmsName();

    /**
	 * Used in INSERT statement
	 */
    public abstract String getNewIdValue(String tableName);

    /**
	 * Used to get new id by adding 1 to the result of this query
	 */
    public abstract String getLastIdQuery(String tableName);

    /**
	 * Used to correct the result of getLastIdQuery (often is needed in case of first call)
	 */
    public String getLastIdQuery(String idColumnName, String tableName) {
        return "select max(" + idColumnName + ") from " + tableName;
    }

    /**
	 * Rounds DATE or TIMESTAMP depending on roundMode: DD (day), MM (month) and so on
	 * Does nothing if roundMode==null. If roundMode="", rounds by default (DD)
	 */
    public String roundDate(String date, String roundMode) {
        return date;
    }

    /**
	 * Converts java object to SQL string value
	 */
    public String java2db(String tableName, String columnName, Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "'" + value.toString() + "'";
        if (value instanceof Date) return "'" + value.toString() + "'";
        return value.toString();
    }

    /**
	 * Converts ResultSet object to java object
	 */
    public Object db2java(String tableName, String columnName, Object value) {
        return value;
    }

    /**
	 * Do not call this method with InputStream or other complex objects!
	 * Not static is because bug in Oracle Lite 4.0.: double vaues can't contain '.' (??!!)
	 * TO DO: convert double values to not-exponent form for DECIMAL(30,15) datatype, not DOUBLE PRECISION
	 *  (not for Oracle Lite)
	 * @return java.lang.String - guaranteed to be not null
	 * @param value java.lang.Object --- where mipt.search.Criterion - fieldValue, notValue, compare are significant
	 * @param sqlType int - 0 to determine by itself
	 * @param purpose int - <=0 for "where" clause; 1 = for update; 2 - for insert
	 //* Note: if purpose==-1 "<> value" or "is not null" is used
	 //* Note: if value=(String) then this is used: purpose==-2 => "like value", purpose==-3 => "not like value"
	 */
    public String java2db(Object value, int sqlType, int purpose, String dateRoundMode) {
        Criterion where = null;
        if (value instanceof Criterion) {
            where = (Criterion) value;
            value = where.fieldValue;
        } else {
            where = new Criterion(-2, value);
        }
        if (value == null) {
            switch(purpose) {
                case 2:
                    return "NULL";
                case 1:
                    return " = NULL";
                case 0:
                default:
                    return where.notValue ? " IS NOT NULL" : " IS NULL";
            }
        }
        if (sqlType == Types.NULL) sqlType = sqlType(value);
        value = java2dbInternal(where, value, sqlType, purpose, dateRoundMode);
        switch(where.fieldIndex) {
            case -3:
                return (String) value;
            case -4:
                return java2db(where, sqlType, purpose, dateRoundMode);
        }
        String result = value.toString();
        switch(purpose) {
            case 2:
                break;
            case 1:
                result = " = " + result;
                break;
            case 0:
            default:
                result = getCompareSymbol(where.notValue, where.compare) + result;
        }
        return result;
    }

    /**
	 * If where.fieldIndex became -3 after call to this method, caller must return immediately
	 *   with the returned result; if -4 - must return java2db(recursiion)
	 * Arguments value and where is not null here! 
	 */
    protected Object java2dbInternal(Criterion where, Object value, int sqlType, int purpose, String dateRoundMode) {
        boolean setDate = true;
        switch(sqlType) {
            case Types.VARCHAR:
                value = '\'' + value.toString() + '\'';
                break;
            case Types.TIME:
                if (setDate && !(value instanceof Time)) {
                    value = new Time(((java.util.Date) value).getTime());
                    setDate = false;
                }
            case Types.DATE:
                if (setDate && !(value instanceof Date)) {
                    value = new Date(((java.util.Date) value).getTime());
                    setDate = false;
                }
            case Types.TIMESTAMP:
                if (setDate && !(value instanceof Timestamp)) {
                    value = new Timestamp(((java.util.Date) value).getTime());
                    setDate = false;
                }
                String date = value.toString();
                int dotIndex = date.lastIndexOf('.');
                if (dotIndex >= 0) date = date.substring(0, dotIndex);
                date = "TO_DATE('" + date + "', 'YYYY-MM-DD HH24:MI:SS')";
                if (purpose == 0 && where.compare == Criterion.EQUAL && sqlType != Types.TIME) date = roundDate(date, dateRoundMode);
                value = date;
                break;
            case Types.BIT:
                if (value instanceof Boolean) {
                    value = new Integer(((Boolean) value).booleanValue() ? 1 : 0);
                    break;
                } else {
                    where.fieldValue = null;
                    where.fieldIndex = -4;
                    return value;
                }
            case Types.INTEGER:
                if (purpose == 0) {
                    String res = javaArray2dbArray(value, where.notValue);
                    if (res != null) {
                        where.fieldIndex = -3;
                        return res;
                    }
                }
                int intValue = 0;
                long longValue = 0;
                if (value instanceof Long) longValue = ((Long) value).longValue(); else intValue = ((Integer) value).intValue();
                if (longValue == Long.MIN_VALUE || intValue == Integer.MIN_VALUE) {
                    where.fieldValue = null;
                    where.fieldIndex = -4;
                    return value;
                }
                break;
            case Types.DOUBLE:
                double doubleValue = ((Double) value).doubleValue();
                if (Double.isNaN(doubleValue)) {
                    where.fieldValue = null;
                    where.fieldIndex = -4;
                    return value;
                }
        }
        return value;
    }

    /**
	 * Sets the given object as i-th argument of the given statement
	 * @param statement java.sql.PreparedStatement
	 * @param i int - parameter index
	 * @param obj java.lang.Object
	 * @param sqlType int - 0 to determine by itself
	 */
    public void java2db(PreparedStatement statement, int i, Object obj, int sqlType) throws java.sql.SQLException {
        if (obj == null) {
            statement.setNull(i, sqlType);
            return;
        }
        if (sqlType == Types.NULL) sqlType = sqlType(obj);
        if (sqlType == Types.NULL) {
            if (obj instanceof java.math.BigDecimal) statement.setBigDecimal(i, (java.math.BigDecimal) obj); else statement.setObject(i, obj);
            return;
        }
        switch(sqlType) {
            case Types.VARCHAR:
                statement.setString(i, (String) obj);
                return;
            case Types.BIT:
                if (obj instanceof Boolean) statement.setBoolean(i, ((Boolean) obj).booleanValue()); else statement.setNull(i, sqlType);
                return;
            case Types.INTEGER:
                if (obj instanceof Long) {
                    long longValue = ((Long) obj).longValue();
                    if (longValue == Long.MIN_VALUE) statement.setNull(i, sqlType); else statement.setLong(i, longValue);
                } else {
                    int intValue = ((Integer) obj).intValue();
                    if (intValue == Integer.MIN_VALUE) statement.setNull(i, sqlType); else statement.setInt(i, intValue);
                }
                return;
            case Types.DOUBLE:
                double doubleValue = ((Double) obj).doubleValue();
                if (Double.isNaN(doubleValue)) statement.setNull(i, sqlType); else statement.setDouble(i, doubleValue);
                return;
            case Types.TIMESTAMP:
                Timestamp timestamp;
                if (obj instanceof Timestamp) timestamp = (Timestamp) obj; else timestamp = new Timestamp(((java.util.Date) obj).getTime());
                statement.setTimestamp(i, timestamp);
                return;
            case Types.DATE:
                Date date;
                if (obj instanceof Date) date = (Date) obj; else date = new Date(((java.util.Date) obj).getTime());
                statement.setDate(i, date);
                return;
            case Types.TIME:
                Time time;
                if (obj instanceof Time) time = (Time) obj; else time = new Time(((java.util.Date) obj).getTime());
                statement.setTime(i, time);
                return;
            case Types.LONGVARBINARY:
            case Types.LONGVARCHAR:
                InputStream stream = (InputStream) obj;
                if (stream instanceof ZeroInputStream) statement.setNull(i, sqlType); else try {
                    if (sqlType == Types.LONGVARCHAR) statement.setAsciiStream(i, stream, stream.available()); else statement.setBinaryStream(i, stream, stream.available());
                } catch (java.io.IOException e) {
                }
                return;
            case Types.OTHER:
            default:
                statement.setObject(i, obj);
                return;
        }
    }

    /**
	 *
	 */
    public String javaArray2dbArray(Object arrayValue, boolean notValue) {
        long[] longs = null;
        int n = 0;
        if (arrayValue instanceof long[]) {
            longs = (long[]) arrayValue;
            n = longs.length;
            if (n <= 1) {
                long value = Long.MAX_VALUE;
                if (n == 1) value = longs[0];
                return (notValue ? " <> " : " = ") + Long.toString(value);
            }
        } else if (arrayValue instanceof int[]) {
            int ints[] = (int[]) arrayValue;
            n = ints.length;
            if (n <= 1) {
                int value = Integer.MAX_VALUE;
                if (n == 1) value = ints[0];
                return (notValue ? " <> " : " = ") + Integer.toString(value);
            } else {
                longs = new long[ints.length];
                for (int i = 0; i < ints.length; i++) longs[i] = ints[i];
            }
        } else {
            return null;
        }
        StringBuffer list = new StringBuffer(5 * n + 10);
        list.append(notValue ? " NOT IN (" : " IN (");
        n--;
        for (int i = 0; i <= n; i++) {
            list.append(Long.toString(longs[i]));
            if (i < n) list.append(", ");
        }
        list.append(')');
        return list.toString();
    }

    /**
	 * Integer and InputStream and Boolean can`t be null, but (boolean)null <=> false
	 */
    protected Object javaNull2realNull(Object obj, int sqlType) {
        if (obj == null) return null;
        if (sqlType == Types.NULL) sqlType = sqlType(obj);
        switch(sqlType) {
            case Types.INTEGER:
                if (obj instanceof Long) {
                    if (((Long) obj).longValue() == Long.MIN_VALUE) return null;
                } else if (((Integer) obj).intValue() == Integer.MIN_VALUE) return null;
                break;
            case Types.DOUBLE:
                if (((Double) obj).isNaN()) return null;
                break;
            case Types.LONGVARBINARY:
            case Types.LONGVARCHAR:
                if (obj instanceof ZeroInputStream) return null;
                break;
        }
        return obj;
    }

    /**
	 * Returns sql type for the object if it is unknown from metadata
	 * @return int - can be Types.NULL if type doesn`t supported
	 * @param obj java.lang.Object
	 */
    protected int sqlType(Object obj) {
        if (obj instanceof String) return Types.VARCHAR; else if (obj instanceof Integer) return Types.INTEGER; else if (obj instanceof Long) return Types.INTEGER; else if (obj instanceof Boolean) return Types.BIT; else if (obj instanceof Double) return Types.DOUBLE; else if (obj instanceof java.util.Date) return Types.TIMESTAMP; else if (obj instanceof InputStream) return Types.LONGVARBINARY; else if ((obj instanceof StringBuffer) || (obj instanceof byte[])) return Types.OTHER; else return Types.NULL;
    }

    /**
	 *
	 */
    public String getCompareSymbol(boolean notValue, int compare) {
        switch(compare) {
            case Criterion.LIKE:
                return notValue ? " NOT LIKE " : " LIKE ";
            case Criterion.MORE:
                return notValue ? " <= " : " > ";
            case Criterion.LESS:
                return notValue ? " >= " : " < ";
            case Criterion.EQUAL:
            default:
                return notValue ? " <> " : " = ";
        }
    }

    /**
	 * 
	 * // Not static is because bug in Cache 4.0.: can't call get*(i) two times
	 * // On Cache do not also call this method in a chaotic order (i values)
	 * @return int
	 * @param set java.sql.ResultSet
	 * @param i int - column index
	 * @param sqlType int
	 * @param createLong boolean - if Long should be created instead of Integer
	 */
    public Object db2java(ResultSet set, int i, int sqlType, boolean createLong) throws SQLException {
        String str;
        switch(sqlType) {
            case Types.VARCHAR:
                return set.getString(i);
            case Types.DECIMAL:
                createLong = true;
            case Types.INTEGER:
                if (createLong) return new Long(set.getString(i) == null ? Long.MIN_VALUE : set.getLong(i)); else return new Integer(set.getString(i) == null ? Integer.MIN_VALUE : set.getInt(i));
            case Types.BIT:
                str = set.getString(i);
                return str == null ? null : (str.equals("1") ? Boolean.TRUE : Boolean.FALSE);
            case Types.DOUBLE:
                return new Double(set.getString(i) == null ? Double.NaN : set.getDouble(i));
            case Types.TIMESTAMP:
                return set.getTimestamp(i);
            case Types.DATE:
                return set.getDate(i);
            case Types.TIME:
                return set.getTime(i);
            case Types.LONGVARBINARY:
                return set.getBinaryStream(i);
            case Types.LONGVARCHAR:
                return set.getAsciiStream(i);
            case Types.OTHER:
                return set.getObject(i);
            default:
                return set.getObject(i);
        }
    }

    /**
	 * Used in insert statement instead of LOB
	 */
    public abstract String getInsertEmptyLOBFunction(boolean isCLOB);

    /**
	 * 
	 */
    public void updateLOB(int i, Object obj, ResultSet set) throws SQLException {
        if (obj instanceof StringBuffer) {
            String str = obj.toString();
            readAndWrite(new StringReader(str), getWriter(i, set), ((StringBuffer) obj).length());
        } else if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            readAndWrite(new ByteArrayInputStream(bytes), getOutputStream(i, set), bytes.length);
        }
    }

    protected abstract Writer getWriter(int i, ResultSet set) throws SQLException;

    protected abstract OutputStream getOutputStream(int i, ResultSet set) throws SQLException;

    /**
	 * Can be used to read Blob objects to byte array
	 */
    public boolean readAndWrite(InputStream input, OutputStream output, int length) {
        if (length <= 0) length = 4096;
        byte buf[] = new byte[length];
        try {
            while (true) {
                int i = input.read(buf);
                if (i <= 0) break;
                output.write(buf, 0, i);
            }
            input.close();
            output.flush();
            output.close();
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /**
	 * Can be used to read Clob objects to character array
	 */
    public boolean readAndWrite(Reader reader, Writer writer, int length) {
        if (length <= 0) length = 4096;
        char buf[] = new char[length];
        try {
            while (true) {
                int i = reader.read(buf);
                if (i <= 0) break;
                writer.write(buf, 0, i);
            }
            reader.close();
            writer.flush();
            writer.close();
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /**
	 * Converts DB data type to one of JDBC Types.* constants
	 * Used for creation column type info from JDBC Metadata
	 */
    public int db2jdbc(short dbSqlType, int size) {
        switch(dbSqlType) {
            case Types.VARCHAR:
            case Types.INTEGER:
            case Types.BIT:
            case Types.DOUBLE:
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.LONGVARBINARY:
            case Types.LONGVARCHAR:
            case Types.OTHER:
                return dbSqlType;
        }
        switch(size) {
            case 1:
                return Types.BIT;
            case 10:
                return Types.INTEGER;
            case 22:
                return Types.INTEGER;
            case 30:
                return Types.DOUBLE;
            default:
                return dbSqlType;
        }
    }

    /**
	 * Converts Types.* constants to DB sql type
	 * Used for decoding column type info from internal values (see db2jdbc) to JDBC Metadata
	 */
    public int jdbc2db(int sqlType) {
        switch(sqlType) {
            case Types.VARCHAR:
            case Types.INTEGER:
                return sqlType;
            case Types.BIT:
                return Types.NUMERIC;
            case Types.DOUBLE:
                return Types.DECIMAL;
            default:
                return sqlType;
        }
    }
}

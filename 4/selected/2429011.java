package goldengate.common.database.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import goldengate.common.database.exception.GoldenGateDatabaseSqlError;

/**
 * Database Value to help getting and setting value from and to database
 * @author Frederic Bregier
 *
 */
public class DbValue {

    /**
     * Real value
     */
    public Object value;

    /**
     * Data Type
     */
    public int type;

    /**
     * Column name
     */
    public String column;

    public DbValue(String value) {
        this.value = value;
        type = Types.VARCHAR;
    }

    public DbValue(String value, boolean LONG) {
        this.value = value;
        type = Types.LONGVARCHAR;
    }

    public DbValue(boolean value) {
        this.value = value;
        type = Types.BIT;
    }

    public DbValue(byte value) {
        this.value = value;
        type = Types.TINYINT;
    }

    public DbValue(short value) {
        this.value = value;
        type = Types.SMALLINT;
    }

    public DbValue(int value) {
        this.value = value;
        type = Types.INTEGER;
    }

    public DbValue(long value) {
        this.value = value;
        type = Types.BIGINT;
    }

    public DbValue(float value) {
        this.value = value;
        type = Types.REAL;
    }

    public DbValue(double value) {
        this.value = value;
        type = Types.DOUBLE;
    }

    public DbValue(byte[] value) {
        this.value = value;
        type = Types.VARBINARY;
    }

    public DbValue(Date value) {
        this.value = value;
        type = Types.DATE;
    }

    public DbValue(Timestamp value) {
        this.value = value;
        type = Types.TIMESTAMP;
    }

    public DbValue(java.util.Date value) {
        this.value = new Timestamp(value.getTime());
        type = Types.TIMESTAMP;
    }

    public DbValue(String value, String name) {
        this.value = value;
        type = Types.VARCHAR;
        column = name;
    }

    public DbValue(String value, String name, boolean LONG) {
        this.value = value;
        type = Types.LONGVARCHAR;
        column = name;
    }

    public DbValue(boolean value, String name) {
        this.value = value;
        type = Types.BIT;
        column = name;
    }

    public DbValue(byte value, String name) {
        this.value = value;
        type = Types.TINYINT;
        column = name;
    }

    public DbValue(short value, String name) {
        this.value = value;
        type = Types.SMALLINT;
        column = name;
    }

    public DbValue(int value, String name) {
        this.value = value;
        type = Types.INTEGER;
        column = name;
    }

    public DbValue(long value, String name) {
        this.value = value;
        type = Types.BIGINT;
        column = name;
    }

    public DbValue(float value, String name) {
        this.value = value;
        type = Types.REAL;
        column = name;
    }

    public DbValue(double value, String name) {
        this.value = value;
        type = Types.DOUBLE;
        column = name;
    }

    public DbValue(byte[] value, String name) {
        this.value = value;
        type = Types.VARBINARY;
        column = name;
    }

    public DbValue(Date value, String name) {
        this.value = value;
        type = Types.DATE;
        column = name;
    }

    public DbValue(Timestamp value, String name) {
        this.value = value;
        type = Types.TIMESTAMP;
        column = name;
    }

    public DbValue(java.util.Date value, String name) {
        this.value = new Timestamp(value.getTime());
        type = Types.TIMESTAMP;
        column = name;
    }

    public DbValue(Reader value, String name) {
        this.value = value;
        type = Types.CLOB;
        column = name;
    }

    public DbValue(InputStream value, String name) {
        this.value = value;
        type = Types.BLOB;
        column = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    public void setValue(short value) {
        this.value = value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public void setValue(Date value) {
        this.value = value;
    }

    public void setValue(Timestamp value) {
        this.value = value;
    }

    public void setValue(java.util.Date value) {
        this.value = new Timestamp(value.getTime());
    }

    public void setValue(Reader value) {
        this.value = value;
    }

    public void setValue(InputStream value) {
        this.value = value;
    }

    public Object getValue() throws GoldenGateDatabaseSqlError {
        switch(type) {
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.BIT:
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.VARBINARY:
            case Types.DATE:
            case Types.TIMESTAMP:
            case Types.CLOB:
            case Types.BLOB:
                return value;
            default:
                throw new GoldenGateDatabaseSqlError("Type unknown: " + type);
        }
    }

    public String getValueAsString() throws GoldenGateDatabaseSqlError {
        switch(type) {
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                return (String) value;
            case Types.BIT:
                return ((Boolean) value).toString();
            case Types.TINYINT:
                return ((Byte) value).toString();
            case Types.SMALLINT:
                return ((Short) value).toString();
            case Types.INTEGER:
                return ((Integer) value).toString();
            case Types.BIGINT:
                return ((Long) value).toString();
            case Types.REAL:
                return ((Float) value).toString();
            case Types.DOUBLE:
                return ((Double) value).toString();
            case Types.VARBINARY:
                return new String((byte[]) value);
            case Types.DATE:
                return ((Date) value).toString();
            case Types.TIMESTAMP:
                return ((Timestamp) value).toString();
            case Types.CLOB:
                {
                    StringBuilder sBuilder = new StringBuilder();
                    Reader reader = ((Reader) value);
                    char[] cbuf = new char[4096];
                    int len;
                    try {
                        len = reader.read(cbuf);
                        while (len > 0) {
                            sBuilder.append(cbuf, 0, len);
                            len = reader.read(cbuf);
                        }
                    } catch (IOException e) {
                        throw new GoldenGateDatabaseSqlError("Error while reading Clob as String", e);
                    }
                    return sBuilder.toString();
                }
            case Types.BLOB:
                {
                    InputStream reader = ((InputStream) value);
                    int len;
                    ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
                    try {
                        len = reader.available();
                        while (len > 0) {
                            buffer.writeBytes(reader, len);
                        }
                    } catch (IOException e) {
                        throw new GoldenGateDatabaseSqlError("Error while reading Clob as String", e);
                    }
                    len = buffer.readableBytes();
                    byte[] dst = new byte[len];
                    buffer.readBytes(dst);
                    return new String((byte[]) dst);
                }
            default:
                throw new GoldenGateDatabaseSqlError("Type unknown: " + type);
        }
    }

    public void setValueFromString(String svalue) throws GoldenGateDatabaseSqlError {
        switch(type) {
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                value = svalue;
                break;
            case Types.BIT:
                value = Boolean.parseBoolean(svalue);
                break;
            case Types.TINYINT:
                value = Byte.parseByte(svalue);
                break;
            case Types.SMALLINT:
                value = Short.parseShort(svalue);
                break;
            case Types.INTEGER:
                value = Integer.parseInt(svalue);
                break;
            case Types.BIGINT:
                value = Long.parseLong(svalue);
                break;
            case Types.REAL:
                value = Float.parseFloat(svalue);
                break;
            case Types.DOUBLE:
                value = Double.parseDouble(svalue);
                break;
            case Types.VARBINARY:
                value = svalue.getBytes();
                break;
            case Types.DATE:
                try {
                    value = DateFormat.getDateTimeInstance().parse(svalue);
                } catch (ParseException e) {
                    throw new GoldenGateDatabaseSqlError("Error in Date: " + svalue, e);
                }
                break;
            case Types.TIMESTAMP:
                try {
                    value = DateFormat.getDateTimeInstance().parse(svalue);
                } catch (ParseException e) {
                    throw new GoldenGateDatabaseSqlError("Error in Timestamp: " + svalue, e);
                }
                break;
            case Types.CLOB:
                try {
                    value = new InputStreamReader(new FileInputStream(svalue));
                } catch (FileNotFoundException e) {
                    throw new GoldenGateDatabaseSqlError("Error in CLOB: " + svalue, e);
                }
                break;
            case Types.BLOB:
                try {
                    value = new FileInputStream(svalue);
                } catch (FileNotFoundException e) {
                    throw new GoldenGateDatabaseSqlError("Error in BLOB: " + svalue, e);
                }
                break;
            default:
                throw new GoldenGateDatabaseSqlError("Type unknown: " + type);
        }
    }
}

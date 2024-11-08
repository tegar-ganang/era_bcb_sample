package edu.washington.mysms.coding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import edu.washington.mysms.Message;

/**
 * A result coder returns results in a serialized packet format as follows:
 * 
 * <p>[#Cols | ColumnsDefinition] [#Rows | Rows ]</p>
 * <p>Where the columns definition defines the name and length of each field
 * in the following rows.  For variable length rows, a 4-byte unsigned int
 * precedes the field to denote the length.</p>
 * 
 * @author Anthony Poon
 */
public class FieldsOnlyResultCoder implements ResultDecoder, ResultEncoder {

    private boolean gzip;

    /**
	 * Construct a new FieldsOnlyResultCoder using the default charset: UTF-8.
	 * Will GZIP the encoding to try to shorten it.
	 */
    public FieldsOnlyResultCoder() {
        this.gzip = true;
    }

    /**
	 * Construct a new FieldsOnlyResultCoder using the default charset: UTF-8.
	 * Set whether to GZIP the encoding to try to save space.
	 * 
	 * @param gzip Whether to GZIP or not.
	 */
    public FieldsOnlyResultCoder(boolean gzip) {
        this.gzip = gzip;
    }

    /**
	 * Encode and serialize the given ResultTable into a buffer.
	 * 
	 * @param message The containing message.
	 * @param result The table to encode.
	 * @return A buffer that represents the table.
	 * @throws Exception If a failure occurs during encoding this message.
	 */
    public ByteBuffer encode(Message<?> message, ResultTable result) throws IOException, IndexOutOfBoundsException {
        EncodingStream out = new EncodingStream();
        ColumnsDefinition columns = result.getColumnsDefinition();
        if (result.size() > Short.MAX_VALUE) {
            throw new IndexOutOfBoundsException("Too many columns in this table.  Max supported # of columns is " + Short.MAX_VALUE + ".");
        }
        short numColumns = (short) columns.size();
        out.encodeShort(numColumns);
        for (ResultColumn column : columns) {
            encodeColumn(out, column);
        }
        int numRows = result.size();
        out.encodeInteger(numRows);
        for (ResultRow row : result) {
            encodeRow(out, row);
        }
        out.close();
        if (this.gzip) {
            ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
            GZIPOutputStream gz_out = new GZIPOutputStream(ba_out);
            gz_out.write(out.toByteArray());
            gz_out.close();
            return ByteBuffer.wrap(ba_out.toByteArray());
        } else {
            return ByteBuffer.wrap(out.toByteArray());
        }
    }

    /**
	 * Decode the given buffer into an instance of a ResultTable.
	 * 
	 * @param message The containing message.
	 * @param buffer A buffer that represents the results.
	 * @return A table that contains the decoded results.
	 * @throws Exception If a failure occurs during decoding this message.
	 */
    public ResultTable decode(Message<?> message, ByteBuffer buffer) throws IOException, ClassNotFoundException, IndexOutOfBoundsException {
        buffer.rewind();
        if (this.gzip) {
            ByteArrayInputStream ba_in = new ByteArrayInputStream(buffer.array());
            GZIPInputStream gz_in = new GZIPInputStream(ba_in);
            ByteArrayOutputStream ba_out = new ByteArrayOutputStream(buffer.capacity());
            while (gz_in.available() == 1) {
                ba_out.write(gz_in.read());
            }
            buffer = ByteBuffer.wrap(ba_out.toByteArray());
        }
        DecodingStream in = new DecodingStream(buffer.array());
        short numColumns = in.decodeShort();
        if (numColumns < 0) {
            throw new IndexOutOfBoundsException("Buffer indicates a negative number of columns.");
        }
        ColumnsDefinition columns = new ColumnsDefinition(numColumns);
        for (int i = 0; i < numColumns; i++) {
            columns.add(decodeColumn(in));
        }
        columns.setFinalized();
        ResultTable result = new ResultTable(columns);
        int numRows = in.decodeInteger();
        for (int i = 0; i < numRows; i++) {
            result.add(decodeRow(in, columns));
        }
        return result;
    }

    /**
	 * Return an instance of a corresponding encoder capable of creating the
	 * encoder buffers that this decoder understands.
	 * 
	 * @return An instance of a ResultEncoder for the corresponding type.
	 */
    public ResultEncoder getEncoder() {
        return this;
    }

    /**
	 * Return an instance of a corresponding decoder capable of reconstituting
	 * results from the buffers encoded by this encoder.
	 * 
	 * @return An instance of a ResultDecoder for the corresponding type.
	 */
    public ResultDecoder getDecoder() {
        return this;
    }

    /**
	 * Return a byte that represents the type of encoding used by this encoder.
	 * This byte does not necessarily need to be included in any encoded results.
	 * 
	 * @return A byte representing encoding type.
	 */
    public byte getEncodingType() {
        return 2;
    }

    private static final byte UNRECOGNIZED = 0;

    private static final byte BOOLEAN = 1;

    private static final byte SHORT = 2;

    private static final byte INTEGER = 3;

    private static final byte LONG = 4;

    private static final byte FLOAT = 5;

    private static final byte DOUBLE = 6;

    private static final byte STRING = 7;

    private static final byte DATE = 8;

    private static final byte TIME = 9;

    private static final byte TIMESTAMP = 10;

    /**
	 * Encode and write the given column to the given OutputStream.
	 * 
	 * @param out The stream to write to.
	 * @param column The column to encode.
	 * @throws IOException If an error occurs during writing.
	 */
    private void encodeColumn(EncodingStream out, ResultColumn column) throws IOException {
        out.encodeString(column.name);
        byte type_number = UNRECOGNIZED;
        if (column.type.equals(Boolean.class)) {
            type_number = BOOLEAN;
        } else if (column.type.equals(Short.class)) {
            type_number = SHORT;
        } else if (column.type.equals(Integer.class)) {
            type_number = INTEGER;
        } else if (column.type.equals(Long.class)) {
            type_number = LONG;
        } else if (column.type.equals(Float.class)) {
            type_number = FLOAT;
        } else if (column.type.equals(Double.class)) {
            type_number = DOUBLE;
        } else if (column.type.equals(String.class)) {
            type_number = STRING;
        } else if (column.type.equals(Date.class)) {
            type_number = DATE;
        } else if (column.type.equals(Time.class)) {
            type_number = TIME;
        } else if (column.type.equals(Timestamp.class)) {
            type_number = TIMESTAMP;
        }
        out.encodeByte(type_number);
        if (type_number == UNRECOGNIZED) {
            out.encodeObject(column.type);
        }
        out.encodeBoolean(column.nullable);
    }

    /**
	 * Decode a ResultColumn from the given buffer.
	 * 
	 * @param in The buffer to read from.
	 * @return A ResultColumn read from the buffer.
	 * @throws IOException If an error occurred during reading.
	 * @throws ClassNotFoundException If the correct class could not be found while decoding objects.
	 */
    private ResultColumn decodeColumn(DecodingStream in) throws IOException, ClassNotFoundException {
        String name = in.decodeString();
        Class<?> type = null;
        byte type_number = in.decodeByte();
        switch(type_number) {
            case BOOLEAN:
                type = Boolean.class;
                break;
            case SHORT:
                type = Short.class;
                break;
            case INTEGER:
                type = Integer.class;
                break;
            case LONG:
                type = Long.class;
                break;
            case FLOAT:
                type = Float.class;
                break;
            case DOUBLE:
                type = Double.class;
                break;
            case STRING:
                type = String.class;
                break;
            case DATE:
                type = Date.class;
                break;
            case TIME:
                type = Time.class;
                break;
            case TIMESTAMP:
                type = Timestamp.class;
                break;
            default:
                type = (Class<?>) in.decodeObject();
                break;
        }
        boolean nullable = in.decodeBoolean();
        if (name == null || type == null) {
            throw new NullPointerException("Could not parse name or type.");
        }
        return new ResultColumn(name, nullable, type);
    }

    /**
	 * Encode and write the given row to the given stream.
	 * 
	 * @param out The stream to write to.
	 * @param row The row to encode and write.
	 * @throws ComplianceException If the row given is not compliant.
	 * @throws IOException If an error occurred during writing.
	 */
    private void encodeRow(EncodingStream out, ResultRow row) throws ComplianceException, IOException {
        if (!row.isCompliant()) {
            throw new ComplianceException("Cannot encode a non-compliant row.");
        }
        ColumnsDefinition columns = row.getColumnsDefinition();
        for (int i = 0; i < columns.size(); i++) {
            ResultColumn column = columns.get(i);
            Object value = row.get(i);
            if (column.nullable) {
                out.encodeBoolean(value == null);
            }
            if (column.type.equals(Boolean.class)) {
                out.encodeBoolean(((Boolean) value).booleanValue());
            } else if (column.type.equals(Short.class)) {
                out.encodeShort(((Short) value).shortValue());
            } else if (column.type.equals(Integer.class)) {
                out.encodeInteger(((Integer) value).intValue());
            } else if (column.type.equals(Long.class)) {
                out.encodeLong(((Long) value).longValue());
            } else if (column.type.equals(Float.class)) {
                out.encodeFloat(((Float) value).floatValue());
            } else if (column.type.equals(Double.class)) {
                out.encodeDouble(((Double) value).doubleValue());
            } else if (column.type.equals(String.class)) {
                out.encodeString((String) value);
            } else if (column.type.equals(Date.class)) {
                out.encodeDate((Date) value);
            } else if (column.type.equals(Time.class)) {
                out.encodeTime((Time) value);
            } else if (column.type.equals(Timestamp.class)) {
                out.encodeTimestamp((Timestamp) value);
            } else {
                out.encodeObject(value);
            }
        }
    }

    /**
	 * Decode a ResultRow from the given buffer that is compliant
	 * with the given ColumnsDefinition.
	 * 
	 * @param in The buffer to read from.
	 * @param columns The definition to ensure compliance.
	 * @return A row read from the buffer.
	 * @throws ComplianceException If the row read is non-compliant with the definition.
	 * @throws IOException If an error occurred during reading.
	 * @throws ClassNotFoundException If the class was not found while reading an object.
	 */
    private ResultRow decodeRow(DecodingStream in, ColumnsDefinition columns) throws ComplianceException, IOException, ClassNotFoundException {
        ResultRow row = new ResultRow(columns);
        for (int i = 0; i < columns.size(); i++) {
            ResultColumn column = columns.get(i);
            Object value = null;
            if (column.nullable) {
                boolean isNull = in.decodeBoolean();
                if (isNull) {
                    value = null;
                    row.set(i, value);
                    continue;
                }
            }
            if (column.type.equals(Boolean.class)) {
                value = new Boolean(in.decodeBoolean());
            } else if (column.type.equals(Short.class)) {
                value = new Short(in.decodeShort());
            } else if (column.type.equals(Integer.class)) {
                value = new Integer(in.decodeInteger());
            } else if (column.type.equals(Long.class)) {
                value = new Long(in.decodeLong());
            } else if (column.type.equals(Float.class)) {
                value = new Float(in.decodeFloat());
            } else if (column.type.equals(Double.class)) {
                value = new Double(in.decodeDouble());
            } else if (column.type.equals(String.class)) {
                value = in.decodeString();
            } else if (column.type.equals(Date.class)) {
                value = in.decodeDate();
            } else if (column.type.equals(Time.class)) {
                value = in.decodeTime();
            } else if (column.type.equals(Timestamp.class)) {
                value = in.decodeTimestamp();
            } else {
                value = in.decodeObject();
            }
            row.set(i, value);
        }
        if (!row.isCompliant()) {
            throw new ComplianceException("Decoded a non-compliant row.");
        }
        return row;
    }
}

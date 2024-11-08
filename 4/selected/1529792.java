package com.completex.objective.components.persistency.type;

import com.completex.objective.components.persistency.OdalPersistencyException;
import com.completex.objective.components.persistency.OdalRuntimePersistencyException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * @author Gennady Krizhevsky
 */
public class ClobImpl implements Clob, Externalizable {

    public static final ClobImpl NULL_CLOB = new ClobImpl();

    static {
        NULL_CLOB.setData("");
    }

    private static final String MESSAGE = "This operation is unsupported in the implementation that is used to set data only";

    private String data;

    private Reader reader;

    private InputStream inputStream;

    private String charsetName;

    private int chunkSize = TypeHandler.DEFAULT_CHUNK_SIZE;

    public ClobImpl() {
    }

    public ClobImpl(String data, String charsetName) {
        this(data);
        this.charsetName = charsetName;
    }

    public ClobImpl(String data) {
        this.data = data;
        this.reader = new StringReader(data);
    }

    public ClobImpl(Reader reader, String charsetName) {
        this.reader = reader;
        this.charsetName = charsetName;
    }

    public ClobImpl(Reader reader) {
        this.reader = reader;
    }

    public ClobImpl(InputStream inputStream, String charsetName) {
        this.inputStream = inputStream;
        this.charsetName = charsetName;
    }

    public ClobImpl(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public long length() throws SQLException {
        return data == null ? 0 : data.length();
    }

    public String getString() throws SQLException {
        if (data == null) {
            if (reader != null) {
                CharArrayWriter charArrayWriter = new CharArrayWriter();
                writeClob(chunkSize, reader, charArrayWriter);
                data = charArrayWriter.toString();
            } else if (inputStream != null) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                writeClob(chunkSize, inputStream, outputStream);
                if (charsetName == null) {
                    data = new String(outputStream.toByteArray());
                } else {
                    try {
                        data = new String(outputStream.toByteArray(), charsetName);
                    } catch (UnsupportedEncodingException e) {
                        throw new OdalRuntimePersistencyException("Unsupported charset [" + charsetName + "]", e);
                    }
                }
            }
        }
        return data;
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        charsetName = (String) in.readObject();
        data = (String) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            out.writeObject(charsetName);
            out.writeObject(getString());
        } catch (SQLException e) {
            throw new IOException(e.toString());
        }
    }

    public static void writeClob(InputStream inputStream, OutputStream outputStream) {
        writeClob(TypeHandler.DEFAULT_CHUNK_SIZE, inputStream, outputStream);
    }

    public static void writeClob(int chunkSize, InputStream inputStream, OutputStream outputStream) {
        byte[] chunk = new byte[chunkSize];
        int len;
        try {
            while ((len = inputStream.read(chunk, 0, chunkSize)) > -1) {
                outputStream.write(chunk, 0, len);
            }
        } catch (IOException e) {
            throw new OdalRuntimePersistencyException(e.toString());
        }
    }

    public static void writeClob(Reader reader, Writer charArrayWriter) {
        writeClob(TypeHandler.DEFAULT_CHUNK_SIZE, reader, charArrayWriter);
    }

    public static void writeClob(int chunkSize, Reader reader, Writer charArrayWriter) {
        char[] chunk = new char[chunkSize];
        int len;
        try {
            while ((len = reader.read(chunk, 0, chunkSize)) > -1) {
                charArrayWriter.write(chunk, 0, len);
            }
        } catch (IOException e) {
            throw new OdalRuntimePersistencyException(String.valueOf(e));
        }
    }

    public String getCharsetName() {
        return charsetName;
    }

    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }

    public String getSubString(long pos, int length) throws SQLException {
        if (pos < 1 || pos > this.length()) {
            throw new OdalRuntimePersistencyException("Invalid position in CLOB object set");
        }
        if ((pos - 1) + length > this.length()) {
            throw new OdalRuntimePersistencyException("Invalid position and substring length combination for this CLOB object");
        }
        try {
            return getString().substring((int) (pos - 1), length);
        } catch (IndexOutOfBoundsException e) {
            throw new OdalRuntimePersistencyException(e.toString());
        }
    }

    public Reader getCharacterStream() throws SQLException {
        if (data != null) {
            return new StringReader(data);
        } else {
            return reader;
        }
    }

    public InputStream getAsciiStream() throws SQLException {
        InputStream stream = inputStream;
        try {
            if (data != null) {
                byte[] bytes = charsetName == null ? data.getBytes() : data.getBytes(charsetName);
                stream = new ByteArrayInputStream(bytes);
            }
        } catch (UnsupportedEncodingException e) {
            throw new OdalPersistencyException(e);
        }
        return stream;
    }

    public long position(String searchstr, long start) throws SQLException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public long position(Clob searchstr, long start) throws SQLException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int setString(long pos, String str) throws SQLException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public int setString(long pos, String str, int offset, int len) throws SQLException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public OutputStream setAsciiStream(long pos) throws SQLException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public Writer setCharacterStream(long pos) throws SQLException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public void truncate(long len) throws SQLException {
        throw new UnsupportedOperationException(MESSAGE);
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Reader getReader() {
        return reader;
    }

    public void setReader(Reader reader) {
        this.reader = reader;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void free() throws SQLException {
    }

    public Reader getCharacterStream(long pos, long length) throws SQLException {
        return null;
    }
}

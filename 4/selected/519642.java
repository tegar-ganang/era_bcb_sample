package maze.common.adv_io.basic.impl;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import maze.common.adv_io.basic.BasicReader;
import maze.commons.adv_shared.io.impl.LimitedInputStream;
import maze.commons.shared.base.BasicBase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Normunds Mazurs (MAZE)
 * 
 */
public class BasicReaderImpl extends BasicBase implements BasicReader {

    protected final DataInputStream dataInputStream;

    protected DataInputStream createDataInputStream(final InputStream InputStream) {
        return new DataInputStream(InputStream);
    }

    protected BasicReaderImpl(final InputStream InputStream) {
        this.dataInputStream = createDataInputStream(InputStream);
    }

    public static BasicReader create(final InputStream inputStream) {
        return new BasicReaderImpl(inputStream);
    }

    public static BasicReader create(final byte[] a, final int offset, final int length) {
        assert a != null;
        assert length >= 0;
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(a, offset, length);
        return create(byteArrayInputStream);
    }

    public static BasicReader create(final byte[] a, final int offset) {
        assert a != null;
        assert offset >= 0;
        return create(a, offset, a.length - offset);
    }

    public static BasicReader create(final byte[] a) {
        return create(a, 0);
    }

    public static BasicReader create(final ByteBuffer buffer, final int offset, final int length) {
        assert buffer != null;
        return create(buffer.array(), offset, length);
    }

    public static BasicReader create(final ByteBuffer buffer, final int offset) {
        assert buffer != null;
        return create(buffer.array(), offset);
    }

    public static BasicReader create(final ByteBuffer buffer) {
        return create(buffer, 0);
    }

    public static BasicReader createFromFile(final File file) throws IOException {
        return create(FileUtils.openInputStream(file));
    }

    public static BasicReader createFromFile(final String filePath) throws IOException {
        return createFromFile(new File(filePath));
    }

    @Override
    public int readInt() {
        try {
            return dataInputStream.readInt();
        } catch (IOException e) {
            throw createRuntimeException(e);
        }
    }

    @Override
    public int readUnsignedShort() {
        try {
            return dataInputStream.readUnsignedShort();
        } catch (IOException e) {
            throw createRuntimeException(e);
        }
    }

    protected InputStream createLimitedInputStream(final int limit) {
        return new LimitedInputStream(dataInputStream, limit);
    }

    @Override
    public String readFixString(final int len) {
        if (len < 1) {
            return StringUtils.EMPTY;
        }
        final StringWriter sw = new StringWriter();
        try {
            IOUtils.copy(createLimitedInputStream(len), sw, null);
        } catch (IOException e) {
            throw createRuntimeException(e);
        }
        return sw.toString();
    }

    @Override
    public int readFixBytes(final byte[] bytes) {
        if (bytes == null || bytes.length < 1) {
            return 0;
        }
        try {
            dataInputStream.readFully(bytes);
            return bytes.length;
        } catch (Exception e) {
            throw createRuntimeException(e);
        }
    }

    @Override
    public String readString() {
        final int len = readInt();
        return readFixString(len);
    }

    @Override
    public Object readFixBean() {
        try {
            return new ObjectInputStream(dataInputStream).readObject();
        } catch (Exception e) {
            throw createRuntimeException(e);
        }
    }

    @Override
    public boolean readBoolean() {
        try {
            return dataInputStream.readBoolean();
        } catch (IOException e) {
            throw createRuntimeException(e);
        }
    }

    @Override
    public byte readByte() {
        try {
            final int i = dataInputStream.read();
            if (i == -1) {
                throw new EOFException();
            }
            return (byte) i;
        } catch (IOException e) {
            throw createRuntimeException(e);
        }
    }

    @Override
    public boolean isEndReached() {
        try {
            return dataInputStream.available() < 1;
        } catch (IOException e) {
            throw createRuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            dataInputStream.close();
        } catch (IOException e) {
            throw createRuntimeException(e);
        }
    }
}

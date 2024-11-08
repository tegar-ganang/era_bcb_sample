package lab.formats;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;

public class RawBinaryFormatReader implements RawFormatReader {

    private static byte[] reverse(byte[] b) {
        byte[] res = new byte[b.length];
        for (int i = 0; i < b.length; i++) {
            res[b.length - 1 - i] = b[i];
        }
        return res;
    }

    private ByteBuffer m_mbb;

    private long m_size;

    private boolean m_isReversed;

    /**
	 * Ctor. Reads size bytes from the file starting from given offset. This
	 * region of file will be mapped to memory or fully read to an array
	 * 
	 * @param filename
	 *            the file with data
	 * @param offset
	 *            starting offset
	 * @param size
	 *            num of bytes to read / map
	 * @param isMapped
	 *            if false the content is read into an array, otherwise the
	 *            region is just memory mapped.
	 * @throws IOException
	 */
    public RawBinaryFormatReader(String filename, long offset, long size, boolean isMapped, boolean isReversed) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(filename, "r");
        if (!isMapped) {
            byte b[] = new byte[(int) size];
            raf.read(b, (int) offset, (int) size);
            raf.close();
            m_mbb = ByteBuffer.wrap(b);
        } else {
            MappedByteBuffer buffer = raf.getChannel().map(MapMode.READ_ONLY, offset, size);
            raf.close();
            m_mbb = buffer;
        }
        m_size = size;
        m_isReversed = isReversed;
    }

    public double readDouble() throws IOException {
        if (!m_isReversed) {
            return m_mbb.getDouble();
        }
        byte[] b = new byte[Double.SIZE / 8];
        m_mbb.get(b);
        b = reverse(b);
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(b));
        return din.readDouble();
    }

    public float readFloat() throws IOException {
        if (!m_isReversed) {
            return m_mbb.getFloat();
        }
        byte[] b = new byte[Float.SIZE / 8];
        m_mbb.get(b);
        b = reverse(b);
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(b));
        return din.readFloat();
    }

    public int readInt() throws IOException {
        if (!m_isReversed) {
            return m_mbb.getInt();
        }
        byte[] b = new byte[Integer.SIZE / 8];
        m_mbb.get(b);
        b = reverse(b);
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(b));
        return din.readInt();
    }

    public void seek(int offset) throws IOException {
        m_mbb.position(offset);
    }

    public long length() throws IOException {
        return m_size;
    }
}

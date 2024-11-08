package jaxlib.io.stream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.WritableByteChannel;
import jaxlib.jaxlib_private.CheckArg;
import jaxlib.jaxlib_private.io.CheckIOArg;

/**
 * Extented version of <tt>java.io.RandomAccessFile</tt> providing additional functionality.
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: XRandomAccessFile.java 1059 2004-04-08 18:22:44Z joerg_wassmer $
 */
public class XRandomAccessFile extends RandomAccessFile implements XDataInput, XDataOutput {

    private final File canonicalFile;

    private final File file;

    public XRandomAccessFile(File file, String mode) throws IOException {
        super(file, mode);
        this.file = file;
        this.canonicalFile = file.getCanonicalFile();
    }

    /**
   * Returns the canonical form of the file specified at construction time.
   *
   * @see File#getCanonicalFile()
   *
   * @since JaXLib 1.0
   */
    public final File getCanonicalFile() {
        return this.canonicalFile;
    }

    /**
   * Returns the file specified at construction time.
   *
   * @since JaXLib 1.0
   */
    public final File getFile() {
        return this.file;
    }

    public final int available() throws IOException {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, length() - getFilePointer()));
    }

    public void closeInstance() throws IOException {
        close();
    }

    public void flush() throws IOException {
        getChannel().force(true);
    }

    public final boolean isOpen() {
        return getChannel().isOpen();
    }

    public int read(ByteBuffer dest) throws IOException {
        return getChannel().read(dest);
    }

    public void readFully(ByteBuffer dest) throws IOException {
        RandomAccessFiles.readFully(this, dest);
    }

    public void readFully(CharBuffer dest) throws IOException {
        RandomAccessFiles.readFully(this, dest);
    }

    public void readFully(DoubleBuffer dest) throws IOException {
        RandomAccessFiles.readFully(this, dest);
    }

    public void readFully(FloatBuffer dest) throws IOException {
        RandomAccessFiles.readFully(this, dest);
    }

    public void readFully(IntBuffer dest) throws IOException {
        RandomAccessFiles.readFully(this, dest);
    }

    public void readFully(LongBuffer dest) throws IOException {
        RandomAccessFiles.readFully(this, dest);
    }

    public void readFully(ShortBuffer dest) throws IOException {
        RandomAccessFiles.readFully(this, dest);
    }

    public long readUnsignedInt() throws IOException {
        return ((long) readInt()) & 0x7FFFFFFFFFFFFFFFL;
    }

    public long skip(long count) throws IOException {
        return RandomAccessFiles.skip(this, count);
    }

    public final int skipBytes(int count) throws IOException {
        return super.skipBytes(count);
    }

    public final void skipFully(long count) throws IOException {
        if (skip(count) < count) throw new EOFException();
    }

    public long transferFrom(InputStream in, long maxCount) throws IOException {
        return DataIOImpl.transferBytes(in, this, maxCount);
    }

    public void transferBytesFullyFrom(DataInput in, long count) throws IOException {
        DataIOImpl.transferBytesFully(in, this, count);
    }

    public long transferBytesTo(DataOutput dest, long maxCount) throws IOException {
        return RandomAccessFiles.transferBytes(this, dest, maxCount);
    }

    public long transferTo(OutputStream dest, long maxCount) throws IOException {
        return RandomAccessFiles.transfer(this, dest, maxCount);
    }

    public long transferToByteChannel(WritableByteChannel dest, long maxCount) throws IOException {
        return RandomAccessFiles.transferToByteChannel(this, dest, maxCount);
    }

    public int write(ByteBuffer source) throws IOException {
        return RandomAccessFiles.write(this, source);
    }

    public int write(CharBuffer source) throws IOException {
        return RandomAccessFiles.write(this, source);
    }

    public int write(DoubleBuffer source) throws IOException {
        return RandomAccessFiles.write(this, source);
    }

    public int write(FloatBuffer source) throws IOException {
        return RandomAccessFiles.write(this, source);
    }

    public int write(IntBuffer source) throws IOException {
        return RandomAccessFiles.write(this, source);
    }

    public int write(LongBuffer source) throws IOException {
        return RandomAccessFiles.write(this, source);
    }

    public int write(ShortBuffer source) throws IOException {
        return RandomAccessFiles.write(this, source);
    }
}

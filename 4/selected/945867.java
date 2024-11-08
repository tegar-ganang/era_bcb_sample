package net.sourceforge.chopchophttpclient.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This class writes the first memCapacity bytes to a byte array in memory and any further writes beyond that to
 * a temporary file on disk.  The temporary file will be marked for deletion with {@link File#deleteOnExit()} and
 * if the instantiated object has {@link #finalize()} called during garbage collection the {@link File#delete()}
 * will be called.
 * 
 * <p>
 * A temporary zero byte file is always created upon instantiation of this class as an object. 
 * </p>
 * 
 * <p>
 * Use toByteMultiArray() or writeTo() to get all the bytes.  Order will always be bytes in memory first followed
 * by bytes from the disk buffer.  If there are less bytes written than in memory than memCapacity then only the
 * number of actually written bytes will be returned.
 * </p>
 * 
 * <p>
 * The maximum number of total bytes supported is defined by {@link #MAXIMUM_SIZE}.
 * </p>
 * 
 * <p>
 * The maximum possible size of the in-memory buffer is {@link Integer#MAX_VALUE}. 
 * </p>
 *  
 * @author rbeede
 * 
 * @version 2009-05-11
 */
public class MemoryAndDiskOutputStream extends OutputStream {

    /**
	 * Used for reading from the temporary file on disk
	 */
    public static final int BLOCK_BUFFER_SIZE = 64 * 1024;

    /**
	 * 2^40 or 1 tebibyte
	 */
    public static final long MAXIMUM_SIZE = 1099511627776L;

    protected final byte[] memBuffer;

    protected int memBufferCount = 0;

    /**
	 * Total number of bytes written to either memory or the disk cache
	 */
    protected long count = 0;

    private final File tmpDiskBuffer;

    private final FileOutputStream tmpDiskBufferFOS;

    /**
	 * Calling this constructor also creates the zero byte disk buffer file on disk
	 * 
	 * @param memCapacity Initial capacity for the in-memory buffer.  Must not be negative.
	 * @throws IOException If a temporary file for the on-disk buffer file could not be created
	 * @throws IllegalArgumentException If memCapacity is negative
	 */
    public MemoryAndDiskOutputStream(final int memCapacity) throws IOException {
        if (memCapacity < 0) {
            throw new IllegalArgumentException("Negative initial memory capacity: " + memCapacity);
        }
        this.memBuffer = new byte[memCapacity];
        this.tmpDiskBuffer = File.createTempFile(MemoryAndDiskOutputStream.class.getSimpleName(), null);
        this.tmpDiskBuffer.deleteOnExit();
        this.tmpDiskBufferFOS = new FileOutputStream(this.tmpDiskBuffer);
    }

    @Override
    public void write(int b) throws IOException {
        if (this.count >= MAXIMUM_SIZE) {
            throw new IOException("Maximum size of " + MAXIMUM_SIZE + " has been reached");
        }
        if (this.count < this.memBufferCount) {
            this.memBuffer[this.memBufferCount] = (byte) b;
            this.memBufferCount++;
        } else {
            this.tmpDiskBufferFOS.write(b);
        }
        this.count++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (this.count >= MAXIMUM_SIZE) {
            throw new IOException("Maximum size of " + MAXIMUM_SIZE + " has been reached");
        }
        final int inMemBufferLength;
        if (len > (this.memBuffer.length - this.memBufferCount)) {
            inMemBufferLength = (this.memBuffer.length - this.memBufferCount);
        } else {
            inMemBufferLength = len;
        }
        if (0 != inMemBufferLength) {
            System.arraycopy(b, off, this.memBuffer, this.memBufferCount, inMemBufferLength);
            this.memBufferCount += inMemBufferLength;
        }
        if (inMemBufferLength != len) {
            final int diskBufferLength = len - inMemBufferLength;
            final int diskBufferOffset = off + inMemBufferLength;
            this.tmpDiskBufferFOS.write(b, diskBufferOffset, diskBufferLength);
        }
        this.count += len;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (null != this.tmpDiskBufferFOS) {
            try {
                this.tmpDiskBufferFOS.close();
            } catch (IOException e) {
                ;
            }
            if (null != this.tmpDiskBuffer) {
                this.tmpDiskBuffer.delete();
            }
        }
    }

    /**
	 * <p style="font-weight: bold;">Notes</p>
	 * <ul>
	 * 	<li>The first dimension will always have a size of 1.  This is for future expansion of larger support possibly
	 * 		up to {@link Long#MAX_VALUE} bytes.
	 * 	</li>
	 * 	<li>The first byte is at [0][0][0]</li>
	 * 	<li>If count were {@link #MAXIMUM_SIZE} then the last byte would be at index
	 * 		[0][(Math.ceil({@value #MAXIMUM_SIZE} / {@value Integer#MAX_VALUE})) - 1][({@value #MAXIMUM_SIZE} % {@value Integer#MAX_VALUE}) - 1]
	 * 	</li>
	 * </ul>
	 * 
	 * @return Three dimensional array with all the bytes from all buffers.  Returned array is a copy and not an
	 * 			internal reference
	 * @throws IOException If the on disk buffer temporary file could not be read
	 */
    public byte[][][] toByteMultiArray() throws IOException {
        final int firstDimSize = 1;
        final int secondDimSize = (int) Math.ceil(this.count / (double) Integer.MAX_VALUE);
        final int lastThirdDimSize = (int) Math.ceil(this.count % Integer.MAX_VALUE);
        final byte[][][] allBytes = new byte[firstDimSize][secondDimSize][];
        for (int fst = 0; fst < firstDimSize; fst++) {
            for (int snd = 0; snd < secondDimSize; snd++) {
                final int offset = fst * snd;
                if (fst == (firstDimSize - 1) && (snd == secondDimSize - 1)) {
                    allBytes[fst][snd] = getBytes(offset, lastThirdDimSize);
                } else {
                    allBytes[fst][snd] = getBytes(offset, Integer.MAX_VALUE);
                }
            }
        }
        return allBytes;
    }

    /**
	 * @param outputStream Where to write the bytes to.  Never closed.
	 * @throws IOException If any errors occur reading/writing data
	 */
    public void writeTo(final OutputStream outputStream) throws IOException {
        byte[] readBuffer;
        long offset = 0;
        while (offset < this.count) {
            readBuffer = getBytes(offset, BLOCK_BUFFER_SIZE);
            outputStream.write(readBuffer);
            offset += readBuffer.length;
        }
    }

    /**
	 * @param offset
	 * @param length Number of bytes to retrieve (if greater than remaining is set to number remaining from offset)
	 * @return Byte array with specified data from either the in-memory or disk buffer or both.  Length is always
	 * 			the actual amount of data read and may be less than the length passed in
	 * @throws IOException If any errors occur reading the data from disk
	 * @throws IllegalArgumentException If offset exceeds the size of the data 
	 */
    private byte[] getBytes(final long offset, int length) throws IOException {
        if (offset > this.count) {
            throw new IllegalArgumentException("Offset of " + offset + " is beyond size of " + this.count);
        } else if (length > this.count - offset) {
            length = (int) (this.count - offset);
        }
        final byte[] combinedBytes = new byte[length];
        int bytesCopiedFromMemory = 0;
        if (offset < this.memBufferCount) {
            int lengthInMemory;
            if ((offset + length) > this.memBufferCount) {
                lengthInMemory = (int) (this.memBufferCount - offset);
            } else {
                lengthInMemory = length;
            }
            System.arraycopy(this.memBuffer, (int) offset, combinedBytes, 0, lengthInMemory);
            bytesCopiedFromMemory = lengthInMemory;
        }
        if (offset > this.memBufferCount || (offset + length) > this.memBufferCount) {
            final long offsetOnDisk;
            if (offset > this.memBufferCount) {
                offsetOnDisk = offset - this.memBufferCount;
            } else if ((offset + length) > this.memBufferCount) {
                offsetOnDisk = 0;
            } else {
                throw new RuntimeException("unexpected behavior with offset < this.memBufferCount");
            }
            final int lengthOnDisk = length - bytesCopiedFromMemory;
            final FileInputStream fis;
            try {
                fis = new FileInputStream(this.tmpDiskBuffer);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Temporary file with buffer data is missing!", e);
            }
            fis.skip(offsetOnDisk);
            fis.read(combinedBytes, bytesCopiedFromMemory, lengthOnDisk);
            fis.close();
        }
        return combinedBytes;
    }

    public long size() {
        return this.count;
    }
}

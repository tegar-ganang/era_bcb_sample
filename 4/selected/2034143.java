package org.torweg.pulse.util.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.torweg.pulse.service.PulseException;

/**
 * an extended {@code FastByteArrayOutputStream} that will turn into a disk
 * based buffer once its overflowSize (see: {@link #setOverflowSize(int)}) has
 * been reached.
 * 
 * @author Thomas Weber
 * @version $Revision$
 */
public class BufferedDiskOverflowOutputStream extends FastByteArrayOutputStream implements Serializable {

    /**
	 * serialVersionUID.
	 */
    private static final long serialVersionUID = 7142171412783521235L;

    /**
	 * the logger for {@code BufferedDiskOverflowOutputStream}.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferedDiskOverflowOutputStream.class);

    /**
	 * the default overflow size (500kB).
	 */
    private transient int overflowSize = 500 * MemoryUnit.KB.size();

    /**
	 * the tempFile.
	 */
    private transient File tempFile = null;

    /**
	 * the tempFile output stream.
	 */
    private transient OutputStream tempFileOut = null;

    /**
	 * flag indicating whether the tempFile output stream has been closed.
	 */
    private transient boolean overflowStreamClosed = false;

    /**
	 * the number of bytes buffered.
	 */
    private transient int bytesBuffered = 0;

    /**
	 * serialize this {@code BufferedDiskOverflowOutputStream} instance.
	 * 
	 * @serialData The overflowSize as an {@code int}, followed by the size of
	 *             the buffer as an {@code int}, followed by the buffered
	 *             {@code bytes}
	 * @param s
	 *            the object output stream
	 * @throws IOException
	 *             on errors writing the serialized form
	 */
    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        s.writeInt(overflowSize);
        s.writeInt(this.bytesBuffered);
        InputStream in = null;
        if (isOverflowToDisk()) {
            close();
            in = new FileInputStream(this.tempFile);
        } else {
            in = new ByteArrayInputStream(getByteArray());
        }
        for (int i = 0; i < this.bytesBuffered; i++) {
            s.write(in.read());
        }
        in.close();
    }

    /**
	 * deserialize this {@code BufferedDiskOverflowOutputStream} instance.
	 * 
	 * @param s
	 *            the object input stream
	 * @throws IOException
	 *             on errors reading the serialized form
	 * @throws ClassNotFoundException
	 *             on errors reading the serialized form
	 */
    private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        this.overflowSize = s.readInt();
        int bufferSize = s.readInt();
        for (int i = 0; i < bufferSize; i++) {
            write(s.read());
        }
    }

    /**
	 * creates a new {@code BufferedDiskOverflowOutputStream}. The buffer
	 * capacity is initially 1024 bytes, though its size increases if necessary.
	 * The default overflow size of 500kB will be used.
	 */
    public BufferedDiskOverflowOutputStream() {
        super();
    }

    /**
	 * creates a new {@code BufferedDiskOverflowOutputStream} writing through to
	 * the given {@code OutputStream}. The buffer capacity is initially 1024
	 * bytes, though its size increases if necessary. The default overflow size
	 * of 500kB will be used.
	 * 
	 * @param size
	 *            the initial size
	 * @param target
	 *            the output stream to write through
	 * @throws IllegalArgumentException
	 *             if size is negative
	 */
    public BufferedDiskOverflowOutputStream(final int size, final OutputStream target) {
        super(size, target);
    }

    /**
	 * creates a new {@code BufferedDiskOverflowOutputStream}, with a buffer
	 * capacity of the specified size, in bytes. The default overflow size of
	 * 500kB will be used.
	 * 
	 * @param size
	 *            the initial size
	 * @throws IllegalArgumentException
	 *             if size is negative
	 */
    public BufferedDiskOverflowOutputStream(final int size) {
        super(size);
    }

    /**
	 * creates a new byte array output stream, with a buffer capacity of the
	 * specified size, in bytes writing through to the given
	 * {@code OutputStream}. The default overflow size of 500kB will be used.
	 * 
	 * @param target
	 *            the output stream to write through
	 */
    public BufferedDiskOverflowOutputStream(final OutputStream target) {
        super(target);
    }

    /**
	 * 
	 * @param nextChunkLength
	 *            the length of the next chunk to be written
	 * @throws OverflowException
	 *             on errors creating the disk overflow
	 */
    private void checkOverflow(final int nextChunkLength) throws OverflowException {
        if (!isOverflowToDisk() && size() + nextChunkLength > this.overflowSize) {
            try {
                this.tempFile = File.createTempFile(getClass().getSimpleName(), ".dat");
            } catch (IOException e) {
                throw new OverflowException("Error creating temporary file: " + e.getMessage(), e);
            }
            try {
                this.tempFileOut = new FileOutputStream(this.tempFile);
            } catch (FileNotFoundException e) {
                throw new OverflowException("Error opening stream on overflow file: " + e.getMessage(), e);
            }
            LOGGER.debug("Starting disk overflow to {}", this.tempFile.getAbsolutePath());
            try {
                super.writeTo(this.tempFileOut);
            } catch (IOException e) {
                throw new OverflowException("Error flushing buffer to disk: " + e.getMessage(), e);
            }
            super.resetBuffer();
        }
    }

    /**
	 * returns the size in bytes when the buffer shall be turned to a disk based
	 * buffer.
	 * 
	 * @return the overflow size
	 */
    public final int getOverflowSize() {
        return this.overflowSize;
    }

    /**
	 * sets the size in bytes when the buffer shall be turned to a disk based
	 * buffer.
	 * 
	 * @param ovrflwSz
	 *            the overflow size to set
	 */
    public final void setOverflowSize(final int ovrflwSz) {
        this.overflowSize = ovrflwSz;
    }

    /**
	 * delegates the call to {@link #clearOverflow()}.
	 */
    @Override
    public void resetBuffer() {
        clearOverflow();
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void write(final int b) throws OverflowException {
        int nextCount = this.bytesBuffered + 1;
        checkOverflow(1);
        if (!isOverflowToDisk()) {
            super.write(b);
            this.bytesBuffered = nextCount;
        } else {
            try {
                this.tempFileOut.write(b);
                this.bytesBuffered = nextCount;
            } catch (IOException e) {
                throw new OverflowException("Error flushing buffer to disk: " + e.getMessage(), e);
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void write(final byte[] b, final int off, final int len) {
        int nextCount = this.bytesBuffered + len;
        checkOverflow(len);
        if (!isOverflowToDisk()) {
            super.write(b, off, len);
            this.bytesBuffered = nextCount;
        } else {
            try {
                this.tempFileOut.write(b, off, len);
                this.bytesBuffered = nextCount;
            } catch (IOException e) {
                throw new OverflowException("Error flushing buffer to disk: " + e.getMessage(), e);
            }
        }
    }

    /**
	 * returns the currently buffered bytes as a {@code byte[]}. {@inheritDoc}
	 */
    @Override
    public byte[] getByteArray() {
        if (isOverflowToDisk()) {
            FastByteArrayOutputStream sink = new FastByteArrayOutputStream();
            try {
                this.close();
                InputStream i = new FileInputStream(this.tempFile);
                byte[] byteBuffer = new byte[4096];
                int n = 0;
                while (-1 != (n = i.read(byteBuffer))) {
                    sink.write(byteBuffer, 0, n);
                }
                i.close();
                sink.close();
                this.tempFileOut = new FileOutputStream(this.tempFile, true);
            } catch (Exception e) {
                throw new OverflowException("Error retrieving byte buffer: " + e.getMessage(), e);
            }
            return sink.getByteArray();
        } else {
            return super.getByteArray();
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public int size() {
        return this.bytesBuffered;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void flush() throws OverflowException {
        super.flush();
        if (isOverflowToDisk()) {
            try {
                this.tempFileOut.flush();
            } catch (IOException e) {
                throw new OverflowException("Error flushing buffer to disk: " + e.getMessage(), e);
            }
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public void close() {
        super.close();
        if (isOverflowToDisk() && !this.overflowStreamClosed) {
            try {
                this.tempFileOut.flush();
                this.tempFileOut.close();
                this.overflowStreamClosed = true;
            } catch (IOException e) {
                throw new OverflowException("Error closing disk buffer: " + e.getMessage(), e);
            }
        }
    }

    /**
	 * returns whether disk overflow exists.
	 * 
	 * @return whether disk overflow exists
	 */
    public final boolean isOverflowToDisk() {
        return (this.tempFileOut != null);
    }

    /**
	 * {@inheritDoc}
	 * <p>
	 * Moreover disk overflow resources are deleted.
	 * </p>
	 */
    public final void clearOverflow() {
        super.resetBuffer();
        if (this.tempFileOut != null) {
            if (!this.overflowStreamClosed) {
                try {
                    this.tempFileOut.close();
                } catch (IOException e) {
                    LOGGER.warn("Could not close disk buffer: " + e.getLocalizedMessage(), e);
                }
            }
            String fileName = this.tempFile.getAbsolutePath();
            if (!this.tempFile.delete()) {
                this.tempFile.deleteOnExit();
            }
            this.tempFile = null;
            this.tempFileOut = null;
            LOGGER.debug("Cleaned disk overflow {}", fileName);
        }
        this.bytesBuffered = 0;
    }

    /**
	 * is thrown on errors producing the disk overflow.
	 */
    public static class OverflowException extends PulseException {

        /**
		 * serialVersionUID.
		 */
        private static final long serialVersionUID = -8730733295304130789L;

        /**
		 * creates a new {@link OverflowException}.
		 * 
		 * @param msg
		 *            the message
		 * @param cause
		 *            the cause
		 */
        public OverflowException(final String msg, final Throwable cause) {
            super(msg, cause);
        }
    }
}

package org.apache.axiom.attachments.impl;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import org.apache.axiom.attachments.SizeAwareDataSource;
import org.apache.axiom.attachments.utils.BAAOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Attachment processing uses a lot of buffers.
 * The BufferUtils class attempts to reuse buffers to prevent 
 * excessive GarbageCollection
 */
public class BufferUtils {

    private static Log log = LogFactory.getLog(BufferUtils.class);

    public static final int BUFFER_LEN = 4 * 1024;

    static boolean ENABLE_FILE_CHANNEL = true;

    static boolean ENABLE_BAAOS_OPT = true;

    private static byte[] _cacheBuffer = new byte[BUFFER_LEN];

    private static boolean _cacheBufferInUse = false;

    private static ByteBuffer _cacheByteBuffer = ByteBuffer.allocate(BUFFER_LEN);

    private static boolean _cacheByteBufferInUse = false;

    /**
     * Private utility to write the InputStream contents to the OutputStream.
     * @param is
     * @param os
     * @throws IOException
     */
    public static void inputStream2OutputStream(InputStream is, OutputStream os) throws IOException {
        if (ENABLE_FILE_CHANNEL && os instanceof FileOutputStream) {
            if (inputStream2FileOutputStream(is, (FileOutputStream) os)) {
                return;
            }
        }
        if (ENABLE_BAAOS_OPT && os instanceof BAAOutputStream) {
            inputStream2BAAOutputStream(is, (BAAOutputStream) os, Long.MAX_VALUE);
            return;
        }
        byte[] buffer = getTempBuffer();
        try {
            int bytesRead = is.read(buffer);
            while (bytesRead > 0 || is.available() > 0) {
                if (bytesRead > 0) {
                    os.write(buffer, 0, bytesRead);
                }
                bytesRead = is.read(buffer);
            }
        } finally {
            releaseTempBuffer(buffer);
        }
    }

    /**
     * @param is InputStream
     * @param os OutputStream
     * @param limit maximum number of bytes to read
     * @return total bytes read
     * @throws IOException
     */
    public static int inputStream2OutputStream(InputStream is, OutputStream os, int limit) throws IOException {
        if (ENABLE_BAAOS_OPT && os instanceof BAAOutputStream) {
            return (int) inputStream2BAAOutputStream(is, (BAAOutputStream) os, (long) limit);
        }
        byte[] buffer = getTempBuffer();
        int totalWritten = 0;
        int bytesRead = 0;
        try {
            do {
                int len = (limit - totalWritten) > BUFFER_LEN ? BUFFER_LEN : (limit - totalWritten);
                bytesRead = is.read(buffer, 0, len);
                if (bytesRead > 0) {
                    os.write(buffer, 0, bytesRead);
                    if (bytesRead > 0) {
                        totalWritten += bytesRead;
                    }
                }
            } while (totalWritten < limit && (bytesRead > 0 || is.available() > 0));
            return totalWritten;
        } finally {
            releaseTempBuffer(buffer);
        }
    }

    /**
     * Opimized writing to FileOutputStream using a channel
     * @param is
     * @param fos
     * @return false if lock was not aquired
     * @throws IOException
     */
    public static boolean inputStream2FileOutputStream(InputStream is, FileOutputStream fos) throws IOException {
        FileChannel channel = null;
        FileLock lock = null;
        ByteBuffer bb = null;
        try {
            channel = fos.getChannel();
            if (channel != null) {
                lock = channel.tryLock();
            }
            bb = getTempByteBuffer();
        } catch (Throwable t) {
        }
        if (lock == null || bb == null || !bb.hasArray()) {
            releaseTempByteBuffer(bb);
            return false;
        }
        try {
            int bytesRead = is.read(bb.array());
            while (bytesRead > 0 || is.available() > 0) {
                if (bytesRead > 0) {
                    int written = 0;
                    if (bytesRead < BUFFER_LEN) {
                        ByteBuffer temp = ByteBuffer.allocate(bytesRead);
                        temp.put(bb.array(), 0, bytesRead);
                        temp.position(0);
                        written = channel.write(temp);
                    } else {
                        bb.position(0);
                        written = channel.write(bb);
                        bb.clear();
                    }
                }
                bytesRead = is.read(bb.array());
            }
        } finally {
            lock.release();
            releaseTempByteBuffer(bb);
        }
        return true;
    }

    /** 
     * inputStream2BAAOutputStream
     * @param is
     * @param baaos
     * @param limit
     * @return
     */
    public static long inputStream2BAAOutputStream(InputStream is, BAAOutputStream baaos, long limit) throws IOException {
        return baaos.receive(is, limit);
    }

    /**
     * Exception used by SizeLimitedOutputStream if the size limit has been exceeded.
     */
    private static class SizeLimitExceededException extends IOException {

        private static final long serialVersionUID = -6644887187061182165L;
    }

    /**
     * An output stream that counts the number of bytes written to it and throws an
     * exception when the size exceeds a given limit.
     */
    private static class SizeLimitedOutputStream extends OutputStream {

        private final int maxSize;

        private int size;

        public SizeLimitedOutputStream(int maxSize) {
            this.maxSize = maxSize;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            size += len;
            checkSize();
        }

        public void write(byte[] b) throws IOException {
            size += b.length;
            checkSize();
        }

        public void write(int b) throws IOException {
            size++;
            checkSize();
        }

        private void checkSize() throws SizeLimitExceededException {
            if (size > maxSize) {
                throw new SizeLimitExceededException();
            }
        }
    }

    /**
     * The method checks to see if attachment is eligble for optimization.
     * An attachment is eligible for optimization if and only if the size of 
     * the attachment is greated then the optimzation threshold size limit. 
     * if the Content represented by DataHandler has size less than the 
     * optimize threshold size, the attachment will not be eligible for 
     * optimization, instead it will be inlined.
     * returns 1 if DataHandler data is bigger than limit.
     * returns 0 if DataHandler data is smaller.
     * return -1 if an error occurs or unsupported.
     * @param in
     * @return
     * @throws IOException
     */
    public static int doesDataHandlerExceedLimit(DataHandler dh, int limit) {
        if (limit == 0) {
            return -1;
        }
        DataSource ds = dh.getDataSource();
        if (ds instanceof SizeAwareDataSource) {
            return ((SizeAwareDataSource) ds).getSize() > limit ? 1 : 0;
        } else if (ds instanceof javax.mail.util.ByteArrayDataSource) {
            try {
                return ((ByteArrayInputStream) ds.getInputStream()).available() > limit ? 1 : 0;
            } catch (IOException ex) {
                return -1;
            }
        } else if (ds instanceof FileDataSource) {
            return ((FileDataSource) ds).getFile().length() > limit ? 1 : 0;
        } else {
            try {
                dh.writeTo(new SizeLimitedOutputStream(limit));
            } catch (SizeLimitExceededException ex) {
                return 1;
            } catch (IOException ex) {
                log.warn(ex.getMessage());
                return -1;
            }
            return 0;
        }
    }

    private static synchronized byte[] getTempBuffer() {
        synchronized (_cacheBuffer) {
            if (!_cacheBufferInUse) {
                _cacheBufferInUse = true;
                return _cacheBuffer;
            }
        }
        return new byte[BUFFER_LEN];
    }

    private static void releaseTempBuffer(byte[] buffer) {
        synchronized (_cacheBuffer) {
            if (buffer == _cacheBuffer) {
                _cacheBufferInUse = false;
            }
        }
    }

    private static synchronized ByteBuffer getTempByteBuffer() {
        synchronized (_cacheByteBuffer) {
            if (!_cacheByteBufferInUse) {
                _cacheByteBufferInUse = true;
                return _cacheByteBuffer;
            }
        }
        return ByteBuffer.allocate(BUFFER_LEN);
    }

    private static void releaseTempByteBuffer(ByteBuffer buffer) {
        synchronized (_cacheByteBuffer) {
            if (buffer == _cacheByteBuffer) {
                _cacheByteBufferInUse = false;
            }
        }
    }
}

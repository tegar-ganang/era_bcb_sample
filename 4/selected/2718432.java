package com.nonesole.persistence.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import com.nonesole.persistence.exception.OperationsException;

/**
 * File wrapper
 * @author JACK LEE
 * @version 1.0 - build in 2009-07-22
 */
public class FileWrapper {

    private FileChannel fc = null;

    private MappedByteBuffer out = null;

    private FileLock fl = null;

    private int start = 0;

    private StringBuilder line = null;

    private ByteBuffer cacheBuffer = null;

    private static final int CACHE_SIZE = 1024;

    private CharBuffer cb = null;

    private String charsetType = AuailableCharsets.UTF_8;

    /**
	 * Set current charset<br>
	 * Please refer to com.nonesole.persistence.file.AuailableCharsets
	 * @param charsetType the charsetType to set
	 */
    public void setCharset(String charset) {
        this.charsetType = charset;
    }

    /**
	 * Construction
	 * @param url - URL
	 * */
    public FileWrapper(URL url) throws OperationsException, IOException {
        if (null == url) throw new OperationsException("URL is null");
        if (null == url.toString()) throw new OperationsException("File path is null");
        fc = new RandomAccessFile(url.getFile(), "rw").getChannel();
        cacheBuffer = ByteBuffer.allocate(CACHE_SIZE);
    }

    /**
	 * Clear and reset parameter.
	 * @throws IOException 
	 * */
    public void clear() throws IOException {
        this.start = 0;
        if (null != fc) fc.position(0);
    }

    /**
	 * If or not have next line to reading.
	 * @return true - readLine() can be used
	 * */
    public boolean hasMore() throws IOException {
        if (start < fc.size()) return true; else return false;
    }

    /**
	 * Read file to string.
	 * @return String 
	 * */
    public String readAll() throws IOException {
        return readAll(true);
    }

    /**
	 * Read file to string.
	 * @param isLock - true means lock file channel when read file
	 * @return String 
	 * */
    public String readAll(boolean isLock) throws IOException {
        if (isLock) lock();
        int blocks = (int) (fc.size() / CACHE_SIZE);
        int tails = (int) (fc.size() % CACHE_SIZE);
        line = new StringBuilder();
        byte[] b = null;
        fc.position(0);
        for (int i = 0; i < blocks; i++) {
            cacheBuffer.clear();
            fc.read(cacheBuffer);
            cacheBuffer.rewind();
            cb = Charset.forName(charsetType).decode(cacheBuffer);
            cb.rewind();
            while (cb.hasRemaining()) {
                line.append(cb.get());
            }
        }
        if (tails > 0) {
            cacheBuffer.clear();
            fc.read(cacheBuffer);
            b = new byte[tails];
            cacheBuffer.rewind();
            cacheBuffer.get(b, 0, tails);
            line.append(new String(b, this.charsetType));
        }
        if (isLock) release();
        return line.toString();
    }

    /**
	 * Read one line of string<br>
	 * @return one line of string
	 * */
    public String readLine() throws IOException {
        line = new StringBuilder();
        byte b;
        int offset = 0;
        byte[] dst;
        readByte();
        while (true) {
            if (cacheBuffer.hasRemaining()) {
                b = cacheBuffer.get();
                start++;
                offset++;
                if ('\n' == b || '\r' == b) {
                    dst = new byte[offset];
                    cacheBuffer.rewind();
                    cacheBuffer.get(dst);
                    return line.append(new String(dst, this.charsetType)).toString();
                }
            } else {
                dst = new byte[CACHE_SIZE];
                line.append(new String(dst, this.charsetType));
                readByte();
                offset = 0;
            }
        }
    }

    /**
	 * Read byte
	 * */
    private void readByte() throws IOException {
        if (null == fc) throw new IOException("FileChannel is null.");
        cacheBuffer.clear();
        fc.read(cacheBuffer, start);
        cacheBuffer.rewind();
    }

    /**
	 * Write content at tail of file
	 * @param str
	 * */
    public void write(String str) throws OperationsException, IOException {
        if (null == str || "".equals(str)) throw new OperationsException("Can not write null.");
        lock();
        long start = fc.size();
        long size = str.getBytes().length;
        write(start, size, str.getBytes());
        release();
    }

    /**
	 * Replace what user want to erase or rewrite. 
	 * @param regex 
	 * @param replacement 
	 * */
    public void replace(String regex, String replacement) throws IOException, OperationsException {
        if (null == regex || "".equals(regex) || null == replacement) throw new OperationsException("String is null");
        lock();
        String result = readAll(false).replaceAll(regex, replacement);
        byte[] b = result.getBytes();
        int size = b.length;
        fc.truncate(size);
        write(0, size, b);
        release();
    }

    /**
	 * Write content at where user want to insert.
	 * @param offset - long type
	 * @param length 
	 * @param b - byte array
	 * @throws IOException 
	 * */
    private void write(long offset, long length, byte[] b) throws IOException {
        out = fc.map(FileChannel.MapMode.READ_WRITE, offset, length);
        out.put(b);
        out.force();
        out = null;
    }

    /**
	 * Release file channel.
	 * */
    public void release() throws IOException {
        if (null != fl) {
            fl.release();
            fl = null;
        }
    }

    /**
	 * Destroy current object
	 * */
    public void destroy() throws IOException {
        release();
        out = null;
        fc.close();
        fc = null;
        System.gc();
    }

    /**
	 * Lock file channel
	 * */
    public void lock() throws IOException {
        if (null == fl) fl = fc.tryLock();
    }
}

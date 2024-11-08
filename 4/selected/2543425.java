package com.google.gwt.dev.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * A nifty class that lets you squirrel away data on the file system. Write
 * once, read many times. Instance of this are thread-safe by way of internal
 * synchronization.
 * 
 * Note that in the current implementation, the backing temp file will get
 * arbitrarily large as you continue adding things to it. There is no internal
 * GC or compaction.
 */
public class DiskCache {

    private boolean atEnd = true;

    private RandomAccessFile file;

    public DiskCache() {
        try {
            File temp = File.createTempFile("gwt", "byte-cache");
            temp.deleteOnExit();
            file = new RandomAccessFile(temp, "rw");
            file.setLength(0);
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize byte cache", e);
        }
    }

    /**
   * Read some bytes off disk.
   * 
   * @param token a handle previously returned from
   *          {@link #writeByteArray(byte[])}
   * @return the bytes that were written
   */
    public synchronized byte[] readByteArray(long token) {
        try {
            atEnd = false;
            file.seek(token);
            int length = file.readInt();
            byte[] result = new byte[length];
            file.readFully(result);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read from byte cache", e);
        }
    }

    public <T> T readObject(long token, Class<T> type) {
        try {
            byte[] bytes = readByteArray(token);
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            return Util.readStreamAsObject(in, type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unexpected exception deserializing from disk cache", e);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected exception deserializing from disk cache", e);
        }
    }

    /**
   * Read a String from disk.
   * 
   * @param token a handle previously returned from {@link #writeString(String)}
   * @return the String that was written
   */
    public String readString(long token) {
        return Util.toString(readByteArray(token));
    }

    /**
   * Write the rest of the data in an input stream to disk.
   * 
   * @return a handle to retrieve it later
   */
    public synchronized long transferFromStream(InputStream in) {
        byte[] buf = Util.takeThreadLocalBuf();
        try {
            long position = moveToEndPosition();
            file.writeInt(-1);
            int length = 0;
            int bytesRead;
            while ((bytesRead = in.read(buf)) != -1) {
                file.write(buf, 0, bytesRead);
                length += bytesRead;
            }
            file.seek(position);
            file.writeInt(length);
            atEnd = false;
            return position;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read from byte cache", e);
        } finally {
            Util.releaseThreadLocalBuf(buf);
        }
    }

    /**
   * Reads bytes of data back from disk and writes them into the specified
   * output stream.
   */
    public synchronized void transferToStream(long token, OutputStream out) {
        byte[] buf = Util.takeThreadLocalBuf();
        try {
            atEnd = false;
            file.seek(token);
            int length = file.readInt();
            int bufLen = buf.length;
            while (length > bufLen) {
                int read = file.read(buf, 0, bufLen);
                length -= read;
                out.write(buf, 0, read);
            }
            while (length > 0) {
                int read = file.read(buf, 0, length);
                length -= read;
                out.write(buf, 0, read);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read from byte cache", e);
        } finally {
            Util.releaseThreadLocalBuf(buf);
        }
    }

    /**
   * Write a byte array to disk.
   * 
   * @return a handle to retrieve it later
   */
    public synchronized long writeByteArray(byte[] bytes) {
        try {
            long position = moveToEndPosition();
            file.writeInt(bytes.length);
            file.write(bytes);
            return position;
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to byte cache", e);
        }
    }

    public long writeObject(Object object) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Util.writeObjectToStream(out, object);
            return writeByteArray(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException on in-memory stream", e);
        }
    }

    /**
   * Write a String to disk.
   * 
   * @return a handle to retrieve it later
   */
    public long writeString(String str) {
        return writeByteArray(Util.getBytes(str));
    }

    @Override
    protected synchronized void finalize() throws Throwable {
        close();
    }

    private void close() throws Throwable {
        if (file != null) {
            file.setLength(0);
            file.close();
            file = null;
        }
    }

    /**
   * Moves to the end of the file if necessary and returns the offset position.
   * Caller must synchronize.
   * 
   * @return the offset position of the end of the file
   * @throws IOException
   */
    private long moveToEndPosition() throws IOException {
        if (atEnd) {
            return file.getFilePointer();
        } else {
            long position = file.length();
            file.seek(position);
            atEnd = true;
            return position;
        }
    }
}

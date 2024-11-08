package com.kni.etl.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import com.kni.etl.dbutils.ResourcePool;

/**
 * The Class Store.
 */
public final class Store {

    /** The records held. */
    private long mRecordsHeld = 0;

    /** The file channel. */
    private FileChannel mFileChannel = null;

    /** The input stream. */
    private FileInputStream mInputStream;

    /** The output stream. */
    private FileOutputStream mOutputStream;

    /** The temp file name. */
    private String mTempFileName;

    /** The byte output stream. */
    private OutputStream mByteOutputStream;

    /** The object output stream. */
    private ObjectOutputStream mObjectOutputStream;

    /** The byte input stream. */
    private InputStream mByteInputStream;

    /** The object input stream. */
    private ObjectInputStream mObjectInputStream;

    private BufferedOutputStream mBufferedOutputStream;

    private BufferedInputStream mBufferedInputStream;

    /**
     * Instantiates a new store.
     * 
     * @param pWriteBufferSize the write buffer size
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public Store(int pWriteBufferSize) throws IOException {
        File fd = File.createTempFile("KETL.", ".spool");
        this.mTempFileName = fd.getAbsolutePath();
        this.mOutputStream = new FileOutputStream(fd);
        this.mFileChannel = this.mOutputStream.getChannel();
        this.mByteOutputStream = java.nio.channels.Channels.newOutputStream(this.mFileChannel);
        this.mBufferedOutputStream = new BufferedOutputStream(this.mByteOutputStream, pWriteBufferSize);
        this.mObjectOutputStream = new ObjectOutputStream(this.mBufferedOutputStream);
    }

    /**
     * Add.
     * 
     * @param pObject the object
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public final void add(Object pObject) throws IOException {
        this.mObjectOutputStream.writeObject(pObject);
        this.mRecordsHeld++;
    }

    /**
     * Add.
     * 
     * @param pObject the object
     * @param len the len
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public final void add(Object[] pObject, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            this.mObjectOutputStream.writeObject(pObject[i]);
            this.mRecordsHeld++;
        }
    }

    /**
     * Close.
     */
    public final void close() {
        this.writeClose();
        this.readClose();
        File fd = new File(this.mTempFileName);
        if (fd.exists()) {
            if (fd.delete() == false) {
                ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.INFO_MESSAGE, "Problems deleting " + fd.getAbsolutePath());
            }
        }
    }

    /**
     * Write close.
     */
    private final void writeClose() {
        try {
            this.mObjectOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.mBufferedOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.mByteOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.mFileChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.mOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read close.
     */
    private final void readClose() {
        try {
            this.mObjectInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.mBufferedInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.mByteInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.mFileChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            this.mInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Commit.
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public final void commit() throws IOException {
        this.mObjectOutputStream.flush();
        this.mByteOutputStream.flush();
        this.writeClose();
    }

    @Override
    protected final void finalize() throws Throwable {
        this.commit();
        this.close();
        super.finalize();
    }

    /**
     * Checks for next.
     * 
     * @return true, if successful
     */
    public final boolean hasNext() {
        return (this.mRecordsHeld == 0) ? false : true;
    }

    /**
     * Next.
     * 
     * @return the object
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws ClassNotFoundException the class not found exception
     */
    public final Object next() throws IOException, ClassNotFoundException {
        this.mRecordsHeld--;
        return this.mObjectInputStream.readObject();
    }

    /**
     * Start.
     * 
     * @param readBufferSize the read buffer size
     * @param objectBufferSize the object buffer size
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public final void start(int readBufferSize, int objectBufferSize) throws IOException {
        this.mInputStream = new FileInputStream(this.mTempFileName);
        this.mFileChannel = this.mInputStream.getChannel();
        this.mByteInputStream = java.nio.channels.Channels.newInputStream(this.mFileChannel);
        this.mBufferedInputStream = new BufferedInputStream(this.mByteInputStream, readBufferSize);
        this.mObjectInputStream = new ObjectInputStream(this.mBufferedInputStream);
    }
}

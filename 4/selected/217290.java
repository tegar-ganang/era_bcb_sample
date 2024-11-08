package org.szegedi.nbpipe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

public class GlobalByteBufferPool implements ByteBufferPool {

    private static final int MEMORY_BLOCKSIZE = 4096;

    private static final int FILE_BLOCKSIZE = 65536;

    private final LinkedList memoryBuffers = new LinkedList();

    private final LinkedList fileBuffers = new LinkedList();

    private boolean blocking = false;

    public GlobalByteBufferPool(int memorySize, int fileSize, File file) throws IOException {
        if (memorySize > 0) allocateDirect(memorySize);
        if (fileSize > 0) allocateFile(fileSize, file);
    }

    public synchronized void setBlockingWhenExhausted(boolean blocking) {
        this.blocking = blocking;
    }

    public synchronized boolean isBlockingWhenExhausted() {
        return blocking;
    }

    public ByteBuffer getMemoryBuffer() {
        return getBuffer(memoryBuffers, fileBuffers);
    }

    public ByteBuffer getFileBuffer() {
        return getBuffer(fileBuffers, memoryBuffers);
    }

    private ByteBuffer getBuffer(LinkedList primaryList, LinkedList secondaryList) {
        ByteBuffer buffer = getBuffer(primaryList, false);
        if (buffer == null) {
            buffer = getBuffer(secondaryList, false);
            if (buffer == null) {
                if (blocking) buffer = getBuffer(primaryList, true); else buffer = ByteBuffer.allocate(MEMORY_BLOCKSIZE);
            }
        }
        return buffer;
    }

    private ByteBuffer getBuffer(LinkedList list, boolean wait) {
        synchronized (list) {
            if (list.isEmpty()) {
                if (wait) {
                    try {
                        list.wait();
                    } catch (InterruptedException e) {
                        return null;
                    }
                } else return null;
            }
            return (ByteBuffer) list.removeFirst();
        }
    }

    public void putBuffer(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            switch(buffer.capacity()) {
                case MEMORY_BLOCKSIZE:
                    putBuffer(buffer, memoryBuffers);
                    break;
                case FILE_BLOCKSIZE:
                    putBuffer(buffer, fileBuffers);
                    break;
            }
        }
    }

    public int getBufferType(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            switch(buffer.capacity()) {
                case MEMORY_BLOCKSIZE:
                    return TYPE_MEMORY;
                case FILE_BLOCKSIZE:
                    return TYPE_FILE;
            }
        }
        return TYPE_UNKNOWN;
    }

    private void putBuffer(ByteBuffer buffer, LinkedList list) {
        buffer.clear();
        synchronized (list) {
            list.addFirst(buffer);
            list.notify();
        }
    }

    private void allocateDirect(int size) {
        int bufCount = size / MEMORY_BLOCKSIZE;
        size = bufCount * MEMORY_BLOCKSIZE;
        ByteBuffer directBuf = ByteBuffer.allocateDirect(size);
        sliceBuffer(directBuf, MEMORY_BLOCKSIZE, memoryBuffers);
    }

    private void allocateFile(int size, File f) throws IOException {
        int bufCount = size / FILE_BLOCKSIZE;
        size = bufCount * FILE_BLOCKSIZE;
        RandomAccessFile file = new RandomAccessFile(f, "rw");
        try {
            file.setLength(size);
            ByteBuffer fileBuf = file.getChannel().map(FileChannel.MapMode.READ_WRITE, 0L, size);
            sliceBuffer(fileBuf, FILE_BLOCKSIZE, fileBuffers);
        } finally {
            file.close();
        }
    }

    private void sliceBuffer(ByteBuffer buf, int blockSize, LinkedList list) {
        int bufCount = buf.capacity() / blockSize;
        int pos = 0;
        for (int i = 0; i < bufCount; ++i) {
            int newLimit = pos + blockSize;
            buf.limit(newLimit);
            list.addLast(buf.slice());
            pos = newLimit;
            buf.position(pos);
        }
    }
}

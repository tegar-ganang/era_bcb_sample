package org.processmining.framework.log.rfb.fsio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Random;

/**
 * @author Christian W. Guenther (christian@deckfour.org)
 *
 */
public class FS2BlockProvider {

    protected static Random random = new Random();

    protected static int shadowSize = 4;

    protected static int currentShadowSize = 0;

    protected static int lastRequestIndex = 0;

    protected static MappedByteBuffer[] centralMaps = null;

    protected static FS2BlockProvider[] currentMapOwners = null;

    static {
        centralMaps = new MappedByteBuffer[shadowSize];
        currentMapOwners = new FS2BlockProvider[shadowSize];
    }

    protected static synchronized MappedByteBuffer requestMap(FS2BlockProvider requester) throws IOException {
        synchronized (FS2BlockProvider.class) {
            for (int i = 0; i < currentShadowSize; i++) {
                if (currentMapOwners[i] == requester) {
                    lastRequestIndex = i;
                    return centralMaps[i];
                }
            }
            if (currentShadowSize < shadowSize) {
                currentMapOwners[currentShadowSize] = requester;
                FileChannel channel = requester.getFile().getChannel();
                MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, requester.size());
                centralMaps[currentShadowSize] = map;
                lastRequestIndex = currentShadowSize;
                currentShadowSize++;
                return map;
            } else {
                int kickIndex = random.nextInt(shadowSize - 1);
                if (kickIndex == lastRequestIndex) {
                    kickIndex++;
                }
                centralMaps[kickIndex].force();
                currentMapOwners[kickIndex] = requester;
                FileChannel channel = requester.getFile().getChannel();
                MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, requester.size());
                centralMaps[kickIndex] = map;
                lastRequestIndex = kickIndex;
                System.gc();
                return map;
            }
        }
    }

    protected boolean mapped;

    protected RandomAccessFile file;

    protected int size;

    protected int blockSize;

    protected int numberOfBlocks;

    protected ArrayList<FS2Block> freeBlocks;

    public FS2BlockProvider(File storage, int size, int blockSize, boolean mapped) throws IOException {
        synchronized (this) {
            this.mapped = mapped;
            this.size = size;
            this.blockSize = blockSize;
            if (storage.exists() == false) {
                storage.createNewFile();
            }
            this.file = new RandomAccessFile(storage, "rw");
            numberOfBlocks = size / blockSize;
            freeBlocks = new ArrayList<FS2Block>();
            for (int i = 0; i < numberOfBlocks; i++) {
                FS2Block block = new FS2Block(this, i);
                freeBlocks.add(block);
            }
        }
    }

    public RandomAccessFile getFile() {
        return file;
    }

    public int size() {
        return size;
    }

    public int numberOfBlocks() {
        return numberOfBlocks;
    }

    public synchronized int numberOfFreeBlocks() {
        return freeBlocks.size();
    }

    public int blockSize() {
        return blockSize;
    }

    public synchronized FS2Block allocateBlock() {
        if (freeBlocks.size() > 0) {
            return freeBlocks.remove(0);
        } else {
            return null;
        }
    }

    public synchronized void freeBlock(FS2Block block) {
        freeBlocks.add(block);
    }

    public int getBlockOffset(int blockNumber) {
        return blockNumber * blockSize;
    }

    public synchronized int read(int blockNumber, int blockOffset, byte[] buffer) throws IOException {
        return read(blockNumber, blockOffset, buffer, 0, buffer.length);
    }

    public synchronized int read(int blockNumber, int blockOffset, byte[] buffer, int bufferOffset, int length) throws IOException {
        long pointer = getBlockOffset(blockNumber) + blockOffset;
        int readable = blockSize - blockOffset;
        int readLength = length;
        if (readable < length) {
            readLength = readable;
        }
        if (mapped == true) {
            MappedByteBuffer map = FS2BlockProvider.requestMap(this);
            map.position((int) pointer);
            map.get(buffer, bufferOffset, readLength);
            return readLength;
        } else {
            file.seek(pointer);
            return file.read(buffer, bufferOffset, readLength);
        }
    }

    public synchronized int read(int blockNumber, int blockOffset) throws IOException {
        long pointer = getBlockOffset(blockNumber) + blockOffset;
        if (mapped == true) {
            MappedByteBuffer map = FS2BlockProvider.requestMap(this);
            map.position((int) pointer);
            int result = map.get();
            return result + 128;
        } else {
            file.seek(pointer);
            return file.read();
        }
    }

    public synchronized void write(int blockNumber, int blockOffset, byte[] buffer) throws IOException {
        write(blockNumber, blockOffset, buffer, 0, buffer.length);
    }

    public synchronized void write(int blockNumber, int blockOffset, byte[] buffer, int bufferOffset, int length) throws IOException {
        long pointer = getBlockOffset(blockNumber) + blockOffset;
        int writable = blockSize - blockOffset;
        int writeLength = length;
        if (writable < length) {
            writeLength = writable;
        }
        if (mapped == true) {
            MappedByteBuffer map = FS2BlockProvider.requestMap(this);
            map.position((int) pointer);
            map.put(buffer, bufferOffset, writeLength);
        } else {
            file.seek(pointer);
            file.write(buffer, bufferOffset, writeLength);
        }
    }

    public synchronized void write(int blockNumber, int blockOffset, int value) throws IOException {
        long pointer = getBlockOffset(blockNumber) + blockOffset;
        if (mapped == true) {
            MappedByteBuffer map = FS2BlockProvider.requestMap(this);
            map.position((int) pointer);
            map.put((byte) (value - 128));
        } else {
            file.seek(pointer);
            file.write(value);
        }
    }
}

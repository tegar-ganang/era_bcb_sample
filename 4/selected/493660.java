package org.processmining.framework.log.rfb.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import org.processmining.framework.log.rfb.io.monitor.BlockDataStorageInfo;

/**
 * This class implements a storage container providing dynamically
 * sized blocks of bytes. Initially, each block data storage contains
 * one single block, which extends on the whole backing operating 
 * system file. With the allocation of further blocks, the current
 * blocks are partitioned symmetrically, in a recursive manner. This means,
 * on each partitioning level, each currently contained block is cut in half,
 * yielding a new block each, extending over the prior block's second half.
 * In this way, each partitioning doubles the number of blocks in the
 * block data storage while cutting each block's size in half. 
 * We assume that files are allocated or written consecutively, i.e. one
 * after another. If we further assume that files are written in large
 * numbers of the same size of file, this block data storage scales perfectly,
 * adjusting its partitioning dynamically to the size of required byte chunks.
 * The block data storage can be tuned in its behavior with two parameters. 
 * One of it being the size of the block data storage, i.e. the number of
 * bytes which is provided in a whole. The other parameter is the partitioning
 * limit. If this limit, as a percentage of each block's inhibited space from
 * the space it can maximally hold, is increased for at least one block in the
 * storage, the storage can no longer be partitioned.
 * This concept is geared towards the idea of a virtual file system, where
 * file metaphors represent abstract sequential byte storages, which can extend
 * over multiple storage blocks as provided by this class.
 * 
 * @author Christian W. Guenther (christian at deckfour dot org)
 *
 */
public class BlockDataStorage {

    /**
	 * Returns the largest integer that is a power of two
	 * and smaller than the given integer parameter, i.e.
	 * the power-of-two-floor of n.
	 * @param n 
	 * @return
	 */
    protected static int powerOfTwoFloor(int n) {
        return Integer.highestOneBit(n);
    }

    /**
	 * The operating system level file backing the storage
	 */
    protected File file = null;

    /**
	 * This buffer will hold the memory-mapped backing file
	 */
    protected MappedByteBuffer buffer = null;

    /**
	 * Size of the storage file, in bytes.
	 */
    protected int size = 0;

    /**
	 * Current offset in the storage file, from which will be
	 * read or written the next time
	 */
    protected int pointer = 0;

    /**
	 * Current size of blocks provided by this storage
	 */
    protected int blockSize = 0;

    /**
	 * The partition level in which this storage currently is in.
	 * The number of blocks is two to the power of the 
	 * current partitioning level.
	 */
    protected int partitionLevel = 0;

    /**
	 * If at least one block in the provided set exceeds a usage
	 * percentage given in this threshold, no further partitioning
	 * must occur. As value within [0.0, 0.5].
	 */
    protected double partitionThreshold = 0.375;

    /**
	 * List of the currently provided storage blocks within
	 * this storage.
	 */
    protected ArrayList<StorageBlock> blocks = null;

    /**
	 * List or provided storage blocks in this storage, which can
	 * still be allocated, i.e. are free.
	 */
    protected ArrayList<StorageBlock> freeBlocks = null;

    /**
	 * Create a new block data storage, based on the given file
	 * with the given size.
	 * @param aFile File which is to back this block storage.
	 * @param fileSize Size of the block data storage in bytes.
	 * @throws IOException
	 */
    public BlockDataStorage(File aFile, int fileSize) throws IOException {
        synchronized (this) {
            file = aFile;
            if (file.exists() == false) {
                file.createNewFile();
            }
            FileChannel rwBufferChannel = new RandomAccessFile(file, "rw").getChannel();
            buffer = rwBufferChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            size = fileSize;
            pointer = 0;
            blockSize = size;
            partitionLevel = 0;
            partitionThreshold = 0.375;
            StorageBlock initial = new StorageBlock(this, 0, size);
            blocks = new ArrayList<StorageBlock>();
            blocks.add(initial);
            freeBlocks = new ArrayList<StorageBlock>();
            freeBlocks.add(initial);
        }
    }

    /**
	 * Allocate a new block of bytes from this block
	 * data storage. If no block is found in the list of
	 * free blocks, partitioning the current set of blocks
	 * to create free blocks is attempted. If this is successful,
	 * one of the newly created blocks is returned. If not,
	 * this method will return <code>null</code>.
	 * @return
	 */
    public synchronized StorageBlock allocateBlock() {
        if (freeBlocks.size() > 0) {
            return freeBlocks.remove(0);
        } else if (partition() == true) {
            return freeBlocks.remove(0);
        } else {
            return null;
        }
    }

    /**
	 * This method registers the given indexed block as free
	 * with the storage. It must be ensured by the calling party,
	 * that the respectiv block is indeed no longer in use and
	 * is safe to be reassigned. The free block will be added to
	 * the list of free blocks.
	 * @param index Index of the block to be freed from allocation.
	 */
    public void freeBlock(int index) {
        synchronized (this) {
            if (freeBlocks.contains(blocks.get(index))) {
                System.err.println("ERROR: block freed twice!");
            }
            freeBlocks.add(blocks.get(index));
        }
        if (freeBlocks.size() == blocks.size()) {
            defragment();
        }
    }

    /**
	 * Returns the file which is backing this block data storage.
	 * @return File handle to the backing file.
	 */
    public File getFile() {
        return file;
    }

    /**
	 * This method performs a partitioning of the current set of blocks.
	 * If the set partitioning threshold is exceeded by at least one block
	 * in the set, partitioning is aborted and <code>false</code> is returned.
	 * If partitioning is successful, <code>true</code> is returned, implying
	 * adjustment of all prior contained blocks and creation of all respective
	 * new blocks.
	 * @return A success indicator flag.
	 */
    protected synchronized boolean partition() {
        for (StorageBlock block : blocks) {
            if (block.fillRatio() > partitionThreshold) {
                return false;
            }
        }
        partitionLevel++;
        if (partitionLevel == 1) {
            blockSize = size / 2;
            blocks.get(0).setMaxSize(blockSize);
            StorageBlock addedBlock = new StorageBlock(this, 1, blockSize);
            blocks.add(addedBlock);
            freeBlocks.add(addedBlock);
        } else {
            int addedBlocks = 1 << (partitionLevel - 1);
            int stepSize = size / addedBlocks;
            blockSize = stepSize >> 1;
            for (StorageBlock block : blocks) {
                block.setMaxSize(blockSize);
            }
            int totalBlocks = addedBlocks << 1;
            StorageBlock nBlock = null;
            for (int i = blocks.size(); i < totalBlocks; i++) {
                nBlock = new StorageBlock(this, i, blockSize);
                blocks.add(nBlock);
                freeBlocks.add(nBlock);
            }
        }
        return true;
    }

    /**
	 * Attempts to defragment the block data storage, i.e. to recombine
	 * previously partitioned block, so that the storage becomes less
	 * scattered.<br/>
	 * <b>Notice:</b> This initial implementation will only attempt to
	 * restore the initial state of a partitioned storage that is 
	 * completely empty. Later implementation should improve this situation!
	 * @return Whether defragmentation was successful
	 */
    public synchronized boolean defragment() {
        if (freeBlocks.size() == blocks.size()) {
            pointer = 0;
            blockSize = size;
            partitionLevel = 0;
            freeBlocks.clear();
            blocks.clear();
            StorageBlock initial = new StorageBlock(this, 0, size);
            blocks.add(initial);
            freeBlocks.add(initial);
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Returns the offset in bytes of the indexed block, measured
	 * from the beginning of the block-partitioned file.
	 * @param blockIndex Index of the block, whose offset is requested.
	 * @return The offset of the indexed block from the beginning of the
	 * file, in bytes.
	 */
    public synchronized int getBlockOffset(int blockIndex) {
        if (blockIndex == 0) {
            return 0;
        } else if (blockIndex == 1) {
            return (size / 2);
        } else {
            int blocksInPartitioningLevel = powerOfTwoFloor(blockIndex);
            int subIndex = blockIndex % blocksInPartitioningLevel;
            int stepSize = size / blocksInPartitioningLevel;
            return (stepSize / 2) + (subIndex * stepSize);
        }
    }

    /**
	 * Internal method to translate block-private file pointer addresses, given
	 * as block index and block-private offset, into global, file-wide offsets.
	 * The provided number of bytes allows the method to check, whether the
	 * calling method will transcend the given block's boundaries. In this case,
	 * an IOException will be thrown. Otherwise the method returns the requested
	 * global offset as long.
	 * @param blockIndex Index of the requesting block.
	 * @param blockOffset Offset within the requesting block.
	 * @param bytes Number of bytes to be read or written from the requested offset.
	 * @return The global offset in bytes, translated from the given parameters.
	 */
    protected synchronized int translateOffsetChecking(int blockIndex, int blockOffset, int bytes) throws IOException {
        if (blockOffset + bytes > blockSize) {
            throw new IOException("Access was requested which exceeds the requesting block's current boundaries!");
        } else {
            return (getBlockOffset(blockIndex) + blockOffset);
        }
    }

    /**
	 * Returns the current block size of the partitioned file.
	 * @return Current block size in bytes.
	 */
    public synchronized int getBlockSize() {
        return blockSize;
    }

    /**
	 * Returns information about the current usage of this block data storage's blocks
	 * @return an aggregated information container
	 */
    public BlockDataStorageInfo getInfo() {
        int[] blockFillSizes = new int[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            try {
                blockFillSizes[i] = (int) blocks.get(i).length();
            } catch (IOException e) {
                System.err.println("Exception when assembling block data storage info!");
                e.printStackTrace();
                blockFillSizes[i] = blockSize;
            }
        }
        return new BlockDataStorageInfo(file, partitionLevel, blocks.size(), blockSize, blockFillSizes);
    }

    public synchronized void close() throws IOException {
    }

    public synchronized int length() throws IOException {
        return size;
    }

    public synchronized void write(int blockNumber, int offset, int b) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 1);
        buffer.position(pointer);
        buffer.put((byte) b);
    }

    public synchronized void write(int blockNumber, int offset, byte[] b) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, b.length);
        buffer.position(pointer);
        buffer.put(b);
    }

    public synchronized void write(int blockNumber, int offset, byte[] b, int off, int len) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, len);
        buffer.position(pointer);
        buffer.put(b, off, len);
    }

    public synchronized void writeBoolean(int blockNumber, int offset, boolean v) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 1);
        buffer.position(pointer);
        byte val = v ? (byte) 1 : (byte) 0;
        buffer.put(val);
    }

    public synchronized void writeByte(int blockNumber, int offset, int v) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 1);
        buffer.position(pointer);
        buffer.put((byte) v);
    }

    public synchronized void writeBytes(int blockNumber, int offset, String s) throws IOException {
        byte[] bytes = s.getBytes();
        pointer = this.translateOffsetChecking(blockNumber, offset, bytes.length);
        buffer.position(pointer);
        buffer.put(bytes);
    }

    public synchronized void writeChar(int blockNumber, int offset, int v) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 2);
        buffer.position(pointer);
        buffer.putChar((char) v);
    }

    public synchronized void writeChars(int blockNumber, int offset, String s) throws IOException {
        char[] chars = s.toCharArray();
        pointer = this.translateOffsetChecking(blockNumber, offset, (chars.length * 2));
        for (int i = 0; i < chars.length; i++) {
            buffer.position(pointer);
            buffer.putChar(chars[i]);
            pointer += 2;
        }
    }

    public synchronized void writeDouble(int blockNumber, int offset, double v) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 8);
        buffer.position(pointer);
        buffer.putDouble(v);
    }

    public synchronized void writeFloat(int blockNumber, int offset, float v) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 4);
        buffer.position(pointer);
        buffer.putFloat(v);
    }

    public synchronized void writeInt(int blockNumber, int offset, int v) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 4);
        buffer.position(pointer);
        buffer.putInt(v);
    }

    public synchronized void writeLong(int blockNumber, int offset, long v) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 8);
        buffer.position(pointer);
        buffer.putLong(v);
    }

    public synchronized void writeShort(int blockNumber, int offset, int v) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 2);
        buffer.position(pointer);
        buffer.putShort((short) v);
    }

    public synchronized boolean readBoolean(int blockNumber, int offset) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 1);
        buffer.position(pointer);
        byte val = buffer.get();
        if (val == 0) {
            return false;
        } else {
            return true;
        }
    }

    public synchronized byte readByte(int blockNumber, int offset) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 1);
        buffer.position(pointer);
        return buffer.get();
    }

    public synchronized char readChar(int blockNumber, int offset) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 2);
        buffer.position(pointer);
        return buffer.getChar();
    }

    public synchronized double readDouble(int blockNumber, int offset) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 8);
        buffer.position(pointer);
        return buffer.getDouble();
    }

    public synchronized float readFloat(int blockNumber, int offset) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 4);
        buffer.position(pointer);
        return buffer.getFloat();
    }

    public synchronized void readFully(int blockNumber, int offset, byte[] b) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, b.length);
        buffer.position(pointer);
        buffer.get(b);
    }

    public synchronized void readFully(int blockNumber, int offset, byte[] b, int off, int len) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, len);
        buffer.position(pointer);
        buffer.get(b, off, len);
    }

    public synchronized int readInt(int blockNumber, int offset) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 4);
        buffer.position(pointer);
        return buffer.getInt();
    }

    public synchronized long readLong(int blockNumber, int offset) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 8);
        buffer.position(pointer);
        return buffer.getLong();
    }

    public synchronized short readShort(int blockNumber, int offset) throws IOException {
        pointer = this.translateOffsetChecking(blockNumber, offset, 2);
        buffer.position(pointer);
        return buffer.getShort();
    }

    public synchronized int readUnsignedByte(int blockNumber, int offset) throws IOException {
        throw new IOException("Not implemented on this level!");
    }

    public synchronized int readUnsignedShort(int blockNumber, int offset) throws IOException {
        throw new IOException("Not implemented on this level!");
    }
}

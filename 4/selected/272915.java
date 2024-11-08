package com.antlersoft.odb;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.PhantomReference;
import java.lang.ref.WeakReference;

/**
 * <p>
 * This class allocates regions within a RandomAccessFile much like
 * malloc allocates regions in memory.  The caller can request a
 * region of arbitrary size; the class will extend the file if necessary
 * to accomodate it.  A caller can free a region, making the space inside
 * available for re-allocation.  Adjacent free regions are coalesced when
 * the second is freed.
 * </p><p>
 * This allocator makes no attempt to take into account the performance
 * properties of different random access operations on different architectures.
 * </p><p>
 * When a new allocator in a new file is created, it is created with an
 * initial region.  This initial region is made available to the user as
 * getInitialRegion, but may not be freed or re-allocated.
 * </p><p>
 * The allocator is initially created with three parameters: the first
 * is the size of the initial region.  The second is the minimum client visible
 * portion of a region; as discussed below, this must be at least 4.  It
 * can be made larger to reduce fragmentation in the allocator.  The
 * final parameter is the minimum amount to be added to a file when a region
 * that is larger than any available free region is requested.  This can
 * reduce fragmentation of the file in the file system(?).
 * </p><p>
 * Default values are available for all the parameters.
 * </p><p>
 * By specifying a USE_MEMORY_MAPPED flag in the creation flags,
 * you tell the allocator to make a best effort to use a MappedByteBuffer
 * rather than RandomAccessFile to perform IO on the underlying file.  Since
 * resizing a byte buffer is expensive, it is only resized in 4 MB increments
 * (after the initial size).
 * </p><p>
 * This class is not thread-safe; the client is responsible for coordinating
 * access to a single instance.  SafeDiskAllocator provides a thread-safe
 * implementation.  Conceptually, there are two distinct resources that
 * might be contended for at runtime: the internal state, and the underlying
 * file.  The modify count, used for managing allocators, is part of the internal
 * state but is grouped with the underlying file for this purpose.
 * </p><p>
 * Implementation Details:
 * </p><p>
 * All numbers are stored in Java-serialization standard format as 32-bit
 * integers.  Sizes and offsets are stored as signed integers; the absolute
 * value is taken when the values are interpreted.  If the integers is
 * negative, that provides additional information about the associated region.
 * </p><p>
 * Each region is 8 bytes longer than the client visible portion.
 * The first and last four bytes of a region are the size of the region in
 * the file.  If the first four bytes are
 * negative, it means that the region is free.  For free regions, the first
 * four bytes of what was the client visible portion of the region is
 * converted to the file offset to the next free region, or 0 if this is
 * the last free region.  The next four bytes refer to the
 * </p><p>
 * This implies that the smallest allowable client part of a region is 4
 * bytes, and therefore the smallest allowable region is 12 bytes.  Callers
 * can request smaller regions, but the region size will silently be increased
 * to the minimum.  The allocator does not keep track of what the originally
 * requested size of the region was.
 * </p><p>
 * If there are free regions when an allocation call is made, the first free
 * region large enough to contain the requested allocation is used.  If this
 * free region is big enough so that more than the minimum region size is
 * left over, the region is split.
 * </p><p>
 * Free regions are kept in a linked list that starts with the last free
 * region in the file.  When looking for a free region for an allocation,
 * this linked list is traversed towards the front of the file.
 * </p><p>
 * In addition to the regions, the file uses some overhead space at the
 * front of the file.
 * </p><p>
 * 4 bytes - Offset of initial region<br>
 * 4 bytes - Smallest allocatable region size (internal size,
 * not client visible size)<br>
 * 4 bytes - Size increment when file is expanded<br>
 * 4 bytes - Total size of file<br>
 * 4 bytes - Offset of last free region in file (start of free region<br>
 * list).  If there are no free regions, this value is 0.<br>
 */
public class DiskAllocator {

    public static final int FORCE_CREATE = 1;

    public static final int FORCE_EXIST = 2;

    public static final int FORCE_MATCHING = 4;

    public static final int OVERWRITE_PARAMS = 8;

    public static final int USE_MEMORY_MAPPED = 16;

    public static final int DEFAULT_CHUNK_SIZE = 8;

    public static final int MIN_CHUNK_SIZE = 8;

    public static final int DEFAULT_INCREMENT_SIZE = 2048;

    public DiskAllocator(File file, int initialRegion, int chunkSize, int sizeIncrement, int creationFlags) throws IOException, DiskAllocatorException {
        if (chunkSize < MIN_CHUNK_SIZE) chunkSize = MIN_CHUNK_SIZE;
        if (sizeIncrement < MIN_SIZE_INCREMENT) sizeIncrement = MIN_SIZE_INCREMENT;
        if (sizeIncrement < chunkSize + REGION_OVERHEAD_SIZE) sizeIncrement = chunkSize + REGION_OVERHEAD_SIZE;
        largestFreeRegion = 0;
        modifyCount = 0l;
        if (file.exists()) initializeFromExisting(file, initialRegion, chunkSize, sizeIncrement, creationFlags); else initializeNew(file, initialRegion, chunkSize, sizeIncrement, creationFlags);
        readFreeList();
        checkInvariant();
    }

    public DiskAllocator(File file) throws IOException, DiskAllocatorException {
        this(file, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_SIZE, DEFAULT_INCREMENT_SIZE, 0);
    }

    /**
     * Depends on and modifies internal state of the class as well as the underlying file
     * @param size
     * @return
     * @throws IOException
     * @throws DiskAllocatorException
     */
    public int allocate(int size) throws IOException, DiskAllocatorException {
        checkInvariant();
        modifyCount++;
        ListIterator<FreeRegion> freeIterator = freeList.listIterator(freeList.size());
        FreeRegion freeRegion = null;
        int biggestSoFar = 0;
        size = normalizeRegionSize(size);
        if (largestFreeRegion == 0 || size <= largestFreeRegion) {
            for (; freeIterator.hasPrevious(); ) {
                freeRegion = freeIterator.previous();
                if (freeRegion.size > biggestSoFar) biggestSoFar = freeRegion.size;
                if (freeRegion.size >= size) break; else freeRegion = null;
            }
        }
        if (freeRegion == null) {
            if (biggestSoFar > largestFreeRegion) {
                largestFreeRegion = biggestSoFar;
            }
            extendFile(size);
            freeIterator = freeList.listIterator();
            freeRegion = freeIterator.next();
        } else {
            freeIterator.next();
            if (freeRegion.size >= largestFreeRegion) largestFreeRegion = 0;
        }
        int nextFreeRegionOffset = 0;
        if (freeIterator.hasNext()) {
            nextFreeRegionOffset = freeIterator.next().offset;
            freeIterator.previous();
        }
        int prevOffset = 0;
        freeIterator.previous();
        if (freeIterator.hasPrevious()) {
            prevOffset = freeIterator.previous().offset;
            freeIterator.next();
        }
        freeIterator.next();
        if (freeRegion.size - size >= minimumRegionSize) {
            FreeRegion newFreeRegion = freeRegion;
            freeRegion = new FreeRegion(freeRegion.offset, size);
            newFreeRegion.offset += size;
            newFreeRegion.size -= size;
            randomFile.seek(freeRegion.offset);
            randomFile.writeInt(size);
            randomFile.seek(newFreeRegion.offset - REGION_END_OFFSET);
            randomFile.writeInt(size);
            randomFile.writeInt(-(newFreeRegion.size));
            randomFile.writeInt(nextFreeRegionOffset);
            randomFile.writeInt(prevOffset);
            randomFile.seek(newFreeRegion.offset + newFreeRegion.size - REGION_END_OFFSET);
            randomFile.writeInt(newFreeRegion.size);
            if (newFreeRegion.size > largestFreeRegion && largestFreeRegion != 0) largestFreeRegion = newFreeRegion.size;
            if (prevOffset == 0) lastFreeRegionOffset = newFreeRegion.offset; else {
                randomFile.seek(prevOffset + REGION_FREE_PTR_OFFSET);
                randomFile.writeInt(newFreeRegion.offset);
            }
            if (nextFreeRegionOffset != 0) {
                randomFile.seek(nextFreeRegionOffset + REGION_PREV_FREE_OFFSET);
                randomFile.writeInt(newFreeRegion.offset);
            }
        } else {
            randomFile.seek(freeRegion.offset);
            randomFile.writeInt(freeRegion.size);
            if (prevOffset == 0) lastFreeRegionOffset = nextFreeRegionOffset; else {
                randomFile.seek(prevOffset + REGION_FREE_PTR_OFFSET);
                randomFile.writeInt(nextFreeRegionOffset);
            }
            if (nextFreeRegionOffset != 0) {
                randomFile.seek(nextFreeRegionOffset + REGION_PREV_FREE_OFFSET);
                randomFile.writeInt(prevOffset);
            }
            freeIterator.remove();
        }
        checkInvariant();
        return freeRegion.offset + REGION_START_OFFSET;
    }

    /**
	 * Depends on an modifies internal state of the class and the underlying file
	 * @param regionOffset
	 * @throws IOException
	 * @throws DiskAllocatorException
	 */
    public void free(int regionOffset) throws IOException, DiskAllocatorException {
        checkInvariant();
        regionOffset -= REGION_START_OFFSET;
        if (regionOffset < initialRegionOffset || regionOffset > fileSize - REGION_OVERHEAD_SIZE - MIN_CHUNK_SIZE) throw new DiskAllocatorException("Not a region");
        if (regionOffset == initialRegionOffset) throw new DiskAllocatorException("Can't free initial region");
        modifyCount++;
        randomFile.seek(regionOffset - REGION_END_OFFSET);
        int previousRegionSize = randomFile.readInt();
        int previousRegionOffset = regionOffset - previousRegionSize;
        int regionSize = randomFile.readInt();
        FreeRegion toFree = new FreeRegion(regionOffset, regionSize);
        int nextRegionOffset = regionOffset + regionSize;
        if (previousRegionOffset < initialRegionOffset || previousRegionOffset >= regionOffset || previousRegionSize < REGION_OVERHEAD_SIZE + MIN_CHUNK_SIZE || regionSize < REGION_OVERHEAD_SIZE + MIN_CHUNK_SIZE || nextRegionOffset <= regionOffset || nextRegionOffset > fileSize || (nextRegionOffset < fileSize && nextRegionOffset > fileSize - MIN_CHUNK_SIZE - REGION_OVERHEAD_SIZE)) throw new DiskAllocatorException("Not a region or corrupt file " + Integer.toString(previousRegionOffset) + " " + Integer.toString(previousRegionSize) + " " + Integer.toString(regionOffset) + " " + Integer.toString(regionSize) + " " + Integer.toString(nextRegionOffset) + " " + Integer.toString(fileSize));
        if (lastFreeRegionOffset == 0) {
            lastFreeRegionOffset = regionOffset;
            randomFile.seek(regionOffset);
            randomFile.writeInt(-regionSize);
            randomFile.writeInt(0);
            randomFile.writeInt(0);
            largestFreeRegion = regionSize;
            freeList.add(toFree);
            checkInvariant();
            return;
        }
        ListIterator<FreeRegion> freeIterator = freeList.listIterator();
        FreeRegion prevFreeRegion = null;
        FreeRegion nextFreeRegion = freeIterator.next();
        for (; nextFreeRegion.offset > regionOffset; nextFreeRegion = freeIterator.next()) {
            prevFreeRegion = nextFreeRegion;
            if (!freeIterator.hasNext()) {
                nextFreeRegion = null;
                break;
            }
        }
        boolean changeSize = false;
        if (nextFreeRegion != null && previousRegionOffset == nextFreeRegion.offset) {
            regionOffset = previousRegionOffset;
            regionSize += previousRegionSize;
            toFree.offset = regionOffset;
            toFree.size = regionSize;
            freeIterator.remove();
            if (freeIterator.hasNext()) {
                nextFreeRegion = freeIterator.next();
            } else {
                nextFreeRegion = null;
            }
            changeSize = true;
        }
        if (prevFreeRegion != null && nextRegionOffset == prevFreeRegion.offset) {
            regionSize += prevFreeRegion.size;
            toFree.size = regionSize;
            changeSize = true;
            if (nextFreeRegion != null) freeIterator.previous();
            freeIterator.previous();
            freeIterator.remove();
            if (freeIterator.hasPrevious()) {
                prevFreeRegion = freeIterator.previous();
                freeIterator.next();
            } else {
                prevFreeRegion = null;
            }
            if (freeIterator.hasNext()) freeIterator.next();
        }
        if (regionSize > largestFreeRegion && largestFreeRegion != 0) largestFreeRegion = regionSize;
        if (nextFreeRegion != null) {
            randomFile.seek(nextFreeRegion.offset + REGION_PREV_FREE_OFFSET);
            randomFile.writeInt(regionOffset);
        }
        randomFile.seek(regionOffset);
        randomFile.writeInt(-regionSize);
        randomFile.writeInt(nextFreeRegion == null ? 0 : nextFreeRegion.offset);
        randomFile.writeInt(prevFreeRegion == null ? 0 : prevFreeRegion.offset);
        if (changeSize) {
            randomFile.seek(regionOffset + regionSize - REGION_END_OFFSET);
            randomFile.writeInt(regionSize);
        }
        if (prevFreeRegion == null) {
            lastFreeRegionOffset = regionOffset;
        } else {
            randomFile.seek(prevFreeRegion.offset + REGION_FREE_PTR_OFFSET);
            randomFile.writeInt(regionOffset);
        }
        if (nextFreeRegion != null) freeIterator.previous();
        freeIterator.add(toFree);
        checkInvariant();
    }

    /**
	 * Does not change or depend on non-fixed internal state of class
	 * @return Offset of the initial, fixed-size region
	 */
    public int getInitialRegion() {
        return initialRegionOffset + REGION_START_OFFSET;
    }

    /**
	 * Does not change or depend on non-fixed internal state of class
	 * @return Size of smallest allocatable chunk
	 */
    public int getChunkSize() {
        return minimumRegionSize;
    }

    /**
     * Depends on internal state of class and underlying file but does not modify it
     * @param regionOffset
     * @return
     * @throws IOException
     * @throws DiskAllocatorException
     */
    public int getRegionSize(int regionOffset) throws IOException, DiskAllocatorException {
        regionOffset -= REGION_START_OFFSET;
        if (regionOffset < initialRegionOffset || regionOffset > fileSize - MIN_CHUNK_SIZE + REGION_OVERHEAD_SIZE) throw new DiskAllocatorException("Invalid region");
        randomFile.seek(regionOffset);
        return randomFile.readInt() - REGION_OVERHEAD_SIZE;
    }

    /**
	 * Depends on internal state of class and underlying file but does not modify it
	 * @param offset
	 * @param size
	 * @return
	 * @throws IOException
	 * @throws DiskAllocatorException
	 */
    public byte[] read(int offset, int size) throws IOException, DiskAllocatorException {
        if (offset < initialRegionOffset || offset > fileSize) throw new DiskAllocatorException("Not a region");
        if (size + offset < offset || size + offset > fileSize) throw new DiskAllocatorException("Reading past end of region");
        byte[] retVal = new byte[size];
        randomFile.seek(offset);
        randomFile.readFully(retVal);
        return retVal;
    }

    /**
	 * Depends on but does not modify internal state; modifies underlying file/modify count
	 * @param offset
	 * @param toWrite
	 * @throws IOException
	 * @throws DiskAllocatorException
	 */
    public void write(int offset, byte[] toWrite) throws IOException, DiskAllocatorException {
        int size = toWrite.length;
        if (offset < initialRegionOffset || offset > fileSize) throw new DiskAllocatorException("Not a region");
        if (size + offset < offset || size + offset > fileSize) throw new DiskAllocatorException("Writing past end of region");
        modifyCount++;
        randomFile.seek(offset);
        randomFile.write(toWrite);
    }

    /**
	 * Depends on and modifies internal state; modifies underlying file
	 * @throws IOException
	 */
    public void sync() throws IOException {
        writeOverhead();
        randomFile.sync();
    }

    /**
	 * Depends on an modifies internal state of the class and the underlying file
	 */
    public void close() throws IOException {
        IOException syncException = null;
        try {
            sync();
        } catch (IOException ioe) {
            syncException = ioe;
        }
        randomFile.close();
        randomFile = null;
        if (syncException != null) throw syncException;
    }

    /**
     * True if the file did not exist before this allocator was created.
     */
    public boolean isNewFile() {
        return newFile;
    }

    /**
     * Returns an iterator over the Integer values for the offsets of
     * the non-free regions excluding the initial region.
     * This iterator becomes invalid if the underlying DiskAllocator
     * object is modified in any way (allocations, frees, writes).
     */
    public Iterator<Integer> iterator() throws IOException {
        return new DiskAllocatorIterator(this);
    }

    public void walkInternalFreeList(PrintStream ps) {
        Statistics stats = new Statistics();
        int oldOffset = -1;
        int oldSize = 0;
        ps.println("Largest free region: " + Integer.toString(largestFreeRegion));
        for (ListIterator<FreeRegion> li = freeList.listIterator(); li.hasNext(); ) {
            FreeRegion fr = li.next();
            if (oldOffset != -1) {
                if (fr.offset >= oldOffset) ps.println("****Free list out of order");
                if (fr.offset == oldOffset + oldSize) ps.println("****Contiguous free regions");
            }
            oldOffset = fr.offset;
            oldSize = fr.size;
            stats.addValue(fr.size);
        }
        ps.println(stats.toString());
    }

    public void walkRegions(PrintStream ps) throws IOException, DiskAllocatorException {
        Statistics stats = new Statistics();
        for (Iterator<Integer> i = iterator(); i.hasNext(); ) {
            stats.addValue(getRegionSize(i.next().intValue()));
        }
        ps.println(stats.toString());
    }

    private static final int OVERHEAD_SIZE = 20;

    private static final int REGION_OVERHEAD_SIZE = 8;

    private static final int MIN_SIZE_INCREMENT = OVERHEAD_SIZE + REGION_OVERHEAD_SIZE + MIN_CHUNK_SIZE;

    private static final int REGION_START_OFFSET = 4;

    private static final int REGION_END_OFFSET = 4;

    private static final int REGION_FREE_PTR_OFFSET = 4;

    private static final int REGION_PREV_FREE_OFFSET = REGION_FREE_PTR_OFFSET + 4;

    private IRandomAccess randomFile;

    private long modifyCount;

    private boolean newFile;

    private int initialRegionOffset;

    private int minimumRegionSize;

    private int fileIncrementSize;

    private int fileSize;

    private int lastFreeRegionOffset;

    private int largestFreeRegion;

    private LinkedList<FreeRegion> freeList;

    private void initializeFromExisting(File file, int initialRegionSize, int chunkSize, int sizeIncrement, int creationFlags) throws IOException, DiskAllocatorException {
        if ((creationFlags & FORCE_CREATE) != 0) {
            throw new DiskAllocatorException("File already exists");
        }
        ORandomAccess underlying = new ORandomAccess(file, "rw");
        randomFile = underlying;
        if ((creationFlags & USE_MEMORY_MAPPED) != 0) {
            try {
                randomFile = new MappedAccess(underlying, 0L);
            } catch (IOException ioe) {
            }
        }
        newFile = false;
        randomFile.seek(0);
        initialRegionOffset = randomFile.readInt();
        if (initialRegionOffset != OVERHEAD_SIZE) throw new DiskAllocatorException("Invalid or corrupt file");
        minimumRegionSize = randomFile.readInt();
        fileIncrementSize = randomFile.readInt();
        fileSize = randomFile.readInt();
        lastFreeRegionOffset = randomFile.readInt();
        if ((creationFlags & FORCE_MATCHING) != 0) {
            int firstSize = randomFile.readInt();
            if (minimumRegionSize != chunkSize + REGION_OVERHEAD_SIZE || fileIncrementSize != sizeIncrement || firstSize != normalizeRegionSize(initialRegionSize)) {
                throw new DiskAllocatorException("Parameter mismatch");
            }
        }
        if ((creationFlags & OVERWRITE_PARAMS) != 0) {
            minimumRegionSize = chunkSize + REGION_OVERHEAD_SIZE;
            fileIncrementSize = sizeIncrement;
            writeOverhead();
        }
    }

    private void initializeNew(File file, int initialRegionSize, int chunkSize, int sizeIncrement, int creationFlags) throws IOException, DiskAllocatorException {
        if ((creationFlags & FORCE_EXIST) != 0) {
            throw new DiskAllocatorException("File does not exist");
        }
        ORandomAccess underlying = new ORandomAccess(file, "rw");
        randomFile = underlying;
        newFile = true;
        initialRegionOffset = OVERHEAD_SIZE;
        minimumRegionSize = chunkSize + REGION_OVERHEAD_SIZE;
        initialRegionSize = normalizeRegionSize(initialRegionSize);
        fileIncrementSize = sizeIncrement;
        int usedLength = initialRegionSize + OVERHEAD_SIZE;
        if (usedLength <= fileIncrementSize) fileSize = fileIncrementSize; else fileSize = (usedLength / fileIncrementSize) * fileIncrementSize + ((usedLength % fileIncrementSize) != 0 ? fileIncrementSize : 0);
        if (fileSize != usedLength && fileSize - usedLength < minimumRegionSize) fileSize += fileIncrementSize;
        if (fileSize < fileIncrementSize) throw new DiskAllocatorException("Initial size overflow");
        if ((creationFlags & USE_MEMORY_MAPPED) != 0) {
            try {
                randomFile = new MappedAccess(underlying, 0L);
                randomFile.extend(fileSize);
            } catch (IOException ioe) {
            }
        }
        if (fileSize == usedLength) {
            lastFreeRegionOffset = 0;
        } else {
            lastFreeRegionOffset = usedLength;
        }
        writeOverhead();
        randomFile.seek(OVERHEAD_SIZE);
        randomFile.writeInt(initialRegionSize);
        randomFile.writeInt(0);
        randomFile.seek(usedLength - REGION_END_OFFSET);
        randomFile.writeInt(initialRegionSize);
        if (lastFreeRegionOffset != 0) {
            randomFile.writeInt(-(fileSize - usedLength));
            randomFile.writeInt(0);
            randomFile.writeInt(0);
            randomFile.seek(fileSize - REGION_END_OFFSET);
            randomFile.writeInt(fileSize - usedLength);
        }
    }

    private int normalizeRegionSize(int userRegionSize) throws DiskAllocatorException {
        if (userRegionSize < 0) throw new DiskAllocatorException("Can't allocate negative space");
        userRegionSize += REGION_OVERHEAD_SIZE;
        if ((userRegionSize % minimumRegionSize) != 0) userRegionSize = (userRegionSize / minimumRegionSize + 1) * minimumRegionSize;
        if (userRegionSize < minimumRegionSize) throw new DiskAllocatorException("Size calculation overflow");
        return userRegionSize;
    }

    private void writeOverhead() throws IOException {
        randomFile.seek(0);
        randomFile.writeInt(initialRegionOffset);
        randomFile.writeInt(minimumRegionSize);
        randomFile.writeInt(fileIncrementSize);
        randomFile.writeInt(fileSize);
        randomFile.writeInt(lastFreeRegionOffset);
    }

    private void readFreeList() throws IOException, DiskAllocatorException {
        freeList = new LinkedList<FreeRegion>();
        for (int nextFreeRegion = lastFreeRegionOffset; nextFreeRegion != 0; ) {
            randomFile.seek(nextFreeRegion);
            int size = -randomFile.readInt();
            if (size <= 0) throw new DiskAllocatorException("Free list corrupt reading structure");
            if (size > largestFreeRegion) largestFreeRegion = size;
            freeList.add(new FreeRegion(nextFreeRegion, size));
            nextFreeRegion = randomFile.readInt();
        }
    }

    /**
	 * This function extends the allocator file so that there is a free region
	 * at the end with at least requestedRegionSize bytes in it.  The file is
	 * extended in multiples of the file increment.
	 * If the last region in the file before it was extended is free, that
	 * region is coalesced with the region added by extending the file.
	 */
    private int extendFile(int requestedRegionSize) throws IOException, DiskAllocatorException {
        int endRegionSize = 0;
        if (lastFreeRegionOffset != 0) {
            endRegionSize = freeList.getFirst().size;
            if (lastFreeRegionOffset + endRegionSize != fileSize) endRegionSize = 0; else if (endRegionSize >= largestFreeRegion) largestFreeRegion = 0;
        }
        int addedSize = ((requestedRegionSize - endRegionSize) / fileIncrementSize + ((requestedRegionSize - endRegionSize) % fileIncrementSize == 0 ? 0 : 1)) * fileIncrementSize;
        if (addedSize < 0 || (long) addedSize + (long) fileSize > (long) Integer.MAX_VALUE) throw new DiskAllocatorException("File extension overflow");
        int newLastRegionSize = endRegionSize + addedSize;
        int newRegionOffset;
        if (endRegionSize != 0) {
            newRegionOffset = lastFreeRegionOffset;
            randomFile.seek(lastFreeRegionOffset + REGION_FREE_PTR_OFFSET);
            lastFreeRegionOffset = randomFile.readInt();
            freeList.removeFirst();
        } else {
            newRegionOffset = fileSize;
        }
        if (fileSize + addedSize != newRegionOffset + newLastRegionSize) throw new DiskAllocatorException("File size mismatch");
        randomFile = randomFile.extend(fileSize + addedSize);
        fileSize = fileSize + addedSize;
        randomFile.seek(newRegionOffset);
        randomFile.writeInt(-newLastRegionSize);
        randomFile.writeInt(lastFreeRegionOffset);
        randomFile.writeInt(0);
        lastFreeRegionOffset = newRegionOffset;
        randomFile.seek(fileSize - REGION_END_OFFSET);
        randomFile.writeInt(newLastRegionSize);
        freeList.addFirst(new FreeRegion(newRegionOffset, newLastRegionSize));
        checkInvariant();
        return newLastRegionSize;
    }

    private void checkInvariant() throws DiskAllocatorException {
        if (lastFreeRegionOffset == 0) {
            if (!freeList.isEmpty()) throw new DiskAllocatorException("Free offset with empty free list?");
        } else {
            if (freeList.isEmpty()) throw new DiskAllocatorException("Non-zero offset with empty free list");
            if (lastFreeRegionOffset != freeList.getFirst().offset) {
                throw new DiskAllocatorException("lastFreeRegionOffset " + Integer.toString(lastFreeRegionOffset) + " does not match first offset " + Integer.toString(freeList.getFirst().offset));
            }
        }
    }

    private static class FreeRegion {

        int offset;

        int size;

        FreeRegion(int o, int s) {
            offset = o;
            size = s;
        }
    }

    private static class DiskAllocatorIterator implements Iterator<Integer> {

        private DiskAllocator allocator;

        private long startingModifyCount;

        private int nextOffset;

        private int nextSize;

        private boolean isNext;

        DiskAllocatorIterator(DiskAllocator alloc) throws IOException {
            allocator = alloc;
            startingModifyCount = allocator.modifyCount;
            nextOffset = allocator.initialRegionOffset;
            allocator.randomFile.seek(nextOffset);
            nextSize = allocator.randomFile.readInt();
            if (nextSize < 0) nextSize = -nextSize;
            getNextNonFree();
        }

        public boolean hasNext() {
            return isNext;
        }

        public Integer next() {
            if (allocator.modifyCount != startingModifyCount) {
                throw new IllegalStateException("Underlying allocator modified");
            }
            if (!isNext) {
                throw new NoSuchElementException();
            }
            Integer result = new Integer(nextOffset + REGION_START_OFFSET);
            try {
                getNextNonFree();
            } catch (IOException ioe) {
                throw new IllegalStateException(ioe.getMessage());
            }
            return result;
        }

        private void getNextNonFree() throws IOException {
            isNext = false;
            for (nextOffset += nextSize; nextOffset < allocator.fileSize && !isNext; ) {
                allocator.randomFile.seek(nextOffset);
                nextSize = allocator.randomFile.readInt();
                if (nextSize < 0) {
                    nextOffset -= nextSize;
                } else isNext = true;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Common interface for random file access that may be backed by a very thinly wrapped
     * RandomAccessFile or a memory buffer
     * 
     * @author Michael A. MacDonald
     *
     */
    static interface IRandomAccess extends Closeable {

        void sync() throws IOException;

        void readFully(byte[] buf) throws IOException;

        int readInt() throws IOException;

        void seek(long addr) throws IOException;

        void write(byte[] buf) throws IOException;

        void writeInt(int i) throws IOException;

        IRandomAccess extend(long newSize) throws IOException;
    }

    static class ORandomAccess extends RandomAccessFile implements IRandomAccess {

        private FileDescriptor fd;

        ORandomAccess(File f, String mode) throws FileNotFoundException {
            super(f, mode);
        }

        public void sync() throws IOException {
            if (fd == null) {
                fd = getFD();
            }
            fd.sync();
        }

        public IRandomAccess extend(long newSize) throws IOException {
            return this;
        }
    }

    static class MappedAccess implements IRandomAccess {

        private static final long MAPPED_SIZE_INCREMENT = 4 * 1024 * 1024;

        private static final long MINIMUM_MAPPED_SIZE = 16 * 1024;

        private FileChannel channel;

        private ORandomAccess randomAccess;

        private MappedByteBuffer buffer;

        private long currentMax;

        MappedAccess(ORandomAccess underlying, long initialSize) throws IOException {
            randomAccess = underlying;
            channel = underlying.getChannel();
            if (initialSize <= 0L) {
                initialSize = channel.size();
            }
            if (initialSize < MINIMUM_MAPPED_SIZE) {
                initialSize = MINIMUM_MAPPED_SIZE;
            }
            currentMax = initialSize;
            buffer = channel.map(MapMode.READ_WRITE, 0L, currentMax);
        }

        @Override
        public void close() throws IOException {
            buffer.force();
            buffer = null;
            randomAccess.close();
        }

        @Override
        public IRandomAccess extend(long newSize) throws IOException {
            if (newSize <= currentMax) return this;
            buffer.force();
            ReferenceQueue<MappedByteBuffer> queue = new ReferenceQueue<MappedByteBuffer>();
            @SuppressWarnings("unused") PhantomReference<MappedByteBuffer> phantom = new PhantomReference<MappedByteBuffer>(buffer, queue);
            @SuppressWarnings("unused") WeakReference<MappedByteBuffer> weak = new WeakReference<MappedByteBuffer>(buffer, null);
            buffer = null;
            if (newSize - currentMax < MAPPED_SIZE_INCREMENT) currentMax += MAPPED_SIZE_INCREMENT; else currentMax = newSize;
            try {
                System.gc();
                try {
                    queue.remove();
                } catch (InterruptedException ie) {
                }
                buffer = channel.map(MapMode.READ_WRITE, 0L, currentMax);
            } catch (IOException ioe) {
                return randomAccess;
            }
            return this;
        }

        @Override
        public void readFully(byte[] buf) throws IOException {
            buffer.get(buf);
        }

        @Override
        public int readInt() throws IOException {
            return buffer.getInt();
        }

        @Override
        public void seek(long addr) throws IOException {
            buffer.position((int) addr);
        }

        @Override
        public void sync() throws IOException {
            buffer.force();
        }

        @Override
        public void write(byte[] buf) throws IOException {
            buffer.put(buf);
        }

        @Override
        public void writeInt(int i) throws IOException {
            buffer.putInt(i);
        }
    }
}

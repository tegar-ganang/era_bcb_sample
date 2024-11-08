package org.enerj.server.pageserver;

import static org.enerj.util.ByteArrayUtil.getInt;
import static org.enerj.util.ByteArrayUtil.getLong;
import static org.enerj.util.ByteArrayUtil.putInt;
import static org.enerj.util.ByteArrayUtil.putLong;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileLock;
import java.util.Properties;
import java.util.logging.Logger;
import org.enerj.util.StringUtil;

/**
 * Ener-J File Page Server. 
 * This class is <em>not</em> thread-safe.
 * <p>
 * Freed pages contain an 8-byte (long) physical offset to the next free page in the free page list.
 *
 * @version $Id: FilePageServer.java,v 1.4 2006/05/05 13:47:14 dsyrstad Exp $
 * @author <a href="mailto:dsyrstad@ener-j.org">Dan Syrstad</a>
 */
public class FilePageServer implements PageServer {

    private static final Logger sLogger = Logger.getLogger(FilePageServer.class.getName());

    public static final String VOLUME_PROP = "FilePageServer.volume";

    public static final String PAGE_SIZE_PROP = "FilePageServer.pageSize";

    /** Note that RandomAccessFile and FileChannel, by themselves, are thread-safe. */
    private RandomAccessFile mVolumeFile = null;

    /** The volume's header information. This attribute is read-only, but the 
     * contents of the header are read/write. 
     */
    private VolumeHeader mHeader = null;

    /** Shortcut to header's page size - we'll need this often. This is read-only. */
    private int mPageSize;

    /** True if the volume is read-only. This attribute is read-only. */
    private boolean mReadOnly = false;

    /** The file name corresponding to the volume. */
    private String mVolumeFileName;

    /**
     * Constructs a new FilePageServer.
     *
     * @param aVolumeFileName the full path and filename of the volume.
     * @param shouldForceOpen if true, volume will be opened even if it was not previously closed
     *  properly. This is primarily used for recovery. The volume will be marked as
     *  "closed properly" when it is closed.
     *
     * @throws PageServerException if an error occurs. This includes, but is not limited to:
     *   PageServerNotFoundException if the volume cannot be found; VolumeNeedsRecoveryException
     *  if the header "open" flag is set and shouldForceOpen is false.
     */
    private FilePageServer(String aVolumeFileName, boolean shouldForceOpen) throws PageServerException {
        mVolumeFileName = aVolumeFileName;
        File file = new File(aVolumeFileName);
        if (!file.exists()) {
            throw new PageServerNotFoundException("Cannot find volume: " + aVolumeFileName);
        }
        if (!file.canRead()) {
            throw new PageServerException("Cannot read volume: " + aVolumeFileName + " because of permission settings");
        }
        mReadOnly = !file.canWrite();
        String openMode = (mReadOnly ? "r" : "rw");
        boolean success = false;
        try {
            mVolumeFile = new RandomAccessFile(file, openMode);
            FileLock fileLock = mVolumeFile.getChannel().tryLock();
            if (fileLock == null) {
                throw new PageServerException("Cannot lock volume: " + aVolumeFileName);
            }
            mHeader = new VolumeHeader();
            mHeader.read(mVolumeFile);
            if (mHeader.isOpen() && !shouldForceOpen) {
                throw new VolumeNeedsRecoveryException("Volume was not properly closed: " + aVolumeFileName);
            }
            mHeader.setOpenFlag(true);
            mHeader.write(mVolumeFile);
            mVolumeFile.getChannel().force(true);
            mPageSize = mHeader.getPageSize();
            success = true;
        } catch (FileNotFoundException e) {
            throw new PageServerNotFoundException("Cannot find volume: " + aVolumeFileName, e);
        } catch (PageServerException e) {
            throw e;
        } catch (Throwable t) {
            throw new PageServerException("Cannot open volume " + aVolumeFileName + ": " + t, t);
        } finally {
            if (!success) {
                if (mVolumeFile != null) {
                    try {
                        mVolumeFile.close();
                    } catch (Exception e) {
                    }
                    mVolumeFile = null;
                }
            }
        }
    }

    /**
     * Creates a page server physical volume (file). The file may exist on 
     * the OS's filesystem, or it may be a raw device (e.g., a raw disk partition).
     *
     * @param aVolumeFileName the full pathname of the volume's file. This file must
     *  not exist unless it is a raw device.
     * @param aPageSize the page size for pages in this volume. Must not exceed
     *  MAX_PAGE_SIZE and must be at least MIN_PAGE_SIZE. Should normally be an multiple of the disk block size.
     * @param aDatabaseID the unique database ID associated with this volume.
     * @param aLogicalFirstPageOffset the logical page offset for the first page in this
     *  volume. This will be rounded up to the nearest page boundary.
     * @param aMaximumSize the maximum size for this volume. This will be rounded up
     *  to the nearest page boundary. If this value is zero, the volume will grow unbounded.
     * @param aPreAllocatedSize the number of bytes to pre-allocate. This will be rounded up
     *  to the nearest page boundary.
     *
     * @throws PageServerException if an error occurs.
     */
    public static void createVolume(String aVolumeFileName, int aPageSize, long aDatabaseID, long aLogicalFirstPageOffset, long aMaximumSize, long aPreAllocatedSize) throws PageServerException {
        File volumeFile = new File(aVolumeFileName);
        if (volumeFile.exists()) {
            if (volumeFile.isFile() || volumeFile.isDirectory()) {
                throw new PageServerException("File already exists: " + aVolumeFileName);
            }
        }
        if (aPageSize < MIN_PAGE_SIZE || aPageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page Size out of range: " + aPageSize);
        }
        aLogicalFirstPageOffset = ((aLogicalFirstPageOffset + aPageSize - 1) / aPageSize) * aPageSize;
        aMaximumSize = ((aMaximumSize + aPageSize - 1) / aPageSize) * aPageSize;
        aPreAllocatedSize = ((aPreAllocatedSize + aPageSize - 1) / aPageSize) * aPageSize;
        if (aLogicalFirstPageOffset < NULL_OFFSET) {
            throw new IllegalArgumentException("First page offset out of range: " + aLogicalFirstPageOffset);
        }
        RandomAccessFile volume = null;
        boolean success = false;
        FileLock lock = null;
        try {
            volume = new RandomAccessFile(volumeFile, "rw");
            lock = volume.getChannel().tryLock();
            if (lock == null) {
                throw new PageServerException("Cannot lock volume: " + volumeFile);
            }
            VolumeHeader hdr = new VolumeHeader(aPageSize, aDatabaseID, aLogicalFirstPageOffset, aMaximumSize);
            hdr.write(volume);
            if (aPreAllocatedSize != 0) {
                final int bufSize = 8192;
                byte[] buf = new byte[bufSize];
                long startOffset = hdr.getPhysicalFirstPageOffset();
                long bytesLeft = aPreAllocatedSize - startOffset;
                volume.seek(startOffset);
                while (bytesLeft > 0) {
                    int numToWrite = (int) (bufSize > bytesLeft ? bytesLeft : bufSize);
                    volume.write(buf, 0, numToWrite);
                    bytesLeft -= numToWrite;
                }
            }
            success = true;
        } catch (IOException e) {
            throw new PageServerException("Error creating volume: " + e, e);
        } finally {
            if (volume != null) {
                try {
                    if (lock != null) {
                        lock.release();
                    }
                    volume.close();
                    volume = null;
                } catch (IOException e) {
                    if (success) {
                        throw new PageServerException("Error closing volume: " + e, e);
                    }
                }
            }
            if (!success) {
                volumeFile.delete();
            }
        }
    }

    /**
     * Creates a page server physical volume (file) that is part of a "chain"
     * of volumes. The file may exist on 
     * the OS's filesystem, or it may be a raw device (e.g., a raw disk partition).
     *
     * @param aPrecedingVolumeFileName the full pathname of the volume which will
     *  precede this one. This file must exist and it's header must match aDatabaseID.
     * @param aVolumeFileName the full pathname of the volume's file. This file must
     *  not exist unless it is a raw device.
     * @param aDatabaseID the unique database ID associated with this volume.
     * @param aLogicalFirstPageOffset the logical page offset for the first page in this
     *  volume. This will be rounded up to the nearest page boundary.
     * @param aMaximumSize the maximum size for this volume. This will be rounded up
     *  to the nearest page boundary. If this value is zero, the volume will grow unbounded.
     * @param aPreAllocatedSize the number of bytes to pre-allocate. This will be rounded up
     *  to the nearest page boundary.
     *
     * @throws PageServerException if an error occurs.
     */
    public static void createChainedVolume(String aPrecedingVolumeFileName, String aVolumeFileName, long aDatabaseID, long aLogicalFirstPageOffset, long aMaximumSize, long aPreAllocatedSize) throws PageServerException {
        throw new PageServerException("Not implemented yet");
    }

    /**
     * Gets the volume's file name.
     *
     * @return the file name.
     */
    public String getVolumeFileName() {
        return mVolumeFileName;
    }

    /**
     * Connects to a FilePageServer.
     *
     * @param someProperties properties which specify the connect parameters.
     * The properties can contain the following keys:<br>
     * <ul>
     * <li><i>FilePageServer.volume</i> - the file name of volume managed by this FilePageServer. Required.</li>   
     * <li><i>PageServer.forceOpen</i> - forces the page server to open the volume if it wasn't closed properly. Optional.</li>
     * </ul>
     *
     * @return a PageServer. 
     *
     * @throws PageServerException if an error occurs. This includes, but is not limited to:
     *   PageServerNotFoundException if the volume cannot be found; VolumeNeedsRecoveryException
     *  if the volume was not previously closed properly and shouldForceOpen is false.
     */
    public static PageServer connect(Properties someProperties) throws PageServerException {
        String volume = someProperties.getProperty("FilePageServer.volume");
        if (volume == null) {
            throw new PageServerException("FilePageServer.volume property is required");
        }
        volume = StringUtil.substituteMacros(volume, someProperties);
        boolean forceOpen = Boolean.valueOf(someProperties.getProperty("PageServer.forceOpen", "false"));
        return new FilePageServer(volume, forceOpen);
    }

    public void disconnect() throws PageServerException {
        try {
            mHeader.setOpenFlag(false);
            mHeader.write(mVolumeFile);
            mVolumeFile.getChannel().force(true);
            mVolumeFile.close();
        } catch (Exception e) {
            throw new PageServerException("Could not properly close volume: " + e, e);
        } finally {
            mVolumeFile = null;
            mHeader = null;
        }
        sLogger.fine("FilePageServer disconnected/shutdown");
    }

    public long getLogicalFirstPageOffset() {
        return mHeader.getLogicalFirstPageOffset();
    }

    public long getLogicalLastPageOffset() {
        long maxSize = mHeader.getMaximumSize();
        if (maxSize == 0) {
            return 0;
        }
        return mHeader.getLogicalFirstPageOffset() + (mHeader.getMaximumSize() - mHeader.getPhysicalFirstPageOffset()) - mPageSize;
    }

    public int getPageSize() {
        return mPageSize;
    }

    public long getVolumeCreationDate() {
        return mHeader.getCreationDate();
    }

    public long getDatabaseID() {
        return mHeader.getDatabaseID();
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    public void loadPage(byte[] aBuffer, int anIndex, int aLength, long aLogicalPageOffset, int anOffset) throws PageServerException {
        if ((aLength + anOffset) > getPageSize()) {
            throw new IllegalArgumentException("(aBuffer.remaining() + anOffset) > getPageSize()");
        }
        long physicalPageOffset = mHeader.convertLogicalToPhysical(aLogicalPageOffset);
        try {
            mVolumeFile.seek(physicalPageOffset + anOffset);
            mVolumeFile.read(aBuffer, anIndex, aLength);
        } catch (Exception e) {
            throw new PageServerException("Error loading page at " + aLogicalPageOffset + ": " + e, e);
        }
    }

    public void storePage(byte[] aBuffer, int anIndex, int aLength, long aLogicalPageOffset, int anOffset) throws PageServerException {
        if (mReadOnly) {
            throw new PageServerException("Volume is read only");
        }
        if ((aLength + anOffset) > getPageSize()) {
            throw new IllegalArgumentException("(aBuffer.remaining() + anOffset) > getPageSize()");
        }
        long physicalPageOffset = mHeader.convertLogicalToPhysical(aLogicalPageOffset);
        try {
            mVolumeFile.seek(physicalPageOffset + anOffset);
            mVolumeFile.write(aBuffer, anIndex, aLength);
        } catch (Exception e) {
            throw new PageServerException("Error storing page at " + aLogicalPageOffset + ": " + e, e);
        }
    }

    public long allocatePage() throws PageServerException {
        if (mReadOnly) {
            throw new PageServerException("Volume is read only");
        }
        long firstFreePage = 0L;
        try {
            firstFreePage = mHeader.getFreePageListHead();
            if (firstFreePage == NULL_OFFSET) {
                long nextPage = mHeader.getNextAllocationOffset();
                long maxSize = mHeader.getMaximumSize();
                if (maxSize > 0 && nextPage >= maxSize) {
                    throw new PageServerNoMoreSpaceException("No more space left in volume");
                }
                mHeader.setNextAllocationOffset(nextPage + mPageSize);
                return mHeader.convertPhysicalToLogical(nextPage);
            }
            mVolumeFile.seek(firstFreePage);
            long nextFreePage = mVolumeFile.readLong();
            mHeader.setFreePageListHead(nextFreePage);
            return mHeader.convertPhysicalToLogical(firstFreePage);
        } catch (Throwable t) {
            throw new PageServerException("Error reading free page list at " + firstFreePage + ": " + t, t);
        }
    }

    public void freePage(long aLogicalPageOffset) throws PageServerException {
        long physicalPageOffset = 0L;
        try {
            physicalPageOffset = mHeader.convertLogicalToPhysical(aLogicalPageOffset);
            long firstFreePage = mHeader.getFreePageListHead();
            mVolumeFile.seek(physicalPageOffset);
            mVolumeFile.writeLong(firstFreePage);
            mHeader.setFreePageListHead(physicalPageOffset);
        } catch (Throwable t) {
            throw new PageServerException("Error updating free page list at " + physicalPageOffset + ": " + t, t);
        }
    }

    public void syncAllPages() throws PageServerException {
        try {
            mHeader.write(mVolumeFile);
            mVolumeFile.getChannel().force(false);
        } catch (Throwable t) {
            throw new PageServerException("Error syncing pages: " + t, t);
        }
    }

    /**
     * Header for the volume.
     */
    private static final class VolumeHeader {

        private static final long sSignature = 0x0DBDA7ABA5315FABL;

        private static final int CURRENT_HEADER_VERSION = 1;

        private int mHeaderVersion;

        private long mDatabaseID;

        private long mLogicalFirstPageOffset;

        private long mPhysicalFirstPageOffset;

        private long mMaximumSize;

        private int mPageSize;

        private long mFreePageListHead;

        private boolean mOpenFlag;

        private long mCreationDate;

        private long mNextAllocationOffset;

        /**
         * Construct an empty VolumeHeader so that it can be read.
         */
        VolumeHeader() {
        }

        VolumeHeader(int aPageSize, long aDatabaseID, long aLogicalFirstPageOffset, long aMaximumSize) {
            mHeaderVersion = CURRENT_HEADER_VERSION;
            mDatabaseID = aDatabaseID;
            mLogicalFirstPageOffset = aLogicalFirstPageOffset;
            mPhysicalFirstPageOffset = ((1024 + aPageSize - 1) / aPageSize) * aPageSize;
            mMaximumSize = aMaximumSize;
            mPageSize = aPageSize;
            mFreePageListHead = NULL_OFFSET;
            mOpenFlag = false;
            mCreationDate = System.currentTimeMillis();
            mNextAllocationOffset = mPhysicalFirstPageOffset;
        }

        /** 
         * Gets Database ID.
         *
         * @return the Database ID.
         */
        long getDatabaseID() {
            return mDatabaseID;
        }

        /** 
         * Gets the LogicalFirstPageOffset.
         *
         * @return the LogicalFirstPageOffset.
         */
        long getLogicalFirstPageOffset() {
            return mLogicalFirstPageOffset;
        }

        /** 
         * Gets the PhysicalFirstPageOffset.
         *
         * @return PhysicalFirstPageOffset.
         *
         */
        long getPhysicalFirstPageOffset() {
            return mPhysicalFirstPageOffset;
        }

        /** 
         * Gets the MaximumSize of the volume.
         *
         * @return the MaximumSize.
         */
        long getMaximumSize() {
            return mMaximumSize;
        }

        /** 
         * Sets the MaximumSize.
         *
         * @param aMaximumSize the MaximumSize.
         */
        void setMaximumSize(long aMaximumSize) {
            mMaximumSize = aMaximumSize;
        }

        /** 
         * Gets the PageSize.
         *
         * @return the PageSize.
         */
        int getPageSize() {
            return mPageSize;
        }

        /** 
         * Gets the FreePageListHead.
         *
         * @return the FreePageListHead or NULL_OFFSET if there are no free pages.
         */
        long getFreePageListHead() {
            return mFreePageListHead;
        }

        /** 
         * Sets the FreePageListHead.
         *
         * @param aFreePageListHead the FreePageListHead or NULL_OFFSET if there
         *  are no more free pages.
         */
        void setFreePageListHead(long aFreePageListHead) {
            mFreePageListHead = aFreePageListHead;
        }

        /** 
         * Determines if the volume is currently open.
         *
         * @return true if the volume is open (or not closed properly), false
         *  if it is closed.
         */
        boolean isOpen() {
            return mOpenFlag;
        }

        /** 
         * Sets the whether the volume is open.
         *
         * @param anOpenFlag true if the volume is now open, else false.
         */
        void setOpenFlag(boolean anOpenFlag) {
            mOpenFlag = anOpenFlag;
        }

        /** 
         * Gets the date in which the volume was created.
         *
         * @return the date in number of milliseconds since Jan 1, 1970 00:00 UTC.
         *  Suitable for use with "new Date(long)".
         */
        long getCreationDate() {
            return mCreationDate;
        }

        /** 
         * Gets the physical offset to the next page that can be allocated when
         * the free page list is empty.
         *
         * @return the NextAllocationOffset.
         */
        long getNextAllocationOffset() {
            return mNextAllocationOffset;
        }

        /** 
         * Sets the physical offset to the next page that can be allocated when
         * the free page list is empty.
         *
         * @param aNextAllocationOffset the NextAllocationOffset.
         */
        void setNextAllocationOffset(long aNextAllocationOffset) {
            mNextAllocationOffset = aNextAllocationOffset;
        }

        /**
         * Converts a logical page offset to a physical offset.
         *
         * @param aLogicalPageOffset the logical page offset.
         *
         * @return the physical offset into the volume.
         */
        long convertLogicalToPhysical(long aLogicalPageOffset) {
            return (aLogicalPageOffset - mLogicalFirstPageOffset) + mPhysicalFirstPageOffset;
        }

        /**
         * Converts a physical page offset to a logical offset.
         *
         * @param aPhysicalPageOffset the physical page offset.
         *
         * @return the logical offset into the volume.
         */
        long convertPhysicalToLogical(long aPhysicalPageOffset) {
            return (aPhysicalPageOffset - mPhysicalFirstPageOffset) + mLogicalFirstPageOffset;
        }

        /**
         * Reads the header from the given FileChannel starting at NULL_OFFSET.
         */
        void read(RandomAccessFile aFile) throws PageServerException {
            byte[] hdr = new byte[1024];
            try {
                aFile.seek(NULL_OFFSET);
                aFile.readFully(hdr);
                int idx = 0;
                long signature = getLong(hdr, idx);
                idx += 8;
                if (signature != sSignature) {
                    throw new PageServerException("Volume signature " + Long.toHexString(signature) + " != " + Long.toHexString(sSignature));
                }
                mHeaderVersion = getInt(hdr, idx);
                idx += 4;
                if (mHeaderVersion != CURRENT_HEADER_VERSION) {
                    throw new PageServerException("Header version " + mHeaderVersion + " is not valid");
                }
                mDatabaseID = getLong(hdr, idx);
                idx += 8;
                mLogicalFirstPageOffset = getLong(hdr, idx);
                idx += 8;
                mPhysicalFirstPageOffset = getLong(hdr, idx);
                idx += 8;
                mMaximumSize = getLong(hdr, idx);
                idx += 8;
                mPageSize = getInt(hdr, idx);
                idx += 4;
                mFreePageListHead = getLong(hdr, idx);
                idx += 8;
                mOpenFlag = (hdr[idx++] != 0);
                mCreationDate = getLong(hdr, idx);
                idx += 8;
                mNextAllocationOffset = getLong(hdr, idx);
                idx += 8;
            } catch (BufferUnderflowException e) {
                throw new PageServerException("Underflow reading volume header:" + e.toString(), e);
            } catch (IOException e) {
                throw new PageServerException("Error reading volume header:" + e.toString(), e);
            }
        }

        /**
         * Writes the header to the given FileChannel starting at NULL_OFFSET.
         */
        void write(RandomAccessFile aFile) throws PageServerException {
            byte[] hdr = new byte[1024];
            try {
                int idx = 0;
                idx += putLong(hdr, idx, sSignature);
                idx += putInt(hdr, idx, mHeaderVersion);
                idx += putLong(hdr, idx, mDatabaseID);
                idx += putLong(hdr, idx, mLogicalFirstPageOffset);
                idx += putLong(hdr, idx, mPhysicalFirstPageOffset);
                idx += putLong(hdr, idx, mMaximumSize);
                idx += putInt(hdr, idx, mPageSize);
                idx += putLong(hdr, idx, mFreePageListHead);
                hdr[idx++] = (byte) (mOpenFlag ? 1 : 0);
                idx += putLong(hdr, idx, mCreationDate);
                idx += putLong(hdr, idx, mNextAllocationOffset);
                aFile.seek(NULL_OFFSET);
                aFile.write(hdr);
            } catch (BufferOverflowException e) {
                throw new PageServerException("Overflow writing volume header:" + e.toString(), e);
            } catch (IOException e) {
                throw new PageServerException("Error writing volume header:" + e.toString(), e);
            }
        }
    }
}

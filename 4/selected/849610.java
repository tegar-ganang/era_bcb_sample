package org.tranche.flatfile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import org.tranche.annotations.Fix;
import org.tranche.commons.Debuggable;
import org.tranche.exceptions.AssertionFailedException;
import org.tranche.exceptions.UnexpectedEndOfDataBlockException;
import org.tranche.hash.BigHash;
import org.tranche.util.IOUtil;

/**
 * <p>Represents a block of data. Might have more than one file in it.</p>
 * <p>Note that the following critical activities are synchronized on the DataBlock object:</p>
 * <ul>
 *   <li>Adding bytes to data block</li>
 *   <li>Checking whether data block has bytes</li>
 *   <li>Getting bytes from data block</li>
 *   <li>Deleting block</li>
 *   <li>Getting hashes in data block</li>
 * </ul>
 * @author Jayson Falkner - jfalkner@umich.edu
 * @author Bryan E. Smith - bryanesmith@gmail.com
 */
@Fix(problem = "OutOfMemory due to greater than 450K array of subBlocks instantiated, almost all with 256 null elements. Took around 400MB or more memory.", solution = "Lazily instantiate subBlocks, since only accessed when needed. If servers have a lot of disk space, need substantially more than 512MB.", day = 15, month = 8, year = 2008, author = "Bryan Smith")
public class DataBlock extends Debuggable implements Comparable {

    public static BigHash HASH_LENGTH_ZERO = new BigHash(new byte[0]);

    /**
     * <p>The maximum DataBlock size, in bytes.</p>
     */
    public static final int DEFAULT_MAX_BLOCK_SIZE = 100 * 1024 * 1024;

    /**
     * <p>The amount of headers per file.</p>
     * <p>Each header's size in bytes is equal to BigHash.HASH_LENGTH + 1 + 1 + 4 + 4. Every new block will start with this many headers. To get the blocks size, you must also include all the data in the block.</p>
     */
    public static final int DEFAULT_HEADERS_PER_FILE = 1000;

    private static int maxBlockSize = DEFAULT_MAX_BLOCK_SIZE;

    private static int headersPerFile = DEFAULT_HEADERS_PER_FILE;

    /**
     * <p>The maximum DataBlock size, in bytes.</p>
     * @deprecated Use getMaxBlockSize()
     */
    public static final int MAX_BLOCK_SIZE = DEFAULT_MAX_BLOCK_SIZE;

    /**
     * <p>The amount of headers per file.</p>
     * <p>Each header's size in bytes is equal to BigHash.HASH_LENGTH + 1 + 1 + 4 + 4. Every new block will start with this many headers. To get the blocks size, you must also include all the data in the block.</p>
     * @deprecated Use getHeadersPerFile
     */
    public static final int HEADERS_PER_FILE = 1000;

    /**
     * <p>The two-letter file name of the DataBlock.</p>
     */
    String filename;

    /**
     * <p>The DataDirectoryConfiguration object to which this DataBlock instance belongs.</p>
     */
    DataDirectoryConfiguration ddc;

    private DataBlock[] subBlocks = null;

    /**
     * <p>The flag representing a data chunk.</p>
     */
    static final byte DATA = 1;

    /**
     * <p>The flag representing a meta data chunk.</p>
     */
    static final byte META_DATA = 2;

    /**
     * <p>The status flag representing an okay chunk.</p>
     */
    static final byte STATUS_OK = 0;

    /**
     * <p>The status flag representing a deleted chunk.</p>
     */
    static final byte STATUS_DELETED = 1;

    /**
     * <p>Maximum wasted space allowed before this block resizes itself.</p>
     */
    private final int MAX_WASTED_SPACE_ALLOWED = 1024 * 1024 * 5;

    /**
     * <p>Used to read the header of the file to see what is in it one entry at a time.</p>
     * <p>This is the size of an entry in bytes. Each header is hash + (byte) type + (byte) status + int (offset in block) + int (size)</p>
     */
    static final int bytesPerEntry = (BigHash.HASH_LENGTH + 1 + 1 + 4 + 4);

    /**
     * <p>Need a reference back to DBU to repair files</p>
     */
    private final DataBlockUtil dbu;

    /**
     * 
     * @param filename
     * @param ddc
     * @param dbu
     */
    public DataBlock(String filename, DataDirectoryConfiguration ddc, DataBlockUtil dbu) {
        this.filename = filename;
        this.ddc = ddc;
        this.dbu = dbu;
    }

    /**
     * <p>Returns true if this DataBlock is a directory. (I.e., it has children DataBlock instances.)</p>
     * @return
     */
    public final boolean isDirectory() {
        return new File(ddc.getDirectoryFile().getAbsolutePath() + filename).isDirectory();
    }

    /**
     * <p>Get absolute path to underlying file for DataBlock. No guarentee if regular file or directory.</p>
     * @return The absolute path to the file
     */
    public final String getAbsolutePath() {
        return new File(ddc.getDirectoryFile().getAbsolutePath(), filename).getAbsolutePath();
    }

    /**
     * <p>Returns true if this file is half-way through a merge.</p>
     * @return
     */
    public final boolean isMerging() {
        return new File(ddc.getDirectoryFile().getAbsolutePath() + filename + ".merge").exists();
    }

    /**
     * <p>Return the size in bytes of the underlying file for the DataBlock instance.</p>
     * @return
     */
    public final long length() {
        return new File(ddc.getDirectoryFile().getAbsolutePath() + filename).length();
    }

    /**
     * <p>Returns the underlying file for the DataBlock. Might be a merge file (.merge) or the normal file/directory.</p>
     * @return
     */
    private final File getRegularOrMergeFile() {
        File merge = new File(ddc.getDirectoryFile().getAbsolutePath() + filename + ".merge");
        if (merge.exists()) {
            return merge;
        }
        File regular = new File(ddc.getDirectoryFile().getAbsolutePath() + filename);
        return regular;
    }

    /**
     * <p>Returns the list of either data or meta-data hashes stored in this block. If true is passed as a parameter, only meta-data is returned. Otherwise only data hashes are returned.</p>
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    public final synchronized List<BigHash> getHashes(boolean isMetaData) throws Exception {
        if (isDirectory() && !isMerging()) {
            return new ArrayList(0);
        }
        byte[] buf = new byte[getBytesToRead()];
        ArrayList<BigHash> hashesToReturn = new ArrayList();
        File blockFile = getRegularOrMergeFile();
        if (!blockFile.exists()) {
            return hashesToReturn;
        }
        RandomAccessFile ras = new RandomAccessFile(blockFile, "r");
        try {
            final byte isMetaDataByte = isMetaData ? META_DATA : DATA;
            fillWithBytes(buf, ras, blockFile.getAbsolutePath(), "Reading in header to get hashes for " + (isMetaData ? "meta data" : "data") + ".");
            for (int i = 0; i < getHeadersPerFile(); i++) {
                int offset = i * bytesPerEntry;
                BigHash h = BigHash.createFromBytes(buf, offset);
                byte type = buf[offset + BigHash.HASH_LENGTH];
                byte status = buf[offset + BigHash.HASH_LENGTH + 1];
                int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);
                if (s == 0 && o == 0) {
                    break;
                }
                if (status != STATUS_OK || type != isMetaDataByte) {
                    continue;
                }
                hashesToReturn.add(BigHash.createFromBytes(h.toByteArray()));
            }
            return hashesToReturn;
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Uses cached information to get chunk.</p>
     * @param o Offset of chunk
     * @param s Size of chunk
     * @return Bytes for chunk
     * @throws java.lang.Exception
     */
    public final synchronized byte[] getBytes(final int o, final int s) throws Exception {
        File rasFile = getRegularOrMergeFile();
        if (!rasFile.exists()) {
            throw new FileNotFoundException("Bytes don't exist on this server for data block file; could not find file from getBytes(int,int): " + rasFile.getAbsolutePath());
        }
        if (rasFile.isDirectory()) {
            return null;
        }
        RandomAccessFile ras = new RandomAccessFile(rasFile, "r");
        try {
            byte[] content = new byte[s];
            ras.seek(o);
            fillWithBytes(content, ras, rasFile.getAbsolutePath(), "Reading in chunk based on cached offset and size to return.");
            return content;
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Returns the bytes (chunk) representing by the hash, or throws a FileNotFoundException if not found.</p>
     * @param hash
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    public final synchronized byte[] getBytes(BigHash hash, boolean isMetaData) throws Exception {
        if (isDirectory() && !isMerging()) {
            return ddc.dbu.getDataBlockToAddChunk(hash).getBytes(hash, isMetaData);
        }
        byte[] buf = new byte[getBytesToRead()];
        final byte isMetaDataByte = isMetaData ? META_DATA : DATA;
        File rasFile = getRegularOrMergeFile();
        if (!rasFile.exists()) {
            throw new FileNotFoundException("Bytes don't exist on this server for data block file; could not find file from getBytes(BigHash,boolean): " + rasFile.getAbsolutePath());
        }
        if (rasFile.isDirectory()) {
            int stopHere = 0;
        }
        RandomAccessFile ras = new RandomAccessFile(rasFile, "r");
        try {
            fillWithBytes(buf, ras, rasFile.getAbsolutePath(), "Reading header to get " + (isMetaData ? "meta data" : "data") + " chunk.");
            int entryNumber = 0;
            for (int i = 0; i < getHeadersPerFile(); i++) {
                entryNumber = i;
                int offset = i * bytesPerEntry;
                BigHash entryHash = BigHash.createFromBytes(buf, offset);
                byte type = buf[offset + BigHash.HASH_LENGTH];
                byte status = buf[offset + BigHash.HASH_LENGTH + 1];
                int entryOffset = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                int chunkSize = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);
                if (chunkSize == 0 && entryOffset == 0 && !entryHash.equals(HASH_LENGTH_ZERO)) {
                    break;
                }
                if (status != STATUS_OK || !entryHash.equals(hash) || type != isMetaDataByte) {
                    continue;
                }
                byte[] content = new byte[chunkSize];
                ras.seek(entryOffset);
                fillWithBytes(content, ras, rasFile.getAbsolutePath(), "Reading in " + (isMetaData ? "meta data" : "data") + " chunk to return.");
                return content;
            }
            throw new FileNotFoundException("Bytes don't exist on this server for data block file; read total of " + entryNumber + " entries before giving up:" + rasFile.getAbsolutePath() + " [" + hash + "]");
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Check whether the bytes (chunk) represented by the hash exist in this DataBlock instance.</p>
     * @param hash
     * @param isMetaData
     * @return
     * @throws java.lang.Exception
     */
    public final synchronized boolean hasBytes(BigHash hash, boolean isMetaData) throws Exception {
        if (isDirectory() && !isMerging()) {
            return ddc.dbu.getDataBlockToAddChunk(hash).hasBytes(hash, isMetaData);
        }
        File rasFile = getRegularOrMergeFile();
        if (!rasFile.exists()) {
            return false;
        }
        byte[] buf = new byte[getBytesToRead()];
        final byte isMetaDataByte = isMetaData ? META_DATA : DATA;
        RandomAccessFile ras = new RandomAccessFile(rasFile, "r");
        try {
            fillWithBytes(buf, ras, rasFile.getAbsolutePath(), "Reading in header while checking if has " + (isMetaData ? "meta data" : "data") + " chunk.");
        } finally {
            ras.close();
        }
        for (int i = 0; i < getHeadersPerFile(); i++) {
            int offset = i * bytesPerEntry;
            BigHash h = BigHash.createFromBytes(buf, offset);
            byte type = buf[offset + BigHash.HASH_LENGTH];
            byte status = buf[offset + BigHash.HASH_LENGTH + 1];
            int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
            int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);
            if (s == 0 && o == 0) {
                break;
            }
            if (status != STATUS_OK || !h.equals(hash) || type != isMetaDataByte) {
                continue;
            }
            try {
                if (this.dbu.isUseCache()) {
                    DataBlockCacheEntry e = DataBlockCacheEntry.create(hash, DataBlock.this, o, s);
                    this.dbu.getDataBlockCache().add(e, isMetaData);
                }
            } catch (Exception e) {
            }
            return true;
        }
        return false;
    }

    /**
     * <p>Add a chunk to this DataBlock.</p>
     * @param hash
     * @param isMetaData
     * @param bytes
     * @throws java.lang.Exception
     */
    public final synchronized void addBytes(BigHash hash, boolean isMetaData, byte[] bytes) throws Exception {
        addBytes(hash, isMetaData, bytes, 1);
    }

    /**
     * <p>Add a chunk to this DataBlock.</p>
     * @param hash
     * @param isMetaData
     * @param bytes
     * @param recursionCount The number of times this has been recursively called while waiting for DataBlock to merge. (After a certain number of times, this will fail and throw an exception.)
     * @throws java.lang.Exception
     */
    public final synchronized void addBytes(BigHash hash, boolean isMetaData, byte[] bytes, int recursionCount) throws Exception {
        final String blockPath = ddc.getDirectoryFile().getAbsolutePath() + filename;
        if (recursionCount >= 100 && isMerging()) {
            throw new Exception("Cannot add bytes; still merging, tried " + recursionCount + " times for " + blockPath);
        }
        if (isMerging()) {
            Thread.sleep(50);
            ddc.dbu.getDataBlockToAddChunk(hash).addBytes(hash, isMetaData, bytes, recursionCount + 1);
            return;
        }
        byte[] buf = new byte[getBytesToRead()];
        lazyCreateFile(buf);
        long bytesUsed = 0;
        long bytesWasted = 0;
        final byte isMetaDataByte = isMetaData ? META_DATA : DATA;
        int nextValidOffset = getBytesToRead();
        RandomAccessFile ras = new RandomAccessFile(blockPath, "rw");
        try {
            fillWithBytes(buf, ras, blockPath, "Reading in header for data block to add " + (isMetaData ? "meta data" : "data") + " chunk.");
            int totalEntriesRead = 0;
            for (int i = 0; i < getHeadersPerFile(); i++) {
                totalEntriesRead++;
                int offset = i * bytesPerEntry;
                BigHash h = BigHash.createFromBytes(buf, offset);
                byte type = buf[offset + BigHash.HASH_LENGTH];
                byte status = buf[offset + BigHash.HASH_LENGTH + 1];
                int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);
                if (h.equals(hash)) {
                    if (type == isMetaDataByte) {
                        ras.seek(i * bytesPerEntry + BigHash.HASH_LENGTH + 1);
                        ras.write(STATUS_DELETED);
                        status = STATUS_DELETED;
                    }
                }
                if (status == STATUS_OK) {
                    bytesUsed += s;
                } else {
                    bytesWasted += s;
                }
                if (o != 0) {
                    nextValidOffset = o + s;
                    continue;
                }
                ras.seek(nextValidOffset);
                ras.write(bytes);
                byte[] headerBuf = new byte[bytesPerEntry];
                System.arraycopy(hash.toByteArray(), 0, headerBuf, 0, BigHash.HASH_LENGTH);
                headerBuf[BigHash.HASH_LENGTH] = isMetaDataByte;
                headerBuf[BigHash.HASH_LENGTH + 1] = STATUS_OK;
                headerBuf[BigHash.HASH_LENGTH + 2] = (byte) (nextValidOffset >> 24);
                headerBuf[BigHash.HASH_LENGTH + 2 + 1] = (byte) (nextValidOffset >> 16);
                headerBuf[BigHash.HASH_LENGTH + 2 + 2] = (byte) (nextValidOffset >> 8);
                headerBuf[BigHash.HASH_LENGTH + 2 + 3] = (byte) (nextValidOffset);
                headerBuf[BigHash.HASH_LENGTH + 6] = (byte) (bytes.length >> 24);
                headerBuf[BigHash.HASH_LENGTH + 6 + 1] = (byte) (bytes.length >> 16);
                headerBuf[BigHash.HASH_LENGTH + 6 + 2] = (byte) (bytes.length >> 8);
                headerBuf[BigHash.HASH_LENGTH + 6 + 3] = (byte) (bytes.length);
                ras.seek(offset);
                ras.write(headerBuf);
                ddc.adjustUsedSpace(bytes.length);
                bytesUsed += bytes.length;
                boolean tooManyBytes = ras.length() > DataBlock.getMaxBlockSize();
                boolean tooManyHeaders = i >= DataBlock.getHeadersPerFile() - 1;
                boolean tooMuchWastedSpace = bytesWasted > MAX_WASTED_SPACE_ALLOWED;
                if (!tooMuchWastedSpace && !tooManyBytes && !tooManyHeaders) {
                    return;
                }
                boolean tooManyBytesAdjusted = (ras.length() - bytesWasted) > DataBlock.getMaxBlockSize();
                boolean dontSplitBlock = tooMuchWastedSpace && !tooManyHeaders && !tooManyBytesAdjusted;
                cleanUpDataBlock(dontSplitBlock);
                return;
            }
            try {
                cleanUpDataBlock(false);
            } catch (Exception ex) {
                System.err.println(ex.getClass().getSimpleName() + " while cleaning up data block (recursionCount=" + recursionCount + "): " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
            if (recursionCount <= 3) {
                ddc.dbu.getDataBlockToAddChunk(hash).addBytes(hash, isMetaData, bytes, recursionCount + 1);
            } else {
                throw new Exception("Can't write bytes to this block. Block is full! and recursionCount is " + recursionCount + ": " + blockPath + " <total entries read: " + totalEntriesRead + ", size of file: " + ras.length() + ">");
            }
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Lazily create the underlying file with enough space for the header.</p>
     * @param buf
     * @throws java.lang.Exception
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    private void lazyCreateFile(final byte[] buf) throws Exception, FileNotFoundException, IOException {
        File file = new File(ddc.getDirectoryFile().getAbsolutePath() + filename);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean madeDirs = parent.mkdirs();
                if (!madeDirs) {
                    throw new Exception("Can't make required parent directories: " + file.getAbsolutePath());
                }
            }
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(buf);
            } finally {
                IOUtil.safeClose(fos);
            }
            ddc.adjustUsedSpace(buf.length);
        }
    }

    /**
     * <p>Helper method to clean up a data block. Relies on a variable that signifies if the block should be split or not. Either way the block might be moved to a different disk.</p>
     * @param dontSplitBlock
     * @throws java.lang.Exception
     */
    final synchronized void cleanUpDataBlock(boolean dontSplitBlock) throws Exception {
        File normalFile = new File(ddc.getDirectoryFile().getAbsolutePath() + filename);
        if (!normalFile.exists()) {
            return;
        }
        byte[] buf = new byte[getBytesToRead()];
        lazyCreateFile(buf);
        if (!dontSplitBlock) {
            File backupFile = new File(ddc.getDirectoryFile().getAbsolutePath() + filename + ".merge");
            boolean renamedFile = normalFile.renameTo(backupFile);
            if (!renamedFile) {
                throw new Exception("Can't expand data block! Reverting to old block. Existing files " + renamedFile + " exists: " + backupFile.exists() + "; " + normalFile + " exists: " + normalFile.exists());
            }
            if (ddc.dbu.purposelyFailCleanUp) {
                throw new Exception("Purposely failed for testing post .backup file creation.");
            }
            boolean madeDirectory = normalFile.mkdir();
            if (!madeDirectory) {
                try {
                    IOUtil.renameFallbackCopy(backupFile, normalFile);
                } finally {
                    throw new Exception("Can't expand data block! Reverting to old block.");
                }
            }
            DataBlockToMerge dbtm = new DataBlockToMerge(backupFile, ddc);
            ddc.dbu.mergeQueue.put(dbtm);
        } else {
            File backupFile = new File(ddc.getDirectoryFile().getAbsolutePath() + filename + ".backup");
            boolean renamedFile = normalFile.renameTo(backupFile);
            if (!renamedFile) {
                throw new Exception("Can't expand data block! Reverting to old block. Existing files " + renamedFile + " exists: " + backupFile.exists() + "; " + normalFile + " exists: " + normalFile.exists());
            }
            if (ddc.dbu.purposelyFailCleanUp) {
                throw new Exception("Purposely failed for testing post .backup file creation.");
            }
            long sizeToDecrement = backupFile.length();
            ddc.dbu.mergeOldDataBlock(backupFile, buf);
            ddc.adjustUsedSpace(-sizeToDecrement);
        }
    }

    private void mergeDataBlockNow(DataBlockToMerge dbtm) throws Exception {
        long sizeToDecrement = dbtm.fileToMerge.length();
        try {
            this.dbu.mergeOldDataBlock(dbtm.fileToMerge);
        } catch (UnexpectedEndOfDataBlockException ex) {
            this.dbu.repairCorruptedDataBlock(dbtm.fileToMerge, "ProjectFindingThread: merging old data block (2, indefinite merging)");
            throw ex;
        }
        dbtm.ddc.adjustUsedSpace(-sizeToDecrement);
    }

    /**
     * <p>Delete the bytes (chunk) from this DataBlock based on hash.</p>
     * @param hash
     * @param isMetaData
     * @throws java.lang.Exception
     */
    public final synchronized void deleteBytes(BigHash hash, boolean isMetaData) throws Exception {
        if (isDirectory() && !isMerging()) {
            ddc.dbu.getDataBlockToAddChunk(hash).deleteBytes(hash, isMetaData);
            return;
        }
        File rasFile = getRegularOrMergeFile();
        if (!rasFile.exists()) {
            return;
        }
        byte[] buf = new byte[getBytesToRead()];
        lazyCreateFile(buf);
        final byte isMetaDataByte = isMetaData ? META_DATA : DATA;
        int nextValidOffset = getBytesToRead();
        RandomAccessFile ras = new RandomAccessFile(rasFile, "rw");
        try {
            fillWithBytes(buf, ras, rasFile.getAbsolutePath(), "Reading in headers for data block to delete a " + (isMetaData ? "meta data" : "data") + " chunk.");
            for (int i = 0; i < getHeadersPerFile(); i++) {
                int offset = i * bytesPerEntry;
                BigHash h = BigHash.createFromBytes(buf, offset);
                byte type = buf[offset + BigHash.HASH_LENGTH];
                byte status = buf[offset + BigHash.HASH_LENGTH + 1];
                int o = buf[BigHash.HASH_LENGTH + offset + 2] << 24 | (buf[BigHash.HASH_LENGTH + offset + 2 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 2 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 2 + 3] & 0xff);
                int s = buf[BigHash.HASH_LENGTH + offset + 6] << 24 | (buf[BigHash.HASH_LENGTH + offset + 6 + 1] & 0xff) << 16 | (buf[BigHash.HASH_LENGTH + offset + 6 + 2] & 0xff) << 8 | (buf[BigHash.HASH_LENGTH + offset + 6 + 3] & 0xff);
                if (h.equals(hash) && type == isMetaDataByte && status != STATUS_DELETED) {
                    ras.seek(i * bytesPerEntry + BigHash.HASH_LENGTH + 1);
                    ras.write(STATUS_DELETED);
                    return;
                }
                if (o == 0) {
                    break;
                }
            }
        } finally {
            ras.close();
        }
    }

    /**
     * <p>Helper method to ensure that the RAS reads all of the bytes desired.</p>
     * @param buf A byte buffer to hold the data. The random access file's data will be transfered to filled this buffer.
     * @param ras Random access file for the data block.
     * @param blockFilePath The path to the file accessed by the RandomAccessFile, ras. Used for error messages.
     * @throws java.lang.Exception If any I/O errors occur, or if cannot fill the entire buffer with data from RandomAccessFile (could be corrupted data block).
     */
    static final void fillWithBytes(final byte[] buf, final RandomAccessFile ras, String blockFilePath, String description) throws Exception {
        int bytesRead = 0;
        while (bytesRead != buf.length) {
            int read = ras.read(buf, bytesRead, buf.length - bytesRead);
            if (read == -1) {
                throw new UnexpectedEndOfDataBlockException("EOF reached and expected more bytes! For data block at: " + blockFilePath + " <" + description + ">");
            }
            bytesRead += read;
        }
    }

    /**
     * <p>Compare underlying files for DataBlock instances.</p>
     * @param o
     * @return
     */
    public final int compareTo(Object o) {
        DataBlock db = (DataBlock) o;
        return filename.compareTo(db.filename);
    }

    /**
     * <p>Return the subblocks for this DataBlock.</p>
     * @return
     */
    protected final DataBlock[] getSubBlocks() {
        synchronized (DataBlock.this) {
            if (subBlocks == null) {
                subBlocks = new DataBlock[256];
            }
        }
        return subBlocks;
    }

    /**
     * <p>The data block moves itself. This synchronized method places the data block in a new DataDirectoryConfiguration.</p>
     * <p>The intended use for this method is to balance a server's data across data directories.</p>
     * @param newDDC
     * @return
     * @throws java.lang.Exception
     */
    protected final synchronized boolean moveToDataDirectoryConfiguration(DataDirectoryConfiguration newDDC) throws Exception {
        if (newDDC.equals(this.ddc)) {
            return false;
        }
        boolean moved = false;
        final File srcFile = getRegularOrMergeFile();
        final long srcBytes = srcFile.length();
        if (this.isDirectory()) {
            throw new AssertionFailedException("Trying to move data block, is a directory (not allowed): " + srcFile.getAbsolutePath());
        }
        try {
            if (!srcFile.exists()) {
                throw new AssertionFailedException("Trying to move data block, doesn't exist: " + srcFile.getAbsolutePath());
            }
            if (!newDDC.getDirectoryFile().exists() || !newDDC.getDirectoryFile().isDirectory()) {
                newDDC.getDirectoryFile().mkdirs();
                if (!newDDC.getDirectoryFile().exists()) {
                    throw new Exception("Trying to move data block from source<" + srcFile.getAbsolutePath() + "> to destination DDC, but cannot create DDC data directory: " + newDDC.getDirectoryFile().getAbsolutePath());
                }
                if (!newDDC.getDirectoryFile().isDirectory()) {
                    throw new Exception("Trying to move data block from source<" + srcFile.getAbsolutePath() + "> to destination DDC, but cannot use DDC because it is not a directory: " + newDDC.getDirectoryFile().getAbsolutePath());
                }
            }
            if (newDDC.getActualSize() >= newDDC.getSizeLimit()) {
                return false;
            }
            final String regularOrMergeFileName = this.filename + (srcFile.getName().endsWith(".merge") ? ".merge" : "");
            final File destFile = new File(newDDC.getDirectory(), regularOrMergeFileName);
            if (destFile.exists()) {
                throw new AssertionFailedException("Want to move data block <" + srcFile.getAbsolutePath() + "> to new destination<" + destFile.getAbsolutePath() + ">, but destination exists.");
            }
            destFile.getParentFile().mkdirs();
            moved = srcFile.renameTo(destFile);
            if (!moved && !destFile.exists()) {
                IOUtil.copyFile(srcFile, destFile);
                final long destBytes = destFile.length();
                if (destBytes == srcBytes) {
                    IOUtil.safeDelete(srcFile);
                    if (srcFile.exists()) {
                        throw new Exception("Could not delete source data block<" + srcFile.getAbsolutePath() + "> file after moving to destination<" + destFile.getAbsolutePath() + ">.");
                    }
                    moved = true;
                } else {
                    throw new Exception("After trying to move data block, size<" + srcBytes + "> or src data directory<" + srcFile.getAbsolutePath() + "> doesn't match size<" + destBytes + "> of destination directory<" + destFile.getAbsolutePath() + ">.");
                }
            }
            return moved;
        } finally {
            if (moved) {
                this.ddc.adjustUsedSpace(-srcBytes);
                newDDC.adjustUsedSpace(+srcBytes);
                this.ddc = newDDC;
            }
        }
    }

    /**
     * @return the maxBlockSize
     */
    public static int getMaxBlockSize() {
        return maxBlockSize;
    }

    /**
     * @param aMaxBlockSize the maxBlockSize to set
     */
    public static void setMaxBlockSize(int aMaxBlockSize) {
        maxBlockSize = aMaxBlockSize;
    }

    /**
     * @return the headersPerFile
     */
    public static int getHeadersPerFile() {
        return headersPerFile;
    }

    /**
     * @param aHeadersPerFile the headersPerFile to set
     */
    public static void setHeadersPerFile(int aHeadersPerFile) {
        headersPerFile = aHeadersPerFile;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    /**
     * 
     * @return
     */
    public static int getBytesToRead() {
        return bytesPerEntry * getHeadersPerFile();
    }
}

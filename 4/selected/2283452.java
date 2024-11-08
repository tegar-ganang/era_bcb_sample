package com.faunos.skwish.sys.tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import com.faunos.skwish.sys.BaseSegment;
import com.faunos.skwish.sys.mgr.FileConventions;

/**
 * Utility for removing the last entries in a base segment. Doing so, allows
 * entries to be re-written.
 * <p/>
 * Comes in handy when some large batch job has failed
 * late in the job and then started producing junk. (That was the first use-case.)
 *
 * @author Babak Farhang
 */
public class SegmentTruncator {

    /**
     * Truncates the entry count in a (managed or unmanaged) base segment
     * and trims the backing files to minimum size.
     * 
     *  @param entryCount
     *         the new entry count. Has no effect if greater than the current
     *         entry count
     *  @param dir
     *         the directory in which the index (offset) file and entry
     *         (content) file may be found. See file conventions below.
     *  @see BaseSegment
     *  @see BaseSegment#truncateEntryCount(long)
     *  @see FileConventions#INDEX_FILE
     *  @see FileConventions#ENTRY_FILE
     *  @see #truncateBaseSegment(File, File, int)
     */
    public static void truncateBaseSegment(File dir, int entryCount) throws IOException {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("not a directory: " + dir);
        }
        File indexFile = new File(dir, FileConventions.INDEX_FILE);
        File entryFile = new File(dir, FileConventions.ENTRY_FILE);
        truncateBaseSegment(indexFile, entryFile, entryCount);
    }

    /**
     * Truncates the entry count in a (managed or unmanaged) base segment
     * and trims the backing files to minimum size.
     * 
     *  @param entryCount
     *         the new entry count. Has no effect if greater than the current
     *         entry count
     *  @param indexFile
     *         the file in which the entry offsets are stored
     *  @param entryFile
     *         the file in which the entry contents are stored
     *
     *  @see BaseSegment
     *  @see BaseSegment#truncateEntryCount(long)
     */
    public static void truncateBaseSegment(File indexFile, File entryFile, int entryCount) throws IOException {
        FileChannel indexChannel = new RandomAccessFile(indexFile, "rw").getChannel();
        FileChannel entryChannel = new RandomAccessFile(entryFile, "rw").getChannel();
        BaseSegment baseSegment = new BaseSegment(indexChannel, entryChannel);
        baseSegment.truncateEntryCount(entryCount);
        baseSegment.close();
    }
}

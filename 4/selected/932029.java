package net.jsrb.runtime.impl.tm;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import net.jsrb.rtl.*;
import net.jsrb.util.*;
import net.jsrb.util.log4j.Logger;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

/**
 * Transaction Logger Implementation class.
 * <BR>Save transaction recovery info to disk during committing.
 * <BR>To improve performance, The basic unit of the log file is PAGE( 1024 bytes), 
 * 64 continuation pages is a GROUP. The whole log file is divided into many GROUPs.
 * <BR>GROUP is the save/flush unit.
 * <BR>GROUP is the used by TransactionLogger internally, Client using PAGE only.
 */
public class TransactionLogger {

    private static final Logger TRC_LOGGER = Logger.getLogger(TransactionLogger.class);

    private static class LogGroup {

        MappedByteBuffer mbb;

        long dirtyTime;

        volatile boolean dirty;

        volatile AtomicInteger usedPages = new AtomicInteger();

        BitSet pages;
    }

    private static final int PAGE_SIZE = 1024;

    private static final int GROUP_PAGES = 64;

    private RandomAccessFile raf;

    /**
     * Group status
     */
    private LogGroup[] groups;

    private int pageCount;

    public TransactionLogger(File logfile, int pageCount) throws IOException {
        final String METHOD = "TransactionLogger(File logfile,int pageCount)";
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|entry,logfile=" + logfile + ",pageCount=" + pageCount);
        }
        createLogFile(logfile, pageCount);
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|exit");
        }
    }

    public void clear() {
        final String METHOD = "clear()";
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|entry");
        }
        for (int i = 0; i < groups.length; i++) {
            groups[i].usedPages.set(0);
            groups[i].pages.clear();
            groups[i].mbb.putLong(0);
            saveGroup(groups[i], true);
        }
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|exit");
        }
    }

    public int alloc() {
        final String METHOD = "alloc()";
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|entry");
        }
        int result = -1;
        synchronized (this) {
            LogGroup group = groups[0];
            int groupIndex = 0;
            int pageInGroup = 0;
            int groupCount = groups.length;
            for (int i = 0; i < groupCount; i++) {
                LogGroup currGroup = groups[i];
                if (currGroup.usedPages.get() == 0) {
                    group = currGroup;
                    groupIndex = i;
                    break;
                } else if (group.usedPages.get() > currGroup.usedPages.get()) {
                    group = currGroup;
                }
            }
            if (group.usedPages.get() < GROUP_PAGES) {
                group.usedPages.incrementAndGet();
                BitSet pages = group.pages;
                for (int i = 0; i < GROUP_PAGES; i++) {
                    if (!pages.get(i)) {
                        pages.set(i);
                        pageInGroup = i;
                        break;
                    }
                }
                result = (groupIndex << 16) | pageInGroup;
            }
        }
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|exit,result=" + result);
        }
        return result;
    }

    public void free(int pageIndex) {
        final String METHOD = "free(int pageIndex)";
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|entry,pageIndex=" + pageIndex);
        }
        int groupIndex = (pageIndex >> 16) & 0XFFFF;
        int pageInGroup = (pageIndex) & 0XFFFF;
        LogGroup group = groups[groupIndex];
        group.usedPages.decrementAndGet();
        group.pages.clear(pageInGroup);
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|exit");
        }
    }

    public void putPage(int pageIndex, Buffer buffer, boolean save) {
        final String METHOD = "putPage(int pageIndex,Buffer buffer,boolean save)";
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|entry,pageIndex=" + pageIndex + ",buffer=" + buffer + ",save=" + save);
        }
        int groupIndex = (pageIndex >> 16) & 0XFFFF;
        int pageInGroup = (pageIndex) & 0XFFFF;
        LogGroup group = groups[groupIndex];
        DirectBuffer db = (DirectBuffer) group.mbb;
        long pageAddr = db.address() + PAGE_SIZE * pageInGroup;
        int pageData = buffer.remaining();
        if (pageData > PAGE_SIZE) {
            pageData = PAGE_SIZE;
        }
        if (buffer.isNativeBuffer()) {
            nbuf nbuf = (nbuf) buffer;
            string.memcpy(pageAddr, nbuf.addr() + nbuf.position(), pageData);
        } else {
            ByteArrayBuffer bab = (ByteArrayBuffer) buffer;
            NativeUtil.putBytes(pageAddr, bab.getBufferData(), bab.position(), pageData);
        }
        group.dirty = true;
        group.dirtyTime = System.currentTimeMillis();
        if (save) {
            saveGroup(group, false);
        }
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|exit");
        }
    }

    public void savePage(int pageIndex, boolean forceSave) {
        final String METHOD = "savePage(int pageIndex,boolean forceSave)";
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|entry,pageIndex=" + pageIndex + ",forceSave=" + forceSave);
        }
        int groupIndex = (pageIndex >> 16) & 0XFFFF;
        saveGroup(groups[groupIndex], forceSave);
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|exit");
        }
    }

    public int getPageCount() {
        final String METHOD = "getPageCount()";
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|entry");
        }
        int result = pageCount;
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|exit,result=" + result);
        }
        return result;
    }

    public void close() throws IOException {
        final String METHOD = "close()";
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|entry");
        }
        int groupCount = groups.length;
        for (int i = 0; i < groupCount; i++) {
            LogGroup group = groups[i];
            saveGroup(group, true);
            closeMappedBuffer(group.mbb);
            groups[i] = null;
        }
        raf.close();
        if (TRC_LOGGER.isDebugMinEnabled()) {
            TRC_LOGGER.debugMin(METHOD + "|exit");
        }
    }

    private void createLogFile(File logfile, int pageCount) throws IOException {
        final String METHOD = "createLogFile(File logfile,int pageCount)";
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|entry,logfile=" + logfile + ",pageCount=" + pageCount);
        }
        int groupCount = (pageCount / GROUP_PAGES) + 1;
        this.pageCount = groupCount * GROUP_PAGES;
        int groupSize = GROUP_PAGES * PAGE_SIZE;
        long fileSize = groupCount * groupSize;
        raf = new RandomAccessFile(logfile, "rw");
        if (fileSize != raf.length()) {
            raf.setLength(groupCount);
        }
        groups = new LogGroup[groupCount];
        for (int i = 0; i < groupCount; i++) {
            LogGroup group = new LogGroup();
            group.mbb = raf.getChannel().map(MapMode.READ_WRITE, groupSize * i, groupSize);
            group.pages = new BitSet(GROUP_PAGES);
            groups[i] = group;
        }
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|exit");
        }
    }

    private boolean saveGroup(LogGroup group, boolean forceSave) {
        final String METHOD = "saveGroup(LogGroup group,boolean forceSave)";
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|entry,group=" + group + ",forceSave=" + forceSave);
        }
        boolean result = false;
        if ((group != null) && (group.mbb != null) && (forceSave || group.dirty)) {
            group.dirty = false;
            group.dirtyTime = System.currentTimeMillis();
            result = true;
            group.mbb.force();
        }
        if (TRC_LOGGER.isDebugMidEnabled()) {
            TRC_LOGGER.debugMid(METHOD + "|exit,result=" + result);
        }
        return result;
    }

    private void closeMappedBuffer(MappedByteBuffer buffer) {
        if (buffer != null && buffer instanceof DirectBuffer) {
            Cleaner cleaner = ((DirectBuffer) buffer).cleaner();
            buffer = null;
            cleaner.clean();
        }
    }
}

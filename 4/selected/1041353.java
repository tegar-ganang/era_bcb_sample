package com.dbxml.db.common.btree;

import java.io.*;
import com.dbxml.db.core.DBException;
import com.dbxml.db.core.FaultCodes;
import com.dbxml.db.core.data.Value;
import com.dbxml.db.core.filer.FilerException;
import com.dbxml.db.core.transaction.Transaction;
import com.dbxml.db.core.transaction.TransactionException;
import com.dbxml.db.core.transaction.TransactionLog;
import com.dbxml.util.Configurable;
import com.dbxml.util.Configuration;
import com.dbxml.util.SoftHashMap;
import com.dbxml.util.dbXMLException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.nio.ByteBuffer;

/**
 * Paged is a paged file foundation that is used by both the BTree
 * class and the ValueIndexer.  It provides flexible paged I/O and
 * page caching functionality.
 */
public abstract class Paged implements Configurable {

    private static final PageFilter[] EmptyPageFilters = new PageFilter[0];

    protected static final byte UNUSED = 0;

    protected static final byte OVERFLOW = 126;

    protected static final byte DELETED = 127;

    private static List pageFilterList = new ArrayList();

    private static PageFilter[] pageFilters;

    private static int pageFilterPadding;

    private Configuration config;

    private Map pages = new SoftHashMap();

    private boolean txSupported;

    private PagedLog log;

    private File file;

    private RandomAccessFile raf;

    private FileChannel fc;

    private FileLock lock;

    private boolean opened;

    private FileHeader fileHeader;

    public Paged() {
        fileHeader = createFileHeader();
    }

    public Paged(File file) {
        this();
        setFile(file);
    }

    public void setConfig(Configuration config) throws dbXMLException {
        this.config = config;
    }

    public Configuration getConfig() {
        return config;
    }

    private static final void calculatePageFilterPadding() {
        pageFilterPadding = 0;
        for (int i = 0; pageFilters != null && i < pageFilters.length; i++) pageFilterPadding += pageFilters[i].getPadding();
    }

    /**
    * setPageFilter adds a PageFilter to the PageFilter chain.
    *
    * @param pageFilter The PageFilter
    */
    public static final void addPageFilter(PageFilter pageFilter) {
        pageFilterList.add(pageFilter);
        pageFilters = (PageFilter[]) pageFilterList.toArray(EmptyPageFilters);
        calculatePageFilterPadding();
    }

    /**
    * removePageFilter removes the PageFilter from the PageFilter chain.
    *
    * @param pageFilter The PageFilter
    */
    public static final void removePageFilter(PageFilter pageFilter) {
        pageFilterList.remove(pageFilter);
        if (!pageFilterList.isEmpty()) pageFilters = (PageFilter[]) pageFilterList.toArray(EmptyPageFilters); else pageFilters = null;
        calculatePageFilterPadding();
    }

    /**
    * listPageFilters returns a list of the PageFilters in the PageFilter
    * chain.
    *
    * @return The PageFilter list
    */
    public static final PageFilter[] listPageFilters() {
        if (pageFilters != null) return pageFilters; else return EmptyPageFilters;
    }

    /**
    * setFile sets the file object for this Paged.
    *
    * @param file The File
    */
    protected final void setFile(File file) {
        this.file = file;
    }

    /**
    * getFile returns the file object for this Paged.
    *
    * @return The File
    */
    protected final File getFile() {
        return file;
    }

    protected final void checkTransaction(Transaction tx) throws DBException {
        if (txSupported) {
            if (log == null) throw new DBException(FaultCodes.GEN_CRITICAL_ERROR, "No Transaction Log");
            if (tx == null) throw new DBException(FaultCodes.GEN_CRITICAL_ERROR, "No Transaction Context");
            tx.addTransactionLog(log);
        }
    }

    /**
    * getPage returns the page specified by pageNum.
    *
    * @param lp The Page number
    * @return The requested Page
    * @throws IOException if an Exception occurs
    */
    protected final Page getPage(Transaction tx, Long lp) throws DBException, IOException {
        checkTransaction(tx);
        Page p;
        synchronized (this) {
            p = (Page) pages.get(lp);
            if (p == null) {
                p = new Page(lp.longValue());
                pages.put(lp, p);
            }
        }
        synchronized (p) {
            p.read(tx);
        }
        return p;
    }

    /**
    * getPage returns the page specified by pageNum.
    *
    * @param pageNum The Page number
    * @return The requested Page
    * @throws IOException if an Exception occurs
    */
    protected final Page getPage(Transaction tx, long pageNum) throws DBException, IOException {
        return getPage(tx, new Long(pageNum));
    }

    /**
    * readValue reads the multi-Paged Value starting at the specified
    * Page.
    *
    * @param page The starting Page
    * @return The Value
    * @throws IOException if an Exception occurs
    */
    protected final Value readValue(Transaction tx, Page page) throws DBException, IOException {
        checkTransaction(tx);
        PageHeader sph = page.getPageHeader();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(sph.getRecordLen());
        Page p = page;
        PageHeader ph = null;
        long nextPage;
        while (true) {
            ph = p.getPageHeader();
            p.streamTo(bos);
            nextPage = ph.getNextPage();
            if (nextPage != -1) p = getPage(tx, nextPage); else break;
        }
        return new Value(bos.toByteArray());
    }

    /**
    * readValue reads the multi-Paged Value starting at the specified
    * page number.
    *
    * @param page The starting page number
    * @return The Value
    * @throws IOException if an Exception occurs
    */
    protected final Value readValue(Transaction tx, long page) throws DBException, IOException {
        return readValue(tx, getPage(tx, page));
    }

    /**
    * writeValue writes the multi-Paged Value starting at the specified
    * Page.
    *
    * @param page The starting Page
    * @param value The Value to write
    * @throws IOException if an Exception occurs
    */
    protected final void writeValue(Transaction tx, Page page, Value value) throws DBException, IOException {
        checkTransaction(tx);
        InputStream is = value.getInputStream();
        PageHeader hdr = page.getPageHeader();
        hdr.setRecordLen(value.getLength());
        page.streamFrom(is);
        while (is.available() > 0) {
            Page lpage = page;
            PageHeader lhdr = hdr;
            long np = lhdr.getNextPage();
            if (np != -1) page = getPage(tx, np); else {
                page = getFreePage(tx);
                lhdr.setNextPage(page.getPageNum());
            }
            hdr = page.getPageHeader();
            hdr.setStatus(OVERFLOW);
            page.streamFrom(is);
            lpage.write(tx);
        }
        long np = hdr.getNextPage();
        if (np != -1) unlinkPages(tx, np);
        hdr.setNextPage(-1);
        page.write(tx);
    }

    /**
    * writeValue writes the multi-Paged Value starting at the specified
    * page number.
    *
    * @param page The starting page number
    * @param value The Value to write
    * @throws IOException if an Exception occurs
    */
    protected final void writeValue(Transaction tx, long page, Value value) throws DBException, IOException {
        writeValue(tx, getPage(tx, page), value);
    }

    /**
    * unlinkPages unlinks a set of pages starting at the specified Page.
    *
    * @param page The starting Page to unlink
    * @throws IOException if an Exception occurs
    */
    protected final void unlinkPages(Transaction tx, Page page) throws DBException, IOException {
        checkTransaction(tx);
        if (page.pageNum < fileHeader.pageCount) {
            long nextPage = page.header.nextPage;
            page.header.setStatus(DELETED);
            page.header.setNextPage(-1);
            page.write(tx);
            page = nextPage != -1 ? getPage(tx, nextPage) : null;
        }
        if (page != null) {
            long firstPage = page.pageNum;
            while (page.header.nextPage != -1) page = getPage(tx, page.header.nextPage);
            long lastPage = page.pageNum;
            if (fileHeader.lastFreePage != -1) {
                Page p = getPage(tx, fileHeader.lastFreePage);
                p.header.setNextPage(firstPage);
                p.write(tx);
            }
            if (fileHeader.firstFreePage == -1) fileHeader.setFirstFreePage(firstPage);
            fileHeader.setLastFreePage(lastPage);
        }
    }

    /**
    * unlinkPages unlinks a set of pages starting at the specified
    * page number.
    *
    * @param pageNum The starting page number to unlink
    * @throws IOException if an Exception occurs
    */
    protected final void unlinkPages(Transaction tx, long pageNum) throws DBException, IOException {
        unlinkPages(tx, getPage(tx, pageNum));
    }

    /**
    * getFreePage returns the first free Page from secondary storage.
    * If no Pages are available, the file is grown as appropriate.
    *
    * @return The next free Page
    * @throws IOException if an Exception occurs
    */
    protected final Page getFreePage(Transaction tx) throws DBException, IOException {
        checkTransaction(tx);
        Page p = null;
        long pageNum = fileHeader.firstFreePage;
        if (pageNum != -1) {
            p = getPage(tx, pageNum);
            fileHeader.setFirstFreePage(p.getPageHeader().nextPage);
            if (fileHeader.firstFreePage == -1) fileHeader.setLastFreePage(-1);
        } else {
            pageNum = fileHeader.totalCount;
            fileHeader.setTotalCount(pageNum + 1);
            p = getPage(tx, pageNum);
        }
        p.header.setNextPage(-1);
        p.header.setStatus(UNUSED);
        return p;
    }

    protected final void checkOpened() throws DBException {
        if (!opened) throw new FilerException(FaultCodes.COL_COLLECTION_CLOSED, "Filer is closed");
    }

    /**
    * getFileHeader returns the FileHeader
    *
    * @return The FileHeader
    */
    public FileHeader getFileHeader() {
        return fileHeader;
    }

    public boolean exists() {
        return file.exists();
    }

    private void reset() {
        lock = null;
        fc = null;
        raf = null;
    }

    public boolean create() throws DBException {
        try {
            raf = new RandomAccessFile(file, "rw");
            fc = raf.getChannel();
            lock = fc.tryLock();
            if (lock == null) {
                System.err.println("FATAL ERROR: Cannot open '" + file.getName() + "' for exclusive access");
                System.exit(1);
            }
            fileHeader.write();
            createTransactionLog();
            openTransactionLog();
            Transaction tx = new Transaction();
            checkTransaction(tx);
            try {
                tx.commit();
            } catch (DBException e) {
                tx.cancel();
                throw e;
            }
            closeTransactionLog();
            lock.release();
            raf.close();
            fc.close();
            reset();
            return true;
        } catch (Exception e) {
            throw new FilerException(FaultCodes.GEN_CRITICAL_ERROR, "Error creating " + file.getName(), e);
        }
    }

    public boolean open() throws DBException {
        try {
            raf = new RandomAccessFile(file, "rw");
            fc = raf.getChannel();
            lock = fc.tryLock();
            if (lock == null) {
                System.err.println("FATAL ERROR: Cannot open '" + file.getName() + "' for exclusive access");
                System.exit(1);
            }
            if (exists()) {
                fileHeader.read();
                opened = true;
            } else opened = false;
            if (opened) openTransactionLog();
            return opened;
        } catch (Exception e) {
            throw new FilerException(FaultCodes.GEN_CRITICAL_ERROR, "Error opening " + file.getName(), e);
        }
    }

    public synchronized boolean close() throws DBException {
        try {
            if (opened) {
                Transaction tx = new Transaction();
                checkTransaction(tx);
                try {
                    tx.commit();
                } catch (DBException e) {
                    tx.cancel();
                    throw e;
                }
                closeTransactionLog();
                opened = false;
                lock.release();
                raf.close();
                fc.close();
                reset();
                return true;
            } else return false;
        } catch (Exception e) {
            throw new FilerException(FaultCodes.GEN_CRITICAL_ERROR, "Error closing " + file.getName(), e);
        }
    }

    public boolean isOpened() {
        return opened;
    }

    public boolean drop() throws DBException {
        try {
            close();
            dropTransactionLog();
            if (exists()) return getFile().delete(); else return true;
        } catch (Exception e) {
            throw new FilerException(FaultCodes.COL_CANNOT_DROP, "Can't drop " + file.getName(), e);
        }
    }

    public void flush(Transaction tx) throws DBException {
        checkTransaction(tx);
        boolean error = false;
        if (fileHeader.dirty) {
            try {
                fileHeader.write();
            } catch (Exception e) {
                error = true;
            }
        }
        try {
            fc.force(true);
        } catch (Exception e) {
            error = true;
        }
        if (error) throw new FilerException(FaultCodes.GEN_CRITICAL_ERROR, "Error performing flush!");
    }

    private void createTransactionLog() throws DBException {
        if (txSupported) {
            String filename = file.getPath() + ".tx";
            File logFile = new File(filename);
            log = new PagedLog(this, logFile);
            if (!log.exists()) log.create();
        }
    }

    private void openTransactionLog() throws DBException {
        if (txSupported) {
            String filename = file.getPath() + ".tx";
            File logFile = new File(filename);
            log = new PagedLog(this, logFile);
            log.playback(new PagedLogPlaybackImpl());
            log.create();
            log.open();
        }
    }

    private void closeTransactionLog() throws DBException {
        if (txSupported && log != null) log.close();
    }

    private void dropTransactionLog() throws DBException {
        if (txSupported && log != null) {
            if (log.isOpened()) log.close();
            log.drop();
        }
    }

    protected void setTransactionSupported(boolean txSupported) {
        this.txSupported = txSupported;
    }

    protected boolean isTransactionSupported() {
        return txSupported;
    }

    public TransactionLog getTransactionLog() {
        return log;
    }

    /**
    * createFileHeader must be implemented by a Paged implementation
    * in order to create an appropriate subclass instance of a FileHeader.
    *
    * @return a new FileHeader
    */
    public abstract FileHeader createFileHeader();

    /**
    * createFileHeader must be implemented by a Paged implementation
    * in order to create an appropriate subclass instance of a FileHeader.
    *
    * @param read If true, reads the FileHeader from disk
    * @return a new FileHeader
    * @throws IOException if an exception occurs
    */
    public abstract FileHeader createFileHeader(boolean read) throws IOException;

    /**
    * createFileHeader must be implemented by a Paged implementation
    * in order to create an appropriate subclass instance of a FileHeader.
    *
    * @param pageCount The number of pages to allocate for primary storage
    * @return a new FileHeader
    */
    public abstract FileHeader createFileHeader(long pageCount);

    /**
    * createFileHeader must be implemented by a Paged implementation
    * in order to create an appropriate subclass instance of a FileHeader.
    *
    * @param pageCount The number of pages to allocate for primary storage
    * @param pageSize The size of a Page (should be a multiple of a FS block)
    * @return a new FileHeader
    */
    public abstract FileHeader createFileHeader(long pageCount, int pageSize);

    /**
    * createPageHeader must be implemented by a Paged implementation
    * in order to create an appropriate subclass instance of a PageHeader.
    *
    * @return a new PageHeader
    */
    public abstract PageHeader createPageHeader();

    public static Value[] insertArrayValue(Value[] vals, Value val, int idx) {
        Value[] newVals = new Value[vals.length + 1];
        if (idx > 0) System.arraycopy(vals, 0, newVals, 0, idx);
        newVals[idx] = val;
        if (idx < vals.length) System.arraycopy(vals, idx, newVals, idx + 1, vals.length - idx);
        return newVals;
    }

    public static Value[] deleteArrayValue(Value[] vals, int idx) {
        Value[] newVals = new Value[vals.length - 1];
        if (idx > 0) System.arraycopy(vals, 0, newVals, 0, idx);
        if (idx < newVals.length) System.arraycopy(vals, idx + 1, newVals, idx, newVals.length - idx);
        return newVals;
    }

    /**
    * FileHeader
    */
    public abstract class FileHeader {

        private boolean dirty;

        private int workSize;

        private short headerSize;

        private int pageSize;

        private long pageCount;

        private long totalCount;

        private long firstFreePage = -1;

        private long lastFreePage = -1;

        private byte pageHeaderSize = 48;

        private long recordCount;

        public FileHeader() {
            this(1024);
        }

        public FileHeader(long pageCount) {
            this(pageCount, 4096);
        }

        public FileHeader(long pageCount, int pageSize) {
            this.pageSize = pageSize;
            this.pageCount = pageCount;
            totalCount = pageCount;
            headerSize = (short) pageSize;
            calculateWorkSize();
        }

        public FileHeader(boolean read) throws IOException {
            if (read) read();
        }

        public final synchronized void read() throws IOException {
            synchronized (raf) {
                raf.seek(0);
                read(raf);
            }
            calculateWorkSize();
        }

        public synchronized void read(RandomAccessFile raf) throws IOException {
            headerSize = raf.readShort();
            pageSize = raf.readInt();
            pageCount = raf.readLong();
            totalCount = raf.readLong();
            firstFreePage = raf.readLong();
            lastFreePage = raf.readLong();
            pageHeaderSize = raf.readByte();
            recordCount = raf.readLong();
        }

        public final synchronized void write() throws IOException {
            if (!dirty) return;
            synchronized (raf) {
                raf.seek(0);
                write(raf);
            }
            dirty = false;
        }

        public synchronized void write(RandomAccessFile raf) throws IOException {
            raf.writeShort(headerSize);
            raf.writeInt(pageSize);
            raf.writeLong(pageCount);
            raf.writeLong(totalCount);
            raf.writeLong(firstFreePage);
            raf.writeLong(lastFreePage);
            raf.writeByte(pageHeaderSize);
            raf.writeLong(recordCount);
        }

        public final synchronized void setDirty() {
            dirty = true;
        }

        public final synchronized boolean isDirty() {
            return dirty;
        }

        /**
       * The size of the FileHeader.  Usually 1 OS Page
       *
       * @param headerSize The size of the FileHeader
       */
        public final synchronized void setHeaderSize(short headerSize) {
            this.headerSize = headerSize;
            dirty = true;
        }

        /**
       * The size of the FileHeader.  Usually 1 OS Page
       *
       * @return The size of the FileHeader
       */
        public final synchronized short getHeaderSize() {
            return headerSize;
        }

        /**
       * The size of a page.  Usually a multiple of a FS block
       *
       * @param pageSize The size of a page
       */
        public final synchronized void setPageSize(int pageSize) {
            this.pageSize = pageSize;
            calculateWorkSize();
            dirty = true;
        }

        /**
       * The size of a page.  Usually a multiple of a FS block
       *
       * @return The size of a page
       */
        public final synchronized int getPageSize() {
            return pageSize;
        }

        /**
       * The number of pages in primary storage
       *
       * @param pageCount The number of pages
       */
        public final synchronized void setPageCount(long pageCount) {
            this.pageCount = pageCount;
            dirty = true;
        }

        /**
       * The number of pages in primary storage
       *
       * @return The number of pages
       */
        public final synchronized long getPageCount() {
            return pageCount;
        }

        /**
       * The number of total pages in the file
       *
       * @param totalCount The number of total pages
       */
        public final synchronized void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
            dirty = true;
        }

        /**
       * The number of total pages in the file
       *
       * @return The number of total pages
       */
        public final synchronized long getTotalCount() {
            return totalCount;
        }

        /**
       * The first free page in unused secondary space
       *
       * @param firstFreePage The first free page
       */
        public final synchronized void setFirstFreePage(long firstFreePage) {
            this.firstFreePage = firstFreePage;
            dirty = true;
        }

        /**
       * The first free page in unused secondary space
       *
       * @return The first free page
       */
        public final synchronized long getFirstFreePage() {
            return firstFreePage;
        }

        /**
       * The last free page in unused secondary space
       *
       * @param lastFreePage The last free page
       */
        public final synchronized void setLastFreePage(long lastFreePage) {
            this.lastFreePage = lastFreePage;
            dirty = true;
        }

        /**
       * The last free page in unused secondary space
       *
       * @return The last free page
       */
        public final synchronized long getLastFreePage() {
            return lastFreePage;
        }

        /**
       * The size of a page header. The value 64 is sufficient.
       *
       * @param pageHeaderSize The size of the page header
       */
        public final synchronized void setPageHeaderSize(byte pageHeaderSize) {
            this.pageHeaderSize = pageHeaderSize;
            calculateWorkSize();
            dirty = true;
        }

        /**
       * The size of a page header. The value 64 is sufficient.
       *
       * @return The size of the page header
       */
        public final synchronized byte getPageHeaderSize() {
            return pageHeaderSize;
        }

        /**
       * The number of records being managed by the file (not pages)
       *
       * @param recordCount The record count
       */
        public final synchronized void setRecordCount(long recordCount) {
            this.recordCount = recordCount;
            dirty = true;
        }

        /**
       * Increment the number of records being managed by the file
       */
        public final synchronized void incRecordCount() {
            recordCount++;
            dirty = true;
        }

        /**
       * Decrement the number of records being managed by the file
       */
        public final synchronized void decRecordCount() {
            recordCount--;
            dirty = true;
        }

        /**
       * The number of records being managed by the file (not pages)
       *
       * @return The record count
       */
        public final synchronized long getRecordCount() {
            return recordCount;
        }

        private synchronized void calculateWorkSize() {
            workSize = pageSize - (pageHeaderSize + pageFilterPadding);
        }

        public final synchronized int getWorkSize() {
            return workSize;
        }
    }

    /**
    * PageHeader
    */
    public abstract class PageHeader {

        private boolean dirty;

        private byte status = UNUSED;

        private int dataLen;

        private int decodedLen;

        private int recordLen;

        private long nextPage = -1;

        public PageHeader() {
        }

        public PageHeader(ByteBuffer buf) throws IOException {
            read(buf);
        }

        public synchronized void read(ByteBuffer buf) throws IOException {
            status = buf.get();
            dirty = false;
            if (status == UNUSED) return;
            dataLen = buf.getInt();
            decodedLen = buf.getInt();
            recordLen = buf.getInt();
            nextPage = buf.getLong();
        }

        public synchronized void write(ByteBuffer buf) throws IOException {
            dirty = false;
            buf.put(status);
            buf.putInt(dataLen);
            buf.putInt(decodedLen);
            buf.putInt(recordLen);
            buf.putLong(nextPage);
        }

        public final synchronized boolean isDirty() {
            return dirty;
        }

        public final synchronized void setDirty() {
            dirty = true;
        }

        /** The status of this page (UNUSED, RECORD, DELETED, etc...) */
        public final synchronized void setStatus(byte status) {
            this.status = status;
            dirty = true;
        }

        /** The status of this page (UNUSED, RECORD, DELETED, etc...) */
        public final synchronized byte getStatus() {
            return status;
        }

        /** The length of the Data */
        public final synchronized void setDecodedLen(int decodedLen) {
            this.decodedLen = decodedLen;
            dirty = true;
        }

        /** The length of the Data */
        public final synchronized int getDecodedLen() {
            return decodedLen;
        }

        /** The length of the Data */
        public final synchronized void setDataLen(int dataLen) {
            this.dataLen = dataLen;
            dirty = true;
        }

        /** The length of the Data */
        public final synchronized int getDataLen() {
            return dataLen;
        }

        /** The length of the Record's value */
        public synchronized void setRecordLen(int recordLen) {
            this.recordLen = recordLen;
            dirty = true;
        }

        /** The length of the Record's value */
        public final synchronized int getRecordLen() {
            return recordLen;
        }

        /** The next page for this Record (if overflowed) */
        public final synchronized void setNextPage(long nextPage) {
            this.nextPage = nextPage;
            dirty = true;
        }

        /** The next page for this Record (if overflowed) */
        public final synchronized long getNextPage() {
            return nextPage;
        }
    }

    /**
    * Page
    */
    public final class Page implements Comparable {

        /** This page number */
        private long pageNum;

        /** The data for this page */
        private ByteBuffer data;

        private ByteBuffer oldData;

        /** The Header for this Page */
        private PageHeader header = createPageHeader();

        /** The position (relative) of the Data in the data array */
        private int dataPos;

        /** The offset into the file that this page starts */
        private long offset;

        /** Whether or not the page needs to be forced to disk */
        private boolean dirty;

        public Page() {
        }

        public Page(long pageNum) throws IOException {
            this();
            setPageNum(pageNum);
        }

        public final synchronized void read(Transaction tx) throws DBException, IOException {
            checkTransaction(tx);
            if (data == null) {
                byte[] b = new byte[fileHeader.pageSize];
                synchronized (raf) {
                    raf.seek(offset);
                    raf.read(b);
                }
                data = ByteBuffer.wrap(b);
                if (txSupported) {
                    byte[] ob = new byte[fileHeader.pageSize];
                    System.arraycopy(b, 0, ob, 0, fileHeader.pageSize);
                    oldData = ByteBuffer.wrap(ob);
                }
                header.read(data);
                dataPos = fileHeader.pageHeaderSize;
            }
        }

        public final synchronized void write(Transaction tx) throws DBException, IOException {
            checkTransaction(tx);
            if (txSupported) {
                byte[] ob = oldData.array();
                log.write(tx, offset, ob);
            }
            data.rewind();
            header.write(data);
            synchronized (raf) {
                raf.seek(offset);
                raf.write(data.array());
            }
            if (txSupported) {
                byte[] b = data.array();
                byte[] ob = new byte[b.length];
                System.arraycopy(b, 0, ob, 0, b.length);
                oldData = ByteBuffer.wrap(ob);
            }
        }

        public synchronized void setPageNum(long pageNum) {
            this.pageNum = pageNum;
            offset = fileHeader.headerSize + (pageNum * fileHeader.pageSize);
        }

        public synchronized long getPageNum() {
            return pageNum;
        }

        public synchronized PageHeader getPageHeader() {
            return header;
        }

        public synchronized void streamTo(OutputStream os) throws IOException {
            if (header.dataLen > 0) {
                byte[] b = new byte[header.dataLen];
                data.position(dataPos);
                data.get(b);
                if (pageFilters != null) {
                    for (int i = pageFilters.length - 1; i >= 0; i--) b = pageFilters[i].decode(b, fileHeader.workSize);
                }
                os.write(b, 0, header.decodedLen);
            }
        }

        public synchronized void streamFrom(InputStream is) throws IOException {
            int avail = is.available();
            header.dataLen = fileHeader.workSize;
            if (avail < header.dataLen) header.dataLen = avail;
            header.decodedLen = header.dataLen;
            if (header.dataLen > 0) {
                byte[] b = new byte[header.dataLen];
                is.read(b);
                if (pageFilters != null) {
                    for (int i = 0; i < pageFilters.length; i++) b = pageFilters[i].encode(b, fileHeader.workSize);
                }
                header.dataLen = b.length;
                data.position(dataPos);
                data.put(b);
            }
        }

        public synchronized int compareTo(Object o) {
            return (int) (pageNum - ((Page) o).pageNum);
        }
    }

    /**
    * PagedLogPlaybackImpl
    */
    private class PagedLogPlaybackImpl implements PagedLogPlayback {

        private Map transactions;

        public void beginPlayback() {
            transactions = new HashMap();
        }

        public void start(long transactionID) throws TransactionException {
        }

        public void commit(long transactionID) throws TransactionException {
            Long id = new Long(transactionID);
            transactions.remove(id);
        }

        public void cancel(long transactionID) throws TransactionException {
            Long id = new Long(transactionID);
            List list = (List) transactions.remove(id);
            if (list != null) {
                try {
                    Iterator iter = list.iterator();
                    while (iter.hasNext()) {
                        PlaybackEntry entry = (PlaybackEntry) iter.next();
                        synchronized (raf) {
                            raf.seek(entry.offset);
                            raf.write(entry.buffer.array());
                        }
                    }
                } catch (IOException e) {
                    throw new TransactionException(FaultCodes.COL_CANNOT_STORE, "Error restoring from Transaction Log", e);
                }
            }
        }

        public void write(long transactionID, long offset, ByteBuffer buffer) throws TransactionException {
            Long id = new Long(transactionID);
            List list = (List) transactions.get(id);
            if (list == null) {
                list = new ArrayList();
                transactions.put(id, list);
            }
            list.add(new PlaybackEntry(offset, buffer));
        }

        public void checkpoint() throws TransactionException {
            transactions.clear();
        }

        public void endPlayback() {
            transactions.clear();
        }
    }

    /**
    * PlaybackEntry
    */
    private class PlaybackEntry {

        public long offset;

        public ByteBuffer buffer;

        public PlaybackEntry(long offset, ByteBuffer buffer) {
            this.offset = offset;
            this.buffer = buffer;
        }
    }
}

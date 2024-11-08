package xbird.storage.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import xbird.storage.DbException;
import xbird.util.concurrent.reference.ReferenceMap;
import xbird.util.concurrent.reference.ReferenceType;
import xbird.util.io.FastMultiByteArrayOutputStream;

/**
 * 
 * <DIV lang="en"></DIV>
 * <DIV lang="ja"></DIV>
 * 
 * @author Makoto YUI (yuin405+xbird@gmail.com)
 */
@NotThreadSafe
public abstract class Paged {

    private static final Log LOG = LogFactory.getLog(Paged.class);

    public static final int DEFAULT_PAGESIZE = 1024 * 4;

    protected static final byte UNUSED = 0;

    protected static final byte OVERFLOW = 126;

    /** Page ID of non-existent page */
    protected static final int NO_PAGE = -1;

    private final Map<Long, Reference<Page>> _pages = new ReferenceMap<Long, Reference<Page>>(ReferenceType.WEAK, ReferenceType.SOFT, 64);

    private final FileHeader _fileHeader;

    protected final File _file;

    private boolean _opened = false;

    private RandomAccessFile _raf = null;

    private FileChannel _fc = null;

    public Paged(File file) {
        this(file, DEFAULT_PAGESIZE);
    }

    public Paged(File file, int pageSize) {
        this._fileHeader = createFileHeader(pageSize);
        this._file = file;
    }

    public File getFile() {
        return _file;
    }

    /** create index resources and close it. */
    public boolean create() throws DbException {
        return create(true);
    }

    public boolean create(boolean close) throws DbException {
        ensureResourceOpen();
        try {
            _fileHeader.write();
        } catch (IOException e) {
            throw new DbException(e);
        }
        if (close) {
            close();
        } else {
            this._opened = true;
        }
        return true;
    }

    public boolean open() throws DbException {
        ensureResourceOpen();
        if (exists()) {
            try {
                _fileHeader.read();
            } catch (IOException e) {
                throw new DbException(e);
            }
            this._opened = true;
            return true;
        } else {
            this._opened = false;
            return false;
        }
    }

    protected final RandomAccessFile ensureResourceOpen() throws DbException {
        if (_raf == null) {
            try {
                this._raf = new RandomAccessFile(_file, "rw");
            } catch (FileNotFoundException e) {
                throw new DbException(e);
            }
        }
        if (_fc == null) {
            this._fc = _raf.getChannel();
        }
        return _raf;
    }

    public boolean close() throws DbException {
        if (_opened) {
            this._opened = false;
            try {
                _raf.close();
                _fc.close();
            } catch (IOException e) {
                throw new DbException(e);
            }
            reset();
            return true;
        } else {
            return false;
        }
    }

    protected final void checkOpened() throws DbException {
        if (!_opened) {
            throw new DbException("Not opened");
        }
    }

    private final void reset() {
        this._raf = null;
        this._fc = null;
    }

    public boolean drop() throws DbException {
        close();
        if (exists()) {
            return getFile().delete();
        } else {
            return true;
        }
    }

    public final boolean exists() {
        return _file.exists();
    }

    public void flush() throws DbException {
        try {
            if (_fileHeader._fhDirty) {
                _fileHeader.write();
            }
            _fc.force(true);
        } catch (IOException e) {
            throw new DbException(e);
        }
    }

    /**
     * createFileHeader must be implemented by a Paged implementation
     * in order to create an appropriate subclass instance of a FileHeader.
     *
     * @return a new FileHeader
     */
    protected abstract FileHeader createFileHeader(int pageSize);

    /**
     * createPageHeader must be implemented by a Paged implementation
     * in order to create an appropriate subclass instance of a PageHeader.
     */
    protected abstract PageHeader createPageHeader();

    /**
     * getPage returns the page specified by pageNum.
     */
    protected final Page getPage(long pageNum) throws DbException {
        Page p = null;
        Reference<Page> ref = _pages.get(pageNum);
        if (ref != null) {
            p = ref.get();
        }
        if (p == null) {
            p = new Page(pageNum);
            try {
                p.read();
            } catch (IOException e) {
                throw new DbException(e);
            }
            _pages.put(pageNum, new WeakReference<Page>(p));
        }
        return p;
    }

    /**
     * getFreePage returns the first free Page from secondary storage.
     * If no Pages are available, the file is grown as appropriate.
     */
    protected final Page getFreePage() throws DbException {
        Page p = null;
        if (_fileHeader._firstFreePage != NO_PAGE) {
            p = getPage(_fileHeader._firstFreePage);
            _fileHeader.setFirstFreePage(p._pageHeader._nextPage);
            if (_fileHeader._firstFreePage == NO_PAGE) {
                _fileHeader.setLastFreePage(NO_PAGE);
            }
        }
        if (p == null) {
            p = getPage(_fileHeader.incrTotalPageCount());
        }
        p.initPage();
        return p;
    }

    /**
     * unlinkPages unlinks a set of pages starting at the specified
     * page number.
     */
    protected final void unlinkPages(long pageNum) throws DbException {
        unlinkPages(getPage(pageNum));
    }

    /**
     * unlinkPages unlinks a set of pages starting at the specified Page.
     */
    protected final void unlinkPages(Page page) throws DbException {
        Page nextPage = page;
        if (nextPage != null) {
            long firstPage = nextPage._pageNum;
            long nextPageNum = nextPage.getPageHeader().getNextPage();
            while (nextPageNum != NO_PAGE) {
                nextPage = getPage(nextPageNum);
                nextPageNum = nextPage.getPageHeader().getNextPage();
            }
            long lastPage = nextPage.getPageNum();
            if (_fileHeader._lastFreePage != NO_PAGE) {
                Page p = getPage(_fileHeader._lastFreePage);
                p._pageHeader.setNextPage(firstPage);
                p.write();
            }
            if (_fileHeader._firstFreePage == NO_PAGE) {
                _fileHeader.setFirstFreePage(firstPage);
            }
            _fileHeader.setLastFreePage(lastPage);
        }
    }

    /**
     * writeValue writes the multi-Paged Value starting at the specified
     * Page.
     *
     * @param page The starting Page
     * @param value The Value to write
     */
    public final void writeValue(Page page, Value value) throws DbException {
        InputStream is = value.getInputStream();
        PageHeader hdr = page.getPageHeader();
        hdr.setRecordLength(value.getLength());
        try {
            page.readData(is);
        } catch (IOException e) {
            throw new DbException(e);
        }
        Page lastPage = page;
        while (true) {
            final int available;
            try {
                available = is.available();
            } catch (IOException e) {
                throw new DbException(e);
            }
            if (available == 0) {
                break;
            }
            LOG.debug("page overflowed");
            Page lpage = lastPage;
            PageHeader lhdr = hdr;
            long np = lhdr.getNextPage();
            if (np != NO_PAGE) {
                lastPage = getPage(np);
            } else {
                lastPage = getFreePage();
                lhdr.setNextPage(lastPage.getPageNum());
            }
            hdr = lastPage.getPageHeader();
            hdr.setStatus(OVERFLOW);
            try {
                lastPage.readData(is);
            } catch (IOException e) {
                throw new DbException(e);
            }
            lpage.write();
        }
        long np = hdr.getNextPage();
        if (np != NO_PAGE) {
            unlinkPages(np);
        }
        hdr.setNextPage(NO_PAGE);
        lastPage.write();
    }

    /**
     * writeValue writes the multi-Paged Value starting at the specified
     * page number.
     *
     * @param page The starting page number
     * @param value The Value to write
     */
    public final void writeValue(long page, Value value) throws DbException {
        writeValue(getPage(page), value);
    }

    @Deprecated
    public final long writeValue(Value value) throws DbException {
        Page p = getFreePage();
        writeValue(p, value);
        return p.getPageNum();
    }

    /**
     * readValue reads the multi-Paged Value starting at the specified
     * Page.
     *
     * @param page The starting Page
     * @return The Value
     */
    public final Value readValue(Page page) throws DbException {
        PageHeader sph = page.getPageHeader();
        FastMultiByteArrayOutputStream bos = new FastMultiByteArrayOutputStream(sph.getRecordLength());
        Page p = page;
        while (true) {
            try {
                p.writeData(bos);
            } catch (IOException e) {
                throw new DbException(e);
            }
            PageHeader ph = p.getPageHeader();
            long nextPage = ph.getNextPage();
            if (nextPage == NO_PAGE) {
                break;
            }
            p = getPage(nextPage);
        }
        return new Value(bos.toByteArray());
    }

    /**
     * readValue reads the multi-Paged Value starting at the specified
     * page number.
     *
     * @param page The starting page number
     * @return The Value
     */
    @Deprecated
    public final Value readValue(long page) throws DbException {
        return readValue(getPage(page));
    }

    protected FileHeader getFileHeader() {
        return _fileHeader;
    }

    public abstract class FileHeader {

        private boolean _fhDirty = true;

        private int _workSize;

        /** The size of the <code>FileHeader</code>. Usually 1 OS page (4096 byte). */
        private short _fhSize;

        /** The size of a <code>Page</code> */
        private int _pageSize;

        /** The number of total pages in the file */
        private long _totalPageCount;

        /** The first free page in unused secondary space */
        private long _firstFreePage = NO_PAGE;

        /** The last free page in unused secondary space */
        private long _lastFreePage = NO_PAGE;

        /** The size of the <code>PageHeader</code> */
        private byte _pageHeaderSize = PageHeader.DEFAULT_PAGE_HEADER_SIZE;

        public FileHeader(int pageSize) {
            this._pageSize = pageSize;
            this._fhSize = (short) 4096;
            this._workSize = calculateWorkSize();
        }

        public final void write() throws IOException {
            if (!_fhDirty) {
                return;
            }
            _raf.seek(0);
            write(_raf);
            if (LOG.isDebugEnabled()) {
                LOG.debug("wrote file header");
            }
            this._fhDirty = false;
        }

        protected void write(RandomAccessFile raf) throws IOException {
            raf.writeShort(_fhSize);
            raf.writeInt(_pageSize);
            raf.writeLong(_totalPageCount);
            raf.writeLong(_firstFreePage);
            raf.writeLong(_lastFreePage);
            raf.writeByte(_pageHeaderSize);
        }

        public final void read() throws IOException {
            _raf.seek(0);
            read(_raf);
            this._workSize = calculateWorkSize();
        }

        protected void read(RandomAccessFile raf) throws IOException {
            this._fhSize = raf.readShort();
            this._pageSize = raf.readInt();
            this._totalPageCount = raf.readLong();
            this._firstFreePage = raf.readLong();
            this._lastFreePage = raf.readLong();
            this._pageHeaderSize = raf.readByte();
        }

        public final void setFirstFreePage(long page) {
            this._firstFreePage = page;
            this._fhDirty = true;
        }

        public long getFirstFreePage() {
            return _firstFreePage;
        }

        public final void setLastFreePage(long page) {
            this._lastFreePage = page;
            this._fhDirty = true;
        }

        public long getLastFreePage() {
            return _lastFreePage;
        }

        public final long incrTotalPageCount() {
            this._fhDirty = true;
            return _totalPageCount++;
        }

        public final void setDirty(boolean dirty) {
            this._fhDirty = dirty;
        }

        public void setTotalPageCount(long pageCount) {
            this._fhDirty = true;
            this._totalPageCount = pageCount;
        }

        public final long getTotalPageCount() {
            return _totalPageCount;
        }

        public final int getPageSize() {
            return _pageSize;
        }

        public final int getWorkSize() {
            return _workSize;
        }

        private final int calculateWorkSize() {
            return _pageSize - _pageHeaderSize;
        }
    }

    public abstract static class PageHeader {

        public static final int DEFAULT_PAGE_HEADER_SIZE = 127;

        private byte _status = UNUSED;

        private int _dataLen;

        private int _recordLen;

        private long _nextPage = NO_PAGE;

        public PageHeader() {
        }

        public PageHeader(ByteBuffer buf) {
            read(buf);
        }

        public void read(ByteBuffer buf) {
            this._status = buf.get();
            if (_status == UNUSED) {
                return;
            }
            this._dataLen = buf.getInt();
            this._recordLen = buf.getInt();
            this._nextPage = buf.getLong();
        }

        public void write(ByteBuffer buf) {
            buf.put(_status);
            buf.putInt(_dataLen);
            buf.putInt(_recordLen);
            buf.putLong(_nextPage);
        }

        /** The status of this page (UNUSED, RECORD, DELETED, etc...) */
        public final void setStatus(byte status) {
            this._status = status;
        }

        /** The status of this page (UNUSED, RECORD, DELETED, etc...) */
        public final byte getStatus() {
            return _status;
        }

        /** The next page for this Record (if overflowed) */
        public final void setNextPage(long nextPage) {
            this._nextPage = nextPage;
        }

        /** The next page for this Record (if overflowed) */
        public final long getNextPage() {
            return _nextPage;
        }

        /** The length of the Data */
        public final void setDataLength(int dataLen) {
            this._dataLen = dataLen;
        }

        public final int getDataLength() {
            return _dataLen;
        }

        public final void setRecordLength(int length) {
            this._recordLen = length;
        }

        public final int getRecordLength() {
            return _recordLen;
        }
    }

    public final class Page implements Comparable<Page> {

        private final long _pageNum;

        private final PageHeader _pageHeader;

        /** The offset into the file that this page starts */
        private final long _pageOffset;

        /** The data for this page */
        private ByteBuffer _pageData = null;

        /** The position (relative) of the Data in the data array */
        private int _dataPos;

        public Page(long pageNum) {
            this._pageNum = pageNum;
            this._pageHeader = createPageHeader();
            this._pageOffset = _fileHeader._fhSize + (pageNum * _fileHeader._pageSize);
        }

        public synchronized void read() throws IOException {
            if (_pageData == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("read in page#" + _pageNum + " from page offset " + _pageOffset);
                }
                byte[] buf = new byte[_fileHeader._pageSize];
                _raf.seek(_pageOffset);
                _raf.read(buf);
                this._pageData = ByteBuffer.wrap(buf);
                _pageHeader.read(_pageData);
                this._dataPos = _fileHeader._pageHeaderSize;
            }
        }

        public synchronized void write() throws DbException {
            _pageData.rewind();
            _pageHeader.write(_pageData);
            try {
                _raf.seek(_pageOffset);
                _raf.write(_pageData.array());
            } catch (IOException e) {
                throw new DbException(e);
            }
        }

        /**
         * Flushes content of the dirty page into the file
         */
        public void flush() throws IOException {
            if (LOG.isDebugEnabled()) {
                LOG.debug("write out page#" + _pageNum + " to page offset " + _pageOffset);
            }
            _raf.seek(_pageOffset);
            _raf.write(_pageData.array());
        }

        public void writeData(OutputStream os) throws IOException {
            if (_pageHeader._dataLen > 0) {
                byte[] b = new byte[_pageHeader._dataLen];
                _pageData.position(_dataPos);
                _pageData.get(b);
                os.write(b);
            }
        }

        public void readData(InputStream is) throws IOException {
            int avail = is.available();
            int datalen = _fileHeader._workSize;
            if (avail < datalen) {
                datalen = avail;
            }
            _pageHeader.setDataLength(datalen);
            if (datalen > 0) {
                byte[] b = new byte[datalen];
                is.read(b);
                _pageData.position(getDataPos());
                _pageData.put(b);
            }
        }

        public PageHeader getPageHeader() {
            return _pageHeader;
        }

        public long getPageNum() {
            return _pageNum;
        }

        private int getDataPos() {
            return _dataPos;
        }

        protected void initPage() {
            _pageHeader.setNextPage(NO_PAGE);
            _pageHeader.setStatus(UNUSED);
        }

        public int compareTo(Page other) {
            return (int) (_pageNum - other._pageNum);
        }

        @Override
        public String toString() {
            return "page#" + _pageNum;
        }
    }
}

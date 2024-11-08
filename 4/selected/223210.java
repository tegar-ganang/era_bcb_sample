package net.sf.joafip.file.service;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.java.util.PLinkedTreeSet;

/**
 * Instances of this class support both reading and writing to a random access
 * file, using a read and write cache between {@link IRandomAccessFile}
 * delegation<br>
 * this is a cache proxy for {@link IRandomAccessFile} implementation<br>
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
public class RandomAccessFileReadWriteCache_old extends AbstractRandomAccessFile {

    /** maximum number of pages on read cache */
    private final int maxPage;

    /** cache page size */
    private final int pageSize;

    /** binary mask to obtains first byte position of page from position in file */
    private final long pagePositionMask;

    /** binary mask to obtains offset in page from position in file */
    private final long inPageMask;

    /** for random read write file access */
    private final IRandomAccessFile randomAccessFileDelegate;

    /** read cache is a map of pages */
    private final Map<Long, byte[]> readCacheMap = new TreeMap<Long, byte[]>();

    private final Set<Long> pageSet = new PLinkedTreeSet<Long>();

    /** write cache is a map of pages, can not be cleared until write in file */
    private final Map<Long, byte[]> writeCacheMap = new TreeMap<Long, byte[]>();

    /** current position in file */
    private long positionInFile = 0;

    /** true if use read cache */
    private boolean haveReadCache;

    /**
	 * default constructor initialize to 250 pages of 4Kbyte<br>
	 * for test purpose<br>
	 * 
	 * @param file
	 *            the file to use for read/write operations
	 */
    public RandomAccessFileReadWriteCache_old(final IRandomAccessFile randomAccessFile) {
        super();
        this.randomAccessFileDelegate = randomAccessFile;
        pageSize = 0x1fff + 1;
        inPageMask = 0x1fff;
        pagePositionMask = 0xffffffffffffE000L;
        maxPage = 250;
        this.haveReadCache = true;
        _log.warn("use default cache size of " + (maxPage * pageSize) + " bytes, " + maxPage + " pages, " + pageSize + " page size");
    }

    /**
	 * construction setting the maximum number of pages for read cache and
	 * setting the page size<br>
	 * 
	 * @param file
	 *            the file to use for read/write operations
	 * @param pageSize
	 *            page size ( number of byte ), must be greater or equals to
	 *            1024
	 * @param maxPage
	 *            maximum number of page for read cache
	 */
    public RandomAccessFileReadWriteCache_old(final IRandomAccessFile randomAccessFile, final int pageSize, final int maxPage) {
        super();
        if (pageSize < 256) {
            throw new IllegalArgumentException("page size must be greater or equals to 256");
        }
        this.randomAccessFileDelegate = randomAccessFile;
        final int bitForPageSize = (int) (Math.log(pageSize) / Math.log(2));
        this.pageSize = 1 << bitForPageSize;
        inPageMask = this.pageSize - 1;
        pagePositionMask = ~inPageMask;
        this.maxPage = ((maxPage * pageSize) / this.pageSize) + 1;
        this.haveReadCache = true;
        if (this.pageSize != pageSize || this.maxPage != maxPage) {
            _log.warn("use cache size of " + (this.maxPage * this.pageSize) + " bytes, " + this.maxPage + " pages, " + this.pageSize + " page size. was configured for: " + (maxPage * pageSize) + " cache size, " + maxPage + " pages, " + pageSize + " page size");
        } else {
            _log.info("cache size of " + (maxPage * pageSize) + " bytes, " + maxPage + " pages, " + pageSize + " page size");
        }
    }

    public RandomAccessFileReadWriteCache_old(final IRandomAccessFile randomAccessFile, final int pageSize, final int maxPage, final boolean haveReadCache) {
        this(randomAccessFile, pageSize, maxPage);
        this.haveReadCache = haveReadCache;
    }

    public void openImpl() throws IOException {
        randomAccessFileDelegate.open();
        positionInFile = 0;
    }

    public void closeImpl() throws IOException {
        writeModified();
        randomAccessFileDelegate.close();
    }

    public void flushImpl() throws IOException {
        writeModified();
        randomAccessFileDelegate.flush();
    }

    private void writeModified() throws IOException {
        if (_log.isDebugEnabled()) {
            _log.debug("to write=" + writeCacheMap.size() + ", readed=" + readCacheMap.size() + " " + (new Date()));
        }
        final Iterator<Long> iterator = writeCacheMap.keySet().iterator();
        long previousPagePositionInfile = Long.MIN_VALUE;
        while (iterator.hasNext()) {
            final long pagePositionInfile = iterator.next();
            if (previousPagePositionInfile + pageSize != pagePositionInfile) {
                randomAccessFileDelegate.seek(pagePositionInfile);
            }
            final byte[] page = writeCacheMap.get(pagePositionInfile);
            if (page.length != pageSize) {
                throw new IOException("data size is " + page.length + " for " + pageSize + " expected");
            }
            randomAccessFileDelegate.write(page);
            if (haveReadCache) {
                readCacheMap.put(pagePositionInfile, page);
            } else {
                pageSet.remove(pagePositionInfile);
            }
            previousPagePositionInfile = pagePositionInfile;
        }
        writeCacheMap.clear();
        if (_log.isDebugEnabled()) {
            _log.debug("readed=" + readCacheMap.size() + " " + (new Date()));
        }
    }

    public void deleteIfExistsImpl() throws IOException {
        clearCache();
        randomAccessFileDelegate.deleteIfExists();
    }

    public void clearCache() {
        readCacheMap.clear();
        writeCacheMap.clear();
        pageSet.clear();
    }

    public void seekImpl(final long positionInFile) throws IOException {
        this.positionInFile = positionInFile;
    }

    @Override
    protected long currentPositionInFileImpl() throws IOException {
        return positionInFile;
    }

    public int readImpl(final byte[] data) throws IOException {
        final int length = data.length;
        int readed = 0;
        byte[] page;
        do {
            page = getPage(false);
            if (page == null) {
                readed = -1;
            } else {
                final int positionInPage = (int) (positionInFile & inPageMask);
                final int inPageLength = pageSize - positionInPage;
                final int toRead = length - readed;
                final int copyLength;
                if (inPageLength < toRead) {
                    copyLength = inPageLength;
                } else {
                    copyLength = toRead;
                }
                System.arraycopy(page, positionInPage, data, readed, copyLength);
                positionInFile += copyLength;
                readed += copyLength;
            }
        } while (page != null && readed < length);
        return readed;
    }

    public void writeImpl(final byte[] data) throws IOException {
        writeImpl(data, data.length);
    }

    public void writeImpl(final byte[] data, final int length) throws IOException {
        int writed = 0;
        byte[] page;
        do {
            page = getPage(true);
            final int positionInPage = (int) (positionInFile & inPageMask);
            final int toWrite = length - writed;
            final int inPage = pageSize - positionInPage;
            final int lengthToCopy;
            if (toWrite < inPage) {
                lengthToCopy = toWrite;
            } else {
                lengthToCopy = inPage;
            }
            System.arraycopy(data, writed, page, positionInPage, lengthToCopy);
            writed += lengthToCopy;
            positionInFile += lengthToCopy;
        } while (writed < length);
    }

    public long lengthImpl() throws IOException {
        return randomAccessFileDelegate.length();
    }

    public void setLengthImpl(final long newSize) throws IOException {
        randomAccessFileDelegate.setLength((newSize & pagePositionMask) + pageSize);
    }

    /**
	 * get a page
	 * 
	 * @param forWrite
	 *            true if for write, create the page if not exist.
	 * @return the page or null if end of file reached
	 * @throws IOException
	 */
    private byte[] getPage(final boolean forWrite) throws IOException {
        final long pagePositionInFile = positionInFile & pagePositionMask;
        byte[] page = writeCacheMap.get(pagePositionInFile);
        if (page == null) {
            page = readCacheMap.get(pagePositionInFile);
            if (page == null) {
                randomAccessFileDelegate.seek(pagePositionInFile);
                page = new byte[pageSize];
                final int readed = randomAccessFileDelegate.read(page);
                if (readed == -1) {
                    if (forWrite) {
                        writeCacheMap.put(pagePositionInFile, page);
                        pageSet.add(pagePositionInFile);
                    } else {
                        page = null;
                    }
                } else {
                    if (forWrite) {
                        writeCacheMap.put(pagePositionInFile, page);
                        pageSet.add(pagePositionInFile);
                    } else if (!writeCacheMap.containsKey(pagePositionInFile) && haveReadCache) {
                        readCacheMap.put(pagePositionInFile, page);
                        pageSet.add(pagePositionInFile);
                    }
                }
                adjustCache();
            } else if (forWrite) {
                readCacheMap.remove(pagePositionInFile);
                writeCacheMap.put(pagePositionInFile, page);
                pageSet.remove(pagePositionInFile);
                pageSet.add(pagePositionInFile);
            }
        } else {
            pageSet.remove(pagePositionInFile);
            pageSet.add(pagePositionInFile);
        }
        return page;
    }

    /**
	 * adjust number of page in cache
	 * 
	 * @throws IOException
	 * 
	 */
    private void adjustCache() throws IOException {
        if (pageSet.size() > maxPage) {
            final Iterator<Long> iterator = pageSet.iterator();
            final long removedPosition = iterator.next();
            iterator.remove();
            if (writeCacheMap.containsKey(removedPosition)) {
                randomAccessFileDelegate.seek(removedPosition);
                final byte[] page = writeCacheMap.get(removedPosition);
                randomAccessFileDelegate.write(page);
                writeCacheMap.remove(removedPosition);
                if (_log.isDebugEnabled()) {
                    _log.debug("remove to write");
                }
            } else {
                if (readCacheMap.remove(removedPosition) == null) {
                    throw new IOException("page " + removedPosition + " must be in read or write cache");
                }
            }
        }
    }

    public void copy(final String fileName) throws IOException {
        if (opened) {
            flushImpl();
        }
        randomAccessFileDelegate.copy(fileName);
    }

    @Override
    public boolean differs(final String fileName, final SortedMap<Integer, Integer> diffMap) throws IOException {
        if (opened) {
            flushImpl();
        }
        return randomAccessFileDelegate.differs(fileName, diffMap);
    }
}

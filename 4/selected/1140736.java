package org.exist.storage.store;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import org.dbxml.core.DBException;
import org.dbxml.core.data.Value;
import org.dbxml.core.filer.BTree;
import org.dbxml.core.filer.BTreeCallback;
import org.dbxml.core.filer.BTreeException;
import org.dbxml.core.indexer.IndexQuery;
import org.exist.dom.DocumentImpl;
import org.exist.dom.NodeImpl;
import org.exist.dom.NodeIndexListener;
import org.exist.dom.NodeProxy;
import org.exist.dom.XMLUtil;
import org.exist.storage.BufferStats;
import org.exist.storage.NativeBroker;
import org.exist.storage.Signatures;
import org.exist.storage.cache.Cache;
import org.exist.storage.cache.Cacheable;
import org.exist.storage.cache.LRUCache;
import org.exist.util.ByteConversion;
import org.exist.util.Lock;
import org.exist.util.Lockable;
import org.exist.util.ReadOnlyException;
import org.exist.util.ReentrantReadWriteLock;
import org.exist.util.hashtable.Object2LongIdentityHashMap;
import org.exist.xquery.TerminatedException;
import org.w3c.dom.Node;

/**
 * This is the main storage for XML nodes. Nodes are stored in document order.
 * Every document gets its own sequence of pages, which is bound to the
 * writing thread to avoid conflicting writes. The page structure is as follows:
 * 
 * | page header | (tid1 node-data, tid2 node-data, ..., tidn node-data) |
 * 
 * node-data contains the raw binary data of the node as returned by {@link org.exist.dom.NodeImpl#serialize()}.
 * Within a page, a node is identified by a unique id, called tuple id (tid). Every node 
 * can thus be located by a virtual address pointer, which consists of the page id and 
 * the tid. Both components are encoded in a long value (with additional bits used for
 * optional flags). The address pointer is used to reference nodes from the indexes. It should 
 * thus remain unchanged during the life-time of a document.
 * 
 * However, XUpdate requests may insert new nodes in the middle of a page. In these
 * cases, the page will be split and the upper portion of the page is copied to a split page.
 * The record in the original page will be replaced by a forward link, pointing to the new
 * location of the node data in the split page.
 * 
 * As a consequence, the class has to distinguish three different types of data records:
 * 
 * 1) Ordinary record:
 * 
 * | tid | length | data |
 * 
 * 3) Relocated record:
 * 
 * | tid | length | address pointer to original location | data |
 * 
 * 2) Forward link:
 * 
 * | tid | address pointer |
 * 
 * tid and length each use two bytes (short), address pointers 8 bytes (long).
 * The upper two bits of the tid are used to indicate the type of the record 
 * (see {@see org.exist.storage.store.ItemId}).
 * 
 * @author Wolfgang Meier <wolfgang@exist-db.org>
 */
public class DOMFile extends BTree implements Lockable {

    public static final short FILE_FORMAT_VERSION_ID = 2;

    public static final byte LOB = 21;

    public static final byte RECORD = 20;

    public static final short OVERFLOW = 0;

    public static final long DATA_SYNC_PERIOD = 4200;

    private final Cache dataCache;

    private DOMFileHeader fileHeader;

    private Object owner = null;

    private Lock lock = null;

    private final Object2LongIdentityHashMap pages = new Object2LongIdentityHashMap(64);

    private DocumentImpl currentDocument = null;

    protected DOMFile(int buffers, int dataBuffers) {
        super(buffers);
        lock = new ReentrantReadWriteLock("dom.dbx");
        fileHeader = (DOMFileHeader) getFileHeader();
        fileHeader.setPageCount(0);
        fileHeader.setTotalCount(0);
        dataCache = new LRUCache(dataBuffers);
        dataCache.setFileName("dom.dbx");
    }

    public DOMFile(File file, int buffers, int dataBuffers) {
        this(buffers, dataBuffers);
        setFile(file);
    }

    protected final Cache getPageBuffer() {
        return dataCache;
    }

    /**
     * @return
     */
    public short getFileVersion() {
        return FILE_FORMAT_VERSION_ID;
    }

    public void setCurrentDocument(DocumentImpl doc) {
        this.currentDocument = doc;
    }

    /**
     * Append a value to the current page. 
     * 
     * This method is called when storing a new document. Each writing
     * thread gets its own sequence of pages for writing a document, so all
     * document nodes are stored in sequential order. A new page will be allocated
     * if the current page is full. If the value is larger than the page size, it will 
     * be written to an overflow page.
     * 
     * @param value
     *                   the value to append
     * @return the virtual storage address of the value
     */
    public long add(byte[] value) throws ReadOnlyException {
        if (value == null || value.length == 0) return -1;
        if (value.length + 4 > fileHeader.getWorkSize()) {
            LOG.debug("Creating overflow page");
            OverflowDOMPage overflow = new OverflowDOMPage();
            overflow.write(value);
            byte[] pnum = ByteConversion.longToByte(overflow.getPageNum());
            return add(pnum, true);
        } else return add(value, false);
    }

    /**
     * Append a value to the current page. If overflowPage is
     * true, the value will be saved into its own, reserved chain
     * of pages. The current page will just contain a link to the first
     * overflow page.
     * 
     * @param value
     * @param overflowPage
     * @return
     * @throws ReadOnlyException
     */
    private long add(byte[] value, boolean overflowPage) throws ReadOnlyException {
        final int valueLen = value.length;
        final Object myOwner = owner;
        DOMPage page = getCurrentPage();
        if (page == null || page.len + 4 + valueLen > page.data.length) {
            DOMPage newPage = new DOMPage();
            if (page != null) {
                DOMFilePageHeader ph = page.getPageHeader();
                ph.setNextDataPage(newPage.getPageNum());
                newPage.getPageHeader().setPrevDataPage(page.getPageNum());
                page.setDirty(true);
                dataCache.add(page);
            }
            page = newPage;
            setCurrentPage(newPage);
            if (owner != myOwner) LOG.error("Owner changed during transaction!!!!!!!!!!!!!!!!!");
        }
        final DOMFilePageHeader ph = page.getPageHeader();
        final short tid = ph.getNextTID();
        ByteConversion.shortToByte(tid, page.data, page.len);
        page.len += 2;
        ByteConversion.shortToByte(overflowPage ? OVERFLOW : (short) valueLen, page.data, page.len);
        page.len += 2;
        System.arraycopy(value, 0, page.data, page.len, valueLen);
        page.len += valueLen;
        ph.incRecordCount();
        ph.setDataLength(page.len);
        page.setDirty(true);
        dataCache.add(page, 2);
        final long p = StorageAddress.createPointer((int) page.getPageNum(), tid);
        return p;
    }

    /**
     * Store a raw binary resource into the file. The data will always
     * be written into an overflow page.
     * 
     * @param value
     * @return
     */
    public long addBinary(byte[] value) {
        OverflowDOMPage overflow = new OverflowDOMPage();
        overflow.write(value);
        return overflow.getPageNum();
    }

    /**
     * Return binary data stored with {@link #addBinary(byte[])}.
     * 
     * @param pageNum
     * @return
     */
    public byte[] getBinary(long pageNum) {
        return getOverflowValue(pageNum);
    }

    /**
     * Insert a new node after the specified node.
     * 
     * @param key
     * @param value
     * @return
     */
    public long insertAfter(DocumentImpl doc, Value key, byte[] value) {
        try {
            final long p = findValue(key);
            if (p == KEY_NOT_FOUND) return -1;
            return insertAfter(doc, p, value);
        } catch (BTreeException e) {
            LOG.warn("key not found", e);
        } catch (IOException e) {
            LOG.warn("IO error", e);
        }
        return -1;
    }

    /**
     * Insert a new node after the node located at the specified address.
     * 
     * If the previous node is in the middle of a page, the page is split. If the
     * node is appended at the end and the page does not have enough room
     * for the node, a new page is added to the page sequence.
     * 
     * @param doc
     *                   the document to which the new node belongs.
     * @param address
     *                   the storage address of the node after which the new value
     *                   should be inserted.
     * @param value
     *                   the value of the new node.
     * @return
     */
    public long insertAfter(DocumentImpl doc, long address, byte[] value) {
        boolean isOverflow = false;
        if (value.length + 4 > fileHeader.getWorkSize()) {
            OverflowDOMPage overflow = new OverflowDOMPage();
            LOG.debug("creating overflow page: " + overflow.getPageNum());
            overflow.write(value);
            value = ByteConversion.longToByte(overflow.getPageNum());
            isOverflow = true;
        }
        RecordPos rec = findRecord(address);
        if (rec == null) {
            LOG.warn("page not found");
            return -1;
        }
        short l = ByteConversion.byteToShort(rec.page.data, rec.offset);
        if (ItemId.isRelocated(rec.tid)) rec.offset += 8;
        if (l == OVERFLOW) rec.offset += 10; else rec.offset = rec.offset + l + 2;
        int dataLen = rec.page.getPageHeader().getDataLength();
        if (rec.offset < dataLen) {
            if (dataLen + value.length + 4 < fileHeader.getWorkSize() && rec.page.getPageHeader().hasRoom()) {
                int end = rec.offset + value.length + 4;
                System.arraycopy(rec.page.data, rec.offset, rec.page.data, end, dataLen - rec.offset);
                rec.page.len = dataLen + value.length + 4;
                rec.page.getPageHeader().setDataLength(rec.page.len);
            } else {
                rec = splitDataPage(doc, rec);
                if (rec.offset + value.length + 4 > fileHeader.getWorkSize() || !rec.page.getPageHeader().hasRoom()) {
                    DOMPage newPage = new DOMPage();
                    LOG.debug("creating additional page: " + newPage.getPageNum());
                    newPage.getPageHeader().setNextDataPage(rec.page.getPageHeader().getNextDataPage());
                    newPage.getPageHeader().setPrevDataPage(rec.page.getPageNum());
                    rec.page.getPageHeader().setNextDataPage(newPage.getPageNum());
                    rec.page.setDirty(true);
                    dataCache.add(rec.page);
                    rec.page = newPage;
                    rec.offset = 0;
                    rec.page.len = value.length + 4;
                    rec.page.getPageHeader().setDataLength(rec.page.len);
                    rec.page.getPageHeader().setRecordCount((short) 1);
                } else {
                    rec.page.len = rec.offset + value.length + 4;
                    rec.page.getPageHeader().setDataLength(rec.page.len);
                    dataLen = rec.offset;
                }
            }
        } else if (dataLen + value.length + 4 > fileHeader.getWorkSize() || !rec.page.getPageHeader().hasRoom()) {
            DOMPage newPage = new DOMPage();
            LOG.debug("creating new page: " + newPage.getPageNum());
            long next = rec.page.getPageHeader().getNextDataPage();
            newPage.getPageHeader().setNextDataPage(next);
            newPage.getPageHeader().setPrevDataPage(rec.page.getPageNum());
            rec.page.getPageHeader().setNextDataPage(newPage.getPageNum());
            if (-1 < next) {
                DOMPage nextPage = getCurrentPage(next);
                nextPage.getPageHeader().setPrevDataPage(newPage.getPageNum());
                nextPage.setDirty(true);
                dataCache.add(nextPage);
            }
            rec.page.setDirty(true);
            dataCache.add(rec.page);
            rec.page = newPage;
            rec.offset = 0;
            rec.page.len = value.length + 4;
            rec.page.getPageHeader().setDataLength(rec.page.len);
        } else {
            rec.page.len = dataLen + value.length + 4;
            rec.page.getPageHeader().setDataLength(rec.page.len);
        }
        short tid = rec.page.getPageHeader().getNextTID();
        ByteConversion.shortToByte((short) tid, rec.page.data, rec.offset);
        rec.offset += 2;
        ByteConversion.shortToByte(isOverflow ? 0 : (short) value.length, rec.page.data, rec.offset);
        rec.offset += 2;
        System.arraycopy(value, 0, rec.page.data, rec.offset, value.length);
        rec.offset += value.length;
        rec.page.getPageHeader().incRecordCount();
        rec.page.setDirty(true);
        if (rec.page.getPageHeader().getCurrentTID() >= ItemId.DEFRAG_LIMIT && doc != null) doc.triggerDefrag();
        dataCache.add(rec.page);
        return StorageAddress.createPointer((int) rec.page.getPageNum(), tid);
    }

    /**
     * Split a data page at the position indicated by the rec parameter.
     * 
     * The portion of the page starting at rec.offset is moved into a new page.
     * Every moved record is marked as relocated and a link is stored into
     * the original page to point to the new record position.  
     * 
     * @param doc
     * @param rec
     */
    private RecordPos splitDataPage(DocumentImpl doc, RecordPos rec) {
        if (currentDocument != null) currentDocument.incSplitCount();
        boolean requireSplit = false;
        for (int pos = rec.offset; pos < rec.page.len; ) {
            short currentId = ByteConversion.byteToShort(rec.page.data, pos);
            if (!ItemId.isLink(currentId)) {
                requireSplit = true;
                break;
            }
            pos += 10;
        }
        if (!requireSplit) {
            LOG.debug("page " + rec.page.getPageNum() + ": no split required");
            rec.offset = rec.page.len;
            return rec;
        }
        NodeIndexListener idx = doc.getIndexListener();
        int oldDataLen = rec.page.getPageHeader().getDataLength();
        byte[] oldData = rec.page.data;
        long oldPageNum = rec.page.getPageNum();
        rec.page.data = new byte[fileHeader.getWorkSize()];
        System.arraycopy(oldData, 0, rec.page.data, 0, rec.offset);
        rec.page.len = rec.offset;
        rec.page.setDirty(true);
        DOMPage firstSplitPage = new DOMPage();
        DOMPage nextSplitPage = firstSplitPage;
        nextSplitPage.getPageHeader().setNextTID((short) (rec.page.getPageHeader().getCurrentTID()));
        short tid, currentId, currentLen, realLen;
        long backLink;
        short splitRecordCount = 0;
        LOG.debug("splitting " + rec.page.getPageNum() + " at " + rec.offset + ": new: " + nextSplitPage.getPageNum() + "; next: " + rec.page.getPageHeader().getNextDataPage());
        for (int pos = rec.offset; pos < oldDataLen; splitRecordCount++) {
            currentId = ByteConversion.byteToShort(oldData, pos);
            tid = ItemId.getId(currentId);
            pos += 2;
            if (ItemId.isLink(currentId)) {
                ByteConversion.shortToByte(currentId, rec.page.data, rec.page.len);
                rec.page.len += 2;
                System.arraycopy(oldData, pos, rec.page.data, rec.page.len, 8);
                rec.page.len += 8;
                pos += 8;
                continue;
            }
            currentLen = ByteConversion.byteToShort(oldData, pos);
            pos += 2;
            realLen = (currentLen == OVERFLOW ? 8 : currentLen);
            if (nextSplitPage.len + realLen + 12 > fileHeader.getWorkSize()) {
                DOMPage newPage = new DOMPage();
                newPage.getPageHeader().setNextTID((short) (rec.page.getPageHeader().getNextTID() - 1));
                newPage.getPageHeader().setPrevDataPage(nextSplitPage.getPageNum());
                LOG.debug("creating new split page: " + newPage.getPageNum());
                nextSplitPage.getPageHeader().setNextDataPage(newPage.getPageNum());
                nextSplitPage.getPageHeader().setDataLength(nextSplitPage.len);
                nextSplitPage.getPageHeader().setRecordCount(splitRecordCount);
                nextSplitPage.setDirty(true);
                dataCache.add(nextSplitPage);
                dataCache.add(newPage);
                nextSplitPage = newPage;
                splitRecordCount = 0;
            }
            if (ItemId.isRelocated(currentId)) {
                backLink = ByteConversion.byteToLong(oldData, pos);
                pos += 8;
                RecordPos origRec = findRecord(backLink, false);
                long forwardLink = StorageAddress.createPointer((int) nextSplitPage.getPageNum(), tid);
                ByteConversion.longToByte(forwardLink, origRec.page.data, origRec.offset);
                origRec.page.setDirty(true);
                dataCache.add(origRec.page);
            } else backLink = StorageAddress.createPointer((int) rec.page.getPageNum(), tid);
            ByteConversion.shortToByte(ItemId.setIsRelocated(currentId), nextSplitPage.data, nextSplitPage.len);
            nextSplitPage.len += 2;
            ByteConversion.shortToByte(currentLen, nextSplitPage.data, nextSplitPage.len);
            nextSplitPage.len += 2;
            ByteConversion.longToByte(backLink, nextSplitPage.data, nextSplitPage.len);
            nextSplitPage.len += 8;
            try {
                System.arraycopy(oldData, pos, nextSplitPage.data, nextSplitPage.len, realLen);
            } catch (ArrayIndexOutOfBoundsException e) {
                LOG.error("pos = " + pos + "; len = " + nextSplitPage.len + "; currentLen = " + realLen + "; tid = " + currentId + "; page = " + rec.page.getPageNum());
                throw e;
            }
            nextSplitPage.len += realLen;
            pos += realLen;
            if (idx != null) {
                idx.nodeChanged(StorageAddress.createPointer((int) oldPageNum, tid), StorageAddress.createPointer((int) nextSplitPage.getPageNum(), tid));
            }
            if (!ItemId.isRelocated(currentId)) {
                if (rec.page.len + 10 > fileHeader.getWorkSize()) {
                    DOMPage newPage = new DOMPage();
                    newPage.getPageHeader().setNextTID((short) (rec.page.getPageHeader().getNextTID() - 1));
                    newPage.getPageHeader().setPrevDataPage(rec.page.getPageNum());
                    newPage.getPageHeader().setNextDataPage(rec.page.getPageHeader().getNextDataPage());
                    LOG.debug("creating new page after split: " + newPage.getPageNum());
                    rec.page.getPageHeader().setNextDataPage(newPage.getPageNum());
                    rec.page.getPageHeader().setDataLength(rec.page.len);
                    rec.page.getPageHeader().setRecordCount(countRecordsInPage(rec.page));
                    rec.page.setDirty(true);
                    dataCache.add(rec.page);
                    dataCache.add(newPage);
                    rec.page = newPage;
                    rec.page.len = 0;
                }
                ByteConversion.shortToByte(ItemId.setIsLink(currentId), rec.page.data, rec.page.len);
                rec.page.len += 2;
                long forwardLink = StorageAddress.createPointer((int) nextSplitPage.getPageNum(), tid);
                ByteConversion.longToByte(forwardLink, rec.page.data, rec.page.len);
                rec.page.len += 8;
            }
        }
        if (nextSplitPage.len == 0) {
            LOG.warn("page " + nextSplitPage.getPageNum() + " is empty. Remove it");
            dataCache.remove(nextSplitPage);
            if (nextSplitPage == firstSplitPage) firstSplitPage = null;
            try {
                unlinkPages(nextSplitPage.page);
            } catch (IOException e) {
                LOG.warn("Failed to remove empty split page: " + e.getMessage(), e);
            }
            nextSplitPage = null;
        } else {
            nextSplitPage.getPageHeader().setDataLength(nextSplitPage.len);
            nextSplitPage.getPageHeader().setNextDataPage(rec.page.getPageHeader().getNextDataPage());
            nextSplitPage.getPageHeader().setRecordCount(splitRecordCount);
            nextSplitPage.setDirty(true);
            dataCache.add(nextSplitPage);
            firstSplitPage.getPageHeader().setPrevDataPage(rec.page.getPageNum());
            if (nextSplitPage != firstSplitPage) {
                firstSplitPage.setDirty(true);
                dataCache.add(firstSplitPage);
            }
        }
        long next = rec.page.getPageHeader().getNextDataPage();
        if (-1 < next) {
            DOMPage nextPage = getCurrentPage(next);
            nextPage.getPageHeader().setPrevDataPage(nextSplitPage.getPageNum());
            nextPage.setDirty(true);
            dataCache.add(nextPage);
        }
        rec.page = getCurrentPage(rec.page.getPageNum());
        if (firstSplitPage != null) {
            rec.page.getPageHeader().setNextDataPage(firstSplitPage.getPageNum());
        }
        rec.page.getPageHeader().setDataLength(rec.page.len);
        rec.page.getPageHeader().setRecordCount(countRecordsInPage(rec.page));
        rec.offset = rec.page.len;
        return rec;
    }

    /**
     * Returns the number of records stored in a page.
     * 
     * @param page
     * @return
     */
    private short countRecordsInPage(DOMPage page) {
        short count = 0;
        short currentId, vlen;
        int dlen = page.getPageHeader().getDataLength();
        for (int pos = 0; pos < dlen; count++) {
            currentId = ByteConversion.byteToShort(page.data, pos);
            if (ItemId.isLink(currentId)) {
                pos += 10;
            } else {
                vlen = ByteConversion.byteToShort(page.data, pos + 2);
                if (ItemId.isRelocated(currentId)) {
                    pos += vlen == OVERFLOW ? 20 : vlen + 12;
                } else pos += vlen == OVERFLOW ? 12 : vlen + 4;
            }
        }
        return count;
    }

    public String debugPageContents(DOMPage page) {
        StringBuffer buf = new StringBuffer();
        buf.append("Page " + page.getPageNum() + ": ");
        short count = 0;
        short currentId, vlen;
        int dlen = page.getPageHeader().getDataLength();
        for (int pos = 0; pos < dlen; count++) {
            currentId = ByteConversion.byteToShort(page.data, pos);
            buf.append(ItemId.getId(currentId) + "[" + pos);
            if (ItemId.isLink(currentId)) {
                buf.append(':').append(10).append("] ");
                pos += 10;
            } else {
                vlen = ByteConversion.byteToShort(page.data, pos + 2);
                if (vlen < 0) {
                    LOG.warn("Illegal length: " + vlen);
                    return buf.toString();
                }
                buf.append(':').append(vlen).append("] ");
                if (ItemId.isRelocated(currentId)) {
                    pos += vlen == OVERFLOW ? 20 : vlen + 12;
                } else pos += vlen == OVERFLOW ? 12 : vlen + 4;
            }
        }
        buf.append("; records in page: " + count);
        buf.append("; nextTID: " + page.getPageHeader().getCurrentTID());
        return buf.toString();
    }

    public boolean close() throws DBException {
        flush();
        super.close();
        return true;
    }

    public boolean create() throws DBException {
        if (super.create((short) 12)) return true; else return false;
    }

    public FileHeader createFileHeader() {
        return new DOMFileHeader(1024, PAGE_SIZE);
    }

    public FileHeader createFileHeader(boolean read) throws IOException {
        return new DOMFileHeader(read);
    }

    public FileHeader createFileHeader(long pageCount) {
        return new DOMFileHeader(pageCount, PAGE_SIZE);
    }

    public FileHeader createFileHeader(long pageCount, int pageSize) {
        return new DOMFileHeader(pageCount, pageSize);
    }

    protected Page createNewPage() {
        try {
            Page page = getFreePage();
            DOMFilePageHeader ph = (DOMFilePageHeader) page.getPageHeader();
            ph.setStatus(RECORD);
            ph.setDirty(true);
            ph.setNextDataPage(-1);
            ph.setPrevDataPage(-1);
            ph.setNextTID((short) -1);
            ph.setDataLength(0);
            ph.setRecordCount((short) 0);
            if (currentDocument != null) currentDocument.incPageCount();
            return page;
        } catch (IOException ioe) {
            LOG.warn(ioe);
            return null;
        }
    }

    protected void unlinkPages(Page page) throws IOException {
        super.unlinkPages(page);
    }

    public PageHeader createPageHeader() {
        return new DOMFilePageHeader();
    }

    public ArrayList findKeys(IndexQuery query) throws IOException, BTreeException {
        final FindCallback cb = new FindCallback(FindCallback.KEYS);
        try {
            query(query, cb);
        } catch (TerminatedException e) {
            LOG.warn("Method terminated");
        }
        return cb.getValues();
    }

    private long findNode(NodeImpl node, long target, Iterator iter) {
        if (node.hasChildNodes()) {
            final long firstChildId = XMLUtil.getFirstChildId((DocumentImpl) node.getOwnerDocument(), node.getGID());
            if (firstChildId < 0) {
                LOG.debug("first child not found: " + node.getGID());
                return 0;
            }
            final long lastChildId = firstChildId + node.getChildCount();
            long p;
            for (long gid = firstChildId; gid < lastChildId; gid++) {
                NodeImpl child = (NodeImpl) iter.next();
                if (child == null) LOG.warn("Next node missing. gid = " + gid + "; last = " + lastChildId + "; parent= " + node.getNodeName() + "; count = " + node.getChildCount());
                if (gid == target) {
                    return ((NodeIterator) iter).currentAddress();
                }
                child.setGID(gid);
                if ((p = findNode(child, target, iter)) != 0) return p;
            }
        }
        return 0;
    }

    /**
     * Find a node by searching for a known ancestor in the index. If an
     * ancestor is found, it is traversed to locate the specified descendant
     * node.
     * 
     * @param lock
     * @param node
     * @return @throws
     *              IOException
     * @throws BTreeException
     */
    protected long findValue(Object lock, NodeProxy node) throws IOException, BTreeException {
        final DocumentImpl doc = (DocumentImpl) node.getDoc();
        final NativeBroker.NodeRef nodeRef = new NativeBroker.NodeRef(doc.getDocId(), node.getGID());
        final long p = findValue(nodeRef);
        if (p == KEY_NOT_FOUND) {
            long id = node.getGID();
            long parentPointer = -1;
            do {
                id = XMLUtil.getParentId(doc, id);
                if (id < 1) {
                    LOG.warn(node.gid + " not found.");
                    Thread.dumpStack();
                    throw new BTreeException("node " + node.gid + " not found.");
                }
                NativeBroker.NodeRef parentRef = new NativeBroker.NodeRef(doc.getDocId(), id);
                try {
                    parentPointer = findValue(parentRef);
                } catch (BTreeException bte) {
                }
            } while (parentPointer == KEY_NOT_FOUND);
            final long firstChildId = XMLUtil.getFirstChildId(doc, id);
            final Iterator iter = new NodeIterator(lock, this, node.getDocument(), parentPointer);
            final NodeImpl n = (NodeImpl) iter.next();
            n.setGID(id);
            final long address = findNode(n, node.gid, iter);
            if (address == 0) {
                return KEY_NOT_FOUND;
            } else return address;
        } else return p;
    }

    /**
     * Find matching nodes for the given query.
     * 
     * @param query
     *                   Description of the Parameter
     * @return Description of the Return Value
     * @exception IOException
     *                        Description of the Exception
     * @exception BTreeException
     *                        Description of the Exception
     */
    public ArrayList findValues(IndexQuery query) throws IOException, BTreeException {
        FindCallback cb = new FindCallback(FindCallback.VALUES);
        try {
            query(query, cb);
        } catch (TerminatedException e) {
            LOG.warn("Method terminated");
        }
        return cb.getValues();
    }

    /**
     * Flush all buffers to disk.
     * 
     * @return Description of the Return Value
     * @exception DBException
     *                        Description of the Exception
     */
    public boolean flush() throws DBException {
        super.flush();
        dataCache.flush();
        closeDocument();
        try {
            if (fileHeader.isDirty()) fileHeader.write();
        } catch (IOException ioe) {
            LOG.debug("sync failed", ioe);
        }
        return true;
    }

    public void printStatistics() {
        super.printStatistics();
        StringBuffer buf = new StringBuffer();
        buf.append(getFile().getName()).append(" DATA ");
        buf.append(dataCache.getBuffers()).append(" / ");
        buf.append(dataCache.getUsedBuffers()).append(" / ");
        buf.append(dataCache.getHits()).append(" / ");
        buf.append(dataCache.getFails());
        LOG.info(buf.toString());
    }

    public BufferStats getDataBufferStats() {
        return new BufferStats(dataCache.getBuffers(), dataCache.getUsedBuffers(), dataCache.getHits(), dataCache.getFails());
    }

    /**
     * Retrieve a node by key
     * 
     * @param key
     * @return Description of the Return Value
     */
    public Value get(Value key) {
        try {
            long p = findValue(key);
            if (p == KEY_NOT_FOUND) return null;
            return get(p);
        } catch (BTreeException bte) {
            return null;
        } catch (IOException ioe) {
            LOG.debug(ioe);
            return null;
        }
    }

    /**
     * Retrieve a node described by the given NodeProxy.
     * 
     * @param node
     *                   Description of the Parameter
     * @return Description of the Return Value
     */
    public Value get(NodeProxy node) {
        try {
            long p = findValue(owner, node);
            if (p == KEY_NOT_FOUND) return null;
            return get(p);
        } catch (BTreeException bte) {
            return null;
        } catch (IOException ioe) {
            LOG.debug(ioe);
            return null;
        }
    }

    /**
     * Retrieve node at virtual address p.
     * 
     * @param p
     *                   Description of the Parameter
     * @return Description of the Return Value
     */
    public Value get(long p) {
        RecordPos rec = findRecord(p);
        if (rec == null) {
            LOG.warn("object at " + StorageAddress.toString(p) + " not found.");
            Thread.dumpStack();
            return null;
        }
        short l = ByteConversion.byteToShort(rec.page.data, rec.offset);
        rec.offset += 2;
        if (ItemId.isRelocated(rec.tid)) rec.offset += 8;
        Value v;
        if (l == OVERFLOW) {
            long pnum = ByteConversion.byteToLong(rec.page.data, rec.offset);
            byte[] data = getOverflowValue(pnum);
            v = new Value(data);
        } else v = new Value(rec.page.data, rec.offset, l);
        v.setAddress(p);
        return v;
    }

    protected byte[] getOverflowValue(long pnum) {
        try {
            OverflowDOMPage overflow = new OverflowDOMPage(pnum);
            return overflow.read();
        } catch (IOException e) {
            LOG.error("io error while loading overflow value", e);
            return null;
        }
    }

    public void removeOverflowValue(long pnum) {
        try {
            OverflowDOMPage overflow = new OverflowDOMPage(pnum);
            overflow.delete();
        } catch (IOException e) {
            LOG.error("io error while removing overflow value", e);
        }
    }

    /**
     * Retrieve the last page in the current sequence.
     * 
     * @return The currentPage value
     */
    private final DOMPage getCurrentPage() {
        long pnum = pages.get(owner);
        if (pnum < 0) {
            final DOMPage page = new DOMPage();
            pages.put(owner, page.page.getPageNum());
            dataCache.add(page);
            return page;
        } else return getCurrentPage(pnum);
    }

    /**
     * Retrieve the page with page number p
     * 
     * @param p
     *                   Description of the Parameter
     * @return The currentPage value
     */
    protected final DOMPage getCurrentPage(long p) {
        DOMPage page = (DOMPage) dataCache.get(p);
        if (page == null) {
            page = new DOMPage(p);
        }
        return page;
    }

    public void closeDocument() {
        pages.remove(owner);
    }

    /**
     * Open the file.
     * 
     * @return Description of the Return Value
     * @exception DBException
     *                        Description of the Exception
     */
    public boolean open() throws DBException {
        return super.open(FILE_FORMAT_VERSION_ID);
    }

    /**
     * Put a new key/value pair.
     * 
     * @param key
     *                   Description of the Parameter
     * @param value
     *                   Description of the Parameter
     * @return Description of the Return Value
     */
    public long put(Value key, byte[] value) throws ReadOnlyException {
        long p = add(value);
        try {
            addValue(key, p);
        } catch (IOException ioe) {
            LOG.debug(ioe);
            return -1;
        } catch (BTreeException bte) {
            LOG.debug(bte);
            return -1;
        }
        return p;
    }

    /**
     * Physically remove a node. The data of the node will be removed from
     * the page and the occupied space is freed.
     */
    public void remove(Value key) {
        try {
            long p = findValue(key);
            if (p == KEY_NOT_FOUND) return;
            remove(key, p);
        } catch (BTreeException bte) {
            LOG.debug(bte);
        } catch (IOException ioe) {
            LOG.debug(ioe);
        }
    }

    /**
     * Remove the link at the specified position from the file.
     * 
     * @param p
     */
    private void removeLink(long p) {
        RecordPos rec = findRecord(p, false);
        DOMFilePageHeader ph = rec.page.getPageHeader();
        int end = rec.offset + 8;
        System.arraycopy(rec.page.data, rec.offset + 8, rec.page.data, rec.offset - 2, rec.page.len - end);
        rec.page.len = rec.page.len - 10;
        ph.setDataLength(rec.page.len);
        rec.page.setDirty(true);
        ph.decRecordCount();
        if (rec.page.len == 0) {
            removePage(rec.page);
            rec.page = null;
        } else {
            dataCache.add(rec.page);
        }
    }

    /**
     * Physically remove a node. The data of the node will be removed from
     * the page and the occupied space is freed.
     * 
     * @param p
     */
    public void remove(long p) {
        RecordPos rec = findRecord(p);
        int startOffset = rec.offset - 2;
        DOMFilePageHeader ph = rec.page.getPageHeader();
        short l = ByteConversion.byteToShort(rec.page.data, rec.offset);
        rec.offset += 2;
        if (ItemId.isLink(rec.tid)) {
            throw new RuntimeException("Cannot remove link ...");
        }
        if (ItemId.isRelocated(rec.tid)) {
            long backLink = ByteConversion.byteToLong(rec.page.data, rec.offset);
            removeLink(backLink);
            rec.offset += 8;
            l += 8;
        }
        if (l == OVERFLOW) {
            long pnum = ByteConversion.byteToLong(rec.page.data, rec.offset);
            rec.offset += 8;
            try {
                OverflowDOMPage overflow = new OverflowDOMPage(pnum);
                overflow.delete();
            } catch (IOException e) {
                LOG.error("io error while removing overflow page", e);
            }
            l += 8;
        }
        int end = startOffset + 4 + l;
        int len = ph.getDataLength();
        System.arraycopy(rec.page.data, end, rec.page.data, startOffset, len - end);
        rec.page.setDirty(true);
        len = len - l - 4;
        ph.setDataLength(len);
        rec.page.len = len;
        rec.page.setDirty(true);
        ph.decRecordCount();
        if (rec.page.len == 0) {
            LOG.debug("removing page " + rec.page.getPageNum());
            removePage(rec.page);
            rec.page = null;
        } else {
            rec.page.cleanUp();
            dataCache.add(rec.page);
        }
    }

    /**
     * Physically remove a node. The data of the node will be removed from
     * the page and the occupied space is freed. 
     */
    public void remove(Value key, long p) {
        remove(p);
        try {
            removeValue(key);
        } catch (BTreeException e) {
            LOG.warn("btree error while removing node", e);
        } catch (IOException e) {
            LOG.warn("io error while removing node", e);
        }
    }

    /**
     * Remove the specified page. The page is added
     * to the list of free pages.
     * 
     * @param page
     */
    public void removePage(DOMPage page) {
        dataCache.remove(page);
        DOMFilePageHeader ph = page.getPageHeader();
        if (ph.getNextDataPage() > -1) {
            DOMPage next = getCurrentPage(ph.getNextDataPage());
            next.getPageHeader().setPrevDataPage(ph.getPrevDataPage());
            next.setDirty(true);
            dataCache.add(next);
        }
        if (ph.getPrevDataPage() > -1) {
            DOMPage prev = getCurrentPage(ph.getPrevDataPage());
            prev.getPageHeader().setNextDataPage(ph.getNextDataPage());
            prev.setDirty(true);
            dataCache.add(prev);
        }
        try {
            ph.setNextDataPage(-1);
            ph.setPrevDataPage(-1);
            ph.setDataLength(0);
            ph.setNextTID((short) -1);
            ph.setRecordCount((short) 0);
            unlinkPages(page.page);
        } catch (IOException ioe) {
            LOG.warn(ioe);
        }
        if (currentDocument != null) currentDocument.decPageCount();
    }

    public void removeAll(long p) {
        long pnum = StorageAddress.pageFromPointer(p);
        while (-1 < pnum) {
            DOMPage page = getCurrentPage(pnum);
            pnum = page.getPageHeader().getNextDataPage();
            dataCache.remove(page);
            try {
                DOMFilePageHeader ph = page.getPageHeader();
                ph.setNextDataPage(-1);
                ph.setPrevDataPage(-1);
                ph.setDataLength(0);
                ph.setNextTID((short) -1);
                ph.setRecordCount((short) 0);
                page.len = 0;
                unlinkPages(page.page);
            } catch (IOException e) {
                LOG.warn("Error while removing page: " + e.getMessage(), e);
            }
        }
    }

    public String debugPages(DocumentImpl doc) {
        StringBuffer buf = new StringBuffer();
        buf.append("Pages used by ").append(doc.getName());
        buf.append("; docId ").append(doc.getDocId()).append(':');
        long pnum = StorageAddress.pageFromPointer(((NodeImpl) doc.getFirstChild()).getInternalAddress());
        while (-1 < pnum) {
            DOMPage page = getCurrentPage(pnum);
            dataCache.add(page);
            buf.append(' ').append(pnum);
            pnum = page.getPageHeader().getNextDataPage();
        }
        buf.append("; Document metadata at " + StorageAddress.toString(doc.getAddress()));
        return buf.toString();
    }

    /**
     * Set the last page in the sequence to which nodes are currently appended.
     * 
     * @param page
     *                   The new currentPage value
     */
    private final void setCurrentPage(DOMPage page) {
        long pnum = pages.get(owner);
        if (pnum == page.page.getPageNum()) return;
        pages.put(owner, page.page.getPageNum());
    }

    /**
     * Get the active Lock object for this file.
     * 
     * @see org.exist.util.Lockable#getLock()
     */
    public final Lock getLock() {
        return lock;
    }

    /**
     * The current object owning this file.
     * 
     * @param obj
     *                   The new ownerObject value
     */
    public final void setOwnerObject(Object obj) {
        owner = obj;
    }

    /**
     * Update the key/value pair.
     * 
     * @param key
     *                   Description of the Parameter
     * @param value
     *                   Description of the Parameter
     * @return Description of the Return Value
     */
    public boolean update(Value key, byte[] value) throws ReadOnlyException {
        try {
            long p = findValue(key);
            if (p == KEY_NOT_FOUND) return false;
            update(p, value);
        } catch (BTreeException bte) {
            LOG.debug(bte);
            bte.printStackTrace();
            return false;
        } catch (IOException ioe) {
            LOG.debug(ioe);
            return false;
        }
        return true;
    }

    /**
     * Update the key/value pair where the value is found at address p.
     * 
     * @param key
     *                   Description of the Parameter
     * @param p
     *                   Description of the Parameter
     * @param value
     *                   Description of the Parameter
     */
    public void update(long p, byte[] value) throws ReadOnlyException {
        RecordPos rec = findRecord(p);
        short l = ByteConversion.byteToShort(rec.page.data, rec.offset);
        rec.offset += 2;
        if (ItemId.isRelocated(rec.tid)) rec.offset += 8;
        if (value.length < l) {
            throw new IllegalStateException("shrinked");
        } else if (value.length > l) {
            throw new IllegalStateException("value too long: expected: " + value.length + "; got: " + l);
        } else {
            System.arraycopy(value, 0, rec.page.data, rec.offset, value.length);
        }
        rec.page.setDirty(true);
    }

    /**
     * Retrieve the string value of the specified node.
     * 
     * @param proxy
     * @return
     */
    public String getNodeValue(NodeProxy proxy) {
        try {
            long address = proxy.getInternalAddress();
            if (address < 0) address = findValue(this, proxy);
            if (address == BTree.KEY_NOT_FOUND) return null;
            final RecordPos rec = findRecord(address);
            if (rec == null) {
                LOG.warn("Node data could not be found! Page: " + StorageAddress.pageFromPointer(address) + "; tid: " + StorageAddress.tidFromPointer(address));
                throw new RuntimeException("Node data could not be found for node " + proxy.gid);
            }
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            getNodeValue(os, rec, true);
            final byte[] data = os.toByteArray();
            String value;
            try {
                value = new String(data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                value = new String(data);
            }
            return value;
        } catch (BTreeException e) {
            LOG.warn("btree error while reading node value", e);
        } catch (IOException e) {
            LOG.warn("io error while reading node value", e);
        }
        return null;
    }

    private void getNodeValue(ByteArrayOutputStream os, RecordPos rec, boolean firstCall) {
        boolean foundNext = false;
        do {
            if (rec.offset > rec.page.getPageHeader().getDataLength()) {
                final long nextPage = rec.page.getPageHeader().getNextDataPage();
                if (nextPage < 0) {
                    LOG.warn("bad link to next page");
                    return;
                }
                rec.page = getCurrentPage(nextPage);
                dataCache.add(rec.page);
                rec.offset = 2;
            }
            rec.tid = ByteConversion.byteToShort(rec.page.data, rec.offset - 2);
            if (ItemId.isLink(rec.tid)) {
                rec.offset += 10;
            } else foundNext = true;
        } while (!foundNext);
        int len = ByteConversion.byteToShort(rec.page.data, rec.offset);
        rec.offset += 2;
        if (ItemId.isRelocated(rec.tid)) rec.offset += 8;
        byte[] data = rec.page.data;
        int readOffset = rec.offset;
        if (len == OVERFLOW) {
            final long op = ByteConversion.byteToLong(data, rec.offset);
            data = getOverflowValue(op);
            len = data.length;
            readOffset = 0;
            rec.offset += 8;
        }
        final short type = Signatures.getType(data[readOffset]);
        switch(type) {
            case Node.ELEMENT_NODE:
                final int children = ByteConversion.byteToInt(data, readOffset + 1);
                final byte attrSizeType = (byte) ((data[readOffset] & 0x0C) >> 0x2);
                final short attributes = (short) Signatures.read(attrSizeType, data, readOffset + 5);
                rec.offset += len + 2;
                for (int i = 0; i < children; i++) {
                    getNodeValue(os, rec, false);
                    if (children - attributes > 1) os.write((byte) ' ');
                }
                return;
            case Node.TEXT_NODE:
                os.write(data, readOffset + 1, len - 1);
                break;
            case Node.ATTRIBUTE_NODE:
                if (firstCall) {
                    final byte idSizeType = (byte) (data[readOffset] & 0x3);
                    final boolean hasNamespace = (data[readOffset] & 0x10) == 0x10;
                    int next = Signatures.getLength(idSizeType) + 1;
                    if (hasNamespace) {
                        next += 2;
                        final short prefixLen = ByteConversion.byteToShort(data, readOffset + next);
                        next += prefixLen + 2;
                    }
                    os.write(rec.page.data, readOffset + next, len - next);
                }
                break;
        }
        if (len != OVERFLOW) rec.offset += len + 2;
    }

    protected RecordPos findRecord(long p) {
        return findRecord(p, true);
    }

    /**
         * Find a record within the page or the pages linked to it.
         * 
         * @param p
         * @return
         */
    protected RecordPos findRecord(long p, boolean skipLinks) {
        long pageNr = StorageAddress.pageFromPointer(p);
        short targetId = StorageAddress.tidFromPointer(p);
        DOMPage page;
        int pos;
        short currentId, vlen;
        RecordPos rec;
        while (pageNr > -1) {
            page = getCurrentPage(pageNr);
            dataCache.add(page);
            rec = page.findRecord(targetId);
            if (rec == null) {
                pageNr = page.getPageHeader().getNextDataPage();
                if (pageNr == page.getPageNum()) {
                    LOG.debug("circular link to next page on " + pageNr);
                    return null;
                }
                LOG.debug(owner.toString() + ": tid " + targetId + " not found on " + page.page.getPageInfo() + ". Loading " + pageNr + "; contents: " + debugPageContents(page));
            } else if (rec.isLink) {
                if (!skipLinks) return rec;
                long forwardLink = ByteConversion.byteToLong(page.data, rec.offset);
                pageNr = StorageAddress.pageFromPointer(forwardLink);
                targetId = StorageAddress.tidFromPointer(forwardLink);
            } else {
                return rec;
            }
        }
        return null;
    }

    private final class DOMFileHeader extends BTreeFileHeader {

        protected LinkedList reserved = new LinkedList();

        public DOMFileHeader() {
        }

        public DOMFileHeader(long pageCount) {
            super(pageCount);
        }

        public DOMFileHeader(long pageCount, int pageSize) {
            super(pageCount, pageSize);
        }

        public DOMFileHeader(long pageCount, int pageSize, byte blockSize) {
            super(pageCount, pageSize, blockSize);
        }

        public DOMFileHeader(boolean read) throws IOException {
            super(read);
        }

        public void addReservedPage(long page) {
            reserved.addFirst(new Long(page));
        }

        public long getReservedPage() {
            if (reserved.size() == 0) return -1;
            return ((Long) reserved.removeLast()).longValue();
        }

        public void read(java.io.RandomAccessFile raf) throws IOException {
            super.read(raf);
            int rp = raf.readInt();
            long l;
            for (int i = 0; i < rp; i++) {
                l = raf.readLong();
                reserved.addFirst(new Long(l));
            }
        }

        public void write(java.io.RandomAccessFile raf) throws IOException {
            super.write(raf);
            raf.writeInt(reserved.size());
            Long l;
            for (Iterator i = reserved.iterator(); i.hasNext(); ) {
                l = (Long) i.next();
                raf.writeLong(l.longValue());
            }
        }
    }

    protected static final class DOMFilePageHeader extends BTreePageHeader {

        protected int dataLen = 0;

        protected long nextDataPage = -1;

        protected long prevDataPage = -1;

        protected short tid = -1;

        protected short records = 0;

        public DOMFilePageHeader() {
            super();
        }

        public DOMFilePageHeader(byte[] data, int offset) throws IOException {
            super(data, offset);
        }

        public void decRecordCount() {
            --records;
        }

        public short getCurrentTID() {
            return tid;
        }

        public short getNextTID() {
            if (++tid == ItemId.ID_MASK) throw new RuntimeException("no spare ids on page");
            return tid;
        }

        public boolean hasRoom() {
            return tid < ItemId.MAX_ID;
        }

        public void setNextTID(short tid) {
            if (tid > ItemId.MAX_ID) throw new RuntimeException("TID overflow! TID = " + tid);
            this.tid = tid;
        }

        public int getDataLength() {
            return dataLen;
        }

        public long getNextDataPage() {
            return nextDataPage;
        }

        public long getPrevDataPage() {
            return prevDataPage;
        }

        public short getRecordCount() {
            return records;
        }

        public void incRecordCount() {
            records++;
        }

        public int read(byte[] data, int offset) throws IOException {
            offset = super.read(data, offset);
            records = ByteConversion.byteToShort(data, offset);
            offset += 2;
            dataLen = ByteConversion.byteToInt(data, offset);
            offset += 4;
            nextDataPage = ByteConversion.byteToLong(data, offset);
            offset += 8;
            prevDataPage = ByteConversion.byteToLong(data, offset);
            offset += 8;
            tid = ByteConversion.byteToShort(data, offset);
            return offset + 2;
        }

        public int write(byte[] data, int offset) throws IOException {
            offset = super.write(data, offset);
            ByteConversion.shortToByte(records, data, offset);
            offset += 2;
            ByteConversion.intToByte(dataLen, data, offset);
            offset += 4;
            ByteConversion.longToByte(nextDataPage, data, offset);
            offset += 8;
            ByteConversion.longToByte(prevDataPage, data, offset);
            offset += 8;
            ByteConversion.shortToByte(tid, data, offset);
            return offset + 2;
        }

        public void setDataLength(int len) {
            dataLen = len;
        }

        public void setNextDataPage(long page) {
            nextDataPage = page;
        }

        public void setPrevDataPage(long page) {
            prevDataPage = page;
        }

        public void setRecordCount(short recs) {
            records = recs;
        }
    }

    protected final class DOMPage implements Cacheable {

        byte[] data;

        int len = 0;

        Page page;

        DOMFilePageHeader ph;

        int refCount = 0;

        int timestamp = 0;

        boolean saved = true;

        boolean invalidated = false;

        public DOMPage() {
            page = createNewPage();
            ph = (DOMFilePageHeader) page.getPageHeader();
            data = new byte[fileHeader.getWorkSize()];
            len = 0;
        }

        public DOMPage(long pos) {
            try {
                page = getPage(pos);
                load(page);
            } catch (IOException ioe) {
                LOG.debug(ioe);
                ioe.printStackTrace();
            }
        }

        public DOMPage(Page page) {
            this.page = page;
            load(page);
        }

        public RecordPos findRecord(short targetId) {
            final int dlen = ph.getDataLength();
            short currentId;
            short vlen;
            RecordPos rec = null;
            byte flags;
            for (int pos = 0; pos < dlen; ) {
                currentId = ByteConversion.byteToShort(data, pos);
                flags = ItemId.getFlags(currentId);
                if (ItemId.matches(currentId, targetId)) {
                    if ((flags & ItemId.LINK_FLAG) != 0) {
                        rec = new RecordPos(pos + 2, this, currentId);
                        rec.isLink = true;
                    } else {
                        rec = new RecordPos(pos + 2, this, currentId);
                    }
                    break;
                } else if ((flags & ItemId.LINK_FLAG) != 0) {
                    pos += 10;
                } else {
                    vlen = ByteConversion.byteToShort(data, pos + 2);
                    if (vlen < 0) {
                        LOG.warn("page = " + page.getPageNum() + "; pos = " + pos + "; vlen = " + vlen + "; tid = " + currentId + "; target = " + targetId);
                    }
                    if ((flags & ItemId.RELOCATED_FLAG) != 0) {
                        pos += vlen + 12;
                    } else {
                        pos += vlen + 4;
                    }
                    if (vlen == OVERFLOW) pos += 8;
                }
            }
            return rec;
        }

        public long getKey() {
            return page.getPageNum();
        }

        public int getReferenceCount() {
            return refCount;
        }

        public int decReferenceCount() {
            return refCount > 0 ? --refCount : 0;
        }

        public int incReferenceCount() {
            if (refCount < Cacheable.MAX_REF) ++refCount;
            return refCount;
        }

        public void setReferenceCount(int count) {
            refCount = count;
        }

        public void setTimestamp(int timestamp) {
            this.timestamp = timestamp;
        }

        public int getTimestamp() {
            return timestamp;
        }

        public DOMFilePageHeader getPageHeader() {
            return ph;
        }

        public long getPageNum() {
            return page.getPageNum();
        }

        public boolean isDirty() {
            return !saved;
        }

        public void setDirty(boolean dirty) {
            saved = !dirty;
            page.getPageHeader().setDirty(dirty);
        }

        private void load(Page page) {
            try {
                data = page.read();
                ph = (DOMFilePageHeader) page.getPageHeader();
                len = ph.getDataLength();
                if (data.length == 0) {
                    LOG.debug("page " + page.getPageNum() + " data length == 0");
                    return;
                }
            } catch (IOException ioe) {
                LOG.debug(ioe);
                ioe.printStackTrace();
            }
            saved = true;
        }

        public void write() {
            if (page == null) return;
            try {
                if (!ph.isDirty()) return;
                ph.setDataLength(len);
                writeValue(page, data);
                setDirty(false);
            } catch (IOException ioe) {
                LOG.error(ioe);
            }
        }

        public String dumpPage() {
            return "Contents of page " + page.getPageNum() + ": " + hexDump(data);
        }

        public boolean sync() {
            if (isDirty()) {
                write();
                return true;
            }
            return false;
        }

        public boolean allowUnload() {
            return true;
        }

        public boolean equals(Object obj) {
            DOMPage other = (DOMPage) obj;
            return page.equals(other.page);
        }

        public void invalidate() {
            invalidated = true;
        }

        public boolean isInvalidated() {
            return invalidated;
        }

        /**
         * Walk through the page after records have been removed.
         * Set the tid counter to the next spare id that can be used for following
         * insertions. 
         */
        public void cleanUp() {
            final int dlen = ph.getDataLength();
            short currentId, vlen, tid;
            short maxTID = 0;
            for (int pos = 0; pos < dlen; ) {
                currentId = ByteConversion.byteToShort(data, pos);
                tid = ItemId.getId(currentId);
                if (tid > ItemId.MAX_ID) {
                    LOG.debug(debugPageContents(this));
                    throw new RuntimeException("TID overflow in page " + getPageNum());
                }
                if (tid > maxTID) maxTID = tid;
                if (ItemId.isLink(currentId)) {
                    pos += 10;
                } else {
                    vlen = ByteConversion.byteToShort(data, pos + 2);
                    if (ItemId.isRelocated(currentId)) {
                        pos += vlen == OVERFLOW ? 20 : vlen + 12;
                    } else pos += vlen == OVERFLOW ? 12 : vlen + 4;
                }
            }
            ph.setNextTID(maxTID);
        }
    }

    /**
     * This represents an overflow page. Overflow pages are created if
     * the node data exceeds the size of one page. An overflow page is a
     * sequence of DOMPages.
     *  
     * @author wolf
     *
     */
    protected final class OverflowDOMPage {

        Page firstPage = null;

        public OverflowDOMPage() {
            LOG.debug("Creating overflow page");
            firstPage = createNewPage();
        }

        public OverflowDOMPage(long first) throws IOException {
            firstPage = getPage(first);
        }

        public void write(byte[] data) {
            try {
                int remaining = data.length;
                int chunkSize = fileHeader.getWorkSize();
                Page page = firstPage, next = null;
                int chunk, pos = 0;
                Value value;
                while (remaining > 0) {
                    chunkSize = remaining > fileHeader.getWorkSize() ? fileHeader.getWorkSize() : remaining;
                    value = new Value(data, pos, chunkSize);
                    remaining -= chunkSize;
                    if (remaining > 0) {
                        next = createNewPage();
                        page.getPageHeader().setNextPage(next.getPageNum());
                    } else page.getPageHeader().setNextPage(-1);
                    writeValue(page, value);
                    pos += chunkSize;
                    page = next;
                    next = null;
                }
            } catch (IOException e) {
                LOG.error("io error while writing overflow page", e);
            }
        }

        public byte[] read() {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Page page = firstPage;
            byte[] chunk;
            long np;
            int count = 0;
            while (page != null) {
                try {
                    chunk = page.read();
                    os.write(chunk);
                    np = page.getPageHeader().getNextPage();
                    page = np > -1 ? getPage(np) : null;
                } catch (IOException e) {
                    LOG.error("io error while loading overflow page " + firstPage.getPageNum() + "; read: " + count, e);
                    break;
                }
                ++count;
            }
            return os.toByteArray();
        }

        public void delete() throws IOException {
            Page page = firstPage;
            long np;
            while (page != null) {
                page.read();
                LOG.debug("removing overflow page " + page.getPageNum());
                np = page.getPageHeader().getNextPage();
                unlinkPages(page);
                page = np > -1 ? getPage(np) : null;
            }
        }

        public long getPageNum() {
            return firstPage.getPageNum();
        }
    }

    public final void addToBuffer(DOMPage page) {
        dataCache.add(page);
    }

    private final class FindCallback implements BTreeCallback {

        public static final int KEYS = 1;

        public static final int VALUES = 0;

        int mode = VALUES;

        ArrayList values = new ArrayList();

        public FindCallback(int mode) {
            this.mode = mode;
        }

        public ArrayList getValues() {
            return values;
        }

        public boolean indexInfo(Value value, long pointer) {
            switch(mode) {
                case VALUES:
                    RecordPos rec = findRecord(pointer);
                    short l = ByteConversion.byteToShort(rec.page.data, rec.offset);
                    int dataStart = rec.offset + 2;
                    values.add(new Value(rec.page.data, dataStart, l));
                    return true;
                case KEYS:
                    values.add(value);
                    return true;
            }
            return false;
        }
    }

    private final class RangeCallback implements BTreeCallback {

        ArrayList values = new ArrayList();

        public RangeCallback() {
        }

        public ArrayList getValues() {
            return values;
        }

        public boolean indexInfo(Value value, long pointer) throws TerminatedException {
            RecordPos rec = findRecord(pointer);
            short l = ByteConversion.byteToShort(rec.page.data, rec.offset);
            int dataStart = rec.offset + 2;
            values.add(new Value(rec.page.data, dataStart, l));
            return true;
        }
    }

    protected static final class RecordPos {

        DOMPage page = null;

        int offset = -1;

        short tid = 0;

        boolean isLink = false;

        public RecordPos(int offset, DOMPage page, short tid) {
            this.offset = offset;
            this.page = page;
            this.tid = tid;
        }
    }
}

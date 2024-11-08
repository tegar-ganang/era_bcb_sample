package dovetaildb.bagindex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import dovetaildb.fileaccessor.MappedFile;
import dovetaildb.fileaccessor.OffsetValueFilePair;
import dovetaildb.fileaccessor.PagedFile;
import dovetaildb.scan.AbstractScanner;
import dovetaildb.scan.IntegerScanner;
import dovetaildb.scan.Scanner;
import dovetaildb.scan.ScannerUtil;
import dovetaildb.score.Score;
import dovetaildb.score.Scorer;
import dovetaildb.store.BytesInterface;
import dovetaildb.store.ChannelBasedMappedFile;
import dovetaildb.store.ChunkedMemoryMappedFile;
import dovetaildb.store.MappedBytesInterface;
import dovetaildb.store.VarPosition;
import dovetaildb.util.Pair;
import dovetaildb.util.Util;
import dovetaildb.util.VIntScanner;

public class TrieBagIndex extends BagIndex {

    /**
	 * Pages:
	 * Trie page (link up + summary list + link complete list + 256 links down)
	 * Single value posting list page (link up + single value + compressed doc id list) -- NOT USED
	 * Multi value posting list page (link prev:4 + link next:4 + compressed doc id list + value list) (as much as fits on one page)
	 *   First offset is the complete initial doc number, subsequent offsets are increments with the low bit reserved for whether the value is different than the previous.
	 *   When low bit is not set, a value pointer VLong follows, the low bit of which indicates whether the value is inline.
	 *   When this low bit is not set, the value is considered an offset into the values file and a Vlong for the value length follows.
	 * ID terms are suffixed with pointers to remainder of data -> (compressed value page link & offset) list
	 * Deletions: add a *del:<docid>*(<txn>) field
	 * 
	 */
    PagedFile pages;

    BytesInterface pageBi;

    protected final int pageSize;

    protected final int overflowThreshold;

    protected final int minRecsPerPage;

    protected BytesInterface maxDocIdFile;

    long maxDocId;

    /** Simple 4 bytes per docId, indexed from zero, representing an unsigned pointer into the docFields file */
    protected MappedBytesInterface docIdToDoc;

    /** Each doc is a list of VInts, each doc is an ordered list of value pointers, low bit indicates 
	 *    an offset into the page file (1) or 
	 *    an offset into the overflow file (0) 
	 *  In the case of the overflow data, a VInt for length follows 
	 */
    protected MappedBytesInterface docFields;

    /** Raw overflow data */
    protected ChannelBasedMappedFile overflowFieldData;

    protected enum PageType {

        TRIE, SINGLE_VAL, MULTI_VAL
    }

    ;

    protected final int LEAF_MASK = 0x80000000;

    protected final int SINGLE_MASK = 0x80000000;

    private String homeDir;

    protected int getPrevLeafPage(int page) {
        return pageBi.getInt(pages.getIntOffestForPage(page));
    }

    protected int getNextLeafPage(int page) {
        return pageBi.getInt(pages.getIntOffestForPage(page) + 1);
    }

    private void setPrevLeafPage(int page, int prevPage) {
        pageBi.putInt(pages.getIntOffestForPage(page), prevPage);
    }

    private void setNextLeafPage(int page, int nextPage) {
        pageBi.putInt(pages.getIntOffestForPage(page + 1), nextPage);
    }

    private long descendOffset(int page, byte byteValue) {
        return pages.getIntOffestForPage(page) + 3 + (byteValue & 0xff);
    }

    private int descend(int page, byte byteValue) {
        long intOffset = descendOffset(page, byteValue);
        if (intOffset == 0) return 0; else return pageBi.getInt(intOffset);
    }

    private int getParentPage(int page) {
        return pageBi.getInt(pages.getIntOffestForPage(page));
    }

    private int getSummaryPage(int page) {
        return pageBi.getInt(pages.getIntOffestForPage(page) + 1);
    }

    private int getLinkCompletePage(int page) {
        return pageBi.getInt(pages.getIntOffestForPage(page) + 2);
    }

    final class LookedUpTerm {

        public int page, termPrefixLen, parentPage;

        public LookedUpTerm(int page, int termPrefixLen, int parentPage) {
            this.page = page;
            this.termPrefixLen = termPrefixLen;
            this.parentPage = parentPage;
        }
    }

    protected LookedUpTerm descendByTerm(byte[] term) {
        int parentPage = -1;
        int page = 0;
        int termIdx = 0;
        while (termIdx < term.length) {
            page = descend(page, term[termIdx++]);
            if ((LEAF_MASK & page) == LEAF_MASK) {
                break;
            }
        }
        return new LookedUpTerm(page, termIdx, parentPage);
    }

    protected LookedUpTerm findLeafForTerm(byte[] term) {
        LookedUpTerm result = descendByTerm(term);
        int page = result.page;
        if ((LEAF_MASK & page) != LEAF_MASK) {
            page = getLinkCompletePage(page);
        }
        result.page = page & ~LEAF_MASK;
        return result;
    }

    /** returns the next page */
    private int extendSingleValuedPage(int page) {
        int newPage = pages.newPageIndex();
        long offset = pages.getIntOffestForPage(page);
        pageBi.putInt(offset, newPage);
        long newPageByteOffset = pages.getByteOffestForPage(newPage);
        for (int i = 0; i < 5; i++) {
            pageBi.putByte(newPageByteOffset + i, (byte) 0);
        }
        return newPage;
    }

    final class TermInDocRec implements Comparable {

        final byte[] term;

        final long docId;

        final long termValueOffset;

        final int termValueLength;

        int bump;

        public TermInDocRec(byte[] term, long docId) {
            this(term, docId, 0);
        }

        public TermInDocRec(byte[] term, long docId, long termValueOffset) {
            this.term = term;
            this.docId = docId;
            this.termValueOffset = termValueOffset;
            this.termValueLength = 0;
            this.bump = 0;
        }

        public TermInDocRec(long docId, long termValueOffset, int termValueLength) {
            this.termValueOffset = termValueOffset;
            this.termValueLength = termValueLength;
            this.docId = docId;
            this.term = null;
        }

        public TermInDocRec(long docId, TermInDocRec prev) {
            this.docId = docId;
            this.bump = prev.bump;
            this.term = prev.term;
            this.termValueOffset = prev.termValueOffset;
            this.termValueLength = prev.termValueLength;
        }

        public boolean equals(Object otherObj) {
            TermInDocRec o = (TermInDocRec) otherObj;
            if (this.term == o.term && this.termValueOffset == o.termValueOffset && this.termValueLength == o.termValueLength) {
                return true;
            }
            return compareTo(otherObj) == 0;
        }

        public int compareTo(Object otherObj) {
            if (bump != 0) throw new RuntimeException("Not implemented");
            TermInDocRec other = (TermInDocRec) otherObj;
            int cmp = Util.compareBytes(this.term, other.term);
            if (cmp == 0) {
                cmp = (int) (this.docId - other.docId);
            }
            return cmp;
        }

        public int getLength() {
            if (term != null) return term.length - bump; else return termValueLength;
        }

        public int firstByte() {
            if (term != null) {
                return term[bump];
            } else {
                throw new RuntimeException("not yet implemented");
            }
        }

        public byte[] getTerm() {
            if (term != null) {
                if (bump != 0) throw new RuntimeException("not yet implemented");
                return term;
            } else {
                throw new RuntimeException("not yet implemented");
            }
        }

        public byte getByteAt(int i) {
            if (term != null) {
                return term[bump + i];
            } else {
                return pageBi.getByte(this.termValueOffset + bump + i);
            }
        }

        public void bump(int bump) {
            if (term == null) {
                this.bump = bump;
            } else {
                throw new RuntimeException("not yet implemented");
            }
        }
    }

    class PagePlan {

        ArrayList<TermInDocRec> docs;

        PagePlan[] subPlans;

        public PagePlan() {
            docs = new ArrayList<TermInDocRec>();
            subPlans = null;
        }

        public PagePlan(ArrayList<TermInDocRec> docs) {
            this.docs = docs;
        }
    }

    private PagePlan generatePlan(List<TermInDocRec> docs) {
        int numDocs = docs.size();
        PagePlan plan = new PagePlan();
        if ((numDocs <= minRecsPerPage) || (docs.get(0).compareTo(docs.get(numDocs - 1)) == 0)) {
            for (TermInDocRec doc : docs) {
                plan.docs.add(doc);
            }
        } else {
            plan.subPlans = new PagePlan[256];
            ArrayList<TermInDocRec>[] buckets = new ArrayList[256];
            for (TermInDocRec doc : docs) {
                int len = doc.getLength();
                if (len == 0) {
                    plan.docs.add(doc);
                } else {
                    int insertIdx = doc.firstByte() & 0xff;
                    ArrayList<TermInDocRec> bucket = buckets[insertIdx];
                    if (bucket == null) {
                        buckets[insertIdx] = bucket = new ArrayList<TermInDocRec>();
                    }
                    bucket.add(doc);
                }
            }
            int i = -1;
            for (ArrayList<TermInDocRec> bucket : buckets) {
                if (i == -1) {
                    if (bucket != null) {
                        plan.docs = bucket;
                    }
                } else {
                    if (bucket != null) {
                        plan.subPlans[i - 1] = generatePlan(bucket);
                    }
                }
                i++;
            }
        }
        return plan;
    }

    private int writePlan(PagePlan plan, int parentPage) {
        int newPage = pages.newPageIndex();
        if (plan.subPlans == null) {
            TermInDocRec prev = null;
            VarPosition position = new VarPosition(pages.getByteOffestForPage(newPage));
            VarPosition cap = new VarPosition(pages.getByteOffestForPage(newPage + 1));
            for (TermInDocRec rec : plan.docs) {
                writeTermInDocRecAndValue(prev, rec, position, cap);
                prev = rec;
            }
        } else {
            long intOffset = pages.getIntOffestForPage(newPage);
            pageBi.putInt(intOffset++, parentPage);
            pageBi.putInt(intOffset++, 0);
            if ((plan.docs != null) && (plan.docs.size() > 0)) {
                pageBi.putInt(intOffset++, writePlan(new PagePlan(plan.docs), newPage));
            } else {
                pageBi.putInt(intOffset++, 0);
            }
            pageBi.putInt(intOffset++, 0);
            for (PagePlan subPlan : plan.subPlans) {
                if (subPlan == null) {
                    pageBi.putInt(intOffset++, 0);
                } else {
                    int subPageId = writePlan(subPlan, newPage);
                    pageBi.putInt(intOffset++, subPageId);
                }
            }
        }
        return newPage;
    }

    /** page is assumed to be a multi-valued leaf page */
    private void split(int parentPage, byte byteValue) {
        int page = descend(parentPage, byteValue);
        ArrayList<TermInDocRec> recs = parseMultiValuedLeafPage(page);
        Collections.sort(recs);
        PagePlan plan = generatePlan(recs);
        int newRootPage = writePlan(plan, parentPage);
        pageBi.putInt(descendOffset(page, byteValue), newRootPage);
        pages.markForDeletion(page);
    }

    private ArrayList<dovetaildb.bagindex.TrieBagIndex.TermInDocRec> parseMultiValuedLeafPage(int page) {
        ArrayList<TermInDocRec> recs = new ArrayList<TermInDocRec>();
        long firstPage = page;
        long nextPage = -1;
        do {
            long byteOffset = pages.getByteOffestForPage(page) + 8L;
            VarPosition top = new VarPosition(pages.getByteOffestForPage(page + 1));
            VarPosition vp = new VarPosition(byteOffset);
            while (true) {
                TermInDocRec rec = this.readTermInDocRec(null, vp, top);
                if (rec == null) break; else recs.add(rec);
            }
            nextPage = pageBi.getUInt(pages.getIntOffestForPage(page) + 1);
        } while (firstPage != nextPage);
        return recs;
    }

    private int makeLeafPageUsing(List<TermInDocRec> bucket, int newTriePage) {
        int page = pages.newPageIndex();
        byte[] firstTerm = bucket.get(0).term;
        boolean isSingleValued = true;
        for (TermInDocRec rec : bucket) {
            if (Util.compareBytes(firstTerm, rec.term) != 0) {
                isSingleValued = false;
                break;
            }
        }
        long byteOffset = pages.getByteOffestForPage(page);
        long byteOffsetCap = pages.getByteOffestForPage(page + 1);
        long intOffset = pages.getIntOffestForPage(page);
        if (isSingleValued) {
            pageBi.putInt(intOffset, SINGLE_MASK);
            VarPosition vp = new VarPosition(byteOffset + 4);
            for (TermInDocRec rec : bucket) {
                pageBi.putVLong(vp, rec.docId, byteOffsetCap);
            }
            pageBi.putVLong(vp, 0, byteOffsetCap);
        } else {
            pageBi.putByte(byteOffset++, (byte) 0);
            VarPosition vp = new VarPosition(byteOffset);
            for (TermInDocRec rec : bucket) {
                pageBi.putVLong(vp, rec.docId, byteOffsetCap);
                pageBi.putVLong(vp, rec.term.length, byteOffsetCap);
                byteOffsetCap -= rec.term.length;
                pageBi.putBytes(byteOffsetCap, rec.term.length, rec.term, 0);
            }
            pageBi.putVLong(vp, 0, byteOffsetCap);
        }
        return page;
    }

    protected void backUp(VarPosition vp) {
        long i = vp.position - 2;
        while ((pageBi.getByte(i) & 0x80) == 0) {
            i--;
        }
        vp.position = i + 1;
    }

    protected boolean writeTermInDocRecAndValue(TermInDocRec prev, TermInDocRec rec, VarPosition position, VarPosition top) {
        int len = rec.getLength();
        if (len <= overflowThreshold) {
            if (position.position - top.position <= len) return false;
            if (!writeTermInDocRec(prev, rec, position, top.position - len)) return false;
            top.position -= len;
            pageBi.putBytes(top.position, len, rec.term, (int) rec.termValueOffset);
            return true;
        } else {
            return writeTermInDocRec(prev, rec, position, top.position);
        }
    }

    protected boolean writeTermInDocRec(TermInDocRec prev, TermInDocRec rec, VarPosition position, long cap) {
        if (prev.compareTo(rec) == 0) {
            return pageBi.putVLong(position, (rec.docId << 1) | 0x01, cap);
        } else {
            if (!pageBi.putVLong(position, rec.docId << 1, cap)) return false;
            int len = rec.getLength();
            if (len <= overflowThreshold) {
                if (pageBi.putVLong(position, (len << 1) | 0x01, cap)) return true;
            } else {
                long insertPosition = overflowFieldData.logicalAppend(rec.term, rec.bump, rec.getLength());
                if (pageBi.putVLong(position, (insertPosition << 1), cap)) {
                    if (pageBi.putVLong(position, len, cap)) return true; else backUp(position);
                }
            }
            backUp(position);
            pageBi.putVLong(position, 0, cap);
            return false;
        }
    }

    protected TermInDocRec readTermInDocRec(TermInDocRec prev, VarPosition position, VarPosition top) {
        long docId = pageBi.getVLong(position);
        boolean sameVal = (docId & 0x01) == 0x01;
        docId >>= 1;
        if (docId == 0) {
            return null;
        } else if (sameVal) {
            return new TermInDocRec(docId, prev);
        } else {
            int valLen = (int) pageBi.getVLong(position);
            boolean isInline = (valLen & 0x01) == 0x01;
            valLen >>= 1;
            if (isInline) {
                top.position -= valLen;
                byte[] literal = new byte[valLen];
                pageBi.getBytes(top.position, valLen, literal, 0);
                return new TermInDocRec(literal, docId);
            } else {
                long valPos = pageBi.getVLong(position);
                return new TermInDocRec(docId, valPos, valLen);
            }
        }
    }

    /** returns the offset to the written entry */
    protected long insertTerm(TermInDocRec rec) {
        LookedUpTerm ret = findLeafForTerm(rec.getTerm());
        int termPrefixLen = ret.termPrefixLen;
        int parentTriePage = ret.parentPage;
        int firstLeafPage = ret.page;
        int lastLeafPage = this.getPrevLeafPage(firstLeafPage);
        rec.bump(termPrefixLen);
        VarPosition pos = getDocsStartForPage(lastLeafPage);
        VarPosition top = getDocsCapForPage(lastLeafPage);
        TermInDocRec cur = null;
        TermInDocRec prev = null;
        boolean allSame = true;
        do {
            cur = readTermInDocRec(prev, pos, top);
            if (allSame && prev != null && !cur.equals(prev)) allSame = false;
        } while (cur != null);
        long startingPos = pos.position;
        boolean wroteIt = writeTermInDocRecAndValue(prev, rec, pos, top);
        if (wroteIt) {
            return startingPos;
        }
        if (allSame) {
            int newPage = pages.newPageIndex();
            setNextLeafPage(newPage, firstLeafPage);
            setPrevLeafPage(newPage, lastLeafPage);
            pos = getDocsStartForPage(newPage);
            top = getDocsCapForPage(newPage);
            prev = null;
            startingPos = pos.position;
            wroteIt = writeTermInDocRecAndValue(prev, rec, pos, top);
            if (!wroteIt) throw new RuntimeException();
            setNextLeafPage(lastLeafPage, newPage);
            setPrevLeafPage(firstLeafPage, newPage);
            return startingPos;
        }
        byte lastByte = rec.getByteAt(termPrefixLen - 1);
        split(parentTriePage, lastByte);
        return insertTerm(rec);
    }

    private VarPosition getDocsStartForPage(int leafPage) {
        long offset = pages.getByteOffestForPage(leafPage);
        return new VarPosition(offset + 8);
    }

    private VarPosition getDocsCapForPage(int leafPage) {
        return new VarPosition(pages.getByteOffestForPage(leafPage + 1));
    }

    TrieBagIndex() {
        this(1036, 8);
    }

    TrieBagIndex(int pageSize, int overflowThreshold) {
        this.pageSize = 0;
        this.overflowThreshold = overflowThreshold;
        this.minRecsPerPage = (pageSize - 8) / (overflowThreshold + 20);
    }

    @Override
    public void close() {
        pages.close();
    }

    @Override
    public long commitNewRev(long[] deletions, Collection<Pair<byte[][], byte[][]>> inserts) {
        int numInserts = inserts.size();
        long revNum = maxDocId + numInserts;
        if (numInserts == 0) {
            inserts.add(new Pair<byte[][], byte[][]>(null, null));
            revNum++;
        }
        Arrays.sort(deletions);
        deleteInRev(deletions, revNum);
        ByteArrayOutputStream docEntryBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream termValueBuf = new ByteArrayOutputStream();
        VIntScanner.writeVLong(docEntryBuf, (long) termValueBuf.size());
        byte[][][] groups = new byte[2][][];
        for (Pair<byte[][], byte[][]> docPair : inserts) {
            byte[][] indexTerms = docPair.getLeft();
            byte[][] storeTerms = docPair.getRight();
            groups[0] = storeTerms;
            groups[1] = indexTerms;
            for (byte[][] insertTerms : groups) {
                for (byte[] term : insertTerms) {
                    long sz = term.length;
                    VIntScanner.writeVLong(docEntryBuf, sz);
                    try {
                        termValueBuf.write(term);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    TermInDocRec rec = new TermInDocRec(term, maxDocId);
                    long dataPosition = insertTerm(rec);
                }
            }
            maxDocId++;
        }
        pageBi.force();
        maxDocIdFile.putLong(0, maxDocId);
        maxDocIdFile.force();
        return maxDocId;
    }

    protected void deleteInRev(long[] deletions, long revNum) {
        byte[] delTerm = new byte[2 + 8];
        delTerm[0] = 0;
        delTerm[1] = 'd';
        TermInDocRec rec = new TermInDocRec(delTerm, revNum, 2 + 8);
        for (long deletion : deletions) {
            Util.beLongToBytes(deletion, delTerm, 2);
            this.insertTerm(rec);
        }
    }

    public void fetchSubRange(int rootPage, int termIdx, ArrayList<Scanner> scanners, byte[] term1, byte[] term2, boolean isOpen1, boolean isOpen2, boolean isExclusive1, boolean isExclusive2, long revNum, Score score) {
        long offset = this.getTrieStartForPage(rootPage);
        long t1 = isOpen1 || termIdx >= term1.length ? 0 : term1[termIdx] & 0xff;
        long t2 = isOpen2 || termIdx >= term2.length ? 255 : term2[termIdx] & 0xff;
        for (long t = t1; t <= t2; t++) {
            int page = pageBi.getInt(offset + t);
            if (page == 0) {
                continue;
            }
            boolean isLeaf = (LEAF_MASK & page) == LEAF_MASK;
            if (isLeaf) {
                page &= ~LEAF_MASK;
                scanners.add(new LeafScanner(page, revNum, score));
            } else {
                Score nextScore = null;
                if (score != null) {
                    nextScore = score.duplicate();
                    nextScore.add((byte) t);
                }
                fetchSubRange(page, termIdx + 1, scanners, term1, term2, t > t1 || isOpen1, t < t2 || isOpen2, isExclusive1, isExclusive2, revNum, nextScore);
            }
        }
    }

    private long getTrieStartForPage(int page) {
        return pages.getIntOffestForPage(page) + 3;
    }

    @Override
    public Scanner fetchRange(byte[] prefix, byte[] term1, byte[] term2, boolean isExclusive1, boolean isExclusive2, long revNum, Scorer scorer) {
        Score score = scorer == null ? null : scorer.newScore();
        LookedUpTerm prefixResult = descendByTerm(prefix);
        for (int i = 0; i < prefixResult.termPrefixLen; i++) {
            score.add(prefix[i]);
        }
        final int page = prefixResult.page;
        boolean isLeaf = (LEAF_MASK & page) == LEAF_MASK;
        if (isLeaf) {
            return new LeafScanner(page, revNum, score);
        } else {
            ArrayList<Scanner> scanners = new ArrayList<Scanner>();
            fetchSubRange(page, 0, scanners, term1, term2, false, false, isExclusive1, isExclusive2, revNum, score);
            return ScannerUtil.conjunctiveScanner(scanners);
        }
    }

    private final class LeafScanner extends AbstractScanner {

        int page, firstPage;

        VarPosition pos, top;

        TermInDocRec cur = null;

        TermInDocRec prev = null;

        long revNum;

        double min, max;

        LeafScanner(int firstPage, long revNum, Score score) {
            this.firstPage = firstPage;
            int page = firstPage;
            pos = getDocsStartForPage(page);
            top = getDocsCapForPage(page);
            this.revNum = revNum;
            if (score == null) {
                this.min = Double.MIN_VALUE;
                this.max = Double.MAX_VALUE;
            } else {
                this.min = score.min();
                this.max = score.max();
            }
        }

        public long doc() {
            return cur.docId;
        }

        public boolean next() {
            cur = readTermInDocRec(prev, pos, top);
            while (cur == null) {
                page = getNextLeafPage(page);
                if (page == firstPage) {
                    return false;
                } else {
                    pos = getDocsStartForPage(page);
                    top = getDocsCapForPage(page);
                    cur = readTermInDocRec(prev, pos, top);
                    prev = null;
                }
            }
            return true;
        }

        public boolean dropDocsScoringLessThan(double amount) {
            if (amount > max) {
                cur = null;
                return false;
            } else {
                return true;
            }
        }

        public boolean dropDocsScoringMoreThan(double amount) {
            if (amount < min) {
                cur = null;
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public Scanner fetchTd(byte[] term, long revNum) {
        LookedUpTerm leaf = findLeafForTerm(term);
        int firstPage = leaf.page;
        return new LeafScanner(firstPage, revNum, null);
    }

    @Override
    public Scanner fetchAll(long revNum) {
        return new IntegerScanner(revNum);
    }

    @Override
    public Scanner fetchDeletions(long revNum) {
        return null;
    }

    @Override
    public BagIndexDoc fetchDoc(long docId) {
        return null;
    }

    @Override
    public BagIndexDoc refetchDoc(BagIndexDoc doc, long docId) {
        return fetchDoc(docId);
    }

    @Override
    public String getHomedir() {
        return null;
    }

    protected FileChannel openFile(String name) {
        String filename = homeDir + File.separatorChar + "maxdocid";
        RandomAccessFile maxDocIdRaf;
        try {
            maxDocIdRaf = new RandomAccessFile(new File(filename), "rw");
            return maxDocIdRaf.getChannel();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setHomedir(String homeDir) {
        try {
            this.homeDir = homeDir;
            File pagesFile = new File(homeDir + File.separatorChar + "pages");
            boolean newIndex = !pagesFile.exists();
            if (newIndex) {
                System.out.println("No index present at " + homeDir + "; creating a new index.");
            }
            pages = new PagedFile(pagesFile);
            pageBi = pages.getBytesInterface();
            maxDocIdFile = new MappedBytesInterface(openFile("maxdocid"));
            if (newIndex) {
                maxDocIdFile = maxDocIdFile.ensureSizeAtLeast(8);
                maxDocIdFile.putLong(0, 0);
                int pageLocation = writePlan(new PagePlan(), 0);
                if (pageLocation != 0) throw new RuntimeException("page location not at root - unexpected");
            }
            docIdToDoc = new MappedBytesInterface(openFile("docidtodoc"));
            docFields = new MappedBytesInterface(openFile("docfields"));
            overflowFieldData = new ChunkedMemoryMappedFile(openFile("overflow"));
            this.maxDocId = maxDocIdFile.getLong(0);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getCurrentRevNum() {
        return maxDocId;
    }
}

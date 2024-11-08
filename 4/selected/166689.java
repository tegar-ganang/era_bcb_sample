package dovetaildb.bagindex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import dovetaildb.bytes.AbstractBytes;
import dovetaildb.bytes.ArrayBytes;
import dovetaildb.bytes.Bytes;
import dovetaildb.bytes.CompoundBytes;
import dovetaildb.bytes.MaterializedBytes;
import dovetaildb.bytes.SlicedBytes;
import dovetaildb.querynode.QueryNode;
import dovetaildb.querynode.RangeQueryNode;
import dovetaildb.scan.LiteralScanner;
import dovetaildb.scan.Scanner;
import dovetaildb.score.ConstantScorer;
import dovetaildb.score.Scorer;
import dovetaildb.store.BytesInterface;
import dovetaildb.store.ChunkedMemoryMappedFile;
import dovetaildb.store.VarPosition;
import dovetaildb.util.Pair;
import dovetaildb.util.PubliclyCloneable;
import dovetaildb.util.Util;

public class ImmutableBagIndex extends BagIndex {

    /**
PART 1 page:
VInt header: bottom bit indicates whether it's a segment push + part change or a part change only, second to bottom indictes whether
 the part change applies only to terms terminating at this value.  value indicates number of consumed value bytes
<value>
part change: backwards offset  and initial docId
  OPTIONAL:   segment push: backwards offset and term cap (num bytes and value)
  
PART 2 page:
VInt docid increment, bottom three bits indicate whether value is inline (and its length), or two flags for whether value is external or this is a segment push
is segment push (docid increment is applied prior to push and a VInts for
  the backwards offset, bottom bit = 1 indicates there is an increment cap vint which
  immediately follows. 
  Increment cap for the push is inclusive, so a value of 0 means pull only one entry.
  the next docid is the increment in addition to the cap if present, otherwise it's
  an increment added to the frst vint in this record
if it has an external value, two VInts follow, for backward offset and length
if it has an inline value, the value bytes follow inline
*/
    protected BytesInterface data;

    protected BytesInterface header;

    protected long maxDocId;

    protected String homeDir;

    protected long version;

    private long rootPos;

    static class DocTerm {

        long docId;

        Bytes term;
    }

    static class Edit extends DocTerm implements Comparable<DocTerm> {

        boolean isDelete;

        public Edit(long docId, Bytes term, boolean isDelete) {
            this.docId = docId;
            this.term = term;
            this.isDelete = isDelete;
        }

        public int compareTo(DocTerm o) {
            long ret = docId - o.docId;
            if (ret == 0) return term.compareTo(o.term); else return (ret > 0) ? 1 : -1;
        }

        public String toString() {
            return "Edit(" + docId + "," + term + "," + (isDelete ? "del" : "ins") + ")";
        }
    }

    static interface Rec extends Cloneable {

        public Rec next();

        public Rec down();

        public Rec clone();

        public long compareTo(DocTerm docTerm);

        public long cumulativeCount();

        public Bytes getPrefix();

        public Bytes getSuffix();
    }

    static interface DocRec extends Rec {

        public long getDocId();

        public DocRec next();

        public DocRec down();

        public DocRec clone();

        public long getDownCt();
    }

    static interface TermRec extends Rec {

        public DocRec getDocList();

        public long getDocListCap();

        public TermRec next();

        public TermRec down();

        public TermRec clone();
    }

    static final class BoundedDocRec implements DocRec {

        long ct;

        DocRec dr;

        public BoundedDocRec(DocRec dr, long ct) {
            this.dr = dr;
            this.ct = ct;
        }

        public boolean stopsHere() {
            return dr.cumulativeCount() >= ct;
        }

        public MemDocRec getMemDocRec() {
            if (!stopsHere()) {
                return new MemDocRec(dr);
            } else {
                for (BoundedDocRec sub = down(); sub != null; sub = sub.next()) {
                    if (sub.stopsHere()) {
                        MemDocRec r = sub.getMemDocRec();
                        r.setDown(dr.down());
                        r.setDownCt(ct - 1);
                        return r;
                    }
                }
                throw new RuntimeException("NOT HANDLED");
            }
        }

        public BoundedDocRec clone() {
            return new BoundedDocRec(dr.clone(), ct);
        }

        public long getDocId() {
            return dr.getDocId();
        }

        public long getDownCt() {
            long downct = dr.getDownCt();
            return (ct > downct) ? downct : ct;
        }

        public long compareTo(DocTerm docTerm) {
            return dr.compareTo(docTerm);
        }

        public long cumulativeCount() {
            return dr.cumulativeCount();
        }

        public Bytes getPrefix() {
            return dr.getPrefix();
        }

        public Bytes getSuffix() {
            return dr.getSuffix();
        }

        public BoundedDocRec down() {
            long downct = getDownCt();
            DocRec down = dr.down();
            if (down == null || downct < 1) return null;
            return new BoundedDocRec(down, downct);
        }

        public String toString() {
            return "BoundedDocRec";
        }

        public BoundedDocRec next() {
            ct -= dr.cumulativeCount();
            if (ct <= 0) return null;
            dr = dr.next();
            if (dr == null) return null;
            while (ct < dr.cumulativeCount()) {
                dr = dr.down();
            }
            return this;
        }
    }

    abstract static class MemRec implements Rec {

        Bytes prefix, suffix;

        public abstract void setNext(Rec r);

        public abstract void setDown(Rec r);

        public abstract void setDownCt(long ct);

        public Bytes getPrefix() {
            return prefix;
        }

        public Bytes getSuffix() {
            return suffix;
        }

        public MemRec clone() {
            try {
                return (MemRec) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static final class MemDocRec extends MemRec implements DocRec {

        DocRec next, down;

        long docId, downCt;

        public MemDocRec(Bytes prefix, Bytes suffix, long docId, DocRec next) {
            this(prefix, suffix, docId, next, null, 0);
        }

        public MemDocRec(Bytes prefix, Bytes suffix, long docId, DocRec next, DocRec down, long downCt) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.docId = docId;
            this.next = next;
            this.down = down;
            this.downCt = downCt;
        }

        public MemDocRec(DocRec cur) {
            DocRec temp = cur.clone();
            this.next = temp.next();
            this.down = cur.down();
            this.docId = cur.getDocId();
            this.downCt = cur.getDownCt();
            this.prefix = cur.getPrefix();
            this.suffix = cur.getSuffix();
        }

        public void setNext(Rec r) {
            this.next = (DocRec) r;
        }

        public long getDocId() {
            return this.docId;
        }

        public long cumulativeCount() {
            return downCt + 1;
        }

        public DocRec down() {
            return down;
        }

        public DocRec next() {
            return next;
        }

        public long compareTo(DocTerm docTerm) {
            long delta = docId - docTerm.docId;
            if (delta != 0) return delta;
            return new CompoundBytes(prefix, suffix).compareTo(docTerm.term);
        }

        public void setDown(Rec r) {
            this.down = (DocRec) r;
        }

        public long getDownCt() {
            return this.downCt;
        }

        public void setDownCt(long ct) {
            this.downCt = ct;
        }

        public String toString() {
            String s = System.identityHashCode(this) + ":" + prefix + "@" + System.identityHashCode(prefix) + " " + suffix + " doc" + this.getDocId() + " ";
            if (down != null) {
                s += "dwn(" + this.downCt + ")->" + System.identityHashCode(down) + " ";
            }
            s += "nxt->" + System.identityHashCode(next);
            return s;
        }

        public MemDocRec clone() {
            return (MemDocRec) super.clone();
        }
    }

    static final class MemTermRec extends MemRec implements TermRec {

        TermRec next, down;

        DocRec docs;

        long docCt, downCt;

        Bytes prefix, suffix;

        public MemTermRec(Bytes prefix, Bytes suffix, TermRec next, DocRec docs, long docCt) {
            this(prefix, suffix, next, docs, docCt, null, 0);
        }

        public MemTermRec(Bytes prefix, Bytes suffix, TermRec next, DocRec docs, long docCt, TermRec down, long downCt) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.next = next;
            this.docs = docs;
            this.docCt = docCt;
            this.down = down;
            this.downCt = downCt;
        }

        public void setNext(Rec r) {
            this.next = (TermRec) r;
        }

        public long cumulativeCount() {
            return docCt + 1;
        }

        public TermRec down() {
            return down;
        }

        public TermRec next() {
            return next;
        }

        public long compareTo(DocTerm docTerm) {
            return new CompoundBytes(prefix, suffix).compareTo(docTerm.term);
        }

        public DocRec getDocList() {
            return docs;
        }

        public long getDocListCap() {
            return docCt;
        }

        public void setDown(Rec r) {
            this.down = (TermRec) r;
        }

        public void setDownCt(long ct) {
            this.docCt = ct;
        }

        public MemTermRec clone() {
            return (MemTermRec) super.clone();
        }
    }

    static class RecBuffer<T extends MemRec> {

        T head = null;

        T tail = null;

        long ct = 0;

        public void append(T seg) {
            ct += seg.cumulativeCount();
            seg.setNext(null);
            if (tail == null) {
                head = tail = seg;
            } else {
                tail.setNext(seg);
                tail = seg;
            }
        }

        public long cumulativeCount() {
            return ct;
        }

        public void extend(RecBuffer<T> buffer) {
            T otherHead = buffer.head;
            if (otherHead == null) return;
            if (tail == null) {
                head = buffer.head;
                tail = buffer.tail;
            } else {
                tail.setNext(buffer.head);
                tail = buffer.tail;
            }
            ct += buffer.ct;
        }

        public void clear() {
            this.ct = 0;
            this.head = this.tail = null;
        }

        public String toString() {
            StringBuffer b = new StringBuffer("RecBuffer\n");
            T h = head;
            LinkedList<T> todo = new LinkedList<T>();
            HashSet<T> seen = new HashSet<T>();
            todo.add(head);
            while (!todo.isEmpty()) {
                h = todo.removeFirst();
                b.append("{\n");
                while (h != null) {
                    if (seen.contains(h)) break;
                    seen.add(h);
                    b.append(h.toString());
                    b.append("\n");
                    Rec down = h.down();
                    if (down != null && !seen.contains(down)) todo.addLast((T) down);
                    h = (T) h.next();
                }
                b.append("}\n");
            }
            return b.toString();
        }
    }

    public static String docListToString(BoundedDocRec cur, String indentPrefix) {
        StringBuffer b = new StringBuffer();
        while (cur != null) {
            BoundedDocRec down = cur.down();
            if (down != null) {
                b.append(docListToString(down, indentPrefix + "  "));
            }
            b.append(indentPrefix + cur.dr);
            b.append('\n');
            cur = cur.next();
        }
        return b.toString();
    }

    public static RecBuffer<MemDocRec> applyDocEdits(List<Edit> edits, DocRec head, long ct) {
        BoundedDocRec bhead = (head == null) ? null : new BoundedDocRec(head, ct);
        RecBuffer<MemDocRec> results = new RecBuffer<MemDocRec>();
        applyDocEdits(edits, bhead, results);
        return results;
    }

    public static void applyDocEdits(List<Edit> edits, BoundedDocRec head, RecBuffer<MemDocRec> result) {
        int editLength = edits.size();
        Bytes prefix = (head == null) ? ArrayBytes.EMPTY_BYTES : head.getPrefix();
        int prefixLen = prefix.getLength();
        BoundedDocRec cur = head;
        int editIdx = 0;
        Edit edit = null;
        if (!edits.isEmpty()) edit = edits.get(0);
        while (edit != null && cur != null) {
            long cmp = cur.compareTo(edit);
            if (cmp < 0) {
                result.append(new MemDocRec(cur.dr));
                cur = cur.next();
            } else {
                if (cur.down() == null) {
                    if (edit.isDelete) {
                        if (cmp != 0) throw new RuntimeException("Unexpected");
                        cur = cur.next();
                    } else {
                        MemDocRec newRec = new MemDocRec(prefix, new SlicedBytes(edit.term, prefixLen), edit.docId, null, null, 0);
                        result.append(newRec);
                    }
                    editIdx++;
                } else {
                    BoundedDocRec subSegment = cur.down();
                    int startIdx = editIdx;
                    for (; editIdx < editLength; editIdx++) {
                        edit = edits.get(editIdx);
                        cmp = cur.compareTo(edit);
                        if (cmp <= 0) break;
                    }
                    applyDocEdits(edits.subList(startIdx, editIdx), subSegment, result);
                    if (edit.isDelete && cmp == 0) {
                        editIdx++;
                    } else {
                        MemDocRec tail = new MemDocRec(cur.dr);
                        tail.down = null;
                        tail.downCt = 0;
                        result.append(tail);
                    }
                    cur = cur.next();
                }
                edit = (editIdx < edits.size()) ? edits.get(editIdx) : null;
            }
        }
        while (cur != null) {
            result.append(new MemDocRec(cur.dr));
            cur = cur.next();
        }
        while (editIdx < edits.size()) {
            edit = edits.get(editIdx++);
            MemDocRec newRec = new MemDocRec(prefix, new SlicedBytes(edit.term, prefixLen), edit.docId, null, null, 0);
            result.append(newRec);
        }
    }

    public static <T extends MemRec> RecBuffer<T> balance(RecBuffer<T> buffer, int leafSize) {
        long ct = buffer.cumulativeCount();
        if (ct < leafSize) return buffer;
        long targetLen = (long) Math.sqrt(ct);
        RecBuffer<T> toWrite = new RecBuffer<T>();
        RecBuffer<T> accumulator = new RecBuffer<T>();
        long accumLen = 0;
        T cur = buffer.head;
        while (cur != null) {
            T clone = (T) cur.clone();
            cur = (T) cur.next();
            if (cur != null) {
                accumulator.append(clone);
                accumLen += cur.cumulativeCount();
            }
            if (accumLen >= targetLen || cur == null) {
                accumLen = 0;
                if (accumulator.head == accumulator.tail) {
                    if (accumulator.head != null) {
                        toWrite.append(accumulator.head);
                        accumulator.clear();
                    }
                } else {
                    long accumCt = accumulator.cumulativeCount();
                    RecBuffer<T> segment = balance(accumulator, leafSize);
                    T head = segment.head;
                    T tail = segment.tail;
                    tail = (T) tail.clone();
                    tail.setDown(head);
                    tail.setDownCt(accumCt - 1);
                    toWrite.append(tail);
                }
                accumulator = new RecBuffer<T>();
            }
            if (cur == null) {
                toWrite.append(clone);
            }
        }
        return toWrite;
    }

    public static void TERM_VER_applyDocEdits(List<Edit> edits, BoundedDocRec head, RecBuffer<MemDocRec> result) {
        int editLength = edits.size();
        Bytes prefix = (head == null) ? ArrayBytes.EMPTY_BYTES : head.getPrefix();
        int prefixLen = prefix.getLength();
        BoundedDocRec cur = head;
        int editIdx = 0;
        Edit edit = null;
        if (!edits.isEmpty()) edit = edits.get(0);
        while (edit != null && cur != null) {
            long cmp = cur.compareTo(edit);
            if (cmp < 0) {
                result.append(new MemDocRec(cur.dr));
                cur = cur.next();
            } else {
                if (cur.down() == null) {
                    if (edit.isDelete) {
                        if (cmp != 0) throw new RuntimeException("Unexpected");
                        cur = cur.next();
                    } else {
                        MemDocRec newRec = new MemDocRec(prefix, new SlicedBytes(edit.term, prefixLen), edit.docId, null, null, 0);
                        result.append(newRec);
                    }
                    editIdx++;
                } else {
                    BoundedDocRec subSegment = cur.down();
                    int startIdx = editIdx;
                    for (; editIdx < editLength; editIdx++) {
                        edit = edits.get(editIdx);
                        cmp = cur.compareTo(edit);
                        if (cmp <= 0) break;
                    }
                    applyDocEdits(edits.subList(startIdx, editIdx), subSegment, result);
                    if (edit.isDelete && cmp == 0) {
                        editIdx++;
                    } else {
                        MemDocRec tail = new MemDocRec(cur.dr);
                        tail.down = null;
                        tail.downCt = 0;
                        result.append(tail);
                    }
                    cur = cur.next();
                }
                edit = (editIdx < edits.size()) ? edits.get(editIdx) : null;
            }
        }
        while (cur != null) {
            result.append(new MemDocRec(cur.dr));
            cur = cur.next();
        }
        while (editIdx < edits.size()) {
            edit = edits.get(editIdx++);
            MemDocRec newRec = new MemDocRec(prefix, new SlicedBytes(edit.term, prefixLen), edit.docId, null, null, 0);
            result.append(newRec);
        }
    }

    @Override
    public void close() {
        data = null;
        header = null;
    }

    private static final byte[] ZERO_BYTES = new byte[0];

    protected void writeNewFile(String filename, long size) {
        try {
            File file = new File(homeDir + File.separatorChar + filename);
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.setLength(size);
            raf.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected BytesInterface openFile(String filename) {
        try {
            File file = new File(homeDir + File.separatorChar + filename);
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            return ChunkedMemoryMappedFile.mapFile(channel);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void reopen() {
        if (!new File(homeDir + File.separatorChar + "header").exists()) {
            version = 0;
            maxDocId = 0;
            rootPos = 0;
            writeNewFile("header", 10 * 8);
            writeNewFile("data", 0);
            forceHeader();
        }
        header = openFile("header");
        parseHeader();
        data = openFile("data");
    }

    protected void parseHeader() {
        version = header.getLong(0);
        maxDocId = header.getLong(1);
        rootPos = header.getLong(2);
    }

    protected void forceHeader() {
        header.putLong(0, version);
        header.putLong(1, maxDocId);
        header.putLong(2, rootPos);
        header.force();
    }

    @Override
    public long getCurrentRevNum() {
        return maxDocId;
    }

    @Override
    public String getHomedir() {
        return homeDir;
    }

    @Override
    public void setHomedir(String homeDir) {
        this.homeDir = homeDir;
        reopen();
    }

    @Override
    public RangeQueryNode getRange(byte[] prefix, byte[] term1, byte[] term2, boolean isExclusive1, boolean isExclusive2, long revNum) {
        return new ImmutableBagRangeQuery();
    }

    @Override
    public QueryNode getTerm(byte[] term, long revNum) {
        return getRange(term, null, null, true, true, revNum);
    }

    @Override
    public long commitNewRev(Collection<EditRec> edits) {
        return 0;
    }
}

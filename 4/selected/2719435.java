package com.onionnetworks.io;

import com.onionnetworks.util.*;
import java.io.*;
import java.util.*;

public class CommitRaf extends FilterRAF {

    RangeSet committed = new RangeSet();

    IOException e;

    HashMap buffers = new HashMap();

    public CommitRaf(RAF raf) {
        super(raf);
    }

    public synchronized void seekAndWrite(long pos, byte[] b, int off, int len) throws IOException {
        if (e != null) {
            throw e;
        }
        if (len != 0) {
            Range r = new Range(pos, pos + len - 1);
            RangeSet rs = new RangeSet();
            rs.add(r);
            if (!committed.intersect(rs).isEmpty()) {
                throw new IOException("Illegal write attempt.  Parts of range " + "already committed. :" + r);
            }
        }
        _raf.seekAndWrite(pos, b, off, len);
        if (len == 0) {
            return;
        }
        fillBlockedBuffers(pos, b, off, len);
    }

    public synchronized void commit(Range r) {
        committed.add(r);
        this.notifyAll();
    }

    public synchronized void commit(RangeSet rs) {
        committed.add(rs);
        this.notifyAll();
    }

    private synchronized void fillBlockedBuffers(long pos, byte[] b, int off, int len) {
        if (buffers.isEmpty()) {
            return;
        }
        Range r = new Range(pos, pos + len - 1);
        for (Iterator it = buffers.keySet().iterator(); it.hasNext(); ) {
            Object key = (Object) it.next();
            Tuple t = (Tuple) buffers.get(key);
            Range r2 = (Range) t.getLeft();
            Buffer buf = (Buffer) t.getRight();
            long min = Math.max(r.getMin(), r2.getMin());
            long max = Math.min(r.getMax(), r2.getMax());
            if (min <= max) {
                System.arraycopy(b, (int) (off + (min - r.getMin())), buf.b, (int) (buf.off + (min - r2.getMin())), (int) (max - min + 1));
            }
        }
    }

    public synchronized void seekAndReadFully(long pos, byte[] b, int off, int len) throws IOException {
        throw new IOException("unsupported operation");
    }

    public synchronized int seekAndRead(long pos, byte[] b, int off, int len) throws IOException {
        boolean directWrite = false;
        Range r = null;
        Object key = new Object();
        while (!isClosed() && e == null && len != 0) {
            if (getMode().equals("r") && (length() == 0 || committed.equals(new RangeSet(new Range(0, length() - 1))))) {
                return _raf.seekAndRead(pos, b, off, len);
            }
            if (r == null) {
                r = new Range(pos, pos + len - 1);
            }
            RangeSet rs = new RangeSet();
            rs.add(r);
            RangeSet avail = committed.intersect(rs);
            Range first = null;
            if (!avail.isEmpty()) {
                first = (Range) avail.iterator().next();
            }
            if (committed.contains(pos)) {
                if (directWrite) {
                    return (int) first.size();
                } else {
                    return _raf.seekAndRead(pos, b, off, (int) first.size());
                }
            } else {
                directWrite = true;
                if (first != null) {
                    r = new Range(pos, first.getMin() - 1);
                }
                buffers.put(key, new Tuple(r, new Buffer(b, off, len)));
                try {
                    this.wait();
                } catch (InterruptedException ie) {
                    throw new InterruptedIOException(ie.getMessage());
                } finally {
                    buffers.remove(key);
                }
            }
        }
        if (e != null) {
            throw e;
        }
        if (isClosed()) {
            throw new IOException("RAF closed");
        }
        if (len == 0) {
            return 0;
        }
        throw new IllegalStateException("Method should have already " + "returned.");
    }

    public synchronized void setException(IOException e) {
        this.e = e;
        this.notifyAll();
    }

    public synchronized void close() throws IOException {
        _raf.close();
        this.notifyAll();
    }
}

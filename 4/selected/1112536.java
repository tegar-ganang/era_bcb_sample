package org.nakedobjects.plugins.file.server;

import java.util.ArrayList;
import java.util.List;

class Lock {

    private Thread write;

    private List<Thread> reads = new ArrayList<Thread>();

    public boolean isWriteLocked() {
        return write != null;
    }

    public void addRead(Thread transaction) {
        reads.add(transaction);
    }

    public void setWrite(Thread transaction) {
        write = transaction;
    }

    public void remove(Thread transaction) {
        if (write == transaction) {
            write = null;
        } else {
            reads.remove(transaction);
        }
    }

    public boolean isEmpty() {
        return write == null && reads.isEmpty();
    }
}

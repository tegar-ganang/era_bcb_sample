package org.geotools.caching.spatialindex;

import java.util.*;

public class RWLock {

    private int active_readers;

    private int waiting_readers;

    private int active_writers;

    private final LinkedList writer_locks = new LinkedList();

    public synchronized void read_lock() {
        if ((active_writers == 0) && (writer_locks.size() == 0)) {
            ++active_readers;
        } else {
            ++waiting_readers;
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public synchronized boolean read_lock_noblock() {
        if ((active_writers == 0) && (writer_locks.size() == 0)) {
            ++active_readers;
            return true;
        }
        return false;
    }

    public synchronized void read_unlock() {
        if (--active_readers == 0) {
            notify_writers();
        }
    }

    public void write_lock() {
        Object lock = new Object();
        synchronized (lock) {
            synchronized (this) {
                boolean okay_to_write = (writer_locks.size() == 0) && (active_readers == 0) && (active_writers == 0);
                if (okay_to_write) {
                    ++active_writers;
                    return;
                }
                writer_locks.addLast(lock);
            }
            try {
                lock.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public synchronized boolean write_lock_noblock() {
        if ((writer_locks.size() == 0) && (active_readers == 0) && (active_writers == 0)) {
            ++active_writers;
            return true;
        }
        return false;
    }

    public synchronized void write_unlock() {
        --active_writers;
        if (waiting_readers > 0) {
            notify_readers();
        } else {
            notify_writers();
        }
    }

    private void notify_readers() {
        active_readers += waiting_readers;
        waiting_readers = 0;
        notifyAll();
    }

    private void notify_writers() {
        if (writer_locks.size() > 0) {
            Object oldest = writer_locks.removeFirst();
            ++active_writers;
            synchronized (oldest) {
                oldest.notify();
            }
        }
    }
}

package org.vrspace.neurogrid;

import java.util.*;
import java.io.*;
import org.vrspace.util.*;

public class MapDB extends DBAdapter implements LoadListener, Runnable {

    HashMap tables = new HashMap();

    protected HashMap cache = new HashMap();

    protected Store store;

    protected boolean active = true;

    public static final int QUEUED = 0;

    public static final int SYNC = 1;

    protected int mode = SYNC;

    protected org.vrspace.util.Queue q = new org.vrspace.util.Queue();

    /** sleep time, millis */
    public long sleep = 1000;

    private Thread thread;

    /**
  Initializes this DBAdapter
  */
    public void init(Store store) throws Exception {
        this.store = store;
        tables.put(UriTriple.class, new TreeSet(new TripleComparator()));
        tables.put(Event.class, new TreeSet(new EventComparator()));
        tables.put(UriDesc.class, new TreeSet(new UriDescComparator()));
        tables.put(Uri.class, new TreeSet(new UriComparator()));
        tables.put(Keyword.class, new TreeSet(new KeywordComparator()));
        tables.put(Node.class, new TreeSet(new NodeComparator()));
        tables.put(NGUser.class, new TreeSet(new NodeComparator()));
        tables.put(Predicate.class, new TreeSet(new PredicateComparator()));
        tables.put(EventType.class, new TreeSet(new EventTypeComparator()));
        store.init(this, this);
        if (mode == QUEUED) {
            startWriter();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new DBShutdown(), "DB ShutdownHook"));
        instance = this;
        Logger.logInfo("MapDB initialized");
    }

    /**
  Creates and starts writer thread with maximum priority
  */
    protected void startWriter() {
        thread = new Thread(this, getClass().getName() + " writer");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
  Shutdown hook - stops writer thread
  */
    private class DBShutdown implements Runnable {

        public void run() {
            stopWriter();
            Logger.logInfo("DB shutdown complete");
        }
    }

    /**
  Interrupts and stops writer thread if any, flushes queue
  */
    protected void stopWriter() {
        if (this.thread != null) {
            active = false;
            thread.interrupt();
            flush();
        }
    }

    /**
  Stores everything from the queue to the store
  */
    private synchronized void flush() {
        int tmp = q.size();
        while (q.size() > 0) {
            NGObject obj = (NGObject) q.remove();
            try {
                store(obj);
            } catch (Throwable t) {
                Logger.logError(t);
            }
        }
        Logger.logDebug("Stored " + tmp + " objects");
    }

    /**
  Writer thread run() impl. Calls <tt>flush()</tt> every <tt>sleep<tt> ms
  */
    public void run() {
        Logger.logDebug("Async DB write running");
        while (active) {
            if (q.size() > 0) {
                flush();
            } else {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ie) {
                    active = false;
                    flush();
                }
            }
        }
        thread = null;
    }

    public NGObject put(NGObject obj) throws Exception {
        if (obj.db_id == 0) {
            NGObject existing = get(obj.getClass(), obj);
            if (existing != null) {
                obj.setFields(existing);
                return existing;
            }
        }
        if (obj instanceof Storable) {
            synchronized (this) {
                try {
                    ((Storable) obj).store(this.store);
                } catch (Throwable t) {
                    Logger.logError("Error storing " + obj + "\n" + obj.toText(), t);
                }
            }
        } else if (mode == QUEUED) {
            loaded(obj);
            q.add(obj);
        } else if (mode == SYNC) {
            loaded(obj);
            store(obj);
        }
        return obj;
    }

    /**
  Delete <tt>obj</tt>
  */
    public synchronized void delete(NGObject obj) throws Exception {
        q.removeElement(obj);
        TreeSet set = getTable(obj.getClass());
        set.remove(obj);
        this.store.delete(obj);
    }

    protected TreeSet getTable(Class what) {
        TreeSet set = (TreeSet) tables.get(what);
        if (set == null) {
            set = new TreeSet();
            tables.put(what, set);
        }
        return set;
    }

    /**
  Stores an object to a reference map, called by Store when an object loads.
  */
    public synchronized void loaded(NGObject obj) {
        if (obj == null) {
            throw new NullPointerException("Cannot store null!");
        }
        DBRef ref = new DBRef(obj, this);
        if (obj.db_id == 0) throw new RuntimeException("Object ID is 0");
        if (obj == null) {
            throw new NullPointerException("Cannot store null!!!");
        }
        obj.db = this;
        obj.init();
        if (cache.get(obj.getID()) == null) {
            cache.put(obj.getID(), ref);
        }
        TreeSet set = getTable(obj.getClass());
        set.add(ref);
        putCnt++;
    }

    /**
  Stores <tt>obj</tt> to current store
  */
    protected synchronized void store(NGObject obj) {
        try {
            this.store.store(obj);
        } catch (Exception e) {
            Logger.logError(e);
        }
    }

    /**
  Returns number of stored instances of this class.
  */
    public synchronized int count(Class what) {
        TreeSet set = getTable(what);
        return set.size();
    }

    /**
  Returns total swap size
  */
    public synchronized long size() {
        long ret = 0;
        Iterator it = tables.values().iterator();
        while (it.hasNext()) {
            TreeSet set = (TreeSet) it.next();
            ret += set.size();
        }
        return ret;
    }

    /**
  Returns the iterator on all instances of a class
  */
    public synchronized FilteredIterator iterator(Class what) {
        TreeSet set = getTable(what);
        return new FilteredIterator(set);
    }

    /**
  Returns the iterator on all instances of a class
  */
    public synchronized FilteredIterator iterator(Class what, Query q) {
        TreeSet set = getTable(what);
        return new FilteredIterator(set, q);
    }

    /**
  Retrieves a subset of a class
  */
    public synchronized SortedSet subSet(Class what, Object start, Object end) {
        TreeSet set = getTable(what);
        if (set == null) {
            throw new IllegalArgumentException("Cannot get subset of " + what.getName());
        }
        return set.subSet(start, end);
    }

    /**
  Utility method
  */
    public NGObject get(Class what, NGObject value) {
        NGObject ret = null;
        SortedSet tmp = subSet(what, value, value.next());
        Iterator it = new FilteredIterator(tmp);
        if (it.hasNext()) {
            ret = (NGObject) it.next();
            if (it.hasNext()) {
                throw new RuntimeException("Multiple values encountered! " + what.getName() + " " + value);
            }
        }
        return ret;
    }

    public synchronized boolean contains(Class what, long id) {
        boolean ret = (cache.get(new ID(what, id)) != null);
        return ret;
    }

    /**
  Called from DBRef when SoftReference.get() returns null, that is - when gc expired the object
  */
    public synchronized NGObject get(Class what, long id) {
        NGObject ret = null;
        DBRef ref = (DBRef) cache.get(new ID(what, id));
        if (ref != null) ret = (NGObject) ref.get();
        getCnt++;
        if (ret == null) {
            try {
                ret = this.store.retrieve(what, id);
                if (ret != null) {
                    reloadCnt++;
                }
            } catch (Exception e) {
                Logger.logError("Store internal error: ", e);
            }
        } else {
            hitCnt++;
        }
        return ret;
    }

    /**
  Set new write mode: SYNC or QUEUED
  */
    public void setMode(int mode) {
        if (mode != this.mode) {
            this.mode = mode;
            if (mode == SYNC) {
                stopWriter();
            } else if (mode == QUEUED) {
                startWriter();
            } else {
                throw new RuntimeException("Invalid writer mode: " + mode);
            }
        }
    }

    public Object[] getRange(Class what, NGObject value) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }
}

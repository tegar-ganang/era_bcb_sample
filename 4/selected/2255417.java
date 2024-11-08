package com.incendiaryblue.util;

import java.util.*;

/**
 * The ObjectPool class is designed to provide a pool of identical objects which
 * can be made available to a class, reducing the need for synchronisation on
 * stateful static objects.
 *
 * The performance benefit is a reduction in the necessity for synchronisation.
 * While using a single static stateful object in code may require
 * synchronisation on every use of it, the ObjectPool is synchronised internally
 * and only requires synchronisation on getting and returning objects to the
 * pool.
 *
 * ObjectPool is ideal for subclassing anonymously. To do this, simply
 * implement the getNewObject() abstract method.
 *
 * This class provides functionality for removal of objects from the pool that
 * are no longer valid, e.g. sockets that have unexpectedly closed. The pool
 * ensures that no object that is currently in use is disposed of until it is
 * released back into the pool.
 *
 */
public abstract class ObjectPool {

    protected static Debug debug = new Debug(ObjectPool.class);

    private Set usedPool;

    private LinkedList freePool;

    private Map objectEntryMap;

    private Thread creator;

    private int maxSize;

    private int waiting = 0;

    private int maxCreateAttempts = 5;

    private boolean createFailed = false;

    /**
	 * Create an object pool with a default number of objects (default = 5)
	 */
    public ObjectPool() {
        this(5);
    }

    /**
	 * Create an object pool initalized with the given number of objects.
	 */
    public ObjectPool(int maxSize) {
        this.maxSize = maxSize;
        usedPool = new HashSet();
        freePool = new LinkedList();
        objectEntryMap = new HashMap();
    }

    /**
	 * Set the maximum number of attempts the pool should make to create a new
	 * object. If an object is not created within this number of attempts, a
	 * RuntimeException will be thrown.
	 */
    public void setMaxAttempts(int numAttempts) {
        this.maxCreateAttempts = numAttempts;
    }

    /**
	 * This method returns an object from the pool. The calling thread then has
	 * exclusive access to the object until it releases it by calling
	 * releaseObject().
	 */
    public synchronized Object getObject() {
        ObjectPoolEntry o = null;
        int numAttempts = 0;
        while (freePool.isEmpty()) {
            if (objectEntryMap.size() >= this.maxSize) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            } else {
                try {
                    Object obj = getNewObject();
                    addNew(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (++numAttempts >= maxCreateAttempts) {
                        throw new RuntimeException("Unable to create new object in pool.");
                    }
                }
            }
        }
        o = (ObjectPoolEntry) freePool.removeFirst();
        usedPool.add(o);
        debug.write("getObject() got clean object...");
        debug.write(" - " + freePool.size() + " objects in free pool.");
        debug.write(" - " + usedPool.size() + " objects in used pool.");
        debug.write(" - " + objectEntryMap.size() + " objects total.");
        return o.object;
    }

    /**
	 * Release the Object o back into the ObjectPool for use by another thread.
	 * If another thread has called removeObject() on the pool at any time while
	 * o has been in use, o will be removed from the pool when this method is
	 * called. This is because o is potentially an invalid object, and should
	 * not be obtained by another thread.
	 */
    public synchronized void releaseObject(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
        ObjectPoolEntry entry = (ObjectPoolEntry) objectEntryMap.get(o);
        if (entry == null) {
            throw new IllegalArgumentException("Object not from this pool");
        }
        releaseEntry(entry);
    }

    private synchronized void releaseEntry(ObjectPoolEntry entry) {
        if (!usedPool.remove(entry)) {
            throw new IllegalArgumentException("Object not from this pool, or releaseObject() already called");
        }
        if (!entry.dirty) {
            debug.write("releaseEntry() returning clean object to free pool...");
            freePool.addLast(entry);
            debug.write(" - " + freePool.size() + " objects in free pool.");
            debug.write(" - " + usedPool.size() + " objects in used pool.");
            debug.write(" - " + objectEntryMap.size() + " objects total.");
            if (waiting > 0) {
                notify();
            }
        } else {
            debug.write("releaseEntry() caught dirty used object - removing from pool...");
            objectEntryMap.remove(entry.object);
            try {
                dispose(entry.object);
            } catch (Exception e) {
                System.err.println("Error removing object from pool:");
                e.printStackTrace();
            }
        }
    }

    /**
	 * Removes an object from the pool. The parameter o must reference an object
	 * that is 'owned' by the calling thread (ie the calling thread has called
	 * getObject() to obtain o from the pool). When this method is called, o
	 * is automatically released and removed from the pool, all free objects
	 * in the pool are removed from the pool, and all objects currently in use
	 * by other threads are marked for removal, to be removed when they are
	 * released by their respective threads. This means that calling this
	 * method will eventually cause ALL objects currently in the pool to be
	 * refreshed.
	 */
    public synchronized void removeObject(Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
        ObjectPoolEntry entry = (ObjectPoolEntry) objectEntryMap.get(o);
        if (entry == null) {
            throw new IllegalArgumentException("Object not from this pool");
        }
        if (entry.dirty) {
            debug.write("object already marked for removal... releasing...");
            releaseEntry(entry);
            return;
        }
        removeAll();
        releaseEntry(entry);
    }

    /**
	 * Removes all objects from the pool. Free objects are immediately removed
	 * and used objects are marked for removal when they are released by the
	 * using threads.
	 */
    public synchronized void removeAll() {
        ObjectPoolEntry entry;
        for (Iterator i = freePool.iterator(); i.hasNext(); ) {
            entry = (ObjectPoolEntry) i.next();
            i.remove();
            objectEntryMap.remove(entry.object);
            try {
                debug.write("removeAll() removing free pool object...");
                dispose(entry.object);
            } catch (Exception e) {
                System.err.println("Error disposing of object in pool:");
                e.printStackTrace();
            }
        }
        for (Iterator i = usedPool.iterator(); i.hasNext(); ) {
            debug.write("removeAll() marking used object dirty for removal...");
            entry = (ObjectPoolEntry) i.next();
            entry.dirty = true;
        }
        return;
    }

    /**
	 * This method is called on an object in the pool when it is about to be
	 * removed from the pool. Subclasses should override this method to provide
	 * special requirements for object disposal, e.g. closing a socket or
	 * database connection.
	 */
    protected void dispose(Object o) throws Exception {
        return;
    }

    public synchronized void destroy() {
        removeAll();
        for (Iterator i = usedPool.iterator(); i.hasNext(); ) {
            try {
                dispose(((ObjectPoolEntry) i.next()).object);
            } catch (Exception e) {
                System.err.println("Error disposing of object in pool:");
                e.printStackTrace();
            }
        }
    }

    public void finalize() {
        this.destroy();
    }

    private synchronized void addNew(Object o) {
        ObjectPoolEntry entry = new ObjectPoolEntry(o);
        objectEntryMap.put(o, entry);
        freePool.add(entry);
        debug.write("addNew() adding new object to pool...");
        if (waiting > 0) {
            notify();
        }
    }

    /**
	 * This method must be implemented by a subclass. It returns an object to be
	 * added to the pool. This method has protected access because it should
	 * only be called from within this class or its subclasses. While a subclass
	 * could declare the method public, this would negate the purpose of the
	 * pool, which manages the creation of objects for the developer when it is
	 * required.
	 */
    protected abstract Object getNewObject() throws Exception;

    class ObjectPoolEntry {

        public Object object;

        public boolean dirty = false;

        public ObjectPoolEntry(Object o) {
            this.object = o;
        }
    }
}

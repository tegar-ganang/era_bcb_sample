package net.sf.compositor.util;

import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * An abstraction of what is required for a pool of some resource.
 * When a resource is required, get one from here.
 * An existing resource is returned if one is free, or else a new resource is created.
 * When the resource is no longer needed, return it to the pool with {@link #freeElement(Object)}.
 * Implementations of specific pools extend this class and provide versions of {@link #newElement()} and {@link #timedOut(ResourcePool.Holder)}.
 * Beware of over riding the constructors, as this will stop the tidying up
 * thread being created.  In this case, some other method could be used for calling {@link #flushFreeElements()},
 * or a thread created elsewhere and given the resource pool to {@link #run()}.  This should be a daemon thread.
 * <br>
 * Make sure you synchronize on <code>this</code> (i.e. the pool) if you need to:
 * <ul>
 *	<li>get the size of the pool accurately;
 *	<li>change the size of the pool;
 * </ul>
 */
public abstract class ResourcePool<R> implements Runnable {

    /** Logging info */
    private static String s_thisClass = StackProbe.getMyClassName() + '.';

    static Log s_log = Log.getInstance();

    /** Holds {@link Holder}s for the free elements in this pool. */
    protected List<Holder<R>> m_freePool = new Vector<Holder<R>>();

    /** Holds the busy elements in this pool (not {@link Holder}s). */
    protected List<R> m_busyPool = new Vector<R>();

    /** Holds the time to sleep between calling {@link #flushFreeElements()}. */
    private int m_sleepTime;

    /** Holds thread that calls {@link #flushFreeElements()}. */
    private Thread m_tidyUp;

    /** Holds maximum pool size. */
    private int m_poolLimit;

    /**
	 * Beware of over-riding the constructors of this class, as this will stop
	 * the tidying up thread being created.  In this case, some other method
	 * could be used for calling {@link #flushFreeElements()}, or a thread
	 * created elsewhere and given the resource pool to {@link #run()}.  This
	 * should be a daemon thread.
	 *
	 * @param sleepTime  Sets milliseconds tidying up thread sleeps between
	 *                   tidy ups.  A <code>sleepTime</code> of <code>0</code>
	 *                   prevents a tidying up thread being created.  See
	 *                   above!
	 * @param poolLimit  Limit on the size of this pool.  For an unlimited
	 *                   pool, use a <code>maxSize</code> of <code>0</code>.
	 * @see #flushFreeElements()
	 * @see #run()
	 * @see #ResourcePool()
	 */
    protected ResourcePool(final int sleepTime, final int poolLimit) {
        super();
        final String thisMethod = s_thisClass + "<init>(" + sleepTime + "): ";
        if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "start");
        if (sleepTime > 0) {
            m_sleepTime = sleepTime;
            if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "creating tidy up thread...");
            m_tidyUp = new Thread(this);
            if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "making tidy up thread a daemon...");
            m_tidyUp.setDaemon(true);
            if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "starting tidy up thread...");
            m_tidyUp.start();
        }
        if (poolLimit > 0) {
            m_poolLimit = poolLimit;
        } else {
            m_poolLimit = 0;
        }
        if (s_log.isOnVerbose()) s_log.write(Log.VERBOSE, thisMethod + "end");
    }

    /**
	 * Constructs a pool with no size limit and a tidying up thread sleep time
	 * of something over a minute.  Beware of over riding the constructors of
	 * this class: see comments under {@link #ResourcePool(int, int)}.
	 *
	 * @see #ResourcePool(int,int)
	 */
    protected ResourcePool() {
        this(61440, 0);
    }

    /**
	 * Get a resource from the pool.
	 *
	 * @return  a free element from the pool (which may be a newly added
	 *          element if none were free), or <code>null</code> if a new
	 *          element could not be created because the pool is full or there
	 *          is some other problem creating a new element.
	 */
    public R getElement() {
        final String thisMethod = s_thisClass + "getElement: ";
        final boolean loggingVerbose = s_log.isOnVerbose();
        if (loggingVerbose) s_log.write(Log.VERBOSE, thisMethod + "start");
        R result = null;
        if (loggingVerbose) s_log.write(Log.VERBOSE, thisMethod + "looking for free element...");
        synchronized (m_freePool) {
            if (m_freePool.size() > 0) {
                result = m_freePool.remove(0).element;
            }
        }
        if (result == null) {
            if (m_poolLimit == 0 || getPoolSize() < m_poolLimit) {
                if (loggingVerbose) s_log.write(Log.VERBOSE, thisMethod + "creating new element...");
                try {
                    result = newElement();
                } catch (BadResourcePoolElement e) {
                    s_log.error(thisMethod + e);
                    result = null;
                }
            } else {
                if (loggingVerbose) s_log.write(Log.VERBOSE, thisMethod + "not creating new element - pool full. 8~(");
            }
        }
        if (result != null) {
            if (loggingVerbose) s_log.write(Log.VERBOSE, thisMethod + "putting element in busy pool...");
            m_busyPool.add(result);
        }
        if (loggingVerbose) s_log.write(Log.VERBOSE, thisMethod + "end");
        return result;
    }

    /**
	 * Tells the pool that the resource is no longer in use.
	 *
	 * @param element The resource pool element to free
	 */
    public synchronized void freeElement(final Object element) throws BadResourcePoolElement {
        boolean badElement = true;
        for (int i = 0, busyPoolSize = m_busyPool.size(); i < busyPoolSize; i++) {
            final R thisElement = m_busyPool.get(i);
            if (element == thisElement) {
                m_busyPool.remove(i);
                m_freePool.add(new Holder<R>(thisElement, new Date()));
                badElement = false;
                break;
            }
        }
        if (badElement) {
            throw new BadResourcePoolElement();
        }
    }

    /**
	 * Subclasses must provide some method of creating elements.
	 *
	 * @return a resource to add to the pool.
	 * @throws BadResourcePoolElement to indicate a problem creating a new element
	 */
    protected abstract R newElement() throws BadResourcePoolElement;

    /**
	 * Subclasses must provide some method of determining whether an element should be
	 * removed from the pool when it is idle.  This will normally be after some period
	 * of inactivity, but any criteria could be used.  This method is called by a
	 * thread created by the pool, and will only be called for free elements.
	 *
	 * @param thisElementHolder The wrapper for the element to be considered for removal
	 * @return Whether the pool should remove this element
	 * @see #ResourcePool(int,int)
	 * @see #run()
	 * @see #flushFreeElements()
	 */
    protected abstract boolean timedOut(Holder<R> thisElementHolder);

    /**
	 * Don't call this method.  Called by the tidy up thread - calls <code>{@link #flushFreeElements()}</code>.
	 */
    public void run() {
        for (; ; ) {
            try {
                Thread.sleep(m_sleepTime);
            } catch (InterruptedException e) {
            }
            flushFreeElements();
        }
    }

    /**
	 * Tidies up the pool, freeing unused elements
	 */
    public synchronized void flushFreeElements() {
        for (int i = 0, freePoolSize = m_freePool.size(); i < freePoolSize; i++) {
            if (timedOut(m_freePool.get(i))) {
                m_freePool.remove(i);
            }
        }
    }

    /**
	 * Does exactly what it says on the tin.
	 *
	 * @see #m_freePool
	 * @see #m_busyPool
	 */
    public int getPoolSize() {
        return m_freePool.size() + m_busyPool.size();
    }

    /**
	 * Does exactly what it says on the tin.
	 *
	 * @see #m_busyPool
	 */
    public int getBusyPoolSize() {
        return m_busyPool.size();
    }

    /**
	 * Does exactly what it says on the tin.
	 *
	 * @see #m_freePool
	 */
    public int getFreePoolSize() {
        return m_freePool.size();
    }

    /**
	 * Does exactly what it says on the tin.
	 *
	 * @see #m_sleepTime
	 */
    public int getSleepTime() {
        return m_sleepTime;
    }

    /**
	 * Does exactly what it says on the tin.
	 *
	 * @see #m_sleepTime
	 */
    public void setSleepTime(final int sleepTime) {
        m_sleepTime = sleepTime;
    }

    /**
	 * Does exactly what it says on the tin.
	 *
	 * @see #m_poolLimit
	 */
    public int getPoolLimit() {
        return m_poolLimit;
    }

    /**
	 * Does exactly what it says on the tin, but note that subclasses should
	 * have some way of dealing with the pool limit getting smaller, as the
	 * current size may be bigger than the new maximum. You may want to over
	 * ride this method, calling <code>super.setPoolLimit(...)</code>, and then
	 * handling the size reduction appropriately. It is possible to ignore the
	 * excess of elements, as no new elements will be created by
	 * {@link #getElement()} while the size is greater than the maximum.
	 *
	 * @return the old pool size limit
	 */
    protected int setPoolLimit(final int newLimit) {
        final int result = m_poolLimit;
        synchronized (this) {
            m_poolLimit = newLimit;
        }
        return result;
    }

    /**
	 * Wraps elements in the free pool so we can see how old they are.
	 */
    protected class Holder<R> {

        protected R element;

        protected Date lastInUse;

        protected Holder(final R element, final Date lastInUse) {
            this.element = element;
            this.lastInUse = lastInUse;
        }
    }
}

package symore.util;

import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;

/**
 * Implementation of a semaphore with shared and exclusive locks.
 * A shared lock is granted if no exclusive lock is currently granted.
 * An exclusive lock can only be acquired if no other lock is granted.
 * Maintains a priority queue to handle locks in the order they come in.
 * A shared lock is postponed if an exclusive lock with higher priority is waiting
 * in the queue in order not to starve processes waiting for exclusive locks.
 * @author Frank Bregulla, Manuel Scholz
 *
 */
public class ReaderWriterSemaphore {

    Logger logger = Logger.getLogger(this.getClass());

    private Thread currentWriter;

    private SortedMap reader, writer;

    private int prio = 0;

    private int readerCount = 0;

    private int writerPrio = 5;

    public ReaderWriterSemaphore() {
        reader = new TreeMap();
        writer = new TreeMap();
    }

    public synchronized void acquireShared() {
        try {
            logger.debug("Thread " + Thread.currentThread().getName() + " tries to acquire shared lock...");
            int myPrio = prio++;
            reader.put(new Integer(myPrio), Thread.currentThread());
            while ((currentWriter != null) || ((!writer.isEmpty()) && (myPrio > ((Integer) writer.firstKey()).intValue()))) {
                wait();
            }
            reader.remove(new Integer(myPrio));
            readerCount++;
            if (!reader.isEmpty()) notifyAll();
            logger.debug("Thread " + Thread.currentThread().getName() + " acquired shared lock");
        } catch (InterruptedException e) {
        }
    }

    public synchronized void acquireExclusive() {
        try {
            logger.debug("Thread " + Thread.currentThread().getName() + " tries to acquire exclusive lock");
            int myPrio = prio + writerPrio;
            prio++;
            writer.put(new Integer(myPrio), Thread.currentThread());
            while ((currentWriter != null) || (readerCount > 0) || ((!reader.isEmpty()) && (prio >= ((Integer) reader.firstKey()).intValue()))) {
                wait();
            }
            writer.remove(new Integer(myPrio));
            currentWriter = Thread.currentThread();
            logger.debug("Thread " + Thread.currentThread().getName() + " acquired exclusive lock");
        } catch (InterruptedException e) {
        }
    }

    public synchronized void releaseShared() {
        if ((currentWriter != null) || (readerCount <= 0)) throw new IllegalStateException();
        readerCount--;
        logger.debug("Thread " + Thread.currentThread().getName() + " released shared lock");
        notifyAll();
    }

    public synchronized void releaseExclusive() {
        if (!Thread.currentThread().equals(currentWriter)) throw new IllegalStateException();
        currentWriter = null;
        logger.debug("Thread " + Thread.currentThread().getName() + " released excusive lock");
        notifyAll();
    }
}

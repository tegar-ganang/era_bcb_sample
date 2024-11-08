package org.kineticsystem.commons.data.demo;

import java.util.concurrent.locks.*;
import org.kineticsystem.commons.data.model.*;

/**
 * This class is used to measure the performance of an <tt>ActiveList</tt>.
 * It measures the number of events sent and the number of atomic changes (add,
 * delete, update) executed by the list in a given interval of time.
 * @author Giovanni Remigi
 * @version $Revision: 8 $
 */
public class PerformanceMonitor extends Thread implements ActiveListListener {

    /** Lock used to protect a shared resource. */
    private ReadWriteLock usrLock;

    /** Lock used to protect a shared resource. */
    private ReadWriteLock threadLock;

    /**
     * The interval time in milliseconds used to monitor the total number of
     * changes.
     */
    private long interval;

    /** The monitoring information since the last update. */
    private Info info;

    /** 
     * The monitoring information obtained from the last monitored time 
     * interval.
     */
    private Info avgInfo;

    /**
     * Constructor.
     * @param interval The monitoring time interval in milliseconds.
     */
    public PerformanceMonitor(long interval) {
        this.interval = interval;
        this.info = new Info();
        this.avgInfo = new Info();
        this.usrLock = new ReentrantReadWriteLock();
        this.threadLock = new ReentrantReadWriteLock();
    }

    /** 
     * Return the number of changes and events in the given time interval.
     * @return The number of changes and events in the given time interval.
     */
    public Info getInfo() {
        usrLock.readLock().lock();
        Info tmp = avgInfo;
        usrLock.readLock().unlock();
        return tmp;
    }

    /**
     * Collect <tt>ActiveListEvent</tt> and monitor the number of changes in
     * the given time interval.
     * @param event The collected event.
     */
    public void contentsChanged(ActiveListEvent event) {
        threadLock.writeLock().lock();
        info.setChanges(info.getChanges() + event.getY() - event.getX() + 1);
        info.setEvents(info.getEvents() + 1);
        threadLock.writeLock().unlock();
    }

    /** {@inheritDoc} */
    public void run() {
        while (!isInterrupted()) {
            threadLock.writeLock().lock();
            usrLock.writeLock().lock();
            avgInfo = info;
            info = new Info();
            threadLock.writeLock().unlock();
            usrLock.writeLock().unlock();
            try {
                sleep(interval);
            } catch (InterruptedException ex) {
                interrupt();
            }
        }
    }

    /**
     * This is an information wrapper. It stores the number of events sent and
     * the number of changes executed by the monitored list in the given time
     * interval.
     */
    public class Info {

        /** The number of events setn by the list. */
        private int events;

        /** The number of changes (add, delete, update) made over the list. */
        private int changes;

        /** Constructor. */
        public Info() {
            this.events = 0;
            this.changes = 0;
        }

        public String toString() {
            StringBuffer str = new StringBuffer();
            str.append("[");
            str.append("events=" + events);
            str.append(",");
            str.append("changes=" + changes);
            str.append("]");
            return str.toString();
        }

        public void setEvents(int events) {
            this.events = events;
        }

        public int getEvents() {
            return events;
        }

        public void setChanges(int changes) {
            this.changes = changes;
        }

        public int getChanges() {
            return changes;
        }
    }
}

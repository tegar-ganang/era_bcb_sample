package com.kni.etl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Insert the type's description here. Creation date: (4/17/2002 6:04:43 PM)
 * 
 * @author: Administrator
 */
public class ReadWriteLock implements Serializable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3546076973559265077L;

    /** The TRACE. */
    public static boolean TRACE = false;

    /** The given locks. */
    private int givenLocks = 0;

    /** The waiting writers. */
    private int waitingWriters = 0;

    /** The mutex. */
    private transient Object mutex = new Object();

    /**
     * Gets the read lock.
     * 
     * @return the read lock
     */
    public void getReadLock() {
        synchronized (this.mutex) {
            try {
                while ((this.givenLocks == -1) || (this.waitingWriters != 0)) {
                    if (ReadWriteLock.TRACE) {
                        System.out.println(Thread.currentThread().toString() + "waiting for readlock");
                    }
                    this.mutex.wait();
                }
            } catch (java.lang.InterruptedException e) {
                System.out.println(e);
            }
            this.givenLocks++;
            if (ReadWriteLock.TRACE) {
                System.out.println(Thread.currentThread().toString() + " got readlock, GivenLocks = " + this.givenLocks);
            }
        }
    }

    /**
     * Gets the write lock.
     * 
     * @return the write lock
     */
    public void getWriteLock() {
        synchronized (this.mutex) {
            this.waitingWriters++;
            try {
                while (this.givenLocks != 0) {
                    if (ReadWriteLock.TRACE) {
                        System.out.println(Thread.currentThread().toString() + "waiting for writelock");
                    }
                    this.mutex.wait();
                }
            } catch (java.lang.InterruptedException e) {
                System.out.println(e);
            }
            this.waitingWriters--;
            this.givenLocks = -1;
            if (ReadWriteLock.TRACE) {
                System.out.println(Thread.currentThread().toString() + " got writelock, GivenLocks = " + this.givenLocks);
            }
        }
    }

    /**
     * Release lock.
     */
    public void releaseLock() {
        synchronized (this.mutex) {
            if (this.givenLocks == 0) {
                return;
            }
            if (this.givenLocks == -1) {
                this.givenLocks = 0;
            } else {
                this.givenLocks--;
            }
            if (ReadWriteLock.TRACE) {
                System.out.println(Thread.currentThread().toString() + " released lock, GivenLocks = " + this.givenLocks);
            }
            this.mutex.notifyAll();
        }
    }

    /**
     * Checks if is locked.
     * 
     * @return true, if is locked
     */
    public boolean isLocked() {
        boolean res = false;
        synchronized (this.mutex) {
            if (this.givenLocks != 0) {
                res = true;
            }
            if (ReadWriteLock.TRACE) {
                System.out.println(Thread.currentThread().toString() + " checked for lock, GivenLocks = " + this.givenLocks);
            }
        }
        return res;
    }

    /**
     * Write object.
     * 
     * @param s the s
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    /**
     * Read object.
     * 
     * @param s the s
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void readObject(ObjectInputStream s) throws IOException {
        try {
            s.defaultReadObject();
            this.mutex = new Object();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

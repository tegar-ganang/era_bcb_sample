package org.ws4d.java.util.concurrency;

import java.util.Enumeration;
import java.util.Vector;
import org.ws4d.java.logging.Log;

/**
 * Multiple Reader, Single Writer - Lock implementation.
 */
public final class ReadWriteLock extends AbstractLock {

    private Vector waiters = new Vector();

    private int firstWriter = Integer.MAX_VALUE;

    /**
	 * Returns index of a thread in the waiters queue.
	 * 
	 * @param t the thread.
	 * @return index of a thread in the waiters queue.
	 */
    private int getIndex(Thread t) {
        int size = waiters.size();
        for (int i = 0; i < size; i++) {
            Node n = (Node) waiters.elementAt(i);
            if (n.thread == t) {
                return i;
            }
        }
        return -1;
    }

    /**
	 * Returns index of the first writer thread in the waiters queue.
	 * 
	 * @param t the thread.
	 * @return index of the first writer thread in the waiters queue.
	 */
    private int firstWriter() {
        int size = waiters.size();
        for (int i = 0; i < size; i++) {
            Node n = (Node) waiters.elementAt(i);
            if (n.state == Node.WRITER) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
	 * Returns index of the last granted thread in the waiters queue.
	 * 
	 * @param t the thread.
	 * @return index of the last granted thread in the waiters queue.
	 */
    private int lastGranted() {
        for (int i = waiters.size() - 1; i > -1; i--) {
            Node n = (Node) waiters.elementAt(i);
            if (n.granted == true) {
                return i;
            }
        }
        return -1;
    }

    /**
	 * Upgrade node of thread at position idx to a writer.
	 *
	 * @param idx index of the thread.
	 * @param n Node meta object of this thread.
	 * 
	 * @return new index of thread 
	 */
    private int upgrade(int idx, Node n) {
        int lastGranted = lastGranted();
        n.state = Node.WRITER;
        firstWriter = lastGranted;
        if (lastGranted > 0) {
            waiters.removeElementAt(idx);
            waiters.insertElementAt(n, lastGranted);
            n.granted = false;
            return lastGranted;
        }
        return idx;
    }

    /**
	 * Acquire a "read" lock.
	 */
    public synchronized void lockRead() {
        Thread current = Thread.currentThread();
        int idx = getIndex(current);
        Node n = null;
        if (idx != -1) {
            n = (Node) waiters.elementAt(idx);
        } else {
            n = new Node(current, Node.READER);
            waiters.addElement(n);
            idx = waiters.size() - 1;
        }
        while (idx > firstWriter) {
            try {
                long past = System.currentTimeMillis();
                Log.warn(current + " | Must wait for read lock!! Thread: " + current);
                wait();
                long now = System.currentTimeMillis();
                Log.warn(current + " | Waited for " + (now - past) + "ms to receive read lock. Thread: " + current);
            } catch (InterruptedException e) {
            }
            idx = getIndex(current);
        }
        n.acquires++;
        n.granted = true;
    }

    /**
	 * Acquire a "write" lock.
	 */
    public synchronized void lockWrite() {
        Thread current = Thread.currentThread();
        int idx = getIndex(current);
        Node n = null;
        if (idx != -1) {
            n = (Node) waiters.elementAt(idx);
            if (n.state != Node.WRITER) {
                Log.debug("Upgrading from read to write lock. Node: " + n);
                idx = upgrade(idx, n);
            }
        } else {
            n = new Node(current, Node.WRITER);
            waiters.addElement(n);
            idx = waiters.size() - 1;
            if (firstWriter == Integer.MAX_VALUE) {
                firstWriter = idx;
            }
        }
        while (idx != 0) {
            try {
                long past = System.currentTimeMillis();
                Log.warn(current + " | Must wait for write lock!! Thread: " + current);
                wait();
                long now = System.currentTimeMillis();
                Log.warn(current + " | Waited for " + (now - past) + "ms to receive write lock. Thread: " + current);
            } catch (Exception e) {
            }
            idx = getIndex(current);
        }
        n.acquires++;
        n.granted = true;
    }

    /**
	 * Release the lock.
	 */
    public synchronized void unlock() {
        Thread current = Thread.currentThread();
        Node n;
        int idx = getIndex(current);
        if (idx > firstWriter) {
            throw new IllegalArgumentException("Lock not held!");
        }
        n = (Node) waiters.elementAt(idx);
        n.acquires--;
        if (n.acquires == 0) {
            waiters.removeElementAt(idx);
            if (idx <= firstWriter) {
                firstWriter = firstWriter();
            }
            notifyAll();
        }
    }
}

/**
 * Node to contain informations about waiting threads.
 */
final class Node {

    static final int READER = 0;

    static final int WRITER = 1;

    Thread thread;

    int state;

    int acquires;

    boolean granted;

    /**
	 * Constructs a node.
	 * 
	 * @param current thread of the node.
	 * @param state state of the node.
	 */
    Node(Thread current, int state) {
        this.thread = current;
        this.state = state;
        this.granted = false;
        acquires = 0;
    }

    /**
	 * Returns human-readable description of the node.
	 */
    public String toString() {
        return new String("{" + thread + " " + thread.hashCode() + ", s=" + state + ", a=" + acquires + ", g=" + granted + "}");
    }
}

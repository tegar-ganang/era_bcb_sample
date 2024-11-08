package org.eclipse.core.internal.registry;

/**
 * Monitor ensuring no more than one writer working concurrently.
 * Multiple readers are allowed to perform simultaneously.
 * 
 * This class was borrowed from org.eclipse.jdt.internal.core.search.indexing. 
 */
public class ReadWriteMonitor {

    /**
	 * <0 : writing (cannot go beyond -1, i.e one concurrent writer)
	 * =0 : idle
	 * >0 : reading (number of concurrent readers)
	 */
    private int status = 0;

    private Thread writeLockowner;

    /**
	 * Concurrent reading is allowed
	 * Blocking only when already writing.
	 */
    public synchronized void enterRead() {
        if (writeLockowner == Thread.currentThread()) return;
        while (status < 0) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        status++;
    }

    /**
	 * Only one writer at a time is allowed to perform
	 * Blocking only when already writing or reading.
	 */
    public synchronized void enterWrite() {
        if (writeLockowner != Thread.currentThread()) {
            while (status != 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            writeLockowner = Thread.currentThread();
        }
        status--;
    }

    /**
	 * Only notify waiting writer(s) if last reader
	 */
    public synchronized void exitRead() {
        if (writeLockowner == Thread.currentThread()) return;
        if (--status == 0) notifyAll();
    }

    /**
	 * When writing is over, all readers and possible
	 * writers are granted permission to restart concurrently
	 */
    public synchronized void exitWrite() {
        if (writeLockowner != Thread.currentThread()) throw new IllegalStateException("Current owner is " + writeLockowner);
        if (++status == 0) {
            writeLockowner = null;
            notifyAll();
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.hashCode());
        if (status == 0) {
            buffer.append("Monitor idle ");
        } else if (status < 0) {
            buffer.append("Monitor writing ");
        } else if (status > 0) {
            buffer.append("Monitor reading ");
        }
        buffer.append("(status = ");
        buffer.append(this.status);
        buffer.append(")");
        return buffer.toString();
    }
}

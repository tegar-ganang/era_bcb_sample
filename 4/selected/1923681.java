package net.assimilator.watch;

import java.util.*;
import java.io.*;
import java.util.logging.*;

/**
 * Provides a queued approach to archive a Watch record
 */
public abstract class QueuedArchive implements Archivable {

    private List archiveQueue = Collections.synchronizedList(new LinkedList());

    private transient Thread archiveChild;

    private static Logger logger = Logger.getLogger("com.sun.sio.watch");

    /**
     * Creates new QueuedArchive
     */
    public QueuedArchive() {
        ArchiveWriter writeThread = new ArchiveWriter(this);
        writeThread.start();
    }

    /**
     * Performs the actual write to the underlying resource
     * 
     * @param calculable the Calculable record to archive
     */
    protected abstract void archiveToResource(Calculable calculable) throws IOException;

    /**
     * Closes the underlying Resource
     */
    protected abstract void closeResource();

    /**
     * Closes the archive
     */
    public synchronized void close() {
        archiveChild.interrupt();
        try {
            wait();
        } catch (InterruptedException e) {
            logger.warning("Interrupted waiting for close");
        }
    }

    /**
     * Archive a record from the WatchDataSource history by placing it on a
     * queue
     * 
     * @param calculable the Calculable record to archive
     */
    public void archive(Calculable calculable) {
        synchronized (archiveQueue) {
            archiveQueue.add(calculable);
            archiveQueue.notifyAll();
        }
    }

    /**
     * The thread that writes to the archive from the archiveQueue
     */
    class ArchiveWriter extends Thread {

        private QueuedArchive queuedArchive;

        public ArchiveWriter(QueuedArchive queuedArchive) {
            this.queuedArchive = queuedArchive;
            setDaemon(true);
            setName(this.getClass().getName() + ":ArchiveWriter");
        }

        public void run() {
            archiveChild = Thread.currentThread();
            while (!archiveChild.isInterrupted()) {
                if (archiveQueue.isEmpty()) {
                    try {
                        synchronized (archiveQueue) {
                            archiveQueue.wait(30000);
                        }
                    } catch (InterruptedException ex) {
                        logger.warning("Archive Thread interrupted");
                    }
                } else {
                    Calculable calc = (Calculable) archiveQueue.remove(0);
                    try {
                        archiveToResource(calc);
                    } catch (IOException ex) {
                        logger.warning("Cannot archive: " + calc + ", " + ex.getMessage());
                    }
                }
            }
            while (!archiveQueue.isEmpty()) {
                Calculable calc = (Calculable) archiveQueue.remove(0);
                try {
                    archiveToResource(calc);
                } catch (IOException ex) {
                    logger.warning("Cannot archive: " + calc + ", " + ex.getMessage());
                }
            }
            closeResource();
            synchronized (queuedArchive) {
                queuedArchive.notifyAll();
            }
        }
    }
}

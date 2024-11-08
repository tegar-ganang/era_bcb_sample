package org.jcyclone.ext.adisk;

import org.jcyclone.core.queue.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * This is an implementation of AFile which uses a pool of threads
 * which perform blocking I/O (through the java.io.RandomAccessFile
 * class) on files. This is a portable implementation but is not
 * intended to be high-performance.
 *
 * @author Matt Welsh
 * @see org.jcyclone.ext.adisk.AFile
 */
class AFileTPImpl extends AFileImpl implements IElement {

    private File f;

    RandomAccessFile raf;

    private AFile afile;

    private AFileTPScheduler tm;

    private ISink compQ;

    private IQueue eventQ;

    private boolean readOnly;

    private boolean closed;

    /**
	 * Create an AFileTPIMpl with the given AFile, filename, completion
	 * queue, create/readOnly flags, and Thread Manager.
	 */
    AFileTPImpl(AFile afile, String fname, ISink compQ, boolean create, boolean readOnly, AFileTPScheduler tm) throws IOException {
        this.afile = afile;
        this.tm = tm;
        this.compQ = compQ;
        this.readOnly = readOnly;
        eventQ = new LinkedBlockingQueue();
        f = new File(fname);
        if (!f.exists() && !create) {
            throw new FileNotFoundException("File not found: " + fname);
        }
        if (f.isDirectory()) {
            throw new FileIsDirectoryException("Is a directory: " + fname);
        }
        if (readOnly) {
            raf = new RandomAccessFile(f, "r");
        } else {
            raf = new RandomAccessFile(f, "rw");
        }
        closed = false;
    }

    /**
	 * Enqueues the given request (which must be an AFileRequest)
	 * to the file.
	 */
    public void enqueue(IElement req) throws SinkException {
        AFileRequest areq = (AFileRequest) req;
        if (closed) {
            throw new SinkClosedException("Sink is closed");
        }
        if (readOnly && (areq instanceof AFileWriteRequest)) {
            throw new BadElementException("Cannot enqueue write request for read-only file", areq);
        }
        areq.afile = afile;
        try {
            eventQ.enqueue(areq);
        } catch (SinkException se) {
            throw new InternalError("AFileTPImpl.enqueue got SinkException - this should not happen, please contact <mdw@cs.berkeley.edu>");
        }
        if (eventQ.size() == 1) {
            tm.fileReady(this);
        }
    }

    /**
	 * Enqueues the given request (which must be an AFileRequest)
	 * to the file.
	 */
    public boolean enqueueLossy(IElement req) {
        AFileRequest areq = (AFileRequest) req;
        if (closed || (readOnly && (areq instanceof AFileWriteRequest))) {
            return false;
        }
        areq.afile = afile;
        try {
            eventQ.enqueue(areq);
        } catch (SinkException se) {
            throw new InternalError("AFileTPImpl.enqueue got SinkException - this should not happen, please contact <mdw@cs.berkeley.edu>");
        }
        if (eventQ.size() == 1) {
            tm.fileReady(this);
        }
        return true;
    }

    /**
	 * Enqueues the given requests (which must be AFileRequests)
	 * to the file.
	 */
    public void enqueue_many(IElement[] elements) throws SinkException {
        if (closed) {
            throw new SinkClosedException("Sink is closed");
        }
        for (int i = 0; i < elements.length; i++) {
            enqueue(elements[i]);
        }
    }

    /**
	 * Enqueues the given requests (which must be AFileRequests)
	 * to the file.
	 */
    public void enqueueMany(List elements) throws SinkException {
        if (closed) {
            throw new SinkClosedException("Sink is closed");
        }
        for (int i = 0; i < elements.size(); i++) {
            enqueue((IElement) elements.get(i));
        }
    }

    /**
	 * Return information on the properties of the file.
	 */
    AFileStat stat() {
        AFileStat s = new AFileStat();
        s.afile = afile;
        s.isDirectory = f.isDirectory();
        s.canRead = f.canRead();
        s.canWrite = f.canWrite();
        s.length = f.length();
        return s;
    }

    /**
	 * Close the file after all enqueued requests have completed.
	 * Disallows any additional requests to be enqueued on this file.
	 * A SinkClosedEvent will be posted on the file's completion queue
	 * when the close is complete.
	 */
    public void close() {
        enqueueLossy(new AFileCloseRequest(afile, compQ));
        closed = true;
    }

    /**
	 * Causes a SinkFlushedEvent to be posted on the file's completion queue
	 * when all pending requests have completed.
	 */
    public void flush() {
        enqueueLossy(new AFileFlushRequest(afile, compQ));
    }

    /**
	 * Return the per-file event queue.
	 */
    IQueue getQueue() {
        return eventQ;
    }
}

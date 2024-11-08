package pyrasun.eio;

import java.nio.channels.*;
import java.net.Socket;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;

/**
 * A ReadWriteEndpoint is an endpoint that represents a socket channel, but has
 * been cleverly named "ReadWriteEndpoint" to befuddle and confuse you into thinking
 * it's alot more impressive than it actually is.<br>
 * <br>
 * In addition to the mechanisms it inherits from the abstract Endpoint class,
 * ReadWriteEndpoint encapsulates the machinery for managing READ, PROCESS, and WRITE events.
 * Specifically, it contains the following:
 * <ul>
 * <li>Objects read in via READ events are automatically placed in a PROCESS queue.</li>
 * <li>PROCESS EIOWorkers are passed in objects on the PROCESS queue in a FIFO manner</li>
 * <li>Writes may be queued or done directly, depending on the defining EIOPoolingStrategy.
 *  If the Endpoint's coordinator has been setup with the DEDICATED_READER strategy, then
 *  writes are done directly to the channle in a blocknig manner.  If any other strategy is
 *  used, then writes are placed on a WRITE queue and handed to workers from the queue in
 *  a FIFO manner.</li>
 * <li>Both READs and WRITEs are throttled if their queues are enabled.  If the maximum
 *  PROCESS queue or write queue limit is reached, enqueues to the offending queue are turned
 * off until enough items are drained from it (either by processing items on the PROCESS queue,
 * or by physically writing objects from the WRITE queue).<br>
 * <br>
 * In the READ case, the exact behavior depends on whether the DEDICATED_READER strategy is
 * used.  If it is, then we physically block the READER thread until the PROCESS queue is
 * sufficiently drained.  If the DEDICATED_READER strategy is not used, then we remove
 * READ interest from the Endpoint until the PROCESS queue is sufficiently drained (and
 * thus the READ worker is not called until the queue is properly drained).<br>
 * <br>
 * In the WRITE case, we physically block 
 * </ul>
 */
public class ReadWriteEndpoint extends Endpoint {

    private SocketChannel nioChannel;

    private List readyForProcessing = new LinkedList();

    private List readyForWriting = new LinkedList();

    private EIOInputBuffer inputBuffer;

    private EIOOutputBuffer outputBuffer;

    private int readLimit = 50;

    private int readRestart = 10;

    private boolean noReadsForYou;

    private Object dedicatedReadLocker = new Object();

    private int writeLimit = 50;

    private int writeRestart = 10;

    private boolean noWritesForYou;

    private Object writeBlocker = new Object();

    private boolean writeInProgress;

    private boolean dedicatedReader = false;

    /**
   * Construct a new ReadWriteEndpoint.  This generally happens when your socket
   * server (er, I mean AcceptingEndpoint) accepts a new connection and needs to
   * add it into EIO, or when a client successfully connects to a server.
   *
   * @param coordinator the EndpointCoordinator that defines that kind of Endpoint this is, and how to handle it.
   * @param nioChannel The SocketChannel.  evil evil evil for a generic beyond-NIO library!
   */
    public ReadWriteEndpoint(final EndpointCoordinator coordinator, final SocketChannel nioChannel) {
        super(coordinator);
        this.nioChannel = nioChannel;
        if (!directWrites) {
            addInterest(EIOEvent.READ);
        }
        nameInit();
        dedicatedReader = coordinator.getEventDescriptor(EIOEvent.READ).useUniqueThread();
        readLimit = coordinator.getEventDescriptor(EIOEvent.READ).getThrottleLimit();
        readRestart = coordinator.getEventDescriptor(EIOEvent.READ).getThrottleRestart();
        writeLimit = coordinator.getEventDescriptor(EIOEvent.WRITE).getThrottleLimit();
        writeRestart = coordinator.getEventDescriptor(EIOEvent.WRITE).getThrottleRestart();
    }

    public SelectableChannel getNIOChannel() {
        return (nioChannel);
    }

    /**
   * Get the input buffer associated with this endpoint.  The underlying buffer
   * is created on-demand - if you want to do your own buffer management, knock yourself
   * out, and you won't pay for having extra buffers hanging around within EmberIO so
   * long as you don't call getInputBuffer().
   *
   * The buffer you get is nice because it's automatically hooked into the whole EIO
   * framework, and alot of nasty nasty business is automatically handled for you.
   */
    public EIOInputBuffer getInputBuffer() {
        if (inputBuffer == null) {
            inputBuffer = new EIOInputBuffer(this);
        }
        return (inputBuffer);
    }

    /**
   * Get the output buffer associated with this endpoint.  This buffer is created
   * on-demand, so if you're a lunatic and want to do your own buffer management
   * go right ahead, and you won't pay the cost of creating one of these.
   *
   * The buffer you get is nice because it's automatically hooked into the whole EIO
   * framework, and alot of nasty nasty business is automatically handled for you.
   */
    public EIOOutputBuffer getOutputBuffer() {
        if (outputBuffer == null) {
            outputBuffer = new EIOOutputBuffer(this);
        }
        return (outputBuffer);
    }

    /**
   * Returns the next Object available as input to a PROCESS event.  End users should
   * only call this if you're seriously subverting the EIO library and are trying to do
   * some tremendous hack without Ember's knowledge or cooperation.
   */
    public Object nextForProcessing() {
        boolean reinject = false;
        Object ret = null;
        synchronized (readyForProcessing) {
            ret = readyForProcessing.size() > 0 ? readyForProcessing.remove(0) : null;
            if (noReadsForYou && readyForProcessing.size() < readRestart) {
                numReadPauses++;
                addInterest(EIOEvent.READ);
                reinject = true;
                noReadsForYou = false;
                if (dedicatedReader) {
                    synchronized (dedicatedReadLocker) {
                        dedicatedReadLocker.notifyAll();
                    }
                }
            }
        }
        if (reinject) {
            coordinator.registerForEvents(this);
        }
        return (ret);
    }

    /**
   * Adds a new Object to be available as input to a PROCESS event.  End users should
   * only call this if you're seriously subverting the EIO library and are trying to do
   * some tremendous hack without Ember's knowledge or cooperation.
   */
    public synchronized void addToProcessing(Object o) {
        synchronized (readyForProcessing) {
            readyForProcessing.add(o);
            if (!dedicatedReader && !noReadsForYou && readyForProcessing.size() > readLimit) {
                removeInterest(EIOEvent.READ);
                noReadsForYou = true;
            }
        }
        processingDispatch(false);
    }

    public boolean needsReadPause() {
        return (!noReadsForYou && readyForProcessing.size() > readLimit);
    }

    public void readPause() {
        synchronized (dedicatedReadLocker) {
            while (noReadsForYou) {
                try {
                    dedicatedReadLocker.wait();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
   * If no one else is handling PROCESS on this right now, dispatch it for handling
   */
    public synchronized void processingDispatch(boolean meProc) {
        boolean doit = false;
        synchronized (readyForProcessing) {
            doit = (readyForProcessing.size() > 0);
        }
        if (doit) {
            setReadiness(EIOEvent.PROCESS.id());
            getCoordinator().handleEvent(this);
        }
    }

    /**
   * Returns the next object available as input to a WRITE event. End users should
   * only call this if you're seriously subverting the EIO library and are trying to do
   * some tremendous hack without Ember's knowledge or cooperation.
   */
    public Object nextForWriting() {
        Object ret = null;
        boolean unpause = false;
        synchronized (readyForWriting) {
            ret = (readyForWriting.size() > 0) ? readyForWriting.remove(0) : null;
            if (noWritesForYou && readyForWriting.size() < writeRestart) {
                unpause = true;
            }
        }
        if (unpause) {
            synchronized (writeBlocker) {
                noWritesForYou = false;
                writeBlocker.notifyAll();
            }
        }
        return (ret);
    }

    public synchronized int getWriteQueueDepth() {
        int num = readyForWriting.size();
        if (isWriteInProgress()) {
            num++;
        }
        return (num);
    }

    public synchronized int getProcessingQueueDepth() {
        int num = readyForProcessing.size();
        return (num);
    }

    /**
   * Returns the true if the writing object queue is empty. End users should
   * only call this if you're seriously subverting the EIO library and are trying to do
   * some tremendous hack without Ember's knowledge or cooperation.
   */
    public boolean isWriteQueueEmpty() {
        synchronized (readyForWriting) {
            return (readyForWriting.size() == 0);
        }
    }

    /**
   * Tell EIO to write an object out on this Endpoint.  NOTE: this write is
   * asynchronous. You may never know if this actually works!
   */
    public void write(Object o) throws IOException {
        if (isAborted(EIOEvent.WRITE)) {
            return;
        }
        if (o == null) {
            return;
        }
        boolean pauseOnWrite = false;
        synchronized (readyForWriting) {
            readyForWriting.add(o);
            if (!noWritesForYou && readyForWriting.size() > writeLimit && !directWrites) {
                numWritePauses++;
                pauseOnWrite = true;
                noWritesForYou = true;
            }
        }
        if (directWrites) {
            writeHandler.dispatch(this);
        } else {
            if (pauseOnWrite && Thread.currentThread().getThreadGroup() != context.getThreadGroup()) {
                synchronized (writeBlocker) {
                    long then = System.currentTimeMillis();
                    while (noWritesForYou) {
                        try {
                            writeBlocker.wait();
                        } catch (Exception e) {
                        }
                    }
                }
            }
            addInterest(EIOEvent.WRITE);
            coordinator.registerForEvents(this);
        }
    }

    /**
   * If 'inProgress' is true, this indicates that a write is in progress right now
   * and hasn't been completed yet.  End users need to use this if they are avoiding
   * EIO's own buffer management and are rolling their own.  In that case,
   * call setIsWriteInProgress (true) if your WRITE EIOWorker couldn't complete a
   * write, and call it with false if you finished a write before returning from
   * the worker.
   */
    public void setIsWriteInProgress(boolean inProgress) {
        this.writeInProgress = inProgress;
    }

    /**
   * Returns true if a write is in progress or a partial write was performed that
   * needs to be taken care of at some later time.
   */
    public boolean isWriteInProgress() {
        return (writeInProgress);
    }

    /**
   * Returns true if there are objects sitting in the write queue.
   */
    public boolean arePendingWrites() {
        return (!isWriteQueueEmpty() || isWriteInProgress());
    }

    /**
   * Evil, evil hack.
   */
    public void abortWrites() {
        synchronized (readyForWriting) {
            readyForWriting.clear();
            setIsWriteInProgress(false);
            eventsAborted |= EIOEvent.WRITE.id();
        }
        unlockProcessing(EIOEvent.WRITE);
    }

    public boolean noWritesForMe() {
        return (noWritesForYou);
    }

    public boolean noReadsForMe() {
        return (noReadsForYou);
    }

    private void nameInit() {
        if (nioChannel != null) {
            Socket socket = nioChannel.socket();
            String localHost = socket.getLocalAddress().getHostName();
            int localPort = socket.getLocalPort();
            String remoteHost = socket.getInetAddress().getHostName();
            int remotePort = socket.getPort();
            String stringID = "RWEndpoint " + localHost + ":" + localPort + ", remote " + remoteHost + ":" + remotePort;
            setInternalName(stringID);
        } else {
            setInternalName("RWEndpoint <no network information available>");
        }
    }
}

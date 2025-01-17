package se.kth.cid.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper to split a single InputStream into several InputStreams and/or
 * OutputStreams. The OutputStreams have to exist already and are given in the
 * constructor, whereas InputStreams are created on demand.
 * 
 * The preferred way of using this class is to pass on OutputStreams in the
 * constructor. If newly created InputStreams (this includes the usage of action
 * objects and the createInputStream()-method) are used, the performance is not
 * optimal. Piped streams are responsible for that, they have an internal and
 * too small buffer of 1024 bytes. They work synchronized, so they also suffer
 * from locking operations. The solution is to use CustomPipedInputStream, which
 * allows us to set adjust the size of the internal circular buffer.
 * 
 * @author Hannes Ebner
 * @version $Id$
 */
public class InputStreamSplitter extends Thread {

    Log log = LogFactory.getLog(InputStreamSplitter.class);

    /**
	 * The InputStream we want to split.
	 */
    private InputStream originalStream;

    /**
	 * Collection of streams to write to.
	 */
    private List<OutputStream> sinks;

    /**
	 * Collection of piped streams for reading.
	 */
    private List<PipedInputStream> sources;

    /**
	 * Collection of to be executed actions.
	 */
    private List<InputStreamConsumer> consumers;

    /**
	 * Collection of threads executing the actions.
	 */
    private List<Thread> threads;

    /**
	 * Locking helper.
	 */
    private Object lockThreading = new Object();

    /**
	 * Amount of read/written bytes.
	 */
    private long byteCount;

    /**
	 * Size of the read buffer.
	 */
    private int bufferSize;

    /**
	 * Amount of created InputStreams.
	 */
    private int runningThreads;

    /**
	 * Tells us whether reading/writing is done or not.
	 */
    private boolean finished;

    /**
	 * True if the original InputStream has been read already.
	 */
    private boolean usable;

    /**
	 * Interface to be implemented by the to be executed action objects.
	 * 
	 * @author Hannes Ebner
	 */
    public interface InputStreamConsumer {

        public void consume(InputStream is);
    }

    public abstract static class RunnableInputStreamConsumer implements InputStreamConsumer, Runnable {

        private final InputStream in;

        public RunnableInputStreamConsumer(InputStream in) {
            this.in = in;
        }

        public final void run() {
            consume(in);
        }
    }

    /**
	 * Initializes the splitter with the source stream.
	 * 
	 * @param in
	 *            Source stream which is supposed to be split.
	 */
    public InputStreamSplitter(InputStream in) {
        this.usable = true;
        this.originalStream = in;
        this.sinks = Collections.synchronizedList(new ArrayList<OutputStream>());
        this.sources = Collections.synchronizedList(new ArrayList<PipedInputStream>());
        this.consumers = Collections.synchronizedList(new ArrayList<InputStreamConsumer>());
        this.threads = Collections.synchronizedList(new ArrayList<Thread>());
        this.bufferSize = 8192;
    }

    /**
	 * Initializes the splitter with the source stream and a collection of
	 * InputStreamConsumer objects.
	 * 
	 * @param in
	 *            Source stream which is supposed to be split.
	 * @param consumers
	 *            Collection of InputStreamConsumer objects.
	 */
    public InputStreamSplitter(InputStream in, Collection<InputStreamConsumer> consumers) {
        this(in);
        this.consumers.addAll(consumers);
    }

    /**
	 * Initializes the splitter with the source stream and some output streams
	 * to write to.
	 * 
	 * @param in
	 *            Source stream which is supposed to be split.
	 * @param out
	 *            Array of output streams.
	 */
    public InputStreamSplitter(InputStream in, OutputStream[] out) {
        this(in);
        this.sinks = Collections.synchronizedList(Arrays.asList(out));
    }

    /**
	 * Adds a single InputStreamConsumer object to the internal collection of
	 * actions.
	 * 
	 * @param consumer
	 */
    public void addConsumer(InputStreamConsumer consumer) {
        synchronized (consumers) {
            this.consumers.add(consumer);
        }
    }

    private void checkUsable() {
        if (!this.usable) {
            throw new IllegalStateException("The source has already been read.");
        }
    }

    /**
	 * Returns an InputStream which will contain the same data as the source
	 * stream. This method creates piped streams internally, which allows an
	 * OutputStream to be converted into an InputStream.
	 * 
	 * @return A newly created InputStream.
	 * @throws IOException
	 */
    public InputStream createInputStream() throws IOException {
        this.checkUsable();
        PipedInputStream pis;
        PipedOutputStream pos;
        pis = new CustomPipedInputStream(this.bufferSize);
        pos = new PipedOutputStream(pis);
        this.sources.add(pis);
        this.sinks.add(pos);
        return pis;
    }

    /**
	 * Creates and starts a thread for each action object. New streams are
	 * created within the threads.
	 * 
	 * @throws IOException
	 */
    private void startConsumerThreads() throws IOException {
        this.runningThreads = 0;
        Iterator consumingObjects = this.consumers.iterator();
        while (consumingObjects.hasNext()) {
            final InputStreamConsumer streamConsumer = (InputStreamConsumer) consumingObjects.next();
            final InputStream is = createInputStream();
            Thread newThread = new Thread(new Runnable() {

                public void run() {
                    synchronized (lockThreading) {
                        runningThreads++;
                        lockThreading.notifyAll();
                    }
                    streamConsumer.consume(is);
                }
            });
            this.threads.add(newThread);
            newThread.start();
        }
        synchronized (lockThreading) {
            boolean interrupted = Thread.interrupted();
            while (consumers.size() != runningThreads) {
                try {
                    lockThreading.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
	 * Reads from an InputStream and writes to all given OutputStreams,
	 * including the ones which are piped to InputStreams again. The thread
	 * reads into a buffer and writes sequencially to OutputStreams. One
	 * possible flaw is the overall writing speed which will not be faster than
	 * the slowest OutputStream respectively reader from a piped InputStream.
	 */
    public void run() {
        this.finished = false;
        byte[] buffer = new byte[this.bufferSize];
        int readCount = 0;
        OutputStream[] os;
        try {
            this.checkUsable();
            this.startConsumerThreads();
            os = (OutputStream[]) this.sinks.toArray(new OutputStream[this.sinks.size()]);
            while (true) {
                if ((readCount = this.originalStream.read(buffer)) != -1) {
                    for (int i = 0; i < os.length; i++) {
                        os[i].write(buffer, 0, readCount);
                    }
                    this.byteCount += readCount;
                } else {
                    for (int i = 0; i < os.length; i++) {
                        os[i].flush();
                        os[i].close();
                    }
                    break;
                }
            }
        } catch (IOException ioe) {
            log.error(ioe);
        } finally {
            try {
                if (this.originalStream != null) {
                    this.originalStream.close();
                }
            } catch (IOException ioe) {
            }
        }
        this.usable = false;
        this.finished = true;
    }

    /**
	 * Waits until all action threads are finished. This does not include
	 * OutputStreams which have been passed on to the constructor.
	 * 
	 * Alternatively a join() can be called to wait for the whole thread. A
	 * normal join() will return quicker, since it just waits for the main
	 * thread and does not take any action threads into consideration.
	 */
    public void joinConsumerThreads() {
        boolean interrupted = Thread.interrupted();
        try {
            this.join();
        } catch (InterruptedException ie) {
        }
        while (!threads.isEmpty()) {
            try {
                ((Thread) threads.get(0)).join();
                threads.remove(0);
            } catch (InterruptedException ie) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
	 * Returns the amount of bytes which have been read from the InputStream.
	 * 
	 * @return Byte count.
	 */
    public long getByteCount() {
        return this.byteCount;
    }

    public boolean isFinished() {
        return this.finished;
    }
}

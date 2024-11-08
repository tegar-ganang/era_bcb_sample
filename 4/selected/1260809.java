package com.dna.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/** 
 * Binder Class enables input stream and output streams to be bound.
 * All datas coming from the input stream will be forwarded to the
 * output streams.
 * 
 * @author Patrick Di Loreto
 * @version 1.0
 *
 */
public class StreamJack extends OutputStream implements Runnable {

    /**
	 * Default Binder Buffer Size.
	 */
    public static final int DEFAULT_BUFFER_SIZE = 8272;

    private static Map<InputStream, List<OutputStream>> tracedInputs = null;

    private static boolean trace = false;

    /**
	 * This method enable the functionality to add the same InputStream in a StreamJack
	 * to multiple outputs. Activate this functionality decress the performances.
	 * @param value true to activate.
	 * @return true if activated.
	 */
    public static synchronized boolean traceInputs(boolean value) {
        if (tracedInputs == null) {
            tracedInputs = new Hashtable<InputStream, List<OutputStream>>();
            trace = value;
        }
        return trace;
    }

    protected InputStream source;

    protected List<OutputStream> destinations = new ArrayList<OutputStream>();

    protected boolean active = false;

    protected boolean suspend = false;

    protected boolean autoFlush = false;

    protected int bufferSize = DEFAULT_BUFFER_SIZE;

    public boolean isActive() {
        return active;
    }

    public boolean isSuspend() {
        return suspend;
    }

    protected StreamJack() {
    }

    /**
	 * Build a standard binder with the specified streams and the default buffer size.
	 * @see Binder.DEFAULT_BUFFER_SIZE.
	 * @param source input stream from which reads incoming datas.
	 * @param destination output stream where to write the datas.
	 */
    public StreamJack(InputStream source, OutputStream destination) {
        this(source, destination, DEFAULT_BUFFER_SIZE);
    }

    /**
	 * Build a binder with a specified buffer size, the input stream and severals output streams.
	 * @param bufferSize
	 * @param source
	 * @param destinations
	 */
    public StreamJack(int bufferSize, InputStream source, OutputStream... destinations) {
        this.source = source;
        for (int i = 0; i < destinations.length; i++) this.destinations.add(destinations[i]);
        this.bufferSize = bufferSize;
        addTracing(this.destinations);
    }

    private void addTracing(Collection<OutputStream> destionations) {
        if (trace) {
            List<OutputStream> bounds = tracedInputs.get(this.source);
            if (bounds != null && destinations != null) {
                for (OutputStream out : destinations) if (!bounds.contains(out)) bounds.add(out);
            } else tracedInputs.put(this.source, new ArrayList<OutputStream>(destinations));
        }
    }

    private void removeTracing(Collection<OutputStream> destinations) {
        if (trace) {
            List<OutputStream> bounds = tracedInputs.get(this.source);
            if (bounds != null && destinations != null) for (OutputStream out : destinations) if (bounds.contains(out)) bounds.remove(out);
        }
    }

    /**
	 * Build a binder with the specified input stream and several output streams using the default
	 * buffer size.
	 * @param source
	 * @param destinations
	 */
    public StreamJack(InputStream source, OutputStream... destinations) {
        this(DEFAULT_BUFFER_SIZE, source, destinations);
    }

    /**
	 * Build a binder with the specified buffer size, input stream and output stream.
	 * @param source
	 * @param destination
	 * @param bufferSize
	 */
    public StreamJack(InputStream source, OutputStream destination, int bufferSize) {
        this(bufferSize, source, new OutputStream[] { destination });
    }

    /**
	 * Start the binding activity in a dedicated thread.
	 * @return the thread associated with the input stream.
	 */
    public Thread start() {
        if (!active) {
            addTracing(this.destinations);
            Thread thread = new Thread(this);
            active = true;
            thread.start();
            return thread;
        } else if (suspend) {
            restart();
            removeTracing(this.destinations);
        }
        return null;
    }

    /**
	 * Stop the binding activity.
	 */
    public void stop() {
        active = false;
        removeTracing(this.destinations);
    }

    /**
	 * Suspend the binding activity.
	 */
    public void suspend() {
        suspend = true;
        removeTracing(this.destinations);
    }

    /**
	 * Resume the binding activity.
	 */
    public void restart() {
        suspend = false;
        addTracing(this.destinations);
        notify();
    }

    /**
	 * Start synchronously the binder with the invoker thread.
	 * @see Binder.start() to start the binding activity in another thread.
	 */
    @Override
    public void run() {
        byte[] buffer = null;
        while (active) {
            if (suspend) try {
                wait();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            buffer = new byte[bufferSize];
            try {
                int read = source.read(buffer);
                if (read < 0) active = false; else {
                    if (read > bufferSize) read = bufferSize;
                    this.write(buffer, 0, read);
                }
            } catch (IOException e) {
                active = false;
                try {
                    this.close();
                } catch (IOException e1) {
                }
                System.err.println(e.getLocalizedMessage());
            }
        }
    }

    /**
	 * Force the flush to the output streams.
	 */
    public void flush() throws IOException {
        for (OutputStream destination : destinations) destination.flush();
    }

    /**
	 * Enable the auto flush on the output streams.
	 * @param auto if true enable the auto flush.
	 */
    public void autoFlush(boolean auto) {
        this.autoFlush = auto;
    }

    /**
	 * Close all streams involved in the binding activity.
	 */
    @Override
    public void close() throws IOException {
        if (source != null) {
            source.close();
        }
        for (OutputStream destination : destinations) try {
            destination.close();
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
            continue;
        }
    }

    /**
	 * Add a new output streams to forward datas coming from the input stream
	 * @param destination output stream
	 * @return true if added, false else.
	 */
    public boolean addOutput(OutputStream destination) {
        boolean response = destinations.add(destination);
        if (response) addTracing(Arrays.asList(destination));
        return response;
    }

    /**
	 * Remove the specified output stream from the binder.
	 * Note: it doesn't close the stream.
	 * @param destination output stream to be removed.
	 * @return true if removed, false else.
	 */
    public boolean removeOutput(OutputStream destination) {
        removeTracing(Arrays.asList(destination));
        return destinations.remove(destination);
    }

    @Override
    public void write(int i) throws IOException {
        int err = 0;
        List<OutputStream> destinations = null;
        if (trace) {
            destinations = tracedInputs.get(this.source);
        }
        if (destinations == null) destinations = this.destinations;
        for (OutputStream destination : destinations) {
            try {
                destination.write(i);
                if (autoFlush) destination.flush();
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
                err++;
                if (err == destinations.size()) throw new IOException("All destinations gave error");
            }
        }
    }
}

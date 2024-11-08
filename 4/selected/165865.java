package gov.nist.applet.phone.media.messaging;

import java.util.Enumeration;
import java.util.Vector;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.media.DataSink;
import javax.media.MediaLocator;
import javax.media.Control;
import javax.media.datasink.DataSinkErrorEvent;
import javax.media.datasink.DataSinkEvent;
import javax.media.datasink.DataSinkListener;
import javax.media.datasink.EndOfStreamEvent;
import javax.media.protocol.SourceTransferHandler;
import javax.media.protocol.PushDataSource;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.PullDataSource;
import javax.media.protocol.DataSource;
import javax.media.protocol.SourceStream;
import javax.media.IncompatibleSourceException;

/**
 * This Data source Handler allow one to write directly from a DataSource 
 * to an array of byte.
 * This Data Source Handler allow one to fill a buffer with MPEG_AUDIO 
 * or GSM audio data.
 * Example : One can record his voice from a microphone to a buffer in either a
 * MP3 or GSM format. To get the buffer with the recorded voice just call the
 * getRecordBuffer method
 * @author Jean Deruelle <jean.deruelle@nist.gov>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class DataSourceHandler implements DataSink, SourceTransferHandler, Runnable {

    protected Vector listeners = new Vector(1);

    private static final boolean DEBUG = false;

    protected static final int NOT_INITIALIZED = 0;

    protected static final int OPENED = 1;

    protected static final int STARTED = 2;

    protected static final int CLOSED = 3;

    protected int state = NOT_INITIALIZED;

    protected DataSource source;

    protected SourceStream[] streams;

    protected SourceStream stream;

    protected boolean push;

    protected Control[] controls;

    protected String contentType = null;

    protected int bytesWritten = 0;

    protected static final int BUFFER_LEN = 128 * 1024;

    protected boolean syncEnabled = false;

    long lastSyncTime = -1;

    ByteArrayOutputStream baout = null;

    protected byte[] buffer1 = new byte[BUFFER_LEN];

    protected byte[] buffer2 = new byte[BUFFER_LEN];

    protected boolean buffer1Pending = false;

    protected long buffer1PendingLocation = -1;

    protected int buffer1Length;

    protected boolean buffer2Pending = false;

    protected long buffer2PendingLocation = -1;

    protected int buffer2Length;

    protected long nextLocation = 0;

    protected Thread writeThread = null;

    private Integer bufferLock = new Integer(0);

    private boolean receivedEOS = false;

    public int WRITE_CHUNK_SIZE = 16384;

    private boolean streamingEnabled = false;

    /**
     * Constructs a new DataSourceHandler
     */
    public DataSourceHandler() {
        baout = new ByteArrayOutputStream();
    }

    /**
     * Set the data source of this data source handler
     * @exception IncompatibleSourceException - if the data source cannot be 
     * handled, this exception is thrown
     */
    public void setSource(DataSource ds) throws IncompatibleSourceException {
        if (!(ds instanceof PushDataSource) && !(ds instanceof PullDataSource)) {
            throw new IncompatibleSourceException("Incompatible datasource");
        }
        source = ds;
        if (source instanceof PushDataSource) {
            push = true;
            try {
                ((PushDataSource) source).connect();
            } catch (IOException ioe) {
            }
            streams = ((PushDataSource) source).getStreams();
        } else {
            push = false;
            try {
                ((PullDataSource) source).connect();
            } catch (IOException ioe) {
            }
            streams = ((PullDataSource) source).getStreams();
        }
        if (streams == null || streams.length != 1) throw new IncompatibleSourceException("DataSource should have 1 stream");
        stream = streams[0];
        contentType = source.getContentType();
        if (push) ((PushSourceStream) stream).setTransferHandler(this);
    }

    /**
     * Set the output <code>MediaLocator</code>.
     * This method should only be called once; an error is thrown if
     * the locator has already been set.
     * @param output <code>MediaLocator</code> that describes where 
     * 		the output goes.
     */
    public void setOutputLocator(MediaLocator output) {
    }

    /**
	 * Our DataSink does not need to be opened.
     */
    public void open() {
    }

    /**
	 * @see javax.media.DataSink#getOutputLocator()
	 */
    public MediaLocator getOutputLocator() {
        return null;
    }

    /**
	 * @see javax.media.DataSink#start()
	 */
    public void start() throws IOException {
        if (source != null) source.start();
        if (writeThread == null) {
            writeThread = new Thread(this);
            writeThread.setName("DataSourceHandler Thread");
            writeThread.start();
        }
        setState(STARTED);
    }

    /**
     * Stop the data-transfer.
     * If the source has not been connected and started,
     * <CODE>stop</CODE> does nothing.
     */
    public void stop() throws IOException {
        if (state == STARTED) {
            if (source != null) source.stop();
            setState(OPENED);
        }
    }

    /**
	 * Set the state of this data source handler
	 */
    protected void setState(int state) {
        synchronized (this) {
            this.state = state;
        }
    }

    /**
	 * @see javax.media.DataSink#close()
	 */
    public final void close() {
        synchronized (this) {
            if (state == CLOSED) return;
            setState(CLOSED);
        }
        if (push) {
            for (int i = 0; i < streams.length; i++) {
                ((PushSourceStream) streams[i]).setTransferHandler(null);
            }
        }
        try {
            source.stop();
        } catch (IOException e) {
            System.err.println("IOException when stopping source " + e);
        }
        if (source != null) source.disconnect();
        removeAllListeners();
    }

    /**
	 * Get the current content type for this data source handler
	 *
	 * @return The current <CODE>ContentDescriptor</CODE> for this data source handler.
	 */
    public String getContentType() {
        return contentType;
    }

    /**
	 * Obtain the collection of objects that
	 * control the object that implements this interface.
	 * <p>
	 *
	 * No controls are supported.
	 * A zero length array is returned.
	 *
	 * @return A zero length array
	 */
    public Object[] getControls() {
        if (controls == null) {
            controls = new Control[0];
        }
        return controls;
    }

    /**
	 * Obtain the object that implements the specified
	 * <code>Class</code> or <code>Interface</code>
	 * The full class or interface name must be used.
	 * <p>
	 *
	 * The control is not supported.
	 * <code>null</code> is returned.
	 *
	 * @return <code>null</code>.
	 */
    public Object getControl(String controlName) {
        return null;
    }

    public synchronized void transferData(PushSourceStream pss) {
        if (state == STARTED) {
            int totalRead = 0;
            int spaceAvailable = BUFFER_LEN;
            int bytesRead = 0;
            if (buffer1Pending) {
                synchronized (bufferLock) {
                    while (buffer1Pending) {
                        if (DEBUG) System.err.println("Waiting for free buffer");
                        try {
                            bufferLock.wait();
                        } catch (InterruptedException ie) {
                        }
                    }
                }
                if (DEBUG) System.err.println("Got free buffer");
            }
            while (spaceAvailable > 0) {
                try {
                    bytesRead = pss.read(buffer1, totalRead, spaceAvailable);
                    if (bytesRead > 16 * 1024 && WRITE_CHUNK_SIZE < 32 * 1024) {
                        if (bytesRead > 64 * 1024 && WRITE_CHUNK_SIZE < 128 * 1024) WRITE_CHUNK_SIZE = 128 * 1024; else if (bytesRead > 32 * 1024 && WRITE_CHUNK_SIZE < 64 * 1024) WRITE_CHUNK_SIZE = 64 * 1024; else if (WRITE_CHUNK_SIZE < 32 * 1024) WRITE_CHUNK_SIZE = 32 * 1024;
                    }
                } catch (IOException ioe) {
                }
                if (bytesRead <= 0) {
                    break;
                }
                totalRead += bytesRead;
                spaceAvailable -= bytesRead;
            }
            if (totalRead > 0) {
                synchronized (bufferLock) {
                    buffer1Pending = true;
                    buffer1PendingLocation = nextLocation;
                    buffer1Length = totalRead;
                    nextLocation = -1;
                    if (DEBUG) System.err.println("Notifying consumer");
                    bufferLock.notifyAll();
                }
            }
            if (bytesRead == -1) {
                if (DEBUG) System.err.println("Got EOS");
                receivedEOS = true;
            }
        }
    }

    /**
     *  Asynchronous write thread
     */
    public void run() {
        while (!(state == CLOSED)) {
            if (state == STARTED) {
                synchronized (bufferLock) {
                    while (!buffer1Pending && !buffer2Pending && state != CLOSED && !receivedEOS) {
                        if (DEBUG) System.err.println("Waiting for filled buffer");
                        try {
                            bufferLock.wait(500);
                        } catch (InterruptedException ie) {
                        }
                        if (DEBUG) System.err.println("Consumer notified");
                    }
                }
                if (buffer2Pending) {
                    if (DEBUG) System.err.println("Writing Buffer2");
                    write(buffer2, buffer2PendingLocation, buffer2Length);
                    if (DEBUG) System.err.println("Done writing Buffer2");
                    buffer2Pending = false;
                }
                synchronized (bufferLock) {
                    if (buffer1Pending) {
                        byte[] tempBuffer = buffer2;
                        buffer2 = buffer1;
                        buffer2Pending = true;
                        buffer2PendingLocation = buffer1PendingLocation;
                        buffer2Length = buffer1Length;
                        buffer1Pending = false;
                        buffer1 = tempBuffer;
                        if (DEBUG) System.err.println("Notifying producer");
                        bufferLock.notifyAll();
                    } else {
                        if (receivedEOS) break;
                    }
                }
            }
            if (receivedEOS) {
                if (DEBUG) System.err.println("Sending EOS: streamingEnabled is " + streamingEnabled);
                if (!streamingEnabled) {
                    sendEndofStreamEvent();
                }
            }
        }
    }

    /**
	 * seek a specific location in the dataSource
	 * @return the location where the data source is now placed
	 */
    public synchronized long seek(long where) {
        nextLocation = where;
        return where;
    }

    /**
	 * Write the data in parameter in the data source handler buffer
	 * @param buffer - data to write into the data source handler buffer
	 * @param location - location to start 
	 * @param length - length to write
	 */
    private void write(byte[] buffer, long location, int length) {
        int offset, toWrite;
        offset = 0;
        while (length > 0) {
            toWrite = WRITE_CHUNK_SIZE;
            if (length < toWrite) toWrite = length;
            baout.write(buffer, offset, toWrite);
            bytesWritten += toWrite;
            length -= toWrite;
            offset += toWrite;
            Thread.yield();
        }
    }

    /**
     * @see javax.media.DataSink#addDataSinkListener(javax.media.datasink.DataSinkListener)
     */
    public void addDataSinkListener(DataSinkListener dsl) {
        if (dsl != null) if (!listeners.contains(dsl)) listeners.addElement(dsl);
    }

    /**
	 * @see javax.media.DataSink#removeDataSinkListener(javax.media.datasink.DataSinkListener)
	 */
    public void removeDataSinkListener(DataSinkListener dsl) {
        if (dsl != null) listeners.removeElement(dsl);
    }

    /**
	 * Send event to the listeners
	 * @param event - the vent to send
	 */
    protected void sendEvent(DataSinkEvent event) {
        if (!listeners.isEmpty()) {
            synchronized (listeners) {
                Enumeration list = listeners.elements();
                while (list.hasMoreElements()) {
                    DataSinkListener listener = (DataSinkListener) list.nextElement();
                    listener.dataSinkUpdate(event);
                }
            }
        }
    }

    /**
	 * remove All the listeners from this data source handler
	 */
    protected void removeAllListeners() {
        listeners.removeAllElements();
    }

    /**
	 * Send an end of stream event to the listeners
	 */
    protected final void sendEndofStreamEvent() {
        sendEvent(new EndOfStreamEvent(this));
    }

    /**
	 * Send a data sink error event to the listeners
	 * @param reason - the reason of the error
	 */
    protected final void sendDataSinkErrorEvent(String reason) {
        sendEvent(new DataSinkErrorEvent(this, reason));
    }

    /**
	 * Get the recorded buffer of this data source handler
	 * @return byte array containing the data recorded
	 */
    public byte[] getRecordBuffer() {
        byte[] recordedBuffer = baout.toByteArray();
        baout.reset();
        return recordedBuffer;
    }
}

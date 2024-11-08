package com.jme3.network.base;

import com.jme3.network.ErrorListener;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.kernel.Connector;
import com.jme3.network.kernel.ConnectorException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  Wraps a single Connector and forwards new messages
 *  to the supplied message dispatcher.  This is used
 *  by DefaultClient to manage its connector objects.
 *  This is only responsible for message reading and provides
 *  no support for buffering writes.
 *
 *  <p>This adapter assumes a simple protocol where two
 *  bytes define a (short) object size with the object data
 *  to follow.  Note: this limits the size of serialized
 *  objects to 32676 bytes... even though, for example,
 *  datagram packets can hold twice that. :P</p>  
 *
 *  @version   $Revision: 8944 $
 *  @author    Paul Speed
 */
public class ConnectorAdapter extends Thread {

    private static final int OUTBOUND_BACKLOG = 16000;

    private Connector connector;

    private MessageListener<Object> dispatcher;

    private ErrorListener<Object> errorHandler;

    private AtomicBoolean go = new AtomicBoolean(true);

    private BlockingQueue<ByteBuffer> outbound;

    private WriterThread writer;

    private boolean reliable;

    public ConnectorAdapter(Connector connector, MessageListener<Object> dispatcher, ErrorListener<Object> errorHandler, boolean reliable) {
        super(String.valueOf(connector));
        this.connector = connector;
        this.dispatcher = dispatcher;
        this.errorHandler = errorHandler;
        this.reliable = reliable;
        setDaemon(true);
        outbound = new ArrayBlockingQueue<ByteBuffer>(OUTBOUND_BACKLOG);
        writer = new WriterThread();
        writer.start();
    }

    public void close() {
        go.set(false);
        writer.shutdown();
        if (connector.isConnected()) {
            connector.close();
        }
    }

    protected void dispatch(Message m) {
        dispatcher.messageReceived(null, m);
    }

    public void write(ByteBuffer data) {
        try {
            outbound.put(data);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for queue to drain", e);
        }
    }

    protected void handleError(Exception e) {
        if (!go.get()) return;
        errorHandler.handleError(this, e);
    }

    public void run() {
        MessageProtocol protocol = new MessageProtocol();
        try {
            while (go.get()) {
                ByteBuffer buffer = connector.read();
                if (buffer == null) {
                    if (go.get()) {
                        throw new ConnectorException("Connector closed.");
                    } else {
                        break;
                    }
                }
                protocol.addBuffer(buffer);
                Message m = null;
                while ((m = protocol.getMessage()) != null) {
                    m.setReliable(reliable);
                    dispatch(m);
                }
            }
        } catch (Exception e) {
            handleError(e);
        }
    }

    protected class WriterThread extends Thread {

        public WriterThread() {
            super(String.valueOf(connector) + "-writer");
        }

        public void shutdown() {
            interrupt();
        }

        private void write(ByteBuffer data) {
            try {
                connector.write(data);
            } catch (Exception e) {
                handleError(e);
            }
        }

        public void run() {
            while (go.get()) {
                try {
                    ByteBuffer data = outbound.take();
                    write(data);
                } catch (InterruptedException e) {
                    if (!go.get()) return;
                    throw new RuntimeException("Interrupted waiting for data", e);
                }
            }
        }
    }
}

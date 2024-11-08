package com.simconomy.xmpp;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import com.simconomy.xmpp.events.tls.FeaturesStanzaEvent;
import com.simconomy.xmpp.model.StanzaType;

/**
 * Writes packets to a XMPP server. Packets are sent using a dedicated thread. Packet
 * interceptors can be registered to dynamically modify packets before they're actually
 * sent. Packet listeners can be registered to listen for all outgoing packets.
 *
 * @author Matt Tucker
 */
public class PacketWriter {

    private Thread writerThread;

    private Thread keepAliveThread;

    private Writer writer;

    private XMPPConnection connection;

    private final BlockingQueue<String> queue;

    private boolean done;

    /**
     * Timestamp when the last stanza was sent to the server. This information is used
     * by the keep alive process to only send heartbeats when the connection has been idle.
     */
    private long lastActive = System.currentTimeMillis();

    /**
     * Creates a new packet writer with the specified connection.
     *
     * @param connection the connection.
     */
    protected PacketWriter(XMPPConnection connection) {
        this.queue = new ArrayBlockingQueue<String>(500, true);
        this.connection = connection;
        init();
    }

    /**
    * Initializes the writer in order to be used. It is called at the first connection and also
    * is invoked if the connection is disconnected by an error.
    */
    protected void init() {
        this.writer = connection.writer;
        done = false;
        writerThread = new Thread() {

            public void run() {
                writePackets(this);
            }
        };
        writerThread.setName("Smack Packet Writer (" + connection.connectionCounterValue + ")");
        writerThread.setDaemon(true);
    }

    /**
     * Sends the specified packet to the server.
     *
     * @param packet the packet to send.
     */
    public void sendPacket(String packet) {
        if (!done) {
            try {
                queue.put(packet);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                return;
            }
            synchronized (queue) {
                queue.notifyAll();
            }
        }
    }

    /**
     * Starts the packet writer thread and opens a connection to the server. The
     * packet writer will continue writing packets until {@link #shutdown} or an
     * error occurs.
     */
    public void startup() {
        writerThread.start();
    }

    /**
     * Starts the keep alive process. A white space (aka heartbeat) is going to be
     * sent to the server every 30 seconds (by default) since the last stanza was sent
     * to the server.
     */
    void startKeepAliveProcess() {
        int keepAliveInterval = 30000;
        if (keepAliveInterval > 0) {
            KeepAliveTask task = new KeepAliveTask(keepAliveInterval);
            keepAliveThread = new Thread(task);
            task.setThread(keepAliveThread);
            keepAliveThread.setDaemon(true);
            keepAliveThread.setName("Smack Keep Alive (" + connection.connectionCounterValue + ")");
            keepAliveThread.start();
        }
    }

    void setWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Shuts down the packet writer. Once this method has been called, no further
     * packets will be written to the server.
     */
    public void shutdown() {
        done = true;
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    /**
     * Cleans up all resources used by the packet writer.
     */
    void cleanup() {
    }

    /**
     * Returns the next available packet from the queue for writing.
     *
     * @return the next packet for writing.
     */
    private String nextPacket() {
        String packet = null;
        while (!done && (packet = queue.poll()) == null) {
            try {
                synchronized (queue) {
                    queue.wait();
                }
            } catch (InterruptedException ie) {
            }
        }
        return packet;
    }

    private void writePackets(Thread thisThread) {
        try {
            openStream();
            while (!done && (writerThread == thisThread)) {
                String packet = nextPacket();
                if (packet != null) {
                    synchronized (writer) {
                        writer.write(packet);
                        writer.flush();
                        lastActive = System.currentTimeMillis();
                    }
                }
            }
            try {
                synchronized (writer) {
                    while (!queue.isEmpty()) {
                        String packet = queue.remove();
                        writer.write(packet);
                    }
                    writer.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            queue.clear();
            try {
                writer.write("</stream:stream>");
                writer.flush();
            } catch (Exception e) {
            } finally {
                try {
                    writer.close();
                } catch (Exception e) {
                }
            }
        } catch (IOException ioe) {
            if (!done) {
                done = true;
                connection.packetReader.notifyConnectionError(ioe);
            }
        }
    }

    /**
     * Sends to the server a new stream element. This operation may be requested several times
     * so we need to encapsulate the logic in one place. This message will be sent while doing
     * TLS, SASL and resource binding.
     *
     * @throws IOException If an error occurs while sending the stanza to the server.
     */
    public void openStream() throws IOException {
        connection.packetReader.addPacketListener(StanzaType.features, new FeaturesStanzaEvent());
        StringBuilder stream = new StringBuilder();
        stream.append("<stream:stream");
        stream.append(" to=\"").append(connection.serviceName).append("\"");
        stream.append(" xmlns=\"jabber:client\"");
        stream.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
        stream.append(" version=\"1.0\">");
        writer.write(stream.toString());
        writer.flush();
    }

    /**
     * A TimerTask that keeps connections to the server alive by sending a space
     * character on an interval.
     */
    private class KeepAliveTask implements Runnable {

        private int delay;

        private Thread thread;

        public KeepAliveTask(int delay) {
            this.delay = delay;
        }

        protected void setThread(Thread thread) {
            this.thread = thread;
        }

        public void run() {
            try {
                Thread.sleep(15000);
            } catch (InterruptedException ie) {
            }
            while (!done && keepAliveThread == thread) {
                synchronized (writer) {
                    if (System.currentTimeMillis() - lastActive >= delay) {
                        try {
                            writer.write(" ");
                            writer.flush();
                        } catch (Exception e) {
                        }
                    }
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}

package games.strategy.net;

import games.strategy.engine.message.HubInvocationResults;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.SpokeInvocationResults;
import games.strategy.engine.message.SpokeInvoke;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.util.concurrent.LinkedBlockingQueue;

class Connection {

    private static Logger s_logger = Logger.getLogger(Connection.class.getName());

    private Socket m_socket;

    private OutputStream m_socketOut;

    private InputStream m_socketIn;

    private ObjectOutputStream m_out;

    private ObjectInputStream m_in;

    private volatile boolean m_shutdown = false;

    private IConnectionListener m_listener;

    private INode m_localNode;

    private INode m_remoteNode;

    private Thread m_reader;

    private Thread m_writer;

    private final LinkedBlockingQueue<MessageHeader> m_waitingToBeSent = new LinkedBlockingQueue<MessageHeader>();

    private IObjectStreamFactory m_objectStreamFactory;

    private long m_totalRead = 0;

    private long m_totatlWriten = 0;

    private final boolean m_remoteIsServer;

    public Connection(Socket s, INode ident, IConnectionListener listener, IObjectStreamFactory fact, boolean remoteIsServer, SocketStreams streams) throws IOException {
        m_remoteIsServer = remoteIsServer;
        m_objectStreamFactory = fact;
        init(s, ident, listener, streams);
    }

    public void log(MessageHeader header, boolean read) {
        if (!s_logger.isLoggable(Level.FINEST)) return;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = m_objectStreamFactory.create(sink);
            writeHeader(header, out);
            out.flush();
            out.close();
            sink.close();
            int size = sink.toByteArray().length;
            if (read) m_totalRead += size; else m_totatlWriten += size;
            String message = (read ? "READ:" : "WRITE:") + header.getMessage() + " size:" + size + " total:" + (read ? m_totalRead : m_totatlWriten);
            s_logger.log(Level.FINEST, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init(Socket s, INode ident, IConnectionListener listener, SocketStreams streams) throws IOException {
        m_socket = s;
        m_localNode = ident;
        m_listener = listener;
        m_socketOut = streams.getSocketOut();
        m_out = m_objectStreamFactory.create(streams.getBufferedOut());
        m_out.writeObject(m_localNode);
        m_out.flush();
        m_socketIn = streams.getSocketIn();
        BufferedInputStream bufferedIn = new BufferedInputStream(streams.getBufferedIn());
        m_in = m_objectStreamFactory.create(bufferedIn);
        try {
            m_remoteNode = (INode) m_in.readObject();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            throw new IllegalStateException("INode class not found");
        }
        m_reader = new Thread(new Reader(), "ConnectionReader for " + m_localNode.getName() + ":" + m_localNode.getAddress());
        m_reader.start();
        m_writer = new Thread(new Writer(), "ConnectionWriter for " + m_localNode.getName() + ":" + m_localNode.getAddress());
        m_writer.start();
    }

    /**
     * Blocks until no more data remains to be written or the socket is
     * shutdown.
     */
    public void flush() {
        if (m_shutdown) return;
        Object lock = new Object();
        synchronized (lock) {
            while (!m_shutdown && !m_waitingToBeSent.isEmpty()) {
                try {
                    lock.wait(50);
                } catch (InterruptedException ie) {
                }
            }
        }
    }

    public INode getLocalNode() {
        return m_localNode;
    }

    public INode getRemoteNode() {
        return m_remoteNode;
    }

    /**
     * Write the MessageHeader over the network. Returns immediately.
     * 
     * @param msg
     */
    public void send(MessageHeader msg) {
        m_waitingToBeSent.offer(msg);
    }

    public boolean shutDown() {
        if (!m_shutdown) {
            m_shutdown = true;
            try {
                try {
                    m_socket.shutdownInput();
                    m_socket.shutdownOutput();
                    m_socketIn.close();
                    m_socketOut.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                m_socket.close();
                m_writer.interrupt();
                if (!m_socket.isClosed()) throw new IllegalStateException("Not closed");
                if (!m_socket.isOutputShutdown()) throw new IllegalStateException("Output not closed");
                if (!m_socket.isInputShutdown()) throw new IllegalStateException("Input not closed");
                return true;
            } catch (Exception e) {
                System.err.println("Exception shutting down");
                e.printStackTrace();
                s_logger.info("Socket closed:" + m_socket.getRemoteSocketAddress());
            }
        }
        return false;
    }

    public boolean isConnected() {
        return !m_shutdown;
    }

    private void messageReceived(MessageHeader header) {
        if (!m_remoteIsServer && !header.getFrom().equals(m_remoteNode)) {
            throw new IllegalStateException("Non server node trying to spoof a message from a different node");
        }
        if (header != null) m_listener.messageReceived(header, this);
    }

    /**
     * Most messages we pass will be one of the types below
     * since each of these is externalizable, we can 
     * reduce network traffic considerably by skipping the 
     * writing of the full identifiers, and simply write a single
     * byte to show the type. 
     */
    static byte getType(Object msg) {
        if (msg instanceof HubInvoke) return 1; else if (msg instanceof SpokeInvoke) return 2; else if (msg instanceof HubInvocationResults) return 3; else if (msg instanceof SpokeInvocationResults) return 4;
        return Byte.MAX_VALUE;
    }

    private Externalizable getTemplate(byte type) {
        switch(type) {
            case 1:
                return new HubInvoke();
            case 2:
                return new SpokeInvoke();
            case 3:
                return new HubInvocationResults();
            case 4:
                return new SpokeInvocationResults();
            default:
                throw new IllegalStateException("not recognized, " + type);
        }
    }

    private void writeHeader(MessageHeader next, ObjectOutputStream out) throws IOException {
        if (next.getFor() == null) {
            out.write(1);
        } else {
            out.write(0);
            if (next.getFor().equals(m_remoteNode)) {
                out.write(1);
            } else {
                out.write(0);
                next.getFor().writeExternal(out);
            }
        }
        if (next.getFrom().equals(m_localNode)) {
            out.write(1);
        } else {
            out.write(0);
            next.getFrom().writeExternal(out);
        }
        byte type = getType(next.getMessage());
        out.write(type);
        if (type != Byte.MAX_VALUE) {
            ((Externalizable) next.getMessage()).writeExternal(out);
        } else {
            out.writeObject(next.getMessage());
        }
        out.reset();
    }

    class Writer implements Runnable {

        public void run() {
            while (!m_shutdown) {
                MessageHeader next;
                try {
                    next = (MessageHeader) m_waitingToBeSent.take();
                    if (next == null) continue;
                } catch (InterruptedException e) {
                    continue;
                }
                write(next);
                log(next, false);
            }
        }

        private void write(MessageHeader next) {
            if (!m_shutdown) {
                try {
                    writeHeader(next, m_out);
                    if (m_waitingToBeSent.peek() == null) m_out.flush();
                } catch (IOException ioe) {
                    if (ioe instanceof ObjectStreamException) System.err.println("Error writing:" + next);
                    if (!m_shutdown) {
                        ioe.printStackTrace();
                        if (shutDown()) {
                            List<MessageHeader> unsent = new ArrayList<MessageHeader>(m_waitingToBeSent);
                            unsent.add(next);
                            m_listener.fatalError(ioe, Connection.this, unsent);
                        }
                    }
                }
            }
        }
    }

    class Reader implements Runnable {

        private MessageHeader readMessageHeader() throws IOException, ClassNotFoundException {
            INode to;
            if (m_in.read() == 1) {
                to = null;
            } else {
                if (m_in.read() == 1) {
                    to = m_localNode;
                } else {
                    to = new Node();
                    to.readExternal(m_in);
                }
            }
            INode from;
            if (m_in.read() == 1) {
                from = m_remoteNode;
            } else {
                from = new Node();
                from.readExternal(m_in);
            }
            Serializable message;
            byte type = (byte) m_in.read();
            if (type != Byte.MAX_VALUE) {
                Externalizable template = getTemplate(type);
                template.readExternal(m_in);
                message = template;
            } else {
                message = (Serializable) m_in.readObject();
            }
            return new MessageHeader(to, from, message);
        }

        @SuppressWarnings("unchecked")
        public void run() {
            while (!m_shutdown) {
                try {
                    MessageHeader msg = readMessageHeader();
                    log(msg, true);
                    messageReceived(msg);
                } catch (ClassNotFoundException cnfe) {
                    s_logger.log(Level.SEVERE, "class not found? remote connection" + m_socket.getRemoteSocketAddress(), cnfe);
                } catch (IOException ioe) {
                    if (!m_shutdown) {
                        if (ioe instanceof EOFException) {
                        } else if (ioe instanceof SocketException) {
                            if (ioe.getMessage().equals("Connection reset") || ioe.getMessage().equals("Socket closed")) {
                            } else {
                                ioe.printStackTrace();
                            }
                        } else {
                            ioe.printStackTrace();
                        }
                        if (shutDown()) {
                            List<MessageHeader> unsent = new ArrayList(Arrays.asList(m_waitingToBeSent.toArray()));
                            m_listener.fatalError(ioe, Connection.this, unsent);
                        }
                    }
                }
            }
        }
    }
}

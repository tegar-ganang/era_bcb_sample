package games.strategy.net.nio;

import games.strategy.engine.message.HubInvocationResults;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.SpokeInvocationResults;
import games.strategy.engine.message.SpokeInvoke;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.INode;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;
import games.strategy.net.nio.QuarantineConversation.ACTION;
import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * A thread to Decode messages from a reader.
 * 
 * @author sgb
 */
public class Decoder {

    private static final Logger s_logger = Logger.getLogger(Decoder.class.getName());

    private final NIOReader m_reader;

    private volatile boolean m_running = true;

    private final IErrorReporter m_errorReporter;

    private final IObjectStreamFactory m_objectStreamFactory;

    private final NIOSocket m_nioSocket;

    /**
	 * These sockets are quarantined. They have not logged in, and messages
	 * read from them are not passed outside of the quarantine conversation.
	 */
    private final ConcurrentHashMap<SocketChannel, QuarantineConversation> m_quarantine = new ConcurrentHashMap<SocketChannel, QuarantineConversation>();

    private final Thread m_thread;

    public Decoder(final NIOSocket nioSocket, final NIOReader reader, final IErrorReporter reporter, final IObjectStreamFactory objectStreamFactory, final String threadSuffix) {
        m_reader = reader;
        m_errorReporter = reporter;
        m_objectStreamFactory = objectStreamFactory;
        m_nioSocket = nioSocket;
        m_thread = new Thread(new Runnable() {

            public void run() {
                loop();
            }
        }, "Decoder -" + threadSuffix);
        m_thread.start();
    }

    public void shutDown() {
        m_running = false;
        m_thread.interrupt();
    }

    private void loop() {
        while (m_running) {
            try {
                SocketReadData data;
                try {
                    data = m_reader.take();
                } catch (final InterruptedException e) {
                    continue;
                }
                if (data == null || !m_running) continue;
                if (s_logger.isLoggable(Level.FINEST)) {
                    s_logger.finest("Decoding packet:" + data);
                }
                final ByteArrayInputStream stream = new ByteArrayInputStream(data.getData());
                try {
                    final MessageHeader header = readMessageHeader(data.getChannel(), m_objectStreamFactory.create(stream));
                    if (s_logger.isLoggable(Level.FINEST)) {
                        s_logger.log(Level.FINEST, "header decoded:" + header);
                    }
                    final Socket s = data.getChannel().socket();
                    if (!m_running || s == null || s.isInputShutdown()) continue;
                    final QuarantineConversation converstation = m_quarantine.get(data.getChannel());
                    if (converstation != null) {
                        sendQuarantine(data.getChannel(), converstation, header);
                    } else {
                        if (m_nioSocket.getLocalNode() == null) throw new IllegalStateException("we are writing messages, but no local node");
                        if (header.getFrom() == null) throw new IllegalArgumentException("Null from:" + header);
                        if (s_logger.isLoggable(Level.FINER)) {
                            s_logger.log(Level.FINER, "decoded  msg:" + header.getMessage() + " size:" + data.size());
                        }
                        m_nioSocket.messageReceived(header, data.getChannel());
                    }
                } catch (final Exception ioe) {
                    s_logger.log(Level.SEVERE, "error reading object", ioe);
                    m_errorReporter.error(data.getChannel(), ioe);
                }
            } catch (final Exception e) {
                s_logger.log(Level.WARNING, "error in decoder", e);
            }
        }
    }

    private void sendQuarantine(final SocketChannel channel, final QuarantineConversation conversation, final MessageHeader header) {
        final ACTION a = conversation.message(header.getMessage());
        if (a == ACTION.TERMINATE) {
            if (s_logger.isLoggable(Level.FINER)) {
                s_logger.log(Level.FINER, "Terminating quarantined connection to:" + channel.socket().getRemoteSocketAddress());
            }
            conversation.close();
            m_errorReporter.error(channel, new CouldNotLogInException());
        } else if (a == ACTION.UNQUARANTINE) {
            if (s_logger.isLoggable(Level.FINER)) {
                s_logger.log(Level.FINER, "Accepting quarantined connection to:" + channel.socket().getRemoteSocketAddress());
            }
            m_nioSocket.unquarantine(channel, conversation);
            m_quarantine.remove(channel);
        }
    }

    private MessageHeader readMessageHeader(final SocketChannel channel, final ObjectInputStream objectInput) throws IOException, ClassNotFoundException {
        INode to;
        if (objectInput.read() == 1) {
            to = null;
        } else {
            if (objectInput.read() == 1) {
                to = m_nioSocket.getLocalNode();
            } else {
                to = new Node();
                ((Node) to).readExternal(objectInput);
            }
        }
        INode from;
        final int readMark = objectInput.read();
        if (readMark == 1) {
            from = m_nioSocket.getRemoteNode(channel);
        } else if (readMark == 2) {
            from = null;
        } else {
            from = new Node();
            ((Node) from).readExternal(objectInput);
        }
        Serializable message;
        final byte type = (byte) objectInput.read();
        if (type != Byte.MAX_VALUE) {
            final Externalizable template = getTemplate(type);
            template.readExternal(objectInput);
            message = template;
        } else {
            message = (Serializable) objectInput.readObject();
        }
        return new MessageHeader(to, from, message);
    }

    public static Externalizable getTemplate(final byte type) {
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

    /**
	 * Most messages we pass will be one of the types below
	 * since each of these is externalizable, we can
	 * reduce network traffic considerably by skipping the
	 * writing of the full identifiers, and simply write a single
	 * byte to show the type.
	 */
    public static byte getType(final Object msg) {
        if (msg instanceof HubInvoke) return 1; else if (msg instanceof SpokeInvoke) return 2; else if (msg instanceof HubInvocationResults) return 3; else if (msg instanceof SpokeInvocationResults) return 4;
        return Byte.MAX_VALUE;
    }

    public void add(final SocketChannel channel, final QuarantineConversation conversation) {
        m_quarantine.put(channel, conversation);
    }

    public void closed(final SocketChannel channel) {
        final QuarantineConversation conversation = m_quarantine.remove(channel);
        if (conversation != null) conversation.close();
    }
}

package games.strategy.net.nio;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * A thread that reads socket data using NIO from a collection of sockets.<br>
 * 
 * Data is read in packets, and placed in the output queye.<br>
 * 
 * Packets are placed in the output queue in order they are read from the socket.
 * 
 * @author sgb
 */
public class NIOReader {

    private static final Logger s_logger = Logger.getLogger(NIOReader.class.getName());

    private final LinkedBlockingQueue<SocketReadData> m_outputQueue = new LinkedBlockingQueue<SocketReadData>();

    private volatile boolean m_running = true;

    private final Map<SocketChannel, SocketReadData> m_reading = new ConcurrentHashMap<SocketChannel, SocketReadData>();

    private final IErrorReporter m_errorReporter;

    private final Selector m_selector;

    private final Object m_socketsToAddMutex = new Object();

    private final List<SocketChannel> m_socketsToAdd = new ArrayList<SocketChannel>();

    private long m_totalBytes;

    public NIOReader(final IErrorReporter reporter, final String threadSuffix) {
        m_errorReporter = reporter;
        try {
            m_selector = Selector.open();
        } catch (final IOException e) {
            s_logger.log(Level.SEVERE, "Could not create Selector", e);
            throw new IllegalStateException(e);
        }
        final Thread t = new Thread(new Runnable() {

            public void run() {
                loop();
            }
        }, "NIO Reader - " + threadSuffix);
        t.start();
    }

    public void shutDown() {
        m_running = false;
        try {
            m_selector.close();
        } catch (final Exception e) {
            s_logger.log(Level.WARNING, "error closing selector", e);
        }
    }

    public void add(final SocketChannel channel) {
        synchronized (m_socketsToAddMutex) {
            m_socketsToAdd.add(channel);
            m_selector.wakeup();
        }
    }

    private void selectNewChannels() {
        List<SocketChannel> toAdd = null;
        synchronized (m_socketsToAddMutex) {
            if (m_socketsToAdd.isEmpty()) return;
            toAdd = new ArrayList<SocketChannel>(m_socketsToAdd);
            m_socketsToAdd.clear();
        }
        for (final SocketChannel channel : toAdd) {
            try {
                channel.register(m_selector, SelectionKey.OP_READ);
            } catch (final ClosedChannelException e) {
                return;
            }
        }
    }

    private void loop() {
        while (m_running) {
            try {
                if (s_logger.isLoggable(Level.FINEST)) {
                    s_logger.finest("selecting...");
                }
                try {
                    m_selector.select();
                } catch (final Exception e) {
                    s_logger.log(Level.INFO, "error reading selection", e);
                }
                if (!m_running) continue;
                selectNewChannels();
                final Set<SelectionKey> selected = m_selector.selectedKeys();
                if (s_logger.isLoggable(Level.FINEST)) {
                    s_logger.finest("selected:" + selected.size());
                }
                final Iterator<SelectionKey> iter = selected.iterator();
                while (iter.hasNext()) {
                    final SelectionKey key = iter.next();
                    iter.remove();
                    if (key.isValid() && key.isReadable()) {
                        final SocketChannel channel = (SocketChannel) key.channel();
                        final SocketReadData packet = getReadData(channel);
                        if (s_logger.isLoggable(Level.FINEST)) {
                            s_logger.finest("reading packet:" + packet + " from:" + channel.socket().getRemoteSocketAddress());
                        }
                        try {
                            final boolean done = packet.read(channel);
                            if (done) {
                                m_totalBytes += packet.size();
                                if (s_logger.isLoggable(Level.FINE)) {
                                    String remote = "null";
                                    final Socket s = channel.socket();
                                    SocketAddress sa = null;
                                    if (s != null) sa = s.getRemoteSocketAddress();
                                    if (sa != null) remote = sa.toString();
                                    s_logger.log(Level.FINE, " done reading from:" + remote + " size:" + packet.size() + " readCalls;" + packet.getReadCalls() + " total:" + m_totalBytes);
                                }
                                enque(packet);
                            }
                        } catch (final Exception e) {
                            s_logger.log(Level.FINER, "exception reading", e);
                            key.cancel();
                            m_errorReporter.error(channel, e);
                        }
                    } else if (!key.isValid()) {
                        s_logger.fine("Remotely closed");
                        final SocketChannel channel = (SocketChannel) key.channel();
                        key.cancel();
                        m_errorReporter.error(channel, new SocketException("triplea:key cancelled"));
                    }
                }
            } catch (final Exception e) {
                s_logger.log(Level.WARNING, "error in reader", e);
            }
        }
    }

    private void enque(final SocketReadData packet) {
        m_reading.remove(packet.getChannel());
        m_outputQueue.offer(packet);
    }

    private SocketReadData getReadData(final SocketChannel channel) {
        if (m_reading.containsKey(channel)) return m_reading.get(channel);
        final SocketReadData packet = new SocketReadData(channel);
        m_reading.put(channel, packet);
        return packet;
    }

    public SocketReadData take() throws InterruptedException {
        return m_outputQueue.take();
    }

    public void closed(final SocketChannel channel) {
        m_reading.remove(channel);
    }
}

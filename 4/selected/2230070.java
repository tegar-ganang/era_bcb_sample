package org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking;

import java.util.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServerException;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerTCP;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.networkmanager.VirtualServerChannelSelector;
import com.aelitis.azureus.core.networkmanager.VirtualServerChannelSelectorFactory;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;

/**
 * @author parg
 *
 */
public class TRNonBlockingServer extends TRTrackerServerTCP implements VirtualServerChannelSelector.SelectListener {

    private static final LogIDs LOGID = LogIDs.TRACKER;

    private static final int TIMEOUT_CHECK_INTERVAL = 10 * 1000;

    private static final int CLOSE_DELAY = 5 * 1000;

    private TRNonBlockingServerProcessorFactory processor_factory;

    private final VirtualChannelSelector read_selector;

    private final VirtualChannelSelector write_selector;

    private List connections_to_close = new ArrayList();

    private List processors = new ArrayList();

    private InetAddress current_bind_ip;

    private long total_timeouts;

    private long total_connections;

    public static final int MAX_CONCURRENT_CONNECTIONS = COConfigurationManager.getIntParameter("Tracker TCP NonBlocking Conc Max");

    private final AEMonitor this_mon = new AEMonitor("TRNonBlockingServer");

    private VirtualServerChannelSelector accept_server;

    private boolean immediate_close = COConfigurationManager.getBooleanParameter("Tracker TCP NonBlocking Immediate Close");

    private volatile boolean closed;

    public TRNonBlockingServer(String _name, int _port, InetAddress _bind_ip, boolean _apply_ip_filter, TRNonBlockingServerProcessorFactory _processor_factory) throws TRTrackerServerException {
        this(_name, _port, _bind_ip, _apply_ip_filter, true, _processor_factory);
    }

    public TRNonBlockingServer(String _name, int _port, InetAddress _bind_ip, boolean _apply_ip_filter, boolean _start_up_ready, TRNonBlockingServerProcessorFactory _processor_factory) throws TRTrackerServerException {
        super(_name, _port, false, _apply_ip_filter, _start_up_ready);
        processor_factory = _processor_factory;
        read_selector = new VirtualChannelSelector(_name + ":" + _port, VirtualChannelSelector.OP_READ, false);
        write_selector = new VirtualChannelSelector(_name + ":" + _port, VirtualChannelSelector.OP_WRITE, true);
        boolean ok = false;
        if (_port == 0) {
            throw (new TRTrackerServerException("port of 0 not currently supported"));
        }
        try {
            InetSocketAddress address;
            if (_bind_ip == null) {
                _bind_ip = NetworkAdmin.getSingleton().getSingleHomedServiceBindAddress();
                if (_bind_ip == null) {
                    address = new InetSocketAddress(_port);
                } else {
                    current_bind_ip = _bind_ip;
                    address = new InetSocketAddress(_bind_ip, _port);
                }
            } else {
                current_bind_ip = _bind_ip;
                address = new InetSocketAddress(_bind_ip, _port);
            }
            accept_server = VirtualServerChannelSelectorFactory.createBlocking(address, 0, this);
            accept_server.start();
            AEThread read_thread = new AEThread("TRTrackerServer:readSelector") {

                public void runSupport() {
                    selectLoop(read_selector);
                }
            };
            read_thread.setDaemon(true);
            read_thread.start();
            AEThread write_thread = new AEThread("TRTrackerServer:writeSelector") {

                public void runSupport() {
                    selectLoop(write_selector);
                }
            };
            write_thread.setDaemon(true);
            write_thread.start();
            AEThread close_thread = new AEThread("TRTrackerServer:closeScheduler") {

                public void runSupport() {
                    closeLoop();
                }
            };
            close_thread.setDaemon(true);
            close_thread.start();
            Logger.log(new LogEvent(LOGID, "TRTrackerServer: Non-blocking listener established on port " + getPort()));
            ok = true;
        } catch (Throwable e) {
            Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR, "Tracker.alert.listenfail"), new String[] { "" + getPort() });
            throw (new TRTrackerServerException("TRTrackerServer: accept fails", e));
        } finally {
            if (!ok) {
                destroySupport();
            }
        }
    }

    public InetAddress getBindIP() {
        return (current_bind_ip);
    }

    public void setImmediateClose(boolean immediate) {
        immediate_close = immediate;
    }

    protected void selectLoop(VirtualChannelSelector selector) {
        long last_time = 0;
        while (!closed) {
            try {
                selector.select(100);
                if (selector == read_selector) {
                    long now = SystemTime.getCurrentTime();
                    if (now < last_time) {
                        last_time = now;
                    } else if (now - last_time >= TIMEOUT_CHECK_INTERVAL) {
                        last_time = now;
                        checkTimeouts(now);
                    }
                }
            } catch (Throwable e) {
                Debug.printStackTrace(e);
            }
        }
    }

    public void newConnectionAccepted(ServerSocketChannel server, SocketChannel channel) {
        final TRNonBlockingServerProcessor processor = processor_factory.create(this, channel);
        int num_processors;
        try {
            this_mon.enter();
            total_connections++;
            processors.add(processor);
            num_processors = processors.size();
        } finally {
            this_mon.exit();
        }
        if (MAX_CONCURRENT_CONNECTIONS != 0 && num_processors > MAX_CONCURRENT_CONNECTIONS) {
            removeAndCloseConnection(processor);
        } else if (isIPFilterEnabled() && ip_filter.isInRange(channel.socket().getInetAddress().getHostAddress(), "Tracker", null)) {
            removeAndCloseConnection(processor);
        } else {
            VirtualChannelSelector.VirtualSelectorListener read_listener = new VirtualChannelSelector.VirtualSelectorListener() {

                private boolean selector_registered;

                public boolean selectSuccess(VirtualChannelSelector selector, SocketChannel sc, Object attachment) {
                    try {
                        int read_result = processor.processRead();
                        if (read_result == 0) {
                            if (selector_registered) {
                                read_selector.pauseSelects(sc);
                            }
                        } else if (read_result < 0) {
                            removeAndCloseConnection(processor);
                        } else {
                            if (!selector_registered) {
                                selector_registered = true;
                                read_selector.register(sc, this, null);
                            }
                        }
                        return (read_result != 2);
                    } catch (Throwable e) {
                        Debug.printStackTrace(e);
                        removeAndCloseConnection(processor);
                        return (false);
                    }
                }

                public void selectFailure(VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg) {
                    removeAndCloseConnection(processor);
                }
            };
            read_listener.selectSuccess(read_selector, channel, null);
        }
    }

    protected void readyToWrite(final TRNonBlockingServerProcessor processor) {
        final VirtualChannelSelector.VirtualSelectorListener write_listener = new VirtualChannelSelector.VirtualSelectorListener() {

            private boolean selector_registered;

            public boolean selectSuccess(VirtualChannelSelector selector, SocketChannel sc, Object attachment) {
                try {
                    int write_result = processor.processWrite();
                    if (write_result > 0) {
                        if (selector_registered) {
                            write_selector.resumeSelects(sc);
                        } else {
                            selector_registered = true;
                            write_selector.register(sc, this, null);
                        }
                    } else if (write_result == 0) {
                        removeAndCloseConnection(processor);
                    } else if (write_result < 0) {
                        processor.failed();
                        removeAndCloseConnection(processor);
                    }
                    return (write_result != 2);
                } catch (Throwable e) {
                    Debug.printStackTrace(e);
                    removeAndCloseConnection(processor);
                    return (false);
                }
            }

            public void selectFailure(VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg) {
                removeAndCloseConnection(processor);
            }
        };
        write_listener.selectSuccess(write_selector, processor.getSocketChannel(), null);
    }

    protected void removeAndCloseConnection(TRNonBlockingServerProcessor processor) {
        processor.completed();
        try {
            this_mon.enter();
            if (processors.remove(processor)) {
                read_selector.cancel(processor.getSocketChannel());
                write_selector.cancel(processor.getSocketChannel());
                if (immediate_close) {
                    try {
                        processor.closed();
                        processor.getSocketChannel().close();
                    } catch (Throwable e) {
                    }
                } else {
                    connections_to_close.add(processor);
                }
            }
        } finally {
            this_mon.exit();
        }
    }

    public void checkTimeouts(long now) {
        try {
            this_mon.enter();
            List new_processors = new ArrayList(processors.size());
            for (int i = 0; i < processors.size(); i++) {
                TRNonBlockingServerProcessor processor = (TRNonBlockingServerProcessor) processors.get(i);
                if (now - processor.getStartTime() > PROCESSING_GET_LIMIT) {
                    read_selector.cancel(processor.getSocketChannel());
                    write_selector.cancel(processor.getSocketChannel());
                    connections_to_close.add(processor);
                    total_timeouts++;
                } else {
                    new_processors.add(processor);
                }
            }
            processors = new_processors;
        } finally {
            this_mon.exit();
        }
    }

    public void closeLoop() {
        List pending_list = new ArrayList();
        long default_delay = CLOSE_DELAY * 2 / 3;
        long delay = default_delay;
        while (!closed) {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (Throwable e) {
                    Debug.printStackTrace(e);
                }
            }
            long start = SystemTime.getCurrentTime();
            for (int i = 0; i < pending_list.size(); i++) {
                try {
                    TRNonBlockingServerProcessor processor = (TRNonBlockingServerProcessor) pending_list.get(i);
                    processor.closed();
                    processor.getSocketChannel().close();
                } catch (Throwable e) {
                }
            }
            try {
                this_mon.enter();
                pending_list = connections_to_close;
                connections_to_close = new ArrayList();
            } finally {
                this_mon.exit();
            }
            long duration = SystemTime.getCurrentTime() - start;
            if (duration < 0) {
                duration = 0;
            }
            delay = default_delay - duration;
        }
    }

    protected void closeSupport() {
        closed = true;
        accept_server.stop();
        destroySupport();
    }
}

package com.aelitis.azureus.core.networkmanager.impl.tcp;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.stats.AzureusCoreStats;
import com.aelitis.azureus.core.stats.AzureusCoreStatsProvider;

public class TCPNetworkManager {

    private static int WRITE_SELECT_LOOP_TIME = 25;

    private static int WRITE_SELECT_MIN_LOOP_TIME = 0;

    private static int READ_SELECT_LOOP_TIME = 25;

    private static int READ_SELECT_MIN_LOOP_TIME = 0;

    protected static int tcp_mss_size;

    private static final TCPNetworkManager instance = new TCPNetworkManager();

    public static TCPNetworkManager getSingleton() {
        return (instance);
    }

    public static boolean TCP_INCOMING_ENABLED;

    public static boolean TCP_OUTGOING_ENABLED;

    static {
        COConfigurationManager.addAndFireParameterListener("TCP.Listen.Port.Enable", new ParameterListener() {

            public void parameterChanged(String name) {
                TCP_INCOMING_ENABLED = TCP_OUTGOING_ENABLED = COConfigurationManager.getBooleanParameter(name);
            }
        });
        COConfigurationManager.addAndFireParameterListeners(new String[] { "network.tcp.read.select.time", "network.tcp.read.select.min.time", "network.tcp.write.select.time", "network.tcp.write.select.min.time" }, new ParameterListener() {

            public void parameterChanged(String name) {
                WRITE_SELECT_LOOP_TIME = COConfigurationManager.getIntParameter("network.tcp.write.select.time");
                WRITE_SELECT_MIN_LOOP_TIME = COConfigurationManager.getIntParameter("network.tcp.write.select.min.time");
                READ_SELECT_LOOP_TIME = COConfigurationManager.getIntParameter("network.tcp.read.select.time");
                READ_SELECT_MIN_LOOP_TIME = COConfigurationManager.getIntParameter("network.tcp.read.select.min.time");
            }
        });
    }

    /**
	   * Get the configured TCP MSS (Maximum Segment Size) unit, i.e. the max (preferred) packet payload size.
	   * NOTE: MSS is MTU-40bytes for TCPIP headers, usually 1460 (1500-40) for standard ethernet
	   * connections, or 1452 (1492-40) for PPPOE connections.
	   * @return mss size in bytes
	   */
    public static int getTcpMssSize() {
        return tcp_mss_size;
    }

    public static void refreshRates(int min_rate) {
        tcp_mss_size = COConfigurationManager.getIntParameter("network.tcp.mtu.size") - 40;
        if (tcp_mss_size > min_rate) tcp_mss_size = min_rate - 1;
        if (tcp_mss_size < 512) tcp_mss_size = 512;
    }

    private final VirtualChannelSelector read_selector = new VirtualChannelSelector("TCP network manager", VirtualChannelSelector.OP_READ, true);

    private final VirtualChannelSelector write_selector = new VirtualChannelSelector("TCP network manager", VirtualChannelSelector.OP_WRITE, true);

    private final TCPConnectionManager connect_disconnect_manager = new TCPConnectionManager();

    private final IncomingSocketChannelManager incoming_socketchannel_manager = new IncomingSocketChannelManager("TCP.Listen.Port", "TCP.Listen.Port.Enable");

    private long read_select_count;

    private long write_select_count;

    protected TCPNetworkManager() {
        Set types = new HashSet();
        types.add(AzureusCoreStats.ST_NET_TCP_SELECT_READ_COUNT);
        types.add(AzureusCoreStats.ST_NET_TCP_SELECT_WRITE_COUNT);
        AzureusCoreStats.registerProvider(types, new AzureusCoreStatsProvider() {

            public void updateStats(Set types, Map values) {
                if (types.contains(AzureusCoreStats.ST_NET_TCP_SELECT_READ_COUNT)) {
                    values.put(AzureusCoreStats.ST_NET_TCP_SELECT_READ_COUNT, new Long(read_select_count));
                }
                if (types.contains(AzureusCoreStats.ST_NET_TCP_SELECT_WRITE_COUNT)) {
                    values.put(AzureusCoreStats.ST_NET_TCP_SELECT_WRITE_COUNT, new Long(write_select_count));
                }
            }
        });
        AEThread2 read_selector_thread = new AEThread2("ReadController:ReadSelector", true) {

            public void run() {
                while (true) {
                    try {
                        if (READ_SELECT_MIN_LOOP_TIME > 0) {
                            long start = SystemTime.getHighPrecisionCounter();
                            read_selector.select(READ_SELECT_LOOP_TIME);
                            long duration = SystemTime.getHighPrecisionCounter() - start;
                            duration = duration / 1000000;
                            long sleep = READ_SELECT_MIN_LOOP_TIME - duration;
                            if (sleep > 0) {
                                try {
                                    Thread.sleep(sleep);
                                } catch (Throwable e) {
                                }
                            }
                        } else {
                            read_selector.select(READ_SELECT_LOOP_TIME);
                        }
                        read_select_count++;
                    } catch (Throwable t) {
                        Debug.out("readSelectorLoop() EXCEPTION: ", t);
                    }
                }
            }
        };
        read_selector_thread.setPriority(Thread.MAX_PRIORITY - 2);
        read_selector_thread.start();
        AEThread2 write_selector_thread = new AEThread2("WriteController:WriteSelector", true) {

            public void run() {
                while (true) {
                    try {
                        if (WRITE_SELECT_MIN_LOOP_TIME > 0) {
                            long start = SystemTime.getHighPrecisionCounter();
                            write_selector.select(WRITE_SELECT_LOOP_TIME);
                            long duration = SystemTime.getHighPrecisionCounter() - start;
                            duration = duration / 1000000;
                            long sleep = WRITE_SELECT_MIN_LOOP_TIME - duration;
                            if (sleep > 0) {
                                try {
                                    Thread.sleep(sleep);
                                } catch (Throwable e) {
                                }
                            }
                        } else {
                            write_selector.select(WRITE_SELECT_LOOP_TIME);
                            write_select_count++;
                        }
                    } catch (Throwable t) {
                        Debug.out("writeSelectorLoop() EXCEPTION: ", t);
                    }
                }
            }
        };
        write_selector_thread.setPriority(Thread.MAX_PRIORITY - 2);
        write_selector_thread.start();
    }

    public void setExplicitBindAddress(InetAddress address) {
        incoming_socketchannel_manager.setExplicitBindAddress(address);
    }

    public void clearExplicitBindAddress() {
        incoming_socketchannel_manager.clearExplicitBindAddress();
    }

    public boolean isEffectiveBindAddress(InetAddress address) {
        return (incoming_socketchannel_manager.isEffectiveBindAddress(address));
    }

    /**
		 * Get the socket channel connect / disconnect manager.
		 * @return connect manager
		 */
    public TCPConnectionManager getConnectDisconnectManager() {
        return connect_disconnect_manager;
    }

    /**
	 * Get the virtual selector used for socket channel read readiness.
	 * @return read readiness selector
	 */
    public VirtualChannelSelector getReadSelector() {
        return read_selector;
    }

    /**
	 * Get the virtual selector used for socket channel write readiness.
	 * @return write readiness selector
	 */
    public VirtualChannelSelector getWriteSelector() {
        return write_selector;
    }

    public boolean isTCPListenerEnabled() {
        return (incoming_socketchannel_manager.isEnabled());
    }

    /**
	 * Get port that the TCP server socket is listening for incoming connections on.
	 * @return port number
	 */
    public int getTCPListeningPortNumber() {
        return (incoming_socketchannel_manager.getTCPListeningPortNumber());
    }

    public long getLastIncomingNonLocalConnectionTime() {
        return (incoming_socketchannel_manager.getLastNonLocalConnectionTime());
    }
}

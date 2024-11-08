package br.unb.unbiquitous.ubiquitos.network.rtp.connectionManager;

import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ChannelManager;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManagerListener;
import br.unb.unbiquitous.ubiquitos.network.ethernet.EthernetDevice;
import br.unb.unbiquitous.ubiquitos.network.ethernet.connectionManager.EthernetConnectionManager;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;
import br.unb.unbiquitous.ubiquitos.network.rtp.channelManager.EthernetRTPChannelManager;
import br.unb.unbiquitous.ubiquitos.network.rtp.rtp.RtpChannel;

public class EthernetRTPConnectionManager extends EthernetConnectionManager {

    /** The ResourceBundle to get some properties. */
    private ResourceBundle resource;

    /** Specify the passive port range to be used*/
    public static final String UBIQUITOS_ETH_RTP_PASSIVE_PORT_RANGE_KEY = "ubiquitos.eth.rtp.passivePortRange";

    public static String UBIQUITOS_ETH_RTP_PASSIVE_PORT_RANGE;

    /** Object for logging registration.*/
    public static final Logger logger = Logger.getLogger(EthernetRTPConnectionManager.class.getName());

    /** A Connection Manager Listener (ConnectionManagerControlCenter) */
    private ConnectionManagerListener connectionManagerListener = null;

    /** Server Connection */
    private EthernetDevice serverDevice;

    /** Attribute to control the closing of the Connection Manager */
    private boolean closingEthernetConnectionManager = false;

    /** The ChannelManager for new channels */
    private EthernetRTPChannelManager channelManager;

    /**
	 * Constructor
	 * @throws UbiquitOSException
	 */
    public EthernetRTPConnectionManager() throws NetworkException {
    }

    /** 
     *  Sets the Listener who will be notified when a Connections is established.
     */
    public void setConnectionManagerListener(ConnectionManagerListener connectionManagerListener) {
        this.connectionManagerListener = connectionManagerListener;
    }

    /** 
     *  Sets the ResourceBundle to get some properties.
     */
    public void setResourceBundle(ResourceBundle resourceBundle) {
        resource = resourceBundle;
        if (resource == null) {
            String msg = "ResourceBundle is null";
            logger.fatal(msg);
            throw new RuntimeException(msg);
        } else {
            try {
                UBIQUITOS_ETH_RTP_PASSIVE_PORT_RANGE = resource.getString(UBIQUITOS_ETH_RTP_PASSIVE_PORT_RANGE_KEY);
            } catch (Exception e) {
                String msg = "Incorrect ethernet rtp port";
                logger.fatal(msg);
                throw new RuntimeException(msg);
            }
        }
    }

    /**
	 * Finalize the Connection Manager.
	 */
    public synchronized void tearDown() {
        try {
            closingEthernetConnectionManager = true;
            logger.debug("Closing Ethernet RTP Connection Manager...");
            if (channelManager != null) {
                channelManager.tearDown();
            }
            RtpChannel.tearDown();
            notify();
            logger.debug("Ethernet RTP Connection Manager is closed.");
        } catch (Exception ex) {
            closingEthernetConnectionManager = false;
            String msg = "Error closing Ethernet RTP Connection Manager. ";
            logger.fatal(msg, ex);
            ex.printStackTrace();
            throw new RuntimeException(msg + ex);
        }
    }

    /**
	 * A method for retrieve the networkDevice of this connection.
	 * @return networkDevice
	 */
    public NetworkDevice getNetworkDevice() {
        if (serverDevice == null) {
            serverDevice = new EthernetDevice("0.0.0.0", -1, EthernetConnectionType.RTP);
        }
        return serverDevice;
    }

    /**
	 * A method for retrive the channel manager of this connection manager
	 * @return channel managar
	 */
    public ChannelManager getChannelManager() {
        if (channelManager == null) {
            channelManager = new EthernetRTPChannelManager(UBIQUITOS_ETH_RTP_PASSIVE_PORT_RANGE);
        }
        return channelManager;
    }

    /**
	 * Method extends from Runnable. Starts the connection Manager
	 */
    public void run() {
        logger.debug("Starting UbiquitOS Smart-Space Ethernet RTP Connection Manager.");
        logger.info("Starting Ethernet RTP Connection Manager...");
        logger.info("Ethernet RTP Connection Manager is started.");
        while (true) {
            try {
                synchronized (this) {
                    wait();
                    if (closingEthernetConnectionManager) return;
                }
            } catch (Exception ex) {
                if (!closingEthernetConnectionManager) {
                    String msg = "Error starting Ethernet RTP Connection Manager. ";
                    logger.fatal(msg, ex);
                    throw new RuntimeException(msg + ex);
                } else {
                    return;
                }
            }
        }
    }
}

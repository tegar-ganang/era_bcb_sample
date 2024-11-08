package br.unb.unbiquitous.ubiquitos.network.loopback.connectionManager;

import java.io.IOException;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ChannelManager;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManager;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManagerListener;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.network.loopback.LoopbackChannel;
import br.unb.unbiquitous.ubiquitos.network.loopback.LoopbackDevice;
import br.unb.unbiquitous.ubiquitos.network.loopback.channelManager.LoopbackChannelManager;
import br.unb.unbiquitous.ubiquitos.network.loopback.connection.LoopbackServerConnection;
import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;

/**
 * 
 * This class is the connection manager of loopback connections. It is responsible for listening
 * to new incoming connections and handling them correctly.
 * 
 * @author Lucas Paranhos Quintella
 *
 */
public class LoopbackConnectionManager implements ConnectionManager {

    /** Object for logging registration.*/
    public static final Logger logger = Logger.getLogger(LoopbackConnectionManager.class.getName());

    /** A Connection Manager Listener (ConnectionManagerControlCenter) */
    private ConnectionManagerListener connectionManagerListener;

    /** The ChannelManager for new data channels */
    private LoopbackChannelManager channelManager;

    /** The ResourceBundle to get some properties. */
    @SuppressWarnings("unused")
    private ResourceBundle resource;

    /** The device that will be listening to new connections */
    private LoopbackDevice listeningDevice;

    private LoopbackServerConnection listeningConnection;

    /** Indicates that some thread tried to tear down the Connection Manager */
    private boolean closingLoopbackConnectionManager;

    /** The default ID of the Connection Manager server that will be listening to connections */
    public static final int DEFAULT_ID = 0;

    /**
	 * Constructor
	 * @throws UbiquitOSException
	 */
    public LoopbackConnectionManager() throws NetworkException {
        LoopbackDevice.initDevicesID();
        LoopbackChannel.initChannel();
    }

    /**
	 * Getter of the Channel Manager of the Connection Manager
	 * @return The Channel Manager
	 */
    public synchronized ChannelManager getChannelManager() {
        if (this.channelManager == null) {
            this.channelManager = new LoopbackChannelManager();
        }
        return this.channelManager;
    }

    /**
	 * Returns the server device that is listening to new incoming connections.
	 * @return The server device
	 */
    public synchronized NetworkDevice getNetworkDevice() {
        if (this.listeningDevice == null) {
            this.listeningDevice = new LoopbackDevice(DEFAULT_ID);
        }
        return this.listeningDevice;
    }

    /**
	 * Setter of the middleware's Network Control Center
	 * @param connectionManagerListener Reference for the Control Center
	 */
    public void setConnectionManagerListener(ConnectionManagerListener connectionManagerListener) {
        this.connectionManagerListener = connectionManagerListener;
    }

    /**
	 * Setter of the resource bundle
	 * @param resource Reference to the resource bundle
	 */
    public void setResourceBundle(ResourceBundle resource) {
        this.resource = resource;
    }

    /**
	 * Tears down the Connection Manager and all of its dependencies.
	 */
    public void tearDown() {
        try {
            logger.debug("Closing Loopback Connection Manager...");
            this.closingLoopbackConnectionManager = true;
            this.listeningConnection.closeConnection();
            if (this.channelManager != null) {
                this.channelManager.tearDown();
            }
            LoopbackChannel.tearDown();
        } catch (Exception e) {
            this.closingLoopbackConnectionManager = false;
            String msg = "Error stoping Loopback Connection Manager. ";
            logger.fatal(msg, e);
            throw new RuntimeException(msg + e);
        }
    }

    /**
	 * Waits for new connections indefinitely and handle them properly.
	 */
    public void run() {
        logger.debug("Starting uOS Smart-Space Loopback Connection Manager.");
        logger.info("Starting Loopback Connection Manager...");
        this.listeningConnection = new LoopbackServerConnection(getNetworkDevice());
        logger.info("Loopback Connection Manager is started.");
        while (true) {
            try {
                this.connectionManagerListener.handleClientConnection(this.listeningConnection.accept());
                logger.info("Loopback Connection Manager -- Connection handled!");
            } catch (IOException ex) {
                if (!closingLoopbackConnectionManager) {
                    String msg = "Error handling connection at Loopback Connection Manager. ";
                    logger.fatal(msg, ex);
                    throw new RuntimeException(msg + ex);
                } else {
                    logger.debug("Loopback Connection Manager is closed.");
                    return;
                }
            }
        }
    }
}

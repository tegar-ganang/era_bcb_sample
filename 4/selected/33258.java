package br.unb.unbiquitous.ubiquitos.network.bluetooth.connectionManager;

import java.util.ResourceBundle;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connection;
import org.apache.log4j.Logger;
import bluetooth.BtUtil;
import bluetooth.BtUtilException;
import bluetooth.BtUtilServerListener;
import br.unb.unbiquitous.ubiquitos.network.bluetooth.BluetoothDevice;
import br.unb.unbiquitous.ubiquitos.network.bluetooth.channelManager.BluetoothChannelManager;
import br.unb.unbiquitous.ubiquitos.network.bluetooth.connection.BluetoothClientConnection;
import br.unb.unbiquitous.ubiquitos.network.bluetooth.connection.BluetoothServerConnection;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ChannelManager;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManager;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManagerListener;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;

/**
 * Manage the ubiquitos-smartspace service interface.
 *
 * @author Passarinho
 */
public class BluetoothConnectionManager implements ConnectionManager, BtUtilServerListener {

    /** The ResourceBundle to get some properties. */
    private ResourceBundle resource;

    /** Specify the client and the provider for the bluetooth connection */
    private static final String UBIQUITOS_BTH_PROVIDER_KEY = "ubiquitos.bth.provider";

    public static String UBIQUITOS_BTH_PROVIDER;

    private static final String UBIQUITOS_BTH_CLIENT_KEY = "ubiquitos.bth.client";

    private String UBIQUITOS_BTH_CLIENT;

    /** Object for logging registration.*/
    private static final Logger logger = Logger.getLogger(BluetoothConnectionManager.class.getName());

    /** A simple way to handle the bluetooth stuff. */
    private BtUtil btUtil = null;

    /** A Connection Manager Listener (ConnectionManagerControlCenter) */
    private ConnectionManagerListener connectionManagerListener = null;

    /** Server Connection */
    private BluetoothDevice serverDevice;

    private BluetoothServerConnection server;

    /** The ChannelManager for new channels */
    private BluetoothChannelManager channelManager;

    /**
	 * Constructor
	 * @throws UbiquitOSException
	 */
    public BluetoothConnectionManager() throws NetworkException {
        try {
            btUtil = new BtUtil();
        } catch (BtUtilException ex) {
            throw new NetworkException("Error creating Bluetooth Connection Manager: " + ex.toString());
        }
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
        this.resource = resourceBundle;
    }

    /**
	 * Finalize the Connection Manager
	 */
    public void tearDown() {
        try {
            logger.debug("Closing Bluetooth Connection Manager...");
            server.closeConnection();
            if (channelManager != null) {
                channelManager.tearDown();
            }
            logger.debug("Bluetooth Connection Manager is closed.");
        } catch (Exception e) {
            String msg = "Error stoping Bluetooth Connection Manager. ";
            logger.fatal(msg, e);
        }
    }

    /**
	 * Method extends from Runnable. Starts the connection Manager
	 */
    public void run() {
        logger.debug("Starting UbiquitOS Smart-Space Bluetooth Connection Manager.");
        logger.info("Starting Bluetooth Connection Manager...");
        if (resource == null) {
            String msg = "ResourceBundle is null";
            logger.fatal(msg);
            throw new RuntimeException(msg);
        } else {
            UBIQUITOS_BTH_PROVIDER = resource.getString(UBIQUITOS_BTH_PROVIDER_KEY);
            UBIQUITOS_BTH_CLIENT = resource.getString(UBIQUITOS_BTH_CLIENT_KEY);
        }
        try {
            server = new BluetoothServerConnection(this, btUtil, (BluetoothDevice) getNetworkDevice(), UBIQUITOS_BTH_CLIENT);
            server.start();
            logger.info("Bluetooth Connection Manager is started.");
        } catch (Exception ex) {
            String msg = "Error starting Bluetooth Connection Manager. ";
            logger.fatal(msg, ex);
        }
    }

    /**
     * Method invoked by the BtUtil when a connection is established with the server.
     * @param arg0
     */
    public void connectionEstablished(Connection con) {
        logger.debug("Bluetooth Connection Manager received a connection! Delegating it...");
        RemoteDevice client = btUtil.getClientDevice(con);
        if (client == null) return;
        BluetoothClientConnection clientConnection = null;
        try {
            clientConnection = new BluetoothClientConnection(con, new BluetoothDevice(client, btUtil), null);
        } catch (Exception e) {
            logger.error("Error while creating bluetooth client connection. " + e);
        }
        connectionManagerListener.handleClientConnection(clientConnection);
    }

    /**
	 * A method for retrieve the networkDevice of this connection.
	 * @return networkDevice
	 */
    public NetworkDevice getNetworkDevice() {
        if (serverDevice == null) {
            try {
                serverDevice = new BluetoothDevice(LocalDevice.getLocalDevice(), UBIQUITOS_BTH_CLIENT, btUtil);
            } catch (BluetoothStateException e) {
                throw new RuntimeException("Error creating Bluetooth Connection Manager: " + e.toString());
            }
        }
        return serverDevice;
    }

    /**
	 * A method for retrive the channel manager of this connection manager
	 * @return channel managar
	 */
    public ChannelManager getChannelManager() {
        if (channelManager == null) {
            channelManager = new BluetoothChannelManager(btUtil, UBIQUITOS_BTH_CLIENT);
        }
        return channelManager;
    }
}

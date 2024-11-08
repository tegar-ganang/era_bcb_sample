package br.unb.unbiquitous.ubiquitos.network.connectionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.network.model.NetworkDevice;
import br.unb.unbiquitous.ubiquitos.network.model.connection.ClientConnection;

/**
 * Manage the ubiquitos-smartspace service interface.
 *
 * @author Passarinho
 */
public class ConnectionManagerControlCenter implements ConnectionManagerListener {

    private static Logger logger = Logger.getLogger(ConnectionManagerControlCenter.class);

    private static final String PARAM_SEPARATOR = ",";

    private static final String CONNECTION_MANAGER_CLASS_KEY = "ubiquitos.connectionManager";

    private List<ConnectionManager> connectionManagersList;

    private Map<ConnectionManager, Thread> connectionManagersThreadMap;

    private Map<String, ConnectionManager> connectionManagersMap;

    /** The Adaptability Engine module reference. */
    private MessageListener messageListener = null;

    /** The resource bundle from where we can get a set of configurations. */
    private ResourceBundle resource;

    /**
	 * Constructor using AdaptabilityEngine
	 * @param AdaptabilityEngine
	 * @throws UbiquitOSException
	 */
    public ConnectionManagerControlCenter(MessageListener messageListener, ResourceBundle resourceBundle) throws NetworkException {
        this.resource = resourceBundle;
        this.messageListener = messageListener;
        loadAndStartConnectionManagers();
    }

    /**
     * Method invoked to handle a connection established from a UbiquitOS Client.
     * @param con
     * @throws UbiquitOSException
     */
    public void handleClientConnection(ClientConnection clientConnection) {
        ThreadedConnectionHandler threadedConnectionHandling = new ThreadedConnectionHandler(clientConnection, messageListener);
        threadedConnectionHandling.start();
    }

    /**
	 * Finalize the Connection Manager.
	 */
    public void tearDown() {
        for (ConnectionManager cm : connectionManagersList) {
            cm.tearDown();
            try {
                connectionManagersThreadMap.get(cm).join();
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    /**
     * A method for retrieve all network devices that are waiting for connection in connection managers.
     * @return list of networkDevice.
     */
    public List<NetworkDevice> getNetworkDevices() {
        List<NetworkDevice> networkDeviceList = new ArrayList<NetworkDevice>();
        for (ConnectionManager cm : connectionManagersList) {
            networkDeviceList.add(cm.getNetworkDevice());
        }
        return networkDeviceList;
    }

    /**
     * A method for retrieve the networkDevice of the given type from the connectionManager.
     * @param networkDeviceType
     * @return networkDevice
     * @throws NetworkException
     */
    public NetworkDevice getNetworkDevice(String networkDeviceType) throws NetworkException {
        ConnectionManager connectionManager = null;
        for (ConnectionManager cm : connectionManagersList) {
            if (cm.getNetworkDevice().getNetworkDeviceType().equals(networkDeviceType)) {
                connectionManager = cm;
                break;
            }
        }
        if (connectionManager == null) {
            throw new NetworkException("There is no Connection Manager for the given connection type: " + networkDeviceType);
        }
        return connectionManager.getNetworkDevice();
    }

    /**
     * A method for retrieve an available networkDevice of the given type.
     * @param networkDeviceType
     * @return networkDevice
     * @throws NetworkException
     */
    public NetworkDevice getAvailableNetworkDevice(String networkDeviceType) throws NetworkException {
        ChannelManager channelManager = null;
        for (ConnectionManager cm : connectionManagersList) {
            if (cm.getNetworkDevice().getNetworkDeviceType().equals(networkDeviceType)) {
                channelManager = cm.getChannelManager();
                break;
            }
        }
        if (channelManager == null) {
            throw new NetworkException("There is no Channel Manager for the given connection type: " + networkDeviceType);
        }
        return channelManager.getAvailableNetworkDevice();
    }

    /**
     * Retrieve the channelID from the given networkDevice.
     * @param networkDevice
     * @return channelID
     * @throws Exception
     */
    public String getChannelID(String networkDeviceName) {
        return networkDeviceName.split(":")[1];
    }

    /**
     * Retrieve the host from the given networkDevice.
     * @param networkDevice
     * @return host
     * @throws Exception
     */
    public String getHost(String networkDeviceName) {
        return networkDeviceName.split(":")[0];
    }

    /**
     * Open a passive connection based on the networkDeviceName and networkDeviceType given, when the remote
     * host connect on this passive connection a clientConnection is created e returned. 
     * @param networkDeviceName
     * @param networkDeviceType
     * @return clientConnection
     * @throws NetworkException
     */
    public ClientConnection openPassiveConnection(String networkDeviceName, String networkDeviceType) throws NetworkException {
        ChannelManager channelManager = null;
        for (ConnectionManager cm : connectionManagersList) {
            if (cm.getNetworkDevice().getNetworkDeviceType().equals(networkDeviceType)) {
                channelManager = cm.getChannelManager();
                break;
            }
        }
        if (channelManager == null) {
            throw new NetworkException("There is no Channel Manager for the given connection type: " + networkDeviceType);
        }
        try {
            return channelManager.openPassiveConnection(networkDeviceName);
        } catch (Exception e) {
            throw new NetworkException("Could not create channel.", e);
        }
    }

    /**
     * Open a active connection, clientConnection, based on the networkDeviceName and networkDeviceType given. 
     * @param networkDeviceName
     * @param networkDeviceType
     * @return clientConnection
     * @throws NetworkException
     */
    public ClientConnection openActiveConnection(String networkDeviceName, String networkDeviceType) throws NetworkException {
        ChannelManager channelManager = null;
        for (ConnectionManager cm : connectionManagersList) {
            if (cm.getNetworkDevice().getNetworkDeviceType().equals(networkDeviceType)) {
                channelManager = cm.getChannelManager();
                break;
            }
        }
        if (channelManager == null) {
            throw new NetworkException("There is no Channel Manager for the given connection type: " + networkDeviceType);
        }
        try {
            return channelManager.openActiveConnection(networkDeviceName);
        } catch (Exception e) {
            throw new NetworkException("Could not create channel.", e);
        }
    }

    /**
	 * Loads dynamically the Connection Managers defined in the UbiquitOS properties file
	 */
    private void loadAndStartConnectionManagers() throws NetworkException {
        connectionManagersList = new ArrayList<ConnectionManager>();
        connectionManagersMap = new HashMap<String, ConnectionManager>();
        connectionManagersThreadMap = new HashMap<ConnectionManager, Thread>();
        try {
            String connectionPropertie = null;
            if (this.resource != null) {
                connectionPropertie = resource.getString(CONNECTION_MANAGER_CLASS_KEY);
            }
            String[] connectionsArray = null;
            if (connectionPropertie != null) {
                connectionsArray = connectionPropertie.split(PARAM_SEPARATOR);
            }
            for (String radar : connectionsArray) {
                @SuppressWarnings("rawtypes") Class c = Class.forName(radar);
                ConnectionManager newConMan = (ConnectionManager) c.newInstance();
                newConMan.setConnectionManagerListener(this);
                newConMan.setResourceBundle(resource);
                connectionManagersList.add(newConMan);
                connectionManagersMap.put(radar, newConMan);
            }
        } catch (Exception e) {
            NetworkException ex = new NetworkException("Error reading UbiquitOS Resource Bundle Propertie File. " + "Check if the files exists or there is no errors in his definitions." + " The found error is: " + e.getMessage());
            throw ex;
        }
        if (connectionManagersList == null || connectionManagersList.isEmpty()) {
            NetworkException ex = new NetworkException("There is no Connection Managers defined on Connection Managers Control Center");
            throw ex;
        }
        for (ConnectionManager connectionManager : connectionManagersList) {
            Thread t = new Thread(connectionManager);
            t.start();
            connectionManagersThreadMap.put(connectionManager, t);
        }
    }

    /**
     * Returns the current instance of the informed connection manager.
     * 
     * @param cManagerClass Name of the class of the connection manager to be found.
     * @return ConnectionManager if found. Null otherwise.
     */
    public ConnectionManager findConnectionManagerInstance(String cManagerClass) {
        return connectionManagersMap.get(cManagerClass);
    }
}

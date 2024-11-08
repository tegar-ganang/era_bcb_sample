package br.unb.unbiquitous.ubiquitos.network.bluetooth.radar;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import javax.bluetooth.RemoteDevice;
import org.apache.log4j.Logger;
import bluetooth.BtUtil;
import bluetooth.BtUtilException;
import br.unb.unbiquitous.ubiquitos.network.bluetooth.BluetoothDevice;
import br.unb.unbiquitous.ubiquitos.network.bluetooth.channelManager.BluetoothChannelManager;
import br.unb.unbiquitous.ubiquitos.network.connectionManager.ConnectionManager;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.network.model.connection.ClientConnection;
import br.unb.unbiquitous.ubiquitos.network.radar.Radar;
import br.unb.unbiquitous.ubiquitos.network.radar.RadarListener;

/**
 * This class implements a Bluetooth Radar for the smart-space
 * It implements 3 interfaces:
 *   Runnable - For running on a independent thread
 *   BtUtilClientListener - for recieving the Bluetooth discovery events
 *   Radar - For stating and stoping the Radar. Default for all UbiquitOS radars
 *   
 * It has a listener that is invoked when a device enter or exist of it bluetooth area.
 *   
 *
 * @author alegomes & Passarinho
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BluetoothRadar implements Radar {

    private static final int RESTART_WAIT_TIME = 1000;

    private static final int WAIT_TIME_BETWEEN_RETRIES = 100;

    private static final int MAX_CONNECT_RETRIES = 1;

    /** Object for logging registration.
     * Rembember that: DEBUG < INFO < WARN < ERROR < FATAL */
    public static final Logger logger = Logger.getLogger(Radar.class.getName());

    /** A simple way to handle the bluetooth stuff. */
    private BtUtil btUtil = null;

    /** This is the list of devices present in the smart-space. */
    private Vector neighborDevices = new Vector();

    /** A RadarListener object interested in receiving UbiquitOS Radar notifications, 
     * like "a new device has entered the smart-space" and "device X has left the smart-space". */
    private RadarListener radarListener;

    /** Indicates whether the radar is running or not. */
    private boolean started = false;

    /** Shared device pool **/
    private BluetoothDevicePool devicePoll = BluetoothDevicePool.getInstance();

    /** Properties used to control known devices from the smart-space **/
    private Set<BluetoothDevice> discoveredDevices = new HashSet<BluetoothDevice>();

    /** Property that stores address of the devices present in the last search**/
    private Set<String> lastSearchDevices = new HashSet<String>();

    /**
     * The connection manager responsible for handling the information of connections.
     */
    private ConnectionManager connectionManager;

    /**
     * Constructor
     * @param listener Some object interested in receive Radar notifications
     *  about devices entrance and exit.
     */
    public BluetoothRadar(RadarListener listener) throws NetworkException {
        try {
            btUtil = new BtUtil();
        } catch (BtUtilException ex) {
            throw new NetworkException("[BluetoothRadar] Error creating Radar Subsystem: " + ex.toString());
        }
        radarListener = listener;
    }

    /**
     * Runnable implementation
     *  - called my runnable.start() method to start the thread
     */
    public void run() {
        while (started) {
            try {
                logger.debug("[BluetoothRadar] Starting a new device discovey...");
                Vector<RemoteDevice> devices = btUtil.discoverDevicesSync();
                logger.debug("[BluetoothRadar] Querying Devices Discovered.");
                deviceDiscoveryFinished(devices);
                logger.debug("[BluetoothRadar] Discovered Finished. Waiting " + RESTART_WAIT_TIME + "ms for restart.");
                Thread.sleep(RESTART_WAIT_TIME);
            } catch (Exception e) {
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Re-start the radar
     */
    public void reRun() {
        if (started == false) {
            startRadar();
            Thread t = new Thread() {

                @Override
                public void run() {
                    try {
                        logger.debug("[BluetoothRadar] Starting a new device discovey...");
                        btUtil.discoverDevicesAsync();
                        logger.debug("[BluetoothRadar] A new device discovery was started.");
                    } catch (BtUtilException e) {
                        logger.debug("[BluetoothRadar] The device discovery process could not be started. " + e);
                    }
                }
            };
            t.start();
        }
    }

    /**
     * Start the space scan process.
     */
    public void startRadar() {
        started = true;
        try {
            devicePoll.radarActive(this);
        } catch (NetworkException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Stop the space scan process.
     */
    public void stopRadar() {
        synchronized (devicePoll) {
            try {
                started = false;
                devicePoll.radarInactive(this);
                devicePoll.wait();
            } catch (NetworkException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e1) {
                logger.error("Could not stop radar", e1);
            }
        }
    }

    /**
     * Part of the BtUtilClientListener interface implementation.
     * Method invoked when a new device is discovered by the inquiry process
     * started by the <code>BtUtil.discoverDevicesAsync()</code> method.
     * 
     * @param device A reference to the just discovered device.
     */
    public void deviceDiscovered(RemoteDevice device) {
        synchronized (discoveredDevices) {
            String deviceName = btUtil.getDeviceName(device);
            logger.debug("[BluetoothRadar] A device was found [" + deviceName + "].");
            logger.info("[BluetoothRadar] [" + deviceName + "] is in the smart-space.");
        }
    }

    /**
     * This method checks if the device is uOS compliant. 
     * This is done checking if the control channel is opened.
     * 
     * @param device Device to be queried.
     * @param deviceName Name of the device.
     * @return <code>true</code> if it is uOS compliant
     */
    private boolean isDeviceUosCompliant(RemoteDevice device, String deviceName) {
        int retries = 0;
        while (retries < MAX_CONNECT_RETRIES) {
            try {
                logger.debug("[BluetoothRadar] Checking if " + deviceName + " is uOS compliant. " + (retries + 1) + " of " + MAX_CONNECT_RETRIES + " chances.");
                ClientConnection cc = ((BluetoothChannelManager) connectionManager.getChannelManager()).openActiveConnection(device.getBluetoothAddress());
                cc.closeConnection();
                logger.debug("[BluetoothRadar] " + deviceName + " is uOS compliant.");
                return true;
            } catch (Exception e) {
                logger.info("Failed to connect to open a control channel with device " + deviceName, e);
                retries++;
                if (retries < MAX_CONNECT_RETRIES) {
                    try {
                        Thread.sleep(WAIT_TIME_BETWEEN_RETRIES);
                    } catch (InterruptedException e1) {
                        logger.error("Failed to connect to open a control channel with device " + deviceName, e1);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Part of the BtUtilClientListener interface implementation.
     * Method invoked when inquiry process started by the
     * <code>BtUtil.discoverDevicesAsync()</code> method is finished.
     * 
     * @param devices A list of javax.bluetooth.RemoteDevice objects.
     */
    public void deviceDiscoveryFinished(Vector devices) {
        logger.debug("[BluetoothRadar] Device discovery finished. " + neighborDevices.size() + " devices found before " + getDeviceNameList(neighborDevices) + ". " + devices.size() + " devices just found " + getDeviceNameList(devices));
        Vector devicesRecentlyDiscovered = new Vector();
        devicesRecentlyDiscovered.addAll(devices);
        if (started) {
            Set<String> devicesIn = new HashSet<String>();
            logger.debug("[BluetoothRadar]  " + devices.size() + " devices to be notifyied.");
            for (Object device : devices) {
                BluetoothDevice bd = new BluetoothDevice((RemoteDevice) device, btUtil);
                DeviceDiscoveryNotifyier notifyier = new DeviceDiscoveryNotifyier(bd);
                notifyier.run();
                lastSearchDevices.add(bd.getNetworkDeviceName());
                devicesIn.add(bd.getNetworkDeviceName());
            }
            Set<String> devicesLeft = new HashSet<String>();
            for (String lastDevice : lastSearchDevices) {
                if (!devicesIn.contains(lastDevice)) {
                    devicesLeft.add(lastDevice);
                    BluetoothDevice bd = new BluetoothDevice(lastDevice);
                    radarListener.deviceLeft(bd);
                }
            }
            lastSearchDevices = devicesIn;
            discoveredDevices.clear();
        }
        RemoteDevice device = null;
        for (int i = 0; i < neighborDevices.size(); i++) {
            device = (RemoteDevice) neighborDevices.elementAt(i);
            if (!devicesRecentlyDiscovered.contains(device)) {
                devicePoll.removeDevice(device);
                notifyDeviceLeft(device);
            }
        }
        neighborDevices = new Vector();
        neighborDevices.addAll(devicesRecentlyDiscovered);
    }

    private class DeviceDiscoveryNotifyier implements Runnable {

        private BluetoothDevice device;

        public DeviceDiscoveryNotifyier(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            devicePoll.addDevice(device.getRemoteDevice());
            if (isDeviceUosCompliant(device.getRemoteDevice(), btUtil.getDeviceName(device.getRemoteDevice()))) {
                logger.debug("[BluetoothRadar] Notifying Listener about device " + device.getNetworkDeviceName() + "...");
                try {
                    radarListener.deviceEntered(device);
                } catch (Exception e) {
                    logger.error("[BluetoothRadar] The device could not be properly discovered. ", e);
                }
            }
        }
    }

    /**
     * Part of the BtUtilClientListener interface implementation.
     * Method invoked when the service discovery process, started by the
     * <code>BtUtil.discoverServicesAsync()</code>, is finished.
     * @param services A list of javax.bluetooth.ServiceRecord objects.
     */
    public void serviceDiscoveryFinished(Vector services) {
        if (services != null) {
            for (Object object : services) {
                logger.debug("Service:" + object);
            }
        }
    }

    /**
     * Method invoked when a device inquiry or a service discovery fails and
     * a new one is about to start.
     * @param _try The try number.
     */
    public void tryingAgain(int _try) {
        logger.debug(".");
    }

    /**
     * Get a legible name list of the specified devices in the vector.
     */
    private String getDeviceNameList(Vector devices) {
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        RemoteDevice d = null;
        for (int i = 0; i < devices.size(); i++) {
            d = (RemoteDevice) devices.elementAt(i);
            buf.append(btUtil.getDeviceName(d) + ", ");
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * Notify listeners that a device has left the smart-space.
     * @param device The device that left the smart-space.
     */
    private void notifyDeviceLeft(RemoteDevice device) {
        String deviceName = null;
        logger.info("[BluetoothRadar] [" + deviceName + "] device has left the smart-space.");
        BluetoothDevice bluetoothDevice = new BluetoothDevice(device, btUtil);
        radarListener.deviceLeft(bluetoothDevice);
        neighborDevices.remove(device);
        devicePoll.removeDevice(device);
    }

    /**
	 * @return the started
	 */
    public boolean isStarted() {
        return started;
    }

    @Override
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
}

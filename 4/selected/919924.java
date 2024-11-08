package com.sshtools.j2ssh.forwarding;

import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.connection.Channel;
import com.sshtools.j2ssh.connection.ChannelFactory;
import com.sshtools.j2ssh.connection.ConnectionProtocol;
import com.sshtools.j2ssh.connection.InvalidChannelException;
import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.util.StartStopState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketPermission;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 *
 * @author $author$
 * @version $Revision: 1.37 $
 */
public class ForwardingClient implements ChannelFactory {

    private static Log log = LogFactory.getLog(ForwardingClient.class);

    /**  */
    public static final String REMOTE_FORWARD_REQUEST = "tcpip-forward";

    /**  */
    public static final String REMOTE_FORWARD_CANCEL_REQUEST = "cancel-tcpip-forward";

    private ConnectionProtocol connection;

    private List channelTypes = new Vector();

    private Map localForwardings = new HashMap();

    private Map remoteForwardings = new HashMap();

    private XDisplay xDisplay;

    private ForwardingConfiguration x11ForwardingConfiguration;

    /**
     * Creates a new ForwardingClient object.
     *
     * @param connection
     *
     * @throws IOException
     */
    public ForwardingClient(ConnectionProtocol connection) throws IOException {
        this.connection = connection;
        connection.addChannelFactory(ForwardingSocketChannel.REMOTE_FORWARDING_CHANNEL, this);
        connection.addChannelFactory(ForwardingSocketChannel.X11_FORWARDING_CHANNEL, this);
    }

    /**
     *
     *
     * @return
     */
    public List getChannelType() {
        return channelTypes;
    }

    /**
     *
     *
     * @param localDisplay
     */
    public void enableX11Forwarding(XDisplay localDisplay) {
        xDisplay = localDisplay;
        x11ForwardingConfiguration = new ForwardingConfiguration("x11", "", 0, xDisplay.getHost(), xDisplay.getPort());
    }

    /**
     *
     *
     * @return
     */
    public ForwardingConfiguration getX11ForwardingConfiguration() {
        return x11ForwardingConfiguration;
    }

    /**
     *
     *
     * @return
     */
    public boolean hasActiveConfigurations() {
        if ((localForwardings.size() == 0) && (remoteForwardings.size() == 0)) {
            return false;
        }
        Iterator it = localForwardings.values().iterator();
        while (it.hasNext()) {
            if (((ForwardingConfiguration) it.next()).getState().getValue() == StartStopState.STARTED) {
                return true;
            }
        }
        it = remoteForwardings.values().iterator();
        while (it.hasNext()) {
            if (((ForwardingConfiguration) it.next()).getState().getValue() == StartStopState.STARTED) {
                return true;
            }
        }
        return false;
    }

    public void synchronizeConfiguration(SshConnectionProperties properties) {
        ForwardingConfiguration fwd = null;
        if (properties.getLocalForwardings().size() > 0) {
            for (Iterator it = properties.getLocalForwardings().values().iterator(); it.hasNext(); ) {
                try {
                    fwd = (ForwardingConfiguration) it.next();
                    fwd = addLocalForwarding(fwd);
                    if (properties.getForwardingAutoStartMode()) {
                        startLocalForwarding(fwd.getName());
                    }
                } catch (Throwable ex) {
                    log.warn((("Failed to start local forwarding " + fwd) != null) ? fwd.getName() : "", ex);
                }
            }
        }
        if (properties.getRemoteForwardings().size() > 0) {
            for (Iterator it = properties.getRemoteForwardings().values().iterator(); it.hasNext(); ) {
                try {
                    fwd = (ForwardingConfiguration) it.next();
                    addRemoteForwarding(fwd);
                    if (properties.getForwardingAutoStartMode()) {
                        startRemoteForwarding(fwd.getName());
                    }
                } catch (Throwable ex) {
                    log.warn((("Failed to start remote forwarding " + fwd) != null) ? fwd.getName() : "", ex);
                }
            }
        }
    }

    /**
     *
     *
     * @return
     */
    public boolean hasActiveForwardings() {
        if ((localForwardings.size() == 0) && (remoteForwardings.size() == 0)) {
            return false;
        }
        Iterator it = localForwardings.values().iterator();
        while (it.hasNext()) {
            if (((ForwardingConfiguration) it.next()).getActiveForwardingSocketChannels().size() > 0) {
                return true;
            }
        }
        it = remoteForwardings.values().iterator();
        while (it.hasNext()) {
            if (((ForwardingConfiguration) it.next()).getActiveForwardingSocketChannels().size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     *
     * @param addressToBind
     * @param portToBind
     *
     * @return
     *
     * @throws ForwardingConfigurationException
     */
    public ForwardingConfiguration getLocalForwardingByAddress(String addressToBind, int portToBind) throws ForwardingConfigurationException {
        Iterator it = localForwardings.values().iterator();
        ForwardingConfiguration config;
        while (it.hasNext()) {
            config = (ForwardingConfiguration) it.next();
            if (config.getAddressToBind().equals(addressToBind) && (config.getPortToBind() == portToBind)) {
                return config;
            }
        }
        throw new ForwardingConfigurationException("The configuration does not exist");
    }

    /**
     *
     *
     * @param name
     *
     * @return
     *
     * @throws ForwardingConfigurationException
     */
    public ForwardingConfiguration getLocalForwardingByName(String name) throws ForwardingConfigurationException {
        if (!localForwardings.containsKey(name)) {
            throw new ForwardingConfigurationException("The configuraiton does not exist!");
        }
        return (ForwardingConfiguration) localForwardings.get(name);
    }

    /**
     *
     *
     * @param name
     *
     * @return
     *
     * @throws ForwardingConfigurationException
     */
    public ForwardingConfiguration getRemoteForwardingByName(String name) throws ForwardingConfigurationException {
        if (!remoteForwardings.containsKey(name)) {
            throw new ForwardingConfigurationException("The configuraiton does not exist!");
        }
        return (ForwardingConfiguration) remoteForwardings.get(name);
    }

    /**
     *
     *
     * @return
     */
    public Map getLocalForwardings() {
        return localForwardings;
    }

    /**
     *
     *
     * @return
     */
    public Map getRemoteForwardings() {
        return remoteForwardings;
    }

    /**
     *
     *
     * @param addressToBind
     * @param portToBind
     *
     * @return
     *
     * @throws ForwardingConfigurationException
     */
    public ForwardingConfiguration getRemoteForwardingByAddress(String addressToBind, int portToBind) throws ForwardingConfigurationException {
        Iterator it = remoteForwardings.values().iterator();
        ForwardingConfiguration config;
        while (it.hasNext()) {
            config = (ForwardingConfiguration) it.next();
            if (config.getAddressToBind().equals(addressToBind) && (config.getPortToBind() == portToBind)) {
                return config;
            }
        }
        throw new ForwardingConfigurationException("The configuration does not exist");
    }

    /**
     *
     *
     * @param name
     *
     * @throws ForwardingConfigurationException
     */
    public void removeLocalForwarding(String name) throws ForwardingConfigurationException {
        if (!localForwardings.containsKey(name)) {
            throw new ForwardingConfigurationException("The name is not a valid forwarding configuration");
        }
        ForwardingListener listener = (ForwardingListener) localForwardings.get(name);
        if (listener.isRunning()) {
            stopLocalForwarding(name);
        }
        localForwardings.remove(name);
    }

    /**
     *
     *
     * @param name
     *
     * @throws IOException
     * @throws ForwardingConfigurationException
     */
    public void removeRemoteForwarding(String name) throws IOException, ForwardingConfigurationException {
        if (!remoteForwardings.containsKey(name)) {
            throw new ForwardingConfigurationException("The name is not a valid forwarding configuration");
        }
        ForwardingListener listener = (ForwardingListener) remoteForwardings.get(name);
        if (listener.isRunning()) {
            stopRemoteForwarding(name);
        }
        remoteForwardings.remove(name);
    }

    /**
     *
     *
     * @param uniqueName
     * @param addressToBind
     * @param portToBind
     * @param hostToConnect
     * @param portToConnect
     *
     * @return
     *
     * @throws ForwardingConfigurationException
     */
    public ForwardingConfiguration addLocalForwarding(String uniqueName, String addressToBind, int portToBind, String hostToConnect, int portToConnect) throws ForwardingConfigurationException {
        if (localForwardings.containsKey(uniqueName)) {
            throw new ForwardingConfigurationException("The configuration name already exists!");
        }
        Iterator it = localForwardings.values().iterator();
        ForwardingConfiguration config;
        while (it.hasNext()) {
            config = (ForwardingConfiguration) it.next();
            if (config.getAddressToBind().equals(addressToBind) && (config.getPortToBind() == portToBind)) {
                throw new ForwardingConfigurationException("The address and port are already in use");
            }
        }
        SecurityManager manager = System.getSecurityManager();
        if (manager != null) {
            try {
                manager.checkPermission(new SocketPermission(addressToBind + ":" + String.valueOf(portToBind), "accept,listen"));
            } catch (SecurityException e) {
                throw new ForwardingConfigurationException("The security manager has denied listen permision on " + addressToBind + ":" + String.valueOf(portToBind));
            }
        }
        ForwardingConfiguration cf = new ClientForwardingListener(uniqueName, connection, addressToBind, portToBind, hostToConnect, portToConnect);
        localForwardings.put(uniqueName, cf);
        return cf;
    }

    /**
     *
     *
     * @param fwd
     *
     * @return
     *
     * @throws ForwardingConfigurationException
     */
    public ForwardingConfiguration addLocalForwarding(ForwardingConfiguration fwd) throws ForwardingConfigurationException {
        return addLocalForwarding(fwd.getName(), fwd.getAddressToBind(), fwd.getPortToBind(), fwd.getHostToConnect(), fwd.getPortToConnect());
    }

    /**
     *
     *
     * @param uniqueName
     * @param addressToBind
     * @param portToBind
     * @param hostToConnect
     * @param portToConnect
     *
     * @throws ForwardingConfigurationException
     */
    public void addRemoteForwarding(String uniqueName, String addressToBind, int portToBind, String hostToConnect, int portToConnect) throws ForwardingConfigurationException {
        if (remoteForwardings.containsKey(uniqueName)) {
            throw new ForwardingConfigurationException("The remote forwaring configuration name already exists!");
        }
        Iterator it = remoteForwardings.values().iterator();
        ForwardingConfiguration config;
        while (it.hasNext()) {
            config = (ForwardingConfiguration) it.next();
            if (config.getAddressToBind().equals(addressToBind) && (config.getPortToBind() == portToBind)) {
                throw new ForwardingConfigurationException("The remote forwarding address and port are already in use");
            }
        }
        SecurityManager manager = System.getSecurityManager();
        if (manager != null) {
            try {
                manager.checkPermission(new SocketPermission(hostToConnect + ":" + String.valueOf(portToConnect), "connect"));
            } catch (SecurityException e) {
                throw new ForwardingConfigurationException("The security manager has denied connect permision on " + hostToConnect + ":" + String.valueOf(portToConnect));
            }
        }
        remoteForwardings.put(uniqueName, new ForwardingConfiguration(uniqueName, addressToBind, portToBind, hostToConnect, portToConnect));
    }

    /**
     *
     *
     * @param fwd
     *
     * @throws ForwardingConfigurationException
     */
    public void addRemoteForwarding(ForwardingConfiguration fwd) throws ForwardingConfigurationException {
        if (remoteForwardings.containsKey(fwd.getName())) {
            throw new ForwardingConfigurationException("The remote forwaring configuration name already exists!");
        }
        Iterator it = remoteForwardings.values().iterator();
        ForwardingConfiguration config;
        while (it.hasNext()) {
            config = (ForwardingConfiguration) it.next();
            if (config.getAddressToBind().equals(fwd.getAddressToBind()) && (config.getPortToBind() == fwd.getPortToBind())) {
                throw new ForwardingConfigurationException("The remote forwarding address and port are already in use");
            }
        }
        SecurityManager manager = System.getSecurityManager();
        if (manager != null) {
            try {
                manager.checkPermission(new SocketPermission(fwd.getHostToConnect() + ":" + String.valueOf(fwd.getPortToConnect()), "connect"));
            } catch (SecurityException e) {
                throw new ForwardingConfigurationException("The security manager has denied connect permision on " + fwd.getHostToConnect() + ":" + String.valueOf(fwd.getPortToConnect()));
            }
        }
        remoteForwardings.put(fwd.getName(), fwd);
    }

    /**
     *
     *
     * @param channelType
     * @param requestData
     *
     * @return
     *
     * @throws InvalidChannelException
     */
    public Channel createChannel(String channelType, byte[] requestData) throws InvalidChannelException {
        if (channelType.equals(ForwardingSocketChannel.X11_FORWARDING_CHANNEL)) {
            if (xDisplay == null) {
                throw new InvalidChannelException("Local display has not been set for X11 forwarding.");
            }
            try {
                ByteArrayReader bar = new ByteArrayReader(requestData);
                String originatingHost = bar.readString();
                int originatingPort = (int) bar.readInt();
                log.debug("Creating socket to " + x11ForwardingConfiguration.getHostToConnect() + "/" + x11ForwardingConfiguration.getPortToConnect());
                Socket socket = new Socket(x11ForwardingConfiguration.getHostToConnect(), x11ForwardingConfiguration.getPortToConnect());
                ForwardingSocketChannel channel = x11ForwardingConfiguration.createForwardingSocketChannel(channelType, x11ForwardingConfiguration.getHostToConnect(), x11ForwardingConfiguration.getPortToConnect(), originatingHost, originatingPort);
                channel.bindSocket(socket);
                channel.addEventListener(x11ForwardingConfiguration.monitor);
                return channel;
            } catch (IOException ioe) {
                throw new InvalidChannelException(ioe.getMessage());
            }
        }
        if (channelType.equals(ForwardingSocketChannel.REMOTE_FORWARDING_CHANNEL)) {
            try {
                ByteArrayReader bar = new ByteArrayReader(requestData);
                String addressBound = bar.readString();
                int portBound = (int) bar.readInt();
                String originatingHost = bar.readString();
                int originatingPort = (int) bar.readInt();
                ForwardingConfiguration config = getRemoteForwardingByAddress(addressBound, portBound);
                Socket socket = new Socket(config.getHostToConnect(), config.getPortToConnect());
                ForwardingSocketChannel channel = config.createForwardingSocketChannel(channelType, config.getHostToConnect(), config.getPortToConnect(), originatingHost, originatingPort);
                channel.bindSocket(socket);
                channel.addEventListener(config.monitor);
                return channel;
            } catch (ForwardingConfigurationException fce) {
                throw new InvalidChannelException("No valid forwarding configuration was available for the request address");
            } catch (IOException ioe) {
                throw new InvalidChannelException(ioe.getMessage());
            }
        }
        throw new InvalidChannelException("The server can only request a remote forwarding channel or an" + "X11 forwarding channel");
    }

    /**
     *
     *
     * @param uniqueName
     *
     * @throws ForwardingConfigurationException
     */
    public void startLocalForwarding(String uniqueName) throws ForwardingConfigurationException {
        if (!localForwardings.containsKey(uniqueName)) {
            throw new ForwardingConfigurationException("The name is not a valid forwarding configuration");
        }
        try {
            ForwardingListener listener = (ForwardingListener) localForwardings.get(uniqueName);
            listener.start();
        } catch (IOException ex) {
            throw new ForwardingConfigurationException(ex.getMessage());
        }
    }

    /**
     *
     *
     * @throws IOException
     * @throws ForwardingConfigurationException
     */
    public void startX11Forwarding() throws IOException, ForwardingConfigurationException {
        if (x11ForwardingConfiguration == null) {
            throw new ForwardingConfigurationException("X11 forwarding hasn't been enabled.");
        }
        ByteArrayWriter baw = new ByteArrayWriter();
        baw.writeString(x11ForwardingConfiguration.getAddressToBind());
        baw.writeInt(x11ForwardingConfiguration.getPortToBind());
        x11ForwardingConfiguration.getState().setValue(StartStopState.STARTED);
        if (log.isDebugEnabled()) {
            log.info("X11 forwarding started");
            log.debug("Address to bind: " + x11ForwardingConfiguration.getAddressToBind());
            log.debug("Port to bind: " + String.valueOf(x11ForwardingConfiguration.getPortToBind()));
            log.debug("Host to connect: " + x11ForwardingConfiguration.hostToConnect);
            log.debug("Port to connect: " + x11ForwardingConfiguration.portToConnect);
        } else {
            log.info("Request for X11 rejected.");
        }
    }

    /**
     *
     *
     * @param name
     *
     * @throws IOException
     * @throws ForwardingConfigurationException
     */
    public void startRemoteForwarding(String name) throws IOException, ForwardingConfigurationException {
        if (!remoteForwardings.containsKey(name)) {
            throw new ForwardingConfigurationException("The name is not a valid forwarding configuration");
        }
        ForwardingConfiguration config = (ForwardingConfiguration) remoteForwardings.get(name);
        ByteArrayWriter baw = new ByteArrayWriter();
        baw.writeString(config.getAddressToBind());
        baw.writeInt(config.getPortToBind());
        connection.sendGlobalRequest(REMOTE_FORWARD_REQUEST, true, baw.toByteArray());
        remoteForwardings.put(name, config);
        config.getState().setValue(StartStopState.STARTED);
        log.info("Remote forwarding configuration '" + name + "' started");
        if (log.isDebugEnabled()) {
            log.debug("Address to bind: " + config.getAddressToBind());
            log.debug("Port to bind: " + String.valueOf(config.getPortToBind()));
            log.debug("Host to connect: " + config.hostToConnect);
            log.debug("Port to connect: " + config.portToConnect);
        }
    }

    /**
     *
     *
     * @param uniqueName
     *
     * @throws ForwardingConfigurationException
     */
    public void stopLocalForwarding(String uniqueName) throws ForwardingConfigurationException {
        if (!localForwardings.containsKey(uniqueName)) {
            throw new ForwardingConfigurationException("The name is not a valid forwarding configuration");
        }
        ForwardingListener listener = (ForwardingListener) localForwardings.get(uniqueName);
        listener.stop();
        log.info("Local forwarding configuration " + uniqueName + "' stopped");
    }

    /**
     *
     *
     * @param name
     *
     * @throws IOException
     * @throws ForwardingConfigurationException
     */
    public void stopRemoteForwarding(String name) throws IOException, ForwardingConfigurationException {
        if (!remoteForwardings.containsKey(name)) {
            throw new ForwardingConfigurationException("The remote forwarding configuration does not exist");
        }
        ForwardingConfiguration config = (ForwardingConfiguration) remoteForwardings.get(name);
        ByteArrayWriter baw = new ByteArrayWriter();
        baw.writeString(config.getAddressToBind());
        baw.writeInt(config.getPortToBind());
        connection.sendGlobalRequest(REMOTE_FORWARD_CANCEL_REQUEST, true, baw.toByteArray());
        config.getState().setValue(StartStopState.STOPPED);
        log.info("Remote forwarding configuration '" + name + "' stopped");
    }

    public class ClientForwardingListener extends ForwardingListener {

        public ClientForwardingListener(String name, ConnectionProtocol connection, String addressToBind, int portToBind, String hostToConnect, int portToConnect) {
            super(name, connection, addressToBind, portToBind, hostToConnect, portToConnect);
        }

        public ForwardingSocketChannel createChannel(String hostToConnect, int portToConnect, Socket socket) throws ForwardingConfigurationException {
            return createForwardingSocketChannel(ForwardingSocketChannel.LOCAL_FORWARDING_CHANNEL, hostToConnect, portToConnect, socket.getInetAddress().getHostAddress(), socket.getPort());
        }
    }
}

package de.iritgo.openmetix.core.network;

import de.iritgo.openmetix.core.action.Action;
import de.iritgo.openmetix.core.iobject.NoSuchIObjectException;
import de.iritgo.openmetix.core.logger.Log;
import de.iritgo.openmetix.core.thread.Threadable;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Channel.
 *
 * @version $Id: Channel.java,v 1.1 2005/04/24 18:10:45 grappendorf Exp $
 */
public class Channel extends Threadable {

    /** Error type. */
    public static int NETWORK_OK = 0;

    /** Error type. */
    public static int NETWORK_ERROR = 1;

    /** Error type. */
    public static int NETWORK_CLOSE = 2;

    /** Error type. */
    public static int NETWORK_ERROR_CLOSING = 3;

    /** Our socket. */
    private Socket socket = null;

    /** Our network service. */
    private NetworkService networkService = null;

    /** Our stream organizer. */
    private StreamOrganizer streamOrganizer;

    /** Our channel number. */
    private double channelNumber;

    /** The connection state. */
    private int connectionState;

    /** Total number of received objects. */
    private int numReceivedObjects;

    /** Total number of sent objects. */
    private int numSentObjects;

    /**
	 * Save your custom context object for a relation.
	 */
    private Object customContextObject;

    /**
	 * In case of a connection timeout we send an action to the other end point.
	 * After sending the action we set this flag to true. After another timeout,
	 * we can identify this situation by checking this flag.
	 */
    private boolean aliveCheckSent;

    /**
	 * Create a new Channel.
	 *
	 * @param socket The Socket for input/output action.
	 * @param networkService Networkbase for storge received objects.
	 */
    public Channel(Socket socket, NetworkService networkService) throws IOException {
        super("Channel:" + (networkService.getNumChannels()));
        init(socket, networkService, networkService.getNumChannels());
    }

    /**
	 * Create a new Channel.
	 *
	 * @param socket The Socket for input/output action.
	 * @param networkService Networkbase for storge received objects.
	 * @param channelNumber The channel number.
	 */
    public Channel(Socket socket, NetworkService networkService, double channelNumber) throws IOException {
        super("channel" + channelNumber);
        init(socket, networkService, channelNumber);
    }

    /**
	 * Initialize the Channel.
	 *
	 * @param The Socket for input/output action.
	 * @param Networkbase for storge received objects.
	 * @param The channel number.
	 */
    private void init(Socket socket, NetworkService networkService, double channelNumber) throws IOException {
        this.socket = socket;
        this.networkService = networkService;
        this.channelNumber = channelNumber;
        numReceivedObjects = 0;
        numSentObjects = 0;
        connectionState = NETWORK_OK;
        streamOrganizer = networkService.getDefaultStreamOrganizer(socket);
    }

    /**
	 * Set the custom context.
	 *
	 * @param custom context.
	 */
    public void setCustomContextObject(Object customContextObject) {
        this.customContextObject = customContextObject;
    }

    /**
	 * Set the custom context.
	 *
	 * @return object The custom object.
	 */
    public Object getCustomContextObject() {
        return customContextObject;
    }

    /**
	 * Set the channel number.
	 *
	 * @param channelNumber The new channel number.
	 */
    public void setChannelNumber(double channelNumber) {
        this.channelNumber = channelNumber;
    }

    /**
	 * Get the channel number.
	 *
	 * @return The channel number.
	 */
    public double getChannelNumber() {
        return channelNumber;
    }

    /**
	 * @see de.iritgo.openmetix.core.thread.Threadable#run()
	 */
    public void run() {
        Object object = null;
        setState(Threadable.RUNNING);
        try {
            if (connectionState != NETWORK_OK) {
                Log.logWarn("network", "Channel.run", "A network error occurred. Closing connection");
                setState(Threadable.CLOSING);
                return;
            }
            object = streamOrganizer.receive();
            if (object == null) {
                Log.logError("network", "Channel.run", "Receiving NULL-Object" + channelNumber + ").");
                setConnectionState(NETWORK_ERROR);
                networkService.fireError(this, new NoSuchIObjectException("null"));
                networkService.fireConnectionTerminated(this);
                return;
            }
            Log.logDebug("network", "Channel.run", "Receiving Object (Channel:" + channelNumber + ").");
            ++numReceivedObjects;
            networkService.callReceiveNetworkActionProcessor((Action) object, this);
        } catch (SocketTimeoutException x) {
            Log.logDebug("network", "Channel.run", "SocketTimeoutException");
            networkService.fireError(this, x);
            return;
        } catch (NoSuchIObjectException x) {
            Log.logError("network", "Channel.run", "NoSuchPrototypeRegisteredException: " + x);
            object = null;
            setConnectionState(NETWORK_ERROR);
            networkService.fireError(this, x);
            networkService.fireConnectionTerminated(this);
            return;
        } catch (ClassNotFoundException x) {
            Log.logError("network", "Channel.run", "ClassNotFoundException");
            setConnectionState(NETWORK_ERROR);
            networkService.fireError(this, x);
            networkService.fireConnectionTerminated(this);
            return;
        } catch (EOFException x) {
            Log.logDebug("network", "Channel.run", "EOFException (ConnectionClosed?!)");
            setConnectionState(NETWORK_CLOSE);
            setState(Threadable.CLOSING);
            networkService.fireError(this, x);
            networkService.fireConnectionTerminated(this);
            return;
        } catch (SocketException x) {
            Log.logDebug("network", "Channel.run", "SocketClosed.");
            setConnectionState(NETWORK_CLOSE);
            setState(Threadable.CLOSING);
            networkService.fireError(this, x);
            networkService.fireConnectionTerminated(this);
            return;
        } catch (IOException x) {
            setState(Threadable.CLOSING);
            Log.logError("network", "Channel.run", "IOException!");
            setConnectionState(NETWORK_ERROR);
            networkService.fireError(this, x);
            networkService.fireConnectionTerminated(this);
            return;
        }
    }

    /**
	 * Send an object over the ObjectStream.
	 *
	 * @param object The Object to send.
	 */
    public void send(Object object) {
        try {
            Log.logDebug("network", "Channel.send", "Sending Object (Channel:" + channelNumber + "):" + object);
            ++numSentObjects;
            streamOrganizer.send(object);
        } catch (IOException e) {
            Log.logError("network", "Channel.send", "Serializeable?" + e);
        }
    }

    /**
	 * Get the connection state.
	 *
	 * @return the connection state.
	 */
    public int getConnectionState() {
        return connectionState;
    }

    /**
	 * Set connection state.
	 *
	 * @param connectionState The new connection state.
	 */
    public void setConnectionState(int connectionState) {
        this.connectionState = connectionState;
    }

    /**
	 * @see de.iritgo.openmetix.core.thread.Threadable#dispose()
	 */
    public void dispose() {
        setState(Threadable.CLOSING);
        connectionState = NETWORK_CLOSE;
        try {
            streamOrganizer.flush();
            streamOrganizer.close();
        } catch (IOException x) {
            Log.logError("network", "Channel.dispose", x.toString());
            setConnectionState(NETWORK_ERROR_CLOSING);
        }
    }

    /**
	 * Get the network service.
	 *
	 * @return The network service.
	 */
    public NetworkService getNetworkBase() {
        return networkService;
    }

    /**
	 * Get the number of received objects.
	 *
	 * @return Number of received objects.
	 */
    public int getNumReceivedObjects() {
        return numReceivedObjects;
    }

    /**
	 * Get the number of sent objects.
	 *
	 * @return Number of sent objects.
	 */
    public int getNumSentObjects() {
        return numSentObjects;
    }

    /**
	 * Get the number of all received/sent objects
	 *
	 * @return Number of all object.
	 */
    public int getNumAllObjects() {
        return numReceivedObjects + numSentObjects;
    }

    /**
	 * Flush the streams.
	 */
    public void flush() {
        try {
            streamOrganizer.flush();
        } catch (IOException x) {
            Log.logError("network", "Channel.dispose", x.toString());
        }
    }

    /**
	 * Set a new stream organizer for this connected channel
	 *
	 * @param streamOrganizer The stream organizer.
	 */
    public void setStreamOrganizer(StreamOrganizer streamOrganizer) throws IOException {
        if (streamOrganizer != null) {
            streamOrganizer.flush();
        }
        this.streamOrganizer = streamOrganizer.create(socket);
    }

    /**
	 * Get the current alive check flag.
	 *
	 * @return The current flag.
	 */
    public boolean isAliveCheckSent() {
        return aliveCheckSent;
    }

    /**
	 * Set the alive check flag.
	 *
	 * @param aliveCheckSend The new flag.
	 */
    public void setAliveCheckSent(boolean aliveCheckSent) {
        this.aliveCheckSent = aliveCheckSent;
    }
}

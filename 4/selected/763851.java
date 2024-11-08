package de.iritgo.openmetix.framework.client.network;

import de.iritgo.openmetix.core.logger.Log;
import de.iritgo.openmetix.core.network.Channel;
import de.iritgo.openmetix.core.network.NetworkService;
import de.iritgo.openmetix.core.network.NetworkSystemAdapter;
import de.iritgo.openmetix.framework.base.action.AliveCheckServerAction;
import de.iritgo.openmetix.framework.client.Client;
import java.net.SocketTimeoutException;

/**
 * NetworkSystemListener.
 *
 * @version $Id: NetworkSystemListenerImpl.java,v 1.1 2005/04/24 18:10:43 grappendorf Exp $
 */
public class NetworkSystemListenerImpl extends NetworkSystemAdapter {

    public void error(NetworkService networkBase, Channel connectedChannel, SocketTimeoutException x) {
        if (connectedChannel.isAliveCheckSent()) {
            connectionTerminated(networkBase, connectedChannel);
            return;
        }
        connectedChannel.setAliveCheckSent(true);
        connectedChannel.send(new AliveCheckServerAction(AliveCheckServerAction.CLIENT));
        connectedChannel.flush();
    }

    public void connectionTerminated(NetworkService networkBase, Channel connectedChannel) {
        if (connectedChannel.getConnectionState() == (Channel.NETWORK_ERROR_CLOSING)) {
            Log.logError("network", "NetworkSystemListenerImpl.work", "Unable to close connection: " + connectedChannel.getConnectionState());
        }
        double channelNumber = connectedChannel.getChannelNumber();
        if (connectedChannel.getConnectionState() != Channel.NETWORK_ERROR_CLOSING) {
            networkBase.closeChannel(connectedChannel.getChannelNumber());
        }
        Client.instance().lostNetworkConnection();
        Log.log("network", "NetworkSystemListenerImpl.work", "Lost connection to server: " + connectedChannel.getConnectionState(), Log.INFO);
        return;
    }
}

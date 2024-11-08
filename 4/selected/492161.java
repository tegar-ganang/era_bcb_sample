package de.iritgo.openmetix.framework.server.network;

import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.logger.Log;
import de.iritgo.openmetix.core.network.Channel;
import de.iritgo.openmetix.core.network.NetworkService;
import de.iritgo.openmetix.core.network.NetworkSystemAdapter;
import de.iritgo.openmetix.framework.base.action.AliveCheckAction;
import de.iritgo.openmetix.framework.base.shutdown.ShutdownManager;
import de.iritgo.openmetix.framework.server.Server;
import de.iritgo.openmetix.framework.user.User;
import de.iritgo.openmetix.framework.user.UserRegistry;
import java.net.SocketTimeoutException;
import java.util.Date;

/**
 * NetworkSystemListener.
 *
 * @version $Id: NetworkSystemListenerImpl.java,v 1.1 2005/04/24 18:10:48 grappendorf Exp $
 */
public class NetworkSystemListenerImpl extends NetworkSystemAdapter {

    public void error(NetworkService networkBase, Channel connectedChannel, SocketTimeoutException x) {
        if (connectedChannel.isAliveCheckSent()) {
            connectionTerminated(networkBase, connectedChannel);
            return;
        }
        connectedChannel.setAliveCheckSent(true);
        connectedChannel.send(new AliveCheckAction(AliveCheckAction.SERVER));
        connectedChannel.flush();
    }

    public void connectionTerminated(NetworkService networkBase, Channel connectedChannel) {
        if (connectedChannel.getConnectionState() == Channel.NETWORK_ERROR_CLOSING) {
            Log.logError("network", "NetworkSystemListenerImpl", "Unable to close connection for channel " + connectedChannel.getChannelNumber() + " (connection state: " + connectedChannel.getConnectionState() + ")");
        }
        Server server = Server.instance();
        UserRegistry userRegistry = server.getUserRegistry();
        User user = null;
        user = (User) connectedChannel.getCustomContextObject();
        if (user == null) {
            user = new User();
            user.setNetworkChannel(connectedChannel.getChannelNumber());
            user.setName("");
        }
        Log.log("network", "NetworkSystemListenerImpl", "Lost connection to user '" + user + "' (channel number: " + connectedChannel.getChannelNumber() + ")", Log.INFO);
        user.setOnline(false);
        user.setLoggedOutDate(new Date());
        ((ShutdownManager) Engine.instance().getManagerRegistry().getManager("shutdown")).shutdown(user);
        try {
            if (connectedChannel.getConnectionState() != Channel.NETWORK_ERROR_CLOSING) {
                networkBase.closeChannel(connectedChannel.getChannelNumber());
            }
        } catch (Exception x) {
        }
        Log.log("network", "NetworkSystemListenerImpl", "Cleaned up connection to user '" + user + "' (channel number: " + connectedChannel.getChannelNumber() + ")", Log.INFO);
    }
}

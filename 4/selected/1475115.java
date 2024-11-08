package org.timothyb89.jtelirc.karma;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import org.timothyb89.jtelirc.channel.Channel;
import org.timothyb89.jtelirc.server.Server;
import org.timothyb89.jtelirc.server.ServerListener;

/**
 *
 * @author tim
 */
public class KarmaManager implements ServerListener {

    /**
	 * The list of KarmaListeners
	 */
    private List<KarmaListener> listeners;

    /**
	 * Creates a new KarmaManager
	 */
    public KarmaManager() {
        listeners = new ArrayList<KarmaListener>();
    }

    /**
	 * Gets the KarmaListener for the given channel.
	 * @param c The channel to search for
	 * @return The KarmaListener for the given channel, or null if there isn't one
	 */
    public KarmaListener getListener(Channel c) {
        for (KarmaListener l : listeners) {
            if (l.getChannel().equals(c)) {
                return l;
            }
        }
        return null;
    }

    /**
	 * Called when disconnected from the server to clear the list of message
	 * listeners.
	 * @param server The server that the client was disconnected from.
	 */
    public void onDisconnected(Server server) {
        List<KarmaListener> copy = new ArrayList<KarmaListener>();
        copy.addAll(listeners);
        for (KarmaListener l : copy) {
            l.getChannel().removeListener(l);
            listeners.remove(l);
        }
    }

    /**
	 * Called when a channel has been joined to add a new KarmaListner
	 * @param c The channel joined
	 */
    public void onChannelJoined(Channel c) {
        listeners.add(new KarmaListener(c));
    }

    /**
	 * Called when the client has left a channel to remove the KarmaListener
	 * @param c The channel left
	 * @param msg The part message (if any)
	 */
    public void onChannelParted(Channel c, String msg) {
        List<KarmaListener> copy = new ArrayList<KarmaListener>();
        copy.addAll(listeners);
        for (KarmaListener l : copy) {
            if (l.getChannel() == c) {
                l.getChannel().removeListener(l);
                listeners.remove(l);
            }
        }
    }

    /**
	 * Called when connected to a server. Currently not used.
	 * @param server The server connected to.
	 */
    public void onConnected(Server server) {
    }
}

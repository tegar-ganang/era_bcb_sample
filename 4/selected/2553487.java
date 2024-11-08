package org.objectstyle.cayenne.remote.service;

import java.io.Serializable;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.remote.RemoteSession;

/**
 * An object that stores server side objects for the client session.
 * 
 * @author Andrus Adamchik
 * @since 1.2
 */
public class ServerSession implements Serializable {

    protected RemoteSession session;

    protected DataChannel channel;

    public ServerSession(RemoteSession session, DataChannel channel) {
        this.session = session;
        this.channel = channel;
    }

    public DataChannel getChannel() {
        return channel;
    }

    public RemoteSession getSession() {
        return session;
    }
}

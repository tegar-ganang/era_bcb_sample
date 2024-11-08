package ca.qc.adinfo.rouge.server.core;

import java.util.Collection;
import java.util.HashMap;
import org.jboss.netty.channel.Channel;

public class SessionManager {

    private HashMap<Integer, SessionContext> sessions;

    public SessionManager() {
        this.sessions = new HashMap<Integer, SessionContext>();
    }

    public void registerSession(SessionContext session) {
        synchronized (this.sessions) {
            this.sessions.put(session.getChannel().getId(), session);
        }
    }

    public void unregisterSession(SessionContext session) {
        synchronized (this.sessions) {
            this.sessions.remove(session.getChannel().getId());
        }
    }

    public SessionContext getSession(Channel channel) {
        synchronized (this.sessions) {
            return this.sessions.get(channel.getId());
        }
    }

    public SessionContext getSession(int id) {
        synchronized (this.sessions) {
            return this.sessions.get(id);
        }
    }

    public Collection<SessionContext> getSessions() {
        synchronized (this.sessions) {
            return this.sessions.values();
        }
    }

    public int getNumberSession() {
        return this.sessions.size();
    }
}

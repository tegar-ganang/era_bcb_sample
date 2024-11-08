package jerklib;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by IntelliJ IDEA.
 * User: jottinger
 * Date: Mar 11, 2008
 * Time: 5:41:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestConnectionManager extends jerklib.ConnectionManager {

    void connect(final Session session) throws IOException {
        SocketChannel sChannel = SocketChannel.open();
        Connection con = new Connection(this, sChannel, session) {

            jerklib.Channel getChannel(java.lang.String A) {
                return new Channel(A, session);
            }
        };
        session.setConnection(con);
        socChanMap.put(sChannel, session);
    }

    @Override
    Session getSessionFor(Connection con) {
        System.out.println("Session count: " + sessionMap.values().size());
        System.out.flush();
        for (Session session : sessionMap.values()) {
            System.out.println(session.getConnection());
            System.out.flush();
            return session;
        }
        return null;
    }

    @Override
    void makeConnections() {
        for (Session s : sessionMap.values()) {
            if (s.getState().equals(Session.State.DISCONNECTED)) {
                try {
                    connect(s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Only constructor - Takes a profile to use as
     * default profile for new Connections
     *
     * @param defaultProfile default user profile
     * @see jerklib.Profile
     * @see jerklib.ProfileImpl
     */
    public TestConnectionManager(Profile defaultProfile) {
        super(defaultProfile);
        IRCEventFactory.setManager(this);
    }
}

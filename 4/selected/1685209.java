package org.jnerve;

import java.net.*;
import java.io.*;
import java.util.*;
import org.jnerve.message.*;
import org.jnerve.message.handler.*;
import org.jnerve.persistence.*;
import org.jnerve.session.*;
import org.jnerve.session.event.*;
import org.jnerve.util.*;

/** Server class serves as entry point for napster server.
  * Network handling and objects of application-wide scope go here, 
  * to be passed to other objects if necessary.
  */
public class Server extends Thread implements ServerFacilities, SessionEventListener, StatisticsMaintainer {

    private int port;

    private int maxConnections;

    private int sessionsStorageInitialSize = 1000;

    private int sessionsStorageGrowthSpurt = 1000;

    private MessageHandlerFactory messageHandlerFactory = null;

    private MessageFactory messageFactory = null;

    private Vector activeSessions = null;

    private UserPersistenceStore userPersistenceStore = null;

    private JNerveConfiguration config = null;

    private int fileCount = 0;

    private int userCount = 0;

    private int sizeInMegs = 0;

    private static final int megsPerGig = 1000;

    private Hotlist hotlist = null;

    private ChannelManager channelManager = null;

    private Banlist banlist = null;

    public Server(JNerveConfiguration config) {
        this.config = config;
        port = config.getServerPort();
        maxConnections = config.getMaxConnections();
        this.hotlist = new Hotlist(this);
        this.channelManager = new ChannelManager(this);
        this.banlist = new Banlist();
        messageHandlerFactory = new MessageHandlerFactory();
        if (config.isPolicyOpenDoor()) {
            Logger.getInstance().log(Logger.INFO, "OpenDoor policy is in effect.");
            messageHandlerFactory.addHandlerForType(MessageTypes.CLIENT_LOGIN_REQUEST, new OpenDoorLoginHandler());
        }
        messageFactory = new MessageFactory();
        activeSessions = new Vector(sessionsStorageInitialSize, sessionsStorageGrowthSpurt);
        try {
            userPersistenceStore = (UserPersistenceStore) Class.forName(config.getUserPersistenceStoreClassname()).newInstance();
            userPersistenceStore.init(config.getProperties());
        } catch (Exception e) {
            Logger.getInstance().log(Logger.SEVERE, "Could not instantiate user persistence store: " + e.toString());
        }
    }

    public void run() {
        Logger logger = Logger.getInstance();
        logger.log(Logger.INFO, "Starting napster server (port=" + port + ",maxConnections=" + maxConnections);
        int sessionTimeout = getJNerveConfiguration().getSessionTimeout() * 1000;
        try {
            ServerSocket socket = new ServerSocket(port, maxConnections);
            while (true) {
                Socket estSocket = socket.accept();
                estSocket.setTcpNoDelay(true);
                estSocket.setSoTimeout(sessionTimeout);
                Session s = new Session(estSocket, this);
                s.addSessionEventListener(getHotlist());
                s.addSessionEventListener(getChannelManager());
                s.addSessionEventListener(this);
                s.start();
            }
        } catch (IOException ioe) {
            logger.log(Logger.SEVERE, ioe.toString());
        }
    }

    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public MessageHandlerFactory getMessageHandlerFactory() {
        return messageHandlerFactory;
    }

    public StatisticsMaintainer getStatisticsMaintainer() {
        return this;
    }

    public JNerveConfiguration getJNerveConfiguration() {
        return config;
    }

    public UserPersistenceStore getUserPersistenceStore() {
        return userPersistenceStore;
    }

    public void processEvent(SessionEvent se) {
        switch(se.getType()) {
            case SessionEvent.REGISTER:
                {
                    addActiveSession(se.getSession());
                    break;
                }
            case SessionEvent.TERMINATE:
                {
                    removeActiveSession(se.getSession());
                    break;
                }
        }
    }

    public Vector searchForShares(SearchParameters params) {
        Vector results = new Vector(params.getMaxResults());
        Enumeration e = activeSessions.elements();
        while (e.hasMoreElements() && results.size() <= params.getMaxResults()) {
            Session s = (Session) e.nextElement();
            s.search(params, results);
        }
        return results;
    }

    public Vector searchForShares(ResumeParameters params) {
        Vector results = new Vector(params.getMaxResults());
        Enumeration e = activeSessions.elements();
        while (e.hasMoreElements() && results.size() <= params.getMaxResults()) {
            Session s = (Session) e.nextElement();
            s.search(params, results);
        }
        return results;
    }

    public void addActiveSession(Session s) {
        Logger.getInstance().log(Logger.DEBUG, "adding session to active list");
        activeSessions.addElement(s);
    }

    public void removeActiveSession(Session s) {
        Logger.getInstance().log(Logger.DEBUG, "removing session from active list");
        activeSessions.removeElement(s);
    }

    public UserState searchForUserState(String nickname) {
        Session s = searchForSession(nickname);
        if (s != null) {
            return s.getUserState();
        }
        return null;
    }

    public Session searchForSession(String nickname) {
        for (int x = 0; x < activeSessions.size(); x++) {
            Session s = (Session) activeSessions.elementAt(x);
            UserState us = s.getUserState();
            if (nickname.equals(us.getNickname())) {
                return s;
            }
        }
        return null;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void incFileCount(int x) {
        fileCount += x;
    }

    public void decFileCount(int x) {
        fileCount -= x;
    }

    public int getUserCount() {
        return userCount;
    }

    public void incUserCount(int x) {
        userCount += x;
    }

    public void decUserCount(int x) {
        userCount -= x;
    }

    public int getTotalLibrarySizeInGigs() {
        return sizeInMegs / megsPerGig;
    }

    public void incTotalLibrarySize(int megs) {
        sizeInMegs += megs;
    }

    public void decTotalLibrarySize(int megs) {
        sizeInMegs -= megs;
    }

    public Hotlist getHotlist() {
        return hotlist;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public Banlist getBanlist() {
        return banlist;
    }

    public Enumeration getSessions() {
        return activeSessions.elements();
    }
}

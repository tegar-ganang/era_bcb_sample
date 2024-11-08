package de.iritgo.openmetix.framework.appcontext;

import de.iritgo.openmetix.core.base.BaseObject;
import de.iritgo.openmetix.core.iobject.IObject;
import de.iritgo.openmetix.framework.user.User;
import java.util.Locale;

/**
 * @version $Id: AppContext.java,v 1.1 2005/04/24 18:10:44 grappendorf Exp $
 */
public class AppContext extends BaseObject {

    private static AppContext appContext;

    /** I'm a server? */
    private boolean server;

    /** I'm a client? */
    private boolean client;

    /** The server IP/URL. */
    private String serverIP;

    /** The user */
    private User user;

    /** This objects syncs the CommandProcessor and the ActionProcessor. */
    private Object lockObject;

    /** Is the client connected with the Server? */
    private boolean connectedWithServer;

    private double channelNumber;

    private IObject applicationObject;

    /** The current language. */
    private Locale locale;

    /**
	 * Standard constructor
	 */
    public AppContext() {
        super("appcontext");
        lockObject = new Object();
        user = null;
        connectedWithServer = false;
        locale = new Locale("de");
    }

    /**
	 * Set the channelnumber.
	 *
	 * @param channelNumber The channelNumber.
	 */
    public void setChannelNumber(double channelNumber) {
        this.channelNumber = channelNumber;
    }

    /**
	 * Get the channelnumber.
	 *
	 * @return channelnumber.
	 */
    public double getChannelNumber() {
        return channelNumber;
    }

    /**
	 * Set the State of the Connection.
	 */
    public void setConnectionState(boolean connectedWithServer) {
        this.connectedWithServer = connectedWithServer;
    }

    /**
	 * Get the State of the Connection.
	 *
	 * @return state.
	 */
    public boolean isConnectedWithServer() {
        return connectedWithServer;
    }

    /**
	 * Is the User logged in?
	 *
	 * @return loggedIn.
	 */
    public boolean isUserLoggedIn() {
        return user.isOnline();
    }

    /**
	 * Set the ServerIP.
	 *
	 * @param serverIP The current serverIP.
	 */
    public void setServerIP(String serverIP) {
        this.serverIP = serverIP;
    }

    /**
	 * Get the ServerIP.
	 *
	 * @return The current serverIP.
	 */
    public String getServerIP() {
        return serverIP;
    }

    /**
	 * Set the User.
	 *
	 * @param user The current user.
	 */
    public void setUser(User user) {
        this.user = user;
    }

    /**
	 * Get the User.
	 *
	 * @return The current user.
	 */
    public User getUser() {
        return user;
    }

    /**
	 * Set the Server.
	 *
	 * @param server The current server.
	 */
    public void setServer(boolean server) {
        this.server = server;
    }

    /**
	 * Get the Server.
	 *
	 * @return The current server.
	 */
    public boolean getServer() {
        return server;
    }

    /**
	 * Set the Client.
	 *
	 * @param client The current client.
	 */
    public void setClient(boolean client) {
        this.client = client;
    }

    /**
	 * Get the Client.
	 *
	 * @return The current client.
	 */
    public boolean getClient() {
        return client;
    }

    /**
	 * Get the ApplicationDataObject.
	 *
	 * @param applicationObject The new ApplicationDataObject.
	 */
    public void setApplicationObject(IObject applicationObject) {
        this.applicationObject = applicationObject;
    }

    /**
	 * Set the ApplicationDataObject.
	 */
    public IObject getAppObject() {
        return applicationObject;
    }

    public synchronized Object getLockObject() {
        return lockObject;
    }

    public static AppContext instance() {
        if (appContext != null) {
            return appContext;
        }
        appContext = new AppContext();
        return appContext;
    }

    /**
	 * Get the current language.
	 *
	 * @return The current language.
	 */
    public Locale getLocale() {
        return locale;
    }

    /**
	 * Set the current language.
	 *
	 * @param locale The new language.
	 */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }
}

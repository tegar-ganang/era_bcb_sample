package whisper;

import gui.Resources;
import gui.channels.Channel;
import gui.channels.ChannelGroup;
import gui.channels.ChannelPrivate;
import gui.channels.ChannelPublic;
import gui.components.SecondLifeImage;
import gui.windows.AvatarSearchDialog;
import gui.windows.GroupNotice;
import gui.windows.GroupSearchDialog;
import gui.windows.LoginWindow;
import gui.windows.MainWindow;
import gui.windows.popupdialogs.PopupDialog;
import gui.windows.popupdialogs.PopupFriendshipRequest;
import gui.windows.popupdialogs.PopupGroupInvitation;
import gui.windows.popupdialogs.PopupTeleportOffer;
import gui.windows.popupdialogs.TosUpdateDialog;
import gui.windows.popupdialogs.PopupDialog.Severity;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import models.requests.RequestGroupChatJoin;
import models.requests.RequestGroupChatLeave;
import models.requests.RequestOfflineMessages;
import models.secondlife.Agent;
import models.secondlife.AvatarProfile;
import models.secondlife.Group;
import models.secondlife.Message;
import connection.WhisperTransportConnection;

/**
 * Whisper is a client for the WhisperTransport proxy. It aims to provide a feature-rich yet
 * lightweight client for Second Life allowing for communicating with other agents. While the primary
 * goal of the client is to implement communication, other more advanced features are also supported.
 * 
 * @author Thomas Pedley.
 */
public class Whisper implements Observable {

    /** The client name. */
    public static final String NAME = "Whisper";

    /** The version string. */
    public static final String VERSION = "v1.3";

    /** Flag to indicate whether debugging is enabled. */
    private static final boolean DEBUG = false;

    /** Identitifer for the system tab. */
    public static final String TAB_RAW = "Raw";

    /** Identitifer for the public tab. */
    public static final String TAB_PUBLIC = "Public";

    /** A static reference to one's self. */
    private static Whisper self;

    /** The connection to the WhisperTransport proxy. */
    private WhisperTransportConnection connection;

    /** Flag to indicate the login status. */
    private boolean loggedIn = false;

    /** The main window for the application. */
    private MainWindow mainWindow;

    /** The login window for the application. */
    private LoginWindow loginWindow;

    /** A collection of friends known to the currently logged in agent. */
    private HashMap<UUID, Agent> friends = new HashMap<UUID, Agent>();

    /** A collection of groups that the currently logged in agent is a member of. */
    private HashMap<UUID, Group> groups = new HashMap<UUID, Group>();

    /** A collection of nearby agents. */
    private HashMap<UUID, Agent> nearbyAvatars = new HashMap<UUID, Agent>();

    /** A collection of observers observing this object. */
    private ArrayList<Observer> observers = new ArrayList<Observer>();

    /** A flag to indicate whether login has been aborted. */
    private boolean abortLogin = false;

    /** A flag to indicate whether a logout was requested. */
    private boolean requestedLogout = false;

    /** The currently connected avatar's name. */
    private String name;

    /** Whether the avatar tracking state can be set. */
    private boolean avatarTrackingCanSetState = true;

    /** The current avatar tracking state. */
    private boolean avatarTrackingState = true;

    /** Whether the image state can be set. */
    private boolean imageCanSetState = true;

    /** The current image state (true means that images are enabled). */
    private boolean imageState = true;

    /** The current Sim name. */
    private String simName;

    /** The current parcel name. */
    private String parcelName;

    /** The current location image UUID. */
    private UUID locationImageUUID;

    /** A collection of images. */
    private HashMap<UUID, SecondLifeImage> images = new HashMap<UUID, SecondLifeImage>();

    /** Flag to indicate whether all groups have been received. */
    private boolean receivedAllGroups = false;

    /** Flag to indicate whether all friends have been received. */
    private boolean receivedAllFriends = false;

    /** Flag to indicate whether we have attempted offline message retrieval. */
    private boolean attemptedOfflineMessageRetrieval = false;

    /** Flag to indicate whether the tray is flashing. */
    private boolean trayFlashing = false;

    /** Lock for the system tray. */
    private Integer trayLock = new Integer(0);

    /** The focus state of the application. */
    private boolean focusState = true;

    /** A collection of pending profiles to display. */
    private HashMap<UUID, gui.windows.AvatarProfile> pendingProfiles = new HashMap<UUID, gui.windows.AvatarProfile>();

    /**
	 * Program entry point.
	 * 
	 * @param args Commandline arguments.
	 */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        Resources.ICON_BUTTON_EXIT.getDescription();
        new Whisper();
    }

    /**
	 * Constructor.
	 */
    public Whisper() {
        self = this;
        connection = new WhisperTransportConnection(new ResponseParser(this));
        Runtime.getRuntime().addShutdownHook(new Thread() {

            /**
			 * Called when the shutdown hook is run.
			 */
            @Override
            public void run() {
                if (Config.getConfig().saveSettings) {
                    Config.getConfig().save();
                } else {
                    Config.getConfig().delete();
                }
            }
        });
        setTrayIcon(Resources.IMAGE_WINDOW_ICON, true);
        showLoginWindow();
    }

    /**
	 * Show the login window.
	 */
    public void showLoginWindow() {
        if (mainWindow != null && mainWindow.isVisible()) {
            mainWindow.setVisible(false);
        }
        loginWindow = new LoginWindow();
        loginWindow.setVisible(true);
    }

    /**
	 * Initialise
	 */
    public void initialise() {
        clearFriends();
        clearGroups();
        if (mainWindow != null) {
            mainWindow.dispose();
        }
        mainWindow = new MainWindow();
        if (isDebugging()) mainWindow.addChannel(new ChannelPrivate(TAB_RAW, mainWindow, false), true);
        mainWindow.addChannel(new ChannelPublic(TAB_PUBLIC, mainWindow, false), false);
    }

    /**
	 * Get an existing channel.
	 * 
	 * @param name The name of the channel.
	 * @return The channel, null if not found.
	 */
    public Channel getChannel(String name) {
        return mainWindow.getChannel(name);
    }

    /**
	 * Get an existing channel.
	 * 
	 * @param UUID The UUID of the channel.
	 * @return The channel, null if not found.
	 */
    public Channel getChannel(UUID UUID) {
        return mainWindow.getChannel(UUID);
    }

    /**
	 * Start the client.
	 * 
	 * @throws IOException Thrown if streams to and from the provided host cannot be opened.
	 * @throws UnknownHostException Thrown if the provided host cannot be resolved.
	 */
    public void connect() throws UnknownHostException, IOException {
        setTrayIcon(Resources.IMAGE_WINDOW_ICON, true);
        abortLogin = false;
        requestedLogout = false;
        loggedIn = false;
        this.setLoginStatus(false);
        connection.start();
    }

    /**
	 * Disconnect.
	 * 
	 * @param displayDisconnectedError True to display disconnected error (only if we were logged in), false not to.
	 */
    public void disconnect(boolean displayDisconnectedError) {
        if (getLoginStatus() && displayDisconnectedError) {
            displayError("Disconnected from Second Life");
        }
        setLoginStatus(false);
        connection.close();
        clearFriends();
        clearGroups();
        clearNearbyAvatars();
        getPublicChannel().updateAvatarPositions(nearbyAvatars.values());
        updateObservers(true);
        mainWindow.getPublicChannel().receiveInformationalMessage("Disconnected from Second Life.");
        mainWindow.setAllChannelsDisconnected();
        setTrayIcon(Resources.IMAGE_WINDOW_ICON, true);
    }

    /**
	 * Set the requested logout flag.
	 */
    public void setRequestedLogout() {
        requestedLogout = true;
    }

    /**
	 * Get the requested logout flag.
	 * 
	 * @return The requested logout flag.
	 */
    public boolean getRequestedLogout() {
        return requestedLogout;
    }

    /**
	 * Get the connection that this client is using.
	 * 
	 * @return The connection that this client is using.
	 */
    public WhisperTransportConnection getConnection() {
        return connection;
    }

    /**
	 * Get the login status.
	 * 
	 * @return True if logged in, false if not.
	 */
    public boolean getLoginStatus() {
        return loggedIn;
    }

    /**
	 * Set the login status.
	 * 
	 * @param loggedIn True if logged in, false if not.
	 */
    public void setLoginStatus(boolean loggedIn) {
        this.loggedIn = loggedIn;
        if (loggedIn) {
            loginWindow.setVisible(false);
            mainWindow.setVisible(true);
            mainWindow.getPublicChannel().receiveInformationalMessage("Entered Second Life.");
        }
    }

    /**
	 * Add a friend.
	 * 
	 * @param friend The friend to add.
	 */
    public void upsertFriend(final Agent friend) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        upsertFriend(friend);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            synchronized (friends) {
                if (!friends.containsKey(friend.getUUID())) {
                    friends.put(friend.getUUID(), friend);
                } else {
                    Agent retrievedFriend = friends.get(friend.getUUID());
                    retrievedFriend.setName(friend.getName());
                    if (retrievedFriend.getOnline() != friend.getOnline()) {
                        retrievedFriend.setOnline(friend.getOnline());
                        Channel c = getChannel(friend.getName());
                        if (c != null) {
                            c.receiveInformationalMessage(friend.getName() + " is now " + (friend.getOnline() ? "online" : "offline"));
                        }
                    }
                }
            }
            updateObservers(false);
        }
    }

    /**
	 * Clear the friends list.
	 */
    public void clearFriends() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        clearFriends();
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            synchronized (friends) {
                friends.clear();
            }
            updateObservers(true);
        }
    }

    /**
	 * Add a group.
	 * 
	 * @param group The group to add.
	 */
    public void upsertGroup(final Group group) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        upsertGroup(group);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            synchronized (groups) {
                if (!groups.containsKey(group.getUUID())) {
                    groups.put(group.getUUID(), group);
                    new RequestGroupChatJoin(connection, group.getUUID()).execute();
                }
            }
            updateObservers(false);
        }
    }

    /**
	 * Clear the group list.
	 */
    public void clearGroups() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        clearGroups();
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            synchronized (groups) {
                for (Group tmpGroup : groups.values()) {
                    new RequestGroupChatLeave(connection, tmpGroup.getUUID()).execute();
                }
                groups.clear();
            }
            updateObservers(true);
        }
    }

    /**
	 * Add a nearby agent.
	 * 
	 * @param nearbyAgent The friend to add.
	 */
    public void upsertNearbyAgent(Agent nearbyAgent) {
        synchronized (nearbyAvatars) {
            if (nearbyAgent.getPresent()) {
                if (!nearbyAvatars.containsKey(nearbyAgent.getUUID())) {
                    nearbyAvatars.put(nearbyAgent.getUUID(), nearbyAgent);
                }
            } else {
                if (nearbyAvatars.containsKey(nearbyAgent.getUUID())) {
                    nearbyAvatars.remove(nearbyAgent.getUUID());
                }
            }
        }
        getPublicChannel().updateObserver(false);
        getPublicChannel().updateAvatarPositions(getNearbyAgents().values());
        mainWindow.updateObserver(false);
    }

    /**
	 * Helper method to display an error.
	 */
    public void displayError(String error) {
        displayPopup("Error", error, Severity.ERROR);
    }

    /**
	 * Set the login abortion flag.
	 * 
	 * @param abort True if aborted, otherwise false.
	 */
    public void setAbortLogin(boolean abort) {
        if (loginWindow.isVisible()) {
            this.abortLogin = abort;
            this.loggedIn = false;
            this.disconnect(false);
        }
    }

    /**
	 * Get the login abortion flag.
	 */
    public boolean getAbortLogin() {
        return abortLogin;
    }

    /**
	 * Add an observer.
	 * 
	 * @param o The observer to add
	 */
    @Override
    public void addObserver(Observer o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    /**
	 * Remove an observer.
	 * 
	 * @param o The observer to remove.
	 */
    @Override
    public void removeObserver(Observer o) {
        if (observers.contains(o)) {
            observers.remove(o);
        }
    }

    /**
	 * Notify all observers of a change.
	 * 
	 * @param purgeFirst True indicates that observers should purge data before reading in changes.
	 */
    @Override
    public void updateObservers(final boolean purgeFirst) {
        try {
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        for (Observer o : observers) {
                            o.updateObserver(purgeFirst);
                        }
                    }
                });
            } else {
                for (Observer o : observers) {
                    o.updateObserver(purgeFirst);
                }
            }
        } catch (Exception ex) {
            if (Whisper.isDebugging()) {
                ex.printStackTrace();
            }
        }
    }

    /**
	 * Get the friends associated with this agent.
	 * 
	 * @return The friends associated with this agent.
	 */
    public HashMap<UUID, Agent> getFriends() {
        HashMap<UUID, Agent> friendsClone = new HashMap<UUID, Agent>();
        synchronized (friends) {
            for (Agent tmpAgent : friends.values()) {
                friendsClone.put(tmpAgent.getUUID(), tmpAgent);
            }
        }
        return friendsClone;
    }

    /**
	 * Get the groups associated with this agent.
	 * 
	 * @return The friends associated with this agent.
	 */
    public HashMap<UUID, Group> getGroups() {
        HashMap<UUID, Group> groupsClone = new HashMap<UUID, Group>();
        synchronized (groups) {
            for (Group tmpGroup : groups.values()) {
                groupsClone.put(tmpGroup.getUUID(), tmpGroup);
            }
        }
        return groupsClone;
    }

    /**
	 * Get the nearby agents.
	 * 
	 * @return The nearby agents.
	 */
    public HashMap<UUID, Agent> getNearbyAgents() {
        HashMap<UUID, Agent> nearbyAgentsClone = new HashMap<UUID, Agent>();
        synchronized (nearbyAvatars) {
            for (Agent tmpAgent : nearbyAvatars.values()) {
                nearbyAgentsClone.put(tmpAgent.getUUID(), tmpAgent);
            }
        }
        return nearbyAgentsClone;
    }

    /**
	 * Get a reference to the current client.
	 * 
	 * @return A reference to the current client.
	 */
    public static Whisper getClient() {
        return self;
    }

    /**
	 * Process a public chat message.
	 * 
	 * @param message The message to process.
	 */
    public void processChatMessage(Message message) {
        if (message.getMessage() == null || message.getMessage().length() <= 0) return;
        getPublicChannel().receiveMessage(message);
        mainWindow.setTabIconForChannel(getPublicChannel(), Resources.ICON_PENDING_MESSAGES, true);
    }

    /**
	 * Process a group chat message.
	 * 
	 * @param message The message to process.
	 * @param sessionUUID The UUID of the chat session.
	 */
    public void processGroupMessage(Message message, final UUID sessionUUID) {
        final Channel c;
        if (getChannel(sessionUUID) == null) {
            Group tmpGroup = getGroups().get(sessionUUID);
            if (tmpGroup != null) {
                c = new ChannelGroup(tmpGroup.getName(), mainWindow, true);
                c.setUUID(sessionUUID);
                tmpGroup.addObserver((ChannelGroup) c);
                tmpGroup.updateObservers(false);
                c.receiveMessage(message);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        /**
						 * Called when the thread is run.
						 */
                        @Override
                        public void run() {
                            mainWindow.addChannel(c, false);
                            mainWindow.setTabIconForChannel(c, Resources.ICON_PENDING_MESSAGES, true);
                        }
                    });
                } catch (Exception e) {
                }
            }
        } else {
            c = getChannel(sessionUUID);
            c.receiveMessage(message);
            mainWindow.setTabIconForChannel(c, Resources.ICON_PENDING_MESSAGES, true);
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                /**
				 * Called when the thread is run.
				 */
                @Override
                public void run() {
                    mainWindow.updatePendingMessages(sessionUUID);
                }
            });
        } catch (Exception e) {
        }
    }

    /**
	 * Update the pending messages for the given channel.
	 */
    public void redrawFriendsAndGroupsList() {
        mainWindow.repaintListIcons();
    }

    /**
	 * Process an instant message.
	 * 
	 * @param name The name of the sender.
	 * @param fromUUID The UUID of the sender.
	 * @param message The message text.
	 * @param online Whether the message is an online or an offline message.
	 * @param sessionUUID The session UUID (could be a group message).
	 */
    public void processInstantMessage(final String name, final UUID fromUUID, final String message, final boolean online, final UUID sessionUUID) {
        final Channel c;
        final Agent friend = getFriends().get(fromUUID);
        if (friend != null) {
            if (online && name.toLowerCase().equals(friend.getName().toLowerCase())) friend.setOnline(true);
        }
        final Group group = getGroups().get(sessionUUID);
        if (group != null) {
            c = new ChannelGroup(group.getName(), mainWindow, true);
            c.setUUID(sessionUUID);
            new RequestGroupChatJoin(connection, sessionUUID).execute();
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is run.
					 */
                    @Override
                    public void run() {
                        synchronized (groups) {
                            mainWindow.addChannel(c, false);
                            mainWindow.updatePendingMessages(sessionUUID);
                            c.receiveMessage(new Message(message, name, fromUUID, online));
                        }
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
            return;
        }
        if (getChannel(fromUUID) != null) {
            c = getChannel(fromUUID);
        } else {
            c = new ChannelPrivate(name, mainWindow, true);
            c.setUUID(fromUUID);
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                /**
				 * Called when the thread is run.
				 */
                @Override
                public void run() {
                    synchronized (friends) {
                        if (friend != null) {
                            friend.setTyping(false);
                        }
                        mainWindow.addChannel(c, false);
                        mainWindow.updatePendingMessages(fromUUID);
                        c.receiveMessage(new Message(message, name, fromUUID, online));
                        mainWindow.setTabIconForChannel(c, Resources.ICON_PENDING_MESSAGES, true);
                    }
                }
            });
        } catch (Exception ex) {
        }
    }

    /**
	 * Called to exit the application.
	 * 
	 * @param status The exit status.
	 */
    public static void exit(int status) {
        System.exit(status);
    }

    /**
	 * Display a popup box.
	 * 
	 * @param title The title.
	 * @param message The message.
	 * @param severity The severity of the dialog.
	 */
    public void displayPopup(final String title, final String message, final Severity severity) {
        new Thread(new Runnable() {

            /**
			 * Called when the thread is run.
			 */
            @Override
            public void run() {
                if (loginWindow.isVisible()) {
                    new PopupDialog(loginWindow, title, message, severity).setVisible(true);
                } else {
                    new PopupDialog(mainWindow, title, message, severity).setVisible(true);
                }
            }
        }).start();
    }

    /**
	 * Display the ToS update box.
	 */
    public void displayTosUpdate() {
        new Thread(new Runnable() {

            /**
			 * Called when the thread is run.
			 */
            @Override
            public void run() {
                if (loginWindow.isVisible()) {
                    new TosUpdateDialog(loginWindow).setVisible(true);
                } else {
                    new TosUpdateDialog(mainWindow).setVisible(true);
                }
            }
        }).start();
    }

    /**
	 * Get the name of the currently logged in avatar.
	 * 
	 * @return The avatar's name.
	 */
    public String getName() {
        return name;
    }

    /**
	 * Set the name of the currently logged in avatar.
	 * 
	 * @param name The avatar's name.
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * Get the public channel.
	 * 
	 * @return The public channel.
	 */
    public ChannelPublic getPublicChannel() {
        return this.mainWindow.getPublicChannel();
    }

    /**
	 * Set whether the avatar tracking state can be set by the client.
	 * 
	 * @param canSet True if it can be set, false if not.
	 */
    public void setAvatarTrackingCanSetState(final boolean canSet) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        setAvatarTrackingCanSetState(canSet);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            this.avatarTrackingCanSetState = canSet;
            mainWindow.getPublicChannel().showParticipants(canSet);
            mainWindow.setAvatarTrackingCanSetState(canSet);
        }
    }

    /**
	 * Get whether the avatar tracking state can be set by the client.
	 * 
	 * @return True if the state can be set, otherwise false.
	 */
    public boolean getAvatarTrackingCanSetState() {
        return avatarTrackingCanSetState;
    }

    /**
	 * Set the current avatar tracking state.
	 * 
	 * @param avatarTrackingState True to enable, false to disable.
	 */
    public void setAvatarTrackingState(boolean avatarTrackingState) {
        this.avatarTrackingState = avatarTrackingState;
        mainWindow.setAvatarTrackingState(avatarTrackingState);
    }

    /**
	 * Get the current avatar tracking state.
	 * 
	 * @return The current avatar tracking state.
	 */
    public boolean getTrackingState() {
        return avatarTrackingState;
    }

    /**
	 * Receive an avatar tracking set state response.
	 * 
	 * @param success True if the request was successful, otherwise false.
	 */
    public void receiveAvatarTrackingSetStateResponse(boolean success) {
        mainWindow.receiveAvatarTrackingSetStateResponse(success);
    }

    /**
	 * Receive an image set state response.
	 * 
	 * @param success True if the request was successful, otherwise false.
	 */
    public void receiveImageSetStateResponse(boolean success) {
        mainWindow.receiveImageSetStateResponse(success);
    }

    /**
	 * Clear all nearby avatars.
	 */
    public void clearNearbyAvatars() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        clearNearbyAvatars();
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            synchronized (nearbyAvatars) {
                nearbyAvatars.clear();
            }
            mainWindow.clearNearbyAvatars();
        }
    }

    /**
	 * Get the friend search dialog.
	 * 
	 * @return The friend search dialog.
	 */
    public AvatarSearchDialog getFriendSearchDialog() {
        return mainWindow.getFriendSearchDialog();
    }

    /**
	 * Get the group search dialog.
	 * 
	 * @return The group search dialog.
	 */
    public GroupSearchDialog getGroupSearchDialog() {
        return mainWindow.getGroupSearchDialog();
    }

    /**
	 * Remove a friend.
	 * 
	 * @param friendUUID The UUID of the friend to remove.
	 */
    public void removeFriend(final UUID friendUUID) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        removeFriend(friendUUID);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            synchronized (friends) {
                if (friends.containsKey(friendUUID)) {
                    friends.remove(friendUUID);
                    closeChannel(friendUUID);
                }
            }
            updateObservers(false);
        }
    }

    /**
	 * Remove a group.
	 * 
	 * @param groupUUID The UUID of the group to remove.
	 */
    public void removeGroup(final UUID groupUUID) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        removeGroup(groupUUID);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            synchronized (groups) {
                if (groups.containsKey(groupUUID)) {
                    groups.remove(groupUUID);
                    closeChannel(groupUUID);
                }
            }
            updateObservers(false);
        }
    }

    /**
	 * Close a channel based on its UUID.
	 * 
	 * @param channelUUID The UUID of the channel to close.
	 */
    public void closeChannel(final UUID channelUUID) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        closeChannel(channelUUID);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            mainWindow.closeChannel(channelUUID);
        }
    }

    /**
	 * Display a friendship request popup.
	 * 
	 * @param fromName The name of the agent requesting friendship.
	 * @param fromUUID The UUID of the agent requesting friendship.
	 * @param sessionUUID The UUID of the session in which friendship was requested.
	 */
    public void displayFriendshipRequestPopup(final String fromName, final UUID fromUUID, final UUID sessionUUID) {
        new Thread(new Runnable() {

            /**
			 * Called when the thread is run.
			 */
            @Override
            public void run() {
                new PopupFriendshipRequest(mainWindow, fromName, fromUUID, sessionUUID).setVisible(true);
            }
        }).start();
    }

    /**
	 * Display a group invitation popup.
	 * 
	 * @param agentName The name of the agent inviting us to the group.
	 * @param groupName The name of the group we're being invited to.
	 * @param groupUUID The UUID of the group we're being invited to.
	 * @param sessionUUID The UUID of the session in which the invite is being offered.
	 */
    public void displayGroupInvitationPopup(final String agentName, final String groupName, final UUID groupUUID, final UUID sessionUUID) {
        new Thread(new Runnable() {

            /**
			 * Called when the thread is run.
			 */
            @Override
            public void run() {
                new PopupGroupInvitation(mainWindow, agentName, groupName, groupUUID, sessionUUID).setVisible(true);
            }
        }).start();
    }

    /**
	 * Display a teleportation offer popup.
	 * 
	 * @param agentName The name of the agent offering the teleportation.
	 * @param agentUUID The UUID of the agent offering the teleportation.
	 * @param message The message associated with the offer.
	 */
    public void displayTeleportOfferPopup(final String agentName, final UUID agentUUID, final String message) {
        new Thread(new Runnable() {

            /**
			 * Called when the thread is run.
			 */
            @Override
            public void run() {
                new PopupTeleportOffer(mainWindow, agentName, agentUUID, message).setVisible(true);
            }
        }).start();
    }

    /**
	 * Update the location of the agent.
	 * 
	 * @param simName The name of the Sim where the agent is.
	 * @param x The local X coordinate.
	 * @param y The local Y coordinate.
	 * @param z The local Z coordinate.
	 * @param imageUUID The UUID of the map image.
	 */
    public void updateLocation(final String simName, final float x, final float y, final float z, final UUID imageUUID) {
        this.simName = simName;
        this.locationImageUUID = imageUUID;
        updateLocationDetails();
    }

    /**
	 * Update the GUI to reflect the current location.
	 */
    private void updateLocationDetails() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        updateLocationDetails();
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            mainWindow.getPublicChannel().setCurrentLocation(this.simName, this.parcelName, this.locationImageUUID);
        }
    }

    /**
	 * Set whether the image state can be set.
	 * 
	 * @param canSet True if it can be set, otherwise false.
	 */
    public void setImageCanSetState(final boolean canSet) {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    /**
					 * Called when the thread is executed.
					 */
                    @Override
                    public void run() {
                        setImageCanSetState(canSet);
                    }
                });
            } catch (Exception ex) {
                if (Whisper.isDebugging()) {
                    ex.printStackTrace();
                }
            }
        } else {
            this.imageCanSetState = canSet;
            mainWindow.setImageCanSetState(canSet);
        }
    }

    /**
	 * Get the current image state.
	 * 
	 * @return True if images are enabled, otherwise false.
	 */
    public boolean getImageState() {
        return imageState;
    }

    /**
	 * Set the current image state.
	 * 
	 * @param imageState True to enable, false to disable.
	 */
    public void setImageState(boolean imageState) {
        this.imageState = imageState;
        mainWindow.setImageState(imageState);
    }

    /**
	 * Get whether the image state can be set by the client.
	 * 
	 * @return True if the state can be set, otherwise false.
	 */
    public boolean getImageCanSetState() {
        return imageCanSetState;
    }

    /**
	 * Request an image from Second Life.
	 * 
	 * @param imageUUID The UUID of the image.
	 * @param dimension The desired image dimension.
	 * @param initialImage The initial image to use (displayed whilst loading).
	 * @return A {@link SecondLifeImage} containing the image.
	 */
    public SecondLifeImage requestImage(UUID imageUUID, Dimension dimension, BufferedImage initialImage) {
        if (imageUUID == null || imageUUID.equals(new UUID(0, 0))) return new SecondLifeImage(imageUUID, dimension, initialImage);
        synchronized (images) {
            if (!images.containsKey(imageUUID)) {
                SecondLifeImage sli = new SecondLifeImage(imageUUID, dimension, initialImage);
                images.put(imageUUID, sli);
                sli.request();
                return sli;
            }
            return images.get(imageUUID);
        }
    }

    /**
	 * Resolve an image request.
	 * 
	 * @param imageUUID The UUID of the image to resolve.
	 * @param base64ImageData The base64 encoded image data.
	 */
    public void resolveImage(UUID imageUUID, String base64ImageData) {
        synchronized (images) {
            if (images.containsKey(imageUUID)) {
                SecondLifeImage sli = images.get(imageUUID);
                sli.decodeAndDraw(base64ImageData);
            }
        }
    }

    /**
	 * Update the current parcel.
	 * 
	 * @param parcelName The name of the parcel.
	 */
    public void updateParcel(String parcelName) {
        this.parcelName = parcelName;
        updateLocationDetails();
    }

    /**
	 * Get the current Sim name.
	 * 
	 * @return The current Sim name.
	 */
    public String getSimName() {
        return simName;
    }

    /**
	 * Add a channel for the given agent.
	 * 
	 * @param agent The agent to add a channel for.
	 */
    public void addPrivateChannel(Agent agent) {
        mainWindow.addChannel(new ChannelPrivate(agent.getName(), mainWindow, true), true);
    }

    /**
	 * Update the encryption state.
	 * 
	 * @param encrypting True if encrypting, false if not.
	 */
    public void updateEncryptionState(boolean encrypting) {
        mainWindow.updateSecurityImage(encrypting);
    }

    /**
	 * Flag that all groups have been received.
	 */
    public void setReceivedAllGroups() {
        receivedAllGroups = true;
        attemptOfflineMessageRequest();
    }

    /**
	 * Flag that all friends have been received.
	 */
    public void setReceivedAllFriends() {
        receivedAllFriends = true;
        attemptOfflineMessageRequest();
    }

    /**
	 * Attempt to retrieve all offline messages.
	 */
    private void attemptOfflineMessageRequest() {
        if (receivedAllGroups && receivedAllFriends && !attemptedOfflineMessageRetrieval) {
            new RequestOfflineMessages(getConnection()).execute();
            attemptedOfflineMessageRetrieval = true;
        }
    }

    /**
	 * Show an avatar profile.
	 * 
	 * @param avatarUUID The UUID of the avatar whose profile is to be displayed.
	 * @param avatarName The name of the avatar whose profile is to be displayed.
	 */
    public void showAvatarProfile(final UUID avatarUUID, final String avatarName) {
        new Thread(new Runnable() {

            /**
			 * Called when the thread is run.
			 */
            @Override
            public void run() {
                gui.windows.AvatarProfile profileUI;
                if (!pendingProfiles.containsKey(avatarUUID)) {
                    if (loginWindow.isVisible()) {
                        profileUI = new gui.windows.AvatarProfile(loginWindow, avatarName, avatarUUID);
                    } else {
                        profileUI = new gui.windows.AvatarProfile(mainWindow, avatarName, avatarUUID);
                    }
                    synchronized (pendingProfiles) {
                        pendingProfiles.put(avatarUUID, profileUI);
                    }
                } else {
                    synchronized (pendingProfiles) {
                        profileUI = pendingProfiles.get(avatarUUID);
                    }
                }
                profileUI.setVisible(true);
            }
        }).start();
    }

    /**
	 * Process an incoming avatar profile. This is called when the Second Life server
	 * responds to an avatar profile request.
	 * 
	 * @param aProfile The incoming {@link AvatarProfile}
	 */
    public void processAvatarProfile(AvatarProfile aProfile) {
        gui.windows.AvatarProfile profileUI = null;
        synchronized (pendingProfiles) {
            if (pendingProfiles.containsKey(aProfile.getAvatarUUID())) {
                profileUI = pendingProfiles.get(aProfile.getAvatarUUID());
            }
        }
        if (profileUI != null) {
            profileUI.Process(aProfile);
            removeAvatarProfile(aProfile.getAvatarUUID());
        }
    }

    /**
	 * Determine whether we're debugging.
	 * 
	 * @return True if debugging, otherwise false.
	 */
    public static boolean isDebugging() {
        return DEBUG;
    }

    /**
	 * Display a group notice.
	 * 
	 * @param groupName The name of the sending group.
	 * @param agentName The name of the sender.
	 * @param subject The notice subject.
	 * @param message The notice message.
	 */
    public void showGroupNotice(final String groupName, final String agentName, final String subject, final String message) {
        new Thread(new Runnable() {

            /**
			 * Called when the thread is run.
			 */
            @Override
            public void run() {
                if (loginWindow.isVisible()) {
                    new GroupNotice(loginWindow, groupName, agentName, subject, message).setVisible(true);
                } else {
                    new GroupNotice(mainWindow, groupName, agentName, subject, message).setVisible(true);
                }
            }
        }).start();
    }

    /**
	 * Set the tray icon.
	 * 
	 * @param image The image to set.
	 * @param stopFlashing True to stop flashing, false to continue flashing.
	 */
    public void setTrayIcon(BufferedImage image, boolean stopFlashing) {
        synchronized (trayLock) {
            if (stopFlashing) trayFlashing = false;
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                if (tray.getTrayIcons().length <= 0) {
                    try {
                        TrayIcon ti = new TrayIcon(image, "Whisper " + VERSION);
                        ti.setImageAutoSize(true);
                        tray.add(ti);
                    } catch (AWTException e) {
                    }
                } else {
                    tray.getTrayIcons()[0].setImage(image);
                }
            }
        }
    }

    /**
	 * Flash the tray icon between two images if the application does not have focus.
	 * 
	 * @param imageA The first image to display.
	 * @param imageB The second image to display.
	 */
    public void flashTrayIcon(final BufferedImage imageA, final BufferedImage imageB) {
        if (focusState) return;
        if (trayFlashing) return;
        trayFlashing = true;
        new Thread(new Runnable() {

            /**
			 * Called when the thread is run.
			 */
            @Override
            public void run() {
                while (trayFlashing) {
                    synchronized (trayLock) {
                        if (trayFlashing) {
                            setTrayIcon(imageA, false);
                        } else {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    synchronized (trayLock) {
                        if (trayFlashing) {
                            setTrayIcon(imageB, false);
                        } else {
                            break;
                        }
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }

    /**
	 * Set the focus state of the application.
	 * 
	 * @param focusState The focus state of the application.
	 */
    public void setFocusState(boolean focusState) {
        this.focusState = focusState;
    }

    /**
	 * Remove a pending avatar profile.
	 * 
	 * @param avatarUUID The UUID of the avatar whose profile is to be removed.
	 */
    public void removeAvatarProfile(UUID avatarUUID) {
        synchronized (pendingProfiles) {
            if (pendingProfiles.containsKey(avatarUUID)) {
                pendingProfiles.remove(avatarUUID);
            }
        }
    }
}

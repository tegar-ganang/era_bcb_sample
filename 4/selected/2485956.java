package free.jin.channels;

import free.freechess.FreechessConnection;
import free.jin.Connection;
import free.jin.Jin;
import free.jin.Preferences;
import free.jin.channels.prefs.ChannelsManagerPrefsPanel;
import free.jin.console.Console;
import free.jin.event.*;
import free.jin.plugin.Plugin;
import free.jin.plugin.PluginUIContainer;
import free.jin.ui.PreferencesPanel;
import free.jin.ui.UIProvider;
import org.tonic.ui.swing.CloseTabAction;
import org.tonic.ui.swing.CloseableTabbedPane;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import static java.util.Calendar.*;

/**
 * This plugin lets user see channel and shouts in tabs.
 * @author whp
 */
public class ChannelsManager extends Plugin implements ChannelsListener, ConnectionListener, ChatListener, ChangeListener, PlainTextListener {

    /**
     * The container for this plugin's ui.
     */
    private PluginUIContainer ui;

    /**
     * Component object needed for close popup menu item.
     */
    private Component componentOver;

    /**
     * JTabbedPane for chatConsoles.
     */
    private JTabbedPane mainPane;

    /**
     * ConsoleManager preferences.
     */
    private Preferences consolePreferences;

    /**
     * Map of chatConsoles. Channels' numbers are the keys.
     */
    private Map<Integer, Console> chatConsoles;

    /**
     * Boolean value indicating that channels manager received information about user's channels for the first time.
     */
    private boolean firstTime;

    /**
     * Icon for new chat event notfication in tabs.
     */
    private Icon newChatIcon;

    /**
     * Icon for notification about direct tells in tabs.
     */
    private Icon directTellIcon;

    private Icon nullIcon;

    /**
     * Set that holds all channel numbers - helps sorting and managing channels.
     */
    private Set<Integer> channelSet;

    /**
     * Pop menu for each tab.
     */
    private JPopupMenu channelMenu;

    private boolean listReceived;

    /**
     * Creates a new instance of ChannelsManager
     */
    public ChannelsManager() {
    }

    /**
     * Overriden from free.jin.plugin.Plugin
     *
     * @return plugin's id (Simple string - without any spaces)
     */
    public String getId() {
        return "channels";
    }

    /**
     * Returns the name of the plugin. Overriden from free.jin.plugin.Plugin
     *
     * @return plugin's name (normal string)
     */
    public String getName() {
        return "Channels Manager";
    }

    /**
     * Starts the plugin.
     */
    public void start() {
        chatConsoles = Collections.synchronizedMap(new TreeMap<Integer, Console>());
        channelSet = Collections.synchronizedSet(new TreeSet<Integer>());
        ui = getPluginUIContainer();
        consolePreferences = getConsolePreferences();
        createIcons();
        getControls();
        createChannelMenu();
        registerConnListeners();
    }

    private void createChannelMenu() {
        channelMenu = new JPopupMenu();
        JMenuItem close = new JMenuItem("Remove channel", nullIcon);
        JMenuItem manCh = new JMenuItem("Add/remove channels...", nullIcon);
        channelMenu.add(close);
        channelMenu.add(manCh);
        close.addActionListener(new CloseListener());
        manCh.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String channelStrings = (String) new ManageChannelsPanel().askResult();
                if (channelStrings != null && channelStrings.length() > 0) {
                    String[] channelsCommands = channelStrings.split("\\s+");
                    for (String s : channelsCommands) {
                        s = s.trim();
                        if (s.indexOf('-') != -1) {
                            getConn().sendCommand("-ch " + s.replace("-", ""));
                        } else {
                            getConn().sendCommand("+ch " + s);
                        }
                    }
                }
            }
        });
    }

    /**
     * Overrides <code>hasPreverencesUI</code> to return whether the plugin
     * will display a preferences UI (the setting is taken from the
     * <pre>"preferences.show"</pre> property.
     * @return boolean value indicating whether this plugin have ui for setting preferences
     */
    public boolean hasPreferencesUI() {
        return getPrefs().getBool("preferences.show", true);
    }

    /**
     * This method returns preferences UI for this plugin.
     * @return instance of ChannelsManagerPrefsPanel
     */
    public PreferencesPanel getPreferencesUI() {
        return new ChannelsManagerPrefsPanel(this);
    }

    /**
     * Method that readies all icons needed for this plugin.
     */
    private void createIcons() {
        newChatIcon = getIconsEasy("newChatEvent.png");
        directTellIcon = getIconsEasy("directTell.png");
        nullIcon = new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
    }

    private Icon getIconsEasy(String s) {
        URL iconURL = ChannelsManager.class.getResource(s);
        Icon icon = new ImageIcon(iconURL);
        return icon;
    }

    private Preferences getConsolePreferences() {
        Plugin consolePlugin = getPlugin("console");
        Preferences preferences = consolePlugin.getPrefs();
        return preferences;
    }

    /**
     * Close action for tabs.
     */
    private CloseTabAction closeAction = new CloseTabAction() {

        public void act(CloseableTabbedPane closableTabbedPane, int tabIndex) {
            if (!mainPane.getTitleAt(tabIndex).matches(".*shouts")) {
                getConn().sendCommand("-ch " + mainPane.getTitleAt(tabIndex));
            }
        }
    };

    /**
     * Registers all Listeners for this plugin.
     */
    private void registerConnListeners() {
        Connection conn = getConn();
        BasicListenerManager listenerManager = (BasicListenerManager) conn.getListenerManager();
        listenerManager.addChannelsListener(this);
        listenerManager.addConnectionListener(this);
        listenerManager.addChatListener(this);
        listenerManager.addPlainTextListener(this);
        mainPane.addChangeListener(this);
    }

    /**
     * Gets plugin's ui container according to the current type of DI.
     *
     * @return plugin's ui container
     */
    private PluginUIContainer getPluginUIContainer() {
        PluginUIContainer container = createContainer("channels", UIProvider.HIDEABLE_CONTAINER_MODE);
        return container;
    }

    /**
     * This method updates the ui for displaying updated channel list.
     *
     * @param remove        indicates wether channel should be removed (true) or added (false)
     * @param channelNumber number of channel to be added or removed
     */
    private synchronized void updateChannelsView(boolean remove, int channelNumber) {
        ArrayList<Integer> keys = new ArrayList<Integer>(chatConsoles.keySet());
        Collections.sort(keys);
        Iterator<Integer> iterator = keys.iterator();
        while (iterator.hasNext()) {
            Integer nextKey = iterator.next();
            if (chatConsoles.containsKey(new Integer(channelNumber)) && remove) {
                mainPane.remove(chatConsoles.remove(new Integer(channelNumber)));
            }
            if (!chatConsoles.containsKey(new Integer(channelNumber)) && !remove) {
                int index;
                for (int i1 = keys.size() - 1; i1 >= 0; i1--) {
                    if (channelNumber > (keys.get(i1)).intValue()) {
                        index = i1 + 1;
                        Console addConsole = new Console(getConn(), consolePreferences, "tell " + Integer.toString(channelNumber));
                        chatConsoles.put(new Integer(channelNumber), addConsole);
                        mainPane.insertTab(Integer.toString(channelNumber), nullIcon, chatConsoles.get(new Integer(channelNumber)), null, index);
                        break;
                    } else if (i1 < 1) {
                        index = 0;
                        Console addConsole = new Console(getConn(), consolePreferences, "tell " + Integer.toString(channelNumber));
                        chatConsoles.put(channelNumber, addConsole);
                        mainPane.insertTab(Integer.toString(channelNumber), nullIcon, chatConsoles.get(new Integer(channelNumber)), null, index);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Method overriden from ChannelsListener
     *
     * @param evt
     */
    public void channelRemoved(ChannelsEvent evt) {
        int channelNumber = evt.getChannelNumber();
        updateChannelsView(true, channelNumber);
    }

    /**
     * Method overriden from ChannelsListener. Get called when indication that channel was added to user's channel list.
     *
     * @param evt
     */
    public void channelAdded(ChannelsEvent evt) {
        int channelNumber = evt.getChannelNumber();
        updateChannelsView(false, channelNumber);
    }

    /**
     * This method is called when channel list arrives from server. It fills JTabbedPane with tabs of channels and
     * special tabs for shouts, ishouts and chess shouts.
     */
    public void channelListReceived(ChannelsEvent evt) {
        int[] channels = evt.getChannelsNumbers();
        if (firstTime) {
            for (int i = 0; i < channels.length; i++) {
                Console chatConsole = new Console(getConn(), consolePreferences, ("tell " + channels[i]));
                chatConsoles.put(channels[i], chatConsole);
                mainPane.addTab(Integer.toString(channels[i]), nullIcon, chatConsole);
                channelSet.add(new Integer(channels[i]));
            }
            for (int j = 0; j < 3; j++) {
                if (j == 0 && getPrefs().getBool("tabs.shout", true)) {
                    Console chatConsole = new Console(getConn(), consolePreferences, "shout");
                    chatConsoles.put(new Integer(500), chatConsole);
                    mainPane.addTab("shouts", nullIcon, chatConsole);
                } else if (j == 1 && getPrefs().getBool("tabs.cshout", true)) {
                    Console chatConsole = new Console(getConn(), consolePreferences, "cshout");
                    chatConsoles.put(new Integer(501), chatConsole);
                    mainPane.addTab("cshouts", nullIcon, chatConsole);
                } else if (getPrefs().getBool("tabs.plain", true)) {
                    Console chatConsole = new Console(getConn(), consolePreferences, "plain");
                    chatConsoles.put(new Integer(502), chatConsole);
                    mainPane.addTab("plain text", nullIcon, chatConsole);
                }
            }
            firstTime = false;
        } else {
            for (int i = 0; i < channels.length; i++) {
                if (channelSet.add(new Integer(channels[i]))) {
                    updateChannelsView(false, channels[i]);
                } else {
                }
            }
        }
        listReceived = true;
    }

    private void getControls() {
        if (getPrefs().getBool("tabs.closeable")) {
            CloseableTabbedPane closeablePane = new CloseableTabbedPane();
            closeablePane.setCloseTabAction(closeAction);
            mainPane = closeablePane;
        } else {
            mainPane = new JTabbedPane();
        }
        mainPane.addMouseListener(new ComponentSeeker());
        ui.getContentPane().setLayout(new BorderLayout());
        ui.setTitle(getName());
        ui.getContentPane().add(mainPane, BorderLayout.CENTER);
    }

    /**
     * This method get called when channel tell arrives from the server.
     *
     * @param evt
     */
    public void chatMessageReceived(ChatEvent evt) {
        String chatType = evt.getType();
        Object channelNumber = evt.getForum();
        if ((channelNumber != null && chatType.equals("channel-tell")) || chatType.matches("(shout)|(cshout)|(ishout)")) {
            String channelName = null;
            if (channelNumber != null) {
                channelName = channelNumber.toString();
            }
            String message = evt.getMessage();
            Console receivingConsole;
            if (chatType.equals("shout") || chatType.equals("ishout")) {
                receivingConsole = chatConsoles.get(new Integer(500));
            } else if (chatType.equals("cshout")) {
                receivingConsole = chatConsoles.get(new Integer(501));
            } else {
                receivingConsole = chatConsoles.get(new Integer(Integer.parseInt(channelName)));
            }
            Console selectedConsole = (Console) mainPane.getSelectedComponent();
            if (!receivingConsole.equals(selectedConsole)) {
                Integer index = null;
                if (chatType.matches("(shout)|(ishout)")) {
                    index = (mainPane.getTabCount() - 3);
                } else if (chatType.equals("cshout")) {
                    index = (mainPane.getTabCount() - 2);
                } else {
                    for (int k = 0; k < mainPane.getTabCount(); k++) {
                        if ((mainPane.getTitleAt(k)).equals(channelNumber.toString())) {
                            index = new Integer(k);
                            break;
                        }
                    }
                }
                highlightTab(message, index);
            }
            String type = evt.getType();
            Object forum = evt.getForum();
            String sender = evt.getSender();
            String chatMessageType = type + "." + ((forum == null) ? "" : forum.toString()) + "." + sender;
            receivingConsole.addToOutput(translateChat(evt), chatMessageType);
        }
    }

    /**
     * This method display graphic indicator on the tab when message is sent to not-selected tab.
     * @param message - text of message sent to client from server.
     * @param index - the number of tab to be hightlighted.
     */
    private void highlightTab(String message, Integer index) {
        if (!message.contains(Jin.getInstance().getConnManager().getSession().getUser().getUsername())) {
            mainPane.setIconAt(index, newChatIcon);
        } else {
            mainPane.setIconAt(index, directTellIcon);
        }
    }

    /**
     * Gets called when a connection attempt is made.
     */
    public void connectionAttempted(Connection conn, String hostname, int port) {
    }

    /**
     * Gets called when the connection to the server is established.
     */
    public void connectionEstablished(Connection conn) {
    }

    /**
     * Gets called when the connection attempt failed.
     */
    public void connectingFailed(Connection conn, String reason) {
    }

    /**
     * Gets called when the login procedure is successful. Here it sends inch <user_name> command to the server.
     */
    public void loginSucceeded(Connection conn) {
        ((FreechessConnection) getConn()).sendCommFromPlugin("=ch");
        firstTime = true;
    }

    /**
     * Gets called when the login procedure fails. Note that <code>reason</code> may be null.
     */
    public void loginFailed(Connection conn, String reason) {
    }

    /**
     * Gets called when the connection to the server is lost.
     */
    public void connectionLost(Connection conn) {
        mainPane.removeAll();
        ui.dispose();
        chatConsoles.clear();
        firstTime = false;
    }

    protected String translateChat(ChatEvent evt) {
        String timestamp = getTimestamp();
        String type = evt.getType();
        String sender = evt.getSender();
        String title = evt.getSenderTitle();
        String rating = (evt.getSenderRating() == -1) ? "----" : String.valueOf(evt.getSenderRating());
        String message = evt.getMessage();
        Object forum = evt.getForum();
        if (type.equals("tell")) {
            return new StringBuilder().append(timestamp).append(sender).append(title).append(" tells you: ").append(message).toString();
        } else if (type.equals("say")) {
            return timestamp + sender + title + " says: " + message;
        } else if (type.equals("ptell")) {
            return timestamp + sender + title + " (your partner) tells you: " + message;
        } else if (type.equals("qtell")) {
            return timestamp + ":" + message;
        } else if (type.equals("qtell.tourney")) {
            return timestamp + ":" + sender + title + "(T" + forum + "): " + message;
        } else if (type.equals("channel-tell")) {
            return timestamp + sender + title + "(" + forum + "): " + message;
        } else if (type.equals("kibitz")) {
            return timestamp + sender + title + "(" + rating + ")[" + forum + "] kibitzes: " + message;
        } else if (type.equals("whisper")) {
            return timestamp + sender + title + "(" + rating + ")[" + forum + "] whispers: " + message;
        } else if (type.equals("shout")) {
            return timestamp + sender + title + " shouts: " + message;
        } else if (type.equals("ishout")) {
            return timestamp + "--> " + sender + title + " " + message;
        } else if (type.equals("tshout")) {
            return timestamp + ":" + sender + title + " t-shouts: " + message;
        } else if (type.equals("cshout")) {
            return timestamp + sender + title + " c-shouts: " + message;
        } else if (type.equals("announcement")) {
            return "    **ANNOUNCEMENT** from " + sender + ": " + message;
        }
        return evt.toString();
    }

    /**
     * Removes icon from tab when it is selected.
     *
     * @param e ChangeEvent
     */
    public void stateChanged(ChangeEvent e) {
        mainPane.setIconAt(mainPane.getSelectedIndex(), nullIcon);
    }

    /**
     * Returns local time in format [HH:MM]
     * @return hour and minutes time
     */
    private String getTimestamp() {
        StringBuilder time = new StringBuilder(8);
        if (getPrefs().getBool("date.display")) {
            TimeZone tz = TimeZone.getDefault();
            String hour = String.valueOf(getInstance(tz).get(HOUR_OF_DAY));
            if (hour.length() == 1) {
                hour = '0' + hour;
            }
            String minute = String.valueOf(getInstance(tz).get(MINUTE));
            if (minute.length() == 1) {
                minute = '0' + minute;
            }
            time = time.append('[').append(hour).append(':').append(minute).append("] ");
        } else {
        }
        return time.toString();
    }

    public void plainTextReceived(PlainTextEvent evt) {
        Console selectedConsole = (Console) mainPane.getSelectedComponent();
        Console receivingConsole = chatConsoles.get(502);
        if (listReceived && receivingConsole != null) {
            receivingConsole.addToOutput(evt.getText(), "plain");
            if (!selectedConsole.equals(receivingConsole)) {
                highlightTab("", mainPane.getTabCount() - 1);
            }
        }
    }

    /**
     * This class checks over which component mouse was clicked.
     */
    class ComponentSeeker extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            if (e.getButton() == MouseEvent.BUTTON3) {
                for (int i = 0; i < mainPane.getTabCount(); i++) {
                    if (mainPane.getBoundsAt(i).contains(x, y)) {
                        channelMenu.show(e.getComponent(), x, y);
                        if (!(mainPane.getTitleAt(mainPane.indexAtLocation(x, y)).matches(".*shouts"))) {
                            componentOver = mainPane.getComponentAt(mainPane.indexAtLocation(x, y));
                        } else {
                        }
                    }
                }
            }
        }
    }

    /**
     * This listener is responsible for sending remove channel command to server according over which channel tab mouse was clicked.
     */
    class CloseListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {
            if (componentOver != null) {
                getConn().sendCommand("-ch " + mainPane.getTitleAt(mainPane.indexOfComponent(componentOver)));
            }
        }
    }
}

package org.javadelic.bajjer;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.BoxLayout;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Field;
import org.javadelic.burrow.JabListener;
import org.javadelic.burrow.JabConfiguration;
import org.javadelic.burrow.JabPresence;
import org.javadelic.burrow.JabIq;
import org.javadelic.burrow.JabEvent;
import org.javadelic.burrow.JabFavorite;
import org.javadelic.burrow.JabUtil;
import org.javadelic.burrow.JabParser;
import org.javadelic.burrow.query.Private;

public class BajjerMenu extends JMenuBar implements ActionListener, JabListener {

    public static final int UNKNOWN = 0;

    public static final int SIGNON = 1;

    public static final int CLIENT = 2;

    public static boolean bDebug = false;

    private JMenu bajjerMenu;

    private JMenuItem configItem;

    private JMenuItem joinConference;

    private JMenuItem quitItem;

    private JMenu favoriteMenu;

    private JMenu removeMenu;

    private int screen;

    private int baseFavoriteMenuSize = 0;

    private JabPresence presence;

    private Vector favorites = new Vector();

    private HashMap favoritesMap = new HashMap();

    private String[] classes = { "BajjerClient", "BajjerCompose", "BajjerConference", "BajjerError", "BajjerInbox", "BajjerJoinConference", "BajjerMenu", "BajjerRawMode", "BajjerRoster", "BajjerSignon", "JabCommandProcessor", "JabConfiguration", "JabConfigurationParser", "JabConnection", "JabDispatcher", "JabEvent", "JabExtension", "JabFavorite", "JabMessage", "JabPacket", "JabParser", "JabPresence", "JabProfile", "JabReader", "JabUtil", "JabWriter" };

    public BajjerMenu(int screen) {
        super();
        this.screen = screen;
        bajjerMenu = new JMenu("Bajjer");
        this.add(bajjerMenu);
        configItem = new JMenuItem("Configure");
        joinConference = new JMenuItem("Join Conference");
        if (screen == SIGNON) {
            joinConference.setEnabled(false);
        }
        joinConference.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                new BajjerJoinConference();
            }
        });
        favoriteMenu = new JMenu("Favorite Groups");
        favoriteMenu.setEnabled(false);
        JMenuItem addFavorite = new JMenuItem("Add Favorite");
        addFavorite.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                new AddFavorite(Bajjer.clientWindow);
            }
        });
        removeMenu = new JMenu("Remove Favorite");
        removeMenu.addActionListener(this);
        favoriteMenu.add(addFavorite);
        favoriteMenu.add(removeMenu);
        favoriteMenu.addSeparator();
        baseFavoriteMenuSize = favoriteMenu.getItemCount();
        final JCheckBoxMenuItem toFront = new JCheckBoxMenuItem("toFront");
        toFront.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Bajjer.bToFront = toFront.getState();
            }
        });
        final JCheckBoxMenuItem audioAlert = new JCheckBoxMenuItem("Audio Alert");
        audioAlert.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Bajjer.bBeep = audioAlert.getState();
            }
        });
        quitItem = new JMenuItem("Quit");
        quitItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                System.exit(0);
            }
        });
        bajjerMenu.add(configItem);
        bajjerMenu.addSeparator();
        bajjerMenu.add(joinConference);
        bajjerMenu.add(favoriteMenu);
        bajjerMenu.addSeparator();
        bajjerMenu.add(toFront);
        bajjerMenu.add(audioAlert);
        bajjerMenu.addSeparator();
        bajjerMenu.add(quitItem);
        JMenu presenceMenu = new JMenu("Presence");
        this.add(presenceMenu);
        JMenuItem available = createMenuItem("Available", "chat");
        JMenuItem bajjerme = createMenuItem("Bajjer Me!", "chat");
        JMenu awayMenu = new JMenu("Away");
        JMenuItem lunch = createMenuItem("At Lunch", "away");
        JMenuItem phone = createMenuItem("On the Phone", "away");
        JMenuItem meeting = createMenuItem("Meeting", "away");
        awayMenu.add(lunch);
        awayMenu.add(phone);
        awayMenu.add(meeting);
        JMenu xaMenu = new JMenu("Extended Away");
        JMenuItem sleeping = createMenuItem("Sleeping", "xa");
        JMenuItem home = createMenuItem("Gone home", "xa");
        xaMenu.add(sleeping);
        xaMenu.add(home);
        JMenu doNotDisturbMenu = new JMenu("Do Not Disturb");
        JMenuItem coding = createMenuItem("Busy Coding", "dnd");
        JMenuItem yoga = createMenuItem("Practicing Yoga", "dnd");
        JMenuItem boss = createMenuItem("Boss is watching", "dnd");
        doNotDisturbMenu.add(coding);
        doNotDisturbMenu.add(yoga);
        doNotDisturbMenu.add(boss);
        JMenuItem custom = new JMenuItem("Custom Presence");
        custom.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                new CustomPresence(Bajjer.clientWindow);
            }
        });
        presenceMenu.add(available);
        presenceMenu.add(bajjerme);
        presenceMenu.add(awayMenu);
        presenceMenu.add(xaMenu);
        presenceMenu.add(doNotDisturbMenu);
        presenceMenu.addSeparator();
        presenceMenu.add(custom);
        if (screen == SIGNON) {
            presenceMenu.setEnabled(false);
        }
        JMenu debugMenu = new JMenu("Debug");
        this.add(debugMenu);
        for (int i = 0; i < classes.length; i++) {
            debugMenu.add(createCheckboxMenuItem(classes[i], "debug"));
        }
        if (screen != SIGNON) {
            Bajjer.jabberServer.addListener((JabListener) this, JabUtil.IQ);
            JabIq iq = new JabIq();
            iq.setId("BajjerMenu:favorites");
            iq.setType("get");
            iq.setQuery("jabber:iq:private", "Private", new Private());
            iq.send(Bajjer.jabberServer);
        }
    }

    private JMenuItem createMenuItem(String label, String command) {
        JMenuItem item = new JMenuItem(label);
        item.setActionCommand(command);
        item.addActionListener(this);
        return item;
    }

    private JCheckBoxMenuItem createCheckboxMenuItem(String label, String command) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
        item.setActionCommand(command);
        item.addActionListener(this);
        return item;
    }

    public void actionPerformed(ActionEvent evt) {
        JMenuItem item = (JMenuItem) evt.getSource();
        String command = item.getActionCommand();
        BajjerClient client = Bajjer.clientWindow;
        if (command.equals("favorite")) {
            Bajjer.signonWindow.setVisible(false);
            client.setVisible(true);
            JabFavorite favorite = (JabFavorite) favoritesMap.get(item.getText());
            String room = (favorite.getChannel() + "@" + favorite.getServer()).toLowerCase();
            HashMap roomList = client.getRoomList();
            if (roomList.containsKey(room)) {
                client.setActive(room);
            } else {
                BajjerConference conferenceWindow = new BajjerConference(favorite.getChannel(), favorite.getServer(), favorite.getNickname());
                client.addRoom(room, conferenceWindow.getConferenceRoom());
                client.addWindow("Group Chat - " + favorite.getChannel(), conferenceWindow.getConferenceRoom());
                presence = new JabPresence("", favorite.getChannel() + "@" + favorite.getServer() + "/" + favorite.getNickname(), "", "Online", "0", "", null);
                presence.send(Bajjer.jabberServer);
            }
        } else if (command.equals("remove")) {
            String favoriteName = item.getText();
            JabFavorite favorite = (JabFavorite) favoritesMap.get(favoriteName);
            favoritesMap.remove(favoriteName);
            favorites.remove(favorite);
            Private preferences = new Private();
            preferences.setFavorites(favorites);
            JabIq iq = new JabIq();
            iq.setId("BajjerMenu:removefavorite");
            iq.setType("set");
            iq.setQuery("jabber:iq:private", "Private", preferences);
            iq.send(Bajjer.jabberServer);
            iq = new JabIq();
            iq.setId("BajjerMenu:favorites");
            iq.setType("get");
            iq.setQuery("jabber:iq:private", "Private", new Private());
            iq.send(Bajjer.jabberServer);
        } else if (command.equals("debug")) {
            JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) evt.getSource();
            try {
                Class clazz = Class.forName(checkbox.getText());
                Field field = clazz.getField("bDebug");
                field.setBoolean(clazz, checkbox.getState());
            } catch (Exception e) {
                System.out.println("Unable to set debugging for class: " + checkbox.getText());
                System.out.println("Exception = " + e.toString());
            }
        } else {
            String value = item.getText();
            presence = new JabPresence(value, "0", command);
            Vector rooms = JabUtil.vectorFromHashMap(client.getRoomList());
            presence.broadcast(Bajjer.jabberServer, rooms);
        }
    }

    public void messageReceived(JabEvent evt) {
    }

    public void presenceReceived(JabEvent evt) {
    }

    public void xmlSentToServer(JabEvent evt) {
    }

    public void xmlReceivedFromServer(JabEvent evt) {
    }

    public void iqReceived(JabEvent evt) {
        JabIq iq = evt.getIq();
        if ((iq.getId() != null) && (iq.getId().equals("BajjerMenu:favorites"))) {
            while (favoriteMenu.getItemCount() > baseFavoriteMenuSize) {
                favoriteMenu.remove(baseFavoriteMenuSize);
            }
            removeMenu.removeAll();
            Vector queries = iq.getQueries();
            Object userObj = iq.consume("Private");
            if (userObj == null) {
                return;
            }
            favorites = ((Private) userObj).getFavorites();
            debug("favorites.size() = " + favorites.size());
            if (favorites.size() > 0) {
                JMenuItem item = null;
                favoriteMenu.setEnabled(true);
                for (int i = 0; i < favorites.size(); i++) {
                    JabFavorite favorite = (JabFavorite) favorites.get(i);
                    debug("Adding " + favorite.getName());
                    item = new JMenuItem(favorite.getName());
                    item.setActionCommand("favorite");
                    item.addActionListener(this);
                    favoriteMenu.add(item);
                    item = new JMenuItem(favorite.getName());
                    item.setActionCommand("remove");
                    item.addActionListener(this);
                    removeMenu.add(item);
                    favoritesMap.put(favorite.getName(), favorite);
                }
            }
        }
    }

    private void debug(String msg) {
        if (bDebug) {
            System.out.println("BajjerMenu: " + msg);
        }
    }

    /*******************************************************************
	  * CustomPresence creates the dialog window for entereing custom
	  * presence information. 
	  *******************************************************************/
    class CustomPresence extends JDialog implements ActionListener {

        JRadioButton availableButton;

        JRadioButton awayButton;

        JRadioButton xaButton;

        JRadioButton dndButton;

        JTextField statusField;

        public CustomPresence(JFrame parent) {
            super(parent, "Custom Presence", true);
            Container pane = this.getContentPane();
            pane.setLayout(new BorderLayout());
            JPanel logo = new JPanel();
            logo.setBackground(Color.white);
            JLabel bajTitle = new JLabel();
            URLClassLoader cl = (URLClassLoader) getClass().getClassLoader();
            URL url = cl.findResource("org/javadelic/bajjer/bajjer2.jpg");
            bajTitle.setIcon(new ImageIcon(url));
            logo.add(bajTitle);
            pane.add(logo, "West");
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Online Status"));
            availableButton = new JRadioButton("Available");
            awayButton = new JRadioButton("Away");
            xaButton = new JRadioButton("Extended Away");
            dndButton = new JRadioButton("Do Not Disturb");
            ButtonGroup group = new ButtonGroup();
            group.add(availableButton);
            group.add(awayButton);
            group.add(xaButton);
            group.add(dndButton);
            center.add(availableButton);
            center.add(awayButton);
            center.add(xaButton);
            center.add(dndButton);
            availableButton.setSelected(true);
            center.add(new JLabel("Description"));
            statusField = new JTextField(10);
            center.add(statusField);
            pane.add(center, "Center");
            JPanel buttons = new JPanel();
            buttons.setBackground(Color.white);
            JButton send = new JButton("Send");
            send.addActionListener(this);
            buttons.add(send);
            JButton close = new JButton("Close");
            close.addActionListener(this);
            buttons.add(close);
            pane.add(buttons, "South");
            this.pack();
            this.show();
        }

        public void actionPerformed(ActionEvent evt) {
            if (evt.getActionCommand().equals("Close")) {
                this.dispose();
            } else if (evt.getActionCommand().equals("Send")) {
                String showValue = new String("");
                String statusValue = new String("Online");
                if (awayButton.isSelected()) {
                    showValue = "away";
                    statusValue = "away";
                } else if (xaButton.isSelected()) {
                    showValue = "xa";
                    statusValue = "Extended away";
                } else if (dndButton.isSelected()) {
                    showValue = "dnd";
                    statusValue = "Do not disturb";
                }
                if ((statusField.getText() != null) && (statusField.getText().length() > 0)) {
                    statusValue = statusField.getText();
                }
                JabPresence presence = new JabPresence(statusValue, "0", showValue);
                Vector rooms = JabUtil.vectorFromHashMap(Bajjer.clientWindow.getRoomList());
                presence.broadcast(Bajjer.jabberServer, rooms);
                this.dispose();
            }
        }
    }

    /*******************************************************************
	  * AddFavorite creates the dialog window for adding to the list of
	  * favorite chat rooms.
	  *******************************************************************/
    class AddFavorite extends JDialog implements ActionListener {

        JTextField name;

        JTextField channel;

        JTextField server;

        JTextField nickname;

        public AddFavorite(JFrame parent) {
            super(parent, "Add to Favorites", true);
            Container pane = this.getContentPane();
            pane.setLayout(new BorderLayout());
            JPanel logo = new JPanel();
            logo.setBackground(Color.white);
            JLabel bajTitle = new JLabel();
            URLClassLoader cl = (URLClassLoader) getClass().getClassLoader();
            URL url = cl.findResource("org/javadelic/bajjer/bajjer2.jpg");
            bajTitle.setIcon(new ImageIcon(url));
            logo.add(bajTitle);
            pane.add(logo, "West");
            JPanel conference = new JPanel();
            conference.setLayout(new BoxLayout(conference, BoxLayout.Y_AXIS));
            conference.setBackground(Color.white);
            conference.add(new JLabel("Name:", JLabel.RIGHT));
            name = new JTextField("", 20);
            conference.add(name);
            conference.add(new JLabel("Channel:", JLabel.RIGHT));
            channel = new JTextField("", 20);
            conference.add(channel);
            conference.add(new JLabel("Server:", JLabel.RIGHT));
            server = new JTextField("conference.jabber.org", 20);
            conference.add(server);
            conference.add(new JLabel("Nick:", JLabel.RIGHT));
            nickname = new JTextField(20);
            conference.add(nickname);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.setBackground(Color.white);
            JButton addFavorite = new JButton("Add");
            addFavorite.addActionListener(this);
            JButton close = new JButton("Close");
            close.addActionListener(this);
            buttons.add(addFavorite);
            buttons.add(close);
            pane.add(conference, "Center");
            pane.add(buttons, "South");
            this.pack();
            this.show();
        }

        public void actionPerformed(ActionEvent evt) {
            JabFavorite favorite;
            if (evt.getActionCommand().equals("Close")) {
                this.dispose();
            } else if (evt.getActionCommand().equals("Add")) {
                favorite = new JabFavorite(name.getText(), channel.getText(), server.getText(), nickname.getText());
                favorites.add(favorite);
                favoritesMap.put(name.getText(), favorite);
                Private preferences = new Private();
                preferences.setFavorites(favorites);
                JabIq iq = new JabIq();
                iq.setId("BajjerMenu:addfavorite");
                iq.setType("set");
                iq.setQuery("jabber:iq:private", "Private", preferences);
                iq.send(Bajjer.jabberServer);
                iq = new JabIq();
                iq.setId("BajjerMenu:favorites");
                iq.setType("get");
                iq.setQuery("jabber:iq:private", "Private", new Private());
                iq.send(Bajjer.jabberServer);
                this.dispose();
            }
        }
    }
}

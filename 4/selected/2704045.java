package org.speakmon.coffeehouse.swingui;

import org.speakmon.coffeehouse.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.net.UnknownHostException;
import java.io.IOException;

/**
 * ChatWindow.java
 *
 *
 * Created: Mon Oct 28 02:37:06 2002
 *
 * @author <a href="mailto:ben@speakmon.org">Ben Speakmon</a>
 * @version
 */
public class ChatWindow extends JPanel implements ChannelManager {

    private static final Border border = BorderFactory.createEtchedBorder();

    private final JTabbedPane chatWindow;

    private static final Session session = Session.getInstance();

    private Map channelTabs;

    public ChatWindow() {
        super();
        session.setManager(this);
        channelTabs = Collections.synchronizedMap(new HashMap());
        chatWindow = new JTabbedPane();
        chatWindow.setBorder(BorderFactory.createEmptyBorder());
        ServerConnection server = session.createServerConnection("localhost", 6667);
        server.setNick("GorgeousOrifice");
        server.setUsername("gorgorif");
        server.setRealname("gorgorif");
        server.setInvisible(false);
        try {
            server.open();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setLayout(new GridLayout(1, 1));
        add(chatWindow);
    }

    public void add(Channel channel) {
        if (!channelTabs.containsKey(channel.getName())) {
            Component tab = new ChatWindowTab(channel);
            addTab(channel.getName(), tab);
        }
    }

    private synchronized void addTab(String tabName, Component tab) {
        chatWindow.addTab(tabName, tab);
        chatWindow.setSelectedComponent(tab);
        channelTabs.put(tabName, tab);
    }

    public void remove(ServerListener listener) {
        if (channelTabs.containsKey(listener.getName())) {
            chatWindow.remove((Component) channelTabs.get(listener));
            channelTabs.remove(listener.getName());
        }
    }

    public ServerListener getChannel(String name) {
        ServerListener chan = null;
        Set keySet = channelTabs.keySet();
        for (Iterator i = keySet.iterator(); i.hasNext(); ) {
            chan = (ServerListener) i.next();
            if (chan.getName().equals(name)) {
                break;
            }
        }
        return chan;
    }

    public ServerListener getCurrentChannel() {
        ChatWindowTab tab = (ChatWindowTab) chatWindow.getSelectedComponent();
        return tab.getServerListener();
    }
}

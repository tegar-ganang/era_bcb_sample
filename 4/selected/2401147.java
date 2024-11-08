package com.cjw.ircclient;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.moxy.irc.*;

class ClientConnection implements ConsoleListener {

    Client client;

    int id;

    IRCPanel console;

    Hashtable querypanels;

    Vector panels;

    ClientConnection(Client client, int id, ChatWindow window) {
        this.client = client;
        this.id = id;
        querypanels = new Hashtable();
        panels = new Vector();
        console = window.createPanel(client.getConnection(id).getServer(), id);
        registerPanel(console);
        client.registerConsoleListener(id, this);
    }

    void handleReconnect() {
        int i;
        IRCPanel panel;
        IRCConnection connection;
        String channel;
        for (i = 0; i < panels.size(); i++) {
            panel = (IRCPanel) panels.elementAt(i);
            channel = panel.getChannelName();
            connection = client.getConnection(id);
            connection.joinChannel(channel);
        }
    }

    private IRCPanel createNewQueryPanel(String name) {
        IRCPanel panel;
        panel = console.getParentWindow().createPanel(name, id);
        panel.setChannelName(name);
        querypanels.put(name, panel);
        return panel;
    }

    private void deleteQueryPanel(IRCPanel panel) {
        int index;
        Enumeration keys, values;
        keys = querypanels.keys();
        values = querypanels.elements();
        for (; values.hasMoreElements(); ) if (values.nextElement() == panel) querypanels.remove(keys.nextElement()); else keys.nextElement();
    }

    void deletePanel(IRCPanel panel) {
        panels.removeElement(panel);
        if (querypanels.contains(panel)) {
            deleteQueryPanel(panel);
            return;
        } else {
            if (panel == console) {
                if (panels.size() == 0) {
                    client.disconnect(id);
                } else {
                    console = (IRCPanel) panels.firstElement();
                }
            }
            panel.detach();
        }
    }

    void registerPanel(IRCPanel panel) {
        panels.addElement(panel);
    }

    public void handleConsoleMsg(IRCMessage message) {
        int i;
        StringBuffer buffer;
        IRCLine line;
        int index;
        line = message.getIRCLine();
        switch(message.getMsgType()) {
            case IRCMessage.MSG_JOIN:
                IRCPanel panel = null;
                IRCPanel window;
                String channel;
                IRCConnection connection;
                boolean foundPanel;
                System.out.println("Client::handleConsoleMsg called with JOIN");
                foundPanel = false;
                channel = message.getTarget();
                connection = message.getConnection();
                for (i = 0; i < panels.size(); i++) {
                    panel = (IRCPanel) panels.elementAt(i);
                    if (panel.getChannelName() != null && panel.getChannelName().equalsIgnoreCase(channel)) {
                        foundPanel = true;
                        break;
                    }
                }
                if (!foundPanel) {
                    window = console;
                    if (window.isTaken()) panel = window.getParentWindow().createPanel(channel, id); else {
                        panel = window;
                    }
                    foundPanel = true;
                }
                if (foundPanel) {
                    connection.registerChannelListener(channel, panel);
                    panel.setChannelName(channel);
                }
                break;
            case IRCMessage.MSG_NOTICE:
                console.showString("--- " + message.getIRCLine().getRemaining());
                break;
            default:
                buffer = new StringBuffer();
                buffer.append(message.getType() + " ");
                for (i = 0; i < message.getParamCount(); i++) buffer.append((String) message.getParam(i) + " ");
                buffer.append(message.getIRCLine().getRemaining());
                if (console == null) System.out.println("message to non-existant console" + buffer.toString()); else console.showString(buffer.toString());
        }
    }

    public void handlePrivateMsg(IRCConnection connection, String sender, String message) {
        IRCPanel panel;
        String name;
        name = IRCMessage.getNickName(sender);
        panel = (IRCPanel) querypanels.get(name);
        if (panel == null) panel = createNewQueryPanel(name);
        panel.handleMessage(sender, new IRCLine(message));
    }

    public void handlePrivateNotice(IRCConnection connection, String sender, String message) {
    }

    public void handleNickChange(IRCConnection connection, String oldName, String newName) {
        IRCPanel panel;
        panel = (IRCPanel) querypanels.get(oldName);
        if (panel == null) return;
        panel.setChannelName(newName);
        querypanels.remove(oldName);
        querypanels.put(newName, panel);
    }
}

package com.cjw.ircclient;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import org.moxy.irc.*;

class ChatWindow extends JFrame implements ActionListener, KeyListener, ChangeListener {

    Vector panels;

    JTextField inputLine;

    JTabbedPane pane;

    ClientGUI application;

    JLabel nickLabel;

    IRCPanel lastSelectedTab;

    ChatWindow(ClientGUI application) {
        JPanel bottomPanel;
        this.application = application;
        setSize(600, 400);
        initJMenus();
        getContentPane().setLayout(new BorderLayout());
        panels = new Vector();
        lastSelectedTab = null;
        pane = new JTabbedPane();
        getContentPane().add(pane, BorderLayout.CENTER);
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        nickLabel = new JLabel();
        bottomPanel.add(nickLabel, BorderLayout.WEST);
        inputLine = new JTextField(120);
        bottomPanel.add(inputLine, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        inputLine.addKeyListener(this);
        pane.addChangeListener(this);
    }

    private void initJMenus() {
        JMenuBar menubar;
        JMenu menu;
        JMenuItem item;
        menubar = new JMenuBar();
        menu = new JMenu("File");
        item = new JMenuItem("New Connection...");
        item.addActionListener(this);
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem("Close");
        item.addActionListener(this);
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem("Preferences...");
        item.addActionListener(this);
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem("Quit");
        item.addActionListener(this);
        menu.add(item);
        menubar.add(menu);
        menu = new JMenu("Edit");
        item = new JMenuItem("Undo");
        item.addActionListener(this);
        menu.add(item);
        menu.addSeparator();
        item = new JMenuItem("Cut");
        item.addActionListener(this);
        menu.add(item);
        item = new JMenuItem("Copy");
        item.addActionListener(this);
        menu.add(item);
        item = new JMenuItem("Paste");
        item.addActionListener(this);
        menu.add(item);
        menubar.add(menu);
        menu = new JMenu("Windows");
        item = new JMenuItem("Show Connections");
        item.addActionListener(this);
        menu.add(item);
        menubar.add(menu);
        this.setJMenuBar(menubar);
    }

    public void doConnect(String server, int port, String nick) {
        application.doConnect(server, port, nick, this);
    }

    public IRCPanel createPanel(String name) {
        return createPanel(name, -1);
    }

    public IRCPanel createPanel(String name, int connectionID) {
        IRCPanel panel;
        panel = new IRCPanel(connectionID, application, this);
        panels.addElement(panel);
        pane.addTab(name, panel);
        application.registerPanel(panel);
        return panel;
    }

    void setTabName(IRCPanel panel, String name) {
        int index;
        index = pane.indexOfComponent(panel);
        if (index < 0) return;
        pane.setTitleAt(index, name);
    }

    private void updateUserNick() {
        IRCPanel panel;
        panel = (IRCPanel) pane.getSelectedComponent();
        if (panel == null) {
            nickLabel.setText("[none]");
            nickLabel.setIcon(null);
            return;
        }
        nickLabel.setText(panel.getNickName());
        if (panel.isOpped()) nickLabel.setIcon(new ImageIcon("green_dot.gif")); else if (panel.isVoiced()) nickLabel.setIcon(new ImageIcon("yellow_dot.gif")); else nickLabel.setIcon(null);
    }

    void nickChanged(IRCPanel panel) {
        if (panel == pane.getSelectedComponent()) updateUserNick();
    }

    public void stateChanged(ChangeEvent event) {
        int index;
        updateUserNick();
        index = pane.getSelectedIndex();
        if (index < 0) return;
        pane.setForegroundAt(index, Color.black);
        if (lastSelectedTab != null) lastSelectedTab.resetHighlightState();
        lastSelectedTab = (IRCPanel) pane.getSelectedComponent();
    }

    public void titleChanged(IRCPanel panel) {
        String name;
        name = panel.getChannelName();
        if (name == null) setTabName(panel, "none"); else setTabName(panel, name);
    }

    public void actionPerformed(ActionEvent event) {
        String command;
        command = event.getActionCommand();
        System.out.println("action: " + command);
        if (command.equals("New Connection...")) handleNewConnection(); else if (command.equals("Close")) handleClose(); else if (command.equals("Preferences...")) handlePreferences(); else if (command.equals("Quit")) handleQuit(); else if (command.equals("Show Connections")) handleShowConnections();
    }

    public void keyTyped(KeyEvent evt) {
    }

    public void keyReleased(KeyEvent evt) {
    }

    public void keyPressed(KeyEvent evt) {
        String line;
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            line = inputLine.getText();
            if (line.equals("")) return;
            inputLine.setText("");
            ((IRCPanel) pane.getSelectedComponent()).doInput(new IRCLine(line));
        }
    }

    void somethingHappened(IRCPanel target, int level) {
        int index;
        Color color;
        if (target == pane.getSelectedComponent()) return;
        index = pane.indexOfComponent(target);
        if (index < 0) return;
        switch(level) {
            case 0:
                color = Color.black;
                break;
            case 100:
                color = Color.red;
                break;
            case 200:
                color = Color.blue;
                break;
            default:
                color = Color.black;
        }
        pane.setForegroundAt(index, color);
    }

    private void handleNewConnection() {
        (new ConnectDialog(this)).show();
    }

    private void handleClose() {
    }

    private void handlePreferences() {
    }

    void handleQuit() {
        application.handleQuit();
    }

    void handleQuit(String message) {
        application.handleQuit(message);
    }

    void handleShowConnections() {
        application.showConnectionsWindow();
    }

    void removePanel(IRCPanel panel) {
        if (!panels.removeElement(panel)) return;
        application.deletePanel(panel);
        pane.remove(panel);
    }

    void handleDisconnect(int connectionID) {
        int i;
        IRCPanel currentPanel;
        for (i = 0; i < panels.size(); i++) {
            currentPanel = (IRCPanel) panels.elementAt(i);
            if (currentPanel.getConnection() == connectionID) {
                currentPanel.reset(true);
                currentPanel.showString("<disconnected>");
            }
        }
    }
}

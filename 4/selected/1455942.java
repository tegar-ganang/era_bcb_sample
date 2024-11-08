package org.jdamico.ircivelaclient.view;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import jerklib.Channel;
import jerklib.Session;
import org.jdamico.ircivelaclient.config.Constants;
import org.jdamico.ircivelaclient.util.IRCIvelaClientStringUtils;

public class ChatPanel extends JPanel {

    private static int row = 0;

    private static final long serialVersionUID = -97950894125721726L;

    private Session session;

    boolean threadSuspended;

    private JScrollPane mainContentScrollPane = null;

    private JEditorPane mainContentArea = new JEditorPane();

    private JTextArea messageArea = new JTextArea();

    private LoadingPanel loadingPanel;

    private UsersPanel usersPanel;

    private Document doc = null;

    private Hashtable userColorTable = new Hashtable();

    private ArrayList<String> usersHost = new ArrayList<String>();

    String[] colors = { "purple", "black", "green", "pink", "gray" };

    public ChatPanel(HandleApplet parent) {
        this.setLayout(null);
        this.usersPanel = new UsersPanel(parent);
        this.usersPanel.setSize(100, 100);
        this.loadingPanel = new LoadingPanel();
        this.loadingPanel.setSize(300, 300);
        this.loadingPanel.setLocation(250, 50);
        mainContentArea.setEditable(false);
        mainContentArea.setContentType(Constants.MAINCONTENT_CONTENT_TYPE);
        mainContentArea.setEditorKit(new HTMLEditorKit());
        setVisible(true);
        mainContentArea.setBackground(Color.WHITE);
        mainContentScrollPane = new JScrollPane(mainContentArea);
        messageArea.setEnabled(false);
        messageArea.setAutoscrolls(true);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(false);
        messageArea.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                msgKeyPressed(evt);
            }

            private void msgKeyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    String tempMsg = messageArea.getText().replaceAll("Constants.TEACHER_IDENTIFIER", Constants.BLANK_STRING).trim();
                    if (!tempMsg.equalsIgnoreCase("") && !tempMsg.equalsIgnoreCase("\n")) {
                        tempMsg = messageArea.getText().replaceAll(Constants.LINE_BREAK, Constants.BLANK_STRING);
                        updateMainContentArea("Me: " + tempMsg, "blue", StaticData.isTeacher);
                        sendMessage();
                    }
                    messageArea.setText("");
                    messageArea.setCaretPosition(0);
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(this.usersPanel);
        add(mainContentScrollPane);
        add(messageArea);
        add(loadingPanel);
        add(scrollPane);
        scrollPane.setBounds(810, 5, 100, 390);
        mainContentScrollPane.setBounds(5, 5, 800, 390);
        mainContentArea.setBackground(Color.WHITE);
        messageArea.setBounds(5, 400, 904, 70);
        messageArea.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Type your message here."));
        appendText(mainContentArea, IRCIvelaClientStringUtils.singleton().showVersion());
        appendText(mainContentArea, "<b><font color='blue'>USER: </font>" + StaticData.nick + "</b> <b><font color='blue'>ROOM: </font></b>" + StaticData.channel + "<br />");
    }

    private JEditorPane appendText(JEditorPane tA, String text) {
        doc = (Document) tA.getDocument();
        try {
            ((HTMLEditorKit) tA.getEditorKit()).read(new java.io.StringReader(text), tA.getDocument(), tA.getDocument().getLength());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        tA.setCaretPosition(doc.getLength());
        return tA;
    }

    public void sendMessage() {
        String tempMsg = messageArea.getText().replaceAll("Constants.TEACHER_IDENTIFIER", Constants.BLANK_STRING);
        tempMsg = messageArea.getText().replaceAll(Constants.LINE_BREAK, Constants.BLANK_STRING);
        StaticData.clientMessage = tempMsg;
        Channel channel = session.getChannel(StaticData.channel);
        channel.say(StaticData.clientMessage);
        StaticData.chatMessage = IRCIvelaClientStringUtils.singleton().setMyMessage(StaticData.clientMessage);
        messageArea.setText(Constants.BLANK_STRING);
        messageArea.setFocusable(true);
    }

    public void sendBroadcastPrivateMessage(String msg) {
        Enumeration<String> nicks = this.userColorTable.keys();
        while (nicks.hasMoreElements()) {
            String nickTemp = nicks.nextElement();
            session.sayPrivate(nickTemp, msg);
        }
    }

    public void sendSystemMessage(String sysMsg) {
        if (session != null) {
            Channel channel = session.getChannel(StaticData.channel);
            channel.say(StaticData.clientMessage);
        }
    }

    public void setConnectedUsers() {
        Channel currentChannel = session.getChannel(StaticData.channel);
        List<String> connectedUsers = currentChannel.getNicks();
        List<String> activeUsers = new ArrayList<String>();
    }

    public void populateConnectedUsers(Channel currentChannel) {
        List<String> connectedUsers = currentChannel.getNicks();
        List<String> activeUsers = new ArrayList<String>();
        for (int l = 0; l < connectedUsers.size(); l++) {
            addUserTable(connectedUsers.get(l).trim());
        }
    }

    public void enableControls() {
        messageArea.setEnabled(true);
        messageArea.setEditable(true);
        messageArea.setAutoscrolls(true);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(false);
        messageArea.setText(Constants.BLANK_STRING);
    }

    public void updateMainContentArea(String msg, String color, boolean isTeacher) {
        mainContentArea.scrollRectToVisible(new Rectangle(0, mainContentArea.getBounds(null).height, 1, 1));
        appendText(mainContentArea, IRCIvelaClientStringUtils.singleton().setMessage(msg, row, color, isTeacher, false));
        row++;
        mainContentArea.setFocusable(true);
        mainContentArea.setVisible(true);
        mainContentArea.setEnabled(true);
    }

    public String getColor(String nick) {
        return (String) this.userColorTable.get(nick);
    }

    public void addUserTable(String nick) {
        if (userColorTable.contains(nick.trim())) return;
        if (StaticData.teacher.equalsIgnoreCase(nick.trim())) {
            userColorTable.put(nick.trim(), "red");
            this.usersPanel.addUser(nick.trim());
            return;
        }
        int pos = IRCIvelaClientStringUtils.generateRandomNumber(colors.length - 1);
        userColorTable.put(nick.trim(), colors[pos]);
        this.usersPanel.addUser(nick.trim());
    }

    public void removeUserTable(String nick) {
        userColorTable.remove(nick.trim());
        this.usersPanel.removeUser(nick.trim());
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void stopLoadingPanel() {
        this.loadingPanel.setLoadingFlag(false);
        this.remove(this.loadingPanel);
    }

    public void addUserHost(String ip) {
        this.usersHost.add(ip.trim());
    }

    public void removeUserHost(String ip) {
        this.usersHost.remove(ip.trim());
    }

    public ArrayList<String> getUserHost() {
        return this.usersHost;
    }
}

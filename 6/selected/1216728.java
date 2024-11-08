package net.zestflood.mapadeo;

import net.zestflood.mapadeo.friendlist.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.provider.ProviderManager;
import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.*;

/**
 * implements a simple chat client gui.
 */
public class Friendlist extends javax.swing.JFrame implements ConnectionListener {

    private ServiceDiscoveryManager SDM;

    Settings conf;

    RosterManager RosterMan;

    DefaultListModel RosterListModel;

    XMLIQExtensionListener IQListener;

    ChatListener ChatListener;

    Vector<ChatInstanceHandlerMUC> MUChats;

    XMPPConnection Connection;

    private void repaintInviteMenu() {
        this.inviteMenu.removeAll();
        Iterator<ChatInstanceHandlerMUC> it = this.MUChats.iterator();
        for (; it.hasNext(); ) {
            final ChatInstanceHandlerMUC temp2 = it.next();
            this.inviteMenu.add(new AbstractAction(temp2.getTitle()) {

                final ChatInstanceHandlerMUC temp = temp2;

                public void actionPerformed(ActionEvent e) {
                    temp.inviteUser(((Contact) RosterList.getSelectedValue()).getJID());
                }
            });
        }
    }

    /**
     * Gets called by ChatInstanceHandlerMUC to signal a new
     * whiteboard instance.
     * @param MUC
     */
    public void MUCadded(ChatInstanceHandlerMUC MUC) {
        this.MUChats.addElement(MUC);
        repaintInviteMenu();
    }

    /**
     * Gets called by ChatInstanceHandlerMUC to signal the end
     * of a whiteboard instance.
     * @param MUC
     */
    public void MUCremoved(ChatInstanceHandlerMUC MUC) {
        this.MUChats.remove(MUC);
        repaintInviteMenu();
    }

    /**
     * ConnectionListener method
     */
    @Override
    public void connectionClosed() {
        this.ConnectionStateLabel.setText("disconnected");
        disconnected();
    }

    /**
     * ConnectionListener method
     */
    @Override
    public void connectionClosedOnError(Exception e) {
        this.ConnectionStateLabel.setText("ConnectionError: " + e.toString());
        disconnected();
    }

    /**
     * ConnectionListener method
     */
    @Override
    public void reconnectingIn(int seconds) {
        this.ConnectionStateLabel.setText("reconnecting in " + seconds + " seconds");
    }

    /**
     * ConnectionListener method
     */
    @Override
    public void reconnectionFailed(Exception e) {
        this.ConnectionStateLabel.setText("reconnection failed: " + e.toString());
        disconnected();
    }

    /**
     * ConnectionListener method
     */
    @Override
    public void reconnectionSuccessful() {
        this.ConnectionStateLabel.setText("reconnected");
        connected();
    }

    private void connected() {
        this.RosterList.setEnabled(true);
        this.disconnect.setEnabled(true);
        this.connect.setEnabled(false);
        this.showRosterChanger.setEnabled(true);
    }

    private void disconnected() {
        this.RosterList.setEnabled(false);
        this.disconnect.setEnabled(false);
        this.connect.setEnabled(true);
        this.showRosterChanger.setEnabled(false);
    }

    void disconnect() {
        this.Connection.disconnect();
        this.ConnectionStateLabel.setText("disconnected");
        disconnected();
    }

    void connect() {
        if (!this.conf.get("XMPP.username").isEmpty()) {
            if (this.conf.get("XMPP.password").isEmpty()) {
                this.passwordDialog.setVisible(true);
            }
            connected();
            ConnectionConfiguration Configuration = new ConnectionConfiguration(this.conf.get("XMPP.server"), Integer.valueOf(this.conf.get("XMPP.port")));
            this.Connection = new XMPPConnection(Configuration);
            try {
                ServiceDiscoveryManager.setIdentityName("Mapadeo");
                ServiceDiscoveryManager.setIdentityType("pc");
                this.Connection.connect();
                SDM = ServiceDiscoveryManager.getInstanceFor(this.Connection);
                SDM.addFeature("http://mapadeo.zestflood.net/1.0/xhtml");
                this.ConnectionStateLabel.setText("authenticating");
                if (this.conf.get("XMPP.password").isEmpty()) {
                    this.Connection.login(this.conf.get("XMPP.username"), this.passwordTextField.getText(), this.conf.get("XMPP.location"));
                } else {
                    this.Connection.login(this.conf.get("XMPP.username"), this.conf.get("XMPP.password"), this.conf.get("XMPP.location"));
                }
                this.ConnectionStateLabel.setText("connected");
                if (this.Connection.isUsingTLS()) {
                    this.ConnectionStateLabel.setText("connected [TLS]");
                } else {
                    this.ConnectionStateLabel.setText("connected");
                }
                this.MUChats = new Vector<ChatInstanceHandlerMUC>();
                this.Connection.addConnectionListener(this);
                this.IQListener = new XMLIQExtensionListener(this.Connection, this);
                this.Connection.addPacketListener(IQListener, new PacketTypeFilter(XMLIQExtension.class));
                this.RosterListModel.clear();
                this.RosterMan = new RosterManager(this.Connection.getRoster(), this.RosterListModel);
                this.ChatListener = new ChatListener(this, Connection);
                this.Connection.getChatManager().addChatListener(this.ChatListener);
                MultiUserChat.addInvitationListener(Connection, ChatListener);
            } catch (XMPPException e) {
                this.ConnectionStateLabel.setText(e.getMessage());
                this.Connection.disconnect();
                disconnected();
            }
            this.passwordTextField.setText("");
        }
    }

    public Friendlist(Settings conf) {
        super("Mapadeo");
        this.conf = conf;
        org.jivesoftware.smack.XMPPConnection.DEBUG_ENABLED = false;
        Locale.setDefault(new Locale(conf.get("language"), conf.get("country")));
        this.RosterListModel = new javax.swing.DefaultListModel();
        ProviderManager.getInstance().addExtensionProvider("control", "http://mapadeo.zestflood.net/1.0/xhtml", new XMLControlExtensionProvider());
        ProviderManager.getInstance().addExtensionProvider("x", "http://mapadeo.zestflood.net/1.0/xhtml", new XMLExtensionProvider());
        ProviderManager.getInstance().addIQProvider("query", "http://mapadeo.zestflood.net/1.0/xhtml", XMLIQExtension.class);
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        passwordDialog = new javax.swing.JDialog();
        passwordTextField = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        RosterListContextMenu = new javax.swing.JPopupMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        createChatMenuItem = new javax.swing.JMenuItem();
        inviteMenu = new javax.swing.JMenu();
        ConnectionStateLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        RosterList = new javax.swing.JList();
        MenuBar = new javax.swing.JMenuBar();
        control = new javax.swing.JMenu();
        openSingleWhiteboard = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        connect = new javax.swing.JMenuItem();
        disconnect = new javax.swing.JMenuItem();
        showAccountManager = new javax.swing.JMenuItem();
        showRosterChanger = new javax.swing.JMenuItem();
        language = new javax.swing.JMenu();
        english = new javax.swing.JMenuItem();
        german = new javax.swing.JMenuItem();
        passwordDialog.setTitle("enter password");
        passwordDialog.setMinimumSize(new java.awt.Dimension(300, 120));
        passwordDialog.setModal(true);
        passwordTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passwordTextFieldActionPerformed(evt);
            }
        });
        jButton1.setText("ok");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout passwordDialogLayout = new javax.swing.GroupLayout(passwordDialog.getContentPane());
        passwordDialog.getContentPane().setLayout(passwordDialogLayout);
        passwordDialogLayout.setHorizontalGroup(passwordDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, passwordDialogLayout.createSequentialGroup().addContainerGap().addGroup(passwordDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE).addComponent(passwordTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 182, Short.MAX_VALUE)).addContainerGap()));
        passwordDialogLayout.setVerticalGroup(passwordDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(passwordDialogLayout.createSequentialGroup().addContainerGap().addComponent(passwordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jButton1).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("net/zestflood/mapadeo/friendlist/i10n");
        jMenuItem2.setText(bundle.getString("Friendlist.contextmenu.whiteboard_p2p"));
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        RosterListContextMenu.add(jMenuItem2);
        jMenuItem1.setText(bundle.getString("Friendlist.contextmenu.whiteboard_group"));
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        RosterListContextMenu.add(jMenuItem1);
        createChatMenuItem.setText("chat");
        createChatMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createChatMenuItemActionPerformed(evt);
            }
        });
        RosterListContextMenu.add(createChatMenuItem);
        inviteMenu.setText(bundle.getString("Friendlist.contextmenu.invite"));
        RosterListContextMenu.add(inviteMenu);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        ConnectionStateLabel.setText("offline");
        RosterList.setModel(RosterListModel);
        RosterList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        RosterList.setEnabled(false);
        RosterList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                RosterListMousePressed(evt);
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                RosterListMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(RosterList);
        control.setText(bundle.getString("Friendlist.control"));
        openSingleWhiteboard.setText(bundle.getString("Friendlist.control.openSingleWhiteboard"));
        openSingleWhiteboard.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openSingleWhiteboardActionPerformed(evt);
            }
        });
        control.add(openSingleWhiteboard);
        control.add(jSeparator1);
        connect.setText(bundle.getString("Friendlist.control.connect"));
        connect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectActionPerformed(evt);
            }
        });
        control.add(connect);
        disconnect.setText(bundle.getString("Friendlist.control.disconnect"));
        disconnect.setEnabled(false);
        disconnect.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectActionPerformed(evt);
            }
        });
        control.add(disconnect);
        showAccountManager.setText(bundle.getString("Friendlist.control.accmanager"));
        showAccountManager.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showAccountManagerActionPerformed(evt);
            }
        });
        control.add(showAccountManager);
        showRosterChanger.setText(bundle.getString("Friendlist.control.rosterchanger"));
        showRosterChanger.setEnabled(false);
        showRosterChanger.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showRosterChangerActionPerformed(evt);
            }
        });
        control.add(showRosterChanger);
        MenuBar.add(control);
        language.setText(bundle.getString("Friendlist.language"));
        english.setText("english");
        english.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                englishActionPerformed(evt);
            }
        });
        language.add(english);
        german.setText("deutsch");
        german.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                germanActionPerformed(evt);
            }
        });
        language.add(german);
        MenuBar.add(language);
        setJMenuBar(MenuBar);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE).addComponent(ConnectionStateLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(ConnectionStateLabel)));
        pack();
    }

    private void connectActionPerformed(java.awt.event.ActionEvent evt) {
        final Friendlist cur = this;
        java.awt.EventQueue.invokeLater(new Runnable() {

            Friendlist rec = cur;

            public void run() {
                rec.connect();
            }
        });
    }

    private void disconnectActionPerformed(java.awt.event.ActionEvent evt) {
        this.disconnect();
    }

    private void showAccountManagerActionPerformed(java.awt.event.ActionEvent evt) {
        net.zestflood.mapadeo.friendlist.AccountManager AM = new net.zestflood.mapadeo.friendlist.AccountManager(this, this.conf);
        AM.setVisible(true);
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        this.passwordDialog.setVisible(false);
    }

    private void passwordTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {
        initWhiteBoard temp = new initWhiteBoard(this, false, (Contact) this.RosterListModel.get(this.RosterList.getSelectedIndex()), this.SDM, this.IQListener, this.Connection);
        temp.setVisible(true);
    }

    private void RosterListMousePressed(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            int index = RosterList.getSelectedIndex();
            if (index >= 0) {
                this.repaintInviteMenu();
                RosterListContextMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }

    private void RosterListMouseReleased(java.awt.event.MouseEvent evt) {
        if (evt.isPopupTrigger()) {
            int index = RosterList.getSelectedIndex();
            if (index >= 0) {
                this.repaintInviteMenu();
                RosterListContextMenu.show(evt.getComponent(), evt.getX(), evt.getY());
            }
        }
    }

    private void englishActionPerformed(java.awt.event.ActionEvent evt) {
        Locale.setDefault(new Locale("en", "US"));
        conf.set("language", "en");
        conf.set("country", "US");
        conf.save();
    }

    private void germanActionPerformed(java.awt.event.ActionEvent evt) {
        Locale.setDefault(new Locale("de", "DE"));
        conf.set("language", "de");
        conf.set("country", "DE");
        conf.save();
    }

    private void showRosterChangerActionPerformed(java.awt.event.ActionEvent evt) {
        RosterChanger temp = new RosterChanger(this, false, this.Connection.getRoster(), this.Connection);
        temp.setVisible(true);
    }

    private void createChatMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        ChatWindow newWindow = new ChatWindow(this, this.ChatListener);
        Chat newChat = this.Connection.getChatManager().createChat(((Contact) RosterList.getSelectedValue()).getJID(), newWindow);
        newWindow.setChat(newChat);
        newWindow.setVisible(true);
    }

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {
        ChatInstanceHandlerMUC newMUCGUI = new ChatInstanceHandlerMUC(this.Connection, this);
        newMUCGUI.create();
        newMUCGUI.inviteUser(((Contact) RosterList.getSelectedValue()).getJID());
    }

    private void openSingleWhiteboardActionPerformed(java.awt.event.ActionEvent evt) {
        ChatInstanceHandlerSingle single = new ChatInstanceHandlerSingle();
    }

    private javax.swing.JLabel ConnectionStateLabel;

    private javax.swing.JMenuBar MenuBar;

    private javax.swing.JList RosterList;

    private javax.swing.JPopupMenu RosterListContextMenu;

    private javax.swing.JMenuItem connect;

    private javax.swing.JMenu control;

    private javax.swing.JMenuItem createChatMenuItem;

    private javax.swing.JMenuItem disconnect;

    private javax.swing.JMenuItem english;

    private javax.swing.JMenuItem german;

    private javax.swing.JMenu inviteMenu;

    private javax.swing.JButton jButton1;

    private javax.swing.JMenuItem jMenuItem1;

    private javax.swing.JMenuItem jMenuItem2;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JMenu language;

    private javax.swing.JMenuItem openSingleWhiteboard;

    private javax.swing.JDialog passwordDialog;

    private javax.swing.JTextField passwordTextField;

    private javax.swing.JMenuItem showAccountManager;

    private javax.swing.JMenuItem showRosterChanger;
}

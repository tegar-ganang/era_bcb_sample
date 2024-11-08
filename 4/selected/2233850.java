package com.scottandjoe.texasholdem.gui;

import com.scottandjoe.texasholdem.gameplay.Player;
import com.scottandjoe.texasholdem.misc.KillableThread;
import com.scottandjoe.texasholdem.networking.EncryptedMessageReader;
import com.scottandjoe.texasholdem.networking.EncryptedMessageWriter;
import com.scottandjoe.texasholdem.networking.Message;
import com.scottandjoe.texasholdem.networking.MessageReader;
import com.scottandjoe.texasholdem.networking.MessageWriter;
import com.scottandjoe.texasholdem.misc.Settings;
import com.scottandjoe.texasholdem.networking.LobbyServer;
import com.scottandjoe.texasholdem.networking.EMSCorruptedException;
import com.scottandjoe.texasholdem.networking.EMSException;
import com.scottandjoe.texasholdem.networking.EMSExceptionHandler;
import com.scottandjoe.texasholdem.networking.NetworkUtilities;
import com.scottandjoe.texasholdem.resources.Utilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import javax.swing.ImageIcon;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Calendar;
import javax.crypto.NoSuchPaddingException;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A client-side GUI that connects to a LobbyServer.
 *
 *
 * @author Scott DellaTorre
 * @author Joe Stein
 * @see LobbyServer
 */
public class LobbyWindow extends JFrame implements ActionListener, EMSExceptionHandler {

    private static final int EXPECTED = 0;

    private static final String FOCUS_CHECK_ACTION = "FocusCheck";

    private static final int FOCUS_CHECK_DELAY = 500;

    private static final String HOST_STR = " (Host)";

    private static final String KICK_USER_ACTION = "KickUser";

    private static final int LEAVE_LOBBY = 0;

    private static final String NEW_CHAT_TITLE = "NEW MESSAGE";

    private static final String OBSERVING_STR = " (Observing)";

    private static final String READY_STR = " (Ready)";

    private static final int UNEXPECTED = 1;

    private static final int QUIT = 1;

    private DefaultListModel dlm;

    private boolean dcxn = false;

    private Timer focusCheckTimer;

    private boolean hasFocus = true;

    private LobbyWindow lobbyWindow = this;

    private boolean newChat = false;

    private int numOfReady = 0;

    private int playerCount = 0;

    private JPopupMenu popupMenu = null;

    private ServerReaderThread serverReaderThread;

    private Settings settings;

    private LobbyWindowSwingSafe swingSafe = new LobbyWindowSwingSafe(this);

    private boolean waitForPlayers;

    private EncryptedMessageWriter writer;

    /** Creates a new LobbyWindow based on given connection parameters.
    * @param ip Host IP.
    * @param port Port to connect to on host IP.
    * @param settings.getUserName() This client's settings.getUserName().
    * @param canLeave If the user can leave the room or not. Hosts should obviously not be allowed to leave.
    * @see LobbyServer
    */
    public LobbyWindow(Settings settings) {
        this.settings = settings;
        Utilities.setUIManager();
        initComponents();
        addWindowListener(new LobbyWindowListener());
        if (settings.getUserType() == Settings.HOST) {
            popupMenu = new JPopupMenu();
            JMenuItem menuItem = new JMenuItem("Kick");
            menuItem.addActionListener(this);
            menuItem.setActionCommand(KICK_USER_ACTION);
            popupMenu.add(menuItem);
        }
        addWindowFocusListener(new ChatFocusListener());
        dlm = (DefaultListModel) playerList.getModel();
        getRootPane().setDefaultButton(sendBtn);
        readyBtn.setVisible(false);
        try {
            serverReaderThread = new ServerReaderThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverReaderThread.start();
        focusCheckTimer = new Timer(FOCUS_CHECK_DELAY, this);
        focusCheckTimer.setActionCommand(FOCUS_CHECK_ACTION);
        focusCheckTimer.setInitialDelay(0);
        focusCheckTimer.start();
        chatField.requestFocus();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getActionCommand().equals(FOCUS_CHECK_ACTION)) {
            if (!hasFocus && newChat) {
                if (getTitle().equals(NEW_CHAT_TITLE)) {
                    setTitle("");
                } else {
                    setTitle(NEW_CHAT_TITLE);
                }
            }
        } else if (evt.getActionCommand().equals(KICK_USER_ACTION)) {
            String name = (String) ((Object[]) playerList.getSelectedValue())[0];
            int result = JOptionPane.showConfirmDialog(this, "Do you want to ban the user's IP address?", "Kick " + name, JOptionPane.YES_NO_CANCEL_OPTION);
            if (result != JOptionPane.CANCEL_OPTION) {
                String content;
                if (result == JOptionPane.YES_OPTION) {
                    content = "KickAndBan";
                } else {
                    content = "Kick";
                }
                Utilities.log(Utilities.LOG_OUTPUT, "Kicking " + name + " from lobby.");
                NetworkUtilities.sendMessage(writer, new Message(Message.Type.KICK_USER, name, content), false);
                Utilities.log(Utilities.LOG_OUTPUT, "Kicked " + name + " from lobby.");
            }
        }
    }

    private void confirmWindowClose(int option) {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to quit?", "Leaving Game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            swingSafe.leaveLobby(true, true);
            if (option == LEAVE_LOBBY) {
                new TableSelectWindow(settings, null);
            }
        }
    }

    public synchronized void handleException(EMSException emse) {
        if (!dcxn) {
            Utilities.log(Utilities.LOG_OUTPUT, "Server disconnected unexpectedly.");
            swingSafe.serverDcxn(UNEXPECTED);
        }
    }

    private class ChatFocusListener implements WindowFocusListener {

        public void windowGainedFocus(WindowEvent e) {
            hasFocus = true;
            newChat = false;
            setTitle("");
        }

        public void windowLostFocus(WindowEvent e) {
            hasFocus = false;
        }
    }

    private class ImageRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ((JLabel) c).setText((String) ((Object[]) value)[0] + "\n" + (String) ((Object[]) value)[2]);
            ((JLabel) c).setIcon((ImageIcon) ((Object[]) value)[1]);
            ((JLabel) c).setEnabled(true);
            if (index == 0) {
                ((JLabel) c).setFont(new Font(((JLabel) c).getFont().getFamily(), Font.BOLD, ((JLabel) c).getFont().getSize()));
            }
            if (isSelected) {
                ((JLabel) c).setBackground(Color.BLACK);
            } else if (index % 2 == 0) {
                ((JLabel) c).setBackground(Color.LIGHT_GRAY);
            }
            return c;
        }
    }

    private class LobbyWindowListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (dcxn) {
                swingSafe.leaveLobby(true, false);
            } else {
                confirmWindowClose(LEAVE_LOBBY);
            }
        }
    }

    /** A subclass of KillableThread to listen for messages to the server.
    * <p>
    * ServerReader reads messages and evaluates them to determine the affect they should have on the GUI and
    * the game.
    *
    */
    private class ServerReaderThread extends KillableThread {

        private EncryptedMessageReader reader;

        private Socket socket;

        private Message mes = null;

        private ServerReaderThread() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
            super("Lobby Window Server Reader Thread");
            Utilities.log(Utilities.LOG_OUTPUT, "Creating socket to " + settings.getHostIP() + ":" + settings.getHostPort());
            try {
                socket = new Socket(settings.getHostIP(), settings.getHostPort());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            Utilities.log(Utilities.LOG_OUTPUT, "Socket created successfully.");
            try {
                reader = new EncryptedMessageReader(new MessageReader(socket.getInputStream()), "AES", Integer.MAX_VALUE, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void closeReader() throws IOException {
            reader.close();
        }

        public void doRun() {
            try {
                mes = reader.readMessage();
                Utilities.logParcial(Utilities.LOG_OUTPUT, "Message received from server: ");
                if (mes.getType() == Message.Type.ASK_TO_SPECTATE) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Asking user to spectate.");
                    int response = swingSafe.showConfirmDialog("The lobby has reached the max amount of players.\n" + "Would you like to spectate?", "Max Players Reached", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (response == JOptionPane.YES_OPTION) {
                        Utilities.log(Utilities.LOG_OUTPUT, "User will spectate.");
                        settings.setObserving(true);
                        NetworkUtilities.sendMessage(writer, new Message(Message.Type.ASK_TO_SPECTATE, settings.getUserName(), true), false);
                        NetworkUtilities.sendMessage(writer, new Message(Message.Type.JOINED_LOBBY, settings.getUserName(), settings.getUserImage()), false);
                    } else {
                        Utilities.log(Utilities.LOG_OUTPUT, "User will not spectate, leaving lobby.");
                        swingSafe.createNewTableSelectWindow(settings, null);
                        swingSafe.leaveLobby(false, false);
                    }
                } else if (mes.getType() == Message.Type.CHAT) {
                    if (!hasFocus) {
                        newChat = true;
                    }
                    Utilities.log(Utilities.LOG_OUTPUT, "Chat from " + mes.getName());
                    Calendar today = Calendar.getInstance();
                    String meridiem;
                    if (today.get(Calendar.AM_PM) == Calendar.AM) {
                        meridiem = "AM";
                    } else {
                        meridiem = "PM";
                    }
                    String hour, minute, second;
                    hour = String.valueOf(today.get(Calendar.HOUR));
                    if (hour.equals("0")) {
                        hour = "12";
                    }
                    minute = String.valueOf(today.get(Calendar.MINUTE));
                    second = String.valueOf(today.get(Calendar.SECOND));
                    if (today.get(Calendar.MINUTE) < 10) {
                        minute = "0" + minute;
                    }
                    if (today.get(Calendar.SECOND) < 10) {
                        second = "0" + second;
                    }
                    String time = hour + ":" + minute + ":" + second + " " + meridiem;
                    swingSafe.appendToChat(mes.getName() + " (" + time + "): " + (String) mes.getContent());
                } else if (mes.getType() == Message.Type.CONNECTED) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Connected");
                    reader.reInitialize("AES", Integer.MAX_VALUE, settings.getSecretKey());
                    writer = new EncryptedMessageWriter(lobbyWindow, new MessageWriter(socket.getOutputStream()), "RSA", 117, (PublicKey) mes.getContent());
                    NetworkUtilities.sendMessageAndWait(writer, new Message(Message.Type.REQUEST_HEADER, settings.getUserName(), settings.isObserving(), settings.getSecretKey()), true);
                    writer.reInitialize("AES", Integer.MAX_VALUE, settings.getSecretKey());
                    NetworkUtilities.sendMessage(writer, new Message(Message.Type.REQUEST_CLIENT_INFO, settings.getUserName(), "reqNames"), false);
                } else if (mes.getType() == Message.Type.CONNECTION_DENIED) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Connection denied");
                    swingSafe.createNewTableSelectWindow(settings, new String[] { mes.getName(), "Couldn't Connect to Server" });
                    swingSafe.leaveLobby(false, false);
                } else if (mes.getType() == Message.Type.DELIVER_CLIENT_INFO) {
                    Serializable[] content = (Serializable[]) mes.getContent();
                    for (Serializable player : content) {
                        if (player != null) {
                            Serializable[] playerInfo = (Serializable[]) player;
                            swingSafe.addPlayer((String) playerInfo[0], (ImageIcon) playerInfo[1], (String) playerInfo[2]);
                        }
                    }
                    Utilities.log(Utilities.LOG_OUTPUT, "Client info delivered");
                    NetworkUtilities.sendMessage(writer, new Message(Message.Type.JOINED_LOBBY, settings.getUserName(), settings.getUserImage()), false);
                } else if (mes.getType() == Message.Type.JOINED_LOBBY) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Member " + mes.getName() + " joined lobby");
                    Object[] content = (Object[]) mes.getContent();
                    swingSafe.addPlayer(mes.getName(), (ImageIcon) content[0], (String) content[1]);
                } else if (mes.getType() == Message.Type.KICK_USER) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Kicked from lobby");
                    swingSafe.createNewTableSelectWindow(settings, new String[] { (String) mes.getContent(), "Kicked From Lobby" });
                    swingSafe.leaveLobby(false, true);
                } else if (mes.getType() == Message.Type.LEFT_LOBBY) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Member " + mes.getName() + " left lobby");
                    swingSafe.removePlayer(mes.getName());
                } else if (mes.getType() == Message.Type.READY) {
                    boolean ready = ((String) mes.getContent()).equals("true");
                    Utilities.logParcial(Utilities.LOG_OUTPUT, mes.getName() + " ready: " + ready);
                    swingSafe.setPlayerReady(mes.getName(), ready);
                } else if (mes.getType() == Message.Type.ROOM_CLOSED) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Room closed");
                    swingSafe.serverDcxn(EXPECTED);
                } else if (mes.getType() == Message.Type.ROOM_LOCKED) {
                    if (((String) mes.getContent()).equals("true")) {
                        Utilities.log(Utilities.LOG_OUTPUT, "Room locked");
                        if (waitForPlayers) {
                            swingSafe.allowReady(true);
                            swingSafe.setStatus("Waiting for players to be ready..." + "\n" + numOfReady + "/" + playerCount + " players ready.");
                        } else {
                            swingSafe.setStatus("Waiting for host to start game...");
                        }
                    } else {
                        Utilities.log(Utilities.LOG_OUTPUT, "Room unlocked");
                        swingSafe.allowReady(false);
                        if (swingSafe.getReadyButtonText().equals("Not Ready")) {
                            NetworkUtilities.sendMessage(writer, new Message(Message.Type.READY, settings.getUserName(), "false"), false);
                        }
                        swingSafe.setReadyButtonText("Ready");
                        swingSafe.setStatus("Waiting for more players...");
                    }
                } else if (mes.getType() == Message.Type.SERVER_INFO) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Server info delivered");
                    swingSafe.setNameLabelText(mes.getName() + " Lobby");
                    waitForPlayers = (Boolean) mes.getContent();
                    if (waitForPlayers && !settings.isObserving()) {
                        swingSafe.setReadyButtonVisible(true);
                    }
                } else if (mes.getType() == Message.Type.STARTING_GAME) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Starting game");
                    Serializable[] content = (Serializable[]) mes.getContent();
                    Player[] players = new Player[content.length];
                    for (int i = 0; i < players.length; i++) {
                        Serializable[] playerInfo = (Serializable[]) content[i];
                        players[i] = new Player((String) playerInfo[0], (ImageIcon) playerInfo[1], (Integer) playerInfo[2]);
                    }
                    swingSafe.createNewGameWindow(settings, players, reader, writer);
                    swingSafe.leaveLobby(false, false);
                } else if (mes.getType() == Message.Type.STATISTICS_UPDATE) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Status update - " + mes.getContent());
                    swingSafe.setStatus((String) mes.getContent());
                } else if (mes.getType() == Message.Type.USERNAME_TAKEN) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Username taken");
                    swingSafe.newUsername();
                } else {
                    Utilities.log(Utilities.LOG_OUTPUT, mes);
                }
            } catch (EMSCorruptedException emsce) {
                handleException(emsce);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void postDeath() {
        }

        public void preDeath() {
        }

        public void preRun() {
        }
    }

    private class LobbyWindowSwingSafe extends SwingSafe {

        private String getReadyButtonTextReturn;

        private LobbyWindowSwingSafe(LobbyWindow parent) {
            super(parent);
        }

        private void addPlayer(final String name, final ImageIcon image, final String info) {
            if (!info.contains(OBSERVING_STR)) {
                playerCount++;
            }
            invokeLater(new Runnable() {

                public void run() {
                    dlm.addElement(new Object[] { name, image, info });
                }
            });
        }

        private void allowReady(final boolean enabled) {
            invokeLater(new Runnable() {

                public void run() {
                    readyBtn.setEnabled(enabled);
                }
            });
        }

        private void appendToChat(final String text) {
            invokeLater(new Runnable() {

                public void run() {
                    chatArea.setText(chatArea.getText() + text + "\n");
                    chatArea.setCaretPosition(chatArea.getText().length());
                }
            });
        }

        private void createNewGameWindow(final Settings settings, final Player[] players, final EncryptedMessageReader reader, final EncryptedMessageWriter writer) {
            invokeLater(new Runnable() {

                public void run() {
                    new GameWindow(settings, players, reader, writer);
                }
            });
        }

        private void createNewTableSelectWindow(final Settings settings, final String[] message) {
            invokeLater(new Runnable() {

                public void run() {
                    new TableSelectWindow(settings, message);
                }
            });
        }

        private String getReadyButtonText() {
            try {
                SwingSafe.invokeAndWait(new Runnable() {

                    public void run() {
                        getReadyButtonTextReturn = readyBtn.getText();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return getReadyButtonTextReturn;
        }

        private void leaveLobby(boolean closeReader, boolean writeMessage) {
            invokeLater(new Runnable() {

                public void run() {
                    setVisible(false);
                }
            });
            if (!dcxn) {
                dcxn = true;
                if (writeMessage) {
                    try {
                        NetworkUtilities.sendMessageAndWait(writer, new Message(Message.Type.LEFT_LOBBY, settings.getUserName(), "leftLobby"), false);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
                focusCheckTimer.stop();
                if (closeReader) {
                    try {
                        serverReaderThread.closeReader();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                serverReaderThread.kill();
            }
            invokeLater(new Runnable() {

                public void run() {
                    dispose();
                }
            });
        }

        private void newUsername() {
            String newUsername = showInputDialog("That username is already in use.\n" + "Please enter a different one:", "Username In Use", JOptionPane.INFORMATION_MESSAGE);
            if (newUsername == null) {
                createNewTableSelectWindow(settings, null);
                leaveLobby(false, false);
            } else if (newUsername.length() > 0) {
                NetworkUtilities.sendMessage(writer, new Message(Message.Type.JOINED_LOBBY, newUsername, settings.getUserImage()), false);
                settings.setUserName(newUsername);
            } else {
                newUsername();
            }
        }

        private void removePlayer(final String name) {
            invokeLater(new Runnable() {

                public void run() {
                    for (int i = 0; i < dlm.size(); i++) {
                        Object[] content = (Object[]) dlm.elementAt(i);
                        if (((String) content[0]).equals(name)) {
                            if (!((String) content[2]).contains(OBSERVING_STR)) {
                                playerCount--;
                            }
                            dlm.remove(i);
                        }
                    }
                }
            });
        }

        private void serverDcxn(final int dcxnType) {
            dcxn = true;
            focusCheckTimer.stop();
            serverReaderThread.kill();
            try {
                serverReaderThread.closeReader();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            invokeLater(new Runnable() {

                public void run() {
                    chatArea.setEnabled(false);
                    chatField.setEnabled(false);
                    dlm.removeAllElements();
                    playerList.setEnabled(false);
                    readyBtn.setEnabled(false);
                    sendBtn.setEnabled(false);
                    if (dcxnType == EXPECTED) {
                        statusArea.setText("Room closed.");
                        JOptionPane.showMessageDialog(parent, "Host closed room.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                    } else if (dcxnType == UNEXPECTED) {
                        statusArea.setText("Disconnected.");
                        JOptionPane.showMessageDialog(parent, "Unexpectedly disconnected from server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }

        private void setNameLabelText(final String text) {
            invokeLater(new Runnable() {

                public void run() {
                    nameLabel.setText(text);
                }
            });
        }

        private void setPlayerReady(final String name, final boolean ready) {
            invokeLater(new Runnable() {

                public void run() {
                    for (int i = 0; i < dlm.size(); i++) {
                        Object[] content = (Object[]) dlm.get(i);
                        String currName = ((String) content[0]);
                        if (currName.equals(name)) {
                            if (ready) {
                                content[2] = ((String) content[2]) + READY_STR;
                            } else {
                                content[2] = ((String) content[2]).replace(READY_STR, "");
                            }
                            dlm.set(i, content);
                        }
                    }
                    if (ready) {
                        numOfReady++;
                    } else {
                        numOfReady--;
                    }
                    if (readyBtn.isEnabled()) {
                        statusArea.setText("Waiting for players to be ready...\n" + numOfReady + "/" + playerCount + " players ready.");
                    }
                }
            });
        }

        private void setReadyButtonText(final String text) {
            invokeLater(new Runnable() {

                public void run() {
                    readyBtn.setText(text);
                }
            });
        }

        private void setReadyButtonVisible(final boolean visible) {
            invokeLater(new Runnable() {

                public void run() {
                    readyBtn.setVisible(visible);
                }
            });
        }

        private void setStatus(final String text) {
            invokeLater(new Runnable() {

                public void run() {
                    statusArea.setText(text);
                }
            });
        }
    }

    /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        nameLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        playerList = new javax.swing.JList();
        jLabel4 = new javax.swing.JLabel();
        statusArea = new javax.swing.JTextArea();
        readyBtn = new javax.swing.JButton();
        chatField = new javax.swing.JTextField();
        sendBtn = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        chatArea = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        leaveLobbyMenuItem = new javax.swing.JMenuItem();
        quitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        clearChatMenuItem = new javax.swing.JMenuItem();
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        nameLabel.setFont(new java.awt.Font("Lucida Grande", 1, 24));
        nameLabel.setText("*Name* Lobby");
        jLabel3.setFont(new java.awt.Font("Lucida Grande", 0, 14));
        jLabel3.setText("Game Status:");
        playerList.setModel(new DefaultListModel());
        playerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        playerList.setCellRenderer(new ImageRenderer());
        playerList.setRequestFocusEnabled(false);
        playerList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                playerListMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(playerList);
        jLabel4.setFont(new java.awt.Font("Lucida Grande", 0, 14));
        jLabel4.setText("Players:");
        statusArea.setColumns(20);
        statusArea.setEditable(false);
        statusArea.setRows(5);
        statusArea.setText("Waiting for more players...");
        statusArea.setAutoscrolls(false);
        statusArea.setFocusable(false);
        statusArea.setRequestFocusEnabled(false);
        readyBtn.setText("Ready");
        readyBtn.setEnabled(false);
        readyBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                readyBtnActionPerformed(evt);
            }
        });
        chatField.setDocument(new LimitedDocument(100));
        chatField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                chatFieldKeyPressed(evt);
            }

            public void keyReleased(java.awt.event.KeyEvent evt) {
                chatFieldKeyReleased(evt);
            }
        });
        sendBtn.setText("Send");
        sendBtn.setEnabled(false);
        sendBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendBtnActionPerformed(evt);
            }
        });
        chatArea.setColumns(20);
        chatArea.setEditable(false);
        chatArea.setRows(5);
        chatArea.setWrapStyleWord(true);
        jScrollPane2.setViewportView(chatArea);
        fileMenu.setText("File");
        leaveLobbyMenuItem.setText("Leave Lobby");
        leaveLobbyMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leaveLobbyMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(leaveLobbyMenuItem);
        quitMenuItem.setText("Quit");
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(quitMenuItem);
        jMenuBar1.add(fileMenu);
        editMenu.setText("Edit");
        clearChatMenuItem.setText("Clear Chat");
        clearChatMenuItem.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearChatMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(clearChatMenuItem);
        jMenuBar1.add(editMenu);
        setJMenuBar(jMenuBar1);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(66, 66, 66).add(jLabel4).add(165, 165, 165).add(jLabel3)).add(layout.createSequentialGroup().addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 182, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().add(statusArea, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 312, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(readyBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 104, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(layout.createSequentialGroup().add(chatField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 371, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(sendBtn)).add(jScrollPane2))).add(nameLabel)))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(nameLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel4).add(jLabel3)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(readyBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE).add(statusArea, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(chatField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(sendBtn))).add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)).addContainerGap()));
        pack();
    }

    private void readyBtnActionPerformed(java.awt.event.ActionEvent evt) {
        if (readyBtn.getText().equals("Ready")) {
            NetworkUtilities.sendMessage(writer, new Message(Message.Type.READY, settings.getUserName(), "true"), false);
            readyBtn.setText("Not Ready");
        } else if (readyBtn.getText().equals("Not Ready")) {
            NetworkUtilities.sendMessage(writer, new Message(Message.Type.READY, settings.getUserName(), "false"), false);
            readyBtn.setText("Ready");
        }
    }

    private void sendBtnActionPerformed(java.awt.event.ActionEvent evt) {
        NetworkUtilities.sendMessage(writer, new Message(Message.Type.CHAT, settings.getUserName(), chatField.getText()), false);
        chatField.setText("");
        sendBtn.setEnabled(false);
    }

    private void chatFieldKeyReleased(java.awt.event.KeyEvent evt) {
        if (chatField.getText().equals("")) {
            sendBtn.setEnabled(false);
        } else {
            sendBtn.setEnabled(true);
        }
    }

    private void chatFieldKeyPressed(java.awt.event.KeyEvent evt) {
        if (chatField.getText().equals("") && evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            evt.consume();
        }
    }

    private void clearChatMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        chatArea.setText("");
    }

    private void leaveLobbyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        confirmWindowClose(LEAVE_LOBBY);
    }

    private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {
        confirmWindowClose(QUIT);
    }

    private void playerListMouseReleased(java.awt.event.MouseEvent evt) {
        if (SwingUtilities.isRightMouseButton(evt) && popupMenu != null) {
            int index = playerList.locationToIndex(evt.getPoint());
            if (index != -1) {
                playerList.setSelectedIndex(index);
                Object[] content = (Object[]) dlm.getElementAt(playerList.getSelectedIndex());
                if (!((String) content[2]).contains(HOST_STR)) {
                    popupMenu.show(playerList, evt.getX(), evt.getY());
                }
            }
        }
    }

    private javax.swing.JTextArea chatArea;

    private javax.swing.JTextField chatField;

    private javax.swing.JMenuItem clearChatMenuItem;

    private javax.swing.JMenu editMenu;

    private javax.swing.JMenu fileMenu;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JMenuBar jMenuBar1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JMenuItem leaveLobbyMenuItem;

    private javax.swing.JLabel nameLabel;

    private javax.swing.JList playerList;

    private javax.swing.JMenuItem quitMenuItem;

    private javax.swing.JButton readyBtn;

    private javax.swing.JButton sendBtn;

    private javax.swing.JTextArea statusArea;
}

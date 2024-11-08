package com.usoog.hextd.UI;

import com.usoog.commons.gamecore.DefaultUser;
import com.usoog.commons.gamecore.User;
import com.usoog.commons.gamecore.UserInfo;
import com.usoog.commons.gamecore.exception.FailedToLoadReplayException;
import com.usoog.commons.gamecore.exception.NotReadyYetException;
import com.usoog.commons.gamecore.gameloop.GliMpCatchup;
import com.usoog.commons.gamecore.gamemanager.GameListener;
import com.usoog.commons.gamecore.gamephase.FactoryGamePhase.DefaultGamePhaseName;
import com.usoog.commons.gamecore.gamephase.GamePhase;
import com.usoog.commons.gamecore.message.GameInfo;
import com.usoog.commons.gamecore.message.MessageError;
import com.usoog.commons.gamecore.message.MessageFetch;
import com.usoog.commons.gamecore.message.MessageGameCreate;
import com.usoog.commons.gamecore.message.MessageGameJoin;
import com.usoog.commons.gamecore.message.MessageGameList;
import com.usoog.commons.gamecore.message.MessageMapData;
import com.usoog.commons.gamecore.message.MessageMapList;
import com.usoog.commons.gamecore.message.MessageMapLoad;
import com.usoog.commons.gamecore.message.MessagePing;
import com.usoog.commons.gamecore.message.MessagePlayerForfeit;
import com.usoog.commons.gamecore.message.MessagePong;
import com.usoog.commons.gamecore.message.MessageReplay;
import com.usoog.commons.gamecore.message.MessageSay;
import com.usoog.commons.gamecore.message.MessageUnknown;
import com.usoog.commons.gamecore.message.MessageUserAuth;
import com.usoog.commons.gamecore.message.MessageUserInGame;
import com.usoog.commons.gamecore.message.MessageUserInfo;
import com.usoog.commons.gamecore.message.MessageUserKick;
import com.usoog.commons.gamecore.message.MessageUserList;
import com.usoog.commons.gamecore.player.LocalPlayerListener;
import com.usoog.commons.network.ConnectionListener;
import com.usoog.commons.network.MessageListener;
import com.usoog.commons.network.NetworkClient;
import com.usoog.commons.network.NetworkConnection;
import com.usoog.commons.network.NetworkServerConnection;
import com.usoog.commons.network.message.ActionMessage;
import com.usoog.hextd.Constants.settingKey;
import com.usoog.hextd.Constants;
import com.usoog.hextd.core.GameStateImplementation;
import com.usoog.hextd.core.HexTDPlayer;
import com.usoog.hextd.util.HTMLEncode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import com.usoog.commons.network.message.Message;
import com.usoog.tdcore.message.MessageSettings;
import com.usoog.hextd.HexTD;
import com.usoog.hextd.core.GameGridImplementation;
import com.usoog.hextd.core.GameManagerImplementation;
import com.usoog.hextd.core.MapLoaderClient;
import com.usoog.hextd.hex.HexTile;
import com.usoog.hextd.util.Cache;
import com.usoog.tdcore.message.TDActionMessage;
import com.usoog.tdcore.message.TDFactoryMessage;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.SocketAddress;

public class PanelNetConnection extends javax.swing.JPanel implements ConnectionListener, MessageListener, NetworkConnection, GameListener<HexTile, HexTDPlayer, GameGridImplementation, GameManagerImplementation, GameStateImplementation>, LocalPlayerListener<HexTile, HexTDPlayer, GameGridImplementation, GameManagerImplementation, GameStateImplementation> {

    /**
	 * Commands are used to encapsulate code into something that can be put in a
	 * Map
	 *
	 * TODO: set back to private after DataNucleus fixes it's schemaTool
	 */
    public interface MessageCommand {

        public void execute(Message message);
    }

    private Map<String, MessageCommand> messageCommands = new HashMap<String, MessageCommand>();

    private HexTD mainGame;

    private GameManagerImplementation gameManager;

    private StringBuilder htmlContent;

    private NetworkClient client;

    private String serverAddress;

    private int serverPort;

    private TDFactoryMessage messageFactory;

    private Constants.AuthStatus authStatus = Constants.AuthStatus.unknown;

    private List<GameInfo> games = new ArrayList<GameInfo>();

    private Map<Integer, GameInfo> gamesById = new HashMap<Integer, GameInfo>();

    private List<UserInfo> users = new ArrayList<UserInfo>();

    private Map<Integer, UserInfo> usersByUserId = new HashMap<Integer, UserInfo>();

    private Map<Integer, UserInfo> usersByPlayerId = new HashMap<Integer, UserInfo>();

    private Integer channelId = -1;

    private String channelName;

    private final UserInfo localUserInfo = new UserInfo();

    private User localUser = new DefaultUser(localUserInfo);

    private int playerGameId;

    private String status = "Not connected to server.";

    private Timer pingTimer;

    private TimerTask pingTask;

    private HTMLEditorKit editorkit;

    private PlayerListRenderer plr;

    private Message sendLater;

    private boolean keepTextBar = true;

    private boolean isConnected = false;

    private boolean wasConnected = false;

    private boolean keepFullLog = false;

    private StringBuilder fullLog = new StringBuilder();

    private MapLoaderClient mapLoader;

    /** Creates new form PanelNetConnection */
    public PanelNetConnection() {
        gamesById = new HashMap<Integer, GameInfo>();
        editorkit = new HTMLEditorKit() {

            @Override
            protected Parser getParser() {
                try {
                    Class c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
                    Parser defaultParser = (Parser) c.newInstance();
                    return defaultParser;
                } catch (Throwable e) {
                }
                return null;
            }
        };
        createPhaseExecutors();
    }

    public void setContext(HexTD main, GameManagerImplementation gm) {
        mainGame = main;
        gameManager = gm;
        messageFactory = gm.getMessageFactory();
        gm.addGameListener(this);
        doInit();
        StyleSheet s = Cache.getInstance().loadStyleSheet("styles.css");
        editorkit.setStyleSheet(s);
        jTextPane_output.setEditorKit(editorkit);
        gameManager.addLocalPlayerListener(this);
    }

    private void doInit() {
        initComponents();
        fillMessageMap();
        plr = new PlayerListRenderer();
        jListPlayerList.setCellRenderer(plr);
        jPanelInLobby.setVisible(false);
        jButtonExit.setVisible(false);
        setNoConnection();
        htmlContent = new StringBuilder();
    }

    public JPanel getChannelList() {
        return jPanelChannels;
    }

    /**
	 * Checks if the game has progressed enough that leaving means forfeiting.
	 * Presents the user with a question if this is the case.
	 * @return true if user really wants to leave
	 */
    public boolean optionalForfeitGame() {
        String gameKey = gameManager.getGameState().getGamePhase().getKey();
        if (gameKey.equalsIgnoreCase("mpPlaying") || gameKey.equalsIgnoreCase("mpCatchup")) {
            if (gameManager.getGameState().getGameTime().getTime() > 6000) {
                Object[] options = { "Yes, forfeit", "No, keep playing" };
                int n = JOptionPane.showOptionDialog(this, "Leaving now will cause you to lose this game?", "Really leave?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (n == JOptionPane.YES_OPTION) {
                    MessagePlayerForfeit m = new MessagePlayerForfeit();
                    m.setTick(gameManager.getGameState().getGameTime().getTime());
                    m.setSenderId(playerGameId);
                    sendMessage(m);
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    public void exitGame() {
        if (optionalForfeitGame()) {
            sendMessage(new MessageGameJoin(0));
        }
    }

    public void setServerAddress(String address, int port) {
        serverAddress = address;
        serverPort = port;
    }

    public void startClient() {
        if (client == null) {
            setStatus("Connecting to server...");
            client = new NetworkClient(messageFactory);
            client.addConnectionListener(this);
            client.addMessageListener(this);
            client.setPort(serverPort);
            client.setServer(serverAddress);
            client.connect();
        }
    }

    private void doJoin() {
        Object value = jListChannelList.getSelectedValue();
        if (value != null && value instanceof GameInfo) {
            GameInfo gameInfo = (GameInfo) value;
            MessageGameJoin m = new MessageGameJoin(gameInfo.getId());
            m.setSenderId(localUserInfo.getUserId());
            sendMessage(m);
        }
    }

    private void setNoConnection() {
        jTextField_send.setEnabled(false);
        jButton_send.setEnabled(false);
    }

    public void stop() {
        if (client != null) {
            client.close();
        }
    }

    public void updatePlayerNames() {
        GameStateImplementation gameState = gameManager.getGameState();
        DefaultListModel m = (DefaultListModel) jListPlayerList.getModel();
        m.clear();
        for (int i = 0; i < users.size(); i++) {
            UserInfo info = users.get(i);
            m.addElement(info);
            HexTDPlayer p = gameState.getPlayer(info.getPlayerId());
            User u = new DefaultUser(info);
            if (p != null) {
                p.setUser(u);
            }
        }
    }

    public void updateChannelList() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    updateChannelList();
                }
            });
        } else {
            DefaultListModel m = (DefaultListModel) jListChannelList.getModel();
            m.clear();
            gamesById.clear();
            for (GameInfo game : games) {
                gamesById.put(game.getId(), game);
                if (game.getId() != 0) {
                    m.addElement(game);
                }
            }
            setChannelTitle();
        }
    }

    public int getConnectedPlayerCount() {
        return users.size();
    }

    private void setChannelTitle() {
        GameInfo info = gamesById.get(channelId);
        if (info != null) {
            channelName = info.getName();
        }
        TitledBorder b = (TitledBorder) jPanelChat.getBorder();
        b.setTitle(getUserName() + " in: " + channelName);
        jPanelChat.repaint();
    }

    /**
	 * Create executers for messages
	 */
    private void fillMessageMap() {
        messageCommands.put(MessageUserList.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageUserList mul = (MessageUserList) message;
                users = mul.getUsers();
                usersByUserId.clear();
                usersByPlayerId.clear();
                for (UserInfo info : users) {
                    usersByUserId.put(info.getUserId(), info);
                    usersByPlayerId.put(info.getPlayerId(), info);
                }
                updatePlayerNames();
            }
        });
        messageCommands.put(MessageGameList.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageGameList mgl = (MessageGameList) message;
                games = mgl.getGames();
                updateChannelList();
            }
        });
        messageCommands.put(MessageUserInGame.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                GameStateImplementation gameState = gameManager.getGameState();
                gameManager.getGameLoopManager().stop();
                MessageUserInGame muig = (MessageUserInGame) message;
                channelId = muig.getChannelId();
                playerGameId = muig.getPlayerId();
                gameManager.setLocalPlayerId(playerGameId);
                System.out.println("PanelNetConnection::textReceived: Channel: " + channelId + " Player: " + playerGameId);
                setChannelTitle();
                jTextField_send.setEnabled(true);
                jButton_send.setEnabled(true);
                if (channelId == 0) {
                    jPanelChannels.setVisible(true);
                    jPanelInLobby.setVisible(true);
                    jButtonExit.setVisible(false);
                    gameManager.getGameState().setGamePhase(DefaultGamePhaseName.MP_CONNECT.name());
                    jButtonKick.setVisible(false);
                    plr.setLongFormat(true);
                } else {
                    GameInfo info = gamesById.get(channelId);
                    if (info == null) {
                        channelName = "Unknown Channels";
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "Joined unknown channel with id{0}", channelId);
                    } else {
                        channelName = info.getName();
                    }
                    jPanelInLobby.setVisible(false);
                    jButtonExit.setVisible(true);
                    plr.setLongFormat(true);
                    if (muig.isRunning()) {
                        gameState.setGamePhase(DefaultGamePhaseName.MP_CATCHUP.name());
                        GliMpCatchup gl = (GliMpCatchup) gameState.getGamePhase().getGameLoop();
                        gl.setTargetTick(muig.getLastTick());
                    } else {
                        if (muig.getPlayerId() == 0) {
                            gameState.setGamePhase(DefaultGamePhaseName.MP_SELECTING.name());
                        } else {
                            gameState.setGamePhase(DefaultGamePhaseName.MP_CLIENT_SELECTING.name());
                        }
                    }
                    if (muig.getPlayerId() == 0) {
                        jButtonKick.setVisible(true);
                    } else {
                        jButtonKick.setVisible(false);
                    }
                }
            }
        });
        messageCommands.put(MessageSay.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageSay ms = (MessageSay) message;
                int speakerId = ms.getSenderId();
                UserInfo speaker = usersByUserId.get(speakerId);
                if (speaker != null) {
                    int gameId = speaker.getPlayerId();
                    int relId = gameId % 6;
                    String cls = "p" + relId;
                    addOutputText("<span class='" + cls + "'>" + speaker.getName() + "</span>: " + HTMLEncode.encode(ms.getText()) + "<br>");
                } else {
                    addOutputText(ms.getText());
                }
            }
        });
        messageCommands.put(MessageUserInfo.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageUserInfo mui = (MessageUserInfo) message;
                localUserInfo.updateFrom(mui.getUserInfo(), true);
                setStatus("Connected to server and authenticated. Multiplayer available.");
                authStatus = authStatus.success;
                doSendLater();
                if (channelId != -1) {
                    sendMessage(new MessageGameJoin(channelId));
                }
            }
        });
        messageCommands.put(MessageError.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageError me = (MessageError) message;
                switch(Constants.ErrorType.valueOf(me.getErrorType())) {
                    case AuthFailed:
                        if (gameManager.getGameState().getGamePhase().getKey().equals(DefaultGamePhaseName.TITLE_SCREEN.name())) {
                            setStatus("Connected to server but not authenticated.");
                            authStatus = authStatus.failed;
                            mainGame.showAlert("You are not logged in on Usoog.com:\n" + "Only single player available.");
                        }
                        doSendLater();
                        break;
                    case ReplayLoadFailed:
                        if (gameManager.getGameState().getGamePhase().getKey().equals(DefaultGamePhaseName.LOAD_REPLAY.name())) {
                            setStatus("Failed to load replay!");
                            gameManager.getGameState().setGamePhase(DefaultGamePhaseName.TITLE_SCREEN.name());
                        }
                        break;
                }
            }
        });
        messageCommands.put(MessageSettings.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageSettings mst = (MessageSettings) message;
                for (Entry<String, String> entry : mst.getSettings().entrySet()) {
                    settingKey key = Constants.settingKey.valueOf(entry.getKey());
                    switch(key) {
                        case ready:
                            UserInfo player = usersByUserId.get(mst.getSenderId());
                            if (player != null) {
                                player.setReady(Boolean.parseBoolean(entry.getValue()));
                            }
                            break;
                        case pause:
                            boolean paused = Boolean.parseBoolean(entry.getValue());
                            gameManager.setPause(paused, false);
                            break;
                        case ladderGame:
                        case openGame:
                        case publicGame:
                        case mapId:
                            gameManager.fireSettingChanged(key, entry.getValue());
                            break;
                        default:
                            System.out.println("PanelNetConnection::ExecuteMessageSettings: Unhandled setting: " + entry.getKey());
                            break;
                    }
                }
            }
        });
        messageCommands.put(MessagePong.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
            }
        });
        messageCommands.put(MessageReplay.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageReplay mrb = (MessageReplay) message;
                String key = gameManager.getGameState().getGamePhase().getKey();
                Logger.getLogger(PanelNetConnection.class.getName()).log(Level.INFO, "Got replay. Gamestate is {0}", key);
                if (key.equals(DefaultGamePhaseName.LOAD_REPLAY.name())) {
                    try {
                        gameManager.loadReplayLog(new BufferedReader(new StringReader(mrb.getReplay())), true);
                    } catch (FailedToLoadReplayException ex) {
                        Logger.getLogger(PanelNetConnection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (key.equals(DefaultGamePhaseName.MP_JOIN_RUNNING.name()) || key.equals(DefaultGamePhaseName.MP_CATCHUP.name())) {
                    try {
                        gameManager.loadReplayLog(new BufferedReader(new StringReader(mrb.getReplay())), true);
                    } catch (FailedToLoadReplayException ex) {
                        Logger.getLogger(PanelNetConnection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        messageCommands.put(MessageUnknown.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                try {
                    System.out.println("PanelNetConnection::textReceived: Unknown message:\n" + message.getMessage());
                } catch (Exception ex) {
                    Logger.getLogger(PanelNetConnection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        messageCommands.put(MessageMapList.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                mapLoader.parseRemoteIndex((MessageMapList) message);
            }
        });
        messageCommands.put(MessageMapData.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                mapLoader.parseRemoteMap((MessageMapData) message);
            }
        });
        messageCommands.put(MessageMapLoad.KEY, new MessageCommand() {

            @Override
            public void execute(Message message) {
                MessageMapLoad mml = (MessageMapLoad) message;
                try {
                    mml.exec(gameManager);
                } catch (NotReadyYetException ex) {
                    Logger.getLogger(PanelNetConnection.class.getName()).log(Level.WARNING, "MessageMapLoad received, but map not ready yet.", ex);
                }
            }
        });
    }

    @Override
    public void messageReceived(Message message) {
        if (keepFullLog) {
            fullLog.append(" <-- ");
            fullLog.append(message.getMessage());
            fullLog.append("\n");
        }
        try {
            MessageCommand command = messageCommands.get(message.getKey());
            if (command != null) {
                command.execute(message);
            } else {
                if (message instanceof ActionMessage) {
                    gameManager.addNetworkPlayerAction((TDActionMessage) message);
                } else {
                    System.out.println("PanelNetConnection::lineReceived: Message is not an ActionMessage and not handled locally!");
                    System.out.println("PanelNetConnection::lineReceived: " + message.getMessage().substring(0, 25) + "...");
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(PanelNetConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void sendMessage(Message message) {
        if (client == null && wasConnected) {
            startClient();
        }
        if (client != null) {
            client.sendMessage(message);
            if (keepFullLog) {
                fullLog.append(" --> ");
                fullLog.append(message.getMessage());
                fullLog.append("\n");
            }
        }
    }

    public void transmitSetting(Constants.settingKey key, String value) {
        Map<String, String> table = new HashMap<String, String>();
        table.put(key.name(), value);
        sendMessage(new MessageSettings(localUserInfo.getUserId(), table));
    }

    public void transmitSetting(Map<String, String> table) {
        sendMessage(new MessageSettings(localUserInfo.getUserId(), table));
    }

    private synchronized void addOutputText(String text) {
        htmlContent.insert(0, text);
        jTextPane_output.setText("<html><body>" + htmlContent + "</body></html>");
        jTextPane_output.setCaretPosition(0);
    }

    @Override
    public void connectionEstablished(NetworkServerConnection connection) {
        setStatus("Connected to server, authenticating...");
        System.out.println("PanelNetConnection::connectionEstablished");
        addOutputText("<-- New Connection!");
        wasConnected = true;
        isConnected = true;
        if (client != null) {
            sendMessage(new MessageUserAuth(localUserInfo.getToken()));
        }
        mainGame.updateGui();
    }

    @Override
    public void connectionLost(String reason) {
        isConnected = false;
        System.out.println("PanelNetConnection::connectionLost");
        setStatus("Connecting to server failed: " + reason);
        addOutputText("<div class='playerJoined'>Connection Failed because: " + reason + "</div>");
        client.close();
        client = null;
    }

    @Override
    public void connectionClosed() {
        isConnected = false;
        setStatus("Not connected to server.");
        authStatus = Constants.AuthStatus.unknown;
        client.close();
        client = null;
        addOutputText("<div class='playerJoined'>Lost Connection!</div>");
    }

    public boolean isConnected() {
        return (isConnected && client != null);
    }

    public synchronized void loadReplay(boolean mp, int replayId) {
        Message message;
        if (mp) {
            message = new MessageFetch(Constants.FetchType.mp.name(), replayId);
        } else {
            message = new MessageFetch(Constants.FetchType.sp.name(), replayId);
        }
        if (authStatus == Constants.AuthStatus.unknown) {
            sendLater = message;
        } else {
            sendMessage(message);
        }
    }

    private synchronized void doSendLater() {
        if (sendLater != null) {
            sendMessage(sendLater);
            sendLater = null;
        }
    }

    private void doSay() {
        String text = jTextField_send.getText().trim();
        if (text.length() > 0) {
            MessageSay m = new MessageSay(text);
            m.setSenderId(localUserInfo.getUserId());
            sendMessage(m);
            addOutputText("<span class='p" + playerGameId + "'>" + localUserInfo.getName() + "</span>: " + HTMLEncode.encode(text) + "<br>");
            jTextField_send.setText("");
        }
    }

    public String getToken() {
        return localUserInfo.getToken();
    }

    public void setToken(String t) {
        localUserInfo.setToken(t);
    }

    public String getUserName() {
        return localUserInfo.getName();
    }

    private void kickPlayer() {
        UserInfo selected = (UserInfo) jListPlayerList.getSelectedValue();
        if (selected != null) {
            MessageUserKick m = new MessageUserKick(localUserInfo.getUserId());
            m.setSenderId(selected.getUserId());
            sendMessage(m);
        }
    }

    @Override
    public void setVisible(boolean aFlag) {
        if (aFlag) {
            if (pingTimer == null) {
                pingTimer = new Timer("pingTimer", true);
            }
            if (pingTask == null) {
                pingTask = new TimerTask() {

                    @Override
                    public void run() {
                        sendMessage(new MessagePing());
                    }
                };
                pingTimer.schedule(pingTask, 120000, 120000);
            }
        } else {
            if (pingTask != null) {
                pingTask.cancel();
                pingTask = null;
            }
        }
        super.setVisible(aFlag);
    }

    public void showTextBar() {
        jTextField_send.setVisible(true);
        jTextField_send.requestFocusInWindow();
        jButton_send.setVisible(true);
    }

    public void hideTextBar() {
        if (!keepTextBar) {
            jTextField_send.setVisible(false);
            jButton_send.setVisible(false);
        }
    }

    @Override
    public SocketAddress getRemoteAddress() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addMessageListener(MessageListener l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeMessageListener(MessageListener l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
	 * A PhaseExecutor encapsulates a set of commands to run when the gamePhase
	 * changes to a certain gamePhase.
	 *
	 * TODO: set back to private after DataNucleus fixes it's schemaTool
	 */
    public interface PhaseExecutor {

        public void execute(GamePhase newGamePhase);
    }

    private Map<String, PhaseExecutor> phaseExecutorMap = new HashMap<String, PhaseExecutor>();

    private void createPhaseExecutors() {
        phaseExecutorMap.put(DefaultGamePhaseName.TITLE_SCREEN.name(), new PhaseExecutor() {

            @Override
            public void execute(GamePhase newGamePhase) {
                setVisible(false);
            }
        });
        phaseExecutorMap.put(DefaultGamePhaseName.MP_CONNECT.name(), new PhaseExecutor() {

            @Override
            public void execute(GamePhase newGamePhase) {
                setVisible(true);
            }
        });
        phaseExecutorMap.put(DefaultGamePhaseName.MP_SELECTING.name(), new PhaseExecutor() {

            @Override
            public void execute(GamePhase newGamePhase) {
                setVisible(true);
            }
        });
        phaseExecutorMap.put(DefaultGamePhaseName.MP_CLIENT_SELECTING.name(), new PhaseExecutor() {

            @Override
            public void execute(GamePhase newGamePhase) {
                setVisible(true);
            }
        });
        phaseExecutorMap.put(DefaultGamePhaseName.MP_PLAYING.name(), new PhaseExecutor() {

            @Override
            public void execute(GamePhase newGamePhase) {
                setVisible(true);
            }
        });
    }

    @Override
    public void gamePhaseChanged(GamePhase newGamePhase) {
        PhaseExecutor executor = phaseExecutorMap.get(newGamePhase.getKey());
        if (executor != null) {
            executor.execute(newGamePhase);
        }
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        jFrame1 = new javax.swing.JFrame();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextPaneFullLog = new javax.swing.JTextPane();
        jButton1 = new javax.swing.JButton();
        jButtonKick = new javax.swing.JButton();
        jPanelChat = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextPane_output = new javax.swing.JTextPane();
        jTextField_send = new javax.swing.JTextField();
        jButton_send = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListPlayerList = new javax.swing.JList();
        jPanelChannels = new javax.swing.JPanel();
        jPanelInLobby = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jListChannelList = new javax.swing.JList();
        jButtonJoin = new javax.swing.JButton();
        jButtonCreate = new javax.swing.JButton();
        jButtonExit = new javax.swing.JButton();
        jScrollPane4.setViewportView(jTextPaneFullLog);
        jFrame1.getContentPane().add(jScrollPane4, java.awt.BorderLayout.CENTER);
        jButton1.setText("refresh");
        jButton1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jFrame1.getContentPane().add(jButton1, java.awt.BorderLayout.SOUTH);
        setLayout(new java.awt.GridBagLayout());
        jButtonKick.setBackground(Constants.colorBackGround);
        jButtonKick.setForeground(Constants.colorForeGround);
        jButtonKick.setText("Kick");
        jButtonKick.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonKickActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0E-5;
        gridBagConstraints.weighty = 1.0E-5;
        add(jButtonKick, gridBagConstraints);
        jPanelChat.setBackground(Constants.colorBackGround);
        jPanelChat.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Chat", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12), Constants.colorForeGround));
        jPanelChat.setForeground(Constants.colorForeGround);
        jPanelChat.setMaximumSize(new java.awt.Dimension(2147483647, 175));
        jPanelChat.setPreferredSize(new java.awt.Dimension(134, 175));
        jPanelChat.setLayout(new java.awt.GridBagLayout());
        jScrollPane1.setBackground(Constants.colorBackGround);
        jScrollPane1.setForeground(Constants.colorForeGround);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jTextPane_output.setBackground(Constants.colorBackGround);
        jTextPane_output.setEditable(false);
        jTextPane_output.setForeground(Constants.colorForeGround);
        jScrollPane1.setViewportView(jTextPane_output);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 1.0E-4;
        jPanelChat.add(jScrollPane1, gridBagConstraints);
        jTextField_send.setBackground(Constants.colorBackGround);
        jTextField_send.setForeground(Constants.colorForeGround);
        jTextField_send.setText("Some text");
        jTextField_send.setCaretColor(Constants.colorForeGroundPale);
        jTextField_send.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField_sendActionPerformed(evt);
            }
        });
        jTextField_send.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextField_sendFocusLost(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.01;
        jPanelChat.add(jTextField_send, gridBagConstraints);
        jButton_send.setBackground(Constants.colorBackGround);
        jButton_send.setForeground(Constants.colorForeGround);
        jButton_send.setText("Send");
        jButton_send.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_sendActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        jPanelChat.add(jButton_send, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.1;
        add(jPanelChat, gridBagConstraints);
        jScrollPane2.setBackground(Constants.colorBackGround);
        jScrollPane2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Players", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12), Constants.colorForeGround));
        jScrollPane2.setForeground(Constants.colorForeGround);
        jScrollPane2.setMaximumSize(new java.awt.Dimension(150, 32767));
        jScrollPane2.setMinimumSize(new java.awt.Dimension(0, 0));
        jScrollPane2.setPreferredSize(new java.awt.Dimension(150, 128));
        jListPlayerList.setBackground(Constants.colorBackGround);
        jListPlayerList.setForeground(Constants.colorForeGround);
        jListPlayerList.setModel(new DefaultListModel());
        jListPlayerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(jListPlayerList);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        add(jScrollPane2, gridBagConstraints);
        jPanelChannels.setBackground(Constants.colorBackGround);
        jPanelChannels.setForeground(Constants.colorForeGround);
        jPanelChannels.setLayout(new java.awt.GridBagLayout());
        jPanelInLobby.setBackground(Constants.colorBackGround);
        jPanelInLobby.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Games", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12), Constants.colorForeGround));
        jPanelInLobby.setForeground(Constants.colorForeGround);
        jPanelInLobby.setLayout(new java.awt.GridBagLayout());
        jScrollPane3.setBackground(Constants.colorBackGround);
        jScrollPane3.setForeground(Constants.colorForeGround);
        jScrollPane3.setMaximumSize(new java.awt.Dimension(100, 32767));
        jScrollPane3.setMinimumSize(new java.awt.Dimension(0, 0));
        jScrollPane3.setPreferredSize(new java.awt.Dimension(100, 157));
        jListChannelList.setBackground(Constants.colorBackGround);
        jListChannelList.setForeground(Constants.colorForeGround);
        jListChannelList.setModel(new DefaultListModel());
        jListChannelList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListChannelList.setCellRenderer(new ChannelListRenderer());
        jListChannelList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jListChannelListMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(jListChannelList);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.01;
        jPanelInLobby.add(jScrollPane3, gridBagConstraints);
        jButtonJoin.setBackground(Constants.colorBackGround);
        jButtonJoin.setForeground(Constants.colorForeGround);
        jButtonJoin.setText("Join");
        jButtonJoin.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonJoinActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 2);
        jPanelInLobby.add(jButtonJoin, gridBagConstraints);
        jButtonCreate.setBackground(Constants.colorBackGround);
        jButtonCreate.setForeground(Constants.colorForeGround);
        jButtonCreate.setText("New");
        jButtonCreate.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCreateActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 0, 0);
        jPanelInLobby.add(jButtonCreate, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 0.01;
        gridBagConstraints.weighty = 0.01;
        jPanelChannels.add(jPanelInLobby, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        add(jPanelChannels, gridBagConstraints);
        jButtonExit.setBackground(Constants.colorBackGround);
        jButtonExit.setForeground(Constants.colorForeGround);
        jButtonExit.setText("Leave");
        jButtonExit.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExitActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0E-5;
        gridBagConstraints.weighty = 1.0E-5;
        add(jButtonExit, gridBagConstraints);
    }

    private void jButton_sendActionPerformed(java.awt.event.ActionEvent evt) {
        doSay();
        if (keepFullLog) {
            jTextPaneFullLog.setText(fullLog.toString());
            jFrame1.setVisible(true);
            jFrame1.pack();
        }
    }

    private void jTextField_sendActionPerformed(java.awt.event.ActionEvent evt) {
        doSay();
    }

    private void jButtonJoinActionPerformed(java.awt.event.ActionEvent evt) {
        doJoin();
    }

    private void jButtonCreateActionPerformed(java.awt.event.ActionEvent evt) {
        String s = JOptionPane.showInputDialog("Please enter a name for your game", localUserInfo.getName() + "'s Game");
        if ((s != null) && (s.length() > 0)) {
            sendMessage(new MessageGameCreate(s));
        }
    }

    private void jButtonExitActionPerformed(java.awt.event.ActionEvent evt) {
        exitGame();
    }

    private void jListChannelListMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() > 1) {
            doJoin();
        }
    }

    private void jButtonKickActionPerformed(java.awt.event.ActionEvent evt) {
        kickPlayer();
    }

    private void jTextField_sendFocusLost(java.awt.event.FocusEvent evt) {
        hideTextBar();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        jTextPaneFullLog.setText(fullLog.toString());
    }

    private javax.swing.JButton jButton1;

    private javax.swing.JButton jButtonCreate;

    private javax.swing.JButton jButtonExit;

    private javax.swing.JButton jButtonJoin;

    private javax.swing.JButton jButtonKick;

    private javax.swing.JButton jButton_send;

    private javax.swing.JFrame jFrame1;

    private javax.swing.JList jListChannelList;

    private javax.swing.JList jListPlayerList;

    private javax.swing.JPanel jPanelChannels;

    private javax.swing.JPanel jPanelChat;

    private javax.swing.JPanel jPanelInLobby;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JScrollPane jScrollPane4;

    private javax.swing.JTextField jTextField_send;

    private javax.swing.JTextPane jTextPaneFullLog;

    private javax.swing.JTextPane jTextPane_output;

    /**
	 * @return the status
	 */
    public String getStatus() {
        return status;
    }

    /**
	 * @param status the status to set
	 */
    public void setStatus(String status) {
        this.status = status;
        mainGame.setNetStatus(status);
    }

    /**
	 * @return the keepTextBar
	 */
    public boolean isKeepTextBar() {
        return keepTextBar;
    }

    /**
	 * @param keepTextBar the keepTextBar to set
	 */
    public void setKeepTextBar(boolean ktb) {
        if (keepTextBar != ktb) {
            keepTextBar = ktb;
            jTextField_send.setVisible(ktb);
            jButton_send.setVisible(ktb);
        }
    }

    /**
	 * @return the localPlayerInfo
	 * After making changes to the local user info,
	 * @see sendUpdatedUserInfo() must be called to send the changes to the
	 * server.
	 */
    public UserInfo getLocalUserInfo() {
        return localUserInfo;
    }

    /**
	 * Sends updates to the local user's info to the server.
	 */
    public void sendUpdatedUserInfo() {
        MessageUserInfo m = new MessageUserInfo(localUserInfo);
        m.setSenderId(localUserInfo.getUserId());
        sendMessage(m);
    }

    @Override
    public void localPlayerChanged(HexTDPlayer newLocalPlayer) {
        if (newLocalPlayer != null) {
            newLocalPlayer.setUser(localUser);
        }
    }

    @Override
    public void mapLoading() {
    }

    @Override
    public void mapLoaded() {
        updatePlayerNames();
    }

    @Override
    public void speedChanged() {
    }

    @Override
    public void pauseChanged() {
    }

    /**
	 * @return the mapLoader
	 */
    public MapLoaderClient getMapLoader() {
        return mapLoader;
    }

    /**
	 * @param mapLoader the mapLoader to set
	 */
    public void setMapLoader(MapLoaderClient mapLoader) {
        this.mapLoader = mapLoader;
    }
}

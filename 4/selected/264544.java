package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IClientChannel;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.framework.ui.background.WaitWindow;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessengerException;
import games.strategy.engine.random.CryptoRandomSource;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ServerLauncher implements ILauncher {

    private static final Logger s_logger = Logger.getLogger(ServerLauncher.class.getName());

    public static final String SERVER_ROOT_DIR_PROPERTY = "triplea.server.root.dir";

    private final int m_clientCount;

    private final IRemoteMessenger m_remoteMessenger;

    private final IChannelMessenger m_channelMessenger;

    private final IMessenger m_messenger;

    private final GameData m_gameData;

    private final Map<String, String> m_localPlayerMapping;

    private final Map<String, INode> m_remotelPlayers;

    private final GameSelectorModel m_gameSelectorModel;

    private final ServerModel m_serverModel;

    private ServerGame m_serverGame;

    private Component m_ui;

    private final CountDownLatch m_erroLatch = new CountDownLatch(1);

    private volatile boolean m_isLaunching = true;

    private ServerReady m_serverReady;

    private volatile boolean m_abortLaunch = false;

    private final List<INode> m_observersThatTriedToJoinDuringStartup = Collections.synchronizedList(new ArrayList<INode>());

    private final WaitWindow m_gameLoadingWindow = new WaitWindow("Loading game, please wait.");

    private InGameLobbyWatcher m_inGameLobbyWatcher;

    public ServerLauncher(final int clientCount, final IRemoteMessenger remoteMessenger, final IChannelMessenger channelMessenger, final IMessenger messenger, final GameSelectorModel gameSelectorModel, final Map<String, String> localPlayerMapping, final Map<String, INode> remotelPlayers, final ServerModel serverModel) {
        m_clientCount = clientCount;
        m_remoteMessenger = remoteMessenger;
        m_channelMessenger = channelMessenger;
        m_messenger = messenger;
        m_gameData = gameSelectorModel.getGameData();
        m_gameSelectorModel = gameSelectorModel;
        m_localPlayerMapping = localPlayerMapping;
        m_remotelPlayers = remotelPlayers;
        m_serverModel = serverModel;
    }

    public void setInGameLobbyWatcher(final InGameLobbyWatcher watcher) {
        m_inGameLobbyWatcher = watcher;
    }

    public void launch(final Component parent) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Wrong thread");
        final Runnable r = new Runnable() {

            public void run() {
                try {
                    launchInNewThread(parent);
                } finally {
                    m_gameLoadingWindow.doneWait();
                    if (m_inGameLobbyWatcher != null) {
                        m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.IN_PROGRESS, m_serverGame);
                    }
                }
            }
        };
        final Thread t = new Thread(r);
        m_gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(parent));
        m_gameLoadingWindow.setVisible(true);
        m_gameLoadingWindow.showWait();
        JOptionPane.getFrameForComponent(parent).setVisible(false);
        t.start();
    }

    private void launchInNewThread(final Component parent) {
        if (m_inGameLobbyWatcher != null) {
            m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.LAUNCHING, null);
        }
        m_ui = parent;
        m_serverModel.setServerLauncher(this);
        s_logger.fine("Starting server");
        m_serverReady = new ServerReady(m_clientCount);
        m_remoteMessenger.registerRemote(m_serverReady, ClientModel.CLIENT_READY_CHANNEL);
        byte[] gameDataAsBytes;
        try {
            gameDataAsBytes = gameDataToBytes(m_gameData);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
        final Set<IGamePlayer> localPlayerSet = m_gameData.getGameLoader().createPlayers(m_localPlayerMapping);
        final Messengers messengers = new Messengers(m_messenger, m_remoteMessenger, m_channelMessenger);
        m_serverGame = new ServerGame(m_gameData, localPlayerSet, m_remotelPlayers, messengers);
        m_serverGame.setInGameLobbyWatcher(m_inGameLobbyWatcher);
        ((IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME)).doneSelectingPlayers(gameDataAsBytes, m_serverGame.getPlayerManager().getPlayerMapping());
        final boolean useSecureRandomSource = !m_remotelPlayers.isEmpty() && !m_localPlayerMapping.isEmpty();
        if (useSecureRandomSource) {
            final PlayerID remotePlayer = m_serverGame.getPlayerManager().getRemoteOpponent(m_messenger.getLocalNode(), m_gameData);
            final CryptoRandomSource randomSource = new CryptoRandomSource(remotePlayer, m_serverGame);
            m_serverGame.setRandomSource(randomSource);
        }
        try {
            m_gameData.getGameLoader().startGame(m_serverGame, localPlayerSet);
        } catch (IllegalStateException e) {
            m_abortLaunch = true;
            Throwable error = e;
            while (error.getMessage() == null) error = error.getCause();
            final String message = error.getMessage();
            m_gameLoadingWindow.doneWait();
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
                }
            });
        } catch (final Exception e) {
            e.printStackTrace();
            m_abortLaunch = true;
        }
        m_serverReady.await();
        m_remoteMessenger.unregisterRemote(ClientModel.CLIENT_READY_CHANNEL);
        final Thread t = new Thread("Triplea, start server game") {

            @Override
            public void run() {
                try {
                    m_isLaunching = false;
                    if (!m_abortLaunch) {
                        if (useSecureRandomSource) {
                            warmUpCryptoRandomSource();
                        }
                        m_gameLoadingWindow.doneWait();
                        m_serverGame.startGame();
                    } else {
                        m_serverGame.stopGame();
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                JOptionPane.showMessageDialog(m_ui, "Error during startup, game aborted.");
                            }
                        });
                    }
                } catch (final MessengerException me) {
                    me.printStackTrace(System.out);
                    try {
                        if (!m_abortLaunch) m_erroLatch.await();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                m_gameSelectorModel.loadDefaultGame(parent);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        JOptionPane.getFrameForComponent(parent).setVisible(true);
                    }
                });
                m_serverModel.setServerLauncher(null);
                m_serverModel.newGame();
                if (m_inGameLobbyWatcher != null) {
                    m_inGameLobbyWatcher.setGameStatus(GameDescription.GameStatus.WAITING_FOR_PLAYERS, null);
                }
            }
        };
        t.start();
    }

    private void warmUpCryptoRandomSource() {
        final Thread t = new Thread("Warming up crypto random source") {

            @Override
            public void run() {
                try {
                    m_serverGame.getRandomSource().getRandom(m_gameData.getDiceSides(), 2, "Warming up crpyto random source");
                } catch (final RuntimeException re) {
                    re.printStackTrace(System.out);
                }
            }
        };
        t.start();
    }

    public void addObserver(final IObserverWaitingToJoin observer, final INode newNode) {
        if (m_isLaunching) {
            m_observersThatTriedToJoinDuringStartup.add(newNode);
            observer.cannotJoinGame("Game is launching, try again soon");
            return;
        }
        m_serverGame.addObserver(observer);
    }

    public static byte[] gameDataToBytes(final GameData data) throws IOException {
        final ByteArrayOutputStream sink = new ByteArrayOutputStream(25000);
        new GameDataManager().saveGame(sink, data);
        sink.flush();
        sink.close();
        return sink.toByteArray();
    }

    public void connectionLost(final INode node) {
        if (m_isLaunching) {
            if (m_observersThatTriedToJoinDuringStartup.remove(node)) return;
            m_serverReady.clientReady();
            m_abortLaunch = true;
            return;
        }
        if (m_serverGame.getPlayerManager().isPlaying(node)) {
            if (m_serverGame.isGameSequenceRunning()) saveAndEndGame(node); else m_serverGame.stopGame();
            m_erroLatch.countDown();
        } else {
        }
    }

    private void saveAndEndGame(final INode node) {
        final DateFormat format = new SimpleDateFormat("MMM_dd_'at'_HH_mm");
        SaveGameFileChooser.ensureDefaultDirExists();
        final File f = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, "connection_lost_on_" + format.format(new Date()) + ".tsvg");
        m_serverGame.saveGame(f);
        m_serverGame.stopGame();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                final String message = "Connection lost to:" + node.getName() + " game is over.  Game saved to:" + f.getName();
                JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_ui), message);
            }
        });
    }
}

class ServerReady implements IServerReady {

    private final CountDownLatch m_latch;

    ServerReady(final int waitCount) {
        m_latch = new CountDownLatch(waitCount);
    }

    public void clientReady() {
        m_latch.countDown();
    }

    public void await() {
        try {
            m_latch.await();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }
}

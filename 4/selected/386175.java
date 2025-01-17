package games.strategy.engine.framework;

import games.strategy.debug.Console;
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.DefaultDelegateBridge;
import games.strategy.engine.delegate.DelegateExecutionManager;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.display.DefaultDisplayBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.startup.mc.IObserverWaitingToJoin;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.DefaultPlayerBridge;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRemoteRandom;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.engine.random.RandomStats;
import games.strategy.engine.vault.Vault;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.Dynamix_AI.CommandCenter.CachedInstanceCenter;
import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.util.ListenerList;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author Sean Bridges
 * 
 *         Represents a running game.
 *         Lookups to get a GamePlayer from PlayerId and the current Delegate.
 */
public class ServerGame implements IGame {

    public static final String DISPLAY_CHANNEL = "games.strategy.engine.framework.ServerGame.DISPLAY_CHANNEL";

    public static final RemoteName SERVER_REMOTE = new RemoteName("games.strategy.engine.framework.ServerGame.SERVER_REMOTE", IServerRemote.class);

    private final ListenerList<GameStepListener> m_gameStepListeners = new ListenerList<GameStepListener>();

    private final GameData m_data;

    private final Map<PlayerID, IGamePlayer> m_gamePlayers = new HashMap<PlayerID, IGamePlayer>();

    private final IServerMessenger m_messenger;

    private final ChangePerformer m_changePerformer;

    private final IRemoteMessenger m_remoteMessenger;

    private final IChannelMessenger m_channelMessenger;

    private final Vault m_vault;

    private final RandomStats m_randomStats;

    private IRandomSource m_randomSource = new PlainRandomSource();

    private IRandomSource m_delegateRandomSource;

    private final DelegateExecutionManager m_delegateExecutionManager = new DelegateExecutionManager();

    private volatile boolean m_isGameOver = false;

    private InGameLobbyWatcher m_inGameLobbyWatcher;

    private boolean m_needToInitialize = true;

    private boolean m_firstRun = true;

    /**
	 * When the delegate execution is stopped, we countdown on this latch to prevent the startgame(...) method from returning.
	 * <p>
	 */
    private final CountDownLatch m_delegateExecutionStoppedLatch = new CountDownLatch(1);

    /**
	 * Has the delegate signalled that delegate execution should stop.
	 */
    private volatile boolean m_delegateExecutionStopped = false;

    private final IServerRemote m_serverRemote = new IServerRemote() {

        public byte[] getSavedGame() {
            final ByteArrayOutputStream sink = new ByteArrayOutputStream(5000);
            try {
                saveGame(sink);
            } catch (final IOException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            }
            return sink.toByteArray();
        }
    };

    public static RemoteName getDisplayChannel(final GameData data) {
        return new RemoteName(DISPLAY_CHANNEL, data.getGameLoader().getDisplayType());
    }

    private final PlayerManager m_players;

    /**
	 * 
	 * @param localPlayers
	 *            Set - A set of GamePlayers
	 * @param messenger
	 *            IServerMessenger
	 * @param remotePlayerMapping
	 *            Map
	 */
    public ServerGame(final GameData data, final Set<IGamePlayer> localPlayers, final Map<String, INode> remotePlayerMapping, final Messengers messengers) {
        m_data = data;
        m_messenger = (IServerMessenger) messengers.getMessenger();
        m_remoteMessenger = messengers.getRemoteMessenger();
        m_channelMessenger = messengers.getChannelMessenger();
        m_vault = new Vault(m_channelMessenger);
        final Map<String, INode> allPlayers = new HashMap<String, INode>(remotePlayerMapping);
        for (final IGamePlayer player : localPlayers) {
            allPlayers.put(player.getName(), m_messenger.getLocalNode());
        }
        m_players = new PlayerManager(allPlayers);
        m_channelMessenger.registerChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
        CachedInstanceCenter.CachedGameData = data;
        setupLocalPlayers(localPlayers);
        setupDelegateMessaging(data);
        m_changePerformer = new ChangePerformer(data);
        m_randomStats = new RandomStats(m_remoteMessenger);
        m_remoteMessenger.registerRemote(m_serverRemote, SERVER_REMOTE);
    }

    /**
	 * @param localPlayers
	 */
    private void setupLocalPlayers(final Set<IGamePlayer> localPlayers) {
        final Iterator<IGamePlayer> localPlayersIter = localPlayers.iterator();
        while (localPlayersIter.hasNext()) {
            final IGamePlayer gp = localPlayersIter.next();
            final PlayerID player = m_data.getPlayerList().getPlayerID(gp.getName());
            m_gamePlayers.put(player, gp);
            final IPlayerBridge bridge = new DefaultPlayerBridge(this);
            gp.initialize(bridge, player);
            final RemoteName descriptor = getRemoteName(gp.getID(), m_data);
            m_remoteMessenger.registerRemote(gp, descriptor);
        }
    }

    public void addObserver(final IObserverWaitingToJoin observer) {
        try {
            if (!m_delegateExecutionManager.blockDelegateExecution(2000)) {
                observer.cannotJoinGame("Could not block delegate execution");
                return;
            }
        } catch (final InterruptedException e) {
            observer.cannotJoinGame(e.getMessage());
            return;
        }
        try {
            final ByteArrayOutputStream sink = new ByteArrayOutputStream(1000);
            saveGame(sink);
            observer.joinGame(sink.toByteArray(), m_players.getPlayerMapping());
        } catch (final IOException ioe) {
            observer.cannotJoinGame(ioe.getMessage());
            return;
        } finally {
            m_delegateExecutionManager.resumeDelegateExecution();
        }
    }

    private void setupDelegateMessaging(final GameData data) {
        for (final IDelegate delegate : data.getDelegateList()) {
            addDelegateMessenger(delegate);
        }
    }

    public void addDelegateMessenger(final IDelegate delegate) {
        final Class<? extends IRemote> remoteType = delegate.getRemoteType();
        if (remoteType == null) return;
        final Object wrappedDelegate = m_delegateExecutionManager.createInboundImplementation(delegate, new Class[] { delegate.getRemoteType() });
        final RemoteName descriptor = getRemoteName(delegate);
        m_remoteMessenger.registerRemote(wrappedDelegate, descriptor);
    }

    public static RemoteName getRemoteName(final IDelegate delegate) {
        return new RemoteName("games.strategy.engine.framework.ServerGame.DELEGATE_REMOTE." + delegate.getName(), delegate.getRemoteType());
    }

    public static RemoteName getRemoteName(final PlayerID id, final GameData data) {
        return new RemoteName("games.strategy.engine.framework.ServerGame.PLAYER_REMOTE." + id.getName(), data.getGameLoader().getRemotePlayerType());
    }

    public static RemoteName getRemoteRandomName(final PlayerID id) {
        return new RemoteName("games.strategy.engine.framework.ServerGame.PLAYER_RANDOM_REMOTE" + id.getName(), IRemoteRandom.class);
    }

    public GameData getData() {
        return m_data;
    }

    private GameStep getCurrentStep() {
        return m_data.getSequence().getStep();
    }

    private static final String GAME_HAS_BEEN_SAVED_PROPERTY = "games.strategy.engine.framework.ServerGame.GameHasBeenSaved";

    /**
	 * And here we go.
	 * Starts the game in a new thread
	 */
    public void startGame() {
        try {
            final boolean gameHasBeenSaved = m_data.getProperties().get(GAME_HAS_BEEN_SAVED_PROPERTY, false);
            if (!gameHasBeenSaved) m_data.getProperties().set(GAME_HAS_BEEN_SAVED_PROPERTY, Boolean.TRUE);
            startPersistentDelegates();
            if (gameHasBeenSaved) {
                runStep(gameHasBeenSaved);
            }
            while (!m_isGameOver) {
                if (m_delegateExecutionStopped) {
                    try {
                        m_delegateExecutionStoppedLatch.await();
                    } catch (final InterruptedException e) {
                    }
                } else {
                    runStep(false);
                }
            }
        } catch (final GameOverException goe) {
            if (!m_isGameOver) goe.printStackTrace();
            return;
        }
    }

    public void stopGame() {
        if (m_isGameOver) return;
        m_isGameOver = true;
        ErrorHandler.setGameOver(true);
        m_delegateExecutionStoppedLatch.countDown();
        try {
            if (!m_delegateExecutionManager.blockDelegateExecution(4000)) {
                Console.getConsole().dumpStacks();
                System.exit(0);
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        try {
            m_delegateExecutionManager.setGameOver();
            getGameModifiedBroadcaster().shutDown();
            m_randomStats.shutDown();
            m_channelMessenger.unregisterChannelSubscriber(m_gameModifiedChannel, IGame.GAME_MODIFICATION_CHANNEL);
            m_remoteMessenger.unregisterRemote(SERVER_REMOTE);
            m_vault.shutDown();
            final Iterator<IGamePlayer> localPlayersIter = m_gamePlayers.values().iterator();
            while (localPlayersIter.hasNext()) {
                final IGamePlayer gp = localPlayersIter.next();
                m_remoteMessenger.unregisterRemote(getRemoteName(gp.getID(), m_data));
            }
            final Iterator<IDelegate> delegateIter = m_data.getDelegateList().iterator();
            while (delegateIter.hasNext()) {
                final IDelegate delegate = delegateIter.next();
                final Class<? extends IRemote> remoteType = delegate.getRemoteType();
                if (remoteType == null) continue;
                m_remoteMessenger.unregisterRemote(getRemoteName(delegate));
            }
        } catch (final RuntimeException re) {
            re.printStackTrace();
        } finally {
            m_delegateExecutionManager.resumeDelegateExecution();
        }
        m_data.getGameLoader().shutDown();
    }

    private void autoSave() {
        FileOutputStream out = null;
        try {
            SaveGameFileChooser.ensureDefaultDirExists();
            final File autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.AUTOSAVE_FILE_NAME);
            out = new FileOutputStream(autosaveFile);
            saveGame(out);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void autoSaveRound() {
        FileOutputStream out = null;
        try {
            SaveGameFileChooser.ensureDefaultDirExists();
            File autosaveFile;
            if (m_data.getSequence().getRound() % 2 == 0) autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.AUTOSAVE_EVEN_ROUND_FILE_NAME); else autosaveFile = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.AUTOSAVE_ODD_ROUND_FILE_NAME);
            out = new FileOutputStream(autosaveFile);
            saveGame(out);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveGame(final File f) {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(f);
            saveGame(fout);
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveGame(final OutputStream out) throws IOException {
        try {
            if (!m_delegateExecutionManager.blockDelegateExecution(3000)) {
                new IOException("Could not lock delegate execution").printStackTrace();
            }
        } catch (final InterruptedException ie) {
            throw new IOException(ie.getMessage());
        }
        try {
            new GameDataManager().saveGame(out, m_data);
        } finally {
            m_delegateExecutionManager.resumeDelegateExecution();
        }
    }

    private void runStep(final boolean stepIsRestoredFromSavedGame) {
        if (getCurrentStep().hasReachedMaxRunCount()) {
            m_data.getSequence().next();
            return;
        }
        if (m_isGameOver) return;
        startStep(stepIsRestoredFromSavedGame);
        if (m_isGameOver) return;
        waitForPlayerToFinishStep();
        if (m_isGameOver) return;
        final boolean autoSaveAfterDelegateDone = endStep();
        if (m_isGameOver) return;
        if (m_data.getSequence().next()) {
            m_data.getHistory().getHistoryWriter().startNextRound(m_data.getSequence().getRound());
            autoSaveRound();
        }
        if (autoSaveAfterDelegateDone) autoSave();
    }

    /**
	 * 
	 * @return true if the step should autosave
	 */
    private boolean endStep() {
        m_delegateExecutionManager.enterDelegateExecution();
        try {
            getCurrentStep().getDelegate().end();
        } finally {
            m_delegateExecutionManager.leaveDelegateExecution();
        }
        getCurrentStep().incrementRunCount();
        if (m_data.getSequence().getStep().getDelegate().getClass().isAnnotationPresent(AutoSave.class)) {
            if (m_data.getSequence().getStep().getDelegate().getClass().getAnnotation(AutoSave.class).afterStepEnd()) return true;
        }
        return false;
    }

    private void startPersistentDelegates() {
        final Iterator<IDelegate> delegateIter = m_data.getDelegateList().iterator();
        while (delegateIter.hasNext()) {
            final IDelegate delegate = delegateIter.next();
            if (!(delegate instanceof IPersistentDelegate)) {
                continue;
            }
            final DefaultDelegateBridge bridge = new DefaultDelegateBridge(m_data, this, new DelegateHistoryWriter(m_channelMessenger), m_randomStats, m_delegateExecutionManager);
            CachedInstanceCenter.CachedDelegateBridge = bridge;
            if (m_delegateRandomSource == null) {
                m_delegateRandomSource = (IRandomSource) m_delegateExecutionManager.createOutboundImplementation(m_randomSource, new Class[] { IRandomSource.class });
            }
            bridge.setRandomSource(m_delegateRandomSource);
            m_delegateExecutionManager.enterDelegateExecution();
            try {
                delegate.start(bridge);
            } finally {
                m_delegateExecutionManager.leaveDelegateExecution();
            }
        }
    }

    private void startStep(final boolean stepIsRestoredFromSavedGame) {
        if (!stepIsRestoredFromSavedGame) {
            if (m_data.getSequence().getStep().getDelegate().getClass().isAnnotationPresent(AutoSave.class)) {
                if (m_data.getSequence().getStep().getDelegate().getClass().getAnnotation(AutoSave.class).beforeStepStart()) autoSave();
            }
        }
        final DefaultDelegateBridge bridge = new DefaultDelegateBridge(m_data, this, new DelegateHistoryWriter(m_channelMessenger), m_randomStats, m_delegateExecutionManager);
        CachedInstanceCenter.CachedDelegateBridge = bridge;
        CachedInstanceCenter.CachedGameData = m_data;
        if (m_delegateRandomSource == null) {
            m_delegateRandomSource = (IRandomSource) m_delegateExecutionManager.createOutboundImplementation(m_randomSource, new Class[] { IRandomSource.class });
        }
        bridge.setRandomSource(m_delegateRandomSource);
        if (m_needToInitialize) {
            addPlayerTypesToGameData(m_gamePlayers.values(), m_players, bridge);
        }
        notifyGameStepChanged(stepIsRestoredFromSavedGame);
        m_delegateExecutionManager.enterDelegateExecution();
        try {
            getCurrentStep().getDelegate().start(bridge);
        } finally {
            m_delegateExecutionManager.leaveDelegateExecution();
        }
    }

    private void waitForPlayerToFinishStep() {
        final PlayerID playerID = getCurrentStep().getPlayerID();
        if (playerID == null) return;
        final IGamePlayer player = m_gamePlayers.get(playerID);
        if (player != null) {
            player.start(getCurrentStep().getName());
        } else {
            final INode destination = m_players.getNode(playerID.getName());
            final IGameStepAdvancer advancer = (IGameStepAdvancer) m_remoteMessenger.getRemote(ClientGame.getRemoteStepAdvancerName(destination));
            advancer.startPlayerStep(getCurrentStep().getName(), playerID);
        }
    }

    public void addGameStepListener(final GameStepListener listener) {
        m_gameStepListeners.add(listener);
    }

    public void removeGameStepListener(final GameStepListener listener) {
        m_gameStepListeners.remove(listener);
    }

    private void notifyGameStepChanged(final boolean loadedFromSavedGame) {
        final String stepName = getCurrentStep().getName();
        final String delegateName = getCurrentStep().getDelegate().getName();
        final String displayName = getCurrentStep().getDisplayName();
        final PlayerID id = getCurrentStep().getPlayerID();
        getGameModifiedBroadcaster().stepChanged(stepName, delegateName, id, m_data.getSequence().getRound(), displayName, loadedFromSavedGame);
        final Iterator<GameStepListener> iter = m_gameStepListeners.iterator();
        while (iter.hasNext()) {
            final GameStepListener listener = iter.next();
            listener.gameStepChanged(stepName, delegateName, id, m_data.getSequence().getRound(), getCurrentStep().getDisplayName());
        }
    }

    private void addPlayerTypesToGameData(final Collection<IGamePlayer> localPlayers, final PlayerManager allPlayers, final IDelegateBridge aBridge) {
        final GameData data = aBridge.getData();
        if (getCurrentStep() == null || getCurrentStep().getPlayerID() == null || (m_firstRun)) {
            m_firstRun = false;
            return;
        }
        final CompositeChange change = new CompositeChange();
        final Set<String> allPlayersString = allPlayers.getPlayers();
        aBridge.getHistoryWriter().startEvent("Game Loaded");
        for (final IGamePlayer player : localPlayers) {
            allPlayersString.remove(player.getName());
            final boolean isHuman = player instanceof TripleAPlayer;
            aBridge.getHistoryWriter().addChildToEvent(player.getName() + ((player.getName().endsWith("s") || player.getName().endsWith("ese") || player.getName().endsWith("ish")) ? " are" : " is") + " now being played by: " + player.getType());
            final PlayerID p = data.getPlayerList().getPlayerID(player.getName());
            final String newWhoAmI = ((isHuman ? "Human" : "AI") + ":" + player.getType());
            if (!p.getWhoAmI().equals(newWhoAmI)) change.add(ChangeFactory.changePlayerWhoAmIChange(p, newWhoAmI));
        }
        final Iterator<String> playerIter = allPlayersString.iterator();
        while (playerIter.hasNext()) {
            final String player = playerIter.next();
            playerIter.remove();
            aBridge.getHistoryWriter().addChildToEvent(player + ((player.endsWith("s") || player.endsWith("ese") || player.endsWith("ish")) ? " are" : " is") + " now being played by: Human:Client");
            final PlayerID p = data.getPlayerList().getPlayerID(player);
            final String newWhoAmI = "Human:Client";
            if (!p.getWhoAmI().equals(newWhoAmI)) change.add(ChangeFactory.changePlayerWhoAmIChange(p, newWhoAmI));
        }
        if (!change.isEmpty()) aBridge.addChange(change);
        m_needToInitialize = false;
        if (!allPlayersString.isEmpty()) throw new IllegalStateException("Not all Player Types (ai/human/client) could be added to game data.");
    }

    public IMessenger getMessenger() {
        return m_messenger;
    }

    public IChannelMessenger getChannelMessenger() {
        return m_channelMessenger;
    }

    public IRemoteMessenger getRemoteMessenger() {
        return m_remoteMessenger;
    }

    private IGameModifiedChannel getGameModifiedBroadcaster() {
        return (IGameModifiedChannel) m_channelMessenger.getChannelBroadcastor(IGame.GAME_MODIFICATION_CHANNEL);
    }

    public void addChange(final Change aChange) {
        getGameModifiedBroadcaster().gameDataChanged(aChange);
    }

    public boolean canSave() {
        return true;
    }

    public IRandomSource getRandomSource() {
        return m_randomSource;
    }

    public void setRandomSource(final IRandomSource randomSource) {
        m_randomSource = randomSource;
        m_delegateRandomSource = null;
    }

    public Vault getVault() {
        return m_vault;
    }

    private final IGameModifiedChannel m_gameModifiedChannel = new IGameModifiedChannel() {

        public void gameDataChanged(final Change aChange) {
            assertCorrectCaller();
            m_changePerformer.perform(aChange);
            m_data.getHistory().getHistoryWriter().addChange(aChange);
        }

        private void assertCorrectCaller() {
            if (!MessageContext.getSender().equals(m_messenger.getServerNode())) {
                throw new IllegalStateException("Only server can change game data");
            }
        }

        public void startHistoryEvent(final String event) {
            assertCorrectCaller();
            m_data.getHistory().getHistoryWriter().startEvent(event);
        }

        public void addChildToEvent(final String text, final Object renderingData) {
            assertCorrectCaller();
            m_data.getHistory().getHistoryWriter().addChildToEvent(new EventChild(text, renderingData));
        }

        public void setRenderingData(final Object renderingData) {
            assertCorrectCaller();
            m_data.getHistory().getHistoryWriter().setRenderingData(renderingData);
        }

        public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName, final boolean loadedFromSavedGame) {
            assertCorrectCaller();
            if (loadedFromSavedGame) return;
            m_data.getHistory().getHistoryWriter().startNextStep(stepName, delegateName, player, displayName);
        }

        public void shutDown() {
        }
    };

    public void addDisplay(final IDisplay display) {
        display.initialize(new DefaultDisplayBridge(m_data));
        m_channelMessenger.registerChannelSubscriber(display, ServerGame.getDisplayChannel(getData()));
    }

    public void removeDisplay(final IDisplay display) {
        m_channelMessenger.unregisterChannelSubscriber(display, ServerGame.getDisplayChannel(getData()));
    }

    public boolean isGameOver() {
        return m_isGameOver;
    }

    public PlayerManager getPlayerManager() {
        return m_players;
    }

    public InGameLobbyWatcher getInGameLobbyWatcher() {
        return m_inGameLobbyWatcher;
    }

    public void setInGameLobbyWatcher(final InGameLobbyWatcher inGameLobbyWatcher) {
        m_inGameLobbyWatcher = inGameLobbyWatcher;
    }

    public void stopGameSequence() {
        m_delegateExecutionStopped = true;
    }

    public boolean isGameSequenceRunning() {
        return !m_delegateExecutionStopped;
    }
}

interface IServerRemote extends IRemote {

    public byte[] getSavedGame();
}

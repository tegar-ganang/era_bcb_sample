package games.strategy.engine.lobby.server;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.lobby.server.headless.HeadlessLobbyConsole;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.ui.LobbyAdminConsole;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.triplea.util.LoggingPrintStream;
import games.strategy.util.Version;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * LobbyServer.java
 * 
 * Created on May 23, 2006, 6:44 PM
 * 
 * @author Harry
 */
public class LobbyServer {

    private static final String PORT = "triplea.lobby.port";

    private static final String UI = "triplea.lobby.ui";

    private static final String CONSOLE = "triplea.lobby.console";

    public static final String ADMIN_USERNAME = "Admin";

    private static final Logger s_logger = Logger.getLogger(LobbyServer.class.getName());

    public static final String LOBBY_CHAT = "_LOBBY_CHAT";

    public static final Version LOBBY_VERSION = new Version(1, 0, 0);

    private final Messengers m_messengers;

    /** Creates a new instance of LobbyServer */
    public LobbyServer(final int port) {
        IServerMessenger server;
        try {
            server = new ServerMessenger(ADMIN_USERNAME, port);
        } catch (final IOException ex) {
            s_logger.log(Level.SEVERE, ex.toString());
            throw new IllegalStateException(ex.getMessage());
        }
        m_messengers = new Messengers(server);
        server.setLoginValidator(new LobbyLoginValidator());
        new ChatController(LOBBY_CHAT, m_messengers);
        final StatusManager statusManager = new StatusManager(m_messengers);
        statusManager.shutDown();
        new UserManager().register(m_messengers.getRemoteMessenger());
        new ModeratorController(server).register(m_messengers.getRemoteMessenger());
        final LobbyGameController controller = new LobbyGameController((ILobbyGameBroadcaster) m_messengers.getChannelMessenger().getChannelBroadcastor(ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL), server);
        controller.register(m_messengers.getRemoteMessenger());
        server.setAcceptNewConnections(true);
    }

    private static void setUpLogging() {
        try {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("server-logging.properties"));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        Logger.getAnonymousLogger().info("Redirecting std out");
        System.setErr(new LoggingPrintStream("ERROR", Level.SEVERE));
        System.setOut(new LoggingPrintStream("OUT", Level.INFO));
    }

    public static void main(final String args[]) {
        try {
            final InputStream in = System.in;
            final PrintStream out = System.out;
            setUpLogging();
            final int port = Integer.parseInt(System.getProperty(PORT, "3303"));
            System.out.println("Trying to listen on port:" + port);
            final LobbyServer server = new LobbyServer(port);
            System.out.println("Starting database");
            Database.getConnection().close();
            s_logger.info("Lobby started");
            if (Boolean.parseBoolean(System.getProperty(UI, "false"))) {
                startUI(server);
            }
            if (Boolean.parseBoolean(System.getProperty(CONSOLE, "false"))) {
                startConsole(server, in, out);
            }
        } catch (final Exception ex) {
            s_logger.log(Level.SEVERE, ex.toString(), ex);
        }
    }

    private static void startConsole(final LobbyServer server, final InputStream in, final PrintStream out) {
        System.out.println("starting console");
        new HeadlessLobbyConsole(server, in, out).start();
    }

    private static void startUI(final LobbyServer server) {
        System.out.println("starting ui");
        final LobbyAdminConsole console = new LobbyAdminConsole(server);
        console.setSize(800, 700);
        console.setLocationRelativeTo(null);
        console.setVisible(true);
    }

    public IServerMessenger getMessenger() {
        return (IServerMessenger) m_messengers.getMessenger();
    }

    public Messengers getMessengers() {
        return m_messengers;
    }
}

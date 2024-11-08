package cubeworld;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import cubeworld.command.ChannelTalk;
import cubeworld.command.Exit;
import cubeworld.command.WhoAmI;

/**
 * @author Garg Oyle (garg_oyle@users.sourceforge.net)
 */
public class Server {

    /**
     * Accept timeout.
     */
    private static final int TIMEOUT = 10;

    /**
     * Highest valid port number.
     */
    private static final int MAX_PORT = 65535;

    /**
     * Server socket.
     */
    private ServerSocket mServerSocket;

    /**
     * Client sockets.
     */
    private ArrayList<SocketSession> mSessions = new ArrayList<SocketSession>();

    /**
     * Old client sockets.
     */
    private ArrayList<SocketSession> mOldSessions = new ArrayList<SocketSession>();

    /**
     * Known channels.
     */
    private HashMap<String, Channel> mChannels = new HashMap<String, Channel>();

    /**
     * Available commands.
     */
    private ArrayList<Command> mCommands = new ArrayList<Command>();

    /**
     * @param args command line parameters
     */
    public static void main(final String[] args) {
        Server server = new Server();
        server.run(args);
    }

    /**
     * Run the server with given parameters.
     *
     * @param args parameters
     */
    public final void run(final String[] args) {
        if (!commandLineParametersOk(args)) {
            System.err.println("usage: java -jar cubeworld.jar <port>");
            System.exit(-1);
        }
        try {
            setUp(Integer.parseInt(args[0]));
            System.out.println("setUp finished - entering main loop.");
            mainLoop();
            tearDown();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Setup the server.
     *
     * @param port port for listening for incoming connections.
     * @throws IOException in case of communication failures
     */
    private void setUp(final int port) throws IOException {
        mServerSocket = new ServerSocket(port);
        mServerSocket.setReuseAddress(true);
        mServerSocket.setSoTimeout(TIMEOUT);
        registerChannels();
        registerCommands();
    }

    /**
     * Register available standard commands.
     */
    private void registerCommands() {
        mCommands.add(new Exit());
        mCommands.add(new ChannelTalk(mChannels));
        mCommands.add(new WhoAmI());
    }

    /**
     * Register known channels.
     */
    private void registerChannels() {
        registerChannel("cubeworld");
        registerChannel("mentor");
        registerChannel("info");
    }

    /**
     * Register a channel to make it known to the world.
     * @param name channel's name
     */
    public final void registerChannel(final String name) {
        if (!mChannels.containsKey(name)) {
            Channel channel = new Channel(name);
            try {
                Logger channelLogger = channel.getLogger();
                FileHandler handler = new FileHandler("channel-" + name + ".log", true);
                handler.setFormatter(new CubeworldChannelFormatter());
                channelLogger.addHandler(handler);
                channelLogger.setUseParentHandlers(false);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mChannels.put(name, channel);
        }
    }

    /**
     * Main loop.
     *
     * @throws IOException in case of communication failures
     */
    private void mainLoop() throws IOException {
        boolean stop = false;
        while (!stop) {
            acceptNew();
            readClients();
            processWorld();
            writeClients();
            dismissOld();
        }
    }

    /**
     * Processing the world.
     */
    private void processWorld() {
        for (SocketSession session : mSessions) {
            if (!session.isVerified()) {
                initializeSession(session);
            } else {
                processSession(session);
            }
            if (!session.isAlive()) {
                removeSession(session);
            }
        }
    }

    /**
     * Initialize the session.
     * @param session to initialize
     */
    private void initializeSession(final SocketSession session) {
        for (String line : session.getInput()) {
            String normalizedLine = line.trim();
            if (0 == normalizedLine.length()) {
                return;
            }
            if (null == session.getName()) {
                session.setName(normalizedLine);
                session.addOutput("What's your password " + session.getName() + "? ");
            } else {
                Player player = loadPlayer(session.getName(), normalizedLine);
                if (null != player) {
                    session.setVerified(true);
                    session.setPlayer(player);
                    session.addOutput("Hello " + session.getName() + "! Welcome to the cubeworld.");
                    mChannels.get("info").send(session.getName() + " enters cubeworld.");
                    removeDuplicateSession(session);
                } else {
                    session.addOutput("Invalid name or password.");
                    removeSession(session);
                }
            }
        }
    }

    /**
     * Load a player with given name and password.
     *
     * @param name player's name
     * @param password it's password
     * @return the player or null if no such player or invalid password
     */
    private Player loadPlayer(final String name, final String password) {
        if (null != password && password.equals("11111")) {
            Player player = new Player(name);
            return player;
        }
        return null;
    }

    /**
     * Remove duplicate session.
     * @param newSession new session.
     */
    private void removeDuplicateSession(final SocketSession newSession) {
        for (SocketSession session : mSessions) {
            if (session.getName().equals(newSession.getName()) && !session.equals(newSession)) {
                session.addOutput("Take over from other connection.");
                removeSession(session);
            }
        }
    }

    /**
     * Process a single session.
     * @param session to process
     */
    private void processSession(final Session session) {
        for (String line : session.getInput()) {
            boolean processed = false;
            String normalizedLine = line.trim();
            if (0 == normalizedLine.length()) {
                continue;
            }
            for (Command command : mCommands) {
                if (command.process(normalizedLine, session)) {
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                session.addOutput("unknown command.");
            }
        }
    }

    /**
     * Indicate session as old for removal.
     *
     * @param session the session to remove
     */
    private void removeSession(final SocketSession session) {
        if (!mOldSessions.contains(session)) {
            mOldSessions.add(session);
            mChannels.get("info").send(session.getName() + " closed session.");
            for (String channelName : session.getChannels()) {
                mChannels.get(channelName).remove(session);
            }
        }
    }

    /**
     * Accept new incoming connections.
     *
     * @throws IOException in case of communication failures.
     */
    private void acceptNew() throws IOException {
        try {
            Socket socket = mServerSocket.accept();
            socket.setSoTimeout(TIMEOUT);
            SocketSession session = new SocketSession(socket);
            session.addOutput("What's your name? ");
            mSessions.add(session);
            mChannels.get("info").register(session);
        } catch (SocketTimeoutException e) {
            assert true;
        }
    }

    /**
     * Try to read from clients.
     */
    private void readClients() {
        for (SocketSession session : mSessions) {
            try {
                session.read();
            } catch (IOException e) {
                removeSession(session);
            }
        }
    }

    /**
     * Try to write to clients.
     */
    private void writeClients() {
        for (SocketSession session : mSessions) {
            try {
                session.write();
            } catch (IOException e) {
                removeSession(session);
            }
        }
    }

    /**
     * Dismiss closed connections.
     */
    private void dismissOld() {
        for (SocketSession session : mOldSessions) {
            try {
                session.getSocket().close();
            } catch (IOException e) {
                assert true;
            }
            mSessions.remove(session);
        }
        mOldSessions.clear();
    }

    /**
     * Clean up.
     *
     * @throws IOException in case of communication failures.
     */
    private void tearDown() throws IOException {
        for (SocketSession session : mSessions) {
            session.getSocket().close();
        }
        mServerSocket.close();
    }

    /**
     * Verify command line parameters.
     *
     * @param args to check
     * @return true if valid parameters
     */
    protected static boolean commandLineParametersOk(final String[] args) {
        if (null == args) {
            return false;
        }
        if (1 != args.length) {
            return false;
        }
        if (!isPortNumber(args[0])) {
            return false;
        }
        return true;
    }

    /**
     * Assert the parameter to be a valid port number.
     *
     * @param parameter to check
     * @return true if valid
     */
    protected static boolean isPortNumber(final String parameter) {
        try {
            int portNumber = Integer.parseInt(parameter);
            if (0 >= portNumber || MAX_PORT < portNumber) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}

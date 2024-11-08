package cz.xf.tomason.tictactoe.multiplayer.server;

import cz.xf.tomason.tictactoe.Player;
import cz.xf.tomason.tictactoe.exceptions.network.NetworkException;
import cz.xf.tomason.tictactoe.util.Constants;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final int SOCKET_TO = 100;

    private static final int PORT = Constants.PORT;

    private ServerSocket socket;

    private boolean running;

    private PlayerPoll poll;

    private Thread connectionThread;

    private Set<ClientHandler> clientThreads;

    private Logger log;

    public Server() {
        running = false;
        clientThreads = new HashSet<ClientHandler>();
        poll = new PlayerPoll();
        log = Logger.getLogger("serverLog");
        log.setLevel(Level.ALL);
    }

    public void startServer() {
        log.info("Starting server");
        if (socket != null || connectionThread != null) {
            log.severe("Exception: socket or thread already exist");
            return;
        }
        try {
            socket = new ServerSocket(PORT);
            running = true;
            connectionThread = new Thread(new Connector());
            connectionThread.start();
            log.info("Server started");
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            log.severe("Exception: " + ex.getMessage());
        }
    }

    public void stopServer() {
        stopServer(2 * SOCKET_TO);
    }

    public void stopServer(long timeout) {
        log.info("Stopping server...");
        if (socket == null || connectionThread == null) {
            log.severe("Exception: no socket or thread");
            return;
        }
        try {
            running = false;
            connectionThread.join(timeout);
            for (ClientHandler ch : clientThreads) {
                ch.sendServerDown();
                ch.join(timeout);
            }
            Thread.sleep(timeout);
            socket.close();
            connectionThread = null;
            socket = null;
            log.info("Successfuly stopped");
        } catch (InterruptedException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            log.severe("Exception: " + ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            log.severe("Exception: " + ex.getMessage());
        }
    }

    public void printStatus(OutputStream os) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.write("--- SERVER STATUS ---");
        bw.newLine();
        bw.write(" - status: ");
        bw.write(running ? "running" : "stopped");
        bw.newLine();
        bw.write(" - connection thread: ");
        bw.write(connectionThread == null ? "null" : connectionThread.toString());
        bw.newLine();
        bw.write(" - client threads:");
        bw.newLine();
        bw.write("  - count: ");
        bw.write(Integer.toString(clientThreads.size()));
        bw.newLine();
        for (Thread t : clientThreads) {
            bw.write("  - ");
            bw.write(t.toString());
            bw.newLine();
        }
        bw.write("--- SERVER STATUS ---");
        bw.newLine();
        bw.flush();
    }

    public void addClientHandler(ClientHandler client) {
        log.info("adding client thread");
        clientThreads.add(client);
    }

    public void removeClientHandler(ClientHandler client) {
        log.info("removing client thread");
        clientThreads.remove(client);
    }

    public Set<Player> getPlayerSet() {
        log.info("getPlayersSet called");
        return poll.getPlayers();
    }

    public String getPlayerString() {
        log.info("getPlayersString called");
        return poll.getPlayersString();
    }

    public void registerPlayer(Player player, ClientHandler handler) throws NetworkException {
        log.info("registerPlayer called");
        poll.addPlayer(player, handler);
    }

    public void unregisterPlayer(Player player) throws NetworkException {
        log.info("unregisterPlayer called");
        poll.removePlayer(player);
    }

    public ClientHandler getHandler(Player player) throws NetworkException {
        log.info("getHandler called");
        return poll.getHandler(player);
    }

    private class Connector implements Runnable {

        public void run() {
            try {
                socket.setSoTimeout(SOCKET_TO);
            } catch (SocketException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                log.severe("Exception: " + ex.getMessage());
            }
            while (running) {
                try {
                    new ClientHandler(socket.accept(), Server.this).start();
                } catch (SocketTimeoutException ex) {
                } catch (Exception ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    log.severe("Exception: " + ex.getMessage());
                }
            }
        }
    }
}

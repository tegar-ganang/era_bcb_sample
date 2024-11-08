package com.pz.net.master;

import com.pz.net.Command;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.TreeMap;
import static com.pz.util.Log.d;
import static com.pz.net.Command.*;
import static com.pz.util.ByteBufferHelper.*;

/**
 *
 * @author jannek
 */
public class MasterServer extends Thread {

    private static PrintStream out = System.out;

    private DatagramChannel udpChannel;

    private DatagramSocket udpSocket;

    private ByteBuffer udpData;

    private ServerSocket tcpSocket;

    private ByteBuffer tcpData;

    private long lastServerListUpdate;

    protected final ByteBuffer serverList;

    private final ByteBuffer udpUpdate;

    private final ByteBuffer udpNotification;

    private boolean running;

    private final Map<Integer, Client> all;

    private final Map<Integer, Client> players;

    private final Map<String, Client> byName;

    private final Map<Integer, Server> servers;

    private final PriorityQueue<Client> byTtl;

    private final LinkedList<Server> sendNotification;

    public static void main(String[] args) {
        File confirm = new File("confirmstop.txt");
        if (confirm.exists()) confirm.delete();
        MasterServer ms = null;
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 13370;
        try {
            ms = new MasterServer(port);
            ms.start();
            ms.tcpRun();
        } catch (SocketException ex) {
            d("Port " + port + " is not available.\n" + ex.getMessage());
        } catch (IOException ex) {
            ex.printStackTrace(out);
        } finally {
            if (ms != null) ms.close();
        }
        d("Goodbye.");
    }

    /**
     * Creates a new master server, listening with both UDP and TCP on the
     * provided port.
     * @param port Both UDP and TCP
     * @throws IOException
     */
    public MasterServer(int port) throws IOException {
        udpChannel = DatagramChannel.open();
        udpChannel.configureBlocking(true);
        udpSocket = udpChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        udpSocket.bind(address);
        udpData = ByteBuffer.allocate(50);
        udpUpdate = ByteBuffer.allocate(1);
        put(MASTER_UPDATE_UDP, udpUpdate);
        udpNotification = ByteBuffer.allocate(1);
        put(UPDATE_NOTIFICATION, udpNotification);
        tcpSocket = new ServerSocket();
        tcpSocket.bind(address);
        tcpData = ByteBuffer.allocate(200);
        serverList = ByteBuffer.allocate(10000);
        all = new TreeMap<Integer, Client>();
        players = new TreeMap<Integer, Client>();
        byName = new HashMap<String, Client>();
        servers = new TreeMap<Integer, Server>();
        byTtl = new PriorityQueue<Client>();
        sendNotification = new LinkedList<Server>();
        updateServerList();
        running = true;
        d("Server created on port " + port);
    }

    public Client getClient(int id) {
        if (!all.containsKey(id)) return null;
        return all.get(id);
    }

    public Client getPlayer(int id) {
        if (!players.containsKey(id)) return null;
        return players.get(id);
    }

    public synchronized Client createPlayer(String name) {
        if (byName.containsKey(name)) return null;
        Client player = new Client(name);
        players.put(player.id, player);
        byName.put(name, player);
        return player;
    }

    public synchronized Server createServer(String name, String game, short maxPlayers, byte[] data) {
        if (byName.containsKey(name)) return null;
        Server server = new Server(name, game, maxPlayers, data);
        servers.put(server.id, server);
        byName.put(name, server);
        return server;
    }

    public Server getServer(int id) {
        if (!servers.containsKey(id)) return null;
        return servers.get(id);
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Makes sure that the server is shutting down properly.
     */
    public void close() {
        try {
            running = false;
            udpChannel.close();
            tcpSocket.close();
            d("MS has been closed.");
        } catch (IOException ex) {
            d(ex);
        }
    }

    public void updateServerList() {
        if (lastServerListUpdate + 10000 > System.currentTimeMillis()) return;
        synchronized (serverList) {
            serverList.clear();
            serverList.put("Hello biatch!!".getBytes());
            serverList.flip();
        }
    }

    /**
     * The TCP server loop, waits for a new connection, and creates a connection
     * object in a new thread, to handle the client.
     */
    public void tcpRun() {
        while (isRunning() && !tcpSocket.isClosed()) {
            try {
                Connection.start(this, tcpSocket.accept().getChannel());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * The UDP server loop, takes care of keeping track of the UDP ports of the
     * players and servers, and sending notifications to the servers, when a new
     * player connects.
     */
    @Override
    public void run() {
        SocketAddress from;
        Client client;
        long loopCount = 0;
        while (isRunning()) {
            d("run.");
            udpData.clear();
            from = null;
            client = null;
            loopCount++;
            do {
                if (loopCount % 100000 == 0) d("loop count = " + (loopCount / 100000) + "e5");
                if (loopCount % 100 == 0) {
                    sendNotification();
                }
                sendUpdateRequest();
                try {
                    from = udpChannel.receive(udpData);
                } catch (IOException ex) {
                    d("udp read " + ex.getMessage());
                    continue;
                }
            } while (from == null);
            udpData.flip();
            d(from);
            d(new String(udpData.array(), 0, udpData.limit()));
            switch(getCommand(udpData)) {
                case LINK_UDP_UPDATE_CLIENT:
                    client = getClient(udpData.getInt());
                    break;
                case UPDATE_PLAYER:
                    client = getPlayer(udpData.getInt());
                    break;
                case LINK_UDP_UPDATE_SERVER:
                    client = getServer(udpData.getInt());
                    break;
            }
            if (client == null || !client.isKey(udpData.getInt())) continue;
            client.setAddress(from);
            byTtl.remove(client);
        }
        close();
    }

    private void sendUpdateRequest() {
        if (!byTtl.isEmpty() && byTtl.peek().nextUpdate < System.currentTimeMillis() - 30000) {
            Client client = byTtl.poll();
            try {
                udpUpdate.clear();
                udpChannel.send(udpUpdate, client.getAddress());
            } catch (IOException ex) {
                d("udp update " + ex.getMessage());
            }
            client.updateSent = true;
            byTtl.offer(client);
            client.updateSent = false;
        }
    }

    /**
     * send a notification to a server, if the server should check for updates.
     */
    private void sendNotification() {
        synchronized (sendNotification) {
            if (!sendNotification.isEmpty()) {
                try {
                    udpNotification.clear();
                    udpChannel.send(udpNotification, sendNotification.getFirst().getAddress());
                } catch (IOException ex) {
                    d("udp notification " + ex.getMessage());
                }
                sendNotification.add(sendNotification.remove());
            }
        }
    }

    protected void addNotification(Server server) {
        synchronized (sendNotification) {
            if (!sendNotification.contains(server)) sendNotification.add(server);
        }
    }

    protected void removeNotification(Server server) {
        synchronized (sendNotification) {
            if (sendNotification.contains(server)) sendNotification.remove(server);
        }
    }

    private boolean isSuperUser(Client client) {
        if (client == null) return false;
        d("Super user #" + client.id + " " + client.name);
        return true;
    }

    /**
     * Reads input from a file placed next to the master server.
     * @param client
     */
    protected void readInputFromFile(Client client) {
        if (!isSuperUser(client)) {
            d("User #" + client.id + " " + client.name + " is not a super user");
            return;
        }
        File confirm = new File("msinput.txt");
        if (confirm.exists()) {
            Scanner scanner;
            try {
                scanner = new Scanner(confirm);
            } catch (FileNotFoundException ex) {
                d(ex);
                d("Failed input attempt from " + client.getAddress());
                return;
            }
            while (scanner.hasNextLine()) {
                if (scanner.nextLine().equals("stopserver")) {
                    d("Server shutting down.");
                    running = false;
                }
            }
            scanner.close();
            confirm.delete();
        }
    }
}

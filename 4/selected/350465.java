package server;

import help.AdventureHelp;
import help.Help;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import player.PlayerManager;
import player.PlayerManagerImp;
import server.util.Broadcaster;
import server.util.BroadcasterImp;
import server.util.Ticker;
import server.util.TickerImp;
import server.util.tickevents.*;
import adventure.Adventure;
import adventure.AdventureEdit;
import adventure.AdventureInput;
import adventure.AdventureInputImp;
import channel.master.MasterChannelTest;
import colour.Colour;
import console.IOBuffer;

/**
 *
 * @author Michael Hanns
 *
 */
public class NetworkServerImp extends Thread implements NetworkServer {

    private double version = 1.0;

    private String serverName;

    private int serverPort;

    private int maxCapacity;

    private boolean hostingServer;

    private ServerSocket server;

    private IOBuffer terminal;

    private NetworkPreferences netprefs;

    private ExecutorService exec;

    private ArrayList<NetworkClientThread> playerThreads;

    private PlayerManager players;

    private Adventure adv;

    private AdventureInput advInp;

    private Broadcaster broadcaster;

    private Ticker serverTicker;

    private Help advHelp;

    private Help shellHelp;

    public NetworkServerImp(IOBuffer term, NetworkPreferences prefs, Help shHelp, double vers) {
        terminal = term;
        hostingServer = false;
        netprefs = prefs;
        shellHelp = shHelp;
        playerThreads = new ArrayList<NetworkClientThread>();
        broadcaster = new BroadcasterImp();
        this.version = vers;
    }

    @Override
    public void run() {
        acceptConnections();
    }

    private void acceptConnections() {
        try {
            while (hostingServer) {
                Socket newCon = server.accept();
                removeIdleThreads();
                if (players.onlinePlayers() >= maxCapacity) {
                    PrintWriter out = new PrintWriter(newCon.getOutputStream(), true);
                    out.println("WITNA Server v" + Colour.colourise(version + "", Colour.green) + ", you are playing " + Colour.colourise(adv.getName(), Colour.yellow) + " v" + Colour.colourise(adv.getVersion() + "", Colour.yellow) + ".\n");
                    out.println("This server is currently full at the moment. Please try again later.");
                    newCon.close();
                    newCon = null;
                } else {
                    final NetworkClientThread newGame = new NetworkClientThreadImp(newCon, advInp, players, broadcaster, netprefs.getMOTD(), maxCapacity, version);
                    Runnable task = new Runnable() {

                        @Override
                        public void run() {
                            newGame.start();
                        }
                    };
                    playerThreads.add(newGame);
                    exec.submit(task);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeIdleThreads() {
        for (int x = 0; x < playerThreads.size(); x++) {
            if (playerThreads.get(x).disconnected()) {
                playerThreads.get(x).disconnect(false);
                playerThreads.remove(x);
                x--;
            }
        }
    }

    @Override
    public void processLocalCommand(String input) {
        if (input.equalsIgnoreCase("DISCONNECT") || input.equalsIgnoreCase("QUIT")) {
            disconnect();
        } else if (input.toUpperCase().startsWith("MOTD")) {
            if (input.length() == 4) {
                terminal.writeTo("\nSet the Message of the Day to what?");
            } else if (input.length() == 5 && input.charAt(4) == ' ') {
                terminal.writeTo("\nSet the Message of the Day to what?");
            } else if (input.length() > 5 && input.charAt(4) == ' ') {
                setMOTD(Colour.colourise(input.substring(5), Colour.yellow) + Colour.colourise("", Colour.grey));
            }
        } else if (input.toUpperCase().startsWith("SERVERMSG")) {
            if (input.length() == 9) {
                terminal.writeTo("\nYou need to enter a server message to broadcast.");
            } else if (input.length() == 10 && input.charAt(9) == ' ') {
                terminal.writeTo("\nYou need to enter a server message to broadcast.");
            } else if (input.length() > 10 && input.charAt(9) == ' ') {
                broadcaster.broadcastServerMessage(input.substring(10));
                terminal.writeTo("\nSERVERMESSAGE sent: '" + Colour.colourise(input.substring(10), Colour.white) + Colour.colourise("'", Colour.grey));
            }
        } else if (input.toUpperCase().startsWith("CHANNELJOIN")) {
            if (input.length() == 11) {
                terminal.writeTo("\nChannel not specified.\nSyntax: channeljoin <channelipaddress>");
            } else if (input.length() == 12 && input.charAt(11) == ' ') {
                terminal.writeTo("\nChannel not specified.\nSyntax: channeljoin <channelipaddress>");
            } else if (input.length() > 12 && input.charAt(11) == ' ') {
                String channelIP = input.substring(12).trim();
                setChannel(channelIP);
            }
        } else if (input.toUpperCase().startsWith("SERVERJOIN")) {
            if (input.length() == 11 || (input.length() == 12 && input.charAt(11) == ' ') || (input.length() > 12 && input.charAt(11) == ' ')) {
                terminal.writeTo("\nYou are currently hosting a server in this terminal. " + "If you wish to join a server, please open a new instance of WITNA " + "or disconnect the server first.");
            }
        } else if (input.equalsIgnoreCase("SAVE")) {
            if (players.savePlayers()) {
                terminal.writeTo("\n" + Colour.colourise("Player status saved.", Colour.green) + Colour.colourise("", Colour.grey));
            } else {
                terminal.writeTo("\nERROR: Player status not saved. Cannot create file. Ensure WITNA is not installed to a read-only directory.");
            }
        } else if (input.startsWith("HELP")) {
            if (input.length() == 4) {
                help("");
            } else if (input.length() == 5 && input.charAt(11) == ' ') {
                help("");
            } else if (input.length() > 5 && input.charAt(11) == ' ') {
                help(input.substring(5));
            }
        } else if (input.startsWith("?")) {
            if (input.length() == 1) {
                help("");
            } else if (input.length() == 2 && input.charAt(11) == ' ') {
                help("");
            } else if (input.length() > 2 && input.charAt(11) == ' ') {
                help(input.substring(2));
            }
        } else {
            terminal.writeTo("\nServer command not recognised.");
        }
    }

    @Override
    public boolean hostServer(String name, String advPath, int capacity, int port) {
        try {
            serverName = name;
            maxCapacity = capacity;
            serverPort = port;
            server = new ServerSocket(serverPort);
            exec = Executors.newFixedThreadPool(maxCapacity + 1);
            adv = filehandling.Opening.readFromFile(advPath);
        } catch (IOException e) {
            return false;
        }
        if (adv != null) {
            players = new PlayerManagerImp(netprefs.getPlayerFilePath(), adv);
            ((AdventureEdit) adv).setPlayerManager(players);
            ((AdventureEdit) adv).setBroadcaster(broadcaster);
            advInp = new AdventureInputImp(adv);
            setupTicker();
            if (((AdventureEdit) adv).ready()) {
                terminal.writeTo("\nAdventure file read, starting server...\n");
                terminal.writeTo(Colour.colourise("\nWITNA Server v" + version, Colour.green));
                terminal.writeTo(Colour.colourise("\n-----", Colour.white));
                terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Server      : ", Colour.green) + Colour.colourise(serverName + "/" + adv.getName() + " v" + adv.getVersion(), Colour.yellow));
                terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Global ip   : ", Colour.green));
                final int serverPort = port;
                Runnable task = new Runnable() {

                    @Override
                    public void run() {
                        String globalip = getGlobalIP();
                        String localip = getLocalIP();
                        terminal.writeTo(Colour.colourise(globalip + ":" + serverPort, Colour.yellow));
                        terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Local  ip   : ", Colour.green) + Colour.colourise(localip + ":" + serverPort, Colour.yellow));
                        terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Max capacity: ", Colour.green) + Colour.colourise(maxCapacity + "", Colour.yellow));
                        String status = getNetStatus(globalip, localip);
                        terminal.writeTo(Colour.colourise("\n-", Colour.white) + Colour.colourise("Connection  : ", Colour.green) + Colour.colourise(status, Colour.yellow) + Colour.colourise("\n", Colour.grey));
                        advHelp = new AdventureHelp(terminal);
                        ((AdventureEdit) adv).setHelpFile(advHelp);
                    }
                };
                new Thread(task).start();
                hostingServer = true;
                return true;
            } else {
                terminal.writeTo("\nInternal error: PlayerManager or Broadcaster not yet set!");
                return true;
            }
        } else {
            terminal.writeTo("\nAdventure file not found. Cancelling serverhost..");
            return false;
        }
    }

    private void setupTicker() {
        Runnable ticker = new Runnable() {

            @Override
            public void run() {
                serverTicker = new TickerImp(adv, broadcaster, players);
                serverTicker.addTickEvent(new CleanupRooms(broadcaster, ((AdventureEdit) adv).getRoomManager()));
                serverTicker.addRoundEvent(new ExecuteCombat(adv));
                serverTicker.addTickEvent(new Repopulate(broadcaster, ((AdventureEdit) adv).getRoomManager(), players));
                serverTicker.addTickEvent(new RegeneratePlayers(players));
                serverTicker.addTickEvent(new SavePlayers(players));
                serverTicker.startTicking();
            }
        };
        exec.submit(ticker);
    }

    @Override
    public void disconnect() {
        terminal.writeTo("\nDisconnecting server...");
        terminal.writeTo("\nPlayer status saved.");
        try {
            if (hostingServer) {
                serverTicker.stopTicking();
                for (int x = 0; x < playerThreads.size(); x++) {
                    playerThreads.get(x).disconnect(true);
                }
                server.close();
            }
            serverTicker = null;
            exec = null;
            server = null;
            hostingServer = false;
        } catch (IOException e) {
            terminal.writeTo("\nError disconnecting server!");
        }
    }

    private void setMOTD(String motd) {
        netprefs.setMOTD(motd);
        terminal.writeTo("\nMessage of the Day set to: " + motd);
    }

    private void setChannel(String channelIP) {
        final String chanDets = serverPort + " `" + serverName + "` " + "`" + adv.getName() + "` " + "`" + adv.getDescription() + "` " + "`" + adv.getVersion() + "` " + "`" + maxCapacity + "`";
        if (channelIP.equalsIgnoreCase("DEFAULT")) {
            terminal.writeTo("\nFetching default channel IP..");
            channelIP = MasterChannelTest.getDefaultChannelIP();
        }
        final String finalIP = channelIP;
        terminal.writeTo("\nCommunicating with channel..");
        Runnable task = new Runnable() {

            @Override
            public void run() {
                terminal.writeTo(serverTicker.setChannel(finalIP, chanDets));
            }
        };
        exec.submit(task);
    }

    private String getGlobalIP() {
        try {
            URL whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");
            InputStream reader = whatismyip.openStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(reader));
            String ip = in.readLine();
            reader.close();
            in.close();
            if (ip != null) {
                return ip;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private String getLocalIP() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private String getNetStatus(String globalip, String localip) {
        if (!globalip.equals("127.0.0.1")) {
            return "ONLINE";
        } else {
            if (!localip.equals("127.0.0.1")) {
                return "LOCAL";
            } else {
                return "OFFLINE";
            }
        }
    }

    private void help(String query) {
        if (query.length() > 0) {
            String result = "WITNA Client Shell v" + version + " Helpfiles" + "\n----------------------------------\n\n" + shellHelp.query(query);
            terminal.writeTo(result);
        } else {
            String genericHelp = "WITNA Client Shell v" + version + " Helpfiles\n" + "--------------------" + "----" + "----------\n\n" + "Welcome to the WITNA Client. This is where you will find " + "an extensive collection of helpfiles detailling the launching " + "and maintaining of a WITNA custom server, along with basic " + "getting started information for joining servers, etc.\n\n" + "If you are a new user, try searching for 'getting started'.";
            terminal.writeTo(genericHelp);
        }
    }
}

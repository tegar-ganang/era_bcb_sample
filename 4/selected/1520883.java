package console;

import help.Help;
import help.ShellHelp;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.StringTokenizer;
import server.NetworkPreferences;
import server.NetworkPreferencesImp;
import server.NetworkServer;
import server.NetworkServerImp;
import channel.Channel;
import channel.ChannelImp;
import channel.master.MasterChannelImp;
import client.NetworkClient;
import client.NetworkClientImp;
import colour.Colour;

/**
 *
 * @author Michael Hanns
 *
 */
public class ClientShellImp implements ClientShell {

    private double version = 1.0;

    private IOBuffer terminal;

    private NetworkClient client;

    private NetworkServer server;

    private Channel channel;

    private NetworkPreferences netprefs;

    private boolean hostingServer = false;

    private boolean recievingData = false;

    private boolean hostingChannel = false;

    private Help help;

    public ClientShellImp(IOBuffer term) {
        terminal = term;
        netprefs = new NetworkPreferencesImp();
        terminal.writeTo(Colour.colourise("WITNA Client Shell v" + version, Colour.yellow) + Colour.colourise("\n------------------------\n", Colour.white) + Colour.colourise("", Colour.grey));
        help = new ShellHelp(term);
    }

    @Override
    public void inputLine(String input) {
        if (recievingData) {
            clientCommand(input);
        } else if (hostingServer) {
            serverCommand(input);
        } else if (hostingChannel) {
            channelCommand(input);
        } else {
            shellCommand(input);
        }
    }

    private void clientCommand(String input) {
        StringTokenizer tokens = new StringTokenizer(input);
        String command = "";
        if (tokens.hasMoreTokens()) {
            command = tokens.nextToken();
        }
        if (command.equalsIgnoreCase("DISCONNECT") || command.equalsIgnoreCase("QUIT")) {
            client.processLocalCommand(command);
            disconnectClient();
        } else {
            client.processLocalCommand(input);
            if (client.connected()) {
                client.sendData(input);
            } else {
                disconnectClient();
            }
        }
    }

    private void serverCommand(String input) {
        StringTokenizer tokens = new StringTokenizer(input);
        String command = "";
        if (tokens.hasMoreTokens()) {
            command = tokens.nextToken();
        }
        if (command.equalsIgnoreCase("DISCONNECT") || command.equalsIgnoreCase("QUIT")) {
            server.processLocalCommand(command);
            disconnectServer();
        } else {
            server.processLocalCommand(input);
        }
    }

    private void channelCommand(String input) {
        StringTokenizer tokens = new StringTokenizer(input);
        String command = "";
        if (tokens.hasMoreTokens()) {
            command = tokens.nextToken();
        }
        if (command.equalsIgnoreCase("DISCONNECT") || command.equalsIgnoreCase("QUIT")) {
            channel.processLocalCommand(command);
            disconnectChannel();
        } else {
            channel.processLocalCommand(input);
        }
    }

    private void shellCommand(String input) {
        StringTokenizer tokens = new StringTokenizer(input);
        String command = "";
        if (tokens.hasMoreTokens()) {
            command = tokens.nextToken();
        }
        if (command.equalsIgnoreCase("SERVERJOIN")) {
            joinServer(tokens);
        } else if (command.equalsIgnoreCase("SERVERHOST")) {
            hostServer(tokens);
        } else if (command.equalsIgnoreCase("CHANNELHOST")) {
            hostChannel(tokens, false);
        } else if (command.equalsIgnoreCase("MASTERCHANNELHOST")) {
            hostChannel(tokens, true);
        } else if (command.equalsIgnoreCase("SETTINGS") && !tokens.hasMoreTokens()) {
            printSettings();
        } else if (command.equals("?") || command.equalsIgnoreCase("HELP")) {
            help(tokens);
        } else {
            terminal.writeTo("\nCommand not recognised.");
        }
    }

    private void disconnectClient() {
        client = null;
        recievingData = false;
        terminal.writeTo("\nReturning to WITNA Shell...");
    }

    private void disconnectServer() {
        server = null;
        hostingServer = false;
        terminal.writeTo("\nReturning to WITNA Shell...");
    }

    private void disconnectChannel() {
        channel = null;
        hostingChannel = false;
        terminal.writeTo("\nReturning to WITNA Shell...");
    }

    private void joinServer(StringTokenizer tokens) {
        if (tokens.hasMoreTokens()) {
            if (tokens.countTokens() > 2) {
                terminal.writeTo("\nYou must specify ONLY an address and port to join a server.\n" + "Syntax: serverjoin 127.0.0.1 723");
            } else {
                String ip = tokens.nextToken();
                String port = "723";
                if (tokens.hasMoreTokens()) {
                    port = tokens.nextToken();
                }
                if (isInteger(port)) {
                    int portNo = Integer.parseInt(port);
                    terminal.writeTo("\nAttempting to connect to " + ip + " on port " + portNo + "...");
                    client = new NetworkClientImp(terminal);
                    recievingData = client.connectToServer(ip, portNo);
                    client.start();
                } else {
                    terminal.writeTo("\nInvalid port number.");
                }
            }
        } else {
            terminal.writeTo("\nYou must specify an address and port to join a server.\n" + "Syntax: serverjoin 127.0.0.1 723");
        }
    }

    private void hostServer(StringTokenizer tokens) {
        if (tokens.countTokens() >= 4) {
            String capStr = tokens.nextToken();
            String portStr = tokens.nextToken();
            if (isInteger(capStr) && isInteger(portStr)) {
                int cap = Integer.parseInt(capStr);
                int port = Integer.parseInt(portStr);
                if (portFree(port, false)) {
                    String serverName = tokens.nextToken();
                    if (serverName.charAt(0) == '\'') {
                        while (tokens.hasMoreTokens() && !serverName.endsWith("'")) {
                            serverName = serverName.concat(" ".concat(tokens.nextToken()));
                        }
                    }
                    serverName = serverName.replace("'", "");
                    if (tokens.hasMoreTokens()) {
                        String advPath = tokens.nextToken();
                        if (advPath.charAt(0) == '\'') {
                            while (tokens.hasMoreTokens() && !advPath.endsWith("'")) {
                                advPath = advPath.concat(" ".concat(tokens.nextToken()));
                            }
                        }
                        advPath = advPath.replace("'", "");
                        server = new NetworkServerImp(terminal, netprefs, help, version);
                        if (server.hostServer(serverName, advPath, cap, port)) {
                            server.start();
                            hostingServer = true;
                        } else {
                            terminal.writeTo("\nServerhost failed. Invalid adventure file or port already in use.");
                        }
                    } else {
                        terminal.writeTo("\nYou must specify a maximum capacity, port and server name to host a server.\n" + "Syntax: serverhost 24 723 'Sample Server Name' 'C:/file adddress/'");
                    }
                } else {
                    terminal.writeTo("\nERROR: Port " + port + " is already in use or reserved for channels. Cannot host server.");
                }
            } else {
                terminal.writeTo("\nYou must specify a maximum capacity, port and server name to host a server.\n" + "Syntax: serverhost 24 723 'Sample Server Name' 'C:/file adddress/'");
            }
        } else {
            terminal.writeTo("\nYou must specify a maximum capacity, port and server name to host a server.\n" + "Syntax: serverhost 24 723 'Sample Server Name' 'C:/file adddress/'");
        }
    }

    private void hostChannel(StringTokenizer tokens, boolean master) {
        int port = 724;
        if (master) {
            port = 725;
        }
        if (portFree(port, true)) {
            if (tokens.hasMoreTokens()) {
                if (master) {
                    if (tokens.countTokens() == 2) {
                        String user = tokens.nextToken();
                        String pass = tokens.nextToken();
                        channel = new MasterChannelImp(user, pass, terminal);
                    } else {
                        terminal.writeTo("\nYou must specify the admin username and password.\n" + "Syntax: masterchannelhost username password");
                    }
                } else {
                    String name = tokens.nextToken();
                    while (tokens.hasMoreTokens()) {
                        name = name.concat(" ".concat(tokens.nextToken()));
                    }
                    channel = new ChannelImp(name, terminal);
                }
                channel.start();
                hostingChannel = true;
            } else {
                terminal.writeTo("\nYou must specify the channel name.\n" + "Syntax: channelhost Sample Server Name");
            }
        } else {
            terminal.writeTo("\nERROR: Port " + port + " is already in use. Cannot host channel.");
        }
    }

    private void printSettings() {
        String settings = "Settings Information" + "\n--------------------" + "\nPlayer Files Location: " + netprefs.getPlayerFilePath() + "\n";
        terminal.writeTo("\n" + settings);
    }

    private void help(StringTokenizer tokens) {
        if (tokens.hasMoreTokens()) {
            String query = tokens.nextToken();
            while (tokens.hasMoreTokens()) {
                query = query.concat(" ".concat(tokens.nextToken()));
            }
            String result = "WITNA Client Shell v" + version + " Helpfiles" + "\n----------------------------------\n\n" + help.query(query);
            terminal.writeTo("\n" + result);
        } else {
            String genericHelp = "WITNA Client Shell v" + version + " Helpfiles\n" + "--------------------" + "----" + "----------\n\n" + "Welcome to the WITNA Client. This is where you will find " + "an extensive collection of helpfiles detailling the launching " + "and maintaining of a WITNA custom server, along with basic " + "getting started information for joining servers, etc.\n\n" + "If you are a new user, try searching for 'getting started'.\n" + "Syntax: help getting started";
            terminal.writeTo("\n" + genericHelp);
        }
    }

    private boolean isInteger(String intStr) {
        try {
            Integer.parseInt(intStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean portFree(int port, boolean channel) {
        if (!channel && (port == 726 || port == 725)) {
            return false;
        }
        try {
            ServerSocket s = new ServerSocket(port);
            s.close();
            s = null;
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

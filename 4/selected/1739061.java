package jircbot.irc;

import jircbot.utils.ConfigReader;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import jircbot.Profile;

/**
 * Handles the irc connection IO tasks.
 * 
 * @author matt
 */
public class Irc {

    private String server;

    private int port;

    private String nick;

    private String name;

    private String[] channels;

    private ConfigReader cfgReader;

    private IrcConnection ircConn;

    private ArrayList<IrcObserver> observers;

    private boolean registered = false;

    private boolean connected = false;

    private boolean verbose = false;

    /**
	 * Constructor method; sets all variables and creates the ircConnection
	 * 
	 * @param cfgReader
	 */
    public Irc(ConfigReader cfgReader) {
        this.cfgReader = cfgReader;
        observers = new ArrayList();
        setServer(cfgReader.getVar("server"));
        setPort(Integer.parseInt(cfgReader.getVar("port")));
        if (cfgReader.getVar("verbose") != null && cfgReader.getVar("verbose").equals("true")) {
            verbose = true;
        }
        setNick(cfgReader.getVar("nick"));
        setName(cfgReader.getVar("name"));
        setChannels(cfgReader.getVars("channels"));
        checkVars();
        ircConn = new IrcConnection(getServer(), getPort(), this, verbose);
    }

    public Irc(Profile profile) {
        setServer(profile.getServer());
        setPort(profile.getPort());
        ircConn = new IrcConnection(getServer(), getPort(), this, false);
        setNick(profile.getNick());
        setName(profile.getHost());
        setChannels(profile.getStartChannels());
        verbose = profile.getVerbose();
        observers = new ArrayList();
        ircConn = new IrcConnection(getServer(), getPort(), this, verbose);
    }

    public void connect() {
        ircConn.connect();
        ircConn.sendLine("USER " + getNick() + " JrcBt JircBot: " + getName());
        setNick(getNick());
    }

    public void addObserver(IrcObserver o) {
        observers.add(o);
    }

    void joinChannel(String channel) {
        ircConn.sendLine("JOIN " + channel);
    }

    void joinChannels(String[] channels) {
        if (channels != null) {
            for (int i = 0; i < channels.length; i++) {
                joinChannel(channels[i]);
            }
        }
    }

    void sendLine(String line) {
        ircConn.sendLine(line);
    }

    void parse(IrcServerMessage message) {
        if (message.getCommand().equals("PING")) {
            ircConn.sendLine("PONG :" + message.getData());
        }
        if (message.getCommand().equals("MODE")) {
            if (!isRegistered()) {
                setRegistered(true);
                joinChannels(getChannels());
            }
        }
        if (!observers.isEmpty()) {
            for (int i = 0; i < observers.size(); i++) {
                observers.get(i).serverMessageReceived(message);
            }
        }
    }

    String getNick() {
        return nick;
    }

    void setNick(String nick) {
        if (ircConn != null && ircConn.isConnected()) {
            ircConn.sendLine("NICK " + nick);
        }
        this.nick = nick;
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }

    private void checkVars() {
        String missingVars = "";
        String[] compulsaryVars = { "server", "channels" };
        for (int i = 0; i < compulsaryVars.length; i++) {
            if (cfgReader.getVar(compulsaryVars[i]) == null) {
                missingVars += compulsaryVars[i];
            }
        }
        if (cfgReader.getVar("nick") == null) {
            nick = "JircBot";
        }
        if (cfgReader.getVar("name") == null) {
            name = "Jb";
        }
        if (!(missingVars.equals(""))) {
            System.out.println("You are missing the following " + "required variables: " + missingVars + ".");
            System.exit(1);
        }
    }

    private void setServer(String server) {
        this.server = server;
    }

    private String getServer() {
        return server;
    }

    private void setPort(int port) {
        this.port = port;
    }

    private int getPort() {
        return port;
    }

    private String[] getChannels() {
        return channels;
    }

    private void setChannels(String[] channels) {
        this.channels = channels;
    }

    private String getName() {
        return name;
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setRegistered(boolean registered) {
        this.registered = registered;
    }

    private boolean isRegistered() {
        return registered;
    }
}

/**
 * Class for handling the irc connection
 * 
 * @author matt
 */
class IrcConnection implements Runnable {

    public boolean connected = false;

    private Socket ircSocket;

    private BufferedReader in;

    private BufferedWriter out;

    private String server;

    private int port;

    private Irc irc;

    private boolean verbose;

    private boolean loop = true;

    IrcConnection(String server, int port, Irc irc, boolean verbose) {
        this.server = server;
        this.port = port;
        this.irc = irc;
        this.verbose = verbose;
    }

    public void run() {
        String line;
        try {
            while (loop) {
                while ((line = in.readLine()) != null) {
                    if (verbose) {
                        System.out.println(line);
                    }
                    parse(line);
                }
            }
        } catch (IOException e) {
            System.out.println("I/O Error: " + e);
        }
    }

    void connect() {
        try {
            ircSocket = new Socket(server, port);
            out = new BufferedWriter(new OutputStreamWriter(ircSocket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(ircSocket.getInputStream()));
            if (ircSocket.isConnected()) {
                new Thread(this).start();
                setConnected(true);
            }
        } catch (UnknownHostException e) {
            System.out.println("Unkown host: " + e);
        } catch (IOException e) {
            System.out.println("IOException on server " + server + " with port " + port + ": " + e);
        }
    }

    void sendLine(String line) {
        try {
            out.write(line + "\r\n");
            out.flush();
        } catch (IOException e) {
            System.out.println("I/O Error: " + e);
        }
    }

    boolean isConnected() {
        if (ircSocket != null) {
            return ircSocket.isConnected();
        } else {
            return false;
        }
    }

    private void parse(String line) {
        IrcServerMessage ircServerMessage = new IrcServerMessage(line);
        if (ircServerMessage.getCommand() != null) {
            irc.parse(ircServerMessage);
        }
    }

    private void setConnected(boolean b) {
        connected = b;
    }
}

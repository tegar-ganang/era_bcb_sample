package tk.bot;

import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TKBot {

    BotGUI gui;

    BotCommandFilter rawFilter = new BotCommandFilter(this);

    BotCommandFilter commandFilter = new BotCommandFilter(this);

    LinkedList commandBuffer = new LinkedList();

    static ThreadGroup threadGroup = new ThreadGroup("TKBot Threads");

    BotConnection connection;

    BotUserManager manager = new BotUserManager(this);

    static BotInterfaceManager dbManager;

    String name = "";

    String currentLogon = "";

    public String[] servers;

    public int serverIndex = 0;

    boolean loggedOn = false;

    static boolean finished = false;

    boolean logData = false;

    boolean autoLogOn = true;

    boolean errorLog = true;

    boolean rawMode = false;

    boolean floodProtection = false;

    boolean isHomeRestriction = true;

    String logFile;

    FileWriter logOut;

    BotLoader loader = new BotLoader(this);

    BotModule[] commands;

    BotDatabaseInterface userTable;

    protected String logonName;

    protected String logonPassword;

    protected String logonChannel;

    public static int DEBUG = 0;

    public TKBot() {
        dbManager = new BotInterfaceManager("config.bot", false);
        loadProperties();
        errorLog(errorLog);
        loadModules();
        if (autoLogOn) connect();
    }

    public void errorLog(boolean b) {
        this.errorLog = b;
        try {
            if (errorLog) System.setOut(new PrintStream(new FileOutputStream("err.bot", true), true)); else System.setOut(System.out);
        } catch (Exception e) {
            System.out.println("Dang it didn't work!");
        }
    }

    public void rawMode(boolean b) {
        this.rawMode = b;
    }

    public void loadProperties() {
        BotDatabaseInterface db = (BotDatabaseInterface) dbManager.getInterface("config");
        servers = BotUtilities.parse(db.getProperty("servers", "battle.net"), ";");
        autoLogOn = (db.getProperty("autoLogOn", "true")).equals("true");
        errorLog = (db.getProperty("errorLog", "true")).equals("true");
        userTable = (BotDatabaseInterface) TKBot.dbManager.getInterface("usertable");
        DEBUG = Integer.parseInt(db.getProperty("DEBUG", "5"));
    }

    public void loadModules() {
        BotModule[] modules = loader.loadModules("types");
        commands = loader.loadModules("customcommands");
        BotUtilities.enableArray(loader.loadModules("comprop"), true);
    }

    public static void main(String[] args) {
        TKBot bot = new TKBot();
    }

    public void append(String line) {
        gui.append(line);
        if (logData) {
            try {
                logOut.write(line);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public void append(char c) {
        gui.append(c);
        if (logData) {
            try {
                logOut.write(c);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public void appendLine(String line) {
        gui.appendLine(line);
        if (logData) {
            try {
                logOut.write(line + "\r\n");
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static boolean getStatus() {
        return TKBot.finished;
    }

    public static void setStatus(boolean finished) {
        TKBot.finished = finished;
    }

    public void addCommand(String command) {
        commandBuffer.add(command);
    }

    public String getNextCommand() {
        if (!commandBuffer.isEmpty()) {
            return (String) commandBuffer.removeFirst();
        } else return "";
    }

    public BotCommandFilter getFilter() {
        if (rawMode) return rawFilter; else return commandFilter;
    }

    public Hashtable getUserTable() {
        userTable.put("%time%", (new Date()).toString());
        return userTable;
    }

    public void connect() {
        if (true) {
            finished = false;
            commandBuffer = new LinkedList();
            if (connection != null) connection.shutdown();
            connection = new BotConnection(this);
            connection.start();
        } else {
            appendLine("You must disconnect first.");
            appendLine("Disconnecting");
            disconnect();
        }
    }

    public void disconnect() {
        if (connection != null) {
            if (connection.isAlive() || (!finished)) {
                connection.shutdown();
            }
        }
        loggedOn(false);
    }

    public boolean isLoggedOn() {
        return loggedOn;
    }

    public void logon(String name, String password, String channel, String server) {
        logon(name, password, channel);
    }

    public void logon(String name, String password, String channel) {
        this.addCommand(name);
        this.addCommand(password);
        this.logonName = name;
        this.logonPassword = password;
        this.logonChannel = channel;
    }

    public void logon(String name) {
        currentLogon = name;
        BotDatabaseInterface db = (BotDatabaseInterface) dbManager.getInterface("logon");
        String[] data = BotUtilities.parse(db.getProperty(name, "guest,,bot,battle.net"), ",");
        String[] Default = { "guest", "", "bot", "battle.net" };
        if (data.length != 4) data = Default;
        logon(data[0], data[1], data[2], data[3]);
        appendLine("Loggging on " + name);
    }

    public void loggedOn(boolean loggedOn) {
        this.loggedOn = loggedOn;
        if ((logonChannel != null) && (loggedOn)) this.addCommand("/join " + logonChannel);
    }

    public void reconnect() {
        disconnect();
        Thread.currentThread().yield();
        connection = new BotConnection(this);
        connect();
        if (currentLogon != "") logon(currentLogon);
    }

    public void logData(String file) {
        this.logData = true;
        this.logFile = file;
        try {
            logOut = new FileWriter(logFile, true);
        } catch (IOException e) {
            System.out.println(e.toString());
            appendLine("Logging Error");
        }
    }

    public void stopLogging() {
        this.logData = false;
        this.logFile = "";
        try {
            logOut.close();
        } catch (IOException e) {
            System.out.println(e);
            appendLine("Stop logging Error");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BotGUI getGUI() {
        return gui;
    }

    public void setGUI(BotGUI g) {
        this.gui = g;
        gui.addGUI();
        gui.init();
        gui.addControlCommands();
    }

    public void removeGUI(BotGUI g) {
        gui.dispose();
        if (this.gui.equals(g)) this.gui = null;
    }

    public boolean getFloodProtection() {
        return floodProtection;
    }

    public void setFloodProtection(boolean protect) {
        this.floodProtection = protect;
    }

    public static ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    public boolean isHome() {
        if (!isHomeRestriction) return true;
        if ((this.gui == null) || (this.logonChannel == null)) return false; else {
            return gui.getUserDisplay().getChannel().toLowerCase().equals(logonChannel.toLowerCase());
        }
    }

    public void clearBuffer() {
        if (commandBuffer != null) commandBuffer.clear();
    }
}

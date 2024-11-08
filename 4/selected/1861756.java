package net.sf.opengroove.realmserver;

import java.util.Map;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

public class OldConvergiaServer extends Thread {

    public static String generateMetadata(Properties properties) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (Map.Entry entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!isFirst) builder.append("|");
            isFirst = false;
            builder.append(key + "=" + value);
        }
        return builder.toString();
    }

    public static Properties parseMetadata(String userMetadata) {
        String[] items = userMetadata.split("\\|");
        Properties p = new Properties();
        for (String item : items) {
            int pxIndex = item.indexOf("=");
            p.setProperty(item.substring(0, pxIndex), item.substring(pxIndex + 1));
        }
        return p;
    }

    public static final String serverGreeting = "Trivergia.com InTouch 3 server speaking. Please follow the normal protocol procedure. Or, send an email to webmaster@trivergia.com for more info.";

    public static HashMap<String, ConnectionHandler> userConnections = new HashMap<String, ConnectionHandler>();

    /**
     * Watchdog wraps the input stream of the socket specified. all read access
     * to the socket should be done through the watchdog's getInputStream()
     * method. if no data is successfully read from the inputstream for 60
     * seconds, the socket will be closed. remember to start the watchdog (by
     * calling start()) in order to activate this timeout detection feature.
     * 
     * the watchdog does nothing to the socket's output stream (it only affects
     * the input stream).
     * 
     * The watchdog exists for two reasons, to make sure that a client can still
     * reconnect if their internet connection goes down (and the server doesn't
     * realize they've disconnected) and to prevent against denial-of-service
     * attacks where someone connects a bunch of times but doesn't send the
     * TCP/IP-related shutdown packets, and so the server still thinks it has an
     * active connection to the client.
     * 
     * @author Alexander Boyd
     * 
     */
    public static class Watchdog extends Thread {

        private Socket socket;

        private InputStream inputStream;

        private InputStream customInputStream;

        private int lastNumSeconds = 0;

        public Watchdog(Socket socket) throws IOException {
            this.socket = socket;
            this.inputStream = socket.getInputStream();
            this.customInputStream = new InputStream() {

                @Override
                public int read() throws IOException {
                    int i = Watchdog.this.inputStream.read();
                    lastNumSeconds = 0;
                    return i;
                }
            };
        }

        public void run() {
            while (!socket.isClosed()) {
                try {
                    Thread.sleep(1000);
                    lastNumSeconds++;
                    if (lastNumSeconds > 60) {
                        System.out.println("socket closed by watchdog");
                        socket.close();
                    }
                } catch (Exception ex1) {
                    ex1.printStackTrace();
                }
            }
        }

        public InputStream getInputStream() {
            return customInputStream;
        }
    }

    public static int numConnections = 0;

    private static Properties userAuthProperties = new Properties();

    public static class Workspace implements Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = -1461914878602529846L;

        private String creator;

        private String id;

        private String[] users;

        public String getCreator() {
            return creator;
        }

        public void setCreator(String creator) {
            this.creator = creator;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String[] getUsers() {
            return users;
        }

        public void setUsers(String[] users) {
            this.users = users;
        }
    }

    public static class ConnectionHandler extends Thread {

        public int hashCode() {
            return username.hashCode() + 31;
        }

        public ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        private Socket socket;

        private PushbackInputStream in;

        private OutputStream out;

        private String username;

        public String getUsername() {
            return username;
        }

        public void run() {
            try {
                System.out.println("starting connection watchdog");
                Watchdog watchdog = new Watchdog(socket);
                watchdog.start();
                System.out.println("watchdog started.");
                in = new PushbackInputStream(watchdog.getInputStream());
                out = socket.getOutputStream();
                sendLine("OK " + serverGreeting);
                System.out.println("reading username");
                String username = readLineT();
                username = username.toLowerCase().trim();
                System.out.println("username is " + username);
                System.out.println("reading password");
                String password = readLineT();
                System.out.println("password is " + password);
                System.out.println("REMOVE THESE LINES ABOUT PASSWORD");
                System.out.println("reading real password");
                String realPassword = userAuthProperties.getProperty(username);
                System.out.println("real password is " + realPassword);
                if (!password.equals(realPassword)) {
                    System.out.println("failed auth");
                    Thread.sleep(8000);
                    out.write("FAIL incorrect username and/or password (for questions send email to intouch@trivergia.com)\r\n".getBytes("ASCII"));
                    out.flush();
                    socket.close();
                    return;
                } else {
                    Thread.sleep(3000);
                    System.out.println("succeeded auth");
                    out.write("OK please send your connection mode (command is the only value for now), NO REPLY will be sent, then start sending commands. you can use the help command for info.\r\n".getBytes("ASCII"));
                    out.flush();
                }
                this.username = username;
                System.out.println("waiting for mode");
                String mode = readLineT();
                System.out.println("mode was " + mode);
                if (!(mode.equalsIgnoreCase("command"))) throw new RuntimeException("invalid mode " + mode);
                if (mode.equalsIgnoreCase("command")) {
                    if (userConnections.get(username) != null) {
                        System.out.println("you are already connected");
                        out.write("FAIL You are already connected with a command socket\r\n".getBytes("ASCII"));
                        out.flush();
                        socket.close();
                        return;
                    }
                    userConnections.put(username, this);
                    notifyUserHere(username);
                    while (!socket.isClosed()) {
                        String line = readLineT();
                        System.out.println("line read from client user " + username + " was " + line.substring(0, Math.min(line.length(), 255)));
                        if (line == null) continue;
                        String command;
                        if (line.indexOf(" ") == -1) command = line; else command = line.substring(0, line.indexOf(" "));
                        String arguments = line.substring(command.length()).trim();
                        System.out.println("command was " + command);
                        if (command.equalsIgnoreCase("help")) {
                            sendTextGroup(command, "This server is used to allow multiple computers behind firewalls (in otherwords, not normally connectable to\r\n" + "each other) to exchange data. It was originally written as the server component of InTouch Communicator 3.\r\n" + "-------------------------------------\r\n" + "For more information, send an email to intouch@trivergia.com or webmaster@trivergia.com, or use the command commandhelp to list commands available or get info on a command.\r\n" + "You can also visit http://static.trivergia.com/intouch3-protocol for more information.\r\n" + "-------------------------------------\r\n" + "By convention, user metadata is made up of pipe-seperated strings (seperated by | ). each string has the format name=value. for\r\n" + "example, your metadata could be name1=value1|name2=value2|name3=value3 .\r\n" + "-------------------------------------\r\n" + "normal user accounts' usernames start with a letter or a number, and can contain letters, numbers, and spaces" + ". special accounts (i'll get to this in a bit) start with an underscore." + "accounts are not allowed to contain 2 underscores in a row.\r\n" + "Anyway, about special accounts. Special accounts are generally always online.\r\n" + "special accounts are usually there to serve useful purposes.\r\n" + "right now no special accounts have been implemented, but there are a few planned. the\r\n" + "special account _cache will be available for you to store large amounts of data" + "and later retrieve it. this could be used for sending offline messages.");
                        } else if (command.equalsIgnoreCase("listall")) {
                            StringBuilder responseBuilder = new StringBuilder();
                            for (Object user : userAuthProperties.keySet()) {
                                responseBuilder.append(user + "\r\n");
                            }
                            sendTextGroup(command, responseBuilder.toString());
                        } else if (command.equalsIgnoreCase("listonline")) {
                            StringBuilder responseBuilder = new StringBuilder();
                            for (Object user : userConnections.keySet()) {
                                responseBuilder.append(user + "\r\n");
                            }
                            sendTextGroup(command, responseBuilder.toString());
                        } else if (command.equalsIgnoreCase("listoffline")) {
                            StringBuilder responseBuilder = new StringBuilder();
                            ArrayList usersToSend = new ArrayList(userAuthProperties.keySet());
                            usersToSend.removeAll(userConnections.keySet());
                            for (Object user : usersToSend) {
                                responseBuilder.append(user + "\r\n");
                            }
                            sendTextGroup(command, responseBuilder.toString());
                        } else if (command.equalsIgnoreCase("sendmessage")) {
                            arguments = arguments.trim();
                            if (arguments.indexOf(" ") == -1) {
                                sendTextGroup(command, "FAIL invalid arguments supplied");
                            } else {
                                String to = arguments.substring(0, arguments.indexOf(" "));
                                String message = arguments.substring(arguments.indexOf(" ") + 1);
                                if (message.length() > 65535) {
                                    sendTextGroup(command, "FAIL message is too long (" + message.length() + " bytes), maximum length is 65535 bytes");
                                } else {
                                    if (userConnections.get(to) == null) {
                                        sendTextGroup(command, "FAIL the user specified is not connected right now (in the future, this command will support offline message caching)");
                                    } else {
                                        ConnectionHandler toHandler = userConnections.get(to);
                                        toHandler.sendTextGroup("RECEIVEMESSAGE", username + " " + message);
                                        sendTextGroup(command, "OK message successfully sent");
                                    }
                                }
                            }
                        } else if (command.equalsIgnoreCase("quit")) {
                            sendTextGroup(command, "OK disconnecting now");
                            break;
                        } else if (command.equalsIgnoreCase("getuserstatushash")) {
                            sendTextGroup(command, "" + userConnections.size() + "z" + userConnections.keySet().hashCode());
                        } else if (command.equalsIgnoreCase("commandhelp")) {
                            sendTextGroup(command, "current available commands are: COMMANDHELP, HELP, LISTALL, LISTONLINE, LISTOFFLINE, SENDMESSAGE, QUIT, GETTIME, GETUSERMETADATA, SETUSERMETADATA\r\n" + "if a message is sent to you, you will get an 'orphan' response (a response without a command to back it) where the\r\n" + "command is RECEIVEMESSAGE. to try this out, connect 2 users to the server, then use sendmessage to send a message from one to the other,\r\n" + "and see what you get on the receiving user's side.");
                        } else if (command.equalsIgnoreCase("gettime")) {
                            sendTextGroup(command, "" + System.currentTimeMillis());
                        } else if (command.equalsIgnoreCase("getusermetadata")) {
                            if (arguments.contains("/") || arguments.contains("\\") || arguments.contains(".")) throw new RuntimeException();
                            if (!new File(userMetadataFolder, arguments).exists()) {
                                sendTextGroup(command, "FAIL invalid user specified (the user either does not exist or does not have metadata set)");
                            } else {
                                sendTextGroup(command, "OK " + readFile(new File(userMetadataFolder, arguments)));
                            }
                        } else if (command.equalsIgnoreCase("getusermetadataattribute")) {
                            if (arguments.contains("/") || arguments.contains("\\")) throw new RuntimeException();
                            int sIndex = arguments.indexOf(" ");
                            String tUser = arguments.substring(0, sIndex);
                            String attribute = arguments.substring(sIndex + 1);
                            if (!new File(userMetadataFolder, tUser).exists()) {
                                sendTextGroup(command, "FAIL invalid user specified (the user either does not exist or does not have metadata set)");
                            } else {
                                String userMetadata = readFile(new File(userMetadataFolder, tUser));
                                Properties parsedMetadata = parseMetadata(userMetadata);
                                String result = parsedMetadata.getProperty(attribute);
                                if (result == null) sendTextGroup(command, "FAIL the attribute specified does not exist"); else sendTextGroup(command, "OK " + result);
                            }
                        } else if (command.equalsIgnoreCase("setusermetadata")) {
                            if (arguments.length() > (800 * 1024)) {
                                sendTextGroup(command, "FAIL user metadata too long");
                            }
                            writeFile(arguments, new File(userMetadataFolder, username));
                            sendTextGroup(command, "OK");
                        } else if (command.equalsIgnoreCase("setusermetadataproperty")) {
                            int pIndex = arguments.indexOf(" ");
                            String atName = arguments.substring(0, pIndex);
                            String atValue = arguments.substring(pIndex + 1);
                            Properties userMd = parseMetadata(readFile(new File(userMetadataFolder, username)));
                            userMd.setProperty(atName, atValue);
                            String newMd = generateMetadata(userMd);
                            if (newMd.length() > (800 * 1024)) {
                                sendTextGroup(command, "FAIL user metadata too long");
                            }
                            writeFile(newMd, new File(userMetadataFolder, username));
                            sendTextGroup(command, "OK");
                        } else if (command.equalsIgnoreCase("gettime")) {
                            sendTextGroup(command, "" + System.currentTimeMillis());
                        } else if (command.equalsIgnoreCase("NOP") || command.equalsIgnoreCase("NOOP")) {
                            sendTextGroup(command, "OK");
                        } else if (command.equalsIgnoreCase("CREATEWORKSPACE")) {
                            verifyNonPath(arguments);
                            vc(arguments);
                            verifyLength(arguments, 256);
                            long myWorkspaceCount = 0;
                            for (File f : workspaceFolder.listFiles()) {
                                Workspace workspace = (Workspace) readObjectFromFile(f);
                                if (workspace.getCreator().equalsIgnoreCase(username)) myWorkspaceCount++;
                            }
                            if (myWorkspaceCount > 20) throw new RuntimeException();
                            File workspaceFile = new File(workspaceFolder, arguments);
                            if (workspaceFile.exists()) sendTextGroup(command, "FAIL you already have a workspace with that id"); else {
                                Workspace workspace = new Workspace();
                                workspace.setCreator(username);
                                workspace.setId(arguments);
                                workspace.setUsers(new String[] {});
                                writeObjectToFile(workspace, workspaceFile);
                                new File(workspaceDataFolder, arguments).mkdirs();
                                sendTextGroup(command, "OK the workspace was successfully created");
                            }
                        } else if (command.equalsIgnoreCase("SETWORKSPACEPERMISSIONS")) {
                            verifyNonPath(arguments);
                            System.out.println("arguments:" + arguments);
                            String[] firstSplit = arguments.split(" ", 2);
                            String workspaceId = firstSplit[0];
                            String usersString = firstSplit[1];
                            System.out.println("workspaceid is " + workspaceId + " and usersString is " + usersString);
                            verifyLength(usersString, 5000);
                            String[] users = usersString.split(" ");
                            System.out.println(users.length + " users were included");
                            for (String u : users) System.out.println("u:" + u);
                            vc(workspaceId);
                            File workspaceFile = new File(workspaceFolder, workspaceId);
                            if (!workspaceFile.exists()) {
                                System.out.println("invalid workspace id");
                                sendTextGroup(command, "FAIL invalid workspace id");
                            } else {
                                System.out.println("ok, setting users");
                                Workspace workspace = (Workspace) readObjectFromFile(workspaceFile);
                                workspace.setUsers(users);
                                writeObjectToFile(workspace, workspaceFile);
                                sendTextGroup(command, "OK successfully set allowed users");
                            }
                        } else if (command.equalsIgnoreCase("DELETEWORKSPACE")) {
                            verifyNonPath(arguments);
                            vc(arguments);
                            recursiveDelete(new File(workspaceDataFolder, arguments));
                            recursiveDelete(new File(workspaceFolder, arguments));
                            sendTextGroup(command, "OK workspace successfully deleted");
                        } else if (command.equalsIgnoreCase("GETWORKSPACEPROPERTY")) {
                            verifyNonPath(arguments);
                            String[] firstSplit = arguments.split(" ", 2);
                            String workspaceId = firstSplit[0];
                            String key = firstSplit[1];
                            verifyIAmParticipant(workspaceId);
                            File workspaceDataFile = new File(workspaceDataFolder, workspaceId);
                            File keyFile = new File(workspaceDataFile, key);
                            if (!keyFile.exists()) {
                                sendTextGroup(command, "FAIL that key doesn't exist");
                            } else {
                                sendTextGroup(command, "OK " + readFile(keyFile));
                            }
                        } else if (command.equalsIgnoreCase("SETWORKSPACEPROPERTY")) {
                            String[] split = arguments.split(" ", 3);
                            String workspaceId = split[0];
                            String key = split[1];
                            String value = null;
                            if (split.length > 2) value = split[2];
                            verifyNonPath(workspaceId);
                            verifyNonPath(key);
                            verifyIAmParticipant(workspaceId);
                            verifyLength(key, 1024);
                            if (value != null) verifyLength(value, 50 * 1024);
                            File workspaceDataFile = new File(workspaceDataFolder, workspaceId);
                            long length = 0;
                            for (File f : workspaceDataFile.listFiles()) {
                                length += f.length();
                                length += f.getName().length();
                                length += 10;
                            }
                            if (length > (4 * 1000 * 1000)) {
                                throw new RuntimeException("workspace too large");
                            }
                            File keyFile = new File(workspaceDataFile, key);
                            if (value == null) keyFile.delete(); else writeFile(value, keyFile);
                            sendTextGroup(command, "OK property written/deleted successfully");
                        } else if (command.equalsIgnoreCase("LISTWORKSPACEPROPERTIES")) {
                            verifyNonPath(arguments);
                            String workspaceId = null;
                            String prefix = null;
                            arguments = arguments.trim();
                            if (arguments.indexOf(" ") == -1) {
                                workspaceId = arguments;
                            } else {
                                String[] split = arguments.split(" ", 2);
                                workspaceId = split[0];
                                prefix = split[1];
                            }
                            verifyIAmParticipant(workspaceId);
                            File workspaceDataFile = new File(workspaceDataFolder, workspaceId);
                            FilenameFilter filter = null;
                            final String fPrefix = prefix;
                            if (prefix != null) filter = new FilenameFilter() {

                                public boolean accept(File folder, String name) {
                                    return name.startsWith(fPrefix);
                                }
                            };
                            String[] keys = workspaceDataFile.list(filter);
                            sendTextGroup(command, "OK " + delimited(keys, "\n"));
                        } else if (command.equalsIgnoreCase("LISTOWNEDWORKSPACES")) {
                            sendTextGroup(command, "");
                        } else if (command.equalsIgnoreCase("LISTWORKSPACES")) {
                            sendTextGroup(command, "");
                        } else if (command.equalsIgnoreCase("CANACCESS")) {
                            verifyNonPath(arguments);
                            try {
                                verifyIAmParticipant(arguments);
                                sendTextGroup(command, "OK you have access to this workspace");
                            } catch (Exception e) {
                                sendTextGroup(command, "FAIL you do not have access to this workspace");
                            }
                        } else if (command.equalsIgnoreCase("")) {
                            sendTextGroup(command, "");
                        } else if (command.equalsIgnoreCase("")) {
                            sendTextGroup(command, "");
                        } else if (command.equalsIgnoreCase("")) {
                            sendTextGroup(command, "");
                        } else if (command.equalsIgnoreCase("")) {
                            sendTextGroup(command, "");
                        } else if (command.equalsIgnoreCase("")) {
                            sendTextGroup(command, "");
                        } else {
                            sendTextGroup(command, "FAIL unknown command");
                        }
                        out.flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (this.username != null && userConnections.get(username) == this) {
                            userConnections.remove(username);
                            try {
                                notifyUserGone(username);
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                        }
                    } finally {
                        System.out.println("before subtracting from numconnections, it is " + numConnections);
                        numConnections--;
                    }
                }
            }
        }

        /**
         * this method verifies that this user is a member of the workspace
         * specified. throws a RuntimeException if the workspace does not exist
         * or if it does but we are not a member of the workspace
         */
        private void verifyIAmParticipant(String workspaceId) {
            verifyNonPath(workspaceId);
            File workspaceFile = new File(workspaceFolder, workspaceId);
            if (!workspaceFile.exists()) {
                throw new RuntimeException("the workspace specified does not exist");
            }
            Workspace workspace = (Workspace) readObjectFromFile(workspaceFile);
            System.out.println("checking against " + workspace.getCreator());
            if (username.equals(workspace.getCreator())) return;
            for (String u : workspace.getUsers()) {
                System.out.println("checking against " + u);
                if (u.equals(username)) return;
            }
            throw new RuntimeException("the workspace specified does not contain this user as a member");
        }

        /**
         * verifies that the length of the arguments supplied does not exceed
         * <code>i</code> characters. a <code>RuntimeException</code> is
         * raised if the arguments are longer than that.
         * 
         * @param arguments
         * @param i
         */
        private void verifyLength(String arguments, int i) {
            if (arguments.length() > i) throw new RuntimeException("the arguments specified are too long");
        }

        /**
         * verifies that we are the creator of the workspace id specified. an
         * exception is thrown if we are not the creator of that workspace.
         * 
         * @param arguments
         */
        private void vc(String arguments) {
            if (arguments.indexOf("-") == -1) throw new RuntimeException("workspace id " + arguments + " is invalid because it does not contain a hyphen");
            int dIndex = arguments.indexOf("-");
            String wUsername = arguments.substring(0, dIndex);
            if (!wUsername.equals(username)) throw new RuntimeException("workspace id " + arguments + " is not owned by the user " + username + " , which is should be to perform the operation specified");
        }

        /**
         * this method vaidates that the arguments do not contain / or \\.
         */
        private void verifyNonPath(String arguments) {
            if (arguments.contains("/") || arguments.contains("\\") || arguments.contains(".")) throw new RuntimeException();
        }

        public void sendTextGroup(String command, String data) throws IOException {
            synchronized (out) {
                System.out.println("sending text group to user " + username + ": " + data.substring(0, Math.min(data.length(), 255)));
                out.write((command.replace(" ", "") + " " + data + (data.endsWith("\n") ? "\r\n" : "\r\n\r\n")).getBytes("ASCII"));
                out.flush();
            }
        }

        private void sendLine(String line) throws IOException {
            synchronized (out) {
                out.write((line + (line.endsWith("\n") ? "" : "\r\n")).getBytes("ASCII"));
                out.flush();
            }
        }

        private String readLineN() throws IOException {
            return readLineBx(false);
        }

        private String readLineT() throws IOException {
            return readLineBx(true);
        }

        private String readLineBx(boolean throwEx) throws IOException {
            synchronized (in) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (true) {
                    int i = in.read();
                    if (baos.size() > 1000 * 1000) throw new RuntimeException("too many bytes read");
                    if (i == -1 && throwEx) {
                        if (baos.toByteArray().length == 0) throw new EOFException("end of input from socket"); else return new String(baos.toByteArray(), "ASCII");
                    } else if (i == -1) {
                        if (baos.toByteArray().length == 0) return null; else return new String(baos.toByteArray(), "ASCII");
                    } else if (i == '\r') continue; else if (i == '\n') return new String(baos.toByteArray(), "ASCII"); else {
                        baos.write(i);
                    }
                }
            }
        }
    }

    private static ServerSocket serversocket;

    private static final int port = 64482;

    private static final boolean useSSL = true;

    private static File userMetadataFolder;

    private static File workspaceFolder;

    private static File workspaceDataFolder;

    private static final String version = "0.1.16";

    /**
     * @param args
     */
    public static void main(String[] args) throws Throwable {
        System.out.println("InTouch 3 Server, version " + version);
        System.out.println("Copyright 2007-2008 Alexander Boyd");
        System.out.println("Send all questions and comments to javawizard@trivergia.com");
        System.out.println();
        System.out.println("Initializing on port " + port);
        System.out.println("Using " + (useSSL ? "secure" : "insecure") + " communications (" + (useSSL ? "" : "not ") + "SSL)");
        if (useSSL) {
            ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
            serversocket = factory.createServerSocket(port);
        } else {
            serversocket = new ServerSocket(port);
        }
        System.out.println("Server socket successfully opened.");
        System.out.println("Loading user auth file...");
        userAuthProperties.load(new FileInputStream("users.properties"));
        System.out.println("Setting folder paths...");
        userMetadataFolder = new File("usermetadata");
        workspaceFolder = new File("workspaces");
        workspaceDataFolder = new File("/mnt/it3sworkspacedata");
        if (!workspaceFolder.exists()) workspaceFolder.mkdirs();
        if (!workspaceDataFolder.exists()) workspaceDataFolder.mkdirs();
        System.out.println("InTouch 3 Server has successfully started.");
        System.out.println("To terminate InTouch 3 Server, use Ctrl+C or KILL.");
        doServer();
    }

    public static void notifyUserGone(String username) {
        sendBulkMessage("USERGONE", username);
    }

    /**
     * sends the specified message text to all users currently connected.
     * 
     * @param string
     */
    private static void sendBulkMessage(String command, String data) {
        for (Map.Entry<String, OldConvergiaServer.ConnectionHandler> entry : userConnections.entrySet()) {
            ConnectionHandler h = entry.getValue();
            try {
                h.sendTextGroup(command, data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void notifyUserHere(String username) {
        sendBulkMessage("USERHERE", username);
    }

    private static void doServer() {
        while (true) {
            try {
                System.out.println("Waiting for a connection...");
                Socket socket = serversocket.accept();
                System.out.println("Connection received. number of active connections is " + numConnections + ".");
                if (numConnections > 1000) {
                    socket.close();
                    continue;
                }
                numConnections++;
                System.out.println("dispatching to connection handler...");
                new ConnectionHandler(socket).start();
                System.out.println("Successfully dispatched connection.");
            } catch (Exception ex1) {
                ex1.printStackTrace();
            }
        }
    }

    /**
     * reads the file specified in to a string. the file must not be larger than
     * 5 MB.
     * 
     * @param file.
     * @return
     */
    static String readFile(File file) {
        try {
            if (file.length() > (5 * 1000 * 1000)) throw new RuntimeException("the file is " + file.length() + " bytes. that is too large. it can't be larger than 5000000 bytes.");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(file);
            copy(fis, baos);
            fis.close();
            baos.flush();
            baos.close();
            return new String(baos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeFile(String string, File file) {
        try {
            file = file.getAbsoluteFile();
            file.getParentFile().mkdirs();
            ByteArrayInputStream bais = new ByteArrayInputStream(string.getBytes("UTF-8"));
            FileOutputStream fos = new FileOutputStream(file);
            copy(bais, fos);
            bais.close();
            fos.flush();
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int amount;
        while ((amount = in.read(buffer)) != -1) {
            out.write(buffer, 0, amount);
        }
    }

    private static void writeObjectToFile(Serializable object, File file) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(object);
            oos.flush();
            oos.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Serializable readObjectFromFile(File file) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            Object object = ois.readObject();
            ois.close();
            return (Serializable) object;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    static void recursiveDelete(File transmissionFolder) {
        if (transmissionFolder.isDirectory()) {
            for (File file : transmissionFolder.listFiles()) {
                recursiveDelete(file);
            }
        }
        transmissionFolder.delete();
    }

    /**
     * returns a string containing each of the items in the list specified,
     * separated by <code>delimiter</code>. if there are no items, the empty
     * string is returned. this method is designed to approximately be the
     * opposite of String.split, except that split uses regex instead of literal
     * strings.
     * 
     * @param items
     * @param delimiter
     * @return
     */
    public static String delimited(String[] items, String delimiter) {
        String s = "";
        for (String i : items) {
            if (!s.equals("")) s += delimiter;
            s += i;
        }
        return s;
    }
}

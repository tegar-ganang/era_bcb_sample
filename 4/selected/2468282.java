package Networking.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;
import GUI.ServerGUI;

public class ServerThread extends Thread {

    private Socket socket = null;

    private String inputLine;

    private int leadingByte;

    private ServerGUI GUI = null;

    private ConnectionHandler ch = null;

    ObjectInputStream objIn = null;

    private User user = null;

    private static ArrayList<User> userList = NamShubServer.getUserList();

    private static HashMap<String, Channel> channelList = NamShubServer.getChannelList();

    private static HashMap<Integer, Game> gameList = NamShubServer.getGameList();

    public ServerThread(Socket socket, ServerGUI GUI, ConnectionHandler ch) {
        this.socket = socket;
        this.GUI = GUI;
        this.ch = ch;
    }

    public void run() {
        try {
            objIn = new ObjectInputStream(socket.getInputStream());
            while (ch.isListening()) {
                leadingByte = objIn.readByte();
                System.out.println(leadingByte + " Outer loop");
                if (leadingByte == 4) {
                    objIn.close();
                    userList.remove(user);
                    user.getChannel().removeUserQuietly(user);
                    user.getChannel().channelcast(user.getUserName() + " has disconnected.", 2);
                    GUI.removeUser(user);
                    GUI.append("Connection to remote user " + user.getUserName() + " closed.\n");
                    return;
                } else if (leadingByte == 0) {
                    try {
                        inputLine = (String) objIn.readObject();
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                    System.out.println("Inputline: " + inputLine);
                    if (inputLine.equals("userlist")) {
                        for (User u : user.getChannel().getUsers()) broadcast(user, "adduser " + u.getUserName(), 0);
                    } else if (inputLine.equals("gamelist")) {
                        for (Integer i : gameList.keySet()) broadcast(user, "addgame " + gameList.get(i).getName(), 0);
                    } else if (inputLine.equals("channellist")) {
                        for (String s : channelList.keySet()) broadcast(user, "addchannel " + s, 0);
                    } else if (inputLine.startsWith("hostgame")) {
                        if (inputLine.split(" ").length > 1) {
                            System.err.println("HostGame command received.");
                            ArrayList<User> temp = new ArrayList<User>();
                            temp.add(user);
                            int i;
                            for (i = 0; gameList.keySet().contains(i); i++) ;
                            System.err.println("GameID found and set");
                            try {
                                gameList.put(i, new Game(inputLine.split(" ", 2)[1], temp, i));
                            } catch (GameIsFullException e) {
                            }
                        }
                    } else if (inputLine.startsWith("joingame")) {
                        if (inputLine.split(" ").length > 1) {
                            try {
                                for (Game g : gameList.values()) if (g.getName().equals(inputLine.split(" ", 2)[1])) g.addPlayer(user);
                            } catch (GameIsFullException e) {
                                broadcast(user, "gameisfull", 1);
                            }
                        }
                    } else if (inputLine.startsWith("joinchannel")) {
                        if (channelList.get(inputLine.split(" ", 2)[1]) != null) {
                            channelList.get(inputLine.split(" ", 2)[1]).addUser(user);
                        } else {
                            channelList.put(inputLine.split(" ", 2)[1], new Channel(inputLine.split(" ", 2)[1], new ArrayList<User>()));
                            channelList.get(inputLine.split(" ", 2)[1]).addUser(user);
                        }
                    } else if (inputLine.startsWith("select")) {
                        gameList.get(Integer.parseInt(inputLine.split(" ", 4)[1])).remoteSelect(user, inputLine);
                    } else if (inputLine.startsWith("gamechat")) {
                        gameList.get(Integer.parseInt(inputLine.split(" ", 3)[1])).gameCast(" [" + user + "]: " + inputLine.split(" ", 3)[2]);
                    }
                } else if (leadingByte == 1) {
                } else if (leadingByte == 2) {
                    System.out.println("Was here");
                    try {
                        inputLine = (String) objIn.readObject();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    System.out.println(inputLine);
                    if (inputLine.charAt(0) == '!') {
                        if (inputLine.equals("!hack")) {
                            GUI.append("Someone's employing hax of the leet-est variety. Abort! Abort!\n");
                            broadcast(user, "Hey, hey, HEY! Stop that!");
                        } else if (inputLine.equals("!crash")) {
                            GUI.append("Someone tried to crash the server and failed. High five.\n");
                            broadcast(user, "Nice try. NOPE. Hahahaha, FAILURE!");
                        } else if (inputLine.equals("!getinfo")) {
                            broadcast(user, "IP: " + InetAddress.getLocalHost() + "\tPort: " + socket.getLocalPort());
                        } else {
                            user.getChannel().channelcast("Bad server command issued - ignored.", 2);
                        }
                    } else if (inputLine.charAt(0) == '/') {
                        if (inputLine.startsWith("/me")) {
                            GUI.append(user.getUserName() + " " + inputLine.substring(4) + "\n");
                            user.getChannel().channelcast(user.getUserName() + " " + inputLine.substring(4), 2);
                        }
                    } else {
                        GUI.append("{" + user.getChannel().getName() + "} [" + user.getUserName() + "]: " + inputLine + "\n");
                        user.getChannel().channelcast("[" + user.getUserName() + "]: " + inputLine, 2);
                    }
                } else if (leadingByte == 3) {
                    GUI.append("Beginning user authentication\n");
                    try {
                        inputLine = (String) objIn.readObject();
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                    user = new User(socket, inputLine.substring(10));
                    GUI.append("Authentication complete\n");
                    GUI.addUser(user);
                    userList.add(user);
                    broadcast(user, "Authentication complete");
                    System.out.println(user);
                    channelList.get("Lobby").addUser(user);
                }
            }
            System.err.println(inputLine);
            user.getChannel().channelcast("Connection to remote user " + user.getUserName() + " terminated.", 2);
            user.getChannel().removeUserQuietly(user);
            userList.remove(user);
            GUI.removeUser(user);
            GUI.append("Connection to remote user " + user.getUserName() + " terminated.\n");
            objIn.close();
        } catch (SocketException se) {
            GUI.append("Unable to write to user " + user + "\n");
            System.err.println("Unable to write to user " + user);
            try {
                objIn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            userList.remove(user);
            user.getChannel().removeUserQuietly(user);
            user.getChannel().channelcast(user.getUserName() + " has disconnected.", 2);
            GUI.removeUser(user);
            GUI.append("Connection to remote user " + user.getUserName() + " closed.\n");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Unable to read input from connection.", "Network Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Broadcast a message to all currently connected users
     * @param output The message to be broadcast
     * @throws IOException 
     */
    public static void broadcast(String output) throws IOException {
        for (User u : userList) {
            u.getObjWriter().write(2);
            u.getObjWriter().flush();
            u.getObjWriter().writeObject(output);
            u.getObjWriter().flush();
        }
    }

    /**
	 * Broadcast a message to a specific user
	 * @param u The user to broadcast to
	 * @param output The message to be broadcast
	 * @throws IOException 
	 */
    public static void broadcast(User u, String output) throws IOException {
        u.getObjWriter().write(2);
        u.getObjWriter().flush();
        u.getObjWriter().writeObject(output);
        u.getObjWriter().flush();
    }

    public static ArrayList<User> getUserList() {
        return userList;
    }

    /**
     * Broadcast an object to all currently connected users
     * @param output The object to be broadcast
     * @param type The type of object being broadcast: 0 - Command(String), 1 - Turn, 2 - Message(String), 3 - Authorization, 4 - Terminate 
     * @throws IOException 
     */
    public static void broadcast(Object output, int type) throws IOException {
        for (User u : userList) {
            u.getObjWriter().write(type);
            u.getObjWriter().flush();
            u.getObjWriter().writeObject(output);
            u.getObjWriter().flush();
        }
    }

    /**
	 * Broadcast an object to a specific user
	 * @param u The user to broadcast to
	 * @param output The message to be broadcast
	 * @param type object type: 0 - Command(String), 1 - Turn, 2 - Message(String), 3 - Authorization, 4 - Terminate
	 * @throws IOException 
	 */
    public static void broadcast(User u, Object output, int type) throws SocketException, IOException {
        u.getObjWriter().write(type);
        u.getObjWriter().flush();
        u.getObjWriter().writeObject(output);
        u.getObjWriter().flush();
    }
}

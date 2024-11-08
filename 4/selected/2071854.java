package Networking.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;
import GUI.ServerGUI;

public class NamShubServer {

    private boolean listening = false;

    public boolean isListening() {
        return listening;
    }

    private ServerSocket serverSocket = null;

    private static ArrayList<User> userList = new ArrayList<User>();

    private static HashMap<String, Channel> channelList = new HashMap<String, Channel>();

    private static HashMap<Integer, Game> gameList = new HashMap<Integer, Game>();

    private ServerGUI GUI = null;

    private ConnectionHandler connectionHandler = null;

    public NamShubServer() {
        GUI = new ServerGUI(this);
        GUI.setVisible(true);
        channelList.put("Lobby", new Channel("Lobby", new ArrayList<User>(), false));
    }

    public void terminateSession() {
        if (listening == true) {
            try {
                ServerThread.broadcast("Server Shutting Down - Closing Connection");
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (User u : userList) {
                try {
                    ServerThread.broadcast(u, "Server shut down.", 4);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            userList.clear();
            GUI.clearUsers();
            listening = false;
            connectionHandler.setListening(false);
            try {
                serverSocket.close();
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(null, "Unable to close connection.", "Network Error", JOptionPane.ERROR_MESSAGE);
            }
            GUI.append("Server closed.\n");
        } else JOptionPane.showMessageDialog(null, "No open connections to terminate.", "PEBKAC", JOptionPane.ERROR_MESSAGE);
    }

    public void beginSession(int port, String IP) {
        if (listening == false) {
            listening = true;
            try {
                serverSocket = new ServerSocket(port);
                GUI.append("Started server on port " + port + " at address " + InetAddress.getLocalHost().toString() + "\n");
            } catch (UnknownHostException e) {
                JOptionPane.showMessageDialog(null, "Unable to resolve local host.", "Network Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException bpe) {
                JOptionPane.showMessageDialog(null, "Could not listen on port: " + port, "Port Unavailable", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }
            connectionHandler = new ConnectionHandler(serverSocket, GUI);
            connectionHandler.start();
        } else JOptionPane.showMessageDialog(null, "A connection is already open.", "User Error", JOptionPane.ERROR_MESSAGE);
    }

    public static ArrayList<User> getUserList() {
        return userList;
    }

    public static void setUserList(ArrayList<User> u) {
        userList = u;
    }

    public static HashMap<String, Channel> getChannelList() {
        return channelList;
    }

    public static void setChannelList(HashMap<String, Channel> channelList) {
        NamShubServer.channelList = channelList;
    }

    /**
	 * @return the gameList
	 */
    public static HashMap<Integer, Game> getGameList() {
        return gameList;
    }

    /**
	 * @param gameList the gameList to set
	 */
    public static void setGameList(HashMap<Integer, Game> gameList) {
        NamShubServer.gameList = gameList;
    }
}

package DaDTC;

import java.net.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 *
 * @author Owner
 */
public class ServerThread implements Runnable {

    DaDTC daDTC;

    InfoHub infoHub;

    private ServerSocket serverSocket = null;

    private Hashtable clients = new Hashtable();

    private Hashtable usernames = new Hashtable();

    private int clientConnections = 0;

    /** Creates a new instance of ServerThread */
    public ServerThread(DaDTC parent, InfoHub info) {
        daDTC = parent;
        infoHub = info;
    }

    public void run() {
        if (!infoHub.isOnline()) {
            try {
                serverSocket = new ServerSocket(infoHub.getServerPort());
                infoHub.setOnline();
                daDTC.pushTerminalLog("Server has been created. Listening for new connections...");
            } catch (Exception e) {
            }
        }
        while (infoHub.isOnline()) {
            try {
                Socket socket = serverSocket.accept();
                clients.put(socket, new ClientHub(socket));
                BufferedWriter bWrite = ((ClientHub) clients.get(socket)).getChannelWrite();
                BufferedReader bRead = ((ClientHub) clients.get(socket)).getChannelRead();
                String user = bRead.readLine().substring(infoHub.getCommConnect().length() + 1);
                if (user != null && user.length() > 0 && !usernames.containsKey(user)) {
                    daDTC.pushTerminalLog("Incomming client connection request...");
                    clientConnections++;
                    ((ClientHub) clients.get(socket)).setUsername(user);
                    usernames.put(user, socket);
                    daDTC.pushTerminalLog("Looking up hostname for client " + user + "...");
                    daDTC.pushTerminalLog("Client " + user + " (" + socket.getInetAddress().getHostName() + ") has connected.");
                    daDTC.setStatusConnected();
                    bWrite.write(infoHub.getCommConnectionAccepted() + " " + user);
                    bWrite.newLine();
                    bWrite.flush();
                    new ThreadControlMessages(socket).start();
                } else {
                    bWrite.write(infoHub.getCommUserExistance());
                    bWrite.newLine();
                    bWrite.flush();
                    clients.remove(socket);
                    socket.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public void sendMessage(String msg, Socket socket) {
        synchronized (clients) {
            BufferedWriter bWrite = null;
            try {
                if (socket == null) {
                    Enumeration allClients = clients.elements();
                    while (allClients.hasMoreElements()) {
                        bWrite = ((ClientHub) allClients.nextElement()).getChannelWrite();
                        bWrite.write(msg);
                        bWrite.newLine();
                        bWrite.flush();
                    }
                } else {
                    bWrite = ((ClientHub) clients.get(socket)).getChannelWrite();
                    bWrite.write(msg);
                    bWrite.newLine();
                    bWrite.flush();
                }
            } catch (IOException e) {
            }
        }
    }

    private class ThreadControlMessages extends Thread {

        private Thread thisThread = null;

        private Socket socket = null;

        public ThreadControlMessages(Socket socket) {
            this.socket = socket;
            this.setName("threadControlMessages_" + socket.toString());
        }

        public void run() {
            try {
                BufferedReader bRead = ((ClientHub) clients.get(socket)).getChannelRead();
                thisThread = Thread.currentThread();
                String strReceive = null;
                while (infoHub.isOnline() && thisThread == Thread.currentThread()) {
                    if ((strReceive = bRead.readLine()) != null && strReceive.length() > 0) {
                        if (strReceive.equalsIgnoreCase(infoHub.getCommClientDisconnected())) {
                            terminateClient(socket);
                            thisThread = null;
                        } else if (strReceive.startsWith(infoHub.getCommPrivateMessage())) {
                            String msg = infoHub.getCommPrivateMessageOf() + ((ClientHub) clients.get(socket)).getUsername() + strReceive.substring(strReceive.indexOf(" ", infoHub.getCommPrivateMessage().length() + 1));
                            sendMessage(msg, (Socket) usernames.get(strReceive.split(" ")[1]));
                        } else {
                            sendMessage("[" + ((ClientHub) clients.get(socket)).getUsername() + "] " + strReceive, null);
                        }
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    private void terminateClient(Socket socket) {
        synchronized (clients) {
            clientConnections--;
            if (clientConnections == 0) {
                daDTC.setStatusListening();
            }
            daDTC.pushTerminalLog("Client [" + ((ClientHub) clients.get(socket)).getUsername() + "] " + socket.getInetAddress().getHostAddress() + " disconnected");
            sendMessage(infoHub.getCommRemoveUser() + ((ClientHub) clients.get(socket)).getUsername(), null);
            usernames.remove(((ClientHub) clients.get(socket)).getUsername());
            clients.remove(socket);
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    public void terminate() {
        daDTC.setStatusNotConnected();
        sendMessage(infoHub.getCommServerTerminated(), null);
        infoHub.setOffline();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
        clientConnections = 0;
        clients.clear();
        usernames.clear();
        daDTC.pushTerminalLog("Server Terminated");
    }
}

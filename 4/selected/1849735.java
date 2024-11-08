package org.ugr.bluerose.devices;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Vector;
import org.ugr.bluerose.ICommunicationDevice;
import org.ugr.bluerose.threads.ReplyReadThread;
import org.ugr.bluerose.threads.RequestReadThread;
import org.ugr.bluerose.messages.*;

/**
* Module for transferring messages through the system
* default TCP transmission interface.
*
* @author Carlos Rodriguez Dominguez
* @date 16-10-2009
*/
public class TcpCompatibleDevice extends Thread implements ICommunicationDevice {

    protected java.util.Hashtable<String, TcpClient> clients;

    protected java.util.Hashtable<String, ReplyReadThread> clientThreads;

    protected String servantAddress = "";

    protected boolean isServant = false;

    protected int servantPort = -1;

    public TcpCompatibleDevice() {
        clients = new java.util.Hashtable<String, TcpClient>();
        clientThreads = new java.util.Hashtable<String, ReplyReadThread>();
    }

    public void broadcast(Vector<Byte> message) {
        java.util.Vector<String> neighbours = getNeighbours();
        for (int i = 0; i < neighbours.size(); i++) {
            write(neighbours.get(i), message);
        }
    }

    public void closeAllConnections() {
        clients.clear();
        clientThreads.clear();
    }

    public void closeConnection(String userID) {
        if (isConnectionOpenned(userID)) {
            clients.remove(userID);
            clientThreads.remove(userID);
        }
    }

    public String getDeviceName() {
        return "TcpCompatibleDevice";
    }

    public Vector<String> getNeighbours() {
        Vector<String> res = new Vector<String>();
        java.util.Enumeration<String> e = clients.keys();
        while (e.hasMoreElements()) {
            res.add(e.nextElement());
        }
        return res;
    }

    public String getServantIdentifier() {
        return servantAddress;
    }

    public boolean isAvailable() {
        java.util.Enumeration<String> e = clients.keys();
        while (e.hasMoreElements()) {
            if (clients.get(e.nextElement()).isOpenned()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlockantConnection() {
        return true;
    }

    public boolean isConnectionOpenned(String userID) {
        TcpClient cli = clients.get(userID);
        if (cli == null) return false; else return (cli.isOpenned());
    }

    public boolean isConnectionOriented() {
        return true;
    }

    public boolean isUserAvailable(String userID) {
        return isConnectionOpenned(userID);
    }

    public void openConnection(String userID, Dictionary<String, Vector<Byte>> pars) throws Exception {
        if (clients.get(userID) != null) return;
        String[] strs = userID.split(":");
        String host = strs[0];
        int port = Integer.parseInt(strs[1]);
        clients.put(userID, new TcpClient(host, port));
    }

    public void openConnection(String userID) throws Exception {
        this.openConnection(userID, null);
    }

    public Vector<Byte> read(String senderID) {
        TcpClient cli = clients.get(senderID);
        return cli.read();
    }

    public void setServantIdentifier(String servantID) {
        servantAddress = servantID;
    }

    public void setServant(String multiplexingId) {
        isServant = true;
        servantPort = Integer.parseInt(multiplexingId);
        if (servantAddress == null || servantAddress == "" || servantAddress == "localhost" || servantAddress == "127.0.0.1") {
            try {
                java.util.Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
                boolean fin = false;
                while (e.hasMoreElements() && !fin) {
                    NetworkInterface dev = e.nextElement();
                    if (!dev.getName().contains("lo") && dev.getName().length() <= 3) {
                        try {
                            java.util.Enumeration<InetAddress> e2 = dev.getInetAddresses();
                            while (e2.hasMoreElements()) {
                                InetAddress addr = e2.nextElement();
                                if (!addr.getClass().equals(java.net.Inet6Address.class)) {
                                    servantAddress = addr.getHostAddress() + ":" + servantPort;
                                    if (!servantAddress.startsWith("127")) {
                                        if (!servantAddress.startsWith("0.0.0.0") && !addr.getHostAddress().contains(":")) {
                                            fin = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            servantAddress = "127.0.0.1";
                            ex.printStackTrace();
                        }
                    }
                }
                if (!e.hasMoreElements()) {
                    System.out.println("Warning: Only local connections will be allowed. Please, check your network configuration");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            String results[] = servantAddress.split(":");
            if (results.length < 2) {
                servantAddress = servantAddress + ":" + servantPort;
            }
        }
        this.start();
    }

    public void waitForConnections() {
        try {
            this.join();
        } catch (InterruptedException e) {
        }
    }

    public boolean write(String readerID, Vector<Byte> data) {
        TcpClient cli = clients.get(readerID);
        if (cli == null) return false;
        if (data.get(8) == MessageHeader.REQUEST_MSG) {
            RequestMessage msg = new RequestMessage(data);
            if (((RequestMessageHeader) msg.header).mode == MessageHeader.TWOWAY_MODE) {
                if (!clientThreads.containsKey(readerID)) {
                    ReplyReadThread rth = new ReplyReadThread(this, readerID);
                    clientThreads.put(readerID, rth);
                    rth.start();
                }
            }
        }
        return cli.write(data);
    }

    @Override
    public void run() {
        java.net.ServerSocket servantSocket = null;
        try {
            servantSocket = new java.net.ServerSocket();
            servantSocket.setReuseAddress(true);
            servantSocket.bind(new java.net.InetSocketAddress(servantPort));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        while (true) {
            Socket newsockfd = null;
            try {
                newsockfd = servantSocket.accept();
            } catch (IOException e1) {
            }
            if (newsockfd != null) {
                String userID = newsockfd.getInetAddress().getHostAddress() + ":" + newsockfd.getPort();
                System.out.println("Connection opened: " + userID);
                try {
                    clients.put(userID, new TcpClient(newsockfd));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                RequestReadThread th = new RequestReadThread(this, userID);
                th.start();
            }
        }
    }
}

package rpg.server.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rpg.server.packets.Packet;

/**
 * rothens.tumblr.com
 * @author Rothens
 */
public class NetworkManager {

    public static final Object threadSyncObject = new Object();

    public static int numReadThreads;

    public static int numWriteThreads;

    private Object sendQueueLock;

    private Socket networkSocket;

    private final SocketAddress remoteAddress;

    private DataInputStream socketInputStream;

    private DataOutputStream socketOutputStream;

    private boolean isRunning;

    private List readPackets;

    private List dataPackets;

    private NetworkHandler nethandler;

    private boolean isServerTerminating = false;

    private Thread writeThread;

    private Thread readThread;

    private boolean isTerminating = false;

    private String terminationReason = "";

    private int lastReadAgo;

    private int queueByteLength;

    public NetworkManager(Socket socket, String name, NetworkHandler nethandler) throws IOException {
        sendQueueLock = new Object();
        isRunning = true;
        readPackets = Collections.synchronizedList(new ArrayList());
        dataPackets = Collections.synchronizedList(new ArrayList());
        networkSocket = socket;
        remoteAddress = socket.getRemoteSocketAddress();
        lastReadAgo = 0;
        queueByteLength = 0;
        this.nethandler = nethandler;
        try {
            socket.setSoTimeout(30000);
            socket.setTrafficClass(24);
        } catch (SocketException se) {
            System.err.println(se.getMessage());
        }
        socketInputStream = new DataInputStream(socket.getInputStream());
        socketOutputStream = new DataOutputStream(socket.getOutputStream());
        readThread = new ReaderThread(this, name + " reader thread");
        writeThread = new WriterThread(this, name + " writer thread");
        readThread.start();
        writeThread.start();
    }

    public void setNetworkHandler(NetworkHandler nethandler) {
        this.nethandler = nethandler;
    }

    private boolean sendPacket() {
        boolean flag = false;
        try {
            if (!dataPackets.isEmpty()) {
                Packet packet;
                synchronized (sendQueueLock) {
                    packet = (Packet) dataPackets.remove(0);
                    queueByteLength -= packet.getSize() + 1;
                }
                Packet.writePacket(packet, socketOutputStream);
                flag = true;
            }
        } catch (Exception e) {
            if (!isTerminating) {
                e.printStackTrace();
            }
            return false;
        }
        return flag;
    }

    private boolean readPacket() {
        boolean flag = false;
        try {
            Packet packet = Packet.readPacket(socketInputStream, nethandler.isServerHandler());
            if (packet != null) {
                readPackets.add(packet);
                flag = true;
            }
        } catch (Exception ex) {
            if (!isTerminating) {
                ex.printStackTrace();
            }
            return false;
        }
        return flag;
    }

    public void wakeThreads() {
        readThread.interrupt();
        writeThread.interrupt();
    }

    public void networkShutdown(String s) {
        if (!isRunning) {
            return;
        }
        isTerminating = true;
        terminationReason = s;
        isRunning = false;
        try {
            socketInputStream.close();
            socketInputStream = null;
        } catch (Exception e) {
        }
        try {
            socketOutputStream.close();
            socketOutputStream = null;
        } catch (Exception e) {
        }
        try {
            networkSocket.close();
            networkSocket = null;
        } catch (Exception e) {
        }
    }

    public void processReadPackets() {
        if (queueByteLength > 0x100000) {
            networkShutdown("Buffer overflow!");
        }
        if (readPackets.isEmpty()) {
            if (lastReadAgo++ == 1200) {
                networkShutdown("Connection timed out!");
            }
        } else {
            lastReadAgo = 0;
        }
        Packet packet;
        for (int i = 1000; !readPackets.isEmpty() && i-- >= 0; packet.processPacketData(nethandler)) {
            packet = (Packet) readPackets.remove(0);
        }
        wakeThreads();
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void addToSendQueue(Packet packet) {
        if (isServerTerminating) {
            return;
        }
        synchronized (sendQueueLock) {
            queueByteLength += packet.getSize() + 1;
            dataPackets.add(packet);
        }
    }

    public void serverShutdown() {
        wakeThreads();
        isServerTerminating = true;
        readThread.interrupt();
    }

    public Socket getSocket() {
        return networkSocket;
    }

    static DataOutputStream getOutputStream(NetworkManager networkmanager) {
        return networkmanager.socketOutputStream;
    }

    static boolean isRunning(NetworkManager networkmanager) {
        return networkmanager.isRunning;
    }

    static boolean isServerTerminating(NetworkManager networkmanager) {
        return networkmanager.isServerTerminating;
    }

    static boolean readNetworkPacket(NetworkManager networkmanager) {
        return networkmanager.readPacket();
    }

    static boolean sendNetworkPacket(NetworkManager networkmanager) {
        return networkmanager.sendPacket();
    }

    static boolean getIsTerminating(NetworkManager networkmanager) {
        return networkmanager.isTerminating;
    }

    static Thread getReadThread(NetworkManager networkmanager) {
        return networkmanager.readThread;
    }

    static Thread getWriteThread(NetworkManager networkmanager) {
        return networkmanager.writeThread;
    }
}

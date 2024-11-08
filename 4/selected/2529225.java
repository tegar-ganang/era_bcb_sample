package PRISM.rocs.moos;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MoosClient {

    String clientName;

    String serverName;

    Socket sock;

    MTQueue outqueue = new MTQueue();

    MoosMessage EXIT = new MoosMessage();

    ReaderThread reader;

    WriterThread writer;

    List<MoosListener> listeners = new ArrayList<MoosListener>();

    int msPollDelay = 200;

    int messageCount = 0;

    static final int MAX_MESSAGES_PER_PACKET = 10;

    static MoosClient ref = null;

    public MoosClient(String clientName, String serverName) throws UnknownHostException, IOException {
        this.clientName = clientName;
        this.serverName = serverName;
        connect(serverName, 9000);
        ref = this;
    }

    public static synchronized MoosClient getMoosClient() {
        if (ref == null) {
            System.out.println("Moos client not initialised !");
        }
        return ref;
    }

    protected void connect(String ipaddr, int port) throws UnknownHostException, IOException {
        System.out.println("Creating Socket to " + ipaddr + "...");
        sock = new Socket(ipaddr, port);
        System.out.println("Socket created");
        reader = new ReaderThread();
        reader.start();
        System.out.println("Moos reader started");
        MoosMessage mm = new MoosMessage();
        mm.messageId = 0;
        mm.messageType = MoosMessage.DATA;
        mm.dataType = MoosMessage.STRING;
        mm.s = clientName;
        System.out.println("Moos connetc: adding first packet");
        MoosPacket packet = new MoosPacket();
        packet.addMessage(mm);
        System.out.println("Moos connetc: sending first packet");
        DataOutputStream douts = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));
        packet.write(douts);
        douts.flush();
        System.out.println("Moos connetc: starting Writer thread");
        writer = new WriterThread();
        writer.start();
    }

    public void notify(String channel, double data) {
        MoosMessage message = new MoosMessage();
        message.messageId = messageCount++;
        message.messageType = MoosMessage.NOTIFY;
        message.dataType = MoosMessage.DOUBLE;
        message.time = System.currentTimeMillis() / 1000.0;
        message.source = clientName;
        message.key = channel;
        message.d1 = data;
        outqueue.put(message);
    }

    public void notify(String channel, String data) {
        MoosMessage message = new MoosMessage();
        message.messageId = messageCount++;
        message.messageType = MoosMessage.NOTIFY;
        message.dataType = MoosMessage.STRING;
        message.time = System.currentTimeMillis() / 1000.0;
        message.source = clientName;
        message.key = channel;
        message.s = data;
        outqueue.put(message);
    }

    public void register(String channel, double minUpdatePeriod) {
        MoosMessage message = new MoosMessage();
        message.messageId = messageCount++;
        message.messageType = MoosMessage.REGISTER;
        message.dataType = MoosMessage.DOUBLE;
        message.source = clientName;
        message.key = channel;
        message.s = "";
        message.d1 = minUpdatePeriod;
        outqueue.put(message);
    }

    protected void receivedPacket(MoosPacket packet) {
        int nummessages = packet.getNumMessages();
        synchronized (listeners) {
            for (int i = nummessages - 1; i >= 0; i--) {
                MoosMessage mm = packet.getMessage(i);
                if (mm.messageType == MoosMessage.NULL_MSG) continue;
                for (MoosListener listener : listeners) {
                    listener.receivedMoosMessage(mm);
                }
            }
        }
    }

    public void addListener(MoosListener ml) {
        synchronized (listeners) {
            if (!listeners.contains(ml)) listeners.add(ml);
        }
    }

    public void removeListener(MoosListener ml) {
        synchronized (listeners) {
            if (listeners.contains(ml)) listeners.remove(ml);
        }
    }

    /** Processes data from the hub. **/
    protected class ReaderThread extends Thread {

        DataInputStream ins;

        public ReaderThread() throws IOException {
            ins = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
        }

        public void run() {
            while (true) {
                try {
                    MoosPacket packet = new MoosPacket();
                    packet.read(ins);
                    receivedPacket(packet);
                } catch (IOException ex) {
                    System.out.println("MOOSClient ReaderThread, lost connection " + ex.getMessage());
                    break;
                } catch (Exception ex) {
                    System.out.println("MOOSClient ReaderThread, unexpected exception " + ex.getMessage());
                }
            }
            outqueue.put(EXIT);
        }
    }

    /** Writes objects in the outqueue to the socket. **/
    protected class WriterThread extends Thread {

        DataOutputStream outs;

        public WriterThread() throws IOException {
            outs = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));
        }

        public void run() {
            while (true) {
                try {
                    MoosPacket packet = new MoosPacket();
                    while (packet.getNumMessages() < MAX_MESSAGES_PER_PACKET && outqueue.size() > 0) {
                        MoosMessage mm = (MoosMessage) (outqueue.getBlock());
                        packet.addMessage(mm);
                    }
                    if (packet.getNumMessages() == 0) {
                        MoosMessage mm = new MoosMessage();
                        mm.messageId = messageCount++;
                        mm.messageType = MoosMessage.DATA;
                        mm.dataType = MoosMessage.DOUBLE;
                        mm.source = clientName;
                        mm.key = "";
                        mm.s = "";
                        packet.addMessage(mm);
                    }
                    packet.write(outs);
                    outs.flush();
                } catch (Exception ex) {
                    System.out.println("Writer Thread 1 Exception " + ex);
                    break;
                }
                try {
                    if (outqueue.size() == 0) Thread.sleep(msPollDelay);
                } catch (InterruptedException ex) {
                    System.out.println("Writer Thread Exception 2 " + ex);
                }
            }
        }
    }
}

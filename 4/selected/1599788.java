package PowersetThingy;

import java.awt.Frame;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import javax.swing.JOptionPane;
import tools.*;
import tools.Debug;
import tools.Exceptions.*;
import AccordionPowersetDrawer.Powerset;
import AccordionPowersetDrawer.SetNode;

class Client implements Runnable {

    static final int MESSAGE_PLAY = 0;

    static final int MESSAGE_PAUSE = 1;

    static final int MESSAGE_RESET = 2;

    static final int MESSAGE_DISC = 3;

    static final int MESSAGE_CONSTRAINT = 4;

    static int PORT = 6677;

    static String HOST = "peccary.cs.ubc.ca";

    static int BUFFSIZE = 4000;

    SocketChannel sChannel;

    ByteBuffer inBuff = ByteBuffer.allocate(BUFFSIZE);

    ByteBuffer outBuff = ByteBuffer.allocate(1024);

    Selector readSelector;

    Selector writeSelector;

    boolean sendMsgFlag = false;

    boolean CONNECTFLAG = false;

    boolean readyToDisconnect = false;

    String msg;

    PowersetThingy pt_;

    Powerset powerset;

    public Client(String host, int port, PowersetThingy pt) {
        HOST = host;
        PORT = port;
        pt_ = pt;
        powerset = pt.powerset;
    }

    public Client(PowersetThingy pt) {
        pt_ = pt;
        powerset = pt.powerset;
    }

    public void setHost(String host) throws ClientNotDisconnectedException {
        if (isConnected()) throw new ClientNotDisconnectedException();
        HOST = host;
    }

    public void setPort(int port) throws ClientNotDisconnectedException {
        if (isConnected()) throw new ClientNotDisconnectedException();
        PORT = port;
    }

    public String getHost() {
        return HOST;
    }

    public int getPort() {
        return PORT;
    }

    public void connect() throws ClientNotDisconnectedException, ConnectException {
        if (isConnected()) throw new ClientNotDisconnectedException(); else {
            Debug.print("Client::Connect() - Connecting...", 2);
            try {
                sChannel = createSocketChannel(HOST, PORT);
                while (!sChannel.finishConnect()) {
                }
            } catch (IOException e) {
                throw new ConnectException();
            }
            Debug.print("Client::Connect() - Connected!", 2);
            try {
                readSelector = Selector.open();
                writeSelector = Selector.open();
                sChannel.register(readSelector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
                sChannel.register(writeSelector, SelectionKey.OP_WRITE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static SocketChannel createSocketChannel(String hostName, int port) throws IOException {
        SocketChannel sChannel = SocketChannel.open();
        sChannel.configureBlocking(false);
        sChannel.connect(new InetSocketAddress(hostName, port));
        return sChannel;
    }

    public boolean isConnected() {
        if (sChannel == null) return false;
        return sChannel.isConnected();
    }

    public void disconnect() throws ClientNotConnectedException {
        if (!isConnected()) throw new ClientNotConnectedException(); else {
            Debug.print("Client::Disconnect() - Disconnecting...", 2);
            sendMsg("disc");
            try {
                sChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String createMsg(int messageType) {
        String ans = null;
        switch(messageType) {
            case MESSAGE_PLAY:
                Debug.print("createMsg::messageType = PLAY", 3);
                ans = "play";
                break;
            case MESSAGE_PAUSE:
                Debug.print("createMsg::messageType = PAUSE", 3);
                ans = "paus";
                break;
            case MESSAGE_RESET:
                Debug.print("createMsg::messageType = RESET", 3);
                ans = "rese";
                break;
            case MESSAGE_DISC:
                Debug.print("createMsg::messageType = DISC", 3);
                ans = "disc";
                break;
        }
        return ans;
    }

    public static String createMsg(int messageType, ConstraintMessage constraintMsg) {
        String ans = null;
        if (messageType == MESSAGE_CONSTRAINT) {
            try {
                ans = constraintMsg.getString();
            } catch (FreqConstraintNotSetException e) {
                ans = "";
                e.printStackTrace();
            }
        }
        return ans;
    }

    public void sendMsg(String msg) throws ClientNotConnectedException {
        if (!isConnected()) throw new ClientNotConnectedException();
        try {
            int select = writeSelector.select();
            while (select == 0) {
                select = writeSelector.select();
                Debug.print("sendMsg::Socket not ready for write", 3);
            }
            if (select < 0) {
                Debug.print("sendMsg::Error! (sendMsg). write select < 0.", 3);
            }
            if (select > 0) {
                Debug.print("sendMsg::Socket is ready for write", 3);
                Set readyKeys = writeSelector.selectedKeys();
                Iterator readyItor = readyKeys.iterator();
                while (readyItor.hasNext()) {
                    SelectionKey key = (SelectionKey) readyItor.next();
                    readyItor.remove();
                    SocketChannel keyChannel = (SocketChannel) key.channel();
                    if (key.isWritable()) {
                        outBuff = null;
                        outBuff = ByteBuffer.wrap(new String(msg).getBytes());
                        int numBytesWritten = keyChannel.write(outBuff);
                        outBuff.clear();
                        Debug.print("sendMsg::Writing finished", 3);
                    }
                }
            }
        } catch (IOException e) {
            Debug.print("sendMsg::Error! (sendMsg). " + e, 3);
        }
    }

    public void run() {
        int readStatus = 0;
        int setSize = 0;
        int setCount = 0;
        int totalBytesRecvd = 0;
        boolean addOrRemove = true;
        SetNode newNode;
        ArrayList newSet = new ArrayList();
        outer: while (true) {
            if (isConnected()) {
                ByteBuffer inBuff = ByteBuffer.allocateDirect(1024);
                try {
                    inBuff.clear();
                    int numBytesRead = sChannel.read(inBuff);
                    if (numBytesRead == -1) {
                        sChannel.close();
                    } else {
                        inBuff.flip();
                        Debug.print("Client Listener()::Reading ints. NumBytesRead: " + numBytesRead, 3);
                        totalBytesRecvd = totalBytesRecvd + numBytesRead;
                        for (int i = 0; i < numBytesRead; i = i + 4) {
                            int theInt = inBuff.getInt();
                            Debug.print("Client Listener()::readStatus is " + readStatus, 3);
                            switch(readStatus) {
                                case (0):
                                    Debug.print("Client Listener()::setSize  = " + theInt, 3);
                                    setSize = theInt;
                                    setCount = 0;
                                    newSet = new ArrayList();
                                    readStatus = 1;
                                    break;
                                case (1):
                                    Debug.print("Client Listener()::add remove  is  " + theInt, 3);
                                    addOrRemove = (theInt == 1);
                                    readStatus = 2;
                                    break;
                                case (2):
                                    Debug.print("Client Listener()::adding set to list " + theInt, 3);
                                    theInt = theInt % powerset.universeCount;
                                    newSet.add(new Integer(theInt));
                                    setCount++;
                                    if (setCount == setSize) {
                                        int setKey = powerset.setToKey(newSet);
                                        newNode = new SetNode(setKey, powerset);
                                        if (addOrRemove) {
                                            Debug.print("Client Listener()::Add Set Key: " + newNode.getKey(), 3);
                                            powerset.putSet(newNode);
                                            readStatus = 0;
                                        } else {
                                            Debug.print("Client Listener()::Remove Set Key: " + newNode.getKey(), 3);
                                            powerset.removeSet(setKey);
                                            readStatus = 0;
                                        }
                                        pt_.requestRedrawAll();
                                    }
                                    break;
                            }
                        }
                    }
                } catch (IOException e) {
                    Debug.print("Client Listener::Connection was closed prematurely, closing connection.", 2);
                    try {
                        sChannel.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    break outer;
                }
            }
        }
    }
}

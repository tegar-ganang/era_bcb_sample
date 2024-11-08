package com.quikj.server.framework;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class AceIPCServer extends AceThread implements AceIPCEntityInterface {

    private static final int MAX_INIT_FAILURES_IN_A_ROW = 10;

    private DatagramSocket socket = null;

    private AceDatagram sockListener = null;

    private Object socketLock = new Object();

    private HashMap clientList = new HashMap();

    private int maxConnections;

    private int hbInterval;

    private AceThread unsolMsgHandler = null;

    private AceThread connectHandler = null;

    private AceThread disconnectHandler = null;

    private long userParm;

    private int port;

    public AceIPCServer(long user_parm, String name, int port, int max_connections, int hb_interval, AceThread unsol_msg_handler, AceThread connect_handler, AceThread disconnect_handler) throws IOException, AceException {
        super(name);
        if (initNewSocket(port, user_parm) == false) {
            throw new AceException("Datagram socket initialization failed");
        }
        userParm = user_parm;
        maxConnections = max_connections;
        hbInterval = hb_interval;
        unsolMsgHandler = unsol_msg_handler;
        connectHandler = connect_handler;
        disconnectHandler = disconnect_handler;
        this.port = port;
    }

    public static void main(String[] args) {
        class ServerUser extends AceThread {

            class ConnectedClient {

                private InetAddress address;

                private int port;

                public ConnectedClient(InetAddress addr, int port) {
                    this.address = addr;
                    this.port = port;
                }

                public InetAddress getAddress() {
                    return address;
                }

                public int getPort() {
                    return port;
                }
            }

            class DataSender extends AceThread {

                private int sendInterval;

                private ServerUser parent;

                public DataSender(int send_data_interval, ServerUser parent) throws IOException {
                    super();
                    sendInterval = send_data_interval;
                    this.parent = parent;
                    System.out.println("DATA SENDER THREAD ID = " + getAceThreadId());
                }

                public void dispose() {
                }

                public void run() {
                    int msg_counter = 0;
                    byte[] msg_data = new byte[4];
                    try {
                        while (true) {
                            sleep(sendInterval);
                            AceInputSocketStream.intToBytesMsbFirst(++msg_counter, msg_data, 0);
                            parent.broadcastMessage(msg_data, 0, msg_data.length);
                        }
                    } catch (InterruptedException ex) {
                        System.err.println("DataSender sleep interrupted");
                    }
                }
            }

            private AceIPCServer ipcServer;

            private DataSender dataSender = null;

            private ArrayList clientList = new ArrayList();

            public ServerUser(String name, int port, int max_connections, int hb_interval, int send_data_interval) throws IOException, AceException {
                super(name);
                ipcServer = new AceIPCServer(5000, name, port, max_connections, hb_interval, this, this, this);
                if (send_data_interval > 0) {
                    dataSender = new DataSender(send_data_interval, this);
                }
                System.out.println(name + " THREAD ID = " + getAceThreadId());
            }

            public void broadcastMessage(byte[] msg_data, int offset, int length) {
                synchronized (clientList) {
                    int num_elements = clientList.size();
                    for (int i = 0; i < num_elements; i++) {
                        ConnectedClient element = (ConnectedClient) clientList.get(i);
                        if (ipcServer.sendIPCMessage(msg_data, offset, length, this, element.getAddress(), element.getPort()) == false) {
                            System.err.println(getName() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " Broadcast sending failed to client addr = " + (element.getAddress()).toString() + ", port = " + element.getPort() + " : " + ((AceThread) (Thread.currentThread())).getErrorMessage());
                        }
                    }
                }
            }

            public void run() {
                ipcServer.start();
                if (dataSender != null) {
                    dataSender.start();
                }
                while (true) {
                    AceMessageInterface message = waitMessage();
                    if (message == null) {
                        System.err.println(getName() + " Null message encountered");
                        continue;
                    } else if ((message instanceof AceSignalMessage) == true) {
                        System.out.println(getName() + " Signal received, ID = " + ((AceSignalMessage) message).getSignalId() + ", signal message = " + ((AceSignalMessage) message).getMessage());
                        ipcServer.dispose();
                        if (dataSender != null) {
                            dataSender.dispose();
                        }
                        super.dispose();
                        break;
                    }
                    if ((message instanceof AceIPCMessage) == true) {
                        AceIPCMessage msg = (AceIPCMessage) message;
                        switch(msg.getEvent()) {
                            case AceIPCMessage.CONNECTION_ESTABLISHED:
                                {
                                    InetAddress addr = msg.getFarEndAddress();
                                    int port = msg.getFarEndPort();
                                    System.out.println(getName() + '_' + getAceThreadId() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " CONNECTION ESTABLISHED WITH CLIENT ADDR = " + addr + ", PORT = " + port + ", registration data size = " + msg.getUserDataLength() + ", reg bytes = " + AceIPCMessage.dumpRawBytes(msg.getMessage(), msg.getUserDataOffset(), msg.getUserDataLength()) + ", userparm=" + msg.getUserParm());
                                    if (dataSender != null) {
                                        synchronized (clientList) {
                                            if (clientList.add(new ConnectedClient(addr, port)) == false) {
                                                System.err.println(getName() + " Couldn't add new client into list, addr = " + addr + ", port = " + port);
                                            }
                                        }
                                    }
                                }
                                break;
                            case AceIPCMessage.DISCONNECT:
                                {
                                    System.out.println(getName() + '_' + getAceThreadId() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " CONNECTION DISCONNECTED, CLIENT ADDR = " + msg.getFarEndAddress() + ", PORT = " + msg.getFarEndPort() + ", userparm=" + msg.getUserParm());
                                    if (dataSender != null) {
                                        synchronized (clientList) {
                                            int num_elements = clientList.size();
                                            int i;
                                            for (i = 0; i < num_elements; i++) {
                                                ConnectedClient element = (ConnectedClient) clientList.get(i);
                                                if (element.getAddress().equals(msg.getFarEndAddress())) if (element.getPort() == msg.getFarEndPort()) {
                                                    break;
                                                }
                                            }
                                            if (i < num_elements) {
                                                clientList.remove(i);
                                            }
                                        }
                                    }
                                }
                                break;
                            case AceIPCMessage.MESSAGE_RECEIVED:
                                {
                                    int msg_num = (int) AceInputSocketStream.octetsToIntMsbFirst(msg.getMessage(), msg.getUserDataOffset(), msg.getUserDataLength());
                                    System.out.println(getName() + '_' + getAceThreadId() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " RECEIVED " + ((msg.solicitedMessage() == true) ? "solicited " : "unsolicited ") + "senderThreadID=" + msg.getSenderThreadId() + " userparm=" + msg.getUserParm() + "    :  " + msg_num);
                                    if (dataSender == null) {
                                        byte[] reply = new byte[4];
                                        AceInputSocketStream.intToBytesMsbFirst(++msg_num, reply, 0);
                                        if (ipcServer.sendIPCMessage(reply, 0, reply.length, msg.getSenderThreadId(), this, msg.getFarEndAddress(), msg.getFarEndPort()) == false) {
                                            System.err.println(getName() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " Message sending failed : " + getErrorMessage());
                                        }
                                    }
                                }
                                break;
                            default:
                                {
                                    System.err.println(getName() + " Unexpected IPC message event encountered : " + msg.getEvent());
                                }
                                break;
                        }
                    } else {
                        System.err.println(getName() + " Unexpected Ace message type encountered : " + message.messageType());
                    }
                }
            }
        }
        try {
            int port = 3000;
            int max_connections = 5;
            int hb_interval = 2000;
            int send_data_interval = 0;
            if ((args.length != 0) && (args.length != 4)) {
                System.out.println("Arguments (all or nothing): <port>, <max connections>, <hb interval(ms)>, <send user data interval(ms) - if 0, sends upon receipt>");
                System.out.println("Defaults: port=" + port + ", max connections=" + max_connections + ", hb interval=" + hb_interval + ", send user data interval=" + send_data_interval);
                System.exit(0);
            }
            if (args.length == 4) {
                try {
                    port = Integer.parseInt(args[0]);
                    max_connections = Integer.parseInt(args[1]);
                    hb_interval = Integer.parseInt(args[2]);
                    send_data_interval = Integer.parseInt(args[3]);
                } catch (NumberFormatException ex) {
                    System.err.println("Input must be numeric");
                    System.exit(1);
                }
            }
            AceTimer.Instance().start();
            ServerUser user = new ServerUser("TestServer", port, max_connections, hb_interval, send_data_interval);
            user.start();
            user.join();
            System.exit(0);
        } catch (IOException ex) {
            System.err.println("IOException in main " + ex.getMessage());
            System.exit(1);
        } catch (AceException ex) {
            System.err.println("AceException in main " + ex.getMessage());
            System.exit(1);
        } catch (InterruptedException ex) {
            System.err.println("InterruptedException in main " + ex.getMessage());
            System.exit(1);
        }
    }

    private void addClient(AceIPCServerConnection client_element, byte[] reg_data, int offset, int length) {
        synchronized (clientList) {
            int addr_int = addressToInt(client_element.getClientAddress());
            HashMap element = (HashMap) clientList.get(new Integer(addr_int));
            if (element == null) {
                element = new HashMap();
                element.put(new Integer(client_element.getClientPort()), client_element);
                clientList.put(new Integer(addr_int), element);
            } else {
                element.put(new Integer(client_element.getClientPort()), client_element);
            }
        }
        if (connectHandler != null) {
            AceIPCMessage msg_for_user = new AceIPCMessage(AceIPCMessage.CONNECTION_ESTABLISHED, this, reg_data, offset, length, client_element.getClientAddress(), client_element.getClientPort(), userParm);
            if (connectHandler.sendMessage(msg_for_user) == false) {
                System.err.println(getName() + ": AceIPCServer.addClient() -- Could not send IPC message to the user connect handler thread : " + getErrorMessage());
            }
        }
    }

    private int addressToInt(InetAddress addr) {
        int ret = 0;
        byte[] addr_bytes = addr.getAddress();
        try {
            ret = (int) AceInputSocketStream.octetsToIntMsbFirst(addr_bytes, 0, 4);
        } catch (NumberFormatException ex) {
            System.err.println(getName() + ": AceIPCServer.addressToInt() -- NumberFormatException converting InetAddress to int : " + ex.getMessage());
        }
        return ret;
    }

    protected void connectionClosed(AceIPCServerConnection conn, boolean send_disc_msg) {
        removeClient(conn);
        if (send_disc_msg == true) {
            sendDisconnectMessage(conn.getClientAddress(), conn.getClientPort());
        }
    }

    public void dispose() {
        dropConnections();
        dropSocket();
        if (this.isAlive() == true) {
            if (interruptWait(AceSignalMessage.SIGNAL_TERM, "Normal IPC Server dispose") == false) {
                System.err.println(getName() + ": AceIPCServer.dispose() -- Could not interrupt own wait : " + getErrorMessage());
                super.dispose();
            }
        } else {
            super.dispose();
        }
    }

    private void dropConnection(AceIPCServerConnection conn, boolean send_disc_msg) {
        InetAddress client_addr = conn.getClientAddress();
        int client_port = conn.getClientPort();
        conn.dispose();
        removeClient(conn);
        if (send_disc_msg == true) {
            sendDisconnectMessage(client_addr, client_port);
        }
    }

    private void dropConnections() {
        synchronized (clientList) {
            Collection elements = clientList.values();
            for (Iterator i = elements.iterator(); i.hasNext() == true; ) {
                HashMap element = (HashMap) i.next();
                Collection subelements = element.values();
                for (Iterator j = subelements.iterator(); j.hasNext() == true; ) {
                    AceIPCServerConnection conn = (AceIPCServerConnection) j.next();
                    InetAddress addr = conn.getClientAddress();
                    int port = conn.getClientPort();
                    conn.dispose();
                    if (disconnectHandler != null) {
                        AceIPCMessage msg_for_user = new AceIPCMessage(AceIPCMessage.DISCONNECT, this, addr, port, userParm);
                        if (disconnectHandler.sendMessage(msg_for_user) == false) {
                            System.err.println(getName() + ": AceIPCServer.dropConnections() -- Could not send IPC message to the user disconnect handler thread : " + getErrorMessage());
                        }
                    }
                }
                element.clear();
            }
            clientList.clear();
        }
    }

    private void dropSocket() {
        synchronized (socketLock) {
            if (sockListener != null) {
                sockListener.dispose();
                sockListener = null;
                socket = null;
            }
            flushMessages();
        }
    }

    private AceIPCServerConnection findClient(InetAddress addr, int port) {
        synchronized (clientList) {
            int addr_int = addressToInt(addr);
            HashMap element = (HashMap) clientList.get(new Integer(addr_int));
            if (element != null) {
                return ((AceIPCServerConnection) element.get(new Integer(port)));
            }
        }
        return null;
    }

    private void getNewSocket() {
        dropConnections();
        dropSocket();
        for (int i = 0; i < MAX_INIT_FAILURES_IN_A_ROW; i++) {
            if (initNewSocket(port, userParm) == true) {
                sockListener.start();
                break;
            }
        }
    }

    private boolean initNewSocket(int port, long user_parm) {
        synchronized (socketLock) {
            try {
                socket = new DatagramSocket(port);
            } catch (SocketException ex) {
                System.err.println(getName() + ": AceIPCServer.initNewSocket() -- Socket error creating DatagramSocket : " + ex.getMessage() + ' ' + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF));
                return false;
            }
            try {
                sockListener = new AceDatagram(user_parm, getName() + "_sockListener", this, socket, AceIPCMessage.MAX_IPC_MSG_SIZE);
            } catch (IOException ex) {
                System.err.println(getName() + ": AceIPCServer.initNewSocket() -- IO error creating AceDatagram : " + ex.getMessage());
                socket.close();
                socket = null;
                return false;
            } catch (AceException ex) {
                System.err.println(getName() + ": AceIPCServer.initNewSocket() -- Ace error creating AceDatagram : " + ex.getMessage());
                socket.close();
                socket = null;
                return false;
            }
        }
        return true;
    }

    private void processConnReqMessage(AceIPCConnReqMessage conn_message, InetAddress addr, int port) {
        int current_num_connections = 0;
        synchronized (clientList) {
            Collection elements = clientList.values();
            for (Iterator i = elements.iterator(); i.hasNext() == true; ) {
                current_num_connections += ((HashMap) i.next()).size();
            }
        }
        if (current_num_connections < maxConnections) {
            try {
                AceIPCServerConnection client_conn = new AceIPCServerConnection(getName() + '_' + addr + '_' + port, addr, port, hbInterval, this);
                if (sendConnectResponseMessage(AceIPCConnRespMessage.STATUS_OK, addr, port) == true) {
                    addClient(client_conn, conn_message.getBytes(), conn_message.userDataOffset(), conn_message.userDataLength());
                    client_conn.start();
                } else {
                    client_conn.dispose();
                }
            } catch (IOException ex) {
                System.err.println(getName() + ": AceIPCServer.processConnReqMessage() -- IOException creating AceIPCServerConnection : " + ex.getMessage());
                boolean status = sendConnectResponseMessage(AceIPCConnRespMessage.STATUS_REFUSED, addr, port);
            }
        } else {
            boolean status = sendConnectResponseMessage(AceIPCConnRespMessage.STATUS_TRY_LATER, addr, port);
        }
    }

    private void processEvent(AceMessageInterface message) {
        if ((message instanceof AceDatagramMessage) == true) {
            if (((AceDatagramMessage) message).getStatus() == AceDatagramMessage.READ_COMPLETED) {
                try {
                    AceIPCMessageParser parser = new AceIPCMessageParser(((AceDatagramMessage) message).getBuffer(), ((AceDatagramMessage) message).getLength());
                    int msg_type = parser.getMessageType();
                    AceIPCServerConnection client_conn = findClient(((AceDatagramMessage) message).getAddress(), ((AceDatagramMessage) message).getPort());
                    switch(msg_type) {
                        case AceIPCMessageInterface.CONN_REQ_MSG:
                            {
                                if (client_conn != null) {
                                    client_conn.dispose();
                                    removeClient(client_conn);
                                }
                                processConnReqMessage((AceIPCConnReqMessage) parser.getMessage(), ((AceDatagramMessage) message).getAddress(), ((AceDatagramMessage) message).getPort());
                            }
                            break;
                        default:
                            {
                                switch(msg_type) {
                                    case AceIPCMessageInterface.HB_MSG:
                                        {
                                            if (client_conn != null) {
                                                if (client_conn.resetReceiveTiming() == false) {
                                                    dropConnection(client_conn, true);
                                                }
                                            }
                                        }
                                        break;
                                    case AceIPCMessageInterface.DISCONNECT_MSG:
                                        {
                                            if (client_conn != null) {
                                                client_conn.dispose();
                                                removeClient(client_conn);
                                            }
                                        }
                                        break;
                                    case AceIPCMessageInterface.USER_MSG:
                                        {
                                            if (client_conn != null) {
                                                if (client_conn.resetReceiveTiming() == true) {
                                                    processUserMessage((AceIPCUserMessage) parser.getMessage(), ((AceDatagramMessage) message).getAddress(), ((AceDatagramMessage) message).getPort());
                                                } else {
                                                    dropConnection(client_conn, true);
                                                }
                                            }
                                        }
                                        break;
                                    default:
                                        {
                                            System.err.println(getName() + ": AceIPCServer.processEvent() -- Unexpected message type received : " + parser.getMessageType() + ", msg follows: " + '\n' + AceIPCMessage.dumpRawBytes(((AceDatagramMessage) message).getBuffer(), 0, ((AceDatagramMessage) message).getLength()));
                                        }
                                        break;
                                }
                            }
                            break;
                    }
                } catch (AceException ex) {
                    System.err.println(getName() + ": AceIPCServer.processEvent() -- Error parsing message, AceException : " + ex.getMessage() + ", msg follows: " + '\n' + AceIPCMessage.dumpRawBytes(((AceDatagramMessage) message).getBuffer(), 0, ((AceDatagramMessage) message).getLength()));
                    return;
                }
            } else {
                System.err.println(getName() + ": AceIPCServer.processEvent() -- Error on datagram read, status = " + ((AceDatagramMessage) message).getStatus());
                getNewSocket();
            }
        } else {
            System.err.println(getName() + ": AceIPCServer.processEvent() -- Unexpected Ace message type encountered : " + message.messageType());
        }
    }

    private void processUserMessage(AceIPCUserMessage received_message, InetAddress addr, int port) {
        int to_thread_id = received_message.getToThreadID();
        AceIPCMessage msg_for_user = new AceIPCMessage(AceIPCMessage.MESSAGE_RECEIVED, (to_thread_id > 0 ? true : false), this, received_message.getFromThreadID(), received_message.getBytes(), received_message.userDataOffset(), received_message.userDataLength(), addr, port, userParm);
        if (to_thread_id > 0) {
            AceThread to_thread = AceThread.getAceThreadObject(to_thread_id);
            if (to_thread != null) {
                if (to_thread.sendMessage(msg_for_user) == false) {
                    System.err.println(getName() + ": AceIPCServer.processUserMessage() -- Could not send solicited IPC message to thread id = " + to_thread_id + " : " + getErrorMessage());
                }
            } else {
            }
        } else {
            if (unsolMsgHandler != null) {
                if (unsolMsgHandler.sendMessage(msg_for_user) == false) {
                    System.err.println(getName() + ": AceIPCServer.processUserMessage() -- Could not send IPC message to the unsolicited msg handler thread : " + getErrorMessage());
                }
            }
        }
    }

    private void removeClient(AceIPCServerConnection client_element) {
        boolean element_removed = false;
        synchronized (clientList) {
            int addr_int = addressToInt(client_element.getClientAddress());
            HashMap element = (HashMap) clientList.get(new Integer(addr_int));
            if (element != null) {
                AceIPCServerConnection subelement = (AceIPCServerConnection) element.get(new Integer(client_element.getClientPort()));
                if (subelement != null) {
                    element.remove(new Integer(client_element.getClientPort()));
                    element_removed = true;
                    if (element.isEmpty() == true) {
                        clientList.remove(new Integer(addr_int));
                    }
                }
            }
        }
        if (element_removed == true) if (disconnectHandler != null) {
            AceIPCMessage msg_for_user = new AceIPCMessage(AceIPCMessage.DISCONNECT, this, client_element.getClientAddress(), client_element.getClientPort(), userParm);
            if (disconnectHandler.sendMessage(msg_for_user) == false) {
                System.err.println(getName() + ": AceIPCServer.removeClient() -- Could not send IPC message to the user disconnect handler thread : " + getErrorMessage());
            }
        }
    }

    public void run() {
        sockListener.start();
        while (true) {
            AceMessageInterface message = waitMessage();
            if (message == null) {
                continue;
            } else if ((message instanceof AceSignalMessage) == true) {
                super.dispose();
                break;
            }
            processEvent(message);
        }
    }

    private boolean sendConnectResponseMessage(int status, InetAddress addr, int port) {
        return (sendMessage(new AceIPCConnRespMessage(status, hbInterval), addr, port));
    }

    private void sendDisconnectMessage(InetAddress addr, int port) {
        boolean status = sendMessage(new AceIPCDiscMessage(), addr, port);
    }

    protected boolean sendHeartbeatMessage(InetAddress addr, int port) {
        return sendMessage(new AceIPCHeartbeatMessage(), addr, port);
    }

    public boolean sendIPCMessage(byte[] message, int offset, int len, AceThread sender, InetAddress addr, int port) {
        return sendIPCMessage(message, offset, len, 0, sender, addr, port);
    }

    public boolean sendIPCMessage(byte[] message, int offset, int len, InetAddress addr, int port) {
        return sendIPCMessage(message, offset, len, 0, null, addr, port);
    }

    public boolean sendIPCMessage(byte[] message, int offset, int len, int to_thread_id, AceThread sender, InetAddress addr, int port) {
        Thread parent_thread = null;
        if (sender == null) {
            parent_thread = Thread.currentThread();
        } else {
            parent_thread = sender;
        }
        if ((parent_thread instanceof AceThread) == false) {
            writeErrorMessage("The calling thread must be an instance of AceThread");
            return false;
        }
        boolean retval = true;
        AceIPCServerConnection client_conn = findClient(addr, port);
        if (client_conn == null) {
            writeErrorMessage("The client is not currently connected");
            retval = false;
        } else if (client_conn.resetSendTiming() == false) {
            dropConnection(client_conn, true);
            writeErrorMessage("Fatal timing error encountered, connection dropped");
            retval = false;
        } else {
            AceIPCUserMessage ipc_msg = new AceIPCUserMessage(to_thread_id, ((AceThread) parent_thread).getAceThreadId(), message, offset, len);
            if (sendMessage(ipc_msg, addr, port) == false) {
                writeErrorMessage("Socket error sending message, connection dropped");
                retval = false;
            }
        }
        return retval;
    }

    public boolean sendIPCMessage(byte[] message, int offset, int len, int to_thread_id, InetAddress addr, int port) {
        return sendIPCMessage(message, offset, len, to_thread_id, null, addr, port);
    }

    private boolean sendMessage(AceIPCMessageInterface message, InetAddress addr, int port) {
        boolean retval = true;
        synchronized (socketLock) {
            if (socket == null) {
                return false;
            }
            DatagramPacket dp = new DatagramPacket(message.getBytes(), message.getLength(), addr, port);
            try {
                socket.send(dp);
            } catch (IOException ex) {
                System.err.println(getName() + ": AceIPCServer.sendMessage() -- IOException sending message on socket, error : " + ex.getMessage() + ", dest address = " + addr.toString() + ", dest port = " + port + ' ' + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + ", message follows: \n" + message.traceIPCMessage(true));
                retval = false;
            }
        }
        if (retval == false) {
        }
        return retval;
    }

    public AceMessageInterface waitIPCMessage() {
        Thread thr = Thread.currentThread();
        if ((thr instanceof AceThread) == false) {
            writeErrorMessage("This method is not being called from an object which is a sub-class of type AceThread");
            return null;
        }
        AceThread cthread = (AceThread) thr;
        while (true) {
            AceMessageInterface msg_received = cthread.waitMessage();
            if ((msg_received instanceof AceIPCMessage) == true) {
                if (((AceIPCMessage) msg_received).getEntity() == this) {
                    return msg_received;
                }
            } else if ((msg_received instanceof AceSignalMessage) == true) {
                return msg_received;
            }
        }
    }
}

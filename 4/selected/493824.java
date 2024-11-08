package com.quikj.server.framework;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

public class AceIPCClient extends AceThread implements AceIPCEntityInterface {

    private static final int CONNECT_SHORT_TIMER = 5 * 1000;

    private static final int CONNECT_LONG_TIMER = 15 * 1000;

    private static final int TRY_AGAIN_LATER_TIMER = 60 * 1000;

    private static final int MAX_INIT_FAILURES_IN_A_ROW = 5;

    private static final int STATE_CONNECTING = 1;

    private static final int STATE_CONNECTED = 2;

    private static final int STATE_DISCONNECTED = 3;

    private static final int STATE_WAITING_BEFORE_RETRY = 4;

    private DatagramSocket socket = null;

    private AceDatagram sockListener = null;

    private Object ipcLock = new Object();

    private InetAddress serverAddress;

    private int serverPort;

    private int hbInterval;

    private AceThread unsolMsgHandler = null;

    private AceThread connectHandler = null;

    private AceThread disconnectHandler = null;

    private byte[] registrationData = null;

    private int regDataOffset = 0;

    private int regDataLength = 0;

    private long userParm;

    private int state = STATE_DISCONNECTED;

    private int sendTimerId = -1;

    private int receiveTimerId = -1;

    public AceIPCClient(long user_parm, String name, String server_host, int server_port, AceThread unsol_msg_handler, AceThread connect_handler, AceThread disconnect_handler, byte[] registration_data, int reg_data_offset, int reg_data_len) throws IOException, AceException {
        super(name);
        try {
            serverAddress = InetAddress.getByName(server_host);
        } catch (UnknownHostException ex) {
            throw new AceException("Could not resolve inet address of host " + server_host + ", error " + ex.getMessage());
        }
        if (initNewSocket(user_parm) == false) {
            throw new AceException("Datagram socket initialization failed");
        }
        serverPort = server_port;
        unsolMsgHandler = unsol_msg_handler;
        connectHandler = connect_handler;
        disconnectHandler = disconnect_handler;
        registrationData = registration_data;
        regDataOffset = reg_data_offset;
        regDataLength = reg_data_len;
        userParm = user_parm;
    }

    public static void main(String[] args) {
        class ClientUser extends AceThread {

            class DataSender extends AceThread {

                private int sendInterval;

                private ClientUser parent;

                public DataSender(int send_data_interval, ClientUser parent) throws IOException {
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
                            parent.sendMessage(msg_data, 0, msg_data.length);
                        }
                    } catch (InterruptedException ex) {
                        System.err.println("DataSender sleep interrupted");
                    }
                }
            }

            private AceIPCClient ipcClient;

            private DataSender dataSender = null;

            public ClientUser(String name, int port, String server_hostname, int send_data_interval) throws IOException, AceException {
                super(name);
                byte[] reg_data = { 2, 4, 6, 8 };
                ipcClient = new AceIPCClient(1000, name, server_hostname, port, this, this, this, reg_data, 0, reg_data.length);
                if (send_data_interval > 0) {
                    dataSender = new DataSender(send_data_interval, this);
                }
                System.out.println(name + " THREAD ID = " + getAceThreadId());
            }

            public void run() {
                ipcClient.start();
                if (dataSender != null) {
                    dataSender.start();
                }
                while (true) {
                    AceMessageInterface message = ipcClient.waitIPCMessage();
                    if (message == null) {
                        System.err.println(getName() + " Null message encountered");
                        continue;
                    } else if ((message instanceof AceSignalMessage) == true) {
                        System.out.println(getName() + " Signal received, ID = " + ((AceSignalMessage) message).getSignalId() + ", signal message = " + ((AceSignalMessage) message).getMessage());
                        ipcClient.dispose();
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
                                    System.out.println(getName() + '_' + getAceThreadId() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " CONNECTION ESTABLISHED WITH SERVER ADDR = " + addr + ", PORT = " + port + ", userparm=" + msg.getUserParm());
                                }
                                break;
                            case AceIPCMessage.DISCONNECT:
                                {
                                    System.out.println(getName() + '_' + getAceThreadId() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " CONNECTION DISCONNECTED, SERVER ADDR = " + msg.getFarEndAddress() + ", PORT = " + msg.getFarEndPort() + ", userparm=" + msg.getUserParm());
                                }
                                break;
                            case AceIPCMessage.MESSAGE_RECEIVED:
                                {
                                    int msg_num = (int) AceInputSocketStream.octetsToIntMsbFirst(msg.getMessage(), msg.getUserDataOffset(), msg.getUserDataLength());
                                    System.out.println(getName() + '_' + getAceThreadId() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " RECEIVED " + ((msg.solicitedMessage() == true) ? "solicited " : "unsolicited ") + "senderThreadID=" + msg.getSenderThreadId() + " userparm=" + msg.getUserParm() + "    :  " + msg_num);
                                    if (dataSender == null) {
                                        byte[] reply = new byte[4];
                                        AceInputSocketStream.intToBytesMsbFirst(++msg_num, reply, 0);
                                        if (ipcClient.sendIPCMessage(reply, 0, reply.length, msg.getSenderThreadId(), this) == false) {
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

            public void sendMessage(byte[] msg_data, int offset, int length) {
                if (ipcClient.sendIPCMessage(msg_data, offset, length, this) == false) {
                    System.err.println(getName() + (new Date()) + ' ' + (new Date().getTime() & 0xFFFF) + " Message sending failed  : " + ((AceThread) (Thread.currentThread())).getErrorMessage());
                }
            }
        }
        try {
            int port = 3000;
            String hostname = "localhost";
            int send_data_interval = 0;
            if ((args.length != 0) && (args.length != 3)) {
                System.out.println("Arguments (all or nothing): <server port>, <server hostname>, <send user data interval(ms) - if 0, sends upon receipt>");
                System.out.println("Defaults: port=" + port + ", hostname=" + hostname + ", send user data interval=" + send_data_interval);
                System.exit(0);
            }
            if (args.length == 3) {
                try {
                    port = Integer.parseInt(args[0]);
                    hostname = args[1];
                    send_data_interval = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    System.err.println("Input must be numeric");
                    System.exit(1);
                }
            }
            AceTimer.Instance().start();
            ClientUser user = new ClientUser("TestClient", port, hostname, send_data_interval);
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

    public void dispose() {
        synchronized (ipcLock) {
            dropConnection();
            dropSocket();
        }
        if (this.isAlive() == true) {
            if (interruptWait(AceSignalMessage.SIGNAL_TERM, "Normal IPC Client dispose") == false) {
                System.err.println(getName() + ": AceIPCClient.dispose() -- Could not interrupt own wait : " + getErrorMessage());
                super.dispose();
            }
        } else {
            super.dispose();
        }
    }

    private void dropConnection() {
        dropConnection(true);
    }

    private void dropConnection(boolean send_disc_msg) {
        if (send_disc_msg == true) {
            switch(state) {
                case STATE_CONNECTED:
                case STATE_CONNECTING:
                    {
                        sendDisconnectMessage();
                    }
                    break;
                default:
                    break;
            }
        }
        state = STATE_DISCONNECTED;
        flushMessages();
        try {
            AceTimer.Instance().cancelAllTimers(this);
        } catch (IOException ex) {
            System.err.println(getName() + ": AceIPCClient.dropConnection() -- Error canceling timers : " + ex.getMessage());
            return;
        }
    }

    private void dropSocket() {
        if (sockListener != null) {
            sockListener.dispose();
            sockListener = null;
            socket = null;
        }
    }

    private boolean initConnection(int timer_value) {
        state = STATE_DISCONNECTED;
        for (int i = 0; i < MAX_INIT_FAILURES_IN_A_ROW; i++) {
            if (sendConnectRequestMessage() == true) {
                try {
                    sendTimerId = AceTimer.Instance().startTimer(timer_value, this, 0);
                    if (sendTimerId < 0) {
                        System.err.println(getName() + ": AceIPCClient.initConnection() -- Failure starting timer, returned ID = " + sendTimerId);
                        sendDisconnectMessage();
                        return false;
                    } else {
                        state = STATE_CONNECTING;
                        return true;
                    }
                } catch (IOException ex) {
                    System.err.println(getName() + ": AceIPCClient.initConnection() -- IOException starting timer : " + ex.getMessage());
                    sendDisconnectMessage();
                    return false;
                }
            } else {
                dropSocket();
                if (initNewSocket() == false) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean initNewSocket() {
        return initNewSocket(userParm);
    }

    private boolean initNewSocket(long user_parm) {
        try {
            socket = new DatagramSocket();
        } catch (SocketException ex) {
            System.err.println(getName() + ": AceIPCClient.initNewSocket() -- Socket error creating DatagramSocket : " + ex.getMessage());
            return false;
        }
        try {
            sockListener = new AceDatagram(user_parm, getName() + "_sockListener", this, socket, AceIPCMessage.MAX_IPC_MSG_SIZE);
        } catch (IOException ex) {
            System.err.println(getName() + ": AceIPCClient.initNewSocket() -- IO error creating AceDatagram : " + ex.getMessage());
            socket.close();
            socket = null;
            return false;
        } catch (AceException ex) {
            System.err.println(getName() + ": AceIPCClient.initNewSocket() -- Ace error creating AceDatagram : " + ex.getMessage());
            socket.close();
            socket = null;
            return false;
        }
        return true;
    }

    private void notifyUserOfDisc() {
        if (disconnectHandler != null) {
            AceIPCMessage msg_for_user = new AceIPCMessage(AceIPCMessage.DISCONNECT, this, serverAddress, serverPort, userParm);
            if (disconnectHandler.sendMessage(msg_for_user) == false) {
                System.err.println(getName() + ": AceIPCClient.notifyUserOfDisc() -- Could not send IPC message to the user disconnect handler thread : " + getErrorMessage());
            }
        }
    }

    private void processConnectedEvent(AceMessageInterface message) {
        if ((message instanceof AceDatagramMessage) == true) {
            if (((AceDatagramMessage) message).getStatus() == AceDatagramMessage.READ_COMPLETED) {
                stopTimer(receiveTimerId);
                try {
                    receiveTimerId = AceTimer.Instance().startTimer((hbInterval * AceIPCHeartbeatMessage.TOLERANCE_FACTOR), this, 0);
                    if (receiveTimerId < 0) {
                        System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Failure starting timer, returned ID = " + receiveTimerId);
                        notifyUserOfDisc();
                        dropConnection();
                        dropSocket();
                        return;
                    }
                } catch (IOException ex) {
                    System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- IOException starting timer : " + ex.getMessage());
                    notifyUserOfDisc();
                    dropConnection();
                    dropSocket();
                    return;
                }
                try {
                    AceIPCMessageParser parser = new AceIPCMessageParser(((AceDatagramMessage) message).getBuffer(), ((AceDatagramMessage) message).getLength());
                    switch(parser.getMessageType()) {
                        case AceIPCMessageInterface.HB_MSG:
                            {
                                if (resetSendTimer(true) == false) {
                                    notifyUserOfDisc();
                                    dropConnection();
                                    dropSocket();
                                } else {
                                    if (sendHeartbeatMessage() == false) {
                                        notifyUserOfDisc();
                                        reconnectWithNewSocket();
                                    }
                                }
                            }
                            break;
                        case AceIPCMessageInterface.DISCONNECT_MSG:
                            {
                                notifyUserOfDisc();
                                state = STATE_DISCONNECTED;
                                flushMessages();
                                try {
                                    AceTimer.Instance().cancelAllTimers(this);
                                } catch (IOException ex) {
                                    System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Error canceling timers : " + ex.getMessage());
                                } finally {
                                    if (initConnection(CONNECT_SHORT_TIMER) == false) {
                                        dropSocket();
                                    }
                                }
                            }
                            break;
                        case AceIPCMessageInterface.USER_MSG:
                            {
                                AceIPCUserMessage received_message = (AceIPCUserMessage) parser.getMessage();
                                int to_thread_id = received_message.getToThreadID();
                                AceIPCMessage msg_for_user = new AceIPCMessage(AceIPCMessage.MESSAGE_RECEIVED, (to_thread_id > 0 ? true : false), this, received_message.getFromThreadID(), received_message.getBytes(), received_message.userDataOffset(), received_message.userDataLength(), ((AceDatagramMessage) message).getAddress(), ((AceDatagramMessage) message).getPort(), userParm);
                                if (to_thread_id > 0) {
                                    AceThread to_thread = AceThread.getAceThreadObject(to_thread_id);
                                    if (to_thread != null) {
                                        if (to_thread.sendMessage(msg_for_user) == false) {
                                            System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Could not send solicited IPC message to thread id = " + to_thread_id + " : " + getErrorMessage());
                                        }
                                    } else {
                                    }
                                } else {
                                    if (unsolMsgHandler != null) {
                                        if (unsolMsgHandler.sendMessage(msg_for_user) == false) {
                                            System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Could not send IPC message to the unsolicited msg handler thread : " + getErrorMessage());
                                        }
                                    }
                                }
                            }
                            break;
                        default:
                            {
                                System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Unexpected message type received : " + parser.getMessageType() + ", msg follows: " + '\n' + AceIPCMessage.dumpRawBytes(((AceDatagramMessage) message).getBuffer(), 0, ((AceDatagramMessage) message).getLength()));
                                notifyUserOfDisc();
                                reconnect();
                            }
                            break;
                    }
                } catch (AceException ex) {
                    System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Error parsing message, AceException : " + ex.getMessage() + ", msg follows: " + '\n' + AceIPCMessage.dumpRawBytes(((AceDatagramMessage) message).getBuffer(), 0, ((AceDatagramMessage) message).getLength()));
                    return;
                }
            } else {
                notifyUserOfDisc();
                reconnectWithNewSocket();
            }
        } else if ((message instanceof AceTimerMessage) == true) {
            if (((AceTimerMessage) message).getTimerId() == sendTimerId) {
                if (resetSendTimer(false) == false) {
                    notifyUserOfDisc();
                    dropConnection();
                    dropSocket();
                } else {
                    if (sendHeartbeatMessage() == false) {
                        notifyUserOfDisc();
                        reconnectWithNewSocket();
                    }
                }
            } else if (((AceTimerMessage) message).getTimerId() == receiveTimerId) {
                System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Receive timer expired: LOST HEARTBEAT");
                notifyUserOfDisc();
                reconnectWithNewSocket();
            } else {
                System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Message received with unexpected timer ID = " + ((AceTimerMessage) message).getTimerId());
            }
        } else {
            System.err.println(getName() + ": AceIPCClient.processConnectedEvent() -- Unexpected Ace message type encountered : " + message.messageType());
        }
    }

    private void processConnectingEvent(AceMessageInterface message) {
        if ((message instanceof AceDatagramMessage) == true) {
            stopTimer(sendTimerId);
            if (((AceDatagramMessage) message).getStatus() == AceDatagramMessage.READ_COMPLETED) {
                try {
                    AceIPCMessageParser parser = new AceIPCMessageParser(((AceDatagramMessage) message).getBuffer(), ((AceDatagramMessage) message).getLength());
                    switch(parser.getMessageType()) {
                        case AceIPCMessageInterface.CONN_RESP_MSG:
                            {
                                processConnRespMessage((AceIPCConnRespMessage) parser.getMessage());
                            }
                            break;
                        default:
                            {
                                state = STATE_DISCONNECTED;
                                flushMessages();
                                if (initConnection(CONNECT_SHORT_TIMER) == false) {
                                    dropSocket();
                                }
                            }
                            break;
                    }
                } catch (AceException ex) {
                    System.err.println(getName() + ": AceIPCClient.processConnectingEvent() -- Error parsing message, AceException : " + ex.getMessage() + ", msg follows: " + '\n' + AceIPCMessage.dumpRawBytes(((AceDatagramMessage) message).getBuffer(), 0, ((AceDatagramMessage) message).getLength()));
                    return;
                }
            } else {
                reconnectWithNewSocket();
            }
        } else if ((message instanceof AceTimerMessage) == true) {
            if (initConnection(CONNECT_LONG_TIMER) == false) {
                dropSocket();
            }
        } else {
            System.err.println(getName() + ": AceIPCClient.processConnectingEvent() -- Unexpected Ace message type encountered : " + message.messageType());
        }
    }

    private void processConnRespMessage(AceIPCConnRespMessage conn_message) {
        switch(conn_message.getStatus()) {
            case AceIPCConnRespMessage.STATUS_OK:
                {
                    state = STATE_CONNECTED;
                    hbInterval = conn_message.getHbInterval();
                    try {
                        sendTimerId = AceTimer.Instance().startTimer(hbInterval, this, 0);
                        receiveTimerId = AceTimer.Instance().startTimer((hbInterval * AceIPCHeartbeatMessage.TOLERANCE_FACTOR), this, 0);
                        if ((sendTimerId < 0) || (receiveTimerId < 0)) {
                            System.err.println(getName() + ": AceIPCClient.processConnRespMessage() -- Failure starting one or more timers, returned IDs = " + sendTimerId + ", " + receiveTimerId);
                            dropConnection();
                            dropSocket();
                            return;
                        } else {
                            if (sendHeartbeatMessage() == false) {
                                reconnectWithNewSocket();
                            } else {
                                if (connectHandler != null) {
                                    AceIPCMessage msg_for_user = new AceIPCMessage(AceIPCMessage.CONNECTION_ESTABLISHED, this, serverAddress, serverPort, userParm);
                                    if (connectHandler.sendMessage(msg_for_user) == false) {
                                        System.err.println(getName() + ": AceIPCClient.processConnRespMessage() -- Could not send IPC message to the user connect handler thread : " + getErrorMessage());
                                    }
                                }
                            }
                        }
                    } catch (IOException ex) {
                        System.err.println(getName() + ": AceIPCClient.processConnRespMessage() -- IOException starting one or more timers : " + ex.getMessage());
                        dropConnection();
                        dropSocket();
                        return;
                    }
                }
                break;
            default:
                {
                    state = STATE_WAITING_BEFORE_RETRY;
                    try {
                        sendTimerId = AceTimer.Instance().startTimer(TRY_AGAIN_LATER_TIMER, this, 0);
                        if (sendTimerId < 0) {
                            System.err.println(getName() + ": AceIPCClient.processConnRespMessage() -- Failure starting timer, returned ID = " + sendTimerId);
                            dropConnection();
                            dropSocket();
                            return;
                        }
                    } catch (IOException ex) {
                        System.err.println(getName() + ": AceIPCClient.processConnRespMessage() -- IOException starting timer : " + ex.getMessage());
                        dropConnection();
                        dropSocket();
                        return;
                    }
                }
                break;
        }
    }

    private void processWaitingToRetryEvent(AceMessageInterface message) {
        if ((message instanceof AceTimerMessage) == true) {
            if (initConnection(CONNECT_SHORT_TIMER) == false) {
                dropSocket();
            }
        } else {
            System.err.println(getName() + ": AceIPCClient.processWaitingToRetryEvent() -- Unexpected Ace message type encountered : " + message.messageType());
        }
    }

    private void reconnect() {
        dropConnection(true);
        if (initConnection(CONNECT_SHORT_TIMER) == false) {
            dropSocket();
        }
    }

    private void reconnectWithNewSocket() {
        dropConnection(false);
        dropSocket();
        if (initNewSocket() == true) {
            if (initConnection(CONNECT_SHORT_TIMER) == false) {
                dropSocket();
            } else {
                sockListener.start();
            }
        }
    }

    private boolean resetSendTimer(boolean currently_running) {
        if (currently_running == true) {
            stopTimer(sendTimerId);
        }
        try {
            sendTimerId = AceTimer.Instance().startTimer(hbInterval, this, 0);
            if (sendTimerId < 0) {
                System.err.println(getName() + ": AceIPCClient.resetSendTimer() -- Failure starting timer, returned ID = " + sendTimerId);
                return false;
            }
        } catch (IOException ex) {
            System.err.println(getName() + ": AceIPCClient.resetSendTimer() -- IOException starting timer : " + ex.getMessage());
            return false;
        }
        return true;
    }

    public void run() {
        if (initConnection(CONNECT_SHORT_TIMER) == false) {
            dropSocket();
        } else {
            sockListener.start();
        }
        while (true) {
            AceMessageInterface message = waitMessage();
            if (message == null) {
                continue;
            } else if ((message instanceof AceSignalMessage) == true) {
                super.dispose();
                break;
            }
            synchronized (ipcLock) {
                switch(state) {
                    case STATE_CONNECTING:
                        {
                            processConnectingEvent(message);
                        }
                        break;
                    case STATE_CONNECTED:
                        {
                            processConnectedEvent(message);
                        }
                        break;
                    case STATE_WAITING_BEFORE_RETRY:
                        {
                            processWaitingToRetryEvent(message);
                        }
                        break;
                    case STATE_DISCONNECTED:
                        break;
                    default:
                        {
                            System.err.println(getName() + ": AceIPCClient.run() -- Bad state encountered : " + state);
                            reconnect();
                        }
                        break;
                }
            }
        }
    }

    private boolean sendConnectRequestMessage() {
        return (sendMessage(new AceIPCConnReqMessage(registrationData, regDataOffset, regDataLength)));
    }

    private void sendDisconnectMessage() {
        boolean status = sendMessage(new AceIPCDiscMessage());
    }

    private boolean sendHeartbeatMessage() {
        return (sendMessage(new AceIPCHeartbeatMessage()));
    }

    public boolean sendIPCMessage(byte[] message, int offset, int len) {
        return sendIPCMessage(message, offset, len, 0, null);
    }

    public boolean sendIPCMessage(byte[] message, int offset, int len, AceThread sender) {
        return sendIPCMessage(message, offset, len, 0, sender);
    }

    public boolean sendIPCMessage(byte[] message, int offset, int len, int to_thread_id) {
        return sendIPCMessage(message, offset, len, to_thread_id, null);
    }

    public boolean sendIPCMessage(byte[] message, int offset, int len, int to_thread_id, AceThread sender) {
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
        synchronized (ipcLock) {
            if (state != STATE_CONNECTED) {
                writeErrorMessage("The client is not currently connected");
                retval = false;
            } else if (resetSendTimer(true) == false) {
                dropConnection();
                dropSocket();
                writeErrorMessage("Fatal timing error encountered");
                retval = false;
            } else {
                AceIPCUserMessage ipc_msg = new AceIPCUserMessage(to_thread_id, ((AceThread) parent_thread).getAceThreadId(), message, offset, len);
                if (sendMessage(ipc_msg) == false) {
                    reconnectWithNewSocket();
                    writeErrorMessage("Socket error sending message, attempting reconnect");
                    retval = false;
                }
            }
        }
        return retval;
    }

    private boolean sendMessage(AceIPCMessageInterface message) {
        DatagramPacket dp = new DatagramPacket(message.getBytes(), message.getLength(), serverAddress, serverPort);
        try {
            socket.send(dp);
        } catch (IOException ex) {
            System.err.println(getName() + ": AceIPCClient.sendMessage() -- IOException sending message on socket, error : " + ex.getMessage() + ", dest address = " + serverAddress.toString() + ", dest port = " + serverPort + ", message follows: \n" + message.traceIPCMessage(true));
            return false;
        }
        return true;
    }

    private void stopTimer(int timer_id) {
        try {
            boolean status = AceTimer.Instance().cancelTimer(timer_id, this);
        } catch (IOException ex) {
            System.err.println(getName() + ": AceIPCClient.stopTimer() -- IOException canceling timer ID = " + timer_id + " : " + ex.getMessage());
            return;
        }
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

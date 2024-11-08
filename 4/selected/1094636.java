package com.quikj.server.framework;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class AceDatagram extends AceThread implements AceCompareMessageInterface {

    private DatagramSocket socket = null;

    private int maxMsgSize;

    private long userParm;

    private AceThread parent;

    private boolean quit = false;

    public AceDatagram(long user_parm, String name, AceThread cthread, DatagramSocket socket, int max_msg_size) throws IOException, AceException {
        super(name, true);
        Thread parent_thread;
        if (cthread == null) {
            parent_thread = Thread.currentThread();
        } else {
            parent_thread = cthread;
        }
        if ((parent_thread instanceof AceThread) == false) {
            throw new AceException("The thread supplied as a parameter is not an AceThread");
        }
        parent = (AceThread) parent_thread;
        this.socket = socket;
        maxMsgSize = max_msg_size;
        userParm = user_parm;
    }

    public AceDatagram(long user_parm, String name, DatagramSocket socket, int max_msg_size) throws IOException, AceException {
        this(user_parm, name, null, socket, max_msg_size);
    }

    public void dispose() {
        quit = true;
        if (socket != null) {
            socket.close();
            socket = null;
        }
        flushMessage();
        super.dispose();
    }

    public boolean flushMessage() {
        return parent.removeMessage(new AceDatagramMessage(this, 0, null, 0, null, 0, userParm), this);
    }

    public void run() {
        byte[] buffer = null;
        while (true) {
            try {
                buffer = new byte[maxMsgSize];
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                socket.receive(dp);
                AceDatagramMessage msg = new AceDatagramMessage(this, AceDatagramMessage.READ_COMPLETED, buffer, dp.getLength(), dp.getAddress(), dp.getPort(), userParm);
                if (parent.sendMessage(msg) == false) {
                    System.err.println(getName() + ":AceDatagram.run() -- Could not send read completed message : " + getErrorMessage());
                }
            } catch (IOException ex) {
                if (quit == true) {
                    return;
                } else {
                    System.err.println(getName() + ":AceDatagram.run() -- IO error occured : " + ex.getMessage());
                }
            }
        }
    }

    public boolean same(AceMessageInterface obj1, AceMessageInterface obj2) {
        boolean ret = false;
        if (((obj1 instanceof AceDatagramMessage) == true) && ((obj2 instanceof AceDatagramMessage) == true)) {
            if (((AceDatagramMessage) obj1).getAceDatagram() == ((AceDatagramMessage) obj2).getAceDatagram()) {
                ret = true;
            }
        }
        return ret;
    }

    public AceMessageInterface waitDatagramMessage() {
        Thread thr = Thread.currentThread();
        if ((thr instanceof AceThread) == false) {
            writeErrorMessage("This method is not being called from an object which is a sub-class of type AceThread");
            return null;
        }
        AceThread cthread = (AceThread) thr;
        while (true) {
            AceMessageInterface msg_received = cthread.waitMessage();
            if ((msg_received instanceof AceDatagramMessage) == true) {
                if (((AceDatagramMessage) msg_received).getAceDatagram() == this) {
                    return msg_received;
                }
            } else if ((msg_received instanceof AceSignalMessage) == true) {
                return msg_received;
            }
        }
    }
}

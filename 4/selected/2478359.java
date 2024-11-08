package juppDHT;

import ch.epfl.jupp.common.Action;
import ch.epfl.jupp.common.ConfValues;
import ch.epfl.jupp.l1.L1Listener;
import ch.epfl.jupp.l1.L1;
import ch.epfl.jupp.l2.L2;
import ch.epfl.jupp.l2.L2Listener;
import ch.epfl.jupp.l2.ringMgr.Neighbor;
import ch.epfl.jupp.l2.space.RoutingEntry;
import j2me.nio.ByteBuffer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;

public class Peer {

    private L1 l1;

    private L2 l2;

    private static String[] tokenize(String input, String separator) {
        Vector vector = new Vector();
        StringTokenizer strTokens = new StringTokenizer(input, separator);
        String[] strings;
        while (strTokens.hasMoreTokens()) vector.addElement(strTokens.nextToken());
        strings = new String[vector.size()];
        for (int i = 0; i < strings.length; i++) strings[i] = (String) vector.get(i);
        return strings;
    }

    private void setConfValues(String name, String value) {
        if (name.equals("JoinTimeout")) {
            ConfValues.JoinTimeout = new Integer(value).intValue();
        } else if (name.equals("RingRequestTimeout")) {
            ConfValues.RingRequestTimeout = new Integer(value).intValue();
        } else if (name.equals("RingMaxNeighbours")) {
            ConfValues.RingMaxNeighbours = new Integer(value).intValue();
        } else if (name.equals("SpaceMaxTableSize")) {
            ConfValues.SpaceMaxTableSize = new Integer(value).intValue();
        } else if (name.equals("SpaceRequestTimeout")) {
            ConfValues.SpaceRequestTimeout = new Integer(value).intValue();
        } else if (name.equals("SpaceDistMaxRetry")) {
            ConfValues.SpaceDistMaxRetry = new Integer(value).intValue();
        } else if (name.equals("SpaceDistRequestTimeout")) {
            ConfValues.SpaceDistRequestTimeout = new Integer(value).intValue();
        } else if (name.equals("RCMsgTimeout")) {
            ConfValues.RCMsgTimeout = new Integer(value).intValue();
        } else if (name.equals("RCMsgRetran")) {
            ConfValues.RCMsgRetran = new Integer(value).intValue();
        } else if (name.equals("ConnCleanDelay")) {
            ConfValues.ConnCleanDelay = new Integer(value).intValue();
        } else if (name.equals("L2MsgMaxHops")) {
            ConfValues.L2MsgMaxHops = new Integer(value).intValue();
        } else if (name.equals("NumSendThreads")) {
            ConfValues.NumSendThreads = new Integer(value).intValue();
        } else if (name.equals("NumReceiveThreads")) {
            ConfValues.NumReceiveThreads = new Integer(value).intValue();
        } else if (name.equals("receivingDelay")) {
            ConfValues.receivingDelay = new Integer(value).intValue();
        } else if (name.equals("maxPacketsOneRound")) {
            ConfValues.maxPacketsOneRound = new Integer(value).intValue();
        } else if (name.equals("L2Hop2HopACK")) {
            int val = new Integer(value).intValue();
            ConfValues.L2Hop2HopACK = val != 0;
        } else if (name.equals("L2RoutedMsgTimeout")) {
            ConfValues.L2RoutedMsgTimeout = new Integer(value).intValue();
        }
    }

    private void loadProperties(String filename) {
        try {
            File file = new File(filename);
            if (!file.exists()) return;
            BufferedReader in = new BufferedReader(new FileReader(file));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.trim().length() == 0) continue;
                if (inputLine.trim().startsWith("#")) continue;
                String[] tokens = tokenize(inputLine, "=");
                setConfValues(tokens[0], (tokens.length == 2 ? tokens[1] : ""));
            }
            in.close();
        } catch (IOException e) {
            System.err.println("Could not read/write property file '" + filename + "'! " + e);
        }
    }

    public Peer(Address localAddr, Address joinAddr, Id id, int port, String conf) {
        if (conf != null) loadProperties(conf);
        if (localAddr != null) l1 = new L1(port, localAddr.getHostName()); else try {
            l1 = new L1(port, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        l2 = new L2(l1, joinAddr, false);
        l2.setId(id);
    }

    public boolean join() {
        return l2.ringMgr.join();
    }

    public boolean join(Id id) {
        if (id != null) l2.setId(id);
        return l2.ringMgr.join();
    }

    public void shutDown() {
        l2.ringMgr.leave();
        l2.stop();
        l1.shutdown();
    }

    public Address getAddr() {
        return l1.getLocalAddr();
    }

    public Id getId() {
        return l2.getId();
    }

    public IdRange getRange() {
        return l2.getRange();
    }

    public void bind(Listener listener, short port) {
        new ListenerHelper(listener, port);
    }

    public void send(Id id, short port, ByteBuffer buffer, Action act) {
        l2.send(id, port, buffer, act);
    }

    public void noTcSend(Id id, short port, ByteBuffer buffer) {
        l2.noTcSend(id, port, new ByteBuffer[] { buffer });
    }

    public void send(Id from, Id to, short port, ByteBuffer buffer) {
        l2.send(new IdRange(from, to), port, buffer);
    }

    public void send(Address addr, short port, ByteBuffer buffer, Action act) {
        l2.send(addr, port, buffer, act);
    }

    public void blockingSend1(Address addr, short port, ByteBuffer buffer) throws IOException {
        ByteBuffer[] buffers = new ByteBuffer[1];
        buffers[0] = buffer;
        l1.send1(addr, port, buffers);
    }

    public void blockingSend(Address addr, short port, ByteBuffer buffer) throws IOException {
        ByteBuffer[] buffers = new ByteBuffer[1];
        buffers[0] = buffer;
        l1.send(addr, port, buffers);
    }

    public void broadcast(short port, ByteBuffer buffer) {
        Id id = new Id();
        send(id, id, port, buffer);
    }

    public void closeConnection(Address addr) {
        l1.close(addr);
    }

    public boolean isResponsible(Id id) {
        return l2.ringMgr.isResponsible(id);
    }

    public boolean isReady() {
        return l2.isOnline() && l2.space.isReady();
    }

    public Address getRandomRoutingEntry() {
        RoutingEntry entry = l2.getSpace().table.randomLookup();
        if (entry == null) return null;
        return entry.getAddr();
    }

    public Address getFirstLeftNeighbor() {
        Neighbor n = l2.ringMgr.getFirstLeft();
        if (n == null) return null;
        return n.addr;
    }

    public Address getFirstRightNeighbor() {
        Neighbor n = l2.ringMgr.getFirstRight();
        if (n == null) return null;
        return n.addr;
    }

    public String toString() {
        return l2.toString();
    }

    private class ListenerHelper implements L1Listener, L2Listener {

        Listener listener;

        ListenerHelper(Listener listener, short port) {
            this.listener = listener;
            l1.bind(this, port);
            l2.bind(this, port);
        }

        public void newConnection(Address addr) {
        }

        public void connectionClosed(Address addr) {
            listener.connectionClosed(addr);
        }

        public void receive(Address from, ByteBuffer buffer, Action sendAck) {
            if (sendAck != null) sendAck.run();
            listener.receive(buffer);
        }

        public void receive(ByteBuffer buffer) {
            listener.receive(buffer);
        }

        public void rangeChange() {
            listener.rangeChange();
        }

        public boolean isUse(Address addr) {
            return false;
        }

        public String toString() {
            return "ListenerHelper";
        }
    }
}

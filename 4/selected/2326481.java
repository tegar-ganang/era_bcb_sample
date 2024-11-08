package com.knowgate.jcifs.netbios;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.StringTokenizer;
import com.knowgate.debug.*;
import com.knowgate.misc.Gadgets;
import com.knowgate.jcifs.Config;

class NameServiceClient implements Runnable {

    static final int DEFAULT_SO_TIMEOUT = 5000;

    static final int DEFAULT_RCV_BUF_SIZE = 576;

    static final int DEFAULT_SND_BUF_SIZE = 576;

    static final int NAME_SERVICE_UDP_PORT = 137;

    static final int DEFAULT_RETRY_COUNT = 2;

    static final int DEFAULT_RETRY_TIMEOUT = 3000;

    static final int RESOLVER_LMHOSTS = 1;

    static final int RESOLVER_BCAST = 2;

    static final int RESOLVER_WINS = 3;

    private static final int SND_BUF_SIZE = Config.getInt("jcifs.netbios.snd_buf_size", DEFAULT_SND_BUF_SIZE);

    private static final int RCV_BUF_SIZE = Config.getInt("jcifs.netbios.rcv_buf_size", DEFAULT_RCV_BUF_SIZE);

    private static final int SO_TIMEOUT = Config.getInt("jcifs.netbios.soTimeout", DEFAULT_SO_TIMEOUT);

    private static final int RETRY_COUNT = Config.getInt("jcifs.netbios.retryCount", DEFAULT_RETRY_COUNT);

    private static final int RETRY_TIMEOUT = Config.getInt("jcifs.netbios.retryTimeout", DEFAULT_RETRY_TIMEOUT);

    private static final int LPORT = Config.getInt("jcifs.netbios.lport", 0);

    private static final InetAddress LADDR = Config.getInetAddress("jcifs.netbios.laddr", null);

    private static final String RO = Config.getProperty("jcifs.resolveOrder");

    private final Object LOCK = new Object();

    private int lport, closeTimeout;

    private byte[] snd_buf, rcv_buf;

    private DatagramSocket socket;

    private DatagramPacket in, out;

    private HashMap responseTable = new HashMap();

    private Thread thread;

    private int nextNameTrnId = 0;

    private int[] resolveOrder;

    InetAddress laddr, baddr;

    NameServiceClient() {
        this(LPORT, LADDR);
    }

    NameServiceClient(int lport, InetAddress laddr) {
        this.lport = lport;
        this.laddr = laddr;
        try {
            baddr = Config.getInetAddress("jcifs.netbios.baddr", InetAddress.getByName("255.255.255.255"));
        } catch (UnknownHostException uhe) {
        }
        snd_buf = new byte[SND_BUF_SIZE];
        rcv_buf = new byte[RCV_BUF_SIZE];
        out = new DatagramPacket(snd_buf, SND_BUF_SIZE, baddr, NAME_SERVICE_UDP_PORT);
        in = new DatagramPacket(rcv_buf, RCV_BUF_SIZE);
        if (RO == null || RO.length() == 0) {
            if (NbtAddress.getWINSAddress() == null) {
                resolveOrder = new int[2];
                resolveOrder[0] = RESOLVER_LMHOSTS;
                resolveOrder[1] = RESOLVER_BCAST;
            } else {
                resolveOrder = new int[3];
                resolveOrder[0] = RESOLVER_LMHOSTS;
                resolveOrder[1] = RESOLVER_WINS;
                resolveOrder[2] = RESOLVER_BCAST;
            }
        } else {
            int[] tmp = new int[3];
            StringTokenizer st = new StringTokenizer(RO, ",");
            int i = 0;
            while (st.hasMoreTokens()) {
                String s = st.nextToken().trim();
                if (s.equalsIgnoreCase("LMHOSTS")) {
                    tmp[i++] = RESOLVER_LMHOSTS;
                } else if (s.equalsIgnoreCase("WINS")) {
                    if (NbtAddress.getWINSAddress() == null) {
                        if (DebugFile.trace) {
                            DebugFile.writeln("NetBIOS resolveOrder specifies WINS however the " + "jcifs.netbios.wins property has not been set");
                        }
                        continue;
                    }
                    tmp[i++] = RESOLVER_WINS;
                } else if (s.equalsIgnoreCase("BCAST")) {
                    tmp[i++] = RESOLVER_BCAST;
                } else if (s.equalsIgnoreCase("DNS")) {
                    ;
                } else if (DebugFile.trace) {
                    DebugFile.writeln("unknown resolver method: " + s);
                }
            }
            resolveOrder = new int[i];
            System.arraycopy(tmp, 0, resolveOrder, 0, i);
        }
    }

    int getNextNameTrnId() {
        if ((++nextNameTrnId & 0xFFFF) == 0) {
            nextNameTrnId = 1;
        }
        return nextNameTrnId;
    }

    void ensureOpen(int timeout) throws IOException {
        closeTimeout = 0;
        if (SO_TIMEOUT != 0) {
            closeTimeout = Math.max(SO_TIMEOUT, timeout);
        }
        if (socket == null) {
            socket = new DatagramSocket(lport, laddr);
            thread = new Thread(this, "JCIFS-NameServiceClient");
            thread.setDaemon(true);
            thread.start();
        }
    }

    void tryClose() {
        synchronized (LOCK) {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            thread = null;
            responseTable.clear();
        }
    }

    public void run() {
        int nameTrnId;
        NameServicePacket response;
        while (thread == Thread.currentThread()) {
            in.setLength(RCV_BUF_SIZE);
            try {
                socket.setSoTimeout(closeTimeout);
                socket.receive(in);
            } catch (IOException ioe) {
                tryClose();
                break;
            }
            if (DebugFile.trace) DebugFile.writeln("NetBIOS: new data read from socket");
            nameTrnId = NameServicePacket.readNameTrnId(rcv_buf, 0);
            response = (NameServicePacket) responseTable.get(new Integer(nameTrnId));
            if (response == null || response.received) {
                continue;
            }
            synchronized (response) {
                response.readWireFormat(rcv_buf, 0);
                response.received = true;
                response.notify();
            }
        }
    }

    void send(NameServicePacket request, NameServicePacket response, int timeout) throws IOException {
        Integer nid = null;
        int count = 0;
        synchronized (response) {
            do {
                try {
                    synchronized (LOCK) {
                        request.nameTrnId = getNextNameTrnId();
                        nid = new Integer(request.nameTrnId);
                        out.setAddress(request.addr);
                        out.setLength(request.writeWireFormat(snd_buf, 0));
                        response.received = false;
                        responseTable.put(nid, response);
                        ensureOpen(timeout + 1000);
                        socket.send(out);
                    }
                    response.wait(timeout);
                } catch (InterruptedException ie) {
                } finally {
                    responseTable.remove(nid);
                }
                if (!response.received && NbtAddress.NBNS.length > 1 && NbtAddress.isWINS(request.addr)) {
                    request.addr = NbtAddress.switchWINS();
                    if (count == 0) {
                        count = NbtAddress.NBNS.length - 1;
                    }
                }
            } while (count-- > 0);
        }
    }

    NbtAddress getByName(Name name, InetAddress addr) throws UnknownHostException {
        int n;
        NameQueryRequest request = new NameQueryRequest(name);
        NameQueryResponse response = new NameQueryResponse();
        if (addr != null) {
            request.addr = addr;
            request.isBroadcast = (addr.getAddress()[3] == (byte) 0xFF);
            n = RETRY_COUNT;
            do {
                try {
                    send(request, response, RETRY_TIMEOUT);
                } catch (IOException ioe) {
                    if (DebugFile.trace) new ErrorHandler(ioe);
                    throw new UnknownHostException(name.name);
                }
                if (response.received && response.resultCode == 0) {
                    response.addrEntry.hostName.srcHashCode = addr.hashCode();
                    return response.addrEntry;
                }
            } while (--n > 0 && request.isBroadcast);
            throw new UnknownHostException(name.name);
        }
        for (int i = 0; i < resolveOrder.length; i++) {
            try {
                switch(resolveOrder[i]) {
                    case RESOLVER_LMHOSTS:
                        NbtAddress ans = Lmhosts.getByName(name);
                        if (ans != null) {
                            ans.hostName.srcHashCode = 0;
                            return ans;
                        }
                        break;
                    case RESOLVER_WINS:
                    case RESOLVER_BCAST:
                        if (resolveOrder[i] == RESOLVER_WINS && name.name != NbtAddress.MASTER_BROWSER_NAME && name.hexCode != 0x1d) {
                            request.addr = NbtAddress.getWINSAddress();
                            request.isBroadcast = false;
                        } else {
                            request.addr = baddr;
                            request.isBroadcast = true;
                        }
                        n = RETRY_COUNT;
                        while (n-- > 0) {
                            try {
                                send(request, response, RETRY_TIMEOUT);
                            } catch (IOException ioe) {
                                if (DebugFile.trace) new ErrorHandler(ioe);
                                throw new UnknownHostException(name.name);
                            }
                            if (response.received && response.resultCode == 0) {
                                response.addrEntry.hostName.srcHashCode = request.addr.hashCode();
                                return response.addrEntry;
                            } else if (resolveOrder[i] == RESOLVER_WINS) {
                                break;
                            }
                        }
                        break;
                }
            } catch (IOException ioe) {
            }
        }
        throw new UnknownHostException(name.name);
    }

    NbtAddress[] getNodeStatus(NbtAddress addr) throws UnknownHostException {
        int n, srcHashCode;
        NodeStatusRequest request;
        NodeStatusResponse response;
        response = new NodeStatusResponse(addr);
        request = new NodeStatusRequest(new Name(NbtAddress.ANY_HOSTS_NAME, 0x00, null));
        request.addr = addr.getInetAddress();
        n = RETRY_COUNT;
        while (n-- > 0) {
            try {
                send(request, response, RETRY_TIMEOUT);
            } catch (IOException ioe) {
                if (DebugFile.trace) new ErrorHandler(ioe);
                throw new UnknownHostException(addr.toString());
            }
            if (response.received && response.resultCode == 0) {
                srcHashCode = request.addr.hashCode();
                for (int i = 0; i < response.addressArray.length; i++) {
                    response.addressArray[i].hostName.srcHashCode = srcHashCode;
                }
                return response.addressArray;
            }
        }
        throw new UnknownHostException(addr.hostName.name);
    }
}

package org.piax.trans.ts.udpx;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.grlea.log.SimpleLogger;

/**
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
class ConnectionMgr implements SelectorHandler {

    private static final SimpleLogger log = new SimpleLogger(ConnectionMgr.class);

    public static final int MAX_PACKET_BUFFER_SIZE = 1500;

    final UdpXTransportService transService;

    private final ReceiveHandler receiveHandler;

    private final DatagramChannel channel;

    private final SelectDispatcher selector;

    private final ConcurrentMap<InetSocketAddress, UdpConnection> connections;

    private final SelectionKey key;

    private final ByteBuffer inBuf = ByteBuffer.allocate(MAX_PACKET_BUFFER_SIZE);

    volatile InetSocketAddress natAddr = null;

    private final Object mutexOfNATAddr = new Object();

    private final Object mutexOfReversal = new Object();

    /** secondary relay が有効になっているピアのセット */
    private final Set<UdpXLocator> secondaryRelayPeers = new HashSet<UdpXLocator>();

    /** アドレスからそのアドレスをrelayとして使用するpeerのセットへのMap */
    private final Map<InetSocketAddress, Set<UdpXLocator>> relay2PeersMap = new HashMap<InetSocketAddress, Set<UdpXLocator>>();

    /** UDP Hole Punching に失敗したリスト */
    private final Map<UdpXLocator, Object> blackList = new ConcurrentHashMap<UdpXLocator, Object>();

    /** 公表のNATアドレスの代わりに使用するNATアドレスの対応表 */
    private final Map<InetSocketAddress, InetSocketAddress> natMap = new ConcurrentHashMap<InetSocketAddress, InetSocketAddress>();

    ConnectionMgr(UdpXTransportService transService, int port) throws IOException {
        this.transService = transService;
        this.receiveHandler = transService;
        channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(port));
        channel.configureBlocking(false);
        connections = new ConcurrentHashMap<InetSocketAddress, UdpConnection>();
        selector = new SelectDispatcher();
        key = selector.register(this, SelectionKey.OP_READ);
        selector.start();
    }

    /**
     * remoteにより指定されたアドレスへのUDPコネクションを張る。
     * すでにコネクションが張られている場合はそれを取得する。
     * needsMaintainがtrueの場合は維持コネクションとして管理する。
     * 
     * @param remote 相手アドレス
     * @param needsMaintain 維持が必要な場合true 
     * @return UDPコネクション
     */
    synchronized UdpConnection getConnection(InetSocketAddress remote, boolean needsMaintain) {
        if (remote == null) {
            throw new IllegalArgumentException("remote should not be null");
        }
        UdpConnection conn = connections.get(remote);
        if (conn == null) {
            conn = new UdpConnection(this, remote, needsMaintain);
            connections.put(remote, conn);
        } else {
            if (needsMaintain) conn.setMaintain();
        }
        return conn;
    }

    /**
     * targetにより指定されたpeerのrelayへのUDPコネクションを張る。
     * relayが有効でない場合は、nullが返る。
     * 
     * @param target 相手ピア
     * @param needsMaintain 維持が必要な場合true 
     * @return UDPコネクション
     */
    synchronized UdpConnection getConnection(UdpXLocator target, boolean needsMaintain) {
        if (target == null) {
            throw new IllegalArgumentException("target should not be null");
        }
        InetSocketAddress relay;
        if (!secondaryRelayPeers.contains(target)) {
            relay = target.global;
            Set<UdpXLocator> peers = relay2PeersMap.get(target.global);
            if (peers == null) {
                peers = new HashSet<UdpXLocator>();
                relay2PeersMap.put(target.global, peers);
            }
            peers.add(target);
        } else {
            if (target.global2 == null) return null;
            relay = target.global2;
        }
        return getConnection(relay, needsMaintain);
    }

    /**
     * 他のピアが維持しているUDPコネクションを自分が使えるUDPコネクションとして
     * 登録する。
     * 
     * @param remote 相手ピアのアドレス（nullチェック不要）
     * @return UDPコネクション
     */
    synchronized UdpConnection receiveConnection(InetSocketAddress remote) {
        log.debugObject("receiveConnection addr", remote);
        UdpConnection conn = connections.get(remote);
        if (conn == null) {
            conn = new UdpConnection(this, remote, false);
            connections.put(remote, conn);
        }
        return conn;
    }

    /**
     * dummyConnectionReversalのための一時的なUDPコネクションを取得する。
     * 
     * @param remote 相手アドレス
     * @return 一時的なUDPコネクション
     */
    UdpConnection getTmpConnection(InetSocketAddress remote) {
        if (remote == null) {
            throw new IllegalArgumentException("remote should not be null");
        }
        return new UdpConnection(this, remote, false);
    }

    synchronized void acceptDeath(InetSocketAddress remote) {
        connections.remove(remote);
        Set<UdpXLocator> peers = relay2PeersMap.get(remote);
        if (peers != null) {
            secondaryRelayPeers.addAll(peers);
        }
        transService.acceptDeath(remote);
    }

    InetSocketAddress getNATAddr(InetSocketAddress relay, long timeout) throws IOException {
        UdpConnection conn = getConnection(relay, true);
        conn.send(UdpPacket.newControlReq(UdpPacket.NAT_ADDR_REQ));
        synchronized (mutexOfNATAddr) {
            try {
                mutexOfNATAddr.wait(timeout);
            } catch (InterruptedException e) {
                acceptDeath(relay);
            }
        }
        if (natAddr == null) throw new InterruptedIOException();
        return natAddr;
    }

    void fin() {
        selector.unregister(key);
        selector.fin();
        try {
            channel.close();
        } catch (IOException e) {
            log.warnException(e);
        }
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    UdpXLocator getMe() {
        return (UdpXLocator) transService.getLocator();
    }

    private void dummyConnectionReversal(UdpXLocator target) {
        ByteBuffer req = UdpPacket.newUdpPacketBuff(UdpPacket.CONN_REVERSAL_DUM, getMe(), target, (short) -1, (short) -1, new byte[0]);
        try {
            getTmpConnection(target.nat).send(req);
        } catch (IOException e) {
            log.debug("TRY NAT Traversal: PRE reversal expectably FAILED");
        }
    }

    private void tryConnectionReversal(UdpXLocator target, long timeout) throws IOException {
        ByteBuffer req = UdpPacket.newUdpPacketBuff(UdpPacket.CONN_REVERSAL_REQ, getMe(), target, (short) -1, (short) -1, new byte[0]);
        UdpConnection conn = getConnection(target, true);
        if (conn == null) {
            throw new IOException();
        }
        conn.send(req);
        synchronized (mutexOfReversal) {
            try {
                mutexOfReversal.wait(timeout);
            } catch (InterruptedException e) {
            }
        }
    }

    void send(UdpXLocator toPeer, ByteBuffer bbuf) throws IOException {
        UdpConnection conn;
        if (getMe().equals(toPeer)) {
            conn = getConnection(getMe().getLocalAddress(), false);
        } else if (getMe().isGlobal()) {
            InetSocketAddress nat;
            if (toPeer.isGlobal()) {
                conn = getConnection(toPeer.global, false);
            } else if (getMe().global.equals(toPeer.global)) {
                conn = getConnection(toPeer.nat, false);
            } else if ((nat = natMap.get(toPeer.nat)) != null) {
                conn = getConnection(nat, false);
            } else {
                if ((conn = connections.get(toPeer.nat)) == null) {
                    log.debug("TRY NAT Traversal: REQ connection reversal");
                    tryConnectionReversal(toPeer, UdpXTransportService.CONNECTION_REVERSAL_TIMEOUT);
                    conn = connections.get(toPeer.nat);
                    if (conn == null) {
                        log.debug("NAT Traversal: FAILURE!");
                    } else {
                        log.debug("NAT Traversal: SUCCESS!");
                    }
                }
                if (conn == null) {
                    conn = getConnection(toPeer, false);
                    if (conn == null) {
                        throw new IOException();
                    }
                }
            }
        } else if (getMe().sameSite(toPeer)) {
            conn = getConnection(toPeer.privateAddr, false);
        } else {
            if (toPeer.isGlobal()) {
                conn = getConnection(toPeer.global, true);
            } else {
                if (blackList.keySet().contains(toPeer)) {
                    conn = null;
                } else if ((conn = connections.get(toPeer.nat)) == null) {
                    log.debug("TRY NAT Traversal: PRE connection reversal");
                    dummyConnectionReversal(toPeer);
                    log.debug("TRY NAT Traversal: REQ connection reversal");
                    tryConnectionReversal(toPeer, UdpXTransportService.CONNECTION_REVERSAL_TIMEOUT);
                    conn = connections.get(toPeer.nat);
                    if (conn == null) {
                        log.debug("NAT Traversal: FAILURE!");
                        blackList.put(toPeer, null);
                    } else {
                        log.debug("NAT Traversal: SUCCESS!");
                    }
                }
                if (conn == null) {
                    conn = getConnection(toPeer, false);
                    if (conn == null) {
                        throw new IOException();
                    }
                }
            }
        }
        conn.send(bbuf);
    }

    /**
     * パケットheaderにある送信先のlocatorを見て、relayingを実行する。
     * <p>
     * 送信先locatorに間違いがある場合は、warning logを生成し、握りつぶす。
     * この場合は、<code>false</code> を返す。
     * <p>
     * NATの背後へのrelayingを行う必要があり、かつ、コネクションが張られていない
     * 場合は、<code>IOException</code> を返す。
     * 
     * @param toPeer 送信先ピアのlocator
     * @param bbuf 送信データ
     * @return relayに成功した場合 <code>true</code>
     * @throws IOException 通信エラーが発生した場合
     */
    private boolean relay(UdpXLocator toPeer, ByteBuffer bbuf) throws IOException {
        log.entry("relay()");
        UdpConnection conn = null;
        if (!getMe().isGlobal()) {
            log.warn("Illegal packet received. to addr:" + toPeer);
            return false;
        }
        if (!(getMe().global.equals(toPeer.global) || getMe().global.equals(toPeer.global2))) {
            conn = getConnection(toPeer, false);
            if (conn == null) {
                throw new IOException();
            }
            conn.send(bbuf);
            return true;
        }
        if (toPeer.nat == null) {
            log.warn("Illegal packet received. to addr:" + toPeer);
            return false;
        }
        conn = connections.get(toPeer.nat);
        if (conn == null) {
            throw new IOException("could not relay to NAT behind");
        }
        conn.send(bbuf);
        return true;
    }

    public void doReadOperation() {
        try {
            inBuf.clear();
            InetSocketAddress src = (InetSocketAddress) channel.receive(inBuf);
            inBuf.flip();
            if (src == null || inBuf.remaining() == 0) {
                log.debug("null read!");
                return;
            }
            UdpConnection conn = connections.get(src);
            if (conn == null) conn = receiveConnection(src);
            if (UdpPacket.isControl(inBuf) && conn != null) {
                if (UdpPacket.isType(inBuf, UdpPacket.NAT_ADDR_REQ)) {
                    conn.send(UdpPacket.newNATAddrAck(src));
                } else if (UdpPacket.isType(inBuf, UdpPacket.NAT_ADDR_ACK)) {
                    synchronized (mutexOfNATAddr) {
                        natAddr = UdpPacket.getNatAddr(inBuf);
                        mutexOfNATAddr.notifyAll();
                    }
                } else {
                    if (UdpPacket.isType(inBuf, UdpPacket.KEEPALIVE_REQ)) {
                        natAddr = UdpPacket.getNatAddr(inBuf);
                        if (natAddr != null) natMap.put(natAddr, src);
                    }
                    conn.receive(inBuf);
                }
                return;
            }
            UdpXLocator toPeer = UdpPacket.getDstLocator(inBuf);
            if (!getMe().equals(toPeer)) {
                log.debugObject("toPeer", toPeer);
                log.debugObject("me", getMe());
                relay(toPeer, inBuf);
                return;
            }
            if (UdpPacket.isType(inBuf, UdpPacket.CONN_REVERSAL_REQ)) {
                log.debugObject("toPeer", toPeer);
                log.debug("TRY NAT Traversal: DO connection reversal");
                UdpXLocator srcLoc = UdpPacket.getSrcLocator(inBuf);
                ByteBuffer ack = UdpPacket.newUdpPacketBuff(UdpPacket.CONN_REVERSAL_ACK, getMe(), srcLoc, (short) -1, (short) -1, new byte[0]);
                conn = getConnection(srcLoc.getGlobalAddress(), true);
                conn.send(ack);
            } else if (UdpPacket.isType(inBuf, UdpPacket.CONN_REVERSAL_ACK)) {
                log.debugObject("toPeer", toPeer);
                log.debug("TRY NAT Traversal: ACC connection reversal");
                synchronized (mutexOfReversal) {
                    mutexOfReversal.notifyAll();
                }
            } else if (UdpPacket.isType(inBuf, UdpPacket.CONN_REVERSAL_DUM)) {
                log.debugObject("toPeer", toPeer);
                log.debug("TRY NAT Traversal: ACC PRE connection reversal");
            } else {
                receiveHandler.receive(inBuf);
            }
        } catch (IOException e) {
            log.warnException(e);
        }
    }
}

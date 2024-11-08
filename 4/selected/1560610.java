package org.piax.trans.ts.udpx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.TimerTask;
import org.grlea.log.SimpleLogger;

/**
 * <p>
 * ここで行うことは、コネクション自体の管理に限定される。
 * コネクション保持のためのkeep aliveのやり取りや通信エラーを判別するための
 * タイムアウト時間の調整を行う。
 * 
 * @author     Mikio Yoshida
 * @version    2.1.0
 */
class UdpConnection implements ReceiveHandler {

    private static final SimpleLogger log = new SimpleLogger(UdpConnection.class);

    public static int NO_ACK_THRESHOLD = 1;

    public static final int KEEPALIVE_PERIOD = 20 * 1000;

    private class KeepAliver extends TimerTask {

        @Override
        public void run() {
            if (noAck >= NO_ACK_THRESHOLD) {
                mgr.acceptDeath(remote);
                return;
            }
            noAck++;
            try {
                send(UdpPacket.newKeepAliveReq(mgr.getMe().nat));
            } catch (IOException e) {
                log.warnException(e);
            }
        }
    }

    private final ConnectionMgr mgr;

    private final InetSocketAddress remote;

    private int noAck = 0;

    private boolean needsMaintain = false;

    /**
     * UDPコネクションを生成する。
     * needsMaintain を trueに指定した場合は、コネクション維持のため、
     * 定期的に keepAliveパケットを送信する。
     * 
     * @param mgr ConnectionMgrオブジェクト
     * @param remote 接続先
     * @param needsMaintain 維持が必要な場合true 
     */
    UdpConnection(ConnectionMgr mgr, InetSocketAddress remote, boolean needsMaintain) {
        this.mgr = mgr;
        this.remote = remote;
        if (needsMaintain) {
            this.needsMaintain = true;
            mgr.transService.schedule(new KeepAliver(), KEEPALIVE_PERIOD, KEEPALIVE_PERIOD);
        }
    }

    void setMaintain() {
        if (!needsMaintain) {
            needsMaintain = true;
            mgr.transService.schedule(new KeepAliver(), 0L, KEEPALIVE_PERIOD);
        }
    }

    void send(ByteBuffer bbuf) throws IOException {
        mgr.getChannel().send(bbuf, remote);
    }

    /**
     * ここでは、keep aliveに関係する受信処理だけを行う。
     */
    public void receive(ByteBuffer bbuf) {
        byte c = bbuf.get(bbuf.position());
        if (c == UdpPacket.KEEPALIVE_ACK) {
            noAck--;
            return;
        }
        if (c == UdpPacket.KEEPALIVE_REQ) {
            log.debug("keepAlive REQ received");
            try {
                ByteBuffer buf = ByteBuffer.allocate(1);
                buf.put(UdpPacket.KEEPALIVE_ACK);
                buf.flip();
                send(buf);
            } catch (IOException e) {
                log.warnException(e);
            }
            return;
        }
        log.error("Illegal control packet:" + bbuf.get(0));
    }
}

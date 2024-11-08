package net.sf.jqql.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import net.sf.jqql.QQ;
import net.sf.jqql.packets.ErrorPacket;
import net.sf.jqql.packets.PacketParseException;
import net.sf.jqql.packets._08InPacket;
import net.sf.jqql.packets._08OutPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <pre>
 * UDP Socks5 连接类
 * </pre>
 *
 * @author luma
 */
public class UDPSocks5Port extends AbstractPort implements IProxyHandler {

    /**
     * Log类
     */
    private static final Log log = LogFactory.getLog(UDPSocks5Port.class);

    /**
     * UDP channel
     */
    private DatagramChannel channel;

    /**
     * Socks5 代理类
     */
    private Socks5Proxy proxy;

    /**
     * 代理是否已经准备好
     */
    private boolean ready;

    /**
     * 构造函数
     *
     * @param policy        端口策略
     * @param serverAddress 服务器地址
     * @throws IOException 如果构造port失败
     */
    public UDPSocks5Port(IConnectionPolicy policy, InetSocketAddress serverAddress) throws IOException {
        super(policy);
        ready = false;
        this.remoteAddress = serverAddress;
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(0));
        proxy = new Socks5Proxy(this, policy.getProxyUsername(), policy.getProxyPassword(), channel);
        proxy.setProxyAddress(policy.getProxy());
        proxy.setRemoteAddress(serverAddress);
        proxy.setClientPort(channel.socket().getLocalPort());
    }

    public void dispose() {
        proxy.dispose();
    }

    public void start() {
        proxy.start();
    }

    public SelectableChannel channel() {
        return channel;
    }

    public void receive() throws IOException, PacketParseException {
        receiveBuf.clear();
        for (int len = channel.read(receiveBuf); len > 0; len = channel.read(receiveBuf)) {
            receiveBuf.flip();
            skipProxyHeader();
            _08InPacket packet = policy.parseIn(receiveBuf, false);
            if (packet == null) {
                receiveBuf.clear();
                continue;
            }
            policy.pushIn(packet);
            receiveBuf.clear();
        }
    }

    /**
     * 跳过代理头部
     */
    protected void skipProxyHeader() {
        int pos = receiveBuf.position();
        byte addressType = receiveBuf.get(3);
        if (addressType == Socks5Proxy.ATYP_DOMAIN_NAME) receiveBuf.position(pos + 6 + receiveBuf.get(4)); else if (addressType == Socks5Proxy.ATYP_IPV4) receiveBuf.position(pos + 10); else if (addressType == Socks5Proxy.ATYP_IPV6) receiveBuf.position(pos + 22); else log.error("代理头部包含不支持的地址类型");
    }

    /**
     * 添加代理包的头部，Socks5代理包的格式为
     * +----+------+------+----------+----------+----------+
     * |RSV | FRAG | ATYP | DST.ADDR | DST.PORT |   DATA   |
     * +----+------+------+----------+----------+----------+
     * | 2  |  1   |  1   | Variable |    2     | Variable |
     * +----+------+------+----------+----------+----------+
     */
    protected void fillProxyHeader() {
        sendBuf.putChar((char) 0).put((byte) 0).put(proxy.isIp ? Socks5Proxy.ATYP_IPV4 : Socks5Proxy.ATYP_DOMAIN_NAME).put(proxy.remoteAddress).putChar((char) proxy.remotePort);
    }

    public void send() throws IOException {
        while (!isEmpty()) {
            sendBuf.clear();
            fillProxyHeader();
            _08OutPacket packet = remove();
            packet.fill(sendBuf);
            sendBuf.flip();
            if (packet.needAck()) {
                channel.write(sendBuf);
                packet.setTimeout(System.currentTimeMillis() + QQ.QQ_TIMEOUT_SEND);
                policy.pushResend(packet, getId());
                log.debug("Sent - " + packet.toString());
            } else {
                int count = packet.getSendCount();
                for (int i = 0; i < count; i++) {
                    sendBuf.rewind();
                    channel.write(sendBuf);
                    log.debug("Sent - " + packet.toString());
                }
            }
        }
    }

    public void send(_08OutPacket packet) {
        try {
            sendBuf.clear();
            fillProxyHeader();
            packet.fill(sendBuf);
            sendBuf.flip();
            if (packet.needAck()) {
                channel.write(sendBuf);
                log.debug("Sent - " + packet.toString());
            } else {
                int count = packet.getSendCount();
                for (int i = 0; i < count; i++) {
                    sendBuf.rewind();
                    channel.write(sendBuf);
                    log.debug("Sent - " + packet.toString());
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void send(ByteBuffer buffer) {
        try {
            if (ready) channel.write(buffer);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public INIOHandler getNIOHandler() {
        if (ready) return this; else return proxy;
    }

    public boolean isConnected() {
        return true;
    }

    public void processConnect(SelectionKey sk) throws IOException {
    }

    public void processRead(SelectionKey sk) throws IOException, PacketParseException {
        receive();
    }

    public void processWrite() throws IOException {
        send();
    }

    public void proxyReady(InetSocketAddress bindAddress) throws IOException {
        ready = true;
        channel.connect(bindAddress);
        ((PortGate) getPool()).getPorter().register(this, SelectionKey.OP_READ);
    }

    public void proxyAuthFail() {
        proxyError("Proxy Auth Fail");
    }

    public void proxyError(String err) {
        ErrorPacket packet = policy.createErrorPacket(ErrorPacket.ERROR_PROXY, getId());
        packet.errorMessage = err;
        policy.pushIn(packet);
    }
}

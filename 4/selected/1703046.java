package com.peterhi.client;

import com.peterhi.net.Protocol;
import com.peterhi.net.Sender;
import com.peterhi.net.messages.Message;
import com.peterhi.net.packet.Manager;
import com.peterhi.net.packet.Packet;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;

/**
 *
 * @author YUN TAO
 */
public class DatagramSession implements Sender {

    private DatagramChannel channel;

    private SocketAddress remoteAddress;

    private DatagramClient client;

    public DatagramSession(DatagramClient client, SocketAddress localAddress, SocketAddress remoteAddress) throws IOException {
        if (client == null) throw new NullPointerException();
        if (localAddress == null) throw new NullPointerException();
        if (remoteAddress == null) throw new NullPointerException();
        this.client = client;
        this.remoteAddress = remoteAddress;
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(localAddress);
    }

    public DatagramClient getClient() {
        return client;
    }

    public DatagramChannel getChannel() {
        return channel;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(SocketAddress address) {
        remoteAddress = address;
    }

    public synchronized void close() throws IOException {
        channel.close();
        Manager.getInstance().removeRelevant(remoteAddress);
    }

    public synchronized void send(SocketAddress socketAddress, ByteBuffer buf) throws IOException {
        channel.send(buf, socketAddress);
    }

    public synchronized void send(Message message) throws IOException {
        List<Packet> list = message.packetize(this, remoteAddress, Protocol.BODY_BEGIN, Protocol.RUDP_BODY_SIZE);
        for (Packet item : list) {
            item.send();
        }
    }

    public synchronized void post(Message message) throws IOException {
        List<Packet> list = message.packetize(this, remoteAddress, Protocol.BODY_BEGIN, Protocol.RUDP_BODY_SIZE);
        Manager.getInstance().send(list);
    }
}

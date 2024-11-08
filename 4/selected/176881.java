package com.golden.gamedev.engine.network.tcp;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import com.golden.gamedev.engine.BaseClient;

/**
 * 
 * @author Paulus Tuerah
 */
public class TCPClient extends BaseClient {

    private static final int BUFFER_SIZE = 10240;

    private SocketChannel client;

    private Selector packetReader;

    private ByteBuffer readBuffer;

    private ByteBuffer writeBuffer;

    private DataInputStream input;

    private PipedOutputStream storage;

    private boolean waitingForLength = true;

    private int length;

    /** Creates a new instance of TCPClient */
    public TCPClient(String host, int port) throws IOException {
        this(new InetSocketAddress(host, port));
    }

    public TCPClient(SocketAddress host) throws IOException {
        this.client = SocketChannel.open();
        this.packetReader = Selector.open();
        this.client.connect(host);
        this.init();
    }

    protected TCPClient(TCPServer server, SocketChannel client, Selector packetReader) throws IOException {
        super(server);
        this.client = client;
        this.packetReader = packetReader;
        this.init();
    }

    private void init() throws IOException {
        this.storage = new PipedOutputStream();
        this.input = new DataInputStream(new PipedInputStream(this.storage, TCPClient.BUFFER_SIZE * 5));
        this.readBuffer = ByteBuffer.allocate(TCPClient.BUFFER_SIZE);
        this.writeBuffer = ByteBuffer.allocate(TCPClient.BUFFER_SIZE);
        this.readBuffer.order(ByteOrder.BIG_ENDIAN);
        this.writeBuffer.order(ByteOrder.BIG_ENDIAN);
        this.client.configureBlocking(false);
        this.client.socket().setTcpNoDelay(true);
        this.client.register(this.packetReader, SelectionKey.OP_READ, this);
    }

    public void update(long elapsedTime) throws IOException {
        super.update(elapsedTime);
        if (this.packetReader.selectNow() > 0) {
            Iterator packetIterator = this.packetReader.selectedKeys().iterator();
            while (packetIterator.hasNext()) {
                SelectionKey key = (SelectionKey) packetIterator.next();
                packetIterator.remove();
                this.read();
            }
        }
    }

    protected synchronized void read() throws IOException {
        this.readBuffer.clear();
        int bytesRead = this.client.read(this.readBuffer);
        if (bytesRead < 0) {
            throw new IOException("Reached end of stream");
        } else if (bytesRead == 0) {
            return;
        }
        this.storage.write(this.readBuffer.array(), 0, bytesRead);
        while (this.input.available() > 0) {
            if (this.waitingForLength) {
                if (this.input.available() > 2) {
                    this.length = this.input.readShort();
                    this.waitingForLength = false;
                } else {
                    break;
                }
            } else {
                if (this.input.available() >= this.length) {
                    byte[] data = new byte[this.length];
                    this.input.readFully(data);
                    this.addReceivedPacket(data);
                    this.waitingForLength = true;
                } else {
                    break;
                }
            }
        }
    }

    protected synchronized void sendPacket(byte[] data) throws IOException {
        this.writeBuffer.clear();
        this.writeBuffer.putShort((short) data.length);
        this.writeBuffer.put(data);
        this.writeBuffer.rewind();
        this.writeBuffer.limit(data.length + 2);
        this.client.write(this.writeBuffer);
        this.writeBuffer.limit(2048);
    }

    protected void disconnectImpl() throws IOException {
        this.client.close();
    }

    public boolean isConnected() {
        return this.client.isConnected();
    }

    public String getDetail() {
        return String.valueOf(this.client.socket().getLocalSocketAddress());
    }

    public String getRemoteDetail() {
        return this.client.socket().getRemoteSocketAddress().toString();
    }
}

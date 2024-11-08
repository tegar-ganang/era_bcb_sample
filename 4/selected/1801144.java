package ru.satseqsys.gate.server2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Set;
import ru.satseqsys.gate.CommandProcessor;
import ru.satseqsys.gate.reply.ReplyCommand;
import ru.satseqsys.gate.reply.ReplyQueue;
import ru.satseqsys.gate.util.StreamSplitter2;

public class TCPServer2 {

    private CommandProcessor<?> processor;

    private int port = 8000;

    private Selector selector = null;

    private ServerSocketChannel selectableChannel = null;

    int keysAdded = 0;

    public TCPServer2() throws IOException {
        initialize();
    }

    public TCPServer2(int port) throws IOException {
        this.port = port;
        initialize();
    }

    public void initialize() throws IOException {
        this.selector = SelectorProvider.provider().openSelector();
        this.selectableChannel = ServerSocketChannel.open();
        this.selectableChannel.configureBlocking(false);
        InetSocketAddress isa = new InetSocketAddress(InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }), this.port);
        this.selectableChannel.socket().bind(isa);
    }

    public void finalize() throws IOException {
        this.selectableChannel.close();
        this.selector.close();
    }

    public void start() throws Exception {
        SelectionKey acceptKey = this.selectableChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        System.out.println("Acceptor loop...");
        while ((keysAdded = acceptKey.selector().select()) > 0) {
            Set<SelectionKey> readyKeys = this.selector.selectedKeys();
            Iterator<SelectionKey> i = readyKeys.iterator();
            while (i.hasNext()) {
                SelectionKey key = (SelectionKey) i.next();
                i.remove();
                if (key.isAcceptable()) {
                    ServerSocketChannel nextReady = (ServerSocketChannel) key.channel();
                    SocketChannel channel = nextReady.accept();
                    channel.configureBlocking(false);
                    SelectionKey readKey = channel.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    readKey.attach(new ChannelCallback(channel));
                } else if (key.isReadable()) {
                    readMessage((ChannelCallback) key.attachment());
                } else if (key.isWritable()) {
                    ChannelCallback callback = (ChannelCallback) key.attachment();
                    byte[] somethingToWrite = getSomethingToWrite(callback);
                    if (somethingToWrite != null) {
                        ByteBuffer buf = ByteBuffer.wrap(somethingToWrite);
                        callback.getChannel().write(buf);
                    }
                }
            }
        }
        System.out.println("End acceptor loop...");
    }

    private byte[] getSomethingToWrite(ChannelCallback callback) {
        ReplyCommand replyCommand = ReplyQueue.takeFirstCommand(callback.getDeviceId());
        if (replyCommand == null) {
            return null;
        }
        return replyCommand.getText().getBytes();
    }

    public void writeMessage(SocketChannel channel, String message) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(message.getBytes());
        int nbytes = channel.write(buf);
        System.out.println("Wrote " + nbytes + " to channel.");
    }

    static final int BUFSIZE = 8;

    public void readMessage(ChannelCallback callback) throws Exception, InterruptedException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFSIZE);
        int num = callback.getChannel().read(byteBuffer);
        byteBuffer.flip();
        byte[] result = new byte[num];
        for (int i = 0; i < num; i++) {
            result[i] = byteBuffer.array()[i];
        }
        callback.append(result);
    }

    public class ChannelCallback {

        private SocketChannel channel;

        private Long deviceId;

        private StreamSplitter2 splitter = new StreamSplitter2(";") {

            @Override
            public void processToken(String token) throws Exception {
                deviceId = processor.processCommand(token);
            }
        };

        public ChannelCallback(SocketChannel channel) {
            this.channel = channel;
        }

        public SocketChannel getChannel() {
            return this.channel;
        }

        public void append(byte[] values) throws Exception {
            splitter.processArray(values);
        }

        public Long getDeviceId() {
            return deviceId;
        }
    }

    public void setProcessor(CommandProcessor<?> processor) {
        this.processor = processor;
    }
}

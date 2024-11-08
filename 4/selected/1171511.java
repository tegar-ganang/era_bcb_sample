package com.wuala.loader2.copied.rmi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import com.wuala.loader2.copied.Util;
import com.wuala.loader2.copied.rmi.v2.ProtocolV2;
import com.wuala.loader2.copied.rmi.v2.ReaderV2;
import com.wuala.loader2.copied.rmi.v2.StartEncryptionPing;
import com.wuala.loader2.copied.rmi.v2.WriterV2;
import com.wuala.loader2.crypto.SymmetricKey;

public class ParallelChannel {

    private Reader reader;

    private Writer writer;

    private Selector selector;

    private SelectionKey key;

    private SocketChannel channel;

    public static String CONNECTION_PASSWORD;

    public ParallelChannel(Reader reader, Writer writer, InetAddress address, int port) throws IOException {
        this.reader = reader;
        this.writer = writer;
        SocketAddress sa = new InetSocketAddress(address, port);
        channel = SocketChannel.open(sa);
        channel.socket().setReceiveBufferSize(channel.socket().getReceiveBufferSize() * 4);
        if (ProtocolV2.isEncryptedPort(port)) {
            channel.configureBlocking(true);
            SymmetricKey key = SymmetricKey.createNew();
            ((WriterV2) writer).initEncryption(key);
            ((ReaderV2) reader).initEncryption(key);
            StartEncryptionPing ping = new StartEncryptionPing(key, CONNECTION_PASSWORD);
            ByteBuffer header = ping.getHeader();
            ByteBuffer init = ping.getBytes();
            Util.writeFully(channel, header);
            Util.writeFully(channel, init);
        } else {
            ((WriterV2) writer).initEncryption(null);
            ((ReaderV2) reader).initEncryption(null);
        }
        channel.configureBlocking(false);
        selector = Selector.open();
        key = channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }

    public void doSomeWork() throws IOException {
        try {
            while (hasWork()) {
                checkOutput(key);
                checkInput(key);
                selector.select(3000);
            }
        } catch (CancelledKeyException e) {
            throw new IOException("Shutdown " + e.getMessage());
        }
    }

    public void wakeup() {
        selector.wakeup();
    }

    public boolean hasWork() {
        return reader.needMoreData() || writer.hasMoreData();
    }

    private void checkOutput(SelectionKey key) throws IOException {
        if (writer.hasMoreData() && key.isWritable()) {
            writer.sendData(channel);
        }
        int ops = key.interestOps();
        boolean hasWrite = (ops & SelectionKey.OP_WRITE) != 0;
        if (writer.hasMoreData() != hasWrite) {
            key.interestOps(ops ^ SelectionKey.OP_WRITE);
        }
    }

    private void checkInput(SelectionKey key) throws IOException {
        if (reader.needMoreData() && key.isReadable()) {
            reader.readData(channel);
        }
        int ops = key.interestOps();
        boolean hasRead = (ops & SelectionKey.OP_READ) != 0;
        if (reader.needMoreData() != hasRead) {
            key.interestOps(ops ^ SelectionKey.OP_READ);
        }
    }

    public void close() throws IOException {
        selector.close();
        channel.close();
    }
}

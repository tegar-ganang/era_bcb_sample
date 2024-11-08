package com.wuala.loader2.starter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class InstanceConnection {

    static final byte PROTOCOL_VERSION = 2;

    public static final byte[] PROBE = new byte[] { PROTOCOL_VERSION, 123, -17, 99, 21, 66, -3 };

    public static final byte[] ANSWER = new byte[] { PROTOCOL_VERSION, 41, -121, 7, 104, -55, -97 };

    private static final int CONNECT_PATIENCE = 3000;

    private static final int TCP_CONNECT_TIMEOUT = 100;

    private SocketChannel channel;

    public InstanceConnection(Configuration config) throws InterruptedException, IOException {
        int port = config.getInstancePort();
        if (port == 0) {
            throw new IOException();
        } else {
            channel = SocketChannel.open();
            channel.socket().connect(new InetSocketAddress("localhost", port), TCP_CONNECT_TIMEOUT);
            try {
                channel.configureBlocking(false);
                sendProbe(config);
                readConfirmation();
                channel.configureBlocking(true);
            } catch (IOException e) {
                channel.close();
                throw e;
            }
        }
    }

    private void sendProbe(Configuration config) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(PROBE.length + 8);
        buffer.put(PROBE);
        buffer.putLong(config.getInstanceCode());
        buffer.flip();
        writeFully(channel, buffer);
    }

    private void readConfirmation() throws IOException, InterruptedException {
        ByteBuffer buffer = ByteBuffer.allocate(ANSWER.length);
        int patience = CONNECT_PATIENCE;
        channel.read(buffer);
        while (buffer.hasRemaining()) {
            if (patience > 0) {
                Thread.sleep(50);
                patience -= 50;
            } else {
                throw new IOException();
            }
            int read = channel.read(buffer);
            if (read == -1) {
                throw new IOException();
            }
        }
        for (int i = 0; i < ANSWER.length; i++) {
            if (buffer.get(i) != ANSWER[i]) {
                throw new IOException();
            }
        }
    }

    public String sendCommand(String[] args) throws IOException {
        send(args == null ? new String[] {} : args);
        return receive();
    }

    public void close() throws IOException {
        channel.close();
    }

    private String receive() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(5);
        readFully(channel, header);
        header.flip();
        byte version = header.get();
        if (version == PROTOCOL_VERSION) {
            int answerLen = header.getInt();
            ByteBuffer answerBytes = ByteBuffer.allocate(answerLen);
            readFully(channel, answerBytes);
            answerBytes.flip();
            byte[] data = answerBytes.array();
            return new String(data);
        } else {
            return "Received invalid answer (" + version + ").";
        }
    }

    private void send(String[] args) throws IOException {
        int len = 1 + 4 + args.length * 4;
        for (String s : args) {
            len += s.length() * 4;
        }
        ByteBuffer buffer = ByteBuffer.allocate(len);
        buffer.put(PROTOCOL_VERSION);
        buffer.putInt(args.length);
        for (String s : args) {
            byte[] data = s.getBytes();
            buffer.putInt(data.length);
            buffer.put(data);
        }
        buffer.flip();
        writeFully(channel, buffer);
    }

    private void readFully(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer);
            if (read == -1) {
                throw new IOException();
            }
        }
    }

    private void writeFully(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int read = channel.write(buffer);
            if (read == -1) {
                throw new IOException();
            }
        }
    }
}

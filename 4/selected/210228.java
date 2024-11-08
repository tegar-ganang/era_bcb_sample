package com.wuala.loader2.starter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;
import com.wuala.loader2.service.IApplication;
import com.wuala.loader2.service.IApplicationInfo;

public class InstanceSocket extends Thread {

    private boolean running;

    private Configuration config;

    private IApplication instance;

    private ServerSocketChannel socket;

    private StatusMessage out, err;

    public InstanceSocket(IApplication instance, Configuration config) {
        super("Instance Socket");
        this.instance = instance;
        this.config = config;
        this.running = true;
        this.out = new StatusMessage(System.out, 100);
        System.setOut(this.out);
        this.err = new StatusMessage(System.err, 100);
        System.setErr(this.err);
        this.setDaemon(true);
        this.start();
    }

    private void initialize() throws IOException {
        this.socket = ServerSocketChannel.open();
        try {
            this.socket.configureBlocking(true);
            int port = config.getInstancePort();
            if (port == 0) {
                findPort(config, socket.socket());
            } else {
                socket.socket().bind(new InetSocketAddress("localhost", port));
            }
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    private void findPort(Configuration config, ServerSocket socket) throws IOException {
        Random rand = new Random();
        IOException ex = null;
        for (int i = 0; i < 20; i++) {
            try {
                int port = 52000 + rand.nextInt(10000);
                socket.bind(new InetSocketAddress("localhost", port));
                config.saveProperty(IApplicationInfo.INSTANCE_PORT, Integer.toString(port));
                config.saveProperty(IApplicationInfo.INSTANCE_CODE, Long.toString(rand.nextLong()));
                return;
            } catch (IOException e) {
                ex = e;
            }
        }
        throw ex;
    }

    @Override
    public void run() {
        try {
            while (running) {
                try {
                    initialize();
                    break;
                } catch (IOException e) {
                    System.err.println("Warning, could not start instance socket on port " + config.getInstancePort() + " due to " + e.getMessage() + ". Sending commands to this instance won't be possible. Trying again in 10s.");
                    Thread.sleep(10000);
                }
            }
            try {
                while (running && socket.isOpen()) {
                    SocketChannel channel = socket.accept();
                    try {
                        try {
                            channel.configureBlocking(true);
                            readProbe(channel);
                            writeConfirmation(channel);
                            while (true) {
                                String[] command = readCommand(channel);
                                try {
                                    String answer = handle(command);
                                    writeAnswer(channel, answer);
                                } catch (Throwable e) {
                                    writeAnswer(channel, e.toString() + "\n" + e.getStackTrace()[0].toString() + "\n" + e.getStackTrace()[1].toString());
                                }
                            }
                        } finally {
                            channel.close();
                        }
                    } catch (ClosedByInterruptException e) {
                        throw e;
                    } catch (IOException e) {
                    }
                }
            } finally {
                if (this.socket != null) {
                    this.socket.close();
                }
            }
        } catch (InterruptedException e1) {
        } catch (ClosedByInterruptException e) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String handle(String[] command) {
        if (command.length == 1 && command[0].equals(IApplicationInfo.CONSUME_OUT)) {
            String s1 = out.consume();
            if (s1 == null) {
                s1 = err.consume();
            }
            return s1 == null ? "" : s1;
        } else {
            return instance.execute(command);
        }
    }

    public void shutdown() {
        this.running = false;
        this.interrupt();
    }

    private String[] readCommand(SocketChannel channel) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(5);
        readFully(channel, header);
        header.flip();
        byte v = header.get();
        if (v == InstanceConnection.PROTOCOL_VERSION) {
            String[] args = new String[header.getInt()];
            for (int i = 0; i < args.length; i++) {
                ByteBuffer len = ByteBuffer.allocate(4);
                readFully(channel, len);
                len.flip();
                ByteBuffer data = ByteBuffer.allocate(len.getInt());
                readFully(channel, data);
                data.flip();
                args[i] = new String(data.array());
            }
            return args;
        } else {
            throw new IOException();
        }
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

    private void writeAnswer(SocketChannel channel, String answer) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(5 + answer.length() * 4);
        buffer.put(InstanceConnection.PROTOCOL_VERSION);
        buffer.putInt(answer.length());
        buffer.put(answer.getBytes());
        buffer.flip();
        writeFully(channel, buffer);
    }

    private void readProbe(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(InstanceConnection.PROBE.length + 8);
        readFully(channel, buffer);
        buffer.flip();
        for (int i = 0; i < InstanceConnection.PROBE.length; i++) {
            if (InstanceConnection.PROBE[i] != buffer.get()) {
                throw new IOException();
            }
        }
        if (buffer.getLong() != config.getInstanceCode()) {
            throw new IOException();
        }
    }

    private void writeConfirmation(SocketChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(InstanceConnection.ANSWER);
        writeFully(channel, buffer);
    }
}

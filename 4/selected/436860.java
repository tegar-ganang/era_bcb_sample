package uk.azdev.openfire.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

public abstract class AbstractServerEmulator implements Runnable {

    private ServerSocket socket;

    private Thread myThread;

    private boolean errorOccurred;

    private CountDownLatch latch;

    public AbstractServerEmulator() throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(true);
        channel.socket().bind(new InetSocketAddress(0));
        socket = channel.socket();
        errorOccurred = false;
        latch = new CountDownLatch(1);
    }

    public void start() {
        myThread = new Thread(this, "ServerEmulator");
        myThread.start();
    }

    public int getBoundPort() {
        return socket.getLocalPort();
    }

    public void stop() throws InterruptedException {
        latch.countDown();
        myThread.join();
    }

    public boolean didErrorOccur() {
        return errorOccurred;
    }

    public void run() {
        try {
            Socket acceptedConnection = socket.accept();
            SocketChannel channel = acceptedConnection.getChannel();
            channel.configureBlocking(true);
            doWork(channel);
            latch.await();
            acceptedConnection.close();
            socket.close();
        } catch (Exception e) {
            System.err.println("Exception occurred in server emulator");
            e.printStackTrace();
            errorOccurred = true;
        }
    }

    public abstract void doWork(SocketChannel channel) throws IOException;
}

package foo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SingleThreadedServer implements Runnable {

    private final int serverPort;

    private final Object syncLock = new Object();

    private Thread runningThread;

    private ServerSocket serverSocket;

    private boolean isStopped;

    public SingleThreadedServer(int aPort) {
        this.serverPort = aPort;
    }

    public void run() {
        synchronized (syncLock) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while (!this.stopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if (this.stopped()) {
                    System.out.println("Server Stopped");
                    return;
                }
                throw new RuntimeException("Unable to accept connection", e);
            }
            try {
                processClientRequest(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Server stopped");
    }

    private void processClientRequest(Socket clientSocket) throws IOException {
        InputStream input = clientSocket.getInputStream();
        OutputStream output = clientSocket.getOutputStream();
        long time = System.currentTimeMillis();
        output.write(("HTTP/1.1 200 OK\n\n<html><body>" + "Singlethreaded Server: " + time + "</body></html>").getBytes());
        output.close();
        input.close();
        System.out.println("Request processed:" + time);
    }

    private synchronized boolean stopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to close server socket", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
            System.out.println("Server listening to " + this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Unable to bind to port:" + this.serverPort, e);
        }
    }
}

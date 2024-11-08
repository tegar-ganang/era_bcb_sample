package net.pesahov.remote.socket;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * @author Pesahov Dmitry
 * @since 2.0
 */
public class PackageTest {

    /**
     * @param factory
     * @param address
     * @param request
     * @throws SocketException
     * @throws IOException
     */
    protected static void testClient(UnderlyingSocketFactory factory, SocketAddress address, String request) throws SocketException, IOException {
        Socket socket = new RemoteSocket(factory);
        socket.setKeepAlive(false);
        socket.connect(address);
        OutputStream out = socket.getOutputStream();
        out.write(request.getBytes());
        out.flush();
        int i = 0;
        int length;
        int arvLength = 0;
        int minLength = Integer.MAX_VALUE;
        int maxLength = Integer.MIN_VALUE;
        long ttw = 0L;
        long totalLength = 0L;
        boolean headerRead = false;
        byte[] buffer = new byte[1024 * 64];
        long readStartTime = System.currentTimeMillis();
        long totalReadStartTime = System.currentTimeMillis();
        InputStream input = new BufferedInputStream(socket.getInputStream());
        FileOutputStream output = new FileOutputStream("c:\\temp\\testFile.rar", false);
        while ((length = input.read(buffer)) != -1) {
            int offset = 0;
            if (!headerRead) {
                while (offset < length - 4 && (buffer[offset] != '\r' || buffer[offset + 1] != '\n' || buffer[offset + 2] != '\r' || buffer[offset + 3] != '\n')) offset++;
                headerRead = offset < length - 4;
                offset += 4;
            }
            output.write(buffer, offset, length - offset);
            if (i++ != 0) arvLength = (arvLength + length - offset) / 2; else arvLength = length;
            if (length < minLength) minLength = length - offset;
            if (length > maxLength) maxLength = length - offset;
            totalLength += length - offset;
            System.out.println("Read time (length=" + (length - offset) + "/" + totalLength + ", TTW=" + ttw + ") " + (System.currentTimeMillis() - readStartTime));
            if (length - offset < 512 * 24) ttw = 24; else ttw = (length - offset) / 512;
            try {
                Thread.sleep(ttw);
            } catch (InterruptedException e) {
            }
            readStartTime = System.currentTimeMillis();
        }
        long totalReadStopTime = System.currentTimeMillis();
        System.out.println("\r\nTotal read time (interations=" + i + ", avrLength=" + arvLength + ", minLength=" + minLength + ", maxLength=" + maxLength + ", totalLength=" + totalLength + "): " + (totalReadStopTime - totalReadStartTime));
        socket.close();
        output.close();
    }

    /**
     * @param factory
     * @param host
     * @param port
     * @param request
     * @throws SocketException
     * @throws IOException
     */
    protected static void testServer(UnderlyingSocketFactory factory, final String host, final int port, final String request) throws SocketException, IOException {
        RemoteServerSocket serverSocket = new RemoteServerSocket(factory, port);
        startServer(serverSocket);
        System.out.println("Server socket created: " + serverSocket);
        for (int i = 0; i < 3; i++) {
            Thread.yield();
            startClient(i + 1, host, port, request);
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        serverSocket.close();
        System.out.println("Server socket closed: " + serverSocket);
    }

    /**
     * @param clientId
     * @param host
     * @param port
     * @param request
     */
    private static void startClient(final int clientId, final String host, final int port, final String request) {
        Runnable runnable = new Runnable() {

            public void run() {
                try {
                    Socket socket = new Socket(host, port);
                    System.out.println("Client[" + clientId + "] created: " + socket);
                    OutputStream out = socket.getOutputStream();
                    out.write(request.getBytes());
                    out.write(0);
                    out.flush();
                    Thread.yield();
                    InputStream in = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    int data;
                    while ((data = reader.read()) > 0) System.out.println("Client[" + clientId + "]: " + ((char) data));
                    socket.close();
                    System.out.println("Client[" + clientId + "] closed: " + socket);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        new Thread(runnable, "Client[" + clientId + "]").start();
    }

    /**
     * @param serverSocket
     */
    private static void startServer(final ServerSocket serverSocket) {
        Runnable runnable = new Runnable() {

            public void run() {
                for (int i = 0; i < 3; i++) {
                    try {
                        acceptClient(i + 1, serverSocket.accept());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        new Thread(runnable, "Server").start();
    }

    /**
     * @param socket
     */
    private static void acceptClient(final int clientId, final Socket socket) {
        Runnable runnable = new Runnable() {

            public void run() {
                try {
                    System.out.println("AcceptClient[" + clientId + "] created: " + socket);
                    int data;
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    while ((data = in.read()) > 0) out.write(data);
                    out.flush();
                    socket.close();
                    System.out.println("AcceptClient[" + clientId + "] closed: " + socket);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        new Thread(runnable, "AcceptCleint[" + clientId + "]").start();
    }
}

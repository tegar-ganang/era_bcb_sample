package com.agentfactory.josf.communications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPSocketServer {

    private int my_backlog = 5;

    private ServerSocket my_serverSocket;

    public TCPSocketServer(int a_port) {
        try {
            my_serverSocket = new ServerSocket(a_port, my_backlog);
            System.out.println("TCP socket listening on port " + a_port);
        } catch (IOException ioe) {
            System.out.println("Cannot Start TCP Server");
        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }

    public String listen() {
        if (my_serverSocket != null) {
            while (true) {
                try {
                    Socket socket = my_serverSocket.accept();
                    System.out.println("Incomming Connection Request: " + socket.getInetAddress());
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    socket.getChannel();
                    String msg = in.readLine();
                    in.close();
                    socket.close();
                    return msg;
                } catch (IOException ioe) {
                    System.out.println("TCP Server is unable to listen...");
                } catch (SecurityException se) {
                    se.printStackTrace();
                }
            }
        } else {
            return null;
        }
    }

    public static void main(String[] args) {
        int port = 3000;
        TCPSocketServer server = new TCPSocketServer(port);
        while (true) {
            String get = server.listen();
            if (get != null) System.out.println(get);
        }
    }
}

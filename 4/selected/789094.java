package com.agentfactory.josf.communications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPSocketClient {

    public String my_serverHost;

    public int my_serverPort;

    public TCPSocketClient(String the_serverHost, int the_serverPort) {
        my_serverHost = the_serverHost;
        my_serverPort = the_serverPort;
    }

    public void sendMessageOneWay(String a_message) {
        try {
            Socket socket = new Socket(my_serverHost, my_serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(a_message + "\nEND");
            out.flush();
            out.close();
            socket.close();
        } catch (IOException ioe) {
        } catch (SecurityException se) {
            System.err.println("Security Exception");
        }
    }

    public String sendMessage(String a_message) {
        String response = "";
        try {
            Socket socket = new Socket(my_serverHost, my_serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(a_message + "\nEND");
            out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socket.getChannel();
            response = in.readLine();
            out.close();
            in.close();
            socket.close();
        } catch (IOException ioe) {
        } catch (SecurityException se) {
            System.err.println("Security Exception");
        }
        return response;
    }
}

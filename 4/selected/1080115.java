package javamail.conn;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

/**
 *
 * @author sriram
 */
public class tcpSockReadWrite {

    private boolean isServer = false;

    private Socket socket = null;

    public DataOutputStream sockOut = null;

    private BufferedReader sockIn = null;

    public tcpSockReadWrite(boolean server, Socket cSocket) {
        isServer = server;
        socket = cSocket;
        try {
            socket.setSoTimeout(100);
        } catch (SocketException e) {
            System.out.println("Unable to set read timeout!");
        }
    }

    public void openReadWrite() {
        try {
            if (socket != null) {
                if (sockOut == null) sockOut = new DataOutputStream(socket.getOutputStream());
                if (sockIn == null) sockIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } else {
                System.err.println("Socket not connected!");
                System.err.println("Socket must be open before read/write");
            }
        } catch (IOException e) {
            System.err.println("Could not open sockets for read/write");
        }
    }

    public boolean isServer() {
        return isServer;
    }

    public void closeAllSockets() {
        try {
            if (sockIn != null) sockIn.close();
            if (sockOut != null) sockOut.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.print("Error: Unable to close the sockets - ");
            System.err.print(e.getMessage());
        }
    }

    public String readLine() {
        String line = null;
        if (sockIn == null) {
            openReadWrite();
        }
        try {
            line = sockIn.readLine();
        } catch (IOException e) {
        }
        return line;
    }

    public String readChars() {
        char[] line = new char[2048];
        if (sockIn == null) {
            openReadWrite();
        }
        try {
            sockIn.read(line);
        } catch (IOException e) {
        }
        String result = new String(line);
        return result;
    }

    public void writeLine(String line) {
        if (sockOut == null) {
            openReadWrite();
        }
        try {
            sockOut.write(line.getBytes());
        } catch (IOException e) {
            System.err.println("Unable to read from socket!");
            System.err.println(e.getMessage());
        }
    }

    public void write(byte[] b, int off, int count) {
        if (sockOut == null) {
            openReadWrite();
        }
        try {
            sockOut.write(b, off, count);
        } catch (IOException e) {
            System.err.println("Unable to read from socket!");
            System.err.println(e.getMessage());
        }
    }
}

package lib;

import java.net.*;
import java.io.*;

public class test {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Syntax: testServer <port>");
        Socket client = accept(Integer.parseInt(args[0]));
        try {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            out.write("You are now connected to the test server.\r\n".getBytes("latin1"));
            int x;
            while ((x = in.read()) > -1) out.write(x);
        } finally {
            System.out.println("Closing");
            client.close();
        }
    }

    static Socket accept(int port) throws IOException {
        System.out.println("Starting on port " + port);
        ServerSocket server = new ServerSocket(port);
        System.out.println("Waiting");
        Socket client = server.accept();
        System.out.println("Accepted from " + client.getInetAddress());
        server.close();
        return client;
    }
}

package de.knup.jedi.jayshare.FileTransfer;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class FTPServer extends Server {

    private static final int MAX_QUEUE_LEN = 50;

    private static final long MAX_THREAD_DELAY = 3000;

    private Thread thread;

    private String exportRoot;

    public FTPServer(InetAddress addr, int port, String root) {
        super(addr, port);
        thread = null;
        exportRoot = root;
    }

    public void run() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port, MAX_QUEUE_LEN, addr);
            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    FTPRequestHandler request = new FTPRequestHandler(socket, exportRoot);
                    thread = new Thread(request);
                    thread.start();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}

class FTPRequestHandler implements Runnable {

    private static final String CRLF = "\r\n";

    private static final int ASCII = 0;

    private static final int BINARY = 1;

    private static final int FILE = 0;

    private static final int RECORD = 1;

    private Socket socket;

    private InputStream input;

    private OutputStream output;

    private BufferedReader br;

    private String exportRoot;

    private int port;

    private InetAddress addr;

    private InetAddress serverAddr;

    public FTPRequestHandler(Socket socket, String root) throws Exception {
        this.exportRoot = root;
        this.socket = socket;
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        serverAddr = socket.getLocalAddress();
        addr = null;
    }

    public void run() {
        try {
            processRequests();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void processRequests() throws Exception {
        int transType = ASCII;
        int structure = FILE;
        boolean running = true;
        output.write("220 Service ready".getBytes());
        output.write(CRLF.getBytes());
        while (running) {
            String requestLine = br.readLine();
            System.out.println(requestLine);
            StringTokenizer s = new StringTokenizer(requestLine);
            String temp = s.nextToken();
            boolean cmd_supported = false;
            String fileName;
            String serverLine = "Server: jayshare FTP server" + CRLF;
            String answer = null;
            FileInputStream fis = null;
            boolean fileExists = false;
            if (temp.equals("USER")) {
                cmd_supported = true;
                answer = "331 User name ok, user logged in";
            } else if (temp.equals("PASS")) {
                cmd_supported = true;
                answer = "230 User logged in";
            } else if (temp.equals("PORT")) {
                cmd_supported = true;
                int ip1, ip2, ip3, ip4;
                ip1 = Integer.valueOf(s.nextToken(", ")).intValue();
                ip2 = Integer.valueOf(s.nextToken()).intValue();
                ip3 = Integer.valueOf(s.nextToken()).intValue();
                ip4 = Integer.valueOf(s.nextToken()).intValue();
                port = 256 * Integer.valueOf(s.nextToken()).intValue();
                port += Integer.valueOf(s.nextToken()).intValue();
                addr = InetAddress.getByName(ip1 + "." + ip2 + "." + ip3 + "." + ip4);
                answer = "200 Command OK";
            } else if (temp.equals("TYPE")) {
                cmd_supported = true;
                String type = s.nextToken();
                if (type.equals("I")) {
                    transType = BINARY;
                    answer = "200 Command OK";
                } else if (type.equals("A")) {
                    transType = ASCII;
                    answer = "200 Command OK";
                } else answer = "504 Command not implemented for that parameter" + CRLF + "221- only (I)mage and (A)SCII are supported.";
            } else if (temp.equals("MODE")) {
                cmd_supported = true;
            } else if (temp.equals("STRU")) {
                cmd_supported = true;
                String stru = s.nextToken();
                if (stru.equals("F")) structure = FILE; else if (stru.equals("R")) structure = RECORD; else answer = "504 Command not implemented for that parameter" + CRLF + "221- only (F)ile and (R)ecord are supported.";
            } else if (temp.equals("RETR")) {
                cmd_supported = true;
            } else if (temp.equals("STOR")) {
                cmd_supported = true;
            } else if (temp.equals("NOOP")) {
                cmd_supported = true;
            } else if (temp.equals("PASV")) {
                cmd_supported = true;
                answer = "Entering Passive Mode (" + serverAddr.getHostAddress() + "," + 232 + "," + 222 + ")";
            } else if (temp.equals("QUIT")) {
                cmd_supported = true;
                running = false;
            }
            if (cmd_supported && answer != null) {
                output.write(answer.getBytes());
            } else output.write("502 Command not implemented".getBytes());
            output.write(CRLF.getBytes());
        }
        try {
            output.close();
            br.close();
            socket.close();
        } catch (Exception e) {
        }
    }

    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
        byte[] buffer = new byte[1024];
        int bytes = 0;
        while ((bytes = fis.read(buffer)) >= 0) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        return "application/octet-stream";
    }
}

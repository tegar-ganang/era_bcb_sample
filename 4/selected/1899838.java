package org.paw.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

/**
 *
 * @author  LUELLJOC
 * @version 
 */
public class PawAdmin {

    PawServer pawServer;

    int port;

    String user = null;

    String pass = null;

    boolean adminStarted = false;

    Vector allSockets;

    ServerSocket adminSocket;

    int lastPortUsed;

    /** Creates new PawAdmin */
    public PawAdmin(PawServer pawServer, int port, String user, String pass) {
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.pawServer = pawServer;
        this.allSockets = new Vector();
        startAdmin();
    }

    public void startAdmin() {
        PrintStream out = System.out;
        try {
            adminSocket = new ServerSocket(port);
            out.println("PAW Admin Server started on port " + this.port);
            lastPortUsed = port;
            while (true) {
                Socket socket = adminSocket.accept();
                (new ClientThread(this, socket)).start();
                allSockets.add(socket);
            }
        } catch (BindException e) {
            out.println("Port " + port + " is already in use");
        } catch (IOException ie) {
            out.println(ie.getMessage());
        }
    }

    public void closeAllSockets() {
        for (Enumeration e = allSockets.elements(); e.hasMoreElements(); ) {
            try {
                Socket socket = (Socket) e.nextElement();
                socket.close();
            } catch (Exception ex) {
            }
        }
        allSockets = new Vector();
    }
}

class ClientThread extends Thread {

    private PawAdmin pawAdmin;

    private Socket socket;

    private int port;

    private PawServer pawServer;

    private String user = null, pass = null;

    public ClientThread(PawAdmin pawAdmin, Socket socket) {
        this.pawAdmin = pawAdmin;
        this.socket = socket;
        this.port = pawAdmin.port;
        this.pawServer = pawAdmin.pawServer;
        this.user = pawAdmin.user;
        this.pass = pawAdmin.pass;
    }

    public String readLine(InputStream in) throws IOException {
        int c;
        StringBuffer line = new StringBuffer();
        while ((c = in.read()) != -1) {
            if (c == '\r') {
            } else if (c == '\n') {
                return (line.toString());
            } else {
                line.append((char) c);
            }
        }
        return (null);
    }

    @Override
    public void run() {
        String serverVersion = PawServer.class.getPackage().getImplementationVersion();
        String msg = "220 PAW " + serverVersion + " Admin Server on port " + port;
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            out.write((msg + "\r\n").getBytes());
            String line;
            String userString = "", passString = "";
            line = readLine(in);
            if (line.startsWith("user") && line.indexOf(" ") != -1) userString = line.substring(5);
            line = readLine(in);
            if (line.startsWith("pass") && line.indexOf(" ") != -1) passString = line.substring(5);
            if (!userString.equals(this.user) || !passString.equals(this.pass)) {
                out.write(("403 Login failed" + "\r\n").getBytes());
                socket.close();
                return;
            }
            out.write(("240 Login correct" + "\r\n").getBytes());
            while ((line = readLine(in)) != null) {
                if (line.equals("quit")) {
                    out.write(("221 PAW Admin Server closing" + " connection\r\n").getBytes());
                    break;
                } else if (line.startsWith("status")) {
                    outputServerStatus(out);
                    out.write(("226 End of Server Status" + "\r\n").getBytes());
                } else if (line.equals("server stop")) {
                    if (pawServer.serverStarted) {
                        pawServer.server.listen.close();
                        pawServer.serverStarted = false;
                        out.write(("200 Server has been stopped" + "\r\n").getBytes());
                    } else out.write(("400 Server already stopped" + "\r\n").getBytes());
                } else if (line.equals("server start")) {
                    if (!pawServer.serverStarted) {
                        pawServer.startServer();
                        pawServer.serverStarted = true;
                        out.write(("200 Server has been started" + "\r\n").getBytes());
                    } else out.write(("400 Server already running" + "\r\n").getBytes());
                } else if (line.equals("admin restart")) {
                    out.write(("200 Admin server will be restarted" + "\r\n").getBytes());
                    pawAdmin.user = pawServer.adminUser;
                    pawAdmin.pass = pawServer.adminPass;
                    pawAdmin.port = Integer.decode(pawServer.adminPort).intValue();
                    boolean restartNecessary = pawAdmin.lastPortUsed != pawAdmin.port ? true : false;
                    if (restartNecessary || !pawServer.adminActive) {
                        pawAdmin.closeAllSockets();
                        pawAdmin.adminSocket.close();
                    }
                    if (pawServer.adminActive && restartNecessary) pawAdmin.startAdmin();
                    break;
                } else if (line.equals("init")) {
                    pawServer.init();
                    out.write(("200 Init sent to server" + "\r\n").getBytes());
                } else if (line.equals("shutdown")) {
                    out.write(("200 Shutting down server" + "\r\n").getBytes());
                    System.exit(0);
                } else if (line.startsWith("getconf") && line.indexOf(" ") != -1) {
                    String filename = line.substring(8);
                    if (!readFile("conf", filename, out)) out.write(("400 Error while accessing File " + filename + "\r\n").getBytes()); else out.write(("226 Output of file " + filename + " completed" + "\r\n").getBytes());
                } else if (line.startsWith("getlog") && line.indexOf(" ") != -1) {
                    String filename = line.substring(7);
                    if (!readFile("logs", filename, out)) out.write(("400 Error while accessing LogFile " + filename + "\r\n").getBytes()); else out.write(("226 Output of logfile " + filename + " completed" + "\r\n").getBytes());
                } else if (line.startsWith("put") && line.indexOf(" ") != -1) {
                    String filename = line.substring(4);
                    out.write(("200 Singel . in a new line means EOF" + "\r\n").getBytes());
                    if (!putFile(filename, in)) out.write(("400 Error while writing File " + filename + "\r\n").getBytes()); else out.write(("226 File " + filename + " has been written" + "\r\n").getBytes());
                } else if (line.startsWith("clearlog")) {
                    pawServer.redirectStdout();
                    out.write(("226 Logfile has been cleared\r\n").getBytes());
                } else if (line.equals("getBrazilConfig")) {
                    Enumeration e = pawServer.config.keys();
                    while (e.hasMoreElements()) {
                        String key = (String) e.nextElement();
                        String value = (String) pawServer.config.get(key);
                        out.write((key + "=" + value + "\n").getBytes());
                    }
                    out.write(("226 End of Brazil config file\r\n").getBytes());
                } else out.write(("500 Command unrecognized: " + "\"" + line + "\"" + "\r\n").getBytes());
            }
            socket.close();
        } catch (IOException e) {
        } catch (NullPointerException ne) {
        }
    }

    public boolean readFile(String dir, String filename, OutputStream out) {
        if (filename.indexOf("..") != -1) return false;
        File f = new File(dir + File.separatorChar + filename);
        if (f.canRead()) {
            try {
                BufferedReader is = new BufferedReader(new FileReader(f));
                String s = null;
                while ((s = is.readLine()) != null) {
                    out.write((s + "\r\n").getBytes());
                }
                is.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean putFile(String filename, InputStream in) {
        boolean ret = false;
        if (filename.indexOf("..") != -1) return false;
        String newFileName = "conf" + File.separatorChar + filename;
        File newFile = new File(newFileName);
        boolean success = false;
        if (newFile.exists()) {
            int maxDelRetries = 10000;
            do {
                success = newFile.delete();
                maxDelRetries--;
            } while (!success && maxDelRetries > 0);
        } else {
            success = true;
        }
        ret = success;
        if (success) {
            File f = new File("conf" + File.separatorChar + filename + new Date().getTime());
            BufferedWriter os = null;
            try {
                os = new BufferedWriter(new FileWriter(f));
                String line;
                while ((line = readLine(in)) != null) {
                    if (line.equals(".")) break;
                    os.write(line + "\n");
                }
                os.close();
                ret = f.renameTo(new File(newFileName));
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                ret = false;
            } finally {
                try {
                    if (f.exists()) {
                        os.close();
                        f.delete();
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
        return ret;
    }

    private void outputServerStatus(OutputStream out) {
        try {
            if (pawServer.serverStarted) {
                String output = "Server is running!\n";
                output += "PAW Version: " + PawServer.class.getPackage().getImplementationVersion() + "\n";
                String hostName;
                try {
                    hostName = java.net.InetAddress.getLocalHost().getHostName();
                } catch (Exception e) {
                    hostName = "Unknown (No IP Address)";
                }
                output += "Host: " + hostName + "\n";
                output += "OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + "/" + System.getProperty("os.arch") + ")\n";
                output += "Java Version: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
                out.write((output + "\n").getBytes());
            } else {
                out.write(("Server not running!" + "\n").getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package org.vrspace.server;

import org.vrspace.util.*;
import org.vrspace.attributes.*;
import org.vrspace.server.*;
import org.vrspace.server.object.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author  rmeyers
 */
public class PortMapper extends VrmlFile implements Daemon, Runnable {

    public int _serverPort;

    public int[] _ports = null;

    ServerSocket _serverSocket = null;

    static boolean debugging = false;

    static boolean runtests = false;

    boolean singleMode = false;

    static {
        ownerRole.add("_serverPort");
        ownerRole.add("_ports");
    }

    /** Creates a new instance of PortMapper */
    public PortMapper() {
    }

    public PortMapper(int serverPort, int[] ports) {
        _serverPort = serverPort;
        _ports = ports;
    }

    public void initialized() {
    }

    public void startup(Dispatcher d) {
        start();
    }

    public void start() {
        try {
            _serverSocket = new ServerSocket();
            _serverSocket.bind(new InetSocketAddress(_serverPort));
            new Thread(this).start();
        } catch (Exception e) {
            System.out.println("Error during initialization: " + e.toString());
        }
    }

    public void shutdown() {
    }

    public void run() {
        try {
            debug("Server awaiting connection");
            while (_serverSocket != null) {
                Socket socket = _serverSocket.accept();
                debug("Accepted Connection");
                map(socket);
            }
        } catch (Exception e) {
            System.out.println("Server Error: " + e.toString());
        } finally {
            try {
                _serverSocket.close();
            } catch (Exception e) {
            }
        }
    }

    public void map(Socket socket) {
        OutputStream out = null, targetOut = null;
        InputStream in = null, targetIn = null;
        try {
            out = socket.getOutputStream();
            in = socket.getInputStream();
        } catch (IOException e) {
            System.out.println("Couldn't get I/O for the connection.");
            socket = null;
        }
        Socket targetSocket = null;
        try {
            if (socket != null) {
                if (!singleMode) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String line = "";
                    int read;
                    while ((read = br.read()) != -1) {
                        if (read != 10) {
                            line = line + (char) read;
                        } else {
                            break;
                        }
                    }
                    String[] components = line.split(" ");
                    int port = Integer.valueOf(components[2]).intValue();
                    debug("Parsed port: " + port);
                    boolean foundPort = false;
                    for (int i = 0; i < _ports.length; i++) {
                        if (_ports[i] == port) {
                            foundPort = true;
                        }
                    }
                    if (foundPort) {
                        targetSocket = new Socket("localhost", port);
                    }
                } else {
                    targetSocket = new Socket("localhost", _ports[0]);
                }
                targetOut = targetSocket.getOutputStream();
                targetIn = targetSocket.getInputStream();
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.toString());
        }
        if (targetSocket != null) {
            new Joint(targetIn, out).start();
            Thread.yield();
            new Joint(in, targetOut).start();
            debug("Connection Created");
        }
        Thread.yield();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int serverPort = -1;
        try {
            serverPort = Integer.valueOf(args[0]).intValue();
        } catch (Exception e) {
        }
        String[] portsArray = args[1].split(",");
        int[] ports = new int[portsArray.length];
        PortMapper.debug("Using ports: ", false);
        for (int i = 0; i < portsArray.length; i++) {
            try {
                ports[i] = Integer.valueOf(portsArray[i]).intValue();
                PortMapper.debug(ports[i] + " ", false);
            } catch (Exception e) {
            }
        }
        PortMapper.debug("");
        PortMapper mapper = new PortMapper(serverPort, ports);
        mapper.start();
        if (runtests) {
            mapper.test(serverPort, 8500);
        }
    }

    public static void debug(String message, boolean crlf) {
        if (debugging) {
            if (crlf) {
                System.out.println(message);
            } else {
                System.out.print(message);
            }
        }
    }

    public static void debug(String message) {
        debug(message, true);
    }

    public class Joint implements Runnable {

        BufferedInputStream _in = null;

        OutputStream _out = null;

        public Joint(InputStream in, OutputStream out) {
            _in = new BufferedInputStream(in);
            _out = new BufferedOutputStream(out);
        }

        public void start() {
            new Thread(this).start();
        }

        public void run() {
            int numRead = 0;
            byte[] data = new byte[1024];
            try {
                int read;
                while ((numRead = _in.read(data)) != -1) {
                    _out.write(data, 0, numRead);
                    if (debugging) {
                        debug("Block Transfer: ");
                        for (int i = 0; i < numRead; i++) {
                            read = data[i];
                            if (read != 10 && read != 13) {
                                debug(new Character((char) (byte) read) + "", false);
                            } else if (read == 13) {
                                debug("13_ ");
                            } else if (read == 13) {
                                debug("10_ ", false);
                            }
                        }
                    }
                    _out.flush();
                    if (_in.available() == 0) {
                        Thread.yield();
                    }
                }
            } catch (Exception e) {
                debug("Joint Exception: " + e.toString());
            }
            debug("Finished Joint Connection.");
            try {
                _in.close();
                _out.close();
            } catch (Exception e) {
            }
        }
    }

    public void test(int serverPort, int port) {
        try {
            Socket socket = new PortMapperSocket("localhost", serverPort, port);
            InputStream in = socket.getInputStream();
            socket.close();
        } catch (Exception e) {
            System.out.println("Test Error: " + e.toString());
        }
    }

    public void testServer(final int port) {
        (new Thread() {

            public void run() {
                try {
                    System.out.println("Test Server Start");
                    ServerSocket s = new ServerSocket(port);
                    Socket socket = s.accept();
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    System.out.println("Test Server Connection Accepted");
                    try {
                        int read;
                        while ((read = in.read()) != -1) {
                            System.out.println("TS: " + read);
                            out.write(read);
                            Thread.yield();
                        }
                    } catch (Exception e) {
                        System.out.println("Error: " + e.toString());
                    }
                    System.out.println("Test Server End");
                    s.close();
                } catch (Exception e) {
                    System.out.println("Test Server Error: " + e.toString());
                }
            }
        }).start();
    }
}

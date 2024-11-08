package iwork.multibrowse;

import java.io.*;
import java.net.*;

public class SingleFileWebServer implements Runnable {

    public static final int BUFFER_SIZE = 1024;

    public static final int DEFAULT_TIMEOUT = 10000;

    public static final int[] PORTS_TO_TRY = { 80, 29892, 29893, 29894, 29895, 29896, 29897, 29898, 29900, 29901, 29902, 29903, 29904, 29905, 29906, 29907, 29908, 29909, 29910 };

    protected File _file;

    protected ServerSocket _serverSocket;

    int _numConnections;

    protected int timeout;

    public SingleFileWebServer(File file, int numConnections) {
        this(file, numConnections, DEFAULT_TIMEOUT);
    }

    public SingleFileWebServer(File file, int numConnections, int timeout) {
        this.timeout = timeout;
        this._numConnections = numConnections;
        _file = file;
        boolean success = false;
        for (int i = 0; i < PORTS_TO_TRY.length; i++) {
            if (success) break;
            try {
                _serverSocket = new ServerSocket(PORTS_TO_TRY[i]);
                _serverSocket.setSoTimeout(timeout);
                success = true;
            } catch (BindException ex) {
                System.out.println("Could not bind on port " + PORTS_TO_TRY[i]);
            } catch (Exception ex) {
                ex.printStackTrace();
                _serverSocket = null;
            }
        }
    }

    public String getURLString() {
        if (_serverSocket != null) {
            try {
                String addr = InetAddress.getLocalHost().getHostAddress();
                int port = _serverSocket.getLocalPort();
                String fileName = _file.getName();
                return "http://" + addr + ":" + port + "/" + fileName;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    protected InputStream getStream() {
        try {
            return new FileInputStream(_file);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void run() {
        try {
            for (int i = _numConnections; i > 0 || _numConnections == -1; i--) {
                Socket socket = _serverSocket.accept();
                System.out.println("server: accepting connection");
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = in.readLine();
                while (line != null && line.length() > 0) {
                    System.out.println(line);
                    line = in.readLine();
                }
                System.out.println("server: read request");
                InputStream stream = getStream();
                String response = "HTTP/1.0 200 OK\r\n" + "Content-Type: " + "application/octet-stream\r\n" + "\r\n";
                OutputStream out = socket.getOutputStream();
                out.write(response.getBytes());
                System.out.println("server: sent response header");
                System.out.println(response);
                int bytes_read;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytes_read = stream.read(buffer)) > 0) {
                    out.write(buffer, 0, bytes_read);
                    System.out.println("server: wrote " + bytes_read + " bytes");
                }
                out.flush();
                stream.close();
                System.out.println("server: closing connection");
                socket.close();
            }
        } catch (SocketTimeoutException to_ex) {
            System.out.println("server timed out");
            try {
                System.out.println("server: exiting");
                _serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

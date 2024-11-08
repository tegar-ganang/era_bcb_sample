package funny_proxy;

import funny_proxy.addons.HostPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;

public class WorkingThread extends Thread {

    private RemoteInputThread remoteLocalThread;

    private InputStream inputStream;

    private OutputStream outputStream;

    private InputStream inputStream2;

    private OutputStream outputStream2;

    private Socket sock;

    private HostPort connect;

    private HostPort lastConnect = null;

    private Socket connection;

    private volatile boolean inLoop = true;

    public WorkingThread(Socket sock) {
        this.sock = sock;
        openStreams();
    }

    private void openStreams() {
        try {
            this.inputStream = sock.getInputStream();
            this.outputStream = sock.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readGetHTTP() {
        StringBuilder sb = new StringBuilder();
        try {
            int c;
            int len = 0;
            while ((c = inputStream.read()) != -1) {
                char x = (char) c;
                sb.append(x);
                len++;
                if (x == '\n') {
                    if (len > 3) {
                        if (sb.substring(len - 4, len).equals("\r\n\r\n")) {
                            break;
                        }
                    }
                }
            }
        } catch (SocketException e) {
            inLoop = false;
        } catch (IOException e) {
            inLoop = false;
        } catch (Exception e) {
            inLoop = false;
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void writeOutput(OutputStream outputStreamX, String readGet) throws IOException {
        writeOutput(outputStreamX, readGet.getBytes());
    }

    private void writeOutput(OutputStream outputStreamX, byte[] data) throws IOException {
        outputStreamX.write(data);
        outputStreamX.flush();
    }

    @Override
    public void run() {
        while (inLoop) {
            String readGet = readGetHTTP();
            if (readGet.isEmpty()) {
                inLoop = false;
                continue;
            }
            if (remoteLocalThread != null) {
                try {
                    remoteLocalThread.setMustEnd();
                    remoteLocalThread.interrupt();
                } catch (Exception e) {
                }
            }
            HostPort host = null;
            try {
                host = Utils.parseHost(readGet);
                if (host == null) {
                    continue;
                }
                setName(host.getHTTPString());
                if (Utils.isLocal(host.getHost())) {
                    URL url = Utils.parseURL(readGet);
                    readGet = Utils.replaceHost(readGet, url, host);
                    connect = host;
                } else {
                    connect = Runner.url;
                }
            } catch (NullPointerException e) {
                connect = Runner.url;
            }
            try {
                if (lastConnect != connect) {
                    endSock();
                    connection = new Socket(connect.getHost(), connect.getPort());
                    lastConnect = connect;
                    System.out.println("Connected to '" + host.getHTTPString() + "' via '" + connection.getInetAddress().getHostName() + ":" + connection.getPort() + "'");
                } else {
                    System.out.println("Reused connection: '" + host.getHTTPString() + "'");
                }
                System.out.flush();
                outputStream2 = connection.getOutputStream();
                inputStream2 = connection.getInputStream();
                remoteLocalThread = new RemoteInputThread(inputStream2, outputStream, this);
                remoteLocalThread.start();
                int size = Utils.getContentLength(readGet);
                writeOutput(outputStream2, readGet);
                if (size > 0) {
                    int b;
                    byte[] buffer = new byte[RemoteInputThread.BUFFER_SIZE];
                    while (size > 0) {
                        if (size < buffer.length) {
                            b = inputStream.read(buffer, 0, size);
                        } else {
                            b = inputStream.read(buffer);
                        }
                        if (b == -1) {
                            break;
                        }
                        outputStream2.write(buffer, 0, b);
                        outputStream2.flush();
                        size -= b;
                    }
                }
            } catch (ConnectException e) {
                System.out.println("Connection closed (" + host.getHTTPString() + ")");
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    this.finalize();
                } catch (Throwable ex) {
                }
            }
        }
        endThread();
    }

    @Override
    protected void finalize() throws Throwable {
        endThread();
    }

    private void endThread() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        endSock();
        try {
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            remoteLocalThread.interrupt();
        } catch (Exception e) {
        }
    }

    private void endSock() {
        try {
            try {
                inputStream2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
        }
    }

    public void closeLoop() {
        inLoop = false;
        interrupt();
        endThread();
    }
}

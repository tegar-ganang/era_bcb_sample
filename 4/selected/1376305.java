package soht.client.java.core;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.HttpURLConnection;
import java.net.SocketException;
import soht.client.java.configuration.ConfigurationManager;

/**
 * Handles the incoming and ougoing data when using
 * stateless connections.
 *
 * @author Eric Daugherty
 */
public class ProxyReadWrite extends BaseProxy {

    private static final long DEFAULT_SLEEP_TIME = 200;

    private static final long MAX_SLEEP_TIME = 2000;

    /** The input stream to read from the local client */
    private InputStream in;

    /** The output stream to write to the local client */
    private OutputStream out;

    public ProxyReadWrite(String name, ConfigurationManager configurationManager, long connectionId, Socket socket) throws IOException {
        super(name, configurationManager, connectionId, socket);
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public void run() {
        boolean isRunning = true;
        long sleepTime = DEFAULT_SLEEP_TIME;
        try {
            HttpURLConnection urlConnection;
            BufferedWriter out;
            byte[] inputBytes = new byte[1024];
            byte[] outputBytes = new byte[8192];
            int inputCount = 0;
            boolean inputShutdown = false;
            int outputCount = 0;
            while (isRunning) {
                try {
                    inputCount = 0;
                    int available = in.available();
                    if (available > 0) {
                        if (available >= inputBytes.length) inputCount = in.read(inputBytes); else inputCount = in.read(inputBytes, 0, available);
                    } else if (outputCount == 0) {
                        long sleepTill = System.currentTimeMillis() + sleepTime;
                        socket.setSoTimeout(50);
                        while ((sleepTill > System.currentTimeMillis()) && (inputCount == 0)) {
                            try {
                                inputCount = in.read(inputBytes);
                                if (inputCount < 0) {
                                    inputShutdown = true;
                                    break;
                                }
                            } catch (Exception e) {
                            }
                        }
                        if (inputCount == 0) {
                            sleepTime += 200;
                            if (sleepTime >= MAX_SLEEP_TIME) sleepTime = MAX_SLEEP_TIME;
                        } else sleepTime = DEFAULT_SLEEP_TIME;
                    }
                } catch (SocketException socketException) {
                    if (!"Socket closed".equals(socketException.getMessage())) {
                        System.out.println("Error reading data from server: " + socketException);
                    }
                    closeServer();
                    break;
                }
                if (inputShutdown) {
                    closeServer();
                    break;
                }
                urlConnection = configurationManager.getURLConnection();
                out = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
                out.write("action=readwrite");
                out.write("&");
                out.write("id=" + connectionId);
                out.write("&");
                out.write("datalength=" + inputCount);
                out.write("&");
                out.write("data=");
                out.write(encode(inputBytes, inputCount));
                out.flush();
                out.close();
                urlConnection.connect();
                InputStream serverInputStream = urlConnection.getInputStream();
                outputCount = 0;
                boolean isFirst = true;
                int startIndex = 1;
                while (true) {
                    int count = serverInputStream.read(outputBytes);
                    if (count == -1) {
                        break;
                    }
                    if (isFirst && count > 0 && outputBytes[0] == 0) {
                        isRunning = false;
                        break;
                    }
                    startIndex = isFirst ? 1 : 0;
                    try {
                        this.out.write(outputBytes, startIndex, count - startIndex);
                    } catch (IOException e) {
                        closeServer();
                    }
                    isFirst = false;
                    outputCount += (count - startIndex);
                }
            }
            socket.close();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }
}

package org.furthurnet.socket;

import java.net.*;
import java.io.*;
import java.util.Vector;
import org.furthurnet.datastructures.DataList;
import org.furthurnet.datastructures.Message;
import org.furthurnet.datastructures.ThrottleManager;
import org.furthurnet.datastructures.supporting.ClientMonitor;
import org.furthurnet.datastructures.supporting.Common;
import org.furthurnet.datastructures.supporting.Constants;
import org.furthurnet.datastructures.supporting.Handler;
import org.furthurnet.datastructures.supporting.SpeedCalculator;
import org.furthurnet.furi.ServiceManager;

public class SocketHandler implements Serializable {

    private DataInputStream in = null;

    private DataOutputStream out = null;

    private OutputStream socketOut = null;

    private Socket clientSocket = null;

    private ServerSocket server = null;

    private Handler hnd = null;

    private int chunksize = 1024;

    private byte[] buf = new byte[chunksize];

    private byte[] writeBuf = new byte[Constants.PACKET_SIZE];

    private ClientMonitor monitor;

    private int port = -1;

    boolean isConnected = false;

    private Vector connList = new Vector();

    public SocketHandler(Handler _hnd) {
        super();
        hnd = _hnd;
    }

    public SocketHandler(Handler _hnd, Socket cs) {
        hnd = _hnd;
        clientSocket = cs;
        openStreams();
    }

    public SocketHandler(Handler _hnd, SocketExtraction se) {
        hnd = _hnd;
        overrideSocketInfo(se);
    }

    public synchronized void setSocket(Socket cs) {
        clientSocket = cs;
        openStreams();
    }

    public synchronized void overrideSocketInfo(SocketExtraction se) {
        clientSocket = se.socket;
        in = se.in;
        out = se.out;
        isConnected = true;
    }

    public synchronized SocketExtraction getSocketExtraction() {
        SocketExtraction se = new SocketExtraction();
        se.socket = clientSocket;
        se.in = in;
        se.out = out;
        clientSocket = null;
        in = null;
        out = null;
        isConnected = false;
        return se;
    }

    public synchronized void connectLocal(String host, int targetPort) {
        connect(host, targetPort, -1);
    }

    public synchronized void connect(String host, int port, int targetPort) {
        LogMessage(5, "Connecting to " + host);
        close();
        if (host == null) return;
        try {
            clientSocket = new Socket(host, port);
            openStreams();
            if (targetPort >= 0) out.write((Integer.toString(targetPort) + "\n").getBytes());
            LogMessage(5, "Connection to " + host + "/" + port + " established on local port " + clientSocket.getLocalPort());
            return;
        } catch (UnknownHostException e) {
            LogMessage(1, "Unknown host:" + host);
            close();
        } catch (IOException e) {
            LogMessage(3, "Could not connect :" + host + " - " + e.getMessage());
            close();
        }
    }

    public synchronized boolean startListening(int _port) {
        LogMessage(3, "Listening on port " + _port);
        port = _port;
        return true;
    }

    public void checkForConnection(int timeout) {
        synchronized (hnd) {
            if (connList.size() == 0) hnd.Wait(timeout);
            if (connList.size() > 0) {
                SocketExtraction se = (SocketExtraction) connList.remove(0);
                overrideSocketInfo(se);
            }
        }
    }

    public SocketExtraction getConnection(int timeout) {
        if (hnd == null) return null;
        synchronized (hnd) {
            if (connList.size() == 0) hnd.Wait(timeout);
            if (connList.size() > 0) {
                SocketExtraction se = (SocketExtraction) connList.remove(0);
                return se;
            }
            return null;
        }
    }

    private synchronized void openStreams() {
        try {
            socketOut = clientSocket.getOutputStream();
            out = new DataOutputStream(socketOut);
            in = new DataInputStream(clientSocket.getInputStream());
            isConnected = true;
        } catch (IOException e) {
            close();
            isConnected = false;
            clientSocket = null;
        }
    }

    public boolean send(String text) {
        return send(text, null, 0, null);
    }

    public boolean send(String text, SpeedCalculator speedCalc) {
        return send(text, null, 0, speedCalc);
    }

    public boolean send(String text, DataInputStream input, int numBytes, SpeedCalculator speedCalc) {
        try {
            int maxUp = ServiceManager.getCfg().mMaxUpstream * 1000;
            LogMessage(5, "writing to socket.");
            ThrottleManager.throttle(text.length() * 8, maxUp);
            out.write(text.getBytes());
            if (speedCalc != null) speedCalc.addBytes(text.length());
            if (input == null) out.flush(); else {
                int count = 0;
                int totalPacketSize = numBytes;
                long checksum = 0;
                byte prev = 1;
                while (numBytes > 0) {
                    if (numBytes >= chunksize) {
                        input.readFully(buf);
                        for (int i = 0; i < buf.length; i++) {
                            checksum += buf[i] + (buf[i] - prev) * (buf[i] - prev);
                            prev = buf[i];
                        }
                        ThrottleManager.throttle(buf.length * 8, maxUp);
                        out.write(buf);
                        numBytes -= chunksize;
                        if (speedCalc != null) speedCalc.addBytes(buf.length);
                    } else {
                        input.readFully(buf, 0, numBytes);
                        for (int i = 0; i < numBytes; i++) {
                            checksum += buf[i] + (buf[i] - prev) * (buf[i] - prev);
                            prev = buf[i];
                        }
                        ThrottleManager.throttle(buf.length * 8, maxUp);
                        out.write(buf, 0, numBytes);
                        numBytes = 0;
                        if (speedCalc != null) speedCalc.addBytes(numBytes);
                    }
                }
                out.writeLong(checksum);
                out.flush();
                if (speedCalc != null) speedCalc.addBytes(8);
                int numBytesToSkip = Constants.PACKET_SIZE - totalPacketSize;
                while (numBytesToSkip > 0) numBytesToSkip -= input.skip(numBytesToSkip);
            }
            String logText = new String(text);
            while (logText.indexOf('\n') >= 0) logText = logText.substring(0, logText.indexOf('\n')) + "|" + logText.substring(logText.indexOf('\n') + 1);
            LogMessage(5, "Message sent to " + clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + ": " + logText);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private String recieve() {
        return recieve(1000);
    }

    private String recieve(int timeout) {
        try {
            clientSocket.setSoTimeout(timeout);
        } catch (Exception e) {
            LogMessage(4, "Could not set timeout.");
        }
        StringBuffer line = new StringBuffer();
        try {
            char ch = 0;
            int b = 0;
            do {
                b = in.read();
                if (b >= 0) {
                    ch = (char) b;
                    if (ch != '\n') line.append(ch);
                }
            } while ((ch != '\n') && (b >= 0));
        } catch (NullPointerException e) {
            LogMessage(4, "Null pointer exception on socket recieve.");
            isConnected = false;
            return null;
        } catch (InterruptedIOException e) {
            LogMessage(4, "Interrupted IO Exception.");
            isConnected = false;
            return null;
        } catch (IOException e) {
            LogMessage(4, "Standard IO Exception.");
            isConnected = false;
            return null;
        }
        return line.toString();
    }

    private DataList readRemainingBytes(String messageType) throws Exception {
        DataList data = new DataList();
        data.numBytes = Integer.parseInt(recieve(Constants.MESSAGE_WAIT_TIME));
        in.readFully(writeBuf, 0, data.numBytes);
        long checksum = 0;
        byte prev = 1;
        for (int i = 0; i < data.numBytes; i++) {
            checksum += writeBuf[i] + (writeBuf[i] - prev) * (writeBuf[i] - prev);
            prev = writeBuf[i];
        }
        if (checksum != in.readLong()) {
            hnd.LogMessage(3, "corrupt packet, retrying");
            return null;
        }
        data.data = writeBuf;
        return data;
    }

    public synchronized void setMonitor(ClientMonitor _monitor) {
        monitor = _monitor;
    }

    public void close() {
        isConnected = false;
        try {
            in.close();
            out.close();
            clientSocket.close();
        } catch (Exception e) {
        }
        clientSocket = null;
        hnd = null;
        monitor = null;
    }

    public void closeAll() {
        try {
            server.close();
        } catch (Exception e) {
        }
        try {
            send(Constants.OK_BYE + "\n" + "0" + "\n");
        } catch (Exception e) {
        }
        close();
    }

    public Message getMessage() {
        return getMessage(Constants.MESSAGE_WAIT_TIME);
    }

    public Message getMessage(int timeout) {
        Message temp = new Message();
        try {
            temp.messageType = recieve(timeout);
            if (temp.messageType == null) return null;
            if (temp.messageType.length() != 3) return null;
            temp.numParams = Integer.parseInt(recieve(Constants.MESSAGE_WAIT_TIME));
            temp.params = new String[temp.numParams];
            for (int i = 0; i < temp.numParams; i++) temp.params[i] = recieve(Constants.MESSAGE_WAIT_TIME);
            if (includesByteArray(temp.messageType)) temp.data = readRemainingBytes(temp.messageType);
        } catch (Exception e) {
            return null;
        }
        echoMessage(temp);
        return validateMessage(temp);
    }

    private void echoMessage(Message m) {
        String text = m.messageId + "|" + m.messageType + "|";
        for (int i = 0; i < m.numParams; i++) text += m.params[i] + "|";
        LogMessage(5, "Message recieved :" + text);
    }

    private Message validateMessage(Message m) {
        if (m.messageType == null) return null; else return m;
    }

    public boolean isConnected() {
        return isConnected;
    }

    private boolean includesByteArray(String messageType) {
        return ((Common.equalStrings(messageType, Constants.PACKET_HERE)) || (Common.equalStrings(messageType, Constants.PACKET_HERE_BYE)) || (Common.equalStrings(messageType, Constants.SET_CHILDREN_AND_PACKET_HERE)) || (Common.equalStrings(messageType, Constants.CONFIG_INITIAL)) || (Common.equalStrings(messageType, Constants.CONFIG_UPDATE)) || (Common.equalStrings(messageType, Constants.SET_FILE_DESC)) || (Common.equalStrings(messageType, Constants.SET_FILE_SET_DESC)));
    }

    private void LogMessage(int i, String s) {
        if (hnd != null) hnd.LogMessage(i, s);
    }

    public void forwardConnection(SocketExtraction s) {
        synchronized (hnd) {
            connList.add(s);
            hnd.notify();
        }
    }

    public synchronized String getHostAddress() {
        return clientSocket.getInetAddress().getHostAddress();
    }
}

package governor.ftp;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;

public final class FTPClient {

    private Socket socket;

    private PrintStream os;

    private DataInputStream is;

    private FTPCallback callback;

    static int PRELIM = 1;

    static int COMPLETE = 2;

    static int CONTINUE = 3;

    static int TRANSIENT = 4;

    static int ERROR = 5;

    public static int ASCII = 1;

    public static int BINARY = 2;

    public FTPClient(String s, FTPCallback c) throws UnknownHostException, IOException {
        callback = c;
        socket = new Socket(s, 21);
        os = new PrintStream(socket.getOutputStream());
        is = new DataInputStream(socket.getInputStream());
        GetReply(is);
    }

    public int GetReply(DataInputStream is) {
        return GetReply(is, true);
    }

    public int GetReply(DataInputStream is, boolean report) {
        String buffer;
        try {
            do {
                buffer = is.readLine();
                if (callback != null && report) callback.output(buffer);
            } while (!(Character.isDigit(buffer.charAt(0)) && Character.isDigit(buffer.charAt(1)) && Character.isDigit(buffer.charAt(2)) && buffer.charAt(3) == ' '));
        } catch (IOException e) {
            System.err.println("Error getting reply from controlport");
            return (0);
        }
        return (Integer.parseInt(buffer.substring(0, 1)));
    }

    public String GetReplyString(DataInputStream is) {
        String buffer;
        try {
            do {
                buffer = is.readLine();
                callback.output(buffer);
            } while (!(Character.isDigit(buffer.charAt(0)) && Character.isDigit(buffer.charAt(1)) && Character.isDigit(buffer.charAt(2)) && buffer.charAt(3) == ' '));
        } catch (IOException e) {
            System.err.println("Error getting reply from controlport");
            return ("");
        }
        return (buffer);
    }

    public boolean Command(String command) {
        return Command(command, true);
    }

    public boolean Command(String command, boolean report) {
        os.println(command);
        int result = GetReply(is, report);
        return (result == PRELIM || result == COMPLETE || result == CONTINUE);
    }

    public boolean Upload(String filename, String destination, int type, boolean process) {
        Socket dataSocket;
        String address, host = null;
        int port;
        os.println("pasv");
        address = GetReplyString(is);
        address = address.substring(address.indexOf('(') + 1, address.indexOf(')'));
        StringTokenizer t = new StringTokenizer(address, ",");
        for (int i = 0; i < 4; i++) {
            if (host == null) host = t.nextToken(); else host += "." + t.nextToken();
        }
        port = Integer.parseInt(t.nextToken()) << 8;
        port += Integer.parseInt(t.nextToken());
        try {
            dataSocket = new Socket(host, port);
        } catch (IOException e) {
            System.err.println("Could not connect to server, " + e);
            return (false);
        }
        if (type == ASCII) os.println("type a"); else os.println("type i");
        GetReply(is);
        os.println("stor " + destination);
        int result = GetReply(is);
        if (result == PRELIM) {
            try {
                OutputStream out = dataSocket.getOutputStream();
                byte buffer[] = new byte[1024];
                RandomAccessFile in = new RandomAccessFile(filename, "r");
                String line;
                int amount;
                if (type == ASCII && process) {
                    while ((line = in.readLine()) != null) {
                        line = callback.processData(line);
                        line.getBytes(0, line.length(), buffer, 0);
                        out.write(buffer, 0, line.length());
                        out.write('\n');
                    }
                } else {
                    while ((amount = in.read(buffer)) > 0) out.write(buffer, 0, amount);
                }
                in.close();
                out.close();
                dataSocket.close();
                result = GetReply(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return (result == COMPLETE);
        } else {
            return (false);
        }
    }

    public boolean Port(ServerSocket serverSocket) {
        int localport = serverSocket.getLocalPort();
        InetAddress inetaddress = serverSocket.getInetAddress();
        InetAddress localip;
        try {
            localip = inetaddress.getLocalHost();
        } catch (UnknownHostException e) {
            System.out.println("can't get local host");
            return (false);
        }
        byte[] addrbytes = localip.getAddress();
        short addrshorts[] = new short[4];
        for (int i = 0; i <= 3; i++) {
            addrshorts[i] = addrbytes[i];
            if (addrshorts[i] < 0) addrshorts[i] += 256;
        }
        os.println("port " + addrshorts[0] + "," + addrshorts[1] + "," + addrshorts[2] + "," + addrshorts[3] + "," + ((localport & 0xff00) >> 8) + "," + (localport & 0x00ff));
        int result = GetReply(is);
        return (result == COMPLETE);
    }
}

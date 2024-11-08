package bman.tools.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ByteStation {

    static Logger log = Logger.getLogger(ByteStation.class.getName());

    ByteReceiver receiver;

    int serverPort = 917;

    Server socketListener;

    Map<String, SocketChannel> socketMap = new HashMap<String, SocketChannel>();

    public ByteStation(ByteReceiver r) {
        receiver = r;
        serverPort = 778;
        int max = serverPort + 10;
        while (socketListener == null && serverPort < max) {
            log.info("Attempting to create ByteStation with port " + serverPort);
            try {
                socketListener = new Server();
            } catch (Exception e) {
                log.info("Attempt at port " + serverPort + " failed.");
                serverPort++;
            }
        }
        log.info("\n\n ByteStation Listening at " + serverPort + "\n\n");
    }

    public ByteStation(ByteReceiver r, int port) {
        try {
            receiver = r;
            serverPort = port;
            socketListener = new Server();
            log.info("\n\n ByteStation Listening at " + serverPort + "\n\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * @return Returns the port where this ByteStation is listening to. In other words this is the port where a 
	 * particular instance of a ByteStation is listening for any incoming connections and/or messages. 
	 */
    public int getPort() {
        return serverPort;
    }

    protected void removeSocket(String key) {
        this.socketMap.remove(key);
    }

    /**
	 * This method was added so that ServerSocketListener can
	 * add new socket connections to the socketMap of Radio
	 */
    protected void addSocket(SocketChannel s) {
        socketMap.put(s.toString(), s);
    }

    /**
	 * This method was created for inner class Sever.
	 * This is so that SocketChannels of clients who connected to this
	 * ByteStation can get added to the socketMap. 
	 */
    public SocketSession createSession(SocketChannel sc) {
        this.addSocket(sc);
        return new SocketSession(this, sc.toString());
    }

    public SocketSession createSession(String address) {
        getChannel(address);
        return new SocketSession(this, address);
    }

    protected SocketChannel getChannel(String address) {
        SocketChannel sc = (SocketChannel) socketMap.get(address);
        if (sc == null) {
            try {
                String ip = address;
                int port = 777;
                int i = address.indexOf(":");
                if (i != -1) {
                    ip = address.substring(0, i);
                    if (i + 1 < address.length()) port = Integer.parseInt(address.substring(i + 1));
                }
                log.info("Connecting to " + ip + " on port " + port);
                sc = SocketChannel.open();
                sc.connect(new InetSocketAddress(ip, port));
                socketMap.put(address, sc);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return sc;
    }

    /**
	 * The server class screates the ServerSocket that listens
	 * for any clients attempting to connect to the ByteStation.
	 * 
	 * For each client that successfully connects, a ByteListener is
	 * created and the SocketChannel to that client is added to ByteStations
	 * connection map.
	 *  
	 * @author MrJacky
	 *
	 */
    class Server extends Thread {

        ServerSocketChannel ssc;

        boolean run = true;

        public Server() {
            try {
                ssc = ServerSocketChannel.open();
                log.info("Binding server to port " + serverPort);
                ssc.socket().bind(new InetSocketAddress(serverPort));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            start();
        }

        public void run() {
            log.info("ByteStation listening on port " + ssc.socket().getLocalPort());
            while (run) {
                try {
                    SocketChannel s = ssc.accept();
                    SocketSession session = createSession(s);
                    new ByteListener(session, receiver);
                    String address = s.socket().getInetAddress().getHostAddress();
                    log.info("Adding socket connection: " + address);
                } catch (Exception e) {
                    log.warning("Error in accepting new socket connection.");
                    e.printStackTrace();
                }
            }
        }
    }
}

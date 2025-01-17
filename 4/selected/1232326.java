package jerklib;

import jerklib.Session.State;
import jerklib.events.IRCEvent;
import jerklib.events.IRCEvent.Type;
import jerklib.listeners.WriteRequestListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * A class for reading and writing to an IRC connection.
 * This class will also handle PING/PONG.
 * 
 * @author mohadib
 *
 */
class Connection {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private final ConnectionManager manager;

    private final SocketChannel socChannel;

    final List<WriteRequest> writeRequests = Collections.synchronizedList(new ArrayList<WriteRequest>());

    private final ByteBuffer readBuffer = ByteBuffer.allocate(2048);

    private boolean gotFragment;

    private final StringBuffer stringBuff = new StringBuffer();

    private String actualHostName;

    private final Session session;

    /**
	 * @param manager
	 * @param socChannel - socket channel to read from
	 * @param session - Session this Connection belongs to
	 */
    Connection(ConnectionManager manager, SocketChannel socChannel, Session session) {
        this.manager = manager;
        this.socChannel = socChannel;
        this.session = session;
    }

    /**
	 * Get profile use for this Connection
	 * 
	 * @return the Profile
	 */
    Profile getProfile() {
        return session.getRequestedConnection().getProfile();
    }

    /**
	 * Sets the actual host name of this Connection.
	 * @param name
	 */
    void setHostName(String name) {
        actualHostName = name;
    }

    /**
	 * Gets actual hostname for Connection.
	 * 
	 * @return hostname
	 */
    String getHostName() {
        return actualHostName;
    }

    /**
	 * Adds a listener to be notified of all data written via this Connection
	 * 
	 * @param request
	 */
    void addWriteRequest(WriteRequest request) {
        writeRequests.add(request);
    }

    /**
	 * Called to finish the Connection Process
	 * 
	 * @return true if fincon is successfull
	 * @throws IOException
	 */
    boolean finishConnect() throws IOException {
        return socChannel.finishConnect();
    }

    /**
	 * Reads from connection and creates default IRCEvents that 
	 * are added to the ConnectionManager for relaying
	 * 
	 * @return bytes read
	 */
    int read() {
        if (!socChannel.isConnected()) {
            log.severe("Read call while sochan.isConnected() == false");
            return -1;
        }
        readBuffer.clear();
        int numRead = 0;
        try {
            numRead = socChannel.read(readBuffer);
        } catch (Exception e) {
            e.printStackTrace();
            session.disconnected(e);
        }
        if (numRead == -1) {
            session.disconnected(new Exception("Numb read -1"));
        }
        if (session.getState() == State.DISCONNECTED || numRead <= 0) {
            return 0;
        }
        readBuffer.flip();
        String tmpStr = new String(readBuffer.array(), 0, numRead);
        if (tmpStr.indexOf("\r\n") == -1) {
            stringBuff.append(tmpStr);
            gotFragment = true;
            return numRead;
        }
        if (gotFragment) {
            tmpStr = stringBuff.toString() + tmpStr;
            stringBuff.delete(0, stringBuff.length());
            gotFragment = false;
        }
        String[] strSplit = tmpStr.split("\r\n");
        for (int i = 0; i < (strSplit.length - 1); i++) {
            manager.addToEventQueue(new IRCEvent(strSplit[i], session, Type.DEFAULT));
        }
        String last = strSplit[strSplit.length - 1];
        if (!tmpStr.endsWith("\r\n")) {
            stringBuff.append(last);
            gotFragment = true;
        } else {
            manager.addToEventQueue(new IRCEvent(last, session, Type.DEFAULT));
        }
        return numRead;
    }

    /**
	 * Writes all requests in queue to server
	 * 
	 * @return number bytes written
	 */
    long lastWrite = System.currentTimeMillis();

    int bursts = 0;

    int maxBurst = 5;

    long nextWrite = -1;

    int doWrites() {
        if (writeRequests.isEmpty()) {
            return 0;
        }
        WriteRequest req = null;
        if (nextWrite > System.currentTimeMillis()) return 0;
        if (System.currentTimeMillis() - lastWrite < 3000) {
            if (bursts == maxBurst) {
                nextWrite = System.currentTimeMillis() + 8000;
                bursts = 0;
                return 0;
            }
            bursts++;
        } else {
            bursts = 0;
            lastWrite = System.currentTimeMillis();
        }
        req = writeRequests.remove(0);
        String data;
        if (req.getType() == WriteRequest.Type.CHANNEL_MSG) {
            if (req.getMessage().length() > 100) {
                writeRequests.add(0, new WriteRequest(req.getMessage().substring(100), req.getChannel(), req.getSession()));
                data = "PRIVMSG " + req.getChannel().getName() + " :" + req.getMessage().substring(0, 100) + "\r\n";
            } else {
                data = "PRIVMSG " + req.getChannel().getName() + " :" + req.getMessage() + "\r\n";
            }
        } else if (req.getType() == WriteRequest.Type.PRIVATE_MSG) {
            if (req.getMessage().length() > 255) {
                writeRequests.add(0, new WriteRequest(req.getMessage().substring(100), req.getSession(), req.getNick()));
                data = "PRIVMSG " + req.getNick() + " :" + req.getMessage().substring(0, 100) + "\r\n";
            } else {
                data = "PRIVMSG " + req.getNick() + " :" + req.getMessage() + "\r\n";
            }
        } else {
            data = req.getMessage();
            if (!data.endsWith("\r\n")) {
                data += "\r\n";
            }
        }
        byte[] dataArray = data.getBytes();
        ByteBuffer buff = ByteBuffer.allocate(dataArray.length);
        buff.put(dataArray);
        buff.flip();
        int amount = 0;
        try {
            amount = socChannel.write(buff);
        } catch (IOException e) {
            e.printStackTrace();
            session.disconnected(e);
        }
        if (session.getState() == State.DISCONNECTED) {
            return amount;
        }
        fireWriteEvent(req);
        System.out.println("Wrote " + amount + " " + req.getType() + " " + bursts);
        return amount;
    }

    /**
	 * Send a ping
	 */
    void ping() {
        writeRequests.add(new WriteRequest("PING " + actualHostName + "\r\n", session));
        session.pingSent();
    }

    /**
	 * Send a pong
	 * 
	 * @param event , the Ping event
	 */
    void pong(IRCEvent event) {
        session.gotResponse();
        String data = event.getRawEventData().substring(event.getRawEventData().lastIndexOf(":") + 1);
        writeRequests.add(new WriteRequest("PONG " + data + "\r\n", session));
    }

    /**
	 * Alert connection a pong was received
	 */
    void gotPong() {
        session.gotResponse();
    }

    /**
	 * Close connection
	 * 
	 * @param quitMessage
	 */
    void quit(String quitMessage) {
        try {
            if (quitMessage == null) quitMessage = "";
            WriteRequest request = new WriteRequest("QUIT :" + quitMessage + "\r\n", session);
            writeRequests.add(request);
            doWrites();
            socChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Fires a write request to all write listeners
	 * 
	 * @param request
	 */
    void fireWriteEvent(WriteRequest request) {
        for (WriteRequestListener listener : manager.getWriteListeners()) {
            listener.receiveEvent(request);
        }
    }
}

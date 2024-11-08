package com.fujitsu.arcon.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.unicore.upl.ConsignJob;
import org.unicore.upl.Reply;
import org.unicore.upl.RetrieveOutcomeReply;
import org.unicore.upl.ServerRequest;
import org.unicore.utility.PacketisedInputStream;
import org.unicore.utility.PacketisedOutputStream;
import com.fujitsu.arcon.gateway.logger.Logger;
import com.fujitsu.arcon.gateway.logger.LoggerLevel;
import com.fujitsu.arcon.gateway.logger.LoggerManager;

/**
 * Representation of a connection on the underlying protocol.
 *
 * @author Sven van den Berghe, fujitsu
 *
 * @version $Id: StandardConnection.java,v 1.3 2005/01/27 13:49:09 svenvdb Exp $
 *
 **/
public class StandardConnection implements VsiteConnection {

    public static String IDLE = "Idle";

    public static String HANDLING_REQUEST = "Handling request";

    public static String HEADER_TO = "Processing header to Vsite";

    public static String STREAMING_TO = "Streaming extra bytes to Vsite";

    public static String WAITING_FOR_REPLY = "Waiting for reply from Vsite";

    public static String REPLY_CLIENT = "Sending reply to client";

    public static String REPLY_STREAMED_CLIENT = "Sending streamed bytes to client";

    public static String FINISHED = "Finished";

    private Socket socket_to_vsite;

    private InputStream is_to_vsite;

    private OutputStream os_to_vsite;

    private VsiteConnectionFactoryImpl vcfi;

    private ObjectInputStream ois_to_vsite;

    private ObjectOutputStream oos_to_vsite;

    private Logger logger;

    /**
     * Create a new Vsite connection
     *
     **/
    public StandardConnection(VsiteConnectionFactoryImpl vcfi) {
        logger = LoggerManager.get(getClass().getName());
        this.vcfi = vcfi;
    }

    public void init(Socket s, InputStream i, OutputStream o) throws Exception {
        socket_to_vsite = s;
        is_to_vsite = i;
        os_to_vsite = o;
        oos_to_vsite = new ObjectOutputStream(os_to_vsite);
        ois_to_vsite = new ObjectInputStream(is_to_vsite);
    }

    public void close() {
        try {
            is_to_vsite.close();
            os_to_vsite.close();
            socket_to_vsite.close();
        } catch (Exception ex) {
        }
    }

    public String getTarget() {
        return vcfi.getName();
    }

    private String name;

    private String state;

    private long time_of_state_change;

    VsiteConnection vconnection = null;

    private static int counter = 0;

    private boolean first_time = true;

    public void processRequest(ObjectOutputStream oos, ObjectInputStream ois, Socket socket, ServerRequest sr) throws Exception {
        if (first_time) {
            first_time = false;
        } else {
            socket_to_vsite.getOutputStream().write(0);
        }
        ObjectOutputStream oos_to_client = oos;
        Socket socket_to_client = socket;
        name = socket_to_client.getInetAddress().getHostName() + "-to-" + vcfi.getName() + "-" + (counter++);
        InputStream is_to_client = socket_to_client.getInputStream();
        OutputStream os_to_client = socket_to_client.getOutputStream();
        newState(HANDLING_REQUEST);
        boolean do_again = true;
        do {
            try {
                oos_to_vsite.reset();
                newState(HEADER_TO);
                oos_to_vsite.writeObject(sr);
                oos_to_vsite.flush();
                break;
            } catch (IOException ioex) {
                if (do_again) {
                    if (logger.CHAT) logger.log(name, "Reconnecting because: " + ioex.getMessage(), LoggerLevel.CHAT);
                    vcfi.reconnect(this);
                    do_again = false;
                } else {
                    logger.warning(name, "IO problems with connection to Vsite.", ioex);
                    sendVsiteDownToClient(oos_to_client);
                    throw new Exception("Vsite <" + vcfi.getName() + "> cannot be contacted, because: " + ioex.getMessage());
                }
            }
        } while (true);
        if (sr instanceof ConsignJob && ((ConsignJob) sr).hasStreamed()) {
            newState(STREAMING_TO);
            try {
                transferData(is_to_client, os_to_vsite);
            } catch (FromException fex) {
                close();
                throw new Exception(name + " IO Error streaming from client.\n" + fex);
            } catch (ToException tex) {
                sendVsiteDownToClient(oos_to_client);
                logger.warning(name, "IO Error streaming bytes to Vsite.", tex);
                throw new Exception("IO Error streaming bytes to Vsite <" + vcfi.getName() + ">");
            }
        }
        newState(WAITING_FOR_REPLY);
        Reply upl_reply = null;
        try {
            upl_reply = (Reply) ois_to_vsite.readObject();
        } catch (IOException ioex) {
            sendVsiteDownToClient(oos_to_client);
            logger.warning(name, "IO Error reading UPL Reply object.", ioex);
            throw new Exception("Error reading UPL Reply object from Vsite.", ioex);
        } catch (Exception ex) {
            sendVsiteDownToClient(oos_to_client);
            logger.warning(name, "Error reading UPL Reply object.", ex);
            throw new Exception("Error reading UPL Reply object from Vsite (2)", ex);
        }
        newState(REPLY_CLIENT);
        try {
            oos_to_client.writeObject(upl_reply);
            oos_to_client.flush();
        } catch (IOException ioex) {
            close();
            throw new Exception("Error sending UPL Reply object to client: " + ioex);
        }
        if ((upl_reply instanceof RetrieveOutcomeReply && ((RetrieveOutcomeReply) upl_reply).hasStreamed())) {
            newState(REPLY_STREAMED_CLIENT);
            try {
                transferData(is_to_vsite, os_to_client);
            } catch (FromException fex) {
                sendVsiteDownToClient(oos_to_client);
                logger.warning(name, "IO Error streaming reply from NJS.", fex);
                throw new Exception("IO Error streaming reply from NJS.");
            } catch (ToException tex) {
                close();
                throw new Exception("IO Error streaming reply bytes to Client: " + tex);
            }
        }
        newState(IDLE);
    }

    private void sendVsiteDownToClient(ObjectOutputStream oos_to_client) {
        try {
            oos_to_client.writeObject(GWReply.makeReply(GWReply.VSITE_DOWN));
            oos_to_client.flush();
        } catch (IOException iioex) {
        }
        close();
    }

    private void newState(String new_state) {
        state = new_state;
        time_of_state_change = System.currentTimeMillis();
        if (logger.DRIVEL) logger.log(name, "Changing to state <" + state + ">", LoggerLevel.DRIVEL);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (logger.DRIVEL) logger.log(name, "Finalised", LoggerLevel.DRIVEL);
    }

    private static int MY_BUFFER_SIZE = 4 * 4096;

    private byte[] my_buffer = new byte[MY_BUFFER_SIZE];

    private void transferData(InputStream is, OutputStream os) throws FromException, ToException {
        PacketisedInputStream from = new PacketisedInputStream(is);
        PacketisedOutputStream to = new PacketisedOutputStream(os);
        int read;
        try {
            read = from.read(my_buffer);
        } catch (IOException ioex) {
            throw (FromException) (new FromException(ioex.getMessage())).fillInStackTrace();
        }
        while (read > 0) {
            try {
                to.write(my_buffer, 0, read);
            } catch (IOException ioex) {
                throw (ToException) (new ToException(ioex.getMessage())).fillInStackTrace();
            }
            try {
                read = from.read(my_buffer);
            } catch (IOException ioex) {
                throw (FromException) (new FromException(ioex.getMessage())).fillInStackTrace();
            }
        }
        try {
            to.finish();
        } catch (Exception ex) {
        }
    }

    class FromException extends IOException {

        public FromException(String s) {
            super(s);
        }
    }

    class ToException extends IOException {

        public ToException(String s) {
            super(s);
        }
    }
}

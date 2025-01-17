package org.metastatic.rsync.v2;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import org.metastatic.rsync.*;

/**
 * A SocketClient connects to rsync servers over TCP port 873. This
 * version of SocketClient connects in the same fashion as rsync-2.*
 * series clients, using protocol version 26.
 *
 * @version $Revision: 1.5 $
 */
public class SocketClient implements Constants {

    /** The socket connected to the remote server. */
    protected Socket socket;

    /** Our input stream from the server. */
    protected InputStream in;

    /** Our output stream to the server. */
    protected OutputStream out;

    /** The server's message-of-the-day. */
    protected String serverMOTD;

    /** The modules available on the server. */
    protected LinkedList modules;

    /**
    * The raw list of server messages, possibly the MOTD and the module
    * list.
    */
    protected LinkedList serverMessages;

    /** Our connection status. */
    protected boolean connected;

    /** The server's protocol version. */
    protected int remoteVersion;

    /** Whether or not the server wants authorization. */
    protected boolean authReqd;

    /** The server's authentication challenge. */
    protected String challenge;

    /** @ERROR messages the server sends. */
    protected String error;

    /** The protocol version we conform to. */
    public static final int PROTOCOL_VERSION = 26;

    /** The minimum protocol version we support. */
    public static final int MIN_PROTOCOL_VERSION = 15;

    /**
    * This class cannot be directly instatiated. Use either {@link
    * #connect(java.lang.String,java.lang.String)} or {@link
    * #connect(java.lang.String,int,java.lang.String)}.
    */
    protected SocketClient(Socket socket, InputStream in, OutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        connected = true;
        authReqd = false;
        serverMOTD = new String();
        modules = new LinkedList();
        serverMessages = new LinkedList();
    }

    /**
    * Connect to <tt>host</tt> over port 873, asking for
    * <tt>module</tt>.
    *
    * @param host   The host to connect to.
    * @param module The module to use on the host, or can be either
    *    <tt>null</tt> or <tt>"#list"</tt> to fetch a listing of
    *    modules.
    * @return A new SocketClient.
    */
    public static SocketClient connect(String host, String module) throws IOException {
        return connect(host, RSYNCD_PORT, module);
    }

    /**
    * Connect to <tt>host</tt> over <tt>port</tt>, asking for
    * <tt>module</tt>.
    *
    * @param host   The host to connect to.
    * @param port   The port to connect to.
    * @param module The module to use on the host, or can be either
    *    <tt>null</tt> or <tt>"#list"</tt> to fetch a listing of
    *    modules.
    * @return A new SocketClient.
    */
    public static SocketClient connect(String host, int port, String module) throws IOException {
        Socket s = new Socket(host, port);
        s.setTcpNoDelay(true);
        s.setKeepAlive(true);
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();
        String server_greeting = Util.readLine(in);
        SocketClient c = new SocketClient(s, in, out);
        if (!server_greeting.startsWith(RSYNCD_GREETING)) {
            s.close();
            throw new IOException("server sent \"" + server_greeting + "\" rather than greeting");
        }
        try {
            c.remoteVersion = Integer.parseInt(server_greeting.substring(RSYNCD_GREETING.length()));
        } catch (NumberFormatException nfe) {
            s.close();
            throw new IOException("didn't get server version");
        }
        if (c.remoteVersion < MIN_PROTOCOL_VERSION) {
            s.close();
            throw new IOException("unsupported protocol version " + c.remoteVersion);
        }
        Util.writeASCII(out, RSYNCD_GREETING + PROTOCOL_VERSION + "\n");
        if (module == null) {
            Util.writeASCII(out, "#list\n");
        } else {
            Util.writeASCII(out, module + '\n');
        }
        while (true) {
            String line = null;
            try {
                line = Util.readLine(in);
            } catch (EOFException eof) {
                if (c.remoteVersion < 25) {
                    s.close();
                    c.connected = false;
                    break;
                } else {
                    throw eof;
                }
            }
            if (line.startsWith(RSYNCD_AUTHREQD)) {
                c.authReqd = true;
                c.challenge = line.substring(RSYNCD_AUTHREQD.length());
                break;
            } else if (line.startsWith(RSYNCD_OK)) {
                break;
            } else if (line.startsWith(RSYNCD_EXIT)) {
                s.close();
                c.connected = false;
                break;
            } else if (line.startsWith(AT_ERROR)) {
                c.error = line.substring(AT_ERROR.length());
                c.connected = false;
                return c;
            } else {
                c.serverMessages.add(line);
            }
        }
        return c;
    }

    /**
    * See if we are connected. There is no way to terminate the
    * connection if we are the client during this initial phase; the
    * server ends the session only if we send an erronous module name,
    *
    * @return <tt>true</tt> If we are connected.
    */
    public boolean connected() {
        return connected;
    }

    /**
    * Get the messages the server sends during connection. This includes
    * the message-of-the-day, and possibly a listing of available
    * modules.
    *
    * @return The server's messages, in order.
    */
    public List getServerMessages() {
        return java.util.Collections.unmodifiableList(serverMessages);
    }

    /**
    * Test if authentication is required. A {@link Rsync} object cannot
    * be obtained if this method returns true.
    *
    * @reutrn <tt>true</tt> If the user must authenticate herself.
    */
    public boolean authRequired() {
        return authReqd;
    }

    /**
    * Authenticate the user to the server.
    *
    * @param user The username.
    * @param pass The password.
    * @return <tt>true</tt> If authentication succeeds. The connection
    *    will be closed if this method returns false.
    */
    public boolean authenticate(String user, String pass) throws IOException {
        MessageDigest hash = null;
        try {
            MessageDigest.getInstance("BrokenMD4");
        } catch (NoSuchAlgorithmException x) {
            throw new Error(x);
        }
        hash.update(new byte[4], 0, 4);
        try {
            hash.update(pass.getBytes("US-ASCII"), 0, pass.length());
            hash.update(challenge.getBytes("US-ASCII"), 0, challenge.length());
        } catch (java.io.UnsupportedEncodingException shouldNeverHappen) {
        }
        String response = Util.base64(hash.digest());
        Util.writeASCII(out, user + " " + response + '\n');
        String reply = Util.readLine(in);
        if (reply.startsWith(RSYNCD_OK)) {
            authReqd = false;
            return true;
        }
        connected = false;
        error = reply;
        return false;
    }

    /**
    * Get the last error message the server reported.
    *
    * @return The last error message.
    */
    public String getError() {
        return error;
    }

    /**
    * Send the "server args".
    *
    * @param sargv The server args.
    */
    public void serverArgs(String[] sargv) throws IOException {
        for (int i = 0; i < sargv.length; i++) {
            out.write((sargv[i] + "\n").getBytes("US-ASCII"));
        }
    }
}

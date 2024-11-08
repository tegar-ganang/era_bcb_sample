package com.ibm.maf.atp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedExceptionAction;
import com.ibm.atp.AtpConstants;
import com.ibm.atp.auth.Auth;
import com.ibm.atp.auth.AuthPacket;
import com.ibm.atp.auth.Authentication;
import com.ibm.atp.auth.AuthenticationProtocolException;
import com.ibm.atp.auth.SharedSecret;
import com.ibm.atp.auth.SharedSecrets;
import com.ibm.awb.misc.Hexadecimal;
import com.ibm.awb.misc.Resource;
import com.ibm.maf.AgentNotFound;
import com.ibm.maf.ClassName;
import com.ibm.maf.ClassUnknown;
import com.ibm.maf.DeserializationFailed;
import com.ibm.maf.MAFAgentSystem;
import com.ibm.maf.MAFExtendedException;
import com.ibm.maf.MessageEx;
import com.ibm.maf.Name;
import com.ibm.maf.NotHandled;

/**
 * @version 1.10 $Date: 2009/07/28 07:04:53 $
 * @author Danny D. Langue
 * @author Gaku Yamamoto
 * @author Mitsuru Oshima
 */
final class ConnectionHandler extends Thread implements AtpConstants {

    /**
     * message digest algorithm.
     */
    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA";

    private static MessageDigest _mdigest = null;

    private MAFAgentSystem _maf = null;

    private ServerSocket _serversocket = null;

    private Socket _connection = null;

    private Authentication _auth = null;

    private boolean _authenticated = false;

    private static ThreadGroup group = new ThreadGroup("ConnectionHandler");

    private static int BUFFSIZE = 2048;

    private static int number = 0;

    private static boolean authentication = false;

    private static int max_handlers = 32;

    private static int num_handlers = 0;

    private static int idle_handlers = 0;

    static {
        try {
            _mdigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        update();
    }

    static boolean http_tunneling = false;

    static boolean http_messaging = false;

    private java.util.Hashtable headers = new java.util.Hashtable();

    byte future_reply[] = null;

    private static final String CRLF = "\r\n";

    public ConnectionHandler(MAFAgentSystem maf, ServerSocket s) {
        super(group, "handler:" + (number++));
        this._serversocket = s;
        this._maf = maf;
        num_handlers++;
        this.start();
    }

    static byte[] calculateMIC(SharedSecret secret, byte[] agent) {
        if (secret == null) {
            return null;
        }
        if (agent == null) {
            return null;
        }
        _mdigest.reset();
        _mdigest.update(agent);
        _mdigest.update(secret.secret());
        return _mdigest.digest();
    }

    private static boolean equalsSeq(byte[] seqa, byte[] seqb) {
        if ((seqa == null) && (seqb == null)) {
            return true;
        }
        if ((seqa == null) || (seqb == null) || (seqa.length != seqb.length)) {
            return false;
        }
        for (int i = 0; i < seqa.length; i++) {
            if (seqa[i] != seqb[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] getMIC(AtpRequest request) {
        String micstr = request.getRequestParameter("mic");
        if (micstr == null) {
            return null;
        }
        byte[] mic = null;
        try {
            mic = Hexadecimal.parseSeq(micstr);
        } catch (NumberFormatException excpt) {
            System.err.println("Illegal MIC in ATP request header : " + micstr);
        }
        return mic;
    }

    private void handle() throws IOException {
        AtpRequest request = null;
        AtpResponse response = null;
        InetAddress remoteHost = null;
        System.currentTimeMillis();
        remoteHost = this._connection.getInetAddress();
        verboseOut("[Connected from " + remoteHost + ']');
        InputStream in = new BufferedInputStream(this._connection.getInputStream(), BUFFSIZE);
        in.mark(128);
        DataInput di = new DataInputStream(in);
        String topLine = di.readLine();
        in.reset();
        if (topLine == null) {
            try {
                this._connection.close();
            } catch (IOException exx) {
                System.err.println(exx.toString());
            }
            return;
        }
        this._auth = null;
        this._authenticated = false;
        if (AuthPacket.isTopLine(topLine)) {
            if (SharedSecrets.getSharedSecrets() == null) {
                throw new IOException("Authentication failed : no shared secrets.");
            }
            this._auth = new Authentication(Auth.SECOND_TURN, di, this._connection);
            try {
                this._authenticated = this._auth.authenticate();
            } catch (AuthenticationProtocolException excpt) {
                System.err.println(excpt.toString());
                try {
                    this._connection.close();
                } catch (IOException exx) {
                    System.err.println(exx.toString());
                }
                throw new IOException("Authentication failed : " + excpt.getMessage());
            } catch (IOException excpt) {
                System.err.println(excpt.toString());
                try {
                    this._connection.close();
                } catch (IOException exx) {
                    System.err.println(exx.toString());
                }
                throw new IOException("Authentication failed : " + excpt.getMessage());
            }
            if (!this._authenticated) {
                response = new AtpResponseImpl(new BufferedOutputStream(this._connection.getOutputStream(), BUFFSIZE));
                response.sendError(NOT_AUTHENTICATED);
                try {
                    this._connection.close();
                } catch (IOException exx) {
                    System.err.println(exx.toString());
                }
                return;
            }
            in.mark(128);
            topLine = di.readLine();
            in.reset();
            if (topLine == null) {
                try {
                    this._connection.close();
                } catch (IOException exx) {
                    System.err.println(exx.toString());
                }
                return;
            }
        }
        String protocol = topLine.trim();
        protocol = protocol.substring(protocol.lastIndexOf(' ') + 1, protocol.lastIndexOf('/'));
        if (protocol.equalsIgnoreCase("ATP")) {
            if (authentication && ((this._auth == null) || !this._authenticated)) {
                System.err.println("ATP connection from unauthenticated host is closed.");
                response = new AtpResponseImpl(new BufferedOutputStream(this._connection.getOutputStream(), BUFFSIZE));
                response.sendError(NOT_AUTHENTICATED);
                try {
                    this._connection.close();
                } catch (IOException exx) {
                    System.err.println(exx.toString());
                }
                return;
            }
            request = new AtpRequestImpl(in);
            response = new AtpResponseImpl(new BufferedOutputStream(this._connection.getOutputStream(), BUFFSIZE));
        } else if (protocol.equalsIgnoreCase("HTTP")) {
            verboseOut("[Accepting HTTP..]");
            HttpFilter.readHttpHeaders(in, this.headers);
            String r = (String) this.headers.get("method");
            String type = (String) this.headers.get("content-type");
            if ("GET".equalsIgnoreCase(r) && http_messaging) {
                verboseOut("[Http/GET request received.]");
                request = new HttpCGIRequestImpl(in, this.headers);
                response = new HttpCGIResponseImpl(this._connection.getOutputStream());
            } else if ("POST".equalsIgnoreCase(r) && "application/x-atp".equalsIgnoreCase(type) && http_tunneling) {
                verboseOut("[Http/POST request received.]");
                request = new AtpRequestImpl(in);
                response = new AtpResponseImpl(new HttpResponseOutputStream(this._connection.getOutputStream()));
            } else if ("POST".equalsIgnoreCase(r) && "application/x-www-url-form".equalsIgnoreCase(type)) {
                verboseOut("[POST request received.]");
                this.sendHttpResponse();
                verboseOut("[Sending responser.]");
                return;
            } else {
                throw new IOException("Unknown Content-Type:" + type);
            }
        } else {
            throw new IOException("unknown protocol " + protocol);
        }
        try {
            request.parseHeaders();
        } catch (IOException ex) {
            try {
                this._connection.close();
            } catch (IOException exx) {
                System.err.println(exx.toString());
            }
            Daemon.error(remoteHost, System.currentTimeMillis(), "", ex.toString());
            Daemon.access(remoteHost, System.currentTimeMillis(), request.getRequestLine(), response.getStatusCode(), String.valueOf('-'));
            return;
        }
        try {
            this.handleRequest(request, response);
        } catch (IOException ioe) {
            if (Daemon.isVerbose()) {
                ioe.printStackTrace();
            }
            Daemon.error(remoteHost, System.currentTimeMillis(), "", ioe.toString());
            ioe.printStackTrace();
            try {
                response.sendError(INTERNAL_ERROR);
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        } finally {
            try {
                this._connection.close();
            } catch (IOException e) {
                System.err.println(e.toString());
            }
            Daemon.access(remoteHost, System.currentTimeMillis(), request.getRequestLine(), response.getStatusCode(), String.valueOf('-'));
        }
    }

    /**
     * Handles Dispatch Requests
     */
    protected void handleDispatchRequest(AtpRequest request, AtpResponse response) throws IOException {
        response.setContentType("application/x-aglets");
        boolean sent = false;
        try {
            MAFAgentSystem class_sender = MAFAgentSystem.getMAFAgentSystem(request.getSender());
            DataInputStream in = new DataInputStream(request.getInputStream());
            String codebase = in.readUTF();
            int len = in.readInt();
            ClassName class_names[] = new ClassName[len];
            for (int i = 0; i < len; i++) {
                String name = in.readUTF();
                byte desc[] = new byte[in.readInt()];
                in.readFully(desc, 0, desc.length);
                class_names[i] = new ClassName(name, desc);
            }
            byte agent[] = new byte[in.readInt()];
            in.readFully(agent, 0, agent.length);
            if ((this._auth != null) && this._authenticated) {
                byte[] mic = getMIC(request);
                SharedSecret secret = this._auth.getSelectedSecret();
                if ((mic != null) && (secret != null) && (agent != null)) {
                    if (!verifyMIC(mic, secret, agent)) {
                        throw new IOException("Incorrect MIC of transfered aglet.");
                    }
                    verboseOut("MIC is CORRECT.");
                }
            }
            this._maf.receive_agent(request.getAgentName(), request.getAgentProfile(), agent, request.getPlaceName(), class_names, codebase, class_sender);
            response.getOutputStream();
            response.setStatusCode(OKAY);
            response.sendResponse();
            sent = true;
        } catch (SecurityException ex) {
            response.sendError(FORBIDDEN);
            sent = true;
        } catch (ClassUnknown ex) {
            response.sendError(NOT_FOUND);
            sent = true;
        } catch (DeserializationFailed ex) {
            response.sendError(NOT_FOUND);
            sent = true;
        } catch (MAFExtendedException ex) {
            response.sendError(INTERNAL_ERROR);
            sent = true;
        } finally {
            if (sent == false) {
                response.sendError(INTERNAL_ERROR);
            }
        }
    }

    /**
     * Handles fetch requests.
     * 
     * @param request
     * @param response
     */
    protected void handleFetchRequest(AtpRequest request, AtpResponse response) throws IOException {
        response.setContentType("application/x-aglets");
        boolean sent = false;
        try {
            byte b[][] = this._maf.fetch_class(null, request.getFetchClassFile(), request.getAgentProfile());
            OutputStream out = response.getOutputStream();
            verboseOut("fetch_class(" + request.getFetchClassFile() + ") : " + b[0].length + "bytes");
            out.write(b[0]);
            response.setStatusCode(OKAY);
            response.sendResponse();
            sent = true;
        } catch (ClassUnknown ex) {
            response.sendError(NOT_FOUND);
            sent = true;
        } catch (MAFExtendedException ex) {
            response.sendError(INTERNAL_ERROR);
            sent = true;
        } finally {
            if (sent == false) {
                response.sendError(NOT_FOUND);
            }
        }
    }

    protected void handleMessageRequest(AtpRequest request, AtpResponse response) throws IOException {
        response.setContentType("application/x-aglets");
        boolean sent = false;
        Name name = request.getAgentName();
        InputStream in = request.getInputStream();
        byte type = (byte) in.read();
        int len = request.getContentLength();
        byte content[] = new byte[len - 1];
        new DataInputStream(in).readFully(content);
        try {
            switch(type) {
                case SYNC:
                    try {
                        byte ret[] = this._maf.receive_message(name, content);
                        OutputStream out = response.getOutputStream();
                        out.write(HANDLED);
                        out.write(ret, 0, ret.length);
                        response.setStatusCode(OKAY);
                        response.sendResponse();
                    } catch (NotHandled ex) {
                        response.getOutputStream().write(NOT_HANDLED);
                        response.setStatusCode(OKAY);
                        response.sendResponse();
                    } catch (MessageEx ex) {
                        DataOutput out = new DataOutputStream(response.getOutputStream());
                        out.writeByte(EXCEPTION);
                        ex.write(out);
                        response.setStatusCode(OKAY);
                        response.sendResponse();
                    }
                    break;
                case FUTURE:
                    String sender_address = request.getSender();
                    MAFAgentSystem sender = MAFAgentSystem.getMAFAgentSystem(sender_address);
                    long id = this._maf.receive_future_message(name, content, sender);
                    if (sender instanceof MAFAgentSystem_ATPClient) {
                        ((MAFAgentSystem_ATPClient) sender).registerFutureReply(this, id);
                    }
                    OutputStream out = response.getOutputStream();
                    response.setStatusCode(OKAY);
                    response.sendResponse();
                    synchronized (this) {
                        while (this.future_reply == null) {
                            try {
                                this.wait();
                            } catch (InterruptedException ex) {
                            }
                        }
                        out.write(this.future_reply);
                        out.flush();
                        out.close();
                        this.future_reply = null;
                    }
                    break;
                case ONEWAY:
                    this._maf.receive_oneway_message(name, content);
                    response.getOutputStream();
                    response.setStatusCode(OKAY);
                    response.sendResponse();
                    break;
            }
            sent = true;
        } catch (AgentNotFound ex) {
            response.sendError(NOT_FOUND);
            sent = true;
        } catch (ClassUnknown ex) {
            response.sendError(NOT_FOUND);
            sent = true;
        } catch (DeserializationFailed ex) {
            response.sendError(NOT_FOUND);
            sent = true;
        } catch (MAFExtendedException ex) {
            response.sendError(INTERNAL_ERROR);
            sent = true;
        } finally {
            if (sent == false) {
                response.sendError(INTERNAL_ERROR);
            }
        }
    }

    /**
     * Handle ATP Requests
     */
    void handleRequest(AtpRequest request, AtpResponse response) throws IOException {
        switch(request.getMethod()) {
            case DISPATCH:
                this.handleDispatchRequest(request, response);
                break;
            case RETRACT:
                this.handleRetractRequest(request, response);
                break;
            case FETCH:
                this.handleFetchRequest(request, response);
                break;
            case MESSAGE:
                this.handleMessageRequest(request, response);
                break;
            default:
                response.sendError(BAD_REQUEST);
                break;
        }
    }

    /**
     * Handles retract requests.
     * 
     * @param request
     * @param response
     */
    protected void handleRetractRequest(AtpRequest request, AtpResponse response) throws IOException {
        response.setContentType("application/x-aglets");
        boolean sent = false;
        try {
            byte b[] = this._maf.retract_agent(request.getAgentName());
            OutputStream out = response.getOutputStream();
            out.write(b);
            response.setStatusCode(OKAY);
            response.sendResponse();
            sent = true;
        } catch (SecurityException ex) {
            response.sendError(FORBIDDEN);
            sent = true;
        } catch (AgentNotFound ex) {
            response.sendError(NOT_FOUND);
            sent = true;
        } catch (MAFExtendedException ex) {
            ex.printStackTrace();
            response.sendError(INTERNAL_ERROR);
            sent = true;
        } finally {
            if (sent == false) {
                response.sendError(INTERNAL_ERROR);
            }
        }
    }

    @Override
    public synchronized void run() {
        try {
            while (true) {
                try {
                    idle_handlers++;
                    try {
                        final ServerSocket fServerSocket = this._serversocket;
                        this._connection = (Socket) AccessController.doPrivileged(new PrivilegedExceptionAction() {

                            @Override
                            public Object run() throws IOException {
                                return fServerSocket.accept();
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    idle_handlers--;
                    if ((idle_handlers == 0) && (num_handlers < max_handlers)) {
                        new ConnectionHandler(this._maf, this._serversocket);
                    }
                    this.handle();
                    this._connection = null;
                    this.headers.clear();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            num_handlers--;
        }
    }

    synchronized void sendFutureReply(byte b[]) {
        this.future_reply = b;
        this.notify();
    }

    private void sendHttpResponse() throws IOException {
        PrintStream p = new PrintStream(this._connection.getOutputStream());
        p.print("HTTP/1.0 200 OKAY" + CRLF);
        p.print("Content-type: text/html" + CRLF);
        String s = "<HTTP><HEAD> ATP DAEMON/0.1 </HEAD><BODY>" + "<H1> IBM ATP DAEMON/0.1 </H1><BR>" + "</BODY></HTTP>";
        p.print("Content-length: " + s.length() + CRLF + CRLF);
        p.println(s);
        this._connection.getOutputStream().flush();
        this._connection.close();
    }

    @Override
    public String toString() {
        return super.toString() + ", handling = " + (this._connection != null);
    }

    public static void update() {
        Resource res = Resource.getResourceFor("atp");
        BUFFSIZE = res.getInteger("atp.buffersize", 2048);
        authentication = res.getBoolean("atp.authentication", false);
        http_tunneling = res.getBoolean("atp.http.tunneling", false);
        http_messaging = res.getBoolean("atp.http.messaging", false);
        max_handlers = res.getInteger("atp.maxHandlerThread", 32);
    }

    private static void verboseOut(String msg) {
        Daemon.verboseOut(msg);
    }

    static boolean verifyMIC(byte[] mic, SharedSecret secret, byte[] agent) {
        if (mic == null) {
            System.err.println("No MIC");
            return false;
        }
        if (secret == null) {
            System.err.println("No authenticated security domain");
            return false;
        }
        if (agent == null) {
            System.err.println("No Aglet");
            return false;
        }
        byte[] digest = calculateMIC(secret, agent);
        verboseOut("MIC=" + Hexadecimal.valueOf(mic));
        verboseOut("digest=" + Hexadecimal.valueOf(digest));
        return equalsSeq(mic, digest);
    }
}

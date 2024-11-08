package com.quikj.server.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

public class AceInputSocketStream extends AceThread implements AceCompareMessageInterface {

    public static final int READ = 0;

    public static final int READLINE = 1;

    public static final int ONE_BYTE_LENGTH = 2;

    public static final int TWO_BYTE_LENGTH_MSB_FIRST = 3;

    public static final int TWO_BYTE_LENGTH_LSB_FIRST = 4;

    public static final int CUSTOM = 5;

    public static final int MULTILINE = 6;

    public static final int HTTP = 7;

    public static final int PROXY = 8;

    private static final int STATE_COLLECTING_DISCRIMINATOR = 0;

    private static final int STATE_COLLECTING_HEADER = 1;

    private static final int STATE_COLLECTING_LENGTH = 2;

    private static final int STATE_COLLECTING_DATA = 3;

    private AceThread parentThread;

    private Socket socket;

    private InputStream inputStream = null;

    private BufferedReader bReader = null;

    private byte[] protocolDiscriminator;

    private int maxMsgSize;

    private int operationMode;

    private int tagLength;

    private AceInputFilterInterface inputFilter;

    private long userParm;

    private OutputStream proxyStream;

    public AceInputSocketStream(long user_parm, String name, AceThread cthread, Socket socket, byte[] discriminator, int max_msg_size, int mode, int tag_length, AceInputFilterInterface filter) throws IOException, AceException {
        super(name, true);
        socket.setSoTimeout(20000);
        Thread parent_thread;
        if (cthread == null) {
            parent_thread = Thread.currentThread();
        } else {
            parent_thread = cthread;
        }
        if ((parent_thread instanceof AceThread) == false) {
            throw new AceException("The thread supplied as a parameter is not an AceThread");
        }
        if ((mode != READ) && (mode != READLINE) && (mode != ONE_BYTE_LENGTH) && (mode != TWO_BYTE_LENGTH_MSB_FIRST) && (mode != TWO_BYTE_LENGTH_LSB_FIRST) && (mode != CUSTOM) && (mode != MULTILINE) && (mode != HTTP) && (mode != PROXY)) {
            throw new AceException("Invalid mode : " + mode);
        }
        parentThread = (AceThread) parent_thread;
        this.socket = socket;
        inputStream = socket.getInputStream();
        protocolDiscriminator = discriminator;
        maxMsgSize = max_msg_size;
        operationMode = mode;
        tagLength = tag_length;
        inputFilter = filter;
        userParm = user_parm;
    }

    public AceInputSocketStream(long user_parm, String name, Socket socket) throws IOException, AceException {
        this(user_parm, name, null, socket, null, 0, READLINE, 0, null);
    }

    public AceInputSocketStream(long user_parm, String name, Socket socket, boolean multiline) throws IOException, AceException {
        this(user_parm, name, null, socket, null, 0, MULTILINE, 0, null);
    }

    public AceInputSocketStream(long user_parm, String name, Socket socket, byte[] discriminator, int max_msg_size, int mode, int tag_length) throws IOException, AceException {
        this(user_parm, name, null, socket, discriminator, max_msg_size, mode, tag_length, null);
    }

    public AceInputSocketStream(long user_parm, String name, Socket socket, int max_msg_size) throws IOException, AceException {
        this(user_parm, name, null, socket, null, max_msg_size, READ, 0, null);
    }

    public AceInputSocketStream(long user_parm, String name, Socket socket, int max_msg_size, AceInputFilterInterface filter) throws IOException, AceException {
        this(user_parm, name, null, socket, null, max_msg_size, CUSTOM, 0, filter);
    }

    public AceInputSocketStream(long user_parm, String name, Socket socket, int max_msg_size, int mode, int tag_length) throws IOException, AceException {
        this(user_parm, name, null, socket, null, max_msg_size, mode, tag_length, null);
    }

    public AceInputSocketStream(long user_parm, String name, Socket socket, OutputStream proxyStream) throws IOException, AceException {
        this(user_parm, name, null, socket, null, 0, PROXY, 0, null);
        this.proxyStream = proxyStream;
    }

    public AceInputSocketStream(long user_parm, String name, Socket socket, String http) throws IOException, AceException {
        this(user_parm, name, null, socket, null, 0, HTTP, 0, null);
    }

    public static void intToBytesMsbFirst(int value, byte[] buffer, int offset) {
        int shift = 3;
        for (int i = 0; i < 4; i++) {
            buffer[offset + i] = (byte) (value >>> ((shift--) * 8));
        }
    }

    public static void intToBytesMsbLast(int value, byte[] buffer, int offset) {
        int shift = 3;
        for (int i = 3; i >= 0; i--) {
            buffer[offset + i] = (byte) (value >>> ((shift--) * 8));
        }
    }

    public static void longToBytesMsbFirst(long value, byte[] buffer, int offset) {
        int shift = 7;
        for (int i = 0; i < 8; i++) {
            buffer[offset + i] = (byte) (value >>> ((shift--) * 8));
        }
    }

    public static void longToBytesMsbLast(long value, byte[] buffer, int offset) {
        int shift = 7;
        for (int i = 7; i >= 0; i--) {
            buffer[offset + i] = (byte) (value >>> ((shift--) * 8));
        }
    }

    public static void main(String[] args) {
        class MyParentClass extends AceThread {

            private boolean multiline;

            public MyParentClass(boolean multiline) throws IOException {
                super();
                this.multiline = multiline;
            }

            public void run() {
                ServerSocket ssock = null;
                Socket sock = null;
                try {
                    ssock = new ServerSocket(5000);
                    sock = ssock.accept();
                    AceInputSocketStream ais;
                    if (multiline == true) {
                        ais = new AceInputSocketStream(1L, "AceInputSocketStream", sock, multiline);
                    } else {
                        ais = new AceInputSocketStream(1L, "AceInputSocketStream", sock);
                    }
                    ais.start();
                    while (true) {
                        AceMessageInterface msg = ais.waitInputStreamMessage();
                        if ((msg instanceof AceInputSocketStreamMessage) == true) {
                            System.out.print("A message is received with status = ");
                            AceInputSocketStreamMessage is_msg = (AceInputSocketStreamMessage) msg;
                            if (is_msg.getStatus() == AceInputSocketStreamMessage.READ_COMPLETED) {
                                System.out.println("Received message with user parm : " + is_msg.getUserParm() + '\n' + is_msg.getLines());
                            } else {
                                break;
                            }
                        } else if ((msg instanceof AceSignalMessage) == true) {
                            AceSignalMessage signal = (AceSignalMessage) msg;
                            System.out.println("Received signal : " + signal.getSignalId());
                            break;
                        } else {
                            System.err.println("Unexpected message : " + msg.messageType() + " received");
                            break;
                        }
                    }
                    sock.close();
                    ssock.close();
                } catch (IOException ex1) {
                    try {
                        if (sock != null) sock.close();
                        if (ssock != null) ssock.close();
                        System.err.println("IOException : " + ex1.getMessage());
                        return;
                    } catch (IOException ex4) {
                        System.err.println("IOException : " + ex4.getMessage());
                        return;
                    }
                } catch (AceException ex2) {
                    try {
                        if (sock != null) sock.close();
                        if (ssock != null) ssock.close();
                        System.err.println("AceException : " + ex2.getMessage());
                        return;
                    } catch (IOException ex4) {
                        System.err.println("IOException : " + ex4.getMessage());
                        return;
                    }
                }
            }
        }
        try {
            String dflt = new String("readline");
            if (args.length > 0) {
                dflt = args[0];
            }
            AceThread pobj = null;
            if (dflt.equals("readline") == true) {
                pobj = new MyParentClass(false);
            } else if (dflt.equals("multiline") == true) {
                pobj = new MyParentClass(true);
            } else {
                System.err.println("Unknown option : " + dflt);
                System.exit(1);
            }
            pobj.start();
            pobj.join();
            System.exit(0);
        } catch (IOException ex1) {
            System.err.println("IOException in main " + ex1.getMessage());
            System.exit(0);
        } catch (InterruptedException ex2) {
            System.err.println("InterruptedException in main " + ex2.getMessage());
            System.exit(1);
        }
    }

    public static long octetsToIntMsbFirst(byte[] buffer, int offset, int length) throws NumberFormatException {
        if ((length > 8) || (length < 1)) {
            throw new NumberFormatException();
        }
        long ret = 0L;
        int len = length;
        for (int i = 0; i < len; i++) {
            ret |= (((buffer[offset + i]) << ((length - 1) * 8)) & (0xFF << ((--length) * 8)));
        }
        return ret;
    }

    public static long octetsToIntMsbLast(byte[] buffer, int offset, int length) throws NumberFormatException {
        if ((length > 8) || (length < 1)) {
            throw new NumberFormatException();
        }
        long ret = 0L;
        int len = length;
        for (int i = len - 1; i >= 0; i--) {
            ret |= (((long) buffer[offset + i]) << ((length - 1) * 8)) & (0xFF << ((--length) * 8));
        }
        return ret;
    }

    public static void shortToBytesMsbFirst(short value, byte[] buffer, int offset) {
        int shift = 1;
        for (int i = 0; i < 2; i++) {
            buffer[offset + i] = (byte) (value >>> ((shift--) * 8));
        }
    }

    public static void shortToBytesMsbLast(short value, byte[] buffer, int offset) {
        int shift = 1;
        for (int i = 1; i >= 0; i--) {
            buffer[offset + i] = (byte) (value >>> ((shift--) * 8));
        }
    }

    public void dispose() {
        System.out.println(getName() + " being disposed");
        try {
            this.interrupt();
            socket.close();
            socket = null;
        } catch (IOException ex) {
            System.err.println(getName() + ": AceInputSocketStream.dispose() -- " + ex.getMessage());
        }
        flushMessage();
        super.dispose();
    }

    public boolean flushMessage() {
        return parentThread.removeMessage(new AceInputSocketStreamMessage(this, null, 0, 0, userParm, null), this);
    }

    private void processProxy() throws IOException {
        byte[] read_buffer = new byte[1000];
        int len = 0;
        do {
            try {
                len = inputStream.read(read_buffer, 0, 1000);
                if (len > 0) {
                    proxyStream.write(read_buffer, 0, len);
                    proxyStream.flush();
                }
            } catch (InterruptedIOException ex) {
                System.out.println(getName() + " proxy server thread is interrupted");
                if (isInterrupted()) {
                    return;
                }
            }
        } while (len >= 0);
        if (len == -1) {
            if (isInterrupted() == true) {
                return;
            }
            if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, 0, AceInputSocketStreamMessage.EOF_REACHED, userParm, null)) == false) {
                System.err.println(getName() + ": AceInputSocketStream.processRead() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
            }
            return;
        }
    }

    private void processRead() throws IOException {
        byte[] read_buffer = new byte[maxMsgSize];
        int len = 0;
        while (true) {
            while (len == 0) {
                try {
                    len = inputStream.read(read_buffer, 0, maxMsgSize);
                } catch (InterruptedIOException ex) {
                    if (isInterrupted() == true) {
                        return;
                    } else {
                        continue;
                    }
                }
            }
            if (len == -1) {
                if (isInterrupted() == true) {
                    return;
                }
                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, 0, AceInputSocketStreamMessage.EOF_REACHED, userParm, null)) == false) {
                    System.err.println(getName() + ": AceInputSocketStream.processRead() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
                }
                return;
            }
            if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, len, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null)) == false) {
                System.err.println(getName() + ": AceInputSocketStream.processRead() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
            }
        }
    }

    private void processReadCustom() throws IOException {
        byte[] read_buffer = new byte[maxMsgSize];
        int offset = 0;
        while (true) {
            int read_length = inputFilter.numberOfBytesToRead();
            int saved_offset = offset;
            while (read_length > 0) {
                int length_read = 0;
                try {
                    length_read = inputStream.read(read_buffer, offset, read_length);
                } catch (InterruptedIOException ex) {
                    if (isInterrupted() == true) {
                        return;
                    } else {
                        continue;
                    }
                } catch (IndexOutOfBoundsException ex) {
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, offset, AceInputSocketStreamMessage.MESSAGE_OVERFLOW, userParm, null)) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadCustom() -- Could not send overflow  message to the requesting thread: " + getErrorMessage());
                    }
                    return;
                }
                if (length_read == -1) {
                    if (isInterrupted() == true) {
                        return;
                    }
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, offset, AceInputSocketStreamMessage.EOF_REACHED, userParm, null)) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadCustom() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
                    }
                    return;
                }
                read_length -= length_read;
                offset += length_read;
            }
            int ret = inputFilter.processMessage(read_buffer, saved_offset, offset - saved_offset);
            switch(ret) {
                case AceInputFilterInterface.CONTINUE_RECEIVING:
                    break;
                case AceInputFilterInterface.SEND_MESSAGE:
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, offset, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null)) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadCustom() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
                    }
                    offset = 0;
                    read_buffer = new byte[maxMsgSize];
                    break;
                case AceInputFilterInterface.RESET_BUFFER:
                    offset = 0;
                    read_buffer = new byte[maxMsgSize];
                    break;
                default:
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, offset, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "Unknown return from processReadMessage()")) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadCustom() -- Could not send error message to the requesting thread: " + getErrorMessage());
                    }
                    offset = 0;
                    read_buffer = new byte[maxMsgSize];
                    break;
            }
        }
    }

    private void processReadHTTP() throws IOException {
        BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream));
        do {
            boolean to_cont = true;
            boolean simple_req = false;
            String method = null;
            String url = null;
            Vector header_fields = new Vector();
            int content_length = -1;
            char[] body = null;
            String line = null;
            String http_version = null;
            boolean req_message;
            int version = 0;
            String http_status = null;
            String http_reason = "";
            while (true) {
                to_cont = true;
                try {
                    line = bReader.readLine();
                } catch (InterruptedIOException ex) {
                    if (isInterrupted() == true) {
                        return;
                    } else {
                        continue;
                    }
                }
                if (isInterrupted() == true) {
                    return;
                }
                if (line == null) {
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, null, null, null, null, null, 0, AceInputSocketStreamMessage.EOF_REACHED, userParm, null)) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
                    }
                    return;
                }
                if (line.length() > 0) {
                    break;
                }
            }
            StringTokenizer strtok = new StringTokenizer(line, " ");
            int num_fields = strtok.countTokens();
            if (num_fields < 2) {
                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, null, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The request/response line must have at least two tokens")) == false) {
                    System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                }
                return;
            }
            String first_req_field = strtok.nextToken();
            if (first_req_field.toUpperCase().startsWith("HTTP") == true) {
                req_message = false;
            } else {
                req_message = true;
            }
            if (req_message == true) {
                if (num_fields == 2) {
                    method = first_req_field;
                    if (method.toUpperCase().equals("GET") == true) {
                        simple_req = true;
                        url = strtok.nextToken();
                        if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, true, method, url, null, null, null, 0, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null)) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
                        }
                        to_cont = false;
                    } else {
                        if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, method, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "A simple HTTP request must have a GET method")) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error  message to the requesting thread: " + getErrorMessage());
                        }
                        return;
                    }
                } else {
                    method = first_req_field;
                    url = strtok.nextToken();
                    String version_s = strtok.nextToken();
                    StringTokenizer tok = new StringTokenizer(version_s, "/");
                    if (tok.countTokens() < 2) {
                        if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, method, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The HTTP version in the request message is not in proper format")) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                        }
                        return;
                    }
                    String protocol = tok.nextToken();
                    if (protocol.toUpperCase().equals("HTTP") == false) {
                        if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, method, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The HTTP version in the request message is not in proper format - no HTTP found")) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                        }
                        return;
                    }
                    http_version = tok.nextToken();
                }
            } else {
                String version_s = first_req_field;
                StringTokenizer tok = new StringTokenizer(version_s, "/");
                if (tok.countTokens() < 2) {
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, null, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The HTTP version in the response message is not in proper format")) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                    }
                    return;
                }
                String protocol = tok.nextToken();
                if (protocol.toUpperCase().equals("HTTP") == false) {
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, null, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The HTTP version in the response message is not in proper format - no HTTP header found")) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                    }
                    return;
                }
                http_version = tok.nextToken();
                http_status = strtok.nextToken();
                try {
                    version = Integer.parseInt(http_status);
                } catch (NumberFormatException ex) {
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, null, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The HTTP version in the response message is not an integer")) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                    }
                    return;
                }
                if (num_fields > 2) {
                    StringBuffer strbuf = new StringBuffer();
                    for (int i = 2; i < num_fields; i++) {
                        strbuf.append(strtok.nextToken());
                        if (i < num_fields - 1) {
                            strbuf.append(" ");
                        }
                    }
                    http_reason = strbuf.toString();
                }
            }
            if (to_cont == true) {
                StringBuffer header_line = new StringBuffer();
                while (true) {
                    try {
                        line = bReader.readLine();
                    } catch (InterruptedIOException ex) {
                        if (isInterrupted() == true) {
                            return;
                        } else {
                            continue;
                        }
                    }
                    if (isInterrupted() == true) {
                        return;
                    }
                    if (line == null) {
                        AceInputSocketStreamMessage ais = null;
                        if (req_message == true) {
                            ais = new AceInputSocketStreamMessage(this, false, method, url, http_version, header_fields, null, 0, AceInputSocketStreamMessage.EOF_REACHED, userParm, null);
                        } else {
                            ais = new AceInputSocketStreamMessage(this, http_version, http_status, http_reason, header_fields, null, 0, AceInputSocketStreamMessage.EOF_REACHED, userParm, null);
                        }
                        if (parentThread.sendMessage(ais) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
                        }
                        return;
                    }
                    if (line.length() <= 0) {
                        if (header_line.length() > 0) {
                            String header = header_line.toString();
                            StringTokenizer tok = new StringTokenizer(header, ":");
                            if (tok.countTokens() < 2) {
                                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, method, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "A header field contains less than two tokens in " + (req_message == true ? "request" : "response") + " message")) == false) {
                                    System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                                }
                                return;
                            }
                            String key = tok.nextToken().trim();
                            StringBuffer value_buf = new StringBuffer();
                            boolean ft = true;
                            while (tok.hasMoreTokens() == true) {
                                if (ft == false) {
                                    value_buf.append(":");
                                }
                                value_buf.append(tok.nextToken());
                                ft = false;
                            }
                            String value = value_buf.toString().trim();
                            if (key.toUpperCase().equals("CONTENT-LENGTH") == true) {
                                try {
                                    content_length = Integer.parseInt(value);
                                } catch (NumberFormatException ex) {
                                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, method, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The content length parameter does not have a numeric value in " + (req_message == true ? "request" : "response") + " message")) == false) {
                                        System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                                    }
                                    return;
                                }
                            } else {
                                header_fields.addElement(new AceHTTPHeader(key, value));
                            }
                        }
                        break;
                    } else if ((line.startsWith(" ") == true) || (line.startsWith("\t") == true)) {
                        header_line.append(line);
                    } else {
                        if (header_line.length() > 0) {
                            String header = header_line.toString();
                            StringTokenizer tok = new StringTokenizer(header, ":");
                            if (tok.countTokens() < 2) {
                                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, method, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The content length field does not have at least two tokens in " + (req_message == true ? "request" : "response") + " message")) == false) {
                                    System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                                }
                                return;
                            }
                            String key = tok.nextToken().trim();
                            StringBuffer value_buf = new StringBuffer();
                            boolean ft = true;
                            while (tok.hasMoreTokens() == true) {
                                if (ft == false) {
                                    value_buf.append(":");
                                }
                                value_buf.append(tok.nextToken());
                                ft = false;
                            }
                            String value = value_buf.toString().trim();
                            if (key.toUpperCase().equals("CONTENT-LENGTH") == true) {
                                try {
                                    content_length = Integer.parseInt(value);
                                } catch (NumberFormatException ex) {
                                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, method, null, null, null, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "The content lenght param does not have a numeric value in " + (req_message == true ? "request" : "response") + " message")) == false) {
                                        System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send error message to the requesting thread: " + getErrorMessage());
                                    }
                                    return;
                                }
                            } else {
                                header_fields.addElement(new AceHTTPHeader(key, value));
                            }
                        }
                        header_line = new StringBuffer(line);
                    }
                }
                if (content_length == -1) {
                    if (req_message == true) {
                        content_length = 0;
                    } else {
                        if ((version < 199) || (version == 204) || (version == 304)) {
                            content_length = 0;
                        }
                    }
                }
                if (content_length == 0) {
                    AceInputSocketStreamMessage ais = null;
                    if (req_message == true) {
                        ais = new AceInputSocketStreamMessage(this, false, method, url, http_version, header_fields, null, 0, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null);
                    } else {
                        ais = new AceInputSocketStreamMessage(this, http_version, http_status, http_reason, header_fields, null, 0, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null);
                    }
                    if (parentThread.sendMessage(ais) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
                    }
                    to_cont = false;
                }
            }
            if (to_cont == true) {
                int length_read = 0;
                StringBuffer body_buffer = new StringBuffer();
                int num_to_receive = 0;
                if (content_length >= 0) {
                    body = new char[content_length];
                    num_to_receive = content_length;
                } else {
                    body = new char[1000];
                    num_to_receive = body.length;
                }
                while (true) {
                    int len_rcvd = 0;
                    try {
                        len_rcvd = bReader.read(body, 0, num_to_receive);
                    } catch (InterruptedIOException ex) {
                        if (isInterrupted() == true) {
                            return;
                        } else {
                            continue;
                        }
                    }
                    if (isInterrupted() == true) {
                        return;
                    }
                    if (len_rcvd == -1) {
                        AceInputSocketStreamMessage ais = null;
                        if (req_message == true) {
                            ais = new AceInputSocketStreamMessage(this, false, method, url, http_version, header_fields, body_buffer.toString().toCharArray(), length_read, AceInputSocketStreamMessage.EOF_REACHED, userParm, null);
                        } else {
                            ais = new AceInputSocketStreamMessage(this, http_version, http_status, http_reason, header_fields, body_buffer.toString().toCharArray(), length_read, AceInputSocketStreamMessage.EOF_REACHED, userParm, null);
                        }
                        if (parentThread.sendMessage(ais) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
                        }
                        return;
                    }
                    body_buffer.append(body, 0, len_rcvd);
                    length_read += len_rcvd;
                    if (content_length == -1) {
                        continue;
                    }
                    num_to_receive -= len_rcvd;
                    if (length_read >= content_length) {
                        AceInputSocketStreamMessage ais = null;
                        if (req_message == true) {
                            ais = new AceInputSocketStreamMessage(this, false, method, url, http_version, header_fields, body_buffer.toString().toCharArray(), length_read, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null);
                        } else {
                            ais = new AceInputSocketStreamMessage(this, http_version, http_status, http_reason, header_fields, body_buffer.toString().toCharArray(), length_read, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null);
                        }
                        if (parentThread.sendMessage(ais) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadHTTP() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
                        }
                        break;
                    }
                }
            }
        } while (true);
    }

    private void processReadLength() throws IOException {
        int state;
        int length_to_read = 0;
        if (protocolDiscriminator == null) {
            if (tagLength == 0) {
                state = STATE_COLLECTING_LENGTH;
                switch(operationMode) {
                    case ONE_BYTE_LENGTH:
                        length_to_read = 1;
                        break;
                    case TWO_BYTE_LENGTH_MSB_FIRST:
                    case TWO_BYTE_LENGTH_LSB_FIRST:
                        length_to_read = 2;
                        break;
                }
            } else {
                state = STATE_COLLECTING_HEADER;
                length_to_read = tagLength;
            }
        } else {
            state = STATE_COLLECTING_DISCRIMINATOR;
            length_to_read = protocolDiscriminator.length;
        }
        int init_state = state;
        int init_length_to_read = length_to_read;
        int offset = 0;
        byte[] read_buffer = new byte[maxMsgSize];
        while (true) {
            int segment_offset = offset;
            while (length_to_read > 0) {
                int length_read = 0;
                try {
                    length_read = inputStream.read(read_buffer, offset, length_to_read);
                } catch (InterruptedIOException ex) {
                    if (isInterrupted() == true) {
                        return;
                    } else {
                        continue;
                    }
                }
                if (length_read == -1) {
                    if (isInterrupted() == true) {
                        return;
                    }
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, offset, AceInputSocketStreamMessage.EOF_REACHED, userParm, null)) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadLength() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
                    }
                    return;
                }
                offset += length_to_read;
                length_to_read -= length_read;
            }
            switch(state) {
                case STATE_COLLECTING_DISCRIMINATOR:
                    int i;
                    for (i = 0; i < protocolDiscriminator.length; i++) {
                        if (read_buffer[segment_offset + i] != protocolDiscriminator[i]) {
                            if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, offset, AceInputSocketStreamMessage.DISCRIMINATOR_MISMATCH, userParm, null)) == false) {
                                System.err.println(getName() + ": AceInputSocketStream.processReadLength() -- Could not send discriminator mismatch message to the requesting thread: " + getErrorMessage());
                            }
                            return;
                        }
                    }
                    if (i == protocolDiscriminator.length) {
                        if (tagLength == 0) {
                            state = STATE_COLLECTING_LENGTH;
                            switch(operationMode) {
                                case ONE_BYTE_LENGTH:
                                    length_to_read = 1;
                                    break;
                                case TWO_BYTE_LENGTH_MSB_FIRST:
                                case TWO_BYTE_LENGTH_LSB_FIRST:
                                    length_to_read = 2;
                                    break;
                            }
                        } else {
                            state = STATE_COLLECTING_HEADER;
                            length_to_read = tagLength;
                        }
                    }
                    break;
                case STATE_COLLECTING_HEADER:
                    state = STATE_COLLECTING_LENGTH;
                    switch(operationMode) {
                        case ONE_BYTE_LENGTH:
                            length_to_read = 1;
                            break;
                        case TWO_BYTE_LENGTH_MSB_FIRST:
                        case TWO_BYTE_LENGTH_LSB_FIRST:
                            length_to_read = 2;
                            break;
                    }
                    break;
                case STATE_COLLECTING_LENGTH:
                    switch(operationMode) {
                        case ONE_BYTE_LENGTH:
                            length_to_read = read_buffer[segment_offset];
                            break;
                        case TWO_BYTE_LENGTH_MSB_FIRST:
                            length_to_read = (int) octetsToIntMsbFirst(read_buffer, segment_offset, 2);
                            break;
                        case TWO_BYTE_LENGTH_LSB_FIRST:
                            length_to_read = (int) octetsToIntMsbLast(read_buffer, segment_offset, 2);
                            break;
                    }
                    if (offset + length_to_read > maxMsgSize) {
                        if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, offset, AceInputSocketStreamMessage.MESSAGE_OVERFLOW, userParm, null)) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadLength() -- Could not send overflow message to the requesting thread: " + getErrorMessage());
                        }
                        return;
                    } else {
                        state = STATE_COLLECTING_DATA;
                    }
                    break;
                case STATE_COLLECTING_DATA:
                    if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, read_buffer, offset, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null)) == false) {
                        System.err.println(getName() + ": AceInputSocketStream.processReadLength() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
                    }
                    state = init_state;
                    length_to_read = init_length_to_read;
                    offset = 0;
                    read_buffer = new byte[maxMsgSize];
                    break;
            }
        }
    }

    private void processReadLine() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while (true) {
            while (true) {
                try {
                    line = reader.readLine();
                    break;
                } catch (InterruptedIOException ex) {
                    if (isInterrupted() == true) {
                        return;
                    } else {
                        continue;
                    }
                }
            }
            if (line == null) {
                if (isInterrupted() == true) {
                    return;
                }
                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, (String) null, AceInputSocketStreamMessage.EOF_REACHED, userParm, null)) == false) {
                    System.err.println(getName() + ": AceInputSocketStream.processReadLine() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
                }
                return;
            } else {
                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, line, AceInputSocketStreamMessage.READ_COMPLETED, userParm, null)) == false) {
                    System.err.println(getName() + ": AceInputSocketStream.processReadLine() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
                }
            }
        }
    }

    private void processReadMultiline() throws IOException {
        bReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            do {
                StringBuffer strbuf = new StringBuffer();
                while (true) {
                    String line = null;
                    try {
                        line = bReader.readLine();
                    } catch (InterruptedIOException ex) {
                        if (isInterrupted() == true) {
                            return;
                        } else {
                            continue;
                        }
                    }
                    if (line == null) {
                        if (isInterrupted() == true) {
                            return;
                        }
                        if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, strbuf.toString(), AceInputSocketStreamMessage.EOF_REACHED, userParm, null)) == false) {
                            System.err.println(getName() + ": AceInputSocketStream.processReadMultiline() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
                        }
                        return;
                    }
                    if (line.startsWith(".") == true) {
                        break;
                    } else if (line.startsWith(" .") == true) {
                        strbuf.append(line.substring(1) + '\n');
                    } else {
                        strbuf.append(line + '\n');
                    }
                }
                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, strbuf.toString(), AceInputSocketStreamMessage.READ_COMPLETED, userParm, null)) == false) {
                    System.err.println(getName() + ": AceInputSocketStream.processReadMultiline() -- Could not send read completed message to the requesting thread: " + getErrorMessage());
                }
            } while (true);
        } catch (IOException ex) {
            if (isInterrupted() == false) {
                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, false, null, null, null, null, null, 0, AceInputSocketStreamMessage.EOF_REACHED, userParm, null)) == false) {
                    System.err.println(getName() + ": AceInputSocketStream.processReadMultiline() -- Could not send EOF message to the requesting thread: " + getErrorMessage());
                }
            }
            return;
        }
    }

    public void run() {
        try {
            switch(operationMode) {
                case READ:
                    processRead();
                    break;
                case READLINE:
                    processReadLine();
                    break;
                case ONE_BYTE_LENGTH:
                case TWO_BYTE_LENGTH_MSB_FIRST:
                case TWO_BYTE_LENGTH_LSB_FIRST:
                    processReadLength();
                    break;
                case CUSTOM:
                    processReadCustom();
                    break;
                case MULTILINE:
                    processReadMultiline();
                    break;
                case HTTP:
                    processReadHTTP();
                    break;
                case PROXY:
                    processProxy();
                    break;
            }
        } catch (IOException ex) {
            System.err.println(operationMode + " IO Exception : " + ex.getMessage());
            if (isInterrupted() == false) {
                if (parentThread.sendMessage(new AceInputSocketStreamMessage(this, null, 0, AceInputSocketStreamMessage.ERROR_OCCURED, userParm, "IOException: " + ex.getMessage())) == false) {
                    System.err.println(getName() + ": AceInputSocketStream.run() -- Could not send error message to the requesting thread: " + getErrorMessage());
                }
            }
        }
    }

    public boolean same(AceMessageInterface obj1, AceMessageInterface obj2) {
        boolean ret = false;
        if (((obj1 instanceof AceInputSocketStreamMessage) == true) && ((obj2 instanceof AceInputSocketStreamMessage) == true)) {
            if (((AceInputSocketStreamMessage) obj1).getInputSocketStream() == ((AceInputSocketStreamMessage) obj2).getInputSocketStream()) {
                ret = true;
            }
        }
        return ret;
    }

    public AceMessageInterface waitInputStreamMessage() {
        Thread thr = Thread.currentThread();
        if ((thr instanceof AceThread) == false) {
            writeErrorMessage("This method is not being called from an object which is a sub-class of type AceThread");
            return null;
        }
        AceThread cthread = (AceThread) thr;
        while (true) {
            AceMessageInterface msg_received = cthread.waitMessage();
            if ((msg_received instanceof AceInputSocketStreamMessage) == true) {
                if (((AceInputSocketStreamMessage) msg_received).getInputSocketStream() == this) {
                    return msg_received;
                }
            } else if ((msg_received instanceof AceSignalMessage) == true) {
                return msg_received;
            }
        }
    }

    public AceThread getParentThread() {
        return parentThread;
    }

    public void setParentThread(AceThread parentThread) {
        this.parentThread = parentThread;
    }
}

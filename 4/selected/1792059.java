package us.conxio.hl7.hl7stream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;
import us.conxio.hl7.hl7message.HL7Message;

/**
 *
 * @author scott
 */
public class HL7MLLPStream extends HL7StreamBase implements HL7Stream {

    private static final int STX = 0x0b, FS = 0x1c, EOB = 0x0d;

    private static Logger logger = Logger.getLogger(HL7MLLPStream.class);

    private static final String ACK_CODE_OK = HL7Message.ACK_CODE_OK;

    /**
    * the host name of the destination of the socket, if a writer, or "locahost"
    * if the stream is a reader.
    */
    String host;

    /**
    * The port number of the destination if a writer, or the port whcih the
    * server is listening on, if a server.
    */
    int port, timeOutSeconds = 30;

    /**
    * The i/o socket reference by which the object performs i/o.
    */
    Socket socket;

    /**
    * The BufferedReader used for reading the socket.
    */
    BufferedReader in;

    /**
    * The BufferedWriter used for writing to the socket.
    */
    BufferedWriter out;

    /**
    * Constructs a new stream object using the argument socket.
    * @param sock A connected socket resulting from a connection request to a
    * listening port specified in an invocation of accept().
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    public HL7MLLPStream(Socket sock) throws HL7IOException {
        socket = sock;
        directive = READER;
        mediaType = SOCKET_TYPE;
    }

    /**
    * Constructs a new stream object using the argument socket.
    * @param sock A connected socket resulting from a connection request to a
    * listening port specified in an invocation of accept().
    * @param openReq A boolean flag indicating that the reader should be opened
    * at the time of construction.
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    public HL7MLLPStream(Socket sock, boolean openReq) throws HL7IOException {
        this(sock);
        if (openReq) openReader();
    }

    /**
    * Creates an outgoing tcp socket stream connection to the argument host
    * and port.
    * @param host The host DNS name to which the connection is requested.
    * @param port The tcp port number upon which the connection is requested.
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    public HL7MLLPStream(String hostStr, int portV) throws HL7IOException {
        port = portV;
        host = hostStr;
        directive = WRITER;
        mediaType = SOCKET_TYPE;
    }

    /**
    * Creates an outgoing tcp socket stream connection to the argument host
    * and port.
    * @param host The host DNS name to which the connection is requested.
    * @param port The tcp port number upon which the connection is requested.
    * @param openReq A boolean flag indicating that the writer should be opened
    * at the time of construction.
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    public HL7MLLPStream(String host, int port, boolean openReq) throws HL7IOException {
        this(host, port);
        if (openReq) openWriter();
    }

    public boolean open() throws HL7IOException {
        if (isOpen()) return true;
        if (isWriter()) {
            return openWriter();
        } else if (isReader()) {
            return openReader();
        }
        throw new HL7IOException("Not a reader. Not a writer.", HL7IOException.INCONSISTENT_STATE);
    }

    /**
    * Closes the context tcp socket stream.
    * @return true if the operation is successful, otherwise false.
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    public boolean close() throws HL7IOException {
        if (isClosed()) return true;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ioEx) {
            throw new HL7IOException(streamID() + "IOException", ioEx);
        }
        statusValue = CLOSED;
        return true;
    }

    private String _readMsg() throws HL7IOException {
        StringBuilder hl7Msg = new StringBuilder();
        int inData;
        boolean startFound = false, fsFound = false;
        try {
            while ((inData = in.read()) != -1) {
                if (inData == STX) {
                    startFound = true;
                    continue;
                }
                if (inData == FS) {
                    fsFound = true;
                    continue;
                }
                if (inData == EOB && fsFound) return (hl7Msg.toString());
                if (startFound) hl7Msg.append((char) inData);
            }
        } catch (IOException ioEx) {
            throw new HL7IOException(streamID() + "IOException", ioEx);
        }
        if (hl7Msg.length() > 0) return hl7Msg.toString();
        return null;
    }

    /**
    * Reads a HL7 message string from the context socket stream.
    * @return a String containing the read message.
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    public String readMsg() throws HL7IOException {
        if (isClosed()) {
            throw new HL7IOException(streamID() + "Socket previously closed.", HL7IOException.STREAM_CLOSED);
        }
        if (socket.isClosed() || !socket.isConnected()) {
            close();
            throw new HL7IOException(streamID() + "Socket closed. Closing Stream", HL7IOException.STREAM_CLOSED);
        }
        String retnStr = _readMsg();
        if (retnStr != null && !retnStr.isEmpty()) return retnStr;
        if (socket.isClosed() || !socket.isConnected()) {
            close();
            throw new HL7IOException(streamID() + "Socket closed after read. Closing Stream", HL7IOException.STREAM_CLOSED);
        }
        return (null);
    }

    /**
    * Reads a HL7Message object from the context socket stream.
    * @return the read HL7Message object.
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    public HL7Message read() throws HL7IOException {
        String msgStr = readMsg();
        if (msgStr == null || msgStr.isEmpty()) return null;
        HL7Message msg = new HL7Message(msgStr);
        String msgCtlID = msg.get("MSH.10");
        String traceHeader = new StringBuffer("read(): got [").append(msg.idString()).append("].[").append(msgCtlID).append("]:").toString();
        logger.trace(traceHeader);
        logger.trace(msg.toString());
        HL7Message ack = msg.acknowledgment(true, "ok", null, null);
        traceHeader = new StringBuffer("read(): Sending acknowledgment [").append(ack.idString()).append("].[").append(ack.get("MSH.10")).append("]:").toString();
        writeMsgString(ack.toString());
        logger.trace(traceHeader);
        logger.trace(ack.toString());
        return (msg);
    }

    /**
    * Writes a HL7 message string on the context outound stream.
    * @param msg a HL7 message string.
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    public void writeMsgString(String msg) throws HL7IOException {
        if (msg == null) {
            throw new HL7IOException("Null HL7 Msg.", HL7IOException.NULL_MSG);
        }
        if (!isOpen() || out == null) open();
        try {
            out.write((char) STX + msg + (char) FS + (char) EOB);
            out.flush();
        } catch (IOException ioEx) {
            throw new HL7IOException(streamID() + "IOException", ioEx);
        }
    }

    /**
    * Writes the argument HL7Message object on the context outbound stream.
    * @param hl7Msg the HL7Message object to be written.
    * @return true if the operation is successful, otherwise false.
    * @throws us.conxio.HL7.HL7Stream.HL7IOException
    */
    @SuppressWarnings("empty-statement")
    public boolean write(HL7Message hl7Msg) throws HL7IOException {
        String msgCtlID = hl7Msg.controlID();
        String traceHeader = "write(" + hl7Msg.idString() + "):";
        logger.trace(traceHeader + " writing.");
        writeMsgString(hl7Msg.toHL7String());
        logger.trace(traceHeader + " wrote.");
        String hl7MsgStr;
        while ((hl7MsgStr = readMsg()) == null) ;
        HL7Message reply = new HL7Message(hl7MsgStr);
        String ackCode = reply.get("MSA.1");
        String replyCtlID = reply.get("MSA.2");
        logger.trace(traceHeader + " got " + reply.idString() + ":" + ackCode + "." + replyCtlID + ".");
        if (!ackCode.equals(ACK_CODE_OK)) {
            HL7IOException hiEx = new HL7IOException(streamID() + traceHeader + "NAck (" + ackCode + ") received with " + replyCtlID + " in " + reply.idString(), HL7IOException.HL7_NACK);
            logger.error("throwing ", hiEx);
            throw hiEx;
        } else if (!replyCtlID.equals(msgCtlID)) {
            HL7IOException hiEx = new HL7IOException(traceHeader + "Ack received for " + msgCtlID + "with MSA.2: " + replyCtlID + " in " + reply.idString(), HL7IOException.WRONG_CTLID);
            logger.error("throwing ", hiEx);
            throw hiEx;
        }
        return true;
    }

    private boolean isWriter() {
        return this.directive == WRITER;
    }

    private boolean isReader() {
        return this.directive == READER;
    }

    private boolean openReader() throws HL7IOException {
        try {
            this.out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        } catch (IOException ioEx) {
            throw new HL7IOException(streamID() + "IOException", ioEx);
        }
        this.statusValue = OPEN;
        return true;
    }

    private boolean openWriter() throws HL7IOException {
        Socket sock;
        try {
            sock = new Socket(this.host, this.port);
            sock.setSoTimeout(this.timeOutSeconds * 1000);
            this.out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            this.in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (UnknownHostException uhEx) {
            throw new HL7IOException(streamID() + "UnknownHost.", HL7IOException.UNKNOWN_HOST);
        } catch (SocketException sEx) {
            throw new HL7IOException(streamID() + "SocketException", sEx);
        } catch (IOException ioEx) {
            throw new HL7IOException(streamID() + "IOException", ioEx);
        }
        socket = sock;
        statusValue = OPEN;
        return true;
    }

    private String streamID() {
        return new StringBuilder("Stream(").append(host).append(":").append(Integer.toString(port)).append(")").toString();
    }
}

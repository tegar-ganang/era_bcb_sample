package com.koutra.dist.proc.cluster;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.util.ByteArrayUtil;

/**
 * Used in conjunction with a <code>PipedReader</code>, this class is an writer
 * that can write the character stream to a socket, thereby allowing pipelines
 * to perform part of their computation on a remote JVM.
 * 
 * @author Pafsanias Ftakas
 */
public class PipedWriter extends Writer {

    private static final Logger logger = Logger.getLogger(PipedWriter.class);

    protected ServerSocket socket;

    protected Socket clientSocket;

    protected String charset;

    protected Writer writer;

    protected InetAddress otherEndHostIP;

    protected int otherEndPort;

    /**
	 * Initializing constructor. Initializes a server socket.
	 * @param charset the charset to use for transforming the stream to characters.
	 * @throws IOException iff something goes wrong while setting up the server socket.
	 */
    public PipedWriter(String charset) throws IOException {
        this.socket = new ServerSocket(0);
        if (logger.isDebugEnabled()) logger.debug("Started server socket at port: " + socket.getLocalPort());
        this.clientSocket = null;
        this.charset = charset;
        this.writer = null;
    }

    /**
	 * Accessor for the server socket host address.
	 * @return the server socket host address.
	 */
    public byte[] getServerHostIP() {
        return socket.getInetAddress().getAddress();
    }

    /**
	 * Accessor for the server socket port number.
	 * @return the server socket port number.
	 */
    public int getServerPort() {
        return socket.getLocalPort();
    }

    /**
	 * Accessor for the host address communicated through the client socket input stream.
	 * @return the other end's host address.
	 */
    public byte[] getOtherEndHostIP() {
        return otherEndHostIP.getAddress();
    }

    /**
	 * Accessor for the port number communicated through the client socket input stream.
	 * @return the other end's port number.
	 */
    public int getOtherEndPort() {
        return otherEndPort;
    }

    /**
	 * Connect the server socket to the client. This method potentially blocks until the
	 * corresponding <code>PipedReader</code> connects to us. After connecting with the
	 * client, this class receives the remote end information from the other side.
	 * This information is "don't care" for a task boundary that sits at the end of
	 * a demux/mux pipeline, but is very important for a task boundary that sits at
	 * the begining of the demux/mux pipeline, as it contains the server socket information
	 * for the server socket sitting at the task boundary at the end.
	 * @throws IOException iff something goes wrong during the connection to the client
	 * and the receiving of the remote information.
	 */
    public void connect() throws IOException {
        clientSocket = socket.accept();
        if (logger.isDebugEnabled()) logger.debug("Accepted socket: " + clientSocket.getLocalAddress() + ":" + clientSocket.getLocalPort());
        writer = new OutputStreamWriter(clientSocket.getOutputStream(), charset);
        Reader reader = new InputStreamReader(clientSocket.getInputStream());
        StringWriter writer = new StringWriter();
        int count;
        char[] buffer = new char[8 * 1024];
        while ((count = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, count);
            if (writer.toString().indexOf('\n') != -1) break;
        }
        if (logger.isTraceEnabled()) logger.trace("Read '" + writer.toString().trim() + "' from the socket.");
        String readInfo = writer.toString().substring(0, writer.toString().indexOf('\n'));
        String remoteIPString = readInfo.substring(0, readInfo.indexOf('|'));
        String remotePortString = readInfo.substring(readInfo.indexOf('|') + 1, readInfo.length() - 1);
        otherEndHostIP = InetAddress.getByAddress(ByteArrayUtil.decodeFromHex(remoteIPString));
        otherEndPort = Integer.parseInt(remotePortString);
    }

    /**
	 * Implementation of the <code>Writer</code> interface.
	 */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (writer == null) connect();
        writer.write(cbuf, off, len);
    }

    /**
	 * Implementation of the <code>Writer</code> interface.
	 */
    @Override
    public void flush() throws IOException {
        if (writer == null) connect();
        writer.flush();
    }

    /**
	 * Implementation of the <code>Writer</code> interface.
	 */
    @Override
    public void close() throws IOException {
        if (writer == null) connect();
        writer.close();
    }
}

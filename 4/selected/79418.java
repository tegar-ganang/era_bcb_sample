package com.koutra.dist.proc.cluster;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.log4j.Logger;
import com.koutra.dist.proc.util.ByteArrayUtil;

/**
 * Used in conjunction with a <code>PipedInputStream</code>, this class is an output
 * stream that can write the byte stream to a socket, thereby allowing pipelines
 * to perform part of their computation on a remote JVM.
 * 
 * @author Pafsanias Ftakas
 */
public class PipedOutputStream extends OutputStream {

    private static final Logger logger = Logger.getLogger(PipedOutputStream.class);

    protected ServerSocket socket;

    protected Socket clientSocket;

    protected InetAddress otherEndHostIP;

    protected int otherEndPort;

    /**
	 * Initializing constructor. Initializes a server socket.
	 * @throws IOException iff something goes wrong while setting up the server socket.
	 */
    public PipedOutputStream() throws IOException {
        socket = new ServerSocket(0);
        if (logger.isDebugEnabled()) logger.debug("Started server socket at port: " + socket.getLocalPort());
        clientSocket = null;
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
	 * corresponding <code>PipedInputStream</code> connects to us. After connecting with the
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
	 * Implementation of the <code>OutputStream</code> interface.
	 */
    @Override
    public void write(int b) throws IOException {
        if (clientSocket == null) connect();
        clientSocket.getOutputStream().write(b);
    }

    /**
	 * Override the <code>OutputStream</code> implementation for better performance.
	 */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (clientSocket == null) connect();
        clientSocket.getOutputStream().write(b, off, len);
    }

    /**
	 * Override the <code>OutputStream</code> implementation to flush the client socket's
	 * output stream.
	 */
    @Override
    public void flush() throws IOException {
        if (clientSocket == null) connect();
        clientSocket.getOutputStream().flush();
    }

    /**
	 * Override the <code>OutputStream</code> implementation to close the client socket's
	 * output stream.
	 */
    @Override
    public void close() throws IOException {
        if (clientSocket == null) connect();
        clientSocket.getOutputStream().close();
    }
}

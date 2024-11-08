package com.simpleftp.ftp.server.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.simpleftp.ftp.server.ThreadPool;
import com.simpleftp.ftp.server.connection.FtpTelnetInputStream;
import com.simpleftp.ftp.server.connection.FtpTelnetOutputStream;
import com.simpleftp.ftp.server.exception.BindException;
import com.simpleftp.ftp.server.filesystem.FileSystemManager;
import com.simpleftp.ftp.server.objects.FtpReply;
import com.simpleftp.ftp.server.objects.FtpTransferStatus;
import com.simpleftp.ftp.server.objects.FtpUserSession;
import com.simpleftp.ftp.server.utils.FtpConstants;
import com.simpleftp.ftp.server.utils.FtpLogger;
import com.simpleftp.ftp.server.utils.FtpServerConfiguration;
import com.simpleftp.ftp.server.utils.TextUtil;

/**
 * This is a singleton class which will take care of all data transfer
 * between the host and the client. This class should be a pure thread safe
 * class as multiple sessions will be using this for sending and receiving data.
 * @author sajil
 */
public class FtpDataConnectionManager {

    private static boolean create = true;

    private static FtpDataConnectionManager connectionManager = null;

    private FtpSessionManager sessionManager;

    private ExecutorService workerPool;

    private static Logger logger = FtpLogger.getLogger();

    Future<String> future = null;

    /**
	 * private constructor to make the class singleton.
	 * */
    private FtpDataConnectionManager() {
        sessionManager = FtpSessionManager.getSessionManager();
        int poolSize = Integer.parseInt(FtpServerConfiguration.getServerConfiguration().getPoolSize().trim());
        workerPool = ThreadPool.getWorkerThreadPool(poolSize);
    }

    /**
	 * The method to create the singleton object. I have made it 
	 * synchronized for making it thread safe. 
	 * */
    public static synchronized FtpDataConnectionManager getDataConnectionManager() {
        if (create) {
            create = false;
            connectionManager = new FtpDataConnectionManager();
        }
        return connectionManager;
    }

    /**
	 * Method for checking whether there is already a data connection opened
	 * for the session at same port and host.
	 * @param session the FtpUserSession
	 * @param hostIp String the host address
	 * @param port   String the host port number  
	 * */
    public boolean hasDataConnection(FtpUserSession session, String hostIP, String port) {
        boolean hasDataConnection = false;
        if (session == null) {
            return false;
        }
        if (session.getDataConnection() == null) {
            return false;
        }
        if (session.getDataConnection().getInputStream() == null || session.getDataConnection().getOutputStream() == null) {
            return false;
        } else if (session.getDataConnection().getClientIP().equals(hostIP) && session.getDataConnection().getClientPort().equals(port)) {
            return true;
        }
        return hasDataConnection;
    }

    /**
	 * The method will open a port and listen for client to connect.
	 * Also create a data stream and assign it for the current session for
	 * data transfer.
	 * @param session current session
	 * */
    public int listenAtPort(final FtpUserSession session) throws BindException {
        final ServerSocket serverSocket = openSocket(0);
        future = workerPool.submit(new Callable<String>() {

            public String call() {
                try {
                    serverSocket.setSoTimeout(5000);
                    Socket socket = serverSocket.accept();
                    OutputStream out = socket.getOutputStream();
                    InputStream in = socket.getInputStream();
                    if (session.getTransferStatus().getTransferType() == FtpTransferStatus.TRANSFER_TYPE.ASCII) {
                        in = new FtpTelnetInputStream(in, isStickyCRLF());
                        out = new FtpTelnetOutputStream(out, isStickyCRLF());
                    }
                    session.getDataConnection().setInputStream(in);
                    session.getDataConnection().setOutputStream(out);
                    if (FtpLogger.debug) {
                        logger.debug("Established inbound data connection from the client");
                    }
                } catch (IOException e) {
                    logger.error("Error while accepting connection", e);
                }
                return "";
            }
        });
        return serverSocket.getLocalPort();
    }

    private ServerSocket openSocket(int tryCnt) throws BindException {
        int nextFreePort = getRandomPort();
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(nextFreePort);
        } catch (IOException e) {
            if (FtpLogger.debug) {
                logger.debug("Opening port at " + nextFreePort + "failed");
            }
            if (tryCnt < 5) {
                openSocket(tryCnt++);
            } else {
                throw new BindException("Unable to open a local port");
            }
        }
        if (FtpLogger.debug) {
            logger.debug("Opening port at " + nextFreePort);
        }
        return serverSocket;
    }

    /**
	 * Here I am generating a port number in random. The only criteria I have put is the 
	 * port number should be in between FTP_MIN_PORT = 1025 and FTP_MAX_PORT = 65535.//TODO These values
	 * should be configurable through server.xml
	 */
    private Integer getRandomPort() {
        return (int) (FtpConstants.FTP_MIN_PORT + Math.random() * (FtpConstants.FTP_MAX_PORT - FtpConstants.FTP_MIN_PORT) + 0.5);
    }

    /**
	 * read from the data connection and write in to the file.
	 * @param session
	 */
    public void handleDataReceive(final FtpUserSession session, final OutputStream writter) {
        workerPool.execute(new Runnable() {

            public void run() {
                Thread.currentThread().setName("Data Receive Thread");
                writeIntoFile(session, writter);
            }
        });
    }

    private void writeIntoFile(final FtpUserSession session, final OutputStream writter) {
        InputStream clientStream = session.getDataConnection().getInputStream();
        byte[] buffer = new byte[FtpConstants.bufferSize];
        long byteTransferred = 0;
        session.getTransferStatus().setTransferInProgress(true);
        try {
            while (true) {
                int read = clientStream.read(buffer, 0, buffer.length);
                if (read == -1) break;
                writter.write(buffer, 0, read);
                byteTransferred += read;
                session.getTransferStatus().setByteTransfered(byteTransferred);
            }
            writter.close();
        } catch (Exception e) {
            session.getControlConnection().scheduleSend(FtpReply.getFtpReply(451));
            session.getDataConnection().close();
            logger.error("Exception while writtinginto local file" + e);
            return;
        }
        session.getTransferStatus().setTransferInProgress(false);
        session.getDataConnection().close();
        session.getControlConnection().scheduleSend(FtpReply.getFtpReply(226));
        if (FtpLogger.debug) {
            logger.debug("Data transfer complete for user session" + session.getSessionKey());
        }
    }

    /**
	 * read from the file and write in to the client socket.
	 * @param session
	 * @param writter
	 */
    public void handleDataSend(final FtpUserSession session, final InputStream fileReader) {
        workerPool.execute(new Runnable() {

            public void run() {
                Thread.currentThread().setName("Data Send Thread");
                readFromFile(session, fileReader);
            }
        });
    }

    private void readFromFile(final FtpUserSession session, final InputStream fileReader) {
        OutputStream clientStream = session.getDataConnection().getOutputStream();
        FileInputStream fileOut = (FileInputStream) fileReader;
        FileChannel fcin = fileOut.getChannel();
        byte[] buffer = new byte[FtpConstants.bufferSize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        long byteTransferred = 0;
        session.getTransferStatus().setTransferInProgress(true);
        try {
            while (true) {
                byteBuffer.clear();
                int r = fcin.read(byteBuffer);
                if (r == -1) {
                    break;
                }
                byteBuffer.flip();
                clientStream.write(byteBuffer.array(), 0, byteBuffer.limit());
                byteTransferred += r;
                session.getTransferStatus().setByteTransfered(byteTransferred);
            }
            session.getTransferStatus().setTransferInProgress(false);
            fileReader.close();
        } catch (Exception e) {
            session.getControlConnection().scheduleSend(FtpReply.getFtpReply(451));
            session.getDataConnection().close();
            logger.error("Exceptio while reading from local file" + e);
            return;
        }
        session.getDataConnection().close();
        session.getControlConnection().scheduleSend(FtpReply.getFtpReply(226));
        if (FtpLogger.debug) {
            logger.debug("Data transfer complete for user session" + session.getSessionKey());
        }
    }

    /**
	 * Create an out bound data connection to the client. Also set the future object to be checked for 
	 * completion of the data connection, which can be checked from the subsequent client commands to
	 * make sure the data connection is established.
	 * */
    public synchronized void createDataConnection(final FtpUserSession session, final String hostName, final int portNum) throws UnknownHostException, IOException {
        future = workerPool.submit(new Callable<String>() {

            public String call() {
                Socket ftpClientSocket;
                try {
                    ftpClientSocket = new Socket(hostName, portNum);
                    OutputStream out = ftpClientSocket.getOutputStream();
                    InputStream in = ftpClientSocket.getInputStream();
                    if (session.getTransferStatus().getTransferType() == FtpTransferStatus.TRANSFER_TYPE.ASCII) {
                        in = new FtpTelnetInputStream(in, isStickyCRLF());
                        out = new FtpTelnetOutputStream(out, isStickyCRLF());
                    }
                    session.getDataConnection().setClientIP(hostName);
                    session.getDataConnection().setClientPort(portNum + "");
                    session.getDataConnection().setInputStream(in);
                    session.getDataConnection().setOutputStream(out);
                    if (FtpLogger.debug) {
                        logger.debug("Established outbound data connection to the client");
                    }
                } catch (UnknownHostException e) {
                    logger.error("Could not established outbound data connection to the client " + e);
                    session.getControlConnection().scheduleSend(FtpReply.getFtpReply(425));
                } catch (IOException e) {
                    logger.error("Could not establish outbound data connection to the client " + e);
                    session.getControlConnection().scheduleSend(FtpReply.getFtpReply(425));
                }
                return "";
            }
        });
    }

    public synchronized Future<String> getFuture() {
        return future;
    }

    /**
	 * The method to check for 'line break' .
	 * return true for CRLF and false for LF
	 * */
    public static boolean isStickyCRLF() {
        String lineSeparator = System.getProperty("line.separator");
        if ("\r\n".equals(lineSeparator)) {
            return true;
        }
        return false;
    }
}

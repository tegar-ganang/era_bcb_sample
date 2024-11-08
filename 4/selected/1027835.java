package soht.server.java;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import javax.servlet.http.*;
import org.apache.log4j.Logger;

/**
 * Handles communication between client and proxy.
 * Implements the IO functionality for the Socket over HTTP
 * proxy service.  This servlet accepts incoming HTTP connections,
 * and opens the requested socket session and proxies the IO over
 * the HTTP connection.
 *
 * @author Eric Daugherty
 */
public class SocketProxyServlet extends HttpServlet {

    private static final long serialVersionUID = 2704423272951272740L;

    private static Logger log = Logger.getLogger(SocketProxyServlet.class);

    private static ConnectionManager connectionManager = new ConnectionManager();

    private static int readerCount = 0;

    private static int writerCount = 0;

    public static ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public static int getReaderCount() {
        return readerCount;
    }

    public static int getWriterCount() {
        return writerCount;
    }

    /**
     * Handles GET requests.  We want to ignore these requests because all
     * clients use the POST method.  This will return a blank page so that
     * anyone who snoopes around will not get any information about the
     * services offered.
     *
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws java.io.IOException thrown if there is an error writing the output.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("<HTML>");
        out.println("<BODY>");
        out.println("</BODY>");
        out.println("</HTML>");
    }

    /**
     * Handles POST reqeusts.  Clients post a request here to initiate a
     * session or to read, write or close an existing session.
     *
     * @param request the HttpServletRequest
     * @param response the HttpServletResponse
     * @throws IOException thrown if there is an error writing the output.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");
        if (action != null && action.equals("open")) {
            log.info("Open connection request received.");
            open(request, response);
        } else {
            long id = Long.parseLong(request.getParameter("id"));
            if (action != null && action.equals("close")) {
                log.info("Close connection request recieved.");
                connectionManager.removeConnection(id);
            } else if (action != null && action.equals("read")) {
                log.debug("Read request recieved.");
                read(id, response, true);
            } else if (action != null && action.equals("write")) {
                log.debug("Write request recieved.");
                write(id, request);
            } else if (action != null && action.equals("readwrite")) {
                log.debug("Read/Write request recieved.");
                readWrite(id, request, response);
            }
        }
    }

    /**
     * Handles all connection setup for open connection requests.
     */
    private void open(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = new PrintWriter(response.getWriter());
        try {
            String host = request.getParameter("host");
            String port = request.getParameter("port");
            if (host == null || port == null) {
                throw new Exception("Host or port parameter not specified.");
            }
            User user = null;
            if (SocketProxyAdminServlet.isPasswordRequired()) {
                String username = request.getParameter("username");
                String password = request.getParameter("password");
                if (username == null || password == null) {
                    throw new Exception("Username and Password Required!");
                }
                user = SocketProxyAdminServlet.getUser(username);
                if (user == null) {
                    throw new Exception("Invalid Username!");
                }
                if (!user.isPasswordValid(password)) {
                    throw new Exception("Invalid Password!");
                }
            }
            Socket socket = new Socket(InetAddress.getByName(host), Integer.parseInt(port));
            ConnectionInfo connectionInfo = new ConnectionInfo(user, request.getRemoteHost(), host, port, socket, new Date(), socket.getInputStream(), socket.getOutputStream());
            connectionManager.addConnection(connectionInfo);
            log.info("Connection Opened: " + connectionInfo.toString());
            out.println("SUCCESS");
            out.println(connectionInfo.getConnectionId());
        } catch (Throwable e) {
            log.error("Error processing request for new connection. " + e.getMessage());
            out.println("FAIL - " + e.getMessage());
        }
    }

    /**
     * Handles a READ request from the client.  Data is read from the
     * proxied connection and written to the client.
     *
     * @param id the proxied connection id.
     * @param response the connection to the client.
     * @param block true if the thread should block while waiting for data.
     */
    private void read(long id, HttpServletResponse response, boolean block) {
        byte[] bytes = new byte[1024 * SocketProxyAdminServlet.getBlockSize()];
        int count;
        if (log.isDebugEnabled()) log.debug("Read Buffer Size: " + bytes.length);
        try {
            addReader();
            OutputStream out = response.getOutputStream();
            ConnectionInfo info = connectionManager.getConnection(id);
            if (info == null) {
                log.error("Client requested a connection that was closed (connectionId:" + id + ").");
                out.write(0);
                out.close();
                return;
            }
            InputStream in = info.getInputStream();
            if (!block) {
                info.getSocket().setSoTimeout(100);
            } else {
                info.getSocket().setSoTimeout(0);
            }
            boolean isFirst = true;
            while (true) {
                count = 0;
                if (block) {
                    count = in.read(bytes);
                } else {
                    try {
                        count = in.read(bytes);
                    } catch (SocketTimeoutException timeoutException) {
                    }
                }
                if (count == -1) {
                    out.write(0);
                    out.close();
                    connectionManager.removeConnection(info.getConnectionId());
                    log.info("Removing connection because the remote server closed the connection.");
                    break;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Client read " + count + " bytes.");
                    StringBuffer debugOut = new StringBuffer("Data: ");
                    for (int index = 0; index < count; index++) {
                        debugOut.append((int) bytes[index]);
                        debugOut.append(",");
                    }
                    log.debug(debugOut);
                }
                if (isFirst) {
                    out.write(1);
                    isFirst = false;
                }
                out.write(bytes, 0, count);
                out.flush();
                if (!block) {
                    out.close();
                    break;
                }
            }
        } catch (IOException ioe) {
        } catch (Throwable t) {
            log.error("Non IO Error occured while reading from the proxied telnet connection. " + t.getMessage(), t);
        } finally {
            removeReader();
            if (block) {
                connectionManager.removeConnection(id);
            }
        }
    }

    /**
     * Processes a WRITE request from the client and writes the data sent from
     * the client to the proxied connection.
     *
     * @param id the proxied connection id.
     * @param request used to read the WRITE data from the client.
     */
    private void write(long id, HttpServletRequest request) {
        try {
            addWriter();
            ConnectionInfo info = connectionManager.getConnection(id);
            if (info != null) {
                OutputStream socketOut = info.getOutputStream();
                String data = request.getParameter("data");
                int dataLength = Integer.parseInt(request.getParameter("datalength"));
                byte[] decodedBytes = decode(data, dataLength);
                if (log.isDebugEnabled()) {
                    log.debug("Client wrote " + dataLength + " bytes.");
                    StringBuffer debugOut = new StringBuffer("Data: ");
                    for (int index = 0; index < decodedBytes.length; index++) {
                        debugOut.append((int) decodedBytes[index]);
                        debugOut.append(",");
                    }
                    log.debug(debugOut);
                }
                socketOut.write(decodedBytes);
            } else {
                log.info("Write method attempted to write to a closed connection");
            }
        } catch (IOException ioe) {
        } catch (Throwable t) {
            connectionManager.removeConnection(id);
            log.error("Non IO Error occured while writing to the proxied telnet connection. " + t.getMessage(), t);
        } finally {
            removeWriter();
        }
    }

    /**
     * Processes a READ/WRITE request from the client.  Writes the data sent from
     * the client to the proxied connection and reads data from the proxied connection
     * and writes it to the client.
     *
     * @param id the proxied connection id.
     * @param request used to read the WRITE data from the client.
     * @param response used to write the READ data to the client.
     */
    private void readWrite(long id, HttpServletRequest request, HttpServletResponse response) {
        write(id, request);
        read(id, response, false);
    }

    /**
     * Converts the data payload into a byte array.  See the
     * <a href='http://www.ericdaugherty.com/dev/soht/protocol.html'>
     * Protocol definition</a> for more details.
     *
     * @param inputData encoded String
     * @param decodedLength the number of 'real' bytes sent.
     * @return byte array.
     */
    private byte[] decode(String inputData, int decodedLength) {
        byte[] rawBytes = inputData.getBytes();
        byte[] decodedBytes = new byte[decodedLength];
        int rawIndex = 0;
        char character;
        int decodedInt;
        for (int decodedIndex = 0; decodedIndex < decodedLength; decodedIndex++) {
            character = (char) rawBytes[rawIndex];
            if (character == '#') {
                decodedInt = Integer.decode("#" + (char) rawBytes[rawIndex + 1] + (char) rawBytes[rawIndex + 2]).intValue();
                decodedBytes[decodedIndex] = (byte) decodedInt;
                rawIndex = rawIndex + 3;
            } else {
                decodedBytes[decodedIndex] = rawBytes[rawIndex];
                rawIndex++;
            }
        }
        return decodedBytes;
    }

    /**
     * Provides synchronized access to the total number of readers count.
     */
    private static synchronized void addReader() {
        readerCount++;
    }

    /**
     * Provides synchronized access to the total number of readers count.
     */
    private static synchronized void removeReader() {
        readerCount--;
    }

    /**
     * Provides synchronized access to the total number of writers count.
     */
    private static synchronized void addWriter() {
        writerCount++;
    }

    /**
     * Provides synchronized access to the total number of writers count.
     */
    private static synchronized void removeWriter() {
        writerCount--;
    }
}

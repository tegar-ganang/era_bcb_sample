package com.beanstalktech.servlet.server;

import com.beanstalktech.common.connection.ServerConnection;
import com.beanstalktech.common.context.AppEvent;
import com.beanstalktech.common.context.AppError;
import com.beanstalktech.common.server.*;
import com.beanstalktech.common.utility.Logger;
import java.net.*;
import java.io.*;
import java.util.Vector;

/**
 * Communicates with SOAP protocol services through TCP/IP sockets and HTTP
 * envelopes. Because InputStream read methods are blocking, this adapter 
 * implements Runnable and runs in its own thread. The ServerConnection 
 * that owns it can thereby recover from long-running responses or server 
 * crashes.
 * 
 * @author Beanstalk Technologies LLC
 * @version 1.0 3/15/2002
 * @since Beanstalk V2.2
 * @see com.beanstalktech.common.server.ServerAdapterManager
 */
public class SOAPHTTPServerAdapter extends ServerAdapter implements Runnable {

    protected static final int MAX_HTTP_RESPONSE_HEADER_LENGTH = 2048;

    protected static final int READ_BLOCK_SIZE = 10000;

    protected static final int MAX_HTTP_RESPONSE_BODY_LENGTH = 100000;

    protected Thread m_thread;

    protected AppEvent m_event;

    /**
     * This method advertises the class as a server adapter for
     * SOAPHTTP Server requests.
     * 
     * @param event Application Event containing a reference to the Server 
     * Connection for the request
     */
    public void handle_SOAPHTTP(AppEvent event) {
        handleRequest(event);
    }

    /**
     * Implements
     * 
     * @param event
     */
    public void handleRequest(AppEvent event) {
        m_event = event;
        m_thread = new Thread(this);
        m_thread.start();
    }

    public void run() {
        try {
            Socket socket = (Socket) provideResource(m_event);
            SOAPHTTPServerRequest request = (SOAPHTTPServerRequest) m_event.getConnection().getRequest();
            getLogger(m_event).logMessage(0, "Executing request: " + m_event.getEventType());
            String outString = createRequestHeaders(request) + request.getSOAPRequest();
            getLogger(m_event).logMessage(10, "SOAPHTTPServerAdapter: Sending message: \n" + outString);
            socket.getOutputStream().write(outString.getBytes());
            boolean gotResponse = false;
            InputStream inputStream = null;
            int responseStatus = 500;
            int responseLength = 0;
            Vector responseHeaders = new Vector();
            while (!gotResponse) {
                inputStream = socket.getInputStream();
                responseHeaders = getResponseHeaders(m_event, inputStream);
                responseStatus = getResponseStatus(m_event, responseHeaders);
                responseLength = getResponseLength(m_event, responseHeaders);
                if (responseStatus != 100) gotResponse = true;
            }
            String response = "";
            if (responseLength > 0) {
                getLogger(m_event).logMessage(0, "SOAPHTTP Server Adapter: Response length is: " + responseLength);
                byte[] responseBytes = new byte[READ_BLOCK_SIZE];
                StringBuffer buffer = new StringBuffer();
                int totalBytesRead = 0;
                while (totalBytesRead < responseLength) {
                    int bytesRead = inputStream.read(responseBytes);
                    if (bytesRead == -1) break;
                    buffer.append(new String(responseBytes, 0, bytesRead));
                    responseBytes = new byte[READ_BLOCK_SIZE];
                    totalBytesRead += bytesRead;
                }
                getLogger(m_event).logMessage(0, "SOAPHTTP Server Adapter: Actual bytes read: " + totalBytesRead);
                response = buffer.toString();
            } else if (isChunked(m_event, responseHeaders)) {
                getLogger(m_event).logMessage(10, "SOAPHTTP Server Adapter: Response is chunked...");
                int chunkLength = getChunkLength(m_event, inputStream);
                StringBuffer buffer = new StringBuffer();
                while (chunkLength > 0) {
                    byte[] responseBytes = new byte[READ_BLOCK_SIZE];
                    int totalBytesRead = 0;
                    while (totalBytesRead < chunkLength) {
                        int maxBytes = Math.min(READ_BLOCK_SIZE, chunkLength - totalBytesRead);
                        int bytesRead = inputStream.read(responseBytes, 0, maxBytes);
                        if (bytesRead == -1) break;
                        buffer.append(new String(responseBytes, 0, bytesRead));
                        responseBytes = new byte[READ_BLOCK_SIZE];
                        totalBytesRead += bytesRead;
                    }
                    getLogger(m_event).logMessage(10, "SOAPHTTP Server Adapter: Chunk bytes read: " + totalBytesRead + " of " + chunkLength);
                    chunkLength = getChunkLength(m_event, inputStream);
                }
                response = buffer.toString();
            } else {
                getLogger(m_event).logError(8, "SOAPHTTP Server Adapter: Warning: Content length not specified in HTTP response.");
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < MAX_HTTP_RESPONSE_BODY_LENGTH; i++) {
                    int c = inputStream.read();
                    if (c == -1) break;
                    buffer.append((char) c);
                }
                response = buffer.toString();
            }
            ((ServerConnection) m_event.getConnection()).setServerResource(this, socket);
            m_event.getConnection().setResponse(response);
            getLogger(m_event).logMessage(0, "SOAPHTTP Server Adapter received: " + response);
        } catch (ServerResourceException e) {
            getLogger(m_event).logError(3, "SOAPHTTP Server Adapter failed to obtain socket.");
            m_event.setStatus(AppError.ms_serverError, "SOAPHTTPServerAdapter failed to connect to server.");
            m_event.getConnection().setError(AppError.ms_serverError);
        } catch (Exception e) {
            getLogger(m_event).logError(3, "SOAPHTTP Server Adapter failed to write or read from socket. " + "Exception: " + e);
            m_event.setStatus(AppError.ms_serverError, "SOAPHTTPServerAdapter failed to connect to server.");
            m_event.getConnection().setError(AppError.ms_serverError);
        }
    }

    /**
     * Creates a socket connection. This method is called by the parent's
     * provideResource method when a socket is not available in the
     * socket pool.
     */
    protected synchronized Object createResource(AppEvent event) throws ServerResourceException {
        try {
            SOAPHTTPServerRequest request = (SOAPHTTPServerRequest) m_event.getConnection().getRequest();
            URL url = request.getURL();
            int port = url.getPort();
            if (port == -1) port = 80;
            getLogger(event).logMessage(10, "SOAPHTTPServerAdapter: Obtaining socket for: " + url.getHost() + ":" + port);
            return new Socket(url.getHost(), port);
        } catch (Exception e) {
            getLogger(event).logError(3, "SOAPHTTPServerAdapter: Create socket failed. Exception: " + e);
            throw new ServerResourceException();
        }
    }

    /**
     * Creates the HTTP Request Headers and returns them as a string
     * <P>
     * @param request SOAPHTTPServerRequest object containing header information
     * @return The generated headers, as a single String
     */
    protected static String createRequestHeaders(SOAPHTTPServerRequest request) {
        URL requestURL = request.getURL();
        return new String("POST " + requestURL.getFile() + " HTTP/1.1\n" + "Host: " + requestURL.getHost() + "\n" + "Content-Type: " + request.getContentType() + "\n" + "Content-Length: " + request.getSOAPRequestLength() + "\n" + "SOAPAction: \"" + request.getSOAPAction() + "\"\n" + "\n");
    }

    /**
    * Reads the HTTP headers from a response input stream.
    */
    protected static Vector getResponseHeaders(AppEvent event, InputStream inputStream) {
        Vector headers = new Vector(4, 4);
        StringBuffer buffer = new StringBuffer();
        try {
            for (int i = 0; i < MAX_HTTP_RESPONSE_HEADER_LENGTH; i++) {
                int c = inputStream.read();
                if (c == -1) {
                    throw new Exception("Unexpected end of stream at offset: " + i + " Buffer contents: " + buffer.toString());
                } else if (c == '\n') {
                    String header = buffer.toString().trim();
                    if (header.equals("")) break;
                    getLogger(event).logMessage(3, "Response header: " + header);
                    headers.add(header);
                    buffer = new StringBuffer();
                } else {
                    buffer.append((char) c);
                }
            }
        } catch (Exception e) {
            getLogger(event).logError(3, "SOAPHTTP Server Adapter exception while reading response header. Exception: " + e);
        } finally {
            return headers;
        }
    }

    /**
    * Gets the length of a response message by reading the HTTP headers of
    * the response and interpreting the value specified for the Content Length
    * header.
    */
    protected static int getResponseLength(AppEvent event, Vector headers) {
        int length = 0;
        try {
            for (int i = 0; i < headers.size(); i++) {
                String header = (String) headers.elementAt(i);
                if (header.indexOf("Content-Length:") > -1) {
                    length = new Integer(header.substring(header.indexOf(':') + 1).trim()).intValue();
                    break;
                }
            }
        } catch (Exception e) {
            getLogger(event).logError(3, "SOAPHTTP Server Adapter exception while reading response length. Exception: " + e);
        } finally {
            return length;
        }
    }

    /**
    * Determines if the response encoding is "chunked" -- i.e. the body is
    * divided into segments preceded by byte length indicators.
    */
    protected static boolean isChunked(AppEvent event, Vector headers) {
        int length = 0;
        try {
            for (int i = 0; i < headers.size(); i++) {
                String header = (String) headers.elementAt(i);
                if (header.startsWith("Transfer-Encoding: chunked")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            getLogger(event).logError(3, "SOAPHTTP Server Adapter exception while checking for chunked. Exception: " + e);
            return false;
        }
    }

    /**
    * Reads a chunk length (hexadecimal characters followed
    * by newline) from an input stream
    */
    protected static int getChunkLength(AppEvent event, InputStream inputStream) {
        StringBuffer buffer = new StringBuffer();
        String lengthString = "";
        int length = -1;
        try {
            for (int i = 0; i < 100; i++) {
                int c = inputStream.read();
                if (c == -1) {
                    throw new Exception("Unexpected end of stream while reading chunk length");
                } else if (c == '\n') {
                    lengthString = buffer.toString().trim();
                    if (lengthString.length() > 0) {
                        length = Integer.parseInt(lengthString, 16);
                        break;
                    }
                } else {
                    buffer.append((char) c);
                }
            }
            getLogger(event).logMessage(10, "SOAPHTTP Server Adapter: Chunk length string: \"" + buffer.toString() + "\"");
        } catch (Exception e) {
            getLogger(event).logError(3, "SOAPHTTP Server Adapter exception while reading chunk length: \"" + lengthString + "\" Exception: " + e);
        } finally {
            return length;
        }
    }

    /**
    * Gets the status of a response message by reading the HTTP headers of
    * the response and interpreting the value specified on the status line.
    */
    protected static int getResponseStatus(AppEvent event, Vector headers) {
        int status = 500;
        try {
            for (int i = 0; i < headers.size(); i++) {
                String header = (String) headers.elementAt(i);
                if (header.startsWith("HTTP/")) {
                    header = header.substring(header.indexOf(' ') + 1);
                    header = header.substring(0, header.indexOf(' '));
                    status = new Integer(header).intValue();
                    break;
                }
            }
        } catch (Exception e) {
            getLogger(event).logError(3, "SOAPHTTP Server Adapter exception while reading response status. Exception: " + e);
        } finally {
            getLogger(event).logMessage(10, "SOAPHTTPServerAdapter: Response status: " + status);
            return status;
        }
    }

    /**
     * Overrides ServerAdapter method to return a resource to a pool. Instead
     * the resource (a Socket) is closed and the resources in use counter decremented.
     */
    public synchronized void releaseResource(AppEvent event, Object resource) {
        try {
            String serverID = ((ServerConnection) event.getConnection()).getServerID();
            ((Socket) resource).close();
            ms_resourcesInUse--;
            getLogger(event).logMessage(3, "Closed socket for server: " + serverID + " Sockets still in use: " + ms_resourcesInUse);
        } catch (Exception e) {
            getLogger(event).logError(1, "SOAPHTTP Server Adapter could not close a socket. Exception: " + e);
        }
    }
}

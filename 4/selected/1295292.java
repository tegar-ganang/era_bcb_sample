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
 * Communicates with HTTP server through TCP/IP sockets. This version
 * is suitable for servlets or applications, not for applets.
 * Because InputStream read methods are blocking, this adapter 
 * implements Runnable and runs in its own thread. The ServerConnection 
 * that owns it can thereby recover from long-running responses or server 
 * crashes.
 * 
 * @author Beanstalk Technologies LLC
 * @version 1.0 5/24/2002
 * @since Beanstalk V2.2
 * @seecom.beanstalktech.common.server.ServerAdapterManager
 */
public class HTTPServerAdapter extends ServerAdapter implements Runnable {

    protected static final int MAX_HTTP_RESPONSE_HEADER_LENGTH = 2048;

    protected static final int READ_BLOCK_SIZE = 10000;

    protected static final int RESPONSE_BUFFER_LENGTH = 10000;

    protected Thread m_thread;

    protected AppEvent m_event;

    /**
     * This method advertises the class as a server adapter for
     * HTTP Server requests.
     * 
     * @param event Application Event containing a reference to the Server 
     * Connection for the request
     */
    public void handle_HTTP(AppEvent event) {
        handleRequest(event);
    }

    /**
     * This method advertises the class as a server adapter for
     * ScriptHTTP Server requests.
     * 
     * @param event Application Event containing a reference to the Server 
     * Connection for the request
     */
    public void handle_ScriptHTTP(AppEvent event) {
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
            HTTPServerRequest request = (HTTPServerRequest) m_event.getConnection().getRequest();
            String outString = createRequestHeaders(request);
            getLogger(m_event).logMessage(10, "HTTPServerAdapter: Sending message: \n" + outString);
            OutputStream out = socket.getOutputStream();
            out.write(outString.getBytes());
            out.write(request.getRequestBytes());
            boolean gotResponse = false;
            InputStream inputStream = null;
            int responseStatus = 500;
            int responseLength = 0;
            String responseContentType = "";
            String responseLocation = "";
            Vector responseHeaders = new Vector();
            while (!gotResponse) {
                inputStream = socket.getInputStream();
                responseHeaders = getResponseHeaders(inputStream);
                responseStatus = getResponseStatus(responseHeaders);
                responseLength = getResponseLength(responseHeaders);
                responseContentType = getResponseType(responseHeaders);
                if (responseStatus != 100) gotResponse = true;
            }
            if (responseStatus == 302) {
                responseLocation = getResponseLocation(responseHeaders);
                getLogger(m_event).logMessage(8, "HTTPServerAdapter: Re-directing request to: " + responseLocation);
                URL redirectURL = new URL(responseLocation);
                request.setURL(redirectURL);
                handleRequest(m_event);
                return;
            }
            byte[] responseBytes = new byte[1];
            if (responseLength > -1) {
                getLogger(m_event).logMessage(0, "HTTP Server Adapter: Status: " + responseStatus + " Reported Length: " + responseLength);
                responseBytes = new byte[responseLength];
                int totalBytesRead = 0;
                while (totalBytesRead < responseLength) {
                    int bytesRead = inputStream.read(responseBytes, totalBytesRead, responseLength - totalBytesRead);
                    if (bytesRead == -1) break;
                    totalBytesRead += bytesRead;
                }
            } else {
                byte[] buffer = new byte[RESPONSE_BUFFER_LENGTH];
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                boolean moreData = true;
                while (moreData) {
                    int bytesRead = inputStream.read(buffer, 0, RESPONSE_BUFFER_LENGTH);
                    if (bytesRead == -1) {
                        moreData = false;
                    } else if (bytesRead > 0) {
                        byteStream.write(buffer, 0, bytesRead);
                    }
                }
                responseBytes = byteStream.toByteArray();
                getLogger(m_event).logMessage(8, "HTTP Server Adapter: Content length not specified in HTTP response. Bytes read: " + responseBytes.length);
            }
            ((ServerConnection) m_event.getConnection()).setServerResource(this, socket);
            m_event.getConnection().setResponse(new HTTPServerResponse(responseBytes, responseContentType));
        } catch (ServerResourceException e) {
            getLogger(m_event).logError(3, "HTTP Server Adapter failed to obtain socket.");
            m_event.getConnection().setError(AppError.ms_serverError);
        } catch (Exception e) {
            getLogger(m_event).logError(3, "HTTP Server Adapter failed to write or read from socket. " + "Exception: " + e);
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
            HTTPServerRequest request = (HTTPServerRequest) m_event.getConnection().getRequest();
            URL url = request.getURL();
            int port = url.getPort();
            if (port == -1) port = 80;
            return new Socket(url.getHost(), port);
        } catch (Exception e) {
            getLogger(m_event).logError(3, "HTTPServerAdapter: Create socket failed. Exception: " + e);
            throw new ServerResourceException();
        }
    }

    /**
     * Creates the HTTP Request Headers and returns them as a string
     * <P>
     * @param request HTTPServerRequest object containing header information
     * @return The generated headers, as a single String
     */
    protected static String createRequestHeaders(HTTPServerRequest request) {
        URL requestURL = request.getURL();
        return new String(request.getMethod() + " " + requestURL.getFile() + " HTTP/1.1\n" + "Host: " + requestURL.getHost() + "\n" + "Content-Type: " + request.getContentType() + "\n" + "Content-Length: " + request.getRequestBytesLength() + "\n" + "\n");
    }

    /**
    * Reads the HTTP headers from a response input stream.
    */
    protected Vector getResponseHeaders(InputStream inputStream) {
        Vector headers = new Vector(4, 4);
        StringBuffer buffer = new StringBuffer();
        try {
            for (int i = 0; i < MAX_HTTP_RESPONSE_HEADER_LENGTH; i++) {
                int c = inputStream.read();
                if (c == -1) {
                    throw new Exception("Unexpected end of stream at offset: " + i + " Buffer contents: " + buffer.toString());
                } else if (c == '\n') {
                    headers.addElement(buffer.toString());
                    if (((String) headers.lastElement()).trim().equals("")) break;
                    buffer = new StringBuffer();
                } else {
                    buffer.append((char) c);
                }
            }
        } catch (Exception e) {
            getLogger(m_event).logError(3, "HTTP Server Adapter exception while reading response header. Exception: " + e);
        } finally {
            return headers;
        }
    }

    /**
    * Gets the length of a response message by reading the HTTP headers of
    * the response and interpreting the value specified for the Content Length
    * header. If the length cannot be determined, -1 is returned.
    * <P>
    * @param headers Vector containing the response headers
    * @return Length of the response reported in the headers
    */
    protected int getResponseLength(Vector headers) {
        int length = -1;
        try {
            for (int i = 0; i < headers.size(); i++) {
                String header = (String) headers.elementAt(i);
                if (header.indexOf("Content-Length:") > -1) {
                    length = new Integer(header.substring(header.indexOf(':') + 1).trim()).intValue();
                    break;
                }
            }
        } catch (Exception e) {
            getLogger(m_event).logError(3, "HTTP Server Adapter exception while reading response length. Exception: " + e);
        } finally {
            return length;
        }
    }

    /**
    * Returns the value for the Content-Type header in the
    * HTTP response
    */
    protected String getResponseType(Vector headers) {
        String contentType = "text/html";
        try {
            for (int i = 0; i < headers.size(); i++) {
                String header = (String) headers.elementAt(i);
                if (header.indexOf("Content-Type:") > -1) {
                    contentType = header.substring(header.indexOf(':') + 1).trim();
                    break;
                }
            }
        } catch (Exception e) {
            getLogger(m_event).logError(3, "HTTP Server Adapter exception while reading response type. Exception: " + e);
        } finally {
            return contentType;
        }
    }

    protected String getResponseLocation(Vector headers) {
        String location = "";
        try {
            for (int i = 0; i < headers.size(); i++) {
                String header = (String) headers.elementAt(i);
                if (header.indexOf("Location:") > -1) {
                    location = header.substring(header.indexOf(':') + 1).trim();
                    break;
                }
            }
        } catch (Exception e) {
            getLogger(m_event).logError(3, "HTTP Server Adapter exception while reading re-direct location. Exception: " + e);
        } finally {
            return location;
        }
    }

    /**
    * Gets the status of a response message by reading the HTTP headers of
    * the response and interpreting the value specified on the status line.
    */
    protected int getResponseStatus(Vector headers) {
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
            getLogger(m_event).logError(3, "HTTP Server Adapter exception while reading response status. Exception: " + e);
        } finally {
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
            getLogger(m_event).logMessage(10, "Closed socket for server: " + serverID + " Sockets still in use: " + ms_resourcesInUse);
        } catch (Exception e) {
            getLogger(m_event).logError(1, "HTTP Server Adapter could not close a socket. Exception: " + e);
        }
    }
}

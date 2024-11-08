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
 * Communicates with Simple protocol servers through TCP/IP sockets. Because
 * InputStream read methods are blocking, this adapter implements Runnable
 * and runs in its own thread. The ServerConnection that owns it can thereby
 * recover from long-running responses or server crashes.
 * 
 * @author Stuart Sugarman/Widgetry
 * @version 1.0 12/5/1999
 * @since Beanstalk V1.0
 * @see com.beanstalktech.common.server.ServerAdapterManager
 */
public class SimpleServerAdapter extends ServerAdapter implements Runnable {

    protected Thread m_thread;

    protected AppEvent m_event;

    /**
     * This method advertises the class as a server adapter for
     * Simple Server requests.
     * 
     * @param event Application Event containing a reference to the Server 
     * Connection for the request
     */
    public void handle_Simple(AppEvent event) {
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
        getLogger(m_event).logMessage(0, "Executing query: " + m_event.getConnection().getRequest());
        try {
            Socket socket = (Socket) provideResource(m_event);
            String request = (String) m_event.getConnection().getRequest();
            socket.getOutputStream().write(getStringLength(request));
            socket.getOutputStream().write(request.getBytes());
            InputStream inputStream = socket.getInputStream();
            int responseLength = getResponseLength(inputStream);
            getLogger(m_event).logMessage(0, "Simple Server Adapter: Response length is: " + responseLength);
            byte[] responseBytes = new byte[responseLength];
            inputStream.read(responseBytes);
            ((ServerConnection) m_event.getConnection()).setServerResource(this, socket);
            m_event.getConnection().setResponse(new String(responseBytes));
            getLogger(m_event).logMessage(0, "Simple Server Adapter received: " + new String(responseBytes));
        } catch (ServerResourceException e) {
            getLogger(m_event).logError(3, "Simple Server Adapter failed to obtain socket.");
            m_event.getConnection().setError(AppError.ms_serverError);
        } catch (Exception e) {
            getLogger(m_event).logError(3, "Simple Server Adapter failed to write or read from socket. " + "Exception: " + e);
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
            ServerConnection connection = (ServerConnection) event.getConnection();
            return new Socket(getInetAddress(connection.getServerID()), getPort(connection.getServerID()));
        } catch (Exception e) {
            throw new ServerResourceException();
        }
    }

    /**
    * Given a serverID of the form "host:port", extracts the host portion and
    * converts it to an InetAddress.
    */
    protected InetAddress getInetAddress(String serverID) throws UnknownHostException {
        String hostName = new String(serverID);
        int colonPosition = serverID.indexOf(":");
        if (colonPosition > 0) {
            hostName = serverID.substring(0, colonPosition);
        }
        getLogger(m_event).logMessage(0, "Simple Server adapter host name: " + hostName);
        return InetAddress.getByName(hostName);
    }

    /**
    * Given a serverID of the form "host:port", extracts the port portion and
    * converts it to an integer
    */
    protected int getPort(String serverID) {
        int port = 0;
        int colonPosition = serverID.indexOf(":");
        if (colonPosition > -1 && colonPosition < serverID.length()) {
            try {
                port = new Integer(serverID.substring(colonPosition + 1)).intValue();
            } catch (Exception e) {
                getLogger(m_event).logError(3, "Simple Server Adapter exception while parsing port. Exception: " + e);
            }
        }
        getLogger(m_event).logMessage(0, "Simple Server port: " + port);
        return port;
    }

    /**
     * Computes the length of a string and formats the length into a two byte array
     */
    protected byte[] getStringLength(String string) {
        byte[] byteLength = { 0, 0 };
        try {
            int stringLength = new Integer(string.length()).intValue();
            byteLength[1] = (byte) (stringLength % (Byte.MAX_VALUE + 1));
            byteLength[0] = (byte) ((stringLength - byteLength[1]) / (Byte.MAX_VALUE + 1));
            return byteLength;
        } catch (Exception e) {
            getLogger(m_event).logError(3, "Simple Server Adapter exception while computing string length. Exception: " + e);
            return byteLength;
        }
    }

    /**
      * Gets the length of a response message by examining the first 2 bytes of the
      * socket InputStream. T
      */
    protected int getResponseLength(InputStream inputStream) {
        byte[] byteLength = { 0, 0 };
        try {
            inputStream.read(byteLength);
        } catch (Exception e) {
            getLogger(m_event).logError(3, "Simple Server Adapter exception while reading response length. Exception: " + e);
        } finally {
            return (int) ((byteLength[0] * (Byte.MAX_VALUE + 1)) + byteLength[1]);
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
            getLogger(m_event).logMessage(3, "Closed socket for server: " + serverID + " Sockets still in use: " + ms_resourcesInUse);
        } catch (Exception e) {
            getLogger(m_event).logError(1, "Simple Server Adapter could not close a socket. Exception: " + e);
        }
    }
}
